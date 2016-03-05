package com.bsb.hike;

import com.bsb.hike.utils.Logger;
import com.facebook.stetho.Stetho;
import com.squareup.leakcanary.LeakCanary;

/**
 * Created by piyush on 25/02/16.
 */
public class HikeMessengerAppCustomDev extends HikeMessengerApp
{
	public static final String TAG = "HikeMessengerAppCustomDev";

	@Override
	public void onCreate()
	{
		super.onCreate();
		Logger.d(TAG, "Running HikeMessengerAppCustomDevDebug : " + "Injecting Stetho + LeakCanary Libraries");
		Stetho.initialize(Stetho.newInitializerBuilder(this).enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
				.enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this)).build());
		LeakCanary.install(this);
	}
}
