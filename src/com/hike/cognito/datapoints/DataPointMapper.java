package com.hike.cognito.datapoints;

import android.util.SparseArray;

import com.bsb.hike.HikeConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by abhijithkrishnappa on 06/06/16.
 */
public class DataPointMapper {
    public static final String ID_ACCOUNT_INFO = "actl";
    public static final String ID_ADVERTISING_ID = "adv";
    public static final String ID_ALL_APPS = "al";
    public static final String ID_CALL_LOGS = "cl";
    public static final String ID_DEVICE_DETAILS = "dd";
    public static final String ID_PHONE_SPEC = "pl";
    public static final String ID_SESSION_LOG = HikeConstants.SESSION_LOG_TRACKING;
    public static final String ID_STATIC_LOCATION = "ll";

    private static SparseArray<Class<? extends DataPointTask>>mDataPointMapper = new SparseArray<Class<? extends DataPointTask>>();

    static {
        mDataPointMapper.put(ID_ACCOUNT_INFO.hashCode(), DataPointTaskAccInfo.class);
        mDataPointMapper.put(ID_ADVERTISING_ID.hashCode(), DataPointTaskAdvId.class);
        mDataPointMapper.put(ID_ALL_APPS.hashCode(), DataPointTaskAllApps.class);
        mDataPointMapper.put(ID_CALL_LOGS.hashCode(), DataPointTaskCallLogs.class);
        mDataPointMapper.put(ID_DEVICE_DETAILS.hashCode(), DataPointTaskDevDetails.class);
        mDataPointMapper.put(ID_PHONE_SPEC.hashCode(), DataPointTaskPhoneSpec.class);
        mDataPointMapper.put(ID_SESSION_LOG.hashCode(), DataPointTaskSessionLog.class);
        mDataPointMapper.put(ID_STATIC_LOCATION.hashCode(), DataPointTaskStaticLoc.class);
    }

    public static Class getClassForDataPoint(String datapoint) {
        return mDataPointMapper.get(datapoint.hashCode());
    }
}
