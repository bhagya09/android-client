package com.bsb.hike.voip;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.widget.Chronometer;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPConstants.CallQuality;
import com.bsb.hike.voip.VoIPConstants.CallStatus;
import com.bsb.hike.voip.VoIPDataPacket.PacketType;
import com.bsb.hike.voip.VoIPEncryptor.EncryptionStage;
import com.bsb.hike.voip.VoIPUtils.ConnectionClass;

public class VoIPClient  {		
	
	private final int PACKET_TRACKING_SIZE = 128;
	private final int HEARTBEAT_INTERVAL = 1000;
	private final int HEARTBEAT_TIMEOUT = 5000;
	private final int HEARTBEAT_HARD_TIMEOUT = 60000;
	
	private String phoneNumber;
	private String internalIPAddress, externalIPAddress;
	private int internalPort, externalPort;
	private String ourInternalIPAddress, ourExternalIPAddress;
	private int ourInternalPort, ourExternalPort;
	private String name;
	private boolean initiator, ender;
	private int version;
	private volatile ConnectionMethods preferredConnectionMethod = ConnectionMethods.UNKNOWN;
	private InetAddress cachedInetAddress = null;
	private String relayAddress;
	private int relayPort;
	private String tag = VoIPConstants.TAG;
	private int IceSocketTimeout;

	private Context context;
	private DatagramSocket socket = null;
	private OpusWrapper opusWrapper;
	private Thread iceThread = null, partnerSocketInfoTimeoutThread = null, responseTimeoutThread = null, senderThread = null, receivingThread = null;
	private Thread sendingThread = null;
	private Thread codecDecompressionThread = null, compressionThread = null;
	private boolean keepRunning = true;
	public boolean connected = false;
	public boolean reconnecting = false;
	private long lastReconnectAttemptAt = 0;
	private int currentPacketNumber = 0, rawVoiceSent = 0;
	public boolean socketInfoReceived = false, socketInfoSent = false;
	private boolean establishingConnection = false;
	private int totalBytesReceived = 0, totalBytesSent = 0;
	private int totalPacketsSent = 0, totalPacketsReceived = 0;
	private int audioPacketsReceivedPerSecond = 0;
	private int lastPacketReceived = 0;
	private Handler handler;
	private int previousHighestRemotePacketNumber = 0;
	private BitSet packetTrackingBits = new BitSet(PACKET_TRACKING_SIZE);
	private long lastHeartbeat;	
	public VoIPEncryptor encryptor = new VoIPEncryptor();
	public boolean cryptoEnabled = true;
	private VoIPEncryptor.EncryptionStage encryptionStage;
	public boolean remoteHold = false, remoteMute = false;
	public boolean audioStarted = false;
	private VoIPConstants.CallStatus currentCallStatus;
	private int localBitrate = VoIPConstants.BITRATE_WIFI, remoteBitrate = 0;
	private int chronoBackup = 0;
	private Chronometer chronometer = null;
	private int reconnectAttempts = 0;
	private int droppedDecodedPackets = 0;
	public int callSource = -1;
	private boolean isSpeaking = false, isRinging = false;
	private int voicePacketCount = 0;
	public boolean isDummy = false, isHost = false;		
	private String selfMsisdn;

	// Conference related
	public ArrayList<VoIPClient> clientMsisdns = new ArrayList<>();
	public boolean isHostingConference;
	public boolean isInAHostedConference;
	public String groupChatMsisdn;
	public boolean forceMute;
	
	// Audio quality
	private final int QUALITY_BUFFER_SIZE = 5;	// Quality is calculated over this many seconds
	private final int QUALITY_CALCULATION_FREQUENCY = 4;	// Quality is calculated every 'x' playback samples
	private BitSet playbackTrackingBits = new BitSet(VoIPConstants.AUDIO_SAMPLE_RATE * QUALITY_BUFFER_SIZE/ OpusWrapper.OPUS_FRAME_SIZE);
	private int playbackFeederCounter = 0;
	private CallQuality currentCallQuality = CallQuality.UNKNOWN;
	private int plcCounter = 0;
	private int packetLoss = 0, remotePacketLoss = 0;
	private long lastCongestionControlTimestamp = 0;
	private int audioFramesPerUDPPacket = 1;
	private int bitrateAdjustment = 0;
	private ConnectionClass localConnectionClass = ConnectionClass.Unknown;
	private int frameHits, frameMisses;
	
	private final ConcurrentHashMap<Integer, VoIPDataPacket> ackWaitQueue		 = new ConcurrentHashMap<Integer, VoIPDataPacket>();
	private final LinkedBlockingQueue<VoIPDataPacket> samplesToDecodeQueue     = new LinkedBlockingQueue<VoIPDataPacket>();
	private final LinkedBlockingQueue<VoIPDataPacket> buffersToSendQueue      = new LinkedBlockingQueue<VoIPDataPacket>();
	private final ConcurrentLinkedQueue<VoIPDataPacket> decodedBuffersQueue      = new ConcurrentLinkedQueue<VoIPDataPacket>();
	private final LinkedBlockingQueue<VoIPDataPacket> samplesToEncodeQueue      = new LinkedBlockingQueue<VoIPDataPacket>();
	
	// Simulate packet loss
	private int simulatedPacketLossPercentage = 0;
	
	public enum ConnectionMethods {
		UNKNOWN,
		PRIVATE,
		PUBLIC,
		RELAY
	}
	
	public VoIPClient(Context context, Handler handler) {
		super();
		this.context = context;
		this.handler = handler;
		encryptionStage = EncryptionStage.STAGE_INITIAL;
		currentCallQuality = CallQuality.UNKNOWN;
		setCallStatus(VoIPConstants.CallStatus.UNINITIALIZED);

		if (context != null)
			selfMsisdn =  Utils.getUserContactInfo(context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE)).getMsisdn();
	}

	public String getName() {
		return name;
	}
	
	public String getPhoneNumber() {
		return phoneNumber;
	}
	
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
		
		// Get name from MSISDN
		ContactInfo contactInfo = ContactManager.getInstance().getContact(phoneNumber);
		if (contactInfo == null) {
//			Logger.d(tag, "Unable to retrieve contact info.");
			name = phoneNumber;
		} else {
			name = contactInfo.getNameOrMsisdn();
		}
		
		tag = VoIPConstants.TAG + " <" + name + ">";
	}
	
	public String getInternalIPAddress() {
		return internalIPAddress;
	}
	
	public void setInternalIPAddress(String internalIPAddress) {
		this.internalIPAddress = internalIPAddress;
		cachedInetAddress = null;
	}
	
	public String getExternalIPAddress() {
		return externalIPAddress;
	}
	
	public void setExternalIPAddress(String externalIPAddress) {
		this.externalIPAddress = externalIPAddress;
		cachedInetAddress = null;
	}
	
	public int getInternalPort() {
		return internalPort;
	}
	
	public void setInternalPort(int internalPort) {
		this.internalPort = internalPort;
	}
	
	public int getExternalPort() {
		return externalPort;
	}
	
	public void setExternalPort(int externalPort) {
		this.externalPort = externalPort;
	}
	
	public boolean isInitiator() {
		return initiator;
	}
	
	public void setInitiator(boolean initiator) {
		this.initiator = initiator;
	}

	public boolean isEnder(){
		return ender;
	}

	public void setEnder(boolean ender) {
		this.ender = ender;
	}
	
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public ConnectionMethods getPreferredConnectionMethod() {
		return preferredConnectionMethod;
	}
	
	public void setPreferredConnectionMethod(
			ConnectionMethods preferredConnectionMethod) {
		this.preferredConnectionMethod = preferredConnectionMethod;
		cachedInetAddress = null;
		// Logger.d(logTag, "Setting preferred connection method to: " + preferredConnectionMethod.toString());
	}
	
	public String getPreferredIPAddress() {
		String ip;
		if (preferredConnectionMethod == ConnectionMethods.PRIVATE)
			ip = getInternalIPAddress();
		else if (preferredConnectionMethod == ConnectionMethods.PUBLIC)
			ip = getExternalIPAddress();
		else
			ip = relayAddress;
		return ip;
	}
	
	public int getPreferredPort() {
		int port;
		if (preferredConnectionMethod == ConnectionMethods.PRIVATE)
			port = getInternalPort();
		else if (preferredConnectionMethod == ConnectionMethods.PUBLIC)
			port = getExternalPort();
		else
			port = relayPort;
		return port;
	}
	
	public InetAddress getCachedInetAddress() {
		if (cachedInetAddress == null) {
			try {
//				Log.d(VoIPActivity.logTag, "preferred address: " + getPreferredIPAddress());
				cachedInetAddress = InetAddress.getByName(getPreferredIPAddress());
			} catch (UnknownHostException e) {
				Logger.e(tag, "VoIPClient UnknownHostException: " + e.toString());
			}
		}
		// Log.d(VoIPActivity.logTag, "cached address: " + cachedInetAddress.toString());
		return cachedInetAddress;
	}

	public String getRelayAddress() {
		return relayAddress;
	}
	
	public void setRelayAddress(String relayAddress) {
		this.relayAddress = relayAddress;
	}

	public int getRelayPort() {
		return relayPort;
	}

	public void setRelayPort(int relayPort) {
		this.relayPort = relayPort;
	}

	public String getOurInternalIPAddress() {
		return ourInternalIPAddress;
	}

	public void setOurInternalIPAddress(String ourInternalIPAddress) {
		this.ourInternalIPAddress = ourInternalIPAddress;
	}

	public String getOurExternalIPAddress() {
		return ourExternalIPAddress;
	}

	public void setOurExternalIPAddress(String ourExternalIPAddress) {
		this.ourExternalIPAddress = ourExternalIPAddress;
	}

	public int getOurInternalPort() {
		return ourInternalPort;
	}

	public void setOurInternalPort(int ourInternalPort) {
		this.ourInternalPort = ourInternalPort;
	}

	public int getOurExternalPort() {
		return ourExternalPort;
	}

	public void setOurExternalPort(int ourExternalPort) {
		this.ourExternalPort = ourExternalPort;
	}

	public boolean isSpeaking() {
		return isSpeaking;
	}

	public synchronized void setSpeaking(boolean isSpeaking) {
		if (this.isSpeaking != isSpeaking) {
//			Logger.d(tag, "Speaking: " + isSpeaking);
			this.isSpeaking = isSpeaking;
			sendHandlerMessage(VoIPConstants.MSG_UPDATE_SPEAKING);
		}
	}

	public synchronized void removeExternalSocketInfo() {
		setOurExternalIPAddress(null);
		setOurExternalPort(0);
		if (socket != null) {
			socket.close();
			socket = null;
		}
	}
	
	/**
	 * Every call to this function will increase the response timeout by one second. 
	 * A default long timeout (like 10s) is not a good idea since UDP packets can get lost
	 * and we should retry quickly to reduce call patching time. <br/>
	 * Hence, a compromise is to keep a short initial timeout, but increase it with every failure. 
	 */
	private synchronized void getNewSocket() {
		try {
			socket = new DatagramSocket();
			socket.setReuseAddress(true);
			socket.setSoTimeout((IceSocketTimeout++) * 1000);
			
			setOurInternalIPAddress(VoIPUtils.getLocalIpAddress(context)); 
			setOurInternalPort(socket.getLocalPort());
		} catch (SocketException e) {
			Logger.d(tag, "getNewSocket() SocketException: " + e.toString());
		}
	}
	
	/**
	 * Initiates a call to this client. <br/>
	 * Step one of call initiation is to retrieve your public ip:port
	 * using our version of ICE. 
	 */
	public void retrieveExternalSocket() {

		IceSocketTimeout = VoIPConstants.INITIAL_ICE_SOCKET_TIMEOUT;
		keepRunning = true;
		socketInfoSent = false;
		
		iceThread = new Thread(new Runnable() {

			@Override
			public void run() {

				removeExternalSocketInfo();
				
				byte[] receiveData = new byte[10240];
				
				Logger.d(tag, "Retrieving external socket information..");
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.RELAY_INIT);
				DatagramPacket incomingPacket = new DatagramPacket(receiveData, receiveData.length);

				boolean continueSending = true;
				int counter = 0;

				getNewSocket();

				while (continueSending && keepRunning && (counter < 10 || reconnecting)) {
					counter++;
					try {
						/**
						 * If we are initiating the connection, then we set the relay server
						 * to be used by both clients. 
						 * Also check if the relay has already been set (in case of conferences)
						 */
						if (!isInitiator() && TextUtils.isEmpty(getRelayAddress())) {
							InetAddress host = null;
							try {
								host = InetAddress.getByName(VoIPConstants.ICEServerName);
							} catch (UnknownHostException e) {
								// Fall back to hardcoded IPs
								Logger.w(tag, "UnknownHostException while retrieving relay host.");
								host = VoIPUtils.getRelayIpFromHardcodedAddresses();
							}
							
							if (host == null) {
								Logger.e(tag, "Unable to get relay server's IP address.");
								return;
							}
							
							setRelayAddress(host.getHostAddress());
							setRelayPort(VoIPUtils.getRelayPort(context));
						}

						Logger.d(tag, "ICE Sending.");
						sendPacket(dp, false);
						
						if (socket == null)
							return;
						
						socket.receive(incomingPacket);
						
						String serverResponse = new String(incomingPacket.getData(), 0, incomingPacket.getLength());
						Logger.d(tag, "ICE Received: " + serverResponse);
						setExternalSocketInfo(serverResponse);
						continueSending = false;
						
					} catch (SocketTimeoutException e) {
						Logger.d(tag, "UDP timeout on ICE. #" + counter + ". New timeout: " + IceSocketTimeout);
						getNewSocket();
					} catch (IOException e) {
						Logger.d(tag, "retrieveExternalSocket() IOException" + e.toString());
						try {
							Thread.sleep(500);
						} catch (InterruptedException e1) {
							Logger.d(tag, "Waiting for external socket info interrupted.");
						}
					} catch (JSONException e) {
						Logger.d(tag, "JSONException: " + e.toString());
						continueSending = true;
					}
				}
				
				if (haveExternalSocketInfo()) {
					sendSocketInfoToPartner();
					if (socketInfoReceived)
						establishConnection();
					else
						startPartnerSocketInfoTimeoutThread();
				} else {
					if (!Thread.currentThread().isInterrupted()) {
						Logger.d(tag, "Failed to retrieve external socket.");
						sendHandlerMessage(VoIPConstants.MSG_EXTERNAL_SOCKET_RETRIEVAL_FAILURE);
						sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.EXTERNAL_SOCKET_RETRIEVAL_FAILURE);
						stop();
					}
				}
			}
		}, "ICE_THREAD");
		
		iceThread.start();
		
	}

	public boolean haveExternalSocketInfo() {
		
		if (socket == null)
			return false;
		
		if (getOurExternalIPAddress() != null && 
				!getOurExternalIPAddress().isEmpty() && 
				getOurExternalPort() > 0)
			return true;
		else
			return false;
	}
	
	public void sendSocketInfoToPartner() {
		if (getPhoneNumber() == null || getPhoneNumber().isEmpty()) {
			Logger.e(tag, "Have no partner info. Quitting.");
			return;
		}

		if (!haveExternalSocketInfo()) {
			Logger.d(tag, "Can't send socket info (don't have it!)");
			return;
		}
		
		try {
			JSONObject socketData = new JSONObject();
			socketData.put(VoIPConstants.Extras.INTERNAL_IP, getOurInternalIPAddress());
			socketData.put(VoIPConstants.Extras.INTERNAL_PORT, getOurInternalPort());
			socketData.put(VoIPConstants.Extras.EXTERNAL_IP, getOurExternalIPAddress());
			socketData.put(VoIPConstants.Extras.EXTERNAL_PORT, getOurExternalPort());
			socketData.put(VoIPConstants.Extras.RELAY, getRelayAddress());
			socketData.put(VoIPConstants.Extras.RELAY_PORT, getRelayPort());
			socketData.put(VoIPConstants.Extras.CALL_ID, VoIPService.getCallId());
			socketData.put(VoIPConstants.Extras.INITIATOR, !isInitiator());
			socketData.put(VoIPConstants.Extras.RECONNECTING, reconnecting);
			socketData.put(VoIPConstants.Extras.VOIP_VERSION, VoIPConstants.VOIP_VERSION);
			
			if (!TextUtils.isEmpty(groupChatMsisdn) && !isInitiator())
				socketData.put(VoIPConstants.Extras.GROUP_CHAT_MSISDN, groupChatMsisdn);
			
			if (isInAHostedConference)
				socketData.put(VoIPConstants.Extras.CONFERENCE, true);
			
			JSONObject data = new JSONObject();
			data.put(HikeConstants.MESSAGE_ID, new Random().nextInt(10000));
			data.put(HikeConstants.TIMESTAMP, System.currentTimeMillis() / 1000); 
			data.put(HikeConstants.METADATA, socketData);

			JSONObject message = new JSONObject();
			message.put(HikeConstants.TO, getPhoneNumber());
			message.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE_VOIP_0);
			message.put(HikeConstants.SUB_TYPE, HikeConstants.MqttMessageTypes.VOIP_SOCKET_INFO);
			message.put(HikeConstants.DATA, data);
			
			HikeMqttManagerNew.getInstance().sendMessage(message, MqttConstants.MQTT_QOS_ZERO);
			Logger.d(tag, "Sent socket information to partner. Reconnecting: " + reconnecting);
			socketInfoSent = true;

		} catch (JSONException e) {
			e.printStackTrace();
			Logger.w(tag, "sendSocketInfoToPartner JSON error: " + e.toString());
		} 
		
	}
	
	private void startHeartbeat() {
		
		// Heartbeat, and other housekeeping
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				lastHeartbeat = System.currentTimeMillis();

				while (keepRunning == true) {
					
					// Send heartbeat packet
					// Include packets received / second info
					VoIPDataPacket dp = new VoIPDataPacket(PacketType.HEARTBEAT);
					if (version >= 3)
						dp.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(packetLoss).array());
					else
						dp.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(audioPacketsReceivedPerSecond).array());
					audioPacketsReceivedPerSecond = 0;
					sendPacket(dp, false);

					if (System.currentTimeMillis() - lastHeartbeat > HEARTBEAT_TIMEOUT && !reconnecting) {
//						Logger.w(logTag, "Heartbeat failure. Reconnecting.. ");
						startReconnectBeeps();
						if (connected) {
							if (!isInitiator())
								reconnect();
							else {
								Logger.w(tag, "Requesting a reconnect..");
								VoIPDataPacket packet = new VoIPDataPacket(PacketType.REQUEST_RECONNECT);
								sendPacket(packet, false);
							}
						} 
					}
					
					if (System.currentTimeMillis() - lastHeartbeat > HEARTBEAT_HARD_TIMEOUT) {
						if (reconnectForConference()) {
							Thread.currentThread().interrupt();
							return;
						}
						Logger.w(tag, "Giving up on connection.");
						hangUp();
						break;
					}
					
					sendPacketsWaitingForAck();
					
					// Drop packets if getting left behind
					while (samplesToEncodeQueue.size() > VoIPConstants.MAX_SAMPLES_BUFFER) {
						Logger.d(tag, "Dropping to_encode packet.");
						samplesToEncodeQueue.poll();
					}
					
					while (samplesToDecodeQueue.size() > VoIPConstants.MAX_SAMPLES_BUFFER) {
						Logger.d(tag, "Dropping to_decode packet.");
						samplesToDecodeQueue.poll();
					}
					
					while (buffersToSendQueue.size() > VoIPConstants.MAX_SAMPLES_BUFFER) {
						Logger.d(tag, "Dropping encoded packet.");
						buffersToSendQueue.poll();
					}

					while (decodedBuffersQueue.size() > VoIPConstants.MAX_SAMPLES_BUFFER) {
//						Logger.d(logTag, "Dropping decoded packet.");
						droppedDecodedPackets++;
						decodedBuffersQueue.poll();
					}

					try {
						Thread.sleep(HEARTBEAT_INTERVAL);
					} catch (InterruptedException e) {
						Logger.d(tag, "Heartbeat InterruptedException: " + e.toString());
					}
					
				}
			}
		}, "LISTEN_HEART_BEAT_THREAD").start();
		
	}
	
	/**
	 * Reconnect after a communications failure.
	 */
	public void reconnect() {

		if (reconnecting)
			return;

		if (System.currentTimeMillis() - lastReconnectAttemptAt < VoIPConstants.RECONNECT_THRESHOLD)
			return;
		
		reconnecting = true;
		reconnectAttempts++;
		lastReconnectAttemptAt = System.currentTimeMillis();
		Logger.w(tag, "Reconnecting..");

		// Interrupt the receiving thread since we will make the socket null
		// and it could throw an NPE.
		if (receivingThread != null)
			receivingThread.interrupt();
		
		setCallStatus(VoIPConstants.CallStatus.RECONNECTING);
		sendHandlerMessage(VoIPConstants.MSG_RECONNECTING);
		socketInfoReceived = false;
		connected = false;
		retrieveExternalSocket();
		startReconnectBeeps();
	}
	
	
	
	/**
	 * Once socket information for the partner has been received, this
	 * function should be called to establish and verify a UDP connection.
	 */
	public void establishConnection() {
		
		if (establishingConnection) {
			Logger.w(tag, "Already trying to establish connection.");
			return;
		}
		
		if (socket == null) {
			Logger.w(tag, "establishConnection() called with null socket.");
			stop();
			return;
		}
		
		if (!socketInfoSent) {
			Logger.w(tag, "Can't establish connection since we haven't sent socket info yet.");
			return;
		}
		
		if (partnerSocketInfoTimeoutThread != null)
			partnerSocketInfoTimeoutThread.interrupt();

		setPreferredConnectionMethod(ConnectionMethods.UNKNOWN);
		establishingConnection = true;
		connected = false;
		Logger.d(tag, "Trying to establish UDP connection..");
		
		// Sender thread
		senderThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (keepRunning) {
					if (Thread.currentThread().isInterrupted())
						break;

					try {
						VoIPDataPacket dp = null;
						synchronized (VoIPClient.this) {
							ConnectionMethods currentMethod = getPreferredConnectionMethod();

							setPreferredConnectionMethod(ConnectionMethods.PRIVATE);
							dp = new VoIPDataPacket(PacketType.COMM_UDP_SYN_PRIVATE);
							sendPacket(dp, false);
							setPreferredConnectionMethod(ConnectionMethods.PUBLIC);
							dp = new VoIPDataPacket(PacketType.COMM_UDP_SYN_PUBLIC);
							sendPacket(dp, false);
							setPreferredConnectionMethod(ConnectionMethods.RELAY);
							dp = new VoIPDataPacket(PacketType.COMM_UDP_SYN_RELAY);
							sendPacket(dp, false);

							setPreferredConnectionMethod(currentMethod);
						}
						Thread.sleep(250);
					} catch (InterruptedException e) {
						Logger.d(tag, "Stopping sending thread.");
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

				establishingConnection = false;
				stopReconnectBeeps();
				if (senderThread != null)
					senderThread.interrupt();
				
				if (connected == true) {
					Logger.d(tag, "UDP connection established :) " + getPreferredConnectionMethod());
					connectionEstablished();

					if (reconnecting) {
						setInitialCallStatus();
						sendHandlerMessage(VoIPConstants.MSG_RECONNECTED);
						// Give the heartbeat a chance to recover
						lastHeartbeat = System.currentTimeMillis() + 5000;
						startSendingAndReceiving();
						reconnecting = false;
					} else {
						if (!isInitiator())
							startResponseTimeout();
						startStreaming();
						sendHandlerMessage(VoIPConstants.MSG_CONNECTED);
					}
				} else {
					Logger.d(tag, "UDP connection failure! :(");
					if (!reconnectForConference()) {
						sendHandlerMessage(VoIPConstants.MSG_CONNECTION_FAILURE);
						sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.UDP_CONNECTION_FAIL);
						stop();
					}
				}
			}
		}).start();

	}

	/**
	 * Once a connection has been made to the call recipient (and
	 * presumably their phone is ringing), wait for a definite amount of
	 * time for the call to be answered / rejected. 
	 */
	private void startResponseTimeout() {
		responseTimeoutThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(VoIPConstants.TIMEOUT_PARTNER_ANSWER);
					if (!isAudioRunning()) {
						// Call not answered yet?
						sendHandlerMessage(VoIPConstants.MSG_PARTNER_ANSWER_TIMEOUT);
						sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_PARTNER_ANSWER_TIMEOUT);
						// Sleep for a little bit before destroying this object
						// since the call failure screen will need its info. 
						Thread.sleep(500);

						// Edge case error fixing. If the call went into reconnection
						// before it was answered, then normally no outgoing missed call
						// would appear in our chat thread since we aren't connected.
						// Hence, make it appear as if we ARE connected, so the missed call appears. 
						connected = true;
						stop();
					}
				} catch (InterruptedException e) {
					// Do nothing, all is good
				}
			}
		}, "PARTNER_TIMEOUT_THREAD");
		
		responseTimeoutThread.start();
	}

	public void startSendingAndReceiving() {
		
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
				ArrayList<VoIPDataPacket> stagingQueue = new ArrayList<>(); 
				
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				while (keepRunning == true) {

					if (Thread.interrupted()) 
						break;

					try {
						VoIPDataPacket dp = buffersToSendQueue.take();

						// Encrypt packet
						if (cryptoEnabled && encryptionStage == EncryptionStage.STAGE_READY) {
							byte[] origData = dp.getData();
							dp.write(encryptor.aesEncrypt(origData));
							dp.setEncrypted(true);
						}

						stagingQueue.add(dp);
					} catch (InterruptedException e) {
						break;
					}
					
					// Check if we have enough buffers to send
					if (stagingQueue.size() >= audioFramesPerUDPPacket) {
						if (audioFramesPerUDPPacket == 1) {
							sendPacket(stagingQueue.get(0), false);
						} else {
							// Combine multiple frames into one packet
							VoIPDataPacket jumbo = new VoIPDataPacket(PacketType.MULTIPLE_AUDIO_PACKETS);
							for (VoIPDataPacket dp : stagingQueue) {
								
								if (dp.isVoice())
									jumbo.setVoice(true);
								if (dp.isEncrypted())
									jumbo.setEncrypted(true);
								
								jumbo.addToDataList(dp.getData());
							}
							sendPacket(jumbo, false);
						}
						
						stagingQueue.clear();
					}
				}
			}
		}, "VOIP_SEND_THREAD");
		
		sendingThread.start();
	}
	
	
	public void close() {

		Logger.d(tag,
				"===== Call Summary (" + getPhoneNumber() + ") =====" +
				"\nBytes sent / received: " + totalBytesSent + " / " + totalBytesReceived +
				"\nPackets sent / received: " + totalPacketsSent + " / " + totalPacketsReceived +
				"\nFrame hits: " + frameHits + ", misses: " + frameMisses + " (" + (frameMisses * 100 / (frameHits + frameMisses + 1)) +"%)" +
				"\nPure voice bytes: " + rawVoiceSent +
				"\nDropped decoded packets: " + droppedDecodedPackets +
				"\nReconnect attempts: " + reconnectAttempts +
				"\nCall duration: " + getCallDuration() + " seconds \n" +
				"=========================================");
		
		if (getCallStatus() != VoIPConstants.CallStatus.PARTNER_BUSY) {
			setCallStatus(VoIPConstants.CallStatus.ENDED);
		}

		// Call summary in chat thread
		if (connected || getCallDuration() > 0) {
			if (TextUtils.isEmpty(groupChatMsisdn)) {
				VoIPUtils.addMessageToChatThread(context, VoIPClient.this, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_CALL_SUMMARY, getCallDuration(), -1, true);
			}
			else {
				if (isHostingConference) {
					// Hack!
					// Replacing the client msisdn with group chat msisdn, so that the call summary
					// goes in the right place
					setPhoneNumber(groupChatMsisdn);
					VoIPUtils.addMessageToChatThread(context, VoIPClient.this, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_CALL_SUMMARY, getCallDuration(), -1, true);
				}
			}

		}

		if (iceThread != null)
			iceThread.interrupt();

		if (partnerSocketInfoTimeoutThread != null)
			partnerSocketInfoTimeoutThread.interrupt();
		
		if (responseTimeoutThread != null)
			responseTimeoutThread.interrupt();
		
		if (receivingThread != null)
			receivingThread.interrupt();

		if (sendingThread != null)
			sendingThread.interrupt();

		if (compressionThread != null)
			compressionThread.interrupt();
		
		if (codecDecompressionThread != null)
			codecDecompressionThread.interrupt();
		
		sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_END);
		
		synchronized (VoIPClient.this) {
			if (chronometer != null) {
				chronometer.stop();
				chronometer = null;
			}

			if (opusWrapper != null) {
				opusWrapper.destroy();
				opusWrapper = null;
			}
		}
		
		if(reconnecting) {
			sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_DROP);
		}
		
		// send a call rejected message through hike as well
		VoIPUtils.sendVoIPMessageUsingHike(getPhoneNumber(), 
				HikeConstants.MqttMessageTypes.VOIP_CALL_CANCELLED, 
				VoIPService.getCallId(), false);
		
		keepRunning = false;
		socketInfoReceived = false;
		establishingConnection = false;
		stopReconnectBeeps();
		connected = false;
		audioStarted = false;
		reconnecting = false;
		removeExternalSocketInfo();
		
		ackWaitQueue.clear();
		samplesToDecodeQueue.clear();
		buffersToSendQueue.clear();
		decodedBuffersQueue.clear();
		samplesToEncodeQueue.clear();
	}

	private void setExternalSocketInfo(String ICEResponse) throws JSONException {
		JSONObject jsonObject = new JSONObject(ICEResponse);
		setOurExternalIPAddress(jsonObject.getString("IP"));
		setOurExternalPort(Integer.parseInt(jsonObject.getString("Port")));
		Logger.d(tag, "External socket - " + getOurExternalIPAddress() + ":" + getOurExternalPort());
		Logger.d(tag, "Internal socket - " + getOurInternalIPAddress() + ":" + getOurInternalPort());
	}

	/**
	 * Wait for partner to send us their socket information
	 * Set timeout so we don't wait indefinitely
	 */
	private void startPartnerSocketInfoTimeoutThread() {
		
		final int numLoop = 5;
		partnerSocketInfoTimeoutThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				for (int i = 0; (i < numLoop || reconnecting) && keepRunning; i++) {
					try {
						Thread.sleep(VoIPConstants.TIMEOUT_PARTNER_SOCKET_INFO / numLoop);
						sendSocketInfoToPartner();		// Retry sending socket info. 
					} catch (InterruptedException e) {
						return;
					}
				}

				if (reconnectForConference()) 
					return;
				
				sendHandlerMessage(VoIPConstants.MSG_PARTNER_SOCKET_INFO_TIMEOUT);
				if (!isInitiator() && !reconnecting) {
					VoIPUtils.sendMissedCallNotificationToPartner(getPhoneNumber(), groupChatMsisdn);
				}
				sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.PARTNER_SOCKET_INFO_TIMEOUT);
				stop();					
			}
		}, "PARTNER_TIMEOUT_THREAD");
		
		partnerSocketInfoTimeoutThread.start();
	}
	
	public synchronized void sendPacket(VoIPDataPacket dp, boolean requiresAck) {
		
		if (dp.getType() != PacketType.ACK && dp.getPacketNumber() == 0)
			dp.setPacketNumber(currentPacketNumber++);
		
		dp.setRequiresAck(requiresAck);
		dp.setTimestamp(System.currentTimeMillis());
		
		if (requiresAck == true)
			addPacketToAckWaitQueue(dp);

		if (dp == null || keepRunning == false)
			return;
		
		if (socket == null) {
			Logger.d(tag, "Socket is null.");
			return;
		}
		
		// While reconnecting don't send anything except for connection setup packets
		if (reconnecting) {
			if (!dp.getPacketType().toString().startsWith("COMM") && dp.getPacketType() != PacketType.RELAY_INIT)
				return;
		}
		
		if (getPreferredConnectionMethod() == ConnectionMethods.RELAY) {
			dp.setDestinationIP(getExternalIPAddress());
			dp.setDestinationPort(getExternalPort());
		}
		
		if (dp.getType() == PacketType.AUDIO_PACKET)
			rawVoiceSent += dp.getLength();
		
		if (dp.getType() == PacketType.AUDIO_PACKET) {
			// Voice packet numbers are disabled for conferences
			if (!isInAHostedConference && version >= 2)
				dp.setVoicePacketNumber(voicePacketCount++);
		}
		
		// Serialize everything except for P2P voice data packets
		byte[] packetData = VoIPUtils.getUDPDataFromPacket(dp);
		
		if (packetData == null)
			return;

		// Simulated packet loss
		if (simulatedPacketLossPercentage > 0 ) {
			if (new Random().nextInt(100) < simulatedPacketLossPercentage) {
				Logger.d(tag, "Oops. I'm going to lose a packet on purpose.");
				return;
			}
		}	
		
		try {
			DatagramPacket packet = null;
			if (dp.getType() == PacketType.RELAY_INIT)
				packet = new DatagramPacket(packetData, packetData.length, InetAddress.getByName(getRelayAddress()), getRelayPort());
			else
				packet = new DatagramPacket(packetData, packetData.length, getCachedInetAddress(), getPreferredPort());
				
			socket.send(packet);
			
			totalBytesSent += packet.getLength();
			totalPacketsSent++;
		} catch (IOException e) {
			Logger.w(tag, "sendPacket() IOException: " + e.toString());
		}
		
	}
	
	private void addPacketToAckWaitQueue(VoIPDataPacket dp) {
		synchronized (ackWaitQueue) {
			if (ackWaitQueue.containsKey(dp.getPacketNumber()))
				return;

			ackWaitQueue.put(dp.getPacketNumber(), dp);
		}
	}
	
	private void removePacketFromAckWaitQueue(int packetNumber) {
		synchronized (ackWaitQueue) {
			ackWaitQueue.remove(packetNumber);
		}
	}
	
	private void sendPacketsWaitingForAck() {
		if (ackWaitQueue.isEmpty() || !connected)
			return;
		
		synchronized (ackWaitQueue) {
			long currentTime = System.currentTimeMillis();
			for (VoIPDataPacket dp : ackWaitQueue.values()) {
				if (dp.getTimestamp() < currentTime - 1000) {	// Give each packet 1 second to get ack
					Logger.d(tag, "Resending packet: " + dp.getType());
					sendPacket(dp, true);
				}
			}
		}		
	}
	
	private void sendHandlerMessage(int message) {
		sendHandlerMessage(message, null);
	}
	
	private void sendHandlerMessage(int message, Bundle bundle) {
		
		if (bundle == null)
			bundle = new Bundle();

		bundle.putString(VoIPConstants.MSISDN, getPhoneNumber());
		Message msg = Message.obtain();
		msg.what = message;
		msg.setData(bundle);
		if (handler != null)
			handler.sendMessage(msg);
	}
	

	public void startReceiving() {
		if (receivingThread != null) {
			receivingThread.interrupt();
		}
		
		receivingThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
//				Logger.w(logTag, "Receiving thread starting and listening on: " + socket.getLocalPort());
				byte[] buffer = new byte[50000];
				while (keepRunning == true) {

					if (Thread.currentThread().isInterrupted()) {
						break;
					}
					
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					try {
						socket.setSoTimeout(0);
						socket.receive(packet);
						totalBytesReceived += packet.getLength();
						totalPacketsReceived++;
//						Logger.d(tag, "Received " + packet.getLength() + " bytes.");
						
					} catch (IOException e) {
						break;
					}
					
					byte[] realData = new byte[packet.getLength()];
					System.arraycopy(packet.getData(), 0, realData, 0, packet.getLength());
					VoIPDataPacket dataPacket = VoIPUtils.getPacketFromUDPData(realData);
					
					if (dataPacket == null)
						continue;
					
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
						Logger.w(tag, "Unknown packet type.");
						continue;
					}
					
					lastHeartbeat = System.currentTimeMillis();

					switch (dataPacket.getType()) {
					case COMM_UDP_SYN_PRIVATE:
						if (isInAHostedConference) {
							Logger.d(tag, "Ignoring " + dataPacket.getType() + " since we are in conference");
							break;
						}
						Logger.d(tag, "Received " + dataPacket.getType());
						synchronized (VoIPClient.this) {
							ConnectionMethods currentMethod = getPreferredConnectionMethod();
							setPreferredConnectionMethod(ConnectionMethods.PRIVATE);
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_SYNACK_PRIVATE);
							sendPacket(dp, false);
							setPreferredConnectionMethod(currentMethod);
						}
						break;
						
					case COMM_UDP_SYN_PUBLIC:
						if (isInAHostedConference) {
							Logger.d(tag, "Ignoring " + dataPacket.getType() + " since we are in conference");
							break;
						}
						Logger.d(tag, "Received " + dataPacket.getType());
						synchronized (VoIPClient.this) {
							ConnectionMethods currentMethod = getPreferredConnectionMethod();
							setPreferredConnectionMethod(ConnectionMethods.PUBLIC);
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_SYNACK_PUBLIC);
							sendPacket(dp, false);
							setPreferredConnectionMethod(currentMethod);
						}
						break;
						
					case COMM_UDP_SYN_RELAY:
						Logger.d(tag, "Received " + dataPacket.getType());
						
						synchronized (VoIPClient.this) {
							ConnectionMethods currentMethod = getPreferredConnectionMethod();
							setPreferredConnectionMethod(ConnectionMethods.RELAY);
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_SYNACK_RELAY);
							sendPacket(dp, false);
							setPreferredConnectionMethod(currentMethod);
						}
						break;

					case COMM_UDP_SYNACK_PRIVATE:
					case COMM_UDP_ACK_PRIVATE:
						if (isInAHostedConference) {
							Logger.d(tag, "Ignoring " + dataPacket.getType() + " since we are in conference");
							break;
						}
						Logger.d(tag, "Received " + dataPacket.getType());
						synchronized (VoIPClient.this) {
							if (senderThread != null)
								senderThread.interrupt();
							setPreferredConnectionMethod(ConnectionMethods.PRIVATE);
							if (connected) break;

							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_ACK_PRIVATE);
							sendPacket(dp, true);
						}
						connected = true;
						break;
						
					case COMM_UDP_SYNACK_PUBLIC:
					case COMM_UDP_ACK_PUBLIC:
						if (isInAHostedConference) {
							Logger.d(tag, "Ignoring " + dataPacket.getType() + " since we are in conference");
							break;
						}
						Logger.d(tag, "Received " + dataPacket.getType());
						synchronized (VoIPClient.this) {
							if (senderThread != null)
								senderThread.interrupt();
							setPreferredConnectionMethod(ConnectionMethods.PUBLIC);
							if (connected) break;
							
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_ACK_PUBLIC);
							sendPacket(dp, true);
						}
						connected = true;
						break;
						
					case COMM_UDP_SYNACK_RELAY:
					case COMM_UDP_ACK_RELAY:
						Logger.d(tag, "Received " + dataPacket.getType());
						synchronized (VoIPClient.this) {
							if (getPreferredConnectionMethod() == ConnectionMethods.PRIVATE || 
									getPreferredConnectionMethod() == ConnectionMethods.PUBLIC) {
								Logger.d(tag, "Ignoring " + dataPacket.getType() + " since we are expecting a " +
										getPreferredConnectionMethod() + " connection.");
								break;
							}
							if (senderThread != null)
								senderThread.interrupt();
							setPreferredConnectionMethod(ConnectionMethods.RELAY);
							if (connected) break;

							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_ACK_RELAY);
							sendPacket(dp, true);
						}
						connected = true;
						break;
						
					case AUDIO_PACKET:
						audioPacketsReceivedPerSecond++;
						processAudioPacket(dataPacket);
						break;
						
					case MULTIPLE_AUDIO_PACKETS:
						for (byte[] data : dataPacket.getDataList()) {
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.AUDIO_PACKET);
							dp.setData(data);
							dp.setEncrypted(dataPacket.isEncrypted());
							processAudioPacket(dp);
						}
						break;
						
					case HEARTBEAT:
						lastHeartbeat = System.currentTimeMillis();
						if (dataPacket.getData() != null) {
							try {
								if (version >= 3) {
									remotePacketLoss = ByteBuffer.wrap(dataPacket.getData()).order(ByteOrder.LITTLE_ENDIAN).getInt();
									processRemotePacketLoss();
								}

							} catch (BufferUnderflowException e) {
								Logger.w(tag, "BufferUnderflowException exception: " + e.toString());
							}
						}
						
						// Mostly redundant check to ensure that neither of the phones
						// is playing the reconnecting tone
						stopReconnectBeeps();
						break;
						
					case ACK:
						removePacketFromAckWaitQueue(dataPacket.getPacketNumber());
						break;
						
					case ENCRYPTION_PUBLIC_KEY:
						if (isInitiator() != true) {
							Logger.e(tag, "Was not expecting a public key.");
							break;
						}
						
						Logger.d(tag, "Received public key.");
						if (encryptor.getPublicKey() == null) {
							encryptor.setPublicKey(dataPacket.getData());
							encryptionStage = EncryptionStage.STAGE_GOT_PUBLIC_KEY;
							exchangeCryptoInfo();
						}
						break;
						
					case ENCRYPTION_SESSION_KEY:
						if (isInitiator() == true) {
							Logger.e(tag, "Was not expecting a session key.");
							break;
						}
						
						if (encryptor.getSessionKey() == null) {
							encryptor.setSessionKey(encryptor.rsaDecrypt(dataPacket.getData()));
							Logger.d(tag, "Received session key.");
							encryptionStage = EncryptionStage.STAGE_GOT_SESSION_KEY;
							exchangeCryptoInfo();
						}
						break;
						
					case ENCRYPTION_RECEIVED_SESSION_KEY:
						Logger.d(tag, "Encryption ready. MD5: " + encryptor.getSessionMD5());
						encryptionStage = EncryptionStage.STAGE_READY;
						break;
						
					case END_CALL:
						Logger.d(tag, "Other party hung up.");
						setEnder(true);
						stop();
						break;
						
					case START_VOICE:
						interruptResponseTimeoutThread();
						startRecordingAndPlayback();
						break;
						
					case CALL_DECLINED:
						setEnder(true);
						stop();
						break;
						
					case CURRENT_BITRATE:
						remoteBitrate = ByteBuffer.wrap(dataPacket.getData()).order(ByteOrder.LITTLE_ENDIAN).getInt();
						setIdealBitrate();
						break;

					case HOLD_ON:
						setRemoteHold(true);
						break;
						
					case HOLD_OFF:
						setRemoteHold(false);
						break;

					case MUTE_ON:
						setRemoteMute(true);
						break;

					case MUTE_OFF:
						setRemoteMute(false);
						break;
						
					case CLIENTS_LIST_JSON:
						if (dataPacket.getData() != null) {
							try {
								updateClientsList(new String(dataPacket.getData(), "UTF-8"));
							} catch (UnsupportedEncodingException e) {
								Logger.e(tag, "UnsupportedEncodingException in startReceiving(): " + e.toString());
							}
						}
						break;
						
					case RESET_PACKET_LOSS:
						playbackTrackingBits.clear();
						playbackFeederCounter = 0;
						packetLoss = 0;
						break;
						
					case REQUEST_RECONNECT:
						Logger.w(tag, "Reconnection requested.");
						reconnect();
						break;
						
					case FORCE_MUTE_ON:
						forceMute = true;
						sendHandlerMessage(VoIPConstants.MSG_UPDATE_FORCE_MUTE_LAYOUT);
						break;
						
					case FORCE_MUTE_OFF:
						forceMute = false;
						sendHandlerMessage(VoIPConstants.MSG_UPDATE_FORCE_MUTE_LAYOUT);
						break;
						
					case SPEECH_OFF:
						setSpeaking(false);
						break;
						
					case SPEECH_ON:
						setSpeaking(true);
						break;
						
					default:
						Logger.w(tag, "Received unexpected packet: " + dataPacket.getType());
						break;
						
					}
				}
			}
		}, "VOIP_RECEIVE_THREAD");
		
		receivingThread.start();
	}
	
	private void processAudioPacket(VoIPDataPacket dataPacket) {
		
		if (dataPacket.isEncrypted()) {
			byte[] encryptedData = dataPacket.getData();
			dataPacket.write(encryptor.aesDecrypt(encryptedData));
			dataPacket.setEncrypted(false);
		}
		
		if (dataPacket.isVoice() && !isSpeaking) {
			setSpeaking(true);
		} else if (!dataPacket.isVoice() && isSpeaking) {
			setSpeaking(false);
		}
		
		samplesToDecodeQueue.add(dataPacket);
		
	}
	
	public void sendAnalyticsEvent(String ek)
	{
		sendAnalyticsEvent(ek, -1);
	}

	public void sendAnalyticsEvent(String ek, int value)
	{
//		Logger.d(tag + " Analytics", "Logging event: " + ek);
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_TYPE, HikeConstants.LogEvent.VOIP);
			metadata.put(HikeConstants.EVENT_KEY, ek);
			metadata.put(VoIPConstants.Analytics.IS_CALLER, isInitiator() ? 0 : 1);
			metadata.put(VoIPConstants.Analytics.CALL_ID, VoIPService.getCallId());
			metadata.put(VoIPConstants.Analytics.APP_VERSION_NAME, VoIPUtils.getAppVersionName(context));
			metadata.put(VoIPConstants.Analytics.OS_VERSION, Build.VERSION.SDK_INT);
			metadata.put(VoIPConstants.Analytics.IS_CONFERENCE, isHostingConference || isInAHostedConference == true ? 1 : 0);
			metadata.put(VoIPConstants.Analytics.NETWORK_TYPE, VoIPUtils.getConnectionClass(context).ordinal());
			
			String toMsisdn = getPhoneNumber();
			
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
				metadata.put(VoIPConstants.Analytics.IS_ENDER, isEnder() ? 0 : 1);
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
	
	private synchronized void exchangeCryptoInfo() {

		if (cryptoEnabled == false)
			return;

		new Thread(new Runnable() {
			
			@Override
			public void run() {
				if (encryptionStage == EncryptionStage.STAGE_INITIAL && isInitiator() != true) {
					// The initiator (caller) generates and sends a public key
					encryptor.initKeys();
					VoIPDataPacket dp = new VoIPDataPacket(PacketType.ENCRYPTION_PUBLIC_KEY);
					dp.write(encryptor.getPublicKey());
					sendPacket(dp, true);
					Logger.d(tag, "Sending public key.");
				}

				if (encryptionStage == EncryptionStage.STAGE_GOT_PUBLIC_KEY && isInitiator() == true) {
					// Generate and send the AES session key
					encryptor.initSessionKey();
					byte[] encryptedSessionKey = encryptor.rsaEncrypt(encryptor.getSessionKey(), encryptor.getPublicKey());
					VoIPDataPacket dp = new VoIPDataPacket(PacketType.ENCRYPTION_SESSION_KEY);
					dp.write(encryptedSessionKey);
					sendPacket(dp, true);
					Logger.d(tag, "Sending AES key.");
				}

				if (encryptionStage == EncryptionStage.STAGE_GOT_SESSION_KEY) {
					VoIPDataPacket dp = new VoIPDataPacket(PacketType.ENCRYPTION_RECEIVED_SESSION_KEY);
					sendPacket(dp, true);
					encryptionStage = EncryptionStage.STAGE_READY;
					Logger.d(tag, "Encryption ready. MD5: " + encryptor.getSessionMD5());
				}
			}
		}, "EXCHANGE_CRYPTO_THREAD").start();
	}

	private void setRemoteHold(boolean newHold) {
		
		if (remoteHold == newHold)
			return;

		remoteHold = newHold;
		sendHandlerMessage(VoIPConstants.MSG_UPDATE_REMOTE_HOLD);
	}

	public boolean isRemoteMute() {
		return remoteMute;
	}

	public void setRemoteMute(boolean remoteMute) {
		this.remoteMute = remoteMute;
	}

	private void startStreaming() {
		
		startCodec(); 
		startSendingAndReceiving();
		startHeartbeat();
		exchangeCryptoInfo();
		sendLocalBitrate();
		
		Logger.d(tag, "Streaming started.");
	}
	
	private void startCodec() {
		try
		{
			Logger.d(tag, "Instantiating Opus for " + getPhoneNumber());
			opusWrapper = new OpusWrapper();
			opusWrapper.getDecoder(VoIPConstants.AUDIO_SAMPLE_RATE, 1);
			opusWrapper.getEncoder(VoIPConstants.AUDIO_SAMPLE_RATE, 1, localBitrate);
		}
		catch (UnsatisfiedLinkError e)
		{
			Logger.e(tag, "Codec exception: " + e.toString());
			hangUp();
			return;
		}
		catch (Exception e) 
		{
			Logger.e(tag, "Codec exception: " + e.toString());
			hangUp();
			return;
		}
		
		startDecompression();
		startCompression();
	}
	
	private void startCompression() {
		compressionThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				byte[] compressedData = new byte[OpusWrapper.OPUS_FRAME_SIZE * 10];
				int compressedDataLength = 0;
				
				while (keepRunning) {
					
					try {
						VoIPDataPacket packetToEncode = samplesToEncodeQueue.take();
						byte[] pcmData = packetToEncode.getData();
						
						// Set encoding bitrate depending on whether audio has voice
						if (packetToEncode.isVoice())
							opusWrapper.setEncoderBitrate(getVoiceBitrate());
						else
							opusWrapper.setEncoderBitrate(OpusWrapper.OPUS_LOWEST_SUPPORTED_BITRATE);
							
						// Get compressed data from the encoder
						if ((compressedDataLength = opusWrapper.encode(pcmData, compressedData)) > 0) {
							byte[] trimmedCompressedData = new byte[compressedDataLength];
							System.arraycopy(compressedData, 0, trimmedCompressedData, 0, compressedDataLength);
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.AUDIO_PACKET);
							dp.write(trimmedCompressedData);
							dp.setVoice(packetToEncode.isVoice());
							buffersToSendQueue.put(dp);
						} else {
							Logger.w(tag, "Compression error.");
						}
					} catch (InterruptedException e) {
						break;
					} catch (Exception e) {
						Logger.e(tag, "Compression error: " + e.toString());
						break;
					}
				}
			}
		}, "COMPRESSION_THREAD");
		
		compressionThread.start();
	}

	
	private void startDecompression() {
		
		codecDecompressionThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				int uncompressedLength = 0;
				while (keepRunning == true) {
					VoIPDataPacket dpdecode;
					try {
						dpdecode = samplesToDecodeQueue.take();
					} catch (InterruptedException e1) {
						break;
					}
					
					byte[] uncompressedData = new byte[OpusWrapper.OPUS_FRAME_SIZE * 2];	
					
					if (dpdecode.getVoicePacketNumber() > 0 && 
							lastPacketReceived > 0 &&
							dpdecode.getVoicePacketNumber() <= lastPacketReceived) {
						Logger.w(tag, "Old packet received.");
						continue;	// We received an old packet again
					}

					if (dpdecode.getVoicePacketNumber() > 0 && dpdecode.getVoicePacketNumber() > lastPacketReceived + 1) {
//						Logger.w(tag, "Packet loss. Current: " + dpdecode.getVoicePacketNumber() +
//								", Expected: " + (lastPacketReceived + 1));
						try {
							uncompressedLength = opusWrapper.fec(dpdecode.getData(), uncompressedData);
							uncompressedLength = uncompressedLength * 2;
							if (uncompressedLength > 0) {
								// We have a decoded packet
								lastPacketReceived = dpdecode.getVoicePacketNumber();

								VoIPDataPacket dp = new VoIPDataPacket(PacketType.AUDIO_PACKET);
								byte[] packetData = new byte[uncompressedLength];
								System.arraycopy(uncompressedData, 0, packetData, 0, uncompressedLength);
								dp.write(packetData);

								synchronized (decodedBuffersQueue) {
									decodedBuffersQueue.add(dp);
									decodedBuffersQueue.notify();
								}

							}
						} catch (Exception e) {
							Logger.d(tag, "Opus decode exception: " + e.toString());
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

							VoIPDataPacket dp = new VoIPDataPacket(PacketType.AUDIO_PACKET);
							byte[] packetData = new byte[uncompressedLength];
							System.arraycopy(uncompressedData, 0, packetData, 0, uncompressedLength);
							dp.write(packetData);

							synchronized (decodedBuffersQueue) {
								decodedBuffersQueue.add(dp);
								decodedBuffersQueue.notify();
							}

						}
					} catch (Exception e) {
						Logger.d(tag, "Opus decode exception: " + e.toString());
					}
				}
			}
		}, "CODE_DECOMPRESSION_THREAD");
		
		codecDecompressionThread.start();
	}
	

	public boolean isAudioRunning() {
		return audioStarted;
	}
	
	public void setCallStatus(VoIPConstants.CallStatus status)
	{
		currentCallStatus = status;
	}

	public VoIPConstants.CallStatus getCallStatus()
	{
		return currentCallStatus;
	}

	public void setInitialCallStatus()
	{
		if(isAudioRunning())
		{
			setCallStatus(VoIPConstants.CallStatus.ACTIVE);
		}
		else
		{
			setCallStatus(isInitiator() ? VoIPConstants.CallStatus.INCOMING_CALL : VoIPConstants.CallStatus.OUTGOING_CONNECTING);
		}
	}

	public void setIdealBitrate() {
		
		localConnectionClass = VoIPUtils.getConnectionClass(context);

		int twoGBitrate = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.VOIP_BITRATE_2G, VoIPConstants.BITRATE_2G);
		int threeGBitrate = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.VOIP_BITRATE_3G, VoIPConstants.BITRATE_3G);
		int wifiBitrate = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.VOIP_BITRATE_WIFI, VoIPConstants.BITRATE_WIFI);
		int conferenceBitrate = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.VOIP_BITRATE_CONFERENCE, VoIPConstants.BITRATE_CONFERENCE);
		
		if (localConnectionClass == ConnectionClass.TwoG)
			localBitrate = twoGBitrate;
		else if (localConnectionClass == ConnectionClass.ThreeG)
			localBitrate = threeGBitrate;
		else if (localConnectionClass == ConnectionClass.WiFi || localConnectionClass == ConnectionClass.FourG)
			localBitrate = wifiBitrate;
		else 
			localBitrate = wifiBitrate;

		// Conference override
		if ((isInAHostedConference || isHostingConference) && localBitrate > conferenceBitrate)
			localBitrate = conferenceBitrate;
		
		if (remoteBitrate > 0 && remoteBitrate < localBitrate)
			localBitrate = remoteBitrate;
		
		Logger.d(tag, "Detected ideal bitrate: " + localBitrate);
		
	}
	
	public void sendLocalBitrate() {

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
	
	public void startChrono() {

		try {
			if (chronometer == null) {
				Logger.d(tag, "Starting chrono.");
				chronometer = new Chronometer(context);
				chronometer.setBase(SystemClock.elapsedRealtime());
				chronometer.start();
			}
		} catch (Exception e) {
			Logger.w(tag, "Chrono exception: " + e.toString());
		}
	}

	public int getCallDuration() {
		int seconds = 0;
		synchronized (VoIPClient.this) {
			if (chronometer != null) {
				seconds = (int) ((SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000);
			} else
				seconds = chronoBackup;
		}
		
		return seconds;
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
	}
	
	private void interruptResponseTimeoutThread() {
		if (responseTimeoutThread != null)
			responseTimeoutThread.interrupt();
	}
	
	public VoIPDataPacket getDecodedBuffer() {
		
		if (!connected)
			return null;
		
		boolean hit = true;
		VoIPDataPacket dp = decodedBuffersQueue.poll();
		
		if (dp == null && opusWrapper != null) {
			// We do not have audio data from the client. 
			// Use packet loss concealment to extrapolate data.
			try {
				if (plcCounter++ > VoIPConstants.PLC_LIMIT) {
					// We have had no data from the client for a while. 
					// Assume they are not speaking. 
					setSpeaking(false);
				} else {
					// Use the decoder to extrapolate previous samples 
					// into a current sample.
					dp = new VoIPDataPacket(PacketType.AUDIO_PACKET);
					byte[] data = new byte[OpusWrapper.OPUS_FRAME_SIZE * 2];
					opusWrapper.plc(data);
					dp.setData(data);
				}
			} catch (Exception e) {
				Logger.e(tag, "PLC Exception: " + e.toString());
			}
			hit = false;
		} 
		
		if (isSpeaking()) {
			playbackFeederCounter++;
			if (playbackFeederCounter == Integer.MAX_VALUE)
				playbackFeederCounter = 0;
			
			if (hit) {
				plcCounter = 0;
				playbackTrackingBits.set(playbackFeederCounter % playbackTrackingBits.size());
				frameHits++;
			} else {
				playbackTrackingBits.clear(playbackFeederCounter % playbackTrackingBits.size());
				frameMisses++;
			}
			
			if (playbackFeederCounter % QUALITY_CALCULATION_FREQUENCY == 0)
				calculateQuality();
		}
		
		return dp;
	}
	
	private void calculateQuality() {

		if (remoteHold || remoteMute)
			return;
		
		int cardinality = playbackTrackingBits.cardinality();
		packetLoss = (100 - (cardinality*100 / (playbackFeederCounter < playbackTrackingBits.size() ? playbackFeederCounter : playbackTrackingBits.size())));
//		Logger.d(tag, "Loss: " + packetLoss + ", playbackFeederCounter: " + playbackFeederCounter);
		
		CallQuality newQuality;
		
		if (packetLoss < 10)
			newQuality = CallQuality.EXCELLENT;
		else if (packetLoss < 20)
			newQuality = CallQuality.GOOD;
		else if (packetLoss < 30)
			newQuality = CallQuality.FAIR;
		else 
			newQuality = CallQuality.WEAK;

		if (currentCallQuality != newQuality && getCallDuration() > QUALITY_BUFFER_SIZE) {
			currentCallQuality = newQuality;
			sendHandlerMessage(VoIPConstants.MSG_UPDATE_QUALITY);
		}

	}

	public CallQuality getQuality() {
		return currentCallQuality;
	}
	
	public void updateLocalSpeech(boolean speech) {
		VoIPDataPacket dp = null;
		if (speech)
			dp = new VoIPDataPacket(PacketType.SPEECH_ON);
		else
			dp = new VoIPDataPacket(PacketType.SPEECH_ON);

		sendPacket(dp, true);
	}
	
	private void processRemotePacketLoss() {
		
		if (remotePacketLoss < VoIPConstants.ACCEPTABLE_PACKET_LOSS && bitrateAdjustment >= 0)
			return;
		
		if (!audioStarted) 
			return;
		
		if (lastCongestionControlTimestamp > System.currentTimeMillis() - VoIPConstants.CONGESTION_CONTROL_REPEAT_THRESHOLD * 1000)
			return;
		
		if (getCallDuration() < QUALITY_BUFFER_SIZE + 1)
			return;
		
		if (isHostingConference || isInAHostedConference)
			return;
		
		if (remotePacketLoss < VoIPConstants.ACCEPTABLE_PACKET_LOSS)
			bitrateAdjustment += VoIPConstants.BITRATE_STEP_UP;
		else
			bitrateAdjustment -= remotePacketLoss * getVoiceBitrate() / 100;
		
		if (getVoiceBitrate() < OpusWrapper.OPUS_LOWEST_SUPPORTED_BITRATE) {
			bitrateAdjustment = OpusWrapper.OPUS_LOWEST_SUPPORTED_BITRATE - localBitrate;
			if (version >= 3 && audioFramesPerUDPPacket < VoIPConstants.MAXIMUM_FRAMES_PER_PACKET) {
				audioFramesPerUDPPacket++;	
				bitrateAdjustment = 0;
			}
		}
		
		Logger.d(tag, "Remote loss: " + remotePacketLoss + 
				", bitrate: " + getVoiceBitrate() +
				", frames/packet: " + audioFramesPerUDPPacket);
		
		sendPacket(new VoIPDataPacket(PacketType.RESET_PACKET_LOSS), false);
		lastCongestionControlTimestamp = System.currentTimeMillis();
	}
	
	private boolean reconnectForConference() {
		
		// The version check is a little bit of a hack. 
		// If we have never managed to connect to a client, we won't even know the version
		// and hence a reconnect will not be attempted. 
		if (version >= 2 && isInAHostedConference && keepRunning) {
			reconnecting = false;
			audioStarted = false;
			
			// Socket info timeout thread will be running since we will 
			// already be trying to reconnect.
			if (partnerSocketInfoTimeoutThread != null)
				partnerSocketInfoTimeoutThread.interrupt();
			socketInfoReceived = false;
			
			retrieveExternalSocket();
			Logger.w(tag, "Yup, reconnecting.");
			return true;
		}
		
		return false;
	}
	
	public void addSampleToEncode(VoIPDataPacket dp) {
		
		// If we are in a large conference, then don't send non-voice audio
		if (!dp.isVoice() && clientMsisdns.size() > VoIPConstants.CONFERENCE_THRESHOLD)
			return;
		
		// If we are on a 2G connection, don't send non-voice audio
		if (!dp.isVoice() && (localConnectionClass == ConnectionClass.TwoG || localConnectionClass == ConnectionClass.ThreeG))
			return;
		
		samplesToEncodeQueue.add(dp);
	}
	
	private void updateClientsList(String json) {
		
//		Logger.w(tag, "Updating: " + json);
		try {
			clientMsisdns.clear();
			JSONObject jsonObject = new JSONObject(json);
			JSONArray jsonArray = jsonObject.getJSONArray(VoIPConstants.Extras.VOIP_CLIENTS);
			if (jsonArray != null) {
				for (int i = 0; i < jsonArray.length(); i++) {
					VoIPClient client = new VoIPClient(null, null);
					JSONObject clientObject = jsonArray.getJSONObject(i);
					client.setPhoneNumber(clientObject.getString(VoIPConstants.Extras.MSISDN));
					client.setSpeaking(clientObject.getBoolean(VoIPConstants.Extras.SPEAKING));
					client.setCallStatus(CallStatus.values()[clientObject.getInt(VoIPConstants.Extras.STATUS)]);
					if (clientObject.has(VoIPConstants.Extras.RINGING))
						client.setRinging(clientObject.getBoolean(VoIPConstants.Extras.RINGING));
					client.isDummy = true;
					
					// Ignoring your own msisdn
					if (client.getPhoneNumber().equals(selfMsisdn))
						continue;
					
					// Mark the host client
					if (client.getPhoneNumber().equals(getPhoneNumber())) {
						client.isHost = true;
						clientMsisdns.add(0, client);
					} else
						clientMsisdns.add(client);
				}
			} else
				Logger.w(tag, "Clients array is empty.");
		} catch (JSONException e) {
			Logger.e(tag, "Error parsing clients JSON: " + e.toString());
		}

		if (clientMsisdns.size() <= 1) {
			Logger.w(tag, "Conference over?");
		} else
			isHostingConference = true;
		
		sendHandlerMessage(VoIPConstants.MSG_UPDATE_CONTACT_DETAILS);
	}
	
	public int getVoiceBitrate() {
		return localBitrate + bitrateAdjustment;
	}
	
	public boolean isRinging() {
		boolean ringing = false;
		
		if (isDummy)
			return isRinging;
		
		if (connected && !audioStarted)
			ringing = true;
		
		return ringing;
	}
	
	public void setRinging(boolean isRinging) {
		this.isRinging = isRinging;
	}
	
	public boolean isHost() {
		return isHost;
	}

	private void stop() {
		sendHandlerMessage(VoIPConstants.MSG_VOIP_CLIENT_STOP);
	}
	
	private void connectionEstablished() {
		lastPacketReceived = 0;
		bitrateAdjustment = 0;
		audioFramesPerUDPPacket = 1;
		sendHandlerMessage(VoIPConstants.CONNECTION_ESTABLISHED_FIRST_TIME);
	}

	private void startRecordingAndPlayback() {
		sendHandlerMessage(VoIPConstants.MSG_START_RECORDING_AND_PLAYBACK);
	}
	
	private void startReconnectBeeps() {
		sendHandlerMessage(VoIPConstants.MSG_START_RECONNECTION_BEEPS);
	}
	
	private void stopReconnectBeeps() {
		sendHandlerMessage(VoIPConstants.MSG_STOP_RECONNECTION_BEEPS);
	}
	
}

