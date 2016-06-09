package com.bsb.hike.abtest;

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
public class ABTestAbortTest {

    private static final String TAG = ABTestAbortTest.class.getSimpleName();

    @Before
    public void init() {
        Log.d(TAG, "Initializing ABTEST in abort");
        ABTest.clearExperiments();
        ABTest.refreshExperiments();

    }

    @Test
    public void testABExpAbort1() throws Throwable {

        Log.d(TAG, " Abort Default Value: " + ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
        Assert.assertEquals(ABTestJunitConstants.DEFAULT_VALUE, ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));

        String experimentAbort = "{ \"t\": \"AB-Exp-Abort\", \"d\" : { \"md\": { \"ver\": 1, \"expList\": [ { \"expId\": \"SAMPLE-EXPERIMENTID-1\", \"variantId\" : \"SAMPLE-VariantID-1\", \"cbUrl\" : \"http://hike.co.in/...\" } ] } } }";
        parse_StartService(ABTestJunitConstants.REQUEST_TYPE_EXPERIMENT_ABORT, experimentAbort);

        Log.d(TAG, "Abort Experimental Value: " + ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
        Assert.assertEquals(ABTestJunitConstants.DEFAULT_VALUE, ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));


    }

    @Test
    public void testABExpAbort2() throws Throwable {


        Log.d(TAG, " Testing with init abort Default Value  " + ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
        Assert.assertEquals(ABTestJunitConstants.DEFAULT_VALUE, ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));

        String experimentInit = "{ \"t\": \"AB-Exp-Init\", \"d\": { \"md\": { \"ver\": 1, \"expList\": [ { \"expId\":\"SAMPLE-EXPERIMENTID-1\", \"expType\": 0, \"desc\": \"AB Demo\", \"variantId\" : \"SAMPLE-VariantID-1\", \"sTime\": 1461643490000, \"eTime\": 1466380914000, \"varList\": [ { \"varName\":\"ABTEST-SAMPLE-01\", \"type\" : 2, \"defValue\": \"1\", \"expValue\": \"2\" } ], \"cbUrl\" : \"http://hike.co.in/...\" } ] } } }";
        parse_StartService("AB-Exp-Init", experimentInit);


        String experimentAbort = "{ \"t\": \"AB-Exp-Abort\", \"d\": { \"md\": { \"ver\": 1, \"expList\": [ { \"expId\":\"SAMPLE-EXPERIMENTID-1\", \"expType\": 0, \"desc\": \"AB Demo\", \"variantId\" : \"SAMPLE-VariantID-1\", \"sTime\": 1461643490000, \"eTime\": 1466380914000, \"varList\": [ { \"varName\":\"ABTEST-SAMPLE-01\", \"type\" : 2, \"defValue\": \"1\", \"expValue\": \"2\" } ], \"cbUrl\" : \"http://hike.co.in/...\" } ] } } }";
        parse_StartService(ABTestJunitConstants.REQUEST_TYPE_EXPERIMENT_ABORT, experimentAbort);


        Log.d(TAG, "Testing with init abort Experimental Value: " + ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
        Assert.assertEquals(ABTestJunitConstants.DEFAULT_VALUE, ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));


    }

    /**
     * This method has to be take the request for the json format and start the intent service
     *
     * @param request
     * @param experiment
     */

    private void parse_StartService(String request, String experiment) {
        JSONObject experimentAbortJson = null;

        try {
            experimentAbortJson = new JSONObject(experiment);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ABTest.onRequestReceived(request, experimentAbortJson);

        //SystemClock.sleep(WAIT_TIME_FOR_REQUEST);

        ABTest.refreshExperiments();

    }

    @After
    public void clearExp() {
        Log.d(TAG, "after abort test clearing experiment");
        ABTest.clearExperiments();
    }
}



