package com.bsb.hike.timeline.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.smartImageLoader.TimelineUpdatesImageLoader;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StoryItem;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.PinnedSectionListView;
import com.bsb.hike.view.RoundedImageView;

import java.util.List;

/**
 * Populates "friends/stories" tab content
 * <p/>
 * Created by AtulM on 24/05/16.
 */
public class StoryListAdapter extends BaseAdapter implements PinnedSectionListView.PinnedSectionListAdapter {

    private final IconLoader mDPImageLoader;

    private final TimelineUpdatesImageLoader mTimelineImageLoader;

    private List<StoryItem> mStoryItemList;

    private final Context mContext;

    private LayoutInflater mInflater;

    private final String TAG = StoryListAdapter.class.getSimpleName();

    private class ViewHolder {
        public ViewGroup parentView;

        public TextView titleView;

        public TextView subTextView;

        public ImageView avatarView;

        public TextView countView;

        public View spaceView;
    }

    public StoryListAdapter(List<StoryItem> argStoryItemList) {
        mStoryItemList = argStoryItemList;
        mContext = HikeMessengerApp.getInstance().getApplicationContext();
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //Setup loaders for profile photos and timeline photo posts
        mTimelineImageLoader = new TimelineUpdatesImageLoader(mContext, mContext.getResources().getDimensionPixelSize(R.dimen.icon_picture_size));
        mTimelineImageLoader.setImageFadeIn(false);

        mDPImageLoader = new IconLoader(mContext, mContext.getResources().getDimensionPixelSize(R.dimen.icon_picture_size));
        mDPImageLoader.setDefaultAvatarIfNoCustomIcon(true);
        mDPImageLoader.setImageFadeIn(false);

    }

    public void setStoryItemList(List<StoryItem> argList) {
        this.mStoryItemList = argList;
    }

    @Override
    public int getCount() {
        return mStoryItemList.size();
    }

    @Override
    public StoryItem getItem(int index) {
        return mStoryItemList.get(index);
    }

    @Override
    public long getItemId(int index) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        // Header and non-header
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getType() == StoryItem.TYPE_HEADER ? 0 : 1;
    }

    @Override
    public View getView(int index, View view, ViewGroup viewGroup) {
        StoryItem storyItem = getItem(index);

        ViewHolder viewHolder = null;

        if (view == null) {
            if (storyItem.getType() == StoryItem.TYPE_HEADER) {
                view = mInflater.inflate(R.layout.story_header_view, null, false);
                viewHolder = new ViewHolder();
                viewHolder.titleView = (TextView) view.findViewById(R.id.name);
                viewHolder.countView = (TextView) view.findViewById(R.id.count);
                viewHolder.parentView = (ViewGroup) view.findViewById(R.id.parent);
                viewHolder.spaceView = view.findViewById(R.id.spaceView);
                view.setTag(viewHolder);
            } else if (storyItem.getType() == StoryItem.TYPE_INTENT || storyItem.getType() == StoryItem.TYPE_FRIEND || storyItem.getType() == StoryItem.TYPE_BRAND) {
                view = mInflater.inflate(R.layout.list_item_story, null, false);
                viewHolder = new ViewHolder();
                viewHolder.avatarView = (ImageView) view.findViewById(R.id.avatar);
                viewHolder.titleView = (TextView) view.findViewById(R.id.title);
                viewHolder.subTextView = (TextView) view.findViewById(R.id.subtext);
                viewHolder.parentView = (ViewGroup) view.findViewById(R.id.parent);
                view.setTag(viewHolder);
            }
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        if (storyItem.getType() == StoryItem.TYPE_HEADER) {
            if (TextUtils.isEmpty(storyItem.getTitle())) {
                viewHolder.titleView.setVisibility(View.GONE);
                viewHolder.countView.setVisibility(View.GONE);
                viewHolder.parentView.setPadding(0,0,0,0);
                viewHolder.spaceView.setVisibility(View.VISIBLE);
            } else {
                viewHolder.titleView.setVisibility(View.VISIBLE);
                viewHolder.countView.setVisibility(View.VISIBLE);
                viewHolder.titleView.setText(storyItem.getTitle());
                viewHolder.countView.setText(storyItem.getSubText());
                viewHolder.parentView.setPadding(Utils.dpToPx(19),Utils.dpToPx(18),Utils.dpToPx(16),Utils.dpToPx(8));
                viewHolder.spaceView.setVisibility(View.GONE);
            }
        } else if (storyItem.getType() == StoryItem.TYPE_INTENT) {
            Drawable timelineLogoDrawable = ContextCompat.getDrawable(mContext, R.drawable.ic_dp_timeline);
            Drawable otherFeaturesDrawable = ContextCompat.getDrawable(mContext, R.drawable.other_features_bg);
            viewHolder.avatarView.setImageDrawable(timelineLogoDrawable);
            viewHolder.avatarView.setBackground(otherFeaturesDrawable);
            viewHolder.titleView.setText(storyItem.getTitle());
            viewHolder.titleView.setAlpha(1f);
            viewHolder.avatarView.setAlpha(1f);

            String subText = storyItem.getSubText();
            if (!TextUtils.isEmpty(subText)) {
                viewHolder.subTextView.setText(subText);
                viewHolder.subTextView.setVisibility(View.VISIBLE);

                if (mContext.getString(R.string.timeline_sub_no_updt).equals(subText)) {
                    // Color gray
                    viewHolder.subTextView.setTextColor(mContext.getResources().getColor(R.color.stories_sub_text_unread));
                } else {
                    // Color blue
                    viewHolder.subTextView.setTextColor(mContext.getResources().getColor(R.color.blue_hike));
                }
            }
        } else if (storyItem.getType() == StoryItem.TYPE_FRIEND) {
            viewHolder.titleView.setText(storyItem.getTitle());
            String subText = storyItem.getSubText();
            if (!TextUtils.isEmpty(subText)) {
                viewHolder.subTextView.setText(subText);
                viewHolder.subTextView.setVisibility(View.VISIBLE);
            }
            List<StatusMessage> statusMessagesList = storyItem.getDataObjects();
            ContactInfo contactInfo = (ContactInfo) storyItem.getTypeInfo();
            if (!Utils.isEmpty(statusMessagesList) || storyItem.getCategory() == StoryItem.CATEGORY_DEFAULT) {
                if (storyItem.getCategory() == StoryItem.CATEGORY_DEFAULT) {
                    //Load profile pic
                    mDPImageLoader.loadImage(contactInfo.getMsisdn(), viewHolder.avatarView, false, false, true, contactInfo);
                } else {
                    //Load last photo post
                    RoundedImageView roundImageView = (RoundedImageView) viewHolder.avatarView;
                    roundImageView.setOval(true);
                    mTimelineImageLoader.loadImage(statusMessagesList.get(0).getMappedId() + "_icon", viewHolder.avatarView, false, false, false, statusMessagesList.get(0));
                }
            } else {
                Logger.wtf(TAG, "Friends story item but no stories attached!!");
            }


            //Setup alpha
            if(storyItem.getCategory() != StoryItem.CATEGORY_RECENT)
            {
                viewHolder.titleView.setAlpha(0.6f);
                viewHolder.avatarView.setAlpha(0.6f);
            }
            else
            {
                viewHolder.titleView.setAlpha(1f);
                viewHolder.avatarView.setAlpha(1f);
            }
            viewHolder.subTextView.setTextColor(mContext.getResources().getColor(R.color.stories_sub_text_unread));

        } else if (storyItem.getType() == StoryItem.TYPE_BRAND) {
            // TODO
        }

        return view;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        StoryItem storyItem = getItem(position);
        if (StoryItem.TYPE_HEADER == storyItem.getType()) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean isItemViewTypePinned(int viewType) {
        return viewType == StoryItem.TYPE_HEADER ? true : false;
    }
}
