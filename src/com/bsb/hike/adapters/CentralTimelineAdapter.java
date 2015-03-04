package com.bsb.hike.adapters;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ImageViewerInfo;
import com.bsb.hike.models.Protip;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.smartImageLoader.TimelineImageLoader;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.PeopleActivity;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.StatusUpdate;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.WhichScreen;

public class CentralTimelineAdapter extends BaseAdapter
{

	public static final long EMPTY_STATUS_NO_STATUS_ID = -3;

	public static final long EMPTY_STATUS_NO_STATUS_RECENTLY_ID = -5;

	public static final long FTUE_ITEM_ID = -6;

	private int protipIndex;

	private List<StatusMessage> statusMessages;

	private Activity context;

	private String userMsisdn;

	private int mBigImageSize;

	private int mIconImageSize;

	private TimelineImageLoader bigPicImageLoader;

	private IconLoader iconImageLoader;

	private LayoutInflater inflater;

	private enum ViewType
	{
		PROFILE_PIC_CHANGE, OTHER_UPDATE, FTUE_ITEM, FTUE_CARD
	}

	public CentralTimelineAdapter(Activity context, List<StatusMessage> statusMessages, String userMsisdn)
	{
		this.context = context;
		mBigImageSize = context.getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size);
		mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		this.statusMessages = statusMessages;
		this.userMsisdn = userMsisdn;
		this.bigPicImageLoader = new TimelineImageLoader(context, mBigImageSize);
		this.iconImageLoader = new IconLoader(context, mIconImageSize);
		this.iconImageLoader.setDefaultAvatarIfNoCustomIcon(true);
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.protipIndex = -1;
	}

	@Override
	public int getCount()
	{
		return statusMessages.size();
	}

	@Override
	public StatusMessage getItem(int position)
	{
		return statusMessages.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean areAllItemsEnabled()
	{
		return false;
	}

	@Override
	public boolean isEnabled(int position)
	{
		ViewType viewType = ViewType.values()[getItemViewType(position)];
		if (viewType == ViewType.FTUE_ITEM)
		{
			return false;
		}
		else if (viewType == ViewType.OTHER_UPDATE)
		{
			StatusMessage statusMessage = getItem(position);
			if (EMPTY_STATUS_NO_STATUS_ID == statusMessage.getId() || EMPTY_STATUS_NO_STATUS_RECENTLY_ID == statusMessage.getId())
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public int getViewTypeCount()
	{
		return ViewType.values().length;
	}

	@Override
	public int getItemViewType(int position)
	{
		StatusMessage message = getItem(position);
		if (message.getId() == FTUE_ITEM_ID)
		{
			return ViewType.FTUE_ITEM.ordinal();
		}
		else if (message.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
		{
			return ViewType.PROFILE_PIC_CHANGE.ordinal();
		}
		else if (EMPTY_STATUS_NO_STATUS_ID == message.getId() || EMPTY_STATUS_NO_STATUS_RECENTLY_ID == message.getId())
		{
			return ViewType.FTUE_CARD.ordinal();
		}
		return ViewType.OTHER_UPDATE.ordinal();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ViewType viewType = ViewType.values()[getItemViewType(position)];

		final StatusMessage statusMessage = getItem(position);

		final ViewHolder viewHolder;
		if (convertView == null)
		{
			viewHolder = new ViewHolder();

			switch (viewType)
			{

			case OTHER_UPDATE:
				convertView = inflater.inflate(R.layout.timeline_item, null);

				viewHolder.avatar = (ImageView) convertView.findViewById(R.id.avatar);
				viewHolder.avatarFrame = (ImageView) convertView.findViewById(R.id.avatar_frame);

				viewHolder.name = (TextView) convertView.findViewById(R.id.name);
				viewHolder.mainInfo = (TextView) convertView.findViewById(R.id.main_info);
				viewHolder.extraInfo = (TextView) convertView.findViewById(R.id.details);
				viewHolder.timeStamp = (TextView) convertView.findViewById(R.id.timestamp);

				viewHolder.yesBtn = (TextView) convertView.findViewById(R.id.yes_btn);
				viewHolder.noBtn = (TextView) convertView.findViewById(R.id.no_btn);

				viewHolder.statusImg = (ImageView) convertView.findViewById(R.id.status_pic);

				viewHolder.buttonDivider = convertView.findViewById(R.id.button_divider);

				viewHolder.infoContainer = convertView.findViewById(R.id.btn_container);

				viewHolder.moodsContainer = (ViewGroup) convertView.findViewById(R.id.moods_container);

				viewHolder.parent = convertView.findViewById(R.id.main_content);
				break;

			case FTUE_ITEM:
				convertView = inflater.inflate(R.layout.ftue_updates_item, null);

				viewHolder.name = (TextView) convertView.findViewById(R.id.name);
				viewHolder.mainInfo = (TextView) convertView.findViewById(R.id.main_info);

				viewHolder.contactsContainer = (ViewGroup) convertView.findViewById(R.id.contacts_container);
				viewHolder.parent = convertView.findViewById(R.id.main_content);
				viewHolder.seeAll = (TextView) convertView.findViewById(R.id.see_all);

				break;
			case PROFILE_PIC_CHANGE:
				convertView = inflater.inflate(R.layout.profile_pic_timeline_item, null);

				viewHolder.avatar = (ImageView) convertView.findViewById(R.id.avatar);

				viewHolder.name = (TextView) convertView.findViewById(R.id.name);
				viewHolder.mainInfo = (TextView) convertView.findViewById(R.id.main_info);
				viewHolder.largeProfilePic = (ImageView) convertView.findViewById(R.id.profile_pic);
				viewHolder.timeStamp = (TextView) convertView.findViewById(R.id.timestamp);
				viewHolder.infoContainer = convertView.findViewById(R.id.info_container);
				viewHolder.parent = convertView.findViewById(R.id.main_content);
				break;
			case FTUE_CARD:
				convertView = inflater.inflate(R.layout.ftue_status_update_card_content, null);
				viewHolder.parent  = convertView.findViewById(R.id.main_content);
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
		case OTHER_UPDATE:

			viewHolder.avatar.setScaleType(ScaleType.FIT_CENTER);
			viewHolder.avatar.setBackgroundResource(0);

			if (statusMessage.getStatusMessageType() == StatusMessageType.PROTIP)
			{
				viewHolder.avatar.setImageResource(R.drawable.ic_protip);
				viewHolder.avatarFrame.setVisibility(View.GONE);
			}
			else if (statusMessage.hasMood())
			{
				viewHolder.avatar.setImageResource(EmoticonConstants.moodMapping.get(statusMessage.getMoodId()));
				viewHolder.avatarFrame.setVisibility(View.GONE);
			}
			else
			{
				setAvatar(statusMessage.getMsisdn(), viewHolder.avatar);
			}
			viewHolder.name.setText(userMsisdn.equals(statusMessage.getMsisdn()) ? "Me" : statusMessage.getNotNullName());

			viewHolder.mainInfo.setText(statusMessage.getText());

			viewHolder.timeStamp.setVisibility(View.VISIBLE);
			viewHolder.timeStamp.setText(statusMessage.getTimestampFormatted(true, context));

			viewHolder.statusImg.setVisibility(View.GONE);

			viewHolder.buttonDivider.setVisibility(View.VISIBLE);

			int padding = context.getResources().getDimensionPixelSize(R.dimen.status_btn_padding);
			viewHolder.noBtn.setPadding(padding, viewHolder.noBtn.getPaddingTop(), padding, viewHolder.noBtn.getPaddingTop());
			viewHolder.noBtn.setText(R.string.not_now);

			viewHolder.infoContainer.setVisibility(View.GONE);
			viewHolder.moodsContainer.setVisibility(View.GONE);

			switch (statusMessage.getStatusMessageType())
			{
			case NO_STATUS:
				viewHolder.infoContainer.setVisibility(View.VISIBLE);
				viewHolder.extraInfo.setVisibility(View.VISIBLE);
				viewHolder.yesBtn.setVisibility(View.VISIBLE);
				viewHolder.noBtn.setVisibility(View.GONE);

				viewHolder.yesBtn.setTag(statusMessage);
				viewHolder.yesBtn.setOnClickListener(yesBtnClickListener);
				break;
			case FRIEND_REQUEST:
				viewHolder.extraInfo.setVisibility(View.VISIBLE);
				viewHolder.yesBtn.setVisibility(View.VISIBLE);
				viewHolder.noBtn.setVisibility(View.VISIBLE);

				viewHolder.extraInfo.setText(context.getString(R.string.added_as_hike_friend_info, Utils.getFirstName(statusMessage.getNotNullName())));
				viewHolder.yesBtn.setText(R.string.confirm);
				viewHolder.noBtn.setText(R.string.no_thanks);
				break;
			case TEXT:
				viewHolder.extraInfo.setVisibility(View.GONE);
				viewHolder.yesBtn.setVisibility(View.GONE);
				viewHolder.noBtn.setVisibility(View.GONE);

				SmileyParser smileyParser = SmileyParser.getInstance();
				viewHolder.mainInfo.setText(smileyParser.addSmileySpans(statusMessage.getText(), true));
				Linkify.addLinks(viewHolder.mainInfo, Linkify.ALL);
				viewHolder.mainInfo.setMovementMethod(null);
				break;
			case FRIEND_REQUEST_ACCEPTED:
			case USER_ACCEPTED_FRIEND_REQUEST:
				viewHolder.yesBtn.setVisibility(View.GONE);
				viewHolder.noBtn.setVisibility(View.GONE);
				viewHolder.extraInfo.setVisibility(View.GONE);

				boolean friendRequestAccepted = statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST_ACCEPTED;

				viewHolder.mainInfo.setText(context.getString(friendRequestAccepted ? R.string.accepted_your_favorite_request_details
						: R.string.you_accepted_favorite_request_details, Utils.getFirstName(statusMessage.getNotNullName())));
				break;
			case PROTIP:
				Protip protip = statusMessage.getProtip();

				viewHolder.infoContainer.setVisibility(View.VISIBLE);

				viewHolder.buttonDivider.setVisibility(View.GONE);
				viewHolder.timeStamp.setVisibility(View.GONE);

				viewHolder.noBtn.setVisibility(View.VISIBLE);
				viewHolder.noBtn.setText(R.string.dismiss);
				viewHolder.yesBtn.setText(R.string.download);

				if (!TextUtils.isEmpty(protip.getText()))
				{
					viewHolder.extraInfo.setVisibility(View.VISIBLE);
					viewHolder.extraInfo.setText(protip.getText());

				}
				else
				{
					viewHolder.extraInfo.setVisibility(View.GONE);
				}

				if (!TextUtils.isEmpty(protip.getImageURL()))
				{

					ImageViewerInfo imageViewerInfo = new ImageViewerInfo(statusMessage.getMappedId(), protip.getImageURL(), true);
					viewHolder.statusImg.setTag(imageViewerInfo);
					viewHolder.statusImg.setOnClickListener(imageClickListener);
					bigPicImageLoader.loadImage(protip.getMappedId(), viewHolder.statusImg, isListFlinging);
					viewHolder.statusImg.setVisibility(View.VISIBLE);
				}
				else
				{
					viewHolder.statusImg.setVisibility(View.GONE);
				}
				if (!TextUtils.isEmpty(protip.getGameDownlodURL()))
				{
					viewHolder.yesBtn.setTag(statusMessage);
					viewHolder.yesBtn.setVisibility(View.VISIBLE);
					viewHolder.buttonDivider.setVisibility(View.VISIBLE);
					viewHolder.yesBtn.setOnClickListener(yesBtnClickListener);
				}
				else
				{
					viewHolder.yesBtn.setVisibility(View.GONE);
				}

				Linkify.addLinks(viewHolder.mainInfo, Linkify.ALL);
				viewHolder.mainInfo.setMovementMethod(null);

				Linkify.addLinks(viewHolder.extraInfo, Linkify.ALL);
				viewHolder.mainInfo.setMovementMethod(null);
				break;
			}

			viewHolder.avatar.setTag(statusMessage);

			viewHolder.noBtn.setTag(statusMessage);
			viewHolder.noBtn.setOnClickListener(noBtnClickListener);

			break;

		case PROFILE_PIC_CHANGE:
			setAvatar(statusMessage.getMsisdn(), viewHolder.avatar);
			viewHolder.name.setText(userMsisdn.equals(statusMessage.getMsisdn()) ? "Me" : statusMessage.getNotNullName());
			viewHolder.mainInfo.setText(R.string.status_profile_pic_notification);

			ImageViewerInfo imageViewerInfo = new ImageViewerInfo(statusMessage.getMappedId(), null, true);

			viewHolder.largeProfilePic.setTag(imageViewerInfo);
			viewHolder.largeProfilePic.setOnClickListener(imageClickListener);

			/*
			 * Fetch larger image
			 */
			bigPicImageLoader.loadImage(statusMessage.getMappedId(), viewHolder.largeProfilePic, isListFlinging);

			viewHolder.timeStamp.setText(statusMessage.getTimestampFormatted(true, context));

			viewHolder.infoContainer.setTag(statusMessage);
			viewHolder.infoContainer.setOnClickListener(onProfileInfoClickListener);
			break;

		case FTUE_ITEM:
			viewHolder.name.setText(R.string.favorites_ftue_item_label);
			viewHolder.mainInfo.setText(R.string.ftue_updates_are_fun_with_favorites);

			viewHolder.contactsContainer.removeAllViews();

			int limit = HikeConstants.FTUE_LIMIT;

			View parentView = null;
			for (ContactInfo contactInfo : HomeActivity.ftueContactsData.getCompleteList())
			{
				FavoriteType favoriteType = contactInfo.getFavoriteType();
				if (favoriteType == FavoriteType.FRIEND || favoriteType == FavoriteType.REQUEST_SENT || favoriteType == FavoriteType.REQUEST_SENT_REJECTED
						|| favoriteType == FavoriteType.REQUEST_RECEIVED)
				{
					continue;
				}

				parentView = inflater.inflate(R.layout.ftue_recommended_list_item, parent, false);

				ImageView avatar = (ImageView) parentView.findViewById(R.id.avatar);
				TextView name = (TextView) parentView.findViewById(R.id.contact);
				TextView status = (TextView) parentView.findViewById(R.id.info);
				ImageView addFriendBtn = (ImageView) parentView.findViewById(R.id.add_friend);
				addFriendBtn.setVisibility(View.VISIBLE);
				parentView.findViewById(R.id.add_friend_divider).setVisibility(View.VISIBLE);

				setAvatar(contactInfo.getMsisdn(), avatar);

				name.setText(contactInfo.getName());
				status.setText(contactInfo.getMsisdn());

				addFriendBtn.setTag(contactInfo);
				addFriendBtn.setOnClickListener(addOnClickListener);

				viewHolder.contactsContainer.addView(parentView);
				
				parentView.setTag(contactInfo);
				parentView.setOnClickListener(ftueListItemClickListener);

				if (--limit == 0)
				{
					break;
				}
			}
			viewHolder.seeAll.setText(R.string.see_all_upper_caps);
			viewHolder.seeAll.setOnClickListener(seeAllBtnClickListener);
			break;
		case FTUE_CARD:
			if (EMPTY_STATUS_NO_STATUS_ID == statusMessage.getId() || EMPTY_STATUS_NO_STATUS_RECENTLY_ID == statusMessage.getId())
			{
				viewHolder.parent.setTag(statusMessage);
				viewHolder.parent.setOnClickListener(yesBtnClickListener);
			}
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

		return convertView;
	}

	private void setAvatar(String msisdn, ImageView avatar)
	{
		iconImageLoader.loadImage(msisdn, true, avatar, true);
	}

	private class ViewHolder
	{
		ImageView avatar;

		ImageView avatarFrame;

		TextView name;

		TextView mainInfo;

		TextView extraInfo;

		TextView timeStamp;

		TextView yesBtn;

		TextView noBtn;

		ImageView statusImg;

		View buttonDivider;

		ImageView largeProfilePic;

		View infoContainer;

		View parent;

		ViewGroup contactsContainer;

		ViewGroup moodsContainer;
		
		TextView seeAll;
	}

	private OnClickListener imageClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			ImageViewerInfo imageViewerInfo = (ImageViewerInfo) v.getTag();

			String mappedId = imageViewerInfo.mappedId;
			String url = imageViewerInfo.url;

			Bundle arguments = new Bundle();
			arguments.putString(HikeConstants.Extras.MAPPED_ID, mappedId);
			arguments.putString(HikeConstants.Extras.URL, url);
			arguments.putBoolean(HikeConstants.Extras.IS_STATUS_IMAGE, true);

			HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_IMAGE, arguments);

		}
	};

	private OnClickListener yesBtnClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			StatusMessage statusMessage = (StatusMessage) v.getTag();
			if (EMPTY_STATUS_NO_STATUS_ID == statusMessage.getId() || EMPTY_STATUS_NO_STATUS_RECENTLY_ID == statusMessage.getId())
			{
				Intent intent = new Intent(context, StatusUpdate.class);
				context.startActivity(intent);

				try 
				{
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.POST_UPDATE_FROM_CARD);
					HAManager.getInstance().record(HikeConstants.UI_EVENT, HikeConstants.LogEvent.CLICK, metadata);
				}
				catch (JSONException e) 
				{
					Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}				
			}
			else if (statusMessage.getStatusMessageType() == StatusMessageType.PROTIP)
			{
				Protip protip = statusMessage.getProtip();
				String url = protip.getGameDownlodURL();
				Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				try
				{
					context.startActivity(marketIntent);
				}
				catch (ActivityNotFoundException e)
				{
					Logger.e(CentralTimelineAdapter.class.getSimpleName(), "Unable to open market");
				}
				HikeMessengerApp.getPubSub().publish(HikePubSub.GAMING_PROTIP_DOWNLOADED, protip);
			}
		}
	};

	private OnClickListener noBtnClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			StatusMessage statusMessage = (StatusMessage) v.getTag();
			if (statusMessage.getStatusMessageType() == StatusMessageType.PROTIP)
			{
				/*
				 * Removing the protip
				 */
				try
				{
					statusMessages.remove(getProtipIndex());
					notifyDataSetChanged();

					Editor editor = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
					editor.putLong(HikeMessengerApp.CURRENT_PROTIP, -1);
					editor.commit();

					HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_PROTIP, statusMessage.getProtip().getMappedId());
				}
				catch (IndexOutOfBoundsException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
	};

	private OnClickListener onProfileInfoClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			StatusMessage statusMessage = (StatusMessage) v.getTag();
			if ((statusMessage.getStatusMessageType() == StatusMessageType.NO_STATUS) || (statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST)
					|| (statusMessage.getStatusMessageType() == StatusMessageType.PROTIP))
			{
				return;
			}
			else if (userMsisdn.equals(statusMessage.getMsisdn()))
			{
				Intent intent = new Intent(context, ProfileActivity.class);
				intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
				context.startActivity(intent);
				return;
			}

			Intent intent = Utils.createIntentFromContactInfo(new ContactInfo(null, statusMessage.getMsisdn(), statusMessage.getNotNullName(), statusMessage.getMsisdn()), true);
			intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
			intent.setClass(context, ChatThread.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			context.startActivity(intent);
			context.finish();
		}
	};

	private OnClickListener addOnClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			ContactInfo contactInfo = (ContactInfo) v.getTag();

			Utils.addFavorite(context, contactInfo, true);

			ContactInfo contactInfo2 = new ContactInfo(contactInfo);

			try 
			{
				JSONObject metadata = new JSONObject();
				
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ADD_UPDATES_CLICK);
			
				String msisdn = contactInfo2.getMsisdn();
			
				if(TextUtils.isEmpty(msisdn))
				{
					metadata.put(HikeConstants.TO, msisdn);
				}
				HAManager.getInstance().record(HikeConstants.UI_EVENT, HikeConstants.LogEvent.CLICK, metadata);
			}
			catch (JSONException e) 
			{
				Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}

			if (!contactInfo.isOnhike())
				Utils.sendInviteUtil(contactInfo2, context, HikeConstants.FTUE_ADD_SMS_ALERT_CHECKED, context.getString(R.string.ftue_add_prompt_invite_title),
						context.getString(R.string.ftue_add_prompt_invite), WhichScreen.UPDATES_TAB);
		}
	};
	
	private OnClickListener seeAllBtnClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(context, PeopleActivity.class);
			context.startActivity(intent);
			
			try 
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.FTUE_FAV_CARD_SEEL_ALL_CLICKED);
				HAManager.getInstance().record(HikeConstants.UI_EVENT, HikeConstants.LogEvent.CLICK, metadata);
			}
			catch (JSONException e) 
			{
				Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}
		}
	};
	
	private OnClickListener ftueListItemClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			ContactInfo contactInfo = (ContactInfo) v.getTag();

			Utils.startChatThread(context, contactInfo);
						
			try 
			{
				JSONObject metadata = new JSONObject();

				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.FTUE_FAV_CARD_START_CHAT_CLICKED);
			
				String msisdn = contactInfo.getMsisdn();
			
				if(TextUtils.isEmpty(msisdn))
				{
					metadata.put(HikeConstants.TO, msisdn);
				}
				HAManager.getInstance().record(HikeConstants.UI_EVENT, HikeConstants.LogEvent.CLICK, metadata);
			}
			catch (JSONException e) 
			{
				Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}

			context.finish();
		}
	};

	/**
	 * @return the protipIndex
	 */
	public int getProtipIndex()
	{
		return protipIndex;
	}

	/**
	 * @param protipIndex
	 *            the protipIndex to set
	 */
	public void setProtipIndex(int protipIndex)
	{
		this.protipIndex = protipIndex;
	}

	public TimelineImageLoader getTimelineImageLoader()
	{
		return bigPicImageLoader;
	}

	public IconLoader getIconImageLoader()
	{
		return iconImageLoader;
	}

	private boolean isListFlinging;

	public void setIsListFlinging(boolean b)
	{
		boolean notify = b != isListFlinging;

		isListFlinging = b;

		if (notify && !isListFlinging)
		{
			notifyDataSetChanged();
		}
	}
}
