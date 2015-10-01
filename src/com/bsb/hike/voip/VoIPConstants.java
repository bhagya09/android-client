package com.bsb.hike.voip;


public class VoIPConstants {
	public static final String TAG = "VoIP";

	/**
	 * <p>Current VoIP protocol version.</p>
	 * <p>Added in <b>v2</b>: <br/>
	 * - Voice packet numbers, so that FEC can be triggered
	 * <p>Added in <b>v3</b>: <br/>
	 * - Congestion control and conference
	 * </p>
	 */
	public static final int VOIP_VERSION = 3;
	
	// Relay and ICE server 
	public static final String ICEServerName = "relay.hike.in";	 // Staging: 54.179.137.97, Production: relay.hike.in 
	public static final int ICEServerPort = 9998; 
	final static String[] ICEServerIpAddresses = {"52.74.88.97", "52.74.113.80"};

	public static final int AUDIO_SAMPLE_RATE = 48000; 
	public static final int MAX_SAMPLES_BUFFER = 3;

	// Packet prefixes
	public static final byte PP_RAW_VOICE_PACKET = 0x01;
	public static final byte PP_ENCRYPTED_VOICE_PACKET = 0x02;
	public static final byte PP_PROTOCOL_BUFFER = 0x03;

	/**
	 * Time (ms) to wait before the client being called replies with its
	 * own socket information.
	 */
	public static final int TIMEOUT_PARTNER_SOCKET_INFO = 15000;
	
	/**
	 * Time (ms) to wait for the person being called to accept or decline a call.
	 */
	public static final int TIMEOUT_PARTNER_ANSWER = 30000;

	/**
	 * Maximum size of a group to launch a conference call directly
	 */
	public static final int MAXIMUM_GROUP_CHAT_SIZE = 100;
	
	/**
	 * If a client does not provide audio data continuously this many times, 
	 * we assume they are not speaking. 
	 */
	public static final int PLC_LIMIT = 5;

	/**
	 * If packet loss increases beyond this threshold, congestion control
	 * will be triggered. 
	 */
	public static final int ACCEPTABLE_PACKET_LOSS = 10;

	/**
	 * If the number of participants in a conference exceeds this threshold, 
	 * clients will stop transmitting audio completely when they do not
	 * detect a voice signal from their mic. 
	 */
	public static final int CONFERENCE_THRESHOLD = 3;

	/**
	 * Number of seconds to wait for before triggering congestion control again.
	 */
	public static final int CONGESTION_CONTROL_REPEAT_THRESHOLD = 3;

	/**
	 * Number of ms to wait before broadcasting the list of clients again. 
	 */
	public static final int CONFERENCE_CLIENTS_LIST_BROADCAST_REPEAT = 2000;
	
	/**
	 * Do not attempt a reconnect before this much time has passed
	 * since the last reconnect.
	 */
	public static final int RECONNECT_THRESHOLD = 10000;

	/**
	 * Maximum number of audio frames that can be put in a single
	 * UDP packet. 
	 */
	public static final int MAXIMUM_FRAMES_PER_PACKET = 3;

	/**
	 * If bitrate had been lowered previously and now there is 
	 * no packet loss, then it will be increased in tiny
	 * increments.
	 */
	public static final int BITRATE_STEP_UP = 200;

	/**
	 * The minimum duration of a call (in seconds) beyond which the call 
	 * rating popup can be displayed.
	 */
	public static final int MIN_CALL_DURATION_FOR_RATING_POPUP = 10;

	public static final int INITIAL_ICE_SOCKET_TIMEOUT = 2;
	
	// Intent actions
	public static final String PARTNER_REQUIRES_UPGRADE = "pru";
	public static final String PARTNER_IN_CALL = "incall";
	public static final String PARTNER_HAS_BLOCKED_YOU = "blocked";
	
	// Default bitrates
	public static final int BITRATE_2G = 12000;
	public static final int BITRATE_3G = 16000;
	public static final int BITRATE_WIFI = 24000;
	public static final int BITRATE_CONFERENCE = 16000;

	public static final String CALL_ID = "callId";
	public static final String MSISDN = "msisdn";
	public static final String IS_CALL_INITIATOR = "isCallInitiator";
	public static final String IS_CONNECTED = "isConnected";
	public static final String CALL_RATING = "rating";
	public static final String CALL_NETWORK_TYPE = "network";
	public static final String PARTNER_MSISDN = "pmsisdn";
	public static final String CALL_FAILED_REASON = "callfailreason";
	public static final String PARTNER_NAME = "pname";
	public static final String CALL_RATE_BUNDLE = "callRateBundle";
	public static final String IS_CONFERENCE = "isConf";
	public static final String CALL_DURATION = "duration";

	/*
	 *  Handler Message Constants
	 */
	public static final int MSG_SHUTDOWN_ACTIVITY = 1;
	public static final int CONNECTION_ESTABLISHED_FIRST_TIME = 2;
	public static final int MSG_AUDIO_START = 3;
	public static final int MSG_CONNECTION_FAILURE = 4;
	public static final int MSG_EXTERNAL_SOCKET_RETRIEVAL_FAILURE = 5;
	public static final int MSG_PARTNER_SOCKET_INFO_TIMEOUT = 6;
	public static final int MSG_PARTNER_ANSWER_TIMEOUT = 7;
	public static final int MSG_RECONNECTING = 8;
	public static final int MSG_RECONNECTED = 9;
	public static final int MSG_UPDATE_QUALITY = 10;
	public static final int MSG_NETWORK_SUCKS = 11;
	public static final int MSG_ALREADY_IN_NATIVE_CALL = 13;
	public static final int MSG_AUDIORECORD_FAILURE = 14;
	public static final int MSG_UPDATE_REMOTE_HOLD = 15;
	public static final int MSG_VOIP_CLIENT_STOP = 16;
	public static final int MSG_START_RECORDING_AND_PLAYBACK = 19;
	public static final int MSG_START_RECONNECTION_BEEPS = 21;
	public static final int MSG_STOP_RECONNECTION_BEEPS = 22;
	public static final int MSG_CONNECTED = 23;
	public static final int MSG_JOINED_CONFERENCE = 24;
	public static final int MSG_UPDATE_CONTACT_DETAILS = 26;
	public static final int MSG_UPDATE_SPEAKING = 27;
	public static final int MSG_BLUETOOTH_SHOW = 28;
	public static final int MSG_DOES_NOT_SUPPORT_CONFERENCE = 29;
	public static final int MSG_PARTNER_BUSY = 30;
	public static final int MSG_UPDATE_FORCE_MUTE_LAYOUT = 31;
	public static final int MSG_UPDATE_CALL_BUTTONS = 32;
	public static final int MSG_PARTNER_INCOMPATIBLE_PLATFORM = 33;

	public static final class Analytics
	{
		public static final String CALL_RATING = "rate";

		public static final String CALL_ID = "callid";

		public static final String IS_CALLER = "caller";

		public static final String IS_ENDER = "ender";

		public static final String NETWORK_TYPE = "net";

		public static final String CALL_ISSUES = "issues";

		public static final String NEW_LOG ="nl";

		public static final String CALL_SOURCE = "source";

		public static final String DATA_SENT = "dsent";

		public static final String DATA_RECEIVED = "drec";

		public static final String STATE = "state";

		public static final String DURATION = "dur";

		public static final String CALL_CONNECT_FAIL_REASON = "reason";

		public static final String IS_CONFERENCE = "isconf";
	}

	public static final class CallFailedCodes
	{
		public static final int PARTNER_UPGRADE = 0;

		public static final int PARTNER_INCOMPAT = 1;

		public static final int PARTNER_BLOCKED_USER = 2;

		public static final int PARTNER_BUSY = 3;

		public static final int UDP_CONNECTION_FAIL = 4;

		public static final int EXTERNAL_SOCKET_RETRIEVAL_FAILURE = 5;

		public static final int PARTNER_SOCKET_INFO_TIMEOUT = 6;

		public static final int CALLER_BAD_NETWORK = 7;

		public static final int CALLER_IN_NATIVE_CALL = 8;

		public static final int PARTNER_ANSWER_TIMEOUT = 9;
	}

	public static final class Extras
	{
		public static final String ACTION = "action";

		public static final String SET_PARTNER_INFO = "setpartnerinfo";

		public static final String OUTGOING_CALL = "outgoingcall";

		public static final String MSISDN = "msisdn";

		public static final String MSISDNS = "msisdns";		// Used for group calling

		public static final String CALL_SOURCE = "call_source";

		public static final String GROUP_CHAT_MSISDN = "groupChatMsisdn";

		public static final String INTERNAL_IP = "internalIP";

		public static final String INTERNAL_PORT = "internalPort";

		public static final String EXTERNAL_IP = "externalIP";

		public static final String EXTERNAL_PORT = "externalPort";

		public static final String RELAY = "relay";

		public static final String RELAY_PORT = "relayport";

		public static final String RECONNECTING = "reconnecting";

		public static final String INITIATOR = "initiator";

		public static final String CALL_ID = "callId";

		public static final String INCOMING_CALL = "incomingCall";
		
		public static final String VOIP_VERSION = "version";
		
		public static final String CONFERENCE = "conf";
		
		public static final String REMOVE_FAILED_FRAGMENT = "removeFailedFrag";
		
		public static final String STATUS = "st";
		
		public static final String SPEAKING = "sp";
		
		public static final String RINGING = "r";
		
		public static final String VOIP_CLIENTS = "cl";
		
	}

	
	/**
	 * Call quality
	 */
	
	public static enum CallQuality {
		EXCELLENT,
		GOOD,
		FAIR,
		WEAK,
		UNKNOWN
	}

	/**
	 * Current status of a VoIP Client. 
	 * <p>
	 * <b>IMPORTANT: </b> Do not change the order of this enum. We use the ordinal values.
	 * </p>
	 *
	 */
	public static enum CallStatus
	{
		OUTGOING_CONNECTING, 
		OUTGOING_RINGING, 
		INCOMING_CALL, 
		PARTNER_BUSY, 
		RECONNECTING, 
		ON_HOLD, 
		ACTIVE, 
		ENDED, 
		UNINITIALIZED,
		UNUSED_PLACEHOLDER_FOR_BACKWARD_COMPATIBILITY
	}

	/**
	 * Track packets received for last X seconds
	 */
	public static final int QUALITY_WINDOW = 3;
	
}
