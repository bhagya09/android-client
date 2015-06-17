package com.bsb.hike;

public class AppConfig
{
	/*
	 * to create logs build for users
	 * 1. SHOW_LOGS = true;
	 * 2. SHOW_SEND_LOGS_OPTION = true
	 * 3. PRODUCTION_BROKER_HOST_NAME = "dmqtt.im.hike.in";
	 */
	public static final boolean SHOW_LOGS = BuildConfig.DEBUG;

	public static final boolean ALLOW_STAGING_TOGGLE = BuildConfig.DEBUG;

	public static final boolean SHOW_SEND_LOGS_OPTION = BuildConfig.DEBUG;
	
	public static final String PRODUCTION_BROKER_HOST_NAME = "mqtt.im.hike.in";
	
	public static final String COMMIT_ID = "commitid";

	public static final String BRANCH_NAME = "branchname";
}
