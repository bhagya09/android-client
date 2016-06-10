package com.bsb.hike.abtest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Created by anshumanraypritam on 08/06/16.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        ABTestInitTest.class,
        ABTestRolloutTest.class,
        ABTestAbortTest.class
})
public class ABTestSuiteTest {
}
