package com.bsb.hike.models;

import java.util.Comparator;

import android.text.TextUtils;

import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.utils.PairModified;

public class GroupParticipant implements Comparable<GroupParticipant>
{
	private boolean hasLeft;

	private boolean onDnd;
	
	private boolean isAdmin;
	
	private String grpID;
	
	private int type;

	private ContactInfo contactInfo;

	public String getUid() {
		return contactInfo.getUid();
	}

	public static final class Participant_Type
	{
		public static final int MEMBER = 0;

		public static final int ADMIN = 1;

	}

	public GroupParticipant(ContactInfo contactInfo, String grpID)
	{
		this(contactInfo, false, false, 0,grpID);
	}
	public GroupParticipant(ContactInfo contactInfo,int type)
	{
		this(contactInfo, false, false, type, null);
	}

	public GroupParticipant(ContactInfo contactInfo, boolean hasLeft, boolean onDnd, int type, String grpId)
	{
		this.contactInfo = contactInfo;
		this.hasLeft = hasLeft;
		this.onDnd = onDnd;
		this.type = type;
		this.grpID = grpId;
	}

	public void setOnDnd(boolean onDnd)
	{
		this.onDnd = onDnd;
	}

	public boolean onDnd()
	{
		return onDnd;
	}
	
	public String getGrpID() {
		return grpID;
	}
	public void setGrpID(String grpID) {
		this.grpID = grpID;
	}

	public boolean hasLeft()
	{
		return hasLeft;
	}

	public void setHasLeft(boolean hasLeft)
	{
		this.hasLeft = hasLeft;
	}

	public ContactInfo getContactInfo()
	{
		return contactInfo;
	}

	public void setContactInfo(ContactInfo info)
	{
		contactInfo = info;
	}

	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}

	public boolean isAdmin() {
		if(type==Participant_Type.ADMIN){
			return true;
		}
		return false;
	}

	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
	@Override
	public int compareTo(GroupParticipant another)
	{
		return this.contactInfo.compareTo(another.contactInfo);
	}

	public static Comparator<PairModified<GroupParticipant, String>> lastSeenTimeComparator = new Comparator<PairModified<GroupParticipant, String>>()
	{

		@Override
		public int compare(PairModified<GroupParticipant, String> lhs, PairModified<GroupParticipant, String> rhs)
		{
			ContactInfo lhsContactInfo = lhs.getFirst().contactInfo;
			ContactInfo rhsContactInfo = rhs.getFirst().contactInfo;

			if (TextUtils.isEmpty(lhs.getSecond()) && TextUtils.isEmpty(rhs.getSecond()))
			{
				return (lhsContactInfo.getMsisdn().toLowerCase().compareTo(rhsContactInfo.getMsisdn().toLowerCase()));
			}
			else if (TextUtils.isEmpty(lhs.getSecond()))
			{
				return 1;
			}
			else if (TextUtils.isEmpty(rhs.getSecond()))
			{
				return -1;
			}
			else if (lhs.getSecond().startsWith("+") && !rhs.getSecond().startsWith("+"))
			{
				return 1;
			}
			else if (!lhs.getSecond().startsWith("+") && rhs.getSecond().startsWith("+"))
			{
				return -1;
			}
			return (lhs.getSecond().toLowerCase().compareTo(rhs.getSecond().toLowerCase()));
		}
	};

	public boolean isSMSGroupMember()
	{
		return !contactInfo.isOnhike();
	}
	
}
