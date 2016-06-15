package com.bsb.hike.platform.nativecards;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;

/**
 * Created by varunarora on 11/06/16.
 */
public class JFLViewHolder extends ViewHolder
{
    public JFLViewHolder(Context context) {
        super(context);
    }

    public void initializeHolder(View view, ConvMessage convMessage)
    {
        super.initializeHolder(view, convMessage);
    }

    public void clearViewHolder(View view)
    {
    }

    @Override
    public void processViewHolder(View view)
    {
        if (actionContainer != null)
        {
            ViewGroup.LayoutParams actionContainerParams = actionContainer.getLayoutParams();
            actionContainerParams.width = view.findViewById(R.id.file_thumb).getLayoutParams().width;
            actionContainer.setLayoutParams(actionContainerParams);
        }
    }
}
