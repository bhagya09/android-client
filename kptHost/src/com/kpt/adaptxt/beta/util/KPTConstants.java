package com.kpt.adaptxt.beta.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class KPTConstants {

	public static final String MARKET_SEARCH_WORD = "market://details?id=";

	public static final String PREF_CORE_MAINTENANCE_MODE = "maintanence_mode";
	//public static final String PREF_CORE_LICENSE_EXPIRED = "base_license_expired";
	public static final String PREF_KPT_IME_RUNNING="kpt_ime_running";
	public static final String PREF_PHONE_ATX_STATE = "phone_atx_state";
	//public static final String PREF_INMOBILE_AD_REGISTER = "is_registered";
	public static final String PREF_IS_PROFILE_RESTORE = "is_restore_profile";
	//Settings Related Preferences
	 public static String PREF_KEY_SOUND="current_key_sound";
	 public static String PREF_HIDE_NOTIFICATION="hide_notification";
	    public static String PREF_KEY_VIBRATION="current_vibration_rate";
	    public static String PREF_SUGGESTION_ENABLE="enable_suggestonbar";
	    public static String PREF_KEY_POPUP="current_popup_rate";
	    public static final int DEFAULT_POPUP=300;
	    public static final float DEFAULT_VOLUME=3.0f;
	    public static final int DEFAULT_VIBRATION=3;
	//Language related sharedpreferences
	public static final String PREF_IS_LANGUAGE_LIST_DIRTY = "lang_list_dirty";


	//Base upgrade related sharedpreferences
	//public static final String PREF_SHOW_BASE_UPDATE_INCOMPATIBLE_DIALOG = "base_update_incomaptible_dialog";
	public static final String PREF_BASE_HAS_BEEN_UPGRADED= "backup_dict";
	//public static final String PREF_BASE_IS_UPGRADING = "base_upgrading";
	//public static final String PREF_BASE_UPGRADE_PREV_ADDON_VERSION= "add_version";
	//public static final String PREF_OLD_ADDONS_UPGRADE = "upgrading_old_dicts";


	//Addon related sharedpreferences
	public static final String PREF_IS_ADDON_INSTALLED = "isaddoninstalled";
	public static final String PREF_ADDON_DOWNLOAD_INPROGRESS = "addon_download";
	public static final String PREF_DOWNLOAD_STARTED = "download_started";
	public static final String PREF_CORE_IS_PACKAGE_INSTALLATION_INPROGRESS = "package_install";
	public static final String PREF_CORE_IS_PACKAGE_UNINSTALLATION_INPROGRESS = "package_uninstall";
	public static final String PREF_CORE_IS_PACKAGE_IN_QUEUE = "package_in_queue";
	public static final String PREF_CORE_INSTALL_PACKAGE_NAME = "package_name";
	public static final String PREF_CORE_UNINSTALL_PACKAGE_NAME = "un_package_name";
	public static final String PREF_CORE_INITIALIZED_FIRST_TIME = "core_initialized";
	public static final String PREF_SHOW_INCOMPATIBLE_UPDATE_DIALOG = "incomaptible_dialog";


	//Keyboard customization preferences
	public static final String PREF_CUST_KB_BG_PATH = "kb_cust_keyboard_background";
	public static final String PREF_CUST_FONT_STYLE = "kb_cust_font_type";
	public static final String PREF_CUST_SUGGESTION_HEIGHT = "kb_cust_suggestion_height";
	public static final String PREF_CUST_SUGGESTION_LANDSCAPE_HEIGHT = "kb_cust_suggestion_landscape_height";

	public static final String PREF_CUST_FONT_SIZE = "kb_cust_font_size";
	public static final String PREF_CUSTOM_BASE_VAL = "cust_height_base_val";
	public static final String PREF_CUSTOM_BASE_VAL_LANDSCAPE = "cust_height_base_val_landscape";
	public static final String PREF_CUSTOMIZATION_CLOSED = "customization_activity_closed";
	public static final String PREF_CUST_GLIDE_TRACE_PATH = "kb_cust_glide_trace_color";


	//Social Accounts Learning preferences
	public static final String PREF_LEARNING_UPDATE_ALL = "update_all";
	public static final String PREF_LEARNING_SMS = "learning_sms";
	public static final String PREF_LEARNING_PHONE = "learning_phone";
	public static final String PREF_LEARNING_FACBOOK = "learning_facebook";
	public static final String PREF_LEARNING_TWITTER = "learning_twitter";
	public static final String PREF_LEARNING_GMAIL = "Gmail";
	public static final String PREF_FACEBOOK_LOGGED_FIRST_TIME="kpt_facebook_first_launch";
	public static final String PREF_TWITTER_LOGGED_FIRST_TIME="kpt_twitter_first_launch";
	public static final String PREF_GMAIL_LOGGED_FIRST_TIME="kpt_gmail_first_launch";
	public static final String PREF_ELAPSED_TIME_STAMP="elapsed_timeStamp";
	public static final String PREF_ELAPSED_ACCOUNT_TYPE="elapsed_accountType";
	public static final String PREF_CORE_LEARNING_INPROGRESS = "Core_learning";
	public static final String PREF_DONT_LEARN_WHILE_POSTING = "dont_learn_while_posting";
	public static final String SEARCH_PREINSTALL_ADDONS = "search_for_pre_installed_addons";
	public static final String PREF_FB_CLICK = "facebook_click_flag";


	//Keyboard type preferences
	public static final String PREF_PORT_KEYBOARD_TYPE = "layout_portrait";
//	public static final String PREF_LAND_KEYBOARD_TYPE = "layout_landscape";
	public static final String PREF_CURRENT_KEYBOARD_TYPE = "keyboard_types";


	//Settings screen shared preferences
	public static final String PREF_AUTOCORRECTION = "autocorrection";
	public static final String PREF_AUTOCORRECTION_MODE = "autocorrection_mode";
	public static final String PREF_DISPLAY_ACCENTS = "Display_Accents";
	public static final String PREF_VIBRATE_ON = "vibrate_on";
	public static final String PREF_SOUND_ON = "sound_on";
	public static final String PREF_POPUP_ON = "popup_on";
	public static final String PREF_ATR_FEATURE = "atr_feature";
	public static final String PREF_AUTO_CAPITALIZATION = "auto_capitalization";
	public static final String PREF_PRIVATE_MODE = "private_mode";
	public static final String PREF_THEME_MODE = "theme_mode";
	public static final String PREF_GLIDE = "glide";

	public static final String PREF_APP_CTXT_SUGGS = "app_context_feature";

	//Eula SharedPreferences
	public static final String PREFERENCE_EULA_ACCEPTED = "eula.accepted";
	public static String PREFERENCE_SETUP_COMPLETED = "setup_wizard_completed";

	//Addon Compatibility sharedpreferences
	public static final String PREF_CURRENT_COMPATIBILITY_XML_VERSION = "current_base_version";
	public static final String PREF_CURRENT_COMPATIBILITY_THEMES_XML_VERSION = "current_master_themes_version";
	
	public static final String PREF_COMPATIBILITY_XML_VER_LAST_CHECK_TIME = "compatibility_last_download_time";
	public static final String PREF_POPULAR_XML_VER_LAST_CHECK_TIME = "popular_lats_download_time";
	public static final String PREF_CURRENT_LATEST_ADDONS_VERSION = "current_latest_addons_ver";
	//public static final String PREF_PUNCTUTION_PREDICTION = "punctutionprediction";



	//Used for addon installation and uninstalltion (Moved from package handler service)
	public static int SIMPLE_NOTFICATION_ID;
	public static final String PACKAGE_NAME = "com.kpt.adaptxt.beta.PACKAGE_NAME";
	public static final String KPT_ACTION_PACKAGE_UNINSTALL = "com.kpt.adaptxt.beta.PACKAGE_UNINSTALL";
	public static final String KPT_ACTION_PACKAGE_BASE_UPGRADE = "base_upgraded";
	public static final String KPT_ACTION_PACKAGE_DOWNLOAD_INSTALL = "download_install";
	public static final String KPT_ACTION_BROADCAST_THEME_ADDON_INSTALLED = "addon_theme_installed";
	public static final String INTENT_EXTRA_URL = "url";
	public static final String INTENT_EXTRA_ZIP_FILE_NAME = "zipfile";
	public static final String INTENT_EXTRA_FILE_NAME = "filename";
	//public static final String INTENT_EXTRA_ADDON_VER_XML = "curr_version";
	public static final String ADDON_EXTENSION = "_Android_KPT_smartphone";
	public static final String ADDON_ATP_EXTENSION = ".atp";
	public static final String ADDON_XML_EXTENSION = ".xml";
	public static final String DOWNLOAD_TASK_SUCCESS = "success";
	public static final String DOWNLOAD_TASK_FAILED = "failed";
	
	


	//Keyboard key constatnts
	// Keyboard XML Tags
	public static final int KEYCODE_SHIFT = -1;
	public static final int KEYCODE_MODE_CHANGE = -2;
	public static final int KEYCODE_CANCEL = -3;
	public static final int KEYCODE_DONE = -4;
	public static final int KEYCODE_DELETE = -5;
	public static final int KEYCODE_ALT = -6;
	public static final int KEYCODE_SMILEY = -7;
	public static final int KEYCODE_JUMP_TO_TERTIARY = -8;
	public static final int KEYCODE_MODE_CHANGE_SHIFT = -9;
	public static final int KEYCODE_SPACE = 32;
	public static final int KEYCODE_ENTER = 10;
	public static final int KEYCODE_XI = 1;
	public static final int KEYCODE_ADAPTXT = 2;
	public static final int KEYCODE_PERIOD = 46;
	public static final int KEYCODE_DELETE_LONGPRESS = -106;
	public static final int KEYCODE_DANDA = 2404;
	public static final int KEYCODE_MIC = 44;
	public static final int KEYCODE_DOT_COMMA = -21;
	public static final int KEYCODE_AT = 64;
	public static final int KEYCODE_PHONE_LANGUAGE_AND_SHARE = -13;
	public static final int KEYCODE_PHONE_MODE_SYM = -15; 
	public static final int KEYCODE_PHONE_MODE_SHIFT_SYM = -16;
	public static final int KEYCODE_ATX = -17;
	public static final int KEYCODE_PHONE_SYM_PAGE_CHANGE = -18;
	public static final int KEYCODE_ZERO = 48;
	public static final int KEYCODE_LOOKUP = -555;
	public static final int KEYCODE_CONJUNCTION = -1073;
	public static final int KEYCODE_SHIFTINDIC = -19;
	public static final int KEYCODE_INFINITY = 8734;
	public static final int KEYCODE_THAI_COMMIT = 294;
	public static final int KEYCODE_THAI_SHIFT = 7541;
	public static final int KEYCODE_THAI_SYMBOLBUBBLE = 7542;
	public static final int KEYCODE_CARET = 94;
	public static final int KEYCODE_NEPALI_MARATHI_ZERO = 2406;
	public static final int KEYCODE_BENGALI_ZERO = 2534;
	public static final int KEYCODE_OPTIONS = -100;
	public static final int KEYCODE_SHIFT_LONGPRESS = -101;
	public static final int KEYCODE_VOICE = -102;
	public static final int KEYCODE_F1 = -103;
	public static final int KEYCODE_NEXT_LANGUAGE = -104;
	public static final int KEYCODE_PREV_LANGUAGE = -105;
	public static final int KEYCODE_SPACE_DOUBLE_TAP = -107;
	public static final int KEYCODE_SHIFT_CHANGE = -121;
	public static final int KEYCODE_ADD_LINE = -110;
	public static final int KEYCODE_RIGHT_ARROW = -120;
	public static final int KEYCODE_DOUBLE_CONSONANT = -108;
	public static final int KEYCODE_LOCALE_SWITCH = -23;
	
	public static final int KEYCODE_DISPLAY_CLIPBOARD = -130;
	public static final int KEYCODE_LAUNCH_SETTINGS = -131;
	public static final int KEYCODE_DOT = 46;
	public static final int KEYCODE_LAUNCH_QUICK_SETTINGS_DIALOG = -132;
	public static final int KEYCODE_LAUNCH_SHARE_DIALOG = -133;
	public static final int KEYCODE_SPACE_LONGPRESS_START = -134;
	public static final int KEYCODE_SPACE_LONGPRESS_END = -135;
	public static final int KEYCODE_GLOBE = -136;

	public static final int NUMBER_OF_POPUPCHARS_INROW = 9;

	/**
	 * Constants Related To Themes
	 */
	public static final int BRIGHT_THEME = 1;
	public static final int THEME_ENABLE = 1;
	public static final int THEME_INTERNAL = 0;
	public static final int THEME_EXTERNAL = 1;
	public static final int THEME_CUSTOM = -1;
	public static final int DEFAULT_THEME = 0;

	public static final int MAX_USER_DICTIONARY_WORD_LIMIT = 1500;
	public static final String THAI_LANGUAGE = "Thai";
	
	
	public static final String BACKUP_FOLDER = ".adaptxt_back_up";
	public static final String BACKUP_THEME_XML_FILE = "/themedata.xml";
	public static final String BACKUP_VERSION_XML_FILE = "/versionInfo.xml";
	public static final String BACKUP_SHARED_PREF_XML_FOLDER = "/shared_prefs";
	public static final String BACKUP_SETTING_PREF_XML_FILE = "/com.kpt.adaptxt.beta_preferences.xml";
	public static final String BACKUP_SPECIAL_SETTING_PREF_XML_FILE = "/prefs.xml";
	public static final String BACKUP_APP_SHORTCUTS_PREF_XML_FILE = "/applications_shortcuts_pref.xml";
	public static final String BACKUP_CLIPBOARD_SHORTCUTS_PREF_XML_FILE = "/clipboard_shortcuts_pref.xml";
	public static final String BACKUP_WEB_SHORTCUTS_PREF_XML_FILE = "/websites_shortcuts_pref.xml";
	public static final String BACKUP_PROFILE_FOLDER = "/profile_store";
	/**
	 *  Constants related to determine Keyboard type (qwerty or 12 key)
	 * 
	 */
	public static final String KEYBOARD_TYPE_QWERTY = "0";
	public static final String KEYBOARD_TYPE_PHONE = "1";

	public static boolean mXi_enable = false; //variable to store whether xi key enable or not
	public static boolean mIsNavigationBackwar = true;// variable to know the backward navigation

	private static final int TEXT_BEFORE_CONSTANT = 1000;

	public static final List<String> FB_PUBLISH_PERMISSIONS = Arrays.asList("publish_actions");
	public static final List<String> FB_READ_PERMISSIONS = Arrays.asList("read_stream", "read_mailbox",
			"user_about_me", "user_activities", "user_birthday",
			"user_checkins", "user_education_history", "user_events",
			"user_hometown", "user_interests", "user_likes",
			"user_location", "user_notes", "user_groups",
			"user_religion_politics", "email",
			"user_status", "user_website", "user_work_history");

	/**
	 *  Constatnts related to application package name
	 * 
	 */
	public static final String PACKAGE_NAME_QUICK_OFFICE = "com.quickoffice.android";
	public static final String PACKAGE_NAME_EMIAL = "com.android.email";
	public static final String PACKAGE_NAME_EMAIL_DEFAULT_LG = "com.lge.email";
	public static final String PACKAGE_NAME_POLARIS_OFFICE_SAMSUNG_STORE = "com.infraware.polarisoffice4";
	public static final String PACKAGE_NAME_EVERNOTE = "com.evernote";
	public static final String PACKAGE_NAME_CHATON = "com.sec.chaton";
	public static final String PACKAGE_NAME_VIBER =  "com.viber.voip";
	public static final String PACKAGE_NAME_POLARIS = "com.infraware";
	public static final String PACKAGE_NAME_KINGSOFT = "cn.wps.moffice_eng";
	public static final String PACKAGE_NAME_POLARIS_OFFICE_HTC_INBUILT = "com.infraware.docmaster";
	public static final String PACKAGE_NAME_POLARIS_OFFICE = "com.infraware.polarisoffice";
	public static final String SAMSUNG_CALCULATOR = "com.sec.android.app.popupcalculator";
	public static final String PACKAGE_NAME_HTC_NOTE ="com.htc.notes";
	public static final String PACKAGE_NAME_HTC_MAIL ="com.htc.android.mail";
	public static final String SEARCH_EDITOR = "com.google.android.googlequicksearchbox";
	public static final String PACKAGE_NAME_CHROME_WEB_EDITORS = "com.android.chrome";
	public static final int TEAM_VIEWR_MAIL_ENTER_ACTION = 1073741830;
	public static final String TEAM_VIEWR_EDITOR_PACKAGE_NAME = "com.teamviewer.teamviewer.market.mobile";
	public static final String ONE_NOTE_APPLICATION ="com.microsoft.office.onenote";//Fix for TP Item - 13415
	public static final String S_NOTE_APPLICATION_SAMSUNG ="com.sec.android.app.snotebook";
	public static final String PACKAGE_NAME_GOOGLE_MAPS ="com.google.android.apps.maps";
	public static final String PACKAGE_NAME_WHATSAPP ="com.whatsapp";
	public static final int UNKNOWN_EDITOR_TYPE_EMAIL = 0;
	public static final int UNKNOWN_EDITOR_TYPE_POLARIS_OFFICE_SAMSUNG_STORE = 1;
	public static final int UNKNOWN_EDITOR_TYPE_EVERNOTE = 2;
	public static final int UNKNOWN_EDITOR_TYPE_KINGSOFT = 3;
	public static final int UNKNOWN_EDITOR_TYPE_POLARIS_OFFICE_HTC_INBUILT = 4;
	public static final int UNKNOWN_EDITOR_TYPE_SAMSUNG_CALCULATOR = 5;
	public static final int UNKNOWN_EDITOR_TYPE_HTC_NOTE = 6;
	public static final int UNKNOWN_EDITOR_TYPE_SEARCH_EDITOR = 7;
	public static final int UNKNOWN_EDITOR_TYPE_HTC_MAIL = 8;
	public static final int UNKNOWN_EDITOR_TYPE_CHROME_WEB_EDITORS = 9;
	public static final int UNKNOWN_EDITOR_TYPE_S_NOTE = 10;
	public static final int UNKNOWN_EDITOR_TYPE_LG_EMAIL = 11;
	public static final int UNKNOWN_EDITOR_TYPE_QUICK_OFFICE = 12;
	
	/**
	 * Number of time dictionary installation should be re-tried on a failure case
	 */
	public static final int NO_OF_RETRY_INSTALLATION = 3;
	
	/**
	 * Interval based on which the next retry of add-on installation will be done on a failure case
	 */
	public static final int INSTALLATION_RETRY_DELAY = 1000*5; 
	
	
	/**
	 * Constants for XI & Acc hide options
	 */
	public static final String PREF_HIDE_XI_KEY = "hide_xi_pref";
	public static final String PREF_HIDE_ACC_KEY = "hide_acc_pref";
	
	public static final String PREFS_ADDONS_UPDATE_COUNT = "addons_update_count";
	
	public static final String APP_PACKAGE_NAME = "com.kpt.adaptxt.beta";
	public static final String APP_PACKAGE_NAME_PRO = "com.kpt.adaptxt.premium";
	public static final String KPT_ACTION_BACKUP_DATA = "backup_data";
	public static final String KPT_ACTION_RESTORE_DATA = "restore_data";
	
	public static final String KPT_ACTION_BACKUP_DATA_TYPE = "backup_data_type";
	public static final String KPT_ACTION_RESTORE_DATA_TYPE = "restore_data_type";
	
	public static final String KPT_ACTION_BACKUP_DATA_TYPE_PROFILE = "backup_data_profile";
	public static final String KPT_ACTION_BACKUP_DATA_TYPE_DATABASE = "backup_data_database";
	public static final String KPT_ACTION_BACKUP_DATA_TYPE_SHARED_PREF = "backup_data_sharedprefs";
	
	public static final String KPT_ACTION_RESTORE_DATA_TYPE_PROFILE = "restore_data_profile";
	public static final String KPT_ACTION_RESTORE_DATA_TYPE_DATABASE = "restore_data_database";
	public static final String KPT_ACTION_RESTORE_DATA_TYPE_SHARED_PREF = "restore_data_sharedprefs";

	public static final String KPT_ACTION_BACKUP_DATA_TYPE_TIMED_BACK_UP = "timed_backup_data";
	
	//Adaptxt V3.0 sailent updates for dictionaries , learn and unlearn trends
	public static final String KPT_ACTION_PACKAGE_UPDATE = "com.kpt.adaptxt.PACKAGE_UPDATE";
	public static final String KPT_ACTION_PACKAGE_LEARN_TREND = "com.kpt.adaptxt.PACKAGE_LEARN_TREND";
	public static final String KPT_ACTION_PACKAGE_UNLEARN_TREND = "com.kpt.adaptxt.PACKAGE_UPNLEARN_TREND";
	

	
	
	
	public static final String LOCATION_PREF = "location_pref";
	//public static final String KPT_APP_BASED_SUGGESTION = "application_based_suggestion";
	
	public static final String EXTERNAL_THEMMES_ZIPNAME = "ExternalThemes.zip";
	
	public static final String RESULT_STATUS = "Status";



	public static final String RESULT_CODE = "Code";

	public static final String RESULT_DESC = "Description";
	public static final int RESULT_SUCESS = 0;
	
	public static final String PREF_SILENT_UPDATE = "silent_update";
	
	public static final ArrayList<String> SMS_APPLICATON_LIST = new ArrayList<String>(){{
		add("com.android.mms");
		add("com.whatsapp");
		add("com.handcent.nextsms");
		add("com.facebook.orca");
		add("jp.naver.line.android");
		add("com.google.android.talk");
		add("com.skype.raider");
		add("com.yahoo.mobile.client.android.im");
		add("com.google.android.talk");
		add("com.bsb.hike");
		add("com.tencent.mm");
		add("com.viber.voip");
		add("com.sec.chaton");
		add("com.bbm");
		add("com.jb.gosms");
		add("com.sgiggle.production");
		add("kik.android");
		add("com.kakao.talk");
		add("com.spartancoders.gtok");
		add("com.sonyericsson.conversations");		
	}};
	
	public static final ArrayList<String> EMAIL_APPLICATON_LIST = new ArrayList<String>(){{
		add("com.htc.android.mail");
		add("com.android.mail");
		add("com.google.android.email");
		add("com.android.email");
		add("org.kman.AquaMail");
		add("com.gau.go.launcherex.gowidget.emailwidget");
		add("ru.mail.mailapp");
		
	}};
	public static final ArrayList<String> GMAIL_APPLICATON_LIST = new ArrayList<String>(){{
		add("com.google.android.gm");
		add("com.yahoo.mobile.client.android.mail");
		
	}};
	public static final ArrayList<String> SOCIAL_APPLICATON_LIST = new ArrayList<String>(){{
		add("com.facebook.katana");
		add("com.twitter.android");
		add("com.google.android.apps.plus");
		add("com.instagram.android");
		add("com.pinterest");
		add("com.hootsuite.droid.full");		
		
	}};

	public static final String PREF_AUTO_SPACE = "auto_spacing";


	public static final int MAX_NEARBY_KEYS = 12;
	
	/**
	 * glide related constants
	 */
	public static final int TEXT_ENTRY_TYPE_NORMAL = 0;
	public static final int TEXT_ENTRY_TYPE_GLIDE = 1;
	
	//types of glide suggestions
	public static final int GLIDE_SUGG_NORMAL = 0;
	public static final int GLIDE_SUGG_COMPLETION = 1;
	
	public static final String NEW_STRING_ADDONS = "(New)";

	public static final String PREMIUMUSER_NOT_CHECKED = "premium_user_not_check";
	
	

	public static final int  ATXAppCtxtGeneric = 1;
	public static final int  ATXAppCtxtGmail   = 2;
	public static final int ATXAppCtxtEmail   = 3;
	public static final int ATXAppCtxtSocial  = 4;
	public static final int ATXAppCtxtSMS     = 5;
	public static final int ATXAppCtxtPrivate = 6;
	
	
	public static final String PREF_LOCATION_DICT_ALREADY_INSTALLED = "loc_dict_already_installed";

	public static final int MULTI_LINES = 17;
	public static final int MULTI_LINES_ADAPTXT = 131073;
	public static final int MULTILINE_LINE_EDITOR = 0;
	public static final int SINGLE_LINE_EDITOR = 1;
	public static final String ATX_ASSETS_FOLDER = "adaptxt/";
	
	
	public static final String XML_CONTENT_TAG = "content";
	public static final String BASE_VERSION_FILE_NAME = "version";
	public static final String XML_CONTENTS_TAG = "contents";
	public static final String XML_ADDON_TAG = "addon";
	public static final String XML_DICT_DISPLAY_NAME = "displayname";
	public static final String XML_DICT_FILE_NAME = "filename";
	public static final String XML_ZIP_FILENAME = "zipfilename";
	public static final String XML_SEARCH = "searchstring";
	public static final String XML_TYPE = "type";
	public static final String XML_CURRENT_VERSION = "currentver";
	public static final String XML_PREVIOUS_VERSION = "previousversions";
	public static final String XML_COMP_BASE_VERSION = "compatiblebasever";
	public static final String XML_VERSION = "ver";
	public static final String XML_ATTR_NO = "no";
	public static final String XML_BASE_URL = "baseurl";
	public static final String XML_PRICE = "price";
	
	
	public static final String ACTION_BASE_PACKAGE_ADDED = "action_base_package_added";
	public static final String ACTION_BASE_PACKAGE_REMOVED = "action_base_package_removed";
	public static final String ACTION_BASE_PACKAGE_REPLACED = "action_base_package_replaced";
	
	public static final String PREFS_UNSUPPORTED_LANG_STATUS = "unsupported_languages";
	public static final String KPT_CHANGE_LANG_ID = "change_component_id";
}
