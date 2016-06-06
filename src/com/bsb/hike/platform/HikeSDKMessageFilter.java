package com.bsb.hike.platform;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.ConvMessage;

public class HikeSDKMessageFilter implements HikePlatformConstants{
	public static ConvMessage filterMessage(JSONObject json, Context context){
		try{

		ConvMessage convMessage = new ConvMessage();
        convMessage.setIsSent(true);
            //TODO make it generic
        convMessage.setMessageType(HikeCardConstants.CARD_MSG_TYPE.equals(json.optString(MESSAGE_TYPE)) ? HikeConstants.MESSAGE_TYPE.CONTENT
                : HikeConstants.MESSAGE_TYPE.PLAIN_TEXT);
        validateLayoutId(json);
		convMessage.platformMessageMetadata = new PlatformMessageMetadata(json.optJSONObject(DATA));
        convMessage.setMessage(convMessage.platformMessageMetadata.notifText);
		// fetch love here is present
		return convMessage;

		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		return null;
	}

    private static void validateLayoutId(JSONObject json) {

        if (!json.has(DATA))
            return;
        JSONObject data  = json.optJSONObject(DATA);

        try {
            int layoutId = data.optInt(LAYOUT_ID);
            if ((layoutId != CardConstants.GAMES_CARD_LAYOUT))
                data.put(LAYOUT_ID, CardConstants.GAMES_CARD_LAYOUT);

        } catch (JSONException e) {
            e.printStackTrace();
        }


    }
}
