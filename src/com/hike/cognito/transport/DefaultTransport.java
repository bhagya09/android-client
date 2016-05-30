package com.hike.cognito.transport;

import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.hike.abtest.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by abhijithkrishnappa on 18/05/16.
 */
public class DefaultTransport extends Transport {
    public static final String ANALYTICS_VERSION = "ver";
    public static final String ANALYTICS_CTS = "cts";
    private static final String TAG = DefaultTransport.class.getSimpleName();

    DefaultTransport() {
        super(null, false);
    }

    DefaultTransport(boolean isPii) {
        super(null, isPii);
    }

    public void sendData(String data) {
        if (isPII()) {
            Logger.d(TAG, "UserCognito, logging: Data NOT secure, Continuing!!!");
        }
        //TODO currently JSON format supported.
        JSONObject datatoLog = null;
        try {
            datatoLog = new JSONObject(data);
            datatoLog.put(ANALYTICS_VERSION, AnalyticsConstants.V2.VERSION_VALUE);
            datatoLog.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.ACT_USER_COGNITO_LOGS);
            datatoLog.put(AnalyticsConstants.V2.PHYLUM, data);
            datatoLog.put(ANALYTICS_CTS, System.currentTimeMillis());
        } catch (JSONException je) {
            je.printStackTrace();
        }

        if (datatoLog != null) {
            Logger.d(TAG, "UserCognito, logging:" + datatoLog);
            HAManager.getInstance().recordV2(datatoLog);
        }
    }

    @Override
    public void sendJsonData(JSONObject data) {

    }

    @Override
    public void sendJsonArrayData(JSONArray data) {

    }
}
