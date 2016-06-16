package com.bsb.hike;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.gcmnetworkmanager.HikeGcmNetworkMgr;
import com.bsb.hike.roboClasses.MultiDexShadowClass;

import org.junit.Before;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLog;

/**
 * Created by ashishagarwal on 23/05/16.
 */
@Config(shadows = { MultiDexShadowClass.class })
@Implements(HikeMessengerApp.class)
public class TestHikeMessengerApp extends HikeMessengerApp
{

	private String TAG = "TestHikeMessengerApp";

	@Before
	public void setUp()
	{
		ShadowLog.stream = System.out;
		Log.d(TAG, "setUp");

	}

	public void onCreate()
	{
		SharedPreferences accountSettingsPrefs = RuntimeEnvironment.application.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE);
		accountSettingsPrefs.edit()
				.putString(HikeConstants.ChatHead.PACKAGE_LIST,
						"[{\"a\":\"Whatsapp\",\"p\":\"com.whatsapp\",\"e\":true},{\"a\":\"Viber\",\"p\":\"com.viber.voip\",\"e\":true},{\"a\":\"Messenger\",\"p\":\"com.facebook.orca\",\"e\":true},{\"a\":\"Line\",\"p\":\"jp.naver.line.android\",\"e\":true},{\"a\":\"Wechat\",\"p\":\"com.tencent.mm\",\"e\":true},{\"a\":\" Telegram\",\"p\":\"org.telegram.messenger\",\"e\":true}]")
				.commit();

		super.onCreate();
		Log.d(TAG, "onCreate");
	}

	@Implementation
	public void initCrashReportingTool()
	{
		Log.d(TAG, "initCrashReportingTool");

	}

    @Implementation
    public void checkAndTriggerPendingGcmNetworkCalls()
    {
        Log.d(TAG, "checkAndTriggerPendingGcmNetworkCalls");
    }

}
