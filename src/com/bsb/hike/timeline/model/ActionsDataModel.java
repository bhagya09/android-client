package com.bsb.hike.timeline.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

import org.json.JSONArray;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * Contains action count and actor contact info objects
 * 
 * @author Atul M
 */
public class ActionsDataModel
{
	private int totalCount;

	private LinkedHashSet<ContactInfo> contactInfoList = new LinkedHashSet<ContactInfo>();;

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
		STATUS_UPDATE("su"), CARD("card"), CHANNEL("channel"), UNKNOWN("unknown");

		String mTypeString;

		ActivityObjectTypes(String typeString)
		{
			mTypeString = typeString;
		}

		public String getTypeString()
		{
			return mTypeString;
		}
		
		public static ActivityObjectTypes getTypeFromString(String inputStr)
		{
			ActivityObjectTypes type = UNKNOWN;
			
			if(inputStr.equals(STATUS_UPDATE.getTypeString())){
				type = STATUS_UPDATE;
			}
			
			return type;
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

	public void setTotalCount(int count)
	{
		this.totalCount = count;
	}
	
	public int getTotalCount()
	{
		return totalCount;
	}

	public ActionsDataModel.ActionTypes getType()
	{
		return type;
	}

	public void setType(ActionsDataModel.ActionTypes type)
	{
		this.type = type;
	}

	public LinkedHashSet<ContactInfo> getContactInfoList()
	{
		return contactInfoList;
	}

	public String getContactsMsisdnJSON()
	{
		JSONArray jsonArray = new JSONArray();

		for (ContactInfo contact : contactInfoList)
		{
			jsonArray.put(contact.getMsisdn());
		}

		return jsonArray.toString();
	}
	
	public boolean addContacts(Collection<ContactInfo> argContactInfo)
	{
		if (argContactInfo == null)
		{
			throw new IllegalArgumentException("addContact(argContactInfo) : input ContactInfo cannot be null");
		}

		boolean isAdded = true;
		
		for(ContactInfo contact:argContactInfo)
		{
			boolean cAdd = addContact(contact.getMsisdn());
			if(!cAdd)
			{
				isAdded = cAdd;
			}
		}
		return isAdded;
	}
	
	public boolean addContact(String msisdn)
	{
		Logger.d(HikeConstants.TIMELINE_COUNT_LOGS, "adding: " + msisdn);

		if (TextUtils.isEmpty(msisdn))
		{
			throw new IllegalArgumentException("addContact(argContactInfo) : input msisdn cannot be null");
		}

		ContactInfo contactInfo = Utils.getUserContactInfo(true);

		if (!msisdn.equals(contactInfo.getMsisdn()))
		{
			contactInfo = ContactManager.getInstance().getContact(msisdn,true,true);
		}

		if (contactInfo != null)
		{
			// Check isAlready present
			for (ContactInfo cInfo : contactInfoList)
			{
				if (cInfo.getMsisdn().equals(contactInfo.getMsisdn()))
				{
					return false;
				}
			}

			Logger.d(HikeConstants.TIMELINE_COUNT_LOGS, "adding coninfo name: " + contactInfo.getName());
			boolean isAdded = contactInfoList.add(contactInfo);
			Logger.d(HikeConstants.TIMELINE_COUNT_LOGS, "adding " + (isAdded ? "issuccess" : "failed"));
			
			if(isAdded)
			{
				totalCount++;
			}
			
			return isAdded;
		}
		return false;
	}

	public boolean removeContact(String msisdn)
	{
		Logger.d(HikeConstants.TIMELINE_COUNT_LOGS, "removing: " + msisdn);
		if (TextUtils.isEmpty(msisdn))
		{
			throw new IllegalArgumentException("removeContact(argContactInfo) : input msisdn cannot be null");
		}

		if (contactInfoList == null || contactInfoList.isEmpty())
		{
			return false;
		}

		boolean isRemoved = false;

		for (ContactInfo cinfo : contactInfoList)
		{
			if (cinfo.getMsisdn().equals(msisdn))
			{
				isRemoved = contactInfoList.remove(cinfo);
				break;
			}
		}
		Logger.d(HikeConstants.TIMELINE_COUNT_LOGS, "isRemoved: " + isRemoved);
		
		if(isRemoved)
		{
			totalCount--;
		}
		
		return isRemoved;
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

	public boolean isLikedBySelf()
	{
		String selfMsisdn = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, null);

		if (getAllMsisdn() == null || getAllMsisdn().isEmpty())
		{
			return false;
		}
		return getAllMsisdn().contains(selfMsisdn);
	}

	@Override
	public String toString()
	{
		return "ActionsDataModel [count=" + totalCount + ", contactInfoList=" + contactInfoList + ", type=" + type + "]";
	}
	
	
}
