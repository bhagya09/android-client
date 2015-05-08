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
import android.os.Handler;
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

public class VoIPService extends Service {
	
	private final IBinder myBinder = new LocalBinder();
	private static final int NOTIFICATION_IDENTIFIER = 10;

	private Messenger mMessenger;
	private volatile boolean keepRunning = true;
	private VoIPClient clientPartner = null;
	private boolean mute, hold, speaker, vibratorEnabled = true;
	private int droppedDecodedPackets = 0;
	private int minBufSizePlayback, minBufSizeRecording;
	private OpusWrapper opusWrapper;
	private Resampler resampler;
	private Thread connectionTimeoutThread = null;
	private Thread recordingThread = null, playbackThread = null, codecCompressionThread = null, codecDecompressionThread = null;
	private AudioTrack audioTrack = null;
	private static int callId = 0;
	private NotificationManager notificationManager;
	private NotificationCompat.Builder builder;
	private AudioManager audioManager;
	private int initialAudioMode, initialRingerMode;
	private boolean initialSpeakerMode;
	private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener;
	private int reconnectAttempts = 0;
	private int playbackSampleRate = 0;
	private int callSource = -1;
	private Thread notificationThread;

	// Ringtones (incoming and outgoing)
	private Ringtone ringtone;
	private Vibrator vibrator = null;
	private int ringtoneStreamID = 0;
	private boolean isRingingIncoming = false, isRingingOutgoing = false;

	// Sounds
	private SoundPool soundpool;
	private SparseIntArray soundpoolMap;
	private static final int SOUND_ACCEPT = R.raw.call_answer;
	private static final int SOUND_DECLINE = R.raw.call_end;
	private static final int SOUND_INCOMING_RINGTONE = R.raw.ring_tone;
	private static final int SOUND_RECONNECTING = R.raw.reconnect;

	// Network quality test
	private int networkQualityPacketsReceived = 0;

	private final ConcurrentLinkedQueue<VoIPDataPacket> samplesToEncodeQueue     = new ConcurrentLinkedQueue<VoIPDataPacket>();
	private final ConcurrentLinkedQueue<VoIPDataPacket> decodedBuffersQueue      = new ConcurrentLinkedQueue<VoIPDataPacket>();
	
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
	
	// Handler for messages from VoIP clients
	Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			Logger.w(VoIPConstants.TAG, "Received message to service handler: " + msg.what);
			switch (msg.what) {
			case VoIPConstants.MSG_VOIP_CLIENT_STOP:
				stop();
				break;
				
			case VoIPConstants.MSG_VOIP_CLIENT_OUTGOING_CALL_RINGTONE:
				playOutgoingCallRingtone();
				break;
				
			case VoIPConstants.MSG_VOIP_CLIENT_INCOMING_CALL_RINGTONE:
				playIncomingCallRingtone();
				break;
				
			case VoIPConstants.MSG_UPDATE_REMOTE_HOLD:
				clientPartner.setCallStatus(!hold && !clientPartner.remoteHold ? VoIPConstants.CallStatus.ACTIVE : VoIPConstants.CallStatus.ON_HOLD);
				sendHandlerMessage(VoIPConstants.MSG_UPDATE_REMOTE_HOLD);
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
	
	public void setClientPartner(VoIPClient clientPartner) {
		this.clientPartner = clientPartner;
	}
	
	@SuppressLint("InlinedApi") @Override
	public void onCreate() {
		super.onCreate();
		Logger.d(VoIPConstants.TAG, "VoIPService onCreate()");
		acquireWakeLock();
		
		clientPartner = new VoIPClient(getApplicationContext(), handler);
//		String myMsisdn = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(HikeMessengerApp.MSISDN_SETTING, null);

		setCallid(0);
		clientPartner.setCallStatus(VoIPConstants.CallStatus.UNINITIALIZED);
		initAudioManager();
		keepRunning = true;
		isRingingIncoming = false;
		
		if (resamplerEnabled)
			playbackSampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_VOICE_CALL);
		else
			playbackSampleRate = VoIPConstants.AUDIO_SAMPLE_RATE;
		
		Logger.d(VoIPConstants.TAG, "Native playback sample rate: " + AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_VOICE_CALL));

		if (android.os.Build.VERSION.SDK_INT >= 17) {
			String rate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
			String size = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
			Logger.d(VoIPConstants.TAG, "Device frames/buffer:" + size + ", sample rate: " + rate);
		}
		
		VoIPUtils.resetNotificationStatus();

		minBufSizePlayback = AudioTrack.getMinBufferSize(playbackSampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
		minBufSizeRecording = AudioRecord.getMinBufferSize(VoIPConstants.AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		
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
			clientPartner.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_HANDSHAKE_COMPLETE);

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
			if (clientPartner.socketInfoReceived && !partnerReconnecting) {
				Logger.d(VoIPConstants.TAG, "Repeat call initiation message.");
				// Try sending our socket info again. Caller could've missed our original message.
				if (!clientPartner.connected)
					clientPartner.sendSocketInfoToPartner();
				
				return returnInt;
			}
			
			// Check in case the other client is reconnecting to us
			if (clientPartner.connected && partnerCallId == getCallId() && partnerReconnecting) {
				Logger.w(VoIPConstants.TAG, "Partner trying to reconnect with us. CallId: " + getCallId());
				if (!clientPartner.reconnecting) {
					reconnect();
				} 
				if (clientPartner.socketInfoSent)
					clientPartner.establishConnection();
			} else {
				// All good. 
				setCallid(partnerCallId);
				if (clientPartner.isInitiator() && !clientPartner.reconnecting) {
					Logger.w(VoIPConstants.TAG, "Detected incoming VoIP call from: " + clientPartner.getPhoneNumber());
					clientPartner.retrieveExternalSocket();
				} else {
					// We have already sent our socket info to partner
					// And now they have sent us their's, so let's establish connection
					clientPartner.establishConnection();
				}
			}

			clientPartner.socketInfoReceived = true;
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
				sendHandlerMessage(VoIPConstants.MSG_ALREADY_IN_NATIVE_CALL);
				clientPartner.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.CALLER_IN_NATIVE_CALL);
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

			startNotificationThread();
			
			// Show activity
			Intent i = new Intent(getApplicationContext(), VoIPActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
			
			clientPartner.retrieveExternalSocket();
			clientPartner.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_CLICK);
		}

		if(clientPartner.getCallStatus() == VoIPConstants.CallStatus.UNINITIALIZED)
		{
			clientPartner.setInitialCallStatus();
		}

		return returnInt;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
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
					if (!clientPartner.connected) {
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
		
		notificationThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (keepRunning) {
					try {
						Thread.sleep(1000);
						if (keepRunning)
							showNotification();
					} catch (InterruptedException e) {
						Logger.d(VoIPConstants.TAG, "Notification thread interrupted.");
						break;
					}

				}
			}
		}, "NOTIFICATION_THREAD");

		notificationThread.start();
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

		int callDuration = clientPartner.getCallDuration();
		String durationString = (callDuration == 0)? "" : String.format(Locale.getDefault(), " (%02d:%02d)", (callDuration / 60), (callDuration % 60));

		String title = null;
		if (clientPartner.getName() == null)
			title = getString(R.string.voip_call_chat);
		else
			title = getString(R.string.voip_call_notification_title, clientPartner.getName()); 
		
		String text = null;
		switch (clientPartner.getCallStatus())
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
					if (clientPartner.getCallDuration() > 0 && hold == true)
						setHold(false);
					break;
				case AudioManager.AUDIOFOCUS_LOSS:
					Logger.w(VoIPConstants.TAG, "AUDIOFOCUS_LOSS");
					if (clientPartner.getCallDuration() > 0)
						setHold(true);
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
					Logger.d(VoIPConstants.TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
					if (clientPartner.getCallDuration() > 0)
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
	
	public ConnectionMethods getConnectionMethod() {
		return clientPartner.getPreferredConnectionMethod();
	}
	
	public void startChrono() {

		try {
			if (clientPartner.chronometer == null) {
//				Looper.prepare();
//				Logger.w(VoIPConstants.TAG, "Starting chrono..");
				clientPartner.chronometer = new Chronometer(VoIPService.this);
				clientPartner.chronometer.setBase(SystemClock.elapsedRealtime());
				clientPartner.chronometer.start();
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
				clientPartner.connected = false;
				setCallid(0);
				return;
			}
			keepRunning = false;
		}

		if (clientPartner.getCallStatus() != VoIPConstants.CallStatus.PARTNER_BUSY)
		{
			clientPartner.setCallStatus(VoIPConstants.CallStatus.ENDED);
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
		
		clientPartner.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_END);

		if(clientPartner.reconnecting)
		{
			clientPartner.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_DROP);
		}
		
		// send a call rejected message through hike as well
		VoIPUtils.sendVoIPMessageUsingHike(clientPartner.getPhoneNumber(), 
				HikeConstants.MqttMessageTypes.VOIP_CALL_CANCELLED, 
				getCallId(), false);

		// Reset variables
		setCallid(0);
		isRingingOutgoing = false;
		isRingingIncoming = false;
		
		clientPartner.close();

		// Terminate threads
		if(notificationThread!=null)
			notificationThread.interrupt();

		if (connectionTimeoutThread != null)
			connectionTimeoutThread.interrupt();

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
		
		if (clientPartner.connected == true) {
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
		
		releaseAudioManager();
		
		// Empty the queues
		samplesToEncodeQueue.clear();
		decodedBuffersQueue.clear();
		
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
				clientPartner.sendPacket(dp, true);
				stop();
			}
		},"HANG_UP_THREAD").start();
		VoIPUtils.addMessageToChatThread(this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_CALL_SUMMARY, clientPartner.getCallDuration(), -1, false);
	}
	
	public void rejectIncomingCall() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.CALL_DECLINED);
				clientPartner.sendPacket(dp, true);
				stop();
			}
		},"REJECT_INCOMING_CALL_THREAD").start();
		
		// Here we don't show a missed call notification, but add the message to the chat thread
		VoIPUtils.addMessageToChatThread(this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING, 0, -1, false);
		clientPartner.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_REJECT);
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

		if (clientPartner.reconnecting)
			return;
		else
			clientPartner.reconnecting = true;

		reconnectAttempts++;
		Logger.w(VoIPConstants.TAG, "VoIPService reconnect()");

		// Interrupt the receiving thread since we will make the socket null
		// and it could throw an NPE.
		clientPartner.interruptReceivingThread();
		
		clientPartner.setCallStatus(VoIPConstants.CallStatus.RECONNECTING);
		sendHandlerMessage(VoIPConstants.MSG_RECONNECTING);
		clientPartner.socketInfoReceived = false;
		clientPartner.socketInfoSent = false;
		clientPartner.connected = false;
		clientPartner.retrieveExternalSocket();
		clientPartner.startReconnectBeeps();
	}
	
	private void startCodec() {
		try
		{
			opusWrapper = new OpusWrapper();
			opusWrapper.getDecoder(VoIPConstants.AUDIO_SAMPLE_RATE, 1);
			opusWrapper.getEncoder(VoIPConstants.AUDIO_SAMPLE_RATE, 1, clientPartner.localBitrate);
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
					VoIPDataPacket dpdecode = clientPartner.samplesToDecodeQueue.peek();
					if (dpdecode != null) {
						clientPartner.samplesToDecodeQueue.poll();
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
						synchronized (clientPartner.samplesToDecodeQueue) {
							try {
								clientPartner.samplesToDecodeQueue.wait();
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
									opusWrapper.setEncoderBitrate(clientPartner.localBitrate);
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
								synchronized (clientPartner.encodedBuffersQueue) { 
									clientPartner.encodedBuffersQueue.add(dp);
									clientPartner.encodedBuffersQueue.notify();
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
				clientPartner.sendPacket(dp, true);
			}
		}, "ACCEPT_INCOMING_CALL_THREAD").start();

		startRecordingAndPlayback();
		clientPartner.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_ACCEPT);
	}
	
	private void startRecordingAndPlayback() {

		if (clientPartner.audioStarted == true) {
			Logger.d(VoIPConstants.TAG, "Audio already started.");
			return;
		}

		if(clientPartner.getPreferredConnectionMethod() == ConnectionMethods.RELAY)
		{
			clientPartner.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_RELAY);
		}

		if (!isConnected()) {
			Logger.d(VoIPConstants.TAG, "Call has been answered before connection was established.");
			clientPartner.startReconnectBeeps();
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
		clientPartner.setCallStatus(VoIPConstants.CallStatus.ACTIVE);
		sendHandlerMessage(VoIPConstants.MSG_AUDIO_START);
		startChrono();
		clientPartner.audioStarted = true;
		
		// When the call has been answered, we will send our network connection class
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				clientPartner.setIdealBitrate();
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.CURRENT_BITRATE);
				dp.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(clientPartner.localBitrate).array());
				clientPartner.sendPacket(dp, true);
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
					recorder = new AudioRecord(audioSource, VoIPConstants.AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufSizeRecording);
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
						if (resamplerEnabled && playbackSampleRate != VoIPConstants.AUDIO_SAMPLE_RATE) {
							// We need to resample the output signal
							byte[] output = resampler.reSample(dp.getData(), 16, VoIPConstants.AUDIO_SAMPLE_RATE, playbackSampleRate);
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
	
	/**
	 * Is the VoIP service currently connected to another phone?
	 * This can return <b>false</b> even for an ongoing call, in case
	 * a reconnection is being attempted. To check if we are current in call, 
	 * use getCallId() instead.  
	 * 
	 * @return
	 */
	private boolean isConnected() {
		return clientPartner.connected;
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

		clientPartner.setCallStatus(!hold && !clientPartner.remoteHold ? VoIPConstants.CallStatus.ACTIVE : VoIPConstants.CallStatus.ON_HOLD);
		sendHandlerMessage(VoIPConstants.MSG_UPDATE_HOLD_BUTTON);
		
		// Send hold status to partner
		sendHoldStatus();
	}	

	public boolean getHold()
	{
		return hold;
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
				clientPartner.sendPacket(dp, true);
			}
		}).start();
	}
	
	public String getSessionKeyHash() {
		String hash = null;
		if (clientPartner.encryptor != null)
			hash = clientPartner.encryptor.getSessionMD5();
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
			
			if (clientPartner.reconnecting || clientPartner.audioStarted)
				return;
			
			if (isRingingOutgoing == true) {
				Logger.w(VoIPConstants.TAG, "Outgoing ringer is already ringing.");
				return;
			} else isRingingOutgoing = true;

			Logger.d(VoIPConstants.TAG, "Playing outgoing call ringer.");
			clientPartner.setCallStatus(VoIPConstants.CallStatus.OUTGOING_RINGING);
			sendHandlerMessage(VoIPConstants.CONNECTION_ESTABLISHED_FIRST_TIME);
			clientPartner.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_ESTABLISHED);
			setAudioModeInCall();
			ringtoneStreamID = playFromSoundPool(SOUND_INCOMING_RINGTONE, true);
		}
	}
	
	@SuppressWarnings("deprecation")
	private void playIncomingCallRingtone() {

		if (clientPartner.reconnecting || clientPartner.audioStarted || keepRunning == false)
			return;

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
	
}

