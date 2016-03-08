package com.bsb.hike.chatthemes;

/**
 * Created by sriram on 22/02/16.
 */
public class HikeChatThemeConstants
{
	// Bit Constants for Theme Type.
	public static final int THEME_TYPE_TILED = 1 << 0;

	public static final int THEME_TYPE_ANIMATED = 1 << 1;

	// Identify the type of Asset, if it is an color / Bitmap / Nine Patch / Bease64String (Thumbnail) / Shape
	public static final byte ASSET_TYPE_UNKNOWN = -1;

	public static final byte ASSET_TYPE_COLOR = 0;

	public static final byte ASSET_TYPE_BITMAP = 1;

	public static final byte ASSET_TYPE_NINE_PATCH = 2;

	public static final byte ASSET_TYPE_BASE64STRING = 3;

	public static final byte ASSET_TYPE_SHAPE = 4;

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

	public static final byte ASSET_INDEX_STATUS_BAR_COLOR = 13;

	public static final byte ASSET_INDEX_COUNT = 14;

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

	public static final int ASSET_STATUS_STATUS_BAR_COLOR = 1 << ASSET_INDEX_STATUS_BAR_COLOR;

	// Bit Constant to check if all the assets are download, in our case it is 11 1111 1111 1111. (for 14 Assets)
	public static final int ASSET_STATUS_DOWNLOAD_COMPLETE = ASSET_STATUS_BG_PORTRAIT | ASSET_STATUS_BG_LANDSCAPE | ASSET_STATUS_ACTION_BAR_BG | ASSET_STATUS_CHAT_BUBBLE_BG
			| ASSET_STATUS_SENT_NUDGE_BG | ASSET_STATUS_RECEIVED_NUDGE_BG | ASSET_STATUS_INLINE_UPDATE_BG | ASSET_STATUS_MULTISELECT_BUBBLE_BG | ASSET_STATUS_OFFLINE_MESSAGE_BG
			| ASSET_STATUS_STATUS_BAR_BG | ASSET_STATUS_SMS_TOGGLE_BG | ASSET_STATUS_THUMBNAIL | ASSET_STATUS_BUBBLE_COLOR | ASSET_STATUS_STATUS_BAR_COLOR;

	// Asset Download is a Success / Failure
	public static final byte ASSET_DOWNLOAD_STATUS_SUCCESS = 0;

	public static final byte ASSET_DOWNLOAD_STATUS_FAILURE = 1;

	// File Extension Types
	public static final String FILEEXTN_9PATCH = "9.png";

	public static final String FILEEXTN_PNG = "png";

	public static final String FILEEXTN_JPG = "jpg";

	public static final String FILEEXTN_JPEG = "jpeg";

	// JSON Constants
	public static final String JSON_DWNLD_ASSET_ID = "asset_ids";
}
