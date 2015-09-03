package com.bsb.hike.timeline.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.RoundedImageView;

public class DisplayContactsAdapter extends BaseAdapter
{

	private List<String> msisdnList;

	private LayoutInflater layoutInflater;

	private Context mContext;

	private IconLoader mAvatarLoader;
	
	private String suMsisdn;
	
	private int totalCount;
	
	//Contact saved in Address Book or Fav contact
	private final int KNOWN_CONTACT_VIEW_TYPE = -15;

	//Contact neither saved in Address book nor fav
	private final int UNKNOWN_CONTACT_VIEW_TYPE = -16;

	public DisplayContactsAdapter(List<String> argList, String suMsisdn)
	{
		List<String> argMsisdnList = new ArrayList<String>(argList);
		
		if (argMsisdnList == null || argMsisdnList.isEmpty())
		{
			throw new IllegalArgumentException("DisplayContactsAdapter(): input cannot be null or empty");
		}

		this.suMsisdn = suMsisdn;
		
		totalCount = argMsisdnList.size();
		
		// Iterate and remove all msisdns which are
		// 1) Not saved in Addressbook
		// 2) Non fav
		for (int j = argMsisdnList.size() - 1; j >= 0; j--)
		{
			ContactInfo contactInfo = ContactManager.getInstance().getContact(argMsisdnList.get(j), true, false);
			if (contactInfo != null)
			{
				// Contact is unsaved.............. AND .... Contact is non fav .... AND Contact is not a self user
				if (contactInfo.isUnknownContact() 
						&& !ContactInfo.FavoriteType.FRIEND.equals(contactInfo.getFavoriteType())
						&& !Utils.isSelfMsisdn(contactInfo.getMsisdn()))
				{
					argMsisdnList.remove(j);
				}
			}
			else
			{
				argMsisdnList.remove(j);
			}
		}

		mContext = HikeMessengerApp.getInstance().getApplicationContext();

		layoutInflater = LayoutInflater.from(mContext);

		msisdnList = argMsisdnList;

		mAvatarLoader = new IconLoader(mContext, mContext.getResources().getDimensionPixelSize(R.dimen.icon_picture_size));
		mAvatarLoader.setDefaultAvatarIfNoCustomIcon(true);
	}

	@Override
	public int getCount()
	{
		if(msisdnList.size() == totalCount)
		{
			return msisdnList.size();
		}
		else
		{
			return msisdnList.size() + 1;
		}
	}

	@Override
	public String getItem(int position)
	{
		String msisdn = null;
		switch (getItemViewType(position))
		{
		case KNOWN_CONTACT_VIEW_TYPE:
			msisdn = msisdnList.get(position);
			break;

		case UNKNOWN_CONTACT_VIEW_TYPE:
			msisdn = suMsisdn;
			break;

		default:
			break;
		}
		return msisdn;
	}

	@Override
	public long getItemId(int arg0)
	{
		// Same ID for all
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ViewHolder holder = null;

		int viewType = getItemViewType(position);

		if (convertView == null)
		{
			switch (viewType)
			{
			case KNOWN_CONTACT_VIEW_TYPE:
				convertView = layoutInflater.inflate(R.layout.contacts_display_list_item, parent, false);
				holder = new ViewHolder(convertView, KNOWN_CONTACT_VIEW_TYPE);
				convertView.setTag(holder);
				break;

			case UNKNOWN_CONTACT_VIEW_TYPE:
				convertView = layoutInflater.inflate(R.layout.unknown_contact_list_item, parent, false);
				holder = new ViewHolder(convertView, UNKNOWN_CONTACT_VIEW_TYPE);
				convertView.setTag(holder);
				break;

			default:
				break;
			}
		}
		else
		{
			holder = (ViewHolder) convertView.getTag();
		}

		ContactInfo contactInfo = ContactManager.getInstance().getContact((getItem(position)), true, false);

		if (contactInfo == null)
		{
			throw new IllegalStateException("DisplayContactsAdapter getView(): msisdn which doesn't have contact info");
		}

		switch (viewType)
		{
		case KNOWN_CONTACT_VIEW_TYPE:
			if (TextUtils.isEmpty(contactInfo.getName()))
			{
				ContactInfo myContactInfo = Utils.getUserContactInfo(false);
				if (myContactInfo.getMsisdn().equals(getItem(position)))
				{
					holder.contactName.setText(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.NAME_SETTING, mContext.getString(R.string.me)));
				}
				else
				{
					holder.contactName.setText(contactInfo.getNameOrMsisdn());
				}
			}
			else
			{
				holder.contactName.setText(contactInfo.getNameOrMsisdn());
			}

			// TODO Make this generic
			holder.contactStatus.setText(R.string.liked_this_post);

			holder.avatar.setOval(true);
			mAvatarLoader.loadImage(contactInfo.getMsisdn(), holder.avatar);
			break;

		case UNKNOWN_CONTACT_VIEW_TYPE:
			String text = String.format(mContext.getString(R.string.like_count_by_unknow_contacts), totalCount - msisdnList.size(), contactInfo.getNameOrMsisdn());
			holder.otherLikesCount.setText(text);
			break;

		default:
			break;
		}

		return convertView;
	}

	private class ViewHolder
	{
		protected TextView contactName;

		protected TextView contactStatus;

		protected RoundedImageView avatar;
		
		protected TextView otherLikesCount;

		public ViewHolder(View argView, int viewType)
		{
			if(viewType == KNOWN_CONTACT_VIEW_TYPE)
			{
				contactName = (TextView) argView.findViewById(R.id.contact_name);

				contactStatus = (TextView) argView.findViewById(R.id.contact_status);	

				avatar = (RoundedImageView) argView.findViewById(R.id.avatar);
			}
			else if(viewType == UNKNOWN_CONTACT_VIEW_TYPE)
			{
				otherLikesCount = (TextView) argView.findViewById(R.id.other_like_count);
			}
		}
	}

	@Override
	public int getItemViewType(int position)
	{
		if (position < msisdnList.size())
		{
			return KNOWN_CONTACT_VIEW_TYPE;
		}
		else 
		{
			return UNKNOWN_CONTACT_VIEW_TYPE;
		}
	}
	
	@Override
	public boolean isEnabled(int position)
	{
		int viewType = getItemViewType(position);
		if(viewType == KNOWN_CONTACT_VIEW_TYPE)
		{
			return super.isEnabled(position);
		}
		return false;
	}
}
