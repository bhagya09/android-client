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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.timeline.model.StoryItem;
import com.bsb.hike.view.PinnedSectionListView;

import java.util.List;

/**
 * Populates "friends/stories" tab content
 * <p/>
 * Created by AtulM on 24/05/16.
 */
public class StoryListAdapter extends BaseAdapter implements PinnedSectionListView.PinnedSectionListAdapter {

    private final List<StoryItem> mStoryItemList;

    private final Context mContext;

    private LayoutInflater mInflater;

    private class ViewHolder {
        public TextView titleView;

        public TextView subTextView;

        public ImageView avatarView;
    }

    public StoryListAdapter(List<StoryItem> argStoryItemList) {
        mStoryItemList = argStoryItemList;
        mContext = HikeMessengerApp.getInstance().getApplicationContext();
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
        return getItem(position).getType() == StoryItem.TYPE_HEADER ? StoryItem.TYPE_HEADER : StoryItem.TYPE_DEFAULT;
    }

    @Override
    public View getView(int index, View view, ViewGroup viewGroup) {
        StoryItem storyItem = getItem(index);

        ViewHolder viewHolder = null;

        if (view == null) {

            if (storyItem.getType() == StoryItem.TYPE_HEADER) {

            } else if (storyItem.getType() == StoryItem.TYPE_INTENT || storyItem.getType() == StoryItem.TYPE_FRIEND || storyItem.getType() == StoryItem.TYPE_BRAND) {
                view = mInflater.inflate(R.layout.list_item_story, null, false);
                viewHolder = new ViewHolder();
                viewHolder.avatarView = (ImageView) view.findViewById(R.id.avatar);
                viewHolder.titleView = (TextView) view.findViewById(R.id.title);
                viewHolder.subTextView = (TextView) view.findViewById(R.id.subtext);
                view.setTag(viewHolder);
            }
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        if (storyItem.getType() == StoryItem.TYPE_HEADER) {

        } else if (storyItem.getType() == StoryItem.TYPE_INTENT) {
            Drawable timelineLogoDrawable = ContextCompat.getDrawable(mContext, R.drawable.ic_dp_timeline);
            Drawable otherFeaturesDrawable = ContextCompat.getDrawable(mContext, R.drawable.other_features_bg);
            viewHolder.avatarView.setImageDrawable(timelineLogoDrawable);
            viewHolder.avatarView.setBackground(otherFeaturesDrawable);
            viewHolder.titleView.setText(storyItem.getTitle());

            String subText = storyItem.getSubText();
            if (!TextUtils.isEmpty(subText)) {
                viewHolder.subTextView.setText(subText);
                viewHolder.subTextView.setVisibility(View.VISIBLE);
            }
        } else if (storyItem.getType() == StoryItem.TYPE_FRIEND) {

        } else if (storyItem.getType() == StoryItem.TYPE_BRAND) {
            // TODO
        }

        if (index == 0) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) viewHolder.avatarView.getLayoutParams();
            params.topMargin = HikePhotosUtils.dpToPx(22);
            params.bottomMargin = HikePhotosUtils.dpToPx(20);
            viewHolder.avatarView.setLayoutParams(params);
        }

        return view;

    }

    @Override
    public boolean isItemViewTypePinned(int viewType) {
        return viewType == StoryItem.TYPE_HEADER ? true : false;
    }
}
