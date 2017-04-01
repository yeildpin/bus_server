package com.bus.server.cotrolcenter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.RejectedExecutionException;

import org.apache.log4j.Logger;

import com.bus.domain.Utils.Utils;
import com.bus.server.Application;

public class BusControlCenterServer extends Thread {
	private Logger logger = Logger.getLogger(getClass().getSimpleName());
	private boolean runningServer = true;
	private ServerSocket mSocketServer = null;
	private String mHost;
	private int mPort;
	private SocketAddress mAddress = null;
	
	public BusControlCenterServer(String host, int port) {
		this.mHost = host;
		this.mPort = port;
	}
	
	@Override
	public void run() {
		mAddress = new InetSocketAddress(mHost, mPort);
		while(runningServer) {
			try {
				if(mSocketServer == null) {
					mSocketServer = new ServerSocket();
				}
				mSocketServer.setReuseAddress(true);
				mSocketServer.bind(mAddress);
				logger.info("the bus control center server starting");
				while (true) {
					Socket client = mSocketServer.accept();
					try {
						Application.serverCachePool.execute(new BusControlCenterHandler(client));
					} catch (RejectedExecutionException e) {
						logger.error("there are not enough system resources available to run");
						try {
							client.close();
						} catch (Exception e2) { }
					}
				}
			} catch (IOException e) {
				logger.error(Utils.getExceptionInfo(e));
				if(mSocketServer != null) {
					try {
						mSocketServer.close();
					} catch (Exception e2) { }
				}
				try {
					Thread.sleep(2*1000);
				} catch (Exception e2) { }
			}
			logger.info("the bus control center server stoped,trying to restart...");
		}
	}
}
