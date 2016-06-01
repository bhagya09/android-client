package com.bsb.hike.localisation;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.LOCAL_LANGUAGE_PREF, LocalLanguage.PhoneLangauge.getLocale());
    }

    public static LocalLanguage getApplicationLocalLanguage(Context context)
    {
        String currentLocalLangLocale = getApplicationLocalLanguageLocale();
        for (LocalLanguage ll : LocalLanguage.getHikeSupportedLanguages(context)) {
            if (currentLocalLangLocale.equals(ll.getLocale()))
                return ll;
        }
        return null;
    }

    synchronized public static boolean setApplicationLocalLanguage(LocalLanguage lang, String source)
    {
        if (HikeMessengerApp.isLocalisationEnabled())
        {
            if (TextUtils.isEmpty(lang.getLocale())) {
                HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.LOCAL_LANGUAGE_PREF);
            } else {
                HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.LOCAL_LANGUAGE_PREF, lang.getLocale());
            }

            HikeMessengerApp.getInstance().setupLocalLanguage();
            Utils.sendLocaleToServer();
            sendAnalyticsForAppLanguageChanged(source);
            HikeMessengerApp.getPubSub().publish(HikePubSub.LOCAL_LANGUAGE_CHANGED,lang);
            return true;
        }
        return false;
    }

    public static Locale getDeviceDefaultLocale()
    {
        return Locale.getDefault();
    }

    public static void requestLanguageOrderListFromServer()
    {

        IRequestListener langcallRequestListener = new IRequestListener()
        {
            @Override
            public void onRequestSuccess(Response result)
            {

                try
                {
                    JSONObject jObject = (JSONObject) result.getBody().getContent();
                    JSONArray langlistarray = jObject.getJSONArray(HikeConstants.LANG_LIST_ORDER);
                    StringBuffer listS = new StringBuffer();

                    if(langlistarray!=null){
                        for (int i = 0; i < langlistarray.length(); i++)
                        {
                            listS .append(langlistarray.getString(i).trim());
                            if (i != (langlistarray.length() - 1))
                            {
                                listS.append(",");
                            }
                        }
                        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.LANG_LIST_ORDER, listS.toString());
                        HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.UPGRADE_LANG_ORDER, 1);

                       handleHikeSupportedListOrderChange(HikeMessengerApp.getInstance().getApplicationContext());

                    }
                }
                catch (JSONException e)
                {
                }

            }

            @Override
            public void onRequestProgressUpdate(float progress)
            {
            }

            @Override
            public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
            {

            }
        };

        if (!HikeSharedPreferenceUtil.getInstance().contains(HikeConstants.LANG_LIST_ORDER)&&HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.UPGRADE_LANG_ORDER,0)==0)
        {
            RequestToken langlistToken = HttpRequests.getLanguageListOrderHTTP(langcallRequestListener);
            langlistToken.execute();
        }

    }
    public static void handleHikeSupportedListOrderChange(Context context){
        sortHikeSupportedLanguage(LocalLanguage.getHikeSupportedLanguages(context));
        LocalLanguage.refreshdeviceSupportedHikeList(context);

    }
    public static void sortHikeSupportedLanguage(List<LocalLanguage> list)
    {
        String serverString = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.LANG_LIST_ORDER, null);
        List<LocalLanguage> sortedList = new ArrayList<LocalLanguage>();
        if (serverString != null)
        {
            String[] langorder = serverString.split(",");
            boolean[] visited = new boolean[list.size()];
            // All items with null locale
            for (int i = 0; i < list.size(); i++)
            {
                LocalLanguage item = list.get(i);
                if (LocalLanguage.PhoneLangauge.getLocale().equals(item.getLocale()))
                {
                    sortedList.add(list.get(i));
                    visited[i] = true;
                }
            }
            // In order defined by Server
            for (String locale : langorder)
            {
                for (int i = 0; i < list.size(); i++)
                {
                    LocalLanguage temp = list.get(i);
                    if (temp!=null&&!visited[i] && locale.equalsIgnoreCase(temp.getLocale()))
                    {
                        visited[i] = true;
                        sortedList.add(temp);

                    }
                }
            }
            for (int i = 0; i < list.size(); i++)
            {
                if (!visited[i])
                {
                    sortedList.add(list.get(i));
                }

            }
            // Copy Sorted List to List
            list.clear();
            list.addAll(sortedList);
            for(LocalLanguage l:list)
            Logger.d("langlist"," --"+l);

        }

    }

    public static int getCurrentLocaleDisplayNameResourceID(String locale){
        switch (locale){
            case "bn":
                return R.string.bengali;
            case "gu":
                return R.string.gujarati;
            case "hi":
                return R.string.hindi;
            case "kn":
                return R.string.kannada;
            case "ml":
                return R.string.malyalam;
            case "mr":
                return R.string.marathi;
            case "ta":
                return R.string.tamil;
            case "te":
                return R.string.telugu;
            default:
                return R.string.english;
        }
    }
    
    public static void sendAnalyticsForAppLanguageChanged(String source) {
    	try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.APP_LANGUAGE_CHANGED_EVENT);
			metadata.put(HikeConstants.APP_LANGUAGE, getApplicationLocalLanguageLocale());
			metadata.put(HikeConstants.APP_LANGUAGE_CHANGE_SOURCE, source);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json : " + e);
		}
    }
}
