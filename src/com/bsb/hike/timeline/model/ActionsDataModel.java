package com.bsb.hike.timeline.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

import com.bsb.hike.models.ContactInfo;

/**
 * Contains action count and actor contact info objects
 * 
 * @author Atul M
 */
public class ActionsDataModel
{
	private int count;

	private LinkedHashSet<ContactInfo> contactInfoList;

	private ActionsDataModel.DataTypes type;

	public enum DataTypes
	{
		LIKE, COMMENT, VIEWS
	}

	private ActionsDataModel()
	{
		// Set default params
	}

	public ActionsDataModel(ActionsDataModel.DataTypes argType)
	{
		this();
		type = argType;
	}

	public int getCount()
	{
		return count;
	}

	public ActionsDataModel.DataTypes getType()
	{
		return type;
	}

	public void setType(ActionsDataModel.DataTypes type)
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

	public boolean addContact(Collection<ContactInfo> argContactInfo)
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
