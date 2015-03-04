package com.bsb.hike.platform;

import java.net.URISyntaxException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.ui.ComposeChatActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * Created by shobhitmandloi on 30/10/14.
 */
public class CardController {


    public static void callToAction(JSONObject androidIntent, Context mContext) throws URISyntaxException
    {
        Intent intent = Intent.parseUri(androidIntent.optString(HikePlatformConstants.INTENT_URI), 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    public static void callToActionWebView(JSONObject androidIntent, Context mContext) throws URISyntaxException
    {
        Utils.startWebViewActivity(mContext,androidIntent.optString(HikePlatformConstants.INTENT_URI), "Hike");
    }

    public static void forwardCalltoAction(Context mContext, ConvMessage convMessage){

        Intent intent = new Intent(mContext, ComposeChatActivity.class);
        String msg;
        intent.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
        JSONArray multipleMsgArray = new JSONArray();
        JSONObject multiMsgFwdObject = new JSONObject();
        try {
            multiMsgFwdObject.put(HikeConstants.MESSAGE_TYPE.MESSAGE_TYPE, convMessage.getMessageType());
            multiMsgFwdObject.put(HikeConstants.MESSAGE_TYPE.MESSAGE_TYPE, HikeConstants.MESSAGE_TYPE.CONTENT);
            if(convMessage.platformMessageMetadata!=null){
                multiMsgFwdObject.put(HikeConstants.METADATA, convMessage.platformMessageMetadata.JSONtoString());
            }
            multipleMsgArray.put(multiMsgFwdObject);
        } catch (JSONException e) {
            Logger.e("CardRenderer", "Invalid JSON", e);
        }
        String phoneNumber = convMessage.getMsisdn();
        ContactInfo contactInfo = ContactManager.getInstance().getContactInfoFromPhoneNo(phoneNumber);
        String mContactName = contactInfo.getName();
        intent.putExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT, multipleMsgArray.toString());
        intent.putExtra(HikeConstants.Extras.PREV_MSISDN, convMessage.getMsisdn());
        intent.putExtra(HikeConstants.Extras.PREV_NAME, mContactName);
        mContext.startActivity(intent);

    }

}
