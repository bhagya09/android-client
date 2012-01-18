package com.bsb.hike.utils;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.models.ConvMessage;

public class DbConversationListener implements Listener
{

	HikeConversationsDatabase mConversationDb;

	HikeUserDatabase mUserDb;
	
	private HikePubSub mPubSub;

	private Editor mEditor;

	public DbConversationListener(Context context)
	{
		mPubSub = HikeMessengerApp.getPubSub();
		mConversationDb = new HikeConversationsDatabase(context);
		mUserDb = new HikeUserDatabase(context);
		mEditor = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_SENT, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.SMS_CREDIT_CHANGED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_RECEIVED_FROM_OTHER_CLIENT, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.SERVER_RECEIVED_MSG, this);
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.MESSAGE_SENT.equals(type))
		{
			ConvMessage convMessage = (ConvMessage) object;
			long msgID = mConversationDb.addConversationMessages(convMessage);
			convMessage.setMsgID(msgID); // set the msgID for this message.
			mPubSub.publish(HikePubSub.WS_SEND, convMessage.serialize("send")); // this is used to be sent by the web socket.
		}
		else if (HikePubSub.MESSAGE_RECEIVED_FROM_OTHER_CLIENT.equals(type))  // represents event when a client receive msg from other client through server.
		{
			ConvMessage message = (ConvMessage) object;
			mConversationDb.addConversationMessages(message);
			mPubSub.publish(HikePubSub.MESSAGE_RECEIVED, message);
			mPubSub.publish(HikePubSub.WS_SEND, message.serializeDeliveryReport("msgDelivered")); // handle return to sender
		}
		else if (HikePubSub.SMS_CREDIT_CHANGED.equals(type))
		{
			Integer credits = (Integer) object;
			mEditor.putInt(HikeMessengerApp.SMS_SETTING, credits.intValue());
			mEditor.commit();
		} 
		else if (HikePubSub.SERVER_RECEIVED_MSG.equals(type))  // server got msg from client 1 and sent back received msg receipt
		{
			updateDB(object,ConvMessage.State.SENT_CONFIRMED.ordinal());
		}
		else if (HikePubSub.MESSAGE_DELIVERED.equals(type))  // server got msg from client 1 and sent back received msg receipt
		{
			updateDB(object,ConvMessage.State.SENT_DELIVERED.ordinal());
		}
		
		else if (HikePubSub.MESSAGE_DELIVERED_READ.equals(type))  // server got msg from client 1 and sent back received msg receipt
		{
			updateDB(object,ConvMessage.State.SENT_DELIVERED_READ.ordinal());
		}
		
	}

	private void updateDB(Object object, int status)
	{
		long msgID = (Long)object;
		/* TODO we should lookup the convid for this user, since otherwise one could set mess with the state for other conversations */
		int rowsAffected = mConversationDb.updateMsgStatus(0,msgID,status);
		if(rowsAffected<=0) // signifies no msg.
		{
			Log.e("DbConversationListener", "Could not update msgstatus for message: " + msgID);
			// TODO : Handle this case
		}	
	}
}
