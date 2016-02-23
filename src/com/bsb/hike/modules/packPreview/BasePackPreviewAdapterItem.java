package com.bsb.hike.modules.packPreview;

import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by anubhavgupta on 10/02/16.
 */
public abstract class BasePackPreviewAdapterItem
{
    public abstract View getView();

    public abstract RecyclerView.ViewHolder getViewHolder();

    public abstract void releaseResources();
}
