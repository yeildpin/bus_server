package com.bus.server.cotrolcenter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.apache.log4j.Logger;

import com.bus.domain.Utils.JsonUtils;
import com.bus.domain.bus.BusStationReportInfo;
import com.bus.server.Application;
import com.bus.server.processor.BusInfoProcessor;

public class StationReportHandler extends Thread {
	private Logger logger = Logger.getLogger(getClass().getSimpleName());
	private static byte TCP_PAC_START=0x7d;
	private static byte TCP_PAC_END=0x7f;
	private static int totalPackLen = 29;
	private boolean runningTask = true;
	private Socket controlCenterSocket = null;
	private InetSocketAddress mSocketAddress = null;
	private String mHost;
	private int mPort;
	private InputStream mSocketInput = null;
	private OutputStream mSocketOutput = null;
	/** 已接收数据缓冲 */
	private List<Byte> mDataReceived = null;
	/** 已解析的完整数据包 */
	private List<List<Byte>> mDataReadeds = null;
	private Byte lastestReceivedByte = null;
	
	public StationReportHandler(String host, int port) {
		runningTask = true;
		this.mHost = host;
		this.mPort = port;
		mSocketAddress = new InetSocketAddress(this.mHost, this.mPort);
	}
	
	public void stopTask() {
		runningTask = false;
	}

	@Override
	public void run() {
		File recvDic = new File(Application.appHomePath+"/report");
		if(!recvDic.exists()) {
			recvDic.mkdirs();
		}
		File recvFile = new File(Application.appHomePath+"/report","recvbuffer_"+this.getId()+".txt");
		FileOutputStream recvOutputStream = null;
		while(runningTask) {
			try {
				controlCenterSocket = new Socket();
				controlCenterSocket.setSoTimeout(5*1000);
				controlCenterSocket.setKeepAlive(true);
				controlCenterSocket.connect(mSocketAddress, 15*1000);
                mSocketInput = controlCenterSocket.getInputStream();
                mSocketOutput = controlCenterSocket.getOutputStream();
                logger.info("the bus station report server starting[Task ID:"+this.getId()+"]");
        		if(!recvFile.exists()) {
        			recvFile.createNewFile();
        		}
                recvOutputStream = new FileOutputStream(recvFile);
                byte[] buffer = new byte[512];
                long latestRecvTime = System.currentTimeMillis();
				while(true) {
	                int bytes = -1;
	                try {
	                	bytes = mSocketInput.read(buffer);
	                } catch (SocketTimeoutException e) {
					}
	                if(bytes < 1) {
	                	if(System.currentTimeMillis()-latestRecvTime > 2*60*1000) {
	                		throw new IOException("no data received timed out");
	                	}
	                	Thread.sleep(1);
	                	continue;
	                }
	                latestRecvTime = System.currentTimeMillis();
	                byte []recvStr = (" "+JsonUtils.BinaryToHexString(Arrays.copyOfRange(buffer, 0, bytes))).getBytes();
	                recvOutputStream.write(recvStr, 0, recvStr.length);
	                recvOutputStream.flush();
	                processBuffer(buffer, bytes);
				}
			} catch (IOException e) {
				logger.debug(JsonUtils.getExceptionInfo(e));
			} catch (InterruptedException e) {
			} finally {
				if(controlCenterSocket != null) {
					try {
						controlCenterSocket.close();
					} catch (Exception e2) { }
					controlCenterSocket = null;
				}
				try {
					Thread.sleep(2*1000);
				} catch (InterruptedException e2) { }
			}
			resetDataBuffer();
			lastestReceivedByte = null;
			logger.info("the bus station report server stoped, trying restart[Task ID:"+this.getId()+"]");
		}
	}
	
	private void resetDataBuffer() {
		if(mDataReceived!= null) {
			mDataReceived.clear();
		}
		mDataReceived = new ArrayList<Byte>();
	}
	
	private void processBuffer(byte []buffer, int length) {
        for(int i=0; i<length;i++) {
        	byte tb = buffer[i];
        	if(tb == TCP_PAC_START) {
    			if(i==0) {
    				if(lastestReceivedByte == null ) {
    					continue;
        			} else if (lastestReceivedByte == TCP_PAC_END) {
        				resetDataBuffer();
        			}
    			} else if (buffer[i-1] == TCP_PAC_END) {
    				resetDataBuffer();
    			}
        	}
    		if(mDataReceived == null) {
    			continue;
    		}
    		mDataReceived.add(tb);
        	if(tb == TCP_PAC_END) {
        		if(mDataReceived.size() < 2) {
        			continue;
        		}
        		if(mDataReceived.get(1) != 0x01 && mDataReceived.get(1) != 0x02) {
        			resetDataBuffer();
        			continue;
        		}
        		if(mDataReceived.size() < totalPackLen) {
        			continue;
        		}
        		List<Byte> dataReaded = new ArrayList<Byte>(mDataReceived);
        		if(mDataReadeds == null) {
        			mDataReadeds = new ArrayList<List<Byte>>();
        		}
        		mDataReadeds.add(dataReaded);
        		resetDataBuffer();
        	}
        }
        lastestReceivedByte = buffer[length-1];
        if(mDataReadeds == null || mDataReadeds.size() < 1) {
        	return;
        }

        Iterator<List<Byte>> iterReads = mDataReadeds.iterator();
        while(iterReads.hasNext()) {
        	List<Byte> tReaded = iterReads.next();
        	if(tReaded.size() < totalPackLen) {
        		continue;
        	}
        	BusStationReportInfo reportInfo = new BusStationReportInfo();
        	reportInfo.setReportType(tReaded.get(1));
        	reportInfo.setNearestBusApart(tReaded.get(12)&0xff);
        	try {
        		reportInfo.setBusLineName(new String(JsonUtils.listToArray(tReaded.subList(2, 12)), "UTF-8"));
				reportInfo.setBusno(new String(JsonUtils.listToArray(tReaded.subList(13, 23)), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				logger.error(JsonUtils.getExceptionInfo(e));
			}
        	reportInfo.setBusLineDirection(tReaded.get(23)&0xff);
        	reportInfo.setBusStationNo(JsonUtils.bytesToInt(tReaded.subList(24, 28)));
        	BusInfoProcessor infoProcessor = Application.busLineProcessor.get(""+reportInfo.getBusLineName());
			if(infoProcessor == null) {
				infoProcessor = new BusInfoProcessor();
				Application.busLineProcessor.put(""+reportInfo.getBusLineName(), infoProcessor);
				try {
					Application.busProcessorPool.execute(infoProcessor);
				} catch (RejectedExecutionException e) {
					logger.error(JsonUtils.getExceptionInfo(e));
				}
			}
			infoProcessor.addStationReport(reportInfo);
        	iterReads.remove();
        }
        mDataReadeds.clear();
	}
}
