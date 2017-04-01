package com.bus.server.rpc.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.bus.domain.RPCMessage;
import com.bus.domain.Utils.JsonUtils;
import com.bus.domain.bus.BusLine;
import com.bus.domain.bus.BusLineList;
import com.bus.domain.bus.BusStation;
import com.bus.server.database.DbConnectionManager;
import com.bus.server.rpc.IBusInfo;

public class BusInfoImpl implements IBusInfo {
	private Logger logger = Logger.getLogger(getClass().getSimpleName());

	@Override
	public RPCMessage queryBusLineByName(String busLineName) {
		RPCMessage result = new RPCMessage();
		result.setRpccode(-1);
		result.setMessage("获取数据失败");
		Connection connection = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql = null;
		try {
			connection = DbConnectionManager.getConnection();
			sql = "select * from tb_linestop t1 where t1.linename=?"
					+ " order by direction,stopsn";
			pstmt = connection.prepareStatement(sql);
			pstmt.setString(1, busLineName);
			rs = pstmt.executeQuery();
			BusLineList busLineList = new BusLineList();
			Map<String, BusLine> busLines = new HashMap<String, BusLine>();
			while(rs.next()) {
				String linename = rs.getString("linename");
				int direction = rs.getInt("direction");
				String key = linename+"##"+direction;
				BusLine busLine = busLines.get(key);
				if(busLine == null) {
					busLine = new BusLine();
					busLine.setLinename(linename);
					busLine.setDirection(direction);
					busLines.put(key, busLine);
				}
				BusStation busStation = new BusStation();
				busStation.setStopno(rs.getInt("stopno"));
				busStation.setStopname(rs.getString("stopname"));
				busStation.setStopsn(rs.getInt("stopsn"));
				busStation.setLat(rs.getDouble("lat"));
				busStation.setLon(rs.getDouble("lon"));
				busLine.addBusStation(busStation);
			}
			for(BusLine busLine : busLines.values()) {
				busLineList.addBusLine(busLine);
			}
			result.setRpccode(1);
			result.setMessage("获取数据成功");
			result.setDataContent(JsonUtils.objToJson(busLineList));
		} catch (SQLException e) {
			logger.error("queryBusLineByName:"+busLineName+"\n"+sql);
			logger.error(JsonUtils.getExceptionInfo(e));;
		} finally {
			DbConnectionManager.closeConnection(rs, pstmt, connection);
		}
		return result;
	}
}
