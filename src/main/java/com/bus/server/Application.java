package com.bus.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.bus.server.processor.BusInfoProcessor;
import com.bus.server.processor.BusReportProcessor;
import com.yeild.common.Utils.ConvertUtils;
import com.yeild.mqtt.MqttServerTask;

public class Application {
	private static Logger logger = Logger.getLogger(Application.class);
	public static String appHomePath;
	public static String appConfPath;
	private static Properties appConfig = null;
	
    /** 运行服务端主要进程 */
	public static ExecutorService serverCachePool = Executors.newCachedThreadPool();
	/** 运行公交线路运算进程 */
	public static ExecutorService busProcessorPool = null;
	/** 运行客户端消息处理进程，并发数受限 */
	public static ExecutorService clientProcessorPool = null;
	
	public static MqttServerTask mqttServerTask = null;
	
	public static ConcurrentHashMap<String, BusInfoProcessor> busLineProcessor = new ConcurrentHashMap<String, BusInfoProcessor>();
	public static ConcurrentHashMap<String, BusReportProcessor> busReportProcessor = new ConcurrentHashMap<String, BusReportProcessor>();

	public static boolean loadAppConfig(String confPath) throws IOException {
		InputStream confInputStream = null;
		try {
			File confFile = new File(confPath);
			confInputStream = new FileInputStream(confFile);
			appConfig = new Properties();
			appConfig.load(confInputStream);
			busProcessorPool = Executors.newFixedThreadPool(getAppConf("maxBusLineCount", 100));
			clientProcessorPool = Executors.newFixedThreadPool(getAppConf("maxProcessorCount", 100));
			return true;
		} catch (IOException e) {
			logger.debug("the app conf file does not exits");
			 throw e;
		} finally {
			if (confInputStream != null) {
				try {
					confInputStream.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	public static String getAppConf(String key) {
		return appConfig.getProperty(key);
	}
	
	public static String getAppConf(String key, String defaultValue) {
		return appConfig.getProperty(key, defaultValue);
	}
	
	public static String getAppHomePath() {
		String appPath = System.getProperty("workHome");
		File appHomeFile = null;
		if(appPath != null) {
			appHomeFile = new File(appPath);
			if(appHomeFile.exists()) {
				return appPath;
			}
		}
		appHomeFile = new File("../");
		try {
			return appHomeFile.getCanonicalFile().getAbsolutePath();
		} catch (IOException e) {
		}
		return null;
	}
	
	public static int getAppConf(String key, int defaultValue) {
		return ConvertUtils.parseInt(appConfig.getProperty(key, ""), defaultValue);
	}
}
