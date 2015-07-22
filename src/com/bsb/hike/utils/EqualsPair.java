package com.bsb.hike.utils;

import android.util.Pair;

public class EqualsPair<F,S> extends Pair<F, S>
{
	public EqualsPair(F first, S second)
	{
		super(first, second);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof EqualsPair)
		{
			EqualsPair<F, S> pair = (EqualsPair<F, S>) o;
			return first.equals(pair.first) && second.equals(pair.second);
		}
		return false;
	}
}
