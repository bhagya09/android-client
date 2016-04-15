package com.bsb.hike.modules.stickerdownloadmgr;

public class StickerConstants
{
	public static final String STICKER_SETTINGS_TASK_ARG = "stickerSettingsTaskArg";

	public enum StickerSettingsTask
	{
		STICKER_REORDER_TASK, STICKER_DELETE_TASK, STICKER_HIDE_TASK, STICKER_UPDATE_TASK, STICKER_INVALID_TASK;

		private int task;
	}

	public enum STState
	{
		NOT_STARTED, INITIALIZED, IN_PROGRESS, PAUSED, CANCELLED, COMPLETED, ERROR
	}

	public enum DownloadType
	{
		NEW_CATEGORY, UPDATE, MORE_STICKERS
	}

	public enum StickerType
	{
		MINI("mini"), SMALL("small"), LARGE("large");

        private String value;

        StickerType(String value)
        {
            this.value = value;
        }

        public String getValue()
        {
            return value;
        }
	}

	public enum DownloadSource
	{
		FIRST_TIME(0), X_MORE(1), SHOP(2), RETRY(3), SETTINGS(4), PREVIEW(5), POPUP(7);

		private int value;

		DownloadSource(int value)
		{
			this.value = value;
		}

		public int getValue()
		{
			return value;
		}

	}

	public enum HttpRequestType
	{
		POST, GET, HEAD
	}

	public enum StickerRequestType
	{
		SINGLE(0, "ss"),
		MULTIPLE(1, "sm"),
		PREVIEW(2, "sp"),
		ENABLE_DISABLE(3, "sed"),
		SIZE(4, "ssz"),
		SIGNUP_UPGRADE(5, "ssu"),
		SHOP(6, "ssp"),
		TAGS(7, "st"),
		SINGLE_TAG(8, "sit"),
		CATEGORY_DETAIL(9, "scd"),
		MINI(10, "mini"),
		FORCED(11, "forced"),
		FETCH_CATEGORY(12, "ftch"),
		UPDATE_CATEGORY(13, "updtCt"),
		UPDATE_ORDER(14, "updtOdr");

		private final int type;
		private final String label;
		
		StickerRequestType(int type, String label)
		{
			this.type = type;
			this.label = label;
		}

		public int getType()
		{
			return this.type;
		}
		public String getLabel()
		{
			return this.label;
		}
	};

	public static final int DEFAULT_STICKER_THRESHOLD_FOR_CDN = 5;

	public static final long DEFAULT_TTL_MINI_STICKERS = 1 * 24 * 60 * 60 * 1000; // 1 day

	public static final short DEFAULT_PACK_PREVIEW_VIEW_ALL_VISIBLE_ROWS = 3;
}
