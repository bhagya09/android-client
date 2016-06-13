package com.hike.cognito.datapoints;

import android.location.Location;

import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by abhijithkrishnappa on 23/05/16.
 */
public class DataPointTaskStaticLoc extends DataPointTask {
    private static final String TAG = DataPointTaskStaticLoc.class.getSimpleName();

    private static final String LATITUDE = "lat";
    private static final String LONGITUDE = "long";
    private static final String RADIUS = "rd";
    private static final String TIMESTAMP = "ts";

    public DataPointTaskStaticLoc(String url, Boolean isPii, Integer transportType) {
        super(url, isPii, transportType);
    }

    public static class LocLogPojo {
        final double latitude;
        final double longitude;
        final float radius;
        final long timeStamp;

        public LocLogPojo(double latitude, double longitude, float radius, long timeStamp) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.radius = radius;
            this.timeStamp = timeStamp;
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject jsonObj = new JSONObject();
            jsonObj.putOpt(LATITUDE, this.latitude);
            jsonObj.putOpt(LONGITUDE, this.longitude);
            jsonObj.putOpt(RADIUS, this.radius);
            jsonObj.putOpt(TIMESTAMP, this.timeStamp);
            return jsonObj;
        }
    }

    @Override
    JSONArray recordData() {
        Location bestLocation = Utils.getPassiveLocation();
        if (bestLocation == null || (bestLocation.getLongitude() == 0.0D && bestLocation.getLatitude() == 0.0D))
            return null;
        LocLogPojo locLog = new LocLogPojo(bestLocation.getLatitude(), bestLocation.getLongitude(),
                bestLocation.getAccuracy(), bestLocation.getTime());
        List<LocLogPojo> locLogList = new ArrayList<LocLogPojo>(1);
        locLogList.add(locLog);

        if (locLogList == null)
            return null;

        JSONArray locJsonArray = new JSONArray();
        try {
            for (LocLogPojo locationLog : locLogList) {
                locJsonArray.put(locationLog.toJSON());
            }
        } catch (JSONException jse) {
            jse.printStackTrace();
        }
        Logger.d(TAG, locJsonArray.toString());
        return locJsonArray;
    }
}
