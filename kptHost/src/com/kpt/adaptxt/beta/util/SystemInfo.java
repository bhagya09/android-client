package com.kpt.adaptxt.beta.util;


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;

/**
 *  Class that gives you all the required information related to a device like
 *  the android version of the device, brand info, country, manufacturer, model of the device
 *  etc
 * 
 */
public final class SystemInfo {
	//private static final String TAG = "SystemInfo";

	public static String getAllSettings() {
		return "";
	}

	public static String getAndroidVersion() {
		Object[] arrayOfObject = new Object[1];
		String str = Build.VERSION.RELEASE;
		arrayOfObject[0] = str;
		return String.format("Android version: %s", arrayOfObject);
	}

	/* public static String getApplicationInfo(Context paramContext)
  {
    Object[] arrayOfObject = new Object[8];
    String str1 = getCountry(paramContext);
    arrayOfObject[0] = str1;
    String str2 = getBrandInfo();
    arrayOfObject[1] = str2;
    String str3 = getModelInfo();
    arrayOfObject[2] = str3;
    String str4 = getDeviceInfo();
    arrayOfObject[3] = str4;
    String str5 = getVersionInfo(paramContext);
    arrayOfObject[4] = str5;
    String str6 = getLocale(paramContext);
    arrayOfObject[5] = str6;
    String str7 = getAndroidVersion();
    arrayOfObject[6] = str7;
    String str8 = getSupportTAG(paramContext);
    arrayOfObject[7] = str8;
    return String.format("%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n", arrayOfObject);
  }*/

	public static String getBrandInfo()
	{
		Object[] arrayOfObject = new Object[1];
		String str = Build.BRAND;
		arrayOfObject[0] = str;
		return String.format("Brand: %s", arrayOfObject);
	}

	public static String getCountry(Context paramContext)
	{
		TelephonyManager localTelephonyManager = (TelephonyManager)paramContext.getSystemService("phone");
		Object[] arrayOfObject = new Object[1];
		String str = localTelephonyManager.getNetworkCountryIso();
		arrayOfObject[0] = str;
		return String.format("Country: %s", arrayOfObject);
	}

	public static String getDeviceInfo()
	{
		Object[] arrayOfObject = new Object[1];
		String str = Build.DEVICE;
		arrayOfObject[0] = str;
		return String.format("Device: %s", arrayOfObject);
	}

	public static String getLocale(Context paramContext)
	{
		Object[] arrayOfObject = new Object[1];
		String str = paramContext.getResources().getConfiguration().locale.getDisplayName();
		arrayOfObject[0] = str;
		return String.format("Locale: %s", arrayOfObject);
	}

	public static String getManufacturer()
	{
		String str = null;
		try
		{
			str = (String)Build.class.getDeclaredField("MANUFACTURER").get(null);
			return str;
		}
		catch (Exception localException)
		{
			while (true)
				str = "";
		}
	}

	public static String getModelInfo()
	{
		Object[] arrayOfObject = new Object[1];
		String str = Build.MODEL;
		arrayOfObject[0] = str;
		return String.format("Model: %s", arrayOfObject);
	}

	public static PackageInfo getPackageInfo(Context paramContext){
		PackageInfo localPackageInfo1 = null;
		try{
			PackageManager localPackageManager = paramContext.getPackageManager();
			String str = paramContext.getPackageName();
			localPackageInfo1 = localPackageManager.getPackageInfo(str, 0);
			return localPackageInfo1;
		}
		catch (PackageManager.NameNotFoundException localNameNotFoundException){
			return localPackageInfo1;
		}
	}

	/*public static String getSupportTAG(Context paramContext){
		String str = InstallationId.getId(paramContext);
		Object[] arrayOfObject;
		if (str != null)
		{
			arrayOfObject = new Object[1];
			arrayOfObject[0] = str;
		}
		for (str = String.format("Support TAG: %s", arrayOfObject); ; str = "")
			return str;
	}*/

	public static String getVersionInfo(Context paramContext){
		PackageInfo localPackageInfo = getPackageInfo(paramContext);
		Object[] arrayOfObject = new Object[2];
		String str = localPackageInfo.versionName;
		arrayOfObject[0] = str;
		Integer localInteger = Integer.valueOf(localPackageInfo.versionCode);
		arrayOfObject[1] = localInteger;
		return String.format("Version: %s (release %s)", arrayOfObject);
	}
}