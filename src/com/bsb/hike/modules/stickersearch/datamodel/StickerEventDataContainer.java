/**
 * File   : StickerEventDataContainer.java
 * Content: It is a container class to store festive data for a particular sticker.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.datamodel;

import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bsb.hike.modules.stickersearch.StickerSearchConstants;
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
		populateRangesAndRanks(timeStampRangeDataArray, mTimeStampRanges, mTimeStampRangesRanks);
		populateRangesAndRanks(dayRangeDataArray, mDayRanges, mDayRangesRanks);
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

	private void populateRangesAndRanks(JSONArray source, JSONArray targetRanges, JSONArray targetRanks)
	{
		int count = (source == null) ? 0 : source.length();
		JSONObject range;

		for (int i = 0; i < count; i++)
		{
			range = source.optJSONObject(i);
			if (range != null)
			{
				int rank = range.optInt(StickerSearchConstants.KEY_EVENT_RANK, StickerSearchConstants.MAX_RANK_DURING_EVENT);
				range.remove(StickerSearchConstants.KEY_EVENT_RANK);
				targetRanges.put(range);
				targetRanks.put(rank);
			}
		}
	}

	public String getEventId()
	{
		return mEventId;
	}

	public String getOtherNames()
	{
		return mOtherNames;
	}

	public JSONArray getTimeStampRanges()
	{
		return mTimeStampRanges;
	}

	public JSONArray getDayRanges()
	{
		return mDayRanges;
	}

	public JSONArray getTimeStampRangesRanks()
	{
		return mTimeStampRangesRanks;
	}

	public JSONArray getDayRangesRanks()
	{
		return mDayRangesRanks;
	}

	public boolean isValidData()
	{
		boolean result = (!Utils.isBlank(mEventId) && isValidRanges(mTimeStampRanges) && isValidRanges(mDayRanges));

		if (!result)
		{
			Logger.e(TAG, "Event data is incorrect for festival id: " + mEventId);
		}

		return result;
	}

	private boolean isValidRanges(JSONArray rangeArray)
	{
		boolean result = true;
		int totalRanges = (rangeArray == null) ? 0 : rangeArray.length();
		JSONObject range;

		for (int i = 0; i < totalRanges; i++)
		{
			range = rangeArray.optJSONObject(i);
			int start = range.optInt(StickerSearchConstants.KEY_EVENT_RANGE_START, 0);
			int end = range.optInt(StickerSearchConstants.KEY_EVENT_RANGE_END, 0);

			if ((end > 0) && (end <= start))
			{
				result = false;
				break;
			}
		}

		return result;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;

		result = prime * result + ((mEventId == null) ? 0 : mEventId.hashCode());
		result = prime * result + ((mOtherNames == null) ? 0 : mOtherNames.hashCode());
		result = prime * result + ((mTimeStampRanges == null) ? 0 : mTimeStampRanges.hashCode());
		result = prime * result + ((mTimeStampRangesRanks == null) ? 0 : mTimeStampRangesRanks.hashCode());
		result = prime * result + ((mDayRanges == null) ? 0 : mDayRanges.hashCode());
		result = prime * result + ((mDayRangesRanks == null) ? 0 : mDayRangesRanks.hashCode());
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
		else if (!mTimeStampRanges.equals(other.mTimeStampRanges))
		{
			return false;
		}

		if (mTimeStampRangesRanks == null)
		{
			if (other.mTimeStampRangesRanks != null)
			{
				return false;
			}
		}
		else if (!mTimeStampRangesRanks.equals(other.mTimeStampRangesRanks))
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
		else if (!mDayRanges.equals(other.mDayRanges))
		{
			return false;
		}

		if (mDayRangesRanks == null)
		{
			if (other.mDayRangesRanks != null)
			{
				return false;
			}
		}
		else if (!mDayRangesRanks.equals(other.mDayRangesRanks))
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
}