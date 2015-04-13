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

	private ArrayList<Integer> indexList;

	private String searchText;

	private int defaultItemViewBacklash = 1;

	// itemViewBacklash can not be negative.
	private int itemViewBacklash;

	private ArrayList<Searchable> itemList;

	public SearchManager()
	{
		this.indexList = new ArrayList<Integer>();
	}
	
	public void init(Collection<? extends Searchable> collection)
	{
		this.itemList = new ArrayList<Searchable>(collection);
		clearSearch();
	}

	public void makeNewSearch(String s)
	{
		searchText = s;
		indexList.clear();
	}

	public void clearSearch()
	{
		makeNewSearch("");
	}

	public String getSearchText()
	{
		return searchText;
	}

	public void setItemViewBacklash(int backlash)
	{
		itemViewBacklash = backlash;
	}

	public int getNextItem(int cursorPosition, int lastPosition)
	{
		// if the search text is empty, no need to perform any search.
		if (TextUtils.isEmpty(searchText) || lastPosition < cursorPosition)
		{
			return -1;
		}
		itemViewBacklash = defaultItemViewBacklash;
		// View backlash cannot be greater than the number of visible items.
		if (itemViewBacklash >= lastPosition-cursorPosition)
		{
			itemViewBacklash = lastPosition-cursorPosition;
		}
		Logger.d("search","first last:" + cursorPosition + lastPosition);
		Logger.d("search", "nextMessage()");
		Logger.d("search", "list: " + indexList.toString());
		int searchCusrsor = getCurrentCursor(cursorPosition);
		Logger.d("search", "currentCusrsor: " + searchCusrsor);

		int start, end, threshold = -1;
		int lastMessageIndex = itemList.size() - 1;
		// If the index list is empty, this is first request.
		// Make a fresh search.
		if (indexList.isEmpty())
		{
			start = searchCusrsor;
			end = lastMessageIndex;
			threshold = searchCusrsor;
		}
		else
		{
			int lastIndex = indexList.get(indexList.size() - 1);
			int firstIndex = indexList.get(0);
			// If current position is after our selection range
			if (searchCusrsor >= lastIndex)
			{
				start = lastIndex + 1;
				end = lastMessageIndex;
				threshold = searchCusrsor;
			}
			// If current position is before our selection range
			else if (searchCusrsor < firstIndex)
			{
				start = searchCusrsor;
				end = firstIndex - 1;
			}
			// If current position is within our selection range
			// No need to perform search again
			else
			{
				return getNextIndexItem(searchCusrsor);
			}
		}
		boolean found = false;
		if (end == lastMessageIndex)
		{
			found = searchFirstMessage(start, end, threshold);
		}
		else
		{
			found = searchAllMessages(start, end);
		}
		return getNextIndexItem(searchCusrsor);
	}
	
	private int getNextIndexItem(int searchCusrsor)
	{
		int position = Collections.binarySearch(indexList, searchCusrsor);
		Logger.d("search", "position: " + position);
		if (position >= 0)
		{
			position++;
		}
		else
		{
			position *= (-1);
			position -= 1;
		}
		if (position >= 0 && position < indexList.size())
		{
			return applyBackLash(indexList.get(position));
		}
		else
		{
			return -1;
		}
	}

	public int getPrevItem(int cursorPosition)
	{
		if (TextUtils.isEmpty(searchText))
		{
			return -1;
		}
		itemViewBacklash = defaultItemViewBacklash;
		Logger.d("search", "prevMessage()");
		Logger.d("search", "list: " + indexList.toString());
		int searchCusrsor = getCurrentCursor(cursorPosition);
		Logger.d("search", "currentCusrsor: " + searchCusrsor);
		int position = Collections.binarySearch(indexList, searchCusrsor);
		Logger.d("search", "position: " + position);

		int start, end, threshold = -1;
		// If the index list is empty, this is first request.
		// Make a fresh search.
		if (indexList.isEmpty())
		{
			start = searchCusrsor;
			end = 0;
			threshold = searchCusrsor;
		}
		else
		{
			int lastIndex = indexList.get(indexList.size() - 1);
			int firstIndex = indexList.get(0);
			// If current position is after our selection range
			if (searchCusrsor > lastIndex)
			{
				start = searchCusrsor;
				end = lastIndex + 1;
				threshold = searchCusrsor;
			}
			// If current position is before our selection range
			else if (searchCusrsor <= firstIndex)
			{
				start = firstIndex - 1;
				end = 0;
				threshold = searchCusrsor;
			}
			// If current position is within our selection range
			// No need to perform search again
			else
			{
				return getPrevIndexItem(searchCusrsor);
			}
		}
		boolean found = false;
		if (end == 0)
		{
			found = searchFirstMessage(start, end, threshold);
		}
		else
		{
			found = searchAllMessages(start, end);
		}
		return getPrevIndexItem(searchCusrsor);
	}
	
	private int getPrevIndexItem(int searchCusrsor)
	{
		int position = Collections.binarySearch(indexList, searchCusrsor);
		if (position >= 0)
		{
			position--;
		}
		else
		{
			position *= (-1);
			position -= 2;
		}
		if (position >= 0 && position < indexList.size())
		{
			return applyBackLash(indexList.get(position));
		}
		else
		{
			return -1;
		}
	}

	/**
	 * Gets the cursor position about which the search is to be performed.
	 * Adds a backlash if required.
	 * @param cursorPosition
	 * 		as provided by the caller.
	 * @return
	 */
	private int getCurrentCursor(int cursorPosition)
	{
		if ((cursorPosition + itemViewBacklash) < itemList.size())
		{
			return (cursorPosition + itemViewBacklash);
		}
		else
		{
			return cursorPosition;
		}
	}

	/**
	 * Search all items in the range provided.
	 * Updates the index list accordingly.
	 * @param from
	 * @param to
	 * @return
	 * 		false if not item is found, true otherwise.
	 */
	private boolean searchAllMessages(int from, int to)
	{
		boolean found = false;
		if (from > to)
		{
			from ^= to ^= from ^= to;
		}
		for (; from <= to; from++)
		{
			// Just a precaution.
			// This is possible if the caller sends a junk call due to some reason.
			if (from >= itemList.size())
				continue;

			if (itemList.get(from).doesItemContain(searchText))
			{
				Logger.d("search", "adding: " + from);
				if (!indexList.contains(from))
				{
					indexList.add(from);
					Collections.sort(indexList);
				}
				found = true;
			}
		}
		return found;
	}

	/**
	 * Searches for first item after the threshold in the range provided.
	 * Updates indexList on its way.
	 * @param from
	 * @param to
	 * @param threshold
	 * @return
	 * 		false if not item is found, true otherwise.
	 */
	private boolean searchFirstMessage(int from, int to, int threshold)
	{
		Logger.d("search", "searching first in range: " + from + "-" + to + " with threshold of " + threshold);
		if (from > to)
		{
			for (; from >= to; from--)
			{
				// Just a precaution.
				// This is possible if the caller sends a junk call due to some reason.
				if (from >= itemList.size())
					continue;

				if (itemList.get(from).doesItemContain(searchText))
				{
					Logger.d("search", "adding: " + from);
					if (!indexList.contains(from))
					{
						indexList.add(from);
						Collections.sort(indexList);
					}
					if (threshold >= 0)
					{
						if (from < threshold)
						{
							return true;
						}
					}
					else
					{
						return true;
					}
				}
			}
			return false;
		}
		else
		{
			for (; from <= to; from++)
			{
				// Just a precaution.
				// This is possible if the caller sends a junk call due to some reason.
				if (from >= itemList.size())
					continue;

				if (itemList.get(from).doesItemContain(searchText))
				{
					Logger.d("search", "adding: " + from);
					if (!indexList.contains(from))
					{
						indexList.add(from);
						Collections.sort(indexList);
					}
					if (threshold >= 0)
					{
						if (from > threshold)
						{
							return true;
						}
					}
					else
					{
						return true;
					}
				}
			}
			return false;
		}
	}

	private int applyBackLash(int position)
	{
		Logger.d("search","itemViewBacklash: " + itemViewBacklash);
		if (position <= itemViewBacklash)
			position = 0;
		else
			position -= itemViewBacklash;

		return position;
	}

	/**
	 * Update the itemList with the new set of data.
	 * @param collection
	 */
	public void updateDataSet(Collection<? extends Searchable> collection)
	{
		itemList = new ArrayList<>(collection);
	}

	/**
	 * Updates the existing index list.
	 * This is called when new element are added to the item collection set.
	 * @param count
	 */
	public void updateIndex(int count)
	{
		for (int i=0; i < indexList.size(); i++)
		{
			indexList.set(i, indexList.get(i) + count );
		}
	}

}
