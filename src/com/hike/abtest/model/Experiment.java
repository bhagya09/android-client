package com.hike.abtest.model;

/**
 * Experiment domain Model
 * Created by abhijithkrishnappa on 29/03/16.
 */
public class Experiment {
    private String mExperimentId = null;
    private String mVariantId = null;
    private String mDescription = "No Description";
    private long mStartTime = 0l;
    private long mEndTime = 0l;
    private String mCallbackUrl =  null;
    private boolean mIsRolledOut = false;


    public static final int EXPERIMENT_STATE_PENDING = 0;
    public static final int EXPERIMENT_STATE_RUNNING = 1;
    public static final int EXPERIMENT_STATE_STOPPED = 2;
    public static final int EXPERIMENT_STATE_ROLLED_OUT = 3;
    public static final int EXPERIMENT_STATE_ABORTED = 100;//Not required as of now...

    public Experiment(String experimentId, String variantId, String description,
                      long startTime, long endTime, boolean rollout, String callbackUrl) {
        mExperimentId = experimentId;
        mVariantId = variantId;
        mDescription = description;
        mStartTime = startTime;
        mEndTime = endTime;
        mIsRolledOut = rollout;
        mCallbackUrl =  callbackUrl;
    }

    //TODO: Write Builder

    public boolean isRolledOut() {
        return mIsRolledOut;
    }

    public int getExperimentState() {
        if(mIsRolledOut) {
            return EXPERIMENT_STATE_ROLLED_OUT;
        }

        long currentTime = System.currentTimeMillis();

        if(mStartTime > currentTime) {
            return EXPERIMENT_STATE_PENDING;
        } else if(mStartTime < currentTime && mEndTime > currentTime) {
            return EXPERIMENT_STATE_RUNNING;
        } else {
            return EXPERIMENT_STATE_STOPPED;
        }
    }

    public String getExperimentId() {
        return mExperimentId;
    }

    public String getVariantId() {
        return mVariantId;
    }

    public String toString() {
        return "ExperimentId: " + mExperimentId + " VariantId:" + mVariantId +
                " Description:" + mDescription + " StartTime:" + mStartTime +
                " EndTime:" + mEndTime + " isRolledOut:" + mIsRolledOut;
    }
}
