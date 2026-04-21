package com.example.dataproviderapp.mqtt

import com.example.dataproviderapp.BuildConfig
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttException

object MqttClient {
    private val clientId = "android-kotlin-client"
    private var client: MqttClient? = null
    private val lock = Any()

    private var telemetryTripId: Long? = null
    private var onTripSpeed: ((Int) -> Unit)? = null
    private var onTripRpm: ((Int) -> Unit)? = null
    private var onTripTemp: ((Int) -> Unit)? = null

    fun connectMqtt() {
        synchronized(lock) {
            if (client == null) {
                client = MqttClient(BuildConfig.MQTT_URL, clientId, null)
                attachCallbackLocked()
            }
        }

        val options = MqttConnectOptions().apply {
            isCleanSession = true
            connectionTimeout = 10
            keepAliveInterval = 20
        }

        synchronized(lock) {
            try {
                if (client?.isConnected != true) {
                    client?.connect(options)
                }
            } catch (_: MqttException) { }
        }
    }

    fun publishJson(topic: String, json: String) {
        val message = MqttMessage(json.toByteArray()).apply {
            qos = 1
            isRetained = false
        }

        synchronized(lock) {
            val c = client ?: run {
                connectMqtt()
                client
            } ?: return

            if (!c.isConnected) {
                connectMqtt()
            }

            try {
                if (c.isConnected) {
                    c.publish(topic, message)
                }
            } catch (_: MqttException) {
                // ignore
            }
        }
    }

    fun subscribeTripTelemetry(
        tripId: ULong,
        onSpeed: (Int) -> Unit,
        onRpm: (Int) -> Unit,
        onTemp: (Int) -> Unit,
    ) {
        synchronized(lock) {
            if (client == null || client?.isConnected != true) {
                connectMqtt()
            }

            telemetryTripId = tripId.toLong()
            onTripSpeed = onSpeed
            onTripRpm = onRpm
            onTripTemp = onTemp

            val c = client ?: return
            if (!c.isConnected) return

            try {
                c.subscribe("telemetry/speed/$tripId", 1)
                c.subscribe("telemetry/RPM/$tripId", 1)
                c.subscribe("telemetry/tempreture/$tripId", 1)
            } catch (_: MqttException) { }
        }
    }

    fun unsubscribeTripTelemetry(tripId: ULong) {
        synchronized(lock) {
            val c = client
            if (c != null && c.isConnected) {
                try {
                    c.unsubscribe("telemetry/speed/$tripId")
                    c.unsubscribe("telemetry/RPM/$tripId")
                    c.unsubscribe("telemetry/tempreture/$tripId")
                } catch (_: MqttException) { }
            }

            if (telemetryTripId?.toULong() == tripId) {
                telemetryTripId = null
                onTripSpeed = null
                onTripRpm = null
                onTripTemp = null
            }
        }
    }

    fun disconnect() {
        synchronized(lock) {
            try {
                client?.disconnect()
            } catch (_: MqttException) {
                // ignore
            } finally {
                client = null
                telemetryTripId = null
                onTripSpeed = null
                onTripRpm = null
                onTripTemp = null
            }
        }
    }

    private fun attachCallbackLocked() {
        val c = client ?: return

        c.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) = Unit

            override fun connectionLost(cause: Throwable?) = Unit

            override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val t = topic ?: return
                val payload = message?.payload?.toString(Charsets.UTF_8)?.trim().orEmpty()
                val value = payload.toIntOrNull() ?: return

                val tripIdLocal = telemetryTripId
                if (tripIdLocal == null) return

                when (t) {
                    "telemetry/speed/$tripIdLocal" -> onTripSpeed?.invoke(value)
                    "telemetry/RPM/$tripIdLocal" -> onTripRpm?.invoke(value)
                    "telemetry/tempreture/$tripIdLocal" -> onTripTemp?.invoke(value)
                }
            }
        })
    }
}