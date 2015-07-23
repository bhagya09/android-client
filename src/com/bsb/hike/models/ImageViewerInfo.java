package com.bsb.hike.models;

import android.os.Bundle;

public class ImageViewerInfo
{

	public String mappedId;

	public String url;

	public boolean isStatusMessage;

	public boolean isDefaultImage;

	public String fileKey;

	private Bundle mBundle;

	public ImageViewerInfo(String mappedId, String url, boolean isStatusMessage)
	{
		this(mappedId, url, isStatusMessage, false);
	}

	public ImageViewerInfo(String mappedId, String url, boolean isStatusMessage, boolean isDefaultImage)
	{
		this(mappedId, url, isStatusMessage, isDefaultImage, null);
	}

	public ImageViewerInfo(String mappedId, String url, boolean isStatusMessage, boolean isDefaultImage, String fileKey)
	{
		this.mappedId = mappedId;
		this.url = url;
		this.isStatusMessage = isStatusMessage;
		this.fileKey = fileKey;
		this.isDefaultImage = isDefaultImage;
	}
	
	public void setBundle(Bundle argBundle)
	{
		mBundle = argBundle;
	}
	
	public Bundle getBundle()
	{
		return mBundle;
	}
}
