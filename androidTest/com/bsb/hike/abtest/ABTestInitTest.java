package com.bsb.hike.abtest;

import android.os.SystemClock;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.hike.abtest.ABTest;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by anshumanraypritam on 07/06/16.
 */


@RunWith(AndroidJUnit4.class)
public class ABTestInitTest {

    private static final String TAG = ABTestInitTest.class.getSimpleName();

    @Before
    public void init() {
        Log.d(TAG, "Initializing ABTEST before every test case in init");
        ABTest.clearExperiments();
        ABTest.refreshExperiments();

    }

    @Test
    public void testABExpInit1() throws Throwable {
        initTest();
    }

    /**
     * This test case for the initializing the experiment
     *
     *  Experiment ID: SAMPLE-EXPERIMENTID-1
     *  Start time: 26 April 2016 at 9:34:50 AM
     *  End Time: 20 June 2016 at 5:31:54 AM
     */
    private void initTest() {
        Log.d(TAG, "Init initial Default Value: " + ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
        Assert.assertEquals(ABTestJunitConstants.DEFAULT_VALUE, ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));

        String experimentInit = "{ \"t\": \"AB-Exp-Init\", \"d\": { \"md\": { \"ver\": 1, \"expList\": [ { \"expId\":\"SAMPLE-EXPERIMENTID-1\", \"expType\": 0, \"desc\": \"AB Demo\", \"variantId\" : \"SAMPLE-VariantID-1\", \"sTime\": 1461643490000, \"eTime\": 1466380914000, \"varList\": [ { \"varName\":\"ABTEST-SAMPLE-01\", \"type\" : 2, \"defValue\": \"1\", \"expValue\": \"2\" } ], \"cbUrl\" : \"http://hike.co.in/...\" } ] } } }";
        parse_StartService(ABTestJunitConstants.REQUEST_TYPE_EXPERIMENT_INIT, experimentInit);


        Log.d(TAG, "Init Experimental Value: " + ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
        Assert.assertEquals(ABTestJunitConstants.EXPERIMENT_VALUE, ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
    }

    @Test
    public void testABExpInit2() throws Throwable {
        initMultipleValues();
    }

    /**
     * This test case for the init with multiple experiments values
     *  Experiment ID: SAMPLE-EXPERIMENTID-1
     *  Start time: 6 June 2016 6 June 2016
     *  End Time: 6 June 2017 at 11:01:54
     */
    private void initMultipleValues() {
        Log.d(TAG, "Init  multiple experiment Default Value: " + ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
        Assert.assertEquals(ABTestJunitConstants.DEFAULT_VALUE, ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));

        String initwithMultipleValues = "{ \"t\": \"AB-Exp-Init\", \"d\": { \"md\": { \"ver\": 1, \"expList\": [ { \"expId\":\"SAMPLE-EXPERIMENTID-1\", \"expType\": 0, \"desc\": \"AB Demo\", \"variantId\" : \"SAMPLE-VariantID-1\", \"sTime\": 1465191114000, \"eTime\": 1496727114000, \"varList\": [ { \"varName\":\"ABTEST-SAMPLE-01\", \"type\" : 2, \"defValue\": \"1\", \"expValue\": \"2\" }, { \"varName\":\"ABTEST-SAMPLE-02\", \"type\" : 1, \"defValue\": \"true\", \"expValue\": \"false\" }, { \"varName\":\"ABTEST-SAMPLE-03\", \"type\" : 3, \"defValue\": \"888888888\", \"expValue\": \"999999999\" }, { \"varName\":\"ABTEST-SAMPLE-04\", \"type\" : 4, \"defValue\": \"Hike\", \"expValue\": \"Hike Ltd\" } ], \"cbUrl\" : \"http://hike.co.in/...\" } ] } } }";
        parse_StartService(ABTestJunitConstants.REQUEST_TYPE_EXPERIMENT_INIT, initwithMultipleValues);

        Log.d(TAG, "Init multiple experiment Experimental Value: " + ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
        Assert.assertEquals(ABTestJunitConstants.EXPERIMENT_VALUE, ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
    }

    @Test
    public void testABExpInit3() throws Throwable {
        initTimeExpired();
    }

    /**
     * This method is for the experiments  which has passed  the expired time
     *  Experiment ID: SAMPLE-EXPERIMENTID-1
     *  Start time: 5 June 2016 at 11:40:40 AM
     *  End Time:  6 June 2016 at 11:40:40 AM
     */
    private void initTimeExpired() {
        Log.d(TAG, "Init ExpiredTime Default Value: " + ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
        Assert.assertEquals(ABTestJunitConstants.DEFAULT_VALUE, ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));

        String experimentInit = "{ \"t\": \"AB-Exp-Init\", \"d\": { \"md\": { \"ver\": 1, \"expList\": [ { \"expId\":\"SAMPLE-EXPERIMENTID-1\", \"expType\": 0, \"desc\": \"AB Demo\", \"variantId\" : \"SAMPLE-VariantID-1\", \"sTime\": 1465107040000, \"eTime\": 1465193440000, \"varList\": [ { \"varName\":\"ABTEST-SAMPLE-01\", \"type\" : 2, \"defValue\": \"1\", \"expValue\": \"2\" } ], \"cbUrl\" : \"http://hike.co.in/...\" } ] } } }";
        parse_StartService(ABTestJunitConstants.REQUEST_TYPE_EXPERIMENT_INIT, experimentInit);

        Log.d(TAG, "Init ExpiredTime Experimental Default Value: " + ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
        Assert.assertEquals(ABTestJunitConstants.EXPERIMENT_DEFAULT_VALUE, ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
    }


    /**
     * This method has to be take the request for the json format and start the intent service
     *
     * @param request
     * @param experiment
     */

    private void parse_StartService(String request, String experiment) {
        JSONObject experimentInitJson = null;

        try {
            experimentInitJson = new JSONObject(experiment);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ABTest.onRequestReceived(ABTestJunitConstants.REQUEST_TYPE_EXPERIMENT_INIT, experimentInitJson);
         /* Sleep for 10sec */
        SystemClock.sleep(ABTestJunitConstants.WAIT_TIME_FOR_REQUEST);

        ABTest.refreshExperiments();

    }


    @After
    public void clearExp() {
        Log.d(TAG, "Clearing ABTEST at after every test case");
        ABTest.clearExperiments();
    }
}

