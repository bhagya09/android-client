package com.bsb.hike.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;

public class CallerQuickReplyListAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<String> quickReplyList;
    private LayoutInflater mInflater ;

    public CallerQuickReplyListAdapter(Context mContext, ArrayList<String> quickReplyList)
    {
        this.mContext = mContext;
        this.quickReplyList = quickReplyList;
        this.mInflater = LayoutInflater.from(mContext);
    }


    @Override
    public int getCount()
    {
        if (!Utils.isEmpty(quickReplyList))
        {
            return quickReplyList.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int position)
    {
        if (!Utils.isEmpty(quickReplyList))
        {
            return quickReplyList.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    class ViewHolder
    {
        TextView quickReplyItem;

        public ViewHolder(TextView quickReplyItem)
        {
            super();
            this.quickReplyItem = quickReplyItem;
        }

        public TextView getQuickReplyItem()
        {
            return this.quickReplyItem;
        }

    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent)
    {
        ViewHolder listHolder;
        TextView quickReplyItem;
        if (convertView == null)
        {
            convertView = mInflater.inflate(R.layout.caller_quick_reply_list_item, parent, false);
            quickReplyItem = (TextView) convertView.findViewById(R.id.quick_reply_item);
            listHolder = new ViewHolder(quickReplyItem);
            convertView.setTag(listHolder);
        }
        else
        {
            listHolder = (ViewHolder) convertView.getTag();
            quickReplyItem = listHolder.getQuickReplyItem();
        }

        quickReplyItem.setText(quickReplyList.get(position));

        return convertView;
    }
}
