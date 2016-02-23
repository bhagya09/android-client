package com.bsb.hike.models;

import android.graphics.Bitmap;

import com.bsb.hike.chatthemes.HikeChatThemeConstants;

/**
 * Created by sriram on 22/02/16.
 */
public class HikeChatTheme
{

	private String themeId;

	private Bitmap thumbnailBmp;

	private boolean themeVisibilityStatus;

	private int assetDownloadStatus = 0;

	private int themeType;

	private HikeChatThemeAsset[] assets;

	private String metadata;

	public HikeChatTheme()
	{
		assets = new HikeChatThemeAsset[HikeChatThemeConstants.ASSET_INDEX_COUNT];
	}

	public boolean isVisible()
	{
		return themeVisibilityStatus;
	}

	public void setVisibilityStatus(boolean status)
	{
		this.themeVisibilityStatus = status;
	}

	public void setAsset(byte type, HikeChatThemeAsset asset)
	{
		this.assets[type] = asset;
	}

	public HikeChatThemeAsset getAsset(byte type)
	{
		return this.assets[type];
	}

	public HikeChatThemeAsset[] getAssets()
	{
		return this.assets;
	}

	public String getThemeId()
	{
		return themeId;
	}

	public void setThemeId(String themeId)
	{
		this.themeId = themeId;
	}

	public Bitmap getThumbnailBmp()
	{
		return thumbnailBmp;
	}

	public void setThumbnailBmp(Bitmap thumbnailBmp)
	{
		this.thumbnailBmp = thumbnailBmp;
	}

	public int getThemeType()
	{
		return themeType;
	}

	public void setThemeType(int status)
	{
		this.themeType |= status;
	}

	public boolean isEnabled(int status)
	{
		return ((themeType & status) == status);
	}

	public String getMetadata()
	{
		return metadata;
	}

	public void setMetadata(String metadata)
	{
		this.metadata = metadata;
	}

	public int getAssetDownloadStatus()
	{
		return assetDownloadStatus;
	}

	public void setAssetDownloadStatus(int assetDownloadStatus)
	{
		this.assetDownloadStatus |= assetDownloadStatus;
	}

	public void overrideAssetDownloadStatus(int assetDownloadStatus)
	{
		this.assetDownloadStatus = assetDownloadStatus;
	}

	public boolean isAssetDownloaded(int assetStatus)
	{
		return ((this.assetDownloadStatus & assetStatus) > 0);
	}
}
