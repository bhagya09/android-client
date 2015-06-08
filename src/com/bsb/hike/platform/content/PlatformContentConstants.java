package com.bsb.hike.platform.content;

import java.io.File;

import android.os.Environment;

/**
 * Constants
 */
public class PlatformContentConstants
{
	public static final String CONTENT_DIR_NAME = "Content";

	public static final String TEMP_DIR_NAME = "Temp";

	public static final String HIKE_DIR_NAME = "Hike";

	public static String PLATFORM_CONTENT_DIR = Environment.getExternalStorageDirectory() + File.separator + HIKE_DIR_NAME + File.separator + CONTENT_DIR_NAME + File.separator;

	public static final String KEY_TEMPLATE_PATH = "basePath";

	public static final String PLATFORM_CONFIG_FILE_NAME = "config.json";

	public static final String PLATFORM_CONFIG_VERSION_ID = "version";

	public static final String CONTENT_AUTHORITY_BASE = "content://com.bsb.hike.providers.HikeProvider/";

	public static final String CONTENT_FONTPATH_BASE = "fontpath://";

	public static final String ASSETS_FONTS_DIR = "fonts/";
	
	public static final String MESSAGE_ID = "message_id";
}
