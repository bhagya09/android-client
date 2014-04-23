package com.bsb.hike.adapters;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.json.JSONArray;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
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
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.HikeTip.TipType;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.filetransfer.FileSavedState;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.filetransfer.FileTransferManager;
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
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.MessageMetadata.NudgeAnimationType;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.tasks.DownloadSingleStickerTask;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.HikeTip;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.StickerManager.StickerCategoryId;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;
import com.bsb.hike.view.HoloCircularProgress;

public class MessagesAdapter extends BaseAdapter implements OnClickListener, OnLongClickListener, OnCheckedChangeListener
{

	public static final int LAST_READ_CONV_MESSAGE_ID = -911;

	private enum ViewType
	{
		RECEIVE, SEND_SMS, SEND_HIKE, PARTICIPANT_INFO, FILE_TRANSFER_SEND, FILE_TRANSFER_RECEIVE, LAST_READ, STATUS_MESSAGE, SMS_TOGGLE, UNREAD_COUNT
	};

	private class ViewHolder
	{
		LinearLayout dayContainer;

		TextView messageTextView;

		TextView dayTextView;

		ImageView image;

		ImageView avatarFrame;

		ViewGroup container;

		ImageView fileThumb;

		View participantDetailsFT;

		TextView participantNameFT;

		TextView participantNameFTUnsaved;

		View loadingThumb;

		ImageView poke;

		View messageContainer;

		TextView messageInfo;

		CheckBox smsToggle;

		TextView hikeSmsText;

		TextView regularSmsText;

		View stickerPlaceholder;

		ProgressBar stickerLoader;

		View stickerParticipantDetails;
		
		TextView stickerParticipantName;
		
		TextView stickerParticipantNameUnsaved;

		ImageView stickerImage;

		View bubbleContainer;

		ImageView sending;

		ImageView typing;

		ViewGroup avatarContainer;

		ViewGroup typingAvatarContainer;

		// @GM View Items needed for pause/resume overlay
		View circularProgressBg;

		View circularProgressBgExt;

		HoloCircularProgress circularProgress;

		HoloCircularProgress circularProgressExt;

		ProgressBar wating;

		ProgressBar watingExt;

		ProgressBar watingRec;

		ImageView ftAction;

		ImageView ftActionExt;

		ImageView recAction;

		View recPlaceholder;

		View fileDetails;

		View messageSize;

		TextView fileSize;

		TextView fileName;

		TextView fileSizeExt;

		TextView recDuration;

		HoloCircularProgress recProgress;

		TextView messageTime;

		TextView ftMessageTime;

		TextView extMessageTime;

		TextView intMessageTime;

		ImageView messageStatus;

		ImageView ftMessageStatus;

		ImageView extMessageStatus;

		ImageView intMessageStatus;

		View ftMessageTimeStatus;

		View extMessageTimeStatus;

		View intMessageTimeStatus;

		View dayLeft;

		View dayRight;

		ImageView pokeCustom;

		TextView fileExtension;

		View selectedStateOverlay;

		View sdrFtueTip;
		
		ImageView filmstripLeft;
		
		ImageView filmstripRight;
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

	private String statusIdForTip;

	private long msgIdForSdrTip = -1;

	private SharedPreferences preferences;

	private boolean isGroupChat;

	private ChatTheme chatTheme;

	private boolean isDefaultTheme = true;

	private IconLoader iconLoader;

	// private StickerLoader largeStickerLoader;
	private int mIconImageSize;

	private Set<Integer> mSelectedItemsIds;

	private boolean isActionModeOn = false;

	private boolean shownSdrIntroTip = true;
	
	private boolean sdrTipFadeInShown = false;

	public MessagesAdapter(Context context, ArrayList<ConvMessage> objects, Conversation conversation, ChatThread chatThread)
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
		this.mSelectedItemsIds = new HashSet<Integer>();
		setLastSentMessagePosition();
		this.shownSdrIntroTip = preferences.getBoolean(HikeMessengerApp.SHOWN_SDR_INTRO_TIP, false);
	}

	public void setChatTheme(ChatTheme theme)
	{
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
		Logger.d(getClass().getSimpleName(), "received convMsg" + convMessage.getMsgID());
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
		if (convMessage.getUnreadCount() > 0)
		{
			type = ViewType.UNREAD_COUNT;
		}
		else if (convMessage.getTypingNotification() != null)
		{
			type = ViewType.RECEIVE;
		}
		else if (convMessage.getMsgID() == ConvMessage.SMS_TOGGLE_ID)
		{
			type = ViewType.SMS_TOGGLE;
		}
		else if (convMessage.getMsgID() == LAST_READ_CONV_MESSAGE_ID)
		{
			type = ViewType.LAST_READ;
		}
		else if (convMessage.isFileTransferMessage())
		{
			type = convMessage.isSent() ? ViewType.FILE_TRANSFER_SEND : ViewType.FILE_TRANSFER_RECEIVE;
		}
		else if (convMessage.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE)
		{
			type = ViewType.STATUS_MESSAGE;
		}
		else if (convMessage.getParticipantInfoState() != ParticipantInfoState.NO_INFO)
		{
			type = ViewType.PARTICIPANT_INFO;
		}
		else if (convMessage.isSent())
		{
			type = conversation.isOnhike() ? ViewType.SEND_HIKE : ViewType.SEND_SMS;
		}
		else
		{
			type = ViewType.RECEIVE;
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
		return ViewType.values().length;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ViewType viewType = ViewType.values()[getItemViewType(position)];

		final ConvMessage convMessage = getItem(position);

		ViewHolder holder = null;
		View v = convertView;
		if (v == null)
		{
			holder = new ViewHolder();

			switch (viewType)
			{
			case LAST_READ:
				v = inflater.inflate(R.layout.last_read_line, null);
				break;
			case UNREAD_COUNT:
				v = inflater.inflate(R.layout.message_item_receive, null);
				holder.dayContainer = (LinearLayout) v.findViewById(R.id.day_container);
				holder.dayTextView = (TextView) v.findViewById(R.id.day);
				holder.container = (ViewGroup) v.findViewById(R.id.participant_info_container);
				holder.dayLeft = v.findViewById(R.id.day_left);
				holder.dayRight = v.findViewById(R.id.day_right);
				v.findViewById(R.id.receive_message_container).setVisibility(View.GONE);
				break;
			case STATUS_MESSAGE:
				v = inflater.inflate(R.layout.in_thread_status_update, null);

				holder.image = (ImageView) v.findViewById(R.id.avatar);
				holder.avatarFrame = (ImageView) v.findViewById(R.id.avatar_frame);
				holder.messageTextView = (TextView) v.findViewById(R.id.status_text);
				holder.dayTextView = (TextView) v.findViewById(R.id.status_info);
				holder.container = (ViewGroup) v.findViewById(R.id.content_container);
				holder.messageInfo = (TextView) v.findViewById(R.id.timestamp);
				break;
			case PARTICIPANT_INFO:
				v = inflater.inflate(R.layout.message_item_receive, null);

				holder.image = (ImageView) v.findViewById(R.id.avatar);
				holder.dayContainer = (LinearLayout) v.findViewById(R.id.day_container);
				holder.dayTextView = (TextView) v.findViewById(R.id.day);
				holder.container = (ViewGroup) v.findViewById(R.id.participant_info_container);
				holder.dayLeft = v.findViewById(R.id.day_left);
				holder.dayRight = v.findViewById(R.id.day_right);

				holder.image.setVisibility(View.GONE);
				v.findViewById(R.id.receive_message_container).setVisibility(View.GONE);
				break;

			case FILE_TRANSFER_SEND:
				v = inflater.inflate(R.layout.message_item_send, parent, false);

				holder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
				holder.loadingThumb = v.findViewById(R.id.loading_thumb);
				holder.image = (ImageView) v.findViewById(R.id.msg_status_indicator);
				holder.messageTextView = (TextView) v.findViewById(R.id.message_send_ft);
				v.findViewById(R.id.message_send).setVisibility(View.GONE);
				holder.messageSize = (View) v.findViewById(R.id.message_size_ft);
				holder.fileSize = (TextView) v.findViewById(R.id.file_size);
				holder.fileDetails = (View) v.findViewById(R.id.file_details);
				holder.fileSizeExt = (TextView) v.findViewById(R.id.file_size_ext);
				holder.fileName = (TextView) v.findViewById(R.id.file_name);
				holder.fileExtension = (TextView) v.findViewById(R.id.file_extension);
				holder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
				holder.ftMessageTime = (TextView) v.findViewById(R.id.message_time_ft);
				holder.ftMessageStatus = (ImageView) v.findViewById(R.id.message_status_ft);
				holder.ftMessageTimeStatus = (View) v.findViewById(R.id.message_time_status_ft);
				holder.circularProgress = (HoloCircularProgress) v.findViewById(R.id.circular_progress);
				holder.circularProgressExt = (HoloCircularProgress) v.findViewById(R.id.circular_progress_ext);
				holder.circularProgressBg = (View) v.findViewById(R.id.circular_bg);
				holder.circularProgressBgExt = (View) v.findViewById(R.id.circular_bg_ext);
				holder.wating = (ProgressBar) v.findViewById(R.id.initializing);
				holder.watingExt = (ProgressBar) v.findViewById(R.id.initializing_ext);
				holder.ftAction = (ImageView) v.findViewById(R.id.ft_action);
				holder.ftActionExt = (ImageView) v.findViewById(R.id.ft_action_ext);
				holder.recPlaceholder = (View) v.findViewById(R.id.rec_placeholder);
				holder.recDuration = (TextView) v.findViewById(R.id.rec_duration);
				holder.recProgress = (HoloCircularProgress) v.findViewById(R.id.rec_circular_progress);
				holder.watingRec = (ProgressBar) v.findViewById(R.id.rec_initializing);
				holder.recAction = (ImageView) v.findViewById(R.id.rec_action);
				holder.filmstripLeft = (ImageView) v.findViewById(R.id.filmstrip_left);
				holder.filmstripRight = (ImageView) v.findViewById(R.id.filmstrip_right);
			case SEND_HIKE:
			case SEND_SMS:
				if (v == null)
				{
					v = inflater.inflate(R.layout.message_item_send, parent, false);
				}
				holder.dayContainer = (LinearLayout) v.findViewById(R.id.day_container);
				holder.dayLeft = v.findViewById(R.id.day_left);
				holder.dayRight = v.findViewById(R.id.day_right);
				holder.dayTextView = (TextView) v.findViewById(R.id.day);
				holder.poke = (ImageView) v.findViewById(R.id.poke_sent);
				holder.pokeCustom = (ImageView) v.findViewById(R.id.poke_sent_custom);
				holder.messageContainer = v.findViewById(R.id.sent_message_container);
				if (holder.messageTextView == null)
				{
					holder.messageTextView = (TextView) v.findViewById(R.id.message_send);
				}

				holder.messageInfo = (TextView) v.findViewById(R.id.msg_info);

				holder.stickerPlaceholder = v.findViewById(R.id.sticker_placeholder);
				holder.stickerLoader = (ProgressBar) v.findViewById(R.id.loading_progress);
				holder.stickerParticipantName = (TextView) v.findViewById(R.id.participant_name);
				holder.stickerImage = (ImageView) v.findViewById(R.id.sticker_image);
				holder.bubbleContainer = v.findViewById(R.id.bubble_container);
				holder.sending = (ImageView) v.findViewById(R.id.sending_anim);
				holder.messageTime = (TextView) v.findViewById(R.id.message_time);
				holder.messageStatus = (ImageView) v.findViewById(R.id.message_status);
				holder.extMessageTime = (TextView) v.findViewById(R.id.message_time_ext);
				holder.extMessageStatus = (ImageView) v.findViewById(R.id.message_status_ext);
				holder.intMessageTime = (TextView) v.findViewById(R.id.message_time_int);
				holder.intMessageStatus = (ImageView) v.findViewById(R.id.message_status_int);
				holder.extMessageTimeStatus = (View) v.findViewById(R.id.message_time_status_ext);
				holder.intMessageTimeStatus = (View) v.findViewById(R.id.message_time_status_int);
				holder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
				holder.sdrFtueTip = v.findViewById(R.id.sdr_ftue_tip);
				break;

			case FILE_TRANSFER_RECEIVE:
				v = inflater.inflate(R.layout.message_item_receive, parent, false);

				holder.fileThumb = (ImageView) v.findViewById(R.id.file_thumb);
				holder.messageTextView = (TextView) v.findViewById(R.id.message_receive_ft);
				holder.messageSize = (View) v.findViewById(R.id.message_size_ft);
				holder.fileSize = (TextView) v.findViewById(R.id.file_size);
				holder.messageTextView.setVisibility(View.VISIBLE);
				holder.fileDetails = (View) v.findViewById(R.id.file_details);
				holder.fileSizeExt = (TextView) v.findViewById(R.id.file_size_ext);
				holder.fileName = (TextView) v.findViewById(R.id.file_name);
				holder.fileExtension = (TextView) v.findViewById(R.id.file_extension);
				v.findViewById(R.id.message_receive).setVisibility(View.GONE);
				holder.ftMessageTime = (TextView) v.findViewById(R.id.message_time_ft);
				holder.ftMessageStatus = (ImageView) v.findViewById(R.id.message_status_ft);
				holder.ftMessageTimeStatus = (View) v.findViewById(R.id.message_time_status_ft);
				holder.circularProgress = (HoloCircularProgress) v.findViewById(R.id.circular_progress);
				holder.circularProgressExt = (HoloCircularProgress) v.findViewById(R.id.circular_progress_ext);
				holder.circularProgressBg = (View) v.findViewById(R.id.circular_bg);
				holder.circularProgressBgExt = (View) v.findViewById(R.id.circular_bg_ext);
				holder.wating = (ProgressBar) v.findViewById(R.id.initializing);
				holder.watingExt = (ProgressBar) v.findViewById(R.id.initializing_ext);
				holder.ftAction = (ImageView) v.findViewById(R.id.ft_action);
				holder.ftActionExt = (ImageView) v.findViewById(R.id.ft_action_ext);
				holder.recPlaceholder = (View) v.findViewById(R.id.rec_placeholder);
				holder.recDuration = (TextView) v.findViewById(R.id.rec_duration);
				holder.recProgress = (HoloCircularProgress) v.findViewById(R.id.rec_circular_progress);
				holder.watingRec = (ProgressBar) v.findViewById(R.id.rec_initializing);
				holder.recAction = (ImageView) v.findViewById(R.id.rec_action);
				holder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
				holder.filmstripLeft = (ImageView) v.findViewById(R.id.filmstrip_left);
				holder.filmstripRight = (ImageView) v.findViewById(R.id.filmstrip_right);
			case RECEIVE:
				if (v == null)
				{
					v = inflater.inflate(R.layout.message_item_receive, parent, false);
				}
				holder.participantDetailsFT = (View) v.findViewById(R.id.participant_details_ft);
				holder.participantNameFT = (TextView) v.findViewById(R.id.participant_name_ft);
				holder.participantNameFTUnsaved = (TextView) v.findViewById(R.id.participant_name_ft_unsaved);
				holder.image = (ImageView) v.findViewById(R.id.avatar);
				holder.avatarContainer = (ViewGroup) v.findViewById(R.id.avatar_container);
				if (holder.messageTextView == null)
				{
					holder.messageTextView = (TextView) v.findViewById(R.id.message_receive);
				}
				holder.poke = (ImageView) v.findViewById(R.id.poke_receive);
				holder.pokeCustom = (ImageView) v.findViewById(R.id.poke_receive_custom);
				holder.messageContainer = v.findViewById(R.id.receive_message_container);
				holder.dayContainer = (LinearLayout) v.findViewById(R.id.day_container);
				holder.dayTextView = (TextView) v.findViewById(R.id.day);
				holder.dayLeft = v.findViewById(R.id.day_left);
				holder.dayRight = v.findViewById(R.id.day_right);
				holder.container = (ViewGroup) v.findViewById(R.id.participant_info_container);
				holder.stickerPlaceholder = v.findViewById(R.id.sticker_placeholder);
				holder.stickerLoader = (ProgressBar) v.findViewById(R.id.loading_progress);
				holder.stickerParticipantDetails = (View) v.findViewById(R.id.participant_details);
				holder.stickerParticipantName = (TextView) v.findViewById(R.id.participant_name);
				holder.stickerParticipantNameUnsaved = (TextView) v.findViewById(R.id.participant_name_unsaved);
				holder.stickerImage = (ImageView) v.findViewById(R.id.sticker_image);
				holder.bubbleContainer = v.findViewById(R.id.bubble_container);
				holder.messageInfo = (TextView) v.findViewById(R.id.msg_info);
				holder.typing = (ImageView) v.findViewById(R.id.typing);
				holder.typingAvatarContainer = (ViewGroup) v.findViewById(R.id.typing_avatar_container);
				holder.messageTime = (TextView) v.findViewById(R.id.message_time);
				holder.extMessageTime = (TextView) v.findViewById(R.id.message_time_ext);
				holder.intMessageTime = (TextView) v.findViewById(R.id.message_time_int);
				holder.container.setVisibility(View.GONE);
				holder.selectedStateOverlay = v.findViewById(R.id.selected_state_overlay);
				break;
			case SMS_TOGGLE:
				v = inflater.inflate(R.layout.sms_toggle_item, parent, false);

				holder.messageTextView = (TextView) v.findViewById(R.id.sms_toggle_subtext);
				holder.smsToggle = (CheckBox) v.findViewById(R.id.checkbox);
				holder.hikeSmsText = (TextView) v.findViewById(R.id.hike_text);
				holder.regularSmsText = (TextView) v.findViewById(R.id.sms_text);
			}
			v.setTag(holder);
		}
		else
		{
			holder = (ViewHolder) v.getTag();
		}

		// int fieldCount = 1;
		for (Field field : holder.getClass().getDeclaredFields())
		{
			View view = null;
			field.setAccessible(true);
			if (("this$0").equals(field.getName()))
			{
				continue;
			}
			try
			{
				if (field.get(holder) != null)
				{
					if (field.get(holder) instanceof View)
						view = (View) field.get(holder);
				}

			}
			catch (IllegalArgumentException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IllegalAccessException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (view != null)
			{
				view.setVisibility(View.GONE);
			}
			// fieldCount++;
		}
		if (holder.bubbleContainer != null)
			holder.bubbleContainer.setVisibility(View.VISIBLE);

		// Applicable to all kinds of messages
		if (convMessage.getTypingNotification() != null)
		{
			holder.typing.setVisibility(View.VISIBLE);

			AnimationDrawable ad = (AnimationDrawable) holder.typing.getDrawable();
			ad.setCallback(holder.typing);
			ad.setVisible(true, true);
			ad.start();

			if (isGroupChat)
			{
				holder.typingAvatarContainer.setVisibility(View.VISIBLE);

				GroupTypingNotification groupTypingNotification = (GroupTypingNotification) convMessage.getTypingNotification();
				List<String> participantList = groupTypingNotification.getGroupParticipantList();

				holder.typingAvatarContainer.removeAllViews();

				for (int i = participantList.size() - 1; i >= 0; i--)
				{
					String msisdn = participantList.get(i);

					View avatarContainer = inflater.inflate(R.layout.small_avatar_container, holder.typingAvatarContainer, false);
					ImageView imageView = (ImageView) avatarContainer.findViewById(R.id.avatar);
					/*
					 * Catching OOB here since the participant list can be altered by another thread. In that case an OOB will be thrown here. The only impact that will have is
					 * that the image which has been removed will be skipped.
					 */
					try
					{
						setAvatar(msisdn, imageView);

						holder.typingAvatarContainer.addView(avatarContainer);
					}
					catch (IndexOutOfBoundsException e)
					{

					}
				}
			}
			return v;
		}
		if (viewType == ViewType.SMS_TOGGLE)
		{
			smsToggleSubtext = holder.messageTextView;
			hikeSmsText = holder.hikeSmsText;
			regularSmsText = holder.regularSmsText;

			if (isDefaultTheme)
			{
				holder.hikeSmsText.setTextColor(context.getResources().getColor(R.color.sms_choice_unselected));
				holder.regularSmsText.setTextColor(context.getResources().getColor(R.color.sms_choice_unselected));
				holder.messageTextView.setTextColor(context.getResources().getColor(R.color.sms_choice_unselected));
				holder.smsToggle.setButtonDrawable(R.drawable.sms_checkbox);
				v.setBackgroundResource(R.drawable.bg_sms_toggle);
			}
			else
			{
				holder.hikeSmsText.setTextColor(context.getResources().getColor(R.color.white));
				holder.regularSmsText.setTextColor(context.getResources().getColor(R.color.white));
				holder.messageTextView.setTextColor(context.getResources().getColor(R.color.white));
				holder.smsToggle.setButtonDrawable(R.drawable.sms_checkbox_custom_theme);
				v.setBackgroundResource(chatTheme.smsToggleBgRes());
			}

			boolean smsToggleOn = Utils.getSendSmsPref(context);
			holder.smsToggle.setChecked(smsToggleOn);
			setSmsToggleSubtext(smsToggleOn);

			holder.messageTextView.setVisibility(View.VISIBLE);
			holder.smsToggle.setVisibility(View.VISIBLE);
			holder.hikeSmsText.setVisibility(View.VISIBLE);
			holder.regularSmsText.setVisibility(View.VISIBLE);

			holder.smsToggle.setOnCheckedChangeListener(this);
			return v;
		}

		if (showDayIndicator(position))
		{
			String dateFormatted = convMessage.getMessageDate(context);
			holder.dayTextView.setText(dateFormatted.toUpperCase());

			if (isDefaultTheme)
			{
				holder.dayTextView.setTextColor(context.getResources().getColor(R.color.list_item_header));
				holder.dayLeft.setBackgroundColor(context.getResources().getColor(R.color.day_line));
				holder.dayRight.setBackgroundColor(context.getResources().getColor(R.color.day_line));
			}
			else
			{
				holder.dayTextView.setTextColor(context.getResources().getColor(R.color.white));
				holder.dayLeft.setBackgroundColor(context.getResources().getColor(R.color.white));
				holder.dayRight.setBackgroundColor(context.getResources().getColor(R.color.white));
			}
			holder.dayTextView.setVisibility(View.VISIBLE);
			holder.dayLeft.setVisibility(View.VISIBLE);
			holder.dayRight.setVisibility(View.VISIBLE);
			holder.dayContainer.setVisibility(View.VISIBLE);
		}
		boolean firstMessageFromParticipant = ifFirstMessageFromRecepient(convMessage, position);
		MessageMetadata metadata = convMessage.getMetadata();

		// //////////////////////////////////////////////////////////////////////////
		// Categorical Applications
		if (viewType == ViewType.RECEIVE || viewType == ViewType.SEND_HIKE || viewType == ViewType.SEND_SMS)
		{
			if (metadata != null && metadata.isPokeMessage())
			{
				holder.stickerPlaceholder.setVisibility(View.VISIBLE);
				holder.stickerPlaceholder.setBackgroundResource(0);
				setGroupParticipantName(convMessage, holder.stickerParticipantDetails, holder.stickerParticipantName, holder.stickerParticipantNameUnsaved, firstMessageFromParticipant);
				holder.stickerParticipantName.setTextColor(context.getResources().getColor(chatTheme.offlineMsgTextColor()));
				if (holder.stickerParticipantNameUnsaved != null)
				{
					holder.stickerParticipantNameUnsaved.setTextColor(context.getResources().getColor(chatTheme.offlineMsgTextColor()));
				}
				// if (isDefaultTheme)
				// {
				// holder.poke.setVisibility(View.VISIBLE);
				// holder.messageContainer.setVisibility(View.VISIBLE);
				// setNudgeImageResource(chatTheme, holder.poke, convMessage.isSent());
				// }
				// else
				if (!chatTheme.isAnimated())
				{
					holder.pokeCustom.setVisibility(View.VISIBLE);
					holder.messageContainer.setVisibility(View.GONE);
					setNudgeImageResource(chatTheme, holder.pokeCustom, convMessage.isSent());
				}
				else
				{
					holder.pokeCustom.setVisibility(View.VISIBLE);
					holder.messageContainer.setVisibility(View.GONE);

					setNudgeImageResource(chatTheme, holder.pokeCustom, convMessage.isSent());
					if (metadata.getNudgeAnimationType() != NudgeAnimationType.NONE)
					{
						metadata.setNudgeAnimationType(NudgeAnimationType.NONE);
						holder.pokeCustom.startAnimation(AnimationUtils.loadAnimation(context, R.anim.valetines_nudge_anim));
					}
				}
			}
			else if (convMessage.isStickerMessage())
			{
				holder.stickerPlaceholder.setVisibility(View.VISIBLE);
				holder.stickerPlaceholder.setBackgroundResource(0);

				Sticker sticker = metadata.getSticker();
				setGroupParticipantName(convMessage, holder.stickerParticipantDetails, holder.stickerParticipantName, holder.stickerParticipantNameUnsaved, firstMessageFromParticipant);
				holder.stickerParticipantName.setTextColor(context.getResources().getColor(chatTheme.offlineMsgTextColor()));
				if (holder.stickerParticipantNameUnsaved != null)
				{
					holder.stickerParticipantNameUnsaved.setTextColor(context.getResources().getColor(chatTheme.offlineMsgTextColor()));
				}
//				if (!convMessage.isSent())
//				{
//					
//					if (firstMessageFromParticipant)
//					{
//						holder.stickerParticipantName.setVisibility(View.VISIBLE);
//						holder.stickerParticipantName.setText(((GroupConversation) conversation).getGroupParticipantFirstName(convMessage.getGroupParticipantMsisdn()));
//						holder.stickerParticipantName.setTextColor(context.getResources().getColor(isDefaultTheme ? R.color.chat_color : R.color.white));
//					}
//					else
//					{
//						holder.stickerParticipantName.setVisibility(View.GONE);
//					}
//				}

				/*
				 * If this is the default category, then the sticker are part of the app bundle itself
				 */
				if (sticker.getStickerIndex() != -1)
				{
					holder.stickerImage.setVisibility(View.VISIBLE);
					if (StickerCategoryId.doggy.equals(sticker.getCategory().categoryId))
					{
						// TODO : this logic has to change, we should not calculate stuff based on sticker index but stickerId
						int idx = sticker.getStickerIndex();
						if (idx >= 0)
							holder.stickerImage.setImageResource(StickerManager.getInstance().LOCAL_STICKER_RES_IDS_DOGGY[idx]);
					}
					else if (StickerCategoryId.humanoid.equals(sticker.getCategory().categoryId))
					{
						// TODO : this logic has to change, we should not calculate stuff based on sticker index but stickerId
						int idx = sticker.getStickerIndex();
						if (idx >= 0)
							holder.stickerImage.setImageResource(StickerManager.getInstance().LOCAL_STICKER_RES_IDS_HUMANOID[idx]);
					}
				}
				else
				{
					String categoryId;
					/*
					 * If the category is an unknown one, we have the category id stored in the metadata.
					 */
					if (sticker.getCategory().categoryId == StickerCategoryId.unknown)
					{
						categoryId = metadata.getUnknownStickerCategory();
					}
					else
					{
						categoryId = sticker.getCategory().categoryId.name();
					}
					String stickerId = sticker.getStickerId();

					String categoryDirPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(context, categoryId) + HikeConstants.LARGE_STICKER_ROOT;
					File stickerImage = null;
					if (categoryDirPath != null)
					{
						stickerImage = new File(categoryDirPath, stickerId);
					}

					String key = categoryId + stickerId;
					boolean downloadingSticker = StickerManager.getInstance().isStickerDownloading(key);

					if (stickerImage != null && stickerImage.exists() && !downloadingSticker)
					{
						Drawable stickerDrawable = HikeMessengerApp.getLruCache().getSticker(stickerImage.getPath());
						if (stickerDrawable != null)
						{
							holder.stickerImage.setVisibility(View.VISIBLE);
							// largeStickerLoader.loadImage(stickerImage.getPath(), holder.stickerImage, isListFlinging);
							holder.stickerImage.setImageDrawable(stickerDrawable);
							// holder.stickerImage.setImageDrawable(IconCacheManager
							// .getInstance().getSticker(context,
							// stickerImage.getPath()));
						}
						else
						{
							holder.stickerLoader.setVisibility(View.VISIBLE);
							holder.stickerPlaceholder.setBackgroundResource(R.drawable.bg_sticker_placeholder);
						}
					}
					else
					{
						holder.stickerLoader.setVisibility(View.VISIBLE);
						holder.stickerPlaceholder.setBackgroundResource(R.drawable.bg_sticker_placeholder);

						/*
						 * Download the sticker if not already downloading.
						 */
						if (!downloadingSticker)
						{
							DownloadSingleStickerTask downloadSingleStickerTask = new DownloadSingleStickerTask(context, categoryId, stickerId);
							StickerManager.getInstance().insertTask(key, downloadSingleStickerTask);
							Utils.executeFtResultAsyncTask(downloadSingleStickerTask);
						}
					}
				}
			}
			else
			{
				holder.messageTextView.setVisibility(View.VISIBLE);
				holder.messageContainer.setVisibility(View.VISIBLE);

				CharSequence markedUp = convMessage.getMessage();
				// Fix for bug where if a participant leaves the group chat, the
				// participant's name is never shown
				setGroupParticipantName(convMessage, holder.participantDetailsFT, holder.participantNameFT, holder.participantNameFTUnsaved, firstMessageFromParticipant);

				SmileyParser smileyParser = SmileyParser.getInstance();
				markedUp = smileyParser.addSmileySpans(markedUp, false);
				holder.messageTextView.setText(markedUp);
				Linkify.addLinks(holder.messageTextView, Linkify.ALL);
				Linkify.addLinks(holder.messageTextView, Utils.shortCodeRegex, "tel:");
			}

			if (!convMessage.isSent())
			{
				if (firstMessageFromParticipant)
				{
					holder.image.setVisibility(View.VISIBLE);
					setAvatar(convMessage.getGroupParticipantMsisdn(), holder.image);
					holder.avatarContainer.setVisibility(View.VISIBLE);
				}
				else
				{
					holder.avatarContainer.setVisibility(isGroupChat ? View.INVISIBLE : View.GONE);
				}
			}

			if (convMessage.isStickerMessage() || (metadata != null && metadata.isPokeMessage()))
			{
				setNewSDR(position, holder.extMessageTime, holder.extMessageStatus, true, holder.extMessageTimeStatus, holder.messageInfo, holder.bubbleContainer, holder.sending);
			}
			else
			{
				setNewSDR(position, holder.messageTime, holder.messageStatus, false, null, holder.messageInfo, holder.bubbleContainer, holder.sending);

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
						showSdrTip(holder.sdrFtueTip);
					}
					else
					{
						holder.sdrFtueTip.setVisibility(View.GONE);
					}
				}
			}

			// if(isDefaultTheme)
			// {
			// if(convMessage.isStickerMessage() || (metadata != null && metadata.isPokeMessage()))
			// {
			// setNewSDR(position, holder.extMessageTime, holder.extMessageStatus, false, holder.extMessageTimeStatus, holder.messageInfo);
			// }
			// else
			// {
			// setNewSDR(position, holder.messageTime, holder.messageStatus, true, null, holder.messageInfo);
			// }
			// }
			// else
			// {
			// setSDRAndTimestamp(position, holder.messageInfo, holder.sending, holder.bubbleContainer);
			// }
		}
		else if (viewType == ViewType.FILE_TRANSFER_SEND || viewType == ViewType.FILE_TRANSFER_RECEIVE)
		{
			FileSavedState fss = null;
			final HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			HikeFileType hikeFileType = hikeFile.getHikeFileType();
			File file = hikeFile.getFile();
			if (convMessage.isSent())
			{
				fss = FileTransferManager.getInstance(context).getUploadFileState(convMessage.getMsgID(), file);
			}
			else
			{
				fss = FileTransferManager.getInstance(context).getDownloadFileState(convMessage.getMsgID(), file);
			}
			Logger.d(getClass().getSimpleName(), "FT msdId: " + convMessage.getMsgID());
			Logger.d(getClass().getSimpleName(), "FT state: " + fss.getFTState().toString());

			if (hikeFile.getHikeFileType() != HikeFileType.AUDIO_RECORDING)
				holder.messageContainer.setVisibility(View.VISIBLE);

			// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Setting Thumbnail
			holder.fileThumb.setBackgroundResource(0);
			holder.fileThumb.setImageResource(0);
			boolean showThumbnail = false;
			Drawable thumbnail = null;
			if (hikeFileType == HikeFileType.CONTACT)
			{
				createFileThumb(holder.fileThumb);
				holder.fileThumb.setImageResource(R.drawable.ic_default_contact);
				holder.fileName.setText(hikeFile.getDisplayName());
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
					holder.fileSizeExt.setText(phone);
					holder.fileSizeExt.setVisibility(View.VISIBLE);
				}
				else if (!TextUtils.isEmpty(email))
				{
					holder.fileSizeExt.setText(email);
					holder.fileSizeExt.setVisibility(View.VISIBLE);
				}

				holder.fileThumb.setVisibility(View.VISIBLE);
				holder.fileName.setVisibility(View.VISIBLE);
				holder.fileDetails.setVisibility(View.VISIBLE);
			}
			else if (hikeFileType == HikeFileType.AUDIO)
			{
				createFileThumb(holder.fileThumb);
				holder.fileName.setText(hikeFile.getFileName());
				//holder.fileSizeExt.setText(dataDisplay(hikeFile.getFileSize()));
				if(convMessage.isSent() && ((int) hikeFile.getFile().length() > 0))
				{
					holder.fileSizeExt.setText(dataDisplay((int) hikeFile.getFile().length()));
				}
				else if (hikeFile.getFileSize() > 0)
				{
					holder.fileSizeExt.setText(dataDisplay(hikeFile.getFileSize()));
				}
				else
				{
					holder.fileSizeExt.setText("");
				}
				String ext =  Utils.getFileExtension(hikeFile.getFileName()).toUpperCase();
				if(!TextUtils.isEmpty(ext))
				{
					holder.fileExtension.setText(ext);
				}
				else
				{
					holder.fileExtension.setText("?");
				}
				
				holder.fileThumb.setVisibility(View.VISIBLE);
				holder.fileName.setVisibility(View.VISIBLE);
				holder.fileSizeExt.setVisibility(View.VISIBLE);
				holder.fileExtension.setVisibility(View.VISIBLE);
				holder.fileDetails.setVisibility(View.VISIBLE);
			}
			else if (hikeFileType == HikeFileType.AUDIO_RECORDING)
			{
				holder.stickerPlaceholder.setVisibility(View.VISIBLE);
				holder.stickerPlaceholder.setBackgroundResource(0);
				setGroupParticipantName(convMessage, holder.stickerParticipantDetails, holder.stickerParticipantName, holder.stickerParticipantNameUnsaved, firstMessageFromParticipant);
				holder.stickerParticipantName.setTextColor(context.getResources().getColor(chatTheme.offlineMsgTextColor()));
				if (holder.stickerParticipantNameUnsaved != null)
				{
					holder.stickerParticipantNameUnsaved.setTextColor(context.getResources().getColor(chatTheme.offlineMsgTextColor()));
				}
				
				ShapeDrawable circle = new ShapeDrawable(new OvalShape());
				circle.setIntrinsicHeight((int) (36 * Utils.densityMultiplier));
				circle.setIntrinsicWidth((int) (36 * Utils.densityMultiplier));
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

				holder.recPlaceholder.setBackgroundDrawable(circle);
				holder.recPlaceholder.setVisibility(View.VISIBLE);
			}
			else if (hikeFileType == HikeFileType.VIDEO)
			{
				showThumbnail = ((convMessage.isSent()) || (conversation instanceof GroupConversation) || (!TextUtils.isEmpty(conversation.getContactName())) || (hikeFile
						.wasFileDownloaded())) && (hikeFile.getThumbnail() != null);
				thumbnail = null;
				if (hikeFile.getThumbnail() == null && !TextUtils.isEmpty(hikeFile.getFileKey()))
				{
					thumbnail = HikeMessengerApp.getLruCache().getFileIconFromCache(hikeFile.getFileKey());
					if (thumbnail != null)
					{
						showThumbnail = true;
					}
				}
				else
				{
					thumbnail = hikeFile.getThumbnail();
				}
				if ((TextUtils.isEmpty(conversation.getContactName())) && (!hikeFile.wasFileDownloaded()) && !(conversation instanceof GroupConversation)
						&& (!convMessage.isSent()))
					showThumbnail = false;

				if(thumbnail != null)
				{
					if (showThumbnail)
					{
						holder.fileThumb.setBackgroundDrawable(thumbnail);
					}
					else
					{
						createMediaThumb(holder.fileThumb);
					}
	
					if(convMessage.isSent() && ((int) hikeFile.getFile().length() > 0))
					{
						holder.fileSize.setText(dataDisplay((int) hikeFile.getFile().length()));
					}
					else if (hikeFile.getFileSize() > 0)
					{
						holder.fileSize.setText(dataDisplay(hikeFile.getFileSize()));
					}
					else
					{
						holder.fileSize.setText("");
					}
					
					holder.fileSize.setVisibility(View.VISIBLE);
					holder.messageSize.setVisibility(View.VISIBLE);
					holder.fileThumb.setVisibility(View.VISIBLE);
					holder.filmstripLeft.setVisibility(View.VISIBLE);
					holder.filmstripRight.setVisibility(View.VISIBLE);
				}
				else
				{
					createFileThumb(holder.fileThumb);
					holder.fileName.setText(hikeFile.getFileName());
					if(convMessage.isSent() && ((int) hikeFile.getFile().length() > 0))
					{
						holder.fileSizeExt.setText(dataDisplay((int) hikeFile.getFile().length()));
					}
					else if (hikeFile.getFileSize() > 0)
					{
						holder.fileSizeExt.setText(dataDisplay(hikeFile.getFileSize()));
					}
					else
					{
						holder.fileSizeExt.setText("");
					}
					String ext =  Utils.getFileExtension(hikeFile.getFileName()).toUpperCase();
					if(!TextUtils.isEmpty(ext))
					{
						holder.fileExtension.setText(ext);
					}
					else
					{
						holder.fileExtension.setText("?");
					}
					
					holder.fileThumb.setVisibility(View.VISIBLE);
					holder.fileName.setVisibility(View.VISIBLE);
					holder.fileSizeExt.setVisibility(View.VISIBLE);
					holder.fileExtension.setVisibility(View.VISIBLE);
					holder.fileDetails.setVisibility(View.VISIBLE);
				}
			}
			else if (hikeFileType == HikeFileType.IMAGE || hikeFileType == HikeFileType.LOCATION)
			{

				showThumbnail = ((convMessage.isSent()) || (conversation instanceof GroupConversation) || (!TextUtils.isEmpty(conversation.getContactName())) || (hikeFile
						.wasFileDownloaded())) && (hikeFile.getThumbnail() != null);
				thumbnail = null;
				if (hikeFile.getThumbnail() == null && !TextUtils.isEmpty(hikeFile.getFileKey()))
				{
					thumbnail = HikeMessengerApp.getLruCache().getFileIconFromCache(hikeFile.getFileKey());
					if (thumbnail != null)
					{
						showThumbnail = true;
					}
				}
				else
				{
					thumbnail = hikeFile.getThumbnail();
				}
				if ((TextUtils.isEmpty(conversation.getContactName())) && (!hikeFile.wasFileDownloaded()) && !(conversation instanceof GroupConversation)
						&& (!convMessage.isSent()))
					showThumbnail = false;

				if(thumbnail != null || hikeFileType == HikeFileType.LOCATION)
				{
				
				if (showThumbnail)
				{
					holder.fileThumb.setBackgroundDrawable(thumbnail);
					holder.fileThumb.setVisibility(View.VISIBLE);
				}
				else if (hikeFileType == HikeFileType.IMAGE)
				{
					createMediaThumb(holder.fileThumb);
				}
				else
				{
					createMediaThumb(holder.fileThumb);
					holder.fileThumb.setImageResource(R.drawable.ic_default_location);
					holder.fileThumb.setScaleType(ScaleType.CENTER);
				}
				holder.fileThumb.setVisibility(View.VISIBLE);
				}
				else
				{
					createFileThumb(holder.fileThumb);
					holder.fileName.setText(hikeFile.getFileName());
					if(convMessage.isSent() && ((int) hikeFile.getFile().length() > 0))
					{
						holder.fileSizeExt.setText(dataDisplay((int) hikeFile.getFile().length()));
					}
					else if (hikeFile.getFileSize() > 0)
					{
						holder.fileSizeExt.setText(dataDisplay(hikeFile.getFileSize()));
					}
					else
					{
						holder.fileSizeExt.setText("");
					}
					String ext =  Utils.getFileExtension(hikeFile.getFileName()).toUpperCase();
					if(!TextUtils.isEmpty(ext))
					{
						holder.fileExtension.setText(ext);
					}
					else
					{
						holder.fileExtension.setText("?");
					}
					
					holder.fileThumb.setVisibility(View.VISIBLE);
					holder.fileName.setVisibility(View.VISIBLE);
					holder.fileSizeExt.setVisibility(View.VISIBLE);
					holder.fileExtension.setVisibility(View.VISIBLE);
					holder.fileDetails.setVisibility(View.VISIBLE);
				}
			}
			else if (hikeFileType == HikeFileType.OTHER)
			{
				createFileThumb(holder.fileThumb);
				holder.fileName.setText(hikeFile.getFileName());
				if(convMessage.isSent() && ((int) hikeFile.getFile().length() > 0))
				{
					holder.fileSizeExt.setText(dataDisplay((int) hikeFile.getFile().length()));
				}
				else if (hikeFile.getFileSize() > 0)
				{
					holder.fileSizeExt.setText(dataDisplay(hikeFile.getFileSize()));
				}
				else
				{
					holder.fileSizeExt.setText("");
				}
				String ext =  Utils.getFileExtension(hikeFile.getFileName()).toUpperCase();
				if(!TextUtils.isEmpty(ext))
				{
					holder.fileExtension.setText(ext);
				}
				else
				{
					holder.fileExtension.setText("?");
				}
				
				holder.fileThumb.setVisibility(View.VISIBLE);
				holder.fileName.setVisibility(View.VISIBLE);
				holder.fileSizeExt.setVisibility(View.VISIBLE);
				holder.fileExtension.setVisibility(View.VISIBLE);
				holder.fileDetails.setVisibility(View.VISIBLE);
			}

			// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Setting Thumbnail Dimensions
			RelativeLayout.LayoutParams fileThumbParams = (RelativeLayout.LayoutParams) holder.fileThumb.getLayoutParams();
			if (hikeFileType == HikeFileType.AUDIO_RECORDING)
			{

			}
			else if ((!showThumbnail) && (hikeFileType == HikeFileType.LOCATION))
			{
				holder.fileThumb.setScaleType(ScaleType.CENTER_INSIDE);
			}
			else if ((!showThumbnail)
					&& (hikeFileType == HikeFileType.AUDIO || hikeFileType == HikeFileType.IMAGE || hikeFileType == HikeFileType.VIDEO
					|| hikeFileType == HikeFileType.OTHER || hikeFileType == HikeFileType.CONTACT || hikeFileType == HikeFileType.LOCATION))
			{
				holder.fileThumb.setScaleType(ScaleType.CENTER);
			}
			else if (showThumbnail && thumbnail != null)
			{
				holder.fileThumb.setScaleType(ScaleType.CENTER);
				fileThumbParams.height = (int) (150 * Utils.densityMultiplier);
				fileThumbParams.width = (int) ((thumbnail.getIntrinsicWidth() * fileThumbParams.height) / thumbnail.getIntrinsicHeight());
				/*
				 * fixed the bug when image thumbnail is very big. By specifying a maximum width for the thumbnail so that download button can also fit to the screen.
				 */
				int maxWidth = (int) (250 * Utils.densityMultiplier);
				fileThumbParams.width = Math.min(fileThumbParams.width, maxWidth);
				int minWidth = (int) (119 * Utils.densityMultiplier);
				fileThumbParams.width = Math.max(fileThumbParams.width, minWidth);

				if (fileThumbParams.width == minWidth)
				{
					fileThumbParams.height = ((thumbnail.getIntrinsicHeight() * minWidth) / thumbnail.getIntrinsicWidth());
				}
			}
			else
			{
				holder.fileThumb.setScaleType(ScaleType.CENTER);
				fileThumbParams.height = LayoutParams.WRAP_CONTENT;
				fileThumbParams.width = LayoutParams.WRAP_CONTENT;
			}
			holder.fileThumb.setScaleType(ScaleType.CENTER);
			holder.fileThumb.setLayoutParams(fileThumbParams);

			// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Setting and Showing MessageTextView
			if (hikeFileType == HikeFileType.AUDIO_RECORDING)
			{
				// Utils.setupFormattedTime(holder.recDuration, hikeFile.getRecordingDuration());
				// holder.recDuration.setVisibility(View.VISIBLE);
			}
			else if (hikeFileType == HikeFileType.CONTACT)
			{
				// holder.messageTextView.setText(hikeFile.getFileName());
				// holder.messageTextView.setVisibility(View.VISIBLE);
			}
			else
			{
				holder.messageTextView.setText(hikeFile.getFileName());
			}

			// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Setting up the FileButton
			if (hikeFileType == HikeFileType.AUDIO_RECORDING)
			{
				if (fss.getFTState() == FTState.COMPLETED || (convMessage.isSent() && !TextUtils.isEmpty(hikeFile.getFileKey())))
				{
					// holder.mediaAction.setBackgroundResource(0);
					// holder.mediaAction.setImageResource(0);
					// holder.mediaAction.setScaleType(ScaleType.CENTER_INSIDE);
					holder.recAction.setBackgroundResource(0);
					holder.recAction.setImageResource(0);
					holder.recAction.setScaleType(ScaleType.CENTER_INSIDE);
					if (hikeFile.getFileKey().equals(voiceMessagePlayer.getFileKey()))
					{
						holder.recAction.setVisibility(View.VISIBLE);
						if (voiceMessagePlayer.getPlayerState() == VoiceMessagePlayerState.PLAYING)
						{
							holder.recAction.setImageResource(R.drawable.ic_pause_rec);
						}
						else
						{
							holder.recAction.setImageResource(R.drawable.ic_mic);
						}
						// holder.mediaAction.setBackgroundResource(R.drawable.bg_red_btn_selector);
						holder.recDuration.setTag(hikeFile.getFileKey());
						voiceMessagePlayer.setDurationTxt(holder.recDuration, holder.recProgress);
						holder.recDuration.setVisibility(View.VISIBLE);
						holder.recProgress.setVisibility(View.VISIBLE);
					}
					else
					{
						if ((!convMessage.isSent()) || (!TextUtils.isEmpty(hikeFile.getFileKey())))
						{
							// setFileButtonResource(holder.mediaAction, convMessage, hikeFile);
							// holder.mediaAction.setBackgroundResource(R.drawable.bg_red_btn_selector);
							// holder.recAction.setImageResource(R.drawable.ic_open_received_file);
							holder.recAction.setImageResource(R.drawable.ic_mic);
							holder.recAction.setVisibility(View.VISIBLE);
						}
						Utils.setupFormattedTime(holder.recDuration, hikeFile.getRecordingDuration());
						holder.recDuration.setVisibility(View.VISIBLE);
						holder.recProgress.setVisibility(View.INVISIBLE);
					}
				}
				else
				{
					holder.recAction.setImageResource(R.drawable.ic_mic);
					holder.recAction.setVisibility(View.VISIBLE);
				}
			}

			// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Setting Margin View
			// if (holder.marginView != null)
			// {
			// holder.marginView.setVisibility(hikeFile.getThumbnail() == null && !showThumbnail ? View.VISIBLE : View.GONE);
			// }
			setGroupParticipantName(convMessage, holder.participantDetailsFT, holder.participantNameFT, holder.participantNameFTUnsaved, firstMessageFromParticipant);

			// if (!convMessage.isSent())
			// {
			// if (firstMessageFromParticipant)
			// {
			// holder.participantNameFT.setText(((GroupConversation) conversation).getGroupParticipantFirstName(convMessage.getGroupParticipantMsisdn()));
			// holder.participantNameFT.setVisibility(View.VISIBLE);
			// }
			// else
			// {
			// holder.participantNameFT.setVisibility(View.GONE);
			// }
			// }
			holder.messageContainer.setTag(convMessage);
			holder.messageContainer.setOnClickListener(this);
			holder.messageContainer.setOnLongClickListener(this);

			if(holder.circularProgressBgExt != null)
			{
				holder.circularProgressBgExt.setTag(convMessage);
				holder.circularProgressBgExt.setOnClickListener(this);
				holder.circularProgressBgExt.setOnLongClickListener(this);
			}
			
			if(holder.recPlaceholder != null)
			{
				holder.recPlaceholder.setTag(convMessage);
				holder.recPlaceholder.setOnClickListener(this);
				holder.recPlaceholder.setOnLongClickListener(this);
			}

			// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Overlay Visibility
			// Tap overlay will be there only in case of image and video.
			// if ((hikeFile.getHikeFileType() != HikeFileType.CONTACT) && (holder.fileThumb.getVisibility() == View.VISIBLE))
			// {
			// if(hikeFile.getHikeFileType() == HikeFileType.LOCATION)
			// {
			// if(!convMessage.isSent() || !TextUtils.isEmpty(hikeFile.getFileKey()))
			// {
			// holder.overlayBg.getLayoutParams().height = (int) (32 * Utils.densityMultiplier);
			// holder.overlayBg.setVisibility(View.VISIBLE);
			// }
			// }
			// else if (hikeFile.getHikeFileType() == HikeFileType.AUDIO_RECORDING)
			// {
			// if (fss.getFTState() == FTState.COMPLETED || (convMessage.isSent() && !TextUtils.isEmpty(hikeFile.getFileKey())))
			// {
			//
			// }
			// else
			// {
			// holder.overlayBg.getLayoutParams().height = (int) (32 * Utils.densityMultiplier);
			// holder.overlayBg.setVisibility(View.VISIBLE);
			// }
			// }
			// else
			// {
			// if(fss.getFTState() == FTState.COMPLETED || (convMessage.isSent() && !TextUtils.isEmpty(hikeFile.getFileKey())))
			// {
			// holder.overlayBg.getLayoutParams().height = (int) (32 * Utils.densityMultiplier);
			// holder.overlayBg.setVisibility(View.VISIBLE);
			// }
			// else
			// {
			// holder.overlayBg.getLayoutParams().height = (int) (40 * Utils.densityMultiplier);
			// }
			// switch (fss.getFTState())
			// {
			// case NOT_STARTED:
			// if (!convMessage.isSent())
			// {
			// holder.overlayBg.setVisibility(View.VISIBLE);
			// }
			// break;
			// case INITIALIZED:
			// case IN_PROGRESS:
			// case PAUSING:
			// case PAUSED:
			// case ERROR:
			// holder.overlayBg.setVisibility(View.VISIBLE);
			// break;
			// default:
			// break;
			// }
			// }
			// }

			// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Overlay status
			if (hikeFileType == HikeFileType.LOCATION || hikeFileType == HikeFileType.CONTACT)
			{
				if((!convMessage.isSent()) || (convMessage.isSent() && !TextUtils.isEmpty(hikeFile.getFileKey())))
				{
					
				}
				else if (FileTransferManager.getInstance(context).isFileTaskExist(convMessage.getMsgID()))
				{
					
				}
				else
				{
					if (hikeFileType == HikeFileType.LOCATION)
					{
						holder.ftAction.setImageResource(R.drawable.ic_retry_image_video);
						holder.ftAction.setVisibility(View.VISIBLE);
						holder.ftAction.setScaleType(ScaleType.CENTER);
						holder.circularProgressBg.setVisibility(View.VISIBLE);
					}
					else
					{
						holder.ftActionExt.setImageResource(R.drawable.ic_retry_other);
						holder.ftActionExt.setVisibility(View.VISIBLE);
						holder.ftActionExt.setScaleType(ScaleType.CENTER);
						holder.circularProgressBgExt.setVisibility(View.VISIBLE);
					}
				}
			}
			else if (((hikeFileType == HikeFileType.IMAGE) || (hikeFileType == HikeFileType.VIDEO)) && (thumbnail != null))
			{
				ImageView ftAction;
				View circularProgressBg;
				ftAction = holder.ftAction;
				circularProgressBg = holder.circularProgressBg;
				switch (fss.getFTState())
				{
				case NOT_STARTED:
					if (!convMessage.isSent())
					{
						ftAction.setImageResource(R.drawable.ic_download_image_video);
						ftAction.setVisibility(View.VISIBLE);
						circularProgressBg.setVisibility(View.VISIBLE);
					}
					else if (TextUtils.isEmpty(hikeFile.getFileKey()))
					{
						ftAction.setImageResource(R.drawable.ic_retry_image_video);
						ftAction.setVisibility(View.VISIBLE);
						circularProgressBg.setVisibility(View.VISIBLE);
					}
					break;
				case INITIALIZED:
					break;
				case IN_PROGRESS:
					ftAction.setImageResource(R.drawable.ic_pause_image_video);
					ftAction.setVisibility(View.VISIBLE);
					circularProgressBg.setVisibility(View.VISIBLE);
					break;
				case PAUSING:
					ftAction.setImageResource(0);
					ftAction.setVisibility(View.VISIBLE);
					circularProgressBg.setVisibility(View.VISIBLE);
					break;
				case PAUSED:
					ftAction.setImageResource(R.drawable.ic_retry_image_video);
					ftAction.setVisibility(View.VISIBLE);
					circularProgressBg.setVisibility(View.VISIBLE);
					break;
				case ERROR:
					ftAction.setImageResource(R.drawable.ic_retry_image_video);
					ftAction.setVisibility(View.VISIBLE);
					circularProgressBg.setVisibility(View.VISIBLE);
					break;
				default:
					break;
				}
				ftAction.setScaleType(ScaleType.CENTER);
			}
			else if (hikeFileType != HikeFileType.AUDIO_RECORDING)
			{
				switch (fss.getFTState())
				{
				case NOT_STARTED:
					if (!convMessage.isSent())
					{
						holder.ftActionExt.setImageResource(R.drawable.ic_download_other);
						holder.ftActionExt.setVisibility(View.VISIBLE);
						holder.circularProgressBgExt.setVisibility(View.VISIBLE);
					}
					else if (TextUtils.isEmpty(hikeFile.getFileKey()))
					{
						holder.ftActionExt.setImageResource(R.drawable.ic_retry_other);
						holder.ftActionExt.setVisibility(View.VISIBLE);
						holder.circularProgressBgExt.setVisibility(View.VISIBLE);
					}
					break;
				case INITIALIZED:
					break;
				case IN_PROGRESS:
					holder.ftActionExt.setImageResource(R.drawable.ic_pause_other);
					holder.ftActionExt.setVisibility(View.VISIBLE);
					holder.circularProgressBgExt.setVisibility(View.VISIBLE);
					break;
				case PAUSING:
					holder.ftActionExt.setImageResource(0);
					holder.ftActionExt.setVisibility(View.VISIBLE);
					holder.circularProgressBgExt.setVisibility(View.VISIBLE);
					break;
				case PAUSED:
					holder.ftActionExt.setImageResource(R.drawable.ic_retry_other);
					holder.ftActionExt.setVisibility(View.VISIBLE);
					holder.circularProgressBgExt.setVisibility(View.VISIBLE);
					break;
				case ERROR:
					holder.ftActionExt.setImageResource(R.drawable.ic_retry_other);
					holder.ftActionExt.setVisibility(View.VISIBLE);
					holder.circularProgressBgExt.setVisibility(View.VISIBLE);
					break;
				default:
					break;
				}
				holder.ftActionExt.setScaleType(ScaleType.CENTER);
			}
			// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Setting up Overlay Contents
			
			if (hikeFileType == HikeFileType.LOCATION || hikeFileType == HikeFileType.CONTACT)
			{
				if(hikeFile.wasFileDownloaded() || (convMessage.isSent() && !TextUtils.isEmpty(hikeFile.getFileKey())))
				{
					
				}
				else if (FileTransferManager.getInstance(context).isFileTaskExist(convMessage.getMsgID()))
				{
					showTransferInitialization(holder, hikeFile, thumbnail);
				}
				else
				{
					
				}
			}
			//else if ((hikeFileType == HikeFileType.IMAGE) || (hikeFileType == HikeFileType.VIDEO))
			else
			{
				if (convMessage.isSent()) // File is being sent
				{
					Logger.d(getClass().getSimpleName(), "updating upload progress : " + fss.getFTState().toString() + "fileKey: " + hikeFile.getFileKey().toString());
					switch (fss.getFTState())
					{
					case COMPLETED:
						break;
					case NOT_STARTED:
					case CANCELLED:
						break;
					case INITIALIZED:
						showTransferInitialization(holder, hikeFile, thumbnail);
						// setFileTypeText(holder.fileType, hikeFile.getHikeFileType());
						// holder.fileType.setVisibility(View.VISIBLE);
						break;
					case ERROR:
						// Logger.d(getClass().getSimpleName(), "error display");
						// holder.image.setVisibility(View.VISIBLE);
						// holder.image.setImageResource(getDownloadFailedResIcon());
						showTransferProgress(holder, fss, convMessage.getMsgID(), hikeFile, convMessage.isSent(), thumbnail);
						break;
					case PAUSING:
						showTransferInitialization(holder, hikeFile, thumbnail);
						break;
					case PAUSED:
						showTransferProgress(holder, fss, convMessage.getMsgID(), hikeFile, convMessage.isSent(), thumbnail);
						break;
					case IN_PROGRESS:
						showTransferProgress(holder, fss, convMessage.getMsgID(), hikeFile, convMessage.isSent(), thumbnail);
						break;
					default:
					}
				}
				else
				// File is being received
				{
					Logger.d(getClass().getSimpleName(), "setting progress visibility : " + fss.getFTState().toString());
					switch (fss.getFTState())
					{
					case INITIALIZED:
						showTransferInitialization(holder, hikeFile, thumbnail);
						break;
					case PAUSING:
						showTransferInitialization(holder, hikeFile, thumbnail);
						break;
					case PAUSED:
					case ERROR:
						showTransferProgress(holder, fss, convMessage.getMsgID(), hikeFile, convMessage.isSent(), thumbnail);
						break;
					case IN_PROGRESS:
						showTransferProgress(holder, fss, convMessage.getMsgID(), hikeFile, convMessage.isSent(), thumbnail);
						break;
					case NOT_STARTED:
					case CANCELLED:
					case COMPLETED:
					default:
						break;

					}
				}
			}

			// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Message status and time stamp
			if (convMessage.isSent())
			{
				// if (!TextUtils.isEmpty(hikeFile.getFileKey()))
				// {
				if(hikeFile.getHikeFileType() == HikeFileType.LOCATION)
				{
					setNewSDR(position, holder.ftMessageTime, holder.ftMessageStatus, true, holder.ftMessageTimeStatus, holder.messageInfo, holder.bubbleContainer, holder.sending);
				}
				else if ((hikeFile.getHikeFileType() == HikeFileType.VIDEO) || (hikeFile.getHikeFileType() == HikeFileType.IMAGE))
				{
					if(thumbnail != null)
					{
						setNewSDR(position, holder.ftMessageTime, holder.ftMessageStatus, true, holder.ftMessageTimeStatus, holder.messageInfo, holder.bubbleContainer, holder.sending);
					}
					else
					{
						setNewSDR(position, holder.messageTime, holder.messageStatus, false, null, holder.messageInfo, holder.bubbleContainer, holder.sending);
					}
				}
				else if ((hikeFile.getHikeFileType() == HikeFileType.AUDIO) || (hikeFile.getHikeFileType() == HikeFileType.CONTACT)
						|| (hikeFile.getHikeFileType() == HikeFileType.OTHER))
				{
					// setFileTypeText(holder.fileType, hikeFile);
					// holder.fileType.setVisibility(View.VISIBLE);
					setNewSDR(position, holder.messageTime, holder.messageStatus, false, null, holder.messageInfo, holder.bubbleContainer, holder.sending);
				}
				else if (hikeFile.getHikeFileType() == HikeFileType.AUDIO_RECORDING)
				{
					setNewSDR(position, holder.extMessageTime, holder.extMessageStatus, true, holder.extMessageTimeStatus, holder.messageInfo, holder.bubbleContainer,
							holder.sending);
				}
				else
				{
					setNewSDR(position, holder.intMessageTime, holder.intMessageStatus, false, holder.intMessageTimeStatus, holder.messageInfo, holder.bubbleContainer,
							holder.sending);
				}
			}
			else
			{
				if (firstMessageFromParticipant)
				{
					holder.image.setVisibility(View.VISIBLE);
					setAvatar(convMessage.getGroupParticipantMsisdn(), holder.image);
					holder.avatarContainer.setVisibility(View.VISIBLE);
				}
				else
				{
					holder.avatarContainer.setVisibility(isGroupChat ? View.INVISIBLE : View.GONE);
				}
				if (hikeFile.getHikeFileType() == HikeFileType.LOCATION)
				{
					setNewSDR(position, holder.ftMessageTime, holder.ftMessageStatus, true, holder.ftMessageTimeStatus, holder.messageInfo, holder.bubbleContainer, holder.sending);
				}
				else if ((hikeFile.getHikeFileType() == HikeFileType.VIDEO) || (hikeFile.getHikeFileType() == HikeFileType.IMAGE))
				{
					if(thumbnail != null)
					{
						setNewSDR(position, holder.ftMessageTime, holder.ftMessageStatus, true, holder.ftMessageTimeStatus, holder.messageInfo, holder.bubbleContainer, holder.sending);
					}
					else
					{
						setNewSDR(position, holder.messageTime, holder.messageStatus, false, null, holder.messageInfo, holder.bubbleContainer, holder.sending);
					}
				}
				else if ((hikeFile.getHikeFileType() == HikeFileType.AUDIO) || (hikeFile.getHikeFileType() == HikeFileType.CONTACT)
						|| (hikeFile.getHikeFileType() == HikeFileType.OTHER))
				{
					// if(fss.getFTState() == FTState.COMPLETED)
					// {
					setNewSDR(position, holder.messageTime, holder.messageStatus, false, null, holder.messageInfo, holder.bubbleContainer, holder.sending);
					// setFileTypeText(holder.fileType, hikeFile);
					// holder.fileType.setVisibility(View.VISIBLE);
					// }
				}
				else if (hikeFile.getHikeFileType() == HikeFileType.AUDIO_RECORDING)
				{
					setNewSDR(position, holder.extMessageTime, holder.extMessageStatus, true, holder.extMessageTimeStatus, holder.messageInfo, holder.bubbleContainer,
							holder.sending);
				}
				else
				{
					setNewSDR(position, holder.intMessageTime, holder.intMessageStatus, false, holder.intMessageTimeStatus, holder.messageInfo, holder.bubbleContainer,
							holder.sending);
				}
			}
		} // End of File Transfer Message
		else if (viewType == ViewType.STATUS_MESSAGE)
		{
			holder.image.setVisibility(View.VISIBLE);
			holder.avatarFrame.setVisibility(View.VISIBLE);
			holder.messageTextView.setVisibility(View.VISIBLE);
			holder.dayTextView.setVisibility(View.VISIBLE);
			holder.container.setVisibility(View.VISIBLE);
			holder.messageInfo.setVisibility(View.VISIBLE);

			if (isDefaultTheme)
			{
				holder.dayTextView.setTextColor(context.getResources().getColor(R.color.list_item_subtext));
				holder.messageInfo.setTextColor(context.getResources().getColor(R.color.timestampcolor));
				holder.messageTextView.setTextColor(context.getResources().getColor(R.color.list_item_header));
			}
			else
			{
				holder.dayTextView.setTextColor(context.getResources().getColor(R.color.white));
				holder.messageInfo.setTextColor(context.getResources().getColor(R.color.white));
				holder.messageTextView.setTextColor(context.getResources().getColor(R.color.white));
			}
			holder.container.setBackgroundResource(chatTheme.inLineUpdateBGResId());

			StatusMessage statusMessage = convMessage.getMetadata().getStatusMessage();

			holder.dayTextView.setText(context.getString(R.string.xyz_posted_update, Utils.getFirstName(conversation.getLabel())));

			holder.messageInfo.setText(statusMessage.getTimestampFormatted(true, context));

			if (statusMessage.getStatusMessageType() == StatusMessageType.TEXT)
			{
				SmileyParser smileyParser = SmileyParser.getInstance();
				holder.messageTextView.setText(smileyParser.addSmileySpans(statusMessage.getText(), true));
				Linkify.addLinks(holder.messageTextView, Linkify.ALL);

			}
			else if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
			{
				holder.messageTextView.setText(R.string.changed_profile);
			}

			if (statusMessage.hasMood())
			{
				holder.image.setBackgroundDrawable(null);
				holder.image.setImageResource(EmoticonConstants.moodMapping.get(statusMessage.getMoodId()));
				holder.avatarFrame.setVisibility(View.GONE);
			}
			else
			{
				setAvatar(conversation.getMsisdn(), holder.image);
				holder.avatarFrame.setVisibility(View.VISIBLE);
			}

			holder.container.setTag(convMessage);
			if (!isActionModeOn)
			{
				holder.container.setEnabled(true);
				holder.container.setOnClickListener(this);
			}
			else
			{
				holder.container.setEnabled(false);
			}

			boolean showTip = false;
			boolean shownStatusTip = preferences.getBoolean(HikeMessengerApp.SHOWN_STATUS_TIP, false);

			if (!shownStatusTip)
			{
				if (chatThread.tipView == null)
				{
					showTip = true;
				}
				else
				{
					TipType tipType = (TipType) chatThread.tipView.getTag();
					if (tipType == TipType.STATUS && statusMessage.getMappedId().equals(statusIdForTip))
					{
						showTip = true;
					}
				}
			}

			if (showTip)
			{
				chatThread.tipView = v.findViewById(R.id.status_tip);
				statusIdForTip = statusMessage.getMappedId();
				HikeTip.showTip(chatThread, TipType.STATUS, chatThread.tipView, Utils.getFirstName(conversation.getLabel()));
			}
			else
			{
				v.findViewById(R.id.status_tip).setVisibility(View.GONE);
			}

			return v;
		}

		else if (viewType == ViewType.PARTICIPANT_INFO)
		{
			holder.container.setVisibility(View.VISIBLE);
			ParticipantInfoState infoState = convMessage.getParticipantInfoState();
			((ViewGroup) holder.container).removeAllViews();
			int positiveMargin = (int) (8 * Utils.densityMultiplier);
			int left = 0;
			int top = 0;
			int right = 0;
			int bottom = positiveMargin;

			int layoutRes = chatTheme.systemMessageLayoutId();

			if (infoState == ParticipantInfoState.PARTICIPANT_JOINED)
			{
				JSONArray participantInfoArray = metadata.getGcjParticipantInfo();
				TextView participantInfo = (TextView) inflater.inflate(layoutRes, null);
				String message;
				String highlight = Utils.getGroupJoinHighlightText(participantInfoArray, (GroupConversation) conversation);
				if (metadata.isNewGroup())
				{
					message = String.format(context.getString(R.string.new_group_message), highlight);
				}
				else
				{
					message = String.format(context.getString(R.string.add_to_group_message), highlight);
				}
				setTextAndIconForSystemMessages(participantInfo, Utils.getFormattedParticipantInfo(message, highlight), isDefaultTheme ? R.drawable.ic_joined_chat
						: R.drawable.ic_joined_chat_custom);
				((ViewGroup) holder.container).addView(participantInfo);
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

						((ViewGroup) holder.container).addView(mainMessage);
					}
					String participantMsisdn = metadata.getMsisdn();
					String name = ((GroupConversation) conversation).getGroupParticipantFirstName(participantMsisdn);
					message = Utils.getFormattedParticipantInfo(String.format(context.getString(R.string.left_conversation), name), name);
				}
				else
				{
					message = context.getString(R.string.group_chat_end);
				}
				setTextAndIconForSystemMessages(participantInfo, message, isDefaultTheme ? R.drawable.ic_left_chat : R.drawable.ic_left_chat_custom);

				LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				lp.setMargins(left, top, right, 0);
				participantInfo.setLayoutParams(lp);
				((ViewGroup) holder.container).addView(participantInfo);
			}
			else if (infoState == ParticipantInfoState.USER_JOIN || infoState == ParticipantInfoState.USER_OPT_IN)
			{
				String name;
				String message;
				if (conversation instanceof GroupConversation)
				{
					String participantMsisdn = metadata.getMsisdn();
					name = ((GroupConversation) conversation).getGroupParticipantFirstName(participantMsisdn);
					message = context.getString(infoState == ParticipantInfoState.USER_JOIN ? (metadata.isOldUser() ? R.string.user_back_on_hike : R.string.joined_hike_new)
							: R.string.joined_conversation, name);
				}
				else
				{
					name = Utils.getFirstName(conversation.getLabel());
					message = context.getString(infoState == ParticipantInfoState.USER_JOIN ? (metadata.isOldUser() ? R.string.user_back_on_hike : R.string.joined_hike_new)
							: R.string.optin_one_to_one, name);
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

				((ViewGroup) holder.container).addView(mainMessage);
				if (creditsMessageView != null)
				{
					((ViewGroup) holder.container).addView(creditsMessageView);
				}
			}
			else if ((infoState == ParticipantInfoState.CHANGED_GROUP_NAME) || (infoState == ParticipantInfoState.CHANGED_GROUP_IMAGE))
			{
				String msisdn = metadata.getMsisdn();
				String userMsisdn = preferences.getString(HikeMessengerApp.MSISDN_SETTING, "");

				String participantName = userMsisdn.equals(msisdn) ? context.getString(R.string.you) : ((GroupConversation) conversation).getGroupParticipantFirstName(msisdn);
				String message = String.format(context.getString(convMessage.getParticipantInfoState() == ParticipantInfoState.CHANGED_GROUP_NAME ? R.string.change_group_name
						: R.string.change_group_image), participantName);

				TextView mainMessage = (TextView) inflater.inflate(layoutRes, null);
				int icRes;
				if (infoState == ParticipantInfoState.CHANGED_GROUP_NAME)
				{
					icRes = isDefaultTheme ? R.drawable.ic_group_info : R.drawable.ic_group_info_custom;
				}
				else
				{
					icRes = isDefaultTheme ? R.drawable.ic_group_image : R.drawable.ic_group_image_custom;
				}
				setTextAndIconForSystemMessages(mainMessage, Utils.getFormattedParticipantInfo(message, participantName), icRes);

				((ViewGroup) holder.container).addView(mainMessage);
			}
			else if (infoState == ParticipantInfoState.BLOCK_INTERNATIONAL_SMS)
			{
				String info = context.getString(R.string.block_internation_sms);
				String textToHighlight = context.getString(R.string.block_internation_sms_bold_text);

				TextView mainMessage = (TextView) inflater.inflate(layoutRes, null);
				setTextAndIconForSystemMessages(mainMessage, Utils.getFormattedParticipantInfo(info, textToHighlight), isDefaultTheme ? R.drawable.ic_no_int_sms
						: R.drawable.ic_no_int_sms_custom);

				((ViewGroup) holder.container).addView(mainMessage);
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

				((ViewGroup) holder.container).addView(mainMessage);
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
						String name = conversation instanceof GroupConversation ? ((GroupConversation) conversation).getGroupParticipantFirstName(dndNumbers.optString(i)) : Utils
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

					((ViewGroup) holder.container).addView(dndMessage);
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
					name = userMsisdn.equals(msisdn) ? context.getString(R.string.you) : ((GroupConversation) conversation).getGroupParticipantFirstName(msisdn);
				}
				else
				{
					name = userMsisdn.equals(msisdn) ? context.getString(R.string.you) : Utils.getFirstName(conversation.getLabel());
				}

				String message = context.getString(R.string.chat_bg_changed, name);

				setTextAndIconForSystemMessages(mainMessage, Utils.getFormattedParticipantInfo(message, name), isDefaultTheme ? R.drawable.ic_change_theme
						: R.drawable.ic_change_theme_custom);

				((ViewGroup) holder.container).addView(mainMessage);
			}
			return v;
		}
		else if (viewType == ViewType.UNREAD_COUNT)
		{
			holder.container.setVisibility(View.VISIBLE);
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
			((ViewGroup) holder.container).removeAllViews();
			((ViewGroup) holder.container).addView(participantInfo);
			return v;
		}

		if (convMessage.isSent() && holder.messageContainer != null)
		{
			/* label outgoing hike conversations in green */
			if (chatTheme == ChatTheme.DEFAULT)
			{
				holder.messageContainer.setBackgroundResource(!convMessage.isSMS() ? R.drawable.ic_bubble_blue_selector : R.drawable.ic_bubble_green_selector);
			}
			else
			{
				holder.messageContainer.setBackgroundResource(chatTheme.bubbleResId());
			}
		}
		if (isActionModeOn)
		{
			/*
			 * This is an transparent overlay over all the message which will listen click events while action mode is on.
			 */
			holder.selectedStateOverlay.setVisibility(View.VISIBLE);
			holder.selectedStateOverlay.setOnClickListener(selectedStateOverlayClickListener);
			holder.selectedStateOverlay.setOnLongClickListener(this);

			if (isSelected(position))
			{
				/*
				 * If a message has been selected then background of selected state overlay will change to selected state color. otherwise this overlay will be transparent
				 */
				holder.selectedStateOverlay.setBackgroundColor(context.getResources().getColor(chatTheme.multiSelectBubbleColor()));
			}
			else
			{
				holder.selectedStateOverlay.setBackgroundColor(context.getResources().getColor(R.color.transparent));
			}
		}
		else
		{
			holder.selectedStateOverlay.setVisibility(View.GONE);
		}
		return v;
	}

	private void showSdrTip(View sdrFtueTip)
	{
		sdrFtueTip.setVisibility(View.VISIBLE);
		if(!sdrTipFadeInShown)
		{
			Animation anim =  AnimationUtils.loadAnimation(context, R.anim.fade_in_animation);
			anim.setDuration(1000);
			sdrFtueTip.startAnimation(anim);
			sdrTipFadeInShown= true;
		}
	}

	private void setAvatar(String msisdn, ImageView imageView)
	{
		iconLoader.loadImage(msisdn, true, imageView, true);
	}

	private int getDownloadFailedResIcon()
	{
		return isDefaultTheme ? R.drawable.ic_download_failed : R.drawable.ic_download_failed_custom;
	}

	private void setNudgeImageResource(ChatTheme chatTheme, ImageView iv, boolean isMessageSent)
	{
		iv.setImageResource(isMessageSent ? chatTheme.sentNudgeResId() : chatTheme.receivedNudgeResId());
	}

	// @GM
	// The following methods returns the user readable size when passed the bytes in size
	private String dataDisplay(int bytes)
	{
		Logger.d(getClass().getSimpleName(), "DataDisplay of bytes : " + bytes);
		if (bytes < 0)
			return ("");
		if (bytes >= (1000 * 1024))
		{
			int mb = bytes / (1024 * 1024);
			int mbPoint = bytes % (1024 * 1024);
			mbPoint /= (1024 * 102);
			return (Integer.toString(mb) + "." + Integer.toString(mbPoint) + " MB");
		}
		else if (bytes >= 1000)
		{
			int kb;
			if (bytes < 1024) // To avoid showing "1000KB"
				kb = bytes / 1000;
			else
				kb = bytes / 1024;
			return (Integer.toString(kb) + " KB");
		}
		else
			return (Integer.toString(bytes) + " B");
	}

	private void setGroupParticipantName(ConvMessage convMessage, View participantDetails, TextView participantName, TextView participantNameUnsaved,
			boolean firstMessageFromParticipant)
	{
		if (!convMessage.isSent())
		{
			if (firstMessageFromParticipant)
			{
				String number = null;
				String name = ((GroupConversation) conversation).getGroupParticipantFirstName(convMessage.getGroupParticipantMsisdn());
				if (((GroupConversation) conversation).getGroupParticipant(convMessage.getGroupParticipantMsisdn()).getContactInfo().isUnknownContact())
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
					participantName.setText(name);
				}
				participantDetails.setTag(convMessage);
				participantDetails.setOnClickListener(contactClick);
				participantDetails.setVisibility(View.VISIBLE);
				participantName.setVisibility(View.VISIBLE);
			}
		}
	}

	private void showTransferInitialization(ViewHolder holder, HikeFile hikeFile, Drawable thumbnail)
	{
		if (((hikeFile.getHikeFileType() == HikeFileType.IMAGE) || (hikeFile.getHikeFileType() == HikeFileType.VIDEO)) && thumbnail != null)
		{
			holder.wating.setVisibility(View.VISIBLE);
			holder.circularProgressBg.setVisibility(View.VISIBLE);
		}
		else if (hikeFile.getHikeFileType() == HikeFileType.LOCATION)
		{
			holder.wating.setVisibility(View.VISIBLE);
			holder.circularProgressBg.setVisibility(View.VISIBLE);
		}
		else if (hikeFile.getHikeFileType() == HikeFileType.AUDIO_RECORDING)
		{
			holder.watingRec.setVisibility(View.VISIBLE);
		}
		else
		{
			holder.watingExt.setVisibility(View.VISIBLE);
			holder.circularProgressBgExt.setVisibility(View.VISIBLE);
		}
	}

	private void showTransferProgress(ViewHolder holder, FileSavedState fss, long msgId, HikeFile hikeFile, boolean isSent, Drawable thumbnail)
	{
		int progress = FileTransferManager.getInstance(context).getFTProgress(msgId, hikeFile.getFile(), isSent);
		int chunkSize = FileTransferManager.getInstance(context).getChunkSize(msgId);
		if (fss.getTotalSize() <= 0 || (fss.getTransferredSize() == 0 && fss.getFTState() == FTState.IN_PROGRESS))
		{
			showTransferInitialization(holder, hikeFile, thumbnail);
		}
		else
		{
			if (((hikeFile.getHikeFileType() == HikeFileType.IMAGE) || (hikeFile.getHikeFileType() == HikeFileType.VIDEO)) && (thumbnail != null))
			{
				holder.circularProgress.setProgress(progress * 0.01f);
				holder.circularProgress.setVisibility(View.VISIBLE);
				holder.circularProgressBg.setVisibility(View.VISIBLE);
			}
			else if (hikeFile.getHikeFileType() == HikeFileType.AUDIO_RECORDING)
			{
				showTransferInitialization(holder, hikeFile, thumbnail);
			}
			else
			{
				holder.circularProgressExt.setProgress(progress * 0.01f);
				holder.circularProgressExt.setVisibility(View.VISIBLE);
				holder.circularProgressBgExt.setVisibility(View.VISIBLE);
			}
		}

	}

	private boolean ifFirstMessageFromRecepient(ConvMessage convMessage, int position)
	{
		boolean ret = false;
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
		return ret;
	}

	private void createFileThumb(ImageView fileThumb)
	{
		// TODO Auto-generated method stub
		Logger.d(getClass().getSimpleName(), "creating default thumb. . . ");
		int pixels = context.getResources().getDimensionPixelSize(R.dimen.file_message_item_size);
		Logger.d(getClass().getSimpleName(), "density: " + Utils.densityMultiplier);
		fileThumb.getLayoutParams().height = pixels;
		fileThumb.getLayoutParams().width = pixels;
		// fileThumb.setBackgroundColor(context.getResources().getColor(R.color.file_message_item_bg));
		fileThumb.setBackgroundResource(R.drawable.bg_file_thumb);
		fileThumb.setImageResource(0);
	}
	
	private void createMediaThumb(ImageView fileThumb)
	{
		// TODO Auto-generated method stub
		Logger.d(getClass().getSimpleName(), "creating default thumb. . . ");
		int pixels = context.getResources().getDimensionPixelSize(R.dimen.file_thumbnail_size);
		//int pixels = (int) (250 * Utils.densityMultiplier);
		Logger.d(getClass().getSimpleName(), "density: " + Utils.densityMultiplier);
		fileThumb.getLayoutParams().height = pixels;
		fileThumb.getLayoutParams().width = pixels;
		fileThumb.setBackgroundColor(context.getResources().getColor(R.color.file_message_item_bg));
		fileThumb.setImageResource(0);
	}

	// View.OnClickListener fileClick = new OnClickListener()
	// {
	//
	// @Override
	// public void onClick(View v)
	// {
	// ConvMessage convMessage = (ConvMessage) v.getTag(R.string.One);
	// switch (v.getId())
	// {
	// case R.id.overlayBg:
	//
	// HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
	// File file = hikeFile.getFile();
	//
	// if (convMessage.isSent())
	// {
	// FileSavedState fss = FileTransferManager.getInstance(context).getUploadFileState(convMessage.getMsgID(), file);
	//
	// View overlay = (View) v;
	// overlay.setClickable(false);
	// ImageView resumeButton = (ImageView) v.getTag(R.string.Two);
	// // convMessage.setResumeButtonVisibility(false);
	//
	// Logger.d("Upload- button pressed", fss.getFTState().toString());
	//
	// // If the file is complete or has not started yet overlay should not be in action
	// if (fss.getFTState() == FTState.NOT_STARTED || fss.getFTState() == FTState.COMPLETED || fss.getFTState() == FTState.PAUSING)
	// return;
	//
	// if (fss.getFTState() == FTState.IN_PROGRESS)
	// {
	// resumeButton.setImageResource(R.drawable.ic_pause_ftr_disabled);
	// FileTransferManager.getInstance(context).pauseTask(convMessage.getMsgID());
	// }
	// else
	// {
	// resumeButton.setImageResource(R.drawable.ic_resume_ftr_disabled);
	// FileTransferManager.getInstance(context).uploadFile(convMessage, conversation.isOnhike());
	// }
	//
	// }
	// else
	// {
	// FileSavedState fss = FileTransferManager.getInstance(context).getDownloadFileState(convMessage.getMsgID(), file);
	//
	// View overlay = (View) v;
	// overlay.setClickable(false);
	// ImageView resumeButton = (ImageView) v.getTag(R.string.Two);
	// // convMessage.setResumeButtonVisibility(false);
	//
	// Logger.d("Download- button pressed", fss.getFTState().toString());
	//
	// // If the file is complete or has not started yet overlay should not be in action
	// if (fss.getFTState() == FTState.COMPLETED || fss.getFTState() == FTState.PAUSING)
	// return;
	//
	// if (fss.getFTState() == FTState.IN_PROGRESS)
	// {
	// resumeButton.setImageResource(R.drawable.ic_pause_ftr_disabled);
	// FileTransferManager.getInstance(context).pauseTask(convMessage.getMsgID());
	// }
	// else
	// {
	// resumeButton.setImageResource(R.drawable.ic_resume_ftr_disabled);
	// FileTransferManager.getInstance(context).downloadFile(file, hikeFile.getFileKey(), convMessage.getMsgID(), hikeFile.getHikeFileType(), convMessage, true);
	// }
	// }
	// notifyDataSetChanged();
	// }
	//
	// }
	// };

	View.OnClickListener contactClick = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			final ConvMessage message = (ConvMessage) v.getTag();
			ArrayList<String> optionsList = new ArrayList<String>();
			String number = null;
			final String name = ((GroupConversation) conversation).getGroupParticipant(message.getGroupParticipantMsisdn()).getContactInfo().getName();
			if (((GroupConversation) conversation).getGroupParticipant(message.getGroupParticipantMsisdn()).getContactInfo().isUnknownContact())
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
						context.startActivity(intent);
					}
				}
			});

			AlertDialog alertDialog = builder.show();
			alertDialog.getListView().setDivider(context.getResources().getDrawable(R.drawable.ic_thread_divider_profile));
			// chatThread.showContactDetails(items, name, null, true);
		}
	};

	private void setFileButtonResource(ImageView button, ConvMessage convMessage, HikeFile hikeFile)
	{
		// TODO : handle according to filestate
		button.setBackgroundResource(R.drawable.bg_red_btn_selector);
		if (FileTransferManager.getInstance(context).isFileTaskExist(convMessage.getMsgID()))
		{
			button.setImageResource(R.drawable.ic_download_file);
			button.setBackgroundResource(R.drawable.bg_red_btn_disabled);
		}
		else if (hikeFile.wasFileDownloaded() && hikeFile.getHikeFileType() != HikeFileType.CONTACT)
		{
			button.setImageResource(R.drawable.ic_open_received_file);
		}
		else
		{
			button.setImageResource(R.drawable.ic_download_file);
		}
	}

	private void setTextAndIconForSystemMessages(TextView textView, CharSequence text, int iconResId)
	{
		textView.setText(text);
		textView.setCompoundDrawablesWithIntrinsicBounds(iconResId, 0, 0, 0);
	}

	private void showTryingAgainIcon(ImageView iv, long ts)
	{
		/*
		 * We are checking this so that we can delay the try again icon from being shown immediately if the user just sent the msg. If it has been over 5 secs then the user will
		 * immediately see the icon though.
		 */
		if ((((long) System.currentTimeMillis() / 1000) - ts) < 3)
		{
			iv.setVisibility(View.INVISIBLE);

			Animation anim = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
			anim.setStartOffset(4000);
			anim.setDuration(1);

			iv.setAnimation(anim);
		}
		iv.setVisibility(View.VISIBLE);
		iv.setImageResource(R.drawable.ic_retry_sending);
	}

	Handler handler = new Handler();

	private void scheduleUndeliveredText(TextView tv, View container, ImageView iv, long ts)
	{
		if (showUndeliveredMessage != null)
		{
			handler.removeCallbacks(showUndeliveredMessage);
		}

		long diff = (((long) System.currentTimeMillis() / 1000) - ts);

		if (Utils.isUserOnline(context) && diff < HikeConstants.DEFAULT_UNDELIVERED_WAIT_TIME)
		{
			showUndeliveredMessage = new ShowUndeliveredMessage(tv, container, iv);
			handler.postDelayed(showUndeliveredMessage, (HikeConstants.DEFAULT_UNDELIVERED_WAIT_TIME - diff) * 1000);
		}
		else
		{
			showUndeliveredTextAndSetClick(tv, container, iv, true);
		}
	}

	private void showFileTransferElements(ViewHolder holder)
	{
		holder.fileThumb.setVisibility(View.VISIBLE);
	}

	private boolean showDayIndicator(int position)
	{
		/*
		 * We show the time stamp in the status message separately so no need to show this time stamp.
		 */
		if (ViewType.values()[getItemViewType(position)] == ViewType.STATUS_MESSAGE)
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
		else if (previous.getMsgID() == LAST_READ_CONV_MESSAGE_ID)
		{
			return false;
		}

		Calendar currentMessageCalendar = Calendar.getInstance();
		currentMessageCalendar.setTimeInMillis(current.getTimestamp() * 1000);

		Calendar previousMessageCalendar = Calendar.getInstance();
		previousMessageCalendar.setTimeInMillis(previous.getTimestamp() * 1000);

		return (previousMessageCalendar.get(Calendar.DAY_OF_YEAR) != currentMessageCalendar.get(Calendar.DAY_OF_YEAR));
	}

	private void setSDRAndTimestamp(int position, TextView tv, ImageView iv, View container)
	{
		/*
		 * We show the time stamp in the status message separately so no need to show this time stamp.
		 */
		if (ViewType.values()[getItemViewType(position)] == ViewType.STATUS_MESSAGE)
		{
			return;
		}
		tv.setVisibility(View.VISIBLE);
		if (iv != null)
		{
			iv.setVisibility(View.GONE);
		}

		tv.setTextColor(context.getResources().getColor(isDefaultTheme ? R.color.list_item_subtext : R.color.white));

		ConvMessage current = getItem(position);
		if (current.isSent() && (position == lastSentMessagePosition))
		{
			switch (current.getState())
			{
			case SENT_UNCONFIRMED:
				tv.setVisibility(View.GONE);
				iv.setVisibility(View.VISIBLE);

				AnimationDrawable ad = (AnimationDrawable) context.getResources().getDrawable(isDefaultTheme ? R.drawable.sending : R.drawable.sending_custom);
				iv.setImageDrawable(ad);
				ad.setCallback(iv);
				ad.setVisible(true, true);
				ad.start();

				if (!current.isSMS())
				{
					scheduleUndeliveredText(tv, container, iv, current.getTimestamp());
				}
				break;
			case SENT_CONFIRMED:
				tv.setText(context.getString(!current.isSMS() ? R.string.sent : R.string.sent_via_sms, current.getTimestampFormatted(false, context)));
				if (!current.isSMS())
				{
					scheduleUndeliveredText(tv, container, iv, current.getTimestamp());
				}
				break;
			case SENT_DELIVERED:
				tv.setText(R.string.delivered);
				break;
			case SENT_DELIVERED_READ:
				if (!isGroupChat)
				{
					tv.setText(R.string.read);
				}
				else
				{
					setReadByForGroup(current, tv);
				}
				break;
			}
		}
		else
		{

			ConvMessage next = position == getCount() - 1 ? null : getItem(position + 1);

			if (next == null || (next.isSent() != current.isSent()) || (next.getTimestamp() - current.getTimestamp() > 2 * 60))
			{
				tv.setText(current.getTimestampFormatted(false, context));
				return;
			}
			tv.setVisibility(View.GONE);
		}
	}

	private void setNewSDR(int position, TextView time, ImageView status, boolean ext, View messageTimeStatus, TextView messageInfo, View container, ImageView sending)
	{
		ConvMessage message = getItem(position);

		time.setText(message.getTimestampFormatted(false, context));
		time.setVisibility(View.VISIBLE);
		if (message.isSent())
		{
			if(message.isFileTransferMessage() && (TextUtils.isEmpty(message.getMetadata().getHikeFiles().get(0).getFileKey())))
			{
				if(ext)
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
					status.setImageResource(R.drawable.ic_tick_white);
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
					status.setImageResource(R.drawable.ic_tick);
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

		if (messageTimeStatus != null)
			messageTimeStatus.setVisibility(View.VISIBLE);

		if ((message.getState() != null) && (position == lastSentMessagePosition))
		{
			messageInfo.setText("");
			messageInfo.setVisibility(View.VISIBLE);
			if (message.getState() == State.SENT_DELIVERED_READ && isGroupChat)
			{
				// messageInfo.setVisibility(View.VISIBLE);
				messageInfo.setTextColor(context.getResources().getColor(isDefaultTheme ? R.color.list_item_subtext : R.color.white));
				setReadByForGroup(message, messageInfo);
			}
			else if (message.getState() == State.SENT_UNCONFIRMED || message.getState() == State.SENT_CONFIRMED)
			{
				if (!message.isSMS())
				{
					scheduleUndeliveredText(messageInfo, container, sending, message.getTimestamp());
				}
			}
		}
	}

	private void setReadByForGroup(ConvMessage convMessage, TextView tv)
	{
		GroupConversation groupConversation = (GroupConversation) conversation;
		JSONArray readByArray = convMessage.getReadByArray();

		if (readByArray == null || groupConversation.getGroupMemberAliveCount() == readByArray.length())
		{
			tv.setText(R.string.read_by_everyone);
		}
		else
		{
			StringBuilder sb = new StringBuilder();

			int lastIndex = readByArray.length() - HikeConstants.MAX_READ_BY_NAMES;

			boolean moreNamesThanMaxCount = false;
			if (lastIndex < 0)
			{
				lastIndex = 0;
			}
			else if (lastIndex == 1)
			{
				/*
				 * We increment the last index if its one since we can
				 * accommodate another name in this case. 
				 */
				lastIndex ++;
				moreNamesThanMaxCount = true;
			}
			else if (lastIndex > 0)
			{
				moreNamesThanMaxCount = true;
			}

			for (int i = readByArray.length() - 1; i >= lastIndex; i--)
			{
				sb.append(groupConversation.getGroupParticipantFirstName(readByArray.optString(i)));
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
			v.performLongClick();
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

		if (lastSentMessagePosition != -1 && convMessage.isSent() && convMessage.equals(convMessages.get(lastSentMessagePosition)) && isMessageUndelivered(convMessage)
				&& convMessage.getState() != State.SENT_UNCONFIRMED && !chatThread.isContactOnline())
		{
			long diff = (((long) System.currentTimeMillis() / 1000) - convMessage.getTimestamp());

			/*
			 * Only show fallback if the message has not been sent for our max wait time.
			 */
			if (diff >= HikeConstants.DEFAULT_UNDELIVERED_WAIT_TIME || !Utils.isUserOnline(context))
			{

				if (conversation.isOnhike())
				{
					if (!Utils.isUserOnline(context))
					{
						if (conversation instanceof GroupConversation)
						{
							Toast.makeText(context, R.string.gc_fallback_offline, Toast.LENGTH_LONG).show();
						}
						else
						{
							showSMSDialog(true);
						}
					}
					else
					{
						if (conversation instanceof GroupConversation)
						{
							showSMSDialog(false);
						}
						else
						{
							/*
							 * Only show the H2S fallback option if messaging indian numbers.
							 */
							showSMSDialog(!conversation.getMsisdn().startsWith(HikeConstants.INDIA_COUNTRY_CODE));
						}
					}
				}
				else
				{
					sendAllUnsentMessagesAsSMS(Utils.getSendSmsPref(context));
				}
				return;
			}
		}
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
						else if (fss.getFTState() != FTState.PAUSING && fss.getFTState() != FTState.INITIALIZED)
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
					else if (fss.getFTState() != FTState.PAUSING && fss.getFTState() != FTState.INITIALIZED)
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
		File receivedFile = hikeFile.getFile();
		Logger.d(getClass().getSimpleName(), "Opening file");
		Intent openFile = new Intent(Intent.ACTION_VIEW);
		if (hikeFile.getHikeFileType() == HikeFileType.LOCATION)
		{
			String uri = String.format(Locale.US, "geo:%1$f,%2$f?z=%3$d&q=%1$f,%2$f", hikeFile.getLatitude(), hikeFile.getLongitude(), hikeFile.getZoomLevel());
			openFile.setData(Uri.parse(uri));
		}
		else if (hikeFile.getHikeFileType() == HikeFileType.CONTACT)
		{
			saveContact(hikeFile);
			return;
		}
		else if (hikeFile.getHikeFileType() == HikeFileType.AUDIO_RECORDING)
		{
			String fileKey = hikeFile.getFileKey();

			ImageView recAction = (ImageView) parent.findViewById(R.id.rec_action);
			// TextView durationTxt = (TextView) parent.findViewById(convMessage.isSent() ? R.id.message_send : R.id.message_receive_ft);
			TextView durationTxt = (TextView) parent.findViewById(R.id.rec_duration);
			View durationProgress = (View) parent.findViewById(R.id.rec_circular_progress);
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
		}
		else
		{
			openFile.setDataAndType(Uri.fromFile(receivedFile), hikeFile.getFileTypeString());
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

	@Override
	public boolean onLongClick(View view)
	{
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

		Utils.sendNativeSmsLogEvent(isChecked);

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

	private void setSmsToggleSubtext(boolean isChecked)
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

		TextView tv;

		ImageView iv;

		View container;

		public ShowUndeliveredMessage(TextView tv, View container, ImageView iv)
		{
			this.tv = tv;
			this.container = container;
			this.iv = iv;
		}

		@Override
		public void run()
		{
			if (lastSentMessagePosition >= convMessages.size() || lastSentMessagePosition == -1)
			{
				return;
			}
			ConvMessage lastSentMessage = convMessages.get(lastSentMessagePosition);
			if (isMessageUndelivered(lastSentMessage))
			{
				showUndeliveredTextAndSetClick(tv, container, iv, true);
			}
		}
	}

	private void showUndeliveredTextAndSetClick(TextView tv, View container, ImageView iv, boolean fromHandler)
	{
		String undeliveredText = getUndeliveredTextRes();
		if (!TextUtils.isEmpty(undeliveredText))
		{
			iv.setVisibility(View.GONE);
			tv.setVisibility(View.VISIBLE);
			tv.setText(undeliveredText);
		}
		tv.setTextColor(context.getResources().getColor(chatTheme.offlineMsgTextColor()));

		container.setTag(convMessages.get(lastSentMessagePosition));
		container.setOnClickListener(this);

		container.setOnLongClickListener(this);
	}

	private String getUndeliveredTextRes()
	{
		ConvMessage convMessage = convMessages.get(lastSentMessagePosition);

		int res;
		if (convMessage.getState() == State.SENT_UNCONFIRMED)
		{
			/*
			 * We don't want to show the user as offline. So we return blank here.
			 */
			return "";
		}
		else
		{
			/*
			 * We don't show the contact as offline if the user is online in the last time.
			 */
			if (chatThread.isContactOnline())
			{
				return "";
			}
			res = conversation.isOnhike() && !(conversation instanceof GroupConversation) ? R.string.msg_undelivered : R.string.sms_undelivered;
		}
		return context.getString(res, Utils.getFirstName(conversation.getLabel()));
	}

	private void showSMSDialog(final boolean nativeOnly)
	{
		final Dialog dialog = new Dialog(chatThread, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.sms_undelivered_popup);
		dialog.setCancelable(true);

		View hikeSMS = dialog.findViewById(R.id.hike_sms_container);
		View nativeSMS = dialog.findViewById(R.id.native_sms_container);
		View divider = dialog.findViewById(R.id.divider);
		TextView nativeHeader = (TextView) dialog.findViewById(R.id.native_sms_header);

		hikeSMS.setVisibility(nativeOnly ? View.GONE : View.VISIBLE);
		divider.setVisibility(nativeOnly ? View.GONE : View.VISIBLE);

		if (conversation instanceof GroupConversation)
		{
			nativeSMS.setVisibility(View.GONE);
			divider.setVisibility(View.GONE);
		}

		final CheckBox sendHike = (CheckBox) dialog.findViewById(R.id.hike_sms_checkbox);

		final CheckBox sendNative = (CheckBox) dialog.findViewById(R.id.native_sms_checkbox);

		final Button sendBtn = (Button) dialog.findViewById(R.id.btn_send);
		sendBtn.setEnabled(false);

		if (PreferenceManager.getDefaultSharedPreferences(context).contains(HikeConstants.SEND_UNDELIVERED_AS_NATIVE_SMS_PREF))
		{
			boolean nativeOn = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.SEND_UNDELIVERED_AS_NATIVE_SMS_PREF, false);
			if (nativeOn || nativeOnly)
			{
				sendNative.setChecked(true);
				sendBtn.setEnabled(true);
			}
			else if (!nativeOnly || (conversation instanceof GroupConversation))
			{
				sendHike.setChecked(true);
				sendBtn.setEnabled(true);
			}
		}

		int numUnsentMessages = getAllUnsentMessages(false).size();
		nativeHeader.setText(context.getString(R.string.x_regular_sms, numUnsentMessages));

		sendHike.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				sendHike.setChecked(true);
				sendNative.setChecked(false);
				sendBtn.setEnabled(true);
			}
		});

		sendNative.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				sendHike.setChecked(false);
				sendNative.setChecked(true);
				sendBtn.setEnabled(true);
			}
		});

		sendBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				if (sendHike.isChecked())
				{
					Utils.setSendUndeliveredSmsSetting(context, false);
					sendAllUnsentMessagesAsSMS(false);
				}
				else
				{
					if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.RECEIVE_SMS_PREF, false))
					{
						showSMSClientDialog(false, null, false);
					}
					else
					{
						sendAllUnsentMessagesAsSMS(true);
						Utils.setSendUndeliveredSmsSetting(context, true);
					}
				}
				dialog.dismiss();
			}
		});

		dialog.show();
	}

	private void showSMSClientDialog(final boolean triggeredFromToggle, final CompoundButton checkBox, final boolean showingNativeInfoDialog)
	{
		final Dialog dialog = new Dialog(chatThread, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.enable_sms_client_popup);
		dialog.setCancelable(showingNativeInfoDialog);

		TextView header = (TextView) dialog.findViewById(R.id.header);
		TextView body = (TextView) dialog.findViewById(R.id.body);
		Button btnOk = (Button) dialog.findViewById(R.id.btn_ok);
		Button btnCancel = (Button) dialog.findViewById(R.id.btn_cancel);

		header.setText(showingNativeInfoDialog ? R.string.native_header : R.string.use_hike_for_sms);
		body.setText(showingNativeInfoDialog ? R.string.native_info : R.string.use_hike_for_sms_info);

		if (showingNativeInfoDialog)
		{
			btnCancel.setVisibility(View.GONE);
			btnOk.setText(R.string.continue_txt);
		}
		else
		{
			btnCancel.setText(R.string.cancel);
			btnOk.setText(R.string.allow);
		}

		btnOk.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
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
						sendAllUnsentMessagesAsSMS(true);
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
		});

		dialog.setOnCancelListener(new OnCancelListener()
		{

			@Override
			public void onCancel(DialogInterface dialog)
			{
				if (showingNativeInfoDialog)
				{
					checkBox.setChecked(false);
				}
			}
		});

		btnCancel.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
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
			}
		});

		dialog.show();
	}

	private List<ConvMessage> getAllUnsentMessages(boolean resetTimestamp)
	{
		List<ConvMessage> unsentMessages = new ArrayList<ConvMessage>();
		int count = 0;
		for (int i = lastSentMessagePosition; i >= 0; i--)
		{
			ConvMessage convMessage = convMessages.get(i);
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
			if (++count >= HikeConstants.MAX_FALLBACK_NATIVE_SMS)
			{
				break;
			}
		}
		return unsentMessages;
	}

	private void sendAllUnsentMessagesAsSMS(boolean nativeSMS)
	{
		List<ConvMessage> unsentMessages = getAllUnsentMessages(true);
		Logger.d(getClass().getSimpleName(), "Unsent messages: " + unsentMessages.size());

		if (nativeSMS)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.SEND_NATIVE_SMS_FALLBACK, unsentMessages);
		}
		else
		{
			if (conversation.isOnhike())
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.SEND_HIKE_SMS_FALLBACK, unsentMessages);
			}
			else
			{
				for (ConvMessage convMessage : unsentMessages)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, convMessage.serialize());
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
		return ((!convMessage.isSMS() && convMessage.getState().ordinal() < State.SENT_DELIVERED.ordinal()) || (convMessage.isSMS() && convMessage.getState().ordinal() < State.SENT_CONFIRMED
				.ordinal())) && fileUploaded && !isGroupChatInternational;
	}

	enum VoiceMessagePlayerState
	{
		PLAYING, PAUSED, STOPPED
	};

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
				switch (playerState)
				{
				case PLAYING:
				case PAUSED:
					int progress = 0;
					if (mediaPlayer.getDuration() > 0)
						progress = (mediaPlayer.getCurrentPosition() * 100) / mediaPlayer.getDuration();
					((HoloCircularProgress) durationProgress).setProgress(progress * 0.01f);
					Utils.setupFormattedTime(durationTxt, mediaPlayer.getCurrentPosition() / 1000);
					break;
				case STOPPED:
					((HoloCircularProgress) durationProgress).setProgress(0.00f);
					Utils.setupFormattedTime(durationTxt, mediaPlayer.getDuration() / 1000);
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

	// public StickerLoader getStickerLoader()
	// {
	// return largeStickerLoader;
	// }

	public IconLoader getIconImageLoader()
	{
		return iconLoader;
	}

	public void toggleSelection(int position)
	{
		selectView(position, !isSelected(position));
	}

	public void removeSelection()
	{
		mSelectedItemsIds.clear();
		notifyDataSetChanged();
	}

	public void selectView(int position, boolean value)
	{
		if (value)
		{
			mSelectedItemsIds.add(position);
		}
		else
		{
			mSelectedItemsIds.remove(position);
		}

		notifyDataSetChanged();
	}

	public void setPositionsSelected(ArrayList<Integer> selectedPositions)
	{
		mSelectedItemsIds.addAll(selectedPositions);
		notifyDataSetChanged();
	}

	public int getSelectedCount()
	{
		return mSelectedItemsIds.size();
	}

	public Set<Integer> getSelectedIds()
	{
		return mSelectedItemsIds;
	}

	public boolean isSelected(int position)
	{
		return mSelectedItemsIds.contains(position);
	}

	public void setActionMode(boolean isOn)
	{
		isActionModeOn = isOn;
	}

	public boolean shownSdrToolTip()
	{
		return msgIdForSdrTip != -1;
	}
}
