/**
 * 
 */
package com.bsb.hike.modules.contactmgr;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.chatHead.CallerContentModel;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.db.DbException;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.FtueContactsData;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.Mute;
import com.bsb.hike.modules.iface.ITransientCache;
import com.bsb.hike.tasks.UpdateAddressBookTask;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.PhoneUtils;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Gautam & Sidharth
 * 
 */
public class ContactManager implements ITransientCache, HikePubSub.Listener
{
	// This should always be present so making it loading on class loading itself
	private volatile static ContactManager _instance;

	private PersistenceCache persistenceCache;

	private TransientCache transientCache;

	private HikeUserDatabase hDb;

	private Context context;

	private static String[] pubSubListeners = { HikePubSub.APP_BACKGROUNDED };
	
	private String selfMsisdn;

	public static final byte SYNC_CONTACTS_CHANGED = 0;

	public static final byte SYNC_CONTACTS_NO_CONTACTS_FOUND_IN_ANDROID_ADDRESSBOOK = 1;

	public static final byte SYNC_CONTACTS_DB_IN_SYNC = 2;

	public static final byte SYNC_CONTACTS_ERROR = 3;

	private static final String ADDRESSBOOK_AREA = "ab";

	private static final String AB_SYNC = "sync";

	private static final String HIKE_CONTACT_LIST = "hikeList";

	private static final String DEVICE_CONTACT_LIST = "deviceList";

	private static final String NEW_CONTACT_LIST = "newList";

	private static final String REMOVE_CONTACT_LIST = "remList";

	private static final String SERVER_RESPONSE_CONTACT_LIST = "sResponseList";

	private ContactManager()
	{
		context = HikeMessengerApp.getInstance().getApplicationContext();
		setSelfMsisdn(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, ""));
		hDb = HikeUserDatabase.getInstance();
		persistenceCache = new PersistenceCache(hDb);
		transientCache = new TransientCache(hDb);
	}

	public static ContactManager getInstance()
	{
		if (_instance == null)
		{
			synchronized (ContactManager.class)
			{
				if (_instance == null)
				{
					_instance = new ContactManager();
					init();
				}
			}
		}
		return _instance;
	}

	private static void init()
	{
		if (_instance == null)
		{
			throw new IllegalStateException("Contact Manager getInstance() should be called that will create a new instance and initialize");
		}

		HikeMessengerApp.getPubSub().addListeners(_instance, pubSubListeners);

		// Called to set name for group whose group name is empty (group created by ios) , we cannot do this inside persistence cache load memory because at taht point transient
		// and persistence cache have not been initialized completely
		_instance.persistenceCache.updateGroupNames();
	}

	public void reinitializeUserDB()
	{
		hDb.reinitializeDB();
	}

	public void clearUserDbTable(String tableName)
	{
		hDb.clearTable(tableName);
	}
	
	public SQLiteDatabase getWritableDatabase()
	{
		return hDb.getWriteDatabase();
	}

	public SQLiteDatabase getReadableDatabase()
	{
		return hDb.getReadableDatabase();
	}

	/**
	 * This method should unload the transient memory at app launch event
	 */
	@Override
	public void unload()
	{
		Logger.i(getClass().getSimpleName(), "clearing transient cache ");
		transientCache.clearMemory();
	}

	/**
	 * This method clears the persistence memory
	 */
	public void clearCache()
	{
		persistenceCache.clearMemory();
		transientCache.clearMemory();
	}

	/**
	 * This method should be used when a conversation gets deleted or last sender in group is changed
	 * 
	 * @param msisdn
	 * @param ifOneToOneConversation
	 */
	public void removeContact(String msisdn, boolean ifOneToOneConversation)
	{
		persistenceCache.removeContact(msisdn, ifOneToOneConversation);
	}

	/**
	 * This is used to remove the list of msisdns from either 1-n or 1-1 conversation and should be called when multiple group or one to one conversations are deleted.
	 * 
	 * @param msisdns
	 */
	public void removeContacts(String msisdn)
	{
		if (OneToNConversationUtils.isOneToNConversation(msisdn))
		{
			persistenceCache.removeGroup(msisdn);
			transientCache.removeGroup(msisdn);
		}
		else
		{
			persistenceCache.removeContact(msisdn);
		}
	}

	/**
	 * This method is used when contacts are deleted from the address book and we set their name to null in the cache
	 * 
	 * @param contacts
	 */
	public void deleteContacts(List<ContactInfo> contacts)
	{
		for (ContactInfo contact : contacts)
		{
			contact.setName(null);
			contact.setId(contact.getMsisdn());
			updateContacts(contact);
		}
	}

	/**
	 * This method updates the contact info object in memory and if not found in memory then loads it in {@link TransientCache}.
	 * 
	 * @param contact
	 */
	public void updateContacts(ContactInfo contact)
	{
		persistenceCache.updateContact(contact);
		transientCache.updateContact(contact);
	}

	/**
	 * This updates a list of contactInfo objects in memory and if not found in memory then loads it in {@link TransientCache}
	 * 
	 * @param updatescontacts
	 */
	public void updateContacts(List<ContactInfo> updatescontacts)
	{
		for (ContactInfo contact : updatescontacts)
		{
			updateContacts(contact);
		}
	}

	/**
	 * This method is called while syncing contacts if some changes have been made to contacts in address book
	 * 
	 * @param contacts
	 */
	private void syncContacts(List<ContactInfo> contacts)
	{
		if (null != contacts)
		{
			updateContacts(contacts);
			transientCache.allContactsLoaded = false;
		}
	}

	public String getName(String msisdn)
	{
		return getName(msisdn, false);
	}

	/**
	 * This method returns the name of a particular <code>msisdn</code>.For name of a group participant {@link #getName(String, String)} should be used because it also handles the
	 * unsaved contact name in a group. This method does not make a database query if contact is not in memory but returns the msisdn.
	 * 
	 * @param msisdn
	 * @return Returns the name of contact or group depending on msisdn whether it is phone number of contact or group id
	 */
	public String getName(String msisdn, boolean returnNullIfNotFound)
	{
		String name = persistenceCache.getName(msisdn);
		if (null == name)
		{
			name = transientCache.getName(msisdn);
		}
		if (null == name)
		{
			// fetch from db if not found
			if (OneToNConversationUtils.isOneToNConversation(msisdn))
			{
				GroupDetails grpDetails = getGroupDetails(msisdn);
				if (grpDetails != null)
				{
					name = grpDetails.getGroupName();
				}
			}
			else
			{
				ContactInfo contact = getContact(msisdn, true, false);
				if (contact != null)
				{
					name = contact.getName();
				}
			}
		}
		if (null == name && !returnNullIfNotFound)
			return msisdn;
		return name;
	}

	/**
	 * This method simple returns the name from contact Info object if contact is saved else use the {@link PersistenceCache} and {@link TransientCache} to get the name of an
	 * unsaved contact. This method does not make a database query if contact is not in memory but returns msisdn.
	 * 
	 * @param groupId
	 *            group id is needed as unsaved contact name can be different in different groups. Name for unsaved contact is same as in group creator address book.
	 * @param msisdn
	 * @return
	 */
	public String getName(String groupId, String msisdn)
	{
		String name = persistenceCache.getName(groupId, msisdn);
		if (null == name)
		{
			name = transientCache.getName(groupId, msisdn);
		}
		if (null == name)
			return msisdn;
		return name;
	}

	/**
	 * This method sets the name of a group participant contact in both {@link PersistenceCache} and {@link TransientCache}. In case of saved contact it is address book name and in
	 * case of unsaved it is name obtained from group members table.
	 * 
	 * @param grpId
	 * @param msisdn
	 * @param name
	 */
	public void setGroupParticipantContactName(String grpId, String msisdn, String name)
	{
		persistenceCache.setUnknownContactName(grpId, msisdn, name);
		transientCache.setUnknownContactName(grpId, msisdn, name);
	}

	/**
	 * This function will return {@link ContactInfo} object or null for a particular msisdn
	 * <p>
	 * Search in {@link PersistenceCache} first, if not found, search in {@link TransientCache}
	 * </p>
	 * 
	 * @param msisdn
	 * @return
	 */
	public ContactInfo getContact(String msisdn)
	{
		ContactInfo contact = persistenceCache.getContact(msisdn);
		if (null == contact)
		{
			contact = transientCache.getContact(msisdn);
		}
		return contact;
	}

	/**
	 * Returns the {@link ContactInfo} for a particular msisdn. If not found in memory makes a DB call. This method should be called when {@link ContactInfo} object is needed even
	 * if it is an unsaved contact.
	 * <p>
	 * Inserts the object in {@link TransientCache} if <code>loadInTransient</code> is set to true otherwise in {@link PersistenceCache}.
	 * </p>
	 * 
	 * @param msisdn
	 * @param loadInTransient
	 * @return
	 */
	public ContactInfo getContact(String msisdn, boolean loadInTransient, boolean ifOneToOneConversation)
	{
		return getContact(msisdn, loadInTransient, ifOneToOneConversation, false);
	}

	/**
	 * Returns the {@link ContactInfo} for a particular msisdn. If not found in memory makes a DB call
	 * <p>
	 * Inserts the object in transient memory if loadInTransient is set to true otherwise in persistence memory
	 * </p>
	 * 
	 * @param msisdn
	 * @param loadInTransient
	 * @param ifNotFoundReturnNull
	 *            if set to true and contact is not saved in address book then returns null
	 * @return
	 */
	public ContactInfo getContact(String msisdn, boolean loadInTransient, boolean ifOneToOneConversation, boolean ifNotFoundReturnNull)
	{
		ContactInfo contact = getContact(msisdn);
		if (null == contact)
		{
			if (loadInTransient)
			{
				contact = transientCache.putInCache(msisdn, ifNotFoundReturnNull);
			}
			else
			{
				contact = persistenceCache.putInCache(msisdn, ifNotFoundReturnNull, ifOneToOneConversation);
			}
		}
		else
		{
			if (ifNotFoundReturnNull && contact.getName() == null)
			{
				return null;
			}

			if (!loadInTransient)
			{
				// move to persistence if found in transient using getcontact used in first line of method (if loadintransient is false)
				ContactInfo con = persistenceCache.getContact(msisdn);
				if (null == con)
				{
					con = transientCache.getContact(msisdn);
					persistenceCache.insertContact(con, ifOneToOneConversation);
				}
				else
				{
					/*
					 * Now contact is in persistence cache but it can be either in 1-1 contacts map or group contacts map. Below method will move the contact from group map to 1-1
					 * map if contact is of 1-1 conversation and vice versa
					 */
					persistenceCache.move(msisdn, ifOneToOneConversation);
				}
			}
		}
		return contact;
	}

	/**
	 * Returns List of {@link ContactInfo} objects for some msisdns. Inserts the contact in {@link TransientCache} if <code>loadInTransient</code> is set to true otherwise in
	 * {@link PersistenceCache}. ANd this method also handles the case where we want to load in persistence and contact is already in transient cache then we put this contact in
	 * persistence too.
	 * 
	 * @param msisdns
	 * @param loadInTransient
	 * @return
	 */
	public List<ContactInfo> getContact(List<String> msisdns, boolean loadInTransient, boolean ifOneToOneConversation)
	{
		List<ContactInfo> contacts = new ArrayList<ContactInfo>();

		List<String> msisdnsDB = new ArrayList<String>();

		for (String msisdn : msisdns)
		{
			ContactInfo c = getContact(msisdn);
			if (null != c)
			{
				contacts.add(c);
			}
			else
			{
				msisdnsDB.add(msisdn);
			}
		}

		if (msisdnsDB.size() > 0)
		{
			List<ContactInfo> contactsDB;
			if (loadInTransient)
			{
				contactsDB = transientCache.putInCache(msisdnsDB);
			}
			else
			{
				contactsDB = persistenceCache.putInCache(msisdnsDB, ifOneToOneConversation);
			}

			if (null != contactsDB)
			{
				contacts.addAll(contactsDB);
			}
		}

		if (!loadInTransient)
		{
			// move to persistence if found in transient using getcontact used in first line of method (if loadintransient is false)
			for (String msisdn : msisdns)
			{
				ContactInfo con = persistenceCache.getContact(msisdn);
				if (null == con)
				{
					con = transientCache.getContact(msisdn);
					persistenceCache.insertContact(con, ifOneToOneConversation);
				}
			}
		}

		return contacts;
	}

	/**
	 * Returns the list of all the contacts
	 * 
	 * @return
	 */
	public List<ContactInfo> getAllContacts()
	{
		return transientCache.getAllContacts();
	}

	
	public List<ContactInfo> getAllContacts(boolean ignoreUnknownContacts)
	{
		return transientCache.getAllContacts(ignoreUnknownContacts);
	}
	/**
	 * This method should be called when last message in a group is changed, we remove the previous contact from the {@link PersistenceCache} and inserts the new contacts in memory
	 * 
	 * @param map
	 */
	public void removeOlderLastGroupMsisdns(Map<String, Pair<List<String>, Long>> map)
	{
		List<String> msisdns = new ArrayList<String>();
		List<String> msisdnsDB = new ArrayList<String>();

		for (Entry<String, Pair<List<String>, Long>> mapEntry : map.entrySet())
		{
			String groupId = mapEntry.getKey();
			Pair<List<String>, Long> lastMsisdnspair = mapEntry.getValue();
			if (null != lastMsisdnspair)
			{
				List<String> lastMsisdns = lastMsisdnspair.first;
				updateGroupRecency(groupId, lastMsisdnspair.second);
				msisdns.addAll(persistenceCache.removeOlderLastGroupMsisdn(groupId, lastMsisdns));
			}
		}

		for (String ms : msisdns)
		{
			ContactInfo contact = transientCache.getContact(ms);
			if (null == contact)
			{
				msisdnsDB.add(ms);
			}
			else
			{
				persistenceCache.insertContact(contact, false);
			}
		}
		persistenceCache.putInCache(msisdnsDB, false);
		for (Entry<String, Pair<List<String>, Long>> mapEntry : map.entrySet())
		{
			String groupId = mapEntry.getKey();
			Pair<List<String>, Long> lastPair = mapEntry.getValue();
			if (null != lastPair)
			{
				List<String> last = lastPair.first;
				for (String ms : last)
				{
					ContactInfo contact = getContact(ms, false, false);
					if (null != contact && null != contact.getName())
					{
						setGroupParticipantContactName(groupId, ms, contact.getName());
					}
				}
			}
		}
	}

	/**
	 * Returns true if group is alive otherwise false
	 * 
	 * @param groupId
	 * @return
	 */
	public boolean isGroupAlive(String groupId)
	{
		return persistenceCache.isGroupAlive(groupId);
	}

	/**
	 * Sets the group alive status in {@link #persistenceCache}
	 * 
	 * @param groupId
	 * @param alive
	 */
	public void setGroupAlive(String groupId, boolean alive)
	{
		persistenceCache.setGroupAlive(groupId, alive);
	}

	/**
	 * Returns true if group is mute otherwise false
	 * 
	 * @param groupId
	 * @return
	 */
	public boolean isGroupMute(String groupId)
	{
		return persistenceCache.isGroupMute(groupId);
	}

	/**
	 * Sets the group mute status in {@link #persistenceCache}
	 * 
	 * @param groupId
	 * @param mute
	 */
	public void setGroupMute(String groupId, boolean mute)
	{
		persistenceCache.setGroupMute(groupId, mute);
	}

	/**
	 * Sets the chat mute status in {@link #persistenceCache}
	 *
	 * @param msisdn
	 * @param mute
     */
	public void setChatMute(String msisdn, Mute mute)
	{
		persistenceCache.setChatMute(msisdn, mute);
	}
	
	/**
	 * Gets the Name, alive and Mute status of Group
	 * 
	 * @param msisdn
	 * @return GroupDetails
	 */
	public GroupDetails getGroupDetails(String msisdn)
	{
		return persistenceCache.getGroupDetails(msisdn);
	}

	/**
	 * Fetches the Mute data for a conversation
	 *
	 * @param msisdn
	 * @return
     */
	public Mute getMute(String msisdn)
	{
		return persistenceCache.getMute(msisdn);
	}

	/**
	 *
	 * @param msisdn
	 * @return boolean stating whether to show notifications for a given chat
     */
	public boolean shouldShowNotifForMutedConversation(String msisdn)
	{
		Mute mute = persistenceCache.getMute(msisdn);
		if (mute != null && mute.isMute())
		{
			return mute.shouldShowNotifInMute();
		}
		return true;
	}

	/**
	 *
	 * @param msisdn
	 * @return boolean stating whether the given chat is muted or not
     */
	public boolean isChatMuted(String msisdn)
	{
		Mute mute = persistenceCache.getMute(msisdn);
		if (mute != null)
		{
			return mute.isMute();
		}
		return false;
	}
	
	/**
	 * This method returns a list {@link ContactInfo} objects of a particular favorite type and if parameter <code>onHike</code> is one then these are hike contacts otherwise non
	 * hike contacts. This list should not contain contact whose msisdn is same as parameter <code>myMsisdn</code>. Unsaved contacts are also included in these.
	 * 
	 * @param favoriteType
	 * @param onHike
	 * @param myMsisdn
	 * @return
	 */
	public List<ContactInfo> getContactsOfFavoriteType(FavoriteType favoriteType, int onHike, String myMsisdn)
	{
		return getContactsOfFavoriteType(favoriteType, onHike, myMsisdn, false, false);
	}

	/**
	 * This method returns a list {@link ContactInfo} objects of a particular favorite type and if parameter <code>onHike</code> is one then these are hike contacts otherwise non
	 * hike contacts. This list should not contain contact whose msisdn is same as parameter <code>myMsisdn</code>.
	 * <p>
	 * If parameter <code>nativeSMSOn</code> is false then contacts which are either hike contacts or contacts whose msisdns start with '+91' are included in the list
	 * </p>
	 * Returned list may also contain unsaved contacts.
	 * 
	 * @param favoriteType
	 * @param onHike
	 * @param myMsisdn
	 * @param nativeSMSOn
	 * @return
	 */
	public List<ContactInfo> getContactsOfFavoriteType(FavoriteType favoriteType, int onHike, String myMsisdn, boolean nativeSMSOn)
	{
		return getContactsOfFavoriteType(favoriteType, onHike, myMsisdn, nativeSMSOn, false);
	}

	/**
	 * This method returns a list {@link ContactInfo} objects of a particular favorite type and if parameter <code>onHike</code> is one then these are hike contacts otherwise non
	 * hike contacts. This list should not contain contact whose msisdn is same as parameter <code>myMsisdn</code>.
	 * <p>
	 * If parameter <code>nativeSMSOn</code> is false then contacts which are either hike contacts or contacts whose msisdns start with '+91' are included in the list
	 * </p>
	 * If parameter <code>ignoreUnknownContacts</code> is set to true then returned list does not contains unsaved contacts
	 * 
	 * @param favoriteType
	 * @param onHike
	 * @param myMsisdn
	 * @param nativeSMSOn
	 * @param ignoreUnknownContacts
	 * @return
	 */
	public List<ContactInfo> getContactsOfFavoriteType(FavoriteType favoriteType, int onHike, String myMsisdn, boolean nativeSMSOn, boolean ignoreUnknownContacts)
	{
		if (favoriteType == FavoriteType.NOT_FRIEND)
		{
			List<ContactInfo> contacts = transientCache.getNOTFRIENDScontacts(onHike, myMsisdn, nativeSMSOn, ignoreUnknownContacts);

			if (!transientCache.isAllContactsLoaded())
			{
				// if all contacts are not loaded then we get contacts from DB (using method getNOTFRIENDScontacts) , some contacts can be already in memory either in persistence
				// cache or transient cache . To avoid duplicates we first check in both cache if not found then only insert in transient cache.
				for (ContactInfo con : contacts)
				{
					if (null == getContact(con.getMsisdn()))
					{
						transientCache.insertContact(con);
					}
				}
			}
			return contacts;
		}
		else
		{
			return getContactsOfFavoriteType(new FavoriteType[] { favoriteType }, onHike, myMsisdn, nativeSMSOn, ignoreUnknownContacts);
		}
	}

	/**
	 * This method returns a list {@link ContactInfo} objects of favorite types given by array parameter <code>favoriteType</code> and if parameter <code>onHike</code> is one then
	 * these are hike contacts otherwise non hike contacts. This list should not contain contact whose msisdn is same as parameter <code>myMsisdn</code>.
	 * <p>
	 * If parameter <code>nativeSMSOn</code> is false then contacts which are either hike contacts or contacts whose msisdns start with '+91' are included in the list
	 * </p>
	 * If parameter <code>ignoreUnknownContacts</code> is set to true then returned list does not contains unsaved contacts
	 * 
	 * @param favoriteType
	 * @param onHike
	 * @param myMsisdn
	 * @param nativeSMSOn
	 * @param ignoreUnknownContacts
	 * @return
	 */
	public List<ContactInfo> getContactsOfFavoriteType(FavoriteType[] favoriteType, int onHike, String myMsisdn, boolean nativeSMSOn, boolean ignoreUnknownContacts)
	{
		List<ContactInfo> contacts = transientCache.getContactsOfFavoriteType(favoriteType, onHike, myMsisdn, nativeSMSOn, ignoreUnknownContacts);

		if (!transientCache.allContactsLoaded)
		{
			for (ContactInfo con : contacts)
			{
				if (null == getContact(con.getMsisdn()))
				{
					transientCache.insertContact(con);
				}
			}
		}
		return contacts;
	}

	/**
	 * This method returns a list of {@link ContactInfo} objects which are hike contacts and msisdns are in parameter <code>msisdnsIn</code> and not in <code>msisdnsNotIn</code>.
	 * List should also not contain contact corresponding to msisdn in parameter <code>myMsisdn</code>.
	 * 
	 * @param limit
	 *            Maximum number of hike contacts that are needed
	 * @param msisdnsIn
	 * @param msisdnsNotIn
	 * @param myMsisdn
	 * @return
	 */
	public List<ContactInfo> getHikeContacts(int limit, Set<String> msisdnsIn, Set<String> msisdnsNotIn, String myMsisdn)
	{
		List<ContactInfo> contacts = transientCache.getHikeContacts(limit, msisdnsIn, msisdnsNotIn, myMsisdn);

		if (!transientCache.allContactsLoaded)
		{
			for (ContactInfo con : contacts)
			{
				if (null == getContact(con.getMsisdn()))
				{
					transientCache.insertContact(con);
				}
			}
		}
		return contacts;
	}

	/**
	 * This method returns the non hike contacts
	 * 
	 * @return
	 */
	public List<Pair<AtomicBoolean, ContactInfo>> getNonHikeContacts()
	{
		List<Pair<AtomicBoolean, ContactInfo>> nonHikeContacts = transientCache.getNonHikeContacts();
		if (!transientCache.isAllContactsLoaded())
		{
			for (Pair<AtomicBoolean, ContactInfo> pair : nonHikeContacts)
			{
				ContactInfo contact = pair.second;
				if (null == getContact(contact.getMsisdn()))
				{
					transientCache.insertContact(contact);
				}
			}
		}
		return nonHikeContacts;
	}

	/**
	 * This method returns the list of {@link ContactInfo} objects which are not on hike and are the most contacted contacts of the user
	 * 
	 * @param limit
	 *            Maximum number of contacts that are needed
	 * @return
	 */
	public List<ContactInfo> getNonHikeMostContactedContacts(int limit)
	{
		/*
		 * Sending twice the limit to account for the contacts that might be on hike
		 */
		Pair<String, Map<String, Integer>> data = getMostContactedContacts(context, limit * 2);
		List<ContactInfo> contacts = transientCache.getNonHikeMostContactedContacts(data.first, data.second, limit);

		if (!transientCache.isAllContactsLoaded())
		{
			for (ContactInfo con : contacts)
			{
				if (null == getContact(con.getMsisdn()))
				{
					transientCache.insertContact(con);
				}
			}
		}
		return contacts;
	}

	/**
	 * This method returns {@link ContactInfo} object whose phoneNumber matches with parameter <code>number</code>. And if not found in memory makes DB query
	 * 
	 * @param number
	 * @return
	 */
	public ContactInfo getContactInfoFromPhoneNo(String number)
	{
		ContactInfo contact = persistenceCache.getContactInfoFromPhoneNo(number);
		if (null != contact)
		{
			return contact;
		}
		return transientCache.getContactInfoFromPhoneNo(number);
	}

	/**
	 * The parameter <code>number</code> can be either Phone number or msisdn, so this method returns {@link ContactInfo} object in which either Phone number matches number or
	 * msisdn matches number.
	 * 
	 * @param number
	 * @return
	 */
	public ContactInfo getContactInfoFromPhoneNoOrMsisdn(String number)
	{
		return getContactInfoFromPhoneNoOrMsisdn(number, true);
	}
	
	public ContactInfo getContactInfoFromPhoneNoOrMsisdn(String number, boolean selfNameAsYou)
	{
		ContactInfo contact = persistenceCache.getContactInfoFromPhoneNoOrMsisdn(number);
		if (null != contact)
		{
			return contact;
		}
		contact = transientCache.getContactInfoFromPhoneNoOrMsisdn(number);
		if (null != contact)
		{
			return contact;
		}
		
		ContactInfo selfContactInfo = Utils.getUserContactInfo(selfNameAsYou);
		if (number.equals(selfContactInfo.getMsisdn()))
		{
			contact = selfContactInfo;
		}
		
		return contact;
	}


    /**
     * The parameter <code>number</code> would be array of Phone numbers and array of msisdn, so this method returns {@link List<ContactInfo>} object in which either Phone number matches number or
     * msisdn matches number.
     *
     * @param
     * @return
     */
	public List<ContactInfo> getContactInfoListForMsisdnFilter(String msisdnList)
	{

		List<String> msisdns = new ArrayList<String>(Arrays.asList(msisdnList.split(",")));

		List<ContactInfo> contactInfoList = new ArrayList<>();

		// Traversing and checking in both persistent and transient cache for msisdns list
		Iterator<String> it = msisdns.iterator();
		while (it.hasNext())
		{
			String number = it.next();
			if (!TextUtils.isEmpty(number))
			{
				ContactInfo contact = persistenceCache.getContactInfoFromPhoneNoOrMsisdn(number);
				if (null != contact)
				{
					contactInfoList.add(contact);
					it.remove();
				}
				else
				{
					contact = transientCache.getContactInfoFromPhoneNoOrMsisdn(number);
					if (null != contact)
					{
						contactInfoList.add(contact);
						it.remove();
					}
				}
			}
		}

		List contactsInfoListFromDbCall = new ArrayList();

		if (msisdns.size() > 0)
		{
			contactsInfoListFromDbCall = transientCache.getContactListFromDb(msisdns);
		}

		if (contactsInfoListFromDbCall != null)
			contactInfoList.addAll(contactsInfoListFromDbCall);

		return contactInfoList;
	}

	@Override
	public void load()
	{
		// TODO Auto-generated method stub
	}

	/**
	 * return true if conversation with this id already exists group and 1-1 conversations both will be checked The implementation is thread safe
	 * 
	 * @param msisdn
	 * @return
	 */
	public boolean isConvExists(String id)
	{
		return persistenceCache.convExist(id);
	}

	/**
	 * Thread safe implementation. Returns true if group with parameter <code>groupId</code> already exists.
	 * 
	 * @param groupId
	 * @return
	 */
	public boolean isGroupExist(String groupId)
	{
		return persistenceCache.isGroupExists(groupId);
	}

	/**
	 * This method inserts a group with <code>grpId</code> and <code>groupName</code> in the {@link PersistenceCache}. Should be called when new group is created. Group alive
	 * status is set as true in this method
	 * 
	 * @param grpId
	 * @param groupName
	 */
	public void insertGroup(String grpId, String groupName)
	{
		persistenceCache.insertGroup(grpId, groupName, true);
	}

	/**
	 * This method inserts a group with <code>grpId</code> and <code>groupName</code> in the {@link PersistenceCache}. Group alive status is passed as a parameter
	 * 
	 * @param grpId
	 * @param groupName
	 * @param alive
	 */
	public void insertGroup(String grpId, String groupName, boolean alive)
	{
		persistenceCache.insertGroup(grpId, groupName, alive);
	}

	/**
	 * This method deletes the contacts of particular set of ids given by parameter <code>keySet</code> from users database
	 *
	 * @param keySet
	 */
	public void deleteMultipleContactInDB(Map<String, List<ContactInfo>> keySet)
	{
		hDb.deleteMultipleRows(keySet);
	}

	/**
	 * This method updates the contacts given as parameter <code>updatedContacts</code> in the database
	 * 
	 * @param updatedContacts
	 */
	public void updateContactsinDB(List<ContactInfo> updatedContacts)
	{
		hDb.updateContacts(updatedContacts);
	}

	/**
	 * This method updates the hike status of a contact in memory (if currently loaded in memory) and as well as in the database
	 * 
	 * @param msisdn
	 * @param onhike
	 * @return
	 */
	public int updateHikeStatus(String msisdn, boolean onhike)
	{
		ContactInfo contact = getContact(msisdn);
		if (null != contact)
		{
			ContactInfo updatedContact = new ContactInfo(contact);
			updatedContact.setOnhike(onhike);
			updateContacts(updatedContact);
		}
		return hDb.updateHikeContact(msisdn, onhike);
	}

	/**
	 * This method checks whether a contact has a icon or not. 
	 * 
	 * @param msisdn
	 * @return
	 */
	public boolean hasIcon(String msisdn)
	{
		return hDb.hasIcon(msisdn);
	}

	/**
	 * This method updates the <code>lastMessaged</code> parameter of {@link ContactInfo} object and also updates it in database
	 * 
	 * @param msisdn
	 * @param timestamp
	 */
	public void updateContactRecency(String msisdn, long timestamp)
	{
		updateContactRecency(msisdn, timestamp, true);
	}

	/**
	 * This method updates the <code>lastMessaged</code> parameter of {@link ContactInfo} object and updates in database depending on parameter <code>updateInDb</code>
	 * 
	 * @param msisdn
	 * @param timestamp
	 * @param updateInDb
	 */
	public void updateContactRecency(String msisdn, long timestamp, boolean updateInDb)
	{
		ContactInfo contact = getContact(msisdn);
		if (null != contact)
		{
			ContactInfo updatedContact = new ContactInfo(contact);
			updatedContact.setLastMessaged(timestamp);
			updateContacts(updatedContact);
		}
		if (updateInDb)
		{
			hDb.updateContactRecency(msisdn, timestamp);
		}
	}

	/**
	 * This method is used to insert in block table when a contact is blocked
	 * 
	 * @param msisdn
	 */
	public void block(String msisdn)
	{
		persistenceCache.block(msisdn);
		hDb.block(msisdn);
	}

	public void block(List<String> msisdns)
	{
		persistenceCache.block(msisdns);
		List<PairModified<String,String>> list=new ArrayList<>(msisdns.size());
		for (String s:msisdns)
		{
			list.add(new PairModified<String, String>(s,null));
		}
		hDb.addBlockList(list);
	}

	/**
	 * This method updates the favorite type of a contact in memory as well as in database
	 * 
	 * @param msisdn
	 * @param ftype
	 */
	public void toggleContactFavorite(String msisdn, FavoriteType ftype)
	{
		ContactInfo contact = getContact(msisdn);
		if (null != contact)
		{
			ContactInfo updatedContact = new ContactInfo(contact);
			updatedContact.setFavoriteType(ftype);
			updateContacts(updatedContact);
		}
		hDb.toggleContactFavorite(msisdn, ftype);
	}

	/**
	 * This method deletes the msisdn from the block table when a contact is unblocked
	 * 
	 * @param msisdn
	 */
	public void unblock(String msisdn)
	{
		persistenceCache.unblock(msisdn);
		hDb.unblock(msisdn);
	}

	/**
	 * This method removes the icon from database and also updates the <code>hasCustomPhoto</code> parameter of {@link ContactInfo} object
	 * 
	 * @param msisdn
	 */
	public boolean removeIcon(String msisdn)
	{
		return hDb.removeIcon(msisdn);
	}

	/**
	 * This method updates the hike join time of a contact in memory as well as in database
	 * 
	 * @param msisdn
	 * @param hikeJoinTime
	 */
	public void setHikeJoinTime(String msisdn, long hikeJoinTime)
	{
		ContactInfo contact = getContact(msisdn);
		if (null != contact)
		{
			ContactInfo updatedContact = new ContactInfo(contact);
			updatedContact.setHikeJoinTime(hikeJoinTime);
			updateContacts(updatedContact);
		}
		hDb.setHikeJoinTime(msisdn, hikeJoinTime);
	}

	/**
	 * This method sets the contacts icon in database and updates the <code>hasCustomPhoto</code> parameter of {@link ContactInfo}
	 * 
	 * @param msisdn
	 * @param data
	 * @param isProfileImage
	 */
	public void setIcon(String msisdn, byte[] data, boolean isProfileImage)
	{
		hDb.setIcon(msisdn, data, isProfileImage);
	}

	/**
	 * This method returns the favorite type of a contact. If contact is already loaded in memory then we can get from {@link ContactInfo#getFavoriteType()} otherwise we make a
	 * database query.
	 * 
	 * @param msisdn
	 * @return
	 */
	public FavoriteType getFriendshipStatus(String msisdn)
	{
		ContactInfo contact = getContact(msisdn);
		if (null != contact)
		{
			return contact.getFavoriteType();
		}
		return hDb.getFriendshipStatus(msisdn);
	}

	/**
	 * This method returns the string of an icon in thumbnails table
	 * 
	 * @param id
	 * @return
	 */
	public String getIconIdentifierString(String id)
	{
		return hDb.getIconIdentifierString(id);
	}

	/**
	 * This method changes the favorite type of contacts in memory as well as in database using the json passed as parameter
	 * 
	 * @param favorites
	 */
	public void setMultipleContactsToFavorites(JSONArray favorites)
	{
		hDb.setMultipleContactsToFavorites(favorites);
	}

	public boolean isUnknownContact(String msisdn)
	{
		ContactInfo contact = getContact(msisdn, true, false);
		if (null == contact)
		{
			return true;
		}
		return contact.isUnknownContact();
	}
	
	/**
	 * This method returns true if a particular msisdn is blocked otherwise false
	 * 
	 * @param msisdn
	 * @return
	 */
	public boolean isBlocked(String msisdn)
	{
		return persistenceCache.isBlocked(msisdn);
	}

	/**
	 * This method updates the last seen time of a contact in memory as well as in database
	 * 
	 * @param msisdn
	 * @param lastSeenTime
	 */
	public void updateLastSeenTime(String msisdn, long lastSeenTime)
	{
		ContactInfo contact = getContact(msisdn);
		if (null != contact)
		{
			ContactInfo updatedContact = new ContactInfo(contact);
			updatedContact.setLastSeenTime(lastSeenTime);
			updateContacts(updatedContact);
		}
		hDb.updateLastSeenTime(msisdn, lastSeenTime);
	}

	/**
	 * This methods updates the <code>isOffline</code> parameter of {@link ContactInfo} object in memory as well as in database
	 * 
	 * @param msisdn
	 * @param isOffline
	 */
	public void updateIsOffline(String msisdn, int isOffline)
	{
		ContactInfo contact = getContact(msisdn);
		if (null != contact)
		{
			ContactInfo updatedContact = new ContactInfo(contact);
			updatedContact.setOffline(isOffline);
			updateContacts(updatedContact);
		}
		hDb.updateIsOffline(msisdn, isOffline);
	}

	/**
	 * This method checks whether a contact exists or not i.e it is a saved contact or not
	 * 
	 * @param msisdn
	 * @return
	 */
	public boolean doesContactExist(String msisdn)
	{
		ContactInfo contact = persistenceCache.getContact(msisdn);
		if (null != contact && null != contact.getName())
			return true;
		return transientCache.doesContactExist(msisdn);
	}

	/**
	 * This method returns a drawable of an icon from the database
	 * 
	 * @param msisdn
	 * @return
	 */
	public Drawable getIcon(String msisdn)
	{
		return hDb.getIcon(msisdn);
	}

	/**
	 * Replaces thumbnail for the given msisdn
	 * 
	 * @param msisdn
	 * @param thumbnail
	 */
	public void changeUsersDbThumbnail(String msisdn, byte[] thumbnail)
	{
		hDb.setThumbnail(msisdn, thumbnail);
	}
	
	/**
	 * This method returns a byte array of an icon from the database
	 * 
	 * @param id
	 * @return
	 */
	public byte[] getIconByteArray(String id)
	{
		return hDb.getIconByteArray(id);
	}

	/**
	 * Deletes all the users database tables
	 */
	public void deleteAll()
	{
		hDb.deleteAll();
	}

	/**
	 * This methods returns a HashSet of blocked msisdns
	 * 
	 * @return
	 */
	public Set<String> getBlockedMsisdnSet()
	{
		return persistenceCache.getBlockedMsisdnSet();
	}

	/**
	 * Sets the address book from the list of contacts Deletes any existing contacts from the db
	 * 
	 * @param contacts
	 *            list of contacts to set/add
	 * @param blockedMsisdns
	 * @throws DbException
	 */
	public void setAddressBookAndBlockList(List<ContactInfo> contacts, List<PairModified<String,String>> blockedMsisdns) throws DbException
	{
		List<String> blockList=new ArrayList<>(blockedMsisdns.size());
		for(PairModified<String,String> pm:blockedMsisdns)
		{
			blockList.add(pm.getFirst());
		}
		persistenceCache.block(blockList);
		hDb.setAddressBookAndBlockList(contacts, blockedMsisdns);
	}

	public void syncContactExtraInfo()
	{
		hDb.syncContactExtraInfo();
	}

	/**
	 * This method returns the list of contacts and it is paired with a AtomicBoolean which tells whether a contact is blocked or not.
	 * 
	 * @return
	 */
	public List<Pair<AtomicBoolean, ContactInfo>> getBlockedUserList()
	{
		return transientCache.getBlockedUserList();
	}

	/**
	 * This method returns an object of {@link FtueContactsData} class which includes server recommended contacts and hike and sms contacts and their count
	 * 
	 * @param prefs
	 * @return
	 */
	public FtueContactsData getFTUEContacts(SharedPreferences prefs)
	{
		return hDb.getFTUEContacts(prefs);
	}

	/**
	 * This method updates the invite time stamp in memory as well as in database
	 * 
	 * @param msisdn
	 * @param time
	 */
	public void updateInvitedTimestamp(String msisdn, long time)
	{
		ContactInfo contact = getContact(msisdn);
		if (null != contact)
		{
			ContactInfo updatedContact = new ContactInfo(contact);
			updatedContact.setInviteTime(time);
			updateContacts(updatedContact);
		}
		hDb.updateInvitedTimestamp(msisdn, time);
	}

	/**
	 * This method returns a map of group id and group name for a list of group ids given as a parameter and inserts these in {@link PersistenceCache}
	 * 
	 * @param grpIds
	 * @return
	 */
	public Map<String, GroupDetails> getGroupDetails(List<String> grpIds)
	{
		Map<String, GroupDetails> groupNames = HikeConversationsDatabase.getInstance().getIdGroupDetailsMap(grpIds);
		for (Entry<String, GroupDetails> mapEntry : groupNames.entrySet())
		{
			String groupId = mapEntry.getKey();
			GroupDetails grpDetails = mapEntry.getValue();
			persistenceCache.insertGroup(groupId, grpDetails);
		}
		return groupNames;
	}

	/**
	 * Returns a list of participants to a group with their names in case of unsaved contact. This method also increases the reference count if contact is already loaded in memory
	 * 
	 * @param groupId
	 * @param activeOnly
	 * @param notShownStatusMsgOnly
	 * @return
	 */
	public List<PairModified<GroupParticipant, String>> getGroupParticipants(String groupId, boolean activeOnly, boolean notShownStatusMsgOnly)
	{
		return getGroupParticipants(groupId, activeOnly, notShownStatusMsgOnly, true);
	}

	/**
	 * Returns a list of participants in a 1-n conversation with their names in case of unsaved contact. This method also increases reference count if the contact is already loaded
	 * in memory
	 * 
	 * @param convId
	 * @return
	 */
	public List<PairModified<GroupParticipant, String>> getActiveConversationParticipants(String convId)
	{
		return getGroupParticipants(convId, true, false);
	}

	/**
	 * Returns a list of participants of a group with their names in case of unsaved contact. This method also increases the reference count if contact is already loaded in memory
	 * 
	 * @param groupId
	 * @param activeOnly
	 * @param notShownStatusMsgOnly
	 * @param fetchParticipants
	 * @return
	 */
	public List<PairModified<GroupParticipant, String>> getGroupParticipants(String groupId, boolean activeOnly, boolean notShownStatusMsgOnly, boolean fetchParticipants)
	{
		Pair<Map<String, PairModified<GroupParticipant, String>>, List<String>> groupPair = transientCache.getGroupParticipants(groupId, activeOnly, notShownStatusMsgOnly,
				fetchParticipants);
		Map<String, PairModified<GroupParticipant, String>> groupParticipantsMap = groupPair.first;
		List<String> allMsisdns = groupPair.second;

		if (null != allMsisdns)
		{
			// at least one msisdn is required to run this in query
			if (fetchParticipants && allMsisdns.size() > 0)
			{
				List<ContactInfo> list = new ArrayList<ContactInfo>();
				List<String> msisdnsDB = new ArrayList<String>();

				// traverse all msisdns if found in transient memory increment ref count by one
				for (String ms : allMsisdns)
				{
					ContactInfo contact = transientCache.getContact(ms);

					if (null != contact)
					{
						// increment ref count
						transientCache.incrementRefCount(ms);
						list.add(contact);
					}
					else
					{
						contact = persistenceCache.getContact(ms);
						if (null != contact)
						{
							transientCache.insertContact(contact);
							list.add(contact);
						}
						else
						{
							msisdnsDB.add(ms);
						}
					}
				}

				list.addAll(transientCache.putInCache(msisdnsDB));

				for (ContactInfo contactInfo : list)
				{
					PairModified<GroupParticipant, String> groupParticipantPair = groupParticipantsMap.get(contactInfo.getMsisdn());
					if (contactInfo.getName() == null)
					{
						String name = groupParticipantPair.getFirst().getContactInfo().getName();
						groupParticipantPair.setSecond(name);

						/*
						 * For unsaved participants we don't have Contact information in users table -- their on hike status is not known to us. We get on hike status of unsaved
						 * contact in a group from group members table
						 */
						ContactInfo con = groupParticipantPair.getFirst().getContactInfo();
						contactInfo.setOnhike(con.isOnhike());
						groupParticipantPair.getFirst().setContactInfo(contactInfo);
					}
					else
					{
						groupParticipantPair.getFirst().setContactInfo(contactInfo);
						groupParticipantPair.setSecond(contactInfo.getName());
					}
				}
			}
		}

		/*
		 * When activeOnly is false and notShownStatusMsgOnly is false then only we get all the group participants therefore we should insert in transient cache only when we have
		 * all the group participants and not partial.
		 */
		if (!activeOnly && !notShownStatusMsgOnly)
			transientCache.insertGroupParticipants(groupId, groupParticipantsMap);

		List<PairModified<GroupParticipant, String>> groupParticipantsList = new ArrayList<PairModified<GroupParticipant, String>>(groupParticipantsMap.values());
		return groupParticipantsList;
	}

	/**
	 * This method adds group participants for a particular <code>groupId</code> into {@link TransientCache}.
	 * 
	 * @param groupId
	 * @param participantList
	 */
	public void addGroupParticipants(String groupId, Map<String, PairModified<GroupParticipant, String>> participantList)
	{
		transientCache.addGroupParticipants(groupId, participantList);
		persistenceCache.updateDefaultGroupName(groupId);
	}

	/**
	 * This method removes group participant of a particular group from {@link TransientCache}
	 * 
	 * @param groupId
	 * @param msisdn
	 */
	public void removeGroupParticipant(String groupId, String msisdn)
	{
		transientCache.removeGroupParticipants(groupId, msisdn);
		persistenceCache.updateDefaultGroupName(groupId);
	}

	/**
	 * This method removes multiple group participants from {@link TransientCache}
	 * @param groupId
	 * @param msisdns
	 */
	public void removeGroupParticipant(String groupId, Collection<String> msisdns)
	{
		transientCache.removeGroupParticipants(groupId, msisdns);
		persistenceCache.updateDefaultGroupName(groupId);
	}

	public void removeGroup(String groupId)
	{
		transientCache.removeGroup(groupId);
	}

	/**
	 * Sets the group name in persistence cache , should be called when group name is changed
	 * 
	 * @param groupId
	 * @param name
	 */
	public void setGroupName(String groupId, String name)
	{
		persistenceCache.setGroupName(groupId, name);
	}

	public void updateGroupRecency(String groupId, long timestamp)
	{
		persistenceCache.updateGroupRecency(groupId, timestamp);
	}

	/**
	 * Returns the number of participants in a particular group.
	 * 
	 * @param groupId
	 * @return
	 */
	public int getActiveParticipantCount(String groupId)
	{
		return transientCache.getGroupParticipantsCount(groupId);
	}

	/**
	 * Call this when we think the address book has changed. Checks for updates, posts to the server, writes them to the local database and updates existing conversations
	 * 
	 * @param ctx
	 */
	public byte syncUpdates(Context ctx)
	{
		List<ContactInfo> newContacts = null;
		byte result = SYNC_CONTACTS_NO_CONTACTS_FOUND_IN_ANDROID_ADDRESSBOOK;
		if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.HIDE_DELETED_CONTACTS, false))
		{
			newContacts = getContacts(ctx);
		}
		else
		{
			newContacts = getContactsOld(ctx);
		}
		if (newContacts == null)
		{
			return result;
		}

		if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ENABLE_AB_SYNC_CHANGE, true))
		{
			result = syncUpdates(newContacts, transientCache.getAllContactsForSyncing());
		}
		else
		{
			result = syncUpdatesOld(newContacts, transientCache.getAllContactsForSyncing());
		}
		return result;
	}

	/**
	 * Sync Updates **Deprecated**
	 * @deprecated
	 * @param ctx
	 */
	public byte syncUpdatesOld(List<ContactInfo> deviceContacts, List<ContactInfo> hikeContacts)
	{
		Logger.d("ContactUtils", "Old way to sync conctacts.");
		Map<String, List<ContactInfo>> new_contacts_by_id = convertToMap(deviceContacts);
		Map<String, List<ContactInfo>> hike_contacts_by_id = convertToMap(hikeContacts);

		/*
		 * iterate over every item in the phone db, items that are equal remove from both maps items that are different, leave in 'new' map and remove from 'hike' map send the
		 * 'new' map as items to add, and send the 'hike' map as IDs to remove
		 */
		Map.Entry<String, List<ContactInfo>> entry = null;
		Set<String> msisdns = new HashSet<String>();
		for (Iterator<Map.Entry<String, List<ContactInfo>>> iterator = new_contacts_by_id.entrySet().iterator(); iterator.hasNext();)
		{
			entry = iterator.next();
			String id = entry.getKey();
			List<ContactInfo> contacts_for_id = entry.getValue();
			List<ContactInfo> hike_contacts_for_id = hike_contacts_by_id.get(id);

			/*
			 * If id is not present in hike user DB i.e new contact is added to Phone AddressBook. When the items are the same, we remove the item @ the current iterator. This will
			 * result in the item *not* being sent to the server
			 */
			if (hike_contacts_for_id == null)
			{
				continue;
			}
			else if (areListsEqual(contacts_for_id, hike_contacts_for_id))
			{
				/* hike db is up to date, so don't send update */
				iterator.remove();
				hike_contacts_by_id.remove(id);
				for (ContactInfo con : hike_contacts_for_id)
				{
					msisdns.add(con.getMsisdn());
				}
				continue;
			}
			/* item is different than our db, so send an update */
			for (ContactInfo con : hike_contacts_for_id)
			{
				msisdns.add(con.getMsisdn());
			}
			hike_contacts_by_id.remove(id);
		}

		/*
		 * Google contact ids changes randomly for some contacts in addressbook which causes unnecessary http calls to the server.
		 * 
		 * If a contact id of a contact changes from "prev" to "curr" then we make a call to delete "prev" and add "curr".
		 * 
		 * To reduce these types of http requests , we are checking for equality of name and number also in following logic and removing these entries to be added and deleted if
		 * contact name and number are same even if there ids have changed
		 */
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.CONTACT_REMOVE_DUPLICATES_WHILE_SYNCING, true))
		{
			Logger.d("ContactUtils", "New contacts :" + new_contacts_by_id.size() + "  deleted contacts : " + hike_contacts_by_id.size() + "  before removing duplicates");
			Logger.d("ContactUtils", "New contacts Details before removing duplicates :" + new_contacts_by_id);
			Logger.d("ContactUtils", "Deleted contacts Details before removing duplicates : " + hike_contacts_by_id);

			HashSet<String> hikeContactsSet = new HashSet<String>();
			for (Entry<String, List<ContactInfo>> en : hike_contacts_by_id.entrySet())
			{
				List<ContactInfo> contactsList = en.getValue();
				for (ContactInfo contact : contactsList)
				{
					hikeContactsSet.add(contact.getName() + "_" + contact.getPhoneNum());
				}
			}

			HashSet<String> commonContactsSet = new HashSet<String>();

			Map.Entry<String, List<ContactInfo>> newContactEntry = null;
			for (Iterator<Map.Entry<String, List<ContactInfo>>> iterator = new_contacts_by_id.entrySet().iterator(); iterator.hasNext();)
			{
				newContactEntry = iterator.next();
				List<ContactInfo> contactsList = newContactEntry.getValue();
				for (int index = contactsList.size() - 1; index >= 0; --index)
				{
					ContactInfo contact = contactsList.get(index);
					String key = contact.getName() + "_" + contact.getPhoneNum();
					if (hikeContactsSet.contains(key))
					{
						contactsList.remove(index);
						commonContactsSet.add(key);
					}
				}
				if (contactsList.isEmpty())
				{
					iterator.remove();
				}
			}

			Logger.d("ContactUtils", "Common contacts set : " + commonContactsSet);

			Map.Entry<String, List<ContactInfo>> hikeContactEntry = null;
			for (Iterator<Map.Entry<String, List<ContactInfo>>> iterator = hike_contacts_by_id.entrySet().iterator(); iterator.hasNext();)
			{
				hikeContactEntry = iterator.next();
				List<ContactInfo> contactsList = hikeContactEntry.getValue();
				for (int index = contactsList.size() - 1; index >= 0; --index)
				{
					ContactInfo contact = contactsList.get(index);
					String key = contact.getName() + "_" + contact.getPhoneNum();
					if (commonContactsSet.contains(key))
					{
						contactsList.remove(index);
					}
				}
				if (contactsList.isEmpty())
				{
					iterator.remove();
				}
			}
		}

		/*
		 * our address object should an update dictionary, and a list of IDs to remove
		 */

		/* return early if things are in sync */
		if ((new_contacts_by_id.isEmpty()) && (hike_contacts_by_id.isEmpty()))
		{
			Logger.d("ContactUtils", "DB in sync");
			return SYNC_CONTACTS_DB_IN_SYNC;
		}

		try
		{
			JSONArray ids_json = new JSONArray();
			for (String string : hike_contacts_by_id.keySet())
			{
				ids_json.put(string);
			}
			Logger.d("ContactUtils", "New contacts:" + new_contacts_by_id.size() + " DELETED contacts: " + ids_json.length());
			Logger.d("ContactUtils", "New contacts Details :" + new_contacts_by_id);
			Logger.d("ContactUtils", "DELETED contacts Details : " + ids_json);
			
			List<ContactInfo> updatedContacts = null;
			if (Utils.isAddressbookCallsThroughHttpMgrEnabled())
			{
				updatedContacts = new UpdateAddressBookTask(new_contacts_by_id, ids_json).execute();
			}
			else
			{
				updatedContacts = AccountUtils.updateAddressBook(new_contacts_by_id, ids_json);
			}

			if (updatedContacts == null)
			{
				Logger.e("ContactUtils", " updated contacts is null some error occurred during request execution");
				return SYNC_CONTACTS_ERROR;
			}

			List<ContactInfo> contactsToDelete = new ArrayList<ContactInfo>();
			String myMsisdn = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, "");
			
			if (!hike_contacts_by_id.isEmpty())
			{
				for (Entry<String, List<ContactInfo>> mapEntry : new_contacts_by_id.entrySet())
				{
					List<ContactInfo> contacts = mapEntry.getValue();
					for (ContactInfo con : contacts)
					{
						msisdns.add(con.getMsisdn());
					}
				}
			}

			for (Entry<String, List<ContactInfo>> mapEntry : hike_contacts_by_id.entrySet())
			{
				List<ContactInfo> contacts = mapEntry.getValue();
				contactsToDelete.addAll(contacts);
				// Update conversation fragement for deleted ids
				for (ContactInfo c : contacts)
				{
					c.setName(null);
					c.setId(c.getMsisdn());
					/*
					 * not deleting profile icon in case of contact to be deleted 
					 * 1. is self contact
					 * 2. has favorite state : friends
					 * 3. has favorite state : request received
					 * 4. has favorite state : request received rejected
					 * 
					 * Also if one msisdn is saved with more than one name in address book
					 */
					if (Utils.shouldDeleteIcon(c, myMsisdn) && !msisdns.contains(c.getMsisdn()))
					{
						HikeMessengerApp.getLruCache().deleteIconForMSISDN(c.getMsisdn());
					}
					HikeMessengerApp.getPubSub().publish(HikePubSub.CONTACT_DELETED, c);
				}
			}

			deleteContacts(contactsToDelete);

			/* Delete ids from hike user DB */
			deleteMultipleContactInDB(hike_contacts_by_id);
			updateContactsinDB(updatedContacts);
			syncContacts(updatedContacts);

		}
		catch (Exception e)
		{
			Logger.e("ContactUtils", "error updating addressbook", e);
			return SYNC_CONTACTS_ERROR;
		}
		return SYNC_CONTACTS_CHANGED;
	}
	
	public byte syncUpdates(List<ContactInfo> deviceContacts, List<ContactInfo> hikeContacts)
	{

		Map<String, List<ContactInfo>> new_contacts_by_id = convertToMap(deviceContacts);
		Map<String, List<ContactInfo>> hike_contacts_by_id = convertToMap(hikeContacts);
		Map<String, ContactInfo> hike_contacts_by_msisdn = convertToMapWithMsisdnAsKey(hikeContacts);
		Map<String, ContactInfo> hike_contacts_by_name_msisdn = convertToMapWithNameMsisdnAsKey(hikeContacts);

		/*
		 * iterate over every item in the phone db, items that are equal remove from both maps items that are different, leave in 'new' map and remove from 'hike' map send the
		 * 'new' map as items to add, and send the 'hike' map as IDs to remove
		 */
		Map.Entry<String, List<ContactInfo>> entry = null;
		Set<String> msisdns = new HashSet<String>();
		for (Iterator<Map.Entry<String, List<ContactInfo>>> iterator = new_contacts_by_id.entrySet().iterator(); iterator.hasNext();)
		{
			entry = iterator.next();
			String id = entry.getKey();
			List<ContactInfo> contacts_for_id = entry.getValue();
			List<ContactInfo> hike_contacts_for_id = hike_contacts_by_id.get(id);

			/*
			 * If id is not present in hike user DB i.e new contact is added to Phone AddressBook. When the items are the same, we remove the item @ the current iterator. This will
			 * result in the item *not* being sent to the server
			 */
			if (hike_contacts_for_id == null)
			{
				/*
				 * Removing the contact which is already present in hike db with different id on the basis of msisdn.
				 */
				Logger.d("ContactUtils", "Contact won't present in hike DB for id = " + id);
				for (Iterator<ContactInfo> iterator2 = contacts_for_id.iterator(); iterator2.hasNext();)
				{
					ContactInfo c = (ContactInfo) iterator2.next();
					if(hike_contacts_by_msisdn.containsKey(c.getPhoneNum()))
					{
						ContactInfo hikeCInfo = (ContactInfo) hike_contacts_by_msisdn.get(c.getPhoneNum());
						/*
						 * One special case is handled where user deleted and added the same contact again at the same time.
						 */
						if(hikeCInfo != null && !new_contacts_by_id.containsKey(hikeCInfo.getId()) && hike_contacts_by_id.containsKey(hikeCInfo.getId()))
						{
							Logger.d("ContactUtils", "Removing contact from android db contact list");
							iterator2.remove();
						}
					}
				}
				if(contacts_for_id.isEmpty())
				{
					iterator.remove();
				}
				continue;
			}
			else if (areListsEqual(contacts_for_id, hike_contacts_for_id))
			{
				/* hike db is up to date, so don't send update */
				iterator.remove();
				hike_contacts_by_id.remove(id);
				for (ContactInfo con : hike_contacts_for_id)
				{
					msisdns.add(con.getMsisdn());
				}
				continue;
			}
			else if(contacts_for_id != null && contacts_for_id.size() < hike_contacts_for_id.size())
			{
				for (ContactInfo con : hike_contacts_for_id)
				{
					msisdns.add(con.getMsisdn());
				}
				for (Iterator<ContactInfo> itrtr = contacts_for_id.iterator(); itrtr.hasNext();)
				{
					ContactInfo c1 = (ContactInfo) itrtr.next();
					for (Iterator<ContactInfo> itrtr2 = hike_contacts_for_id.iterator(); itrtr2.hasNext();)
					{
						ContactInfo c2 = (ContactInfo) itrtr2.next();
						if(c1.hashCode() == c2.hashCode())
						{
							itrtr2.remove();
							hike_contacts_by_name_msisdn.remove(c2.getName()  + "_" + c2.getPhoneNum());
						}
					}
				}
				if(contacts_for_id.isEmpty())
				{
					iterator.remove();
				}
				if(hike_contacts_for_id.isEmpty())
				{
					hike_contacts_by_id.remove(id);
				}
				continue;
			}
			/* item is different than our db, so send an update */
			for (ContactInfo con : hike_contacts_for_id)
			{
				msisdns.add(con.getMsisdn());
			}
			hike_contacts_by_id.remove(id);
		}

		/*
		 * Consuming request for contacts which are present in hike with different id.
		 * Based on name + msisdn pair.
		 */
		if(!new_contacts_by_id.isEmpty() || !hike_contacts_by_id.isEmpty())
		{
			for (Iterator<Map.Entry<String, List<ContactInfo>>> iterator = new_contacts_by_id.entrySet().iterator(); iterator.hasNext();)
			{
				entry = iterator.next();
				String id = entry.getKey();
				List<ContactInfo> contacts_for_id = entry.getValue();
				for (Iterator<ContactInfo> iterator2 = contacts_for_id.iterator(); iterator2.hasNext();)
				{
					ContactInfo c = iterator2.next();
					String addKey = c.getName() + "_" + c.getPhoneNum();
					if(hike_contacts_by_name_msisdn.containsKey(addKey))
					{
						iterator2.remove();
						if(!hike_contacts_by_id.isEmpty())
						{
							ContactInfo tInfo = hike_contacts_by_name_msisdn.get(addKey);
							if(tInfo != null && hike_contacts_by_id.containsKey(tInfo.getId()))
							{
								List<ContactInfo> hike_contacts_for_id = hike_contacts_by_id.get(tInfo.getId());
								for (Iterator<ContactInfo> itrtr = hike_contacts_for_id.iterator(); itrtr.hasNext();)
								{
									ContactInfo c2 = (ContactInfo) itrtr.next();
									String removeKey = c2.getName() + "_" + c2.getPhoneNum();
									if(addKey.equals(removeKey))
									{
										itrtr.remove();
									}
								}
							}
						}
					}
				}
				if(contacts_for_id.isEmpty())
				{
					iterator.remove();
				}
			}
		}

		/* return early if things are in sync */
		if ((new_contacts_by_id.isEmpty()) && (hike_contacts_by_id.isEmpty()))
		{
			Logger.d("ContactUtils", "DB in sync");
			return SYNC_CONTACTS_DB_IN_SYNC;
		}

		try
		{
			JSONArray ids_json = new JSONArray();
			for (String string : hike_contacts_by_id.keySet())
			{
				ids_json.put(string);
			}
			Logger.d("ContactUtils", "New contacts:" + new_contacts_by_id.size() + " DELETED contacts: " + ids_json.length());
			Logger.d("ContactUtils", "New contacts Details :" + new_contacts_by_id);
			Logger.d("ContactUtils", "DELETED contacts Details : " + ids_json);
			
			List<ContactInfo> updatedContacts = null;
			if (Utils.isAddressbookCallsThroughHttpMgrEnabled())
			{
				updatedContacts = new UpdateAddressBookTask(new_contacts_by_id, ids_json).execute();
			}
			else
			{
				updatedContacts = AccountUtils.updateAddressBook(new_contacts_by_id, ids_json);
			}

			if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ENABLE_AB_SYNC_DEBUGING, false))
			{
				JSONObject debugData = new JSONObject();
				try
				{
					debugData.put(HIKE_CONTACT_LIST, hikeContacts);
					debugData.put(DEVICE_CONTACT_LIST, deviceContacts);
					debugData.put(NEW_CONTACT_LIST, ContactUtils.getJsonContactList(new_contacts_by_id, false));
					debugData.put(REMOVE_CONTACT_LIST, ContactUtils.getJsonContactList(hike_contacts_by_id, false));
					debugData.put(SERVER_RESPONSE_CONTACT_LIST, updatedContacts);
				} catch (JSONException e) {
					Logger.e(AnalyticsConstants.ANALYTICS_TAG, "AB : Exception occurred while logging dev debug log : "+ e);
				}
				HAManager.getInstance().logDevEvent(ADDRESSBOOK_AREA, AB_SYNC, debugData);
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ENABLE_AB_SYNC_DEBUGING, false);
			}

			if (updatedContacts == null)
			{
				Logger.e("ContactUtils", " updated contacts is null some error occurred during request execution");
				return SYNC_CONTACTS_ERROR;
			}

			List<ContactInfo> contactsToDelete = new ArrayList<ContactInfo>();
			String myMsisdn = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, "");
			
			if (!hike_contacts_by_id.isEmpty())
			{
				for (Entry<String, List<ContactInfo>> mapEntry : new_contacts_by_id.entrySet())
				{
					List<ContactInfo> contacts = mapEntry.getValue();
					for (ContactInfo con : contacts)
					{
						msisdns.add(con.getMsisdn());
					}
				}
			}

			for (Entry<String, List<ContactInfo>> mapEntry : hike_contacts_by_id.entrySet())
			{
				List<ContactInfo> contacts = mapEntry.getValue();
				contactsToDelete.addAll(contacts);
				// Update conversation fragement for deleted ids
				for (ContactInfo c : contacts)
				{
					c.setName(null);
					c.setId(c.getMsisdn());
					/*
					 * not deleting profile icon in case of contact to be deleted 
					 * 1. is self contact
					 * 2. has favorite state : friends
					 * 3. has favorite state : request received
					 * 4. has favorite state : request received rejected
					 * 
					 * Also if one msisdn is saved with more than one name in address book
					 */
					if (Utils.shouldDeleteIcon(c, myMsisdn) && !msisdns.contains(c.getMsisdn()))
					{
						HikeMessengerApp.getLruCache().deleteIconForMSISDN(c.getMsisdn());
					}
					HikeMessengerApp.getPubSub().publish(HikePubSub.CONTACT_DELETED, c);
				}
			}

			deleteContacts(contactsToDelete);

			/* Delete ids from hike user DB */
			deleteMultipleContactInDB(hike_contacts_by_id);
			updateContactsinDB(updatedContacts);
			syncContacts(updatedContacts);

		}
		catch (Exception e)
		{
			Logger.e("ContactUtils", "error updating addressbook", e);
			return SYNC_CONTACTS_ERROR;
		}
		return SYNC_CONTACTS_CHANGED;
	}

	public boolean areListsEqual(List<ContactInfo> list1, List<ContactInfo> list2)
	{
		if (list1 != null && list2 != null)
		{
			if (list1.size() != list2.size())
			{
				return false;
			}
			else if (list1.size() == 0 && list2.size() == 0)
			{
				return false;
			}
			else
			// represents same number of elements
			{
				/* compare each element */
				HashSet<ContactInfo> set1 = new HashSet<ContactInfo>(list1.size());
				for (ContactInfo c : list1)
				{
					set1.add(c);
				}
				boolean flag = true;
				for (ContactInfo c : list2)
				{
					if (!set1.contains(c))
					{
						flag = false;
						break;
					}
				}
				return flag;
			}
		}
		else
		{
			return false;
		}
	}

	public Map<String, List<ContactInfo>> convertToMap(List<ContactInfo> contacts)
	{
		Map<String, List<ContactInfo>> ret = new HashMap<String, List<ContactInfo>>(contacts.size());
		for (ContactInfo contactInfo : contacts)
		{
			if ("__HIKE__".equals(contactInfo.getId()))
			{
				continue;
			}

			List<ContactInfo> l = ret.get(contactInfo.getId());
			if (l == null)
			{
				/*
				 * Linked list is used because removal using iterator is O(1) in linked list vs O(n) in Arraylist
				 */
				l = new LinkedList<ContactInfo>();
				ret.put(contactInfo.getId(), l);
			}
			l.add(contactInfo);
		}
		return ret;
	}

	/**
	 * Converting contacts list to map where msisdn will be key.
	 */
	public Map<String, ContactInfo> convertToMapWithMsisdnAsKey(List<ContactInfo> contacts)
	{
		Map<String, ContactInfo> ret = new HashMap<String, ContactInfo>(contacts.size());
		for (ContactInfo contactInfo : contacts)
		{
			if ("__HIKE__".equals(contactInfo.getId()))
			{
				continue;
			}

			ContactInfo l = ret.get(contactInfo.getPhoneNum());
			if (l == null)
			{
				ret.put(contactInfo.getPhoneNum(), l);
			}
		}
		return ret;
	}

	/**
	 * Converting contacts list to map where <name_msisdn> will be key.
	 */
	public Map<String, ContactInfo> convertToMapWithNameMsisdnAsKey(List<ContactInfo> contacts)
	{
		Map<String, ContactInfo> ret = new HashMap<String, ContactInfo>(contacts.size());
		for (ContactInfo contactInfo : contacts)
		{
			if ("__HIKE__".equals(contactInfo.getId()))
			{
				continue;
			}

			ContactInfo l = ret.get(contactInfo.getPhoneNum());
			if (l == null)
			{
				ret.put(contactInfo.getName() + "_" + contactInfo.getPhoneNum(), l);
			}
		}
		return ret;
	}

	/**
	 * This method is used to get the contacts from the phone's address book and used during contact sync up
	 * @deprecated
	 * @param ctx
	 * @return
	 */
	public List<ContactInfo> getContactsOld(Context ctx)
	{
		Logger.d("ContactUtils", "Old way to read conctacts from device");
		HashSet<String> contactsToStore = new HashSet<String>();
		String[] projection = new String[] { ContactsContract.Contacts._ID, ContactsContract.Contacts.HAS_PHONE_NUMBER, ContactsContract.Contacts.DISPLAY_NAME };

		String selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + "='1'";
		Cursor contacts = null;

		List<ContactInfo> contactinfos = new ArrayList<ContactInfo>();
		Map<String, String> contactNames = new HashMap<String, String>();
		try
		{
			contacts = ctx.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, projection, selection, null, null);

			/*
			 * Added this check for an issue where the cursor is null in some random cases (We suspect that happens when hotmail contacts are synced.)
			 */
			if (contacts == null)
			{
				return null;
			}

			int idFieldColumnIndex = contacts.getColumnIndex(ContactsContract.Contacts._ID);
			int nameFieldColumnIndex = contacts.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
			Logger.d("ContactUtils", "Starting to scan address book");
			while (contacts.moveToNext())
			{
				String id = contacts.getString(idFieldColumnIndex);
				String name = contacts.getString(nameFieldColumnIndex);
				contactNames.put(id, name);
			}
		}
		catch (SecurityException e)
		{
			/**
			 * currently some one plus one users are getting this exception on 5.1. we haven't found
			 * any particular reason how it can happen. putting a catch block for the same.
			 *
			 * In future, when our target sdk version is 23 and user denies contacts permission
			 * to hike. then we would get a security exception here, if we try to access contacts.
			 */

			Logger.e("ContactUtils", "Exception while querying contacts to name projection", e);
		}
		finally
		{
			if (contacts != null)
			{
				contacts.close();
			}
		}

		Cursor phones = null;

		try
		{
			phones = ctx.getContentResolver().query(Phone.CONTENT_URI, new String[] { Phone.CONTACT_ID, Phone.NUMBER }, null, null, null);
			/*
			 * Added this check for an issue where the cursor is null in some random cases (We suspect that happens when hotmail contacts are synced.)
			 */
			if (phones == null)
			{
				return null;
			}

			int numberColIdx = phones.getColumnIndex(Phone.NUMBER);
			int idColIdx = phones.getColumnIndex(Phone.CONTACT_ID);

			while (phones.moveToNext())
			{
				String number = phones.getString(numberColIdx);
				String id = phones.getString(idColIdx);
				String name = contactNames.get(id);
				if ((name != null) && (number != null))
				{
					if (contactsToStore.add("_" + name + "_" + number)) // if
																		// this
																		// element
																		// is
																		// added
																		// successfully
																		// , it
																		// returns
																		// true
					{
						contactinfos.add(new ContactInfo(id, null, name, number));
					}
				}
			}
		}
		catch (SecurityException e)
		{
			/**
			 * similar reason here as above security exception.
			 */

			Logger.e("ContactUtils", "Exception while querying phone numbers", e);
		}
		finally
		{
			if (phones != null)
			{
				phones.close();

			}
		}

		/*
		 * We will catch exceptions here since we do not know which devices support this URI.
		 */
		Cursor cursorSim = null;
		try
		{
			Uri simUri = Uri.parse("content://icc/adn");
			cursorSim = ctx.getContentResolver().query(simUri, null, null, null, null);
			if(cursorSim != null){
				while (cursorSim.moveToNext())
				{
					try
					{
						String id = cursorSim.getString(cursorSim.getColumnIndex("_id"));
						String name = cursorSim.getString(cursorSim.getColumnIndex("name"));
						String number = cursorSim.getString(cursorSim.getColumnIndex("number"));
						if ((name != null) && (number != null))
						{
							if (contactsToStore.add("_" + name + "_" + number)) // if
																				// this
																				// element
																				// is
																				// added
																				// successfully
																				// ,
																				// it
																				// returns
																				// true
							{
								contactinfos.add(new ContactInfo(id, null, name, number));
							}
						}
					}
					catch (Exception e)
					{
						Logger.w("ContactUtils", "Expection while adding sim contacts", e);
					}
				}
			}
		}
		catch (SecurityException e)
		{
			/**
			 * similar reason here as above security exception.
			 */

			Logger.e("ContactUtils", "Exception while querying sim contacts", e);
		}
		catch (Exception e)
		{
			Logger.w("ContactUtils", "Expection while querying for sim contacts", e);
		}
		finally
		{
			if (cursorSim != null)
			{
				cursorSim.close();
			}
		}

		return contactinfos;
	}

	/**
	 * This method is used to read the contacts from the phone's address book and used during contact sync up
	 * 
	 * @param ctx
	 * @return
	 */
	public List<ContactInfo> getContacts(Context ctx)
	{
		HashSet<String> contactsToStore = new HashSet<String>();
		String[] projection = new String[] { ContactsContract.Contacts._ID, ContactsContract.Contacts.HAS_PHONE_NUMBER, ContactsContract.Contacts.DISPLAY_NAME
				, ContactsContract.Contacts.IN_VISIBLE_GROUP};

		/*
		 * Added check for visibility of contact i.e. IN_VISIBLE_GROUP
		 */
		String selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + "='1'" + " AND " + ContactsContract.Contacts.IN_VISIBLE_GROUP + "='1'";
		Cursor contacts = null;

		List<ContactInfo> contactinfos = new ArrayList<ContactInfo>();
		Map<String, String> contactNames = new HashMap<String, String>();
		HashMap<String, ContactInfo> readContacts = new HashMap<String, ContactInfo>();
		try
		{
			contacts = ctx.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, projection, selection, null, null);

			/*
			 * Added this check for an issue where the cursor is null in some random cases (We suspect that happens when hotmail contacts are synced.)
			 */
			if (contacts == null)
			{
				return null;
			}

			int idFieldColumnIndex = contacts.getColumnIndex(ContactsContract.Contacts._ID);
			int nameFieldColumnIndex = contacts.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
			Logger.d("ContactUtils", "Starting to scan address book");
			while (contacts.moveToNext())
			{
				String id = contacts.getString(idFieldColumnIndex);
				String name = contacts.getString(nameFieldColumnIndex);
				contactNames.put(id, name);
			}
		}
		catch (SecurityException e)
		{
			/**
			 * currently some one plus one users are getting this exception on 5.1. we haven't found
			 * any particular reason how it can happen. putting a catch block for the same.
			 *
			 * In future, when our target sdk version is 23 and user denies contacts permission
			 * to hike. then we would get a security exception here, if we try to access contacts.
			 */

			Logger.e("ContactUtils", "Exception while querying contacts to name projection", e);
		}
		finally
		{
			if (contacts != null)
			{
				contacts.close();
			}
		}

		Cursor phones = null;

		try
		{
			phones = ctx.getContentResolver().query(Phone.CONTENT_URI, new String[] { Phone.CONTACT_ID, Phone.NUMBER }, null, null, null);
			/*
			 * Added this check for an issue where the cursor is null in some random cases (We suspect that happens when hotmail contacts are synced.)
			 */
			if (phones == null)
			{
				return null;
			}

			int numberColIdx = phones.getColumnIndex(Phone.NUMBER);
			int idColIdx = phones.getColumnIndex(Phone.CONTACT_ID);

			while (phones.moveToNext())
			{
				String number = phones.getString(numberColIdx);
				String id = phones.getString(idColIdx);
				String name = contactNames.get(id);
				if ((name != null) && (number != null))
				{
					if (contactsToStore.add("_" + name + "_" + number)) // if
																		// this
																		// element
																		// is
																		// added
																		// successfully
																		// , it
																		// returns
																		// true
					{
						ContactInfo cInfo = new ContactInfo(id, null, name, number);
						contactinfos.add(cInfo);
						readContacts.put(id, cInfo);
					}
				}
			}
		}
		catch (SecurityException e)
		{
			/**
			 * similar reason here as above security exception.
			 */

			Logger.e("ContactUtils", "Exception while querying phone numbers", e);
		}
		finally
		{
			if (phones != null)
			{
				phones.close();

			}
		}

		/*
		 * RawContacts table query for deleted flag to remove the deleted contact from the list.
		 */
		Cursor rawContacts = null;
		try
		{
			rawContacts = ctx.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, 
					new String[] {ContactsContract.RawContacts.CONTACT_ID, ContactsContract.RawContacts.DELETED}, null, null, null);

			/*
			 * Added this check for an issue where the cursor is null in some random cases (We suspect that happens when hotmail contacts are synced.)
			 */
			if (rawContacts == null)
			{
				return null;
			}

			int idFieldColumnIndex = rawContacts.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID);
			int deleteFieldColumnIndex = rawContacts.getColumnIndex(ContactsContract.RawContacts.DELETED);
			while (rawContacts.moveToNext())
			{
				String id = rawContacts.getString(idFieldColumnIndex);
				String deleted = rawContacts.getString(deleteFieldColumnIndex);
				if(!TextUtils.isEmpty(deleted) && deleted.equals("1"))
				{
					ContactInfo contactInfo = readContacts.get(id);
					contactinfos.remove(contactInfo);
				}
			}
		}
		catch (SecurityException e)
		{
			/**
			 * currently some one plus one users are getting this exception on 5.1. we haven't found
			 * any particular reason how it can happen. putting a catch block for the same.
			 *
			 * In future, when our target sdk version is 23 and user denies contacts permission
			 * to hike. then we would get a security exception here, if we try to access contacts.
			 */

			Logger.e("ContactUtils", "Exception while querying contacts from raw contacts table", e);
		}
		finally
		{
			if (rawContacts != null)
			{
				rawContacts.close();
			}
		}

		/*
		 * We will catch exceptions here since we do not know which devices support this URI.
		 */
		Cursor cursorSim = null;
		try
		{
			Uri simUri = Uri.parse("content://icc/adn");
			cursorSim = ctx.getContentResolver().query(simUri, null, null, null, null);
			if(cursorSim != null){
				while (cursorSim.moveToNext())
				{
					try
					{
						String id = cursorSim.getString(cursorSim.getColumnIndex("_id"));
						String name = cursorSim.getString(cursorSim.getColumnIndex("name"));
						String number = cursorSim.getString(cursorSim.getColumnIndex("number"));
						if ((name != null) && (number != null))
						{
							if (contactsToStore.add("_" + name + "_" + number)) // if
																				// this
																				// element
																				// is
																				// added
																				// successfully
																				// ,
																				// it
																				// returns
																				// true
							{
								contactinfos.add(new ContactInfo(id, null, name, number));
							}
						}
					}
					catch (Exception e)
					{
						Logger.w("ContactUtils", "Expection while adding sim contacts", e);
					}
				}
			}
		}
		catch (SecurityException e)
		{
			/**
			 * similar reason here as above security exception.
			 */

			Logger.e("ContactUtils", "Exception while querying sim contacts", e);
		}
		catch (Exception e)
		{
			Logger.w("ContactUtils", "Expection while querying for sim contacts", e);
		}
		finally
		{
			if (cursorSim != null)
			{
				cursorSim.close();
			}
		}

		return contactinfos;
	}

	/**
	 * This method updates the hike status of a contact in memory (if currently loaded in memory) and as well as in the database
	 * 
	 * @param ctx
	 * @param msisdn
	 * @param onhike
	 * @return
	 */
	public int updateHikeStatus(Context ctx, String msisdn, boolean onhike)
	{
		return updateHikeStatus(msisdn, onhike);
	}

	/**
	 * Used to get the recent contacts where we get the recency from the android contacts table. This method also returns a string which can be used as the argument to a SELECT IN
	 * query
	 * 
	 * @param context
	 * @param limit
	 * @return
	 */
	public Pair<String, Map<String, Long>> getRecentNumbers(Context context, int limit)
	{
		Cursor c = null;
		Map<String, Long> recentlyContactedNumbers = new HashMap<String, Long>();
		StringBuilder sb = new StringBuilder("");
		try
		{
			String sortBy = limit > -1 ? Phone.LAST_TIME_CONTACTED + " DESC LIMIT " + limit : null;
			c = context.getContentResolver().query(Phone.CONTENT_URI, new String[] { Phone.NUMBER, Phone.LAST_TIME_CONTACTED }, null, null, sortBy);

			if (c != null && c.getCount() > 0)
			{
				int numberColIdx = c.getColumnIndex(Phone.NUMBER);
				int lastTimeContactedIdx = c.getColumnIndex(Phone.LAST_TIME_CONTACTED);

				sb = new StringBuilder("(");
				while (c.moveToNext())
				{
					String number = c.getString(numberColIdx);

					if (TextUtils.isEmpty(number))
					{
						continue;
					}

					long lastTimeContacted = c.getLong(lastTimeContactedIdx);

					/*
					 * Checking if we already have this number and whether the last time contacted was sooner than the newer value.
					 */
					if (recentlyContactedNumbers.containsKey(number) && recentlyContactedNumbers.get(number) > lastTimeContacted)
					{
						continue;
					}
					recentlyContactedNumbers.put(number, c.getLong(lastTimeContactedIdx));

					number = DatabaseUtils.sqlEscapeString(number);
					sb.append(number + ",");
				}
				sb.replace(sb.length() - 1, sb.length(), ")");
			}
			else
			{
				sb = new StringBuilder("()");
			}


		}
		catch (SecurityException e)
		{
			Logger.e("ContactUtils", "Exception getRecentNumbers", e);
		}
		finally
		{
			if (c != null)
			{
				c.close();
			}
		}

		return new Pair<String, Map<String, Long>>(sb.toString(), recentlyContactedNumbers);
	}

	/**
	 * This method will give us the user's most contacted contacts. We also try to get the greenblue contacts if the user has them synced and then sort those based on times
	 * contacts.
	 * 
	 * @param context
	 * @param limit
	 * @return
	 */
	public Pair<String, Map<String, Integer>> getMostContactedContacts(Context context, int limit)
	{
		Cursor greenblueContactsCursor = null;
		Cursor phoneContactsCursor = null;
		Cursor otherContactsCursor = null;

		Map<String, Integer> mostContactedNumbers = new HashMap<String, Integer>();
		StringBuilder sb = null;
		try
		{
			String[] projection = new String[] { ContactsContract.RawContacts.CONTACT_ID };

			String selection = ContactsContract.RawContacts.ACCOUNT_TYPE + "= 'com.whatsapp'";
			greenblueContactsCursor = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, projection, selection, null, null);
			
			StringBuilder greenblueContactIds = null;
			if(greenblueContactsCursor != null)
			{
				int id = greenblueContactsCursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID);
				if (greenblueContactsCursor.getCount() > 0)
				{
					greenblueContactIds = new StringBuilder("(");
	
					while (greenblueContactsCursor.moveToNext())
					{
						greenblueContactIds.append(greenblueContactsCursor.getInt(id) + ",");
						String msisdn = greenblueContactsCursor.getInt(id) + "";
						if (isIndianMobileNumber(msisdn))
						{
							greenblueContactIds.append(msisdn + ",");
						}
					}
					greenblueContactIds.replace(greenblueContactIds.lastIndexOf(","), greenblueContactIds.length(), ")");
				}
			}

			String[] newProjection = new String[] { Phone.NUMBER, Phone.TIMES_CONTACTED };
			String newSelection = greenblueContactIds != null ? (Phone.CONTACT_ID + " IN " + greenblueContactIds.toString()) : null;

			phoneContactsCursor = context.getContentResolver().query(Phone.CONTENT_URI, newProjection, newSelection, null, Phone.TIMES_CONTACTED + " DESC LIMIT " + limit);

			if ((phoneContactsCursor != null) && (phoneContactsCursor.getCount() > 0))
			{
				sb = new StringBuilder("(");

				extractContactInfo(phoneContactsCursor, sb, mostContactedNumbers, true);

			}
			/*
			 * This number is required when the user does not have enough greenblue contacts.
			 */
			int otherContactsRequired = limit - mostContactedNumbers.size();

			if (otherContactsRequired > 0)
			{
				if (greenblueContactIds != null)
				{
					newSelection = Phone.CONTACT_ID + " NOT IN " + greenblueContactIds.toString();
				}
				else
				{
					newSelection = null;
				}

				otherContactsCursor = context.getContentResolver().query(Phone.CONTENT_URI, newProjection, newSelection, null,
						Phone.TIMES_CONTACTED + " DESC LIMIT " + otherContactsRequired);

				if ((otherContactsCursor != null) && (otherContactsCursor.getCount() > 0))
				{
					if (sb == null)
					{
						sb = new StringBuilder("(");
					}
					extractContactInfo(otherContactsCursor, sb, mostContactedNumbers, false);
				}
			}

			if (sb == null || mostContactedNumbers.isEmpty())
			{
				sb = new StringBuilder("()");
			}
			else
			{
				sb.replace(sb.length() - 1, sb.length(), ")");
			}

		}
		catch (SecurityException e)
		{
			Logger.e("ContactUtils", "Exception getMostContactedContacts", e);
		}
		finally
		{
			if (greenblueContactsCursor != null)
			{
				greenblueContactsCursor.close();
			}
			if (phoneContactsCursor != null)
			{
				phoneContactsCursor.close();
			}
			if (otherContactsCursor != null)
			{
				otherContactsCursor.close();
			}
		}
		return new Pair<String, Map<String, Integer>>(sb.toString(), mostContactedNumbers);
	}

	public List<ContactInfo> getAllConversationContactsSorted(boolean removeNewOrReturningUsers, boolean fetchGroups)
	{
		List<ContactInfo> allContacts = new ArrayList<ContactInfo>();
		List<ContactInfo> oneToOneContacts = HikeConversationsDatabase.getInstance().getOneToOneContacts(removeNewOrReturningUsers);
		allContacts.addAll(oneToOneContacts);
		if(fetchGroups)
		{
			allContacts.addAll(getConversationGroupsAsContacts(false));
		}

		/*
		 * Sort in descending order of timestamp.
		 */
		Collections.sort(allContacts, new Comparator<ContactInfo>()
		{
			@Override
			public int compare(ContactInfo lhs, ContactInfo  rhs)
			{
				return ((Long)rhs.getLastMessaged()).compareTo(lhs.getLastMessaged());
			}
		});
		return allContacts;
	}

	public List<ContactInfo> getConversationGroupsAsContacts(boolean shouldSort)
	{
		return getConversationGroupsAsContacts(shouldSort,true);
	}
	public List<ContactInfo> getConversationGroupsAsContacts(boolean shouldSort,boolean fetchGroupCount)
	{
		Map<String, Integer> groupCountMap=null;
		List<GroupDetails> groupDetails = persistenceCache.getGroupDetailsList();
		List<ContactInfo> groupContacts = new ArrayList<ContactInfo>();
		if(fetchGroupCount) {
			groupCountMap = HikeConversationsDatabase.getInstance().getAllGroupsActiveParticipantCount();
		}
		for(GroupDetails group : groupDetails)
		{
			if(group.isGroupAlive())
			{
				if (!OneToNConversationUtils.isBroadcastConversation(group.getGroupId()))
				{
					int numMembers = 0;
					if(groupCountMap!=null && groupCountMap.containsKey(group.getGroupId()))
					{
						numMembers = groupCountMap.get(group.getGroupId());
					}
					String numberMembers = context.getString(R.string.num_people, (numMembers + 1));

					ContactInfo groupContact = new ContactInfo(group.getGroupId(), group.getGroupId(), group.getGroupName(), numberMembers, true);
					groupContact.setLastMessaged(group.getTimestamp());

					groupContacts.add(groupContact);
				}
			}
		}
		if(shouldSort)
		{
			Collections.sort(groupContacts, new Comparator<ContactInfo>()
			{
				@Override
				public int compare(ContactInfo lhs, ContactInfo  rhs)
				{
					return ((Long)rhs.getLastMessaged()).compareTo(lhs.getLastMessaged());
				}
			});
		}

		return groupContacts;
	}

	public boolean isIndianMobileNumber(String number)
	{
		if (HikeMessengerApp.isIndianUser())
		{
			Pattern pattern = Pattern.compile("^(?:(?:\\+|0{0,2})91(\\s*[\\-]\\s*)?|[0]?)?[789]\\d{9}$");
			Matcher matcher = pattern.matcher(number);
			if (matcher.matches())
				return true;
		}
		else
		{
			return true;
		}

		return false;
	}

	private void extractContactInfo(Cursor c, StringBuilder sb, Map<String, Integer> numbers, boolean greenblueContacts)
	{
		int numberColIdx = c.getColumnIndex(Phone.NUMBER);
		int timesContactedIdx = c.getColumnIndex(Phone.TIMES_CONTACTED);

		while (c.moveToNext())
		{
			String number = c.getString(numberColIdx);

			if (TextUtils.isEmpty(number))
			{
				continue;
			}
			if (isIndianMobileNumber(number))
			{

				/*
				 * We apply a multiplier of 2 for greenblue contacts to give them a greater weight.
				 */
				int lastTimeContacted = greenblueContacts ? 2 * c.getInt(timesContactedIdx) : c.getInt(timesContactedIdx);

				/*
				 * Checking if we already have this number and whether the last time contacted was sooner than the newer value.
				 */
				if (numbers.containsKey(number) && numbers.get(number) > lastTimeContacted)
				{
					continue;
				}
				numbers.put(number, lastTimeContacted);

				number = DatabaseUtils.sqlEscapeString(number);
				sb.append(number + ",");
			}
		}
	}

	/**
	 * This method updates the parameter <code>onGreenBlue</code> of {@link ContactInfo} , sets it to true for contacts which are also on WA
	 * 
	 * @param context
	 * @param contactinfos
	 */
	public void setGreenBlueStatus(Context context, List<ContactInfo> contactinfos)
	{
		Cursor greenblueContactsCursor = null;
		Cursor phoneContactsCursor = null;
		try
		{
			String[] projection = new String[] { ContactsContract.RawContacts.CONTACT_ID };

			String selection = ContactsContract.RawContacts.ACCOUNT_TYPE + "= 'com.whatsapp'";
			greenblueContactsCursor = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, projection, selection, null, null);

			/*
			 * We were getting this cursor as null for some reason (saw crashes on the dev console).
			 */
			if (greenblueContactsCursor == null)
			{
				return;
			}

			int id = greenblueContactsCursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID);

			StringBuilder greenblueContactIds = null;
			if (greenblueContactsCursor.getCount() > 0)
			{
				greenblueContactIds = new StringBuilder("(");

				while (greenblueContactsCursor.moveToNext())
				{
					greenblueContactIds.append(greenblueContactsCursor.getInt(id) + ",");
				}
				greenblueContactIds.replace(greenblueContactIds.lastIndexOf(","), greenblueContactIds.length(), ")");
			}

			if (greenblueContactIds != null)
			{
				String[] newProjection = new String[] { Phone.NUMBER, Phone.DISPLAY_NAME };
				String newSelection = (Phone.CONTACT_ID + " IN " + greenblueContactIds.toString());

				phoneContactsCursor = context.getContentResolver().query(Phone.CONTENT_URI, newProjection, newSelection, null, Phone.NUMBER + " DESC");

				if ((phoneContactsCursor != null) && (phoneContactsCursor.getCount() > 0))
				{
					setGreenBlueContacs(phoneContactsCursor, contactinfos);
				}
			}

		}
		catch (SecurityException e)
		{
			Logger.e("ContactUtils", "Exception setGreenBlueStatus", e);
		}
		finally
		{
			if (greenblueContactsCursor != null)
			{
				greenblueContactsCursor.close();
			}
			if (phoneContactsCursor != null)
			{
				phoneContactsCursor.close();
			}
		}
	}

	private void setGreenBlueContacs(Cursor c, List<ContactInfo> contactinfos)
	{
		int numberColIdx = c.getColumnIndex(Phone.NUMBER);
		HashSet<String> greenBlueContacts = new HashSet<String>(c.getCount());
		while (c.moveToNext())
		{
			String number = c.getString(numberColIdx);
			greenBlueContacts.add(number);
		}

		for (ContactInfo contact : contactinfos)
		{
			if (greenBlueContacts.contains(contact.getPhoneNum()))
			{
				contact.setOnGreenBlue(true);
			}
		}
	}

	public ArrayList<String> getMsisdnFromId(ArrayList<String> selectionArgs)
	{
		String msisdnStatement = PhoneUtils.getMsisdnStatement(selectionArgs);
		if (TextUtils.isEmpty(msisdnStatement))
		{
			return new ArrayList<String>();
		}
		Cursor c = getReadableDatabase().query(DBConstants.USERS_TABLE, new String[] { DBConstants.MSISDN },
				DBConstants.PLATFORM_USER_ID + " IN " + msisdnStatement, null, null, null, null);

		ArrayList<String> msisdnList = new ArrayList<String>();

		if (c.moveToFirst())
		{
			do
			{
				msisdnList.add(c.getString(c.getColumnIndexOrThrow(DBConstants.MSISDN)));
			}
			while (c.moveToNext());
		}

		// Incase of hike id == platform uid for user, add self msisdn
		for (String id : selectionArgs)
		{
			if (id.equals(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_UID_SETTING, null)))
			{
				ContactInfo userContact = Utils.getUserContactInfo(context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE));
				msisdnList.add(userContact.getMsisdn());
			}
		}

		return msisdnList;
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.APP_BACKGROUNDED.equals(type))
		{
			/*
			 * Clearing transient cache when app is backgroubded
			 */
			unload();
		}
	}

	/**
	 * Shutdowns the contact manager and re-initializes it with new values in db {@see #shutdown()}
	 */
	public void refreshContactManager()
	{
		// clear persistence, transient cache and assign null to all other class members
		shutdown();
		// create a new instance and initialize it
		getInstance();
	}

	/**
	 * Shutdown's both {@link PersistenceCache} and {@link TransientCache} and make all references to null
	 */
	public void shutdown()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		persistenceCache.shutdown();
		persistenceCache = null;
		transientCache.shutdown();
		transientCache = null;
		hDb = null;
		context = null;
		_instance = null;
	}

	public void platformUserIdEntry(JSONArray data)
	{
		hDb.platformUserIdDbEntry(data);
	}

	public ArrayList<String> getMsisdnForMissingPlatformUID()
	{
		return hDb.getMsisdnsForMissingPlatformUID();
	}

	
	public String getSelfMsisdn()
	{
		return selfMsisdn;
	}

	public void setSelfMsisdn(String msisdn)
	{
		this.selfMsisdn = msisdn;
	}


	public void setParticipantAdmin(String groupId, String msisdn) {
		transientCache.updateGroupParticipantDetail(groupId, msisdn);
	}
	
	public void updateAdminState(String msisdn) {
		transientCache.updateContactDetailInAllGroups(msisdn);
	}
	
	/**
	 * Returns the path of the user DP thumbnail. eg - file:///data/data/com.bsb.hike/cache/+91112233456.jpg
	 * @param msisdn
	 * @return
	 */
	public String getImagePathForThumbnail(String msisdn)
	{
		return hDb.getImagePathForThumbnail(msisdn);
	}

	public void updateCallerTable(CallerContentModel callerContentModel, boolean setIsBlock)
	{
		HikeUserDatabase.getInstance().updateCallerTable(callerContentModel, setIsBlock);
	}

	public void insertIntoCallerTable(CallerContentModel callerContentModel, boolean isCompleteData, boolean setIsBlock)
	{
		HikeUserDatabase.getInstance().insertIntoCallerTable(callerContentModel, isCompleteData, setIsBlock);
	}

	public void insertIntoCallerTable(CallerContentModel callerContentModel, boolean isCompleteData, boolean setIsBlock, long creationTime)
	{
		HikeUserDatabase.getInstance().insertIntoCallerTable(callerContentModel, isCompleteData, setIsBlock, creationTime);
	}

	public void updateMdIntoCallerTable(CallerContentModel callerContentModel)
	{
		HikeUserDatabase.getInstance().updateMdIntoCallerTable(callerContentModel);
	}

	public CallerContentModel getCallerContentModelFromMsisdn(String msisdn)
	{
		return HikeUserDatabase.getInstance().getCallerContentModelFromMsisdn(msisdn);
	}

	public Cursor getCallerBlockContactCursor()
	{
		return HikeUserDatabase.getInstance().getCallerBlockContactCursor();
	}

	public int updateBlockStatusIntoCallerTable(String msisdn, ContentValues contentValues) {
		return HikeUserDatabase.getInstance().updateBlockStatusIntoCallerTable(msisdn, contentValues);
	}

	public Cursor getAllUnsyncedContactCursor() {
		return HikeUserDatabase.getInstance().getAllUnsyncedContactCursor();
	}

	public void updateCallerSyncStatus(JSONObject callerSyncJSON)
	{
		HikeUserDatabase.getInstance().updateCallerSyncStatus(callerSyncJSON);
	}

	public void insertAllBlockedContactsIntoCallerTable(ArrayList<CallerContentModel> callerContent)
	{
		HikeUserDatabase.getInstance().insertAllBlockedContactsIntoCallerTable(callerContent);
	}

	public boolean isMyMsisdn(String outsideMsisdn)
	{
		return selfMsisdn.equals(outsideMsisdn);
	}

	public void toggleChatSpam(String msisdn, int markSpam) {
		HikeUserDatabase.getInstance().toggleChatSpamUser(msisdn, markSpam);
	}

	/**
	 * From now on we classify a friend as :
	 * 1. The person whom I have added as a friend. Irrespective of the status of the request at the other end
	 *
	 * @return
	 */
	public boolean isOneWayFriend(String msidn)
	{
		FavoriteType favoriteType = getFriendshipStatus(msidn);
		return (favoriteType == FavoriteType.REQUEST_SENT ||
				favoriteType == FavoriteType.REQUEST_SENT_REJECTED ||
				isTwoWayFriend(msidn));
	}

	/**
	 * 2 Way friend works if a user added someone as a friend and the other person also added the user as a friend
	 *
	 * @return
	 */
	public boolean isTwoWayFriend(String msisdn)
	{
		return getFriendshipStatus(msisdn) == FavoriteType.FRIEND;
	}
}

