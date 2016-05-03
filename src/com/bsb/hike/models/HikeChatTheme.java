package com.bsb.hike.models;

import android.graphics.Bitmap;

import com.bsb.hike.chatthemes.ChatThemeManager;
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

	private int themeType = 0;

	private String[] assets;

	private String metadata;

	public HikeChatTheme()
	{
		assets = new String[HikeChatThemeConstants.ASSET_INDEX_COUNT];
	}

	public boolean isVisible()
	{
		return themeVisibilityStatus;
	}

	public void setVisibilityStatus(boolean status)
	{
		this.themeVisibilityStatus = status;
	}

	public void setAsset(byte type, String assetId)
	{
		this.assets[type] = assetId;
	}

	public String getAssetId(byte type)
	{
		return assets[type];
	}

	public String[] getAssets()
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

	// if a theme is tiled, the first bit of themeType is set
	public boolean isTiled()
	{
		return (themeType & HikeChatThemeConstants.THEME_TYPE_TILED) == HikeChatThemeConstants.THEME_TYPE_TILED;
	}

	//if a theme is animated, the second bit of themeType is set
	public boolean isAnimated()
	{
		return (themeType & HikeChatThemeConstants.THEME_TYPE_ANIMATED) == HikeChatThemeConstants.THEME_TYPE_ANIMATED;
	}

	public boolean isCustomTheme()
	{
		return (themeType & HikeChatThemeConstants.THEME_TYPE_CUSTOM) == HikeChatThemeConstants.THEME_TYPE_CUSTOM;
	}
}
