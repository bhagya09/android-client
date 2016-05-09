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
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
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
import android.widget.Toast;

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
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPDataPacket.PacketType;
import com.bsb.hike.voip.protobuf.VoIPSerializer;
import com.bsb.hike.chatthread.ChatThreadActivity;

public class VoIPUtils {

	private static String tag = VoIPConstants.TAG + " Utils";
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
		CHAT_THREAD, PROFILE_ACTIVITY, MISSED_CALL_NOTIF, CALL_FAILED_FRAG, ADD_TO_CONFERENCE, GROUP_CHAT, HIKE_STICKY_CALLER
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
    	        Logger.e(tag, "Unable to get host address.");
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
    		Logger.w(tag, "Null phone number while adding message to chat thread. Message: " + messageType + ", Duration: " + duration + ", Phone: " + clientPartner.getPhoneNumber());
    		return;
    	}
    		
    	Logger.d(tag, "Adding message to chat thread. Message: " + messageType + ", Duration: " + duration + ", Phone: " + clientPartner.getPhoneNumber());
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
		if(messageType.equals(HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING) || messageType.equals(HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_OUTGOING))
		{
			selfGenerated = false;
		}

		try
		{
			Logger.d(tag, "Adding message of type: " + messageType + " to chat thread.");
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
			Logger.w(tag, "addMessageToChatThread() JSONException: " + e.toString());
		}    	
    }

    /**
     * Put a missed call notification on the other client's chat thread. 
     * @param toMsisdn
     * @param fromMsisdn Pass null to automatically use your own msisdn.
     */
	public static void sendMissedCallNotificationToPartner(String toMsisdn, String fromMsisdn) {

		try {
			JSONObject metaData = new JSONObject();
			metaData.put("time", System.currentTimeMillis());
			
			if (!TextUtils.isEmpty(fromMsisdn))
				metaData.put(HikeConstants.MSISDN, fromMsisdn);
			
			JSONObject data = new JSONObject();
			data.put(HikeConstants.MESSAGE_ID, new Random().nextInt(10000));
			data.put(HikeConstants.TIMESTAMP, System.currentTimeMillis() / 1000); 
			data.put(HikeConstants.METADATA, metaData);

			JSONObject message = new JSONObject();
			message.put(HikeConstants.TO, toMsisdn);
			message.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE_VOIP_1);
			message.put(HikeConstants.SUB_TYPE, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING);
			message.put(HikeConstants.DATA, data);
			
			HikeMqttManagerNew.getInstance().sendMessage(message, MqttConstants.MQTT_QOS_ONE);
			Logger.d(tag, "Sent missed call notifier to partner.");
			
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
//		Logger.w(tag, "Our connection class: " + connection.name());
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
	
	/**
	 * Whether to show the ratings popup or not. It is controlled by -
	 * 1. There is a upper limit on how many times we can ask a user to rate. This is hard coded
	 * in {@link VoIPConstants#DEFAULT_MAX_RATINGS_REQUESTS}, but can also be set by the server
	 * by sending a {@link HikeConstants#VOIP_RATINGS_LEFT} config packet.
	 * 2. We can set a probability of the ratings popup showing up. Also controlled by a config
	 * packet from the server.
	 * @return
	 */
	public static boolean shouldShowCallRatePopupNow()
	{
		HikeSharedPreferenceUtil sharedPref = HikeSharedPreferenceUtil.getInstance();

		int ratingsLeft = sharedPref.getData(HikeConstants.VOIP_RATINGS_LEFT, VoIPConstants.DEFAULT_MAX_RATINGS_REQUESTS);

		if (ratingsLeft > 0) {
			int frequency = sharedPref.getData(HikeMessengerApp.VOIP_CALL_RATE_POPUP_FREQUENCY, -1);
			if (frequency > 0) {
				boolean showNow = ((new Random().nextInt(frequency) + 1) == frequency);
				if (showNow) {
					sharedPref.saveData(HikeConstants.VOIP_RATINGS_LEFT, --ratingsLeft);
					Logger.d(tag, "Showing rating popup. Ratings left: " + ratingsLeft);
					return true;
				} else
					return false;
			}
			else
				return false;
		} else
			return false;
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
		Intent messageIntent = IntentFactory.createChatThreadIntentFromMsisdn(context, msisdn, true,false, ChatThreadActivity.ChatThreadOpenSources.NOTIF);
		PendingIntent messagePendingIntent = PendingIntent.getActivity(context, 0, messageIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Action actions[] = null;
		
		if (OneToNConversationUtils.isGroupConversation(msisdn)) {
			actions = new NotificationCompat.Action[1];
			actions[0] = new NotificationCompat.Action(R.drawable.ic_action_message, context.getString(R.string.voip_missed_call_message), messagePendingIntent);
		} else {
			Intent callIntent = IntentFactory.getVoipCallIntent(context, msisdn, CallSource.MISSED_CALL_NOTIF);
			PendingIntent callPendingIntent = PendingIntent.getService(context, 0, callIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			
			actions = new NotificationCompat.Action[2];
			actions[0] = new NotificationCompat.Action(R.drawable.ic_action_call, context.getString(R.string.voip_missed_call_action), callPendingIntent);
			actions[1] = new NotificationCompat.Action(R.drawable.ic_action_message, context.getString(R.string.voip_missed_call_message), messagePendingIntent);
		}

		return actions;
	}
	
	public static boolean useAEC(Context context)
	{
		return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.VOIP_AEC_ENABLED, true);
	}
	
	public static boolean isConferencingEnabled(Context context) 
	{
		boolean voipEnabled = Utils.isVoipActivated(context);
		if (voipEnabled == false)
			return false;
		
		boolean conferenceEnabled = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.VOIP_CONFERENCING_ENABLED, false);
		return conferenceEnabled;
	}
	
	public static boolean isGroupCallEnabled(Context context) 
	{
		boolean voipEnabled = Utils.isVoipActivated(context);
		if (voipEnabled == false)
			return false;
		
		boolean enabled = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.VOIP_GROUP_CALL_ENABLED, false);
		return enabled;
	}

	public static boolean isBluetoothEnabled(Context context)
	{
		boolean bluetoothEnabled = true;
		return bluetoothEnabled;
	}
	
	/**
	 * <p>Check if we can host a conference or not. </p>
	 * <p>
	 * Following is checked - <br/>
	 * 1. If the device supports KitKat or above. <br/>
	 * 2. If we are currently connected to a network that supports conference. <br/>
	 * 3. If the conference group size is under the defined limit. <br/>
	 * 4. If user is online. <br/>
	 * </p>
	 * A toast will be shown if any error is encountered. 
	 * @param context
	 * @return
	 */
	public static boolean checkIfConferenceIsAllowed(Context context, int newSize) {
		
		// OS check
		if (!Utils.isIceCreamOrHigher()) {
			Toast.makeText(context, context.getString(R.string.voip_conference_os_support), Toast.LENGTH_LONG).show();
			return false;
		}
		
		// Conference size check
		if (newSize > VoIPConstants.MAXIMUM_GROUP_CHAT_SIZE) {
			Toast.makeText(context, context.getString(R.string.voip_group_too_large, VoIPConstants.MAXIMUM_GROUP_CHAT_SIZE), Toast.LENGTH_LONG).show();
			return false;
		}
		
		// User online check
		if (!Utils.isUserOnline(context))
		{
			Toast.makeText(context, context.getString(R.string.voip_offline_error), Toast.LENGTH_SHORT).show();
			return false;
		}
		
		// Network check
		ConnectionClass connectionClass = VoIPUtils.getConnectionClass(HikeMessengerApp.getInstance());
		if (connectionClass == ConnectionClass.TwoG) {
			Toast.makeText(context, context.getString(R.string.voip_conference_network_support), Toast.LENGTH_LONG).show();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Used to communicate between two clients using the server. Delivery is not guaranteed.
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
			
			HikeMqttManagerNew.getInstance().sendMessage(message, MqttConstants.MQTT_QOS_ZERO);
			Logger.d(tag, "Sent call request message of type: " + callMessage + " to: " + recipient);

		} catch (JSONException e) {
			e.printStackTrace();
			Logger.w(tag, "sendSocketInfoToPartner JSON error: " + e.toString());
		} 
	}

	public static void handleVOIPPacket(Context context, JSONObject jsonObj) 
	{
		try {
			// VoIP checks
			if (jsonObj.has(HikeConstants.SUB_TYPE)) 
			{	
				String subType = jsonObj.getString(HikeConstants.SUB_TYPE);
				Logger.d(tag, "Message subtype: " + subType);

				if (subType.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_CANCELLED)) {
					// Check for call cancelled message
					JSONObject metadataJSON = jsonObj.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);

					if (metadataJSON.getInt(VoIPConstants.Extras.CALL_ID) != VoIPService.getCallId()) {
						Logger.d(tag, "Ignoring call cancelled message. local: " + VoIPService.getCallId() +
								", remote: " + metadataJSON.getInt(VoIPConstants.Extras.CALL_ID));
						return;
					}
					
					Intent i = new Intent(context.getApplicationContext(), VoIPService.class);
					i.putExtra(VoIPConstants.Extras.ACTION, subType);
					i.putExtra(VoIPConstants.Extras.MSISDN, jsonObj.getString(HikeConstants.FROM));
					i.putExtra(VoIPConstants.Extras.CALL_ID, metadataJSON.getInt(VoIPConstants.Extras.CALL_ID));
					context.startService(i);
					return;
				}
				
				if (subType.equals(HikeConstants.MqttMessageTypes.VOIP_SOCKET_INFO) ||
						subType.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_REQUEST) ||
						subType.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_REQUEST_RESPONSE) ||
						subType.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_RESPONSE_RESPONSE)) {
					
					if (!Utils.isVoipActivated(context))
						return;
					
					JSONObject metadataJSON = jsonObj.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);
					
					// Check if the initiator (us) has already hung up
					if (metadataJSON.getBoolean(VoIPConstants.Extras.INITIATOR) == false &&
							metadataJSON.getInt(VoIPConstants.Extras.CALL_ID) != VoIPService.getCallId()) {
						Logger.w(tag, "Receiving a reply for a terminated call. local: " + VoIPService.getCallId() +
								", remote: " + metadataJSON.getInt(VoIPConstants.Extras.CALL_ID));
						return;		
					}

					/*
					 * Call Initiation Messages
					 * Added: 24 Mar, 2015 (AJ)
					 * These are being used purely for analytics.
					 */
					if (subType.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_REQUEST) ||
							subType.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_REQUEST_RESPONSE) ||
							subType.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_RESPONSE_RESPONSE)) {

//					Logger.w(tag, "Received: " + subType);

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
							
						/*
						 * Socket information is the same as a request for call initiation. 
						 * The calling party sends its socket information to the callee, and
						 * the callee at that point should start its voip service (and not the 
						 * activity) so it can reply with its own socket information. 
						 */
						Logger.d(tag, "Receiving socket info..");
						
						Intent i = new Intent(context.getApplicationContext(), VoIPService.class);
						i.putExtra(VoIPConstants.Extras.ACTION, VoIPConstants.Extras.SET_PARTNER_INFO);
						i.putExtra(VoIPConstants.Extras.MSISDN, jsonObj.getString(HikeConstants.FROM));
						
						if (metadataJSON.has(VoIPConstants.Extras.INTERNAL_IP))
							i.putExtra(VoIPConstants.Extras.INTERNAL_IP, metadataJSON.getString(VoIPConstants.Extras.INTERNAL_IP));
						
						i.putExtra(VoIPConstants.Extras.INTERNAL_PORT, metadataJSON.getInt(VoIPConstants.Extras.INTERNAL_PORT));
						i.putExtra(VoIPConstants.Extras.EXTERNAL_IP, metadataJSON.optString(VoIPConstants.Extras.EXTERNAL_IP));
						i.putExtra(VoIPConstants.Extras.EXTERNAL_PORT, metadataJSON.optInt(VoIPConstants.Extras.EXTERNAL_PORT));
						i.putExtra(VoIPConstants.Extras.RELAY, metadataJSON.optString(VoIPConstants.Extras.RELAY));
						i.putExtra(VoIPConstants.Extras.RELAY_PORT, metadataJSON.optInt(VoIPConstants.Extras.RELAY_PORT));
						i.putExtra(VoIPConstants.Extras.RECONNECTING, metadataJSON.getBoolean(VoIPConstants.Extras.RECONNECTING));
						i.putExtra(VoIPConstants.Extras.INITIATOR, metadataJSON.getBoolean(VoIPConstants.Extras.INITIATOR));
						i.putExtra(VoIPConstants.Extras.CALL_ID, metadataJSON.getInt(VoIPConstants.Extras.CALL_ID));
						
						if (metadataJSON.has(VoIPConstants.Extras.VOIP_VERSION))
							i.putExtra(VoIPConstants.Extras.VOIP_VERSION, metadataJSON.getInt(VoIPConstants.Extras.VOIP_VERSION));
						
						if (metadataJSON.has(VoIPConstants.Extras.GROUP_CHAT_MSISDN))
							i.putExtra(VoIPConstants.Extras.GROUP_CHAT_MSISDN, metadataJSON.getString(VoIPConstants.Extras.GROUP_CHAT_MSISDN));
						
						if (metadataJSON.has(VoIPConstants.Extras.CONFERENCE))
							i.putExtra(VoIPConstants.Extras.CONFERENCE, metadataJSON.getBoolean(VoIPConstants.Extras.CONFERENCE));
						
						context.startService(i);
						return;
					}
				}
				
				if (subType.equals(HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING)) 
				{
					Logger.d(tag, "Adding a missed call to our chat history.");
					JSONObject metadataJSON = jsonObj.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);

					VoIPClient clientPartner = new VoIPClient(context, null);
					
					if (metadataJSON.has(HikeConstants.MSISDN))
						clientPartner.setPhoneNumber(metadataJSON.getString(HikeConstants.MSISDN));
					else
						clientPartner.setPhoneNumber(jsonObj.getString(HikeConstants.FROM));
					
					clientPartner.setInitiator(true);
					VoIPUtils.addMessageToChatThread(context, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING, 0, jsonObj.getJSONObject(HikeConstants.DATA).getLong(HikeConstants.TIMESTAMP), true);
				}
				
				if (subType.equals(HikeConstants.MqttMessageTypes.VOIP_ERROR_CALLEE_INCOMPATIBLE_NOT_UPGRADABLE) ||
						subType.equals(HikeConstants.MqttMessageTypes.VOIP_ERROR_CALLEE_DOES_NOT_SUPPORT_CONFERENCE) ||
						subType.equals(HikeConstants.MqttMessageTypes.VOIP_ERROR_ALREADY_IN_CALL) ||
						subType.equals(HikeConstants.MqttMessageTypes.VOIP_ERROR_CALLEE_INCOMPATIBLE_UPGRADABLE) ||
						subType.equals(HikeConstants.MqttMessageTypes.VOIP_ERROR_CUSTOM_MESSAGE))
				{
					Intent i = new Intent(context, VoIPService.class);
					i.putExtra(VoIPConstants.Extras.ACTION, subType);
					i.putExtra(VoIPConstants.Extras.MSISDN, jsonObj.getString(HikeConstants.FROM));

					// Parse the custom error message that _might_ be included
					if (jsonObj.has(HikeConstants.DATA) && jsonObj.getJSONObject(HikeConstants.DATA).has(HikeConstants.CUSTOM_MESSAGE))
						i.putExtra(VoIPConstants.Extras.CUSTOM_MESSAGE, jsonObj.getJSONObject(HikeConstants.DATA).getString(HikeConstants.CUSTOM_MESSAGE));

					context.startService(i);
				}
			}
		} catch (JSONException e) {
			Logger.w(tag, "JSONException: " + e.toString());
		}
	}
	
	public static byte[] addPCMSamples(byte[] original, byte[] toadd) {
		
		if (original.length != toadd.length) {
			Logger.w(tag, "PCM samples length does not match (A). " +
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
			Logger.w(tag, "PCM samples length does not match (S). " +
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

	/**
	 * If DNS lookup on relay servers fails, then this method will return a randomly selected
	 * IP address of a relay server. The client has a hardcoded list, and this list can be altered by
	 * sending a configuration packet.
	 * <p>
	 * <b>Warning:</b> The configuration packet for a new list of relay servers will only work
	 * above (and including) Honeycomb (>= API v11). Consider fixing this in the future, or
	 * dropping support for Gingerbread. 
	 * </p> 
	 * 
	 * @return InetAddress 
	 */
	public static InetAddress getRelayIpFromHardcodedAddresses() {

		Set<String> ipSet = HikeSharedPreferenceUtil.getInstance().getDataSet(HikeConstants.VOIP_RELAY_IPS, null);
		Random random = new Random();
		int index = 0;
		InetAddress address = null;
		
		try {
			String ip = null;
			if (ipSet != null && ipSet.size() > 0) {
				// Pick an IP from the list sent by ac packet
				index = random.nextInt(ipSet.size());
				int i = 0;
				for (String str : ipSet) {
					if (i++ == index) {
						ip = str;
						break;
					}
				}
			}
			else {
				// Pick an IP from hardcoded list
				index = random.nextInt(VoIPConstants.ICEServerIpAddresses.length);
				ip = VoIPConstants.ICEServerIpAddresses[index];
			}

			address = InetAddress.getByName(ip);
		} catch (UnknownHostException e) {
			Logger.w(tag, "Unable to retrieve hardcoded relay IP address.");
		}
		
		Logger.d(tag, "Retrieved IP address for relay server: " + address.getHostAddress());
		return address;
	}

	/**
	 * Helper function to display total and available memory using Logger.
	 * @param context
	 */
	public static void showMemoryUsage(Context context) {
		
		MemoryInfo mi = new MemoryInfo();
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(mi);
		long availableMegs = mi.availMem / 1048576L;
		long totalMemory = mi.totalMem / 1048576L;
		
		Logger.d(tag, "Memory total: " + totalMemory + " MB, available: " + availableMegs + " MB");
	}

	public static byte[] getUDPDataFromPacket(VoIPDataPacket dp) {
		
		// Serialize everything except for P2P voice data packets
		byte[] packetData = null;
		byte prefix;
		
		// Force everything to PB
		packetData = VoIPSerializer.serialize(dp);
		prefix = VoIPConstants.PP_PROTOCOL_BUFFER;

		if (packetData == null)
			return null;
		
		byte[] finalData = new byte[packetData.length + 1];	
		finalData[0] = prefix;
		System.arraycopy(packetData, 0, finalData, 1, packetData.length);
		packetData = finalData;

		return packetData;
	}

	public static VoIPDataPacket getPacketFromUDPData(byte[] data, int dataLength) {
		VoIPDataPacket dp;
		byte prefix = data[0];

		if (prefix == VoIPConstants.PP_PROTOCOL_BUFFER) {
			dp = (VoIPDataPacket) VoIPSerializer.deserialize(data, dataLength);
		} else {
			// This code path should no longer be used, except when communicating with a very old
			// client.
			byte[] packetData = new byte[dataLength - 1];
			System.arraycopy(data, 1, packetData, 0, packetData.length);
			dp = new VoIPDataPacket(PacketType.AUDIO_PACKET);
			dp.setData(packetData);
			if (prefix == VoIPConstants.PP_ENCRYPTED_VOICE_PACKET)
				dp.setEncrypted(true);
			else
				dp.setEncrypted(false);
		}
		
		return dp;
	}
	
	/**
	 * Returns true if we are already in an active call, 
	 * and notifies the caller.
	 * @param context
	 * @param fromMsisdn
	 * @param callId
	 * @return
	 */
	public static boolean checkForActiveCall(Context context, String fromMsisdn, int callId, boolean insertMissedCall) {
		// Check for currently active call
		if ((callId != VoIPService.getCallId() && VoIPService.getCallId() > 0) ||
				VoIPUtils.isUserInCall(context)) {
			Logger.w(tag, "We are already in a call. local: " + VoIPService.getCallId() +
					", remote: " + callId);

			if (insertMissedCall)
				VoIPUtils.sendVoIPMessageUsingHike(fromMsisdn, 
						HikeConstants.MqttMessageTypes.VOIP_ERROR_ALREADY_IN_CALL, 
						callId, 
						false);
			return true;
		}
		return false;
	}

	/**
	 * Returns a <b>PendingIntent</b> that can be caused for setting
	 * up the action buttons in a VoIP notification.
	 * @param context
	 * @param requestCode
	 * @param action
	 * @return
	 */
	public static PendingIntent getPendingIntentForVoip(Context context, int requestCode, String action) {
		Intent intent = new Intent(context, VoIPService.class);
		intent.putExtra(VoIPConstants.Extras.ACTION, action);
		return PendingIntent.getService(context, requestCode, intent, 0);
	}
	
	/**
	 * Request all temporary system dialogs to dismiss. 
	 * This causes the notification drawer to close as well.
	 * @param context
	 */
	public static void closeSystemDialogs(Context context) {
		// Close all system dialogs and/or the notification bar
		Intent closeDiaogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		context.sendBroadcast(closeDiaogs);
	}
}
