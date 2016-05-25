package com.bsb.hike.modules.contactmgr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.database.DatabaseUtils;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.models.PrivacyPreferences;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.PairModified;

class PersistenceCache extends ContactsCache
{
	private HikeUserDatabase hDb;

	// Memory persistence for all one to one conversation contacts that should always be loaded
	private Map<String, ContactInfo> convsContactsPersistence;

	// Memory persistence for all group conversation last messaged contacts with reference count that should always be loaded
	private Map<String, PairModified<ContactInfo, Integer>> groupContactsPersistence;

	// Memory persistence for all group names and list of msisdns(last message in group) that should always be loaded
	private Map<String, GroupDetails> groupPersistence;

	private Set<String> blockedMsisdns;

	private Map<String, PrivacyPreferences> lastSeenPrivacyList; // This list contains a subset of msisdns to which LS is enabled or disabled based on LS preferences

	private Map<String, PrivacyPreferences> statusUpdatePrivacyList; // This list contains a subset of msisdns for which SU disabled based on LS preferences

	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

	private final Lock readLock = readWriteLock.readLock();

	private final Lock writeLock = readWriteLock.writeLock();

	/**
	 * Initializes all the maps and calls {@link #loadMemory} which fills all the map when HikeService is started
	 */
	PersistenceCache(HikeUserDatabase db)
	{
		hDb = db;
		convsContactsPersistence = new HashMap<String, ContactInfo>();
		groupContactsPersistence = new HashMap<String, PairModified<ContactInfo, Integer>>();
		groupPersistence = new HashMap<String, GroupDetails>();
		blockedMsisdns = new HashSet<String>();
		lastSeenPrivacyList = new HashMap<String, PrivacyPreferences>();
		statusUpdatePrivacyList = new HashMap<String, PrivacyPreferences>();
		loadMemory();
	}

	/**
	 * get contact info from memory. Returns null if not found in memory. The implementation is thread safe.
	 * 
	 * @param key
	 * @return
	 */
	ContactInfo getContact(String key)
	{
		readLock.lock();
		try
		{
			ContactInfo c = null;
			c = convsContactsPersistence.get(key);
			if (null == c) // contact not found in persistence cache
			{
				PairModified<ContactInfo, Integer> contactPair = groupContactsPersistence.get(key);
				if (null != contactPair)
				{
					c = contactPair.getFirst();
				}
			}
			return c;
		}
		finally
		{
			readLock.unlock();
		}
	}

	/**
	 * Thread safe method that inserts contact in {@link #convsContactsPersistence}.
	 * 
	 * @param contact
	 */
	void insertContact(ContactInfo contact)
	{
		insertContact(contact, true);
	}

	/**
	 * Inserts contact in {@link #groupContactsPersistence}.
	 * <p>
	 * Make sure that it is not already in memory , we are setting reference count to one here.This implementation is thread safe.
	 * </p>
	 * 
	 * @param contact
	 * @param name
	 *            name if contact is not saved in address book otherwise null
	 */
	void insertContact(ContactInfo contact, boolean ifOneToOneConversation)
	{
		if (null != contact)
		{
			writeLock.lock();
			try
			{
				if (ifOneToOneConversation)
				{
					convsContactsPersistence.put(contact.getMsisdn(), contact);
				}
				else
				{
					PairModified<ContactInfo, Integer> contactPair = new PairModified<ContactInfo, Integer>(contact, 1);
					groupContactsPersistence.put(contact.getMsisdn(), contactPair);
				}
			}
			finally
			{
				writeLock.unlock();
			}
		}
	}

	/**
	 * Removes the contact from {@link #convsContactsPersistence}.
	 * 
	 * @param contact
	 */
	void removeContact(String msisdn)
	{
		removeContact(msisdn, true);
	}

	/**
	 * Removes the contact the {@link #convsContactsPersistence} OR decrements the reference count in {@link #groupContactsPersistence} , if reference count becomes 0 then removes
	 * from this map Depending on whether it is one to one conversation or group conversation . This method is thread safe.
	 * 
	 * @param contact
	 * @param ifOneToOneConversation
	 *            true if it is one to one conversation contact otherwise false
	 */
	void removeContact(String msisdn, boolean ifOneToOneConversation)
	{
		writeLock.lock();
		try
		{
			removeFromCache(msisdn, ifOneToOneConversation);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * This method checks if contact is in {@link #groupContactsPersistence} and it is also 1-1 conversation contact then moves it in {@link #convsContactsPersistence} and also
	 * vice versa
	 * 
	 * @param msisdn
	 * @param ifOneToOneConversation
	 */
	void move(String msisdn, boolean ifOneToOneConversation)
	{
		if (ifOneToOneConversation)
		{
			if (!convsContactsPersistence.containsKey(msisdn))
			{
				ContactInfo contact = null;
				PairModified<ContactInfo, Integer> contactPair = groupContactsPersistence.get(msisdn);
				if (null != contactPair)
				{
					contact = contactPair.getFirst();
				}
				insertContact(contact);
			}
		}
		else
		{
			if (!groupPersistence.containsKey(msisdn))
			{
				ContactInfo contact = convsContactsPersistence.get(msisdn);
				insertContact(contact, ifOneToOneConversation);
			}
		}

	}

	/**
	 * This method is not Thread safe , removes the contact for a particular msisdn from the cache - if it is one to one conversation removes from {@link #convsContactsPersistence}
	 * otherwise decrements the reference count by one , if reference count becomes zero then remove it completely from {@link #groupContactsPersistence}
	 * 
	 * @param msisdn
	 * @param ifOneToOneConversation
	 *            used to determine whether to remove from conversation contacts map or group map
	 */
	private void removeFromCache(String msisdn, boolean ifOneToOneConversation)
	{
		if (ifOneToOneConversation)
		{
			convsContactsPersistence.remove(msisdn);
		}
		else
		{
			PairModified<ContactInfo, Integer> contactPair = groupContactsPersistence.get(msisdn);
			if (null != contactPair)
			{
				contactPair.setSecond(contactPair.getSecond() - 1);
				if (contactPair.getSecond() == 0)
				{
					groupContactsPersistence.remove(msisdn);
				}
			}
		}
	}

	/**
	 * This method is thread safe and removes the mapping for particular grpId (if group is deleted) and their corresponding last msisnds are removed from the group contacts map
	 * 
	 * @param grpId
	 */
	void removeGroup(String grpId)
	{
		writeLock.lock();
		try
		{
			GroupDetails grpDetails = groupPersistence.get(grpId);
			if (null != grpDetails)
			{
				ConcurrentLinkedQueue<PairModified<String, String>> lastMsisdns = grpDetails.getLastMsisdns();
				for (PairModified<String, String> msPair : lastMsisdns)
				{
					removeFromCache(msPair.getFirst(), false);
				}
				groupPersistence.remove(grpId);
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Thread safe method that updates the contact in memory.
	 * 
	 * @param contact
	 */
	void updateContact(ContactInfo contact)
	{
		writeLock.lock();
		try
		{
			if (convsContactsPersistence.containsKey(contact.getMsisdn()))
				convsContactsPersistence.put(contact.getMsisdn(), contact);
			
			PairModified<ContactInfo, Integer> contactPair = groupContactsPersistence.get(contact.getMsisdn());
			if (null != contactPair)
			{
				contactPair.setFirst(contact);
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Should be called when group name is changed, name is stored in persistence cache {@link #groupPersistence}.
	 * 
	 * @param groupId
	 * @param name
	 */
	void setGroupName(String groupId, String name)
	{
		writeLock.lock();
		try
		{
			GroupDetails grpDetails = groupPersistence.get(groupId);
			if (null != grpDetails)
			{
				grpDetails.setCustomGroupName(name);
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	void updateGroupRecency(String groupId, long timestamp)
	{
		writeLock.lock();
		try
		{
			GroupDetails grpDetails = groupPersistence.get(groupId);
			if (null != grpDetails)
			{
				grpDetails.setTimestamp(timestamp);
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Returns name of group if msisdn is group ID else contact name - if contact is unsaved then this method returns null as for unsaved contact we need groupId to get name. This
	 * implementation is thread safe.
	 * 
	 * @param msisdn
	 * @return
	 */
	String getName(String msisdn)
	{
		/**
		 * Always try to take locks when and where required. Here we are separating out locking into different zones so that lock acquired should be for minimum time possible.
		 */
		if (OneToNConversationUtils.isOneToNConversation(msisdn))
		{
			GroupDetails grpDetails = null;
			readLock.lock();
			try
			{
				grpDetails = groupPersistence.get(msisdn);
				if (null == grpDetails)
					return null;
				String groupName = grpDetails.getGroupName();
				if (!(TextUtils.isEmpty(groupName) || groupName.equals(grpDetails.getGroupId())))
					return grpDetails.getGroupName();
			}
			finally
			{
				readLock.unlock();
			}
			/*
			 * most of cases will return before only-- below case is for groups which are created by ios and dont have name , so we create one using group participants and set in our
			 * cache
			 */

			writeLock.lock();
			try
			{
				List<PairModified<GroupParticipant, String>> grpParticipants = ContactManager.getInstance().getGroupParticipants(msisdn, false, false);
				String grpName = OneToNConversation.defaultConversationName(new ArrayList<PairModified<GroupParticipant, String>>(grpParticipants));
				grpDetails.setDefaultGroupName(grpName);
				return grpName;
			}
			finally
			{
				writeLock.unlock();
			}
		}

		/*
		 * This is for getting name of an normal contact
		 */
		readLock.lock();
		try
		{
			ContactInfo c = null;
			c = convsContactsPersistence.get(msisdn);
			if (null == c)
			{
				PairModified<ContactInfo, Integer> contactPair = groupContactsPersistence.get(msisdn);
				if (null != contactPair)
				{
					c = contactPair.getFirst();
				}
			}

			if (null == c)
				return null;

			return c.getName();
		}
		finally
		{
			readLock.unlock();
		}
	}

	void updateDefaultGroupName(String grpId)
	{
		readLock.lock();
		try
		{
			GroupDetails grpDetails = groupPersistence.get(grpId);
			if (null != grpDetails)
			{
				String grpName = grpDetails.getCustomGroupName();
				if (!TextUtils.isEmpty(grpName))
				{
					return;
				}
				List<PairModified<GroupParticipant, String>> grpParticipants = ContactManager.getInstance().getGroupParticipants(grpId, false, false);
				grpName = OneToNConversation.defaultConversationName(new ArrayList<PairModified<GroupParticipant, String>>(grpParticipants));
				grpDetails.setDefaultGroupName(grpName);
			}
		}
		finally
		{
			readLock.unlock();
		}
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
			String name = getName(msisdn); // can be a saved contact
			if (null == name)
			{
				GroupDetails grpDetails = groupPersistence.get(groupId);
				if (null != grpDetails)
					name = grpDetails.getName(msisdn);
			}
			return name;
		}
		finally
		{
			readLock.unlock();
		}
	}

	/**
	 * This function sets name in {@link #groupContactsPersistence} cache if the contact is not saved.
	 * <p>
	 * Suppose some unsaved contact is already in memory and it is added in a group then we set the name of contact using group table.
	 * </p>
	 * 
	 * @param msisdn
	 * @param name
	 */
	void setUnknownContactName(String grpId, String msisdn, String name)
	{
		writeLock.lock();
		try
		{
			GroupDetails grpDet = groupPersistence.get(grpId);
			grpDet.setName(msisdn, name);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * This will load all persistence contacts in memory. This method is called when HikeService is started and fills all the persistence cache at that time only. This method is
	 * not thread safe therefore should be called before mqtt thread is started.
	 */
	void loadMemory()
	{
		if(HikeConversationsDatabase.getInstance() == null)
			return;
		
		ConversationMsisdns allmsisdns = HikeConversationsDatabase.getInstance().getConversationMsisdns();
		// oneToOneMsisdns contains list of msisdns with whom one to one conversation currently exists
		// groupLastMsisdnsMap is map between group Id and list of last msisdns (msisdns of last message) in a group
		List<String> oneToOneMsisdns = allmsisdns.getOneToOneMsisdns();
		Map<String, Pair<List<String>, Long>> groupLastMsisdnsMap = allmsisdns.getGroupLastMsisdnsWithTimestamp();

		Map<String, GroupDetails> groupNamesMap = HikeConversationsDatabase.getInstance().getIdGroupDetailsMap();

		HashSet<String> grouplastMsisdns = new HashSet<String>();

		/**
		 * groupPersistence is now populated using groupNamesMap(for group name) and groupLastMsisdnsMap(msisdns in last message of a group) , these are needed so that if last
		 * message in a group changes then previous contact Info objects can be removed from persistence cache otherwise after some time all contacts will be loaded in cache
		 */
		for (Entry<String, GroupDetails> mapEntry : groupNamesMap.entrySet())
		{
			String grpId = mapEntry.getKey();
			GroupDetails groupDetails = mapEntry.getValue();
			Pair<List<String>, Long> lastMsisdnsAndTimestamp = groupLastMsisdnsMap.get(grpId);
			long timestamp = 0;
			ConcurrentLinkedQueue<PairModified<String, String>> lastMsisdnsConcurrentLinkedQueue = new ConcurrentLinkedQueue<PairModified<String, String>>();
			if (null != lastMsisdnsAndTimestamp)
			{
				List<String> lastMsisdns = lastMsisdnsAndTimestamp.first;
				timestamp = lastMsisdnsAndTimestamp.second;
				if (null != lastMsisdns)
				{
					grouplastMsisdns.addAll(lastMsisdns);
					for (String ms : lastMsisdns)
					{
						lastMsisdnsConcurrentLinkedQueue.add(new PairModified<String, String>(ms, null));
						// name for unsaved contact will be set later because at this point we don't know which msisdns are saved and which are not.
					}
				}
			}
			groupDetails.setTimestamp(timestamp);
			groupDetails.setLastMsisdns(lastMsisdnsConcurrentLinkedQueue);
			groupPersistence.put(grpId, groupDetails);
		}

		// msisdnsToGetContactInfo is combination of one to one msisdns and group last msisdns to get contact info from users db
		List<String> msisdnsToGetContactInfo = new ArrayList<String>();
		msisdnsToGetContactInfo.addAll(oneToOneMsisdns);
		msisdnsToGetContactInfo.addAll(grouplastMsisdns);

		Map<String, ContactInfo> contactsMap = new HashMap<String, ContactInfo>();
		if (msisdnsToGetContactInfo.size() > 0)
		{
			contactsMap = hDb.getContactInfoFromMsisdns(msisdnsToGetContactInfo, true);
		}

		// traverse through oneToOneMsisdns and get from contactsMap and put in convsContactsPersistence cache , remove from contactsMap if not present in grouplastMsisdns(because
		// some msisdns will be common between one to one msisdns and group last msisdns)
		for (String ms : oneToOneMsisdns)
		{
			ContactInfo contact = contactsMap.get(ms);
			convsContactsPersistence.put(ms, contact);
			if (!grouplastMsisdns.contains(ms))
			{
				contactsMap.remove(ms);
			}
		}

		// traverse through contactsMap which is left and put in groupContactsPersistence if contactInfo name is null we have to get names for that
		StringBuilder unknownGroupMsisdns = new StringBuilder("(");
		for (Entry<String, ContactInfo> mapEntry : contactsMap.entrySet())
		{
			String msisdn = mapEntry.getKey();
			ContactInfo contact = mapEntry.getValue();
			PairModified<ContactInfo, Integer> contactPair = new PairModified<ContactInfo, Integer>(contact, 1);
			groupContactsPersistence.put(msisdn, contactPair);

			if (null == contact.getName())
			{
				unknownGroupMsisdns.append(DatabaseUtils.sqlEscapeString(msisdn) + ",");
			}
		}

		// get names of unknown group contacts from group members table
		Map<String, Map<String, String>> unknownGroupMsisdnsName = new HashMap<String, Map<String, String>>();

		int idx = unknownGroupMsisdns.lastIndexOf(",");
		if (idx >= 0)
		{
			unknownGroupMsisdns.replace(idx, unknownGroupMsisdns.length(), ")");
			unknownGroupMsisdnsName = HikeConversationsDatabase.getInstance().getGroupMembersName(unknownGroupMsisdns.toString());
		}

		// set names for unknown group contacts in groupPersistence cache
		for (Entry<String, Map<String, String>> mapEntry : unknownGroupMsisdnsName.entrySet())
		{
			String groupId = mapEntry.getKey();
			Map<String, String> map = mapEntry.getValue();
			GroupDetails grpDetails = groupPersistence.get(groupId);
			if (null != grpDetails)
			{
				ConcurrentLinkedQueue<PairModified<String, String>> list = grpDetails.getLastMsisdns();
				for (PairModified<String, String> msPair : list)
				{
					String name = map.get(msPair.getFirst());
					msPair.setSecond(name);
				}
			}
		}

		blockedMsisdns = hDb.getBlockedMsisdnSet();
	}

	/**
	 * This method should be called when both persistence and transient cache have been initialized. It traverses through {@link #groupPersistence} map and if group name is empty
	 * or equal to group id then it creates a default group name from group participants and sets it in map.
	 */
	void updateGroupNames()
	{
		writeLock.lock();
		try
		{
			for (Entry<String, GroupDetails> mapEntry : groupPersistence.entrySet())
			{
				String msisdn = mapEntry.getKey();
				GroupDetails grpDetails = mapEntry.getValue();
				if (null != grpDetails)
				{
					String groupName = grpDetails.getGroupName();
					if (TextUtils.isEmpty(groupName) || groupName.equals(grpDetails.getGroupId()))
					{
						List<PairModified<GroupParticipant, String>> grpParticipants = ContactManager.getInstance().getGroupParticipants(msisdn, false, false);
						String grpName = OneToNConversation.defaultConversationName(new ArrayList<PairModified<GroupParticipant, String>>(grpParticipants));
						grpDetails.setDefaultGroupName(grpName);
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
	 * This method makes a database query to load the contact for a msisdn in persistence convs map and returns the same
	 * 
	 * @param msisdn
	 * @param ifNotFoundReturnNull
	 *            if true returns null if contact is not saved in address book
	 */
	ContactInfo putInCache(String msisdn, boolean ifNotFoundReturnNull)
	{
		return putInCache(msisdn, ifNotFoundReturnNull, true);
	}

	/**
	 * This method makes a database query to load the contact info for a msisdn in persistence memory and returns the same
	 * 
	 * @param msisdn
	 * @param ifNotFoundReturnNull
	 *            if true returns null if contact is not saved
	 * @param ifOneToOneConversation
	 *            used to determine whether to put in {@link #convsContactsPersistence} or {@link #groupContactsPersistence}.
	 * @return Returns contact info object
	 */
	ContactInfo putInCache(String msisdn, boolean ifNotFoundReturnNull, boolean ifOneToOneConversation)
	{
		ContactInfo contact = hDb.getContactInfoFromMSISDN(msisdn, ifNotFoundReturnNull);

		insertContact(contact, ifOneToOneConversation);

		return contact;
	}

	/**
	 * This method makes a database query to load the contactInfo of msisdns in {@link #convsContactsPersistence} and returns the list of same
	 * 
	 * @param msisdns
	 */
	List<ContactInfo> putInCache(List<String> msisdns)
	{
		return putInCache(msisdns, true);
	}

	/**
	 * This method makes a database query to load the contactInfo of msisdns in persistence memory and returns the list of same
	 * 
	 * @param msisdns
	 * @param ifOneToOneConversation
	 *            used to determine whether to put in {@link #convsContactsPersistence} or {@link #groupContactsPersistence}.
	 * @return
	 */
	List<ContactInfo> putInCache(List<String> msisdns, boolean ifOneToOneConversation)
	{
		if (msisdns.size() > 0)
		{
			Map<String, ContactInfo> map = hDb.getContactInfoFromMsisdns(msisdns, true);
			for (Entry<String, ContactInfo> mapEntry : map.entrySet())
			{
				ContactInfo contact = mapEntry.getValue();
				insertContact(contact, ifOneToOneConversation);
			}
			return new ArrayList<ContactInfo>(map.values());
		}
		return null;
	}

	/**
	 * This method is used for removing msisdns from the group persistence cache when last message in group is changed and their reference count is decremented in group contacts
	 * map by one which is done by {@link #removeFromCache} method
	 * 
	 * @param groupId
	 * @param currentGroupMsisdns
	 */
	List<String> removeOlderLastGroupMsisdn(String groupId, List<String> currentGroupMsisdns)
	{
		if (groupPersistence != null)
		{
			GroupDetails nameAndLastMsisdns;
			readLock.lock();
			try
			{
				nameAndLastMsisdns = groupPersistence.get(groupId);
			}
			finally
			{
				readLock.unlock();
			}

			List<String> msisdns = new ArrayList<String>();
			if (null != nameAndLastMsisdns)
			{
				ConcurrentLinkedQueue<PairModified<String, String>> grpMsisdns = nameAndLastMsisdns.getLastMsisdns();
				if (null != grpMsisdns)
				{
					boolean flag;
					writeLock.lock();
					try
					{
						for (PairModified<String, String> msisdnPair : grpMsisdns)
						{
							flag = false;
							for (String ms : currentGroupMsisdns)
							{
								if (ms.equals(msisdnPair.getFirst()))
								{
									flag = true;
									break;
								}
							}
							if (!flag)
							{
								removeFromCache(msisdnPair.getFirst(), false);
							}
							else
							{
								// if contact is in group map then increase ref count by 1
								PairModified<ContactInfo, Integer> contactPair = groupContactsPersistence.get(msisdnPair.getFirst());
								if (null != contactPair)
								{
									contactPair.setSecond(contactPair.getSecond() + 1);
								}
								else
								{
									// if contact is in convsMap then insert in groupMap with ref count 1
									ContactInfo contact = convsContactsPersistence.get(msisdnPair.getFirst());
									if (null == contact)
									{
										// get contact from db
										// add to a list
										msisdns.add(msisdnPair.getFirst());
									}
									else
									{
										insertContact(contact, false);
									}
								}
							}
						}
					}
					finally
					{
						writeLock.unlock();
					}
				}

				// lock is not needed here because grpMsisdns is concurrentLinkedQueue
				grpMsisdns.clear();

				Map<String, String> groupParticipantsNameMap = new HashMap<String, String>();
				if (currentGroupMsisdns.size() > 0)
				{
					groupParticipantsNameMap = HikeConversationsDatabase.getInstance().getGroupParticipantNameMap(groupId, currentGroupMsisdns);
				}

				for (String ms : currentGroupMsisdns)
				{
					PairModified<String, String> msisdnNamePair = new PairModified<String, String>(ms, groupParticipantsNameMap.get(ms));
					grpMsisdns.add(msisdnNamePair);
				}
			}
			// returning msisdns which are not found in persistence cache because before making new objects we should also check transient cache
			return msisdns;
		}
		return null;
	}

	/**
	 * clears the persistence memory
	 */
	void clearMemory()
	{
		writeLock.lock();
		try
		{
			if (null != convsContactsPersistence)
			{
				convsContactsPersistence.clear();
			}

			if (null != groupContactsPersistence)
			{
				groupContactsPersistence.clear();
			}

			if (null != groupPersistence)
			{
				groupPersistence.clear();
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * If group conversation, check in groupPersistence else check in convPersistance
	 * 
	 * @param id
	 * @return
	 */
	boolean convExist(String id)
	{
		readLock.lock();
		try
		{
			if (OneToNConversationUtils.isOneToNConversation(id))
				return groupPersistence.containsKey(id);
			else
				return convsContactsPersistence.containsKey(id);
		}
		finally
		{
			readLock.unlock();
		}
	}

	/**
	 * This method returns true or false depending on whether group exists or not
	 * 
	 * @param groupId
	 * @return
	 */
	public boolean isGroupExists(String groupId)
	{
		readLock.lock();
		try
		{
			return groupPersistence.containsKey(groupId);
		}
		finally
		{
			readLock.unlock();
		}
	}

	boolean isGroupAlive(String groupId)
	{
		readLock.lock();
		try
		{
			GroupDetails grpDetails = groupPersistence.get(groupId);
			if (null == grpDetails)
			{
				return false;
			}
			return grpDetails.isGroupAlive();
		}
		finally
		{
			readLock.unlock();
		}
	}

	void setGroupAlive(String groupId, boolean alive)
	{
		writeLock.lock();
		try
		{
			GroupDetails grpDetails = groupPersistence.get(groupId);
			if (null != grpDetails)
			{
				grpDetails.setIsGroupAlive(alive);
			}
		}
		finally
		{
			writeLock.unlock();
		}

	}
	
	boolean isGroupMute(String groupId)
	{
		readLock.lock();
		try
		{
			GroupDetails grpDetails = groupPersistence.get(groupId);
			if (null == grpDetails)
			{
				return false;
			}
			return grpDetails.isGroupMute();
		}
		finally
		{
			readLock.unlock();
		}
	}

	void setGroupMute(String groupId, boolean mute)
	{
		writeLock.lock();
		try
		{
			GroupDetails grpDetails = groupPersistence.get(groupId);
			if (null != grpDetails)
			{
				grpDetails.setGroupMute(mute);
			}
		}
		finally
		{
			writeLock.unlock();
		}

	}

	public void insertGroup(String grpId, GroupDetails grpDetails)
	{
		writeLock.lock();
		try
		{
			String groupName = grpDetails.getGroupName();
			if (TextUtils.isEmpty(groupName) || groupName.equals(grpId))
			{
				List<PairModified<GroupParticipant, String>> grpParticipants = ContactManager.getInstance().getGroupParticipants(grpId, false, false);
				groupName = OneToNConversation.defaultConversationName(new ArrayList<PairModified<GroupParticipant, String>>(grpParticipants));
				grpDetails.setDefaultGroupName(groupName);
			}
			groupPersistence.put(grpId, grpDetails);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Inserts the group with group id and groupName in the {@link #groupPersistence}
	 * 
	 * @param grpId
	 * @param groupName
	 */
	public void insertGroup(String grpId, String groupName, boolean alive)
	{
		writeLock.lock();
		try
		{
			ConcurrentLinkedQueue<PairModified<String, String>> clq = new ConcurrentLinkedQueue<PairModified<String, String>>();
			String defaultGroupName = null;
			if (TextUtils.isEmpty(groupName) || groupName.equals(grpId))
			{
				List<PairModified<GroupParticipant, String>> grpParticipants = ContactManager.getInstance().getGroupParticipants(grpId, false, false);
				defaultGroupName = OneToNConversation.defaultConversationName(new ArrayList<PairModified<GroupParticipant, String>>(grpParticipants));
			}
			GroupDetails grpDetails = new GroupDetails(grpId, groupName, defaultGroupName, alive, clq);
			groupPersistence.put(grpId, grpDetails);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * This method is used when <code>number</code> can be msisdn or phone number . First we check in cache assuming number is msisdn , if not found then traverse through all the
	 * contacts in persistence cache to find if any contactInfo object contains given number as phoneNumber. This method returns null if not found in memory and not makes a DB
	 * call.
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
		readLock.lock();
		try
		{
			for (Entry<String, ContactInfo> mapEntry : convsContactsPersistence.entrySet())
			{
				ContactInfo con = mapEntry.getValue();
				if (null != con && con.getPhoneNum().equals(number))
				{
					contact = con;
					break;
				}
			}

			if (null == contact)
			{
				for (Entry<String, PairModified<ContactInfo, Integer>> mapEntry : groupContactsPersistence.entrySet())
				{
					PairModified<ContactInfo, Integer> contactPair = mapEntry.getValue();
					if (null != contactPair && contactPair.getFirst().getPhoneNum().equals(number))
					{
						contact = contactPair.getFirst();
						break;
					}
				}
			}

			return contact;
		}
		finally
		{
			readLock.unlock();
		}
	}

	/**
	 * Traverse through all the contacts in persistence cache to find if any contactInfo object contains given number as phoneNumber. This method returns null if not found in
	 * memory and not makes a DB call.
	 * 
	 * @param number
	 * @return
	 */
	public ContactInfo getContactInfoFromPhoneNo(String number)
	{
		ContactInfo contact = null;
		readLock.lock();
		try
		{
			for (Entry<String, ContactInfo> mapEntry : convsContactsPersistence.entrySet())
			{
				ContactInfo con = mapEntry.getValue();
				if (null != con && con.getPhoneNum().equals(number))
				{
					contact = con;
					break;
				}
			}

			if (null == contact)
			{
				for (Entry<String, PairModified<ContactInfo, Integer>> mapEntry : groupContactsPersistence.entrySet())
				{
					PairModified<ContactInfo, Integer> contactPair = mapEntry.getValue();
					if (null != contactPair && contactPair.getFirst().getPhoneNum().equals(number))
					{
						contact = contactPair.getFirst();
						break;
					}
				}
			}

			return contact;
		}
		finally
		{
			readLock.unlock();
		}
	}

	/**
	 * Returns one to one contacts from {@link #convsContactsPersistence}.
	 *
	 * @param
	 * @return List of convContacts
	 */
	List<ContactInfo> getConversationOneToOneContacts()
	{
		List<ContactInfo> convContacts = new ArrayList<ContactInfo>();
		readLock.lock();
		try
		{
			for(Map.Entry<String, ContactInfo> entry : convsContactsPersistence.entrySet())
			{
				convContacts.add(entry.getValue());
			}
			return convContacts;
		}
		finally
		{
			readLock.unlock();
		}
	}

	List<GroupDetails> getGroupDetailsList()
	{
		// traverse through groupPersistence
		readLock.lock();
		try
		{
			List<GroupDetails> groupsList = new ArrayList<GroupDetails>();
			for (Entry<String, GroupDetails> mapEntry : groupPersistence.entrySet())
			{
				GroupDetails grpDetails = mapEntry.getValue();
				groupsList.add(grpDetails);
			}
			return groupsList;
		}
		finally
		{
			readLock.unlock();
		}
	}

	public GroupDetails getGroupDetails(String msisdn)
	{
		readLock.lock();
		try
		{
			return groupPersistence.get(msisdn);
		}
		finally
		{
			readLock.unlock();
		}
	}

	/**
	 * Blocks a msisdn , adds it to {@link #blockedMsisdns}
	 * 
	 * @param msisdn
	 */
	public void block(String msisdn)
	{
		writeLock.lock();
		try
		{
			blockedMsisdns.add(msisdn);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Blocks a list of msisdns , adds them to {@link #blockedMsisdns}
	 * 
	 * @param msisdn
	 */
	public void block(List<String> msisdn)
	{
		writeLock.lock();
		try
		{
			blockedMsisdns.addAll(msisdn);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Unblocks a msisdn , removes it from {@link #blockedMsisdns}
	 * 
	 * @param msisdn
	 */
	public void unblock(String msisdn)
	{
		writeLock.lock();
		try
		{
			blockedMsisdns.remove(msisdn);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Returns true if the msisdn is blocked otherwise false
	 * 
	 * @param msisdn
	 * @return
	 */
	public boolean isBlocked(String msisdn)
	{
		readLock.lock();
		try
		{
			return blockedMsisdns.contains(msisdn);
		}
		finally
		{
			readLock.unlock();
		}
	}

	/**
	 * Returns the set of all blocked msisdns
	 * 
	 * @return
	 */
	public Set<String> getBlockedMsisdnSet()
	{
		readLock.lock();
		try
		{
			// creating a new hash set from current one to prevent outside world not to change cache set
			return new HashSet<String>(blockedMsisdns);
		}
		finally
		{
			readLock.unlock();
		}
	}
	
	/**
	 * Clears the memory {@see #clearMemory()} and make all references null
	 */
	void shutdown()
	{
		clearMemory();
		hDb = null;
		convsContactsPersistence = null;
		groupContactsPersistence = null;
		groupPersistence = null;
	}
}
