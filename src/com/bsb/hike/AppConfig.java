package com.bsb.hike;

import com.bsb.hike.utils.Utils;

public class AppConfig
{
	/*
	 * to create logs build for users 1. SHOW_LOGS = true; 2. SHOW_SEND_LOGS_OPTION = true 3. PRODUCTION_BROKER_HOST_NAME = "dmqtt.im.hike.in";
	 */

	public static boolean DEBUG_LOGS_ENABLED = Utils.isSendLogsEnabled() || BuildConfig.DEBUG;

	public static boolean SHOW_LOGS = DEBUG_LOGS_ENABLED;

	public static boolean ALLOW_STAGING_TOGGLE = BuildConfig.DEBUG;

	public static boolean SHOW_SEND_LOGS_OPTION = DEBUG_LOGS_ENABLED;

	public static String PRODUCTION_BROKER_HOST_NAME = DEBUG_LOGS_ENABLED ? "dmqtt.im.hike.in" : "mqtt.im.hike.in";

	public static String COMMIT_ID = "commitid";

	public static String BRANCH_NAME = "branchname";

	public static boolean TIMELINE_READ_DEBUG = true;

	public static void refresh()
	{
		DEBUG_LOGS_ENABLED = Utils.isSendLogsEnabled() || BuildConfig.DEBUG;

		SHOW_LOGS = DEBUG_LOGS_ENABLED;

		SHOW_SEND_LOGS_OPTION = DEBUG_LOGS_ENABLED;

		PRODUCTION_BROKER_HOST_NAME = DEBUG_LOGS_ENABLED ? "dmqtt.im.hike.in" : "mqtt.im.hike.in";
	}
}
