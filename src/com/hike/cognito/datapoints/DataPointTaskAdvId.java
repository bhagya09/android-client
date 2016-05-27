package com.hike.cognito.datapoints;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Logger;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by abhijithkrishnappa on 23/05/16.
 */
public class DataPointTaskAdvId extends DataPointTask {
    private static final String TAG = DataPointTaskAdvId.class.getSimpleName();

    public DataPointTaskAdvId(String url, boolean isPii, int transportType) {
        super(url, isPii, transportType);
    }

    @Override
    JSONArray recordData() {
        try {
            AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(HikeMessengerApp.getInstance().getApplicationContext());
            return new JSONArray().put(new JSONObject().putOpt(HikeConstants.ADVERTSING_ID_ANALYTICS, adInfo.getId()));
        } catch (JSONException jse) {
            Logger.d(TAG, "IOException" + jse.toString());
        } catch (IOException e) {
            Logger.d(TAG, "IOException" + e.toString());
        } catch (GooglePlayServicesRepairableException e) {
            Logger.d(TAG, "play service repairable exception" + e.toString());
        } catch (GooglePlayServicesNotAvailableException e) {
            Logger.d(TAG, "play services not found Exception" + e.toString());
        }
        return null;
    }
}
