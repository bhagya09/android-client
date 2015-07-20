package com.bsb.hike.timeline.model;

import java.util.HashMap;
import java.util.List;

import com.bsb.hike.models.ContactInfo;

import android.text.TextUtils;

public class TimelineActions
{
	private HashMap<String, ActionsDataModel> timelineActionsMap;

	public TimelineActions()
	{
		timelineActionsMap = new HashMap<String, ActionsDataModel>();
	}

	public ActionsDataModel getActionsForSU(String uuid, ActionsDataModel.ActionTypes actionType)
	{
		if (TextUtils.isEmpty(uuid) || timelineActionsMap == null || timelineActionsMap.isEmpty())
		{
			return null;
		}

		return timelineActionsMap.get(uuid+actionType);
	}

	public void addActionDetails(String uuid, List<ContactInfo> contactInfo, ActionsDataModel.ActionTypes type, int totalCount)
	{
		ActionsDataModel actionDM = timelineActionsMap.get(uuid);

		boolean newInstance = false;
		if (actionDM == null)
		{
			actionDM = new ActionsDataModel(type);
			newInstance = true;
		}

		actionDM.addContacts(contactInfo);
		actionDM.setCount(totalCount);

		if (newInstance)
		{
			timelineActionsMap.put(uuid+type, actionDM);
		}
	}
}
