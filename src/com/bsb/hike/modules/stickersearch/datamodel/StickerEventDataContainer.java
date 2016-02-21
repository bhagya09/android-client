/**
 * File   : StickerEventDataContainer.java
 * Content: It is a container class to store festive data for a particular sticker.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.datamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

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

	private String mOtherNames;

	private JSONArray mTimeStampRanges;

	private JSONArray mDayRanges;

	private JSONArray mTimeStampRangesRanks;

	private JSONArray mDayRangesRanks;

	public StickerEventDataContainer(String id, JSONArray names, JSONArray timeStampRangeDataArray, JSONArray dayRangeDataArray)
	{
		mEventId = id;
		mTimeStampRanges = new JSONArray();
		mDayRanges = new JSONArray();
		mTimeStampRangesRanks = new JSONArray();
		mDayRangesRanks = new JSONArray();

		populateAlternateNames(names);

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

	private void populateAlternateNames(JSONArray names)
	{
		if (names != null)
		{
			StringBuilder namesStringBuilder = new StringBuilder();
			String alternateName;

			for (int i = 0; i < names.length(); i++)
			{
				alternateName = names.optString(i);
				if (!Utils.isBlank(alternateName))
				{
					namesStringBuilder.append(alternateName.trim().toUpperCase(Locale.ENGLISH));
					namesStringBuilder.append(StickerSearchConstants.STRING_DISSOCIATOR);
				}
			}

			if (namesStringBuilder.length() > 0)
			{
				namesStringBuilder.setLength(namesStringBuilder.length() - 1);
			}

			mOtherNames = namesStringBuilder.toString();
		}
	}

	private boolean populateRangesAndRanks(JSONArray source, JSONArray targetRanges, JSONArray targetRanks, boolean isDayRange)
	{
		int count = (source == null) ? 0 : source.length();
		ArrayList<Range> rangeDataList = new ArrayList<Range>();
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
				if ((i < (count - 1)) && (r.getStart() > -1))
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
						if ((rnext.getEnd() > -1) && (rnext.getStart() > rnext.getEnd()))
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
					if (r.getStart() > -1)
					{
						json.put(StickerSearchConstants.KEY_EVENT_RANGE_START, r.getStart());
					}

					if (r.getEnd() > -1)
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
		return mOtherNames;
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

	public String appendAndGetTimeStampRangesRankJSONString(Long id, String existingTimeStampRangesRankString)
	{
		try
		{
			JSONObject json;
			if (!Utils.isBlank(existingTimeStampRangesRankString))
			{
				json = new JSONObject(existingTimeStampRangesRankString);
			}
			else
			{
				json = new JSONObject();
			}

			if (mTimeStampRangesRanks != null)
			{
				json.put(String.valueOf(id), mTimeStampRangesRanks);
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

	public String appendAndGetDayRangesRankJSONString(Long id, String existingDayRangesRankString)
	{
		try
		{
			JSONObject json;
			if (!Utils.isBlank(existingDayRangesRankString))
			{
				json = new JSONObject(existingDayRangesRankString);
			}
			else
			{
				json = new JSONObject();
			}

			if (mDayRangesRanks != null)
			{
				json.put(String.valueOf(id), mDayRangesRanks);
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
	public String toString()
	{
		return "[event: " + mEventId + ", <names=" + mOtherNames + "><rt=" + mTimeStampRanges + "><rtr=" + mTimeStampRangesRanks + "><rd=" + mDayRanges + "><rdr=" + mDayRangesRanks
				+ ">]";
	}

	private static class Range implements Comparable<Range>
	{
		private long mStart;

		private long mEnd;

		private int mRank;

		private boolean isValidRange;

		private Range(JSONObject rangeData, boolean isDayRange)
		{
			mStart = rangeData.optLong(StickerSearchConstants.KEY_EVENT_RANGE_START, -1L);
			mEnd = rangeData.optLong(StickerSearchConstants.KEY_EVENT_RANGE_END, -1L);
			mRank = rangeData.optInt(StickerSearchConstants.KEY_EVENT_RANK, StickerSearchConstants.MAX_RANK_DURING_EVENT);
			if (isDayRange)
			{
				isValidRange = isValidDay(mStart) && isValidDay(mEnd);
			}
			else
			{
				isValidRange = true;

				if (mStart > -1)
				{
					if ((mEnd > -1) && (mEnd <= mStart))
					{
						isValidRange = false;
					}
				}
				else
				{
					if (mEnd > -1)
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