package com.bsb.hike.timeline.adapter;

import java.util.List;

import android.accounts.Account;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.AccountInfo;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.RoundedImageView;

public class DisplayContactsAdapter extends BaseAdapter
{

	private List<String> msisdnList;

	private LayoutInflater layoutInflater;

	private Context mContext;

	private IconLoader mAvatarLoader;

	public DisplayContactsAdapter(List<String> argMsisdnList)
	{
		if (argMsisdnList == null || argMsisdnList.isEmpty())
		{
			throw new IllegalArgumentException("DisplayContactsAdapter(): input cannot be null or empty");
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
		return msisdnList.size();
	}

	@Override
	public String getItem(int position)
	{
		return msisdnList.get(position);
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

		if (convertView == null)
		{
			convertView = layoutInflater.inflate(R.layout.contacts_display_list_item, parent, false);
			holder = new ViewHolder(convertView);
			convertView.setTag(holder);
		}
		else
		{
			holder = (ViewHolder) convertView.getTag();
		}

		ContactInfo contactInfo = ContactManager.getInstance().getContact((getItem(position)), true,  false);

		if (contactInfo == null)
		{
			throw new IllegalStateException("DisplayContactsAdapter getView(): msisdn which doesn't have contact info");
		}

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

		return convertView;
	}

	private class ViewHolder
	{
		protected TextView contactName;

		protected TextView contactStatus;

		protected RoundedImageView avatar;

		public ViewHolder(View argView)
		{
			contactName = (TextView) argView.findViewById(R.id.contact_name);

			contactStatus = (TextView) argView.findViewById(R.id.contact_status);

			avatar = (RoundedImageView) argView.findViewById(R.id.avatar);
		}
	}

}
