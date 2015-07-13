package com.bsb.hike.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import android.text.TextUtils;

/**
 * SearchManager performs a search on a set of data according to the search logic
 * provided by the user.
 * @author gauravmittal
 */
public class SearchManager
{

	/**
	 * Contains the search logic to apply when searching in the data set
	 * To be used by the caller to provide the search logic.
	 * @author gauravmittal
	 */
	public interface Searchable
	{
		/**
		 * Checks if the item contains the search text.
		 * 
		 * @return If the item has the search text.
		 */
		boolean doesItemContain(String s);
	}

	private boolean active;

	public SearchManager()
	{
		super();
		active = true;
	}

	public void deactivate()
	{
		active = false;
	}

	public boolean isActive()
	{
		return active;
	}

	/**
	 * Searches for first item in the range provided.
	 * @param list
	 * 	list of {@link Searchable} items to search from
	 * @param from
	 * @param to
	 * @param searchText
	 * 	text to search for
	 * @return
	 * 		the index of the item if found, else -1.
	 */
	public int searchFirstItem(Collection<? extends Searchable> collection, int from, int to, String searchText)
	{
		ArrayList<? extends Searchable> list = new ArrayList<>(collection);
		from = checkNApplyBound(from, list.size());
		to = checkNApplyBound(to,list.size());
		if (from > to)
		{
			for (; from >= to; from--)
			{
				if (!active)
					break;

				if (list.get(from).doesItemContain(searchText))
				{
					return from;
				}
			}
		}
		else
		{
			for (; from <= to; from++)
			{
				if (!active)
					break;

				if (list.get(from).doesItemContain(searchText))
				{
					return from;
				}
			}
		}
		return -1;
	}

	/*
	 * Just a precaution.
	 * This is possible if the caller sends a junk call due to some reason.
	 */
	private int checkNApplyBound(int position, int size)
	{
		if (position >= size)
			return (size - 1);
		else if (position < 0)
			return 0;
		else
			return position;
	}
}
