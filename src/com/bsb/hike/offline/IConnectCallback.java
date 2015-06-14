package com.bsb.hike.offline;

public interface IConnectCallback
{

	public void onConnect();
	
	public void onDisconnect(OfflineException e);
}
