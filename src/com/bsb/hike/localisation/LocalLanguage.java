package com.bsb.hike.localisation;

import android.content.Context;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gauravmittal on 20/10/15.
 */
public class LocalLanguage {

    private String locale;

    private String name;

    public LocalLanguage(String languageName, String locale) {
        this.name = languageName;
        this.locale = locale;
    }

    public String getLocale() {
        return locale;
    }

    public String getDisplayName() {
        return name;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    public static List<LocalLanguage> getSupportedLanguages(Context context) {
        ArrayList<LocalLanguage> list = new ArrayList<LocalLanguage>();
        list.add(new LocalLanguage(context.getString(R.string.system_language), ""));    // system Default
        list.add(new LocalLanguage("English", "en"));    // English
        list.add(new LocalLanguage("हिन्दी", "hi"));    // Hindi
        list.add(new LocalLanguage("বাংলা", "bn"));    // Bengali
        list.add(new LocalLanguage("मराठी", "mr"));    // Marathi
        list.add(new LocalLanguage("ગુજરાતી", "gu"));    // Gujarati
        list.add(new LocalLanguage("தமிழ்", "ta"));    // Tamil
        list.add(new LocalLanguage("తెలుగు", "te"));    // Telugu
        list.add(new LocalLanguage("ಕನ್ನಡ", "kn"));    // Kannada
        list.add(new LocalLanguage("മലയാളം", "ml"));    // Malayalam

        return list;
    }





}
