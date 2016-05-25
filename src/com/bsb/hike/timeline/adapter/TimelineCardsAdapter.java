package com.bsb.hike.timeline.adapter;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.imageHttp.HikeImageDownloader;
import com.bsb.hike.imageHttp.HikeImageWorker.TaskCallbacks;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ImageViewerInfo;
import com.bsb.hike.models.Protip;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.smartImageLoader.TimelineUpdatesImageLoader;
import com.bsb.hike.timeline.LoveCheckBoxToggleListener;
import com.bsb.hike.timeline.TimelineActionsManager;
import com.bsb.hike.timeline.model.ActionsDataModel;
import com.bsb.hike.timeline.model.ActionsDataModel.ActionTypes;
import com.bsb.hike.timeline.model.ActionsDataModel.ActivityObjectTypes;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.timeline.view.StatusUpdate;
import com.bsb.hike.timeline.view.TimelineSummaryActivity;
import com.bsb.hike.timeline.view.UpdatesFragment;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.HikeUiHandler;
import com.bsb.hike.utils.HikeUiHandler.IHandlerCallback;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.ProfileImageLoader;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.RoundedImageView;

public class TimelineCardsAdapter extends RecyclerView.Adapter<TimelineCardsAdapter.ViewHolder> implements IHandlerCallback, TaskCallbacks, Listener
{
	private final int PROFILE_PIC_CHANGE = -11;

	private final int OTHER_UPDATE = -12;

	public final static int FTUE_CARD_INIT = -14;

	public final static int FTUE_CARD_EXIT = -17;

	public final static int FTUE_CARD_FAV = -18;

	public final static int USER_PROFILE_HEADER = -19;

	private final int IMAGE = -15;

	private final int TEXT_IMAGE = -16;

	public static final long EMPTY_STATUS_NO_STATUS_ID = -3;

	public static final long EMPTY_STATUS_NO_STATUS_RECENTLY_ID = -5;

	private ProfileImageLoader profileLoader;

	protected AlertDialog alertDialog;

	private HashSet<String> mSUViewedSet = new HashSet<String>();

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

		CheckBox checkBoxLove;

		View ftueShow;

		View actionsLayout;

		TextView textBtnLove;

		ImageView cancelFTUE;

		View profileBtnDivider;

		ImageView changeProfile;

		View cardView;

		TextView ftueBottomText;

		ImageView favIcon;

		public ViewHolder(View convertView, int viewType)
		{
			super(convertView);

			// Common views
			parent = convertView.findViewById(R.id.main_content);
			name = (TextView) convertView.findViewById(R.id.name);
			mainInfo = (TextView) convertView.findViewById(R.id.main_info);
			checkBoxLove = (CheckBox) convertView.findViewById(R.id.btn_love);
			actionsLayout = convertView.findViewById(R.id.actions_layout);
			textBtnLove = (TextView) convertView.findViewById(R.id.text_btn_love);
			cardView = convertView.findViewById(R.id.card_view);

			// Grab view references
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
			case PROFILE_PIC_CHANGE:
			case IMAGE:
			case TEXT_IMAGE:
				avatar = (ImageView) convertView.findViewById(R.id.avatar);
				largeProfilePic = (ImageView) convertView.findViewById(R.id.profile_pic);
				timeStamp = (TextView) convertView.findViewById(R.id.timestamp);
				infoContainer = convertView.findViewById(R.id.info_container);
				break;
			case FTUE_CARD_INIT:
				ftueShow = convertView.findViewById(R.id.ftue_show);
				break;
			case FTUE_CARD_EXIT:
				ftueShow = convertView.findViewById(R.id.ftue_show);
				break;
			case FTUE_CARD_FAV:
				largeProfilePic = (ImageView) convertView.findViewById(R.id.dp_big);
				avatar = (ImageView) convertView.findViewById(R.id.avatar);
				ftueShow = convertView.findViewById(R.id.ftue_show);
				cancelFTUE = (ImageView) convertView.findViewById(R.id.remove_ftue);
				ftueBottomText = (TextView) convertView.findViewById(R.id.addfavtext);
				favIcon = (ImageView) convertView.findViewById(R.id.ftue_fav_icon);
				break;
			case USER_PROFILE_HEADER:
				largeProfilePic = (ImageView) convertView.findViewById(R.id.profile_pic);
				extraInfo = (TextView) convertView.findViewById(R.id.details);
				profileBtnDivider = convertView.findViewById(R.id.button_divider);
				changeProfile = (ImageView) convertView.findViewById(R.id.change_profile);
			}

		}
	}

	private Context mContext;

	private List<StatusMessage> mStatusMessages;

	private LayoutInflater mInflater;

	private IconLoader mIconImageLoader;

	private TimelineUpdatesImageLoader timelineImageLoader;

	private String mUserMsisdn;

	private int mProtipIndex;

	private SoftReference<Activity> mActivity;

	private List<ContactInfo> mFtueFriendList;

	private LoaderManager loaderManager;

	private HikeUiHandler mHikeUiHandler;

	private boolean isShowCountEnabled;

	private boolean mShowUserProfile;

	private ArrayList<String> mFilteredMsisdns;

	private HikeImageDownloader mImageDownloader;

	private ContactInfo profileContactInfo;

	private boolean isDestroyed = false;

	public TimelineCardsAdapter(Activity activity, List<StatusMessage> statusMessages, String userMsisdn, List<ContactInfo> ftueFriendList, LoaderManager loadManager,
			boolean showUserProfile, ArrayList<String> filterMsisdns)
	{
		mContext = HikeMessengerApp.getInstance().getApplicationContext();
		mStatusMessages = statusMessages;
		mUserMsisdn = userMsisdn;
		mShowUserProfile = showUserProfile;
		timelineImageLoader = new TimelineUpdatesImageLoader(mContext, mContext.getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size));
		mIconImageLoader = new IconLoader(mContext, mContext.getResources().getDimensionPixelSize(R.dimen.icon_picture_size));
		mIconImageLoader.setDefaultAvatarIfNoCustomIcon(true);
		mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mProtipIndex = -1;
		mActivity = new SoftReference<Activity>(activity);
		mFtueFriendList = ftueFriendList;
		loaderManager = loadManager;
		mHikeUiHandler = new HikeUiHandler(this);
		isShowCountEnabled = Utils.isTimelineShowCountEnabled();
		mFilteredMsisdns = filterMsisdns;

		if (mShowUserProfile)
		{
			profileContactInfo = ContactManager.getInstance().getContact(filterMsisdns.get(0), true, true);
		}

		HikeMessengerApp.getPubSub().addListener(HikePubSub.FAVORITE_TOGGLED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.DELETE_STATUS, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.ACTIVITY_UPDATE, this);
	}

	@Override
	public int getItemCount()
	{
		int size = 0;
		if (mShowUserProfile)
		{
			size = (1 + mStatusMessages.size());
		}
		else
		{
			size = mStatusMessages.size();
		}
		
		if(size == 0)
		{
			Message emptyMessage = Message.obtain();
			emptyMessage.arg1 = UpdatesFragment.EMPTY_STATE;
			this.handleUIMessage(emptyMessage);
		}
		else // Need to send a message in-case if list is not empty to remove empty state
		{
			Message emptyMessage = Message.obtain();
			emptyMessage.arg1 = UpdatesFragment.FILL_STATE;
			this.handleUIMessage(emptyMessage);
		}
		
		return size;
	}

	@Override
	public int getItemViewType(int position)
	{
		if(mShowUserProfile && position == 0)
		{
			return USER_PROFILE_HEADER;
		}
		
		if(mShowUserProfile)
		{
			position -= 1;
		}
		
		StatusMessage message = mStatusMessages.get(position);
		if (message.getId() == FTUE_CARD_INIT)
		{
			return FTUE_CARD_INIT;
		}
		if (message.getId() == FTUE_CARD_FAV)
		{
			return FTUE_CARD_FAV;
		}
		if (message.getId() == FTUE_CARD_EXIT)
		{
			return FTUE_CARD_EXIT;
		}
		else if (message.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
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
	public void onBindViewHolder(ViewHolder viewHolder, int position)
	{
		int viewType = getItemViewType(position);//viewHolder.getItemViewType();
		Logger.d(HikeConstants.TIMELINE_LOGS, "view type is " + viewType + ", itemID " + viewHolder.getItemViewType() + ", position "+ position);

		if (viewType == USER_PROFILE_HEADER)
		{
			final String headerMsisdn = mFilteredMsisdns.get(0);
			if (mActivity.get() != null)
			{
				String mapedId = headerMsisdn + ProfileActivity.PROFILE_PIC_SUFFIX;

				ProfileImageLoader profileImageLoader = new ProfileImageLoader(mActivity.get(), headerMsisdn, viewHolder.largeProfilePic, mContext.getResources()
						.getDimensionPixelSize(R.dimen.timeine_profile_picture_size), false, true);
				profileImageLoader.loadProfileImage(loaderManager);

				ImageViewerInfo imageViewerInf = new ImageViewerInfo(mapedId, headerMsisdn, false, !ContactManager.getInstance().hasIcon(headerMsisdn));
				viewHolder.largeProfilePic.setTag(imageViewerInf);

				viewHolder.largeProfilePic.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						onViewImageClicked(v);
					}
				});
			}

			boolean isMyProfile = mUserMsisdn.equals(headerMsisdn);
			
			ContactInfo mHeaderConInfo = ContactManager.getInstance().getContact(headerMsisdn, true, false);

			viewHolder.name.setText(isMyProfile ? HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.NAME_SETTING, "Me") :mHeaderConInfo.getNameOrMsisdn());

			if (!isMyProfile)
			{
				viewHolder.changeProfile.setImageResource(R.drawable.ic_action_message);
				viewHolder.changeProfile.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						if (mActivity.get() != null && mActivity.get() instanceof ProfileActivity)
						{
							Intent intent = IntentFactory.createChatThreadIntentFromContactInfo(mActivity.get(),
									ContactManager.getInstance().getContact(headerMsisdn, true, false), false, false,
									ChatThreadActivity.ChatThreadOpenSources.TIMELINE);
							startActivity(intent);
						}
					}
				});
				viewHolder.extraInfo.setText(headerMsisdn);
			}
			else
			{
				viewHolder.extraInfo.setText(headerMsisdn);
				viewHolder.changeProfile.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						if (mActivity.get() != null && mActivity.get() instanceof ProfileActivity)
						{
							ProfileActivity profileActivity = (ProfileActivity) mActivity.get();
							profileActivity.changeProfilePicture();
						}
					}
				});
			}

			return;
		}

		if (mShowUserProfile)
		{
			position -= 1;
		}

		StatusMessage statusMessage = mStatusMessages.get(position);

		ActionsDataModel likesData = TimelineActionsManager.getInstance().getActionsData().getActions(statusMessage.getMappedId(), ActionTypes.LIKE, ActivityObjectTypes.STATUS_UPDATE);

		statusMessage.setActionsData(likesData);

		if(!TextUtils.isEmpty(statusMessage.getMappedId()))
		{
			mSUViewedSet.add(statusMessage.getMappedId());
		}

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

			viewHolder.mainInfo.setText(statusMessage.getText().trim());

			viewHolder.timeStamp.setVisibility(View.VISIBLE);
			viewHolder.timeStamp.setText(statusMessage.getTimestampFormatted(true, mContext));

			viewHolder.statusImg.setVisibility(View.GONE);

			viewHolder.buttonDivider.setVisibility(View.VISIBLE);

			int padding = mContext.getResources().getDimensionPixelSize(R.dimen.status_btn_padding);
			viewHolder.noBtn.setPadding(padding, viewHolder.noBtn.getPaddingTop(), padding, viewHolder.noBtn.getPaddingTop());
			viewHolder.noBtn.setText(R.string.not_now);

			viewHolder.infoContainer.setVisibility(View.GONE);
			viewHolder.moodsContainer.setVisibility(View.GONE);

			viewHolder.actionsLayout.setVisibility(View.GONE);
			
			//Due to view reusage
			viewHolder.parent.setOnClickListener(null);
			viewHolder.parent.setOnLongClickListener(null);

			viewHolder.cardView.setOnClickListener(null);
			viewHolder.cardView.setOnLongClickListener(null);

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
				viewHolder.mainInfo.setText(smileyParser.addSmileySpans(statusMessage.getText().trim(), true));
				Linkify.addLinks(viewHolder.mainInfo, Linkify.ALL);
				viewHolder.cardView.setTag(statusMessage);
				viewHolder.cardView.setOnClickListener(timelinePostDetailsListener);
				viewHolder.cardView.setOnLongClickListener(onCardLongPressListener);
				
				viewHolder.textBtnLove.setTag(statusMessage);
				viewHolder.textBtnLove.setOnClickListener(onLikesClickListener);

				viewHolder.checkBoxLove.setOnCheckedChangeListener(null);
				viewHolder.checkBoxLove.setTag(statusMessage);

				boolean selfLiked = false;

				if (likesData != null)
				{
					selfLiked = likesData.isLikedBySelf();

					if (isShowCountEnabled || statusMessage.isMyStatusUpdate())
					{
						if (likesData.getTotalCount() == 0)
						{
							viewHolder.textBtnLove.setText(R.string.like_this);
						}
						else if (likesData.getTotalCount() == 1)
						{
							viewHolder.textBtnLove.setText(String.format(mContext.getString(R.string.num_like), likesData.getTotalCount()));
						}
						else
						{
							viewHolder.textBtnLove.setText(String.format(mContext.getString(R.string.num_likes), likesData.getTotalCount()));
						}
					}
					else
					{
						if (selfLiked)
						{
							viewHolder.textBtnLove.setText(R.string.liked_it);
						}
						else
						{
							viewHolder.textBtnLove.setText(R.string.like_this);
						}
					}
				}
				else
				{
					viewHolder.textBtnLove.setText(R.string.like_this);
				}

				if (selfLiked)
				{
					viewHolder.checkBoxLove.setChecked(true);
				}
				else
				{
					viewHolder.checkBoxLove.setChecked(false);
				}

				viewHolder.checkBoxLove.setOnCheckedChangeListener(onLoveToggleListener);

				viewHolder.actionsLayout.setVisibility(View.VISIBLE);
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
				viewHolder.parent.setTag(statusMessage);
				viewHolder.parent.setOnClickListener(onProfileInfoClickListener);
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

					viewHolder.statusImg.setTag(statusMessage);
					viewHolder.statusImg.setOnClickListener(timelinePostDetailsListener);
					// TODO
					// profileImageLoader.loadImage(protip.getMappedId(), viewHolder.statusImg, isListFlinging);
					timelineImageLoader.loadImage(protip.getMappedId(), viewHolder.statusImg, false);
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
			case JOINED_HIKE:
				viewHolder.yesBtn.setVisibility(View.GONE);
				viewHolder.noBtn.setVisibility(View.GONE);
				viewHolder.extraInfo.setVisibility(View.GONE);
				viewHolder.mainInfo.setText(statusMessage.getText());
				break;
			default:
				break;
			}

			viewHolder.avatar.setTag(statusMessage);

			viewHolder.noBtn.setTag(statusMessage);
			viewHolder.noBtn.setOnClickListener(noBtnClickListener);

			break;

		case IMAGE:
		case TEXT_IMAGE:
		case PROFILE_PIC_CHANGE:
			RoundedImageView roundAvatar1 = (RoundedImageView) viewHolder.avatar;
			roundAvatar1.setScaleType(ScaleType.FIT_CENTER);
			roundAvatar1.setBackgroundResource(0);

			if (statusMessage.hasMood())
			{
				// For moods we dont want to use rounded corners
				roundAvatar1.setOval(false);
				roundAvatar1.setImageResource(EmoticonConstants.moodMapping.get(statusMessage.getMoodId()));
			}
			else
			{
				roundAvatar1.setOval(true);
				setAvatar(statusMessage.getMsisdn(), viewHolder.avatar);
			}

			viewHolder.name.setText(mUserMsisdn.equals(statusMessage.getMsisdn()) ? HikeMessengerApp.getInstance().getApplicationContext().getString(R.string.me) : statusMessage
					.getNotNullName());

			if (TextUtils.isEmpty(statusMessage.getText()) || statusMessage.getText().equals("null"))
			{
				if (statusMessage.getStatusMessageType() == StatusMessageType.IMAGE || statusMessage.getStatusMessageType() == StatusMessageType.TEXT_IMAGE)
				{
					viewHolder.mainInfo.setVisibility(View.GONE);
				}
				else if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
				{
					viewHolder.mainInfo.setText(R.string.changed_profile);
				}
			}
			else
			{
				SmileyParser smileyParser = SmileyParser.getInstance();
				viewHolder.mainInfo.setText(smileyParser.addSmileySpans(statusMessage.getText().trim(), true));
				Linkify.addLinks(viewHolder.mainInfo, Linkify.ALL);
				viewHolder.mainInfo.setMovementMethod(null);
			}

			viewHolder.cardView.setTag(statusMessage);
			viewHolder.cardView.setOnClickListener(timelinePostDetailsListener);
			
			viewHolder.largeProfilePic.setTag(statusMessage);
			viewHolder.largeProfilePic.setOnClickListener(timelinePostDetailsListener);

			timelineImageLoader.loadImage(statusMessage.getMappedId(), viewHolder.largeProfilePic, false, false, false, statusMessage);

			viewHolder.timeStamp.setText(statusMessage.getTimestampFormatted(true, mContext));

			viewHolder.infoContainer.setTag(statusMessage);
			viewHolder.infoContainer.setOnClickListener(onProfileInfoClickListener);
			viewHolder.largeProfilePic.setOnLongClickListener(onCardLongPressListener);
			viewHolder.infoContainer.setOnLongClickListener(onCardLongPressListener);

			viewHolder.textBtnLove.setTag(statusMessage);
			viewHolder.textBtnLove.setOnClickListener(onLikesClickListener);

			boolean selfLiked = false;

			if (likesData != null)
			{
				selfLiked = likesData.isLikedBySelf();

				if (isShowCountEnabled || statusMessage.isMyStatusUpdate())
				{
					if (likesData.getTotalCount() == 0)
					{
						viewHolder.textBtnLove.setText(R.string.like_this);
					}
					else if (likesData.getTotalCount() == 1)
					{
						viewHolder.textBtnLove.setText(String.format(mContext.getString(R.string.num_like), likesData.getTotalCount()));
					}
					else
					{
						viewHolder.textBtnLove.setText(String.format(mContext.getString(R.string.num_likes), likesData.getTotalCount()));
					}
				}
				else
				{
					if (selfLiked)
					{
						viewHolder.textBtnLove.setText(R.string.liked_it);
					}
					else
					{
						viewHolder.textBtnLove.setText(R.string.like_this);
					}
				}
			}
			else
			{
				viewHolder.textBtnLove.setText(R.string.like_this);
			}

			viewHolder.checkBoxLove.setOnCheckedChangeListener(null);

			viewHolder.checkBoxLove.setTag(statusMessage);

			if (selfLiked)
			{
				viewHolder.checkBoxLove.setChecked(true);
			}
			else
			{
				viewHolder.checkBoxLove.setChecked(false);
			}
			viewHolder.checkBoxLove.setOnCheckedChangeListener(onLoveToggleListener);
			break;

		case FTUE_CARD_FAV:
			ContactInfo contact = mFtueFriendList.get(0);
			viewHolder.name.setText(contact.getNameOrMsisdn());
			((RoundedImageView)viewHolder.avatar).setOval(true);
			setAvatar(contact.getMsisdn(), viewHolder.avatar);
			viewHolder.ftueShow.setTag(viewType);
			viewHolder.ftueShow.setOnClickListener(ftueListItemClickListener);
			viewHolder.ftueBottomText.setText(Utils.isFavToFriendsMigrationAllowed() ? R.string.timeline_add_as_frn : R.string.timeline_add_as_fav);
			viewHolder.favIcon.setImageResource(Utils.isFavToFriendsMigrationAllowed() ? R.drawable.ic_84_addfriend : R.drawable.icon_favorites);
			int imageSize = mContext.getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size);
			profileLoader = new ProfileImageLoader(mContext, contact.getMsisdn(), viewHolder.largeProfilePic, imageSize, false, true);
			profileLoader.setLoaderListener(new ProfileImageLoader.LoaderListener()
			{

				@Override
				public void onLoaderReset(Loader<Boolean> arg0)
				{
					// dismissProgressDialog();
				}

				@Override
				public void onLoadFinished(Loader<Boolean> arg0, Boolean arg1)
				{
					// dismissProgressDialog();
				}

				@Override
				public Loader<Boolean> onCreateLoader(int arg0, Bundle arg1)
				{
					// showProgressDialog();
					return null;
				}

				@Override
				public void startDownloading()
				{
					// showProgressDialog();
					beginImageDownload();
				}
			});
			profileLoader.loadProfileImage(loaderManager);
			viewHolder.cancelFTUE.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					// show card with next contact or exit card
					addFTUEItemIfExists(false);
				}
			});
			break;
		case FTUE_CARD_INIT:
		case FTUE_CARD_EXIT:
			viewHolder.ftueShow.setTag(viewType);
			viewHolder.ftueShow.setOnClickListener(ftueListItemClickListener);
			break;
		}

		// TODO Removed for v1
		// if (position >= mLastPosition)
		// {
		// Animator[] anims = getAnimators(viewHolder.itemView);
		// int length = anims.length;
		// for (int i = length; i > 0; i--)
		// {
		// Animator anim = anims[i - 1];
		// anim.setInterpolator(cardInterp);
		// anim.setDuration(500).start();
		// }
		// mLastPosition = position;
		// }

		// Done to support Quick Return
		if (position == 0 && !mShowUserProfile)
		{
			viewHolder.parent.setPadding(0, HikePhotosUtils.dpToPx(52), 0, 0);
		}
		else
		{
			viewHolder.parent.setPadding(0, 0, 0, 0);
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
		Logger.d(HikeConstants.TIMELINE_LOGS, "onCreateViewHolder " + viewType);
		View convertView = null;

		switch (viewType)
		{
		case USER_PROFILE_HEADER:
			convertView = mInflater.inflate(R.layout.timeline_profile_header, parent, false);
			return new ViewHolder(convertView, viewType);
		case OTHER_UPDATE:
			convertView = mInflater.inflate(R.layout.timeline_item, parent, false);
			return new ViewHolder(convertView, viewType);
		case FTUE_CARD_INIT:
			convertView = mInflater.inflate(R.layout.timeline_ftue_init, parent, false);
			return new ViewHolder(convertView, viewType);
		case FTUE_CARD_FAV:
			convertView = mInflater.inflate(R.layout.timeline_ftue_add_fav, parent, false);
			return new ViewHolder(convertView, viewType);
		case FTUE_CARD_EXIT:
			convertView = mInflater.inflate(R.layout.timeline_ftue_exit, parent, false);
			return new ViewHolder(convertView, viewType);
		case PROFILE_PIC_CHANGE:
			convertView = mInflater.inflate(R.layout.profile_pic_timeline_item, parent, false);
			return new ViewHolder(convertView, viewType);
		case IMAGE:
			convertView = mInflater.inflate(R.layout.profile_pic_timeline_item, parent, false);
			return new ViewHolder(convertView, viewType);
		case TEXT_IMAGE:
			convertView = mInflater.inflate(R.layout.profile_pic_timeline_item, parent, false);
			return new ViewHolder(convertView, viewType);
		default:
			return null;
		}
	}

	private void setAvatar(String msisdn, ImageView avatar)
	{
		mIconImageLoader.loadImage(msisdn, avatar, false, true);
	}

	private OnClickListener timelinePostDetailsListener = new OnClickListener()
	{
		private long lastClickTime = 0;

		@Override
		public void onClick(View v)
		{
			if (SystemClock.elapsedRealtime() - lastClickTime < 1000)
			{
				return;
			}

			lastClickTime = SystemClock.elapsedRealtime();

			if (mActivity.get() != null)
			{
				ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(mActivity.get(), v, mContext.getString(R.string.timeline_transition_anim));
				StatusMessage statusMessage = (StatusMessage) v.getTag();
				Intent intent = new Intent(mActivity.get(), TimelineSummaryActivity.class);
				intent.putExtra(HikeConstants.Extras.MAPPED_ID, statusMessage.getMappedId());
				ActivityCompat.startActivity(mActivity.get(), intent, options.toBundle());
			}
		}
	};
	
	private OnClickListener onLikesClickListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			if (mActivity.get() != null)
			{
				Object viewTag = v.getTag();

				if (viewTag != null && mActivity.get() != null)
				{
					StatusMessage statusMessage = null;

					if (viewTag instanceof StatusMessage)
					{
						statusMessage = (StatusMessage) viewTag;
					}
					else if (viewTag instanceof ImageViewerInfo)
					{
						ImageViewerInfo imageInfo = (ImageViewerInfo) viewTag;
						statusMessage = imageInfo.getStatusMessage();
					}

					if (statusMessage == null)
					{
						return;
					}
					
					ActionsDataModel actionsData = TimelineActionsManager.getInstance().getActionsData().getActions(statusMessage.getMappedId(), ActionTypes.LIKE, ActivityObjectTypes.STATUS_UPDATE);

					if (actionsData!= null && actionsData.getAllMsisdn() != null && !actionsData.getAllMsisdn().isEmpty() && (Utils.isTimelineShowLikesEnabled() || statusMessage.isMyStatusUpdate()))
					{
						HikeDialog contactsListDialog = HikeDialogFactory.showDialog(mActivity.get(), HikeDialogFactory.LIKE_CONTACTS_DIALOG, statusMessage.getMsisdn(), null,
								actionsData.getAllMsisdn());

						// TODO bind with activity when supporting landscape
						contactsListDialog.show();
						JSONObject metadataSU = new JSONObject();
						try
						{
							metadataSU.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.TIMELINE_SUMMARY_LIKES_DIALOG_OPEN);
							metadataSU.put(AnalyticsConstants.UPDATE_TYPE, "" + ActivityFeedCursorAdapter.getPostType(statusMessage));
							metadataSU.put(AnalyticsConstants.TIMELINE_U_ID, statusMessage.getMsisdn());
							HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, HAManager.EventPriority.HIGH, metadataSU);
						}
						catch (JSONException e)
						{
							Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
						}
					}

					return;

				}
			}
		}
	};

	private OnLongClickListener onCardLongPressListener = new OnLongClickListener()
	{
		@Override
		public boolean onLongClick(View v)
		{
			Object viewTag = v.getTag();

			if (viewTag != null && mActivity.get() != null)
			{
				StatusMessage statusMessage = null;

				if (viewTag instanceof StatusMessage)
				{
					statusMessage = (StatusMessage) viewTag;
				}
				else if (viewTag instanceof ImageViewerInfo)
				{
					ImageViewerInfo imageInfo = (ImageViewerInfo) viewTag;
					statusMessage = imageInfo.getStatusMessage();
				}

				if (statusMessage == null)
				{
					return false;
				}

				longPressListClickListener.setStatusMessage(statusMessage);

				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mActivity.get());
				ArrayAdapter<String> dialogAdapter = new ArrayAdapter<String>(mActivity.get(), R.layout.alert_item, R.id.item, getLongPressListItemsArray(statusMessage));
				dialogBuilder.setAdapter(dialogAdapter, longPressListClickListener);
				alertDialog = dialogBuilder.show();
				alertDialog.getListView().setDivider(null);
				alertDialog.getListView().setPadding(0, HikeMessengerApp.getInstance().getResources().getDimensionPixelSize(R.dimen.menu_list_padding_top), 0, HikeMessengerApp.getInstance().getResources().getDimensionPixelSize(R.dimen.menu_list_padding_bottom));
				
				return true;

			}

			return false;
		}
	};

	private String[] getLongPressListItemsArray(StatusMessage argStatusMessage)
	{
		ArrayList<String> optionsList = new ArrayList<String>();

		if (!argStatusMessage.isMyStatusUpdate())
		{
			optionsList.add(String.format(mContext.getString(R.string.message_person), argStatusMessage.getNotNullName()));
		}

		if (argStatusMessage.getStatusMessageType() == StatusMessageType.TEXT)
		{
			optionsList.add(mContext.getString(R.string.copy));
		}

		optionsList.add(mContext.getString(R.string.delete_post));

		final String[] options = new String[optionsList.size()];
		optionsList.toArray(options);

		return options;
	}

	private DialogListItemClickListener longPressListClickListener = new DialogListItemClickListener();

	private class DialogListItemClickListener implements DialogInterface.OnClickListener
	{
		StatusMessage mStatusMessage;

		public void setStatusMessage(StatusMessage argStatusMessage)
		{
			mStatusMessage = argStatusMessage;
		}

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			final String[] options = getLongPressListItemsArray(mStatusMessage);

			String option = options[which];

			if (mContext.getString(R.string.copy).equals(option))
			{
				Utils.setClipboardText(mStatusMessage.getText(), mContext);
				JSONObject metadataSU = new JSONObject();
				try
				{
					metadataSU.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.TIMELINE_CARD_LONG_PRESS);
					metadataSU.put(AnalyticsConstants.UPDATE_TYPE, "" + ActivityFeedCursorAdapter.getPostType(mStatusMessage));
					metadataSU.put(AnalyticsConstants.TIMELINE_U_ID, mStatusMessage.getMsisdn());
					metadataSU.put(AnalyticsConstants.TIMELINE_OPTION_TYPE, HikeConstants.LogEvent.TIMELINE_LONG_PRESS_COPY);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, HAManager.EventPriority.HIGH, metadataSU);
				}
				catch (JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}
			}
			else if (mContext.getString(R.string.delete_post).equals(option))
			{
				if (mStatusMessage.isMyStatusUpdate())
				{
					showDeleteStatusConfirmationDialog(mStatusMessage);
				}
				else
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_STATUS, mStatusMessage.getMappedId());
					JSONObject metadataSU = new JSONObject();
					try
					{
						metadataSU.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.TIMELINE_CARD_LONG_PRESS);
						metadataSU.put(AnalyticsConstants.UPDATE_TYPE, "" + ActivityFeedCursorAdapter.getPostType(mStatusMessage));
						metadataSU.put(AnalyticsConstants.TIMELINE_U_ID, mStatusMessage.getMsisdn());
						metadataSU.put(AnalyticsConstants.TIMELINE_OPTION_TYPE, HikeConstants.LogEvent.TIMELINE_LONG_PRESS_DELETE);
						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, HAManager.EventPriority.HIGH, metadataSU);
					}
					catch (JSONException e)
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
					}
				}
			}
			else if (String.format(mContext.getString(R.string.message_person), mStatusMessage.getNotNullName()).equals(option))
			{
				if (mActivity.get() != null)
				{
					Intent intent = IntentFactory.createChatThreadIntentFromContactInfo(mActivity.get(),
							ContactManager.getInstance().getContact(mStatusMessage.getMsisdn(),true,true), false, false, ChatThreadActivity.ChatThreadOpenSources.TIMELINE);
					startActivity(intent);
					JSONObject metadataSU = new JSONObject();
					try
					{
						metadataSU.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.TIMELINE_CARD_LONG_PRESS);
						metadataSU.put(AnalyticsConstants.UPDATE_TYPE, "" + ActivityFeedCursorAdapter.getPostType(mStatusMessage));
						metadataSU.put(AnalyticsConstants.TIMELINE_U_ID, mStatusMessage.getMsisdn());
						metadataSU.put(AnalyticsConstants.TIMELINE_OPTION_TYPE, HikeConstants.LogEvent.TIMELINE_LONG_PRESS_MESSAGE);
						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, HAManager.EventPriority.HIGH, metadataSU);
					}
					catch (JSONException e)
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
					}
				}
			}
		}
	};

	public void removeStatusUpdate(final String statusId)
	{
		if (mActivity.get() != null)
		{
			mActivity.get().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if(mActivity.get() == null || mActivity.get().isFinishing())
					{
						return;
					}
					
					if (mStatusMessages == null || mStatusMessages.isEmpty())
					{
						return;
					}

					for (StatusMessage statusMessage : mStatusMessages)
					{
						if (statusId.equals(statusMessage.getMappedId()))
						{
							mStatusMessages.remove(statusMessage);
							Logger.d(HikeConstants.TIMELINE_LOGS, "SU list after deleting post "+ mStatusMessages);
							break;
						}
					}

					notifyDataSetChanged();
				}
			});
		}
	}

	public void onViewImageClicked(View v)
	{
		ImageViewerInfo imageViewerInfo = (ImageViewerInfo) v.getTag();

		String mappedId = imageViewerInfo.mappedId;
		String url = imageViewerInfo.url;

		Bundle arguments = new Bundle();
		arguments.putString(HikeConstants.Extras.MAPPED_ID, mappedId);
		arguments.putString(HikeConstants.Extras.URL, url);
		arguments.putBoolean(HikeConstants.Extras.IS_STATUS_IMAGE, imageViewerInfo.isStatusMessage);
		
		if(!imageViewerInfo.isStatusMessage && Utils.isSelfMsisdn(imageViewerInfo.url))
		{
			arguments.putBoolean(HikeConstants.CAN_EDIT_DP, true);
		}

		HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_IMAGE, arguments);
	}

	private void showDeleteStatusConfirmationDialog(final StatusMessage statusMessage)
	{
		HikeDialogFactory.showDialog(mActivity.get(), HikeDialogFactory.DELETE_STATUS_TIMELINE_DIALOG, new HikeDialogListener()
		{
			@Override
			public void positiveClicked(final HikeDialog hikeDialog)
			{
				JSONObject json = null;
				try
				{
					json = new JSONObject();
					json.put(HikeConstants.STATUS_ID, statusMessage.getMappedId());
				}
				catch (JSONException e)
				{
					Logger.e("", "exception while deleting status : " + e);
				}
				
				HttpRequests.deleteStatusRequest(json, new IRequestListener()
				{
					@Override
					public void onRequestSuccess(Response result)
					{
						HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_STATUS, statusMessage.getMappedId());
						
						JSONObject metadataSU = new JSONObject();
						try
						{
							metadataSU.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.TIMELINE_CARD_LONG_PRESS);
							metadataSU.put(AnalyticsConstants.UPDATE_TYPE, "" + ActivityFeedCursorAdapter.getPostType(statusMessage));
							metadataSU.put(AnalyticsConstants.TIMELINE_U_ID, statusMessage.getMsisdn());
							metadataSU.put(AnalyticsConstants.TIMELINE_OPTION_TYPE, HikeConstants.LogEvent.TIMELINE_LONG_PRESS_DELETE);
							HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, HAManager.EventPriority.HIGH, metadataSU);
						}
						catch (JSONException e)
						{
							Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
						}

						// update the preference value used to store latest dp change status update id
						if (statusMessage.getMappedId().equals(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.DP_CHANGE_STATUS_ID, null))
								&& statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
						{
							HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.DP_CHANGE_STATUS_ID, "");
						}

						if (hikeDialog != null && hikeDialog.isShowing())
						{
							hikeDialog.dismiss();
						}
					}

					@Override
					public void onRequestProgressUpdate(float progress)
					{
						// Do nothing
					}

					@Override
					public void onRequestFailure(HttpException httpException)
					{
						Toast.makeText(mContext, R.string.delete_status_error, Toast.LENGTH_LONG).show();
						if (hikeDialog != null && hikeDialog.isShowing())
						{
							hikeDialog.dismiss();
						}
					}
				}).execute();
			}

			@Override
			public void negativeClicked(HikeDialog hikeDialog)
			{
				hikeDialog.dismiss();
			}

			@Override
			public void neutralClicked(HikeDialog hikeDialog)
			{

			}

		});
	}

	private OnClickListener yesBtnClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			StatusMessage statusMessage = (StatusMessage) v.getTag();
			if (EMPTY_STATUS_NO_STATUS_ID == statusMessage.getId() || EMPTY_STATUS_NO_STATUS_RECENTLY_ID == statusMessage.getId())
			{
				Intent intent = new Intent(mContext, StatusUpdate.class);
				startActivity(intent);

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
					startActivity(marketIntent);
				}
				catch (ActivityNotFoundException e)
				{
					Logger.e(TimelineCardsAdapter.class.getSimpleName(), "Unable to open market");
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

					HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.CURRENT_PROTIP, -1);

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
					statusMessage.getMsisdn()), false, false, ChatThreadActivity.ChatThreadOpenSources.TIMELINE);
			// Add anything else to the intent
			intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			// TODO
			// mContext.finish();
		}
	};

	private OnClickListener ftueListItemClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			JSONObject metadata = new JSONObject();
			switch ((int) v.getTag())
			{
			case FTUE_CARD_INIT:

				// remove FTUE INIT Card
				removeFTUEItemIfExists(FTUE_CARD_INIT);
				
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.INIT_CARD_SHOWN, true);
				
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.INIT_CARD_ON_TOP, false);
				
				ContactInfo contact = mFtueFriendList.get(0);
				StatusMessage statusMessage = new StatusMessage(TimelineCardsAdapter.FTUE_CARD_FAV, null, contact.getMsisdn(), contact.getName(), null, null, 0);
				mStatusMessages.add(0, statusMessage);

				try
				{
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.FTUE_SHOW_ME_CLICKED);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, HAManager.EventPriority.HIGH, metadata);
				}
				catch (JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}
				notifyDataSetChanged();
				break;

			case FTUE_CARD_FAV:

				// show card with next contact or exit card
				addFTUEItemIfExists(true);

				break;

			case FTUE_CARD_EXIT:

				// remove FTUE Exit Card
				removeFTUEItemIfExists(FTUE_CARD_EXIT);
				
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.EXIT_CARD_ON_TOP, false);
				
				if(mFtueFriendList.isEmpty())
				{
					// Set enable FTUE time to false
					HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ENABLE_TIMELINE_FTUE, false);
				}
				else
				{
					contact = mFtueFriendList.get(0);
					statusMessage = new StatusMessage(TimelineCardsAdapter.FTUE_CARD_FAV, null, contact.getMsisdn(), contact.getName(), null, null, 0);
					mStatusMessages.add(0, statusMessage);
				}
				
				try
				{
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.FTUE_GOT_IT_CLICKED);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, HAManager.EventPriority.HIGH, metadata);
				}
				catch (JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}
				
				notifyDataSetChanged();
				break;

			default:
				break;
			}

		}
	};
	
	private OnCheckedChangeListener onLoveToggleListener = new LoveCheckBoxToggleListener()
	{
		@Override
		public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked)
		{
			if (mShowUserProfile)
			{
				// First check if user is friends with msisdn
				if (profileContactInfo.getFavoriteType() != FavoriteType.FRIEND && !Utils.isSelfMsisdn(profileContactInfo.getMsisdn()))
				{
					toggleCompButtonState(buttonView, onLoveToggleListener);
					if (mActivity.get() != null)
					{
						HikeDialogFactory.showDialog(mActivity.get(), HikeDialogFactory.ADD_TO_FAV_DIALOG, new HikeDialogListener()
						{
							@Override
							public void positiveClicked(HikeDialog hikeDialog)
							{
								Utils.toggleFavorite(mContext, profileContactInfo, false, HikeConstants.AddFriendSources.UNKNOWN);
								if (hikeDialog != null && hikeDialog.isShowing())
								{
									hikeDialog.dismiss();
								}
							}

							@Override
							public void neutralClicked(HikeDialog hikeDialog)
							{
								// Do nothing
							}

							@Override
							public void negativeClicked(HikeDialog hikeDialog)
							{
								if (hikeDialog != null && hikeDialog.isShowing())
								{
									hikeDialog.dismiss();
								}
							}
						}, profileContactInfo.getNameOrMsisdn());
					}
					return;
				}
			}
			
			super.onCheckedChanged(buttonView, isChecked);
		}
	};

	private void toggleCompButtonState(CompoundButton argButton,OnCheckedChangeListener argListener)
	{
		//unlink-relink onchange listener
		argButton.setOnCheckedChangeListener(null);
		argButton.toggle();
		argButton.setOnCheckedChangeListener(argListener);
	}
	
	public void setProtipIndex(int startIndex)
	{
		mProtipIndex = startIndex;
	}

	private void startActivity(Intent argIntent)
	{
		if (mActivity.get() != null)
		{
			mActivity.get().startActivity(argIntent);
		}
	}

	public void removeAllFTUEItems()
	{
		removeFTUEItemIfExists(FTUE_CARD_INIT);
		removeFTUEItemIfExists(FTUE_CARD_FAV);
		removeFTUEItemIfExists(FTUE_CARD_EXIT);
	}

	private void removeFTUEItemIfExists(int id)
	{
		if (!mStatusMessages.isEmpty())
		{
			for (int i = 0; i < mStatusMessages.size(); i++)
			{
				if (mStatusMessages.get(i).getId() == id)
				{
					mStatusMessages.remove(i);
					notifyDataSetChanged();
					break;
				}
			}
		}
	}

	private void addFTUEItemIfExists(boolean addFav)
	{
		StatusMessage statusMessage = null;
		if (!mStatusMessages.isEmpty() && mFtueFriendList != null && !mFtueFriendList.isEmpty())
		{
			ContactInfo contact = mFtueFriendList.get(0);
			if (addFav && (contact.getFavoriteType()!=null && contact.getFavoriteType() != FavoriteType.FRIEND))
			{
				Logger.d("tl_ftue", "Adding " + contact.getMsisdn() +" as friend");
				Utils.toggleFavorite(mContext, contact, true, HikeConstants.AddFriendSources.TIMELINE_FTUE_SCREEN);
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ANY_TIMELINE_FTUE_FAV_CLICKED, true);
			}
			removeFTUEItemIfExists(FTUE_CARD_FAV);

			Set<String> list = HikeSharedPreferenceUtil.getInstance().getStringSet(HikeConstants.TIMELINE_FTUE_MSISDN_LIST, null);
			list.remove(mFtueFriendList.get(0).getMsisdn());
			HikeSharedPreferenceUtil.getInstance().saveStringSet(HikeConstants.TIMELINE_FTUE_MSISDN_LIST, list);

			mFtueFriendList.remove(0);
			
			if (!mFtueFriendList.isEmpty())
			{
				contact = mFtueFriendList.get(0);
				statusMessage = new StatusMessage(TimelineCardsAdapter.FTUE_CARD_FAV, null, contact.getMsisdn(), contact.getName(), null, null, 0);
				mStatusMessages.add(0, statusMessage);
			}
			else
			{
				Logger.d("tl_ftue", "List is empty after clicking fav card, checking to show EXIT card or not");
				if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ANY_TIMELINE_FTUE_FAV_CLICKED, false)
						&& !HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.EXIT_CARD_SHOWN, false))
				{
					statusMessage = new StatusMessage(TimelineCardsAdapter.FTUE_CARD_EXIT, null, null, null, null, null, 0);
					mStatusMessages.add(0, statusMessage);
					HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.EXIT_CARD_SHOWN, true);
					HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.EXIT_CARD_ON_TOP, true);
				}
			}
			notifyDataSetChanged();
		}
	}

	public void setFTUEFriendList(List<ContactInfo> fndList)
	{
		this.mFtueFriendList = fndList;
	}

	public void onSuccess(Response result)
	{
		mHikeUiHandler.post(new Runnable()
		{

			@Override
			public void run()
			{
				profileLoader.loadFromFile();
			}
		});
	}

	@Override
	public void handleUIMessage(Message msg)
	{
		// TODO Auto-generated method stub

	}

	private void beginImageDownload()
	{
		String fileName = Utils.getProfileImageFileName(mUserMsisdn);
		mImageDownloader = HikeImageDownloader.newInstance(mUserMsisdn, fileName, true, false, null, null, null, true, false);
		mImageDownloader.setTaskCallbacks(this);
		mImageDownloader.startLoadingTask();
	}

	@Override
	public void onProgressUpdate(float percent)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onCancelled()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onFailed()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onTaskAlreadyRunning()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (mShowUserProfile && (HikePubSub.FAVORITE_TOGGLED.equals(type) || HikePubSub.FRIEND_REQUEST_ACCEPTED.equals(type) || HikePubSub.REJECT_FRIEND_REQUEST.equals(type)))
		{
			final Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;

			ContactInfo contactInfo = favoriteToggle.first;
			FavoriteType favoriteType = favoriteToggle.second;

			if (!profileContactInfo.getMsisdn().equals(contactInfo.getMsisdn()))
			{
				return;
			}
			else
			{
				this.profileContactInfo.setFavoriteType(favoriteType);
			}
		}
		else if (HikePubSub.DELETE_STATUS.equals(type))
		{
			if (object != null && object instanceof String)
			{
				removeStatusUpdate((String) object);
				Message emptyMessage = Message.obtain();
				emptyMessage.arg1 = UpdatesFragment.MSG_DELETE;
				handleUIMessage(emptyMessage);
			}
		}
		else if (HikePubSub.ACTIVITY_UPDATE.equals(type))
		{
			if (!isDestroyed && mActivity.get() != null)
			{
				mActivity.get().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						notifyDataSetChanged();
					}
				});
			}
		}
	}

	public HashSet<String> getSUViewedSet()
	{
		return mSUViewedSet;
	}

	public void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, HikePubSub.FAVORITE_TOGGLED);
		HikeMessengerApp.getPubSub().removeListeners(this, HikePubSub.DELETE_STATUS);
		HikeMessengerApp.getPubSub().removeListeners(this, HikePubSub.ACTIVITY_UPDATE);
		
		// For any async tasks which completes after user goes away from this adapter
		isDestroyed = true;
	}
}
