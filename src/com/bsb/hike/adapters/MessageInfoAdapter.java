package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.messageinfo.MessageInfoDataModel;
import com.bsb.hike.messageinfo.MessageInfoItem;
import com.bsb.hike.messageinfo.MessageInfoView;
import com.bsb.hike.messageinfo.RemainingItemAdapter;
import com.bsb.hike.messageinfo.RemainingListItem;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.platform.CardRenderer;
import com.bsb.hike.platform.WebViewCardRenderer;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.ui.MessageInfoActivity;
import com.bsb.hike.utils.Logger;

public class MessageInfoAdapter extends BaseAdapter
{

	public static final int LIST_NAME = 0;

	public static final int LIST_REMAINING_GROUP = 1;

	public static final int LIST_ONE_TO_N_CONTACT = 2;

	public static final int LIST_ONE_TO_ONE = 3;

	public static final int MESSAGE_INFO_VIEW = 4;

	private Context context;

	private IconLoader iconLoader;

	private List<MessageInfoItem> completeitemList;

	public Conversation conversation;

	public ConvMessage convMessage;

	private MessageInfoView messageInfoView;

	private CardRenderer mMessageInfoCardRenderer;

	private WebViewCardRenderer mWebViewCardRenderer;

	public View view;

	public MessageInfoAdapter(MessageInfoActivity messageInfoActivity, List<MessageInfoItem> completeitemList, ConvMessage convMessage)
	{
		// super(messageInfoActivity,-1,itemList);
		context = messageInfoActivity;
		int mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size_messageinfo);
		this.iconLoader = new IconLoader(context, mIconImageSize);
		iconLoader.setDefaultAvatarIfNoCustomIcon(true);
		this.completeitemList = completeitemList;
		this.convMessage = convMessage;
		this.mMessageInfoCardRenderer=new CardRenderer(messageInfoActivity);

	}

	public void setMessageInfoView(MessageInfoView messageInfoView)
	{
		this.messageInfoView = messageInfoView;
	}

	@Override
	public int getItemViewType(int position)
	{

		MessageInfoItem profileItem = getItem(position);

		return profileItem.getViewType();
	}

	@Override
	public int getCount()
	{
		return completeitemList.size();
	}

	@Override
	public MessageInfoItem getItem(int position)
	{
		return completeitemList.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	public void addAll(HashSet<MessageInfoItem> map)
	{
		completeitemList.clear();
		completeitemList.addAll(map);
	}

	@Override
	public int getViewTypeCount()
	{
		return 5;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		int viewType = getItemViewType(position);

		MessageInfoItem messageInfoItem = getItem(position);
		View v = convertView;

		ViewHolder viewHolder = null;

		if (v == null)
		{
			viewHolder = new ViewHolder();

			switch (viewType)
			{
			case LIST_NAME:
				v = inflater.inflate(R.layout.messageinfo_sectionhead, null);
				viewHolder.listheader = (TextView) v.findViewById(R.id.headerTextView);
				viewHolder.headerImageViewRight = (ImageView) v.findViewById(R.id.headerImageView);
				v.setTag(viewHolder);
				break;

			case LIST_REMAINING_GROUP:
				v = inflater.inflate(R.layout.messageinfo_remaining_items, null);
				viewHolder.remainingItemsTextView = (TextView) v.findViewById(R.id.remainingItems);
				v.setOnClickListener(remainingItemonClick);
				v.setTag(viewHolder);
				break;

			case LIST_ONE_TO_N_CONTACT:
				v = new LinearLayout(context);
				viewHolder.parent = inflater.inflate(R.layout.messageinfo_item_contact, (LinearLayout) v, false);
				viewHolder.contactName = (TextView) viewHolder.parent.findViewById(R.id.contact);
				viewHolder.contactAvatar = (ImageView) viewHolder.parent.findViewById(R.id.avatar);
				viewHolder.timeStamp = (TextView) viewHolder.parent.findViewById(R.id.timestamp);
				v.setTag(viewHolder);
				break;

			case LIST_ONE_TO_ONE:
				v = inflater.inflate(R.layout.messageinfo_list_onetoone, null);
				viewHolder.listheader = (TextView) v.findViewById(R.id.headerTextView);
				viewHolder.headerImageViewRight = (ImageView) v.findViewById(R.id.headerImageView);
				viewHolder.timeStamp = (TextView) v.findViewById(R.id.timestamp);
				v.setTag(viewHolder);
				break;
			case MESSAGE_INFO_VIEW:

				v = messageInfoView.getView(v, convMessage);

				break;

			}

		}

		switch (viewType)
		{
		case LIST_NAME:
			viewHolder = (ViewHolder) v.getTag();
			MessageInfoItem.MessageStatusHeader statusHeaderItem = ((MessageInfoItem.MessageStatusHeader) messageInfoItem);
			String heading = statusHeaderItem.getHeaderString();
			viewHolder.listheader.setText(heading);
			viewHolder.headerImageViewRight.setImageResource(statusHeaderItem.getDrawable());
			Logger.d("refresh", "adapter LISTNAME " + messageInfoItem + " position " + position);
			break;

		case LIST_ONE_TO_N_CONTACT:
			viewHolder = (ViewHolder) v.getTag();
			LinearLayout parentView = (LinearLayout) v;
			parentView.removeAllViews();
			MessageInfoItem.MesageInfoParticipantItem participant = (MessageInfoItem.MesageInfoParticipantItem) messageInfoItem;
			parentView.setBackgroundColor(Color.WHITE);
			participant.applyDividerBehavior(viewHolder.parent);
			ContactInfo contactInfo = participant.getContactInfo();

			Logger.d("MessageInfo", "Adapter viewHolder " + viewHolder);

			Logger.d("MessageInfo", "Adapter text " + viewHolder.text);
			Logger.d("MessageInfo", "Adapter extrainfo " + viewHolder.extraInfo);

			viewHolder.contactName.setText(contactInfo.getNameOrMsisdn());

			setAvatar(contactInfo.getMsisdn(), viewHolder.contactAvatar);

			viewHolder.timeStamp.setText(participant.getDisplayedTimeStamp());

			Logger.d("refresh", "Adapter parent" + viewHolder.parent);
			parentView.addView(viewHolder.parent);
			Logger.d("refresh", "adapter LISTCONTACTGROUP " + messageInfoItem + " position " + position);
			break;
		case LIST_REMAINING_GROUP:
			viewHolder = (ViewHolder) v.getTag();
			MessageInfoItem.MesageInfoRemainingItem remainingItem = ((MessageInfoItem.MesageInfoRemainingItem) messageInfoItem);
			String remaining = remainingItem.getRemainingItem();
			viewHolder.remainingItemsTextView.setText(remaining);
			viewHolder.remainingItem = remainingItem;

			Logger.d("refresh", "adapter LISTREMAINING " + messageInfoItem + " position " + position);
			break;
		case LIST_ONE_TO_ONE:
			viewHolder = (ViewHolder) v.getTag();
			MessageInfoItem.MessageInfoItemOnetoOne onetoOneList = ((MessageInfoItem.MessageInfoItemOnetoOne) messageInfoItem);
			viewHolder.listheader.setText(onetoOneList.getHeader());
			viewHolder.headerImageViewRight.setImageResource(onetoOneList.getDrawableheaderIcon());
			viewHolder.timeStamp.setText(onetoOneList.getDisplayedTimeStamp());
			Logger.d("refresh", "adapter LIST_ONE_TO_ONE " + messageInfoItem + " position " + position);
			break;
		case MESSAGE_INFO_VIEW:
			v = messageInfoView.getView(v, convMessage);
			break;

		}

		return v;
	}

	public View.OnClickListener remainingItemonClick = new View.OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			MessageInfoItem.MesageInfoRemainingItem remainingItem = ((ViewHolder) v.getTag()).remainingItem;
			if (remainingItem != null)
			{
				if (remainingItem.shouldshowList())
					showRemainingItemDialog(remainingItem);
			}
		}
	};

	public void showRemainingItemDialog(MessageInfoItem.MesageInfoRemainingItem remainingItem)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View remView = inflater.inflate(R.layout.remaining_item_list, null);
		ListView remaininglistView = (ListView) remView.findViewById(android.R.id.list);
		ArrayList<RemainingListItem> remList = new ArrayList<RemainingListItem>();
		for (MessageInfoDataModel.MessageInfoParticipantData data : remainingItem.remainingItemList)
		{
			remList.add(new RemainingListItem(data.getContactInfo().getNameOrMsisdn(), data.getContactInfo().getMsisdn(), data.getDrawableMessageState()));

		}
		RemainingItemAdapter adapter = new RemainingItemAdapter(remList);
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
		remaininglistView.setAdapter(adapter);
		alertDialog.setView(remView);
		alertDialog.show();

	}

	private void setAvatar(String msisdn, ImageView avatarView)
	{
		iconLoader.loadImage(msisdn, avatarView, false, true);
	}

	private class ViewHolder
	{
		TextView listheader;

		TextView text;

		TextView subText;

		TextView extraInfo;

		View parent;

		View phoneNumViewDivider;

		// Views for Read List Played and Delivered List
		TextView contactName;

		ImageView contactAvatar;

		TextView timeStamp;

		TextView readTimeStamp;

		TextView expandedReadDescription;

		TextView expandedReadTimeStamp;

		TextView expandedDeliveredDescription;

		TextView expandedDeliveredTimeStamp;

		TextView expandedPlayedDescription;

		TextView expandedPlayedTimeStamp;

		ImageView headerImageViewRight;

		TextView remainingItemsTextView;

		TextView headerOneToOne;

		TextView timeStampOnetoOne;

		MessageInfoItem.MesageInfoRemainingItem remainingItem;

	}

}