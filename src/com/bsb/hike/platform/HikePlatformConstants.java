package com.bsb.hike.platform;

public interface HikePlatformConstants
{
	String DATA = "d";
	String METADATA = "md";
	String ASSETS = "assets";
	String TEXTS = "textLinks";
	String IMAGES = "images";
	String VIDEOS = "videos";
	String AUDIO = "audios";
	String ACTIONS = "actions";
	String INTENT_URI = "intentUri";
	String CONTENT_TYPE = "contentType";
	String VERSION = "version";
	String TAG = "platform";
	String KEY = "key";
	String SUBTYPE = "st";
	String TEXT = "title";
	String THUMBNAIL = "thumbnail";
	String URL = "url";
	String MEDIA_SIZE = "size";
	String DURATION = "duration";
	String LAYOUT_ID = "layoutID";
	String NOTIF_TEXT = "summary";
	String LOVE_ID = "loveID";
	String RECEPIENT = "to";
	String MESSAGE_TYPE = "t";
	String ANDROID_INTENT = "android";
	String CHANNEL_SOURCE = "channelSource";
	String CLICK_TRACK_URL = "clickTrackURL";
	public static final String GAME_SDK_ID = "gamesdk";
	String MESSAGE = "message";
	String SOURCE = "clientPkgName";
	String HELPER_DATA = "hd";
	String HEIGHT = "h";
	String CARD_OBJECT = "cardObj";
	String NOTIF_TEXT_WC = "notifText";
	String WC_PUSH_KEY = "push";
	String CRICKET_CHAT_THEME_ID = "39";
	String APP_NAME = "appName";
	String APP_PACKAGE = "appPackage";
	String LAYOUT = "layoutId";
	String LONG_PRESS_DISABLED = "lpd";
	String HIKE_MSISDN = "hikemsisdn";
	String PLATFORM_USER_ID = "platformUid";
	String PLATFORM_TOKEN = "platformToken";
	String PLATFORM_UID_FOR_ADDRESS_BOOK_FETCH = "platformUidForAddressBookFetch";
	public int NUMBER_OF_RETRIES = 3;  // number of retries
	public int RETRY_DELAY = 1000; // 1 sec
	public float BACK_OFF_MULTIPLIER = 2.0f; // exponential time delay multiplier
	public int HTTP_CALL_MADE = 2;
	public int MAKE_HTTP_CALL = 1;
	public static final String FETCH_TAG = "platformFetch";
	public static final String CONTENT_ID = "content_id";
	public static final String NAMESPACE = "nameSpace";
	public static final String PARAMS = "params";
	public static final String STATUS = "status";
	public static final String STATUS_CODE = "status_code";
	public static final String RESPONSE = "response";
	public static final String ERROR_MESSAGE = "error_message";
	public static final String SUCCESS = "success";
	public static final String FAILURE = "failure";
	public static final String NON_MESSAGING_BOT_TYPE = "nm_type";
	public static final String MICROAPP_MODE = "nm_app";
	public static final String URL_MODE = "url_app";
	public static final String NATIVE_MODE = "native_mode";
	public static final String CUSTOMER_SUPPORT_BOT_MSISDN = "+hikecs+";
	public static final String MESSAGE_HASH = "h";
	public static final String EVENT_ID = "eventId";
	public static final String EVENT_STATUS = "eventStatus";
	public static final String EVENT_DATA = "d";
	public static final String EVENT_TYPE = "et";
	public static final String SHARED_EVENT = "se";
	public static final String NORMAL_EVENT = "e";
	public static final String RECIPIENT_NAMESPACES = "recipients";
	public static final String MAPPED_EVENT_ID = "i";
	public static final String EVENT_CARDDATA = "cd";
	public static final String CLIENT_TIMESTAMP = "c";
	public static final String MICROAPP_DATA = "mmData";
	public static final String PARENT_MSISDN = "parent_msisdn";
	public static final String SPECIAL = "spl";
	public static final String REQUEST_CODE = "request_code";
	public static final String REPLACE_MICROAPP_VERSION = "replace";
	public static final String BOT_VERSION = "version";
	public static final String CALLBACK_ID = "callback_id";
	public static final String PLATFORM_UIDS = "platformUids";
	public static final String ANONYMOUS_NAME = "anonymousName";
	public static final String ERROR = "error";
	public static final String ANONYMOUS_NAMES = "anonymousNames";
	public static final String FORWARD_CARD_OBJECT = "fwdCardObj";
	public static final String TIMESTAMP = "timestamp";
	public static final String ASSOCIATE_CBOT = "assocCbot";
	public static final String GAME_ACTIVE ="gameActive" ;
    public static final String IS_SDK ="isSdk" ;
    public static final String INCOMPLETE_ZIP_DOWNLOAD = "incomplete_zip_download";
	String HIKE_AFFINITY="hike_affinity";
	String CLUB_BY_MSISDN="clubbymsisdn";
	public static final String CUSTOM_TABS="customTabs";
	String PACKET_DATA = "packetData";
	String PREF_NETWORK = "preferredNetwork";

    public static final class PlatformFetchType
	{
		public static final int SELF = 1;

		public static final int FULL_ADDRESS_BOOK = 2;

		public static final int PARTIAL_ADDRESS_BOOK = 3;

		public static final int SELF_ANONYMOUS_NAME = 4;

		public static final int OTHER_ANONYMOUS_NAME = 5;

	}

	public static class EventType
	{
		public static final int SHARED_EVENT = 0;
		public static final int NORMAL_EVENT = 1;
	}

	public static class EventStatus
	{
		public static final int EVENT_SENT = 0;
		public static final int EVENT_RECEIVED = 1;
	}

    /**
     * Analytics for Platform
     */
	public static final String BOT_OPEN = "bot_open";
	public static final String BOT_OPEN_MQTT = "bot_open_m";
    public static final String BOT_LONG_PRESS = "bot_lp";
    public static final String BOT_VIEW_PROFILE = "bot_vp";
    public static final String BOT_ADD_SHORTCUT = "bot_as";
    public static final String BOT_DELETE_CHAT = "bot_del";
    public static final String BOT_DELETE_BLOCK_CHAT = "bot_del_block";
    public static final String BOT_CLEAR_CONVERSATION = "bot_clc";
    public static final String BOT_EMAIL_CONVERSATION = "bot_emc";
    public static final String CONVERSATION_FRAGMENT = "cf";
    public static final String OVERFLOW_MENU = "om";
    public static final String BOT_BLOCK_CHAT = "bot_blc";
    public static final String BOT_UNBLOCK_CHAT = "bot_ublc";
    public static final String BOT_MUTE_CHAT = "bot_muc";
    public static final String BOT_UNMUTE_CHAT = "bot_umuc";
    public static final String BOT_CHAT_THEME_PICKER = "bot_cht";
    public static final String CARD_DELETE = "card_del";
    public static final String ACTION_BAR = "ab";
	public static final String CARD_LOADED = "card_load";
	public static final String CARD_STATE = "state";
	public static final String ERROR_CODE = "err_code";
	public static final String BOT_ERROR = "bot_err";
	public static final String CARD_TYPE = "card_type";
	public static final String CARD_FORWARD = "card_fwd";
	public static final String BLOCKED_MESSAGE = "blocked_msg";
	public static final String CARD = "card";
	public static final String NOTIF = "notif";
	public static final String APP_VERSION = "appVersion";
	public static final String OVERFLOW_MENU_CLICKED = "om_click";
	public static final String BOT_CREATED = "cbot";
	public static final String BOT_CREATED_MQTT = "cbot_m";
	public static final String BOT_CREATION_FAILED = "cbot_err";
	public static final String BOT_CREATION_FAILED_MQTT = "cbot_err_m";
	public static final String MICROAPP_DOWNLOADED = "mapp";
	public static final String MICROAPP_DOWNLOAD_FAILED = "mapp_err";
    public static final String NAME = "name";

	public static final String PLATFORM_BRIDGE_NAME = "PlatformBridge";

	public static final String LAYOUT_DATA = "ld";

	public static final String FILE_ID = "file_id";

	public static final String NOTIFICATION = "notification";

	public static final String NOTIFICATION_SOUND = "notification_sound";

	public static final String INCREASE_UNREAD = "increase_unread";

	public static final String REARRANGE_CHAT = "rearrange_chat";

	public static final String MESSAGE_ID = "message_id";

	public static final String CONV_MSISDN = "conv_msisdn";
	
	String ALARM_DATA = "alarm_data";
	
	String DELETE_CARD = "delete_card";
	String ORIENTATION = "orientation";

	public static final String SILENT_PUSH = "silent";

	public static final String LOUD_PUSH = "loud";
	
	public static final String FILE_DESCRIPTOR = "file:///";

	public static final String  NO_PUSH = "none";
	
	public static final String TARGET_PLATFORM = "target_platform";
	
	public static final String TARGET_ACTIVITY = "target_activity";

    public static final String MAPP_VERSION_CODE = "mAppVersionCode";
	
	public static final String MIN_PLATFORM = "min_platform";

	public static final String IS_SENT = "isSent";

	public static final String INFLATION_TIME = "inflationTime";

	public static final String TEMPLATING_TIME = "templatingTime";

	public static final String RENDERING_TIME = "renderingTime";

	public static final String PROFILING_TIME = "time";

	public static final String CONFIG_DATA = "cd";

	public static final String ENABLE_BOT = "enable_bot";

	public static final String BOT_TYPE = "bot_type";

	public static final String NOTIF_DATA = "notifData";

	/**
	 * Overflow menu item constants :
	 */
	public static final String TITLE = "title";
	
	public static final String ID = "id";
	
	public static final String ENABLED = "en";
	
	public static final String IS_CHECKED = "is_checked";
	
	/**
	 * Overflow menu item constants end
	 */
	
	public static final int VERSION_NANO = 0;
	
	public static final int VERSION_ALTO = 1;

	public static final int CURRENT_VERSION = 10;

	public static final String AB_COLOR = "color";
	
	final String BLOCK = "block";
	
	final String MUTE = "mute";
	
	final String NETWORK_TYPE = "networkType";
	String PLATFORM_VERSION = "platform_version";
	
	final String HIKE_MESSAGE = "hm";
	
	final String JS_INJECT = "js_inject";
	
	final String FULL_SCREEN_CONFIG = "full_screen_config";
	
	final String SECONDARY_TITLE = "secondary_title";
	
	final String STATUS_BAR_COLOR = "sb_color";
	String RESUME_SUPPORTED = "resume_supported";
	final String ASSOCIATE_MAPP = "associate_mapp";
	public static final String PLATFORM_USER_ID_SYNC = "plfsync";
	
	public static final String BOT_DESCRIPTION = "desc";
	
	public static final String BOT_LATEST_VERSION = "latest_version";
	
	public static final String MSISDN = "msisdn";

    public static final String BOT_MSISDN = "botMsisdn";
	
	public static final String BOT_NAME = "name";
	
	public static final String BOTS = "bots";
	
	public static final String ALL_REQUIRED = "all_required";
	
	public static final String BOT_DP = "dp";
	
	public static final String GAME_PROCESS="org.cocos2dx.gameprocess";
	
	public static final String LAST_GAME="lastGame";
	
	public static final String GAME_CHANNEL="+hikegames+";
	
	final static String RESUME_SUPPORT = "resume_support";

    final class PlatformBotType
    {
        public static final byte WEB_MICRO_APPS = 1;

        public static final byte ONE_TIME_POPUPS = 2;

        public static final byte NATIVE_APPS = 3;

        public static final byte HIKE_MAPPS = 4;

    }

	public static final String IS_SHORTCUT = "is_shortcut";

	public static final String TYPE = "type";

	public static class UrlInterceptTypes
	{
		public static final int INTERCEPT_AND_CLOSE_WEBVIEW = 1;
	}

	public static final String EXTRA_DATA = "extra_data";

	public static final String RECURRING_LOCATION = "rec_loc";

	public static final String RECURRING_LOCATION_END_TIME = "loc_end_time";

	public static final String TIME_INTERVAL = "interval";

	public static final String TEAM_HIKE_MSISDN = "+hike+";
	
	public static final String EMMA_BOT_MSISDN = "+hike1+";
	
	public static final String GAMES_HIKE_MSISDN = "+hike2+";
	
	public static final String HIKE_DAILY_MSISDN = "+hike3+";
	
	public static final String HIKE_SUPPORT_MSISDN = "+hike4+";
	
	public static final String NATASHA_MSISDN = "+hike5+";
	
	public static final String CRICKET_HIKE_MSISDN = "+hikecricket+";

    public static final String PLATFORM_SDK_PATH = "platformSdkPath";

    public static final String PLATFORM_WEB_SDK = "platformSdk";

	public static final int MAPP_DEFAULT_RETRY_COUNT = 2;

    public static final String APPS = "apps";

    int CHROME_TABS_PENDING_INTENT_SHARE = -299;
	int CHROME_TABS_PENDING_INTENT_FORWARD = -300;

    String MICRO_APP_NEWS_STORAGE_NAME = "hikenews";

    String MICRO_APP_CRICKET_STORAGE_NAME = "hikecricketnew";

    String MICRO_APP_NEWS_REGEX = "news";

    String MICRO_APP_CRICKET_REGEX = "cricket";

	String TTL = "timeToLive";

	String DEFULT_NETWORK = "unknown";

	String AUTO_RESUME = "autoResume";

	public static class PlatformDwnldState
	{
		public final static int IN_PROGRESS = 0;
		public final static int FAILED = 1;
		public final static int SUCCESS = 2;
	}

	public static class PlatformTypes
	{
		public final static int CBOT = 0;
		public final static int MAPP = 1;
	}

	long oneDayInMS = 86400000;
	String BIG_PICTURE = "big_picture";

	String BITMAP_URL = "bitmap_url";
}
