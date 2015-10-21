package com.bsb.hike.localisation;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

/**
 * Created by gauravmittal on 20/10/15.
 */
public class LocalLanguageUtils {

    public static boolean isLocalLanguageSelected()
    {
        return HikeSharedPreferenceUtil.getInstance().contains(HikeConstants.LOCAL_LANGUAGE_PREF);
    }

    public static String getApplicationLocalLanguageLocale()
    {
        return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.LOCAL_LANGUAGE_PREF, "");
    }

    public static void setApplicationLocalLanguage(LocalLanguage lang)
    {
        if (TextUtils.isEmpty(lang.getLocale())) {
            HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.LOCAL_LANGUAGE_PREF);
        } else {
            HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.LOCAL_LANGUAGE_PREF, lang.getLocale());
        }
    }
}
