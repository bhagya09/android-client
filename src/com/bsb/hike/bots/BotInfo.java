package com.bsb.hike.bots;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.Conversation.ConvInfo;
import com.bsb.hike.platform.HikePlatformConstants;

/**
 * Created by shobhit on 22/04/15.
 */
public class BotInfo extends ConvInfo
{
	public static final int MESSAGING_BOT = 1;
	public static final int NON_MESSAGING_BOT = 2;

	public int type;

	public int configuration;

	private boolean isReceiveEnabled;

	public String metadata;


	public static abstract class InitBuilder<P extends InitBuilder<P>> extends ConvInfo.InitBuilder<P>
	{
		private int type, config;

		private boolean isReceiveEnabled;

		private String metadata;

		public P setType(int type)
		{
			this.type = type;
			return getSelfObject();
		}

		public P setConfig(int config)
		{
			this.config = config;
			return  getSelfObject();
		}

		public P setMetadata(String metadata)
		{
			this.metadata = metadata;
			return getSelfObject();
		}

		public P setIsReceiveEnabled(boolean isReceiveEnabled)
		{
			this.isReceiveEnabled = isReceiveEnabled;
			return getSelfObject();
		}

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
		public P setOnHike(boolean onHike)
		{
			return super.setOnHike(true);
		}

		@Override
		public BotInfo build()
		{
			return new BotInfo(this);
		}

	}

	public boolean isReceiveEnabled()
	{
		return isReceiveEnabled;
	}

	public void setIsReceiveEnabled(boolean isReceiveEnabled)
	{
		this.isReceiveEnabled = isReceiveEnabled;
	}

	public void setType(int type)
	{
		this.type = type;
	}

	public void setConfiguration(int configuration)
	{
		this.configuration = configuration;
	}

	public void setMetadata(String metadata)
	{
		this.metadata = metadata;
	}

	public int getConfiguration()
	{
		return configuration;
	}

	public String getMetadata()
	{
		return metadata;
	}

	public int getType()
	{
		return type;
	}

	public static class HikeBotBuilder extends BotInfo.InitBuilder<HikeBotBuilder>
	{
		public HikeBotBuilder(String msisdn)
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
		this.configuration = builder.config;
		this.metadata = builder.metadata;
		this.isReceiveEnabled = builder.isReceiveEnabled;
	}

	public static BotInfo getBotInfoForBotMsisdn(String msisdn)
	{
		return HikeMessengerApp.hikeBotNamesMap.get(msisdn);
	}

	public boolean isMessagingBot()
	{
		return type == MESSAGING_BOT;
	}

	public boolean isNonMessagingBot()
	{
		return type == NON_MESSAGING_BOT;
	}
	
	@Override
	public void setOnHike(boolean isOnHike)
	{
		super.setOnHike(true);
	}
	
	/**
	 * Returns the microAppName this botInfo object belongs to.
	 * 
	 * @return
	 */
	public String getMicroAppName()
	{
		String appName = null;

		if (metadata != null)
		{
			try
			{
				JSONObject md = new JSONObject(metadata);
				appName = md.getString(HikePlatformConstants.APP_NAME);
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return appName;
	}

}
