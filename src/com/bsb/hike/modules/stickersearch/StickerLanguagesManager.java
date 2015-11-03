package com.bsb.hike.modules.stickersearch;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by anubhavgupta on 28/10/15.
 */
public class StickerLanguagesManager {

    private static final String TAG = "StickerLanguagesManager";

    private static volatile StickerLanguagesManager _instance;

    public static final short NOT_DOWNLOADED_LANGUAGE_SET_TYPE = 0;

    public static final short DOWNLOADING_LANGUAGE_SET_TYPE = 1;

    public static final short DOWNLOADED_LANGUAGE_SET_TYPE = 2;

    public static final short FORBIDDEN_LANGUAGE_SET_TYPE = 3;

    private Map<String, String> localLanguagesMap;

    private StickerLanguagesManager()
    {
        initialiseLocalLanguagesMap();
    }

    public static StickerLanguagesManager getInstance()
    {
        if (_instance == null)
        {
            synchronized (StickerLanguagesManager.class)
            {
                if (_instance == null)
                {
                    _instance = new StickerLanguagesManager();
                }
            }
        }

        return _instance;
    }

    public void downloadTagsForLanguage(String language)
    {
        if(containsLanguage(DOWNLOADING_LANGUAGE_SET_TYPE, language) || containsLanguage(DOWNLOADED_LANGUAGE_SET_TYPE, language) || containsLanguage(FORBIDDEN_LANGUAGE_SET_TYPE, language))
        {
            Logger.d(TAG, "language cannnot be downloaded " + this.toString());
            return ;
        }

        Logger.d(TAG, "language to be added to non downloaded list : " + language);
        Logger.d(TAG, "dump 1 : " + this.toString());
        addToLanguageSet(NOT_DOWNLOADED_LANGUAGE_SET_TYPE, Collections.singletonList(language));
        if(Utils.isEmpty(getLanguageSet(DOWNLOADING_LANGUAGE_SET_TYPE)))
        {
            downloadTagsForNextLanguage();
        }
    }

    public void downloadTagsForNextLanguage()
    {
        Logger.d(TAG, "dump 2.1 : " + this.toString());
        Set<String> downloadingLanguages = getLanguageSet(DOWNLOADING_LANGUAGE_SET_TYPE);
        addToLanguageSet(DOWNLOADED_LANGUAGE_SET_TYPE, downloadingLanguages);
        removeFromLanguageSet(DOWNLOADING_LANGUAGE_SET_TYPE, downloadingLanguages);
        Logger.d(TAG, "language added to downloaded list : " + downloadingLanguages);
        Logger.d(TAG, "dump 2.2 : " + this.toString());

        Set<String> undownloadedLanguageList = getLanguageSet(NOT_DOWNLOADED_LANGUAGE_SET_TYPE);

        Iterator<String> iterator = undownloadedLanguageList.iterator();
        if(!iterator.hasNext())
        {
            return ;
        }

        String language = iterator.next();
        List<String> undownloadedLanguages = Collections.singletonList(language);

        addToLanguageSet(DOWNLOADING_LANGUAGE_SET_TYPE, undownloadedLanguages);
        removeFromLanguageSet(NOT_DOWNLOADED_LANGUAGE_SET_TYPE, undownloadedLanguages);
        Logger.d(TAG, "language added to downloading list " + language);
        Logger.d(TAG, "dump 3 : " + this.toString());

        StickerSearchManager.getInstance().downloadStickerTags(true, StickerSearchConstants.STATE_LANGUAGE_TAGS_DOWNLOAD, getLanguageSet(DOWNLOADING_LANGUAGE_SET_TYPE));
    }

    public void retryDownloadDefaultTagsForLanguages()
    {
        Map<String, Boolean> defaultTagLanguageStatusMap = (HashMap<String, Boolean> ) HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.DEFAULT_TAG_DOWNLOAD_LANGUAGES_PREF).getAllData();
        ArrayList<String> languages = new ArrayList<>();
        for(String lang : defaultTagLanguageStatusMap.keySet())
        {
            if(defaultTagLanguageStatusMap.get(lang) == false) // not downloaded
            {
                languages.add(lang);
            }
        }
        StickerManager.getInstance().downloadDefaultTags(false, languages);
    }

    public void redownloadAllDefaultTagsForLanguages(boolean isSignUp)
    {
        Map<String, Boolean> defaultTagLanguageStatusMap = (HashMap<String, Boolean> ) HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.DEFAULT_TAG_DOWNLOAD_LANGUAGES_PREF).getAllData();
        ArrayList<String> languages = new ArrayList<>();
        for(String lang : defaultTagLanguageStatusMap.keySet())
        {
            HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.DEFAULT_TAG_DOWNLOAD_LANGUAGES_PREF).saveData(lang, false);
            languages.add(lang);
        }
        StickerManager.getInstance().downloadDefaultTags(isSignUp, languages);
    }

    public void downloadDefaultTagsForLanguage(String language)
    {
        HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.DEFAULT_TAG_DOWNLOAD_LANGUAGES_PREF).saveData(language, false);
        StickerManager.getInstance().downloadDefaultTags(false, Collections.singleton(language));
    }

    public void addToLanguageSet(int type, Collection<String> languages)
    {
        if(Utils.isEmpty(languages))
        {
            return ;
        }

        switch (type)
        {
            case NOT_DOWNLOADED_LANGUAGE_SET_TYPE:
                HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.NOT_DOWNLOADED_LANGUAGES_SET, getNewSet(HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.NOT_DOWNLOADED_LANGUAGES_SET, new HashSet<String>()), languages, true));
                break;
            case DOWNLOADING_LANGUAGE_SET_TYPE:
                HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.DOWNLOADING_LANGUAGES_SET, getNewSet(HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.DOWNLOADING_LANGUAGES_SET, new HashSet<String>()), languages, true));
                break;
            case DOWNLOADED_LANGUAGE_SET_TYPE:
                HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.DOWNLOADED_LANGUAGES_SET, getNewSet(HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.DOWNLOADED_LANGUAGES_SET, new HashSet<String>()), languages, true));
                break;
            case FORBIDDEN_LANGUAGE_SET_TYPE:
                HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.FORBIDDEN_LANGUAGES_SET, getNewSet(HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.FORBIDDEN_LANGUAGES_SET, new HashSet<String>()), languages, true));
                break;
        }
    }

    public void removeFromLanguageSet(int type, Collection<String> languages)
    {
        if(Utils.isEmpty(languages))
        {
            return ;
        }

        switch (type)
        {
            case NOT_DOWNLOADED_LANGUAGE_SET_TYPE:
                HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.NOT_DOWNLOADED_LANGUAGES_SET, getNewSet(HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.NOT_DOWNLOADED_LANGUAGES_SET, new HashSet<String>()), languages, false));
                break;
            case DOWNLOADING_LANGUAGE_SET_TYPE:
                HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.DOWNLOADING_LANGUAGES_SET, getNewSet(HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.DOWNLOADING_LANGUAGES_SET, new HashSet<String>()), languages, false));
                break;
            case DOWNLOADED_LANGUAGE_SET_TYPE:
                HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.DOWNLOADED_LANGUAGES_SET, getNewSet(HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.DOWNLOADED_LANGUAGES_SET, new HashSet<String>()), languages, false));
                break;
            case FORBIDDEN_LANGUAGE_SET_TYPE:
                HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.FORBIDDEN_LANGUAGES_SET, getNewSet(HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.FORBIDDEN_LANGUAGES_SET, new HashSet<String>()), languages, false));
                break;
        }
    }

    public Set<String> getLanguageSet(int type)
    {
        switch (type)
        {
            case NOT_DOWNLOADED_LANGUAGE_SET_TYPE:
                return HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.NOT_DOWNLOADED_LANGUAGES_SET, new HashSet<String>());
            case DOWNLOADING_LANGUAGE_SET_TYPE:
                return HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.DOWNLOADING_LANGUAGES_SET, new HashSet<String>());
            case DOWNLOADED_LANGUAGE_SET_TYPE:
                return  HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.DOWNLOADED_LANGUAGES_SET, new HashSet<String>(Collections.singleton(StickerSearchConstants.DEFAULT_KEYBOARD_LANGUAGE)));
            case FORBIDDEN_LANGUAGE_SET_TYPE:
                return  HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.FORBIDDEN_LANGUAGES_SET, new HashSet<String>());
            default:
                return HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.DOWNLOADED_LANGUAGES_SET, new HashSet<String>(Collections.singleton(StickerSearchConstants.DEFAULT_KEYBOARD_LANGUAGE)));
        }
    }

    public boolean containsLanguage(int type, String language)
    {
        Set<String> set = getLanguageSet(type);

        if(Utils.isEmpty(set) || !set.contains(language)) {
            return false;
        }
        else {
            return true;
        }
    }

    private Set<String> getNewSet(Set<String> existingSet, Collection<String> newCollection , Boolean addOperation)
    {
        Set<String> resultSet = new HashSet<>(existingSet);

        if(addOperation)
        {
            resultSet.addAll(newCollection);
        }
        else
        {
            resultSet.removeAll(newCollection);
        }
        return resultSet;
    }

    private void initialiseLocalLanguagesMap() {

        localLanguagesMap = new HashMap<>();
        localLanguagesMap.put("English", "eng");
        localLanguagesMap.put("हिन्दी", "hin");
        localLanguagesMap.put("বাংলা", "ben");
        localLanguagesMap.put("मराठी", "mar");
        localLanguagesMap.put("ગુજરાતી", "guj");
        localLanguagesMap.put("தமிழ்", "tam");
        localLanguagesMap.put("తెలుగు", "tel");
        localLanguagesMap.put("ಕನ್ನಡ", "kan");
        localLanguagesMap.put("മലയാളം", "mal");
    }

    public String getLanguageCode(String language)
    {
        return localLanguagesMap.get(language);
    }

    public Map<String, String> getLanguagesMap()
    {
        return localLanguagesMap;
    }


    public String toString()
    {
        return      "\n"
                  + "Not Downloaded Set  : " + getLanguageSet(NOT_DOWNLOADED_LANGUAGE_SET_TYPE)
                  + "\n"
                  + "Downloading Set : "     + getLanguageSet(DOWNLOADING_LANGUAGE_SET_TYPE)
                  + "\n"
                  + "Downloaded Set : "      + getLanguageSet(DOWNLOADED_LANGUAGE_SET_TYPE)
                  + "\n"
                  + "Forbidden Set : "       + getLanguageSet(FORBIDDEN_LANGUAGE_SET_TYPE);

    }

    public void clearAllSets()
    {
        HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.NOT_DOWNLOADED_LANGUAGES_SET);
        HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.DOWNLOADING_LANGUAGES_SET);
        HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.DOWNLOADED_LANGUAGES_SET);
        HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.FORBIDDEN_LANGUAGES_SET);
    }

    public String listToSting(Collection<String> languages)
    {
        String result = "";

        for(String lang : languages)
        {
            result += lang + ",";
        }
        return result.substring(0, result.length() -1); // remove last comma
    }

    public Set<String> getAccumulatedSet(int... types)
    {
        Set<String> accumulatedSet = new HashSet<>();

        for(int type : types)
        {
            accumulatedSet.addAll(getLanguageSet(type));
        }
        return accumulatedSet;
    }
}

