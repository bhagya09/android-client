package com.bsb.hike.messageinfo;

import android.content.Context;
import android.view.View;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessageInfoAdapter;
import com.bsb.hike.analytics.ChatAnalyticConstants;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import java.util.List;

/**
 * Created by ravi on 4/21/16.
 */

public abstract class MessageInfoItem
{

	public static final int HEADER_ID = -1;

	// TODO: Need to find a better way
	public static final int READLIST = 1;

	public static final int DELIVEREDLIST = 2;

	public static final int PLAYEDLIST = 3;

	public static final int READ_CONTACT = 4;

	public static final int DELIVERED_CONTACT = 5;

	public static final int PLAYED_CONTACT = 6;

	public static final int REMAINING_ITEM = 7;

	public static final int MESSAGE_VIEW =8;

	private int type = DELIVEREDLIST;

	public int viewType ;

	protected Context mContext = HikeMessengerApp.getInstance().getApplicationContext();

	private int itemId;

	private Object text;

	public void onClick(){
		return;
	}

	public MessageInfoItem(int itemId, Object text, int viewType)
	{
		this.itemId = itemId;
		this.text = text;
		this.viewType = viewType;
	}

	public MessageInfoItem(int itemId, Object text)
	{
		this.itemId = itemId;
		this.text = text;
	}

	public int getViewType()
	{
		return viewType;
	}

	public int getItemId()
	{
		return itemId;
	}

	public Object getText()
	{
		return text;
	}

	public static int getSize(){

		return HikeMessengerApp.getInstance().getApplicationContext().getResources().getDimensionPixelSize(R.dimen.icon_picture_size_messageinfo);
	}

	public static class MessageStatusHeader extends MessageInfoItem
	{

		private String headerString;

		private int drawableRight;

		public MessageStatusHeader(String headerString, int drawableRight)
		{
			super(HEADER_ID, null, MessageInfoAdapter.LIST_NAME);
			this.headerString = headerString;
			this.drawableRight = drawableRight;
		}

		public void setHeaderString(String headerString)
		{
			this.headerString = headerString;
		}

		public String getHeaderString()
		{
			return headerString;
		}

		public int getDrawable()
		{
			return drawableRight;
		}
	}

	public static class MessageInfoItemOnetoOne extends MessageInfoItem
	{
		String header;

		long timeStamp;

		int drawableheaderIcon;

		public MessageInfoItemOnetoOne(int itemId, String header, int drawableheaderIcon)
		{
			super(itemId, header);
			this.header = header;
			this.drawableheaderIcon = drawableheaderIcon;
			viewType = MessageInfoAdapter.LIST_ONE_TO_ONE;
		}

		public void setTimeStamp(long timeStamp)
		{
			this.timeStamp = timeStamp;
		}

		public String getDisplayedTimeStamp()
		{
			if (timeStamp == 0)
				return "-";
			return Utils.getFormattedTimeinMessageInfo(timeStamp);
		}

		public String getHeader()
		{
			return header;
		}

		public int getDrawableheaderIcon()
		{
			return drawableheaderIcon;
		}
	}

	public static class MesageInfoParticipantItem extends MessageInfoItem
	{

		private int type;

		private String name;

		private String msisdn;

		private long readTimeStamp, deliveryTimeStamp, playedTimeStamp;

		private String formattedReadTimeStamp, formattedDeliveryTimeStamp, formattedPlayedTimeStamp;

		MessageInfoDataModel.MessageInfoParticipantData participantData;

		public MesageInfoParticipantItem(MessageInfoDataModel.MessageInfoParticipantData participantData, int type, int viewType)
		{
			super(0, participantData.contactInfo.getMsisdn(), viewType);
			this.participantData = participantData;
			readTimeStamp = participantData.getReadTimeStamp();
			deliveryTimeStamp = participantData.getDeliveredTimeStamp();
			playedTimeStamp = participantData.getPlayedTimeStamp();
			msisdn=participantData.getContactInfo().getMsisdn();
			this.type = type;
			this.viewType = viewType;

			// TODO Auto-generated constructor stub

		}
		public int getHashCode(){
			int h=msisdn.hashCode()+type;
			return h;
		}
		public int getType(){
			return type;
		}
		public static final int NO_DIVIDER=0;
		public static final int SMALL_DIVIDER=1;
		public static final int BIG_DIVIDER=2;
		int dividerBehavior=SMALL_DIVIDER;
		public void setDividerBehavior(int dividerBehavior){
			this.dividerBehavior=dividerBehavior;
		}
		public void applyDividerBehavior(View view){
			if(view==null)
				return ;
			switch (dividerBehavior){
				case NO_DIVIDER:
					view.findViewById(R.id.dividermiddle).setVisibility(View.GONE);
					view.findViewById(R.id.dividerend).setVisibility(View.GONE);
					break;
				case SMALL_DIVIDER:
					view.findViewById(R.id.dividermiddle).setVisibility(View.VISIBLE);
					view.findViewById(R.id.dividerend).setVisibility(View.GONE);
					break;
				case BIG_DIVIDER:
					view.findViewById(R.id.dividerend).setVisibility(View.VISIBLE);
					view.findViewById(R.id.dividermiddle).setVisibility(View.GONE);
					break;
			}
		}
		public String getDisplayedTimeStamp()
		{
			// Type t=Type.values()[type];
			switch (type)
			{
			case READ_CONTACT:
				return getReadTimeStamp();
			default:
				return getDeliveredTimeStamp();
			}
		}

		public void setReadTimeStamp(long readTimeStamp)
		{
			this.readTimeStamp = readTimeStamp;
		}

		public void setDeliveryTimeStamp(long deliveryTimeStamp)
		{
			this.deliveryTimeStamp = deliveryTimeStamp;
		}

		public void setPlayedTimeStamp(long playedTimeStamp)
		{
			this.playedTimeStamp = playedTimeStamp;
		}

		public String getReadTimeStamp()
		{
		//	return Utils.getFormattedTime(false, mContext, participantData.getReadTimeStamp());
			return Utils.getFormattedTimeinMessageInfo(participantData.getReadTimeStamp());
		}

		public String getDeliveredTimeStamp()
		{
			//return Utils.getFormattedTime(false, mContext, participantData.getDeliveredTimeStamp());
			return Utils.getFormattedTimeinMessageInfo(participantData.getDeliveredTimeStamp());
		}

		public String getPlayedTimeStamp()
		{
			return Utils.getFormattedTime(false, mContext, participantData.getPlayedTimeStamp());
		}

		public String getInfo()
		{
			return participantData.getContactInfo().getNameOrMsisdn();
		}

		public boolean hasbeenDelivered()
		{
			return participantData.hasBeenDelivered();
		}

		public boolean hasRead()
		{
			return participantData.hasRead();
		}

		public boolean hasPlayed()
		{
			return participantData.hasBeenPlayed();
		}

		public ContactInfo getContactInfo()
		{
			return participantData.contactInfo;
		}

		public String getMsisdn(){
			return msisdn;
		}

		public String getTimeStampDescription()
		{
			String desc = "None";
			Logger.d("MessageInfo", " read " + readTimeStamp + " delivery timestamp " + deliveryTimeStamp + " played Time " + playedTimeStamp);
			switch (type)
			{
			case READ_CONTACT:
				if (readTimeStamp == 0)
					desc = "Not yet";
				else
					desc = "Read at " + getReadTimeStamp();
				break;
			case PLAYED_CONTACT:
				if (playedTimeStamp == 0)
					desc = "Not yet";
				else
					desc = "Played at " + getPlayedTimeStamp();
				break;
			case DELIVERED_CONTACT:
				if (deliveryTimeStamp == 0)
					desc = "Not delivered yet";
				else
					desc = "Delivered at " + getDeliveredTimeStamp();
				break;
			default:
				desc = "No activity";

			}
			return desc;
		}

	}
	 public static class MessageInfoViewItem extends MessageInfoItem{
		 public MessageInfoViewItem(ConvMessage convMessage){
			 super(0,"MessageInfoView",MessageInfoAdapter.MESSAGE_INFO_VIEW);


		 }
	 }
	public static class MessageInfoSMSItem extends MessageInfoItem{
		public MessageInfoSMSItem(){
			super(0,"MessageInfoSMS",MessageInfoAdapter.MESSAGE_INFO_SMS);

		}
	}
	public static class MessageInfoNotApplicableItem extends MessageInfoItem{
		public MessageInfoNotApplicableItem(){
			super(0,"MessageInfoSMS",MessageInfoAdapter.MESSAGE_INFO_NOTAPPLICABLE);

		}
	}
	public static class MessageInfoEmptyItem extends MessageInfoItem{
		public MessageInfoEmptyItem(){
			super(0,"MessageInfoSMS",MessageInfoAdapter.MESSAGE_INFO_EMPTY);

		}
	}
	public static class MesageInfoReadItem extends MessageInfoItem
	{

		private String name;

		private String number;

		public MesageInfoReadItem(int itemId, String name, String number, int viewType)
		{
			super(itemId, name, viewType);
			this.name = name;
			this.number = number;
			// TODO Auto-generated constructor stub
		}

	}

	public static class MesageInfoRemainingItem extends MessageInfoItem
	{

		private int countTotal;

		private int emptyStateText;

		public List<MessageInfoDataModel.MessageInfoParticipantData> remainingItemList;

		public  static final int READ_REMAINING=1;

		public static final int DELIVERED_REMAINING=2;

		public  int listType=READ_REMAINING;


		public MesageInfoRemainingItem(int itemId, int countTotal, int viewType, int emptyStateText,int listType)
		{
			super(itemId, countTotal, viewType);
			this.countTotal = countTotal;
			this.emptyStateText = emptyStateText;
			this.listType=listType;
			// TODO Auto-generated constructor stub
		}

		public String getRemainingItem()
		{
			if (remainingItemList.size() == countTotal&&listType==READ_REMAINING)
			{
				return mContext.getResources().getString(emptyStateText);
			}
			return mContext.getString(R.string.remaining_items, remainingItemList.size());
		}

		public void setEmptyStateText(int emptyStateText)
		{
			this.emptyStateText = emptyStateText;
		}

		public boolean shouldshowList(){
			return remainingItemList.size()!=countTotal||listType==DELIVERED_REMAINING;
		}
		public String getType(){
			if(listType==READ_REMAINING)
				return ChatAnalyticConstants.MessageInfoEvents.READ_REMAINING_ANALYTICS;
			else
				return ChatAnalyticConstants.MessageInfoEvents.DELIVERED_REMAINING_ANALYTICS;
		}


	}

	@Override
	public String toString()
	{
		return getClass().getName();
	}
}