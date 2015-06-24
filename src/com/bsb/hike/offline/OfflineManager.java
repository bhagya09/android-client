package com.bsb.hike.offline;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

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
import android.view.View;
import android.widget.ImageView.ScaleType;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessagesAdapter.FTViewHolder;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeOfflinePersistence;
import com.bsb.hike.filetransfer.FileSavedState;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.offline.OfflineConstants.ERRORCODE;
import com.bsb.hike.offline.OfflineConstants.HandlerConstants;
import com.bsb.hike.offline.OfflineConstants.OFFLINE_STATE;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.hike.transporter.TException;
import com.hike.transporter.Transporter;
import com.hike.transporter.interfaces.IConnectionListener;
import com.hike.transporter.models.Config;
import com.hike.transporter.models.SenderConsignment;
import com.hike.transporter.models.Topic;
import com.squareup.okhttp.internal.http.Transport;

/**
 * 
 * @author himanshu, deepak malik , sahil This class forms the base of Offline Messaging and deals with socket connection,text transfer and file transfer queue.
 */

public class OfflineManager implements IWIfiReceiverCallback, PeerListListener,IConnectionListener
{
	private static OfflineManager _instance = null;

	private ArrayList<IOfflineCallbacks> listeners;

	private volatile boolean inFileTransferInProgress = false;

	private Context context;

	private volatile String connectedDevice = null;

	private ConnectionManager connectionManager;

	private static final String TAG = OfflineManager.class.getName();

	private boolean scanResultsAvailable = false;

	private boolean isConnectedToHotspot = false;

	private int tryGetScanResults = 0;

	OfflineBroadCastReceiver receiver;

	private volatile OFFLINE_STATE offlineState;

	private boolean startedForChatThread = false;
	
	private HikeConverter hikeConverter;
	
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

	private OfflineManager()
	{
		init();
	}

	public static OfflineManager getInstance()
	{
		if (_instance == null)
		{
			synchronized (OfflineManager.class)
			{
				if (_instance == null)
				{
					_instance = new OfflineManager();

				}
			}
		}
		return _instance;
	}

	private void handleMsgOnBackEndThread(Message msg)
	{
		switch (msg.what)
		{
		case OfflineConstants.HandlerConstants.SAVE_MSG_DB:
			saveToDb((ConvMessage) msg.obj);
			break;
		case OfflineConstants.HandlerConstants.DISCONNECT_AFTER_TIMEOUT:
			disconnect((String) msg.obj);
			break;
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
				shutDown(new OfflineException(OfflineException.CONNECTION_TIME_OUT));
			}
			break;
		case OfflineConstants.HandlerConstants.START_SCAN:
			runNetworkScan((int) msg.obj);
			msg.obj = ((int) msg.obj) + 1;
			performWorkOnBackEndThread(msg);
			break;
		case OfflineConstants.HandlerConstants.SHUTDOWN:
			shutDownProcess((OfflineException) msg.obj);
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

	private void saveToDb(ConvMessage convMessage)
	{
		HikeConversationsDatabase.getInstance().addConversationMessages(convMessage, true);
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
		connectionManager = ConnectionManager.getInstance();
		hikeConverter =  HikeConverter.getInstance();
		transporter = Transporter.getInstance();
		listeners = new ArrayList<IOfflineCallbacks>();
		setDeviceNameAsMsisdn();
		receiver = new OfflineBroadCastReceiver(this);
		setOfflineState(OFFLINE_STATE.NOT_CONNECTED);
		Logger.d(TAG, "Contructor called");
	}

	private void setDeviceNameAsMsisdn()
	{
		// TODO : Restore back to previous deviceName
		connectionManager.setDeviceNameAsMsisdn();
	}

	public void disconnect(String msisdn)
	{
		// Since disconnect is called, stop sending ghost packets
		//removeMessage(OfflineConstants.HandlerConstants.SEND_GHOST_PACKET);
		shutDown(new OfflineException(OfflineException.GHOST_PACKET_NOT_RECEIVED));

	}
	
	@Override
	public void onRequestPeers()
	{
		connectionManager.requestPeers(this);
	}
	
	public void setInOfflineFileTransferInProgress(boolean val)
	{
		this.inFileTransferInProgress = val;
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
				
				// since we already have the result no need to scan again
				removeMessage(OfflineConstants.HandlerConstants.RECONNECT_TO_HOTSPOT);
				removeMessage(OfflineConstants.HandlerConstants.REMOVE_CONNECT_MESSAGE);
				Logger.d(TAG, "Removed callback for disconnect");
				
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
	public void checkConnectedNetwork()
	{
		String offlineNetworkMsisdn = connectionManager.getConnectedHikeNetworkMsisdn();
		Logger.d(TAG, "CheckConnectedNetwork");

		if (offlineNetworkMsisdn != null && connectedDevice == null && offlineState == OFFLINE_STATE.CONNECTING)
		{
			connectedDevice = offlineNetworkMsisdn;
			initClientConfig();
			transporter.initAsClient(transporterConfig, context,hikeConverter,hikeConverter,this,handler.getLooper());
		}
	}

	private void initClientConfig() {
		topics  =  new ArrayList<Topic>();
		Topic textTopic = new Topic(OfflineConstants.TEXT_TOPIC);
		Topic fileTopic =  new Topic(OfflineConstants.FILE_TOPIC);
		topics.add(textTopic);
		topics.add(fileTopic);
		transporterConfig =  new Config.ConfigBuilder(topics,connectionManager.getHostAddress(),OfflineConstants.PORT_PING,OfflineConstants.TEXT_TOPIC).sendoldPersistedMessages(false).build();
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
		initServerConfig();
		transporter.initAsServer(transporterConfig,context,hikeConverter,hikeConverter,this,handler.getLooper());
	}

	private void initServerConfig() {
		topics  =  new ArrayList<Topic>();
		Topic textTopic = new Topic(OfflineConstants.TEXT_TOPIC);
		Topic fileTopic =  new Topic(OfflineConstants.FILE_TOPIC);
		topics.add(textTopic);
		topics.add(fileTopic);
		transporterConfig = new Config.ConfigBuilder(topics, OfflineConstants.IP_SERVER, OfflineConstants.PORT_PING,OfflineConstants.TEXT_TOPIC).sendoldPersistedMessages(false).build();
	}

	public void connectToHotspot(String msisdn)
	{
		//threadManager.startReceivingThreads();
		connectionManager.connectToHotspot(msisdn);
	}

	String connectinMsisdn = "";

	public void connectAsPerMsisdn(final String msisdn)
	{
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
		setOfflineState(OFFLINE_STATE.CONNECTING);
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

	public void setConnectingMsisdnAsConnectedDevice()
	{
		this.connectedDevice=connectinMsisdn;
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

	public void setOfflineState(OFFLINE_STATE offlineState)
	{
		this.offlineState = offlineState;
		Logger.d("OfflineManager", "Offline state is " + offlineState);
	}

	public OFFLINE_STATE getOfflineState()
	{
		return offlineState;
	}

	public void removeListener(IOfflineCallbacks listener)
	{
		if (listeners.contains(listener))
		{
			listeners.remove(listener);
		}
	}

	public synchronized void shutDown(OfflineException exception)
	{
		Logger.d(TAG, "ShudDown called Due to reason " + exception.getReasonCode());

		
		Message msg=Message.obtain();
		msg.what=HandlerConstants.SHUTDOWN;
		msg.obj=exception;
		performWorkOnBackEndThread(msg);
	}

	public void shutDownProcess(OfflineException exception)
	{
		if (getOfflineState() != OFFLINE_STATE.DISCONNECTED)
		{
			HikeMessengerApp.getInstance().showToast("Disconnected Reason " + exception.getReasonCode());
			sendDisconnectToListeners();

			setOfflineState(OFFLINE_STATE.DISCONNECTED);

			//currentReceivingFiles.clear();
			//currentSendingFiles.clear();

			// if a sending file didn't go change from spinner to retry button
			HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);

			connectionManager.closeConnection(getConnectedDevice());

			clearAllVariables();
		}
	}
	private void sendDisconnectToListeners()
	{
		if (getOfflineState() == OFFLINE_STATE.CONNECTED)
		{
			hikeConverter.deleteRemainingReceivingFiles();
			final ConvMessage convMessage = OfflineUtils.createOfflineInlineConvMessage("o:" + connectedDevice, context.getString(R.string.connection_deestablished),
					OfflineConstants.OFFLINE_MESSAGE_DISCONNECTED_TYPE);
			HikeConversationsDatabase.getInstance().addConversationMessages(convMessage, true);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
			
			for (IOfflineCallbacks offlineListener : listeners)
			{
				offlineListener.onDisconnect(ERRORCODE.USERDISCONNECTED);
			}
		}

		if (getOfflineState() == OFFLINE_STATE.CONNECTING)
		{
			for (IOfflineCallbacks offlineListener : listeners)
			{
				offlineListener.onDisconnect(ERRORCODE.TIMEOUT);
			}
		}

	}

	public void clearAllVariables()
	{
		connectedDevice = null;
		connectinMsisdn = null;
		setInOfflineFileTransferInProgress(false);
		removeAllMessages();
		startedForChatThread = false;
	}

	public void setConnectedDevice(String connectedDevice2)
	{
		this.connectedDevice = connectedDevice2;
	}	

	@Override
	public void onConnect() {
		
		this.connectedDevice=connectinMsisdn;
		removeMessage(OfflineConstants.HandlerConstants.DISCONNECT_AFTER_TIMEOUT);
		removeMessage(OfflineConstants.HandlerConstants.CONNECT_TO_HOTSPOT);
		offlineState = OFFLINE_STATE.CONNECTED;
		final ConvMessage convMessage = OfflineUtils.createOfflineInlineConvMessage("o:" + connectedDevice, context.getString(R.string.connection_established),
				OfflineConstants.OFFLINE_MESSAGE_CONNECTED_TYPE);
		HikeConversationsDatabase.getInstance().addConversationMessages(convMessage, true);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
		for (IOfflineCallbacks offlineListener : listeners)
		{
			offlineListener.connectedToMsisdn(connectedDevice);
		}
	}

	@Override
	public void onDisconnect(TException tException) {
		if (getOfflineState() != OFFLINE_STATE.DISCONNECTED)
		{
			HikeMessengerApp.getInstance().showToast("Disconnected Reason " + tException.getReasonCode());
			sendDisconnectToListeners();
			
			setOfflineState(OFFLINE_STATE.DISCONNECTED);
			
			hikeConverter.shutDown(tException);
			
			// if a sending file didn't go change from spinner to retry button
			HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
			
			connectionManager.closeConnection(getConnectedDevice());
			clearAllVariables();
		}
	}

	public void sendConsignment(SenderConsignment senderConsignment) {
		transporter.publish(senderConsignment);
	}

	public void disconnectAfterTimeout() {
		Message msg = Message.obtain();
		msg.what = OfflineConstants.HandlerConstants.DISCONNECT_BY_USER;
		msg.obj = null;
		handler.sendMessageDelayed(msg,OfflineConstants.WAITING_TIME_TO_DISCONNECT);
	}

}