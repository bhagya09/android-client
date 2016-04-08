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
import com.bsb.hike.chatthemes.ChatThemeManager;
import com.bsb.hike.chatthemes.HikeChatThemeConstants;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.ui.PinHistoryActivity;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

public class PinHistoryAdapter extends BaseAdapter implements OnLongClickListener, OnClickListener
{
	private Activity context;

	private List<ConvMessage> textPins;

	private List<Object> listData;

	private LayoutInflater inflater;

	private OneToNConversation mConversation;

	private boolean addDateInBetween;

	private boolean isDefaultTheme;

	private Set<Long> mSelectedPinIds;
	
	private boolean isActionModeOn = false;
	
	private PinHistoryActivity pinHistory;
	
	private String chatThemeId;

	public PinHistoryAdapter(Activity context, List<ConvMessage> textPins, String userMsisdn, long convId, OneToNConversation conversation, boolean addDateInbetween, String themeId, PinHistoryActivity pinHistory)
	{
		this.context = context;
		this.isDefaultTheme = themeId.equals(ChatThemeManager.getInstance().defaultChatThemeId);
		this.addDateInBetween = addDateInbetween;
		this.mConversation = conversation;		
		this.pinHistory = pinHistory;
		this.chatThemeId = themeId;
		
		this.textPins = textPins;
		addDateInBetween();
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		mSelectedPinIds = new HashSet<Long>();
	}

	private enum ViewType
	{
		TEXT, DATE_SEP
	}

	public void appendPinstoView(List<ConvMessage> list)
	{
		textPins.addAll(list);
		addDateInBetween();
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

	public int getCurrentPinsCount()
	{
		return textPins.size();
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
				setDayIndicatorColor(convertView);
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
			ConvMessage textPin = (ConvMessage) data;
			if (textPin.isSent())
			{

				viewHolder.sender.setText(context.getString(R.string.you) + ", ");
			}
			else
			{
				if (OneToNConversationUtils.isGroupConversation(textPin.getMsisdn()))
				{
					String number = null;
					String name = mConversation.getConvParticipantFirstNameAndSurname(textPin.getGroupParticipantMsisdn());

					if (mConversation.getConversationParticipant(textPin.getGroupParticipantMsisdn()).getFirst().getContactInfo().isUnknownContact())
					{
						number = textPin.getGroupParticipantMsisdn();
					}

					if (number != null)
					{
						viewHolder.sender.setText(number + " ~ " + name + ", ");
					}
					else
					{
						viewHolder.sender.setText(name + ", ");
					}
				}
			}
			CharSequence markedUp = textPin.getMessage();
			SmileyParser smileyParser = SmileyParser.getInstance();
			markedUp = smileyParser.addSmileySpans(markedUp, false);

			viewHolder.detail.setText(markedUp);
			viewHolder.timestamp.setText(textPin.getTimestampFormatted(false, context));
			Linkify.addLinks(viewHolder.detail, Linkify.ALL);
			Linkify.addLinks(viewHolder.detail, Utils.shortCodeRegex, "tel:");		
			
			setSelection(textPin, viewHolder.selectedStateOverlay);
		}
			break;
		case DATE_SEP:
			TextView tv = (TextView) convertView.findViewById(R.id.day);
			tv.setText((String) data);
			break;
		}

		return convertView;
	}

	private void setDayIndicatorColor(View inflated)
	{
		TextView dayTextView = (TextView) inflated.findViewById(R.id.day);
		View dayLeft = inflated.findViewById(R.id.day_left);
		View dayRight = inflated.findViewById(R.id.day_right);

		if (isDefaultTheme)
		{
			dayTextView.setTextColor(context.getResources().getColor(R.color.list_item_header));
			dayLeft.setBackgroundColor(context.getResources().getColor(R.color.day_line));
			dayRight.setBackgroundColor(context.getResources().getColor(R.color.day_line));
		}
		else
		{
			dayTextView.setTextColor(context.getResources().getColor(R.color.white));
			dayLeft.setBackgroundColor(context.getResources().getColor(R.color.white));
			dayRight.setBackgroundColor(context.getResources().getColor(R.color.white));
		}

	}

	private void addDateInBetween()
	{
		List<Object> newData = new ArrayList<Object>();
		if (textPins.size() > 0)
		{
			if (addDateInBetween)
			{
				long timeStamp = textPins.get(0).getTimestamp();
				Calendar c = Calendar.getInstance();
				c.setTimeInMillis(timeStamp * 1000);
				String timeToShow = textPins.get(0).getMessageDate(context);
				newData.add(timeToShow);
				newData.add(textPins.get(0));
				for (int i = 1; i < textPins.size(); i++)
				{
					Calendar newC = Calendar.getInstance();
					newC.setTimeInMillis(textPins.get(i).getTimestamp() * 1000);
					if (c.get(Calendar.DAY_OF_YEAR) != newC.get(Calendar.DAY_OF_YEAR))
					{
						newData.add(textPins.get(i).getMessageDate(context));
						c = newC;
					}
					newData.add(textPins.get(i));
				}
			}
			else
			{
				for (ConvMessage con : textPins)
				{
					newData.add(con);
				}
			}
		}
		listData = newData;
		notifyDataSetChanged();
	}
	
	public void addPinMessage(ConvMessage convMsg)
	{
		textPins.add(0, convMsg);
		
		long timeStamp = textPins.get(0).getTimestamp();
		Calendar cFirst = Calendar.getInstance();
		cFirst.setTimeInMillis(timeStamp * 1000);
		
		if(textPins.size() > 1)
		{
			Calendar cSecond = Calendar.getInstance();
			cSecond.setTimeInMillis(((ConvMessage)textPins.get(1)).getTimestamp() * 1000);
			
			if (cFirst.get(Calendar.DAY_OF_YEAR) != cSecond.get(Calendar.DAY_OF_YEAR))
			{
				listData.add(0,((ConvMessage)textPins.get(0)).getMessageDate(context));
			}
			listData.add(1, convMsg);
		}
		else
		{
			listData.add(0,((ConvMessage)textPins.get(0)).getMessageDate(context));
			listData.add(convMsg);
		}
	}
		
	public Set<Long> getSelectedPinsIds()
	{
		return mSelectedPinIds;
	}
		
	public HashMap<Long, ConvMessage> getSelectedPinsMap()
	{
		HashMap<Long, ConvMessage> selectedPins = new HashMap<Long, ConvMessage>();
		
		for (ConvMessage pin : textPins)
		{
			if (mSelectedPinIds.contains(pin.getMsgID()))
			{
				selectedPins.put(pin.getMsgID(), pin);
			}
		}
		return selectedPins;
	}
	
	public void selectView(ConvMessage pin, boolean value)
	{
		if (value)
		{
			mSelectedPinIds.add(pin.getMsgID());
		}
		else
		{
			mSelectedPinIds.remove(pin.getMsgID());
		}

		notifyDataSetChanged();
	}
	
	public void removeSelection()
	{
		mSelectedPinIds.clear();		
		notifyDataSetChanged();
	}
	
	public int getSelectedPinsCount()
	{
		return mSelectedPinIds.size();
	}
	
	public void toggleSelection(ConvMessage convMsg)
	{
		selectView(convMsg, !isSelected(convMsg));
	}
	
	public boolean isSelected(ConvMessage convMsg)
	{
		return mSelectedPinIds.contains(convMsg.getMsgID());
	}

	public void removeMessage(ConvMessage pin)
	{
		int index = textPins.lastIndexOf(pin);		
		textPins.remove(index);
					
		index = listData.lastIndexOf(pin);
		
		boolean isPrevPin = false;
		boolean isNextPin = false;
		boolean isLastPin = false;		
		
		// check if previous item in list is a pin or date separator
		if(listData.size() > 1)
		{
			if(listData.get(index-1) instanceof ConvMessage)
			{
				isPrevPin = true;
			}
		}
		// index+1 should be within bounds		
		if(index+1 <= listData.size()-1)
		{
			// check if next item in list is a pin or a date separator
			if(listData.get(index+1) instanceof ConvMessage)
			{
				isNextPin = true;
			}
		}
		// index+1 beyond list-size mean this is the last item in the list
		else
		{
			isLastPin = true;
		}
		listData.remove(index);
		
		// remove the date separator as well
		// date separator of a pin is eligible for deletion only if
		// 1. previous and next items in the list w.r.t to current index are date separators
		// 2. previous item w.r.t to current index is date separator and current index is the last item in the list
		if((!isPrevPin && isLastPin) || (!isPrevPin && !isNextPin))
		{
			listData.remove(index-1);
		}
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
	
	View.OnClickListener selectedStateOverlayClickListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			if (isActionModeOn)
			{
				pinHistory.showPinContextMenu((ConvMessage) v.getTag());
			}
			return;
		}
	};

	private void setSelection(ConvMessage convMessage, View overlay)
	{
		if (isActionModeOn)
		{
			overlay.setVisibility(View.VISIBLE);
			overlay.setTag(convMessage);
			overlay.setOnClickListener(selectedStateOverlayClickListener);			

			if (isSelected(convMessage))
			{
				//calling multi select color for the theme
				overlay.setBackground(ChatThemeManager.getInstance().
						getDrawableForTheme(chatThemeId, HikeChatThemeConstants.ASSET_INDEX_MULTISELECT_CHAT_BUBBLE_BG));
			}
			else
			{
				overlay.setBackgroundColor(context.getResources().getColor(R.color.transparent));
			}
		}
		else
		{
			overlay.setVisibility(View.GONE);
		}
	}
	
	public void setActionMode(boolean isOn)
	{
		isActionModeOn = isOn;
	}
	
	public void addPins(List<ConvMessage> pins)
	{
		Collections.reverse(pins);
		textPins.addAll(0, pins);		
		addDateInBetween();
		notifyDataSetChanged();
	}
}
