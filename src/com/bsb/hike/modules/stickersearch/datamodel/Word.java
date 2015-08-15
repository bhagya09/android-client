/**
 * File   : Word.java
 * Content: It is a definition class to store a typed independent string in text.
 * @author  Ved Prakash Singh [ved@hike.in]
 */

package com.bsb.hike.modules.stickersearch.datamodel;

public class Word
{
	public static final int BOUNDARY_COUNT = 2;

	public static final int BOUNDARY_INDEX_START = 0;

	public static final int BOUNDARY_INDEX_END = 1;

	private String mValue;

	private int mStartIndex;

	private int mEndIndexExclusive;

	private int mEndIndexInclusive;

	public Word(String value, int start, int end)
	{
		mValue = value;
		mStartIndex = start;
		mEndIndexExclusive = end;
		mEndIndexInclusive = end - 1;
	}

	public String getValue()
	{
		return mValue;
	}

	public int getStart()
	{
		return mStartIndex;
	}

	public int getEnd()
	{
		return mEndIndexExclusive;
	}

	public int getRealEnd()
	{
		return mEndIndexInclusive;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;

		/* Computation must be followed in same order as used in equals() to avoid collision due to same hashCode generated for unequal object */
		result = prime * result + mStartIndex;
		result = prime * result + mEndIndexExclusive;
		result = prime * result + ((mValue == null) ? 0 : mValue.hashCode());

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

		Word other = (Word) obj;

		if (mStartIndex != other.mStartIndex)
		{
			return false;
		}

		if (mEndIndexExclusive != other.mEndIndexExclusive)
		{
			return false;
		}

		if (mValue == null)
		{
			if (other.mValue != null)
			{
				return false;
			}
		}
		else if (!mValue.equals(other.mValue))
		{
			return false;
		}

		return true;
	}

	@Override
	public String toString()
	{
		return "[" + mValue + ": [" + mStartIndex + ", " + mEndIndexExclusive + ")]";
	}
}