package com.bus.server.database.c3p0;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.bus.server.Application;
import com.bus.server.database.ConnectionProvider;
import com.bus.server.database.DbConnectionManager;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

public class C3p0ConnectionProvider implements ConnectionProvider {
	private Logger logger = Logger.getLogger(getClass().getSimpleName());
	private ComboPooledDataSource pooledDataSource;
    private String dbname;
	
	public C3p0ConnectionProvider() {
		initDatasource();
	}
	
	public C3p0ConnectionProvider(String dbName) {
		setDbName(dbName);
		initDatasource();
	}
    
    public void setDbName(String dbName) {
    	this.dbname = dbName;
    }

	@Override
	public boolean isPooled() {
		return true;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return pooledDataSource.getConnection();
	}

	@Override
	public void start() {
		if(pooledDataSource == null) {
			initDatasource();
		}
	}

	@Override
	public void restart() {
		
	}

	@Override
	public void destroy() {
		try {
			pooledDataSource.close();
			DataSources.destroy(pooledDataSource);
		} catch (SQLException e) {
		}
		pooledDataSource = null;
	}
	
	public void initDatasource() {
		pooledDataSource = new ComboPooledDataSource();
		String driverClass = Application.getDbConf("driver");
		try {
			Class.forName(driverClass);
			pooledDataSource.setDriverClass(driverClass);
		} catch (ClassNotFoundException e2) {
			logger.error(e2.getMessage()+" could not load, using default driver");
		} catch (PropertyVetoException e) {
		}
		pooledDataSource.setUser(Application.getDbConfWithName(dbname, "username"));
		pooledDataSource.setPassword(Application.getDbConfWithName(dbname, "password"));
		pooledDataSource.setJdbcUrl(Application.getDbConfWithName(dbname, "serverURL"));
		pooledDataSource.setPreferredTestQuery(Application.getDbConfWithName(dbname, "testSQL", DbConnectionManager.getTestSQL(driverClass)));
		
		pooledDataSource.setAcquireIncrement(Application.getDbPoolConf("acquireIncrement", 1));
		pooledDataSource.setAcquireRetryAttempts(Application.getDbPoolConf("acquireRetryAttempts", 3));
		pooledDataSource.setAcquireRetryDelay(Application.getDbPoolConf("acquireRetryDelay", 3));
		pooledDataSource.setInitialPoolSize(Application.getDbPoolConf("initialPoolSize", 3));
		pooledDataSource.setMinPoolSize(Application.getDbPoolConf("minPoolSize", 3));
		pooledDataSource.setMaxPoolSize(Application.getDbPoolConf("maxPoolSize", 15));
		pooledDataSource.setMaxStatements(Application.getDbPoolConf("maxStatements", 0));
		pooledDataSource.setMaxStatementsPerConnection(Application.getDbPoolConf("maxStatementsPerConnection", 0));
		pooledDataSource.setMaxIdleTime(Application.getDbPoolConf("maxIdleTime", 3600));
		pooledDataSource.setIdleConnectionTestPeriod(Application.getDbPoolConf("idleConnectionTestPeriod", 60));
		pooledDataSource.setTestConnectionOnCheckout(Application.getDbPoolConf("testConnectionOnCheckout", false));
		pooledDataSource.setTestConnectionOnCheckin(Application.getDbPoolConf("testConnectionOnCheckin", false));
		pooledDataSource.setCheckoutTimeout(Application.getDbPoolConf("checkoutTimeout", 5));
		pooledDataSource.setUnreturnedConnectionTimeout(Application.getDbPoolConf("unreturnedConnectionTimeout", pooledDataSource.getMaxIdleTime()+10));
		pooledDataSource.setDebugUnreturnedConnectionStackTraces(Application.getDbPoolConf("debugUnreturnedConnectionStackTraces", false));
		pooledDataSource.setNumHelperThreads(Application.getDbPoolConf("numHelperThreads", 3));
		
		pooledDataSource.setConnectionCustomizerClassName(C3p0ConnectionCustomizer.class.getName());
    }
	
	@Override
	public String toString() {
		if(pooledDataSource == null) {
			return super.toString();
		}
		try {
			return "TotalConnectionCount-->"+pooledDataSource.getNumConnectionsAllUsers() + ",AvailableConnectionCount-->"
			+ pooledDataSource.getNumIdleConnectionsAllUsers() + ",ActiveConnectionCount-->" + pooledDataSource.getNumBusyConnectionsAllUsers();
		} catch (SQLException e) {
			return super.toString();
		}
	}
}
