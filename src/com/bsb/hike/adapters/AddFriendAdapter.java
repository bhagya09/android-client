package com.bsb.hike.adapters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.smartImageLoader.IconLoader;

public class AddFriendAdapter extends SectionedBaseAdapter {
	private HashMap<Integer, List<ContactInfo>> sectionsData;
	private Set<ContactInfo> selectedFriends;
	private Context context;
	private IconLoader iconloader;
	private int mIconImageSize;


	public AddFriendAdapter(Context context, int resource,
			HashMap<Integer, List<ContactInfo>> sectionsData) {
		this.context = context;
		this.sectionsData = sectionsData;
		selectedFriends = new HashSet<ContactInfo>();
		mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		this.iconloader = new IconLoader(context, mIconImageSize);
		iconloader.setDefaultAvatarIfNoCustomIcon(true);
	}

	@Override
	public Object getItem(int section, int position)
	{
		return sectionsData.get(section).get(position );
	}

	@Override
	public long getItemId(int section, int position)
	{
		return 0;
	}

	@Override
	public int getSectionCount()
	{
		return sectionsData.size();
	}

	@Override
	public int getCountForSection(int section)
	{
		return sectionsData.get(section).size();
	}

	@Override
	public View getItemView(int section, int position, View convertView, ViewGroup parent)
	{
		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(
					R.layout.hike_list_item, null);
			ViewHolder holder = new ViewHolder();
			holder.userImage = (ImageView) convertView
					.findViewById(R.id.contact_image);
			holder.name = (TextView) convertView
					.findViewById(R.id.name);
			holder.status = (TextView) convertView
					.findViewById(R.id.number);
			holder.checkbox = (CheckBox) convertView
					.findViewById(R.id.checkbox);
			convertView.setTag(holder);
		}
		ViewHolder holder = (ViewHolder) convertView.getTag();
		ContactInfo contact = (ContactInfo) getItem(section,position);
		holder.name.setText(contact.getName());
		holder.status.setText(contact.getMsisdn());
		iconloader.loadImage(contact.getMsisdn(), holder.userImage, false, true);
		if (selectedFriends.contains(contact)) {
			holder.checkbox.setChecked(true);
		} else {
			holder.checkbox.setChecked(false);
		}

		return convertView;
	}

	@Override
	public int getItemViewTypeCount()
	{
		return 1;
	}

	@Override
	public int getSectionHeaderViewTypeCount()
	{
		return 1;
	}

	@Override
	public View getSectionHeaderView(int section, View convertView, ViewGroup parent)
	{
		if (convertView == null)
		{
			LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = li.inflate(R.layout.friends_group_view, parent, false);
			convertView.setBackgroundColor(context.getResources().getColor(R.color.white));
		}
		TextView textView = (TextView) convertView.findViewById(R.id.name);
		TextView countView = (TextView) convertView.findViewById(R.id.count);
		switch (section)
		{
		case 0:
			textView.setText(getSectionCount()==1? R.string.hike_contacts : R.string.recommended_contacts_section);
			break;
		case 1:
			textView.setText(R.string.hike_contacts);
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
	
	private static class ViewHolder {
		ImageView userImage;
		TextView name;
		TextView status;
		CheckBox checkbox;
	}

	public Set<ContactInfo> getSelectedFriends() {
		return selectedFriends;
	}

	public void unSelectAllFriends() {
		selectedFriends.clear();
		notifyDataSetChanged();
	}

	public int getSelectedFriendsCount() {
		return selectedFriends.size();
	}

	public void unSelectItem(ContactInfo contactInfo)
	{
		selectedFriends.remove(contactInfo);
	}

	public void selectItem(ContactInfo contactInfo)
	{
		selectedFriends.add(contactInfo);
	}
	
	public IconLoader getIconImageLoader()
	{
		return iconloader;
	}
}
