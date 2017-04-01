package com.bus.server.mqtt;

import java.io.UnsupportedEncodingException;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public class PushMqttMessage extends MqttMessage {
	private String topic;
	
	public PushMqttMessage() {
	}
	
	public PushMqttMessage(MqttMessage message) {
		this(null, message);
	}
	
	public PushMqttMessage(String topic, MqttMessage message) {
		this.topic = topic;
		if(message != null) {
			this.setId(message.getId());
			this.setQos(message.getQos());
			this.setRetained(message.isRetained());
			this.setPayload(message.getPayload());
		}
	}
	
	public void setPayload(String msg) {
		try {
			setPayload(msg.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			setPayload(msg.getBytes());
		}
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic.replaceAll("[^0-9a-zA-Z/_\\-+#]*", "");
	}
	
	
}
