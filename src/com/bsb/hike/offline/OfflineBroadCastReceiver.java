package com.bsb.hike.offline;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Logger;

public class OfflineBroadCastReceiver extends BroadcastReceiver
{

	private static final String TAG = "OfflineBroadCastReceived";

	IWIfiReceiverCallback wifiCallBack;

	public OfflineBroadCastReceiver(IWIfiReceiverCallback callback)
	{
		this.wifiCallBack = callback;
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		String action = intent.getAction();
		if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action))
		{
			wifiCallBack.onRequestPeers();
		}
		else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action))
		{
			int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, 10000);
			if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED)
			{
				wifiCallBack.onDiscoveryStarted();
			}
			else
			{
				wifiCallBack.onDiscoveryStopped();
			}

		}
		else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action))
		{
			wifiCallBack.onScanResultAvailable();
		}
		else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action))
		{
			NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			WifiManager wifiManager = (WifiManager) HikeMessengerApp.getInstance().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

			String ssid = wifiManager.getConnectionInfo().getSSID();
			if (!TextUtils.isEmpty(ssid))
			{
				Logger.d(TAG, "OfflineBroadCast ssid: " + ssid);
				if (ssid.length() > 2)
					ssid = ssid.substring(1, ssid.length() - 1);

				if (netInfo != null && netInfo.getDetailedState().equals(NetworkInfo.DetailedState.CONNECTED) && OfflineUtils.isOfflineSsid(ssid) && netInfo.isConnected())
				{
					wifiCallBack.checkConnectedNetwork();
				}
			}
		}
		else if(WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action))
		{
			int extraWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE ,
				    WifiManager.WIFI_STATE_UNKNOWN);
				 
				  switch(extraWifiState){
				  case WifiManager.WIFI_STATE_DISABLED:
				   break;
				  case WifiManager.WIFI_STATE_DISABLING:
				   break;
				  case WifiManager.WIFI_STATE_ENABLED:
				   break;
				  case WifiManager.WIFI_STATE_ENABLING:
				   break;
				  case WifiManager.WIFI_STATE_UNKNOWN:
				  break;
				  }
			
		}
		else if(OfflineConstants.WIFI_HOTSPOT_STATE.equals(action))
		{
			int state = intent.getIntExtra("wifi_state", 
                    0); 	 
				  switch(state){
				  case OfflineConstants.WIFI_HOTSPOT_STATE_DISABLED:
				   break;
				  case OfflineConstants.WIFI_HOTSPOT_STATE_DISABLING:
				   break;
				  case OfflineConstants.WIFI_HOTSPOT_STATE_ENABLED:
				   break;
				  case OfflineConstants.WIFI_HOTSPOT_STATE_ENABLING:
				   break;
				  case OfflineConstants.WIFI_HOTSPOT_STATE_UNKNOWN:
				  break;
				  }
			Logger.d(TAG,"Wifi Hotspot State " + state );
		}

	}
}
