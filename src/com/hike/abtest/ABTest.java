package com.hike.abtest;

import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.bsb.hike.HikeConstants;
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
    private static AtomicBoolean areExperimentsForNewUserAvail = new AtomicBoolean(false);

    private ABTest() {
        mDataPersist = DataPersist.getInstance(mContext);
        mDataManager = new DataManager(mDataPersist);
    }

    private static ABTest getInstance() {
        if (instance == null) {
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
     */
    public static void apply(Context context) {
        if (isInitialized.get()) {
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
     * @param varKey       The name of the variable to retrieve.
     * @param defaultValue Value to return if this variable does not exist OR experiment is not applicable.
     * @return Returns the variable value if the corresponding experiment is applicable, or defValue.
     * Throws ClassCastException, if the variable type mismatches
     */
    public static int getInt(String varKey, int defaultValue) {
        int result = defaultValue;
        if (!isInitialized.get()) {
            return result;
        }

        Integer variableValue = getInstance().getDataManager().getVariable(varKey, Integer.class);
        if (variableValue != null) {
            result = variableValue.intValue();
        }

        return result;
    }

    /**
     * Retrieve a boolean value from the ABExperiments if any.
     *
     * @param varKey       The name of the variable to retrieve.
     * @param defaultValue Value to return if this variable does not exist OR experiment is not applicable.
     * @return Returns the variable value if the corresponding experiment is applicable, or defValue.
     * Throws ClassCastException, if the variable type mismatches
     */
    public static boolean getBoolean(String varKey, boolean defaultValue) {
        boolean result = defaultValue;
        if (!isInitialized.get()) {
            return result;
        }

        Boolean variableValue = getInstance().getDataManager().getVariable(varKey, Boolean.class);
        if (variableValue != null) {
            result = variableValue.booleanValue();
        }

        return result;
    }

    /**
     * Retrieve a long value from the ABExperiments if any.
     *
     * @param varKey       The name of the variable to retrieve.
     * @param defaultValue Value to return if this variable does not exist OR experiment is not applicable.
     * @return Returns the variable value if the corresponding experiment is applicable, or defValue.
     * Throws ClassCastException, if the variable type mismatches
     */
    public static long getLong(String varKey, long defaultValue) {
        long result = defaultValue;
        if (!isInitialized.get()) {
            return result;
        }

        Long variableValue = getInstance().getDataManager().getVariable(varKey, Long.class);
        if (variableValue != null) {
            result = variableValue.longValue();
        }

        return result;
    }

    /**
     * Retrieve a String value from the ABExperiments if any.
     *
     * @param varKey       The name of the variable to retrieve.
     * @param defaultValue Value to return if this variable does not exist OR experiment is not applicable.
     * @return Returns the variable value if the corresponding experiment is applicable, or defValue.
     * Throws ClassCastException, if the variable type mismatches
     */
    public static String getString(String varKey, String defaultValue) {
        String result = defaultValue;
        if (!isInitialized.get()) {
            return result;
        }

        String variableValue = getInstance().getDataManager().getVariable(varKey, String.class);
        if (variableValue != null) {
            result = variableValue;
        }

        return result;
    }

    /**
     * Retrieve a ABExperiments associated with the variable, which can be logged as analytics
     *
     * @param varKey The name of the variable to retrieve.
     * @return Returns experiment details for the given variable if applicable, or null.
     */
    public static synchronized JSONObject getLogDetails(String varKey) {
        if (!isInitialized.get()) {
            return null;
        }
        JSONObject experimentDetails = null;

        Experiment experiment = getInstance().getDataManager().getExperiment(varKey);

        if (experiment != null) {
            //Logging only when experiment is running
            if (experiment.getExperimentState() == Experiment.EXPERIMENT_STATE_RUNNING) {
                experimentDetails = AnalyticsUtil.getExperimentAnalyticsJson(experiment.getExperimentId(),
                        experiment.getVariantId());
            }
        }

        return experimentDetails;
    }

    /**
     * Retrieve a ABExperiments associated with the variable, which can be logged as analytics
     *
     * @param requestType    type of the request received
     * @param requestPayload payload of the request received
     * @return returns true if the message is handled, false otherwise
     */
    public static boolean onRequestReceived(String requestType, JSONObject requestPayload) {
        boolean result = false;
        if (mContext == null) {
            Logger.d(TAG, "onRequestReceived, ABTesting not started, do nothing..");
            return result;
        }

        if (DataParser.isABTestMessage(requestType)) {
            Logger.d(TAG, "requestType: " + requestType);
            Logger.d(TAG, "requestPayload: " + requestPayload.toString());
            try {
                if (requestPayload.has(HikeConstants.DATA) &&
                        requestPayload.getJSONObject(HikeConstants.DATA).has(HikeConstants.METADATA)) {
                    requestPayload = requestPayload.getJSONObject(HikeConstants.DATA)
                            .getJSONObject(HikeConstants.METADATA);
                    Logger.d(TAG, "AB Request Payload: " + requestPayload);
                    UpdateExperimentService.onRequestReceived(mContext, requestType, requestPayload.toString(),
                            getInstance().getDataPersist());
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Logger.e(TAG, "Error Parsing AB Request packet!!!");
            }
            result = true;
        }

        return result;
    }

    /**
     * Retrieve ABExperiments associated with the new user on sign up complete
     * Called only once after signup complete
     */
    public static void fetchNewUserExperiments() {
        Logger.d(TAG, "Fetching new user experiments if any!");
        new NewUserExperimentFetchTask().execute();
    }

    /**
     * Apply new user ABExperiments, if available
     */
    public static void applyNewUserExperiments() {
        Logger.d(TAG, "Checking for new User experiments...");
        if (areExperimentsForNewUserAvail.get()) {
            Logger.d(TAG, "New User experiments available, Applying...");
            getInstance().loadExperiments();
        }
    }

    /*package*/
    static void setNewExperimentsAvailable() {
        areExperimentsForNewUserAvail.getAndSet(true);
    }

}
