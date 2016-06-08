package com.hike.abtest.dataPersist;

import java.util.List;
import java.util.Map;

import android.content.Context;

/**
 * Persistence interface for Experiments.
 * Created by abhijithkrishnappa on 03/04/16.
 */
public abstract class DataPersist {
    private static final String TAG = DataPersist.class.getSimpleName();
    protected Context mContext = null;


    public DataPersist(Context context) {
        mContext = context;
    }

    public static DataPersist getInstance(Context context) {
        return new DataPersistPrefImpl(context);
    }

    public abstract void persistNewExperiment(Map<String, String> experimentMap);

    public abstract void persistRollOuts(Map<String, String> experimentMap);

    public abstract void abortExperiment(List<String> experimentIds);

    public abstract Map<String, ?> getAllExperiments();

    public abstract void clearAll();
}
