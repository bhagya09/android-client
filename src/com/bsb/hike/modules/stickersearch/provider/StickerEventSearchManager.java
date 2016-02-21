/**
 * File   : StickerEventSearchManager.java
 * Content: It provides permanent gateway to provide functionality for events going to occur in near future.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchDatabase;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import android.util.Pair;

public enum StickerEventSearchManager
{
	INSTANCE;
	private static final String TAG = StickerEventSearchManager.class.getSimpleName();

	private static ConcurrentHashMap<Long, Event> sCacheForNowCastEvents = new ConcurrentHashMap<Long, Event>();

	private static long sLatestEventLoadingTime;

	public static StickerEventSearchManager getInstance()
	{
		return INSTANCE;
	}

	public void readNowCastEvents()
	{
		Logger.i(TAG, "init()");

		sLatestEventLoadingTime = System.currentTimeMillis();
		Map<Pair<Long, String>, Pair<String, String>> rawData = HikeStickerSearchDatabase.getInstance().readAllEventsData();

		int eventCount = (rawData == null) ? 0 : rawData.size();
		if (eventCount > 0)
		{
			Set<Pair<Long, String>> ids = rawData.keySet();
			long id;
			String eventId;
			Pair<String, String> eventData;
			Event event;

			for (Pair<Long, String> idPair : ids)
			{
				id = idPair.first;
				eventId = idPair.second;
				eventData = rawData.get(idPair);
				event = new Event(eventId, eventData.first, eventData.second);

				if (event.nowCast())
				{
					sCacheForNowCastEvents.put(id, event);
				}
			}

			Logger.d(TAG, "readNowCastEvents(), Total now cast events are " + sCacheForNowCastEvents.size());
		}
		else
		{
			Logger.d(TAG, "readNowCastEvents(), No event data found in database.");
		}
	}

	public int getNowCastTimeStampRangeRank(String eventRankListData)
	{
		if (!Utils.isBlank(eventRankListData))
		{
			try
			{
				JSONObject json = new JSONObject(eventRankListData);
				Iterator<String> ids = json.keys();
				if (ids != null)
				{
					while (ids.hasNext())
					{
						try
						{
							String idString = ids.next();
							long id = Long.parseLong(idString);
							Event event = sCacheForNowCastEvents.get(id);
							if (event != null)
							{
								int rankIndex = event.getTimeStampRangeIndex();
								JSONArray ranks = json.optJSONArray(idString);
								if ((ranks != null) && (rankIndex >= 0) && (rankIndex < ranks.length()))
								{
									return ranks.optInt(rankIndex, -1);
								}
							}
						}
						catch (NumberFormatException e)
						{
							e.printStackTrace();
						}
					}
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		return -1;
	}

	public void clearNowCastEvents()
	{
		sCacheForNowCastEvents.clear();
		sLatestEventLoadingTime = 0;
	}

	private static class Event
	{
		private String mEventName;

		private ArrayList<String> mOtherNames;

		private int mTimeStampRangeIndex = -1;

		private boolean mNowCast = false;

		private Event(String eventId, String names, String ranges)
		{
			mEventName = eventId;
			mOtherNames = StickerSearchUtility.split(names, StickerSearchConstants.REGEX_SPACE);

			if (!Utils.isBlank(ranges))
			{
				try
				{
					JSONObject json = new JSONObject(ranges);

					// Fetch all possible date-time ranges of the event
					JSONArray timeStampRanges = json.optJSONArray(StickerSearchConstants.KEY_EVENT_RANGE_TIME);
					int rangeCount = (timeStampRanges == null) ? 0 : timeStampRanges.length();

					if (rangeCount > 0)
					{
						JSONObject range;
						long start;
						long end;

						for (int i = (rangeCount - 1); i >= 0; i--)
						{
							range = timeStampRanges.optJSONObject(i);
							if (range != null)
							{
								start = range.optLong(StickerSearchConstants.KEY_EVENT_RANGE_START, -1L);
								end = range.optLong(StickerSearchConstants.KEY_EVENT_RANGE_END, (start + StickerSearchConstants.DEFAULT_EVENT_DURATION));

								if ((start > 0) && (StickerEventSearchManager.sLatestEventLoadingTime >= start) && (StickerEventSearchManager.sLatestEventLoadingTime < end))
								{
									mTimeStampRangeIndex = i;
									mNowCast = true;
									break;
								}
							}
						}
					}
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}
		}

		private String getEventName()
		{
			return mEventName;
		}

		private ArrayList<String> getOtherNames()
		{
			return mOtherNames;
		}

		private int getTimeStampRangeIndex()
		{
			return mTimeStampRangeIndex;
		}

		private boolean nowCast()
		{
			return mNowCast;
		}
	}
}