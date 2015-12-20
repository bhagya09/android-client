package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.StickerTagDownloadTask;
import com.bsb.hike.modules.stickersearch.StickerLanguagesManager;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import java.util.List;
import java.util.Set;

public class InitiateStickerTagDownloadTask implements Runnable
{
	private static final String TAG = InitiateStickerTagDownloadTask.class.getSimpleName();

	private boolean firstTime;
	
	private int state;

	private Set<String> languagesSet;
	
	public InitiateStickerTagDownloadTask(boolean firstTime, int state, Set<String> languagesSet)
	{
		this.firstTime = firstTime;
		this.state = state;
		this.languagesSet = languagesSet;
	}

	@Override
	public void run()
	{
		Set<String> stickerSet;
		
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

					if (!Utils.isEmpty(stickers))
					{
						for(Sticker sticker : stickers)
						{
							stickerSet.add(StickerManager.getInstance().getStickerSetString(sticker));
						}
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

			if(state == StickerSearchConstants.STATE_LANGUAGE_TAGS_DOWNLOAD)  // in one case tags are downloaded but still language has not moved from downloading to downloaded
			{
				StickerLanguagesManager.getInstance().addToLanguageSet(StickerLanguagesManager.DOWNLOADED_LANGUAGE_SET_TYPE, languagesSet);
				StickerLanguagesManager.getInstance().removeFromLanguageSet(StickerLanguagesManager.DOWNLOADING_LANGUAGE_SET_TYPE, languagesSet);
			}
			return ;
		}
		
		StickerTagDownloadTask stickerTagDownloadTask = new StickerTagDownloadTask(stickerSet, state, languagesSet);
		stickerTagDownloadTask.execute();
	}
}