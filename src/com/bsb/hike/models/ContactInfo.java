package com.bsb.hike.models;

import android.text.TextUtils;

import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.models.utils.JSONSerializable;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.tasks.GetHikeJoinTimeTask;
import com.bsb.hike.utils.LastSeenComparator;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ContactInfo implements JSONSerializable, Comparable<ContactInfo>
{
	public boolean getBlockedStatus() {
		return isBlocked;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public enum FavoriteType
	{
		NOT_FRIEND, REQUEST_RECEIVED, FRIEND, AUTO_RECOMMENDED_FAVORITE, REQUEST_SENT, REQUEST_SENT_REJECTED, REQUEST_RECEIVED_REJECTED
	}

	@Override
	public String toString()
	{
		return "ContactInfo [id = " + id + " , name=" + name + ", msisdn=" + msisdn +  ", rawNum =" + phoneNum + "]";
	}

	private String name;

	private String msisdn;

	private String id;

	private short bits = 0;

	private boolean isBlocked = false;

	private String uid = null;

	/*
	 * bits 1111
	 * 
	 * bit index 3210
	 */

	/*
	 * index value
	 * 
	 * 0 onhike
	 * 
	 * 1 hasCustomPhoto
	 * 
	 * 2 onGreenBlue
	 * 
	 * 3 FavType
	 * 
	 * 4 FavType
	 * 
	 * 5 FavType
	 * 
	 * 6 offline
	 * 
	 * 7 offline
	 * 
	 * FavouriteType has seven values so taking three bits
	 * 
	 * offline value can be -1, 0 , 1 so taking two bits
	 */

	private String msisdnType;

	private long lastMessaged;

	private String phoneNum;

	private long hikeJoinTime;

	private long lastSeenTime;

	private long inviteTime;

	private String platformId;

	private PrivacyPreferences mPrivacyPrefs = new PrivacyPreferences(PrivacyPreferences.DEFAULT_VALUE);

	/**
	 * Returns true if bit at index is 1 otherwise false
	 * 
	 * @param index
	 * @throws IndexOutOfBoundsException
	 */
	private boolean get(int index)
	{
		if (index < 0 || index >= 16)
			throw new IndexOutOfBoundsException("Using short so value of index can be between 0 and 15 inclusive");
		return ((bits & (1 << index)) != 0);
	}

	/**
	 * Sets the bit at index to 1
	 * 
	 * @param index
	 * @throws IndexOutOfBoundsException
	 */
	private void set(int index)
	{
		if (index < 0 || index >= 16)
			throw new IndexOutOfBoundsException("Using short so value of index can be between 0 and 15 inclusive");
		bits = (short) (bits | (1 << index));
	}

	/**
	 * Sets the bit at index to 0
	 * 
	 * @param index
	 * @throws IndexOutOfBoundsException
	 */
	private void clear(int index)
	{
		if (index < 0 || index >= 16)
			throw new IndexOutOfBoundsException("Using short so value of index can be between 0 and 15 inclusive");
		bits = (short) (bits & ~(1 << index));
	}

	/**
	 * Sets the bit at index to 1 if val is true and 0 if val is false
	 * 
	 * @param index
	 * @param val
	 */
	private void set(int index, boolean val)
	{
		if (val)
			set(index);
		else
			clear(index);
	}

	/**
	 * Sets the bits from fromIndex to toIndex with binary of num . The bit at toIndex is least significant bit of num and the bit at fromindex is most significant bit.
	 * 
	 * @param fromIndex
	 * @param toIndex
	 * @param num
	 * @throws IllegalArgumentException
	 * @throws IndexOutOfBoundsException
	 */
	private void setNum(int fromIndex, int toIndex, int num)
	{
		if (toIndex < fromIndex)
			throw new IllegalArgumentException("Starting index should be less than or equal to ending index");
		if (fromIndex < 0 || toIndex >= 16)
			throw new IndexOutOfBoundsException("Value of index can be between 0 and 15 inclusive");
		while (num > 0)
		{
			if (toIndex < fromIndex)
				throw new IllegalArgumentException("Number of bits required for num is greater than number of bits we set(to-from+1)");
			set(toIndex, (num & 1) != 0);
			num = num >> 1;
			toIndex--;
		}
		if (toIndex >= fromIndex)
			clear(fromIndex, toIndex);
	}

	/**
	 * Sets all the bits from fromIndex to toIndex to 0
	 * 
	 * @param fromIndex
	 * @param toIndex
	 * @throws IllegalArgumentException
	 * @throws IndexOutOfBoundsException
	 */
	private void clear(int fromIndex, int toIndex)
	{
		if (toIndex < fromIndex)
			throw new IllegalArgumentException("Starting index should be less than or equal to ending index");
		if (fromIndex < 0 || toIndex >= 16)
			throw new IndexOutOfBoundsException("Value of index can be between 0 and 15 inclusive");
		if (fromIndex == toIndex)
			clear(fromIndex);
		else
		{
			int n = 0;
			for (int i = fromIndex; i <= toIndex; ++i)
			{
				n = n << 1;
				n += 1;
			}
			n = n << fromIndex;
			bits = (short) (bits & ~n);
		}
	}

	/**
	 * Returns the integer represented by bits from fromIndex to toIndex with least significant bit as toIndex and most significant bit as fromIndex
	 * 
	 * @param fromIndex
	 * @param toIndex
	 * @throws IllegalArgumentException
	 * @throws IndexOutOfBoundsException
	 */
	private int getNum(int fromIndex, int toIndex)
	{
		if (toIndex < fromIndex)
			throw new IllegalArgumentException("Starting index should be less than or equal to ending index");
		if (fromIndex < 0 || toIndex >= 16)
			throw new IndexOutOfBoundsException("Value of index can be between 0 and 15 inclusive");
		if (toIndex == fromIndex)
			return (get(fromIndex)) ? 1 : 0;
		else
		{
			int res = 0, x = 1;
			for (int i = toIndex; i >= fromIndex; --i)
			{
				if (get(i))
					res += x;
				x = x << 1;
			}
			return res;
		}
	}

	public String getPlatformId()
	{
		return platformId;
	}

	public boolean isPlatformUIDExist()
	{
		return !TextUtils.isEmpty(platformId);
	}

	public String getName()
	{
		return name;
	}

	public String getNameOrMsisdn()
	{
		return name != null ? name : msisdn;
	}

	public String getFirstName()
	{
		if (TextUtils.isEmpty(name))
		{
			return this.msisdn;
		}

		return Utils.getFirstName(this.name);
	}
    
	public String getFirstNameAndSurname()
	{
		if(name==null || TextUtils.isEmpty(name))
		{
			return this.msisdn;
		}
		
		return Utils.getFirstNameAndSurname(this.name);
	}

	public void setPlatformId(String platformId)
	{
		this.platformId = "";
		if (!TextUtils.isEmpty(platformId) && !platformId.equals("null"))
		{
			this.platformId = platformId;
		}

	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getMsisdn()
	{
		return msisdn;
	}

	public void setMsisdn(String msisdn)
	{
		this.msisdn = msisdn;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public boolean isOnhike()
	{
		return get(0);
	}

	public void setOnhike(boolean onhike)
	{
		set(0, onhike);
	}

	public String getPhoneNum()
	{
		return phoneNum;
	}

	public void setPhoneNum(String phoneNum)
	{
		this.phoneNum = phoneNum;
	}

	public boolean hasCustomPhoto()
	{
		return get(1);
	}

	public void setHasCustomPhoto(boolean hasCustomPhoto)
	{
		set(1, hasCustomPhoto);
	}

	public String getMsisdnType()
	{
		return msisdnType;
	}

	public void setMsisdnType(String msisdnType)
	{
		this.msisdnType = msisdnType;
	}

	public long getLastMessaged()
	{
		return lastMessaged;
	}

	public void setLastMessaged(long lastMessaged)
	{
		this.lastMessaged = lastMessaged;
	}

	public FavoriteType getFavoriteType()
	{
		int n = getNum(3, 5);
		if (7 == n)
			return null;
		return FavoriteType.values()[n];
	}

	private int getFavoriteTypeNumRepresentation()
	{
		return getNum(3, 5);
	}
	
	public void setFavoriteType(FavoriteType favoriteType)
	{
		int n = favoriteType.ordinal();
		setNum(3, 5, n);
	}

	public long getHikeJoinTime()
	{
		return hikeJoinTime;
	}

	public void setHikeJoinTime(long hikeJoinTime)
	{
		this.hikeJoinTime = hikeJoinTime;
	}

	public boolean isUnknownContact()
	{
		if (msisdn == null)
		{
			return false;
		}
		/*
		 * For unknown contacts, we make the id and msisdn equal.
		 */
		return TextUtils.isEmpty(id) ? true : msisdn.equals(id);
	}

	public boolean isGroupConversationContact()
	{
		if (phoneNum == null)
		{
			return false;
		}
		/*
		 * For group conversations, we make the phone number and id equal.
		 */
		return phoneNum.equals(id);
	}

	public long getLastSeenTime()
	{
		return lastSeenTime;
	}

	public void setLastSeenTime(long lastSeenTime)
	{
		this.lastSeenTime = lastSeenTime;
	}

	public int getOffline()
	{
		return getNum(6, 7) - 1;
	}

	public void setOffline(int offline)
	{
		setNum(6, 7, offline + 1);
	}

	public long getInviteTime()
	{
		return inviteTime;
	}

	public void setInviteTime(long inviteTime)
	{
		this.inviteTime = inviteTime;
	}

	public boolean isOnGreenBlue()
	{
		return get(2);
	}

	public void setOnGreenBlue(boolean onGreenBlue)
	{
		set(2, onGreenBlue);
	}

	public String getFormattedHikeJoinTime()
	{
		String format = "MMM ''yy";
		DateFormat df = new SimpleDateFormat(format);
		return df.format(new Date(hikeJoinTime * 1000));
	}

	public ContactInfo(String id, String msisdn, String name, String phoneNum)
	{
		this(id, msisdn, name, phoneNum, false, "", 0, false);
	}

	public ContactInfo(String id, String msisdn, String name, String phoneNum, boolean onHike)
	{
		this(id, msisdn, name, phoneNum, onHike, "", 0, false);
	}

	public ContactInfo(String id, String msisdn, String name, String phoneNum, boolean onhike, String msisdnType, long lastMessaged, boolean hasCustomPhoto)
	{
		this(id, msisdn, name, phoneNum, onhike, msisdnType, lastMessaged, hasCustomPhoto, 0);
	}

	public ContactInfo(String id, String msisdn, String name, String phoneNum, boolean onhike, String msisdnType, long lastMessaged, boolean hasCustomPhoto, long hikeJoinTime)
	{

		this(id, msisdn, name,phoneNum, onhike, msisdnType, lastMessaged, hasCustomPhoto, hikeJoinTime, "",null);
	}


	public ContactInfo(String id, String msisdn, String name, String phoneNum, boolean onhike, String platformId,String uid)
	{
		this(id, msisdn, name, phoneNum, onhike, "", 0, false, 0, platformId,uid);
	}

	public ContactInfo()
	{
		// Does nothing
	}

	public ContactInfo(ContactInfo contactInfo)
	{
		this(contactInfo.getId(), contactInfo.getMsisdn(), contactInfo.getName(), contactInfo.getPhoneNum(), contactInfo.isOnhike(), "", contactInfo.getLastMessaged(), contactInfo
				.hasCustomPhoto(), contactInfo.getHikeJoinTime(), contactInfo.getPlatformId(),contactInfo.getUid());
		setNum(3, 5, contactInfo.getFavoriteTypeNumRepresentation());
		this.inviteTime = contactInfo.getInviteTime();
		this.lastSeenTime = contactInfo.getLastSeenTime();
		setNum(6, 7, contactInfo.getOffline() + 1);
	}

	public String getUid() {
		return uid;
	}

	public ContactInfo(String id, String msisdn, String name, String phoneNum, boolean onhike, String msisdnType, long lastMessaged, boolean hasCustomPhoto, long hikeJoinTime, String platformId,String uid)
	{
		this.id = id;
		this.msisdn = msisdn;
		this.name = name;
		set(0, onhike);
		this.phoneNum = phoneNum;
		set(1, hasCustomPhoto);
		this.msisdnType = msisdnType;
		this.lastMessaged = lastMessaged;
		this.hikeJoinTime = hikeJoinTime;
		setNum(6, 7, 2);
		setNum(3, 5, 7);
		setPlatformId(platformId);
		this.uid = uid;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((TextUtils.isEmpty(name)) ? 0 : name.hashCode());
		result = prime * result + ((phoneNum == null) ? 0 : phoneNum.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContactInfo other = (ContactInfo) obj;
		if (id == null)
		{
			if (other.id != null)
				return false;
		}
		else if (!id.equals(other.id))
			return false;
		if (TextUtils.isEmpty(name))
		{
			if (!TextUtils.isEmpty(other.name))
				return false;
		}
		else if (!name.trim().equals(other.name.trim()))
			return false;
		if (phoneNum == null)
		{
			if (other.phoneNum != null)
				return false;
		}
		else if (!phoneNum.equals(other.phoneNum))
			return false;
		return true;
	}

	public JSONObject toJSON() throws JSONException
	{
		return toJSON(true);
	}
	
	public JSONObject toJSON(boolean msisdn) throws JSONException
	{
		JSONObject json = new JSONObject();
		if(msisdn)
		{
			json.put("phone_no", this.phoneNum);
		}
		json.put("name", this.name);
		json.put("id", this.id);
		json.put(HikePlatformConstants.PLATFORM_USER_ID,this.platformId);
		return json;
	}

	public JSONObject getPlatformInfo() throws JSONException
	{
		JSONObject json = new JSONObject();
		json.put("name", getNameOrMsisdn());
		json.put(HikePlatformConstants.PLATFORM_USER_ID,this.platformId);
        json.put(HikePlatformConstants.MSISDN, getMsisdn());
        return json;
	}

	public static LastSeenComparator lastSeenTimeComparator = new LastSeenComparator(true);

	public static LastSeenComparator lastSeenTimeComparatorWithoutFav = new LastSeenComparator(false);

	@Override
	public int compareTo(ContactInfo rhs)
	{
		if (rhs == null)
		{
			return -1;
		}
		if (TextUtils.isEmpty(this.name) && TextUtils.isEmpty(rhs.name))
		{
			return (this.msisdn.toLowerCase().compareTo(((ContactInfo) rhs).msisdn.toLowerCase()));
		}
		else if (TextUtils.isEmpty(this.name))
		{
			return 1;
		}
		else if (TextUtils.isEmpty(rhs.name))
		{
			return -1;
		}
		else if (this.name.startsWith("+") && !rhs.name.startsWith("+"))
		{
			return 1;
		}
		else if (!this.name.startsWith("+") && rhs.name.startsWith("+"))
		{
			return -1;
		}
		return (this.name.toLowerCase().compareTo(((ContactInfo) rhs).name.toLowerCase()));
	}

	public boolean isNotOrRejectedFavourite()
	{
		return (this.getFavoriteType() == FavoriteType.NOT_FRIEND
				|| this.getFavoriteType() == FavoriteType.REQUEST_SENT_REJECTED 
				|| this.getFavoriteType() == FavoriteType.REQUEST_RECEIVED_REJECTED);
	}
	

	public void httpGetHikeJoinTime()
	{
		GetHikeJoinTimeTask getHikeJoinTimeTask = new GetHikeJoinTimeTask(msisdn);
		getHikeJoinTimeTask.execute();
	}

	public boolean isBot()
	{
		return BotUtils.isBot(msisdn);
	}

	public boolean isBlocked()
	{
		return ContactManager.getInstance().isBlocked(msisdn);
	}

	/**
	 * From now on we classify a friend as :
	 * 1. The person whom I have added as a friend. Irrespective of the status of the request at the other end
	 *
	 * @return
	 */
	public boolean isMyOneWayFriend()
	{
		FavoriteType favoriteType = this.getFavoriteType();
		return (favoriteType == FavoriteType.REQUEST_SENT ||
				favoriteType == FavoriteType.REQUEST_SENT_REJECTED ||
				isMyTwoWayFriend());
	}

	/**
	 * 2 Way friend works if a user added someone as a friend and the other person also added the user as a friend
	 *
	 * @return
	 */
	public boolean isMyTwoWayFriend()
	{
		return this.getFavoriteType() == FavoriteType.FRIEND;
	}

	public boolean isFriendRequestReceivedForMe()
	{
		return this.getFavoriteType() == FavoriteType.REQUEST_RECEIVED;
	}

	public boolean isNotMyFriend()
	{
		return this.getFavoriteType() == FavoriteType.REQUEST_RECEIVED_REJECTED || this.getFavoriteType() == FavoriteType.NOT_FRIEND;
	}

	public void setBlockStatus(boolean status)
	{
		this.isBlocked = status;
	}

	public void toggleLastSeen()
	{
		mPrivacyPrefs.toggleLastSeen();
	}

	public void toggleStatusUpdate()
	{
		mPrivacyPrefs.toggleStatusUpdate();
	}

	public void setPrivacyPrefs(PrivacyPreferences prefs)
	{
		this.mPrivacyPrefs = prefs;
	}

	public PrivacyPreferences getPrivacyPrefs()
	{
		return mPrivacyPrefs;
	}

}
