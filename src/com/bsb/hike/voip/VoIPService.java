package com.bsb.hike.voip;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.widget.Chronometer;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.voip.VoIPClient.ConnectionMethods;
import com.bsb.hike.voip.VoIPConstants.CallQuality;
import com.bsb.hike.voip.VoIPDataPacket.PacketType;
import com.bsb.hike.voip.VoIPEncryptor.EncryptionStage;
import com.bsb.hike.voip.VoIPUtils.CallSource;
import com.bsb.hike.voip.VoIPUtils.ConnectionClass;
import com.bsb.hike.voip.protobuf.VoIPSerializer;
import com.bsb.hike.voip.view.VoIPActivity;
import com.musicg.dsp.Resampler;

public class VoIPService extends Service implements Listener
{
	
	private final IBinder myBinder = new LocalBinder();
	private final int AUDIO_SAMPLE_RATE = 48000; 
	private final int PACKET_TRACKING_SIZE = 128;
	private final int HEARTBEAT_INTERVAL = 1000;
	private final int HEARTBEAT_TIMEOUT = 5000;
	private final int HEARTBEAT_HARD_TIMEOUT = 60000;
	private final int MAX_SAMPLES_BUFFER = 3;
	private static final int NOTIFICATION_IDENTIFIER = 10;

	private int localBitrate = VoIPConstants.BITRATE_WIFI, remoteBitrate = 0;
	private boolean cryptoEnabled = true;
	private Messenger mMessenger;
	private static boolean connected = false;
	private boolean reconnecting = false;
	private int currentPacketNumber = 0;
	private int previousHighestRemotePacketNumber = 0;
	private volatile boolean keepRunning = true;
	private DatagramSocket socket = null;
	private VoIPClient clientPartner = null;
	private BitSet packetTrackingBits = new BitSet(PACKET_TRACKING_SIZE);
	private long lastHeartbeat;
	private int totalBytesSent = 0, totalBytesReceived = 0, rawVoiceSent = 0;
	private VoIPEncryptor encryptor = new VoIPEncryptor();
	private VoIPEncryptor.EncryptionStage encryptionStage;
	private boolean mute, hold, speaker, vibratorEnabled = true, remoteHold = false;
	private static boolean audioStarted = false;
	private int droppedDecodedPackets = 0;
	private int minBufSizePlayback, minBufSizeRecording;
	private OpusWrapper opusWrapper;
	private Resampler resampler;
	private Thread partnerTimeoutThread = null, connectionTimeoutThread = null;
	private Thread recordingThread = null, playbackThread = null, sendingThread = null, receivingThread = null, codecCompressionThread = null, codecDecompressionThread = null;
	private AudioTrack audioTrack = null;
	private static int callId = 0;
	private int totalPacketsSent = 0, totalPacketsReceived = 0;
	private NotificationManager notificationManager;
	private NotificationCompat.Builder builder;
	private AudioManager audioManager;
	private int initialAudioMode, initialRingerMode;
	private boolean initialSpeakerMode;
	private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener;
	private boolean socketInfoSent = false, socketInfoReceived = false, establishingConnection = false;
	private int reconnectAttempts = 0;
	private Chronometer chronometer = null;
	private int chronoBackup = 0;
	private int playbackSampleRate = 0;
	private Thread senderThread, reconnectingBeepsThread;
	private boolean reconnectingBeeps = false;
	private int callSource = -1;

	// Ringtones (incoming and outgoing)
	private Ringtone ringtone;
	private Vibrator vibrator = null;
	private int ringtoneStreamID = 0;
	private boolean isRingingIncoming = false, isRingingOutgoing = false;

	// Call quality fields
	private int qualityCounter = 0;
	private long lastQualityReset = 0;
	private CallQuality currentCallQuality = CallQuality.UNKNOWN;
	
	// Sounds
	private SoundPool soundpool;
	private SparseIntArray soundpoolMap;
	private static final int SOUND_ACCEPT = R.raw.call_answer;
	private static final int SOUND_DECLINE = R.raw.call_end;
	private static final int SOUND_INCOMING_RINGTONE = R.raw.ring_tone;
	private static final int SOUND_RECONNECTING = R.raw.reconnect;

	// Network quality test
	private int networkQualityPacketsReceived = 0;

	private final ConcurrentLinkedQueue<VoIPDataPacket> samplesToDecodeQueue     = new ConcurrentLinkedQueue<VoIPDataPacket>();
	private final ConcurrentLinkedQueue<VoIPDataPacket> samplesToEncodeQueue     = new ConcurrentLinkedQueue<VoIPDataPacket>();
	private final ConcurrentLinkedQueue<VoIPDataPacket> encodedBuffersQueue      = new ConcurrentLinkedQueue<VoIPDataPacket>();
	private final ConcurrentLinkedQueue<VoIPDataPacket> decodedBuffersQueue      = new ConcurrentLinkedQueue<VoIPDataPacket>();
	private final ConcurrentHashMap<Integer, VoIPDataPacket> ackWaitQueue		 = new ConcurrentHashMap<Integer, VoIPDataPacket>();
	
	// Packet prefixes
	private static final byte PP_RAW_VOICE_PACKET = 0x01;
	private static final byte PP_ENCRYPTED_VOICE_PACKET = 0x02;
	private static final byte PP_PROTOCOL_BUFFER = 0x03;
	
	// Echo cancellation
	private boolean resamplerEnabled = false;
	private boolean aecEnabled = true;
	private boolean useVADToReduceData = true;
	SolicallWrapper solicallAec = null;
	private boolean aecSpeakerSignal = false, aecMicSignal = false;
	private int audiotrackFramesWritten = 0;
	private VoIPDataPacket silentPacket;

	// Wakelock
	private WakeLock wakeLock = null;

	private VoIPConstants.CallStatus currentCallStatus;

	String pubSubListeners[] = {HikePubSub.STOP_VOIP_SERVICE};

	@Override
	public IBinder onBind(Intent intent) {
		return myBinder;
	}

	public class LocalBinder extends Binder {
		public VoIPService getService() {
			return VoIPService.this;
		}
	}
	
	public void setClientPartner(VoIPClient clientPartner) {
		this.clientPartner = clientPartner;
	}
	
	@SuppressLint("InlinedApi") @Override
	public void onCreate() {
		super.onCreate();
		Logger.d(VoIPConstants.TAG, "VoIPService onCreate()");
		acquireWakeLock();

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);

		clientPartner = new VoIPClient();
//		String myMsisdn = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(HikeMessengerApp.MSISDN_SETTING, null);

		setCallid(0);
		encryptionStage = EncryptionStage.STAGE_INITIAL;
		initAudioManager();
		keepRunning = true;
		isRingingIncoming = false;
		
		if (resamplerEnabled)
			playbackSampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_VOICE_CALL);
		else
			playbackSampleRate = AUDIO_SAMPLE_RATE;
		
		Logger.d(VoIPConstants.TAG, "Native playback sample rate: " + AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_VOICE_CALL));

		if (android.os.Build.VERSION.SDK_INT >= 17) {
			String rate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
			String size = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
			Logger.d(VoIPConstants.TAG, "Device frames/buffer:" + size + ", sample rate: " + rate);
		}
		
		VoIPUtils.resetNotificationStatus();
		startNotificationThread();

		minBufSizePlayback = AudioTrack.getMinBufferSize(playbackSampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
		minBufSizeRecording = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		
		if (!VoIPUtils.useAEC(getApplicationContext())) {
			Logger.w(VoIPConstants.TAG, "AEC disabled.");
			aecEnabled = false;
		}
		
		if (aecEnabled) {
			Logger.d(VoIPConstants.TAG, "Old minBufSizeRecording: " + minBufSizeRecording);
			if (minBufSizeRecording < SolicallWrapper.SOLICALL_FRAME_SIZE * 2) {
				minBufSizeRecording = SolicallWrapper.SOLICALL_FRAME_SIZE * 2;
			} else {
				minBufSizeRecording = ((minBufSizeRecording + (SolicallWrapper.SOLICALL_FRAME_SIZE * 2) - 1) / (SolicallWrapper.SOLICALL_FRAME_SIZE * 2)) * SolicallWrapper.SOLICALL_FRAME_SIZE * 2;
			}
			Logger.d(VoIPConstants.TAG, "New minBufSizeRecording: " + minBufSizeRecording);
		}
		
		startConnectionTimeoutThread();
		// CPU Info
		// Logger.d(VoIPConstants.TAG, "CPU: " + VoIPUtils.getCPUInfo());
	}

	@Override
	synchronized public int onStartCommand(Intent intent, int flags, int startId) {
		
		int returnInt = super.onStartCommand(intent, flags, startId);
		
		Logger.d(VoIPConstants.TAG, "VoIPService onStartCommand()");

		if (intent == null)
			return returnInt;

		String action = intent.getStringExtra(VoIPConstants.Extras.ACTION);

		if (action == null || action.isEmpty()) {
			return returnInt;
		}

		setSpeaker(false);

		// Call rejection message
		if (action.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_CANCELLED)) {
			Logger.d(VoIPConstants.TAG, "Call cancelled message.");
			if (keepRunning == true) {
				Logger.w(VoIPConstants.TAG, "Hanging up call because of call cancelled message.");
				hangUp();
			}
			return returnInt;
		}
		
		// Incoming call message
		if (action.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_REQUEST)) {

			int partnerCallId = intent.getIntExtra(VoIPConstants.Extras.CALL_ID, 0);
			setCallid(partnerCallId);
			clientPartner.setInitiator(true);
			
			// Send call initiation ack message
			VoIPUtils.sendVoIPMessageUsingHike(intent.getStringExtra(VoIPConstants.Extras.MSISDN), 
					HikeConstants.MqttMessageTypes.VOIP_CALL_REQUEST_RESPONSE, 
					partnerCallId, 
					false);
			
			/*
			 *  Would be great to start retrieving our external socket here.
			 *  Unfortunately, we can't do that because we need to know the relay port:ip
			 *  that the call initiator is going to connect to, since we need to connect
			 *  to the same socket.  
			 */
			// retrieveExternalSocket();
		}
		
		// Incoming call ack message
		if (action.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_REQUEST_RESPONSE)) {

			int partnerCallId = intent.getIntExtra(VoIPConstants.Extras.CALL_ID, 0);
			if (getCallId() == 0 || getCallId() != partnerCallId) {
				Logger.w(VoIPConstants.TAG, "Was not expecting message: " + action);
				return returnInt;
			}
			
			// Send call initiation ack ack message
			VoIPUtils.sendVoIPMessageUsingHike(intent.getStringExtra(VoIPConstants.Extras.MSISDN), 
					HikeConstants.MqttMessageTypes.VOIP_CALL_RESPONSE_RESPONSE, 
					getCallId(), 
					true);
			
			// Start ringing
			// playOutgoingCallRingtone();
			
		}
		
		// Incoming call ack ack message
		if (action.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_RESPONSE_RESPONSE)) {

			int partnerCallId = intent.getIntExtra(VoIPConstants.Extras.CALL_ID, 0);
			if (getCallId() == 0 || getCallId() != partnerCallId) {
				Logger.w(VoIPConstants.TAG, "Was not expecting message: " + action);
				return returnInt;
			}

			clientPartner.setPhoneNumber(intent.getStringExtra(VoIPConstants.Extras.MSISDN));
			sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_HANDSHAKE_COMPLETE);

			// Start playing outgoing ring
			// playIncomingCallRingtone();
		}

		// Socket information
		if (action.equals(VoIPConstants.Extras.SET_PARTNER_INFO)) 
		{
			
			int partnerCallId = intent.getIntExtra(VoIPConstants.Extras.CALL_ID, 0);
						
			// Error case: partner is trying to reconnect to us, but we aren't
			// expecting a reconnect
			boolean partnerReconnecting = intent.getBooleanExtra(VoIPConstants.Extras.RECONNECTING, false);
			if (partnerReconnecting == true && partnerCallId != getCallId()) {
				Logger.w(VoIPConstants.TAG, "Partner trying to reconnect? Remote: " + partnerCallId + ", Self: " + getCallId());
				return returnInt;
			}

			clientPartner.setInternalIPAddress(intent.getStringExtra(VoIPConstants.Extras.INTERNAL_IP));
			clientPartner.setInternalPort(intent.getIntExtra(VoIPConstants.Extras.INTERNAL_PORT, 0));
			clientPartner.setExternalIPAddress(intent.getStringExtra(VoIPConstants.Extras.EXTERNAL_IP));
			clientPartner.setExternalPort(intent.getIntExtra(VoIPConstants.Extras.EXTERNAL_PORT, 0));
			clientPartner.setPhoneNumber(intent.getStringExtra(VoIPConstants.Extras.MSISDN));
			clientPartner.setInitiator(intent.getBooleanExtra(VoIPConstants.Extras.INITIATOR, true));
			clientPartner.setRelayAddress(intent.getStringExtra(VoIPConstants.Extras.RELAY));
			clientPartner.setRelayPort(intent.getIntExtra(VoIPConstants.Extras.RELAY_PORT, VoIPConstants.ICEServerPort));

			// Error case: we are receiving a delayed v0 message for a call we 
			// initiated earlier. 
			if (!clientPartner.isInitiator() && partnerCallId != getCallId()) {
				Logger.w(VoIPConstants.TAG, "Receiving a return v0 for a invalid call.");
				return returnInt;
			}
				
			// Error case: we are receiving a repeat v0 during call setup
			if (socketInfoReceived && !partnerReconnecting) {
				Logger.d(VoIPConstants.TAG, "Repeat call initiation message.");
				// Try sending our socket info again. Caller could've missed our original message.
				if (!connected)
					sendSocketInfoToPartner();
				
				return returnInt;
			}
			
			// Check in case the other client is reconnecting to us
			if (connected && partnerCallId == getCallId() && partnerReconnecting) {
				Logger.w(VoIPConstants.TAG, "Partner trying to reconnect with us. CallId: " + getCallId());
				if (!reconnecting) {
					reconnect();
				} 
				if (socketInfoSent)
					establishConnection();
			} else {
				// All good. 
				setCallid(partnerCallId);
				if (clientPartner.isInitiator() && !reconnecting) {
					Logger.w(VoIPConstants.TAG, "Detected incoming VoIP call from: " + clientPartner.getPhoneNumber());
					retrieveExternalSocket();
				} else {
					// We have already sent our socket info to partner
					// And now they have sent us their's, so let's establish connection
					establishConnection();
				}
			}

			socketInfoReceived = true;
		}
		
		// We are initiating a VoIP call
		if (action.equals(VoIPConstants.Extras.OUTGOING_CALL)) 
		{

			// Edge case. 
			String myMsisdn = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(HikeMessengerApp.MSISDN_SETTING, null);
			String msisdn = intent.getStringExtra(VoIPConstants.Extras.MSISDN);

			if (myMsisdn != null && myMsisdn.equals(msisdn)) 
			{
				Logger.wtf(VoIPConstants.TAG, "Don't be ridiculous!");
				stop();
				return returnInt;
			}
			
			// Error case: we are in a cellular call
			if (VoIPUtils.isUserInCall(getApplicationContext())) 
			{
				Logger.w(VoIPConstants.TAG, "We are already in a cellular call.");
				sendHandlerMessage(VoIPConstants.MSG_ALREADY_IN_CALL);
				sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.CALLER_IN_NATIVE_CALL);
				return returnInt;
			}

			// Edge case: call button was hit for someone we are already speaking with. 
			if (getCallId() > 0 && clientPartner.getPhoneNumber()!=null && clientPartner.getPhoneNumber().equals(msisdn)) 
			{
				// Show activity
				Logger.d(VoIPConstants.TAG, "Restoring activity..");
				Intent i = new Intent(getApplicationContext(), VoIPActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(i);
				return returnInt;
			}
			
			if (getCallId() > 0) 
			{
				Logger.e(VoIPConstants.TAG, "Error. Already in a call.");
				sendHandlerMessage(VoIPConstants.MSG_ALREADY_IN_CALL);
				return returnInt;
			}
			
			// we are making an outgoing call
			clientPartner.setPhoneNumber(intent.getStringExtra(VoIPConstants.Extras.MSISDN));
			clientPartner.setInitiator(false);

			callSource = intent.getIntExtra(VoIPConstants.Extras.CALL_SOURCE, -1);
			if(callSource == CallSource.MISSED_CALL_NOTIF.ordinal())
			{
				VoIPUtils.cancelMissedCallNotification(getApplicationContext());
			}

			setCallid(new Random().nextInt(2000000000));
			Logger.w(VoIPConstants.TAG, "Making outgoing call to: " + clientPartner.getPhoneNumber() + ", id: " + getCallId());

			// Send call initiation message
			VoIPUtils.sendVoIPMessageUsingHike(clientPartner.getPhoneNumber(), 
					HikeConstants.MqttMessageTypes.VOIP_CALL_REQUEST, 
					getCallId(), true);
			
			// Show activity
			Intent i = new Intent(getApplicationContext(), VoIPActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
			
			retrieveExternalSocket();
			sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_CLICK);
		}

		if(getCallStatus() == null)
		{
			setInitialCallStatus();
		}

		return returnInt;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		stop();
		setCallid(0);	// Redundant, for bug #44018
		dismissNotification();
		releaseWakeLock();
		Logger.d(VoIPConstants.TAG, "VoIP Service destroyed.");
	}
	
	private void startConnectionTimeoutThread() {
		
		if (connectionTimeoutThread != null) {
			Logger.d(VoIPConstants.TAG, "Restarting connection timeout thread.");
			connectionTimeoutThread.interrupt();
			connectionTimeoutThread = null;
		}
		
		connectionTimeoutThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(30000);
					if (!connected) {
						Logger.w(VoIPConstants.TAG, "Why aren't we connected yet? Terminating service.");
						keepRunning = true;	// So that stop() is executed entirely. 
						stop();
					}
				} catch (InterruptedException e) {
					Logger.d(VoIPConstants.TAG, "Connection timeout thread interrupted. Das ist gut!");
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
			Logger.d(VoIPConstants.TAG, "Wakelock acquired.");
		}
	}
	
	private void releaseWakeLock() 
	{
		if (wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
			Logger.d(VoIPConstants.TAG, "Wakelock released.");
		} else {
			Logger.d(VoIPConstants.TAG, "Wakelock not detected.");
		}
	}
	
	private void startNotificationThread() {
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (keepRunning) {
					try {
						Thread.sleep(1000);
						if (keepRunning)
							showNotification();
					} catch (InterruptedException e) {
						// All good
						break;
					}
					
				}
			}
		}, "NOTIFICATION_THREAD").start();
	}

	private void showNotification() {
		
//		Logger.d(VoIPConstants.TAG, "Showing notification..");
		Intent myIntent = new Intent(getApplicationContext(), VoIPActivity.class);
		myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, myIntent, 0);

		if (notificationManager == null)
			notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		if (builder == null) 
			builder = new NotificationCompat.Builder(getApplicationContext());

		int callDuration = getCallDuration();
		String durationString = (callDuration == 0)? "" : String.format(Locale.getDefault(), " (%02d:%02d)", (callDuration / 60), (callDuration % 60));

		String title = null;
		if (clientPartner.getName() == null)
			title = getString(R.string.voip_call_chat);
		else
			title = getString(R.string.voip_call_notification_title, clientPartner.getName()); 
		
		String text = null;
		switch (getCallStatus()) {
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
			
		default:
			text = getString(R.string.voip_call_notification_text, durationString); 
			break;
		}

		Notification myNotification = builder
		.setContentTitle(title)
		.setContentText(text)
		.setSmallIcon(HikeNotification.getInstance(this).returnSmallIcon())
		.setContentIntent(pendingIntent)
		.setOngoing(true)
		.setAutoCancel(true)
		.build();
		
		notificationManager.notify(null, NOTIFICATION_IDENTIFIER, myNotification);
	}

	public void dismissNotification() {
		// Dismiss notification
		if (notificationManager != null) {
			Logger.d(VoIPConstants.TAG, "Removing notification..");
			notificationManager.cancel(NOTIFICATION_IDENTIFIER);
		}
	}
	
	private void initAudioManager() {

//		Logger.w(VoIPConstants.TAG, "Initializing audio manager.");
		
		if (audioManager == null)
			audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		
		saveCurrentAudioSettings();
		
		// Audio focus
		mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
			
			@Override
			public void onAudioFocusChange(int focusChange) {
				switch (focusChange) {
				case AudioManager.AUDIOFOCUS_GAIN:
					Logger.w(VoIPConstants.TAG, "AUDIOFOCUS_GAIN");
					if (getCallDuration() > 0 && hold == true)
						setHold(false);
					break;
				case AudioManager.AUDIOFOCUS_LOSS:
					Logger.w(VoIPConstants.TAG, "AUDIOFOCUS_LOSS");
					if (getCallDuration() > 0)
						setHold(true);
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
					Logger.d(VoIPConstants.TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
					if (getCallDuration() > 0)
						setHold(true);
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
					Logger.w(VoIPConstants.TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
//					setHold(true);
					break;
				}
			}
		};
		
		int result = audioManager.requestAudioFocus(mOnAudioFocusChangeListener, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
		if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			Logger.w(VoIPConstants.TAG, "Unable to gain audio focus. result: " + result);
		} else
			Logger.d(VoIPConstants.TAG, "Received audio focus.");
		
		initSoundPool();

		// Check vibrator
		if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT)
			vibratorEnabled = false;
		else
			vibratorEnabled = true;
	}
	
	private void releaseAudioManager() {
		if (audioManager != null) {
			audioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
			restoreAudioSettings();
		}
		
		if (soundpool != null) {
			Logger.d(VoIPConstants.TAG, "Releasing soundpool.");
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
		initialRingerMode = audioManager.getRingerMode();
		initialSpeakerMode = audioManager.isSpeakerphoneOn();
	}

	private void restoreAudioSettings() {
		audioManager.setMode(initialAudioMode);
		audioManager.setRingerMode(initialRingerMode);
		audioManager.setSpeakerphoneOn(initialSpeakerMode);
	}
	
	@SuppressWarnings("deprecation")
	@SuppressLint("InlinedApi") 
	private void initSoundPool() {
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			AudioAttributes audioAttributes = new AudioAttributes.Builder()
			.setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
			.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
			.build();

			soundpool = new SoundPool.Builder()
			.setMaxStreams(2)
			.setAudioAttributes(audioAttributes)
			.build();
		} else {
			soundpool = new SoundPool(2, AudioManager.STREAM_VOICE_CALL, 0);
		}

		soundpoolMap = new SparseIntArray(3);
		soundpoolMap.put(SOUND_ACCEPT, soundpool.load(getApplicationContext(), SOUND_ACCEPT, 1));
		soundpoolMap.put(SOUND_DECLINE, soundpool.load(getApplicationContext(), SOUND_DECLINE, 1));
		soundpoolMap.put(SOUND_INCOMING_RINGTONE, soundpool.load(getApplicationContext(), SOUND_INCOMING_RINGTONE, 1));
		soundpoolMap.put(SOUND_RECONNECTING, soundpool.load(getApplicationContext(), SOUND_RECONNECTING, 1));
	}
	
	private int playFromSoundPool(int soundId, boolean loop) {
		int streamID = 0;
		if (soundpool == null || soundpoolMap == null) {
			initSoundPool();
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
		return clientPartner;
	}
	
	public void startStreaming() {
		
		if (clientPartner == null) {
			Logger.e(VoIPConstants.TAG, "Clients (partner and/or self) not set.");
			return;
		}
		
		startCodec(); 
		startSendingAndReceiving();
		startHeartbeat();
		exchangeCryptoInfo();
		
		if (connectionTimeoutThread != null)
			connectionTimeoutThread.interrupt();
		
		Logger.d(VoIPConstants.TAG, "Streaming started.");
	}
	
	public int getCallDuration() {
		int seconds = 0;
		if (chronometer != null) {
			seconds = (int) ((SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000);
		} else
			seconds = chronoBackup;
		
		return seconds;
	}
	
	public ConnectionMethods getConnectionMethod() {
		return clientPartner.getPreferredConnectionMethod();
	}
	
	public void startChrono() {

		try {
			if (chronometer == null) {
//				Looper.prepare();
//				Logger.w(VoIPConstants.TAG, "Starting chrono..");
				chronometer = new Chronometer(VoIPService.this);
				chronometer.setBase(SystemClock.elapsedRealtime());
				chronometer.start();
//				Looper.loop();
			}
		} catch (Exception e) {
			Logger.w(VoIPConstants.TAG, "Chrono exception: " + e.toString());
		}
	}
	

	/**
	 * Terminate the service. 
	 */
	synchronized public void stop() {

		synchronized (this) {
			if (keepRunning == false) {
				// Logger.w(VoIPConstants.TAG, "Trying to stop a stopped service?");
				sendHandlerMessage(VoIPConstants.MSG_SHUTDOWN_ACTIVITY);
				connected = false;
				setCallid(0);
				return;
			}
			keepRunning = false;
		}

		if(currentCallStatus!=VoIPConstants.CallStatus.PARTNER_BUSY)
		{
			setCallStatus(VoIPConstants.CallStatus.ENDED);
		}

		Bundle bundle = new Bundle();
		bundle.putInt(VoIPConstants.CALL_ID, getCallId());
		bundle.putInt(VoIPConstants.IS_CALL_INITIATOR, clientPartner.isInitiator() ? 0 : 1);
		bundle.putInt(VoIPConstants.CALL_NETWORK_TYPE, VoIPUtils.getConnectionClass(getApplicationContext()).ordinal());
		bundle.putString(VoIPConstants.PARTNER_MSISDN, clientPartner.getPhoneNumber());

		sendHandlerMessage(VoIPConstants.MSG_SHUTDOWN_ACTIVITY, bundle);

		Logger.d(VoIPConstants.TAG,
				"============= Call Summary =============\n" +
				"Bytes sent / received: " + totalBytesSent + " / " + totalBytesReceived +
				"\nPackets sent / received: " + totalPacketsSent + " / " + totalPacketsReceived +
				"\nPure voice bytes: " + rawVoiceSent +
				"\nDropped decoded packets: " + droppedDecodedPackets +
				"\nReconnect attempts: " + reconnectAttempts +
				"\nCall duration: " + getCallDuration() + "\n" +
				"========================================");
		
		sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_END);

		if(reconnecting)
		{
			sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_DROP);
		}
		
		// send a call rejected message through hike as well
		VoIPUtils.sendVoIPMessageUsingHike(clientPartner.getPhoneNumber(), 
				HikeConstants.MqttMessageTypes.VOIP_CALL_CANCELLED, 
				getCallId(), false);

		// Reset variables
		setCallid(0);
		connected = false;
		socketInfoReceived = false;
		audioStarted = false;
		establishingConnection = false;
		isRingingOutgoing = false;
		isRingingIncoming = false;

		if(socket != null)
			socket.close();

		// Terminate threads
		if (connectionTimeoutThread != null)
			connectionTimeoutThread.interrupt();

		if (partnerTimeoutThread != null)
			partnerTimeoutThread.interrupt();

		if (reconnectingBeepsThread != null)
			reconnectingBeepsThread.interrupt();
		
		if (sendingThread != null)
			sendingThread.interrupt();

		if (receivingThread != null)
			receivingThread.interrupt();
		
		if (codecDecompressionThread != null)
			codecDecompressionThread.interrupt();
		
		if (codecCompressionThread != null)
			codecCompressionThread.interrupt();
		
		if (playbackThread != null)
			playbackThread.interrupt();
		
		if (recordingThread != null)
			recordingThread.interrupt();
		
		stopRingtone();
		stopFromSoundPool(ringtoneStreamID);
		
		if (connected == true) {
			setSpeaker(true);
			playFromSoundPool(SOUND_DECLINE, false);
		}
		
		if (opusWrapper != null) {
			opusWrapper.destroy();
			opusWrapper = null;
			
		}

		if (solicallAec != null) {
			solicallAec.destroy();
			solicallAec = null;
		}
		
		if(chronometer != null)
		{
			chronometer.stop();
			chronometer = null;
		}

		releaseAudioManager();
		
		// Empty the queues
		samplesToDecodeQueue.clear();
		samplesToEncodeQueue.clear();
		encodedBuffersQueue.clear();
		decodedBuffersQueue.clear();
		ackWaitQueue.clear();
		
		releaseWakeLock();
		stopSelf();
	}
	
	/**
	 * Same as {@link #stop()}, except that a call termination packet
	 * is sent to the call partner as well. 
	 */
	public void hangUp() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.END_CALL);
				sendPacket(dp, true);
				stop();
			}
		},"HANG_UP_THREAD").start();
		VoIPUtils.addMessageToChatThread(this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_CALL_SUMMARY, getCallDuration(), -1, false);
	}
	
	public void rejectIncomingCall() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.CALL_DECLINED);
				sendPacket(dp, true);
				stop();
			}
		},"REJECT_INCOMING_CALL_THREAD").start();
		
		// Here we don't show a missed call notification, but add the message to the chat thread
		VoIPUtils.addMessageToChatThread(this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING, 0, -1, false);
		sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_REJECT);
	}
	
	public void setMute(boolean mute)
	{
		this.mute = mute;
	}

	public boolean getMute()
	{
		return mute;
	}
	
	private void sendHandlerMessage(int message) {
		Message msg = Message.obtain();
		msg.what = message;
		try {
			if (mMessenger != null)
				mMessenger.send(msg);
		} catch (RemoteException e) {
			Logger.e(VoIPConstants.TAG, "Messenger RemoteException: " + e.toString());
		}
	}

	private void sendHandlerMessage(int message, Bundle bundle) {
		Message msg = Message.obtain();
		msg.what = message;
		msg.setData(bundle);
		try {
			if (mMessenger != null)
				mMessenger.send(msg);
		} catch (RemoteException e) {
			Logger.e(VoIPConstants.TAG, "Messenger RemoteException: " + e.toString());
		}
	}

	/**
	 * Reconnect after a communications failure.
	 */
	private void reconnect() {

		if (reconnecting)
			return;
		else
			reconnecting = true;

		reconnectAttempts++;
		Logger.w(VoIPConstants.TAG, "VoIPService reconnect()");
		setCallStatus(VoIPConstants.CallStatus.RECONNECTING);
		sendHandlerMessage(VoIPConstants.MSG_RECONNECTING);
		socketInfoReceived = false;
		socketInfoSent = false;
		connected = false;
		removeExternalSocketInfo();
		retrieveExternalSocket();
		startReconnectBeeps();
	}
	
	private void startReconnectBeeps() {
		if (reconnectingBeeps)
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
	
	private void setIdealBitrate() {
		
		ConnectionClass connection = VoIPUtils.getConnectionClass(getApplicationContext());

		SharedPreferences prefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		int twoGBitrate = prefs.getInt(HikeMessengerApp.VOIP_BITRATE_2G, VoIPConstants.BITRATE_2G);
		int threeGBitrate = prefs.getInt(HikeMessengerApp.VOIP_BITRATE_3G, VoIPConstants.BITRATE_3G);
		int wifiBitrate = prefs.getInt(HikeMessengerApp.VOIP_BITRATE_WIFI, VoIPConstants.BITRATE_WIFI);
		
		if (connection == ConnectionClass.TwoG)
			localBitrate = twoGBitrate;
		else if (connection == ConnectionClass.ThreeG)
			localBitrate = threeGBitrate;
		else if (connection == ConnectionClass.WiFi)
			localBitrate = wifiBitrate;
		else 
			localBitrate = wifiBitrate;
		
		if (remoteBitrate > 0 && remoteBitrate < localBitrate)
			localBitrate = remoteBitrate;
		
		Logger.d(VoIPConstants.TAG, "Detected ideal bitrate: " + localBitrate);
		sendHandlerMessage(VoIPConstants.MSG_CURRENT_BITRATE);
		
		if (opusWrapper != null)
			opusWrapper.setEncoderBitrate(localBitrate);
	}
	
	private void startHeartbeat() {
	
		// Sending heart beat
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.HEARTBEAT);
				while (keepRunning == true) {
					sendPacket(dp, false);
					try {
						Thread.sleep(HEARTBEAT_INTERVAL);
					} catch (InterruptedException e) {
						Logger.d(VoIPConstants.TAG, "Heartbeat InterruptedException: " + e.toString());
						break;
					}

					if (isAudioRunning())
						chronoBackup++;
				}
			}
		}, "SEND_HEART_BEAT_THREAD").start();
		
		// Listening for heartbeat, and housekeeping
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				lastHeartbeat = System.currentTimeMillis();
				while (keepRunning == true) {
					if (System.currentTimeMillis() - lastHeartbeat > HEARTBEAT_TIMEOUT && !reconnecting) {
//						Logger.w(VoIPConstants.TAG, "Heartbeat failure. Reconnecting.. ");
						startReconnectBeeps();
						if (!clientPartner.isInitiator() && isConnected() && isAudioRunning())
							reconnect();
					}
					
					if (System.currentTimeMillis() - lastHeartbeat > HEARTBEAT_HARD_TIMEOUT) {
						Logger.d(VoIPConstants.TAG, "Giving up on connection.");
						hangUp();
						break;
					}
					
					sendPacketsWaitingForAck();
					
					// Monitor quality of incoming data
					if ((System.currentTimeMillis() - lastQualityReset > VoIPConstants.QUALITY_WINDOW * 1000) 
							&& getCallDuration() > VoIPConstants.QUALITY_WINDOW
							&& remoteHold == false) {
						
						CallQuality newQuality;
						int idealPacketCount = (AUDIO_SAMPLE_RATE * VoIPConstants.QUALITY_WINDOW) / OpusWrapper.OPUS_FRAME_SIZE; 
						if (qualityCounter >= idealPacketCount)
							newQuality = CallQuality.EXCELLENT;
						else if (qualityCounter >= idealPacketCount - VoIPConstants.QUALITY_WINDOW)
							newQuality = CallQuality.GOOD;
						else if (qualityCounter >= idealPacketCount - VoIPConstants.QUALITY_WINDOW * 2)
							newQuality = CallQuality.FAIR;
						else 
							newQuality = CallQuality.WEAK;

						if (currentCallQuality != newQuality) {
							currentCallQuality = newQuality;
							sendHandlerMessage(VoIPConstants.MSG_UPDATE_QUALITY);
						}

						qualityCounter = 0;
						lastQualityReset = System.currentTimeMillis();
					}
					
					// Drop packets if getting left behind
					while (samplesToDecodeQueue.size() > MAX_SAMPLES_BUFFER) {
						Logger.d(VoIPConstants.TAG, "Dropping to_decode packet.");
						samplesToDecodeQueue.poll();
					}
					
					while (samplesToEncodeQueue.size() > MAX_SAMPLES_BUFFER) {
						if (audioStarted)
							Logger.d(VoIPConstants.TAG, "Dropping to_encode packet.");
						samplesToEncodeQueue.poll();
					}
					
					while (encodedBuffersQueue.size() > MAX_SAMPLES_BUFFER) {
						Logger.d(VoIPConstants.TAG, "Dropping encoded packet.");
						encodedBuffersQueue.poll();
					}

					while (decodedBuffersQueue.size() > MAX_SAMPLES_BUFFER) {
//						Logger.d(VoIPConstants.TAG, "Dropping decoded packet.");
						droppedDecodedPackets++;
						decodedBuffersQueue.poll();
					}

					try {
						Thread.sleep(HEARTBEAT_INTERVAL);
					} catch (InterruptedException e) {
						Logger.d(VoIPConstants.TAG, "Heartbeat InterruptedException: " + e.toString());
					}
					
				}
			}
		}, "LISTEN_HEART_BEAT_THREAD").start();
		
	}
	
	private void startCodec() {
		try
		{
			opusWrapper = new OpusWrapper();
			opusWrapper.getDecoder(AUDIO_SAMPLE_RATE, 1);
			opusWrapper.getEncoder(AUDIO_SAMPLE_RATE, 1, localBitrate);
		}
		catch (UnsatisfiedLinkError e)
		{
			Logger.e(VoIPConstants.TAG, "Codec exception: " + e.toString());
			hangUp();
		}
		catch (Exception e) 
		{
			Logger.e(VoIPConstants.TAG, "Codec exception: " + e.toString());
			hangUp();
		}
		
		if (resamplerEnabled) {
			resampler = new Resampler();
		}
		
		startCodecDecompression();
		startCodecCompression();
		
		/*
		// Set audio gain
		SharedPreferences preferences = getSharedPreferences(HikeMessengerApp.VOIP_SETTINGS, Context.MODE_PRIVATE);
		gain = preferences.getInt(HikeMessengerApp.VOIP_AUDIO_GAIN, 0);
		opusWrapper.setDecoderGain(gain);
		*/
		
		// Set encoder complexity which directly affects CPU usage
		opusWrapper.setEncoderComplexity(0);
		
		// Initialize AEC
		if (aecEnabled) 
		{
			try 
			{
				solicallAec = new SolicallWrapper();
				solicallAec.init();
			}
			catch (UnsatisfiedLinkError e)
			{
				Logger.e(VoIPConstants.TAG, "Solicall init error: " + e.toString());
				solicallAec = null;
				aecEnabled = false;
			}
			catch (IOException e) 
			{
				Logger.e(VoIPConstants.TAG, "Solicall init exception: " + e.toString());
				solicallAec = null;
				aecEnabled = false;
			}	
		}
	}
	
	private void startCodecDecompression() {
		
		codecDecompressionThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				int lastPacketReceived = 0;
				int uncompressedLength = 0;
				while (keepRunning == true) {
					VoIPDataPacket dpdecode = samplesToDecodeQueue.peek();
					if (dpdecode != null) {
						samplesToDecodeQueue.poll();
						byte[] uncompressedData = new byte[OpusWrapper.OPUS_FRAME_SIZE * 10];	// Just to be safe, we make a big buffer
						
						if (dpdecode.getVoicePacketNumber() > 0 && dpdecode.getVoicePacketNumber() <= lastPacketReceived)
							continue;	// We received an old packet again
						
						// Handle packet loss (unused as on Dec 16, 2014)
						if (dpdecode.getVoicePacketNumber() > lastPacketReceived + 1) {
							Logger.d(VoIPConstants.TAG, "Packet loss! (" + (dpdecode.getVoicePacketNumber() - lastPacketReceived) + ")");
							lastPacketReceived = dpdecode.getVoicePacketNumber();
							try {
								uncompressedLength = opusWrapper.plc(dpdecode.getData(), uncompressedData);
								uncompressedLength *= 2;	
								if (uncompressedLength > 0) {
									VoIPDataPacket dp = new VoIPDataPacket(PacketType.VOICE_PACKET);
									dp.write(uncompressedData);
									dp.setLength(uncompressedLength);
									
									synchronized (decodedBuffersQueue) {
										decodedBuffersQueue.add(dp);
										decodedBuffersQueue.notify();
									}
								}
							} catch (Exception e) {
								Logger.d(VoIPConstants.TAG, "PLC exception: " + e.toString());
							}
						}
						
						// Regular decoding
						try {
							// Logger.d(VoIPActivity.logTag, "Decompressing data of length: " + dpdecode.getLength());
							uncompressedLength = opusWrapper.decode(dpdecode.getData(), uncompressedData);
							uncompressedLength = uncompressedLength * 2;
							if (uncompressedLength > 0) {
								// We have a decoded packet
								lastPacketReceived = dpdecode.getVoicePacketNumber();

								VoIPDataPacket dp = new VoIPDataPacket(PacketType.VOICE_PACKET);
								byte[] packetData = new byte[uncompressedLength];
								System.arraycopy(uncompressedData, 0, packetData, 0, uncompressedLength);
								dp.write(packetData);
								
								synchronized (decodedBuffersQueue) {
									decodedBuffersQueue.add(dp);
									decodedBuffersQueue.notify();
								}

							}
						} catch (Exception e) {
							Logger.d(VoIPConstants.TAG, "Opus decode exception: " + e.toString());
						}
					} else {
						// Wait till we have a packet to decompress
						synchronized (samplesToDecodeQueue) {
							try {
								samplesToDecodeQueue.wait();
							} catch (InterruptedException e) {
//								Logger.d(VoIPConstants.TAG, "samplesToDecodeQueue interrupted: " + e.toString());
								break;
							}
						}
					}
				}
			}
		}, "CODE_DECOMPRESSION_THREAD");
		
		codecDecompressionThread.start();
	}
	
	private void startCodecCompression() {
		codecCompressionThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				byte[] compressedData = new byte[OpusWrapper.OPUS_FRAME_SIZE * 10];
				int compressedDataLength = 0;
				boolean lowBitrateTrigger = false;
				
				while (keepRunning == true) {
					VoIPDataPacket dpencode = samplesToEncodeQueue.peek();
					if (dpencode != null) {
						samplesToEncodeQueue.poll();

						// AEC
						if (solicallAec != null && aecEnabled && aecMicSignal && aecSpeakerSignal) {
							int ret = solicallAec.processMic(dpencode.getData());
							
							if (useVADToReduceData) {
								
								/*
								 * If the mic signal does not contain voice, we can handle the situation in three ways -
								 * 1. Don't transmit anything. The other end will fill up the gap with silence. Downside - signal quality indicator will switch to weak. 
								 * 2. Send a special "silent" packet. Downside - older builds will not support this, and fall back to (1).
								 * 3. Lower the bitrate for non-voice packets. Downside - (1) and (2) will reduce the CPU usage, and lower bandwidth consumption even more. 
								 */

								// Approach (3)
								if (ret == 0) {
									if (!lowBitrateTrigger) {
										// There is no voice signal, bitrate should be lowered
										lowBitrateTrigger = true;
										opusWrapper.setEncoderBitrate(OpusWrapper.OPUS_LOWEST_SUPPORTED_BITRATE);
									}
								} else if (lowBitrateTrigger) {
									// Mic signal is reverting to voice
									lowBitrateTrigger = false;
									opusWrapper.setEncoderBitrate(localBitrate);
								}
							}
						} else
							aecMicSignal = true;
						
						try {
							// Add the uncompressed audio to the compression buffer
							opusWrapper.queue(dpencode.getData());
							// Get compressed data from the encoder
							while ((compressedDataLength = opusWrapper.getEncodedData(OpusWrapper.OPUS_FRAME_SIZE, compressedData, compressedData.length)) > 0) {
								byte[] trimmedCompressedData = new byte[compressedDataLength];
								System.arraycopy(compressedData, 0, trimmedCompressedData, 0, compressedDataLength);
								VoIPDataPacket dp = new VoIPDataPacket(PacketType.VOICE_PACKET);
								dp.write(trimmedCompressedData);
								synchronized (encodedBuffersQueue) { 
									encodedBuffersQueue.add(dp);
									encodedBuffersQueue.notify();
								}
							}
						} catch (Exception e) {
							Logger.e(VoIPConstants.TAG, "Compression error: " + e.toString());
						}
						
					} else {
						synchronized (samplesToEncodeQueue) {
							try {
								samplesToEncodeQueue.wait();
							} catch (InterruptedException e) {
//								Logger.d(VoIPConstants.TAG, "samplesToEncodeQueue interrupted: " + e.toString());
								break;
							}
						}
					}
				}
			}
		}, "CODE_COMPRESSION_THREAD");
		
		codecCompressionThread.start();
	}
	
	public void acceptIncomingCall() {
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.START_VOICE);
				sendPacket(dp, true);
			}
		}, "ACCEPT_INCOMING_CALL_THREAD").start();

		startRecordingAndPlayback();
		sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_ACCEPT);
	}
	
	private void startRecordingAndPlayback() {

		if (audioStarted == true) {
			Logger.d(VoIPConstants.TAG, "Audio already started.");
			return;
		}

		if(clientPartner.getPreferredConnectionMethod() == ConnectionMethods.RELAY)
		{
			sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_RELAY);
		}

		if (!isConnected()) {
			Logger.d(VoIPConstants.TAG, "Call has been answered before connection was established.");
			startReconnectBeeps();
		}
		
		Logger.d(VoIPConstants.TAG, "Starting audio record / playback.");
		if (partnerTimeoutThread != null)
			partnerTimeoutThread.interrupt();
		stopRingtone();
		stopFromSoundPool(ringtoneStreamID);
		isRingingOutgoing = isRingingIncoming = false;
		playFromSoundPool(SOUND_ACCEPT, false);
		startRecording();
		startPlayBack();
		setCallStatus(VoIPConstants.CallStatus.ACTIVE);
		sendHandlerMessage(VoIPConstants.MSG_AUDIO_START);
		startChrono();
		audioStarted = true;
		
		// When the call has been answered, we will send our network connection class
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				setIdealBitrate();
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.CURRENT_BITRATE);
				dp.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(localBitrate).array());
				sendPacket(dp, true);
			}
		}, "SEND_CURRENT_BITRATE").start();
	}
	
	private void startRecording() {
		recordingThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

				AudioRecord recorder = null;
				Logger.d(VoIPConstants.TAG, "minBufSizeRecording: " + minBufSizeRecording);
				
				int audioSource = VoIPUtils.getAudioSource();

				// Start recording audio from the mic
				try
				{
					recorder = new AudioRecord(audioSource, AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufSizeRecording);
					recorder.startRecording();
				}
				catch(IllegalArgumentException e)
				{
					Logger.e(VoIPConstants.TAG, "AudioRecord init failed." + e.toString());
					sendHandlerMessage(VoIPConstants.MSG_AUDIORECORD_FAILURE);
				}
				catch (IllegalStateException e)
				{
					Logger.e(VoIPConstants.TAG, "Recorder exception: " + e.toString());
					sendHandlerMessage(VoIPConstants.MSG_AUDIORECORD_FAILURE);
				}
				
				if (recorder == null)
					return;
				
				// Start processing recorded data
				byte[] recordedData = new byte[minBufSizeRecording];
				int retVal;
            	int index = 0;
            	int newSize = 0;
				while (keepRunning == true) {
					retVal = recorder.read(recordedData, 0, recordedData.length);
					if (retVal != recordedData.length) {
						Logger.w(VoIPConstants.TAG, "Unexpected recorded data length. Expected: " + recordedData.length + ", Recorded: " + retVal);
						continue;
					}
					
					if (mute == true)
						continue;

//					Logger.d(VoIPConstants.TAG, "Recorded: " + Arrays.toString(recordedData).substring(0, 50));
					
					// Break input audio into smaller chunks
                	while (index < retVal) {
                		if (retVal - index < SolicallWrapper.SOLICALL_FRAME_SIZE * 2)
                			newSize = retVal - index;
                		else
                			newSize = SolicallWrapper.SOLICALL_FRAME_SIZE * 2;

                		byte[] data = new byte[newSize];
                		System.arraycopy(recordedData, index, data, 0, newSize);
                		index += newSize;

	                	// Add it to the samples to encode queue
						VoIPDataPacket dp = new VoIPDataPacket(VoIPDataPacket.PacketType.VOICE_PACKET);
	                	dp.write(data);

	                	synchronized (samplesToEncodeQueue) {
		                	samplesToEncodeQueue.add(dp);
	                		samplesToEncodeQueue.notify();
						}
                	}
					index = 0;
					
					if (Thread.interrupted()) {
//						Logger.w(VoIPConstants.TAG, "Stopping recording thread.");
						break;
					}
				}
				
				// Stop recording
				if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
					recorder.stop();
				
				recorder.release();
			}
		}, "RECORDING_THREAD");
		
		recordingThread.start();
	}
	
	private void startPlayBack() {
		
		playbackThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				int index = 0, size = 0;
				Logger.d(VoIPConstants.TAG, "AUDIOTRACK - minBufSizePlayback: " + minBufSizePlayback + ", playbackSampleRate: " + playbackSampleRate);
			
				setAudioModeInCall();
				try {
					audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, playbackSampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufSizePlayback, AudioTrack.MODE_STREAM);
				} catch (IllegalArgumentException e) {
					Logger.w(VoIPConstants.TAG, "Unable to initialize AudioTrack: " + e.toString());
					hangUp();
					return;
				}
				
				try {
					audioTrack.play();
				} catch (IllegalStateException e) {
					Logger.e(VoIPConstants.TAG, "Audiotrack error: " + e.toString());
					hangUp();
				}
				
				// Clear the audio queue
				decodedBuffersQueue.clear();
				
				byte[] solicallSpeakerBuffer = new byte[SolicallWrapper.SOLICALL_FRAME_SIZE * 2];
				while (keepRunning == true) {
					VoIPDataPacket dp = decodedBuffersQueue.peek();
					if (dp != null) {
						decodedBuffersQueue.poll();

						// AEC
						if (solicallAec != null && aecEnabled && aecSpeakerSignal && aecMicSignal) {
							index = 0;
							while (index < dp.getData().length) {
								size = Math.min(SolicallWrapper.SOLICALL_FRAME_SIZE * 2, dp.getLength() - index);
								System.arraycopy(dp.getData(), index, solicallSpeakerBuffer, 0, size);
								solicallAec.processSpeaker(solicallSpeakerBuffer);
								index += size; 
							}
						} else
							aecSpeakerSignal = true;

						// Resample
						if (resamplerEnabled && playbackSampleRate != AUDIO_SAMPLE_RATE) {
							// We need to resample the output signal
							byte[] output = resampler.reSample(dp.getData(), 16, AUDIO_SAMPLE_RATE, playbackSampleRate);
							dp.write(output);
						} 
						
						// For streaming mode, we must write data in chunks <= buffer size
						index = 0;
//						long timer = System.currentTimeMillis();
						while (index < dp.getLength()) {
							size = Math.min(minBufSizePlayback, dp.getLength() - index);
							audioTrack.write(dp.getData(), index, size);
							index += size; 
						}
						audiotrackFramesWritten += dp.getLength() / 2;
//						Logger.d(VoIPConstants.TAG, "Time: " + (System.currentTimeMillis() - timer) + "ms, " +
//								"Data: " + (dp.getLength() / 2) * 1000 / playbackSampleRate + "ms.");

					} else {
						synchronized (decodedBuffersQueue) {
							try {
								decodedBuffersQueue.wait();
							} catch (InterruptedException e) {
//								Logger.d(VoIPConstants.TAG, "decodedBuffersQueue interrupted: " + e.toString());
								break;
							}
						}
					}
					
					if (Thread.interrupted()) {
//						Logger.w(VoIPConstants.TAG, "Stopping playback thread.");
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
						Logger.w(VoIPConstants.TAG, "Audiotrack IllegalStateException: " + e.toString());
					}
				}
			}
		}, "PLAY_BACK_THREAD");
		
		playbackThread.start();
		startAudioTrackMonitoringThread();
	}
	
	private void startAudioTrackMonitoringThread() {
		
		byte[] silence = new byte[OpusWrapper.OPUS_FRAME_SIZE * 2];
		silentPacket = new VoIPDataPacket(PacketType.VOICE_PACKET);
		silentPacket.setData(silence);

		new Thread(new Runnable() {
			
			@Override
			public void run() {
				final int frameDuration = (OpusWrapper.OPUS_FRAME_SIZE * 1000) / (playbackSampleRate * 2);		// Monitor will run every 30ms
				
				while (keepRunning) {
					
					try {
						if (audioTrack != null) {
							if (audiotrackFramesWritten < audioTrack.getPlaybackHeadPosition() + OpusWrapper.OPUS_FRAME_SIZE &&
									decodedBuffersQueue.size() == 0) {
								// We are running low on speaker data
			                	synchronized (decodedBuffersQueue) {
				                	decodedBuffersQueue.add(silentPacket);
				                	decodedBuffersQueue.notify();
								}
							}
						}
						Thread.sleep(frameDuration);
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					} catch (IllegalStateException e) {
						Logger.d(VoIPConstants.TAG, "startAudioTrackMonitoringThread() IllegalStateException: " + e.toString());
						break;
					} catch (NullPointerException e) {
						Logger.d(VoIPConstants.TAG, "startAudioTrackMonitoringThread() NullPointerException: " + e.toString());
						break;
					}
				}
				
			}
		}, "AUDIOTRACK_MONITOR_THREAD").start();
	}
	
	private void startSendingAndReceiving() {
		
		// In case we are reconnecting, current sending and receiving threads
		// need to be restarted because the sockets would have changed.
		if (sendingThread != null)
			sendingThread.interrupt();
		if (receivingThread != null)
			receivingThread.interrupt();
		
		startSending();
		startReceiving();
	}
	
	private void startSending() {
		sendingThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				int voicePacketCount = 1;
				while (keepRunning == true) {
					
					if (Thread.interrupted()) {
//						Logger.w(VoIPConstants.TAG, "Quitting sending thread.");
						break;
					}

					VoIPDataPacket dp = encodedBuffersQueue.peek();
					if (dp != null) {
						encodedBuffersQueue.poll();
						dp.voicePacketNumber = voicePacketCount++;
						
						// Encrypt packet
						if (encryptionStage == EncryptionStage.STAGE_READY) {
							byte[] origData = dp.getData();
							dp.write(encryptor.aesEncrypt(origData));
							dp.setEncrypted(true);
						}
						
						sendPacket(dp, false);
					} else {
						synchronized (encodedBuffersQueue) {
							try {
								encodedBuffersQueue.wait();
							} catch (InterruptedException e) {
//								Logger.d(VoIPConstants.TAG, "encodedBuffersQueue interrupted: " + e.toString());
								break;
							}
						}
					}
				}
			}
		}, "VOIP_SEND_THREAD");
		
		sendingThread.start();
	}
	
	private void startReceiving() {
		if (receivingThread != null) {
//			Logger.d(VoIPConstants.TAG, "Stopping receiving thread before restarting.");
			receivingThread.interrupt();
		}
		
		receivingThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
//				Logger.w(VoIPConstants.TAG, "Receiving thread starting and listening on: " + socket.getLocalPort());
				byte[] buffer = new byte[50000];
				while (keepRunning == true) {

					if (Thread.currentThread().isInterrupted()) {
//						Logger.w(VoIPConstants.TAG, "Quitting receiving thread.");
						break;
					}
					
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					try {
						socket.setSoTimeout(0);
						socket.receive(packet);
//						Logger.d(VoIPConstants.TAG, "Received something.");
						totalBytesReceived += packet.getLength();
						totalPacketsReceived++;
						
					} catch (IOException e) {
						Logger.e(VoIPConstants.TAG, "startReceiving() IOException: " + e.toString());
						break;
					}
					
					byte[] realData = new byte[packet.getLength()];
					System.arraycopy(packet.getData(), 0, realData, 0, packet.getLength());
					VoIPDataPacket dataPacket = getPacketFromUDPData(realData);
					
					if (dataPacket == null)
						continue;
//					Logger.w(VoIPConstants.TAG, "Received datapacket: " + dataPacket.getType());
					
					// ACK tracking
					if (dataPacket.getType() != PacketType.ACK)
						markPacketReceived(dataPacket.getPacketNumber());

					// ACK response
					if (dataPacket.isRequiresAck() == true) {
						VoIPDataPacket dp = new VoIPDataPacket(PacketType.ACK);
						dp.setPacketNumber(dataPacket.getPacketNumber());
						sendPacket(dp, false);
					}
					
					// Latency tracking
					if (dataPacket.getTimestamp() > 0) {
					}
					
					if (dataPacket.getType() == null) {
						Logger.w(VoIPConstants.TAG, "Unknown packet type.");
						continue;
					}
					
					lastHeartbeat = System.currentTimeMillis();

					switch (dataPacket.getType()) {
					case COMM_UDP_SYN_PRIVATE:
						Logger.d(VoIPConstants.TAG, "Received " + dataPacket.getType());
						synchronized (clientPartner) {
							ConnectionMethods currentMethod = clientPartner.getPreferredConnectionMethod();
							clientPartner.setPreferredConnectionMethod(ConnectionMethods.PRIVATE);
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_SYNACK_PRIVATE);
							sendPacket(dp, false);
							clientPartner.setPreferredConnectionMethod(currentMethod);
						}
						break;
						
					case COMM_UDP_SYN_PUBLIC:
						Logger.d(VoIPConstants.TAG, "Received " + dataPacket.getType());
						synchronized (clientPartner) {
							ConnectionMethods currentMethod = clientPartner.getPreferredConnectionMethod();
							clientPartner.setPreferredConnectionMethod(ConnectionMethods.PUBLIC);
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_SYNACK_PUBLIC);
							sendPacket(dp, false);
							clientPartner.setPreferredConnectionMethod(currentMethod);
						}
						break;
						
					case COMM_UDP_SYN_RELAY:
						Logger.d(VoIPConstants.TAG, "Received " + dataPacket.getType());
						
						synchronized (clientPartner) {
							ConnectionMethods currentMethod = clientPartner.getPreferredConnectionMethod();
							clientPartner.setPreferredConnectionMethod(ConnectionMethods.RELAY);
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_SYNACK_RELAY);
							sendPacket(dp, false);
							clientPartner.setPreferredConnectionMethod(currentMethod);
						}
						break;
						
					case COMM_UDP_SYNACK_PRIVATE:
					case COMM_UDP_ACK_PRIVATE:
						Logger.d(VoIPConstants.TAG, "Received " + dataPacket.getType());
						synchronized (clientPartner) {
							if (senderThread != null)
								senderThread.interrupt();
							clientPartner.setPreferredConnectionMethod(ConnectionMethods.PRIVATE);
							if (connected) break;

							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_ACK_PRIVATE);
							sendPacket(dp, true);
						}
						connected = true;
						break;
						
					case COMM_UDP_SYNACK_PUBLIC:
					case COMM_UDP_ACK_PUBLIC:
						Logger.d(VoIPConstants.TAG, "Received " + dataPacket.getType());
						synchronized (clientPartner) {
							if (senderThread != null)
								senderThread.interrupt();
							clientPartner.setPreferredConnectionMethod(ConnectionMethods.PUBLIC);
							if (connected) break;
							
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_ACK_PUBLIC);
							sendPacket(dp, true);
						}
						connected = true;
						break;
						
					case COMM_UDP_SYNACK_RELAY:
					case COMM_UDP_ACK_RELAY:
						Logger.d(VoIPConstants.TAG, "Received " + dataPacket.getType());
						synchronized (clientPartner) {
							if (clientPartner.getPreferredConnectionMethod() == ConnectionMethods.PRIVATE || 
									clientPartner.getPreferredConnectionMethod() == ConnectionMethods.PUBLIC) {
								Logger.d(VoIPConstants.TAG, "Ignoring " + dataPacket.getType() + " since we are expecting a " +
										clientPartner.getPreferredConnectionMethod() + " connection.");
								break;
							}
							if (senderThread != null)
								senderThread.interrupt();
							clientPartner.setPreferredConnectionMethod(ConnectionMethods.RELAY);
							if (connected) break;

							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_ACK_RELAY);
							sendPacket(dp, true);
						}
						connected = true;
						break;
						
					case VOICE_PACKET:
						qualityCounter++;
						if (dataPacket.isEncrypted()) {
							byte[] encryptedData = dataPacket.getData();
							dataPacket.write(encryptor.aesDecrypt(encryptedData));
							dataPacket.setEncrypted(false);
						}
						
						synchronized (samplesToDecodeQueue) {
							samplesToDecodeQueue.add(dataPacket);
							samplesToDecodeQueue.notify();
						}
						break;
						
					case HEARTBEAT:
						// Logger.d(VoIPConstants.TAG, "Received heartbeat.");
						lastHeartbeat = System.currentTimeMillis();
						
						// Mostly redundant check to ensure that neither of the phones
						// is playing the reconnecting tone
						if (reconnectingBeepsThread != null) {
							reconnectingBeepsThread.interrupt();
							reconnectingBeepsThread = null;
						}
						
						break;
						
					case ACK:
						removePacketFromAckWaitQueue(dataPacket.getPacketNumber());
						break;
						
					case ENCRYPTION_PUBLIC_KEY:
						if (clientPartner.isInitiator() != true) {
							Logger.e(VoIPConstants.TAG, "Was not expecting a public key.");
							continue;
						}
						Logger.d(VoIPConstants.TAG, "Received public key.");
						encryptor.setPublicKey(dataPacket.getData());
						encryptionStage = EncryptionStage.STAGE_GOT_PUBLIC_KEY;
						exchangeCryptoInfo();
						break;
						
					case ENCRYPTION_SESSION_KEY:
						if (clientPartner.isInitiator() == true) {
							Logger.e(VoIPConstants.TAG, "Was not expecting a session key.");
							continue;
						}
						encryptor.setSessionKey(encryptor.rsaDecrypt(dataPacket.getData()));
						Logger.d(VoIPConstants.TAG, "Received session key.");
						encryptionStage = EncryptionStage.STAGE_GOT_SESSION_KEY;
						exchangeCryptoInfo();
						break;
						
					case ENCRYPTION_RECEIVED_SESSION_KEY:
						Logger.d(VoIPConstants.TAG, "Encryption ready. MD5: " + encryptor.getSessionMD5());
						encryptionStage = EncryptionStage.STAGE_READY;
						break;
						
					case END_CALL:
						Logger.d(VoIPConstants.TAG, "Other party hung up.");
						clientPartner.setEnder(true);
						stop();
						VoIPUtils.addMessageToChatThread(getApplicationContext(), clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_CALL_SUMMARY, getCallDuration(), -1, true);
						break;
						
					case START_VOICE:
						startRecordingAndPlayback();
						break;
						
					case CALL_DECLINED:
						clientPartner.setEnder(true);
						sendHandlerMessage(VoIPConstants.MSG_OUTGOING_CALL_DECLINED);
						stop();
						break;
						
					case CURRENT_BITRATE:
						remoteBitrate = ByteBuffer.wrap(dataPacket.getData()).order(ByteOrder.LITTLE_ENDIAN).getInt();
						setIdealBitrate();
						break;

					case NETWORK_QUALITY:
						networkQualityPacketsReceived++;
						break;
					default:
						Logger.w(VoIPConstants.TAG, "Received unexpected packet: " + dataPacket.getType());
						break;
						
					case HOLD_ON:
						setRemoteHold(true);
						break;
						
					case HOLD_OFF:
						setRemoteHold(false);
						break;
						
					}
				}
			}
		}, "VOIP_RECEIVE_THREAD");
		
		receivingThread.start();
	}
	
	private void sendPacket(VoIPDataPacket dp, boolean requiresAck) {
		
		if (dp == null || keepRunning == false)
			return;
		
		if (socket == null) {
			Logger.d(VoIPConstants.TAG, "Socket is null.");
			return;
		}
		
		// While reconnecting don't send anything except for connection setup packets
		if (reconnecting) {
			if (!dp.getPacketType().toString().startsWith("COMM") && dp.getPacketType() != PacketType.RELAY_INIT)
				return;
		}
		
		if (clientPartner.getPreferredConnectionMethod() == ConnectionMethods.RELAY) {
			dp.setDestinationIP(clientPartner.getExternalIPAddress());
			dp.setDestinationPort(clientPartner.getExternalPort());
		}
		
		if (dp.getType() != PacketType.ACK && dp.getPacketNumber() == 0)
			dp.setPacketNumber(currentPacketNumber++);
		
		if (dp.getType() == PacketType.VOICE_PACKET)
			rawVoiceSent += dp.getLength();
		
		dp.setRequiresAck(requiresAck);
		dp.setTimestamp(System.currentTimeMillis());
		
		if (requiresAck == true)
			addPacketToAckWaitQueue(dp);

//		Logger.w(VoIPConstants.TAG, "Sending type: " + dp.getType());
		
		// Serialize everything except for P2P voice data packets
		byte[] packetData = getUDPDataFromPacket(dp);
		
		if (packetData == null)
			return;
		
		try {
			DatagramPacket packet = null;
			if (dp.getType() == PacketType.RELAY_INIT)
				packet = new DatagramPacket(packetData, packetData.length, InetAddress.getByName(clientPartner.getRelayAddress()), clientPartner.getRelayPort());
			else
				packet = new DatagramPacket(packetData, packetData.length, clientPartner.getCachedInetAddress(), clientPartner.getPreferredPort());
				
//			Logger.d(VoIPConstants.TAG, "Sending type: " + dp.getType() + " to: " + packet.getAddress() + ":" + packet.getPort());
			socket.send(packet);
			totalBytesSent += packet.getLength();
			totalPacketsSent++;
		} catch (IOException e) {
			Logger.w(VoIPConstants.TAG, "sendPacket() IOException: " + e.toString());
		}
		
	}
	
	private byte[] getUDPDataFromPacket(VoIPDataPacket dp) {
		
		// Serialize everything except for P2P voice data packets
		byte[] packetData = null;
		byte prefix;
		
		if (dp.getType() == PacketType.VOICE_PACKET && clientPartner.getPreferredConnectionMethod() != ConnectionMethods.RELAY) {
			packetData = dp.getData();
			if (dp.isEncrypted()) {
				prefix = PP_ENCRYPTED_VOICE_PACKET;
			} else {
				prefix = PP_RAW_VOICE_PACKET;
			}
		} else {
			packetData = VoIPSerializer.serialize(dp);
			prefix = PP_PROTOCOL_BUFFER;
		}
		
		if (packetData == null)
			return null;
		
		byte[] finalData = new byte[packetData.length + 1];	
		finalData[0] = prefix;
		System.arraycopy(packetData, 0, finalData, 1, packetData.length);
		packetData = finalData;

		return packetData;
	}
	
	private VoIPDataPacket getPacketFromUDPData(byte[] data) {
		VoIPDataPacket dp = null;
		byte prefix = data[0];
		byte[] packetData = new byte[data.length - 1];
		System.arraycopy(data, 1, packetData, 0, packetData.length);

//		Logger.w(VoIPConstants.TAG, "Prefix: " + prefix);
		if (prefix == PP_PROTOCOL_BUFFER) {
			dp = (VoIPDataPacket) VoIPSerializer.deserialize(packetData);
		} else {
			dp = new VoIPDataPacket(PacketType.VOICE_PACKET);
			dp.setData(packetData);
			if (prefix == PP_ENCRYPTED_VOICE_PACKET)
				dp.setEncrypted(true);
			else
				dp.setEncrypted(false);
		}
		
		return dp;
	}
	
	private void addPacketToAckWaitQueue(VoIPDataPacket dp) {
		synchronized (ackWaitQueue) {
			if (ackWaitQueue.containsKey(dp.getPacketNumber()))
				return;

			ackWaitQueue.put(dp.getPacketNumber(), dp);
		}
	}
	
	private void markPacketReceived(int packetNumber) {
		if (packetNumber > previousHighestRemotePacketNumber) {
			// New highest packet received
			// Set all bits between this and previous highest packet to zero
			int mod1 = packetNumber % PACKET_TRACKING_SIZE;
			int mod2 = previousHighestRemotePacketNumber % PACKET_TRACKING_SIZE;
			if (mod1 > mod2)
				packetTrackingBits.clear(mod2 + 1, mod1);
			else {
				if (mod2 + 1 < PACKET_TRACKING_SIZE - 1)
					packetTrackingBits.clear(mod2 + 1, PACKET_TRACKING_SIZE - 1);
				packetTrackingBits.clear(0, mod1);
			}
			previousHighestRemotePacketNumber = packetNumber;
		}
		
		// Mark packet as received
		int mod = packetNumber % PACKET_TRACKING_SIZE;
		packetTrackingBits.set(mod);
	}
	
	private void sendPacketsWaitingForAck() {
		if (ackWaitQueue.isEmpty() || !connected)
			return;
		
		synchronized (ackWaitQueue) {
			Iterator<Integer> iterator = ackWaitQueue.keySet().iterator();;
			long currentTime = System.currentTimeMillis();

			while (iterator.hasNext()) {
				Integer i = iterator.next();
				if (ackWaitQueue.get(i).getTimestamp() < currentTime - 1000) {	// Give each packet 1 second to get ack
					Logger.d(VoIPConstants.TAG, "Re-Requesting ack for: " + ackWaitQueue.get(i).getType());
					sendPacket(ackWaitQueue.get(i), true);
				}
			}
		}		
	}
	
	private void removePacketFromAckWaitQueue(int packetNumber) {
		synchronized (ackWaitQueue) {
			ackWaitQueue.remove(packetNumber);
		}
	}
	
	private synchronized void exchangeCryptoInfo() {

		if (cryptoEnabled == false)
			return;

		new Thread(new Runnable() {
			
			@Override
			public void run() {
				if (encryptionStage == EncryptionStage.STAGE_INITIAL && clientPartner.isInitiator() != true) {
					// The initiator (caller) generates and sends a public key
					encryptor.initKeys();
					VoIPDataPacket dp = new VoIPDataPacket(PacketType.ENCRYPTION_PUBLIC_KEY);
					dp.write(encryptor.getPublicKey());
					sendPacket(dp, true);
					Logger.d(VoIPConstants.TAG, "Sending public key.");
				}

				if (encryptionStage == EncryptionStage.STAGE_GOT_PUBLIC_KEY && clientPartner.isInitiator() == true) {
					// Generate and send the AES session key
					encryptor.initSessionKey();
					byte[] encryptedSessionKey = encryptor.rsaEncrypt(encryptor.getSessionKey(), encryptor.getPublicKey());
					VoIPDataPacket dp = new VoIPDataPacket(PacketType.ENCRYPTION_SESSION_KEY);
					dp.write(encryptedSessionKey);
					sendPacket(dp, true);
					Logger.d(VoIPConstants.TAG, "Sending AES key.");
				}

				if (encryptionStage == EncryptionStage.STAGE_GOT_SESSION_KEY) {
					VoIPDataPacket dp = new VoIPDataPacket(PacketType.ENCRYPTION_RECEIVED_SESSION_KEY);
					sendPacket(dp, true);
					encryptionStage = EncryptionStage.STAGE_READY;
					Logger.d(VoIPConstants.TAG, "Encryption ready. MD5: " + encryptor.getSessionMD5());
				}
			}
		}, "EXCHANGE_CRYPTO_THREAD").start();
	}

	public int adjustBitrate(int delta) {
		if (delta > 0 && localBitrate + delta < 64000)
			localBitrate += delta;
		if (delta < 0 && localBitrate + delta >= 3000)
			localBitrate += delta;
		
		if (opusWrapper == null)
			return localBitrate;
		
		opusWrapper.setEncoderBitrate(localBitrate);
		sendHandlerMessage(VoIPConstants.MSG_CURRENT_BITRATE);
		
		return localBitrate;
	}
	
	public int getBitrate() {
		return localBitrate;
	}
	
	/**
	 * Is the VoIP service currently connected to another phone?
	 * This can return <b>false</b> even for an ongoing call, in case
	 * a reconnection is being attempted. To check if we are current in call, 
	 * use getCallId() instead.  
	 * 
	 * @return
	 */
	public static boolean isConnected() {
		return connected;
	}
	
	public static boolean isAudioRunning() {
		return audioStarted;
	}
	
	public void setHold(boolean newHold) {
		
		Logger.d(VoIPConstants.TAG, "Changing hold to: " + newHold + " from: " + this.hold);

		if (this.hold == newHold)
			return;
		
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

		setCallStatus(!hold && !remoteHold ? VoIPConstants.CallStatus.ACTIVE : VoIPConstants.CallStatus.ON_HOLD);
		sendHandlerMessage(VoIPConstants.MSG_UPDATE_HOLD_BUTTON);
		
		// Send hold status to partner
		sendHoldStatus();
	}	

	public boolean getHold()
	{
		return hold;
	}
	
	private void setRemoteHold(boolean newHold) {
		
		if (remoteHold == newHold)
			return;

		remoteHold = newHold;
		setCallStatus(!hold && !remoteHold ? VoIPConstants.CallStatus.ACTIVE : VoIPConstants.CallStatus.ON_HOLD);
		sendHandlerMessage(VoIPConstants.MSG_UPDATE_REMOTE_HOLD);
	}

	private void sendHoldStatus() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				VoIPDataPacket dp = null;
				if (hold == true)
					dp = new VoIPDataPacket(PacketType.HOLD_ON);
				else
					dp = new VoIPDataPacket(PacketType.HOLD_OFF);
				sendPacket(dp, true);
			}
		}).start();
	}
	
	public String getSessionKeyHash() {
		String hash = null;
		if (encryptor != null)
			hash = encryptor.getSessionMD5();
		return hash;
	}

	public void setSpeaker(boolean speaker)
	{
		this.speaker = speaker;
		if(audioManager!=null)
		{
			audioManager.setSpeakerphoneOn(speaker);
			// Logger.d(VoIPConstants.TAG, "Speaker set to: " + speaker);
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
		synchronized (this) {
			
			if (reconnecting || audioStarted)
				return;
			
			if (isRingingOutgoing == true) {
				Logger.w(VoIPConstants.TAG, "Outgoing ringer is already ringing.");
				return;
			} else isRingingOutgoing = true;

			Logger.d(VoIPConstants.TAG, "Playing outgoing call ringer.");
			setCallStatus(VoIPConstants.CallStatus.OUTGOING_RINGING);
			sendHandlerMessage(VoIPConstants.CONNECTION_ESTABLISHED_FIRST_TIME);
			sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_ESTABLISHED);
			setAudioModeInCall();
			ringtoneStreamID = playFromSoundPool(SOUND_INCOMING_RINGTONE, true);
		}
	}
	
	@SuppressWarnings("deprecation")
	private void playIncomingCallRingtone() {

		if (reconnecting || audioStarted || keepRunning == false)
			return;

		synchronized (this) {
			
			if (isRingingIncoming == true)
				return;
			else isRingingIncoming = true;
			
			// Show activity
			Intent intent = IntentFactory.getVoipIncomingCallIntent(VoIPService.this);
			startActivity(intent);

			// Ringer
			Logger.d(VoIPConstants.TAG, "Playing ringtone.");
			Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
			if (ringtone == null)
				ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
			ringtone.setStreamType(AudioManager.STREAM_RING);
			ringtone.play();		

			// Vibrator
			if (vibratorEnabled == true) {
				vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
				if (vibrator != null) {
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
				if (ringtone != null && ringtone.isPlaying())
				{
					ringtone.stop();
					ringtone = null;
				}
			} catch (IllegalStateException e) {
				Logger.w(VoIPConstants.TAG, "stopRingtone() IllegalStateException: " + e.toString());
			}

			// stop vibrating
			if (vibrator != null) {
				vibrator.cancel();
				vibrator = null;
			}
			
			isRingingIncoming = false;
		}
	}
	
	public CallQuality getQuality() {
		return currentCallQuality;
	}
	
	public void retrieveExternalSocket() {

		keepRunning = true;
		
		Thread iceThread = new Thread(new Runnable() {

			@Override
			public void run() {

				removeExternalSocketInfo();
				
				byte[] receiveData = new byte[10240];
				
				try {
					Logger.d(VoIPConstants.TAG, "Retrieving external socket information..");
					VoIPDataPacket dp = new VoIPDataPacket(PacketType.RELAY_INIT);
					DatagramPacket incomingPacket = new DatagramPacket(receiveData, receiveData.length);

					boolean continueSending = true;
					int counter = 0;

					socket = new DatagramSocket();
					socket.setReuseAddress(true);
					socket.setSoTimeout(2000);

					while (continueSending && keepRunning && (counter < 10 || reconnecting)) {
						counter++;
						try {
							InetAddress host = InetAddress.getByName(VoIPConstants.ICEServerName);
							
							/**
							 * If we are initiating the connection, then we set the relay server
							 * to be used by both clients. 
							 */
							if (!clientPartner.isInitiator()) {
								clientPartner.setRelayAddress(host.getHostAddress());
								clientPartner.setRelayPort(VoIPUtils.getRelayPort(getApplicationContext()));
							}

							clientPartner.setOurInternalIPAddress(VoIPUtils.getLocalIpAddress(getApplicationContext())); 
							clientPartner.setOurInternalPort(socket.getLocalPort());

							Logger.d(VoIPConstants.TAG, "ICE Sending.");
							sendPacket(dp, false);
							socket.receive(incomingPacket);
							
							String serverResponse = new String(incomingPacket.getData(), 0, incomingPacket.getLength());
							Logger.d(VoIPConstants.TAG, "ICE Received: " + serverResponse);
							setExternalSocketInfo(serverResponse);
							continueSending = false;
							
						} catch (SocketTimeoutException e) {
							Logger.d(VoIPConstants.TAG, "UDP timeout on ICE. #" + counter);
						} catch (IOException e) {
							Logger.d(VoIPConstants.TAG, "retrieveExternalSocket() IOException" + e.toString());
							try {
								Thread.sleep(500);
							} catch (InterruptedException e1) {
								Logger.d(VoIPConstants.TAG, "Waiting for external socket info interrupted.");
							}
						} catch (JSONException e) {
							Logger.d(VoIPConstants.TAG, "JSONException: " + e.toString());
							continueSending = true;
						}
					}

				} catch (SocketException e2) {
					Logger.d(VoIPConstants.TAG, "retrieveExternalSocket() IOException2: " + e2.toString());
				}
				
				if (haveExternalSocketInfo()) {
					if (clientPartner.isInitiator() || reconnecting || isNetworkGoodEnough()) {
						sendSocketInfoToPartner();
						if (socketInfoReceived)
							establishConnection();
						else
							startPartnerTimeoutThread();
					} else {
						if (keepRunning) {
							Logger.w(VoIPConstants.TAG, "Network is not good enough.");
							sendHandlerMessage(VoIPConstants.MSG_NETWORK_SUCKS);
							sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.CALLER_BAD_NETWORK);
						}
						stop();
						return;
					}
				} else {
					Logger.d(VoIPConstants.TAG, "Failed to retrieve external socket.");
					sendHandlerMessage(VoIPConstants.MSG_EXTERNAL_SOCKET_RETRIEVAL_FAILURE);
					sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.EXTERNAL_SOCKET_RETRIEVAL_FAILURE);
					stop();
				}
			}
		}, "ICE_THREAD");
		
		iceThread.start();
		
	}

	private void setExternalSocketInfo(String ICEResponse) throws JSONException {
		JSONObject jsonObject = new JSONObject(ICEResponse);
		clientPartner.setOurExternalIPAddress(jsonObject.getString("IP"));
		clientPartner.setOurExternalPort(Integer.parseInt(jsonObject.getString("Port")));
		Logger.d(VoIPConstants.TAG, "External socket - " + clientPartner.getOurExternalIPAddress() + ":" + clientPartner.getOurExternalPort());
		Logger.d(VoIPConstants.TAG, "Internal socket - " + clientPartner.getOurInternalIPAddress() + ":" + clientPartner.getOurInternalPort());
	}
	
	private boolean haveExternalSocketInfo() {
		if (clientPartner.getOurExternalIPAddress() != null && 
				!clientPartner.getOurExternalIPAddress().isEmpty() && 
				clientPartner.getOurExternalPort() > 0)
			return true;
		else
			return false;
	}
	
	private void removeExternalSocketInfo() {
		clientPartner.setOurExternalIPAddress(null);
		clientPartner.setOurExternalPort(0);
		if (socket != null) {
			socket.close();
			socket = null;
		}
	}
	
	private void sendSocketInfoToPartner() {
		if (clientPartner.getPhoneNumber() == null || clientPartner.getPhoneNumber().isEmpty()) {
			Logger.e(VoIPConstants.TAG, "Have no partner info. Quitting.");
			return;
		}

		if (!haveExternalSocketInfo()) {
			Logger.d(VoIPConstants.TAG, "Can't send socket info (don't have it!)");
			return;
		}
		
		try {
			JSONObject socketData = new JSONObject();
			socketData.put("internalIP", clientPartner.getOurInternalIPAddress());
			socketData.put("internalPort", clientPartner.getOurInternalPort());
			socketData.put("externalIP", clientPartner.getOurExternalIPAddress());
			socketData.put("externalPort", clientPartner.getOurExternalPort());
			socketData.put("relay", clientPartner.getRelayAddress());
			socketData.put("relayport", clientPartner.getRelayPort());
			socketData.put("callId", getCallId());
			socketData.put("initiator", !clientPartner.isInitiator());
			socketData.put("reconnecting", reconnecting);
			
			JSONObject data = new JSONObject();
			data.put(HikeConstants.MESSAGE_ID, new Random().nextInt(10000));
			data.put(HikeConstants.TIMESTAMP, System.currentTimeMillis() / 1000); 
			data.put(HikeConstants.METADATA, socketData);

			JSONObject message = new JSONObject();
			message.put(HikeConstants.TO, clientPartner.getPhoneNumber());
			message.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE_VOIP_0);
			message.put(HikeConstants.SUB_TYPE, HikeConstants.MqttMessageTypes.VOIP_SOCKET_INFO);
			message.put(HikeConstants.DATA, data);
			
			HikeMqttManagerNew.getInstance().sendMessage(message, MqttConstants.MQTT_QOS_ONE);
			Logger.d(VoIPConstants.TAG, "Sent socket information to partner. Reconnecting: " + reconnecting);
			socketInfoSent = true;

		} catch (JSONException e) {
			e.printStackTrace();
			Logger.w(VoIPConstants.TAG, "sendSocketInfoToPartner JSON error: " + e.toString());
		} 
		
	}
	
	private void startPartnerTimeoutThread() {
		// Wait for partner to send us their socket information
		// Set timeout so we don't wait indefinitely
		
		final int numLoop = 5;
		partnerTimeoutThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				for (int i = 0; i < numLoop || reconnecting; i++) {
					try {
						Thread.sleep(VoIPConstants.TIMEOUT_PARTNER_SOCKET_INFO / numLoop);
						sendSocketInfoToPartner();		// Retry sending socket info. 
					} catch (InterruptedException e) {
						Logger.d(VoIPConstants.TAG, "Timeout thread interrupted.");
						return;
					}
				}

				sendHandlerMessage(VoIPConstants.MSG_PARTNER_SOCKET_INFO_TIMEOUT);
				if (!clientPartner.isInitiator() && !reconnecting) {
					VoIPUtils.addMessageToChatThread(VoIPService.this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_OUTGOING, 0, -1, false);
					VoIPUtils.sendMissedCallNotificationToPartner(clientPartner);
				}
				sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.PARTNER_SOCKET_INFO_TIMEOUT);
				stop();					
			}
		}, "PARTNER_TIMEOUT_THREAD");
		
		partnerTimeoutThread.start();
	}
	
	/**
	 * Once socket information for the partner has been received, this
	 * function should be called to establish and verify a UDP connection.
	 */
	public void establishConnection() {
		
		if (establishingConnection) {
			Logger.w(VoIPConstants.TAG, "Already trying to establish connection.");
			return;
		}
		
		if (socket == null) {
			Logger.w(VoIPConstants.TAG, "establishConnection() called with null socket.");
			return;
		}
		
		clientPartner.setPreferredConnectionMethod(ConnectionMethods.UNKNOWN);
		establishingConnection = true;
		connected = false;
		if (partnerTimeoutThread != null)
			partnerTimeoutThread.interrupt();
		Logger.d(VoIPConstants.TAG, "Trying to establish P2P connection..");
		
		// Sender thread
		senderThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (keepRunning) {
					if (Thread.currentThread().isInterrupted())
						break;

					try {
						VoIPDataPacket dp = null;
						synchronized (clientPartner) {
							ConnectionMethods currentMethod = clientPartner.getPreferredConnectionMethod();
							clientPartner.setPreferredConnectionMethod(ConnectionMethods.PRIVATE);
							dp = new VoIPDataPacket(PacketType.COMM_UDP_SYN_PRIVATE);
							sendPacket(dp, false);
							clientPartner.setPreferredConnectionMethod(ConnectionMethods.PUBLIC);
							dp = new VoIPDataPacket(PacketType.COMM_UDP_SYN_PUBLIC);
							sendPacket(dp, false);
							clientPartner.setPreferredConnectionMethod(ConnectionMethods.RELAY);
							dp = new VoIPDataPacket(PacketType.COMM_UDP_SYN_RELAY);
							sendPacket(dp, false);
							clientPartner.setPreferredConnectionMethod(currentMethod);
						}
						Thread.sleep(250);
					} catch (InterruptedException e) {
						Logger.d(VoIPConstants.TAG, "Stopping sending thread.");
						break;
					}
				}
			}
		}, "SENDER_THREAD");
		
		startReceiving();
		senderThread.start();
		
		// Monitoring / timeout thread
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					for (int i = 0; i < 20; i++) {
						if (connected == true) {
							break;
						}
						Thread.sleep(500);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (senderThread != null)
					senderThread.interrupt();
				establishingConnection = false;
				if (reconnectingBeepsThread != null)
					reconnectingBeepsThread.interrupt();
				
				if (connected == true) {
					Logger.d(VoIPConstants.TAG, "UDP connection established :) " + clientPartner.getPreferredConnectionMethod());
					
					if (!clientPartner.isInitiator()) 
						playOutgoingCallRingtone();
					else
						playIncomingCallRingtone();

					if (reconnecting) {
						setInitialCallStatus();
						sendHandlerMessage(VoIPConstants.MSG_RECONNECTED);
						// Give the heartbeat a chance to recover
						lastHeartbeat = System.currentTimeMillis() + 5000;
						startSendingAndReceiving();
						reconnecting = false;
					} else {
						startStreaming();
						startResponseTimeout();
					}
				} else {
					Logger.d(VoIPConstants.TAG, "UDP connection failure! :(");
					sendHandlerMessage(VoIPConstants.MSG_CONNECTION_FAILURE);
					if (!reconnecting) {
						if (!clientPartner.isInitiator())
							VoIPUtils.addMessageToChatThread(VoIPService.this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_OUTGOING, 0, -1, false);
						else
							VoIPUtils.addMessageToChatThread(VoIPService.this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING, 0, -1, true);
					}
					sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.UDP_CONNECTION_FAIL);
					stop();
				}
			}
		}).start();

	}

	private void startResponseTimeout() {
		partnerTimeoutThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(VoIPConstants.TIMEOUT_PARTNER_ANSWER);
					if (!isAudioRunning()) {
						// Call not answered yet?
						if (connected) 
						{
							if (!clientPartner.isInitiator())
							{
								sendHandlerMessage(VoIPConstants.MSG_PARTNER_ANSWER_TIMEOUT);
								sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_PARTNER_ANSWER_TIMEOUT);
								VoIPUtils.addMessageToChatThread(VoIPService.this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_OUTGOING, 0, -1, false);
							}
							else
							{
								VoIPUtils.addMessageToChatThread(VoIPService.this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING, 0, -1, true);
							}
						}
						stop();
						
					}
				} catch (InterruptedException e) {
					// Do nothing, all is good
				}
			}
		}, "PARTNER_TIMEOUT_THREAD");
		
		partnerTimeoutThread.start();
	}

	private boolean isNetworkGoodEnough() {
		boolean good = false;

		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.VOIP_NETWORK_TEST_ENABLED, false) == false) {
			Logger.d(VoIPConstants.TAG, "Network test disabled.");
			return true;
		}
		
		SharedPreferences prefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		int simulateBitrate = prefs.getInt(HikeMessengerApp.VOIP_BITRATE_2G, VoIPConstants.BITRATE_2G);

		final int TOTAL_TEST_TIME = VoIPUtils.getQualityTestSimulatedCallDuration(getApplicationContext());
		final int TOTAL_TEST_BYTES = (simulateBitrate * TOTAL_TEST_TIME) / 8;
		final int TEST_PACKETS = TOTAL_TEST_TIME * 15;	// Assuming ~15 packets per second
		final int TIME_TO_WAIT_FOR_PACKETS = 1;
		final int ACCEPTABLE_LOSS_PCT = VoIPUtils.getQualityTestAcceptablePacketLoss(getApplicationContext());

		byte[] packetData = new byte[TOTAL_TEST_BYTES / TEST_PACKETS];
		VoIPDataPacket dp = new VoIPDataPacket(PacketType.NETWORK_QUALITY);
		dp.setData(packetData);
		startReceiving();
		
//		clientPartner.setRelayAddress(clientSelf.getRelayAddress());
//		clientPartner.setRelayPort(clientSelf.getRelayPort());
		clientPartner.setPreferredConnectionMethod(ConnectionMethods.RELAY);

		networkQualityPacketsReceived = 0;

		Logger.d(VoIPConstants.TAG, "Testing network with " + TEST_PACKETS + " packets of " + packetData.length + " bytes over " + TOTAL_TEST_TIME + " seconds.");
		
		try {
			for (int i = 0; i < TEST_PACKETS; i++) {
				sendPacket(dp, false);
				Thread.sleep((TOTAL_TEST_TIME * 1000) / TEST_PACKETS);
				if (Thread.currentThread().isInterrupted())
					break;
			}
			Thread.sleep(TIME_TO_WAIT_FOR_PACKETS * 1000);
		} catch (InterruptedException e) {
			Logger.d(VoIPConstants.TAG, "Network test cancelled.");
		}

		if (networkQualityPacketsReceived > ((100 - ACCEPTABLE_LOSS_PCT) * TEST_PACKETS)/100)
			good = true;

		Logger.d(VoIPConstants.TAG, "Network test returned " + networkQualityPacketsReceived 
				+ " packets. Sent: " + TEST_PACKETS + ". Verdict: " + good);

		// Reset the preferred connection method
		clientPartner.setPreferredConnectionMethod(ConnectionMethods.UNKNOWN);
		
		return good;
	}

	public void setCallStatus(VoIPConstants.CallStatus status)
	{
		currentCallStatus = status;
	}

	public VoIPConstants.CallStatus getCallStatus()
	{
		return currentCallStatus;
	}

	private void setInitialCallStatus()
	{
		if(isAudioRunning())
		{
			setCallStatus(VoIPConstants.CallStatus.ACTIVE);
		}
		else
		{
			setCallStatus(clientPartner.isInitiator() ? VoIPConstants.CallStatus.INCOMING_CALL : VoIPConstants.CallStatus.OUTGOING_CONNECTING);
		}
	}

	public void sendAnalyticsEvent(String ek)
	{
		sendAnalyticsEvent(ek, -1);
	}

	public void sendAnalyticsEvent(String ek, int value)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_TYPE, HikeConstants.LogEvent.VOIP);
			metadata.put(HikeConstants.EVENT_KEY, ek);
			metadata.put(VoIPConstants.Analytics.IS_CALLER, clientPartner.isInitiator() ? 0 : 1);
			metadata.put(VoIPConstants.Analytics.CALL_ID, getCallId());
			metadata.put(VoIPConstants.Analytics.NETWORK_TYPE, VoIPUtils.getConnectionClass(getApplicationContext()).ordinal());
			
			String toMsisdn = clientPartner.getPhoneNumber();
			
			if(!TextUtils.isEmpty(toMsisdn))
			{
				metadata.put(AnalyticsConstants.TO, toMsisdn);
			}

			if(ek.equals(HikeConstants.LogEvent.VOIP_CALL_CLICK))
			{
				metadata.put(VoIPConstants.Analytics.CALL_SOURCE, callSource);
			}
			else if(ek.equals(HikeConstants.LogEvent.VOIP_CALL_END) || ek.equals(HikeConstants.LogEvent.VOIP_CALL_DROP) ||
					ek.equals(HikeConstants.LogEvent.VOIP_CALL_REJECT) || ek.equals(HikeConstants.LogEvent.VOIP_PARTNER_ANSWER_TIMEOUT))
			{
				metadata.put(VoIPConstants.Analytics.DATA_SENT, totalBytesSent);
				metadata.put(VoIPConstants.Analytics.DATA_RECEIVED, totalBytesReceived);
				metadata.put(VoIPConstants.Analytics.IS_ENDER, clientPartner.isEnder() ? 0 : 1);
				if(getCallDuration() > 0)
				{
					metadata.put(VoIPConstants.Analytics.DURATION, getCallDuration());
				}
			}
			else if(ek.equals(HikeConstants.LogEvent.VOIP_CALL_SPEAKER) || ek.equals(HikeConstants.LogEvent.VOIP_CALL_HOLD) || ek.equals(HikeConstants.LogEvent.VOIP_CALL_MUTE))
			{
				metadata.put(VoIPConstants.Analytics.STATE, value);
			}
			else if(ek.equals(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED))
			{
				metadata.put(VoIPConstants.Analytics.CALL_CONNECT_FAIL_REASON, value);
			}

			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
		}
		catch (JSONException e)
		{
			Logger.w(AnalyticsConstants.ANALYTICS_TAG, "Invalid json");
		}
	}

	@Override
	public void onEventReceived(String type, Object object) 
	{
		if(HikePubSub.STOP_VOIP_SERVICE.equals(type))
		{
			stop();
		}
	}
}

