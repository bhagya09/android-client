/**
 * 
 */
package com.bsb.hike.models.Conversation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;

/**
 * @author anubansal
 *
 */
public class BroadcastConversation extends OneToNConversation
{

	/**
	 * @param builder
	 */
	public BroadcastConversation(InitBuilder<?> builder)
	{
		super(builder);
	}

	@Override
	public JSONObject serialize(String type)
	{
		JSONObject object = new JSONObject();
		try
		{
			object.put(HikeConstants.TYPE, type);
			object.put(HikeConstants.TO, getMsisdn());
			if (type.equals(HikeConstants.MqttMessageTypes.BROADCAST_LIST_JOIN))
			{
				JSONArray array = new JSONArray();
				for (Entry<String, PairModified<GroupParticipant, String>> participant : conversationParticipantList.entrySet())
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
			Logger.e("GroupConversation", "invalid json message", e);
		}
		return object;
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
			for (int i = 1; i < broadcastParticipants.size(); i++)
			{
				name += ", " + Utils.extractFullFirstName(broadcastParticipants.get(i).getFirstNameAndSurname());
			}
			return name;
		}
	}

	/**
	 * Builder base class extending {@link OneToNConversation.InitBuilder}
	 * 
	 * @author anubansal
	 * 
	 * @param <P>
	 */
	protected static abstract class InitBuilder<P extends InitBuilder<P>> extends OneToNConversation.InitBuilder<P>
	{

		public InitBuilder(String msisdn)
		{
			super(msisdn);
		}

		public BroadcastConversation build()
		{
			return new BroadcastConversation(this);
		}
	}

	/**
	 * Builder class used to generating {@link BroadcastConversation}
	 * <p>
	 * Bare bone Usage : BroadcastConversation conv = BroadcastConversation.ConversationBuilder(msisdn).build();<br>
	 * Other examples : BroadcastConversation conv = BroadcastConversation.ConversationBuilder(msisdn).setConvName("ABC").setIsAlive(true).build();
	 * 
	 * @author anubansal
	 * 
	 */
	public static class ConversationBuilder extends BroadcastConversation.InitBuilder<ConversationBuilder>
	{

		public ConversationBuilder(String msisdn)
		{
			super(msisdn);
		}

		@Override
		protected ConversationBuilder getSelfObject()
		{
			return this;
		}

	}
}
