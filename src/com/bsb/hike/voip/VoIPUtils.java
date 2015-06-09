package com.bsb.hike.voip;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Enumeration;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.view.VoIPActivity;

public class VoIPUtils {

	public static String ndkLibPath = "lib/armeabi/";

	public static enum ConnectionClass {
		TwoG,
		ThreeG,
		FourG,
		WiFi,
		Unknown
	}

	public static enum CallSource
	{
		CHAT_THREAD, PROFILE_ACTIVITY, MISSED_CALL_NOTIF, CALL_FAILED_FRAG
	}
	
    public static boolean isWifiConnected(Context context) {
    	ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

    	if (mWifi.isConnected()) {
    		return true;
    	}    	
    	else
    		return false;
    }	
	
    public static String getLocalIpAddress(Context c) {
    	
    	if (isWifiConnected(c)) {
    		WifiManager wifiMgr = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
    		WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
    		int ipAddress = wifiInfo.getIpAddress();
    		
    	    // Convert little-endian to big-endianif needed
    	    if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
    	        ipAddress = Integer.reverseBytes(ipAddress);
    	    }

    	    byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

    	    String ipAddressString;
    	    try {
    	        ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
    	    } catch (UnknownHostException ex) {
    	        Logger.e(VoIPConstants.TAG, "Unable to get host address.");
    	        ipAddressString = null;
    	    }

    	    return ipAddressString;    		
    	} else {
	        try {
	            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	                NetworkInterface intf = en.nextElement();
	                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                    InetAddress inetAddress = enumIpAddr.nextElement();
	                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
	                        return inetAddress.getHostAddress();
	                    }
	                }
	            }
	        } catch (NullPointerException ex) {
	            ex.printStackTrace();
	        } catch (SocketException ex) {
	            ex.printStackTrace();
	        }
        return null;
    	}
    }	

    /**
     * Add a VoIP related message to the chat thread.
     * @param context
     * @param clientPartner
     * @param messageType
     * @param duration
     */
    public static void addMessageToChatThread(Context context, VoIPClient clientPartner, String messageType, int duration, long timeStamp, boolean shouldShowPush) 
    {

    	if (TextUtils.isEmpty(clientPartner.getPhoneNumber())) {
    		Logger.w(VoIPConstants.TAG, "Null phone number while adding message to chat thread. Message: " + messageType + ", Duration: " + duration + ", Phone: " + clientPartner.getPhoneNumber());
    		return;
    	}
    		
    	Logger.d(VoIPConstants.TAG, "Adding message to chat thread. Message: " + messageType + ", Duration: " + duration + ", Phone: " + clientPartner.getPhoneNumber());
    	HikeConversationsDatabase mConversationDb = HikeConversationsDatabase.getInstance();
    	Conversation mConversation = mConversationDb.getConversation(clientPartner.getPhoneNumber(), HikeConstants.MAX_MESSAGES_TO_LOAD_INITIALLY, false);
    	long timestamp = System.currentTimeMillis() / 1000;
    	if (timeStamp > 0)
    	{
    		timestamp = timeStamp;
    	}
    	
		JSONObject jsonObject = new JSONObject();
		JSONObject data = new JSONObject();
		
		if (duration == 0 && messageType == HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_CALL_SUMMARY) {
			if (clientPartner.isInitiator())
				messageType = HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING;
			else
				messageType = HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_OUTGOING;
		}

		boolean selfGenerated = true;
		if(messageType.equals(HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING))
		{
			selfGenerated = false;
		}

		try
		{
			Logger.d(VoIPConstants.TAG, "Adding message of type: " + messageType + " to chat thread.");
			data.put(HikeConstants.MESSAGE_ID, Long.toString(timestamp));
			data.put(HikeConstants.VOIP_CALL_DURATION, duration);
			data.put(HikeConstants.VOIP_CALL_INITIATOR, !clientPartner.isInitiator());
			data.put(HikeConstants.TIMESTAMP, timestamp);

			jsonObject.put(HikeConstants.DATA, data);
			jsonObject.put(HikeConstants.TYPE, messageType);
			jsonObject.put(HikeConstants.TO, clientPartner.getPhoneNumber());
			
			ConvMessage convMessage = new ConvMessage(jsonObject, mConversation, context, selfGenerated);
			convMessage.setShouldShowPush(shouldShowPush);
			mConversationDb.addConversationMessages(convMessage,true);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
		}
		catch (JSONException e)
		{
			Logger.w(VoIPConstants.TAG, "addMessageToChatThread() JSONException: " + e.toString());
		}    	
    }

	public static void sendMissedCallNotificationToPartner(VoIPClient clientPartner) {

		try {
			JSONObject socketData = new JSONObject();
			socketData.put("time", System.currentTimeMillis());
			
			JSONObject data = new JSONObject();
			data.put(HikeConstants.MESSAGE_ID, new Random().nextInt(10000));
			data.put(HikeConstants.TIMESTAMP, System.currentTimeMillis() / 1000); 
			data.put(HikeConstants.METADATA, socketData);

			JSONObject message = new JSONObject();
			message.put(HikeConstants.TO, clientPartner.getPhoneNumber());
			message.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE_VOIP_1);
			message.put(HikeConstants.SUB_TYPE, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING);
			message.put(HikeConstants.DATA, data);
			
			HikeMqttManagerNew.getInstance().sendMessage(message, MqttConstants.MQTT_QOS_ONE);
			Logger.d(VoIPConstants.TAG, "Sent missed call notifier to partner.");
			
		} catch (JSONException e) {
			e.printStackTrace();
		} 
		
	}

	/**
	 * Tells you how you are connected to the Internet. 
	 * 2G / 3G / WiFi etc.
	 * @param context
	 * @return ConnectionClass 2G / 3G / 4G / WiFi
	 */
	public static ConnectionClass getConnectionClass(Context context) {
		ConnectionClass connection = null;
		
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (mWifi != null && mWifi.isConnected()) {
		    connection = ConnectionClass.WiFi;
		} else {
		    TelephonyManager mTelephonyManager = (TelephonyManager)
		            context.getSystemService(Context.TELEPHONY_SERVICE);
		    int networkType = mTelephonyManager.getNetworkType();
		    switch (networkType) {
		        case TelephonyManager.NETWORK_TYPE_GPRS:
		        case TelephonyManager.NETWORK_TYPE_EDGE:
		        case TelephonyManager.NETWORK_TYPE_CDMA:
		        case TelephonyManager.NETWORK_TYPE_1xRTT:
		        case TelephonyManager.NETWORK_TYPE_IDEN:
		            connection = ConnectionClass.TwoG;
		            break;
		        case TelephonyManager.NETWORK_TYPE_UMTS:
		        case TelephonyManager.NETWORK_TYPE_EVDO_0:
		        case TelephonyManager.NETWORK_TYPE_EVDO_A:
		        case TelephonyManager.NETWORK_TYPE_HSDPA:
		        case TelephonyManager.NETWORK_TYPE_HSUPA:
		        case TelephonyManager.NETWORK_TYPE_HSPA:
		        case TelephonyManager.NETWORK_TYPE_EVDO_B:
		        case TelephonyManager.NETWORK_TYPE_EHRPD:
		        case TelephonyManager.NETWORK_TYPE_HSPAP:
		            connection = ConnectionClass.ThreeG;
		            break;
		        case TelephonyManager.NETWORK_TYPE_LTE:
		            connection = ConnectionClass.FourG;
		            break;
		        default:
		            connection = ConnectionClass.Unknown;
		            break;
		    }
		}
//		Logger.w(VoIPConstants.TAG, "Our connection class: " + connection.name());
		return connection;
	}
	
	/**
	 * Is the user currently in a call?
	 * @param context
	 * @return
	 */
	public static boolean isUserInCall(Context context) {
		boolean callActive = false;
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		if (telephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE)
			callActive = true;
		
		return callActive;
	}
	
	public static int getAudioSource() {
		int source = MediaRecorder.AudioSource.MIC;
		String model = android.os.Build.MODEL;
		
		if (android.os.Build.VERSION.SDK_INT >= 11)
			source = MediaRecorder.AudioSource.MIC;
		
		Logger.d(VoIPConstants.TAG, "Phone model: " + model);
		
//		if (model.contains("Nexus 5") || 
//				model.contains("Nexus 4"))
//			source = MediaRecorder.AudioSource.VOICE_RECOGNITION;
		
		return source;
	}
	
	public static boolean shouldShowCallRatePopupNow()
	{
		return HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOW_VOIP_CALL_RATE_POPUP, false);
	}
	
	public static void setupCallRatePopupNextTime()
	{
		HikeSharedPreferenceUtil sharedPref = HikeSharedPreferenceUtil.getInstance();
		int callsCount = sharedPref.getData(HikeMessengerApp.VOIP_ACTIVE_CALLS_COUNT, 0);
		sharedPref.saveData(HikeMessengerApp.VOIP_ACTIVE_CALLS_COUNT, ++callsCount);

		int frequency = sharedPref.getData(HikeMessengerApp.VOIP_CALL_RATE_POPUP_FREQUENCY, -1);
		boolean shownAlready = sharedPref.getData(HikeMessengerApp.SHOW_VOIP_CALL_RATE_POPUP, false);

		if(callsCount == frequency)
		{
			// Show popup next time
			sharedPref.saveData(HikeMessengerApp.SHOW_VOIP_CALL_RATE_POPUP, true);
			sharedPref.saveData(HikeMessengerApp.VOIP_ACTIVE_CALLS_COUNT, 0);
		}
		else if(shownAlready)
		{
			// Shown for the first time, dont show later
			sharedPref.saveData(HikeMessengerApp.SHOW_VOIP_CALL_RATE_POPUP, false);
		}
	}
	
	/**
	 * Returns the relay port that should be used. 
	 * This can be set by the server, and otherwise defaults to VoIPConstants.ICEServerPort
	 * @return
	 */
	public static int getRelayPort(Context context) {
		
		SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		int port = prefs.getInt(HikeConstants.VOIP_RELAY_SERVER_PORT, VoIPConstants.ICEServerPort);
		return port;
	}

	public static void cancelMissedCallNotification(Context context)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.CANCEL_ALL_NOTIFICATIONS, null);
		Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		context.sendBroadcast(it);
	}

	public static NotificationCompat.Action[] getMissedCallNotifActions(Context context, String msisdn)
	{
		Intent callIntent = IntentFactory.getVoipCallIntent(context, msisdn, CallSource.MISSED_CALL_NOTIF);
		PendingIntent callPendingIntent = PendingIntent.getService(context, 0, callIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Intent messageIntent = IntentFactory.createChatThreadIntentFromMsisdn(context, msisdn, true);
		PendingIntent messagePendingIntent = PendingIntent.getActivity(context, 0, messageIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Action actions[] = new NotificationCompat.Action[2];
		actions[0] = new NotificationCompat.Action(R.drawable.ic_action_call, context.getString(R.string.voip_missed_call_action), callPendingIntent);
		actions[1] = new NotificationCompat.Action(R.drawable.ic_action_message, context.getString(R.string.voip_missed_call_message), messagePendingIntent);

		return actions;
	}
	
	public static int getQualityTestAcceptablePacketLoss(Context context) {
		
		SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		int apl = prefs.getInt(HikeConstants.VOIP_QUALITY_TEST_ACCEPTABLE_PACKET_LOSS, 20);
		return apl;
	}
	
	public static int getQualityTestSimulatedCallDuration(Context context) {
		
		SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		int scd = prefs.getInt(HikeConstants.VOIP_QUALITY_TEST_SIMULATED_CALL_DURATION, 2);
		return scd;
	}
	
	public static boolean useAEC(Context context) 
	{
		boolean useAec = false;
		// Disable AEC on <= 2.3 devices
		if (Utils.isHoneycombOrHigher())
		{
			useAec = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.VOIP_AEC_ENABLED, true);
		}
		return useAec;
	}
	
	public static boolean isConferencingEnabled(Context context) 
	{
		boolean conferenceEnabled = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.VOIP_CONFERENCING_ENABLED, true);
		return conferenceEnabled;
	}
	
	public static boolean isBluetoothEnabled(Context context) 
	{
		boolean bluetoothEnabled = false;
		
		// Below KitKat startBluetoothSco() requires BROADCAST_STICKY permission
		// http://stackoverflow.com/questions/8678642/startbluetoothsco-throws-security-exception-broadcast-sticky-on-ics
		// https://code.google.com/p/android/issues/detail?id=25136
		if (Utils.isKitkatOrHigher())
			bluetoothEnabled = true;
		else
			Logger.w(VoIPConstants.TAG, "Bluetooth disabled since phone does not support Kitkat.");
		
		return bluetoothEnabled;
	}
	
	/**
	 * Used to communicate between two clients using the server
	 * @param recipient		Recipient's MSISDN
	 * @param callMessage	One of the MQTT Message types ({@linkplain com.bsb.hike.HikeConstants.MqttMessageTypes})
	 * @param callId		If there is an associated call ID, put it here
	 * @param callInitiator Optional parameter.
	 */
	public static void sendVoIPMessageUsingHike(String recipient, String callMessage, int callId, boolean callInitiator) {
		try {
			JSONObject socketData = new JSONObject();
			socketData.put("callId", callId);
			socketData.put("initiator", callInitiator);
			socketData.put("reconnecting", false);
			
			JSONObject data = new JSONObject();
			data.put(HikeConstants.MESSAGE_ID, new Random().nextInt(10000));
			data.put(HikeConstants.TIMESTAMP, System.currentTimeMillis() / 1000); 
			data.put(HikeConstants.METADATA, socketData);

			JSONObject message = new JSONObject();
			message.put(HikeConstants.TO, recipient);
			message.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE_VOIP_0);
			message.put(HikeConstants.SUB_TYPE, callMessage);
			message.put(HikeConstants.DATA, data);
			
			HikeMqttManagerNew.getInstance().sendMessage(message, MqttConstants.MQTT_QOS_ONE);
			Logger.d(VoIPConstants.TAG, "Sent call request message of type: " + callMessage + " to: " + recipient);

		} catch (JSONException e) {
			e.printStackTrace();
			Logger.w(VoIPConstants.TAG, "sendSocketInfoToPartner JSON error: " + e.toString());
		} 
	}

	public static void handleVOIPPacket(Context context, JSONObject jsonObj) throws JSONException
	{
		// VoIP checks
		if (jsonObj.has(HikeConstants.SUB_TYPE)) 
		{	
			String subType = jsonObj.getString(HikeConstants.SUB_TYPE);
			Logger.d(VoIPConstants.TAG, "Message subtype: " + subType);

			if (subType.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_CANCELLED)) {
				// Check for call cancelled message
				JSONObject metadataJSON = jsonObj.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);

				if (metadataJSON.getInt(VoIPConstants.Extras.CALL_ID) != VoIPService.getCallId()) {
					Logger.d(VoIPConstants.TAG, "Ignoring call cancelled message. local: " + VoIPService.getCallId() +
							", remote: " + metadataJSON.getInt(VoIPConstants.Extras.CALL_ID));
					return;
				}
				
				Intent i = new Intent(context.getApplicationContext(), VoIPService.class);
				i.putExtra(VoIPConstants.Extras.ACTION, subType);
				i.putExtra(VoIPConstants.Extras.CALL_ID, metadataJSON.getInt(VoIPConstants.Extras.CALL_ID));
				context.startService(i);
				return;
			}
			
			if (subType.equals(HikeConstants.MqttMessageTypes.VOIP_SOCKET_INFO) ||
					subType.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_REQUEST) ||
					subType.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_REQUEST_RESPONSE) ||
					subType.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_RESPONSE_RESPONSE)) {
				
				JSONObject metadataJSON = jsonObj.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);
				
				// Check if the initiator (us) has already hung up
				if (metadataJSON.getBoolean(VoIPConstants.Extras.INITIATOR) == false &&
						metadataJSON.getInt(VoIPConstants.Extras.CALL_ID) != VoIPService.getCallId()) {
					Logger.w(VoIPConstants.TAG, "Receiving a reply for a terminated call. local: " + VoIPService.getCallId() +
							", remote: " + metadataJSON.getInt(VoIPConstants.Extras.CALL_ID));
					return;		
				}

				/*
				 * Call Initiation Messages
				 * Added: 24 Mar, 2015 (AJ)
				 * Prior to this addition, socket information messages served as call
				 * initiation messages as well. We are now introducing a separate class
				 * of messages for call initiation to speed up the process. 
				 */
				if (subType.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_REQUEST) ||
						subType.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_REQUEST_RESPONSE) ||
						subType.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_RESPONSE_RESPONSE)) {

//					Logger.w(VoIPConstants.TAG, "Received: " + subType);

					Intent i = new Intent(context.getApplicationContext(), VoIPService.class);
					i.putExtra(VoIPConstants.Extras.ACTION, subType);
					i.putExtra(VoIPConstants.Extras.MSISDN, jsonObj.getString(HikeConstants.FROM));
					i.putExtra(VoIPConstants.Extras.INITIATOR, metadataJSON.getBoolean(VoIPConstants.Extras.INITIATOR));
					i.putExtra(VoIPConstants.Extras.CALL_ID, metadataJSON.getInt(VoIPConstants.Extras.CALL_ID));
					context.startService(i);
					return;
				}

				// Socket info
				if (subType.equals(HikeConstants.MqttMessageTypes.VOIP_SOCKET_INFO)) 
				{
					// Check for currently active call
					if ((metadataJSON.getInt(VoIPConstants.Extras.CALL_ID) != VoIPService.getCallId() && VoIPService.getCallId() > 0) ||
							VoIPUtils.isUserInCall(context)) {
						Logger.w(VoIPConstants.TAG, "We are already in a call. local: " + VoIPService.getCallId() +
								", remote: " + metadataJSON.getInt(VoIPConstants.Extras.CALL_ID));

						VoIPUtils.sendVoIPMessageUsingHike(jsonObj.getString(HikeConstants.FROM), 
								HikeConstants.MqttMessageTypes.VOIP_ERROR_ALREADY_IN_CALL, 
								metadataJSON.getInt(VoIPConstants.Extras.CALL_ID), 
								false);
						return;
					}
						
					/*
					 * Socket information is the same as a request for call initiation. 
					 * The calling party sends its socket information to the callee, and
					 * the callee at that point should start its voip service (and not the 
					 * activity) so it can reply with its own socket information. 
					 */
					Logger.d(VoIPConstants.TAG, "Receiving socket info..");
					
					Intent i = new Intent(context.getApplicationContext(), VoIPService.class);
					i.putExtra(VoIPConstants.Extras.ACTION, VoIPConstants.Extras.SET_PARTNER_INFO);
					i.putExtra(VoIPConstants.Extras.MSISDN, jsonObj.getString(HikeConstants.FROM));
					i.putExtra(VoIPConstants.Extras.INTERNAL_IP, metadataJSON.getString(VoIPConstants.Extras.INTERNAL_IP));
					i.putExtra(VoIPConstants.Extras.INTERNAL_PORT, metadataJSON.getInt(VoIPConstants.Extras.INTERNAL_PORT));
					i.putExtra(VoIPConstants.Extras.EXTERNAL_IP, metadataJSON.getString(VoIPConstants.Extras.EXTERNAL_IP));
					i.putExtra(VoIPConstants.Extras.EXTERNAL_PORT, metadataJSON.getInt(VoIPConstants.Extras.EXTERNAL_PORT));
					i.putExtra(VoIPConstants.Extras.RELAY, metadataJSON.getString(VoIPConstants.Extras.RELAY));
					i.putExtra(VoIPConstants.Extras.RELAY_PORT, metadataJSON.getInt(VoIPConstants.Extras.RELAY_PORT));
					i.putExtra(VoIPConstants.Extras.RECONNECTING, metadataJSON.getBoolean(VoIPConstants.Extras.RECONNECTING));
					i.putExtra(VoIPConstants.Extras.INITIATOR, metadataJSON.getBoolean(VoIPConstants.Extras.INITIATOR));
					i.putExtra(VoIPConstants.Extras.CALL_ID, metadataJSON.getInt(VoIPConstants.Extras.CALL_ID));
					context.startService(i);
					return;
				}
			}
			
			if (subType.equals(HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING)) 
			{
				Logger.d(VoIPConstants.TAG, "Adding a missed call to our chat history.");
				VoIPClient clientPartner = new VoIPClient(context, null);
				clientPartner.setPhoneNumber(jsonObj.getString(HikeConstants.FROM));
				clientPartner.setInitiator(true);
				VoIPUtils.addMessageToChatThread(context, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING, 0, jsonObj.getJSONObject(HikeConstants.DATA).getLong(HikeConstants.TIMESTAMP), true);
			}
			
			if (subType.equals(HikeConstants.MqttMessageTypes.VOIP_ERROR_CALLEE_INCOMPATIBLE_UPGRADABLE)) 
			{
				String message = jsonObj.getJSONObject(HikeConstants.DATA).getString(HikeConstants.HIKE_MESSAGE);
				Intent i = new Intent(context, VoIPActivity.class);
				i.putExtra(VoIPConstants.Extras.ACTION, VoIPConstants.PARTNER_REQUIRES_UPGRADE);
				i.putExtra(VoIPConstants.Extras.MSISDN, jsonObj.getString(HikeConstants.FROM));
				i.putExtra(VoIPConstants.Extras.MESSAGE, message);
				i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(i);
			}
			
			if (subType.equals(HikeConstants.MqttMessageTypes.VOIP_ERROR_CALLEE_INCOMPATIBLE_NOT_UPGRADABLE)) 
			{
				String message = jsonObj.getJSONObject(HikeConstants.DATA).getString(HikeConstants.HIKE_MESSAGE);
				Intent i = new Intent(context, VoIPActivity.class);
				i.putExtra(VoIPConstants.Extras.ACTION, VoIPConstants.PARTNER_INCOMPATIBLE);
				i.putExtra(VoIPConstants.Extras.MSISDN, jsonObj.getString(HikeConstants.FROM));
				i.putExtra(VoIPConstants.Extras.MESSAGE, message);
				i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(i);
			}
			
			if (subType.equals(HikeConstants.MqttMessageTypes.VOIP_ERROR_CALLEE_HAS_BLOCKED_YOU)) 
			{
				String message = jsonObj.getJSONObject(HikeConstants.DATA).getString(HikeConstants.HIKE_MESSAGE);
				Intent i = new Intent(context, VoIPActivity.class);
				i.putExtra(VoIPConstants.Extras.ACTION, VoIPConstants.PARTNER_HAS_BLOCKED_YOU);
				i.putExtra(VoIPConstants.Extras.MSISDN, jsonObj.getString(HikeConstants.FROM));
				i.putExtra(VoIPConstants.Extras.MESSAGE, message);
				i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(i);
			}
			
			if (subType.equals(HikeConstants.MqttMessageTypes.VOIP_ERROR_ALREADY_IN_CALL)) 
			{
				Intent i = new Intent(context, VoIPActivity.class);
				i.putExtra(VoIPConstants.Extras.ACTION, VoIPConstants.PARTNER_IN_CALL);
				i.putExtra(VoIPConstants.Extras.MSISDN, jsonObj.getString(HikeConstants.FROM));
				i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(i);
			}
			
		}
	
	}
	
	public static byte[] addPCMSamples(byte[] original, byte[] toadd) {
		
		if (original.length != toadd.length) {
			Logger.w(VoIPConstants.TAG, "PCM samples length does not match (A). " +
					original.length + " vs " + toadd.length);
			return original;
		}
		
		// Get original sample as short
		ShortBuffer shortBuffer = ByteBuffer.wrap(original).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
		short[] originalShorts = new short[shortBuffer.capacity()];
		shortBuffer.get(originalShorts);
		
		// Get second sample as short
		ShortBuffer shortBuffer2 = ByteBuffer.wrap(toadd).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
		short[] toAddShorts = new short[shortBuffer2.capacity()];
		shortBuffer2.get(toAddShorts);
		
		// Add them together in a short array
		short[] finalShorts = new short[shortBuffer2.capacity()];
		for (int i = 0; i < finalShorts.length; i++) {
			int sum = (int) ((originalShorts[i] + toAddShorts[i]));
			if (sum > Short.MAX_VALUE) {
				finalShorts[i] = Short.MAX_VALUE;
			} else if (sum < Short.MIN_VALUE) {
				finalShorts[i] = Short.MIN_VALUE;
			} else
				finalShorts[i] = (short) (sum);
		}
		
		// Convert short array to byte array
		ByteBuffer buffer = ByteBuffer.allocate(finalShorts.length * 2);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.asShortBuffer().put(finalShorts);
		return buffer.array();
	}
	
	public static byte[] subtractPCMSamples(byte[] from, byte[] tosubtract) {
		
		if (from.length != tosubtract.length) {
			Logger.w(VoIPConstants.TAG, "PCM samples length does not match (S). " +
					from.length + " vs " + tosubtract.length);
			return from;
		}

		// Get original sample as short
		ShortBuffer shortBuffer = ByteBuffer.wrap(from).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
		short[] originalShorts = new short[shortBuffer.capacity()];
		shortBuffer.get(originalShorts);
		
		// Get second sample as short
		ShortBuffer shortBuffer2 = ByteBuffer.wrap(tosubtract).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
		short[] toAddShorts = new short[shortBuffer2.capacity()];
		shortBuffer2.get(toAddShorts);
		
		// Subtract them together in a short array
		short[] finalShorts = new short[shortBuffer2.capacity()];
		for (int i = 0; i < finalShorts.length; i++) {
			int sum = (int) ((originalShorts[i] - toAddShorts[i]));
			if (sum > Short.MAX_VALUE) {
				finalShorts[i] = Short.MAX_VALUE;
			} else if (sum < Short.MIN_VALUE) {
				finalShorts[i] = Short.MIN_VALUE;
			} else
				finalShorts[i] = (short) (sum);
		}
		
		// Convert short array to byte array
		ByteBuffer buffer = ByteBuffer.allocate(finalShorts.length * 2);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.asShortBuffer().put(finalShorts);
		return buffer.array();
	}
	
	
}
