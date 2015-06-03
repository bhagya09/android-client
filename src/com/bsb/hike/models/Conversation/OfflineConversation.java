package com.bsb.hike.models.Conversation;

import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.utils.Logger;

public class OfflineConversation extends OneToOneConversation
{

	private String displayMsisdn;

	protected OfflineConversation(InitBuilder<?> builder)
	{
		super(builder);
		this.displayMsisdn = builder.displayMsisdn;
		Logger.d("OfflineChatThread","In Builder");
	}
	
	public String getDisplayMsisdn()
	{
		return displayMsisdn; 
	}

	protected static abstract class InitBuilder<P extends InitBuilder<P>> extends OneToOneConversation.InitBuilder<P>
	{
		private String displayMsisdn;

		public InitBuilder(String msisdn)
		{
			super(msisdn);
			displayMsisdn=msisdn.replace("o:", "");
		}

		public P setConvInfo(BotInfo newConvInfo)
		{
			this.convInfo = newConvInfo;
			return getSelfObject();
		}
		public OfflineConversation build()
		{
			return new OfflineConversation(this);
		}
	}

	
	/**
	 * 
	 * @author himanshu
	 *
	 */
	public static class ConversationBuilder extends OfflineConversation.InitBuilder<ConversationBuilder>
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

		@Override
		protected ConvInfo getConvInfo(String msisdn)
		{
			return new ConvInfo.ConvInfoBuilder(msisdn).build();
		}
	}
}
