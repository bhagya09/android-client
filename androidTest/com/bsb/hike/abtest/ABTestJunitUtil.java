package com.bsb.hike.abtest;

import com.hike.abtest.ABTest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by anshumanraypritam on 09/06/16.
 * <p/>
 * This class contains all the constants for the junit test cases
 */
public class ABTestJunitUtil {


    public static final long WAIT_TIME_FOR_REQUEST = 10000;

    public static final int DEFAULT_VALUE = 1000;

    public static final int EXPERIMENT_DEFAULT_VALUE = 1;

    public static final int EXPERIMENT_VALUE = 2;

    public static final String REQUEST_TYPE_EXPERIMENT_INIT = "AB-Exp-Init";

    public static final String REQUEST_TYPE_EXPERIMENT_ABORT = "AB-Exp-Abort";

    public static final String REQUEST_TYPE_EXPERIMENT_ROLL_OUT = "AB-Exp-Rollout";

    public static final String VARIABEL_NAME = "ABTEST-SAMPLE-01";


    public static final int ROLLOUT_EXPERIMENT_DEFAULT_VALUE = 200;

    public static final int ROLLOUT_EXPERIMENT_VALUE = 200;

    public static final boolean EXPERIMENT_BOOLEAN_DEFAULT_VALUE = false;

    public static final boolean EXPERIMENT_BOOLEAN_EXPECTED_VALUE = true;

    public static final long EXPERIMENT_LONG_DEFAULT_VALUE = 888888888;

    public static final long EXPERIMENT_LONG_EXPECTED_VALUE = 999999999;


    public static final String EXPERIMENT_STRING_DEFAULT_VALUE = "Hike";

    public static final String EXPERIMENT_STRING_EXPECTED_VALUE = "Hike Ltd";


    /**
     * This is a reflection  method which can have the access to the private method of the ABTest clearexperiment method
     */

    public static void reflect_ClearExp(){
        Method method = null;
        try {

            method = ABTest.class.getDeclaredMethod("clearExperiments");
            method.setAccessible(true);
            method.invoke(null);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * This is a reflection  method which can have the access to the private method of the ABTest refreshExperiment method
     */

    public static void reflect_RefreshExp(){
        Method method = null;
        try {

            method = ABTest.class.getDeclaredMethod("refreshExperiments");
            method.setAccessible(true);
            method.invoke(null);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
