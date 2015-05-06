package com.bsb.hike.voip;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.voip.VoIPDataPacket.PacketType;

public class VoIPClient  {		
	
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
	private Thread iceThread = null, partnerTimeoutThread = null, senderThread = null;
	private boolean keepRunning = true;
	public boolean reconnecting = false;
	private int currentPacketNumber = 0;
	public boolean socketInfoReceived = false, socketInfoSent = false;
	private boolean establishingConnection = false;
	public boolean connected = false;
	
	public enum ConnectionMethods {
		UNKNOWN,
		PRIVATE,
		PUBLIC,
		RELAY
	}
	
	

	public VoIPClient(Context context) {
		super();
		this.context = context;
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

							setOurInternalIPAddress(VoIPUtils.getLocalIpAddress(context)); 
							setOurInternalPort(socket.getLocalPort());

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
					sendSocketInfoToPartner();
					if (socketInfoReceived)
						establishConnection();
					else
						startPartnerTimeoutThread();
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

				if (senderThread != null)
					senderThread.interrupt();
				establishingConnection = false;
				if (reconnectingBeepsThread != null)
					reconnectingBeepsThread.interrupt();
				
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
					}
				} else {
					Logger.d(VoIPConstants.TAG, "UDP connection failure! :(");
					sendHandlerMessage(VoIPConstants.MSG_CONNECTION_FAILURE);
					if (!reconnecting) {
						if (!isInitiator())
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

	public void interruptSenderThread() {
		if (senderThread != null)
			senderThread.interrupt();
		
	}
	
	public void close() {
		
		keepRunning = false;
		socketInfoReceived = false;
		establishingConnection = false;
		
		if (iceThread != null)
			iceThread.interrupt();

		if (partnerTimeoutThread != null)
			partnerTimeoutThread.interrupt();
		
		removeExternalSocketInfo();
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
	
}

