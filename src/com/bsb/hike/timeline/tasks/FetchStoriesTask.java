package com.bsb.hike.timeline.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StoryItem;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by atul on 28/05/16.
 */
public abstract class FetchStoriesTask extends AsyncTask<Void, List, Void> {

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
        }
        storyItemList.add(timelineItem);

        // Notify
        onProgressUpdate(storyItemList);

        // Get recents
        recentsList = HikeConversationsDatabase.getInstance().getStories(StoryItem.CATEGORY_RECENT);
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

    @Override
    protected abstract void onProgressUpdate(List... itemList);
}
