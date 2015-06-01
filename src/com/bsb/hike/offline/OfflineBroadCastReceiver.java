package com.bsb.hike.offline;

import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;

import com.bsb.hike.utils.Logger;

public class OfflineBroadCastReceiver extends BroadcastReceiver
{

	private static final String TAG="OfflineBroadCastReceived";
	
	IWIfiReceiverCallback wifiCallBack;
	
	public OfflineBroadCastReceiver(IWIfiReceiverCallback callback)
	{
		this.wifiCallBack=callback;
	}
	@Override
	public void onReceive(Context context, Intent intent)
	{
		 String action = intent.getAction();    
	        if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) 
	        {
//	        	if(deviceActionListener!=null)	request Peers
//	        		deviceActionListener.requestPeers();
	        	wifiCallBack.onRequestPeers();
	        }      
	        else if(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action))
	        {
	            int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE,10000);
	            Logger.d(TAG, "State-> " + state);
	        }
	        else if(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action))
	        {
	        	//Scan Results Avaibale
//	        	scanResultsAvailable = true;
//	        	if(deviceActionListener!=null)
//	        	{
//		        	HashMap<String, ScanResult> currentNearbyNetworks = deviceActionListener.getDistinctWifiNetworks();
//		            deviceActionListener.updateCurrentNearByDevices(currentNearbyNetworks);
//		        	Logger.d(TAG, "Scan results available");
//	        	}
	        	//wifiCallBack.onScanResultAvailable(currentNearByNetwork);
	        }
	        else if(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION.equals(action))
	        {
	        	Logger.d(TAG, "SUPPLICANT_CONNECTION_CHANGE_ACTION");
	        }
	        else if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action))
	        {
	         	NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
	            if( netInfo.isConnected() )
	            {
	                Logger.d(TAG, "Offline mode is connected");
	            }   
	            else
	            {
//	            	if (OfflineManager.getInstance().)
//	            	{
//	            		Logger.d(TAG, "Offline mode is disconnected");
//	            	}
	            }
	        }
	        
	}

}
