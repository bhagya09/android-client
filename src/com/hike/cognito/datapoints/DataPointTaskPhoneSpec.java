package com.hike.cognito.datapoints;

import com.bsb.hike.utils.PhoneSpecUtils;

import org.json.JSONArray;

/**
 * Created by abhijithkrishnappa on 23/05/16.
 */
public class DataPointTaskPhoneSpec extends DataPointTask {

    public DataPointTaskPhoneSpec(String url, Boolean isPii, Integer transportType) {
        super(url, isPii, transportType);
    }

    @Override
    JSONArray recordData() {
        return PhoneSpecUtils.getPhoneSpec();
    }
}
