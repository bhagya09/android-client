package com.bsb.hike.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import android.os.Environment;
import android.util.Log;

import com.bsb.hike.AppConfig;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;

public class Logger
{
	/**
	 * Send a {@link #VERBOSE} log message.
	 * 
	 * @param tag
	 *            Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 *            The message you would like logged.
	 */
	public static void v(String tag, String msg)
	{
		if (AppConfig.SHOW_LOGS)
		{
			Log.v(tag, msg);
		}
	}

	/**
	 * Send a {@link #VERBOSE} log message and log the exception.
	 * 
	 * @param tag
	 *            Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 *            The message you would like logged.
	 * @param tr
	 *            An exception to log
	 */
	public static void v(String tag, String msg, Throwable tr)
	{
		if (AppConfig.SHOW_LOGS)
		{
			Log.v(tag, msg, tr);
		}
	}

	/**
	 * Send a {@link #DEBUG} log message.
	 * 
	 * @param tag
	 *            Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 *            The message you would like logged.
	 */
	public static void d(String tag, final String msg)
	{
		if (AppConfig.SHOW_LOGS)
		{
			/*if (HikeConstants.TIMELINE_LOGS.equals(tag) || HikeConstants.TIMELINE_COUNT_LOGS.equals(tag))
			{
				// THIS IS TEMPORARY. REMOVE BEFORE MARKET LAUNCH TODO TODO TODO
				String dest = Environment.getExternalStorageDirectory().getAbsolutePath() + "/"+ tag + Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + ".txt";
				String message = (Utils.getFormattedDateTimeFromTimestamp(System.currentTimeMillis(), HikeMessengerApp.getInstance().getResources().getConfiguration().locale) + msg);
				BufferedWriter output = null;
				try
				{
					output = new BufferedWriter(new FileWriter(dest, true));
					output.newLine();
					output.append(message);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				finally
				{
					if (output != null)
					{
						try
						{
							output.flush();
							output.close();
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}

					}
				}
			}*/
			Log.d(tag, msg);
		}
	}

	/**
	 * Send a {@link #DEBUG} log message and log the exception.
	 * 
	 * @param tag
	 *            Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 *            The message you would like logged.
	 * @param tr
	 *            An exception to log
	 */
	public static void d(String tag, String msg, Throwable tr)
	{
		if (AppConfig.SHOW_LOGS)
		{
			Log.d(tag, msg, tr);
		}
	}

	/**
	 * Send an {@link #INFO} log message.
	 * 
	 * @param tag
	 *            Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 *            The message you would like logged.
	 */
	public static void i(String tag, String msg)
	{
		if (AppConfig.SHOW_LOGS)
		{
			Log.i(tag, msg);
		}
	}

	/**
	 * Send a {@link #INFO} log message and log the exception.
	 * 
	 * @param tag
	 *            Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 *            The message you would like logged.
	 * @param tr
	 *            An exception to log
	 */
	public static void i(String tag, String msg, Throwable tr)
	{
		if (AppConfig.SHOW_LOGS)
		{
			Log.i(tag, msg, tr);
		}
	}

	/**
	 * Send a {@link #WARN} log message.
	 * 
	 * @param tag
	 *            Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 *            The message you would like logged.
	 */
	public static void w(String tag, String msg)
	{
		if (AppConfig.SHOW_LOGS)
		{
			Log.w(tag, msg);
		}
	}

	/**
	 * Send a {@link #WARN} log message and log the exception.
	 * 
	 * @param tag
	 *            Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 *            The message you would like logged.
	 * @param tr
	 *            An exception to log
	 */
	public static void w(String tag, String msg, Throwable tr)
	{
		if (AppConfig.SHOW_LOGS)
		{
			Log.w(tag, msg, tr);
		}
	}

	/*
	 * Send a {@link #WARN} log message and log the exception.
	 * 
	 * @param tag Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
	 * 
	 * @param tr An exception to log
	 */
	public static void w(String tag, Throwable tr)
	{
		if (AppConfig.SHOW_LOGS)
		{
			Log.w(tag, tr);
		}
	}

	/**
	 * Send an {@link #ERROR} log message.
	 * 
	 * @param tag
	 *            Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 *            The message you would like logged.
	 */
	public static void e(String tag, String msg)
	{
		if (AppConfig.SHOW_LOGS)
		{
			Log.e(tag, msg);
		}
	}

	/**
	 * Send a {@link #ERROR} log message and log the exception.
	 * 
	 * @param tag
	 *            Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
	 * @param msg
	 *            The message you would like logged.
	 * @param tr
	 *            An exception to log
	 */
	public static void e(String tag, String msg, Throwable tr)
	{
		if (AppConfig.SHOW_LOGS)
		{
			Log.w(tag, msg, tr);
		}
	}
	
	public static void wtf(String tag, String msg, Throwable tr)
	{
		if (AppConfig.SHOW_LOGS)
		{
			Log.wtf(tag, msg, tr);
		}
	}
	
	public static void wtf(String tag, String msg)
	{
		if (AppConfig.SHOW_LOGS)
		{
			Log.wtf(tag, msg);
		}
	}

}