package com.bsb.hike.modules.quickstickersuggestions.model;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by anubhavgupta on 12/05/16.
 */
public class QuickSuggestionSticker extends Sticker
{
	private static final String TAG = "QuickSuggestionSticker";

	private int clickCount;

	private int visibleCount;

	public QuickSuggestionSticker(String catId, String stkId)
	{
		super(catId, stkId);
	}

	public int getClickCount()
	{
		return clickCount;
	}

	public void setClickCount(int clickCount)
	{
		this.clickCount = clickCount;
	}

	public int getVisibleCount()
	{
		return visibleCount;
	}

	public void setVisibleCount(int visibleCount)
	{
		this.visibleCount = visibleCount;
	}

	public JSONObject toJSON()
	{
		JSONObject jsonObject = new JSONObject();
		try
		{
			jsonObject.put(HikeConstants.CATEGORY_ID, getCategoryId());
			jsonObject.put(HikeConstants.STICKER_ID, getStickerId());
			jsonObject.put(HikeConstants.CLICK_COUNT, clickCount);
			jsonObject.put(HikeConstants.VISIBLE_COUNT, visibleCount);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "exception in serialization of quick suggestion sticker :", e);
		}
		return jsonObject;
	}

	public static QuickSuggestionSticker fromJSON(JSONObject jsonObject)
	{
		QuickSuggestionSticker quickSuggestionSticker = null;
		try
		{
			quickSuggestionSticker = new QuickSuggestionSticker(jsonObject.getString(HikeConstants.CATEGORY_ID), jsonObject.getString(HikeConstants.STICKER_ID));
			quickSuggestionSticker.setClickCount(jsonObject.getInt(HikeConstants.CLICK_COUNT));
			quickSuggestionSticker.setVisibleCount(jsonObject.getInt(HikeConstants.VISIBLE_COUNT));
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "exception in deserialization of quick suggestion sticker :", e);
		}
		return quickSuggestionSticker;
	}
}
