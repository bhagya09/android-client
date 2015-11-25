package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.smartImageLoader.IconLoader;

public class HikeInviteAdapter extends SectionedBaseAdapter implements TextWatcher
{
	private HashMap<Integer, List<Pair<AtomicBoolean, ContactInfo>>> completeSectionsData;

	private HashMap<Integer, List<Pair<AtomicBoolean, ContactInfo>>> filteredSectionsData;

	private ContactFilter filter;

	private String filterString;

	private boolean showingBlockedList;

	private IconLoader iconLoader;

	private int mIconImageSize;

	private Activity activity;

	public HikeInviteAdapter(Activity activity, int viewItemId, HashMap<Integer, List<Pair<AtomicBoolean, ContactInfo>>> completeSectionsData, boolean showingBLockedList)
	{

		// super(activity, viewItemId, completeList);
		mIconImageSize = activity.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		this.activity = activity;
		this.filteredSectionsData = completeSectionsData;
		this.completeSectionsData = new HashMap<Integer, List<Pair<AtomicBoolean, ContactInfo>>>(completeSectionsData.size());
		this.completeSectionsData = (HashMap<Integer, List<Pair<AtomicBoolean, ContactInfo>>>) completeSectionsData.clone();
		this.filter = new ContactFilter();
		this.showingBlockedList = showingBLockedList;
		iconLoader = new IconLoader(activity, mIconImageSize);
		iconLoader.setDefaultAvatarIfNoCustomIcon(true);
	}

	public HashMap<Integer, List<Pair<AtomicBoolean, ContactInfo>>> getCompleteList()
	{
		return completeSectionsData;
	}

	@Override
	public View getItemView(int section, int position, View convertView, ViewGroup parent)
	{
		Pair<AtomicBoolean, ContactInfo> pair = (Pair<AtomicBoolean, ContactInfo>) getItem(section, position);

		AtomicBoolean isChecked = null;
		ContactInfo contactInfo = null;
		if (pair != null)
		{
			isChecked = pair.first;
			contactInfo = pair.second;
		}
		else
		{
			contactInfo = new ContactInfo(filterString, filterString, filterString, filterString);
		}

		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = convertView;
		if (v == null)
		{
			v = inflater.inflate(R.layout.hike_list_item, parent, false);
		}
		ImageView imageView = (ImageView) v.findViewById(R.id.contact_image);
		if (pair != null)
		{
			iconLoader.loadImage(contactInfo.getMsisdn(), imageView, false, true, true);
		}
		else
		{
			imageView.setImageDrawable(HikeBitmapFactory.getDefaultTextAvatar(null));
		}

		TextView textView = (TextView) v.findViewById(R.id.name);
		textView.setText(contactInfo.getName());

		TextView numView = (TextView) v.findViewById(R.id.number);
		String msisdn = contactInfo.getMsisdn();
		if (pair != null)
		{
			numView.setText(msisdn);
			if (!TextUtils.isEmpty(contactInfo.getMsisdnType()))
			{
				numView.append(" (" + contactInfo.getMsisdnType() + ")");
			}
		}
		else
		{
			numView.setText(showingBlockedList ? R.string.tap_here_block : R.string.tap_here_invite);
		}

		if (HikeMessengerApp.hikeBotInfoMap.containsKey(msisdn))
		{
			numView.setVisibility(View.GONE);
		}
		else
		{
			numView.setVisibility(isEnabled(section, position) ? View.VISIBLE : View.INVISIBLE);
		}
		CheckBox checkBox = (CheckBox) v.findViewById(R.id.checkbox);
		checkBox.setVisibility(pair != null ? View.VISIBLE : View.GONE);
		checkBox.setButtonDrawable(showingBlockedList ? R.drawable.block_button : R.drawable.hike_list_item_checkbox);

		if (pair != null)
		{
			checkBox.setChecked(isChecked.get());
			v.setTag(pair);
		}
		else
		{
			v.setTag(contactInfo);
		}
		return v;
	}

	@Override
	public void afterTextChanged(Editable s)
	{
		filter.filter(s);
		//Regex Explanation - number can start with '+', then any character between [0-9] one or more time and any character among them [-, ., space, slash ]only once
		//if this pattern match then ignore all the hyphen, dot, space, slash 
		if(s.toString().trim().matches("^\\+?(([0-9]+)[-.\\s/]?)*")){
		    filterString =s.toString().trim().replaceAll("[-.\\s /]", "");
		}else{
			filterString =s.toString().trim();
		}
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

				HashMap<Integer, List<Pair<AtomicBoolean, ContactInfo>>> filteredSectionsContacts = new HashMap<Integer, List<Pair<AtomicBoolean, ContactInfo>>>();
				//Regex Explanation - number can start with '+', then any character between [0-9] one or more time and any character among them [-, ., space, slash ]only once
				if(textToBeFiltered.matches("^\\+?(([0-9]+)[-.\\s/]?)*")){
					textToBeFiltered = constraint.toString().toLowerCase().trim().replaceAll("[-.\\s /]", "");
				}
				Set<Entry<Integer, List<Pair<AtomicBoolean, ContactInfo>>>> entrySet = HikeInviteAdapter.this.completeSectionsData.entrySet();
				for (Entry<Integer, List<Pair<AtomicBoolean, ContactInfo>>> entry : entrySet)
				{
					int section = entry.getKey();
					List<Pair<AtomicBoolean, ContactInfo>> filteredContacts = new ArrayList<Pair<AtomicBoolean, ContactInfo>>();
					for (Pair<AtomicBoolean, ContactInfo> info : entry.getValue())
					{
						if (info != null)
						{
							ContactInfo contactInfo = info.second;
							if (contactInfo.getName().toLowerCase().contains(textToBeFiltered) || contactInfo.getMsisdn().contains(textToBeFiltered))
							{
								filteredContacts.add(info);
							}
						}
					}
					if (section + 1 == completeSectionsData.size() && shouldShowExtraElement(textToBeFiltered))
					{
						filteredContacts.add(null);
					}
					filteredSectionsContacts.put(section, filteredContacts);
				}
				results.count = filteredSectionsContacts.size();
				results.values = filteredSectionsContacts;

			}
			else
			{
				results.count = HikeInviteAdapter.this.completeSectionsData.size();
				results.values = HikeInviteAdapter.this.completeSectionsData;
			}
			return results;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results)
		{
			filteredSectionsData = (HashMap<Integer, List<Pair<AtomicBoolean, ContactInfo>>>) results.values;
			notifyDataSetChanged();
		}
	}

	private boolean shouldShowExtraElement(String s)
	{
		String pattern = "(\\+?\\d*)";
		if (s.matches(pattern))
		{
			return true;
		}
		return false;
	}

	@Override
	public boolean areAllItemsEnabled()
	{
		return false;
	}
	
	@Override
	public boolean isEmpty()
	{
		for (Integer section : filteredSectionsData.keySet())
			{
				if(getCountForSection(section)>0)
				{
					return false;
				}
			}
		return true;
	}

	public boolean isEnabled(int section, int position)
	{
		if (getItem(section, position) == null)
		{
			return filterString.matches(HikeConstants.VALID_MSISDN_REGEX);
		}
		return true;
	}

	@Override
	public int getItemViewTypeCount()
	{
		return 1;
	}

	@Override
	public Object getItem(int section, int position)
	{
		// TODO Auto-generated method stub
		return filteredSectionsData.get(section).get(position);
	}

	@Override
	public long getItemId(int section, int position)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getSectionCount()
	{
		// TODO Auto-generated method stub
		return filteredSectionsData.size();
	}

	@Override
	public int getCountForSection(int section)
	{
		// TODO Auto-generated method stub
		return filteredSectionsData.get(section).size();
	}

	@Override
	public View getSectionHeaderView(int section, View convertView, ViewGroup parent)
	{
		if (convertView == null)
		{
			LayoutInflater li = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = li.inflate(R.layout.friends_group_view, parent, false);
			convertView.setBackgroundColor(activity.getResources().getColor(R.color.white));
		}
		TextView textView = (TextView) convertView.findViewById(R.id.name);
		TextView countView = (TextView) convertView.findViewById(R.id.count);
		switch (section)
		{
		case 0:
			if (!showingBlockedList)
			{
				textView.setText(getSectionCount() == 1 ? R.string.all_contacts : R.string.recommended_contacts_section);
			}
			else
			{
				textView.setText(getSectionCount() == 1 ? R.string.all_contacts : R.string.blocked_contacts);
			}
			break;
		case 1:
			textView.setText(R.string.all_contacts);
			break;
		default:
			break;
		}
		int sectionCount = getCountForSection(section);
		countView.setText(sectionCount+"");
		if(sectionCount > 0)
		{
			convertView.findViewById(R.id.section_view).setVisibility(View.VISIBLE);
		}
		else
		{
			convertView.findViewById(R.id.section_view).setVisibility(View.GONE);
		}
		return convertView;
	}
	
	public IconLoader getIconLoader()
	{
		return iconLoader;
	}
}
