package com.bsb.hike.voip;

import java.util.ArrayList;


public class VoIPDataPacket implements Cloneable {

	public class BroadcastListItem {
		private String ip;
		private int port;

		public String getIp() {
			return ip;
		}
		public void setIp(String ip) {
			this.ip = ip;
		}
		public int getPort() {
			return port;
		}
		public void setPort(int port) {
			this.port = port;
		}
	}
	
	private boolean encrypted = false; 
	private PacketType packetType;
	private byte[] data;
	private String destinationIP;
	private int destinationPort;
	private int packetNumber;
	private boolean requiresAck = false;
	private int voicePacketNumber;
	private long timestamp;
	private boolean isVoice;
	private ArrayList<BroadcastListItem> broadcastList = null;
	private ArrayList<byte[]> dataList = null;

	int length = 0;		// Used to indicate length of actual data in "data"

	public enum PacketType {
		UPDATE (0),									// Unused
		CALL (1),									// Unused
		CALL_DECLINED (2),
		AUDIO_PACKET (3),
		END_CALL (4),
		HEARTBEAT (5), 
		START_VOICE (6),
		NO_ANSWER (7),								// Unused
		ENCRYPTION_PUBLIC_KEY (8),
		ENCRYPTION_SESSION_KEY (9),
		ENCRYPTION_RECEIVED_SESSION_KEY (10),
		ENCRYPTION_SET_ON(13),						// Unused
		ENCRYPTION_SET_OFF(14),						// Unused
		ACK (11),
		RECORDING_SAMPLE_RATE (12),					// Unused
		RELAY_INIT (15),							// Hard coded in server code 
		RELAY (16),									// Unused
		CURRENT_BITRATE (17),
		REQUEST_BITRATE (18),						// Unused
		PACKET_LOSS_BIT_ARRAY (19),					// Unused
		CELLULAR_INCOMING_CALL (20),				// Unused
		COMM_UDP_SYN_PRIVATE (21),
		COMM_UDP_SYN_PUBLIC (22),
		COMM_UDP_SYN_RELAY (23),
		COMM_UDP_SYNACK_PRIVATE (24),
		COMM_UDP_SYNACK_PUBLIC (25),
		COMM_UDP_SYNACK_RELAY (26),
		COMM_UDP_ACK_PRIVATE (27),
		COMM_UDP_ACK_PUBLIC (28),
		COMM_UDP_ACK_RELAY (29),
		NETWORK_QUALITY (30),						// Unused
		HOLD_ON (31), 
		HOLD_OFF (32),
		CLIENTS_LIST (33),							// Unused
		RESET_PACKET_LOSS (34),
		MULTIPLE_AUDIO_PACKETS (35),
		MUTE_ON (36),
		MUTE_OFF (37),
		CLIENTS_LIST_JSON (38),
		REQUEST_RECONNECT (39), 
		FORCE_MUTE_ON (40), 
		FORCE_MUTE_OFF (41),
		SPEECH_ON (42), 
		SPEECH_OFF (43),
		REPEAT_AUDIO_PACKET_REQUEST (44),
		REPEAT_AUDIO_PACKET_REQUEST_RESPONSE (45),
		RTT_REQUEST (46),
		RTT_RESPONSE (47)
		;
		
		private final int value;
		
		private PacketType(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
		
		public static PacketType fromValue(int value) {
			switch (value) {
			case 0:
				return UPDATE;
			case 1:
				return CALL;
			case 2:
				return CALL_DECLINED;
			case 3:
				return AUDIO_PACKET;
			case 4:
				return END_CALL;
			case 5:
				return HEARTBEAT;
			case 6:
				return START_VOICE;
			case 7:
				return NO_ANSWER;
			case 8:
				return ENCRYPTION_PUBLIC_KEY;
			case 9:
				return ENCRYPTION_SESSION_KEY;
			case 10:
				return ENCRYPTION_RECEIVED_SESSION_KEY;
			case 11:
				return ACK;
			case 12:
				return RECORDING_SAMPLE_RATE;
			case 13:
				return ENCRYPTION_SET_ON;
			case 14:
				return ENCRYPTION_SET_OFF;
			case 15:
				return RELAY_INIT;
			case 16:
				return RELAY;
			case 17:
				return CURRENT_BITRATE;
			case 18:
				return REQUEST_BITRATE;
			case 19:
				return PACKET_LOSS_BIT_ARRAY;
			case 20:
				return CELLULAR_INCOMING_CALL;
			case 21:
				return COMM_UDP_SYN_PRIVATE;
			case 22:
				return COMM_UDP_SYN_PUBLIC;
			case 23:
				return COMM_UDP_SYN_RELAY;
			case 24:
				return COMM_UDP_SYNACK_PRIVATE;
			case 25:
				return COMM_UDP_SYNACK_PUBLIC;
			case 26:
				return COMM_UDP_SYNACK_RELAY;
			case 27:
				return COMM_UDP_ACK_PRIVATE;
			case 28:
				return COMM_UDP_ACK_PUBLIC;
			case 29:
				return COMM_UDP_ACK_RELAY;
			case 30:
				return NETWORK_QUALITY;
			case 31:
				return HOLD_ON;
			case 32:
				return HOLD_OFF;
			case 33:
				return CLIENTS_LIST;
			case 34:
				return RESET_PACKET_LOSS;
			case 35:
				return MULTIPLE_AUDIO_PACKETS;
			case 36:
				return MUTE_ON;
			case 37:
				return MUTE_OFF;
			case 38:
				return CLIENTS_LIST_JSON;
			case 39:
				return REQUEST_RECONNECT;
			case 40:
				return FORCE_MUTE_ON;
			case 41:
				return FORCE_MUTE_OFF;
			case 42:
				return SPEECH_ON;
			case 43:
				return SPEECH_OFF;
			case 44:
				return REPEAT_AUDIO_PACKET_REQUEST;
			case 45:
				return REPEAT_AUDIO_PACKET_REQUEST_RESPONSE;
			case 46:
				return RTT_REQUEST;
			case 47:
				return RTT_RESPONSE;
			default:
				return UPDATE;
			}
		}
	};

	public PacketType getPacketType() {
		return packetType;
	}

	public void setPacketType(PacketType packetType) {
		this.packetType = packetType;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public String getDestinationIP() {
		return destinationIP;
	}

	public void setDestinationIP(String destinationIP) {
		this.destinationIP = destinationIP;
	}

	public int getDestinationPort() {
		return destinationPort;
	}

	public void setDestinationPort(int destinationPort) {
		this.destinationPort = destinationPort;
	}

	public VoIPDataPacket(PacketType packetType) {
		this.packetType = packetType;
	}
	
	public VoIPDataPacket(PacketType packetType, byte[] bytes) {
		this.packetType = packetType;
		data = bytes;
	}
	
	public VoIPDataPacket() {
	}

	public void write(byte[] data) {
		this.data = data;
	}
	
	public void reset() {
		data = null;
	}

	public byte[] getData() {
		return data;
	}
	
	public int getLength() {
		if (data == null)
			return 0;
		
		if (length > 0)
			return length;
		
		return data.length;
	}
	
	public void setLength(int length) {
		this.length = length;
	}

	public PacketType getType() {
		return packetType;
	}

	public boolean isEncrypted() {
		return encrypted;
	}
	
	public void setEncrypted(boolean encrypted) {
		this.encrypted = encrypted;
	}
	
	public int getPacketNumber() {
		return packetNumber;
	}

	public int getVoicePacketNumber() {
		return voicePacketNumber;
	}

	public void setVoicePacketNumber(int voicePacketNumber) {
		this.voicePacketNumber = voicePacketNumber;
	}

	public void setPacketNumber(int packetNumber) {
		this.packetNumber = packetNumber;
	}

	
	/**
	 * @return the requiresAck
	 */
	public boolean isRequiresAck() {
		return requiresAck;
	}

	/**
	 * @param requiresAck the requiresAck to set
	 */
	public void setRequiresAck(boolean requiresAck) {
		this.requiresAck = requiresAck;
	}

	public boolean isVoice() {
		return isVoice;
	}

	public void setVoice(boolean isVoice) {
		this.isVoice = isVoice;
	}


	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public ArrayList<BroadcastListItem> getBroadcastList() {
		return broadcastList;
	}

	public void setBroadcastList(ArrayList<BroadcastListItem> broadcastList) {
		this.broadcastList = broadcastList;
	}

	public void addToBroadcastList(BroadcastListItem item) {
		if (broadcastList == null)
			broadcastList = new ArrayList<BroadcastListItem>();
		broadcastList.add(item);
	}

	public ArrayList<byte[]> getDataList() {
		return dataList;
	}

	public void addToDataList(byte[] data) {
		if (dataList == null) {
			dataList = new ArrayList<>();
		}
		dataList.add(data);
	}
	
}
