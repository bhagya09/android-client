package com.bsb.hike.chatHead;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.chatHead.StickerShareSettings.ListViewItem;

public class ChatHeadSettingsArrayAdapter extends ArrayAdapter<ListViewItem>
{
	Context mContext;
	
	private static ArrayList<StickerShareSettings.ListViewItem> mListViewItems;

	public ChatHeadSettingsArrayAdapter(Context context, int resource, List<ListViewItem> objects)
	{
		super(context, resource, objects);
		mContext = context;
		mListViewItems = (ArrayList<StickerShareSettings.ListViewItem>)objects;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ListViewItem listItem = new ListViewItem();
		listItem = mListViewItems.get(position);
		if (convertView == null)
		{
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.settings_sticker_share_item, null);
		}
		ImageView imgView = (ImageView) convertView.findViewById(R.id.app_icon);
		TextView txtView = (TextView) convertView.findViewById(R.id.app_name);
		mListViewItems.get(position).mCheckBox = (CheckBox) convertView.findViewById(R.id.checkbox_item);
		imgView.setBackground(listItem.appIcon);
		txtView.setText(listItem.appName);
		if (listItem.appChoice)
		{
			mListViewItems.get(position).mCheckBox.setChecked(true);
		}
		else
		{
			mListViewItems.get(position).mCheckBox.setChecked(false);

		}
		convertView.setTag(position);
		mListViewItems.get(position).mCheckBox.setTag(position);
		mListViewItems.get(position).mCheckBox.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				StickerShareSettings.onItemClickEvent((int) v.getTag());
			}
		});

		convertView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				StickerShareSettings.onItemClickEvent((int) v.getTag());
			}
		});

		return convertView;
	}

}
