package com.bus.server;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.RejectedExecutionException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.bus.domain.Utils.Utils;
import com.bus.server.cotrolcenter.BusControlCenterServer;
import com.bus.server.cotrolcenter.StationReportHandler;
import com.bus.server.database.DbConnectionManager;
import com.bus.server.mqtt.MqttServerTask;
import com.bus.server.rpc.BusProcessMethod;

/**
 * 
 */
public class BusServer {

	public static void main(String[] args) {
		Logger logger = Logger.getLogger(BusServer.class);
		logger.info("the server initializing...");
		Application.appHomePath = Utils.getAppHomePath();
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
			
			Application.loadDbConfig(Application.appConfPath + "db.properties");
			Connection connection = DbConnectionManager.getConnection();
			DbConnectionManager.closeConnection(connection);
			
			Application.classRpcProcessMethod = BusProcessMethod.class;
			
			Application.mqttServerTask = new MqttServerTask(Application.appConfPath + "mqtt.properties",
					Application.appConfPath + "ssl.properties");
			Application.serverCachePool.execute(Application.mqttServerTask);
			
			if(!Application.mqttServerTask.waitLoginComplete()) {
				throw new Exception("the mqtt server login failed.");
			}
			
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
			logger.error(Utils.getExceptionInfo(e));
		} catch (SQLException e1) {
			logger.info("init database connection failed");
			logger.error(Utils.getExceptionInfo(e1));
		} catch (RejectedExecutionException e) {
			logger.info("there are not enough system resources available to run");
			logger.error("there are not enough system resources available to run\n"+Utils.getExceptionInfo(e));
			Application.serverCachePool.shutdownNow();
		} catch (Exception e) {
			logger.error(Utils.getExceptionInfo(e));
			Application.serverCachePool.shutdownNow();
		}
	}

}
