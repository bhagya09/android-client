package com.bsb.hike.platform;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.widget.GridLayoutManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.CardComponent;
import com.bsb.hike.platform.nativecards.NativeCardManager;
import com.bsb.hike.platform.nativecards.NativeCardUtils;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontTextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pushkargupta on 13/04/16.
 */
public class NativeCardRenderer implements View.OnLongClickListener
{

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    private Context context;
    ViewHolderFactory viewHolderFactory;
    public NativeCardRenderer(Context context)
    {
        this.context =context;
       viewHolderFactory = new ViewHolderFactory(context);
    }
    public View getView(View view, final ConvMessage convMessage, ViewGroup parent)
    {
        int cardType = convMessage.platformMessageMetadata.layoutId;
        ViewHolderFactory.ViewHolder viewHolder;
        if (view == null)
        {
            view = NativeCardManager.getInflatedViewAsPerType(context, cardType, parent, convMessage.isSent());
            viewHolder = viewHolderFactory.getViewHolder(cardType);
            view.setTag(viewHolder);
        }
        else
        {
            viewHolder = (ViewHolderFactory.ViewHolder) view.getTag();
        }
        viewHolder.clearViewHolder(view);
        viewHolder.initializeHolder(view, convMessage);
        cardDataFiller(convMessage, viewHolder);
        viewHolder.processViewHolder(view);
        return view;
    }
    public int getCardCount()
    {
        //Multiplying by 2 so as to consider both the sent and received types
        return NativeCardManager.NativeCardType.values().length * 2;
    }
    public int getItemViewType(ConvMessage convMessage)
    {
        //Add the length of NativeCardType enum for received card type.
        return convMessage.isSent()?convMessage.platformMessageMetadata.layoutId:convMessage.platformMessageMetadata.layoutId + NativeCardManager.NativeCardType.values().length;
    }
    private void cardDataFiller(final ConvMessage convMessage, final ViewHolderFactory.ViewHolder viewHolder)
    {
        if(!TextUtils.isEmpty(convMessage.platformMessageMetadata.backgroundColor)){
            Utils.setRectangularBackground(viewHolder.messageContainer,Color.parseColor(convMessage.platformMessageMetadata.backgroundColor) );
//           viewHolder.messageContainer.setBackgroundColor(Color.parseColor(convMessage.platformMessageMetadata.backgroundColor));
        }
        for (CardComponent.TextComponent textComponent : convMessage.platformMessageMetadata.textComponents)
        {
            String tag = textComponent.getTag();
            if (!TextUtils.isEmpty(tag))
            {

                CustomFontTextView tv = (CustomFontTextView) viewHolder.viewHashMap.get(tag);
                if (null != tv)
                {
                    tv.setVisibility(View.VISIBLE);
                    tv.setText(textComponent.getText());
                }
            }

        }

        for (CardComponent.MediaComponent mediaComponent : convMessage.platformMessageMetadata.mediaComponents)
        {
            String tag = mediaComponent.getTag();

            if (!TextUtils.isEmpty(tag))
            {
                View mediaView = viewHolder.viewHashMap.get(tag);
                if (null != mediaView && mediaView instanceof ImageView)
                {

                    mediaView.setVisibility(View.VISIBLE);
                    String data = mediaComponent.getKey();
                    BitmapDrawable value = HikeMessengerApp.getLruCache().getBitmapDrawable(data);

                    ((ImageView) mediaView).setImageDrawable(value);

                }

            }
        }

    }

    private void cardCallToActions(ArrayList<CardComponent.ActionComponent> actionComponents, final ViewHolderFactory.ViewHolder viewHolder, final boolean isAppInstalled, final String channelSource)
    {
        for (final CardComponent.ActionComponent actionComponent : actionComponents)
        {
            final String tag = actionComponent.getTag();
            if (!TextUtils.isEmpty(tag))
            {
                View actionView = viewHolder.viewHashMap.get(tag);
                actionView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {

                            TextView cardTitleView = (TextView) v.findViewWithTag(context.getString(R.string.content_card_title_tag));
                            TextView actionTextView = (TextView) v.findViewWithTag(context.getString(R.string.content_card_action_tag));
                            String cardName = (String) cardTitleView.getText();
                            String actionText = (String) actionTextView.getText();


                            if (tag.equalsIgnoreCase(context.getString(R.string.content_card_tag)) && !isAppInstalled) {

                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put(HikePlatformConstants.INTENT_URI, CardConstants.PLAY_STORE_TEXT + channelSource);
                                CardController.callToAction(jsonObject, context);
                            } else {
                                CardController.callToAction(actionComponent.getAndroidIntent(), context);
                            }
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
        }
    }


}
