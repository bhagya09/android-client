package com.bsb.hike.modules.stickersearch.tasks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchSetupManager;
import com.bsb.hike.utils.StickerManager;

public class RemoveDeletedStickerTagsTask implements Runnable 
{

	public RemoveDeletedStickerTagsTask()
	{
		
	}
	
	@Override
	public void run()
	{
		List<StickerCategory> stickerCategories = StickerManager.getInstance().getStickerCategoryList();
		Set<String> stickerSet = new HashSet<String>();
		
		for(StickerCategory stickerCategory : stickerCategories)
		{
			if(stickerCategory.isCustom())
			{
				continue;
			}
			List<Sticker> stickerList  = stickerCategory.getStickerList();
			
			if(stickerList != null)
			{
				for(Sticker sticker : stickerList)
				{
					stickerSet.add(StickerManager.getInstance().getStickerSetString(sticker));
				}
			}
		}
		
		StickerSearchSetupManager.getInstance().updateStickerList(stickerSet);
	}

}
