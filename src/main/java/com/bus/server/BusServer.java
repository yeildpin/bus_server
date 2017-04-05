package com.bus.server;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.RejectedExecutionException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.bus.server.cotrolcenter.BusControlCenterServer;
import com.bus.server.cotrolcenter.StationReportHandler;
import com.bus.server.processor.MqttMessageProcessor;
import com.yeild.common.Utils.CommonUtils;
import com.yeild.common.dbtools.database.DbConnectionManager;
import com.yeild.mqtt.MqttServerTask;

/**
 * 
 */
public class BusServer {

	public static void main(String[] args) {
		Logger logger = Logger.getLogger(BusServer.class);
		logger.info("the server initializing...");
		Application.appHomePath = Application.getAppHomePath();
		Application.appConfPath = Application.appHomePath + "/conf/";
		if(Application.appHomePath == null || !new File(Application.appConfPath).exists()) {
			logger.error("the app home path not found");
			logger.debug("if you run on Eclipse with Java Applicaion,please add the VM arguments in the Run Configurations -> Arguments:\n"
					+ "-DworkHome=\"${workspace_loc:bus_server}/target/bus_server\"");
			return;
		}
		String log4jConfig = Application.appConfPath + "log4j.properties";
		if (new File(log4jConfig).exists()) {
			PropertyConfigurator.configure(log4jConfig);
			logger.info("log4j initialize success");
		} else {
			logger.info("log4j config file not found,using default");
		}
		try {
			Application.loadAppConfig(Application.appConfPath + "server.properties");
			
			DbConnectionManager.initDBConf(Application.appConfPath);
			DbConnectionManager.initDatabase();
			
			Application.mqttServerTask = new MqttServerTask(Application.appConfPath);
			Application.mqttServerTask.addMqttConnectorListener(Application.mqttListener);
			Application.serverCachePool.execute(Application.mqttServerTask);
			
			if(!Application.mqttServerTask.waitLoginComplete()) {
				throw new Exception("the mqtt server login failed.");
			}

			Application.mqttMessageProcessor = new MqttMessageProcessor();
			Application.serverCachePool.execute(Application.mqttMessageProcessor);
			
			BusControlCenterServer controlCenterServer = new BusControlCenterServer(
					Application.getAppConf("bus.controlcenter.host", "127.0.0.1"),
					Application.getAppConf("bus.controlcenter.port", 9888));
			Application.serverCachePool.execute(controlCenterServer);
			
			StationReportHandler reportHandler = new StationReportHandler(
					Application.getAppConf("bus.stationreport.host", "127.0.0.1"),
					Application.getAppConf("bus.stationreport.port", 9888));
			Application.serverCachePool.execute(reportHandler);
			
			logger.info("the server is starting");
		} catch (IOException e) {
			logger.info("load config file failed");
			logger.error(CommonUtils.getExceptionInfo(e));
		} catch (SQLException e1) {
			logger.info("init database connection failed");
			logger.error(CommonUtils.getExceptionInfo(e1));
		} catch (RejectedExecutionException e) {
			logger.info("there are not enough system resources available to run");
			logger.error("there are not enough system resources available to run\n"+CommonUtils.getExceptionInfo(e));
			Application.serverCachePool.shutdownNow();
		} catch (Exception e) {
			logger.error(CommonUtils.getExceptionInfo(e));
			Application.serverCachePool.shutdownNow();
		}
	}

}
