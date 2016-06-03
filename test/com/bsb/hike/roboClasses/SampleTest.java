package com.bsb.hike.roboClasses;

import android.util.Log;

import com.bsb.hike.BuildConfig;
import com.bsb.hike.roboClasses.CustomRobolectricGradleTestRunner;
import com.bsb.hike.roboClasses.MultiDexShadowClass;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by ashishagarwal on 30/05/16.
 */

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, shadows = { MultiDexShadowClass.class })
public class SampleTest
{

	private String TAG = "SampleTest";

	@Before
	public void setUp()
	{
		ShadowLog.stream = System.out;
		Log.d(TAG, "setUp");
	}

	@Test
	public void a()
	{
		assertFalse(false);
		Log.d(TAG, "a");
	}

	@Test
	public void b()
	{
		Log.d(TAG, "b");
		Assert.assertTrue(true);
	}

	@Test
	public void c()
	{
		Log.d(TAG, "c");
		assertNull(null);
	}

	@After
	public void tearDown()
	{
		Log.d(TAG, "tearDown");
	}
}
