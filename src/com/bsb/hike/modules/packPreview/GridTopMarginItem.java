package com.bsb.hike.modules.packPreview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import com.bsb.hike.R;

/**
 * Created by anubhavgupta on 15/02/16.
 */
public class GridTopMarginItem extends BasePackPreviewAdapterItem
{

	private Context mContext;

	private View gridTopMarginHeaderView;

	private View headerContainer;

	public GridTopMarginItem(Context context)
	{
		this.mContext = context;
		init();
	}

	private void init()
	{
		gridTopMarginHeaderView = LayoutInflater.from(mContext).inflate(R.layout.grid_top_margin_header, null);
		headerContainer = gridTopMarginHeaderView.findViewById(R.id.grid_top_margin_header_container);
	}

	@Override
	public View getView()
	{
		return gridTopMarginHeaderView;
	}

	@Override
	public RecyclerView.ViewHolder getViewHolder()
	{
		return new GridTopMarginItemViewHolder(getView());
	}

	public View getHeaderContainer()
	{
		return headerContainer;
	}

	private class GridTopMarginItemViewHolder extends RecyclerView.ViewHolder
	{
		public GridTopMarginItemViewHolder(View row)
		{
			super(row);
		}
	}

	@Override
	public void releaseResources()
	{

	}
}
