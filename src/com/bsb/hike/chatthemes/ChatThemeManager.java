package com.bsb.hike.chatthemes;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.ChatAnalyticConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeChatTheme;
import com.bsb.hike.models.HikeChatThemeAsset;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Created by sriram on 22/02/16.
 */
public class ChatThemeManager {
    private static ChatThemeManager mInstance;

    // Helps the manager class with all the asset maintainance
    private ChatThemeAssetHelper mAssetHelper;

    // Helps the manager with all the Drawable assets
    private ChatThemeDrawableHelper mDrawableHelper;

    // Maintains the Map of Chatthemes
    private LinkedHashMap<String, HikeChatTheme> mChatThemesMap;

    public String defaultChatThemeId = "0";

    public HikeChatTheme defaultChatTheme = new HikeChatTheme();

    public String customThemeTempUploadImagePath = null;

    public String currentDownloadingAssetsThemeId = null;

    private String TAG = "ChatThemeManager";

    private ArrayList<String> defaultHikeThemes = null;

    private String recentCustomTheme = null;

    private ChatThemeManager() {

    }

    public static ChatThemeManager getInstance() {
        if (mInstance == null) {
            synchronized (ChatThemeManager.class) {
                if (mInstance == null) {
                    mInstance = new ChatThemeManager();
                }
            }
        }
        return mInstance;
    }

    public void initialize() {
        mChatThemesMap = HikeConversationsDatabase.getInstance().getAllChatThemes();
        mDrawableHelper = new ChatThemeDrawableHelper();
        mAssetHelper = new ChatThemeAssetHelper();
        addTempCustomThemeToMap();
    }

    private void getAllHikeThemesForDisplay() {
        defaultHikeThemes = new ArrayList<>();
        Set<String> themeIds = mChatThemesMap.keySet();
        boolean isRecentCTFound = false;
        for (String themeId : themeIds) {
            if (mChatThemesMap.get(themeId).isCustomTheme()) {
                if (!isRecentCTFound && mChatThemesMap.get(themeId).isVisible()) {
                    recentCustomTheme = themeId;
                    isRecentCTFound = true;
                }
            } else {
                defaultHikeThemes.add(themeId);
            }
        }
    }

    public ChatThemeAssetHelper getAssetHelper() {
        return mAssetHelper;
    }

    public ChatThemeDrawableHelper getDrawableHelper() {
        return mDrawableHelper;
    }

    public HikeChatTheme getTheme(String themeId) {
        return mChatThemesMap.get(themeId);
    }

    public void clearThemes() {
        mChatThemesMap.clear();
    }

    /**
     * Checks if all the assets for this is theme are available or not
     *
     * @param themeID theme id
     * @return boolean
     */
    public boolean isThemeAvailable(String themeId) {
        if(themeId == null)
            return false;

        if (themeId.equals(ChatThemeManager.getInstance().defaultChatThemeId)) // the default theme is always available
            return true;

        if (!mChatThemesMap.containsKey(themeId))
            return false;

        HikeChatTheme theme = getTheme(themeId);
        return mAssetHelper.isAssetsAvailableForTheme(theme);
    }

    /**
     * Gives the list of missing assets for this theme
     *
     * @param themeID theme id
     * @return String[]
     * missing assets for given theme
     */
    public String[] getMissingAssetsForTheme(String themeId) {
        // the second check is to avoid any null pointer exception at the getTheme call
        if (!isThemeAvailable(themeId) && mChatThemesMap.containsKey(themeId)) {
            return mAssetHelper.getMissingAssets(getTheme(themeId).getAssets());
        }
        return null;
    }

    public void downloadAssetsForTheme(String themeId) {
        String[] assets = getMissingAssetsForTheme(themeId);
        if ((assets != null) && (assets.length > 0)) {
            currentDownloadingAssetsThemeId = themeId;
            mAssetHelper.assetDownloadRequest(assets);
        }
    }

    public String getCCTTempUploadPath() {
        return getDrawableHelper().getCCTTempUploadRootPath() + File.separator + Long.toString(System.currentTimeMillis()) + "_tmp.jpg";
    }

    //MQTT Signal packet processing
    public void processNewThemeSignal(JSONArray data, boolean areTheseAssetsOnApk) {
        try {
            int len = data.length();

            ArrayList<HikeChatTheme> themeList = new ArrayList<>();
            ArrayList<HikeChatThemeAsset> assetsList = new ArrayList<>();

            //looping of the n no themes sent in the packet
            for (int i = 0; i < len; i++) {
                HikeChatTheme theme = new HikeChatTheme();
                JSONObject t = data.getJSONObject(i);

                String themeID = t.getString(HikeChatThemeConstants.JSON_SIGNAL_THEME_THEMEID);
                theme.setThemeId(themeID);
                theme.setThemeType(t.getInt(HikeChatThemeConstants.JSON_SIGNAL_THEME_THEMESTATE));

                if (t.has(HikeChatThemeConstants.JSON_SIGNAL_THEME_VISIBILITY)) {
                    boolean visible = t.getBoolean(HikeChatThemeConstants.JSON_SIGNAL_THEME_VISIBILITY);
                    theme.setVisibilityStatus(visible);
                }

                if (t.has(HikeChatThemeConstants.JSON_SIGNAL_THEME_ORDER)) {
                    int order = t.getInt(HikeChatThemeConstants.JSON_SIGNAL_THEME_ORDER);
                    theme.setThemeOrderIndex(order);
                }

                if (t.has(HikeChatThemeConstants.JSON_SIGNAL_THEME_SYSTEM_MESSAGE)) {
                    int messageType = t.getInt(HikeChatThemeConstants.JSON_SIGNAL_THEME_SYSTEM_MESSAGE);
                    theme.setSystemMessageType(messageType);
                }

                // looping to the no of indexes for a theme
                for (byte j = 0; j < HikeChatThemeConstants.ASSET_INDEX_COUNT; j++) {
                    JSONObject assetObj = t.getJSONObject(HikeChatThemeConstants.JSON_SIGNAL_THEME[j]);
                    int type = assetObj.getInt(HikeChatThemeConstants.JSON_SIGNAL_ASSET_TYPE);
                    String id = assetObj.getString(HikeChatThemeConstants.JSON_SIGNAL_ASSET_VALUE);
                    if (type == HikeChatThemeConstants.ASSET_TYPE_COLOR) {
                        id = id.toLowerCase();
                    }

                    int size = 0;
                    if (assetObj.has(HikeChatThemeConstants.JSON_SIGNAL_ASSET_SIZE)) {
                        size = assetObj.getInt(HikeChatThemeConstants.JSON_SIGNAL_ASSET_SIZE);
                    }
                    theme.setAsset(j, id);
                    if (!mAssetHelper.hasAsset(id)) {
                        HikeChatThemeAsset hcta = new HikeChatThemeAsset(id, type, null, size);
                        if (areTheseAssetsOnApk) {
                            hcta.setIsDownloaded(HikeChatThemeConstants.ASSET_DOWNLOAD_STATUS_DOWNLOADED_APK);
                        } else {
                            if (type == HikeChatThemeConstants.ASSET_TYPE_COLOR) {
                                hcta.setIsDownloaded(HikeChatThemeConstants.ASSET_DOWNLOAD_STATUS_DOWNLOADED_SDCARD);
                            }
                        }
                        assetsList.add(hcta);
                        mAssetHelper.saveChatThemeAsset(id, hcta);
                    }
                }
                themeList.add(theme);

                mChatThemesMap.put(themeID, theme);
            }

            Logger.d(TAG, "unique chat themes in MQTT packet :" + themeList.size());
            HikeConversationsDatabase.getInstance().saveChatThemes(themeList);
            HikeConversationsDatabase.getInstance().saveChatThemeAssets(assetsList);

            //querying for chat themes data (images) when the packet is received. The call might be removed later.
            if (!areTheseAssetsOnApk) {
                for (HikeChatTheme theme : themeList) {
                    downloadAssetsForTheme(theme.getThemeId());
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String processCustomThemeSignal(JSONObject data, boolean downloadAssets) {
        String themeID = null;
        String assetId = null;
        String thumbnailAssetId = null;
        try {

            themeID = data.getString(HikeChatThemeConstants.JSON_SIGNAL_THEME_THEMEID);

            HikeChatTheme theme = new HikeChatTheme();
            theme.setThemeId(themeID);
            theme.setThemeType(HikeChatThemeConstants.THEME_TYPE_CUSTOM);
            if (downloadAssets) {
                theme.setVisibilityStatus(false);
            } else {
                theme.setVisibilityStatus(true);
            }
            theme.setThemeOrderIndex(0);
            theme.setSystemMessageType(HikeChatThemeConstants.SYSTEM_MESSAGE_TYPE_LIGHT);

            for (byte j = 0; j < HikeChatThemeConstants.ASSET_INDEX_COUNT; j++) {
                String assetKey = HikeChatThemeConstants.JSON_SIGNAL_THEME[j];
                if (assetKey.equalsIgnoreCase(HikeChatThemeConstants.JSON_SIGNAL_THEME_BG_PORTRAIT)) {
                    JSONObject jsonObject = data.getJSONObject(assetKey);
                    assetId = (jsonObject.getString(HikeChatThemeConstants.JSON_SIGNAL_ASSET_VALUE) + HikeChatThemeConstants.FILEEXTN_JPG);
                    int type = jsonObject.getInt(HikeChatThemeConstants.JSON_SIGNAL_ASSET_TYPE);
                    int size = jsonObject.getInt(HikeChatThemeConstants.JSON_SIGNAL_ASSET_SIZE);
                    HikeChatThemeAsset asset = new HikeChatThemeAsset(assetId, type, "", size);
                    mAssetHelper.saveChatThemeAsset(assetId, asset);
                    asset.setIsDownloaded(HikeChatThemeConstants.ASSET_DOWNLOAD_STATUS_NOT_DOWNLOADED);
                    theme.setAsset(HikeChatThemeConstants.ASSET_INDEX_BG_PORTRAIT, assetId);
                } else if (assetKey.equalsIgnoreCase(HikeChatThemeConstants.JSON_SIGNAL_THEME_BG_LANDSCAPE)) {
                    theme.setAsset(HikeChatThemeConstants.ASSET_INDEX_BG_LANDSCAPE, assetId);
                } else if (assetKey.equalsIgnoreCase(HikeChatThemeConstants.JSON_SIGNAL_THEME_THUMBNAIL)) {
                    JSONObject thumbnail = data.getJSONObject(assetKey);
                    thumbnailAssetId = (thumbnail.getString(HikeChatThemeConstants.JSON_SIGNAL_ASSET_VALUE) + HikeChatThemeConstants.FILEEXTN_JPG);
                    int thumbnailType = thumbnail.getInt(HikeChatThemeConstants.JSON_SIGNAL_ASSET_TYPE);
                    int thumbnailSize = thumbnail.getInt(HikeChatThemeConstants.JSON_SIGNAL_ASSET_SIZE);
                    HikeChatThemeAsset thumbnailAsset = new HikeChatThemeAsset(thumbnailAssetId, thumbnailType, "", thumbnailSize);
                    mAssetHelper.saveChatThemeAsset(thumbnailAssetId, thumbnailAsset);
                    thumbnailAsset.setIsDownloaded(HikeChatThemeConstants.ASSET_DOWNLOAD_STATUS_NOT_DOWNLOADED);
                    theme.setAsset(HikeChatThemeConstants.ASSET_INDEX_THUMBNAIL, thumbnailAssetId);
                } else {
                    HikeChatThemeAsset asset = getDrawableHelper().getDefaultCustomDrawable(assetKey);
                    if (asset != null) {
                        mAssetHelper.saveChatThemeAsset(asset.getAssetId(), asset);
                        asset.setIsDownloaded(HikeChatThemeConstants.ASSET_DOWNLOAD_STATUS_DOWNLOADED_APK);
                        theme.setAsset(j, asset.getAssetId());
                    }
                }
            }
            mChatThemesMap.put(themeID, theme);
            HikeConversationsDatabase.getInstance().saveChatTheme(theme);

            if (downloadAssets) {
                downloadAssetsForTheme(themeID);
            } else {
                recentCustomTheme = themeID;

                ArrayList<HikeChatThemeAsset> assetsList = new ArrayList<>();
                String destFilePath = ChatThemeManager.getInstance().getDrawableHelper().getAssetRootPath() + File.separator + assetId;
                Utils.copyFile(customThemeTempUploadImagePath, destFilePath);
                HikeChatThemeAsset asset = getAssetHelper().getChatThemeAsset(assetId);
                asset.setIsDownloaded(HikeChatThemeConstants.ASSET_DOWNLOAD_STATUS_DOWNLOADED_SDCARD);
                assetsList.add(asset);


                try {
                    String thumbnailFilePath = ChatThemeManager.getInstance().getDrawableHelper().getAssetRootPath() + File.separator + thumbnailAssetId;
                    Bitmap thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(customThemeTempUploadImagePath), HikeChatThemeConstants.CHATTHEME_CUSTOM_THUMBNAIL_SIZE, HikeChatThemeConstants.CHATTHEME_CUSTOM_THUMBNAIL_SIZE);
                    byte[] bmpData = Utils.bitmapToBytes(thumbnail, Bitmap.CompressFormat.JPEG, 100);
                    Utils.saveByteArrayToFile(new File(thumbnailFilePath), bmpData);

                    HikeChatThemeAsset thumbAsset = getAssetHelper().getChatThemeAsset(thumbnailAssetId);
                    thumbAsset.setIsDownloaded(HikeChatThemeConstants.ASSET_DOWNLOAD_STATUS_DOWNLOADED_SDCARD);
                    assetsList.add(thumbAsset);
                } catch(IOException e) {
                    e.printStackTrace();
                }

                HikeConversationsDatabase.getInstance().saveChatThemeAssets(assetsList);
            }
            return themeID;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void processDeleteThemeSignal(JSONObject data) {

    }

    public ArrayList<String> getAvailableThemeIds() {
        ArrayList<String> availableThemes = new ArrayList<>();
        if((defaultHikeThemes == null) || (defaultHikeThemes.size() == 0)) {
            getAllHikeThemesForDisplay();
        }
        availableThemes.addAll(defaultHikeThemes);
        if (!TextUtils.isEmpty(recentCustomTheme) && isThemeAvailable(recentCustomTheme) && ChatThreadUtils.isCustomChatThemeEnabled()) {
            availableThemes.add(0, recentCustomTheme);
        }
        return availableThemes;
    }

    /**
     * method to get a drawable given a themeId and an asset index. In case of any problem, it returns a default asset.
     *
     * @param themeId
     * @param assetIndex
     * @return a drawable corresponding to the asset
     */
    public Drawable getDrawableForTheme(String themeId, byte assetIndex) {
        if (themeId.equals(ChatThemeManager.getInstance().defaultChatThemeId) || !ChatThemeManager.getInstance().isThemeAvailable(themeId)) {
            return mDrawableHelper.getDefaultDrawable(assetIndex);
        }
        return mDrawableHelper.getDrawableForTheme(getTheme(themeId), assetIndex);
    }

    public void downloadThemeAssetsMetadata(String themeId, String toUser, String groupId, boolean isCustom) {
        // Automatically enabling the Chatthemes for the receiver, though the packet is not enabled from server. This will help to organically grow chat themes
        // https://hikeapp.atlassian.net/browse/CE-764
        if(isCustom && !ChatThreadUtils.isCustomChatThemeEnabled()){
            HikeAnalyticsEvent.recordAnalyticOrganicCTPacket(themeId, toUser, groupId);
            HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.CUSTOM_CHATTHEME_ENABLED, true);
        }

        if (!mChatThemesMap.containsKey(themeId)) {
            DownloadThemeContentTask downloadAssetIds = new DownloadThemeContentTask(themeId, isCustom);
            downloadAssetIds.execute();
        } else {
            if (isThemeAvailable(themeId)) {
                HikeMessengerApp.getPubSub().publish(HikePubSub.CHATTHEME_DOWNLOAD_SUCCESS, themeId);
            } else {
                downloadAssetsForTheme(themeId);
            }
        }
    }


    public boolean migrateChatThemesToDB() {
        try {
            JSONObject jsonObj = new JSONObject(Utils.loadJSONFromAsset(HikeMessengerApp.getInstance().getApplicationContext(), HikeChatThemeConstants.CHATTHEMES_DEFAULT_JSON_FILE_NAME));
            if (jsonObj != null) {
                JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
                JSONArray themeData = data.getJSONArray(HikeChatThemeConstants.JSON_SIGNAL_THEME_DATA);
                processNewThemeSignal(themeData, true);
            } else {
                Log.v(TAG, "Unable to load " + HikeChatThemeConstants.CHATTHEMES_DEFAULT_JSON_FILE_NAME + " file from assets");
                return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        HikeSharedPreferenceUtil.getInstance().saveData(HikeChatThemeConstants.MIGRATED_CHAT_THEMES_DATA_TO_DB, true);
        return true;
    }

    public int getSystemMessageTextViewLayout(int systemMessageType) {
        switch(systemMessageType){
            case HikeChatThemeConstants.SYSTEM_MESSAGE_TYPE_LIGHT:
                return R.layout.system_message_light;
            case HikeChatThemeConstants.SYSTEM_MESSAGE_TYPE_DARK:
                return R.layout.system_message_dark;
            case HikeChatThemeConstants.SYSTEM_MESSAGE_TYPE_DEFAULT:
                return R.layout.system_message_default_theme;
        }
        return R.layout.system_message_dark;
    }

    public int getSystemMessageBackgroundLayout(int systemMessageType) {
        switch(systemMessageType){
            case HikeChatThemeConstants.SYSTEM_MESSAGE_TYPE_LIGHT:
                return R.drawable.bg_system_message_light;
            case HikeChatThemeConstants.SYSTEM_MESSAGE_TYPE_DARK:
                return R.drawable.bg_system_message_dark;
        }
        return R.drawable.bg_system_message_dark;
    }

    public void postRestoreSetup() {
        HikeSharedPreferenceUtil.getInstance().saveData(HikeChatThemeConstants.MIGRATED_CHAT_THEMES_DATA_TO_DB, false);
        clearThemes();
        mAssetHelper.clearAssets();
        addTempCustomThemeToMap();
    }

    public void addTempCustomThemeToMap() {
        HikeChatTheme theme = new HikeChatTheme();
        theme.setThemeId(HikeChatThemeConstants.THEME_ID_CUSTOM_THEME);
        theme.setThemeType(HikeChatThemeConstants.THEME_TYPE_CUSTOM);
        theme.setVisibilityStatus(false);
        theme.setThemeOrderIndex(0);
        theme.setSystemMessageType(HikeChatThemeConstants.SYSTEM_MESSAGE_TYPE_LIGHT);
        theme.setAssetDownloadStatus(HikeChatThemeConstants.ASSET_STATUS_DOWNLOAD_COMPLETE);

        for (byte j = 0; j < HikeChatThemeConstants.ASSET_INDEX_COUNT; j++) {
            String assetKey = HikeChatThemeConstants.JSON_SIGNAL_THEME[j];
            if (!(assetKey.equalsIgnoreCase(HikeChatThemeConstants.JSON_SIGNAL_THEME_BG_PORTRAIT) || assetKey.equalsIgnoreCase(HikeChatThemeConstants.JSON_SIGNAL_THEME_BG_LANDSCAPE) || assetKey.equalsIgnoreCase(HikeChatThemeConstants.JSON_SIGNAL_THEME_THUMBNAIL))) {
                HikeChatThemeAsset asset = getDrawableHelper().getDefaultCustomDrawable(assetKey);
                if (asset != null) {
                    theme.setAsset(j, asset.getAssetId());
                }
            }
        }
        mChatThemesMap.put(theme.getThemeId(), theme);
    }

}
