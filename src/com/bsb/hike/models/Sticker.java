package com.bsb.hike.models;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

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
	
	private int mStickerAvailabilityStatus = HikeStickerSearchBaseConstants.DEFAULT_AVAILABILITY_STATUS;

	private boolean offlineStatus;

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

	public Sticker(Parcel in)
	{
		this.stickerId = in.readString();
		this.categoryId = in.readString();
		this.category = (StickerCategory) in.readSerializable();
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

	public void setStickerAvailability()
	{
		boolean resultlarge,resultMini;
		String path = getLargeStickerPath();

		if (!Utils.isBlank(path))
		{
			File file = new File(path);
			resultlarge = file.isFile() && file.exists();
		}
		else
		{
			resultlarge = false;
		}

		path = getMiniStickerPath();

		resultMini = HikeMessengerApp.getDiskCache().isKeyExists(path);

		if(resultlarge && resultMini)
		{
			mStickerAvailabilityStatus = HikeStickerSearchBaseConstants.LARGE_AND_MINI_STICKERS_AVAILABLE;
		}
		else if(resultlarge)
		{
			mStickerAvailabilityStatus = HikeStickerSearchBaseConstants.LARGE_STICKER_AVAILABLE_ONLY;
		}
		else if(resultMini)
		{
			mStickerAvailabilityStatus = HikeStickerSearchBaseConstants.MINI_STICKER_AVAILABLE_ONLY;
		}
		else
		{
			mStickerAvailabilityStatus = HikeStickerSearchBaseConstants.STICKER_NOT_AVAILABLE;
		}

		offlineStatus = getStickerOfflinePath() != null && new File(getStickerOfflinePath()).exists();

	}

	public String getCategoryId()
	{
		return categoryId;
	}

	public boolean isUnknownSticker()
	{
		return this.category == null;
	}

	public void verifyStickerAvailabilityStatus()
	{
		if(mStickerAvailabilityStatus == HikeStickerSearchBaseConstants.DEFAULT_AVAILABILITY_STATUS)
		{
			setStickerAvailability();
		}
	}

	public boolean isStickerAvailable()
	{
		verifyStickerAvailabilityStatus();
		return ((mStickerAvailabilityStatus == HikeStickerSearchBaseConstants.LARGE_STICKER_AVAILABLE_ONLY) || (mStickerAvailabilityStatus == HikeStickerSearchBaseConstants.MINI_STICKER_AVAILABLE_ONLY) || (mStickerAvailabilityStatus == HikeStickerSearchBaseConstants.LARGE_AND_MINI_STICKERS_AVAILABLE));
	}

	public boolean isFullStickerAvailable()
	{
		verifyStickerAvailabilityStatus();
		return ((mStickerAvailabilityStatus == HikeStickerSearchBaseConstants.LARGE_STICKER_AVAILABLE_ONLY) || (mStickerAvailabilityStatus == HikeStickerSearchBaseConstants.LARGE_AND_MINI_STICKERS_AVAILABLE));
	}

	public boolean isMiniStickerAvailable()
	{
		verifyStickerAvailabilityStatus();
		return ((mStickerAvailabilityStatus == HikeStickerSearchBaseConstants.MINI_STICKER_AVAILABLE_ONLY) || (mStickerAvailabilityStatus == HikeStickerSearchBaseConstants.LARGE_AND_MINI_STICKERS_AVAILABLE));
	}

	 /* Call this method, only if one needs current download-status of sticker */
	public int getStickerCurrentAvailabilityStatus()
	{
		verifyStickerAvailabilityStatus();
		return mStickerAvailabilityStatus;
	}

	public boolean isStickerOffline()
	{
		verifyStickerAvailabilityStatus();
		return offlineStatus;
	}

	/**
	 * if sticker small image does'nt exist then its disabled
	 * 
	 * @param sticker
	 * @return
	 */
	public boolean isDisabled(Sticker sticker, Context ctx)
	{
		File f = new File(sticker.getSmallStickerPath());
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
		String rootPath = StickerManager.getInstance().getStickerCategoryDirPath(categoryId);

		return (rootPath == null) ? null : (rootPath + HikeConstants.LARGE_STICKER_ROOT + File.separator + stickerId);
	}

	public String getSmallStickerPath()
	{
		return StickerManager.getInstance().getStickerCategoryDirPath(categoryId) + HikeConstants.SMALL_STICKER_ROOT + "/" + stickerId;
	}

	public String getMiniStickerPath()
	{
		return StickerSearchConstants.MINI_STICKER_KEY_CODE+":"+categoryId+":"+stickerId;
	}

	public String getStickerOfflinePath()
	{
		return OfflineUtils.getOfflineStkPath(categoryId, stickerId);
	}

	public String getCategoryPath()
	{
		return StickerManager.getInstance().getStickerCategoryDirPath(categoryId);
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
			cat = new StickerCategory(this.categoryId);
		}
		cat.serializeObj(out);
	}

	public void deSerializeObj(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		// ignoring this varialbe after reading just to ensure backward compatibility
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

	public void setStickerData(String stickerId, String categoryId)
	{
		this.stickerId = stickerId;
		this.categoryId = categoryId;
	}

	public boolean isValidStickerData()
	{
		return (TextUtils.isEmpty(categoryId) && TextUtils.isEmpty(stickerId));
	}

	public void clear()
	{
		this.stickerId = null;
		this.category = null;
		this.categoryId = null;
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
	}

	@Override
	public String toString() {
		return categoryId + ":" + stickerId;
	}
}
