package com.bsb.hike.timeline.adapter;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.util.Linkify;
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
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ImageViewerInfo;
import com.bsb.hike.models.Protip;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.smartImageLoader.TimelineUpdatesImageLoader;
import com.bsb.hike.timeline.model.ActionsDataModel;
import com.bsb.hike.timeline.model.ActionsDataModel.ActionTypes;
import com.bsb.hike.timeline.model.ActionsDataModel.ActivityObjectTypes;
import com.bsb.hike.timeline.model.FeedDataModel;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.timeline.model.TimelineActions;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.StatusUpdate;
import com.bsb.hike.ui.fragments.HeadlessImageDownloaderFragment;
import com.bsb.hike.ui.fragments.HeadlessImageWorkerFragment;
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

public class TimelineCardsAdapter extends RecyclerView.Adapter<TimelineCardsAdapter.ViewHolder> implements IHandlerCallback, HeadlessImageWorkerFragment.TaskCallbacks
{
	private final int PROFILE_PIC_CHANGE = -11;

	private final int OTHER_UPDATE = -12;

	public final static int FTUE_CARD_INIT = -14;
	
	public final static int FTUE_CARD_EXIT = -17;

	public final static int FTUE_CARD_FAV = -18;
	
	private final int IMAGE = -15;
	
	private final int TEXT_IMAGE = -16;

	public static final long EMPTY_STATUS_NO_STATUS_ID = -3;

	public static final long EMPTY_STATUS_NO_STATUS_RECENTLY_ID = -5;

	private TimelineActions mActionsData;
	
	private ProfileImageLoader profileLoader;
	
	protected AlertDialog alertDialog;

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

		TextView loveCount;
		
		TextView ftueShow;

		public ViewHolder(View convertView, int viewType)
		{
			super(convertView);

			// Common views
			parent = convertView.findViewById(R.id.main_content);
			name = (TextView) convertView.findViewById(R.id.name);
			mainInfo = (TextView) convertView.findViewById(R.id.main_info);
			checkBoxLove = (CheckBox)convertView.findViewById(R.id.btn_love);
			loveCount = (TextView)convertView.findViewById(R.id.love_count);

			//Grab view references
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
				ftueShow = (TextView) convertView.findViewById(R.id.ftue_show);
				break;
			case FTUE_CARD_EXIT:
				ftueShow = (TextView) convertView.findViewById(R.id.ftue_show);
				break;
			case FTUE_CARD_FAV:
				largeProfilePic = (ImageView)convertView.findViewById(R.id.dp_big);
				avatar = (ImageView)convertView.findViewById(R.id.avatar);
				ftueShow = (TextView) convertView.findViewById(R.id.ftue_show);
				break;
			}
			
		}
	}

	private Context mContext;

	private List<StatusMessage> mStatusMessages;

	private LayoutInflater mInflater;

	private IconLoader mIconImageLoader;

	private TimelineUpdatesImageLoader profileImageLoader;
	
	private String mUserMsisdn;

	private int mProtipIndex;

	private SoftReference<Activity> mActivity;

	private int mLastPosition = 3;
	
	private List<String> mFtueFriendList;
	
	private LoaderManager loaderManager;
	
	private FragmentManager fragmentManager;
	
	private HikeUiHandler mHikeUiHandler;

	public TimelineCardsAdapter(Activity activity, List<StatusMessage> statusMessages, String userMsisdn, List<String> ftueFriendList, LoaderManager loadManager, FragmentManager fragManager)
	{
		mContext = HikeMessengerApp.getInstance().getApplicationContext();
		mStatusMessages = statusMessages;
		mUserMsisdn = userMsisdn;
		profileImageLoader = new TimelineUpdatesImageLoader(mContext, mContext.getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size));
		mIconImageLoader = new IconLoader(mContext, mContext.getResources().getDimensionPixelSize(R.dimen.icon_picture_size));
		mIconImageLoader.setDefaultAvatarIfNoCustomIcon(true);
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mProtipIndex = -1;
		mActivity = new SoftReference<Activity>(activity);
		mFtueFriendList = ftueFriendList;
		loaderManager = loadManager;
		fragmentManager = fragManager;
		mHikeUiHandler = new HikeUiHandler(this);
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
		StatusMessage statusMessage = mStatusMessages.get(position);
		
		ActionsDataModel likesData = mActionsData.getActions(statusMessage.getMappedId(), ActionTypes.LIKE,ActivityObjectTypes.STATUS_UPDATE);

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
			viewHolder.name.setText(mUserMsisdn.equals(statusMessage.getMsisdn()) ? HikeMessengerApp.getInstance().getString(R.string.me) : statusMessage.getNotNullName());

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
				
				boolean selfLiked = false;
				
				if (likesData != null)
				{
					viewHolder.loveCount.setText(String.valueOf(likesData.getCount()));
					selfLiked = likesData.isLikedBySelf();
				}
				else
				{
					viewHolder.loveCount.setText("0");
				}

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
				
				viewHolder.checkBoxLove.setVisibility(View.VISIBLE);
				
				viewHolder.loveCount.setVisibility(View.VISIBLE);
				
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
					viewHolder.mainInfo.setText(R.string.posted_photo);
				}
				else if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
				{
					viewHolder.mainInfo.setText(R.string.status_profile_pic_notification);
				}
			}
			else
			{
				viewHolder.mainInfo.setText(statusMessage.getText());
			}

			ImageViewerInfo imageViewerInfo = new ImageViewerInfo(statusMessage.getMappedId(), null, true);
			imageViewerInfo.setStatusMessage(statusMessage);
			
			Bundle actionsBundle = new Bundle();

			if (likesData != null)
			{
				actionsBundle.putStringArrayList(HikeConstants.MSISDNS, likesData.getAllMsisdn());
			}
			actionsBundle.putString(HikeConstants.Extras.IMAGE_CAPTION, viewHolder.mainInfo.getText().toString());
			imageViewerInfo.setBundle(actionsBundle);

			viewHolder.largeProfilePic.setTag(imageViewerInfo);
			viewHolder.largeProfilePic.setOnClickListener(imageClickListener);

			profileImageLoader.loadImage(statusMessage.getMappedId(), viewHolder.largeProfilePic, false, false, false, statusMessage);

			viewHolder.timeStamp.setText(statusMessage.getTimestampFormatted(true, mContext));

			viewHolder.infoContainer.setTag(statusMessage);
			viewHolder.infoContainer.setOnClickListener(onProfileInfoClickListener);
			viewHolder.largeProfilePic.setOnLongClickListener(onCardLongPressListener);
			viewHolder.infoContainer.setOnLongClickListener(onCardLongPressListener);

			boolean selfLiked = false;
			
			if (likesData != null)
			{
				viewHolder.loveCount.setText(String.valueOf(likesData.getCount()));
				selfLiked = likesData.isLikedBySelf();
			}
			else
			{
				viewHolder.loveCount.setText("0");
			}

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
			int counter = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.TIMELINE_FTUE_FAV_COUNTER, 0);
			ContactInfo contact = ContactManager.getInstance().getContact(mFtueFriendList.get(counter-1));
			viewHolder.name.setText(contact.getName());
			setAvatar(contact.getMsisdn(), viewHolder.avatar);
			viewHolder.ftueShow.setTag(viewType);
			viewHolder.ftueShow.setOnClickListener(ftueListItemClickListener);
			int imageSize = mContext.getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size);
			profileLoader = new ProfileImageLoader(mContext, contact.getMsisdn(), viewHolder.largeProfilePic, imageSize, false, true);
			profileLoader.setLoaderListener(new ProfileImageLoader.LoaderListener()
			{

				@Override
				public void onLoaderReset(Loader<Boolean> arg0)
				{
					//dismissProgressDialog();
				}

				@Override
				public void onLoadFinished(Loader<Boolean> arg0, Boolean arg1)
				{
					//dismissProgressDialog();
				}

				@Override
				public Loader<Boolean> onCreateLoader(int arg0, Bundle arg1) {
					//showProgressDialog();
					return null;
				}

				@Override
				public void startDownloading()
				{
					//showProgressDialog();
					loadHeadLessImageDownloadingFragment();
				}
			});
			profileLoader.loadProfileImage(loaderManager);
			break;
		case FTUE_CARD_INIT:
		case FTUE_CARD_EXIT:
			viewHolder.ftueShow.setTag(viewType);
			viewHolder.ftueShow.setOnClickListener(ftueListItemClickListener);
			break;
		}

		if (position >= mLastPosition)
		{
			Animator[] anims = getAnimators(viewHolder.itemView);
			int length = anims.length;
			for (int i = length; i > 0; i--)
			{
				Animator anim = anims[i - 1];
				anim.setInterpolator(cardInterp);
				anim.setDuration(500).start();
			}
			mLastPosition = position;
		}

		//Done to support Quick Return
		if (position == 0)
		{
			viewHolder.parent.setPadding(0, HikePhotosUtils.dpToPx(60), 0, 0);
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
		View convertView = null;

		switch (viewType)
		{
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
			arguments.putAll(imageViewerInfo.getBundle());
			
			HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_IMAGE, arguments);
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
				alertDialog.getListView().setDivider(mContext.getResources().getDrawable(R.drawable.ic_thread_divider_profile));
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
		
		optionsList.add(mContext.getString(R.string.copy));
		
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
					removeStatusUpdate(mStatusMessage.getMappedId());
				}
			}
			else if (String.format(mContext.getString(R.string.message_person), mStatusMessage.getNotNullName()).equals(option))
			{
				if (mActivity.get() != null)
				{
					Intent intent = IntentFactory.createChatThreadIntentFromContactInfo(mActivity.get(),
							ContactManager.getInstance().getContactInfoFromPhoneNoOrMsisdn(mStatusMessage.getMsisdn()), true);
					intent.putExtra(HikeConstants.Extras.KEEP_ACTIVITIES, true);
					startActivity(intent);
				}
			}
		}
	};

	
	public void removeStatusUpdate(String statusId)
	{
		if (mStatusMessages == null || mStatusMessages.isEmpty())
		{
			return;
		}

		for (StatusMessage statusMessage : mStatusMessages)
		{
			if (statusId.equals(statusMessage.getMappedId()))
			{
				mStatusMessages.remove(statusMessage);
				break;
			}
		}
		
		notifyDataSetChanged();
	}
	private void showDeleteStatusConfirmationDialog(final StatusMessage statusMessage)
	{
		HikeDialogFactory.showDialog(mActivity.get(), HikeDialogFactory.DELETE_STATUS_TIMELINE_DIALOG, new HikeDialogListener()
		{
			@Override
			public void positiveClicked(final HikeDialog hikeDialog)
			{
				HttpRequests.deleteStatusRequest(statusMessage.getMappedId(), new IRequestListener()
				{
					@Override
					public void onRequestSuccess(Response result)
					{
						HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_STATUS, statusMessage.getMappedId());

						// update the preference value used to store latest dp change status update id
						if (statusMessage.getMappedId().equals(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.DP_CHANGE_STATUS_ID, null))
								&& statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
						{
							HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.DP_CHANGE_STATUS_ID, "");
						}

						removeStatusUpdate(statusMessage.getMappedId());

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
						Toast.makeText(mContext, R.string.delete_status_error, Toast.LENGTH_LONG);
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
				HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_STATUS, statusMessage.getMappedId());
				removeStatusUpdate(statusMessage.getMappedId());
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
					statusMessage.getMsisdn()), true);
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
			
			switch ((int)v.getTag())
			{
			case FTUE_CARD_INIT:
				// make counter 1
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.TIMELINE_FTUE_FAV_COUNTER, 1);
				
				//Show card with contact
				addFTUEItemIfExists();
				
				break;
				
			case FTUE_CARD_FAV:
				
				//increase counter by 1
				int counter = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.TIMELINE_FTUE_FAV_COUNTER, 0);
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.TIMELINE_FTUE_FAV_COUNTER, counter+1);
				
				//show card with next contact or exit card
				addFTUEItemIfExists();
				
				break;
				
			case FTUE_CARD_EXIT:
				
				//reset counter to 0
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.TIMELINE_FTUE_FAV_COUNTER, 0);
				
				//Set enable FTUE time to false
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ENABLE_TIMELINE_FTUE, false);
				
				//remove FTUE Exit Card
				removeFTUEItemIfExists(FTUE_CARD_EXIT);
				
				break;
				
			default:
				break;
			}
			
		}
	};

	private OnCheckedChangeListener onLoveToggleListener = new OnCheckedChangeListener()
	{

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			final StatusMessage statusMessage = (StatusMessage) buttonView.getTag();

			JSONObject json = new JSONObject();

			try
			{
				json.put(HikeConstants.SU_ID, statusMessage.getMappedId());
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}

			if (isChecked)
			{
				RequestToken token = HttpRequests.createLoveLink(json, new IRequestListener()
				{
					@Override
					public void onRequestSuccess(Response result)
					{
						JSONObject response = (JSONObject) result.getBody().getContent();
						if (response.optString("stat").equals("ok"))
						{
							// Increment like count in actions table
							String selfMsisdn = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, null);

							ArrayList<String> actorList = new ArrayList<String>();
							actorList.add(selfMsisdn);

							HikeConversationsDatabase.getInstance().changeActionCountForObjID(statusMessage.getMappedId(),
									ActionsDataModel.ActivityObjectTypes.STATUS_UPDATE.getTypeString(), ActionsDataModel.ActionTypes.LIKE.getKey(), actorList, true);

							FeedDataModel newFeed = new FeedDataModel(System.currentTimeMillis(), ActionTypes.LIKE, selfMsisdn, ActivityObjectTypes.STATUS_UPDATE, statusMessage
									.getMappedId());

							mActionsData.updateByActivityFeed(newFeed);
							
							notifyDataSetChanged();
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
						Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), R.string.love_failed, Toast.LENGTH_SHORT).show();
					}
				}, null);
				token.execute();
			}
			else
			{
				RequestToken token = HttpRequests.removeLoveLink(json, new IRequestListener()
				{
					@Override
					public void onRequestSuccess(Response result)
					{
						JSONObject response = (JSONObject) result.getBody().getContent();
						if (response.optString("stat").equals("ok"))
						{
							// Decrement like count in actions table
							String selfMsisdn = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, null);

							ArrayList<String> actorList = new ArrayList<String>();
							actorList.add(selfMsisdn);

							HikeConversationsDatabase.getInstance().changeActionCountForObjID(statusMessage.getMappedId(),
									ActionsDataModel.ActivityObjectTypes.STATUS_UPDATE.getTypeString(), ActionsDataModel.ActionTypes.LIKE.getKey(), actorList, false);
							
							FeedDataModel newFeed = new FeedDataModel(System.currentTimeMillis(), ActionTypes.UNLIKE, selfMsisdn, ActivityObjectTypes.STATUS_UPDATE, statusMessage
									.getMappedId());

							mActionsData.updateByActivityFeed(newFeed);
							
							notifyDataSetChanged();
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
						Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), R.string.love_failed, Toast.LENGTH_SHORT).show();
					}
				}, null);
				token.execute();
			}
		}
	};

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

	public void setActionsData(TimelineActions actionsData)
	{
		mActionsData = actionsData;
	}

	private void removeFTUEItemIfExists(int id)
	{
		if (!mStatusMessages.isEmpty())
		{
			if (mStatusMessages.get(mStatusMessages.size() - 1).getId() == id)
			{
				mStatusMessages.remove(mStatusMessages.size() - 1);
			}
			notifyDataSetChanged();
		}
	}
	
	private void addFTUEItemIfExists()
	{
		StatusMessage statusMessage = null;
		if (!mStatusMessages.isEmpty())
		{
			int counter = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.TIMELINE_FTUE_FAV_COUNTER, 0);
			ContactInfo contact = null;
			switch (counter)
			{
			
			case 1:
			case 2:
			case 3:
			case 4:
				contact = ContactManager.getInstance().getContact(mFtueFriendList.get(counter-1));
				if(counter == 1)
				{
					removeFTUEItemIfExists(FTUE_CARD_INIT);
				}
				else
				{
					removeFTUEItemIfExists(FTUE_CARD_FAV);
					Utils.addFavorite(mActivity.get(), contact, true);
				}
				statusMessage = new StatusMessage(TimelineCardsAdapter.FTUE_CARD_FAV, null, contact.getMsisdn(), contact.getName(), null, null, 0);
				break;

			case 5:
				removeFTUEItemIfExists(FTUE_CARD_FAV);
				statusMessage = new StatusMessage(TimelineCardsAdapter.FTUE_CARD_EXIT, null, null, null, null, null, 0);
				break;
			default:
				break;
			}
			mStatusMessages.add(0, statusMessage);
			notifyDataSetChanged();
		}
	}
	
	public void setFTUEFriendList(List<String> fndList)
	{
		this.mFtueFriendList = fndList;
	}
	
	private void loadHeadLessImageDownloadingFragment()
	{
		HeadlessImageDownloaderFragment mImageWorkerFragment = (HeadlessImageDownloaderFragment) fragmentManager.findFragmentByTag(HikeConstants.TAG_HEADLESS_IMAGE_DOWNLOAD_FRAGMENT);

	    // If the Fragment is non-null, then it is currently being
	    // retained across a configuration change.
	    if (mImageWorkerFragment == null) 
	    {
	    	String fileName = Utils.getProfileImageFileName(mUserMsisdn);
	    	mImageWorkerFragment = HeadlessImageDownloaderFragment.newInstance(mUserMsisdn, fileName, true, false, null, null, null, true);
	    	mImageWorkerFragment.setTaskCallbacks(this);
	        fragmentManager.beginTransaction().add(mImageWorkerFragment, HikeConstants.TAG_HEADLESS_IMAGE_DOWNLOAD_FRAGMENT).commit();
	    }
	    else
	    {
	    	//Toast.makeText(this, mContext.getResources().getString(R.string.task_already_running), Toast.LENGTH_SHORT).show();
	    	//Logger.d(TAG, "As mImageLoaderFragment already there, so not starting new one");
	    }

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
	public void onProgressUpdate(float percent)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFailed()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleUIMessage(Message msg)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCancelled()
	{
		// TODO Auto-generated method stub
		
	}
	
}
