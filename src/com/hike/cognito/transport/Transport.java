package com.hike.cognito.transport;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by abhijithkrishnappa on 18/05/16.
 */
public abstract class Transport {
    public static final int TRANSPORT_TYPE_DEFAULT = 0;
    public static final int TRANSPORT_TYPE_REALTIME = 1;

    protected String mUrl = null;
    protected boolean mIsPII = false;

    protected Transport(String url, boolean isPII) {
        mUrl = url;
        mIsPII = isPII;
    }

    public static Transport getDefaultTransport(boolean isPII) {
        return new DefaultTransport(isPII);
    }

    public static Transport getTransport(boolean isPII, int transportType) {
        return getTransport(null, isPII, transportType);
    }

    public static Transport getTransport(String url, boolean isPII, int transportType) {
        Transport transport = null;
        switch (transportType) {
            case TRANSPORT_TYPE_DEFAULT:
                transport = getDefaultTransport(isPII);
                break;
            case TRANSPORT_TYPE_REALTIME:
                transport = new RealTimeTransport(url, isPII);
                break;
            default:
                break;
        }

        return transport;
    }

    protected String getUrl() {
        return mUrl;
    }

    protected void setUrl(String mUrl) {
        this.mUrl = mUrl;
    }

    protected boolean isPII() {
        return mIsPII;
    }

    protected void setPII(boolean mIsPII) {
        this.mIsPII = mIsPII;
    }

    public abstract void sendJsonData(JSONObject data);

    public abstract void sendJsonArrayData(JSONArray data);

}
