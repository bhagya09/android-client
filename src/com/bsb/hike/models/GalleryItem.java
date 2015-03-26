package com.bsb.hike.models;

import android.os.Parcel;
import android.os.Parcelable;

public class GalleryItem implements Parcelable
{
	private long id;

	private String bucketId;

	private String name;

	private String filePath;

	private int bucketCount;

	public GalleryItem(long id, String bucketId, String name, String filePath, int bucketCount)
	{
		this.id = id;
		this.bucketId = bucketId;
		this.name = name;
		this.filePath = filePath;
		this.bucketCount = bucketCount;
	}

	public GalleryItem(Parcel source)
	{
		this.id = source.readLong();
		this.bucketId = source.readString();
		this.name = source.readString();
		this.filePath = source.readString();
	}

	public long getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public String getBucketId()
	{
		return bucketId;
	}

	public String getFilePath()
	{
		return filePath;
	}

	public int getBucketCount()
	{
		return bucketCount;
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeLong(id);
		dest.writeString(bucketId);
		dest.writeString(name);
		dest.writeString(filePath);
	}

	public static final Creator<GalleryItem> CREATOR = new Creator<GalleryItem>()
	{
		@Override
		public GalleryItem[] newArray(int size)
		{
			return new GalleryItem[size];
		}

		@Override
		public GalleryItem createFromParcel(Parcel source)
		{
			return new GalleryItem(source);
		}
	};
}