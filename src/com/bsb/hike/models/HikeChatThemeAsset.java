package com.bsb.hike.models;

import com.bsb.hike.chatthemes.HikeChatThemeConstants;

/**
 * Created by sriram on 22/02/16.
 */
public class HikeChatThemeAsset
{
	private String assetId;

	private int type;

	private String value;

	private byte isDownloaded = HikeChatThemeConstants.ASSET_DOWNLOAD_STATUS_NOT_DOWNLOADED;

	public HikeChatThemeAsset(String assetId, int type, String value)
	{
		this.assetId = assetId;
		this.type = type;
		this.value = value;
	}

	public int getType()
	{
		return type;
	}

	public void setType(int type)
	{
		this.type = type;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public String getAssetId()
	{
		return assetId;
	}

	public void setAssetId(String assetId)
	{
		this.assetId = assetId;
	}

	public boolean isDownloaded()
	{
		return (isDownloaded == HikeChatThemeConstants.ASSET_DOWNLOAD_STATUS_DOWNLOADED);
	}

	public boolean isDownloadingInProgress()
	{
		return (isDownloaded == HikeChatThemeConstants.ASSET_DOWNLOAD_STATUS_DOWNLOADING);
	}

	public boolean isAssetMissing()
	{
		return (isDownloaded == HikeChatThemeConstants.ASSET_DOWNLOAD_STATUS_NOT_DOWNLOADED);
	}

	public void setIsDownloaded(byte status)
	{
		this.isDownloaded = status;
	}

	public byte getAssetDownloadStatus()
	{
		return this.isDownloaded;
	}
}
