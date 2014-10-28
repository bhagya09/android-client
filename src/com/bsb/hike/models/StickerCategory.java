package com.bsb.hike.models;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

public class StickerCategory implements Serializable, Comparable<StickerCategory>
{

	private String categoryId;

	private boolean updateAvailable;

	private String categoryName;
	
	private boolean isVisible;
	
	private boolean isCustom;
	
	private boolean isAdded;
	
	private int catIndex;
	
	private String metadata;
	
	private int totalStickers;
	
	private int timeStamp;
	
	public static final int NONE = 0;
	
	public static final int UPDATE = 1;
	
	public static final int DOWNLOADING = 2;
	
	public static final int RETRY = 3;
	
	public static final int DONE = 4;
	
	private int state;

	public StickerCategory(String categoryId, String categoryName, boolean updateAvailable, boolean isVisible, boolean isCustom, boolean isAdded,
			int catIndex, String metadata, int totalStickers, int timeStamp)
	{
		this.categoryId = categoryId;
		this.updateAvailable = updateAvailable;
		this.categoryName = categoryName;
		this.isVisible = isVisible;
		this.isCustom = isCustom;
		this.isAdded = isAdded;
		this.catIndex = catIndex;
		this.metadata = metadata;
		this.totalStickers = totalStickers;
		this.timeStamp = timeStamp;
		this.state = updateAvailable ? UPDATE : NONE;
	}

	// this is mostly used for recents stickers only
	public StickerCategory(String category)
	{
		this.categoryId = category;
		this.updateAvailable = false;
		this.state = NONE;
	}

	public StickerCategory()
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

	public boolean isUpdateAvailable()
	{
		return updateAvailable;
	}

	public void setUpdateAvailable(boolean updateAvailable)
	{
		if (updateAvailable)
		{
			setState(UPDATE);
		}
		this.updateAvailable = updateAvailable;
	}

	public String getCategoryName()
	{
		return categoryName;
	}

	public void setCategoryName(String categoryName)
	{
		this.categoryName = categoryName;
	}

	public boolean isVisible()
	{
		return isVisible;
	}

	public void setVisible(boolean isVisible)
	{
		this.isVisible = isVisible;
	}

	public boolean isCustom()
	{
		return isCustom;
	}

	public void setCustom(boolean isCustom)
	{
		this.isCustom = isCustom;
	}

	public boolean isAdded()
	{
		return isAdded;
	}

	public void setAdded(boolean isAdded)
	{
		this.isAdded = isAdded;
	}

	public int getCategoryIndex()
	{
		return catIndex;
	}

	public void setCategoryIndex(int catIndex)
	{
		this.catIndex = catIndex;
	}

	public String getMetadata()
	{
		return metadata;
	}

	public void setMetadata(String metadata)
	{
		this.metadata = metadata;
	}

	public int getTotalStickers()
	{
		return totalStickers;
	}

	public void setTotalStickers(int totalStickers)
	{
		this.totalStickers = totalStickers;
	}

	public int getTimeStamp()
	{
		return timeStamp;
	}

	public void setTimeStamp(int timeStamp)
	{
		this.timeStamp = timeStamp;
	}
	
	public void setState(int state)
	{
		this.state = state;
	}
	
	public int getState()
	{
		return state;
	}
	
	public List<Sticker> getStickerList(Context context)
	{
		final List<Sticker> stickersList;
		if (isCustom())
		{
			return ((CustomStickerCategory) this).getStickerList(context);
		}
		else
		{

			long t1 = System.currentTimeMillis();
			stickersList = new ArrayList<Sticker>();

			String categoryDirPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(context, getCategoryId());

			if (categoryDirPath != null)
			{
				File categoryDir = new File(categoryDirPath + HikeConstants.SMALL_STICKER_ROOT);

				if (categoryDir.exists())
				{
					String[] stickerIds = categoryDir.list(StickerManager.getInstance().stickerFileFilter);
					for (String stickerId : stickerIds)
					{
						Sticker s = new Sticker(this, stickerId);
						stickersList.add(s);
					}
				}
			}
			Collections.sort(stickersList);
			long t2 = System.currentTimeMillis();
			Logger.d(getClass().getSimpleName(), "Time to sort category : " + getCategoryId() + " in ms : " + (t2 - t1));
		}
		return stickersList;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((categoryId == null) ? 0 : categoryId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StickerCategory other = (StickerCategory) obj;
		if (categoryId == null)
		{
			if (other.categoryId != null)
				return false;
		}
		else if (!categoryId.equals(other.categoryId))
		{
			return false;
		}
		return true;
	}

	public void serializeObj(ObjectOutputStream out) throws IOException
	{
		out.writeUTF(categoryId);
		out.writeBoolean(updateAvailable);
		// After removing reachedEnd variable, we need to write dummy
		// boolean, just to ensure backward/forward compatibility
		out.writeBoolean(true);
	}

	public void deSerializeObj(ObjectInputStream in) throws OptionalDataException, ClassNotFoundException, IOException
	{
		categoryId = in.readUTF();
		updateAvailable = in.readBoolean();
		//ignoring this varialbe after reading just to ensure backward compatibility
		in.readBoolean();
	}

	@Override
	public int compareTo(StickerCategory another)
	{
		if (this.equals(another))
		{
			return 0;
		}

		if (another == null)
		{
			return -1;
		}

		return this.catIndex < another.getCategoryIndex() ? -1 : 1; 
	}
}