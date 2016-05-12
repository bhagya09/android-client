package com.bsb.hike.chatthemes;

/**
 * Created by sriram on 22/02/16.
 */
public class HikeChatThemeConstants {
    // Bit Constants for Theme Type.
    public static final int THEME_TYPE_TILED = 1 << 0;

    public static final int THEME_TYPE_ANIMATED = 1 << 1;

    public static final int THEME_TYPE_CUSTOM = 1 << 2;

    // Identify the type of Asset, if it is an color / Bitmap / Nine Patch / Bease64String (Thumbnail) / Shape. These constants should be in sync with the server packet
    public static final byte ASSET_TYPE_UNKNOWN = -1;

    public static final byte ASSET_TYPE_COLOR = 0;

    public static final byte ASSET_TYPE_PNG = 1;

    public static final byte ASSET_TYPE_JPEG = 2;

    public static final byte ASSET_TYPE_NINE_PATCH = 3;

    public static final byte ASSET_TYPE_BASE64STRING = 4;

    public static final byte SYSTEM_MESSAGE_TYPE_DEFAULT = 0;

    public static final byte SYSTEM_MESSAGE_TYPE_LIGHT = 1;

    public static final byte SYSTEM_MESSAGE_TYPE_DARK = 2;


    // Asset Download is a Success / Failure
    public static final byte ASSET_DOWNLOAD_STATUS_NOT_DOWNLOADED = 0;

    public static final byte ASSET_DOWNLOAD_STATUS_DOWNLOADING = 1;

    public static final byte ASSET_DOWNLOAD_STATUS_DOWNLOADED_SDCARD = 2;

    public static final byte ASSET_DOWNLOAD_STATUS_DOWNLOADED_APK = 3;

    // File Extension Types
    public static final String FILEEXTN_9PATCH = ".9.png";

    public static final String FILEEXTN_PNG = ".png";

    public static final String FILEEXTN_JPG = ".jpg";

    public static final String FILEEXTN_JPEG = ".jpeg";

    // JSON Signal Constants
    public static final String JSON_SIGNAL_THEME_DATA = "theme_data";

    public static final String JSON_SIGNAL_THEME_META = "theme_meta";

    public static final String JSON_SIGNAL_NEW_THEME = "addCbg";

    public static final String JSON_SIGNAL_DEL_THEME = "del_cbg";

    public static final String JSON_SIGNAL_THEME_THEMEID = "theme_id";

    public static final String JSON_SIGNAL_THEME_VISIBILITY = "visibility";

    public static final String JSON_SIGNAL_THEME_ORDER = "order";

    public static final String JSON_SIGNAL_THEME_SYSTEM_MESSAGE = "sysMessageType";

    public static final String JSON_SIGNAL_THEME_THEMESTATE = "theme_state";

    public static final String JSON_SIGNAL_THEME_BG_PORTRAIT = "bg_portrait";

    public static final String JSON_SIGNAL_THEME_BG_LANDSCAPE = "bg_landscape";

    public static final String JSON_SIGNAL_THEME_ACTION_BAR = "header";

    public static final String JSON_SIGNAL_THEME_CHAT_BUBBLE_BG = "chat_bubble";

    public static final String JSON_SIGNAL_THEME_SENT_NUDGE = "send_nudge";

    public static final String JSON_SIGNAL_THEME_RECEIVE_NUDGE = "receive_nudge";

    public static final String JSON_SIGNAL_THEME_INLINE_STATUS_BG = "inline_update_bg";

    public static final String JSON_SIGNAL_THEME_MULTI_SELECT_BUBBLE = "multi_select_bubble";

    public static final String JSON_SIGNAL_THEME_OFFLINE_MSG_BG = "offline_message_color";

    public static final String JSON_SIGNAL_THEME_STATUS_BAR_BG = "status_bar";

    public static final String JSON_SIGNAL_THEME_SMS_TOGGLE_BG = "sms_bg";

    public static final String JSON_SIGNAL_THEME_BUBBLE_COLOR = "bubble_color";

    public static final String JSON_SIGNAL_THEME_THUMBNAIL = "thumbnail";

    public static final String JSON_SIGNAL_ASSET_TYPE = "type";

    public static final String JSON_SIGNAL_ASSET_VALUE = "value";

    public static final String JSON_SIGNAL_ASSET_SIZE = "size";

    // asset indexes
    public static final byte ASSET_INDEX_BG_PORTRAIT = 0;

    public static final byte ASSET_INDEX_BG_LANDSCAPE = 1;

    public static final byte ASSET_INDEX_ACTION_BAR_BG = 2;

    public static final byte ASSET_INDEX_CHAT_BUBBLE_BG = 3;

    public static final byte ASSET_INDEX_SENT_NUDGE_BG = 4;

    public static final byte ASSET_INDEX_RECEIVED_NUDGE_BG = 5;

    public static final byte ASSET_INDEX_INLINE_STATUS_MSG_BG = 6;

    public static final byte ASSET_INDEX_MULTISELECT_CHAT_BUBBLE_BG = 7;

    public static final byte ASSET_INDEX_OFFLINE_MESSAGE_BG = 8;

    public static final byte ASSET_INDEX_STATUS_BAR_BG = 9;

    public static final byte ASSET_INDEX_SMS_TOGGLE_BG = 10;

    public static final byte ASSET_INDEX_THUMBNAIL = 11;

    public static final byte ASSET_INDEX_BUBBLE_COLOR = 12;

    public static final byte ASSET_INDEX_COUNT = 13;

    //to be defined in the same order of asset indexs
    public static final String[] JSON_SIGNAL_THEME = {JSON_SIGNAL_THEME_BG_PORTRAIT, JSON_SIGNAL_THEME_BG_LANDSCAPE, JSON_SIGNAL_THEME_ACTION_BAR, JSON_SIGNAL_THEME_CHAT_BUBBLE_BG,
            JSON_SIGNAL_THEME_SENT_NUDGE, JSON_SIGNAL_THEME_RECEIVE_NUDGE, JSON_SIGNAL_THEME_INLINE_STATUS_BG, JSON_SIGNAL_THEME_MULTI_SELECT_BUBBLE, JSON_SIGNAL_THEME_OFFLINE_MSG_BG,
            JSON_SIGNAL_THEME_STATUS_BAR_BG, JSON_SIGNAL_THEME_SMS_TOGGLE_BG, JSON_SIGNAL_THEME_THUMBNAIL, JSON_SIGNAL_THEME_BUBBLE_COLOR};

    // Bit Constants for Asset Status.
    public static final int ASSET_STATUS_BG_PORTRAIT = 1 << ASSET_INDEX_BG_PORTRAIT;

    public static final int ASSET_STATUS_BG_LANDSCAPE = 1 << ASSET_INDEX_BG_LANDSCAPE;

    public static final int ASSET_STATUS_ACTION_BAR_BG = 1 << ASSET_INDEX_ACTION_BAR_BG;

    public static final int ASSET_STATUS_CHAT_BUBBLE_BG = 1 << ASSET_INDEX_CHAT_BUBBLE_BG;

    public static final int ASSET_STATUS_SENT_NUDGE_BG = 1 << ASSET_INDEX_SENT_NUDGE_BG;

    public static final int ASSET_STATUS_RECEIVED_NUDGE_BG = 1 << ASSET_INDEX_RECEIVED_NUDGE_BG;

    public static final int ASSET_STATUS_INLINE_UPDATE_BG = 1 << ASSET_INDEX_INLINE_STATUS_MSG_BG;

    public static final int ASSET_STATUS_MULTISELECT_BUBBLE_BG = 1 << ASSET_INDEX_MULTISELECT_CHAT_BUBBLE_BG;

    public static final int ASSET_STATUS_OFFLINE_MESSAGE_BG = 1 << ASSET_INDEX_OFFLINE_MESSAGE_BG;

    public static final int ASSET_STATUS_STATUS_BAR_BG = 1 << ASSET_INDEX_STATUS_BAR_BG;

    public static final int ASSET_STATUS_SMS_TOGGLE_BG = 1 << ASSET_INDEX_SMS_TOGGLE_BG;

    public static final int ASSET_STATUS_THUMBNAIL = 1 << ASSET_INDEX_THUMBNAIL;

    public static final int ASSET_STATUS_BUBBLE_COLOR = 1 << ASSET_INDEX_BUBBLE_COLOR;

    // Bit Constant to check if all the assets are download, in our case it is 1 1111 1111 1111. (for 13 Assets)
    public static final int ASSET_STATUS_DOWNLOAD_COMPLETE = ASSET_STATUS_BG_PORTRAIT | ASSET_STATUS_BG_LANDSCAPE | ASSET_STATUS_ACTION_BAR_BG | ASSET_STATUS_CHAT_BUBBLE_BG
            | ASSET_STATUS_SENT_NUDGE_BG | ASSET_STATUS_RECEIVED_NUDGE_BG | ASSET_STATUS_INLINE_UPDATE_BG | ASSET_STATUS_MULTISELECT_BUBBLE_BG | ASSET_STATUS_OFFLINE_MESSAGE_BG
            | ASSET_STATUS_STATUS_BAR_BG | ASSET_STATUS_SMS_TOGGLE_BG | ASSET_STATUS_THUMBNAIL | ASSET_STATUS_BUBBLE_COLOR;


    // JSON Constants
    public static final String JSON_DWNLD_ASSET_ID = "asset_ids";

    public static final String JSON_DWNLD_THEME_ID = "theme_ids";

    public static final String CHAT_THEMES_ROOT = "chatThemes";

    public static final String CHAT_THEME_ID_DOWNLOADING = "1";

    public static final String CHAT_THEME_ID_NOT_DOWNLOADED = "0";

    public static final String CHAT_THEME_ID_DOWNLOADED = "2";

    public static final String CHATTHEMES_DEFAULT_JSON_FILE_NAME = "chatthemes_data";

	public static final String MIGRATE_CHAT_THEMES_DATA_TO_DB = "migrateChatThemesToDB";

	public static final String THEME_PALETTE_CAMERA_ICON = "camera";
}
