package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.SocialNetFriendInfo;
import com.bsb.hike.smartImageLoader.SocialIconLoader;
import com.bsb.hike.utils.Logger;

public class SocialNetInviteAdapter extends ArrayAdapter<Pair<AtomicBoolean, SocialNetFriendInfo>> implements TextWatcher
{

	private ArrayList<Pair<AtomicBoolean, SocialNetFriendInfo>> completeFriendsList;

	private ArrayList<Pair<AtomicBoolean, SocialNetFriendInfo>> filteredList;

	private ContactFilter filter;

	private Context context;

	private LayoutInflater l_Inflater;

	private SocialIconLoader imgLoader;

	private int mIconImageSize;

	private boolean isListFlinging;

	public SocialNetInviteAdapter(Context context, int viewItemId, ArrayList<Pair<AtomicBoolean, SocialNetFriendInfo>> completeFbFriendsList)
	{
		super(context, viewItemId, completeFbFriendsList);
		l_Inflater = LayoutInflater.from(context);
		this.context = context;
		this.filteredList = completeFbFriendsList;
		this.completeFriendsList = new ArrayList<Pair<AtomicBoolean, SocialNetFriendInfo>>(completeFbFriendsList.size());
		this.completeFriendsList.addAll(completeFbFriendsList);
		this.filter = new ContactFilter();
		mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		imgLoader = new SocialIconLoader(context, mIconImageSize);

	}

	public int getCount()
	{
		return filteredList.size();
	}

	public long getItemId(int position)
	{
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent)
	{
		ViewHolder holder;

		SocialNetFriendInfo currFriend = getItem(position).second;
		if (convertView == null)
		{
			convertView = l_Inflater.inflate(R.layout.invite_list_item, null);
			holder = new ViewHolder();
			holder.txt_itemName = (TextView) convertView.findViewById(R.id.name);
			// holder.txt_itemDescription = (TextView)
			// convertView.findViewById(R.id.itemDescription);
			holder.itemImage = (ImageView) convertView.findViewById(R.id.contact_image);
			convertView.setTag(holder);

		}
		else
		{
			holder = (ViewHolder) convertView.getTag();
		}
		// if(position < getCount()){
		Logger.d("getView", currFriend.getName());
		holder.txt_itemName.setText(currFriend.getName());
		CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkbox);
		checkBox.setChecked(getItem(position).first.get());
		holder.itemImage.setTag(currFriend.getImageUrl());
		imgLoader.loadImage(currFriend.getImageUrl(), holder.itemImage, isListFlinging);
		// }
		return convertView;
	}

	class ViewHolder
	{
		TextView txt_itemName;

		TextView txt_itemDescription;

		ImageView itemImage;

		boolean downloadImageRequestSent;
	}

	public String getTitle()
	{
		return context.getResources().getString(R.string.invite);
	}

	@Override
	public void afterTextChanged(Editable s)
	{
		Logger.d("after Text change", s.toString());
		filter.filter(s);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
	}

	private class ContactFilter extends Filter
	{
		@Override
		protected FilterResults performFiltering(CharSequence constraint)
		{
			FilterResults results = new FilterResults();

			String textToBeFiltered = TextUtils.isEmpty(constraint) ? "" : constraint.toString().toLowerCase();

			if (!TextUtils.isEmpty(textToBeFiltered))
			{

				List<Pair<AtomicBoolean, SocialNetFriendInfo>> filteredContacts = new ArrayList<Pair<AtomicBoolean, SocialNetFriendInfo>>();

				for (Pair<AtomicBoolean, SocialNetFriendInfo> info : SocialNetInviteAdapter.this.completeFriendsList)
				{
					if (info != null)
					{
						SocialNetFriendInfo fbFriendInfo = info.second;
						if (fbFriendInfo.getName().toLowerCase().contains(textToBeFiltered))
						{
							filteredContacts.add(info);
						}
					}
				}
				results.count = filteredContacts.size();
				results.values = filteredContacts;

			}
			else
			{
				results.count = SocialNetInviteAdapter.this.completeFriendsList.size();
				results.values = SocialNetInviteAdapter.this.completeFriendsList;
			}
			return results;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results)
		{
			filteredList = (ArrayList<Pair<AtomicBoolean, SocialNetFriendInfo>>) results.values;
			notifyDataSetChanged();
			clear();
			for (Pair<AtomicBoolean, SocialNetFriendInfo> pair : filteredList)
			{
				Logger.d("filtered", pair.second.getName());
				add(pair);
			}
			notifyDataSetInvalidated();
		}
	}

	@Override
	public boolean areAllItemsEnabled()
	{
		return false;
	}

	@Override
	public boolean isEnabled(int position)
	{
		return super.isEnabled(position);
	}

	public SocialIconLoader getSocialIconLoader()
	{
		return imgLoader;
	}

	public void setIsListFlinging(boolean b)
	{
		boolean notify = b != isListFlinging;

		isListFlinging = b;

		if (notify && !isListFlinging)
		{
			notifyDataSetChanged();
		}
	}
}
