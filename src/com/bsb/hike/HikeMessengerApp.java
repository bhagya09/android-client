package com.bsb.hike;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.ReportField;
import org.acra.annotation.ReportsCrashes;
import org.acra.collector.CrashReportData;
import org.acra.sender.HttpSender;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;
import org.acra.util.HttpRequest;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Pair;

import com.bsb.hike.db.DbConversationListener;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeMqttPersistence;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.notifications.ToastListener;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.service.MqttMessagesManager;
import com.bsb.hike.service.RegisterToGCMTrigger;
import com.bsb.hike.service.SendGCMIdToServerTrigger;
import com.bsb.hike.service.UpgradeIntentService;
import com.bsb.hike.smartcache.HikeLruCache;
import com.bsb.hike.smartcache.HikeLruCache.ImageCacheParams;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ActivityTimeLogger;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

@ReportsCrashes(formKey = "", customReportContent = { ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.PHONE_MODEL, ReportField.BRAND, ReportField.PRODUCT,
		ReportField.ANDROID_VERSION, ReportField.STACK_TRACE, ReportField.USER_APP_START_DATE, ReportField.USER_CRASH_DATE })
public class HikeMessengerApp extends Application implements HikePubSub.Listener
{

	public static enum CurrentState
	{
		OPENED, RESUMED, BACKGROUNDED, CLOSED, NEW_ACTIVITY, BACK_PRESSED, NEW_ACTIVITY_IN_BG
	}

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

	public static final String RESTORE_ACCOUNT_SETTING = "restore";
	
	public static final String SIGNUP_COMPLETE = "signup_complete";

	public static final String RESTORING_BACKUP = "restoring_backup";

	public static final String UPDATE_SETTING = "update";

	public static final String ANALYTICS = "analytics";

	public static final String REFERRAL = "referral";

	public static final String ADDRESS_BOOK_SCANNED = "abscanned";

	public static final String CONTACT_LIST_EMPTY = "contactlistempty";

	public static final String SMS_SETTING = "smscredits";

	public static final String NAME = "name";

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

	public static final String COUNTRY_CODE = "countryCode";

	public static final String FILE_PATH = "filePath";

	public static final String TEMP_NAME = "tempName";

	public static final String TEMP_NUM = "tempNum";

	public static final String TEMP_COUNTRY_CODE = "tempCountryCode";

	public static final String GCM_ID_SENT_PRELOAD = "gcm_id_sent_preload";
	
	public static final String GCM_ID_SENT = "gcmIdSent";

	public static final String BLOCK_NOTIFICATIONS = "blockNotification";

	private static final boolean TEST = false; // TODO:: test flag only : turn
												// OFF for Production

	/*
	 * Setting name for the day the was logged on fiksu for "First message sent in day"
	 */
	public static final String DAY_RECORDED = "dayRecorded";

	public static final String LAST_BACK_OFF_TIME = "lastBackOffTime";

	public static final String FACEBOOK_TOKEN = "facebookToken";

	public static final String FACEBOOK_TOKEN_EXPIRES = "facebookTokenExpires";

	public static final String FACEBOOK_USER_ID = "facebookUserId";

	public static final String FACEBOOK_AUTH_COMPLETE = "facebookAuthComplete";

	//public static final String TWITTER_TOKEN = "twitterToken";

	//public static final String TWITTER_TOKEN_SECRET = "twitterTokenSecret";

	//public static final String TWITTER_AUTH_COMPLETE = "twitterAuthComplete";

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

	public static final String APP_LAUNCHES = "appLaunches";

	public static final String DONT_SHOW_APP_RATER = "dontShowAppRater";

	public static final String INTRO_DONE = "introDone";

	public static final String JUST_SIGNED_UP = "justSignedUp";

	public static final String INVITED_NUMBERS = "invitedNumbers";

	public static final String UNSEEN_STATUS_COUNT = "unseenStatusCount";

	public static final String UNSEEN_USER_STATUS_COUNT = "unseenUserStatusCount";

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

	public static final String SHOWN_EMOTICON_TIP = "shownEmoticonTip1";
	
	public static final String SHOWN_PIN_TIP = "shownPinTip";

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

	public static final String FB_SIGNUP = "fbSignup";

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
	
	public static final String STICKED_BTN_CLICKED_FIRST_TIME = "stickerBtnClickedFirstTime";

	public static final String STICKER_SETTING_CHECK_BOX_CLICKED = "stickerSettingCheckBoxClicked";
	
	public static final String STICKER_SETTING_UNCHECK_BOX_CLICKED = "stickerSettingUnCheckBoxClicked";

	public static final String RETRY_NOTIFICATION_COOL_OFF_TIME = "retryNotificationCoolOffTime";
	
	public static final String LED_NOTIFICATION_COLOR_CODE = "led_notification_color_code";

	public static final String NOTIFICATION_TONE_URI = "notificationToneUri";

	public static final String NOTIFICATION_TONE_NAME = "notificaationToneName";

	public static final String SHOWN_VOIP_INTRO_TIP = "shownVoipIntroTip";

	public static final String SHOW_VOIP_CALL_RATE_POPUP = "showVoipCallRatePopup";

	public static final String VOIP_CALL_RATE_POPUP_FREQUENCY = "voipCallRatePopupFrequency";

	public static final String VOIP_ACTIVE_CALLS_COUNT = "voipCallsCount";

	public static final String DETAILED_HTTP_LOGGING_ENABLED = "detailedHttpLoggingEnabled";
	
	public static final String BULK_LAST_SEEN_PREF = "blsPref";

	public static CurrentState currentState = CurrentState.CLOSED;

	//private static Twitter twitter;

	private static HikePubSub mPubSubInstance;

	public static boolean isIndianUser;

	private static Map<String, TypingNotification> typingNotificationMap;

	private static Set<String> stealthMsisdn;

	private AtomicBoolean mInitialized = new AtomicBoolean(false);

	private String token;

	private String msisdn;

	private DbConversationListener dbConversationListener;

	private ToastListener toastListener;

	private ActivityTimeLogger activityTimeLogger;

	public static Map<String, Pair<Integer, Long>> lastSeenFriendsMap;

	public static HashMap<String, String> hikeBotNamesMap;

	public static volatile boolean networkError;

	public static volatile boolean syncingContacts = false;

	public Handler appStateHandler;
	
	private StickerManager sm;
	
	private static HikeMessengerApp _instance;
	
	RegisterToGCMTrigger mmRegisterToGCMTrigger = null;

	SendGCMIdToServerTrigger mmGcmIdToServerTrigger = null;

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
		if(!Utils.isUserSignedUp(getApplicationContext(), false))
		{
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
					
					if(service!=null && service.getClassName().equals(HikeService.class.getName()))
					{
						//Service started
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
	private class CustomReportSender implements ReportSender
	{
		@Override
		public void send(CrashReportData crashReportData) throws ReportSenderException
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
					request.send(new URL(reportUrl), HttpSender.Method.POST, paramsAsString, HttpSender.Type.FORM);
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

		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		msisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
		String uid = settings.getString(HikeMessengerApp.UID_SETTING, null);
		// this is the setting to check whether the avtar DB migration has
		// started or not
		int avtarInt = settings.getInt(HikeConstants.UPGRADE_AVATAR_PROGRESS_USER, -1);
		// this is the setting to check whether the conv DB migration has
		// started or not
		// -1 in both cases means an uninitialized setting, mostly on first
		// launch or interrupted upgrades.
		int convInt = settings.getInt(HikeConstants.UPGRADE_AVATAR_CONV_DB, -1);
		int msgHashGrpReadUpgrade = settings.getInt(HikeConstants.UPGRADE_MSG_HASH_GROUP_READBY, -1);
		int upgradeForDbVersion28 = settings.getInt(HikeConstants.UPGRADE_FOR_DATABASE_VERSION_28, -1);
		ACRA.init(this);
		CustomReportSender customReportSender = new CustomReportSender();
		ErrorReporter.getInstance().setReportSender(customReportSender);

		super.onCreate();
		
		_instance = this;

		Utils.setDensityMultiplier(getResources().getDisplayMetrics());

		// first time or failed DB upgrade.
		if (avtarInt == -1 && convInt == -1)
		{
			Editor mEditor = settings.edit();
			// set the pref to 0 to indicate we've reached the state to init the
			// hike conversation database.
			mEditor.putInt(HikeConstants.UPGRADE_AVATAR_PROGRESS_USER, 0);
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
		/*
		 * Resetting the stealth mode when the app starts. 
		 */
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);
		performPreferenceTransition();
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
			}

			/*
			 * Updating the app version.
			 */
			Editor editor = settings.edit();
			editor.putString(CURRENT_APP_VERSION, actualAppVersion);
			editor.commit();
		}
		// we're basically banking on the fact here that init() would be
		// succeeded by the
		// onUpgrade() calls being triggered in the respective databases.
		HikeConversationsDatabase.init(this);

		sm = StickerManager.getInstance();
		sm.init(getApplicationContext());
		
		// if the setting value is 1 , this means the DB onUpgrade was called
		// successfully.
		if ((settings.getInt(HikeConstants.UPGRADE_AVATAR_CONV_DB, -1) == 1 && settings.getInt(HikeConstants.UPGRADE_AVATAR_PROGRESS_USER, -1) == 1) || 
				settings.getInt(HikeConstants.UPGRADE_MSG_HASH_GROUP_READBY, -1) == 1 || settings.getInt(HikeConstants.UPGRADE_FOR_DATABASE_VERSION_28, -1) == 1 || 
				settings.getInt(StickerManager.MOVED_HARDCODED_STICKERS_TO_SDCARD, 1) == 1 || settings.getInt(StickerManager.UPGRADE_FOR_STICKER_SHOP_VERSION_1, 1) == 1 || TEST)
		{
			startUpdgradeIntent();
		}

		if(settings.getInt(StickerManager.UPGRADE_FOR_STICKER_SHOP_VERSION_1, 1) == 2)
		{
			sm.doInitialSetup();
		}
		
		HikeMqttPersistence.init(this);
		SmileyParser.init(this);

		//String twitterToken = settings.getString(HikeMessengerApp.TWITTER_TOKEN, "");
		//String twitterTokenSecret = settings.getString(HikeMessengerApp.TWITTER_TOKEN_SECRET, "");
		//makeTwitterInstance(twitterToken, twitterTokenSecret);

		setIndianUser(settings.getString(COUNTRY_CODE, "").equals(HikeConstants.INDIA_COUNTRY_CODE));

		SharedPreferences preferenceManager = PreferenceManager.getDefaultSharedPreferences(this);

		// we use this preference to check if this is a fresh install case or an
		// update case
		// in case of an update the SSL pref would not be null

		boolean isSAUser = settings.getString(COUNTRY_CODE, "").equals(HikeConstants.SAUDI_ARABIA_COUNTRY_CODE);

		// Setting SSL_PREF as false for existing SA users with SSL_PREF = true
		if (!preferenceManager.contains(HikeConstants.SSL_PREF) || (isSAUser && settings.getBoolean(HikeConstants.SSL_PREF, false)))
		{
			Editor editor = preferenceManager.edit();
			editor.putBoolean(HikeConstants.SSL_PREF, !(isIndianUser || isSAUser));
			editor.commit();
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
		
		if(Utils.isKitkatOrHigher() && !HikeSharedPreferenceUtil.getInstance().getData(HAS_UNSET_SMS_PREFS_ON_KITKAT_UPGRAGE, false))
		{
			/*
			 * On upgrade in kitkat or higher we need to reset sms setting preferences 
			 * as we are now removing these settings from UI.
			 */
			HikeSharedPreferenceUtil.getInstance().saveData(HAS_UNSET_SMS_PREFS_ON_KITKAT_UPGRAGE, true);
			Editor editor = preferenceManager.edit();
			editor.remove(HikeConstants.SEND_SMS_PREF);
			editor.remove(HikeConstants.RECEIVE_SMS_PREF);
			editor.commit();
		}
		Utils.setupServerURL(settings.getBoolean(HikeMessengerApp.PRODUCTION, true), Utils.switchSSLOn(getApplicationContext()));

		typingNotificationMap = new HashMap<String, TypingNotification>();

		stealthMsisdn = new HashSet<String>();

		initialiseListeners();

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

		makeNoMediaFiles();

		hikeBotNamesMap = new HashMap<String, String>();
		hikeBotNamesMap.put(HikeConstants.FTUE_TEAMHIKE_MSISDN, "team hike");
		hikeBotNamesMap.put(HikeConstants.FTUE_HIKEBOT_MSISDN, "Emma from hike");
		hikeBotNamesMap.put(HikeConstants.FTUE_GAMING_MSISDN, "Games on hike");
		hikeBotNamesMap.put(HikeConstants.FTUE_HIKE_DAILY, "hike daily");
		hikeBotNamesMap.put(HikeConstants.FTUE_HIKE_SUPPORT, "hike support");
		hikeBotNamesMap.put(HikeConstants.NUX_BOT, "Natasha");
		hikeBotNamesMap.put(HikeConstants.CRICKET_BOT, "Cricket");

		HikeConversationsDatabase.getInstance().addBotToHashMap(hikeBotNamesMap);
		initHikeLruCache(getApplicationContext());
		initContactManager();
		/*
		 * Fetching all stealth contacts on app creation so that the conversation cannot be opened through the shortcut or share screen.
		 */
		HikeConversationsDatabase.getInstance().addStealthMsisdnToMap();

		appStateHandler = new Handler();

		HikeMessengerApp.getPubSub().addListener(HikePubSub.CONNECTED_TO_MQTT, this);
		
		registerReceivers();

		if (!HikeSharedPreferenceUtil.getInstance().getData(HikePlatformConstants.CRICKET_PREF_NAME, false))
		{
			cricketBotEntry();
			HikeSharedPreferenceUtil.getInstance().saveData(HikePlatformConstants.CRICKET_PREF_NAME, true);
		}

	}

	// Hard coding the cricket bot on the App's onCreate so that there is a cricket bot entry
	// when there is no bot currently in the app. Using the shared prefs for that matter.
	// Hardcoding the bot name, bot msisdn and the bot chat theme. Can be updated using the
	// AC packet cbot and delete using the ac packet dbot.
	private void cricketBotEntry()
	{
		HikeHandlerUtil mThread = HikeHandlerUtil.getInstance();
		mThread.startHandlerThread();
		mThread.postRunnableWithDelay(new Runnable()
		{
			@Override
			public void run()
			{
				Logger.d("create bot", "cricket bot entry started");
				final JSONObject jsonObject = new JSONObject();
				try
				{
					jsonObject.put(HikeConstants.MSISDN, HikePlatformConstants.CRICKET_BOT_MSISDN);
					jsonObject.put(HikeConstants.NAME, HikePlatformConstants.CRICKET_BOT_NAME);
					jsonObject.put(HikeConstants.BOT_CHAT_THEME, HikePlatformConstants.CRICKET_CHAT_THEME_ID);

					BitmapDrawable drawable = (BitmapDrawable) getApplicationContext().getResources().getDrawable(R.drawable.cric_icon);
					String base64Icon = Utils.drawableToString(drawable);
					if (base64Icon != null)
					{
						jsonObject.put(HikeConstants.BOT_THUMBNAIL, base64Icon);
					}
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				MqttMessagesManager.getInstance(getApplicationContext()).createBot(jsonObject);
			}
		}, 0);

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
		// turn off future push notifications as soon as the app has
		// started.
		// this has to be turned on whenever the upgrade finishes.
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.UPGRADING, true);
		Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
		editor.putBoolean(BLOCK_NOTIFICATIONS, true);
		editor.commit();

		Intent msgIntent = new Intent(this, UpgradeIntentService.class);
		startService(msgIntent);
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
		cache = new HikeLruCache(params, getApplicationContext());
	}

	public static HikeLruCache getLruCache()
	{
		return cache;
	}

	private static ContactManager conMgr;

	private void initContactManager()
	{
		conMgr = ContactManager.getInstance();
		conMgr.init(getApplicationContext());
	}
	
	public static ContactManager getContactManager()
	{
		return conMgr;
	}

	private void makeNoMediaFiles()
	{
		String root = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT;

		File folder = new File(root + HikeConstants.PROFILE_ROOT);
		Utils.makeNoMediaFile(folder);

		folder = new File(root + HikeConstants.AUDIO_RECORDING_ROOT);
		Utils.makeNoMediaFile(folder);

		folder = new File(root + HikeConstants.IMAGE_ROOT + HikeConstants.SENT_ROOT);
		/*
		 * Fixed issue where sent media directory is getting visible in Gallery.
		 */
		Utils.makeNoMediaFile(folder, true);

		folder = new File(root + HikeConstants.VIDEO_ROOT + HikeConstants.SENT_ROOT);
		Utils.makeNoMediaFile(folder);

		folder = new File(root + HikeConstants.AUDIO_ROOT + HikeConstants.SENT_ROOT);
		Utils.makeNoMediaFile(folder);

		folder = new File(root + HikeConstants.AUDIO_RECORDING_ROOT + HikeConstants.SENT_ROOT);
		Utils.makeNoMediaFile(folder);

		folder = new File(root + HikeConstants.OTHER_ROOT + HikeConstants.SENT_ROOT);
		Utils.makeNoMediaFile(folder);
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

	public static void addStealthMsisdnToMap(String msisdn)
	{
		stealthMsisdn.add(msisdn);
	}

	public static void addNewStealthMsisdn(String msisdn)
	{
		addStealthMsisdnToMap(msisdn);
		getPubSub().publish(HikePubSub.STEALTH_CONVERSATION_MARKED, msisdn);
	}

	public static void removeStealthMsisdn(String msisdn)
	{
		removeStealthMsisdn(msisdn, true);
	}

	public static void removeStealthMsisdn(String msisdn, boolean publishEvent)
	{
		stealthMsisdn.remove(msisdn);
		if(publishEvent)
		{
			getPubSub().publish(HikePubSub.STEALTH_CONVERSATION_UNMARKED, msisdn);
		}
	}

	public static void clearStealthMsisdn()
	{
		stealthMsisdn.clear();
	}

	public static boolean isStealthMsisdn(String msisdn)
	{
		return stealthMsisdn.contains(msisdn);
	}

	public void initialiseListeners()
	{
		if (dbConversationListener == null)
		{
			dbConversationListener = new DbConversationListener(getApplicationContext());
		}
		if (toastListener == null)
		{
			toastListener = ToastListener.getInstance(getApplicationContext());
		}
		if (activityTimeLogger == null)
		{
			activityTimeLogger = new ActivityTimeLogger();
		}
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if(HikePubSub.CONNECTED_TO_MQTT.equals(type))
		{
			appStateHandler.post(appStateChangedRunnable);
		}
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

	private void performPreferenceTransition()
	{
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		if (!pref.getBoolean(HikeConstants.PREFERENCE_TRANSITION_SOUND_VIB_TO_LIST, false))
		{
			Editor edit = pref.edit();
			edit.putString(HikeConstants.NOTIF_SOUND_PREF, Utils.getOldSoundPref(this));
			edit.putString(HikeConstants.VIBRATE_PREF_LIST, Utils.getOldVibratePref(this));
			edit.putBoolean(HikeConstants.PREFERENCE_TRANSITION_SOUND_VIB_TO_LIST, true);
			edit.commit();
		}
	}
	
	public boolean isHikeBotNumber(String msisdn)
	{
		return hikeBotNamesMap.containsKey(msisdn);
	}
}
