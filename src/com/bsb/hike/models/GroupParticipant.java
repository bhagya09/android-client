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

	private ContactInfo contactInfo;

	public GroupParticipant(ContactInfo contactInfo)
	{
		this(contactInfo, false, false, false);
	}
	public GroupParticipant(ContactInfo contactInfo,boolean isAdmin)
	{
		this(contactInfo, false, false, isAdmin);
	}

	public GroupParticipant(ContactInfo contactInfo, boolean hasLeft, boolean onDnd, boolean isAdmin)
	{
		this.contactInfo = contactInfo;
		this.hasLeft = hasLeft;
		this.onDnd = onDnd;
		this.isAdmin = isAdmin;
	}

	public void setOnDnd(boolean onDnd)
	{
		this.onDnd = onDnd;
	}

	public boolean onDnd()
	{
		return onDnd;
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

	public boolean isAdmin() {
		return isAdmin;
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
}
