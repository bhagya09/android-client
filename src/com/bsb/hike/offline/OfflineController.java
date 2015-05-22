package com.bsb.hike.offline;

import com.bsb.hike.models.ConvMessage;

public class OfflineController
{

	private IOfflineCallbacks offlineListener=null;
	
	public OfflineController(IOfflineCallbacks listener)
	{
		this.offlineListener=listener;
	}
	
	public void startScanning()
	{
		
	}
	
	public void stopScanning()
	{
		
	}
	
	public String getConnectedDevice()
	{
		return null;
	}
	
	public void sendMessage(ConvMessage convMessage)
	{
		
	}
	
	public boolean isConnected()
	{
		return false;
	}
	
	public void shutDown()
	{
		
	}
}
