package com.bus.server.rpc;

import org.apache.log4j.Logger;

import com.bus.domain.RPCMessage;
import com.bus.domain.Utils.JsonUtils;
import com.bus.server.rpc.impl.BusInfoImpl;

public class BusProcessMethod extends AbstractDataProcessMethod {
	private Logger logger = Logger.getLogger(getClass().getSimpleName());

	@Override
	public String processData(String from, String data) {
		String result = null;
		RPCMessage request = (RPCMessage) JsonUtils.jsonToObj(data, RPCMessage.class);
		if(request == null) {
			RPCMessage resultMsg = new RPCMessage();
			resultMsg.setRpccode(-99);
			resultMsg.setMessage("数据格式错误");
			result = JsonUtils.objToJson(resultMsg);
		}
		if(request.getMessage().equals("get_bus_by_name")) {
			RPCMessage busLineList = new BusInfoImpl().queryBusLineByName(request.getDataContent());
			result = JsonUtils.objToJson(busLineList);
		}
		return result==null?"":result;
	}

}
