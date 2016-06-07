package com.hike.abtest.dataPersist;

import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;

import com.hike.abtest.Logger;
import com.hike.abtest.dataparser.ProtoMapper;

/**
 * SharedPreference implementation of persistence interface
 * Created by abhijithkrishnappa on 11/04/16.
 */
public class DataPersistPrefImpl extends DataPersist {
    private static final String EXPERIMENTS_STORE = "abtest_experiments_store";
    private static final String TAG = DataPersistPrefImpl.class.getSimpleName();
    SharedPreferences mExperimentsPref;

    DataPersistPrefImpl(Context context) {
        super(context);
        mExperimentsPref = mContext.getSharedPreferences(EXPERIMENTS_STORE, Context.MODE_PRIVATE);
    }

    public void persistNewExperiment(Map<String, String> experimentMap) {
        SharedPreferences.Editor mExpEditor = mExperimentsPref.edit();
        for (Map.Entry<String, String> entry : experimentMap.entrySet()) {
            Logger.d(TAG, entry.getKey() + "/" + entry.getValue());
            if (!mExperimentsPref.contains(entry.getKey())) {
                mExpEditor.putString(entry.getKey(), entry.getValue());
            }
        }
        mExpEditor.apply();
    }

    public void persistRollOuts(Map<String, String> experimentMap) {
        Logger.d(TAG, "persistRollOuts");
        SharedPreferences.Editor mExpEditor = mExperimentsPref.edit();

        for (Map.Entry<String, String> entry : experimentMap.entrySet()) {
            Logger.d(TAG, entry.getKey() + "/" + entry.getValue());
            mExpEditor.putString(entry.getKey(), entry.getValue());
        }
        mExpEditor.apply();
    }

    public void abortExperiment(List<String> experimentIds) {
        SharedPreferences.Editor mExpEditor = mExperimentsPref.edit();
        for (String experimentId : experimentIds) {
            if (mExperimentsPref.contains(experimentId)) {
                mExpEditor.remove(experimentId);
                Logger.d(TAG, "Removed experiment: " + experimentId);
            }
        }
        mExpEditor.apply();
    }

    public Map<String, ?> getAllExperiments() {
        Map<String, ?> allExperiments = mExperimentsPref.getAll();
        return allExperiments;
    }

    @Override
    public void clearAll() {
        mExperimentsPref.edit().clear();
        mExperimentsPref.edit().apply();
    }
}
