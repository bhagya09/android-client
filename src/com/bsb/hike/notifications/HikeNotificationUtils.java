package com.bsb.hike.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
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
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
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

	/**
	 * This method checks whether json data received for rich uj notif is valid
	 * @param data
	 * @return
     */
	public static boolean isUJNotifJSONValid(JSONObject data)
	{
		if(data == null)
		{
			Logger.d(HikeConstants.UserJoinMsg.TAG, "received null data for uj notif");
			return false;
		}

		if(TextUtils.isEmpty(data.optString(HikeConstants.MSISDN)))
		{
			Logger.d(HikeConstants.UserJoinMsg.TAG, "empty/null msisdn received for uj notif");
			return false;
		}

		JSONArray ujNotifCTAs = data.optJSONArray(HikeConstants.CTAS);
		if(ujNotifCTAs == null || ujNotifCTAs.length() == 0 || ujNotifCTAs.length() > 2)
		{
			Logger.d(HikeConstants.UserJoinMsg.TAG, "invalid ctas received for uj notif");
			return false;
		}

		return true;
	}

	/**
	 * This method returns a list of {@link android.support.v4.app.NotificationCompat.Action} items. These
	 * will be added as action buttons to make rich uj notif.
	 * @param context - this is needed while creating {@link PendingIntent} for the action buttons
	 * @param actionsJSON
	 * @param msisdn - required as we are opening chatthread on uj notif click
     * @return
     */
	public static List<NotificationCompat.Action> getActionsForUJNotif(Context context, JSONArray actionsJSON, String msisdn)
	{
		Logger.d(HikeConstants.UserJoinMsg.TAG, "creating list of actions for rich uj notif");
		if(actionsJSON == null || actionsJSON.length() == 0)
		{
			Logger.d(HikeConstants.UserJoinMsg.TAG, "json array of CTAs was null/empty so returning null");
			return null;
		}
		List<NotificationCompat.Action> notifActions = new ArrayList<>();
		for(int i = 0; i < actionsJSON.length(); i++)
		{
			JSONObject actionObj = actionsJSON.optJSONObject(i);
			if(actionObj != null)
			{

				notifActions.add(getUJNotifAction(context, actionObj, msisdn));
			}
		}
		return notifActions;
	}

	/**
	 * This method is used to create a single {@link android.support.v4.app.NotificationCompat.Action} from json
	 * sample JSON = {"action": "say_hi","l": "Say hi","md": {"msg": "Hi there!"}}
	 * @param context
	 * @param actionObj
	 * @param msisdn
     * @return
     */
	public static NotificationCompat.Action getUJNotifAction(Context context, JSONObject actionObj, String msisdn)
	{
		Logger.d(HikeConstants.UserJoinMsg.TAG, "creating individual action items for rich uj notif");
		String action = actionObj.optString(HikeConstants.MqttMessageTypes.ACTION);

		int icon;
		String label;
		if(action.equals(HikeConstants.UserJoinMsg.ACTION_SAY_HI))
		{
			icon = R.drawable.nuj_message;
			label = actionObj.optString(HikeConstants.LABEL, context.getString(R.string.uj_default_cta_say_hi));
		}
		else
		{
			icon = R.drawable.nuj_favourite;
			label = actionObj.optString(HikeConstants.LABEL, context.getString(R.string.uj_default_cta_add_friend));
		}

		Intent actionIntent = new Intent(HikeConstants.UserJoinMsg.NOTIF_ACTION_INTENT);
		actionIntent.putExtra(HikeConstants.MqttMessageTypes.ACTION, action);

		if(!TextUtils.isEmpty(msisdn))
		{
			actionIntent.putExtra(HikeConstants.MSISDN, msisdn);
		}

		JSONObject metadata = actionObj.optJSONObject(HikeConstants.METADATA);
		if(metadata != null)
		{
			actionIntent.putExtra(HikeConstants.METADATA, metadata.toString());
		}

		PendingIntent actionPI = PendingIntent.getBroadcast(context, action.hashCode(), actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		return new NotificationCompat.Action(icon, label, actionPI);
	}

}
