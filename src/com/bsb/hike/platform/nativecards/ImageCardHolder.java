package com.bsb.hike.platform.nativecards;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;

/**
 * Created by varunarora on 11/06/16.
 */
public class ImageCardHolder extends ViewHolder
{

    public ImageCardHolder(Context context) {
        super(context);
    }

    public void initializeHolder(View view, ConvMessage convMessage)
    {
        super.initializeHolder(view, convMessage);
    }

    public void clearViewHolder(View view)
    {
        TextView t1Text = (TextView) view.findViewWithTag("T1");
        t1Text.setTextColor(ContextCompat.getColor(mContext, R.color.black));
        t1Text.setVisibility(View.GONE);
        t1Text.setTextSize(14);
        TextView t2Text = (TextView) view.findViewWithTag("T2");
        t2Text.setVisibility(View.GONE);
        t1Text.setTextColor(ContextCompat.getColor(mContext, R.color.black));
        t2Text.setTextSize(11);
        ImageView i1Image = (ImageView) view.findViewWithTag("i1");
        i1Image.setVisibility(View.GONE);
        ImageView placeholderImage = (ImageView) view.findViewById(R.id.placeholder);
        if (placeholderImage != null)
        {
            placeholderImage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void processViewHolder(final View view)
    {
        if(viewHashMap.get("T1") == null && viewHashMap.get("T2")== null){
            view.findViewById(R.id.text_container).setVisibility(View.GONE);
        }
        if(viewHashMap.get("i1") == null){
            view.findViewById(R.id.image_container).setVisibility(View.GONE);
        }
    }
}
