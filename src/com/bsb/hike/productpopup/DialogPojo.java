package com.bsb.hike.productpopup;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * 
 * @author himanshu
 * 
 * This class has the information that the HikeDialogFragment may need to function properly.
 *
 */
public class DialogPojo implements Parcelable
{

	boolean isFullScreen;

	int heigth;

	String html;
	
	Object data;

	
	/**
	 * @param isFullScreen
	 * @param heigth
	 * @param html
	 */
	public DialogPojo(boolean isFullScreen, int heigth, String html)
	{
		this(isFullScreen,heigth,html,null);
	}

	
	
	/**
	 * @param isFullScreen
	 * @param height
	 * @param html
	 * @param data
	 */
	public DialogPojo(boolean isFullScreen, int heigth, String html, Object data)
	{
		this.isFullScreen = isFullScreen;
		this.heigth = heigth;
		this.html = html;
		this.data = data;
	}



	/**
	 * @return the isFullScreen
	 */
	public boolean isFullScreen()
	{
		return isFullScreen;
	}

	/**
	 * @return the heigth
	 */
	public int getHeight()
	{
		return heigth;
	}

	/**
	 * @return the html
	 */
	public String getFormedData()
	{
		return html;
	}
	
	/**
	 * @return the data
	 */
	public Object getData()
	{
		return data;
	}


	/**
	 * @param data the data to set
	 */
	public void setData(Object data)
	{
		this.data = data;
	}



	@Override
	public int describeContents()
	{
		// TODO Auto-generated method stub
		return 0;
	}



	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeValue(data);
		dest.writeByte((byte) (isFullScreen ? 1 : 0));
		dest.writeString(html);
		dest.writeInt(heigth);
	}

	public static final Parcelable.Creator<DialogPojo> CREATOR = new Parcelable.Creator<DialogPojo>()
	{
		public DialogPojo createFromParcel(Parcel in)
		{
			Object data = in.readValue(Object.class.getClassLoader());
			
			boolean isFullScreen = in.readByte() != 0;

			String html = in.readString();

			int heigth = in.readInt();

			return new DialogPojo(isFullScreen, heigth, html, data);
		}

		public DialogPojo[] newArray(int size)
		{
			return new DialogPojo[size];
		}
	};
	
}
