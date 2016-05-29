package com.bsb.hike.models;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CustomStickerCategory extends StickerCategory
{
	private Set<Sticker> stickerSet;

	protected CustomStickerCategory(Init<?> builder)
	{
		super(builder);
		this.stickerSet = builder.stickerSet;
	}

	protected static abstract class Init<S extends Init<S>> extends StickerCategory.Init<S>
	{
		private Set<Sticker> stickerSet;

		public S setStickerSet(Set<Sticker> stickerSet)
		{
			this.stickerSet = stickerSet;
			return self();
		}

		public CustomStickerCategory build()
		{
			return new CustomStickerCategory(this);
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

	private String TAG = "CustomStickerCategory";

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

		if (getCategoryId().equals(StickerManager.RECENT))
		{
			stickerSet = loadRecentsFromDb();
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

	private Set<Sticker> loadRecentsFromDb()
	{
		return HikeConversationsDatabase.getInstance().getRecentStickers();
	}

	private void addDefaultRecentSticker()
	{

		String[] recentSticker = { "002_lol.png", "003_teasing.png", "061_lovelips.png", "092_yo.png", "069_hi.png", "033_hawww.png", "047_saale.png", "042_sahihai.png" };
		String[] recentCat = { "expressions", "humanoid",  "expressions", "expressions", "humanoid", "indian",  "indian", "indian"};

		int count = recentSticker.length;
		for (int i = 0; i < count; i++)
		{
			synchronized (stickerSet)
			{
				Sticker s = new Sticker(recentCat[i], recentSticker[i]);
				File f = new File(s.getSmallStickerFilePath());
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
		if(Utils.isEmpty(stickerSet))
		{
			loadStickers();
		}
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
