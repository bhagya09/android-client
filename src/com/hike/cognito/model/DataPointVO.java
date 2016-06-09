package com.hike.cognito.model;

import com.hike.cognito.datapoints.DataPointTask;

/**
 * Created by abhijithkrishnappa on 08/06/16.
 */
public class DataPointVO {
    private String mId = "";
    private String mDataToCollect = "";
    private Class mDataPointTaskClass = null;
    private boolean mIsPii = false;
    private boolean mIsRealTime = false;

    public DataPointVO(String id, boolean isPii, boolean isRealTime, Class<? extends DataPointTask> dataPointTaskClass) {
        mId = id;
        mIsPii = isPii;
        mIsRealTime = isRealTime;
        mDataPointTaskClass = dataPointTaskClass;
    }

    public String getId() {
        return mId;
    }

    public String getDataToCollect() {
        return mDataToCollect;
    }

    public boolean isPii() {
        return mIsPii;
    }

    public boolean isRealTime() {
        return mIsRealTime;
    }

    public Class<? extends DataPointTask> getDataPointTaskClass() {
        return mDataPointTaskClass;
    }

}