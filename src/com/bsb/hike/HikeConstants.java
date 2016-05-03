package com.bsb.hike;

import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Environment;

import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;

public class HikeConstants
{
	public static final String APP_PUSH_ID = "297726274089";

	public static final String APP_TEST_UID = "9274563810";

	public static final String MAP_API_KEY_DEBUG = "0Luu6V6IYSC0UpLSUZe7oO-bvd392OgrXSnY8aA";

	public static final String MAP_API_KEY_PROD = "0Luu6V6IYSC3olBhRm5jHLKDIn5_CA3P17l_3Mw";

	public static final String MA_TRACKER_AD_ID = "9198";

	public static final String MA_TRACKER_KEY = "00774f47f97b5173432b958a986f0061";

	public static final String MA_TRACKER_USERID = "bsb_hike";

	public static final String MA_TRACKER_REF_ID_PREFIX = "bsb_hike_";

	public static final String MA_SITE_ID = "27576";

	public static final String ANDROID = "android";

	public static final String MESSAGE = "msg";

	public static final String CUSTOM_MESSAGE = "cmsg";

	public static final String TOAST = "tst";
	
	public static final String UI_TOPIC = "/u";

	public static final String APP_TOPIC = "/a";

	public static final String SERVICE_TOPIC = "/s";

	public static final String PUBLISH_TOPIC = "/p";

	public static final String TYPE = "t";

	public static final String DATA = "d";

	public static final String TO = "to";

	public static final String FROM = "f";

	public static final String SUB_TYPE = "st";

	public static final String BACKGROUND_COLOR = "bgc";
	
	public static final String SU_ID = "su_id";

	public static final String SU_ID_LIST = "su_ids";

	public static final String GROUP_CHAT_TIMESTAMP = "gts";

	public static final String GROUP_CREATOR = "c";

	public static final String HIKE_MESSAGE = "hm";

	public static final String SMS_MESSAGE = "sm";

	public static final String PIN_MESSAGE = "pin";
	
	public static final String CAPTION = "cptn";

	public static final String TIMESTAMP = "ts";

	public static final String MESSAGE_ID = "i";

	public static final String EVENT_ID = "ei";

	public static final String METADATA = "md";

	public static final String SPECIES = "s";

	public static final String VARIETY = "v";

	public static final String METADATA_DND = "dnd";

	public static final String ANALYTICS_EVENT = "ae";

	public static final String ALL_INVITEE = "ai";

	public static final String ALL_INVITEE_JOINED = "aij";

	public static final String ALL_INVITEE_2 = "all_invitee";

	public static final String ALL_INVITEE_JOINED_2 = "all_invitee_joined";

	public static final String NAME = "name";

	public static final String DOB = "dob";

	public static final String YEAR = "year";

	public static final String DAY = "day";

	public static final String MONTH = "month";

	public static final String MALE = "m";

	public static final String FEMALE = "f";

	public static final String MSISDN = "msisdn";

	public static final String ADMIN = "admin";

	public static final String ADMIN_MSISDN = "admin_msisdn";

	public static final String GROUP_SETTING = "gs";

	public static final String GROUP_TYPE = "gt";

	public static final String SETTING = "setting";

	public static final String BOT_THUMBNAIL = "dp";

	public static final String CONFIGURATION = "config";

	public static final String BOT_CHAT_THEME = "bg_id";

	public static final String IS_RECEIVE_ENABLED_IN_BOT = "rec_enable";

	public static final String NEW_USER = "nu";

	public static final String EMAIL = "email";
	
	public static final String PRODUCT = "product";
	
	public static final String CLIENT_URL = "clienturl";
	
	public static final String CLIENT_IMAGE_URL = "imageurl";
	
	public static final String CLIENT_TYPE = "client_type";
	
	public static final String PACKAGE_NAME = "package_name";
	
	public static final String SHA1 = "sha1";

	public static final String GENDER = "gender";

	public static final String VERSION = "v";

	public static final String EXPIRE_AT = "expires_at";

	public static final String PUSHACK = "pushack";

	public static final String CRITICAL = "c";

	public static final String CRITICAL_UPDATE_KEY = "critical";

	public static final String INVITE_TOKEN = "invite_token";

	public static final String TOTAL_CREDITS_PER_MONTH = "tc";

	public static final String DND_NUMBERS = "dndnumbers";

	public static final String SOURCE = "source";

	public static final String FILES = "files";

	public static final String CONTENT_TYPE = "ct";

	public static final String ATTACHEMENT_SHARED_FROM = "atsrc";

	public static final String THUMBNAIL = "tn";

	public static final String SOURCE_FILE_PATH = "srcPath";

	public static final String FILE_NAME = "fn";

	public static final String FILE_KEY = "fk";

	public static final String FILE_PATH = "fp";

	public static final String DOWNLOAD_FILE_URL_KEY = "url";

	public static final String CREDITS = "credits";

	public static final String ON_HIKE = "onhike";

	public static final String ROLE = "role";

	public static final String IS_BROADCAST = "isBroadcast";

	public static final String DND = "dnd";

	public static final String DND_USERS = "dndUsers";

	public static final String LATITUDE = "lat";

	public static final String LONGITUDE = "long";

	public static final String LOCATION_PROIVDER = "provider";

	public static final String ZOOM_LEVEL = "zoom";

	public static final String ADDRESS = "add";

	public static final String POKE = "poke";

	public static final String IS_GHOST = "is_ghost";

	public static final String ID = "id";

	public static final String TOKEN = "token";

	public static final String EXPIRES = "expires";

	public static final String POST = "post";

	public static final String ACCOUNT = "account";

	public static final String ACCOUNTS = "accounts";

	public static final String FAVORITES = "favorites";

	public static final String PENDING = "pending";

	public static final String MSISDNS = "msisdns";

	public static final String REWARDS_TOKEN = "reward_token";

	public static final String MQTT_IP_ADDRESSES = "ip";

	public static final String SHOW_REWARDS = "show_rewards";

	public static final String REWARDS = "rewards";

	public static final String GAMES_TOKEN = "games_token";

	public static final String SHOW_GAMES = "show_games";

	public static final String GAMES = "games";

	public static final String TALK_TIME = "tt";

	public static final String PHONE_NUMBERS = "phone_numbers";

	public static final String EMAILS = "emails";

	public static final String ADDRESSES = "addresses";

	public static final String EVENTS = "events";

	public static final String STATUS_ID = "statusid";

	public static final String MOOD = "mood";

	public static final String STATUS_MESSAGE = "msg";

	public static final String PROFILE = "profile";

	public static final String ICON = "icon";

	public static final String BOT = "bot";

	public static final String MESSAGING_BOT = "m_bot";

	public static final String NON_MESSAGING_BOT = "nm_bot";

	public static final String GROUP_CONVERSATION = "gc";

	public static final String ONE_TO_ONE_CONVERSATION = "oc";

	public static final String MUTED = "muted";

	public static final String GROUPS = "groups";

	public static final String POST_AB = "postab";

	public static final String PATCH_AB = "patchab";

	public static final String POST_INFO = "postinfo";

	public static final String PUSH = "push";

	public static final String GCM_STALE_REGISTRATION_REFRESH = "gcmSRR";

	public static final String JOIN_TIME = "jointime";

	public static final String STATUS_MESSAGE_2 = "status-message";

	public static final String TIME_OF_DAY = "timeofday";

	public static final String REQUEST_PENDING = "requestpending";

	public static final String DEVICE_LOCALE = "device_locale";

	public static final String LOCALE = "locale";
	
	public static final String ENABLE_PUSH_BATCHING_STATUS_NOTIFICATIONS = "enablepushbatchingforsu";

	public static final String PUSH_SU = "pushsu";

	public static final String BATCH_HEADER = "h";

	public static final String BATCH_MESSAGE = "m";

	public static final String UPGRADE = "upgrade";

	public static final String DEV_TYPE = "dev_type";

	public static final String APP_VERSION = "app_version";

	public static final String UPDATE_VERSION = "version";

	public static final String DEVICE_VERSION = "deviceversion";

	public static final String CRICKET_MOODS = "cmoods";

	public static final String COUNT = "c";

	public static final String UPDATED_SIZE = "s";

	public static final String PALLETE_POSITION = "idx";

	public static final String DEFAULT_SMS_CLIENT_TUTORIAL = "dsctutorial";

	public static final String STICKER_IDS = "stIds";

	public static final String STICKER = "stk";
	
	public static final String STICKER_CATEGORY_ID = "stickerCategoryId";

	public static final String RESOLUTION_ID = "resId";

	public static final String NUMBER_OF_STICKERS = "nos";

	public static final String DOWNLOAD_SOURCE = "dsrc";

	public static final String TOTAL_STICKERS = "totalStickers";

	public static final String DATA_2 = "data";

	public static final String ADD_STICKER = "addStk";

	public static final String REMOVE_STICKER = "remStk";

	public static final String REMOVE_CATEGORY = "remCat";

	public static final String DISABLED_ST = "disabled";

	public static final String STATUS = "stat";

	public static final String OK = "ok";

	public static final String FAIL = "fail";

	public static final String OK_HTTP = "okhttp";

	public static final String REACHED_STICKER_END = "st";

	public static final String PLAYTIME = "pt";

	public static final String FOREGROUND = "fg";

	public static final String BACKGROUND = "bg";

	public static final String BULK_LAST_SEEN = "bulklastseen";

	public static final String LAST_SEEN = "ls";

	public static final String LAST_SEEN_SETTING = "lastseen";

	public static final String NEW_LAST_SEEN_SETTING = "nls";

	public static final String UJ_NOTIF_SETTING = "ujn";

	public static final String BULK_LAST_SEEN_KEY = "lastseens";

	public static final String PROTIP_HEADER = "h";

	public static final String PROTIP_TEXT = "t";

	public static final String PROTIP_IMAGE_URL = "img";

	public static final String SU_IMAGE_KEY = "img";

	public static final String PROTIP_WAIT_TIME = "wt";

	public static final String PROTIP_GAME_DOWNLOAD_URL = "url";

	public static final String PROTIP_SHOW_PUSH = "fp";

	public static final String NO_SMS = "nosms";

	public static final String RETURNING_USER = "ru";

	public static final String NUMBER_OF_SMS = "no_of_sms";

	public static final String OFFLINE = "offline";

	public static final String SENDER = "sender";

	public static final String RECIPIENT = "recipient";

	public static final String IS_H2H = "is_h2h";

	public static final String NATIVE_SMS = "native_sms";

	public static final String UNIFIED_INBOX = "unified_inbox";

	public static final String PULL_OLD_SMS = "pull_old_sms";

	public static final String TIP_ID = "tip_id";

	public static final String TIP_URL = "tip_url";

	public static final String SMS = "sms";

	public static final String UI_EVENT = "uiEvent";

	public static final String CRC_EVENT = "crc";

	public static final String MD5_HASH = "md5";

	public static final String FILE_SIZE = "fs";

	public static final String DOWNLOAD = "dwnld";

	public static final String CONNECTION_TYPE = "cnnctn";

	public static final String DOWNLOAD_TIME = "dwnTm";

	public static final String APK_SIZE_MULTIPLIER = "sm";

	public static final String INSTALL_PROMPT_FREQUENCY = "ipf";

	public static final String INSTALL_PROMPT_METHOD = "ipm";

	public static final String INSTALL_PROMPT_INTERVAL = "ipi";

	public static final String LOGEVENT_TAG = "mob";

	public static final String FREE_SMS_ON = "free_sms_on";

	public static final String NATIVE_INVITES = "nativeinvites";

	public static final String PREF = "pref";

	public static final String CONTACTS = "contacts";

	public static final String SENDBOT = "sendbot";

	public static final String EVENT_TYPE = "et";

	public static final String EVENT_KEY = "ek";
	
	public static final String EVENT_PATH = "src";
	
	public static final String EVENT_CHECKED = "check";
	
	public static final String EVENT_CONFIRM = "cnfrm";

	public static final String PACKAGE = "pkg";

	public static final String POPUP_SUBTYPE = "pst";

	public static final String SIGNUP_IC = "signupIc";

	public static final String DEVICE_KEY = "device_key";

	public static final String ENABLE_FREE_INVITES = "enable_free_invites";

	public static final String SHOW_FREE_INVITES = "show_free_invites";

	public static final String FREE_INVITE_POPUP_TITLE = "free_invite_popup_title";

	public static final String FREE_INVITE_POPUP_TEXT = "free_invite_popup_text";

	public static final String LIST = "list";

	public static final String BG_ID = "bg_id";

	public static final String CHAT_BACKGROUNDS = "cbgs";

	public static final String CHAT_BACKGROUND = "cbg";

	public static final String CHAT_BACKGROUD_NOTIFICATION = "pushcbg";

	public static final String RESOLUTION = "res";

	public static final String OPERATOR = "op";

	public static final String CIRCLE = "circle";

	public static final String PIXEL_DENSITY_MULTIPLIER = "pdm";

	public static final String CUSTOM = "custom";

	public static final String SOUND_PREF = "soundPref";

	public static final String VIBRATE_PREF = "vibratePref";

	public static final String HIKE_JINGLE_PREF = "jinglePref";

	public static final String NOTIF_SOUND_PREF = "notifSoundPref";

	public static final String VIBRATE_PREF_LIST = "vibratePrefList";

	public static final String TICK_SOUND_PREF = "tickSoundPref";

	public static final String FREE_SMS_PREF = "freeSmsPref";

	public static final String LED_PREF = "ledPref";

	public static final String COLOR_LED_PREF = "colorLedPref";

	public static final String NATIVE_JINGLE_PREF = "jinglePref";

	public static final String SSL_PREF = "sslPref";

	public static final String STATUS_PREF = "statusPref";

	public static final String SEND_SMS_PREF = "sendSmsPref";

	public static final String RECEIVE_SMS_PREF = "receiveSmsPref";

	public static final String SEND_UNDELIVERED_AS_NATIVE_SMS_PREF = "sendUndeliveredAsNativeSmsPref";

	public static final String LAST_SEEN_PREF = "lastSeenPref";

	public static final String LAST_SEEN_PREF_LIST = "lastSeenPrefList";

	public static final String LONG_PRESS_DUR_PREF = "longPressDurationPref";
	
	public static final String KEYPRESS_VOL_PREF = "keyPressVolPref";
	
	public static final String KEYPRESS_VIB_DUR_PREF = "keypressVibDurationPref";
	
	public static final String PROFILE_PIC_PREF = "profilePicPref";

	public static final String SEND_ENTER_PREF = "enterSendPref";

	public static final String DOUBLE_TAP_PREF = "doubleTapPref";

	public static final String STICKER_REORDER_PREF = "stickerReOrderPref";

	public static final String STICKER_DELETE_PREF = "stickerDeletePref";

	public static final String STICKER_HIDE_PREF = "stickerHidePref";

	public static final String STICKER_UPDATE_PREF = "stickerUpdatePref";

	public static final String STICKER_RECOMMEND_PREF = "stickerRecommendPref";

	public static final String STICKER_RECOMMEND_SETTING_OFF_TOAST = "srsofft";

	public static final String STICKER_AUTO_RECOMMEND_SETTING_OFF_TIP = "sarsofft";

	public static final String STICKER_RECOMMEND_AUTOPOPUP_PREF = "stickerRecommendAutopopupPref";

	public static final String CHAT_BG_NOTIFICATION_PREF = "chatBgNotificationPref";

	public static final String RESET_STEALTH_PREF = "resetStealthPref";

	public static final String CHANGE_STEALTH_PASSCODE = "changeStealthPasscode";

	public static final String CHANGE_STEALTH_TIMEOUT = "changeStealthTimeout";

	public static final String STEALTH_INDICATOR_ENABLED = "stealthIndicatorEnabled";
	
	public static final String STEALTH_INDICATOR_SHOW_REPEATED = "stealthIndicatorShowRepeated";

	public static final String STEALTH_INDICATOR_ANIM_ON_RESUME = "stealthIndicatorAnimOnResume";

	public static final int STEALTH_INDICATOR_RESUME_EXPIRED = -1;

	public static final int STEALTH_INDICATOR_RESUME_ACTIVE = 1;

	public static final int STEALTH_INDICATOR_RESUME_RESET = 0;
	
	public static final String STEALTH_INDICATOR_SHOW_ONCE = "stealthIndicatorShowOnce";

	public static final String STEALTH_NOTIFICATION_ENABLED = "stealthNotificationEnabled";

	public static final String STEALTH_PERF_SETTING = "steathPerfCategory";

	public static final String STEALTH_MSISDN = "stlthmsisdn";

	public static final String SEND_UNDELIVERED_AS_NATIVE_PREF = "sendUndeliveredAsNativePref";

	public static final String SEND_UNDELIVERED_ALWAYS_AS_SMS_PREF = "sendUndeliveredAlwaysAsSmsPref";

	public static final String REQUEST_DP = "rdp";

	public static final String C_TIME_STAMP = "cts";

	public static final String HEADER = "h";

	public static final String BODY = "b";

	public static final String ANIMATE = "anm";

	public static final String MESSAGES = "msgs";

	public static final String FILE_IMAGE_QUALITY = "img_quality";

	public static final String CAN_EDIT_DP = "canEditDP";

	public static final String SERVICE = "srv";

	public static final String CREATE = "crt";

	public static final String STICKEY_SERVICE = "stky";

	public static final String HIKE_SERVICE = "hike";

	public static final String HTTP_STATUS_ID = "?status_id=";

	public static final String PACK_VISIBILITY = "packVisibility";

	public static final String OLD_PACK_POSITION = "oldPackPosition";

	public static final String NEW_PACK_POSITION = "newPackPosition";

	// @GM
	// public static final String AUTO_DOWNLOAD_IMAGE_PREF =
	// "autoDownloadImagePref"
	public static final String AUTO_DOWNLOAD_MEDIA_PREF = "AutoDownloadMediaPref";

	public static final String MD_AUTO_DOWNLOAD_IMAGE_PREF = "mdAutoDownloadImagePref";

	public static final String MD_AUTO_DOWNLOAD_AUDIO_PREF = "mdAutoDownloadAudioPref";

	public static final String MD_AUTO_DOWNLOAD_VIDEO_PREF = "mdAutoDownloadVideoPref";

	public static final String WF_AUTO_DOWNLOAD_IMAGE_PREF = "wfAutoDownloadImagePref";

	public static final String WF_AUTO_DOWNLOAD_AUDIO_PREF = "wfAutoDownloadAudioPref";

	public static final String WF_AUTO_DOWNLOAD_VIDEO_PREF = "wfAutoDownloadVideoPref";

	public static final String COMPRESS_VIDEO = "videoCompress";

	public static final String COMPRESS_VIDEO_CATEGORY = "videoCompressCategory";

	public static final String IMAGE_QUALITY = "imageQuality";

	public static final String REMEMBER_IMAGE_CHOICE = "rememberImageChoice";

	public static final String SHOW_IMAGE_QUALITY_TIP = "showImageQualityTip";

	public static final String HIKEBOT = "TD-HIKE";

	public static final String HIKEBOT_CONV_STATE = "isHikeBotConvState";

	public static final String BACKUP_PREF = "backupAccount";

	public static final String NUX = "nux";

	public static final String STICKER_NUX = "stkNux";

	public static final String UNLINK_PREF = "unlinkAccount";

	public static final String DELETE_PREF = "deleteAccount";

	public static final String BLOKED_LIST_PREF = "blockedList";

	public static final String FAV_LIST_PREF = "favoriteList";

	public static final String STEALTH_MODE_PREF = "stealthModeSettings";

	public static final String STEALTH_PREF_SCREEN = "stealthPrefScreen";

	public static final String SYSTEM_HEALTH_PREF = "systemHealth";

	public static final String HELP_FAQS_PREF = "helpFaqs";

	public static final String HELP_FEEDBACK_PREF = "helpFeedback";

	public static final String HELP_TNC_PREF = "helpTnc";

	public static final String STATUS_BOOLEAN_PREF = "statusBooleanPref";

	public static final String NUJ_NOTIF_BOOLEAN_PREF = "hikeNUJNotificationPref";

	public static final String H2O_NOTIF_BOOLEAN_PREF = "hikeOfflineNotificationPref";

	public static final String LOCAL_LANGUAGE_PREF = "appLanguagePref";

	public static final String LOCALIZATION_FTUE_COMPLETE = "localizationFtueComplete";

	public static final String LANGUAGE_DOWNLOAD_ERROR_CODE = "er_c";

	public static final String UPGRADE_AVATAR_PROGRESS_USER = "upgradeAvtarProgressUser";

	public static final String UPGRADING = "upgrading";

    public static final String HIKE_CONTENT_MICROAPPS_MIGRATION = "hikeMicroAppsMigration";

	public static final String UPGRADE_MSG_HASH_GROUP_READBY = "upgradeMsgHashGroupReadby";

	public static final String UPGRADE_FOR_DATABASE_VERSION_28 = "upgradeForDatabaseVersion28";

	public static final String UPGRADE_AVATAR_CONV_DB = "upgradeAvtarProgressConv";

	public static final String NO_CALL_ALERT_CHECKED = "noCallAlertChecked";

	public static final String CRITICAL_STRING = "critical";

	public static final String OPERATOR_SMS_ALERT_CHECKED = "opSmsAlertChecked";

	public static final String SINGLE_INVITE_SMS_ALERT_CHECKED = "singleSmsAlertChecked";

	public static final String FTUE_ADD_SMS_ALERT_CHECKED = "ftueSmsAlertChecked";

	public static final String IS_GAMES_ITEM_CLICKED = "isGamesItemClicked";

	public static final String IS_REWARDS_ITEM_CLICKED = "isRewardsItemClicked";

	public static final String IS_HOME_OVERFLOW_CLICKED = "isHomeOverflowClicked";

	public static final String SHOW_RECENTLY_JOINED_DOT = "showRecentlyJoinedDot";

	public static final String SHOW_RECENTLY_JOINED = "showRecentlyJoined";

	public static final String SHOW_TIMELINE_RED_DOT = "showTimelineRedDot";

	public static final String IS_OF_ICON_CLICKED = "isOfIconClicked";

	public static final String DONE = "Done";

	public static final String PIN_ERROR = "PinError";

	public static final String ADDRESS_BOOK_ERROR = "AddressBookError";

	public static final String CHANGE_NUMBER = "ChangeNumber";

	public static final String SEPARATOR = " - ";

	public static final String GROUP_PARTICIPANT_SEPARATOR = ", ";

	public static final String HELP_URL = "http://www.hike.in/help/android";

	public static final String T_AND_C_URL = "http://www.hike.in/terms/android";

	public static final String SYSTEM_HEALTH_URL = "http://www.twitter.com/hikestatus";

	public static final String IS_TYPING = "is typing...";

	public static final String ARE_TYPING = "are typing...";

	public static final String NEW_GROUP = "new_group";

	public static final String NEW_BROADCAST = "new_broadcast";

	public static final String BROADCAST_ID_PREFIX = "b:";

	public static final String AVATAR = "avatar";
	public static final String LANGUAGE = "lan";

	public static final String NOTIFICATION_TITLE = "notifTitle";
	public static final String NOTIFICATION_TEXT = "notifText";

	public static final String TOGGLE = "toggle";

    public static final String HTTP_NETWORK_CHECK_CALL = "httpNetworkCheckCall";
	public static String ADD_URL = "addUrl";
	public static String DELETE_URL = "deleteUrl";


	public static final String CALLER_BLOKED_LIST_PREF = "callerBlockedList";

	public static final String CALL_TYPE = "call_type";

	public class Shortcut
	{
		public static final String UPDATE = "shtct";

		public static final String CREATE = "cr";

		public static final String DELETE = "dl";
	}

	public static final String CONTACT_UPDATE = "cntct_no";

	public static final String CONTACT_NAME = "cntct_nm";
	
	public static final String CONTACT_NUMBER_OLD = "cntct_no_old";

	public class InviteSection
	{
		public static final String INVITE_SECTION = "invite_section";

		public static final String SHOW_EXTRA_INVITE_SECTION = "show_invite_section";

		public static final String INVITE_SECTION_MAIN_TEXT = "invite_main_text";

		public static final String INVITE_SECTION_BOTTOM_TEXT = "invite_bottom_text";

		public static final String INVITE_SECTION_IMAGE = "invite_image";
	}

	/* Constant used to name the preference file which saves the drafts */
	public static final String DRAFT_SETTING = "draftSetting";

	public static final int CONNECT_TIMEOUT = 30 * 1000;

	public static final int SOCKET_TIMEOUT = 30 * 1000;

	/* how long to wait between sending publish and receiving an acknowledgement */
	public static final long MESSAGE_DELIVERY_TIMEOUT = 5 * 1000;

	/* how long to wait for a ping confirmation */
	public static final long PING_TIMEOUT = 5 * 1000;

	/*
	 * how long to wait to resend message. This should significantly greathr than PING_TIMEOUT
	 */
	public static final long MESSAGE_RETRY_INTERVAL = 15 * 1000;

	/* quiet period of no changes(in seconds) before actually updating the db */
	public static final long CONTACT_UPDATE_TIMEOUT = 5;

	/* how often to ping the server */
	public static final short KEEP_ALIVE = 5 * 60; /* 10 minutes */

	/* how often to ping after a failure */
	public static final int RECONNECT_TIME = 10; /* 10 seconds */

	/* how often to ping after a server unavailable failure */
	public static final int SERVER_UNAVAILABLE_MAX_CONNECT_TIME = 9; /* 9 minutes */

	/* the max amount (in seconds) the reconnect time can be */
	public static final int MAX_RECONNECT_TIME = 120;

	/* the max amount of time we allow the service to run in case of no activity */
	public static final int DISCONNECT_TIME = 10 * 60;

	/* the max amount of time we wait for the PIN */
	public static final int PIN_CAPTURE_TIME = 10 * 1000;

	public static final int NETWORK_ERROR_POP_UP_TIME = 120 * 1000;

	public static final int BACKUP_RESTORE_UI_DELAY = 3 * 1000;

	public static final int HIKE_SYSTEM_NOTIFICATION = 0;

	public static final String ADAPTER_NAME = "hikeadapter";

	/* constants for defining what to do after checking for updates */
	public static final int NORMAL_UPDATE = 2;

	public static final int CRITICAL_UPDATE = 1;

	public static final int NO_UPDATE = 0;
	
	public static final String INVITE_TIP = "invt";
	
	public static final String LABEL = "l" ;
	
	public static final String DISMISS = "dms";
	
	public static final String PERSISTENT_NOTIFICATION = "pn";
	
	public static final String PERSISTENT_NOTIF_ALARM_INTERVAL = "pnai";
	
	public static final String UPDATE_TITLE = "updateTitle";
	
	public static final String UPDATE_ACTION = "updateAction";
	
	public static final String UPDATE_LATER = "updateLater";
	
	public static final String UPDATE_ALARM = "updateAlarm";
	
	public static final String IS_PERSISTENT_UPDATE_NOTIFICATION = "isPers";
	
	public static final String PERSISTENT_NOTIF_MESSAGE = "persNotifMsg";
	
	public static final String PERSISTENT_NOTIF_TITLE = "persNotifTitle";
	
	public static final String PERSISTENT_NOTIF_ACTION = "persNotifAction";
	
	public static final String PERSISTENT_NOTIF_LATER = "persNotifLater";
	
	public static final String PERSISTENT_NOTIF_ALARM = "persNotifAlarm";
	
	public static final String PERSISTENT_NOTIF_URL = "persNotifUrl";
	
	public static final long PERS_NOTIF_ALARM_DEFAULT = 24*60*60;
	
	public static final String SHOULD_SHOW_PERSISTENT_NOTIF = "showPersistNotif";
	
	public static final String IS_PERS_NOTIF_ALARM_SET = "isPersNotifAlarmSet";
	
	public static final String IS_HIKE_APP_FOREGROUNDED = "isHikeAppForegrounded";
	
	public static final String UPDATE_TIP_HEADER = "updateTipHeader";
	
	public static final String UPDATE_TIP_BODY = "updateTipBody";
	
	public static final String UPDATE_TIP_LABEL = "updateTipLabel";
	
	public static final String UPDATE_TIP_DISMISS = "updateTipDismiss";
	
	public static final String UPDATE_TIP_BG_COLOR = "updateTipBgColor";
	
	public static final String INVITE_TIP_HEADER = "inviteTipHeader";
	
	public static final String INVITE_TIP_BODY = "inviteTipBody";
	
	public static final String INVITE_TIP_LABEL = "inviteTipLabel";
	
	public static final String INVITE_TIP_DISMISS = "inviteTipDismiss";
	
	public static final String INVITE_TIP_BG_COLOR = "inviteTipBgColor";
	
	public static final String SHOW_CRITICAL_UPDATE_TIP = "showCriticalUpdateTip";
	
	public static final String SHOW_NORMAL_UPDATE_TIP = "showNormalUpdateTip";
	
	public static final String SHOW_INVITE_TIP = "showInviteTip";
	
	public static final String UPDATE_TIP_AND_PERS_NOTIF_LOG = "UpdateTipPersistentNotif";

	// More explanation required?
	public static final int NUM_TIMES_SCREEN_SHOULD_OPEN_BEFORE_TOOL_TIP = 2;

	public static final String APP_API_VERSION = "3";

	public static final int NUM_SMS_PER_FRIEND = 10;

	public static final int INITIAL_NUM_SMS = 100;

	public static final int MAX_CHAR_IN_NAME = 20;

	public static final int MAX_CONTACTS_IN_GROUP = 1000;

	public static final int MAX_CONTACTS_IN_BROADCAST = 500;

	public static final int MAX_SMS_CONTACTS_IN_GROUP = MAX_CONTACTS_IN_GROUP;

	public static final int PROFILE_IMAGE_DIMENSIONS = 120;

	public static final int SIGNUP_PROFILE_IMAGE_DIMENSIONS = 200;

	public static final String VALID_MSISDN_REGEX = "\\+?[0-9]{1,15}";

	public static final int MAX_BUFFER_SIZE_KB = 100;

	public static final int MAX_FILE_SIZE = 100 * 1024 * 1024;

	public static final int IMAGE_CAPTURE_CODE = 1187;

	public static final int IMAGE_TRANSFER_CODE = 1188;

	public static final int VIDEO_TRANSFER_CODE = 1189;

	public static final int AUDIO_TRANSFER_CODE = 1190;

	public static final int SHARE_LOCATION_CODE = 1192;

	public static final int SHARE_CONTACT_CODE = 1193;

	public static final int SHARE_FILE_CODE = 1194;

	public static final int PLATFORM_REQUEST = 1195;

	public static final int PLATFORM_FILE_CHOOSE_REQUEST = 1196;

    public static final int PLATFORM_MSISDN_FILTER_DISPLAY_REQUEST = 1197;

    public static final int ADD_TO_CONFERENCE_REQUEST = 1196;

	public static final int FACEBOOK_REQUEST_CODE = 64206;

	public static final int MAX_DURATION_RECORDING_SEC = 360;

	public static final int MAX_DIMENSION_THUMBNAIL_PX = 270;

	public static final int MAX_DIMENSION_LOCATION_THUMBNAIL_PX = 220;

	public static final int MAX_DIMENSION_FULL_SIZE_PROFILE_PX = 500;

	public static final int MAX_DIMENSION_MEDIUM_FULL_SIZE_PX = 800;

	public static final int MAX_DIMENSION_LOW_FULL_SIZE_PX = 600;

	public static final int SMO_MAX_DIMENSION_MEDIUM_FULL_SIZE_PX = 1240;

	public static final int SMO_MAX_DIMENSION_LOW_FULL_SIZE_PX = 800;

	public static final int INITIAL_PROGRESS = 5;

	public static final int NO_CHANGE = 0;

	public static final int PARTICIPANT_STATUS_CHANGE = 1;

	public static final int NEW_PARTICIPANT = 2;

	public static final String MAIL = "support@hike.in";

	public static final int MAX_RECENTS_TO_SHOW = 10;

	public static final int MAX_RECENTLY_JOINED_HIKE_TO_SHOW = 5;

	// Had to add this constant since its only available in the android API for
	// Honeycomb and higher.
	public static final int FLAG_HARDWARE_ACCELERATED = 16777216;

	public static final int LOCAL_CLEAR_TYPING_TIME = 6 * 1000;

	public static final int RESEND_TYPING_TIME = 4 * 1000;

	// Number of recent contacts to show in the favorites drawer.
	public static final int RECENT_COUNT_IN_FAVORITE = 10;

	// Number of auto recommend contacts to show in favorites.
	public static final int MAX_AUTO_RECOMMENDED_FAVORITE = 5;

	// Fiksu Currency
	public static final String CURRENCY = "INR";

	// Fiksu Usernames
	public static final String FACEBOOK = "facebook";

	// public static final String TWITTER = "twitter";

	public static final String INVITE = "invite";

	public static final String FIRST_MESSAGE = "first_message";

	// Fiksu Prices
	// public static final int FACEBOOK_CONNECT = 100;

	// public static final int TWITTER_CONNECT = 100;

	public static final int INVITE_SENT = 50;

	public static final int FIRST_MSG_IN_DAY = 10;

	/*
	 * Maximum number of conversations to be made automatically when the user signs up
	 */
	public static final int MAX_CONVERSATIONS = 6;

	/*
	 * Constant used as a type to signify that this message was added locally by the client when the user signed up
	 */
	public static final String INTRO_MESSAGE = "im";

	public static final String LOCATION_CONTENT_TYPE = "hikemap/location";

	public static final String LOCATION_FILE_NAME = "Location";

	public static final String CONTACT_CONTENT_TYPE = "contact/share";

	public static final String CONTACT_FILE_NAME = "Contact";

	public static final int DEFAULT_ZOOM_LEVEL = 16;

	public static final String VOICE_MESSAGE_CONTENT_TYPE = "audio/voice";

	// Picasa URI start for JB devices
	public static final String JB_PICASA_URI_START = "content://com.sec.android.gallery3d";

	// Picasa URI start for other devices
	public static final String OTHER_PICASA_URI_START = "content://com.google.android.gallery3d";

	// Picasa URI prefix for creating convMessage
	public static final String PICASA_PREFIX = "picasaUri:";

	// Gmail URI prefix for all devices
	public static final String GMAIL_PREFIX = "content://gmail-ls";

	// Google Plus URI prefix for all devices
	public static final String GOOGLE_PLUS_PREFIX = "content://com.google.android.apps.photos.content";

	// Google Inbox App URI prefix for all devices
	public static final String GOOGLE_INBOX_PREFIX = "content://com.google.android.apps.bigtop";

	// Google Drive App URI prefix for all devices
	public static final String GOOGLE_DRIVE_PREFIX = "content://com.google.android.apps.docs.storage.legacy";

	public static final int MAX_MESSAGES_TO_LOAD_INITIALLY = 40;

	public static final int MAX_PINS_TO_LOAD_INITIALLY = 20;

	public static final int MAX_OLDER_PINS_TO_LOAD_EACH_TIME = 10;

	public static final int MAX_OLDER_MESSAGES_TO_LOAD_EACH_TIME = 20;

	public static final int MIN_INDEX_TO_LOAD_MORE_MESSAGES = 10;

	public static final int MAX_STATUSES_TO_LOAD_INITIALLY = 30;

	public static final int MAX_OLDER_STATUSES_TO_LOAD_EACH_TIME = 20;

	public static final int SHOW_CREDITS_AFTER_NUM = 10;

	public static final String HIKE_DIRECTORY_ROOT = Environment.getExternalStorageDirectory() + "/Hike";

	public static final String HIKE_MEDIA_DIRECTORY_ROOT = Environment.getExternalStorageDirectory() + "/Hike/Media";

	public static final String HIKE_BACKUP_DIRECTORY_ROOT = Environment.getExternalStorageDirectory() + "/Hike/Backup";

	public static final String PROFILE_ROOT = "/hike Profile Images";

	public static final String IMAGE_ROOT = "/hike Images";

	public static final String VIDEO_ROOT = "/hike Videos";

	public static final String AUDIO_ROOT = "/hike Audios";

	public static final String AUDIO_RECORDING_ROOT = "/hike Voice Messages";

	public static final String OTHER_ROOT = "/hike Others";

	public static final String SENT_ROOT = "/sent";

	public static final String STICKERS_ROOT = "/stickers";

	public static final String DISK_CACHE_ROOT = "/diskcache";

	public static final String LARGE_STICKER_FOLDER_NAME = "stickers_l";

	public static final String SMALL_STICKER_FOLDER_NAME = "stickers_s";

	public static final String OLD_LARGE_STICKER_FOLDER_NAME = "large";

	public static final String OLD_SMALL_STICKER_FOLDER_NAME = "small";

	public static final String LARGE_STICKER_ROOT = "/" + LARGE_STICKER_FOLDER_NAME;

	public static final String SMALL_STICKER_ROOT = "/" + SMALL_STICKER_FOLDER_NAME;

	public static final String HIKE_FILE_LIST_NAME = "hikeFiles";

	public static final String STATUS_MESSAGE_HEADER = "hike-status-message";

	public static final String PIN_HISTORY_FRAGMENT_TAG = "pin-history-fragment";

	public static final String VOIP_CALL_RATE_FRAGMENT_TAG = "voipCallRateFragmentTag";

	public static final String VOIP_CALL_ISSUES_FRAGMENT_TAG = "voipCallIssuesFragmentTag";

	public static final String VOIP_CALL_FAILED_FRAGMENT_TAG = "voipCallFailedFragmentTag";

	public static final String VOIP_CALL_DECLINE_MESSAGE_FRAGMENT_TAG = "voipCallDeclineMessageFragTag";
	/*
	 * Contact Type
	 */
	public static final int ON_HIKE_VALUE = 1;

	public static final int NOT_ON_HIKE_VALUE = 0;

	public static final int BOTH_VALUE = -1;

	public static final String INDIA_COUNTRY_CODE = "+91";

	public static final String SAUDI_ARABIA_COUNTRY_CODE = "+966";
	
	public static final String UAE_COUNTRY_CODE = "+971";
	
	// Soudi Arabia, United Arab Emirates, Egypt, Iran, Turkey, Iraq, Yemen, Syria, Israel, Jordan, Palestine, Lebanon, 
	// Oman, Kuwait, Qatar,	Bahrain, Cyprus
	public static final String[] SSL_NOT_ALLOWED_COUNTRIES = {SAUDI_ARABIA_COUNTRY_CODE, UAE_COUNTRY_CODE, "+20", "+98", "+90", "+964", "+967", 
		"+963", "+972", "+962", "+970", "+961", "+968", "+965", "+974", "+973", "+357"};

	public static final int MDPI_TIMES_10 = 11;

	public static final String NAMESPACE = "http://schemas.android.com/apk/res/com.bsb.hike";

	public static final String FONT = "font";

	public static final String MAX_LINES = "maxLines";

	public static final int MAX_MESSAGE_PREVIEW_LENGTH = 300;

	public static final String FACEBOOK_PROFILEPIC_URL_FORMAT = "https://graph.facebook.com/%1$s/picture?height=%2$d&width=%2$d";

	/*
	 * Constants for Profile Pic
	 */
	/* dialog IDs */
	public static final int NEW_PROFILE_PICTURE = 0;

	public static final int REMOVE_PROFILE_PICTURE = 1;

	/* activityForResult IDs */
	public static final int CAMERA_RESULT = 0;

	public static final int GALLERY_RESULT = 1;

	public static final int CROP_RESULT = 2991;

	public static final int MIN_STATUS_COUNT = 5;

	public static final int MAX_NUX_CONTACTS = 30;

	public static final int MAX_PRECHECKED_CONTACTS = 15;

	// public static final int MAX_TWITTER_POST_LENGTH = 140;

	// public static final int MAX_MOOD_TWITTER_POST_LENGTH = 130;

	public static final int MAX_NUM_STICKER_REQUEST = 10;

	/* In seconds */
	public static final int DEFAULT_PROTIP_WAIT_TIME = 300;

	public static final String PROTIP_STATUS_NAME = "hike team";

	/* In seconds */
	public static final int DEFAULT_UNDELIVERED_WAIT_TIME = 30;

	public static final int MAX_FALLBACK_NATIVE_SMS = 19;

	public static final int MAX_SMS_PULL_IN_INBOX = 2000;

	public static final int MAX_SMS_PULL_IN_SENTBOX = 1000;

	public static final String MICROMAX = "MICROMAX";

	public static final int MAX_READ_BY_NAMES = 4;

	public static final int LDPI_ID = 4;

	public static final int MDPI_ID = 3;

	public static final int HDPI_ID = 2;

	public static final int XHDPI_ID = 1;

	public static final int XXHDPI_ID = 0;

	public static final int XXXHDPI_ID = 12;

	public static final int LDPI_CACHE = 15;

	public static final int MDPI_CACHE = 20;

	public static final int HDPI_CACHE = 25;

	public static final int XHDPI_CACHE = 30;

	public static final int XXHDPI_CACHE = 30;

	public static final int EMPTY_CONVERSATIONS_PREFILL_LIMIT = 6;

	public static final int FTUE_LIMIT = 5;

	public static final int FTUE_CONTACT_CARD_LIMIT = 5;

	public static final String FTUE_MSISDN_TYPE = "ftueContact";

	public static final double PROFILE_PIC_FREE_SPACE = 3 * 1024 * 1024;

	// LED light Notifications constants
	public static final int LED_LIGHTS_ON_MS = 300;

	public static final int LED_LIGHTS_OFF_MS = 1000;

	public static final int JOINED_HIKE_STATUS_ID = -1;

	public static final int SELECT_COUNTRY_REQUEST_CODE = 4001;

	public static final int MAX_VELOCITY_FOR_LOADING_IMAGES_SMALL = 25;

	public static final int MAX_VELOCITY_FOR_LOADING_IMAGES = 10;

	public static final int MAX_VELOCITY_FOR_LOADING_TIMELINE_IMAGES = 5;

	public static final String SCREEN = "screen";

	public static final String FTUE = "ftue";

	public static final int MAX_FAST_SCROLL_VISIBLE_POSITION = 6;

	public static final int STEALTH_OFF = 0;

	public static final int STEALTH_ON_FAKE = 1;

	public static final int STEALTH_ON = 2;

	public static final long RESET_COMPLETE_STEALTH_TIME_MS = 30 * 60 * 1000;

	public static final String ENABLED = "enabled";

	public static final String ENABLED_STEALTH = "en";

	public static final String DISABLED_STEALTH = "di";

	public static final String RESET = "reset";

	public static final String STEALTH = "stlth";

	public static final String FILE_SHARE_PREFIX = "file://";

	public static final String SHARE_CONTACT_CONTENT_TYPE = "text/x-vcard";

	public static final int MAX_LAST_SEEN_RETRY_COUNT = 3;

	public static final int RETRY_WAIT_ADDITION = 2;

	public static final String IMAGE_FRAGMENT_TAG = "imageFragmentTag";

	public static final String STICKER_RECOMMENDATION_FRAGMENT_TAG = "stickerRecommendationFragmentTag";

	public static final String STICKER_RECOMMENDATION_FRAGMENT_FTUE_TAG = "stickerRecommendationFragmentFtueTag";

	public static final String SHOW_STEALTH_POPUP = "stlthrmd";

	public static final String SHOW_FESTIVE_POPUP = "showFestivePopup";

	public static final String HOLI_POPUP = "holi";

	public static final String HIKE_OFFLINE_NOTIFICATION_PREF = "hikeOfflineNotificationPref";

	public static final String PIN = "pin";

	public static final String RIGHTS = "rights";

	public static final String UNREAD_COUNT = "unreadCount";

	public static final String TO_SHOW = "toShow";

	public static final String ADD_MEMBERS = "addMembers";

	public static final String PIN_DISPLAYED = "displayed";

	public static final String STATUS_BAR_BLUE = "blue";

	public static final String STATUS_BAR_TRANSPARENT = "transparent";
	public static final String STATUS_BAR_TIMELINE = "timeline";

	public static final int MAX_MEDIA_ITEMS_TO_LOAD_INITIALLY = 51;

	public static final String TOTAL_SELECTIONS = "t";

	public static final String SUCCESSFUL_SELECTIONS = "s";

	public static final String DRAWABLE = "drawable";

	public static final String VOIP_CALL_DURATION = "vcd";

	public static final String VOIP_CALL_INITIATOR = "vci";

	public static final String VOIP_BITRATE_2G = "vb2g";

	public static final String VOIP_BITRATE_3G = "vb3g";

	public static final String VOIP_BITRATE_WIFI = "vbw";

	public static final String VOIP_BITRATE_CONFERENCE = "vbc";

	public static final String VOIP_ACTIVATED = "voip";

	public static final String VOIP_CALL_RATE_POPUP_SHOW = "vrmcs";

	public static final String VOIP_FTUE_POPUP = "voip_popup";

	public static final String VOIP_CALL_RATE_POPUP_FREQ = "vrmcf";

	public static final String VOIP_RELAY_SERVER_PORT = "rsport";

	public static final String VOIP_AEC_ENABLED = "aec";

	public static final String VOIP_CONFERENCING_ENABLED = "conf";

	public static final String VOIP_GROUP_CALL_ENABLED = "gccall";

	public static final String VOIP_AEC_CPU_NR = "cpunr";

	public static final String VOIP_AEC_CPU = "cpuaec";

	public static final String VOIP_AEC_MO = "aecmo";

	public static final String VOIP_AEC_TYPE = "aect";

	public static final String VOIP_AEC_CNP = "cnp";

	public static final String VOIP_AEC_TAIL_TYPE = "att";

	public static final String VOIP_RATINGS_LEFT = "vrl";

	public static final String WT_1_REVAMP_ENABLED = "wt_1";

	public static final class ChatHead
	{
		public static final String STICKER_WIDGET = "stkr_wdgt";

		public static final String ENABLE = "enable";

		public static final String USER_CONTROL = "usr_ctrl";
		
		public static final String FORCE_ACCESSIBILITY="frc_acsb";
		
		public static final String SHOW_ACCESSIBILITY="shw_acsb";

		public static final String DONT_USE_ACCESSIBILITY="acsb_not";

		public static final String STICKERS_PER_DAY = "stkr_per_day";

		public static final String EXTRA_STICKERS_PER_DAY = "extra_stkr_per_day";

		public static final String TOTAL_STICKER_SHARE_COUNT = "ttl_stkr_shr_count";

		public static final String PACKAGE_LIST = "pkg_list";

		public static final String PACKAGE_NAME = "p";

		public static final String APP_NAME = "a";

		public static final String APP_ENABLE = "e";

		public static final int DEFAULT_NO_STICKERS_PER_DAY = 5;

		public static final String DISMISS_COUNT = "dismiss_count";

		public static final String SNOOZE = "snoozeChatHead";

	}

	public static final String VOIP_RELAY_IPS = "vrip";

	public static final class ResultCodes
	{
		public static final int SELECT_COUNTRY = 4001;

		public static final int CREATE_LOCK_PATTERN = 4002;

		public static final int CONFIRM_LOCK_PATTERN = 4003;

		public static final int CONFIRM_AND_ENTER_NEW_PASSWORD = 4004;

		public static final int CREATE_LOCK_PATTERN_HIDE_CHAT = 4005;

		public static final int CONFIRM_LOCK_PATTERN_HIDE_CHAT = 4006;

		public static final int CONFIRM_LOCK_PATTERN_CHANGE_PREF = 4007;

		public static final int PHOTOS_REQUEST_CODE = 739;
	}

	public static final class Extras
	{
		public static final String MSISDN = "msisdn";

		public static final String NAME = "name";

		public static final String PREV_MSISDN = "prevMsisdn";

		public static final String LOVED_BY_SELF = "selfLove";

		public static final String PREV_NAME = "prevName";

		public static final String BROADCAST_RECIPIENTS = "broadcastRecipients";

		public static final String INVITE = "invite";

		public static final String MSG = "msg";

		public static final String POKE = "poke";

		public static final String PREF = "pref";

		public static final String EDIT = "edit";

		public static final String IMAGE_PATH = "image-path";

		public static final String SCALE = "scale";

		public static final String OUTPUT_X = "outputX";

		public static final String OUTPUT_Y = "outputY";

		public static final String ASPECT_X = "aspectX";

		public static final String ASPECT_Y = "aspectY";

		public static final String JPEG_COMPRESSION_QUALITY = "jpegCompressionQuality";

		public static final String DATA = "data";

		public static final String RETURN_DATA = "return-data";

		public static final String BITMAP = "bitmap";

		public static final String CIRCLE_CROP = "circleCrop";

		public static final String CIRCLE_HIGHLIGHT = "circleHighlight";

		public static final String SCALE_UP = "scaleUpIfNeeded";

		public static final String UPDATE_AVAILABLE = "updateAvailable";

		public static final String KEEP_MESSAGE = "keepMessage";

		public static final String SHOW_CREDITS_HELP = "showCreditsHelp";

		public static final String CREDITS_HELP_COUNTER = "CreditsHelpCounter";

		public static final String SIGNUP_TASK_RUNNING = "signupTaskRunning";

		public static final String SIGNUP_PART = "signupPart";

		public static final String SIGNUP_TEXT = "signupText";

		public static final String SIGNUP_ERROR = "signupError";

		public static final String TOOLTIP_SHOWING = "tooltipShowing";

		public static final String FADE_OUT = "fadeOut";

		public static final String FADE_IN = "fadeIn";

		public static final String EDIT_PROFILE = "editProfile";

		public static final String EMAIL = "email";

		public static final String GENDER = "gender";

		public static final String RESTORE_STATUS = "restoreStatus";

		public static final String OVERLAY_SHOWING = "overlayShowing";

		public static final String GROUP_CHAT = "groupChat";

		public static final String BROADCAST_LIST = "broadcastList";

		public static final String EMOTICON_SHOWING = "emoticonShowing";

		public static final String EXISTING_GROUP_CHAT = "existingGroupChat";

		public static final String ADD_TO_CONFERENCE = "addToConference";

		public static final String EXISTING_BROADCAST_LIST = "existingBroadcastList";

		public static final String LEAVE_GROUP_CHAT = "leaveGroupChat";

		public static final String APP_STARTED_FIRST_TIME = "appStartedFirstTime";

		public static final String LATEST_VERSION = "latestVersion";

		public static final String SHOW_UPDATE_OVERLAY = "showUpdateOverlay";

		public static final String SHOW_UPDATE_TOOL_TIP = "showUpdateToolTip";

		public static final String UPDATE_TOOL_TIP_SHOWING = "updateToolTipShowing";

		public static final String UPDATE_MESSAGE = "updateMessage";

		public static final String APPLICATIONSPUSH_MESSAGE = "applicationsPushMessage";

		public static final String URL_TO_LOAD = "urlToLoad";

		public static final String TITLE = "title";

		public static final String WEBVIEW_ALLOW_LOCATION = "allocLoc";

		public static final String FIRST_TIME_USER = "firstTimeUser";

		public static final String IS_DELETING_ACCOUNT = "isDeletingAccount";

		public static final String SMS_MESSAGE = "incomingSMSMessage";

		public static final String GROUP_LEFT = "groupLeft";

		public static final String ALERT_CANCELLED = "alertCancelled";

		public static final String DEVICE_DETAILS_SENT = "deviceDetailsSent";

		public static final String SIGNUP_MSISDN_ERROR = "signupMsisdnError";

		public static final String FILE_TRANSFER_DIALOG_SHOWING = "fileTransferDialogShowing";

		public static final String FILE_PATH = "filePath";

		public static final String FILE_KEY = "fileKey";

		public static final String FILE_TYPE = "fileType";

		public static final String FILE_NAME = "fileName";

		public static final String RECORDER_DIALOG_SHOWING = "recorderDialogShowing";

		public static final String RECORDER_START_TIME = "recorderStartTime";

		public static final String IS_LEFT_DRAWER_VISIBLE = "isLeftDrawerVisible";

		public static final String IS_RIGHT_DRAWER_VISIBLE = "isRightDrawerVisible";

		public static final String FORWARD_MESSAGE = "forwardMessage";

		public static final String HELP_PAGE = "helpPage";

		public static final String WHICH_EMOTICON_CATEGORY = "whichEmoticonCategory";

		public static final String WHICH_EMOTICON_SUBCATEGORY = "whichEmoticonSubcategory";

		public static final String COUNTRY_CODE = "countryCode";

		public static final String GOING_BACK_TO_HOME = "goingBackToHome";

		public static final String UPDATE_URL = "updateURL";

		public static final String UPDATE_TO_IGNORE = "updateToIgnore";

		public static final String INTRO_MESSAGE_ADDED = "introMessageAdded";

		public static final String LATITUDE = "latitude";

		public static final String LONGITUDE = "longitude";

		public static final String ZOOM_LEVEL = "zoomLevel";

		public static final String CONTACT_INFO = "contactInfo";

		public static final String WHICH_CHAT_THREAD = "whichChatThread";

		public static final String ONE_TO_ONE_CHAT_THREAD = "oneToOneChat";

		public static final String GROUP_CHAT_THREAD = "groupChat";

		public static final String BROADCAST_CHAT_THREAD = "broadcastChat";

		public static final String BOT_CHAT_THREAD = "botChat";

		public static final String CONTACT_INFO_TIMELINE = "contactInfoTimeline";

		public static final String CONTACT_ID = "contactId";

		public static final String ON_HIKE = "onHike";

		public static final String SHOWING_SECOND_LOADING_TXT = "showingSecondLoadingTxt";

		public static final String FACEBOOK_POST_POPUP_SHOWING = "facebookPostPopupShowing";

		public static final String GPS_DIALOG_SHOWN = "gpsDialogShown";

		public static final String REWARDS_PAGE = "rewardsPage";

		public static final String GAMES_PAGE = "gamesPage";

		public static final String CUSTOM_LOCATION_SELECTED = "customLocationSelected";

		public static final String CUSTOM_LOCATION_LAT = "customLocationLat";

		public static final String CUSTOM_LOCATION_LONG = "customLocationLong";

		public static final String OPEN_FAVORITES = "openFavorites";

		public static final String CONTACT_METADATA = "contactMetadata";

		public static final String FROM_CENTRAL_TIMELINE = "fromCentralTimeline";

		public static final String FROM_DELETE_ACCOUNT = "fromDeleteAccount";
		
		public static final String BLOCKED_LIST = "blockedList";

		public static final String NUX1_NUMBERS = "nux1Numbers";

		public static final String NUX_NUMBERS_INVITED = "nuxNumbersInvited";

		public static final String DIALOG_SHOWING = "dialogShowing";

		public static final String SMS_ID = "smsId";

		public static final String RECORDED_TIME = "recordedTime";

		public static final String SHOW_FRIENDS_TUTORIAL = "showFriendsTutorial";

		// public static final String POST_TO_TWITTER = "postToTwitter";

		public static final String RECORDING_TIME = "recordingTime";

		public static final String MAPPED_ID = "mappedId";

		public static final String IS_STATUS_IMAGE = "isStatusImage";

		public static final String URL = "url";

		public static final String TAB_INDEX = "tabIndex";

		public static final String IS_TEXT_SEARCH = "isTextSearch";

		public static final String HTTP_SEARCH_STR = "searchStr";

		public static final String IS_FACEBOOK = "isFacebook";

		public static final String EMOTICON_TYPE = "emoticonType";

		public static final String CREATE_GROUP = "createGroup";

		public static final String CREATE_GROUP_SETTINGS = "createGroupSettings";

		public static final String CREATE_BROADCAST = "createBroadcast";

		public static final String CREATE_GROUP_SRC = "createGroupSource";

		public static final String COMPOSE_MODE = "composeMode";

		public static final int CREATE_BROADCAST_MODE = 7;

		public static final String FROM_CREDITS_SCREEN = "fromCreditsScreen";

		public static final String SHOW_KEYBOARD = "ShowKeyboard";

		public static final String SHOW_RECORDING_DIALOG = "showRecordingDialog";

		public static final String IS_HOME_POPUP_SHOWING = "homePopupType";

		public static final String LAST_UPDATE_PACKET_ID = "lastUpdatePacketId";

		public static final String LAST_APPLICATION_PUSH_PACKET_ID = "lastApplicationPushPacketId";

		public static final String FREE_SMS_POPUP_BODY = "freeSMSPopupBody";

		public static final String FREE_SMS_POPUP_HEADER = "freeSMSPopupHeader";

		public static final String SHOW_STICKER_TIP_FOR_EMMA = "showStickerTipForEmma";

		public static final String FROM_CHAT_THEME_FTUE = "fromChatThemeFtue";

		public static final String NEW_USER = "newUser";

		public static final String CHAT_THEME_WINDOW_OPEN = "chatThemeWindowOpen";

		public static final String SELECTED_THEME = "selectedTheme";

		public static final String BLOKING_TASK_TYPE = "blockingTaskType";

		public static final String MULTIPLE_MSG_OBJECT = "multipleMsgObject";

		public static final String SELECTED_BUCKET = "selectedBucket";

		public static final String GALLERY_SELECTION_SINGLE = "gallerySelection";

		public static final String GALLERY_SELECTIONS = "gallerySelections";

		public static final String GALLERY_ITEMS = "galleryItems";

		public static final String FILE_PATHS = "filePaths";

		public static final String IS_ACTION_MODE_ON = "isActionModeOn";

		public static final String SELECTED_POSITIONS = "selectedPositions";

		public static final String SELECTED_NON_FORWARDABLE_MSGS = "selectedNonForwadableMsgs";

		public static final String SELECTED_NON_TEXT_MSGS = "selectedNonTextMsgs";

		public static final String SELECTED_CANCELABLE_MSGS = "selectedCancelableMsgs";

		public static final String SELECTED_SHARABLE_MSGS_COUNT = "selectedCancelableMsgsCount";

		public static final String SELECTED_SHARABLE_MSGS_PATH = "selectedCancelableMsgsPath";

		public static final String SELECTED_COUNTRY = "selectedCountry";

		public static final String MANUAL_SYNC = "manualSync";

		public static final String IS_FTUT_ADD_FRIEND_POPUP_SHOWING = "isFtueAddFriendPopup";

		public static final String ONETON_CONVERSATION_NAME = "groupName";

		public static final String CONVERSATION_ID = "groupOrBroadcastId";

		public static final String SHOWING_INVALID_PIN_ERROR = "showingInvalidPinError";

		public static final String CALLED_FROM_FTUE_POPUP = "calledFromFtuePopUP";

		public static final String FRIENDS_LIST_COUNT = "friendsListCount";

		public static final String HIKE_CONTACTS_COUNT = "hikeContactsCount";

		public static final String RECOMMENDED_CONTACTS_COUNT = "recommendedContactsCount";

		public static final String SELECTED_SHARABLE_MSGS_MIME_TYPE = "selectedCancelableMsgsMimeType";

		public static final String FROM_NOTIFICATION = "fromNotification";

		public static final String OPEN_ACTIVITY_FEED = "openAcFeed";

		public static final String LAST_STEALTH_POPUP_ID = "lastStealthPopupId";

		public static final String STEALTH_PUSH_HEADER = "stealthPushHeader";

		public static final String STEALTH_PUSH_BODY = "stleathPushBody";

		public static final String OFFLINE_PUSH_KEY = "failed";

		public static final String OFFLINE_MSISDNS = "msisdns";

		public static final String STEALTH_PASS_RESET = "stealthPasswordReset";

		public static final String IS_RESET_PASS = "isResetStealthPasswordFlow";

		public static final String HAS_TIP = "hasTip";

		public static final String TOTAL_MSGS_CURRENTLY_LOADED = "totalMsgsCurrentlyLoaded";

		public static final String PIN_TYPE_SHOWING = "pinTypeShowing";

		public static final String SHARED_FILE_ITEMS = "sharedFileItems";

		public static final String IS_LAST_MESSAGE = "isLastMessage";

		public static final String IS_GROUP_CONVERSATION = "isGroupCoversation";

		public static final String PARTICIPANT_MSISDN_ARRAY = "participantMsisdnArray";

		public static final String PARTICIPANT_NAME_ARRAY = "participantNameArray";

		public static final String CONVERSATION_NAME = "conversationName";

		public static final String DELETE_MEDIA_FROM_PHONE = "deleteMediaFromPhone";

		public static final String DELETED_MESSAGE_TYPE = "deletedMessageType";

		public static final String CURRENT_POSITION = "currentPosition";

		public static final String SDK_THIRD_PARTY_PKG = "third_party_app_pkg";

		public static final String SDK_CONNECTION_TYPE = "connection_type";

		public static final String SELECT_ALL_INITIALLY = "selectAllInitially";

		public static final String NUX_INCENTIVE_MODE = "showNuxIncentiveMode";

		public static final String RETURN_CROP_RESULT_TO_FILE = "returnToFile";

		public static final String CALL_RATE_BUNDLE = "callRateBundle";

		public static final String CLEARED_OUT = "extrasClearedOut";
		
		public static final String HIKE_DIRECT_MODE="hikedirectmode";
		public static final String SHOW_TIMELINE = "showTimeline";

		// constants related to sharing Functioanlity
		public static final class ShareTypes
		{

			public static final int TEXT_SHARE = 0;

			public static final int IMAGE_SHARE = 1;

			public static final int STICKER_SHARE = 2;

		}

		public static final String SHARE_CONTENT = "shareContent";

		public static final String PACKAGE_NAME = "packageName";

		public static final String WHATSAPP_PACKAGE = "com.whatsapp";

		public static final String SHARE_TYPE = "shareType";

		public static final int NOT_SHAREABLE = -1;

		public static final String STICKER_DESCRIPTION = "shareStkrTxt";

		public static final String STICKER_CAPTION = "shareStkrCptn";

		public static final String IMAGE_HEADING = "shareImgTtl";

		public static final String IMAGE_DESCRIPTION = "shareImgTxt";

		public static final String IMAGE_CAPTION = "shareImgCptn";

		public static final String TEXT_HEADING = "shareMsgTitle";

		public static final String TEXT_CAPTION = "shareMsgTxt";

		public static final String SHOW_SHARE_FUNCTIONALITY = "shareWA";

		public static final String STICKER_SHARE = "stkrShr";

		public static final String TEXT_SHARE = "textShr";

		public static final String IMAGE_SHARE = "imgShr";

		public static final String WHATSAPP_SHARE = "whatsappShare";

		// required for analytics
		public static final String CATEGORYID = "md1";

		public static final String STICKERID = "md2";

		public static final String PATH = "md3";

		public static final String GENERAL_SO_TIMEOUT = "sto";

		public static final String OKHTTP_CONNECT_TIMEOUT = "okcto";

		public static final String OKHTTP_READ_TIMEOUT = "okrto";

		public static final String OKHTTP_WRITE_TIMEOUT = "okwto";

		public static final String ENABLE_PHOTOS = "ph_en";

		public static final String ENABLE_CLOUD_SETTING_BACKUP = "rux_stg_bkup";

		public static final String STATUS_UPDATE_SHOW_COUNTS = "su_sc";

		public static final String STATUS_UPDATE_SHOW_LIKES = "su_sl";

		public static final String FT_UPLOAD_SO_TIMEOUT = "ftsto";

		public static final String MAX_MESSAGE_PROCESS_TIME = "mmpt";

		public static final String CHANGE_MAX_MESSAGE_PROCESS_TIME = "cmpt";

		public static final String PHOTOS_RETURN_FILE = "editedReturnFile";

		public static final String CAMERA_RETURN_FILE = "capturedReturnFile";

		public static final String BROADCAST_CREATE_BUNDLE = "broadcastCreationBundle";

		public static final String GROUP_CREATE_BUNDLE = "groupCreationBundle";

		public static final String LAST_MESSAGE_TIMESTAMP = "lastMessageTimeStamp";

		public static final String ENABLE_SEND_LOGS = "ulogs_on";

		public static final String NEW_GROUP = "newGroups";

		public static final String HAS_CUSTOM_ICON = "h_cus_icon";

		public static final String IS_PROFILE_PIC_DOWNLOAD = "is_profile_pic_download";

		public static final String BYTES = "bytes";

		public static final String DEL_SCR_FILE_ON_CALL_FAIL = "del_tmp_file_call_fail";

		public static final String DEL_PREV_MSISDN_PIC = "del_prev_msisdn_pic";
		
		public static final String THUMBNAILS_REQUIRED = "thumbnailsRequired";

		public static final String CHAT_INTENT_TIMESTAMP = "chat_ts";
		
		public static final String GROUP_CHAT_DP = "group_chat_dp";
		
		public static final String IS_MICROAPP_SHOWCASE_INTENT = "microappShowcaseIntent";

		public static final String STICKER_SETTINGS_TASK = "stickerSettingsTask";

        public static final String IS_CONTACT_CHOOSER_FILTER_INTENT = "contactChooserFilterIntent";

        public static final String LIST = "list";

        public static final String MICRO_APPS_REQUEST_CODE = "microapps_request_code";

        public static final String FUNCTION_ID = "function_id";

        public static final String RESULT_CODE = "result_code";

		public static final String PROFILE_DOB = "profile_dob";

    }

	public static final class LogEvent
	{
		// Common tags for Countly. Don't change.

		public static final String SOURCE_APP = "source_app";

		public static final String TAG = "tag";

		public static final String DEVICE_ID = "device_id";

		public static final String OS = "_os";

		public static final String OS_VERSION = "_os_version";

		public static final String DEVICE = "_device";

		public static final String RESOLUTION = "_resolution";

		public static final String CARRIER = "_carrier";

		public static final String APP_VERSION = "_app_version";

		/*
		 * Naming Convention - <screen><event><sub-event>
		 */

		/*
		 * Home screen events <screen> = hoS <event> = profS, invS, feedS, delAC, delC, compB, addSC, creDtiPN, creDtiPY, upDtiPN, upDtipY, upDOBD, upDOB, smSY, smSN, groupS, dRB
		 */
		public static final String MENU = "hoS";

		public static final String DELETE_ALL_CONVERSATIONS_MENU = "hoSdelAC";

		public static final String DELETE_CONVERSATION = "hoSdelC";
		
		public static final String EXIT_GC_CONVERSATION = "exitgc";
		
		public static final String DELETE_GC_CONVERSATION = "delgc";
		
		public static final String LEAVE_GROUP_VIA_PROFILE = "gcinfo";
		
		public static final String LEAVE_GROUP_VIA_HOME = "cvl";
		
		public static final String COMPOSE_BUTTON = "hoScompB";

		public static final String ADD_SHORTCUT = "hoSaddSC";

		public static final String HOME_TOOL_TIP_CLOSED = "hoScreDtiPN";

		public static final String HOME_TOOL_TIP_CLICKED = "hoScreDtiPY";

		public static final String HOME_UPDATE_TOOL_TIP_CLOSED = "hoSupDtiPN";

		public static final String HOME_UPDATE_TOOL_TIP_CLICKED = "hoSupDtiPY";

		public static final String HOME_UPDATE_OVERLAY_DISMISSED = "hoSupDOBD";

		public static final String HOME_UDPATE_OVERLAY_BUTTON_CLICKED = "hoSupDOB";

		public static final String DEFAULT_SMS_DIALOG_YES = "hoSsmSY";

		public static final String DEFAULT_SMS_DIALOG_NO = "hoSsmSN";

		public static final String DRAWER_BUTTON = "hoSdRB";

		public static final String EMAIL_CONVERSATION = "hoSecON";

		/*
		 * Profile screen events <screen> = profS <event> = proES, credS, notyS, privS, helpS
		 */
		public static final String EDIT_PROFILE = "profSproES";

		public static final String NOTIFICATION_SCREEN = "profSnotyS";

		public static final String PRIVACY_SCREEN = "profSprivS";

		public static final String MEDIA_THUMBNAIL_VIA_PROFILE = "profSthumb";

		public static final String OPEN_GALLERY_VIA_PROFILE = "profSopenGallery";

		public static final String SHARED_FILES_VIA_PROFILE = "profSsharedFiles";

		public static final String ADD_TO_FAVOURITE = "profSAddToFav";

		public static final String INVITE_TO_HIKE_VIA_PROFILE = "profSinvite";

		public static final String SET_PROFILE_PIC_GALLERY = "profSpicGallery";

		/*
		 * Invite screen events <screen> = invS <event> = credB, creDtiPN, creDtiPY
		 */
		public static final String INVITE_TOOL_TIP_CLOSED = "invScreDtiPN";

		public static final String INVITE_TOOL_TIP_CLICKED = "invScreDtiPY";

		public static final String CREDIT_TOP_BUTTON = "invScredB";

		/*
		 * Chat thread screen events <screen> = chatS <event> = inVtiPN, inVtopB, blocK, forMsg, infoB, invOB, invOBD, opTiNtaP, calL, adD, grPinfO
		 */
		public static final String CHAT_INVITE_TOOL_TIP_CLOSED = "chatSinVtiPN";

		public static final String CHAT_INVITE_TOP_BUTTON = "chatSinVtopB";

		public static final String MENU_BLOCK = "chatSblocK";

		public static final String FORWARD_MSG = "chatSforMSG";

		public static final String I_BUTTON = "chatSinfoB";

		public static final String INVITE_OVERLAY_BUTTON = "chatSinvOB";

		public static final String INVITE_OVERLAY_DISMISS = "chatSinvOBD";

		public static final String OPT_IN_TAP_HERE = "chatSopTiNtaP";

		public static final String MENU_CALL = "chatScalL";

		public static final String MENU_ADD_TO_CONTACTS = "chatSadD";

		public static final String GROUP_INFO_TOP_BUTTON = "chatSgrPinfO";

		/*
		 * Credits screen events <screen> = credS <event> = inVB
		 */
		public static final String INVITE_BUTTON_CLICKED = "credSinVB";

		/*
		 * Group Info screen <screen> = groupS <event> = adDparT
		 */
		public static final String ADD_PARTICIPANT = "groupSadDparT";

		/*
		 * SignUp screen <screen> = signupS <event> = erroR
		 */
		public static final String SIGNUP_ERROR = "signupSerroR";

		/*
		 * Compose screen <screen> = compS <event> = refContcts
		 */
		public static final String COMPOSE_REFRESH_CONTACTS = "compSrefContcts";

		public static final String SELECT_ALL_HIKE_CONTACTS = "compSslctAllFwd";

		public static final String CONFIRM_FORWARD = "compSconfFwd";

		public static final String SELECT_ALL_SHARE = "compSslctAllShare";

		public static final String CONFIRM_SHARE = "compSconfShare";

		/*
		 * Drawer screen <screen> = drS <event> = homE, gC, inV, reW, creD, proF, settinG
		 */
		public static final String DRAWER_HOME = "drShomE";

		public static final String DRAWER_GROUP_CHAT = "drSgC";

		public static final String DRAWER_INVITE = "drSinV";

		public static final String DRAWER_REWARDS = "drSreW";

		public static final String DRAWER_CREDITS = "drScreD";

		public static final String DRAWER_PROFILE = "drSproF";

		public static final String DRAWER_SETTINGS = "drSsettinG";

		/*
		 * Rewards screen <screen> = rewS <event> = inV, clM, faQ
		 */
		public static final String REWARDS_INVITE = "rewSinV";

		public static final String REWARDS_CLAIM = "rewSclM";

		public static final String REWARDS_FAQ = "rewSfaQ";

		/*
		 * Help screen <screen> = helpS <event> = conT, faQ
		 */
		public static final String HELP_CONTACT = "helpSconT";

		public static final String HELP_FAQ = "helpSfaQ";

		/*
		 * FTUE
		 */
		public static final String CLICK = "click";

		public static final String GRID_6 = "grid6";

		public static final String ADD_FRIENDS_CLICK = "addFriends";

		public static final String ADD_UPDATES_CLICK = "addUpdates";

		public static final String INVITE_FTUE_FRIENDS_CLICK = "inviteFTUEFriendsClick";

		public static final String REMIND_FTUE_FRIENDS_CLICK = "remindFTUEFriendsClick";

		public static final String INVITE_SMS_CLICK = "inviteSMSClick";

		public static final String REMIND_SMS_CLICK = "remindSMSClick";

		public static final String INVITE_FTUE_UPDATES_CLICK = "inviteFTUEUpdatesClick";

		public static final String REMIND_FTUE_UPDATES_CLICK = "remindFTUEUpdatesClick";

		public static final String POST_UPDATE_FROM_CARD = "postUpdateFromCard";

		public static final String POST_UPDATE_FROM_TOP_BAR = "postUpdateFromTopBar";

		public static final String NEW_CHAT_FROM_GRID = "newChatFromGrid";

		public static final String NEW_CHAT_FROM_TOP_BAR = "newChatFromTopBar";

		public static final String INVITE_FROM_GRID = "inviteFromGrid";
		/*
		 * Invite keys
		 */
		public static final String INVITE_FRIENDS_FROM_POPUP_FREE_SMS = "inviteFriendsFromPopupFreeSMS";

		public static final String INVITE_FRIENDS_FROM_POPUP_REWARDS = "inviteFriendsFromPopupRewards";

		public static final String INVITE_SMS_SCREEN_FROM_INVITE = "inviteSMSScreenFromInvite";

		public static final String INVITE_SMS_SCREEN_FROM_CREDIT = "inviteSMSScreenFromCredit";

		public static final String SELECT_ALL_INVITE = "selectAllInvite";

		/*
		 * Sticker Ftue
		 */
		public static final String STICKER_FTUE_BTN_CLICK = "stickerFtueBtnClick";

		public static final String FTUE_TUTORIAL_STICKER_VIEWED = "ftueTutorialStickerViewed";

		public static final String FTUE_TUTORIAL_CBG_VIEWED = "ftueTutorialCbgViewed";

		public static final String START_HIKING = "startHiking";

		public static final String QUICK_SETUP_CLICK = "quickSetupClick";

		public static final String STEALTH_FTUE_DONE = "stlthFtueDone";

		public static final String RESET_STEALTH_INIT = "resetStlthInit";

		public static final String RESET_STEALTH_CANCEL = "resetStlthCancel";

		public static final String EXIT_STEALTH_MODE = "exitStlthMode";

		public static final String FTUE_WELCOME_CARD_CLICKED = "ftueWelcomeCardClicked";

		public static final String FTUE_CARD_START_CHAT_CLICKED = "ftueCardStartChatClicked";

		public static final String FTUE_FAV_CARD_START_CHAT_CLICKED = "ftueFavCardStartChatClicked";

		public static final String FTUE_CARD_SEEL_ALL_CLICKED = "ftueCardSeeAllClicked";

		public static final String FTUE_FAV_CARD_SEEL_ALL_CLICKED = "ftueFavCardSeeAllClicked";

		public static final String FIRST_OFFLINE_TIP_CLICKED = "firstOfflineTipClicked";

		public static final String SECOND_OFFLINE_TIP_CLICKED = "secondOfflineTipClicked";

		public static final String SMS_POPUP_ALWAYS_CLICKED = "smsPopupAlwaysClicked";

		public static final String SMS_POPUP_JUST_ONCE_CLICKED = "smsPopupJustOnceClicked";

		public static final String SMS_POPUP_REGULAR_CHECKED = "smsPopupRegularChecked";

		public static final String FTUE_CARD_LAST_SEEN_CLICKED = "ftueCardLastSeenClicked";

		public static final String FTUE_CARD_GROUP_CLICKED = "ftueCardGroupClicked";

		public static final String FTUE_CARD_HIDDEN_MODE_CLICKED = "ftueCardHiddenModeClicked";

		public static final String WATS_APP_INVITE = "inv_wa";

		public static final String FTUE_CARD_INVITE_CLICKED = "ftueCardInviteClicked";

		public static final String SHOW_TIMELINE_TOP_BAR = "showTimelineTopBar";

		public static final String FAVOURITE_FROM_OVERFLOW = "favoriteFromOverflow";

		public static final String STATUS_UPDATE_FROM_OVERFLOW = "statusUpdateFromOverflow";

		public static final String FTUE_CARD_HIKE_OFFLINE_CLICKED = "ftueCardHikeOfflineClicked";

		public static final String PIN_POSTED_VIA_ICON = "pinPostedViaIcon";

		public static final String PIN_POSTED_VIA_HASH_PIN = "pinPostedViaHashPin";

		public static final String PIN_HISTORY_VIA_MENU = "pinHistoryViaMenu";

		public static final String PIN_HISTORY_VIA_PIN_CLICK = "pinHistoryViaPinClick";

		public static final String STICKER_BTN_CLICKED = "sBnc";

        public static final String EMOTICON_BTN_CLICKED = "eBnc";

        public static final String EMOTICON_SENT = "eSnt";

        public static final String EMOTICON_DATA = "eD";

        public static final String EMOTICON_NAME = "eName";

        public static final String EMOTICON_COUNT = "eCnt";

		public static final String STKR_SHOP_BTN_CLICKED = "shopBtnClicked";

		public static final String STKR_SHOP_BTN_CLICKED_FROM_RECOMMENDATION_FTUE = "ftshpck";

		public static final String STICKER_SETTING_BTN_CLICKED = "stickerSettingBtnClicked";

		public static final String STICKER_RECOMMENDATION_PANEL_SETTINGS_BTN_CLICKED = "srps";

		public static final String STICKER_RECOMMENDATION_MANUAL_SETTING_STATE = "srMTs";

		public static final String STICKER_RECOMMENDATION_AUTOPOPUP_SETTING_STATE = "srATs";

		public static final String STICKER_RECOMMENDATION_REJECTION_KEY = "srCrP";

		public static final String STICKER_RECOMMENDATION_FTUE1_REJECTION_KEY = "srCrFt1";

		public static final String STICKER_RECOMMENDATION_FTUE2_REJECTION_KEY = "srCrFt2";

		public static final String STICKER_RECOMMENDATION_SELECTION_KEY = "acID";

		public static final String STICKER_RECOMMENDATION_ACCURACY_INDEX_KEY = "srAI";

		public static final String STICKER_RECOMMENDATION_REBALANCING_SUMMERIZATION = "srRS";

		public static final String STICKER_FOLDER_ERROR = "stFEr";

		public static final String STICKER_ERROR = "stEr";

		public static final String PACK_DATA_ANALYTIC_EVENT = "pckD";

		public static final String UPDATE_ALL_CONFIRM_CLICKED = "updateAllConfirmClicked";

		public static final String UPDATE_ALL_CANCEL_CLICKED = "updateAllCancelClicked";

		public static final String STICKER_CHECK_BOX_CLICKED = "stickerChkBoxClicked";

		public static final String STICKER_UNCHECK_BOX_CLICKED = "stickerUnchkBoxClicked";

		public static final String SETTING_CLICKED = "stgMS";

		public static final String PRIVACY_SETTING_CLICKED = "psMS";

		public static final String LS_SETTING_CLICKED = "psLS";

		public static final String LS_EVERYONE_CLICKED = "psLS_E";

		public static final String LS_MY_CONTACTS_CLICKED = "psLS_M";

		public static final String LS_FAVOURITES_CLICKED = "psLS_F";

		public static final String LS_NOBODY_CLICKED = "psLS_N";

		public static final String MANAGE_FAV_LIST_SETTING = "psMFav";

		public static final String STICKER_SETTINGS_REORDER_CLICKED = "ssReorderClick";

		public static final String STICKER_SETTINGS_DELETE_CLICKED = "ssDeleteClick";

		public static final String STICKER_SETTINGS_HIDE_CLICKED = "ssHideClick";

		public static final String STICKER_SETTINGS_UPDATE_CLICKED = "ssUpdateClick";

		public static final String STICKER_PACK_HIDE = "stPkHide";

		public static final String PACK_DELETE_CLICKED = "pkDelClick";

		public static final String DELETE_POSITIVE_CLICKED = "delPosClick";

		public static final String DELETE_NEGATIVE_CLICKED = "delNegClick";

		public static final String PACK_DELETE_SUCCESS = "pkDelSuccess";

		public static final String STICKER_PACK_UPDATE = "stPkUpdate";

		public static final String STICKER_PACK_REORDERED = "stPkReorder";

		/*
		 * Settings screen <screen> = settingsS <event> = notifNUJEnabled, notifH2OEnabled, notifNUJDisabled, notifH2ODisabled
		 */
		public static final String SETTINGS_NOTIFICATION_H2O_ON = "settingsSNotifH2OEnabled";

		public static final String SETTINGS_NOTIFICATION_H2O_OFF = "settingsSNotifH2ODisabled";

		public static final String SETTINGS_NOTIFICATION_NUJ_ON = "settingsSNotifNUJEnabled";

		public static final String SETTINGS_NOTIFICATION_NUJ_OFF = "settingsSNotifNUJDisabled";

		/*
		 * HikeSharedFiles Activity Screen = sharedMediaS<event>
		 */
		public static final String OPEN_THUMBNAIL_VIA_GALLERY = "sharedMediaSthumbnailClick";

		/*
		 * Image selection from gallery event.
		 */
		public static final String GALLERY_SELECTION = "gallery";

		/*
		 * File transfer success and failure event.
		 */
		public static final String FILE_TRANSFER_STATUS = "ftStatus";

		/*
		 * Atomic tips click events
		 */
		public static final String ATOMIC_FAVOURITES_TIP_CLICKED = "atomicFavTClick";

		public static final String ATOMIC_INVITE_TIP_CLICKED = "atomicInviteTClick";

		public static final String ATOMIC_PROFILE_PIC_TIP_CLICKED = "atomicProPicTClick";

		public static final String ATOMIC_STATUS_TIP_CLICKED = "atomicStatusTClick";

		public static final String ATOMIC_HTTP_TIP_CLICKED = "atomicHttpTClick";

		public static final String ATOMIC_APP_TIP_SETTINGS_CLICKED = "atomicAppSttngsTClick";

		public static final String ATOMIC_APP_TIP_SETTINGS_NOTIF_CLICKED = "atomicAppSttngsNotifTClick";

		public static final String ATOMIC_APP_TIP_SETTINGS_PRIVACY_CLICKED = "atomicAppSttngsPrivTClick";

		public static final String ATOMIC_APP_TIP_SETTINGS_SMS_CLICKED = "atomicAppSttngsSmsTClick";

		public static final String ATOMIC_APP_TIP_SETTINGS_MEDIA_CLICKED = "atomicAppSttngsMediaTClick";

		public static final String ATOMIC_APP_TIP_INVITE_FREE_SMS_CLICKED = "atomicAppInvFreeSmsTClick";

		public static final String ATOMIC_APP_TIP_INVITE_WHATSAPP_CLICKED = "atomicAppInvWaTClick";

		public static final String ATOMIC_APP_TIP_TIMELINE_CLICKED = "atomicAppTmlineTClick";

		public static final String ATOMIC_APP_TIP_HIKE_EXTRA_CLICKED = "atomicAppHikeExtraClick";

		public static final String ATOMIC_APP_TIP_HIKE_REWARDS_CLICKED = "atomicAppHikeRewardsClick";

		/*
		 * Account Backup Events
		 */
		public static final String BACKUP = "bckMnul";

		public static final String BACKUP_RESTORE = "bckRstr";

		public static final String BACKUP_RESTORE_RETRY = "rstrRtry";

		public static final String BACKUP_RESTORE_SKIP = "rstrSkp";

		/*
		 * Platform Events
		 */
		public static final String SDK_AUTH_DIALOG_VIEWED = "sdkAuthDialogViewed";

		public static final String SDK_AUTH_DIALOG_DECLINED = "sdkAuthDialogDeclined";

		public static final String SDK_AUTH_DIALOG_CONNECT = "sdkAuthDialogConnect";

		public static final String SDK_AUTH_FAILURE = "sdkAuthFailure";

		public static final String SDK_AUTH_SUCCESS = "sdkAuthSuccess";

		public static final String SDK_SEND_MESSAGE = "sdkSendMessage";

		public static final String SDK_INSTALL_HIKE_MESSENGER = "sdkDialogInstallHikeMessenger";

		public static final String SDK_INSTALL_HIKE_ACCEPT = "sdkInstallHikeAccept";

		public static final String SDK_INSTALL_HIKE_DECLINE = "sdkInstallHikeDeclined";

		public static final String SDK_DISCONNECT_APP = "sdkDisconnectApp";

		/*
		 * Content Card events
		 */
		public static final String CONTENT_CARD_TAPPED = "contentCardTapped";

		public static final String RETRY_NOTIFICATION_SENT = "retryNotificationSent";

		/*
		 * Festive Popup events
		 */
		public static final String FESTIVE_POPUP_WISH = "fstvepopwish";

		public static final String FESTIVE_POPUP_DISMISS = "fstvepopdsmss";

		/*
		 * VOIP events
		 */
		public static final String VOIP = "voip";

		public static final String VOIP_CALL_RATE_POPUP_SUBMIT = "vrmcSbmt";

		public static final String VOIP_CALL_CLICK = "cs";

		public static final String VOIP_CALL_ACCEPT = "ca";

		public static final String VOIP_CALL_REJECT = "cr";

		public static final String VOIP_CONNECTION_ESTABLISHED = "coest";

		public static final String VOIP_PARTNER_ANSWER_TIMEOUT = "cpat";

		public static final String VOIP_CALL_SPEAKER = "spk";

		public static final String VOIP_CALL_MUTE = "mut";

		public static final String VOIP_CALL_HOLD = "hld";

		public static final String VOIP_CALL_END = "ce";

		public static final String VOIP_CALL_DROP = "cd";

		public static final String VOIP_NATIVE_CALL_INTERRUPT = "tci";

		public static final String VOIP_CALL_RELAY = "cpur";

		public static final String VOIP_HANDSHAKE_COMPLETE = "vh";

		// GCM Events
		public static final String GCM_EXPIRED = "gcmexp";

		public static final String GCM_PUSH_ACK = "gcmpack";

		public static final String GCM_ANALYTICS_CONTEXT = "gcmst";

		public static final String VOIP_CONNECTION_FAILED = "connf";

		// nux Analytics Event

		public static final String NUX_INTRO_BTN = "nuxIB";

		public static final String NUX_INTRO_SKIP = "nuxIS";

		public static final String NUX_FRNSEL_NEXT = "nuxFSN";

		public static final String NUX_CUSMES_SEND = "nuxMS";

		public static final String NUX_INVITE_MORE = "nuxSF";

		public static final String NUX_REMIND = "nuxRF";

		public static final String NUX_VIEW_MORE = "nuxMr";

		public static final String NUX_TAP_CLAIM = "nuxClm";

		public static final String NUX_FOOTER_COM_NOR = "nuxC2N";

		public static final String NUX_FOOTER_NOR_EXP = "nuxN2E";

		public static final String NUX_FOOTER_NOR_COM = "nuxN2C";

		public static final String NUX_EXPANDED_COM = "nuxE2C";

		public static final String SETTINGS_ENTER_ON = "entersend";

		public static final String SETTINGS_ENTER_OFF = "enterline";

		public static final String SETTINGS_NUDGE_ON = "nudgeon";

		public static final String SETTINGS_NUDGE_OFF = "nudgeoff";

		public static final String UNCHECKED_NUDGE = "doubtap";

		// Photos 5.0 Events
		public static final String PHOTOS_FLOW_OPTION_CLICK = "phTake";

		public static final String PHOTOS_CAMERA_CLICK = "phCamC";

		public static final String PHOTOS_GALLERY_PICK = "phGalP";

		public static final String PHOTOS_FFC_PIC = "phFFC";

		public static final String PHOTOS_APPLIED_FILTER = "phFilA";

		public static final String PHOTOS_APPLIED_DOODLE = "phDodA";

		public static final String PHOTOS_SET_AS_DP = "phSetDP";

		public static final String PHOTOS_SEND_TO = "phSend";

		public static final String PHOTOS_UNABLE_TO_LOAD = "phImgNotLoad";

		public static final String TAP_EDIT = "photo_filter";

		public static final String TAP_CROP = "photo_crop";

		public static final String TAP_CROP_ACCEPT = "photo_crop_ac";

		public static final String TAP_DELETE = "photo_dlt";

		public static final String TAP_ROTATE = "photo_rotate";

		public static final String EDIT_SEND = "edit_send";

		public static final String EDIT_SEND_FILTER = "flt";

		public static final String EDIT_SEND_NO_FILTER = "no_flt";

		public static final String MULSEL_SEND = "mlsel_send";

		public static final String MULSEL_IS_SHARE = "is_share";

		public static final String MULSEL_TIMELINE_SEL = "tl_sel";

		public static final String MULSEL_CONTACT_SEL = "conv_sel";


		// Broadcast
		public static final String NEW_BROADCAST_VIA_OVERFLOW = "nbcOf";

		public static final String BROADCAST_NEXT_MULTI_CONTACT = "bcnxt";

		public static final String BROADCAST_DONE = "bcdn";

		public static final String BROADCAST_SELECT_ALL_NEXT = "bcAll";

		// Home search events

		public static final String HOME_SEARCH = "hmSrch";

		public static final String CHAT_SEARCH = "chtSrch";

		public static final String CHAT_OVRFLW_ITEM = "chtOvrflwItem";

		public static final String DEVICE_ROOT = "root";

		public static final String ADDRESSBOOK_UPLOAD = "addressbookUpload";

		public static final String SEND_DEVICE_DETAILS = "sendDeviceDetails";

		public static final String GET_ACTIVE_NETWORK_INFO = "getActiveNetworkInfo";

		/**
		 * Activity Feed
		 */
		public static final String ACTIVITY_FEED_ACTIONBAR_CLICK = "tL_actf";

		public static final String ACTIVITY_FEED_ITEM_CLICKED = "tL_fdlst";

		public static final String FTUE_SHOW_ME_CLICKED = "tL_FTsM";

		public static final String FTUE_GOT_IT_CLICKED = "tL_GiT";

		public static final String DPI = "dpi";
		
		/**
		 * Timelnie
		 */
		public static final String TIMELINE_OPEN = "tLO";
		
		public static final String TIMELINE_WITH_RED_DOT = "wr";
		
		public static final String TIMELINE_SUMMARY_OPEN = "tL_OpUS";
		
		public static final String TIMELINE_SUMMARY_LIKES_DIALOG_OPEN = "tL_OpLc";
		
		public static final String TIMELINE_CARD_LONG_PRESS = "tL_LPclk";
		
		public static final String TIMELINE_LONG_PRESS_MESSAGE = "mu";
		
		public static final String TIMELINE_LONG_PRESS_DELETE = "du";
		
		public static final String TIMELINE_LONG_PRESS_COPY = "cu";
		
		public static final String TIMELINE_OVERFLOW_OPTIONS = "tL_ofLO";
		
		public static final String TIMELINE_OVERFLOW_OPTION_CLEAR = "clt";
		
		public static final String TIMELINE_OVERFLOW_OPTION_MY_PROFILE = "myp";
		
		public static final String TIMELINE_OVERFLOW_OPTION_FAV = "fav";

		public static final String APP_LANGUAGE_DIALOG_OPEN_EVENT = "app_lng_dlg";
		
		public static final String APP_LANGUAGE_FTUE_SHOWN_EVENT = "app_lng_ftue";
		
		public static final String APP_LANGUAGE_CHANGED_EVENT = "ap_l_chg";

		public static final Object APP_FTUE_DONE_BTN = "ap_ft_d";

		public static final String HIKE_DIRECT_OVRFL_CLK = "hike_dir";

		public static final String NET_INFO_MOBILE = "mobile";

		public static final String NET_INFO_WIFI = "wifi";

		public static final String NET_INFO = "none";

		public static final String KEYBOARD_EXIT_UI_CLOSE_BUTTON = "kec";

		public static final String KEYBOARD_EXIT_UI_OPEN_KEYBOARD = "kek";

		public static final String KEYBOARD_EXIT_UI_PLAYSTORE_BUTTON = "kep";

		public static final String WT_RECORDING_CANCELLED_BY_USER = "wtcan";

	}

	public static final class Toast
	{
		public static final String TOAST_MESSAGE = "t_msg";
		
		public static final String TOAST_GRAVITY = "t_grv";
		
		public static final String TOAST_DURATION = "t_dur";
	}
	
	public static final class MqttMessageTypes
	{
		public static final String AUTO_APK = "atapk";
		
		public static final String MESSAGE_READ = "mr";

		public static final String NEW_MESSAGE_READ = "nmr";

		public static final String MESSAGE = "m";

		public static final String SMS_CREDITS = "sc";

		public static final String DELIVERY_REPORT = "dr";

		public static final String USER_JOINED = "uj";

		public static final String USER_LEFT = "ul";

		public static final String START_TYPING = "st";

		public static final String END_TYPING = "et";

		public static final String INVITE = "i";

		public static final String ICON = "ic";

		public static final String INVITE_INFO = "ii";

		public static final String GROUP_CHAT_JOIN = "gcj";

		public static final String GROUP_ADMIN_UPDATE = "gaa";

		public static final String GROUP_SETTINGS_CHANGE = "gsc";

		public static final String GROUP_CHAT_LEAVE = "gcl";

		public static final String GROUP_CHAT_END = "gce";

		public static final String GROUP_CHAT_NAME = "gcn";

		public static final String BROADCAST_LIST_JOIN = "gcj";

		public static final String BROADCAST_LIST_LEAVE = "gcl";

		public static final String BROADCAST_LIST_END = "gce";

		public static final String BROADCAST_LIST_NAME = "gcn";

		public static final String ANALYTICS_EVENT = "le";

		public static final String UPDATE_AVAILABLE = "ua";

		public static final String ACCOUNT_INFO = "ai";

		public static final String REQUEST_ACCOUNT_INFO = "rai";

		public static final String USER_OPT_IN = "uo";

		public static final String BLOCK_INTERNATIONAL_SMS = "bis";

		public static final String ADD_FAVORITE = "af";

		public static final String REMOVE_FAVORITE = "rf";

		public static final String MUTE = "mute";

		public static final String UNMUTE = "unmute";

		public static final String GROUP_CHAT_KICK = "gck";

		public static final String ACCOUNT_CONFIG = "ac";

		public static final String REWARDS = "rewards";

		public static final String GAMES = "games";

		public static final String DISPLAY_PIC = "dp";

		public static final String SYNC = "sync";

		public static final String STATUS_UPDATE = "su";

		public static final String ACTION = "action";

		public static final String DELETE_STATUS = "dsu";

		public static final String POSTPONE_FAVORITE = "pf";

		public static final String BATCH_STATUS_UPDATE = "bsu";

		public static final String FORCE_SMS = "fsms";

		public static final String STICKER = "stk";

		public static final String APP_STATE = "app";

		public static final String LAST_SEEN = "ls";

		public static final String SERVER_TIMESTAMP = "sts";

		public static final String REQUEST_SERVER_TIMESTAMP = "rsts";

		public static final String PROTIP = "pt";

		public static final String BULK_LAST_SEEN = "bls";

		public static final String FACEBOOK = "fb";

		public static final String UPDATE_PUSH = "update";

		public static final String APPLICATIONS_PUSH = "applications";

		public static final String MULTI_INVITE = "mi";

		public static final String CHAT_BACKGROUD = "cbg";

		public static final String GROUP_OWNER_CHANGE = "goc";

		public static final String REQUEST_DP = "rdp";

		public static final String STEALTH = "stlth";

		public static final String TOGGLE_STEALTH = "ts";

		public static final String POPUP = "popup"; // this is a generic popup
													// type

		public static final String BULK_MESSAGE = "bm";

		public static final String REMOVE_PIC = "icr";

		public static final String MULTIPLE_FORWARD = "mm";

		public static final String TIP = "tip";

		public static final String NUX = "nux";

		public static final String CREATE_MULTIPLE_BOTS = "cbot";

		public static final String REMOVE_MICRO_APP = "dmapp";

		public static final String NOTIFY_MICRO_APP_STATUS = "nmapp";

		public static final String DELETE_MULTIPLE_BOTS = "dbot";

		public static final String MICROAPP_DOWNLOAD = "mapp";

		public static final String PACKET_ECHO = "pecho";

		public static final String VOIP_SOCKET_INFO = "ve";

		public static final String VOIP_CALL_REQUEST = "vcr1";

		public static final String VOIP_CALL_REQUEST_RESPONSE = "vcr2";

		public static final String VOIP_CALL_RESPONSE_RESPONSE = "vcr3";

		public static final String VOIP_CALL_CANCELLED = "vcrj";

		/**
		 * VoIP data packet with QoS 0. This packet will either be delivered immediately or never.
		 */
		public static final String MESSAGE_VOIP_0 = "v0";

		/**
		 * VoIP data packet with QoS 1.
		 */
		public static final String MESSAGE_VOIP_1 = "v1";

		/**
		 * The person we are calling is on a compatible platform, but is using an old version of the client which does not support VoIP.
		 */
		public static final String VOIP_ERROR_CALLEE_INCOMPATIBLE_UPGRADABLE = "e0";

		/**
		 * The person we are calling is on a client that cannot be upgraded to support VoIP. For example, might be on iOS and we have no iOS client.
		 */
		public static final String VOIP_ERROR_CALLEE_INCOMPATIBLE_NOT_UPGRADABLE = "e1";

		/**
		 * The person you are calling has blocked you.
		 * This is unused. 
		 */
		public static final String VOIP_ERROR_CALLEE_HAS_BLOCKED_YOU = "e2";

		/**
		 * Trying to add a person on a VoIP conference who is on an older, unsupported build.
		 */
		public static final String VOIP_ERROR_CALLEE_DOES_NOT_SUPPORT_CONFERENCE = "e3";

		/**
		 * If you receive a packet of this subtype, it implies that the person you are calling is already on a call.
		 */
		public static final String VOIP_ERROR_ALREADY_IN_CALL = "mc";

		/**
		 * VoIP custom error message.
		 * The server will include the error text to display to the user.
		 * Usage (15 Apr, 2016) - To let a user know if the person they are calling is on a
		 * carrier that prohibits VoIP.
		 */
		public static final String VOIP_ERROR_CUSTOM_MESSAGE = "e4";

		public static final String VOIP_MSG_TYPE_CALL_SUMMARY = "vcs";

		public static final String VOIP_MSG_TYPE_MISSED_CALL_INCOMING = "vmci";

		public static final String VOIP_MSG_TYPE_MISSED_CALL_OUTGOING = "vmco";

		public static final String VOIP_MSG_HANG_UP = "vHangUp";

		public static final String VOIP_MSG_DECLINE = "vDecline";

		public static final String VOIP_MSG_ACCEPT = "vAccept";

		public static final String PRODUCT_POPUP = "productpopup";

		public static final String SESSION = "sess";

		public static final String GENERAL_EVENT_QOS_ONE = "ge1";

		public static final String GENERAL_EVENT_QOS_ZERO = "ge0";

		public static final String GENERAL_EVENT_PACKET_ZERO = "ge0";

		public static final String ACTIVITY_UPDATE = "ac_up";

		public static final String TIMELINE_PREFFERED_CONTACTS = "tlpc";
		
		public static final String HIKE_DIRECT_ANALYTICS = "hdle";
		
		public static final String PLATFORM_INFRA_CONFIG = "infc";
	}

	public static final class GeneralEventMessagesTypes
	{
		public static final String MESSAGE_EVENT = "me";
		
		public static final String OFFLINE = "offline";

		public static final String GENERAL_EVENT_DR = "dr";
	}

	
	public static final class SMSNative
	{
		/*
		 * SMS URIs
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://sms");

		public static final Uri INBOX_CONTENT_URI = Uri.withAppendedPath(CONTENT_URI, "inbox");

		public static final Uri SENTBOX_CONTENT_URI = Uri.withAppendedPath(CONTENT_URI, "sent");

		public static final String NUMBER = "address";

		public static final String DATE = "date";

		public static final String MESSAGE = "body";

		public static final String READ = "read";

	}

	public static final class SocialPostResponse
	{
		public static final String NO_TOKEN = "notoken";

		public static final String INVALID_TOKEN = "invalidtoken";

		public static final String FAILURE = "failure";

		public static final String SUCCESS = "success";
	}

	public static enum FTResult
	{
		SUCCESS, UPLOAD_FAILED, FILE_TOO_LARGE, READ_FAIL, DOWNLOAD_FAILED, CANCELLED, FILE_EXPIRED, PAUSED, SERVER_ERROR, FAILED_UNRECOVERABLE, CARD_UNMOUNT, NO_SD_CARD, UNKNOWN_SERVER_ERROR,FILE_SIZE_EXCEEDING
	}

	public static enum SMSSyncState
	{
		SUCCESSFUL, NO_CHANGE, UNSUCCESSFUL
	}

	public static enum EmoticonType
	{
		EMOTICON, STICKERS
	}

	public static final int[] INVITE_STRINGS = { R.string.native_sms_invite_1, R.string.native_sms_invite_2, R.string.native_sms_invite_3, R.string.native_sms_invite_4,
			R.string.native_sms_invite_5, R.string.native_sms_invite_6 };

	// TODO need to finalize this with AM
	public static final int FRIENDS_LIMIT_MAGIC_NUMBER = 8;

	public static final StatusMessageType[] STATUS_TYPE_LIST_TO_FETCH = { StatusMessageType.TEXT};

	public static enum WelcomeTutorial
	{
		INTRO_VIEWED, STICKER_VIEWED, CHAT_BG_VIEWED
	}

	/**
	 * while updating from sound/vibrate single pref to list pref (from app 2.9.0 to ) , we need to respect old setting set by user, so if are done this transition , we will set
	 * this key to true in preference
	 */
	public static final String PREFERENCE_TRANSITION_SOUND_VIB_TO_LIST = "soundVibTransitionDone";

	public static final long[] SHORT_VIB_PATTERN = new long[] { 0, 200, 100, 250 };

	public static final long[] LONG_VIB_PATTERN = new long[] { 0, 1000 };

	public static final int FTUE_HIKE_CONTACT_MIN_LIMIT = 3;

	public static final String PACKAGE_WATSAPP = "com.whatsapp";

	public static final long IMAGE_SIZE_SMALL = 80 * 1024;// Needs to be finalized after discussion

	public static final long IMAGE_SIZE_MEDIUM = 110 * 1024;// Needs to be finalized after discussion

	public static final String SERVER_CONFIG_IMAGE_SIZE_SMALL = "sc_img_sm";

	public static final String SERVER_CONFIG_IMAGE_SIZE_MEDIUM = "sc_img_med";

	public static final String WATSAPP_INVITE_MESSAGE_KEY = "wa_msg";

	public static final String WATSAPP_INVITE_ENABLED = "i_wa";

	public static final class ImageQuality
	{
		public static final int QUALITY_ORIGINAL = 1;

		public static final int QUALITY_MEDIUM = 2;

		public static final int QUALITY_SMALL = 3;

		public static final int QUALITY_DEFAULT = QUALITY_MEDIUM;

		public static final String IMAGE_QUALITY_ORIGINAL = "O";

		public static final String IMAGE_QUALITY_MEDIUM = "M";

		public static final String IMAGE_QUALITY_SMALL = "S";

		public static final String IMAGE_QUALITY_DEFAULT = IMAGE_QUALITY_SMALL;
	}

	public static final class PushType
	{

		public static final int loud = 2;

		public static final int silent = 1;

		public static final int none = 0;

	}

	public static final class UserJoinMsg
	{

		public static final String NOTIF_TITLE = "Ttl";

		public static final String NOTIF_TEXT = "Txt";

		public static final String PUSH_SETTING = "Typ";

		public static final String PERSIST_CHAT = "Cht";

		public static final boolean defaultPersistChat = false;

	}

	public static class MESSAGE_TYPE
	{
		public static final String MESSAGE_TYPE = "messageType";

		public static final int PLAIN_TEXT = 0;

		public static final int TEXT_PIN = 1;

		public static final int CONTENT = 2;

		public static final int WEB_CONTENT = 3;

		public static final int FORWARD_WEB_CONTENT = 4;
	}

	public static class HOME_ACTIVITY_OVERFLOW
	{
		public static final int CREDITS = 1;

		public static final int INVITE_FRIENDS = 2;

		public static final int HIKE_EXTRAS = 3;

		public static final int REWARDS = 4;

		public static final int SETTINGS = 5;

		public static final int NEW_GROUP = 6;

		public static final int TIMELINE = 7;

		public static final int STATUS = 8;

		public static final int LOGS = 9;

		public static final int NEW_BROADCAST = 10;
	}

	public static interface ConvMessagePacketKeys
	{

		public static final String CONTENT_TYPE = "c";

		public static final String PIN_TYPE = "pin";

		public static final String LOVE_ID = "loveID";

		public static final String WEB_CONTENT_TYPE = "wc";

		public static final String FORWARD_WEB_CONTENT_TYPE = "fwc";
	}

	public static class HASH_MESSAGE_TYPE
	{
		public static final int DEFAULT_MESSAGE = 0;

		public static final int HASH_PIN_MESSAGE = 1;
	}

	public static class REQUEST_BASE_URLS
	{
		public static final String HTTP_REQUEST_PROFILE_BASE_URL = "/account/profile/";
	}

	public static final String TEXT_PINS = "text_pins";

	public static final String EXTRA_CONV_ID = "conv_id";

	public static final int MAX_PIN_CONTENT_LINES_IN_HISTORY = 10;

	public static final String URL = "url";
	
	public static final String URLS = "urls";

	public static final String MEDIA_POSITION = "position";

	public static final String FROM_CHAT_THREAD = "ct";

	public static final String LARGE = "large";

	public static final int ATOMIC_APP_TIP_SETTINGS = 1;

	public static final int ATOMIC_APP_TIP_SETTINGS_NOTIF = 2;

	public static final int ATOMIC_APP_TIP_SETTINGS_MEDIA = 3;

	public static final int ATOMIC_APP_TIP_SETTINGS_SMS = 4;

	public static final int ATOMIC_APP_TIP_SETTINGS_PRIVACY = 5;

	public static final int ATOMIC_APP_TIP_TIMELINE = 6;

	public static final int ATOMIC_APP_TIP_INVITE_FREE_SMS = 8;

	public static final int ATOMIC_APP_TIP_INVITE_WATSAPP = 9;

	public static final int ATOMIC_APP_TIP_HIKE_EXTRA = 10;

	public static final int ATOMIC_APP_TIP_HIKE_REWARDS = 11;

	public static final int SHARED_MEDIA_TYPE = 1;

	public static enum STResult
	{
		SUCCESS, FILE_TOO_LARGE, READ_FAIL, DOWNLOAD_FAILED, CANCELLED, SERVER_ERROR, CARD_UNMOUNT, NO_SD_CARD, FILE_EXPIRED
	}

	public static final String PREVIEW_IMAGE = "previewimg";

	public static final String ENABLE_IMAGE = "enableimg";

	public static final String DISABLE_IMAGE = "disableimg";

	public static final String SIZE = "size";

	public static final int NORMAL_MESSAGE_TYPE = 0;

	public static final int MULTI_FORWARD_MESSAGE_TYPE = 1;

	public static final int SHARED_PIN_TYPE = 2;

	public static final int BROADCAST_MESSAGE_TYPE = 3;

	public static final int OFFLINE_MESSAGE_TYPE = 4;
	
	public static final String SHOWN_MULTI_FORWARD_TIP = "shownMultiForwardTip";

	public static final String VISIBLITY = "visibility";

	public static final String INDEX = "index";

	public static final String CAT_NAME = "name";

	public static final String DELIMETER = ":";

    public static final String SEPARATOR_ = "_";

	public static final String HIKE_EXTRAS_NAME = "hike_extras_name";

	public static final String HIKE_EXTRAS_URL = "hike_extras_url";

	public static final String REWARDS_NAME = "rewards_name";

	public static final String REWARDS_URL = "rewards_url";

	public static final String CALL_LOG_ANALYTICS = "cl";

	public static final String ADVERTSING_ID_ANALYTICS = "adv";

	public static final String APP_LOG_ANALYTICS = "al";

	public static final String LOCATION_LOG_ANALYTICS = "ll";

	public static final String FETCH_LOG_ANALYTICS = "gl";

	public static final String SESSION_LOG_TRACKING = "stl";

	public static final String ACCOUNT_LOG_ANALYTICS = "actl";

	public static final String PHONE_SPEC = "pl";

	public static final String SHOP = "shop";

	public static final String BADGE = "badge";
	
	public static final String DESCRIPTION = "desc";

	public static final String STICKER_LIST = "sticker_list";

	public static final String SIMILAR_PACKS = "similar_packs";

	public static final String AUTHOR = "author";

	public static final String COPYRIGHT = "copyright";

	public static final long DEFAULT_RETRY_NOTIF_TIME = 20 * 60 * 1000; // 20
																		// minutes

	public static final String RETRY_COUNT = "retryCount";

	public static final String REPLY_NOTIFICATION_RETRY_TIMER = "rnrt";

	public static final String REPLY_NOTIFICATION_RETRY_COUNT = "rnrc";

	// Intent send to register gcm before and after signup
	public static final String REGISTER_GCM_SIGNUP = "register_gcm_signup";

	public static final int REGISTEM_GCM_BEFORE_SIGNUP = 345;

	public static final int REGISTEM_GCM_AFTER_SIGNUP = 346;

	public static final String INCENTIVE_ID = "incentive_id";

	public static final String GCM_ID = "gcm_id";

	public static final String ADD_CATEGORY = "addCat";

	public static final int LED_DEFAULT_WHITE_COLOR = 0xffffffff;

	/**
	 * Any Change In this Value Should be double checked as this may coincide with any other color value
	 */

	public static final String GET_BULK_LAST_SEEN = "bls";

	public static final int LED_NONE_COLOR = -2;

	/**
	 * Any Change In this Value Should be double checked as this may coincide with any other color value
	 */

	public static long STOP_NOTIF_SOUND_TIME = 3000; // In milliseconds

	public static final String PLAY_NOTIFICATION = "notif";

	public static final String SILENT = "silent";

	public static final String LOUD = "loud";

	public static final String OFF = "off";

	public static final String ENABLE_DETAILED_HTTP_LOGGING = "edhl";

	public static final String ERROR_MESSAGE = "em";

	public static final String EXCEPTION_MESSAGE = "exm";

	public static enum PrivacyOptions
	{
		NOBODY, EVERYONE, FAVORITES, MY_CONTACTS
	}

	public static final class HikePhotos
	{
		public static final String CAMERA_ALLOW_GALLERY_KEY = "galleryKey";

		public static final String EDITOR_ALLOW_COMPRESSION_KEY = "compressKey";

		public static final String PHOTOS_FILTER_NAME_KEY = "phFilName";

		public static final String PHOTOS_DOODLE_COLOR_KEY = "phDodCol";

		public static final String PHOTOS_IS_FFC_MODE = "isFFC";

		public static final int GALLERY_PICKER_REQUEST = 2;

		public static final String FILENAME = "FilePath";

		public static final String DESTINATION_FILENAME = "Destination_FilePath";

		public static final String ORIG_FILE = "OrigFile";

		public static final int MAX_BRUSH_WIDTH = 48;

		public static final int Min_BRUSH_WIDTH = 8;

		public static final int DELTA_BRUSH_WIDTH = 10;

		public static final int FILTER_FRAGMENT_ID = 0;

		public static final int DOODLE_FRAGMENT_ID = 1;

		public static final int DEFAULT_BRUSH_WIDTH = 18;

		public static final int PREVIEW_BRUSH_WIDTH = 38;

		public static final int DEFAULT_BRUSH_COLOR = 0xFF000000;

		public static final int DEFAULT_RING_COLOR = 0x00FFFFFF;

		public static final int SELECTED_RING_COLOR = 0xFFFFFFFF;

		public static final String EMPTY_TAB_TITLE = "";

		public static final float TOUCH_TOLERANCE = 0;

		public static final int DEFAULT_FILTER_APPLY_PERCENTAGE = 100;

		public static final int DOODLE_PREVIEW_COLORS_BAR_HEIGHT = 70;

		public static final int DOODLE_SELECTED_RING_COLOR = 0x0019191A;

		public static final int PHOTOS_PAGER_PADDING = 11;

		public static final int PHOTOS_PAGER_FILTER_WEIGHT_SUM = 5147;

		public static final int PHOTOS_PAGER_DOODLE_WEIGHT_SUM = 10000;

		public static final String PHOTOS_ACTION_CODE = "photos_action_code";

		public static final String ONLY_PROFILE_UPDATE = "update_profile_pic_only";

		public static final int MAX_IMAGE_DIMEN = 1240;

		public static final int MODIFIED_MAX_IMAGE_DIMEN = 1540;

		public static final String HOME_ON_BACK_PRESS = "from_dp_upload";

		public static final int DEFAULT_IMAGE_SAVE_QUALITY = 80;
	}

	public static final String REARRANGE_CHAT = "rearrange_chat";

	public static final String UPDATE_UNREAD_COUNT = "uuc";

	public static final String CONTENT_ID = "content_id";

	public static final String TIMESTAMP_MILLIS = "msec";

	public static final String EVENT_TAG_SESSION = "sess";
	
	public static final String CAM_IMG_PREFIX = "CAM_";
	
	public static final String MESSAGE_PROCESS_TIME = "mpt";

	public static TypedArray DEFAULT_AVATAR_BG_COLOR_ARRAY = null;

	public static int DEFAULT_AVATARS[] = { R.drawable.avatar_bubblegum, R.drawable.avatar_apricot, R.drawable.avatar_carnation, R.drawable.avatar_light_gold,
			R.drawable.avatar_sky_blue };

	public static int DEFAULT_AVATAR_BG_COLORID[] = { R.color.avatar_color_apricot, R.color.avatar_color_bubblegum, R.color.avatar_color_carnation,
			R.color.avatar_color_light_gold, R.color.avatar_color_sky_blue };

	public static String DEFAULT_AVATAR_KEYS[] = { "avatar_buggle_gum", "avatar_apricot", "avatar_carnation", "avatar_light_gold", "avatar_sky_blue" };

	public static String IS_GROUP = "isGroup";

	public static final String URL_WHITELIST = "uwl";

	public static final String IN_HIKE_URL_WHITELIST = "iuwl";

	public static final String BROWSER_URL_WHITELIST = "buwl";

	public static final String ENABLED_WHITELISTED_FEATURE = "enabledWhitelisted";

	public static final String WHITELISTED_DOMAINS[] = new String[] { "hike.in" };

	public static final String BLACKLIST_DOMAIN_ANALYTICS = "blacklist";

	public static final int DEFAULT_MAX_REPLY_RETRY_NOTIF_COUNT = 3;

	public static final String NOTIFICATION_RETRY = "notif";

	public static class NotificationType
	{
		public static final int NORMALMSG1TO1 = 0;

		public static final int NORMALGC = 1;

		public static final int HIDDEN = 2;

		public static final int BOTMSG = 4;

		public static final int CHATTHEMECHNG = 5;

		public static final int STATUSUPDATE = 6;

		public static final int DPUPDATE = 7;

		public static final int NUJORRUJ = 8;

		public static final int FAVADD = 9;

		public static final int H2O = 10;

		public static final int OTHER = 11;

		public static final int ACTIVITYUPDATE = 12;

		public static final int IMAGE_POST = 13;
	}

	public static class GROUPS_TYPE
	{
		public static final int OLD_GROUPS = 0;

		public static final int MULTI_ADMIN = 1;
	}

	public static final String NOTIFICATION_RETRY_JSON = "notifretry";

	public static final String HIGHLIGHT_NLS_PERF = "nlsHighlightPerf";

	public static final String FLUSH = "flush";

	public static final String FORCE_USER = "fu";

	public static final String IS_ROOT = "is_root";

	public static final String PROB_NUM_TEXT_MSG = "p_txt";

	public static final String PROB_NUM_STICKER_MSG = "p_stk";

	public static final String PROB_NUM_MULTIMEDIA_MSG = "p_mul";

	public static final String PROB_NUM_HTTP_ANALYTICS = "p_http";

	public static final String MSG_REL = "rel_m";

	public static final String MSG_REL_UID = "track_id";

	public static final String MSG_REL_MSG_TYPE = "rel_m_type";

	public static final String PRIVATE_DATA = "pd";

	public static final String EXCEPTION = "exception";

	public static final String ENABLE_EXCEPTION_ANALYTIS = "enableExceptionAnalytics";

	public static final String PAYLOAD = "payload";

	public static final String HIKE_CONTACT_PICKER_RESULT = "contact_pick_result";

	public static final String CATEGORY_ID_LIST = "sId_list";

	public static final String HIKE_CONTACT_PICKER_RESULT_FOR_CONFERENCE = "contact_pick_result_for_conference";

	public static final String NOTIFIACTION_DELAY_GROUP = "gnt";

	public static final String NOTIFIACTION_DELAY_ONE_TO_ONE = "ont";

	public static final String KEYBOARD_CONFIGURATION = "kc";
	
	public static final int KEYBOARD_CONFIGURATION_OLD = 1;

	public static final int KEYBOARD_CONFIGURATION_NEW = 2;

	public static final String KPT_EXIT_SERVER_SWITCH = "kess";

	public static final String KPT_EXIT_SERVER_TEXT = "kest";

	public static final String KPT_EXIT_HEADING = "keh";

	public static final String KPT_EXIT_PHONE_BUTTON = "kepb";

	public static final String KPT_EXIT_GOOGLE_BUTTON = "kegb";

	public static final String GET = "get";

	public static final String HIKE_FILE_TYPE = "hft";

	public static final String FTUE_HIKEBOT_MSISDN = "+hike1+";

	public static final String PRIVACY_SETTINGS_CATEGORY = "privacySettingsCategory";

	public static final String KEY = "key";
	
	public static final String LIFE = "life";

	public static final String VALUE = "val";

	public static final String SUPER_COMPRESSED_IMG_SIZE = "c_img_size";

	public static final String NORMAL_IMG_SIZE = "n_img_size";

	public static final String DEFAULT_IMG_QUALITY_FOR_SMO = "d_q_smo";

	public static final String SHOW_TOAST_FOR_DEGRADING_QUALITY = "img_deg_toast";

	public static final String CONSUMED_FORWARDED_DATA = "consumed";

	public static final String CONTACT_UPDATE_WAIT_TIME = "contactUpdateWaitTime";

	public static final String DELETE_IC_ON_CONTACT_REMOVE = "deleteIcOnContactRemove";

	public static final String CONTACT_REMOVE_DUPLICATES_WHILE_SYNCING = "contactRemoveDuplicates";

	public static final String PACKS = "packs";

	public static final String STICKERS = "stkrs";

	public static final String IMAGE = "img";

	public static final String OTHER_EXCEPTION_LOGGING = "otherExLoging";

	public static final String HTTP_EXCEPTION_LOGGING = "httpExc";

	public static final String CONN_PROD_AREA_LOGGING = "connProdAreaLogs";

	public static final String GCM_PROD_AREA_LOGGING = "gcmProdAreaLogs";

	public static final String SERVER_CONFIGURABLE_GROUP_SETTING = "gse";

	public static final String MESSAGING = "messaging";

	public static final String DUPLICATE = "duplicate";

	public static final String MESSAGING_PROD_AREA_LOGGING = "msgingLogs";

	public static final String TAG_HEADLESS_IMAGE_DOWNLOAD_FRAGMENT = "headlessimage_down_fragment";

	public static final String TAG_HEADLESS_IMAGE_UPLOAD_FRAGMENT = "headlessimage_up_fragment";

	public static final String SERVER_CONFIG_DEFAULT_IMAGE_SAVE_QUALITY = "def_img_q";

	public static final String IMAGE_PATHS = "image-paths";

	public static final String EDITED_IMAGE_PATHS = "edited-image-paths";

	public static final String OFFLINE_MESSAGE_REQUEST = "omr";

	public static final String ENABLE_TIMELINE_FTUE = "tl_ftue";

	public static final String HISTORICAL_UPDATE = "hsu";

	public static final String INIT_CARD_SHOWN = "timeline_ftue_init_card_shown";

	public static final String INIT_CARD_ON_TOP = "init_card_on_top";

	public static final String EXIT_CARD_ON_TOP = "exit_card_on_top";

	public static final String EXIT_CARD_SHOWN = "timeline_ftue_exit_card_shown";

	public static final String TIMELINE_FTUE_CARD_TO_SHOW_COUNTER = "timeline_ftue_card_to_show_counter";

	public static final String ANY_TIMELINE_FTUE_FAV_CLICKED = "any_tl_ftue_clicked";

	public static final String TIMELINE_FTUE_MSISDN_LIST = "tl_ftue_msdn_list";

	public static final class SMS_SETTINGS
	{
		public static final String KEY_HIKE_OFFLINE = "hikeOffline";

		public static final String KEY_RECEIVE_SMS_PREF = "receiveSmsPref";

		public static final String FREE_SMS_PREF = "freeSmsPref";

		public static final String KEY_EARN_FREE_SMS = "earnFreeSms";

		public static final String KEY_INVITE_VIA_SMS = "inviteViaSms";

		public static final String HIKE_HIKE = "hike_hike";

		public static final String FREE_HIKE_TO_SMS_INDIA = "freeHike2SMSIndia";
	}

	public static final String NUMBER_OF_PACKS = "np";

	public static final String PACK_DATA = "pd";

	public static final String NOTIFICATIONS_PRIORITY = "npc";

	public static final String SEND_TIMESTAMP = "c";

	public static final String FT_HOST_IPS = "ftHostIps";

	public static final String HTTP_HOST_IPS = "httpHostIps";

	public static final String HTTP_HOST_PLATFORM_IPS = "httpHostPlfIps";

	public static final String SPECIAL_DAY_TRIGGER = "s_d_t";

	public static final Object OFFLINE_MESSAGE_REQUEST_CANCEL = "offreqcan";

	public static final String DP_IMAGE_SIZE = "dp_img_s";

	public static class IntentAction
	{
		public static final String ACTION_KEYBOARD_OPEN = "com.bsb.hike.action.keyboardopen";

		public static final String ACTION_KEYBOARD_CLOSED = "com.bsb.hike.action.keyboardclosed";
	}
	
	public static class MicroApp_Msisdn
	{
		public static final String HIKE_RECHARGE = "+hikerecharge+";
		
		public static final String HIKE_WALLET = "+hikewallet+";

	}
	


	public static class AutoApkDownload
	{
		public static final String NEW_APK_VERSION = "new_apk_version";

		public static final String UPDATE_FROM_DOWNLOADED_APK = "update_from_downloaded_apk";

		public static final String NEW_APK_SIZE = "new_apk_size";

		public static final String NEW_APK_JSON = "HFAPK";

		public static final String DOWNLOAD_APK_URL = "download_url";

		public static final String DOWNLOAD_APK_VERSION = "version";

		public static final String NEW_APK_TIP_JSON = "new_apk_tip_json";

		public static final String DOWNLOAD_APK_SIZE = "apk_size";
	}

	public static final String KEYBOARD_HEIGHT = "keyBoardHeight";

	public static final String RECOMMENDATION_SOURCE = "srs";

	public static final String TAGGED_PHRASE = "tg";

	public static final String TAP_WORD = "tap";

	public static final String ACCURACY = "ac";

	public static final String STICKER_ID = "stkId";

	public static final String CATEGORY_ID = "catId";

	public static final String STICKER_AUTO_RECOMMENDATION_ENABLED = "sare"; // boolean

	public static final String STICKER_AUTO_RECOMMENDATION_CONTINUOUS_REJECTION_COUNT_TO_TURNOFF = "sarcrc"; // integer

	public static final String STICKER_AUTO_RECOMMENDATION_REJECTION_PATTERN_COUNT_TO_TURNOFF = "sarrpc"; // integer

	public static final String STICKER_AUTO_RECOMMENDATION_CONTINUOUS_REJECTION_COUNT_TILL_NOW = "sarcrct"; // integer

	public static final String STICKER_AUTO_RECOMMENDATION_REJECTION_PATTERN_COUNT_TILL_NOW = "sarrpct"; // integer

	public static final String STICKER_UNDOWNLOADED_TAG_TRIGGER = "sudtt";

	public static final String CHAT_SEARCH_ENABLED = "cts_e";

	public static final String CALLBACK_ID = "callbackid";

	public static final String DEVICE_DETAILS = "dd";

	public static final String EXTRAS_BOT_MSISDN = "extras_bot_msisdn";

	public static final String REWARDS_BOT_MSISDN = "rewards_bot_msisdn";

	public static final String TIMELINE_LOGS = "tl_logs";
	
	public static final String TIMELINE_COUNT_LOGS = "tlc_logs";
	
	public static final String STICKER_SETTINGS = "stickerSettings";

	public static final String FORMAT_TIME_OF_THE_DAY = "HH:mm:ss.SSS";

	// Constants for module names of various features=======================================================================[[
	public static final String MODULE_STICKER_SEARCH = "stickersearch";

	// =======================================================================Constants for module names of various features]]

	// Constants for sticker search=========================================================================================[[
	public static final String STICKER_RECOMMENDATION_ENABLED = "sre"; // boolean

	public static final String STICKER_TAG_REFRESH_TIME_INTERVAL = "strt"; // long

	public static final String STICKER_RECOMMENDATION_DOWNLOAD_TAGS = "srdt"; // boolean

	public static final String STICKER_RECOMMENDATION_CONFIGURATION_DATA = "srcd"; // container key for all of following keys of this block

	public static final String STICKER_RECOMMENDATION_CONFIGURATION_MIN_VERSION_TO_APPLY = "srcdvm"; // minimum version (code), with which application will accept configuration data

	public static final String STICKER_TAG_REBALANCING_TRIGGER_TIME_STAMP = "strtt"; // container key for following 4 keys

	public static final String STICKER_DATA_HOUR = "HH"; // integer

	public static final String STICKER_DATA_MINUTE = "mm"; // integer

	public static final String STICKER_DATA_SECOND = "ss"; // integer

	public static final String STICKER_DATA_MILLI_SECOND = "SSS"; // integer

	public static final String STICKER_TAG_REGEX_SEPARATORS = "strs"; // initial to following 2 keys

	public static final String STICKER_TAG_REGEX_SEPARATORS_LATIN_REGULAR = "lr"; // regular expression for Latin default

	public static final String STICKER_TAG_REGEX_SEPARATORS_REGIONAL_REGULAR = "rr"; // regular expression for Regional default

	public static final String STICKER_DATA_TRENDING = "t";

	public static final String STICKER_DATA_LOCAL = "l";

	public static final String STICKER_DATA_GLOBAL = "g";

	public static final String STICKER_TAG_SUMMERY_INTERVAL = "stsi"; // initial to following 3 TLG keys

	public static final String STICKER_TAG_SUMMERY_INTERVAL_TRENDING = "stsi_t"; // long

	public static final String STICKER_TAG_SUMMERY_INTERVAL_LOCAL = "stsi_l"; // long

	public static final String STICKER_TAG_SUMMERY_INTERVAL_GLOBAL = "stsi_g"; // long

	public static final String STICKER_TAG_MAX_FREQUENCY = "stmf"; // initial to following 3 TLG keys

	public static final String STICKER_TAG_MAX_FREQUENCY_TRENDING = "stmf_t"; // float

	public static final String STICKER_TAG_MAX_FREQUENCY_LOCAL = "stmf_l"; // float

	public static final String STICKER_TAG_MAX_FREQUENCY_GLOBAL = "stmf_g"; // float

	public static final String STICKER_SCORE_WEIGHTAGE = "ssw"; // initial to following 4 parameters keys

	public static final String STICKER_SCORE_WEIGHTAGE_MATCH_LATERAL = "ssw_ml"; // float

	public static final String STICKER_SCORE_WEIGHTAGE_EXACT_MATCH = "ssw_em"; // float

	public static final String STICKER_SCORE_WEIGHTAGE_FREQUENCY = "ssw_f"; // float

	public static final String STICKER_SCORE_WEIGHTAGE_CONTEXT_MOMENT = "ssw_cm"; // float

	public static final String STICKER_TAG_LIMIT_EXACT_MATCH = "stlem"; // float

	public static final String STICKER_SCORE_MARGINAL_FULL_MATCH_LATERAL = "ssmfml"; // float;

	public static final String STICKER_TAG_LIMIT_AUTO_CORRECTION = "stlac"; // float

	public static final String STICKER_FREQUENCY_RATIO = "sfr"; // initial to following 3 TLG keys

	public static final String STICKER_FREQUENCY_RATIO_TRENDING = "sfr_t"; // float

	public static final String STICKER_FREQUENCY_RATIO_LOCAL = "sfr_l"; // float

	public static final String STICKER_FREQUENCY_RATIO_GLOBAL = "sfr_g"; // float

	public static final String STICKER_TAG_MAXIMUM_SEARCH = "stms"; // initial to following 4 keys

	public static final String STICKER_TAG_MAXIMUM_SEARCH_TEXT_LIMIT = "stms_tl"; // integer

	public static final String STICKER_TAG_MAXIMUM_SEARCH_TEXT_LIMIT_BROKER = "stms_tlb"; // integer

	public static final String STIKCER_TAG_MAXIMUM_SEARCH_PHRASE_PERMUTATION_SIZE = "stms_pps"; // integer

	public static final String STICKER_TAG_MINIMUM_SEARCH_WORD_LENGTH_FOR_AUTO_CORRECTION = "stms_wlac"; // integer

	public static final String STICKER_TAG_MAXIMUM_SELECTION = "stmsl"; // initial to following 2 keys

	public static final String STICKER_TAG_MAXIMUM_SELECTION_RATIO_PER_SEARCH = "stmsl_srps"; // float

	public static final String STICKER_TAG_MAXIMUM_SELECTION_PER_STICKER = "stmsl_sps"; // integer

	public static final String STICKER_TAG_RETRY_ON_FAILED_LOCALLY = "strfl"; // integer

	public static final String STICKER_WAIT_TIME_SINGLE_CHAR_RECOMMENDATION = "swtscr"; // integer

	public static final String STICKER_SEARCH_BASE = "ssb"; // initial to following 4 keys

	public static final String STICKER_SEARCH_BASE_MAXIMUM_PRIMARY_TABLE_CAPACITY = "ssb_mptc"; // integer

	public static final String STICKER_SEARCH_BASE_THRESHOLD_PRIMARY_TABLE_CAPACITY_FRACTION = "ssb_tptcf"; // float

	public static final String STICKER_SEARCH_BASE_THRESHOLD_EXPANSION_COEFFICIENT = "ssb_tec"; // float

	public static final String STICKER_SEARCH_BASE_THRESHOLD_FORCED_SHRINK_COEFFICIENT = "ssb_tfsc"; // float

	public static final String STICKER_SEARCH_EVENT_TIME_STAMP = "ts";

	public static final String STICKER_SEARCH_AUTO_POPUP_DATA = "ad";

	public static final String STICKER_SEARCH_HAIGHLIGHT_WORD_DATA = "hd";

	public static final String STICKER_SEARCH_REBALANCING_MEMORY_STATUS = "ms";
	
	public static final String STICKER_SEARCH_REBALANCING_ROW_STATUS = "rs";

	// =========================================================================================Constants for sticker search]]

	public static final String PLATFORM_PACKET = "pt";
	
	public static final String TAG = "Tag";

	public static final String REFERRAL_EMAIL_TEXT = "ref_mail_txt";
	
	public static final String REFERRAL_OTHER_TEXT = "ref_oth_txt";
	
	public static final String ALL_STICKER_TAG_DOWNLOAD = "alstktd";

	public static final String NUDGE_SEND_COOLOFF_TIME = "nudge_cool_off";

	public static final String FT_LATENCY_LOGGING = "ft_latency";
	
	public static final String SHOW_NOTIFICATION = "sn";
	
	public static final String SHOW_HIGH_RES_IMAGE = "s_h_r";
	
	public static final String HIKE_DIRECT_UPDGRADE_PEER = "hdu";
	
	public static final String HIKE_DIRECT_UNSUPPORTED_PEER = "hdnu";
	
	public static final String ENABLE_GC_VIA_LINK_SHARING = "en_gc_ls";
	
	public static final String MENU_OPTION_FOR_GC_VIA_WA = "mo_gc_wa";
	
	public static final String MENU_OPTIONS_FOR_GC_VIA_OTHERS = "mo_gc_o";
	
	public static final String ENABLE_MENU_OPTION_FOR_GC_VIA_WA = "en_mo_gc_wa";
	
	public static final String ENABLE_MENU_OPTIONS_FOR_GC_VIA_OTHERS = "en_mo_gc_o";
	
	public static final String TEXT_FOR_GC_VIA_WA = "t_gc_wa";
	
	public static final String TEXT_FOR_GC_VIA_OTHERS = "t_gc_o";

	public static final String WA_GROUP_NUMBER = "wa_grp_number";
	
	public static final String SHARE_LINK_URL_FOR_GC = "share_link_url";

	public static final String CALLER_SHARED_PREF = "caller_shared_pref";

	public static final String IS_BLOCK = "is_block";

	public static final String BOT_TABLE_REFRESH = "btr";
	
	public static final String BOTS = HikePlatformConstants.BOTS;
	
	public static final String ADD_DISCOVERY_BOTS = "add_di_bot";
	
	public static final String ENABLE_BOT_DISCOVERY = "en_bot_di";
	
	public static final String GET_DISCOVERY_BOTS = "get_bots";
	
	public static final String BOTS_DISCOVERY_SECTION = "bds";
	
	public static final String ACTIVATE_STICKY_CALLER_PREF = "activateStickyCaller";

	public static final String SMS_CARD_ENABLE_PREF = "smsCardEnablePref"; 
	
	public static final String ENABLE_KNOWN_NUMBER_CARD_PREF = "knownContactEnablePref";

	public static final String NEW_CHAT_RED_DOT = "nc_dot";
	
	public static final String BADGE_COUNT_ENABLED="badgeCountEnabled";
	
	public static final String BADGECOUNTER="badgeCounter";

	public static final int GPS_STATUS_CHANGED = 1197;
	
	public static final int GPS_SWITCH_OFF = 1198;

	public static final String SHOW_GPS_DIALOG = "gps_dialog_show";

	public static final String KEYBOARD_LIST = "kbd";

	public static final String TIMESTAMP_2 = "timestamp";

	public static final String UNKNOWN_KEYBOARDS = "unknown_kbds";

	public static final float ONE_PERCENT_PROGRESS = 0.01f;

	public static final String FT_USE_APACHE_HTTP_CLIENT = "ft_apache_client";

	// ============================================================================================= LOCALIZATION SWITCHES

	public static final String LOCALIZATION_ENABLED = "local_e";

	// ============================================================================================= LOCALIZATION SWITCHES


	public static final String LANG_LIST_ORDER = "lang_list";

	public static final String PHONE_LANGUAGE = "ph_l";

	public static final String APP_LANGUAGE = "ap_l";
	
	public static final String LANG_ARRAY = "lan_array";

	public static final String UNSUPPORTED_LANG_TOAST_SHOWN = "uns_lang_toast_shown";

	public static final String CALLER_BLOKED_LIST_SYNCHED = "caller_block_list_synched";

	public static final String CALLER_BLOKED_LIST_SYNCHED_SIGNUP = "caller_block_list_synched_signup";

	public static final String APP_LANGUAGE_CHANGE_SOURCE = "ap_l_s";

	public static final String OFFSET = "offset";

	public static final String APP_LANG_CHANGED_DEL_ACC = "d_a";
	
	public static final String APP_LANG_CHANGED_SETTINGS = "stg";
	
	public static final String APP_LANG_CHANGED_FTUE = "ap_f";
	
	public static final String APP_LANG_CHANGED_SERVER_SWITCH = "serv";

	public static final String WHITE_SCREEN_FIX = "w_s_f";

	public static final String CHAT_OPENING_BENCHMARK = "chatOpeningBenchmark";
	
	public static final String CHAT_SCROLL_FETCH_MESSAGES_FROM_DB_BENCHMARK = "chatScrollMsgDBBenchmark";

	public static final String APP_OPENING_BENCHMARK = "appOpeningBenchmark";

	public static final String STICKER_FORCE_DOWNLOAD = "f_download";

	public static final String FORCED_RECENTS_PRESENT = "forced_recents";

	public static final String FORCED_RECENTS_LIST = "forced_recents_list";

    public static final String COMPOSE_SCREEN_OPENING_BENCHMARK = "composeOpeningBenchmark";

	public static final String SINGLE_STICKER_CDN = "sscdn";

	public static final String STICKER_PACK_CDN = "spcdn";

	public static final String STICKER_PACK_CDN_THRESHOLD = "spcdnth";

	public static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5MB

	public static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB

	public static final String MINI_STICKER_ENABLED = "m_stk_st";

	public static final String UNDOWNLOADED_DATA = "ud_d";

	public static final String ENABLE_AB_SYNC_CHANGE = "ab_sync_change";

	public static final String ENABLE_AB_SYNC_DEBUGING = "ab_sync_debug";

	public static final String NET_BLOCKED_STATE_ANALYTICS = "net_block_state";

	public static final String HIDE_DELETED_CONTACTS = "h_d_c";
	
	public static final String RESET_CHAT_KEY_TIP="reset_chat_key_tip";

	public static final String OPEN_COMPOSE_CHAT_ONE_TIME_TRIGGER = "openComposeChatOnSignup";

	public static final String MAX_RETRY_COUNT_MAPPS = "maxRetryMapps";

	public static final String DISPLAY_MINI_IN_CT = "d_m_ct";

	public static final String WIDTH = "width";

	public static final String HEIGHT = "height";

	public static final String STICKER_TYPE = "type";

    public static final String STICKER_IMAGE = "image";

    public static final String MINI_STICKER_IMAGE = "mini_image";

    public static final String TAGS = "tags";

    public static final String RECENTS = "recents";

    public static final String getMetadata = "md";

    public static final String START = "start";

    public static final String END = "end";

    public static final String RANK = "rank";

    public static final int MAX_DISK_CACHE_KEY_LENGTH = 115;

    public static final String MINI_KEY_PREFIX = "mini_";

	public static final String STICKER_SIZE = "stkr_size";

	public static final String CRASH_REPORTING_TOOL="cpt";

	public static final String ACRA="a";

	public static final String CRASHLYTICS="c";

	public static final class INTERCEPTS
	{
		public static final String ENABLE_INTERCEPTS = "enbl_intercepts";

		public static final String SHOW_INTERCEPTS = "show_intercepts";

		public static final String IMAGE = "image";

		public static final String VIDEO = "video";

		public static final String SCREENSHOTS = "sshot";

		public static final String SHOW_IMAGE_INTERCEPT = "show_image_intrcpt";

		public static final String SHOW_VIDEO_INTERCEPT = "show_video_intrcpt";

		public static final String SHOW_SCREENSHOT_INTERCEPT = "show_screenshot_intrcpt";

		public static final String ENABLE_IMAGE_INTERCEPT = "enableImageIntercept";

		public static final String ENABLE_VIDEO_INTERCEPT = "enableVideoIntercept";

		public static final String ENABLE_SCREENSHOT_INTERCEPT = "enableScreenshotIntercept";

		public static final String INTERCEPT_LOG = "intercept_log";

		public static final String INTENT_EXTRA_URI = "intentExtraUri";

		public static final String INTENT_EXTRA_TYPE = "intentExtraType";

		public static final String PREV_MEDIA_URI = "prev_media_uri";

	}

	public static final long ONE_DAY_MILLS = 24 * 60 * 60 * 1000L;

	public static final String SINGLE_STICKER= "sst";

	public static final String STICKER_PACK = "spc";

	public static final String LOG_EMOTICON_USAGE_SWITCH = "l_e_s";

	public static final class KPTConstants {

		public static final String KPT_LANGUAGE_DIR_NAME = "lang-dict";

		public static final String KPT_LANGUAGE_DIR_ROOT = "/" + KPT_LANGUAGE_DIR_NAME;

		public static final String SYSTEM_KEYBOARD_SELECTED = "systemKeyboardSelected";

	}

	public static final String SHOW_STICKER_PREVIEW = "show_sticker_preview";

	public static final int PACKAGE_MANAGER_INTENT_FLAG_MATCH_ALL = 0x00020000;

	public static final String PROB_ACTIVITY_OPEN="prob_act_op";

	public static final int DEFAULT_ACTIVITY_OPEN = 10;

	public static final String ACTIVITY_LATENT_AREA="scrlat";

	public static final String ORIENTATION_PORTRAIT="_p" ;

	public static final String ORIENTATION_LANDSCAPE="_l";

	public static final String SERVER_NAME_SETTING = "serverName";

	public static final String SERVER_GENDER_SETTING = "serverGender";

	public static final String SERVER_BIRTHDAY_DAY = "serverBirthdayDay";

	public static final String SERVER_BIRTHDAY_MONTH = "serverBirthdayMonth";

	public static final String SERVER_BIRTHDAY_YEAR = "serverBirthdayYear";

	public static final String DB_CORRUPT = "db_corrupt";

	public static final String JOURNAL_MODE = "journal_mode";

	public static final String SQLITE_VERSION = "sqlite_ver";

	public static final String JOURNAL_MODE_INDEX = "journal_mode_idx";

	public static final String LOG_SQLITE_PROPERTIES = "log_sqlite_props";

	public static class BackupRestore
	{
		public static final String OS = "os";

		public static final String FROM = "f";

		public static final String TIMESTAMP = "ts";

		public static final String VERSION = "ver";

		public static final String DATA = "d";

		public static final String VALUE = "v";

		public static final String DATA_TYPE = "dt";

		public static final String RUX_BACKUP_TS_PREF = "ruxbkupts";

		public static final String RUX_BACKUP_PENDING = "ruxbkuppending";

		public static final String KEY_SETTINGS = "settings";

		public static final String KEY_MOVED_STICKER_EXTERNAL = "movdstckrext";

	}

	public static final String FAV_TO_FRIENDS_MIGRATION = "f2f_mig";

	public static final String IS_NEW_USER = "is_new_user";

	public static final int DEFAULT_FRIENDS_FTUE_COUNT = 5;

	public static final String FTUE_FRIENDS_COUNT = "friends_ftue_count";

	public static final String FRIENDS_SYSTEM_MESSAGE = "frn_sys_msg";

	public static final class SPACE_MANAGER
	{
		public static final String NOTIFY_DISK_SPACE_USAGE = "nspusg";

		public static final String DISK_SPACE_INFO = "spcinf";

		public static final String MAP_DIRECTORY = "mapdir";

		public static final String DIRECTORY_LIST = "dirlist";

		public static final String DIRECTORY_PATH = "dir_p";

		public static final String DIRECTORY_SIZE = "dir_s";

		public static final String DIRECTORY_TYPE = "dir_t";
	}

	public static final String NUM_ROWS_INITIALLY_VISIBLE = "num_rows_iv";

	public static final String DISK_CACHE_SIZE = "disk_cache_size";

	public static final String HIKE_CUSTOM_PHONE_TYPE = "HIKE";

	public static final class AddFriendSources
	{
		public static final String CHAT_FTUE = "chat_ftue_screen";

		public static final String CHAT_ADD_FRIEND = "chat_add_friend_Screen";

		public static final String FRIENDS_SCREEN = "friends_screen";

		public static final String PROFILE_SCREEN = "profile_screen";

		public static final String TIMELINE_FTUE_SCREEN = "timeline_ftue_screen";

		public static final String FORWARD_SCREEN = "fwd_screen";

		public static final String UNKNOWN = "unknown";
	}

	public static final String LAST_SEEN_TEMP_PREF = "ls_temp";

	public static final String SHOW_RECOMMENDED_PACKS = "shw_rec_pcks";

    public static final String STICKER_DOWNLOAD_ATTEMPTED_SET = "s_d_f_s";

	public static final String POSITION = "pos";

	public static final String VIEW_ALL_CLICKED = "viewAllClicked";

	public static final String TIP_CTA = "tp_cta";

	public static final String TIP_CTA_LINK = "link";

	public static final String TIP_PRIORITY = "tp_prrt";

	public static final String ADD_HEADER = "addHeader";

	public static final String IS_ATOMIC_TIP = "isAtomicTip";
}
