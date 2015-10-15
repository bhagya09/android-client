package com.bsb.hike.notifications;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Action;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.TaskStackBuilder;
import android.text.SpannableString;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.NotificationType;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.AnalyticsConstants.AppOpenSource;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.Protip;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.timeline.model.ActionsDataModel.ActionTypes;
import com.bsb.hike.timeline.model.ActionsDataModel.ActivityObjectTypes;
import com.bsb.hike.timeline.model.FeedDataModel;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.SoundUtils;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPService;
import com.bsb.hike.voip.VoIPUtils;

public class HikeNotification
{
	private String VIB_OFF, VIB_DEF, VIB_SHORT, VIB_LONG;

	private String NOTIF_SOUND_OFF, NOTIF_SOUND_DEFAULT, NOTIF_SOUND_HIKE;

	public static final int HIKE_NOTIFICATION = -89;

	public static final int PROTIP_NOTIFICATION_ID = -89;

	public static final int GAMING_PACKET_NOTIFICATION_ID = -89;

	public static final int FREE_SMS_POPUP_NOTIFICATION_ID = -89;

	public static final int APP_UPDATE_AVAILABLE_ID = -90;
	
	public static final int PERSISTENT_NOTIF_ID = -111;

	public static final int STEALTH_NOTIFICATION_ID = -89;

	public static final int STEALTH_POPUP_NOTIFICATION_ID = -89;

	public static final int HIKE_TO_OFFLINE_PUSH_NOTIFICATION_ID = -89;

	public static final int VOIP_MISSED_CALL_NOTIFICATION_ID = -89;

	// We need a constant notification id for bulk/big text notifications. Since
	// we are using msisdn for other single notifications, it is safe to use any
	// number <= 99
	public static final int HIKE_SUMMARY_NOTIFICATION_ID = -89;

	public static final int TICKER_TEXT_MAX_LENGHT = 100;
	
	public static final int OFFLINE_REQUEST_ID = -91;
	// We need a key to pair notification id. This will be used to retrieve notification id on notification dismiss/action.
	public static final String HIKE_NOTIFICATION_ID_KEY = "hike.notification";

	public static final String HIKE_STEALTH_MESSAGE_KEY = "HIKE_STEALTH_MESSAGE_KEY";


	private static final String SEPERATOR = " ";

	private final Context context;

	private final NotificationManager notificationManager;

	private final SharedPreferences sharedPreferences;
	
	private SharedPreferences settingPref;

	private HikeNotificationMsgStack hikeNotifMsgStack;
	

	
	private static HashMap<String, Long> lastNotificationTimeMap=new HashMap<String, Long>();//For now HashMap<groupId, LastNotifiactionTimeInMillis>()
	
	private static final int DEFAULT_NOTIFICATION_DELAY_FOR_GROUP_IN_SEC = 15;

	private static final int DEFAULT_NOTIFICATION_DELAY_FOR_ONE_TO_ONE_IN_SEC = 0;//one to one

	private static HikeNotification hikeNotificationInstance=new HikeNotification();
	
	private static long lastNotificationPlayedTimeForOneToOne;


	private HikeNotification()
	{
		this.context = HikeMessengerApp.getInstance().getApplicationContext();
		this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		this.sharedPreferences = context.getSharedPreferences(HikeMessengerApp.STATUS_NOTIFICATION_SETTING, 0);
		this.hikeNotifMsgStack = HikeNotificationMsgStack.getInstance();
		this.settingPref = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0); 
		

		if (VIB_DEF == null)
		{
			Resources res = context.getResources();
			VIB_OFF = res.getString(R.string.vib_off);
			VIB_DEF = res.getString(R.string.vib_default);
			VIB_SHORT = res.getString(R.string.vib_short);
			VIB_LONG = res.getString(R.string.vib_long);
			NOTIF_SOUND_OFF = res.getString(R.string.notif_sound_off);
			NOTIF_SOUND_DEFAULT = res.getString(R.string.notif_sound_default);
			NOTIF_SOUND_HIKE = res.getString(R.string.notif_sound_Hike);
		}
	}
	

	public static synchronized HikeNotification getInstance()
	{
		return hikeNotificationInstance;
	}

	public void notifySMSPopup(final String bodyString,int notificationType)
	{
		/*
		 * return straight away if the block notification setting is ON
		 */
		if (sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			return;
		}

		// if notification message stack is empty, add to it and proceed with single notification display
		// else add to stack and notify clubbed messages
		if (hikeNotifMsgStack.isEmpty())
		{
			hikeNotifMsgStack.addMessage(context.getString(R.string.app_name), bodyString, notificationType);
		}
		else
		{
			notifyStringMessage(context.getString(R.string.app_name), bodyString, false, notificationType);
			return;
		}

		/*
		 * invoke the chat thread here. The free SMS invite switch popup should already be showing here ideally by now.
		 */
		final Intent notificationIntent = Utils.getHomeActivityIntent(context);
		notificationIntent.putExtra(HikeConstants.Extras.NAME, context.getString(R.string.team_hike));

		notificationIntent.setData((Uri.parse("custom://" + FREE_SMS_POPUP_NOTIFICATION_ID)));
		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder = getNotificationBuilder(context.getString(R.string.team_hike), bodyString, bodyString, null, smallIconId, false);
		setNotificationIntentForBuilder(mBuilder, notificationIntent, FREE_SMS_POPUP_NOTIFICATION_ID);

		notifyNotification(FREE_SMS_POPUP_NOTIFICATION_ID, mBuilder);
		
		notificationBuilderPostWork();

	}

	public void notifyStealthPopup(final String headerString, int notificationType)
	{
		/*
		 * return straight away if the block notification setting is ON
		 */
		if (sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			return;
		}

		// if notification message stack is empty, add to it and proceed with single notification display
		// else add to stack and notify clubbed messages
		if (hikeNotifMsgStack.isEmpty())
		{
			hikeNotifMsgStack.addMessage(context.getString(R.string.app_name), headerString, notificationType);

		}
		else
		{
			notifyStringMessage(context.getString(R.string.app_name), headerString, false, notificationType);
			return;
		}

		/*
		 * invoke the chat thread here. The Stealth tip popup should already be showing here ideally by now.
		 */
		final Intent notificationIntent = Utils.getHomeActivityIntent(context);
		notificationIntent.putExtra(HikeConstants.Extras.HAS_TIP, true);
		notificationIntent.putExtra(HikeConstants.Extras.NAME, context.getString(R.string.team_hike));

		notificationIntent.setData((Uri.parse("custom://" + STEALTH_POPUP_NOTIFICATION_ID)));
		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder = getNotificationBuilder(context.getString(R.string.team_hike), headerString, headerString, null, smallIconId, false);
		setNotificationIntentForBuilder(mBuilder, notificationIntent, FREE_SMS_POPUP_NOTIFICATION_ID);
		
		notifyNotification(FREE_SMS_POPUP_NOTIFICATION_ID, mBuilder);
		
		notificationBuilderPostWork();
	}

	public void notifyAtomicPopup(final String message, Intent notificationIntent, int notificationType)
	{
		/*
		 * return straight away if the block notification setting is ON
		 */
		if (sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			return;
		}

		// if notification message stack is empty, add to it and proceed with single notification display
		// else add to stack and notify clubbed messages
		if (hikeNotifMsgStack.isEmpty())
		{
			hikeNotifMsgStack.addMessage(context.getString(R.string.app_name), message, notificationType);
		}
		else
		{
			notifyStringMessage(context.getString(R.string.app_name), message, false, notificationType);
			return;
		}

		notificationIntent.putExtra(HikeConstants.Extras.NAME, context.getString(R.string.team_hike));

		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder = getNotificationBuilder(context.getString(R.string.team_hike), message, message, null, smallIconId, false);
		setNotificationIntentForBuilder(mBuilder, notificationIntent, FREE_SMS_POPUP_NOTIFICATION_ID);

		notifyNotification(FREE_SMS_POPUP_NOTIFICATION_ID, mBuilder);
		
		notificationBuilderPostWork();

	}

	public void notifyMessage(final Protip proTip, int notificationType)
	{

		// if notification message stack is empty, add to it and proceed with single notification display
		// else add to stack and notify clubbed messages
		if (hikeNotifMsgStack.isEmpty())
		{
			hikeNotifMsgStack.addMessage(context.getString(R.string.app_name), proTip.getHeader(), notificationType);
		}
		else
		{
			notifyStringMessage(context.getString(R.string.app_name), proTip.getHeader(), false, notificationType);
			return;
		}

		// we've got to invoke the timeline here
		final Intent notificationIntent = Utils.getTimelineActivityIntent(context);
		notificationIntent.putExtra(HikeConstants.Extras.NAME, context.getString(R.string.team_hike));

		notificationIntent.setData((Uri.parse("custom://" + PROTIP_NOTIFICATION_ID)));

		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder = getNotificationBuilder(context.getString(R.string.team_hike), proTip.getHeader(), proTip.getHeader(), null, smallIconId,
				false);

		setNotificationIntentForBuilder(mBuilder, notificationIntent, PROTIP_NOTIFICATION_ID);

		if (!sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			notifyNotification(PROTIP_NOTIFICATION_ID, mBuilder);
		}
		
		notificationBuilderPostWork();
	}

	/*
	 * method to send a notification of an hike update available or applicationspush update. if isApplicationsPushUpdate is false than it is hike app update.
	 */
	public void notifyUpdatePush(int updateType, String packageName, String message, boolean isApplicationsPushUpdate)
	{
		message = (TextUtils.isEmpty(message)) ? context.getString(R.string.update_app) : message;
		final int smallIconId = returnSmallIcon();
		
		NotificationCompat.Builder mBuilder = getNotificationBuilder(context.getString(R.string.team_hike), message, message, null, smallIconId, false);		

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("market://details?id=" + packageName));
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		mBuilder.setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));

		if (!sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			int notificationId = isApplicationsPushUpdate ? GAMING_PACKET_NOTIFICATION_ID : APP_UPDATE_AVAILABLE_ID;
			notifyNotification(notificationId, mBuilder);
		}
		// TODO:: we should reset the gaming download message from preferences
	}
	
	public void notifyPersistentUpdate(String notifTitle, String message, String actionText, String laterText, Uri url, Long alarmInterval)
	{
		message = (TextUtils.isEmpty(message)) ? context.getString(R.string.pers_notif_message) : message;
		notifTitle = !TextUtils.isEmpty(notifTitle) ? notifTitle : context.getString(R.string.pers_notif_title);
		actionText = (TextUtils.isEmpty(actionText)) ? context.getString(R.string.pers_notif_action) : actionText;
		laterText = (TextUtils.isEmpty(laterText)) ? context.getString(R.string.pers_notif_later) : laterText;
		final int smallIconId = returnSmallIcon();	
		NotificationCompat.Builder mBuilder = getNotificationBuilder(notifTitle, message, message, null, smallIconId, true, true, false);
		mBuilder.setAutoCancel(false);
		mBuilder.setOngoing(true);
		Editor editor = settingPref.edit();
		editor.putString(HikeConstants.PERSISTENT_NOTIF_MESSAGE, message);
		editor.putString(HikeConstants.PERSISTENT_NOTIF_TITLE, notifTitle);
		editor.putString(HikeConstants.PERSISTENT_NOTIF_ACTION, actionText);
		editor.putString(HikeConstants.PERSISTENT_NOTIF_LATER, laterText);
		editor.putString(HikeConstants.PERSISTENT_NOTIF_URL, url.toString());
		editor.putLong(HikeConstants.PERSISTENT_NOTIF_ALARM, alarmInterval);
		editor.commit();

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(url);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		mBuilder.setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));
		
		Intent laterIntent = new Intent("com.bsb.hike.PERS_NOTIF_ALARM_INTENT");
		
		mBuilder.addAction(R.drawable.ic_recents_emo_selected, laterText, PendingIntent.getBroadcast(context, 0, laterIntent, 0))
				.addAction(R.drawable.ic_download, actionText, PendingIntent.getActivity(context, 0, intent, 0));
		
		if (!sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false) && !settingPref.getBoolean(HikeConstants.IS_HIKE_APP_FOREGROUNDED, false))
		{
			int notificationId = PERSISTENT_NOTIF_ID;
			notifyNotification(notificationId, mBuilder);
		}
	}

	public void notifyMessage(final ContactInfo contactInfo, final ConvMessage convMsg, boolean isRich, Bitmap bigPictureImage, int notificationType)
	{
		boolean isPin = false;

		boolean forceBlockNotificationSound = convMsg.isSilent();

		if (convMsg.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
			isPin = true;

		final String msisdn = convMsg.getMsisdn();
		// we are using the MSISDN now to group the notifications
		final int notificationId = HIKE_SUMMARY_NOTIFICATION_ID;

		String message = (!convMsg.isFileTransferMessage()) ? convMsg.getMessage() : HikeFileType.getFileTypeMessage(context, convMsg.getMetadata().getHikeFiles().get(0)
				.getHikeFileType(), convMsg.isSent());
		// Message will be empty for type 'uj' when the conversation does not
		// exist
		if (TextUtils.isEmpty(message)
				&& (convMsg.getParticipantInfoState() == ParticipantInfoState.USER_JOIN || convMsg.getParticipantInfoState() == ParticipantInfoState.CHAT_BACKGROUND))
		{
			if (convMsg.getParticipantInfoState() == ParticipantInfoState.USER_JOIN)
			{
				message = String.format(convMsg.getMessage(), contactInfo.getFirstName());
			}
			else
			{
				message = context.getString(R.string.chat_bg_changed, contactInfo.getFirstName());
			}
		}
		final long timestamp = convMsg.getTimestamp();

		String key = (contactInfo != null && !TextUtils.isEmpty(contactInfo.getName())) ? contactInfo.getName() : msisdn;

		// we've got to invoke the chat thread from here with the respective
		// users
		final Intent notificationIntent = new Intent(context, ChatThreadActivity.class);
		if (contactInfo.getName() != null)
		{
			notificationIntent.putExtra(HikeConstants.Extras.NAME, contactInfo.getName());
		}
		notificationIntent.putExtra(HikeConstants.Extras.MSISDN, contactInfo.getMsisdn());
		notificationIntent.putExtra(HikeConstants.Extras.WHICH_CHAT_THREAD, ChatThreadUtils.getChatThreadType(contactInfo.getMsisdn()));
		notificationIntent.putExtra(HikeConstants.Extras.CHAT_INTENT_TIMESTAMP, System.currentTimeMillis());
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		/*
		 * notifications appear to be cached, and their .equals doesn't check 'Extra's. In order to prevent the wrong intent being fired, set a data field that's unique to the
		 * conversation we want to open. http://groups .google.com/group/android-developers/browse_thread/thread /e61ec1e8d88ea94d/1fe953564bd11609?#1fe953564bd11609
		 */

		notificationIntent.setData((Uri.parse("custom://" + notificationId)));

		notificationIntent.putExtra(HikeConstants.Extras.MSISDN, msisdn);
		if (contactInfo != null)
		{
			if (contactInfo.getName() != null)
			{
				notificationIntent.putExtra(HikeConstants.Extras.NAME, contactInfo.getName());
			}
		}
		final int icon = returnSmallIcon();

		/*
		 * Jellybean has added support for emojis so we don't need to add a '*' to replace them
		 */
		if (Build.VERSION.SDK_INT < 16)
		{
			// Replace emojis with a '*'
			message = SmileyParser.getInstance().replaceEmojiWithCharacter(message, "*");
		}

		String partName = "";
		// For showing the name of the contact that sent the message in a group
		// chat
		if (convMsg.isOneToNChat() && !TextUtils.isEmpty(convMsg.getGroupParticipantMsisdn()) && convMsg.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
		{
			GroupParticipant groupParticipant = HikeConversationsDatabase.getInstance().getGroupParticipant(convMsg.getMsisdn(), convMsg.getGroupParticipantMsisdn());

			if (groupParticipant != null)
			{
				ContactInfo participant = HikeConversationsDatabase.getInstance().getGroupParticipant(convMsg.getMsisdn(), convMsg.getGroupParticipantMsisdn()).getContactInfo();

				key = participant.getName();
			}
			if (TextUtils.isEmpty(key))
			{
				key = convMsg.getGroupParticipantMsisdn();
			}

			partName = key;
			if (isPin)
			{
				message = key + " " + context.getString(R.string.pin_notif_text) + HikeConstants.SEPARATOR + message;
			}
			else
			{
				message = key + HikeConstants.SEPARATOR + message;
			}
			key = ContactManager.getInstance().getName(convMsg.getMsisdn());
		}

		boolean doesBigPictureExist = (bigPictureImage == null) ? false : true;
		final String text = String.format("%1$s: %2$s", key, message);
		// For showing the name of the contact that sent the message in a group
		// chat

		if (doesBigPictureExist && isRich)
		{
			final String messageString = (!convMsg.isFileTransferMessage()) ? convMsg.getMessage() : HikeFileType.getFileTypeMessage(context, convMsg.getMetadata().getHikeFiles()
					.get(0).getHikeFileType(), convMsg.isSent());

			if (convMsg.isOneToNChat())
			{
				message = partName + HikeConstants.SEPARATOR + messageString;
			}
			else
				message = messageString;

			// if big picture exists in the new message, check notification stack if we are showing clubbed messages
			// if we are, discard big picture since a message for it "..sent you a photo" already exists in stack
			if (!hikeNotifMsgStack.isEmpty())
			{
				if (!hikeNotifMsgStack.isFromSingleMsisdn() || hikeNotifMsgStack.getSize() > 1)
				{
					return;
				}
				else
				{
					if (hikeNotifMsgStack.getSize() == 1)
					{
						// The only message added was the one for which we now have a big picture
						// Hence remove it and proceed showing the big pic notification
						hikeNotifMsgStack.resetMsgStack();
					}
				}
			}

			// if notification message stack is empty, add to it and proceed with single notification display
			// else add to stack and notify clubbed messages
			if (hikeNotifMsgStack.isEmpty())
			{
				hikeNotifMsgStack.addMessage(convMsg.getMsisdn(), message, notificationType);
			}
			else
			{
				notifyStringMessage(convMsg.getMsisdn(), message, false, notificationType);
				return;
			}
			//TODO we are changing it for temporary please correct this next release as soon as possible (forceBlockNotificationSound=true)
			forceBlockNotificationSound=true;
			// big picture messages ! intercept !
			showNotification(notificationIntent, icon, timestamp, notificationId, text, key, message, msisdn, bigPictureImage, !convMsg.isStickerMessage(), isPin, false, hikeNotifMsgStack.getNotificationSubText(),
					Utils.getAvatarDrawableForNotification(context, msisdn, isPin), forceBlockNotificationSound, 0,isSilentNotification(convMsg),true);
		}
		else
		{
			// if notification message stack is empty, add to it and proceed with single notification display
			// else add to stack and notify clubbed messages
			if (hikeNotifMsgStack.isEmpty())
			{
				hikeNotifMsgStack.addMessage(convMsg.getMsisdn(), message, NotificationType.OTHER);
			}
			else
			{
				notifyStringMessage(convMsg.getMsisdn(), message, false, NotificationType.OTHER);
				return;
			}
			// regular message
			showNotification(notificationIntent, icon, timestamp, HIKE_SUMMARY_NOTIFICATION_ID, text, key, message, msisdn, null, isPin, forceBlockNotificationSound,isSilentNotification(convMsg));
		}
	}
	
	/**
	 * This method will also update last Notification Time 
	 * Message will be silent for t (Server configurable) interval for Each Group independently
	 */
	public boolean isSilentNotification(ConvMessage convMsg){
		return isSilentNotification(convMsg.getMsisdn());
	}
	
	/**
	 * This method will also update last Notification Time 
	 * Message will be silent for t (Server configurable) interval for Each Group independently
	 * and all oneToOne will be silent for t (Server configurable) for all oneToOne 
	 */
	public boolean isSilentNotification(String  msisdn){
		if (TextUtils.isEmpty(msisdn))
		{
			return false;
		}
		
		long timeIntervalInMillis;
		Long lastNotificationTime ;
		boolean isGroupMessage=OneToNConversationUtils.isGroupConversation(msisdn);
		if (isGroupMessage)
		{
			timeIntervalInMillis = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.NOTIFIACTION_DELAY_GROUP,DEFAULT_NOTIFICATION_DELAY_FOR_GROUP_IN_SEC)*1000;
			lastNotificationTime = lastNotificationTimeMap.get(msisdn);
		}else{
			//for one to one
			timeIntervalInMillis = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.NOTIFIACTION_DELAY_ONE_TO_ONE,DEFAULT_NOTIFICATION_DELAY_FOR_ONE_TO_ONE_IN_SEC)*1000;
			lastNotificationTime = lastNotificationPlayedTimeForOneToOne;
		}
		if (lastNotificationTime!=null && (System.currentTimeMillis() - lastNotificationTime) < timeIntervalInMillis)
		{
			return true;
		}

		if (isGroupMessage)
		{
			lastNotificationTimeMap.put(msisdn, System.currentTimeMillis());
		}else{
			lastNotificationPlayedTimeForOneToOne = System.currentTimeMillis();
		}

		return false;
	}

	public void notifyStringMessage(String msisdn, String message, boolean forceNotPlaySound, int notificationType)
	{
		notifyStringMessage(msisdn, message, forceNotPlaySound, notificationType,false);
	}
	
	public void notifyStringMessage(String msisdn, String message, boolean forceNotPlaySound, int notificationType,boolean isSilentNotification)
	{
		try
		{
			hikeNotifMsgStack.addMessage(msisdn, message, notificationType);
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
			return;
		}

		hikeNotifMsgStack.invalidateConvMsgList();
		
		boolean isSingleMsisdn = hikeNotifMsgStack.isFromSingleMsisdn();

		Drawable avatarDrawable = null;
		if (isSingleMsisdn)
		{
			avatarDrawable = Utils.getAvatarDrawableForNotification(context, msisdn, false);
		}

		if (hikeNotifMsgStack.getSize() == 1)
		{
			showBigTextStyleNotification(hikeNotifMsgStack.getNotificationIntent(), hikeNotifMsgStack.getNotificationIcon(), hikeNotifMsgStack.getLatestAddedTimestamp(),
					hikeNotifMsgStack.getNotificationId(), hikeNotifMsgStack.getNotificationTickerText(), hikeNotifMsgStack.getNotificationTitle(),
					hikeNotifMsgStack.getNotificationBigText(0), isSingleMsisdn ? hikeNotifMsgStack.lastAddedMsisdn : "bulk", hikeNotifMsgStack.getNotificationSubText(),
					avatarDrawable, forceNotPlaySound, 0,null,isSilentNotification);

		}
		else
		{
			showInboxStyleNotification(hikeNotifMsgStack.getNotificationIntent(), hikeNotifMsgStack.getNotificationIcon(), hikeNotifMsgStack.getLatestAddedTimestamp(),
					hikeNotifMsgStack.getNotificationId(), hikeNotifMsgStack.getNotificationTickerText(), hikeNotifMsgStack.getNotificationTitle(),
					hikeNotifMsgStack.getNotificationBigText(0), isSingleMsisdn ? hikeNotifMsgStack.lastAddedMsisdn : "bulk", hikeNotifMsgStack.getNotificationSubText(),
					avatarDrawable, hikeNotifMsgStack.getBigTextList(), forceNotPlaySound, 0,isSilentNotification);
		}
	}

	public void notifySummaryMessage(final ArrayList<ConvMessage> convMessagesList)
	{
		hikeNotifMsgStack.addConvMessageList(convMessagesList);
		
		showNotificationForCurrentMsgStack(hikeNotifMsgStack.forceBlockNotificationSound());
	}
	
	public void showNotificationForCurrentMsgStack(boolean shouldNotPlaySound)
	{
		showNotificationForCurrentMsgStack(shouldNotPlaySound, 0);
	}
	/**
	 * Sends a notification for all the currently added messages in hikeNotifMsgStack
	 */
	public void showNotificationForCurrentMsgStack(boolean shouldNotPlaySound, int retryCount)
	{
		Logger.d("NotificationRetry","showNotificationForCurrentMsgStack called");
		
		hikeNotifMsgStack.invalidateConvMsgList();

		boolean isSingleMsisdn = hikeNotifMsgStack.isFromSingleMsisdn();

		Drawable avatarDrawable = null;
		if (isSingleMsisdn)
		{
			avatarDrawable = Utils.getAvatarDrawableForNotification(context, hikeNotifMsgStack.lastAddedMsisdn, false);
		}

		// Possibility to show big picture message
		ConvMessage convMessage = hikeNotifMsgStack.getLastInsertedConvMessage();
					
		if (hikeNotifMsgStack.getSize() == 1 && convMessage != null)
		{
			boolean isSilentNotification=isSilentNotification(convMessage);
			if (convMessage.isInvite())
			{
				return;
			}
			
			else if (convMessage.isStickerMessage())
			{
				Bitmap bigPictureImage = ToastListener.returnBigPicture(convMessage, context);
				if (bigPictureImage != null)
				{
					HAManager.getInstance().setMetadatFieldsForSessionEvent(AnalyticsConstants.AppOpenSource.FROM_NOTIFICATION, convMessage.getMsisdn(), convMessage,
							AnalyticsConstants.ConversationType.NORMAL);
					
					showNotification(hikeNotifMsgStack.getNotificationIntent(), hikeNotifMsgStack.getNotificationIcon(), hikeNotifMsgStack.getLatestAddedTimestamp(),
							hikeNotifMsgStack.getNotificationId(), hikeNotifMsgStack.getNotificationTickerText(), hikeNotifMsgStack.getNotificationTitle(),
							hikeNotifMsgStack.getNotificationBigText(retryCount), convMessage.getMsisdn(), bigPictureImage, !convMessage.isStickerMessage(), false, false,
							hikeNotifMsgStack.getNotificationSubText(), null, shouldNotPlaySound, retryCount,isSilentNotification);
					return;
				}
			}
			else if(convMessage.isVoipMissedCallMsg())
			{
				NotificationCompat.Action[] actions = VoIPUtils.getMissedCallNotifActions(context, convMessage.getMsisdn());
				showBigTextStyleNotification(hikeNotifMsgStack.getNotificationIntent(), hikeNotifMsgStack.getNotificationIcon(), hikeNotifMsgStack.getLatestAddedTimestamp(),
						VOIP_MISSED_CALL_NOTIFICATION_ID, hikeNotifMsgStack.getNotificationTickerText(), hikeNotifMsgStack.getNotificationTitle(),
						hikeNotifMsgStack.getNotificationBigText(retryCount), isSingleMsisdn ? hikeNotifMsgStack.lastAddedMsisdn : "bulk", hikeNotifMsgStack.getNotificationSubText(),
						avatarDrawable, shouldNotPlaySound, retryCount, actions,isSilentNotification);
				return;
			}

			HAManager.getInstance().setMetadatFieldsForSessionEvent(AnalyticsConstants.AppOpenSource.FROM_NOTIFICATION, convMessage.getMsisdn(), convMessage,
						AnalyticsConstants.ConversationType.NORMAL);

			showBigTextStyleNotification(hikeNotifMsgStack.getNotificationIntent(), hikeNotifMsgStack.getNotificationIcon(), hikeNotifMsgStack.getLatestAddedTimestamp(),
					hikeNotifMsgStack.getNotificationId(), hikeNotifMsgStack.getNotificationTickerText(), hikeNotifMsgStack.getNotificationTitle(),
					hikeNotifMsgStack.getNotificationBigText(retryCount), isSingleMsisdn ? hikeNotifMsgStack.lastAddedMsisdn : "bulk", hikeNotifMsgStack.getNotificationSubText(),
					avatarDrawable, shouldNotPlaySound, retryCount,null,isSilentNotification);

		}
		else if (!hikeNotifMsgStack.isEmpty())
		{
			
			if (convMessage != null)
			{
				HAManager.getInstance().setMetadatFieldsForSessionEvent(AnalyticsConstants.AppOpenSource.FROM_NOTIFICATION, convMessage.getMsisdn(), convMessage,
						AnalyticsConstants.ConversationType.NORMAL);
			}
			showInboxStyleNotification(hikeNotifMsgStack.getNotificationIntent(), hikeNotifMsgStack.getNotificationIcon(), hikeNotifMsgStack.getLatestAddedTimestamp(),
					hikeNotifMsgStack.getNotificationId(), hikeNotifMsgStack.getNotificationTickerText(), hikeNotifMsgStack.getNotificationTitle(),
					hikeNotifMsgStack.getNotificationBigText(retryCount), isSingleMsisdn ? hikeNotifMsgStack.lastAddedMsisdn : "bulk", hikeNotifMsgStack.getNotificationSubText(),
					avatarDrawable, hikeNotifMsgStack.getBigTextList(), shouldNotPlaySound, retryCount, (convMessage == null) ? true : isSilentNotification(convMessage));
		}

		// serializeObject();
		
	}

	public void notifyHikeToOfflinePush(ArrayList<String> msisdnList, HashMap<String, String> nameMap, int notificationType)
	{
		/*
		 * return straight away if the block notification setting is ON
		 */
		if (sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			return;
		}

		final int notificationId = HIKE_TO_OFFLINE_PUSH_NOTIFICATION_ID;
		final Intent notificationIntent = new Intent(context, ChatThreadActivity.class);

		String firstMsisdn = msisdnList.get(0);
		notificationIntent.putExtra(HikeConstants.Extras.MSISDN, (firstMsisdn));
		notificationIntent.putExtra(HikeConstants.Extras.NAME, (nameMap.get(firstMsisdn)));
		notificationIntent.putExtra(HikeConstants.Extras.WHICH_CHAT_THREAD, ChatThreadUtils.getChatThreadType(firstMsisdn));
		notificationIntent.putExtra(HikeConstants.Extras.CHAT_INTENT_TIMESTAMP, System.currentTimeMillis());
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		notificationIntent.setData((Uri.parse("custom://" + notificationId)));
		final Drawable avatarDrawable = context.getResources().getDrawable(R.drawable.offline_notification);
		final int smallIconId = returnSmallIcon();

		String title = (msisdnList.size() > 1) ? context.getString(R.string.hike_to_offline_push_title_multiple, msisdnList.size()) : (StealthModeManager.getInstance()
				.isStealthMsisdn(firstMsisdn) ? context.getString(R.string.stealth_notification_message) : context.getString(R.string.hike_to_offline_push_title_single,
				nameMap.get(firstMsisdn)));
		String message = context.getString(R.string.hike_to_offline_text);

		// if notification message stack is empty, add to it and proceed with single notification display
		// else add to stack and notify clubbed messages
		if (hikeNotifMsgStack.isEmpty())
		{
			hikeNotifMsgStack.addMessage(context.getString(R.string.app_name), title + ": " + message, notificationType);
		}
		else
		{
			notifyStringMessage(context.getString(R.string.app_name), title + ": " + message, false, notificationType);
			return;
		}

		NotificationCompat.Builder mBuilder = getNotificationBuilder(title, message, message, avatarDrawable, smallIconId, false);
		setNotificationIntentForBuilder(mBuilder, notificationIntent, HIKE_TO_OFFLINE_PUSH_NOTIFICATION_ID);

		notifyNotification(notificationId, mBuilder);
		
		notificationBuilderPostWork();

	}

	public void notifyStealthMessage(int notificationType, String msisdn)
	{
		final int notificationId = STEALTH_NOTIFICATION_ID;

		String message = context.getString(R.string.stealth_notification_message);
		String key = HIKE_STEALTH_MESSAGE_KEY;

		Boolean stealthNotificationEnabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.STEALTH_NOTIFICATION_ENABLED, true);
		Boolean stealthIndicatorEnabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.STEALTH_INDICATOR_ENABLED, false);
	
		if(stealthIndicatorEnabled)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STEALTH_INDICATOR_ENABLED, true);
			HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_INDICATOR, null);
		}
		/*
		 * return straight away if the block notification setting is ON
		 */
		if (sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false) || !stealthNotificationEnabled)
		{
			return;
		}

		boolean isSilentNotification =isSilentNotification(msisdn);

		// if notification message stack is empty, add to it and proceed with single notification display
		// else add to stack and notify clubbed messages
		if (hikeNotifMsgStack.isEmpty())
		{
			hikeNotifMsgStack.addMessage(key, message, notificationType);
		}
		else
		{
			notifyStringMessage(key, message, false, notificationType,isSilentNotification);
			return;
		}

		// we've got to invoke the timeline here
		final Intent notificationIntent = Utils.getHomeActivityIntent(context);
		notificationIntent.setData((Uri.parse("custom://" + notificationId)));

		final int smallIconId = returnSmallIcon();
		NotificationCompat.Builder mBuilder = getNotificationBuilder(context.getString(R.string.app_name), message, message, null, smallIconId, false,isSilentNotification);
		setNotificationIntentForBuilder(mBuilder, notificationIntent,STEALTH_NOTIFICATION_ID);
		notifyNotification(notificationId, mBuilder);
		notificationBuilderPostWork();
	}

	public void notifyFavorite(final ContactInfo contactInfo, int notificationType)
	{
		/*
		 * return straight away if the block notification setting is ON
		 */
		if (sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			return;
		}

		final int notificationId = HIKE_SUMMARY_NOTIFICATION_ID;

		final String msisdn = contactInfo.getMsisdn();

		final long timeStamp = System.currentTimeMillis() / 1000;

		final Intent notificationIntent = Utils.getPeopleActivityIntent(context);
		notificationIntent.setData((Uri.parse("custom://" + notificationId)));

		final int icon = returnSmallIcon();

		final String key = (contactInfo != null && !TextUtils.isEmpty(contactInfo.getName())) ? contactInfo.getName() : msisdn;

		final String message = context.getString(R.string.add_as_favorite_notification_line);

		final String text = context.getString(R.string.add_as_favorite_notification, key);

		// if notification message stack is empty, add to it and proceed with single notification display
		// else add to stack and notify clubbed messages
		if (hikeNotifMsgStack.isEmpty())
		{
			hikeNotifMsgStack.addMessage(contactInfo.getMsisdn(), message, notificationType);
		}
		else
		{
			notifyStringMessage(contactInfo.getMsisdn(), message, false, notificationType);
			return;
		}

		showNotification(notificationIntent, icon, timeStamp, notificationId, text, key, message, msisdn, null, false, false);
		addNotificationId(notificationId);
	}

	public void notifyStatusMessage(final StatusMessage statusMessage, int notificationType)
	{

		/*
		 * We only proceed if the current status preference value is 0 which denotes that the user wants immediate notifications. Else we simply return
		 */
		if (PreferenceManager.getDefaultSharedPreferences(this.context).getInt(HikeConstants.STATUS_PREF, 0) != 0)
		{
			return;
		}
		// final int notificationId = statusMessage.getMsisdn().hashCode();

		final int notificationId = HIKE_SUMMARY_NOTIFICATION_ID;

		final long timeStamp = statusMessage.getTimeStamp();

		final Intent notificationIntent = Utils.getTimelineActivityIntent(context);
		notificationIntent.setData((Uri.parse("custom://" + notificationId)));

		final int icon = returnSmallIcon();

		final String key = statusMessage.getNotNullName();

		String message = null;
		String text = null;
		if (statusMessage.getStatusMessageType() == StatusMessageType.TEXT)
		{
			message = context.getString(R.string.status_text_notification, "\"" + statusMessage.getText() + "\"");
			/*
			 * Jellybean has added support for emojis so we don't need to add a '*' to replace them
			 */
			if (Build.VERSION.SDK_INT < 16)
			{
				// Replace emojis with a '*'
				message = SmileyParser.getInstance().replaceEmojiWithCharacter(message, "*");
			}
			text = key + " " + message;
		}
		else if (statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST_ACCEPTED)
		{
			String infoSubText = context.getString(Utils.isLastSeenSetToFavorite() ? R.string.both_ls_status_update : R.string.status_updates_proper_casing);
			message = context.getString(R.string.favorite_confirmed_notification, key, infoSubText);
			text = message;
		}
		else if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
		{
			message = context.getString(R.string.status_profile_pic_notification, key);
			text = key + " " + message;
		}
		else if (statusMessage.getStatusMessageType() == StatusMessageType.IMAGE || statusMessage.getStatusMessageType() == StatusMessageType.TEXT_IMAGE)
		{
			message = context.getString(R.string.notif_posted_photo);
			text = key + " " + message;
		}
		else
		{
			/*
			 * We don't know how to display this type. Just return.
			 */
			return;
		}

		// if notification message stack is empty, add to it and proceed with single notification display
		// else add to stack and notify clubbed messages
		if (hikeNotifMsgStack.isEmpty())
		{
			hikeNotifMsgStack.addMessage(statusMessage.getMsisdn(), message, notificationType);
		}
		else
		{
			notifyStringMessage(statusMessage.getMsisdn(), message, true, notificationType);
			return;
		}

		showNotification(notificationIntent, icon, timeStamp, notificationId, text, key, message, statusMessage.getMsisdn(), null, false, true);
		addNotificationId(notificationId);
	}

	public void notifyBigPictureStatusNotification(final String imagePath, final String msisdn, final String name, int notificationType)
	{
		if (PreferenceManager.getDefaultSharedPreferences(this.context).getInt(HikeConstants.STATUS_PREF, 0) != 0)
		{
			return;
		}

		final int notificationId = HIKE_SUMMARY_NOTIFICATION_ID;
		final String key = TextUtils.isEmpty(name) ? msisdn : name;

		String message = null;

		if (notificationType == NotificationType.DPUPDATE)
		{
			message = context.getString(R.string.status_profile_pic_notification);
		}
		else if (notificationType == NotificationType.IMAGE_POST)
		{
			message = context.getString(R.string.posted_photo);
		}
		else
		{
			// Type not covered
			return;
		}

		final String text = key + " " + message;

		final int icon = returnSmallIcon();
		final Intent notificationIntent = Utils.getTimelineActivityIntent(context);
		final Bitmap bigPictureImage = HikeBitmapFactory.decodeBitmapFromFile(imagePath, Bitmap.Config.RGB_565);
		notificationIntent.setData((Uri.parse("custom://" + notificationId)));
		notificationIntent.putExtra(HikeConstants.Extras.MSISDN, msisdn.toString());

		// if big picture exists in the new message, check notification stack if we are showing clubbed messages
		// if we are, discard big picture since a message for it "..sent you a photo" already exists in stack
		if (!hikeNotifMsgStack.isEmpty())
		{
			if (!hikeNotifMsgStack.isFromSingleMsisdn() || hikeNotifMsgStack.getSize() > 1)
			{
				return;
			}
			else
			{
				if (hikeNotifMsgStack.getSize() == 1)
				{
					// The only message added was the one for which we now have a big picture
					// Hence remove it and proceed showing the big pic notification
					hikeNotifMsgStack.resetMsgStack();
				}
			}
		}

		// if notification message stack is empty, add to it and proceed with single notification display
		// else add to stack and notify clubbed messages
		if (hikeNotifMsgStack.isEmpty())
		{
			hikeNotifMsgStack.addMessage(msisdn, message, NotificationType.DPUPDATE);
		}
		else
		{
			notifyStringMessage(msisdn, message, true, NotificationType.DPUPDATE);
			return;
		}

		showNotification(notificationIntent, icon, System.currentTimeMillis(), notificationId, text, key, message, msisdn, bigPictureImage, false, true);
	}

	public void notifyBatchUpdate(final String header, final String message, int notificationType)
	{

		final long timeStamp = System.currentTimeMillis() / 1000;

		final int notificationId = (int) timeStamp;

		final Intent notificationIntent = Utils.getTimelineActivityIntent(context);
		notificationIntent.setData((Uri.parse("custom://" + notificationId)));

		final int icon = returnSmallIcon();

		final String key = header;

		final String text = message;
		// if notification message stack is empty, add to it and proceed with single notification display
		// else add to stack and notify clubbed messages
		if (hikeNotifMsgStack.isEmpty())
		{
			hikeNotifMsgStack.addMessage(context.getString(R.string.app_name), text, notificationType);
		}
		else
		{
			notifyStringMessage(context.getString(R.string.app_name), text, false, notificationType);
			return;
		}
		showNotification(notificationIntent, icon, timeStamp, notificationId, text, key, message, null, null, false, false); // TODO: change this.
		addNotificationId(notificationId);
	}

	private void addNotificationId(final int id)
	{
		String ids = sharedPreferences.getString(HikeMessengerApp.STATUS_IDS, "");

		ids += Integer.toString(id) + SEPERATOR;

		final Editor editor = sharedPreferences.edit();
		editor.putString(HikeMessengerApp.STATUS_IDS, ids);
		editor.commit();
	}

	public void cancelAllStatusNotifications()
	{
		final String ids = sharedPreferences.getString(HikeMessengerApp.STATUS_IDS, "");
		final String[] idArray = ids.split(SEPERATOR);

		for (final String id : idArray)
		{
			if (TextUtils.isEmpty(id.trim()))
			{
				continue;
			}
			notificationManager.cancel(Integer.parseInt(id));
		}

		final Editor editor = sharedPreferences.edit();
		editor.remove(HikeMessengerApp.STATUS_IDS);
		editor.commit();
	}

	public void cancelAllNotifications()
	{
		notificationManager.cancelAll();
		hikeNotifMsgStack.resetMsgStack();
	}
	
	public void checkAndShowUpdateNotif()
	{
		if(settingPref.getBoolean(HikeConstants.SHOULD_SHOW_PERSISTENT_NOTIF, false) && !settingPref.getBoolean(HikeConstants.IS_PERS_NOTIF_ALARM_SET, false))
		{
			if(Utils.isUpdateRequired(settingPref.getString(HikeConstants.Extras.LATEST_VERSION, ""), context))
			{
				Logger.d("UpdateTipPersistentNotif", "Recreating persistent notif for target version:"+settingPref.getString(HikeConstants.Extras.LATEST_VERSION, ""));
				String message = settingPref.getString(HikeConstants.PERSISTENT_NOTIF_MESSAGE, context.getResources().getString(R.string.pers_notif_message));
				String title = settingPref.getString(HikeConstants.PERSISTENT_NOTIF_TITLE, context.getResources().getString(R.string.pers_notif_title));
				String action = settingPref.getString(HikeConstants.PERSISTENT_NOTIF_ACTION, context.getResources().getString(R.string.pers_notif_action));
				String later = settingPref.getString(HikeConstants.PERSISTENT_NOTIF_LATER, context.getResources().getString(R.string.pers_notif_later));
				Uri url = Uri.parse(settingPref.getString(HikeConstants.PERSISTENT_NOTIF_URL, "market://details?id=" + context.getPackageName()));
				Long interval = settingPref.getLong(HikeConstants.PERSISTENT_NOTIF_ALARM, HikeConstants.PERS_NOTIF_ALARM_DEFAULT);
				notifyPersistentUpdate(title, message,action, later, url, interval);
			}
		}
		
	}
	
	public void cancelPersistNotif()
	{
		notificationManager.cancel(PERSISTENT_NOTIF_ID);
	}

	private void showInboxStyleNotification(final Intent notificationIntent, final int icon, final long timestamp, final int notificationId, final CharSequence text,
			final String key, final String message, final String msisdn, String subMessage, Drawable argAvatarDrawable, List<SpannableString> inboxLines, boolean shouldNotPlaySound, int retryCount,boolean isSilentNotification)
	{

		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder;
		mBuilder = null;
		mBuilder = getNotificationBuilder(key, TextUtils.isEmpty(subMessage) ? message : subMessage, text.toString(), argAvatarDrawable, smallIconId, shouldNotPlaySound,isSilentNotification);
		NotificationCompat.InboxStyle inBoxStyle = new NotificationCompat.InboxStyle();
		inBoxStyle.setBigContentTitle(key);
		inBoxStyle.setSummaryText(subMessage);

		// Moves events into the big view
		for (int i = 0; i < inboxLines.size(); i++)
		{
			inBoxStyle.addLine(inboxLines.get(i));
		}

		// Moves the big view style object into the notification object.
		mBuilder.setStyle(inBoxStyle);
		
		//Handling separately for NM Bots because for NM bots, on pressing back/up, the user must be brought to the conversation list. (AND-2692)
		if (BotUtils.isBot(msisdn) && BotUtils.getBotInfoForBotMsisdn(msisdn).isNonMessagingBot())
		{
				setNotificationIntentForBuilderWithBackStack(mBuilder, notificationIntent, notificationId, retryCount);
		}
		else
		{
			setNotificationIntentForBuilder(mBuilder, notificationIntent,notificationId,retryCount);
		}

		if (!sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			notifyNotification(notificationId, mBuilder);
		}
		notificationBuilderPostWork();
	}

	public void showBigTextStyleNotification(final Intent notificationIntent, final int icon, final long timestamp, final int notificationId, final CharSequence text,
			final String key, final String message, final String msisdn, String subMessage, Drawable argAvatarDrawable, boolean shouldNotPlaySound, int retryCount)
	{
		showBigTextStyleNotification(notificationIntent, icon, timestamp, notificationId, text, key, message, msisdn, subMessage, argAvatarDrawable, shouldNotPlaySound, retryCount, null);
	}

	public void showBigTextStyleNotification(final Intent notificationIntent, final int icon, final long timestamp, final int notificationId, final CharSequence text,
			final String key, final String message, final String msisdn, String subMessage, Drawable argAvatarDrawable, boolean shouldNotPlaySound, int retryCount, Action[] actions)
	{
		showBigTextStyleNotification(notificationIntent, icon, timestamp, notificationId, text, key, message, msisdn, subMessage, argAvatarDrawable, shouldNotPlaySound, retryCount, actions,false);
	}
	public void showBigTextStyleNotification(final Intent notificationIntent, final int icon, final long timestamp, final int notificationId, final CharSequence text,
			final String key, final String message, final String msisdn, String subMessage, Drawable argAvatarDrawable, boolean shouldNotPlaySound, int retryCount, Action[] actions,boolean isSilentNotification)
	{

		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder;
		mBuilder = null;
		mBuilder = getNotificationBuilder(key, message, text.toString(), argAvatarDrawable, smallIconId, shouldNotPlaySound,isSilentNotification);
		NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
		bigTextStyle.setBigContentTitle(key);
		if(!TextUtils.isEmpty(subMessage))
		{
			bigTextStyle.setSummaryText(subMessage);
		}
		bigTextStyle.bigText(message);

		if(actions != null)
		{
			for(Action action : actions)
			{
				mBuilder.addAction(action);
			}
		}

		// Moves the big view style object into the notification object.
		mBuilder.setStyle(bigTextStyle);
		
		//Handling separately for NM Bots because for NM bots, on pressing back/up, the user must be brought to the conversation list. (AND-2692)
		if (BotUtils.isBot(msisdn) && BotUtils.getBotInfoForBotMsisdn(msisdn).isNonMessagingBot())
		{
				setNotificationIntentForBuilderWithBackStack(mBuilder, notificationIntent, notificationId, retryCount);
		}
		else
		{
			setNotificationIntentForBuilder(mBuilder, notificationIntent,notificationId,retryCount);
		}
		
		if (!sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			notifyNotification(notificationId, mBuilder);
		}
		notificationBuilderPostWork();
	}

	private void showNotification(final Intent notificationIntent, final int icon, final long timestamp, final int notificationId, final CharSequence text, final String key,
			final String message, final String msisdn, final Bitmap bigPictureImage, boolean isFTMessage, boolean isPin, boolean isBigText, String subMessage,
			Drawable argAvatarDrawable, boolean forceNotPlaySound, int retryCount,boolean isSilentNotification,boolean disableTickerText)
	{

		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder;
		if (bigPictureImage != null)
		{
			mBuilder = getNotificationBuilder(key, message, text.toString(), argAvatarDrawable, smallIconId, forceNotPlaySound,isSilentNotification,disableTickerText);
			final NotificationCompat.BigPictureStyle bigPicStyle = new NotificationCompat.BigPictureStyle();
			bigPicStyle.setBigContentTitle(key);
			if(!TextUtils.isEmpty(subMessage))
			{
				bigPicStyle.setSummaryText(subMessage);
			}else{
				bigPicStyle.setSummaryText(message);
			}
			bigPicStyle.bigPicture(bigPictureImage);
			mBuilder.setSubText(subMessage);
			mBuilder.setStyle(bigPicStyle);
		}
		else
		{
			mBuilder = null;
			if (isBigText)
			{
				mBuilder = getNotificationBuilder(key, message, text.toString(), argAvatarDrawable, smallIconId, forceNotPlaySound,isSilentNotification);
				NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
				bigTextStyle.setBigContentTitle(key);
				bigTextStyle.bigText(message);
				if(!TextUtils.isEmpty(subMessage))
				{
					bigTextStyle.setSummaryText(subMessage);
				}
				mBuilder.setStyle(bigTextStyle);
			}
			else
			{
				mBuilder = getNotificationBuilder(key, message, text.toString(), argAvatarDrawable, smallIconId, false,isSilentNotification);
			}
		}

		setNotificationIntentForBuilder(mBuilder, notificationIntent,notificationId,retryCount);
		
		
		
		if (!sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			notifyNotification(notificationId, mBuilder);
		}
		notificationBuilderPostWork();
	}

	private void showNotification(final Intent notificationIntent, final int icon, final long timestamp, final int notificationId, final CharSequence text, final String key,
			final String message, final String msisdn, final Bitmap bigPictureImage, boolean isPin, boolean forceNotPlaySound)
	{
		showNotification(notificationIntent, icon, timestamp, notificationId, text, key, message, msisdn, bigPictureImage, isPin, forceNotPlaySound, false);
	}
	
	private void showNotification(final Intent notificationIntent, final int icon, final long timestamp, final int notificationId, final CharSequence text, final String key,
			final String message, final String msisdn, final Bitmap bigPictureImage, boolean isPin, boolean forceNotPlaySound,boolean isSilentNotification)
	{
		showNotification(notificationIntent, icon, timestamp, notificationId, text, key, message, msisdn, bigPictureImage, false, isPin, true,
				hikeNotifMsgStack.getNotificationSubText(), Utils.getAvatarDrawableForNotification(context, msisdn, isPin), forceNotPlaySound, 0,isSilentNotification);
	}
	private void showNotification(final Intent notificationIntent, final int icon, final long timestamp, final int notificationId, final CharSequence text, final String key,
			final String message, final String msisdn, final Bitmap bigPictureImage, boolean isFTMessage, boolean isPin, boolean isBigText, String subMessage,
			Drawable argAvatarDrawable, boolean forceNotPlaySound, int retryCount,boolean isSilentNotification)
	{
		showNotification(notificationIntent, icon, timestamp, notificationId, text, key, message, msisdn, bigPictureImage, isFTMessage, isPin, isBigText, subMessage,
				argAvatarDrawable, forceNotPlaySound, retryCount, isSilentNotification, false);

	}

	public int returnSmallIcon()
	{
		if (Build.VERSION.SDK_INT < 16)
		{
			return R.drawable.ic_contact_logo;

		}
		else
		{
			return R.drawable.ic_stat_notify;
		}

	}
	
	/*
	 * creates a notification builder with sound, led and vibrate option set according to app preferences. forceNotPlaySound : true if we want to force not to play notification
	 * sounds or lights.
	 */
	public NotificationCompat.Builder getNotificationBuilder(String contentTitle, String contentText, String tickerText, Drawable avatarDrawable, int smallIconId,
			boolean forceNotPlaySound)
	{
		return getNotificationBuilder(contentTitle, contentText, tickerText, avatarDrawable, smallIconId, forceNotPlaySound,false);
	}
	
	/*
	 * creates a notification builder with sound, led and vibrate option set according to app preferences. forceNotPlaySound : true if we want to force not to play notification
	 * sounds or lights.
	 */
	public NotificationCompat.Builder getNotificationBuilder(String contentTitle, String contentText, String tickerText, Drawable avatarDrawable, int smallIconId,
			boolean forceNotPlaySound,boolean isSilentNotification)
	{
		return getNotificationBuilder(contentTitle, contentText, tickerText, avatarDrawable, smallIconId, forceNotPlaySound,isSilentNotification,false);
	}
	/*
	 * creates a notification builder with sound, led and vibrate option set according to app preferences. forceNotPlaySound : true if we want to force not to play notification
	 * sounds or lights.
	 */
	public NotificationCompat.Builder getNotificationBuilder(String contentTitle, String contentText, String tickerText, Drawable avatarDrawable, int smallIconId,
			boolean forceNotPlaySound,boolean isSilentNotification,boolean disbleTickerText)
	{
		
		final SharedPreferences preferenceManager = PreferenceManager.getDefaultSharedPreferences(this.context);
		String vibrate = preferenceManager.getString(HikeConstants.VIBRATE_PREF_LIST, VIB_DEF);
		final Bitmap avatarBitmap = HikeBitmapFactory.getCircularBitmap(HikeBitmapFactory.returnScaledBitmap((HikeBitmapFactory.drawableToBitmap(avatarDrawable, Bitmap.Config.RGB_565)), context));
		
		// Check the current notification priority
		// notifPriority(0) = PRIORITY_DEFAULT
		// notifPriority(-2) = PRIORITY_MIN
		// notifPriority(2) = PRIORITY_MAX
		// notifPriority(-1) = PRIORITY_LOW
		// notifPriority(1) = PRIORITY_HIGH
		int notifPriority = HikeSharedPreferenceUtil.getInstance().getPref().getInt(HikeConstants.NOTIFICATIONS_PRIORITY, NotificationCompat.PRIORITY_DEFAULT);		
		
		final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context).setContentTitle(contentTitle).setSmallIcon(smallIconId).setLargeIcon(avatarBitmap)
				.setContentText(contentText).setAutoCancel(true).setPriority(notifPriority)
				.setCategory(NotificationCompat.CATEGORY_MESSAGE).setColor(context.getResources().getColor(R.color.blue_hike));
		
		
		if (!disbleTickerText)
		{
			if (!TextUtils.isEmpty(tickerText) && tickerText.length()>TICKER_TEXT_MAX_LENGHT+3)
			{  // we are trimming ticker text so that it will not scroll in status bar.
				tickerText=tickerText.substring(0, TICKER_TEXT_MAX_LENGHT)+"...";
			}
			mBuilder.setTicker(tickerText);
		}
		
		// Reset ticker text since we dont want to tick older messages
		hikeNotifMsgStack.setTickerText(null);
		
		if (!forceNotPlaySound)
		{
			//Play (SOUND + VIBRATION) ONLY WHEN for 
			//1) User is not in audio/video/Voip call....
			// (2nd check is a safe check as this should be handled by NotificationBuilder itself)
			//2) There should not be any voip action running(Calling/Connected) 
			boolean isUserNotOnCall = !Utils.isUserInAnyTypeOfCall(context) && VoIPService.getCallId() <= 0;
			boolean isLollipopAndAbove=Utils.isLollipopOrHigher();
			String notifSound = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.NOTIF_SOUND_PREF, NOTIF_SOUND_HIKE);
			Logger.i("notif", "sound " + notifSound);

			// Decide if Sound is to be played,
			// 1) Settings should be On
			if (!NOTIF_SOUND_OFF.equals(notifSound) && !isSilentNotification)
			{
				if (isLollipopAndAbove)
				{
					playSoundViaBuilder(mBuilder, notifSound);
				}
				// Decide if Sound is to be played,for lower version than lollipop 
				// 1) UserNotOnCall
				// 2) Mode should not be in Silent and Not in Vibrate
				// 3) Notification volume is > 0
				else if (isUserNotOnCall && !SoundUtils.isSilentOrVibrateMode(context) && !SoundUtils.isNotificationStreamVolZero(context))
				{
					if (isAudioServiceBusy())
					{
						playSoundViaPlayer(notifSound);
					}
					else
					{
						playSoundViaBuilder(mBuilder, notifSound);
					}
				}
			}
			// Though Notification Builder should not vibrate if phone is in silent mode,
			// But in some device (Micromax A110), it is vibrating, so we are adding extra
			// safe check here to ensure that it does not vibrate in silent mode
			// Now Vibration is turned off in these 2 scenarios
			// 1) Vibration Settings are off
			// 2) Either Lollipop and above OR Phone is in silent mode
			if (!VIB_OFF.equals(vibrate) && !isSilentNotification && (isLollipopAndAbove || (!SoundUtils.isSilentMode(context) && isUserNotOnCall)))
			{
				if (VIB_DEF.equals(vibrate))
				{
					mBuilder.setDefaults(mBuilder.getNotification().defaults | Notification.DEFAULT_VIBRATE);
				}
				else if (VIB_SHORT.equals(vibrate))
				{
					// short vibrate
					mBuilder.setVibrate(HikeConstants.SHORT_VIB_PATTERN);
				}
				else if (VIB_LONG.equals(vibrate))
				{
					// long vibrate
					mBuilder.setVibrate(HikeConstants.LONG_VIB_PATTERN);
				}
			}
			
			int ledColor = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.LED_NOTIFICATION_COLOR_CODE, HikeConstants.LED_DEFAULT_WHITE_COLOR);
		
			if(ledColor != HikeConstants.LED_NONE_COLOR)
			{
				mBuilder.setLights(ledColor, HikeConstants.LED_LIGHTS_ON_MS, HikeConstants.LED_LIGHTS_OFF_MS);
			}
		}
		return mBuilder;
	}

	private void notifyNotification(int notificationId, Builder builder)
 	{
		notificationManager.notify(notificationId, builder.build());
	}

	public boolean isAudioServiceBusy(){
		// We are considering that Audio service will be busy for follwing conditions:  
		// CASE 1:- If Music Is Playing, then play via Ringtone manager on Music Stream
		// controlled via Music Volume Stream
		// CASE 2:- If wireless/wired handsfree is connected
		//Now We have to play sound ourself via RingtoneManager for above cases,
		AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		return manager.isMusicActive() || manager.isWiredHeadsetOn() || manager.isBluetoothA2dpOn() || (manager.isBluetoothScoAvailableOffCall() && manager
				.isBluetoothScoOn());
	}

	public void setNotificationIntentForBuilder(NotificationCompat.Builder mBuilder, Intent notificationIntent,int notificationId)
	{
		setNotificationIntentForBuilder(mBuilder, notificationIntent, notificationId,0);
	}
	
	public void setNotificationIntentForBuilder(NotificationCompat.Builder mBuilder, Intent notificationIntent,int notificationId,int retryCount)
	{
		//Adding Extra to check While receiving that user has come via clicking Notification
		notificationIntent.putExtra(AnalyticsConstants.APP_OPEN_SOURCE_EXTRA, AppOpenSource.FROM_NOTIFICATION);
        
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(contentIntent);
		
		setOnDeleteIntent(mBuilder, notificationId, retryCount);
	}
	
	public void setNotificationIntentForBuilderWithBackStack(NotificationCompat.Builder mBuilder, Intent notificationIntent,int notificationId,int retryCount)
	{
		//Adding Extra to check While receiving that user has come via clicking Notification
		notificationIntent.putExtra(AnalyticsConstants.APP_OPEN_SOURCE_EXTRA, AppOpenSource.FROM_NOTIFICATION);
		
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        
        stackBuilder.addNextIntent(Utils.getHomeActivityIntent(context));
        stackBuilder.addNextIntent(notificationIntent);
        
        PendingIntent contentIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(contentIntent);
		
		setOnDeleteIntent(mBuilder, notificationId, retryCount);
	}
	
	/**
	 * Add action to a notification builder object. This will add action buttons to the built notification. More at <a
	 * href="http://developer.android.com/guide/topics/ui/notifiers/notifications.html#Actions">Notification Actions</a>
	 * 
	 * @param notificationBuilder
	 * @param icon
	 * @param title
	 * @param actionIntent
	 * @return
	 */
	public NotificationCompat.Builder addNotificationActions(NotificationCompat.Builder notificationBuilder, int icon, CharSequence title, PendingIntent actionIntent)
	{
		notificationBuilder.addAction(icon, title, actionIntent);
		return notificationBuilder;
	}

	/**
	 * Set on delete intent for notifications. This is required in-order to perform actions on notification dismissed/deleted.
	 * 
	 * @param mBuilder
	 * @param notificationId
	 * @return
	 */
	public NotificationCompat.Builder setOnDeleteIntent(NotificationCompat.Builder mBuilder, int notificationId, int retryCount)
	{
		Intent intent = new Intent(context, NotificationDismissedReceiver.class);
		intent.putExtra(HIKE_NOTIFICATION_ID_KEY, notificationId);
		intent.putExtra(HikeConstants.RETRY_COUNT, retryCount);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setDeleteIntent(pendingIntent);

		return mBuilder;
	}
	public long getNextRetryNotificationTime(int retryCount)
	{
		long nextRetryTime = System.currentTimeMillis() + calculateNextRetryNotificationTime(retryCount);
		
		/*
		 * We have a sleep state from 12am - 8am. If the timer is finished in this time frame then 
		 * we wait for the sleep state to get over before showing the local push.
		 */
		Calendar calendar = Calendar.getInstance();
		long toDay12AM = Utils.getTimeInMillis(calendar, 0, 0, 0, 0);
		long toDay7AM = Utils.getTimeInMillis(calendar, 7, 0, 0, 0);
		
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		long nextDay12AM = Utils.getTimeInMillis(calendar, 0, 0, 0, 0);
		long nextDay7AM = Utils.getTimeInMillis(calendar, 7, 0, 0, 0);
		if(nextRetryTime >= toDay12AM && nextRetryTime < toDay7AM)
		{
			nextRetryTime = toDay7AM;
		}
		else if(nextRetryTime >= nextDay12AM && nextRetryTime < nextDay7AM)
		{
			nextRetryTime = nextDay7AM;
		}
		Logger.i("HikeNotification", "currtime = "+ System.currentTimeMillis() + "  nextDay12AM = "+nextDay12AM+ "  nextDay8AM = "+nextDay7AM + "  toDay8AM = "+toDay7AM + " finalRetryTime = "+ nextRetryTime);
		return nextRetryTime;
	}
	
	public long calculateNextRetryNotificationTime(int retryCount)
	{
		int i = retryCount + 1;
		long t = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.RETRY_NOTIFICATION_COOL_OFF_TIME, HikeConstants.DEFAULT_RETRY_NOTIF_TIME);
		
		//2 power (retryCount - 1)
		Random rand = new Random();
		int randomNumber = rand.nextInt(i);
		int multiplyFactor = (int) Math.pow(2, randomNumber);
		
		Logger.d("HikeNotification", " t = "+t + " retryCount = "+i + " randomNumber = "+ randomNumber + " multiplyFactor = " + multiplyFactor);
		return multiplyFactor*t;
	}
	
	/**
	 * In this method we can put all the work which we need to do at the end of showing a notification
	 */
	private void notificationBuilderPostWork()
	{
		HikeAlarmManager.cancelAlarm(context, HikeAlarmManager.REQUESTCODE_RETRY_LOCAL_NOTIFICATION);
	}
	
	private void playSoundViaPlayer(String notifSound)
	{
		if (NOTIF_SOUND_HIKE.equals(notifSound))
		{
			SoundUtils.playSoundFromRaw(context, R.raw.hike_jingle_15, AudioManager.STREAM_MUSIC);
		}
		else if (NOTIF_SOUND_DEFAULT.equals(notifSound))
		{
			SoundUtils.playDefaultNotificationSound(context);
		}
		else
		{
			notifSound = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.NOTIFICATION_TONE_URI, NOTIF_SOUND_HIKE);
			SoundUtils.playSound(context, Uri.parse(notifSound), AudioManager.STREAM_MUSIC);
		}
	}

	private void playSoundViaBuilder(NotificationCompat.Builder mBuilder, String notifSound)
	{
		if (NOTIF_SOUND_HIKE.equals(notifSound))
		{
			mBuilder.setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.hike_jingle_15));
		}
		else if (NOTIF_SOUND_DEFAULT.equals(notifSound))
		{
			mBuilder.setDefaults(mBuilder.getNotification().defaults | Notification.DEFAULT_SOUND);
		}
		else
		{
			notifSound = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.NOTIFICATION_TONE_URI, NOTIF_SOUND_HIKE);
			mBuilder.setSound(Uri.parse(notifSound));
		}
	}
	public  void notifyUserAndOpenHomeActivity(String text, String title, boolean shouldNotPlaySound)
	{
		Intent intent=Utils.getHomeActivityIntent(context);
		showBigTextStyleNotification(intent, 0, System.currentTimeMillis(), HikeNotification.HIKE_SUMMARY_NOTIFICATION_ID, title, text,
				title, "", null, null, shouldNotPlaySound, 0);
	}


	public void handleRetryNotification(int retryCount)
	{
		sendAnalytics();
		HikeNotificationMsgStack.getInstance().processPreNotificationWork();
		HikeNotification.getInstance().showNotificationForCurrentMsgStack(true, retryCount);
	}

	private void sendAnalytics()
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.RETRY_NOTIFICATION_SENT);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}
	
	public void sendNotificationToChatThread(String msisdn, String message, boolean forceNotPlaySound)
	{
		int notifType = NotificationType.NORMALMSG1TO1;
		if (BotUtils.isBot(msisdn))
			notifType = NotificationType.BOTMSG;
		
		if (OneToNConversationUtils.isOneToNConversation(msisdn))
			notifType = NotificationType.NORMALGC;

		if (StealthModeManager.getInstance().isStealthMsisdn(msisdn))
		{
			notifType = NotificationType.HIDDEN;

			notifyStealthMessage(notifType,msisdn);
		}
		else
		{
			notifyStringMessage(msisdn, message, forceNotPlaySound, notifType);
		}
	}


	public void notifyActivityMessage(FeedDataModel activityFeed, int notificationType)
	{

		/*
		 * We only proceed if the current status preference value is 0 which denotes that the user wants immediate notifications. Else we simply return
		 */
		if (PreferenceManager.getDefaultSharedPreferences(this.context).getInt(HikeConstants.STATUS_PREF, 0) != 0)
		{
			return;
		}

		final int notificationId = HIKE_SUMMARY_NOTIFICATION_ID;

		final long timeStamp = activityFeed.getTimestamp();

		final Intent notificationIntent = Utils.getTimelineActivityIntent(context, true, true);

		final int icon = returnSmallIcon();

		ContactInfo actorContactInfo = ContactManager.getInstance().getContact(activityFeed.getActor(),true,false);
		
		String name = actorContactInfo.getNameOrMsisdn();

		final String key = TextUtils.isEmpty(name) ? activityFeed.getActor() : name;

		String message = null;

		if (activityFeed.getActionType() == ActionTypes.LIKE)
		{
			if (activityFeed.getObjType() == ActivityObjectTypes.STATUS_UPDATE)
			{
				StatusMessage statusMessage = HikeConversationsDatabase.getInstance().getStatusMessageFromMappedId(activityFeed.getObjID());

				if(statusMessage == null)
				{
					return;
				}
					
				ContactInfo info = ContactManager.getInstance().getContact(activityFeed.getActor(), true, true);
				
				if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
				{
					message =  info.getNameOrMsisdn()+ " " +context.getString(R.string.dp_like_text);
				}
				else if (statusMessage.getStatusMessageType() == StatusMessageType.IMAGE || statusMessage.getStatusMessageType() == StatusMessageType.TEXT_IMAGE)
				{
					message = info.getNameOrMsisdn() + " " +context.getString(R.string.photo_like_text);
				}
				else if (statusMessage.getStatusMessageType() == StatusMessageType.TEXT)
				{
					message = info.getNameOrMsisdn() + " " +context.getString(R.string.liked_your_post);
				}
				else
				{
					return;
				}
			}

			/*
			 * Jellybean has added support for emojis so we don't need to add a '*' to replace them
			 */
			if (Build.VERSION.SDK_INT < 16)
			{
				// Replace emojis with a '*'
				message = SmileyParser.getInstance().replaceEmojiWithCharacter(message, "*");
			}
		}
		else
		{
			return;
		}

		// if notification message stack is empty, add to it and proceed with single notification display
		// else add to stack and notify clubbed messages
		if (hikeNotifMsgStack.isEmpty())
		{
			hikeNotifMsgStack.addMessage(activityFeed.getActor(), message, notificationType);
		}
		else
		{
			notifyStringMessage(activityFeed.getActor(), message, false, notificationType);
			return;
		}

		showNotification(notificationIntent, icon, timeStamp, notificationId, message, key, message, activityFeed.getActor(), null, false, false);
		
		addNotificationId(notificationId);
	}
	
	public void showOfflineRequestStealthNotification(Intent intent ,String contentTitle,String contentText, String tickerText,int smallIconId)
	{
		NotificationCompat.Builder mBuilder = getNotificationBuilder(contentTitle,contentText,tickerText, null,smallIconId, false,false);
		setNotificationIntentForBuilder(mBuilder, intent,HikeNotification.OFFLINE_REQUEST_ID);
		notifyNotification(HikeNotification.OFFLINE_REQUEST_ID, mBuilder);
	}

}
