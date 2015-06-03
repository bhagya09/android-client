package com.bsb.hike.offline;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Logger;

public class ConnectionManager implements ChannelListener
{
	public static final ConnectionManager _instance = new ConnectionManager();
	private boolean retryChannel = false;
	private Context context;
	private WifiManager wifiManager;
	private WifiP2pManager wifiP2pManager;
	private Channel channel;
	private SharedPreferences settings;
    private String myMsisdn =null;
    private String TAG = ConnectionManager.class.getName();
    private WifiConfiguration prevConfig;
    private boolean isHotSpotCreated  = false;
	private ConnectionManager()
	{
    	init();
	}
	
	public static ConnectionManager getInstance()
	{
		return _instance;
	}
	
	private void init()
	{
		context =HikeMessengerApp.getInstance().getApplicationContext();
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(context, context.getMainLooper(), this);
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
        myMsisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
	}
	
	public void setDeviceNameAsMsisdn() {
		/*
         * Setting device Name to msisdn
         */
        try {
			Method m = wifiP2pManager.getClass().getMethod("setDeviceName", new Class[]{channel.getClass(), String.class,
						WifiP2pManager.ActionListener.class});
			m.invoke(wifiP2pManager, channel, myMsisdn, new WifiP2pManager.ActionListener() {
				@Override
				public void onSuccess() {
					Log.d(TAG, "Device Name changed to " + myMsisdn);
				}
				@Override
				public void onFailure(int reason) {
					Logger.e(TAG, "Unable to set device name as msisdn");
				}
			});
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	public Boolean connectToHotspot(String ssid) 
	{
		WifiConfiguration wc = new WifiConfiguration();
		wc.SSID = "\"" +OfflineUtils.encodeSSID(ssid) +"\"";
		wc.preSharedKey  = "\"" + OfflineUtils.generatePassword(ssid)  +  "\"";
		wifiManager.addNetwork(wc);
		List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
		for( WifiConfiguration i : list ) {
			if(i!=null && i.SSID != null && i.SSID.equals(wc.SSID)) 
			{
				wifiManager.disconnect();
				boolean status = wifiManager.enableNetwork(i.networkId, true);
				wifiManager.reconnect();               
				return status;
			}
		}
		return false;
	}
	
	public void enableDiscovery()
	{
		if (!wifiManager.isWifiEnabled()) {
			wifiManager.setWifiEnabled(true);
        }
		wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() { 
            	Logger.d(TAG, "Wifi Direct Discovery Enabled");
            }

            @Override
            public void onFailure(int reasonCode) {
            	Logger.d(TAG, "Wifi Direct Discovery FAILED");
            }
		});
	}
	
	public void requestPeers(PeerListListener  peerListListener)
	{
		if (wifiP2pManager != null) {
            wifiP2pManager.requestPeers(channel, peerListListener);
		}
	}
	
	@Override
	public void onChannelDisconnected() {
		if (wifiP2pManager != null && !retryChannel) {
            retryChannel = true;
            wifiP2pManager.initialize(context, context.getMainLooper(), this);
        }
	}

	public void startWifiScan() {
		boolean wifiScan = wifiManager.startScan();
		Log.d(TAG, "Wifi Scan returns " + wifiScan);
	}
	
	public void startWifi()
	{
		wifiManager.setWifiEnabled(true);
	}
 
	public void stopWifi() 
	{
		wifiManager.setWifiEnabled(false);
	}
	
	public boolean isWifiEnabled() {
		return wifiManager.isWifiEnabled();
	}
	
	public void stopDiscovery() {
		wifiP2pManager.removeGroup(channel, null);
		wifiP2pManager.stopPeerDiscovery(channel, new ActionListener() {
			@Override
			public void onSuccess() {
				Log.d(TAG, "stop peer discovery started");
			}
			
			@Override
			public void onFailure(int reason) {
				Log.d(TAG, "Stop peer discovery failed");
			}
		});
	}
	
	public Pair<Boolean, String> isConnectedToHikeNetwork() {
		if(wifiManager.getConnectionInfo()!=null)
		{
			String ssid = wifiManager.getConnectionInfo().getSSID();
			Boolean isHikeNetwrok = (OfflineUtils.isOfflineSSID(ssid));
			if(isHikeNetwrok)
			{
				return new Pair<Boolean,String>(isHikeNetwrok,OfflineUtils.decodeSSID(ssid));
			}
		}
		return new Pair<Boolean,String>(false,null);
	}
	public HashMap<String, ScanResult> getWifiNetworksForMyMsisdn() {
		
		List<ScanResult> results = wifiManager.getScanResults();
		HashMap<String,ScanResult> distinctNetworks = new HashMap<String, ScanResult>();
		for(ScanResult scanResult :  results)
		{
			boolean cond = !(TextUtils.isEmpty(scanResult.SSID));
			scanResult.SSID = OfflineUtils.decodeSSID(scanResult.SSID);
			if(cond && scanResult.SSID.contains(myMsisdn))
			{
				if(!distinctNetworks.containsKey(scanResult))
				{
					distinctNetworks.put(scanResult.SSID, scanResult); 
				}
				else
				{
					if(WifiManager.compareSignalLevel(scanResult.level, distinctNetworks.get(scanResult.SSID).level)>0)
					{
						distinctNetworks.put(scanResult.SSID, scanResult);
					}
				}
			}
		}
		return distinctNetworks;
	}

	public Boolean createHotspot(String wifiP2pDeviceName)
	{
		saveCurrentHotspotSSID();
		String targetMsisdn =  wifiP2pDeviceName;
		Boolean result = false;
		ConnectivityManager cman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		Method[] methods = cman.getClass().getMethods();
		try
		{
			wifiManager.setWifiEnabled(false);
			Method enableWifi = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
			String ssid  =   "h_" +  myMsisdn + "_" + targetMsisdn;
			String encryptedSSID = OfflineUtils.encodeSSID(ssid);
			Logger.d(TAG, encryptedSSID);
			String pass = OfflineUtils.generatePassword(ssid);
			Logger.d(TAG, "Password: " + (pass));
			WifiConfiguration  myConfig =  new WifiConfiguration();
			myConfig.SSID = encryptedSSID;
			myConfig.preSharedKey  = pass ;
			myConfig.status = WifiConfiguration.Status.ENABLED;
			myConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			myConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			myConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP); 
			myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP); 
			myConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP); 
			myConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			result = (Boolean) enableWifi.invoke(wifiManager, myConfig, true);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			result = false;
		}
		if(result)
			setIsHotSpotCreated(true);
		return result;
		
	}
	public Boolean closeHotspot(String targetMsisdn)
	{
		Boolean result = false;
		try
		{
			Method enableWifi = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
			String ssid  =   "h_" +  myMsisdn + "_" + targetMsisdn;
			String encryptedSSID = OfflineUtils.encodeSSID(ssid);
			Logger.d(TAG, encryptedSSID);
			String pass = OfflineUtils.generatePassword(ssid);
			Logger.d(TAG, "Password: " + (pass));
			WifiConfiguration  myConfig =  new WifiConfiguration();
			myConfig.SSID = encryptedSSID;
			myConfig.preSharedKey  = pass ;
			myConfig.status =   WifiConfiguration.Status.ENABLED;
			myConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			myConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			myConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP); 
			myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP); 
			myConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP); 
			myConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			result = (Boolean) enableWifi.invoke(wifiManager, myConfig, false);

			// save previous name back
			setPreviousHotspotConfig();
		} 
		catch (Exception e) 
		{
			Logger.e(this.getClass().toString(), "", e);
			result = false;
		}
		if(result)
			setIsHotSpotCreated(false);
		return result;
		
	}
	private void setIsHotSpotCreated(boolean b) {
		
	}

	private void saveCurrentHotspotSSID() 
	{	
		Method[] methods = wifiManager.getClass().getDeclaredMethods();
		for (Method m: methods) 
		{           
			if (m.getName().equals("getWifiApConfiguration")) 
			{
				WifiConfiguration config;
				try 
				{
					config = (WifiConfiguration)m.invoke(wifiManager);
					prevConfig = config;
				} 
				catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) 
				{
					e.printStackTrace();
				}
			}
		}
	}

	private boolean setPreviousHotspotConfig()
	{
		Method setConfigMethod;
		boolean result = false;
		try {
			Method getConfigMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");
			WifiConfiguration wifiConfig = (WifiConfiguration) getConfigMethod.invoke(wifiManager);

			wifiConfig = prevConfig;
			setConfigMethod = wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
			setConfigMethod.invoke(wifiManager, wifiConfig);
			result = true;
		} 
		catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) 
		{
			e.printStackTrace();
			result = false;
		}
		return result;
	}

	public void connectAsPerMsisdn(String msisdn) {
		if(myMsisdn.compareTo(msisdn)>0)
		{
			//myMsisdn is greater
			createHotspot(msisdn);
		}
		else
		{
			
		}
	}
	
	public boolean isHotspotCreated() 
	{
		Method method;
		Boolean isWifiHotspotRunning = false;
		try {
			method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
			method.setAccessible(true);
			isWifiHotspotRunning = (Boolean) method.invoke(wifiManager);
		} catch (IllegalAccessException | IllegalArgumentException
				| NoSuchMethodException| InvocationTargetException e) {
			e.printStackTrace();
		}

		return isWifiHotspotRunning;
	}
	
}