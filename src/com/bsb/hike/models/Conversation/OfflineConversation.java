package com.bsb.hike.models.Conversation;

import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.utils.Logger;

public class OfflineConversation extends OneToOneConversation
{

	private String displayMsisdn;

	OfflineConvInfo convInfo;

	protected OfflineConversation(InitBuilder<?> builder)
	{
		super(builder);
		this.displayMsisdn = builder.displayMsisdn;
		convInfo=(OfflineConvInfo) builder.convInfo;
		Logger.d("OfflineChatThread", "In Builder");
	}

	public String getDisplayMsisdn()
	{
		return convInfo.getDisplayMsisdn();
	}
	
	public String getLabel()
	{
		return convInfo.getLabel();
	}

	protected static abstract class InitBuilder<P extends InitBuilder<P>> extends OneToOneConversation.InitBuilder<P>
	{
		private String displayMsisdn;

		public InitBuilder(String msisdn)
		{
			super(msisdn);
		}

		public P setConvInfo(OfflineConvInfo newConvInfo)
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
		protected OfflineConvInfo getConvInfo(String msisdn)
		{
			return new OfflineConvInfo.OfflineBuilder(msisdn).setDisplayMsisdn(msisdn.replace("o:", "")).build();
		}
	}
}
