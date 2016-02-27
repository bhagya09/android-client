package com.bsb.hike.modules.stickersearch.tasks;

import android.util.Pair;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchDataController;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RemoveDeletedStickerTagsTask implements Runnable
{
	private static final String TAG = RemoveDeletedStickerTagsTask.class.getSimpleName();

	private Set<String> infoSet;

	private int removalType;

	public RemoveDeletedStickerTagsTask(Set<String> infoSet, int removalType)
	{
		this.infoSet = infoSet;
		this.removalType = removalType;
	}

	@Override
	public void run()
	{
		if (removalType == StickerSearchConstants.REMOVAL_BY_EXCLUSION_IN_EXISTING_STCIKERS)
		{
			infoSet = getExistingStickers();
		}

		if (infoSet == null)
		{
			return;
		}

		StickerSearchDataController.getInstance().updateStickerList(infoSet, removalType);
	}

	private Set<String> getExistingStickers()
	{
		// If there is no external storage, do not delete any tags. In this case, we don't show any recommendation.
		if (!Utils.isUserSignedUp(HikeMessengerApp.getInstance(), false) || (Utils.getExternalStorageState() == ExternalStorageState.NONE))
		{
			Logger.d(TAG, "External storage state is none.");
			return null;
		}

		Pair<Boolean, List<StickerCategory>> result = StickerManager.getInstance().getAllStickerCategories();
		if (result.first == false)
		{
			return null;
		}

		List<StickerCategory> stickerCategories = result.second;
		Set<String> stickerSet = new HashSet<String>();

		if (Utils.isEmpty(stickerCategories))
		{
			Logger.w(TAG, "Empty list of sticker categories");
		}
		else
		{
			for (StickerCategory stickerCategory : stickerCategories)
			{
				if (stickerCategory.isCustom())
				{
					continue;
				}

				List<Sticker> stickerList = stickerCategory.getStickerList();

				if (!Utils.isEmpty(stickerList))
				{
					for (Sticker sticker : stickerList)
					{
						stickerSet.add(sticker.getStickerCode());
					}
				}
			}
		}

		return stickerSet;
	}
}