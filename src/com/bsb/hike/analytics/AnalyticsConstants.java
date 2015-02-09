package com.bsb.hike.analytics;

/**
 * @author rajesh
 *
 */
public class AnalyticsConstants 
{	
	public static final long ONE_DAY = 24 * 60 * 60 * 1000;
	
	public static final long ONE_HOUR =  60 * 60 * 1000;
	
	//TODO: changed from 500 to 5 for testing
	public static long MAX_FILE_SIZE = 5 * 1024;

	//TODO: changed from 500 to 10 for testing
	public static long MAX_ANALYTICS_SIZE = 10 * 1024;
	
	/** time of first attempt to send analytics data */ 
	public static int HOUR_OF_DAY_TO_SEND = 8;
	
	/** number of times upload should be tried in one day */
	public static int ANALYTICS_UPLOAD_FREQUENCY = 3;
	
	/** try sending analytics data every 4 hours */
	public static int UPLOAD_TIME_MULTIPLE = 4;
	
	public static boolean IS_ANALYTICS_ENABLED = true;
	
	//TODO: changed from 50 to 10 for testing
	public static final int MAX_EVENTS_IN_MEMORY = 10;
	
	public static final int DEFAULT_SEND_FREQUENCY = 24;
	
	public static final String HTTP_UPLOAD_URL = "http://staging.im.hike.in/v1/logs/analytics"; 
	
	public static final String EVENT_FILE_DIR = "/Analytics";
	
	public static final String NEW_LINE = "\n";
		
	public static final String NORMAL_EVENT_FILE_NAME = "normaldata";
	
	public static final String IMP_EVENT_FILE_NAME = "impdata";
	
	public static final String SRC_FILE_EXTENSION = ".txt";

	public static final String DEST_FILE_EXTENSION = ".gz";

	public static final String ANALYTICS_TAG = "hikeAnalytics";
	
	public static String TYPE = "t";

	public static String ANALYTICS_EVENT = "le_android";
	
	public static String EVENT_PRIORITY = "ep";

	public static String DATA = "d";
	
	public static String METADATA = "md";

	public static String UI_EVENT = "uiEvent";

	public static String NON_UI_EVENT = "nonUiEvent";

	public static String CLICK_EVENT = "click";

	public static String SUB_TYPE = "st";

	public static String EVENT_TYPE = "et";

	public static String EVENT_KEY = "ek";

	public static String TO = "to";

	public static final String ANALYTICS = "analytics";

	public static final String ANALYTICS_FILESIZE = "analyticsfs";

	public static final String ANALYTICS_TOTAL_SIZE = "totalfs";

	public static final String ANALYTICS_SEND_FREQUENCY = "analyticsfreq";
	
	public static final String ANALYTICS_ALARM_TIME = "analyticsalarmtime";

	public static final String ANALYTICS_BACKUP = "backup";

	public static final String EVENT_SUB_TYPE = "st";

	public static final String EVENT_TAG = "tag";

	public static final String CURRENT_TIME_STAMP = "cts";

	public static final String EVENT_TAG_MOB = "mob";

	public static final String EVENT_TAG_CBS = "cbs";

	public static final String DEVICE_DETAILS = "devicedetails";

	public static final String DEVICE_STATS = "devicestats";

	public static final String FILE_TRANSFER = "filetransfer";

	public static final String EXIT_FROM_GALLERY = "exitFromGallery";

	public static final String HIKE_SDK_INSTALL_ACCEPT = "hikeSDKInstallAccept";

	public static final String HIKE_SDK_INSTALL_DECLINE = "hikeSDKInstallDecline";

	public static final String ANALYTICS_THREAD_WRITER = "THREAD-WRITER";

	// Added For Session
	public static final String SESSION_ID = "sid";

	public static final String CONNECTION_TYPE = "con";

	public static final String SOURCE_APP_OPEN = "src";

	public static final String EVENT_TAG_SESSION = "session";

	public static final String SOURCE_CONTEXT = "srcctx";

	public static final String CONVERSATION_TYPE = "slth";

	public static final String MESSAGE_TYPE = "msg_type";

	public static final String SESSION_EVENT = "session";

	public static final String SESSION_TIME = "tt";

	public static final String APP_OPEN_SOURCE_EXTRA = "appOpenSource";

	public static final String DATA_CONSUMED = "dcon";
	
	public static final String FOREGROUND = "fg";
	
	public static final String BACKGROUND = "bg";

	public static final class MessageType
	{
		public static final String NUDGE = "nudge";

		public static final String STICKER = "stk";

		public static final String TEXT = "text";

		public static final String IMAGE = "image";

		public static final String VEDIO = "video";

		public static final String AUDIO = "audio";

		public static final String LOCATION = "location";

		public static final String CONTACT = "contact";
	}

	public static final class ConversationType
	{
		public static final int NORMAL = 0;

		public static final int STLEATH = 1;

	}

	public static final class AppOpenSource
	{

		public static final String REGULAR_APP_OPEN = "regular_open";

		public static final String FROM_NOTIFICATION = "notif";
	}
	
	//Added For Chat Session
	public static final String CHAT_ANALYTICS = "ctal";
	
	public static final String TO_USER = "to_user";
	
	public static final String EVENT_TAG_CHAT_SESSION = "ctal";
}
