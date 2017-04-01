package com.bus.server.database.proxool;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.bus.server.database.ConnectionProvider;
import com.bus.server.database.DbConnectionManager;

/**
 * A connection provider for the embedded hsqlDB database. The database file is stored at
 * <tt>home/database</tt>. The log file for this connection provider is stored at
 * <tt>[home]/logs/EmbeddedConnectionProvider.log</tt>, so you should ensure
 * that the <tt>[home]/logs</tt> directory exists.
 *
 * @author Rock
 */
public class EmbeddedConnectionProvider implements ConnectionProvider {
	
	private static final Logger Log = Logger.getLogger(EmbeddedConnectionProvider.class);

    private Properties settings;
    private String serverURL;
    private String driver = "org.hsqldb.jdbcDriver";
    private String proxoolURL;
    private String dbname;

    public EmbeddedConnectionProvider() {
        System.setProperty("org.apache.commons.logging.LogFactory", "com.ethinkman.util.log.util.CommonsLogFactory");
    }
    
    public void setDbName(String dbName) {
    	this.dbname = dbName;
    }

    public boolean isPooled() {
        return true;
    }

    public Connection getConnection() throws SQLException {
        try {
            Class.forName("org.logicalcobwebs.proxool.ProxoolDriver");
            try {
            	DriverManager.getConnection(proxoolURL, settings);
            }
            catch (SQLException se) {
            	System.out.print(se.getMessage());
            }
            return DriverManager.getConnection(proxoolURL, settings);
        }
        catch (ClassNotFoundException e) {
            throw new SQLException("EmbeddedConnectionProvider: Unable to find driver: "+e);
        }
    }

    public void start() {
        File databaseDir = new File(System.getProperty("homeDir"), File.separator + "embedded-db");
        // If the database doesn't exist, create it.
        if (!databaseDir.exists()) {
            databaseDir.mkdirs();
        }

        try {
            serverURL = "jdbc:hsqldb:" + databaseDir.getCanonicalPath() + File.separator + "openfire";
        }
        catch (IOException ioe) {
            Log.error("EmbeddedConnectionProvider: Error starting connection pool: ", ioe);
        }
        proxoolURL = "proxool.openfire:"+driver+":"+serverURL;
        settings = new Properties();
        settings.setProperty("proxool.maximum-connection-count", "25");
        settings.setProperty("proxool.minimum-connection-count", "3");
        settings.setProperty("proxool.maximum-connection-lifetime", Integer.toString((int)(86400000 * 0.5)));
        settings.setProperty("user", "sa");
        settings.setProperty("password", "");
    }

    public void restart() {
        // Kill off pool.
        destroy();
        // Start a new pool.
        start();
    }

    public void destroy() {
        // Shutdown the database.
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = getConnection();
            pstmt = con.prepareStatement("SHUTDOWN");
            pstmt.execute();
        }
        catch (SQLException sqle) {
            Log.error(sqle.getMessage(), sqle);
        }
        finally {
            DbConnectionManager.closeConnection(pstmt, con);
        }
        // Blank out the settings
        settings = null;
    }

    @Override
	public void finalize() throws Throwable {
        super.finalize();
        destroy();
    }
}