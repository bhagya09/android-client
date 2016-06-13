package com.bsb.hike.platform.nativecards;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.smartImageLoader.HikeDailyCardImageLoader;

/**
 * Created by varunarora on 11/06/16.
 */
public class HikeDailyViewHolder extends ViewHolder
{
    private HikeDailyCardImageLoader hikeDailyCardImageLoader;

    public HikeDailyViewHolder(Context context) {
        super(context);
    }

    public void initializeHolder(View view, ConvMessage convMessage)
    {
        super.initializeHolder(view, convMessage);

    }

    public void clearViewHolder(View view)
    {
        TextView t1Text = (TextView) view.findViewWithTag("T1");
        t1Text.setTextColor(ContextCompat.getColor(mContext, R.color.white));
        t1Text.setVisibility(View.GONE);
        t1Text.setTextSize(20);
        TextView t2Text = (TextView) view.findViewWithTag("T2");
        t2Text.setTextColor(ContextCompat.getColor(mContext, R.color.white));
        t2Text.setVisibility(View.GONE);
        t2Text.setTextSize(12);

    }

    @Override
    public void processViewHolder(final View view)
    {
        //Using hardcoded color code for transparent color in case the foregroundcolor is not present.
        String foregroundColor = convMessage.platformMessageMetadata.cards.get(0).backgroundColor != null ? convMessage.platformMessageMetadata.cards.get(0).backgroundColor
                : "#ffffffff";
        Drawable backgroundDrawable = null;
        ImageView background_image = (ImageView) view.findViewById(R.id.bg_image);
        if (!TextUtils.isEmpty(foregroundColor))
        {
            ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor(foregroundColor));
            backgroundDrawable = new LayerDrawable(new Drawable[] { colorDrawable });
            background_image.setImageDrawable(backgroundDrawable);
        }
        if (convMessage.platformMessageMetadata.cards.get(0).background != null)
        {
            hikeDailyCardImageLoader = new HikeDailyCardImageLoader(convMessage.platformMessageMetadata.layoutId,convMessage.platformMessageMetadata.contentId, convMessage.getMsisdn());
            hikeDailyCardImageLoader.setImageFadeIn(false);
            hikeDailyCardImageLoader.setDontSetBackground(true);
            hikeDailyCardImageLoader.setDefaultDrawableNull(false);
            hikeDailyCardImageLoader.setForeGroundColor(convMessage.platformMessageMetadata.cards.get(0).backgroundColor);
            hikeDailyCardImageLoader.loadImage(convMessage.platformMessageMetadata.cards.get(0).background, background_image);
        }
    }

}
