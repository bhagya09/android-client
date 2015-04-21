package com.bsb.hike.models.Conversation;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.HikeAnalyticsEvent;

/**
 * Conversation primitive for configurable bot chats.
 * 
 * @author Anu/Piyush
 */
public class BotConversation extends OneToOneConversation
{

	/**
	 * This member will be used for defining configurable properties for a bot conversation like enabling stickers, chat theme, VoIP call etc.We will be using bits for setting
	 * properties
	 */
	private short properties = 0;

	/**
	 * 
	 */
	private BotConversation(InitBuilder<?> builder)
	{
		super(builder);
		this.properties = builder.properties;
		this.isOnHike = true;
		/**
		 * Setting the mute state in the constructor itself as it is needed for BotConversations
		 */
		setIsMute(HikeConversationsDatabase.getInstance().isBotMuted(getMsisdn()));
	}

	/**
	 * @return the properties
	 */
	public short getProperties()
	{
		return properties;
	}

	/**
	 * @param properties
	 *            the properties to set
	 */
	public void setProperties(short properties)
	{
		this.properties = properties;
	}

	public static void analyticsForBots(ConvInfo convInfo, String key, String subType)
	{
		JSONObject json = new JSONObject();
		try
		{
			json.put(AnalyticsConstants.EVENT_KEY, key);
			json.put(AnalyticsConstants.ORIGIN, HikePlatformConstants.CONVERSATION_FRAGMENT);
			json.put(AnalyticsConstants.UNREAD_COUNT, convInfo.getUnreadCount());
			json.put(AnalyticsConstants.CHAT_MSISDN, convInfo.getMsisdn());
			HikeAnalyticsEvent.analyticsForBots(AnalyticsConstants.UI_EVENT, subType, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Builder base class extending {@link OneToOneConversation.InitBuilder}
	 * 
	 * @author piyush
	 * 
	 * @param <P>
	 */
	protected static abstract class InitBuilder<P extends InitBuilder<P>> extends OneToOneConversation.InitBuilder<P>
	{
		private short properties;

		public InitBuilder(String msisdn)
		{
			super(msisdn);
		}

		public P setProperties(short property)
		{
			this.properties = property;
			return getSelfObject();
		}

		public BotConversation build()
		{
			return new BotConversation(this);
		}
	}

	/**
	 * Builder class used to generating {@link BotConversation}
	 * <p>
	 * Bare bone Usage : BotConversation conv = BotConversation.ConversationBuilder(msisdn).build();<br>
	 * Other examples : BotConversation conv = BotConversation.ConversationBuilder(msisdn).setConvName("ABC").setProperties(127).build();
	 * 
	 * @author piyush
	 * 
	 */
	public static class ConversationBuilder extends BotConversation.InitBuilder<ConversationBuilder>
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
