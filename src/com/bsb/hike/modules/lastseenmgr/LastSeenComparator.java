package com.bsb.hike.modules.lastseenmgr;

import java.util.Comparator;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;

public class LastSeenComparator implements Comparator<ContactInfo>
{
	public boolean checkFavoriteType;;
	
	public LastSeenComparator(boolean checkFavoriteType)
	{
		this.checkFavoriteType = checkFavoriteType;
	}

	@Override
	public int compare(ContactInfo lhs, ContactInfo rhs)
	{
		if(checkFavoriteType)
		{
			FavoriteType lhsFavoriteType = lhs.getFavoriteType();
			FavoriteType rhsFavoriteType = rhs.getFavoriteType();

			// if both the values are FRIEND REQUEST then compare on the basis of contacts
			if (lhsFavoriteType == FavoriteType.REQUEST_RECEIVED && rhsFavoriteType == FavoriteType.REQUEST_RECEIVED)
				return lhs.compareTo(rhs);

			else if (lhsFavoriteType == FavoriteType.REQUEST_RECEIVED)
				return -1;

			else if (rhsFavoriteType == FavoriteType.REQUEST_RECEIVED)
				return 1;
		}

		return lhs.compareTo(rhs);
	}
}
