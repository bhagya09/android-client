package com.bsb.hike.offline;

import android.os.Message;

import com.bsb.hike.models.ConvMessage;

public class OfflineController
{

	private IOfflineCallbacks offlineListener=null;
	
	OfflineManager offlineManager;
	
	public OfflineController(IOfflineCallbacks listener)
	{
		this.offlineListener=listener;
		offlineManager=OfflineManager.getInstance();
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
	
	/**
	 * Add to Database and send to the recipent.
	 * @param convMessage
	 */
	public void sendMessage(ConvMessage convMessage)
	{
		Message msg = Message.obtain();
		msg.what = OfflineConstants.HandlerConstants.SAVE_MSG_DB;
		msg.obj = convMessage;
		offlineManager.performWorkOnBackEndThread(msg);
		offlineManager.addToTextQueue(convMessage.serialize());
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
