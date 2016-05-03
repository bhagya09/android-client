package com.bsb.hike.modules.stickersearch.tasks;

import android.text.TextUtils;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.stickersearch.datamodel.CategoryTagData;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchDatabase;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.squareup.okhttp.internal.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

                if(TextUtils.isEmpty(categoryName))
                {
                    Logger.e(TAG, "Ignoring pack tag data for ucid = " + ucid + " for empty pack name");
                    continue;
                }

                categoryTagData.setName(categoryName.toLowerCase().trim());
				categoryTagData.setCategoryLastUpdatedTime(categoryJSON.optLong(HikeConstants.TIMESTAMP));
				categoryTagData.setGender(categoryJSON.optInt(HikeConstants.GENDER));


                JSONObject addedData = categoryJSON.optJSONObject(HikeConstants.ADDED_DATA);
                JSONObject removedData = categoryJSON.optJSONObject(HikeConstants.REMOVED_DATA);

				categoryTagData.setThemes(getModifiedFieldList(categoryTagData.getThemes(), (addedData == null) ? null : addedData.optJSONArray(HikeConstants.THEMES),
                        (removedData == null) ? null : removedData.optJSONArray(HikeConstants.THEMES)));
                categoryTagData.setKeywords(getModifiedFieldList(categoryTagData.getKeywords(), (addedData == null) ? null : addedData.optJSONArray(HikeConstants.TAGS),
                        (removedData == null) ? null : removedData.optJSONArray(HikeConstants.TAG)));
                categoryTagData.setLanguages(getModifiedFieldList(categoryTagData.getLanguages(), (addedData == null) ? null : addedData.optJSONArray(HikeConstants.LANGUAGES),
                        (removedData == null) ? null : removedData.optJSONArray(HikeConstants.LANGUAGES)));

                result.add(categoryTagData);
			}
		}

		catch (JSONException e)
		{
			e.printStackTrace();
		}

		HikeStickerSearchDatabase.getInstance().insertCategoryTagDataList(result);
	}

	private List<String> getModifiedFieldList(List<String> currentFieldList, JSONArray addedFieldList, JSONArray removedFieldList) throws JSONException
	{
		if (Utils.isEmpty(currentFieldList))
		{
			currentFieldList = new ArrayList<String>();
		}

		if (!Utils.isEmpty(addedFieldList))
		{
			for (int i = 0; i < addedFieldList.length(); i++)
			{
				String activeField = addedFieldList.optString(i);
				if (!TextUtils.isEmpty(activeField) && !currentFieldList.contains(activeField))
				{
					currentFieldList.add(activeField.toLowerCase().trim());
				}
			}
		}

		if (!Utils.isEmpty(removedFieldList) && !Utils.isEmpty(currentFieldList))
		{
			for (int i = 0; i < removedFieldList.length(); i++)
			{
				String inActiveField = removedFieldList.optString(i);
				if (!TextUtils.isEmpty(inActiveField) && currentFieldList.contains(inActiveField))
				{
					currentFieldList.remove(inActiveField.toLowerCase().trim());
				}
			}
		}

		return currentFieldList;
	}

}
