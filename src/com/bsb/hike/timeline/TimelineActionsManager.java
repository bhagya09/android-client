package com.bsb.hike.timeline;

import com.bsb.hike.timeline.model.TimelineActions;

public class TimelineActionsManager
{
	private volatile static TimelineActionsManager instance;

	private TimelineActionsManager()
	{
		// Avoid instantiation
	}

	public static TimelineActionsManager getInstance()
	{
		if (instance == null)
		{
			synchronized (TimelineActionsManager.class)
			{
				if (instance == null)
				{
					instance = new TimelineActionsManager();
				}
			}
		}
		return instance;
	}

	private TimelineActions actionsData;

	public TimelineActions getActionsData()
	{
		if (actionsData == null)
		{
			actionsData = new TimelineActions();
		}
		return actionsData;
	}

	public void destroy()
	{
		actionsData = null;
	}

	public void setActionsData(TimelineActions actionsData)
	{
		this.actionsData = actionsData;
	}
}
