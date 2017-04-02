package com.bus.server.processor;

import org.apache.log4j.Logger;

import com.yeild.mqtt.PushMqttMessage;

public class MqttMessageProcessor extends Thread {
	private Logger logger = Logger.getLogger(getClass().getSimpleName());
	private PushMqttMessage message;
	
	public MqttMessageProcessor() {
	}
	
	public MqttMessageProcessor(PushMqttMessage message) {
		this.message = message;
	}

	@Override
	public void run() {
//		if(Application.classRpcProcessMethod == null) {
//			logger.error("未注册数据处理方法");
//			return;
//		}
//		try {
//			String msgcontent = new String(message.getPayload(), "UTF-8");
//			AbstractDataProcessMethod processMethod = (AbstractDataProcessMethod) Application.classRpcProcessMethod.newInstance();
//			String []topicSplit = message.getTopic().split("/");
//			String from = topicSplit[topicSplit.length-3];
//			String result = processMethod.processData(from, msgcontent);
//			if(result == null) {
//				return;
//			}
//			PushMqttMessage resultMessage = new PushMqttMessage(message.getTopic().replaceFirst(Application.mqttServerTask.getRpcRequestName()
//					, Application.mqttServerTask.getRpcResponseName()),message);
//			resultMessage.setPayload(result);
//			resultMessage.setRetained(false);
//			int retry = 0;
//			while (true) {
//				if(Application.mqttServerTask.pushMessageAsync(resultMessage)){
//					break;
//				}
//				try {
//					Thread.sleep(1000);
//				} catch (InterruptedException e) {
//				}
//				if(++retry > 7) {
//					break;
//				}
//			}
//			logger.info(message.getTopic()+" processed:\n"+result);
//		} catch (UnsupportedEncodingException e) {
//			logger.error(JsonUtils.getExceptionInfo(e));;
//		} catch (InstantiationException e) {
//			logger.error("未注册正确的数据处理方法");
//		} catch (IllegalAccessException e) {
//			logger.error("未注册正确的数据处理方法");
//		}
	}
}
