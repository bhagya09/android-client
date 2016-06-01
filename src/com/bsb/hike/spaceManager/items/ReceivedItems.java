package com.bsb.hike.spaceManager.items;

import com.bsb.hike.spaceManager.models.SpaceManagerCategory;
import com.bsb.hike.spaceManager.models.SpaceManagerSubCategory;
import static com.bsb.hike.spaceManager.SpaceManagerUtils.CATEGORY_TAG;
import com.bsb.hike.utils.Logger;

import java.util.ArrayList;

/**
 * This is a space manager default category
 *
 * @author paramshah
 */
public class ReceivedItems extends SpaceManagerCategory
{
	public ReceivedItems(String header, ArrayList<SpaceManagerSubCategory> subCategories)
	{
		super(header, subCategories);
		Logger.d(CATEGORY_TAG, "creating category - received items");
		setSize(computeSize());
	}
}
