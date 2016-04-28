package com.hike.abtest;

import android.util.Log;

/**
 * Created by abhijithkrishnappa on 04/04/16.
 */
public class Logger {
    public static final boolean DEBUG = true;
    public static final String prefix = "ABTest ";


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
        if (DEBUG)
        {
            Log.v(tag, prefix+msg);
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
        if (DEBUG)
        {
            Log.v(tag, prefix+msg, tr);
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
    public static void d(String tag, String msg)
    {
        if (DEBUG)
        {
            Log.d(tag, prefix+msg);
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
        if (DEBUG)
        {
            Log.d(tag, prefix+msg, tr);
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
        if (DEBUG)
        {
            Log.i(tag, prefix+msg);
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
        if (DEBUG)
        {
            Log.i(tag, prefix+msg, tr);
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
        if (DEBUG)
        {
            Log.w(tag, prefix+msg);
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
        if (DEBUG)
        {
            Log.w(tag, prefix+msg, tr);
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
        if (DEBUG)
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
        if (DEBUG)
        {
            Log.e(tag, prefix+msg);
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
        if (DEBUG)
        {
            Log.w(tag, prefix+msg, tr);
        }
    }

    public static void wtf(String tag, String msg, Throwable tr)
    {
        if (DEBUG)
        {
            Log.wtf(tag, prefix+msg, tr);
        }
    }

    public static void wtf(String tag, String msg)
    {
        if (DEBUG)
        {
            Log.wtf(tag, prefix+msg);
        }
    }


}
