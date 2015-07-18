package com.bsb.hike.models;

public class ImageViewerInfo
{

	public String mappedId;

	public String url;

	public boolean isStatusMessage;

	public boolean isDefaultImage;

	public String fileKey;

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
}
