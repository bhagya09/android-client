package com.bsb.hike.models.Conversation;

import java.util.ArrayList;

import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.PairModified;

/**
 * This class contains the core fields which are required for a 1-n conversation entity to be displayed on the ConversationFragment screen. This is the atomic unit for 1-n
 * conversation to be displayed on the home screen.
 * 
 * @author Anu/Piyush
 */
public class OneToNConvInfo extends ConvInfo
{
	private boolean isConversationAlive;

	protected OneToNConvInfo(InitBuilder<?> builder)
	{
		super(builder);
		this.setConversationAlive(builder.isConversationAlive);
	}

	/**
	 * @return the isConversationAlive
	 */
	public boolean isConversationAlive()
	{
		return isConversationAlive;
	}

	/**
	 * @param isConversationAlive
	 *            the isConversationAlive to set
	 */
	public void setConversationAlive(boolean isConversationAlive)
	{
		this.isConversationAlive = isConversationAlive;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.bsb.hike.models.Conversation.ConvInfo#getLabel()
	 */
	@Override
	public String getLabel()
	{
		if (!TextUtils.isEmpty(getConversationName()))
			return getConversationName();
		else
		{
			// Before contact manager we were adding all the group participants to conversation object initially when getConversations of HikeConversationDatabase is called
			// But now we do lazy loading, we don't have group participants when we are on home screen
			// In case of empty group name, group Participants are needed so setting it here.
			return OneToNConversation.defaultConversationName(ContactManager.getInstance().getGroupParticipants(getMsisdn(), false, false));
		}
	}
	
	protected static abstract class InitBuilder<P extends InitBuilder<P>> extends ConvInfo.InitBuilder<P>
	{
		private boolean isConversationAlive;

		protected InitBuilder(String msisdn)
		{
			super(msisdn);
		}

		public P setConversationAlive(boolean alive)
		{
			this.isConversationAlive = alive;
			return getSelfObject();
		}
		
		@Override
		public ConvInfo build()
		{
			if (this.validateConvInfo())
			{
				return new OneToNConvInfo(this);
			}
			return null;
		}
	}

	public static class ConvInfoBuilder extends OneToNConvInfo.InitBuilder<ConvInfoBuilder>
	{

		public ConvInfoBuilder(String msisdn)
		{
			super(msisdn);
		}

		@Override
		protected ConvInfoBuilder getSelfObject()
		{
			return this;
		}

	}

	/**
	 * 
	 * @return conversation participant name
	 */
	public String getConvParticipantName(String groupParticipantMsisdn)
	{
		HikeMessengerApp.getContactManager().getContact(groupParticipantMsisdn, true, false);
		String name = HikeMessengerApp.getContactManager().getName(getMsisdn(), groupParticipantMsisdn);
		return name;
	}

	public void updateName()
	{
		setmConversationName(ContactManager.getInstance().getName(getMsisdn()));
	}
}