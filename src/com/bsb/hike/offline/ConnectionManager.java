package com.bsb.hike.offline;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.utils.Logger;

public class ConnectionManager implements ChannelListener
{
	private static ConnectionManager _instance = null;
	private boolean retryChannel = false;
	private Context context;
	private WifiManager wifiManager;
	private WifiP2pManager wifiP2pManager;
	private Channel channel;
    private String myMsisdn =null;
	private String TAG = ConnectionManager.class.getName();
    private WifiConfiguration prevConfig;
	private int connectedNetworkId;
	private ConnectionManager()
	{
    	init();
	}
	
	public static ConnectionManager getInstance()
	{
		if(_instance==null)
			synchronized (ConnectionManager.class) {
				if (_instance == null) {
					_instance = new ConnectionManager();
				}
			}
			
		return _instance;
	}
	
	private void init()
	{
		context =HikeMessengerApp.getInstance().getApplicationContext();
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(context,HikeHandlerUtil.getInstance().getLooper(), this);
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        myMsisdn = OfflineUtils.getMyMsisdn();
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
					Logger.d(TAG, "Device Name changed to " + myMsisdn);
				}
				@Override
				public void onFailure(int reason) {
					Logger.e(TAG, "Unable to set device name as msisdn");
				}
			});
		} catch (NoSuchMethodException e) {
			Logger.e(TAG, e.toString());
		} catch (IllegalAccessException e) {
			Logger.e(TAG,e.toString());
		} catch (IllegalArgumentException e) {
			Logger.e(TAG,e.toString());
		} catch (InvocationTargetException e) {
			Logger.e(TAG,e.toString());
		}
	}
	
	public Boolean connectToHotspot(String targetMsisdn) 
	{
		String ssid = OfflineUtils.getSsidForMsisdn(myMsisdn,targetMsisdn);
		Logger.d("OfflineManager","SSID is "+ssid);
		WifiConfiguration wifiConfig = new WifiConfiguration();
		wifiConfig.SSID = "\"" +OfflineUtils.encodeSsid(ssid) +"\"";
		wifiConfig.preSharedKey  = "\"" + OfflineUtils.generatePassword(ssid)  +  "\"";
		wifiManager.addNetwork(wifiConfig);
		List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
		for( WifiConfiguration wifiConfiguration : list ) 
		{
			if(wifiConfiguration!=null && wifiConfiguration.SSID != null && wifiConfiguration.SSID.equals(wifiConfig.SSID)) 
			{
				wifiManager.disconnect();
				boolean status = wifiManager.enableNetwork(wifiConfiguration.networkId, true);
				connectedNetworkId=wifiConfiguration.networkId;
				wifiManager.reconnect();               
				return status;
			}
		}
		return false;
	}
	
	public void startDiscovery()
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
            wifiP2pManager.initialize(context,HikeHandlerUtil.getInstance().getLooper(), this);
        }
	}

	public void startWifiScan() {
		if(!wifiManager.isWifiEnabled())
		{
			wifiManager.setWifiEnabled(true);
		}
		
		while(!wifiManager.isWifiEnabled())
		{
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block 
				e.printStackTrace();
			}
		}
		boolean wifiScan = wifiManager.startScan();
		Logger.d(TAG, "Wifi Scan returns " + wifiScan);
	}
	
	public void startWifi()
	{
		wifiManager.setWifiEnabled(true);
	}
 
	public void stopWifi() 
	{
		if(wifiManager.isWifiEnabled())
			wifiManager.setWifiEnabled(false);
	}
	
	public boolean isWifiEnabled() {
		return wifiManager.isWifiEnabled();
	}
	
	public void stopDiscovery() {
		wifiP2pManager.stopPeerDiscovery(channel, new ActionListener() {
			@Override
			public void onSuccess() {
				Logger.d(TAG, "stop peer discovery started");
			}
			
			@Override
			public void onFailure(int reason) {
				Logger.d(TAG, "Stop peer discovery failed");
			}
		});
	}
	
	public String getConnectedHikeNetworkMsisdn() {
		if(wifiManager.getConnectionInfo()!=null)
		{
			String ssid = wifiManager.getConnectionInfo().getSSID();
			Logger.d("OfflineManager","Connected SSID in getConnectedHikeNetwork "+ssid);
			
			// System returns ssid as "ssid". Hence removing the quotes.
			ssid = ssid.substring(1, ssid.length()-1);
			Logger.d(TAG, "ssid after removing quotes" + ssid);
			boolean isHikeNetwork = (OfflineUtils.isOfflineSsid(ssid));
			if(isHikeNetwork)
			{
				String decodedSSID = OfflineUtils.decodeSsid(ssid);
				Logger.d("ConnectionManager","decodedSSID  is " + decodedSSID);
				String connectedMsisdn = OfflineUtils.getconnectedDevice(decodedSSID);  
				Logger.d("ConnectionManager","connectedMsisdn is " + connectedMsisdn);
				return connectedMsisdn;
			}
		}
		return null;
	}
	public Map<String, ScanResult> getWifiNetworksForMyMsisdn() {
		
		List<ScanResult> results = wifiManager.getScanResults();
		Map<String,ScanResult> distinctNetworks = new HashMap<String, ScanResult>();
		for(ScanResult scanResult :  results)
		{
			boolean cond = !(TextUtils.isEmpty(scanResult.SSID));
			scanResult.SSID = OfflineUtils.decodeSsid(scanResult.SSID);
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
		// save current WifiHotspot Name
		Logger.d("OfflineManager","wil create hotspot for "+wifiP2pDeviceName);
		saveCurrentHotspotSSID();
		Boolean result = setWifiApEnabled(wifiP2pDeviceName, true);
		Logger.d("OfflineManager", "HotSpot creation result is "+ result);
		return result;
	}
	public Boolean closeHotspot(String targetMsisdn)
	{
		Logger.d(TAG, "going to close hotspot");
		Boolean result = setWifiApEnabled(targetMsisdn, false);
		// restore previous WifiHotspot Name back
		setPreviousHotspotConfig();
		return result;
	}
	
	private boolean setWifiApEnabled(String targetMsisdn,boolean status)
	{
		boolean result = false;
		wifiManager.setWifiEnabled(false);
		Method enableWifi;
		try {
			enableWifi = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
		} catch (NoSuchMethodException e) {
			Logger.e(TAG,e.toString());
			return result;
		}
		String ssid  =   OfflineUtils.getSsidForMsisdn(targetMsisdn, myMsisdn);
		Logger.d("OfflineManager","SSID is "+ssid);
		String encryptedSSID = OfflineUtils.encodeSsid(ssid);
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
		try {
			result = (Boolean) enableWifi.invoke(wifiManager, myConfig,status);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			Logger.e(TAG,e.toString());
			return result;
		}
		
		return result;
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
					Logger.e(TAG,e.toString());
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
			Logger.e(TAG,e.toString());
			result = false;
		}
		return result;
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
			Logger.e(TAG,e.toString());
		}

		return isWifiHotspotRunning;
	}

	public void disconnect(String msisdn) {
		Boolean isWifiHotspotRunning = isHotspotCreated();
		if(isWifiHotspotRunning)
		{
			closeHotspot(msisdn);
		}
		else
		{
			forgetWifiNetwork();
			wifiManager.disconnect();
		}
	}
	
	private void forgetWifiNetwork()
	{
		wifiManager.removeNetwork(connectedNetworkId);
		connectedNetworkId=-1;
	}
	
	public boolean tryConnectingToHotSpot(final String msisdn) 
	{
		Logger.d(TAG, "tryConnectingToHotSpot");
		
		if(!wifiManager.isWifiEnabled())
		{
			wifiManager.setWifiEnabled(true);
		}
		return connectToHotspot(msisdn);
	}
	
	public boolean isConnectedToSSID(String msisdn)
	{
		boolean isConnected = false;
		
		String encodedSSID = OfflineUtils.encodeSsid(OfflineUtils.getSsidForMsisdn(myMsisdn, msisdn));
		String connectedToSSID = wifiManager.getConnectionInfo().getSSID();
		
		// removing quotes.
		if (connectedToSSID.length() > 2)
			connectedToSSID = connectedToSSID.substring(1, connectedToSSID.length()-1);
		Logger.d(TAG, "Connected SSID in isConnectedToSSID: " + connectedToSSID + " EncodedSSID: " + encodedSSID);
		
		ConnectivityManager cm = (ConnectivityManager) HikeMessengerApp.getInstance().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (connectedToSSID.compareTo(encodedSSID)==0 && networkInfo.isConnected())
		{
			isConnected = true;
		}
		return isConnected;
	}

	public String getConnectedSSID()
	{
		String ssid =  wifiManager.getConnectionInfo().getSSID();
		Logger.d(TAG, "In getConnectedSSID: " + ssid);
		ssid = ssid.substring(1, ssid.length()-1);
		return ssid;
	}

	
	
	public void closeConnection(String deviceName) 
	{
		boolean isWifiHotspotRunning = isHotspotCreated();
		if (isWifiHotspotRunning)
		{
			closeHotspot(deviceName);
		}
		else
		{
			if (wifiManager.isWifiEnabled())
			{
				forgetWifiNetwork();

				wifiManager.disconnect();
			}
		}
	}
}