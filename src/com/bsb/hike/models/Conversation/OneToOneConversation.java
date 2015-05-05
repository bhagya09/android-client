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
	protected boolean isOnHike;

	protected OneToOneConversation(InitBuilder<?> builder)
	{
		super(builder);
		this.isOnHike = builder.isOnHike;
	}

	/**
	 * @return the isOnHike
	 */
	@Override
	public boolean isOnHike()
	{
		return isOnHike;
	}

	/**
	 * @param isOnHike
	 *            the isOnHike to set
	 */
	public void setOnHike(boolean isOnHike)
	{
		this.isOnHike = isOnHike;
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
		private boolean isOnHike;

		public InitBuilder(String msisdn)
		{
			super(msisdn, new ConvInfo.ConvInfoBuilder(msisdn));
		}

		public P setIsOnHike(boolean isOnHike)
		{
			this.isOnHike = isOnHike;
			return getSelfObject();
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
