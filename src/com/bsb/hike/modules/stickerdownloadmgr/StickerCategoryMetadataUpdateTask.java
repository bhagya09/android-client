package com.bsb.hike.modules.stickerdownloadmgr;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import com.bsb.hike.utils.Logger;

/**
 * Created by ashishagarwal on 19/04/16.
 */
public class StickerCategoryMetadataUpdateTask implements Runnable
{

	private final String TAG = "StickerCategoryMetadataUpdateTask";

	private int createPackListPageSize;

	private int updatePackListPageSize;

	public StickerCategoryMetadataUpdateTask()
	{
		createPackListPageSize = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.PACK_CREATION_PAGE_SIZE,
				StickerConstants.DEFAULT_PAGE_SIZE_FOR_CATEGORY_CREATION_METADATA);
		updatePackListPageSize = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.PACK_UPDATION_PAGE_SIZE,
				StickerConstants.DEFAULT_PAGE_SIZE_FOR_CATEGORY_UPDATION_METADATA);
	}

	@Override
	public void run()
	{
		List<StickerCategory> stickerCategories = HikeConversationsDatabase.getInstance().getStickerCatToBeSendForMetaData();
		if (Utils.isEmpty(stickerCategories))
		{
			Logger.v(TAG, "already updated after checking all packs in db");
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.UPDATED_ALL_CATEGORIES, true);
			return;
		}
		List<StickerCategory> updateList = new ArrayList<>();
		List<StickerCategory> createList = new ArrayList<>();
		for (StickerCategory stickerCategory : stickerCategories)
		{
			if (stickerCategory.getPackUpdationTime() == 0)
			{
				createList.add(stickerCategory);
				if (createList.size() == createPackListPageSize)
				{
					StickerManager.getInstance().executeFetchCategoryMetadataTask(createList);
					createList = new ArrayList<>();
				}
			}
			else
			{
				updateList.add(stickerCategory);
				if (updateList.size() == updatePackListPageSize)
				{
					StickerManager.getInstance().executeFetchCategoryMetadataTask(updateList);
					updateList = new ArrayList<>();
				}
			}
		}
		StickerManager.getInstance().executeFetchCategoryMetadataTask(createList);
		StickerManager.getInstance().executeFetchCategoryMetadataTask(updateList);
	}
}
