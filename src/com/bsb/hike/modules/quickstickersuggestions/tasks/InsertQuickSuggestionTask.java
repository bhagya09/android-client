package com.bsb.hike.modules.quickstickersuggestions.tasks;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.modules.quickstickersuggestions.model.QuickSuggestionSticker;
import com.bsb.hike.modules.quickstickersuggestions.model.QuickSuggestionStickerCategory;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.quickstickersuggestions.QuickStickerSuggestionController;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by anubhavgupta on 12/05/16.
 */
public class InsertQuickSuggestionTask implements Runnable
{
	private final String TAG = "InsertQuickSuggestionTask";

	private JSONArray quickSuggestionJsonArray;

	public InsertQuickSuggestionTask(JSONArray quickSuggestionJsonArray)
	{
		this.quickSuggestionJsonArray = quickSuggestionJsonArray;
	}

	@Override
	public void run()
	{
		try
		{
			if (Utils.isEmpty(quickSuggestionJsonArray))
			{
				Logger.w(TAG, "quick suggestion json array is empty returning ...");
				return;
			}

            List<StickerCategory> quickSuggestionCategoryList = new ArrayList<>(quickSuggestionJsonArray.length());
			Set<Sticker> quickSuggestionStickerSet = new HashSet<>(quickSuggestionJsonArray.length());

			for (int i = 0; i < quickSuggestionJsonArray.length(); i++)
			{
				JSONObject response = (JSONObject) quickSuggestionJsonArray.get(i);
                StickerCategory quickSuggestionStickerCategory = parseResponse(response);
                if(null != quickSuggestionStickerCategory)
                {
                    quickSuggestionCategoryList.add(quickSuggestionStickerCategory);
					quickSuggestionStickerSet.add(((QuickSuggestionStickerCategory) quickSuggestionStickerCategory).getQuickSuggestSticker());
                }
			}

            HikeConversationsDatabase.getInstance().insertQuickSuggestionData(quickSuggestionCategoryList);
			QuickStickerSuggestionController.getInstance().removeFromRetrySet(quickSuggestionStickerSet);
			sendSignalToUi(quickSuggestionCategoryList);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "exception in inserting quick suggestions ", e);
		}
	}

	private void sendSignalToUi(List<StickerCategory> quicStickerCategoryList)
	{
		for(StickerCategory category : quicStickerCategoryList)
		{
			QuickSuggestionStickerCategory quickSuggestionCategory = (QuickSuggestionStickerCategory) category;
			LocalBroadcastManager.getInstance(HikeMessengerApp.getInstance()).sendBroadcast(new Intent(StickerManager.QUICK_STICKER_SUGGESTION_FETCHED).putExtra(HikeConstants.BUNDLE, quickSuggestionCategory.toBundle()));
		}
	}

	public StickerCategory parseResponse(JSONObject response)
	{
        QuickSuggestionStickerCategory quickSuggestionStickerCategory = null;
		try
		{
			String catId = response.getString(HikeConstants.CATEGORY_ID);
			String stkId = response.getString(HikeConstants.STICKER_ID);

			JSONArray replyJson = response.getJSONArray(HikeConstants.REPLY);
            JSONArray sentJson = response.getJSONArray(HikeConstants.SENT);

             quickSuggestionStickerCategory = new QuickSuggestionStickerCategory.Builder()
					 .setCategoryId(StickerManager.QUICK_SUGGESTIONS)
					 .setQuickSuggestSticker(new Sticker(catId, stkId))
					 .setReplyStickerSet(getStickerSet(replyJson))
					 .setSentStickerSet(getStickerSet(sentJson))
					 .build();


		}
		catch (JSONException e)
		{
			Logger.e(TAG, "exception in parsing quick suggestion response");
		}

        return quickSuggestionStickerCategory;
	}

	public Set<Sticker> getStickerSet(JSONArray jsonArray)
	{

        Set<Sticker> stickerSet = new LinkedHashSet<>();
        try
		{
            if(!Utils.isEmpty(jsonArray))
            {
                for (int i = 0; i < jsonArray.length(); i++)
                {
                    JSONObject json = (JSONObject) jsonArray.get(i);

                    String catId = json.getString(HikeConstants.CATEGORY_ID);
                    String stkId = json.getString(HikeConstants.STICKER_ID);

                    Sticker sticker = new QuickSuggestionSticker(catId, stkId);
                    stickerSet.add(sticker);
                }
            }
		}
		catch (JSONException e)
		{
            Logger.e(TAG, "exception in parsing reply/sent list");
		}
        return  stickerSet;
	}
}
