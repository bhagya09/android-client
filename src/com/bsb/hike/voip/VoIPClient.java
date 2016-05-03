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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
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
import com.bsb.hike.offline.OfflineConstants;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPConstants.CallStatus;
import com.bsb.hike.voip.VoIPDataPacket.PacketType;
import com.bsb.hike.voip.VoIPEncryptor.EncryptionStage;
import com.bsb.hike.voip.VoIPUtils.ConnectionClass;
import com.hike.transporter.models.SenderConsignment;

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
	private boolean isCallActive = false;
	private VoIPConstants.CallStatus currentCallStatus;
	private int localBitrate = VoIPConstants.BITRATE_WIFI, remoteBitrate = 0;
	private Chronometer chronometer = null;
	private int reconnectAttempts = 0;
	private int droppedDecodedPackets = 0;
	public int callSource = -1;
	private volatile boolean isSpeaking = false, isRinging = false;
	private int voicePacketCount = 0;
	public boolean isDummy = false, isHost = false;		
	private String selfMsisdn;
	public boolean incompatible = false;
	private boolean usingHikeDirect = false;
	private int lastAudioPacketPlayed = 0;
	private long lastDecodedBufferRequest = 0;

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
	private int plcCounter = 0;
	private int packetLoss = 0, remotePacketLoss = 0;
	private long lastCongestionControlTimestamp = 0;
	private int audioFramesPerUDPPacket = 1;
	private int bitrateAdjustment = 0;
	private int frameHits, frameMisses;
	private volatile int minimumDecodedQueueSize = 0;
	
	// Round trip time
	private boolean rttSent = false;
	private boolean rttTooHigh = false;
	private long rttSentAt = 0;
	private long rtt = 0;
	
	private final ConcurrentHashMap<Integer, VoIPDataPacket> ackWaitQueue		 = new ConcurrentHashMap<>();
	private final LinkedBlockingQueue<VoIPDataPacket> samplesToDecodeQueue     = new LinkedBlockingQueue<>();
	private final LinkedBlockingQueue<VoIPDataPacket> buffersToSendQueue      = new LinkedBlockingQueue<>();
	private final LinkedList<VoIPDataPacket> decodedBuffersQueue      = new LinkedList<>();
	private final LinkedBlockingQueue<VoIPDataPacket> samplesToEncodeQueue      = new LinkedBlockingQueue<>();
	private final MaxSizeHashMap<Integer, VoIPDataPacket> voicePacketsCache = new MaxSizeHashMap<>(100);

	public enum ConnectionMethods {
		UNKNOWN,
		PRIVATE,
		PUBLIC,
		RELAY
	}
	
	@SuppressWarnings("serial")
	public class MaxSizeHashMap<K, V> extends LinkedHashMap<K, V> {
	    private final int maxSize;

	    public MaxSizeHashMap(int maxSize) {
	        this.maxSize = maxSize;
	    }

	    @Override
	    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
	        return size() > maxSize;
	    }
	}
	
	public VoIPClient(Context context, Handler handler) {
		super();
		this.context = context;
		this.handler = handler;
		encryptionStage = EncryptionStage.STAGE_INITIAL;
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
			sendMessageToService(VoIPConstants.MSG_UPDATE_SPEAKING);
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

				usingHikeDirect = isUsingHikeDirect(); 
				
				removeExternalSocketInfo();
				getNewSocket();
				
				if (!usingHikeDirect) {
					
					byte[] receiveData = new byte[10240];
					
					Logger.d(tag, "Retrieving external socket information..");
					VoIPDataPacket dp = new VoIPDataPacket(PacketType.RELAY_INIT);
					DatagramPacket incomingPacket = new DatagramPacket(receiveData, receiveData.length);

					boolean continueSending = true;
					int counter = 0;

					while (continueSending && keepRunning && (counter < 10 || reconnecting)) {
						counter++;
						try {
							/**
							 * If we are initiating the connection, then we set the relay server
							 * to be used by both clients. 
							 * Also check if the relay has already been set (in case of conferences)
							 */
							if (!isInitiator() && TextUtils.isEmpty(getRelayAddress())) {
								InetAddress host;
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
				}
				
				if (haveExternalSocketInfo() || usingHikeDirect) {
					sendSocketInfoToPartner();
					if (socketInfoReceived)
						establishConnection();
					else
						startPartnerSocketInfoTimeoutThread();
				} else {
					if (!Thread.currentThread().isInterrupted()) {
						Logger.d(tag, "Failed to retrieve external socket.");
						sendMessageToService(VoIPConstants.MSG_EXTERNAL_SOCKET_RETRIEVAL_FAILURE);
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

		return getOurExternalIPAddress() != null &&
				!getOurExternalIPAddress().isEmpty() &&
				getOurExternalPort() > 0;
	}
	
	public void sendSocketInfoToPartner() {
		if (getPhoneNumber() == null || getPhoneNumber().isEmpty()) {
			Logger.e(tag, "Have no partner info. Quitting.");
			return;
		}

		if (!haveExternalSocketInfo() && !usingHikeDirect) {
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
			
			if (usingHikeDirect) {
                SenderConsignment senderConsignment = new SenderConsignment.Builder(message.toString(), OfflineConstants.TEXT_TOPIC).ackRequired(false).persistance(false).build();
                OfflineController.getInstance().sendConsignment(senderConsignment);
            } else
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

				while (keepRunning) {
					
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
					
					while (samplesToDecodeQueue.size() > VoIPConstants.MAX_SAMPLES_BUFFER) {
						Logger.d(tag, "Dropping to_decode packet.");
						samplesToDecodeQueue.poll();
					}
					
					while (buffersToSendQueue.size() > VoIPConstants.MAX_SAMPLES_BUFFER) {
						Logger.d(tag, "Dropping encoded packet.");
						buffersToSendQueue.poll();
					}

					// Check if RTT packet has expired
					if (rttSent && System.currentTimeMillis() - rttSentAt > VoIPConstants.MAX_RTT * 1000) {
						Logger.w(tag, "RTT expired.");
						rttSent = false;
						rttTooHigh = true;
					}

					// If we have a playback buffer, then keep calculating RTT every second. 
					if (minimumDecodedQueueSize > 0)
						measureRTT();
					
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
		sendMessageToService(VoIPConstants.MSG_RECONNECTING);
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
						VoIPDataPacket dp;
						synchronized (VoIPClient.this) {
							ConnectionMethods currentMethod = getPreferredConnectionMethod();

							setPreferredConnectionMethod(ConnectionMethods.PRIVATE);
							dp = new VoIPDataPacket(PacketType.COMM_UDP_SYN_PRIVATE);
							sendPacket(dp, false);
							if (!usingHikeDirect) {
								setPreferredConnectionMethod(ConnectionMethods.PUBLIC);
								dp = new VoIPDataPacket(PacketType.COMM_UDP_SYN_PUBLIC);
								sendPacket(dp, false);
								setPreferredConnectionMethod(ConnectionMethods.RELAY);
								dp = new VoIPDataPacket(PacketType.COMM_UDP_SYN_RELAY);
								sendPacket(dp, false);
							}

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
						if (connected) {
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
				
				if (connected) {
					Logger.d(tag, "UDP connection established :) " + getPreferredConnectionMethod());
					connectionEstablished();

					if (reconnecting) {
						setInitialCallStatus();
						sendMessageToService(VoIPConstants.MSG_RECONNECTED);
						// Give the heartbeat a chance to recover
						lastHeartbeat = System.currentTimeMillis() + 5000;
						startSendingAndReceiving();
						reconnecting = false;
					} else {
						if (!isInitiator())
							startResponseTimeout();
						startStreaming();
						sendMessageToService(VoIPConstants.MSG_CONNECTED);
					}
				} else {
					Logger.d(tag, "UDP connection failure! :(");
					if (!reconnectForConference()) {
						sendMessageToService(VoIPConstants.MSG_CONNECTION_FAILURE);
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
					if (!isCallActive()) {
						// Edge case error fixing. If the call went into reconnection
						// before it was answered, then normally no outgoing missed call
						// would appear in our chat thread since we aren't connected.
						// Hence, make it appear as if we ARE connected, so the missed call appears.
						connected = true;

						// Call not answered yet?
						sendMessageToService(VoIPConstants.MSG_PARTNER_ANSWER_TIMEOUT);
						sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_PARTNER_ANSWER_TIMEOUT);
						VoIPDataPacket dp = new VoIPDataPacket(PacketType.END_CALL);
						sendPacket(dp, true);

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
				while (keepRunning) {

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

						if (dp.getType() == PacketType.AUDIO_PACKET) {
							rawVoiceSent += dp.getLength();

							// Voice packet numbers are disabled for conferences
							if (!isInAHostedConference && version >= 2 && dp.getVoicePacketNumber() == 0) {
								dp.setVoicePacketNumber(voicePacketCount++);
								synchronized (voicePacketsCache) {
									voicePacketsCache.put(dp.getVoicePacketNumber(), dp);
								}
							}
						}
						
						stagingQueue.add(dp);
//						Logger.d(tag, "Sending voice: " + dp.isVoice());
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
							VoIPDataPacket first = stagingQueue.get(0);
							jumbo.setVoicePacketNumber(first.getVoicePacketNumber());
							jumbo.setVoice(first.isVoice());
							jumbo.setEncrypted(first.isEncrypted());
							
							for (VoIPDataPacket dp : stagingQueue) {
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
				"\nCall duration: " + getCallDuration() + " seconds" +
				"\nRTT: " + rtt + " ms" +
				"\nVersion: " + version + "\n" +
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
		if (!incompatible)
			VoIPUtils.sendVoIPMessageUsingHike(getPhoneNumber(), 
					HikeConstants.MqttMessageTypes.VOIP_CALL_CANCELLED, 
					VoIPService.getCallId(), false);
		
		keepRunning = false;
		socketInfoReceived = false;
		establishingConnection = false;
		stopReconnectBeeps();
		connected = false;
		isCallActive = false;
		reconnecting = false;
		removeExternalSocketInfo();
		
		ackWaitQueue.clear();
		samplesToDecodeQueue.clear();
		buffersToSendQueue.clear();
		decodedBuffersQueue.clear();
		samplesToEncodeQueue.clear();
		voicePacketsCache.clear();
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
				
				sendMessageToService(VoIPConstants.MSG_PARTNER_SOCKET_INFO_TIMEOUT);
				if (!isInitiator() && !reconnecting) {
					VoIPUtils.sendMissedCallNotificationToPartner(getPhoneNumber(), groupChatMsisdn);
				}
				sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.PARTNER_SOCKET_INFO_TIMEOUT);
				stop();					
			}
		}, "PARTNER_TIMEOUT_THREAD");
		
		partnerSocketInfoTimeoutThread.start();
	}
	
	public synchronized void sendPacket(VoIPDataPacket dp, boolean guaranteeDelivery) {
		
		if (dp.getType() != PacketType.ACK && dp.getPacketNumber() == 0)
			dp.setPacketNumber(currentPacketNumber++);
		
		dp.setRequiresAck(guaranteeDelivery);
		dp.setTimestamp(System.currentTimeMillis());

		if (guaranteeDelivery)
			addPacketToAckWaitQueue(dp);

		if (!keepRunning)
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
		
		// Serialize everything except for P2P voice data packets
		byte[] packetData = VoIPUtils.getUDPDataFromPacket(dp);
		
		if (packetData == null)
			return;

		/*
		// Simulated packet loss
		int simulatedPacketLossPercentage = 0;
		if (simulatedPacketLossPercentage > 0 ) {
			if (new Random().nextInt(100) < simulatedPacketLossPercentage) {
				Logger.d(tag, "Oops. I'm going to lose a packet on purpose.");
				return;
			}
		}
		*/
		
		try {
			DatagramPacket packet;
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
					Logger.d(tag, "Resending packet: " + dp.getType() + " #" + dp.getPacketNumber());
					sendPacket(dp, true);
				}
			}
		}		
	}
	
	private void sendMessageToService(int message) {
		sendMessageToService(message, null);
	}
	
	private void sendMessageToService(int message, Bundle bundle) {
		
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
				byte[] buffer = new byte[50000];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

				while (keepRunning) {

					if (Thread.currentThread().isInterrupted()) {
						break;
					}
					
					try {
						socket.setSoTimeout(0);
						socket.receive(packet);
						totalBytesReceived += packet.getLength();
						totalPacketsReceived++;

					} catch (IOException e) {
						break;
					}
					
					VoIPDataPacket dataPacket = VoIPUtils.getPacketFromUDPData(packet.getData(), packet.getLength());
					
					if (dataPacket == null)
						continue;
					
					// ACK tracking
					if (dataPacket.getType() != PacketType.ACK)
						markPacketReceived(dataPacket.getPacketNumber());

					// ACK response
					if (dataPacket.isRequiresAck()) {
						VoIPDataPacket dp = new VoIPDataPacket(PacketType.ACK);
						dp.setPacketNumber(dataPacket.getPacketNumber());
						sendPacket(dp, false);
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
						
					case REPEAT_AUDIO_PACKET_REQUEST_RESPONSE:
						Logger.d(tag, "Received resent packet #" + dataPacket.getVoicePacketNumber());
					case AUDIO_PACKET:
						audioPacketsReceivedPerSecond++;
						processAudioPacket(dataPacket);
						break;
						
					case MULTIPLE_AUDIO_PACKETS:
						int firstVoicePacketNumber = dataPacket.getVoicePacketNumber();
						int i = 0;

						if (dataPacket.getDataList() == null) {
							Logger.w(tag, "That was unexpected. MULTIPLE_AUDIO_PACKETS has no audio packets.");
							break;
						}

						for (byte[] data : dataPacket.getDataList()) {
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.AUDIO_PACKET);
							dp.setData(data);
							dp.setVoice(dataPacket.isVoice());
							dp.setEncrypted(dataPacket.isEncrypted());
							if (firstVoicePacketNumber > 0) {
								dp.setVoicePacketNumber(firstVoicePacketNumber + i++);
							}
								
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
						if (!isInitiator()) {
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
						if (isInitiator()) {
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
						setCallAsActive();
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
						sendMessageToService(VoIPConstants.MSG_UPDATE_FORCE_MUTE_LAYOUT);
						break;
						
					case FORCE_MUTE_OFF:
						forceMute = false;
						sendMessageToService(VoIPConstants.MSG_UPDATE_FORCE_MUTE_LAYOUT);
						break;
						
					case SPEECH_OFF:
						setSpeaking(false);
						break;
						
					case SPEECH_ON:
						setSpeaking(true);
						break;
						
					case REPEAT_AUDIO_PACKET_REQUEST:
						synchronized (voicePacketsCache) {
							int requestedPacketNumber = dataPacket.getPacketNumber();
							if (voicePacketsCache.containsKey(requestedPacketNumber)) {
								Logger.d(tag, "Cache hit #" + requestedPacketNumber);
								VoIPDataPacket dp = voicePacketsCache.get(requestedPacketNumber);
								dp.setPacketType(PacketType.REPEAT_AUDIO_PACKET_REQUEST_RESPONSE);
								sendPacket(dp, false);
							} else {
								Logger.w(tag, "Cache miss #" + requestedPacketNumber);
							}
						}
						break;

					case RTT_REQUEST:
						VoIPDataPacket dp = new VoIPDataPacket(PacketType.RTT_RESPONSE);
						sendPacket(dp, false);
						break;
						
					case RTT_RESPONSE:
						if (rttSent) {
							long newRtt = System.currentTimeMillis() - rttSentAt;
//							Logger.d(tag, "Round Trip Time. Was: " + rtt + " ms, Is: " + newRtt + " ms.");
							if (newRtt > VoIPConstants.MAX_RTT * 1000) {
								Logger.w(tag, "Discarding excessive RTT: " + newRtt);
								rttTooHigh = true;
							} else {
								rtt = newRtt;
								if (minimumDecodedQueueSize > 0)
									setAudioLatency();
								rttTooHigh = false;
							}
							rttSent = false;
							
						}
						break;
						
					default:
//						Logger.w(tag, "Received unexpected packet");
						break;
						
					}
				}
			}
		}, "VOIP_RECEIVE_THREAD");
		
		receivingThread.start();
	}
	
	private void processAudioPacket(VoIPDataPacket dataPacket) {
		
		if (dataPacket.getVoicePacketNumber() > 0 && 
				lastAudioPacketPlayed > dataPacket.getVoicePacketNumber()) {
			Logger.d(tag, "Ignoring packet #" + dataPacket.getVoicePacketNumber() + ". Already played #" + lastAudioPacketPlayed);
			measureRTT();
			return;
		}
		
		if (dataPacket.isEncrypted()) {
			byte[] encryptedData = dataPacket.getData();
			dataPacket.write(encryptor.aesDecrypt(encryptedData));
			dataPacket.setEncrypted(false);
		}
		
		if (dataPacket.isVoice()) 
			setSpeaking(true);
		else
			setSpeaking(false);
		
		samplesToDecodeQueue.add(dataPacket);
//		Logger.d(tag, "Received audio packet #"  + dataPacket.getVoicePacketNumber());
		
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
			metadata.put(VoIPConstants.Analytics.IS_CONFERENCE, isHostingConference || isInAHostedConference ? 1 : 0);
			metadata.put(VoIPConstants.Analytics.NETWORK_TYPE, VoIPUtils.getConnectionClass(context).ordinal());
			
			String toMsisdn = getPhoneNumber();
			
			if(!TextUtils.isEmpty(toMsisdn))
			{
				metadata.put(AnalyticsConstants.TO, toMsisdn);
			}

			switch (ek) {
				case HikeConstants.LogEvent.VOIP_CALL_CLICK:
					metadata.put(VoIPConstants.Analytics.CALL_SOURCE, callSource);
					break;
				case HikeConstants.LogEvent.VOIP_CALL_END:
				case HikeConstants.LogEvent.VOIP_CALL_DROP:
				case HikeConstants.LogEvent.VOIP_CALL_REJECT:
				case HikeConstants.LogEvent.VOIP_PARTNER_ANSWER_TIMEOUT:
					metadata.put(VoIPConstants.Analytics.DATA_SENT, totalBytesSent);
					metadata.put(VoIPConstants.Analytics.DATA_RECEIVED, totalBytesReceived);
					metadata.put(VoIPConstants.Analytics.IS_ENDER, isEnder() ? 0 : 1);
					if (getCallDuration() > 0) {
						metadata.put(VoIPConstants.Analytics.DURATION, getCallDuration());
					}
					break;
				case HikeConstants.LogEvent.VOIP_CALL_SPEAKER:
				case HikeConstants.LogEvent.VOIP_CALL_HOLD:
				case HikeConstants.LogEvent.VOIP_CALL_MUTE:
					metadata.put(VoIPConstants.Analytics.STATE, value);
					break;
				case HikeConstants.LogEvent.VOIP_CONNECTION_FAILED:
					metadata.put(VoIPConstants.Analytics.CALL_CONNECT_FAIL_REASON, value);
					break;
			}

			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
		}
		catch (JSONException e)
		{
			Logger.w(AnalyticsConstants.ANALYTICS_TAG, "Invalid json");
		}
	}
	
	private void markPacketReceived(int packetNumber) {
		
		if (packetNumber < 0) {
			Logger.e(tag, "Unexpected packetNumber: " + packetNumber);
			return;
		}
		
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

		if (!cryptoEnabled)
			return;

		new Thread(new Runnable() {
			
			@Override
			public void run() {
				if (encryptionStage == EncryptionStage.STAGE_INITIAL && !isInitiator()) {
					// The initiator (caller) generates and sends a public key
					encryptor.initKeys();
					VoIPDataPacket dp = new VoIPDataPacket(PacketType.ENCRYPTION_PUBLIC_KEY);
					dp.write(encryptor.getPublicKey());
					sendPacket(dp, true);
					Logger.d(tag, "Sending public key.");
				}

				if (encryptionStage == EncryptionStage.STAGE_GOT_PUBLIC_KEY && isInitiator()) {
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
		sendMessageToService(VoIPConstants.MSG_UPDATE_REMOTE_HOLD);
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
		catch (UnsatisfiedLinkError | Exception e)
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
				int compressedDataLength;
				
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
				int uncompressedLength;
				while (keepRunning) {
					VoIPDataPacket dpdecode;
					try {
						dpdecode = samplesToDecodeQueue.take();
					} catch (InterruptedException e1) {
						break;
					}
					
					byte[] uncompressedData = new byte[OpusWrapper.OPUS_FRAME_SIZE * 2];	
					int receivedVoicePacketNumber = dpdecode.getVoicePacketNumber();
					
					if (receivedVoicePacketNumber > 0 && receivedVoicePacketNumber > lastPacketReceived + 1) {
//						Logger.w(tag, "Packet loss. Current: " + receivedVoicePacketNumber +
//								", Expected: " + (lastPacketReceived + 1));

//							Logger.d(tag, "FEC packet #" + (receivedVoicePacketNumber - 1));
						try {
							uncompressedLength = opusWrapper.fec(dpdecode.getData(), uncompressedData);
						} catch (Exception e) {
							Logger.w(tag, "Opus FEC exception: " + e.toString());
							continue;
						}

						uncompressedLength = uncompressedLength * 2;
						if (uncompressedLength > 0) {

							VoIPDataPacket dp = new VoIPDataPacket(PacketType.AUDIO_PACKET);
							byte[] packetData = new byte[uncompressedLength];
							System.arraycopy(uncompressedData, 0, packetData, 0, uncompressedLength);
							dp.write(packetData);
							dp.setVoicePacketNumber(receivedVoicePacketNumber - 1);
							dp.setFecGenerated(true);

							// Request this packet again from the client
							requestVoicePacket(dp.getVoicePacketNumber());
							insertVoicePacketInDecodedQueue(dp);
						}
					}
					
					// Regular decoding
					try {
						// Logger.d(VoIPActivity.logTag, "Decompressing data of length: " + dpdecode.getLength());
						uncompressedLength = opusWrapper.decode(dpdecode.getData(), uncompressedData);
						uncompressedLength = uncompressedLength * 2;
						if (uncompressedLength > 0) {
							// We have a decoded packet
							lastPacketReceived = receivedVoicePacketNumber > lastPacketReceived ? receivedVoicePacketNumber : lastPacketReceived;

							VoIPDataPacket dp = new VoIPDataPacket(PacketType.AUDIO_PACKET);
							byte[] packetData = new byte[uncompressedLength];
							System.arraycopy(uncompressedData, 0, packetData, 0, uncompressedLength);
							dp.write(packetData);
							dp.setVoice(dpdecode.isVoice());
							dp.setVoicePacketNumber(receivedVoicePacketNumber);
							insertVoicePacketInDecodedQueue(dp);
						}
					} catch (Exception e) {
						Logger.d(tag, "Opus decode exception: " + e.toString());
					}
				}
			}
		}, "CODE_DECOMPRESSION_THREAD");
		
		codecDecompressionThread.start();
	}
	

	public boolean isCallActive() {
		return isCallActive;
	}

	public void setIsCallActive(boolean isCallActive) {
		this.isCallActive = isCallActive;
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
		if(isCallActive())
		{
			setCallStatus(VoIPConstants.CallStatus.ACTIVE);
		}
		else
		{
			setCallStatus(isInitiator() ? VoIPConstants.CallStatus.INCOMING_CALL : VoIPConstants.CallStatus.OUTGOING_CONNECTING);
		}
	}

	public void setIdealBitrate() {

		ConnectionClass localConnectionClass = VoIPUtils.getConnectionClass(context);

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

		// Hike direct override
		if (usingHikeDirect)
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
		int seconds;
		synchronized (VoIPClient.this) {
			if (chronometer != null) {
				seconds = (int) ((SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000);
			} else
				seconds = 0;
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

//		printDecodedQueue();
		
		// Introduce an artificial lag if there is packet loss
		if (decodedBuffersQueue.size() < minimumDecodedQueueSize &&
				!isSpeaking()) {
			Logger.d(tag, "Stalling. Current queue size: " + decodedBuffersQueue.size()
			+ ", want: " + minimumDecodedQueueSize);
			return null;
		}

		boolean hit = true;

		VoIPDataPacket dp;
		synchronized (decodedBuffersQueue) {
			dp = decodedBuffersQueue.poll();

			// Drop packets from the queue if it is too large
			while (dp != null &&
					decodedBuffersQueue.size() > minimumDecodedQueueSize &&
					!dp.isVoice()) {
				droppedDecodedPackets++;
				lastAudioPacketPlayed = dp.getVoicePacketNumber();
				dp = decodedBuffersQueue.poll();
			}
		}

		if (dp == null && opusWrapper != null) {
			// We do not have audio data from the client. 
			// Use packet loss concealment to extrapolate data.
			Logger.d(tag, "Miss.");
			hit = false;
			try {
				if (plcCounter++ > VoIPConstants.PLC_LIMIT) {
					// We have had no data from the client for a while. 
					// Assume they are not speaking.
					if (isSpeaking()) {
						Logger.d(tag, "Assuming client has stopped speaking.");
						setSpeaking(false);
					}
				} else {
					// Use the decoder to extrapolate previous samples 
					// into a current sample.
					dp = new VoIPDataPacket(PacketType.AUDIO_PACKET);
					byte[] data = new byte[OpusWrapper.OPUS_FRAME_SIZE * 2];
					opusWrapper.plc(data);
					dp.setData(data);
				}
				
				if (version >= 4) {
					measureRTT();
					if (minimumDecodedQueueSize == 0)
						minimumDecodedQueueSize = 1;
				}
				
			} catch (Exception e) {
				Logger.e(tag, "PLC Exception: " + e.toString());
			}
		} else {

			if (dp != null) {
				lastAudioPacketPlayed = dp.getVoicePacketNumber();
			}
		}

		if (isSpeaking() || version >= 4) {		// After v4, clients always send data (whether speaking or not)
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
	
	private void printDecodedQueue() {
		String output = "";
		long time = System.currentTimeMillis() - lastDecodedBufferRequest;
		synchronized (decodedBuffersQueue) {
			lastDecodedBufferRequest = System.currentTimeMillis();
			for (VoIPDataPacket dp : decodedBuffersQueue) {
				String sp = dp.isVoice() ? "[Y]" : "[N]";
				output += dp.getVoicePacketNumber() + " " + sp + ", ";
			}
		}

		String voice = isSpeaking() ? "[Y]" : "[N]";
		
		Logger.d(tag, voice + " Time: " + time +", Queue: " + output);
	}
	
	private void setAudioLatency() {
		
		if (version < 4)
			return;

		int newQueueSize = (int) (rtt / 60 + 1);
		
//		if (newQueueSize > minimumDecodedQueueSize)
//			minimumDecodedQueueSize = newQueueSize;
//		else if (minimumDecodedQueueSize > 0)
//			minimumDecodedQueueSize--;

		if (minimumDecodedQueueSize != newQueueSize) {
			minimumDecodedQueueSize = newQueueSize;
//			Logger.d(tag, "New audio latency: " + minimumDecodedQueueSize * 60 + " ms, frames: " + minimumDecodedQueueSize);
		}
	}
	
	/**
	 * Request the client to send a voice packet again. 
	 * @param packetNumber Voice packet number that is to be requested again
	 */
	private void requestVoicePacket(int packetNumber) {
		
		if (version < 4)
			return;

		// If the RTT is too high, there is no point of re-requesting packets since they will
		// arrive too late. We'll just end up congesting the network further.
		if (rttTooHigh)
			return;
		
		// Do not re-request packets if we haven't calculated the RTT already
		// and don't have an audio buffer. 
		if (minimumDecodedQueueSize == 0)
			return;

		// Do not request packets if our decoded queue isn't long enough
		if (decodedBuffersQueue.size() < minimumDecodedQueueSize)
			return;

		// Don't request packets again if participating in a hosted conference
		if (isHostingConference)
			return;

		Logger.d(tag, "Requesting voice packet number: " + packetNumber);
		VoIPDataPacket dp = new VoIPDataPacket(PacketType.REPEAT_AUDIO_PACKET_REQUEST);
		dp.setPacketNumber(packetNumber);
		sendPacket(dp, false);
		
	}
	
	/**
	 * Inserts a decoded voice packet in the decoded queue
	 * in the right position. <br/>
	 * @param newPacket VoIPDataPacket containing PCM audio to be inserted.
	 */
	private void insertVoicePacketInDecodedQueue(VoIPDataPacket newPacket) {

		synchronized (decodedBuffersQueue) {
			// If queue is empty
			if (decodedBuffersQueue.size() == 0) {
				decodedBuffersQueue.add(newPacket);
				return;
			}

			// In case there is no packet number, insert packet at tail
			if (newPacket.getVoicePacketNumber() == 0) {
				decodedBuffersQueue.addLast(newPacket);
				return;
			}

			// If packet is being inserted at the tail
			if (decodedBuffersQueue.getLast().getVoicePacketNumber() < newPacket.getVoicePacketNumber()) {

				// Check for packet loss
				if (version >= 4)
					if (newPacket.getVoicePacketNumber() > decodedBuffersQueue.getLast().getVoicePacketNumber() + 1)
						for (int i = decodedBuffersQueue.getLast().getVoicePacketNumber() + 1; i < newPacket.getVoicePacketNumber(); i++) {
							requestVoicePacket(i);
						}

				decodedBuffersQueue.addLast(newPacket);
				return;
			}

			// If packet is being inserted at the head
			if (decodedBuffersQueue.getFirst().getVoicePacketNumber() > newPacket.getVoicePacketNumber()) {
				decodedBuffersQueue.addFirst(newPacket);
				return;
			}

			// Insert in middle
			for (int i = 0; i < decodedBuffersQueue.size(); i++) {
				// Regular insert
				if (decodedBuffersQueue.get(i).getVoicePacketNumber() < newPacket.getVoicePacketNumber() &&
						decodedBuffersQueue.get(i + 1).getVoicePacketNumber() > newPacket.getVoicePacketNumber()) {
					decodedBuffersQueue.add(i + 1, newPacket);
					return;
				}

				// Replacing an FEC packet
				if (decodedBuffersQueue.get(i).getVoicePacketNumber() == newPacket.getVoicePacketNumber()) {
					if (decodedBuffersQueue.get(i).isFecGenerated()) {
						decodedBuffersQueue.remove(i);
						decodedBuffersQueue.add(i, newPacket);
						return;
					} else {
						Logger.d(tag, "Packet #" + newPacket.getVoicePacketNumber() + " already in queue.");
						return;
					}
				}
			}

			// Insertion failed.
			Logger.w(tag, "Decoded buffer insertion failed. Decoded queue size: " + decodedBuffersQueue.size() +
					", new voice packet #" + newPacket.getVoicePacketNumber());
			printDecodedQueue();
		}
	}
	
	private void calculateQuality() {

		if (remoteHold || remoteMute)
			return;
		
		int cardinality = playbackTrackingBits.cardinality();
		packetLoss = (100 - (cardinality*100 / (playbackFeederCounter < playbackTrackingBits.size() ? playbackFeederCounter : playbackTrackingBits.size())));
//		Logger.d(tag, "Loss: " + packetLoss + ", playbackFeederCounter: " + playbackFeederCounter);
		
	}

	public void updateLocalSpeech(boolean speech) {
		VoIPDataPacket dp;
//		Logger.d(tag, "Local speech: " + speech);
		if (speech)
			dp = new VoIPDataPacket(PacketType.SPEECH_ON);
		else
			dp = new VoIPDataPacket(PacketType.SPEECH_OFF);

		sendPacket(dp, true);
	}
	
	private void processRemotePacketLoss() {
		
		if (remotePacketLoss <= VoIPConstants.ACCEPTABLE_PACKET_LOSS && bitrateAdjustment >= 0)
			return;
		
		if (!isCallActive)
			return;
		
		if (lastCongestionControlTimestamp > System.currentTimeMillis() - VoIPConstants.CONGESTION_CONTROL_REPEAT_THRESHOLD * 1000)
			return;
		
		if (getCallDuration() < QUALITY_BUFFER_SIZE + 1)
			return;
		
		if (isHostingConference || isInAHostedConference)
			return;
		
		if (remotePacketLoss <= VoIPConstants.ACCEPTABLE_PACKET_LOSS)
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
		
		Logger.w(tag, "Remote loss: " + remotePacketLoss + 
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
			isCallActive = false;
			
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

		// If the call isn't active yet, discard recorded audio
		if (!isCallActive())
			return;

		// Drop packets if getting left behind
		if (samplesToEncodeQueue.size() > VoIPConstants.MAX_SAMPLES_BUFFER) {
			Logger.d(tag, "Dropping to_encode packet.");
			return;
		}

		samplesToEncodeQueue.add(dp);
	}
	
	private void updateClientsList(String json) {
		
		Logger.w(tag, "Updating: " + json);
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
		
		sendMessageToService(VoIPConstants.MSG_UPDATE_CONTACT_DETAILS);
	}
	
	public int getVoiceBitrate() {
		return localBitrate + bitrateAdjustment;
	}
	
	public boolean isRinging() {
		boolean ringing = false;
		
		if (isDummy)
			return isRinging;
		
		if (connected && !isCallActive)
			ringing = true;
		
		return ringing;
	}

	/**
	 * Measure the round-trip-time to the client.
	 * TODO: This implementation is broken. It is possible for it to measure a lower than actual
	 * RTT if the client receives a response from an older RTT request after a newer request has
	 * been made.
	 */
	private void measureRTT() {
		
		if (version < 4)
			return;
		
		if (rttSent)
			return;
		
//		Logger.d(tag, "Measuring RTT.");
		VoIPDataPacket dp = new VoIPDataPacket(PacketType.RTT_REQUEST);
		sendPacket(dp, false);
		rttSent = true;
		rttSentAt = System.currentTimeMillis();
	}
	
	public void setRinging(boolean isRinging) {
		this.isRinging = isRinging;
	}
	
	public boolean isHost() {
		return isHost;
	}
	
	public boolean isUsingHikeDirect() {
		return OfflineUtils.isConnectedToSameMsisdn(getPhoneNumber());
	}

	private void stop() {
		sendMessageToService(VoIPConstants.MSG_VOIP_CLIENT_STOP);
	}
	
	private void connectionEstablished() {
		lastPacketReceived = 0;
		bitrateAdjustment = 0;
		audioFramesPerUDPPacket = 1;
		decodedBuffersQueue.clear();
		voicePacketsCache.clear();
		measureRTT();
		sendMessageToService(VoIPConstants.CONNECTION_ESTABLISHED_FIRST_TIME);
	}

	private void setCallAsActive() {
		sendMessageToService(VoIPConstants.MSG_CALL_ACTIVE);
	}
	
	private void startReconnectBeeps() {
		sendMessageToService(VoIPConstants.MSG_START_RECONNECTION_BEEPS);
	}
	
	private void stopReconnectBeeps() {
		sendMessageToService(VoIPConstants.MSG_STOP_RECONNECTION_BEEPS);
	}
	
}

