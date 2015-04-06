package com.bsb.hike.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;

import android.content.Context;

public class BroadcastConversation extends GroupConversation {

	public BroadcastConversation(JSONObject jsonObject, Context context)
			throws JSONException {
		super(jsonObject, context);
	}

	public BroadcastConversation(String msisdn, String contactName, String groupOwner, boolean isGroupAlive)
	{
		super(msisdn, contactName, groupOwner, isGroupAlive);
	}
	
	public BroadcastConversation(String msisdn, String contactName, String groupOwner, boolean isGroupAlive, boolean isGroupMute)
	{
		super(msisdn, contactName, groupOwner, isGroupAlive, false);
	}
	
	public static String defaultBroadcastName(ArrayList<String> participantList)
	{
		List<ContactInfo> broadcastParticipants = new ArrayList<ContactInfo>(participantList.size());
		for (String msisdn : participantList)
		{
			ContactInfo contactInfo = ContactManager.getInstance().getContact(msisdn, true, false);
			broadcastParticipants.add(contactInfo);
		}
		Collections.sort(broadcastParticipants);

		String name = Utils.extractFullFirstName(broadcastParticipants.get(0).getFirstNameAndSurname());
		switch (broadcastParticipants.size())
		{
		case 0:
			return "";
		case 1:
			return name;
		default:
			for (int i=1; i<broadcastParticipants.size(); i++)
			{
				name += ", " + Utils.extractFullFirstName(broadcastParticipants.get(i).getFirstNameAndSurname());
			}
			return name;
		}
	}
	
}
