package com.bsb.hike.voip;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.media.audiofx.AutomaticGainControl;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.widget.Chronometer;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPClient.ConnectionMethods;
import com.bsb.hike.voip.VoIPConstants.CallStatus;
import com.bsb.hike.voip.VoIPDataPacket.BroadcastListItem;
import com.bsb.hike.voip.VoIPDataPacket.PacketType;
import com.bsb.hike.voip.VoIPUtils.CallSource;
import com.bsb.hike.voip.view.VoIPActivity;
import com.musicg.dsp.Resampler;

public class VoIPService extends Service implements Listener
{
	
	private final IBinder myBinder = new LocalBinder();
	private static final int NOTIFICATION_IDENTIFIER = 10;
	private final static String tag = VoIPConstants.TAG + " Service";

	private Messenger mMessenger;
	private boolean reconnectingBeeps = false;
	private volatile boolean keepRunning;
	private boolean mute, hold, speaker, vibratorEnabled = true;
	private int minBufSizePlayback, minBufSizeRecording;
	private AudioTrack audioTrack = null;
	private static int callId = 0;
	private NotificationManager notificationManager;
	private NotificationCompat.Builder builder;
	private AudioManager audioManager;
	private int initialAudioMode;
	private boolean initialSpeakerMode;
	private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener;
	private int playbackSampleRate = 0, recordingSampleRate = 0;
	private boolean inCellularCall = false;
	public boolean recordingAndPlaybackRunning = false;
	
	// Conference related
	private boolean conferencingEnabled = false;
	private boolean hostingConference = false;
	private boolean forceMute = false, hostForceMute = false;
	private boolean speechDetected = true;
	private TextToSpeech tts = null;
	
	// Task executors
	private Thread processRecordedSamplesThread = null, bufferSendingThread = null, reconnectingBeepsThread = null;
	private Thread connectionTimeoutThread = null;
	private Thread recordingThread = null, playbackThread = null;
	private Thread notificationThread = null;
	private Thread conferenceBroadcastThread = null;
	private ScheduledExecutorService scheduledExecutorService = null;
	private ScheduledFuture<?> scheduledFuture = null;
	
	// Attached VoIP client(s)
	HashMap<String, VoIPClient> clients = new HashMap<String, VoIPClient>();

	// Ringtones (incoming and outgoing)
	private Ringtone ringtone;
	private Vibrator vibrator = null;
	private int ringtoneStreamID = 0;
	private boolean isRingingIncoming = false, isRingingOutgoing = false;

	// Sounds
	private volatile SoundPool soundpool = null;
	private volatile SparseIntArray soundpoolMap;
	private static final int SOUND_ACCEPT = R.raw.call_answer;
	private static final int SOUND_DECLINE = R.raw.call_end;
	private static final int SOUND_INCOMING_RINGTONE = R.raw.ring_tone;
	private static final int SOUND_RECONNECTING = R.raw.reconnect;

	// Wakelock
	private WakeLock wakeLock = null;
	
	// Resampler
	private boolean resamplerEnabled = true;
	private Resampler resampler = null;

	// Echo cancellation
	private boolean aecEnabled = true;
	private SolicallWrapper solicallAec = null;
	private boolean aecSpeakerSignal = false, aecMicSignal = false;
	
	// Buffer queues
	private final LinkedBlockingQueue<VoIPDataPacket> recordedSamples     = new LinkedBlockingQueue<>(VoIPConstants.MAX_SAMPLES_BUFFER);
	private final LinkedBlockingQueue<VoIPDataPacket> processedRecordedSamples      = new LinkedBlockingQueue<VoIPDataPacket>();
	private final LinkedBlockingQueue<VoIPDataPacket> playbackBuffersQueue      = new LinkedBlockingQueue<VoIPDataPacket>();
	private final LinkedBlockingQueue<VoIPDataPacket> conferenceBroadcastPackets      = new LinkedBlockingQueue<>();
	private final CircularByteBuffer recordBuffer = new CircularByteBuffer();
	
	// Runnable for sending clients list to all clients
	private Runnable clientListRunnable = null;
	private Handler clientListHandler;
	
	// Broadcast listeners
	private BroadcastReceiver phoneStateReceiver = null, powerButtonReceiver = null;
	
	// Support for conference calls
	private Chronometer chronometer = null;
	private String groupChatMsisdn; 
	private DatagramSocket broadcastSocket = null;
	
	// Listener for declining call with a message
	String pubSubListeners[] = {HikePubSub.REJECT_INCOMING_CALL};

	// Bluetooth 
	private boolean isBluetoothEnabled = false;
	private boolean ignoreBluetoothDisconnect;
	BluetoothHelper bluetoothHelper = null;
	private class BluetoothHelper extends BluetoothHeadsetUtils {

		public BluetoothHelper(Context context) {
			super(context);
		}

		@Override
		public void onHeadsetDisconnected() {
			Logger.d(tag, "Bluetooth onHeadsetDisconnected()");
		}

		@Override
		public void onHeadsetConnected() {
			Logger.d(tag, "Bluetooth onHeadsetConnected()");
		}

		@Override
		public void onScoAudioDisconnected() {
			Logger.d(tag, "Bluetooth onScoAudioDisconnected()");
			audioManager.stopBluetoothSco();
			audioManager.setBluetoothScoOn(false);
			if (!ignoreBluetoothDisconnect)
				hangUp();
			else
				ignoreBluetoothDisconnect = false;
		}

		@Override
		public void onScoAudioConnected() {
			Logger.d(tag, "Bluetooth onScoAudioConnected()");
			audioManager.startBluetoothSco();
			audioManager.setBluetoothScoOn(true);
			sendMessageToActivity(VoIPConstants.MSG_BLUETOOTH_SHOW);
		}
	}
	
	public boolean isOnHeadsetSco() {
		if (bluetoothHelper != null && bluetoothHelper.isOnHeadsetSco())
			return true;
		else
			return false;
	}
	
	public void toggleBluetoothFromActivity(boolean enabled) {
		if (enabled) {
			audioManager.setBluetoothScoOn(true);
		} else {
			ignoreBluetoothDisconnect = true;
			audioManager.setBluetoothScoOn(false);
		}
	}
	
	// Handler for messages from VoIP clients
	Handler handler = new Handler() {

		@SuppressWarnings("deprecation")
		@Override
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			String msisdn = bundle.getString(VoIPConstants.MSISDN);
			final VoIPClient client = clients.get(msisdn);
			
			switch (msg.what) {
			case VoIPConstants.MSG_VOIP_CLIENT_STOP:
				Logger.d(tag, msisdn + " has stopped.");
				synchronized (clients) {
					if ((clients.size() == 0) || (clients.size() == 1 && getClient().getPhoneNumber().equals(msisdn)))
						stop();
					else {
						removeFromClients(msisdn);
						playFromSoundPool(SOUND_DECLINE, false);
					}
				}
				break;

			case VoIPConstants.CONNECTION_ESTABLISHED_FIRST_TIME:
				Logger.d(tag, "Connection established with " + msisdn);
				if (client == null)
					return;
				
				if (client.isInitiator()) {
					playIncomingCallRingtone();
				} else {
					if (!recordingAndPlaybackRunning)
						playOutgoingCallRingtone();
					if (hostingConference())
						sendClientsListToAllClients();
				}
				
				// If conference is on force mute, then let the new client know
				if (hostingConference() && hostForceMute) {
					new Thread(new Runnable() {
						
						@Override
						public void run() {
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.FORCE_MUTE_ON);
							client.sendPacket(dp, true);
						}
					}).start();
				}
					
				sendMessageToActivity(VoIPConstants.CONNECTION_ESTABLISHED_FIRST_TIME);
				break;

			case VoIPConstants.MSG_UPDATE_REMOTE_HOLD:
				if (client == null)
					return;
				client.setCallStatus(!hold && !client.remoteHold ? VoIPConstants.CallStatus.ACTIVE : VoIPConstants.CallStatus.ON_HOLD);
				sendMessageToActivity(VoIPConstants.MSG_UPDATE_REMOTE_HOLD);
				break;

			case VoIPConstants.MSG_START_RECORDING_AND_PLAYBACK:
				startRecordingAndPlayback(msisdn);
				break;
				
			case VoIPConstants.MSG_START_RECONNECTION_BEEPS:
				startReconnectBeeps();
				break;
				
			case VoIPConstants.MSG_STOP_RECONNECTION_BEEPS:
				if (reconnectingBeepsThread != null) {
					reconnectingBeepsThread.interrupt();
					reconnectingBeepsThread = null;
				}
				break;
				
			case VoIPConstants.MSG_CONNECTED:
				initializeAEC();
				if (hostingConference())
					sendClientsListToAllClients();
				break;
				
			case VoIPConstants.MSG_SHUTDOWN_ACTIVITY:
				if (bundle.getBoolean(VoIPConstants.IS_CONNECTED) == true) {
					setSpeaker(true);
					playFromSoundPool(SOUND_DECLINE, false);
				}
				
				if (!hostingConference())
					stop();
				break;

			case VoIPConstants.MSG_UPDATE_SPEAKING:
				if (hostingConference())
					sendClientsListToAllClients();
				sendMessageToActivity(VoIPConstants.MSG_UPDATE_SPEAKING);
				break;
				
			case VoIPConstants.MSG_UPDATE_FORCE_MUTE_LAYOUT:
				if (client == null) return;
				
				if (forceMute != client.forceMute) {
					forceMute = client.forceMute;
					Logger.d(tag, "Force mute: " + forceMute);
					if (forceMute == true) {
						setMute(forceMute);
					} 
					// Text to speech
					if (recordingAndPlaybackRunning) {
						if (forceMute == true) 
							tts.speak(getString(R.string.voip_speech_force_mute_on), TextToSpeech.QUEUE_FLUSH, null);
						 else
							tts.speak(getString(R.string.voip_speech_force_mute_off), TextToSpeech.QUEUE_FLUSH, null);
					}

					sendMessageToActivity(VoIPConstants.MSG_UPDATE_FORCE_MUTE_LAYOUT);
				}
				break;
				
			case VoIPConstants.MSG_PARTNER_ANSWER_TIMEOUT:
				hangUp();
				sendMessageToActivity(VoIPConstants.MSG_PARTNER_ANSWER_TIMEOUT);
				break;
				
			default:
				// Pass message to activity through its handler
				sendMessageToActivity(msg.what);
				break;
			}
			super.handleMessage(msg);
		}
		
	};

	@Override
	public IBinder onBind(Intent intent) {
		return myBinder;
	}

	public class LocalBinder extends Binder {
		public VoIPService getService() {
			return VoIPService.this;
		}
	}
	
	@SuppressLint("InlinedApi") @Override
	public void onCreate() {
		super.onCreate();

		Logger.d(tag, "VoIPService onCreate()");
		acquireWakeLock();
		setCallid(0);
		initAudioManager();
		keepRunning = true;
		isRingingIncoming = false;
		
		if (!VoIPUtils.useAEC(getApplicationContext())) {
			Logger.w(tag, "AEC disabled.");
			aecEnabled = false;
		}
		
		if (VoIPUtils.isConferencingEnabled(getApplicationContext())) {
			Logger.d(tag, "Conferencing enabled.");
			conferencingEnabled = true;
		}
		
		if (resamplerEnabled && resampler == null) 
			resampler = new Resampler();
		
		// Initialize text to speech
		tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
			
			@Override
			public void onInit(int status) {
				if (status != TextToSpeech.ERROR)
					tts.setLanguage(Locale.getDefault());
				else
					Logger.w(tag, "Error initializing text to speech.");
			}
		});

		startConnectionTimeoutThread();
		registerBroadcastReceivers();
		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);

	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		stop();
		dismissNotification();
		unregisterBroadcastReceivers();
		
		if (bluetoothHelper != null) {
			bluetoothHelper.stop();
			bluetoothHelper = null;
		}
		
		if (tts != null) {
			tts.stop();
			tts.shutdown();
		}
		
		Logger.d(tag, "VoIP Service destroyed.");
	}
	
	@Override
	synchronized public int onStartCommand(Intent intent, int flags, int startId) {
		
		int returnInt = super.onStartCommand(intent, flags, startId);
		
		 Logger.d(tag, "VoIPService onStartCommand()");

		if (intent == null)
			return returnInt;

		String action = intent.getStringExtra(VoIPConstants.Extras.ACTION);
		final String msisdn = intent.getStringExtra(VoIPConstants.Extras.MSISDN);
		if (action == null || action.isEmpty()) {
			return returnInt;
		}

		VoIPClient client = clients.get(msisdn);
		if (client == null && !TextUtils.isEmpty(msisdn)) {
			Logger.d(tag, "Creating VoIPClient for: " + msisdn);
			client = new VoIPClient(getApplicationContext(), handler);
			client.setPhoneNumber(msisdn);
		}

		// Call rejection message
		if (action.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_CANCELLED)) {
			Logger.d(tag, "Call cancelled message from: " + msisdn);
			if (keepRunning == true && getClient(msisdn) != null) {
				Logger.w(tag, "Hanging up " + msisdn + " because of call cancelled message.");
				getClient(msisdn).hangUp();
			} 
			return returnInt;
		}
		
		// Participant does not support conference error
		if (action.equals(HikeConstants.MqttMessageTypes.VOIP_ERROR_CALLEE_DOES_NOT_SUPPORT_CONFERENCE)) {
			Logger.w(tag, msisdn + " does not support conferencing.");
			VoIPClient cl = getClient(msisdn);
			if (cl != null) {
				// Send message to voip activity
				Bundle bundle = new Bundle();
				bundle.putString(VoIPConstants.PARTNER_NAME, cl.getName());
				sendMessageToActivity(VoIPConstants.MSG_DOES_NOT_SUPPORT_CONFERENCE, bundle);
				cl.hangUp();
			}
		}

		// Recipient is already in a call
		if (action.equals(HikeConstants.MqttMessageTypes.VOIP_ERROR_ALREADY_IN_CALL)) {
			Logger.w(tag, msisdn + " is currently busy.");
			sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.PARTNER_BUSY);
			VoIPClient cl = getClient(msisdn);
			if (cl != null) {
				// Send a missed call alert
				VoIPUtils.sendMissedCallNotificationToPartner(msisdn, 
						TextUtils.isEmpty(cl.groupChatMsisdn) ? null : cl.groupChatMsisdn);

				new Handler().postDelayed(new Runnable() {
					
					@Override
					public void run() {
						// Send message to voip activity
						Bundle bundle = new Bundle();
						bundle.putString(VoIPConstants.MSISDN, msisdn);
						sendMessageToActivity(VoIPConstants.MSG_PARTNER_BUSY, bundle);
						removeFromClients(msisdn);
					}
				}, VoIPConstants.SERVICE_To_ACTIVITY_ERR_MESSAGE_DELAY);
			} else
				Logger.w(tag, "Unable to find the client object who we were calling.");
		}
		
		// Recipient is on an unsupported platform
		if (action.equals(HikeConstants.MqttMessageTypes.VOIP_ERROR_CALLEE_INCOMPATIBLE_NOT_UPGRADABLE)) {
			Logger.w(tag, msisdn + " is on an unsupported platform.");
			sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.PARTNER_INCOMPAT);
			
			new Handler().postDelayed(new Runnable() {
				
				@Override
				public void run() {
					removeFromClients(msisdn);
					final Bundle bundle = new Bundle();
					bundle.putString(VoIPConstants.MSISDN, msisdn);
					sendMessageToActivity(VoIPConstants.MSG_PARTNER_INCOMPATIBLE_PLATFORM, bundle);
				}
			}, VoIPConstants.SERVICE_To_ACTIVITY_ERR_MESSAGE_DELAY);
		}
		
		// Recipient is on an unsupported, but upgradable build
		if (action.equals(HikeConstants.MqttMessageTypes.VOIP_ERROR_CALLEE_INCOMPATIBLE_UPGRADABLE)) {
			Logger.w(tag, msisdn + " needs to upgrade.");
			sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.PARTNER_UPGRADE);
			
			new Handler().postDelayed(new Runnable() {
				
				@Override
				public void run() {
					removeFromClients(msisdn);
					Bundle bundle = new Bundle();
					bundle.putString(VoIPConstants.MSISDN, msisdn);
					sendMessageToActivity(VoIPConstants.MSG_PARTNER_UPGRADABLE_PLATFORM, bundle);
				}
			}, VoIPConstants.SERVICE_To_ACTIVITY_ERR_MESSAGE_DELAY);
		}
		
		// Incoming call message
		if (action.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_REQUEST)) {

			int partnerCallId = intent.getIntExtra(VoIPConstants.Extras.CALL_ID, 0);
			
			if (VoIPUtils.checkForActiveCall(getApplicationContext(), msisdn, partnerCallId, false))
				return returnInt;
			
			setCallid(partnerCallId);
			client.setInitiator(true);
			
			// Send call initiation ack message
			VoIPUtils.sendVoIPMessageUsingHike(msisdn, 
					HikeConstants.MqttMessageTypes.VOIP_CALL_REQUEST_RESPONSE, 
					partnerCallId, 
					false);
		}
		
		// Incoming call ack message
		if (action.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_REQUEST_RESPONSE)) {

			int partnerCallId = intent.getIntExtra(VoIPConstants.Extras.CALL_ID, 0);
			if (getCallId() == 0 || getCallId() != partnerCallId) {
				Logger.w(tag, "Was not expecting message: " + action);
				return returnInt;
			}
			
			// Send call initiation ack ack message
			VoIPUtils.sendVoIPMessageUsingHike(msisdn, 
					HikeConstants.MqttMessageTypes.VOIP_CALL_RESPONSE_RESPONSE, 
					getCallId(), 
					true);
		}
		
		// Incoming call ack ack message
		if (action.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_RESPONSE_RESPONSE)) {

			int partnerCallId = intent.getIntExtra(VoIPConstants.Extras.CALL_ID, 0);
			if (getCallId() == 0 || getCallId() != partnerCallId) {
				Logger.w(tag, "Was not expecting message: " + action);
				return returnInt;
			}

			client.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_HANDSHAKE_COMPLETE);
		}

		// INCOMING CALL
		if (action.equals(VoIPConstants.Extras.SET_PARTNER_INFO)) 
		{
			
			int partnerCallId = intent.getIntExtra(VoIPConstants.Extras.CALL_ID, 0);
						
			if (VoIPUtils.checkForActiveCall(getApplicationContext(), msisdn, partnerCallId, true))
				return returnInt;
			
			// Error case: partner is trying to reconnect to us, but we aren't
			// expecting a reconnect
			boolean partnerReconnecting = intent.getBooleanExtra(VoIPConstants.Extras.RECONNECTING, false);
			if (partnerReconnecting == true && partnerCallId != getCallId()) {
				Logger.w(tag, "Partner trying to reconnect? Remote: " + partnerCallId + ", Self: " + getCallId());
				return returnInt;
			}

			client.setInternalIPAddress(intent.getStringExtra(VoIPConstants.Extras.INTERNAL_IP));
			client.setInternalPort(intent.getIntExtra(VoIPConstants.Extras.INTERNAL_PORT, 0));
			client.setExternalIPAddress(intent.getStringExtra(VoIPConstants.Extras.EXTERNAL_IP));
			client.setExternalPort(intent.getIntExtra(VoIPConstants.Extras.EXTERNAL_PORT, 0));
			client.setInitiator(intent.getBooleanExtra(VoIPConstants.Extras.INITIATOR, true));
			client.setRelayAddress(intent.getStringExtra(VoIPConstants.Extras.RELAY));
			client.setRelayPort(intent.getIntExtra(VoIPConstants.Extras.RELAY_PORT, VoIPConstants.ICEServerPort));
			client.setVersion(intent.getIntExtra(VoIPConstants.Extras.VOIP_VERSION, 1));

			if (intent.hasExtra(VoIPConstants.Extras.GROUP_CHAT_MSISDN)) {
				client.groupChatMsisdn = intent.getStringExtra(VoIPConstants.Extras.GROUP_CHAT_MSISDN);
				client.isHostingConference = true;
				Logger.d(tag, "We are going to participate in a group chat conference with msisdn: " + client.groupChatMsisdn);
			}
			
			if (intent.hasExtra(VoIPConstants.Extras.CONFERENCE))
				client.isHostingConference = true;
			
			// Error case: we are receiving a delayed v0 message for a call we 
			// initiated earlier. 
			if (!client.isInitiator() && partnerCallId != getCallId()) {
				Logger.w(tag, "Receiving a return v0 for a invalid call.");
				return returnInt;
			}
				
			// Error case: we are receiving a repeat v0 during call setup
			if (client.socketInfoReceived && !partnerReconnecting) {
				Logger.d(tag, "Repeat call initiation message.");
				// Try sending our socket info again. Caller could've missed our original message.
				if (!client.connected)
					client.sendSocketInfoToPartner();
				
				return returnInt;
			}
			
			// Check in case the other client is reconnecting to us
			if (client.connected && partnerCallId == getCallId() && partnerReconnecting) {
				Logger.w(tag, "Partner trying to reconnect with us. CallId: " + getCallId());
				if (!client.reconnecting) {
					client.reconnect();
				} 
				if (client.socketInfoSent)
					client.establishConnection();
			} else {
				// All good. 
				setCallid(partnerCallId);
				if (client.isInitiator() && !client.reconnecting) {
					Logger.w(tag, "Detected incoming VoIP call from: " + client.getPhoneNumber());
					addToClients(client);
					client.retrieveExternalSocket();
				} else {
					// We have already sent our socket info to partner
					// And now they have sent us their's, so let's establish connection
					client.establishConnection();
				}
			}

			client.socketInfoReceived = true;
		}
		
		// OUTGOING CALL
		if (action.equals(VoIPConstants.Extras.OUTGOING_CALL)) 
		{
			String myMsisdn = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(HikeMessengerApp.MSISDN_SETTING, null);
			
			// Error case: making a call to yourself
			if (myMsisdn != null && myMsisdn.equals(msisdn)) 
			{
				Logger.wtf(tag, "Don't be ridiculous!");
				if (clients.size() == 0)
					stop();
				return returnInt;
			}
			
			// Error case: we are in a cellular call
			if (VoIPUtils.isUserInCall(getApplicationContext())) 
			{
				Logger.w(tag, "We are already in a cellular call.");
				sendMessageToActivity(VoIPConstants.MSG_ALREADY_IN_NATIVE_CALL);
				if (client == null)	// In case of a group call
					client = new VoIPClient(getApplicationContext(), null);
				client.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.CALLER_IN_NATIVE_CALL);
				return returnInt;
			}

			// Error case: call button was hit for someone we are already speaking with. 
			if (getCallId() > 0 && getClient() != null && getClient().getPhoneNumber() != null && getClient().getPhoneNumber().equals(msisdn)) 
			{
				if (!getClient().connected) {
					Logger.e(tag, "Still trying to connect.");
					return returnInt;
				}
				
				// Show activity
				restoreActivity();
				return returnInt;
			}

			// Error case: We are already in a voip call
			if (getCallId() > 0 && !conferencingEnabled) 
			{
				Logger.e(tag, "Error. Already in a call.");
				return returnInt;
			}
			
			// Error case: We are already in a conference call
			if (getClient() != null && getClient().isHostingConference) {
				Logger.e(tag, "Cannot place call while in a conference.");
				restoreActivity();
				return returnInt;
			}

			// Error case: We are currently receiving a call which we haven't 
			// answered yet
			if (getCallStatus() == CallStatus.INCOMING_CALL) {
				restoreActivity();
				return returnInt;
			}
			
			// All good. Initiate the call
			int callSource = intent.getIntExtra(VoIPConstants.Extras.CALL_SOURCE, -1);
			if (intent.getExtras().containsKey(VoIPConstants.Extras.MSISDNS)) {
				// Group call
				if (intent.hasExtra(VoIPConstants.Extras.GROUP_CHAT_MSISDN))
					groupChatMsisdn = intent.getStringExtra(VoIPConstants.Extras.GROUP_CHAT_MSISDN);
				ArrayList<String> msisdns = intent.getStringArrayListExtra(VoIPConstants.Extras.MSISDNS);
				
				if (!VoIPUtils.checkIfConferenceIsAllowed(getApplicationContext(), clients.size() + msisdns.size()))
					return returnInt;

				restoreActivity();
				
				for (String phoneNumber : msisdns) {
					
					// Check for own phone number in group members
					if (phoneNumber.equals(myMsisdn))
						continue;
					
					client = new VoIPClient(getApplicationContext(), handler);
					client.setPhoneNumber(phoneNumber);
					if (intent.hasExtra(VoIPConstants.Extras.GROUP_CHAT_MSISDN))
						client.groupChatMsisdn = groupChatMsisdn;
					
					initiateOutgoingCall(client, callSource);
					
				}
			} else {
				// Outgoing call to single recipient
				if (clients.size() > 0 && !VoIPUtils.checkIfConferenceIsAllowed(getApplicationContext(), clients.size() + 1))
					return returnInt;
				
				initiateOutgoingCall(client, callSource);
			}
			
			// Show activity
			restoreActivity();
			sendMessageToActivity(VoIPConstants.MSG_UPDATE_CONTACT_DETAILS);
			startBluetooth();
			
			if (clients.size() > 1)
				sendMessageToActivity(VoIPConstants.MSG_UPDATE_FORCE_MUTE_LAYOUT);

		}

		if(client != null && client.getCallStatus() == VoIPConstants.CallStatus.UNINITIALIZED)
			client.setInitialCallStatus();

		return returnInt;
	}

	private void restoreActivity() {
		Logger.d(tag, "Restoring activity..");
		Intent i = new Intent(getApplicationContext(), VoIPActivity.class);
		i.putExtra(VoIPConstants.Extras.REMOVE_FAILED_FRAGMENT, true);
		i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(i);
	}

	private void initiateOutgoingCall(VoIPClient client, int callSource) {
		
		// Check if a call has already been initiated to the client
		if (clients.containsKey(client.getPhoneNumber())) {
			Logger.w(tag, "Client has already been added.");
			restoreActivity();
			return;
		}
		
		// Check if we have blocked this user. 
		// We won't get socket info back from the user, so a voip call will never work
		if (ContactManager.getInstance().isBlocked(client.getPhoneNumber())) {
			Logger.w(tag, "Not attempting call to " + client.getName() + " since they are blocked.");
			return;
		}
		
		client.setInitiator(false);
		client.callSource = callSource;
		
		if(client.callSource == CallSource.MISSED_CALL_NOTIF.ordinal())
			VoIPUtils.cancelMissedCallNotification(getApplicationContext());

		VoIPClient primary = getClient();
		
		if (clients.size() == 1) {
			// We are adding a second client, and hence starting a conference call
			primary.cryptoEnabled = false;
			primary.isInAHostedConference = true;
			primary.setIdealBitrate();
			primary.sendLocalBitrate();
		}
		
		if (clients.size() > 0 && getCallId() > 0) {
			Logger.d(tag, "We're in a conference. Maintaining call id: " + getCallId());
			client.cryptoEnabled = false;
			client.isInAHostedConference = true;

			// Must maintain contact with the same server
			client.setRelayAddress(primary.getRelayAddress());
			client.setRelayPort(primary.getRelayPort());
			
			startConferenceBroadcast();			
		}
		else {
			setCallid(new Random().nextInt(Integer.MAX_VALUE));
			startNotificationThread();
		}
			
		Logger.w(tag, "Making outgoing call to: " + client.getPhoneNumber() + ", id: " + getCallId());
		addToClients(client);

		// Send call initiation message
		VoIPUtils.sendVoIPMessageUsingHike(client.getPhoneNumber(), 
				HikeConstants.MqttMessageTypes.VOIP_CALL_REQUEST, 
				getCallId(), true);

		client.retrieveExternalSocket();
		client.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_CLICK);
	}
	
	private void startConnectionTimeoutThread() {
		
		if (connectionTimeoutThread != null) {
			Logger.d(tag, "Restarting connection timeout thread.");
			connectionTimeoutThread.interrupt();
			connectionTimeoutThread = null;
		}
		
		connectionTimeoutThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(30000);
					
					boolean connected = false;
					for (VoIPClient client : clients.values()) {
						if (client.connected || client.reconnecting)
							connected = true;
					}
					
					if (!connected) {
						Logger.w(tag, "Why aren't we connected yet? Terminating service.");
						keepRunning = true;	// So that stop() is executed entirely. 
						stop();
					}
				} catch (InterruptedException e) {
					Logger.d(tag, "Connection timeout thread interrupted. Das ist gut!");
				}
				
			}
		}, "CONNECTION_TIMEOUT_THREAD");
		
		connectionTimeoutThread.start();
	}
	
	private void acquireWakeLock() 
	{
		if (wakeLock == null) {
			PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HikeWL");
			wakeLock.setReferenceCounted(false);
		}
		
		if (!wakeLock.isHeld()) {
			wakeLock.acquire();
			Logger.d(tag, "Wakelock acquired.");
		}
	}
	
	private void releaseWakeLock() 
	{
		if (wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
			Logger.d(tag, "Wakelock released.");
		} else {
			Logger.d(tag, "Wakelock not detected.");
		}
	}
	
	private void startNotificationThread() {
		
		notificationThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (keepRunning) {
					try {
						Thread.sleep(1000);
						if (keepRunning)
							showNotification();
					} catch (InterruptedException e) {
						// Logger.d(logTag, "Notification thread interrupted.");
						break;
					}

				}
			}
		}, "NOTIFICATION_THREAD");

		notificationThread.start();
	}
	
	private void initializeAEC() {

		if (solicallAec != null) {
			Logger.d(tag, "AEC already initialized.");
			return;
		}
		
		if (aecEnabled) 
		{
			try 
			{
				solicallAec = new SolicallWrapper();
				solicallAec.init();
			}
			catch (UnsatisfiedLinkError e)
			{
				Logger.e(tag, "Solicall init error: " + e.toString());
				solicallAec = null;
				aecEnabled = false;
			}
			catch (IOException e) 
			{
				Logger.e(tag, "Solicall init exception: " + e.toString());
				solicallAec = null;
				aecEnabled = false;
			}	
		}
	}
	
	private void showNotification() {
		
//		Logger.d(logTag, "Showing notification..");
		Intent myIntent = new Intent(getApplicationContext(), VoIPActivity.class);
		myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, myIntent, 0);

		if (notificationManager == null)
			notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		if (builder == null) 
			builder = new NotificationCompat.Builder(getApplicationContext());

		VoIPClient client = getClient();
		if (client == null)
			return;
		
		int callDuration = getCallDuration();
		String durationString = (callDuration <= 0)? "" : String.format(Locale.getDefault(), " (%02d:%02d)", (callDuration / 60), (callDuration % 60));

		String title = null;
		if (client.getName() == null)
			title = getString(R.string.voip_call_chat);
		else
			title = getString(R.string.voip_call_notification_title, client.getName()); 
		
		if (hostingConference())
			title = getString(R.string.voip_conference_call_notification_title); 
		
		String text = null;
		switch (client.getCallStatus())
		{
			case ON_HOLD:
				text = getString(R.string.voip_on_hold);
				break;

			case OUTGOING_CONNECTING:
			case OUTGOING_RINGING:
				text = getString(R.string.voip_call_summary_outgoing);
				break;

			case INCOMING_CALL:
				text = getString(R.string.voip_call_summary_incoming);
				break;

			case ACTIVE:
			case RECONNECTING:
			case PARTNER_BUSY:
			case ENDED:
				text = getString(R.string.voip_call_notification_text, durationString); 
				break;

			case UNINITIALIZED:
				return;
		default:
			break;
		}

		Notification myNotification = builder
		.setContentTitle(title)
		.setContentText(text)
		.setSmallIcon(HikeNotification.getInstance().returnSmallIcon())
		.setColor(getResources().getColor(R.color.blue_hike_m))
		.setContentIntent(pendingIntent)
		.setOngoing(true)
		.setAutoCancel(true)
		.build();
		
		notificationManager.notify(null, NOTIFICATION_IDENTIFIER, myNotification);
		
//		VoIPUtils.showMemoryUsage(getApplicationContext());
	}

	private VoIPClient getClient() {
		VoIPClient client = null;
		synchronized (clients) {
			if (clients.size() > 0)
				client = (VoIPClient) clients.entrySet().iterator().next().getValue();
		}
		return client;
	}
	
	private VoIPClient getClient(String msisdn) {
		VoIPClient client = null;
		client = clients.get(msisdn);
		return client;
	}

	public void dismissNotification() {
		// Dismiss notification
		Logger.d(tag, "Removing notification..");
		HikeNotification.getInstance().cancelNotification(NOTIFICATION_IDENTIFIER);
	}
	
	private void initAudioManager() {

//		Logger.w(logTag, "Initializing audio manager.");
		
		if (audioManager == null)
			audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		
		saveCurrentAudioSettings();
		initSoundPool();
		setSpeaker(false);
		
		// Check vibrator
		if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT)
			vibratorEnabled = false;
		else
			vibratorEnabled = true;

		// Audio focus
		mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
			
			@Override
			public void onAudioFocusChange(int focusChange) {
				VoIPClient client = getClient();
				if (client == null)
					return;
				
				switch (focusChange) {
				case AudioManager.AUDIOFOCUS_GAIN:
					Logger.w(tag, "AUDIOFOCUS_GAIN");
					break;
				case AudioManager.AUDIOFOCUS_LOSS:
					Logger.w(tag, "AUDIOFOCUS_LOSS");
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
					Logger.d(tag, "AUDIOFOCUS_LOSS_TRANSIENT");
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
					Logger.w(tag, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
					break;
				}
			}
		};
		
		int result = audioManager.requestAudioFocus(mOnAudioFocusChangeListener, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
		if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			Logger.w(tag, "Unable to gain audio focus. result: " + result);
		} else
			Logger.d(tag, "Received audio focus.");
	}
	
	private void releaseAudioManager() {
		if (audioManager != null) {
			audioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
			restoreAudioSettings();
		}
		
		if (soundpool != null) {
			Logger.d(tag, "Releasing soundpool.");
			soundpool.release();
			soundpool = null;
		}
	}

	@SuppressLint("InlinedApi") private void setAudioModeInCall() {
		if (android.os.Build.VERSION.SDK_INT >= 11)
			audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);	
	}
	
	private void saveCurrentAudioSettings() {
		initialAudioMode = audioManager.getMode();
		initialSpeakerMode = audioManager.isSpeakerphoneOn();
	}

	private void restoreAudioSettings() {
		audioManager.setMode(initialAudioMode);
		audioManager.setSpeakerphoneOn(initialSpeakerMode);
		audioManager.stopBluetoothSco();
		audioManager.setBluetoothScoOn(false);	
	}
	
	@SuppressWarnings("deprecation")
	@SuppressLint("InlinedApi") 
	private synchronized boolean initSoundPool() {
		
		if (soundpool != null) {
			Logger.d(tag, "Soundpool already initialized.");
			return true;
		}
		
		if (Utils.isLollipopOrHigher()) {
			soundpool = SoundPoolForLollipop.create();
		} else {
			soundpool = new SoundPool(2, AudioManager.STREAM_VOICE_CALL, 0);
		}

		soundpoolMap = new SparseIntArray(3);
		
		if (soundpool == null || soundpoolMap == null) {
			Logger.w(tag, "Soundpool initialization failed.");
			return false;
		}
		
		soundpoolMap.put(SOUND_ACCEPT, soundpool.load(getApplicationContext(), SOUND_ACCEPT, 1));
		soundpoolMap.put(SOUND_DECLINE, soundpool.load(getApplicationContext(), SOUND_DECLINE, 1));
		soundpoolMap.put(SOUND_INCOMING_RINGTONE, soundpool.load(getApplicationContext(), SOUND_INCOMING_RINGTONE, 1));
		soundpoolMap.put(SOUND_RECONNECTING, soundpool.load(getApplicationContext(), SOUND_RECONNECTING, 1));
		
		return true;
	}
	
	private int playFromSoundPool(int soundId, boolean loop) {
		int streamID = 0;
		if (soundpool == null || soundpoolMap == null) {
			if (!initSoundPool())
				return 0;
		}
		
		if (loop)
			streamID = soundpool.play(soundpoolMap.get(soundId), 1, 1, 0, -1, 1);
		else
			streamID = soundpool.play(soundpoolMap.get(soundId), 1, 1, 0, 0, 1);
		
		return streamID;
	}
	
	private void stopFromSoundPool(int streamID) {
		if (soundpool != null)
			soundpool.stop(streamID);
	}
	
	public void setMessenger(Messenger messenger) {
		this.mMessenger = messenger;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		this.mMessenger = null;
		return super.onUnbind(intent);
	}

	private static void setCallid(int callId) {
		VoIPService.callId = callId;
	}
	
	public static int getCallId() {
		return callId;
	}
	
	public VoIPClient getPartnerClient() {
		return getClient();
	}
	
	public ConnectionMethods getConnectionMethod() {
		return getClient().getPreferredConnectionMethod();
	}
	
	/**
	 * Terminate the service. 
	 */
	synchronized public void stop() {

		Logger.d(tag, "Stopping service..");
		keepRunning = false;

		if (!TextUtils.isEmpty(groupChatMsisdn) && hostingConference()) {
			// We were hosting a conference which has now ended. 
			// Put a call summary in the group chat thread. 
			VoIPClient client = new VoIPClient(null, null);
			client.setPhoneNumber(groupChatMsisdn);
			int duration = getCallDuration();
			if (duration < 0)
				duration = 0;
			VoIPUtils.addMessageToChatThread(getApplicationContext(), client, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_CALL_SUMMARY, duration, -1, true);
			groupChatMsisdn = null;
		}

		// Call Rating, analytics etc. 
		VoIPClient clientPartner = getClient();
		Bundle bundle = new Bundle();
		bundle.putInt(VoIPConstants.CALL_ID, getCallId());
		bundle.putInt(VoIPConstants.CALL_NETWORK_TYPE, VoIPUtils.getConnectionClass(getApplicationContext()).ordinal());
		bundle.putInt(VoIPConstants.CALL_DURATION, getCallDuration());
		if (clientPartner != null) {
			bundle.putInt(VoIPConstants.IS_CALL_INITIATOR, clientPartner.isInitiator() ? 0 : 1);
			bundle.putString(VoIPConstants.PARTNER_MSISDN, clientPartner.getPhoneNumber());
			bundle.putBoolean(VoIPConstants.IS_CONFERENCE, hostingConference() || clientPartner.isHostingConference);
		}
		
		sendMessageToActivity(VoIPConstants.MSG_SHUTDOWN_ACTIVITY, bundle);

		synchronized (clients) {
			for (VoIPClient client : clients.values())
				client.close();
			clients.clear();
		}
		
		// Reset variables
		setCallid(0);
		isRingingOutgoing = false;
		isRingingIncoming = false;
		
		// Terminate threads
		if (notificationThread!=null)
			notificationThread.interrupt();

		if (connectionTimeoutThread != null)
			connectionTimeoutThread.interrupt();

		if (playbackThread != null)
			playbackThread.interrupt();
		
		if (recordingThread != null)
			recordingThread.interrupt();

		if (processRecordedSamplesThread != null)
			processRecordedSamplesThread.interrupt();
		
		if (bufferSendingThread != null)
			bufferSendingThread.interrupt();
		
		if (conferenceBroadcastThread != null)
			conferenceBroadcastThread.interrupt();
		
		stopRingtone();
		stopFromSoundPool(ringtoneStreamID);
		releaseAudioManager();
		
		if (chronometer != null) {
			chronometer.stop();
			chronometer = null;
		}
		
		if (solicallAec != null) {
			solicallAec.destroy();
			solicallAec = null;
		}
		
		if (broadcastSocket != null)
			broadcastSocket.close();

		// Empty the queues
		conferenceBroadcastPackets.clear();
		recordedSamples.clear();
		processedRecordedSamples.clear();
		playbackBuffersQueue.clear();
		recordBuffer.clear();
		
		releaseWakeLock();
		stopSelf();
	}
	
	/**
	 * Change your mute status. 
	 * @param mute
	 * @return true, if mute was successfully changed. 
	 */
	public boolean setMute(boolean mute)
	{
		if (forceMute == true && mute == false) {
			Logger.w(tag, "Cannot unmute since we have been forced muted.");
			return false;
		}
		
		this.mute = mute;
		
		// Send mute status to the other party
		final VoIPClient client = getClient();
		if (client == null || hostingConference())
			return true;
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				VoIPDataPacket dp = null;
				if (VoIPService.this.mute == true)
					dp = new VoIPDataPacket(PacketType.MUTE_ON);
				else
					dp = new VoIPDataPacket(PacketType.MUTE_OFF);
				client.sendPacket(dp, true);
			}
		}).start();
		
		return true;
	}
	
	public void setHostForceMute(final boolean mute) {
		hostForceMute = mute;
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				PacketType type = mute ? PacketType.FORCE_MUTE_ON : PacketType.FORCE_MUTE_OFF;
				VoIPDataPacket dp = new VoIPDataPacket(type);
				synchronized (clients) {
					for (VoIPClient client : clients.values()) {
						client.sendPacket(dp, true);
					}
				}
			}
		}).start();
	}

	public boolean getMute()
	{
		return mute;
	}
	
	public boolean getHostForceMute() {
		return hostForceMute;
	}
	
	private void sendMessageToActivity(int message) {
		sendMessageToActivity(message, null);
	}

	private void sendMessageToActivity(int message, Bundle bundle) {
		Message msg = Message.obtain();
		msg.what = message;
		msg.setData(bundle);
		try {
			if (mMessenger != null)
				mMessenger.send(msg);
		} catch (RemoteException e) {
			Logger.e(tag, "Messenger RemoteException: " + e.toString());
		}
	}

	public void rejectIncomingCall() {
		
		final VoIPClient client = getClient();
		if (client == null) return;
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.CALL_DECLINED);
				client.sendPacket(dp, true);
				stop();
			}
		},"REJECT_INCOMING_CALL_THREAD").start();
		
		// Here we don't show a missed call notification, but add the message to the chat thread
		client.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_REJECT);
	}
	
	public void acceptIncomingCall() {
		
		final VoIPClient client = getClient();
		if (client == null) return;
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.START_VOICE);
				client.sendPacket(dp, true);
			}
		}, "ACCEPT_INCOMING_CALL_THREAD").start();

		startBluetooth();
		startRecordingAndPlayback(client.getPhoneNumber());
		client.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_ACCEPT);
	}
	
	private synchronized void startRecordingAndPlayback(String msisdn) {

		final VoIPClient client = getClient(msisdn);
		
		if (client == null)
			return;
		
		if (client.audioStarted == true) {
			Logger.d(tag, "Audio already started.");
			return;
		}

		client.audioStarted = true;

		if(client.getPreferredConnectionMethod() == ConnectionMethods.RELAY) 
			client.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_RELAY);

		playFromSoundPool(SOUND_ACCEPT, false);
		client.startChrono();
		client.setCallStatus(VoIPConstants.CallStatus.ACTIVE);
		stopRingtone();
		stopFromSoundPool(ringtoneStreamID);
		isRingingOutgoing = isRingingIncoming = false;
		playFromSoundPool(SOUND_ACCEPT, false);
		sendMessageToActivity(VoIPConstants.MSG_AUDIO_START);

		if (recordingAndPlaybackRunning == false) {
			recordingAndPlaybackRunning = true;
			Logger.d(tag, "Starting audio record / playback.");
			startRecording();
			startPlayBack();
			startChrono();
		} else {
			Logger.d(tag, "Skipping startRecording() and startPlayBack()");
		}
		
		if (hostingConference()) {
			sendMessageToActivity(VoIPConstants.MSG_UPDATE_SPEAKING);
			sendClientsListToAllClients();
		}
	}
	
	private void startRecording() {
		
		if (recordingThread != null)
			recordingThread.interrupt();
		
		recordingThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					// Sleep for a little bit in case the AudioRecord is being initialized
					// again. Doing it immediately will cause the AudioRecord to fail. 
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					return;
				}
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

				AudioRecord recorder = null;
				
				int audioSource = VoIPUtils.getAudioSource(speaker);

				// Start recording audio from the mic
				// Try different sample rates
				for (int rate : new int[] {VoIPConstants.AUDIO_SAMPLE_RATE, 44100, 24000, 22050}) {
					try
					{
						recordingSampleRate = rate;
						
						minBufSizeRecording = AudioRecord.getMinBufferSize(recordingSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
						if (minBufSizeRecording < 0) {
							Logger.w(tag, "Sample rate " + recordingSampleRate + " is not valid.");
							continue;
						}
						
						if (aecEnabled) {
							// For the Solicall AEC library to work, we must record data in chunks
							// which is a multiple of the library's supported frame size (20ms).
							Logger.d(tag, "Old minBufSizeRecording: " + minBufSizeRecording + " at sample rate: " + recordingSampleRate);
							if (minBufSizeRecording < SolicallWrapper.SOLICALL_FRAME_SIZE * 2) {
								minBufSizeRecording = SolicallWrapper.SOLICALL_FRAME_SIZE * 2;
							} else {
								minBufSizeRecording = ((minBufSizeRecording + (SolicallWrapper.SOLICALL_FRAME_SIZE * 2) - 1) / (SolicallWrapper.SOLICALL_FRAME_SIZE * 2)) * SolicallWrapper.SOLICALL_FRAME_SIZE * 2;
							}
							Logger.d(tag, "New minBufSizeRecording: " + minBufSizeRecording);
						}
						
						recorder = new AudioRecord(audioSource, recordingSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufSizeRecording);
						if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
							recorder.startRecording();
							break;
						}
						else {
							recorder.release();
						}
					}
					catch(IllegalArgumentException e)
					{
						Logger.e(tag, "AudioRecord init failed (" + recordingSampleRate + "): " + e.toString());
					}
					catch (IllegalStateException e)
					{
						Logger.e(tag, "Recorder exception (" + recordingSampleRate + "): " + e.toString());
					}
					
				}
				
				if (recorder == null || recorder.getState() != AudioRecord.STATE_INITIALIZED) {
					Logger.e(tag, "AudioRecord initialization failed. Mic may not work.");
					sendMessageToActivity(VoIPConstants.MSG_AUDIORECORD_FAILURE);
					return;
				}
				
				// Attach AGC
				if (Utils.isJellybeanOrHigher()) {
					if (AutomaticGainControl.isAvailable()) {
						AutomaticGainControl agc = AutomaticGainControl.create(recorder.getAudioSessionId());
						if (agc != null) {
							Logger.w(VoIPConstants.TAG, "Initial AGC status: " + agc.getEnabled());
							agc.setEnabled(true);
						}
					} else
						Logger.w(tag, "AGC not available.");
				}
				
				// Start processing recorded data
				byte[] recordedData = new byte[minBufSizeRecording];
				int retVal;
				while (keepRunning == true) {
					retVal = recorder.read(recordedData, 0, recordedData.length);
					if (retVal != recordedData.length) {
						Logger.w(tag, "Unexpected recorded data length. Expected: " + recordedData.length + ", Recorded: " + retVal);
						continue;
					}
					
					if (mute == true)
						continue;
					
					// Resample
					byte[] output = null;
					if (resamplerEnabled && recordingSampleRate != VoIPConstants.AUDIO_SAMPLE_RATE) {
						// We need to resample the mic signal
						output = resampler.reSample(recordedData, 16, recordingSampleRate, VoIPConstants.AUDIO_SAMPLE_RATE);
						// Logger.d(logTag, "Resampled from: " + recordedData.length + " to: " + output.length);
					} else
						output = recordedData;

					// Break input audio into smaller chunks for Solicall AEC
	            	int index = 0;
	            	int newSize = 0;
                	while (index < retVal) {
                		if (retVal - index < SolicallWrapper.SOLICALL_FRAME_SIZE * 2)
                			newSize = retVal - index;
                		else
                			newSize = SolicallWrapper.SOLICALL_FRAME_SIZE * 2;

                		byte[] data = new byte[newSize];
                		System.arraycopy(output, index, data, 0, newSize);
                		index += newSize;

	                	// Add it to the samples to encode queue
						VoIPDataPacket dp = new VoIPDataPacket(VoIPDataPacket.PacketType.AUDIO_PACKET);
	                	dp.write(data);
	                	try {
		                	recordedSamples.add(dp);
	                	} catch (IllegalStateException e) {
	                		// Recorded samples queue is full
	                	}
                	}
					index = 0;
					
					if (Thread.interrupted()) {
						break;
					}
				}
				
				// Stop recording
				if (recorder!= null && recorder.getState() == AudioRecord.STATE_INITIALIZED)
					recorder.stop();
				
				recorder.release();
			}
		}, "RECORDING_THREAD");
		
		recordingThread.start();
		processRecordedSamples();
	}
	
	private void processRecordedSamples() {

		if (processRecordedSamplesThread != null)
			processRecordedSamplesThread.interrupt();
		
		processRecordedSamplesThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				
				while (keepRunning == true) {
					VoIPDataPacket dpRaw;
					try {
						dpRaw = recordedSamples.take();
					} catch (InterruptedException e) {
						break;
					}

					VoIPClient client = getClient();
					if (client == null)
						continue;

					// AEC
					if (solicallAec != null && aecEnabled && aecMicSignal && aecSpeakerSignal) {
						int ret = solicallAec.processMic(dpRaw.getData());
						if (ret == 0) {
							if (speechDetected == true)
								client.updateLocalSpeech(false);
							speechDetected = false;
						}
						else {
							if (speechDetected == false)
								client.updateLocalSpeech(true);
							speechDetected = true;
						}
					} else
						aecMicSignal = true;
					
					recordBuffer.write(dpRaw.getData());

					// Pass the recorded samples to the client objects
					// so they can be compressed and sent
					while (recordBuffer.getAvailable() >= OpusWrapper.OPUS_FRAME_SIZE * 2) {
						byte[] pcmData = new byte[OpusWrapper.OPUS_FRAME_SIZE * 2];
						recordBuffer.read(pcmData);
						VoIPDataPacket dp = new VoIPDataPacket(PacketType.AUDIO_PACKET);
						dp.setData(pcmData);
						dp.setVoice(speechDetected);
						if (processedRecordedSamples.size() < VoIPConstants.MAX_SAMPLES_BUFFER)
							processedRecordedSamples.add(dp);
						else
							Logger.w(tag, "Recorded buffers queue is full.");
					}
				}
			}
		}, "PROCESS_RECORDED_SAMPLES_THREAD");
		
		processRecordedSamplesThread.start();
	}
	
	private void startPlayBack() {
		
		playbackThread = new Thread(new Runnable() {
			
			@Override
			public void run() {

				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				setAudioModeInCall();
				int index = 0, size = 0;

				if (resamplerEnabled) 
					playbackSampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_VOICE_CALL);
				else
					playbackSampleRate = VoIPConstants.AUDIO_SAMPLE_RATE;
				
				minBufSizePlayback = AudioTrack.getMinBufferSize(playbackSampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
				Logger.d(tag, "AUDIOTRACK - minBufSizePlayback: " + minBufSizePlayback + ", playbackSampleRate: " + playbackSampleRate);
			
				try {
					audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, playbackSampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufSizePlayback, AudioTrack.MODE_STREAM);
				} catch (IllegalArgumentException e) {
					Logger.w(tag, "Unable to initialize AudioTrack: " + e.toString());
					getClient().hangUp();
					return;
				}
				
				try {
					audioTrack.play();
				} catch (IllegalStateException e) {
					Logger.e(tag, "Audiotrack error: " + e.toString());
					getClient().hangUp();
				}
				
				byte[] solicallSpeakerBuffer = new byte[SolicallWrapper.SOLICALL_FRAME_SIZE * 2];
				while (keepRunning == true) {
					VoIPDataPacket dp;
					try {
						dp = playbackBuffersQueue.take();
						if (dp != null) {

							// AEC
							if (solicallAec != null && aecEnabled && aecSpeakerSignal && aecMicSignal) {
								index = 0;
								while (dp.getData() != null && index < dp.getData().length) {
									size = Math.min(SolicallWrapper.SOLICALL_FRAME_SIZE * 2, dp.getLength() - index);
									System.arraycopy(dp.getData(), index, solicallSpeakerBuffer, 0, size);
									solicallAec.processSpeaker(solicallSpeakerBuffer);
									index += size; 
								}
							} else
								aecSpeakerSignal = true;

							// Resample
							byte[] output = dp.getData();
							if (output == null)
								continue;
							
							if (resamplerEnabled && playbackSampleRate != VoIPConstants.AUDIO_SAMPLE_RATE) {
								// We need to resample the output signal
								// Logger.d(logTag, "Resampling.");
								output = resampler.reSample(dp.getData(), 16, VoIPConstants.AUDIO_SAMPLE_RATE, playbackSampleRate);
							} 
							
							// For streaming mode, we must write data in chunks <= buffer size
							index = 0;
							while (index < output.length) {
								size = Math.min(minBufSizePlayback, output.length - index);
								audioTrack.write(output, index, size);
								index += size; 
							}
						} 
					} catch (InterruptedException e) {
						break;
					}
				}
				
				if (audioTrack != null) {
					try {
						audioTrack.pause();
						audioTrack.flush();
						audioTrack.release();
						audioTrack = null;
					} catch (IllegalStateException e) {
						Logger.w(tag, "Audiotrack IllegalStateException: " + e.toString());
					}
				}
				
			}
		}, "PLAY_BACK_THREAD");
		
		playbackThread.start();
		startAudioProcessor();
	}
	
	private void startAudioProcessor() {
		
		// This is how often we feed PCM samples to the speaker. 
		// Should be equal to 60ms for a frame size of 2880. (2880 / 48000)
		int sleepTime = OpusWrapper.OPUS_FRAME_SIZE * 1000 / VoIPConstants.AUDIO_SAMPLE_RATE;
		
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

		if (scheduledExecutorService != null) {
			Logger.w(tag, "Feeder is already running.");
			return;
		} else {
			scheduledExecutorService = Executors.newScheduledThreadPool(1);
		}
		
		byte[] silence = new byte[OpusWrapper.OPUS_FRAME_SIZE * 2];
		final VoIPDataPacket silentPacket = new VoIPDataPacket(PacketType.AUDIO_PACKET);
		silentPacket.setData(silence);
		final HashMap<String, byte[]> clientSample = new HashMap<String, byte[]>();

		
		scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
			
			@Override
			public void run() {
				try {
					android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

					if (keepRunning) {

						// Retrieve decoded samples from all clients and combine into one for playback
						VoIPDataPacket finalDecodedSample = null;
						synchronized (clients) {
							for (VoIPClient client : clients.values()) {
								VoIPDataPacket dp = client.getDecodedBuffer();
								if (dp != null) {
									if (hostingConference())
										clientSample.put(client.getPhoneNumber(), dp.getData());

									if (finalDecodedSample == null)
										finalDecodedSample = dp;
									else {
										// We have to combine samples
										finalDecodedSample.setData(VoIPUtils.addPCMSamples(finalDecodedSample.getData(), dp.getData()));
									}
								} 
							}
						}

						// Quality tracking, and buffer underrun protection
						try {
							if (finalDecodedSample == null) {
								// Logger.d(logTag, "Decoded samples underrun. Adding silence.");
								finalDecodedSample = silentPacket;
							} 

							// Add to our decoded samples queue for playback
							if (!hold) {
								if (playbackBuffersQueue.size() < VoIPConstants.MAX_SAMPLES_BUFFER)
									playbackBuffersQueue.put(finalDecodedSample);
								else
									Logger.w(tag, "Playback buffers queue full.");
							}

						} catch (InterruptedException e) {
							Logger.e(tag, "InterruptedException while adding playback sample: " + e.toString());
						}

						// If we are in conference, then add our own recorded signal as well.
						// Broadcast this signal to all clients, except for the ones that are speaking.
						// If someone is speaking, we need to send them a custom stream without their voice signal.
						
						if (hostingConference()) {
							VoIPDataPacket dp = processedRecordedSamples.poll();
							byte[] conferencePCM = null;
							if (dp != null) {
								conferencePCM = VoIPUtils.addPCMSamples(finalDecodedSample.getData(), dp.getData());
								dp.setData(conferencePCM);
							}
							else {
								dp = new VoIPDataPacket(PacketType.AUDIO_PACKET);
								conferencePCM = finalDecodedSample.getData();	// Host is probably on mute
								dp.setData(conferencePCM);
							}

							// This is the broadcast
							conferenceBroadcastPackets.add(dp);

							synchronized (clients) {
								for (VoIPClient client : clients.values()) {
									if (!client.isSpeaking() || !client.connected)
										continue;

									// Custom streams
									VoIPDataPacket clientDp = new VoIPDataPacket();
									byte[] origPCM = clientSample.get(client.getPhoneNumber());
									byte[] newPCM = null;
									if (origPCM == null) {
										newPCM = conferencePCM;
									} else {
										newPCM = VoIPUtils.subtractPCMSamples(conferencePCM, origPCM);
									}
									clientDp.setData(newPCM);
									clientDp.setVoice(true);
									client.addSampleToEncode(clientDp); 
								}
							}
						} else {
							// We are in a one-to-one call, 
							// so just send our recorded stream to the other client.
							VoIPDataPacket dp = processedRecordedSamples.poll();
							VoIPClient client = getClient();
							if (dp != null && client != null)
								client.addSampleToEncode(dp);
						}

						if (hostingConference())
							clientSample.clear();

					} else {
						Logger.d(tag, "Shutting down decoded samples poller.");
						scheduledFuture.cancel(true);
						scheduledExecutorService.shutdownNow();
					}
				} catch (Exception e) {
					Logger.e(tag, "Exception thrown: " + e);
				}
			}
		}, 0, sleepTime, TimeUnit.MILLISECONDS);
	}
	
	private void startConferenceBroadcast() {
		
		if (conferenceBroadcastThread != null)
			// Already running. 
			return;
		
		conferenceBroadcastThread = new Thread(new Runnable() {

			@Override
			public void run() {

				// Wait till the broadcast frames start coming in
				OpusWrapper opusWrapper = null;
				try {
					VoIPDataPacket broadcastPacket = conferenceBroadcastPackets.take();
					Logger.w(tag, "Starting conference broadcast.");

					opusWrapper = new OpusWrapper();
					opusWrapper.getEncoder(VoIPConstants.AUDIO_SAMPLE_RATE, 1, VoIPConstants.BITRATE_CONFERENCE);
					broadcastSocket = new DatagramSocket();
					InetAddress relayAddress = InetAddress.getByName(getClient().getRelayAddress());
					int relayPort = getClient().getRelayPort();

					byte[] compressedData = new byte[OpusWrapper.OPUS_FRAME_SIZE * 10];
					int compressedDataLength = 0;

					while (keepRunning) {
						// If it's an audio packet, then compress it
						if (broadcastPacket.getType() == PacketType.AUDIO_PACKET) {
							// Compress the audio frame
							if ((compressedDataLength = opusWrapper.encode(broadcastPacket.getData(), compressedData)) > 0) {
								byte[] trimmedCompressedData = new byte[compressedDataLength];
								System.arraycopy(compressedData, 0, trimmedCompressedData, 0, compressedDataLength);
								broadcastPacket.write(trimmedCompressedData);
								broadcastPacket.setVoice(true);

							} else {
								Logger.w(tag, "Conference broadcast compression error.");
							}
						} 
							
						// Create a broadcast list
						synchronized (clients) {
							for (VoIPClient client : clients.values()) {
								
								if (!client.connected)
									continue;
								
								if (client.isSpeaking() && broadcastPacket.getType() == PacketType.AUDIO_PACKET)
									continue;

								BroadcastListItem item = broadcastPacket.new BroadcastListItem();
								item.setIp(client.getExternalIPAddress());
								item.setPort(client.getExternalPort());
								broadcastPacket.addToBroadcastList(item);
							}
						}
						
						// Send the packet
						byte[] packetData = VoIPUtils.getUDPDataFromPacket(broadcastPacket);
						DatagramPacket packet = new DatagramPacket(packetData, packetData.length, 
								relayAddress, relayPort);
						broadcastSocket.send(packet);
						
						// Wait for next packet
						broadcastPacket = conferenceBroadcastPackets.take();
					}
				} catch (InterruptedException e) {
					Logger.d(tag, "Conference broadcast thread interrupted.");
				} catch (IOException e) {
					Logger.e(tag, "Codec IOException: " + e.toString());
				} catch (Exception e) {
					Logger.e(tag, "Codec Exception: " + e.toString());
				} finally {
					if (opusWrapper != null)
						opusWrapper.destroy();
				}

			}
		}, "CONFERENCE_BROADCAST_THREAD");
		conferenceBroadcastThread.start();
	}

	synchronized public void setHold(boolean newHold) {
		
		final VoIPClient client = getClient();

		if (this.hold == newHold || client == null)
			return;
		
		/**
		 * If we get a missed cellular call WHILE we're already receiving a voip call, 
		 * the voip call will erroneously unhold itself. 
		 */
		if (!recordingAndPlaybackRunning && newHold)
			return;
		
		Logger.d(tag, "Changing hold to: " + newHold);
		this.hold = newHold;
		
		if (newHold == true) {
			if (recordingThread != null)
				recordingThread.interrupt();
			if (playbackThread != null)
				playbackThread.interrupt();
		} else {
			// Coming off hold
			startRecording();
			startPlayBack();
		}

		client.setCallStatus(!hold && !client.remoteHold ? VoIPConstants.CallStatus.ACTIVE : VoIPConstants.CallStatus.ON_HOLD);
		sendMessageToActivity(VoIPConstants.MSG_UPDATE_CALL_BUTTONS);
		
		// Send hold status to partner
		sendHoldStatus();
	}	

	public boolean getHold()
	{
		return hold;
	}
	
	private void sendHoldStatus() {
		if (hostingConference())
			return;
		
		final VoIPClient client = getClient();
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				VoIPDataPacket dp = null;
				if (hold == true)
					dp = new VoIPDataPacket(PacketType.HOLD_ON);
				else
					dp = new VoIPDataPacket(PacketType.HOLD_OFF);
				client.sendPacket(dp, true);
			}
		}).start();
	}
	
	public String getSessionKeyHash() {
		String hash = null;
		VoIPClient client = getClient();
		if (client != null && client.encryptor != null)
			hash = client.encryptor.getSessionMD5();
		return hash;
	}

	public void setSpeaker(boolean speaker)
	{
		this.speaker = speaker;
		if (audioManager != null)
		{
			audioManager.setSpeakerphoneOn(speaker);
		}
		
		// If we have swiched off the speaker and a bluetooth headset is connected
		// then revert the voice to the headset. 
		if (bluetoothHelper != null && bluetoothHelper.isOnHeadsetSco()) {
			if (!this.speaker) {
				audioManager.setBluetoothScoOn(true);
			}
		}
	}

	public boolean getSpeaker()
	{
		return speaker;
	}

	/**
	 * Call this function when you are making an outgoing call and
	 * the receiving phone has started ringing.
	 */
	private void playOutgoingCallRingtone() {
		VoIPClient client = getClient();
		synchronized (this) {
			
			if (client == null || client.reconnecting || client.audioStarted)
				return;
			
			if (isRingingOutgoing == true) {
				Logger.d(tag, "Outgoing ringer is already ringing.");
				return;
			} else isRingingOutgoing = true;

			Logger.d(tag, "Playing outgoing call ringer.");
			client.setCallStatus(VoIPConstants.CallStatus.OUTGOING_RINGING);
			client.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_ESTABLISHED);
			setAudioModeInCall();
			ringtoneStreamID = playFromSoundPool(SOUND_INCOMING_RINGTONE, true);
		}
	}
	
	@SuppressWarnings("deprecation")
	private void playIncomingCallRingtone() {

		VoIPClient client = getClient();
		if (client.reconnecting || client.audioStarted || keepRunning == false)
			return;

		// Close all system dialogs and/or the notification bar
		Intent closeDiaogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		getApplicationContext().sendBroadcast(closeDiaogs);
		
		synchronized (this) {
			
			if (isRingingIncoming == true)
				return;
			else isRingingIncoming = true;

			// Show notification
			startNotificationThread();

			// Show activity
			Intent intent = IntentFactory.getVoipIncomingCallIntent(VoIPService.this);
			startActivity(intent);

			// Ringer
			Logger.d(tag, "Playing ringtone.");
			Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
			
			if (ringtone == null) {
				ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
				
				if (ringtone == null) {
					Logger.e(tag, "Unable to get ringtone object.");
					return;
				}
				
				if (Utils.isLollipopOrHigher()) {
					RingtoneForLollipop.create(ringtone);
				} else
					ringtone.setStreamType(AudioManager.STREAM_RING);
				ringtone.play();		
			}
			
			// Vibrator
			if (vibratorEnabled == true) {
				if (vibrator == null) {
					vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
					long[] pattern = {0, 500, 1000};
					vibrator.vibrate(pattern, 0);
				}
			}
		}
	}
	
	public void stopRingtone()
	{
		synchronized (this) {
			// Stop ringtone if playing
			try {
				if (ringtone != null)
				{
					ringtone.stop();
					ringtone = null;
				}
			} catch (IllegalStateException e) {
				Logger.w(tag, "stopRingtone() IllegalStateException: " + e.toString());
			}

			// stop vibrating
			if (vibrator != null) {
				vibrator.cancel();
				vibrator = null;
			}
			
			isRingingIncoming = false;
		}
	}

	public void startReconnectBeeps() {
		if (reconnectingBeeps || hostingConference() || inCellularCall)
			return;
		
		reconnectingBeeps = true;
		
		reconnectingBeepsThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				int streamId = playFromSoundPool(SOUND_RECONNECTING, true);
				while (keepRunning) {
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
						stopFromSoundPool(streamId);
						break;
					}
				}
				reconnectingBeeps = false;
			}
		}, "RECONNECT_THREAD");
		reconnectingBeepsThread.start();
	}

	public void sendAnalyticsEvent(String ek)
	{
		sendAnalyticsEvent(ek, -1);
	}

	public void sendAnalyticsEvent(String ek, int value) {
		VoIPClient client = getClient();
		if (client != null)
			client.sendAnalyticsEvent(ek, value);
	}
	
	public boolean isAudioRunning() {
		return recordingAndPlaybackRunning;
	}

	public void setCallStatus(VoIPConstants.CallStatus status)
	{
		VoIPClient client = getClient();
		
		if (client != null)
			getClient().setCallStatus(status);
	}
	
	public VoIPConstants.CallStatus getCallStatus()
	{
		VoIPClient client = getClient();
		
		if (hostingConference() && recordingAndPlaybackRunning)
			return CallStatus.ACTIVE;
		
		if (client != null)
			return client.getCallStatus();
		else
			return CallStatus.UNINITIALIZED;
	}

	public void hangUp() {
		for (VoIPClient client : clients.values())
			client.hangUp();
		stop();
	}
	
	public synchronized int getCallDuration() {
		if (hostingConference()) {
			int seconds;
			if (chronometer != null) {
				seconds = (int) ((SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000);
			} else {
				seconds = -1;
			}
			return seconds;
		} else {
			VoIPClient client = getClient();
			if (client != null)
				return client.getCallDuration();
			else
				return 0;
		}
	}
	
	public boolean hostingConference() {
		return hostingConference;
	}
	
	public boolean toggleConferencing() {
		conferencingEnabled = !conferencingEnabled;
		return conferencingEnabled;
	}

	public int getClientCount() {
		int num = 1;

		if (hostingConference())
			num = clients.size() + 1;
		else {
			VoIPClient client = getClient();
			if (client != null && client.clientMsisdns != null)
				num = client.clientMsisdns.size() + 1;
		}
		
		return num;
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<VoIPClient> getConferenceClients() {
		if (hostingConference())
			return new ArrayList<VoIPClient>(clients.values());
		else {
			return (ArrayList<VoIPClient>) getClient().clientMsisdns.clone();
		}
	}
	
	private void addToClients(VoIPClient client) {

		synchronized (clients) {
			clients.put(client.getPhoneNumber(), client);
			if (clients.size() > 1) {
				hostingConference = true;
			}
		}
		sendClientsListToAllClients();
	}

	private void removeFromClients(String msisdn) {
		
		synchronized (clients) {
			VoIPClient client = getClient(msisdn);
			if (client != null) {
				client.close();
				clients.remove(msisdn);
			}
		}
		sendClientsListToAllClients();
	}
	
	/**
	 * Sends a JSON of all connected clients, 
	 * to all connected clients.  
	 */
	private void sendClientsListToAllClients() {
		
		if (!hostingConference())
			return;
			
		if (clientListHandler != null && clientListRunnable != null)
			clientListHandler.removeCallbacks(clientListRunnable);
		
		clientListRunnable = new Runnable() {
			
			@Override
			public void run() {
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						sendMessageToActivity(VoIPConstants.MSG_UPDATE_CONTACT_DETAILS);
						synchronized (clients) {

							try {
								JSONArray clientsJson = new JSONArray();
								
								for (VoIPClient client : clients.values()) {
									if (!client.connected) continue;
									
									JSONObject clientJson = new JSONObject();
									clientJson.put(VoIPConstants.Extras.MSISDN, client.getPhoneNumber());
									clientJson.put(VoIPConstants.Extras.STATUS, client.getCallStatus().ordinal());
									clientJson.put(VoIPConstants.Extras.SPEAKING, client.isSpeaking());
									clientJson.put(VoIPConstants.Extras.RINGING, client.isRinging());
									clientsJson.put(clientJson);
								}
								
								// Add our own client
								JSONObject clientJson = new JSONObject();
								clientJson.put(VoIPConstants.Extras.MSISDN, Utils.getUserContactInfo(getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE)).getMsisdn());
								clientJson.put(VoIPConstants.Extras.STATUS, CallStatus.ACTIVE.ordinal());
								clientJson.put(VoIPConstants.Extras.SPEAKING, speechDetected && !mute);
								clientsJson.put(clientJson);

								// Encapsulate the array in another JSON object
								JSONObject json = new JSONObject();
								json.put(VoIPConstants.Extras.VOIP_CLIENTS, clientsJson);
								
								// Send it
								VoIPDataPacket dp = new VoIPDataPacket(PacketType.CLIENTS_LIST_JSON);
								dp.setData(json.toString().getBytes("UTF-8"));
								
								conferenceBroadcastPackets.add(dp);	
								Logger.d(tag, "Sending clients list.");
								
							} catch (JSONException e) {
								Logger.w(tag, "JSONException: " + e.toString());
							} catch (UnsupportedEncodingException e) {
								Logger.e(tag, "UnsupportedEncodingException in sendClientsListToAllClients(): " + e.toString());
							}
						}
					}
				}, "CLIENT_LIST_THREAD").start();
			}
		};
		
		clientListHandler = new Handler();
		clientListHandler.postDelayed(clientListRunnable, VoIPConstants.CONFERENCE_CLIENTS_LIST_BROADCAST_REPEAT);
	}

	/**
	 * Used for detecting cellular calls while in a VoIP call. 
	 * Behaviour is to put the VoIP call on hold when a cellular call comes in, 
	 * and unhold the call when the cellular call is terminated.
	 */
	private void registerBroadcastReceivers() {
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.intent.action.PHONE_STATE");

		phoneStateReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
				if (TelephonyManager.EXTRA_STATE_RINGING.equals(state) ||
						TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
					// We have an incoming or outgoing call
					Logger.w(tag, "Cellular call detected.");
					sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_NATIVE_CALL_INTERRUPT);
					if (isAudioRunning()) {
						inCellularCall = true;
						setHold(true);
					}
					else
						hangUp();
				}

				if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
					// Coming off a call
					Logger.w(tag, "Call over.");
					inCellularCall = false;
					setHold(false);
				}
				
			}
		};

		registerReceiver(phoneStateReceiver, filter);

		// Catch power button
		IntentFilter powerButtonFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		powerButtonReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				Logger.d(tag, "Stopping ringtone.");
				stopRingtone();
			}
		}; 
		registerReceiver(powerButtonReceiver, powerButtonFilter);
	}
	
	private void unregisterBroadcastReceivers() {
		if (phoneStateReceiver != null)
			unregisterReceiver(phoneStateReceiver);
		if (powerButtonReceiver != null)
			unregisterReceiver(powerButtonReceiver);
	}
	
	private void startBluetooth() {
		isBluetoothEnabled = VoIPUtils.isBluetoothEnabled(getApplicationContext());
		if (isBluetoothEnabled && bluetoothHelper == null) {
			bluetoothHelper = new BluetoothHelper(getApplicationContext());
			bluetoothHelper.start();
		}
	}

	public void startChrono() {

		try {
			if (chronometer == null) {
				Logger.d(tag, "Starting chrono.");
				chronometer = new Chronometer(getApplicationContext());
				chronometer.setBase(SystemClock.elapsedRealtime());
				chronometer.start();
			}
		} catch (Exception e) {
			Logger.w(tag, "Chrono exception: " + e.toString());
		}
	}

	public void processErrorIntent(String action, String msisdn) {
		Logger.w(tag, msisdn + " returned an error message: " + action);
	}

	@Override
	public void onEventReceived(String type, Object object) 
	{
		if(HikePubSub.REJECT_INCOMING_CALL.equals(type))
			rejectIncomingCall();
	}
}

