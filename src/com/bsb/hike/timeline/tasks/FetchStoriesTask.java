package com.bsb.hike.timeline.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StoryItem;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by atul on 28/05/16.
 */
public abstract class FetchStoriesTask extends AsyncTask<Void, List, Void> {

    private static final String TAG = FetchStoriesTask.class.getSimpleName();

    private List<StoryItem> storyItemList;

    private List<StoryItem<StatusMessage, ContactInfo>> recentsList;

    private List<StoryItem<StatusMessage, ContactInfo>> allPhotosList;

    private List<StoryItem<StatusMessage, ContactInfo>> cameraShyList;

    private final Context mContext;

    public FetchStoriesTask() {
        mContext = HikeMessengerApp.getInstance().getApplicationContext();
        storyItemList = new ArrayList<StoryItem>();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... voids) {

        //Space header
        StoryItem spaceHeader = new StoryItem(StoryItem.TYPE_HEADER, null);
        storyItemList.add(spaceHeader);

        // Add Timeline as first option
        StoryItem timelineItem = new StoryItem(StoryItem.TYPE_INTENT, mContext.getString(R.string.timeline));
        timelineItem.setIntent(IntentFactory.getTimelineIntent(mContext));
        SharedPreferences sharedPref = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0); // To support old code
        int newUpdates = Utils.getNotificationCount(sharedPref, true, false, true, false); // no. of updates
        int newLikes = Utils.getNotificationCount(sharedPref, false, true, false, false); // no. of loves
        if (newUpdates > 0) {
            timelineItem.setSubText(mContext.getString(R.string.timeline_sub_new_updt));
        } else if (newLikes > 0) {
            timelineItem.setSubText(String.format(mContext.getString(R.string.timeline_sub_likes), newLikes));
        } else {
            timelineItem.setSubText(mContext.getString(R.string.timeline_sub_no_updt));
        }

        storyItemList.add(timelineItem);

        // Notify
        onProgressUpdate(storyItemList);

        // Get recents
        recentsList = HikeConversationsDatabase.getInstance().getStories(StoryItem.CATEGORY_RECENT);
        Collections.reverse(recentsList);
        if (!Utils.isEmpty(recentsList)) {
            // Make a header
            StoryItem recentsHeader = new StoryItem(StoryItem.TYPE_HEADER, mContext.getString(R.string.story_category_recent));
            recentsHeader.setSubText(String.valueOf(recentsList.size()));
            storyItemList.add(recentsHeader);
            storyItemList.addAll(recentsList);
            // Notify
            onProgressUpdate(storyItemList);
        }

        // Get all photos
        allPhotosList = HikeConversationsDatabase.getInstance().getStories(StoryItem.CATEGORY_ALL);
        Collections.reverse(allPhotosList);
        if (!Utils.isEmpty(allPhotosList)) {
            // Make a header
            StoryItem allPhotosHeader = new StoryItem(StoryItem.TYPE_HEADER, mContext.getString(R.string.story_category_allphotos));
            allPhotosHeader.setSubText(String.valueOf(allPhotosList.size()));
            storyItemList.add(allPhotosHeader);
            storyItemList.addAll(allPhotosList);
            // Notify
            onProgressUpdate(storyItemList);
        }

        // Get camera shy
        cameraShyList = HikeConversationsDatabase.getInstance().getStories(StoryItem.CATEGORY_DEFAULT);
        Collections.sort(cameraShyList, cameraShyFriendsComparator);

        if (!Utils.isEmpty(cameraShyList)) {
            // Make a header
            StoryItem defaultHeader = new StoryItem(StoryItem.TYPE_HEADER, mContext.getString(R.string.story_category_default));
            defaultHeader.setSubText(String.valueOf(cameraShyList.size()));
            storyItemList.add(defaultHeader);
            storyItemList.addAll(cameraShyList);
            // Notify
            onProgressUpdate(storyItemList);
        }

        return null;
    }

    Comparator cameraShyFriendsComparator = new Comparator<StoryItem<StatusMessage, ContactInfo>>() {
        @Override
        public int compare(StoryItem<StatusMessage, ContactInfo> i1, StoryItem<StatusMessage, ContactInfo> i2) {
            ContactInfo cInfo1 = i1.getTypeInfo();
            ContactInfo cInfo2 = i2.getTypeInfo();

            if(TextUtils.isEmpty(cInfo1.getName()) && TextUtils.isEmpty(cInfo2.getName()))
            {
                return 0;
            }

            if(TextUtils.isEmpty(cInfo1.getName()) && !TextUtils.isEmpty(cInfo2.getName()))
            {
                return 1;
            }

            if(TextUtils.isEmpty(cInfo2.getName()) && !TextUtils.isEmpty(cInfo1.getName()))
            {
                return -1;
            }

            return cInfo1.getName().compareTo(cInfo2.getName());
        }
    };

    @Override
    protected abstract void onProgressUpdate(List... itemList);
}
