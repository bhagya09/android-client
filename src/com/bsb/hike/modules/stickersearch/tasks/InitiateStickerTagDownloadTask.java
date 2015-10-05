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

	public InitiateStickerTagDownloadTask(boolean firstTime)
	{
		this.firstTime = firstTime;
	}

	@Override
	public void run()
	{
		Set<String> stickerSet = null;
		
		if (firstTime)
		{
			List<StickerCategory> stickerCategoryList = StickerManager.getInstance().getAllStickerCategories().second;
			stickerSet = HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.STICKER_SET, new HashSet<String>());;

			if (Utils.isEmpty(stickerCategoryList))
			{
				Logger.wtf(TAG, "Empty sticker category list while downloading tags first time.");
			}
			else
			{
				for (StickerCategory category : stickerCategoryList)
				{
					List<Sticker> stickers = category.getStickerList();

					if (!Utils.isEmpty(stickers))
					{
						for(Sticker sticker : stickers)
						{
							stickerSet.add(StickerManager.getInstance().getStickerSetString(sticker));
						}
					}
				}

				HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.STICKER_SET, stickerSet);
			}
		}
		else
		{
			stickerSet = HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.STICKER_SET, new HashSet<String>());
		}

		
		if(Utils.isEmpty(stickerSet))
		{
			Logger.wtf(TAG, "empty sticker set.");
			return ;
		}
		
		StickerTagDownloadTask stickerTagDownloadTask = new StickerTagDownloadTask(stickerSet);
		stickerTagDownloadTask.execute();
	}
}