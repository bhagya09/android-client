package com.bsb.hike.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;

public class GroupConversation extends Conversation
{

	private String groupOwner;

	private boolean isGroupAlive;

	private boolean isMuted;

	private boolean hasSmsUser;

	protected Map<String, PairModified<GroupParticipant, String>> groupParticipantList;
	
	private long lastSentMsgId = -1;
	
	private LinkedList<String> readByParticipantsList;

	private int groupMemberAliveCount;

	public GroupConversation(String msisdn, String contactName, String groupOwner, boolean isGroupAlive)
	{
		super(msisdn, contactName, true);
		this.groupOwner = groupOwner;
		this.isGroupAlive = isGroupAlive;
	}
	
	public GroupConversation(String msisdn, String contactName, String groupOwner, boolean isGroupAlive, boolean isGroupMute)
	{
		this(msisdn, contactName, groupOwner, isGroupAlive);
		this.isMuted = isGroupMute; 
	}

	public GroupConversation(JSONObject jsonObject, Context context) throws JSONException
	{
		super(jsonObject.getString(HikeConstants.TO), null, true);
		this.groupOwner = jsonObject.getString(HikeConstants.FROM);
		this.groupParticipantList = new HashMap<String, PairModified<GroupParticipant, String>>();
		JSONArray array = jsonObject.getJSONArray(HikeConstants.DATA);
		List<String> msisdns = new ArrayList<String>();
		for (int i = 0; i < array.length(); i++)
		{
			JSONObject nameMsisdn = array.getJSONObject(i);
			String contactNum = nameMsisdn.getString(HikeConstants.MSISDN);
			msisdns.add(contactNum);
			String contactName = nameMsisdn.getString(HikeConstants.NAME);
			boolean onHike = nameMsisdn.optBoolean(HikeConstants.ON_HIKE);
			boolean onDnd = nameMsisdn.optBoolean(HikeConstants.DND);
			GroupParticipant groupParticipant = new GroupParticipant(new ContactInfo(contactNum, contactNum, contactName, contactNum, onHike), false, onDnd);
			Logger.d(getClass().getSimpleName(), "Parsing JSON and adding contact to conversation: " + contactNum);
			this.groupParticipantList.put(contactNum, new PairModified<GroupParticipant, String>(groupParticipant, contactName));
		}

		List<ContactInfo> contacts = ContactManager.getInstance().getContact(msisdns, true, false);
		for (ContactInfo contact : contacts)
		{
			PairModified<GroupParticipant, String> grpPair = this.groupParticipantList.get(contact.getMsisdn());
			if (null != grpPair)
			{
				GroupParticipant grpParticipant = grpPair.getFirst();
				contact.setOnhike(grpParticipant.getContactInfo().isOnhike());
				grpParticipant.setContactInfo(contact);
				if(null != contact.getName())
				{
					grpPair.setSecond(contact.getName());
				}
			}
		}

		String groupName = ContactManager.getInstance().getName(getMsisdn());
		setContactName(groupName);
	}

	public String getGroupOwner()
	{
		return groupOwner;
	}

	public boolean getIsGroupAlive()
	{
		return isGroupAlive;
	}

	public void setGroupAlive(boolean isAlive)
	{
		this.isGroupAlive = isAlive;
	}

	public void setGroupParticipantList(Map<String, PairModified<GroupParticipant, String>> groupParticipantList)
	{
		this.groupParticipantList = groupParticipantList;
	}

	public void setGroupParticipantList(List<PairModified<GroupParticipant, String>> groupParticipantList)
	{
		this.groupParticipantList = new HashMap<String, PairModified<GroupParticipant, String>>();
		for (PairModified<GroupParticipant, String> grpParticipant : groupParticipantList)
		{
			String msisdn = grpParticipant.getFirst().getContactInfo().getMsisdn();
			this.groupParticipantList.put(msisdn, grpParticipant);
		}
	}

	public Map<String, PairModified<GroupParticipant, String>> getGroupParticipantList()
	{
		return groupParticipantList;
	}

	public PairModified<GroupParticipant, String> getGroupParticipant(String msisdn)
	{
		return groupParticipantList.containsKey(msisdn) ? groupParticipantList.get(msisdn) : new PairModified<GroupParticipant, String>(new GroupParticipant(new ContactInfo(
				msisdn, msisdn, msisdn, msisdn)), msisdn);
	}

	/**
	 * Used to get the name of the contact either from the groupParticipantList or ContactManager
	 * @param msisdn of the contact
	 * @return name of the contact
	 */
	public String getGroupParticipantName(String msisdn)
	{
		String name = null;

		if (null != groupParticipantList)
		{
			PairModified<GroupParticipant, String> grpPair = groupParticipantList.get(msisdn);
			
			if (null != grpPair)
			{
				name = grpPair.getSecond();
			}
		}
		
		/*
		 * If groupParticipantsList is not loaded(in case of conversation screen as we load group members when we enter into GC) then we get name from contact manager
		 */
		if (null == name)
		{
			HikeMessengerApp.getContactManager().getContact(msisdn, true, false);
			name = HikeMessengerApp.getContactManager().getName(getMsisdn(), msisdn);
		}
		return name;
	}

	/**
	 * Used to get the first full name of the contact whose msisdn is known
	 * @param msisdn of the contact
	 * @return first full name of the contact
	 */
	public String getGroupParticipantFullFirstName(String msisdn)
	{
		String fullName = getGroupParticipantName(msisdn);
		
		return Utils.extractFullFirstName(fullName);
	}
	
	/**
	 * Used to get the first name and the last name of the contact whose msisdn is known
	 * @param msisdn of the contact
	 * @return first name + last name of the contact
	 */
	public String getGroupParticipantFirstNameAndSurname(String msisdn)
	{
		return getGroupParticipantName(msisdn);
	}
	
	public String getLabel()
	{
		if (!TextUtils.isEmpty(getContactName()))
			return getContactName();
		else
		{
			setGroupParticipantList(ContactManager.getInstance().getGroupParticipants(getMsisdn(), false, false));
			// Before contact manager we were adding all the group participants to conversation object initially when getConversations of HikeConversationDatabase is called
			// But now we do lazy loading, we don't have group participants when we are on home screen
			// In case of empty group name, group Participants are needed so setting it here.
			return Utils.defaultGroupName(new ArrayList<PairModified<GroupParticipant, String>>(groupParticipantList.values()));
		}
	}

	public void setIsMuted(boolean isMuted)
	{
		this.isMuted = isMuted;
	}

	public boolean isMuted()
	{
		return isMuted;
	}

	public void setHasSmsUser(boolean hasSmsUser)
	{
		this.hasSmsUser = hasSmsUser;
	}

	public boolean hasSmsUser()
	{
		return hasSmsUser;
	}

	public int getGroupMemberAliveCount()
	{
		return groupMemberAliveCount;
	}

	public void setGroupMemberAliveCount(int groupMemberAliveCount)
	{
		this.groupMemberAliveCount = groupMemberAliveCount;
	}

	public JSONObject serialize(String type)
	{
		JSONObject object = new JSONObject();
		try
		{
			object.put(HikeConstants.TYPE, type);
			object.put(HikeConstants.TO, getMsisdn());
			if (type.equals(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN))
			{
				JSONArray array = new JSONArray();
				for (Entry<String, PairModified<GroupParticipant, String>> participant : groupParticipantList.entrySet())
				{
					JSONObject nameMsisdn = new JSONObject();
					nameMsisdn.put(HikeConstants.NAME, participant.getValue().getSecond());
					nameMsisdn.put(HikeConstants.MSISDN, participant.getKey());
					array.put(nameMsisdn);
				}
				object.put(HikeConstants.DATA, array);
			}
			object.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis() / 1000));
		}
		catch (JSONException e)
		{
			Logger.e("ConvMessage", "invalid json message", e);
		}
		return object;
	}
	
	public void setupReadByList(String readBy, long msgId)
	{
		if (msgId < 1)
		{
			return;
		}
		
		if (readByParticipantsList == null)
		{
			readByParticipantsList = new LinkedList<String>();
		}
		readByParticipantsList.clear();
		lastSentMsgId = msgId;
		
		if (readBy == null)
		{
			return;
		}
		try
		{
			JSONArray readByArray;
			readByArray = new JSONArray(readBy);
			for (int i = 0; i < readByArray.length(); i++)
			{
				readByParticipantsList.add(readByArray.optString(i));
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void updateReadByList(String msisdn, long msgId)
	{
		if (lastSentMsgId > msgId || TextUtils.isEmpty(msisdn))
		{
			return;
		}
		if (readByParticipantsList == null)
		{
			readByParticipantsList = new LinkedList<String>();
		}
		
		if (lastSentMsgId == msgId)
		{
			if (!readByParticipantsList.contains(msisdn))
			{
				readByParticipantsList.add(msisdn);
			}
		}
		else if (lastSentMsgId < msgId)
		{
			readByParticipantsList.clear();
			readByParticipantsList.add(msisdn);
			lastSentMsgId = msgId;
		}
	}
	
	public LinkedList<String> getReadByList()
	{
		return readByParticipantsList;
	}
}
