package com.bsb.hike.offline;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeOfflinePersistence;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.offline.OfflineConstants.ERRORCODE;
import com.bsb.hike.offline.OfflineConstants.HandlerConstants;
import com.bsb.hike.offline.OfflineConstants.OFFLINE_STATE;
import com.bsb.hike.utils.Logger;
import com.hike.transporter.DefaultRetryPolicy;
import com.hike.transporter.TException;
import com.hike.transporter.Transporter;
import com.hike.transporter.interfaces.IConnectionListener;
import com.hike.transporter.interfaces.IMessageReceived;
import com.hike.transporter.interfaces.IMessageSent;
import com.hike.transporter.models.Config;
import com.hike.transporter.models.SenderConsignment;
import com.hike.transporter.models.Topic;

/**
 * 
 * @author himanshu, deepak malik , sahil This class forms the base of Offline Messaging and deals with socket connection,text transfer and file transfer queue.
 */

public class OfflineManager implements IWIfiReceiverCallback, PeerListListener,IConnectionListener
{
	private ArrayList<IOfflineCallbacks> listeners;

	private Context context;

	private volatile String connectedDevice = null;

	private ConnectionManager connectionManager;

	private static final String TAG = OfflineManager.class.getName();

	private boolean scanResultsAvailable = false;

	private int tryGetScanResults = 0;

	OfflineBroadCastReceiver receiver;

	private boolean startedForChatThread = false;
	
	private Transporter transporter;
	
	private Config transporterConfig;
	
	private List<Topic> topics;

	Handler handler = new Handler(HikeHandlerUtil.getInstance().getLooper())
	{
		public void handleMessage(android.os.Message msg)
		{
			if (msg == null)
			{
				return;
			}

			handleMsgOnBackEndThread(msg);
		}

	};

	private IMessageSent messageSentCallback=null;

	private IMessageReceived messageReceivedCallback=null;

	public OfflineManager(IMessageSent messageSentCallback,IMessageReceived messageReceivedCallback)
	{
		this.messageSentCallback = messageSentCallback;
		this.messageReceivedCallback = messageReceivedCallback;
		init();
	}


	private void handleMsgOnBackEndThread(Message msg)
	{
		switch (msg.what)
		{
		case OfflineConstants.HandlerConstants.CREATE_HOTSPOT:
			connectionManager.createHotspot((String) msg.obj);
			break;
		case OfflineConstants.HandlerConstants.CONNECT_TO_HOTSPOT:
			startedForChatThread = true;
			startWifiScan();
			Logger.d(TAG, "Scanning for hotspots");
			checkAndRetryConnect((String) msg.obj);
			break;
		case OfflineConstants.HandlerConstants.RECONNECT_TO_HOTSPOT:
			String connectingToMsisdn = (String) msg.obj;
			Logger.d(TAG, "Rescanning for hotspot.");
			startWifiScan();
			Message checkAndRetry = Message.obtain();
			checkAndRetry.what = OfflineConstants.HandlerConstants.RECONNECT_TO_HOTSPOT;
			checkAndRetry.obj = connectingToMsisdn;
			handler.sendMessageDelayed(checkAndRetry, OfflineConstants.TRY_CONNECT_TO_HOTSPOT);
			break;
		case OfflineConstants.HandlerConstants.REMOVE_CONNECT_MESSAGE:
			removeMessage(OfflineConstants.HandlerConstants.RECONNECT_TO_HOTSPOT);
			Logger.d(TAG, "Disconnecting due to timeout");
			if (TextUtils.isEmpty(getConnectedDevice()))
			{
				onDisconnect(new OfflineException(OfflineException.CONNECTION_TIME_OUT));
			}
			break;
		case OfflineConstants.HandlerConstants.START_SCAN:
			runNetworkScan((int) msg.obj);
			msg.obj = ((int) msg.obj) + 1;
			performWorkOnBackEndThread(msg);
			break;
		case OfflineConstants.HandlerConstants.DISCONNECT_BY_USER:
			onDisconnect(new OfflineException(OfflineException.USER_DISCONNECTED));
			break;
		}
	};

	private void checkAndRetryConnect(String msisdn)
	{
		Message checkAndRetry = Message.obtain();
		checkAndRetry.what = OfflineConstants.HandlerConstants.RECONNECT_TO_HOTSPOT;
		checkAndRetry.obj = msisdn;
		handler.sendMessageDelayed(checkAndRetry, OfflineConstants.TRY_CONNECT_TO_HOTSPOT);
	}

	public void performWorkOnBackEndThread(Message msg)
	{
		handler.sendMessage(msg);
	}

	/**
	 * Initialize all your functions here
	 */
	private void init()
	{
		context = HikeMessengerApp.getInstance().getApplicationContext();
		connectionManager = new ConnectionManager(context, HikeHandlerUtil.getInstance().getLooper());
		transporter = Transporter.getInstance();
		listeners = new ArrayList<IOfflineCallbacks>();
		setDeviceNameAsMsisdn();
		receiver = new OfflineBroadCastReceiver(this);
		Logger.d(TAG, "Contructor called");
	}

	private void setDeviceNameAsMsisdn()
	{
		// TODO : Restore back to previous deviceName
		connectionManager.setDeviceNameAsMsisdn();
	}

	public void disconnect()
	{
		// Since disconnect is called, stop sending ghost packets
		//removeMessage(OfflineConstants.HandlerConstants.SEND_GHOST_PACKET);
		onDisconnect(new OfflineException(OfflineException.CONNECTION_TIME_OUT));

	}
	
	@Override
	public void onRequestPeers()
	{
		connectionManager.requestPeers(this);
	}
	
	public String getConnectedDevice()
	{
		return connectedDevice;
	}
	@Override
	public void onHotSpotConnected()
	{

	}

	@Override
	public void onPeersAvailable(WifiP2pDeviceList peers)
	{
		// pass results to listeners
		for (IOfflineCallbacks offlineListener : listeners)
		{
			offlineListener.wifiP2PScanResults(peers);
		}

	}

	@Override
	public void onScanResultAvailable()
	{
		scanResultsAvailable = true;
		Map<String, ScanResult> results = connectionManager.getWifiNetworksForMyMsisdn();

		if (startedForChatThread)
		{
			String ssid = OfflineUtils.getSsidForMsisdn(OfflineUtils.getMyMsisdn(), connectinMsisdn);
			if (results.containsKey(ssid))
			{
				Logger.d(TAG, "Going to connect to Hotspot for msisdn" + ssid);
				connectionManager.connectToHotspot(connectinMsisdn);
				startedForChatThread = false;
			}
		}
		else
		{
			// pass results to listeners
			for (IOfflineCallbacks offlineListener : listeners)
			{
				offlineListener.wifiScanResults(results);
			}
		}

	}

	public void addListener(IOfflineCallbacks listener)
	{
		if (listener == null)
			return;

		listeners.add(listener);

		if (listeners.size() == 1)
		{
			IntentFilter intentFilter = new IntentFilter();
			addIntentFilters(intentFilter);
			context.registerReceiver(receiver, intentFilter);
		}
	}

	public void addBroadReceiver()
	{
		IntentFilter intentFilter = new IntentFilter();
		addIntentFilters(intentFilter);
		context.registerReceiver(receiver, intentFilter);
	}
	private void addIntentFilters(IntentFilter intentFilter)
	{
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
		intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		intentFilter.addAction(OfflineConstants.WIFI_HOTSPOT_STATE_CHANGE_ACTION);
	}

	@Override
	public void onConnetionToWifiNetwork()
	{
		String offlineNetworkMsisdn = connectionManager.getConnectedHikeNetworkMsisdn();
		Logger.d(TAG, "CheckConnectedNetwork");

		if (offlineNetworkMsisdn != null && connectedDevice == null && 
				OfflineController.getInstance().getOfflineState() == OFFLINE_STATE.CONNECTING)
		{
			connectedDevice = offlineNetworkMsisdn;
			initClientConfig(connectedDevice);
			Logger.d(TAG, "Starting as Client");
			transporter.initAsClient(transporterConfig, context,messageSentCallback,messageReceivedCallback,this,handler.getLooper(), HikeOfflinePersistence.getInstance());
		}
	}

	private void initClientConfig(String namespace) 
	{
		Logger.d(TAG, "Initialising client config!");
		topics  =  new ArrayList<Topic>();
		Topic textTopic = new Topic(OfflineConstants.TEXT_TOPIC);
		Topic fileTopic =  new Topic(OfflineConstants.FILE_TOPIC);
		topics.add(textTopic);
		topics.add(fileTopic);
		DefaultRetryPolicy retryPolicy = new DefaultRetryPolicy(4, 500, 1);
		transporterConfig = new Config.ConfigBuilder(topics,connectionManager.getHostAddress(),OfflineConstants.PORT_PING,OfflineConstants.TEXT_TOPIC).
							sendoldPersistedMessages(true).nameSpace(namespace).setRetryPolicy(retryPolicy).build();
	}

	public void removeMessage(int msg)
	{
		handler.removeMessages(msg);
	}

	public void removeAllMessages()
	{
		handler.removeCallbacksAndMessages(null);
		Logger.d(TAG, "Checking ghost packeted runnable is present or not "+handler.hasMessages(HandlerConstants.SEND_GHOST_PACKET));
	}

	@Override
	public void onDiscoveryStarted()
	{

	}

	@Override
	public void onDiscoveryStopped()
	{

	}

	public void startDiscovery()
	{
		connectionManager.startDiscovery();
	}

	public void stopDiscovery()
	{
		connectionManager.stopDiscovery();
	}

	public void createHotspot(final String msisdn)
	{
		Message msg = Message.obtain();
		msg.what = OfflineConstants.HandlerConstants.CREATE_HOTSPOT;
		msg.obj = msisdn;
		performWorkOnBackEndThread(msg);
		initServerConfig(msisdn);
		Logger.d(TAG, "Starting server!");
		transporter.initAsServer(transporterConfig,context,messageSentCallback,messageReceivedCallback,this,handler.getLooper(), HikeOfflinePersistence.getInstance());
	}

	private void initServerConfig(String namespace) 
	{
		topics  =  new ArrayList<Topic>();
		Topic textTopic = new Topic(OfflineConstants.TEXT_TOPIC);
		Topic fileTopic =  new Topic(OfflineConstants.FILE_TOPIC);
		topics.add(textTopic);
		topics.add(fileTopic);
		transporterConfig = new Config.ConfigBuilder(topics, OfflineConstants.IP_SERVER, OfflineConstants.PORT_PING,OfflineConstants.TEXT_TOPIC).sendoldPersistedMessages(true).nameSpace(namespace).build();
	}

	public void connectToHotspot(String msisdn)
	{
		//threadManager.startReceivingThreads();
		connectionManager.connectToHotspot(msisdn);
	}

	String connectinMsisdn = "";

	public void connectAsPerMsisdn(final String msisdn)
	{
		OFFLINE_STATE offlineState = OfflineController.getInstance().getOfflineState();
		if (offlineState == OFFLINE_STATE.CONNECTING)
		{
			HikeMessengerApp.getInstance().showToast("We are already connecting");
			return;
		}
		if(offlineState == OFFLINE_STATE.CONNECTED)
		{
			HikeMessengerApp.getInstance().showToast("We are already connected.Kindly disconnect first and then reconnect");
			return;
		}
		
		OfflineController.getInstance().setOfflineState(OFFLINE_STATE.CONNECTING);
		String myMsisdn = OfflineUtils.getMyMsisdn();
		Message endTries = Message.obtain();
		endTries.what = OfflineConstants.HandlerConstants.REMOVE_CONNECT_MESSAGE;
		endTries.obj = msisdn;
		if (myMsisdn.compareTo(msisdn) > 0)
		{
			Logger.d(TAG, "Will create Hotspot");
			connectinMsisdn=msisdn;
			createHotspot(msisdn);
			handler.sendMessageDelayed(endTries, OfflineConstants.TIME_TO_CONNECT);
		}
		else
		{
			Logger.d(TAG, "Will connect to  Hotspot");
			connectinMsisdn = msisdn;
			Message msg = Message.obtain();
			msg.what = OfflineConstants.HandlerConstants.CONNECT_TO_HOTSPOT;
			msg.obj = msisdn;
			performWorkOnBackEndThread(msg);
			// removing the CONNECT_TO_HOTSPOT message from handler after timeout
			handler.sendMessageDelayed(endTries, OfflineConstants.TIME_TO_CONNECT);
		}
	}

	public boolean isHotspotCreated()
	{
		return connectionManager.isHotspotCreated();
	}

	public void startWifiScan()
	{
		connectionManager.startWifiScan();
	}

	public void startWifi()
	{
		connectionManager.startWifi();
	}

	public void stopWifi()
	{
		connectionManager.stopWifi();
	}

	public void startScan()
	{
		Message startScan = Message.obtain();
		startScan.what = OfflineConstants.HandlerConstants.START_SCAN;
		startScan.obj = tryGetScanResults;
		performWorkOnBackEndThread(startScan);
	}

	private void runNetworkScan(int attemptNumber)
	{

		if (attemptNumber < OfflineConstants.MAXTRIES_FOR_SCAN_RESULTS || (scanResultsAvailable == true))
		{
			connectionManager.startDiscovery();
			try
			{
				Thread.sleep(8000);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			connectionManager.stopDiscovery();
			try
			{
				Thread.sleep(2000);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}

			connectionManager.startWifiScan();
		}
		else
		{
			connectionManager.stopWifi();

			while (connectionManager.isWifiEnabled())
			{
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				Logger.d(TAG, "Waiting for wifi to stop");
			}
			connectionManager.startWifi();
			Logger.d(TAG, "Called start wifi");
			while (!connectionManager.isWifiEnabled())
			{
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				Logger.d(TAG, "Waiting for wifi to start");
			}
			Logger.d(TAG, "Wifi is on, now scanning for any available wifi hotspots");
			connectionManager.startWifiScan();
		}
	}

	public void stopScan()
	{
		removeMessage(OfflineConstants.HandlerConstants.START_SCAN);
	}

	public void removeListener(IOfflineCallbacks listener)
	{
		if (listeners.contains(listener))
		{
			listeners.remove(listener);
		}
	}

	public void clearAllVariables()
	{
		connectedDevice = null;
		connectinMsisdn = null;
		removeAllMessages();
		startedForChatThread = false;
	}

	public void setConnectedDevice(String connectedDevice)
	{
		this.connectedDevice = connectedDevice;
	}	

	@Override
	public void onConnect() 
	{
		OfflineController.getInstance().onConnect();
	}

	protected void sendInfoPacket()
	{
		SenderConsignment senderConsignment = new SenderConsignment.Builder(OfflineUtils.createInfoPkt().toString(), OfflineConstants.TEXT_TOPIC).ackRequired(false)
				.persistance(false).build();
		sendConsignment(senderConsignment);
	}

	// delegate to Controller
	@Override
	public void onDisconnect(TException tException) 
	{
		OfflineController.getInstance().shutdownProcess(tException);
	}


	public void sendConsignment(SenderConsignment senderConsignment) 
	{
		Logger.d(TAG, "Going to publish sender consigment " + senderConsignment.toJSONString());
		transporter.publish(senderConsignment);
	}

	public void disconnectAfterTimeout() {
		Message msg = Message.obtain();
		msg.what = OfflineConstants.HandlerConstants.DISCONNECT_BY_USER;
		msg.obj = null;
		handler.sendMessageDelayed(msg,OfflineConstants.WAITING_TIME_TO_DISCONNECT);
	}
	
	public void releaseResources()
	{
		String targetMsisdn = connectedDevice;
		if (TextUtils.isEmpty(targetMsisdn))
			targetMsisdn = connectinMsisdn;
		connectionManager.closeConnection(targetMsisdn);
		clearAllVariables();
	}


	public void updateListeners(OFFLINE_STATE offline_STATE) {

		// to avoid ConcurrentModificationException we use a cloned list of listeners
		// for traversing.
		ArrayList<IOfflineCallbacks> clonedListeners = new ArrayList<>();
		for(IOfflineCallbacks offlineListener : listeners)
		{
			clonedListeners.add(offlineListener);
		}
		
		if (offline_STATE == OFFLINE_STATE.CONNECTED)
		{
			for (IOfflineCallbacks offlineListener : clonedListeners)
			{
				offlineListener.onDisconnect(ERRORCODE.USERDISCONNECTED);
			}
		}
		else if (offline_STATE == OFFLINE_STATE.CONNECTING)
		{
			for (IOfflineCallbacks offlineListener : clonedListeners)
			{
				offlineListener.onDisconnect(ERRORCODE.TIMEOUT);
			}
		}
	}


	public void setConnectingDeviceAsConnected() {
		this.connectedDevice=connectinMsisdn;
	}


	public void sendConnectedCallback() 
	{
		for (IOfflineCallbacks offlineListener : listeners)
		{
			offlineListener.connectedToMsisdn(getConnectedDevice());
		}
	}

	public void removeMessageFromOfflinePersistance(String msisdn) {
		transporter.deleteFromPersistance(msisdn);
	}

	public String getConnectingDevice() 
	{
		return connectinMsisdn;
	}


}