package com.bsb.hike.offline;

import com.google.gson.annotations.Expose;

/**
 * 
 * @author himanshu
 *	Server side configurable paramters in case of Offline Messaging
 */
public class OfflineParameters
{
	@Expose
	boolean feature = false;

	@Expose
	int gtto = 20000;  // 20 sec

	@Expose
	int scrto = 60000*2; //2 min
	
	@Expose
	int connto = 60000; //1 min 
	
	@Expose
	int portno = OfflineConstants.PORT_PING;
	

	public boolean isOfflineEnabled()
	{
		return feature;
	}

	public int getHeartBeatTimeout()
	{
		return gtto;
	}

	public int getKeepAliveScreenTimeout()
	{
		return scrto;
	}
	
	public int getConnectionTimeout()
	{
		return connto;
	}
	
	public int getPortNo()
	{
		return portno;
	}
	
	
}
