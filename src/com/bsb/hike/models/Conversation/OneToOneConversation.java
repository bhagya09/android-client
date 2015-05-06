package com.bsb.hike.models.Conversation;

import com.bsb.hike.models.ConvMessage;

/**
 * Conversation primitive for 1-1 chats like human chats, bot chats etc.
 * 
 * @author Anu/Piyush
 * 
 */
public class OneToOneConversation extends Conversation
{

	protected OneToOneConversation(InitBuilder<?> builder)
	{
		super(builder);
	}

	/**
	 * @return the isOnHike
	 */
	@Override
	public boolean isOnHike()
	{
		return convInfo.isOnHike();
	}


	@Override
	public void updateLastConvMessage(ConvMessage message)
	{
		convInfo.setLastConversationMsg(message);

		/**
		 * Updates the Conversation timestamp only if the message does not qualify as a broadcast message in a OneToOneConversation.
		 */
		if (!message.isBroadcastMessage())
		{
			setSortingTimeStamp(message.getTimestamp());
		}
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
		public InitBuilder(ConvInfo convInfo)
		{
			super(convInfo);
		}

		public OneToOneConversation build()
		{
			return new OneToOneConversation(this);
		}

	}

	/**
	 * Builder class used to generating {@link OneToOneConversation}
	 * <p>
	 * Bare bone Usage : OneToOneConversation conv = OneToOneConversation.ConversationBuilder(msisdn).build();<br>
	 * Other examples : OneToOneConversation conv = OneToOneConversation.ConversationBuilder(msisdn).setConvName("ABC").setIsOnHike(false).build();
	 * 
	 * @author piyush
	 * 
	 */
	public static class ConversationBuilder extends OneToOneConversation.InitBuilder<ConversationBuilder>
	{

		public ConversationBuilder(ConvInfo convInfo)
		{
			super(convInfo);
		}

		@Override
		protected ConversationBuilder getSelfObject()
		{
			return this;
		}

	}
}
