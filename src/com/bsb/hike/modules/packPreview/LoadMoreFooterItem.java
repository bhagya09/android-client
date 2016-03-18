package com.bsb.hike.modules.packPreview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bsb.hike.R;
import com.bsb.hike.utils.Utils;

/**
 * Created by anubhavgupta on 15/02/16.
 */
public class LoadMoreFooterItem extends BasePackPreviewAdapterItem implements View.OnClickListener
{

	private Context mContext;

	private View loadMoreFooterView;

	private LoadMoreClickedListener loadMoreClickListener;

	interface LoadMoreClickedListener
	{
		void onLoadMoreClicked();
	}

	public LoadMoreFooterItem(Context context)
	{
		this.mContext = context;
		init();
	}

	private void init()
	{
		loadMoreFooterView = LayoutInflater.from(mContext).inflate(R.layout.load_more_footer, null);
		View loadMoreFooterContainer = loadMoreFooterView.findViewById(R.id.load_more_footer_container);

		RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(50));
		params.leftMargin = Utils.dpToPx(16);
		params.topMargin = Utils.dpToPx(10);
		params.rightMargin = Utils.dpToPx(16);
		loadMoreFooterContainer.setLayoutParams(params);
		loadMoreFooterContainer.setOnClickListener(this);
	}

	@Override
	public View getView()
	{
		return loadMoreFooterView;
	}

	@Override
	public RecyclerView.ViewHolder getViewHolder()
	{
		return new LoadMoreFooterItemViewHolder(getView());
	}

	private class LoadMoreFooterItemViewHolder extends RecyclerView.ViewHolder
	{
		public LoadMoreFooterItemViewHolder(View row)
		{
			super(row);
		}
	}

	@Override
	public void releaseResources()
	{

	}

	@Override
	public void onClick(View v)
	{
		if (loadMoreClickListener != null)
		{
			loadMoreClickListener.onLoadMoreClicked();
		}
	}

	public void setOnClickListener(LoadMoreClickedListener loadMoreClickListener)
	{
		this.loadMoreClickListener = loadMoreClickListener;
	}
}
