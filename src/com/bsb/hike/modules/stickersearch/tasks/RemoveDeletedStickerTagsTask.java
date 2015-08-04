package com.bsb.hike.modules.stickersearch.tasks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchDataController;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;

public class RemoveDeletedStickerTagsTask implements Runnable 
{
	private static final String TAG = RemoveDeletedStickerTagsTask.class.getSimpleName();
			
	public RemoveDeletedStickerTagsTask()
	{
		
	}
	
	@Override
	public void run()
	{
		if ((Utils.getExternalStorageState() == ExternalStorageState.NONE)) // if there is no external storage do not delete any tags. In this case we dont show any recommendation
		{
			Logger.d(TAG, "external storage state is none");
			return;
		}
		
		List<StickerCategory> stickerCategories = StickerManager.getInstance().getMyStickerCategoryList();
		Set<String> stickerSet = new HashSet<String>();
		
		for(StickerCategory stickerCategory : stickerCategories)
		{
			if(stickerCategory.isCustom())
			{
				continue;
			}
			List<Sticker> stickerList  = stickerCategory.getStickerList();
			
			if(!Utils.isEmpty(stickerList))
			{
				for(Sticker sticker : stickerList)
				{
					stickerSet.add(StickerManager.getInstance().getStickerSetString(sticker));
				}
			}
		}
		
		StickerSearchDataController.getInstance().updateStickerList(stickerSet);
	}
}
