package com.bsb.hike.messageinfo;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.smartImageLoader.IconLoader;

/**
 * Created by ravi on 5/2/16.
 */
public class RemainingItemAdapter extends BaseAdapter
{
	List<RemainingListItem> completeitemList;

	Context context;

	IconLoader iconLoader;

	public RemainingItemAdapter(List<RemainingListItem> completeitemList)
	{

		context = HikeMessengerApp.getInstance().getApplicationContext();
		int mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		this.iconLoader = new IconLoader(context, mIconImageSize);
		iconLoader.setDefaultAvatarIfNoCustomIcon(true);
		this.completeitemList = completeitemList;
	}

	@Override
	public int getCount()
	{
		return completeitemList.size();
	}

	@Override
	public Object getItem(int position)
	{
		return completeitemList.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return 0;
	}

	public static class ViewHolder
	{
		ImageView contactAvatar;

		TextView contactName;

		ImageView messageStatus;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ViewHolder viewHolder = null;
		if (convertView == null)
		{
			viewHolder = new ViewHolder();
			convertView = inflater.inflate(R.layout.messageinfo_remaining_list_item, null);
			viewHolder.contactAvatar = (ImageView) convertView.findViewById(R.id.avatar);
			viewHolder.contactName = (TextView) convertView.findViewById(R.id.contactName);
			viewHolder.messageStatus = (ImageView) convertView.findViewById(R.id.messagestatus);
			convertView.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder) convertView.getTag();
		}
		RemainingListItem item = (RemainingListItem) getItem(position);
		iconLoader.loadImage(item.msisdn, viewHolder.contactAvatar,false,true);
		viewHolder.messageStatus.setImageResource(item.drawableStatus);
		viewHolder.contactName.setText(item.name);
		return convertView;
	}
}
