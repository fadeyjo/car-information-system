package com.example.dataproviderapp.mqtt

import com.example.dataproviderapp.BuildConfig
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage

object MqttClient {
    private val clientId = "android-kotlin-client"
    private var client: MqttClient? = null

    fun connectMqtt() {
        client = MqttClient(BuildConfig.MQTT_URL, clientId, null)

        val options = MqttConnectOptions().apply {
            isCleanSession = true
            connectionTimeout = 10
            keepAliveInterval = 20
        }

        client!!.connect(options)
    }

    fun publishJson(topic: String, json: String) {
        if (client == null) {
            return
        }

        if (!client!!.isConnected) {
            connectMqtt()
        }

        val message = MqttMessage(json.toByteArray()).apply {
            qos = 1
            isRetained = false
        }

        client!!.publish(topic, message)
    }

    fun disconnect() {
        client?.disconnect()
        client = null
    }
}