package com.bsb.hike.chatthemes;

import java.io.File;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.HikeChatTheme;
import com.bsb.hike.models.HikeChatThemeAsset;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * Created by sriram on 22/02/16.
 */
public class ChatThemeDrawableHelper
{
	private final String TAG = "ChatThemeDrawableHelper";

	public ChatThemeDrawableHelper()
	{

	}

	/**
	 * This method returns the drawable for the theme
	 *
	 * @param Context
	 *            application context
	 * @param HikeChatTheme
	 *            The Theme for which drawable is required
	 * @param assetIndex
	 *            the type of asset
	 * @return the Drawable
	 *
	 */

	public Drawable getDrawableForTheme(HikeChatTheme theme, byte assetIndex)
	{
		return getDrawableForTheme(HikeMessengerApp.getInstance().getApplicationContext(), theme, assetIndex);
	}

	public Drawable getDrawableForTheme(Context context, HikeChatTheme theme, byte assetIndex)
	{
		Drawable drawable = null;
		if (theme != null)
		{
			drawable = getDrawableFromSDCard(theme, assetIndex);
			if (drawable == null)
			{
				Log.v(TAG, "Drawable does not exist on SD Card : ");
			}
		}
		if (drawable == null)
		{
			ChatThemeManager.getInstance().getAssetHelper().setAssetMissing(theme, assetIndex);
			drawable = getDefaultDrawable(assetIndex);
			Log.v(TAG, "Setting the default theme drawable :");
		}
		return drawable;
	}

	/**
	 * This method returns the drawable from SD card
	 *
	 * @param HikeChatTheme
	 *            The Theme for which drawable is required
	 * @param assetIndex
	 *            the type of asset
	 * @return the Drawable
	 *
	 */
	private Drawable getDrawableFromSDCard(HikeChatTheme theme, byte assetIndex)
	{
		HikeChatThemeAsset asset = ChatThemeManager.getInstance().getAssetHelper().getAssetIfRecorded(theme.getAssetId(assetIndex));
		if(asset == null)
		{
			return null;
		}
		return getDrawableFromSDCard(asset);
	}

	private Drawable getDrawableFromSDCard(HikeChatThemeAsset asset)
	{
		Drawable drawable = null;
		if (asset.getType() == HikeChatThemeConstants.ASSET_TYPE_COLOR)
		{
			// java.lang.NumberFormatException: Invalid long: "#1E131C"
			String color = asset.getAssetId(); //assetId are equivalent to values for colors
			if (color.charAt(0) == '#')
			{
				drawable = new ColorDrawable(Color.parseColor(color));
			}
			else
			{
				drawable = new ColorDrawable(Color.parseColor("#" + color));
			}
		}
		else
		{
			drawable = HikeMessengerApp.getLruCache().getBitmapDrawable(asset.getAssetId());
			if (drawable == null)
			{
				String path = ChatThemeManager.getInstance().getDrawableHelper().getThemeAssetStoragePath() + File.separator + asset.getAssetId();
				if (isFileExists(path))
				{
					BitmapDrawable bmp = new BitmapDrawable(HikeMessengerApp.getInstance().getResources(), path);
					HikeMessengerApp.getLruCache().putInCache(asset.getAssetId(), bmp);
					drawable = bmp;
				}
			}
		}
		return drawable;
	}

	private boolean isFileExists(String path)
	{
		return new File(path).exists();
	}

	/**
	 * This method returns the default drawable for the given asset type
	 *
	 * @param assetIndex
	 *            the type of asset
	 * @return the Drawable
	 *
	 */
	public Drawable getDefaultDrawable(int assetIndex)
	{
		switch (assetIndex)
		{
		case HikeChatThemeConstants.ASSET_INDEX_BG_LANDSCAPE:
		case HikeChatThemeConstants.ASSET_INDEX_BG_PORTRAIT:
			return getDrawableFromId(R.color.chat_thread_default_bg);
		case HikeChatThemeConstants.ASSET_INDEX_CHAT_BUBBLE_BG:
			return getDrawableFromId(R.drawable.ic_bubble_blue_selector);
		case HikeChatThemeConstants.ASSET_INDEX_ACTION_BAR_BG:
			return getDrawableFromId(R.drawable.bg_header_transparent);
		case HikeChatThemeConstants.ASSET_INDEX_INLINE_STATUS_MSG_BG:
			return getDrawableFromId(R.drawable.bg_status_chat_thread_default_theme);
		case HikeChatThemeConstants.ASSET_INDEX_MULTISELECT_CHAT_BUBBLE_BG:
			return getDrawableFromId(R.color.light_blue_transparent);
		case HikeChatThemeConstants.ASSET_INDEX_OFFLINE_MESSAGE_BG:
			return getDrawableFromId(R.color.list_item_subtext);
		case HikeChatThemeConstants.ASSET_INDEX_RECEIVED_NUDGE_BG:
			return getDrawableFromId(R.drawable.ic_nudge_hike_receive);
		case HikeChatThemeConstants.ASSET_INDEX_SENT_NUDGE_BG:
			return getDrawableFromId(R.drawable.ic_nudge_hike_sent);
		case HikeChatThemeConstants.ASSET_INDEX_STATUS_BAR_BG:
			return getDrawableFromId(R.color.blue_hike_status_bar_m);
		case HikeChatThemeConstants.ASSET_INDEX_SMS_TOGGLE_BG:
			return getDrawableFromId(R.drawable.bg_sms_toggle);
		case HikeChatThemeConstants.ASSET_INDEX_THUMBNAIL:
			return getDrawableFromId(R.drawable.ic_ct_default_preview);
		}
		return null;
	}

	private Drawable getDrawableFromId(int resId)
	{
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
		{
			return HikeMessengerApp.getInstance().getDrawable(resId);
		}
		else
		{
			return HikeMessengerApp.getInstance().getResources().getDrawable(resId);
		}
	}

	/**
	 * method which returns the storage directory for saving chat theme assets. inspired by similar method in StickerManager
	 * @return the path of the directory
	 */
	public String getThemeAssetStoragePath()
	{
		/*
		 * We give a higher priority to external storage. If we find an exisiting directory in the external storage, we will return its path. Otherwise if there is an exisiting
		 * directory in internal storage, we return its path.
		 *
		 * If the directory is not available in both cases, we return the external storage's path if external storage is available. Else we return the internal storage's path.
		 */
		boolean externalAvailable = false;
		Utils.ExternalStorageState st = Utils.getExternalStorageState();
		Logger.d(TAG, "External Storage state : " + st.name());
		if (st == Utils.ExternalStorageState.WRITEABLE)
		{
			externalAvailable = true;
			String themeDirPath = getExternalThemeDirectory(HikeMessengerApp.getInstance().getApplicationContext());
			Logger.d(TAG, "Theme dir path : " + themeDirPath);
			if (themeDirPath == null)
			{
				return null;
			}

			File themeDir = new File(themeDirPath);

			if (themeDir.exists())
			{
				Logger.d(TAG, "Theme Dir exists ... so returning");
				return themeDir.getPath();
			}
		}
		if (externalAvailable)
		{
			Logger.d(TAG, "Returning external storage dir.");
			return getExternalThemeDirectory(HikeMessengerApp.getInstance().getApplicationContext());
		}
		else
		{
			return null;
		}
	}

	/**
	 * creates a new directory in the external memory for saving chat theme
	 * @param context
	 * @return returns path to the external memory directory
	 */
	private String getExternalThemeDirectory(Context context)
	{
		File dir = context.getExternalFilesDir(null);
		if (dir == null)
		{
			return null;
		}
		String themePath = dir.getPath() + File.separator + HikeChatThemeConstants.CHAT_THEMES_ROOT;
		dir = new File(themePath);

		if(dir.isDirectory())
		{
			return themePath;
		}
		else
		{
			boolean created = dir.mkdir();
			if(created)
			{
				return themePath;
			}
		}
		return null;
	}
}
