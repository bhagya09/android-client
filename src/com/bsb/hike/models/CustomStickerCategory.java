package com.bsb.hike.models;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CustomStickerCategory extends StickerCategory
{
	private Set<Sticker> stickerSet;

	private String TAG = "CustomStickerCategory";

	public CustomStickerCategory(String categoryId)
	{
		super(categoryId);
		this.setCustom(true);
		loadStickers();
	}

	@Override
	public int getState()
	{
		//There is no point having a custom sticker category having a state other than NONE
		return NONE;
	}
	
	@Override
	public void setState(int state)
	{
	}
	
	public CustomStickerCategory(String categoryId, String categoryName, boolean updateAvailable, boolean isVisible, boolean isCustom, boolean isAdded, int catIndex,
			int totalStickers, int categorySize)
	{
		super(categoryId, categoryName, updateAvailable, isVisible, isCustom, isAdded, catIndex, totalStickers, categorySize);
		loadStickers();
	}

	public List<Sticker> getStickerList()
	{

		// right now only recent category is custom
		Set<Sticker> lhs = getStickerSet();

		/*
		 * here using LinkedList as in recents we have to remove the sticker frequently to move it to front and in linked list remove operation is faster compared to arraylist
		 */
		List<Sticker> stickersList = new LinkedList<Sticker>();
		Iterator<Sticker> it = lhs.iterator();
		while (it.hasNext())
		{
			try
			{
				Sticker st = (Sticker) it.next();
				stickersList.add(0, st);
			}
			catch (Exception e)
			{
				Logger.e(getClass().getSimpleName(), "Exception in recent stickers", e);
			}
		}
		
		setDownloadedStickersCount(stickersList.size());
		return stickersList;
	}

	public void loadStickers()
	{
		stickerSet = getSortedListForCategory(getCategoryId(), StickerManager.getInstance().getInternalStickerDirectoryForCategoryId(getCategoryId()));
		if (getCategoryId().equals(StickerManager.RECENT))
		{
			if(stickerSet.isEmpty())
			{
				addDefaultRecentSticker();
			}

			if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.FORCED_RECENTS_PRESENT, false))
			{
				List<Sticker> forcedRecentStickers = StickerManager.getInstance().getForcedRecentsStickers();

				if(forcedRecentStickers != null)
				{
					stickerSet.addAll(forcedRecentStickers);
				}
			}

		}
	}

	/***
	 * 
	 * @param catId
	 * @return
	 * 
	 *         This function can return null if file doesnot exist.
	 */
	public Set<Sticker> getSortedListForCategory(String catId, String dirPath)
	{
		Set<Sticker> list = null;
		FileInputStream fileIn = null;
		ObjectInputStream in = null;
		try
		{
			long t1 = System.currentTimeMillis();
			Logger.d(TAG, "Calling function get sorted list for category : " + catId);
			File dir = new File(dirPath);
			if (!dir.exists())
			{
				dir.mkdirs();
				return Collections.synchronizedSet(new LinkedHashSet<Sticker>(getMaxStickerCount()));
			}
			File catFile = new File(dirPath, catId + ".bin");
			if (!catFile.exists())
				return Collections.synchronizedSet(new LinkedHashSet<Sticker>(getMaxStickerCount()));
			fileIn = new FileInputStream(catFile);
			in = new ObjectInputStream(fileIn);
			int size = in.readInt();
			list = Collections.synchronizedSet(new LinkedHashSet<Sticker>(size));
			for (int i = 0; i < size; i++)
			{
				try
				{
					Sticker s = new Sticker();
					s.deSerializeObj(in);
					File f = new File(s.getSmallStickerPath());
					if(f.exists())
					{
						list.add(s);
					}
				}
				catch (Exception e)
				{
					Logger.e(TAG, "Exception while deserializing sticker", e);
				}
			}
			long t2 = System.currentTimeMillis();
			Logger.d(TAG, "Time in ms to get sticker list of category : " + catId + " from file :" + (t2 - t1));
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception while reading category file.", e);
			list = Collections.synchronizedSet(new LinkedHashSet<Sticker>(getMaxStickerCount()));
		}
		finally
		{
			Utils.closeStreams(in, fileIn);
		}
		return list;
	}

	public void addDefaultRecentSticker()
	{

		String[] recentSticker = { "002_lol.png", "003_teasing.png", "061_lovelips.png", "092_yo.png", "069_hi.png", "033_hawww.png", "047_saale.png", "042_sahihai.png" };
		String[] recentCat = { "expressions", "humanoid",  "expressions", "expressions", "humanoid", "indian",  "indian", "indian"};

		int count = recentSticker.length;
		for (int i = 0; i < count; i++)
		{
			synchronized (stickerSet)
			{
				Sticker s = new Sticker(recentCat[i], recentSticker[i]);
				File f = new File(s.getSmallStickerPath());
				if(f.exists())
				{
					stickerSet.add(s);
				}
			}
		}

	}

	public void addSticker(Sticker st)
	{
		boolean isRemoved = stickerSet.remove(st);
		if (isRemoved) // this means list size is less than 30
			stickerSet.add(st);
		else if (stickerSet.size() == getMaxStickerCount()) // if size is already RECENT_STICKERS_COUNT remove first element and then add
		{
			synchronized (stickerSet)
			{
				Sticker firstSt = stickerSet.iterator().next();
				if (firstSt != null)
					stickerSet.remove(firstSt);
				stickerSet.add(st);
			}
		}
		else
		{
			stickerSet.add(st);
		}
	}

	public void removeSticker(Sticker st)
	{
		synchronized (stickerSet)
		{
			stickerSet.remove(st);
		}
	}

	public Set<Sticker> getStickerSet()
	{
		// TODO Auto-generated method stub
		return stickerSet;
	}

	public int getMaxStickerCount()
	{
		if (getCategoryId().equals(StickerManager.RECENT))
		{
			return StickerManager.RECENT_STICKERS_COUNT;
		}
		else
		{
			return StickerManager.MAX_CUSTOM_STICKERS_COUNT;
		}
	}
}
