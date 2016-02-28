/**
 * File   : StickerEventDataContainer.java
 * Content: It is a container class to store festive data for a particular sticker.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.datamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.utils.Utils;
import com.hike.transporter.utils.Logger;

public class StickerEventDataContainer
{
	private static final String TAG = StickerEventDataContainer.class.getSimpleName();

	private String mEventId;

	private JSONArray mOtherNames;

	private JSONArray mTimeStampRanges;

	private JSONArray mDayRanges;

	private JSONArray mTimeStampRangesRanks;

	private JSONArray mDayRangesRanks;

	public StickerEventDataContainer(String eventId, JSONObject eventData)
	{
		// Fetch all alternate names of the event
		JSONArray alternateEventNameArray = eventData.optJSONArray(StickerSearchConstants.KEY_EVENT_NAMES);

		// Fetch all possible date-time ranges of current event with ranks (if available)
		JSONArray timeStampRangeDataArray = eventData.optJSONArray(StickerSearchConstants.KEY_EVENT_RANGE_TIME);

		// Fetch all possible day ranges of current event with ranks (if available)
		JSONArray dayRangeDataArray = eventData.optJSONArray(StickerSearchConstants.KEY_EVENT_RANGE_DAY);

		populateEventData(eventId, alternateEventNameArray, timeStampRangeDataArray, dayRangeDataArray);
	}

	public StickerEventDataContainer(String eventId, String alternateEventNames, String ranges)
	{
		try
		{
			JSONArray alternateEventNameArray = new JSONArray(alternateEventNames);
			JSONObject rangeJSONObject = new JSONObject(ranges);

			// Fetch all possible date-time ranges of current event with ranks (if available)
			JSONArray timeStampRangeDataArray = rangeJSONObject.optJSONArray(StickerSearchConstants.KEY_EVENT_RANGE_TIME);

			// Fetch all possible day ranges of current event with ranks (if available)
			JSONArray dayRangeDataArray = rangeJSONObject.optJSONArray(StickerSearchConstants.KEY_EVENT_RANGE_DAY);

			populateEventData(eventId, alternateEventNameArray, timeStampRangeDataArray, dayRangeDataArray);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private void populateEventData(String eventId, JSONArray alternateEventNameArray, JSONArray timeStampRangeDataArray, JSONArray dayRangeDataArray)
	{
		mEventId = (eventId == null) ? null : eventId.trim();
		mOtherNames = alternateEventNameArray;
		mTimeStampRanges = new JSONArray(); // Range only data i.e. list of start-end pairs in the form of time-stamp
		mDayRanges = new JSONArray();  // Range only data i.e. list of start-end pairs in the form of day
		mTimeStampRangesRanks = new JSONArray();  // Rank only data i.e. list of ranks for each range listed in mTimeStampRanges
		mDayRangesRanks = new JSONArray(); // Rank only data i.e. list of ranks for each range listed in mDayRanges

		if (!populateRangesAndRanks(timeStampRangeDataArray, mTimeStampRanges, mTimeStampRangesRanks, false))
		{
			mTimeStampRanges = null;
			mTimeStampRangesRanks = null;
		}

		if (!populateRangesAndRanks(dayRangeDataArray, mDayRanges, mDayRangesRanks, true))
		{
			mDayRanges = null;
			mDayRangesRanks = null;
		}
	}

	private boolean populateRangesAndRanks(JSONArray source, JSONArray targetRanges, JSONArray targetRanks, boolean isDayRange)
	{
		int count = (source == null) ? 0 : source.length();
		List<Range> rangeDataList = new ArrayList<Range>();
		JSONObject range;

		for (int i = 0; i < count; i++)
		{
			range = source.optJSONObject(i);
			if (range != null)
			{
				Range r = new Range(range, isDayRange);
				if (r.isValidRange())
				{
					if (!rangeDataList.contains(r))
					{
						rangeDataList.add(r);
					}
				}
				else
				{
					Logger.e(TAG, "populateRangesAndRanks(), Event id: " + mEventId + ", Wrong range: " + range);
				}
			}
		}

		count = rangeDataList.size();
		if (count > 0)
		{
			Collections.sort(rangeDataList);

			for (int i = 0; i < count; i++)
			{
				Range r = rangeDataList.get(i);

				// Skip the range, if it is overlapping (contained) in another range (i.e. next range as ranges are sorted)
				if ((i < (count - 1)) && (r.getStart() > Range.TIME_APPLICABILITY_BOUNDARY))
				{
					Range rnext = rangeDataList.get(i + 1);

					// Check overlapping in both type of ranges (time-stamp and day ranges)
					if (r.getStart() == rnext.getStart())
					{
						continue;
					}

					// Check overlapping (rotation) specially for day ranges
					if (isDayRange)
					{
						// Check rotation (Cases where range overlap between 2 different weeks, e.g. Friday to Monday)
						if ((rnext.getEnd() > Range.TIME_APPLICABILITY_BOUNDARY) && (rnext.getStart() > rnext.getEnd()))
						{
							if (r.getEnd() <= rnext.getEnd())
							{
								continue;
							}
						}
					}
				}

				JSONObject json = new JSONObject();
				try
				{
					if (r.getStart() > Range.TIME_APPLICABILITY_BOUNDARY)
					{
						json.put(StickerSearchConstants.KEY_EVENT_RANGE_START, r.getStart());
					}

					if (r.getEnd() > Range.TIME_APPLICABILITY_BOUNDARY)
					{
						json.put(StickerSearchConstants.KEY_EVENT_RANGE_END, r.getEnd());
					}

					targetRanges.put(json);
					targetRanks.put(r.getRank());
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}

			// Invalidate event data, if no valid range was found in received data
			if (targetRanges.length() == 0)
			{
				return false;
			}
		}

		return true;
	}

	public String getEventId()
	{
		return mEventId;
	}

	public String getOtherNames()
	{
		return (mOtherNames == null) ? null : mOtherNames.toString();
	}

	public String getRangeJSONString()
	{
		try
		{
			JSONObject json = new JSONObject();
			if (mTimeStampRanges != null)
			{
				json.put(StickerSearchConstants.KEY_EVENT_RANGE_TIME, mTimeStampRanges);
			}
			if (mDayRanges != null)
			{
				json.put(StickerSearchConstants.KEY_EVENT_RANGE_DAY, mDayRanges);
			}

			if (json.length() > 0)
			{
				return json.toString();
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		return null;
	}

	public JSONArray getTimeStampEventsRanks()
	{
		return mTimeStampRangesRanks;
	}

	public JSONArray getDayEventsRanks()
	{
		return mDayRangesRanks;
	}

	public boolean isValidData()
	{
		boolean result = (!Utils.isBlank(mEventId) && ((mTimeStampRanges != null) || (mDayRanges != null)));

		if (!result)
		{
			Logger.e(TAG, "Event data is incorrect for festival id: " + mEventId);
		}

		return result;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;

		/* Computation must be followed in same order as used in equals() to avoid collision due to same hashCode generated for unequal object */
		result = prime * result + ((mEventId == null) ? 0 : mEventId.hashCode());
		result = prime * result + ((mOtherNames == null) ? 0 : mOtherNames.hashCode());
		result = prime * result + ((mTimeStampRanges == null) ? 0 : mTimeStampRanges.toString().hashCode());
		result = prime * result + ((mDayRanges == null) ? 0 : mTimeStampRangesRanks.toString().hashCode());

		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}

		if (obj == null)
		{
			return false;
		}

		if (getClass() != obj.getClass())
		{
			return false;
		}

		StickerEventDataContainer other = (StickerEventDataContainer) obj;

		/* Compare in order of raw data types to derived data types i.e. comparison must be done earlier for those data types, which takes low comparison-processing time */
		/* Like order can be: Numeric types ---> Strings ---> Collections of numeric values ---> Collections of Strings or, derived classes and so on */
		if (mEventId == null)
		{
			if (other.mEventId != null)
			{
				return false;
			}
		}
		else if (!mEventId.equals(other.mEventId))
		{
			return false;
		}

		if (mOtherNames == null)
		{
			if (other.mOtherNames != null)
			{
				return false;
			}
		}
		else if (!mOtherNames.equals(other.mOtherNames))
		{
			return false;
		}

		if (mTimeStampRanges == null)
		{
			if (other.mTimeStampRanges != null)
			{
				return false;
			}
		}
		else if (other.mTimeStampRanges == null)
		{
			return false;
		}
		else if ((!mTimeStampRanges.toString().equals(other.mTimeStampRanges.toString())))
		{
			return false;
		}

		if (mDayRanges == null)
		{
			if (other.mDayRanges != null)
			{
				return false;
			}
		}
		else if (other.mDayRanges == null)
		{
			return false;
		}
		else if ((!mDayRanges.toString().equals(other.mDayRanges.toString())))
		{
			return false;
		}

		return true;
	}

	@Override
	public String toString()
	{
		return "[event: " + mEventId + ", <names=" + mOtherNames + "><rt=" + mTimeStampRanges + "><rtr=" + mTimeStampRangesRanks + "><rd=" + mDayRanges + "><rdr=" + mDayRangesRanks
				+ ">]";
	}

	public static class Range implements Comparable<Range>
	{
		public static final long TIME_APPLICABILITY_BOUNDARY = -1L;

		private long mStart;

		private long mEnd;

		private int mRank;

		private boolean isValidRange;

		private Range(JSONObject rangeData, boolean isDayRange)
		{
			mStart = rangeData.optLong(StickerSearchConstants.KEY_EVENT_RANGE_START, TIME_APPLICABILITY_BOUNDARY);
			mEnd = rangeData.optLong(StickerSearchConstants.KEY_EVENT_RANGE_END, TIME_APPLICABILITY_BOUNDARY);
			mRank = rangeData.optInt(StickerSearchConstants.KEY_EVENT_RANK, StickerSearchConstants.MAX_RANK_DURING_EVENT);
			if (isDayRange)
			{
				isValidRange = isValidDay(mStart) && isValidDay(mEnd);
			}
			else
			{
				isValidRange = true;

				if (mStart > TIME_APPLICABILITY_BOUNDARY)
				{
					if ((mEnd > TIME_APPLICABILITY_BOUNDARY) && (mEnd <= mStart))
					{
						isValidRange = false;
					}
				}
				else
				{
					if (mEnd > TIME_APPLICABILITY_BOUNDARY)
					{
						isValidRange = false;
					}
				}
			}
		}

		private boolean isValidDay(long dayId)
		{
			return (dayId >= HikeStickerSearchBaseConstants.DAY.SUNDAY.getId()) && (dayId <= HikeStickerSearchBaseConstants.DAY.SATURDAY.getId());
		}

		public long getStart()
		{
			return mStart;
		}

		public long getEnd()
		{
			return mEnd;
		}

		public int getRank()
		{
			return mRank;
		}

		public boolean isValidRange()
		{
			return isValidRange;
		}

		@Override
		/*
		 * LHS = RHS ==> return 0; LHS > RHS ==> return 1; LHS < RHS ==> return -1;
		 */
		public int compareTo(Range obj)
		{
			if ((obj == null) || (mStart > obj.mStart))
			{
				return 1;
			}
			else if (mStart == obj.mStart)
			{
				if (mEnd == obj.mEnd)
				{
					return 0;
				}
				else if (mEnd > obj.mEnd)
				{
					return 1;
				}
			}

			return -1;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;

			result = prime * result + (int) (mEnd ^ (mEnd >>> 32));
			result = prime * result + (int) (mStart ^ (mStart >>> 32));

			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}

			if (obj == null)
			{
				return false;
			}

			if (getClass() != obj.getClass())
			{
				return false;
			}

			Range other = (Range) obj;

			if (mStart != other.mStart)
			{
				return false;
			}

			if (mEnd != other.mEnd)
			{
				return false;
			}

			return true;
		}
	}
}