package com.bsb.hike.models.Conversation;

import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Mute;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import com.bsb.hike.chatthemes.ChatThemeManager;

/**
 * Conversation objects will be made from this abstract class
 * 
 * @author Anu/Piyush
 * 
 */
public abstract class Conversation implements Comparable<Conversation>
{
	protected ConvInfo convInfo;

	protected ArrayList<ConvMessage> messagesList;

	protected ConversationMetadata metadata;

	/**
	 * Default value of chat theme
	 */
	protected String chatThemeId = ChatThemeManager.getInstance().defaultChatThemeId;

	protected Conversation(InitBuilder<?> builder)
	{
		this.convInfo = builder.convInfo;
		this.messagesList = builder.messagesList;
		this.metadata = builder.metadata;
		this.chatThemeId = builder.chatThemeId;
	}

	/**
	 * @param convInfo
	 *            the convInfo to set
	 */
	protected void setConvInfo(ConvInfo convInfo)
	{
		this.convInfo = convInfo;
	}

	/**
	 * @return the convInfo
	 */
	public ConvInfo getConvInfo()
	{
		return convInfo;
	}

	public void setMute(Mute mute)
	{
		convInfo.setMute(mute);
	}

	public Mute getMute()
	{
		return convInfo.getMute();
	}

	/**
	 * @return the messagesList
	 */
	public ArrayList<ConvMessage> getMessagesList()
	{
		return messagesList;
	}

	/**
	 * @param messagesList
	 *            the messagesList to set
	 */
	public void setMessagesList(ArrayList<ConvMessage> messagesList)
	{
		this.messagesList = messagesList;
	}

	/**
	 * @return the metadata
	 */
	public ConversationMetadata getMetadata()
	{
		return metadata;
	}

	/**
	 * @param metadata
	 *            the metadata to set
	 */
	public void setMetadata(ConversationMetadata metadata)
	{
		this.metadata = metadata;
	}

	/**
	 * @return the chatTheme
	 */
	public String getChatThemeId()
	{
		return chatThemeId;
	}

	/**
	 * By default every conversation is assumed to be in Hike's ecosystem
	 * 
	 * @return
	 */
	public boolean isOnHike()
	{
		return true;
	}

	/**
	 * @param isOnHike
	 *            the isOnHike to set
	 */
	public void setOnHike(boolean isOnHike)
	{
		convInfo.setOnHike(isOnHike);
	}
	
	/**
	 * @param chatThemeId
	 *            the chatTheme to set
	 */
	public void setChatThemeId(String chatThemeId)
	{
		this.chatThemeId = chatThemeId;
	}

	public String getMsisdn()
	{
		return convInfo.getMsisdn();
	}

	public String getConversationName()
	{
		return convInfo.getConversationName();
	}

	public void setConversationName(String convName)
	{
		convInfo.setmConversationName(convName);
	}

	public void setIsMute(boolean isMute)
	{
		convInfo.setIsMute(isMute);
	}

	public boolean isMuted()
	{
		return convInfo.isMute();
	}

	public void setShowNotifInMute(boolean muteNotification)
	{
		convInfo.setShowNotifInMute(muteNotification);
	}

	public boolean shouldShowNotifInMute()
	{
		return convInfo.shouldShowNotifInMute();
	}

	public void setMuteDuration(int muteDuration)
	{
		convInfo.setMuteDuration(muteDuration);
	}

	public int getMuteDuration()
	{
		return convInfo.getMuteDuration();
	}

	public boolean isBlocked()
	{
		return convInfo.isBlocked();
	}
	
	/**
	 * @param isBlocked
	 *            the isBlocked to set
	 */
	public void setBlocked(boolean isBlocked)
	{
		convInfo.setBlocked(isBlocked);
	}
	
	/**
	 * Returns a friendly label for the conversation
	 * 
	 * @return conversationName or msisdn
	 */
	public String getLabel()
	{
		return convInfo.getLabel();
	}

	public long getSortingTimeStamp()
	{
		return convInfo.getSortingTimeStamp();
	}

	public void setSortingTimeStamp(long timeStamp)
	{
		convInfo.setSortingTimeStamp(timeStamp);
	}

	public void setMessages(List<ConvMessage> messages)
	{
		this.messagesList = (ArrayList<ConvMessage>) messages;

		if (messagesList != null && !messagesList.isEmpty())
		{
			setSortingTimeStamp(messagesList.get(messagesList.size() - 1).getTimestamp());
		}
	}

	/**
	 * We update the last message because only the last message is shown in the conversation list view at home
	 * 
	 * @param message
	 *            Incoming ConvMessage object
	 */
	public void updateLastConvMessage(ConvMessage message)
	{
		convInfo.setLastConversationMsg(message);
		setSortingTimeStamp(message.getTimestamp());
	}

	public int getUnreadCount()
	{
		return convInfo.getUnreadCount();
	}

	public void setUnreadCount(int unreadCount)
	{
		convInfo.setUnreadCount(unreadCount);
	}

	public boolean isStealth()
	{
		return convInfo.isStealth();
	}

	public void setIsStealth(boolean isStealth)
	{
		convInfo.setStealth(isStealth);
	}

	@Override
	public String toString()
	{
		return convInfo.toString();
	}

	public JSONObject serialize(String type)
	{
		return this.convInfo.serialize(type);
	}

	@Override
	public int compareTo(Conversation other)
	{
		if (other == null)
		{
			return 1;
		}

		if (this.equals(other))
		{
			return 0;
		}

		return convInfo.compareTo(other.convInfo);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}

		if (obj == null)
		{
			return false;
		}

		if (getClass() != obj.getClass())
		{
			return false;
		}

		Conversation other = (Conversation) obj;

		return convInfo.equals(other.convInfo);
	}

	@Override
	public int hashCode()
	{
		return convInfo.hashCode();
	}

	/**
	 * Builder base class
	 * 
	 * @author piyush
	 * @param <P>
	 */
	protected static abstract class InitBuilder<P extends InitBuilder<P>>
	{
		protected ConvInfo convInfo;

		private String chatThemeId;

		private ArrayList<ConvMessage> messagesList;

		protected ConversationMetadata metadata;

		public InitBuilder(String msisdn)
		{
			convInfo = getConvInfo(msisdn);
		}

		public P setConvName(String convName)
		{
			convInfo.setmConversationName(convName);
			return getSelfObject();
		}

		public P setIsStealth(boolean isStealth)
		{
			convInfo.setStealth(isStealth);
			return getSelfObject();
		}

		public P setSortingTimeStamp(long timeStamp)
		{
			convInfo.setSortingTimeStamp(timeStamp);
			return getSelfObject();
		}

		public P setChatThemeId(String chatThemeId)
		{
			this.chatThemeId = chatThemeId;
			return getSelfObject();
		}

		public P setMessagesList(ArrayList<ConvMessage> messagesList)
		{
			this.messagesList = messagesList;
			return getSelfObject();
		}

		public P setConversationMetadata(ConversationMetadata metadata)
		{
			this.metadata = metadata;
			return getSelfObject();
		}

		protected abstract P getSelfObject();
		
		protected abstract ConvInfo getConvInfo(String msisdn); 

	}
}
