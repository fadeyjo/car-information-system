package api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;

import java.util.UUID;

public class MqttApi {
    private final static String BASE_URL = "tcp://185.120.59.21:1883";

    private static IMqttClient publisher;

    public static void connect() throws MqttException {
        if (publisher == null) {
            String publisherId = UUID.randomUUID().toString();
            publisher = new MqttClient(BASE_URL, publisherId);
        }

        if (!publisher.isConnected()) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(15);

            publisher.connect(options);
        }
    }

    public static void publish(String topic, Object payload) throws JsonProcessingException, MqttException {
        if (publisher == null || !publisher.isConnected()) {
            connect();
        }

        ObjectMapper objectMapper = new ObjectMapper();

        String json = objectMapper.writeValueAsString(payload);

        MqttMessage msg = new MqttMessage(json.getBytes());
        msg.setQos(3);
        msg.setRetained(false);

        publisher.publish(topic, msg);
    }
}
