package com.hike.transporter.utils;

public class TConstants
{
	public static final int CHUNK_SIZE = 256 * 1024; // 256 KB

	public static final String TYPE = "t";

	public static final String ACK = "ack";

	public static final String AWB = "awb";

	public static final String HEARTBEAT = "h";

	public static final String SCREEN = "scr";

	public static final String TEXT = "text";

	public static final String FILE = "file";

	public static final String DATA = "d";

	public static final String FILESIZE = "fs";

	public static final String HANDSHAKE = "hS";

	public static final String FILE_REQUEST = "fr";

	public static final String FILE_REQUEST_REPLY = "frr";
	
	public static final String HANDSHAKE_SERVER="hss";

	public static final String CODE = "code";

	public enum State
	{
		CLOSED, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED
	}

	public static final String TOPIC = "topic";

	public static final String APPLICATION = "app";

	public static final int GHOST_PACKET_SEND_TIME = 10 * 1000;

	public static String CONFIG = "config";

	public static String MSG = "Message";

	public static String TAG = "tag";

	public static String PERSISTANCE = "persistance";

	public static String CONNECTEDSTATUS = "conStat";

	public static final class TDBConstants
	{
		public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS ";

		public static final String PERSISTANCE_TABLE = "PERSISTMSG_TABLE";

		public static final String _ID = "_ID";

		public static final String NAMESPACE = "NAMESPACE";

		public static final String MESSAGE = "MESSAGE";

		public static String AWB = "awb";

		public static String TRANSPOTER = "TransporterDB";

		public static int DBVERION = 1;
	}

	public static final class THandlerConstants
	{
		public static final int SAVE_MSG_DB = -1001;

		public static final int DEL_MSG_DB = -1002;

		public static final int SEND_ALL_MSG = -1003;

		public static final int SEND_HEARTBEAT_PACKET = -1004;

		public static final int DISCONNECT_AFTER_TIMEOUT = -1005;
	}

	public static final int SUCCESS = 100;
	
	public static final String TTLCNN = "ttalConn";

	public static enum ERRORCODES
	{
		SD_CARD_NOT_PRESENT,

		SD_CARD_NOT_WRITABLE,

		NOT_ENOUGH_MEMORY,

		UNKNOWN_ERROR,

		FILE_NOT_EXISTS, NOT_CONNECTED, UNKNOWN;

		public static ERRORCODES getEnumValue(int val)
		{
			if (val >= ERRORCODES.values().length)
			{
				return ERRORCODES.UNKNOWN;
			}
			return ERRORCODES.values()[val];
		}
	};
}
