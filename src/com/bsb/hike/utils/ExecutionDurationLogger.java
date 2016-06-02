package com.bsb.hike.utils;

/**
 * Precision points definition for duration logging
 */
public final class ExecutionDurationLogger {
    public static final String TAG = ExecutionDurationLogger.class.getSimpleName();

    public static final int PRECISION_UNIT_SECOND = 0;

    public static final int PRECISION_UNIT_MILLI_SECOND = 3;

    public static final int PRECISION_UNIT_MICRO_SECOND = 6;

    public static final int PRECISION_UNIT_NANO_SECOND = 9;

    public static final String sec = " s";

    public static final String ms = " ms";

    public static final String μs = " μs";

    public static final String ns = " ns";

    public static final String DELIMITER = ", ";
}
