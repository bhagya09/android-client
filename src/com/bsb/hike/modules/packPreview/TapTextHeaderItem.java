package com.bsb.hike.modules.packPreview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import com.bsb.hike.R;

/**
 * Created by anubhavgupta on 15/02/16.
 */
public class TapTextHeaderItem extends BasePackPreviewAdapterItem
{

	private Context mContext;

	private View tapTextHeaderView;

	public TapTextHeaderItem(Context context)
	{
		this.mContext = context;
		init();
	}

	private void init()
	{
		tapTextHeaderView = LayoutInflater.from(mContext).inflate(R.layout.tap_text_header, null);
	}

	@Override
	public View getView()
	{
		return tapTextHeaderView;
	}

	@Override
	public RecyclerView.ViewHolder getViewHolder()
	{
		return new TapTextHeaderItemViewHolder(getView());
	}

	private class TapTextHeaderItemViewHolder extends RecyclerView.ViewHolder
	{
		public TapTextHeaderItemViewHolder(View row)
		{
			super(row);
		}
	}

	@Override
	public void releaseResources()
	{

	}
}
