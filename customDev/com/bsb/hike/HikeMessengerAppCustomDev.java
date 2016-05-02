package com.bsb.hike;

import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.crashlytics.android.Crashlytics;
import com.facebook.stetho.Stetho;

import org.acra.ACRA;
import org.acra.ErrorReporter;

import io.fabric.sdk.android.Fabric;

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
		Logger.d(TAG, "Running HikeMessengerApp : debug" + "Adding Stetho now");
		Stetho.initialize(Stetho.newInitializerBuilder(this).enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
				.enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this)).build());
	}

	/**
	 * For CustomDev builds, keeping crashlytics on by default. This is purely for testing purposes.
	 */
	@Override
	public void initCrashReportingTool()
	{
		if(HikeSharedPreferenceUtil
				.getInstance().getData(HikeConstants.CRASH_REPORTING_TOOL,HikeConstants.CRASHLYTICS).equals(HikeConstants.CRASHLYTICS))
		{
			Logger.d(TAG,"Initializing Crashlytics");
			Fabric.with(this, new Crashlytics());
			logUser();
		}
		else
		{
			Logger.d(TAG,"Initializing ACRA");
			ACRA.init(this);
			CustomReportSender customReportSender = new CustomReportSender();
			ErrorReporter.getInstance().setReportSender(customReportSender);
		}
	}
}
