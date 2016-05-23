package com.bsb.hike.ces;

import android.support.annotation.IntDef;

/**
 * @author suyash
 *
 */
public final class CesConstants {

	@IntDef({ CESModule.FT, CESModule.MQTT, CESModule.STICKER })
	public @interface CESModule
	{
		int FT = 0;

		int MQTT = 1;

		int STICKER = 2;
	}

	@IntDef({ CESInfoType.L1, CESInfoType.L2 })
	public @interface CESInfoType
	{
		int L1 = 0;

		int L2 = 1;
	}
	public static final String FT_MODULE = "m1";
	public static final String MQTT_MODULE = "m2";
	public static final String STICKER_MODULE = "m3";
	public static final String LEVEL_ONE = "l1";
	public static final String LEVEL_TWO = "l2";

	public static final int FT_STATUS_COMPLETE = 0;
	public static final int FT_STATUS_INCOMPLETE = 1;
	public static final int FT_STATUS_IN_PROGRESS = 2;

	public static final String FT_DOWNLOAD = "download";
	public static final String FT_UPLOAD = "upload";

	public static final class SizeBucket
	{
		public static final String BUCKET_0KB_100KB = "b0";
		public static final String BUCKET_100KB_200KB = "b1";
		public static final String BUCKET_200KB_300KB = "b2";
		public static final String BUCKET_300KB_500KB = "b3";
		public static final String BUCKET_500KB_1MB = "b4";
		public static final String BUCKET_1MB_5MB = "b5";
		public static final String BUCKET_GREATER_THAN_5MB = "b6";
	}

	public static final class LevelOneDataKey
	{
		public static final String AVERAGE_SPEED = "as";
		public static final String MANUAL_RETRIES = "mr";
		public static final String FILE_AVAILABLE_ON_SERVER = "fas";
		public static final String FT_INCOMPLETE = "fti";
		public static final String FT_UNIQUE_ID = "ft_uid";
		public static final String FT_PROCESSING_TIME = "proc_time";
		public static final String FILE_SIZES = "file_sizes";
		public static final String FILE_SIZE_BUCKET = "size_bucket";
	}

	public static final class AnalyticsV2
	{
		public static final String DIVISON = "d";
		public static final String TRIBE = "t";
		public static final String SES_ID = "sid";
	}
}
