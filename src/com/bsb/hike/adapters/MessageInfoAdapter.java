package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.ui.MessageInfoActivity;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

public class MessageInfoAdapter extends BaseAdapter implements OnLongClickListener, OnClickListener
{
	private Activity context;

	private List<ConvMessage> textPins;

	private List<Object> listData;

	private LayoutInflater inflater;

	private Conversation mConversation;

	private boolean isDefaultTheme;

	private boolean isActionModeOn = false;

	private MessageInfoActivity messageInfoActivity;

	private ChatTheme chatTheme;

	public MessageInfoAdapter(Activity context, String userMsisdn, long convId, Conversation conversation, long messageID, ChatTheme theme, MessageInfoActivity messageInfoActivity)
	{
		this.context = context;
		this.isDefaultTheme = theme == ChatTheme.DEFAULT;
		this.mConversation = conversation;
		this.messageInfoActivity = messageInfoActivity;
		this.chatTheme = theme;
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

	}

	private enum ViewType
	{
		TEXT, DATE_SEP
	}

	public void appendPinstoView(List<ConvMessage> list)
	{
		textPins.addAll(list);
	}

	private class ViewHolder
	{
		TextView sender;

		TextView detail;

		TextView timestamp;

		View selectedStateOverlay;
	}

	@Override
	public int getCount()
	{
		return listData.size();
	}

	@Override
	public Object getItem(int position)
	{
		return listData.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return 0;
	}

	@Override
	public int getViewTypeCount()
	{
		return ViewType.values().length;
	}

	@Override
	public boolean areAllItemsEnabled()
	{
		return false;
	}

	@Override
	public boolean isEnabled(int position)
	{
		return getItem(position) instanceof ConvMessage;
	}

	@Override
	public int getItemViewType(int position)
	{
		Object obj = getItem(position);
		if (obj instanceof ConvMessage)
		{
			return ViewType.TEXT.ordinal();
		}
		return ViewType.DATE_SEP.ordinal();
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ViewType viewType = ViewType.values()[getItemViewType(position)];

		final Object data = getItem(position);

		final ViewHolder viewHolder;

		if (convertView == null)
		{
			viewHolder = new ViewHolder();

			switch (viewType)
			{
			case TEXT:
			{
				convertView = inflater.inflate(R.layout.pin_history, null);
				viewHolder.sender = (TextView) convertView.findViewById(R.id.sender);
				viewHolder.detail = (TextView) convertView.findViewById(R.id.text);
				viewHolder.timestamp = (TextView) convertView.findViewById(R.id.timestamp);
				viewHolder.selectedStateOverlay = convertView.findViewById(R.id.selected_state_overlay);
			}
				break;
			case DATE_SEP:
				convertView = inflater.inflate(R.layout.message_day_container, null);

				break;
			}
			convertView.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder) convertView.getTag();
		}

		switch (viewType)
		{
		case TEXT:
		{

		}
			break;
		case DATE_SEP:
			TextView tv = (TextView) convertView.findViewById(R.id.day);
			tv.setText((String) data);
			break;
		}

		return convertView;
	}


	@Override
	public boolean onLongClick(View v)
	{
		return false;
	}

	@Override
	public void onClick(View v)
	{
	}

	OnClickListener selectedStateOverlayClickListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{

			return;
		}
	};


	public void addPins(List<ConvMessage> pins)
	{
		Collections.reverse(pins);
		textPins.addAll(0, pins);
		notifyDataSetChanged();
	}
}
