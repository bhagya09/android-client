package com.bsb.hike.modules.stickersearch.tasks;

import java.util.List;

import com.bsb.hike.modules.stickersearch.datamodel.CategoryTagData;
import com.bsb.hike.modules.stickersearch.provider.db.CategorySearchManager;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchDatabase;

public class CategoryTagInsertTask implements Runnable
{
	private List<CategoryTagData> data;


	public CategoryTagInsertTask(List<CategoryTagData> data)
	{
		this.data = data;
	}


	@Override
	public void run()
	{
        HikeStickerSearchDatabase.getInstance().insertCategoryTagData(data);
	}

}
