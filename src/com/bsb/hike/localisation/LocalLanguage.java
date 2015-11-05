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

    public static final LocalLanguage English = new LocalLanguage("English", "en");

    public static final LocalLanguage Hindi = new LocalLanguage("हिन्दी", "hi");

    public static final LocalLanguage Bengali = new LocalLanguage("বাংলা", "bn");

    public static final LocalLanguage Marathi = new LocalLanguage("मराठी", "mr");

    public static final LocalLanguage Gujarati = new LocalLanguage("ગુજરાતી", "gu");

    public static final LocalLanguage Tamil = new LocalLanguage("தமிழ்", "ta");

    public static final LocalLanguage Telugu = new LocalLanguage("తెలుగు", "te");

    public static final LocalLanguage Kannada = new LocalLanguage("ಕನ್ನಡ", "kn");

    public static final LocalLanguage Malayalam = new LocalLanguage("മലയാളം", "ml");

    private static ArrayList<LocalLanguage> hikeSupportedList;

    private static ArrayList<LocalLanguage> deviceSupportedHikeList;

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

    public static List<LocalLanguage> getHikeSupportedLanguages(Context context)
    {
        if (hikeSupportedList == null)
        {
            hikeSupportedList = new ArrayList<LocalLanguage>();
            hikeSupportedList.add(new LocalLanguage(context.getString(R.string.system_language), ""));    // system Default
            hikeSupportedList.add(English);     // English
            hikeSupportedList.add(Hindi);       // Hindi
            hikeSupportedList.add(Bengali);     // Bengali
            hikeSupportedList.add(Marathi);     // Marathi
            //hikeSupportedList.add(Gujarati);    // Gujarati
            hikeSupportedList.add(Tamil);       // Tamil
            hikeSupportedList.add(Telugu);      // Telugu
            hikeSupportedList.add(Kannada);     // Kannada
            hikeSupportedList.add(Malayalam);   // Malayalam
        }

        return hikeSupportedList;
    }

    public static List<LocalLanguage> getDeviceSupportedHikeLanguages(Context context)
    {
        if (deviceSupportedHikeList == null)
        {
            deviceSupportedHikeList = new ArrayList<LocalLanguage>();
            List<LocalLanguage> hikeList = getHikeSupportedLanguages(context);
            deviceSupportedHikeList.add(hikeList.get(0));
            deviceSupportedHikeList.add(hikeList.get(1));
            for (int i = 2; i < hikeList.size(); i++)
            {
                LocalLanguage item = hikeList.get(i);
                deviceSupportedHikeList.add(item);
            }
        }
        return deviceSupportedHikeList;
    }

}
