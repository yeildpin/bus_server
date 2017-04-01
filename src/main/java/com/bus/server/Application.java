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

import com.bus.domain.Utils.JsonUtils;
import com.bus.server.mqtt.MqttServerTask;
import com.bus.server.processor.BusInfoProcessor;
import com.bus.server.processor.BusReportProcessor;

public class Application {
	private static Logger logger = Logger.getLogger(Application.class);
	public static String appHomePath;
	public static String appConfPath;
	private static Properties appConfig = null;
    private static Properties dbProperties = null;
    public static String dbPoolUsed="proxool";
    public static String dbNameDefault="postgre";
	
    /** 运行服务端主要进程 */
	public static ExecutorService serverCachePool = Executors.newCachedThreadPool();
	/** 运行公交线路运算进程 */
	public static ExecutorService busProcessorPool = null;
	/** 运行客户端消息处理进程，并发数受限 */
	public static ExecutorService clientProcessorPool = null;
	
	public static MqttServerTask mqttServerTask = null;
	
	public static ConcurrentHashMap<String, BusInfoProcessor> busLineProcessor = new ConcurrentHashMap<String, BusInfoProcessor>();
	public static ConcurrentHashMap<String, BusReportProcessor> busReportProcessor = new ConcurrentHashMap<String, BusReportProcessor>();

	/** 客户端发送的RPC消息数据处理，在程序初始化时注册 */
	public static Class<?> classRpcProcessMethod = null;

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
	
	public static int getAppConf(String key, int defaultValue) {
		return JsonUtils.parseInt(appConfig.getProperty(key, ""), defaultValue);
	}
	
    public static void loadDbConfig(String confPath) throws IOException {
    	InputStream confInputStream = null;
		try {
			File confFile = new File(confPath);
			confInputStream = new FileInputStream(confFile);
			dbProperties = new Properties();
			dbProperties.load(confInputStream);
			dbPoolUsed = dbProperties.getProperty("database.pool", "proxool");
			dbNameDefault = dbProperties.getProperty("database.default", "postgre");
		} catch (IOException e) {
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
    
    public static String getDbConf(String name) {
        return getDbConfWithName(dbNameDefault, name);
    }
    
    public static String getDbConf(String name, String defaultValue) {
        return getDbConfWithName(dbNameDefault, name, defaultValue);
    }
    
    public static String getDbConfWithName(String dbname, String name) {
        return getDbConfWithName(dbname, name, null);
    }
    
    public static String getDbConfWithName(String dbname, String name, String defaultValue) {
        if (dbProperties == null) {
            return defaultValue;
        }
        return dbProperties.getProperty(dbname+"."+name, defaultValue);
    }
    
    public static String getDbPoolConf(String name) {
        return getDbPoolConf(name, null);
    }
    
    public static String getDbPoolConf(String name, String defaultValue) {
        if (dbProperties == null) {
            return defaultValue;
        }
        return dbProperties.getProperty(dbPoolUsed+"."+name, defaultValue);
    }
    public static int getDbPoolConf(String name, int defaultValue) {
        return JsonUtils.parseInt(getDbPoolConf(name), defaultValue);
    }
    public static boolean getDbPoolConf(String name, boolean defaultValue) {
        return JsonUtils.parseBoolean(getDbPoolConf(name), defaultValue);
    }
}
