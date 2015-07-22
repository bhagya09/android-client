package com.bsb.hike.timeline.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.timeline.model.ActionsDataModel.ActivityObjectTypes;

import android.text.TextUtils;
import android.util.Pair;

public class TimelineActions
{
	private HashMap<Pair<String, String>, ArrayList<ActionsDataModel>> timelineActionsMap;

	@SuppressWarnings("serial")
	public TimelineActions()
	{
		timelineActionsMap = new HashMap<Pair<String, String>, ArrayList<ActionsDataModel>>()
		{
			@Override
			public ArrayList<ActionsDataModel> get(Object key)
			{
				if (key instanceof Pair)
				{
					return super.get(key);
				}
				else
				{
					throw new IllegalArgumentException("Can only get by Pair<uuid,objType>");
				}
			}
		};
	}

	public ActionsDataModel getActions(String uuid, ActionsDataModel.ActionTypes actionType, ActivityObjectTypes objType)
	{
		if (TextUtils.isEmpty(uuid) || timelineActionsMap == null || timelineActionsMap.isEmpty())
		{
			return null;
		}
		
		Pair<String, String> uuidObjType = new Pair<String, String>(uuid, objType.getTypeString());

		ArrayList<ActionsDataModel> listForUUID = timelineActionsMap.get(uuidObjType);

		if (listForUUID == null || listForUUID.isEmpty())
		{
			return null;
		}

		for (ActionsDataModel action : listForUUID)
		{
			if (action.getType() == actionType)
			{
				return action;
			}
		}

		return null;
	}

	public void addActionDetails(String uuid, List<ContactInfo> contactInfo, ActionsDataModel.ActionTypes type, int totalCount, ActivityObjectTypes objType)
	{
		Pair<String, String> uuidObjType = new Pair<String, String>(uuid, objType.getTypeString());
		
		ArrayList<ActionsDataModel> actionDMList = timelineActionsMap.get(uuidObjType);

		boolean newInstance = false;

		if (actionDMList == null)
		{
			actionDMList = new ArrayList<ActionsDataModel>();
			newInstance = true;
		}

		ActionsDataModel actionDM = null;

		if (actionDMList.isEmpty())
		{
			actionDM = new ActionsDataModel(type);
			actionDMList.add(actionDM);
		}
		else
		{
			for (ActionsDataModel adm : actionDMList)
			{
				if (adm.getType() == type)
				{
					actionDM = adm;
					break;
				}
			}
			if (actionDM == null)
			{
				actionDM = new ActionsDataModel(type);
				actionDMList.add(actionDM);
			}
		}

		actionDM.addContacts(contactInfo);
		actionDM.setCount(totalCount);

		if (newInstance)
		{
			timelineActionsMap.put(uuidObjType, actionDMList);
		}
	}

	public HashMap<Pair<String, String>, ArrayList<ActionsDataModel>> getTimelineActionsMap()
	{
		return timelineActionsMap;
	}

}
