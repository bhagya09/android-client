package com.bsb.hike.chatthemes;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.chatthemes.model.ChatThemeToken;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeChatTheme;
import com.bsb.hike.models.HikeChatThemeAsset;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by sriram on 22/02/16.
 */
public class ChatThemeAssetHelper implements HikePubSub.Listener {
    private String TAG = "ChatThemeAssetHelper";

    // maintains the hashset of all recorded downloaded and non-downloaded assets
    private ConcurrentHashMap<String, HikeChatThemeAsset> mAssets;

    private String[] mPubSubListeners = {HikePubSub.CHATTHEME_CONTENT_DOWNLOAD_SUCCESS, HikePubSub.CHATTHEME_CONTENT_DOWNLOAD_FAILURE};

    public ChatThemeAssetHelper() {
        mAssets = HikeConversationsDatabase.getInstance().getAllChatThemeAssets();
        HikeMessengerApp.getPubSub().addListeners(this, mPubSubListeners);
    }

    public void saveChatThemeAsset(String assetId, HikeChatThemeAsset asset) {
        this.mAssets.put(assetId, asset);
    }

    public void saveChatThemeAssets(HikeChatThemeAsset[] assets) {
        for (HikeChatThemeAsset asset : assets) {
            saveChatThemeAsset(asset.getAssetId(), asset);
        }
    }

    public void clearAssets() {
        if(mAssets != null) {
            mAssets.clear();
        }
    }

    /**
     * Checks if all the assets for this is theme are available or not.
     *
     * @param theme HikeChatTheme
     * @return boolean
     */
    public boolean isAssetsAvailableForTheme(HikeChatTheme theme) {
        if (theme.getAssetDownloadStatus() != HikeChatThemeConstants.ASSET_STATUS_DOWNLOAD_COMPLETE) {
            updateAssetsDownloadStatus(theme);
        }
        return (theme.getAssetDownloadStatus() == HikeChatThemeConstants.ASSET_STATUS_DOWNLOAD_COMPLETE);
    }

    /**
     * Updates the Download status of a asset for given theme
     *
     * @param theme HikeChatTheme
     * @return void
     */

    public void updateAssetsDownloadStatus(HikeChatTheme theme) {
        String[] assets = theme.getAssets();
        for (int i = 0; i < HikeChatThemeConstants.ASSET_INDEX_COUNT; i++) {
            if ((assets[i] != null) && (hasAsset(assets[i])) && (mAssets.get(assets[i]).isDownloaded() || mAssets.get(assets[i]).isAssetOnApk())) {
                theme.setAssetDownloadStatus(1 << i);
            }
        }
    }

    /*
     * Assets are saved in the application cache directory, which is secured to some extent from the user, but not guaranteed When we are trying to set the theme, after the file
     * operation, we found like the asset is delete. though in db and datastructure we have the entry as successfully downloaded. we can make use of this method to reverse the
     * entry. *
     *
     * @param theme HikeChatTheme
     *
     * @param assetType Type of asset
     *
     * @return boolean
     */
    public void setAssetMissing(HikeChatTheme theme, byte assetIndex) {
        int assetStatus = theme.getAssetDownloadStatus();
        assetStatus &= ~(1 << assetIndex);
        theme.overrideAssetDownloadStatus(assetStatus);

        String assetId = theme.getAssetId(assetIndex);
        mAssets.get(assetId).setIsDownloaded(HikeChatThemeConstants.ASSET_DOWNLOAD_STATUS_NOT_DOWNLOADED);

        //updating the database as well
        boolean assetUpdated = HikeConversationsDatabase.getInstance().saveChatThemeAsset(mAssets.get(assetId));
        if (!assetUpdated) {
            Logger.d(TAG, "Unable to update the asset in the DB. DB problem");
        }
    }

    public String[] getMissingAssets(String[] assets) {
        HashSet<String> missingAssets = new HashSet<String>();// Hashset is choosen to avoid placing download request for duplicate assets
        for (String asset : assets) {
            if (!hasAsset(asset) || mAssets.get(asset).isAssetMissing()) {
                missingAssets.add(asset);
            }
        }
        return missingAssets.toArray(new String[missingAssets.size()]);
    }

    /**
     * Places the Network request to Download Assets, Place request only for image assets.
     *
     * @param String [] assetIds
     * @return void
     */
    public void assetDownloadRequest(ChatThemeToken token) {
        DownloadAssetsTask downloadAssets = new DownloadAssetsTask(token);
        downloadAssets.execute();
    }


    public void assetDownloadRequestAcrossThemes(HashMap<String, ChatThemeToken> tokenMap) {
        DownloadMultipleAssetsTask downloadAssets = new DownloadMultipleAssetsTask(tokenMap);
        downloadAssets.execute();
    }

    /**
     * method to check if an asset is recorded or not
     *
     * @param assetId assetId to be verified
     * @return true if the asset is recorded, else false
     */
    public boolean hasAsset(String assetId) {
        return mAssets.containsKey(assetId);
    }

    /**
     * method to return the value of an asset given it's UUID
     *
     * @param assetId the asset to be searched
     * @return value of the asset if present, null otherwise
     */
    public HikeChatThemeAsset getChatThemeAsset(String assetId) {
        return mAssets.get(assetId);
    }

    @Override
    public void onEventReceived(String type, Object object) {
        if (HikePubSub.CHATTHEME_CONTENT_DOWNLOAD_SUCCESS.equals(type)) {
            ChatThemeToken token = (ChatThemeToken) object;
            String[] downloadedAssets = token.getAssets();
            if(!Utils.isEmpty(downloadedAssets)) {
                ArrayList<HikeChatThemeAsset> downloadedThemeAssets = new ArrayList<>();

                for (String dAsset : downloadedAssets) {
                    HikeChatThemeAsset asset = mAssets.get(dAsset);
                    if (asset != null) {
                        asset.setIsDownloaded(HikeChatThemeConstants.ASSET_DOWNLOAD_STATUS_DOWNLOADED_SDCARD);
                        downloadedThemeAssets.add(asset);
                    }
                }
                //writing the downloaded assets into the tables in DB
                HikeConversationsDatabase.getInstance().saveChatThemeAssets(downloadedThemeAssets);
                HikeMessengerApp.getPubSub().publish(HikePubSub.CHATTHEME_DOWNLOAD_SUCCESS, token);
            }
        }
    }


}
