package com.bsb.hike.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author paramshah
 */

public class BirthdayUtils
{
    public static final String TAG = "BirthdayUtils";

    private static final String INVALID_PREF_VALUE = "-1";

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
}
