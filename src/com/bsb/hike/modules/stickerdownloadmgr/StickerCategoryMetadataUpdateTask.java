package com.bsb.hike.modules.stickerdownloadmgr;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ashishagarwal on 19/04/16.
 */
public class StickerCategoryMetadataUpdateTask implements Runnable
{

	@Override
	public void run()
	{
		List<StickerCategory> stickerCategories;
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.UPDATED_ALL_CATEGORIES, false))
		{
			return;
		}
		stickerCategories = HikeConversationsDatabase.getInstance().getStickerCatToBeSendForMetaData();
		if (stickerCategories == null || stickerCategories.isEmpty())
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.UPDATED_ALL_CATEGORIES, true);
			return;
		}
		List<StickerCategory> updateList = new ArrayList<>();
		List<StickerCategory> createList = new ArrayList<>();
		FetchAllCategoriesDownloadTask fetchAllCategoriesDownloadTask;
		for (StickerCategory stickerCategory : stickerCategories)
		{
			if (stickerCategory.getPackUpdationTime() == 0)
			{
				createList.add(stickerCategory);
				if (createList.size() == StickerConstants.PAGE_SIZE_FOR_CATEGORY_CREATION_METADATA)
				{
					fetchAllCategoriesDownloadTask = new FetchAllCategoriesDownloadTask(createList);
					fetchAllCategoriesDownloadTask.execute();
					createList = new ArrayList<>();
				}
			}
			else
			{
				updateList.add(stickerCategory);
				if (updateList.size() == StickerConstants.PAGE_SIZE_FOR_CATEGORY_UPDATION_METADATA)
				{
					fetchAllCategoriesDownloadTask = new FetchAllCategoriesDownloadTask(updateList);
					fetchAllCategoriesDownloadTask.execute();
					updateList = new ArrayList<>();
				}
			}
		}
		fetchAllCategoriesDownloadTask = new FetchAllCategoriesDownloadTask(createList);
		fetchAllCategoriesDownloadTask.execute();
		fetchAllCategoriesDownloadTask = new FetchAllCategoriesDownloadTask(updateList);
		fetchAllCategoriesDownloadTask.execute();
	}
}
