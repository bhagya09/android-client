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
	String LAYOUT = "layoutId";
	String LONG_PRESS_DISABLED = "lpd";
	String HIKE_MSISDN = "hikemsisdn";
	String PLATFORM_USER_ID = "platformUid";
	String PLATFORM_TOKEN = "platformToken";
	String PLATFORM_UID_FOR_ADDRESS_BOOK_FETCH = "platformUidForAddressBookFetch";
	public int numberOfRetries = 3;  // number of retries
	public int retryDelay = 1000; // 1 sec
	public float backOffMultiplier = 2.0f; // exponential time delay multiplier
	public int HTTP_CALL_MADE = 2;
	public int MAKE_HTTP_CALL = 1;
	public static final String PLATFORM_UID_FETCH_TAG = "platformUID";
	public static final String CONTENT_ID = "content_id";
	public static final String NAMESPACE = "namespace";

	public static final class PlatformUIDFetchType
	{
		public static final int SELF = 1;

		public static final int FULL_ADDRESS_BOOK = 2;

		public static final int PARTIAL_ADDRESS_BOOK = 3;

	}

    /**
     * Analytics for Platform
     */
	public static final String BOT_OPEN = "bot_open";
    public static final String BOT_LONG_PRESS = "bot_lp";
    public static final String BOT_VIEW_PROFILE = "bot_vp";
    public static final String BOT_ADD_SHORTCUT = "bot_as";
    public static final String BOT_DELETE_CHAT = "bot_del";
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

	public static final String PLATFORM_BRIDGE_NAME = "PlatformBridge";

	public static final String CARD_DATA = "ld";

	public static final String FILE_ID = "file_id";

	public static final String NOTIFICATION = "notification";

	public static final String NOTIFICATION_SOUND = "notification_sound";

	public static final String INCREASE_UNREAD = "inc_unread";

	public static final String MESSAGE_ID = "message_id";

	public static final String CONV_MSISDN = "conv_msisdn";
	
	String ALARM_DATA = "alarm_data";
	
	String DELETE_CARD = "delete_card";

	public static final String SILENT_PUSH = "silent";

	public static final String LOUD_PUSH = "loud";

	public static final String  NO_PUSH = "none";
	
	public static final String PLATFORM_JS_VERSION = "platform_js";

	public static final String IS_SENT = "isSent";

	public static final String INFLATION_TIME = "inflationTime";

	public static final String TEMPLATING_TIME = "templatingTime";

	public static final String RENDERING_TIME = "renderingTime";

	public static final String PROFILING_TIME = "time";

	public static final String CONFIG_DATA = "cd";

	public static final String ENABLE_BOT = "enable_bot";

	public static final String BOT_TYPE = "bot_type";

	/**
	 * Overflow menu item constants :
	 */
	public static final String TITLE = "title";
	
	public static final String ID = "id";
	
	public static final String ENABLED = "en";
	
	/**
	 * Overflow menu item constants end
	 */
	
	
	public static final int VERSION_1 = 1;
	
	public static final int VERSION_2 = 2;
	
	public static final int CURRENT_VERSION = VERSION_2;
}
