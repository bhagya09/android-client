package com.bsb.hike.modules.httpmgr.interceptor;

/**
 * A node (as in linked list) class of interceptor used in {@link Pipeline}
 * 
 * @author sidharth
 * 
 * @param <T>
 */
public class Interceptor<T>
{
	private String tag;

	private T element;

	Interceptor<T> next;

	Interceptor<T> prev;

	public Interceptor(T element, String tag, Interceptor<T> prev, Interceptor<T> next)
	{
		this.element = element;
		this.tag = tag;
		this.next = next;
		this.prev = prev;
	}

	public String tag()
	{
		return tag;
	}

	public T element()
	{
		return element;
	}

	void setElement(T element)
	{
		this.element = element;
	}

	void setTag(String tag)
	{
		this.tag = tag;
	}
}
