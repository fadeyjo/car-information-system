#include <stdio.h>
#include <stdbool.h>
#include <unistd.h>
#include <string.h>
#include "driver/twai.h"
#include "freertos/projdefs.h"
#include "hal/twai_types.h"
#include "esp_err.h"
#include "esp_log.h"
#include "nvs_flash.h"

#include "esp_bt.h"
#include "esp_nimble_hci.h"
#include "nimble/nimble_port.h"
#include "nimble/nimble_port_freertos.h"
#include "host/ble_hs.h"
#include "host/ble_uuid.h"
#include "host/util/util.h"
#include "os/os_mbuf.h"
#include "services/gap/ble_svc_gap.h"
#include "services/gatt/ble_svc_gatt.h"

void ble_hs_lock(void);
void ble_hs_unlock(void);

#define TWAI_TX_NUM GPIO_NUM_5
#define TWAI_RX_NUM GPIO_NUM_4

#define TRIES_COUNT_TO_GET_CURRENT_DATA_SUPPORTED_PIDS 5
#define DELAY_BETWEEN_TRIES_TO_GET_CURRENT_DATA_SUPPORTED_PIDS_MS 1000

#define IDENTIFIER_TO_SEND_REQUESTS 0x7E0

#define CAN_MASK 0x7F8
#define CAN_FILTER 0x7E8

#define CAN_TX_QUEUE_SIZE 32
#define CAN_RX_QUEUE_SIZE 32


static const char *TAG = "car_gatt";

// ===== BLE protocol =====
// Command (write):
//  - [0] cmd
//    - 0x01 START_SESSION: [1..2] can_speed_le (uint16)
//    - 0x02 GET_DATA:      [1] mode, [2] pid
//    - 0x03 STOP_SESSION:  (no payload)
//
// Notify (notify):
//  - [0] evt
//    - 0x01 SESSION_STARTED: [1..2] can_speed_le, [3..6] supported_pids_le (uint32)
//    - 0x02 OBD_RESPONSE:    [1..4] identifier_le (uint32), [5] dlc, [6..] data (dlc bytes, max 8)
//    - 0x03 SESSION_STOPPED: (no payload)
//    - 0xFF ERROR:           [1] len, [2..] utf8 error text (len bytes, truncated)
enum {
	BLE_CMD_START_SESSION = 0x01,
	BLE_CMD_GET_DATA = 0x02,
	BLE_CMD_STOP_SESSION = 0x03,
};

enum {
	BLE_EVT_SESSION_STARTED = 0x01,
	BLE_EVT_OBD_RESPONSE = 0x02,
	BLE_EVT_SESSION_STOPPED = 0x03
};


typedef struct {
    uint8_t data[66];
    uint16_t len;
} ble_msg_t;

QueueHandle_t ble_queue;

static TaskHandle_t ble_notify_task_handle;

static uint16_t g_ble_conn_handle = BLE_HS_CONN_HANDLE_NONE;
static bool g_ble_notify_enabled = false;
static uint16_t g_ble_notify_val_handle = 0;
static uint8_t g_own_addr_type = 0;

static void ble_notify_session_started(uint16_t can_speed, uint32_t supported_pids);
static void ble_notify_session_stopped(void);
static void ble_notify_obd_response(const twai_message_t *message);

static inline void write_le16(uint8_t *dst, uint16_t v)
{
	dst[0] = (uint8_t)(v & 0xFF);
	dst[1] = (uint8_t)((v >> 8) & 0xFF);
}

static inline void write_le32(uint8_t *dst, uint32_t v)
{
	dst[0] = (uint8_t)(v & 0xFF);
	dst[1] = (uint8_t)((v >> 8) & 0xFF);
	dst[2] = (uint8_t)((v >> 16) & 0xFF);
	dst[3] = (uint8_t)((v >> 24) & 0xFF);
}

static inline uint16_t read_le16(const uint8_t *src)
{
	return (uint16_t)src[0] | ((uint16_t)src[1] << 8);
}

typedef struct {
    uint8_t mode;
    uint8_t pid;
} can_msg_t;


bool twai_initialized = false;

QueueHandle_t data_request_queue = NULL;

TaskHandle_t x_can_rx_task_handle = NULL;
TaskHandle_t x_can_tx_task_handle = NULL;

SemaphoreHandle_t flag_pids_mutex = NULL;
SemaphoreHandle_t pids_mutex = NULL;

bool current_data_supported_pids_received = false;
uint32_t current_data_supported_pids = 0;

static bool ble_can_notify(void)
{
	return g_ble_conn_handle != BLE_HS_CONN_HANDLE_NONE && g_ble_notify_enabled && g_ble_notify_val_handle != 0;
}

static void ble_notify_bytes(const uint8_t *data, uint16_t len)
{
    if (!ble_can_notify()) {
        return;
    }

    ble_msg_t msg;
	memcpy(msg.data, data, len);
	msg.len = len;
	if (xQueueSend(ble_queue, &msg, pdMS_TO_TICKS(50)) != pdTRUE) {
		ESP_LOGW(TAG, "ble notify queue full");
	}
}

static void ble_notify_task(void *arg)
{
	(void)arg;
	ble_msg_t msg;

	for (;;) {
		if (xQueueReceive(ble_queue, &msg, portMAX_DELAY) != pdTRUE) {
			continue;
		}

		ble_hs_lock();

		if (!ble_can_notify()) {
			ble_hs_unlock();
			continue;
		}
		
		printf("Data\n");

		struct os_mbuf *om = ble_hs_mbuf_from_flat(msg.data, msg.len);
		if (!om) {
			ESP_LOGE(TAG, "mbuf alloc failed");
			ble_hs_unlock();
			continue;
		}
		
		printf("Data\n");

		int rc = ble_gatts_notify_custom(
			g_ble_conn_handle,
			g_ble_notify_val_handle,
			om
		);

		if (rc != 0) {
			ESP_LOGE(TAG, "notify failed rc=%d", rc);
			os_mbuf_free_chain(om);
		}

		ble_hs_unlock();
	}
}

static void ble_notify_session_started(uint16_t can_speed, uint32_t supported_pids)
{
	uint8_t buf[1 + 2 + 4];
	buf[0] = BLE_EVT_SESSION_STARTED;
	write_le16(&buf[1], can_speed);
	write_le32(&buf[3], supported_pids);
	ble_notify_bytes(buf, sizeof(buf));
}

static void ble_notify_session_stopped(void)
{
	uint8_t buf[1];
	buf[0] = BLE_EVT_SESSION_STOPPED;
	ble_notify_bytes(buf, sizeof(buf));
}

static void ble_notify_obd_response(const twai_message_t *message)
{
	if (!message)
	{
		return;
	}

	uint8_t dlc = message->data_length_code;
	if (dlc > 8)
	{
		dlc = 8;
	}

	uint8_t buf[1 + 4 + 1 + 8];
	buf[0] = BLE_EVT_OBD_RESPONSE;
	write_le32(&buf[1], (uint32_t)message->identifier);
	buf[5] = dlc;
	memcpy(&buf[6], message->data, dlc);
	ble_notify_bytes(buf, (uint16_t)(6 + dlc));
}

void set_current_data_supported_pids(uint32_t value)
{
	xSemaphoreTake(pids_mutex, portMAX_DELAY);
	current_data_supported_pids = value;
	xSemaphoreGive(pids_mutex);
}

uint32_t get_current_data_supported_pids()
{
	uint32_t buf;
	xSemaphoreTake(pids_mutex, portMAX_DELAY);
	buf = current_data_supported_pids;
	xSemaphoreGive(pids_mutex);
	
	return buf;
}

void set_current_data_supported_pids_received(bool value)
{
	xSemaphoreTake(flag_pids_mutex, portMAX_DELAY);
	current_data_supported_pids_received = value;
	xSemaphoreGive(flag_pids_mutex);
}

bool get_current_data_supported_pids_received()
{
	bool buf;
	xSemaphoreTake(flag_pids_mutex, portMAX_DELAY);
	buf = current_data_supported_pids_received;
	xSemaphoreGive(flag_pids_mutex);
	
	return buf;
}

void reset_current_pids_data()
{
	set_current_data_supported_pids_received(false);
	
	set_current_data_supported_pids(0);
}

void reset_data_request_queue()
{
	if (data_request_queue != NULL)
	{
		xQueueReset(data_request_queue);
	}
}

void delete_x_can_rx_task_handle()
{
	if (x_can_rx_task_handle != NULL)
	{
		vTaskDelete(x_can_rx_task_handle);
		x_can_rx_task_handle = NULL;
	}
}

void delete_x_can_tx_task_handle()
{
	if (x_can_tx_task_handle != NULL)
	{
		vTaskDelete(x_can_tx_task_handle);
		x_can_tx_task_handle = NULL;
	}
}

twai_timing_config_t get_can_speed(uint16_t can_speed)
{
	switch (can_speed) {
    	case 500:
        	return (twai_timing_config_t)TWAI_TIMING_CONFIG_500KBITS();
    	case 250:
        	return (twai_timing_config_t)TWAI_TIMING_CONFIG_250KBITS();
        default:
        	return (twai_timing_config_t)TWAI_TIMING_CONFIG_125KBITS();		
	}
}

esp_err_t twai_init(uint16_t can_speed)
{
    twai_general_config_t g_config = TWAI_GENERAL_CONFIG_DEFAULT(
        TWAI_TX_NUM,
        TWAI_RX_NUM,
        TWAI_MODE_NORMAL
    );
    
    g_config.tx_queue_len = CAN_TX_QUEUE_SIZE;
    g_config.rx_queue_len = CAN_RX_QUEUE_SIZE;
    g_config.alerts_enabled = TWAI_ALERT_NONE;
    g_config.clkout_divider = 0;

    twai_timing_config_t t_config = get_can_speed(can_speed);
    
    twai_filter_config_t f_config = {
    	.acceptance_code = (CAN_FILTER << 21),
    	.acceptance_mask = ~(CAN_MASK << 21),
    	.single_filter = true
	};

    esp_err_t err = twai_driver_install(&g_config, &t_config, &f_config);
    
	if (err != ESP_OK)
	{
    	return err;
	}

    err = twai_start();
    
    if (err != ESP_OK)
    {
    	return err;
	}
	
	twai_initialized = true;
	
	return ESP_OK;
}

void send_obd_request(uint8_t mode, uint8_t pid)
{
    twai_message_t message = {
        .identifier = IDENTIFIER_TO_SEND_REQUESTS,
        .extd = 0,
        .rtr = 0,
        .data_length_code = 8,
        .data = {0x02, mode, pid, 0, 0, 0, 0, 0}
    };
	
	twai_status_info_t  status;
	twai_get_status_info(&status);

	if (status.state != TWAI_STATE_RUNNING)
	{
    	return;
	}
	
    twai_transmit(&message, pdMS_TO_TICKS(1000));
}

void can_transmit_task(void *arg)
{
    can_msg_t msg;

    while (1)
    {
        if (xQueueReceive(data_request_queue, &msg, portMAX_DELAY))
        {
            send_obd_request(msg.mode, msg.pid);
        }
    }
}

bool is_valid_can_identifier(uint32_t identifier)
{
	return identifier >= 0x7E8 && identifier <= 0x7EF;
}

void deserialize_supported_pids(uint8_t response[])
{
	uint32_t splitted =
		((uint32_t)response[3] << 24) +
		((uint32_t)response[4] << 16) +
		((uint32_t)response[5] << 8) +
		((uint32_t)response[6]);
	
	uint32_t mask = 0x80000000;
	
	uint32_t base = 0x80000000;
	
	uint32_t buf = get_current_data_supported_pids();
	
	for (int shift = 0; shift < 32; shift++)
	{
		if ((splitted << shift) & mask)
		{
			buf |= (base >> shift);
		}
	}
	set_current_data_supported_pids(buf);
}

void can_receive_task(void *arg)
{
    twai_message_t message;

    while (1)
    {
        if (twai_receive(&message, pdMS_TO_TICKS(1000)) == ESP_OK)
        {
			printf("RX: id=%lx\n", (unsigned long)message.identifier);
			
			for (int i = 0; i < message.data_length_code; i++)
			{
				printf("%02X", message.data[i]);
			}
			printf("\n");
				
            if (!is_valid_can_identifier(message.identifier))
            {
				continue;
			}
			
			// Проверка, что пришёл ответ
			if (message.data[1] != 0x41)
			{
				continue;
			}
			
			bool received = get_current_data_supported_pids_received();
			
			if (!received)
			{
				if (message.data[2] != 0x00)
				{
					continue;
				}
				
				if (message.data_length_code < 6 || message.data_length_code > 8)
				{
					continue;
				}
				
				deserialize_supported_pids(message.data);
				
				set_current_data_supported_pids_received(true);
				
				continue;
			}
			
			// Минимум ответа: длина ответа, 41 (что пришел ответ), какой PID, минимум 1 байт информации = 3 байта
			if (message.data_length_code < 4 || message.data_length_code > 8)
			{
				continue;
			}
			
			ble_notify_obd_response(&message);
        }
    }
}

void get_supported_current_data_pids()
{
	can_msg_t data = {
		.mode = 0x01,
		.pid = 0x00
	};
	
	if (data_request_queue == NULL)
	{
		return;
	}
	
	bool received;
	
	uint8_t try_count = 0;
	while (try_count < TRIES_COUNT_TO_GET_CURRENT_DATA_SUPPORTED_PIDS)
	{
		xQueueSend(data_request_queue, &data, portMAX_DELAY);
		vTaskDelay(pdMS_TO_TICKS(DELAY_BETWEEN_TRIES_TO_GET_CURRENT_DATA_SUPPORTED_PIDS_MS));
		
		received = get_current_data_supported_pids_received();
		
		if (received)
		{
			return;
		}
		
		try_count += 1;
	}
}

// Должна отработать, когда по BLE пришла команда для окончания сессии
void stop_session()
{
	reset_current_pids_data();
	
	delete_x_can_rx_task_handle();
	delete_x_can_tx_task_handle();
	
	reset_data_request_queue();
	
	ble_notify_session_stopped();
}

// Должна отработать, когда по BLE пришла команда для выполнения запроса к OBDII (с командой передается mode и pid)
void get_data_by_request(uint8_t mode, uint8_t pid)
{
	can_msg_t msg = {
		.mode = mode,
		.pid = pid
	};
	
	if (data_request_queue != NULL)
	{
		xQueueSend(data_request_queue, &msg, portMAX_DELAY);
	}
}

// Должна отработать, когда по BLE пришла команда начала сессии, с командой должна передаваться скорость работы
void start_session(uint16_t can_speed)
{
	ble_notify_session_started(500, 0xff554433);
	return;
	
	esp_err_t err;
	if (!twai_initialized)
	{
		err = twai_init(can_speed);
		
		if (err != ESP_OK)
		{
			return;
		}
	}
	
	if (data_request_queue == NULL)
	{
		data_request_queue = xQueueCreate(10, sizeof(can_msg_t));
	}
	else
	{
		reset_data_request_queue();
	}
	
	if (flag_pids_mutex == NULL)
	{
		flag_pids_mutex = xSemaphoreCreateMutex();
	}
	
	if (pids_mutex == NULL)
	{
		pids_mutex = xSemaphoreCreateMutex();	
	}
	
	if (x_can_rx_task_handle == NULL)
	{
		xTaskCreate(
			can_receive_task,
			"can_rx",
			4096,
			NULL,
			5,
			&x_can_rx_task_handle
		);
	}
	
	if (x_can_tx_task_handle == NULL)
	{
		xTaskCreate(
			can_transmit_task,
			"can_tx",
			4096,
			NULL,
			5,
			&x_can_tx_task_handle
		);	
	}
	
	get_supported_current_data_pids();
	
	bool received = get_current_data_supported_pids_received();
	
	uint32_t pids = get_current_data_supported_pids();
	
	if (!received || pids == 0)
	{
		reset_current_pids_data();
		
		delete_x_can_rx_task_handle();
		delete_x_can_tx_task_handle();
		
		reset_data_request_queue();
		
		return;
	}
	
	ble_notify_session_started(can_speed, pids);
}

// ===== BLE GATT server (NimBLE) =====

static const ble_uuid128_t g_svc_uuid =
	BLE_UUID128_INIT(0x1a, 0x7d, 0x00, 0x00, 0xf2, 0x4b, 0x3c, 0xa2, 0x5b, 0x4a, 0xa2, 0x9c, 0x0f, 0x65, 0x94, 0x3a);
static const ble_uuid128_t g_cmd_uuid =
	BLE_UUID128_INIT(0x1a, 0x7d, 0x01, 0x00, 0xf2, 0x4b, 0x3c, 0xa2, 0x5b, 0x4a, 0xa2, 0x9c, 0x0f, 0x65, 0x94, 0x3a);
static const ble_uuid128_t g_notify_uuid =
	BLE_UUID128_INIT(0x1a, 0x7d, 0x02, 0x00, 0xf2, 0x4b, 0x3c, 0xa2, 0x5b, 0x4a, 0xa2, 0x9c, 0x0f, 0x65, 0x94, 0x3a);

static int gatt_chr_access_cmd(uint16_t conn_handle, uint16_t attr_handle,
							   struct ble_gatt_access_ctxt *ctxt, void *arg)
{
	(void)conn_handle;
	(void)attr_handle;
	(void)ctxt;
	(void)arg;
	return BLE_ATT_ERR_UNLIKELY;
}

static void handle_ble_command(const uint8_t *data, uint16_t len)
{
	for (int i = 0; i < len; i++)
	{
		printf("%02X", data[i]);
	}
	printf("\n");
	
	if (!data || len < 1)
	{
		return;
	}

	uint8_t cmd = data[0];
	switch (cmd)
	{
		case BLE_CMD_START_SESSION:
			if (len < 3)
			{
				return;
			}
			start_session(read_le16(&data[1]));
			return;

		case BLE_CMD_GET_DATA:
			if (len < 3)
			{
				return;
			}
			get_data_by_request(data[1], data[2]);
			return;

		case BLE_CMD_STOP_SESSION:
			stop_session();
			return;

		default:
			return;
	}
}

static int gatt_chr_access_cmd_write(uint16_t conn_handle, uint16_t attr_handle,
									 struct ble_gatt_access_ctxt *ctxt, void *arg)
{
	(void)conn_handle;
	(void)attr_handle;
	(void)arg;

	if (!ctxt || ctxt->op != BLE_GATT_ACCESS_OP_WRITE_CHR)
	{
		return BLE_ATT_ERR_UNLIKELY;
	}

	uint8_t buf[32];
	uint16_t len = OS_MBUF_PKTLEN(ctxt->om);
	if (len > sizeof(buf))
	{
		return BLE_ATT_ERR_INVALID_ATTR_VALUE_LEN;
	}

	int rc = ble_hs_mbuf_to_flat(ctxt->om, buf, sizeof(buf), &len);
	if (rc != 0)
	{
		return BLE_ATT_ERR_UNLIKELY;
	}

	handle_ble_command(buf, len);
	return 0;
}

static const struct ble_gatt_svc_def gatt_svcs[] = {
	{
		.type = BLE_GATT_SVC_TYPE_PRIMARY,
		.uuid = &g_svc_uuid.u,
		.characteristics = (struct ble_gatt_chr_def[]){
			{
				.uuid = &g_cmd_uuid.u,
				.access_cb = gatt_chr_access_cmd_write,
				.flags = BLE_GATT_CHR_F_WRITE | BLE_GATT_CHR_F_WRITE_NO_RSP,
			},
			{
				.uuid = &g_notify_uuid.u,
				.access_cb = gatt_chr_access_cmd,
				.val_handle = &g_ble_notify_val_handle,
				.flags = BLE_GATT_CHR_F_NOTIFY,
			},
			{0}
		},
	},
	{0},
};

static void ble_app_advertise(void);

static int ble_gap_event_cb(struct ble_gap_event *event, void *arg)
{
	(void)arg;

	switch (event->type)
	{
		case BLE_GAP_EVENT_CONNECT:
			if (event->connect.status == 0)
			{
				g_ble_conn_handle = event->connect.conn_handle;
				g_ble_notify_enabled = false;
			}
			else
			{
				g_ble_conn_handle = BLE_HS_CONN_HANDLE_NONE;
				g_ble_notify_enabled = false;
				ble_app_advertise();
			}
			return 0;

		case BLE_GAP_EVENT_DISCONNECT:
			g_ble_conn_handle = BLE_HS_CONN_HANDLE_NONE;
			g_ble_notify_enabled = false;
			ble_app_advertise();
			return 0;

		case BLE_GAP_EVENT_SUBSCRIBE:
			if (event->subscribe.attr_handle == g_ble_notify_val_handle)
			{
				g_ble_notify_enabled = event->subscribe.cur_notify;
			}
			return 0;

		case BLE_GAP_EVENT_MTU:
			return 0;

		default:
			return 0;
	}
}

static void ble_app_advertise(void)
{
	struct ble_hs_adv_fields fields;
	memset(&fields, 0, sizeof(fields));

	const char *name = ble_svc_gap_device_name();
	fields.name = (const uint8_t *)name;
	fields.name_len = (uint8_t)strlen(name);
	fields.name_is_complete = 1;

	fields.flags = BLE_HS_ADV_F_DISC_GEN | BLE_HS_ADV_F_BREDR_UNSUP;
	
	fields.uuids128 = (ble_uuid128_t[]){g_svc_uuid};
	fields.num_uuids128 = 1;
	fields.uuids128_is_complete = 1;

	int rc = ble_gap_adv_set_fields(&fields);
	if (rc != 0)
	{
		ESP_LOGW(TAG, "adv_set_fields 1 rc=%d", rc);
		return;
	}

	struct ble_gap_adv_params adv_params;
	memset(&adv_params, 0, sizeof(adv_params));
	adv_params.conn_mode = BLE_GAP_CONN_MODE_UND;
	adv_params.disc_mode = BLE_GAP_DISC_MODE_GEN;

	rc = ble_gap_adv_start(g_own_addr_type, NULL, BLE_HS_FOREVER, &adv_params, ble_gap_event_cb, NULL);
	if (rc != 0)
	{
		ESP_LOGW(TAG, "adv_start rc=%d", rc);
		return;
	}
}

static void ble_on_sync(void)
{
	int rc = ble_hs_id_infer_auto(0, &g_own_addr_type);
	if (rc != 0)
	{
		ESP_LOGW(TAG, "ble_hs_id_infer_auto rc=%d", rc);
	}
	ble_app_advertise();
}

static void ble_host_task(void *param)
{
	(void)param;
	nimble_port_run();
	nimble_port_freertos_deinit();
}

static esp_err_t ble_init(void)
{
    esp_err_t err = nvs_flash_init();
    if (err == ESP_ERR_NVS_NO_FREE_PAGES || err == ESP_ERR_NVS_NEW_VERSION_FOUND)
    {
        err = nvs_flash_erase();
        
        if (err != ESP_OK)
        {
			return err;
		}
        
        err = nvs_flash_init();
        
        if (err != ESP_OK)
        {
			return err;
		}
    }

	err = nimble_port_init();
	if (err != ESP_OK)
    {
		return err;
	}
    
    
    err = esp_nimble_hci_init();
    if (err != ESP_OK)
    {
		return err;
	}

    ble_svc_gap_init();
    ble_svc_gatt_init();

    ble_hs_cfg.sync_cb = ble_on_sync;

    ble_svc_gap_device_name_set("OBDII");

    int rc = ble_gatts_count_cfg(gatt_svcs);
    if (rc != 0)
    {
        return ESP_FAIL;
    }

    rc = ble_gatts_add_svcs(gatt_svcs);
    if (rc != 0)
    {
        return ESP_FAIL;
    }

    nimble_port_freertos_init(ble_host_task);

    if (ble_queue == NULL) {
		ble_queue = xQueueCreate(10, sizeof(ble_msg_t));
	}
	if (ble_queue == NULL) {
		return ESP_ERR_NO_MEM;
	}
	if (ble_notify_task_handle == NULL) {
		BaseType_t ok = xTaskCreate(
			ble_notify_task,
			"ble_notify",
			4096,
			NULL,
			5,
			&ble_notify_task_handle
		);
		if (ok != pdPASS) {
			return ESP_ERR_NO_MEM;
		}
	}

    return ESP_OK;
}

void app_main(void)
{
	ESP_ERROR_CHECK(ble_init());
	
	while (1)
	{
		vTaskDelay(pdMS_TO_TICKS(1000));
	}
}
