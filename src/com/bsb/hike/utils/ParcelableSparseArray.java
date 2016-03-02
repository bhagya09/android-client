package com.bsb.hike.utils;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

//https://gist.github.com/kaushikgopal/9eea148a2188dc58fe37
public class ParcelableSparseArray extends SparseArray<Object> implements Parcelable
{

	public static final Parcelable.Creator<ParcelableSparseArray> CREATOR = new Creator<ParcelableSparseArray>()
	{

		@Override
		public ParcelableSparseArray createFromParcel(Parcel source)
		{
			return new ParcelableSparseArray(source);
		}

		@Override
		public ParcelableSparseArray[] newArray(int size)
		{
			return new ParcelableSparseArray[size];
		}
	};

	public ParcelableSparseArray()
	{
		super();
	}

	// -----------------------------------------------------------------------
	// Parcelable implementation

	private ParcelableSparseArray(Parcel in)
	{
		SparseArray<Object> sparseArray = in.readSparseArray(ParcelableSparseArray.class.getClassLoader());
		for (int i = 0; i < sparseArray.size(); i++)
		{
			int key = sparseArray.keyAt(i);
			// get the object by the key.
			Object obj = sparseArray.get(key);

			super.put(key, obj);
		}
	}

	public Object[] values()
	{
		Object[] values = new Object[size()];

		for (int i = 0; i < size(); i++)
		{
			values[i] = valueAt(i);
		}

		return values;
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int _)
	{
		out.writeSparseArray((SparseArray<Object>) this);
	}

	public String get(int key)
	{
		return (String) super.get(key);
	}
}
