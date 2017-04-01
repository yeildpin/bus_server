package com.bus.server.rpc;

import com.bus.domain.RPCMessage;

public interface IBusInfo {
	public RPCMessage queryBusLineByName(String busLineName);
}
