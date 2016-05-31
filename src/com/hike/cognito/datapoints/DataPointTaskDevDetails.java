package com.hike.cognito.datapoints;

import android.content.Context;

import com.bsb.hike.GCMIntentService;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.google.android.gcm.GCMRegistrar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by abhijithkrishnappa on 23/05/16.
 */
public class DataPointTaskDevDetails extends DataPointTask {

    public DataPointTaskDevDetails(String url, boolean isPii, int transportType) {
        super(url, isPii, transportType);
    }

    @Override
    JSONArray recordData() {
        Context context = HikeMessengerApp.getInstance().getApplicationContext();
        JSONObject deviceDetails = Utils.getPostDeviceDetails(context);
        try {
            deviceDetails.put(GCMIntentService.DEV_TOKEN, GCMRegistrar.getRegistrationId(context));
            deviceDetails.put(HikeConstants.LogEvent.DEVICE_ID, Utils.getDeviceId(context));
            deviceDetails.put(HikeConstants.LogEvent.DPI, Utils.densityDpi);
            deviceDetails.put(HikeConstants.RESOLUTION_ID, Utils.getResolutionId());
        } catch (JSONException jse) {
            jse.printStackTrace();
        }
        Logger.d("Device Details", deviceDetails.toString());
        return new JSONArray().put(deviceDetails);
    }
}
