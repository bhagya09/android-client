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
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.hike.transporter.utils.Logger;

/**
 * 
 * @author sahil/deepak This class deals with functions related to WifiManager and WifiP2pManager and other Hotspot functionalities
 */
public class ConnectionManager
{
	private boolean retryChannel = false;
	private Context context;
	private WifiManager wifiManager;
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
		Logger.d("OfflineManager","SSID is "+ssid);
		WifiConfiguration wifiConfig = new WifiConfiguration();
		wifiConfig.SSID = "\"" +OfflineUtils.encodeSsid(ssid) +"\"";
		wifiConfig.preSharedKey  = "\"" + OfflineUtils.generatePassword(ssid)  +  "\"";
		//wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
//		wifiConfig.allowedProtocols.set(1);
//		wifiConfig.allowedPairwiseCiphers.set(1);
//		wifiConfig.allowedPairwiseCiphers.set(2);
//		wifiConfig.allowedGroupCiphers.set(0);
//		wifiConfig.allowedGroupCiphers.set(1);
//		wifiConfig.allowedGroupCiphers.set(2);
//        wifiConfig.allowedGroupCiphers.set(3);
//      //  wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
		
//		wifiManager.enableNetwork(result, false);
//		wifiConfig.networkId=result;
//		wifiManager.updateNetwork(wifiConfig);
//		wifiManager.saveConfiguration();
//		wifiConfig.allowedAuthAlgorithms.set(0);
//		wifiConfig.allowedProtocols.set(1);
//		wifiConfig.allowedProtocols.set(0);
//		wifiConfig.allowedPairwiseCiphers.set(2);
//		wifiConfig.allowedPairwiseCiphers.set(1);
//        wifiConfig.allowedGroupCiphers.set(3);
//        wifiConfig.allowedGroupCiphers.set(2);
		
		
		
		
		// stack overflow 
		wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
		wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
		wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
		wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
		wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
		wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
		wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
		wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
		wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		
        int result=wifiManager.addNetwork(wifiConfig);
        wifiManager.saveConfiguration();
		connectToWifi(wifiConfig.SSID);
	}
	
	
	public void startWifiScan()
	{

		// Switching off wifi Hotspot before wifi scanning
		if (isHotspotCreated())
		{
			if (saveCurrentHotspotSSID())
			{
				closeExistingHotspot(prevConfig);
			}
			/*
			 * Adding sleep so that wifi can switch on after closing of hotspot .\ This may take longer on some devices . So will have to change logic later
			 */
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				Logger.e(TAG, "Error in thread sleep while starting wifi");
			}
		}

		if (!wifiManager.isWifiEnabled())
		{
			wifiManager.setWifiEnabled(true);
		}
		// We are trying to switch on user's wifi
		int attempts = 0;
		int MAX_TRIES = new Gson().fromJson(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.OFFLINE, "{}"), OfflineParameters.class).getMaxWifiwaitTime();
		Logger.d(TAG,"Number of reties for swtictching on wifi is   "+MAX_TRIES);
		while (!wifiManager.isWifiEnabled() && attempts < MAX_TRIES)
		{
			try
			{
				Thread.sleep(500);
			}
			catch (InterruptedException e)
			{
				Logger.e(TAG, "Error in thread sleep while starting wifi");
			}
			attempts++;
			Logger.d(TAG, "Trying to start wifi. Wifi State " + wifiManager.getWifiState());
		}
		if (!wifiManager.isWifiEnabled())
		{
			Logger.d(TAG, "WIFI COULD NOT START .CALLING SHUT DOWN");
			OfflineController.getInstance().shutdown(new OfflineException(OfflineException.WIFI_COULD_NOT_START));
			return;
		}
		boolean wifiScan = wifiManager.startScan();
		Logger.d(TAG, "Wifi Scan returns " + wifiScan);
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
	
	public boolean isWifiEnabled()
	{
		return wifiManager.isWifiEnabled();
	}

	public String getConnectedHikeNetworkMsisdn()
	{
		if (wifiManager.getConnectionInfo() != null)
		{
			String ssid = wifiManager.getConnectionInfo().getSSID();
			if (!TextUtils.isEmpty(ssid))
			{
				Logger.d("OfflineManager", "Connected SSID in getConnectedHikeNetwork " + ssid);

				// System returns ssid as "ssid". Hence removing the quotes.
				if (ssid.length() > 2 && ssid.startsWith("\"") && ssid.endsWith("\""))
					ssid = ssid.substring(1, ssid.length() - 1);

				Logger.d(TAG, "ssid after removing quotes" + ssid);
				boolean isHikeNetwork = (OfflineUtils.isOfflineSsid(ssid));
				if (isHikeNetwork)
				{
					String decodedSSID = OfflineUtils.decodeSsid(ssid);
					Logger.d("ConnectionManager", "decodedSSID  is " + decodedSSID);
					String connectedMsisdn = OfflineUtils.getconnectedDevice(decodedSSID);
					Logger.d("ConnectionManager", "connectedMsisdn is " + connectedMsisdn);
					return connectedMsisdn;
				}
			}

		}
		return null;
	}

	public Map<String, ScanResult> getWifiNetworksForMyMsisdn()
	{

		List<ScanResult> results = wifiManager.getScanResults();
		Map<String, ScanResult> distinctNetworks = new HashMap<String, ScanResult>();
		if (results == null || results.isEmpty())
		{
			return distinctNetworks;
		}
		for (ScanResult scanResult : results)
		{
			if(scanResult==null || TextUtils.isEmpty(scanResult.SSID))
			{
				continue;
			}
			
			scanResult.SSID = OfflineUtils.decodeSsid(scanResult.SSID);
			if (!TextUtils.isEmpty(OfflineUtils.getMyMsisdn()) && scanResult.SSID.contains(OfflineUtils.getMyMsisdn()))
			{
				if (!distinctNetworks.containsKey(scanResult))
				{
					distinctNetworks.put(scanResult.SSID, scanResult);
				}
				else
				{
					if (WifiManager.compareSignalLevel(scanResult.level, distinctNetworks.get(scanResult.SSID).level) > 0)
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
		Logger.d("OfflineManager","wil create hotspot for "+wifiP2pDeviceName);
		if(saveCurrentHotspotSSID())
		{
			if(isHotspotCreated())
			{
				closeExistingHotspot(prevConfig);
			}
		}
		Boolean result = setWifiApEnabled(wifiP2pDeviceName, true);
		Logger.d("OfflineManager", "HotSpot creation result is "+ result);
		return result;
	}

	private boolean closeExistingHotspot(WifiConfiguration config)
	{

		if (config == null)
		{
			return false;
		}

		boolean result = false;
		wifiManager.setWifiEnabled(false);
		Method enableWifi;
		try
		{
			enableWifi = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
		}
		catch (NoSuchMethodException e)
		{
			Logger.e(TAG, "setwifiApEnabled method not found");
			return result;
		}
		try
		{
			result = (Boolean) enableWifi.invoke(wifiManager, config, false);
		}
		catch (IllegalAccessException e)
		{
			Logger.e(TAG, "IllegalAccess exception occured");
			return result;
		}
		catch(IllegalArgumentException e)
		{
			Logger.e(TAG, "IllegalArgument exception occured");
			return result;
		}
		catch(InvocationTargetException e)
		{
			Logger.e(TAG, "InvocationTargetException exception occured");
			return result;
		}

		return result;
	}

	public Boolean closeHikeHotspot(String targetMsisdn)
	{
		Logger.d(TAG, "going to close hotspot");
		Boolean result = setWifiApEnabled(targetMsisdn, false);
		// restore previous WifiHotspot Name back
		setPreviousHotspotConfig();
		return result;
	}
	
	private boolean setWifiApEnabled(String targetMsisdn, boolean status)
	{
		boolean result = false;
		wifiManager.setWifiEnabled(false);
		Method enableWifi;
		try
		{
			enableWifi = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
		}
		catch (NoSuchMethodException e)
		{
			Logger.e(TAG, "setWifiApEnabled method not found");
			return result;
		}
		String ssid = OfflineUtils.getSsidForMsisdn(targetMsisdn, OfflineUtils.getMyMsisdn());
		Logger.d("OfflineManager", "SSID is " + ssid + " -> " + status);
		String encryptedSSID = OfflineUtils.encodeSsid(ssid);
		Logger.d(TAG, encryptedSSID);
		String pass = OfflineUtils.generatePassword(ssid);
		Logger.d(TAG, "Password: " + (pass));
		WifiConfiguration myConfig = new WifiConfiguration();
		myConfig.SSID = encryptedSSID;
		myConfig.preSharedKey = pass;
		myConfig.status = WifiConfiguration.Status.ENABLED;
		myConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
//		myConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
//		myConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
//		
//		myConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
//		myConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
//		myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
//		myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
//		myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
//		myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

		if (isHTC)
			setHTCSSID(myConfig);

		try
		{
			result = (Boolean) enableWifi.invoke(wifiManager, myConfig, status);
		}
		catch (IllegalAccessException e)
		{
			Logger.e(TAG, "IllegalAccess exception occured");
			return result;
		}
		catch(IllegalArgumentException e)
		{
			Logger.e(TAG, "IllegalArgument exception occured");
			return result;
		}
		catch(InvocationTargetException e)
		{
			Logger.e(TAG, "InvocationTargetException exception occured");
			return result;
		}
		
		return result;
	}
	
	private void setHTCSSID(WifiConfiguration config)
	{
		
			Field mWifiApProfileField;
			try
			{
				mWifiApProfileField = WifiConfiguration.class.getDeclaredField("mWifiApProfile");
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
			catch (NoSuchFieldException e)
			{
				Logger.e(TAG, "mWifiApProfile method not found");
			}
			catch (IllegalAccessException e)
			{
				Logger.e(TAG, "IllegalAccess exception occured");
			}
			catch (IllegalArgumentException e)
			{
				Logger.e(TAG, "IllegalArgument exception occured");
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
		catch (NoSuchFieldException e)
		{
			Logger.e(TAG, "mWifiApProfile method not found");
		}
		catch (IllegalAccessException e)
		{
			Logger.e(TAG, "IllegalAccess exception occured");
		}
		catch (IllegalArgumentException e)
		{
			Logger.e(TAG, "IllegalArgument exception occured");
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
		catch (NoSuchFieldException e)
		{
			Logger.e(TAG, "mWifiApProfile method not found");
		}
		catch (IllegalAccessException e)
		{
			Logger.e(TAG, "IllegalAccess exception occured");
		}
		catch (IllegalArgumentException e)
		{
			Logger.e(TAG, "IllegalArgument exception occured");
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
			Logger.e(TAG, "Could not save prev config");
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
		catch (IllegalAccessException e)
		{
			Logger.e(TAG, "IllegalAccess exception occured");
		}
		catch (IllegalArgumentException e)
		{
			Logger.e(TAG, "IllegalArgument exception occured");
		}
		catch (NoSuchMethodException e)
		{
			Logger.e(TAG, "setwifiApEnabled method not found");
		}
		catch (InvocationTargetException e)
		{
			Logger.e(TAG, "InvocationTargetException exception occured");
		}
		return configuration;
	}
	
	private WifiConfiguration getHtcWifiApConfiguration(WifiConfiguration standard)
	{
		WifiConfiguration htcWifiConfig = standard;
			Object mWifiApProfileValue;
			try
			{
				mWifiApProfileValue = getFieldValue(standard, "mWifiApProfile");
				if (mWifiApProfileValue != null)
					htcWifiConfig.SSID = (String) getFieldValue(mWifiApProfileValue, "SSID");
			}
			catch (IllegalAccessException e)
			{
				Logger.e(TAG, "IllegalAccessException occured");
			}
			catch (NoSuchFieldException e)
			{
				Logger.e(TAG, "No such field found");
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
		if (prevConfig == null)
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
				Logger.d("OfflineManager", "This is a HTC device");
				methodName = htcMethodName;
			}
			setConfigMethod = wifiManager.getClass().getMethod(methodName, WifiConfiguration.class);
			setConfigMethod.invoke(wifiManager, prevConfig);
			result = true;
		}
		catch (IllegalAccessException e)
		{
			Logger.e(TAG, "IllegalAccess exception occured");
			result  = false;
		}
		catch (IllegalArgumentException e)
		{
			Logger.e(TAG, "IllegalArgument exception occured");
			result  = false;
		}
		catch (NoSuchMethodException e)
		{
			Logger.e(TAG, "setwifiApEnabled method not found");
			result  = false;
		}
		catch (InvocationTargetException e)
		{
			Logger.e(TAG, "InvocationTargetException exception occured");
			result  = false;
		}
		return result;
	}
	
	public boolean isHotspotCreated()
	{
		Method method;
		Boolean isWifiHotspotRunning = false;
		try
		{
			method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
			method.setAccessible(true);
			isWifiHotspotRunning = (Boolean) method.invoke(wifiManager);
		}
		catch (IllegalAccessException e)
		{
			Logger.e(TAG, "IllegalAccess exception occured");

		}
		catch (IllegalArgumentException e)
		{
			Logger.e(TAG, "IllegalArgument exception occured");

		}
		catch (NoSuchMethodException e)
		{
			Logger.e(TAG, "setwifiApEnabled method not found");

		}
		catch (InvocationTargetException e)
		{
			Logger.e(TAG, "InvocationTargetException exception occured");
		}

		return isWifiHotspotRunning;
	}

	private void forgetWifiNetwork()
	{
		boolean success = wifiManager.removeNetwork(connectedNetworkId);
		wifiManager.disconnect();
		wifiManager.saveConfiguration();
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

		if (!isSSIDValid(ssid))
		{
			stopWifi();
			return;
		}

		startWifi();

		Logger.d("OfflineMANAGER", "WILL BE GETTING LIST" + System.currentTimeMillis() + " trying ssid " + ssid);
		List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
		Logger.d("OfflineMANAGER", "LIST RECEIVED" + System.currentTimeMillis());

		if (list != null)
		{
			Logger.d("OfflineManager","List size is "+list.size());
			for (WifiConfiguration wifiConfiguration : list)
			{
				String currentSsid = "";
				if (wifiConfiguration == null || TextUtils.isEmpty(wifiConfiguration.SSID))
				{
					continue;
				}

				if (OfflineUtils.isSSIDWithQuotes(wifiConfiguration.SSID) && !OfflineUtils.isSSIDWithQuotes(ssid))
					currentSsid = wifiConfiguration.SSID.substring(1, wifiConfiguration.SSID.length() - 1);
				else
					currentSsid = wifiConfiguration.SSID;

				Logger.d(TAG, "currentssid is " + currentSsid + "and ssid is " + ssid+"BSSID is "+wifiConfiguration.BSSID);
				if (currentSsid.equals(ssid))
				{
					Logger.d("OfflineManager", "Disconnecting existing ssid. Current ssid is  " + currentSsid + " Ssid in list is  " + wifiConfiguration.SSID+"BSSID is "+wifiConfiguration.BSSID);
					wifiManager.disconnect();
					boolean status = wifiManager.enableNetwork(wifiConfiguration.networkId, true);
					wifiManager.saveConfiguration();
					Logger.d("OfflineManager", "Enabled network" + status);
					connectedNetworkId = wifiConfiguration.networkId;
					wifiManager.reassociate();
					wifiManager.reconnect();
					break;
				}
			}
		}

	}
	
	public void tryConnectingToHotSpot(final String msisdn) 
	{
		Logger.d(TAG, "tryConnectingToHotSpot");
		
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
		
		if(TextUtils.isEmpty(connectedToSSID))
		{
			return isConnected;
		}
		// removing quotes.
		if (connectedToSSID.length() > 2)
			connectedToSSID = connectedToSSID.substring(1, connectedToSSID.length()-1);
		Logger.d(TAG, "Connected SSID in isConnectedToSSID: " + connectedToSSID + " EncodedSSID: " + encodedSSID);
		
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (connectedToSSID.compareTo(encodedSSID)==0 &&  networkInfo!=null && networkInfo.isConnected())
		{
			isConnected = true;
		}
		return isConnected;
	}
	
	public void closeConnection(String deviceName) 
	{

		boolean isWifiHotspotRunning = OfflineUtils.isHotSpotCreated(OfflineController.getInstance().getConnectingDevice());
		if (isWifiHotspotRunning)
		{
			closeHikeHotspot(deviceName);
			if (isSSIDValid(currentnetId))
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

	private void clearAllVariables()
	{
		prevConfig = null;
		connectedNetworkId = -1;
		currentnetId = null;
	}
	
	public String getHostAddress()
	{
		String host = OfflineUtils.getIPFromMac(null);
		int tries = 0;
		int MAX_TRIES = new Gson().fromJson(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.OFFLINE, "{}"), OfflineParameters.class).getMaxTryForIpExtraction();
		Logger.d(TAG,"Number of reties is for getting host address is  "+MAX_TRIES);
		while (TextUtils.isEmpty(host))
		{
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				Logger.e(TAG, "Error in thread sleep");
			}
			host = OfflineUtils.getIPFromMac(null);
			tries++;
			if (tries > MAX_TRIES)
				break;
		}
		Logger.d(TAG, "No. of tries is: " + tries);
		return host;
	}

	public void updateNetworkId()
	{
		currentnetId = wifiManager.getConnectionInfo().getSSID();
	}
	
	private boolean isSSIDValid(String ssid)
	{
		if (TextUtils.isEmpty(ssid) || (ssid.startsWith("0x") || ssid.startsWith("0X")) || ssid.contains("unknown ssid") || ssid.contains("none"))
			return false;

		return true;
	}

}