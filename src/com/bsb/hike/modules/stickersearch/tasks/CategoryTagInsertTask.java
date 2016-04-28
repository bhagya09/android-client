package com.bsb.hike.modules.stickersearch.tasks;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.stickersearch.datamodel.CategoryTagData;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CategoryTagInsertTask implements Runnable
{
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
					continue;
				}

                categoryTagData.setName(categoryJSON.optString(HikeConstants.CAT_NAME));
				categoryTagData.setCategoryLastUpdatedTime(categoryJSON.optLong(HikeConstants.TIMESTAMP));
				categoryTagData.setGender(categoryJSON.optInt(HikeConstants.GENDER));
                categoryTagData.setThemes(getModifiedFieldList(categoryTagData.getThemes(), categoryJSON.optJSONArray(HikeConstants.THEMES)));
                categoryTagData.setLanguages(getModifiedFieldList(categoryTagData.getLanguages(), categoryJSON.optJSONArray(HikeConstants.LANGUAGES)));
                categoryTagData.setKeywords(getModifiedFieldList(categoryTagData.getKeywords(), categoryJSON.optJSONArray(HikeConstants.TAGS)));

                result.add(categoryTagData);
			}
		}

		catch (JSONException e)
		{
			e.printStackTrace();
		}

		HikeStickerSearchDatabase.getInstance().insertCategoryTagDataList(result);
	}

	private List<String> getModifiedFieldList(List<String> currentFieldList, JSONArray updatedInFieldList) throws JSONException
	{
		Pair<List<String>, List<String>> segregatedFieldUpdates = getUpdatedPairs(updatedInFieldList);
		for (String activeField : segregatedFieldUpdates.first)
		{
			if (!currentFieldList.contains(activeField))
			{
				currentFieldList.add(activeField);
			}
		}
		for (String inActiveField : segregatedFieldUpdates.second)
		{
			if (currentFieldList.contains(inActiveField))
			{
				currentFieldList.remove(inActiveField);
			}
		}
		return currentFieldList;
	}

	private Pair<List<String>, List<String>> getUpdatedPairs(JSONArray fields) throws JSONException
	{
		List<String> active = new ArrayList<String>();
		List<String> inactive = new ArrayList<String>();

		for (int i = 0; i < fields.length(); i++)
		{
			JSONObject fieldObject = fields.getJSONObject(i);
			Iterator<String> fieldIterator = fieldObject.keys();
			if (fieldIterator.hasNext())
			{
				String field = fieldIterator.next();
				boolean isActive = fieldObject.optInt(field, 1) == 1;
				if (isActive)
				{
					active.add(field);
				}
				else
				{
					inactive.add(field);
				}
			}

		}

		return new Pair<List<String>, List<String>>(active, inactive);
	}

}
