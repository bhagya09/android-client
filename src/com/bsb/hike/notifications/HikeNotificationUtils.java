package com.bsb.hike.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.NotificationPreview;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

import java.util.List;

public class HikeNotificationUtils
{

	/**
	 * Utility method to get a "msisdn/name - message" preview from ConvMsg.
	 * 
	 * @param context
	 * @param db
	 * @param convMsg
	 * @return
	 */
	public static NotificationPreview getNotificationPreview(Context context, ConvMessage convMsg)
	{

		final String msisdn = convMsg.getMsisdn();

		// Check whether the message contains any files
		String message = (!convMsg.isFileTransferMessage()) ? convMsg.getMessage() : HikeFileType.getFileTypeMessage(context, convMsg.getMetadata().getHikeFiles().get(0)
				.getHikeFileType(), convMsg.isSent());

		ContactManager contactManager = ContactManager.getInstance();

		ContactInfo contactInfo;
		if (convMsg.isOneToNChat())
		{
			contactInfo = new ContactInfo(convMsg.getMsisdn(), convMsg.getMsisdn(), contactManager.getName(msisdn), convMsg.getMsisdn());
		}
		else
		{
			contactInfo = contactManager.getContact(msisdn, true, false);
		}

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

		/*
		 * Jellybean has added support for emojis so we don't need to add a '*' to replace them
		 */
		if (Build.VERSION.SDK_INT < 16)
		{
			// Replace emojis with a '*'
			message = SmileyParser.getInstance().replaceEmojiWithCharacter(message, "*");
		}

		String key = null;
		if (BotUtils.isBot(msisdn))
		{
			key = BotUtils.getBotInfoForBotMsisdn(msisdn).getConversationName();
		}
		else
		{
			key = (contactInfo != null && !TextUtils.isEmpty(contactInfo.getName())) ? contactInfo.getName() : msisdn;
		}
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

			boolean isPin = false;

			if (convMsg.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
				isPin = true;

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
		else if(convMsg.getParticipantInfoState() == ParticipantInfoState.USER_JOIN)
		{
			key = String.format(convMsg.getMetadata().getKey(), key);
		}

		return new NotificationPreview(message, key,convMsg.getNotificationType());
	}

	/**
	 * Provides name for msisdn
	 * 
	 * @param context
	 * @param db
	 * @param convDb
	 * @param argMsisdn
	 * @return
	 */
	public static String getNameForMsisdn(String argMsisdn)
	{
		if (HikeNotification.HIKE_STEALTH_MESSAGE_KEY.equals(argMsisdn))
		{
			return HikeMessengerApp.getInstance().getApplicationContext().getString(R.string.app_name);
		}
		String name = null;
		if (BotUtils.isBot(argMsisdn))
		{
			name = HikeMessengerApp.hikeBotInfoMap.get(argMsisdn).getConversationName();
		}

		if (TextUtils.isEmpty(name))
		{
			name = ContactManager.getInstance().getName(argMsisdn);
		}
		
		if (TextUtils.isEmpty(name))
		{
			name = argMsisdn;
		}
		
		return name;
	}

	/**
	 * Keep this instantiated since makeNotificationLine() will be called repeatedly on receiving notification messages
	 */
	private static final ForegroundColorSpan mBoldSpan = new ForegroundColorSpan(Color.LTGRAY);

	/**
	 * Creates SpannableString of pattern <code>title text</code> with title in a light gray color
	 * 
	 * @param title
	 * @param text
	 * @return
	 */
	public static SpannableString makeNotificationLine(String title, String text)
	{
		final SpannableString spannableString;
		if (title != null && title.length() > 0)
		{
			spannableString = new SpannableString(String.format("%s  %s", title, text));
			spannableString.setSpan(mBoldSpan, 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		else
		{
			spannableString = new SpannableString(text);
		}
		return spannableString;
	}
	
	/**
	 * Restore Notification Params like sound, vibration and led
	 * 
	 * @param Context context
	 */
	public static void restoreNotificationParams(Context context)
	{
		// To get old NotificaticationSoundPref preference before NotificaticationSoundPref list preference
		SharedPreferences defaultPref = PreferenceManager.getDefaultSharedPreferences(context);
		HikeSharedPreferenceUtil hikeSharedPreferenceUtil = HikeSharedPreferenceUtil.getInstance();
		if (!hikeSharedPreferenceUtil.contains(HikeConstants.NOTIF_SOUND_PREF) && defaultPref.contains(HikeConstants.NOTIF_SOUND_PREF))
		{
			hikeSharedPreferenceUtil.saveData(HikeConstants.NOTIF_SOUND_PREF, defaultPref.getString(HikeConstants.NOTIF_SOUND_PREF, context.getString(R.string.notif_sound_default)));
		}

		// To get old NotificaticationLED preference before NotificaticationLED list preference
		if (!hikeSharedPreferenceUtil.contains(HikeMessengerApp.LED_NOTIFICATION_COLOR_CODE) && defaultPref.contains(HikeConstants.LED_PREF))
		{
				hikeSharedPreferenceUtil.saveData(HikeMessengerApp.LED_NOTIFICATION_COLOR_CODE,
						defaultPref.getBoolean(HikeConstants.LED_PREF, true) ? HikeConstants.LED_DEFAULT_WHITE_COLOR : HikeConstants.LED_NONE_COLOR);
				defaultPref.edit().remove(HikeConstants.LED_PREF).commit();
		}
		
		// To get old NotificaticationSoundPref preference before NotificaticationSoundPref list preference
		if (!defaultPref.contains(HikeConstants.VIBRATE_PREF_LIST))
		{
			Editor edit = defaultPref.edit();
			edit.putString(HikeConstants.VIBRATE_PREF_LIST, Utils.getOldVibratePref(context));
			edit.commit();
		}
	}


	public static String getNotificationStringFromList(List<SpannableString> list)
	{
		StringBuilder bigText = new StringBuilder();
		for (int i = 0; i < list.size(); i++)
		{
			bigText.append(list.get(i));

			if (i != list.size() - 1)
			{
				bigText.append("\n");
			}
		}
		return bigText.toString();
	}
}
