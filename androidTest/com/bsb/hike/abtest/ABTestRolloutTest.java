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
 * Created by anshumanraypritam on 08/06/16.
 */

@RunWith(AndroidJUnit4.class)
public class ABTestRolloutTest {

    private static final String TAG = ABTestRolloutTest.class.getSimpleName();



    @Before
    public void init()
    {
        Log.d(TAG, "Initializing ABTEST before every test case");
        ABTest.clearExperiments();


    }
    @Test
    public void expRollout1() throws Throwable {

        rollOutTest();

    }

    /**
     * This test case for the initializing the experiment
     */
    private void rollOutTest() {

        Log.d(TAG, "testABExprollOut initial Default Value: " + ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
        Assert.assertEquals(ABTestJunitConstants.DEFAULT_VALUE, ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));

        String experimentInit = "{ \"t\": \"AB-Exp-Rollout\", \"d\": { \"md\": { \"ver\": 1, \"expList\": [ { \"expId\":\"SAMPLE-EXPERIMENTID-1\", \"expType\": 0, \"desc\": \"AB Demo\", \"variantId\" : \"SAMPLE-VariantID-1\", \"sTime\": 1461643490000, \"eTime\": 1462853090000, \"varList\": [ { \"varName\":\"ABTEST-SAMPLE-01\", \"type\" : 2, \"defValue\": \"200\", \"expValue\": \"200\" } ], \"cbUrl\" : \"http://hike.co.in/...\" } ] } } }";
        parse_StartService(ABTestJunitConstants.REQUEST_TYPE_EXPERIMENT_ROLL_OUT,experimentInit);


        Log.d(TAG, "testABExprollOut Experimental Value: " + ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
        Assert.assertEquals(ABTestJunitConstants.ROLLOUT_EXPERIMENT_VALUE, ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));

    }

    @Test
    public void expRollout2() throws Throwable {

        rolloutValidExp();

    }

    /**
     * This test case for the rollout  with valid init experiments
     */
    private void rolloutValidExp() throws Throwable {

        ABTest.refreshExperiments();
        Log.d(TAG, "testABExprollOut  multiple experiment Default Value: " + ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
        Assert.assertEquals(ABTestJunitConstants.DEFAULT_VALUE, ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));

        //new ABTestInitTest().testABExpInit1();

        String experimentInit = "{ \"t\": \"AB-Exp-Init\", \"d\": { \"md\": { \"ver\": 1, \"expList\": [ { \"expId\":\"SAMPLE-EXPERIMENTID-1\", \"expType\": 0, \"desc\": \"AB Demo\", \"variantId\" : \"SAMPLE-VariantID-1\", \"sTime\": 1461643490000, \"eTime\": 1466380914000, \"varList\": [ { \"varName\":\"ABTEST-SAMPLE-01\", \"type\" : 2, \"defValue\": \"1\", \"expValue\": \"2\" } ], \"cbUrl\" : \"http://hike.co.in/...\" } ] } } }";
        parse_StartService(ABTestJunitConstants.REQUEST_TYPE_EXPERIMENT_INIT,experimentInit);

        String rolloutvalidexp = "{ \"t\": \"AB-Exp-Rollout\", \"d\": { \"md\": { \"ver\": 1, \"expList\": [ { \"expId\":\"SAMPLE-EXPERIMENTID-1\", \"expType\": 0, \"desc\": \"AB Demo\", \"variantId\" : \"SAMPLE-VariantID-1\", \"sTime\": 1461643490000, \"eTime\": 1462853090000, \"varList\": [ { \"varName\":\"ABTEST-SAMPLE-01\", \"type\" : 2, \"defValue\": \"200\", \"expValue\": \"200\" } ], \"cbUrl\" : \"http://hike.co.in/...\" } ] } } }";
        parse_StartService(ABTestJunitConstants.REQUEST_TYPE_EXPERIMENT_ROLL_OUT,rolloutvalidexp);

        Log.d(TAG, "testABExprollOut multiple experiment Experimental Value: " + ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
        Assert.assertEquals(ABTestJunitConstants.ROLLOUT_EXPERIMENT_VALUE, ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
    }

    @Test
    public void expRollout3() throws Throwable {

        rollOutTimeExpired();


    }

    /**
     * This method is for the rollout experiments  which has passed  the expired time
     */
    private void rollOutTimeExpired() throws Throwable {
        ABTest.refreshExperiments();
        Log.d(TAG, "testABExpExpiredTimerollOut Default Value: " + ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
        Assert.assertEquals(ABTestJunitConstants.DEFAULT_VALUE, ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));

        //new ABTestInitTest().testABExpInit3();

        String experimentInit = "{ \"t\": \"AB-Exp-Init\", \"d\": { \"md\": { \"ver\": 1, \"expList\": [ { \"expId\":\"SAMPLE-EXPERIMENTID-1\", \"expType\": 0, \"desc\": \"AB Demo\", \"variantId\" : \"SAMPLE-VariantID-1\", \"sTime\": 1465107040000, \"eTime\": 1465193440000, \"varList\": [ { \"varName\":\"ABTEST-SAMPLE-01\", \"type\" : 2, \"defValue\": \"1\", \"expValue\": \"2\" } ], \"cbUrl\" : \"http://hike.co.in/...\" } ] } } }";
        parse_StartService(ABTestJunitConstants.REQUEST_TYPE_EXPERIMENT_INIT,experimentInit);


        String experimentRollout = "{ \"t\": \"AB-Exp-Rollout\", \"d\": { \"md\": { \"ver\": 1, \"expList\": [ { \"expId\":\"SAMPLE-EXPERIMENTID-1\", \"expType\": 0, \"desc\": \"AB Demo\", \"variantId\" : \"SAMPLE-VariantID-1\", \"sTime\": 1465107040000, \"eTime\": 1465193440000, \"varList\": [ { \"varName\":\"ABTEST-SAMPLE-01\", \"type\" : 2, \"defValue\": \"200\", \"expValue\": \"200\" } ], \"cbUrl\" : \"http://hike.co.in/...\" } ] } } }";
        parse_StartService(ABTestJunitConstants.REQUEST_TYPE_EXPERIMENT_ROLL_OUT,experimentRollout);

        Log.d(TAG, "testABExpExpiredTimerollOut Experimental Default Value: " + ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));
        Assert.assertEquals(ABTestJunitConstants.ROLLOUT_EXPERIMENT_DEFAULT_VALUE, ABTest.getInt(ABTestJunitConstants.VARIABEL_NAME, ABTestJunitConstants.DEFAULT_VALUE));


    }


    /**
     * This method has to be take the request for the json format and start the intent service
     * @param request
     * @param experiment
     */

    private void parse_StartService(String request, String experiment){
        JSONObject experimentInitJson = null;

        try {
            experimentInitJson = new JSONObject(experiment);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ABTest.onRequestReceived(ABTestJunitConstants.REQUEST_TYPE_EXPERIMENT_ROLL_OUT, experimentInitJson);

        SystemClock.sleep(ABTestJunitConstants.WAIT_TIME_FOR_REQUEST);

        ABTest.refreshExperiments();

    }

    @After
    public void clearExp(){
        Log.d(TAG, "Clearing ABTEST at after every test case");
        ABTest.clearExperiments();
    }
}


