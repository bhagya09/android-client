package com.bsb.hike.chatthemes;

import com.bsb.hike.models.HikeChatTheme;

import java.util.HashMap;

/**
 * Created by sriram on 22/02/16.
 */
public class ChatThemeManager {
    private static ChatThemeManager mInstance;

    //Helps the manager class with all the asset maintainance
    private ChatThemeAssetHelper mAssetHelper;

    //Helps the manager with all the Drawable assets
    private ChatThemeDrawableHelper mDrawableHelper;

    // Maintains the Map of Chatthemes
    private HashMap<String, HikeChatTheme> mChatThemesList;

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
        mChatThemesList = new HashMap<>();
        mDrawableHelper = new ChatThemeDrawableHelper();
        mAssetHelper = new ChatThemeAssetHelper();
    }

    private HikeChatTheme getTheme(String themeId) {
        return mChatThemesList.get(themeId);
    }

    /**
     * Checks if all the assets for this is theme are available or not
     *
     * @param  themeID  theme id
     * @return      boolean
     *
     */
    public boolean isThemeAvailable(String themeId){
        HikeChatTheme theme = getTheme(themeId);
        return mAssetHelper.isAssetsAvailableForTheme(theme);
    }

}
