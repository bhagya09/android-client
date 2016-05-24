package com.bsb.hike.modules.stickersearch.tasks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.stickersearch.datamodel.CategoryTagData;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchDatabase;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * Created by akhiltripathi on 12/04/16.
 */

public class CategoryTagInsertTask implements Runnable
{
	private final String TAG = CategoryTagInsertTask.class.getSimpleName();

	private Map<Integer, CategoryTagData> sourceCategoryData;

	private JSONArray categoriesJSON;

	public CategoryTagInsertTask(JSONArray categoriesData, Map<Integer, CategoryTagData> sourceData)
	{
		this.categoriesJSON = categoriesData;
		this.sourceCategoryData = sourceData;

	}

	@Override
	public void run()
	{
		List<CategoryTagData> result = new ArrayList<CategoryTagData>(categoriesJSON.length());

		try
		{
			for (int i = 0; i < categoriesJSON.length(); i++)
			{

				JSONObject categoryJSON = categoriesJSON.getJSONObject(i);
				int ucid = categoryJSON.optInt(HikeConstants.UCID, -1);

				CategoryTagData categoryTagData = sourceCategoryData.get(ucid);
				if (categoryTagData == null)
				{
					Logger.e(TAG, "Ignoring pack tag data for ucid = " + ucid + " for null tag data");
					continue;
				}

				String categoryName = categoryJSON.optString(HikeConstants.CAT_NAME);

				if (TextUtils.isEmpty(categoryName) && TextUtils.isEmpty(categoryTagData.getName()))
				{
					Logger.e(TAG, "Ignoring pack tag data for ucid = " + ucid + " for empty pack name");
					continue;
				}

				if (!TextUtils.isEmpty(categoryName))
				{
					categoryTagData.setName(categoryName.toLowerCase().trim());
				}

				categoryTagData.setCategoryLastUpdatedTime(categoryJSON.optLong(HikeConstants.TIMESTAMP));

				if (categoryJSON.has(HikeConstants.GENDER))
				{
					categoryTagData.setGender(categoryJSON.optInt(HikeConstants.GENDER));
				}

				JSONObject addedData = categoryJSON.optJSONObject(HikeConstants.ADDED_DATA);
				JSONObject removedData = categoryJSON.optJSONObject(HikeConstants.REMOVED_DATA);

				JSONArray categoryThemesAdded = (addedData == null) ? null : addedData.optJSONArray(HikeConstants.THEMES);
				JSONArray categoryThemesremoved = (removedData == null) ? null : removedData.optJSONArray(HikeConstants.THEMES);
				JSONArray categoryLanguagedsAdded = (addedData == null) ? null : addedData.optJSONArray(HikeConstants.LANGUAGES);
				JSONArray categoryLanguagesRemoved = (removedData == null) ? null : removedData.optJSONArray(HikeConstants.LANGUAGES);
				JSONArray categoryKeywordsAdded = (addedData == null) ? null : addedData.optJSONArray(HikeConstants.TAGS);
				JSONArray categoryKeywordsremoved = (removedData == null) ? null : removedData.optJSONArray(HikeConstants.TAGS);

				categoryTagData.setThemes(getModifiedFieldList(categoryTagData.getThemes(), categoryThemesAdded, categoryThemesremoved));
				categoryTagData.setKeywords(getModifiedFieldList(categoryTagData.getKeywords(), categoryKeywordsAdded, categoryKeywordsAdded));
				categoryTagData.setLanguages(getModifiedFieldList(categoryTagData.getLanguages(), categoryLanguagedsAdded, categoryLanguagesRemoved));

				result.add(categoryTagData);
			}
		}

		catch (JSONException e)
		{
			Logger.e(TAG, "Exception While parsing JSON : " + e.getMessage());
		}

		HikeStickerSearchDatabase.getInstance().insertCategoryTagDataList(result);
	}

	private List<String> getModifiedFieldList(List<String> currentFieldList, JSONArray addedFieldList, JSONArray removedFieldList) throws JSONException
	{
		if (Utils.isEmpty(currentFieldList))
		{
			currentFieldList = new ArrayList<String>();
		}

		Set result = new HashSet<String>(currentFieldList);

		if (!Utils.isEmpty(addedFieldList))
		{
			for (int i = 0; i < addedFieldList.length(); i++)
			{
				String activeField = addedFieldList.optString(i);

				if (!TextUtils.isEmpty(activeField))
				{
					result.add(activeField.toLowerCase().trim());
				}
			}
		}

		if (!Utils.isEmpty(removedFieldList) && !Utils.isEmpty(currentFieldList))
		{
			for (int i = 0; i < removedFieldList.length(); i++)
			{
				String inActiveField = removedFieldList.optString(i);
				if (!TextUtils.isEmpty(inActiveField))
				{
					result.remove(inActiveField.toLowerCase().trim());
				}
			}
		}

		return new ArrayList<String>(result);
	}

}
