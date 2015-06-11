package com.bsb.hike.modules.stickersearch.tasks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.util.Pair;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.StickerTagDownloadTask;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StickerManager;

public class InitiateStickerTagDownloadTask implements Runnable
{

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
			List<StickerCategory> stickerCategoryList = StickerManager.getInstance().getStickerCategoryList();

			stickerSet = new HashSet<String>();
			for (StickerCategory category : stickerCategoryList)
			{
				List<Sticker> stickers = category.getStickerList();
				
				for(Sticker sticker : stickers)
				{
					stickerSet.add(StickerManager.getInstance().getStickerSetString(sticker));
				}
			}
			
			HikeSharedPreferenceUtil.getInstance().saveDataSet(HikeMessengerApp.STICKER_SET, stickerSet);
		}
		else
		{
			stickerSet = HikeSharedPreferenceUtil.getInstance().getDataSet(HikeMessengerApp.STICKER_SET, null);
		}
		StickerTagDownloadTask stickerTagDownloadTask = new StickerTagDownloadTask(stickerSet);
		stickerTagDownloadTask.execute();
	}
}
