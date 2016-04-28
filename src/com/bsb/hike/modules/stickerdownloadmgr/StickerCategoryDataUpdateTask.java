package com.bsb.hike.modules.stickerdownloadmgr;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.StickerCategory;
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
		List<StickerCategory> stickerCategoriesMetadataList = HikeConversationsDatabase.getInstance().getStickerCategoriesForMetadataUpdate();
		if (Utils.isEmpty(stickerCategoriesMetadataList))
		{
			Logger.v(TAG, "already updated after checking all packs in db");
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.UPDATED_ALL_CATEGORIES, true);
			return;
		}

        List<String> ucidToUpdate = updateCategoryMetadata(stickerCategoriesMetadataList);

        updateCategoryTagdata(HikeStickerSearchDatabase.getInstance().getStickerCategoriesForTagDataUpdate(ucidToUpdate));

	}

    private List<String> updateCategoryMetadata(List<StickerCategory> stickerCategoriesMetadataList)
    {
        List<StickerCategory> updateList = new ArrayList<StickerCategory>();
        List<StickerCategory> createList = new ArrayList<StickerCategory>();
        List<String> updateUcids = new ArrayList<>(stickerCategoriesMetadataList.size());

        for (StickerCategory stickerCategory : stickerCategoriesMetadataList)
        {
            if (stickerCategory.getPackUpdationTime() == 0)
            {
                createList.add(stickerCategory);
                if (createList.size() == createPackListPageSize)
                {
                    StickerManager.getInstance().fetchCategoryMetadataTask(createList);
                    createList = new ArrayList<StickerCategory>();;
                }
            }
            else
            {
                updateList.add(stickerCategory);
                if (updateList.size() == updatePackListPageSize)
                {
                    StickerManager.getInstance().fetchCategoryMetadataTask(updateList);
                    updateList = new ArrayList<StickerCategory>();
                }
            }
            updateUcids.add(Integer.toString(stickerCategory.getUcid()));
        }
        StickerManager.getInstance().fetchCategoryMetadataTask(createList);
        StickerManager.getInstance().fetchCategoryMetadataTask(updateList);
        return updateUcids;
    }

    private void updateCategoryTagdata(List<CategoryTagData> stickerCategoriesTagDataList)
    {
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
