package com.bsb.hike.spaceManager.items;

import com.bsb.hike.spaceManager.models.CategoryItem;
import com.bsb.hike.spaceManager.models.SubCategoryItem;
import static com.bsb.hike.spaceManager.SpaceManagerUtils.CATEGORY_TAG;

import com.bsb.hike.utils.Logger;

import java.util.ArrayList;

/**
 * This is a space manager default category
 *
 * @author paramshah
 */
public class ReceivedContentCategoryItem extends CategoryItem
{
	public ReceivedContentCategoryItem(String header, ArrayList<SubCategoryItem> subCategories)
	{
		super(header, subCategories);
		Logger.d(CATEGORY_TAG, "creating category - received items");
		setSize(computeSize());
	}
}
