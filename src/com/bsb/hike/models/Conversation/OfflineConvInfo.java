package com.bsb.hike.models.Conversation;

import com.bsb.hike.bots.BotInfo;

public class OfflineConvInfo extends ConvInfo
{

	private String displayMsisdn;

	private OfflineConvInfo(InitBuilder<?> builder)
	{
		super(builder);
		displayMsisdn = builder.displayMsisdn;
	}

	public String getDisplayMsisdn()
	{
		return displayMsisdn;
	}
	
	public static abstract class InitBuilder<P extends InitBuilder<P>> extends ConvInfo.InitBuilder<P>
	{
		private String displayMsisdn;
		
		protected InitBuilder(String msisdn)
		{
			super(msisdn);
		}

		public P setDisplayMsisdn(String displayMsisdn)
		{
			this.displayMsisdn=displayMsisdn;
			return getSelfObject();
		}
		
		public OfflineConvInfo build()
		{
			return new OfflineConvInfo(this);
		}
	}

	public static class OfflineBuilder extends OfflineConvInfo.InitBuilder<OfflineBuilder>
	{
		public OfflineBuilder(String msisdn)
		{
			super(msisdn);
		}

		@Override
		protected OfflineBuilder getSelfObject()
		{
			return this;
		}

	}

}
