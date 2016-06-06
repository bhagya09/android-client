package com.bsb.hike.modules.contactmgr;

import android.database.DatabaseUtils;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.PhoneUtils;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TransientCache extends ContactsCache
{
	HikeUserDatabase hDb;

	// Transient Memory for contacts with their reference count
	private Map<String, PairModified<ContactInfo, Integer>> transientContacts;

	// Transient memory for contacts of group participants
	private Map<String, Map<String, PairModified<GroupParticipant, String>>> groupParticipants;

	private final ReentrantReadWriteLock readWriteLockTrans = new ReentrantReadWriteLock(true);

	private final Lock readLock = readWriteLockTrans.readLock();

	private final Lock writeLock = readWriteLockTrans.writeLock();

	/**
	 * Initializes all the transient maps
	 */
	TransientCache(HikeUserDatabase db)
	{
		transientContacts = new LinkedHashMap<String, PairModified<ContactInfo, Integer>>();
		groupParticipants = new HashMap<String, Map<String, PairModified<GroupParticipant, String>>>();
		hDb = db;
	}

	/**
	 * get contact info from memory. Returns null if not found in memory. This implementation is thread safe.
	 * 
	 * @param key
	 * @return
	 */
	ContactInfo getContact(String key)
	{
		readLock.lock();
		try
		{
			PairModified<ContactInfo, Integer> contactPair = transientContacts.get(key);
			if (null == contactPair)
			{
				return null;
			}
			return contactPair.getFirst();
		}
		finally
		{
			readLock.unlock();
		}
	}

	/**
	 * Inserts contact in {@link #transientContacts}.
	 * <p>
	 * Make sure that it is not already in memory , we are setting reference count to one here
	 * </p>
	 * 
	 * @param contact
	 *            contactinfo to put in map
	 */
	void insertContact(ContactInfo contact)
	{
		if (null != contact)
		{
			writeLock.lock();
			try
			{
				PairModified<ContactInfo, Integer> contactPair = new PairModified<ContactInfo, Integer>(contact, 1);
				transientContacts.put(contact.getMsisdn(), contactPair);
			}
			finally
			{
				writeLock.unlock();
			}
		}
	}

	/**
	 * Inserts a list contacts in {@link #transientContacts}
	 * 
	 * @param contacts
	 */
	void insertContact(List<ContactInfo> contacts)
	{
		writeLock.lock();
		try
		{
			for (ContactInfo contact : contacts)
			{
				if (!transientContacts.containsKey(contact.getMsisdn()))
				{
					if (null != contact)
					{
						PairModified<ContactInfo, Integer> contactPair = new PairModified<ContactInfo, Integer>(contact, 1);
						transientContacts.put(contact.getMsisdn(), contactPair);
					}
				}
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Inserts the map of msisdn and <code>GroupParticipant</code> into {@link #groupParticipants} with <code>grpId</code> as key.
	 * 
	 * @param grpId
	 * @param groupParticipantsMap
	 */
	void insertGroupParticipants(String grpId, Map<String, PairModified<GroupParticipant, String>> groupParticipantsMap)
	{
		writeLock.lock();
		try
		{
			Map<String, PairModified<GroupParticipant, String>> groupParticipantsList = groupParticipants.get(grpId);
			if (null == groupParticipantsList)
			{
				groupParticipantsList = new HashMap<String, PairModified<GroupParticipant, String>>();
				groupParticipants.put(grpId, groupParticipantsList);
			}
			groupParticipantsList.putAll(groupParticipantsMap);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * This method adds groupParticipants only if there is an entry for a particular group in {@link #groupParticipants}.To ensure either all participants are added in the group or
	 * none
	 * 
	 * @param grpId
	 * @param groupParticipantsMap
	 */
	void addGroupParticipants(String grpId, Map<String, PairModified<GroupParticipant, String>> groupParticipantsMap)
	{
		writeLock.lock();
		try
		{
			Map<String, PairModified<GroupParticipant, String>> grpParticipants = groupParticipants.get(grpId);
			if (null != grpParticipants)
			{
				grpParticipants.putAll(groupParticipantsMap);
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Removes this contact from memory if reference count is one otherwise decrements the reference count
	 * 
	 * @param contact
	 */
	void removeContact(String msisdn)
	{
		writeLock.lock();
		try
		{
			if (OneToNConversationUtils.isOneToNConversation(msisdn))
			{
				removeGroup(msisdn);
			}
			else
			{
				PairModified<ContactInfo, Integer> contactPair = transientContacts.get(msisdn);
				if (null != contactPair)
				{
					contactPair.setSecond(contactPair.getSecond() - 1);
					if (contactPair.getSecond() == 0)
					{
						transientContacts.remove(msisdn);
					}
				}
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	private void removeGroupParticipantFromCache(String groupId, String msisdn)
	{
		Map<String, PairModified<GroupParticipant, String>> groupParticipantsList = groupParticipants.get(groupId);
		if (null != groupParticipantsList)
		{
			groupParticipantsList.remove(msisdn);
		}
	}

	/**
	 * Removes the group participant from {@link #groupParticipants} , should be called when user leaves a group chat.
	 * 
	 * @param groupId
	 * @param msisdn
	 */
	void removeGroupParticipants(String groupId, String msisdn)
	{
		writeLock.lock();
		try
		{
			removeGroupParticipantFromCache(groupId, msisdn);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Removes multiple from group participants from the {@link #groupParticipants} map
	 * 
	 * @param groupId
	 * @param msisdns
	 */
	void removeGroupParticipants(String groupId, Collection<String> msisdns)
	{
		writeLock.lock();
		try
		{
			for (String msisdn : msisdns)
			{
				removeGroupParticipantFromCache(groupId, msisdn);
			}
		}
		finally
		{
			writeLock.unlock();
		}

	}

	/**
	 * This method removes the group entry given by the parameter <code>groupId</code> from the {@link #groupParticipants} map
	 * 
	 * @param groupId
	 */
	void removeGroup(String groupId)
	{
		writeLock.lock();
		try
		{
			groupParticipants.remove(groupId);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * updates the contact in memory and if not found in memory inserts the contact in {@link #transientContacts}
	 * 
	 * @param contact
	 */
	void updateContact(ContactInfo contact)
	{
		writeLock.lock();
		try
		{
			for(Map<String, PairModified<GroupParticipant, String>> groupParticipantMap : groupParticipants.values())
			{
				if(groupParticipantMap.containsKey(contact.getMsisdn()))
				{
					PairModified<GroupParticipant, String> grpParticipant = groupParticipantMap.get(contact.getMsisdn());
					grpParticipant.getFirst().setContactInfo(contact);
					if(null != contact.getName())
					{
						grpParticipant.setSecond(contact.getName());
					}
				}
			}
			if (transientContacts.containsKey(contact.getMsisdn()))
			{
				PairModified<ContactInfo, Integer> contactPair = transientContacts.get(contact.getMsisdn());
				contactPair.setFirst(contact);
			}
			else
			{
				// if contact not found to be updated then add in the transient cache and we should set allContactsLoaded to false because if all contacts are loaded even then we
				// can't find some contacts in cache that means some new contacts have been added in addressbook
				insertContact(contact);
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Returns the list of all the contacts sorted by their names.If boolean {@link #allContactsLoaded} is true then all the contacts are retrieved from {@link #transientContacts}.
	 * This method returns both saved and unsaved contacts.
	 * 
	 * @return
	 */
	List<ContactInfo> getAllContacts()
	{
		return getAllContacts(false);
	}

	/**
	 * Returns the list of all the contacts sorted by their names.If boolean {@link #allContactsLoaded} is true then all the contacts are retrieved from {@link #transientContacts}.
	 * If parameter <code>ignoreUnknownContacts</code> is true then returned list only contains saved contacts otherwise both saved and unsaved contacts.
	 * 
	 * @param ignoreUnknownContacts
	 * @return
	 */
	List<ContactInfo> getAllContacts(boolean ignoreUnknownContacts)
	{
		if (!allContactsLoaded)
		{
			loadMemory();
			allContactsLoaded = true;
		}

		List<ContactInfo> allContacts = new ArrayList<ContactInfo>();
		// Traverse through transientContacts and add in allCOntacts list
		readLock.lock();
		try
		{
			for (Entry<String, PairModified<ContactInfo, Integer>> mapEntry : transientContacts.entrySet())
			{
				PairModified<ContactInfo, Integer> contactPair = mapEntry.getValue();
				ContactInfo contact = contactPair.getFirst();
				if (ignoreUnknownContacts && null == contact.getName())
					continue;
				allContacts.add(contactPair.getFirst());
			}
		}
		finally
		{
			readLock.unlock();
		}
		return allContacts;
	}

	/**
	 * This method returns all the contacts this also includes contactInfo of for same msisdn saved with different names in address book and used during contact syncing. Here
	 * everytime we have to make a db query because we only keep the latest contact for a particular msisdn in the cache and not all. We are not adding the contacts in the map here
	 * as when after sync updates we get the diff between phone address book and our database then we update that diff in the cache as well.
	 * 
	 * @return
	 */
	List<ContactInfo> getAllContactsForSyncing()
	{
		return hDb.getAllContactsForSyncing();
	}

	/**
	 * This method returns the name of contact using <code>msisdn</code> parameter.
	 * 
	 * @param msisdn
	 * @return
	 */
	String getName(String msisdn)
	{
		ContactInfo c = getContact(msisdn);
		if (null == c)
			return null;
		return c.getName();
	}

	/**
	 * Returns name of a contact using groupId. This first checks the contactInfo name as contact can be saved contact or unsaved contact.For unsaved we need group id as unsaved
	 * contacts name is different in different groups
	 * 
	 * @param groupId
	 * @param msisdn
	 * @return
	 */
	String getName(String groupId, String msisdn)
	{
		readLock.lock();
		try
		{
			String name = getName(msisdn);
			if (null == name)
			{
				Map<String, PairModified<GroupParticipant, String>> map = groupParticipants.get(groupId);
				if (null != map)
				{
					PairModified<GroupParticipant, String> grpParticipantPair = map.get(msisdn);
					if (null != grpParticipantPair)
						name = grpParticipantPair.getSecond();
				}
			}
			return name;
		}
		finally
		{
			readLock.unlock();
		}
	}

	/**
	 * Sets the name for a unsaved contact in {@link #groupParticipants}.
	 * 
	 * @param msisdn
	 * @param name
	 */
	void setUnknownContactName(String grpId, String msisdn, String name)
	{
		writeLock.lock();
		try
		{
			Map<String, PairModified<GroupParticipant, String>> groupParticipantMap = groupParticipants.get(grpId);
			if (null != groupParticipantMap)
			{
				PairModified<GroupParticipant, String> grpParticipantPair = groupParticipantMap.get(msisdn);
				if (null != grpParticipantPair)
				{
					GroupParticipant grpParticipant = grpParticipantPair.getFirst();
					groupParticipantMap.put(msisdn, new PairModified<GroupParticipant, String>(grpParticipant, name));
				}
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}
	
	void updateGroupParticipantDetail(String grpId, String msisdn)
	{
		writeLock.lock();
		try
		{
			Map<String, PairModified<GroupParticipant, String>> groupParticipantMap = groupParticipants.get(grpId);
			if (null != groupParticipantMap)
			{
				PairModified<GroupParticipant, String> grpParticipantPair = groupParticipantMap.get(msisdn);
				if (null != grpParticipantPair)
				{
					GroupParticipant grpParticipant = grpParticipantPair.getFirst();
					grpParticipant.setType(GroupParticipant.Participant_Type.ADMIN);
				}
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * This function will load all the contacts from DB into transient storage. Contacts which are in persistence map will not be loaded into it.
	 */
	void loadMemory()
	{
		Map<String, ContactInfo> contactMap = hDb.getAllContactInfo();
		LinkedHashMap<String, PairModified<ContactInfo, Integer>> temp = new LinkedHashMap<String, PairModified<ContactInfo, Integer>>();
		writeLock.lock();
		try
		{
			for (Entry<String, ContactInfo> mapEntry : contactMap.entrySet())
			{
				String msisdn = mapEntry.getKey();
				ContactInfo contact = mapEntry.getValue();
				ContactManager.getInstance().updateContacts(contact); // updates in persistence if availabale in persistence
				/*
				 * Since we maintain same reference to contactInfo object in persistence and transient cache , updateContacts method will update the contact in both persistence and
				 * transient cache both if contact is already in persistence and transient cache respectively. So if we get contactpair from transient cache as not null that means
				 * it is updated otherwise we have to create a new object and insert in transient cache which is done using a temp map.
				 */
				PairModified<ContactInfo, Integer> contactPair = transientContacts.get(msisdn);
				if (null == contactPair)
				{
					contactPair = new PairModified<ContactInfo, Integer>(contact, 1);
				}
				else
				{
					contactPair.setFirst(contact);
				}
				temp.put(msisdn, contactPair);

				/*
				 * temp map is needed here because transient cache can contain both saved and unsaved contacts , by executing the below line we are removing the saved from
				 * transient cache as they are already inserted in temp.
				 */
				transientContacts.remove(msisdn);
			}

			/*
			 * Now transient cache contains only unsaved contacts we are adding them to temp map and then assigning temp to transient contacts
			 */
			temp.putAll(transientContacts);
			transientContacts = temp;
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Loads the contact info for a msisdn in transient memory and returns the same
	 * 
	 * @param msisdn
	 * @param ifNotFoundReturnNull
	 * @return
	 */
	ContactInfo putInCache(String msisdn, boolean ifNotFoundReturnNull)
	{
		ContactInfo c = hDb.getContactInfoFromMSISDN(msisdn, ifNotFoundReturnNull);
		insertContact(c);
		return c;
	}

	/**
	 * This method loads the contactInfo of msisdns in transient memory and returns the list of same
	 * 
	 * @param msisdns
	 * @return
	 */
	List<ContactInfo> putInCache(List<String> msisdns)
	{
		List<ContactInfo> contactsList = new ArrayList<ContactInfo>();
		if (msisdns.size() > 0)
		{
			Map<String, ContactInfo> map = hDb.getContactInfoFromMsisdns(msisdns, true);

			for (Entry<String, ContactInfo> mapEntry : map.entrySet())
			{
				ContactInfo contact = mapEntry.getValue();
				insertContact(contact);
			}
			contactsList.addAll(map.values());
		}
		return contactsList;
	}

	/**
	 * 
	 * @param onHike
	 * @param myMsisdn
	 * @param nativeSMSOn
	 * @param ignoreUnknownContacts
	 * @return
	 */
	List<ContactInfo> getNOTFRIENDScontacts(int onHike, String myMsisdn, boolean nativeSMSOn, boolean ignoreUnknownContacts)
	{
		List<ContactInfo> contacts = new ArrayList<ContactInfo>();
		Set<String> blockSet = ContactManager.getInstance().getBlockedMsisdnSet();
		if (allContactsLoaded)
		{
			readLock.lock();
			try
			{
				for (Entry<String, PairModified<ContactInfo, Integer>> savedMapEntry : transientContacts.entrySet())
				{
					String msisdn = savedMapEntry.getKey();
					if (!blockSet.contains(msisdn) && (null != myMsisdn) && !msisdn.equals(myMsisdn))
					{
						PairModified<ContactInfo, Integer> contactPair = savedMapEntry.getValue();
						ContactInfo contact = contactPair.getFirst();
						if (!(ignoreUnknownContacts && (null == contact.getName())))
						{
							contacts.add(contact);
						}
					}
				}
			}
			finally
			{
				readLock.unlock();
			}
		}
		else
		{
			Map<String, ContactInfo> map = hDb.getNOTFRIENDScontactsFromDB(blockSet, onHike, myMsisdn, nativeSMSOn, ignoreUnknownContacts);
			if (map != null)
			{
				contacts.addAll(map.values());
			}
			insertContact(contacts);
		}
		return contacts;
	}

	/**
	 * 
	 * @param favoriteType
	 * @param onHike
	 * @param myMsisdn
	 * @param nativeSMSOn
	 * @param ignoreUnknownContacts
	 * @return
	 */
	List<ContactInfo> getContactsOfFavoriteType(FavoriteType[] favoriteType, int onHike, String myMsisdn, boolean nativeSMSOn, boolean ignoreUnknownContacts)
	{
		List<ContactInfo> contacts = new ArrayList<ContactInfo>();
		Set<String> blockSet = ContactManager.getInstance().getBlockedMsisdnSet();
		if (allContactsLoaded)
		{
			boolean flag;

			readLock.lock();
			try
			{
				for (Entry<String, PairModified<ContactInfo, Integer>> savedMapEntry : transientContacts.entrySet())
				{
					String msisdn = savedMapEntry.getKey();
					PairModified<ContactInfo, Integer> contactPair = savedMapEntry.getValue();
					ContactInfo contactInfo = contactPair.getFirst();

					if (ignoreUnknownContacts && (null == contactInfo.getName()))
					{
						continue;
					}

					if (onHike != HikeConstants.BOTH_VALUE)
					{
						if (!(contactInfo.isOnhike() == (onHike == 1 ? true : false)))
						{
							continue;
						}

						if (onHike == HikeConstants.NOT_ON_HIKE_VALUE)
						{
							if (!msisdn.contains("+91"))
							{
								continue;
							}
						}
					}

					if (!nativeSMSOn)
					{
						if (!(contactInfo.isOnhike() || msisdn.contains("+91")))
						{
							continue;
						}
					}

					flag = false;
					for (FavoriteType favType : favoriteType)
					{
						if (contactInfo.getFavoriteType() == favType)
						{
							flag = true;
							break;
						}
					}

					if (flag && !blockSet.contains(msisdn) && (null != myMsisdn) && !msisdn.equals(myMsisdn))
					{
						contacts.add(contactInfo);
					}
				}
			}
			finally
			{
				readLock.unlock();
			}
		}
		else
		{
			Map<String, ContactInfo> map = hDb.getContactsOfFavoriteTypeDB(favoriteType, blockSet, onHike, myMsisdn, nativeSMSOn, ignoreUnknownContacts);
			if (map != null)
			{
				contacts.addAll(map.values());
			}
			insertContact(contacts);
		}
		return contacts;
	}

	/**
	 * 
	 * @param limit
	 * @param msisdnsIn
	 * @param msisdnsNotIn
	 * @param myMsisdn
	 * @return
	 */
	List<ContactInfo> getHikeContacts(int limit, Set<String> msisdnsIn, Set<String> msisdnsNotIn, String myMsisdn)
	{
		List<ContactInfo> contacts = new ArrayList<ContactInfo>();
		if (allContactsLoaded)
		{
			readLock.lock();
			try
			{
				for (Entry<String, PairModified<ContactInfo, Integer>> mapEntry : transientContacts.entrySet())
				{
					String msisdn = mapEntry.getKey();
					ContactInfo contactInfo = mapEntry.getValue().getFirst();
					if (null != msisdnsIn && msisdnsIn.contains(msisdn) && (null != msisdnsNotIn && !msisdnsNotIn.contains(msisdn))
							&& (null != myMsisdn && !msisdn.equals(myMsisdn)) && contactInfo.isOnhike() && !contactInfo.isUnknownContact())
					{
						contacts.add(contactInfo);
						limit--;
						if (limit == 0)
							break;
					}
				}
			}
			finally
			{
				readLock.unlock();
			}
		}
		else
		{
			String msisdnsInStatement = PhoneUtils.getMsisdnStatement(msisdnsIn);
			String msisdnsNotInStatement = PhoneUtils.getMsisdnStatement(msisdnsNotIn);
			contacts = hDb.getHikeContacts(limit, msisdnsInStatement, msisdnsNotInStatement, myMsisdn);
			insertContact(contacts);
		}
		return contacts;
	}

	/**
	 * 
	 * @return
	 */
	List<Pair<AtomicBoolean, ContactInfo>> getNonHikeContacts()
	{
		List<Pair<AtomicBoolean, ContactInfo>> contacts = new ArrayList<Pair<AtomicBoolean, ContactInfo>>();
		Set<String> blockSet = ContactManager.getInstance().getBlockedMsisdnSet();
		if (allContactsLoaded)
		{
			readLock.lock();
			try
			{
				for (Entry<String, PairModified<ContactInfo, Integer>> savedMapEntry : transientContacts.entrySet())
				{
					String msisdn = savedMapEntry.getKey();
					if (!blockSet.contains(msisdn) && ContactManager.getInstance().isIndianMobileNumber(msisdn))
					{
						PairModified<ContactInfo, Integer> contactPair = savedMapEntry.getValue();
						ContactInfo contactInfo = contactPair.getFirst();
						if (!contactInfo.isOnhike() && (null != contactInfo.getName()))
						{
							contacts.add(new Pair<AtomicBoolean, ContactInfo>(new AtomicBoolean(false), contactInfo));
						}
					}
				}
			}
			finally
			{
				readLock.unlock();
			}
		}
		else
		{
			contacts = hDb.getNonHikeContacts(blockSet);
			writeLock.lock();
			try
			{
				for (Pair<AtomicBoolean, ContactInfo> con : contacts)
				{
					ContactInfo contact = con.second;
					if (!transientContacts.containsKey(contact.getMsisdn()))
					{
						PairModified<ContactInfo, Integer> contactPair = new PairModified<ContactInfo, Integer>(contact, 1);
						transientContacts.put(contact.getMsisdn(), contactPair);
					}
				}
			}
			finally
			{
				writeLock.unlock();
			}
		}
		return contacts;
	}

	/**
	 * 
	 * @param limit
	 * @return
	 */
	List<ContactInfo> getNonHikeMostContactedContacts(String phoneNumbers, final Map<String, Integer> mostContactedValues, int limit)
	{
		List<ContactInfo> contacts = new ArrayList<ContactInfo>();
		if (allContactsLoaded)
		{
			readLock.lock();
			try
			{
				for (Entry<String, PairModified<ContactInfo, Integer>> savedMapEntry : transientContacts.entrySet())
				{
					ContactInfo contactInfo = savedMapEntry.getValue().getFirst();
					String phoneNum = contactInfo.getPhoneNum();
					if (!TextUtils.isEmpty(phoneNumbers) && !TextUtils.isEmpty(phoneNum) && phoneNumbers.contains(DatabaseUtils.sqlEscapeString(phoneNum)) && !contactInfo.isOnhike() && (null != contactInfo.getName()))
					{
						contacts.add(contactInfo);
						limit = limit - 1;
						if (limit == 0)
							break;
					}
				}
			}
			finally
			{
				readLock.unlock();
			}

			Collections.sort(contacts, new Comparator<ContactInfo>()
			{
				@Override
				public int compare(ContactInfo lhs, ContactInfo rhs)
				{
					int lhsContactNum = mostContactedValues.get(lhs.getPhoneNum());
					int rhsContactNum = mostContactedValues.get(rhs.getPhoneNum());

					if (lhsContactNum != rhsContactNum)
					{
						return -((Integer) lhsContactNum).compareTo(rhsContactNum);
					}
					return lhs.getName().toLowerCase().compareTo(rhs.getName().toLowerCase());
				}
			});
		}
		else
		{
			contacts = hDb.getNonHikeMostContactedContactsFromListOfNumbers(phoneNumbers, mostContactedValues, limit);
			insertContact(contacts);
		}
		return contacts;
	}

	/**
	 * Traverse through all the contacts in transient cache to find if any contactInfo object contains given number as phoneNumber. This method returns null if not found in memory
	 * and not makes a DB call.
	 * 
	 * @param number
	 * @return
	 */
	public ContactInfo getContactInfoFromPhoneNo(String number)
	{
		ContactInfo contact = null;
		if (allContactsLoaded)
		{
			readLock.lock();
			try
			{
				for (Entry<String, PairModified<ContactInfo, Integer>> mapEntry : transientContacts.entrySet())
				{
					PairModified<ContactInfo, Integer> contactPair = mapEntry.getValue();
					String phoneNum = contactPair.getFirst().getPhoneNum();
					if (null != contactPair && null != number && phoneNum.equals(number))
					{
						contact = contactPair.getFirst();
						break;
					}
				}
			}
			finally
			{
				readLock.unlock();
			}
		}
		else
		{
			contact = hDb.getContactInfoFromPhoneNo(number);
			if (null != contact && null == getContact(contact.getMsisdn()))
			{
				insertContact(contact);
			}
		}
		return contact;
	}

	/**
	 * This method is used when <code>number</code> can be msisdn or phone number . First we check in cache assuming number is msisdn , if not found then traverse through all the
	 * contacts in transient cache to find if any contactInfo object contains given number as phoneNumber And if not found in transient cache we make a DB call.
	 * 
	 * @param number
	 * @return
	 */
	public ContactInfo getContactInfoFromPhoneNoOrMsisdn(String number)
	{
		ContactInfo contact = getContact(number);

		if (null != contact)
		{
			return contact;
		}

		if (allContactsLoaded)
		{
			readLock.lock();
			try
			{
				for (Entry<String, PairModified<ContactInfo, Integer>> mapEntry : transientContacts.entrySet())
				{
					PairModified<ContactInfo, Integer> contactPair = mapEntry.getValue();
					String phoneNum = contactPair.getFirst().getPhoneNum();
					if (null != contactPair && null != number && phoneNum.equals(number))
					{
						contact = contactPair.getFirst();
						break;
					}
				}
			}
			finally
			{
				readLock.unlock();
			}
		}
		else
		{
			contact = hDb.getContactInfoFromPhoneNoOrMsisdn(number);
			if (null != contact && null == getContact(contact.getMsisdn()))
			{
				insertContact(contact);
			}
		}
		return contact;
	}

    /**
     * This method is used for msisdns or phone numbers list. It will make a db call and insert the contacts in the cache
     */
    public List<ContactInfo> getContactListFromDb(List<String> msisdnList)
    {
        List<ContactInfo> contactInfoList;
        String msisdnString = Utils.listToString(msisdnList,",");

        contactInfoList = hDb.getContactInfoListFromPhoneNoOrMsisdn(msisdnString);

        if(contactInfoList == null)
            return null;

        for (ContactInfo contact: contactInfoList)
        {
            if (null != contact && null == getContact(contact.getMsisdn()))
            {
                insertContact(contact);
            }
        }
        return contactInfoList;
    }


	/**
	 * clears the transient memory
	 */
	void clearMemory()
	{
		writeLock.lock();
		try
		{
			if (null != transientContacts)
			{
				transientContacts.clear();
			}

			if (null != groupParticipants)
			{
				groupParticipants.clear();
			}

			allContactsLoaded = false;
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * This method is used to get group Participants of a particular group specified by <code>groupId</code>. First {@link #groupParticipants} is checked if they are loaded in
	 * memory or not , if not then database query is executed and participants are put in the cache {@link #groupParticipants} map.
	 * 
	 * @param groupId
	 * @param activeOnly
	 * @param notShownStatusMsgOnly
	 * @param fetchParticipants
	 * @return
	 */
	Pair<Map<String, PairModified<GroupParticipant, String>>, List<String>> getGroupParticipants(String groupId, boolean activeOnly, boolean notShownStatusMsgOnly,
			boolean fetchParticipants)
	{
		if (!notShownStatusMsgOnly)
		{
			readLock.lock();
			try
			{
				Map<String, PairModified<GroupParticipant, String>> groupParticipantsMap = groupParticipants.get(groupId);
				if (null != groupParticipantsMap)
				{
					Map<String, PairModified<GroupParticipant, String>> grpParticipants = new HashMap<String, PairModified<GroupParticipant, String>>();
					for (Entry<String, PairModified<GroupParticipant, String>> mapEntry : groupParticipantsMap.entrySet())
					{
						String msisdn = mapEntry.getKey();
						GroupParticipant grp = mapEntry.getValue().getFirst();
						if (!activeOnly)
						{
							grpParticipants.put(msisdn, mapEntry.getValue());
						}
						else
						{
							if (!grp.hasLeft())
							{
								grpParticipants.put(msisdn, mapEntry.getValue());
							}
						}
					}
					return new Pair<Map<String, PairModified<GroupParticipant, String>>, List<String>>(grpParticipants, null);
				}
			}
			finally
			{
				readLock.unlock();
			}
		}
		return HikeConversationsDatabase.getInstance().getGroupParticipants(groupId, activeOnly, notShownStatusMsgOnly);
	}

	/**
	 * This is used for cases when we getGroupParticipants and if found in transient cache we have to increase the reference count by one.
	 * <p>
	 * Since for getting contact info for group participants , we have to check in both persistence and transient cache .These things are done in
	 * {@link ContactManager#getGroupParticipants(String, boolean, boolean, boolean)} where if contact is found in transient cache then it's reference count should be incremented.
	 * </p>
	 * 
	 * @param msisdn
	 */
	void incrementRefCount(String msisdn)
	{
		writeLock.lock();
		try
		{
			PairModified<ContactInfo, Integer> contactPair = transientContacts.get(msisdn);
			if (null != contactPair)
			{
				contactPair.setSecond(contactPair.getSecond() + 1);
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Returns the count of number of participants in a particular group.
	 * 
	 * @param groupId
	 * @return
	 */
	int getGroupParticipantsCount(String groupId)
	{
		readLock.lock();
		try
		{
			int size = 0;
			Map<String, PairModified<GroupParticipant, String>> grpParticipantsMap = groupParticipants.get(groupId);
			if (null != grpParticipantsMap)
			{
				for (Entry<String, PairModified<GroupParticipant, String>> mapEntry : grpParticipantsMap.entrySet())
				{
					PairModified<GroupParticipant, String> grpPair = mapEntry.getValue();
					if (null != grpPair)
					{
						GroupParticipant grp = grpPair.getFirst();
						if (!grp.hasLeft())
							size++;
					}
				}
				return size;
			}
		}
		finally
		{
			readLock.unlock();
		}
		return HikeConversationsDatabase.getInstance().getActiveParticipantCount(groupId);

	}

	/**
	 * This method returns whether contact exist or not if all contacts are loaded then we can get this from memory otherwise we make a DB query
	 * 
	 * @param msisdn
	 * @return
	 */
	boolean doesContactExist(String msisdn)
	{
		ContactInfo contact = getContact(msisdn);
		if (allContactsLoaded)
		{
			return (null != contact && null != contact.getName());
		}
		else
		{
			if (null != contact && null != contact.getName())
				return true;
			else
			{
				return hDb.doesContactExist(msisdn);
			}
		}
	}

	/**
	 * This returns the list of pair of boolean and {@link ContactInfo} objects, boolean tells whether a contact is blocked or not.
	 * 
	 * @return
	 */
	List<Pair<AtomicBoolean, ContactInfo>> getBlockedUserList()
	{
		List<ContactInfo> contacts = getAllContacts(true);
		List<Pair<AtomicBoolean, ContactInfo>> blockedUserList = new ArrayList<Pair<AtomicBoolean, ContactInfo>>();
		List<Pair<AtomicBoolean, ContactInfo>> allUserList = new ArrayList<Pair<AtomicBoolean, ContactInfo>>();
		Set<String> blockedMsisdns = ContactManager.getInstance().getBlockedMsisdnSet();
		for (ContactInfo contact : contacts)
		{
			if ((blockedMsisdns.contains(contact.getMsisdn())))
			{
				blockedUserList.add(new Pair<AtomicBoolean, ContactInfo>(new AtomicBoolean(true), contact));
				blockedMsisdns.remove(contact.getMsisdn());
			}
			else
			{
				allUserList.add(new Pair<AtomicBoolean, ContactInfo>(new AtomicBoolean(false), contact));
			}
		}

		for (String ms : blockedMsisdns)
		{
			String name = ms;
			if (HikeMessengerApp.hikeBotInfoMap.containsKey(ms))
				name = HikeMessengerApp.hikeBotInfoMap.get(ms).getConversationName();
			blockedUserList.add(new Pair<AtomicBoolean, ContactInfo>(new AtomicBoolean(true), new ContactInfo(ms, ms, name, ms)));
		}
		blockedUserList.addAll(allUserList);
		return blockedUserList;
	}

	/**
	 * Clears the memory {@see #clearMemory()} and make all references null
	 */
	void shutdown()
	{
		clearMemory();
		hDb = null;
		transientContacts = null;
		groupParticipants = null;
	}
	
	void updateContactDetailInAllGroups(String contact)
	{
		writeLock.lock();
		try
		{
			for(Map<String, PairModified<GroupParticipant, String>> groupParticipantMap : groupParticipants.values())
			{
				if(groupParticipantMap.containsKey(contact))
				{
					PairModified<GroupParticipant, String> grpParticipant = groupParticipantMap.get(contact);
					grpParticipant.getFirst().setType(GroupParticipant.Participant_Type.MEMBER);
					
				}
			}
			
		}
		finally
		{
			writeLock.unlock();
		}
	}
}
