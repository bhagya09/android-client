package com.bsb.hike.adapters;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.utils.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gauravmittal on 19/05/16.
 */
public class FriendRequestAdapter extends BaseAdapter {

    private List<ContactInfo> completeList;

    private List<ContactInfo> displayList;

    private IconLoader iconLoader;

    private String searchText;

    private ContactFilter contactFilter;

    private Context context;

    private static class ViewHolder {
        // each data item is just a string in this case
        public TextView name;

        public TextView number;

        ImageView avatar;

        View addFriend;

        View addedFriend;

        public ViewHolder(View v) {
            name = (TextView) v.findViewById(R.id.name);
            number = (TextView) v.findViewById(R.id.number);
            avatar = (ImageView) v.findViewById(R.id.avatar);
            addFriend = (TextView) v.findViewById(R.id.add);
            addedFriend = (ImageView) v.findViewById(R.id.added);
        }
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

    @Override
    public int getCount() {
        return displayList.size();
    }

    @Override
    public Object getItem(int position) {
        return displayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_request_item, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        ContactInfo info = this.displayList.get(position);
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

        public boolean filterContactForSearch(ContactInfo convInfo, String textToBeFiltered) {
            if (!TextUtils.isEmpty(convInfo.getName())
                    && (convInfo.getName().toLowerCase().contains(textToBeFiltered) || convInfo.getName().toLowerCase().contains(" " + textToBeFiltered))) {
                return true;
            } else if (convInfo.getMsisdn().contains(textToBeFiltered)) {
                return true;
            }
            return false;
        }
    }

    public AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ContactInfo info = displayList.get(position);

        }
    };


}
