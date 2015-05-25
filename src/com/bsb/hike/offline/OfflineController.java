package com.bsb.hike.offline;

import com.bsb.hike.models.ConvMessage;

public class OfflineController
{

	private IOfflineCallbacks offlineListener=null;
	
	public OfflineController(IOfflineCallbacks listener)
	{
		this.offlineListener=listener;
	}
	
	public void startScanningWifiDirect()
	{
		
	}
	
	public void stopScanningWifiDirect()
	{
		
	}
	
	public String getConnectedDevice()
	{
		return null;
	}
	
	public void sendMessage(ConvMessage convMessage)
	{
		
	}
	
	public void sendAudioFile()
	{
		
	}
	
	public void sendVideoFile()
	{
		
	}
	
	public void sendRecordingFile()
	{
		
	}
	
	public void sendImage()
	{
		
	}
	
	public void sendFile()
	{
		
	}
	
	public void sendApps()
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
