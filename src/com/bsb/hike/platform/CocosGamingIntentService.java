package com.bsb.hike.platform;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.text.TextUtils;

import com.bsb.hike.models.MessageEvent;
import com.bsb.hike.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

/**
 * Created by sagar on 30/10/15.
 */
public class CocosGamingIntentService extends IntentService
{

    public static final String MESSAGE_EVENT_RECEIVED_DATA = "messageEventRecData";

    public CocosGamingIntentService()
    {
        super("CocosGamingIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        Bundle bundleData = intent.getExtras();
        MessageEvent messageEvent = (MessageEvent)bundleData.getParcelable(MESSAGE_EVENT_RECEIVED_DATA);
        if(messageEvent != null)
        {
            NativeBridge nativeBridgeInstance = (NativeBridge) CocosGamingActivity.getNativeBridge();
            if( nativeBridgeInstance != null )
            {
                String parent_msisdn = messageEvent.getParent_msisdn();
                if (!TextUtils.isEmpty(parent_msisdn))
                {
                    List<String> parent_msisdns = Arrays.asList(parent_msisdn.split(",", -1));
                    if (parent_msisdns.size() > 0 && parent_msisdns.contains(nativeBridgeInstance.msisdn))
                    {
                        try {
                            JSONObject jsonObject = PlatformUtils.getPlatformContactInfo(nativeBridgeInstance.msisdn);
                            jsonObject.put(HikePlatformConstants.EVENT_DATA, messageEvent.getEventMetadata());
                            jsonObject.put(HikePlatformConstants.EVENT_ID, messageEvent.getEventId());
                            jsonObject.put(HikePlatformConstants.EVENT_STATUS, messageEvent.getEventStatus());
                            jsonObject.put(HikePlatformConstants.EVENT_TYPE, messageEvent.getEventType());

                            nativeBridgeInstance.eventReceived(jsonObject.toString());
                        }
                        catch (JSONException e)
                        {
                            Logger.e(nativeBridgeInstance.activity.TAG, "JSON Exception in message event received");
                        }
                    }
                }

            }
        }

    }
}
