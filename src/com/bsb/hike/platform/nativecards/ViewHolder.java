package com.bsb.hike.platform.nativecards;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.CardComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by varunarora on 11/06/16.
 */
public abstract class ViewHolder extends ActionViewHolder
{
    protected HashMap<String, View> viewHashMap;

    protected View cardContainer;

    public ViewHolder(Context context) {
        super(context);
    }
    public Map<String,View> getViewHashMap(){
        return viewHashMap;
    }
    public void initializeHolder(View view, final ConvMessage convMessage)
    {
        List<CardComponent.TextComponent> textComponents = convMessage.platformMessageMetadata.cards.get(0).textComponents;
        List<CardComponent.MediaComponent> mediaComponents = convMessage.platformMessageMetadata.cards.get(0).mediaComponents;
        List<CardComponent.ActionComponent> actionComponents = convMessage.platformMessageMetadata.cards.get(0).actionComponents;
        List<CardComponent.ImageComponent> imageComponents = convMessage.platformMessageMetadata.cards.get(0).imageComponents;

        boolean isSent = convMessage.isSent();
        viewHashMap = new HashMap<String, View>();
        time = (TextView) view.findViewById(R.id.time);
        status = (ImageView) view.findViewById(R.id.status);
        timeStatus = (View) view.findViewById(R.id.time_status);
        selectedStateOverlay = view.findViewById(R.id.selected_state_overlay);
        messageContainer = (ViewGroup) view.findViewById(R.id.message_container);
        dayStub = (ViewStub) view.findViewById(R.id.day_stub);
        messageInfoStub = (ViewStub) view.findViewById(R.id.message_info_stub);
        this.convMessage = convMessage;
        if (convMessage.platformMessageMetadata.isWideCard())
        {
            messageContainer.getLayoutParams().width = (int) mContext.getResources().getDimension(R.dimen.native_card_message_container_wide_width);
        }
        else
        {
            messageContainer.getLayoutParams().width = (int) mContext.getResources().getDimension(R.dimen.native_card_message_container_narror_width);
        }

        for (CardComponent.TextComponent textComponent : textComponents)
        {
            String tag = textComponent.getTag();
            if (!TextUtils.isEmpty(tag))
                viewHashMap.put(tag, view.findViewWithTag(tag));
        }

        for (int i = 0; i < mediaComponents.size(); i++)
        {
            CardComponent.MediaComponent mediaComponent = mediaComponents.get(i);
            String tag = mediaComponent.getTag();
            if (!TextUtils.isEmpty(tag))
                viewHashMap.put(tag, view.findViewWithTag(tag));

        }
        for (int i = 0; i < imageComponents.size(); i++)
        {
            CardComponent.ImageComponent mediaComponent = imageComponents.get(i);
            String tag = mediaComponent.getTag();
            if (!TextUtils.isEmpty(tag))
                viewHashMap.put(tag, view.findViewWithTag(tag));
        }

        if (isSent)
        {
            initializeHolderForSender(view);
        }
        else
        {
            initializeHolderForReceiver(view);
        }
        cardContainer = view.findViewById(R.id.card_container);
        showActionContainer(view, cardContainer, actionComponents);

    }

    public void initializeHolderForSender(View view)
    {
        messageInfoStub = (ViewStub) view.findViewById(R.id.message_info_stub);
    }

    public void initializeHolderForReceiver(View view)
    {
        senderDetails = view.findViewById(R.id.sender_details);
        senderName = (TextView) view.findViewById(R.id.sender_name);
        senderNameUnsaved = (TextView) view.findViewById(R.id.sender_unsaved_name);
        avatarImage = (ImageView) view.findViewById(R.id.avatar);
        avatarContainer = (ViewGroup) view.findViewById(R.id.avatar_container);

    }

    public abstract void processViewHolder(View view);
    public void clearViewHolder(View view) {}
}
