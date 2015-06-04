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
	
	public static final String SUBNET = "192.168.43.";

	public static final String IP_SERVER = "192.168.43.1";
	
	public static final int CHUNK_SIZE=1024;
	
	public static final String GHOST = "gst";
	
	public static final String STICKER_PATH = "stickerPath";

	public static final String PING = "ping";
	
	public static final String EXTRAS_APK_PATH = "apkPath";

	public static final String EXTRAS_APK_NAME = "apkName";
	
	public static final String IS_OFFLINE_MESSAGE = "is_offline_message";
	
	public static final int  WAITING_TIMEOUT = 60000;

	public static final long TIME_TO_CONNECT = 30000;

	public static final long TRY_CONNECT_TO_HOTSPOT = 3000;

	public static final int MAXTRIES_FOR_SCAN_RESULTS = 3;
	
	public final class HandlerConstants
	{
		public static final int SAVE_MSG_DB=-101;
		public static final int DISCONNECT_AFTER_TIMEOUT = -102;
		public static final int CREATE_HOTSPOT = -103;
		public static final int CONNECT_TO_HOTSPOT = -104;
		public static final int REMOVE_CONNECT_MESSAGE = -105;
		public static final int START_SCAN = -106;
		public static final int STOP_SCAN = -107;
	}
}
