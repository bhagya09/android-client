/**
 * File   : StickerEventSearchManager.java
 * Content: It provides permanent gateway to provide functionality for events going to occur in near future.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.provider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.datamodel.StickerEventDataContainer.Range;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchDatabase;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public enum StickerEventSearchManager
{
	INSTANCE;
	private static final String TAG = StickerEventSearchManager.class.getSimpleName();

	private static ConcurrentHashMap<Long, Event> sCacheForNowCastEvents = new ConcurrentHashMap<Long, Event>();

	private static long sLatestEventLoadingTime = 0L; // In seconds since start of 1970

	private static int sLatestEventLoadingDay = 0; // Running Day of the week

	public static StickerEventSearchManager getInstance()
	{
		return INSTANCE;
	}

	private boolean idDayChanged()
	{
		int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
		return (sLatestEventLoadingDay != currentDay);
	}

	/**
	 * Load all events into the cache (events, which are likely to occur in near future === now cast events). This method will update such events to cache only, if at least 24
	 * hours have been passed from last loading or application is just started (first loading).
	 */
	public synchronized void loadNowCastEvents()
	{
		Logger.i(TAG, "loadNowCastEvents()");

		/* Following condition-check will be leading the events's loading only once in a day or at the start of application or while adding new events from server */
		if ((sLatestEventLoadingDay > 0) && !idDayChanged())
		{
			return;
		}

		sCacheForNowCastEvents.clear();
		sLatestEventLoadingTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		sLatestEventLoadingDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
		Map<Long, Event> rawData = HikeStickerSearchDatabase.getInstance().readAllEventsData();

		int eventCount = (rawData == null) ? 0 : rawData.size();
		if (eventCount > 0)
		{
			Set<Long> ids = rawData.keySet();
			Event event;

			for (Long id : ids)
			{
				event = rawData.get(id);
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

	public synchronized void clearNowCastEvents()
	{
		sCacheForNowCastEvents.clear();
		sLatestEventLoadingTime = 0L;
		sLatestEventLoadingDay = 0;
	}

	public static class Event
	{
		private String mEventName;

		private ArrayList<String> mOtherNames;

		private int mTimeStampRangeIndex = -1; // Which range is applicable in current time

		private boolean mNowCast = false;

		public Event(String eventId, String names, String ranges)
		{
			mEventName = eventId;

			if (!Utils.isBlank(names))
			{
				/* TODO */
				/* Implementation of personal events based on user's calendar data */
			}

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
								start = range.optLong(StickerSearchConstants.KEY_EVENT_RANGE_START, Range.TIME_APPLICABILITY_BOUNDARY);
								end = range.optLong(StickerSearchConstants.KEY_EVENT_RANGE_END, (start + StickerSearchConstants.DEFAULT_EVENT_DURATION));

								/* Duration of event is 1 second at least (theoretical value) */
								if ((start > 0) && (StickerEventSearchManager.sLatestEventLoadingTime >= start) && (StickerEventSearchManager.sLatestEventLoadingTime < (end - 1)))
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

		public String getEventName()
		{
			return mEventName;
		}

		public ArrayList<String> getOtherNames()
		{
			return mOtherNames;
		}

		public int getTimeStampRangeIndex()
		{
			return mTimeStampRangeIndex;
		}

		public boolean nowCast()
		{
			return mNowCast;
		}
	}
}