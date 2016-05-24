package com.bsb.hike.service;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsMessage;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.chatHead.StickyCaller;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class SMSBroadcastReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		/*
		 * no name setting, so don't bother pulling in SMS's yet or The user doesn't want us pulling in his SMS.
		 */

		Logger.d("HikeMessageReceiver" , System.currentTimeMillis()+"");
		Logger.d("HikeMessageReceiver", "message received");

		// If the User is not authenticated and the GCMID is not sent to the server and the user is connected.

		HikeSharedPreferenceUtil mprefs = HikeSharedPreferenceUtil.getInstance();
		
		Bundle bundle = intent.getExtras();
		if (bundle != null)
		{
			Object[] pdus = (Object[]) bundle.get("pdus");
			final SmsMessage message = SmsMessage.createFromPdu((byte[]) pdus[0]);
			StickyCaller.sms = message.getDisplayMessageBody();
			ChatHeadUtils.showCallerCard(message.getOriginatingAddress());
		}
		
		if (Utils.isUserOnline(context) && (!Utils.isUserAuthenticated(context)) && !mprefs.getData(HikeMessengerApp.GCM_ID_SENT_PRELOAD, false) )
		{

			Intent in = new Intent(HikeService.REGISTER_TO_GCM_ACTION);
			mprefs.saveData(HikeConstants.REGISTER_GCM_SIGNUP, HikeConstants.REGISTEM_GCM_BEFORE_SIGNUP);
			LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(in);
			return;
		}

		if (!Utils.shouldConnectToMQTT() || !PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.RECEIVE_SMS_PREF, false))
		{
			return;
		}

		Logger.d(getClass().getSimpleName(), "Received SMS message");
		Bundle extras = intent.getExtras();
		if (extras != null)
		{
			Logger.d(getClass().getSimpleName(), "Received SMS message with extras " + extras.keySet());
			Object[] extra = (Object[]) extras.get("pdus");
			Logger.d(getClass().getSimpleName(), "Extras length is " + extra.length);
			for (int i = 0; i < extra.length; ++i)
			{
				SmsMessage sms = SmsMessage.createFromPdu((byte[]) extra[i]);
				String body = sms.getMessageBody();
				long timestamp = sms.getTimestampMillis() / 1000;
				String from = sms.getOriginatingAddress();
				ContactInfo contactInfo = ContactManager.getInstance().getContact(from, true, true);
				if (contactInfo == null)
				{
					Logger.d(getClass().getSimpleName(), "Ignoring SMS message because contact not in addressbook phone_no=" + from);
					return;
				}
				try
				{

					JSONObject msg = new JSONObject();
					msg.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE);
					msg.put(HikeConstants.FROM, contactInfo.getMsisdn());

					JSONObject data = new JSONObject();
					data.put(HikeConstants.SMS_MESSAGE, body);
					data.put(HikeConstants.TIMESTAMP, timestamp);
					data.put(HikeConstants.MESSAGE_ID, makeSMSId(from, body, timestamp));

					msg.put(HikeConstants.DATA, data);

					writeToNativeSMSDb(context, msg);

					Logger.d(getClass().getSimpleName(), "Received SMS " + msg.toString());
					Intent smsReceivedIntent = new Intent(context, HikeService.class);
					smsReceivedIntent.putExtra(HikeConstants.Extras.SMS_MESSAGE, msg.toString());
					context.startService(smsReceivedIntent);

					this.abortBroadcast();
				}
				catch (JSONException e)
				{
					Logger.e(getClass().getSimpleName(), "Invalid data for SMS message", e);
					return;
				}

			}
		}
	}

	private int makeSMSId(String msisdn, String message, long timestamp)
	{
		return (int) ((msisdn.hashCode() + message.hashCode() + timestamp) / 1000);
	}

	private void writeToNativeSMSDb(Context context, JSONObject msg) throws JSONException
	{
		ConvMessage convMessage = new ConvMessage(msg, context);

		ContentValues values = new ContentValues();
		values.put(HikeConstants.SMSNative.NUMBER, convMessage.getMsisdn());
		values.put(HikeConstants.SMSNative.DATE, convMessage.getTimestamp() * 1000);
		values.put(HikeConstants.SMSNative.MESSAGE, convMessage.getMessage());

		context.getContentResolver().insert(HikeConstants.SMSNative.INBOX_CONTENT_URI, values);
	}
}
