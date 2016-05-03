package com.bsb.hike.modules.stickersearch.tasks;

import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.MultiStickerTagDownloadTask;
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

	private Set<String> stickerSet;

	private Set<String> languagesSet;

	public InitiateStickerTagDownloadTask(boolean firstTime, int state, Set<String> languagesSet)
	{
		this.firstTime = firstTime;
		this.state = state;
		this.languagesSet = languagesSet;
	}

	public InitiateStickerTagDownloadTask(boolean firstTime, int state, Set<String> languagesSet,Set<String> stickerSet)
	{
		this.firstTime = firstTime;
		this.state = state;
		this.languagesSet = languagesSet;
		this.stickerSet = stickerSet;
	}

	@Override
	public void run()
	{
		if(Utils.isEmpty(stickerSet))
		{
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
						stickerSet.addAll(StickerManager.getInstance().getStickerSetFromList(stickers));
					}

					StickerManager.getInstance().saveStickerSet(stickerSet, state,false);
				}
			}
			else
			{
				stickerSet = StickerManager.getInstance().getStickerSet(state);
			}
		}
		else
		{
			StickerManager.getInstance().saveStickerSet(stickerSet, state, false);
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

		MultiStickerTagDownloadTask stickerTagDownloadTask = new MultiStickerTagDownloadTask(stickerSet, state, languagesSet);
		stickerTagDownloadTask.execute();

	}
}