package com.bsb.hike.offline;

/**
 * 
 * @author himanshu
 *	
 *	This class contains the constants foe Offline Messaging stuff.
 */
public class OfflineConstants
{
	public static  final int MAXTRIES = 1;
	
	public static final int PORT_PING = 18977;

	public static final int PORT_FILE_TRANSFER = 18988;

	public static final int PORT_TEXT_MESSAGE = 18999;

	public static final int SOCKET_TIMEOUT = 5000;

	public static final int pingTimeout = 60 * 1000;
	
	public static final long WAITING_TIME_TO_DISCONNECT = 100;
	
	public static final String SUBNET = "192.168.43.";

	public static final String IP_SERVER = "192.168.43.1";
	
	public static final int CHUNK_SIZE= 256*1024;
	
	public static final String GHOST = "gst";
	
	public static final String ACK = "offline_ack";
	
	public static final String MSG_ID = "offline_msg_id";
	
	public static final String FILE_TRANSFER_ACK = "file_transfer_ack";
	
	public static final String STICKER_PATH = "stickerPath";

	public static final String PING = "ping";
	
	public static final String EXTRAS_APK_PATH = "apkPath";

	public static final String EXTRAS_APK_NAME = "apkName";
	
	public static final String IS_OFFLINE_MESSAGE = "is_offline_message";

	public static final String START_CONNECT_FUNCTION = "startConnectFunction";

	public static final String OFFLINE_MESSAGE_CONNECTED_TYPE = "offmsgconn";
	
	public static final String OFFLINE_MESSAGE_DISCONNECTED_TYPE = "offmsgdis";
	
	public static final String  OFFLINE_FILES_NOT_RECEIVED_TYPE = "offfilenotreceived"; 
	
	public static final long TIME_TO_CONNECT = 60000;

	public static final long TRY_CONNECT_TO_HOTSPOT = 5000;

	public static final int MAXTRIES_FOR_SCAN_RESULTS = 3;
		
	public static final int  GHOST_PACKET_SEND_TIME = 10*1000;
	
	public static final int  GHOST_PACKET_DISCONNECT_TIMEOUT = 20*1000;
	
	public static final String INFO_PKT = "info";
	
	public static final int STICKER_SIZE = 168;
	
	public final class HandlerConstants
	{
		public static final int SAVE_MSG_DB = -101;

		public static final int DISCONNECT_AFTER_TIMEOUT = -102;

		public static final int CREATE_HOTSPOT = -103;

		public static final int CONNECT_TO_HOTSPOT = -104;

		public static final int REMOVE_CONNECT_MESSAGE = -105;

		public static final int START_SCAN = -106;

		public static final int STOP_SCAN = -107;

		public static final int SEND_GHOST_PACKET = -108;

		public static final int RECONNECT_TO_HOTSPOT = -109;

		public static final int SAVE_MSG_PERSISTANCE_DB = -110;
		
		public static final int SEND_PERSISTANCE_MSGS = -111;
		
		public static final int SHUTDOWN = -112;

		public static final int DISCONNECT_BY_USER = -113;

		public static final int MOVE_MESSAGES_TO_MQTT = -114;

		public static final int REMOVE_CONNECT_REQUEST = -115;
	}
	
	public static enum ERRORCODE
	{
		TIMEOUT,DISCONNECTING,OUT_OF_RANGE,COULD_NOT_CONNECT,REQUEST_CANCEL,SHUTDOWN
	}
	
	public static enum OFFLINE_STATE
	{
		NOT_CONNECTED, CONNECTING, CONNECTED,DISCONNECTING,DISCONNECTED
	}
	
	public static final int MAX_TRIES=4;

	public static final String FILE_TYPE = "fileType";

	public static final String WIFI_HOTSPOT_STATE = "android.net.wifi.WIFI_AP_STATE_CHANGED";

	public static final int ALL_THREADS_CONNECTED = 2;
	
	public static final String WIFI_HOTSPOT_STATE_CHANGE_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";

	public static final int WIFI_HOTSPOT_STATE_DISABLING = 10;

	public static final int WIFI_HOTSPOT_STATE_DISABLED = 11;

	public static final int WIFI_HOTSPOT_STATE_ENABLING = 12;

	public static final int WIFI_HOTSPOT_STATE_ENABLED = 13;

	public static final int WIFI_HOTSPOT_STATE_UNKNOWN = 14;

	public static final String SPACE_CHECK = "space_check";

	public static final String SPACE_AVAILABLE = "space_available";
	
	public static final String SPACE_ACK = "space_ack";

	public static final String TEXT_TOPIC = "text";

	public static final String FILE_TOPIC = "file";

	public static final String APK_SELECTION_RESULTS = "apk_selection_results";

	public static final String OFFLINE_FTUE_INFO = "offline_ftue_info";

	public static final String OFFLINE_FTUE_SHOWN_AND_CANCELLED = "offline_ftue_shown";

	public static final String FIRST_OFFLINE_MSISDN = "first_offline_msisdn";

	public static final String DISCONNECT_CLIKED = "Disconnected";
	
	public static final String OFFLINE_MSISDN = "offlineState";

	public static final String CURRENT_RECIEVING_MSG_ID = "recMsgID";

	public static final String OFFLINE_DISCONNECT_FRAGMENT = "offline_disconnect_fragment";

	public static final String OFFLINE_ANIMATION_FRAGMENT = "offline_animation_dialog";

	public static final int WIFI_RETRY_COUNT = 40;

	public static final int  FIRST_MESSAGE_TIME = 8000;

	public static final int SECOND_MESSAGE_TIME = 18300;

	public static final int TIMER_START_TIME = 18000;
	
	public static final int NOTIFICATION_IDENTIFIER = -101;

	public static final String OFFLINE_INDICATOR_CLICKED = "offline_indicator_clicked";
	
	public static final class AnalyticsConstants
	{
		public static final String EVENT_TYPE_OFFLINE = "hdir";

		public static final String EVENT_KEY_PUSH = "hdstrt";
		
		public static final String EVENT_KEY_CONNECTING_POP_UP_CLICK = "hdpup";
		
		public static final String EVENT_KEY_CONNECTED_POP_UP_CLICK = "hdpup2";
		
		public static final String EVENT_KEY_CANCEL = "hdcnl";
				
		public static final String TAG_NO_INTERNET_CLICKED = "noti";
		
		public static final String RETRY_BUTTON_CLICKED = "rtry";
		
		public static final String EVENY_KEY_CONN_TIME = "est";

		public static final String EVENY_KEY_DISCONN_REA = "hddis";

		public static final String TIP_KEY = "tipkey";

	}
	
	public static final String CONNECTION_ID = "conn_id";

	public static final String TIMEOUT = "tm";

	public static enum MessageType
	{
		SENT, RECEIVED
	}

}
