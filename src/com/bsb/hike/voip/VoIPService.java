package com.bsb.hike.voip;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
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
import android.content.SharedPreferences.Editor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.AudioTrack.OnPlaybackPositionUpdateListener;
import android.media.MediaRecorder;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.Chronometer;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.voip.VoIPClient.ConnectionMethods;
import com.bsb.hike.voip.VoIPConstants.CallQuality;
import com.bsb.hike.voip.VoIPDataPacket.PacketType;
import com.bsb.hike.voip.VoIPEncryptor.EncryptionStage;
import com.bsb.hike.voip.VoIPUtils.ConnectionClass;
import com.bsb.hike.voip.protobuf.VoIPSerializer;
import com.bsb.hike.voip.view.VoIPActivity;
import com.musicg.dsp.Resampler;

public class VoIPService extends Service {
	
	private final IBinder myBinder = new LocalBinder();
	private final int AUDIO_SAMPLE_RATE = 48000; 
	private final int PACKET_TRACKING_SIZE = 128;
	private final int HEARTBEAT_INTERVAL = 1000;
	private final int HEARTBEAT_TIMEOUT = 5000;
	private final int HEARTBEAT_HARD_TIMEOUT = 30000;
	private final int MAX_SAMPLES_BUFFER = 3;
	private static final int NOTIFICATION_IDENTIFIER = 10;

	private int localBitrate = VoIPConstants.BITRATE_WIFI, remoteBitrate = 0;
	private boolean cryptoEnabled = true;
	private Messenger mMessenger;
	private static boolean connected = false;
	private boolean reconnecting = false;
	private int currentPacketNumber = 0;
	private int previousHighestRemotePacketNumber = 0;
	private boolean keepRunning = true;
	private DatagramSocket socket = null;
	private VoIPClient clientPartner = null, clientSelf = null;
	private BitSet packetTrackingBits = new BitSet(PACKET_TRACKING_SIZE);
	private long lastHeartbeat;
	private int totalBytesSent = 0, totalBytesReceived = 0, rawVoiceSent = 0;
	private VoIPEncryptor encryptor = new VoIPEncryptor();
	private VoIPEncryptor.EncryptionStage encryptionStage;
	private boolean mute, hold, speaker, vibratorEnabled = true;
	private boolean audioStarted = false;
	private int droppedDecodedPackets = 0;
	private int minBufSizePlayback;
	private int gain = 0;
	private OpusWrapper opusWrapper;
	private Resampler resampler;
	private Thread partnerTimeoutThread = null;
	private Thread recordingThread = null, playbackThread = null, sendingThread = null, receivingThread = null;
	private AudioTrack audioTrack;
	private ToneGenerator toneGenerator = null;
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
	private boolean resamplerEnabled = false;
	private Thread senderThread, reconnectingBeepsThread;
	private Ringtone ringtone;
	private Vibrator vibrator = null;

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
	private int ringtoneStreamID = 0;

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
		
		clientPartner = new VoIPClient();
		clientSelf = new VoIPClient();
		setCallid(0);
		encryptionStage = EncryptionStage.STAGE_INITIAL;
		initAudioManager();
		
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
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		int returnInt = super.onStartCommand(intent, flags, startId);
		
//		Logger.d(VoIPConstants.TAG, "VoIPService onStartCommand()");

		if (intent == null)
			return returnInt;
		
		String action = intent.getStringExtra("action");

		if (action == null || action.isEmpty()) {
			return returnInt;
		}
		
		if (action.equals("setpartnerinfo")) {
			
			int partnerCallId = intent.getIntExtra("callId", 0);
			
			// Error case: we receive a call while we are connecting / connected to another call
			if (getCallId() != 0 && partnerCallId != getCallId()) {
				Logger.w(VoIPConstants.TAG, "Call ID mismatch. Remote: " + partnerCallId + ", Self: " + getCallId());
				try {
					VoIPUtils.sendMessage(intent.getStringExtra("msisdn"), 
							HikeConstants.MqttMessageTypes.MESSAGE_VOIP_0, 
							HikeConstants.MqttMessageTypes.VOIP_ERROR_ALREADY_IN_CALL);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				return returnInt;
			}
			
			// Error case: we are in a cellular call
			if (VoIPUtils.isUserInCall(getApplicationContext())) {
				Log.w(VoIPConstants.TAG, "We are already in a cellular call.");
				try {
					VoIPUtils.sendMessage(intent.getStringExtra("msisdn"), 
							HikeConstants.MqttMessageTypes.MESSAGE_VOIP_0, 
							HikeConstants.MqttMessageTypes.VOIP_ERROR_ALREADY_IN_CALL);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				return returnInt;
			}
			
			// Error case: partner is trying to reconnect to us, but we aren't
			// expecting a reconnect
			boolean partnerReconnecting = intent.getBooleanExtra("reconnecting", false);
			if (partnerReconnecting == true && partnerCallId != getCallId()) {
				Logger.w(VoIPConstants.TAG, "Partner trying to reconnect? Remote: " + partnerCallId + ", Self: " + getCallId());
//				hangUp();
				return returnInt;
			}

			clientPartner = new VoIPClient();
			clientPartner.setInternalIPAddress(intent.getStringExtra("internalIP"));
			clientPartner.setInternalPort(intent.getIntExtra("internalPort", 0));
			clientPartner.setExternalIPAddress(intent.getStringExtra("externalIP"));
			clientPartner.setExternalPort(intent.getIntExtra("externalPort", 0));
			clientPartner.setPhoneNumber(intent.getStringExtra("msisdn"));
			clientPartner.setInitiator(intent.getBooleanExtra("initiator", true));
			clientSelf.setInitiator(!clientPartner.isInitiator());

//			Logger.d(VoIPConstants.TAG, "Setting our relay to: " + intent.getStringExtra("relay"));
			clientSelf.setRelayAddress(intent.getStringExtra("relay"));
			clientPartner.setRelayAddress(intent.getStringExtra("relay"));

			// Error case: we are receiving a delayed v0 message for a call we 
			// initiated earlier. 
			if (!clientPartner.isInitiator() && partnerCallId != getCallId()) {
				Logger.w(VoIPConstants.TAG, "Receiving a return v0 for a invalid call.");
				return returnInt;
			}
				
			// Error case: we are receiving a repeat v0 during call setup
			if (partnerCallId == getCallId() && !partnerReconnecting && clientPartner.isInitiator()) {
				Logger.d(VoIPConstants.TAG, "Repeat call initiation message.");
				// Try sending our socket info again. Caller could've missed our original message.
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
					Logger.d(VoIPConstants.TAG, "Detected incoming VoIP call.");
					retrieveExternalSocket();
				} else {
					// We have already sent our socket info to partner
					// And now they have sent us their's, so let's establish connection
					// OR, we are reconnecting
					establishConnection();
				}
			}

			socketInfoReceived = true;
		}
		
		// We are initiating a VoIP call
		if (action.equals("outgoingcall")) {

			// Edge case. 
			String myMsisdn = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(HikeMessengerApp.MSISDN_SETTING, null);
			if (myMsisdn != null && myMsisdn.equals(intent.getStringExtra("msisdn"))) {
				Logger.wtf(VoIPConstants.TAG, "Don't be ridiculous!");
				return returnInt;
			}
			
			// Error case: we are in a cellular call
			if (VoIPUtils.isUserInCall(getApplicationContext())) {
				Log.w(VoIPConstants.TAG, "We are already in a cellular call.");
				try {
					VoIPUtils.sendMessage(intent.getStringExtra("msisdn"), 
							HikeConstants.MqttMessageTypes.MESSAGE_VOIP_0, 
							HikeConstants.MqttMessageTypes.VOIP_ERROR_ALREADY_IN_CALL);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				return returnInt;
			}
			
			if (getCallId() > 0) {
				Logger.e(VoIPConstants.TAG, "Error. Already in a call.");
				return returnInt;
			}
			
			// we are making an outgoing call
			clientPartner.setPhoneNumber(intent.getStringExtra("msisdn"));
			clientSelf.setInitiator(true);
			clientPartner.setInitiator(false);
			setCallid(new Random().nextInt(99999999));
			Logger.d(VoIPConstants.TAG, "Making outgoing call to: " + clientPartner.getPhoneNumber() + ", id: " + getCallId());
			
			// Show activity
			Intent i = new Intent(getApplicationContext(), VoIPActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
			
			retrieveExternalSocket();
		}

		return returnInt;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stop();
		dismissNotification();
		Logger.d(VoIPConstants.TAG, "VoIP Service destroyed.");
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
		Notification myNotification = builder
		.setContentTitle("Hike Ongoing Call")
		.setContentText("Call in progress " + durationString)
		.setSmallIcon(R.drawable.ic_launcher)
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
					if (getCallDuration() > 0)
						setHold(false);
					break;
				case AudioManager.AUDIOFOCUS_LOSS:
					Logger.w(VoIPConstants.TAG, "AUDIOFOCUS_LOSS");
					setHold(true);
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
					Logger.w(VoIPConstants.TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
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
		setSpeaker(false);

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
		
		/*
		if (soundpool != null) {
			soundpool.release();
			soundpool = null;
		}
		*/
	}

	@SuppressLint("InlinedApi") private void setAudioModeInCall() {
		if (android.os.Build.VERSION.SDK_INT >= 11)
			audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);	
		audioManager.setParameters("noise_suppression=on");
	}
	
	private void saveCurrentAudioSettings() {
//		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		initialAudioMode = audioManager.getMode();
		initialRingerMode = audioManager.getRingerMode();
		initialSpeakerMode = audioManager.isSpeakerphoneOn();
	}

	private void restoreAudioSettings() {
//		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audioManager.setMode(initialAudioMode);
		audioManager.setRingerMode(initialRingerMode);
		audioManager.setSpeakerphoneOn(initialSpeakerMode);
	}
	
	private void initSoundPool() {
		soundpool = new SoundPool(2, AudioManager.STREAM_VOICE_CALL, 0);

		soundpoolMap = new SparseIntArray(3);
		soundpoolMap.put(SOUND_ACCEPT, soundpool.load(getApplicationContext(), SOUND_ACCEPT, 1));
		soundpoolMap.put(SOUND_DECLINE, soundpool.load(getApplicationContext(), SOUND_DECLINE, 1));
		soundpoolMap.put(SOUND_INCOMING_RINGTONE, soundpool.load(getApplicationContext(), SOUND_INCOMING_RINGTONE, 1));
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
	
	public void startStreaming() throws Exception {
		
		if (clientPartner == null || clientSelf == null) {
			throw new Exception("Clients (partner and/or self) not set.");
		}
		
		startCodec(); 
		startSendingAndReceiving();
		startHeartbeat();
		exchangeCryptoInfo();
		
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
	
	public void stop() {
		if (keepRunning == false) {
			// Logger.w(VoIPConstants.TAG, "Trying to stop a stopped service?");
			sendHandlerMessage(VoIPActivity.MSG_SHUTDOWN_ACTIVITY);
			return;
		}
		
		Logger.d(VoIPConstants.TAG, "VoIPService stop()");
		
		sendHandlerMessage(VoIPActivity.MSG_SHUTDOWN_ACTIVITY);
		Logger.d(VoIPConstants.TAG, "Bytes sent / received: " + totalBytesSent + " / " + totalBytesReceived +
				"\nPackets sent / received: " + totalPacketsSent + " / " + totalPacketsReceived +
				"\nPure voice bytes: " + rawVoiceSent +
				"\nDropped decoded packets: " + droppedDecodedPackets +
				"\nReconnect attempts: " + reconnectAttempts +
				"\nCall duration: " + getCallDuration());
		
		if (partnerTimeoutThread != null)
			partnerTimeoutThread.interrupt();

		if (reconnectingBeepsThread != null)
			reconnectingBeepsThread.interrupt();

		// Hangup tone
		/*
		if (clientSelf.isInitiator() || connected)
			toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP);
		*/
		stopRingtone();
		stopFromSoundPool(ringtoneStreamID);
		setSpeaker(true);
		playFromSoundPool(SOUND_DECLINE, false);
		releaseAudioManager();
		
		if (opusWrapper != null)
			opusWrapper.destroy();

		keepRunning = false;
		connected = false;
		setCallid(0);
		chronometer = null;
		stopSelf();
	}
	
	public void hangUp() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.END_CALL);
				sendPacket(dp, true);
				stop();
			}
		}).start();
		VoIPUtils.addMessageToChatThread(this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_CALL_SUMMARY, getCallDuration(), -1);
	}
	
	public void rejectIncomingCall() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.CALL_DECLINED);
				sendPacket(dp, true);
				stop();
			}
		}).start();
		
		// sendHandlerMessage(VoIPActivity.MSG_INCOMING_CALL_DECLINED);
		VoIPUtils.addMessageToChatThread(this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING, 0, -1);

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

	/**
	 * Reconnect after a communications failure.
	 */
	private void reconnect() {

		if (reconnecting)
			return;

		startReconnectBeeps();
		reconnectAttempts++;
		Logger.w(VoIPConstants.TAG, "VoIPService reconnect()");
		sendHandlerMessage(VoIPActivity.MSG_RECONNECTING);
		reconnecting = true;
		socketInfoReceived = false;
		socketInfoSent = false;
		retrieveExternalSocket();
	}
	
	private void startReconnectBeeps() {
		reconnectingBeepsThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				if (toneGenerator == null)
					toneGenerator = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100);
				
				while (keepRunning) {
					toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
						toneGenerator = null;
						break;
					}
				}
			}
		});
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
		}).start();
		
		// Listening for heartbeat, and housekeeping
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				lastHeartbeat = System.currentTimeMillis();
				while (keepRunning == true) {
					if (System.currentTimeMillis() - lastHeartbeat > HEARTBEAT_TIMEOUT && !reconnecting) {
						// Logger.w(VoIPConstants.TAG, "Heartbeat failure. Reconnecting.. ");
						if (clientSelf.isInitiator() && isConnected() && isAudioRunning())
							reconnect();
						else if (!isConnected())	// Give me the call receiver time so the initiator can reestablish connection.
							hangUp();
					}
					
					if (System.currentTimeMillis() - lastHeartbeat > HEARTBEAT_HARD_TIMEOUT) {
						Logger.d(VoIPConstants.TAG, "Giving up on connection.");
						hangUp();
						break;
					}
					
					sendPacketsWaitingForAck();
					showNotification();
					
					// Monitor quality of incoming data
					if ((System.currentTimeMillis() - lastQualityReset > VoIPConstants.QUALITY_WINDOW * 1000) 
							&& getCallDuration() > VoIPConstants.QUALITY_WINDOW) {
						CallQuality newQuality;
						int idealPacketCount = (AUDIO_SAMPLE_RATE * VoIPConstants.QUALITY_WINDOW) / OpusWrapper.OPUS_FRAME_SIZE; 
						if (qualityCounter >= idealPacketCount)
							newQuality = CallQuality.EXCELLENT;
						else if (qualityCounter >= idealPacketCount - VoIPConstants.QUALITY_WINDOW)
							newQuality = CallQuality.GOOD;
						else if (qualityCounter >= idealPacketCount - VoIPConstants.QUALITY_WINDOW)
							newQuality = CallQuality.FAIR;
						else 
							newQuality = CallQuality.WEAK;

						if (newQuality != currentCallQuality) {
							currentCallQuality = newQuality;
							sendHandlerMessage(VoIPActivity.MSG_UPDATE_QUALITY);
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
						Logger.d(VoIPConstants.TAG, "Dropping to_encode packet.");
						samplesToEncodeQueue.poll();
					}
					
					while (decodedBuffersQueue.size() > MAX_SAMPLES_BUFFER + 1) {
						// Logger.d(VoIPConstants.TAG, "Dropping decoded packet.");
						droppedDecodedPackets++;
						decodedBuffersQueue.poll();
					}
					
					while (encodedBuffersQueue.size() > MAX_SAMPLES_BUFFER) {
						Logger.d(VoIPConstants.TAG, "Dropping encoded packet.");
						encodedBuffersQueue.poll();
					}

					try {
						Thread.sleep(HEARTBEAT_INTERVAL);
					} catch (InterruptedException e) {
						Logger.d(VoIPConstants.TAG, "Heartbeat InterruptedException: " + e.toString());
					}
					
				}
			}
		}).start();
		
	}
	
	private void startCodec() {
		try {
			opusWrapper = new OpusWrapper();
			opusWrapper.getDecoder(AUDIO_SAMPLE_RATE, 1);
			opusWrapper.getEncoder(AUDIO_SAMPLE_RATE, 1, localBitrate);
		} catch (Exception e) {
			Logger.e(VoIPConstants.TAG, "Codec exception: " + e.toString());
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
	}
	
	private void startCodecDecompression() {
		
		new Thread(new Runnable() {
			
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

								if (resamplerEnabled && playbackSampleRate != AUDIO_SAMPLE_RATE) {
									// We need to resample the output signal
									byte[] output = resampler.reSample(packetData, 16, AUDIO_SAMPLE_RATE, playbackSampleRate);
									dp.write(output);
								} else {
									dp.write(packetData);
								}
								
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
								Logger.d(VoIPConstants.TAG, "samplesToDecodeQueue interrupted: " + e.toString());
								break;
							}
						}
					}
				}
			}
		}).start();
	}
	
	private void startCodecCompression() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				byte[] compressedData = new byte[OpusWrapper.OPUS_FRAME_SIZE * 10];
				int compressedDataLength = 0;
				while (keepRunning == true) {
					VoIPDataPacket dpencode = samplesToEncodeQueue.peek();
					if (dpencode != null) {
						samplesToEncodeQueue.poll();
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
								Logger.d(VoIPConstants.TAG, "samplesToEncodeQueue interrupted: " + e.toString());
								break;
							}
						}
					}
				}
			}
		}).start();
	}
	
	public void acceptIncomingCall() {
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.START_VOICE);
				sendPacket(dp, true);
			}
		}).start();

		startRecordingAndPlayback();
	}
	
	private void startRecordingAndPlayback() {

		if (audioStarted == true) {
			Logger.d(VoIPConstants.TAG, "Audio already started.");
			return;
		}
		
		Logger.d(VoIPConstants.TAG, "Starting audio record / playback.");
		startRecording();
		startPlayBack();
		partnerTimeoutThread.interrupt();
		stopRingtone();
		stopFromSoundPool(ringtoneStreamID);
		playFromSoundPool(SOUND_ACCEPT, false);
		sendHandlerMessage(VoIPActivity.MSG_AUDIO_START);
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
		}).start();
	}
	
	private void startRecording() {
		recordingThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

				AudioRecord recorder;
				int minBufSizeRecording = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
				Logger.d(VoIPConstants.TAG, "minBufSizeRecording: " + minBufSizeRecording);

				int audioSource = VoIPUtils.getAudioSource();
				recorder = new AudioRecord(audioSource, AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufSizeRecording);

				if (android.os.Build.VERSION.SDK_INT >= 16) {
					// Attach noise suppressor
					if (NoiseSuppressor.isAvailable()) {
						NoiseSuppressor ns = NoiseSuppressor.create(recorder.getAudioSessionId());
						Logger.d(VoIPConstants.TAG, "Initial NS status: " + ns.getEnabled());
						if (ns != null) ns.setEnabled(true);
					} else {
						Logger.d(VoIPConstants.TAG, "Noise suppression not available.");
					}
					
					// Attach echo cancellation
					if (AcousticEchoCanceler.isAvailable()) {
						AcousticEchoCanceler aec = AcousticEchoCanceler.create(recorder.getAudioSessionId());
						Logger.d(VoIPConstants.TAG, "Initial AEC status: " + aec.getEnabled());
						if (aec != null) aec.setEnabled(true);
					} else {
						Logger.d(VoIPConstants.TAG, "Echo cancellation not available.");
					}
					
					// Attach gain control
					if (AutomaticGainControl.isAvailable()) {
						AutomaticGainControl agc = AutomaticGainControl.create(recorder.getAudioSessionId());
						Logger.d(VoIPConstants.TAG, "Initial AGC status: " + agc.getEnabled());
						if (agc != null) agc.setEnabled(true);
					} else {
						Logger.d(VoIPConstants.TAG, "Automatic gain control not available.");
					}
				}
				
				// Start recording audio from the mic
				try {
					recorder.startRecording();
				} catch (IllegalStateException e) {
					Logger.d(VoIPConstants.TAG, "Recorder exception: " + e.toString());
				}
				
				// Start processing recorded data
				byte[] recordedData = new byte[minBufSizeRecording];
				int retVal;
            	int index = 0;
            	int newSize = 0;
				while (keepRunning == true) {
					retVal = recorder.read(recordedData, 0, recordedData.length);
					if (retVal != recordedData.length)
						Logger.w(VoIPConstants.TAG, "Unexpected recorded data length. Expected: " + recordedData.length + ", Recorded: " + retVal);
					if (mute == true)
						continue;

					// Break input audio into smaller chunks
                	while (index < retVal) {
                		if (retVal - index < OpusWrapper.OPUS_FRAME_SIZE * 2)
                			newSize = retVal - index;
                		else
                			newSize = OpusWrapper.OPUS_FRAME_SIZE * 2;

                		byte[] data = new byte[newSize];
                		byte[] withoutEcho = null;
                		System.arraycopy(recordedData, index, data, 0, newSize);
                		index += newSize;

						withoutEcho = data;
                		
	                	// Add it to the samples to encode queue
						VoIPDataPacket dp = new VoIPDataPacket(VoIPDataPacket.PacketType.VOICE_PACKET);
	                	dp.write(withoutEcho);

	                	synchronized (samplesToEncodeQueue) {
		                	samplesToEncodeQueue.add(dp);
	                		samplesToEncodeQueue.notify();
						}
                	}
					index = 0;
					
					if (Thread.interrupted()) {
						Logger.w(VoIPConstants.TAG, "Stopping recording thread.");
						break;
					}
				}
				
				// Stop recording
				if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
					recorder.stop();
				
				recorder.release();
			}
		});
		
		recordingThread.start();
	}
	
	private void startPlayBack() {
		
		playbackThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				byte[] silence = new byte[1000];
				final VoIPDataPacket voiceCache = new VoIPDataPacket(PacketType.VOICE_PACKET);
				voiceCache.setData(silence);

				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				int index = 0, size = 0;
				minBufSizePlayback = AudioTrack.getMinBufferSize(playbackSampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
				Logger.d(VoIPConstants.TAG, "minBufSizePlayback: " + minBufSizePlayback);
			
				setAudioModeInCall();
				audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, playbackSampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufSizePlayback, AudioTrack.MODE_STREAM);
				
				// Audiotrack monitor
				audioTrack.setPlaybackPositionUpdateListener(new OnPlaybackPositionUpdateListener() {
					
					@Override
					public void onPeriodicNotification(AudioTrack track) {
						// do nothing
					}
					
					@Override
					public void onMarkerReached(AudioTrack track) {
						// Logger.w(VoIPConstants.TAG, "Buffer underrun expected.");

						if (voiceCache != null) {
		                	synchronized (decodedBuffersQueue) {
			                	decodedBuffersQueue.add(voiceCache);
			                	decodedBuffersQueue.notify();
							}
						}

					}
				});
				
				try {
					audioTrack.play();
				} catch (IllegalStateException e) {
					Logger.e(VoIPConstants.TAG, "Audiotrack error: " + e.toString());
					hangUp();
				}
				
				while (keepRunning == true) {
					VoIPDataPacket dp = decodedBuffersQueue.peek();
					if (dp != null) {
						decodedBuffersQueue.poll();

						// audioTrack.write(dp.getData(), 0, dp.getLength());

						// For streaming mode, we must write data in chunks <= buffer size
						index = 0;
						while (index < dp.getLength()) {
							size = Math.min(minBufSizePlayback, dp.getLength() - index);
							audioTrack.write(dp.getData(), index, size);
							index += size; 
						}
						audioTrack.setNotificationMarkerPosition(audioTrack.getPlaybackHeadPosition() + (dp.getLength() / 2));

					} else {
						synchronized (decodedBuffersQueue) {
							try {
								decodedBuffersQueue.wait();
							} catch (InterruptedException e) {
								Logger.d(VoIPConstants.TAG, "decodedBuffersQueue interrupted: " + e.toString());
								break;
							}
						}
					}
					
					if (Thread.interrupted()) {
						Logger.w(VoIPConstants.TAG, "Stopping playback thread.");
						break;
					}
				}
				
				if (audioTrack != null) {
					audioTrack.pause();
					audioTrack.flush();
					audioTrack.release();
					audioTrack = null;
				}
			}
		});
		
		playbackThread.start();
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
								Logger.d(VoIPConstants.TAG, "encodedBuffersQueue interrupted: " + e.toString());
								break;
							}
						}
					}
				}
			}
		});
		
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
						// TODO
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
							clientPartner.setPreferredConnectionMethod(ConnectionMethods.PRIVATE);
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_SYNACK_PRIVATE);
							sendPacket(dp, false);
						}
						break;
						
					case COMM_UDP_SYN_PUBLIC:
						Logger.d(VoIPConstants.TAG, "Received " + dataPacket.getType());
						synchronized (clientPartner) {
							clientPartner.setPreferredConnectionMethod(ConnectionMethods.PUBLIC);
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_SYNACK_PUBLIC);
							sendPacket(dp, false);
						}
						break;
						
					case COMM_UDP_SYN_RELAY:
						Logger.d(VoIPConstants.TAG, "Received " + dataPacket.getType());
						synchronized (clientPartner) {
							clientPartner.setPreferredConnectionMethod(ConnectionMethods.RELAY);
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_SYNACK_RELAY);
							sendPacket(dp, false);
						}
						break;
						
					case COMM_UDP_SYNACK_PRIVATE:
					case COMM_UDP_ACK_PRIVATE:
						Logger.d(VoIPConstants.TAG, "Received " + dataPacket.getType());
						senderThread.interrupt();
						clientPartner.setPreferredConnectionMethod(ConnectionMethods.PRIVATE);
						if (connected) break;
						synchronized (clientPartner) {
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_ACK_PRIVATE);
							sendPacket(dp, true);
						}
						connected = true;
						break;
						
					case COMM_UDP_SYNACK_PUBLIC:
					case COMM_UDP_ACK_PUBLIC:
						Logger.d(VoIPConstants.TAG, "Received " + dataPacket.getType());
						senderThread.interrupt();
						clientPartner.setPreferredConnectionMethod(ConnectionMethods.PUBLIC);
						if (connected) break;
						synchronized (clientPartner) {
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_ACK_PUBLIC);
							sendPacket(dp, true);
						}
						connected = true;
						break;
						
					case COMM_UDP_SYNACK_RELAY:
					case COMM_UDP_ACK_RELAY:
						Logger.d(VoIPConstants.TAG, "Received " + dataPacket.getType());
						senderThread.interrupt();
						clientPartner.setPreferredConnectionMethod(ConnectionMethods.RELAY);
						if (connected) break;
						synchronized (clientPartner) {
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
						break;
						
					case ACK:
						removePacketFromAckWaitQueue(dataPacket.getPacketNumber());
						break;
						
					case ENCRYPTION_PUBLIC_KEY:
						if (clientSelf.isInitiator() == true) {
							Logger.e(VoIPConstants.TAG, "Was not expecting a public key.");
							continue;
						}
						Logger.d(VoIPConstants.TAG, "Received public key.");
						encryptor.setPublicKey(dataPacket.getData());
						encryptionStage = EncryptionStage.STAGE_GOT_PUBLIC_KEY;
						exchangeCryptoInfo();
						break;
						
					case ENCRYPTION_SESSION_KEY:
						if (clientSelf.isInitiator() != true) {
							Logger.e(VoIPConstants.TAG, "Was not expecting a session key.");
							continue;
						}
						encryptor.setSessionKey(encryptor.rsaDecrypt(dataPacket.getData()));
						Logger.d(VoIPConstants.TAG, "Received session key.");
						encryptionStage = EncryptionStage.STAGE_GOT_SESSION_KEY;
						exchangeCryptoInfo();
						break;
						
					case ENCRYPTION_RECEIVED_SESSION_KEY:
						Logger.d(VoIPConstants.TAG, "Encryption ready.");
						encryptionStage = EncryptionStage.STAGE_READY;
						break;
						
					case END_CALL:
						Logger.d(VoIPConstants.TAG, "Other party hung up.");
						stop();
						VoIPUtils.addMessageToChatThread(getApplicationContext(), clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_CALL_SUMMARY, getCallDuration(), -1);
						break;
						
					case START_VOICE:
						startRecordingAndPlayback();
						break;
						
					case CALL_DECLINED:
						sendHandlerMessage(VoIPActivity.MSG_OUTGOING_CALL_DECLINED);
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
					}
				}
			}
		});
		
		receivingThread.start();
	}
	
	private void sendPacket(VoIPDataPacket dp, boolean requiresAck) {
		
		if (dp == null)
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
		
		try {
			DatagramPacket packet = null;
			if (dp.getType() == PacketType.RELAY_INIT)
				packet = new DatagramPacket(packetData, packetData.length, InetAddress.getByName(clientSelf.getRelayAddress()), VoIPConstants.ICEServerPort);
			else
				packet = new DatagramPacket(packetData, packetData.length, clientPartner.getCachedInetAddress(), clientPartner.getPreferredPort());
				
//			Logger.d(VoIPConstants.TAG, "Sending type: " + dp.getType() + " to: " + clientPartner.getCachedInetAddress() + ":" + clientPartner.getPreferredPort());
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
		if (ackWaitQueue.isEmpty())
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
				if (encryptionStage == EncryptionStage.STAGE_INITIAL && clientSelf.isInitiator() == true) {
					// The initiator (caller) generates and sends a public key
					encryptor.initKeys();
					VoIPDataPacket dp = new VoIPDataPacket(PacketType.ENCRYPTION_PUBLIC_KEY);
					dp.write(encryptor.getPublicKey());
					sendPacket(dp, true);
					Logger.d(VoIPConstants.TAG, "Sending public key.");
				}

				if (encryptionStage == EncryptionStage.STAGE_GOT_PUBLIC_KEY && clientSelf.isInitiator() == false) {
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
					Logger.d(VoIPConstants.TAG, "Encryption ready.");
				}
			}
		}).start();
	}

	public int adjustBitrate(int delta) {
		if (delta > 0 && localBitrate + delta < 64000)
			localBitrate += delta;
		if (delta < 0 && localBitrate + delta >= 3000)
			localBitrate += delta;
		
		if (opusWrapper == null)
			return localBitrate;
		
		opusWrapper.setEncoderBitrate(localBitrate);
		sendHandlerMessage(VoIPActivity.MSG_CURRENT_BITRATE);
		
		return localBitrate;
	}
	
	public int getBitrate() {
		return localBitrate;
	}
	
	public void adjustGain(int gainDelta) {
		if (gainDelta > 0 && gain > 5000)
			return;
		if (gainDelta < 0 && gain < -5000)
			return;
		gain += gainDelta;
		opusWrapper.setDecoderGain(gain);
		
		// Save the gain preference
		SharedPreferences preferences = getSharedPreferences(HikeMessengerApp.VOIP_SETTINGS, Context.MODE_PRIVATE);
		Editor edit = preferences.edit();
		edit.putInt(HikeMessengerApp.VOIP_AUDIO_GAIN, gain);
		edit.commit();
	}
	
	public static boolean isConnected() {
		return connected;
	}
	
	public boolean isAudioRunning() {
		return audioStarted;
	}
	
	public void setHold(boolean hold) {
		this.hold = hold;
		Logger.d(VoIPConstants.TAG, "Changing hold to: " + hold);
		
		if (hold == true) {
			if (recordingThread != null)
				recordingThread.interrupt();
			if (playbackThread != null)
				playbackThread.interrupt();
		} else {
			// Coming off hold
			startRecording();
			startPlayBack();
		}
		
		sendHandlerMessage(VoIPActivity.MSG_UPDATE_HOLD_BUTTON);
	}	

	public boolean getHold()
	{
		return hold;
	}

	public void setSpeaker(boolean speaker)
	{
		this.speaker = speaker;
		if(audioManager!=null)
		{
			audioManager.setSpeakerphoneOn(speaker);
			Logger.d(VoIPConstants.TAG, "Speaker set to: " + speaker);
		}
	}

	public boolean getSpeaker()
	{
		return speaker;
	}

	private void playIncomingCallRingtone() {
		// Ringer
		Log.w(VoIPConstants.TAG, "Playing ringtone.");
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
	
	public void stopRingtone()
	{
		// Stop ringtone if playing
		if (ringtone != null && ringtone.isPlaying())
		{
			ringtone.stop();
			ringtone = null;
		}
		
		// stop vibrating
		if (vibrator != null) {
			vibrator.cancel();
			vibrator = null;
		}
	}
	
	public CallQuality getQuality() {
		return currentCallQuality;
	}
	
	public void retrieveExternalSocket() {

		clientSelf.setExternalIPAddress(null);
		clientSelf.setExternalPort(0);
		
		Thread iceThread = new Thread(new Runnable() {

			@Override
			public void run() {

				byte[] receiveData = new byte[10240];
				
				try {
					Logger.d(VoIPConstants.TAG, "Retrieving external socket information..");
					InetAddress host = InetAddress.getByName(VoIPConstants.ICEServerName);
					socket = new DatagramSocket();
					socket.setReuseAddress(true);
					socket.setSoTimeout(2000);
					
					/**
					 * If we are initiating the connection, then we set the relay server
					 * to be used by both clients. 
					 */
					if (clientSelf.isInitiator()) {
						clientSelf.setRelayAddress(host.getHostAddress());
					}

					VoIPDataPacket dp = new VoIPDataPacket(PacketType.RELAY_INIT);
					// byte[] dpData = VoIPSerializer.serialize(dp);
					// DatagramPacket outgoingPacket = new DatagramPacket(dpData, dpData.length, host, VoIPConstants.ICEServerPort);
//					sendPacket(dp, false);
					DatagramPacket incomingPacket = new DatagramPacket(receiveData, receiveData.length);

					boolean continueSending = true;
					int counter = 0;

					while (continueSending && keepRunning && (counter < 10 || reconnecting)) {
						counter++;
						try {
							clientSelf.setInternalIPAddress(VoIPUtils.getLocalIpAddress(getApplicationContext())); 
							clientSelf.setInternalPort(socket.getLocalPort());

//							Logger.d(VoIPConstants.TAG, "ICE Sending: " + outgoingPacket.getData().toString() + " to " + host.getHostAddress() + ":" + VoIPConstants.ICEServerPort);
							Logger.d(VoIPConstants.TAG, "ICE Sending.");
//							socket.send(outgoingPacket);
							sendPacket(dp, false);
							socket.receive(incomingPacket);
							
							String serverResponse = new String(incomingPacket.getData(), 0, incomingPacket.getLength());
							Logger.d(VoIPConstants.TAG, "ICE Received: " + serverResponse);
							setExternalSocketInfo(serverResponse);
//							Logger.w(VoIPConstants.TAG, "Setting continueSending to false");
							continueSending = false;
							
						} catch (SocketTimeoutException e) {
							Logger.d(VoIPConstants.TAG, "UDP timeout on ICE. #" + counter);
						} catch (IOException e) {
							Logger.d(VoIPConstants.TAG, "retrieveExternalSocket() IOException");
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

				} catch (SocketException e) {
					Logger.d(VoIPConstants.TAG, "SocketException: " + e.toString());
				} catch (UnknownHostException e) {
					Logger.d(VoIPConstants.TAG, "UnknownHostException: " + e.toString());
				}
				
				if (haveExternalSocketInfo()) {
					if (!clientSelf.isInitiator() || reconnecting || isNetworkGoodEnough()) {
						sendSocketInfoToPartner();
						if (socketInfoReceived)
							establishConnection();
						else
							startPartnerTimeoutThread();
					} else {
						if (keepRunning) {
							Logger.w(VoIPConstants.TAG, "Network is not good enough.");
							sendHandlerMessage(VoIPActivity.MSG_NETWORK_SUCKS);
						}
						stop();
						return;
					}
				} else {
					Logger.d(VoIPConstants.TAG, "Failed to retrieve external socket.");
					sendHandlerMessage(VoIPActivity.MSG_EXTERNAL_SOCKET_RETRIEVAL_FAILURE);
				}
			}
		});
		
		iceThread.start();
		
	}

	private void setExternalSocketInfo(String ICEResponse) throws JSONException {
		JSONObject jsonObject = new JSONObject(ICEResponse);
		clientSelf.setExternalIPAddress(jsonObject.getString("IP"));
		clientSelf.setExternalPort(Integer.parseInt(jsonObject.getString("Port")));
		Logger.d(VoIPConstants.TAG, "External socket - " + clientSelf.getExternalIPAddress() + ":" + clientSelf.getExternalPort());
		Logger.d(VoIPConstants.TAG, "Internal socket - " + clientSelf.getInternalIPAddress() + ":" + clientSelf.getInternalPort());
	}
	
	private boolean haveExternalSocketInfo() {
		if (clientSelf.getExternalIPAddress() != null && 
				!clientSelf.getExternalIPAddress().isEmpty() && 
				clientSelf.getExternalPort() > 0)
			return true;
		else
			return false;
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
			socketData.put("internalIP", clientSelf.getInternalIPAddress());
			socketData.put("internalPort", clientSelf.getInternalPort());
			socketData.put("externalIP", clientSelf.getExternalIPAddress());
			socketData.put("externalPort", clientSelf.getExternalPort());
			socketData.put("relay", clientSelf.getRelayAddress());
			socketData.put("callId", getCallId());
			socketData.put("initiator", clientSelf.isInitiator());
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
			
			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, message);
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
				for (int i = 0; i < numLoop; i++) {
					try {
						Thread.sleep(VoIPConstants.TIMEOUT_PARTNER_SOCKET_INFO / numLoop);
						sendSocketInfoToPartner();		// Retry sending socket info. 
					} catch (InterruptedException e) {
						Logger.d(VoIPConstants.TAG, "Timeout thread interrupted.");
						return;
					}
				}

				sendHandlerMessage(VoIPActivity.MSG_PARTNER_SOCKET_INFO_TIMEOUT);
				if (clientSelf.isInitiator()) {
					VoIPUtils.addMessageToChatThread(VoIPService.this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_OUTGOING, 0, -1);
					VoIPUtils.sendMissedCallNotificationToPartner(clientPartner);
				}
				stop();					
			}
		});
		
		partnerTimeoutThread.start();
	}
	
	/**
	 * Once socket information for the partner has been received, this
	 * function should be called to establish and verify a UDP connection.
	 */
	public void establishConnection() {
		
		if (!reconnecting && (establishingConnection || connected)) {
			Logger.w(VoIPConstants.TAG, "Already trying to establish connection.");
//			sendSocketInfoToPartner();
			return;
		}
		
		establishingConnection = true;
		connected = false;
		if (partnerTimeoutThread != null)
			partnerTimeoutThread.interrupt();
		Logger.d(VoIPConstants.TAG, "Trying to establish P2P connection..");
		Logger.d(VoIPConstants.TAG, "Listening on port: " + socket.getLocalPort());
		
		// Sender thread
		senderThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				int count = 0;
				while (keepRunning) {
					if (Thread.currentThread().isInterrupted())
						break;

					try {
						count++;
						VoIPDataPacket dp = null;
						if (count <= 15) {
							synchronized (clientPartner) {
								clientPartner.setPreferredConnectionMethod(ConnectionMethods.PRIVATE);
								dp = new VoIPDataPacket(PacketType.COMM_UDP_SYN_PRIVATE);
								sendPacket(dp, false);
								clientPartner.setPreferredConnectionMethod(ConnectionMethods.PUBLIC);
								dp = new VoIPDataPacket(PacketType.COMM_UDP_SYN_PUBLIC);
								sendPacket(dp, false);
//								Logger.d(VoIPConstants.TAG, "Sending private ping.");
							}
//							Logger.d(VoIPConstants.TAG, "Receiving thread status: " + receivingThread.getState());
							Thread.sleep(200);
						} else {
							synchronized (clientPartner) {
								clientPartner.setPreferredConnectionMethod(ConnectionMethods.RELAY);
								dp = new VoIPDataPacket(PacketType.COMM_UDP_SYN_RELAY);
								sendPacket(dp, false);
//								Logger.d(VoIPConstants.TAG, "Sending relay ping.");
							}
							Thread.sleep(500);
						}
					} catch (InterruptedException e) {
						Logger.d(VoIPConstants.TAG, "Stopping sending thread.");
						break;
					}
				}
			}
		});
		
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

				senderThread.interrupt();
//				receivingThread.interrupt();
				establishingConnection = false;
				if (reconnectingBeepsThread != null)
					reconnectingBeepsThread.interrupt();
				
				if (connected == true) {
					Logger.d(VoIPConstants.TAG, "UDP connection established :) " + clientPartner.getPreferredConnectionMethod());
					sendHandlerMessage(VoIPActivity.MSG_CONNECTION_ESTABLISHED);
					if (clientSelf.isInitiator() && !reconnecting) {
//						playOnSpeaker(R.raw.ring_tone, true);
						setAudioModeInCall();
						ringtoneStreamID = playFromSoundPool(SOUND_INCOMING_RINGTONE, true);
					} 

					try {
						if (!reconnecting) {
							startStreaming();
							startResponseTimeout();
						}
					} catch (Exception e) {
						Logger.e(VoIPConstants.TAG, "Exception: " + e.toString());
					}
					
					if (!clientSelf.isInitiator() && !reconnecting) {
						// We are receiving a call. 
						// VoIPService was started, and it established a connection. 
						// Now show the activity so the user can answer / decline the call. 
						Intent i = new Intent(getApplicationContext(), VoIPActivity.class);
						i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(i);
						playIncomingCallRingtone();
						// playOnSpeaker(R.raw.ringtone_incoming, true);
					}
					
					if (reconnecting) {
						sendHandlerMessage(VoIPActivity.MSG_RECONNECTED);
						// Give the heartbeat a chance to recover
						lastHeartbeat = System.currentTimeMillis() + 5000;
						startSendingAndReceiving();
						reconnecting = false;
					}
				} else {
					Logger.d(VoIPConstants.TAG, "UDP connection failure! :(");
					sendHandlerMessage(VoIPActivity.MSG_CONNECTION_FAILURE);
					if (!reconnecting) {
						if (clientSelf.isInitiator())
							VoIPUtils.addMessageToChatThread(VoIPService.this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_OUTGOING, 0, -1);
						else
							VoIPUtils.addMessageToChatThread(VoIPService.this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING, 0, -1);
					}
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
						sendHandlerMessage(VoIPActivity.MSG_PARTNER_ANSWER_TIMEOUT);
						if (!reconnecting && !connected) {
							if (!clientSelf.isInitiator())
								VoIPUtils.addMessageToChatThread(VoIPService.this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING, 0, -1);
							else
								VoIPUtils.addMessageToChatThread(VoIPService.this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_OUTGOING, 0, -1);
						}
						
						stop();
						
					}
				} catch (InterruptedException e) {
					// Do nothing, all is good
				}
			}
		});
		
		partnerTimeoutThread.start();
	}

	private boolean isNetworkGoodEnough() {
		boolean good = false;

		SharedPreferences prefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		int simulateBitrate = prefs.getInt(HikeMessengerApp.VOIP_BITRATE_2G, VoIPConstants.BITRATE_2G);

		final int TOTAL_TEST_TIME = 2;
		final int TOTAL_TEST_BYTES = (simulateBitrate * TOTAL_TEST_TIME) / 8;
		final int TEST_PACKETS = 35;
		final int TIME_TO_WAIT_FOR_PACKETS = 3;
		final int ACCEPTABLE_LOSS_PCT = 10;

		byte[] packetData = new byte[TOTAL_TEST_BYTES / TEST_PACKETS];
		VoIPDataPacket dp = new VoIPDataPacket(PacketType.NETWORK_QUALITY);
		dp.setData(packetData);
		startReceiving();
		
		clientPartner.setRelayAddress(clientSelf.getRelayAddress());
		clientPartner.setPreferredConnectionMethod(ConnectionMethods.RELAY);

		networkQualityPacketsReceived = 0;

		Logger.d(VoIPConstants.TAG, "Testing network with " + TEST_PACKETS + " packets of " + packetData.length + " bytes over " + TOTAL_TEST_TIME + " seconds.");
		
		try {
			for (int bytesSent = 0; bytesSent < TOTAL_TEST_BYTES && keepRunning; bytesSent += packetData.length) {
				sendPacket(dp, false);
				Thread.sleep((TOTAL_TEST_TIME * 1000) / TEST_PACKETS);
				if (Thread.currentThread().isInterrupted())
					break;
			}
			Thread.sleep(TIME_TO_WAIT_FOR_PACKETS);
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
}

