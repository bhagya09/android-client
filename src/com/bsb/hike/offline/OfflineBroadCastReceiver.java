package com.bsb.hike.offline;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.offline.OfflineConstants.OFFLINE_STATE;
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
			HikeHandlerUtil.getInstance().postRunnable(new Runnable()
			{
				@Override
				public void run()
				{
					wifiCallBack.onScanResultAvailable();
				}
			});
		}
		else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action))
		{
			NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

			String ssid = wifiManager.getConnectionInfo().getSSID();
			if (!TextUtils.isEmpty(ssid))
			{
				Logger.d(TAG, "OfflineBroadCast ssid: " + ssid + ".......Detailed state is " + (netInfo.getDetailedState() == (NetworkInfo.DetailedState.CONNECTED))
						+ "....idOfflineSSID     " + OfflineUtils.isOfflineSsid(ssid) + "     netIfo state is  " + netInfo.isConnected());

				// HTC desire X gives ssid without quotes but all other devices give ssid with quotes
				if (ssid.length() > 2 && ssid.startsWith("\"") && ssid.endsWith("\""))
					ssid = ssid.substring(1, ssid.length() - 1);

				if ((netInfo != null) && (netInfo.getDetailedState() == (NetworkInfo.DetailedState.CONNECTED)) && (OfflineUtils.isOfflineSsid(ssid)))
				{
					Logger.d(TAG, "inconnected");
					wifiCallBack.onConnectionToWifiNetwork();
				}
			}
		}
		else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action))
		{
			int extraWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

			if(OfflineController.getInstance().isHotspotCreated())
			{
				return;
			}
			Logger.d(TAG,"Wifi state change "+extraWifiState);
			switch (extraWifiState)
			{
			case WifiManager.WIFI_STATE_DISABLED:
				if (OfflineController.getInstance().getOfflineState() == OFFLINE_STATE.CONNECTED)
				{
					OfflineController.getInstance().shutdown(new OfflineException(OfflineException.WIFI_CLOSED));
				}
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
		else if (OfflineConstants.WIFI_HOTSPOT_STATE_CHANGE_ACTION.equals(action))
		{
			int state = intent.getIntExtra("wifi_state", 0);
			
			// Doing this check as in some phone isHotSpotCreated was returning false .So adding another defensive check
			
			if(!OfflineController.getInstance().isHotspotCreated() && !OfflineUtils.isHotSpotCreated(OfflineController.getInstance().getConnectedDevice()))
			{
				return;
			}
			switch (state)
			{
			case OfflineConstants.WIFI_HOTSPOT_STATE_DISABLED:
				break;
			case OfflineConstants.WIFI_HOTSPOT_STATE_DISABLING:
				if(OfflineController.getInstance().getOfflineState()==OFFLINE_STATE.CONNECTED)
				{
					OfflineController.getInstance().shutdown(new OfflineException(OfflineException.HOTSPOT_CLOSED));
				}
				break;
			case OfflineConstants.WIFI_HOTSPOT_STATE_ENABLED:
				break;
			case OfflineConstants.WIFI_HOTSPOT_STATE_ENABLING:
				break;
			case OfflineConstants.WIFI_HOTSPOT_STATE_UNKNOWN:
				break;
			}
			Logger.d(TAG, "Wifi Hotspot State " + state);
		}

	}
}
