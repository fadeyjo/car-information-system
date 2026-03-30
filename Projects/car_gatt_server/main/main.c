#include <stdio.h>
#include <stdbool.h>
#include <unistd.h>
#include "driver/twai.h"
#include "freertos/projdefs.h"
#include "hal/twai_types.h"
#include "esp_err.h"
#include "esp_log.h"

#define TWAI_TX_NUM GPIO_NUM_5
#define TWAI_RX_NUM GPIO_NUM_4

#define TRIES_COUNT_TO_GET_CURRENT_DATA_SUPPORTED_PIDS 5
#define DELAY_BETWEEN_TRIES_TO_GET_CURRENT_DATA_SUPPORTED_PIDS_MS 1000

#define IDENTIFIER_TO_SEND_REQUESTS 0x7E0

#define CAN_MASK 0x7F8
#define CAN_FILTER 0x7E8

#define CAN_TX_QUEUE_SIZE 32
#define CAN_RX_QUEUE_SIZE 32


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

void reset_semaphores()
{
	if (flag_pids_mutex != NULL)
	{
    	vSemaphoreDelete(flag_pids_mutex);
    	flag_pids_mutex = NULL;
	}
	
	if (pids_mutex != NULL)
	{
    	vSemaphoreDelete(pids_mutex);
    	pids_mutex = NULL;
	}
}

void reset_current_pids_data()
{
	set_current_data_supported_pids_received(false);
	
	set_current_data_supported_pids(0);
}

// Освобождение ресурсов очереди
void reset_data_request_queue()
{
	if (data_request_queue != NULL && x_can_tx_task_handle == NULL)
	{
		xQueueReset(data_request_queue);
		vQueueDelete(data_request_queue);
		data_request_queue = NULL;
	}
}

void uninstall_twai_driver()
{
	esp_err_t err = twai_stop();
		
	if (err != ESP_OK)
    {
		// Послать BLE notification: ошибка остановки драйвера после ошибки при старте драйвера
		return;
	}
		
    err = twai_driver_uninstall();
    	
    if (err != ESP_OK)
    {
		// Послать BLE notification: ошибка сброса драйвера после ошибки при старте драйвера
		return;
	}
	
	twai_initialized = false;
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
    	// Послать BLE notification: ошибка при установке драйвера
    	return err;
	}

    err = twai_start();
    
    if (err != ESP_OK)
    {
		uninstall_twai_driver();
		
		// Послать BLE notification: ошибка при старте драйвера
		
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
		// Послать BLE notification: ошибка при отправке запроса
    	return;
	}
	
    esp_err_t err = twai_transmit(&message, pdMS_TO_TICKS(1000));
    
    if (err != ESP_OK)
    {
		// Послать BLE notification: ошибка при отправке запроса
	}
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
			
			// Послать BLE notification: message.identifier, message.data, message.data_length_code, дату и время получения данных
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
	delete_x_can_rx_task_handle();
	delete_x_can_tx_task_handle();
	
	reset_data_request_queue();
	
	uninstall_twai_driver();
	
	reset_semaphores();
	
	// Послать BLE notification: сессия остановлена
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
		
		if (data_request_queue == NULL)
		{
			uninstall_twai_driver();
			
			 // Послать BLE notification: ошибка при создании очереди
			 
			 return;
		}
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
		
		uninstall_twai_driver();
		
		delete_x_can_rx_task_handle();
		delete_x_can_tx_task_handle();
		
		reset_semaphores();
		
		// Послать BLE notification: ошибка при получении списка доступных pids
		
		return;
	}
	
	// Послать BLE notification: current_data_supported_pids
}

void app_main(void)
{
    start_session(500);
    
    get_data_by_request(0x01, 0x00);
	vTaskDelay(pdMS_TO_TICKS(1000));
	get_data_by_request(0x01, 0x0C);
	vTaskDelay(pdMS_TO_TICKS(1000));
    get_data_by_request(0x01, 0x0D);
	vTaskDelay(pdMS_TO_TICKS(1000));
    
    //vTaskDelay(pdMS_TO_TICKS(30000));
    
    stop_session();
}
