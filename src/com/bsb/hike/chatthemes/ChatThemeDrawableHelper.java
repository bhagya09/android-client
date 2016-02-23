package com.bsb.hike.chatthemes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.HikeChatTheme;
import com.bsb.hike.models.HikeChatThemeAsset;

import java.io.File;

/**
 * Created by sriram on 22/02/16.
 */
public class ChatThemeDrawableHelper {
    private final String TAG = "ChatThemeDrawableHelper";

    public ChatThemeDrawableHelper(){

    }

    /**
     * This method returns the drawable for the theme
     *
     * @param  Context  application context
     * @param  HikeChatTheme The Theme for which drawable is required
     * @param  assetType the type of asset
     * @return      the Drawable
     *
     */

    public Drawable getDrawableForTheme(HikeChatTheme theme, byte assetType) {
        return getDrawableForTheme(HikeMessengerApp.getInstance(), theme, assetType);
    }

    public Drawable getDrawableForTheme(Context context, HikeChatTheme theme, byte assetType) {
        Drawable drawable = null;
        if (theme != null) {
            drawable = getDrawableFromSDCard(theme, assetType);
            if (drawable == null) {
                Log.v(TAG, "Drawable does not exist on SD Card : ");
            }
        }
        if (drawable == null) {
            ChatThemeManager.getInstance().getAssetHelper().setAssetMissing(theme, assetType);
            drawable = getDefaultDrawable(assetType);
            Log.v(TAG, "Setting the default theme drawable :");
        }
        return drawable;
    }

    /**
     * This method returns the drawable from SD card
     *
     * @param  HikeChatTheme The Theme for which drawable is required
     * @param  assetType the type of asset
     * @return      the Drawable
     *
     */
    private Drawable getDrawableFromSDCard(HikeChatTheme theme, byte assetType) {
        return getDrawableFromSDCard(theme.getAsset(assetType));
    }

    private Drawable getDrawableFromSDCard(HikeChatThemeAsset asset) {
        Drawable drawable = null;
        if (asset.getType() == HikeChatThemeConstants.ASSETTYPE_COLOR) {
            //java.lang.NumberFormatException: Invalid long: "#1E131C"
            String color = asset.getValue();
            if (color.charAt(0) == '#') {
                drawable = new ColorDrawable(Color.parseColor(color));
            } else {
                drawable = new ColorDrawable(Color.parseColor("#" + color));
            }
        } else if ((asset.getType() == HikeChatThemeConstants.ASSETTYPE_BITMAP) || (asset.getType() == HikeChatThemeConstants.ASSETTYPE_NINE_PATCH)) {
            drawable = HikeMessengerApp.getLruCache().getBitmapDrawable(asset.getValue());
            if (drawable == null) {
                //TODO CHATTHEME Filepath
                String path = "";//getThemeAssetStoragePath() + File.separator + asset.getValue();
                if (isFileExists(path)) {
                    BitmapDrawable bmp = new BitmapDrawable(HikeMessengerApp.getInstance().getResources(), path);
                    HikeMessengerApp.getLruCache().putInCache(asset.getValue(), bmp);
                    drawable = bmp;
                }
            }
        }
        return drawable;
    }

    private boolean isFileExists(String path) {
        return new File(path).exists();
    }

    /**
     * This method returns the default drawable for the given asset type
     *
     * @param  assetType the type of asset
     * @return      the Drawable
     *
     */
    private Drawable getDefaultDrawable(int assetType) {
        switch (assetType) {
            case HikeChatThemeConstants.ASSET_BG_LANDSCAPE:
            case HikeChatThemeConstants.ASSET_BG_PORTRAIT:
                return getDrawableFromId(R.color.chat_thread_default_bg);
            case HikeChatThemeConstants.ASSET_CHAT_BUBBLE_BG:
                return getDrawableFromId(R.drawable.ic_bubble_blue_selector);
            case HikeChatThemeConstants.ASSET_ACTION_BAR_BG:
                return getDrawableFromId(R.drawable.bg_header_transparent);
            case HikeChatThemeConstants.ASSET_INLINE_STATUS_MSG_BG:
                return getDrawableFromId(R.drawable.bg_status_chat_thread_default_theme);
            case HikeChatThemeConstants.ASSET_MULTISELECT_CHAT_BUBBLE_BG:
                return getDrawableFromId(R.color.light_blue_transparent);
            case HikeChatThemeConstants.ASSET_OFFLINE_MESSAGE_BG:
                return getDrawableFromId(R.color.list_item_subtext);
            case HikeChatThemeConstants.ASSET_RECEIVED_NUDGE_BG:
                return getDrawableFromId(R.drawable.ic_nudge_hike_receive);
            case HikeChatThemeConstants.ASSET_SENT_NUDGE_BG:
                return getDrawableFromId(R.drawable.ic_nudge_hike_sent);
            case HikeChatThemeConstants.ASSET_STATUS_BAR_BG:
                return getDrawableFromId(R.color.blue_hike_status_bar_m);
            case HikeChatThemeConstants.ASSET_SMS_TOGGLE_BG:
                return getDrawableFromId(R.drawable.bg_sms_toggle);
        }
        return null;
    }

    private Drawable getDrawableFromId(int resId) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            return HikeMessengerApp.getInstance().getDrawable(resId);
        } else {
            return HikeMessengerApp.getInstance().getResources().getDrawable(resId);
        }
    }
}
