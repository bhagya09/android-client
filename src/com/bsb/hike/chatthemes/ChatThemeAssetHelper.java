package com.bsb.hike.chatthemes;

import com.bsb.hike.models.HikeChatTheme;
import com.bsb.hike.models.HikeChatThemeAsset;

import java.util.HashSet;

/**
 * Created by sriram on 22/02/16.
 */
public class ChatThemeAssetHelper {

    //maintains the hashset of downloaded themes
    private HashSet<String> mDownloadedThemes;


    public void addDownloadedAsset(String assetId) {
        this.mDownloadedThemes.add(assetId);
    }

    /**
     * Checks if all the assets for this is theme are available or not
     *
     * Especially when we are populating the ThemePallete
     * Looping through the complete datastructure or query sql for every theme, and finding the status of downloaded assets consumes lot of time
     * a bit for each assets is maintained such that, we do a bit comparison for all the 11 assets.
     * if the bit pattern is 11111111111 i.e 0x7FF is matched, then all the assets for that theme are downloaded.

     * We pool the database and set the bits for the very first time. Given a scenario where assets being downloaded when the application is running
     * Every every batch of assets downloaded, updating the database and polling for the updated data will take time
     * We are maintaining "HashSet" of Downloaded assets. So the algo here is

     * 1. Check for the downloaded assets pattern
     * 2. if does not match, check those assets in Hashset, and update Bits.
     * 3. check for the pattern and return the result.
     *
     * @param  theme  HikeChatTheme
     * @return      boolean
     *
     */
    public boolean isAssetsAvailableForTheme(HikeChatTheme theme){
        if (theme.getAssetDownloadStatus() != HikeChatThemeConstants.ASSET_DOWNLOAD_COMPLETE_STATUS) {
            HikeChatThemeAsset[] assets = theme.getAssets();
            for (int i = 0; i < HikeChatThemeConstants.ASSET_COUNT; i++) {
                if ((assets[i] != null) && (mDownloadedThemes.contains(assets[i].getAssetId()))) {
                    theme.setAssetDownloadStatus(1 << i);
                }
            }
        }
        return ((theme.getAssetDownloadStatus() & HikeChatThemeConstants.ASSET_DOWNLOAD_COMPLETE_STATUS) == HikeChatThemeConstants.ASSET_DOWNLOAD_COMPLETE_STATUS);
    }

    /*
    * Assets are saved in the application cache directory, which is secured to some extent from the user, but not guaranteed
    * When we are trying to set the theme, after the file operation, we found like the asset is delete. though in db and datastructure we have the entry as successfully downloaded.
    * we can make use of this method to reverse the entry.
    * *
     * @param  theme  HikeChatTheme
     * @param  assetType  Type of asset
     * @return      boolean
     *
     */
    public void setAssetMissing(HikeChatTheme theme, byte assetType) {
        int assetStatus = theme.getAssetDownloadStatus();
        assetStatus &= ~(1 << assetType);
        theme.overrideAssetDownloadStatus(assetStatus);

        //TODO Update asset missing in DB here
    }

}
