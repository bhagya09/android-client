package com.bsb.hike.adapters;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.messageinfo.MessageInfoItem;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.ui.MessageInfoActivity;
import com.bsb.hike.utils.Logger;

public class MessageInfoAdapter extends BaseAdapter
{


 	public static final int LIST_NAME=0;
	public static final int LIST_REMAINING_GROUP=1;
	public static final int LIST_ONE_TO_N_CONTACT =2;
	public static final int LIST_ONE_TO_ONE=3;
	public static final int DEFAULT=4;
	private Context context;

	private IconLoader iconLoader;

	private List<MessageInfoItem> completeitemList;
	public MessageInfoAdapter(MessageInfoActivity messageInfoActivity,List<MessageInfoItem> completeitemList,boolean isOnetoN)
	{
		//super(messageInfoActivity,-1,itemList);
		context=messageInfoActivity;
		int mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		this.iconLoader = new IconLoader(context, mIconImageSize);
		iconLoader.setDefaultAvatarIfNoCustomIcon(true);
		this.completeitemList=completeitemList;
	}




	@Override
	public int getItemViewType(int position)
	{

		MessageInfoItem profileItem = getItem(position);

		return profileItem.getViewType();
	}


	@Override
	public int getCount() {
		return completeitemList.size();
	}

	@Override
	public MessageInfoItem getItem(int position) {
		return completeitemList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		int viewType = getItemViewType(position);

		MessageInfoItem messageInfoItem = getItem(position);
		Logger.d("refresh","adapter "+messageInfoItem+" position "+position);
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
					viewHolder.headerImageViewRight=(ImageView)v.findViewById(R.id.headerImageView);
					break;

				case LIST_ONE_TO_N_CONTACT:
					v = new LinearLayout(context);
					viewHolder.parent = inflater.inflate(R.layout.messageinfo_item_contact, (LinearLayout) v, false);
					viewHolder.contactName = (TextView) viewHolder.parent.findViewById(R.id.contact);
					viewHolder.contactAvatar  = (ImageView) viewHolder.parent.findViewById(R.id.avatar);
					viewHolder.timeStamp=(TextView)viewHolder.parent.findViewById(R.id.timestamp);

					break;
				case LIST_REMAINING_GROUP:
					v = inflater.inflate(R.layout.messageinfo_remaining_items, null);
					viewHolder.remainingItemsTextView = (TextView) v.findViewById(R.id.remainingItems);
					break;

				case LIST_ONE_TO_ONE:
					v = inflater.inflate(R.layout.messageinfo_list_onetoone, null);
					viewHolder.listheader = (TextView) v.findViewById(R.id.headerTextView);
					viewHolder.headerImageViewRight=(ImageView)v.findViewById(R.id.headerImageView);
					viewHolder.timeStamp=(TextView)v.findViewById(R.id.timestamp);

					Logger.d("refresh", "inflating adapter LIST_ONE_TO_ONE " + messageInfoItem + " position " + position);
					break;

			}

			v.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder) v.getTag();
		}

		switch (viewType)
		{
			case LIST_NAME:

				MessageInfoItem.MessageStatusHeader statusHeaderItem=((MessageInfoItem.MessageStatusHeader)messageInfoItem);
				String heading = statusHeaderItem.getHeaderString();
				if(viewHolder.listheader!=null)
				viewHolder.listheader.setText(heading);
				if(viewHolder.headerImageViewRight!=null){
					viewHolder.headerImageViewRight.setImageResource(statusHeaderItem.getDrawable());
				}
				Logger.d("refresh","adapter LISTNAME "+messageInfoItem+" position "+position);
				break;


			case LIST_ONE_TO_N_CONTACT:
				LinearLayout parentView = (LinearLayout) v;
				parentView.removeAllViews();
				MessageInfoItem.MesageInfoParticipantItem participant= (MessageInfoItem.MesageInfoParticipantItem) messageInfoItem;
				parentView.setBackgroundColor(Color.WHITE);

				ContactInfo contactInfo = participant.getContactInfo();

				Logger.d("MessageInfo","Adapter viewHolder "+viewHolder);

				Logger.d("MessageInfo","Adapter text "+viewHolder.text);
				Logger.d("MessageInfo", "Adapter extrainfo " + viewHolder.extraInfo);
				if(viewHolder.contactName!=null){
					viewHolder.contactName.setText(contactInfo.getNameOrMsisdn());
				}


				if(viewHolder.contactAvatar!=null)
				setAvatar(contactInfo.getMsisdn(), viewHolder.contactAvatar);
				if(viewHolder.timeStamp!=null){
					viewHolder.timeStamp.setText(participant.getDisplayedTimeStamp());

				}

				Logger.d("refresh", "Adapter parent" + viewHolder.parent);
				if(viewHolder.parent!=null)
				parentView.addView(viewHolder.parent);
				Logger.d("refresh","adapter LISTCONTACTGROUP "+messageInfoItem+" position "+position);
				break;
			case LIST_REMAINING_GROUP:

				MessageInfoItem.MesageInfoRemainingItem remainingItem=((MessageInfoItem.MesageInfoRemainingItem)messageInfoItem);
				String remaining = remainingItem.getRemainingItem();
				if(viewHolder.remainingItemsTextView!=null)
					viewHolder.remainingItemsTextView.setText(remaining);

				Logger.d("refresh","adapter LISTREMAINING "+messageInfoItem+" position "+position);
				break;
			case LIST_ONE_TO_ONE:
				MessageInfoItem.MessageInfoItemOnetoOne onetoOneList=((MessageInfoItem.MessageInfoItemOnetoOne)messageInfoItem);
				viewHolder.listheader.setText(onetoOneList.getHeader());
				viewHolder.headerImageViewRight.setBackgroundResource(onetoOneList.getDrawableheaderIcon());
				viewHolder.timeStamp.setText(onetoOneList.getDisplayedTimeStamp());
				Logger.d("refresh", "adapter LIST_ONE_TO_ONE " + messageInfoItem + " position " + position);
				break;

		}


		return v;
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
		//Views for Read List Played and Delivered List
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

	}

}