package com.bsb.hike.bots;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONException;
import org.json.JSONObject;

import android.R.integer;
import android.content.Context;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.Conversation.ConvInfo;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.service.MqttMessagesManager;
import com.bsb.hike.utils.Logger;

/**
 * Created by shobhit on 22/04/15.
 */
public class BotInfo extends ConvInfo
{
	public static final int MESSAGING_BOT = 1;
	public static final int NON_MESSAGING_BOT = 2;

	// messaging bot by default because all the earlier bots were messaging by default and all the earlier builds had only messaging bots.
	private int type = MESSAGING_BOT;

	//by default all the configured items are on.
	private int configuration = Integer.MAX_VALUE;

	private String namespace;

	private String metadata;

	private String configData;

	private String notifData;
	
	private AtomicBoolean configDataRefreshed = new AtomicBoolean(false);
	
	private AtomicBoolean isBackPressAllowed = new AtomicBoolean(false);

	private String helperData;
	
	private static final String DEFAULT_UNREAD_COUNT = "1+";

	public static abstract class InitBuilder<P extends InitBuilder<P>> extends ConvInfo.InitBuilder<P>
	{
		private int type, config;

		private String namespace;

		private String metadata, configData, notifData, helperData;

		protected InitBuilder(String msisdn)
		{
			super(msisdn);
		}

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

		public P type(int type)
		{
			this.type = type;
			return getSelfObject();
		}

		public P setConfigData(String configData)
		{
			this.configData = configData;
			return getSelfObject();
		}

		public P setNamespace(String namespace)
		{
			this.namespace = namespace;
			return getSelfObject();
		}

		public P setNotifData(String notifData)
		{
			this.notifData = notifData;
			return getSelfObject();
		}

		public P setHelperData(String helperData)
		{
			this.helperData = helperData;
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

	public String getHelperData()
	{
		return helperData;
	}

	public void setHelperData(String helperData)
	{
		this.helperData = helperData;
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

	public String getConfigData()
	{
		return configData;
	}

	public String getNamespace()
	{
		return namespace;
	}

	public void setNamespace(String namespace)
	{
		this.namespace = namespace;
	}

	public String getNotifData()
	{
		return notifData == null ? "{}" : notifData;
	}
	
	public JSONObject getNotifDataJSON() 
	{
		if(notifData != null ) 
		{
			try
			{
				return new JSONObject(notifData);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		return new JSONObject();
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

	public void setConfigData(String configData)
	{
		this.configData = configData;
		setConfigDataRefreshed(true);
	}

	public void setNotifData(String notifData)
	{
		this.notifData = notifData;
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
		this.configData = builder.configData;
		this.namespace = builder.namespace;
		this.notifData = builder.notifData;
		this.helperData = builder.helperData;
		this.setOnHike(true);
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
	 * @return the configDataRefreshed
	 */
	public boolean isConfigDataRefreshed()
	{
		return configDataRefreshed.get();
	}

	/**
	 * @param configDataRefreshed the configDataRefreshed to set
	 */
	public void setConfigDataRefreshed(boolean configDataRefreshed)
	{
		this.configDataRefreshed.set(configDataRefreshed);
	}

	/**
	 * @return the isBackPressAllowed
	 */
	public boolean getIsBackPressAllowed()
	{
		return isBackPressAllowed.get();
	}

	/**
	 * @param isBackPressAllowed the isBackPressAllowed to set
	 */
	public void setIsBackPressAllowed(boolean isBackPressAllowed)
	{
		this.isBackPressAllowed.set(isBackPressAllowed);
	}
	
	/**
	 * Utility method to get the last message text to be displayed on the homeScreen
	 * 
	 * @return
	 */
	public String getLastMessageText()
	{
		if (isNonMessagingBot())
		{
			if (metadata != null)
			{
				try
				{
					JSONObject md = new JSONObject(metadata);
					JSONObject cardObj = md.getJSONObject(HikePlatformConstants.CARD_OBJECT);
					String hmText = cardObj.optString(HikePlatformConstants.HIKE_MESSAGE, "");
					return hmText;
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}
		}

		else
		{
			if (lastConversationMsg != null)
			{
				return lastConversationMsg.getMessage();
			}
		}

		return "";
	}
	
	@Override
	public void setUnreadCount(int unreadCount)
	{		
		if (isMessagingBot())
		{
			setMessagingBotUnreadCount(unreadCount);
			return;
		}
		else if(isNonMessagingBot())
		{
			setNonMessagingBotUnreadCount(unreadCount);
            return;			
		}
		super.setUnreadCount(unreadCount);
	}
	
	private void setNonMessagingBotUnreadCount(int unreadCount)
	{
		NonMessagingBotConfiguration configuration = new NonMessagingBotConfiguration(getConfiguration());
		if (configuration.isHideUnread())
		{
			super.setUnreadCount(0); 
			return;
		}
		else if(configuration.isShowUnreadOne() && unreadCount > 0)
		{
			super.setUnreadCount(1); 
			return;
		}
		super.setUnreadCount(unreadCount);
	}

	private void setMessagingBotUnreadCount(int unreadCount)
	{
		MessagingBotMetadata messagingBotMetadata = new MessagingBotMetadata(getMetadata());
		MessagingBotConfiguration configuration = new MessagingBotConfiguration(getConfiguration(), messagingBotMetadata.isReceiveEnabled());
		if (configuration.isHideUnread())
		{
			super.setUnreadCount(0); 
			return;
		}
		else if(configuration.isShowUnreadOne() && unreadCount > 0)
		{
			super.setUnreadCount(1); 
			return;
		}
		super.setUnreadCount(unreadCount);
	}

	@Override
	public String getUnreadCountString()
	{
	   if(isNonMessagingBot())
		{
			NonMessagingBotConfiguration configuration = new NonMessagingBotConfiguration(getConfiguration());
			
			if(configuration.isShowUnreadCount() && getUnreadCount() > 1)
			{
				return DEFAULT_UNREAD_COUNT ; 
			}
		}
		return super.getUnreadCountString();
	}
	
}
