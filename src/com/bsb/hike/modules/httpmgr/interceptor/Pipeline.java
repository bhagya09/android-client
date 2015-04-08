package com.bsb.hike.modules.httpmgr.interceptor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

/**
 * This class implements a doubly linked list and also contains a map which has tag of a node as key and node as it's value for O(1) access to node using a given tag. It is used
 * for maintaining request interceptor chain and response interceptor chain
 * 
 * @author sidharth
 * 
 * @param <T>
 */
public class Pipeline<T> implements IPipeline<T>, Iterable<T>
{
	private static final String HEAD_TAG = "head";

	private static final String TAIL_TAG = "tail";

	private Map<String, Interceptor<T>> map;

	private Interceptor<T> head;

	private Interceptor<T> tail;

	public Pipeline()
	{
		map = new HashMap<String, Interceptor<T>>();
		head = new Interceptor<T>(null, HEAD_TAG, null, null);
		tail = new Interceptor<T>(null, TAIL_TAG, null, null);
		head.next = tail;
		tail.prev = head;
	}

	public int size()
	{
		return map.size();
	}

	public void addFirst(String tag, T element)
	{
		Interceptor<T> node = new Interceptor<T>(element, tag, head, head.next);
		head.next = node;
		node.next.prev = node;
		map.put(tag, node);
	}

	public void addLast(String tag, T element)
	{
		Interceptor<T> node = new Interceptor<T>(element, tag, tail.prev, tail);
		tail.prev = node;
		node.prev.next = node;
		map.put(tag, node);
	}

	private Interceptor<T> getNode(String tag)
	{
		Interceptor<T> node = map.get(tag);
		if (node == null)
		{
			throw new NoSuchElementException("No element exists with tag : " + tag);
		}
		return node;
	}

	public void addBefore(String tag, String newTag, T element)
	{
		Interceptor<T> node = getNode(tag);
		Interceptor<T> newNode = new Interceptor<T>(element, newTag, node.prev, node);
		node.prev.next = newNode;
		node.prev = newNode;
		map.put(newTag, newNode);
	}

	public void addAfter(String tag, String newTag, T element)
	{
		Interceptor<T> node = getNode(tag);
		Interceptor<T> newNode = new Interceptor<T>(element, newTag, node, node.next);
		node.next.prev = newNode;
		node.next = newNode;
		map.put(newTag, newNode);
	}

	public void addAll(Pipeline<T> interceptors)
	{
		Interceptor<T> start = interceptors.head.next;
		while (start != interceptors.tail)
		{
			addLast(start.tag(), start.element());
			start = start.next;
		}
	}

	public T getFirst()
	{
		if (head.next == null)
		{
			return null;
		}
		return head.next.element();
	}

	public T getLast()
	{
		if (tail.prev == null)
		{
			return null;
		}
		return tail.prev.element();
	}

	public T get(String tag)
	{
		Interceptor<T> node = getNode(tag);
		return node.element();
	}

	public void removeFirst()
	{
		if (head.next == tail)
		{
			throw new NoSuchElementException();
		}
		Interceptor<T> node = head.next;
		head.next = node.next;
		node.next.prev = head;
		node.prev = null;
		node.next = null;
		map.remove(node.tag());
	}

	public void removeLast()
	{
		if (tail.prev == head)
		{
			throw new NoSuchElementException();
		}
		Interceptor<T> node = tail.prev;
		tail.prev = node.prev;
		node.prev.next = tail;
		node.prev = null;
		node.next = null;
		map.remove(node.tag());
	}

	public void remove(String tag)
	{
		Interceptor<T> node = getNode(tag);
		node.prev.next = node.next;
		node.next.prev = node.prev;
		node.next = null;
		node.prev = null;
		map.remove(tag);
	}

	public void replace(String tag, String newTag, T element)
	{
		Interceptor<T> node = getNode(tag);
		map.remove(tag);
		node.setElement(element);
		node.setTag(newTag);
		map.put(newTag, node);
	}

	@Override
	public Iterator<T> iterator()
	{
		return new PipelineIterator(head);
	}

	private class PipelineIterator implements Iterator<T>
	{
		private Interceptor<T> curr;

		public PipelineIterator(Interceptor<T> curr)
		{
			this.curr = curr;
		}

		@Override
		public boolean hasNext()
		{
			return curr.next != tail;
		}

		@Override
		public T next()
		{
			if (!this.hasNext())
			{
				throw new NoSuchElementException("end of the iteration");
			}
			curr = curr.next;
			return curr.element();
		}

		@Override
		public void remove()
		{
			curr.prev.next = curr.next;
			curr.next.prev = curr.prev;
			curr.next = null;
			curr.prev = null;
		}
	}

	public void printForward()
	{
		System.out.println("\n\n  Printing in forward order ");
		Interceptor<T> start = head;
		while (start != null)
		{
			System.out.println("for element : " + start.element() + " tag : " + start.tag());
			start = start.next;
		}
	}

	public void printBackward()
	{
		System.out.println("\n\n  Printing in reverse order ");
		Interceptor<T> start = tail;
		while (start != null)
		{
			System.out.println("back element : " + start.element() + " tag : " + start.tag());
			start = start.prev;
		}
	}

	public void printMap()
	{
		System.out.println("\n\n  Printing Map ");
		for (Entry<String, Interceptor<T>> entry : map.entrySet())
		{
			System.out.println("map element : " + entry.getValue() + " tag : " + entry.getKey());
		}
	}
}