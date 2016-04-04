package com.bsb.hike;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by anubhavgupta on 04/04/16.
 */
public class HikePubSubUi extends Handler
{
    /****************  Events Start *******************/

    public static final String STICKER_DOWNLOADED = "stickerDownloaded";

    /****************  Events End   *******************/
	private final BlockingQueue<Operation> mQueue;

	private Map<String, Set<Listener>> listeners;

	private boolean handlerActive;

	public class Operation
	{
		public Operation(String type, Object o)
		{
			this.type = type;
			this.payload = o;
		}

		public final String type;

		public final Object payload;
	}

	public interface Listener
	{
		void onUiEventReceived(String type, Object object);
	}

	public HikePubSubUi()
	{
		super(Looper.getMainLooper());
		listeners = new ConcurrentHashMap<String, Set<Listener>>();
		mQueue = new LinkedBlockingQueue<Operation>();
	}

	public void addListener(String type, Listener listener)
	{
		add(type, listener);
	}

	public void addListeners(Listener listener, String... types)
	{
		for (String type : types)
		{
			add(type, listener);
		}
	}

	private void add(String type, Listener listener)
	{
		Set<Listener> list;
		list = listeners.get(type);
		if (list == null)
		{
			synchronized (this) // take a smaller lock
			{
				if ((list = listeners.get(type)) == null)
				{
					list = new CopyOnWriteArraySet<Listener>();
					listeners.put(type, list);
				}
			}
		}
		list.add(listener);
	}

	/*
	 * We also need to make removeListener a synchronized method. if we don't do that it would lead to memory inconsistency issue. in our case some activities won't get destroyed
	 * unless we unregister all listeners and in that slot if activity receives a pubsub event it would try to handle this event which may lead to anything unusual.
	 */
	public void removeListener(String type, Listener listener)
	{
		remove(type, listener);
	}

	public void removeListeners(Listener listener, String... types)
	{
		for (String type : types)
		{
			remove(type, listener);
		}
	}

	private void remove(String type, Listener listener)
	{
		Set<Listener> l = null;
		l = listeners.get(type);
		if (l != null)
		{
			l.remove(listener);
		}
	}

	public boolean publish(String type, Object o)
	{
		Set<Listener> l = listeners.get(type);
		if (l != null && l.size() >= 0)
		{
			mQueue.add(new Operation(type, o));
			if (!handlerActive)
			{
				handlerActive = true;
				if (!sendMessage(obtainMessage()))
				{
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public void handleMessage(Message msg)
	{
		while (true)
		{
			Operation operation = mQueue.poll();
			if (operation == null)
			{
				synchronized (this)
				{
					// Check again, this time in synchronized
					operation = mQueue.poll();
					if (operation == null)
					{
						handlerActive = false;
						return;
					}
				}
			}
			String type = operation.type;
			Object o = operation.payload;

			Set<Listener> list = listeners.get(type);

			if (list == null || list.isEmpty())
			{
				handlerActive = false;
				return;
			}

			for (Listener l : list)
			{
				l.onUiEventReceived(type, o);
			}
		}
	}
}
