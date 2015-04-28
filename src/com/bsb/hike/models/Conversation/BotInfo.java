package com.bsb.hike.models.Conversation;


public class BotInfo extends ConvInfo
{
	public static final int MESSAGING_BOT = 1;
	public static final int NON_MESSAGING_BOT = 2;
	
	private final int type;
	public final int configurtion;
	public final String metadata;
	
	protected static abstract class InitBuilder<P extends InitBuilder<P>> extends ConvInfo.InitBuilder<P>
	{
		private int type,config;
		private String metadata;
		protected InitBuilder(String msisdn)
		{
			super(msisdn);
		}

		public P type(int type)
		{
			this.type = type;
			return getSelfObject();
		}
		
		public P configuration(int configuration)
		{
			this.config = configuration;
			return getSelfObject();
		}
		
		public P metaData(String metaData)
		{
			this.metadata = metaData;
			return getSelfObject();
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
		this.metadata = builder.metadata;
	}
	
	public boolean isMessagingBot()
	{
		return type == MESSAGING_BOT;
	}
	
	public boolean isNonMessagingBot()
	{
		return type == NON_MESSAGING_BOT;
	}
	
	
	private static abstract class BotConfig
	{
		
		
		private int config;
		
		public BotConfig(int config)
		{
			this.config = config;
		}
		protected boolean isBitSet(int bitPosition)
		{
			return ((config>>bitPosition) & 1) ==1;
		}
	}
	public static class MessagingBotConfig extends BotConfig
	{
		public MessagingBotConfig(int config)
		{
			super(config);
		}	
	}
	
	public static class NonMessagingBotConfig extends BotConfig
	{
		// BITS POSITIONS FOR CONFIGURATION
		private static final byte NAV_BAR = 0;
		
		private static final byte SHARE_FORWARD = 1;
		
		public static final byte TAG_PICKER = 2;
		
		public static final byte THREE_DOT_MENU = 3;
		
		// BITS POSITIONS CONFIG ENDS HERE
		
		public NonMessagingBotConfig(int config)
		{
			super(config);
		}
		
		public boolean isNavBarEnabled()
		{
			return isBitSet(NAV_BAR);
		}
		
		public boolean isShareForwardEnabled()
		{
			return isBitSet(SHARE_FORWARD);
		}
		
		public boolean isTagPickerEnabled()
		{
			return isBitSet(TAG_PICKER);
		}
		
		public boolean isThreeDotMenuEnabled()
		{
			return isBitSet(THREE_DOT_MENU);
		}
		
	}
}
