package com.bsb.hike.spaceManager;

import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.spaceManager.models.CategoryItem;
import com.bsb.hike.spaceManager.models.CategoryPojo;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

/**
 * @author paramshah
 */
public class SpaceManagerItemsFetcher
{
	private static List<CategoryPojo> categoryPojosList;

	private static final String TAG = "SpaceManagerItemsFetcher";

	/**
	 * Contains Array of JSON Object
	 * each JSON Object :- subCategoryList(JSON Array), className, header
	 */
	public static final String DEFAULT_ITEM_JSON =
			"[" +
				"{\"subCategoryList\"" +
					":[" +
						"{\"className\":\"com.bsb.hike.spaceManager.items.ViralImagesSubCategoryItem\"," +
						"\"header\":\"Just For Laugh Images\"}" +
					"]," +
					"\"className\":\"com.bsb.hike.spaceManager.items.ReceivedContentCategoryItem\"," +
					"\"header\":\"Received Content\"" +
				"}" +
			"]";

	public static void fetchItems()
	{
		String json = getJSON();
		parseFetchedJSON(json, DEFAULT_ITEM_JSON.equals(json));
	}

	private static String getJSON()
	{
		String json = DEFAULT_ITEM_JSON;
		if (HikeSharedPreferenceUtil.getInstance().contains(SpaceManagerUtils.SPACE_MANAGER_ITEMS))
		{
			json = HikeSharedPreferenceUtil.getInstance().getData(SpaceManagerUtils.SPACE_MANAGER_ITEMS, DEFAULT_ITEM_JSON);
			if (TextUtils.isEmpty(json))
			{
				json = DEFAULT_ITEM_JSON;
			}
		}
		return json;
	}


	public static void parseFetchedJSON(String itemsJson, boolean isDefaultJSON)
	{
		Logger.d(TAG, "fetching space manager items");

		try
		{
			CategoryPojo[] categoryPojos = new Gson().fromJson(itemsJson, CategoryPojo[].class);
			categoryPojosList = Arrays.asList(categoryPojos);
			Logger.d(TAG, "category list: " + categoryPojosList.toString());
			List<CategoryItem> spaceManagerItems = SpaceManagerJavaReflector.reflect(categoryPojosList);
			HikeMessengerApp.getPubSub().publishOnUI(HikePubSub.SPACE_MANAGER_ITEMS_FETCH_SUCCESS, spaceManagerItems);
		}
		catch (ClassNotFoundException |InstantiationException | IllegalAccessException | NoSuchMethodException
				| InvocationTargetException | IllegalArgumentException | JsonSyntaxException ex)
		{
			Logger.d(TAG, "error in fetching items, invalid json ");
			if(isDefaultJSON)
			{
				HikeMessengerApp.getPubSub().publishOnUI(HikePubSub.SPACE_MANAGER_ITEMS_FETCH_FAIL, null);
			}
			else
			{
				HikeSharedPreferenceUtil.getInstance().removeData(SpaceManagerUtils.SPACE_MANAGER_ITEMS);
				parseFetchedJSON(DEFAULT_ITEM_JSON, true);
			}
		}
	}
}
