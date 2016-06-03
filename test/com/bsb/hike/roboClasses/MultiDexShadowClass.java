package com.bsb.hike.roboClasses;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Created by ashishagarwal on 16/05/16.
 */
@Implements(MultiDex.class)
public class MultiDexShadowClass
{
	@Implementation
	public static void install(Context context)
	{
		Log.d("TAG", "multidex install");
		// Do nothing since with Robolectric nothing is dexed.
	}

}
