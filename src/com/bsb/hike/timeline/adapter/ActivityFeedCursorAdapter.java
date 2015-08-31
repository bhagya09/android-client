package com.bsb.hike.timeline.adapter;

import java.lang.ref.WeakReference;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.RecyclerViewCursorAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.ImageViewerInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.smartImageLoader.TimelineUpdatesImageLoader;
import com.bsb.hike.timeline.model.FeedDataModel;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.timeline.view.TimelineSummaryActivity;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.RoundedImageView;

public class ActivityFeedCursorAdapter extends RecyclerViewCursorAdapter<ActivityFeedCursorAdapter.ViewHolder>
{

	private final int PROFILE_PIC_CHANGE = 3;

	private final int IMAGE = 1;

	private final int TEXT_IMAGE = 2;

	private final int TEXT = 0;

	class ViewHolder extends RecyclerView.ViewHolder
	{
		ImageView avatar;

		ImageView avatarFrame;

		TextView name;

		TextView mainInfo;

		TextView extraInfo;

		ImageView statusImg;

		ImageView largeProfilePic;

		View parent;

		ViewGroup contactsContainer;

		ImageView loveStatus;

		public ViewHolder(View convertView, int viewType)
		{
			super(convertView);

			// Common views
			parent = convertView.findViewById(R.id.main_content);
			name = (TextView) convertView.findViewById(R.id.name);
			mainInfo = (TextView) convertView.findViewById(R.id.main_info);

			// Grab view references
			switch (viewType)
			{
			case TEXT:
			case PROFILE_PIC_CHANGE:
			case IMAGE:
			case TEXT_IMAGE:
				avatar = (ImageView) convertView.findViewById(R.id.avatar);
				largeProfilePic = (ImageView) convertView.findViewById(R.id.profile_pic);
				loveStatus = (ImageView) convertView.findViewById(R.id.love_status);
				break;
			}

		}
	}

	private Context mContext;

	private LayoutInflater mInflater;

	private IconLoader mIconImageLoader;

	private TimelineUpdatesImageLoader profileImageLoader;

	private String mUserMsisdn;

	private int mProtipIndex;

	private WeakReference<Activity> mActivity;

	public ActivityFeedCursorAdapter(Activity activity, Cursor c, int flags)
	{
		super(HikeMessengerApp.getInstance().getApplicationContext(), c, flags);
		mContext = HikeMessengerApp.getInstance().getApplicationContext();
		profileImageLoader = new TimelineUpdatesImageLoader(mContext, mContext.getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size));
		mIconImageLoader = new IconLoader(mContext, mContext.getResources().getDimensionPixelSize(R.dimen.icon_picture_size));
		mIconImageLoader.setDefaultAvatarIfNoCustomIcon(true);
		mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mProtipIndex = -1;
		mActivity = new WeakReference<Activity>(activity);
		mUserMsisdn = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, "");
	}

	@Override
	public int getItemViewType(int position)
	{
		mCursor.moveToPosition(position);
		StatusMessage message = new StatusMessage(mCursor);
		if (message.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
		{
			return PROFILE_PIC_CHANGE;
		}
		else if (message.getStatusMessageType() == StatusMessageType.IMAGE)
		{
			return IMAGE;
		}
		else if (message.getStatusMessageType() == StatusMessageType.TEXT_IMAGE)
		{
			return TEXT_IMAGE;
		}
		return TEXT;
	}

	@Override
	public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor)
	{
		StatusMessage statusMessage = new StatusMessage(cursor);

		FeedDataModel feedDataModel = new FeedDataModel(cursor);
		
		int viewType = getItemViewType(cursor.getPosition());

		switch (viewType)
		{
		case TEXT:
		case IMAGE:
		case TEXT_IMAGE:
		case PROFILE_PIC_CHANGE:
			RoundedImageView roundAvatar1 = (RoundedImageView) viewHolder.avatar;
			roundAvatar1.setScaleType(ScaleType.FIT_XY);
			roundAvatar1.setBackgroundResource(0);
			viewHolder.name.setText(mUserMsisdn.equals(feedDataModel.getActor()) ? HikeMessengerApp.getInstance().getApplicationContext().getString(R.string.me) : ContactManager
					.getInstance().getContact(feedDataModel.getActor(), true, true).getFirstNameAndSurname());

			if (feedDataModel.getReadStatus() == 1)
			{
				viewHolder.loveStatus.setVisibility(View.GONE);
			}
			else
			{
				viewHolder.loveStatus.setVisibility(View.VISIBLE);
			}

			roundAvatar1.setOval(true);
			setAvatar(feedDataModel.getActor(), viewHolder.avatar);
			
			if (viewType == TEXT)
			{
				SmileyParser smileyParser = SmileyParser.getInstance();
				viewHolder.mainInfo.setText(smileyParser.addSmileySpans(mContext.getString(R.string.liked_your_post) +", " +statusMessage.getText()+". "+ Utils.getFormattedTime(true, mContext, feedDataModel.getTimestamp()), true));
				Linkify.addLinks(viewHolder.mainInfo, Linkify.ALL);
				viewHolder.mainInfo.setMovementMethod(null);
				viewHolder.largeProfilePic.setVisibility(View.GONE);

			}
			else if (viewType == IMAGE || viewType == TEXT_IMAGE)
			{
				viewHolder.mainInfo.setText(mContext.getString(R.string.photo_like_text) +". " + Utils.getFormattedTime(true, mContext, feedDataModel.getTimestamp()));
				viewHolder.largeProfilePic.setVisibility(View.VISIBLE);
				ImageViewerInfo imageViewerInfo = new ImageViewerInfo(statusMessage.getMappedId(), null, true);
				viewHolder.largeProfilePic.setTag(imageViewerInfo);
				profileImageLoader.loadImage(statusMessage.getMappedId(), viewHolder.largeProfilePic, false, false, false, statusMessage);

			}
			else
			{
				viewHolder.mainInfo.setText(mContext.getString(R.string.dp_like_text) +". " + Utils.getFormattedTime(true, mContext, feedDataModel.getTimestamp()));
				viewHolder.largeProfilePic.setVisibility(View.VISIBLE);
				ImageViewerInfo imageViewerInfo = new ImageViewerInfo(statusMessage.getMappedId(), null, true);
				viewHolder.largeProfilePic.setTag(imageViewerInfo);
				profileImageLoader.loadImage(statusMessage.getMappedId(), viewHolder.largeProfilePic, false, false, false, statusMessage);

			}

			viewHolder.parent.setTag(statusMessage);
			viewHolder.parent.setTag(R.id.activity_feed_item_key, feedDataModel.getActor());
			viewHolder.parent.setOnClickListener(onProfileInfoClickListener);
			break;
		}

	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		View convertView = null;

		switch (viewType)
		{
		case TEXT:
		case PROFILE_PIC_CHANGE:
		case IMAGE:
		case TEXT_IMAGE:
			convertView = mInflater.inflate(R.layout.activity_feed_item, parent, false);
			return new ViewHolder(convertView, viewType);
		default:
			return null;
		}
	}

	private void setAvatar(String msisdn, ImageView avatar)
	{
		mIconImageLoader.loadImage(msisdn, avatar, false, true);
	}

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

			if (StealthModeManager.getInstance().isStealthMsisdn(statusMessage.getMsisdn()))
			{
				if (!StealthModeManager.getInstance().isActive())
				{
					return;
				}
			}

			if (mActivity.get() != null)
			{
				Intent intent = new Intent(mActivity.get(), TimelineSummaryActivity.class);
				intent.putExtra(HikeConstants.Extras.MAPPED_ID, statusMessage.getMappedId());

				startActivity(intent);
				
				JSONObject metadata = new JSONObject();
				try
				{
					String postType = getPostType(statusMessage);
					metadata.put(AnalyticsConstants.EVENT_SOURCE, postType);
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ACTIVITY_FEED_ITEM_CLICKED);
					metadata.put(HikeConstants.MSISDN, v.getTag(R.id.activity_feed_item_key));
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, HAManager.EventPriority.HIGH, metadata);
				}
				catch (JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}
			}
		}
	};

	private void startActivity(Intent argIntent)
	{
		if (mActivity.get() != null)
		{
			mActivity.get().startActivity(argIntent);
		}
	}

	@Override
	protected void onContentChanged()
	{

	}

	public static String getPostType(StatusMessage statusMessage)
	{
		switch (statusMessage.getStatusMessageType())
		{
		case PROFILE_PIC:
			return AnalyticsConstants.DISPLAY_PIC;

		case IMAGE:
			return AnalyticsConstants.PICTURE_UPDATE;

		case TEXT_IMAGE:
			return AnalyticsConstants.PICTURE_TEXT;

		case TEXT:
			return AnalyticsConstants.STATUS_UPDATE;
		
		default:
			break;
		}
		return null;
	}
}
