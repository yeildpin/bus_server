package com.bus.server.processor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.bus.domain.tcp.TcpPackageStationReport;
import com.bus.server.Application;
import com.yeild.mqtt.PushMqttMessage;

public class BusReportProcessor extends Thread {
	private Logger logger = Logger.getLogger(getClass().getSimpleName());
	private LinkedBlockingQueue<TcpPackageStationReport> stationReports = new LinkedBlockingQueue<TcpPackageStationReport>(50);
	private long lastReportTime=0;
	private boolean runningTask = true;
	
	public BusReportProcessor() {
		
	}
	
	public boolean addStationReport(TcpPackageStationReport stationReport) {
		try {
			return stationReports.offer(stationReport, 3, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			return false;
		}
	}
	
	@Override
	public void run() {
		while(runningTask) {
			TcpPackageStationReport stationReport = null;
			try {
				stationReport = stationReports.take();
				if(stationReport.getStationReportTime() < lastReportTime) {
					continue;
				}
				StringBuffer businfos = new StringBuffer();
				businfos.append("线路").append(stationReport.headContent.getSourceLine())
				.append("[子线路号").append(stationReport.getSubBusLineNo())
				.append("]更新:");
				businfos.append("车号").append(stationReport.headContent.getSourceNo());
				switch (stationReport.getStationReportTag()) {
				case 0x01:
					businfos.append("到站");
					break;
				case 0x02:
					businfos.append("离站");
					break;
				}
				businfos.append("，站点编号：").append(stationReport.getCurrentStationNo());
				businfos.append("，此站为");
				switch (stationReport.getStationProperty()) {
				case 0x01:
					businfos.append("始发站(主站)");
					break;
				case 0x02:
					businfos.append("终点站 (副站)");
					break;
				case 0x03:
					businfos.append("中途站");
					break;
				case 0x04:
					businfos.append("虚站");
					break;
				case 0x05:
					businfos.append("拐弯");
					break;
				case 0x06:
					businfos.append("主站上客站");
					break;
				case 0x07:
					businfos.append("主站下客站");
					break;
				case 0x08:
					businfos.append("副站上客站");
					break;
				case 0x09:
					businfos.append("副站下客站");
					break;
				case 0x0A:
					businfos.append("出库");
					break;
				case 0x0B:
					businfos.append("入库");
					break;
				case 0x0C:
					businfos.append("返回站(环形线路的调头站点)");
					break;
				case 0x0D:
					businfos.append("维修厂");
					break;
				}
				businfos.append("，线路类型：");
				switch (stationReport.getBusRouteType()) {
				case 0x01:
					businfos.append("上下行");
					break;
				case 0x02:
					businfos.append("环行");
					break;
				}
				businfos.append("，该车次为");
				switch (stationReport.getBusLineType()) {
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
//				logger.info(businfos.toString());
				PushMqttMessage message = new PushMqttMessage();
				message.setTopic("/server/notify/busline/"+stationReport.headContent.getSourceLine());
				message.setId((int)stationReport.getSubBusLineNo());
				message.setPayload(businfos.toString().getBytes());
				message.setQos(0);
				if(!Application.mqttServerTask.pushMessageAsync(message)) {
					logger.debug(message.getId()+" push failed");
				} else {
					logger.debug(message.getId()+" push success");
				}
				lastReportTime = stationReport.getStationReportTime();
			} catch (InterruptedException e) {
			}
		}
	};
}
