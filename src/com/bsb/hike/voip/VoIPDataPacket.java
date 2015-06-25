package com.bsb.hike.voip;


public class VoIPDataPacket {

	private boolean encrypted = false; 
	private PacketType packetType;
	private byte[] data;
	String destinationIP;
	int destinationPort;
	int packetNumber;
	boolean requiresAck = false;
	int voicePacketNumber;
	long timestamp;
	
	int length = 0;		// Used to indicate length of actual data in "data"

	public enum PacketType {
		UPDATE (0),
		CALL (1),
		CALL_DECLINED (2),
		VOICE_PACKET (3),
		END_CALL (4),
		HEARTBEAT (5), 
		START_VOICE (6),
		NO_ANSWER (7),
		ENCRYPTION_PUBLIC_KEY (8),
		ENCRYPTION_SESSION_KEY (9),
		ENCRYPTION_RECEIVED_SESSION_KEY (10),
		ENCRYPTION_SET_ON(13),
		ENCRYPTION_SET_OFF(14),
		ACK (11),
		RECORDING_SAMPLE_RATE (12),
		RELAY_INIT (15),	// This is hard coded in server code 
		RELAY (16),
		CURRENT_BITRATE (17),
		REQUEST_BITRATE (18),
		PACKET_LOSS_BIT_ARRAY (19),	// Used in the iOS app
		CELLULAR_INCOMING_CALL (20),
		COMM_UDP_SYN_PRIVATE (21),
		COMM_UDP_SYN_PUBLIC (22),
		COMM_UDP_SYN_RELAY (23),
		COMM_UDP_SYNACK_PRIVATE (24),
		COMM_UDP_SYNACK_PUBLIC (25),
		COMM_UDP_SYNACK_RELAY (26),
		COMM_UDP_ACK_PRIVATE (27),
		COMM_UDP_ACK_PUBLIC (28),
		COMM_UDP_ACK_RELAY (29),
		NETWORK_QUALITY (30),
		HOLD_ON (31), 
		HOLD_OFF (32)
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
				return VOICE_PACKET;
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

}
