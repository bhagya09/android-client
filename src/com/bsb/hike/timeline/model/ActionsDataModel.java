package com.bsb.hike.timeline.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

import android.text.TextUtils;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;

/**
 * Contains action count and actor contact info objects
 * 
 * @author Atul M
 */
public class ActionsDataModel
{
	private int count;

	private LinkedHashSet<ContactInfo> contactInfoList;

	private ActionsDataModel.ActionTypes type;
	
	private static final int LIKE_ID = 1;
	
	private static final int UNLIKE_ID = 2;
	
	private static final int COMMENT_ID = 3;
	
	private static final int VIEW_ID = 4;
	
	//TODO Move to more generic class
	public static enum ActionTypes
	{
		LIKE(LIKE_ID), UNLIKE(UNLIKE_ID), COMMENT(COMMENT_ID), VIEW(VIEW_ID);

		int mKey;

		ActionTypes(int argKey)
		{
			mKey = argKey;
		}

		public int getKey()
		{
			return mKey;
		}

		public static ActionTypes getType(int type)
		{
			switch (type)
			{
			case LIKE_ID:
				return ActionTypes.LIKE;
			case UNLIKE_ID:
				return ActionTypes.UNLIKE;
			case COMMENT_ID:
				return ActionTypes.COMMENT;
			case VIEW_ID:
				return ActionTypes.VIEW;
			default:
				throw new IllegalArgumentException("Invalid ActionType key");
			}
		}
	}
	
	//TODO Move to more generic class
	public static enum ActivityObjectTypes
	{
		STATUS_UPDATE("su"), CARD("card"), CHANNEL("channel");

		String mTypeString;

		ActivityObjectTypes(String typeString)
		{
			mTypeString = typeString;
		}

		public String getTypeString()
		{
			return mTypeString;
		}
	}

	private ActionsDataModel()
	{
		// Set default params
	}

	public ActionsDataModel(ActionsDataModel.ActionTypes argType)
	{
		this();
		type = argType;
	}

	public int getCount()
	{
		return count;
	}

	public ActionsDataModel.ActionTypes getType()
	{
		return type;
	}

	public void setType(ActionsDataModel.ActionTypes type)
	{
		this.type = type;
	}

	public void setCount(int count)
	{
		this.count = count;
	}

	public LinkedHashSet<ContactInfo> getContactInfoList()
	{
		return contactInfoList;
	}

	public boolean addContacts(Collection<ContactInfo> argContactInfo)
	{
		if (argContactInfo == null)
		{
			throw new IllegalArgumentException("addContact(argContactInfo) : input ContactInfo cannot be null");
		}

		if (contactInfoList == null)
		{
			contactInfoList = new LinkedHashSet<ContactInfo>();
		}

		return contactInfoList.addAll(argContactInfo);
	}
	
	public boolean addContact(String msisdn)
	{
		if (TextUtils.isEmpty(msisdn))
		{
			throw new IllegalArgumentException("addContact(argContactInfo) : input msisdn cannot be null");
		}

		if (contactInfoList == null)
		{
			contactInfoList = new LinkedHashSet<ContactInfo>();
		}

		ContactInfo contactInfo = ContactManager.getInstance().getContactInfoFromPhoneNoOrMsisdn(msisdn);
		if (contactInfo != null)
		{
			return contactInfoList.add(contactInfo);
		}
		return false;
	}

	public boolean addContact(ContactInfo argContactInfo)
	{
		if (argContactInfo == null)
		{
			throw new IllegalArgumentException("addContact(argContactInfo) : input ContactInfo cannot be null");
		}

		if (contactInfoList == null)
		{
			contactInfoList = new LinkedHashSet<ContactInfo>();
		}

		return contactInfoList.add(argContactInfo);
	}

	public boolean removeContact(ContactInfo argContactInfo)
	{
		if (argContactInfo == null)
		{
			throw new IllegalArgumentException("removeContact(argContactInfo) : input ContactInfo cannot be null");
		}

		if (contactInfoList == null || contactInfoList.isEmpty())
		{
			return false;
		}

		return contactInfoList.remove(argContactInfo);
	}

	/**
	 * Note - May return null.
	 * 
	 * @param argMsisdn
	 * @return
	 */
	public ContactInfo getContactInfo(String argMsisdn)
	{
		if (contactInfoList == null || contactInfoList.isEmpty())
		{
			return null;
		}
		ContactInfo searchedCInfo = null;

		Iterator<ContactInfo> iterator = contactInfoList.iterator();

		while (iterator.hasNext())
		{
			searchedCInfo = iterator.next();
			if (searchedCInfo.getMsisdn().equals(argMsisdn))
			{
				return searchedCInfo;
			}
		}

		return null;
	}

	/**
	 * Note - May return null.
	 * 
	 * @return
	 */
	public ArrayList<String> getAllMsisdn()
	{
		if (contactInfoList == null || contactInfoList.isEmpty())
		{
			return null;
		}

		ArrayList<String> allMsisdnList = new ArrayList<String>();

		Iterator<ContactInfo> iterator = contactInfoList.iterator();

		while (iterator.hasNext())
		{
			ContactInfo cInfo = iterator.next();
			allMsisdnList.add(cInfo.getMsisdn());
		}

		return allMsisdnList;
	}

}
