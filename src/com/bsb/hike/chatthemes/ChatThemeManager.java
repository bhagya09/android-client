package com.bsb.hike.chatthemes;

import com.bsb.hike.models.HikeChatTheme;

import java.util.HashMap;

/**
 * Created by sriram on 22/02/16.
 */
public class ChatThemeManager {
    private static ChatThemeManager mInstance;

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

    public void initialize() {
        mChatThemesList = new HashMap<>();
    }

}
