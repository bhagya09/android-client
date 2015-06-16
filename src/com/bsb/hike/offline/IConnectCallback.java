package com.bsb.hike.offline;

/**
 * 
 * @author himanshu
 *	Interface to be called when the client/server are succesfully connected to each other
 */
public interface IConnectCallback
{

	public void onConnect();
	
	public void onDisconnect(OfflineException e);
}
