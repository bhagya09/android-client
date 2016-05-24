package com.bsb.hike.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author paramshah
 */

public class BirthdayUtils
{
    public static final String TAG = "BirthdayUtils";

    private static final String INVALID_PREF_VALUE = "-1";

    private static final Long DEFAULT_CACHE_TIME_FOR_BDAY_CALL = 1* 60 * 60 * 1000l;

    /**
     * Method to update birthday privacy pref. Calls {@link #sendBDPrefToServer(String, JSONObject, boolean)}
     * to make HTTP call for update
     * @param bdSelectedPrefId
     * @param isDueToFavToFriends
     */
    public static void updateBDPrivacy(String bdSelectedPrefId, boolean isDueToFavToFriends)
    {
        Logger.d(TAG, "initiating update of birthday privacy: pref=" + bdSelectedPrefId + " isDueToFavToFriends:" + isDueToFavToFriends);
        if(bdSelectedPrefId.equals(INVALID_PREF_VALUE))
        {
            Logger.d(TAG, "invalid birthday privacy pref value");
            showBDUpdateFailureToast();
            return;
        }

        Logger.d(TAG, "new birthday privacy id: " + bdSelectedPrefId);

        JSONObject payload = new JSONObject();

        try
        {
            payload.put(HikeConstants.Extras.PREF, Integer.valueOf(bdSelectedPrefId));
            sendBDPrefToServer(bdSelectedPrefId, payload, isDueToFavToFriends);
        }
        catch (JSONException jse)
        {
            Logger.d(TAG, "error in forming request object for birthday privacy update");
            showBDUpdateFailureToast();
        }
    }

    /**
     * Method to notify server of birthday privacy pref update via HTTP
     * @param bdSelectedPrefId
     * @param payload
     * @param isDueToFavToFriends
     */
    private static void sendBDPrefToServer(final String bdSelectedPrefId, JSONObject payload, final boolean isDueToFavToFriends)
    {
        Logger.d(TAG, "dob update payload: " + payload.toString());

        RequestToken bdPrefUpdateRequest = HttpRequests.getBDPrefUpdateRequest(payload, new IRequestListener()
        {
            @Override
            public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
            {
                Logger.d(TAG, "updating bd pref http failure code: " + httpException.getErrorCode());
                showBDUpdateFailureToast();
            }

            @Override
            public void onRequestSuccess(Response result)
            {
                Logger.d(TAG, "http request result code: " + result.getStatusCode());
                saveBDPrivacyPref(bdSelectedPrefId);
                HikeMessengerApp.getPubSub().publishOnUI(HikePubSub.BD_PRIVACY_PREF_UPDATED, isDueToFavToFriends);
            }

            @Override
            public void onRequestProgressUpdate(float progress)
            {
                //doing nothing here
            }
        });

        bdPrefUpdateRequest.execute();
    }

    /**
     * Method to show toast in case of any failure in updating birthday privacy
     */
    private static void showBDUpdateFailureToast()
    {
        Context hikeAppContext = HikeMessengerApp.getInstance().getApplicationContext();
        String toastMsg = hikeAppContext.getString(R.string.bd_change_failed);
        Toast.makeText(hikeAppContext, toastMsg, Toast.LENGTH_SHORT).show();
    }

    /**
     * Method to save birthday privacy pref in shared pref
     * @param bdPrefValue
     */
    public static void saveBDPrivacyPref(String bdPrefValue)
    {
        Logger.d(TAG, "saving new birthday privacy setting: " + bdPrefValue);
        Context hikeAppContext = HikeMessengerApp.getInstance().getApplicationContext();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(hikeAppContext);
        settings.edit().putString(HikeConstants.BIRTHDAY_PRIVACY_PREF, bdPrefValue).commit();
    }

    /**
     * Method to fetch current/default birthday privacy from shared pref
     * @return
     */
    public static String getCurrentBDPref()
    {
        Context hikeAppContext = HikeMessengerApp.getInstance().getApplicationContext();
        String defValue = hikeAppContext.getString(R.string.privacy_favorites);
        return PreferenceManager.getDefaultSharedPreferences(hikeAppContext).getString(HikeConstants.BIRTHDAY_PRIVACY_PREF, defValue);
    }

    /**
     * Method to toggle update birthday privacy based on whether favToFriends is switched on
     * @param favToFriends
     */
    public static void modifyBDPrefForFavToFriends(boolean favToFriends)
    {
        Logger.d(TAG, "modifying birthday privacy for favToFriends");
        Context hikeAppContext = HikeMessengerApp.getInstance().getApplicationContext();
        String currPref = getCurrentBDPref();
        if(favToFriends && (currPref.equals(hikeAppContext.getString(R.string.privacy_my_contacts))
                || currPref.equals(hikeAppContext.getString(R.string.privacy_everyone))))
        {
            currPref = hikeAppContext.getString(R.string.privacy_favorites);
        }
        updateBDPrivacy(currPref, true);
    }

    /**
     * This API returns List of contactinfo from bday msisdns stored in shared pref
     * List is alphabetically sorted
     * @return
     */
    public static List<ContactInfo> getSortedBdayContactListFromSharedPref()
    {
        List<ContactInfo> bdayList = new ArrayList<ContactInfo>();
        Set<String> bdayMsisdns = HikeSharedPreferenceUtil.getInstance().getDataSet(HikeConstants.BDAYS_LIST, null);

        if (bdayMsisdns != null)
        {
            List<String> msisdns = new ArrayList<String>(bdayMsisdns);
            bdayList = ContactManager.getInstance().getContact(msisdns, false, true);

            // Sorting alphabetically
            if (bdayList != null && !bdayList.isEmpty())
            {
                Collections.sort(bdayList, new Comparator<ContactInfo>() {
                    @Override

                    public int compare(ContactInfo lhs, ContactInfo rhs) {
                        return lhs.getFirstName().compareTo(rhs.getFirstName());

                    }

                });
            }
        }
        return bdayList;
    }

    /**
     * This API resets bday List and Bday HTTP call time in Shared Pref
     */
    public static void resetBdayHttpCallInfo()
    {
        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.BDAY_HTTP_CALL_TS, 0l);
        HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeConstants.BDAYS_LIST, null);
    }

    /**
     * save Bday List from tip's json packet into shared pref
     * @param jsonObject
     */
    public static void saveBirthdaysFromTip(JSONObject jsonObject)
    {
        JSONArray msisdns = jsonObject.optJSONArray(HikeConstants.MSISDNS);
        Set<String> bdayMsisdns = new HashSet<>();
        if(msisdns == null || msisdns.length() == 0)
        {
            return;
        }

        bdayMsisdns = getMsisdnSetFromJSONArray(msisdns);

        if(bdayMsisdns.size() != 0)
        {
            HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeConstants.BDAYS_LIST, bdayMsisdns);
        }
    }

    /**
     * This API makes HTTP call to fetch Bday list and save in Shared pref
     */
    public static void fetchAndUpdateBdayList()
    {
        if(!Utils.isBDayInNewChatEnabled())
        {
            return;
        }

        final HikeSharedPreferenceUtil sharedPreferenceUtil = HikeSharedPreferenceUtil.getInstance();

        final long ts = sharedPreferenceUtil.getData(HikeConstants.BDAY_HTTP_CALL_TS, 0l);

        if(System.currentTimeMillis() - ts > sharedPreferenceUtil.getData(HikeConstants.BDAY_HTTP_CALL_TIME_GAP, DEFAULT_CACHE_TIME_FOR_BDAY_CALL)) {
            RequestToken requestToken = HttpRequests.fetchBdaysForCCA(new IRequestListener() {

                @Override
                public void onRequestSuccess(Response result)
                {
                    JSONObject response = (JSONObject) result.getBody().getContent();
                    Logger.d("bday_HTTP_Sucess", "The result from server is " + response);

                    if(!Utils.isResponseValid(response))
                    {
                        Logger.d("bday_HTTP_Sucess", "as stat fail so returning " + response);
                        return;
                    }

                    Set<String> bdayMsisdnSet = null;
                    try
                    {
                        final JSONArray bdayJSONArray = response.getJSONArray(HikeConstants.BIRTHDAY_DATA);

                        if (bdayJSONArray == null || bdayJSONArray.length() == 0)
                        {
                            Logger.d("bday_HTTP_Sucess", "No list in server responce ");
                        }
                        else
                        {
                            bdayMsisdnSet = getMsisdnSetFromJSONArray(bdayJSONArray);
                        }

                        Logger.d("bday_HTTP_Sucess", "Updating time and list in Sp " + bdayMsisdnSet);
                        sharedPreferenceUtil.saveData(HikeConstants.BDAY_HTTP_CALL_TS, System.currentTimeMillis());
                        HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeConstants.BDAYS_LIST, bdayMsisdnSet);
                    }
                    catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onRequestProgressUpdate(float progress)
                {
                }

                @Override
                public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
                {
                    Date currentDate = new Date(System.currentTimeMillis());
                    Date previousDate = new Date(ts);
                    if (!currentDate.equals(previousDate))
                    {
                        Logger.d("bday_HTTP_FAIL", "As Date is changed and call failed, so emptying the bday list");
                        sharedPreferenceUtil.saveData(HikeConstants.BDAYS_LIST, null);
                    }
                }
            });
            requestToken.execute();
        }
    }

    /**
     * This API returns Set of msisdns from JSONArray
     * @return
     */
    public static Set<String> getMsisdnSetFromJSONArray(JSONArray msisdns)
    {
        Set<String> bdayMsisdnSet = new HashSet<String>();
        for(int i = 0; i < msisdns.length(); i++)
        {
            JSONObject msisdnObj = msisdns.optJSONObject(i);
            if(msisdnObj == null)
            {
                continue;
            }
            String msisdn = msisdnObj.optString(HikeConstants.MSISDN);
            if(!TextUtils.isEmpty(msisdn))
            {
                bdayMsisdnSet.add(msisdn);
            }
        }
        return bdayMsisdnSet;
    }

    /**
     * Iterates List of Contacts and removes any hidden mode contact from list
     * if hidden mode is inactive
     *
     * @param bdayContactList
     */
    public static void removeHiddenMsisdn(List<ContactInfo> bdayContactList)
    {
        boolean isActive = StealthModeManager.getInstance().isActive();
        if(!isActive)
        {
            for(ContactInfo contactInfo : bdayContactList)
            {
                if(StealthModeManager.getInstance().isStealthMsisdn(contactInfo.getMsisdn()))
                {
                    bdayContactList.remove(contactInfo);
                }
            }
        }
    }

}
