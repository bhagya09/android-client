package com.bsb.hike.modules.stickersearch.tasks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.StickerTagDownloadTask;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class InitiateStickerTagDownloadTask implements Runnable
{
	private static final String TAG = InitiateStickerTagDownloadTask.class.getSimpleName();

	private boolean firstTime;
	
	private int state;
	
	public InitiateStickerTagDownloadTask(boolean firstTime, int state)
	{
		this.firstTime = firstTime;
		this.state = state;
	}

	@Override
	public void run()
	{
		Set<String> stickerSet = null;
		
		if (firstTime)
		{
			List<StickerCategory> stickerCategoryList = StickerManager.getInstance().getAllStickerCategories().second;
			stickerSet = StickerManager.getInstance().getStickerSet(state);

			if (Utils.isEmpty(stickerCategoryList))
			{
				Logger.wtf(TAG, "Empty sticker category list while downloading tags first time.");
			}
			else
			{
				for (StickerCategory category : stickerCategoryList)
				{
					List<Sticker> stickers = category.getStickerList();
					
					for(Sticker sticker : stickers)
					{
						stickerSet.add(StickerManager.getInstance().getStickerSetString(sticker));
					}
				}

				StickerManager.getInstance().saveStickerSet(stickerSet, state);
			}
		}
		else
		{
			stickerSet = StickerManager.getInstance().getStickerSet(state);
		}

		
		if(Utils.isEmpty(stickerSet))
		{
			Logger.wtf(TAG, "empty sticker set.");
			return ;
		}
		
		StickerTagDownloadTask stickerTagDownloadTask = new StickerTagDownloadTask(stickerSet, state);
		stickerTagDownloadTask.execute();
	}
}