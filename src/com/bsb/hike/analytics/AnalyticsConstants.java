package com.bsb.hike.analytics;

/**
 * @author rajesh
 *
 */
public class AnalyticsConstants 
{	
	/** one minute in milliseconds */
	public static final long ONE_MINUTE =  60 * 1000;
	
	/** one day in seconds */
	public static final int DAY_IN_SECONDS = 24 * 60 * 60;

	/** Default maximum size per file in kilobytes */ 
	public static long MAX_FILE_SIZE = 200; // 200KB

	/** Default maximum analytics size on the client in kilobytes */
	public static long MAX_ANALYTICS_SIZE = 1000; // 1MB
			
	/** Default analytics service status */ 
	public static boolean IS_ANALYTICS_ENABLED = true;

	/** Default maximum events count in memory before they are saved on the disk */
	public static final int MAX_EVENTS_IN_MEMORY = 10;
	
	/** Default frequency at which logs should be tried to be sent to server */
	public static final int DEFAULT_SEND_FREQUENCY = 30; // 30 minutes

	public static final String EVENT_FILE_DIR = "/Analytics";
	
	public static final String NEW_LINE = "\n";
		
	public static final String NORMAL_EVENT_FILE_NAME = "normaldata";
	
	public static final String IMP_EVENT_FILE_NAME = "impdata";
	
	public static final String SRC_FILE_EXTENSION = ".txt";

	public static final String DEST_FILE_EXTENSION = ".gz";

	public static final String ANALYTICS_TAG = "hikeAnalytics";
	
	//TODO @Rajesh, I assume you forgot to set these variables as final.
	public static String TYPE = "t";

	public static String ANALYTICS_EVENT = "le_android";
	
	public static String EVENT_PRIORITY = "ep";

	public static String DATA = "d";
	
	public static String METADATA = "md";

	public static String UI_EVENT = "uiEvent";
	
	public static String MICRO_APP_INFO = "mappInfo";

	public static String NON_UI_EVENT = "nonUiEvent";

	public static final String DEV_EVENT = "devEvent";
	
	public static final String DEV_AREA = "devArea";
	
	public static final String DEV_INFO = "info";

	public static String CLICK_EVENT = "click";

	public static String VIEW_EVENT = "view";

	public static String ERROR_EVENT = "error";
	
	public static String LOG_KEY = "logs";
	
	public static String PHOTOS_ERROR_EVENT = "ph_er";

    public static String MICROAPP_UI_EVENT = "muiEvent";

    public static String MICROAPP_NON_UI_EVENT = "mNonUiEvent";

    public static String LONG_PRESS_EVENT = "longClick";

	public static String SUB_TYPE = "st";

	public static String EVENT_TYPE = "et";

	public static String EVENT_KEY = "ek";

	public static String TO = "to";

    public static String ORIGIN = "org";

	public static String CONTENT_ID = "content_id";

    public static String UNREAD_COUNT = "uc";
    
    public static String SELECTED_USER_COUNT_FWD = "FwdCount";
    
    public static String SELECTED_USER_COUNT_SHARE = "ShareCount";

	public static String DOWNLOAD_EVENT = "dwnld";

	public static final String ANALYTICS = "analytics";

	public static final String ANALYTICS_FILESIZE = "analyticsfs";

	public static final String ANALYTICS_TOTAL_SIZE = "totalfs";

	public static final String ANALYTICS_SEND_FREQUENCY = "analyticsfreq";

	public static final String ANALYTICS_IN_MEMORY_SIZE = "mem_size";

	public static final String ANALYTICS_ALARM_TIME = "alarmsetting";
	
	public static final String SEND_WHEN_CONNECTED = "issendwhenconnected";

	public static final String ANALYTICS_BACKUP = "backup";

	public static final String REMOVE_SUCCESS = "remsucss";

	public static final String REPLACE_SUCCESS = "success";
	
	public static final String REPLACE_FAILURE = "failure";

	public static final String REPLACE_STATUS = "success";
	
	public static final String REMOVE_MICRO_APP = "dmapp";
	
	public static final String NOTIFY_MICRO_APP_STATUS = "nmapp";

	public static final String ANALYTICS_HOME_SEARCH = "search";

	public static final String EVENT_SUB_TYPE = "st";

	public static final String EVENT_TAG = "tag";

	public static final String CURRENT_TIME_STAMP = "cts";

	public static final String EVENT_TAG_MOB = "mob";

    public static final String EVENT_TAG_PLATFORM = "plf";

	public static final String EVENT_TAG_BOTS = "bot";
	
	public static final String EVENT_TAG_PHOTOS = "ph5";

	public static final String CHAT_MSISDN = "chat_msisdn";

	public static final String BOT_NAME = "bot_name";

	public static final String BOT_MSISDN = "bot_msisdn";

	public static final String APP_NAME = "app_name";

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

	public static final String NETWORK_TYPE = "networkType";

	public static final String APP_VERSION = "app_version";

	public static final String HELP_CLICKED = "help_click";
	
	public static String ERROR_TRACE = "error";

	public static String MESSAGES_COUNT = "msg_count";

	public static String TIME_TAKEN = "time";

	public static String UPGRADE_EVENT = "upgrade";
		
	// Edit picture flags and events
	public static final class ProfileImageActions
	{	
		public static final String DP_EDIT_FROM_SETTINGS_PREVIEW_IMAGE = "stngpic";
		
		public static final String DP_EDIT_FROM_PROFILE_SCREEN = "proicon";
		
		public static final String DP_EDIT_FROM_DISPLAY_IMAGE = "propic";
		
		public static final String DP_EDIT_FROM_PROFILE_OVERFLOW_MENU = "editpropic";
		
		public static final String DP_EDIT_EVENT = "editpic";
		
		public static final String DP_REMOVE_EVENT = "rempic";
		
		public static final String DP_REMOVE_CONFIRM_YES = "delpic";
		
		public static final String DP_REMOVE_FROM_FAVOURITES_CHECKED = "check";
	
		public static final String DP_REMOVE_FROM_FAVOURITES_UNCHECKED = "uncheck";
		
		public static final String DP_EDIT_PATH = "flag";
	}
	
	public static final String STICKER_PALLETE = "stkp";
	
	public static final String STICKER_SEARCH = "stks";

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
		
		public static final String MULTIMEDIA = "multimedia";
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
	
	//Added For Last seen Event
	public static final String LAST_SEEN_ANALYTICS_TAG = "lsa";

	public static final String LAST_SEEN_ANALYTICS = "lsa";
	
	//Added For Message Reliability
	
	//These Max Range are fixed for All three
	public static final int MAX_RANGE_TEXT_MSG = 10000;
	
	public static final int MAX_RANGE_STK_MSG = 10000;
	
	public static final int MAX_RANGE_MULTIMEDIA_MSG = 10000;
	
	//----------------------***----------------------------------//
	
	//These three values are server configured
	public static int text_msg_track_decider = 100;
	
	public static int stk_msg_track_decider = 100;
	
	public static int multimedia_msg_track_decider = 100;
	
	//----------------------***----------------------------------//
	
	public static final String T_USER = "t_user";
	
	public static final String TRACK_ID = "track_id";
	
	public static final String MSG_REL = "rel_m";
	
	public static final String MSG_REL_TAG = "rel_m";
	
	public static final String REL_EVENT_STAGE = "stg";
	
	public static final String MSG_REL_CONST_STR = "ek";
	
	public static final String APP_VERSION_NAME = "app_ver";

	public static final String MICRO_APP_REPLACED = "repl";
	
	public static final String MICRO_APP_ID = "rep_id";
	
	public static final String REASON_CODE = "rs";

	public static final String MESSAGE_DATA = "msgData";

	public static final String SERVICE_STATS = "srvStat";

	public static final class MsgRelEventType
	{
		public static final String SEND_BUTTON_CLICKED = "1";

		public static final String DB_ADD_TRANSACTION_COMPLETED = "1.2";
		
		public static final String DB_UPDATE_TRANSACTION_COMPLETED = "1.3";

		public static final String RECV_NOTIF_SOCKET_WRITING = "1.4";

		public static final String SINGLE_TICK_ON_SENDER = "2";

		public static final String SENDER_RECV_ACK = "3";

		public static final String DR_SHOWN_AT_SENEDER_SCREEN = "4";
		
		public static final String MR_SHOWN_AT_SENEDER_SCREEN = "5";

		public static final String SENDER_MQTT_RECV_SENDING_MSG = "5.9";

		public static final String RECEIVER_MQTT_RECVS_SENT_MSG = "9.1";

		public static final String RECEIVER_MQTT_RECV_MSG_ACK = "10.1";

		public static final String DR_RECEIVED_AT_SENEDER_MQTT = "13.1";

		public static final String RECEIVER_MQTT_RECV_MR_FROM_RECEIVER = "13.9";

		public static final String RECIEVR_RECV_MSG = "18";

		public static final String RECEIVER_OPENS_CONV_SCREEN = "19";
	}
	
	public static final class StealthEvents
	{
		public static final String STEALTH = "stlth";

		public static final String STEALTH_MARK_HIDDEN = "MH";

		public static final String STEALTH_MARK_VISIBLE = "MV";

		public static final String STEALTH_HIDE_CHAT = "HC";

		public static final String STEALTH_CONV_MARK = "stlthMrk";

		public static final String STEALTH_SETUP = "stlthStp";

		public static final String STEALTH_ACTIVATE = "stlthActv";

		public static final String STEALTH_PASSWORD_ENTRY = "entStlthPwd";

		public static final String STEALTH_RESULT = "stlthRslt";

		public static final String STEALTH_PASSWORD_CORRECT = "pwdCrct";

		public static final String STEALTH_PASSWORD_CHANGE = "pwdChng";

		public static final String STEALTH_REQUEST = "stlthRqst";
		
		public static final String TIP_REMOVE = "rmTip";
		
		public static final String TIP_SHOW = "shTip";
		
		public static final String TIP_HIDE = "hdTip";

		public static final String STEALTH_HI_CLICK = "hiClk";
		
		public static final String STEALTH_PREFERENCE_CHANGE = "prefChng";
	}
	
	public static final class ChatHeadEvents
	{
		public static final String INFOICON_WITHOUT_CLICK = "iWoC";
		
		public static final String STICKER_HEAD_DISMISS = "stkrHdDs";
		
		public static final String STICKER_SHARE = "stkrShr";
		
		public static final String STICKER_SHOP = "stkrShp";
		
		public static final String INFOICON_CLICK = "iClk";
		
		public static final String STICKER_HEAD = "stkrHd";
		
		public static final String SETTING = "stg";
		
		public static final String HIKE_STICKER_SETTING = "hkStkrStg";
		
		public static final String TEXT_CLICK_SETTING = "txtStg";
		
		public static final String DISABLE_SETTING = "dsbl";
		
		public static final String MORE_STICKERS = "mrStkr";
		
		public static final String OPEN_HIKE = "openHk";
		
		public static final String SNOOZE_TIME = "snz";
		
		public static final String MAIN_LAYOUT_CLICKS = "mLClk";
		
		public static final String ONE_HOUR = "oneHr";
		
		public static final String EIGHT_HOURS = "eghtHr";
		
		public static final String ONE_DAY = "oneDay";
		
		public static final String BACK = "bck";
		
		public static final String STICKER_WDGT = "stkrWgt";
		
		public static final String DISMISS_LIMIT = "dsLmt";

		public static final String SHARE_LIMIT = "shrLmt";
		
		public static final String CAT_ID = "catId";

		public static final String STICKER_ID = "stkrId";
		
		public static final String SOURCE = "s";
		
		public static final String SELECT_ALL = "sAClk";
		
		public static final String APP_CLICK = "aClk";

		public static final String APP_CHECKED = "t";

		public static final String APP_UNCHECKED = "f";

		public static final String DISABLE_TEXT = "dsblTxt";

	}
	
	
	public static final class StickyCallerEvents
	{
		public static final String STICKY_CALLER = "stkyClr";
		
		public static final String CALL_TYPE = "clTyp";
		
		public static final String NUMBER_TYPE = "noTyp";
		
		public static final String MSISDN = "msisdn";
		
		public static final String SUCCESS = "success";
		
		public static final String FAIL = "fail";
	
		public static final String SOURCE = "src";
		
		public static final String CALL_EVENT = "clEvnt";
		
		public static final String FETCHING = "fetch";
		
		public static final String KNOWN = "knw";
		
		public static final String UNKNOWN = "unKnw";
		
		public static final String MISSED = "miss";
	
		public static final String CLIPBOARD = "clip";
		
		public static final String RECEIVED = "rcv";
		
		public static final String DIALED = "dial";
		
		public static final String SERVER = "srv";
		
		public static final String CACHE = "cac";

		public static final String ALREADY_SAVED = "svd";

		public static final String STATUS = "stat";
		
		public static final String CALL_BUTTON = "call";
		
		public static final String FREE_CALL_BUTTON = "frCl";
		
		public static final String SMS_BUTTON = "sms";
		
		public static final String FREE_SMS_BUTTON = "frSms";
		
		public static final String CALLER_SETTINGS_BUTTON = "clrStg";
		
		public static final String ACTIVATE_BUTTON = "activ";
		
		public static final String DEACTIVATE_BUTTON = "dActiv";
		
		public static final String CLOSE_BUTTON = "close";

		public static final String CARD = "crd";

		public static final String SAVE_CONTACT = "svCntct";

		public static final String HIKE = "hike";
		
		public static final String CALLER_SETTINGS_TOGGLE = "clrStgTgl";

		public static final String KNOWN_CARD_SETTINGS_TOGGLE = "knwStgTgl";
		
		public static final String WRONG_JSON = "wrngJsn";

		public static final String SMS_CARD_SETTINGS_TOGGLE = "smsStgTgl";
		
	}
	
	public static String EVENT_SOURCE = "sr";
	
	public static final String WITH_RED_DOT = "wr";
	
	public static final String DISPLAY_PIC = "dp";
	
	public static final String PICTURE_UPDATE = "pu";
	
	public static final String PICTURE_TEXT = "pt";
	
	public static final String STATUS_UPDATE = "su";
	
	public static final String UPDATE_TYPE = "ut";
	
	public static final String TIMELINE_U_ID = "tid";
	
	public static final String TIMELINE_OPTION_TYPE = "ot";
	
	public static final String APP_CRASH_EVENT = "appCrash";
	
	public static final String BOT_NOTIF_TRACKER = "bno";
	
	public static final String BOT_DISCOVERY = "bd";
	
	public static final String DISCOVERY_BOT_DOWNLOAD = "dbd";
	
	public static final String GET_DISCOVERY_BOT_LIST = "gdb";
	
	public static final String DISCOVERY_BOTS = "db";
	
	public static final String MICRO_APP_EVENT = "micro_app";
	
	public static final String EVENT = "event";

	public static final String LOG_FIELD_1 = "fld1";
	
	public static final String LOG_FIELD_5 = "fld5";
	
	public static final String LOG_FIELD_6 = "fld6";

	public static final String FILE_SIZE = "fs";

	public static final String INTERNAL_STORAGE_SPACE = "mem";

	public static final String DISCOVERY_BOT_TAP = "bd_tap";
}
