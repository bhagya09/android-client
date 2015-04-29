package com.bsb.hike.modules.stickerdownloadmgr;

public class StickerConstants
{
	
	public enum STState
	{
		NOT_STARTED, INITIALIZED, IN_PROGRESS, PAUSED, CANCELLED, COMPLETED, ERROR
	}
	
	public enum DownloadType
	{
		NEW_CATEGORY, UPDATE, MORE_STICKERS
	}
	
	public enum DownloadSource
	{
		FIRST_TIME, X_MORE, SHOP, RETRY, SETTINGS
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
		SHOP(6, "ssp");
		
		private final int type;
		private final String label;
		
		private StickerRequestType(int type, String label)
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
}
