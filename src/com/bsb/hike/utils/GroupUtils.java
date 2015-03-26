package com.bsb.hike.utils;

import android.content.Context;

import com.bsb.hike.R;
import com.bsb.hike.models.BroadcastConversation;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.MessageMetadata;

public class GroupUtils {

	public static String getParticipantAddedMessage(ConvMessage convMessage, Context context, String highlight)
	{
		String participantAddedMessage;
		MessageMetadata metadata = convMessage.getMetadata();
		if (convMessage.isBroadcastConversation())
		{
			if (metadata.isNewBroadcast())
			{
				participantAddedMessage = String.format(context.getString(R.string.new_broadcast_message), highlight);
			}
			else
			{
				participantAddedMessage = String.format(context.getString(R.string.add_to_broadcast_message), highlight);
			}
		}
		else
		{
			if (metadata.isNewGroup())
			{
				participantAddedMessage = String.format(context.getString(R.string.new_group_message), highlight);
			}
			else
			{
				participantAddedMessage = String.format(context.getString(R.string.add_to_group_message), highlight);
			}
		}
		return participantAddedMessage;
	}
	
	public static String getParticipantRemovedMessage(Conversation conversation, Context context, String participantName)
	{
		String participantRemovedMessage = String.format(context.getString(conversation instanceof BroadcastConversation ? R.string.removed_from_broadcast : R.string.left_conversation), participantName);
		return participantRemovedMessage;
	}
	
	public static String getConversationNameChangedMessage(Conversation conversation, Context context, String participantName)
	{
		String nameChangedMessage = String.format(context.getString(conversation instanceof BroadcastConversation ? R.string.change_broadcast_name : R.string.change_group_name), participantName);
		return nameChangedMessage;
	}
	
	public static String getConversationEndedMessage(Conversation conversation, Context context)
	{
		String message = context.getString(conversation instanceof BroadcastConversation ? R.string.broadcast_list_end : R.string.group_chat_end);
		return message;
	}
}
