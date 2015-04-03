package com.bsb.hike.notifications;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

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
import android.text.SpannableString;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.AnalyticsConstants.AppOpenSource;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.Protip;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPUtils;

public class HikeNotification
{
	private String VIB_OFF, VIB_DEF, VIB_SHORT, VIB_LONG;

	private String NOTIF_SOUND_OFF, NOTIF_SOUND_DEFAULT, NOTIF_SOUND_HIKE;

	public static final int HIKE_NOTIFICATION = -89;

	public static final int BATCH_SU_NOTIFICATION_ID = 89;

	public static final int PROTIP_NOTIFICATION_ID = -89;

	public static final int GAMING_PACKET_NOTIFICATION_ID = -89;

	public static final int FREE_SMS_POPUP_NOTIFICATION_ID = -89;

	public static final int APP_UPDATE_AVAILABLE_ID = -90;

	public static final int STEALTH_NOTIFICATION_ID = -89;

	public static final int STEALTH_POPUP_NOTIFICATION_ID = -89;

	public static final int HIKE_TO_OFFLINE_PUSH_NOTIFICATION_ID = -89;

	public static final int VOIP_MISSED_CALL_NOTIFICATION_ID = -89;

	// We need a constant notification id for bulk/big text notifications. Since
	// we are using msisdn for other single notifications, it is safe to use any
	// number <= 99
	public static final int HIKE_SUMMARY_NOTIFICATION_ID = -89;

	// We need a key to pair notification id. This will be used to retrieve notification id on notification dismiss/action.
	public static final String HIKE_NOTIFICATION_ID_KEY = "hike.notification";

	public static final String HIKE_STEALTH_MESSAGE_KEY = "HIKE_STEALTH_MESSAGE_KEY";

	private static final long MIN_TIME_BETWEEN_NOTIFICATIONS = 5 * 1000;

	private static final String SEPERATOR = " ";

	private final Context context;

	private final NotificationManager notificationManager;

	private long lastNotificationTime;

	private final SharedPreferences sharedPreferences;

	private HikeNotificationMsgStack hikeNotifMsgStack;
	
	private static HikeNotification hikeNotificationInstance;

	private HikeNotification(final Context context)
	{
		this.context = context;
		this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		this.sharedPreferences = context.getSharedPreferences(HikeMessengerApp.STATUS_NOTIFICATION_SETTING, 0);
		this.hikeNotifMsgStack = HikeNotificationMsgStack.getInstance(context);

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
	
	public static synchronized HikeNotification getInstance(Context context)
	{
		if(hikeNotificationInstance == null)
		{
			hikeNotificationInstance = new HikeNotification(context.getApplicationContext());
		}
		return hikeNotificationInstance;
	}

	public void notifySMSPopup(final String bodyString)
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
			hikeNotifMsgStack.addMessage(context.getString(R.string.app_name), bodyString);
		}
		else
		{
			notifyStringMessage(context.getString(R.string.app_name), bodyString, false);
			return;
		}

		/*
		 * invoke the chat thread here. The free SMS invite switch popup should already be showing here ideally by now.
		 */
		final Intent notificationIntent = Utils.getHomeActivityIntent(context);
		notificationIntent.putExtra(HikeConstants.Extras.NAME, context.getString(R.string.team_hike));

		notificationIntent.setData((Uri.parse("custom://" + FREE_SMS_POPUP_NOTIFICATION_ID)));
		final Drawable avatarDrawable = context.getResources().getDrawable(R.drawable.hike_avtar_protip);
		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder = getNotificationBuilder(context.getString(R.string.team_hike), bodyString, bodyString, avatarDrawable, smallIconId, false);
		setNotificationIntentForBuilder(mBuilder, notificationIntent);

		notificationManager.notify(FREE_SMS_POPUP_NOTIFICATION_ID, mBuilder.getNotification());

	}

	public void notifyStealthPopup(final String headerString)
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
			hikeNotifMsgStack.addMessage(context.getString(R.string.app_name), headerString);
		}
		else
		{
			notifyStringMessage(context.getString(R.string.app_name), headerString, false);
			return;
		}

		/*
		 * invoke the chat thread here. The Stealth tip popup should already be showing here ideally by now.
		 */
		final Intent notificationIntent = Utils.getHomeActivityIntent(context);
		notificationIntent.putExtra(HikeConstants.Extras.HAS_TIP, true);
		notificationIntent.putExtra(HikeConstants.Extras.NAME, context.getString(R.string.team_hike));

		notificationIntent.setData((Uri.parse("custom://" + STEALTH_POPUP_NOTIFICATION_ID)));
		final Drawable avatarDrawable = context.getResources().getDrawable(R.drawable.hike_avtar_protip);
		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder = getNotificationBuilder(context.getString(R.string.team_hike), headerString, headerString, avatarDrawable, smallIconId, false);
		setNotificationIntentForBuilder(mBuilder, notificationIntent);

		notificationManager.notify(FREE_SMS_POPUP_NOTIFICATION_ID, mBuilder.getNotification());
	}

	public void notifyAtomicPopup(final String message, Intent notificationIntent)
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
			hikeNotifMsgStack.addMessage(context.getString(R.string.app_name), message);
		}
		else
		{
			notifyStringMessage(context.getString(R.string.app_name), message, false);
			return;
		}

		notificationIntent.putExtra(HikeConstants.Extras.NAME, context.getString(R.string.team_hike));

		final Drawable avatarDrawable = context.getResources().getDrawable(R.drawable.hike_avtar_protip);
		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder = getNotificationBuilder(context.getString(R.string.team_hike), message, message, avatarDrawable, smallIconId, false);
		setNotificationIntentForBuilder(mBuilder, notificationIntent);

		notificationManager.notify(FREE_SMS_POPUP_NOTIFICATION_ID, mBuilder.getNotification());

	}

	public void notifyMessage(final Protip proTip)
	{

		// if notification message stack is empty, add to it and proceed with single notification display
		// else add to stack and notify clubbed messages
		if (hikeNotifMsgStack.isEmpty())
		{
			hikeNotifMsgStack.addMessage(context.getString(R.string.app_name), proTip.getHeader());
		}
		else
		{
			notifyStringMessage(context.getString(R.string.app_name), proTip.getHeader(), false);
			return;
		}

		// we've got to invoke the timeline here
		final Intent notificationIntent = Utils.getTimelineActivityIntent(context);
		notificationIntent.putExtra(HikeConstants.Extras.NAME, context.getString(R.string.team_hike));

		notificationIntent.setData((Uri.parse("custom://" + PROTIP_NOTIFICATION_ID)));

		final Drawable avatarDrawable = context.getResources().getDrawable(R.drawable.hike_avtar_protip);
		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder = getNotificationBuilder(context.getString(R.string.team_hike), proTip.getHeader(), proTip.getHeader(), avatarDrawable, smallIconId,
				false);

		setNotificationIntentForBuilder(mBuilder, notificationIntent);

		if (!sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			notificationManager.notify(PROTIP_NOTIFICATION_ID, mBuilder.getNotification());
		}
	}

	/*
	 * method to send a notification of an hike update available or applicationspush update. if isApplicationsPushUpdate is false than it is hike app update.
	 */
	public void notifyUpdatePush(int updateType, String packageName, String message, boolean isApplicationsPushUpdate)
	{
		message = (TextUtils.isEmpty(message)) ? context.getString(R.string.update_app) : message;
		final Drawable avatarDrawable = context.getResources().getDrawable(R.drawable.hike_avtar_protip);
		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder = getNotificationBuilder(context.getString(R.string.team_hike), message, message, avatarDrawable, smallIconId, false);

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("market://details?id=" + packageName));
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		mBuilder.setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));

		if (!sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			int notificationId = isApplicationsPushUpdate ? GAMING_PACKET_NOTIFICATION_ID : APP_UPDATE_AVAILABLE_ID;
			notificationManager.notify(notificationId, mBuilder.getNotification());
		}
		// TODO:: we should reset the gaming download message from preferences
	}

	public void notifyMessage(final ContactInfo contactInfo, final ConvMessage convMsg, boolean isRich, Bitmap bigPictureImage)
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
				hikeNotifMsgStack.addMessage(convMsg.getMsisdn(), message);
			}
			else
			{
				notifyStringMessage(convMsg.getMsisdn(), message, false);
				return;
			}

			// big picture messages ! intercept !
			showNotification(notificationIntent, icon, timestamp, notificationId, text, key, message, msisdn, bigPictureImage, !convMsg.isStickerMessage(), isPin, false, hikeNotifMsgStack.getNotificationSubText(),
					null, forceBlockNotificationSound, 0);
		}
		else
		{
			// if notification message stack is empty, add to it and proceed with single notification display
			// else add to stack and notify clubbed messages
			if (hikeNotifMsgStack.isEmpty())
			{
				hikeNotifMsgStack.addMessage(convMsg.getMsisdn(), message);
			}
			else
			{
				notifyStringMessage(convMsg.getMsisdn(), message, false);
				return;
			}
			// regular message
			showNotification(notificationIntent, icon, timestamp, HIKE_SUMMARY_NOTIFICATION_ID, text, key, message, msisdn, null, isPin, forceBlockNotificationSound);
		}
	}

	public void notifyStringMessage(String msisdn, String message, boolean forceNotPlaySound)
	{
		try
		{
			hikeNotifMsgStack.addMessage(msisdn, message);
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
			return;
		}

		hikeNotifMsgStack.invalidateConvMsgList();
		
		boolean isSingleMsisdn = hikeNotifMsgStack.isFromSingleMsisdn();

		Drawable avatarDrawable = null;
		if (!isSingleMsisdn)
		{
			avatarDrawable = context.getResources().getDrawable(R.drawable.hike_avtar_protip);
		}

		if (hikeNotifMsgStack.getSize() == 1)
		{
			showBigTextStyleNotification(hikeNotifMsgStack.getNotificationIntent(), hikeNotifMsgStack.getNotificationIcon(), hikeNotifMsgStack.getLatestAddedTimestamp(),
					hikeNotifMsgStack.getNotificationId(), hikeNotifMsgStack.getNotificationTickerText(), hikeNotifMsgStack.getNotificationTitle(),
					hikeNotifMsgStack.getNotificationBigText(), isSingleMsisdn ? hikeNotifMsgStack.lastAddedMsisdn : "bulk", hikeNotifMsgStack.getNotificationSubText(),
					avatarDrawable, forceNotPlaySound, 0);

		}
		else
		{
			showInboxStyleNotification(hikeNotifMsgStack.getNotificationIntent(), hikeNotifMsgStack.getNotificationIcon(), hikeNotifMsgStack.getLatestAddedTimestamp(),
					hikeNotifMsgStack.getNotificationId(), hikeNotifMsgStack.getNotificationTickerText(), hikeNotifMsgStack.getNotificationTitle(),
					hikeNotifMsgStack.getNotificationBigText(), isSingleMsisdn ? hikeNotifMsgStack.lastAddedMsisdn : "bulk", hikeNotifMsgStack.getNotificationSubText(),
					avatarDrawable, hikeNotifMsgStack.getBigTextList(), forceNotPlaySound, 0);
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
		hikeNotifMsgStack.invalidateConvMsgList();

		boolean isSingleMsisdn = hikeNotifMsgStack.isFromSingleMsisdn();

		Drawable avatarDrawable = null;
		if (!isSingleMsisdn)
		{
			avatarDrawable = context.getResources().getDrawable(R.drawable.hike_avtar_protip);
		}

		// Possibility to show big picture message
		ConvMessage convMessage = hikeNotifMsgStack.getLastInsertedConvMessage();
					
		if (hikeNotifMsgStack.getSize() == 1)
		{
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
							hikeNotifMsgStack.getNotificationBigText(), convMessage.getMsisdn(), bigPictureImage, !convMessage.isStickerMessage(), false, false,
							hikeNotifMsgStack.getNotificationSubText(), null, shouldNotPlaySound, retryCount);
					return;
				}
			}
			else if(convMessage.isVoipMissedCallMsg())
			{
				NotificationCompat.Action[] actions = VoIPUtils.getMissedCallNotifActions(context, convMessage.getMsisdn());
				showBigTextStyleNotification(hikeNotifMsgStack.getNotificationIntent(), hikeNotifMsgStack.getNotificationIcon(), hikeNotifMsgStack.getLatestAddedTimestamp(),
						VOIP_MISSED_CALL_NOTIFICATION_ID, hikeNotifMsgStack.getNotificationTickerText(), hikeNotifMsgStack.getNotificationTitle(),
						hikeNotifMsgStack.getNotificationBigText(), isSingleMsisdn ? hikeNotifMsgStack.lastAddedMsisdn : "bulk", hikeNotifMsgStack.getNotificationSubText(),
						avatarDrawable, shouldNotPlaySound, retryCount, actions);
				return;
			}
		}

		if (hikeNotifMsgStack.getSize() == 1)
		{
			HAManager.getInstance().setMetadatFieldsForSessionEvent(AnalyticsConstants.AppOpenSource.FROM_NOTIFICATION, convMessage.getMsisdn(), convMessage,
					AnalyticsConstants.ConversationType.NORMAL);
			
			showBigTextStyleNotification(hikeNotifMsgStack.getNotificationIntent(), hikeNotifMsgStack.getNotificationIcon(), hikeNotifMsgStack.getLatestAddedTimestamp(),
					hikeNotifMsgStack.getNotificationId(), hikeNotifMsgStack.getNotificationTickerText(), hikeNotifMsgStack.getNotificationTitle(),
					hikeNotifMsgStack.getNotificationBigText(), isSingleMsisdn ? hikeNotifMsgStack.lastAddedMsisdn : "bulk", hikeNotifMsgStack.getNotificationSubText(),
					avatarDrawable, shouldNotPlaySound, retryCount);

		}
		else if (!hikeNotifMsgStack.isEmpty())
		{
			HAManager.getInstance().setMetadatFieldsForSessionEvent(AnalyticsConstants.AppOpenSource.FROM_NOTIFICATION, convMessage.getMsisdn(), convMessage,
					AnalyticsConstants.ConversationType.NORMAL);
			
			showInboxStyleNotification(hikeNotifMsgStack.getNotificationIntent(), hikeNotifMsgStack.getNotificationIcon(), hikeNotifMsgStack.getLatestAddedTimestamp(),
					hikeNotifMsgStack.getNotificationId(), hikeNotifMsgStack.getNotificationTickerText(), hikeNotifMsgStack.getNotificationTitle(),
					hikeNotifMsgStack.getNotificationBigText(), isSingleMsisdn ? hikeNotifMsgStack.lastAddedMsisdn : "bulk", hikeNotifMsgStack.getNotificationSubText(),
					avatarDrawable, hikeNotifMsgStack.getBigTextList(), shouldNotPlaySound, retryCount);
		}

	}

	public void notifyHikeToOfflinePush(ArrayList<String> msisdnList, HashMap<String, String> nameMap)
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
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		notificationIntent.setData((Uri.parse("custom://" + notificationId)));
		final Drawable avatarDrawable = context.getResources().getDrawable(R.drawable.offline_notification);
		final int smallIconId = returnSmallIcon();

		String title = (msisdnList.size() > 1) ? context.getString(R.string.hike_to_offline_push_title_multiple, msisdnList.size()) : (HikeMessengerApp
				.isStealthMsisdn(firstMsisdn) ? context.getString(R.string.stealth_notification_message) : context.getString(R.string.hike_to_offline_push_title_single,
				nameMap.get(firstMsisdn)));
		String message = context.getString(R.string.hike_to_offline_text);

		// if notification message stack is empty, add to it and proceed with single notification display
		// else add to stack and notify clubbed messages
		if (hikeNotifMsgStack.isEmpty())
		{
			hikeNotifMsgStack.addMessage(context.getString(R.string.app_name), title + ": " + message);
		}
		else
		{
			notifyStringMessage(context.getString(R.string.app_name), title + ": " + message, false);
			return;
		}

		NotificationCompat.Builder mBuilder = getNotificationBuilder(title, message, message, avatarDrawable, smallIconId, false);
		setNotificationIntentForBuilder(mBuilder, notificationIntent);

		notificationManager.notify(notificationId, mBuilder.getNotification());

	}

	public void notifyStealthMessage()
	{
		final int notificationId = STEALTH_NOTIFICATION_ID;

		String message = context.getString(R.string.stealth_notification_message);
		String key = HIKE_STEALTH_MESSAGE_KEY;

		String text = message;

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
			hikeNotifMsgStack.addMessage(key, context.getString(R.string.stealth_notification_message));
		}
		else
		{
			notifyStringMessage(key, context.getString(R.string.stealth_notification_message), false);
			return;
		}

		// we've got to invoke the timeline here
		final Intent notificationIntent = Utils.getHomeActivityIntent(context);
		notificationIntent.setData((Uri.parse("custom://" + notificationId)));

		final Drawable avatarDrawable = context.getResources().getDrawable(R.drawable.hike_avtar_protip);
		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder = getNotificationBuilder(context.getString(R.string.app_name), message, text, avatarDrawable, smallIconId, false);

		setNotificationIntentForBuilder(mBuilder, notificationIntent);

		final boolean shouldNotPlayNotification = (System.currentTimeMillis() - lastNotificationTime) < MIN_TIME_BETWEEN_NOTIFICATIONS;
		notificationManager.notify(notificationId, mBuilder.getNotification());
		lastNotificationTime = shouldNotPlayNotification ? lastNotificationTime : System.currentTimeMillis();
	}

	public void notifyFavorite(final ContactInfo contactInfo)
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
			hikeNotifMsgStack.addMessage(contactInfo.getMsisdn(), message);
		}
		else
		{
			notifyStringMessage(contactInfo.getMsisdn(), message, false);
			return;
		}

		showNotification(notificationIntent, icon, timeStamp, notificationId, text, key, message, msisdn, null, false, false);
		addNotificationId(notificationId);
	}

	public void notifyStatusMessage(final StatusMessage statusMessage)
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
			message = context.getString(R.string.favorite_confirmed_notification, key);
			text = message;
		}
		else if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
		{
			message = context.getString(R.string.status_profile_pic_notification, key);
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
			hikeNotifMsgStack.addMessage(statusMessage.getMsisdn(), message);
		}
		else
		{
			notifyStringMessage(statusMessage.getMsisdn(), message, true);
			return;
		}

		showNotification(notificationIntent, icon, timeStamp, notificationId, text, key, message, statusMessage.getMsisdn(), null, false, true);
		addNotificationId(notificationId);
	}

	public void notifyBigPictureStatusNotification(final String imagePath, final String msisdn, final String name)
	{

		if (PreferenceManager.getDefaultSharedPreferences(this.context).getInt(HikeConstants.STATUS_PREF, 0) != 0)
		{
			return;
		}

		final int notificationId = HIKE_SUMMARY_NOTIFICATION_ID;
		final String key = TextUtils.isEmpty(name) ? msisdn : name;
		final String message = context.getString(R.string.status_profile_pic_notification);
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
			hikeNotifMsgStack.addMessage(msisdn, message);
		}
		else
		{
			notifyStringMessage(msisdn, message, true);
			return;
		}

		showNotification(notificationIntent, icon, System.currentTimeMillis(), notificationId, text, key, message, msisdn, bigPictureImage, false, true);
	}

	public void notifyBatchUpdate(final String header, final String message)
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
			hikeNotifMsgStack.addMessage(context.getString(R.string.app_name), text);
		}
		else
		{
			notifyStringMessage(context.getString(R.string.app_name), text, false);
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

	private void showInboxStyleNotification(final Intent notificationIntent, final int icon, final long timestamp, final int notificationId, final CharSequence text,
			final String key, final String message, final String msisdn, String subMessage, Drawable argAvatarDrawable, List<SpannableString> inboxLines, boolean shouldNotPlaySound, int retryCount)
	{

		final boolean shouldNotPlayNotification = shouldNotPlaySound ? shouldNotPlaySound : (System.currentTimeMillis() - lastNotificationTime) < MIN_TIME_BETWEEN_NOTIFICATIONS;

		Drawable avatarDrawable = null;
		if (argAvatarDrawable == null)
		{
			avatarDrawable = Utils.getAvatarDrawableForNotificationOrShortcut(context, msisdn, false);
		}
		else
		{
			avatarDrawable = argAvatarDrawable;
		}

		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder;
		mBuilder = null;
		mBuilder = getNotificationBuilder(key, subMessage, text.toString(), avatarDrawable, smallIconId, shouldNotPlaySound);
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

		setNotificationIntentForBuilder(mBuilder, notificationIntent);

		setOnDeleteIntent(mBuilder, notificationId, retryCount);
		
		notificationBuilderPostWork();

		if (!sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			notificationManager.notify(notificationId, mBuilder.getNotification());
			lastNotificationTime = shouldNotPlayNotification ? lastNotificationTime : System.currentTimeMillis();
		}
	}

	public void showBigTextStyleNotification(final Intent notificationIntent, final int icon, final long timestamp, final int notificationId, final CharSequence text,
			final String key, final String message, final String msisdn, String subMessage, Drawable argAvatarDrawable, boolean shouldNotPlaySound, int retryCount)
	{
		showBigTextStyleNotification(notificationIntent, icon, timestamp, notificationId, text, key, message, msisdn, subMessage, argAvatarDrawable, shouldNotPlaySound, retryCount, null);
	}

	public void showBigTextStyleNotification(final Intent notificationIntent, final int icon, final long timestamp, final int notificationId, final CharSequence text,
			final String key, final String message, final String msisdn, String subMessage, Drawable argAvatarDrawable, boolean shouldNotPlaySound, int retryCount, Action[] actions)
	{

		final boolean shouldNotPlayNotification = shouldNotPlaySound ? shouldNotPlaySound : (System.currentTimeMillis() - lastNotificationTime) < MIN_TIME_BETWEEN_NOTIFICATIONS;

		Drawable avatarDrawable = null;
		if (argAvatarDrawable == null)
		{
			avatarDrawable = Utils.getAvatarDrawableForNotificationOrShortcut(context, msisdn, false);
		}
		else
		{
			avatarDrawable = argAvatarDrawable;
		}

		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder;
		mBuilder = null;
		mBuilder = getNotificationBuilder(key, message, text.toString(), avatarDrawable, smallIconId, shouldNotPlaySound);
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

		setNotificationIntentForBuilder(mBuilder, notificationIntent);

		setOnDeleteIntent(mBuilder, notificationId, retryCount);
		
		notificationBuilderPostWork();

		if (!sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			notificationManager.notify(notificationId, mBuilder.getNotification());
			lastNotificationTime = shouldNotPlayNotification ? lastNotificationTime : System.currentTimeMillis();
		}
	}

	private void showNotification(final Intent notificationIntent, final int icon, final long timestamp, final int notificationId, final CharSequence text, final String key,
			final String message, final String msisdn, final Bitmap bigPictureImage, boolean isFTMessage, boolean isPin, boolean isBigText, String subMessage,
			Drawable argAvatarDrawable, boolean forceNotPlaySound, int retryCount)
	{
		final boolean shouldNotPlayNotification = forceNotPlaySound ? forceNotPlaySound : (System.currentTimeMillis() - lastNotificationTime) < MIN_TIME_BETWEEN_NOTIFICATIONS;

		Drawable avatarDrawable = null;
		if (argAvatarDrawable == null)
		{
			avatarDrawable = Utils.getAvatarDrawableForNotificationOrShortcut(context, msisdn, isPin);
		}
		else
		{
			avatarDrawable = argAvatarDrawable;
		}

		final int smallIconId = returnSmallIcon();

		NotificationCompat.Builder mBuilder;
		if (bigPictureImage != null)
		{
			mBuilder = getNotificationBuilder(key, message, text.toString(), avatarDrawable, smallIconId, forceNotPlaySound);
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
				mBuilder = getNotificationBuilder(key, message, text.toString(), avatarDrawable, smallIconId, forceNotPlaySound);
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
				mBuilder = getNotificationBuilder(key, message, text.toString(), avatarDrawable, smallIconId, false);
			}
		}

		setOnDeleteIntent(mBuilder, notificationId, retryCount);
		setNotificationIntentForBuilder(mBuilder, notificationIntent);
		
		notificationBuilderPostWork();
		
		if (!sharedPreferences.getBoolean(HikeMessengerApp.BLOCK_NOTIFICATIONS, false))
		{
			notificationManager.notify(notificationId, mBuilder.getNotification());
			lastNotificationTime = shouldNotPlayNotification ? lastNotificationTime : System.currentTimeMillis();
		}
	}

	private void showNotification(final Intent notificationIntent, final int icon, final long timestamp, final int notificationId, final CharSequence text, final String key,
			final String message, final String msisdn, final Bitmap bigPictureImage, boolean isPin, boolean forceNotPlaySound)
	{
		showNotification(notificationIntent, icon, timestamp, notificationId, text, key, message, msisdn, bigPictureImage, false, isPin, true,
				hikeNotifMsgStack.getNotificationSubText(), null, forceNotPlaySound, 0);
	}
	
	private void showNotification(final Intent notificationIntent, final int icon, final long timestamp, final int notificationId, final CharSequence text, final String key,
			final String message, final String msisdn, final Bitmap bigPictureImage, boolean isPin, boolean forceNotPlaySound, int retryCount)
	{
		showNotification(notificationIntent, icon, timestamp, notificationId, text, key, message, msisdn, bigPictureImage, false, isPin, true,
				hikeNotifMsgStack.getNotificationSubText(), null, forceNotPlaySound, retryCount);
	}

	private int returnSmallIcon()
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

		final SharedPreferences preferenceManager = PreferenceManager.getDefaultSharedPreferences(this.context);

		String vibrate = preferenceManager.getString(HikeConstants.VIBRATE_PREF_LIST, VIB_DEF);

		final Bitmap avatarBitmap = HikeBitmapFactory.returnScaledBitmap((HikeBitmapFactory.drawableToBitmap(avatarDrawable, Bitmap.Config.RGB_565)), context);

		final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context).setContentTitle(contentTitle).setSmallIcon(smallIconId).setLargeIcon(avatarBitmap)
				.setContentText(contentText).setAutoCancel(true).setTicker(tickerText).setPriority(NotificationCompat.PRIORITY_DEFAULT);

		//Reset ticker text since we dont want to tick older messages
		hikeNotifMsgStack.setTickerText(null);
		
		AudioManager manager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		
		if (!forceNotPlaySound && !manager.isMusicActive())
		{
			final boolean shouldNotPlayNotification = (System.currentTimeMillis() - lastNotificationTime) < MIN_TIME_BETWEEN_NOTIFICATIONS;
			String notifSound = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.NOTIF_SOUND_PREF, NOTIF_SOUND_HIKE);
			if (!shouldNotPlayNotification)
			{
				Logger.i("notif", "sound " + notifSound);

				if (!NOTIF_SOUND_OFF.equals(notifSound))
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

				if (!VIB_OFF.equals(vibrate))
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
			}
			
			int ledColor = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.LED_NOTIFICATION_COLOR_CODE, HikeConstants.LED_DEFAULT_WHITE_COLOR);
		
			//Check if Previously any boolean Key was present for this Led
			if(preferenceManager.contains(HikeConstants.LED_PREF))
			{
				boolean led = preferenceManager.getBoolean(HikeConstants.LED_PREF, true);
				ledColor = led == true ? ledColor : HikeConstants.LED_NONE_COLOR;
				
				//removing previous Key
				preferenceManager.edit().remove(HikeConstants.LED_PREF).commit();
				
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.LED_NOTIFICATION_COLOR_CODE, ledColor);
			}
			if(ledColor != HikeConstants.LED_NONE_COLOR)
			{
				mBuilder.setLights(ledColor, HikeConstants.LED_LIGHTS_ON_MS, HikeConstants.LED_LIGHTS_OFF_MS);
			}
		}
		return mBuilder;
	}

	public void setNotificationIntentForBuilder(NotificationCompat.Builder mBuilder, Intent notificationIntent)
	{
		//Adding Extra to check While receiving that user has come via clicking Notification
		notificationIntent.putExtra(AnalyticsConstants.APP_OPEN_SOURCE_EXTRA, AppOpenSource.FROM_NOTIFICATION);
		
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(contentIntent);
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
	public long getNextRetryNotificationTime()
	{
		long nextRetryTime = System.currentTimeMillis() + HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.RETRY_NOTIFICATION_COOL_OFF_TIME, HikeConstants.DEFAULT_RETRY_NOTIF_TIME);
		
		/*
		 * We have a sleep state from 12am - 8am. If the timer is finished in this time frame then 
		 * we wait for the sleep state to get over before showing the local push.
		 */
		Calendar calendar = Calendar.getInstance();
		long toDay12AM = Utils.getTimeInMillis(calendar, 0, 0, 0, 0);
		long toDay8AM = Utils.getTimeInMillis(calendar, 8, 0, 0, 0);
		
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		long nextDay12AM = Utils.getTimeInMillis(calendar, 0, 0, 0, 0);
		long nextDay8AM = Utils.getTimeInMillis(calendar, 8, 0, 0, 0);
		if(nextRetryTime >= toDay12AM && nextRetryTime < toDay8AM)
		{
			nextRetryTime = toDay8AM;
		}
		else if(nextRetryTime >= nextDay12AM && nextRetryTime < nextDay8AM)
		{
			nextRetryTime = nextDay8AM;
		}
		Logger.i("HikeNotification", "currtime = "+ System.currentTimeMillis() + "  nextDay12AM = "+nextDay12AM+ "  nextDay8AM = "+nextDay8AM + "  toDay8AM = "+toDay8AM + " finalRetryTime = "+ nextRetryTime);
		return nextRetryTime;
	}
	
	/**
	 * In this method we can put all the work which we need to do at the end of showing a notification
	 */
	private void notificationBuilderPostWork()
	{
		HikeAlarmManager.cancelAlarm(context, HikeAlarmManager.REQUESTCODE_RETRY_LOCAL_NOTIFICATION);
	}
	
	public  void notifyUserAndOpenHomeActivity(String text, String title, boolean shouldNotPlaySound)
	{
		Drawable drawable =context.getResources().getDrawable(R.drawable.hike_avtar_protip);
		Intent intent=Utils.getHomeActivityIntent(context);
		HikeNotification.getInstance(context).showBigTextStyleNotification(intent, 0, System.currentTimeMillis(), HikeNotification.HIKE_SUMMARY_NOTIFICATION_ID, title, text,
				title, "", null, drawable, shouldNotPlaySound, 0);
	}
}
