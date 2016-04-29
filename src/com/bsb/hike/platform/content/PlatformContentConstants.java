package com.bsb.hike.platform.content;

import android.os.Environment;

import com.bsb.hike.HikeMessengerApp;

import java.io.File;

/**
 * Constants
 */
public class PlatformContentConstants
{
	public static final String CONTENT_DIR_NAME = "Content";

    public static final String MICROAPPS_CONTENT_DIR_NAME = "MicroAppsContent";

	public static final String TEMP_DIR_NAME = "Temp";

	public static final String HIKE_DIR_NAME = "Hike";

	public static final String HIKE_MICRO_APPS = "HikeMicroApps" + File.separator;

    public static final String HIKE_WEB_MICRO_APPS = "WebMicroApps" + File.separator;

    public static final String HIKE_ONE_TIME_POPUPS = "OneTimePopups" + File.separator;

    public static final String HIKE_GAMES = "Games" + File.separator;

    public static final String HIKE_MAPPS = "mApps" + File.separator;

    public static final String GAME_ENGINE_SO_FILE = "/libcocos2d.so";

    public static final String GAME_SO_FILE = "libcocos2dcpp.so";

	public static String PLATFORM_CONTENT_DIR = Environment.getExternalStorageDirectory() + File.separator + HIKE_DIR_NAME + File.separator + CONTENT_DIR_NAME + File.separator;

    public static final String KEY_TEMPLATE_PATH = "basePath";

	public static final String PLATFORM_CONFIG_FILE_NAME = "config.json";

	public static final String PLATFORM_CONFIG_VERSION_ID = "version";

	public static final String CONTENT_AUTHORITY_BASE = "content://com.bsb.hike.providers.HikeProvider/";

	public static final String CONTENT_FONTPATH_BASE = "fontpath://";

	public static final String ASSETS_FONTS_DIR = "fonts/";
	
	public static final String MESSAGE_ID = "message_id";

    public static final String MICRO_APPS_VERSIONING_PROD_CONTENT_DIR = HikeMessengerApp.getInstance().getApplicationContext().getFilesDir() + File.separator + MICROAPPS_CONTENT_DIR_NAME + File.separator;

    public static final String MICRO_APPS_VERSIONING_STAG_CONTENT_DIR = Environment.getExternalStorageDirectory() + File.separator + PlatformContentConstants.HIKE_DIR_NAME + File.separator + MICROAPPS_CONTENT_DIR_NAME + File.separator;

    // Constant to be used after versioning release only for micro apps migration purpose
    public static String PLATFORM_CONTENT_OLD_DIR = Environment.getExternalStorageDirectory() + File.separator + HIKE_DIR_NAME + File.separator + CONTENT_DIR_NAME + File.separator;

    public static final String MICROAPPS_DP_DIR = "DP";

    public static final String MICROAPPS_MACOSX_DIR = "__MACOSX";

}
