package com.bsb.hike.timeline.model;

import java.util.ArrayList;
import java.util.HashMap;

import com.bsb.hike.models.ContactInfo;

import android.text.TextUtils;

public class TimelineActions
{
	private HashMap<String, ActionsDataModel> timelineActionsMap;

	public TimelineActions()
	{
		timelineActionsMap = new HashMap<String, ActionsDataModel>();
	}

	public ActionsDataModel getActionsForSU(String uuid)
	{
		if (TextUtils.isEmpty(uuid) || timelineActionsMap == null || timelineActionsMap.isEmpty())
		{
			return null;
		}

		return timelineActionsMap.get(uuid);
	}

	public void addActionDetails(String uuid, ArrayList<ContactInfo> contactInfo, ActionsDataModel.DataTypes type, int totalCount)
	{
		ActionsDataModel actionDM = timelineActionsMap.get(uuid);

		boolean newInstance = false;
		if (actionDM == null)
		{
			actionDM = new ActionsDataModel(type);
			newInstance = true;
		}

		actionDM.addContact(contactInfo);
		actionDM.setCount(totalCount);

		if (newInstance)
		{
			timelineActionsMap.put(uuid, actionDM);
		}
	}
}
