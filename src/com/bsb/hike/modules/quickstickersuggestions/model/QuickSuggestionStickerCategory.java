package com.bsb.hike.modules.quickstickersuggestions.model;

import android.os.Bundle;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.CustomStickerCategory;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.quickstickersuggestions.QuickStickerSuggestionController;
import com.bsb.hike.utils.Logger;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by anubhavgupta on 09/05/16.
 */
public class QuickSuggestionStickerCategory extends CustomStickerCategory
{
	private static final String TAG = "QuickSuggestionStickerCategory";

	private volatile Set<Sticker> replyStickers;

	private volatile Set<Sticker> sentStickers;

	private Sticker quickSuggestSticker;

	private boolean showReplyStickers;

	private long lastRefreshTime;

	public Set<Sticker> getReplyStickers()
	{
		return replyStickers;
	}

	public void setReplyStickers(Set<Sticker> replyStickers)
	{
		this.replyStickers = replyStickers;
	}

	public Set<Sticker> getSentStickers()
	{
		return sentStickers;
	}

	public void setSentStickers(Set<Sticker> sentStickers)
	{
		this.sentStickers = sentStickers;
	}

	public boolean isShowReplyStickers() {
		return showReplyStickers;
	}

	private QuickSuggestionStickerCategory(Init<?> builder)
	{
		super(builder);
		this.quickSuggestSticker = builder.quickSuggestSticker;
		this.replyStickers = builder.replyStickers;
		this.sentStickers = builder.sentStickers;
		this.showReplyStickers = builder.showReplyStickers;
		ensureDefaults();
	}

	private void ensureDefaults()
	{
		setCustom(true);
	}

	public long getLastRefreshTime()
	{
		return lastRefreshTime;
	}

	public void setLastRefreshTime(long lastRefreshTime)
	{
		this.lastRefreshTime = lastRefreshTime;
	}

	protected static abstract class Init<S extends Init<S>> extends CustomStickerCategory.Init<S>
	{
		private Set<Sticker> replyStickers;

		private Set<Sticker> sentStickers;

		private Sticker quickSuggestSticker;

		private boolean showReplyStickers;

		private long lastRefreshTime;

		public S setReplyStickerSet(Set<Sticker> stickerSet)
		{
			this.replyStickers = stickerSet;
			return self();
		}

		public S setSentStickerSet(Set<Sticker> stickerSet)
		{
			this.sentStickers = stickerSet;
			return self();
		}

		public S setQuickSuggestSticker(Sticker quickSuggestSticker)
		{
			this.quickSuggestSticker = quickSuggestSticker;
			return self();
		}

		public S showReplyStickers(boolean showReplyStickers)
		{
			this.showReplyStickers = showReplyStickers;
			return self();
		}

		public S setLastRefreshTime(long lastRefreshTime)
		{
			this.lastRefreshTime = lastRefreshTime;
			return self();
		}

		public QuickSuggestionStickerCategory build()
		{
			return new QuickSuggestionStickerCategory(this);
		}
	}

	public static class Builder extends Init<Builder>
	{
		@Override
		protected Builder self()
		{
			return this;
		}
	}

	public Sticker getQuickSuggestSticker()
	{
		return quickSuggestSticker;
	}

	@Override
	public void loadStickers()
	{
		QuickStickerSuggestionController.getInstance().loadQuickStickerSuggestions(this);
	}

	@Override
	public Set<Sticker> getStickerSet()
	{
		return showReplyStickers ? replyStickers : sentStickers;
	}

	@Override
	public List<Sticker> getStickerList()
	{
		if (showReplyStickers)
		{
			if (replyStickers == null)
			{
				loadStickers();
			}
		}
		else
		{
			if (sentStickers == null)
			{
				loadStickers();
			}
		}

		return showReplyStickers ? getReplyStickerList() : getSentStickerList();
	}

	@Override
	public void addSticker(Sticker st) {
	}

	@Override
	public void removeSticker(Sticker st) {
	}

	private ArrayList getReplyStickerList()
	{
		return replyStickers == null ? new ArrayList<Sticker>(0) : new ArrayList<>(replyStickers);
	}

	private ArrayList getSentStickerList()
	{
		return sentStickers == null ? new ArrayList<Sticker>(0) : new ArrayList<>(sentStickers);
	}

	@Override
	public int getDownloadedStickersCount()
	{
		return 0;
	}

	public Bundle toBundle()
	{
		Bundle bundle = new Bundle();

		bundle.putParcelable(HikeConstants.STICKER, quickSuggestSticker);
		bundle.putParcelableArrayList(HikeConstants.REPLY, getReplyStickerList());
		bundle.putParcelableArrayList(HikeConstants.SENT, getSentStickerList());
		bundle.putString(HikeConstants.CATEGORY_ID, getCategoryId());
		bundle.putLong(HikeConstants.LAST_REFRESH_TIME, lastRefreshTime);
		bundle.putBoolean(HikeConstants.SHOW_QUICK_STICKER_SUGGESTION_ON_STICKER_RECEIVE, showReplyStickers);

		return bundle;
	}

	public static QuickSuggestionStickerCategory fromBundle(Bundle bundle)
	{
		ArrayList<Sticker> replyStickers = bundle.getParcelableArrayList(HikeConstants.REPLY);
		ArrayList<Sticker> sentStickers = bundle.getParcelableArrayList(HikeConstants.SENT);

		QuickSuggestionStickerCategory quickSuggestionStickerCategory = new QuickSuggestionStickerCategory.Builder()
				.setQuickSuggestSticker((Sticker) bundle.getParcelable(HikeConstants.STICKER)).setReplyStickerSet(new LinkedHashSet<>(replyStickers))
				.setSentStickerSet(new LinkedHashSet<>(sentStickers)).setCategoryId(bundle.getString(HikeConstants.CATEGORY_ID))
				.setLastRefreshTime(bundle.getLong(HikeConstants.LAST_REFRESH_TIME))
				.showReplyStickers(bundle.getBoolean(HikeConstants.SHOW_QUICK_STICKER_SUGGESTION_ON_STICKER_RECEIVE))
				.build();
		return quickSuggestionStickerCategory;
	}

	public String replyStickerSetToString()
	{
		JSONArray jsonArray = new JSONArray();
		for (Sticker sticker : replyStickers)
		{
			QuickSuggestionSticker quickSuggestionSticker = (QuickSuggestionSticker) sticker;
			jsonArray.put(quickSuggestionSticker.toJSON());
		}
		return jsonArray.toString();
	}

	public String sentStickerSetToSting()
	{
		JSONArray jsonArray = new JSONArray();
		for (Sticker sticker : sentStickers)
		{
			QuickSuggestionSticker quickSuggestionSticker = (QuickSuggestionSticker) sticker;
			jsonArray.put(quickSuggestionSticker.toJSON());
		}
		return jsonArray.toString();
	}

	public static Set<Sticker> replyStickerSetFromString(String jsonString)
	{
		Set<Sticker> replyStickers = new LinkedHashSet<>(8); // got this number through rigorous research
		try
		{
			JSONArray jsonArray = new JSONArray(jsonString);
			for (int i = 0; i < jsonArray.length(); i++)
			{
				Sticker sticker = QuickSuggestionSticker.fromJSON(jsonArray.getJSONObject(i));
				if (sticker != null)
				{
					replyStickers.add(sticker);
				}
			}
		}
		catch (JSONException | NullPointerException e)
		{
			Logger.e(TAG, "exception in deserialization reply sticker set ", e);
		}
		return replyStickers;
	}

	public static Set<Sticker> sentStickerSetFromSting(String jsonString)
	{
		Set<Sticker> sentStickers = new LinkedHashSet<>(0);
		try
		{
			JSONArray jsonArray = new JSONArray(jsonString);
			for (int i = 0; i < jsonArray.length(); i++)
			{
				Sticker sticker = QuickSuggestionSticker.fromJSON(jsonArray.getJSONObject(i));
				if (sticker != null)
				{
					sentStickers.add(sticker);
				}
			}
		}
		catch (JSONException | NullPointerException e)
		{
			Logger.e(TAG, "exception in deserialization of sent sticker set ", e);
		}
		return sentStickers;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QuickSuggestionStickerCategory other = (QuickSuggestionStickerCategory) obj;
		if (getCategoryId() == null)
		{
			if (other.getCategoryId() != null)
				return false;
		}
		else if (!getCategoryId().equals(other.getCategoryId()))
		{
			return false;
		}

		if (getQuickSuggestSticker() == null)
		{
			if (other.getQuickSuggestSticker() != null)
				return false;
		}
		else if (!getQuickSuggestSticker().equals(other.getQuickSuggestSticker()))
		{
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(StickerCategory another) {
		return -1;
	}
}