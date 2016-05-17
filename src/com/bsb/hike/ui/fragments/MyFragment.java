package com.bsb.hike.ui.fragments;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ImageViewerInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.ServicesActivity;
import com.bsb.hike.ui.SettingsActivity;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

/**
 * Created by gauravmittal on 14/04/16.
 */
public class MyFragment extends Fragment {

    private ImageView profileImgView;

    private ImageView statusMood;

    private TextView nameView;

    private TextView statusView;

    private ContactInfo contactInfo;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View parent = inflater.inflate(R.layout.my_fragment, null);
        return parent;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileImgView = (ImageView) view.findViewById(R.id.profile_image);
        statusMood = (ImageView) view.findViewById(R.id.status_mood);
        nameView = (TextView) view.findViewById(R.id.name);
        statusView = (TextView) view.findViewById(R.id.subtext);

        contactInfo = Utils.getUserContactInfo(getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, getActivity().MODE_PRIVATE));

        nameView.setText(contactInfo.getName());

        // set profile picture
        Drawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(contactInfo.getMsisdn());
        if (drawable == null) {
            drawable = HikeBitmapFactory.getDefaultTextAvatar(contactInfo.getMsisdn());
        }
        profileImgView.setImageDrawable(drawable);

        ImageViewerInfo imageViewerInfo = new ImageViewerInfo(contactInfo.getMsisdn() + ProfileActivity.PROFILE_PIC_SUFFIX, null, false, !ContactManager.getInstance().hasIcon(
                contactInfo.getMsisdn()));
        profileImgView.setTag(imageViewerInfo);

        profileImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onViewImageClicked(v);
            }
        });

        // get hike status
        StatusMessage.StatusMessageType[] statusMessagesTypesToFetch = {StatusMessage.StatusMessageType.TEXT};
        StatusMessage status = HikeConversationsDatabase.getInstance().getLastStatusMessage(statusMessagesTypesToFetch, contactInfo);

        if (status != null) {
            if (status.hasMood()) {
                statusMood.setVisibility(View.VISIBLE);
                statusMood.setImageResource(EmoticonConstants.moodMapping.get(status.getMoodId()));
            } else {
                statusMood.setVisibility(View.GONE);
            }
            statusView.setText(SmileyParser.getInstance().addSmileySpans(status.getText(), true));
        } else {
            status = new StatusMessage(HikeConstants.JOINED_HIKE_STATUS_ID, null, contactInfo.getMsisdn(), contactInfo.getName(), getString(R.string.joined_hike_update),
                    StatusMessage.StatusMessageType.JOINED_HIKE, contactInfo.getHikeJoinTime());

            if (status.getTimeStamp() == 0) {
                statusView.setText(status.getText());
            } else {
                statusView.setText(status.getText() + " " + status.getTimestampFormatted(true, getActivity()));
            }
        }

        setupAddFriendBadgeIcon(view.findViewById(R.id.ic_add_friends));
        setupAddedMeBadgeIcon(view.findViewById(R.id.ic_added_me));
        setupServicesBadgeIcon(view.findViewById(R.id.ic_services));

    }

    private void setupAddFriendBadgeIcon(View parentView)
    {
        ((ImageView) parentView.findViewById(R.id.img_icon)).setImageResource(R.drawable.ic_add_friends);
        ((TextView) parentView.findViewById(R.id.txt_title)).setText(R.string.ADD_FRIENDS);
        parentView.setOnClickListener(badgeIconClickListener);

    }

    private void setupAddedMeBadgeIcon(View parentView)
    {
        ((ImageView) parentView.findViewById(R.id.img_icon)).setImageResource(R.drawable.ic_added_me);
        ((TextView) parentView.findViewById(R.id.txt_title)).setText(R.string.ADDED_ME);
        parentView.setOnClickListener(badgeIconClickListener);
    }

    private void setupServicesBadgeIcon(View parentView)
    {
        ((ImageView) parentView.findViewById(R.id.img_icon)).setImageResource(R.drawable.ic_services);
        ((TextView) parentView.findViewById(R.id.txt_title)).setText(R.string.SERVICES);
        parentView.setOnClickListener(badgeIconClickListener);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.my_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent intent = new Intent(getContext(), SettingsActivity.class);
                getActivity().startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onViewImageClicked(View v) {
        Intent intent = new Intent();
        intent.setClass(getActivity(), ProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private View.OnClickListener badgeIconClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId())
            {
                case R.id.ic_add_friends:
                    break;
                case R.id.ic_added_me:
                    break;
                case R.id.ic_services:
                    Intent i1 = new Intent(getContext(), ServicesActivity.class);
                    getActivity().startActivity(i1);
                    break;
            }
        }
    };
}