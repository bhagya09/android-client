package com.bsb.hike.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.bsb.hike.models.MovingList.OnItemsFinishedListener;

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

	@Override
	public boolean add(T object)
	{
		return (items.add(object) && uniqueIds.add(object.getUniqueId()));
	}

	public void add(int index, T object)
	{
		add(index, object, object.getUniqueId());
	}

	public void add(int index, T object, long id)
	{
		items.add(index, object);
		uniqueIds.add(index, id);
	}

	@Override
	public boolean addAll(Collection<? extends T> collection)
	{
		return addAll(size(), collection);
	}

	public boolean addAll(int index, Collection<? extends T> collection)
	{
		return addAll(index, collection, null);
	}

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
			int b = Math.min(size() - 1, index + LoadBufferSize);
			mOnItemsFinishedListener.getMoreItems(this, a, b);
		}
	}

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

	@SuppressWarnings("unchecked")
	public T getRaw(int index)
	{
		return (T) items.get(index);
	}

	public long getUniqueId(int index)
	{
		return uniqueIds.get(index);
	}

	public T set(int index, T object)
	{
		@SuppressWarnings("unchecked")
		T result = (T) items.get(index);
		items.set(index, object);
		uniqueIds.set(index, object.getUniqueId());
		return result;
	}

	@Override
	public void clear()
	{
		items.clear();
		uniqueIds.clear();
	}

	@Override
	public boolean contains(Object object)
	{
		return items.contains(object);
	}

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

	public T remove(int index)
	{
		uniqueIds.remove(index);
		return (T) items.remove(index);
	}
	
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

	@Override
	public boolean removeAll(Collection<?> collection)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> collection)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int size()
	{
		return items.size();
	}

	@Override
	public Object[] toArray()
	{
		return items.toArray();
	}

	@Override
	public <T> T[] toArray(T[] array)
	{
		return items.toArray(array);
	}

	public void setLoadBufferSize(int size)
	{
		LoadBufferSize = size;
	}

	public int lastIndexOf(T item)
	{
		return items.lastIndexOf(item);
	}

}
