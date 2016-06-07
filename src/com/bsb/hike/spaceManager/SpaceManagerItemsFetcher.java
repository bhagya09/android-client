package com.bsb.hike.spaceManager;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.spaceManager.models.CategoryPojo;
import com.bsb.hike.spaceManager.models.SpaceManagerItem;
import com.bsb.hike.spaceManager.models.SubCategoryPojo;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.google.gson.Gson;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author paramshah
 */
public class SpaceManagerItemsFetcher
{
	private static List<CategoryPojo> categoryPojosList;

	private static final String TAG = "SpaceManagerItemsFetcher";

	private static final String DEFAULT_CATEGORY_CLASS = "com.bsb.hike.spaceManager.items.ReceivedContentCategoryItem";

	private static final String DEFAULT_CATEGORY_HEADER = "Received Content";

	private static final String DEFAULT_SUB_CATEGORY_CLASS = "com.bsb.hike.spaceManager.items.ViralImagesSubCategoryItem";

	private static final String DEFAULT_SUB_CATEGORY_HEADER = "Just For Laugh Images";

	private static String getDefaultItemsString()
	{
		/**
		 * Default Items: Category - Received Content, SubCategory - Viral Humor Images
		 */
		SubCategoryPojo subCategoryPojo = new SubCategoryPojo();
		subCategoryPojo.setClassName(DEFAULT_SUB_CATEGORY_CLASS);
		subCategoryPojo.setHeader(DEFAULT_SUB_CATEGORY_HEADER);

		List<SubCategoryPojo> subCategoryPojos = new ArrayList<>();
		subCategoryPojos.add(subCategoryPojo);

		List<CategoryPojo> categoryPojos = new ArrayList<>();
		CategoryPojo categoryPojo = new CategoryPojo();
		categoryPojo.setClassName(DEFAULT_CATEGORY_CLASS);
		categoryPojo.setHeader(DEFAULT_CATEGORY_HEADER);
		categoryPojo.setSubCategoryList(subCategoryPojos);
		categoryPojos.add(categoryPojo);

		return new Gson().toJson(categoryPojos);
	}

	public static void fetchItems()
	{
		Logger.d(TAG, "fetching space manager items");
		String itemsJson = HikeSharedPreferenceUtil.getInstance().getData(SpaceManagerUtils.SPACE_MANAGER_ITEMS, getDefaultItemsString());
		Logger.d(TAG, "items string: " + itemsJson);
		try
		{
			CategoryPojo[] categoryPojos = new Gson().fromJson(itemsJson, CategoryPojo[].class);
			categoryPojosList = Arrays.asList(categoryPojos);
			Logger.d(TAG, "category list: " + categoryPojosList.toString());
			List<SpaceManagerItem> spaceManagerItems = SpaceManagerJavaReflector.reflect(categoryPojosList);
			HikeMessengerApp.getPubSub().publishOnUI(HikePubSub.SPACE_MANAGER_ITEMS_FETCH_SUCCESS, spaceManagerItems);
		}
		catch (ClassNotFoundException |InstantiationException | IllegalAccessException | NoSuchMethodException
				| InvocationTargetException | IllegalArgumentException ex)
		{
			Logger.d(TAG, "error in fetching items " + ex.getMessage());
			HikeMessengerApp.getPubSub().publishOnUI(HikePubSub.SPACE_MANAGER_ITEMS_FETCH_FAIL, null);
		}
	}
}
