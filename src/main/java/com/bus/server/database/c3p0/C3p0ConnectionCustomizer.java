package com.bus.server.database.c3p0;

import java.sql.Connection;

import org.apache.log4j.Logger;

import com.mchange.v2.c3p0.AbstractConnectionCustomizer;

public class C3p0ConnectionCustomizer extends AbstractConnectionCustomizer {
	Logger logger = Logger.getLogger(getClass().getSimpleName()); 

	@Override
	public void onAcquire(Connection c, String parentDataSourceIdentityToken) throws Exception {
		super.onAcquire(c, parentDataSourceIdentityToken);
		logger.debug("open "+c+" ["+parentDataSourceIdentityToken+"]");
	}
	
	@Override
	public void onDestroy(Connection c, String parentDataSourceIdentityToken) throws Exception {
		super.onDestroy(c, parentDataSourceIdentityToken);
		logger.debug("destroy "+c+" ["+parentDataSourceIdentityToken+"]");
	}
	
	@Override
	public void onCheckOut(Connection c, String parentDataSourceIdentityToken) throws Exception {
		super.onCheckOut(c, parentDataSourceIdentityToken);
		logger.debug("checkout "+c+" ["+parentDataSourceIdentityToken+"]");
	}
	
	@Override
	public void onCheckIn(Connection c, String parentDataSourceIdentityToken) throws Exception {
		super.onCheckIn(c, parentDataSourceIdentityToken);
		logger.debug("checkin "+c+" ["+parentDataSourceIdentityToken+"]");
	}
}
