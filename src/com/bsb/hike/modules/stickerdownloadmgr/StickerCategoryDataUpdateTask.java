package com.bsb.hike.modules.stickerdownloadmgr;

import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.httpmgr.request.PriorityConstants;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.stickersearch.datamodel.CategoryTagData;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchDatabase;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import com.bsb.hike.utils.Logger;

/**
 * Created by ashishagarwal on 19/04/16.
 */
public class StickerCategoryDataUpdateTask implements Runnable
{

	private final String TAG = "StickerCategoryMetadataUpdateTask";

	private int createPackListPageSize;

	private int updatePackListPageSize;

	public StickerCategoryDataUpdateTask()
	{
		createPackListPageSize = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.PACK_CREATION_PAGE_SIZE,
				StickerConstants.DEFAULT_PAGE_SIZE_FOR_CATEGORY_CREATION_METADATA);
		updatePackListPageSize = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.PACK_UPDATION_PAGE_SIZE,
				StickerConstants.DEFAULT_PAGE_SIZE_FOR_CATEGORY_UPDATION_METADATA);
	}

	@Override
	public void run()
	{
		Pair<List<StickerCategory>, List<String>> updataLists = HikeConversationsDatabase.getInstance().getStickerCategoriesForDataUpdate(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.FETCH_METADATA_PACK_COUNT, StickerConstants.DEFAULT_CATEGORIES_TO_FETCH_DATA));
		List<StickerCategory> stickerCategoriesMetadataList = updataLists.first;
		List<String> stickerCategoriesTagdataList = updataLists.second;

		if (Utils.isEmpty(stickerCategoriesMetadataList))
		{
			Logger.v(TAG, "Metadata already updated after checking all packs in db");
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.UPDATED_ALL_CATEGORIES_METADATA, true);
		}
		else
		{
			updateCategoryMetadata(stickerCategoriesMetadataList);
		}

		if (Utils.isEmpty(stickerCategoriesTagdataList))
		{
			Logger.v(TAG, "Tagdata already updated after checking all packs in db");
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.UPDATED_ALL_CATEGORIES_TAGDATA, true);
		}
		else
		{
			updateCategoryTagdata(HikeStickerSearchDatabase.getInstance().getStickerCategoriesForTagDataUpdate(stickerCategoriesTagdataList));
		}

        StickerManager.getInstance().updateStickerShopSearchAllowedStatus();

	}

	private void updateCategoryMetadata(List<StickerCategory> stickerCategoriesMetadataList)
	{
		List<StickerCategory> updateList = new ArrayList<StickerCategory>();
		List<StickerCategory> createList = new ArrayList<StickerCategory>();

		for (StickerCategory stickerCategory : stickerCategoriesMetadataList)
		{
			if (stickerCategory.getPackUpdationTime() == 0)
			{
				createList.add(stickerCategory);
				if (createList.size() == createPackListPageSize)
				{
					StickerManager.getInstance().fetchCategoryMetadataTask(createList, Request.REQUEST_TYPE_LONG, PriorityConstants.PRIORITY_LOW, false);
					createList = new ArrayList<StickerCategory>();
					;
				}
			}
			else
			{
				updateList.add(stickerCategory);
				if (updateList.size() == updatePackListPageSize)
				{
					StickerManager.getInstance().fetchCategoryMetadataTask(updateList, Request.REQUEST_TYPE_LONG, PriorityConstants.PRIORITY_LOW, false);
					updateList = new ArrayList<StickerCategory>();
				}
			}
		}
		StickerManager.getInstance().fetchCategoryMetadataTask(createList, Request.REQUEST_TYPE_LONG, PriorityConstants.PRIORITY_LOW, false);
		StickerManager.getInstance().fetchCategoryMetadataTask(updateList, Request.REQUEST_TYPE_LONG, PriorityConstants.PRIORITY_LOW, false);
	}

	private void updateCategoryTagdata(List<CategoryTagData> stickerCategoriesTagDataList)
	{
        if(Utils.isEmpty(stickerCategoriesTagDataList))
        {
            return;
        }

		List<CategoryTagData> updateList = new ArrayList<CategoryTagData>();
		List<CategoryTagData> createList = new ArrayList<CategoryTagData>();

		for (CategoryTagData categoryTagData : stickerCategoriesTagDataList)
		{
			if (categoryTagData.getCategoryLastUpdatedTime() == 0)
			{
				createList.add(categoryTagData);
				if (createList.size() == createPackListPageSize)
				{
					StickerManager.getInstance().fetchCategoryTagdataTask(createList);
					createList = new ArrayList<CategoryTagData>();
				}
			}
			else
			{
				updateList.add(categoryTagData);
				if (updateList.size() == updatePackListPageSize)
				{
					StickerManager.getInstance().fetchCategoryTagdataTask(updateList);
					updateList = new ArrayList<CategoryTagData>();
				}
			}
		}

		StickerManager.getInstance().fetchCategoryTagdataTask(createList);
		StickerManager.getInstance().fetchCategoryTagdataTask(updateList);
	}

}
