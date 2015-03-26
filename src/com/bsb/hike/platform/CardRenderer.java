package com.bsb.hike.platform;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.view.CustomFontTextView;

/**
 * Created by shobhit on 29/10/14.
 */
public class CardRenderer implements View.OnLongClickListener {

    Context mContext;

    public CardRenderer(Context context) {
        this.mContext = context;

    }

    private static final int IMAGE_CARD_LAYOUT_SENT = 0;
    private static final int IMAGE_CARD_LAYOUT_RECEIVED = 1;
    private static final int VIDEO_CARD_LAYOUT_SENT = 2;
    private static final int VIDEO_CARD_LAYOUT_RECEIVED = 3;
    private static final int GAMES_CARD_LAYOUT_SENT = 4;
    private static final int GAMES_CARD_LAYOUT_RECEIVED = 5;
    private static final int ARTICLE_CARD_LAYOUT_SENT = 6;
    private static final int ARTICLE_CARD_LAYOUT_RECEIVED = 7;
    private static final int COLOR_CARD_LAYOUT_SENT = 8;
    private static final int COLOR_CARD_LAYOUT_RECEIVED = 9;


    public static class ViewHolder extends MessagesAdapter.DetailViewHolder {

        HashMap<String, View> viewHashMap;

        public void initializeHolder(View view, List<CardComponent.TextComponent> textComponentList, List<CardComponent.MediaComponent> mediaComponentList, ArrayList<CardComponent.ActionComponent> actionComponents) {

            viewHashMap = new HashMap<String, View>();
            time = (TextView) view.findViewById(R.id.time);
            status = (ImageView) view.findViewById(R.id.status);
            timeStatus = (View) view.findViewById(R.id.time_status);
            selectedStateOverlay = view.findViewById(R.id.selected_state_overlay);
            messageContainer = (ViewGroup) view.findViewById(R.id.message_container);
            dayStub = (ViewStub) view.findViewById(R.id.day_stub);
            messageInfoStub = (ViewStub) view.findViewById(R.id.message_info_stub);


            for (CardComponent.TextComponent textComponent : textComponentList) {
                String tag = textComponent.getTag();
                if (!TextUtils.isEmpty(tag))
                    viewHashMap.put(tag, view.findViewWithTag(tag));
            }

            for (CardComponent.MediaComponent mediaComponent : mediaComponentList) {
                String tag = mediaComponent.getTag();
                if (!TextUtils.isEmpty(tag))
                    viewHashMap.put(tag, view.findViewWithTag(tag));
            }

            for (CardComponent.ActionComponent actionComponent : actionComponents) {
                String tag = actionComponent.getTag();
                if (!TextUtils.isEmpty(tag))
                    viewHashMap.put(tag, view.findViewWithTag(tag));
            }

        }

        public void initializeHolderForSender(View view) {

            messageInfoStub = (ViewStub) view.findViewById(R.id.message_info_stub);

        }

        public void initializeHolderForReceiver(View view) {

            senderDetails = view.findViewById(R.id.sender_details);
            senderName = (TextView) view.findViewById(R.id.sender_name);
            senderNameUnsaved = (TextView) view.findViewById(R.id.sender_unsaved_name);
            avatarImage = (ImageView) view.findViewById(R.id.avatar);
            avatarContainer = (ViewGroup) view.findViewById(R.id.avatar_container);

        }

    }


    public int getCardCount() {
        return CardConstants.CHAT_THREAD_CARD_COUNT_TYPE_COUNT;
    }

    public int getItemViewType(ConvMessage convMessage) {

        Logger.d(CardRenderer.class.getSimpleName(), "hash code for convMessage is " + String.valueOf(convMessage.hashCode()));
        int cardType = convMessage.platformMessageMetadata.layoutId;
        if (convMessage.isSent()) {

            switch (cardType) {
                case 1:
                    return IMAGE_CARD_LAYOUT_SENT;

                case 2:
                    return VIDEO_CARD_LAYOUT_SENT;

                case 3:
                    return GAMES_CARD_LAYOUT_SENT;

                case 4:
                    return ARTICLE_CARD_LAYOUT_SENT;

                case 5:
                    return COLOR_CARD_LAYOUT_SENT;


            }

        } else {
            switch (cardType) {
                case 1:
                    return IMAGE_CARD_LAYOUT_RECEIVED;

                case 2:
                    return VIDEO_CARD_LAYOUT_RECEIVED;

                case 3:
                    return GAMES_CARD_LAYOUT_RECEIVED;

                case 4:
                    return ARTICLE_CARD_LAYOUT_RECEIVED;

                case 5:
                    return COLOR_CARD_LAYOUT_RECEIVED;


            }

        }
        return 0;

    }

    public View getView(View view, final ConvMessage convMessage, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        int cardType = convMessage.platformMessageMetadata.layoutId;
        List<CardComponent.TextComponent> textComponents = convMessage.platformMessageMetadata.textComponents;
        List<CardComponent.MediaComponent> mediaComponents = convMessage.platformMessageMetadata.mediaComponents;
        ArrayList<CardComponent.ActionComponent> actionComponents = convMessage.platformMessageMetadata.actionComponents;

        if (cardType == CardConstants.IMAGE_CARD_LAYOUT) {
            ViewHolder viewHolder;
            if (view == null) {
                viewHolder = new ViewHolder();
                if (convMessage.isSent()) {
                    view = inflater.inflate(R.layout.card_layout_games_sent, parent, false);
                    viewHolder.initializeHolderForSender(view);
                } else {
                    view = inflater.inflate(R.layout.card_layout_games_received, parent, false);
                    viewHolder.initializeHolderForReceiver(view);
                }
                viewHolder.initializeHolder(view, textComponents, mediaComponents, actionComponents);
                view.setTag(viewHolder);
                cardCallToActions(cardType, actionComponents, viewHolder, true, "");
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            cardDataFiller(cardType, textComponents, mediaComponents, viewHolder);


        } else if (cardType == CardConstants.VIDEO_CARD_LAYOUT) {
            ViewHolder viewHolder;
            if (view == null) {
                viewHolder = new ViewHolder();
                if (convMessage.isSent()) {
                    view = inflater.inflate(R.layout.card_layout_video_sent, parent, false);
                    viewHolder.initializeHolderForSender(view);
                } else {
                    view = inflater.inflate(R.layout.card_layout_video_received, parent, false);
                    viewHolder.initializeHolderForReceiver(view);
                }
                viewHolder.initializeHolder(view, textComponents, mediaComponents, actionComponents);
                cardCallToActions(cardType, actionComponents, viewHolder, true, "");
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            cardDataFiller(cardType, textComponents, mediaComponents, viewHolder);


        } else if (cardType == CardConstants.GAMES_CARD_LAYOUT) {
            ViewHolder viewHolder;
            boolean isGamesAppInstalled = convMessage.platformMessageMetadata.isInstalled;
            if (view == null) {
                viewHolder = new ViewHolder();

                if (convMessage.isSent()) {
                    view = inflater.inflate(R.layout.card_layout_games_sent, parent, false);
                    viewHolder.initializeHolderForSender(view);
                } else {
                    view = inflater.inflate(R.layout.card_layout_games_received, parent, false);
                    viewHolder.initializeHolderForReceiver(view);
                }
                viewHolder.initializeHolder(view, textComponents, mediaComponents, actionComponents);

                String channelSource = convMessage.platformMessageMetadata.channelSource;
                cardCallToActions(cardType, actionComponents, viewHolder, isGamesAppInstalled, channelSource);
                view.setTag(viewHolder);

            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            cardDataFiller(cardType, textComponents, mediaComponents, viewHolder);
            if (!isGamesAppInstalled) {
                gameInstalledTextFiller(viewHolder);
            }


        } else if (cardType == CardConstants.ARTICLE_CARD_LAYOUT) {
            ViewHolder viewHolder;
            if (view == null) {
                viewHolder = new ViewHolder();
                if (convMessage.isSent()) {
                    view = inflater.inflate(R.layout.card_layout_article_sent, parent, false);
                    viewHolder.initializeHolderForSender(view);
                } else {
                    view = inflater.inflate(R.layout.card_layout_article_received, parent, false);
                    viewHolder.initializeHolderForReceiver(view);
                }
                viewHolder.initializeHolder(view, textComponents, mediaComponents, actionComponents);
                cardCallToActions(cardType, actionComponents, viewHolder, true, "");
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            cardDataFiller(cardType, textComponents, mediaComponents, viewHolder);


        } if (cardType == CardConstants.COLOR_CARD_LAYOUT) {
            ViewHolder viewHolder;
            if (view == null) {
                viewHolder = new ViewHolder();
                if (convMessage.isSent()) {
                    view = inflater.inflate(R.layout.card_layout_color_sent, parent, false);
                    viewHolder.initializeHolderForSender(view);
                } else {
                    view = inflater.inflate(R.layout.card_layout_color_received, parent, false);
                    viewHolder.initializeHolderForReceiver(view);
                }
                viewHolder.initializeHolder(view, textComponents, mediaComponents, actionComponents);
                cardCallToActions(cardType, actionComponents, viewHolder, true, "");
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            cardDataFiller(cardType, textComponents, mediaComponents, viewHolder);

            forwardCallAction(convMessage, view);

        }

        return view;
    }

    private void forwardCallAction(final ConvMessage convMessage , View view) {

        view.findViewById(R.id.forward_switch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CardController.forwardCalltoAction(mContext, convMessage);
            }
        });


    }



    private void cardCallToActions(final int cardType, ArrayList<CardComponent.ActionComponent> actionComponents, final ViewHolder viewHolder, final boolean isAppInstalled, final String channelSource) {
        for (final CardComponent.ActionComponent actionComponent : actionComponents) {
            final String tag = actionComponent.getTag();
            if (!TextUtils.isEmpty(tag)) {
                View actionView = viewHolder.viewHashMap.get(tag);
                actionView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {

                            if (cardType == CardConstants.GAMES_CARD_LAYOUT) {

                                TextView cardTitleView  = (TextView) v.findViewWithTag(mContext.getString(R.string.content_card_title_tag));
                                TextView actionTextView = (TextView) v.findViewWithTag(mContext.getString(R.string.content_card_action_tag));
                                String cardName = (String) cardTitleView.getText();
                                String actionText = (String) actionTextView.getText();
                                sendLogEvent(cardName, actionText);

                                if (tag.equalsIgnoreCase(mContext.getString(R.string.content_card_tag)) && !isAppInstalled) {

                                    JSONObject jsonObject = new JSONObject();
                                    jsonObject.put(HikePlatformConstants.INTENT_URI, CardConstants.PLAY_STORE_TEXT + channelSource);
                                    CardController.callToAction(jsonObject, mContext);
                                } else {
                                    CardController.callToAction(actionComponent.getAndroidIntent(), mContext);
                                }
                            } else {
                                CardController.callToActionWebView(actionComponent.getAndroidIntent(), mContext);
                            }
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

                actionView.setOnLongClickListener(this);

            }
        }
    }

    private void sendLogEvent(String cardName, String actionText) throws JSONException {
        JSONObject metadata = new JSONObject();
        metadata.put(CardConstants.CARD_NAME, cardName);
        metadata.put(CardConstants.ACTION_TEXT, actionText);
        metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.CONTENT_CARD_TAPPED);
        metadata.put(HikeConstants.LogEvent.SOURCE_APP, HikePlatformConstants.GAME_SDK_ID);
        HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
    }


    private void cardDataFiller(int cardType, List<CardComponent.TextComponent> textComponents, List<CardComponent.MediaComponent> mediaComponents, ViewHolder viewHolder) {

        for (CardComponent.TextComponent textComponent : textComponents) {
            String tag = textComponent.getTag();
            if (!TextUtils.isEmpty(tag)) {

                CustomFontTextView tv = (CustomFontTextView) viewHolder.viewHashMap.get(tag);
                if (null != tv)
                {
                    tv.setVisibility(View.VISIBLE);
                    tv.setText(textComponent.getText());
                }
            }

        }


        for (CardComponent.MediaComponent mediaComponent : mediaComponents) {
            String tag = mediaComponent.getTag();

            if (!TextUtils.isEmpty(tag)) {
                View mediaView = viewHolder.viewHashMap.get(tag);
                if (null != mediaView && mediaView instanceof ImageView) {

                    mediaView.setVisibility(View.VISIBLE);
                    String data = mediaComponent.getKey();
                    BitmapDrawable value = HikeMessengerApp.getLruCache().getBitmapDrawable(data);

                    ((ImageView) mediaView).setImageDrawable(value);

                }

            }
        }

    }


    private void gameInstalledTextFiller(ViewHolder viewHolder) {
        if (viewHolder.viewHashMap.containsKey("T3")) {
            CustomFontTextView cardInstalledText = (CustomFontTextView) viewHolder.viewHashMap.get("T3");
            cardInstalledText.setVisibility(View.VISIBLE);
            cardInstalledText.setText(mContext.getString(R.string.install_text));
        }
        if (viewHolder.viewHashMap.containsKey("T2")) {
            CustomFontTextView cardInstalledSubtext = (CustomFontTextView) viewHolder.viewHashMap.get("T2");
            cardInstalledSubtext.setVisibility(View.VISIBLE);
            cardInstalledSubtext.setText(mContext.getString(R.string.install_description));
        }
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }




}
