package com.hike.cognito.datapoints;


import com.bsb.hike.utils.Logger;
import com.hike.cognito.transport.Transport;

import org.json.JSONArray;

/**
 * Created by abhijithkrishnappa on 18/05/16.
 */
public abstract class DataPointTask implements Runnable {

    private static final String TAG = DataPointTask.class.getSimpleName();
    String mUrl = null;
    boolean mIsPii = false;
    int mTransportType = Transport.TRANSPORT_TYPE_DEFAULT;

    protected DataPointTask(String url, boolean isPii, int transportType) {
        mUrl = url;
        mIsPii = isPii;
        mTransportType = transportType;
    }

    @Override
    public void run() {
        JSONArray data = recordData();
        if (data == null || data.length() == 0) return;
        Logger.d(TAG, "data: " + data);
        Transport transport = Transport.getTransport(mUrl, mIsPii, mTransportType);
        transport.sendJsonArrayData(data);
    }

    abstract JSONArray recordData();
}
