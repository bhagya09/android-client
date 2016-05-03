package com.bsb.hike.platform;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.platform.nativecards.NativeCardManager;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.view.CustomFontTextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by pushkargupta on 15/04/16.
 * This class gets the JsonObject and returns the view
 */
public class ViewHolderFactory
{
    private static NativeCardManager.NativeCardType[] cardTypes = NativeCardManager.NativeCardType.values();
    public static class ShareViewHolder extends MessagesAdapter.DetailViewHolder{
        public ViewStub shareStub;
        public View shareStubInflated;
        protected void showShare(View view){
            if(shareStubInflated == null){
                shareStub = (ViewStub)view.findViewById(R.id.share_stub);
                shareStubInflated = shareStub.inflate();
            }
            shareStubInflated.setVisibility(View.VISIBLE);
        }
    }

    public abstract static class ViewHolder extends ShareViewHolder
    {
        HashMap<String, View> viewHashMap;

        public void initializeHolder(View view, List<CardComponent.TextComponent> textComponentList, List<CardComponent.MediaComponent> mediaComponentList,
                                     ArrayList<CardComponent.ActionComponent> actionComponents, boolean isSent, boolean showShare)
        {

            viewHashMap = new HashMap<String, View>();
            time = (TextView) view.findViewById(R.id.time);
            status = (ImageView) view.findViewById(R.id.status);
            timeStatus = (View) view.findViewById(R.id.time_status);
            selectedStateOverlay = view.findViewById(R.id.selected_state_overlay);
            messageContainer = (ViewGroup) view.findViewById(R.id.message_container);
            dayStub = (ViewStub) view.findViewById(R.id.day_stub);
            messageInfoStub = (ViewStub) view.findViewById(R.id.message_info_stub);

            for (CardComponent.TextComponent textComponent : textComponentList)
            {
                String tag = textComponent.getTag();
                if (!TextUtils.isEmpty(tag))
                    viewHashMap.put(tag, view.findViewWithTag(tag));
            }

            for (CardComponent.MediaComponent mediaComponent : mediaComponentList)
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

    public static class HikeDailyViewHolder extends ViewHolder
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
    }
    public static class JFLViewHolder extends ViewHolder
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
    }
    public static ViewHolder getViewHolder(int type){
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
