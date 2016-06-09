package com.bsb.hike.timeline.view;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.text.util.Linkify;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.smartImageLoader.StoryPhotoLoader;
import com.bsb.hike.timeline.LoveCheckBoxToggleListener;
import com.bsb.hike.timeline.TimelineActionsManager;
import com.bsb.hike.timeline.adapter.ActivityFeedCursorAdapter;
import com.bsb.hike.timeline.model.ActionsDataModel;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StoryItem;
import com.bsb.hike.timeline.tasks.StatusReadDBRunnable;
import com.bsb.hike.timeline.tasks.StoriesDataManager;
import com.bsb.hike.timeline.tasks.UpdateActionsDataRunnable;
import com.bsb.hike.ui.utils.CrossfadePageTransformer;
import com.bsb.hike.ui.utils.StatusBarColorChanger;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows stories of a friend from last "unread" photos
 * <p/>
 * Created by atul on 05/06/16.
 */
public class StoryPhotosActivity extends HikeAppStateBaseFragmentActivity implements StoriesDataManager.StoriesDataListener, HikePubSub.Listener {

    public static final String STORY_MSISDN_INTENT_KEY = "storyMsisdn";

    private final String TAG = StoryPhotosActivity.class.getSimpleName();

    private String mFriendMsisdn;

    private StoryPhotoViewPager pagerView;

    private View viewBlackBGScreen;

    private View viewTranslucentFGScreen;

    private View infoContainer;

    private TextView textViewCaption;

    private TextView textViewCounts;

    private CheckBox checkBoxLove;

    private View imageInfoDivider;

    private PagerAdapter pagerAdapter;

    private StoryItem<StatusMessage, ContactInfo> storyItem;

    private String[] pubSubListeners = {HikePubSub.ACTIVITY_UPDATE, HikePubSub.ACTIONS_DATA_UPDATE};

    private GestureDetector gestureDetector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getWindow().setBackgroundDrawable(new ColorDrawable(0x00000000));
        super.onCreate(savedInstanceState);

        //Set content view
        setContentView(R.layout.story_photos);

        //Change title bar color
        StatusBarColorChanger.setStatusBarColor(getWindow(), Color.BLACK);

        //Read intent data
        mFriendMsisdn = getIntent().getStringExtra(STORY_MSISDN_INTENT_KEY);
        if (TextUtils.isEmpty(mFriendMsisdn)) {
            Logger.wtf(TAG, "No friend msisdn in intent");
            finish();
            return;
        }

        //Get view references
        initReferences();

        //Setup bindings
        pagerAdapter = new StoryViewPagerAdapter();
        textViewCounts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLikesContactsDialog();
            }
        });

        gestureDetector = new GestureDetector(getApplicationContext(), new GestureListener());
        pagerView.setGestureDetector(gestureDetector);
        pagerView.setScrollDurationFactor(3);
        pagerView.setPageTransformer(false, new CrossfadePageTransformer());

        //Get data
        StoriesDataManager.getInstance().getStoryForFriend(mFriendMsisdn, new WeakReference<StoriesDataManager.StoriesDataListener>(this));

        HikeMessengerApp.getInstance().getPubSub().addListeners(this, pubSubListeners);
    }

    private class StoryViewPagerAdapter extends PagerAdapter {

        private final LayoutInflater inflater;
        private final StoryPhotoLoader storyPhotoLoader;

        public StoryViewPagerAdapter() {
            inflater = LayoutInflater.from(StoryPhotosActivity.this);
            storyPhotoLoader = new StoryPhotoLoader();
        }

        @Override
        public int getCount() {
            if (storyItem == null) {
                return 0;
            } else {
                return storyItem.getDataObjects().size();
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            StatusMessage statusMessage = storyItem.getDataObjects().get(position);
            View view = inflater.inflate(R.layout.story_photo_item, container, false);
            ImageView storyPhoto = (ImageView) view.findViewById(R.id.image_view_story);
            storyPhotoLoader.loadImage(statusMessage.getMappedId(), storyPhoto, false, false, false, statusMessage);
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }
    }

    private void initReferences() {
        pagerView = (StoryPhotoViewPager) findViewById(R.id.story_photo_pager); // pager which shows story photos
        viewBlackBGScreen = findViewById(R.id.bg_screen); // activity background
        viewTranslucentFGScreen = findViewById(R.id.fg_screen); // layer on photo on which captions/loves are displayed
        infoContainer = findViewById(R.id.image_info_container); // contains loves, captions
        textViewCaption = (TextView) findViewById(R.id.text_view_caption); // caption text view
        textViewCaption.setMovementMethod(new ScrollingMovementMethod());
        textViewCounts = (TextView) findViewById(R.id.text_view_count); // love count text view
        checkBoxLove = (CheckBox) findViewById(R.id.btn_love); // love checkbox
        imageInfoDivider = findViewById(R.id.imageInfoDivider); // divider in between caption and love counts
    }

    @Override
    public void onDataUpdated(List<StoryItem> storyItemList) {
        if (!Utils.isEmpty(storyItemList)) {
            storyItem = storyItemList.get(0);
            pagerView.setAdapter(pagerAdapter);
            pagerView.addOnPageChangeListener(pageChangeListener);
            checkBoxLove.setTag(getCurrentStatusMessage());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateActionsRelatedViews();
                }
            });

            //Mark first post as read (rest handled onPageSelected for PagerAdapter)
            markAsRead(storyItem, 0);

            //Fetch latest loves from server
            List<StatusMessage> statusMessageList = storyItem.getDataObjects();
            if (!Utils.isEmpty(statusMessageList)) {
                HikeHandlerUtil.getInstance().postRunnableWithDelay(new UpdateActionsDataRunnable(statusMessageList),1000);
                // The delay is purely for improving UX, since on fast phones (Nexus) the runnable completes execution before photo pager is displayed because of which the transition of thumbnail in friends tab is visible (looks glitchy)
            }
        }
    }

    private void markAsRead(StoryItem<StatusMessage, ContactInfo> storyItem, int position) {
        if (!Utils.isEmpty(storyItem.getDataObjects())) {
            List<String> readSuIDList = new ArrayList<String>();
            readSuIDList.add(storyItem.getDataObjects().get(position).getMappedId());
            HikeHandlerUtil.getInstance().postRunnable(new StatusReadDBRunnable(readSuIDList));
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Logger.d(TAG, "onSingleTapConfirmed pager");
            if (pagerView.getCurrentItem() < (pagerAdapter.getCount() - 1)) {
                pagerView.setCurrentItem(pagerView.getCurrentItem() + 1, true);
            } else {
                StoryPhotosActivity.this.finish();
            }
            return true;
        }
    }

    private StatusMessage getCurrentStatusMessage() {
        return storyItem.getDataObjects().get(pagerView.getCurrentItem());
    }

    private ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            Logger.d(TAG, "onPageScrolled " + position + " " + positionOffset + " " + positionOffsetPixels);
        }

        @Override
        public void onPageSelected(int position) {
            Logger.d(TAG, "onPageSelected " + position);
            checkBoxLove.setTag(getCurrentStatusMessage());
            updateActionsRelatedViews();
            markAsRead(storyItem, position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            Logger.d(TAG, "onPageScrollStateChanged " + state);
        }
    };

    private CompoundButton.OnCheckedChangeListener onLoveToggleListener = new LoveCheckBoxToggleListener();

    private void updateActionsRelatedViews() {
        StatusMessage currentStatusMessage = getCurrentStatusMessage();
        ActionsDataModel actionsData = TimelineActionsManager.getInstance().getActionsData()
                .getActions(currentStatusMessage.getMappedId(), ActionsDataModel.ActionTypes.LIKE, ActionsDataModel.ActivityObjectTypes.STATUS_UPDATE);

        if (actionsData == null) {
            // Try to get actions data from database
            ArrayList<String> suIDs = new ArrayList<String>();
            suIDs.add(currentStatusMessage.getMappedId());

            HikeConversationsDatabase.getInstance().getActionsData(ActionsDataModel.ActivityObjectTypes.STATUS_UPDATE.getTypeString(), suIDs,
                    TimelineActionsManager.getInstance().getActionsData());
            actionsData = TimelineActionsManager.getInstance().getActionsData().getActions(currentStatusMessage.getMappedId(), ActionsDataModel.ActionTypes.LIKE, ActionsDataModel.ActivityObjectTypes.STATUS_UPDATE);
        }

        ArrayList<String> msisdns = new ArrayList<String>();
        boolean isLikedByMe = false;
        if (actionsData != null) {
            msisdns = actionsData.getAllMsisdn();
            isLikedByMe = actionsData.isLikedBySelf();
        }

        checkBoxLove.setOnCheckedChangeListener(null);

        checkBoxLove.setChecked(isLikedByMe);

        if (!Utils.isEmpty(msisdns)) {
            // Set count
            if (msisdns.size() == 1) {
                textViewCounts.setText(String.format(getString(R.string.num_like), msisdns.size()));
            } else {
                textViewCounts.setText(String.format(getString(R.string.num_likes), msisdns.size()));
            }
        } else {
            textViewCounts.setText(R.string.like_this);
        }

        checkBoxLove.setOnCheckedChangeListener(onLoveToggleListener);

        viewTranslucentFGScreen.setAlpha(0.25f);
        viewTranslucentFGScreen.setVisibility(View.VISIBLE);
        viewTranslucentFGScreen.animate().setDuration(1000).alpha(0.8f);

        if (currentStatusMessage.getStatusMessageType() == StatusMessage.StatusMessageType.IMAGE) {
            textViewCaption.setVisibility(View.GONE);
        } else {
            SmileyParser smileyParser = SmileyParser.getInstance();
            textViewCaption.setText(smileyParser.addSmileySpans(currentStatusMessage.getText().trim(), true));
            Linkify.addLinks(textViewCaption, Linkify.ALL);
        }
    }

    public void showLikesContactsDialog() {
        StatusMessage currentStatusMessage = getCurrentStatusMessage();
        ArrayList<String> msisdns = new ArrayList<String>();
        ActionsDataModel actionsData = TimelineActionsManager.getInstance().getActionsData()
                .getActions(currentStatusMessage.getMappedId(), ActionsDataModel.ActionTypes.LIKE, ActionsDataModel.ActivityObjectTypes.STATUS_UPDATE);

        if (actionsData != null) {
            msisdns = actionsData.getAllMsisdn();
        }

        if (!Utils.isEmpty(msisdns)) {
            HikeDialog contactsListDialog = HikeDialogFactory.showDialog(StoryPhotosActivity.this, HikeDialogFactory.LIKE_CONTACTS_DIALOG, currentStatusMessage.getMsisdn(), null, msisdns);

            contactsListDialog.show();
            JSONObject metadataSU = new JSONObject();
            try {
                metadataSU.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.TIMELINE_SUMMARY_LIKES_DIALOG_OPEN);
                metadataSU.put(AnalyticsConstants.UPDATE_TYPE, "" + ActivityFeedCursorAdapter.getPostType(currentStatusMessage));
                metadataSU.put(AnalyticsConstants.TIMELINE_U_ID, currentStatusMessage.getMsisdn());
                HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, HAManager.EventPriority.HIGH, metadataSU);
            } catch (JSONException e) {
                Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
            }
        }
    }

    @Override
    public void onEventReceived(String type, Object object) {
        if (HikePubSub.ACTIVITY_UPDATE.equals(type) || HikePubSub.ACTIONS_DATA_UPDATE.equals(type)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateActionsRelatedViews();

                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HikeMessengerApp.getInstance().getPubSub().removeListeners(this, pubSubListeners);
    }
}
