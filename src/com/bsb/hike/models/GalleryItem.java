package com.bsb.hike.models;

import java.util.ArrayList;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.webkit.MimeTypeMap;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.Utils;

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
	public static <T> ArrayList<GalleryItem> getGalleryItemsFromFilepaths(ArrayList<T> filePaths)
	{
		ArrayList<String> mFilePaths = null;
		
		if(filePaths.get(0) instanceof Uri)
		{
			mFilePaths = new ArrayList<String>(filePaths.size());
			for(T path : filePaths)
			{
				mFilePaths.add(Utils.getRealPathFromUri((Uri)path,HikeMessengerApp.getInstance().getApplicationContext()));
			}
		}
		else if(filePaths.get(0) instanceof String)
		{
			mFilePaths = (ArrayList<String>) filePaths;
		}
		
		if(mFilePaths == null || mFilePaths.size() == 0)
		{
			return null;
		}
		
		ArrayList<GalleryItem> retItems = new ArrayList<GalleryItem>(filePaths.size());
		
		for(int i=0;i<mFilePaths.size();i++)
		{
			//Check file type since only images can be handled
			String filename = mFilePaths.get(i);
			String fileExtension = Utils.getFileExtension(filename);
			String fileType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
			HikeFileType hikeFileType = HikeFileType.fromString(fileType, false);
			if (hikeFileType.compareTo(HikeFileType.IMAGE) != 0)
			{
				return null;
			}
			
			retItems.add(new GalleryItem(i, null, "Custom_Item", filename, 0));

		}
		
		return retItems;
	}
	
}