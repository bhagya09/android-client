package com.bsb.hike.offline;

import java.util.HashMap;

import android.net.wifi.ScanResult;
import android.net.wifi.p2p.WifiP2pDeviceList;

public interface IOfflineCallbacks
{
	public void wifiP2PScanResults(WifiP2pDeviceList peerList);

	public void wifiScanResults(HashMap<String, ScanResult> list);

	public void onConnected();

	public void onDisconnect();

}
