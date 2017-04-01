package com.bus.server.processor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.bus.domain.bus.BusStationReportInfo;
import com.bus.server.Application;
import com.bus.server.mqtt.PushMqttMessage;

public class BusInfoProcessor extends Thread {
	private Logger logger = Logger.getLogger(getClass().getSimpleName());
	private LinkedBlockingQueue<BusStationReportInfo> stationReports = new LinkedBlockingQueue<BusStationReportInfo>(100);
	private boolean runningTask = true;
	
	public BusInfoProcessor() {
		
	}
	
	public boolean addStationReport(BusStationReportInfo stationReport) {
		try {
			return stationReports.offer(stationReport, 3, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			return false;
		}
	}
	
	@Override
	public void run() {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		while(runningTask) {
			BusStationReportInfo stationReport = null;
			try {
				stationReport = stationReports.take();
				StringBuffer businfos = new StringBuffer();
				businfos.append(sdf.format(new Date())).append(" ");
				businfos.append("线路").append(stationReport.getBusLineName()).append("更新:");
				switch (stationReport.getReportType()) {
				case 0x01:
					businfos.append("到站");
					break;
				case 0x02:
					businfos.append("预测报站");
					break;
				}
				businfos.append("，车号").append(stationReport.getBusno());
				businfos.append("，站牌号：").append(stationReport.getBusStationNo());
				businfos.append("，该车次为");
				switch (stationReport.getBusLineDirection()) {
				case 0x04:
					businfos.append("上行(主站到副站)");
					break;
				case 0x05:
					businfos.append("下行(副站到主站)");
					break;
				case 0x06:
					businfos.append("环行");
					break;
				}
				businfos.append(",最近公交车距本站的站数").append(stationReport.getNearestBusApart());
				logger.info(businfos.toString());
				PushMqttMessage message = new PushMqttMessage();
				message.setTopic(Application.mqttServerTask.getNotifyTopicPre()+"busline/"+stationReport.getBusLineName()
					+"/"+stationReport.getBusLineDirection()
					+"/"+stationReport.getBusStationNo());
				message.setPayload(businfos.toString());
				message.setQos(0);
				if(!Application.mqttServerTask.pushMessageAsync(message)) {
					Application.mqttServerTask.pushMessage(message);
				}
			} catch (InterruptedException e) {
			}
		}
	};
}
