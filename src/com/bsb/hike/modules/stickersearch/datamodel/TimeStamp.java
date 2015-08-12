/**
 * File   : TimeStamp.java
 * Content: It is a modular class for customized use of time elements of the day.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.datamodel;

import java.util.Calendar;

import com.bsb.hike.utils.Utils;

public class TimeStamp
{
	private static final String NEXT_DISPLAY = ":";

	private int mHour;

	private int mMinute;

	private int mSecond;

	private int mMilliSecond;

	public TimeStamp(int hh, int mm, int ss, int sss)
	{
		mHour = hh;
		mMinute = mm;
		mSecond = ss;
		mMilliSecond = sss;
	}

	public TimeStamp(String timeStamp)
	{
		SetTimeStamp(timeStamp, 0, 0, 0, 0);
	}

	public TimeStamp(String timeStamp, int default_hh, int default_mm, int default_ss, int default_sss)
	{
		SetTimeStamp(timeStamp, default_hh, default_mm, default_ss, default_sss);
	}

	private void SetTimeStamp(String timeStamp, int default_hh, int default_mm, int default_ss, int default_sss)
	{
		if (timeStamp == null)
		{
			mHour = 0;
			mMinute = 0;
			mSecond = 0;
			mMilliSecond = 0;
		}
		else
		{
			String[] timeStampStrings = timeStamp.split(NEXT_DISPLAY);
			int[] timeStampElements = new int[4];

			for (int i = 0; i < 4; i++)
			{
				if (i < timeStampStrings.length)
				{
					try
					{
						timeStampElements[i] = Integer.parseInt(timeStampStrings[i]);
					}
					catch (NumberFormatException e)
					{
						timeStampElements[i] = 0;
					}
				}
				else
				{
					timeStampElements[i] = 0;
				}
			}

			mHour = timeStampElements[0];
			mMinute = timeStampElements[1];
			mSecond = timeStampElements[2];
			mMilliSecond = timeStampElements[3];
		}
	}

	public int getHour()
	{
		return mHour;
	}

	public int getMinute()
	{
		return mMinute;
	}

	public int getSecond()
	{
		return mSecond;
	}

	public int getMilliSecond()
	{
		return mMilliSecond;
	}

	public boolean isValidTimeStampOfTheDay()
	{
		return (mHour >= 0) && (mHour < 24) && (mMinute >= 0) && (mMinute < 60) && (mSecond >= 0) && (mSecond < 60) && (mMilliSecond >= 0) && (mMilliSecond < 1000);
	}

	public long getTimeInMillis()
	{
		return Utils.getTimeInMillis(Calendar.getInstance(), mHour, mMinute, mSecond, mMilliSecond);
	}

	@Override
	public boolean equals(Object other)
	{
		boolean result = (other != null) && (other instanceof TimeStamp);

		if (result)
		{
			TimeStamp ts = (TimeStamp) other;
			result = (mHour == ts.getHour()) && (mMinute == ts.getMinute()) && (mSecond == ts.getSecond()) && (mMilliSecond == ts.getMilliSecond());
		}

		return result;
	}

	@Override
	public String toString()
	{
		return (mHour + NEXT_DISPLAY + mMinute + NEXT_DISPLAY + mSecond + NEXT_DISPLAY + mMilliSecond);
	}
}