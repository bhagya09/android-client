package com.bsb.hike.offline;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Logger;

public class ConnectionManager implements ChannelListener
{
	private final String TAG = "WifiP2pConnectionManager";
	private Context context;
	private WifiP2pManager manager;
	private WifiManager wifiManager;
	private Channel channel;
    private SharedPreferences settings;
    private String myMsisdn;
    private boolean retryChannel = false;
    private PeerListListener peerListListener;
  //  private WifiScanResultListener wifiScanResultListener;
   
//    public String getMsisdn()
//    {
//    	return this.myMsisdn;
//    }
//	public ConnectionManager()
//	{
//		this.context = HikeMessengerApp.getInstance().getApplicationContext();
//		initialise(context);
//	}
//	
//	private void initialise(Context context)
//	{
//        manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
//        channel = manager.initialize(context, context.getMainLooper(), this);
//        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//        if(wifiManager.isWifiEnabled() == false  && !OfflineManager.getInstance().isHotspotCreated(wifiManager))
//        	wifiManager.setWifiEnabled(true);
//        setDeviceNameAsMsisdn();   
//	}
//	
//	private void setDeviceNameAsMsisdn() {
//		/*
//         * Setting device Name to msisdn
//         */
//        try {
//        	settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
//            myMsisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
//			Method m = manager.getClass().getMethod("setDeviceName", new Class[]{channel.getClass(), String.class,
//						WifiP2pManager.ActionListener.class});
//			m.invoke(manager, channel, myMsisdn, new WifiP2pManager.ActionListener() {
//				
//				@Override
//				public void onSuccess() {
//					Log.d(TAG, "Device Name changed to " + myMsisdn);
//				}
//				@Override
//				public void onFailure(int reason) {
//					Logger.e(TAG, "Unable to set device name as msisdn");
//				}
//			});
//		} catch (NoSuchMethodException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public Boolean closeHotspot(String deviceName)
//	{
//		return OfflineManager.getInstance().closeHotspot(context, deviceName);
//	}
//	
//	public Boolean connectToHotspot(String ssid) 
//	{	
//		return OfflineManager.getInstance().connectToHotspot(ssid,wifiManager);
//	}
//	
//	public HashMap<String, ScanResult> getDistinctWifiNetworks()
//	{
//		//wifiManager.startScan();
//   	 	List<ScanResult> results = wifiManager.getScanResults();
//	   	HashMap<String,ScanResult> distinctNetworks = new HashMap<String, ScanResult>();
//	   	for(ScanResult scanResult :  results)
//	   	{
//	   		boolean cond = !(TextUtils.isEmpty(scanResult.SSID));
//	   		scanResult.SSID = OfflineManager.getInstance().decodeSSID(scanResult.SSID);
//	   		if(cond && scanResult.SSID.contains(myMsisdn))
//	   		{
//		   		if(!distinctNetworks.containsKey(scanResult))
//		   		{
//		   			distinctNetworks.put(scanResult.SSID, scanResult); 
//		   		}
//		   		else
//		   		{
//		   			if(WifiManager.compareSignalLevel(scanResult.level, distinctNetworks.get(scanResult.SSID).level)>0)
//		   			{
//		   				distinctNetworks.put(scanResult.SSID, scanResult);
//		   			}
//		   		}
//	   		}
//	   	}
//	   	return distinctNetworks;
//   }
//	
//	public void cancelConnect()
//	{
//		manager.cancelConnect(channel, new ActionListener() {
//        	
//            @Override
//            public void onSuccess() {
//                //wifiP2pConnectionManagerListener.cancelConnectSuccess();
//            }
//
//            @Override
//            public void onFailure(int reasonCode) {
//                //wifiP2pConnectionManagerListener.cancelConnectFailure(reasonCode);
//            }
//        });
//	}
//	
//	public void enableDiscovery()
//	{
//		if (!wifiManager.isWifiEnabled()) {
//			//wifiManager.setWifiEnabled(true);
//            //wifiP2pConnectionManagerListener.notifyWifiNotEnabled();
//        }
//		manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
//        	
//            @Override
//            public void onSuccess() { 
//            	//wifiP2pConnectionManagerListener.discoverySuccess();
//            	Logger.d(TAG, "Wifi Direct Discovery Enabled");
//            }
//
//            @Override
//            public void onFailure(int reasonCode) {
//            	//wifiP2pConnectionManagerListener.discoveryFailure(reasonCode);
//            	Logger.d(TAG, "Wifi Direct Discovery FAILED");
//            }
//		});
//	}
//	
//	public void requestPeers()
//	{
//		if (manager != null) {
//            manager.requestPeers(channel, peerListListener);
//		}
//	}
//	
//	
//
//	public void startScan() {
//		boolean wifiScan = wifiManager.startScan();
//		Log.d(TAG, "Wifi Scan returns " + wifiScan);
//	}
//	
//	public void startWifi()
//	{
//		wifiManager.setWifiEnabled(true);
//	}
//	
//	public  void  updateCurrentNearByDevices(HashMap<String,ScanResult>  currentNearbyNetworks)
//	{
//	   wifiScanResultListener.updateCurrentNearByDevices(currentNearbyNetworks);
//	}
//	 
//	public void stopDiscovery() {
//		manager.removeGroup(channel, null);
//		manager.stopPeerDiscovery(channel, new ActionListener() {
//			@Override
//			public void onSuccess() {
//				Log.d(TAG, "stop peer discovery started");
//			}
//			
//			@Override
//			public void onFailure(int reason) {
//				Log.d(TAG, "Stop peer discovery failed");
//			}
//		});
//	}
//	public void stopWifi() 
//	{
//		wifiManager.setWifiEnabled(false);
//	}
//	public boolean isWifiEnabled() {
//		return wifiManager.isWifiEnabled();
//	}
//	
//	
	@Override
	public void onChannelDisconnected() {
		if (manager != null && !retryChannel) {
            retryChannel = true;
            manager.initialize(context, context.getMainLooper(), this);
        } else {
        	//wifiP2pConnectionManagerListener.channelDisconnectedFailure();
        }
		
	}
	 
}
