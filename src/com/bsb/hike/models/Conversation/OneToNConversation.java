package com.bsb.hike.models.Conversation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;

/**
 * 1-n conversation primitives will be derived from this class.<br>
 * Examples of 1-n conversations : private group chats, broadcast, public chats
 * 
 * @author Anu/Piyush
 * 
 */
public abstract class OneToNConversation extends Conversation
{

	protected String conversationOwner;

	protected Map<String, PairModified<GroupParticipant, String>> conversationParticipantList;

	protected ArrayList<String> readByParticipantsList;

	protected ConvMessage pinnedConvMessage;

	/**
	 * Default value of long is 0, hence setting this as -1 here
	 */
	protected long lastSentMsgId = -1;

	protected int unreadPinnedMessageCount;

	/**
	 * @param builder
	 */
	protected OneToNConversation(InitBuilder<?> builder)
	{
		super(builder);
		
		this.conversationOwner = builder.conversationOwner;

		this.conversationParticipantList = builder.conversationParticipantList;

		this.readByParticipantsList = builder.readByParticipantList;

		this.pinnedConvMessage = builder.pinnedConvmessage;

		this.lastSentMsgId = builder.lastSentMsgId;

		this.unreadPinnedMessageCount = builder.unreadPinnedMessageCount;
		
		setConversationAlive(builder.isAlive);
	}
	
	/**
	 * Returns a friendly label for the conversation
	 * 
	 * @return conversationName or msisdn
	 */
	public String getLabel()
	{
		return ((OneToNConvInfo) convInfo).getLabel();
	}

	/**
	 * @return the conversationOwner
	 */
	public String getConversationOwner()
	{
		return conversationOwner;
	}

	/**
	 * @param conversationOwner
	 *            the conversationOwner to set
	 */
	public void setConversationOwner(String conversationOwner)
	{
		this.conversationOwner = conversationOwner;
	}

	/**
	 * @return the groupParticipantList
	 */
	public Map<String, PairModified<GroupParticipant, String>> getConversationParticipantList()
	{
		return conversationParticipantList;
	}

	/**
	 * @param participantList
	 *            the participantList to set
	 */
	public void setConversationParticipantList(Map<String, PairModified<GroupParticipant, String>> participantList)
	{
		this.conversationParticipantList = participantList;
	}

	public void setConversationParticipantList(List<PairModified<GroupParticipant, String>> participantList)
	{
		this.conversationParticipantList = new HashMap<String, PairModified<GroupParticipant, String>>();
		for (PairModified<GroupParticipant, String> convParticipant : participantList)
		{
			String msisdn = convParticipant.getFirst().getContactInfo().getMsisdn();
			this.conversationParticipantList.put(msisdn, convParticipant);
		}
	}

	public PairModified<GroupParticipant, String> getConversationParticipant(String msisdn)
	{
		return conversationParticipantList.containsKey(msisdn) ? conversationParticipantList.get(msisdn) : new PairModified<GroupParticipant, String>(new GroupParticipant(
				new ContactInfo(msisdn, msisdn, msisdn, msisdn)), msisdn);
	}

	/**
	 * Used to get the name of the contact either from the groupParticipantList or ContactManager
	 * 
	 * @param msisdn
	 *            of the contact
	 * @return name of the contact
	 */
	public String getConversationParticipantName(String msisdn)
	{
		String name = null;

		if (null != conversationParticipantList)
		{
			PairModified<GroupParticipant, String> grpPair = conversationParticipantList.get(msisdn);

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
	 * 
	 * @param msisdn
	 *            of the contact
	 * @return first full name of the contact
	 */
	public String getConvParticipantFullFirstName(String msisdn)
	{
		String fullName = getConversationParticipantName(msisdn);

		return Utils.extractFullFirstName(fullName);
	}

	/**
	 * Used to get the first name and the last name of the contact whose msisdn is known
	 * 
	 * @param msisdn
	 *            of the contact
	 * @return first name + last name of the contact
	 */
	public String getConvParticipantFirstNameAndSurname(String msisdn)
	{
		return getConversationParticipantName(msisdn);
	}

	/**
	 * @return the pinnedConvMessage
	 */
	public ConvMessage getPinnedConvMessage()
	{
		return pinnedConvMessage;
	}

	/**
	 * @param pinnedConvMessage
	 *            the pinnedConvMessage to set
	 */
	public void setPinnedConvMessage(ConvMessage pinnedConvMessage)
	{
		this.pinnedConvMessage = pinnedConvMessage;
	}

	/**
	 * @return the isConversationAlive
	 */
	public boolean isConversationAlive()
	{
		return ((OneToNConvInfo) convInfo).isConversationAlive();
	}

	/**
	 * @param isConversationAlive
	 *            the isConversationAlive to set
	 */
	public void setConversationAlive(boolean isConversationAlive)
	{
		((OneToNConvInfo) convInfo).setConversationAlive(isConversationAlive);
	}

	public int getParticipantListSize()
	{
		if (this.conversationParticipantList != null)
		{
			return conversationParticipantList.size();
		}

		return -1;
	}

	public void setupReadByList(String readBy, long msgId)
	{
		if (msgId < 1)
		{
			return;
		}

		if (readByParticipantsList == null)
		{
			readByParticipantsList = new ArrayList<String>();
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
			readByParticipantsList = new ArrayList<String>();
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

	/**
	 * @return the readByParticipantsList
	 */
	public ArrayList<String> getReadByParticipantsList()
	{
		return readByParticipantsList;
	}

	/**
	 * @return the unreadPinnedMessageCount
	 */
	public int getUnreadPinnedMessageCount()
	{
		return unreadPinnedMessageCount;
	}

	/**
	 * @param unreadPinnedMessageCount
	 *            the unreadPinnedMessageCount to set
	 */
	public void setUnreadPinnedMessageCount(int unreadPinnedMessageCount)
	{
		this.unreadPinnedMessageCount = unreadPinnedMessageCount;
	}

	@Override
	protected void setConvInfo(ConvInfo convInfo)
	{
		if (!(convInfo instanceof OneToNConvInfo))
		{
			throw new IllegalStateException("Pass ConvInfo as OneToNConvInfo object for such type of conversations!");
		}

		this.convInfo = (OneToNConvInfo) convInfo;
	}

	@Override
	public void setMetadata(ConversationMetadata metadata)
	{
		if (!(metadata instanceof OneToNConversationMetadata))
		{
			throw new IllegalStateException("Pass metadata as OneToNConversationMetadata object for such type of conversations!");
		}

		this.metadata = (OneToNConversationMetadata) metadata;
	}

	@Override
	public OneToNConversationMetadata getMetadata()
	{
		return (OneToNConversationMetadata) this.metadata;
	}

	/**
	 * Builder base class extending {@link Conversation.InitBuilder}
	 * 
	 * @author piyush
	 * 
	 * @param <P>
	 */
	protected static abstract class InitBuilder<P extends InitBuilder<P>> extends Conversation.InitBuilder<P>
	{
		private String conversationOwner;

		private Map<String, PairModified<GroupParticipant, String>> conversationParticipantList;

		private ArrayList<String> readByParticipantList;

		private ConvMessage pinnedConvmessage;

		private long lastSentMsgId = -1;

		private int unreadPinnedMessageCount;
		
		private boolean isAlive;
		
		public InitBuilder(String msisdn)
		{
			super(msisdn, new OneToNConvInfo.ConvInfoBuilder(msisdn));
		}

		public P setConversationOwner(String conversationOwner)
		{
			this.conversationOwner = conversationOwner;
			return getSelfObject();
		}

		public P setConversationParticipantsList(Map<String, PairModified<GroupParticipant, String>> participantList)
		{
			this.conversationParticipantList = participantList;
			return getSelfObject();
		}

		public P setConversationParticipantsList(List<PairModified<GroupParticipant, String>> participantList)
		{
			this.conversationParticipantList = new HashMap<String, PairModified<GroupParticipant, String>>();
			for (PairModified<GroupParticipant, String> grpParticipant : participantList)
			{
				String msisdn = grpParticipant.getFirst().getContactInfo().getMsisdn();
				this.conversationParticipantList.put(msisdn, grpParticipant);
			}
			return getSelfObject();
		}

		public P setupReadByList(String readBy, long msgId)
		{
			if (msgId < 1)
			{
				return getSelfObject();
			}

			if (readByParticipantList == null)
			{
				readByParticipantList = new ArrayList<String>();
			}
			readByParticipantList.clear();
			lastSentMsgId = msgId;

			if (readBy == null)
			{
				return getSelfObject();
			}
			try
			{
				JSONArray readByArray;
				readByArray = new JSONArray(readBy);
				for (int i = 0; i < readByArray.length(); i++)
				{
					readByParticipantList.add(readByArray.optString(i));
				}
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return getSelfObject();
		}

		public P setPinnedConvmessage(ConvMessage pinnedConvMessage)
		{
			this.pinnedConvmessage = pinnedConvMessage;
			return getSelfObject();
		}

		public P setUnreadPinnedMsgCount(int count)
		{
			this.unreadPinnedMessageCount = count;
			return getSelfObject();
		}

		@Override
		public P setConversationMetadata(ConversationMetadata metadata)
		{
			if (!(metadata instanceof OneToNConversationMetadata))
			{
				throw new IllegalStateException("Pass metadata as OneToNConversationMetadata object for such type of conversations!");
			}

			this.metadata = (OneToNConversationMetadata) metadata;
			return getSelfObject();
		}
		
		public P setIsAlive(boolean alive)
		{
			this.isAlive = alive;
			return getSelfObject();
		}
	}

	/**
	 * Utility method for returning a default name for a 1-n Conversation
	 * 
	 * @param participantList
	 * @return
	 */
	public static String defaultConversationName(List<PairModified<GroupParticipant, String>> participantList)
	{
		List<GroupParticipant> groupParticipants = new ArrayList<GroupParticipant>();
		for (PairModified<GroupParticipant, String> participant : participantList)
		{
			if (!participant.getFirst().hasLeft())
			{
				groupParticipants.add(participant.getFirst());
			}
		}
		Collections.sort(groupParticipants);
		String name = null;
		if (groupParticipants.size() > 0)
		{
			name = Utils.extractFullFirstName(groupParticipants.get(0).getContactInfo().getFirstNameAndSurname());
		}
		switch (groupParticipants.size())
		{
		case 0:
			return "";
		case 1:
			return name;
		default:
			for (int i = 1; i < groupParticipants.size(); i++)
			{
				name += ", " + Utils.extractFullFirstName(groupParticipants.get(i).getContactInfo().getFirstNameAndSurname());
			}
			return name;
		}
	}

	public static OneToNConversation createOneToNConversationFromJSON(JSONObject jsonObj) throws JSONException
	{
		OneToNConversation conversation;
		String msisdn = jsonObj.getString(HikeConstants.TO);

		Map<String, PairModified<GroupParticipant, String>> participants = new HashMap<String, PairModified<GroupParticipant, String>>();

		JSONArray array = jsonObj.getJSONArray(HikeConstants.DATA);
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
			Logger.d("OneToNConversation", "Parsing JSON and adding contact to conversation: " + contactNum);
			participants.put(contactNum, new PairModified<GroupParticipant, String>(groupParticipant, contactName));
		}

		List<ContactInfo> contacts = ContactManager.getInstance().getContact(msisdns, true, false);
		for (ContactInfo contact : contacts)
		{
			PairModified<GroupParticipant, String> grpPair = participants.get(contact.getMsisdn());
			if (null != grpPair)
			{
				GroupParticipant grpParticipant = grpPair.getFirst();
				contact.setOnhike(grpParticipant.getContactInfo().isOnhike());
				grpParticipant.setContactInfo(contact);
				if (null != contact.getName())
				{
					grpPair.setSecond(contact.getName());
				}
			}
		}

		String convName = ContactManager.getInstance().getName(msisdn);

		if (OneToNConversationUtils.isBroadcastConversation(msisdn))
		{
			conversation = new BroadcastConversation.ConversationBuilder(msisdn).setConversationOwner(jsonObj.getString(HikeConstants.FROM))
					.setConversationParticipantsList(participants).setConversationOwner(convName).build();

		}
		else
		{
			conversation = new GroupConversation.ConversationBuilder(msisdn).setConversationOwner(jsonObj.getString(HikeConstants.FROM))
					.setConversationParticipantsList(participants).setConversationOwner(convName).build();
		}

		return conversation;
	}
}
