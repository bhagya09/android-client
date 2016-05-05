package com.bsb.hike.platform;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.nativecards.NativeCardManager;
import com.bsb.hike.platform.nativecards.NativeCardUtils;
import com.bsb.hike.utils.IntentFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by pushkargupta on 15/04/16.
 * This class gets the JsonObject and returns the view
 */
public class ViewHolderFactory
{
    private Context mContext;
    public ViewHolderFactory(Context context)
    {
        mContext =context;
    }
    private  NativeCardManager.NativeCardType[] cardTypes = NativeCardManager.NativeCardType.values();
    public abstract  class ShareViewHolder extends MessagesAdapter.DetailViewHolder{
        public ViewStub shareStub;
        public View shareStubInflated;
        protected void showShare(final View view){
            if(shareStubInflated == null){
                shareStub = (ViewStub)view.findViewById(R.id.share_stub);
                shareStubInflated = shareStub.inflate();
            }
            shareStubInflated.setVisibility(View.VISIBLE);
            shareStubInflated.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        shareCard(view);
                    }
                });
        }
        public abstract void shareCard(View view);
    }

    public abstract  class ViewHolder extends ShareViewHolder
    {
        HashMap<String, View> viewHashMap;
        protected ConvMessage convMessage;
        public void initializeHolder(View view, ConvMessage convMessage)
        {
            List<CardComponent.TextComponent> textComponents = convMessage.platformMessageMetadata.textComponents;
            List<CardComponent.MediaComponent> mediaComponents = convMessage.platformMessageMetadata.mediaComponents;
            ArrayList<CardComponent.ActionComponent> actionComponents = convMessage.platformMessageMetadata.actionComponents;
            ViewHolderFactory.ViewHolder viewHolder;
            boolean showShare = convMessage.platformMessageMetadata.showShare;
            boolean isSent = convMessage.isSent();
            viewHashMap = new HashMap<String, View>();
            time = (TextView) view.findViewById(R.id.time);
            status = (ImageView) view.findViewById(R.id.status);
            timeStatus = (View) view.findViewById(R.id.time_status);
            selectedStateOverlay = view.findViewById(R.id.selected_state_overlay);
            messageContainer = (ViewGroup) view.findViewById(R.id.message_container);
            dayStub = (ViewStub) view.findViewById(R.id.day_stub);
            messageInfoStub = (ViewStub) view.findViewById(R.id.message_info_stub);
            this.convMessage =convMessage;

            for (CardComponent.TextComponent textComponent : textComponents)
            {
                String tag = textComponent.getTag();
                if (!TextUtils.isEmpty(tag))
                    viewHashMap.put(tag, view.findViewWithTag(tag));
            }

            for (CardComponent.MediaComponent mediaComponent : mediaComponents)
            {
                String tag = mediaComponent.getTag();
                if (!TextUtils.isEmpty(tag))
                    viewHashMap.put(tag, view.findViewWithTag(tag));
            }

            for (CardComponent.ActionComponent actionComponent : actionComponents)
            {
                String tag = actionComponent.getTag();
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
            if(showShare){
                showShare(view);
            }
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
        public abstract void clearViewHolder(View view);
        public abstract void processViewHolder(View view);

    }

    public  class HikeDailyViewHolder extends ViewHolder
    {
        public void clearViewHolder(View view){
            if(shareStubInflated != null){
                shareStubInflated.setVisibility(View.GONE);
            }
            TextView t1Text = (TextView)view.findViewWithTag("T1");
            t1Text.setVisibility(View.GONE);
            TextView t2Text = (TextView)view.findViewWithTag("T2");
            t2Text.setVisibility(View.GONE);
        }

        @Override
        public void processViewHolder(View view) {

        }

        @Override
        public void shareCard(View view) {
            LinearLayout cardContainer = (LinearLayout)view.findViewById(R.id.card_container);
            Uri fileUri=NativeCardUtils.getFileForView((View) cardContainer, HikeMessengerApp.getInstance());
            Intent intent = IntentFactory.getForwardIntentForCards(mContext, convMessage,fileUri);
            mContext.startActivity(intent);

        }
    }
    public class JFLViewHolder extends ViewHolder
    {
        public void clearViewHolder(View view){
            if(shareStubInflated != null){
                shareStubInflated.setVisibility(View.GONE);
            }
            ImageView i1Image = (ImageView)view.findViewWithTag("I1");
            i1Image.setVisibility(View.GONE);
            TextView t2Text = (TextView)view.findViewWithTag("T1");
            t2Text.setVisibility(View.GONE);
        }

        @Override
        public void processViewHolder(View view) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)messageContainer.getLayoutParams();
            layoutParams.width = ((ImageView)view.findViewWithTag("I1")).getDrawable().getIntrinsicWidth();
            messageContainer.setLayoutParams(layoutParams);
        }

        @Override
        public void shareCard(View view) {
            LinearLayout cardContainer = (LinearLayout)view.findViewById(R.id.card_container);
            Uri fileUri=NativeCardUtils.getFileForView((View) cardContainer, HikeMessengerApp.getInstance());
            Intent intent = IntentFactory.getForwardIntentForCards(mContext, convMessage,fileUri);
            mContext.startActivity(intent);
        }
    }
    public ViewHolder getViewHolder(int type){
        switch (cardTypes[type]){
            case HIKE_DAILY:
                return new HikeDailyViewHolder();
            case JFL:
                return new JFLViewHolder();
            default:
                return null;
        }
    }
}
