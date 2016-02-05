package com.bsb.hike.models;

import java.util.ArrayList;
import java.util.List;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.Utils;

public class GalleryItem implements Parcelable
{

	public static final byte DEFAULT_FILE = 1;

	public static final byte CUSTOM = 2;
	
	public static final long CAMERA_TILE_ID = -11L;
	
	public static final String CUSTOM_TILE_NAME = "Custom_Item";

	private long id;

	private String bucketId;

	private String name;

	private String filePath;

	private int bucketCount;

	private String layoutIDName;

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

	public GalleryItem(long id, String name, String layoutIDName, int bucketCount)
	{
		this.id = id;
		this.name = name;
		this.layoutIDName = layoutIDName;
		this.bucketCount = bucketCount;
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
		this.bucketCount = source.readInt();
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
		dest.writeInt(bucketCount);
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GalleryItem other = (GalleryItem) obj;
		if (id != other.id)
			return false;
		return true;
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
	
	@SuppressWarnings("unchecked")
	public static <T> ArrayList<GalleryItem> getGalleryItemsFromFilepaths(List<T> filePaths)
	{
		ArrayList<String> retFilePaths = null;
		
		if(filePaths.isEmpty())
		{
			return null;
		}
		
		if(filePaths.get(0) instanceof Uri)
		{
			retFilePaths = new ArrayList<String>();
			for(T path : filePaths)
			{
				String tempPath = Utils.getAbsolutePathFromUri((Uri)path,HikeMessengerApp.getInstance().getApplicationContext(),true);
				if(tempPath == null)
				{
					continue;
				}
				retFilePaths.add(tempPath);
			}
		}
		else if(filePaths.get(0) instanceof String)
		{
			retFilePaths = (ArrayList<String>) filePaths;
		}
		
		if(retFilePaths.isEmpty())
		{
			return null;
		}
		
		ArrayList<GalleryItem> retItems = new ArrayList<GalleryItem>(filePaths.size());
		
		for(int i=0;i<retFilePaths.size();i++)
		{
			//Check file type since only images can be handled
			//special handling for webp images since some devices cant extract their mime type
			if (!Utils.getFileExtension(retFilePaths.get(i)).equalsIgnoreCase("webp") && HikeFileType.fromFilePath(retFilePaths.get(i), false).compareTo(HikeFileType.IMAGE) != 0)
			{
				return null;
			}
			
			retItems.add(new GalleryItem(i, null, CUSTOM_TILE_NAME, retFilePaths.get(i), 0));

		}
		
		return retItems;
	}
	
}