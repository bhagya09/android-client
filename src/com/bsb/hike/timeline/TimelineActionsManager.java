package com.bsb.hike.timeline;

import android.util.Pair;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.timeline.model.ActionsDataModel;
import com.bsb.hike.timeline.model.ActionsDataModel.ActionTypes;
import com.bsb.hike.timeline.model.ActionsDataModel.ActivityObjectTypes;
import com.bsb.hike.timeline.model.FeedDataModel;
import com.bsb.hike.timeline.model.TimelineActions;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

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

	public void updateActionsData(TimelineActions argActionsData) {
		if (this.actionsData == null) {
			this.actionsData = argActionsData;
		} else {
			HashMap<Pair<String, String>, ArrayList<ActionsDataModel>> actionsMap = argActionsData.getActionsMap(); // Get all cached actions
			Set<Pair<String, String>> uuidObjTypeSet = actionsMap.keySet(); // Get object type (su,card) and uuid pair
			for (Pair<String, String> uuidObj : uuidObjTypeSet) // iterate and update
			{
				ArrayList<ActionsDataModel> modelArrayList = (ArrayList<ActionsDataModel>) argActionsData.getActionsMap().get(uuidObj);
				ActionsDataModel model = modelArrayList.get(0);
				Logger.d(TimelineActionsManager.class.getSimpleName(), "Updating model - " + model.toString());
				actionsData.addActionDetails(uuidObj.first, new ArrayList<ContactInfo>(model.getContactInfoList()), model.getType(), model.getTotalCount(), ActivityObjectTypes.STATUS_UPDATE, true);
			}
		}
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
