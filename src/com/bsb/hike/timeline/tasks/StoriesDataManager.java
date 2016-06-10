package com.bsb.hike.timeline.tasks;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.timeline.TimelineUtils;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StoryItem;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by atul on 01/06/16.
 */
public class StoriesDataManager {

    public static interface StoriesDataListener {
        public void onDataUpdated(List<StoryItem> storyItemList);
    }

    private volatile static StoriesDataManager instance;

    private static final String TAG = StoriesDataManager.class.getSimpleName();

    private List<StoryItem<StatusMessage, ContactInfo>> defaultList;

    private List<StoryItem<StatusMessage, ContactInfo>> recentsList;

    private List<StoryItem<StatusMessage, ContactInfo>> allPhotosList;

    private List<StoryItem<StatusMessage, ContactInfo>> cameraShyList;

    private final Context mContext;

    private StoriesDataManager() {
        mContext = HikeMessengerApp.getInstance().getApplicationContext();
    }

    public static StoriesDataManager getInstance() {
        if (instance == null) {
            synchronized (StoriesDataManager.class) {
                if (instance == null) {
                    instance = new StoriesDataManager();
                }
            }
        }
        return instance;
    }


    public void getAllStoryData(@NonNull StoriesDataListener argListener) {

        final WeakReference<StoriesDataListener> listenerRef = new WeakReference<StoriesDataListener>(argListener);

        HikeHandlerUtil.getInstance().postAtFront(new Runnable() {
            @Override
            public void run() {
                updateDefaultData(listenerRef);
                updateRecentStories(listenerRef);
                updateAllPhotosStories(listenerRef);
                updateCameraShyStories(listenerRef);
            }
        });
    }

    private void notifyDataUpdate(WeakReference<StoriesDataListener> argListenerRef) {
        if (argListenerRef == null) {
            return;
        }

        Object weakObjectListener = argListenerRef.get();
        if (weakObjectListener == null) {
            return;
        }

        StoriesDataListener storiesDataListener = (StoriesDataListener) weakObjectListener;

        storiesDataListener.onDataUpdated(getCombinedList());
    }

    private List getCombinedList() {
        List<StoryItem> storyItemList = new ArrayList<>();

        if (!Utils.isEmpty(defaultList)) {
            storyItemList.addAll(defaultList);
        }

        if (!Utils.isEmpty(recentsList)) {
            storyItemList.addAll(recentsList);
        }

        if (!Utils.isEmpty(allPhotosList)) {
            storyItemList.addAll(allPhotosList);
        }

        if (!Utils.isEmpty(cameraShyList)) {
            storyItemList.addAll(cameraShyList);
        }

        return storyItemList;
    }

    public void updateDefaultData(WeakReference<StoriesDataListener> argListenerRef) {
        defaultList = new ArrayList<>();

        //Space header
        StoryItem spaceHeader = new StoryItem(StoryItem.TYPE_HEADER, null);
        defaultList.add(spaceHeader);

        // Add Timeline as first option
        StoryItem timelineItem = new StoryItem(StoryItem.TYPE_INTENT, mContext.getString(R.string.timeline));
        timelineItem.setIntent(IntentFactory.getTimelineIntent(mContext));
        timelineItem.setSubText(TimelineUtils.getTimelineSubText());
        defaultList.add(timelineItem);

        notifyDataUpdate(argListenerRef);
    }

    public void updateRecentStories(WeakReference<StoriesDataListener> argListenerRef) {
        // Get recents
        recentsList = HikeConversationsDatabase.getInstance().getAllStories(StoryItem.CATEGORY_RECENT);
        if (!Utils.isEmpty(recentsList)) {
            // Make a header
            StoryItem recentsHeader = new StoryItem(StoryItem.TYPE_HEADER, mContext.getString(R.string.story_category_recent));
            recentsHeader.setSubText(String.valueOf(recentsList.size()));
            recentsList.add(0, recentsHeader);

            notifyDataUpdate(argListenerRef);
        }
    }

    public void updateAllPhotosStories(WeakReference<StoriesDataListener> argListenerRef) {
        // Get all photos
        allPhotosList = HikeConversationsDatabase.getInstance().getAllStories(StoryItem.CATEGORY_ALL);
        removeSimilarElements(recentsList, allPhotosList);
        if (!Utils.isEmpty(allPhotosList)) {
            // Make a header
            StoryItem allPhotosHeader = new StoryItem(StoryItem.TYPE_HEADER, mContext.getString(R.string.story_category_allphotos));
            allPhotosHeader.setSubText(String.valueOf(allPhotosList.size()));
            allPhotosList.add(0, allPhotosHeader);

            notifyDataUpdate(argListenerRef);
        }
    }

    public void updateCameraShyStories(WeakReference<StoriesDataListener> argListenerRef) {
        // Get camera shy
        cameraShyList = HikeConversationsDatabase.getInstance().getAllStories(StoryItem.CATEGORY_DEFAULT);
        removeSimilarElements(recentsList, cameraShyList);
        removeSimilarElements(allPhotosList, cameraShyList);
        if (!Utils.isEmpty(cameraShyList)) {
            Collections.sort(cameraShyList, cameraShyFriendsComparator);

            // Make a header
            StoryItem defaultHeader = new StoryItem(StoryItem.TYPE_HEADER, mContext.getString(R.string.story_category_default));
            defaultHeader.setSubText(String.valueOf(cameraShyList.size()));
            cameraShyList.add(0, defaultHeader);

            notifyDataUpdate(argListenerRef);
        }
    }

    private void removeSimilarElements(List referenceList, List workingList) {
        for (int i = workingList.size() - 1; i >= 0; i--) {
            if (referenceList.contains(workingList.get(i))) {
                workingList.remove(i);
            }
        }
    }

    Comparator cameraShyFriendsComparator = new Comparator<StoryItem<StatusMessage, ContactInfo>>() {
        @Override
        public int compare(StoryItem<StatusMessage, ContactInfo> i1, StoryItem<StatusMessage, ContactInfo> i2) {
            ContactInfo cInfo1 = i1.getTypeInfo();
            ContactInfo cInfo2 = i2.getTypeInfo();

            if (TextUtils.isEmpty(cInfo1.getName()) && TextUtils.isEmpty(cInfo2.getName())) {
                return 0;
            }

            if (TextUtils.isEmpty(cInfo1.getName()) && !TextUtils.isEmpty(cInfo2.getName())) {
                return 1;
            }

            if (TextUtils.isEmpty(cInfo2.getName()) && !TextUtils.isEmpty(cInfo1.getName())) {
                return -1;
            }

            return cInfo1.getName().compareTo(cInfo2.getName());
        }
    };

    public void getStoryForFriend(final String friendMsisdn, final WeakReference<StoriesDataListener> argListenerRef) {
        if (argListenerRef == null) {
            return;
        }

        HikeHandlerUtil.getInstance().postAtFront(new Runnable() {
            @Override
            public void run() {

                Object weakRefListener = argListenerRef.get();
                if (weakRefListener == null) {
                    return;
                }

                StoriesDataListener storyListener = (StoriesDataListener) weakRefListener;

                updateRecentStories(null);
                updateAllPhotosStories(null);

                StoryItem friendStory = null;

                for (StoryItem<StatusMessage, ContactInfo> recentStory : recentsList) {
                    ContactInfo cInfo = recentStory.getTypeInfo();
                    if (cInfo != null && friendMsisdn.equals(cInfo.getMsisdn())) {
                        friendStory = recentStory;
                    }
                }

                if (friendStory == null) {
                    for (StoryItem<StatusMessage, ContactInfo> allPhotoStory : allPhotosList) {
                        ContactInfo cInfo = allPhotoStory.getTypeInfo();
                        if (cInfo != null && friendMsisdn.equals(cInfo.getMsisdn())) {
                            friendStory = allPhotoStory;
                        }
                    }
                }

                List<StoryItem> storyItemList = new ArrayList<StoryItem>();
                if (friendStory != null) {
                    storyItemList.add(friendStory);
                }

                storyListener.onDataUpdated(storyItemList);
            }
        });
    }
}
