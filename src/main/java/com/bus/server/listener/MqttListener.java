package com.bus.server.listener;

import java.util.concurrent.RejectedExecutionException;

import org.apache.log4j.Logger;

import com.bus.server.Application;
import com.bus.server.processor.MqttMessageProcessor;
import com.yeild.mqtt.MqttServerTask.OnMqttConnectorListener;
import com.yeild.common.Utils.CommonUtils;
import com.yeild.mqtt.PushMqttMessage;

public class MqttListener implements OnMqttConnectorListener {
	private Logger logger = Logger.getLogger(MqttListener.class);

	@Override
	public void onMqttReceiveMessage(PushMqttMessage pmessage) {
		if(pmessage.getTopic().startsWith(Application.mqttServerTask.getRpcTopicPrefix())) {
			MqttMessageProcessor tProcessor = new MqttMessageProcessor(pmessage);
			try {
				Application.clientProcessorPool.execute(tProcessor);
			} catch (RejectedExecutionException e) {
				logger.error("there are not enough system resources available to run\n"+CommonUtils.getExceptionInfo(e));
			}
		}
	}

}
