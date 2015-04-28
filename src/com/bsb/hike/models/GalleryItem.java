package com.bsb.hike.models;

import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable;

public class GalleryItem implements Parcelable
{

	public static final byte DEFAULT_FILE = 1;

	public static final byte CUSTOM = 2;
	
	public static final long CAMERA_TILE_ID = -11L;

	private long id;

	private String bucketId;

	private String name;

	private String filePath;

	private int bucketCount;

	private String layoutIDName;

	private PendingIntent pIntent;

	private byte type;

	public GalleryItem(long id, String bucketId, String name, String filePath, int bucketCount)
	{
		this.id = id;
		this.bucketId = bucketId;
		this.name = name;
		this.filePath = filePath;
		this.bucketCount = bucketCount;
		type = DEFAULT_FILE;
	}

	public GalleryItem(long id, String name, String layoutIDName, int bucketCount, PendingIntent intent)
	{
		this.name = name;
		this.layoutIDName = layoutIDName;
		this.bucketCount = bucketCount;
		this.pIntent = intent;
		type = CUSTOM;
	}

	public GalleryItem(Parcel source)
	{
		this.type = source.readByte();
		this.id = source.readLong();
		this.bucketId = source.readString();
		this.name = source.readString();
		this.filePath = source.readString();
		this.layoutIDName = source.readString();
		this.pIntent = source.readParcelable(PendingIntent.class.getClassLoader());
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeByte(type);
		dest.writeLong(id);
		dest.writeString(bucketId);
		dest.writeString(name);
		dest.writeString(filePath);
		dest.writeString(layoutIDName);
		dest.writeParcelable(pIntent, flags);
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

	public String getLayoutIDName()
	{
		return layoutIDName;
	}

	public void setFilePath(String filePath)
	{
		this.filePath = filePath;
	}
	
	public byte getType()
	{
		return type;
	}

	public PendingIntent getPendingIntent()
	{
		return pIntent;
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