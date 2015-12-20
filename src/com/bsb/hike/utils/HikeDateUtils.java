package com.bsb.hike.utils;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.text.format.Time;

import com.bsb.hike.R;

import java.util.Locale;

/**
 * Created by gauravmittal on 23/11/15.
 */
public class HikeDateUtils extends DateUtils
{

    /**
     * Returns a string describing 'time' as a time relative to 'now'.
     * <p>
     * Time spans in the past are formatted like "42 minutes ago". Time spans in
     * the future are formatted like "in 42 minutes".
     * <p>
     * Can use {@link #FORMAT_ABBREV_RELATIVE} flag to use abbreviated relative
     * times, like "42 mins ago".
     *
     * @param time the time to describe, in milliseconds
     * @param now the current time in milliseconds
     * @param minResolution the minimum timespan to report. For example, a time
     *            3 seconds in the past will be reported as "0 minutes ago" if
     *            this is set to MINUTE_IN_MILLIS. Pass one of 0,
     *            MINUTE_IN_MILLIS, HOUR_IN_MILLIS, DAY_IN_MILLIS,
     *            WEEK_IN_MILLIS
     * @param flags a bit mask of formatting options, such as
     *            {@link #FORMAT_NUMERIC_DATE} or
     *            {@link #FORMAT_ABBREV_RELATIVE}
     */
    public static CharSequence getRelativeTimeSpanString(Context c,long time, long now, long minResolution,
                                                  int flags)
    {
        boolean abbrevRelative = (flags & (FORMAT_ABBREV_RELATIVE | FORMAT_ABBREV_ALL)) != 0;

        boolean past = (now >= time);
        long duration = Math.abs(now - time);

        int resId;
        long count;
        if (duration < MINUTE_IN_MILLIS && minResolution < MINUTE_IN_MILLIS) {
            count = duration / SECOND_IN_MILLIS;
            if (past) {
                if (abbrevRelative) {
                    resId = R.plurals.abbrev_num_seconds_ago;
                } else {
                    resId = R.plurals.num_seconds_ago;
                }
            } else {
                if (abbrevRelative) {
                    resId = R.plurals.abbrev_in_num_seconds;
                } else {
                    resId = R.plurals.in_num_seconds;
                }
            }
        } else if (duration < HOUR_IN_MILLIS && minResolution < HOUR_IN_MILLIS) {
            count = duration / MINUTE_IN_MILLIS;
            if (past) {
                if (abbrevRelative) {
                    resId = R.plurals.abbrev_num_minutes_ago;
                } else {
                    resId = R.plurals.num_minutes_ago;
                }
            } else {
                if (abbrevRelative) {
                    resId = R.plurals.abbrev_in_num_minutes;
                } else {
                    resId = R.plurals.in_num_minutes;
                }
            }
        } else if (duration < DAY_IN_MILLIS && minResolution < DAY_IN_MILLIS) {
            count = duration / HOUR_IN_MILLIS;
            if (past) {
                if (abbrevRelative) {
                    resId = R.plurals.abbrev_num_hours_ago;
                } else {
                    resId = R.plurals.num_hours_ago;
                }
            } else {
                if (abbrevRelative) {
                    resId = R.plurals.abbrev_in_num_hours;
                } else {
                    resId = R.plurals.in_num_hours;
                }
            }
        } else if (duration < WEEK_IN_MILLIS && minResolution < WEEK_IN_MILLIS) {
            return getRelativeDayString(c, time, now);
        } else {
            // We know that we won't be showing the time, so it is safe to pass
            // in a null context.
            return formatDateRange(null, time, time, flags);
        }

        String format = c.getResources().getQuantityString(resId, (int) count);
        return String.format(format, count);
    }

    /**
     * Returns a string describing a day relative to the current day. For example if the day is
     * today this function returns "Today", if the day was a week ago it returns "7 days ago", and
     * if the day is in 2 weeks it returns "in 14 days".
     *
     * @param c the context
     * @param day the relative day to describe in UTC milliseconds
     * @param today the current time in UTC milliseconds
     */
    private static final String getRelativeDayString(Context c, long day, long today) {

        // TODO: use TimeZone.getOffset instead.
        Time startTime = new Time();
        startTime.set(day);
        int startDay = Time.getJulianDay(day, startTime.gmtoff);

        Time currentTime = new Time();
        currentTime.set(today);
        int currentDay = Time.getJulianDay(today, currentTime.gmtoff);

        int days = Math.abs(currentDay - startDay);
        boolean past = (today > day);

        // TODO: some locales name other days too, such as de_DE's "Vorgestern" (today - 2).
        if (days == 1) {
            if (past) {
                return c.getString(R.string.yesterday);
            } else {
                return c.getString(R.string.tomorrow);
            }
        } else if (days == 0) {
            return c.getString(R.string.today);
        }

        int resId;
        if (past) {
            resId = R.plurals.num_days_ago;
        } else {
            resId = R.plurals.in_num_days;
        }

        String format = c.getResources().getQuantityString(resId, days);
        return String.format(format, days);
    }
}