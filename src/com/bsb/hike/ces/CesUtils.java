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
}
