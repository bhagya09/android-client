package com.bsb.hike.timeline.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.timeline.model.ActionsDataModel.ActionTypes;
import com.bsb.hike.timeline.model.ActionsDataModel.ActivityObjectTypes;
import com.bsb.hike.utils.Logger;

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

	public HashMap<Pair<String, String>, ArrayList<ActionsDataModel>> getActionsMap()
	{
		return timelineActionsMap;
	}

	public ActionsDataModel getActions(String uuid, ActionsDataModel.ActionTypes actionType, ActivityObjectTypes objType)
	{
		Logger.d(HikeConstants.TIMELINE_COUNT_LOGS,"gettingAction: "+ uuid); 
		
		if (TextUtils.isEmpty(uuid) || timelineActionsMap == null || timelineActionsMap.isEmpty())
		{
			return null;
		}

		Pair<String, String> uuidObjType = new Pair<String, String>(uuid, objType.getTypeString());

		ArrayList<ActionsDataModel> listForUUID = timelineActionsMap.get(uuidObjType);

		if (listForUUID == null || listForUUID.isEmpty())
		{
			Logger.d(HikeConstants.TIMELINE_COUNT_LOGS,"gettingAction: failed");
			return null;
		}
		
		Logger.d(HikeConstants.TIMELINE_COUNT_LOGS,"gettingAction: found");

		for (ActionsDataModel action : listForUUID)
		{
			if (action.getType() == actionType)
			{
				return action;
			}
		}

		return null;
	}

	public void addActionDetails(String uuid, List<ContactInfo> contactInfo, ActionsDataModel.ActionTypes type, int totalCount, ActivityObjectTypes objType, boolean overwrite)
	{
		Logger.d(HikeConstants.TIMELINE_COUNT_LOGS,"addActionDetails: "+ uuid);
		
		if(type == ActionTypes.UNLIKE)
		{
			return;
		}
		
		Pair<String, String> uuidObjType = new Pair<String, String>(uuid, objType.getTypeString());

		ArrayList<ActionsDataModel> actionDMList = timelineActionsMap.get(uuidObjType);

		boolean newInstance = false;

		if (actionDMList == null || overwrite)
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
		
		actionDM.setTotalCount(totalCount);

		if (newInstance)
		{
			timelineActionsMap.put(uuidObjType, actionDMList);
		}
	}

	public HashMap<Pair<String, String>, ArrayList<ActionsDataModel>> getTimelineActionsMap()
	{
		return timelineActionsMap;
	}

	public void updateByActivityFeed(FeedDataModel feedData)
	{
		Logger.d(HikeConstants.TIMELINE_COUNT_LOGS,"updateByActivityFeed: "); 
		
		if (feedData == null)
		{
			throw new IllegalArgumentException("updateActivityFeed(): input FeedDataModel cannot be null");
		}

		ActionTypes actionType = feedData.getActionType();

		ActionsDataModel actions = getActions(feedData.getObjID(), actionType == ActionTypes.UNLIKE ? ActionTypes.LIKE : actionType, feedData.getObjType());

		if (actions == null)
		{
			ArrayList<ContactInfo> cInfoList = new ArrayList<ContactInfo>();
			cInfoList.add(ContactManager.getInstance().getContact(feedData.getActor(), true, true));
			addActionDetails(feedData.getObjID(), cInfoList, actionType, 1, feedData.getObjType(),false);
		}
		else
		{
			if (actionType == ActionTypes.UNLIKE)
			{
				actions.removeContact(feedData.getActor());
			}
			else if (actionType == ActionTypes.LIKE)
			{
				actions.addContact(feedData.getActor());
			}
			else if (actionType == ActionTypes.COMMENT)
			{
				// Later versions
			}
			else if (actionType == ActionTypes.VIEW)
			{
				// Later versions
			}
		}
	}

}
