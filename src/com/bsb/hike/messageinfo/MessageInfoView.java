package com.bsb.hike.messageinfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.dialog.ContactDialog;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.models.ContactInfoData;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.MovingList;
import com.bsb.hike.models.PhonebookContact;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.Unique;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.platform.CardRenderer;
import com.bsb.hike.platform.WebViewCardRenderer;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.smartImageLoader.StickerLoader;
import com.bsb.hike.ui.fragments.PhotoViewerFragment;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomMessageTextView;
import com.bsb.hike.view.CustomSendMessageTextView;
import com.bsb.hike.view.HoloCircularProgress;
import com.bsb.hike.voip.VoIPConstants;

/**
 * Created by ravi on 4/20/16.
 */
public class MessageInfoView
{
	ConvMessage convMessage;

	ChatTheme chatTheme;

	Context mContext;

	LayoutInflater inflater;

	private StickerLoader stickerLoader;

	private IconLoader iconLoader;

	private int mIconImageSize;

	private Activity mActivity;

	private VoiceMessagePlayer voiceMessagePlayer;

	public boolean isGroupChat = true;

	private HikeDialog dialog;


	public CardRenderer mMessageInfoCardRenderer;

	public WebViewCardRenderer mWebViewCardRenderer;

	public MovingList<ConvMessage> movingList;

	public boolean shouldShowPlayedList=false;

	public int readListString=R.string.seen_list;

	public static final int ONE_TO_ONE=1;

	public static final int GROUP_CHAT=2;

	private MovingList.OnItemsFinishedListener mOnItemsFinishedListener  = new MovingList.OnItemsFinishedListener()
	{
		@Override
		public void getMoreItems(MovingList<? extends Unique> movingList, int startIndex, int endIndex)
		{
		}

	};
	private enum ViewType
	{
		STICKER_SENT, STICKER_RECEIVE, NUDGE_SENT, NUDGE_RECEIVE, WALKIE_TALKIE_SENT, WALKIE_TALKIE_RECEIVE, VIDEO_SENT, VIDEO_RECEIVE, IMAGE_SENT, IMAGE_RECEIVE, FILE_SENT, FILE_RECEIVE, LOCATION_SENT, LOCATION_RECEIVE, CONTACT_SENT, CONTACT_RECEIVE, RECEIVE, SEND_SMS, SEND_HIKE, PARTICIPANT_INFO, FILE_TRANSFER_SEND, FILE_TRANSFER_RECEIVE, STATUS_MESSAGE, UNREAD_COUNT, TYPING_NOTIFICATION, UNKNOWN_BLOCK_ADD, PIN_TEXT_SENT, PIN_TEXT_RECEIVE, VOIP_CALL;

	};

	private ViewType vType;
	public MessageInfoView(ConvMessage convMessage, ChatTheme chatTheme, Context mContext, Conversation conversation,BaseAdapter adapter)
	{
		this.convMessage = convMessage;
		this.chatTheme = chatTheme;
		this.mContext = mContext;
		this.mActivity = (Activity) mContext;
		this.inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mIconImageSize = mContext.getResources().getDimensionPixelSize(R.dimen.icon_picture_size_messageinfo);
		this.iconLoader = new IconLoader(mContext, mIconImageSize);
		iconLoader.setDefaultAvatarIfNoCustomIcon(true);
		this.voiceMessagePlayer = new VoiceMessagePlayer();
		this.conversation = conversation;
		ArrayList<ConvMessage>list=new ArrayList<ConvMessage>();
		list.add(convMessage);
		this.movingList=new MovingList<ConvMessage>(list,mOnItemsFinishedListener);
		this.mWebViewCardRenderer=new WebViewCardRenderer(mActivity,movingList,adapter);
		this.mMessageInfoCardRenderer=new CardRenderer(mContext);
		stickerLoader = new StickerLoader.Builder()
			.downloadLargeStickerIfNotFound(true)
			.lookForOfflineSticker(true)
			.loadMiniStickerIfNotFound(true)
			.downloadMiniStickerIfNotFound(true)
			.stretchMini(true)
			.build();
		getItemViewType(convMessage);


	}
	private int viewTypeCount=ViewType.values().length;
	public void setChatTheme(ChatTheme chatTheme){
		this.chatTheme=chatTheme;
	}
	public void setConversation(Conversation conversation){
		this.conversation=conversation;
	}
	public View getView(){
		return getView(convMessage);
	}
	public View getView(View convertView,final ConvMessage convMessage){
		int type = getItemViewType(convMessage);
		MessageMetadata metadata = convMessage.getMetadata();
		View v = convertView;
		MessagesAdapter.DayHolder dayHolder=null;
		MessagesAdapter.DetailViewHolder detailViewHolder = new MessagesAdapter.DetailViewHolder();
		ViewType viewType=null;
		// Applicable to all kinds of messages
		if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.CONTENT)
		{
			v = mMessageInfoCardRenderer.getView(v, convMessage, null);
			MessagesAdapter.DetailViewHolder holder = (MessagesAdapter.DetailViewHolder) v.getTag();
			dayHolder = holder;
			setTimeNStatus(holder, true);
		}
		else if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.WEB_CONTENT) {
			//Calling the getView of another adapter to show html cards.
			v = mWebViewCardRenderer.getView(0, convertView, null);
			WebViewCardRenderer.WebViewHolder holder = (WebViewCardRenderer.WebViewHolder) v.getTag();

		}
		else if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT) {
			//Calling the getView of another adapter to show html cards.
			v = mWebViewCardRenderer.getView(0, convertView, null);
			WebViewCardRenderer.WebViewHolder holder = (WebViewCardRenderer.WebViewHolder) v.getTag();
			dayHolder = holder;
			setTimeNStatus(holder, true);
		}else{

			viewType = ViewType.values()[type];
		}
		if (viewType == ViewType.STICKER_SENT)
		{
			StickerViewHolder stickerHolder;
			if (v == null)
			{
				stickerHolder = new StickerViewHolder();

				v = inflater.inflate(R.layout.message_sent_sticker, null, false);
				stickerHolder.placeHolder = v.findViewById(R.id.placeholder);
				stickerHolder.loader = (ProgressBar) v.findViewById(R.id.loading_progress);
				stickerHolder.miniStickerLoader = (ProgressBar) v.findViewById(R.id.mini_loader);
				stickerHolder.image = (ImageView) v.findViewById(R.id.image);
				stickerHolder.time = (TextView) v.findViewById(R.id.time);
				stickerHolder.status = (ImageView) v.findViewById(R.id.status);
				stickerHolder.broadcastIndicator = (ImageView) v.findViewById(R.id.broadcastIndicator);
				stickerHolder.timeStatus = (View) v.findViewById(R.id.time_status);
				stickerHolder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
				stickerHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
				stickerHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
				v.setTag(stickerHolder);
			}
			else
			{
				stickerHolder = (StickerViewHolder) v.getTag();
			}
			stickerHolder.image.setVisibility(View.VISIBLE);
			Sticker sticker = metadata.getSticker();
			stickerLoader.loadSticker(sticker, StickerConstants.StickerType.LARGE, stickerHolder.image, false, true);
			setTimeNStatus(stickerHolder, true);

		}
		else if (viewType == ViewType.NUDGE_SENT)
		{
			NudgeViewHolder nudgeHolder = null;
			if(v==null) {
				nudgeHolder = new NudgeViewHolder();
				v = inflater.inflate(R.layout.message_sent_nudge, null, false);
				nudgeHolder.nudge = (ImageView) v.findViewById(R.id.nudge);
				nudgeHolder.time = (TextView) v.findViewById(R.id.time);
				nudgeHolder.status = (ImageView) v.findViewById(R.id.status);
				nudgeHolder.timeStatus = v.findViewById(R.id.time_status);
				nudgeHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
				nudgeHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
				v.setTag(nudgeHolder);

			}else {
				nudgeHolder=(NudgeViewHolder) v.getTag();
			}
			nudgeHolder.nudge.setImageResource(chatTheme.sentNudgeResId());
			setTimeNStatus(nudgeHolder, true);
		}
		else if (viewType == ViewType.WALKIE_TALKIE_SENT)
		{
			WalkieTalkieViewHolder wtHolder = null;
			if(v==null) {
				wtHolder=new WalkieTalkieViewHolder();
				v = inflater.inflate(R.layout.message_sent_walkie_talkie, null, false);
				wtHolder.action = (ImageView) v.findViewById(R.id.action);
				wtHolder.placeHolder = v.findViewById(R.id.placeholder);
				wtHolder.time = (TextView) v.findViewById(R.id.time);
				wtHolder.status = (ImageView) v.findViewById(R.id.status);
				wtHolder.timeStatus = v.findViewById(R.id.time_status);
				wtHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
				wtHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
				wtHolder.progress = (HoloCircularProgress) v.findViewById(R.id.play_progress);
				wtHolder.duration = (TextView) v.findViewById(R.id.duration);
				v.setTag(wtHolder);
			}else{
				wtHolder=(WalkieTalkieViewHolder)v.getTag();
			}
			wtHolder.duration.setVisibility(View.VISIBLE);
			wtHolder.action.setImageResource(R.drawable.ic_mic);
			ShapeDrawable circle = new ShapeDrawable(new OvalShape());
			circle.setIntrinsicHeight((int) (36 * Utils.scaledDensityMultiplier));
			circle.setIntrinsicWidth((int) (36 * Utils.scaledDensityMultiplier));

			/* label outgoing hike conversations in green */
			if (chatTheme == ChatTheme.DEFAULT)
			{
				circle.getPaint().setColor(mContext.getResources().getColor(!convMessage.isSMS() ? R.color.bubble_blue : R.color.bubble_green));
			}
			else
			{
				circle.getPaint().setColor(mContext.getResources().getColor(chatTheme.bubbleColor()));
			}

			wtHolder.placeHolder.setBackgroundDrawable(circle);
			setTimeNStatus(wtHolder, true);
			final HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);

			if (!TextUtils.isEmpty(hikeFile.getFileKey()))
			{
				wtHolder.action.setBackgroundResource(0);
				wtHolder.action.setImageResource(0);

				wtHolder.action.setImageResource(R.drawable.ic_mic);
				Utils.setupFormattedTime(wtHolder.duration, hikeFile.getRecordingDuration());
				wtHolder.duration.setVisibility(View.VISIBLE);
				wtHolder.progress.setVisibility(View.INVISIBLE);
			}
			wtHolder.action.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		}
		else if (viewType == ViewType.VIDEO_SENT)
		{
			VideoViewHolder videoHolder = null;
			if(v==null) {
				videoHolder=new VideoViewHolder();
				v = inflater.inflate(R.layout.message_sent_video, null, false);
				videoHolder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
				videoHolder.circularProgressBg = v.findViewById(R.id.circular_bg);
				videoHolder.initializing = (ProgressBar) v.findViewById(R.id.initializing);
				videoHolder.circularProgress = (HoloCircularProgress) v.findViewById(R.id.progress);
				videoHolder.circularProgress.setRelatedMsgId(convMessage.getMsgID());
				videoHolder.ftAction = (ImageView) v.findViewById(R.id.action);
				videoHolder.fileDetails = v.findViewById(R.id.file_details);
				videoHolder.fileSize = (TextView) v.findViewById(R.id.file_size);
				videoHolder.fileName = (TextView) v.findViewById(R.id.file_name);
				videoHolder.filmstripLeft = (ImageView) v.findViewById(R.id.filmstrip_left);
				videoHolder.filmstripRight = (ImageView) v.findViewById(R.id.filmstrip_right);
				videoHolder.broadcastIndicator = (ImageView) v.findViewById(R.id.broadcastIndicator);
				videoHolder.time = (TextView) v.findViewById(R.id.time);
				videoHolder.status = (ImageView) v.findViewById(R.id.status);
				videoHolder.timeStatus = (View) v.findViewById(R.id.time_status);
				videoHolder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
				videoHolder.messageContainer = (ViewGroup) v.findViewById(R.id.message_container);
				videoHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
				videoHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
				v.setTag(videoHolder);
			}else
			{
				videoHolder=(VideoViewHolder)v.getTag();
			}
			videoHolder.circularProgressBg.setVisibility(View.GONE);

			videoHolder.circularProgress.setVisibility(View.GONE);
			final HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			Drawable thumbnail = null;
			if (hikeFile.getThumbnail() == null && !TextUtils.isEmpty(hikeFile.getFileKey()))
			{
				thumbnail = HikeMessengerApp.getLruCache().getFileIconFromCache(hikeFile.getFileKey());
			}
			else
			{
				thumbnail = hikeFile.getThumbnail();
			}
			RelativeLayout.LayoutParams fileThumbParams = (RelativeLayout.LayoutParams) videoHolder.fileThumb.getLayoutParams();
			videoHolder.fileThumb.setBackgroundDrawable(thumbnail);// /Look for createMediaThumnail TODO: Check
			videoHolder.fileThumb.setScaleType(ImageView.ScaleType.CENTER);
			fileThumbParams.height = (int) (150 * Utils.scaledDensityMultiplier);
			fileThumbParams.width = (int) ((thumbnail.getIntrinsicWidth() * fileThumbParams.height) / thumbnail.getIntrinsicHeight());
					/*
					 * fixed the bug when image thumbnail is very big. By specifying a maximum width for the thumbnail so that download button can also fit to the screen.
					 */

			// Set Thumbnail Width
			int maxWidth = (int) (250 * Utils.scaledDensityMultiplier);
			fileThumbParams.width = Math.min(fileThumbParams.width, maxWidth);
			int minWidth = (int) (119 * Utils.scaledDensityMultiplier);
			fileThumbParams.width = Math.max(fileThumbParams.width, minWidth);
			if (fileThumbParams.width == minWidth)
			{
				fileThumbParams.height = ((thumbnail.getIntrinsicHeight() * minWidth) / thumbnail.getIntrinsicWidth());
			}
			else if (fileThumbParams.width == maxWidth)
			{
				fileThumbParams.height = ((thumbnail.getIntrinsicHeight() * maxWidth) / thumbnail.getIntrinsicWidth());
			}

			// Set Thumbnail Height
			int minHeight = (int) (70 * Utils.scaledDensityMultiplier);
			fileThumbParams.height = Math.max(fileThumbParams.height, minHeight);
			if (fileThumbParams.height == minHeight)
			{
				int width = ((thumbnail.getIntrinsicWidth() * minHeight) / thumbnail.getIntrinsicHeight());
				if (width >= minWidth && width <= maxWidth)
					fileThumbParams.width = width;
			}
			videoHolder.ftAction.setVisibility(View.VISIBLE);
			videoHolder.ftAction.setImageResource(R.drawable.ic_videoicon);
			videoHolder.fileSize.setText(Utils.getSizeForDisplay(hikeFile.getFile().length()));
			videoHolder.fileSize.setVisibility(View.VISIBLE);
			videoHolder.fileThumb.setVisibility(View.VISIBLE);
			videoHolder.filmstripLeft.setVisibility(View.VISIBLE);
			videoHolder.filmstripRight.setVisibility(View.VISIBLE);
			videoHolder.initializing.setVisibility(View.GONE);
			videoHolder.circularProgress.setVisibility(View.GONE);
			videoHolder.circularProgressBg.setVisibility(View.VISIBLE);
			videoHolder.ftAction.setScaleType(ImageView.ScaleType.CENTER);
			setBubbleColor(convMessage, videoHolder.messageContainer);
			setTimeNStatus(videoHolder, true);
		}
		else if (viewType == ViewType.IMAGE_SENT)
		{
			ImageViewHolder imageHolder=null;
			if(v==null) {
				imageHolder=new ImageViewHolder();
				v = inflater.inflate(R.layout.message_sent_image, null, false);
				imageHolder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
				imageHolder.circularProgressBg = v.findViewById(R.id.circular_bg);
				imageHolder.initializing = (ProgressBar) v.findViewById(R.id.initializing);
				imageHolder.circularProgress = (HoloCircularProgress) v.findViewById(R.id.progress);
				imageHolder.circularProgress.setRelatedMsgId(convMessage.getMsgID());
				imageHolder.ftAction = (ImageView) v.findViewById(R.id.action);
				imageHolder.fileDetails = v.findViewById(R.id.file_details);
				imageHolder.fileSize = (TextView) v.findViewById(R.id.file_size);
				imageHolder.fileName = (TextView) v.findViewById(R.id.file_name);
				imageHolder.broadcastIndicator = (ImageView) v.findViewById(R.id.broadcastIndicator);
				imageHolder.time = (TextView) v.findViewById(R.id.time);
				imageHolder.status = (ImageView) v.findViewById(R.id.status);
				imageHolder.timeStatus = (View) v.findViewById(R.id.time_status);
				imageHolder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
				imageHolder.messageContainer = (ViewGroup) v.findViewById(R.id.message_container);
				imageHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
				imageHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
				imageHolder.caption = (TextView) v.findViewById(R.id.caption);
				v.setTag(imageHolder);
			}else
			{
				imageHolder=(ImageViewHolder)v.getTag();
			}
			imageHolder.circularProgress.setVisibility(View.GONE);
			imageHolder.initializing.setVisibility(View.INVISIBLE);
			imageHolder.circularProgressBg.setVisibility(View.GONE);
			imageHolder.fileThumb.setBackgroundResource(0);
			imageHolder.fileThumb.setImageResource(0);
			createMediaThumb(imageHolder.fileThumb);
			Drawable thumbnail = null;
			final HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			if (hikeFile.getThumbnail() == null && !TextUtils.isEmpty(hikeFile.getFileKey()))
			{
				thumbnail = HikeMessengerApp.getLruCache().getFileIconFromCache(hikeFile.getFileKey());
			}
			else
			{
				thumbnail = hikeFile.getThumbnail();
			}
			imageHolder.fileThumb.setImageDrawable(thumbnail);

			RelativeLayout.LayoutParams fileThumbParams = (RelativeLayout.LayoutParams) imageHolder.fileThumb.getLayoutParams();

			{
				imageHolder.fileThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
				fileThumbParams.height = (int) (150 * Utils.scaledDensityMultiplier);
				fileThumbParams.width = (int) ((thumbnail.getIntrinsicWidth() * fileThumbParams.height) / thumbnail.getIntrinsicHeight());
				/*
				 * fixed the bug when image thumbnail is very big. By specifying a maximum width for the thumbnail so that download button can also fit to the screen.
				 */

				// Set Thumbnail Width
				int maxWidth = (int) (250 * Utils.scaledDensityMultiplier);
				fileThumbParams.width = Math.min(fileThumbParams.width, maxWidth);
				int minWidth = (int) (119 * Utils.scaledDensityMultiplier);
				fileThumbParams.width = Math.max(fileThumbParams.width, minWidth);
				if (fileThumbParams.width == minWidth)
				{
					fileThumbParams.height = ((thumbnail.getIntrinsicHeight() * minWidth) / thumbnail.getIntrinsicWidth());
				}
				else if (fileThumbParams.width == maxWidth)
				{
					fileThumbParams.height = ((thumbnail.getIntrinsicHeight() * maxWidth) / thumbnail.getIntrinsicWidth());
				}

				// Set Thumbnail Height
				int minHeight = (int) (70 * Utils.scaledDensityMultiplier);
				fileThumbParams.height = Math.max(fileThumbParams.height, minHeight);
				if (fileThumbParams.height == minHeight)
				{
					int width = ((thumbnail.getIntrinsicWidth() * minHeight) / thumbnail.getIntrinsicHeight());
					if (width >= minWidth && width <= maxWidth)
						fileThumbParams.width = width;
				}
			}
			imageHolder.fileThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imageHolder.fileThumb.setLayoutParams(fileThumbParams);

			imageHolder.fileThumb.setVisibility(View.VISIBLE);
			setBubbleColor(convMessage, imageHolder.messageContainer);
			setTimeNStatus(imageHolder, true);
			// Set caption
			if (TextUtils.isEmpty(convMessage.getMetadata().getCaption()))
			{
				imageHolder.caption.setText(null);
				imageHolder.caption.setVisibility(View.GONE);
			}
			else
			{
				CharSequence caption = SmileyParser.getInstance().addSmileySpans(convMessage.getMetadata().getCaption(),false);
				imageHolder.caption.setVisibility(View.VISIBLE);
				imageHolder.caption.setText(caption);
				Linkify.addLinks(imageHolder.caption, Linkify.ALL);
			}

		}
		else if (viewType == ViewType.LOCATION_SENT)
		{
			ImageViewHolder imageHolder=null;
			if(v==null) {
				imageHolder = new ImageViewHolder();
				v = inflater.inflate(R.layout.message_sent_image, null, false);

				imageHolder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
				imageHolder.circularProgressBg = v.findViewById(R.id.circular_bg);
				imageHolder.initializing = (ProgressBar) v.findViewById(R.id.initializing);
				imageHolder.circularProgress = (HoloCircularProgress) v.findViewById(R.id.progress);
				imageHolder.circularProgress.setRelatedMsgId(convMessage.getMsgID());
				imageHolder.ftAction = (ImageView) v.findViewById(R.id.action);
				imageHolder.fileDetails = v.findViewById(R.id.file_details);
				imageHolder.fileSize = (TextView) v.findViewById(R.id.file_size);
				imageHolder.fileName = (TextView) v.findViewById(R.id.file_name);
				imageHolder.broadcastIndicator = (ImageView) v.findViewById(R.id.broadcastIndicator);
				imageHolder.time = (TextView) v.findViewById(R.id.time);
				imageHolder.status = (ImageView) v.findViewById(R.id.status);
				imageHolder.timeStatus = (View) v.findViewById(R.id.time_status);
				imageHolder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
				imageHolder.messageContainer = (ViewGroup) v.findViewById(R.id.message_container);
				imageHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
				imageHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
				v.setTag(imageHolder);
			}else{
				imageHolder=(ImageViewHolder)v.getTag();
			}
			imageHolder.circularProgressBg.setVisibility(View.GONE);
			imageHolder.circularProgress.setVisibility(View.GONE);
			imageHolder.initializing.setVisibility(View.INVISIBLE);

			Drawable thumbnail = null;
			final HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			if (hikeFile.getThumbnail() == null && !TextUtils.isEmpty(hikeFile.getFileKey()))
			{
				thumbnail = HikeMessengerApp.getLruCache().getFileIconFromCache(hikeFile.getFileKey());
			}
			else
			{
				thumbnail = hikeFile.getThumbnail();
			}
			if (thumbnail != null)
			{
				imageHolder.fileThumb.setBackgroundDrawable(thumbnail);
			}
			else
			{
				createMediaThumb(imageHolder.fileThumb);
				imageHolder.fileThumb.setImageResource(R.drawable.ic_default_location);
				imageHolder.fileThumb.setScaleType(ImageView.ScaleType.CENTER);
			}
			setBubbleColor(convMessage,imageHolder.messageContainer);
			setTimeNStatus(imageHolder, true);
		}
		else if (viewType == ViewType.CONTACT_SENT)
		{
			FileViewHolder fileHolder=null;
			if(v==null) {
				fileHolder=new FileViewHolder();
				v = inflater.inflate(R.layout.message_sent_file, null, false);
				fileHolder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
				fileHolder.circularProgressBg = v.findViewById(R.id.circular_bg);
				fileHolder.initializing = (ProgressBar) v.findViewById(R.id.initializing);
				fileHolder.circularProgress = (HoloCircularProgress) v.findViewById(R.id.progress);
				fileHolder.circularProgress.setRelatedMsgId(convMessage.getMsgID());
				fileHolder.ftAction = (ImageView) v.findViewById(R.id.action);
				fileHolder.fileDetails = v.findViewById(R.id.file_details);
				fileHolder.fileSize = (TextView) v.findViewById(R.id.file_size);
				fileHolder.fileName = (TextView) v.findViewById(R.id.file_name);
				fileHolder.broadcastIndicator = (ImageView) v.findViewById(R.id.broadcastIndicator);
				fileHolder.time = (TextView) v.findViewById(R.id.time);
				fileHolder.status = (ImageView) v.findViewById(R.id.status);
				fileHolder.timeStatus = (View) v.findViewById(R.id.time_status);
				fileHolder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
				fileHolder.messageContainer = (ViewGroup) v.findViewById(R.id.message_container);
				fileHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
				fileHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
				v.setTag(fileHolder);
			}else{
				fileHolder=(FileViewHolder)v.getTag();
			}
			fileHolder.circularProgressBg.setVisibility(View.GONE);
			final HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			fileHolder.fileThumb.setImageResource(R.drawable.ic_default_contact);
			fileHolder.fileThumb.setScaleType(ImageView.ScaleType.CENTER);
			fileHolder.fileName.setText(hikeFile.getDisplayName());
			fileHolder.fileThumb.setScaleType(ImageView.ScaleType.CENTER);
			fileHolder.fileName.setText(hikeFile.getDisplayName());
			List<ContactInfoData> items = Utils.getContactDataFromHikeFile(hikeFile);
			String phone = null, email = null;
			for (ContactInfoData contactInfoData : items)
			{
				if (contactInfoData.getDataType() == ContactInfoData.DataType.PHONE_NUMBER)
					phone = contactInfoData.getData();

				else if (contactInfoData.getDataType() == ContactInfoData.DataType.EMAIL)
					email = contactInfoData.getData();
			}

			if (!TextUtils.isEmpty(phone))
			{
				fileHolder.fileSize.setText(phone);
				fileHolder.fileSize.setVisibility(View.VISIBLE);
			}
			else if (!TextUtils.isEmpty(email))
			{
				fileHolder.fileSize.setText(email);
				fileHolder.fileSize.setVisibility(View.VISIBLE);
			}

			fileHolder.fileThumb.setVisibility(View.VISIBLE);
			fileHolder.fileName.setVisibility(View.VISIBLE);
			fileHolder.fileDetails.setVisibility(View.VISIBLE);

			setBubbleColor(convMessage, fileHolder.messageContainer);
			setTimeNStatus(fileHolder, false);
		}
		else if (viewType == ViewType.FILE_SENT)
		{
			FileViewHolder fileHolder = null;
			if(v==null) {
				fileHolder=new FileViewHolder();
				v = inflater.inflate(R.layout.message_sent_file, null, false);
				fileHolder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
				fileHolder.fileExtension = (TextView) v.findViewById(R.id.file_extension);
				fileHolder.circularProgressBg = v.findViewById(R.id.circular_bg);
				fileHolder.initializing = (ProgressBar) v.findViewById(R.id.initializing);
				fileHolder.circularProgress = (HoloCircularProgress) v.findViewById(R.id.progress);
				fileHolder.circularProgress.setRelatedMsgId(convMessage.getMsgID());
				fileHolder.ftAction = (ImageView) v.findViewById(R.id.action);
				fileHolder.fileDetails = v.findViewById(R.id.file_details);
				fileHolder.fileSize = (TextView) v.findViewById(R.id.file_size);
				fileHolder.fileName = (TextView) v.findViewById(R.id.file_name);
				fileHolder.broadcastIndicator = (ImageView) v.findViewById(R.id.broadcastIndicator);
				fileHolder.time = (TextView) v.findViewById(R.id.time);
				fileHolder.status = (ImageView) v.findViewById(R.id.status);
				fileHolder.timeStatus = (View) v.findViewById(R.id.time_status);
				fileHolder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
				fileHolder.messageContainer = (ViewGroup) v.findViewById(R.id.message_container);
				fileHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
				fileHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
				v.setTag(fileHolder);
			}else{
				fileHolder=(FileViewHolder)v.getTag();
			}
			fileHolder.circularProgressBg.setVisibility(View.GONE);
			setBubbleColor(convMessage, fileHolder.messageContainer);
			final HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			String fileNameString = hikeFile.getFileName();
			fileHolder.fileName.setText(fileNameString);

			if ((hikeFile.getFile().length() > 0))
			{
				fileHolder.fileSize.setText(Utils.getSizeForDisplay(hikeFile.getFile().length()));
			}
			else if (hikeFile.getFileSize() > 0)
			{
				fileHolder.fileSize.setText(Utils.getSizeForDisplay(hikeFile.getFileSize()));
			}
			else
			{
				fileHolder.fileSize.setText("");
			}
			String ext = Utils.getFileExtension(hikeFile.getFileName()).toUpperCase();
			if (!TextUtils.isEmpty(ext))
			{
				fileHolder.fileExtension.setText(ext);
			}
			else
			{
				fileHolder.fileExtension.setText("?");
			}

			fileHolder.fileThumb.setVisibility(View.VISIBLE);
			fileHolder.fileName.setVisibility(View.VISIBLE);
			fileHolder.fileSize.setVisibility(View.VISIBLE);
			fileHolder.fileExtension.setVisibility(View.VISIBLE);
			fileHolder.fileDetails.setVisibility(View.VISIBLE);
			setTimeNStatus(fileHolder, false);

		}
		else if(viewType==ViewType.SEND_HIKE||viewType==ViewType.SEND_SMS)
		{
			readListString=R.string.read_list;
			TextViewHolder textHolder=null;
			if(v==null) {
				textHolder=new TextViewHolder();
				v = inflater.inflate(R.layout.message_sent_text, null, false);
				textHolder.text = (TextView) v.findViewById(R.id.text);
				textHolder.broadcastIndicator = (ImageView) v.findViewById(R.id.broadcastIndicator);
				textHolder.time = (TextView) v.findViewById(R.id.time);
				textHolder.status = (ImageView) v.findViewById(R.id.status);
				textHolder.timeStatus = (View) v.findViewById(R.id.time_status);
				textHolder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
				textHolder.messageContainer = (ViewGroup) v.findViewById(R.id.message_container);
				textHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
				textHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
				textHolder.sdrTipStub = (ViewStub) v.findViewById(R.id.sdr_ftue_tip);
				v.setTag(textHolder);
			}else
			{
				textHolder = (TextViewHolder) v.getTag();
			}
			setBubbleColor(convMessage, textHolder.messageContainer);
			CustomMessageTextView tv = (CustomMessageTextView) textHolder.text;
			tv.setDimentionMatrixHolder(convMessage);
			if (viewType == ViewType.SEND_HIKE || viewType == ViewType.SEND_SMS)
			{
				CustomSendMessageTextView sendTV = (CustomSendMessageTextView) tv;
				if ((convMessage.isBroadcastMessage() && !convMessage.isBroadcastConversation()) || (convMessage.isOfflineMessage() && convMessage.isSent()))
				{
					sendTV.setBroadcastLength();
				}
				else
				{
					sendTV.setDefaultLength();
				}
			}

			{
				CharSequence markedUp = convMessage.getMessage();
				SmileyParser smileyParser = SmileyParser.getInstance();
				markedUp = smileyParser.addSmileySpans(markedUp, false);
				textHolder.text.setText(markedUp);

			}

			Linkify.addLinks(textHolder.text, Linkify.ALL);
			Linkify.addLinks(textHolder.text, Utils.shortCodeRegex, "tel:");
			setTimeNStatus(textHolder, false);

		}
		v.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (convMessage.getMetadata() != null)
				{
					List<HikeFile> list = convMessage.getMetadata().getHikeFiles();
					if (list != null && !list.isEmpty())
					{
						final HikeFile hikeFile = list.get(0);
						if (!TextUtils.isEmpty(hikeFile.getFileKey()))
						{
							openFile(hikeFile, convMessage, v);
						}
					}
				}
			}
		});
		return v;
	}
	public View getView(final ConvMessage convMessage)
	{
		int type = getItemViewType(convMessage);
		ViewType viewType = ViewType.values()[type];
		MessageMetadata metadata = convMessage.getMetadata();
		View v = null;
		MessagesAdapter.DetailViewHolder detailViewHolder = new MessagesAdapter.DetailViewHolder();
		if (viewType == ViewType.STICKER_SENT)
		{
			v = inflater.inflate(R.layout.message_sent_sticker, null, false);
			Sticker sticker = metadata.getSticker();
			stickerLoader = new StickerLoader.Builder().downloadLargeStickerIfNotFound(true).lookForOfflineSticker(true).loadMiniStickerIfNotFound(true)
					.downloadMiniStickerIfNotFound(true).stretchMini(true).build();
			detailViewHolder.time = (TextView) v.findViewById(R.id.time);
			detailViewHolder.status = (ImageView) v.findViewById(R.id.status);
			detailViewHolder.timeStatus = v.findViewById(R.id.time_status);
			detailViewHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
			detailViewHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
			ImageView stickerImage = (ImageView) v.findViewById(R.id.image);
			stickerImage.setVisibility(View.VISIBLE);

			stickerLoader.loadSticker(sticker, StickerConstants.StickerType.LARGE, stickerImage, false, true);
			setTimeNStatus(detailViewHolder, true);
			return v;
		}
		else if (viewType == ViewType.NUDGE_SENT)
		{
			v = inflater.inflate(R.layout.message_sent_nudge, null, false);
			ImageView nudge = (ImageView) v.findViewById(R.id.nudge);
			detailViewHolder.time = (TextView) v.findViewById(R.id.time);
			detailViewHolder.status = (ImageView) v.findViewById(R.id.status);
			detailViewHolder.timeStatus = v.findViewById(R.id.time_status);
			detailViewHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
			detailViewHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
			nudge.setImageResource(chatTheme.sentNudgeResId());
			setTimeNStatus(detailViewHolder, true);
		}
		else if (viewType == ViewType.WALKIE_TALKIE_SENT)
		{
			v = inflater.inflate(R.layout.message_sent_walkie_talkie, null, false);
			ImageView action = (ImageView) v.findViewById(R.id.action);
			action.setImageResource(R.drawable.ic_mic);
			View placeHolder = v.findViewById(R.id.placeholder);
			detailViewHolder.time = (TextView) v.findViewById(R.id.time);
			detailViewHolder.status = (ImageView) v.findViewById(R.id.status);
			detailViewHolder.timeStatus = v.findViewById(R.id.time_status);
			detailViewHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
			detailViewHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
			HoloCircularProgress progress = (HoloCircularProgress) v.findViewById(R.id.play_progress);
			TextView duration = (TextView) v.findViewById(R.id.duration);
			duration.setVisibility(View.VISIBLE);
			ShapeDrawable circle = new ShapeDrawable(new OvalShape());
			circle.setIntrinsicHeight((int) (36 * Utils.scaledDensityMultiplier));
			circle.setIntrinsicWidth((int) (36 * Utils.scaledDensityMultiplier));

			/* label outgoing hike conversations in green */
			if (chatTheme == ChatTheme.DEFAULT)
			{
				circle.getPaint().setColor(mContext.getResources().getColor(!convMessage.isSMS() ? R.color.bubble_blue : R.color.bubble_green));
			}
			else
			{
				circle.getPaint().setColor(mContext.getResources().getColor(chatTheme.bubbleColor()));
			}

			placeHolder.setBackgroundDrawable(circle);
			setTimeNStatus(detailViewHolder, true);
			final HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);

			if (!TextUtils.isEmpty(hikeFile.getFileKey()))
			{
				action.setBackgroundResource(0);
				action.setImageResource(0);

				action.setImageResource(R.drawable.ic_mic);
				Utils.setupFormattedTime(duration, hikeFile.getRecordingDuration());
				duration.setVisibility(View.VISIBLE);
				progress.setVisibility(View.INVISIBLE);
			}
			action.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		}
		else if (viewType == ViewType.VIDEO_SENT)
		{
			v = inflater.inflate(R.layout.message_sent_video, null, false);
			ImageView fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
			ImageView ftAction = (ImageView) v.findViewById(R.id.action);
			ViewGroup messageContainer = (ViewGroup) v.findViewById(R.id.message_container);
			detailViewHolder.time = (TextView) v.findViewById(R.id.time);
			detailViewHolder.status = (ImageView) v.findViewById(R.id.status);
			detailViewHolder.timeStatus = v.findViewById(R.id.time_status);
			detailViewHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
			detailViewHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
			final HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			Drawable thumbnail = null;
			if (hikeFile.getThumbnail() == null && !TextUtils.isEmpty(hikeFile.getFileKey()))
			{
				thumbnail = HikeMessengerApp.getLruCache().getFileIconFromCache(hikeFile.getFileKey());
			}
			else
			{
				thumbnail = hikeFile.getThumbnail();
			}
			fileThumb.setBackgroundDrawable(thumbnail);// /Look for createMediaThumnail TODO: Check
			setTimeNStatus(detailViewHolder, true);
		}
		else if (viewType == ViewType.IMAGE_SENT)
		{
			v = inflater.inflate(R.layout.message_sent_image, null, false);
			ImageView fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
			ImageView ftAction = (ImageView) v.findViewById(R.id.action);
			detailViewHolder.time = (TextView) v.findViewById(R.id.time);
			detailViewHolder.status = (ImageView) v.findViewById(R.id.status);
			detailViewHolder.timeStatus = v.findViewById(R.id.time_status);
			detailViewHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
			detailViewHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
			ViewGroup messageContainer = (ViewGroup) v.findViewById(R.id.message_container);
			HoloCircularProgress circularProgress = (HoloCircularProgress) v.findViewById(R.id.progress);
			circularProgress.setVisibility(View.GONE);
			ProgressBar initializing = (ProgressBar) v.findViewById(R.id.initializing);
			initializing.setVisibility(View.INVISIBLE);
			View circularProgressBg = v.findViewById(R.id.circular_bg);
			circularProgressBg.setVisibility(View.GONE);

			Drawable thumbnail = null;
			final HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			if (hikeFile.getThumbnail() == null && !TextUtils.isEmpty(hikeFile.getFileKey()))
			{
				thumbnail = HikeMessengerApp.getLruCache().getFileIconFromCache(hikeFile.getFileKey());
			}
			else
			{
				thumbnail = hikeFile.getThumbnail();
			}
			fileThumb.setImageDrawable(thumbnail);

			RelativeLayout.LayoutParams fileThumbParams = (RelativeLayout.LayoutParams) fileThumb.getLayoutParams();

			{
				fileThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
				fileThumbParams.height = (int) (150 * Utils.scaledDensityMultiplier);
				fileThumbParams.width = (int) ((thumbnail.getIntrinsicWidth() * fileThumbParams.height) / thumbnail.getIntrinsicHeight());
				/*
				 * fixed the bug when image thumbnail is very big. By specifying a maximum width for the thumbnail so that download button can also fit to the screen.
				 */

				// Set Thumbnail Width
				int maxWidth = (int) (250 * Utils.scaledDensityMultiplier);
				fileThumbParams.width = Math.min(fileThumbParams.width, maxWidth);
				int minWidth = (int) (119 * Utils.scaledDensityMultiplier);
				fileThumbParams.width = Math.max(fileThumbParams.width, minWidth);
				if (fileThumbParams.width == minWidth)
				{
					fileThumbParams.height = ((thumbnail.getIntrinsicHeight() * minWidth) / thumbnail.getIntrinsicWidth());
				}
				else if (fileThumbParams.width == maxWidth)
				{
					fileThumbParams.height = ((thumbnail.getIntrinsicHeight() * maxWidth) / thumbnail.getIntrinsicWidth());
				}

				// Set Thumbnail Height
				int minHeight = (int) (70 * Utils.scaledDensityMultiplier);
				fileThumbParams.height = Math.max(fileThumbParams.height, minHeight);
				if (fileThumbParams.height == minHeight)
				{
					int width = ((thumbnail.getIntrinsicWidth() * minHeight) / thumbnail.getIntrinsicHeight());
					if (width >= minWidth && width <= maxWidth)
						fileThumbParams.width = width;
				}
			}
			fileThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
			fileThumb.setLayoutParams(fileThumbParams);

			fileThumb.setVisibility(View.VISIBLE);

			setBubbleColor(convMessage, messageContainer);
			setTimeNStatus(detailViewHolder, true);

		}
		else if (viewType == ViewType.LOCATION_SENT)
		{
			v = inflater.inflate(R.layout.message_sent_image, null, false);
			ImageView fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
			View circularProgressBg = v.findViewById(R.id.circular_bg);
			detailViewHolder.time = (TextView) v.findViewById(R.id.time);
			detailViewHolder.status = (ImageView) v.findViewById(R.id.status);
			detailViewHolder.timeStatus = v.findViewById(R.id.time_status);
			detailViewHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
			detailViewHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
			circularProgressBg.setVisibility(View.GONE);
			HoloCircularProgress circularProgress = (HoloCircularProgress) v.findViewById(R.id.progress);
			circularProgress.setVisibility(View.GONE);
			ProgressBar initializing = (ProgressBar) v.findViewById(R.id.initializing);
			initializing.setVisibility(View.INVISIBLE);

			Drawable thumbnail = null;
			final HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			if (hikeFile.getThumbnail() == null && !TextUtils.isEmpty(hikeFile.getFileKey()))
			{
				thumbnail = HikeMessengerApp.getLruCache().getFileIconFromCache(hikeFile.getFileKey());
			}
			else
			{
				thumbnail = hikeFile.getThumbnail();
			}
			if (thumbnail != null)
			{
				fileThumb.setBackgroundDrawable(thumbnail);
			}
			else
			{
				createMediaThumb(fileThumb);
				fileThumb.setImageResource(R.drawable.ic_default_location);
				fileThumb.setScaleType(ImageView.ScaleType.CENTER);
			}
			setTimeNStatus(detailViewHolder, true);
		}
		else if (viewType == ViewType.CONTACT_SENT)
		{
			v = inflater.inflate(R.layout.message_sent_file, null, false);
			ImageView fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
			View fileDetails = v.findViewById(R.id.file_details);
			TextView fileSize = (TextView) v.findViewById(R.id.file_size);
			TextView fileName = (TextView) v.findViewById(R.id.file_name);
			View circularProgressBg = v.findViewById(R.id.circular_bg);

			circularProgressBg.setVisibility(View.GONE);
			detailViewHolder.time = (TextView) v.findViewById(R.id.time);
			detailViewHolder.status = (ImageView) v.findViewById(R.id.status);
			detailViewHolder.timeStatus = v.findViewById(R.id.time_status);
			detailViewHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
			detailViewHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
			ViewGroup messageContainer = (ViewGroup) v.findViewById(R.id.message_container);
			final HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			fileThumb.setImageResource(R.drawable.ic_default_contact);
			fileThumb.setScaleType(ImageView.ScaleType.CENTER);
			fileName.setText(hikeFile.getDisplayName());
			fileThumb.setScaleType(ImageView.ScaleType.CENTER);
			fileName.setText(hikeFile.getDisplayName());
			List<ContactInfoData> items = Utils.getContactDataFromHikeFile(hikeFile);
			String phone = null, email = null;
			for (ContactInfoData contactInfoData : items)
			{
				if (contactInfoData.getDataType() == ContactInfoData.DataType.PHONE_NUMBER)
					phone = contactInfoData.getData();

				else if (contactInfoData.getDataType() == ContactInfoData.DataType.EMAIL)
					email = contactInfoData.getData();
			}

			if (!TextUtils.isEmpty(phone))
			{
				fileSize.setText(phone);
				fileSize.setVisibility(View.VISIBLE);
			}
			else if (!TextUtils.isEmpty(email))
			{
				fileSize.setText(email);
				fileSize.setVisibility(View.VISIBLE);
			}

			fileThumb.setVisibility(View.VISIBLE);
			fileName.setVisibility(View.VISIBLE);
			fileDetails.setVisibility(View.VISIBLE);

			setBubbleColor(convMessage, messageContainer);
			setTimeNStatus(detailViewHolder, false);
		}
		else if (viewType == ViewType.FILE_SENT)
		{
			v = inflater.inflate(R.layout.message_sent_file, null, false);
			ImageView fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
			TextView fileExtension = (TextView) v.findViewById(R.id.file_extension);
			View circularProgressBg = v.findViewById(R.id.circular_bg);
			circularProgressBg.setVisibility(View.GONE);
			View fileDetails = v.findViewById(R.id.file_details);
			TextView fileSize = (TextView) v.findViewById(R.id.file_size);
			TextView fileName = (TextView) v.findViewById(R.id.file_name);
			TextView time = (TextView) v.findViewById(R.id.time);
			ImageView status = (ImageView) v.findViewById(R.id.status);
			View timeStatus = (View) v.findViewById(R.id.time_status);
			ViewGroup messageContainer = (ViewGroup) v.findViewById(R.id.message_container);
			detailViewHolder.time = time;
			detailViewHolder.status = status;
			detailViewHolder.timeStatus = timeStatus;
			detailViewHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
			detailViewHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
			setBubbleColor(convMessage, messageContainer);
			final HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			String fileNameString = hikeFile.getFileName();
			fileName.setText(fileNameString);

			if ((hikeFile.getFile().length() > 0))
			{
				fileSize.setText(Utils.getSizeForDisplay(hikeFile.getFile().length()));
			}
			else if (hikeFile.getFileSize() > 0)
			{
				fileSize.setText(Utils.getSizeForDisplay(hikeFile.getFileSize()));
			}
			else
			{
				fileSize.setText("");
			}
			String ext = Utils.getFileExtension(hikeFile.getFileName()).toUpperCase();
			if (!TextUtils.isEmpty(ext))
			{
				fileExtension.setText(ext);
			}
			else
			{
				fileExtension.setText("?");
			}

			fileThumb.setVisibility(View.VISIBLE);
			fileName.setVisibility(View.VISIBLE);
			fileSize.setVisibility(View.VISIBLE);
			fileExtension.setVisibility(View.VISIBLE);
			fileDetails.setVisibility(View.VISIBLE);
			setTimeNStatus(detailViewHolder, false);

		}
		else
		{

			v = inflater.inflate(R.layout.message_sent_text, null, false);
			TextView text = (TextView) v.findViewById(R.id.text);

			ViewGroup messageContainer = (ViewGroup) v.findViewById(R.id.message_container);
			detailViewHolder.time = (TextView) v.findViewById(R.id.time);
			detailViewHolder.status = (ImageView) v.findViewById(R.id.status);
			detailViewHolder.timeStatus = v.findViewById(R.id.time_status);
			detailViewHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
			detailViewHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
			setBubbleColor(convMessage, messageContainer);
			CustomMessageTextView tv = (CustomMessageTextView) text;
			tv.setDimentionMatrixHolder(convMessage);
			if (viewType == ViewType.SEND_HIKE || viewType == ViewType.SEND_SMS)
			{
				CustomSendMessageTextView sendTV = (CustomSendMessageTextView) tv;
				if ((convMessage.isBroadcastMessage() && !convMessage.isBroadcastConversation()) || (convMessage.isOfflineMessage() && convMessage.isSent()))
				{
					sendTV.setBroadcastLength();
				}
				else
				{
					sendTV.setDefaultLength();
				}
			}

			{
				CharSequence markedUp = convMessage.getMessage();
				SmileyParser smileyParser = SmileyParser.getInstance();
				markedUp = smileyParser.addSmileySpans(markedUp, false);
				text.setText(markedUp);

			}
			setTimeNStatus(detailViewHolder, false);

		}
		v.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (convMessage.getMetadata() != null)
				{
					List<HikeFile> list = convMessage.getMetadata().getHikeFiles();
					if (list != null && !list.isEmpty())
					{
						final HikeFile hikeFile = list.get(0);
						if (!TextUtils.isEmpty(hikeFile.getFileKey()))
						{
							openFile(hikeFile, convMessage, v);
						}
					}
				}
			}
		});
		return v;
	}


	/**
	 * Returns what type of View this item is going to result in * @return an integer
	 */

	public int getItemViewType(ConvMessage message)
	{

		ViewType type = ViewType.SEND_HIKE;

		MessageMetadata metadata = convMessage.getMetadata();
		if (convMessage.isStickerMessage())
		{
			type = ViewType.STICKER_SENT;

		}
		else if (metadata != null && metadata.isPokeMessage())
		{
			if (convMessage.isSent())
			{
				type = ViewType.NUDGE_SENT;
			}

		}
		else if (convMessage.isFileTransferMessage())
		{
			final HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			HikeFile.HikeFileType hikeFileType = hikeFile.getHikeFileType();

			type = ViewType.FILE_SENT;

			if (hikeFileType == HikeFile.HikeFileType.AUDIO_RECORDING)
			{
				type = ViewType.WALKIE_TALKIE_SENT;

			}
			else if (hikeFileType == HikeFile.HikeFileType.VIDEO)
			{
				if (hikeFile.getThumbnail() != null || HikeMessengerApp.getLruCache().getFileIconFromCache(hikeFile.getFileKey()) != null)
				{
					type = ViewType.VIDEO_SENT;

				}
			}
			else if (hikeFileType == HikeFile.HikeFileType.IMAGE)
			{
				if (hikeFile.getThumbnail() != null || HikeMessengerApp.getLruCache().getFileIconFromCache(hikeFile.getFileKey()) != null)
				{

					type = ViewType.IMAGE_SENT;

				}
			}
			else if (hikeFileType == HikeFile.HikeFileType.LOCATION)
			{
				if (convMessage.isSent())
				{
					type = ViewType.LOCATION_SENT;
				}
				else
				{
					type = ViewType.LOCATION_RECEIVE;
				}
			}
			else if (hikeFileType == HikeFile.HikeFileType.CONTACT)
			{
				if (convMessage.isSent())
				{
					type = ViewType.CONTACT_SENT;
				}
				else
				{
					type = ViewType.CONTACT_RECEIVE;
				}
			}
		}
		else if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.CONTENT)
		{
			;
			return viewTypeCount + mMessageInfoCardRenderer.getItemViewType(convMessage);
		}
		else if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.WEB_CONTENT || convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT)
		{
			return viewTypeCount + mMessageInfoCardRenderer.getCardCount() + mWebViewCardRenderer.getItemViewType(0);
		}
		else if (convMessage.isBlockAddHeader())
		{
			Logger.i("chatthread", "getview type unknown header");
			type = ViewType.UNKNOWN_BLOCK_ADD;
		}
		else if (convMessage.getParticipantInfoState() == ConvMessage.ParticipantInfoState.STATUS_MESSAGE)
		{
			type = ViewType.STATUS_MESSAGE;
		}
		else if (convMessage.getParticipantInfoState() == ConvMessage.ParticipantInfoState.VOIP_CALL_SUMMARY
				|| convMessage.getParticipantInfoState() == ConvMessage.ParticipantInfoState.VOIP_MISSED_CALL_INCOMING
				|| convMessage.getParticipantInfoState() == ConvMessage.ParticipantInfoState.VOIP_MISSED_CALL_OUTGOING)
		{
			type = ViewType.VOIP_CALL;
		}
		else if (convMessage.getParticipantInfoState() != ConvMessage.ParticipantInfoState.NO_INFO)
		{
			type = ViewType.PARTICIPANT_INFO;
		}
		else if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
		{
			type = ViewType.PIN_TEXT_SENT;

		}
		else
		{

			type = ViewType.SEND_HIKE;

		}
		vType=type;
		return type.ordinal();
	}

	private void setBubbleColor(ConvMessage convMessage, ViewGroup messageContainer)
	{
		int leftPad = messageContainer.getPaddingLeft();
		int topPad = messageContainer.getPaddingTop();
		int rightPad = messageContainer.getPaddingRight();
		int bottomPad = messageContainer.getPaddingBottom();
		if (convMessage.isSent() && messageContainer != null)
		{
			if (chatTheme == ChatTheme.DEFAULT)
			{
				messageContainer.setBackgroundResource(!convMessage.isSMS() ? R.drawable.ic_bubble_blue_selector : R.drawable.ic_bubble_green_selector);
			}
			else
			{
				messageContainer.setBackgroundResource(chatTheme.bubbleResId());
			}
		}
		messageContainer.setPadding(leftPad, topPad, rightPad, bottomPad);
	}

	private void createMediaThumb(ImageView fileThumb)
	{
		// TODO Auto-generated method stub
		Logger.d(getClass().getSimpleName(), "creating default thumb. . . ");
		int pixels = mContext.getResources().getDimensionPixelSize(R.dimen.file_thumbnail_size);
		Logger.d(getClass().getSimpleName(), "density: " + Utils.scaledDensityMultiplier);
		fileThumb.getLayoutParams().height = pixels;
		fileThumb.getLayoutParams().width = pixels;
		fileThumb.setBackgroundResource(R.drawable.bg_file_thumb);
		/*
		 * When setting default media thumb to image view, need to remove the previous drawable of that view in case of view is re-used by adapter. Fogbugz Id : 37212
		 */
		fileThumb.setImageDrawable(null);
	}




	public static class FTViewHolder extends MessagesAdapter.DetailViewHolder
	{
		public ImageView fileThumb;

		public View circularProgressBg;

		public HoloCircularProgress circularProgress;

		public ProgressBar initializing;

		public ImageView ftAction;

		public View fileDetails;

		public TextView fileSize;

		public TextView fileName;

		public TextView fileExtension;
	}

	private static class StickerViewHolder extends MessagesAdapter.DetailViewHolder
	{
		View placeHolder;

		ProgressBar loader;

		ImageView image;

		ProgressBar miniStickerLoader;
	}

	private static class NudgeViewHolder extends MessagesAdapter.DetailViewHolder
	{
		ImageView nudge;
	}

	private static class WalkieTalkieViewHolder extends MessagesAdapter.DetailViewHolder
	{
		View placeHolder;

		ProgressBar initialization;

		HoloCircularProgress progress;

		ImageView action;

		TextView duration;
	}

	private static class VideoViewHolder extends FTViewHolder
	{
		ImageView filmstripLeft;

		ImageView filmstripRight;
	}

	private static class ImageViewHolder extends FTViewHolder
	{

		public TextView caption;
	}

	private static class FileViewHolder extends FTViewHolder
	{
	}

	private static class TextViewHolder extends MessagesAdapter.DetailViewHolder
	{
		TextView text;

		ViewStub sdrTipStub;
	}

	private static class StatusViewHolder extends MessagesAdapter.DayHolder
	{
		ImageView image;

		ImageView avatarFrame;

		TextView messageTextView;

		TextView dayTextView;

		ViewGroup container;

		TextView messageInfo;

	}

	private static class VoipInfoHolder extends MessagesAdapter.DayHolder
	{

		ImageView image;

		TextView text;

		TextView messageInfo;
	}

	private Conversation conversation;

	private void openFile(HikeFile hikeFile, ConvMessage convMessage, View parent)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_OPENED, hikeFile.getHikeFileType());

		Logger.d(getClass().getSimpleName(), "Opening file");
		Intent openFile = new Intent(Intent.ACTION_VIEW);
		switch (hikeFile.getHikeFileType())
		{
		case LOCATION:
			String uri = String.format(Locale.US, "geo:%1$f,%2$f?z=%3$d&q=%1$f,%2$f", hikeFile.getLatitude(), hikeFile.getLongitude(), hikeFile.getZoomLevel());
			openFile.setData(Uri.parse(uri));
			break;
		case CONTACT:
			 saveContact(hikeFile);
			return;
		case AUDIO_RECORDING:
			if (hikeFile.getFilePath() == null)
			{
				Toast.makeText(mContext, R.string.unable_to_open, Toast.LENGTH_SHORT).show();
				return;
			}
			String fileKey = hikeFile.getFileKey();

			ImageView recAction = (ImageView) parent.findViewById(R.id.action);
			TextView durationTxt = (TextView) parent.findViewById(R.id.duration);
			View durationProgress = (View) parent.findViewById(R.id.play_progress);
			durationTxt.setVisibility(View.VISIBLE);
			durationProgress.setVisibility(View.VISIBLE);

			if (fileKey.equals(voiceMessagePlayer.getFileKey()))
			{
				recAction.setTag(fileKey);
				voiceMessagePlayer.setFileBtn(recAction);
				durationTxt.setTag(fileKey);
				voiceMessagePlayer.setDurationTxt(durationTxt, durationProgress);

				if (voiceMessagePlayer.getPlayerState() == VoiceMessagePlayerState.PLAYING)
				{
					voiceMessagePlayer.pausePlayer();
				}
				else if (voiceMessagePlayer.getPlayerState() == VoiceMessagePlayerState.PAUSED)
				{
					voiceMessagePlayer.resumePlayer();
				}
				else if (voiceMessagePlayer.getPlayerState() == VoiceMessagePlayerState.STOPPED)
				{
					voiceMessagePlayer.playMessage(hikeFile);
				}
			}
			else
			{
				if (voiceMessagePlayer.getPlayerState() == VoiceMessagePlayerState.PLAYING || voiceMessagePlayer.getPlayerState() == VoiceMessagePlayerState.PAUSED)
				{
					voiceMessagePlayer.resetPlayer();
				}

				recAction.setTag(fileKey);
				voiceMessagePlayer.setFileBtn(recAction);
				durationTxt.setTag(fileKey);
				voiceMessagePlayer.setDurationTxt(durationTxt, durationProgress);

				voiceMessagePlayer.playMessage(hikeFile);
			}
			return;
		case IMAGE:
		case VIDEO:
			if (hikeFile.exactFilePathFileExists())
			{
				ArrayList<HikeSharedFile> hsf = new ArrayList<HikeSharedFile>();
				HikeSharedFile sharedFile = new HikeSharedFile(hikeFile.serialize(), hikeFile.isSent(), convMessage.getMsgID(), convMessage.getMsisdn(),
						convMessage.getTimestamp(), convMessage.getGroupParticipantMsisdn());
				if (!TextUtils.isEmpty(hikeFile.getCaption()))
				{
					sharedFile.setCaption(hikeFile.getCaption());
				}
				hsf.add(sharedFile);
				PhotoViewerFragment.openPhoto(R.id.parent_layout, mContext, hsf, true, conversation);
			}
			else
			{
				Toast.makeText(mContext, R.string.unable_to_open, Toast.LENGTH_SHORT).show();
			}
			return;

		default:
			HikeFile.openFile(hikeFile, mContext);
			return;
		}
		try
		{
			mContext.startActivity(openFile);
		}
		catch (ActivityNotFoundException e)
		{
			Logger.w(getClass().getSimpleName(), "Trying to open an unknown format", e);
			Toast.makeText(mContext, R.string.unknown_msg, Toast.LENGTH_SHORT).show();
		}

	}

	enum VoiceMessagePlayerState
	{
		PLAYING, PAUSED, STOPPED
	};

	private class VoiceMessagePlayer implements SensorEventListener
	{
		String fileKey;

		MediaPlayer mediaPlayer;

		ImageView fileBtn;

		TextView durationTxt;

		View durationProgress;

		Handler handler;

		VoiceMessagePlayerState playerState;

		private SensorManager sensorManager;

		private Sensor proximitySensor;

		private boolean proximitySensorExists;

		private AudioManager audioManager;

		private float proximitySensorMaxRange;

		private int initialAudioMode;

		private HeadSetConnectionReceiver headsetReceiver;

		IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);

		public VoiceMessagePlayer()
		{
			handler = new Handler();
			audioManager = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
			initialAudioMode = audioManager.getMode();
			sensorManager = (SensorManager) mActivity.getSystemService(Context.SENSOR_SERVICE);
			headsetReceiver = new HeadSetConnectionReceiver();
			proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

			if (proximitySensor == null)
			{
				proximitySensorExists = false;
			}
			else
			{
				proximitySensorExists = true;
				proximitySensorMaxRange = proximitySensor.getMaximumRange();
			}
		}

		public void playMessage(HikeFile hikeFile)
		{
			Utils.blockOrientationChange(mActivity);

			playerState = VoiceMessagePlayerState.PLAYING;
			fileKey = hikeFile.getFileKey();

			try
			{
				audioManager.setMode(AudioManager.STREAM_MUSIC);
				mediaPlayer = new MediaPlayer();
				mediaPlayer.setDataSource(hikeFile.getFilePath());
				mediaPlayer.prepare();
				mediaPlayer.start();

				setFileBtnResource();

				mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
				{
					@Override
					public void onCompletion(MediaPlayer mp)
					{
						resetPlayer();
					}
				});
				handler.post(updateTimer);

				registerPoximitySensor();
				registerHeadSetReceiver();
			}
			catch (IllegalArgumentException e)
			{
				Logger.w(getClass().getSimpleName(), e);
			}
			catch (IllegalStateException e)
			{
				Logger.w(getClass().getSimpleName(), e);
			}
			catch (IOException e)
			{
				Logger.w(getClass().getSimpleName(), e);
			}
		}

		public void pausePlayer()
		{
			Utils.unblockOrientationChange(mActivity);
			if (mediaPlayer == null)
			{
				return;
			}
			playerState = VoiceMessagePlayerState.PAUSED;
			mediaPlayer.pause();
			setTimer();
			setFileBtnResource();
			unregisterProximitySensor();
			unregisterHeadserReceiver();
		}

		public void resumePlayer()
		{
			if (mediaPlayer == null)
			{
				return;
			}
			Utils.blockOrientationChange(mActivity);
			playerState = VoiceMessagePlayerState.PLAYING;
			mediaPlayer.start();
			handler.post(updateTimer);
			setFileBtnResource();

			registerPoximitySensor();
			registerHeadSetReceiver();
		}

		public void resetPlayer()
		{
			Utils.unblockOrientationChange(mActivity);
			playerState = VoiceMessagePlayerState.STOPPED;

			setTimer();
			setFileBtnResource();

			if (mediaPlayer != null)
			{
				mediaPlayer.stop();
				mediaPlayer.reset();
				mediaPlayer.release();
				mediaPlayer = null;
			}
			fileBtn = null;
			durationTxt = null;
			durationProgress = null;

			unregisterProximitySensor();
			audioManager.setMode(initialAudioMode);
		}

		@Override
		public void onSensorChanged(SensorEvent sensorEvent)
		{
			float distance = sensorEvent.values[0];
			if (distance != proximitySensorMaxRange)
			{
				Logger.d(VoIPConstants.TAG, "Phone is near.");
				audioManager.setSpeakerphoneOn(false);
			}
			else
			{
				Logger.d(VoIPConstants.TAG, "Phone is far.");
				audioManager.setSpeakerphoneOn(true);
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int i)
		{
			// Do nothing
		}

		/*
		 * AND-4884: Listen to headset plug and unregister proximity sensor when plugged and register back
		 * 
		 * @TODO: check for behavior when BT/wireless headset is connected.
		 */
		private static final int HEADSET_UNPLUGGED = 0;

		private static final int HEADSET_PLUGGED = 1;

		private boolean mIsSensorResgistered = false, mIsHeadSetRegistered = false;

		// Crash observed when audio message is double tapped or setDataSource throws exception
		// as Playmessage havent finished register and we call the unregister from pause
		private void registerHeadSetReceiver()
		{
			if (!mIsHeadSetRegistered)
			{
				mActivity.registerReceiver(headsetReceiver, filter);
				mIsHeadSetRegistered = true;
			}
		}

		private void unregisterHeadserReceiver()
		{
			if (mIsHeadSetRegistered)
			{
				mActivity.unregisterReceiver(headsetReceiver);
				mIsHeadSetRegistered = false;
			}
		}

		private class HeadSetConnectionReceiver extends BroadcastReceiver
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG))
				{
					int state = intent.getIntExtra("state", -1);
					switch (state)
					{
					case HEADSET_UNPLUGGED:
						audioManager.setMode(AudioManager.STREAM_MUSIC);
						audioManager.setSpeakerphoneOn(true);
						registerPoximitySensor();
						break;
					case HEADSET_PLUGGED:
						audioManager.setMode(AudioManager.USE_DEFAULT_STREAM_TYPE);
						audioManager.setSpeakerphoneOn(false);
						unregisterProximitySensor();
						break;
					}
				}
			}
		}

		private void registerPoximitySensor()
		{
			if (proximitySensorExists && !mIsSensorResgistered)
			{
				sensorManager.registerListener(VoiceMessagePlayer.this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
				mIsSensorResgistered = true;
			}
		}

		private void unregisterProximitySensor()
		{
			if (proximitySensorExists && mIsSensorResgistered)
			{
				sensorManager.unregisterListener(VoiceMessagePlayer.this);
				mIsSensorResgistered = false;
			}
		}

		public String getFileKey()
		{
			return fileKey;
		}

		public VoiceMessagePlayerState getPlayerState()
		{
			return playerState;
		}

		public void setDurationTxt(TextView durationTxt, View durationProgress)
		{
			this.durationTxt = durationTxt;
			this.durationProgress = durationProgress;
			setTimer();
		}

		public void setFileBtn(ImageView fileBtn)
		{
			this.fileBtn = fileBtn;
		}

		public void setFileBtnResource()
		{
			if (fileBtn == null)
			{
				return;
			}
			String btnFileKey = (String) fileBtn.getTag();
			if (!fileKey.equals(btnFileKey))
			{
				return;
			}
			fileBtn.setImageResource(playerState != VoiceMessagePlayerState.PLAYING ? R.drawable.ic_mic : R.drawable.ic_pause_rec);
		}

		Runnable updateTimer = new Runnable()
		{

			@Override
			public void run()
			{
				setTimer();
				if (playerState == VoiceMessagePlayerState.PLAYING)
				{
					handler.postDelayed(updateTimer, 500);
				}
			}
		};

		private void setTimer()
		{
			if (durationTxt == null || durationProgress == null || fileKey == null || mediaPlayer == null)
			{
				return;
			}
			String txtFileKey = (String) durationTxt.getTag();
			if (!fileKey.equals(txtFileKey))
			{
				return;
			}
			try
			{
				int duration = mediaPlayer.getDuration();

				switch (playerState)
				{
				case PLAYING:
				case PAUSED:
					int progress = 0;
					if (duration > 0)
						progress = (mediaPlayer.getCurrentPosition() * 100) / duration;
					((HoloCircularProgress) durationProgress).setProgress(progress * 0.01f);
					Utils.setupFormattedTime(durationTxt, mediaPlayer.getCurrentPosition() / 1000);
					break;
				case STOPPED:
					((HoloCircularProgress) durationProgress).resetProgress();
					Utils.setupFormattedTime(durationTxt, duration / 1000);
					break;

				}
			}
			catch (IllegalStateException e)
			{
				/*
				 * This can be thrown if we try to get the duration of the media player when it has already stopped.
				 */
				Logger.w(getClass().getSimpleName(), e);
			}
		}

	}

	private void setTimeNStatus(MessagesAdapter.DetailViewHolder detailHolder, boolean ext)
	{
		ConvMessage message = convMessage;
		TextView time = detailHolder.time;
		ImageView status = detailHolder.status;
		View timeStatus = detailHolder.timeStatus;
		time.setText(message.getTimestampFormatted(false, mContext));
		time.setVisibility(View.VISIBLE);
		if (message.isSent())
		{
			if (message.isFileTransferMessage() && (TextUtils.isEmpty(message.getMetadata().getHikeFiles().get(0).getFileKey())))
			{
				if (ext)
				{
					status.setImageResource(R.drawable.ic_clock_white);
					status.setContentDescription(mContext.getResources().getString(R.string.content_des_message_clock_state));
				}
				else
				{
					status.setImageResource(R.drawable.ic_clock);
					status.setContentDescription(mContext.getResources().getString(R.string.content_des_message_clock_state));
				}
			}
			else if (ext)
			{
				switch (message.getState())
				{
				case SENT_UNCONFIRMED:
					status.setImageResource(R.drawable.ic_clock_white);
					status.setContentDescription(mContext.getResources().getString(R.string.content_des_message_clock_state));
					break;
				case SENT_CONFIRMED:
					setIconForSentMessage(message, status, R.drawable.ic_tick_white, R.drawable.ic_sms_white, R.drawable.ic_bolt_white);
					break;
				case SENT_DELIVERED:
					status.setImageResource(R.drawable.ic_double_tick_white);
					status.setContentDescription(mContext.getResources().getString(R.string.content_des_message_double_tick_state));
					break;
				case SENT_DELIVERED_READ:
					status.setImageResource(R.drawable.ic_double_tick_r_white);
					status.setContentDescription(mContext.getResources().getString(R.string.content_des_message_double_tick_read_state));
					break;
				default:
					break;
				}
			}
			else
			{
				switch (message.getState())
				{
				case SENT_UNCONFIRMED:
					status.setImageResource(R.drawable.ic_clock);
					status.setContentDescription(mContext.getResources().getString(R.string.content_des_message_clock_state));
					break;
				case SENT_CONFIRMED:
					setIconForSentMessage(message, status, R.drawable.ic_tick, R.drawable.ic_sms, R.drawable.ic_bolt_grey);
					break;
				case SENT_DELIVERED:
					status.setImageResource(R.drawable.ic_double_tick);
					status.setContentDescription(mContext.getResources().getString(R.string.content_des_message_double_tick_state));
					break;
				case SENT_DELIVERED_READ:
					status.setImageResource(R.drawable.ic_double_tick_r);
					status.setContentDescription(mContext.getResources().getString(R.string.content_des_message_double_tick_read_state));
					break;
				default:
					break;
				}
			}
			status.setScaleType(ImageView.ScaleType.CENTER);
			status.setVisibility(View.VISIBLE);
		}

		if (timeStatus != null)
			timeStatus.setVisibility(View.VISIBLE);

		if ((message.getState() != null)
				&& ((message.getState() == ConvMessage.State.SENT_DELIVERED_READ) || message.getState() == ConvMessage.State.SENT_UNCONFIRMED || message.getState() == ConvMessage.State.SENT_CONFIRMED))
		{
			inflateNSetMessageInfo(convMessage, detailHolder);
		}
		else if (detailHolder.messageInfoInflated != null)
		{
			detailHolder.messageInfoInflated.setVisibility(View.GONE);
		}
	}

	private void setIconForSentMessage(ConvMessage message, ImageView status, int tickResId, int smsDrawableResId, int boltDrawableResId)
	{
		if (conversation.isOnHike() && !(conversation instanceof OneToNConversation) && !message.isBroadcastMessage())
		{
			if (message.isSMS())
			{
				status.setImageResource(smsDrawableResId);
				status.setContentDescription(mContext.getResources().getString(R.string.content_des_message_offline_state));
				return;
			}

		}
		status.setImageResource(tickResId);
		status.setContentDescription(mContext.getResources().getString(R.string.content_des_message_clock_state));
	}

	private void inflateNSetMessageInfo(final ConvMessage message, final MessagesAdapter.DetailViewHolder detailHolder)
	{
		if (detailHolder.messageInfoInflated == null)
		{
			detailHolder.messageInfoStub.setOnInflateListener(new ViewStub.OnInflateListener()
			{
				@Override
				public void onInflate(ViewStub stub, View inflated)
				{
					detailHolder.messageInfoInflated = inflated;
					setMessageInfo(message, detailHolder.messageInfoInflated);
				}
			});
			try
			{
				detailHolder.messageInfoStub.inflate();
			}
			catch (Exception e)
			{

			}
		}
		else
		{
			detailHolder.messageInfoInflated.setVisibility(View.VISIBLE);
			setMessageInfo(message, detailHolder.messageInfoInflated);
		}
	}

	private void setMessageInfo(ConvMessage message, View inflated)
	{
		TextView messageInfo = (TextView) inflated.findViewById(R.id.message_info);
		ImageView sending = (ImageView) inflated.findViewById(R.id.sending_anim);

		messageInfo.setVisibility(View.GONE);
		sending.setVisibility(View.GONE);
		inflated.setVisibility(View.GONE);
		if (message.getState() == ConvMessage.State.SENT_DELIVERED_READ && isGroupChat)
		{
			inflated.setVisibility(View.VISIBLE);
			messageInfo.setVisibility(View.VISIBLE);
			messageInfo.setTextColor(mContext.getResources().getColor(chatTheme == chatTheme.DEFAULT ? R.color.list_item_subtext : R.color.white));

		}
	}

	public int getReadListHeaderString(int chatType){
		if(vType==ViewType.SEND_HIKE||vType==ViewType.SEND_SMS){
			if(chatType==GROUP_CHAT)
				readListString=R.string.read_list;
			else
				readListString=R.string.read_list_one;
		}
		else{
			if(chatType==GROUP_CHAT)
				readListString=R.string.seen_list;
			else
				readListString=R.string.seen_list_one;
		}

		return readListString;
	}
	public int getReadListExpandedString(){
		if(vType==ViewType.SEND_HIKE||vType==ViewType.SEND_SMS)
			readListString=R.string.readExpanded;
		else
			readListString=R.string.seenExpanded;
		return readListString;
	}
	public boolean shouldShowPlayedByList(){
		return false;
	}



	private void saveContact(HikeFile hikeFile)
	{

		final String name = hikeFile.getDisplayName();

		final List<ContactInfoData> items = Utils.getContactDataFromHikeFile(hikeFile);

		PhonebookContact contact = new PhonebookContact();
		contact.name = name;
		contact.items = items;

		this.dialog = HikeDialogFactory.showDialog(mContext, HikeDialogFactory.CONTACT_SAVE_DIALOG, new HikeDialogListener() {

			@Override
			public void positiveClicked(HikeDialog hikeDialog) {
				Spinner accounts = (Spinner) ((ContactDialog) hikeDialog).findViewById(R.id.account_spinner);

				if (accounts.getSelectedItem() != null) {
					Utils.addToContacts(items, name, mContext, accounts);
				} else {
					Utils.addToContacts(items, name, mContext);
				}

				hikeDialog.dismiss();
			}

			@Override
			public void neutralClicked(HikeDialog hikeDialog) {

			}

			@Override
			public void negativeClicked(HikeDialog hikeDialog) {
				hikeDialog.dismiss();
			}
		}, contact, mContext.getString(R.string.SAVE), true);
	}

}
