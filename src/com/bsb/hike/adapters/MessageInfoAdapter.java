package com.bsb.hike.adapters;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.messageinfo.MessageInfoDataModel;
import com.bsb.hike.messageinfo.MessageInfoItem;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ProfileItem;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.smartImageLoader.ProfilePicImageLoader;
import com.bsb.hike.smartImageLoader.SharedFileImageLoader;
import com.bsb.hike.smartImageLoader.TimelineUpdatesImageLoader;
import com.bsb.hike.ui.MessageInfoActivity;
import com.bsb.hike.utils.Logger;

public class MessageInfoAdapter extends ArrayAdapter<MessageInfoItem>
{
	public static final String OPEN_GALLERY = "OpenGallery";

	public static final String IMAGE_TAG = "image";

	public static enum ViewType
	{
		HEADER,CONTACT_INFORMATION
	}

	//TODO: Need to find a better way
	public static final int READLIST=1;
	public static final int DELIVEREDLIST=2;
	public static final int PLAYEDLIST=3;
	public static final int READ_CONTACT =4;
	public static final int DELIVERED_CONTACT =5;
	public static final int PLAYED_CONTACT =6;
	private Context context;

	private MessageInfoActivity messageInfoActivity;


	private Bitmap profilePreview;

	private boolean groupProfile;

	private boolean myProfile;

	private boolean isContactBlocked;

	private IconLoader iconLoader;

	private TimelineUpdatesImageLoader bigPicImageLoader;

	private ProfilePicImageLoader profileImageLoader;

	private SharedFileImageLoader thumbnailLoader;

	private int mIconImageSize;

	private boolean hasCustomPhoto;

	private int sizeOfThumbnail;

	private ConvMessage message;

	private MessageInfoDataModel dataModel;
	public MessageInfoAdapter(MessageInfoActivity messageInfoActivity,List<MessageInfoItem> itemList)
	{
		super(messageInfoActivity,-1,itemList);
		context=messageInfoActivity;
		int mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		this.iconLoader = new IconLoader(context, mIconImageSize);
		iconLoader.setDefaultAvatarIfNoCustomIcon(true);
	}




	@Override
	public int getItemViewType(int position)
	{
		ViewType viewType;
		MessageInfoItem profileItem = getItem(position);
		int itemId = profileItem.getItemId();
		if(MessageInfoItem.HEADER_ID==itemId)
		{
			viewType = ViewType.HEADER;
		}
		else
		{
			viewType=ViewType.CONTACT_INFORMATION;
		}

		return viewType.ordinal();
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		ViewType viewType = ViewType.values()[getItemViewType(position)];

		MessageInfoItem messageInfoItem = getItem(position);

		View v = convertView;

		ViewHolder viewHolder = null;

		if (v == null)
		{
			viewHolder = new ViewHolder();

			switch (viewType)
			{
				case HEADER:
					v = inflater.inflate(R.layout.messageinfo_header_try, null);
					viewHolder.header = (TextView) v.findViewById(R.id.headerTextView);
					break;

				case CONTACT_INFORMATION:
					v = new LinearLayout(context);
					viewHolder.parent = inflater.inflate(R.layout.group_profile_item, (LinearLayout) v, false);
					viewHolder.text = (TextView) viewHolder.parent.findViewById(R.id.name);
					viewHolder.icon  = (ImageView) viewHolder.parent.findViewById(R.id.avatar);
					viewHolder.iconFrame = (ImageView) viewHolder.parent.findViewById(R.id.avatar_frame);
					viewHolder.infoContainer = viewHolder.parent.findViewById(R.id.owner_indicator);
					viewHolder.phoneNumViewDivider = viewHolder.parent.findViewById(R.id.divider);
					viewHolder.extraInfo = (TextView) viewHolder.parent.findViewById(R.id.telephone);

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
			case HEADER:

				String heading = ((MessageInfoItem.MessageStatusHeader)messageInfoItem).getHeaderString();
				if(viewHolder.header!=null)
				viewHolder.header.setText(heading);

				break;


			case CONTACT_INFORMATION:
				LinearLayout parentView = (LinearLayout) v;
				parentView.removeAllViews();
				MessageInfoItem.MesageInfoParticipantItem participant= (MessageInfoItem.MesageInfoParticipantItem) messageInfoItem;
				parentView.setBackgroundColor(Color.WHITE);

				ContactInfo contactInfo = participant.getContactInfo();

				Logger.d("MessageInfo","Adapter viewHolder "+viewHolder);

				Logger.d("MessageInfo","Adapter text "+viewHolder.text);
				Logger.d("MessageInfo", "Adapter extrainfo " + viewHolder.extraInfo);

				if(viewHolder.text!=null)
				viewHolder.text.setText(participant.getInfo());
				if(viewHolder.extraInfo!=null)
				viewHolder.extraInfo.setText(participant.getTimeStampDescription());


				if(viewHolder.icon!=null)
				setAvatar(contactInfo.getMsisdn(), viewHolder.icon);

				Logger.d("MessageInfo", "Adapter parent" + viewHolder.parent);
				if(viewHolder.parent!=null)
				parentView.addView(viewHolder.parent);

				break;


		}

		if (viewHolder.parent != null)
		{
			int bottomPadding;

			if (position == getCount() - 1)
			{
				bottomPadding = context.getResources().getDimensionPixelSize(R.dimen.updates_margin);
			}
			else
			{
				bottomPadding = 0;
			}

			viewHolder.parent.setPadding(0, 0, 0, bottomPadding);
		}

		return v;
	}



	private void setAvatar(String msisdn, ImageView avatarView)
	{
		iconLoader.loadImage(msisdn, avatarView, false, true);
	}

	private class ViewHolder
	{
		TextView header;

		TextView text;

		TextView subText;

		TextView extraInfo;

		ImageView image;

		ImageView icon;

		ImageView iconFrame;

		ImageView phoneIcon;

		TextView timeStamp;

		View infoContainer;

		View parent;

		View phoneNumViewDivider;

	}

	public void setProfilePreview(Bitmap preview)
	{
		this.profilePreview = preview;
		notifyDataSetChanged();
	}



}