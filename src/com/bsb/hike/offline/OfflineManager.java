package com.bsb.hike.offline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

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

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeOfflinePersistence;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.OfflineConstants.ERRORCODE;
import com.bsb.hike.offline.OfflineConstants.HandlerConstants;
import com.bsb.hike.offline.OfflineConstants.OFFLINE_STATE;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.google.ads.AdRequest.ErrorCode;
import com.google.gson.Gson;
import com.hike.transporter.DefaultRetryPolicy;
import com.hike.transporter.TException;
import com.hike.transporter.Transporter;
import com.hike.transporter.interfaces.IConnectionListener;
import com.hike.transporter.interfaces.IMessageReceived;
import com.hike.transporter.interfaces.IMessageSent;
import com.hike.transporter.models.Config;
import com.hike.transporter.models.SenderConsignment;
import com.hike.transporter.models.Topic;
import com.hike.transporter.utils.TConstants.ERRORCODES;

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

	OfflineBroadCastReceiver receiver;

	private boolean startedForChatThread = false;
	
	private Transporter transporter;
	
	private Config transporterConfig;
	
	private List<Topic> topics;
	
	private long timeTakenToEstablishConnection = 0l;
	
	private boolean isClientInitialized=false;
	
	private OfflineClientInfoPOJO connectedClientInfo =null;
	
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
		Logger.d(TAG, "Contructor called");
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
		Map<String, ScanResult> results = connectionManager.getWifiNetworksForMyMsisdn();
		Logger.d(TAG, "On scan results available .  Connected device is "+ connectedDevice +  " started for chat thread is " + startedForChatThread);
		if (startedForChatThread && connectedDevice==null)
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
			addBroadReceiver();
		}
	}

	public void addBroadReceiver()
	{
		if (receiver == null)
		{
			receiver = new OfflineBroadCastReceiver(this);
			IntentFilter intentFilter = new IntentFilter();
			addIntentFilters(intentFilter);
			context.registerReceiver(receiver, intentFilter);
		}
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
	public void onConnectionToWifiNetwork()
	{
		String offlineNetworkMsisdn = connectionManager.getConnectedHikeNetworkMsisdn();
		Logger.d(TAG, "CheckConnectedNetwork");

		if (offlineNetworkMsisdn != null &&  !isClientInitialized && 
				OfflineController.getInstance().getOfflineState() == OFFLINE_STATE.CONNECTING)
		{
			isClientInitialized=true;
			// now transporter initAsClient is on Handler Thread/ backend thread
			final IConnectionListener listener = this;
			handler.post(new Runnable()
			{	
				@Override
				public void run()
				{
					if (initClientConfig(connectinMsisdn))
					{
						Logger.d(TAG, "Starting as Client");
						if (OfflineController.getInstance().getOfflineState() ==  OFFLINE_STATE.CONNECTING)
							transporter.initAsClient(transporterConfig, context,messageSentCallback,messageReceivedCallback, listener,handler.getLooper(), HikeOfflinePersistence.getInstance());
					}
					else
					{
						OfflineController.getInstance().shutdownProcess(new OfflineException(OfflineException.AP_IP_NOT_AVAILABLE));
					}
				}
			});
		}
	}

	private boolean initClientConfig(String namespace) 
	{
		OfflineParameters offlineParameters = new Gson().fromJson(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.OFFLINE, "{}"), OfflineParameters.class);
		Logger.d(TAG, "Initialising client config!");
		topics = new ArrayList<Topic>();
		Topic textTopic = new Topic(OfflineConstants.TEXT_TOPIC);
		Topic fileTopic = new Topic(OfflineConstants.FILE_TOPIC);
		topics.add(textTopic);
		topics.add(fileTopic);
		DefaultRetryPolicy retryPolicy = new DefaultRetryPolicy(4, 500, 1);
		String host = connectionManager.getHostAddress();
		Logger.d(TAG, "host is: " + host);
		if (host != null)
		{
			transporterConfig = new Config.ConfigBuilder(topics, host, offlineParameters.getPortNo(), OfflineConstants.TEXT_TOPIC)
					.sendoldPersistedMessages(true).nameSpace(namespace).setRetryPolicy(retryPolicy).keepAliveTimeout(offlineParameters.getHeartBeatTimeout())
					.keepAliveTimeoutScreenOff(offlineParameters.getKeepAliveScreenTimeout()).build();
			return true;
		}
		return false;
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
		OfflineParameters offlineParameters = new Gson().fromJson(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.OFFLINE, "{}"), OfflineParameters.class);
		topics  =  new ArrayList<Topic>();
		Topic textTopic = new Topic(OfflineConstants.TEXT_TOPIC);
		Topic fileTopic =  new Topic(OfflineConstants.FILE_TOPIC);
		topics.add(textTopic);
		topics.add(fileTopic);
		transporterConfig = new Config.ConfigBuilder(topics, OfflineConstants.IP_SERVER, offlineParameters.getPortNo(),OfflineConstants.TEXT_TOPIC).sendoldPersistedMessages(true).nameSpace(namespace).keepAliveTimeout(offlineParameters.getHeartBeatTimeout()).keepAliveTimeoutScreenOff(offlineParameters.getKeepAliveScreenTimeout()).build();
	}

	public void connectToHotspot(String msisdn)
	{
		//threadManager.startReceivingThreads();
		connectionManager.connectToHotspot(msisdn);
	}

	String connectinMsisdn = "";

	public void connectAsPerMsisdn(final String msisdn)
	{
		addBroadReceiver();
		timeTakenToEstablishConnection=System.currentTimeMillis();
		connectinMsisdn = msisdn;
		OfflineController.getInstance().setOfflineState(OFFLINE_STATE.CONNECTING);
		String myMsisdn = OfflineUtils.getMyMsisdn();
		Message endTries = Message.obtain();
		endTries.what = OfflineConstants.HandlerConstants.REMOVE_CONNECT_MESSAGE;
		endTries.obj = msisdn;
		OfflineParameters offlineParameters = new Gson().fromJson(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.OFFLINE, "{}"), OfflineParameters.class);
		
		startNotificationThread();
		connectionManager.updateNetworkId();
		if (myMsisdn.compareTo(msisdn) > 0)
		{
			Logger.d(TAG, "Will create Hotspot");
			createHotspot(msisdn);
			handler.sendMessageDelayed(endTries,offlineParameters.getConnectionTimeout());
		}
		else
		{
			Logger.d(TAG, "Will connect to  Hotspot");
			;
			Message msg = Message.obtain();
			msg.what = OfflineConstants.HandlerConstants.CONNECT_TO_HOTSPOT;
			msg.obj = msisdn;
			performWorkOnBackEndThread(msg);
			// removing the CONNECT_TO_HOTSPOT message from handler after timeout
			handler.sendMessageDelayed(endTries,offlineParameters.getConnectionTimeout());
			Logger.d(TAG,"time connect handler posted is "+handler.hasMessages(OfflineConstants.HandlerConstants.REMOVE_CONNECT_MESSAGE));
		}
		
	}

	private void startNotificationThread()
	{
		Thread thread=new Thread(new NotificationThread(),"Hike_Direct_Notification");
		thread.start();
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


	public void removeListener(IOfflineCallbacks listener)
	{
		if (listeners.contains(listener))
		{
			listeners.remove(listener);
		}
	}

	public void clearAllVariables()
	{
		timeTakenToEstablishConnection = 0l;
		connectedDevice = null;
		connectinMsisdn = null;
		connectedClientInfo =null;
		removeAllMessages();
		startedForChatThread = false;
		HikeSharedPreferenceUtil.getInstance().saveData(OfflineConstants.OFFLINE_MSISDN, "");
		Logger.d(TAG, "All variables cleared");
		isClientInitialized = false;
		unRegisterReceiver();
	}

	private void unRegisterReceiver()
	{
		if (receiver == null)
		{
			return;
		}

		try
		{
			context.unregisterReceiver(receiver);
		}
		catch (IllegalArgumentException e)
		{
			Logger.e(TAG, "Illegal Argument Exception in unregistering receiver");
		}
		receiver = null;
	}


	public void setConnectedDevice(String connectedDevice)
	{
		this.connectedDevice = connectedDevice;
	}	

	@Override
	public void onConnect() 
	{
		timeTakenToEstablishConnection = System.currentTimeMillis() - timeTakenToEstablishConnection;
		
		OfflineAnalytics.recordTimeForConnection(timeTakenToEstablishConnection);
		
		OfflineSessionTracking.getInstance().startTracking();
		
		OfflineController.getInstance().onConnect();
	}

	protected void sendInfoPacket(long connectId)
	{
		SenderConsignment senderConsignment = new SenderConsignment.Builder(OfflineUtils.createInfoPkt(connectId).toString(), OfflineConstants.TEXT_TOPIC).ackRequired(false)
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
		if (senderConsignment == null)
			return;
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


	public void updateListeners(ERRORCODE errorCode)
	{

		// to avoid ConcurrentModificationException we use a cloned list of listeners
		// for traversing.
		ArrayList<IOfflineCallbacks> clonedListeners = new ArrayList<>();

		clonedListeners.addAll(listeners);

		for (IOfflineCallbacks offlineListener : clonedListeners)
		{
			offlineListener.onDisconnect(errorCode);
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


	public void setConnectedClientInfo(JSONObject clientInfo)
	{
		if(clientInfo==null)
			return;
		
		OfflineClientInfoPOJO  offlineClientInfoPOJO = new Gson().fromJson(clientInfo.toString(), OfflineClientInfoPOJO.class);
		connectedClientInfo = offlineClientInfoPOJO;
	}

	public OfflineClientInfoPOJO getConnectedClientInfo()
	{
		return connectedClientInfo;
	}
}