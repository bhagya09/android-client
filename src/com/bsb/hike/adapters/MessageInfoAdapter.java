package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
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

	public static final int MESSAGE_INFO_SMS = 5;

	public static final int MESSAGE_INFO_NOTAPPLICABLE = 6;

	public static final int MESSAGE_INFO_EMPTY = 7;

	private Context context;

	private IconLoader iconLoader;

	private List<MessageInfoItem> completeitemList;

	public Conversation conversation;

	public ConvMessage convMessage;

	private MessageInfoView messageInfoView;


	public View view;
	private final LayoutParams MATCH_PARENT = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
	private final LayoutParams WRAP_CONTENT = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	public MessageInfoAdapter(MessageInfoActivity messageInfoActivity, List<MessageInfoItem> completeitemList, ConvMessage convMessage)
	{
		// super(messageInfoActivity,-1,itemList);
		context = messageInfoActivity;
		int mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size_messageinfo);
		this.iconLoader = new IconLoader(context, mIconImageSize);
		iconLoader.setDefaultAvatarIfNoCustomIcon(true);
		this.completeitemList = completeitemList;
		this.convMessage = convMessage;

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
		return 8;
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
				viewHolder.defaulttimestampLayout=viewHolder.parent.findViewById(R.id.default_timestamp);
				viewHolder.expandedReadLayout=viewHolder.parent.findViewById(R.id.expandedReadLayout);
				viewHolder.expandedDeliveredLayout=viewHolder.parent.findViewById(R.id.expandedDeliveredLayout);
				viewHolder.expandedReadTimeStamp=(TextView)viewHolder.parent.findViewById(R.id.read_message_timestamp);
				viewHolder.expandedDeliveredTimeStamp=(TextView)viewHolder.parent.findViewById(R.id.delivered_message_timestamp);
				viewHolder.expandedReadDescription=(TextView)viewHolder.parent.findViewById(R.id.expand_read_text);
				viewHolder.expandedDeliveredDescription=(TextView)viewHolder.parent.findViewById(R.id.expand_delivered_text);
				viewHolder.divider=viewHolder.parent.findViewById(R.id.dividermiddle);
				viewHolder.dividerend=viewHolder.parent.findViewById(R.id.dividerend);
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
			case MESSAGE_INFO_SMS:
				v = inflater.inflate(R.layout.messageinfo_sms_item, null);
				break;
			case MESSAGE_INFO_NOTAPPLICABLE:
				v = inflater.inflate(R.layout.messageinfo_sms_item, null);
				viewHolder.text = (TextView) v.findViewById(R.id.text);
				v.setTag(viewHolder);
				break;
			case MESSAGE_INFO_EMPTY:
				v = inflater.inflate(R.layout.messageinfo_empty_item, null);
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
			viewHolder.contactName.setText(contactInfo.getNameOrMsisdn());
			setAvatar(contactInfo.getMsisdn(), viewHolder.contactAvatar);
			viewHolder.timeStamp.setText(participant.getDisplayedTimeStamp());
			Logger.d("refresh", "Adapter parent" + viewHolder.parent);
			parentView.addView(viewHolder.parent);

			animateViews(viewHolder, v, participant);
			Logger.d("refresh", "adapter LISTCONTACTGROUP " + messageInfoItem + " position " + position);
			viewHolder.messageInfoParticipantItem=participant;
			viewHolder.expandedReadDescription.setText(messageInfoView.getReadListExpandedString());
			viewHolder.expandedReadTimeStamp.setText(participant.getReadTimeStamp());
			viewHolder.expandedDeliveredTimeStamp.setText(participant.getDeliveredTimeStamp());
			v.setOnClickListener(readListItemonClick);

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
		case MESSAGE_INFO_NOTAPPLICABLE:
			viewHolder=(ViewHolder)v.getTag();
			viewHolder.text.setText(R.string.messageinfo_not_applicable);
			break;
			

		}

		return v;
	}
	public void animateViews(ViewHolder viewHolder,View v,MessageInfoItem.MesageInfoParticipantItem participant) {

		if (participant.getType() != MessageInfoItem.READ_CONTACT)
			return;

		final View expandedReadLayout = viewHolder.expandedReadLayout;
		final View expandedDeliveredLayout = viewHolder.expandedDeliveredLayout;
		final View defaultTimeStampLayout = viewHolder.defaulttimestampLayout;
		int currentHashCode = participant.getHashCode();

		if (expandSet.contains(currentHashCode)) {

			defaultTimeStampLayout.setVisibility(View.GONE);
			expandedReadLayout.setVisibility(View.VISIBLE);
			expandedDeliveredLayout.setVisibility(View.VISIBLE);

		} else if (collapseSet.contains(currentHashCode)) {

			defaultTimeStampLayout.setVisibility(View.VISIBLE);
			expandedReadLayout.setVisibility(View.GONE);
			expandedDeliveredLayout.setVisibility(View.GONE);
		}


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
	public HashSet<Integer> expandSet=new HashSet<Integer>();
	public HashSet<Integer> collapseSet=new HashSet<Integer>();
	public View.OnClickListener readListItemonClick = new View.OnClickListener()
	{
		@Override
		public void onClick(final View v)
		{

			ViewHolder viewHolder = (ViewHolder) v.getTag();
			MessageInfoItem.MesageInfoParticipantItem participantItem = viewHolder.messageInfoParticipantItem;
			if (participantItem == null)
				return;
			int currentHashCode = participantItem.getHashCode();

			if (!expandSet.contains(currentHashCode) && !collapseSet.contains(currentHashCode)) {
				expandSet.clear();
				expandSet.add(currentHashCode);
			} else if (expandSet.contains(currentHashCode)) {
				collapseSet.clear();
				collapseSet.add(currentHashCode);

				expandSet.remove(currentHashCode);
			} else if (collapseSet.contains(currentHashCode)) {
				expandSet.add(currentHashCode);
				collapseSet.remove(currentHashCode);
			}


			notifyDataSetChanged();


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
		AlertDialog ag = alertDialog.create();
		Resources r = context.getResources();
		DisplayMetrics metrics = r.getDisplayMetrics();
		float px = r.getDimensionPixelSize(R.dimen.read_listitem_expanded_height) * remainingItem.remainingItemList.size();
		int height = (metrics.heightPixels) * 2 / 3;
		if (px > height)
			ag.getWindow().setLayout(metrics.widthPixels, height);
		ag.show();

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

		View defaulttimestampLayout;

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

		MessageInfoItem.MesageInfoParticipantItem messageInfoParticipantItem;

		boolean shouldExpand=false;

		boolean shouldCollapse=false;

		boolean isExpanded=false;

		View expandedReadLayout;

		View expandedDeliveredLayout;

		View collapsedReadLayout;

		View divider;

		View dividerend;


	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		//notifyDataSetInvalidated();
	}
}