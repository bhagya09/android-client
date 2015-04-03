package com.bsb.hike.models.Conversation;

import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;

/**
 * Conversation primitive for GroupChats
 * 
 * @author Anu/Piyush
 * 
 */
public class GroupConversation extends OneToNConversation
{

	private GroupConversation(InitBuilder<?> builder)
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
			if (type.equals(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN))
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

	/**
	 * Builder base class extending {@link OneToNConversation.InitBuilder}
	 * 
	 * @author piyush
	 * 
	 * @param <P>
	 */
	protected static abstract class InitBuilder<P extends InitBuilder<P>> extends OneToNConversation.InitBuilder<P>
	{

		public InitBuilder(String msisdn)
		{
			super(msisdn);
		}

		public GroupConversation build()
		{
			return new GroupConversation(this);
		}
	}

	/**
	 * Builder class used to generating {@link GroupConversation}
	 * <p>
	 * Bare bone Usage : GroupConversation conv = GroupConversation.ConversationBuilder(msisdn).build();<br>
	 * Other examples : GroupConversation conv = GroupConversation.ConversationBuilder(msisdn).setConvName("ABC").setIsAlive(true).build();
	 * 
	 * @author piyush
	 * 
	 */
	public static class ConversationBuilder extends GroupConversation.InitBuilder<ConversationBuilder>
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
