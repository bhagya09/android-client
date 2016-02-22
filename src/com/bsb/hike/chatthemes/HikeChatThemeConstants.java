package com.bsb.hike.chatthemes;

/**
 * Created by sriram on 22/02/16.
 */
public class HikeChatThemeConstants {
    public static final int THEME_TILED = 1 << 0;
    public static final int THEME_ANIMATED = 1 << 1;

    // Identify the type of Asset, if it is an color / Bitmap / Nine Patch / Bease64String (Thumbnail) / Shape
    public static final byte ASSETTYPE_UNKNOWN = -1;
    public static final byte ASSETTYPE_COLOR = 0;
    public static final byte ASSETTYPE_BITMAP = 1;
    public static final byte ASSETTYPE_NINE_PATCH = 2;
    public static final byte ASSETTYPE_BASE64STRING = 3;
    public static final byte ASSETTYPE_SHAPE = 4;

    public static final byte ASSET_BG_PORTRAIT = 0;
    public static final byte ASSET_BG_LANDSCAPE = 1;
    public static final byte ASSET_ACTION_BAR_BG = 2;
    public static final byte ASSET_CHAT_BUBBLE_BG = 3;
    public static final byte ASSET_SENT_NUDGE_BG = 4;
    public static final byte ASSET_RECEIVED_NUDGE_BG = 5;
    public static final byte ASSET_INLINE_STATUS_MSG_BG = 6;
    public static final byte ASSET_MULTISELECT_CHAT_BUBBLE_BG = 7;
    public static final byte ASSET_OFFLINE_MESSAGE_BG = 8;
    public static final byte ASSET_STATUS_BAR_BG = 9;
    public static final byte ASSET_SMS_TOGGLE_BG = 10;
    public static final byte ASSET_THUMBNAIL = 11;
    public static final byte ASSET_COUNT = 12;

    // Bit Constants for Asset Status.
    public static final int ASSET_STATUS_BG_PORTRAIT = 1 << ASSET_BG_PORTRAIT;
    public static final int ASSET_STATUS_BG_LANDSCAPE = 1 << ASSET_BG_LANDSCAPE;
    public static final int ASSET_STATUS_ACTION_BAR_BG = 1 << ASSET_ACTION_BAR_BG;
    public static final int ASSET_STATUS_CHAT_BUBBLE_BG = 1 << ASSET_CHAT_BUBBLE_BG;
    public static final int ASSET_STATUS_SENT_NUDGE_BG = 1 << ASSET_SENT_NUDGE_BG;
    public static final int ASSET_STATUS_RECEIVED_NUDGE_BG = 1 << ASSET_RECEIVED_NUDGE_BG;
    public static final int ASSET_STATUS_INLINE_UPDATE_BG = 1 << ASSET_INLINE_STATUS_MSG_BG;
    public static final int ASSET_STATUS_MULTISELECT_BUBBLE_BG = 1 << ASSET_MULTISELECT_CHAT_BUBBLE_BG;
    public static final int ASSET_STATUS_OFFLINE_MESSAGE_BG = 1 << ASSET_OFFLINE_MESSAGE_BG;
    public static final int ASSET_STATUS_STATUS_BAR_BG = 1 << ASSET_STATUS_BAR_BG;
    public static final int ASSET_STATUS_SMS_TOGGLE_BG = 1 << ASSET_SMS_TOGGLE_BG;
    public static final int ASSET_STATUS_THUMBNAIL = 1 << ASSET_THUMBNAIL;

    // Bit Constant to check if all the assets are download, in our case it is 111 1111 1111. (for 11 Assets)
    public static final int ASSET_DOWNLOAD_COMPLETE_STATUS = 0x7FF;

    //Asset Download is a Success / Failure
    public static final byte ASSET_DOWNLOAD_STATUS_SUCCESS = 0;
    public static final byte ASSET_DOWNLOAD_STATUS_FAILURE = 1;

    //File Extension Types
    public static final String FILEEXTN_9PATCH = "9.png";
    public static final String FILEEXTN_PNG = "png";
    public static final String FILEEXTN_JPG = "jpg";
    public static final String FILEEXTN_JPEG = "jpeg";
}
