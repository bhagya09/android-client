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
	public static final Object FILE_DOWNLOADED ="FileDownloaded" ;
	public static final String RESULT_CODE = "resultCode";
	public static final String MICRO_APP_LOADED ="microAppLoaded";
	public static final String MICRO_APP_OPENED ="microAppOpened" ;
	public static final String USER_GOOGLE_ACCOUNTS_SENT = "userAccountsSent";
	public static final String ACCOUNT_TYPE_GOOGLE = "com.google";
	public static final String EVENTS_TO_UPLOAD_COUNT = "events_to_upload";
	public static final int DEFAULT_THRESHOLD_EVENTS_TO_UPLOAD = 10; //10 events approx size: 1.5 KB, good enough to gzip and upload

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

	public static String HTTP_EVENT = "rel_http";

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

    public static String CLICK_COUNT = "clk_count";

	public static String TIME_TAKEN = "time";

	public static String UPGRADE_EVENT = "upgrade";
	public static Object BOT_CONTENT_FORWARDED = "bot_content_forwarded";
	public static Object BOT_CONTENT_DOWNLAODED = "bot_content_downloaded";
	public static String EVENT_DELETE_ACCOUNT = "evAccDel";

	public static String DELETE_ACCOUNT = "delAcc";

	public static String RESET_ACCOUNT = "resAcc";

	public static String EVENT_USER_GOOGLE_ACCOUNTS = "evUsrGoogleAccs";

	public static String USER_GOOGLE_ACCOUNTS = "usrGoogleAccs";
	public static String FORWARD = "forward";
	public static String BOT_CONTENT_SHARED = "bot_content_shared";

	public static String DOWNLOAD_PAUSED = "download_paused";
	public static String DOWNLOAD_RESUMED = "download_resumed";
	public static String PLATFORM_NOTIFICATION = "platform_notification";
	public static String PLATFORM_RICH_NOTIF = "platform_rich_notif";
	public static String TTL_EXPIRED = "ttlExpired";

	//Constants for v2 (Normalized Taxonomy)
	public static final class V2
	{
		/*package*/ static final String VERSION = "ver";

		/*package*/ static final String RECORD_ID = "r";

		/*package*/ static final String CTS = "cts";

		public static final String VERSION_VALUE = "v2";

		public static final String UNIQUE_KEY = "uk";

		public static final String KINGDOM = "k";

		public static final String PHYLUM = "p";

		public static final String CLASS = "c";

		public static final String ORDER = "o";

		public static final String FAMILY = "fa";

		public static final String GENUS = "g";

		public static final String SPECIES = "s";

		public static final String VARIETY = "v";

		public static final String FORM = "f";

		public static final String VAL_STR = "vs";

		public static final String VAL_INT = "vi";

		public static final String FROM_USER = "fu";

		public static final String DEVICE_ID = "di";

		public static final String REC_ID = "ri";

		public static final String TO_USER = "tu";

		public static final String USER_STATE = "us";

		public static final String TS = "ts";

		public static final String NETWORK = "nw";

		public static final String SOURCE = "src";

		public static final String SERIES = "ser";

		public static final String CENSUS = "cs";

		public static final String BREED = "b";

		public static final String RACE = "ra";

		public static final String POPULATION = "pop";
	}

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

	public static final String STICKER_SEARCH_BACKEND = "ssb";

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

	public static final class AutoApkEvents
	{
		public static final String RECEIVED_INITIAL_PING = "atapk_chk1";

		public static final String MAKING_SERVER_HTTP_REQUEST = "atapk_rsp1";

		public static final String SIZE_VALIDITY = "atapk_sz_vld";

		public static final String UPDATE_VALIDITY = "atapk_up_vld";

		public static final String NETWORK_VALIDITY = "atapk_nw_vld";

		public static final String SERVER_RESPONSE_HTTP = "atapk_cnf";

		public static final String RESUMING_DOWNLOAD = "atapk_resm";

		public static final String INITIATING_DOWNLOAD = "atapk_init";

		public static final String DOWNLOAD_COMPLETION = "atapk_cplt";

		public static final String FILE_VALIDITY = "atapk_fl_vld";

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

		public static final String SMS = "sms";

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

		public static final String CLOSE_SWIPE = "cls_swp";

		public static final String CARD = "crd";

		public static final String SAVE_CONTACT = "svCntct";

		public static final String HIKE = "hike";
		
		public static final String CALLER_SETTINGS_TOGGLE = "clrStgTgl";

		public static final String KNOWN_CARD_SETTINGS_TOGGLE = "knwStgTgl";
		
		public static final String WRONG_JSON = "wrngJsn";

		public static final String SMS_CARD_SETTINGS_TOGGLE = "smsStgTgl";

		public static final String AFTER_INCOMING_UNKNOWN = "aftInc";

		public static final String AFTER_OUTGOING_UNKNOWN = "aftOut";

		public static final String BLOCK = "blk";

		public static final String BLOCK_LIST = "blkLst";

		public static final String POSITIVE_CLICKED = "pos";

		public static final String NEGATIVE_CLICKED = "neg";

		public static final String BLOCK_DIALOG = "blkDlg";

		public static final String UNBLOCK_DIALOG = "unblkDlg";

		public static final String STATIC_QUICK_REPLY_BUTTON = "sqrep";

		public static final String CUSTOM_QUICK_REPLY_BUTTON = "cqrep";

		public static final String QUICK_REPLY_CLOSE_BUTTON = "qrcb";

		public static final String QUICK_REPLY = "qrep";

		public static final String CHAT_THREAD_SPAM_BUTTON = "ctsbutton";

		public static final String TEXT = "txt";
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
	
	public static final String BOT_VIA_MENU = "bvmenu";
	
	public static final String BOT_VIA_HOME_MENU = "home_menu";
	
	public static final String BOT_OPEN_SOURCE_DISC = "bot_discovery";
	
	public static final String BOT_OPEN_SOURCE_NOTIF = "bot_notif";
	
	public static final String BOT_OPEN_SOURCE_SHORTCUT = "bot_shortcut";
	
	public static final String BOT_OPEN_SOURCE = "bot_source";
	
	public static final String BOT_DISCOVERY = "bd";
	
	public static final String DISCOVERY_BOT_DOWNLOAD = "dbd";
	
	public static final String GET_DISCOVERY_BOT_LIST = "gdb";
	
	public static final String DISCOVERY_BOTS = "db";
	
	public static final String MICRO_APP_EVENT = "micro_app";
	
	public static final String EVENT = "event";

	public static final String LOG_FIELD_1 = "fld1";

	public static final String LOG_FIELD_2 = "fld2";

    public static final String LOG_FIELD_3 = "fld3";

	public static final String LOG_FIELD_4 = "fld4";
	
	public static final String LOG_FIELD_5 = "fld5";
	
	public static final String LOG_FIELD_6 = "fld6";

	public static final String UPDATE_TIP_CLICKED = "ugtClk";
	
	public static final String INVITE_TIP_CLICKED = "ivtClk";
	
	public static final String PERSISTENT_NOTIF_CLICKED = "ugPnClk";
	
	public static final String UPDATE_TIP_DISMISSED = "ugtDsm";
	
	public static final String INVITE_TIP_DISMISSED = "ivtDsm";

	public static final String UPDATE_INVITE_TIP = "ugIvTp";

    public static final String UPDATE_TIP_SHOWN = "ugtShw";

    public static final String INVITE_TIP_SHOWN = "ivtShw";

    public static final String UPDATE_PERSISTENT_NOTIF = "ugPnNtf";
			
	public static final String FILE_SIZE = "fs";

	public static final String INTERNAL_STORAGE_SPACE = "mem";

	public static final String DISCOVERY_BOT_TAP = "bd_tap";

	public static final String USER_LOCATION = "location";

    public static final String PLATFORM_CONTENT_DIRECTORY = "platform_content_directory";

    public static final String HIKE_MICRO_APPS_DIRECTORY = "hike_micro_apps_directory";

    public static final String HIKE_WEB_MICRO_APPS_DIRECTORY = "hike_web_micro_apps_directory";

    public static final String GAMES_DIRECTORY = "games_directory";

    public static final String MAPPS_DIRECTORY = "mapps_directory";

    public static final String POPUP_DIRECTORY = "popup_directory";

	public static final String DATABASE_ERROR = "db_error";

	public static final String DATABASE_AREA = "db";

	public static final String SQLITE_PROPERTY = "sqlite_prop";

	public static final class InterceptEvents
	{
		public static final String INTERCEPTS = "intrcpt";

		public static final String INTERCPET_NOTIF_EVENT = "incNtf";

		public static final String INTERCEPT_ACTION = "incActn";

		public static final String INTERCEPT_SCREENSHOT = "incScrn";

		public static final String INTERCEPT_IMAGE = "incImg";

		public static final String INTERCEPT_VIDEO = "incVid";

		public static final String INTERCEPT_NOTIF_CREATED = "incNtfCr";

		public static final String INTERCEPT_SHARE_CLICKED = "incShrClk";

		public static final String INTERCEPT_SET_DP_CLICKED = "incDPClk";

		public static final String INTERCEPT_IMAGE_CLICKED = "incImgClk";

		public static final String INTERCEPT_SETTING_TURNED_ON = "incStgOn";

		public static final String INTERCEPT_SETTING_TURNED_OFF = "incStgOff";

		public static final String INTERCEPT_CLICK_FOR_DELETED_FILE = "incDltClk";
	}

    public static final String DISK_CONSUMPTION_ANALYTICS = "disk_consumption";

    public static final String MICROAPP_DISK_CONSUMPTION = "microapp_disk_consumption";

    public static final String APP_UPDATE_TRIGGER = "app_updated";

    public static final String MICROAPPS_MIGRATION_SUCCESS_TRIGGER = "microapps_migration_success";

	public static String ANALYTICS_EXTRA = "analyticsExtra";

	public static String CHROME_CUSTOM_TABS = "chromeCustomTabs";

	public static final String JUST_OPENED = "justOpened";
	public static final String CHROME_TABS_SUPPORTED = "chromeTabsSupported";
	public static final String CHROME_TABS_UNSUPPORTED = "chromeTabsUnSupported";

	public static final String ACT_LOG_2 = "act_log2";

	public static final String ACT_USERS = "act_users";

	public static final String ACT_STICKER_LOGS = "act_stck";

	public static final String ACT_ABTEST_LOGS = "act_ab";

	public static final String CHAT_OPEN = "chat_open";

	public static final String ADD_FRIEND = "add_friend";

	public static final String USER_TL_OPEN = "user_TL_open";

    public static final String MIGRATION_FAILURE_ANALYTICS = "migration_failure";
	public static final String PLATFORM_RICH_NOTIFS = "platformRichNotifs";
	public static final String BITMAP_DOWNLOAD_SUCCESS = "success";
	public static final String BITMAP_DOWNLOAD_UNSUCESSFULL = "fail";
	public static final String AUTO_DOWNLOAD_OFF = "autoDownloadOff";
	public static final String REQUEST_FAILURE = "requestFailure";

	public static final String TIME_LINE_OPEN = "TL_open";

	public static final String PACK_PREVIEW = "pckPrvw";

	public static final String VIEW_ALL = "viewAll";

	public static final String STICKER_DOWNLOAD_TIME = "stdnt";

	public static final String BLOCK_LIST_BACK_PRESS = "blckPriv";

	public static final String BLOCK_LIST_BACK = "bckBlck";

}
