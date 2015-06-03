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

	public static final String START_CONNECT_FUNCTION = "startConnectFunction";

	public static final String OFFLINE_MESSAGE_CONNECTED_TYPE = "offmsgconn";
	
	public static final String OFFLINE_MESSAGE_DISCONNECTED_TYPE = "offmsgdis";
	
	public final class HandlerConstants
	{
		public static final int SAVE_MSG_DB=-101;
	}
	
	public static enum ERRORCODE
	{
		TIMEOUT,USERDISCONNECTED,OUT_OF_RANGE
	}
}
