package com.bsb.hike.voip;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
import com.bsb.hike.R;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.voip.VoIPClient.ConnectionMethods;
import com.bsb.hike.voip.VoIPConstants.CallQuality;
import com.bsb.hike.voip.VoIPConstants.CallStatus;
import com.bsb.hike.voip.VoIPDataPacket.PacketType;
import com.bsb.hike.voip.VoIPUtils.CallSource;
import com.bsb.hike.voip.view.VoIPActivity;
import com.musicg.dsp.Resampler;

public class VoIPService extends Service {
	
	private final IBinder myBinder = new LocalBinder();
	private static final int NOTIFICATION_IDENTIFIER = 10;

	private Messenger mMessenger;
	private Thread codecCompressionThread = null, reconnectingBeepsThread = null;
	private boolean reconnectingBeeps = false;
	private volatile boolean keepRunning = true;
	private boolean mute, hold, speaker, vibratorEnabled = true;
	private int minBufSizePlayback, minBufSizeRecording;
	private Thread connectionTimeoutThread = null;
	private Thread recordingThread = null, playbackThread = null;
	private AudioTrack audioTrack = null;
	private static int callId = 0;
	private NotificationManager notificationManager;
	private NotificationCompat.Builder builder;
	private AudioManager audioManager;
	private int initialAudioMode, initialRingerMode;
	private boolean initialSpeakerMode;
	private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener;
	private int playbackSampleRate = 0;
	private int callSource = -1;
	private Thread notificationThread;
	
	// Codec
	private OpusWrapper opus;

	// Attached VoIP client(s)
	HashMap<String, VoIPClient> clients = new HashMap<String, VoIPClient>();

	// Ringtones (incoming and outgoing)
	private Ringtone ringtone;
	private Vibrator vibrator = null;
	private int ringtoneStreamID = 0;
	private boolean isRingingIncoming = false, isRingingOutgoing = false;

	// Sounds
	private SoundPool soundpool = null;
	private SparseIntArray soundpoolMap;
	private static final int SOUND_ACCEPT = R.raw.call_answer;
	private static final int SOUND_DECLINE = R.raw.call_end;
	private static final int SOUND_INCOMING_RINGTONE = R.raw.ring_tone;
	private static final int SOUND_RECONNECTING = R.raw.reconnect;

	// Wakelock
	private WakeLock wakeLock = null;
	
	// Resampler
	private boolean resamplerEnabled = false;
	private Resampler resampler;

	// Echo cancellation
	private boolean aecEnabled = true;
	private SolicallWrapper solicallAec = null;
	private boolean useVADToReduceData = true;
	private boolean aecSpeakerSignal = false, aecMicSignal = false;
	private int audiotrackFramesWritten = 0;
	private VoIPDataPacket silentPacket;

	private final ConcurrentLinkedQueue<VoIPDataPacket> samplesToEncodeQueue     = new ConcurrentLinkedQueue<VoIPDataPacket>();
	public final ConcurrentLinkedQueue<VoIPDataPacket> encodedBuffersQueue      = new ConcurrentLinkedQueue<VoIPDataPacket>();

	// Handler for messages from VoIP clients
	Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			String msisdn = bundle.getString(VoIPConstants.MSISDN);
			
			Logger.d(VoIPConstants.TAG, "Received message: " + msg.what + " from: " + msisdn);
			switch (msg.what) {
			case VoIPConstants.MSG_VOIP_CLIENT_STOP:
				if (!inConference())
					stop();
				else {
					Logger.d(VoIPConstants.TAG, msisdn + " has quit the conference.");
					getClient(msisdn).close();
					clients.remove(msisdn);
				}
				break;

			case VoIPConstants.MSG_VOIP_CLIENT_OUTGOING_CALL_RINGTONE:
				playOutgoingCallRingtone();
				break;

			case VoIPConstants.MSG_VOIP_CLIENT_INCOMING_CALL_RINGTONE:
				playIncomingCallRingtone();
				break;

			case VoIPConstants.MSG_UPDATE_REMOTE_HOLD:
				VoIPClient client = clients.get(msisdn);
				client.setCallStatus(!hold && !client.remoteHold ? VoIPConstants.CallStatus.ACTIVE : VoIPConstants.CallStatus.ON_HOLD);
				sendHandlerMessage(VoIPConstants.MSG_UPDATE_REMOTE_HOLD);
				break;

			case VoIPConstants.MSG_START_RECORDING_AND_PLAYBACK:
				startRecordingAndPlayback(msisdn);
				break;
				
			case VoIPConstants.MSG_START_COMPRESSION:
				startCodecCompression();
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
				initializeOpus();
				break;
				
			case VoIPConstants.MSG_SHUTDOWN_ACTIVITY:
				if (bundle.getBoolean(VoIPConstants.IS_CONNECTED) == true) {
					setSpeaker(true);
					playFromSoundPool(SOUND_DECLINE, false);
				}
				
				if (!inConference())
					sendHandlerMessage(msg.what, bundle);
				
				break;
				
			default:
				// Pass message to activity through its handler
				sendHandlerMessage(msg.what);
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

		Logger.d(VoIPConstants.TAG, "VoIPService onCreate()");
		acquireWakeLock();
		setCallid(0);
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
		
		if (resamplerEnabled) {
			resampler = new Resampler();
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
		String msisdn = intent.getStringExtra(VoIPConstants.Extras.MSISDN);
		if (action == null || action.isEmpty()) {
			return returnInt;
		}

		VoIPClient client = clients.get(msisdn);
		if (client == null && !TextUtils.isEmpty(msisdn)) {
			Logger.w(VoIPConstants.TAG, "Received message from " + msisdn +
					" but we do not have an associated client. Creating one.");
			client = new VoIPClient(getApplicationContext(), handler);
		}

		setSpeaker(false);

		// Call rejection message
		if (action.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_CANCELLED)) {
			Logger.d(VoIPConstants.TAG, "Call cancelled message.");
			if (keepRunning == true && getClient() != null && getClient().getPhoneNumber().equals(msisdn)) {
				Logger.w(VoIPConstants.TAG, "Hanging up call because of call cancelled message.");
				client.hangUp();
			} 
			return returnInt;
		}
		
		// Incoming call message
		if (action.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_REQUEST)) {

			int partnerCallId = intent.getIntExtra(VoIPConstants.Extras.CALL_ID, 0);
			setCallid(partnerCallId);
			client.setInitiator(true);
			
			// Send call initiation ack message
			VoIPUtils.sendVoIPMessageUsingHike(msisdn, 
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
			VoIPUtils.sendVoIPMessageUsingHike(msisdn, 
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

			client.setPhoneNumber(msisdn);
			client.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_HANDSHAKE_COMPLETE);

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

			client.setInternalIPAddress(intent.getStringExtra(VoIPConstants.Extras.INTERNAL_IP));
			client.setInternalPort(intent.getIntExtra(VoIPConstants.Extras.INTERNAL_PORT, 0));
			client.setExternalIPAddress(intent.getStringExtra(VoIPConstants.Extras.EXTERNAL_IP));
			client.setExternalPort(intent.getIntExtra(VoIPConstants.Extras.EXTERNAL_PORT, 0));
			client.setPhoneNumber(msisdn);
			client.setInitiator(intent.getBooleanExtra(VoIPConstants.Extras.INITIATOR, true));
			client.setRelayAddress(intent.getStringExtra(VoIPConstants.Extras.RELAY));
			client.setRelayPort(intent.getIntExtra(VoIPConstants.Extras.RELAY_PORT, VoIPConstants.ICEServerPort));

			// Error case: we are receiving a delayed v0 message for a call we 
			// initiated earlier. 
			if (!client.isInitiator() && partnerCallId != getCallId()) {
				Logger.w(VoIPConstants.TAG, "Receiving a return v0 for a invalid call.");
				return returnInt;
			}
				
			// Error case: we are receiving a repeat v0 during call setup
			if (client.socketInfoReceived && !partnerReconnecting) {
				Logger.d(VoIPConstants.TAG, "Repeat call initiation message.");
				// Try sending our socket info again. Caller could've missed our original message.
				if (!client.connected)
					client.sendSocketInfoToPartner();
				
				return returnInt;
			}
			
			// Check in case the other client is reconnecting to us
			if (client.connected && partnerCallId == getCallId() && partnerReconnecting) {
				Logger.w(VoIPConstants.TAG, "Partner trying to reconnect with us. CallId: " + getCallId());
				if (!client.reconnecting) {
					client.reconnect();
				} 
				if (client.socketInfoSent)
					client.establishConnection();
			} else {
				// All good. 
				setCallid(partnerCallId);
				if (client.isInitiator() && !client.reconnecting) {
					Logger.w(VoIPConstants.TAG, "Detected incoming VoIP call from: " + client.getPhoneNumber());
					clients.put(msisdn, client);
					client.retrieveExternalSocket();
				} else {
					// We have already sent our socket info to partner
					// And now they have sent us their's, so let's establish connection
					client.establishConnection();
				}
			}

			client.socketInfoReceived = true;
		}
		
		// We are initiating a VoIP call
		if (action.equals(VoIPConstants.Extras.OUTGOING_CALL)) 
		{

			// Edge case. 
			String myMsisdn = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(HikeMessengerApp.MSISDN_SETTING, null);

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
				client.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.CALLER_IN_NATIVE_CALL);
				return returnInt;
			}

			// Edge case: call button was hit for someone we are already speaking with. 
			if (getCallId() > 0 && client.getPhoneNumber()!=null && client.getPhoneNumber().equals(msisdn)) 
			{
				// Show activity
				Logger.d(VoIPConstants.TAG, "Restoring activity..");
				Intent i = new Intent(getApplicationContext(), VoIPActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(i);
				return returnInt;
			}

			/*
			// Commented to test conferencing
			if (getCallId() > 0) 
			{
				Logger.e(VoIPConstants.TAG, "Error. Already in a call.");
				return returnInt;
			}
			 */
			
			// we are making an outgoing call
			client.setPhoneNumber(msisdn);
			client.setInitiator(false);

			callSource = intent.getIntExtra(VoIPConstants.Extras.CALL_SOURCE, -1);
			if(callSource == CallSource.MISSED_CALL_NOTIF.ordinal())
			{
				VoIPUtils.cancelMissedCallNotification(getApplicationContext());
			}

			if (inConference())
				Logger.d(VoIPConstants.TAG, "We're in a conference. Maintaining call id: " + getCallId());
			else
				setCallid(new Random().nextInt(2000000000));
				
			Logger.w(VoIPConstants.TAG, "Making outgoing call to: " + client.getPhoneNumber() + ", id: " + getCallId());
			clients.put(msisdn, client);

			// Send call initiation message
			VoIPUtils.sendVoIPMessageUsingHike(client.getPhoneNumber(), 
					HikeConstants.MqttMessageTypes.VOIP_CALL_REQUEST, 
					getCallId(), true);

			startNotificationThread();
			
			// Show activity
			Intent i = new Intent(getApplicationContext(), VoIPActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
			
			client.retrieveExternalSocket();
			client.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_CLICK);
		}

		if(client.getCallStatus() == VoIPConstants.CallStatus.UNINITIALIZED)
		{
			client.setInitialCallStatus();
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
					
					boolean connected = false;
					for (VoIPClient client : clients.values()) {
						if (client.connected || client.reconnecting)
							connected = true;
					}
					
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
	
	private void initializeAEC() {

		if (solicallAec != null) {
			Logger.d(VoIPConstants.TAG, "AEC already initialized.");
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
	
	private void initializeOpus() {
		
		if (opus != null) {
			Logger.d(VoIPConstants.TAG, "Opus already initialized.");
			return;
		}
		
		try {
			opus = new OpusWrapper();
			opus.getDecoder(VoIPConstants.AUDIO_SAMPLE_RATE, 1);
			opus.getEncoder(VoIPConstants.AUDIO_SAMPLE_RATE, 1, getClient().localBitrate);
			opus.setEncoderComplexity(0);
		} catch (IOException e) {
			Logger.w(VoIPConstants.TAG, "Opus creation exception: " + e.toString());
		}
		
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

		VoIPClient client = getClient();
		
		int callDuration = client.getCallDuration();
		String durationString = (callDuration == 0)? "" : String.format(Locale.getDefault(), " (%02d:%02d)", (callDuration / 60), (callDuration % 60));

		String title = null;
		if (client.getName() == null)
			title = getString(R.string.voip_call_chat);
		else
			title = getString(R.string.voip_call_notification_title, client.getName()); 
		
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

	private VoIPClient getClient() {
		VoIPClient client = null;
		if (clients.size() > 0)
			client = (VoIPClient) clients.entrySet().iterator().next().getValue();
		return client;
	}
	
	private VoIPClient getClient(String msisdn) {
		VoIPClient client = null;
		client = clients.get(msisdn);
		return client;
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
				VoIPClient client = getClient();
				switch (focusChange) {
				case AudioManager.AUDIOFOCUS_GAIN:
					Logger.w(VoIPConstants.TAG, "AUDIOFOCUS_GAIN");
					if (client.getCallDuration() > 0 && hold == true)
						setHold(false);
					break;
				case AudioManager.AUDIOFOCUS_LOSS:
					Logger.w(VoIPConstants.TAG, "AUDIOFOCUS_LOSS");
					if (client.getCallDuration() > 0)
						setHold(true);
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
					Logger.d(VoIPConstants.TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
					if (client.getCallDuration() > 0)
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
		
		if (soundpool != null) {
			Logger.d(VoIPConstants.TAG, "Soundpool already initialized.");
			return;
		}
		
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
		return getClient();
	}
	
	public ConnectionMethods getConnectionMethod() {
		return getClient().getPreferredConnectionMethod();
	}
	
	public void startChrono() {

		VoIPClient client = getClient();
		
		try {
			if (client.chronometer == null) {
//				Looper.prepare();
//				Logger.w(VoIPConstants.TAG, "Starting chrono..");
				client.chronometer = new Chronometer(VoIPService.this);
				client.chronometer.setBase(SystemClock.elapsedRealtime());
				client.chronometer.start();
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
				setCallid(0);
				return;
			}
			keepRunning = false;
		}

		Logger.d(VoIPConstants.TAG, "Stopping service..");

		for (VoIPClient client : clients.values())
			removeClient(client);
		clients.clear();

		// Reset variables
		setCallid(0);
		isRingingOutgoing = false;
		isRingingIncoming = false;
		
		// Terminate threads
		if(notificationThread!=null)
			notificationThread.interrupt();

		if (connectionTimeoutThread != null)
			connectionTimeoutThread.interrupt();

		if (playbackThread != null)
			playbackThread.interrupt();
		
		if (recordingThread != null)
			recordingThread.interrupt();

		if (codecCompressionThread != null)
			codecCompressionThread.interrupt();
		
		stopRingtone();
		stopFromSoundPool(ringtoneStreamID);
		
		if (solicallAec != null) {
			solicallAec.destroy();
			solicallAec = null;
		}

		if (opus != null) {
			opus.destroy();
			opus = null;
		}
		
		releaseAudioManager();
		
		// Empty the queues
		samplesToEncodeQueue.clear();
		
		sendHandlerMessage(VoIPConstants.MSG_SHUTDOWN_ACTIVITY);
		releaseWakeLock();
		stopSelf();
	}
	
	public void rejectIncomingCall() {
		final VoIPClient client = getClient();

		new Thread(new Runnable() {
			
			@Override
			public void run() {
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.CALL_DECLINED);
				client.sendPacket(dp, true);
				stop();
			}
		},"REJECT_INCOMING_CALL_THREAD").start();
		
		// Here we don't show a missed call notification, but add the message to the chat thread
		VoIPUtils.addMessageToChatThread(this, client, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING, 0, -1, false);
		client.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_REJECT);
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
		sendHandlerMessage(message, null);
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

	public void acceptIncomingCall() {
		
		final VoIPClient client = getClient();
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.START_VOICE);
				client.sendPacket(dp, true);
			}
		}, "ACCEPT_INCOMING_CALL_THREAD").start();

		startRecordingAndPlayback(getClient().getPhoneNumber());
		client.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_ACCEPT);
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

						while (samplesToEncodeQueue.size() > VoIPConstants.MAX_SAMPLES_BUFFER) {
							samplesToEncodeQueue.poll();
						}
						
						if (encodedBuffersQueue.size() > VoIPConstants.MAX_SAMPLES_BUFFER)
							continue;

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
									// There is no voice signal, bitrate should be lowered
									if (!lowBitrateTrigger) {
										lowBitrateTrigger = true;
										opus.setEncoderBitrate(OpusWrapper.OPUS_LOWEST_SUPPORTED_BITRATE);
									}
								} else if (lowBitrateTrigger) {
									// Mic signal is reverting to voice
									lowBitrateTrigger = false;
									opus.setEncoderBitrate(getClient().localBitrate);
								}
							}
						} else
							aecMicSignal = true;
						
						try {
							// Add the uncompressed audio to the compression buffer
							// Logger.d(VoIPConstants.TAG, "Adding data to opus queue: " + dpencode.getData().length);
							opus.queue(dpencode.getData());
							// Get compressed data from the encoder
							while ((compressedDataLength = opus.getEncodedData(OpusWrapper.OPUS_FRAME_SIZE, compressedData, compressedData.length)) > 0) {
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
	

	
	private void startRecordingAndPlayback(String msisdn) {

		final VoIPClient client = getClient(msisdn);
		
		if (client.audioStarted == true) {
			Logger.d(VoIPConstants.TAG, "Audio already started.");
			return;
		}

		if(client.getPreferredConnectionMethod() == ConnectionMethods.RELAY)
		{
			client.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_RELAY);
		}

		if (!isConnected(msisdn)) {
			Logger.d(VoIPConstants.TAG, "Call has been answered before connection was established.");
			startReconnectBeeps();
		}
		
		Logger.d(VoIPConstants.TAG, "Starting audio record / playback.");
		client.interruptResponseTimeoutThread();
		stopRingtone();
		stopFromSoundPool(ringtoneStreamID);
		isRingingOutgoing = isRingingIncoming = false;
		playFromSoundPool(SOUND_ACCEPT, false);
		if (clients.size() == 1) {
			startRecording();
			startPlayBack();
		} else {
			Logger.w(VoIPConstants.TAG, "Skipping startRecording() and startPlayBack()");
		}
		
		client.setCallStatus(VoIPConstants.CallStatus.ACTIVE);
		sendHandlerMessage(VoIPConstants.MSG_AUDIO_START);
		startChrono();
		client.audioStarted = true;
		
		// When the call has been answered, we will send our network connection class
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				client.setIdealBitrate();
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.CURRENT_BITRATE);
				dp.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(client.localBitrate).array());
				client.sendPacket(dp, true);
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
		
		final VoIPClient client = getClient();
		
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
					client.hangUp();
					return;
				}
				
				try {
					audioTrack.play();
				} catch (IllegalStateException e) {
					Logger.e(VoIPConstants.TAG, "Audiotrack error: " + e.toString());
					client.hangUp();
				}
				
				// Clear the audio queue
				client.decodedBuffersQueue.clear();
				
				byte[] solicallSpeakerBuffer = new byte[SolicallWrapper.SOLICALL_FRAME_SIZE * 2];
				while (keepRunning == true) {
					VoIPDataPacket dp = client.decodedBuffersQueue.peek();
					if (dp != null) {
						client.decodedBuffersQueue.poll();

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
						synchronized (client.decodedBuffersQueue) {
							try {
								client.decodedBuffersQueue.wait();
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
		
		final VoIPClient client = getClient();
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
									client.decodedBuffersQueue.size() == 0) {
								// We are running low on speaker data
			                	synchronized (client.decodedBuffersQueue) {
			                		client.decodedBuffersQueue.add(silentPacket);
			                		client.decodedBuffersQueue.notify();
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
	private boolean isConnected(String msisdn) {
		boolean connected = false;
		if (getClient(msisdn) != null)
			connected = getClient(msisdn).connected;
		return connected;
	}
	
	public void setHold(boolean newHold) {
		
		Logger.d(VoIPConstants.TAG, "Changing hold to: " + newHold + " from: " + this.hold);

		if (this.hold == newHold)
			return;
		
		this.hold = newHold;
		final VoIPClient client = getClient();
		
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
		sendHandlerMessage(VoIPConstants.MSG_UPDATE_HOLD_BUTTON);
		
		// Send hold status to partner
		sendHoldStatus();
	}	

	public boolean getHold()
	{
		return hold;
	}
	
	private void sendHoldStatus() {
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
		if (client.encryptor != null)
			hash = client.encryptor.getSessionMD5();
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
		VoIPClient client = getClient();
		synchronized (this) {
			
			if (client.reconnecting || client.audioStarted)
				return;
			
			if (isRingingOutgoing == true) {
				Logger.w(VoIPConstants.TAG, "Outgoing ringer is already ringing.");
				return;
			} else isRingingOutgoing = true;

			Logger.d(VoIPConstants.TAG, "Playing outgoing call ringer.");
			client.setCallStatus(VoIPConstants.CallStatus.OUTGOING_RINGING);
			sendHandlerMessage(VoIPConstants.CONNECTION_ESTABLISHED_FIRST_TIME);
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

	public void startReconnectBeeps() {
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

	public void sendAnalyticsEvent(String ek)
	{
		sendAnalyticsEvent(ek, -1);
	}

	public void sendAnalyticsEvent(String ek, int value) {
		getClient().sendAnalyticsEvent(ek, value);
	}
	
	public CallQuality getQuality() {
		VoIPClient client = getClient();
		
		if (client != null)
			return getClient().getQuality();
		else
			return CallQuality.UNKNOWN;
	}

	public boolean isAudioRunning() {
		return getClient().isAudioRunning();
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
		
		if (client != null)
			return getClient().getCallStatus();
		else
			return CallStatus.UNINITIALIZED;
	}

	public void hangUp() {
		for (VoIPClient client : clients.values())
			client.hangUp();
	}
	
	public int getCallDuration() {
		return getClient().getCallDuration();
	}
	
	private boolean inConference() {
		boolean conference = false;
		if (clients.size() > 1)
			conference = true;
		Logger.d(VoIPConstants.TAG, "Conference check: " + conference);
		return conference;
	}
	
	private void removeClient(String msisdn) {
		VoIPClient client = clients.get(msisdn);
		if (client != null) {
			removeClient(client);
		}
	}
	
	private void removeClient(VoIPClient client) {
		client.close();
		clients.remove(client.getPhoneNumber());
	}
}

