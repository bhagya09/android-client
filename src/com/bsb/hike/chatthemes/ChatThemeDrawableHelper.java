package com.bsb.hike.chatthemes;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.NinePatch;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.HikeChatTheme;
import com.bsb.hike.models.HikeChatThemeAsset;
import com.bsb.hike.utils.Utils;

import java.io.File;

/**
 * Created by sriram on 22/02/16.
 */
public class ChatThemeDrawableHelper {
    private final String TAG = "ChatThemeDrawableHelper";

    private String assetRootPath;

    public ChatThemeDrawableHelper() {
        assetRootPath = getThemeAssetStoragePath();
    }

    public String getAssetRootPath() {
        return assetRootPath;
    }

    /**
     * This method returns the drawable for the theme
     *
     * @param Context       application context
     * @param HikeChatTheme The Theme for which drawable is required
     * @param assetIndex    the type of asset
     * @return the Drawable
     */

    public Drawable getDrawableForTheme(HikeChatTheme theme, byte assetIndex) {
        return getDrawableForTheme(HikeMessengerApp.getInstance().getApplicationContext(), theme, assetIndex);
    }

    public Drawable getDrawableForTheme(Context context, HikeChatTheme theme, byte assetIndex) {
        Drawable drawable = null;
        if (theme != null) {
            drawable = getDrawableFromSDCard(theme, assetIndex);
            if (drawable == null) {
                Log.v(TAG, "Drawable does not exist on SD Card : ");
            }
        }
        if ((theme != null) && (drawable == null)) {
            drawable = getAPKDrawable(theme, assetIndex);
            if (drawable == null) {
                Log.v(TAG, "Drawable does not exist on APK file (preloaded file) :");
            }
        }
        if (drawable == null) {
            ChatThemeManager.getInstance().getAssetHelper().setAssetMissing(theme, assetIndex);
            drawable = getDefaultDrawable(assetIndex);
            Log.v(TAG, "Setting the default theme drawable :");
        }
        return drawable;
    }

    /**
     * This method returns the drawable from SD card
     *
     * @param HikeChatTheme The Theme for which drawable is required
     * @param assetIndex    the type of asset
     * @return the Drawable
     */
    private Drawable getDrawableFromSDCard(HikeChatTheme theme, byte assetIndex) {
        HikeChatThemeAsset asset = ChatThemeManager.getInstance().getAssetHelper().getChatThemeAsset(theme.getAssetId(assetIndex));
        if (asset == null) {
            return null;
        }
        return getDrawableFromSDCard(asset);
    }

    private Drawable getAPKDrawable(HikeChatTheme theme, byte assetIndex) {
        HikeChatThemeAsset asset = ChatThemeManager.getInstance().getAssetHelper().getChatThemeAsset(theme.getAssetId(assetIndex));
        if (asset == null) {
            return null;
        }
        return getResourceDrawableFromName(asset);
    }

    public ColorDrawable getColorDrawable(HikeChatTheme theme, byte assetIndex) {
        HikeChatThemeAsset asset = ChatThemeManager.getInstance().getAssetHelper().getChatThemeAsset(theme.getAssetId(assetIndex));
        if ((asset != null) && (asset.getType() == HikeChatThemeConstants.ASSET_TYPE_COLOR)) {
            return getColorDrawable(asset.getAssetId());
        }
        return new ColorDrawable(Color.WHITE);
    }

    private Drawable getDrawableFromSDCard(HikeChatThemeAsset asset) {
        if (TextUtils.isEmpty(assetRootPath)) {
            Log.v(TAG, "External / Internal storage is not available");
            return null;
        }
        String assetPath = assetRootPath + File.separator + asset.getAssetId();
        switch (asset.getType()) {
            case HikeChatThemeConstants.ASSET_TYPE_JPEG:
            case HikeChatThemeConstants.ASSET_TYPE_PNG:
            case HikeChatThemeConstants.ASSET_TYPE_BASE64STRING:
                BitmapDrawable drawable = HikeMessengerApp.getLruCache().getBitmapDrawable(asset.getAssetId());
                if (drawable == null) {
                    if (isFileExists(assetPath)) {
                        Bitmap b = HikeBitmapFactory.decodeBitmapFromFile(assetPath, Bitmap.Config.RGB_565);
                        drawable = new BitmapDrawable(HikeMessengerApp.getInstance().getResources(), b);
                        HikeMessengerApp.getLruCache().putInCache(asset.getAssetId(), drawable);
                        b = null;
                    } else {
                        Log.v(TAG, "Either path is empty (or) file does not exist at path " + assetPath);
                    }
                }
                return drawable;
            case HikeChatThemeConstants.ASSET_TYPE_NINE_PATCH:
                if (isFileExists(assetPath)) {
                    Bitmap bitmap = HikeBitmapFactory.decodeBitmapFromFile(assetPath, Bitmap.Config.RGB_565);
                    byte[] chunk = bitmap.getNinePatchChunk();
                    boolean result = NinePatch.isNinePatchChunk(chunk);
                    if (result) {
                        return new NinePatchDrawable(HikeMessengerApp.getInstance().getResources(), bitmap, chunk, new Rect(), null);
                    }
                } else {
                    Log.v(TAG, "Either path is empty (or) file does not exist at path " + assetPath);
                }
                return null;
            case HikeChatThemeConstants.ASSET_TYPE_COLOR:
                return getColorDrawable(asset.getAssetId());
        }
        return null;
    }

    private Drawable getResourceDrawableFromName(HikeChatThemeAsset asset) {
        Context context = HikeMessengerApp.getInstance().getApplicationContext();
        int index = asset.getAssetId().indexOf('.');
        String assetName = asset.getAssetId();
        int resourceId = -1;
        if (index > 0) {
            assetName = asset.getAssetId().substring(0, index);
            resourceId = context.getResources().getIdentifier(assetName, "drawable", context.getPackageName());
        }
        if (resourceId == -1) {
            return null;
        }

        Log.v(TAG, "assetName ::: " + assetName + " :: resourceId:: " + resourceId + " :: asset.getType() :: " + asset.getType());
        switch (asset.getType()) {
            case HikeChatThemeConstants.ASSET_TYPE_JPEG:
            case HikeChatThemeConstants.ASSET_TYPE_PNG:
            case HikeChatThemeConstants.ASSET_TYPE_BASE64STRING:
                BitmapDrawable drawable = HikeMessengerApp.getLruCache().getBitmapDrawable(asset.getAssetId() + getOrientationPrefix(context));
                if (drawable == null) {
                    Bitmap b = HikeBitmapFactory.decodeBitmapFromResource(context.getResources(), resourceId, Bitmap.Config.RGB_565);
                    drawable = HikeBitmapFactory.getBitmapDrawable(context.getResources(), b);
                    HikeMessengerApp.getLruCache().putInCache(asset.getAssetId() + getOrientationPrefix(context), drawable);
                    b = null;
                }
                return drawable;
            case HikeChatThemeConstants.ASSET_TYPE_NINE_PATCH:
                return (NinePatchDrawable) getDrawableFromId(resourceId);
            case HikeChatThemeConstants.ASSET_TYPE_COLOR:
                return getColorDrawable(asset.getAssetId());
        }
        return null;
    }

    private ColorDrawable getColorDrawable(String color) {
        // java.lang.NumberFormatException: Invalid long: "#1E131C"
        ColorDrawable cDrawable = null;
        if (color.charAt(0) == '#') {
            cDrawable = new ColorDrawable(Color.parseColor(color));
        } else {
            cDrawable = new ColorDrawable(Color.parseColor("#" + color));
        }
        return cDrawable;
    }

    private String getOrientationPrefix(Context context) {
        return (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) ? HikeConstants.ORIENTATION_LANDSCAPE : HikeConstants.ORIENTATION_PORTRAIT;
    }

    private boolean isFileExists(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        return new File(path).exists();
    }

    /**
     * This method returns the default drawable for the given asset type
     *
     * @param assetIndex the type of asset
     * @return the Drawable
     */
    public Drawable getDefaultDrawable(int assetIndex) {
        switch (assetIndex) {
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

    public HikeChatThemeAsset getDefaultCustomDrawable(String assetId) {
        try {
            switch (assetId) {
                case HikeChatThemeConstants.JSON_SIGNAL_THEME_ACTION_BAR:
                    return getDefaultCustomDrawableAsset(R.drawable.bg_header_transparent_2x, HikeChatThemeConstants.ASSET_TYPE_PNG);

                case HikeChatThemeConstants.JSON_SIGNAL_THEME_CHAT_BUBBLE_BG:
                    return getDefaultCustomDrawableAsset(R.drawable.ic_bubble_blue, HikeChatThemeConstants.ASSET_TYPE_NINE_PATCH);

                case HikeChatThemeConstants.JSON_SIGNAL_THEME_SENT_NUDGE:
                    return getDefaultCustomDrawableAsset(R.drawable.ic_nudge_sent_purpleflower, HikeChatThemeConstants.ASSET_TYPE_PNG);

                case HikeChatThemeConstants.JSON_SIGNAL_THEME_RECEIVE_NUDGE:
                    return getDefaultCustomDrawableAsset(R.drawable.ic_nudge_receive_purpleflower, HikeChatThemeConstants.ASSET_TYPE_PNG);

                case HikeChatThemeConstants.JSON_SIGNAL_THEME_INLINE_STATUS_BG:
                    return getDefaultCustomDrawableAsset(R.drawable.bg_status_chat_thread_custom_theme, HikeChatThemeConstants.ASSET_TYPE_PNG);

                case HikeChatThemeConstants.JSON_SIGNAL_THEME_BUBBLE_COLOR:
                    return getDefaultCustomDrawableAsset(R.color.bubble_blue, HikeChatThemeConstants.ASSET_TYPE_COLOR);

                case HikeChatThemeConstants.JSON_SIGNAL_THEME_SMS_TOGGLE_BG:
                    return getDefaultCustomDrawableAsset(R.drawable.bg_sms_toggle_custom_theme, HikeChatThemeConstants.ASSET_TYPE_NINE_PATCH);

                case HikeChatThemeConstants.JSON_SIGNAL_THEME_MULTI_SELECT_BUBBLE:
                    return getDefaultCustomDrawableAsset(R.color.light_black_transparent, HikeChatThemeConstants.ASSET_TYPE_COLOR);

                case HikeChatThemeConstants.JSON_SIGNAL_THEME_OFFLINE_MSG_BG:
                    return getDefaultCustomDrawableAsset(R.color.white, HikeChatThemeConstants.ASSET_TYPE_COLOR);

                case HikeChatThemeConstants.JSON_SIGNAL_THEME_STATUS_BAR_BG:
                    return getDefaultCustomDrawableAsset(R.color.purpleflower_theme_status_bar_color, HikeChatThemeConstants.ASSET_TYPE_COLOR);
            }
        } catch (Resources.NotFoundException e) {
            Log.v(TAG, "Resource " + assetId + " not found on apk");
            e.printStackTrace();
        }
        return null;
    }

    private HikeChatThemeAsset getDefaultCustomDrawableAsset(int assetDrawableId, int assetType) {
        HikeChatThemeAsset asset = null;
        Context context = HikeMessengerApp.getInstance().getApplicationContext();
        String resourceName = context.getResources().getResourceEntryName(assetDrawableId);
        if (assetType == HikeChatThemeConstants.ASSET_TYPE_NINE_PATCH) {
            resourceName += HikeChatThemeConstants.FILEEXTN_9PATCH;
        } else if (assetType == HikeChatThemeConstants.ASSET_TYPE_PNG) {
            resourceName += HikeChatThemeConstants.FILEEXTN_PNG;
        }
        if (ChatThemeManager.getInstance().getAssetHelper().hasAsset(resourceName)) {
            asset = ChatThemeManager.getInstance().getAssetHelper().getChatThemeAsset(resourceName);
        } else {
            asset = new HikeChatThemeAsset(resourceName, assetType, "", 0);
        }
        return asset;
    }

    private Drawable getDrawableFromId(int resId) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            return HikeMessengerApp.getInstance().getDrawable(resId);
        } else {
            return HikeMessengerApp.getInstance().getResources().getDrawable(resId);
        }
    }

    /**
     * method which returns the storage directory for saving chat theme assets. inspired by similar method in StickerManager
     *
     * @return the path of the directory
     * <p/>
     */
    public String getThemeAssetStoragePath() {
        Utils.ExternalStorageState st = Utils.getExternalStorageState();
        Context context = HikeMessengerApp.getInstance().getApplicationContext();
        if (st == Utils.ExternalStorageState.WRITEABLE) {
            return getExternalThemeDirectory(context);
        } else {
            return getInternalThemeDirectory(context);
        }
    }

    /**
     * creates a new directory in the external memory for saving chat theme
     *
     * @param context
     * @return returns path to the external memory directory
     */
    private String getExternalThemeDirectory(Context context) {
        File dir = context.getExternalFilesDir(null);
        if (dir == null) {
            return null;
        }
        String path = dir.getPath() + File.separator + HikeChatThemeConstants.CHAT_THEMES_ROOT;
        dir = new File(path);
        if (!dir.exists()) {
            dir.mkdir();
        }
        return path;
    }

    private String getInternalThemeDirectory(Context context) {
        String path = context.getFilesDir().getPath() + File.separator + HikeChatThemeConstants.CHAT_THEMES_ROOT;
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdir();
        }
        return path;
    }
}
