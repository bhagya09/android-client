package com.bsb.hike.voip.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.chatthread.ChatThread;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPConstants;

public class CallDeclineWithMessageFragment extends DialogFragment
{
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		final String msisdn = getArguments().getString(VoIPConstants.PARTNER_MSISDN);

		AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
		final String[] messages = getResources().getStringArray(R.array.voip_decline_message_texts);

		dialog.setItems(messages, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				sendMessageOnDecline(msisdn, messages[which]);
			}
		});
		return dialog.create();
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{	
		super.onSaveInstanceState(outState);
	}

	public void sendMessageOnDecline(final String msisdn, final String message) 
	{
		new Handler().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				ConvMessage convMessage = Utils.makeConvMessage(msisdn, message, true);
				ChatThread.addtoMessageMap(convMessage);
				HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessage);
				HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_THREAD, convMessage);
				
			}
		}, 1000);

		HikeMessengerApp.getPubSub().publish(HikePubSub.STOP_VOIP_SERVICE, null);
	}
}
