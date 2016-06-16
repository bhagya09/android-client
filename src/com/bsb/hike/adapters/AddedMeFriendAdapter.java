package com.bsb.hike.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.utils.Utils;

import java.util.List;

/**
 * Created by gauravmittal on 15/06/16.
 */
public class AddedMeFriendAdapter extends BaseAdapter {

    private List<ContactInfo> displayList;

    private IconLoader iconLoader;

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

    public AddedMeFriendAdapter(List<ContactInfo> list, Context context) {
        this.displayList = list;
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
    public ContactInfo getItem(int position) {
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
            viewHolder.addFriend.setOnClickListener(onAddButtonClickListener);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        ContactInfo info = this.displayList.get(position);
        String displayName = info.getNameOrMsisdn();
        viewHolder.name.setText(displayName);
        viewHolder.number.setText(info.getMsisdn());
        viewHolder.avatar.setTag(info.getMsisdn());
        iconLoader.loadImage(info.getMsisdn(), viewHolder.avatar, false, false, true, info);
        if (info.isMyFriend()) {
            viewHolder.addedFriend.setVisibility(View.VISIBLE);
            viewHolder.addFriend.setVisibility(View.GONE);
        } else {
            viewHolder.addedFriend.setVisibility(View.GONE);
            viewHolder.addFriend.setVisibility(View.VISIBLE);
            viewHolder.addFriend.setTag(position);
        }

        return convertView;
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