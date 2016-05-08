package com.bsb.hike.chatthemes;

import android.graphics.drawable.Drawable;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeChatTheme;
import com.bsb.hike.models.HikeChatThemeAsset;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;

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
    private LinkedHashMap<String, HikeChatTheme> mChatThemesList;

    public String defaultChatThemeId = "0";

    public HikeChatTheme defaultChatTheme = new HikeChatTheme();

    private String TAG = "ChatThemeManager";

    private ChatThemeManager() {
        initialize();
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

    private void initialize() {
        mChatThemesList = HikeConversationsDatabase.getInstance().getAllChatThemes();
        mDrawableHelper = new ChatThemeDrawableHelper();
        mAssetHelper = new ChatThemeAssetHelper();

        // initialising the default theme
        defaultChatTheme.setThemeId(defaultChatThemeId);
        mChatThemesList.put(defaultChatThemeId, defaultChatTheme);

        if (!HikeSharedPreferenceUtil.getInstance().getData(HikeChatThemeConstants.SHAREDPREF_DEFAULT_SET_RECORDED, false)) {
            initializeHikeChatThemesWithDefaultSet();
            HikeSharedPreferenceUtil.getInstance().saveData(HikeChatThemeConstants.SHAREDPREF_DEFAULT_SET_RECORDED, true);
        }
    }

    public ChatThemeAssetHelper getAssetHelper() {
        return mAssetHelper;
    }

    public ChatThemeDrawableHelper getDrawableHelper() {
        return mDrawableHelper;
    }

    public HikeChatTheme getTheme(String themeId) {
        return mChatThemesList.get(themeId);
    }

    /**
     * Checks if all the assets for this is theme are available or not
     *
     * @param themeID theme id
     * @return boolean
     */
    public boolean isThemeAvailable(String themeId) {
        if (themeId.equals(ChatThemeManager.getInstance().defaultChatThemeId)) // the default theme is always available
            return true;

        if (themeId == null || !mChatThemesList.containsKey(themeId))
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
        if (!isThemeAvailable(themeId) && mChatThemesList.containsKey(themeId)) // the second check is to avoid any null pointer exception at the getTheme call
        {
            return mAssetHelper.getMissingAssets(getTheme(themeId).getAssets());
        }
        return new String[0];
    }

    public void downloadAssetsForTheme(String themeId) {
        String[] assets = getMissingAssetsForTheme(themeId);
        mAssetHelper.assetDownloadRequest(assets);
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

                // looping to the no of indexes for a theme
                for (byte j = 0; j < HikeChatThemeConstants.ASSET_INDEX_COUNT; j++) {
                    JSONObject assetObj = t.getJSONObject(HikeChatThemeConstants.JSON_SIGNAL_THEME[j]);
                    int type = assetObj.getInt(HikeChatThemeConstants.JSON_SIGNAL_ASSET_TYPE);
                    String id = assetObj.getString(HikeChatThemeConstants.JSON_SIGNAL_ASSET_VALUE);

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

                mChatThemesList.put(themeID, theme);
            }

            Logger.d(TAG, "unique chat themes in MQTT packet :" + themeList.size());
            HikeConversationsDatabase.getInstance().saveChatThemes(themeList);
            HikeConversationsDatabase.getInstance().saveChatThemeAssets(assetsList);

            if (!areTheseAssetsOnApk) {
                for (HikeChatTheme theme : themeList) {
                    //querying for chat themes data (images) when the packet is received. The call might be removed later.
                    downloadAssetsForTheme(theme.getThemeId());
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void processDeleteThemeSignal(JSONObject data) {

    }

    //TODO CHATTHEME work on optimisation of this method
    public ArrayList<String> getAvailableThemeIds() {
        ArrayList<String> availableThemes = new ArrayList<>();
        for (String themeId : mChatThemesList.keySet()) {
            if (isThemeAvailable(themeId)) {
                availableThemes.add(themeId);
            }
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

    //TODO CHATTHEME to work downloading themes from server
    public void downloadThemeAssetsMetadata(String[] themeIds) {
        ArrayList<String> downloadThemeIds = new ArrayList<String>();
        for (String themeId : themeIds) {
            if (!mChatThemesList.containsKey(themeId)) {
                HikeChatTheme theme = new HikeChatTheme();
                theme.setThemeId(themeId);
                theme.setMetadata(HikeChatThemeConstants.CHAT_THEME_ID_NOT_DOWNLOADED);
                mChatThemesList.put(themeId, theme);
            }

            HikeChatTheme theme = getTheme(themeId);
            String themeMetadata = theme.getMetadata();
            if (themeMetadata != null && themeMetadata.equals(HikeChatThemeConstants.CHAT_THEME_ID_NOT_DOWNLOADED)) {
                downloadThemeIds.add(themeId);
            }
        }
        String[] downloadThemesArr = new String[downloadThemeIds.size()];
        for (int i = 0; i < downloadThemesArr.length; i++) {
            downloadThemesArr[i] = downloadThemeIds.get(i);
        }

        DownloadThemeContentTask downloadAssetIds;

        if (downloadThemesArr.length > 0) {
            downloadAssetIds = new DownloadThemeContentTask(downloadThemesArr);
            downloadAssetIds.execute();
        }

        ArrayList<HikeChatTheme> updateThemes = new ArrayList<>();
        for (String themeId : themeIds) {
            updateThemes.add(mChatThemesList.get(themeId));
        }
        HikeConversationsDatabase.getInstance().saveChatThemes(updateThemes);
    }


    public void initializeHikeChatThemesWithDefaultSet() {
        try {
            JSONObject jsonObj = new JSONObject(Utils.loadJSONFromAsset(HikeMessengerApp.getInstance().getApplicationContext(), HikeChatThemeConstants.CHATTHEMES_DEFAULT_JSON_FILE_NAME));
            if (jsonObj != null) {
                JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
                JSONArray themeData = data.getJSONArray(HikeChatThemeConstants.JSON_SIGNAL_THEME_DATA);
                processNewThemeSignal(themeData, true);
            } else {
                Log.v(TAG, "Unable to load " + HikeChatThemeConstants.CHATTHEMES_DEFAULT_JSON_FILE_NAME + " file from assets");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
