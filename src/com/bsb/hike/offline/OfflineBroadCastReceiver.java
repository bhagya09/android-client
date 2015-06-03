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
	        	wifiCallBack.onRequestPeers();
	        }      
	        else if(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action))
	        {
	            int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE,10000);
	            wifiCallBack.onDiscoveryStateChanged();
	        }
	        else if(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action))
	        {
	        	wifiCallBack.onScanResultAvailable();
	        }
	        else if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action))
	        {
	         	NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
	         	if(netInfo!=null && netInfo.getDetailedState().equals(NetworkInfo.DetailedState.CONNECTED))
        		{
	         		wifiCallBack.checkConnectedNetwork(); 
        		}
	        }
	        
	}
}
