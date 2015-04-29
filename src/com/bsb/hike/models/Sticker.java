package com.bsb.hike.models;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.content.Context;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.StickerManager;

public class Sticker implements Serializable, Comparable<Sticker>
{

	private String stickerId;

	private StickerCategory category;
	
	private String categoryId;

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

	public String getCategoryId()
	{
		return categoryId;
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

	public boolean isUnknownSticker()
	{
		return category == null;
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

	public String getStickerPath(Context context)
	{
		String rootPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(categoryId);
		if (rootPath == null)
		{
			return null;
		}
		return rootPath + HikeConstants.LARGE_STICKER_ROOT + "/" + stickerId;
	}

	public String getSmallStickerPath()
	{
		return StickerManager.getInstance().getStickerDirectoryForCategoryId(categoryId) + HikeConstants.SMALL_STICKER_ROOT + "/" + stickerId;
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
		if(cat == null)
		{
			cat = new StickerCategory(this.categoryId);
		}
		cat.serializeObj(out);
	}

	public void deSerializeObj(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		//ignoring this varialbe after reading just to ensure backward compatibility
		in.readInt();
		stickerId = in.readUTF();
		StickerCategory tempcategory = new StickerCategory();
		tempcategory.deSerializeObj(in);
		categoryId = tempcategory.getCategoryId();
		this.category = StickerManager.getInstance().getCategoryForId(categoryId);
	}

	public void setStickerData(int stickerIndex,String stickerId,StickerCategory category){
		this.stickerId = stickerId;
		this.category = category;
	}
}
