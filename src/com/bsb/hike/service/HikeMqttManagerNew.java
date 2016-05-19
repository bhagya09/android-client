package com.bsb.hike.service;

import static com.bsb.hike.MqttConstants.*;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.acra.util.HttpRequest;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttActionListenerNew;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HostInfo;
import com.bsb.hike.HostInfo.ConnectExceptions;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.AnalyticsConstants.MsgRelEventType;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.analytics.MsgRelLogManager;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.db.HikeMqttPersistence;
import com.bsb.hike.db.MqttPersistenceException;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikePacket;
import com.bsb.hike.models.NetInfo;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.offline.OfflineConstants.OFFLINE_STATE;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.offline.OfflineManager;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSSLUtil;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.Utils;

/**
 * Author : GK
 * 
 * This class handles all the MQTT related stuff. Every operation is non blocking and run on dedicated MQTT Thread. Function 'scheduleConnCheck' checks for connection after certain
 * interval of time. All pings are handled by mqtt paho internally. As soon as you get connected simply reschdule next conn check. In case of no netowrk and SERVER unavailable , we
 * should try and connect on exponential basis.
 * */
public class HikeMqttManagerNew extends BroadcastReceiver
{
	private static final String TAG = "HikeMqttManagerNew";
	
	// this variable when true, does not allow mqtt operation such as publish or connect
	// this will become true when you force close or force disconnect mqtt (ex : ssl toggle)
	private boolean forceDisconnect = false;

	private MqttAsyncClient mqtt = null; // main class for handling mqtt connections

	private MqttConnectOptions op = null; // options to set when you connect to mqtt broker

	private Context context;

	private SharedPreferences settings;

	private String brokerHostName;

	private String clientId;

	private volatile boolean pushConnect = false;

	private String uid;

	private String topic;

	private String password;

	private int brokerPortNumber;

	private volatile AtomicBoolean haveUnsentMessages = new AtomicBoolean(false);

	private volatile AtomicBoolean initialised = new AtomicBoolean(false);
	
	private int reconnectTime = 0;

	private Looper mMqttHandlerLooper;

	private Handler mqttThreadHandler;

	private MqttCallback mqttCallBack;

	private IMqttActionListener listernerConnect;

	private MqttMessagesManager mqttMessageManager;

	private IsMqttConnectedCheckRunnable isConnRunnable;

	private ConnectionCheckRunnable connChkRunnable;

	private DisconnectRunnable disConnectRunnable;

	private ActivityCheckRunnable activityChkRunnable;

	private HikeMqttPersistence persistence = null;

	private WakeLock wakelock = null;

	private volatile MQTTConnectionStatus mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED;

	private Messenger mMessenger; // this is used to interact with the mqtt thread

	List<String> serverURIs = null;
	
	ArrayList<Integer> serverPorts = null;

	private volatile short fastReconnect = 0;

	private volatile int retryCount = 0;
	
	private NetInfo previousNetInfo;

	private long maxMessageProcessTime = 0;
	
	private volatile HostInfo previousHostInfo = null;
	
	private ScreenOnOffReceiver screenOnOffReceiver;
	
	private class ActivityCheckRunnable implements Runnable
	{
		@Override
		public void run()
		{
			if (isConnected() || isConnecting())
			{
				try
				{
					mqtt.checkActivity();
					scheduleNextActivityCheck();
				}
				catch (Exception e)
				{
					Logger.e(TAG, "Exception in ActivityCheckRunnable", e);
				}
			}
		}
	}

	private class IsMqttConnectedCheckRunnable implements Runnable
	{
		@Override
		public void run()
		{
			if (!isConnected())
			{
				HikeMessengerApp.networkError = true;
				HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_NETWORK_STATE, null);
			}
		}
	}

	// this is used to check and connect mqtt and will be run on MQTT thread
	private class ConnectionCheckRunnable implements Runnable
	{
		private long sleepTime = 0;

		public void setSleepTime(long t)
		{
			sleepTime = t;
		}

		@Override
		public void run()
		{
			if (sleepTime > 0)
			{
				try
				{
					Thread.sleep(sleepTime);
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				sleepTime = 0;
			}
			connect();
		}
	}

	// this is used to disconnect from mqtt broker and will be run on MQTT thread
	private class DisconnectRunnable implements Runnable
	{
		private boolean reconnect = true;

		public void setReconnect(boolean isReconnect)
		{
			reconnect = isReconnect;
		}

		@Override
		public void run()
		{
			disconnect(reconnect);
			reconnect = true; // resetting value after run
		}
	}

	private final class RetryFailedMessages implements Runnable
	{
		public void run()
		{
			final List<HikePacket> packets = persistence.getAllSentMessages();
			Logger.w(TAG, "Retrying to send " + packets.size() + " messages");
			for (HikePacket hikePacket : packets)
			{
				Logger.d(TAG, "Resending message " + new String(hikePacket.getMessage()));
				send(hikePacket, 1);
				try
				{
					// always give some time for network call to complete
					// lopping this way could exceed the memory too as GC will block
					Thread.sleep(20);
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			haveUnsentMessages.set(false);
		}
	}

	class IncomingHandler extends Handler
	{
		public IncomingHandler(Looper looper)
		{
			super(looper);
		}

		@Override
		public void handleMessage(Message msg)
		{
			try
			{
				switch (msg.what)
				{
				case HikeService.MSG_APP_PUBLISH:
					Bundle bundle = msg.getData();
					HikePacket packet = bundle.getParcelable(HikeConstants.MESSAGE);
					send(packet, msg.arg1);
					break;
				case 12341: // just for testing
					Bundle b = msg.getData();
					String m = b.getString(HikeConstants.MESSAGE);
					long mId = b.getLong(HikeConstants.MESSAGE_ID, -1);
					send(new HikePacket(m.getBytes(), mId, System.currentTimeMillis(), msg.arg2), msg.arg1);
					break;
				}
			}
			catch (Exception e)
			{
				Logger.e(TAG, "Exception", e);
			}
		}
	}

	private HikeMqttManagerNew()
	{
	}

	/*
	 * This inner class is not loaded until getInstance is called.
	 * Also, the class initialization of InstanceHolder is thread safe implicitly.
	 */
	private static class InstanceHolder
	{
		private static final HikeMqttManagerNew INSTANCE = new HikeMqttManagerNew();
	}

	public static HikeMqttManagerNew getInstance()
	{
		return InstanceHolder.INSTANCE;
	}

	/**
	 * This method should be used after creating this object. Note : Functions involving 'this' reference and Threads should not be used or started in constructor as it might
	 * happen that incomplete 'this' object creation took place till that time.
	 * 
	 * @return true if successfully initialized false otherwise
	 */
	public boolean init()
	{
		
		// If user is not signed up. Do not initialize MQTT
		if (!Utils.shouldConnectToMQTT())
		{
			Logger.wtf(TAG, "Not connecting to MQ because user is not signed up or db went kaput!");
			return false;
		}

		if(initialised.get())
		{
			Logger.d(TAG, "Already initialised , return now..");
			return true;
		}
		
		context = HikeMessengerApp.getInstance();
		settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		password = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		topic = uid = settings.getString(HikeMessengerApp.UID_SETTING, null);
		clientId = settings.getString(HikeMessengerApp.MSISDN_SETTING, null) + ":" + HikeConstants.APP_API_VERSION + ":" + true;

		persistence = HikeMqttPersistence.getInstance();
		mqttMessageManager = MqttMessagesManager.getInstance(context);

		maxMessageProcessTime = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.MAX_MESSAGE_PROCESS_TIME, DEFAULT_MAX_MESSAGE_PROCESS_TIME);
		
		createConnectionRunnables();

		initMqttHandlerThread();

		registerBroadcastReceivers();

		setServerUris();
		setServerPorts();
		// mqttThreadHandler.postDelayed(new TestOutmsgs(), 10 * 1000); // this is just for testing
		
		initialised.getAndSet(true);
		return true;
	}

	private void createConnectionRunnables()
	{
		isConnRunnable = new IsMqttConnectedCheckRunnable();
		connChkRunnable = new ConnectionCheckRunnable();
		disConnectRunnable = new DisconnectRunnable();
		activityChkRunnable = new ActivityCheckRunnable();
	}

	private void initMqttHandlerThread()
	{
		HandlerThread mqttHandlerThread = new HandlerThread("MQTT_Thread");
		mqttHandlerThread.start();
		mMqttHandlerLooper = mqttHandlerThread.getLooper();
		mqttThreadHandler = new Handler(mMqttHandlerLooper);
		mMessenger = new Messenger(new IncomingHandler(mMqttHandlerLooper));
	}

	private void registerBroadcastReceivers()
	{
		// register for Screen ON, Network Connection Change
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		filter.addAction(MQTT_CONNECTION_CHECK_ACTION);
		filter.addAction(HikePubSub.SSL_PREFERENCE_CHANGED);
		filter.addAction(HikePubSub.IPS_CHANGED);
		filter.addAction(HikePubSub.PORTS_CHANGED);
		context.registerReceiver(this, filter);
		LocalBroadcastManager.getInstance(context).registerReceiver(this, filter);
		
		screenOnOffReceiver = new ScreenOnOffReceiver();
		IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
		context.registerReceiver(screenOnOffReceiver, intentFilter);

	}

	private int getConnRetryTime()
	{
		return getConnRetryTime(false);
	}

	// this function works on exponential retrying
	private int getConnRetryTime(boolean forceExp)
	{
		if ((reconnectTime == 0 || retryCount < MAX_RETRY_COUNT) && !forceExp)
		{
			Random random = new Random();
			reconnectTime = random.nextInt(HikeConstants.RECONNECT_TIME) + 1;
			retryCount++;
		}
		else
		{
			reconnectTime *= 2;
		}
		// if reconnectTime is 0, select the random value. This will happen in case of forceExp = true
		reconnectTime = reconnectTime > HikeConstants.MAX_RECONNECT_TIME ? HikeConstants.MAX_RECONNECT_TIME
				: (reconnectTime == 0 ? (new Random()).nextInt(HikeConstants.RECONNECT_TIME) + 1 : reconnectTime);
		return reconnectTime;
	}

	// delete the token and send a message to the app to send the user back to the main screen
	private void clearSettings()
	{
		Logger.e(TAG, "Invalid account credentials, so clear settings and move to welcome screen.");
		SharedPreferences.Editor editor = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
		editor.clear();
		editor.commit();
	}

	public Messenger getMessenger()
	{
		return mMessenger;
	}

	private void acquireWakeLock()
	{
		if (wakelock == null)
		{
			PowerManager pm = (PowerManager) context.getSystemService(Service.POWER_SERVICE);
			wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
			wakelock.setReferenceCounted(false);
		}
		wakelock.acquire();
	}

	/**
	 * This function will be used when wakeLock is taken during connect as
	 * 
	 * timeout : seconds
	 */
	private void acquireWakeLock(int timeout)
	{
		if (timeout > 0)
		{
			if (wakelock == null)
			{
				PowerManager pm = (PowerManager) context.getSystemService(Service.POWER_SERVICE);
				wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
				wakelock.setReferenceCounted(false);
			}
			wakelock.acquire(timeout * 1000);
		}
		else
			acquireWakeLock();
		Logger.d(TAG, "Wakelock Acquired");
	}

	private void releaseWakeLock()
	{
		if (wakelock != null && wakelock.isHeld())
		{
			wakelock.release();
			Logger.d(TAG, "Wakelock Released");
		}
	}

	private void scheduleNextActivityCheck()
	{
		try
		{
			mqttThreadHandler.removeCallbacks(activityChkRunnable);
			mqttThreadHandler.postDelayed(activityChkRunnable, 62 * 1000);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void scheduleNextConnectionCheck()
	{
		scheduleNextConnectionCheck(HikeConstants.MAX_RECONNECT_TIME);
	}

	private void scheduleNextConnectionCheck(int reconnectTime)
	{
		try
		{
			mqttThreadHandler.removeCallbacks(connChkRunnable);
			mqttThreadHandler.postDelayed(connChkRunnable, reconnectTime * 1000);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private boolean isConnected()
	{
		if (mqtt == null)
			return false;
		if (!mqtt.isConnected())
		{
			mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED_UNKNOWN_REASON;
			return false;
		}
		return mqtt.isConnected();
	}

	private boolean isConnecting()
	{
		if (mqtt == null)
			return false;
		return mqtt.isConnecting();
	}

	private boolean isDisconnecting()
	{
		if (mqtt == null)
			return false;
		return mqtt.isDisconnecting();
	}

	// This function should be called always from external classes inorder to run connect on MQTT thread
	public void connectOnMqttThread()
	{
		try
		{
			connectOnMqttThread(MQTT_WAIT_BEFORE_RECONNECT_TIME);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in MQTT connect handler: " + e.getMessage());
		}
	}

	private void connectOnMqttThread(long t)
	{
		try
		{
			// make MQTT thread wait for t ms to attempt reconnect
			// remove any pending disconnect runnables before making any connection
			connChkRunnable.setSleepTime(t);
			mqttThreadHandler.removeCallbacks(disConnectRunnable);
			mqttThreadHandler.postAtFrontOfQueue(connChkRunnable);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in MQTT connect handler: " + e.getMessage());
		}
	}

	private void scheduleNetworkErrorTimer()
	{
		if (HikeMessengerApp.networkError == true)
			return;

		if (isAirplaneModeOn(context))
		{
			HikeMessengerApp.networkError = true;
			HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_NETWORK_STATE, null);
			return;
		}
		mqttThreadHandler.postDelayed(isConnRunnable, HikeConstants.NETWORK_ERROR_POP_UP_TIME);
	}

	private void cancelNetworkErrorTimer()
	{
		mqttThreadHandler.removeCallbacks(isConnRunnable);
		if (HikeMessengerApp.networkError == false)
			return;
		HikeMessengerApp.networkError = false;
		HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_NETWORK_STATE, null);
	}

	private static boolean isAirplaneModeOn(Context context)
	{
		return Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) != 0;
	}

	// this should and will run only on MQTT thread so no need to synchronize it explicitly
	private void connect()
	{
		try
		{
			
			if(!Utils.isUserAuthenticated(context))
			{
				Logger.d(TAG, "User not Authenticated");
				return;
			}
			
			if (!Utils.isUserOnline(context))
			{
				Logger.d(TAG, "No Network Connection so should not connect");
				return;
			}

			// if force disconnect is in progress don't connect
			if (forceDisconnect)
				return;

			if (op == null)
			{
				op = new MqttConnectOptions();
				op.setUserName(uid);
				op.setPassword(password.toCharArray());
				op.setCleanSession(true);
				op.setKeepAliveInterval((short) KEEP_ALIVE_SECONDS);
			}

			// get host information for this connect try, which in turn is based on
			// previous host informartion
			HostInfo hostInfo = new HostInfo(previousHostInfo, serverURIs, serverPorts);
			if (mqtt == null)
			{
				// Here I am using my modified MQTT PAHO library
				Logger.d("HikeMqttManagerNew"+hostInfo.getServerUri(), clientId + ":" + pushConnect + ":" + fastReconnect + ":" + Utils.getNetworkType(context)+":"+hostInfo.getPort());
				mqtt = new MqttAsyncClient(hostInfo.getServerUri(), clientId + ":" + pushConnect + ":" + fastReconnect + ":" + Utils.getNetworkType(context)+":"+hostInfo.getPort(), null,
						MAX_INFLIGHT_MESSAGES_ALLOWED);
				mqtt.setCallback(getMqttCallback());
				Logger.d(TAG, "Number of max inflight msgs allowed : " + mqtt.getMaxflightMessages());
			}
			if (isConnected() || isConnecting() || isDisconnecting())
				return;

			mqttConnStatus = MQTTConnectionStatus.CONNECTING;
			// if any network is available, then only connect, else connect at next check or when network gets available
			if (Utils.isUserOnline(context))
			{
				acquireWakeLock(hostInfo.getConnectTimeOut());
				Logger.d(TAG, "Connect using pushconnect : " + pushConnect + "  fast reconnect : " + fastReconnect + " connection time out = "+hostInfo.getConnectTimeOut());
				mqtt.setClientId(clientId + ":" + pushConnect + ":" + fastReconnect + ":" + Utils.getNetworkType(context)+":"+hostInfo.getPort());
				mqtt.setServerURI(hostInfo.getServerUri());
				
				//Setting some connection options which we need to reset on every connect
				if (isSSL())
					op.setSocketFactory(HikeSSLUtil.getSSLSocketFactory());
				else
					op.setSocketFactory(null);
				op.setConnectionTimeout(hostInfo.getConnectTimeOut());
				
				Logger.d(TAG, "MQTT connecting on : " + mqtt.getServerURI());
				
				previousNetInfo = NetInfo.getNetInfo(Utils.getActiveNetInfo()); // update previous netInfo
				
				previousHostInfo = hostInfo;
				
				Logger.d(TAG, "MQTT trying to connect on HostInfo : "+hostInfo.toString());
				mqtt.connect(op, null, getConnectListener());
				scheduleNextActivityCheck();
			}
			else
			{
				// if no network then should rely on network change listener
				Logger.d(TAG, "No network so not trying to connect.");
				mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED;
				// scheduleNextConnectionCheck(getConnRetryTime()); // exponential retry incase of no network
			}
		}
		catch (MqttSecurityException e)
		{
			e.printStackTrace();
			handleMqttException(e, false);
			mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED_UNKNOWN_REASON;
			releaseWakeLock();
		}
		catch (MqttException e)
		{
			Logger.e(TAG, "Connect exception : " + e.getReasonCode());
			handleMqttException(e, true);
			releaseWakeLock();
		}
		catch (Exception e) // this exception cannot be thrown on connect
		{
			mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED_UNKNOWN_REASON;
			Logger.e(TAG, "Connect exception : " + e.getMessage());
			e.printStackTrace();
			scheduleNextConnectionCheck();
			releaseWakeLock();
		}
	}

	private void setServerUris()
	{

		String ipString = settings.getString(HikeMessengerApp.MQTT_IPS, "");
		JSONArray ipArray = null;

		try
		{
			if(ipString!=null && !ipString.isEmpty())
			{
				ipArray = new JSONArray(ipString);
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		if (null != ipArray && ipArray.length() > 0)
		{
			serverURIs = new ArrayList<String>(ipArray.length() + 1);
			int len = ipArray.length();

			serverURIs.add(PRODUCTION_BROKER_HOST_NAME);
			for (int i = 0; i < len; i++)
			{
				// serverURIs[i + 1] =
				if (ipArray.optString(i) != null)
				{
					serverURIs.add(ipArray.optString(i));
				}
			}
		}
		else
		{
			serverURIs = new ArrayList<String>(9);

			serverURIs.add(PRODUCTION_BROKER_HOST_NAME);
			serverURIs.add("54.251.180.0");
			serverURIs.add("54.251.180.1");
			serverURIs.add("54.251.180.2");
			serverURIs.add("54.251.180.3");
			serverURIs.add("54.251.180.4");
			serverURIs.add("54.251.180.5");
			serverURIs.add("54.251.180.6");
			serverURIs.add("54.251.180.7");
		}

	}
	
	private void setServerPorts()
	{
		String portString = settings.getString(MqttConstants.MQTT_PORTS, "");
		JSONArray portArray = null;

		try
		{
			portArray = new JSONArray(portString);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		if (null != portArray && portArray.length() > 0)
		{
			serverPorts = new ArrayList<Integer>(portArray.length() + 1);
			int len = portArray.length();

			for (int i = 0; i < len; i++)
			{
				// serverURIs[i + 1] =
				if (portArray.optInt(i) != 0)
				{
					serverPorts.add(portArray.optInt(i));
				}
			}
		}
		
		if(serverPorts == null || serverPorts.isEmpty())
		{
			serverPorts = new ArrayList<Integer>(PRODUCTION_MQTT_CONNECT_PORTS.length);
			for (Integer defaultPort : PRODUCTION_MQTT_CONNECT_PORTS)
			{
				serverPorts.add(defaultPort);
			}
		}

	}
	
	// This function should be called always from external classes inorder to run connect on MQTT thread
	private void disconnectOnMqttThread(final boolean reconnect)
	{
		try
		{
			disConnectRunnable.setReconnect(reconnect);
			mqttThreadHandler.removeCallbacks(disConnectRunnable); // remove any pending disconnects queued
			mqttThreadHandler.removeCallbacks(connChkRunnable); // remove any pending connects queued
			mqttThreadHandler.postAtFrontOfQueue(disConnectRunnable);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in MQTT connect : " + e.getMessage());
		}
	}

	private void disconnect(final boolean reconnect)
	{
		try
		{
			if (mqtt != null)
			{
				/*
				 * If already disconnecting or disconnected no need to disconnect
				 */
				if(mqtt.isDisconnecting() || mqtt.isDisconnected())
				{
					Logger.d(TAG, "not connected but disconnecting");
					if(mqtt.isDisconnecting())
					{
						Logger.d(TAG, "already disconnecting");
					}
					else if(mqtt.isDisconnected())
					{
						Logger.d(TAG, "already disconnected");
					}
					if (reconnect)
						connectOnMqttThread(MQTT_WAIT_BEFORE_RECONNECT_TIME); // try reconnection after 10 ms
					return ;
				}
				forceDisconnect = true;
				/*
				 * blocking the mqtt thread, so that no other operation takes place till disconnects completes or timeout This will wait for max 1 secs
				 */
				mqtt.disconnectForcibly(QUIESCE_TIME_MILLS, DISCONNECT_TIMEOUT);
				handleDisconnect(reconnect);
			}
		}
		catch (MqttException e)
		{
			// we dont need to handle MQTT exception here as we reconnect depends on reconnect var
			e.printStackTrace();
			handleDisconnect(reconnect);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			handleDisconnect(reconnect);
		}
	}

	private void handleDisconnect(boolean reconnect)
	{
		resetConnectionVariables();
		try
		{
			if(null != mqtt)
			{
				mqtt.close();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		mqtt = null;
		op = null;
		mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED;
		if (reconnect)
			connectOnMqttThread(MQTT_WAIT_BEFORE_RECONNECT_TIME); // try reconnection after 10 ms
		else
		{
			try
			{
				// if you dont want to reconnect simply remove all connection check runnables
				mqttThreadHandler.removeCallbacks(connChkRunnable);
			}
			catch (Exception e)
			{

			}
		}
	}

	/* Listeners for connection */
	private IMqttActionListener getConnectListener()
	{
		if (listernerConnect == null)
		{
			listernerConnect = new IMqttActionListener()
			{
				@Override
				public void onSuccess(IMqttToken arg0)
				{
					try
					{
						if(previousHostInfo!=null && previousHostInfo.getExceptionCount() > 6)
						{
							//-1 implies connect success
							sendHttpNetworkTestRequest(-1);
						}

						pushConnect = false;
						retryCount = 0;
						fastReconnect = 0;
						reconnectTime = 0; // resetting the reconnect timer to 0 as it would have been changed in failure
						mqttConnStatus = MQTTConnectionStatus.CONNECTED;
						Logger.d(TAG, "Client Connected ....");
						cancelNetworkErrorTimer();
						HikeMessengerApp.getPubSub().publish(HikePubSub.CONNECTED_TO_MQTT, null);
						mqttThreadHandler.postAtFrontOfQueue(new RetryFailedMessages());
						saveSuccessfullMqttConnectPrefs();
						resetConnectionVariables();
					}

					/*
					 * catch (MqttException e) { handleMqttException(e, true); }
					 */
					catch (Exception e) // although this might not happen , but still catching it
					{
						e.printStackTrace();
						// if mqtt is not connected then only schedule connection check
						if (!isConnected())
							scheduleNextConnectionCheck();
					}
					finally
					{
						releaseWakeLock();
					}
				}

				@Override
				public void onFailure(IMqttToken arg0, Throwable value)
				{
					try
					{
						if(previousHostInfo != null)
						{
						    previousHostInfo.increaseExceptionCount();
						}
						MqttException exception = (MqttException) value;
						handleMqttException(exception, true);
					}
					catch (Exception e)
					{
						Logger.e(TAG, "Exception in connect failure callback", e);
					}
					finally
					{
						releaseWakeLock();
					}
				}
			};
		}
		return listernerConnect;
	}
	
	private void saveSuccessfullMqttConnectPrefs()
	{
		if(previousHostInfo != null)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(MqttConstants.LAST_MQTT_CONNECT_PORT, previousHostInfo.getPort());
		}
	}


	/* This call back will be called when message is arrived */
	private MqttCallback getMqttCallback()
	{
		if (mqttCallBack == null)
		{
			mqttCallBack = new MqttCallback()
			{
				@Override
				public void messageArrived(String arg0, MqttMessage arg1) throws Exception
				{
					String messageBody = null;
					try
					{
						long messageProcessTime = System.currentTimeMillis();
						
						cancelNetworkErrorTimer();
						byte[] bytes = arg1.getPayload();
						bytes = Utils.uncompressByteArray(bytes);
						messageBody = new String(bytes, "UTF-8");
						Logger.i(TAG, "messageArrived called " + messageBody);
						JSONObject jsonObj = new JSONObject(messageBody);
						mqttMessageManager.saveMqttMessage(jsonObj);
						
						messageProcessTime = System.currentTimeMillis() - messageProcessTime;
						
						if(messageProcessTime > maxMessageProcessTime)
						{
							Logger.d(TAG, messageBody + " took long time to process, time : " + messageProcessTime);
							sendAnalytics(messageProcessTime, null, null);
						}
					}
					catch (JSONException e)
					{
						Logger.e(TAG, "invalid JSON message", e);
						sendAnalytics(0, messageBody, e);
					}
					catch (Throwable e)
					{
						Logger.e(TAG, "Exception when msg arrived : ", e);
						sendAnalytics(0, messageBody, e);
					}
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken arg0)
				{
					// nothing needs to be done here as success will get called eventually
				}

				@Override
				public void connectionLost(Throwable arg0)
				{
					Logger.w(TAG, "Connection Lost : " + arg0.getMessage());
					resetConnectionVariables();
					scheduleNetworkErrorTimer();
					connectOnMqttThread();
				}

				@Override
				public void fastReconnect()
				{
					// TODO Auto-generated method stub

					fastReconnect = 1;
				}
			};
		}
		return mqttCallBack;
	}
	
	private void sendAnalytics(long time, String message, Throwable throwable)
	{
		JSONObject error = new JSONObject();
		try
		{
			if(throwable != null)
			{
				error.put(HikeConstants.ERROR_MESSAGE, message);
				error.put(HikeConstants.EXCEPTION_MESSAGE, Utils.getStackTrace(throwable));
			}
			else if(time > 0)
			{
				error.put(HikeConstants.MESSAGE_PROCESS_TIME, time);
			}
			HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.ERROR_EVENT, EventPriority.HIGH, error);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	// this should always run on MQTT Thread
	private void send(HikePacket packet, int qos)
	{
		// if force disconnect is in progress dont allow mqtt operations to take place
		if (forceDisconnect)
			return;

		if (!isConnected())
		{
			connect();
			return;
		}

		Logger.d(TAG, "About to send message " + new String(packet.getMessage()));
		try
		{
			Logger.d(TAG, "Current inflight msg count : " + mqtt.getInflightMessages());
			/*
			 * while (mqtt.getInflightMessages() + 1 >= mqtt.getMaxflightMessages()) { try { Logger.w(TAG,
			 * String.format("Inflight msgs : %d , MaxInflight count : %d .... Waiting for sometime", mqtt.getInflightMessages(), mqtt.getMaxflightMessages())); Thread.sleep(30); }
			 * catch (InterruptedException e) { // TODO Auto-generated catch block e.printStackTrace(); } }
			 */
			
			// Adding Logs for Message Reliability
			MsgRelLogManager.recordPacketArrivedAtMqtt(packet);
			
			mqtt.publish(this.topic + HikeConstants.PUBLISH_TOPIC, packet.getMessage(), qos, false, packet, new IMqttActionListenerNew()
			{
				@Override
				public void onSuccess(IMqttToken arg0)
				{
					try
					{
						cancelNetworkErrorTimer();
						HikePacket packet = (HikePacket) arg0.getUserContext();
						if (packet != null)
						{
							persistence.removeMessageForPacketId(packet.getPacketId());
							if (packet.getMsgId() > 0)
							{
								Long msgId = packet.getMsgId();
								Logger.d(TAG, "Recieved S status for msg with id : " + msgId);
								
								// Adding Logs for Message Reliability
								MsgRelLogManager.recordAckMsgRelEvent(packet);

								// HikeMessengerApp.getPubSub().publish(HikePubSub.SERVER_RECEIVED_MSG, msgId);
							}
							
							if(packet.getPacketType()== HikeConstants.OFFLINE_MESSAGE_TYPE)
							{
								Logger.d(TAG, "Updating Ordinal value to Normal for mq msgs");
								HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_MESSAGE_ORIGIN_TYPE,
										new Pair<Long, Integer>(packet.getMsgId(), ConvMessage.OriginType.NORMAL.ordinal()));
							}
						}
						if (haveUnsentMessages.get())
						{
							mqttThreadHandler.postAtFrontOfQueue(new RetryFailedMessages());
						}
					}
					catch (Exception e)
					{
						Logger.e(TAG, "Exception in publish success : " + e.getMessage());
						e.printStackTrace();
					}
				}

				@Override
				public void onFailure(IMqttToken arg0, Throwable arg1)
				{
					Logger.e(TAG, "Message delivery failed for : " + arg0.getMessageId() + ", exception : " + arg1.getMessage());
					haveUnsentMessages.set(true);
					connectOnMqttThread();
				}

				@Override
				public void notifyWrittenOnSocket(IMqttToken asyncActionToken)
				{
					HikePacket packet = (HikePacket) asyncActionToken.getUserContext();
					if (packet.getMsgId() > 0)
					{
						Long msgId = packet.getMsgId();
						Logger.d(TAG, "Socket written success for msg with id : " + msgId);
						if(packet.getPacketType() == HikeConstants.MULTI_FORWARD_MESSAGE_TYPE)
						{
							try
							{
								JSONObject jsonObj = new JSONObject(new String(packet.getMessage()));

								JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
								long baseId = data.optLong(HikeConstants.MESSAGE_ID);
								JSONArray messages = data.optJSONArray(HikeConstants.MESSAGES);
								JSONArray contacts = data.optJSONArray(HikeConstants.LIST);

								int count = messages.length() * contacts.length();
								HikeMessengerApp.getPubSub().publish(HikePubSub.SERVER_RECEIVED_MULTI_MSG, new Pair<Long, Integer>(baseId, count));
							} catch (JSONException e) {
								// Do nothing
							}
						}
						else if(packet.getPacketType() == HikeConstants.BROADCAST_MESSAGE_TYPE)
						{
							try
							{
								JSONObject jsonObj = new JSONObject(new String(packet.getMessage()));

								JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
								long baseId = data.optLong(HikeConstants.MESSAGE_ID);
								JSONArray contacts = data.optJSONArray(HikeConstants.LIST);

								int count = contacts.length() + 1;
								HikeMessengerApp.getPubSub().publish(HikePubSub.SERVER_RECEIVED_MULTI_MSG, new Pair<Long, Integer>(baseId, count));
							} catch (JSONException e) {
								// Do nothing
							}
						}
						else
						{
							HikeMessengerApp.getPubSub().publish(HikePubSub.SERVER_RECEIVED_MSG, msgId);
							
							 // Adding Logs for Message Reliability.
							MsgRelLogManager.logPacketForMsgReliability(packet, MsgRelEventType.RECV_NOTIF_SOCKET_WRITING);
						}
					}
				}
			});
		}
		catch (org.eclipse.paho.client.mqttv3.MqttPersistenceException e)
		{
			e.printStackTrace();
			haveUnsentMessages.set(true);
		}
		catch (MqttException e)
		{
			haveUnsentMessages.set(true);
			handleMqttException(e, true);
		}
		catch (Exception e)
		{
			// this might happen if mqtt object becomes null while disconnect, so just ignore
		}
	}

    private void sendHttpNetworkTestRequest(int errorCode)
    {
        if(!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.HTTP_NETWORK_CHECK_CALL, false))
        {
            return;
        }

        int port = previousHostInfo != null ? previousHostInfo.getPort() : 0;
        int networkType = previousNetInfo != null ? previousNetInfo.getNetworkType() : -1;
        int exceptionCount = previousHostInfo != null ? previousHostInfo.getExceptionCount() : -1;
        RequestToken requestToken =  HttpRequests.httpNetworkTestRequest(errorCode, port, networkType, exceptionCount);
        requestToken.execute();
    }
	private void handleMqttException(MqttException e, boolean reConnect)
	{
		Logger.i(TAG, "entered handleMqttException method "+ e.getReasonCode());
		switch (e.getReasonCode())
		{
		case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
			Logger.e(TAG, "Server Unavailable, try reconnecting later");
			mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED;
			Random random = new Random();
			int reconnectIn = random.nextInt(HikeConstants.SERVER_UNAVAILABLE_MAX_CONNECT_TIME) + 1;
			scheduleNextConnectionCheck(reconnectIn * 60); // Converting minutes to seconds
            sendHttpNetworkTestRequest(e.getReasonCode());
			break;
		case MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED:
			Logger.e(TAG, "Client already disconnected.");
			mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED;
			if (reConnect)
				connectOnMqttThread();
            sendHttpNetworkTestRequest(e.getReasonCode());
			break;
		case MqttException.REASON_CODE_CLIENT_CLOSED:
			// this will happen only when you close the conn, so dont do any thing
			mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED;
			break;
		case MqttException.REASON_CODE_CLIENT_CONNECTED:
			mqttConnStatus = MQTTConnectionStatus.CONNECTED;
			break;
		case MqttException.REASON_CODE_CLIENT_DISCONNECT_PROHIBITED:
			// Thrown when an attempt to call MqttClient.disconnect() has been made from within a method on MqttCallback.
			break;
		case MqttException.REASON_CODE_CLIENT_DISCONNECTING:
			if (reConnect)
				scheduleNextConnectionCheck(1); // try reconnect after 1 sec, so that disconnect happens properly
			break;
		case MqttException.REASON_CODE_CLIENT_EXCEPTION:
			Logger.e(TAG, "Client exception : entered REASON_CODE_CLIENT_EXCEPTION");
			if (e.getCause() != null)
			{
				Logger.e(TAG, "Exception : " + e.getCause().getMessage());
				
				String analyticsDevArea = MqttConstants.EXCEPTION_DEV_AREA + "_" + e.getReasonCode();;
				if (e.getCause() instanceof UnknownHostException)
				{
					handleDNSException();
					sendAnalyticsEvent(e, analyticsDevArea + "_" + "0" );
                    sendHttpNetworkTestRequest(100);
				}
				// we are getting this exception in one phone in which message is "Host is unresolved"
				else if (e.getCause() instanceof SocketException)
				{
					if (e.getCause().getMessage() != null && e.getCause().getMessage().indexOf(UNRESOLVED_EXCEPTION) != -1)
					{
						handleDNSException();
						sendAnalyticsEvent(e, analyticsDevArea + "_" + "0" );
                        sendHttpNetworkTestRequest(100);
					}
					else
					{
						handleOtherException();
						sendAnalyticsEvent(e, analyticsDevArea + "_" + "2" );
                        sendHttpNetworkTestRequest(102);
					}
				}
				else if (e.getCause() instanceof SocketTimeoutException)
				{
					handleSocketTimeOutException();
					sendAnalyticsEvent(e, analyticsDevArea + "_" + "1" );
                    sendHttpNetworkTestRequest(101);
				}
				// added this exception for safety , we might also get this exception in some phones
				else if (e.getCause() instanceof UnresolvedAddressException)
				{
					handleDNSException();
					sendAnalyticsEvent(e, analyticsDevArea + "_" + "0" );
                    sendHttpNetworkTestRequest(100);
				}
				// Till this point disconnect has already happened due to exception (This is as per lib)
				else if (reConnect)
				{
					handleOtherException();
					connectOnMqttThread(MQTT_WAIT_BEFORE_RECONNECT_TIME);
					sendAnalyticsEvent(e, analyticsDevArea + "_" + "2" );
                    sendHttpNetworkTestRequest(102);
				}
				else
				{
					handleOtherException();
					sendAnalyticsEvent(e, analyticsDevArea + "_" + "2" );
                    sendHttpNetworkTestRequest(102);
				}
			}
			else
			{
				handleOtherException();
				scheduleNextConnectionCheck(getConnRetryTime());
                sendHttpNetworkTestRequest(e.getReasonCode());
			}
            break;
		case MqttException.REASON_CODE_CLIENT_NOT_CONNECTED:
			Logger.e(TAG, "Client not connected retry connection");
			mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED;
			if (reConnect)
				connectOnMqttThread();
			sendAnalyticsEvent(e);
            sendHttpNetworkTestRequest(e.getReasonCode());
			break;
		case MqttException.REASON_CODE_CLIENT_TIMEOUT:
			// Till this point disconnect has already happened. This could happen in PING or other TIMEOUT happen such as CONNECT, DISCONNECT
			if (reConnect)
				connectOnMqttThread();
			break;
		case MqttException.REASON_CODE_CONNECT_IN_PROGRESS:
			Logger.e(TAG, "Client already in connecting state");
			break;
		case MqttException.REASON_CODE_CONNECTION_LOST:
			Logger.e(TAG, "Client not connected retry connection");
			mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED;
			if (reConnect)
				scheduleNextConnectionCheck(getConnRetryTime());	// since we can get this exception many times due to server exception or during deployment so we dont retry frequently instead with backoff
			break;
		case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
			clearSettings();
			sendAnalyticsEvent(e);
			break;
		case MqttException.REASON_CODE_INVALID_CLIENT_ID:
			clearSettings();
			sendAnalyticsEvent(e);
			break;
		case MqttException.REASON_CODE_INVALID_MESSAGE:
			// simply ignore as message is invalid
			// Remove the corrupt packet from DB(ALL)
			break;
		case MqttException.REASON_CODE_INVALID_PROTOCOL_VERSION:
			clearSettings();
			sendAnalyticsEvent(e);
			break;
		case MqttException.REASON_CODE_MAX_INFLIGHT:
			Logger.e(TAG, "There are already to many messages in publish. Exception : " + e.getMessage());
			break;
		case MqttException.REASON_CODE_NO_MESSAGE_IDS_AVAILABLE:
			// simply ignore as message is invalid due to no msgIds
			sendAnalyticsEvent(e);
			break;
		case MqttException.REASON_CODE_NOT_AUTHORIZED:
			clearSettings();
			sendAnalyticsEvent(e);
			break;
		case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
			handleOtherException();
			scheduleNextConnectionCheck(getConnRetryTime());
			sendAnalyticsEvent(e);
			sendHttpNetworkTestRequest(e.getReasonCode());
			break;
		case MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH:
			clearSettings();
			sendAnalyticsEvent(e);
			break;
		case MqttException.REASON_CODE_SSL_CONFIG_ERROR:
			clearSettings();
			sendAnalyticsEvent(e);
			break;
		case MqttException.REASON_CODE_TOKEN_INUSE:
			clearSettings();
			sendAnalyticsEvent(e);
			break;
		case MqttException.REASON_CODE_UNEXPECTED_ERROR:
			// This could happen while reading or writing error on a socket, hence disconnection happens
			handleOtherException();
			scheduleNextConnectionCheck(getConnRetryTime());
			sendAnalyticsEvent(e);
			break;
		default:
			Logger.e(TAG, "In Default : " + e.getMessage());
			handleOtherException();
			mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED;
			connectOnMqttThread(getConnRetryTime());
			sendAnalyticsEvent(e, MqttConstants.EXCEPTION_DEFAULT);
            sendHttpNetworkTestRequest(e.getReasonCode());
			break;
		}
		e.printStackTrace();
	}
	
	private void sendAnalyticsEvent(MqttException e)
	{
		sendAnalyticsEvent(e, null);
	}
	
	private void sendAnalyticsEvent(MqttException e, String devArea)
	{
		//if server switch is off
		if(!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.CONN_PROD_AREA_LOGGING, false))
		{
			return;
		}
		
		JSONObject infoJson = new JSONObject();
		try 
		{
			infoJson.put(AnalyticsConstants.ERROR_TRACE, Utils.getStackTrace(e));
			infoJson.put(AnalyticsConstants.REASON_CODE, e.getReasonCode());
		
			//eg. dev area for REASON_CODE_CLIENT_EXCEPTION is exception_0.
			if(TextUtils.isEmpty(devArea))
			{
				devArea = MqttConstants.EXCEPTION_DEV_AREA + "_" + e.getReasonCode();
			}
			
			HAManager.getInstance().logDevEvent(MqttConstants.CONNECTION_PROD_AREA, devArea, infoJson);
		} 
		catch (JSONException jsonEx) 
		{
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "Invalid json:",jsonEx);
		}
	}

	private void handleSocketTimeOutException()
	{
		if(previousHostInfo != null)
		{
			previousHostInfo.setExceptionOnConnect(ConnectExceptions.SOCKET_TIME_OUT_EXCEPTION);
		}
		Logger.e(TAG, "Client exception : entered handleSocketTimeOutException");
		connectOnMqttThread(MQTT_WAIT_BEFORE_RECONNECT_TIME);
	}
	
	private void handleOtherException()
	{
		if(previousHostInfo != null)
		{
			Logger.e(TAG, "Client exception : entered handleOtherException");
			previousHostInfo.setExceptionOnConnect(ConnectExceptions.OTHER);
		}
	}


	/**
	 * Dns exception occured, Connect using ips
	 */
	private void handleDNSException()
	{
		Logger.e(TAG, "DNS Failure , Connect using ips");
		if(previousHostInfo != null)
		{
			previousHostInfo.setExceptionOnConnect(ConnectExceptions.DNS_EXCEPTION);
		}
		scheduleNextConnectionCheck(getConnRetryTime());
	}
	@SuppressLint("NewApi") public void destroyMqtt()
	{
		try
		{
			Logger.w(TAG, "Destroying mqtt connection.");
			context.unregisterReceiver(this);
			LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
			context.unregisterReceiver(screenOnOffReceiver);
			disconnectOnMqttThread(false);
			
			if(mMessenger != null)
			{
				mMessenger = null;
			}
			
			if (mMqttHandlerLooper != null)
			{
				if (Utils.hasKitKat())
					mMqttHandlerLooper.quitSafely();
				else
					mMqttHandlerLooper.quit();
				
				mMqttHandlerLooper = null;
				mqttThreadHandler = null;
			}
			initialised.getAndSet(false);
			mqttMessageManager.close();
			Logger.w(TAG, "Mqtt connection destroyed.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	// This will be called on for Broadcast events fired by system
	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
		{
			boolean isNetwork = Utils.isUserOnline(context);
			Logger.d(TAG, "Network change event happened. Network connected : " + isNetwork);
			if (isNetwork)
			{
				if(shouldReconnectNetworkCheck())
				{
					//Here we will disconnect and reconnect
					disconnectOnMqttThread(true);
				}
				else
				{
					connectOnMqttThread();
				}
				ChatHeadUtils.syncFromClientToServer();
				ChatHeadUtils.syncAllCallerBlockedContacts();
			}
			Utils.setupUri(); // TODO : this should be moved out from here to some other place
			HttpRequestConstants.toggleSSL();


		}
		else if (intent.getAction().equals(MQTT_CONNECTION_CHECK_ACTION))
		{
			Logger.d(TAG, "Connection check happened from GCM, client already connected ? : " + isConnected());

			// Using this to disconnect from MQ
			if (intent.hasExtra("destroy"))
			{
				destroyMqtt();
				return;
			}

			boolean reconnect = intent.getBooleanExtra("reconnect", false);
			if (reconnect)
			{
				Logger.d(TAG, "Calling explicit disconnect after server GCM push");
				pushConnect = true;
				disconnectOnMqttThread(true);
				return;
			}
			connectOnMqttThread();
		}
		else if (intent.getAction().equals(HikePubSub.SSL_PREFERENCE_CHANGED))
		{
			/*
			 * ssl settings toggled so disconnect and reconnect mqtt
			 */
			if(shouldReconnectSSLCheck())
			{
				disconnectOnMqttThread(true);
			}
		}
		else if (intent.getAction().equals(HikePubSub.IPS_CHANGED))
		{
			String ipString = intent.getStringExtra("ips");
			saveAndSet(ipString);
		}
		else if (intent.getAction().equals(HikePubSub.PORTS_CHANGED))
		{
			String portsArrayString = intent.getStringExtra(MqttConstants.MQTT_PORTS);
			saveAndSetPorts(portsArrayString);
		}
	}

	/**
	 * Return <b>True</b> if we should disconnect and reconnect in following scenarios
	 * <li>1. if we are not connected and current network is different from previous network</li>
	 * <p></p>
	 * <b>False</b> otherwise
	 * @return
	 */
	private boolean shouldReconnectNetworkCheck()
	{
			/**
			 * In else we should handling only cases where we are in connecting state. So if we are already connected we should not disconnect.
			 */
			if(isConnected())
			{
				return false;
			}
			
			NetInfo currentNetInfo = NetInfo.getNetInfo(Utils.getActiveNetInfo());
			if(previousNetInfo != null && currentNetInfo != null)
			{
				Logger.d(TAG, "previous info : " + previousNetInfo.toString());
				Logger.d(TAG, "current info : " + currentNetInfo.toString());

				if(!previousNetInfo.equals(currentNetInfo)) // previous network info is not equal to current network info , disconnect and re-connect in this case
				{
					return true;
				}
			}
			return false;
	}
	
	/**
	 * Return <b>True</b> if we should disconnect and reconnect in following scenarios
	 * <li>1. If we should connect using ssl and currently we are not connected to ssl</li>
	 * <li>2. if we should not connect using ssl and currently we are connected using ssl</li>
	 * <p></p>
	 * <b>False</b> otherwise
	 * @return
	 */

	private boolean shouldReconnectSSLCheck()
	{
		boolean shouldConnectUsingSSL = Utils.switchSSLOn(context);
		boolean isSSLConnected = isSSL();
		Logger.d(TAG, "SSL Preference has changed. OnSSL : " + shouldConnectUsingSSL + " ,isSSLAlreadyOn : " + isSSLConnected);
		// reconnect using SSL if currently not connected using SSL OR vice versa
		return shouldConnectUsingSSL ^ isSSLConnected;
	}
	
	
	private boolean isSSL()
	{
		if (mqtt != null)
		{
			String uri = mqtt.getServerURI();
			if (uri != null && uri.startsWith("ssl"))
				return true;
		}
		return false;
	}

	private void saveAndSet(String ipString)
	{
		Editor editor = settings.edit();
		editor.putString(HikeMessengerApp.MQTT_IPS, ipString);
		editor.commit();
		setServerUris();
	}
	
	private void saveAndSetPorts(String portsArrayString)
	{
		HikeSharedPreferenceUtil.getInstance().saveData(MqttConstants.MQTT_PORTS, portsArrayString);
		setServerPorts();
	}

	// This class is just for testing .....
	private class TestOutmsgs implements Runnable
	{
		@Override
		public void run()
		{
			Logger.w(TAG, "Starting testing thread .....");
			Thread t = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					testUj();
				}
			});
			t.setName("Test Thread");
			t.start();
		}

		private void testUj()
		{
			int count = 0;
			String myMsisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
			String msisdn = "+919999238132";
			String msisdn1 = "+919868185209";
			if (myMsisdn != null)
			{
				if (msisdn.equalsIgnoreCase(myMsisdn))
				{
					return;
				}
			}
			Random rand = new Random();

			JSONObject bulkPacket = new JSONObject();
			JSONObject data = new JSONObject();
			JSONObject msgs = new JSONObject();
			JSONArray bulkMsgArray = new JSONArray();
			for (int i = 0; i < 25; i++)
			{
				String ujString = String
						.format("{\"t\": \"uj\",\"d\":{\"msisdn\":\"%s\"},\"ts\":%d,\"st\":\"ru\"}", msisdn, Math.abs(System.currentTimeMillis() + rand.nextLong()));
				String ujString1 = String.format("{\"t\": \"uj\",\"d\":{\"msisdn\":\"%s\"},\"ts\":%d,\"st\":\"ru\"}", msisdn1,
						Math.abs(System.currentTimeMillis() + rand.nextLong()));
				try
				{

					JSONObject o = new JSONObject(ujString);
					JSONObject o1 = new JSONObject(ujString1);
					bulkMsgArray.put(o);
					bulkMsgArray.put(o1);
				}
				catch (JSONException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			try
			{
				bulkPacket.put(HikeConstants.TYPE, "bm");
				data.put("msgs", bulkMsgArray);
				bulkPacket.put(HikeConstants.DATA, data);
				bulkPacket.put(HikeConstants.TIMESTAMP, System.currentTimeMillis());
				mqttMessageManager.saveMqttMessage(bulkPacket);

			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		private void testMsg()
		{
			int count = 0;
			String myMsisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
			String msisdn = "+919582974797";

			if (myMsisdn != null)
			{
				if (msisdn.equalsIgnoreCase(myMsisdn))
				{
					return;
				}
			}
			for (int i = 0; i < 50; i++)
			{
				count++;
				Random rand = new Random();
				String data = String.format("{\"t\": \"m\",\"to\": \"" + msisdn + "\",\"d\":{\"hm\":\"%d\",\"i\":%d, \"ts\":%d}}", rand.nextLong(), rand.nextLong(),
						System.currentTimeMillis());

				Logger.d(TAG, "Sending msg : " + data);
				Message msg = Message.obtain();
				msg.what = 12341;
				Bundle bundle = new Bundle();
				bundle.putString(HikeConstants.MESSAGE, data);
				msg.setData(bundle);
				msg.replyTo = mMessenger;
				try
				{
					mMessenger.send(msg);
					Thread.sleep(20);
				}
				catch (RemoteException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (Exception e)
				{

				}
			}

		}
	}

	/*
	 * Call this method to send a message.
	 * On receiving a message, it sends the message to the {@link IncomingHandler} which in turn sends it via mqtt. 
	 *
	 * @param object - Message
	 * @param qos level (MQTT_PUBLISH or MQTT_PUBLISH_LOW)
	 */
	public void sendMessage(JSONObject jsonObj, int qos)
	{
		// added check
		if(jsonObj == null)
		{
			return ;
		}
		
		if (!initialised.get())
		{
			Logger.d(TAG, "Not initialised, initializing...");
			if(!init())
			{
				// not successfully initialized return;
				return;
			}
			
			// successfully initialized
		}
		
		JSONObject o = Utils.cloneJsonObject(jsonObj);
		
		if(o == null)
		{
			return ;
		}
		
		/*
		 * Now on, we would be putting Client sending time ("c") in all qos1 packets,
		 * For all type m packets it should be set from db insert, for all other packets
		 * we would be putting this from here.
		 */
		long currentTime = System.currentTimeMillis();
		if (qos == MQTT_QOS_ONE)
		{
			if(!o.has(HikeConstants.SEND_TIMESTAMP))
			{
				try
				{
					o.put(HikeConstants.SEND_TIMESTAMP, currentTime);
				}
				catch (JSONException e)
				{
					Logger.e(TAG, "Error while trying to put SEND_TIMESTAMP", e);
				}
			}
		}

		
		String data = o.toString();

		long msgId = -1;
		String trackID = null;

		/*
		 * if this is a message, then grab the messageId out of the json object so we can get confirmation of success/failure
		 */
		// get Values of (uid, msgId, type, track) from metadata of convMsg
		if (HikeConstants.MqttMessageTypes.MESSAGE.equals(o.optString(HikeConstants.TYPE)) || HikeConstants.MqttMessageTypes.GENERAL_EVENT_QOS_ONE.equals(o.optString(HikeConstants.TYPE))
				|| (HikeConstants.MqttMessageTypes.INVITE.equals(o.optString(HikeConstants.TYPE))))
		{
			JSONObject json = o.optJSONObject(HikeConstants.DATA);
			msgId = Long.parseLong(json.optString(HikeConstants.MESSAGE_ID, "-1"));
		}
		else if((HikeConstants.MqttMessageTypes.NEW_MESSAGE_READ.equals(o.optString(HikeConstants.TYPE))))
		{
			JSONObject json = o.optJSONObject(HikeConstants.DATA);
			Logger.d(AnalyticsConstants.MSG_REL_TAG, "Sending fetching msgId from DATA:- "+ json);
			Iterator<String> json_keys = json.keys();

		    while(json_keys.hasNext())
		    {
		        msgId = Long.parseLong((String) json_keys.next());
				Logger.d(AnalyticsConstants.MSG_REL_TAG, "Sending NMR to Sender back with msgId:- " + msgId);
		    }
		}
		
		int type;
		if (HikeConstants.MqttMessageTypes.MULTIPLE_FORWARD.equals(o.optString(HikeConstants.SUB_TYPE)))
		{
			type = HikeConstants.MULTI_FORWARD_MESSAGE_TYPE;
		}
		else if(OneToNConversationUtils.isBroadcastConversation((o.optString(HikeConstants.TO))))
		{
			type = HikeConstants.BROADCAST_MESSAGE_TYPE;
		}
		else
		{
			type = HikeConstants.NORMAL_MESSAGE_TYPE;
		}

		HikePacket packet = new HikePacket(data.getBytes(), msgId, currentTime, type);
		setTrackIDInPacketForMsgRel(o, packet);
		addToPersistence(packet, qos);

		Message msg = Message.obtain();
		msg.what = HikeService.MSG_APP_PUBLISH;
		msg.arg1 = qos;

		Bundle bundle = new Bundle();
		bundle.putParcelable(HikeConstants.MESSAGE, packet);

		msg.setData(bundle);
		msg.replyTo = this.mMessenger;

		try
		{
			mMessenger.send(msg);
		}
		catch (RemoteException e)
		{
			/* Service is dead. What to do? */
			Logger.e("HikeServiceConnection", "Remote Service dead", e);
		}
	}

	/*
	 * Adds the created hike packet to mqtt persistence if qos > 0.
	 *
	 * @param packet - HikePacket
	 * @param qos level (MQTT_PUBLISH or MQTT_PUBLISH_LOW)
	 */
	private void addToPersistence(HikePacket packet, int qos)
	{
		/* only care about failures for messages we care about. */
		if (qos > 0 && packet.getPacketId() == -1)
		{
			try
			{
				persistence.addSentMessage(packet);
			}
			catch (MqttPersistenceException e)
			{
				Logger.e(TAG, "Unable to persist message", e);
			}
			catch (Exception e)
			{
				Logger.e(TAG, "Unable to persist message", e);
			}
		}
	}
	
	private void resetConnectionVariables()
	{
		forceDisconnect = false;
		previousHostInfo = null;
	}
	
	/**
	 * Checks for "pd" inside JSONObject... (pd present means message should be tracked) if present, then inserts into packet 2 things
	 * 
	 * A)track_id -> get from pd
	 * 
	 * B)msg type -> type = 'm' OR type = 'nmr'
	 * 
	 * NOTE: (B is used only for Logging.This is not sent to server)
	 * 
	 * @param o
	 * @param packet
	 */
	private void setTrackIDInPacketForMsgRel(JSONObject o, HikePacket packet)
	{
		if (o.has(HikeConstants.PRIVATE_DATA))
		{
			String trackID = "";
			JSONObject pd = o.optJSONObject(HikeConstants.PRIVATE_DATA);
			trackID = pd.optString(HikeConstants.MSG_REL_UID);
			if (trackID != null)
			{
				packet.setTrackId(trackID);
				packet.setMsgType(o.optString(HikeConstants.TYPE));
			}
		}
		else if (HikeConstants.MqttMessageTypes.NEW_MESSAGE_READ.equals(o.optString(HikeConstants.TYPE)))
		{
			try
			{
				String trackID = "";
				JSONObject msgMetadata = o.optJSONObject(HikeConstants.DATA);
				if (msgMetadata != null)
				{
					Iterator<?> keys = msgMetadata.keys();
					while (keys.hasNext())
					{
						String key = (String) keys.next();
						JSONObject pdObject = msgMetadata.getJSONObject(key);
						trackID = pdObject.optString(HikeConstants.MSG_REL_UID);
						if (trackID != null)
						{
							packet.setTrackId(trackID);
							packet.setMsgType(HikeConstants.MqttMessageTypes.NEW_MESSAGE_READ);
							break;
						}
					}
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
	}
}
