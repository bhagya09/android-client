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
	
	private int drawableId;
	
	public GalleryItem(long id, String bucketId, String name, String filePath, int bucketCount)
	{
		this.id = id;
		this.bucketId = bucketId;
		this.name = name;
		this.filePath = filePath;
		this.bucketCount = bucketCount;
	}
	
	public GalleryItem(long id, String name, int drawableId, int bucketCount)
	{
		this.id = id;
		this.name = name;
		this.drawableId = drawableId;
		this.bucketCount = bucketCount;
	}

	public GalleryItem(Parcel source)
	{
		this.id = source.readLong();
		this.bucketId = source.readString();
		this.name = source.readString();
		this.filePath = source.readString();
		this.drawableId = source.readInt();
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
	
	public int getDrawableId()
	{
		return drawableId;
	}
	
	public void setFilePath(String filePath)
	{
		this.filePath = filePath;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeLong(id);
		dest.writeString(bucketId);
		dest.writeString(name);
		dest.writeString(filePath);
		dest.writeInt(drawableId);
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