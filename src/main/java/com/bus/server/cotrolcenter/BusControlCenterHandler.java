package com.bus.server.cotrolcenter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.bus.domain.Utils.JsonUtils;
import com.bus.domain.tcp.TcpPackageContent;
import com.bus.domain.tcp.TcpPackageKeepAlive;
import com.bus.domain.tcp.TcpPackageProcesser;
import com.bus.domain.tcp.TcpPackageStationReport;
import com.bus.server.Application;
import com.bus.server.processor.BusReportProcessor;

public class BusControlCenterHandler extends Thread {
	private Logger logger = Logger.getLogger(getClass().getSimpleName());
	private boolean runningTask;
	private Socket controlCenterSocket = null;
	private InputStream mSocketInput = null;
	private OutputStream mSocketOutput = null;
	/** 已接收数据缓冲 */
	private List<Byte> mDataReceived = null;
	/** 已解析的完整数据包 */
	private List<List<Byte>> mDataReadeds = null;
	private Byte lastestReceivedByte = null;
	/** 上次心跳包发送时间 */
	private long latestKeepAlive = 0;
	private int keepAliveIntervel = 80*1000;
	/** 心跳包等待回复的时间，-1表示已收到 */
	private long keepAliveWaiting = -1;
	private TcpPackageKeepAlive packageKeepAlive = null;
	
	public BusControlCenterHandler(Socket socket) {
		runningTask = true;
		this.controlCenterSocket = socket;
	}
	
	public void stopTask() {
		runningTask = false;
	}
	
	@Override
	public void run() {
		int retryCount = 0;
		File recvFile = new File(Application.appHomePath,"recvbuffer_"+this.getId()+".txt");
		FileOutputStream recvOutputStream = null;
		while(runningTask) {
			try {
                mSocketInput = controlCenterSocket.getInputStream();
                mSocketOutput = controlCenterSocket.getOutputStream();
        		if(!recvFile.exists()) {
        			recvFile.createNewFile();
        		}
                recvOutputStream = new FileOutputStream(recvFile);
                retryCount = 0;
				logger.info("Bus control center["+controlCenterSocket.getInetAddress().getHostName()+"] connected[Task ID:"+this.getId()+"]");
				latestKeepAlive = new Date().getTime();
                byte[] buffer = new byte[256];
				while(true) {
	                int bytes = mSocketInput.read(buffer);
	                if(bytes < 1) {
	                	Thread.sleep(100);
	                	continue;
	                }
	                recvOutputStream.write(buffer, 0, bytes);
	                recvOutputStream.flush();
	                processDatas(buffer, bytes);
	                keepAlive();
				}
			} catch (Exception e) {
				logger.debug(JsonUtils.getExceptionInfo(e));
			} finally {
//				if(retryCount < 10) {
//					retryCount ++;
//	                try {
//						Thread.sleep(2000);
//					} catch (InterruptedException e) { }
//					continue;
//				}
				if(controlCenterSocket != null && controlCenterSocket.isConnected()) {
					try {
						controlCenterSocket.close();
					} catch (Exception e2) { }
				}
				if(recvOutputStream != null) {
					try {
						recvOutputStream.close();
					} catch (IOException e) { }
				}
				logger.info("Bus control center["+controlCenterSocket.getInetAddress().getHostName()+"] disconnected[Task ID:"+this.getId()+"]");
				runningTask = false;
			}
		}
	}
	
	private void keepAlive() throws IOException {
		if(packageKeepAlive == null) {
			packageKeepAlive = new TcpPackageKeepAlive();
			packageKeepAlive.headContent.setSourceType(0x03&0xff);
			packageKeepAlive.headContent.setSourceNo(Long.parseLong(controlCenterSocket.getInetAddress().getHostAddress().replaceAll("[^0-9]", "")));
			packageKeepAlive.headContent.setToResponse(0x02&0xff);
			packageKeepAlive.headContent.setPriority(0x04&0xff);
		}
        if(new Date().getTime() - latestKeepAlive > keepAliveIntervel) {
        	if(keepAliveWaiting != -1) {
        		logger.debug("Bus control center last ping timeout");
        	}
        	packageKeepAlive.headContent.setDataNo(packageKeepAlive.headContent.getDataNo()%255+1);
        	packageKeepAlive.headContent.setTcpTime(System.currentTimeMillis());
        	byte[] keepAlivePac = TcpPackageProcesser.combineTcpPackage(packageKeepAlive);
        	if(keepAlivePac != null && keepAlivePac.length > 0) {
	        	mSocketOutput.write(new byte[]{});
	        	mSocketOutput.flush();
	        	latestKeepAlive = new Date().getTime();
	        	keepAliveWaiting = 0;
	        	logger.debug("Bus control center did send ping("+packageKeepAlive.headContent.getDataNo()+"):\n"+JsonUtils.BinaryToHexString(keepAlivePac));
        	}
        	return;
        }
        keepAliveWaiting = new Date().getTime() - latestKeepAlive;
	}
	
	private void resetDataBuffer() {
		if(mDataReceived!= null) {
			mDataReceived.clear();
		}
		mDataReceived = new ArrayList<Byte>();
	}
	
	private void processDatas(byte[] buffer,int length) {
        int totalPackLen = 0;
        for(int i=0; i<length;i++) {
        	byte tb = buffer[i];
        	if(tb == TcpPackageProcesser.TCP_PAC_START) {
    			if(i==0) {
    				if(lastestReceivedByte == null) {
        				continue;
        			} else if (lastestReceivedByte == TcpPackageProcesser.TCP_PAC_END) {
        				resetDataBuffer();
        				totalPackLen = 0;
        			}
    			} else if (buffer[i-1] == TcpPackageProcesser.TCP_PAC_END) {
    				resetDataBuffer();
    				totalPackLen = 0;
    			}
        	}
    		if(mDataReceived == null) {
    			continue;
    		}
    		mDataReceived.add(tb);
    		if(totalPackLen < 1 && mDataReceived.size()>2) {
    			totalPackLen = ((mDataReceived.get(1)&0xff)<<8)|(mDataReceived.get(2)&0xff);
    		}
        	if(tb == TcpPackageProcesser.TCP_PAC_END) {
        		if(mDataReceived.size() < totalPackLen) {
        			continue;
        		}
        		List<Byte> dataReaded = new ArrayList<Byte>(mDataReceived);
        		if(mDataReadeds == null) {
        			mDataReadeds = new ArrayList<List<Byte>>();
        		}
        		mDataReadeds.add(dataReaded);
				resetDataBuffer();
				totalPackLen = 0;
        	}
        }
        lastestReceivedByte = buffer[length-1];
        if(mDataReadeds == null || mDataReadeds.size() < 1) {
        	return;
        }
        Iterator<List<Byte>> iterReads = mDataReadeds.iterator();
        while(iterReads.hasNext()) {
        	List<Byte> tReaded = iterReads.next();
        	TcpPackageContent packageContent = null;
        	try {
        		packageContent = new TcpPackageProcesser(tReaded).bodyContent;
        	} catch (Exception e) {
        		logger.error(JsonUtils.BinaryToHexString(tReaded));
        		logger.error(JsonUtils.getExceptionInfo(e));
            	iterReads.remove();
        		continue;
			}
        	if(packageContent == null || !packageContent.isHasData()
        			|| packageContent.packageHeader.getTpPackLen() != tReaded.size()) {
        		logger.error(Integer.toHexString(lastestReceivedByte&0xff)+":"+JsonUtils.BinaryToHexString(tReaded));
            	iterReads.remove();
            	continue;
        	}
    		switch(packageContent.packageHeader.getTpType()) {
    		case typeKeepAlive:
    			if(packageContent.headContent.getDataNo() != packageKeepAlive.headContent.getDataNo()) {
    				break;
    			}
	        	logger.debug("Bus control center did receive pong("+packageKeepAlive.headContent.getDataNo()+"):\n"+JsonUtils.BinaryToHexString(tReaded));
    			keepAliveWaiting = -1;
    			break;
    		case typeBusArrive:
    			TcpPackageStationReport stationReport = (TcpPackageStationReport) packageContent;
    			BusReportProcessor infoProcessor = Application.busReportProcessor.get(""+stationReport.getSubBusLineNo());
    			if(infoProcessor == null) {
    				infoProcessor = new BusReportProcessor();
    				Application.busReportProcessor.put(""+stationReport.getSubBusLineNo(), infoProcessor);
    				Application.busProcessorPool.execute(infoProcessor);
    			}
    			infoProcessor.addStationReport(stationReport);
    			break;
			default:
				logger.error("not processed package type:"+packageContent.packageHeader.getTpType().name()+"\n"+JsonUtils.BinaryToHexString(tReaded));
				break;
    		}
        	iterReads.remove();
        }
        mDataReadeds.clear();
	}
}
