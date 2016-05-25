package com.bsb.hike.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author paramshah
 */
public class UJNotifClickReceiver extends BroadcastReceiver
{
    private final String TAG = "ujNotifClickReceiver";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Logger.d(TAG, "received uj notif click intent");
        String broadcastAction = intent.getAction();
        if(broadcastAction.equals(HikeConstants.UserJoinMsg.NOTIF_ACTION_INTENT))
        {
            String ujAction = intent.getStringExtra(HikeConstants.MqttMessageTypes.ACTION);
            String msisdn = intent.getStringExtra(HikeConstants.MSISDN);
            switch (ujAction)
            {
                case HikeConstants.UserJoinMsg.ACTION_ADD_FRIEND:
                    processActionAddFriend(context, intent, msisdn);
                    break;

                case HikeConstants.UserJoinMsg.ACTION_SAY_HI:
                    processActionSayHi(context, intent, msisdn);
                    break;

                case HikeConstants.UserJoinMsg.ACTION_DEFAULT:
                    processActionDefault(context, intent, msisdn);
                    break;
            }
        }

    }

    private void processActionSayHi(Context context, Intent intent, String msisdn)
    {
        Intent ujActionIntent = IntentFactory.createChatThreadIntentFromMsisdn(context, msisdn, true, false, ChatThreadActivity.ChatThreadOpenSources.NOTIF);
        String metadata = intent.getStringExtra(HikeConstants.METADATA);
        String msg = null;
        if(!TextUtils.isEmpty(metadata))
        {
            try
            {
                JSONObject mdJSON = new JSONObject(metadata);
                msg = mdJSON.optString(HikeConstants.MESSAGE);
            }
            catch (JSONException jse)
            {
                Logger.d(TAG, "json exception in retreiving metadata for action");
            }
        }

        if(TextUtils.isEmpty(msg))
        {
            msg = context.getString(R.string.uj_default_pretype_text);
        }
        ujActionIntent.putExtra(HikeConstants.MESSAGE, msg);
        openActivity(context, ujActionIntent);
    }

    private void processActionAddFriend(Context context, Intent intent, String msisdn)
    {
        ContactInfo contactInfo = ContactManager.getInstance().getContact(msisdn, true, true);
        Utils.addFavorite(context, contactInfo, false, HikeConstants.AddFriendSources.NOTIF);
        Intent ujActionIntent = IntentFactory.createChatThreadIntentFromMsisdn(context, msisdn, true, false, ChatThreadActivity.ChatThreadOpenSources.NOTIF);
        openActivity(context, ujActionIntent);
    }

    private void processActionDefault(Context context, Intent intent, String msisdn)
    {
        Intent ujActionIntent = IntentFactory.createChatThreadIntentFromMsisdn(context, msisdn, false, false, ChatThreadActivity.ChatThreadOpenSources.NOTIF);
        openActivity(context, ujActionIntent);
    }

    private void openActivity(Context context, Intent ujActionIntent)
    {
        if(ujActionIntent != null)
        {
            ujActionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(ujActionIntent);
            closeNotifTray(context);
        }
    }

    private void closeNotifTray(Context context)
    {
        Intent closeNotifTray = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(closeNotifTray);
    }
}
