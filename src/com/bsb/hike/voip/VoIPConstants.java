package com.bsb.hike.voip;


public class VoIPConstants {
	public static final String TAG = "VoIP";
	
	// Relay and ICE server 
	public static final String ICEServerName = "relay.hike.in";
	public static final int ICEServerPort = 9999;

	public static final int AUDIO_SAMPLE_RATE = 48000; 

	/**
	 * Time (ms) to wait before the client being called replies with its
	 * own socket information.
	 */
	public static final int TIMEOUT_PARTNER_SOCKET_INFO = 15000;
	
	/**
	 * Time (ms) to wait for the person being called to accept or decline a call.
	 */
	public static final int TIMEOUT_PARTNER_ANSWER = 30000;

	// Intent actions
	public static final String PARTNER_REQUIRES_UPGRADE = "pru";
	public static final String PARTNER_INCOMPATIBLE = "pi";
	public static final String PARTNER_IN_CALL = "incall";
	public static final String PARTNER_HAS_BLOCKED_YOU = "blocked";
	public static final String INCOMING_NATIVE_CALL_HOLD = "hold";
	
	// Default bitrates
	public static final int BITRATE_2G = 12000;
	public static final int BITRATE_3G = 16000;
	public static final int BITRATE_WIFI = 48000;

	public static final String CALL_ID = "callId";
	public static final String IS_CALL_INITIATOR = "isCallInitiator";
	public static final String CALL_RATING = "rating";
	public static final String CALL_NETWORK_TYPE = "network";
	public static final String PARTNER_MSISDN = "pmsisdn";
	public static final String CALL_FAILED_REASON = "callfailreason";
	public static final String PARTNER_NAME = "pname";
	public static final String CALL_RATE_BUNDLE = "callRateBundle";

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
	public static final int MSG_UPDATE_HOLD_BUTTON = 12;
	public static final int MSG_ALREADY_IN_NATIVE_CALL = 13;
	public static final int MSG_AUDIORECORD_FAILURE = 14;
	public static final int MSG_UPDATE_REMOTE_HOLD = 15;
	public static final int MSG_VOIP_CLIENT_STOP = 16;
	public static final int MSG_VOIP_CLIENT_OUTGOING_CALL_RINGTONE = 17;
	public static final int MSG_VOIP_CLIENT_INCOMING_CALL_RINGTONE = 18;

	public static final class Analytics
	{
		public static final String CALL_RATING = "rate";

		public static final String CALL_ID = "callid";

		public static final String IS_CALLER = "caller";

		public static final String IS_ENDER = "ender";

		public static final String NETWORK_TYPE = "net";

		public static final String CALL_ISSUES = "issues";

		public static final String NEW_LOG ="nl";

		public static final String OLD_NETWORK_TYPE = "oldnet";

		public static final String CALL_SOURCE = "source";

		public static final String DATA_SENT = "dsent";

		public static final String DATA_RECEIVED = "drec";

		public static final String STATE = "state";

		public static final String DURATION = "dur";

		public static final String CALL_CONNECT_FAIL_REASON = "reason";
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

		public static final String MESSAGE = "message";

		public static final String SET_PARTNER_INFO = "setpartnerinfo";

		public static final String OUTGOING_CALL = "outgoingcall";

		public static final String MSISDN = "msisdn";

		public static final String CALL_SOURCE = "call_source";

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

	public static enum CallStatus
	{
		OUTGOING_CONNECTING, OUTGOING_RINGING, INCOMING_CALL, PARTNER_BUSY, RECONNECTING, ON_HOLD, ACTIVE, ENDED, UNINITIALIZED
	}

	/**
	 * Track packets received for last X seconds
	 */
	public static final int QUALITY_WINDOW = 3;
	
}
