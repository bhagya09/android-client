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
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.bsb.hike.utils.Logger;
import com.bsb.hike.voip.VoIPConstants.CallQuality;
import com.bsb.hike.voip.VoIPDataPacket.PacketType;
import com.bsb.hike.voip.VoIPEncryptor.EncryptionStage;
import com.bsb.hike.voip.VoIPUtils.ConnectionClass;
import com.bsb.hike.voip.protobuf.VoIPSerializer;

public class VoIPClient  {		
	
	// Packet prefixes
	private static final byte PP_RAW_VOICE_PACKET = 0x01;
	private static final byte PP_ENCRYPTED_VOICE_PACKET = 0x02;
	private static final byte PP_PROTOCOL_BUFFER = 0x03;
	
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
	private ConnectionMethods preferredConnectionMethod = ConnectionMethods.UNKNOWN;
	private InetAddress cachedInetAddress = null;
	private String relayAddress;
	private int relayPort;

	private Context context;
	private DatagramSocket socket = null;
	private OpusWrapper opusWrapper;
	private Thread iceThread = null, partnerTimeoutThread = null, responseTimeoutThread = null, senderThread = null, receivingThread = null;
	private Thread sendingThread = null;
	private Thread codecDecompressionThread = null, compressionThread = null;
	private boolean keepRunning = true;
	public boolean connected = false;
	public boolean reconnecting = false;
	private int currentPacketNumber = 0, rawVoiceSent = 0;
	public boolean socketInfoReceived = false, socketInfoSent = false;
	private boolean establishingConnection = false;
	private int totalBytesReceived = 0, totalBytesSent = 0;
	private int totalPacketsSent = 0, totalPacketsReceived = 0;
	private Handler handler;
	private int previousHighestRemotePacketNumber = 0;
	private BitSet packetTrackingBits = new BitSet(PACKET_TRACKING_SIZE);
	private long lastHeartbeat;	
	public VoIPEncryptor encryptor = new VoIPEncryptor();
	private boolean cryptoEnabled = true;
	private VoIPEncryptor.EncryptionStage encryptionStage;
	public boolean remoteHold = false;
	public boolean audioStarted = false;
	private VoIPConstants.CallStatus currentCallStatus;
	public int localBitrate = VoIPConstants.BITRATE_WIFI, remoteBitrate = 0;
	private int chronoBackup = 0;
	public Chronometer chronometer = null;
	private int reconnectAttempts = 0;
	private int droppedDecodedPackets = 0;
	public int callSource = -1;

	// Call quality fields
	private int qualityCounter = 0;
	private long lastQualityReset = 0;
	private CallQuality currentCallQuality = CallQuality.UNKNOWN;
	
	// Network quality test
	private int networkQualityPacketsReceived = 0;

	public final ConcurrentHashMap<Integer, VoIPDataPacket> ackWaitQueue		 = new ConcurrentHashMap<Integer, VoIPDataPacket>();
	public final ConcurrentLinkedQueue<VoIPDataPacket> samplesToDecodeQueue     = new ConcurrentLinkedQueue<VoIPDataPacket>();
	private final LinkedBlockingQueue<VoIPDataPacket> encodedBuffersQueue      = new LinkedBlockingQueue<VoIPDataPacket>();
	public final ConcurrentLinkedQueue<VoIPDataPacket> decodedBuffersQueue      = new ConcurrentLinkedQueue<VoIPDataPacket>();
	public final LinkedBlockingQueue<byte[]> samplesToEncodeQueue      = new LinkedBlockingQueue<byte[]>();
	
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
		setCallStatus(VoIPConstants.CallStatus.UNINITIALIZED);
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
			Logger.d(VoIPConstants.TAG, "Unable to retrieve contact info.");
			name = phoneNumber;
		} else {
			name = contactInfo.getNameOrMsisdn();
		}
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

	public ConnectionMethods getPreferredConnectionMethod() {
		return preferredConnectionMethod;
	}
	
	public void setPreferredConnectionMethod(
			ConnectionMethods preferredConnectionMethod) {
		this.preferredConnectionMethod = preferredConnectionMethod;
		cachedInetAddress = null;
		// Logger.d(VoIPConstants.TAG, "Setting preferred connection method to: " + preferredConnectionMethod.toString());
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
				Logger.e(VoIPConstants.TAG, "VoIPClient UnknownHostException: " + e.toString());
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

	public void removeExternalSocketInfo() {
		setOurExternalIPAddress(null);
		setOurExternalPort(0);
		if (socket != null) {
			socket.close();
			socket = null;
		}
	}
	
	public void retrieveExternalSocket() {

		keepRunning = true;
		
		iceThread = new Thread(new Runnable() {

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

					setOurInternalIPAddress(VoIPUtils.getLocalIpAddress(context)); 
					setOurInternalPort(socket.getLocalPort());

					while (continueSending && keepRunning && (counter < 10 || reconnecting)) {
						counter++;
						try {
							InetAddress host = InetAddress.getByName(VoIPConstants.ICEServerName);
							
							/**
							 * If we are initiating the connection, then we set the relay server
							 * to be used by both clients. 
							 */
							if (!isInitiator()) {
								setRelayAddress(host.getHostAddress());
								setRelayPort(VoIPUtils.getRelayPort(context));
							}

							Logger.d(VoIPConstants.TAG, "ICE Sending.");
							sendPacket(dp, false);
							
							if (socket == null)
								return;
							
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
					sendSocketInfoToPartner();
					if (socketInfoReceived)
						establishConnection();
					else
						startPartnerTimeoutThread();
				} else {
					if (!Thread.currentThread().isInterrupted()) {
						Logger.d(VoIPConstants.TAG, "Failed to retrieve external socket.");
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
		if (getOurExternalIPAddress() != null && 
				!getOurExternalIPAddress().isEmpty() && 
				getOurExternalPort() > 0)
			return true;
		else
			return false;
	}
	
	public void sendSocketInfoToPartner() {
		if (getPhoneNumber() == null || getPhoneNumber().isEmpty()) {
			Logger.e(VoIPConstants.TAG, "Have no partner info. Quitting.");
			return;
		}

		if (!haveExternalSocketInfo()) {
			Logger.d(VoIPConstants.TAG, "Can't send socket info (don't have it!)");
			return;
		}
		
		try {
			JSONObject socketData = new JSONObject();
			socketData.put("internalIP", getOurInternalIPAddress());
			socketData.put("internalPort", getOurInternalPort());
			socketData.put("externalIP", getOurExternalIPAddress());
			socketData.put("externalPort", getOurExternalPort());
			socketData.put("relay", getRelayAddress());
			socketData.put("relayport", getRelayPort());
			socketData.put("callId", VoIPService.getCallId());
			socketData.put("initiator", !isInitiator());
			socketData.put("reconnecting", reconnecting);
			
			JSONObject data = new JSONObject();
			data.put(HikeConstants.MESSAGE_ID, new Random().nextInt(10000));
			data.put(HikeConstants.TIMESTAMP, System.currentTimeMillis() / 1000); 
			data.put(HikeConstants.METADATA, socketData);

			JSONObject message = new JSONObject();
			message.put(HikeConstants.TO, getPhoneNumber());
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
	
	private void startHeartbeat() {
		
		// Heartbeat, and other housekeeping
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				lastHeartbeat = System.currentTimeMillis();

				while (keepRunning == true) {
					
					// Send heartbeat
					VoIPDataPacket dp = new VoIPDataPacket(PacketType.HEARTBEAT);
					sendPacket(dp, false);

					if (System.currentTimeMillis() - lastHeartbeat > HEARTBEAT_TIMEOUT && !reconnecting) {
//						Logger.w(VoIPConstants.TAG, "Heartbeat failure. Reconnecting.. ");
						startReconnectBeeps();
						if (!isInitiator() && connected && isAudioRunning())
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
						int idealPacketCount = (VoIPConstants.AUDIO_SAMPLE_RATE * VoIPConstants.QUALITY_WINDOW) / OpusWrapper.OPUS_FRAME_SIZE; 
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
					while (samplesToEncodeQueue.size() > VoIPConstants.MAX_SAMPLES_BUFFER) {
						Logger.d(VoIPConstants.TAG, "Dropping to_encode packet.");
						samplesToEncodeQueue.poll();
					}
					
					while (samplesToDecodeQueue.size() > VoIPConstants.MAX_SAMPLES_BUFFER) {
						Logger.d(VoIPConstants.TAG, "Dropping to_decode packet.");
						samplesToDecodeQueue.poll();
					}
					
					while (encodedBuffersQueue.size() > VoIPConstants.MAX_SAMPLES_BUFFER) {
						Logger.d(VoIPConstants.TAG, "Dropping encoded packet.");
						encodedBuffersQueue.poll();
					}

					while (decodedBuffersQueue.size() > VoIPConstants.MAX_SAMPLES_BUFFER) {
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
	
	/**
	 * Reconnect after a communications failure.
	 */
	public void reconnect() {

		if (reconnecting)
			return;
		else
			reconnecting = true;

		reconnectAttempts++;
		Logger.w(VoIPConstants.TAG, "VoIPService reconnect()");

		// Interrupt the receiving thread since we will make the socket null
		// and it could throw an NPE.
		interruptReceivingThread();
		
		setCallStatus(VoIPConstants.CallStatus.RECONNECTING);
		sendHandlerMessage(VoIPConstants.MSG_RECONNECTING);
		socketInfoReceived = false;
		socketInfoSent = false;
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
			Logger.w(VoIPConstants.TAG, "Already trying to establish connection.");
			return;
		}
		
		if (socket == null) {
			Logger.w(VoIPConstants.TAG, "establishConnection() called with null socket.");
			return;
		}
		
		if (partnerTimeoutThread != null)
			partnerTimeoutThread.interrupt();

		setPreferredConnectionMethod(ConnectionMethods.UNKNOWN);
		establishingConnection = true;
		connected = false;
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
						synchronized (this) {
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

				establishingConnection = false;
				stopReconnectBeeps();
				if (senderThread != null)
					senderThread.interrupt();
				
				if (connected == true) {
					Logger.d(VoIPConstants.TAG, "UDP connection established :) " + getPreferredConnectionMethod());
					
					if (!isInitiator()) 
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
						sendHandlerMessage(VoIPConstants.MSG_CONNECTED);
					}
				} else {
					Logger.d(VoIPConstants.TAG, "UDP connection failure! :(");
					sendHandlerMessage(VoIPConstants.MSG_CONNECTION_FAILURE);
					if (!reconnecting) {
						if (!isInitiator())
							VoIPUtils.addMessageToChatThread(context, VoIPClient.this, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_OUTGOING, 0, -1, false);
						else
							VoIPUtils.addMessageToChatThread(context, VoIPClient.this, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING, 0, -1, true);
					}
					sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.UDP_CONNECTION_FAIL);
					stop();
				}
			}
		}).start();

	}

	private void startResponseTimeout() {
		responseTimeoutThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(VoIPConstants.TIMEOUT_PARTNER_ANSWER);
					if (!isAudioRunning()) {
						// Call not answered yet?
						if (connected) 
						{
							if (!isInitiator())
							{
								sendHandlerMessage(VoIPConstants.MSG_PARTNER_ANSWER_TIMEOUT);
								sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_PARTNER_ANSWER_TIMEOUT);
								VoIPUtils.addMessageToChatThread(context, VoIPClient.this, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_OUTGOING, 0, -1, false);
							}
							else
							{
								VoIPUtils.addMessageToChatThread(context, VoIPClient.this, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING, 0, -1, true);
							}
						}
						stop();
						
					}
				} catch (InterruptedException e) {
					// Do nothing, all is good
				}
			}
		}, "PARTNER_TIMEOUT_THREAD");
		
		responseTimeoutThread.start();
	}

	private void startSendingAndReceiving() {
		
		// In case we are reconnecting, current sending and receiving threads
		// need to be restarted because the sockets would have changed.
		if (sendingThread != null)
			sendingThread.interrupt();
		
		interruptReceivingThread();
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

					VoIPDataPacket dp;
					try {
						dp = encodedBuffersQueue.take();
						dp.voicePacketNumber = voicePacketCount++;

						// Encrypt packet
						if (encryptionStage == EncryptionStage.STAGE_READY) {
							byte[] origData = dp.getData();
							dp.write(encryptor.aesEncrypt(origData));
							dp.setEncrypted(true);
						}

						sendPacket(dp, false);
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		}, "VOIP_SEND_THREAD");
		
		sendingThread.start();
	}
	
	
	public void interruptSenderThread() {
		if (senderThread != null)
			senderThread.interrupt();
		
	}
	
	public void interruptReceivingThread() {
		if (receivingThread != null)
			receivingThread.interrupt();
	}
	
	public void close() {

		Logger.d(VoIPConstants.TAG,
				"============= Call Summary =============\n" +
				"Phone number: " + getPhoneNumber() +
				"\nBytes sent / received: " + totalBytesSent + " / " + totalBytesReceived +
				"\nPackets sent / received: " + totalPacketsSent + " / " + totalPacketsReceived +
				"\nPure voice bytes: " + rawVoiceSent +
				"\nDropped decoded packets: " + droppedDecodedPackets +
				"\nReconnect attempts: " + reconnectAttempts +
				"\nCall duration: " + getCallDuration() + "\n" +
				"========================================");
		
		if (getCallStatus() != VoIPConstants.CallStatus.PARTNER_BUSY) {
			setCallStatus(VoIPConstants.CallStatus.ENDED);
		}

		if (iceThread != null)
			iceThread.interrupt();

		if (partnerTimeoutThread != null)
			partnerTimeoutThread.interrupt();
		
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
		
		if(chronometer != null) {
			chronometer.stop();
			chronometer = null;
		}

		if (opusWrapper != null) {
			opusWrapper.destroy();
			opusWrapper = null;
		}
		
		Bundle bundle = new Bundle();
		bundle.putInt(VoIPConstants.CALL_ID, VoIPService.getCallId());
		bundle.putInt(VoIPConstants.IS_CALL_INITIATOR, isInitiator() ? 0 : 1);
		bundle.putInt(VoIPConstants.CALL_NETWORK_TYPE, VoIPUtils.getConnectionClass(context).ordinal());
		bundle.putString(VoIPConstants.PARTNER_MSISDN, getPhoneNumber());
		bundle.putBoolean(VoIPConstants.IS_CONNECTED, connected);

		// sendHandlerMessage(VoIPConstants.MSG_SHUTDOWN_ACTIVITY, bundle);

		sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_END);

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
		removeExternalSocketInfo();
		ackWaitQueue.clear();
		samplesToDecodeQueue.clear();
		encodedBuffersQueue.clear();
		decodedBuffersQueue.clear();
	}

	private void setExternalSocketInfo(String ICEResponse) throws JSONException {
		JSONObject jsonObject = new JSONObject(ICEResponse);
		setOurExternalIPAddress(jsonObject.getString("IP"));
		setOurExternalPort(Integer.parseInt(jsonObject.getString("Port")));
		Logger.d(VoIPConstants.TAG, "External socket - " + getOurExternalIPAddress() + ":" + getOurExternalPort());
		Logger.d(VoIPConstants.TAG, "Internal socket - " + getOurInternalIPAddress() + ":" + getOurInternalPort());
	}

	/**
	 * Wait for partner to send us their socket information
	 * Set timeout so we don't wait indefinitely
	 */
	private void startPartnerTimeoutThread() {
		
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
				if (!isInitiator() && !reconnecting) {
					VoIPUtils.addMessageToChatThread(context, VoIPClient.this, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_OUTGOING, 0, -1, false);
					VoIPUtils.sendMissedCallNotificationToPartner(VoIPClient.this);
				}
				sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.PARTNER_SOCKET_INFO_TIMEOUT);
				stop();					
			}
		}, "PARTNER_TIMEOUT_THREAD");
		
		partnerTimeoutThread.start();
	}
	
	public void sendPacket(VoIPDataPacket dp, boolean requiresAck) {
		
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
		
		if (getPreferredConnectionMethod() == ConnectionMethods.RELAY) {
			dp.setDestinationIP(getExternalIPAddress());
			dp.setDestinationPort(getExternalPort());
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
				packet = new DatagramPacket(packetData, packetData.length, InetAddress.getByName(getRelayAddress()), getRelayPort());
			else
				packet = new DatagramPacket(packetData, packetData.length, getCachedInetAddress(), getPreferredPort());
				
//			Logger.d(VoIPConstants.TAG, "Sending type: " + dp.getType() + " to: " + packet.getAddress() + ":" + packet.getPort());
			socket.send(packet);
			totalBytesSent += packet.getLength();
			totalPacketsSent++;
		} catch (IOException e) {
			Logger.w(VoIPConstants.TAG, "sendPacket() IOException: " + e.toString());
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
					Logger.d(VoIPConstants.TAG, "Re-Requesting ack for: " + dp.getType());
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
						synchronized (this) {
							ConnectionMethods currentMethod = getPreferredConnectionMethod();
							setPreferredConnectionMethod(ConnectionMethods.PRIVATE);
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_SYNACK_PRIVATE);
							sendPacket(dp, false);
							setPreferredConnectionMethod(currentMethod);
						}
						break;
						
					case COMM_UDP_SYN_PUBLIC:
						Logger.d(VoIPConstants.TAG, "Received " + dataPacket.getType());
						synchronized (this) {
							ConnectionMethods currentMethod = getPreferredConnectionMethod();
							setPreferredConnectionMethod(ConnectionMethods.PUBLIC);
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_SYNACK_PUBLIC);
							sendPacket(dp, false);
							setPreferredConnectionMethod(currentMethod);
						}
						break;
						
					case COMM_UDP_SYN_RELAY:
						Logger.d(VoIPConstants.TAG, "Received " + dataPacket.getType());
						
						synchronized (this) {
							ConnectionMethods currentMethod = getPreferredConnectionMethod();
							setPreferredConnectionMethod(ConnectionMethods.RELAY);
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_SYNACK_RELAY);
							sendPacket(dp, false);
							setPreferredConnectionMethod(currentMethod);
						}
						break;
						
					case COMM_UDP_SYNACK_PRIVATE:
					case COMM_UDP_ACK_PRIVATE:
						Logger.d(VoIPConstants.TAG, "Received " + dataPacket.getType());
						synchronized (this) {
							interruptSenderThread();
							setPreferredConnectionMethod(ConnectionMethods.PRIVATE);
							if (connected) break;

							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_ACK_PRIVATE);
							sendPacket(dp, true);
						}
						connected = true;
						break;
						
					case COMM_UDP_SYNACK_PUBLIC:
					case COMM_UDP_ACK_PUBLIC:
						Logger.d(VoIPConstants.TAG, "Received " + dataPacket.getType());
						synchronized (this) {
							interruptSenderThread();
							setPreferredConnectionMethod(ConnectionMethods.PUBLIC);
							if (connected) break;
							
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.COMM_UDP_ACK_PUBLIC);
							sendPacket(dp, true);
						}
						connected = true;
						break;
						
					case COMM_UDP_SYNACK_RELAY:
					case COMM_UDP_ACK_RELAY:
						Logger.d(VoIPConstants.TAG, "Received " + dataPacket.getType());
						synchronized (this) {
							if (getPreferredConnectionMethod() == ConnectionMethods.PRIVATE || 
									getPreferredConnectionMethod() == ConnectionMethods.PUBLIC) {
								Logger.d(VoIPConstants.TAG, "Ignoring " + dataPacket.getType() + " since we are expecting a " +
										getPreferredConnectionMethod() + " connection.");
								break;
							}
							interruptSenderThread();
							setPreferredConnectionMethod(ConnectionMethods.RELAY);
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
						stopReconnectBeeps();
						break;
						
					case ACK:
						removePacketFromAckWaitQueue(dataPacket.getPacketNumber());
						break;
						
					case ENCRYPTION_PUBLIC_KEY:
						if (isInitiator() != true) {
							Logger.e(VoIPConstants.TAG, "Was not expecting a public key.");
							break;
						}
						
						Logger.d(VoIPConstants.TAG, "Received public key.");
						if (encryptor.getPublicKey() == null) {
							encryptor.setPublicKey(dataPacket.getData());
							encryptionStage = EncryptionStage.STAGE_GOT_PUBLIC_KEY;
							exchangeCryptoInfo();
						}
						break;
						
					case ENCRYPTION_SESSION_KEY:
						if (isInitiator() == true) {
							Logger.e(VoIPConstants.TAG, "Was not expecting a session key.");
							break;
						}
						
						if (encryptor.getSessionKey() == null) {
							encryptor.setSessionKey(encryptor.rsaDecrypt(dataPacket.getData()));
							Logger.d(VoIPConstants.TAG, "Received session key.");
							encryptionStage = EncryptionStage.STAGE_GOT_SESSION_KEY;
							exchangeCryptoInfo();
						}
						break;
						
					case ENCRYPTION_RECEIVED_SESSION_KEY:
						Logger.d(VoIPConstants.TAG, "Encryption ready. MD5: " + encryptor.getSessionMD5());
						encryptionStage = EncryptionStage.STAGE_READY;
						break;
						
					case END_CALL:
						Logger.d(VoIPConstants.TAG, "Other party hung up.");
						setEnder(true);
						stop();
						VoIPUtils.addMessageToChatThread(context, VoIPClient.this, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_CALL_SUMMARY, getCallDuration(), -1, true);
						break;
						
					case START_VOICE:
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

					case NETWORK_QUALITY:
						networkQualityPacketsReceived++;
						break;
						
					case HOLD_ON:
						setRemoteHold(true);
						break;
						
					case HOLD_OFF:
						setRemoteHold(false);
						break;

					default:
						Logger.w(VoIPConstants.TAG, "Received unexpected packet: " + dataPacket.getType());
						break;
						
					}
				}
			}
		}, "VOIP_RECEIVE_THREAD");
		
		receivingThread.start();
	}
	
	public void sendAnalyticsEvent(String ek)
	{
		sendAnalyticsEvent(ek, -1);
	}

	public void sendAnalyticsEvent(String ek, int value)
	{
		Logger.d(VoIPConstants.TAG, "Logging event: " + ek);
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_TYPE, HikeConstants.LogEvent.VOIP);
			metadata.put(HikeConstants.EVENT_KEY, ek);
			metadata.put(VoIPConstants.Analytics.IS_CALLER, isInitiator() ? 0 : 1);
			metadata.put(VoIPConstants.Analytics.CALL_ID, VoIPService.getCallId());
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
	
	private byte[] getUDPDataFromPacket(VoIPDataPacket dp) {
		
		// Serialize everything except for P2P voice data packets
		byte[] packetData = null;
		byte prefix;
		
		if (dp.getType() == PacketType.VOICE_PACKET && getPreferredConnectionMethod() != ConnectionMethods.RELAY) {
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
					Logger.d(VoIPConstants.TAG, "Sending public key.");
				}

				if (encryptionStage == EncryptionStage.STAGE_GOT_PUBLIC_KEY && isInitiator() == true) {
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

	private void setRemoteHold(boolean newHold) {
		
		if (remoteHold == newHold)
			return;

		remoteHold = newHold;
		sendHandlerMessage(VoIPConstants.MSG_UPDATE_REMOTE_HOLD);
	}

	private void startStreaming() {
		
		startCodec(); 
		startSendingAndReceiving();
		startHeartbeat();
		exchangeCryptoInfo();
		
		if (responseTimeoutThread != null)
			responseTimeoutThread.interrupt();
		
		Logger.d(VoIPConstants.TAG, "Streaming started.");
	}
	
	private void startCodec() {
		try
		{
			Logger.d(VoIPConstants.TAG, "Instantiating Opus for " + getPhoneNumber());
			opusWrapper = new OpusWrapper();
			opusWrapper.getDecoder(VoIPConstants.AUDIO_SAMPLE_RATE, 1);
			opusWrapper.getEncoder(VoIPConstants.AUDIO_SAMPLE_RATE, 1, localBitrate);

			// Set encoder complexity which directly affects CPU usage
			opusWrapper.setEncoderComplexity(0);

		}
		catch (UnsatisfiedLinkError e)
		{
			Logger.e(VoIPConstants.TAG, "Codec exception: " + e.toString());
			hangUp();
			return;
		}
		catch (Exception e) 
		{
			Logger.e(VoIPConstants.TAG, "Codec exception: " + e.toString());
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
						byte[] pcmData = samplesToEncodeQueue.take();
						
						// Get compressed data from the encoder
						if ((compressedDataLength = opusWrapper.encode(pcmData, compressedData)) > 0) {
							byte[] trimmedCompressedData = new byte[compressedDataLength];
							System.arraycopy(compressedData, 0, trimmedCompressedData, 0, compressedDataLength);
							VoIPDataPacket dp = new VoIPDataPacket(PacketType.VOICE_PACKET);
							dp.write(trimmedCompressedData);
							encodedBuffersQueue.put(dp);
						} else {
							Logger.w(VoIPConstants.TAG, "Compression error.");
						}
					} catch (InterruptedException e) {
						break;
					} catch (Exception e) {
						Logger.e(VoIPConstants.TAG, "Compression error: " + e.toString());
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
		
		ConnectionClass connection = VoIPUtils.getConnectionClass(context);

		SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
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
		
		if (opusWrapper != null)
			opusWrapper.setEncoderBitrate(localBitrate);
	}
	
	public int getCallDuration() {
		int seconds = 0;
		if (chronometer != null) {
			seconds = (int) ((SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000);
		} else
			seconds = chronoBackup;
		
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
		VoIPUtils.addMessageToChatThread(context, VoIPClient.this, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_CALL_SUMMARY, getCallDuration(), -1, false);
	}
	
	
	public CallQuality getQuality() {
		return currentCallQuality;
	}
	
	public void interruptResponseTimeoutThread() {
		if (responseTimeoutThread != null)
			responseTimeoutThread.interrupt();
	}
	
	public void setEncoderBitrate(int bitrate) {
		if (opusWrapper != null)
			opusWrapper.setEncoderBitrate(bitrate);
	}
	
	private void stop() {
		sendHandlerMessage(VoIPConstants.MSG_VOIP_CLIENT_STOP);
	}
	
	private void playOutgoingCallRingtone() {
		sendHandlerMessage(VoIPConstants.MSG_VOIP_CLIENT_OUTGOING_CALL_RINGTONE);
	}

	private void playIncomingCallRingtone() {
		sendHandlerMessage(VoIPConstants.MSG_VOIP_CLIENT_INCOMING_CALL_RINGTONE);
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

