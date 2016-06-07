package com.bsb.hike.spaceManager.items;

import com.bsb.hike.spaceManager.models.SubCategoryItem;
import com.bsb.hike.utils.Logger;
import static com.bsb.hike.spaceManager.SpaceManagerUtils.SUB_CATEGORY_TAG;

/**
 * @author paramshah
 */
public class ViralImagesSubCategoryItem extends SubCategoryItem
{
	public ViralImagesSubCategoryItem(String header)
	{
		super(header);
		setSize(computeSize());
	}

	@Override
	public long computeSize()
	{
		return 0;
	}

	@Override
	public void onDelete()
	{
		Logger.d(SUB_CATEGORY_TAG, "deleting viral humor images");
	}
}
