package com.bsb.hike.models.Conversation;

import com.bsb.hike.HikeConstants;

import org.json.JSONException;

public class OneToOneConversationMetadata  extends ConversationMetadata
{

    /**
     * @param jsonString
     * @throws JSONException
     */
    public OneToOneConversationMetadata(String jsonString) throws JSONException
    {
        super(jsonString);
        if (jsonString == null)
        {
            setShowUnknownUserInfo(HikeConstants.SHOW_USER_IN_INSIDE_CHAT_VIA_UNKNOWN_USER, true);
        }
    }
}
