package com.bsb.hike.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class MovingList<T extends Unique> implements Collection<T>
{

	public interface OnItemsFinishedListener
	{
		public void getMoreItems(MovingList<? extends Unique> movingList, int startIndex, int endIndex);
	}

	OnItemsFinishedListener mOnItemsFinishedListener;

	ArrayList<Unique> items;

	ArrayList<Long> uniqueIds;

	int LoadBufferSize = 1;

	public MovingList(Collection<? extends Unique> collection, OnItemsFinishedListener onItemsFinishedListener)
	{
		this(collection, null, onItemsFinishedListener);
	}

	/**
	 * Constructs a new instance of {@code MovingList} containing the elements of the specified collection.
	 *
	 * @param collection
	 *            the collection of elements to add.
	 */
	public MovingList(Collection<? extends Unique> collection, Collection<Long> ids, OnItemsFinishedListener onItemsFinishedListener)
	{
		items = new ArrayList<Unique>(collection);
		this.mOnItemsFinishedListener = onItemsFinishedListener;
		if (ids == null)
		{
			uniqueIds = getIds(items);
		}
		else
		{
			uniqueIds = new ArrayList<Long>(ids);
		}
	}

	public static ArrayList<Long> getIds(List<? extends Unique> list)
	{
		ArrayList<Long> uniqueIdList = new ArrayList<Long>();
		for (int i = 0; i < list.size(); i++)
		{
			uniqueIdList.add(list.get(i).getUniqueId());
		}
		return uniqueIdList;
	}

	/**
	 * Adds the specified object at the end of this {@code MovingList}.
	 *
	 * @param object
	 *            the object to add.
	 * @return always true
	 */
	@Override
	public boolean add(T object)
	{
		return (items.add(object) && uniqueIds.add(object.getUniqueId()));
	}

	/**
	 * Inserts the specified object into this {@code MovingList} at the specified location. The object is inserted before any previous element at the specified location. If the
	 * location is equal to the size of this {@code ArrayList}, the object is added at the end.
	 *
	 * @param index
	 *            the index at which to insert the object.
	 * @param object
	 *            the object to add.
	 * @throws IndexOutOfBoundsException
	 *             when {@code location < 0 || location > size()}
	 */
	public void add(int index, T object)
	{
		add(index, object, object.getUniqueId());
	}

	/**
	 * Inserts the specified object and the corresponding {@link Unique} Id into this {@code MovingList} at the specified location. The object is inserted before any previous
	 * element at the specified location. If the location is equal to the size of this {@code ArrayList}, the object is added at the end.
	 *
	 * @param index
	 *            the index at which to insert the object.
	 * @param object
	 *            the object to add.
	 * @param id
	 *            the unique id of the object.
	 * @throws IndexOutOfBoundsException
	 *             when {@code location < 0 || location > size()}
	 */
	public void add(int index, T object, long id)
	{
		items.add(index, object);
		uniqueIds.add(index, id);
	}

	/**
	 * Adds the objects in the specified collection to this {@code MovingList}.
	 *
	 * @param collection
	 *            the collection of objects.
	 * @return {@code true} if this {@code ArrayList} is modified, {@code false} otherwise.
	 */
	@Override
	public boolean addAll(Collection<? extends T> collection)
	{
		return addAll(size(), collection);
	}

	/**
	 * Inserts the objects in the specified collection at the specified location in this List. The objects are added in the order they are returned from the collection's iterator.
	 *
	 * @param index
	 *            the index at which to insert.
	 * @param collection
	 *            the collection of objects.
	 * @return {@code true} if this {@code ArrayList} is modified, {@code false} otherwise.
	 * @throws IndexOutOfBoundsException
	 *             when {@code location < 0 || location > size()}
	 */
	public boolean addAll(int index, Collection<? extends T> collection)
	{
		return addAll(index, collection, null);
	}

	/**
	 * Inserts the objects in the specified collection at the specified location in this List. The objects are added in the order they are returned from the collection's iterator.
	 *
	 * @param index
	 *            the index at which to insert.
	 * @param collection
	 *            the collection of objects.
	 * @param ids
	 *            the collection of respective Ids.
	 * @return {@code true} if this {@code ArrayList} is modified, {@code false} otherwise.
	 * @throws IndexOutOfBoundsException
	 *             when {@code location < 0 || location > size()}
	 */
	public boolean addAll(int index, Collection<? extends T> collection, Collection<Long> ids)
	{
		ArrayList<T> list = new ArrayList<T>(collection);
		if (ids == null)
			ids = getIds(list);
		return (items.addAll(index, list) && uniqueIds.addAll(index, ids));
	}

	private void requestMoreItems(int index)
	{
		if (mOnItemsFinishedListener != null)
		{
			int a = Math.max(0, index - LoadBufferSize);
			while (getRaw(a) != null)
			{
				a++;
			}
			int b = Math.min(size() - 1, index + LoadBufferSize);
			while (getRaw(b) != null)
			{
				b--;
			}
			mOnItemsFinishedListener.getMoreItems(this, a, b);
		}
	}

	/**
	 * Returns the element at the specified location in this list. If the element found is null, a synchronous call is made to fetch the element before returning it.
	 *
	 * @param index
	 *            the index of the element to return.
	 * @return the element at the specified index.
	 * @throws IndexOutOfBoundsException
	 *             if {@code location < 0 || location >= size()}
	 */
	@SuppressWarnings("unchecked")
	public T get(int index)
	{
		T item = (T) items.get(index);
		if (item == null)
		{
			requestMoreItems(index);
			item = (T) items.get(index);
		}
		return item;
	}

	/**
	 * Returns the element at the specified location in this list.
	 *
	 * @param location
	 *            the index of the element to return.
	 * @return the element at the specified index.
	 * @throws IndexOutOfBoundsException
	 *             if {@code location < 0 || location >= size()}
	 */
	@SuppressWarnings("unchecked")
	public T getRaw(int index)
	{
		return (T) items.get(index);
	}

	/**
	 * Returns the unique id of the element at the specified location in this list.
	 *
	 * @param location
	 *            the index of the element to return.
	 * @return the element at the specified index.
	 * @throws IndexOutOfBoundsException
	 *             if {@code location < 0 || location >= size()}
	 */
	public long getUniqueId(int index)
	{
		Unique item = items.get(index);
		return (item != null) ? item.getUniqueId() : uniqueIds.get(index);
	}

	/**
	 * Replaces the element at the specified location in this {@code ArrayList} with the specified object.
	 *
	 * @param index
	 *            the index at which to put the specified object.
	 * @param object
	 *            the object to add.
	 * @return the previous element at the index.
	 * @throws IndexOutOfBoundsException
	 *             when {@code location < 0 || location >= size()}
	 */
	public T set(int index, T object)
	{
		return set(index, object, object.getUniqueId());
	}
	
	public T set(int index, T object, Long uniqueId)
	{
		@SuppressWarnings("unchecked")
		T result = (T) items.get(index);
		items.set(index, object);
		uniqueIds.set(index, uniqueId);
		return result;
	}

	/**
	 * Removes all elements from this {@code MovingList}, leaving it empty.
	 *
	 * @see #isEmpty
	 * @see #size
	 */
	@Override
	public void clear()
	{
		items.clear();
		uniqueIds.clear();
	}

    /**
     * Searches this {@code MovingList} for the specified object.
     *
     * @param object
     *            the object to search for.
     * @return {@code true} if {@code object} is an element of this
     *         {@code ArrayList}, {@code false} otherwise
     */
	@Override
	public boolean contains(Object object)
	{
		return items.contains(object);
	}

	/**
     * Tests whether this {@code Collection} contains all objects contained in the
     * specified {@code Collection}. This implementation iterates over the specified
     * {@code Collection}. If one element returned by the iterator is not contained in
     * this {@code Collection}, then {@code false} is returned; {@code true} otherwise.
     *
     * @param collection
     *            the collection of objects.
     * @return {@code true} if all objects in the specified {@code Collection} are
     *         elements of this {@code Collection}, {@code false} otherwise.
     * @throws ClassCastException
     *                if one or more elements of {@code collection} isn't of the
     *                correct type.
     * @throws NullPointerException
     *                if {@code collection} contains at least one {@code null}
     *                element and this {@code Collection} doesn't support {@code null}
     *                elements.
     * @throws NullPointerException
     *                if {@code collection} is {@code null}.
     */
	@Override
	public boolean containsAll(Collection<?> collection)
	{
		return items.containsAll(collection);
	}

	@Override
	public boolean isEmpty()
	{
		return (items.isEmpty() || uniqueIds.isEmpty());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<T> iterator()
	{
		return (Iterator<T>) items.iterator();
	}

	 /**
     * Removes the object at the specified location from this list.
     *
     * @param index
     *            the index of the object to remove.
     * @return the removed object.
     * @throws IndexOutOfBoundsException
     *             when {@code location < 0 || location >= size()}
     */
	@SuppressWarnings("unchecked")
	public T remove(int index)
	{
		uniqueIds.remove(index);
		return (T) items.remove(index);
	}

	/**
	 * Removes the first occurrence of the specified object from this List.
	 * @param object
	 * 				the object to remove
	 * @return {@code true} if this List was modified by this operation, {@code false} otherwise.
	 */
	@Override
	public boolean remove(Object object)
	{
		if (items.remove(object))
		{
			long idToRemove = ((Unique) object).getUniqueId();
			for (Long id : uniqueIds)
			{
				if (id.longValue() == idToRemove)
				{
					uniqueIds.remove(id);
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Does Nothing
	 */
	@Override
	public boolean removeAll(Collection<?> collection)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Does Nothing
	 */
	@Override
	public boolean retainAll(Collection<?> collection)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/**
     * Returns the number of elements in this {@code ArrayList}.
     *
     * @return the number of elements in this {@code ArrayList}.
     */
	@Override
	public int size()
	{
		return items.size();
	}

	/**
     * Returns a new array containing all elements contained in this
     * {@code MovingList}.
     *
     * @return an array of the elements from this {@code MovingList}
     */
	@Override
	public Object[] toArray()
	{
		return items.toArray();
	}

	/**
     * Returns an array containing all elements contained in this
     * {@code MovingList}. If the specified array is large enough to hold the
     * elements, the specified array is used, otherwise an array of the same
     * type is created. If the specified array is used and is larger than this
     * {@code MovingList}, the array element following the collection elements
     * is set to null.
     *
     * @param contents
     *            the array.
     * @return an array of the elements from this {@code MovingList}.
     * @throws ArrayStoreException
     *             when the type of an element in this {@code MovingList} cannot
     *             be stored in the type of the specified array.
     */
	@Override
	public <T> T[] toArray(T[] array)
	{
		return items.toArray(array);
	}

	public void setLoadBufferSize(int size)
	{
		LoadBufferSize = size;
	}

	/**
     * Searches this list for the specified object and returns the index of the
     * last occurrence.
     *
     * @param object
     *            the object to search for.
     * @return the index of the last occurrence of the object, or -1 if the
     *         object was not found.
     */
	public int lastIndexOf(T object)
	{
		return items.lastIndexOf(object);
	}

}
