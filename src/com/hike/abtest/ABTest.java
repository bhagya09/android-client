package com.hike.abtest;

import java.lang.annotation.Documented;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.bsb.hike.analytics.AnalyticsConstants;
import com.hike.abtest.dataPersist.DataPersist;
import com.hike.abtest.dataparser.DataParser;
import com.hike.abtest.model.Experiment;

/**
 * ABTest interface for application
 * Created by abhijithkrishnappa on 22/03/16.
 */
public class ABTest {
    private static final String TAG = ABTest.class.getSimpleName();

    private static ABTest instance = null;
    private static Context mContext = null;
    private static AtomicBoolean isInitialized = new AtomicBoolean(false);
    private DataManager mDataManager = null;
    private DataPersist mDataPersist = null;
    //TODO mark these dirty in case we need to apply it again
    //private static AtomicBoolean areExperimentsDirty = new AtomicBoolean(false);
    //private static AtomicBoolean areRolloutsDirty = new AtomicBoolean(false);

    private ABTest() {
        mDataPersist = DataPersist.getInstance(mContext);
        mDataManager = new DataManager(mDataPersist);
    }

    private static ABTest getInstance() {
        if(instance == null) {
            synchronized (ABTest.class) {
                instance = new ABTest();
            }
        }
        return instance;
    }

    private void loadExperiments() {
        mDataManager.loadExperiments();
    }

    private DataManager getDataManager() {
        return mDataManager;
    }

    private DataPersist getDataPersist() {
        return mDataPersist;
    }
    /**
     * Applies/Loads all the stored ABExperiments if available.
     * Called as and when the application starts up
     *
     * @param context Application context.
     *
     */
    public static void apply(Context context) {
        if(isInitialized.get()) {
            Logger.d(TAG, "Already initialized!!!");
            return;
        }

        mContext = context.getApplicationContext();
        Logger.d(TAG, "Initializing now..");
        getInstance().loadExperiments();
        isInitialized.getAndSet(true);
    }

    /**
     * Retrieve a long value from the ABExperiments if any.
     *
     * @param varKey The name of the variable to retrieve.
     * @param defaultValue Value to return if this variable does not exist OR experiment is not applicable.
     *
     * @return Returns the variable value if the corresponding experiment is applicable, or defValue.
     * Throws ClassCastException, if the variable type mismatches
     */
    public static int getInt(String varKey, int defaultValue) {
        int result = defaultValue;
        if(!isInitialized.get()) {
            return result;
        }

        Integer variableValue = getInstance().getDataManager().getVariable(varKey, Integer.class);
        if(variableValue != null) {
            result = variableValue.intValue();
        }

        return result;
    }

    /**
     * Retrieve a boolean value from the ABExperiments if any.
     *
     * @param varKey The name of the variable to retrieve.
     * @param defaultValue Value to return if this variable does not exist OR experiment is not applicable.
     *
     * @return Returns the variable value if the corresponding experiment is applicable, or defValue.
     * Throws ClassCastException, if the variable type mismatches
     */
    public static boolean getBoolean(String varKey, boolean defaultValue) {
        boolean result = defaultValue;
        if(!isInitialized.get()) {
            return result;
        }

        Boolean variableValue = getInstance().getDataManager().getVariable(varKey, Boolean.class);
        if(variableValue != null) {
            result = variableValue.booleanValue();
        }

        return result;
    }

    /**
     * Retrieve a long value from the ABExperiments if any.
     *
     * @param varKey The name of the variable to retrieve.
     * @param defaultValue Value to return if this variable does not exist OR experiment is not applicable.
     *
     * @return Returns the variable value if the corresponding experiment is applicable, or defValue.
     * Throws ClassCastException, if the variable type mismatches
     */
    public static long getLong(String varKey, long defaultValue) {
        long result = defaultValue;
        if(!isInitialized.get()) {
            return result;
        }

        Long variableValue = getInstance().getDataManager().getVariable(varKey, Long.class);
        if(variableValue!=null) {
            result = variableValue.longValue();
        }

        return result;
    }

    /**
     * Retrieve a String value from the ABExperiments if any.
     *
     * @param varKey The name of the variable to retrieve.
     * @param defaultValue Value to return if this variable does not exist OR experiment is not applicable.
     *
     * @return Returns the variable value if the corresponding experiment is applicable, or defValue.
     * Throws ClassCastException, if the variable type mismatches
     */
    public static String getString(String varKey, String defaultValue) {
        String result = defaultValue;
        if(!isInitialized.get()) {
            return result;
        }

        String variableValue = getInstance().getDataManager().getVariable(varKey, String.class);
        if(variableValue!=null) {
            result = variableValue;
        }

        return result;
    }

    /**
     * Retrieve a ABExperiments associated with the variable, which can be logged as analytics
     *
     * @param varKey The name of the variable to retrieve.
     *
     * @return Returns experiment details for the given variable if applicable, or null.
     */
    public static synchronized JSONObject getLogDetails(String varKey) {
        if(!isInitialized.get()) {
            return null;
        }
        JSONObject experimentDetails = null;

        Experiment experiment = getInstance().getDataManager().getExperiment(varKey);

        if(experiment != null) {
            try {
                //Logging only when experiment is running
                if (experiment.getExperimentState() == Experiment.EXPERIMENT_STATE_RUNNING) {
                    experimentDetails = new JSONObject();
                    experimentDetails.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.ACT_ABTEST_LOGS);
                    experimentDetails.put(AnalyticsConstants.V2.PHYLUM, experiment.getExperimentId());
                    experimentDetails.put(AnalyticsConstants.V2.CLASS, experiment.getVariantId());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return experimentDetails;
    }

    /**
     * Retrieve a ABExperiments associated with the variable, which can be logged as analytics
     *  @param requestType type of the request received
     *  @param requestPayload payload of the request received
     *
     *  @return returns true if the message is handled, false otherwise
     */
    public static boolean onRequestReceived(String requestType, JSONObject requestPayload) {
        boolean result = false;
        if(mContext == null) {
            Logger.d(TAG, "onRequestReceived, ABTesting not started, do nothing..");
            return result;
        }

        if(DataParser.isABTestMessage(requestType)) {
            Logger.d(TAG,"requestType: " + requestType);
            Logger.d(TAG,"requestPayload: " + requestPayload.toString());
            try {
                requestPayload = requestPayload.getJSONObject("md");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Logger.d(TAG, "AB Request Payload: " + requestPayload);
            UpdateExperimentService.onRequestReceived(mContext, requestType, requestPayload.toString(),
                    getInstance().getDataPersist());
            result = true;
        }

        return result;
    }
}
