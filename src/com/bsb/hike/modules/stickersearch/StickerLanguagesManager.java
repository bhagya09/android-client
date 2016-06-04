package com.bsb.hike.modules.stickersearch;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.localisation.FontManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

/**
 * Created by anubhavgupta on 28/10/15.
 * Modified : 3/12/15
 */
public class StickerLanguagesManager {

    public static final Set<Locale> LOCALES_SET = new HashSet<Locale>
            (Arrays.asList(Locale.getAvailableLocales()));

    public static Set<String> ISO_LANGUAGES;

    private static final String TAG = "StickerLanguagesManager";

    private static volatile StickerLanguagesManager _instance;

    public static final short NOT_DOWNLOADED_LANGUAGE_SET_TYPE = 0;

    public static final short DOWNLOADING_LANGUAGE_SET_TYPE = 1;

    public static final short DOWNLOADED_LANGUAGE_SET_TYPE = 2;

    public static final short FORBIDDEN_LANGUAGE_SET_TYPE = 3;

    private StickerLanguagesManager()
    {
        initialiseIsoLanguages();
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
        if(!isValidISOLanguage(language) || containsLanguage(DOWNLOADING_LANGUAGE_SET_TYPE, language) || containsLanguage(DOWNLOADED_LANGUAGE_SET_TYPE, language) || containsLanguage(FORBIDDEN_LANGUAGE_SET_TYPE, language))
        {
            Logger.d(TAG, "language : " + language + " cannnot be downloaded " + this.toString());
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
		@SuppressWarnings("unchecked")
		Map<String, Boolean> defaultTagLanguageStatusMap = (HashMap<String, Boolean>) HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.DEFAULT_TAG_DOWNLOAD_LANGUAGES_PREF)
				.getAllData();
		ArrayList<String> languages = new ArrayList<>();
		for (String lang : defaultTagLanguageStatusMap.keySet())
		{
			if (defaultTagLanguageStatusMap.get(lang) == false) // not downloaded
			{
				languages.add(lang);
			}
		}
		StickerManager.getInstance().downloadDefaultTags(false, languages);
	}

    public void redownloadAllDefaultTagsForLanguages(boolean isSignUp)
	{
		@SuppressWarnings("unchecked")
		Map<String, Boolean> defaultTagLanguageStatusMap = (HashMap<String, Boolean>) HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.DEFAULT_TAG_DOWNLOAD_LANGUAGES_PREF)
				.getAllData();
		ArrayList<String> languages = new ArrayList<>();
		for (String lang : defaultTagLanguageStatusMap.keySet())
		{
			HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.DEFAULT_TAG_DOWNLOAD_LANGUAGES_PREF).saveData(lang, false);
			languages.add(lang);
		}
		StickerManager.getInstance().downloadDefaultTags(isSignUp, languages);
	}

    public void downloadDefaultTagsForLanguage(String language)
    {
        if(!isValidISOLanguage(language) || containsLanguage(FORBIDDEN_LANGUAGE_SET_TYPE, language) ||  HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.DEFAULT_TAG_DOWNLOAD_LANGUAGES_PREF).getData(language, false))
        {
            Logger.d(TAG, "language: " + language + " is wrong or forbidden or defaults tags are already downloaded " + this.toString());
            return ;
        }
        HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.DEFAULT_TAG_DOWNLOAD_LANGUAGES_PREF).saveData(language, false);
        StickerManager.getInstance().downloadDefaultTags(false, Collections.singleton(language));
    }

    public void addToLanguageSet(int type, Collection<String> languages)
    {
        languages = getValidLanguageCollection(languages);
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
        languages = getValidLanguageCollection(languages);

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
                return  HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.DOWNLOADED_LANGUAGES_SET, new HashSet<String>(Collections.singleton(StickerSearchConstants.DEFAULT_KEYBOARD_LANGUAGE_ISO_CODE)));
            case FORBIDDEN_LANGUAGE_SET_TYPE:
                return  HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.FORBIDDEN_LANGUAGES_SET, new HashSet<String>());
            default:
                return HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.DOWNLOADED_LANGUAGES_SET, new HashSet<String>(Collections.singleton(StickerSearchConstants.DEFAULT_KEYBOARD_LANGUAGE_ISO_CODE)));
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

    private void initialiseIsoLanguages()
    {
        ISO_LANGUAGES = new HashSet<>(LOCALES_SET.size());

        for (Locale locale : LOCALES_SET) {
            try {
                String currLang = StickerSearchUtils.getISOCodeFromLocale(locale);
                ISO_LANGUAGES.add(currLang);
            } catch (MissingResourceException e) {
                Logger.e(TAG, "missing local language code for locale : " + locale);
            }
        }

        Logger.d(TAG, "initialising valid languages collection, valid languages :  " + ISO_LANGUAGES);

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
		Logger.d(TAG, "clearing all language sets");
		HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.NOT_DOWNLOADED_LANGUAGES_SET);
		HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.DOWNLOADING_LANGUAGES_SET);
		HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.DOWNLOADED_LANGUAGES_SET);
		HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.FORBIDDEN_LANGUAGES_SET);

		@SuppressWarnings("unchecked")
		Map<String, Boolean> defaultTagLanguageStatusMap = (HashMap<String, Boolean>) HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.DEFAULT_TAG_DOWNLOAD_LANGUAGES_PREF)
				.getAllData();
		for (String lang : defaultTagLanguageStatusMap.keySet())
		{
			HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.DEFAULT_TAG_DOWNLOAD_LANGUAGES_PREF).removeData(lang);
		}
	}

	public String listToString(Collection<String> languages)
	{
		String result = "";

		if (Utils.isEmpty(languages))
		{
			return result;
		}

		for (String lang : languages)
		{
			result += lang + ",";
		}

		return result.substring(0, result.length() - 1); // remove last comma
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

    public void checkAndUpdateForbiddenList(JSONObject result)
    {
        try {
            JSONArray languagesArray = result.getJSONArray(HikeConstants.UNKNOWN_KEYBOARDS);
            if(languagesArray == null || languagesArray.length() == 0)
            {
                return ;
            }

            List<String> languages = new Gson().fromJson(languagesArray.toString(), ArrayList.class);

            languages = (List<String>) getValidLanguageCollection(languages);

            if(Utils.isEmpty(languages))
            {
                Logger.e(TAG, "no valid languages to be added to forbidden list");
            }

            Logger.d(TAG, "languages added to forbidden list : " + languages);

            //removing from rest of sets
            StickerLanguagesManager.getInstance().removeFromLanguageSet(StickerLanguagesManager.NOT_DOWNLOADED_LANGUAGE_SET_TYPE, languages);
            StickerLanguagesManager.getInstance().removeFromLanguageSet(StickerLanguagesManager.DOWNLOADING_LANGUAGE_SET_TYPE, languages);
            StickerLanguagesManager.getInstance().removeFromLanguageSet(StickerLanguagesManager.DOWNLOADED_LANGUAGE_SET_TYPE, languages);

            // adding to forbidden set
            StickerLanguagesManager.getInstance().addToLanguageSet(StickerLanguagesManager.FORBIDDEN_LANGUAGE_SET_TYPE, languages);
        } catch (Exception e) {
            Logger.e(TAG, "exception in json parsing while updating forbidden list : ", e);
        }
    }

    public static boolean isValidISOLanguage(String s) {
        return ISO_LANGUAGES.contains(s);
    }

    public Collection<String> getValidLanguageCollection(Collection<String> languages)
    {
        if(!Utils.isEmpty(languages)) {
            languages = new ArrayList<>(languages);
            Iterator<String> iterator = languages.iterator();
            while (iterator.hasNext()) {
                String lang = iterator.next();
                if (!isValidISOLanguage(lang)) {
                    iterator.remove();
                }
            }
        }
        return languages;
    }

    public List<String> getUnsupportedLanguagesCollection()
    {
        List<String> unsupportedItems = FontManager.getInstance().getUnsupportedLanguageList();
        if(Utils.isEmpty(unsupportedItems))
        {
            return null;
        }

        List<String> unsupportedLanguages = new ArrayList<>(unsupportedItems.size());
        for(String locale : unsupportedItems)
        {
            unsupportedLanguages.add(StickerSearchUtils.getISOCodeFromLocale(new Locale(locale)));
        }

        return unsupportedLanguages;
    }

    public void addKptSupportedLanguages()
    {
        ArrayList<String> kptList = new ArrayList<String>(FontManager.getInstance().getSupportedLanguageList().size());
        for(String locale : FontManager.getInstance().getSupportedLanguageList())
        {
            kptList.add(StickerSearchUtils.getISOCodeFromLocale(new Locale(locale)));
        }

        Logger.d(TAG, "kpt list of languages : " + kptList);
        ISO_LANGUAGES.addAll(kptList);
    }
}

