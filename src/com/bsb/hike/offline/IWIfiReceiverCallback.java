package com.bsb.hike.offline;

import java.util.HashMap;

import android.net.wifi.ScanResult;

public interface IWIfiReceiverCallback
{
	public void onRequestPeers();

	public void onScanResultAvailable();
	
	public void onHotSpotConnected();

	public void onConnectionToWifiNetwork();

	public void onDiscoveryStarted();

	public void onDiscoveryStopped();
}