package com.bsb.hike.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.notifications.HikeNotification;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
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
        recordBirthdayAnalytics(
                AnalyticsConstants.BirthdayEvents.BIRTHDAY_CHANGE_SETTING,
                AnalyticsConstants.BirthdayEvents.BIRTHDAY_SETTING,
                AnalyticsConstants.BirthdayEvents.BIRTHDAY_SETTING_OPEN,
                null, getCurrentBDPref(), null, bdPrefValue, null, null, null, null);
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
        String defValue = Utils.isFavToFriendsMigrationAllowed() ?
                hikeAppContext.getString(R.string.privacy_favorites) : hikeAppContext.getString(R.string.privacy_my_contacts);
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
                        return lhs.getNameOrMsisdn().compareTo(rhs.getNameOrMsisdn());
                    }

                });
            }
            Logger.d("bday_", " Now Sorted list is  " + bdayList);
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
     * @param fromServerPacket
     * @param packetId
     */
    public static void fetchAndUpdateBdayList(final boolean fromServerPacket, final String packetId)
    {
        if(!Utils.isBDayInNewChatEnabled())
        {
            Logger.d("bday_", "Birthday feature is disabled, so no HHTP call ");
            return;
        }

        final HikeSharedPreferenceUtil sharedPreferenceUtil = HikeSharedPreferenceUtil.getInstance();

        final long ts = sharedPreferenceUtil.getData(HikeConstants.BDAY_HTTP_CALL_TS, 0l);

        if(fromServerPacket || System.currentTimeMillis() - ts > sharedPreferenceUtil.getData(HikeConstants.BDAY_HTTP_CALL_TIME_GAP, DEFAULT_CACHE_TIME_FOR_BDAY_CALL)) {
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

                        if (fromServerPacket)
                        {
                            if (!bdayMsisdnSet.isEmpty())
                            {
                                showBdayNotifcations(bdayMsisdnSet, packetId);
                            }
                            else
                            {
                                recordBirthdayAnalytics(
                                        AnalyticsConstants.BirthdayEvents.BIRTHDAY_REQ_RESPONSE,
                                        AnalyticsConstants.BirthdayEvents.BIRTHDAY_PUSH_NOTIF,
                                        AnalyticsConstants.BirthdayEvents.BIRTHDAY_REQ_RESPONSE,
                                        String.valueOf(packetId), null, String.valueOf(Utils.isBDayInNewChatEnabled()), null, null, null, "0", null);
                            }
                        }
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
            if (fromServerPacket)
            {
                recordBirthdayAnalytics(
                        AnalyticsConstants.BirthdayEvents.BIRTHDAY_HTTP_REQ,
                        AnalyticsConstants.BirthdayEvents.BIRTHDAY_PUSH_NOTIF,
                        AnalyticsConstants.BirthdayEvents.BIRTHDAY_HTTP_REQ,
                        String.valueOf(packetId), null, null, null, null, null, null, null);
            }
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
    public static void removeHiddenMsisdnFromContactInfoList(List<ContactInfo> bdayContactList)
    {
        boolean isActive = StealthModeManager.getInstance().isActive();
        if(!isActive)
        {
            Iterator<ContactInfo> iterator = bdayContactList.iterator();
            while(iterator.hasNext())
            {
                ContactInfo contactInfo = iterator.next();
                if(StealthModeManager.getInstance().isStealthMsisdn(contactInfo.getMsisdn()))
                {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Iterates List of Contacts and removes any hidden mode contact from list
     * if hidden mode is inactive
     *
     * @param bdayMsisdnList
     */
    public static void removeHiddenMsisdn(List<String> bdayMsisdnList)
    {
        boolean isActive = StealthModeManager.getInstance().isActive();
        if(!isActive)
        {
            Iterator<String> iterator = bdayMsisdnList.iterator();
            while(iterator.hasNext())
            {
                String msisdn = iterator.next();
                if(StealthModeManager.getInstance().isStealthMsisdn(msisdn))
                {
                    Logger.d("bday_notif_", "Removing stealth misidn from list " + msisdn);
                    iterator.remove();
                }
            }
        }
    }

    /*
     * Fetched list of bday msisdns from Shared pref
     * Publish Pubsub to show notifications if list is non empty
     */
    public static void showBdayNotifcations(Set<String> bdayMsisdnSet, String packetId)
    {
        ArrayList<String> bdayMsisdns = new ArrayList<String>(bdayMsisdnSet);

        removeHiddenMsisdn(bdayMsisdns);

        recordBirthdayAnalytics(
                AnalyticsConstants.BirthdayEvents.BIRTHDAY_REQ_RESPONSE,
                AnalyticsConstants.BirthdayEvents.BIRTHDAY_PUSH_NOTIF,
                AnalyticsConstants.BirthdayEvents.BIRTHDAY_REQ_RESPONSE,
                String.valueOf(packetId), null, String.valueOf(Utils.isBDayInNewChatEnabled()), null, null, null, String.valueOf(bdayMsisdnSet.size() - bdayMsisdns.size()), bdayMsisdns.toString());

        if(bdayMsisdns != null && bdayMsisdns.size() > 0)
        {
            Logger.d("bday_notif_", "Going to show notif for " + bdayMsisdns);
            Pair<ArrayList<String>, String> bdayNotifPair = new Pair<ArrayList<String>, String>(bdayMsisdns, packetId);
            HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_BIRTHDAY_NOTIF, bdayNotifPair);
        }
        else
        {
            Logger.d("bday_", "As list is null or empty, so showing no notification " + bdayMsisdns);
        }
    }

	public static void recordBirthdayAnalytics(String uk, String eventClass, String order, String family, String genus, String species, String variety, String form, String race, String valInt, String toUser)
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.ACT_EXPERIMENT);
			json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.BirthdayEvents.BIRTHDAY);
			json.put(AnalyticsConstants.V2.UNIQUE_KEY, uk);
			json.put(AnalyticsConstants.V2.CLASS, eventClass);
			json.put(AnalyticsConstants.V2.ORDER, order);
			json.put(AnalyticsConstants.V2.FAMILY, family);
            json.put(AnalyticsConstants.V2.GENUS, genus);
            json.put(AnalyticsConstants.V2.SPECIES, species);
            json.put(AnalyticsConstants.V2.VARIETY, variety);
            json.put(AnalyticsConstants.V2.FORM, form);
            json.put(AnalyticsConstants.V2.RACE, race);
            json.put(AnalyticsConstants.V2.VAL_INT, valInt);
            json.put(AnalyticsConstants.V2.TO_USER, toUser);
			HAManager.getInstance().recordV2(json);
		}

		catch (JSONException e)
		{
			e.toString();
		}
	}

	public static void cleanUpBirthdayDataAndNotifications()
	{
		resetBdayHttpCallInfo();
		HikeNotification.getInstance().cancelNotification(HikeNotification.BIRTHDAY_NOTIF);
	}
}
