package com.bsb.hike.ces;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * @author suyash
 *
 */
public class CesUtils{

	public static String getCurrentUTCDate()
	{
		Calendar cldr = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(cldr.getTime());
	}

	public static String getDayBeforeUTCDate()
	{
		Calendar cldr = Calendar.getInstance();
		cldr.add(Calendar.DAY_OF_YEAR, -1);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(cldr.getTime());
	}

	public static double getSpeedInKbps(long fileSize, long procTime)
	{
		return ((double)fileSize/1024)/((double)procTime/1000);
	}

	public static int getMaxSpeed(String network)
	{
		int netType = Integer.parseInt(network);
		int result = CesConstants.MAX_SPEED_ON_UNKNOWN;
		switch (netType) {
		case CesConstants.NET_NONE:
			result = CesConstants.MAX_SPEED_ON_NO_NET;
			break;
		case CesConstants.NET_UNKNOWN:
			result = CesConstants.MAX_SPEED_ON_UNKNOWN;
			break;
		case CesConstants.NET_2G:
			result = CesConstants.MAX_SPEED_ON_2G;
			break;
		case CesConstants.NET_3G:
			result = CesConstants.MAX_SPEED_ON_3G;
			break;
		case CesConstants.NET_4G:
			result = CesConstants.MAX_SPEED_ON_4G;
			break;
		case CesConstants.NET_WIFI:
			result = CesConstants.MAX_SPEED_ON_WIFI;
			break;
		}
		return result;
	}
}
