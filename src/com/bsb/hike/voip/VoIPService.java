package com.bsb.hike.voip;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
import com.bsb.hike.utils.Utils;
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
	private boolean reconnectingBeeps = false;
	private volatile boolean keepRunning;
	private boolean mute, hold, speaker, vibratorEnabled = true;
	private int minBufSizePlayback, minBufSizeRecording;
	private AudioTrack audioTrack = null;
	private static int callId = 0;
	private NotificationManager notificationManager;
	private NotificationCompat.Builder builder;
	private AudioManager audioManager;
	private int initialAudioMode, initialRingerMode;
	private boolean initialSpeakerMode;
	private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener;
	private int playbackSampleRate = 0;
	
	private boolean conferencingEnabled = false;
	
	// Task executors
	private Thread processRecordedSamplesThread = null, bufferSendingThread = null, reconnectingBeepsThread = null;
	private Thread connectionTimeoutThread = null;
	private Thread recordingThread = null, playbackThread = null;
	private Thread notificationThread = null;
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

	private final LinkedBlockingQueue<VoIPDataPacket> recordedSamples     = new LinkedBlockingQueue<VoIPDataPacket>();
	private final LinkedBlockingQueue<VoIPDataPacket> buffersToSend      = new LinkedBlockingQueue<VoIPDataPacket>();
	private final LinkedBlockingQueue<VoIPDataPacket> processedRecordedSamples      = new LinkedBlockingQueue<VoIPDataPacket>();
	private final LinkedBlockingQueue<VoIPDataPacket> playbackBuffersQueue      = new LinkedBlockingQueue<VoIPDataPacket>();
	private final CircularByteBuffer recordBuffer = new CircularByteBuffer();
	

	// Handler for messages from VoIP clients
	Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			String msisdn = bundle.getString(VoIPConstants.MSISDN);
			
			// Logger.d(VoIPConstants.TAG, "Received message: " + msg.what + " from: " + msisdn);
			
			switch (msg.what) {
			case VoIPConstants.MSG_VOIP_CLIENT_STOP:
				Logger.d(VoIPConstants.TAG, msisdn + " has stopped.");
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
			// For the Solicall AEC library to work, we must record data in chunks
			// which are a multiple of the library's supported frame size (20ms).
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

			if (getCallId() > 0 && !conferencingEnabled) 
			{
				Logger.e(VoIPConstants.TAG, "Error. Already in a call.");
				return returnInt;
			}
			
			// we are making an outgoing call
			client.setPhoneNumber(msisdn);
			client.setInitiator(false);

			client.callSource = intent.getIntExtra(VoIPConstants.Extras.CALL_SOURCE, -1);
			if(client.callSource == CallSource.MISSED_CALL_NOTIF.ordinal())
			{
				VoIPUtils.cancelMissedCallNotification(getApplicationContext());
			}

			if (clients.size() > 0 && getCallId() > 0) {
				Logger.d(VoIPConstants.TAG, "We're in a conference. Maintaining call id: " + getCallId());
				// Disable crypto for clients in conference. 
				getClient().cryptoEnabled = false;
				client.cryptoEnabled = false;
			}
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
		clients.clear();
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
				if (client == null)
					return;
				
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

//		synchronized (this) {
//			if (keepRunning == false) {
//				// Logger.w(VoIPConstants.TAG, "Trying to stop a stopped service?");
//				sendHandlerMessage(VoIPConstants.MSG_SHUTDOWN_ACTIVITY);
//				setCallid(0);
//				return;
//			}
//			keepRunning = false;
//		}
//
		Logger.d(VoIPConstants.TAG, "Stopping service..");
		keepRunning = false;

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

		if (processRecordedSamplesThread != null)
			processRecordedSamplesThread.interrupt();
		
		if (bufferSendingThread != null)
			bufferSendingThread.interrupt();
		
		stopRingtone();
		stopFromSoundPool(ringtoneStreamID);
		
		if (solicallAec != null) {
			solicallAec.destroy();
			solicallAec = null;
		}

		releaseAudioManager();
		
		// Empty the queues
		recordedSamples.clear();
		
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
	
	private void startSendingBuffersToEncode() {
		bufferSendingThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				while (keepRunning) {
					VoIPDataPacket dp;
					try {
						// We will only get a buffer to send if a conference is not active. 
						dp = buffersToSend.take();
						for (VoIPClient client : clients.values()) {
							// client.encodedBuffersQueue.put(dp);
							// Not sure if cloning is required. Test later. 
							// VoIPDataPacket dpClone = (VoIPDataPacket)dp.clone();
							// client.samplesToEncodeQueue.put(dp.getData());
							client.addSampleToEncode(dp.getData()); 
						}
					} catch (InterruptedException e1) {
						break;
					}
				}
			}
		}, "BUFFER_ALLOCATOR_THREAD");
		bufferSendingThread.start();
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
				while (keepRunning == true) {
					retVal = recorder.read(recordedData, 0, recordedData.length);
					if (retVal != recordedData.length) {
						Logger.w(VoIPConstants.TAG, "Unexpected recorded data length. Expected: " + recordedData.length + ", Recorded: " + retVal);
						continue;
					}
					
					if (mute == true)
						continue;
					
                	// Add it to the samples to encode queue
//					VoIPDataPacket dp = new VoIPDataPacket(VoIPDataPacket.PacketType.VOICE_PACKET);
//                	dp.write(recordedData);
//                	recordedSamples.add(dp);

					// Break input audio into smaller chunks for Solicall AEC
	            	int index = 0;
	            	int newSize = 0;
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

	                	synchronized (recordedSamples) {
		                	recordedSamples.add(dp);
	                		recordedSamples.notify();
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
				boolean lowBitrateTrigger = false;
				
				while (keepRunning == true) {
					VoIPDataPacket dpencode;
					try {
						dpencode = recordedSamples.take();
					} catch (InterruptedException e) {
						break;
					}

					while (recordedSamples.size() > VoIPConstants.MAX_SAMPLES_BUFFER) {
						// Logger.w(VoIPConstants.TAG, "Dropping recorded buffer. AEC sync will be lost.");
						recordedSamples.poll();
					}

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
							if (ret == 0 && !inConference()) {
								// There is no voice signal, bitrate should be lowered
								if (!lowBitrateTrigger) {
									lowBitrateTrigger = true;
									getClient().setEncoderBitrate(OpusWrapper.OPUS_LOWEST_SUPPORTED_BITRATE);
								}
							} else if (lowBitrateTrigger) {
								// Mic signal is reverting to voice
								lowBitrateTrigger = false;
								getClient().setEncoderBitrate(getClient().localBitrate);
							}
						}
					} else
						aecMicSignal = true;

					if (buffersToSend.size() > VoIPConstants.MAX_SAMPLES_BUFFER)
						continue;

					recordBuffer.write(dpencode.getData());

					// Pass the recorded samples to the client objects
					// so they can be compressed and sent
					while (recordBuffer.getAvailable() >= OpusWrapper.OPUS_FRAME_SIZE * 2) {
						byte[] pcmData = new byte[OpusWrapper.OPUS_FRAME_SIZE * 2];
						recordBuffer.read(pcmData);
						VoIPDataPacket dp = new VoIPDataPacket(PacketType.VOICE_PACKET);
						dp.setData(pcmData);
						
						// If we are not in a conference, then we can just send the recorded
						// sample to the connected client. 
						// However, if we are in a conference, then the recorded sample must be
						// mixed with all the other incoming audio
						if (inConference()) {
							// Maintain a tight queue
							if (processedRecordedSamples.size() < 2)
								processedRecordedSamples.add(dp);
						} else
							buffersToSend.add(dp);
					}

				}
			}
		}, "PROCESS_RECORDED_SAMPLES_THREAD");
		
		processRecordedSamplesThread.start();
		startSendingBuffersToEncode();
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
					getClient().hangUp();
					return;
				}
				
				try {
					audioTrack.play();
				} catch (IllegalStateException e) {
					Logger.e(VoIPConstants.TAG, "Audiotrack error: " + e.toString());
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
//							long timer = System.currentTimeMillis();
							while (index < dp.getLength()) {
								size = Math.min(minBufSizePlayback, dp.getLength() - index);
								audioTrack.write(dp.getData(), index, size);
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
						Logger.w(VoIPConstants.TAG, "Audiotrack IllegalStateException: " + e.toString());
					}
				}
				
			}
		}, "PLAY_BACK_THREAD");
		
		playbackThread.start();
		processDecodedSamples();
	}
	
	private void processDecodedSamples() {
		
		// This is how often we feed PCM samples to the speaker. 
		// Should be equal to 60ms for a frame size of 2880. (2880 / 48000)
		int sleepTime = OpusWrapper.OPUS_FRAME_SIZE * 1000 / VoIPConstants.AUDIO_SAMPLE_RATE;
		
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

		if (scheduledExecutorService != null) {
			Logger.w(VoIPConstants.TAG, "Feeder is already running.");
			return;
		} else {
			scheduledExecutorService = Executors.newScheduledThreadPool(1);
		}
		
		scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
			
			@Override
			public void run() {
				// Logger.d(VoIPConstants.TAG, "Running feeder");
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				
//				long time = System.currentTimeMillis();
				
				byte[] silence = new byte[OpusWrapper.OPUS_FRAME_SIZE * 2];
				VoIPDataPacket silentPacket = new VoIPDataPacket(PacketType.VOICE_PACKET);
				silentPacket.setData(silence);
				
				if (keepRunning) {
					
					HashMap<String, byte[]> clientSample = new HashMap<String, byte[]>();
					
					// Retrieve decoded samples from all clients and combine into one
					VoIPDataPacket decodedSample = null;
					for (VoIPClient client : clients.values()) {
						VoIPDataPacket dp = client.getDecodedBuffer();
						if (dp != null) {
							clientSample.put(client.getPhoneNumber(), dp.getData());
							
							if (decodedSample == null)
								decodedSample = dp;
							else {
								// We have to combine samples
								decodedSample.setData(VoIPUtils.addPCMSamples(decodedSample.getData(), dp.getData()));
							}
						}
					}
					
					// Add to our decoded samples queue
					try {
						if (decodedSample == null) {
							// Logger.d(VoIPConstants.TAG, "Decoded samples underrun. Adding silence.");
							decodedSample = silentPacket;
						}

						if (!hold) {
							if (playbackBuffersQueue.size() < VoIPConstants.MAX_SAMPLES_BUFFER)
								playbackBuffersQueue.put(decodedSample);
							else
								Logger.w(VoIPConstants.TAG, "Playback buffers queue full.");
						}
						
					} catch (InterruptedException e) {
						Logger.e(VoIPConstants.TAG, "InterruptedException while adding playback sample: " + e.toString());
					}
					
					// If we are in conference, then add our own recorded signal as well
					// to send to all connected clients. 
					// From the sum of all signals, we will have to subtract each client's
					// own signal before sending, or they will hear a perfect echo.
					if (inConference()) {
						VoIPDataPacket dp = processedRecordedSamples.poll();
						if (dp != null) {
							byte[] conferencePCM = VoIPUtils.addPCMSamples(decodedSample.getData(), dp.getData());
							dp.setData(conferencePCM);
							
							for (VoIPClient client : clients.values()) {
								VoIPDataPacket clientDp = new VoIPDataPacket();
								byte[] origPCM = clientSample.get(client.getPhoneNumber());
								byte[] newPCM = null;
								if (origPCM == null) {
									newPCM = conferencePCM;
								} else {
									newPCM = VoIPUtils.subtractPCMSamples(conferencePCM, origPCM);
								}
								clientDp.setData(newPCM);
								client.addSampleToEncode(clientDp.getData()); 
							}
						}
					}
					
					clientSample.clear();
					
				} else {
					Logger.d(VoIPConstants.TAG, "Shutting down decoded samples poller.");
					scheduledFuture.cancel(true);
					scheduledExecutorService.shutdownNow();
				}
				
//				Logger.d(VoIPConstants.TAG, "Running time: " + (System.currentTimeMillis() - time) + "ms");
			}
		}, 0, sleepTime, TimeUnit.MILLISECONDS);
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
			
			if (client == null || client.reconnecting || client.audioStarted)
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
			
			if (ringtone == null) {
				Logger.e(VoIPConstants.TAG, "Unable to get ringtone object.");
				return;
			}
			
			if (Utils.isLollipopOrHigher()) {
				AudioAttributes.Builder attrs = new AudioAttributes.Builder();
				attrs.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
				attrs.setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE);
				ringtone.setAudioAttributes(attrs.build());
			} else
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
		VoIPClient client = getClient();
		if (client != null)
			client.sendAnalyticsEvent(ek, value);
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
		// Logger.d(VoIPConstants.TAG, "Conference check: " + conference);
		return conference;
	}
	
	private void removeClient(VoIPClient client) {
		client.close();
		clients.remove(client.getPhoneNumber());
	}
	
	public boolean toggleConferencing() {
		conferencingEnabled = !conferencingEnabled;
		return conferencingEnabled;
	}
}

