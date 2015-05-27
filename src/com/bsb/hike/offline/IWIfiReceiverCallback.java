package com.bsb.hike.offline;

import java.util.HashMap;

import android.net.wifi.ScanResult;

public interface IWIfiReceiverCallback
{
	public void onRequestPeers();

	public void onDiscoveryStateChanged();
	
	public void onScanResultAvailable(HashMap<String, ScanResult> currentNearByNetwork);
	
	public void onHotSpotConnected();
}
