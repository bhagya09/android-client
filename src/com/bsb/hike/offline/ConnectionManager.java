package com.bsb.hike.offline;

import java.lang.reflect.Field;
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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.hike.transporter.utils.Logger;

/**
 * 
 * @author sahil/deepak This class deals with functions related to WifiManager and WifiP2pManager and other Hotspot functionalities
 */
public class ConnectionManager implements ChannelListener
{
	private boolean retryChannel = false;
	private Context context;
	private WifiManager wifiManager;
	private WifiP2pManager wifiP2pManager;
	private Channel channel;
	private String TAG = ConnectionManager.class.getName();
    private WifiConfiguration prevConfig = null;
	private int connectedNetworkId = -1;
	private Looper looper;
	
	//isHtc indiactes that the device is htc as this is required to invoke some htc specific methods 
	private boolean isHTC = false;
	
	private String currentnetId=null;
	
	public ConnectionManager(Context context, Looper looper)
	{
    	init(context, looper);
	}
	
	private void init(Context context, Looper looper)
	{
		this.context = context;
		this.looper = looper;
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(context, looper, this);
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        
        //For htc devices , we need to call different set of reflection methods
        //mWifiApProfile indicates that it is htc device
		try
		{
	        isHTC = (WifiConfiguration.class.getDeclaredField("mWifiApProfile") != null) ? true : false;
		}
		catch (IllegalArgumentException | NoSuchFieldException e)
		{
			Logger.e(TAG, "Device is not htc");
		}
	}
	
	public void connectToHotspot(String targetMsisdn) 
	{
		String ssid = OfflineUtils.getSsidForMsisdn(OfflineUtils.getMyMsisdn(),targetMsisdn);
	//	wifiManager.saveConfiguration();
		Log.d("OfflineManager","SSID is "+ssid);
		WifiConfiguration wifiConfig = new WifiConfiguration();
		wifiConfig.SSID = "\"" +OfflineUtils.encodeSsid(ssid) +"\"";
		wifiConfig.preSharedKey  = "\"" + OfflineUtils.generatePassword(ssid)  +  "\"";
		wifiManager.addNetwork(wifiConfig);
		connectToWifi(wifiConfig.SSID);
	}
	
	public void startDiscovery()
	{
		if (!wifiManager.isWifiEnabled()) {
			wifiManager.setWifiEnabled(true);
        }
		wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() { 
            	Log.d(TAG, "Wifi Direct Discovery Enabled");
            }

            @Override
            public void onFailure(int reasonCode) {
            	Log.d(TAG, "Wifi Direct Discovery FAILED");
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
            wifiP2pManager.initialize(context, looper, this);
        }
	}

	public void startWifiScan() {
		
		// Switching off wifi Hotspot before wifi scanning
		if(isHotspotCreated())
		{
			if(saveCurrentHotspotSSID())
			{
				closeExistingHotspot(prevConfig);
			}
			/*Adding sleep so that wifi can switch on after closing of hotspot .\
				This may take longer on some devices . So will have to change logic later*/
			try 
			{
				Thread.sleep(1000);
			} 
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		
		if(!wifiManager.isWifiEnabled())
		{
			wifiManager.setWifiEnabled(true);
		}
		//We are trying to switch on user's wifi
		int attempts =0;
		while(!wifiManager.isWifiEnabled() && attempts<OfflineConstants.WIFI_RETRY_COUNT)
		{
			try 
			{
				Thread.sleep(500);
			} 
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			attempts++;
			Logger.d(TAG, "Trying to start wifi. Wifi State " + wifiManager.getWifiState());
		}
		if(!wifiManager.isWifiEnabled())
		{
			Logger.d(TAG, "WIFI COULD NOT START .CALLING SHUT DOWN");
			OfflineController.getInstance().shutdown(new OfflineException(OfflineException.WIFI_COULD_NOT_START));
			return;
		}
		boolean wifiScan = wifiManager.startScan();
		Log.d(TAG, "Wifi Scan returns " + wifiScan);
	}
	
	public void startWifi()
	{
		if(!isWifiEnabled())
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
				Log.d(TAG, "stop peer discovery started");
			}
			
			@Override
			public void onFailure(int reason) {
				Log.d(TAG, "Stop peer discovery failed");
			}
		});
	}
	
	public String getConnectedHikeNetworkMsisdn() {
		if(wifiManager.getConnectionInfo()!=null)
		{
			String ssid = wifiManager.getConnectionInfo().getSSID();
			Log.d("OfflineManager","Connected SSID in getConnectedHikeNetwork "+ssid);
			
			// System returns ssid as "ssid". Hence removing the quotes.
			if (ssid.length() > 2&&ssid.startsWith("\"")&&ssid.endsWith("\""))
				ssid = ssid.substring(1, ssid.length() - 1);
			
			Log.d(TAG, "ssid after removing quotes" + ssid);
			boolean isHikeNetwork = (OfflineUtils.isOfflineSsid(ssid));
			if(isHikeNetwork)
			{
				String decodedSSID = OfflineUtils.decodeSsid(ssid);
				Log.d("ConnectionManager","decodedSSID  is " + decodedSSID);
				String connectedMsisdn = OfflineUtils.getconnectedDevice(decodedSSID);  
				Log.d("ConnectionManager","connectedMsisdn is " + connectedMsisdn);
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
			if(cond && scanResult.SSID.contains(OfflineUtils.getMyMsisdn()))
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
				break;
			}
		}
		return distinctNetworks;
	}

	public Boolean createHotspot(String wifiP2pDeviceName)
	{
		// save current WifiHotspot Name
		Log.d("OfflineManager","wil create hotspot for "+wifiP2pDeviceName);
		if(saveCurrentHotspotSSID())
		{
			if(isHotspotCreated())
			{
				closeExistingHotspot(prevConfig);
			}
		}
		Boolean result = setWifiApEnabled(wifiP2pDeviceName, true);
		Log.d("OfflineManager", "HotSpot creation result is "+ result);
		return result;
	}
	private boolean closeExistingHotspot(WifiConfiguration config) {
		
		if(config==null)
		{
			return false;
		}
		
		boolean result = false;
		wifiManager.setWifiEnabled(false);
		Method enableWifi;
		try {
			enableWifi = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
		} catch (NoSuchMethodException e) {
			Log.e(TAG,e.toString());
			return result;
		}
		try {
			result = (Boolean) enableWifi.invoke(wifiManager,config,false);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			Log.e(TAG,e.toString());
			return result;
		}
		
		return result;
	}

	public Boolean closeHikeHotspot(String targetMsisdn)
	{
		Log.d(TAG, "going to close hotspot");
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
			Log.e(TAG,e.toString());
			return result;
		}
		String ssid  =   OfflineUtils.getSsidForMsisdn(targetMsisdn,OfflineUtils.getMyMsisdn());
		Log.d("OfflineManager","SSID is "+ssid + " -> " + status);
		String encryptedSSID = OfflineUtils.encodeSsid(ssid);
		Log.d(TAG, encryptedSSID);
		String pass = OfflineUtils.generatePassword(ssid);
		Log.d(TAG, "Password: " + (pass));
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
		
		if (isHTC)
			setHTCSSID(myConfig);
		
		try {
			result = (Boolean) enableWifi.invoke(wifiManager, myConfig,status);
		} 
		catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) 
		{
			e.printStackTrace();
			return result;
		}
		
		return result;
	}
	
	private void setHTCSSID(WifiConfiguration config)
	{
		try
		{
			Field mWifiApProfileField = WifiConfiguration.class.getDeclaredField("mWifiApProfile");
			mWifiApProfileField.setAccessible(true);
			Object hotSpotProfile = mWifiApProfileField.get(config);
			mWifiApProfileField.setAccessible(false);

			if (hotSpotProfile != null)
			{
				setField(hotSpotProfile, "SSID", config.SSID);
				setField(hotSpotProfile, "key", config.preSharedKey);
				setField(hotSpotProfile, "secureType", "wpa-psk");
				setField(hotSpotProfile, "dhcpEnable", 1);
				setField(hotSpotProfile, "maxConns", 8);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void setField(Object object, String field, String value)
	{	
		try
		{
			Field statusField = object.getClass().getDeclaredField(field);
			statusField.setAccessible(true);
			statusField.set(object, value);
			statusField.setAccessible(false);
		}
		catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException e)
		{
			e.printStackTrace();
		}
	}
	
	private void setField(Object object, String field, int value)
	{
		try
		{
			Field statusField = object.getClass().getDeclaredField(field);
			statusField.setAccessible(true);
			statusField.set(object, value);
			statusField.setAccessible(false);
		}
		catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException e)
		{
			e.printStackTrace();
		}
	}
	
	private Boolean saveCurrentHotspotSSID() 
	{
		try 
		{
			prevConfig = getWifiApConfiguration();
		} 
		catch (IllegalArgumentException e) 
		{
			Logger.e(TAG, "Could not save previous config");
			return false;
		}
		return true;
	}
	
	private WifiConfiguration getWifiApConfiguration()
	{
		WifiConfiguration configuration = null;
		try
		{
			Method method = WifiManager.class.getMethod("getWifiApConfiguration");
			configuration = (WifiConfiguration) method.invoke(wifiManager);
			if (isHTC)
				configuration = getHtcWifiApConfiguration(configuration);
		}
		catch (NoSuchMethodException | IllegalAccessException |
				IllegalArgumentException | InvocationTargetException e)
		{
			e.printStackTrace();
		}
		return configuration;
	}
	
	private WifiConfiguration getHtcWifiApConfiguration(WifiConfiguration standard)
	{
		WifiConfiguration htcWifiConfig = standard;
		try
		{
			Object mWifiApProfileValue = getFieldValue(standard, "mWifiApProfile");
			if (mWifiApProfileValue != null)
				htcWifiConfig.SSID = (String) getFieldValue(mWifiApProfileValue, "SSID");
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Could not get htc config");
		}
		return htcWifiConfig;
	}

	private Object getFieldValue(Object object, String propertyName) throws IllegalAccessException, NoSuchFieldException
	{
		Field field = object.getClass().getDeclaredField(propertyName);
		field.setAccessible(true);
		return field.get(object);
	};    
	    
	private boolean setPreviousHotspotConfig()
	{
		if(prevConfig==null)
		{
			return false;
		}
		
		Method setConfigMethod;
		boolean result = false;
		try 
		{
			String methodName = "setWifiApConfiguration";
			String htcMethodName = "setWifiApConfig";

			if (isHTC)
			{
				Log.d("OfflineManager", "This is a HTC device");
				methodName = htcMethodName;
			}
			setConfigMethod = wifiManager.getClass().getMethod(methodName, WifiConfiguration.class);
			setConfigMethod.invoke(wifiManager, prevConfig);
			result = true;
		} 
		catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) 
		{
			Log.e(TAG,e.toString());
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
			Log.e(TAG,e.toString());
		}

		return isWifiHotspotRunning;
	}

	private void forgetWifiNetwork()
	{
		boolean success = wifiManager.removeNetwork(connectedNetworkId);
		wifiManager.disconnect();
		Logger.d(TAG, "Forget Netwrork was " + success);
		connectedNetworkId = -1;
	}
	
	/**
	 * 
	 * @param ssid
	 * 
	 *            Some devices return SSID with quotes and some without it.So we are first checking that the SSID we saved is with quotes or not and then depending on that removing
	 *            the quotes from the SavedSystemList
	 */
	private void connectToWifi(String ssid)
	{
		
		if(TextUtils.isEmpty(ssid))
		{
			return ;
		}
		
		if(!wifiManager.isWifiEnabled())
		{
			wifiManager.setWifiEnabled(true);
		}
		
		List<WifiConfiguration> list= wifiManager.getConfiguredNetworks();
		
		if (list != null)
		{
			for (WifiConfiguration wifiConfiguration : list)
			{
				String currentSSID = "";
				if (wifiConfiguration == null || TextUtils.isEmpty(wifiConfiguration.SSID))
				{
					continue;
				}
				
				if (OfflineUtils.isSSIDWithQuotes(wifiConfiguration.SSID) && !OfflineUtils.isSSIDWithQuotes(ssid))
					currentSSID = wifiConfiguration.SSID.substring(1, wifiConfiguration.SSID.length() - 1);
				else
					currentSSID=wifiConfiguration.SSID;
				
				if (currentSSID.equals(ssid))
				{
					
					Logger.d("OfflineManager", "Disconnecting existing ssid");
					wifiManager.disconnect();
					boolean status = wifiManager.enableNetwork(wifiConfiguration.networkId, true);
					Logger.d("OfflineManager", "Enabled network" + status);
					connectedNetworkId = wifiConfiguration.networkId;
					wifiManager.reconnect();
					Logger.d("OfflineManager", "trying to connect! to ");

				}
			}
		}
		
	}
	
	public void tryConnectingToHotSpot(final String msisdn) 
	{
		Log.d(TAG, "tryConnectingToHotSpot");
		
		if(!wifiManager.isWifiEnabled())
		{
			wifiManager.setWifiEnabled(true);
		}
		connectToHotspot(msisdn);
	}
	
	public boolean isConnectedToSSID(String msisdn)
	{
		boolean isConnected = false;
		
		String encodedSSID = OfflineUtils.encodeSsid(OfflineUtils.getSsidForMsisdn(OfflineUtils.getMyMsisdn(), msisdn));
		String connectedToSSID = wifiManager.getConnectionInfo().getSSID();
		
		// removing quotes.
		if (connectedToSSID.length() > 2)
			connectedToSSID = connectedToSSID.substring(1, connectedToSSID.length()-1);
		Log.d(TAG, "Connected SSID in isConnectedToSSID: " + connectedToSSID + " EncodedSSID: " + encodedSSID);
		
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
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
		Log.d(TAG, "In getConnectedSSID: " + ssid);
		ssid = ssid.substring(1, ssid.length()-1);
		return ssid;
	}

	
	
	public void closeConnection(String deviceName) 
	{

		boolean isWifiHotspotRunning = OfflineUtils.isHotSpotCreated(OfflineController.getInstance().getConnectingDevice());
		if (isWifiHotspotRunning)
		{
			closeHikeHotspot(deviceName);
			if(!TextUtils.isEmpty(currentnetId))
			{
				startWifi();
			}
			
		}
		else
		{
			if (wifiManager.isWifiEnabled())
			{
				forgetWifiNetwork();
				connectToWifi(currentnetId);
			}
		}
		
		clearAllVariables();
	}

	private void clearAllVariables() {
		prevConfig = null;
		connectedNetworkId = -1;
		currentnetId = null;
	}
	
	public String getHostAddress()
	{
		String host = OfflineUtils.getIPFromMac(null);
		int tries = 0;
		while(TextUtils.isEmpty(host))
		{
			try{
				Thread.sleep(100);
			}
			catch (InterruptedException e){
				e.printStackTrace();
			}
			host = OfflineUtils.getIPFromMac(null);
			tries++;
			if (tries > 150)
				break;
		}
		Logger.d(TAG, "No. of tries is: " + tries);
		return host;
	}

	public void updateNetworkId()
	{
		currentnetId = wifiManager.getConnectionInfo().getSSID();
	}

}