package com.bsb.hike;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;

import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.chatHead.StickyCaller;
import com.bsb.hike.db.DbConversationListener;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeMqttPersistence;
import com.bsb.hike.localisation.LocalLanguageUtils;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.diskcache.Cache;
import com.bsb.hike.modules.diskcache.InternalCache;
import com.bsb.hike.modules.gcmnetworkmanager.HikeGcmNetworkMgr;
import com.bsb.hike.modules.httpmgr.HttpManager;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.notifications.HikeNotificationUtils;
import com.bsb.hike.notifications.ToastListener;
import com.bsb.hike.offline.OfflineConstants;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUIDFetch;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.productpopup.AtomicTipManager;
import com.bsb.hike.productpopup.ProductInfoManager;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.service.RegisterToGCMTrigger;
import com.bsb.hike.service.SendGCMIdToServerTrigger;
import com.bsb.hike.smartcache.HikeLruCache;
import com.bsb.hike.smartcache.HikeLruCache.ImageCacheParams;
import com.bsb.hike.ui.CustomTabsHelper;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ActivityTimeLogger;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.hike.abtest.ABTest;
import com.crashlytics.android.Crashlytics;
import com.twinprime.TwinPrimeSDK.TwinPrimeSDK;

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.ReportField;
import org.acra.annotation.ReportsCrashes;
import org.acra.collector.CrashReportData;
import org.acra.sender.HttpSender;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;
import org.acra.util.HttpRequest;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.fabric.sdk.android.Fabric;


//https://github.com/ACRA/acra/wiki/Backends
@ReportsCrashes(customReportContent = { ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.PHONE_MODEL, ReportField.BRAND, ReportField.PRODUCT,
		ReportField.ANDROID_VERSION, ReportField.STACK_TRACE, ReportField.USER_APP_START_DATE, ReportField.USER_CRASH_DATE })
public class HikeMessengerApp extends MultiDexApplication implements HikePubSub.Listener
{
	public static enum CurrentState
	{
		OPENED, RESUMED, BACKGROUNDED, CLOSED, NEW_ACTIVITY, BACK_PRESSED, NEW_ACTIVITY_IN_BG, OLD_ACTIVITY, NEW_ACTIVITY_INTERNAL
	}

	public static final String DEFAULT_SETTINGS_PREF = "com.bsb.hike_preferences";

	public static final String ACCOUNT_SETTINGS = "accountsettings";

	public static final String VOIP_SETTINGS = "voipsettings";

	public static final String VOIP_AUDIO_GAIN = "voipaudiogain";

	public static final String VOIP_BITRATE_2G = "vb2g";

	public static final String VOIP_BITRATE_3G = "vb3g";

	public static final String VOIP_BITRATE_WIFI = "vbw";

	public static final String MSISDN_SETTING = "msisdn";

	public static final String CARRIER_SETTING = "carrier";

	public static final String NAME_SETTING = "name";

	public static final String TOKEN_SETTING = "token";

	public static final String MESSAGES_SETTING = "messageid";

	public static final String UID_SETTING = "uid";

	public static final String BACKUP_TOKEN_SETTING = "backup_token";

	public static final String PLATFORM_UID_SETTING = "platformUID";

	public static final String PLATFORM_TOKEN_SETTING = "platformToken";
	
	public static final String PLATFORM_AUTH_TOKEN = "platformAuthToken";
	
	public static final String PLATFORM_AUTH_TOKEN_EXPIRY = "platformAuthTokenExpiry";

	public static final String ANONYMOUS_NAME_SETTING = "anonymousName";

	public static final String RESTORE_ACCOUNT_SETTING = "restore";

	public static final String SIGNUP_COMPLETE = "signup_complete";

	public static final String RESTORING_BACKUP = "restoring_backup";

	public static final String UPDATE_SETTING = "update";

	public static final String ANALYTICS = "analytics";

	public static final String REFERRAL = "referral";

	public static final String ADDRESS_BOOK_SCANNED = "abscanned";

	public static final String CONTACT_LIST_EMPTY = "contactlistempty";

	public static final String SMS_SETTING = "smscredits";

	public static final String ACCEPT_TERMS = "acceptterms";

	public static final String CONNECTED_ONCE = "connectedonce";

	public static final String MESSAGES_LIST_TOOLTIP_DISMISSED = "messageslist_tooltip";

	public static final String SPLASH_SEEN = "splashseen";

	public static final String INVITE_TOOLTIP_DISMISSED = "inviteToolTip";

	public static final String EMAIL = "email";

	public static final String CHAT_INVITE_TOOL_TIP_DISMISSED = "chatInviteToolTipDismissed";

	public static final String INVITED = "invited";

	public static final String INVITED_JOINED = "invitedJoined";

	public static final String CHAT_GROUP_INFO_TOOL_TIP_DISMISSED = "chatGroupInfoToolTipDismissed";

	public static final String NUM_TIMES_HOME_SCREEN = "numTimesHomeScreen";

	public static final String NUM_TIMES_CHAT_THREAD_INVITE = "numTimesChatThreadInvite";

	public static final String NUM_TIMES_CHAT_THREAD_GROUP = "numTimesChatThreadGroup";

	public static final String NUM_TIMES_INVITE = "numTimesInvite";

	public static final String SHOW_CREDIT_SCREEN = "showCreditScreen";

	public static final String CONTACT_EXTRA_INFO_SYNCED = "contactExtraInfoSynced2";

	public static final String SHOWN_TUTORIAL = "showTutorial";

	public static final String SHOW_GROUP_CHAT_TOOL_TIP = "showGroupChatToolTip";

	public static final String TOTAL_CREDITS_PER_MONTH = HikeConstants.TOTAL_CREDITS_PER_MONTH;

	public static final String PRODUCTION = "production";

	public static final String PRODUCTION_HOST_TOGGLE = "productionHostToggle";

	public static final String CUSTOM_MQTT_HOST = "cmqttho";

	public static final String CUSTOM_MQTT_PORT = "cmmqttpo";

	public static final String CUSTOM_HTTP_HOST = "cmhttpho";

	public static final String CUSTOM_HTTP_PORT = "chttppo";

	public static final String COUNTRY_CODE = "countryCode";

	public static final String FILE_PATH = "filePath";

	public static final String FILE_PATHS = "multi_filepaths";

	public static final String TEMP_NAME = "tempName";

	public static final String TEMP_NUM = "tempNum";

	public static final String TEMP_COUNTRY_CODE = "tempCountryCode";

	public static final String GCM_ID_SENT_PRELOAD = "gcm_id_sent_preload";

	public static final String GCM_ID_SENT = "gcmIdSent";

	public static final String BLOCK_NOTIFICATIONS = "blockNotification";

	public static final String DP_CHANGE_STATUS_ID = "dpstatusid";

	private static final boolean TEST = false; // TODO:: test flag only : turn
												// OFF for Production

	/*
	 * Setting name for the day the was logged on fiksu for "First message sent in day"
	 */
	public static final String DAY_RECORDED = "dayRecorded";

	public static final String LAST_BACK_OFF_TIME = "lastBackOffTime";

	public static final String LAST_BACK_OFF_TIME_USER_LOGS = "lastBackOffTimeUserLogs";

	public static final String FACEBOOK_TOKEN = "facebookToken";

	public static final String FACEBOOK_TOKEN_EXPIRES = "facebookTokenExpires";

	public static final String FACEBOOK_USER_ID = "facebookUserId";

	public static final String FACEBOOK_AUTH_COMPLETE = "facebookAuthComplete";

	// public static final String TWITTER_TOKEN = "twitterToken";

	// public static final String TWITTER_TOKEN_SECRET = "twitterTokenSecret";

	// public static final String TWITTER_AUTH_COMPLETE = "twitterAuthComplete";

    public static final int DEFAULT_SEND_ANALYTICS_TIME_HOUR = 0;

    public static final String DAILY_ANALYTICS_ALARM_STATUS = "dailyAnalyticsAlarmStatus";

	public static final String MSISDN_ENTERED = "msisdnEntered";

	public static final String BROKER_HOST = "brokerHost";

	public static final String BROKER_PORT = "brokerPort";

	public static final String FAVORITES_INTRO_SHOWN = "favoritesIntroShown";

	public static final String NUDGE_INTRO_SHOWN = "nudgeIntroShown";

	public static final String REWARDS_TOKEN = "rewardsToken";

	public static final String SHOW_REWARDS = "showRewards";

	public static final String TALK_TIME = "talkTime";

	public static final String GAMES_TOKEN = "gamesToken";

	public static final String SHOW_GAMES = "showGames";

	public static final String GCK_SHOWN = "gckShown";

	public static final String ADD_CONTACT_SHOWN = "addContactShown";

	public static final String LAST_STATUS = "lastStatus";

	public static final String LAST_MOOD = "lastMood";

	public static final String INTRO_DONE = "introDone";

	public static final String JUST_SIGNED_UP = "justSignedUp";

	public static final String INVITED_NUMBERS = "invitedNumbers";

	public static final String UNSEEN_STATUS_COUNT = "unseenStatusCount";

	public static final String UNSEEN_USER_STATUS_COUNT = "unseenUserStatusCount";

	public static final String USER_TIMELINE_ACTIVITY_COUNT = "usertimelineactivitycount";

	public static final String BATCH_STATUS_NOTIFICATION_VALUES = "batchStatusNotificationValues";

	public static final String USER_JOIN_TIME = "userJoinTime";

	public static final String DEVICE_DETAILS_SENT = "deviceDetailsSent";

	public static final String LAST_BACK_OFF_TIME_DEV_DETAILS = "lastBackOffTimeDevDetails";

	public static final String SHOW_CRICKET_MOODS = "showCricketMoods";

	public static final String FRIEND_INTRO_SHOWN = "friendIntroShown";

	public static final String STATUS_NOTIFICATION_SETTING = "statusNotificationSetting";

	public static final String STATUS_IDS = "statusIds";

	public static final String SHOWN_SMS_CLIENT_POPUP = "shownSMSClientPopup";

	public static final String SHOWN_SMS_SYNC_POPUP = "shownSMSSyncPopup";

	public static final String SERVER_TIME_OFFSET = "serverTimeOffset";

	public static final String SERVER_TIME_OFFSET_MSEC = "serverTimeOffsetInMsec";

	public static final String SHOWN_EMOTICON_TIP = "shownEmoticonTip1";

	public static final String SHOWN_MOODS_TIP = "shownMoodsTip1";

	public static final String SHOWN_WALKIE_TALKIE_TIP = "shownWalkieTalkieTip";

	public static final String SHOWN_LAST_SEEN_TIP = "shownLastSeenTip";

	public static final String PROTIP_WAIT_TIME = "protipWaitTime";

	public static final String CURRENT_PROTIP = "currentProtip";

	public static final String SHOWN_NATIVE_SMS_INVITE_POPUP = "shownNativeSmsInvitePopup";

	public static final String BUTTONS_OVERLAY_SHOWN = "buttonsOverlayShown";

	public static final String SHOWN_FRIENDS_TUTORIAL = "shownFriendsTutorial";

	public static final String SHOWN_NATIVE_INFO_POPUP = "shownNativeInfoPopup";

	public static final String INVITED_FACEBOOK_FRIENDS_IDS = "invitedFacebookFriendsIds";

	public static final String SERVER_RECOMMENDED_CONTACTS = "serverRecommendedContacts";

	public static final String FIRST_VIEW_FTUE_LIST_TIMESTAMP = "firstViewFtueListTimestamp";

	public static final String HIDE_FTUE_SUGGESTIONS = "hideFtueSuggestions";

	public static final String BIRTHDAY_DAY = "birthdayDay";

	public static final String BIRTHDAY_MONTH = "birthdayMonth";

	public static final String BIRTHDAY_YEAR = "birthdayYear";

	public static final String UPGRADE_RAI_SENT = "upgradeRaiSent";

	public static final String CURRENT_APP_VERSION = "currentAppVersion";

	public static final String SEND_NATIVE_INVITE = "sendNativeInvite";

	public static final String SHOW_FREE_INVITE_POPUP = "showFreeInvitePopup";

	public static final String SET_FREE_INVITE_POPUP_PREF_FROM_AI = "setFreeInvitePopupPrefFromAi";

	public static final String FREE_INVITE_PREVIOUS_ID = "freeInvitePreviousId";

	public static final String FREE_INVITE_POPUP_HEADER = "freeInvitePopupHeader";

	public static final String FREE_INVITE_POPUP_BODY = "freeInvitePopupBody";

	public static final String FREE_INVITE_POPUP_DEFAULT_IMAGE = "freeInviteDefaultImage";

	public static final String SHOWN_CHAT_BG_FTUE = "shownChatBgFtue";

	public static final String SHOWN_CHAT_BG_TOOL_TIP = "shownChatBgToolTip";

	public static final String GREENBLUE_DETAILS_SENT = "gbDetailsSent";

	public static final String LAST_BACK_OFF_TIME_GREENBLUE = "lastBackOffTimeGb";

	public static final String SHOWN_VALENTINE_CHAT_BG_FTUE = "shownValentineChatBgFtue";

	public static final String SHOWN_NEW_CHAT_BG_TOOL_TIP = "shownNewChatBgToolTip";

	public static final String SHOWN_VALENTINE_NUDGE_TIP = "shownValentineNudgeTip";

	public static final String SHOWN_ADD_FRIENDS_POPUP = "shownAddFriendsPopup";

	public static final String WELCOME_TUTORIAL_VIEWED = "welcomeTutorialViewed";

	public static final String SHOWN_SDR_INTRO_TIP = "shownSdrIntroTip";

	public static final String SIGNUP_PROFILE_PIC_PATH = "signupProfilePicSet";

	public static final String LAST_BACK_OFF_TIME_SIGNUP_PRO_PIC = "lastBackOffTimeSignupProPic";

	public static final String SHOWN_FILE_TRANSFER_POP_UP = "shownFileTransferPopUp";

	public static final String SHOWN_GROUP_CHAT_TIP = "shownGroupChatTip";

	public static final String SHOWN_ADD_FAVORITE_TIP = "shownAddFavoriteTip";

	public static final String MQTT_IPS = "mqttIps";

	public static final String STEALTH_ENCRYPTED_PATTERN = "stealthEncryptedPattern";

	public static final String STEALTH_MODE = "stealthMode";

	public static final String STEALTH_MODE_SETUP_DONE = "steatlhModeSetupDone";

	public static final String STEALTH_MODE_FTUE_DONE = "steatlhModeFtueDone";

	public static final String STEALTH_PIN_AS_PASSWORD = "steatlhPinAsPassword";

	public static final String CONV_DB_VERSION_PREF =  "convDbVersion";

	public static final String SHOWING_STEALTH_FTUE_CONV_TIP = "showingStealthFtueConvTip";

	public static final String RESET_COMPLETE_STEALTH_START_TIME = "resetCompleteStealthStartTime";

	public static final String SHOWN_FIRST_UNMARK_STEALTH_TOAST = "shownFirstUnmarkStealthToast";

	public static final String SHOWN_WELCOME_HIKE_TIP = "shownWelcomeHikeTip";

	public static final String SHOW_STEALTH_INFO_TIP = "showStealthInfoTip";

	public static final String SHOW_STEALTH_UNREAD_TIP = "showStelathUnreadTip";

	public static final String STEALTH_UNREAD_TIP_MESSAGE = "stealthUnreadTipMessage";

	public static final String STEALTH_UNREAD_TIP_HEADER = "stealthUnreadTipHeader";

	public static final String LAST_STEALTH_POPUP_ID = "lastStealthPopupId";

	public static final String SHOWN_WELCOME_TO_HIKE_CARD = "shownWelcomeToHikeCard";

	public static final String FRIEND_REQ_COUNT = "frReqCount";

	public static final String HAS_UNSET_SMS_PREFS_ON_KITKAT_UPGRAGE = "hasUnsetSmsPrefsOnKitkatUpgrade";

	public static final String ATOMIC_POP_UP_TYPE_MAIN = "apuTypeMain";

	public static final String ATOMIC_POP_UP_TYPE_CHAT = "apuTypeChat";

	public static final String ATOMIC_POP_UP_STICKER = "stk";

	public static final String ATOMIC_POP_UP_PROFILE_PIC = "pp";

	public static final String ATOMIC_POP_UP_ATTACHMENT = "ft";

	public static final String ATOMIC_POP_UP_INFORMATIONAL = "info";

	public static final String ATOMIC_POP_UP_FAVOURITES = "fav";

	public static final String ATOMIC_POP_UP_THEME = "theme";

	public static final String ATOMIC_POP_UP_INVITE = "inv";

	public static final String ATOMIC_POP_UP_STATUS = "stts";

	public static final String ATOMIC_POP_UP_HTTP = "http";

	public static final String ATOMIC_POP_UP_APP_GENERIC = "app";

	public static final String ATOMIC_POP_UP_APP_GENERIC_WHAT = "appWhat";

	public static final String ATOMIC_POP_UP_HTTP_URL = "httpUrl";

	public static final String ATOMIC_POP_UP_NOTIF_MESSAGE = "apuNotifMessage";

	public static final String ATOMIC_POP_UP_NOTIF_SCREEN = "apuNotifScreen";

	public static final String ATOMIC_POP_UP_HEADER_MAIN = "apuHeaderMain";

	public static final String ATOMIC_POP_UP_MESSAGE_MAIN = "apuMessageMain";

	public static final String ATOMIC_POP_UP_HEADER_CHAT = "apuHeaderChat";

	public static final String ATOMIC_POP_UP_MESSAGE_CHAT = "apuMessageChat";

	public static final String SHOWN_DIWALI_POPUP = "shownDiwaliPopup";

	public static final String SHOWN_SHOP_ICON_BLUE = "shownShopIconBlue";

	public static final String IS_STICKER_CATEGORY_REORDERING_TIP_SHOWN = "showCategoryReordering";

	public static final String STICKER_SETTING_CHECK_BOX_CLICKED = "stickerSettingCheckBoxClicked";

	public static final String STICKER_SETTING_UNCHECK_BOX_CLICKED = "stickerSettingUnCheckBoxClicked";

	public static final String RETRY_NOTIFICATION_COOL_OFF_TIME = "retryNotificationCoolOffTime";

	public static final String LED_NOTIFICATION_COLOR_CODE = "led_notification_color_code";

	public static final String NOTIFICATION_TONE_URI = "notificationToneUri";

	public static final String NOTIFICATION_TONE_NAME = "notificaationToneName";

	public static final String SHOW_VOIP_FTUE_POPUP = "showVoipFtuePopup";

	public static final String VOIP_CALL_RATE_POPUP_FREQUENCY = "voipCallRatePopupFrequency";

	public static final String DETAILED_HTTP_LOGGING_ENABLED = "detailedHttpLoggingEnabled";

	public static final String CT_SEARCH_INDICATOR_SHOWN = "ctSearchIndiShown";

	public static final String CT_SEARCH_CLICKED = "ctSearchClicked";

	public static final String BULK_LAST_SEEN_PREF = "blsPref";

	public static final String TOGGLE_OK_HTTP = "toggleOkHttp";

	public static final String ENABLE_ADDRESSBOOK_THROUGH_HTTP_MGR = "enAbHttpMgr";

	public static final String EDIT_PROFILE_THROUGH_HTTP_MGR = "editProfHttpMgr";

	public static final String PROB_NUM_TEXT_MSG = "num_txt";

	public static final String PROB_NUM_STICKER_MSG = "num_stk";

	public static final String PROB_NUM_MULTIMEDIA_MSG = "num_multi";

	public static final String UPGRADE_FOR_SERVER_ID_FIELD = "upgradeForServerIdField";

	public static final String UPGRADE_FOR_DEFAULT_BOT_ENTRY = "upgradeForBotEntry";

	public static final String UPGRADE_SORTING_ID_FIELD = "upgradeForSortingIdField";

	public static final String UPGRADE_LANG_ORDER = "upgradeLanguageOrder";

	public static final String EXCEPTION_ANALYTIS_ENABLED = "exceptionAnalaticsEnabled";

	public static final String MAX_REPLY_RETRY_NOTIF_COUNT = "maxReplyRetryNotifCount";

	public static final String SSL_ALLOWED = "sslAllowed";

	public static final String CONTACT_UPDATE_WAIT_TIME = "contactUpdateWaitTime";

	public static final String KEYBOARD_HEIGHT_PORTRAIT = "keyboardHeightPortrait";

	public static final String KEYBOARD_HEIGHT_LANDSCAPE = "keyboardHeightLand";

	public static final String FAVORITES_TO_FRIENDS_TRANSITION_STATE = "favToFriendsTransState";

	public static CurrentState currentState = CurrentState.CLOSED;

	// Constants for sticker search=========================================================================================[[
	public static final String TAG_FIRST_TIME_DOWNLOAD = "tagFirstTimeDownload";

	public static final String DEFAULT_TAGS_DOWNLOADED = "defaultTagsDownloaded";

	public static final String STICKER_SET = "stickerSet";

	public static final String STICKER_REFRESH_SET = "stickerRefreshSet";

    public static final String STICKER_SET_FOR_LANGUAGE = "stickerSetForLanguage";

	public static final String STICKER_SET_FORCED_SET = "stickerSetForced";

	public static final String SHOWN_STICKER_RECOMMEND_TIP = "shownStickerRecommendTip";

	public static final String SHOWN_STICKER_RECOMMEND_AUTOPOPUP_OFF_TIP = "shownStickerRecommendAutoPopupOffTip";

	public static final String STICKER_RECOMMEND_SCROLL_FTUE_COUNT = "stickerRecommendScrollFtueCount";

	public static final String SET_ALARM_FIRST_TIME = "setAlarmFirstTime";

    public static final String STICKER_PALLETE_BUTTON_CLICK_ANALYTICS = "lastStickerButtonClickAnalyticsCount";

    public static final String STICKER_SEARCH_BUTTON_CLICK_ANALYTICS = "lastStickerSearchButtonClickAnalyticsCount";

    public static final String EMOTICON_BUTTON_CLICK_ANALYTICS = "lastEmoticonButtonClickAnalyticsCount";

    public static final String EMOTICONS_CLICKED_LIST = "emoticonClickedIndex";

    public static final String VIEWED_IN_PALLETE_CATEGORY_LIST = "viewedInPalletCatList";

	public static final String LAST_STICKER_PACK_AND_ORDERING_SENT_TIME = "lastPackAndOrderingSentTime";

	public static final String LAST_STICKER_TAG_REFRESH_TIME = "lastStickerTagRefreshTime";

	public static final String LAST_SUCCESSFUL_STICKER_TAG_REFRESH_TIME = "lastSuccessfulStickerTagRefreshTime";

	public static final String LAST_RECOMMENDATION_ACCURACY_ANALYTICS_SENT_TIME = "lastRecommendationAccuracyAnalyticsTime";

	public static final String STICKER_TAG_REFRESH_PERIOD = "stickerTagRefreshPeriod";

	public static final String SHOWN_STICKER_RECOMMEND_FTUE = "shownStickerRecommendationFtue";

	public static final String LAST_SUCESSFULL_TAGS_DOWNLOAD_TIME = "lastSuccessfulTagsDownloadTime";

    public static final String NOT_DOWNLOADED_LANGUAGES_SET = "notDownloadedLanguagesSet";

    public static final String DOWNLOADING_LANGUAGES_SET = "downloadingLanguagesSet";

	public static final String DOWNLOADED_LANGUAGES_SET = "downloadedLanguagesSet";

	public static final String FORBIDDEN_LANGUAGES_SET = "forbiddenLanguagesSet";

    public static final String DEFAULT_TAG_DOWNLOAD_LANGUAGES_PREF = "defaultTagDownloadLanguagePref";

	public static final String SINGLE_STICKER_DOWNLOAD_ERROR_COUNT = "singleStickerDownloadErrorCount";

	public static final String STICKER_PACK_DOWNLOAD_ERROR_COUNT = "stickerPackDownloadErrorCount";

	public static final String STICKER_FOLDER_LOCKED_ERROR_OCCURED = "stickerFolderLockedErrorOccured";

	public static final String SHOWN_PACK_PREVIEW_FTUE = "shownPackPreviewFtue";

	public static final String MIGRATE_RECENT_STICKER_TO_DB = "migrateRecentStickersToDb";

	public static final String QUICK_SUGGESTION_RETRY_SET = "quickSuggestionRetrySet";

	// =========================================================================================Constants for sticker search]]

	private static HikePubSub mPubSubInstance;

	public static boolean isIndianUser;

	private static Map<String, TypingNotification> typingNotificationMap;

	private AtomicBoolean mInitialized = new AtomicBoolean(false);

	private String token;

	private String msisdn;

	private DbConversationListener dbConversationListener;

	private ToastListener toastListener;

	private ActivityTimeLogger activityTimeLogger;

	public static Map<String, Pair<Integer, Long>> lastSeenFriendsMap;

	public static ConcurrentHashMap<String, BotInfo> hikeBotInfoMap;

	public static volatile boolean networkError;

	public static volatile boolean syncingContacts = false;

	public Handler appStateHandler;

	private StickerManager sm;

	private static HikeMessengerApp _instance;

	RegisterToGCMTrigger mmRegisterToGCMTrigger = null;

	SendGCMIdToServerTrigger mmGcmIdToServerTrigger = null;

	public static int bottomNavBarHeightPortrait = 0;

	public static int bottomNavBarWidthLandscape = 0;

    public static ConcurrentHashMap<String,Integer> hikeMappInfo = new ConcurrentHashMap<>();

	private static InternalCache diskCache;

	static
	{
		mPubSubInstance = new HikePubSub();
		if (HikeMessengerApp.lastSeenFriendsMap == null)
		{
			HikeMessengerApp.lastSeenFriendsMap = new HashMap<String, Pair<Integer, Long>>();
		}
	}

	public void setServiceAsDisconnected()
	{
		mInitialized.compareAndSet(true, false);
	}

	public void setServiceAsConnected()
	{
		mInitialized.compareAndSet(false, true);
	}

	public void connectToService()
	{
		if (!Utils.shouldConnectToMQTT())
		{
			Logger.d("HikeMessengerApp", "Not Connecting to service yet");
			return;
		}

		Logger.d("HikeMessengerApp", "calling connectToService:" + mInitialized);
		if (!mInitialized.get())
		{
			synchronized (HikeMessengerApp.class)
			{
				if (!mInitialized.get())
				{
					Logger.d("HikeMessengerApp", "Initializing service");

					ComponentName service = HikeMessengerApp.this.startService(new Intent(HikeMessengerApp.this, HikeService.class));

					if (service != null && service.getClassName().equals(HikeService.class.getName()))
					{
						// Service started
						setServiceAsConnected();
					}
					else
					{
						setServiceAsDisconnected();
					}
				}
			}
		}
	}

	/*
	 * Implement a Custom report sender to add our own custom msisdn and token for the username and password
	 */
	public class CustomReportSender implements ReportSender
	{
		@Override
		public void send(Context arg0, CrashReportData crashReportData) throws ReportSenderException
		{
			try
			{
				final String reportUrl = AccountUtils.base + "/logs/android";
				Logger.d(HikeMessengerApp.this.getClass().getSimpleName(), "Connect to " + reportUrl.toString());

				final String login = msisdn;
				final String password = token;

				if (login != null && password != null)
				{
					final HttpRequest request = new HttpRequest();
					request.setLogin(login);
					request.setPassword(password);
					String paramsAsString = getParamsAsString(crashReportData);
					Logger.e(HikeMessengerApp.this.getClass().getSimpleName(), "Params: " + paramsAsString);
					request.send(arg0, new URL(reportUrl), HttpSender.Method.POST, paramsAsString, HttpSender.Type.FORM);
				}
			}
			catch (IOException e)
			{
				Logger.e(HikeMessengerApp.this.getClass().getSimpleName(), "IOException", e);
			}
		}

	}

	/**
	 * Converts a Map of parameters into a URL encoded Sting.
	 *
	 * @param parameters
	 *            Map of parameters to convert.
	 * @return URL encoded String representing the parameters.
	 * @throws UnsupportedEncodingException
	 *             if one of the parameters couldn't be converted to UTF-8.
	 */
	private String getParamsAsString(Map<?, ?> parameters) throws UnsupportedEncodingException
	{

		final StringBuilder dataBfr = new StringBuilder();
		for (final Object key : parameters.keySet())
		{
			if (dataBfr.length() != 0)
			{
				dataBfr.append('&');
			}
			final Object preliminaryValue = parameters.get(key);
			final Object value = (preliminaryValue == null) ? "" : preliminaryValue;
			dataBfr.append(URLEncoder.encode(key.toString(), "UTF-8"));
			dataBfr.append('=');
			dataBfr.append(URLEncoder.encode(value.toString(), "UTF-8"));
		}

		return dataBfr.toString();
	}

	@Override
	public void onTrimMemory(int level)
	{
		// TODO Auto-generated method stub
		super.onTrimMemory(level);
	}

	public void onCreate()
	{
		super.onCreate();
		_instance = this;

		long time = System.currentTimeMillis();
		Utils.enableNetworkListner(this);
		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		msisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
		String uid = settings.getString(HikeMessengerApp.UID_SETTING, null);
		// this is the setting to check whether the conv DB migration has
		// started or not
		// -1 in both cases means an uninitialized setting, mostly on first
		// launch or interrupted upgrades.
		int convInt = settings.getInt(HikeConstants.UPGRADE_AVATAR_CONV_DB, -1);
		int msgHashGrpReadUpgrade = settings.getInt(HikeConstants.UPGRADE_MSG_HASH_GROUP_READBY, -1);
		int upgradeForDbVersion28 = settings.getInt(HikeConstants.UPGRADE_FOR_DATABASE_VERSION_28, -1);



		// We need to set all AppConfig params on the start when _instance have been initialized
		// reason : AppConfig class is loaded before we set _instance ==> HikeSharedPrefUtil won't be able to
		// initialize successfully ==> Utils.isSendLogsEnabled would return false. and Send logs won't show up
		AppConfig.refresh();

		setupAppLocalization();
		Utils.setDensityMultiplier(getResources().getDisplayMetrics());

		// first time or failed DB upgrade.
		if (convInt == -1)
		{
			Editor mEditor = settings.edit();
			// set the pref to 0 to indicate we've reached the state to init the
			// hike conversation database.
			mEditor.putInt(HikeConstants.UPGRADE_AVATAR_CONV_DB, 0);
			mEditor.commit();
		}

		if (msgHashGrpReadUpgrade == -1)
		{
			Editor mEditor = settings.edit();
			// set the pref to 0 to indicate we've reached the state to init the
			// hike conversation database.
			mEditor.putInt(HikeConstants.UPGRADE_MSG_HASH_GROUP_READBY, 0);
			mEditor.commit();
		}

		if (upgradeForDbVersion28 == -1)
		{
			Editor mEditor = settings.edit();
			// set the pref to 0 to indicate we've reached the state to init the
			// hike conversation database.
			mEditor.putInt(HikeConstants.UPGRADE_FOR_DATABASE_VERSION_28, 0);
			mEditor.commit();
		}
		String currentAppVersion = settings.getString(CURRENT_APP_VERSION, "");
		String actualAppVersion = "";
		try
		{
			actualAppVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		}
		catch (NameNotFoundException e)
		{
			Logger.e("AccountUtils", "Unable to get app version");
		}

		if (!currentAppVersion.equals(actualAppVersion))
		{
			if (!currentAppVersion.equals(""))
			{
				Utils.resetUpdateParams(settings);
				// for restore notification default setting
				HikeNotificationUtils.restoreNotificationParams(getApplicationContext());
			}

			/*
			 * Updating the app version.
			 */
			Editor editor = settings.edit();
			editor.putString(CURRENT_APP_VERSION, actualAppVersion);
			editor.commit();
		}

		initImportantAppComponents(settings);

		// if the setting value is 1 , this means the DB onUpgrade was called
		// successfully.
		if ((settings.getInt(HikeConstants.UPGRADE_AVATAR_CONV_DB, -1) == 1)
				|| settings.getInt(HikeConstants.UPGRADE_MSG_HASH_GROUP_READBY, -1) == 1
				|| settings.getInt(HikeConstants.UPGRADE_FOR_DATABASE_VERSION_28, -1) == 1
				|| settings.getInt(StickerManager.MOVED_HARDCODED_STICKERS_TO_SDCARD, 1) == 1
				|| settings.getInt(StickerManager.UPGRADE_FOR_STICKER_SHOP_VERSION_1, 1) == 1
				|| settings.getInt(UPGRADE_FOR_SERVER_ID_FIELD, 0) == 1
				|| settings.getInt(UPGRADE_SORTING_ID_FIELD, 0) == 1
				||settings.getInt(UPGRADE_LANG_ORDER,0)==0
				|| settings.getBoolean(HikeConstants.HIKE_CONTENT_MICROAPPS_MIGRATION, false) == false
				|| settings.getBoolean(HikeConstants.BackupRestore.KEY_MOVED_STICKER_EXTERNAL, false) == false
				|| settings.getBoolean(HikeMessengerApp.MIGRATE_RECENT_STICKER_TO_DB, false) == false
				|| settings.getBoolean(StickerManager.UPGRADE_STICKER_CATEGORIES_TABLE, false) == false
				|| TEST)
		{
			startUpdgradeIntent();
		}
		else
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.UPGRADING, false);
		}

		if (settings.getInt(StickerManager.UPGRADE_FOR_STICKER_SHOP_VERSION_1, 1) == 2)
		{
			sm.doInitialSetup();
		}

		// String twitterToken = settings.getString(HikeMessengerApp.TWITTER_TOKEN, "");
		// String twitterTokenSecret = settings.getString(HikeMessengerApp.TWITTER_TOKEN_SECRET, "");
		// makeTwitterInstance(twitterToken, twitterTokenSecret);

		String countryCode = settings.getString(COUNTRY_CODE, "");
		setIndianUser(countryCode.equals(HikeConstants.INDIA_COUNTRY_CODE));

		SharedPreferences preferenceManager = PreferenceManager.getDefaultSharedPreferences(this);

		// we use this preference to check if this is a fresh install case or an
		// update case
		// in case of an update the SSL pref would not be null

		boolean isSAUser = countryCode.equals(HikeConstants.SAUDI_ARABIA_COUNTRY_CODE);

		// Setting SSL_PREF as false for existing SA users with SSL_PREF = true
		if (!preferenceManager.contains(HikeConstants.SSL_PREF) || (isSAUser && settings.getBoolean(HikeConstants.SSL_PREF, false)))
		{
			Editor editor = preferenceManager.edit();
			editor.putBoolean(HikeConstants.SSL_PREF, !(isIndianUser || isSAUser));
			editor.commit();
		}

		//if ssl_allowed preference is not set then set it
		// this will be usefull for upgrading users.
		if(!HikeSharedPreferenceUtil.getInstance().contains(HikeMessengerApp.SSL_ALLOWED))
		{
			Utils.setSSLAllowed(countryCode);
		}

		if (!preferenceManager.contains(HikeConstants.RECEIVE_SMS_PREF))
		{
			Editor editor = preferenceManager.edit();
			editor.putBoolean(HikeConstants.RECEIVE_SMS_PREF, false);
			editor.commit();
		}

		if (!preferenceManager.contains(HikeConstants.STATUS_BOOLEAN_PREF))
		{
			Editor editor = preferenceManager.edit();
			editor.putBoolean(HikeConstants.STATUS_BOOLEAN_PREF, preferenceManager.getInt(HikeConstants.STATUS_PREF, 0) == 0);
			editor.commit();
		}

		if (Utils.isKitkatOrHigher() && !HikeSharedPreferenceUtil.getInstance().getData(HAS_UNSET_SMS_PREFS_ON_KITKAT_UPGRAGE, false))
		{
			/*
			 * On upgrade in kitkat or higher we need to reset sms setting preferences as we are now removing these settings from UI.
			 */
			HikeSharedPreferenceUtil.getInstance().saveData(HAS_UNSET_SMS_PREFS_ON_KITKAT_UPGRAGE, true);
			Editor editor = preferenceManager.edit();
			editor.remove(HikeConstants.SEND_SMS_PREF);
			editor.remove(HikeConstants.RECEIVE_SMS_PREF);
			editor.commit();
		}


		if (token != null)
		{
			AccountUtils.setToken(token);
		}
		if (uid != null)
		{
			AccountUtils.setUID(uid);
		}
		try
		{
			AccountUtils.setAppVersion(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		}
		catch (NameNotFoundException e)
		{
			Logger.e(getClass().getSimpleName(), "Invalid package", e);
		}

		/*
		 * Replacing GB keys' strings.
		 */
		if (!settings.contains(GREENBLUE_DETAILS_SENT))
		{
			replaceGBKeys();
		}

		validateCriticalDirs();

		makeNoMediaFiles();

		HikeMessengerApp.getPubSub().addListener(HikePubSub.CONNECTED_TO_MQTT, this);

		if (Utils.isUserAuthenticated(this))
		{
			fetchPlatformIDIfNotPresent();
		}

		// Cancel any going OfflineNotification
		HikeNotification.getInstance().cancelNotification(OfflineConstants.NOTIFICATION_IDENTIFIER);

		HikeSharedPreferenceUtil.getInstance().removeData(OfflineConstants.DIRECT_REQUEST_DATA);

        setAnalyticsSendAlarm();

		StickerManager.getInstance().refreshTagData();

		bottomNavBarHeightPortrait = Utils.getBottomNavBarHeight(getApplicationContext());
		bottomNavBarWidthLandscape = Utils.getBottomNavBarWidth(getApplicationContext());
		PlatformUtils.resumeLoggingLocationIfRequired();
		//Init AB-Testing framework
		ABTest.apply(getApplicationContext());
		CustomTabsHelper.getPackageNameToUse(this);
		Logger.d(HikeConstants.APP_OPENING_BENCHMARK, "Time taken in HikeMessengerApp onCreate = " + (System.currentTimeMillis() - time));

		Utils.connectToGcmPreSignup();

	}

	private void validateCriticalDirs()
	{
		HikeHandlerUtil.getInstance().postRunnable(new Runnable()
		{
			@Override
			public void run()
			{
				Utils.validateDirectory(HikeConstants.HIKE_DIRECTORY_ROOT);
				Utils.validateDirectory(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT);
				Utils.validateDirectory(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT);
				Utils.validateDirectory(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.IMAGE_ROOT);
				Utils.validateDirectory(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.IMAGE_ROOT + HikeConstants.SENT_ROOT);
				Utils.validateDirectory(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.VIDEO_ROOT);
				Utils.validateDirectory(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.VIDEO_ROOT + HikeConstants.SENT_ROOT);
				Utils.validateDirectory(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.AUDIO_ROOT);
				Utils.validateDirectory(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.AUDIO_ROOT + HikeConstants.SENT_ROOT);
				Utils.validateDirectory(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.AUDIO_RECORDING_ROOT);
				Utils.validateDirectory(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.AUDIO_RECORDING_ROOT + HikeConstants.SENT_ROOT);
				Utils.validateDirectory(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.OTHER_ROOT);
				Utils.validateDirectory(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.OTHER_ROOT + HikeConstants.SENT_ROOT);
			}
		});
	}

	public void initCrashReportingTool()
	{
		if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.CRASH_REPORTING_TOOL,HikeConstants.ACRA).equals(HikeConstants.CRASHLYTICS))
		{
			Logger.d("HikeMessangerApp","Initializing Crashlytics");
			Fabric.with(this, new Crashlytics());
			logUser();
		}
		else
		{
			Logger.d("HikeMessangerApp","Initializing ACRA");
			ACRA.init(this);
			CustomReportSender customReportSender = new CustomReportSender();
			ErrorReporter.getInstance().setReportSender(customReportSender);
		}
	}

	private void initImportantAppComponents(SharedPreferences prefs)
	{
		// we're basically banking on the fact here that init() would be
		// succeeded by the onUpgrade() calls being triggered in the respective databases.
		initTwinPrime();
		HikeConversationsDatabase.init(this);

		initHikeLruCache(getApplicationContext());
		HttpManager.init();

		sm = StickerManager.getInstance();

		HikeMqttPersistence.init(this);
		SmileyParser.init(this);

		Utils.setupServerURL(prefs.getBoolean(HikeMessengerApp.PRODUCTION, true), Utils.switchSSLOn(getApplicationContext()));
		HttpRequestConstants.setUpBase();

		typingNotificationMap = new HashMap<String, TypingNotification>();

		initialiseListeners();

		hikeBotInfoMap = new ConcurrentHashMap<>();

		initContactManager();
		BotUtils.initBots();
		//Check if any pending platform packet is waiting for download.
		PlatformUtils.retryPendingDownloadsIfAny(Utils.getNetworkShortinOrder(Utils.getNetworkTypeAsString(getApplicationContext())));
		/*
		 * Fetching all stealth contacts on app creation so that the conversation cannot be opened through the shortcut or share screen.
		 */
		StealthModeManager.getInstance().initiate();

		appStateHandler = new Handler();

		registerReceivers();

		ProductInfoManager.getInstance().init();

		AtomicTipManager.getInstance().init();

        // Set default path as internal storage on production host
        PlatformContentConstants.PLATFORM_CONTENT_DIR = PlatformContentConstants.MICRO_APPS_VERSIONING_PROD_CONTENT_DIR;

		PlatformContent.init(prefs.getBoolean(HikeMessengerApp.PRODUCTION, true));

		ChatHeadUtils.startOrStopService(false);

		StickerSearchManager.getInstance().initStickerSearchProviderSetupWizard();

		// Moving the shared pref stored in account prefs to the default prefs.
		// This is done because previously we were saving shared pref for caller in accountutils but now using default settings prefs
		// On a long run this should be deleted
		if (HikeSharedPreferenceUtil.getInstance().contains(HikeConstants.ACTIVATE_STICKY_CALLER_PREF))
		{
			Utils.setSharedPrefValue(this, HikeConstants.ACTIVATE_STICKY_CALLER_PREF,
					HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ACTIVATE_STICKY_CALLER_PREF, false));
			HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.ACTIVATE_STICKY_CALLER_PREF);

		}
		if (HikeSharedPreferenceUtil.getInstance().contains(StickyCaller.CALLER_Y_PARAMS_OLD))
		{
			HikeSharedPreferenceUtil.getInstance().removeData(StickyCaller.CALLER_Y_PARAMS_OLD);
		}

		initCrashReportingTool();

		checkAndTriggerPendingGcmNetworkCalls();
	}

	public void logUser() {
		// TODO: Use the current user's information
		// You can call any combination of these three methods
		String uId = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.UID_SETTING, null);
		if (!TextUtils.isEmpty(uId)) {
			Crashlytics.setUserIdentifier(uId);
		}
	}

	public static InternalCache getDiskCache()
	{
		if(diskCache == null) {

			File cacheDir = new File(Utils.getExternalFilesDirPath(null) + HikeConstants.DISK_CACHE_ROOT);
			long diskCacheSize = Utils.calculateDiskCacheSize(cacheDir);
			Logger.d("disk_cache", "disk cache size : " + diskCacheSize);

			Cache cache = new Cache(cacheDir, diskCacheSize);
			diskCache = cache.getCache();
		}
		return diskCache;
	}

	private void initTwinPrime()
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.TP_ENABLE, 0) == 1)
		{
			new TwinPrimeSDK(getApplicationContext(), HikeConstants.TP_API_KEY);
			// Setting passive location if found for tracking
			Location loc = Utils.getPassiveLocation();
			Logger.d("TwinPrime","PassiveLocation is "+ loc);
			if (loc != null)
				TwinPrimeSDK.setLocation(loc);
		}
	}
	/**
	 * fetching the platform user id from the server. Will not fetch if the platform user id is already present. Will fetch the address book's platform uid on success of this call.
	 */
	private void fetchPlatformIDIfNotPresent()
	{
		HikeSharedPreferenceUtil prefs = HikeSharedPreferenceUtil.getInstance();
		if (prefs.getData(HikeMessengerApp.PLATFORM_UID_SETTING, null) == null && prefs.getData(HikeMessengerApp.PLATFORM_TOKEN_SETTING, null) == null)
		{
			PlatformUIDFetch.fetchPlatformUid(HikePlatformConstants.PlatformFetchType.SELF);
		}
	}

	public static HikeMessengerApp getInstance()
	{
		return _instance;
	}

	private void registerReceivers()
	{
		// TODO Auto-generated method stub

		LocalBroadcastManager mmBroadcastManager = LocalBroadcastManager.getInstance(this);
		mmRegisterToGCMTrigger = new RegisterToGCMTrigger();
		mmGcmIdToServerTrigger = new SendGCMIdToServerTrigger();

		mmBroadcastManager.registerReceiver(mmRegisterToGCMTrigger, new IntentFilter(HikeService.REGISTER_TO_GCM_ACTION));

		mmBroadcastManager.registerReceiver(mmGcmIdToServerTrigger, new IntentFilter(HikeService.SEND_TO_SERVER_ACTION));

	}

	public void startUpdgradeIntent()
	{
		IntentFactory.startUpgradeIntent(this);
	}

	private void replaceGBKeys()
	{
		HikeSharedPreferenceUtil preferenceUtil = HikeSharedPreferenceUtil.getInstance();

		boolean gbDetailsSent = preferenceUtil.getData("whatsappDetailsSent", false);
		int lastGBBackoffTime = preferenceUtil.getData("lastBackOffTimeWhatsapp", 0);

		preferenceUtil.saveData(GREENBLUE_DETAILS_SENT, gbDetailsSent);
		preferenceUtil.saveData(LAST_BACK_OFF_TIME_GREENBLUE, lastGBBackoffTime);
	}

	private static HikeLruCache cache;

	private void initHikeLruCache(Context applicationContext)
	{
		ImageCacheParams params = new ImageCacheParams();
		params.setMemCacheSizePercent(0.15f);
		cache = HikeLruCache.getInstance(params, getApplicationContext());
	}

	public static HikeLruCache getLruCache()
	{
		return cache;
	}

	private void initContactManager()
	{
		/*
		 * Contact Manager getInstance will initialize contact manager if already not initialized and returns the ContactManager's instance
		 */
		ContactManager.getInstance();
	}

	private void makeNoMediaFiles()
	{
		String mediaRoot = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT;

		File folder = new File(mediaRoot + HikeConstants.PROFILE_ROOT);
		Utils.makeNoMediaFile(folder);

		folder = new File(mediaRoot + HikeConstants.AUDIO_RECORDING_ROOT);
		Utils.makeNoMediaFile(folder);

		folder = new File(mediaRoot + HikeConstants.IMAGE_ROOT + HikeConstants.SENT_ROOT);
		/*
		 * Fixed issue where sent media directory is getting visible in Gallery.
		 */
		Utils.makeNoMediaFile(folder, true);

		folder = new File(mediaRoot + HikeConstants.VIDEO_ROOT + HikeConstants.SENT_ROOT);
		Utils.makeNoMediaFile(folder);

		folder = new File(mediaRoot + HikeConstants.AUDIO_ROOT + HikeConstants.SENT_ROOT);
		Utils.makeNoMediaFile(folder);

		folder = new File(mediaRoot + HikeConstants.AUDIO_RECORDING_ROOT + HikeConstants.SENT_ROOT);
		Utils.makeNoMediaFile(folder);

		folder = new File(mediaRoot + HikeConstants.OTHER_ROOT + HikeConstants.SENT_ROOT);
		Utils.makeNoMediaFile(folder);

		folder = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR);
		Utils.makeNoMediaFile(folder, true);

        folder = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.HIKE_MICRO_APPS);
        Utils.makeNoMediaFile(folder, true);

        folder = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.HIKE_MICRO_APPS + PlatformContentConstants.HIKE_WEB_MICRO_APPS);
        Utils.makeNoMediaFile(folder, true);

        folder = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.HIKE_MICRO_APPS + PlatformContentConstants.HIKE_ONE_TIME_POPUPS);
        Utils.makeNoMediaFile(folder, true);

        folder = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.HIKE_MICRO_APPS + PlatformContentConstants.HIKE_GAMES);
        Utils.makeNoMediaFile(folder, true);

        folder = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.HIKE_MICRO_APPS + PlatformContentConstants.HIKE_MAPPS);
        Utils.makeNoMediaFile(folder, true);

	}

	public static HikePubSub getPubSub()
	{
		return mPubSubInstance;
	}

	public static boolean isIndianUser()
	{
		return isIndianUser;
	}

	public static void setIndianUser(boolean val)
	{
		isIndianUser = val;
	}

	public static Map<String, TypingNotification> getTypingNotificationSet()
	{
		return typingNotificationMap;
	}

	public void initialiseListeners()
	{
		if (dbConversationListener == null)
		{
			dbConversationListener = new DbConversationListener(getApplicationContext());
		}
		if (toastListener == null)
		{
			toastListener = ToastListener.getInstance();
		}
		if (activityTimeLogger == null)
		{
			activityTimeLogger = new ActivityTimeLogger();
		}
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.CONNECTED_TO_MQTT.equals(type))
		{
			appStateHandler.post(appStateChangedRunnable);
		}
	}

	public void showToast(final String message)
	{
		appStateHandler.post(new Runnable()
		{

			@Override
			public void run()
			{
				Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
			}
		});
	}

	public void showToast(final String message,final int duration)
	{
		appStateHandler.post(new Runnable()
		{

			@Override
			public void run()
			{
				Toast.makeText(getApplicationContext(), message,duration).show();
			}
		});
	}


	public void showToast(final int stringId,final int duration)
	{
		appStateHandler.post(new Runnable()
		{

			@Override
			public void run()
			{
				Toast.makeText(getApplicationContext(),getResources().getString(stringId), duration).show();
			}
		});
	}

	private Runnable appStateChangedRunnable = new Runnable()
	{

		@Override
		public void run()
		{
			/*
			 * Send a fg/bg packet on reconnecting.
			 */
			Utils.appStateChanged(HikeMessengerApp.this.getApplicationContext(), false, false, false, true, false);
		}
	};

	public static boolean keyboardApproach(Context context)
	{// server side switch
		int kc = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.KEYBOARD_CONFIGURATION, HikeConstants.KEYBOARD_CONFIGURATION_NEW);
		return kc == HikeConstants.KEYBOARD_CONFIGURATION_NEW;
	}

	public static boolean isLocalisationEnabled()
	{
		// server switch
		return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.LOCALIZATION_ENABLED, true);
	}

	private void setupAppLocalization()
	{
		setupLocalLanguage();
		LocalLanguageUtils.handleHikeSupportedListOrderChange(this);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		setupLocalLanguage();
	}

	public void setupLocalLanguage()
	{
		Resources res = getApplicationContext().getResources();
		Configuration config = res.getConfiguration();
		config.locale = Utils.getCurrentLanguageLocale();
		res.updateConfiguration(config, res.getDisplayMetrics());
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		MultiDex.install(this);
	}


    private void setAnalyticsSendAlarm()
    {
        if(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.DAILY_ANALYTICS_ALARM_STATUS, false))
        {
            return;
        }

        long scheduleTime = Utils.getTimeInMillis(Calendar.getInstance(Locale.ENGLISH),HikeMessengerApp.DEFAULT_SEND_ANALYTICS_TIME_HOUR, 0, 0, 0);

        if (scheduleTime < System.currentTimeMillis())
        {
            scheduleTime += HikeConstants.ONE_DAY_MILLS; // Next day at given time
        }

        HikeAlarmManager.setAlarmwithIntentPersistance(HikeMessengerApp.getInstance(), scheduleTime, HikeAlarmManager.REQUESTCODE_LOG_HIKE_ANALYTICS, false, IntentFactory.getPersistantAlarmIntent(), true);

        HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.DAILY_ANALYTICS_ALARM_STATUS, true);
    }

    public static void clearDiskCache()
    {
        diskCache = null;
    }

	private void checkAndTriggerPendingGcmNetworkCalls()
	{
		HikeGcmNetworkMgr.getInstance().triggerPendingGcmNetworkCalls();
	}

}
