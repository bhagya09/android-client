package com.bsb.hike.adapters;

import java.lang.ref.SoftReference;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
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
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.ImageViewerActivity;
import com.bsb.hike.ui.PeopleActivity;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.StatusUpdate;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.WhichScreen;
import com.bsb.hike.view.RoundedImageView;

public class TimelineCardsAdapter extends RecyclerView.Adapter<TimelineCardsAdapter.ViewHolder>
{
	private final int PROFILE_PIC_CHANGE = -11;

	private final int OTHER_UPDATE = -12;

	private final int FTUE_ITEM = -13;

	private final int FTUE_CARD = -14;

	public static final long EMPTY_STATUS_NO_STATUS_ID = -3;

	public static final long EMPTY_STATUS_NO_STATUS_RECENTLY_ID = -5;

	public static final long FTUE_ITEM_ID = -6;

	class ViewHolder extends RecyclerView.ViewHolder
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

		public ViewHolder(View convertView, int viewType)
		{
			super(convertView);

			// Common views
			parent = convertView.findViewById(R.id.main_content);
			name = (TextView) convertView.findViewById(R.id.name);
			mainInfo = (TextView) convertView.findViewById(R.id.main_info);

			switch (viewType)
			{
			case OTHER_UPDATE:
				avatar = (ImageView) convertView.findViewById(R.id.avatar);
				avatarFrame = (ImageView) convertView.findViewById(R.id.avatar_frame);
				extraInfo = (TextView) convertView.findViewById(R.id.details);
				timeStamp = (TextView) convertView.findViewById(R.id.timestamp);
				yesBtn = (TextView) convertView.findViewById(R.id.yes_btn);
				noBtn = (TextView) convertView.findViewById(R.id.no_btn);
				statusImg = (ImageView) convertView.findViewById(R.id.status_pic);
				buttonDivider = convertView.findViewById(R.id.button_divider);
				infoContainer = convertView.findViewById(R.id.btn_container);
				moodsContainer = (ViewGroup) convertView.findViewById(R.id.moods_container);
				break;
			case FTUE_ITEM:
				contactsContainer = (ViewGroup) convertView.findViewById(R.id.contacts_container);
				seeAll = (TextView) convertView.findViewById(R.id.see_all);
				break;
			case PROFILE_PIC_CHANGE:
				avatar = (ImageView) convertView.findViewById(R.id.avatar);
				largeProfilePic = (ImageView) convertView.findViewById(R.id.profile_pic);
				timeStamp = (TextView) convertView.findViewById(R.id.timestamp);
				infoContainer = convertView.findViewById(R.id.info_container);
				break;
			case FTUE_CARD:
				break;
			}
		}
	}

	private Context mContext;

	private List<StatusMessage> mStatusMessages;

	private LayoutInflater mInflater;

	private IconLoader mIconImageLoader;

	private TimelineImageLoader bigPicImageLoader;

	private String mUserMsisdn;

	private int mProtipIndex;

	private SoftReference<Activity> mActivity;

	private int mLastPosition = 3;

	public TimelineCardsAdapter(Activity activity, List<StatusMessage> statusMessages, String userMsisdn)
	{
		mContext = HikeMessengerApp.getInstance().getApplicationContext();
		mStatusMessages = statusMessages;
		mUserMsisdn = userMsisdn;
		bigPicImageLoader = new TimelineImageLoader(mContext, mContext.getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size));
		mIconImageLoader = new IconLoader(mContext, mContext.getResources().getDimensionPixelSize(R.dimen.icon_picture_size));
		mIconImageLoader.setDefaultAvatarIfNoCustomIcon(true);
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mProtipIndex = -1;
		mActivity = new SoftReference<Activity>(activity);
	}

	@Override
	public int getItemCount()
	{
		return mStatusMessages.size();
	}

	@Override
	public int getItemViewType(int position)
	{
		StatusMessage message = mStatusMessages.get(position);
		if (message.getId() == FTUE_ITEM_ID)
		{
			return FTUE_ITEM;
		}
		else if (message.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
		{
			return PROFILE_PIC_CHANGE;
		}
		else if (EMPTY_STATUS_NO_STATUS_ID == message.getId() || EMPTY_STATUS_NO_STATUS_RECENTLY_ID == message.getId())
		{
			return FTUE_CARD;
		}
		return OTHER_UPDATE;
	}

	@Override
	public void onBindViewHolder(ViewHolder viewHolder, int position)
	{
		StatusMessage statusMessage = mStatusMessages.get(position);

		int viewType = getItemViewType(position);

		switch (viewType)
		{
		case OTHER_UPDATE:
			RoundedImageView roundAvatar = (RoundedImageView) viewHolder.avatar;
			roundAvatar.setScaleType(ScaleType.FIT_CENTER);
			roundAvatar.setBackgroundResource(0);

			if (statusMessage.getStatusMessageType() == StatusMessageType.PROTIP)
			{
				roundAvatar.setImageResource(R.drawable.ic_protip);
				viewHolder.avatarFrame.setVisibility(View.GONE);
			}
			else if (statusMessage.hasMood())
			{
				// For moods we dont want to use rounded corners
				roundAvatar.setOval(false);
				roundAvatar.setImageResource(EmoticonConstants.moodMapping.get(statusMessage.getMoodId()));
				viewHolder.avatarFrame.setVisibility(View.GONE);
			}
			else
			{
				roundAvatar.setOval(true);
				setAvatar(statusMessage.getMsisdn(), viewHolder.avatar);
			}
			viewHolder.name.setText(mUserMsisdn.equals(statusMessage.getMsisdn()) ? "Me" : statusMessage.getNotNullName());

			viewHolder.mainInfo.setText(statusMessage.getText());

			viewHolder.timeStamp.setVisibility(View.VISIBLE);
			viewHolder.timeStamp.setText(statusMessage.getTimestampFormatted(true, mContext));

			viewHolder.statusImg.setVisibility(View.GONE);

			viewHolder.buttonDivider.setVisibility(View.VISIBLE);

			int padding = mContext.getResources().getDimensionPixelSize(R.dimen.status_btn_padding);
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

				viewHolder.extraInfo.setText(mContext.getString(R.string.added_as_hike_friend_info, Utils.getFirstName(statusMessage.getNotNullName())));
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
				viewHolder.parent.setTag(statusMessage);
				viewHolder.parent.setOnClickListener(onProfileInfoClickListener);
				break;
			case FRIEND_REQUEST_ACCEPTED:
			case USER_ACCEPTED_FRIEND_REQUEST:
				viewHolder.yesBtn.setVisibility(View.GONE);
				viewHolder.noBtn.setVisibility(View.GONE);
				viewHolder.extraInfo.setVisibility(View.GONE);

				boolean friendRequestAccepted = statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST_ACCEPTED;

				int infoMainResId = friendRequestAccepted ? R.string.accepted_your_favorite_request_details : R.string.you_accepted_favorite_request_details;
				String infoSubText = mContext.getString(Utils.isLastSeenSetToFavorite() ? R.string.both_ls_status_update : R.string.status_updates_proper_casing);
				viewHolder.mainInfo.setText(mContext.getString(infoMainResId, Utils.getFirstName(statusMessage.getNotNullName()), infoSubText));
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
					// TODO
					// bigPicImageLoader.loadImage(protip.getMappedId(), viewHolder.statusImg, isListFlinging);
					bigPicImageLoader.loadImage(protip.getMappedId(), viewHolder.statusImg, false);
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
			case IMAGE:
				break;
			case JOINED_HIKE:
				break;
			case PROFILE_PIC:
				break;
			case TEXT_IMAGE:
				break;
			default:
				break;
			}

			viewHolder.avatar.setTag(statusMessage);

			viewHolder.noBtn.setTag(statusMessage);
			viewHolder.noBtn.setOnClickListener(noBtnClickListener);

			break;

		case PROFILE_PIC_CHANGE:
			setAvatar(statusMessage.getMsisdn(), viewHolder.avatar);
			viewHolder.name.setText(mUserMsisdn.equals(statusMessage.getMsisdn()) ? "Me" : statusMessage.getNotNullName());
			viewHolder.mainInfo.setText(R.string.status_profile_pic_notification);

			ImageViewerInfo imageViewerInfo = new ImageViewerInfo(statusMessage.getMappedId(), null, true);

			viewHolder.largeProfilePic.setTag(imageViewerInfo);
			viewHolder.largeProfilePic.setOnClickListener(imageClickListener);

			/*
			 * Fetch larger image
			 */
			// TODO
			// bigPicImageLoader.loadImage(statusMessage.getMappedId(), viewHolder.largeProfilePic, isListFlinging);
			bigPicImageLoader.loadImage(statusMessage.getMappedId(), viewHolder.largeProfilePic, false);

			viewHolder.timeStamp.setText(statusMessage.getTimestampFormatted(true, mContext));

			viewHolder.infoContainer.setTag(statusMessage);
			viewHolder.infoContainer.setOnClickListener(onProfileInfoClickListener);
			break;

		case FTUE_ITEM:
			viewHolder.name.setText(R.string.favorites_ftue_item_label);
			String infoSubText = mContext.getString(Utils.isLastSeenSetToFavorite() ? R.string.both_ls_status_update : R.string.status_updates_proper_casing);
			viewHolder.mainInfo.setText(mContext.getString(R.string.ftue_updates_are_fun_with_favorites, infoSubText));

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

				parentView = mInflater.inflate(R.layout.ftue_recommended_list_item, null, false);

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
		
		if (position >= mLastPosition)
		{
			Animator[] anims = getAnimators(viewHolder.itemView);
			int length = anims.length;
			for (int i = length; i > 0; i--)
			{
				Animator anim = anims[i-1];
				anim.setInterpolator(cardInterp);
				anim.setDuration(500).start();
			}
			mLastPosition = position;
		}
	}

	private DecelerateInterpolator cardInterp = new DecelerateInterpolator();

	protected Animator[] getAnimators(View view)
	{
		return new Animator[] { ObjectAnimator.ofFloat(view, "alpha", 0, 1f), ObjectAnimator.ofFloat(view, "translationY", 200, 0) };
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup arg0, int viewType)
	{
		View convertView = null;

		switch (viewType)
		{
		case OTHER_UPDATE:
			convertView = mInflater.inflate(R.layout.timeline_item, null);
			return new ViewHolder(convertView, viewType);
		case FTUE_ITEM:
			convertView = mInflater.inflate(R.layout.ftue_updates_item, null);
			return new ViewHolder(convertView, viewType);
		case PROFILE_PIC_CHANGE:
			convertView = mInflater.inflate(R.layout.profile_pic_timeline_item, null);
			return new ViewHolder(convertView, viewType);
		case FTUE_CARD:
			convertView = mInflater.inflate(R.layout.ftue_status_update_card_content, null);
			return new ViewHolder(convertView, viewType);
		default:
			return null;
		}
	}

	private void setAvatar(String msisdn, ImageView avatar)
	{
		mIconImageLoader.loadImage(msisdn, avatar, false, true);
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
			
            int[] screenLocation = new int[2];
            v.getLocationOnScreen(screenLocation);
			arguments.putInt(ImageViewerActivity.animFromLeft, screenLocation[0]);
			arguments.putInt(ImageViewerActivity.animFromTop , screenLocation[1]);
			arguments.putInt(ImageViewerActivity.animFromWidth , v.getWidth());
			arguments.putInt(ImageViewerActivity.animFromHeight , v.getHeight());

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
				Intent intent = new Intent(mContext, StatusUpdate.class);
				mActivity.get().startActivity(intent);

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
					mActivity.get().startActivity(marketIntent);
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
					mStatusMessages.remove(getProtipIndex());
					notifyDataSetChanged();

					Editor editor = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
					editor.putLong(HikeMessengerApp.CURRENT_PROTIP, -1);
					editor.commit();

					HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_PROTIP, statusMessage.getProtip().getMappedId());
				}
				catch (IndexOutOfBoundsException e)
				{
					e.printStackTrace();
				}

			}
		}

	};

	/**
	 * @return the mProtipIndex
	 */
	public int getProtipIndex()
	{
		return mProtipIndex;
	}

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
			else if (mUserMsisdn.equals(statusMessage.getMsisdn()))
			{
				Intent intent = new Intent(mContext, ProfileActivity.class);
				intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
				mActivity.get().startActivity(intent);
				return;
			}

			if (StealthModeManager.getInstance().isStealthMsisdn(statusMessage.getMsisdn()))
			{
				if (!StealthModeManager.getInstance().isActive())
				{
					return;
				}
			}

			Intent intent = IntentFactory.createChatThreadIntentFromContactInfo(mContext, new ContactInfo(null, statusMessage.getMsisdn(), statusMessage.getNotNullName(),
					statusMessage.getMsisdn()), true);
			// Add anything else to the intent
			intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			mActivity.get().startActivity(intent);
			// TODO
			// mContext.finish();
		}
	};

	private OnClickListener addOnClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			ContactInfo contactInfo = (ContactInfo) v.getTag();

			Utils.addFavorite(mContext, contactInfo, true);

			ContactInfo contactInfo2 = new ContactInfo(contactInfo);

			try
			{
				JSONObject metadata = new JSONObject();

				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ADD_UPDATES_CLICK);

				String msisdn = contactInfo2.getMsisdn();

				if (TextUtils.isEmpty(msisdn))
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
				Utils.sendInviteUtil(contactInfo2, mContext, HikeConstants.FTUE_ADD_SMS_ALERT_CHECKED, mContext.getString(R.string.ftue_add_prompt_invite_title),
						mContext.getString(R.string.ftue_add_prompt_invite), WhichScreen.UPDATES_TAB);
		}
	};

	private OnClickListener seeAllBtnClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(mContext, PeopleActivity.class);
			mActivity.get().startActivity(intent);

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

			Utils.startChatThread(mContext, contactInfo);

			try
			{
				JSONObject metadata = new JSONObject();

				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.FTUE_FAV_CARD_START_CHAT_CLICKED);

				String msisdn = contactInfo.getMsisdn();

				if (TextUtils.isEmpty(msisdn))
				{
					metadata.put(HikeConstants.TO, msisdn);
				}
				HAManager.getInstance().record(HikeConstants.UI_EVENT, HikeConstants.LogEvent.CLICK, metadata);
			}
			catch (JSONException e)
			{
				Logger.e(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}
			// TODO
			// mContext.finish();
		}
	};

	public void setProtipIndex(int startIndex)
	{
		mProtipIndex = startIndex;
	}

}
