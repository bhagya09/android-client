package com.bsb.hike.timeline.adapter;

import java.lang.ref.SoftReference;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.RecyclerViewCursorAdapter;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ImageViewerInfo;
import com.bsb.hike.models.Protip;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.smartImageLoader.TimelineUpdatesImageLoader;
import com.bsb.hike.timeline.model.FeedDataModel;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.RoundedImageView;

public class ActivityFeedCardCursorAdapter extends RecyclerViewCursorAdapter<ActivityFeedCardCursorAdapter.ViewHolder>
{

	private final int PROFILE_PIC_CHANGE = 3;

	private final int OTHER_UPDATE = -12;

	private final int IMAGE = 1;
	
	private final int TEXT_IMAGE = 0;

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

		ImageView statusImg;

		ImageView largeProfilePic;

		View infoContainer;

		View parent;

		ViewGroup contactsContainer;
		
		ImageView moodImage;

		ViewGroup moodsContainer;
		
		ImageView loveStatus;

		public ViewHolder(View convertView, int viewType)
		{
			super(convertView);

			// Common views
			parent = convertView.findViewById(R.id.main_content);
			name = (TextView) convertView.findViewById(R.id.name);
			mainInfo = (TextView) convertView.findViewById(R.id.main_info);

			//Grab view references
			switch (viewType)
			{
			case OTHER_UPDATE:
				avatar = (ImageView) convertView.findViewById(R.id.avatar);
				avatarFrame = (ImageView) convertView.findViewById(R.id.avatar_frame);
				extraInfo = (TextView) convertView.findViewById(R.id.details);
				timeStamp = (TextView) convertView.findViewById(R.id.timestamp);
				statusImg = (ImageView) convertView.findViewById(R.id.status_pic);
				infoContainer = convertView.findViewById(R.id.btn_container);
				moodsContainer = (ViewGroup) convertView.findViewById(R.id.moods_container);
				break;
			
			case PROFILE_PIC_CHANGE:
			case IMAGE:
			case TEXT_IMAGE:
				avatar = (ImageView) convertView.findViewById(R.id.avatar);
				largeProfilePic = (ImageView) convertView.findViewById(R.id.profile_pic);
				timeStamp = (TextView) convertView.findViewById(R.id.timestamp);
				infoContainer = convertView.findViewById(R.id.info_container);
				moodImage = (ImageView) convertView.findViewById(R.id.mood_pic);
				loveStatus = (ImageView)convertView.findViewById(R.id.love_status);
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

	private SoftReference<Activity> mActivity;

	private int mLastPosition = 3;

	private static final String TAG = "ActivityFeedCardCursorAdapter";
	
    public ActivityFeedCardCursorAdapter(Activity activity, Cursor c, int flags) 
    {
        super(HikeMessengerApp.getInstance().getApplicationContext(), c, flags);
        mContext = HikeMessengerApp.getInstance().getApplicationContext();
		profileImageLoader = new TimelineUpdatesImageLoader(mContext, mContext.getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size));
		mIconImageLoader = new IconLoader(mContext, mContext.getResources().getDimensionPixelSize(R.dimen.icon_picture_size));
		mIconImageLoader.setDefaultAvatarIfNoCustomIcon(true);
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mProtipIndex = -1;
		mActivity = new SoftReference<Activity>(activity);
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
		return OTHER_UPDATE;
	}

	@Override
	public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor)
	{
		StatusMessage statusMessage = new StatusMessage(cursor);

		FeedDataModel feedDataModel = new FeedDataModel(cursor);

		int viewType = getItemViewType(cursor.getPosition()); 
				
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
			viewHolder.name.setText(mUserMsisdn.equals(statusMessage.getMsisdn()) ? HikeMessengerApp.getInstance().getString(R.string.me) : statusMessage.getNotNullName());

			viewHolder.mainInfo.setText(statusMessage.getText());

			viewHolder.timeStamp.setVisibility(View.VISIBLE);
			viewHolder.timeStamp.setText(statusMessage.getTimestampFormatted(true, mContext));

			viewHolder.statusImg.setVisibility(View.GONE);

			int padding = mContext.getResources().getDimensionPixelSize(R.dimen.status_btn_padding);

			viewHolder.infoContainer.setVisibility(View.GONE);
			viewHolder.moodsContainer.setVisibility(View.GONE);

			switch (statusMessage.getStatusMessageType())
			{
			case NO_STATUS:
				viewHolder.infoContainer.setVisibility(View.VISIBLE);
				viewHolder.extraInfo.setVisibility(View.VISIBLE);
				break;
			case FRIEND_REQUEST:
				viewHolder.extraInfo.setVisibility(View.VISIBLE);

				viewHolder.extraInfo.setText(mContext.getString(R.string.added_as_hike_friend_info, Utils.getFirstName(statusMessage.getNotNullName())));
				break;
			case TEXT:
				viewHolder.extraInfo.setVisibility(View.GONE);

				SmileyParser smileyParser = SmileyParser.getInstance();
				viewHolder.mainInfo.setText(smileyParser.addSmileySpans(statusMessage.getText(), true));
				Linkify.addLinks(viewHolder.mainInfo, Linkify.ALL);
				viewHolder.mainInfo.setMovementMethod(null);
				viewHolder.parent.setTag(statusMessage);
				viewHolder.parent.setOnClickListener(onProfileInfoClickListener);
				break;
			case FRIEND_REQUEST_ACCEPTED:
			case USER_ACCEPTED_FRIEND_REQUEST:
				viewHolder.extraInfo.setVisibility(View.GONE);

				boolean friendRequestAccepted = statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST_ACCEPTED;

				int infoMainResId = friendRequestAccepted ? R.string.accepted_your_favorite_request_details : R.string.you_accepted_favorite_request_details;
				String infoSubText = mContext.getString(Utils.isLastSeenSetToFavorite() ? R.string.both_ls_status_update : R.string.status_updates_proper_casing);
				viewHolder.mainInfo.setText(mContext.getString(infoMainResId, Utils.getFirstName(statusMessage.getNotNullName()), infoSubText));
				break;
			case PROTIP:
				Protip protip = statusMessage.getProtip();

				viewHolder.infoContainer.setVisibility(View.VISIBLE);

				viewHolder.timeStamp.setVisibility(View.GONE);

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
					//viewHolder.statusImg.setOnClickListener(imageClickListener);
					// TODO
					// profileImageLoader.loadImage(protip.getMappedId(), viewHolder.statusImg, isListFlinging);
					profileImageLoader.loadImage(protip.getMappedId(), viewHolder.statusImg, false);
					viewHolder.statusImg.setVisibility(View.VISIBLE);
				}
				else
				{
					viewHolder.statusImg.setVisibility(View.GONE);
				}
				if (!TextUtils.isEmpty(protip.getGameDownlodURL()))
				{
					//viewHolder.buttonDivider.setVisibility(View.VISIBLE);
				}
				else
				{
				}

				Linkify.addLinks(viewHolder.mainInfo, Linkify.ALL);
				viewHolder.mainInfo.setMovementMethod(null);

				Linkify.addLinks(viewHolder.extraInfo, Linkify.ALL);
				viewHolder.mainInfo.setMovementMethod(null);
				break;
			case JOINED_HIKE:
				break;
			default:
				break;
			}

			viewHolder.avatar.setTag(statusMessage);

			break;

		case IMAGE:
		case TEXT_IMAGE:
		case PROFILE_PIC_CHANGE:
			RoundedImageView roundAvatar1 = (RoundedImageView) viewHolder.avatar;
			roundAvatar1.setScaleType(ScaleType.FIT_CENTER);
			roundAvatar1.setBackgroundResource(0);
			
			roundAvatar1.setOval(true);
			setAvatar(feedDataModel.getActor(), viewHolder.avatar);
			
			viewHolder.name.setText(mUserMsisdn.equals(feedDataModel.getActor()) ? HikeMessengerApp.getInstance().getApplicationContext().getString(R.string.me) : 
				ContactManager.getInstance().getContact(feedDataModel.getActor()).getFirstNameAndSurname());

			viewHolder.timeStamp.setText(feedDataModel.getTimestampFormatted(true, mContext));
			
			if(feedDataModel.getReadStatus() == 1)
			{
				viewHolder.loveStatus.setImageResource(R.drawable.ic_lovedit);
			}
			
			if (statusMessage.hasMood())
			{
				// For moods we dont want to use rounded corners
				viewHolder.moodImage.setImageResource(EmoticonConstants.moodMapping.get(statusMessage.getMoodId()));
				viewHolder.moodImage.setVisibility(View.VISIBLE);
			}
				
			if (TextUtils.isEmpty(statusMessage.getText()))
			{
				viewHolder.mainInfo.setText(R.string.status_profile_pic_notification);
			}
			else
			{
				viewHolder.mainInfo.setText(statusMessage.getText());
			}

			ImageViewerInfo imageViewerInfo = new ImageViewerInfo(statusMessage.getMappedId(), null, true);

			viewHolder.largeProfilePic.setTag(imageViewerInfo);

			profileImageLoader.loadImage(statusMessage.getMappedId(), viewHolder.largeProfilePic, false, false, false, statusMessage);

			viewHolder.infoContainer.setTag(statusMessage);
			viewHolder.infoContainer.setOnClickListener(onProfileInfoClickListener);
			break;
		}

		if (cursor.getPosition() >= mLastPosition)
		{
			Animator[] anims = getAnimators(viewHolder.itemView);
			int length = anims.length;
			for (int i = length; i > 0; i--)
			{
				Animator anim = anims[i - 1];
				anim.setInterpolator(cardInterp);
				anim.setDuration(500).start();
			}
			mLastPosition = cursor.getPosition();
		}

	}

	private DecelerateInterpolator cardInterp = new DecelerateInterpolator();

	protected Animator[] getAnimators(View view)
	{
		return new Animator[] { ObjectAnimator.ofFloat(view, "alpha", 0, 1f), ObjectAnimator.ofFloat(view, "translationY", 200, 0) };
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		View convertView = null;

		switch (viewType)
		{
		case OTHER_UPDATE:
			convertView = mInflater.inflate(R.layout.timeline_item, parent, false);
			return new ViewHolder(convertView, viewType);
		case PROFILE_PIC_CHANGE:
			convertView = mInflater.inflate(R.layout.activity_feed_item, parent, false);
			return new ViewHolder(convertView, viewType);
		case IMAGE:
			convertView = mInflater.inflate(R.layout.activity_feed_item, parent, false);
			return new ViewHolder(convertView, viewType);
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
			else if (mUserMsisdn.equals(statusMessage.getMsisdn()))
			{
				Intent intent = new Intent(mContext, ProfileActivity.class);
				intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
				startActivity(intent);
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
			startActivity(intent);
			// TODO
			// mContext.finish();
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

}
