package com.bsb.hike;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchDatabase;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.lang.reflect.Field;

import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by ashishagarwal on 10/05/16.
 */
/**/
@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, shadows = { MultiDexShadowClass.class })
public class UtilsTest
{
	private String TAG = "UtilsTest";

	@Before
	public void setUp()
	{
		ShadowLog.stream = System.out;
		Log.d(TAG, "setUp");
	}

	@Test
	public void getFileExtensionTest()
	{
		assertEquals(Utils.getFileExtension("a.jpg"), "jpg");
		assertEquals(Utils.getFileExtension("a.exe"), "exe");
		assertEquals(Utils.getFileExtension("a.jpg"), "jpg");
		assertEquals(Utils.getFileExtension("A.JPG"), "jpg");
		assertEquals(Utils.getFileExtension("A.PNG"), "png");
		assertEquals(Utils.getFileExtension("A.png.jpg"), "jpg");
		assertEquals(Utils.getFileExtension("A"), "");
		assertEquals(Utils.getFileExtension(""), "");
		assertEquals(Utils.getFileExtension(null), null);
	}

	@Test
	public void validateBotMsisdnTest()
	{
		assertFalse(Utils.validateBotMsisdn(""));
		assertFalse(Utils.validateBotMsisdn(null));
		assertTrue(Utils.validateBotMsisdn("+9876543"));
		assertFalse(Utils.validateBotMsisdn("9876543"));
		assertTrue(Utils.validateBotMsisdn("+asdfg"));
		assertFalse(Utils.validateBotMsisdn("oiuytre"));
	}

/*  Tested this function after making it public and left here as sample
	@Test
	public void isUserUpgradingTest()
	{

		try
		{
			Context context = RuntimeEnvironment.application;
			assertFalse(Utils.isUserUpgrading(null));

			SharedPreferences accountSettingsPrefs = RuntimeEnvironment.application.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE);
			accountSettingsPrefs.edit().putString(HikeMessengerApp.CURRENT_APP_VERSION, null).commit();
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			packageInfo.versionName = "4.2.5.82.49.18.sticker_sprint_25_4_16";
			assertFalse(Utils.isUserUpgrading(context));

			packageInfo.versionName = "4.2.5.82.49.18.sticker_sprint_25_4_16";
			accountSettingsPrefs.edit().putString(HikeMessengerApp.CURRENT_APP_VERSION, "4.2.5.82.49.18.sticker_sprint_25_4_16").commit();
			assertFalse(Utils.isUserUpgrading(context));

			packageInfo.versionName = "";
			assertTrue(Utils.isUserUpgrading(context));

			packageInfo.versionName = "123";
			accountSettingsPrefs.edit().putString(HikeMessengerApp.CURRENT_APP_VERSION, "4.2.5.82.49.18.sticker_sprint_25_4_16").commit();
			assertTrue(Utils.isUserUpgrading(context));

			Context fakeContext = PowerMockito.mock(Context.class);
			PackageManager fakeManager = PowerMockito.mock(PackageManager.class);
			SharedPreferences sharedPreferences = PowerMockito.mock(SharedPreferences.class);
			when(fakeContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0)).thenReturn(sharedPreferences);
			when(sharedPreferences.getString(HikeMessengerApp.CURRENT_APP_VERSION, "")).thenReturn("abc");
			when(fakeContext.getPackageName()).thenReturn("com.bsb.hike");
			when(fakeContext.getPackageManager()).thenReturn(fakeManager);
			when(fakeManager.getPackageInfo(fakeContext.getPackageName(), 0)).thenThrow(new PackageManager.NameNotFoundException());
			assertTrue(Utils.isUserUpgrading(fakeContext));
		}
		catch (PackageManager.NameNotFoundException e)
		{
			assertTrue(false);
		}

	}
*/

	@After
	public void tearDown()
	{
		Log.d(TAG, "tearDown");
	}
}