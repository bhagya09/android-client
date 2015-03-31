package com.bsb.hike.adapters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FileSavedState;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.BroadcastConversation;
import com.bsb.hike.models.ContactInfoData;
import com.bsb.hike.models.ContactInfoData.DataType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupTypingNotification;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.MessageMetadata.NudgeAnimationType;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickerdownloadmgr.IStickerResultListener;
import com.bsb.hike.modules.stickerdownloadmgr.StickerDownloadManager;
import com.bsb.hike.modules.stickerdownloadmgr.StickerException;
import com.bsb.hike.platform.CardRenderer;
import com.bsb.hike.platform.WebViewCardRenderer;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.smartImageLoader.HighQualityThumbLoader;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.HikeDialog;
import com.bsb.hike.ui.HikeDialog.HikeDialogListener;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.fragments.PhotoViewerFragment;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.GroupUtils;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;
import com.bsb.hike.view.CustomSendMessageTextView;
import com.bsb.hike.view.HoloCircularProgress;


public class MessagesAdapter extends BaseAdapter implements OnClickListener, OnLongClickListener, OnCheckedChangeListener
{
	private enum ViewType
	{
		STICKER_SENT, STICKER_RECEIVE, NUDGE_SENT, NUDGE_RECEIVE, WALKIE_TALKIE_SENT, WALKIE_TALKIE_RECEIVE, VIDEO_SENT, VIDEO_RECEIVE, IMAGE_SENT, IMAGE_RECEIVE, FILE_SENT, FILE_RECEIVE, LOCATION_SENT, LOCATION_RECEIVE, CONTACT_SENT, CONTACT_RECEIVE, RECEIVE, SEND_SMS, SEND_HIKE, PARTICIPANT_INFO, FILE_TRANSFER_SEND, FILE_TRANSFER_RECEIVE, STATUS_MESSAGE, UNREAD_COUNT, TYPING_NOTIFICATION, UNKNOWN_BLOCK_ADD, PIN_TEXT_SENT, PIN_TEXT_RECEIVE,
		VOIP_CALL;
		
	};

	private int viewTypeCount = ViewType.values().length;

	public static class DayHolder
	{
		public ViewStub dayStub;

		public View dayStubInflated;
	}

	public static class DetailViewHolder extends DayHolder
	{

		public ImageView status;

		public ImageView broadcastIndicator;
		
		public TextView time;

		public View timeStatus;

		public View senderDetails;

		public TextView senderName;

		public TextView senderNameUnsaved;

		public ImageView avatarImage;

		public ViewGroup avatarContainer;

		public View selectedStateOverlay;

		public ViewGroup messageContainer;

		public ViewStub messageInfoStub;

		public View messageInfoInflated;
	}

	private static class FTViewHolder extends DetailViewHolder
	{
		ImageView fileThumb;

		View circularProgressBg;

		HoloCircularProgress circularProgress;

		ProgressBar initializing;

		ImageView ftAction;

		View fileDetails;

		TextView fileSize;

		TextView fileName;

		TextView fileExtension;
	}

	private static class StickerViewHolder extends DetailViewHolder
	{
		View placeHolder;

		ProgressBar loader;

		ImageView image;
	}

	private static class NudgeViewHolder extends DetailViewHolder
	{
		ImageView nudge;
	}

	private static class WalkieTalkieViewHolder extends DetailViewHolder
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
	}

	private static class FileViewHolder extends FTViewHolder
	{
	}

	private static class TextViewHolder extends DetailViewHolder
	{
		TextView text;

		ViewStub sdrTipStub;
	}

	private static class StatusViewHolder extends DayHolder
	{
		ImageView image;

		ImageView avatarFrame;

		TextView messageTextView;

		TextView dayTextView;

		ViewGroup container;

		TextView messageInfo;

	}
	
	private static class VoipInfoHolder extends DayHolder
	{
		
		ImageView image;
		
		TextView text;

		TextView messageInfo;
	}

	private static class ParticipantInfoHolder extends DayHolder
	{
		ViewGroup container;
	}

	private static class TypingViewHolder
	{
		ImageView typing;

		ViewGroup typingAvatarContainer;
	}

	private Conversation conversation;

	private ArrayList<ConvMessage> convMessages;

	private Context context;

	private ChatThread chatThread;

	private TextView smsToggleSubtext;

	private TextView hikeSmsText;

	private TextView regularSmsText;

	private ShowUndeliveredMessage showUndeliveredMessage;

	private int lastSentMessagePosition = -1;

	private VoiceMessagePlayer voiceMessagePlayer;

	private long msgIdForSdrTip = -1;

	private SharedPreferences preferences;

	private boolean isGroupChat;

	private ChatTheme chatTheme;

	private boolean isDefaultTheme = true;

	private IconLoader iconLoader;

	// private StickerLoader largeStickerLoader;
	private int mIconImageSize;

	private Set<Long> mSelectedItemsIds;

	private boolean isActionModeOn = false;

	private boolean shownSdrIntroTip = true;

	private boolean sdrTipFadeInShown = false;

	private boolean isHikeToOfflineMode = false;

	private String myMsisdn;

	private HighQualityThumbLoader hqThumbLoader;

	private CardRenderer mChatThreadCardRenderer;

	private WebViewCardRenderer mWebViewCardRenderer;

	/*
	 * this is set of all the currently visible messages which are stuck in tick and are not sms
	 */
	private LinkedHashMap<Long, ConvMessage> undeliveredMessages = new LinkedHashMap<Long, ConvMessage>();

	/*
	 * this variable will point to first ConvMessage object which is stuck in tick and have not sent as sms till now. if there is no such message than this should point to null.
	 */
	private ConvMessage firstPendingConvMessage = null;

	public MessagesAdapter(Activity context, ArrayList<ConvMessage> objects, Conversation conversation, ChatThread chatThread)
	{
		mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		// this.largeStickerLoader = new StickerLoader(context);
		this.iconLoader = new IconLoader(context, mIconImageSize);
		iconLoader.setDefaultAvatarIfNoCustomIcon(true);
		this.context = context;
		this.convMessages = objects;
		this.conversation = conversation;
		this.chatThread = chatThread;
		this.voiceMessagePlayer = new VoiceMessagePlayer();
		this.preferences = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		this.isGroupChat = Utils.isGroupConversation(conversation.getMsisdn());
		this.chatTheme = ChatTheme.DEFAULT;
		this.mSelectedItemsIds = new HashSet<Long>();
		setLastSentMessagePosition();
		this.shownSdrIntroTip = preferences.getBoolean(HikeMessengerApp.SHOWN_SDR_INTRO_TIP, false);
		this.myMsisdn = preferences.getString(HikeMessengerApp.MSISDN_SETTING, "");

		hqThumbLoader = new HighQualityThumbLoader();
		hqThumbLoader.setImageFadeIn(false);
		hqThumbLoader.setDefaultDrawableNull(false);
		this.mChatThreadCardRenderer = new CardRenderer(context);
		this.mWebViewCardRenderer = new WebViewCardRenderer(context, convMessages,this);

	}

	public void setChatTheme(ChatTheme theme)
	{
		if (theme == null)
		{
			Logger.d("MessageAdapter", "ChatTheme is null in setChatTheme Method");
			return;
		}
		chatTheme = theme;
		isDefaultTheme = chatTheme == ChatTheme.DEFAULT;
		notifyDataSetChanged();
	}

	public boolean isDefaultTheme()
	{
		return isDefaultTheme;
	}

	public void addMessage(ConvMessage convMessage)
	{
		convMessages.add(convMessage);
		if (convMessage != null && convMessage.isSent())
		{
			lastSentMessagePosition = convMessages.size() - 1;
		}
		if (convMessage.getMetadata() != null && convMessage.getMetadata().isPokeMessage())
		{
			convMessage.getMetadata().setNudgeAnimationType(NudgeAnimationType.SINGLE);
		}
	}

	public void addMessages(List<ConvMessage> oldConvMessages, int index)
	{
		convMessages.addAll(index, oldConvMessages);

		if (lastSentMessagePosition >= index)
		{
			lastSentMessagePosition += oldConvMessages.size();
		}
	}

	public void removeMessage(ConvMessage convMessage)
	{
		/*
		 * Iterating in reverse order since its more likely the user wants to delete one of his/her latest messages.
		 */
		int index = convMessages.lastIndexOf(convMessage);
		convMessages.remove(index);
		/*
		 * We need to update the last sent position
		 */
		if (index == lastSentMessagePosition)
		{
			setLastSentMessagePosition();
		}
		else if (index < lastSentMessagePosition)
		{
			lastSentMessagePosition--;
		}
	}

	public void removeMessage(int index)
	{
		convMessages.remove(index);
		/*
		 * We need to update the last sent position
		 */
		if (index == lastSentMessagePosition)
		{
			setLastSentMessagePosition();
		}
		else if (index < lastSentMessagePosition)
		{
			lastSentMessagePosition--;
		}
	}

	private void setLastSentMessagePosition()
	{
		AsyncTask<Void, Void, Void> getLastSentMessagePositionTask = new AsyncTask<Void, Void, Void>()
		{

			@Override
			protected Void doInBackground(Void... params)
			{
				lastSentMessagePosition = -1;
				for (int i = convMessages.size() - 1; i >= 0; i--)
				{
					ConvMessage convMessage = convMessages.get(i);
					if (convMessage == null)
					{
						continue;
					}
					if (convMessage.isSent())
					{
						lastSentMessagePosition = i;
						break;
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result)
			{
				Logger.d(getClass().getSimpleName(), "Last Postion: " + lastSentMessagePosition);
				if (lastSentMessagePosition == -1)
				{
					return;
				}
				notifyDataSetChanged();
			}
		};
		Utils.executeAsyncTask(getLastSentMessagePositionTask);
	}

	/**
	 * Returns what type of View this item is going to result in * @return an integer
	 */
	@Override
	public int getItemViewType(int position)
	{
		ConvMessage convMessage = getItem(position);
		ViewType type;
		
		MessageMetadata metadata = convMessage.getMetadata();
		if (convMessage.isStickerMessage())
		{
			if (convMessage.isSent())
			{
				type = ViewType.STICKER_SENT;
			}
			else
			{
				type = ViewType.STICKER_RECEIVE;
			}
		}
		else if (metadata != null && metadata.isPokeMessage())
		{
			if (convMessage.isSent())
			{
				type = ViewType.NUDGE_SENT;
			}
			else
			{
				type = ViewType.NUDGE_RECEIVE;
			}
		}
		else if (convMessage.isFileTransferMessage())
		{
			final HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			HikeFileType hikeFileType = hikeFile.getHikeFileType();

			if (convMessage.isSent())
			{
				type = ViewType.FILE_SENT;
			}
			else
			{
				type = ViewType.FILE_RECEIVE;
			}

			if (hikeFileType == HikeFileType.AUDIO_RECORDING)
			{
				if (convMessage.isSent())
				{
					type = ViewType.WALKIE_TALKIE_SENT;
				}
				else
				{
					type = ViewType.WALKIE_TALKIE_RECEIVE;
				}
			}
			else if (hikeFileType == HikeFileType.VIDEO)
			{
				if (hikeFile.getThumbnail() != null || HikeMessengerApp.getLruCache().getFileIconFromCache(hikeFile.getFileKey()) != null)
				{
					if (convMessage.isSent())
					{
						type = ViewType.VIDEO_SENT;
					}
					else
					{
						type = ViewType.VIDEO_RECEIVE;
					}
				}
			}
			else if (hikeFileType == HikeFileType.IMAGE)
			{
				if (hikeFile.getThumbnail() != null || HikeMessengerApp.getLruCache().getFileIconFromCache(hikeFile.getFileKey()) != null)
				{
					if (convMessage.isSent())
					{
						type = ViewType.IMAGE_SENT;
					}
					else
					{
						type = ViewType.IMAGE_RECEIVE;
					}
				}
			}
			else if (hikeFileType == HikeFileType.LOCATION)
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
			else if (hikeFileType == HikeFileType.CONTACT)
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
			return viewTypeCount + mChatThreadCardRenderer.getItemViewType(convMessage);
		}
		else if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.WEB_CONTENT || convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT)
		{
			return viewTypeCount + mChatThreadCardRenderer.getCardCount() + mWebViewCardRenderer.getItemViewType(position);
		}
		else if (convMessage.getUnreadCount() > 0)
		{
			type = ViewType.UNREAD_COUNT;
		}
		else if (convMessage.isBlockAddHeader())
		{
			Logger.i("chatthread", "getview type unknown header");
			type = ViewType.UNKNOWN_BLOCK_ADD;
		}
		else if (convMessage.getTypingNotification() != null)
		{
			type = ViewType.TYPING_NOTIFICATION;
		}
		else if (convMessage.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE)
		{
			type = ViewType.STATUS_MESSAGE;
		}
		else if (convMessage.getParticipantInfoState() == ParticipantInfoState.VOIP_CALL_SUMMARY ||
				convMessage.getParticipantInfoState() == ParticipantInfoState.VOIP_MISSED_CALL_INCOMING ||
				convMessage.getParticipantInfoState() == ParticipantInfoState.VOIP_MISSED_CALL_OUTGOING)
		{
			type = ViewType.VOIP_CALL;
		}
		else if (convMessage.getParticipantInfoState() != ParticipantInfoState.NO_INFO)
		{
			type = ViewType.PARTICIPANT_INFO;
		}
		else if (convMessage.isSent())
		{
			if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
			{
				type = ViewType.PIN_TEXT_SENT;
			}
			else
			{
				type = conversation.isOnhike() ? ViewType.SEND_HIKE : ViewType.SEND_SMS;
			}
		}
		else
		{
			if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
			{
				type = ViewType.PIN_TEXT_RECEIVE;
			}
			else
			{
				type = ViewType.RECEIVE;
			}
		}

		return type.ordinal();
	}

	/**
	 * Returns how many distinct types of views this adapter creates. This is used to reuse the view (via convertView in getView)
	 *
	 * @return how many distinct views this adapter will create
	 */
	@Override
	public int getViewTypeCount()
	{
		return viewTypeCount + mChatThreadCardRenderer.getCardCount() + mWebViewCardRenderer.getViewTypeCount();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		long startTime = System.currentTimeMillis();
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		int type = getItemViewType(position);
		ViewType viewType = null;

		final ConvMessage convMessage = getItem(position);

		DayHolder dayHolder = null;
		View v = convertView;
		// Applicable to all kinds of messages
		if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.CONTENT)
		{
			v = mChatThreadCardRenderer.getView(v, convMessage, parent);
			DetailViewHolder holder = (DetailViewHolder) v.getTag();
			dayHolder = holder;
			setSenderDetails(convMessage, position, holder, false);
			setTimeNStatus(position, holder, true, holder.messageContainer);
			setSelection(convMessage, holder.selectedStateOverlay);
		}
		else if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.WEB_CONTENT) {
			//Calling the getView of another adapter to show html cards.
			v = mWebViewCardRenderer.getView(position, convertView, parent);
			WebViewCardRenderer.WebViewHolder holder = (WebViewCardRenderer.WebViewHolder) v.getTag();
			dayHolder = holder;
			setSelection(convMessage, holder.selectedStateOverlay);
		}
		else if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT) {
			//Calling the getView of another adapter to show html cards.
			v = mWebViewCardRenderer.getView(position, convertView, parent);
			WebViewCardRenderer.WebViewHolder holder = (WebViewCardRenderer.WebViewHolder) v.getTag();
			dayHolder = holder;
			setSelection(convMessage, holder.selectedStateOverlay);
			setSenderDetails(convMessage, position, holder, false);
			setTimeNStatus(position, holder, true, holder.messageContainer);
		}
		else
			viewType = ViewType.values()[type];

		if (viewType == ViewType.TYPING_NOTIFICATION)
		{

			TypingViewHolder typingHolder = null;
			if (v == null)
			{
				typingHolder = new TypingViewHolder();
				v = inflater.inflate(R.layout.typing_notification_receive, parent, false);
				typingHolder.typing = (ImageView) v.findViewById(R.id.typing);
				typingHolder.typingAvatarContainer = (ViewGroup) v.findViewById(R.id.typing_avatar_container);
				v.setTag(typingHolder);
			}
			else
			{
				typingHolder = (TypingViewHolder) v.getTag();
			}

			typingHolder.typing.setVisibility(View.VISIBLE);

			AnimationDrawable ad = (AnimationDrawable) typingHolder.typing.getDrawable();
			ad.setCallback(typingHolder.typing);
			ad.setVisible(true, true);
			ad.start();

			if (isGroupChat)
			{
				typingHolder.typingAvatarContainer.setVisibility(View.VISIBLE);

				GroupTypingNotification groupTypingNotification = (GroupTypingNotification) convMessage.getTypingNotification();
				List<String> participantList = groupTypingNotification.getGroupParticipantList();

				typingHolder.typingAvatarContainer.removeAllViews();

				for (int i = participantList.size() - 1; i >= 0; i--)
				{
					String msisdn = participantList.get(i);

					View avatarContainer = inflater.inflate(R.layout.small_avatar_container, typingHolder.typingAvatarContainer, false);
					ImageView imageView = (ImageView) avatarContainer.findViewById(R.id.avatar);
					/*
					 * Catching OOB here since the participant list can be altered by another thread. In that case an OOB will be thrown here. The only impact that will have is
					 * that the image which has been removed will be skipped.
					 */
					try
					{
						setAvatar(msisdn, imageView);

						typingHolder.typingAvatarContainer.addView(avatarContainer);
					}
					catch (IndexOutOfBoundsException e)
					{

					}
				}
			}
			return v;
		}

		MessageMetadata metadata = convMessage.getMetadata();

		// //////////////////////////////////////////////////////////////////////////
		// Categorical Applications

		if (viewType == ViewType.STICKER_SENT || viewType == ViewType.STICKER_RECEIVE)
		{
			StickerViewHolder stickerHolder = null;
			if (viewType == ViewType.STICKER_SENT)
			{
				if (v == null)
				{
					stickerHolder = new StickerViewHolder();
					v = inflateView(R.layout.message_sent_sticker, parent, false);

					stickerHolder.placeHolder = v.findViewById(R.id.placeholder);
					stickerHolder.loader = (ProgressBar) v.findViewById(R.id.loading_progress);
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
			}
			else if (viewType == ViewType.STICKER_RECEIVE)
			{
				if (v == null)
				{
					stickerHolder = new StickerViewHolder();
					v = inflateView(R.layout.message_receive_sticker, parent, false);

					stickerHolder.placeHolder = v.findViewById(R.id.placeholder);
					stickerHolder.loader = (ProgressBar) v.findViewById(R.id.loading_progress);
					stickerHolder.image = (ImageView) v.findViewById(R.id.image);
					stickerHolder.broadcastIndicator = (ImageView) v.findViewById(R.id.broadcastIndicator);
					stickerHolder.time = (TextView) v.findViewById(R.id.time);
					stickerHolder.status = (ImageView) v.findViewById(R.id.status);
					stickerHolder.timeStatus = (View) v.findViewById(R.id.time_status);
					stickerHolder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
					stickerHolder.senderDetails = v.findViewById(R.id.sender_details);
					stickerHolder.senderName = (TextView) v.findViewById(R.id.sender_name);
					stickerHolder.senderNameUnsaved = (TextView) v.findViewById(R.id.sender_unsaved_name);
					stickerHolder.avatarImage = (ImageView) v.findViewById(R.id.avatar);
					stickerHolder.avatarContainer = (ViewGroup) v.findViewById(R.id.avatar_container);
					stickerHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
					v.setTag(stickerHolder);
				}
				else
				{
					stickerHolder = (StickerViewHolder) v.getTag();
				}
			}
			dayHolder = stickerHolder;
			stickerHolder.placeHolder.setBackgroundResource(0);
			stickerHolder.loader.setVisibility(View.GONE);
			Sticker sticker = metadata.getSticker();
			setSenderDetails(convMessage, position, stickerHolder, true);
			String categoryId = sticker.getCategoryId();
			String stickerId = sticker.getStickerId();
			String categoryDirPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(categoryId) + HikeConstants.LARGE_STICKER_ROOT;
			File stickerImage = null;
			if (categoryDirPath != null)
			{
				stickerImage = new File(categoryDirPath, stickerId);
			}

			if (stickerImage != null && stickerImage.exists())
			{
				Drawable stickerDrawable = HikeMessengerApp.getLruCache().getSticker(stickerImage.getPath());
				if (stickerDrawable != null)
				{
					stickerHolder.placeHolder.setBackgroundResource(0);
					stickerHolder.image.setVisibility(View.VISIBLE);
					// largeStickerLoader.loadImage(stickerImage.getPath(), holder.stickerImage, isListFlinging);
					stickerHolder.image.setImageDrawable(stickerDrawable);
					// holder.stickerImage.setImageDrawable(IconCacheManager
					// .getInstance().getSticker(context,
					// stickerImage.getPath()));
				}
				else
				{
					stickerHolder.loader.setVisibility(View.VISIBLE);
					stickerHolder.placeHolder.setBackgroundResource(R.drawable.bg_sticker_placeholder);
					stickerHolder.image.setVisibility(View.GONE);
					stickerHolder.image.setImageDrawable(null);
				}
			}
			else
			{
				stickerHolder.loader.setVisibility(View.VISIBLE);
				stickerHolder.placeHolder.setBackgroundResource(R.drawable.bg_sticker_placeholder);
				stickerHolder.image.setVisibility(View.GONE);
				stickerHolder.image.setImageDrawable(null);

				StickerDownloadManager.getInstance().DownloadSingleSticker(categoryId, stickerId, new IStickerResultListener()
				{

					@Override
					public void onSuccess(Object result)
					{
						// Here we update sticker category, if we received a different category in download response.
						// This is being done to fix a legacy bug, where catId came as "unknown"
						
						String newCategoryId = (String) result;
						String oldCategoryId = convMessage.getMetadata().getSticker().getStickerId();
						if (!oldCategoryId.equals(newCategoryId))
						{
							try
							{
								MessageMetadata newMetadata = convMessage.getMetadata();
								newMetadata.updateSticker(newCategoryId);
								HikeConversationsDatabase.getInstance().updateMessageMetadata(convMessage.getMsgID(), newMetadata);
							}
							catch (JSONException e)
							{
								Logger.wtf("MessagesAdapter", "Got new categoryId as " + result.toString() + " But failed to update the metadata for : " + convMessage.getMsgID());
							}

						}
						HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_DOWNLOADED, null);

					}

					@Override
					public void onProgressUpdated(double percentage)
					{
						// TODO Auto-generated method stub

					}

					@Override
					public void onFailure(Object result, StickerException exception)
					{
						if (result == null)
						{
							return;
						}
						String largeStickerPath = (String) result;
						(new File(largeStickerPath)).delete();
					}
				});

			}
			displayBroadcastIndicator(convMessage, stickerHolder.broadcastIndicator, false);
			setTimeNStatus(position, stickerHolder, true, stickerHolder.placeHolder);
			setSelection(convMessage, stickerHolder.selectedStateOverlay);
		}
		else if (viewType == ViewType.NUDGE_SENT || viewType == ViewType.NUDGE_RECEIVE)
		{
			NudgeViewHolder nudgeHolder = null;
			if (viewType == ViewType.NUDGE_SENT)
			{
				if (v == null)
				{
					nudgeHolder = new NudgeViewHolder();
					v = inflateView(R.layout.message_sent_nudge, parent, false);
					nudgeHolder.nudge = (ImageView) v.findViewById(R.id.nudge);
					nudgeHolder.time = (TextView) v.findViewById(R.id.time);
					nudgeHolder.status = (ImageView) v.findViewById(R.id.status);
					nudgeHolder.timeStatus = (View) v.findViewById(R.id.time_status);
					nudgeHolder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
					nudgeHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
					nudgeHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
					v.setTag(nudgeHolder);
				}
				else
				{
					nudgeHolder = (NudgeViewHolder) v.getTag();
				}
			}
			else if (viewType == ViewType.NUDGE_RECEIVE)
			{
				if (v == null)
				{
					nudgeHolder = new NudgeViewHolder();
					v = inflateView(R.layout.message_receive_nudge, parent, false);

					nudgeHolder.nudge = (ImageView) v.findViewById(R.id.nudge);
					nudgeHolder.time = (TextView) v.findViewById(R.id.time);
					nudgeHolder.status = (ImageView) v.findViewById(R.id.status);
					nudgeHolder.timeStatus = (View) v.findViewById(R.id.time_status);
					nudgeHolder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
					nudgeHolder.senderDetails = v.findViewById(R.id.sender_details);
					nudgeHolder.senderName = (TextView) v.findViewById(R.id.sender_name);
					nudgeHolder.senderNameUnsaved = (TextView) v.findViewById(R.id.sender_unsaved_name);
					nudgeHolder.avatarImage = (ImageView) v.findViewById(R.id.avatar);
					nudgeHolder.avatarContainer = (ViewGroup) v.findViewById(R.id.avatar_container);
					nudgeHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
					v.setTag(nudgeHolder);
				}
				else
				{
					nudgeHolder = (NudgeViewHolder) v.getTag();
				}
			}
			dayHolder = nudgeHolder;
			setSenderDetails(convMessage, position, nudgeHolder, true);
			if (!chatTheme.isAnimated())
			{
				nudgeHolder.nudge.setVisibility(View.VISIBLE);
				setNudgeImageResource(chatTheme, nudgeHolder.nudge, convMessage.isSent());
			}
			else
			{
				nudgeHolder.nudge.setVisibility(View.VISIBLE);

				setNudgeImageResource(chatTheme, nudgeHolder.nudge, convMessage.isSent());
				if (metadata.getNudgeAnimationType() != NudgeAnimationType.NONE)
				{
					metadata.setNudgeAnimationType(NudgeAnimationType.NONE);
					nudgeHolder.nudge.startAnimation(AnimationUtils.loadAnimation(context, R.anim.valetines_nudge_anim));
				}
			}
			setTimeNStatus(position, nudgeHolder, true, nudgeHolder.nudge);
			setSelection(convMessage, nudgeHolder.selectedStateOverlay);
		}
		else if (convMessage.isFileTransferMessage())
		{
			FileSavedState fss = null;
			final HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			File file = hikeFile.getFile();
			if (convMessage.isSent())
			{
				fss = FileTransferManager.getInstance(context).getUploadFileState(convMessage.getMsgID(), file);
			}
			else
			{
				fss = FileTransferManager.getInstance(context).getDownloadFileState(convMessage.getMsgID(), file);
			}
			boolean showThumbnail = false;
			Drawable thumbnail = null;

			if (viewType == ViewType.WALKIE_TALKIE_SENT || viewType == ViewType.WALKIE_TALKIE_RECEIVE)
			{
				WalkieTalkieViewHolder wtHolder = null;
				if (viewType == ViewType.WALKIE_TALKIE_SENT)
				{
					if (v == null)
					{
						wtHolder = new WalkieTalkieViewHolder();
						v = inflateView(R.layout.message_sent_walkie_talkie, parent, false);
						wtHolder.placeHolder = v.findViewById(R.id.placeholder);
						wtHolder.initialization = (ProgressBar) v.findViewById(R.id.initializing);
						wtHolder.progress = (HoloCircularProgress) v.findViewById(R.id.play_progress);
						wtHolder.action = (ImageView) v.findViewById(R.id.action);
						wtHolder.duration = (TextView) v.findViewById(R.id.duration);
						wtHolder.broadcastIndicator = (ImageView) v.findViewById(R.id.broadcastIndicator);
						wtHolder.time = (TextView) v.findViewById(R.id.time);
						wtHolder.status = (ImageView) v.findViewById(R.id.status);
						wtHolder.timeStatus = (View) v.findViewById(R.id.time_status);
						wtHolder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
						wtHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
						wtHolder.messageInfoStub = (ViewStub) v.findViewById(R.id.message_info_stub);
						v.setTag(wtHolder);
					}
					else
					{
						wtHolder = (WalkieTalkieViewHolder) v.getTag();
					}
				}
				else if (viewType == ViewType.WALKIE_TALKIE_RECEIVE)
				{
					if (v == null)
					{
						wtHolder = new WalkieTalkieViewHolder();
						v = inflateView(R.layout.message_receive_walkie_talkie, parent, false);

						wtHolder.placeHolder = v.findViewById(R.id.placeholder);
						wtHolder.initialization = (ProgressBar) v.findViewById(R.id.initializing);
						wtHolder.progress = (HoloCircularProgress) v.findViewById(R.id.play_progress);
						wtHolder.action = (ImageView) v.findViewById(R.id.action);
						wtHolder.duration = (TextView) v.findViewById(R.id.duration);
						wtHolder.broadcastIndicator = (ImageView) v.findViewById(R.id.broadcastIndicator);
						wtHolder.time = (TextView) v.findViewById(R.id.time);
						wtHolder.status = (ImageView) v.findViewById(R.id.status);
						wtHolder.timeStatus = (View) v.findViewById(R.id.time_status);
						wtHolder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
						wtHolder.senderDetails = v.findViewById(R.id.sender_details);
						wtHolder.senderName = (TextView) v.findViewById(R.id.sender_name);
						wtHolder.senderNameUnsaved = (TextView) v.findViewById(R.id.sender_unsaved_name);
						wtHolder.avatarImage = (ImageView) v.findViewById(R.id.avatar);
						wtHolder.avatarContainer = (ViewGroup) v.findViewById(R.id.avatar_container);
						wtHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
						v.setTag(wtHolder);
					}
					else
					{
						wtHolder = (WalkieTalkieViewHolder) v.getTag();
					}
				}
				dayHolder = wtHolder;
				setSenderDetails(convMessage, position, wtHolder, true);

				ShapeDrawable circle = new ShapeDrawable(new OvalShape());
				circle.setIntrinsicHeight((int) (36 * Utils.scaledDensityMultiplier));
				circle.setIntrinsicWidth((int) (36 * Utils.scaledDensityMultiplier));
				if (convMessage.isSent())
				{
					/* label outgoing hike conversations in green */
					if (chatTheme == ChatTheme.DEFAULT)
					{
						circle.getPaint().setColor(context.getResources().getColor(!convMessage.isSMS() ? R.color.bubble_blue : R.color.bubble_green));
					}
					else
					{
						circle.getPaint().setColor(context.getResources().getColor(chatTheme.bubbleColor()));
					}

				}
				else
				{
					circle.getPaint().setColor(context.getResources().getColor(R.color.bubble_white));
				}
				wtHolder.placeHolder.setBackgroundDrawable(circle);

				if (fss.getFTState() == FTState.COMPLETED || (convMessage.isSent() && !TextUtils.isEmpty(hikeFile.getFileKey())))
				{
					wtHolder.action.setBackgroundResource(0);
					wtHolder.action.setImageResource(0);
					wtHolder.action.setScaleType(ScaleType.CENTER_INSIDE);
					if (hikeFile.getFileKey().equals(voiceMessagePlayer.getFileKey()))
					{
						if (voiceMessagePlayer.getPlayerState() == VoiceMessagePlayerState.PLAYING)
						{
							wtHolder.action.setImageResource(R.drawable.ic_pause_rec);
						}
						else
						{
							wtHolder.action.setImageResource(R.drawable.ic_mic);
						}
						wtHolder.duration.setTag(hikeFile.getFileKey());
						voiceMessagePlayer.setDurationTxt(wtHolder.duration, wtHolder.progress);
						wtHolder.duration.setVisibility(View.VISIBLE);
						wtHolder.progress.setVisibility(View.VISIBLE);
					}
					else
					{
						wtHolder.action.setImageResource(R.drawable.ic_mic);
						Utils.setupFormattedTime(wtHolder.duration, hikeFile.getRecordingDuration());
						wtHolder.duration.setVisibility(View.VISIBLE);
						wtHolder.progress.setVisibility(View.INVISIBLE);
					}
				}
				else
				{
					wtHolder.duration.setVisibility(View.GONE);
					wtHolder.action.setImageResource(R.drawable.ic_mic);
				}

				switch (fss.getFTState())
				{
				case INITIALIZED:
				case IN_PROGRESS:
					wtHolder.initialization.setVisibility(View.VISIBLE);
					break;
				case COMPLETED:
				case NOT_STARTED:
				case CANCELLED:
				case PAUSED:
				case ERROR:
				default:
					wtHolder.initialization.setVisibility(View.GONE);
				}

				displayBroadcastIndicator(convMessage, wtHolder.broadcastIndicator, true);
				setTimeNStatus(position, wtHolder, true, wtHolder.placeHolder);
				setSelection(convMessage, wtHolder.selectedStateOverlay);

				wtHolder.placeHolder.setTag(convMessage);
				wtHolder.placeHolder.setOnClickListener(this);
				wtHolder.placeHolder.setOnLongClickListener(this);
			}
			else if (viewType == ViewType.VIDEO_SENT || viewType == ViewType.VIDEO_RECEIVE)
			{
				VideoViewHolder videoHolder = null;
				if (viewType == ViewType.VIDEO_SENT)
				{
					if ((v != null) && (v.getTag() instanceof VideoViewHolder))
					{
						videoHolder = (VideoViewHolder) v.getTag();
					}
					else
					{
						videoHolder = new VideoViewHolder();
						v = inflateView(R.layout.message_sent_video, parent, false);
						videoHolder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
						videoHolder.circularProgressBg = v.findViewById(R.id.circular_bg);
						videoHolder.initializing = (ProgressBar) v.findViewById(R.id.initializing);
						videoHolder.circularProgress = (HoloCircularProgress) v.findViewById(R.id.progress);
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
					}
				}
				else if (viewType == ViewType.VIDEO_RECEIVE)
				{
					if ((v != null) && (v.getTag() instanceof VideoViewHolder))
					{
						videoHolder = (VideoViewHolder) v.getTag();
					}
					else
					{
						videoHolder = new VideoViewHolder();
						v = inflateView(R.layout.message_receive_video, parent, false);

						videoHolder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
						videoHolder.circularProgressBg = v.findViewById(R.id.circular_bg);
						videoHolder.initializing = (ProgressBar) v.findViewById(R.id.initializing);
						videoHolder.circularProgress = (HoloCircularProgress) v.findViewById(R.id.progress);
						videoHolder.ftAction = (ImageView) v.findViewById(R.id.action);
						videoHolder.fileDetails = v.findViewById(R.id.file_details);
						videoHolder.fileSize = (TextView) v.findViewById(R.id.file_size);
						videoHolder.fileName = (TextView) v.findViewById(R.id.file_name);
						videoHolder.filmstripLeft = (ImageView) v.findViewById(R.id.filmstrip_left);
						videoHolder.filmstripRight = (ImageView) v.findViewById(R.id.filmstrip_right);
						videoHolder.time = (TextView) v.findViewById(R.id.time);
						videoHolder.broadcastIndicator = (ImageView) v.findViewById(R.id.broadcastIndicator);
						videoHolder.status = (ImageView) v.findViewById(R.id.status);
						videoHolder.timeStatus = (View) v.findViewById(R.id.time_status);
						videoHolder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
						videoHolder.senderDetails = v.findViewById(R.id.sender_details);
						videoHolder.senderName = (TextView) v.findViewById(R.id.sender_name);
						videoHolder.senderNameUnsaved = (TextView) v.findViewById(R.id.sender_unsaved_name);
						videoHolder.avatarImage = (ImageView) v.findViewById(R.id.avatar);
						videoHolder.avatarContainer = (ViewGroup) v.findViewById(R.id.avatar_container);
						videoHolder.messageContainer = (ViewGroup) v.findViewById(R.id.message_container);
						videoHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
						v.setTag(videoHolder);
					}
				}
				dayHolder = videoHolder;
				setSenderDetails(convMessage, position, videoHolder, false);

				videoHolder.fileThumb.setBackgroundResource(0);
				videoHolder.fileThumb.setImageResource(0);

				showThumbnail = ((convMessage.isSent()) || (conversation instanceof GroupConversation) || (!TextUtils.isEmpty(conversation.getContactName())) || (hikeFile
						.wasFileDownloaded()));
				if (hikeFile.getThumbnail() == null && !TextUtils.isEmpty(hikeFile.getFileKey()))
				{
					thumbnail = HikeMessengerApp.getLruCache().getFileIconFromCache(hikeFile.getFileKey());
				}
				else
				{
					thumbnail = hikeFile.getThumbnail();
				}

				if (showThumbnail)
				{
					videoHolder.fileThumb.setBackgroundDrawable(thumbnail);
				}
				else
				{
					createMediaThumb(videoHolder.fileThumb);
				}

				RelativeLayout.LayoutParams fileThumbParams = (RelativeLayout.LayoutParams) videoHolder.fileThumb.getLayoutParams();

				if (showThumbnail && thumbnail != null)
				{
					videoHolder.fileThumb.setScaleType(ScaleType.CENTER);
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
				videoHolder.fileThumb.setScaleType(ScaleType.CENTER);
				videoHolder.fileThumb.setLayoutParams(fileThumbParams);

				if (convMessage.isSent() && ((int) hikeFile.getFile().length() > 0) && fss.getFTState() != FTState.INITIALIZED)
				{
					videoHolder.fileSize.setText(Utils.getSizeForDisplay((int) hikeFile.getFile().length()));
					videoHolder.fileSize.setVisibility(View.VISIBLE);
				}
				else if (!convMessage.isSent() && hikeFile.getFileSize() > 0)
				{
					videoHolder.fileSize.setText(Utils.getSizeForDisplay(hikeFile.getFileSize()));
					videoHolder.fileSize.setVisibility(View.VISIBLE);
				}
				else
				{
					videoHolder.fileSize.setText("");
					videoHolder.fileSize.setVisibility(View.GONE);
				}
				videoHolder.fileThumb.setVisibility(View.VISIBLE);
				videoHolder.filmstripLeft.setVisibility(View.VISIBLE);
				videoHolder.filmstripRight.setVisibility(View.VISIBLE);

				displayBroadcastIndicator(convMessage, videoHolder.broadcastIndicator, false);
				setBubbleColor(convMessage, videoHolder.messageContainer);
				setupFileState(videoHolder, fss, convMessage.getMsgID(), hikeFile, convMessage.isSent(), false);
				setTimeNStatus(position, videoHolder, true, videoHolder.fileThumb);
				setSelection(convMessage, videoHolder.selectedStateOverlay);

				videoHolder.fileThumb.setTag(convMessage);
				videoHolder.fileThumb.setOnClickListener(this);
				videoHolder.fileThumb.setOnLongClickListener(this);
			}
			else if (viewType == ViewType.IMAGE_SENT || viewType == ViewType.IMAGE_RECEIVE)
			{
				ImageViewHolder imageHolder = null;
				if (viewType == ViewType.IMAGE_SENT)
				{
					if ((v != null) && (v.getTag() instanceof ImageViewHolder))
					{
						imageHolder = (ImageViewHolder) v.getTag();
					}
					else
					{
						imageHolder = new ImageViewHolder();
						v = inflateView(R.layout.message_sent_image, parent, false);
						imageHolder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
						imageHolder.circularProgressBg = v.findViewById(R.id.circular_bg);
						imageHolder.initializing = (ProgressBar) v.findViewById(R.id.initializing);
						imageHolder.circularProgress = (HoloCircularProgress) v.findViewById(R.id.progress);
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
					}
				}
				else if (viewType == ViewType.IMAGE_RECEIVE)
				{
					if ((v != null) && (v.getTag() instanceof ImageViewHolder))
					{
						imageHolder = (ImageViewHolder) v.getTag();
					}
					else
					{
						imageHolder = new ImageViewHolder();
						v = inflateView(R.layout.message_receive_image, parent, false);

						imageHolder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
						imageHolder.circularProgressBg = v.findViewById(R.id.circular_bg);
						imageHolder.initializing = (ProgressBar) v.findViewById(R.id.initializing);
						imageHolder.circularProgress = (HoloCircularProgress) v.findViewById(R.id.progress);
						imageHolder.ftAction = (ImageView) v.findViewById(R.id.action);
						imageHolder.fileDetails = v.findViewById(R.id.file_details);
						imageHolder.fileSize = (TextView) v.findViewById(R.id.file_size);
						imageHolder.fileName = (TextView) v.findViewById(R.id.file_name);
						imageHolder.broadcastIndicator = (ImageView) v.findViewById(R.id.broadcastIndicator);
						imageHolder.time = (TextView) v.findViewById(R.id.time);
						imageHolder.status = (ImageView) v.findViewById(R.id.status);
						imageHolder.timeStatus = (View) v.findViewById(R.id.time_status);
						imageHolder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
						imageHolder.senderDetails = v.findViewById(R.id.sender_details);
						imageHolder.senderName = (TextView) v.findViewById(R.id.sender_name);
						imageHolder.senderNameUnsaved = (TextView) v.findViewById(R.id.sender_unsaved_name);
						imageHolder.avatarImage = (ImageView) v.findViewById(R.id.avatar);
						imageHolder.avatarContainer = (ViewGroup) v.findViewById(R.id.avatar_container);
						imageHolder.messageContainer = (ViewGroup) v.findViewById(R.id.message_container);
						imageHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
						v.setTag(imageHolder);
					}
				}
				dayHolder = imageHolder;
				setSenderDetails(convMessage, position, imageHolder, false);

				imageHolder.fileThumb.setBackgroundResource(0);
				imageHolder.fileThumb.setImageResource(0);

				showThumbnail = ((convMessage.isSent()) || (conversation instanceof GroupConversation) || (!TextUtils.isEmpty(conversation.getContactName())) || (hikeFile
						.wasFileDownloaded()));

				if (hikeFile.getThumbnail() == null && !TextUtils.isEmpty(hikeFile.getFileKey()))
				{
					thumbnail = HikeMessengerApp.getLruCache().getFileIconFromCache(hikeFile.getFileKey());
				}
				else
				{
					thumbnail = hikeFile.getThumbnail();
				}

				if (showThumbnail)
				{
					imageHolder.fileThumb.setImageDrawable(thumbnail);
					hqThumbLoader.setLoadingImage(thumbnail);
					hqThumbLoader.loadImage(hikeFile.getFilePath(), imageHolder.fileThumb, isListFlinging);
				}
				else
				{
					createMediaThumb(imageHolder.fileThumb);
				}

				RelativeLayout.LayoutParams fileThumbParams = (RelativeLayout.LayoutParams) imageHolder.fileThumb.getLayoutParams();

				if (showThumbnail && thumbnail != null)
				{
					imageHolder.fileThumb.setScaleType(ScaleType.CENTER_CROP);
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
				imageHolder.fileThumb.setScaleType(ScaleType.CENTER_CROP);
				imageHolder.fileThumb.setLayoutParams(fileThumbParams);

				imageHolder.fileThumb.setVisibility(View.VISIBLE);

				displayBroadcastIndicator(convMessage, imageHolder.broadcastIndicator, false);
				setBubbleColor(convMessage, imageHolder.messageContainer);
				setupFileState(imageHolder, fss, convMessage.getMsgID(), hikeFile, convMessage.isSent(), false);
				setTimeNStatus(position, imageHolder, true, imageHolder.fileThumb);
				setSelection(convMessage, imageHolder.selectedStateOverlay);

				imageHolder.fileThumb.setTag(convMessage);
				imageHolder.fileThumb.setOnClickListener(this);
				imageHolder.fileThumb.setOnLongClickListener(this);
			}
			else if (viewType == ViewType.LOCATION_SENT || viewType == ViewType.LOCATION_RECEIVE)
			{
				ImageViewHolder imageHolder = null;
				if (viewType == ViewType.LOCATION_SENT)
				{
					if (v == null)
					{
						imageHolder = new ImageViewHolder();
						v = inflateView(R.layout.message_sent_image, parent, false);
						imageHolder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
						imageHolder.circularProgressBg = v.findViewById(R.id.circular_bg);
						imageHolder.initializing = (ProgressBar) v.findViewById(R.id.initializing);
						imageHolder.circularProgress = (HoloCircularProgress) v.findViewById(R.id.progress);
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
					}
					else
					{
						imageHolder = (ImageViewHolder) v.getTag();
					}
				}
				else if (viewType == ViewType.LOCATION_RECEIVE)
				{
					if (v == null)
					{
						imageHolder = new ImageViewHolder();
						v = inflateView(R.layout.message_receive_image, parent, false);

						imageHolder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
						imageHolder.circularProgressBg = v.findViewById(R.id.circular_bg);
						imageHolder.initializing = (ProgressBar) v.findViewById(R.id.initializing);
						imageHolder.circularProgress = (HoloCircularProgress) v.findViewById(R.id.progress);
						imageHolder.ftAction = (ImageView) v.findViewById(R.id.action);
						imageHolder.fileDetails = v.findViewById(R.id.file_details);
						imageHolder.fileSize = (TextView) v.findViewById(R.id.file_size);
						imageHolder.fileName = (TextView) v.findViewById(R.id.file_name);
						imageHolder.broadcastIndicator = (ImageView) v.findViewById(R.id.broadcastIndicator);
						imageHolder.time = (TextView) v.findViewById(R.id.time);
						imageHolder.status = (ImageView) v.findViewById(R.id.status);
						imageHolder.timeStatus = (View) v.findViewById(R.id.time_status);
						imageHolder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
						imageHolder.senderDetails = v.findViewById(R.id.sender_details);
						imageHolder.senderName = (TextView) v.findViewById(R.id.sender_name);
						imageHolder.senderNameUnsaved = (TextView) v.findViewById(R.id.sender_unsaved_name);
						imageHolder.avatarImage = (ImageView) v.findViewById(R.id.avatar);
						imageHolder.avatarContainer = (ViewGroup) v.findViewById(R.id.avatar_container);
						imageHolder.messageContainer = (ViewGroup) v.findViewById(R.id.message_container);
						imageHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
						v.setTag(imageHolder);
					}
					else
					{
						imageHolder = (ImageViewHolder) v.getTag();
					}
				}
				dayHolder = imageHolder;
				setSenderDetails(convMessage, position, imageHolder, false);
				imageHolder.fileThumb.setBackgroundResource(0);
				imageHolder.fileThumb.setImageResource(0);

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
					imageHolder.fileThumb.setScaleType(ScaleType.CENTER);
				}
				imageHolder.fileThumb.setVisibility(View.VISIBLE);

				if ((!convMessage.isSent()) || (convMessage.isSent() && !TextUtils.isEmpty(hikeFile.getFileKey())))
				{
					imageHolder.circularProgressBg.setVisibility(View.GONE);
				}
				else if (FileTransferManager.getInstance(context).isFileTaskExist(convMessage.getMsgID()))
				{
					imageHolder.initializing.setVisibility(View.VISIBLE);
					imageHolder.circularProgressBg.setVisibility(View.VISIBLE);
				}
				else
				{
					imageHolder.ftAction.setImageResource(R.drawable.ic_retry_image_video);
					imageHolder.ftAction.setVisibility(View.VISIBLE);
					imageHolder.ftAction.setScaleType(ScaleType.CENTER);
					imageHolder.circularProgressBg.setVisibility(View.VISIBLE);
				}

				displayBroadcastIndicator(convMessage, imageHolder.broadcastIndicator, false);
				setBubbleColor(convMessage, imageHolder.messageContainer);
				setTimeNStatus(position, imageHolder, true, imageHolder.fileThumb);
				setSelection(convMessage, imageHolder.selectedStateOverlay);

				imageHolder.fileThumb.setTag(convMessage);
				imageHolder.fileThumb.setOnClickListener(this);
				imageHolder.fileThumb.setOnLongClickListener(this);
			}
			else if (viewType == ViewType.CONTACT_SENT || viewType == ViewType.CONTACT_RECEIVE)
			{
				FileViewHolder fileHolder = null;
				if (viewType == ViewType.CONTACT_SENT)
				{
					if (v == null)
					{
						fileHolder = new FileViewHolder();
						v = inflateView(R.layout.message_sent_file, parent, false);
						fileHolder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
						fileHolder.circularProgressBg = v.findViewById(R.id.circular_bg);
						fileHolder.initializing = (ProgressBar) v.findViewById(R.id.initializing);
						fileHolder.circularProgress = (HoloCircularProgress) v.findViewById(R.id.progress);
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
					}
					else
					{
						fileHolder = (FileViewHolder) v.getTag();
					}
				}
				else if (viewType == ViewType.CONTACT_RECEIVE)
				{
					if (v == null)
					{
						fileHolder = new FileViewHolder();
						v = inflateView(R.layout.message_receive_file, parent, false);

						fileHolder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
						fileHolder.circularProgressBg = v.findViewById(R.id.circular_bg);
						fileHolder.initializing = (ProgressBar) v.findViewById(R.id.initializing);
						fileHolder.circularProgress = (HoloCircularProgress) v.findViewById(R.id.progress);
						fileHolder.ftAction = (ImageView) v.findViewById(R.id.action);
						fileHolder.fileDetails = v.findViewById(R.id.file_details);
						fileHolder.fileSize = (TextView) v.findViewById(R.id.file_size);
						fileHolder.fileName = (TextView) v.findViewById(R.id.file_name);
						fileHolder.broadcastIndicator = (ImageView) v.findViewById(R.id.broadcastIndicator);
						fileHolder.time = (TextView) v.findViewById(R.id.time);
						fileHolder.status = (ImageView) v.findViewById(R.id.status);
						fileHolder.timeStatus = (View) v.findViewById(R.id.time_status);
						fileHolder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
						fileHolder.senderDetails = v.findViewById(R.id.sender_details);
						fileHolder.senderName = (TextView) v.findViewById(R.id.sender_name);
						fileHolder.senderNameUnsaved = (TextView) v.findViewById(R.id.sender_unsaved_name);
						fileHolder.avatarImage = (ImageView) v.findViewById(R.id.avatar);
						fileHolder.avatarContainer = (ViewGroup) v.findViewById(R.id.avatar_container);
						fileHolder.messageContainer = (ViewGroup) v.findViewById(R.id.message_container);
						fileHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
						v.setTag(fileHolder);
					}
					else
					{
						fileHolder = (FileViewHolder) v.getTag();
					}
				}
				dayHolder = fileHolder;
				setSenderDetails(convMessage, position, fileHolder, false);
				fileHolder.fileThumb.setImageResource(R.drawable.ic_default_contact);
				fileHolder.fileThumb.setScaleType(ScaleType.CENTER);
				fileHolder.fileName.setText(hikeFile.getDisplayName());
				List<ContactInfoData> items = Utils.getContactDataFromHikeFile(hikeFile);
				String phone = null, email = null;
				for (ContactInfoData contactInfoData : items)
				{
					if (contactInfoData.getDataType() == DataType.PHONE_NUMBER)
						phone = contactInfoData.getData();

					else if (contactInfoData.getDataType() == DataType.EMAIL)
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

				if ((!convMessage.isSent()) || (convMessage.isSent() && !TextUtils.isEmpty(hikeFile.getFileKey())))
				{
					fileHolder.circularProgressBg.setVisibility(View.GONE);
				}
				else if (FileTransferManager.getInstance(context).isFileTaskExist(convMessage.getMsgID()))
				{
					fileHolder.initializing.setVisibility(View.VISIBLE);
					fileHolder.circularProgressBg.setVisibility(View.VISIBLE);
				}
				else
				{
					fileHolder.ftAction.setImageResource(R.drawable.ic_retry_other);
					fileHolder.ftAction.setVisibility(View.VISIBLE);
					fileHolder.ftAction.setScaleType(ScaleType.CENTER);
					fileHolder.circularProgressBg.setVisibility(View.VISIBLE);
				}

				displayBroadcastIndicator(convMessage, fileHolder.broadcastIndicator, true);
				setBubbleColor(convMessage, fileHolder.messageContainer);
				setTimeNStatus(position, fileHolder, false, fileHolder.messageContainer);
				setSelection(convMessage, fileHolder.selectedStateOverlay);

				fileHolder.fileThumb.setTag(convMessage);
				fileHolder.fileThumb.setOnClickListener(this);
				fileHolder.fileThumb.setOnLongClickListener(this);
				fileHolder.fileDetails.setTag(convMessage);
				fileHolder.fileDetails.setOnClickListener(this);
				fileHolder.fileDetails.setOnLongClickListener(this);
				fileHolder.circularProgressBg.setTag(convMessage);
				fileHolder.circularProgressBg.setOnClickListener(this);
				fileHolder.circularProgressBg.setOnLongClickListener(this);
			}
			else
			{
				FileViewHolder fileHolder = null;
				if (viewType == ViewType.FILE_SENT)
				{
					if ((v != null) && (v.getTag() instanceof FileViewHolder))
					{
						fileHolder = (FileViewHolder) v.getTag();
					}
					else
					{
						fileHolder = new FileViewHolder();
						v = inflateView(R.layout.message_sent_file, parent, false);
						fileHolder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
						fileHolder.fileExtension = (TextView) v.findViewById(R.id.file_extension);
						fileHolder.circularProgressBg = v.findViewById(R.id.circular_bg);
						fileHolder.initializing = (ProgressBar) v.findViewById(R.id.initializing);
						fileHolder.circularProgress = (HoloCircularProgress) v.findViewById(R.id.progress);
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
					}
				}
				else if (viewType == ViewType.FILE_RECEIVE)
				{
					if ((v != null) && (v.getTag() instanceof FileViewHolder))
					{
						fileHolder = (FileViewHolder) v.getTag();
					}
					else
					{
						fileHolder = new FileViewHolder();
						v = inflateView(R.layout.message_receive_file, parent, false);

						fileHolder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
						fileHolder.fileExtension = (TextView) v.findViewById(R.id.file_extension);
						fileHolder.circularProgressBg = v.findViewById(R.id.circular_bg);
						fileHolder.initializing = (ProgressBar) v.findViewById(R.id.initializing);
						fileHolder.circularProgress = (HoloCircularProgress) v.findViewById(R.id.progress);
						fileHolder.ftAction = (ImageView) v.findViewById(R.id.action);
						fileHolder.fileDetails = v.findViewById(R.id.file_details);
						fileHolder.fileSize = (TextView) v.findViewById(R.id.file_size);
						fileHolder.fileName = (TextView) v.findViewById(R.id.file_name);
						fileHolder.broadcastIndicator = (ImageView) v.findViewById(R.id.broadcastIndicator);
						fileHolder.time = (TextView) v.findViewById(R.id.time);
						fileHolder.status = (ImageView) v.findViewById(R.id.status);
						fileHolder.timeStatus = (View) v.findViewById(R.id.time_status);
						fileHolder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
						fileHolder.senderDetails = v.findViewById(R.id.sender_details);
						fileHolder.senderName = (TextView) v.findViewById(R.id.sender_name);
						fileHolder.senderNameUnsaved = (TextView) v.findViewById(R.id.sender_unsaved_name);
						fileHolder.avatarImage = (ImageView) v.findViewById(R.id.avatar);
						fileHolder.avatarContainer = (ViewGroup) v.findViewById(R.id.avatar_container);
						fileHolder.messageContainer = (ViewGroup) v.findViewById(R.id.message_container);
						fileHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
						v.setTag(fileHolder);
					}
				}
				dayHolder = fileHolder;
				setSenderDetails(convMessage, position, fileHolder, false);
				fileHolder.fileName.setText(hikeFile.getFileName());
				if (convMessage.isSent() && ((int) hikeFile.getFile().length() > 0))
				{
					fileHolder.fileSize.setText(Utils.getSizeForDisplay((int) hikeFile.getFile().length()));
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

				displayBroadcastIndicator(convMessage, fileHolder.broadcastIndicator, true);
				setBubbleColor(convMessage, fileHolder.messageContainer);
				setupFileState(fileHolder, fss, convMessage.getMsgID(), hikeFile, convMessage.isSent(), true);
				setTimeNStatus(position, fileHolder, false, fileHolder.messageContainer);
				setSelection(convMessage, fileHolder.selectedStateOverlay);

				fileHolder.fileThumb.setTag(convMessage);
				fileHolder.fileThumb.setOnClickListener(this);
				fileHolder.fileThumb.setOnLongClickListener(this);
				fileHolder.fileDetails.setTag(convMessage);
				fileHolder.fileDetails.setOnClickListener(this);
				fileHolder.fileDetails.setOnLongClickListener(this);
				fileHolder.circularProgressBg.setTag(convMessage);
				fileHolder.circularProgressBg.setOnClickListener(this);
				fileHolder.circularProgressBg.setOnLongClickListener(this);
			}
		}
		else if (viewType == ViewType.RECEIVE || viewType == ViewType.SEND_HIKE || viewType == ViewType.SEND_SMS)
		{
			TextViewHolder textHolder = null;
			if (viewType == ViewType.SEND_HIKE || viewType == ViewType.SEND_SMS)
			{
				if (v == null)
				{
					textHolder = new TextViewHolder();
					v = inflateView(R.layout.message_sent_text, parent, false);
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
				}
				else
				{
					textHolder = (TextViewHolder) v.getTag();
				}
			}
			else if (viewType == ViewType.RECEIVE)
			{
				if (v == null)
				{
					textHolder = new TextViewHolder();
					long time = System.currentTimeMillis();
					v = inflateView(R.layout.message_receive_text, parent, false);
					Logger.d("MSG_GET_VIEW", "" + (System.currentTimeMillis() - time));

					textHolder.text = (TextView) v.findViewById(R.id.text);
					textHolder.broadcastIndicator = (ImageView) v.findViewById(R.id.broadcastIndicator);
					textHolder.time = (TextView) v.findViewById(R.id.time);
					textHolder.status = (ImageView) v.findViewById(R.id.status);
					textHolder.timeStatus = (View) v.findViewById(R.id.time_status);
					textHolder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
					textHolder.senderDetails = v.findViewById(R.id.sender_details);
					textHolder.senderName = (TextView) v.findViewById(R.id.sender_name);
					textHolder.senderNameUnsaved = (TextView) v.findViewById(R.id.sender_unsaved_name);
					textHolder.avatarImage = (ImageView) v.findViewById(R.id.avatar);
					textHolder.avatarContainer = (ViewGroup) v.findViewById(R.id.avatar_container);
					textHolder.messageContainer = (ViewGroup) v.findViewById(R.id.message_container);
					textHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
					v.setTag(textHolder);
				}
				else
				{
					textHolder = (TextViewHolder) v.getTag();
				}
			}
			Logger.i("messageadapter", "message type " + convMessage.getMessageType());

			setBubbleColor(convMessage, textHolder.messageContainer);

			dayHolder = textHolder;
			setSenderDetails(convMessage, position, textHolder, false);
			
			if (viewType == ViewType.SEND_HIKE || viewType == ViewType.SEND_SMS)
			{
				if (convMessage.isBroadcastMessage() && !convMessage.isBroadcastConversation())
				{
					CustomSendMessageTextView tv = (CustomSendMessageTextView) textHolder.text;
					tv.setBroadcastLength();
				}
				else
				{
					CustomSendMessageTextView tv = (CustomSendMessageTextView) textHolder.text;
					tv.setDefaultLength();
				}
			}
			
			CharSequence markedUp = convMessage.getMessage();
			SmileyParser smileyParser = SmileyParser.getInstance();
			markedUp = smileyParser.addSmileySpans(markedUp, false);
			textHolder.text.setText(markedUp);
			Linkify.addLinks(textHolder.text, Linkify.ALL);
			Linkify.addLinks(textHolder.text, Utils.shortCodeRegex, "tel:");

			displayBroadcastIndicator(convMessage, textHolder.broadcastIndicator, true);
			setTimeNStatus(position, textHolder, false, textHolder.messageContainer);
			setSelection(convMessage, textHolder.selectedStateOverlay);

			boolean showTip = false;

			if (viewType == ViewType.SEND_HIKE)
			{
				if (!shownSdrIntroTip && !isGroupChat && convMessage.getState() == State.SENT_DELIVERED_READ)
				{
					if (msgIdForSdrTip == -1)
					{
						showTip = true;
					}
					else if (convMessage.getMsgID() == msgIdForSdrTip)
					{
						showTip = true;
					}
				}

				if (showTip)
				{
					msgIdForSdrTip = convMessage.getMsgID();
					inflateSdrTip(textHolder.sdrTipStub);
				}
				else
				{
					textHolder.sdrTipStub.setVisibility(View.GONE);
				}
			}
		}
		else if (viewType == ViewType.VOIP_CALL)
		{
			VoipInfoHolder infoHolder = null;
			if (v == null)
			{
				infoHolder = new VoipInfoHolder();
				v = inflateView(R.layout.voip_info, null);
				infoHolder.image = (ImageView) v.findViewById(R.id.voip_image);
				infoHolder.text = (TextView) v.findViewById(R.id.voip_text);
				infoHolder.messageInfo = (TextView) v.findViewById(R.id.timestamp);
				infoHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
				v.setTag(infoHolder);
			}
			else
			{
				infoHolder = (VoipInfoHolder) v.getTag();
			}
			dayHolder = infoHolder;
			
			if (isDefaultTheme)
			{
				infoHolder.text.setTextColor(context.getResources().getColor(R.color.list_item_subtext));
				infoHolder.messageInfo.setTextColor(context.getResources().getColor(R.color.timestampcolor));
				((View) v.findViewById(R.id.voip_details)).setBackgroundResource(R.drawable.participant_info_bg);
			}
			else
			{
				infoHolder.text.setTextColor(context.getResources().getColor(R.color.white));
				infoHolder.messageInfo.setTextColor(context.getResources().getColor(R.color.white));
				((View) v.findViewById(R.id.voip_details)).setBackgroundResource(R.drawable.participant_info_custom_bg);
			}
			
			int duration = metadata.getDuration();
			boolean initiator = metadata.isVoipInitiator();

			String message = null;
			int imageId = isDefaultTheme ? R.drawable.ic_voip_ct_miss : R.drawable.ic_voip_ct_miss_custom;
			if (convMessage.getParticipantInfoState() == ParticipantInfoState.VOIP_CALL_SUMMARY)
			{
				if (initiator)
				{
					message = context.getString(R.string.voip_call_summary_outgoing);
					imageId = isDefaultTheme ? R.drawable.ic_voip_ct_out : R.drawable.ic_voip_ct_out_custom; 
				}
				else
				{
					message = context.getString(R.string.voip_call_summary_incoming);
					imageId = isDefaultTheme ? R.drawable.ic_voip_ct_in : R.drawable.ic_voip_ct_in_custom;
				}
				message += String.format(" (%02d:%02d)", (duration / 60), (duration % 60));
			}
			else if (convMessage.getParticipantInfoState() == ParticipantInfoState.VOIP_MISSED_CALL_OUTGOING)
			{
				message = context.getString(R.string.voip_missed_call_outgoing);
			}
			else if (convMessage.getParticipantInfoState() == ParticipantInfoState.VOIP_MISSED_CALL_INCOMING)
			{
				message = context.getString(R.string.voip_missed_call_incoming);
			}
			
			infoHolder.text.setText(message);
			infoHolder.messageInfo.setText(convMessage.getTimestampFormatted(false, context));
			infoHolder.image.setImageResource(imageId);
		}
		else if (viewType == ViewType.STATUS_MESSAGE || viewType == ViewType.PIN_TEXT_SENT || viewType == ViewType.PIN_TEXT_RECEIVE)
		{
			StatusViewHolder statusHolder = null;
			if (v == null)
			{
				statusHolder = new StatusViewHolder();
				v = inflateView(R.layout.in_thread_status_update, null);
				statusHolder.image = (ImageView) v.findViewById(R.id.avatar);
				statusHolder.avatarFrame = (ImageView) v.findViewById(R.id.avatar_frame);
				statusHolder.messageTextView = (TextView) v.findViewById(R.id.status_text);
				statusHolder.dayTextView = (TextView) v.findViewById(R.id.status_info);
				statusHolder.container = (ViewGroup) v.findViewById(R.id.content_container);
				statusHolder.messageInfo = (TextView) v.findViewById(R.id.timestamp);
				statusHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
				v.setTag(statusHolder);
			}
			else
			{
				statusHolder = (StatusViewHolder) v.getTag();
			}
			dayHolder = statusHolder;

			statusHolder.image.setVisibility(View.VISIBLE);
			statusHolder.avatarFrame.setVisibility(View.VISIBLE);
			statusHolder.messageTextView.setVisibility(View.VISIBLE);
			statusHolder.dayTextView.setVisibility(View.VISIBLE);
			statusHolder.container.setVisibility(View.VISIBLE);
			statusHolder.messageInfo.setVisibility(View.VISIBLE);

			if (isDefaultTheme)
			{
				statusHolder.dayTextView.setTextColor(context.getResources().getColor(R.color.list_item_subtext));
				statusHolder.messageInfo.setTextColor(context.getResources().getColor(R.color.timestampcolor));
				statusHolder.messageTextView.setTextColor(context.getResources().getColor(R.color.list_item_header));
			}
			else
			{
				statusHolder.dayTextView.setTextColor(context.getResources().getColor(R.color.white));
				statusHolder.messageInfo.setTextColor(context.getResources().getColor(R.color.white));
				statusHolder.messageTextView.setTextColor(context.getResources().getColor(R.color.white));
			}
			statusHolder.container.setBackgroundResource(chatTheme.inLineUpdateBGResId());
			if (viewType == ViewType.STATUS_MESSAGE)
			{
				fillStatusMessageData(statusHolder, convMessage, v);
			}
			else if (viewType == ViewType.PIN_TEXT_RECEIVE || viewType == ViewType.PIN_TEXT_SENT)
			{
				fillPinTextData(statusHolder, convMessage, v);
			}
		}
		else if (viewType == ViewType.PARTICIPANT_INFO)
		{
			ParticipantInfoHolder participantInfoHolder = null;
			if (v == null)
			{
				participantInfoHolder = new ParticipantInfoHolder();
				v = inflateView(R.layout.participant_info_receive, null);
				participantInfoHolder.container = (ViewGroup) v.findViewById(R.id.participant_info_receive_container);
				participantInfoHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
				v.setTag(participantInfoHolder);
			}
			else
			{
				participantInfoHolder = (ParticipantInfoHolder) v.getTag();
			}
			dayHolder = participantInfoHolder;
			participantInfoHolder.container.setVisibility(View.VISIBLE);
			ParticipantInfoState infoState = convMessage.getParticipantInfoState();
			((ViewGroup) participantInfoHolder.container).removeAllViews();
			int positiveMargin = (int) (8 * Utils.scaledDensityMultiplier);
			int left = 0;
			int top = 0;
			int right = 0;
			int bottom = positiveMargin;

			int layoutRes = chatTheme.systemMessageLayoutId();
			
			if (infoState == ParticipantInfoState.PARTICIPANT_JOINED)
			{
				JSONArray participantInfoArray = metadata.getGcjParticipantInfo();
				TextView participantInfo = (TextView) inflater.inflate(layoutRes, null);
				String highlight = Utils.getGroupJoinHighlightText(participantInfoArray, (GroupConversation) conversation);
				String message = GroupUtils.getParticipantAddedMessage(convMessage, context, highlight);
				
				setTextAndIconForSystemMessages(participantInfo, Utils.getFormattedParticipantInfo(message, highlight), isDefaultTheme ? R.drawable.ic_joined_chat
						: R.drawable.ic_joined_chat_custom);
				((ViewGroup) participantInfoHolder.container).addView(participantInfo);
			}
			else if (infoState == ParticipantInfoState.PARTICIPANT_LEFT || infoState == ParticipantInfoState.GROUP_END)
			{
				TextView participantInfo = (TextView) inflater.inflate(layoutRes, null);

				CharSequence message;
				if (infoState == ParticipantInfoState.PARTICIPANT_LEFT)
				{
					// Showing the block international sms message if the user was
					// booted because of that reason
					if (metadata.isShowBIS())
					{
						String info = context.getString(R.string.block_internation_sms);
						String textToHighlight = context.getString(R.string.block_internation_sms_bold_text);

						TextView mainMessage = (TextView) inflater.inflate(layoutRes, null);
						setTextAndIconForSystemMessages(mainMessage, Utils.getFormattedParticipantInfo(info, textToHighlight), R.drawable.ic_no_int_sms);

						LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
						lp.setMargins(left, top, right, bottom);
						mainMessage.setLayoutParams(lp);

						((ViewGroup) participantInfoHolder.container).addView(mainMessage);
					}
					String participantMsisdn = metadata.getMsisdn();
					String name = ((GroupConversation) conversation).getGroupParticipantFirstNameAndSurname(participantMsisdn);
					message = Utils.getFormattedParticipantInfo(GroupUtils.getParticipantRemovedMessage(conversation, context, name), name);
				}
				else
				{
					message = GroupUtils.getConversationEndedMessage(conversation, context);
					
				}
				setTextAndIconForSystemMessages(participantInfo, message, isDefaultTheme ? R.drawable.ic_left_chat : R.drawable.ic_left_chat_custom);

				LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				lp.setMargins(left, top, right, 0);
				participantInfo.setLayoutParams(lp);
				((ViewGroup) participantInfoHolder.container).addView(participantInfo);
			}
			else if (infoState == ParticipantInfoState.USER_JOIN || infoState == ParticipantInfoState.USER_OPT_IN)
			{
				String name;
				String message = "";
				if (conversation instanceof GroupConversation)
				{
					String participantMsisdn = metadata.getMsisdn();
					name = ((GroupConversation) conversation).getGroupParticipantFirstNameAndSurname(participantMsisdn);
					message = context.getString(infoState == ParticipantInfoState.USER_JOIN ? (metadata.isOldUser() ? R.string.user_back_on_hike : R.string.joined_hike_new)
							: R.string.joined_conversation, name);
				}
				else
				{
					name = Utils.getFirstName(conversation.getLabel());
					if(infoState == ParticipantInfoState.USER_JOIN)
						message = String.format(convMessage.getMessage(),name);
					else
						message = context.getString(R.string.optin_one_to_one, name);
				}

				int icRes;
				if (infoState == ParticipantInfoState.USER_JOIN)
				{
					icRes = isDefaultTheme ? R.drawable.ic_user_join : R.drawable.ic_user_join_custom;
				}
				else
				{
					icRes = isDefaultTheme ? R.drawable.ic_opt_in : R.drawable.ic_opt_in_custom;
				}

				TextView mainMessage = (TextView) inflater.inflate(layoutRes, null);
				setTextAndIconForSystemMessages(mainMessage, Utils.getFormattedParticipantInfo(message, name), icRes);

				TextView creditsMessageView = null;
				if (metadata.getCredits() != -1)
				{
					int credits = metadata.getCredits();
					String creditsMessage = String.format(context.getString(R.string.earned_credits, credits));
					String highlight = String.format(context.getString(R.string.earned_credits_highlight, credits));

					creditsMessageView = (TextView) inflater.inflate(layoutRes, null);
					setTextAndIconForSystemMessages(creditsMessageView, Utils.getFormattedParticipantInfo(creditsMessage, highlight), R.drawable.ic_got_credits);

					LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
					lp.setMargins(left, top, right, 0);
					creditsMessageView.setLayoutParams(lp);
				}
				LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				lp.setMargins(left, top, right, creditsMessageView != null ? bottom : 0);
				mainMessage.setLayoutParams(lp);

				((ViewGroup) participantInfoHolder.container).addView(mainMessage);
				if (creditsMessageView != null)
				{
					((ViewGroup) participantInfoHolder.container).addView(creditsMessageView);
				}
			}
			else if ((infoState == ParticipantInfoState.CHANGED_GROUP_NAME) || (infoState == ParticipantInfoState.CHANGED_GROUP_IMAGE))
			{
				String msisdn = metadata.getMsisdn();
				String userMsisdn = preferences.getString(HikeMessengerApp.MSISDN_SETTING, "");

				String participantName = userMsisdn.equals(msisdn) ? context.getString(R.string.you) : ((GroupConversation) conversation).getGroupParticipantFirstNameAndSurname(msisdn);
				String message;
				if (convMessage.getParticipantInfoState() == ParticipantInfoState.CHANGED_GROUP_NAME)
				{
					message = GroupUtils.getConversationNameChangedMessage(conversation, context, participantName);
				}
				else
				{
					message = String.format(context.getString(R.string.change_group_image), participantName);
				}

				TextView mainMessage = (TextView) inflater.inflate(layoutRes, null);
				int icRes;
				if (infoState == ParticipantInfoState.CHANGED_GROUP_NAME)
				{
					if (convMessage.isBroadcastConversation())
					{
						icRes = R.drawable.ic_broadcast_2;
					}
					else
					{
						icRes = isDefaultTheme ? R.drawable.ic_group_info : R.drawable.ic_group_info_custom;
					}
				}
				else
				{
					icRes = isDefaultTheme ? R.drawable.ic_group_image : R.drawable.ic_group_image_custom;
				}
				setTextAndIconForSystemMessages(mainMessage, Utils.getFormattedParticipantInfo(message, participantName), icRes);

				((ViewGroup) participantInfoHolder.container).addView(mainMessage);
			}
			else if (infoState == ParticipantInfoState.BLOCK_INTERNATIONAL_SMS)
			{
				String info = context.getString(R.string.block_internation_sms);
				String textToHighlight = context.getString(R.string.block_internation_sms_bold_text);

				TextView mainMessage = (TextView) inflater.inflate(layoutRes, null);
				setTextAndIconForSystemMessages(mainMessage, Utils.getFormattedParticipantInfo(info, textToHighlight), isDefaultTheme ? R.drawable.ic_no_int_sms
						: R.drawable.ic_no_int_sms_custom);

				((ViewGroup) participantInfoHolder.container).addView(mainMessage);
			}
			else if (infoState == ParticipantInfoState.INTRO_MESSAGE)
			{
				String name = Utils.getFirstName(conversation.getLabel());
				String message;
				if (conversation.isOnhike())
				{
					boolean firstIntro = conversation.getMsisdn().hashCode() % 2 == 0;
					message = String.format(context.getString(firstIntro ? R.string.start_thread1 : R.string.start_thread1), name);
				}
				else
				{
					message = String.format(context.getString(R.string.intro_sms_thread), name);
				}

				int icRes;
				if (conversation.isOnhike())
				{
					icRes = isDefaultTheme ? R.drawable.ic_user_join : R.drawable.ic_user_join_custom;
				}
				else
				{
					icRes = isDefaultTheme ? R.drawable.ic_sms_user_ct : R.drawable.ic_sms_user_ct_custom;
				}

				TextView mainMessage = (TextView) inflater.inflate(layoutRes, null);
				setTextAndIconForSystemMessages(mainMessage, Utils.getFormattedParticipantInfo(message, name), icRes);

				((ViewGroup) participantInfoHolder.container).addView(mainMessage);
			}
			else if (infoState == ParticipantInfoState.DND_USER)
			{
				JSONArray dndNumbers = metadata.getDndNumbers();

				TextView dndMessage = (TextView) inflater.inflate(layoutRes, null);

				if (dndNumbers != null && dndNumbers.length() > 0)
				{
					StringBuilder dndNamesSB = new StringBuilder();
					for (int i = 0; i < dndNumbers.length(); i++)
					{
						String name = conversation instanceof GroupConversation ? ((GroupConversation) conversation).getGroupParticipantFirstNameAndSurname(dndNumbers.optString(i)) : Utils
								.getFirstName(conversation.getLabel());
						if (i < dndNumbers.length() - 2)
						{
							dndNamesSB.append(name + ", ");
						}
						else if (i < dndNumbers.length() - 1)
						{
							dndNamesSB.append(name + " and ");
						}
						else
						{
							dndNamesSB.append(name);
						}
					}
					String dndNames = dndNamesSB.toString();
					convMessage.setMessage(String.format(context.getString(conversation instanceof GroupConversation ? R.string.dnd_msg_gc : R.string.dnd_one_to_one), dndNames));

					SpannableStringBuilder ssb;
					if (conversation instanceof GroupConversation)
					{
						ssb = new SpannableStringBuilder(convMessage.getMessage());
						ssb.setSpan(new StyleSpan(Typeface.BOLD), convMessage.getMessage().indexOf(dndNames), convMessage.getMessage().indexOf(dndNames) + dndNames.length(),
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

					}
					else
					{

						ssb = new SpannableStringBuilder(convMessage.getMessage());
						ssb.setSpan(new StyleSpan(Typeface.BOLD), convMessage.getMessage().indexOf(dndNames), convMessage.getMessage().indexOf(dndNames) + dndNames.length(),
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						ssb.setSpan(new StyleSpan(Typeface.BOLD), convMessage.getMessage().lastIndexOf(dndNames),
								convMessage.getMessage().lastIndexOf(dndNames) + dndNames.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}

					setTextAndIconForSystemMessages(dndMessage, ssb, isDefaultTheme ? R.drawable.ic_waiting_dnd : R.drawable.ic_waiting_dnd_custom);
					LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
					lp.setMargins(left, top, right, 0);
					dndMessage.setLayoutParams(lp);

					((ViewGroup) participantInfoHolder.container).addView(dndMessage);
				}
			}
			else if (infoState == ParticipantInfoState.CHAT_BACKGROUND)
			{
				TextView mainMessage = (TextView) inflater.inflate(layoutRes, null);

				String msisdn = metadata.getMsisdn();
				String userMsisdn = preferences.getString(HikeMessengerApp.MSISDN_SETTING, "");

				String name;
				if (isGroupChat)
				{
					name = userMsisdn.equals(msisdn) ? context.getString(R.string.you) : ((GroupConversation) conversation).getGroupParticipantFirstNameAndSurname(msisdn);
				}
				else
				{
					name = userMsisdn.equals(msisdn) ? context.getString(R.string.you) : Utils.getFirstName(conversation.getLabel());
				}

				String message = context.getString(R.string.chat_bg_changed, name);

				setTextAndIconForSystemMessages(mainMessage, Utils.getFormattedParticipantInfo(message, name), isDefaultTheme ? R.drawable.ic_change_theme
						: R.drawable.ic_change_theme_custom);

				((ViewGroup) participantInfoHolder.container).addView(mainMessage);
			}
			dayHolder = participantInfoHolder;
		}
		else if (viewType == ViewType.UNREAD_COUNT)
		{
			ParticipantInfoHolder participantInfoHolder = null;
			if (v == null)
			{
				participantInfoHolder = new ParticipantInfoHolder();
				v = inflateView(R.layout.participant_info_receive, null);
				participantInfoHolder.container = (ViewGroup) v.findViewById(R.id.participant_info_receive_container);
				participantInfoHolder.dayStub = (ViewStub) v.findViewById(R.id.day_stub);
				v.setTag(participantInfoHolder);
			}
			else
			{
				participantInfoHolder = (ParticipantInfoHolder) v.getTag();
			}
			dayHolder = participantInfoHolder;
			int layoutRes = chatTheme.systemMessageLayoutId();
			TextView participantInfo = (TextView) inflater.inflate(layoutRes, null);
			if (convMessage.getUnreadCount() == 1)
			{
				participantInfo.setText(context.getResources().getString(R.string.one_unread_message));
			}
			else
			{
				participantInfo.setText(context.getResources().getString(R.string.num_unread_messages, convMessage.getUnreadCount()));
			}
			participantInfoHolder.container.removeAllViews();
			participantInfoHolder.container.addView(participantInfo);
			dayHolder = participantInfoHolder;
		}
		else if (viewType == ViewType.UNKNOWN_BLOCK_ADD)
		{
			Logger.i("chatthread", "getview of unknown header");
			if (convertView == null)
			{
				convertView = inflater.inflate(R.layout.block_add_unknown_contact, parent, false);
			}
			return convertView;
		}
		if (showDayIndicator(position))
		{
			inflateNSetDay(convMessage, dayHolder);
		}
		else if (null != dayHolder && dayHolder.dayStubInflated != null)
		{
			dayHolder.dayStubInflated.setVisibility(View.GONE);
		}
		Logger.i("chatthread", "position " + position + " time taken : " + (System.currentTimeMillis() - startTime));
		return v;
	}

	private void displayBroadcastIndicator(ConvMessage convMessage, ImageView broadcastIndicator, boolean showBlackIcon)
	{
		if (broadcastIndicator != null)
		{
			if (convMessage.isBroadcastMessage() && !convMessage.isBroadcastConversation())
			{
				if (showBlackIcon)
				{
					broadcastIndicator.setImageResource(R.drawable.ic_broadcast_system_message);
					broadcastIndicator.setVisibility(View.VISIBLE);
				}
				else
				{
					broadcastIndicator.setImageResource(R.drawable.ic_broadcast_ft);
					broadcastIndicator.setVisibility(View.VISIBLE);
				}
			}
			else
			{
				broadcastIndicator.setVisibility(View.GONE);
			}
		}
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

	private View inflateView(int resource, ViewGroup root)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return inflater.inflate(resource, root);
	}

	private View inflateView(int resource, ViewGroup root, boolean attachToRoot)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return inflater.inflate(resource, root, attachToRoot);
	}

	private void setSelection(ConvMessage convMessage, View overlay)
	{
		if (isActionModeOn || isHikeToOfflineMode)
		{
			/*
			 * This is an transparent overlay over all the message which will listen click events while action mode is on.
			 */
			overlay.setVisibility(View.VISIBLE);
			overlay.setTag(convMessage);
			overlay.setOnClickListener(selectedStateOverlayClickListener);

			if (isSelected(convMessage))
			{
				/*
				 * If a message has been selected then background of selected state overlay will change to selected state color. otherwise this overlay will be transparent
				 */
				overlay.setBackgroundColor(context.getResources().getColor(chatTheme.multiSelectBubbleColor()));
				Logger.d("sticker", "colored");
			}
			else
			{
				overlay.setBackgroundColor(context.getResources().getColor(R.color.transparent));
				Logger.d("sticker", "transparent");
			}
		}
		else
		{
			overlay.setVisibility(View.GONE);
		}
	}

	private void inflateSdrTip(ViewStub sdrStub)
	{
		sdrStub.setOnInflateListener(new ViewStub.OnInflateListener()
		{
			@Override
			public void onInflate(ViewStub stub, View inflated)
			{
				View tipContainer = inflated.findViewById(R.id.tip_container);
				showSdrTip(tipContainer);
			}
		});
		try
		{
			sdrStub.inflate();
		}
		catch (Exception e)
		{

		}
	}

	private void showSdrTip(View sdrFtueTip)
	{
		sdrFtueTip.setVisibility(View.VISIBLE);
		if (!sdrTipFadeInShown)
		{
			Animation anim = AnimationUtils.loadAnimation(context, R.anim.fade_in_animation);
			anim.setDuration(1000);
			sdrFtueTip.startAnimation(anim);
			sdrTipFadeInShown = true;
		}
	}

	private void setAvatar(String msisdn, ImageView imageView)
	{
		iconLoader.loadImage(msisdn, imageView, false, true, false);
	}

	private void setNudgeImageResource(ChatTheme chatTheme, ImageView iv, boolean isMessageSent)
	{
		iv.setImageResource(isMessageSent ? chatTheme.sentNudgeResId() : chatTheme.receivedNudgeResId());
	}

	private void inflateNSetDay(ConvMessage convMessage, final DayHolder dayHolder)
	{
		final String dateFormatted = convMessage.getMessageDate(context);
		if (dayHolder.dayStubInflated == null)
		{
			dayHolder.dayStub.setOnInflateListener(new ViewStub.OnInflateListener()
			{
				@Override
				public void onInflate(ViewStub stub, View inflated)
				{
					dayHolder.dayStubInflated = inflated;
					setDay(dayHolder.dayStubInflated, dateFormatted);
				}
			});
			try
			{
				dayHolder.dayStub.inflate();
			}
			catch (Exception e)
			{

			}
		}
		else
		{
			dayHolder.dayStubInflated.setVisibility(View.VISIBLE);
			setDay(dayHolder.dayStubInflated, dateFormatted);
		}
	}

	private void setDay(View inflated, String dateFormatted)
	{
		TextView dayTextView = (TextView) inflated.findViewById(R.id.day);
		View dayLeft = inflated.findViewById(R.id.day_left);
		View dayRight = inflated.findViewById(R.id.day_right);

		dayTextView.setText(dateFormatted.toUpperCase());

		if (isDefaultTheme)
		{
			dayTextView.setTextColor(context.getResources().getColor(R.color.list_item_header));
			dayLeft.setBackgroundColor(context.getResources().getColor(R.color.day_line));
			dayRight.setBackgroundColor(context.getResources().getColor(R.color.day_line));
		}
		else
		{
			dayTextView.setTextColor(context.getResources().getColor(R.color.white));
			dayLeft.setBackgroundColor(context.getResources().getColor(R.color.white));
			dayRight.setBackgroundColor(context.getResources().getColor(R.color.white));
		}
	}

	private void setSenderDetails(ConvMessage convMessage, int position, DetailViewHolder detailHolder, boolean ext)
	{
		boolean firstMessageFromParticipant = ifFirstMessageFromRecepient(convMessage, position);
		if (firstMessageFromParticipant)
		{
			setGroupParticipantName(convMessage, detailHolder.senderDetails, detailHolder.senderName, detailHolder.senderNameUnsaved, firstMessageFromParticipant);
			if (ext)
			{
				if (detailHolder.senderName != null)
				{
					detailHolder.senderName.setTextColor(context.getResources().getColor(chatTheme.offlineMsgTextColor()));
				}
				if (detailHolder.senderNameUnsaved != null)
				{
					detailHolder.senderNameUnsaved.setTextColor(context.getResources().getColor(chatTheme.offlineMsgTextColor()));
				}
			}
			else
			{
				if (detailHolder.senderName != null)
				{
					detailHolder.senderName.setTextColor(context.getResources().getColor(R.color.chat_color));
				}
				if (detailHolder.senderNameUnsaved != null)
				{
					detailHolder.senderNameUnsaved.setTextColor(context.getResources().getColor(R.color.unsaved_contact_name));
				}
			}
			detailHolder.avatarImage.setVisibility(View.VISIBLE);
			setAvatar(convMessage.getGroupParticipantMsisdn(), detailHolder.avatarImage);
			detailHolder.avatarContainer.setVisibility(View.VISIBLE);
		}
		else if (detailHolder.avatarContainer != null)
		{
			detailHolder.senderDetails.setVisibility(View.GONE);
			detailHolder.avatarContainer.setVisibility(isGroupChat ? View.INVISIBLE : View.GONE);
		}
	}

	private void setGroupParticipantName(ConvMessage convMessage, View participantDetails, TextView participantName, TextView participantNameUnsaved,
			boolean firstMessageFromParticipant)
	{
		if (!convMessage.isSent())
		{
			if (firstMessageFromParticipant)
			{
				String number = null;
				String name = ((GroupConversation) conversation).getGroupParticipantFirstNameAndSurname(convMessage.getGroupParticipantMsisdn());
				if (((GroupConversation) conversation).getGroupParticipant(convMessage.getGroupParticipantMsisdn()).getFirst().getContactInfo().isUnknownContact())
				{
					number = convMessage.getGroupParticipantMsisdn();
				}

				if (number != null)
				{
					participantName.setText(number);
					participantNameUnsaved.setText("| " + name + " >>");
					participantNameUnsaved.setVisibility(View.VISIBLE);
				}
				else
				{
					participantName.setSingleLine(true);
					participantName.setEllipsize(android.text.TextUtils.TruncateAt.END);
					name = ((GroupConversation) conversation).getGroupParticipantFirstNameAndSurname(convMessage.getGroupParticipantMsisdn());
					participantName.setText(name);
					participantNameUnsaved.setVisibility(View.GONE);
				}
				participantDetails.setTag(convMessage);
				participantDetails.setOnClickListener(contactClick);
				participantDetails.setVisibility(View.VISIBLE);
				participantName.setVisibility(View.VISIBLE);
			}
		}
	}

	private void setupFileState(FTViewHolder holder, FileSavedState fss, long msgId, HikeFile hikeFile, boolean isSent, boolean ext)
	{
		int downloadImage;
		int retryImage;
		int pauseImage;
		int playImage = -1;
		if (ext)
		{
			retryImage = R.drawable.ic_retry_other;
			pauseImage = R.drawable.ic_pause_other;
			downloadImage = R.drawable.ic_download_other;
		}
		else
		{
			retryImage = R.drawable.ic_retry_image_video;
			pauseImage = R.drawable.ic_pause_image_video;
			downloadImage = R.drawable.ic_download_image_video;
			playImage = R.drawable.ic_videoicon;
		}
		holder.ftAction.setVisibility(View.GONE);
		holder.circularProgressBg.setVisibility(View.GONE);
		holder.initializing.setVisibility(View.GONE);
		holder.circularProgress.setVisibility(View.GONE);
		switch (fss.getFTState())
		{
		case NOT_STARTED:
			if (!isSent)
			{
				holder.ftAction.setImageResource(downloadImage);
				holder.ftAction.setVisibility(View.VISIBLE);
				holder.circularProgressBg.setVisibility(View.VISIBLE);
			}
			else
			{
				if (TextUtils.isEmpty(hikeFile.getFileKey()))
				{
					holder.ftAction.setImageResource(retryImage);
					holder.ftAction.setVisibility(View.VISIBLE);
					holder.circularProgressBg.setVisibility(View.VISIBLE);
				}
				else if (hikeFile.getHikeFileType() == HikeFileType.VIDEO && !ext)
				{
					holder.ftAction.setImageResource(playImage);
					holder.ftAction.setVisibility(View.VISIBLE);
					holder.circularProgressBg.setVisibility(View.VISIBLE);
				}
			}
			break;
		case IN_PROGRESS:
			holder.ftAction.setImageResource(pauseImage);
			holder.ftAction.setVisibility(View.VISIBLE);
			holder.circularProgressBg.setVisibility(View.VISIBLE);
			showTransferProgress(holder, fss, msgId, hikeFile, isSent);
			break;
		case INITIALIZED:
			holder.ftAction.setImageResource(0);
			holder.ftAction.setVisibility(View.VISIBLE);
			holder.circularProgressBg.setVisibility(View.VISIBLE);
			showTransferInitialization(holder, hikeFile);
			break;
		case ERROR:
		case PAUSED:
			holder.ftAction.setImageResource(retryImage);
			holder.ftAction.setVisibility(View.VISIBLE);
			holder.circularProgressBg.setVisibility(View.VISIBLE);
			showTransferProgress(holder, fss, msgId, hikeFile, isSent);
			break;
		case CANCELLED:
			holder.ftAction.setImageResource(retryImage);
			holder.ftAction.setVisibility(View.VISIBLE);
			holder.circularProgressBg.setVisibility(View.VISIBLE);
			break;
		case COMPLETED:
			if (hikeFile.getHikeFileType() == HikeFileType.VIDEO && !ext)
			{
				holder.ftAction.setImageResource(playImage);
				holder.ftAction.setVisibility(View.VISIBLE);
				holder.circularProgressBg.setVisibility(View.VISIBLE);
			}
			break;
		default:
			break;
		}
		holder.ftAction.setScaleType(ScaleType.CENTER);
	}

	private void showTransferInitialization(FTViewHolder holder, HikeFile hikeFile)
	{
		holder.initializing.setVisibility(View.VISIBLE);
	}

	private void showTransferProgress(FTViewHolder holder, FileSavedState fss, long msgId, HikeFile hikeFile, boolean isSent)
	{
		int progress = FileTransferManager.getInstance(context).getFTProgress(msgId, hikeFile.getFile(), isSent);
		int chunkSize = FileTransferManager.getInstance(context).getChunkSize(msgId);
		if (fss.getTotalSize() <= 0 && isSent && fss.getFTState() != FTState.ERROR)
		{
			showTransferInitialization(holder, hikeFile);
		}
		else if (fss.getFTState() == FTState.IN_PROGRESS && fss.getTransferredSize() == 0)
		{
			float animatedProgress = 5 * 0.01f;
			if (fss.getTotalSize() > 0 && chunkSize > 0)
			{
				animatedProgress = (float) chunkSize;
				animatedProgress = animatedProgress / fss.getTotalSize();
			}
			if (holder.circularProgress.getRelatedMsgId() == -1 || holder.circularProgress.getCurrentProgress() > animatedProgress
					|| holder.circularProgress.getCurrentProgress() == 1.0f)
			{
				holder.circularProgress.resetProgress();
			}
			if (Utils.isHoneycombOrHigher())
			{
				holder.circularProgress.stopAnimation();
				holder.circularProgress.setAnimatedProgress(0, (int) (animatedProgress * 100), 6 * 1000);
			}
			else
			{
				holder.circularProgress.setProgress(animatedProgress);
			}
			holder.circularProgress.setRelatedMsgId(msgId);
			holder.circularProgress.setVisibility(View.VISIBLE);
			holder.circularProgressBg.setVisibility(View.VISIBLE);
		}
		else if (fss.getFTState() == FTState.IN_PROGRESS || fss.getFTState() == FTState.PAUSED || fss.getFTState() == FTState.ERROR)
		{
			if (progress < 100)
				holder.circularProgress.setProgress(progress * 0.01f);
			if (Utils.isHoneycombOrHigher())
				holder.circularProgress.stopAnimation();
			if (fss.getFTState() == FTState.IN_PROGRESS)
			{
				float animatedProgress = 5 * 0.01f;
				if (fss.getTotalSize() > 0)
				{
					animatedProgress = (float) chunkSize;
					animatedProgress = animatedProgress / fss.getTotalSize();
				}
				if (Utils.isHoneycombOrHigher())
				{
					if (holder.circularProgress.getCurrentProgress() < (0.95f) && progress == 100)
					{
						holder.circularProgress.setAnimatedProgress((int) (holder.circularProgress.getCurrentProgress() * 100), progress, 300);
					}
					else
						holder.circularProgress.setAnimatedProgress(progress, progress + (int) (animatedProgress * 100), 6 * 1000);
				}
			}
			holder.circularProgress.setVisibility(View.VISIBLE);
			holder.circularProgressBg.setVisibility(View.VISIBLE);
		}
		else
		{
			showTransferInitialization(holder, hikeFile);
		}
	}

	private boolean ifFirstMessageFromRecepient(ConvMessage convMessage, int position)
	{
		boolean ret = false;
		if (!convMessage.isSent())
		{
			if (isGroupChat && !TextUtils.isEmpty(convMessage.getGroupParticipantMsisdn()))
			{
				if (position != 0)
				{
					ConvMessage previous = getItem(position - 1);
					if (previous.getParticipantInfoState() != ParticipantInfoState.NO_INFO || !convMessage.getGroupParticipantMsisdn().equals(previous.getGroupParticipantMsisdn()))
					{
						ret = true;
					}
				}
				else
				{
					ret = true;
				}
			}
		}
		return ret;
	}

	private void createMediaThumb(ImageView fileThumb)
	{
		// TODO Auto-generated method stub
		Logger.d(getClass().getSimpleName(), "creating default thumb. . . ");
		int pixels = context.getResources().getDimensionPixelSize(R.dimen.file_thumbnail_size);
		// int pixels = (int) (250 * Utils.densityMultiplier);
		Logger.d(getClass().getSimpleName(), "density: " + Utils.scaledDensityMultiplier);
		fileThumb.getLayoutParams().height = pixels;
		fileThumb.getLayoutParams().width = pixels;
		// fileThumb.setBackgroundColor(context.getResources().getColor(R.color.file_message_item_bg))
		fileThumb.setBackgroundResource(R.drawable.bg_file_thumb);
		/*
		 * When setting default media thumb to image view, need to remove the previous drawable of that view in case of view is re-used by adapter.
		 * Fogbugz Id : 37212
		 */
		fileThumb.setImageDrawable(null);
	}

	View.OnClickListener contactClick = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			final ConvMessage message = (ConvMessage) v.getTag();
			ArrayList<String> optionsList = new ArrayList<String>();
			String number = null;
			final String name = ((GroupConversation) conversation).getGroupParticipant(message.getGroupParticipantMsisdn()).getSecond();
			if (((GroupConversation) conversation).getGroupParticipant(message.getGroupParticipantMsisdn()).getFirst().getContactInfo().isUnknownContact())
			{
				number = message.getGroupParticipantMsisdn();
				optionsList.add(context.getString(R.string.add_to_contacts));
			}
			optionsList.add(context.getString(R.string.send_message));
			final String[] options = new String[optionsList.size()];
			optionsList.toArray(options);

			AlertDialog.Builder builder = new AlertDialog.Builder(chatThread);

			ListAdapter dialogAdapter = new ArrayAdapter<CharSequence>(chatThread, R.layout.alert_item, R.id.item, options);

			if (number != null)
			{
				builder.setTitle(number + " (" + name + ")");
			}
			else
			{
				builder.setTitle(name);
			}

			builder.setAdapter(dialogAdapter, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					String option = options[which];
					if ((context.getString(R.string.add_to_contacts)).equals(option))
					{
						List<ContactInfoData> items = new ArrayList<ContactInfoData>();
						items.add(new ContactInfoData(DataType.PHONE_NUMBER, message.getGroupParticipantMsisdn(), "Mobile"));
						Utils.addToContacts(items, name, context);
					}
					else if ((context.getString(R.string.send_message)).equals(option))
					{
						Intent intent = new Intent();
						// If the contact info was made using a group conversation, then the
						// Group ID is in the contact ID
						intent.putExtra(HikeConstants.Extras.MSISDN, message.getGroupParticipantMsisdn());
						intent.putExtra(HikeConstants.Extras.SHOW_KEYBOARD, true);
						intent.setClass(context, ChatThread.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						context.startActivity(intent);
					}
				}
			});

			AlertDialog alertDialog = builder.show();
			alertDialog.getListView().setDivider(context.getResources().getDrawable(R.drawable.ic_thread_divider_profile));
			// chatThread.showContactDetails(items, name, null, true);
		}
	};

	private void setTextAndIconForSystemMessages(TextView textView, CharSequence text, int iconResId)
	{
		textView.setText(text);
		textView.setCompoundDrawablesWithIntrinsicBounds(iconResId, 0, 0, 0);
	}

	
	Handler handler = new Handler();

	public void scheduleHikeOfflineTip()
	{
		/*
		 * if international number don't show the tip
		 */
		if (!conversation.getMsisdn().startsWith(HikeConstants.INDIA_COUNTRY_CODE))
		{
			return;
		}
		/*
		 * if Kitkat OR higher we should not show tip 1. if user has 0 free SMS left; 2. user himself is not online; 3. if this is an international number;
		 */
		if (Utils.isKitkatOrHigher())
		{
			int currentSmsBalance = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getInt(HikeMessengerApp.SMS_SETTING, 0);
			Logger.d("tesst", "" + (currentSmsBalance == 0) + " " + !Utils.isUserOnline(context) + " " + !conversation.getMsisdn().startsWith(HikeConstants.INDIA_COUNTRY_CODE));
			if (currentSmsBalance == 0 || !Utils.isUserOnline(context))
			{
				return;
			}
		}

		if (showUndeliveredMessage != null)
		{
			handler.removeCallbacks(showUndeliveredMessage);
		}

		if (firstPendingConvMessage != null)
		{
			long diff = (((long) System.currentTimeMillis() / 1000) - firstPendingConvMessage.getTimestamp());

			if (Utils.isUserOnline(context) && diff < HikeConstants.DEFAULT_UNDELIVERED_WAIT_TIME && chatThread.shouldRunTimerForHikeOfflineTip)
			{
				showUndeliveredMessage = new ShowUndeliveredMessage();
				handler.postDelayed(showUndeliveredMessage, (HikeConstants.DEFAULT_UNDELIVERED_WAIT_TIME - diff) * 1000);
			}
			else if (!undeliveredMessages.isEmpty())
			{
				chatThread.showHikeToOfflineTip();
			}
		}
	}

	private boolean showDayIndicator(int position)
	{
		/*
		 * We show the time stamp in the status message separately so no need to show this time stamp.
		 */
		int type = getItemViewType(position);
		ViewType viewtype = null;
		if (type < viewTypeCount)
		{
			viewtype = ViewType.values()[type];
		}

		if (null != viewtype && (viewtype == ViewType.STATUS_MESSAGE || viewtype == ViewType.PIN_TEXT_RECEIVE || viewtype == ViewType.PIN_TEXT_SENT) )
		{
			return false;
		}
		/*
		 * only show the timestamp if the delta between this message and the previous one is greater than 10 minutes
		 */
		ConvMessage current = getItem(position);
		ConvMessage previous = position > 0 ? getItem(position - 1) : null;
		if (previous == null)
		{
			return true;
		}

		Calendar currentMessageCalendar = Calendar.getInstance();
		currentMessageCalendar.setTimeInMillis(current.getTimestamp() * 1000);

		Calendar previousMessageCalendar = Calendar.getInstance();
		previousMessageCalendar.setTimeInMillis(previous.getTimestamp() * 1000);

		return (previousMessageCalendar.get(Calendar.DAY_OF_YEAR) != currentMessageCalendar.get(Calendar.DAY_OF_YEAR));
	}

	private void setTimeNStatus(int position, DetailViewHolder detailHolder, boolean ext, View clickableItem)
	{
		ConvMessage message = getItem(position);
		TextView time = detailHolder.time;
		ImageView status = detailHolder.status;
		View timeStatus = detailHolder.timeStatus;
		time.setText(message.getTimestampFormatted(false, context));
		time.setVisibility(View.VISIBLE);
		if (message.isSent())
		{
			if (message.isFileTransferMessage() && (TextUtils.isEmpty(message.getMetadata().getHikeFiles().get(0).getFileKey())))
			{
				if (ext)
				{
					status.setImageResource(R.drawable.ic_clock_white);
				}
				else
				{
					status.setImageResource(R.drawable.ic_clock);
				}
			}
			else if (ext)
			{
				switch (message.getState())
				{
				case SENT_UNCONFIRMED:
					status.setImageResource(R.drawable.ic_clock_white);
					break;
				case SENT_CONFIRMED:
					setIconForSentMessage(message, status, R.drawable.ic_tick_white, R.drawable.ic_sms_white, R.drawable.ic_bolt_white);
					break;
				case SENT_DELIVERED:
					status.setImageResource(R.drawable.ic_double_tick_white);
					break;
				case SENT_DELIVERED_READ:
					status.setImageResource(R.drawable.ic_double_tick_r_white);
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
					break;
				case SENT_CONFIRMED:
					setIconForSentMessage(message, status, R.drawable.ic_tick, R.drawable.ic_sms, R.drawable.ic_bolt_grey);
					break;
				case SENT_DELIVERED:
					status.setImageResource(R.drawable.ic_double_tick);
					break;
				case SENT_DELIVERED_READ:
					status.setImageResource(R.drawable.ic_double_tick_r);
					break;
				default:
					break;
				}
			}
			status.setScaleType(ScaleType.CENTER);
			status.setVisibility(View.VISIBLE);
		}

		if (timeStatus != null)
			timeStatus.setVisibility(View.VISIBLE);

		if ((message.getState() != null) && (position == lastSentMessagePosition)
				&& ((message.getState() == State.SENT_DELIVERED_READ && isGroupChat) || message.getState() == State.SENT_UNCONFIRMED || message.getState() == State.SENT_CONFIRMED))
		{
			inflateNSetMessageInfo(getItem(position), detailHolder, clickableItem);
		}
		else if (detailHolder.messageInfoInflated != null)
		{
			detailHolder.messageInfoInflated.setVisibility(View.GONE);
		}
	}

	private void setIconForSentMessage(ConvMessage message, ImageView status, int tickResId, int smsDrawableResId, int boltDrawableResId)
	{
		if (conversation.isOnhike() && !(conversation instanceof GroupConversation) && !message.isBroadcastMessage())
		{
			if (message.isSMS())
			{
				status.setImageResource(smsDrawableResId);
				return;
			}
			else if (chatThread.isHikeOfflineTipShowing())
			{
				status.setImageResource(boltDrawableResId);
				return;
			}
		}
		status.setImageResource(tickResId);
	}

	private void inflateNSetMessageInfo(final ConvMessage message, final DetailViewHolder detailHolder, final View clickableItem)
	{
		if (detailHolder.messageInfoInflated == null)
		{
			detailHolder.messageInfoStub.setOnInflateListener(new ViewStub.OnInflateListener()
			{
				@Override
				public void onInflate(ViewStub stub, View inflated)
				{
					detailHolder.messageInfoInflated = inflated;
					setMessageInfo(message, detailHolder.messageInfoInflated, clickableItem);
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
			setMessageInfo(message, detailHolder.messageInfoInflated, clickableItem);
		}
	}

	private void setMessageInfo(ConvMessage message, View inflated, View clickableItem)
	{
		TextView messageInfo = (TextView) inflated.findViewById(R.id.message_info);
		ImageView sending = (ImageView) inflated.findViewById(R.id.sending_anim);

		messageInfo.setVisibility(View.GONE);
		sending.setVisibility(View.GONE);
		inflated.setVisibility(View.GONE);
		if (message.getState() == State.SENT_DELIVERED_READ && isGroupChat)
		{
			inflated.setVisibility(View.VISIBLE);
			messageInfo.setVisibility(View.VISIBLE);
			messageInfo.setTextColor(context.getResources().getColor(isDefaultTheme ? R.color.list_item_subtext : R.color.white));
			setReadByForGroup(message, messageInfo);
		}
	}

	private void setReadByForGroup(ConvMessage convMessage, TextView tv)
	{
		GroupConversation groupConversation = (GroupConversation) conversation;

		LinkedList<String> readByList = groupConversation.getReadByList();

		if (readByList == null)
		{
			tv.setText("");
		}
		else if (groupConversation.getGroupMemberAliveCount() == readByList.size())
		{
			tv.setText(R.string.read_by_everyone);
		}
		else
		{
			StringBuilder sb = new StringBuilder();

			int lastIndex = readByList.size() - HikeConstants.MAX_READ_BY_NAMES;

			boolean moreNamesThanMaxCount = false;
			if (lastIndex < 0)
			{
				lastIndex = 0;
			}
			else if (lastIndex == 1)
			{
				/*
				 * We increment the last index if its one since we can accommodate another name in this case.
				 */
				lastIndex++;
				moreNamesThanMaxCount = true;
			}
			else if (lastIndex > 0)
			{
				moreNamesThanMaxCount = true;
			}

			for (int i = readByList.size() - 1; i >= lastIndex; i--)
			{
				sb.append(groupConversation.getGroupParticipantFullFirstName(readByList.get(i)));
				if (i > lastIndex + 1)
				{
					sb.append(", ");
				}
				else if (i == lastIndex + 1)
				{
					if (moreNamesThanMaxCount)
					{
						sb.append(", ");
					}
					else
					{
						sb.append(" and ");
					}
				}
			}
			String readByString = sb.toString();
			if (moreNamesThanMaxCount)
			{
				tv.setText(context.getString(R.string.read_by_names_number, readByString, lastIndex));
			}
			else
			{
				tv.setText(context.getString(R.string.read_by_names_only, readByString));
			}
		}
	}

	@Override
	public int getCount()
	{
		return convMessages.size();
	}

	@Override
	public ConvMessage getItem(int position)
	{
		return convMessages.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	public boolean isEmpty()
	{
		return getCount() == 0;
	}

	/*
	 * if action mode is on clicking on an item will invoke this listener
	 */
	View.OnClickListener selectedStateOverlayClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			if (isActionModeOn)
			{
				chatThread.showMessageContextMenu((ConvMessage) v.getTag());
			}
			else if (isHikeToOfflineMode)
			{
				chatThread.clickedHikeToOfflineMessage((ConvMessage) v.getTag());
			}
			return;
		}
	};

	@Override
	public void onClick(View v)
	{
		ConvMessage convMessage = (ConvMessage) v.getTag();
		if (convMessage == null)
		{
			return;
		}
		Logger.d(getClass().getSimpleName(), "OnCLICK" + convMessage.getMsgID());

		if (convMessage.isFileTransferMessage())
		{
			// @GM
			MessageMetadata messageMetadata = convMessage.getMetadata();
			HikeFile hikeFile = messageMetadata.getHikeFiles().get(0);
			// HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			if (Utils.getExternalStorageState() == ExternalStorageState.NONE && hikeFile.getHikeFileType() != HikeFileType.CONTACT
					&& hikeFile.getHikeFileType() != HikeFileType.LOCATION)
			{
				Toast.makeText(context, R.string.no_external_storage, Toast.LENGTH_SHORT).show();
				return;
			}
			if (convMessage.isSent())
			{
				Logger.d(getClass().getSimpleName(), "Hike File name: " + hikeFile.getFileName() + " File key: " + hikeFile.getFileKey());

				if (!TextUtils.isEmpty(hikeFile.getFileKey()))
				{
					openFile(hikeFile, convMessage, v);
				}
				else
				{
					if ((hikeFile.getHikeFileType() == HikeFileType.LOCATION) || (hikeFile.getHikeFileType() == HikeFileType.CONTACT))
					{
						FileTransferManager.getInstance(context)
								.uploadContactOrLocation(convMessage, (hikeFile.getHikeFileType() == HikeFileType.CONTACT), conversation.isOnhike());
					}
					else
					{
						File sentFile = hikeFile.getFile();
						FileSavedState fss = FileTransferManager.getInstance(context).getUploadFileState(convMessage.getMsgID(), sentFile);
						if (fss.getFTState() == FTState.IN_PROGRESS)
						{
							FileTransferManager.getInstance(context).pauseTask(convMessage.getMsgID());
						}
						else if (fss.getFTState() != FTState.INITIALIZED)
						{
							FileTransferManager.getInstance(context).uploadFile(convMessage, conversation.isOnhike());
						}
					}
					notifyDataSetChanged();
				}
			}
			else
			{
				File receivedFile = hikeFile.getFile();
				if (((hikeFile.getHikeFileType() == HikeFileType.LOCATION) || (hikeFile.getHikeFileType() == HikeFileType.CONTACT) || hikeFile.wasFileDownloaded()))
				{
					openFile(hikeFile, convMessage, v);
				}
				else
				{
					FileSavedState fss = FileTransferManager.getInstance(context).getDownloadFileState(convMessage.getMsgID(), receivedFile);

					Logger.d(getClass().getSimpleName(), fss.getFTState().toString());

					if (fss.getFTState() == FTState.COMPLETED)
					{
						openFile(hikeFile, convMessage, v);
					}
					else if (fss.getFTState() == FTState.IN_PROGRESS)
					{
						FileTransferManager.getInstance(context).pauseTask(convMessage.getMsgID());
					}
					else if (fss.getFTState() != FTState.INITIALIZED)
					{
						FileTransferManager.getInstance(context).downloadFile(receivedFile, hikeFile.getFileKey(), convMessage.getMsgID(), hikeFile.getHikeFileType(), convMessage,
								true);
					}
					notifyDataSetChanged();
				}
			}
		}
		else if (convMessage.getMetadata() != null && convMessage.getMetadata().getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE)
		{
			Intent intent = new Intent(context, ProfileActivity.class);
			intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
			intent.putExtra(HikeConstants.Extras.ON_HIKE, conversation.isOnhike());

			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra(HikeConstants.Extras.CONTACT_INFO, convMessage.getMsisdn());
			context.startActivity(intent);
		}
	}

	private void openFile(HikeFile hikeFile, ConvMessage convMessage, View parent)
	{
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
			String fileKey = hikeFile.getFileKey();

			ImageView recAction = (ImageView) parent.findViewById(R.id.action);
			// TextView durationTxt = (TextView) parent.findViewById(convMessage.isSent() ? R.id.message_send : R.id.message_receive_ft);
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
				chatThread.hideKeyBoardIfVisible();
				ArrayList<HikeSharedFile> hsf = new ArrayList<HikeSharedFile>();
				hsf.add(new HikeSharedFile(hikeFile.serialize(), hikeFile.isSent(), convMessage.getMsgID(), convMessage.getMsisdn(), convMessage.getTimestamp(), convMessage
						.getGroupParticipantMsisdn()));
				PhotoViewerFragment.openPhoto(R.id.chatThreadParentLayout, context, hsf, true, conversation);
			}
			else
			{
				Toast.makeText(context, R.string.unable_to_open, Toast.LENGTH_SHORT).show();
			}
			return;

		default:
			HikeFile.openFile(hikeFile, context);
			return;
		}
		try
		{
			context.startActivity(openFile);
		}
		catch (ActivityNotFoundException e)
		{
			Logger.w(getClass().getSimpleName(), "Trying to open an unknown format", e);
			Toast.makeText(context, R.string.unknown_msg, Toast.LENGTH_SHORT).show();
		}

	}

	private void saveContact(HikeFile hikeFile)
	{

		String name = hikeFile.getDisplayName();

		List<ContactInfoData> items = Utils.getContactDataFromHikeFile(hikeFile);

		chatThread.showContactDetails(items, name, null, true);
	}

	/*
	 * We need to setup this onLongClickListener for all such message item which are clickable because otherwise these items will consume this event on its on.
	 */
	@Override
	public boolean onLongClick(View view)
	{
		/*
		 * here returning false will pass this event to onItemLongClick method of listview.
		 */
		return false;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = prefs.edit();
		editor.putBoolean(HikeConstants.SEND_SMS_PREF, isChecked);
		editor.commit();

		setSmsToggleSubtext(isChecked);

		HikeMessengerApp.getPubSub().publish(HikePubSub.SEND_SMS_PREF_TOGGLED, null);

		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.NATIVE_SMS, String.valueOf(isChecked));
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
		
		if (isChecked)
		{
			if (!preferences.getBoolean(HikeMessengerApp.SHOWN_NATIVE_INFO_POPUP, false))
			{
				showSMSClientDialog(true, buttonView, true);
			}
			else if (!prefs.getBoolean(HikeConstants.RECEIVE_SMS_PREF, false))
			{
				showSMSClientDialog(true, buttonView, false);
			}
		}
	}

	public void initializeSmsToggleTexts(TextView hikeSMSText, TextView regularSMSText, TextView SMSToggleSubtext)
	{
		hikeSmsText = hikeSMSText;
		regularSmsText = regularSMSText;
		smsToggleSubtext = SMSToggleSubtext;
	}

	public void setSmsToggleSubtext(boolean isChecked)
	{
		String msisdn = preferences.getString(HikeMessengerApp.MSISDN_SETTING, "");

		String text = context.getString(isChecked ? R.string.messaging_my_number : R.string.messaging_hike_number, msisdn);
		SpannableStringBuilder ssb = new SpannableStringBuilder(text);

		if (isChecked)
		{
			ssb.setSpan(new StyleSpan(Typeface.BOLD), text.indexOf(msisdn), text.indexOf(msisdn) + msisdn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			hikeSmsText.setTextColor(context.getResources().getColor(isDefaultTheme ? R.color.sms_choice_unselected : R.color.sms_choice_unselected_custom_theme));
			regularSmsText.setTextColor(context.getResources().getColor(isDefaultTheme ? R.color.sms_choice_selected : R.color.white));
		}
		else
		{
			hikeSmsText.setTextColor(context.getResources().getColor(isDefaultTheme ? R.color.sms_choice_selected : R.color.white));
			regularSmsText.setTextColor(context.getResources().getColor(isDefaultTheme ? R.color.sms_choice_unselected : R.color.sms_choice_unselected_custom_theme));
		}
		smsToggleSubtext.setText(ssb);
	}

	private class ShowUndeliveredMessage implements Runnable
	{
		@Override
		public void run()
		{
			/*
			 * if there is no firstPendingMessage we should not show the tip
			 */
			if (firstPendingConvMessage == null || !isMessageUndelivered(firstPendingConvMessage))
			{
				return;
			}

			long diff = (((long) System.currentTimeMillis() / 1000) - firstPendingConvMessage.getTimestamp());

			if (Utils.isUserOnline(context) && diff >= HikeConstants.DEFAULT_UNDELIVERED_WAIT_TIME)
			{
				chatThread.showHikeToOfflineTip();
			}
		}
	}


	private void showSMSDialog(final boolean nativeOnly)
	{
		final Dialog dialog = new Dialog(chatThread, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.sms_undelivered_popup);
		dialog.setCancelable(true);

		int selectedSmsCount = getSelectedFreeSmsCount();

		TextView popupHeader = (TextView) dialog.findViewById(R.id.popup_header);
		View hikeSMS = dialog.findViewById(R.id.hike_sms_container);
		View nativeSMS = dialog.findViewById(R.id.native_sms_container);
		TextView nativeHeader = (TextView) dialog.findViewById(R.id.native_sms_header);
		TextView hikeSmsHeader = (TextView) dialog.findViewById(R.id.hike_sms_header);
		TextView hikeSmsSubtext = (TextView) dialog.findViewById(R.id.hike_sms_subtext);

		popupHeader.setText(context.getString(R.string.send_sms_as, selectedSmsCount));
		hikeSmsSubtext.setText(context.getString(R.string.free_hike_sms_subtext, chatThread.getCurrentSmsBalance()));

		hikeSMS.setVisibility(nativeOnly ? View.GONE : View.VISIBLE);
		nativeSMS.setVisibility(Utils.isKitkatOrHigher() ? View.GONE : View.VISIBLE);

		final CheckBox sendHike = (CheckBox) dialog.findViewById(R.id.hike_sms_checkbox);

		final CheckBox sendNative = (CheckBox) dialog.findViewById(R.id.native_sms_checkbox);

		final Button alwaysBtn = (Button) dialog.findViewById(R.id.btn_always);
		final Button justOnceBtn = (Button) dialog.findViewById(R.id.btn_just_once);

		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.SEND_UNDELIVERED_ALWAYS_AS_SMS_PREF, false))
		{
			if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.SEND_UNDELIVERED_AS_NATIVE_PREF, false))
			{
				if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.RECEIVE_SMS_PREF, false))
				{
					showSMSClientDialog(false, null, false);
					return;
				}
				sendAllMessagesAsSMS(true, getAllUnsentSelectedMessages(true));
				return;
			}
			else if (!nativeOnly && chatThread.getCurrentSmsBalance() >= selectedSmsCount)
			{
				sendAllMessagesAsSMS(false, getAllUnsentSelectedMessages(true));
				return;
			}
		}

		sendHike.setChecked(true);
		if (!nativeOnly)
		{
			if (chatThread.getCurrentSmsBalance() < selectedSmsCount)
			{
				// disable Free Hike Sms Field and enabling the native sms one.
				hikeSmsSubtext.setText(context.getString(R.string.free_hike_sms_subtext_diabled, chatThread.getCurrentSmsBalance()));
				hikeSmsSubtext.setEnabled(false);
				hikeSmsHeader.setEnabled(false);
				hikeSMS.setEnabled(false);
				sendHike.setEnabled(false);
				sendHike.setChecked(false);
				sendNative.setChecked(true);
			}
			else
			{
				//Now we only show sms dialog if user has 0 free sms
				// otherwise we just send a free sms by default
				sendAllMessagesAsSMS(false, getAllUnsentSelectedMessages(true));
				return;
			}
		}

		nativeHeader.setText(context.getString(R.string.regular_sms));

		OnClickListener hikeSMSOnClickListener = new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				sendHike.setChecked(true);
				sendNative.setChecked(false);
			}
		};

		OnClickListener nativeSMSOnClickListener = new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				sendHike.setChecked(false);
				sendNative.setChecked(true);
			}
		};

		hikeSMS.setOnClickListener(hikeSMSOnClickListener);
		sendHike.setOnClickListener(hikeSMSOnClickListener);
		nativeSMS.setOnClickListener(nativeSMSOnClickListener);
		sendNative.setOnClickListener(nativeSMSOnClickListener);

		alwaysBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				smsDialogSendClick(sendHike.isChecked(), false);
				
				try
				{
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SMS_POPUP_ALWAYS_CLICKED);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				}
				catch(JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}

				if (!sendHike.isChecked())
				{
					try
					{
						JSONObject md = new JSONObject();
						md.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SMS_POPUP_REGULAR_CHECKED);
						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, md);
					}
					catch(JSONException e)
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
					}
				}
				dialog.dismiss();
			}
		});

		justOnceBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				try
				{
					smsDialogSendClick(sendHike.isChecked(), true);
				
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SMS_POPUP_JUST_ONCE_CLICKED);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
					
					if (!sendHike.isChecked())
					{						
						JSONObject md = new JSONObject();
						md.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SMS_POPUP_REGULAR_CHECKED);
						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
					}
					dialog.dismiss();
				}
				catch(JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}
			}
		});

		dialog.show();
	}

	private void smsDialogSendClick(boolean isSendHikeChecked, boolean justOnce)
	{
		if (!justOnce)
		{
			Utils.setSendUndeliveredAlwaysAsSmsSetting(context, true, !isSendHikeChecked);
		}

		if (isSendHikeChecked)
		{
			sendAllMessagesAsSMS(false, getAllUnsentSelectedMessages(true));
		}
		else
		{
			if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.RECEIVE_SMS_PREF, false))
			{
				showSMSClientDialog(false, null, false);
			}
			else
			{
				sendAllMessagesAsSMS(true, getAllUnsentSelectedMessages(true));
			}
		}
	}

	private void showSMSClientDialog(final boolean triggeredFromToggle, final CompoundButton checkBox, final boolean showingNativeInfoDialog)
	{

		HikeDialogListener smsClientDialogListener = new HikeDialog.HikeDialogListener()
		{

			@Override
			public void positiveClicked(Dialog dialog)
			{
				if (showingNativeInfoDialog)
				{
					if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.RECEIVE_SMS_PREF, false))
					{
						showSMSClientDialog(triggeredFromToggle, checkBox, false);
					}
				}
				else
				{
					Utils.setReceiveSmsSetting(context, true);
					if (!triggeredFromToggle)
					{
						sendAllMessagesAsSMS(true, getAllUnsentSelectedMessages(true));
					}
					if (!preferences.getBoolean(HikeMessengerApp.SHOWN_SMS_SYNC_POPUP, false))
					{
						HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_SMS_SYNC_DIALOG, null);
					}
				}
				if (showingNativeInfoDialog)
				{
					Editor editor = preferences.edit();
					editor.putBoolean(HikeMessengerApp.SHOWN_NATIVE_INFO_POPUP, true);
					editor.commit();
				}
				dialog.dismiss();
			}

			@Override
			public void neutralClicked(Dialog dialog)
			{

			}

			@Override
			public void negativeClicked(Dialog dialog)
			{
				if (!showingNativeInfoDialog)
				{
					Utils.setReceiveSmsSetting(context, false);
				}
				dialog.dismiss();
				if (triggeredFromToggle)
				{
					checkBox.setChecked(false);
				}
				dialog.dismiss();
			}

			@Override
			public void onSucess(Dialog dialog)
			{
				// TODO Auto-generated method stub

			}
		};

		Dialog dialog = HikeDialog.showDialog(chatThread, HikeDialog.SMS_CLIENT_DIALOG, smsClientDialogListener, triggeredFromToggle, checkBox, showingNativeInfoDialog);
		dialog.show();
	}

	public LinkedHashMap<Long, ConvMessage> getAllUnsentMessages(boolean resetTimestamp)
	{
		return undeliveredMessages;
	}

	private void sendAllMessagesAsSMS(boolean nativeSMS, List<ConvMessage> unsentMessages)
	{
		Logger.d(getClass().getSimpleName(), "Unsent messages: " + unsentMessages.size());

		if (nativeSMS)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.SEND_NATIVE_SMS_FALLBACK, unsentMessages);
			chatThread.messagesSentCloseHikeToOfflineMode(true);
			removeFromUndeliverdMessage(unsentMessages);
		}
		else
		{
			if (conversation.isOnhike())
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.SEND_HIKE_SMS_FALLBACK, unsentMessages);
				chatThread.messagesSentCloseHikeToOfflineMode(false);
				removeFromUndeliverdMessage(unsentMessages);
			}
			else
			{
				for (ConvMessage convMessage : unsentMessages)
				{
					HikeMqttManagerNew.getInstance().sendMessage(convMessage.serialize(), HikeMqttManagerNew.MQTT_QOS_ONE);
					convMessage.setTimestamp(System.currentTimeMillis() / 1000);
				}
				notifyDataSetChanged();
			}
		}
	}

	private boolean isMessageUndelivered(ConvMessage convMessage)
	{
		boolean fileUploaded = true;
		boolean isGroupChatInternational = false;
		if (convMessage.isFileTransferMessage())
		{
			HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			fileUploaded = !TextUtils.isEmpty(hikeFile.getFileKey());
		}
		if (conversation instanceof GroupConversation)
		{
			isGroupChatInternational = !HikeMessengerApp.isIndianUser();
		}
		return ((!convMessage.isSMS() && convMessage.getState().ordinal() < State.SENT_DELIVERED.ordinal()) ||
				(convMessage.isSMS() && convMessage.getState().ordinal() < State.SENT_CONFIRMED
						.ordinal())) && fileUploaded && !isGroupChatInternational;
	}

	enum VoiceMessagePlayerState
	{
		PLAYING, PAUSED, STOPPED
	}

	;

	private class VoiceMessagePlayer
	{
		String fileKey;

		MediaPlayer mediaPlayer;

		ImageView fileBtn;

		TextView durationTxt;

		View durationProgress;

		Handler handler;

		VoiceMessagePlayerState playerState;

		public VoiceMessagePlayer()
		{
			handler = new Handler();
		}

		public void playMessage(HikeFile hikeFile)
		{
			Utils.blockOrientationChange(chatThread);

			playerState = VoiceMessagePlayerState.PLAYING;
			fileKey = hikeFile.getFileKey();

			try
			{
				mediaPlayer = new MediaPlayer();
				mediaPlayer.setDataSource(hikeFile.getFilePath());
				mediaPlayer.prepare();
				mediaPlayer.start();

				setFileBtnResource();

				mediaPlayer.setOnCompletionListener(new OnCompletionListener()
				{
					@Override
					public void onCompletion(MediaPlayer mp)
					{
						resetPlayer();
					}
				});
				handler.post(updateTimer);
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
			Utils.unblockOrientationChange(chatThread);
			if (mediaPlayer == null)
			{
				return;
			}
			playerState = VoiceMessagePlayerState.PAUSED;
			mediaPlayer.pause();
			setTimer();
			setFileBtnResource();
		}

		public void resumePlayer()
		{
			if (mediaPlayer == null)
			{
				return;
			}
			Utils.blockOrientationChange(chatThread);
			playerState = VoiceMessagePlayerState.PLAYING;
			mediaPlayer.start();
			handler.post(updateTimer);
			setFileBtnResource();
		}

		public void resetPlayer()
		{
			Utils.unblockOrientationChange(chatThread);
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

	public void resetPlayerIfRunning()
	{
		voiceMessagePlayer.resetPlayer();
	}

	public IconLoader getIconImageLoader()
	{
		return iconLoader;
	}

	public HighQualityThumbLoader getHighQualityThumbLoader()
	{
		return hqThumbLoader;
	}

	public void toggleSelection(ConvMessage convMsg)
	{
		selectView(convMsg, !isSelected(convMsg));
	}

	public void removeSelection()
	{
		mSelectedItemsIds.clear();
		notifyDataSetChanged();
	}

	public void selectView(ConvMessage convMsg, boolean value)
	{
		if (value)
		{
			mSelectedItemsIds.add(convMsg.getMsgID());
		}
		else
		{
			mSelectedItemsIds.remove(convMsg.getMsgID());
		}

		notifyDataSetChanged();
	}

	public void setPositionsSelected(long[] selectedConvMsgsIds)
	{
		for (long msgId : selectedConvMsgsIds)
		{
			mSelectedItemsIds.add(msgId);
		}
		notifyDataSetChanged();
	}

	public int getSelectedCount()
	{
		return mSelectedItemsIds.size();
	}

	public HashMap<Long, ConvMessage> getSelectedMessagesMap()
	{
		HashMap<Long, ConvMessage> selectedMsgs = new HashMap<Long, ConvMessage>();
		for (ConvMessage convMessage : convMessages)
		{
			if (mSelectedItemsIds.contains(convMessage.getMsgID()))
			{
				selectedMsgs.put(convMessage.getMsgID(), convMessage);
			}
		}
		return selectedMsgs;
	}

	public long[] getSelectedMsgIdsLongArray()
	{
		long[] result = new long[mSelectedItemsIds.size()];
		int i = 0;
		for (Long msgId : mSelectedItemsIds)
		{
			result[i++] = msgId;
		}
		return result;
	}

	public Set<Long> getSelectedMessageIds()
	{
		return mSelectedItemsIds;
	}

	public boolean isSelected(ConvMessage convMsg)
	{
		return mSelectedItemsIds.contains(convMsg.getMsgID());
	}

	public void setActionMode(boolean isOn)
	{
		isActionModeOn = isOn;
	}

	public boolean shownSdrToolTip()
	{
		return msgIdForSdrTip != -1;
	}

	public void sethikeToOfflineMode(boolean isOn)
	{
		isHikeToOfflineMode = isOn;
	}

	public int getSelectedFreeSmsCount()
	{
		Collection<ConvMessage> selectedMessages = getSelectedMessagesMap().values();
		int totalMsgLength = Utils.combineInOneSmsString(context, false, selectedMessages, true).length();

		return (totalMsgLength / 140) + 1;
	}

	public void hikeOfflineSendClick()
	{
		final HashMap<Long, ConvMessage> selectedMessagesMap = getSelectedMessagesMap();
		ArrayList<Long> selectedMsgIds = new ArrayList<Long>(getSelectedMessageIds());
		Collections.sort(selectedMsgIds);

		if (firstPendingConvMessage != null && !selectedMessagesMap.isEmpty())
		{
			/*
			 * Only show the H2S fallback option if user himself is Online
			 */
			if (!Utils.isUserOnline(context))
			{
				if (!Utils.isKitkatOrHigher())
				{
					showSMSDialog(true);
				}
				else
				{
					// We are not handling this case for now.
				}
			}
			else
			{
				if (!Utils.isKitkatOrHigher())
				{
					/*
					 * Only show the H2S fallback option if messaging indian numbers.
					 */
					showSMSDialog(!conversation.getMsisdn().startsWith(HikeConstants.INDIA_COUNTRY_CODE));
				}
				else
				{
					if (chatThread.getCurrentSmsBalance() < getSelectedFreeSmsCount())
					{
						Toast toast = Toast.makeText(context, context.getString(R.string.kitkat_not_enough_sms, chatThread.getCurrentSmsBalance()), Toast.LENGTH_LONG);
						toast.setGravity(Gravity.CENTER, 0, 0);
						toast.show();
					}
					else
					{
						smsDialogSendClick(true, true);
					}
				}
			}

			return;
		}
	}

	public List<ConvMessage> getAllUnsentSelectedMessages(boolean resetTimestamp)
	{
		List<ConvMessage> unsentMessages = new ArrayList<ConvMessage>();
		final HashMap<Long, ConvMessage> selectedMessagesMap = getSelectedMessagesMap();
		ArrayList<Long> selectedMsgIds = new ArrayList<Long>(getSelectedMessageIds());
		Collections.sort(selectedMsgIds);
		for (int i = 0; i < selectedMsgIds.size(); i++)
		{
			ConvMessage convMessage = selectedMessagesMap.get(selectedMsgIds.get(i));
			if (convMessage == null)
			{
				continue;
			}
			if (!convMessage.isSent())
			{
				break;
			}
			if (!isMessageUndelivered(convMessage))
			{
				break;
			}
			if (resetTimestamp && convMessage.getState().ordinal() < State.SENT_CONFIRMED.ordinal())
			{
				convMessage.setTimestamp(System.currentTimeMillis() / 1000);
			}

			unsentMessages.add(convMessage);
		}

		return unsentMessages;
	}

	public void addToUndeliverdMessage(final ConvMessage convMessage)
	{
		chatThread.runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				if (convMessage.isSMS() || convMessage.isBroadcastMessage())
				{
					return;
				}
				undeliveredMessages.put(convMessage.getMsgID(), convMessage);
				updateFirstPendingConvMessage();
				// We need to schedule hike offline tip always when it is not there
				// Coz there might be cases when user manualy removes the tip
				if (!chatThread.isHikeOfflineTipShowing())
				{
					scheduleHikeOfflineTip();
				}
			}
		});
	}

	public void removeFromUndeliverdMessage(final ConvMessage convMessage)
	{
		removeFromUndeliverdMessage(convMessage, false);
	}

	/**
	 * @param msgDelivered signifies that removeFromUndeliverdMessage is called coz convMessage has been reached to delivered state.
	 */
	public void removeFromUndeliverdMessage(final ConvMessage convMessage, final boolean msgDelivered)
	{
		chatThread.runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				ConvMessage msg = undeliveredMessages.remove(convMessage.getMsgID());

				// if on remove if it returns null don't do anything
				if (msg == null)
				{
					return;
				}

				if (undeliveredMessages.isEmpty())
				{
					/*
					 * if all messages are delivered OR we don't have any undelivered messages than only we should reset this timer not on delivery of some message
					 */
					chatThread.shouldRunTimerForHikeOfflineTip = true;

					chatThread.hideHikeToOfflineTip(false, false, false, msgDelivered);
				}
				if (firstPendingConvMessage.equals(convMessage))
				{
					updateFirstPendingConvMessage();
				}
			}
		});
	}

	private void removeFromUndeliverdMessage(List<ConvMessage> convMessages)
	{
		for (ConvMessage convMessage : convMessages)
		{
			removeFromUndeliverdMessage(convMessage);
		}
	}

	public void removeAllFromUndeliverdMessage()
	{
		chatThread.runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				undeliveredMessages.clear();
				chatThread.shouldRunTimerForHikeOfflineTip = true;
				chatThread.hideHikeToOfflineTip();
				updateFirstPendingConvMessage();
			}
		});
	}

	public void addAllUndeliverdMessages(List<ConvMessage> messages)
	{
		for (ConvMessage convMessage : messages)
		{
			if (convMessage.getState() == State.SENT_CONFIRMED && !convMessage.isSMS() && !convMessage.isBroadcastMessage())
			{
				undeliveredMessages.put(convMessage.getMsgID(), convMessage);
			}
		}
		if (firstPendingConvMessage == null)
		{
			updateFirstPendingConvMessage();
		}
		if (!chatThread.isHikeOfflineTipShowing())
		{
			scheduleHikeOfflineTip();
		}
	}

	private void updateFirstPendingConvMessage()
	{
		if (undeliveredMessages.isEmpty())
		{
			firstPendingConvMessage = null;
		}
		else
		{
			firstPendingConvMessage = undeliveredMessages.get(undeliveredMessages.keySet().iterator().next());
		}
	}

	public long getFirstUnsentMessageId()
	{
		if (firstPendingConvMessage == null)
		{
			return -1;
		}
		return firstPendingConvMessage.getMsgID();
	}

	private void fillStatusMessageData(StatusViewHolder statusHolder, ConvMessage convMessage, View v)
	{
		StatusMessage statusMessage = convMessage.getMetadata().getStatusMessage();

		statusHolder.dayTextView.setText(context.getString(R.string.xyz_posted_update, Utils.getFirstName(conversation.getLabel())));

		statusHolder.messageInfo.setText(statusMessage.getTimestampFormatted(true, context));

		if (statusMessage.getStatusMessageType() == StatusMessageType.TEXT)
		{
			SmileyParser smileyParser = SmileyParser.getInstance();
			statusHolder.messageTextView.setText(smileyParser.addSmileySpans(statusMessage.getText(), true));
			Linkify.addLinks(statusHolder.messageTextView, Linkify.ALL);

		}
		else if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
		{
			statusHolder.messageTextView.setText(R.string.changed_profile);
		}

		if (statusMessage.hasMood())
		{
			statusHolder.image.setBackgroundDrawable(null);
			statusHolder.image.setImageResource(EmoticonConstants.moodMapping.get(statusMessage.getMoodId()));
			statusHolder.avatarFrame.setVisibility(View.GONE);
		}
		else
		{
			setAvatar(conversation.getMsisdn(), statusHolder.image);
			statusHolder.avatarFrame.setVisibility(View.VISIBLE);
		}

		statusHolder.container.setTag(convMessage);
		if (!isActionModeOn)
		{
			statusHolder.container.setEnabled(true);
			statusHolder.container.setOnClickListener(this);
		}
		else
		{
			statusHolder.container.setEnabled(false);
		}
	}

	private void fillPinTextData(StatusViewHolder statusHolder, ConvMessage convMessage, View v)
	{
		String name = convMessage.isSent() ?
				"You" :
				(conversation instanceof GroupConversation) ? ((GroupConversation) conversation).getGroupParticipantFirstNameAndSurname(convMessage.getGroupParticipantMsisdn()) : "";
		statusHolder.dayTextView.setText(context.getString(R.string.xyz_posted_pin, name));

		statusHolder.messageInfo.setText(convMessage.getTimestampFormatted(true, context));

		SmileyParser smileyParser = SmileyParser.getInstance();
		statusHolder.messageTextView.setText(smileyParser.addSmileySpans(convMessage.getMessage(), true));
		Linkify.addLinks(statusHolder.messageTextView, Linkify.ALL);

		setAvatar(convMessage.isSent() ? myMsisdn : convMessage.getGroupParticipantMsisdn(), statusHolder.image);
		statusHolder.avatarFrame.setVisibility(View.VISIBLE);

		statusHolder.container.setTag(convMessage);
		if (!isActionModeOn)
		{
			statusHolder.container.setEnabled(true);
			statusHolder.container.setOnClickListener(this);
		}
		else
		{
			statusHolder.container.setEnabled(false);
		}

	}

	private boolean isListFlinging;

	public void setIsListFlinging(boolean isFling)
	{
		Logger.d("scroll", "Message Adapter set list flinging " + isFling);
		boolean notify = isFling != isListFlinging;
		Logger.d("scroll", "Message Adapter notify " + notify);
		isListFlinging = isFling;

		if (notify && !isListFlinging)
		{
			chatThread.notifyFileThumbnailDataSetChanged();
		}
	}

	public boolean containsMediaMessage(ArrayList<Long> msgIds)
	{
		/*
		 * Iterating in reverse order since its more likely the user wants to know about latest messages.
		 */
		int lastIndex = msgIds.size() - 1;
		for (int i = lastIndex; i >= 0; i--)
		{
			long msgId = msgIds.get(i);
			for (ConvMessage convMessage : convMessages)
			{
				if (convMessage.getMsgID() == msgId && convMessage.isFileTransferMessage())
				{
					HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
					if (hikeFile.getHikeFileType() == HikeFileType.IMAGE || hikeFile.getHikeFileType() == HikeFileType.VIDEO || hikeFile.getHikeFileType() == HikeFileType.AUDIO_RECORDING)
					{
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public void onDestroy()
	{
		if (mWebViewCardRenderer != null)
		{
			mWebViewCardRenderer.onDestroy();
		}
	}
}
