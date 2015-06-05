package com.bsb.hike.offline;

import java.util.Map;

import com.bsb.hike.offline.OfflineConstants.ERRORCODE;

import android.net.wifi.ScanResult;
import android.net.wifi.p2p.WifiP2pDeviceList;

public interface IOfflineCallbacks
{
	public void wifiP2PScanResults(WifiP2pDeviceList peerList);

	public void wifiScanResults(Map<String, ScanResult> results);

	public void onDisconnect(ERRORCODE errorCode);

	public void connectedToMsisdn(String connectedDevice);

}

