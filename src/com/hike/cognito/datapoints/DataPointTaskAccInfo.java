package com.hike.cognito.datapoints;

import android.accounts.Account;
import android.accounts.AccountManager;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by abhijithkrishnappa on 23/05/16.
 */
public class DataPointTaskAccInfo extends DataPointTask {
    private static final String TAG = DataPointTaskAccInfo.class.getSimpleName();
    private static final String ACCOUNT_TYPE = "act";
    private static final String ACCOUNT_NAME = "acn";

    public DataPointTaskAccInfo(String url, Boolean isPii, Integer transportType) {
        super(url, isPii, transportType);
    }

    @Override
    JSONArray recordData() {
        JSONArray accountInfoJsonArray = new JSONArray();
        Account[] accountList = AccountManager.get(HikeMessengerApp.getInstance().getApplicationContext()).getAccounts();
        if (accountList != null && accountList.length > 0) {
            for (Account account : accountList) {
                Logger.d(TAG, account.name + " : " + account.type + " : " + account.toString());
                try {
                    accountInfoJsonArray.put(toJSON(account.name, account.type));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        if (accountInfoJsonArray == null || accountInfoJsonArray.length() == 0) {
            return null;
        }
        return accountInfoJsonArray;
    }

    public JSONObject toJSON(String accountName, String accountType) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        jsonObj.putOpt(ACCOUNT_NAME, accountName);
        jsonObj.putOpt(ACCOUNT_TYPE, accountType);
        return jsonObj;
    }
}
