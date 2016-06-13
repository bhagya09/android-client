package com.hike.abtest;

import com.bsb.hike.MqttConstants;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.service.HikeMqttManagerNew;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by abhijithkrishnappa on 04/05/16.
 */
public class AnalyticsUtil {
    public static final String ABTEST_REQUEST_STATUS = "AB_req_status";
    private static final String TAG = AnalyticsUtil.class.getSimpleName();

    public static JSONObject getExperimentAnalyticsJson(String experimentId, String variantId) {
        JSONObject experimentDetails = null;
        try {
            experimentDetails = new JSONObject();
            experimentDetails.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.ACT_ABTEST_LOGS);
            experimentDetails.put(AnalyticsConstants.V2.PHYLUM, experimentId);
            experimentDetails.put(AnalyticsConstants.V2.CLASS, variantId);
        } catch (JSONException je) {
            je.printStackTrace();
        }

        return experimentDetails;
    }

    public static void sendRequestStatusAnalyticsJson(String request, String status, String experimentId) {
        JSONObject requestStatus = null;
        try {
            requestStatus = new JSONObject();
            requestStatus.put(AnalyticsConstants.V2.UNIQUE_KEY, ABTEST_REQUEST_STATUS);
            requestStatus.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.ACT_ABTEST_LOGS);
            requestStatus.put(AnalyticsConstants.V2.PHYLUM, experimentId);
            requestStatus.put(AnalyticsConstants.V2.CLASS, request);
            requestStatus.put(AnalyticsConstants.V2.ORDER, status);
        } catch (JSONException je) {
            je.printStackTrace();
        }

        if (requestStatus != null) {
            Logger.d(TAG, "Request status:" + requestStatus);
            HAManager.getInstance().recordV2(requestStatus);
        }
    }
}
