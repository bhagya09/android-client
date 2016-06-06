package com.bsb.hike.ces;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.bsb.hike.utils.HikeSharedPreferenceUtil;

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

	public static String getDayBeforeUTCDate(String date)
	{
		Calendar cldr = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			Date time = sdf.parse(date);
			cldr.setTime(time);
			cldr.add(Calendar.DAY_OF_YEAR, -1);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sdf.format(cldr.getTime());
	}

	public static double getSpeedInKbps(long fileSize, long procTime)
	{
		if(procTime == 0)
		{
			return 0;
		}
		double value = ((double)fileSize/1024)/((double)procTime/1000);
		value = Double.parseDouble(new DecimalFormat("##.##").format(value));
		return value;
	}

	public static int getMaxSpeed(String network)
	{
		int result;
		if(network.equals(CesConstants.NET_NONE))
		{
			result = HikeSharedPreferenceUtil.getInstance().getData(CesConstants.NET_NONE, CesConstants.MAX_SPEED_ON_NO_NET);
		}
		else if(network.equals(CesConstants.NET_UNKNOWN))
		{
			result = HikeSharedPreferenceUtil.getInstance().getData(CesConstants.NET_UNKNOWN, CesConstants.MAX_SPEED_ON_UNKNOWN);
		}
		else if(network.equals(CesConstants.NET_2G))
		{
			result = HikeSharedPreferenceUtil.getInstance().getData(CesConstants.NET_2G, CesConstants.MAX_SPEED_ON_2G);
		}
		else if(network.equals(CesConstants.NET_3G))
		{
			result = HikeSharedPreferenceUtil.getInstance().getData(CesConstants.NET_3G, CesConstants.MAX_SPEED_ON_3G);
		}
		else if(network.equals(CesConstants.NET_4G))
		{
			result = HikeSharedPreferenceUtil.getInstance().getData(CesConstants.NET_4G, CesConstants.MAX_SPEED_ON_4G);
		}
		else if(network.equals(CesConstants.NET_WIFI))
		{
			result = HikeSharedPreferenceUtil.getInstance().getData(CesConstants.NET_WIFI, CesConstants.MAX_SPEED_ON_WIFI);
		}
		else
		{
			result = HikeSharedPreferenceUtil.getInstance().getData(CesConstants.NET_UNKNOWN, CesConstants.MAX_SPEED_ON_UNKNOWN);
		}
		return result;
	}

	public static int whichModule(String module)
	{
		int result = -1;
		if(module.equals(CesConstants.FT_MODULE))
		{
			result = CesConstants.CESModule.FT;
		}
		return result;
	}
}
