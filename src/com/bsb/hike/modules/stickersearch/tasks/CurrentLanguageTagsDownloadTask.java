package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.modules.stickersearch.StickerLanguagesManager;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.utils.Logger;

/**
 * Created by anubhavgupta on 04/11/15.
 */
public class CurrentLanguageTagsDownloadTask implements  Runnable {

    private static final String TAG = "CurrentLanguageDownloadTask";

    public CurrentLanguageTagsDownloadTask()
    {

    }

    public void downloadCurrentLanguageTags()
    {
        String currentLanguage = StickerSearchUtils.getCurrentLanguageISOCode();
        Logger.d(TAG, "current language : " + currentLanguage);
        Logger.d(TAG, "forbidden set : " + StickerLanguagesManager.getInstance().getLanguageSet(StickerLanguagesManager.FORBIDDEN_LANGUAGE_SET_TYPE));

        StickerLanguagesManager.getInstance().downloadTagsForLanguage(currentLanguage);
        StickerLanguagesManager.getInstance().downloadDefaultTagsForLanguage(currentLanguage);
    }

    @Override
    public void run() {
        downloadCurrentLanguageTags();
    }
}
