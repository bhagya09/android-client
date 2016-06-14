package com.bsb.hike.adapters;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.HikeFeatureInfo;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.PinnedSectionListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gauravmittal on 19/05/16.
 */
public class FriendRequestAdapter extends BaseAdapter implements PinnedSectionListView.PinnedSectionListAdapter {

    private List<ContactInfo> completeList;

    private List<ContactInfo> displayList;

    private IconLoader iconLoader;

    private String searchText;

    private ContactFilter contactFilter;

    private Context context;

    @Override
    public boolean isItemViewTypePinned(int viewType) {
        return viewType == ViewType.PINNED_SECTION.ordinal();
    }

    private static class ViewHolder {
        // each data item is just a string in this case
        public TextView name;

        public TextView number;

        ImageView avatar;

        TextView addFriend;

        ImageView addedFriend;
    }

    public FriendRequestAdapter(List<ContactInfo> list, Context context) {
        this.completeList = list;
        this.displayList = completeList;
        iconLoader = new IconLoader(context, HikeMessengerApp.getInstance().getApplicationContext().getResources().getDimensionPixelSize(R.dimen.icon_picture_size));
        iconLoader.setImageFadeIn(false);
        iconLoader.setDefaultAvatarIfNoCustomIcon(false);
        iconLoader.setDefaultDrawableNull(false);
        this.context = context;
    }

    public enum ViewType
    {
        PINNED_SECTION, FRIEND, NOT_FRIEND_HIKE, NOT_FRIEND_SMS, BASIC_ITEM
    }

    @Override
    public int getViewTypeCount() {
        return ViewType.values().length;
    }

    @Override
    public int getCount() {
        return displayList.size();
    }

    @Override
    public ContactInfo getItem(int position) {
        return displayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        ContactInfo info = getItem(position);
        if (info.getId() == ViewType.PINNED_SECTION.toString()){
            return ViewType.PINNED_SECTION.ordinal();
        }
        else if (info.getId() == ViewType.BASIC_ITEM.toString()) {
            return ViewType.BASIC_ITEM.ordinal();
        }
        else if (info.isMyOneWayFriend()) {
            return ViewType.FRIEND.ordinal();
        }
        else {
            if (info.isOnhike())
                return ViewType.NOT_FRIEND_HIKE.ordinal();
            else
                return ViewType.NOT_FRIEND_SMS.ordinal();
        }
    }

    private View inflateView(int position, View convertView, ViewGroup parent, ViewType viewType) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            switch (viewType) {
                case FRIEND:
                case NOT_FRIEND_HIKE:
                case NOT_FRIEND_SMS:
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_request_item, parent, false);
                    viewHolder = new ViewHolder();
                    viewHolder.name = (TextView) convertView.findViewById(R.id.name);
                    viewHolder.number = (TextView) convertView.findViewById(R.id.number);
                    viewHolder.avatar = (ImageView) convertView.findViewById(R.id.avatar);
                    viewHolder.addFriend = (TextView) convertView.findViewById(R.id.add);
                    viewHolder.addedFriend = (ImageView) convertView.findViewById(R.id.added);
                    if (viewHolder.addFriend != null)
                        viewHolder.addFriend.setOnClickListener(onAddButtonClickListener);
                    break;
                case BASIC_ITEM:
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.basic_icon_list_item, parent, false);
                    viewHolder = new ViewHolder();
                    viewHolder.name = (TextView) convertView.findViewById(R.id.text);
                    viewHolder.avatar = (ImageView) convertView.findViewById(R.id.icon);
                    break;
                case PINNED_SECTION:
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.friends_group_view, parent, false);
                    viewHolder = new ViewHolder();
                    viewHolder.name = (TextView)convertView.findViewById(R.id.name);
                    convertView.findViewById(R.id.count).setVisibility(View.INVISIBLE);
                    break;
                default:
                    break;
            }
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        return convertView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewType viewType = ViewType.values()[getItemViewType(position)];
        ContactInfo info = this.displayList.get(position);
        convertView = inflateView(position, convertView, parent, viewType);
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();

        if (viewType == ViewType.FRIEND || viewType == ViewType.NOT_FRIEND_HIKE) {
            String displayName = info.getNameOrMsisdn();
            viewHolder.name.setText(displayName);
            viewHolder.number.setText(info.getMsisdn());
            viewHolder.avatar.setTag(info.getMsisdn());
            iconLoader.loadImage(info.getMsisdn(), viewHolder.avatar, false, false, true, info);
            if (info.isMyOneWayFriend()) {
                viewHolder.addedFriend.setVisibility(View.VISIBLE);
                viewHolder.addFriend.setVisibility(View.GONE);
            } else {
                viewHolder.addedFriend.setVisibility(View.GONE);
                viewHolder.addFriend.setVisibility(View.VISIBLE);
                viewHolder.addFriend.setTag(position);
            }

            if (!TextUtils.isEmpty(searchText)) {
                int start = displayName.toLowerCase().indexOf(searchText);
                int end = start + searchText.length();
                if (start >= 0 && end <= displayName.length()) {
                    SpannableString spanName = new SpannableString(displayName);
                    spanName.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.blue_color_span)), start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    viewHolder.name.setText(spanName, TextView.BufferType.SPANNABLE);
                }
            }
        }
        else if (viewType == ViewType.BASIC_ITEM) {
            HikeFeatureInfo hikeFeatureInfo = (HikeFeatureInfo)info ;
            viewHolder.name.setText(hikeFeatureInfo.getName());
            viewHolder.avatar.setImageResource(hikeFeatureInfo.getIconDrawable());
        }
        else if (viewType == ViewType.PINNED_SECTION) {
            viewHolder.name.setText(info.getName());
        }
        return convertView;
    }

    public void setupSearchMode() {
        searchText = "";
        contactFilter = new ContactFilter();
    }

    public void endSearchMode() {
        searchText = null;
        contactFilter = null;
        displayList = completeList;
    }

    public void onSearchQueryChanged(String s, Filter.FilterListener filterListener) {
        if (s == null)
            s = "";

        if (contactFilter != null)
            contactFilter.filter(s, filterListener);
    }

    private class ContactFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            String query = constraint.toString();
            List<ContactInfo> resultList = new ArrayList<>();
            if (TextUtils.isEmpty(query)) {
                resultList = completeList;
            } else if (query.length() > searchText.length()) {
                filterList(displayList, resultList, query);
            } else if (query.length() < searchText.length()){
                filterList(completeList, resultList, query);
            }
            else
            {
                resultList = displayList;
            }
            results.values = resultList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            displayList = (List<ContactInfo>) results.values;
            searchText = constraint.toString();
            notifyDataSetChanged();
        }


        private void filterList(List<ContactInfo> allList, List<ContactInfo> listToUpdate, String textToBeFiltered) {
            for (ContactInfo info : allList) {
                try {
                    if (filterContactForSearch(info, textToBeFiltered)) {
                        listToUpdate.add(info);
                    }
                } catch (Exception ex) {
                    Logger.d(getClass().getSimpleName(), "Exception while filtering conversation contacts." + ex);
                }
            }

        }

        public boolean filterContactForSearch(ContactInfo contactInfo, String textToBeFiltered) {
            if (!TextUtils.isEmpty(contactInfo.getName())
                    && (contactInfo.getName().toLowerCase().contains(textToBeFiltered) || contactInfo.getName().toLowerCase().contains(" " + textToBeFiltered))) {
                return true;
            } else if (contactInfo.getMsisdn().contains(textToBeFiltered)) {
                return true;
            }
            return false;
        }
    }

    View.OnClickListener onAddButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ContactInfo contact = displayList.get((Integer) v.getTag());
            contact.setFavoriteType(Utils.toggleFavorite(context, contact, false, HikeConstants.AddFriendSources.ADDED_ME_SCREEN));
            notifyDataSetChanged();
        }
    };

}
