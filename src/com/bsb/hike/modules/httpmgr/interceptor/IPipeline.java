package com.bsb.hike.modules.httpmgr.interceptor;

/**
 * Interface for maintaining the behavior of for add/remove/get from the {@link Pipeline}
 * 
 * @author sidharth
 * 
 * @param <T>
 */
public interface IPipeline<T>
{
	public void addFirst(String tag, T element);

	public void addLast(String tag, T element);

	public void addBefore(String tag, String newTag, T element);

	public void addAfter(String tag, String newTag, T element);

	public T getFirst();

	public T getLast();

	public T get(String tag);

	public void removeFirst();

	public void removeLast();

	public void remove(String tag);

	public void replace(String tag, String newTag, T element);
}
