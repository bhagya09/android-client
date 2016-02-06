package com.bsb.hike.models;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StickerCategory implements Serializable, Comparable<StickerCategory>
{
	public static final int NONE = 0;

	public static final int UPDATE = 1;

	public static final int DOWNLOADING = 2;

	public static final int RETRY = 3;

	public static final int DONE = 4;

	public static final int DONE_SHOP_SETTINGS = 5;

	private String categoryId;

	private String categoryName;

	private int categorySize;

	private String categoryDesc;

	private int totalStickers;

	private boolean updateAvailable;

	private boolean isVisible;

	private boolean isCustom;

	private boolean isAdded;

	private boolean isPreview;

	private boolean isDownloaded;

	private int catIndex;

	private List<Sticker> allStickers;

	private int state = -1;

	private int downloadedStickersCount = -1;

	protected StickerCategory (Init<?> builder)
	{
		this.categoryId = builder.categoryId;
		this.categoryName = builder.categoryName;
		this.categorySize = builder.categorySize;
		this.categoryDesc = builder.categoryDesc;
		this.totalStickers = builder.totalStickers;
		this.updateAvailable = builder.updateAvailable;
		this.isVisible = builder.isVisible;
		this.isCustom = builder.isCustom;
		this.isAdded = builder.isAdded;
		this.isPreview = builder.isPreview;
		this.isDownloaded = builder.isDownloaded;
		this.catIndex = builder.catIndex;
		this.allStickers = builder.allStickers;
		this.state = builder.state;
		ensureSaneDefaults();
	}

	private void ensureSaneDefaults()
	{
		if(categoryId == null)
		{
			throw new IllegalStateException("Category cannot be null");
		}
		if(state == -1)
		{
			state = isDownloaded ? (isMoreStickerAvailable() ? UPDATE : NONE) : NONE;
		}
	}

	protected static abstract class Init<S extends Init<S>>
	{
		private String categoryId;

		private String categoryName;

		private int categorySize;

		private String categoryDesc;

		private int totalStickers;

		private boolean updateAvailable;

		private boolean isVisible;

		private boolean isCustom;

		private boolean isAdded;

		private boolean isPreview;

		private boolean isDownloaded;

		private int catIndex;

		private List<Sticker> allStickers;

		private int state;

		protected abstract S self();

		public StickerCategory build()
	{
		return new StickerCategory(this);
	}

		public S setCategoryId(String categoryId) {
			this.categoryId = categoryId;
			return  self();
		}

		public S setCategoryName(String categoryName) {
			this.categoryName = categoryName;
			return self();
		}

		public S setCategorySize(int categorySize) {
			this.categorySize = categorySize;
			return self();
		}

		public S setCategoryDesc(String categoryDesc) {
			this.categoryDesc = categoryDesc;
			return self();
		}

		public S setTotalStickers(int totalStickers) {
			this.totalStickers = totalStickers;
			return self();
		}

		public S setUpdateAvailable(boolean updateAvailable) {
			this.updateAvailable = updateAvailable;
			return self();
		}

		public S setIsVisible(boolean isVisible) {
			this.isVisible = isVisible;
			return self();
		}

		public S setIsCustom(boolean isCustom) {
			this.isCustom = isCustom;
			return self();
		}

		public S setIsAdded(boolean isAdded) {
			this.isAdded = isAdded;
			return self();
		}

		public S setIsPreview(boolean isPreview) {
			this.isPreview = isPreview;
			return self();
		}

		public S setIsDownloaded(boolean isDownloaded) {
			this.isDownloaded = isDownloaded;
			return self();
		}

		public S setCatIndex(int catIndex) {
			this.catIndex = catIndex;
			return self();
		}

		public S setAllStickers(List<Sticker> allStickers) {
			this.allStickers = allStickers;
			return self();
		}

		public S setState(int state) {
			this.state = state;
			return self();
		}
	}

	public static class Builder extends Init<Builder>
	{
		@Override
		protected Builder self()
		{
			return this;
		}
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

	public boolean isPreview() {
		return isPreview;
	}

	public void setIsPreview(boolean isPreview) {
		this.isPreview = isPreview;
	}

	public boolean isDownloaded() {
		return isDownloaded;
	}

	public void setIsDownloaded(boolean isDownloaded) {
		this.isDownloaded = isDownloaded;
	}

	public int getCategoryIndex()
	{
		return catIndex;
	}

	public void setCategoryIndex(int catIndex)
	{
		this.catIndex = catIndex;
	}

	public int getCategorySize()
	{
		return categorySize;
	}

	public void setCategorySize(int categorySize)
	{
		this.categorySize = categorySize;
	}

	public int getTotalStickers()
	{
		return totalStickers;
	}

	public void setTotalStickers(int totalStickers)
	{
		this.totalStickers = totalStickers;
	}

	public void setState(int state)
	{
		this.state = state;
	}
	
	public int getState()
	{
		return state;
	}
	
	public String getDescription()
	{
		return this.categoryDesc;
	}
	
	public void setDescription(String description)
	{
		this.categoryDesc = description;
	}

	public void setAllStickers(List<Sticker> allStickers) {
		this.allStickers = allStickers;
	}

	public List<Sticker> getAllStickers()
	{
		if(allStickers == null)
		{
			allStickers = StickerManager.getInstance().getStickerListFromDb(categoryId);
		}
		return allStickers;
	}

	public List<Sticker> getStickerList()
	{
		final List<Sticker> stickersList;
		if (isCustom())
		{
			return ((CustomStickerCategory) this).getStickerList();
		}
		else
		{

			long t1 = System.currentTimeMillis();
			stickersList = new ArrayList<Sticker>();
			
			String[] stickerIds = getStickerFiles();
			if(stickerIds != null)
			{
				for (String stickerId : stickerIds)
				{
					Sticker s = new Sticker(this, stickerId);
					stickersList.add(s);
				}
				setDownloadedStickersCount(stickerIds.length);
			}
			else
			{
				setDownloadedStickersCount(0);
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
	
	/**
	 * Checks for the count of allStickers from the allStickers folder for this category. Returns true if the count is < totalStickers
	 * @return  
	 */
	public boolean isMoreStickerAvailable()
	{
		if(getDownloadedStickersCount() == 0)
		{
			return false;
		}
		return getDownloadedStickersCount() < getTotalStickers();
	}
	
	/**
	 * Returns a list of Sticker files for a given sticker category
	 * @return
	 */
	private String[] getStickerFiles()
	{
		String categoryDirPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(this.categoryId);
		if (categoryDirPath != null)
		{
			File categoryDir = new File(categoryDirPath + HikeConstants.SMALL_STICKER_ROOT);

			if (categoryDir.exists())
			{
				if(categoryDir.list() != null)
				{
					String[] list = categoryDir.list(StickerManager.getInstance().stickerFileFilter);
					return list;
				}
			}
		}
		return null;
	}
	
	public int getMoreStickerCount()
	{
		return this.totalStickers - getDownloadedStickersCount();
	}
	
	public int getDownloadedStickersCount()
	{
		if(downloadedStickersCount == -1)
		{
			updateDownloadedStickersCount();
		}
		return downloadedStickersCount;
	}
	
	public void updateDownloadedStickersCount()
	{
		String[] stickerFiles = getStickerFiles();
		if(stickerFiles != null)
			setDownloadedStickersCount(stickerFiles.length);
		else
			setDownloadedStickersCount(0);
	}
	
	public void setDownloadedStickersCount(int count)
	{
		this.downloadedStickersCount = count;
	}
}