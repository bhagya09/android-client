package com.bsb.hike.timeline.view;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ListView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HomeAnalyticsConstants;
import com.bsb.hike.media.ImageParser;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.contactmgr.HikeUserDatabase;
import com.bsb.hike.timeline.TimelineUtils;
import com.bsb.hike.timeline.adapter.StoryListAdapter;
import com.bsb.hike.timeline.model.StoryItem;
import com.bsb.hike.timeline.tasks.StoriesDataManager;
import com.bsb.hike.timeline.tasks.UpdateActionsDataRunnable;
import com.bsb.hike.ui.CustomTabsBar;
import com.bsb.hike.ui.GalleryActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * This fragment serves as content for one of home screen tabs. Is the entry point for old timeline. Contains list of friends based on their timeline-related activities.
 * <p/>
 * Created by AtulM on 24/05/16.
 */
public class StoryFragment extends Fragment implements View.OnClickListener, HikePubSub.Listener, StoriesDataManager.StoriesDataListener, AdapterView.OnItemClickListener {
    private View fragmentView;

    private ListView listViewStories;

    private List<StoryItem> storyItemList = new ArrayList<StoryItem>();

    private StoryListAdapter storyAdapter;

    private String mGenus;

    private View emptyStateView;

    private View btnAddFriends;

    private Animation shakeAnim;

    private final String TAG = StoryFragment.class.getSimpleName();

    private final String[] pubsubEvents = new String[]{
            HikePubSub.UNSEEN_STATUS_COUNT_CHANGED,
            HikePubSub.TIMELINE_UPDATE_RECIEVED,
            HikePubSub.ICON_CHANGED,
            HikePubSub.ACTIVITY_UPDATE,
            HikePubSub.STATUS_MARKED_READ,
            HikePubSub.STEALTH_MODE_TOGGLED,
            HikePubSub.DELETE_STATUS,
            HikePubSub.FAVORITE_TOGGLED,
            HikePubSub.STEALTH_CONVERSATION_MARKED,
            HikePubSub.STEALTH_CONVERSATION_UNMARKED,
            HikePubSub.FAVORITE_TOGGLED,
            HikePubSub.USER_PRIVACY_TOGGLED};

    private CustomTabsBar.CustomTabBadgeCounterListener badgeCounterListener;

    public static StoryFragment newInstance(@Nullable Bundle argBundle) {
        StoryFragment fragmentInstance = new StoryFragment();
        if (argBundle != null) {
            fragmentInstance.setArguments(argBundle);
        }
        return fragmentInstance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Show action buttons
        setHasOptionsMenu(true);

        // Inflate layout
        fragmentView = inflater.inflate(R.layout.fragment_stories, container, false);

        // Get view references
        listViewStories = (ListView) fragmentView.findViewById(R.id.list_view_story);
        emptyStateView = fragmentView.findViewById(R.id.empty_view);
        btnAddFriends = fragmentView.findViewById(R.id.btn_add_friends);

        return fragmentView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindFragmentViews();
        shakeAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);
    }

    private void bindFragmentViews()
    {
        // Check if user has any friends
        if (HikeUserDatabase.getInstance().isTwoWayFriendsPresent()) {
            bindStoryFragmentList();
            HikeMessengerApp.getInstance().getPubSub().addListeners(this, pubsubEvents); // listen only in non-empty state
        } else {
            //Show empty state
            bindEmptyStateView();
        }
    }

    private void bindStoryFragmentList() {
        listViewStories.setVisibility(View.VISIBLE);
        emptyStateView.setVisibility(View.GONE);
        btnAddFriends.setOnClickListener(null);

        // Setup adapters
        storyAdapter = new StoryListAdapter(storyItemList);

        listViewStories.setAdapter(storyAdapter);

        listViewStories.setOnItemClickListener(this);

        StoriesDataManager.getInstance().getAllStoryData(this);

        // Get actions for SU from DB
        HikeHandlerUtil.getInstance().postRunnable(new UpdateActionsDataRunnable(null));
    }

    private void bindEmptyStateView() {
        listViewStories.setVisibility(View.GONE);
        emptyStateView.setVisibility(View.VISIBLE);
        btnAddFriends.setOnClickListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.story_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_post:
                int galleryFlags = GalleryActivity.GALLERY_CATEGORIZE_BY_FOLDERS | GalleryActivity.GALLERY_CROP_IMAGE | GalleryActivity.GALLERY_COMPRESS_EDITED_IMAGE
                        | GalleryActivity.GALLERY_DISPLAY_CAMERA_ITEM;

                Intent galleryPickerIntent = IntentFactory.getHikeGalleryPickerIntent(getActivity(), galleryFlags, Utils.getNewImagePostFilePath());
                startActivityForResult(galleryPickerIntent, UpdatesFragment.TIMELINE_POST_IMAGE_REQ);
                logTapCameraFromFriendsTab();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }

        final String genus = data.getStringExtra(HikeConstants.Extras.GENUS);
        if (!TextUtils.isEmpty(genus)) {
            mGenus = genus;
        }

        switch (requestCode) {
            case UpdatesFragment.TIMELINE_POST_IMAGE_REQ:
                ImageParser.parseResult(getActivity(), resultCode, data, new ImageParser.ImageParserListener() {
                    @Override
                    public void imageParsed(String imagePath) {
                        // Open Status update activity
                        Intent newSUIntent = IntentFactory.getPostStatusUpdateIntent(getActivity(), null, imagePath, false);
                        Utils.setSpecies(HomeAnalyticsConstants.SU_SPECIES_FRIENDS_TAB, newSUIntent);
                        if(!TextUtils.isEmpty(genus))
                        {
                            Utils.setGenus(genus, newSUIntent);
                        }
                        startActivity(newSUIntent);
                    }

                    @Override
                    public void imageParsed(Uri uri) {
                        // Do nothing
                    }

                    @Override
                    public void imageParseFailed() {
                        // Do nothing
                    }
                }, false);
                break;

            default:
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_add_friends:
                getActivity().startActivity(IntentFactory.getFriendReqActivityAddFriendsIntent(getActivity()));
                break;
        }
    }

    @Override
    public void onEventReceived(String type, Object object) {
        if (type.equals(HikePubSub.UNSEEN_STATUS_COUNT_CHANGED)
                || type.equals(HikePubSub.ACTIVITY_UPDATE)) {
            if (isAdded() && getActivity() != null) {
                HikeHandlerUtil.getInstance().postRunnableWithDelay(new Runnable() {
                    @Override
                    public void run() {

                        if (!getUserVisibleHint()) {
                            showTimeLineUpdatesIndicator();
                        }

                        StoriesDataManager.getInstance().updateDefaultData(new WeakReference<StoriesDataManager.StoriesDataListener>(StoryFragment.this));
                    }
                }, 2000); // This is to avoid changing of subtext right when timeline is tapped since it takes time for timeline activity to show up
            }
        } else if (type.equals(HikePubSub.TIMELINE_UPDATE_RECIEVED)
                || type.equals(HikePubSub.STATUS_MARKED_READ)
                || type.equals(HikePubSub.STEALTH_MODE_TOGGLED)
                || type.equals(HikePubSub.DELETE_STATUS)
                || type.equals(HikePubSub.STEALTH_CONVERSATION_MARKED)
                || type.equals(HikePubSub.STEALTH_CONVERSATION_UNMARKED)
                || type.equals(HikePubSub.USER_PRIVACY_TOGGLED)) {
            if (isAdded() && getActivity() != null) {
                HikeHandlerUtil.getInstance().postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        StoriesDataManager.getInstance().getAllStoryData(StoryFragment.this);
                    }
                });
            }
        } else if (type.equals(HikePubSub.ICON_CHANGED)) {
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        storyAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
        else if (type.equals(HikePubSub.FAVORITE_TOGGLED)) {
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (emptyStateView.getVisibility() == View.VISIBLE) {
                            bindFragmentViews();
                        } else {
                            StoriesDataManager.getInstance().updateCameraShyStories(new WeakReference<StoriesDataManager.StoriesDataListener>(StoryFragment.this));
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        HikeMessengerApp.getInstance().getPubSub().removeListeners(this, pubsubEvents);
    }

    @Override
    public void onDataUpdated(final List<StoryItem> argList) {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!Utils.isEmpty(argList)) {
                        storyItemList = argList;
                        storyAdapter.setStoryItemList(storyItemList);
                        storyAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        StoryItem storyItem = storyItemList.get(position);
        if (storyItem.getType() == StoryItem.TYPE_INTENT) {
            getActivity().startActivity(storyItem.getIntent());
            HikeHandlerUtil.getInstance().postRunnable(new Runnable() {
                @Override
                public void run() {
                    logTimelineOpenAnalyticEvent();
                }
            });
        } else if (storyItem.getType() == StoryItem.TYPE_FRIEND && storyItem.getTypeInfo() != null) {
            if (storyItem.getCategory() == StoryItem.CATEGORY_DEFAULT) {
                view.startAnimation(shakeAnim);
            } else {
                getActivity().startActivity(IntentFactory.getStoryPhotosActivityIntent(getActivity(), ((ContactInfo) storyItem.getTypeInfo()).getMsisdn()));
            }
            logTapFriendAnalyticEvent(storyItem);
        } else if (storyItem.getType() == StoryItem.TYPE_BRAND) {
            // TODO
        }
    }

    private void logTapFriendAnalyticEvent(StoryItem item) {
        try {
            String friendType = null;
            switch (item.getCategory()) {
                case StoryItem.CATEGORY_RECENT:
                    friendType = "recent";
                    break;
                case StoryItem.CATEGORY_ALL:
                    friendType = "all";
                    break;
                case StoryItem.CATEGORY_DEFAULT:
                    friendType = "shy";
                    break;
            }

            JSONObject json = new JSONObject();
            json.put(AnalyticsConstants.V2.UNIQUE_KEY, HomeAnalyticsConstants.UK_HS_FRIENDS);
            json.put(AnalyticsConstants.V2.KINGDOM, HomeAnalyticsConstants.HOMESCREEN_KINGDOM);
            json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
            json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CLICK_EVENT);
            json.put(AnalyticsConstants.V2.ORDER, HomeAnalyticsConstants.UK_HS_FRIENDS);
            json.put(AnalyticsConstants.V2.FAMILY, "view_friend");
            json.put(AnalyticsConstants.V2.SPECIES, friendType);
            HAManager.getInstance().recordV2(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void logTimelineOpenAnalyticEvent() {
        try {

            JSONObject json = new JSONObject();
            json.put(AnalyticsConstants.V2.UNIQUE_KEY, HomeAnalyticsConstants.UK_TL_OPEN);
            json.put(AnalyticsConstants.V2.KINGDOM, HomeAnalyticsConstants.KINGDOM_ACT_LOG2);
            json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
            json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CLICK_EVENT);
            json.put(AnalyticsConstants.V2.ORDER, HomeAnalyticsConstants.UK_TL_OPEN);
            json.put(AnalyticsConstants.V2.FAMILY, Long.toString(System.currentTimeMillis()));
            json.put(AnalyticsConstants.V2.GENUS, "tap");
            json.put(AnalyticsConstants.V2.SPECIES, TimelineUtils.getTimelineSubText().equals(getString(R.string.timeline_sub_no_updt)) ? "no_new_notif" : "new_notif");
            HAManager.getInstance().recordV2(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void logTapCameraFromFriendsTab() {
        try {

            JSONObject json = new JSONObject();
            json.put(AnalyticsConstants.V2.UNIQUE_KEY, HomeAnalyticsConstants.UK_HS_FRIENDS);
            json.put(AnalyticsConstants.V2.KINGDOM, HomeAnalyticsConstants.HOMESCREEN_KINGDOM);
            json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
            json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CLICK_EVENT);
            json.put(AnalyticsConstants.V2.ORDER, HomeAnalyticsConstants.UK_HS_FRIENDS);
            json.put(AnalyticsConstants.V2.FAMILY, "cam");
            HAManager.getInstance().recordV2(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setCustomTabBadgeCounterListener(CustomTabsBar.CustomTabBadgeCounterListener listener)
    {
        this.badgeCounterListener = listener;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        showTimeLineUpdatesIndicator();
        super.onActivityCreated(savedInstanceState);
    }

    private void showTimeLineUpdatesIndicator() {
        if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.FRIENDS_TAB_NOTIF_DOT, false)) {
            if (badgeCounterListener != null) {
                badgeCounterListener.onBadgeCounterUpdated(1);
            }
        }

        else {
            hideTimeLineUpdatesIndicator();
        }
    }

    private void hideTimeLineUpdatesIndicator() {
        if (badgeCounterListener != null) {
            badgeCounterListener.onBadgeCounterUpdated(0);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (isVisibleToUser) {
            HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.FRIENDS_TAB_NOTIF_DOT, false);
            hideTimeLineUpdatesIndicator();
        }
        super.setUserVisibleHint(isVisibleToUser);
    }
}
