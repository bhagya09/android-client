package com.bsb.hike.timeline;

import java.util.ArrayList;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.timeline.model.FeedDataModel;
import com.bsb.hike.timeline.model.TimelineActions;
import com.bsb.hike.timeline.model.ActionsDataModel.ActionTypes;
import com.bsb.hike.timeline.model.ActionsDataModel.ActivityObjectTypes;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

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

	/**
	 * Updates actions data in heap with incoming action
	 * 
	 * @param objUUID
	 *            SU id, Platform id, etc
	 * @param actionType
	 *            Like, Unlike, Comment
	 * @param objectType
	 *            SU, CARD, GROUP
	 */
	public void addMyAction(String objUUID, ActionTypes actionType, ActivityObjectTypes objectType)
	{
		// Increment like count in actions table
		String selfMsisdn = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, null);

		ArrayList<String> actorList = new ArrayList<String>();
		actorList.add(selfMsisdn);

		FeedDataModel newFeed = new FeedDataModel(System.currentTimeMillis(), actionType, selfMsisdn, objectType, objUUID);

		TimelineActionsManager.getInstance().getActionsData().updateByActivityFeed(newFeed);
	}
}
