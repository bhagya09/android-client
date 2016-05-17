package com.bsb.hike.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.utils.StickerManager;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Sticker implements Serializable, Comparable<Sticker>, Parcelable
{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private String stickerId;

	private StickerCategory category;

	private String categoryId;

	private String smallStickerPath;

	private String largeStickerPath;

	private String miniStickerPath;

	private int width;

	private int height;

	public Sticker(StickerCategory category, String stickerId)
	{
		this.category = category;
		this.stickerId = stickerId;
		this.categoryId = category.getCategoryId();
	}

	public Sticker(String categoryId, String stickerId)
	{
		this.stickerId = stickerId;
		this.category = StickerManager.getInstance().getCategoryForId(categoryId);
		this.categoryId = categoryId;
	}

	public Sticker()
	{

	}

	public void setCategoryId(String categoryId)
	{
		this.categoryId = categoryId;
	}

	public void setStickerId(String stickerId)
	{
		this.stickerId = stickerId;
	}

	public void setCategory(StickerCategory category)
	{
		this.category = category;
	}

	public String getCategoryId()
	{
		return categoryId;
	}

	public boolean isStickerAvailable()
	{
		return !getLargeStickerPath().endsWith(getStickerCode());
	}

    public boolean isStickerFileAvailable()
    {
        File f = new File(getLargeStickerPath());
        return f.exists();
    }

    public boolean isStickerOfflineFileAvailable()
    {
		String path = OfflineUtils.getOfflineStkPath(categoryId, stickerId);

		if (TextUtils.isEmpty(path))
		{
			return false;
		}

		File f = new File(path);
		return f.exists();
    }

	/**
	 * if sticker small image does'nt exist then its disabled
	 *
	 * @param sticker
	 * @return
	 */
	public boolean isDisabled()
	{
		File f = new File(this.getSmallStickerPath());
		return !f.exists();
	}

	public String getStickerId()
	{
		return stickerId;
	}

	public StickerCategory getCategory()
	{
		return category;
	}

	public String getLargeStickerPath()
	{
		return !TextUtils.isEmpty(largeStickerPath) ? largeStickerPath : loadStickerPath(true);
	}

    public String getLargeStickerPath(boolean getCurrent)
    {
        if(getCurrent)
        {
            return largeStickerPath;
        }

        return !TextUtils.isEmpty(largeStickerPath) ? largeStickerPath : loadStickerPath(true);
    }

	public void setLargeStickerPath(String largeStickerPath)
	{
		if (!TextUtils.isEmpty(largeStickerPath))
		{
			this.largeStickerPath = largeStickerPath;
		}
	}

	public String getSmallStickerPath()
	{
		return !TextUtils.isEmpty(smallStickerPath) ? smallStickerPath : loadStickerPath(false);
	}

	public void setSmallStickerPath(String smallStickerPath)
	{
		if (!TextUtils.isEmpty(smallStickerPath))
		{
			this.smallStickerPath = smallStickerPath;
		}
	}

	public String getMiniStickerPath()
	{

		if(!TextUtils.isEmpty(miniStickerPath))
		{
			return miniStickerPath;
		}

        StickerManager.getInstance().generateMiniStickerPath(this);

		return miniStickerPath;

	}

	public String getStickerCode()
	{
		return (categoryId + HikeConstants.DELIMETER + stickerId);
	}

	/**
	 * Don't use this. use {@link #getLargeStickerPath() method}
	 * @return
	 */
	public String getLargeStickerFilePath()
	{
		return StickerManager.getInstance().getStickerCategoryDirPath(categoryId) + HikeConstants.LARGE_STICKER_ROOT + "/" + stickerId;
	}

	/**
	 * Don't use this. use {@link #getSmallStickerPath() method}
	 * @return
	 */
	public String getSmallStickerFilePath()
	{
		return StickerManager.getInstance().getStickerCategoryDirPath(categoryId) + HikeConstants.SMALL_STICKER_ROOT + "/" + stickerId;
	}

	private String loadStickerPath(boolean largeSticker)
	{
		loadStickerFromDb();
		if (largeSticker)
		{
			return largeStickerPath != null ? largeStickerPath : HikeConstants.LARGE_STICKER_ROOT + HikeConstants.DELIMETER + getStickerCode();
		}
		else
		{
			return smallStickerPath != null ? smallStickerPath : HikeConstants.SMALL_STICKER_ROOT + HikeConstants.DELIMETER + getStickerCode();
		}
	}

	private void loadStickerFromDb()
	{
		HikeConversationsDatabase.getInstance().getStickerFromStickerTable(this);
	}

	@Override
	public int compareTo(Sticker rhs)
	{
		final int BEFORE = -1;
		final int EQUAL = 0;
		final int AFTER = 1;
		if (this == rhs)
			return EQUAL;

		if (rhs == null)
			throw new NullPointerException();

		if (TextUtils.isEmpty(this.stickerId) && TextUtils.isEmpty(rhs.stickerId))
		{
			return (EQUAL);
		}
		else if (TextUtils.isEmpty(this.stickerId))
		{
			return AFTER;
		}
		else if (TextUtils.isEmpty(rhs.stickerId))
		{
			return BEFORE;
		}
		return (this.stickerId.toLowerCase().compareTo(rhs.stickerId.toLowerCase()));
	}

	/* Need to override equals and hashcode inorder to use them in recentStickers linkedhashset */
	@Override
	public boolean equals(Object object)
	{
		if (object == null)
			return false;
		if (object == this)
			return true;
		if (!(object instanceof Sticker))
			return false;
		Sticker st = (Sticker) object;
		return ((this.stickerId.equals(st.getStickerId())) && (this.categoryId.equals(st.getCategoryId())));
	}

	@Override
	public int hashCode()
	{
		int hash = 3;
		if (!TextUtils.isEmpty(categoryId))
			hash = 7 * hash + this.categoryId.hashCode();
		hash = 7 * hash + this.stickerId.hashCode();
		return hash;
	}

	public void serializeObj(ObjectOutputStream out) throws IOException
	{
		// After removing reachedEnd variable, we need to write dummy
		// boolean, just to ensure backward/forward compatibility
		out.writeInt(0);
		out.writeUTF(stickerId);
		StickerCategory cat = category;
		if (cat == null)
		{
			cat = new StickerCategory.Builder()
					.setCategoryId(categoryId)
					.build();
		}
		cat.serializeObj(out);
	}

	public void deSerializeObj(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		// ignoring this variable after reading just to ensure backward compatibility
		in.readInt();
		stickerId = in.readUTF();
		StickerCategory tempcategory = new StickerCategory();
		tempcategory.deSerializeObj(in);
		categoryId = tempcategory.getCategoryId();
		category = StickerManager.getInstance().getCategoryForId(categoryId);
	}

	public void setStickerData(int stickerIndex, String stickerId, StickerCategory category)
	{
		this.stickerId = stickerId;
		this.category = category;
	}

	public int getHeight()
	{
		return height > 0 ? height : StickerManager.getStickerSize();
	}

	public void setHeight(int height)
	{
		this.height = height;
	}

	public int getWidth()
	{
		return width > 0 ? width : StickerManager.getStickerSize();
	}

	public void setWidth(int width)
	{
		this.width = width;
	}

	public void clear()
	{
		this.stickerId = null;
		this.category = null;
		this.categoryId = null;
	}

	public Sticker(Parcel in)
	{
		this.stickerId = in.readString();
		this.categoryId = in.readString();
		this.category = (StickerCategory) in.readSerializable();
		this.largeStickerPath = in.readString();
		this.smallStickerPath = in.readString();
		this.width = in.readInt();
		this.height = in.readInt();
		/* Default value */
	}

	public static final Parcelable.Creator<Sticker> CREATOR = new Parcelable.Creator<Sticker>()
	{

		@Override
		public Sticker createFromParcel(Parcel source)
		{
			return new Sticker(source);
		}

		@Override
		public Sticker[] newArray(int size)
		{
			return new Sticker[size];
		}
	};

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(stickerId);
		dest.writeString(categoryId);
		dest.writeSerializable(category);
		dest.writeString(largeStickerPath);
		dest.writeString(smallStickerPath);
		dest.writeInt(width);
		dest.writeInt(height);
	}

	@Override
	public String toString()
	{
		return categoryId + ":" + stickerId;
	}

	public void setMiniStickerPath(String miniStickerPath)
	{
		this.miniStickerPath = miniStickerPath;
	}

    public boolean isMiniStickerAvailable()
    {
       return HikeMessengerApp.getDiskCache().isKeyExists(getMiniStickerPath());
    }
    
    public StickerConstants.StickerType getStickerType()
    {
		return isStickerAvailable() ? StickerConstants.StickerType.LARGE : StickerConstants.StickerType.MINI;
    }
}
