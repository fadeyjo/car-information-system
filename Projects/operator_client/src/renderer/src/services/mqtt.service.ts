import { mapGpsDataFromMqtt } from "@renderer/mappers/trip.mappers";
import { GPSDataMqtt } from "@renderer/types/trip.types";
import mqtt, { MqttClient } from "mqtt";

class MQTTService {
  private client: MqttClient | null = null;

  private newTripTopicBase = "new-trip/"
  public newTripCallback: ((tripId: number) => void) | null = null

  private endTripTopicBase = "end-trip/"
  public endTripCallback: ((tripId: number) => void) | null = null

  private gpsDataBase = "gps/new-data/"
  public gpsDataCallback: ((gpsData: GPSDataMqtt) => void) | null = null

  private topics: Array<string> = []

  connect() {
    if (this.client) return;

    this.client = mqtt.connect(import.meta.env.VITE_MQTT_URL);

    this.client.on("connect", () => {
      console.log("MQTT connected");
    });

    this.client.on("error", (err) => {
      console.error("MQTT error:", err);
    });

    this.client.on("message", (receivedTopic, payload) => {
      console.log(receivedTopic)
      if (receivedTopic.startsWith(this.newTripTopicBase)) {
        const id = Number(receivedTopic.split("/")[1])
        if (this.newTripCallback)
            this.newTripCallback(id)
      }
      else if (receivedTopic.startsWith(this.endTripTopicBase)) {
        const id = Number(receivedTopic.split("/")[1])
        if (this.endTripCallback)
            this.endTripCallback(id)
      }
      else if (receivedTopic.startsWith(this.gpsDataBase)) {
        if (this.gpsDataCallback)
            this.gpsDataCallback(mapGpsDataFromMqtt(payload.toString()))
      }
    });
  }

  subscribe(topic: string) {
    if (!this.client) {
      throw new Error("MQTT client is not connected");
    }

    this.client.subscribe(topic, (err) => {
      if (err) {
        console.error("Subscribe error:", err);
      }
    });

    this.topics.push(topic)
  }

  disconnect() {
    this.topics.forEach(element => {
        this.client?.unsubscribe(element)
    });

    this.topics = []
    
    this.newTripCallback = null
    this.endTripCallback = null
    
    this.client?.end();
    this.client = null;
  }
}

export const mqttService = new MQTTService();