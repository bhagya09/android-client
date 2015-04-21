package com.bsb.hike.models.Conversation;


public class BotInfo extends ConvInfo
{
	public static final int MESSAGING_BOT = 1;
	public static final int NON_MESSAGING_BOT = 2;
	
	private final int type;
	public final int configurtion;
	
	protected static abstract class InitBuilder<P extends InitBuilder<P>> extends ConvInfo.InitBuilder<P>
	{
		private int type,config;
		
		protected InitBuilder(String msisdn)
		{
			super(msisdn);
		}

		public P type(int type)
		{
			this.type = type;
			return getSelfObject();
		}
		
		public void configuration(int configuration)
		{
			this.config = configuration;
		}
		
		@Override
		public BotInfo build()
		{
			return new BotInfo(this);
		}
		
	}
	
	public static class HikeBotBuilder extends BotInfo.InitBuilder<HikeBotBuilder>
	{
		protected HikeBotBuilder(String msisdn)
		{
			super(msisdn);
		}

		@Override
		protected HikeBotBuilder getSelfObject()
		{
			return this;
		}
		
	}
	
	private BotInfo(InitBuilder<?> builder)
	{
		super(builder);
		this.type = builder.type;
		this.configurtion = builder.config;
	}
	
	public boolean isMessagingBot()
	{
		return type == MESSAGING_BOT;
	}
	
	public boolean isNonMessagingBot()
	{
		return type == NON_MESSAGING_BOT;
	}
	
	public static class MessagingBotConfig
	{
		
	}
	
	public static class NonMessagingBotConfig
	{
		
	}
}
