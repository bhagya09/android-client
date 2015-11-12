package com.bsb.hike.cropimage;

import android.os.Parcel;
import android.os.Parcelable;

public class CropCompression implements Parcelable
{
	private int mWidth;

	private int mHeight;

	private int mQuality;

	public CropCompression()
	{

	}

	public CropCompression(Parcel in)
	{
		mWidth = in.readInt();
		mHeight = in.readInt();
		mQuality = in.readInt();
	}

	public CropCompression maxWidth(int argWidth)
	{
		mWidth = argWidth;
		return this;
	}

	public CropCompression maxHeight(int argHeight)
	{
		mHeight = argHeight;
		return this;
	}

	public CropCompression quality(int argQuality)
	{
		mQuality = argQuality;
		return this;
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeInt(mWidth);
		dest.writeInt(mHeight);
		dest.writeInt(mQuality);
	}
	
	public int getWidth()
	{
		return mWidth;
	}

	public void setWidth(int argWidth)
	{
		this.mWidth = argWidth;
	}

	public int getHeight()
	{
		return mHeight;
	}

	public void setHeight(int argHeight)
	{
		this.mHeight = argHeight;
	}

	public int getQuality()
	{
		return mQuality > 0 ? mQuality : 70;
	}

	public void setQuality(int argQuality)
	{
		this.mQuality = argQuality;
	}

	public static final Parcelable.Creator<CropCompression> CREATOR = new Parcelable.Creator<CropCompression>()
	{
		public CropCompression createFromParcel(Parcel in)
		{
			return new CropCompression(in);
		}

		public CropCompression[] newArray(int size)
		{
			return new CropCompression[size];
		}
	};

}
