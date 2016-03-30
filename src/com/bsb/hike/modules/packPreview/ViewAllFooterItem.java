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
public class ViewAllFooterItem extends BasePackPreviewAdapterItem implements View.OnClickListener
{

	private Context mContext;

	private View viewAllFooterView;

	private ViewAllClickedListener viewAllClickListener;

	interface ViewAllClickedListener
	{
		void onViewAllClicked();
	}

	public ViewAllFooterItem(Context context)
	{
		this.mContext = context;
		init();
	}

	private void init()
	{
		viewAllFooterView = LayoutInflater.from(mContext).inflate(R.layout.view_all_footer, null);
		View loadMoreFooterContainer = viewAllFooterView.findViewById(R.id.view_all_footer_container);

		RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(72));
		params.leftMargin = Utils.dpToPx(12);
		params.topMargin = Utils.dpToPx(55);
		params.rightMargin = Utils.dpToPx(12);
		loadMoreFooterContainer.setLayoutParams(params);
		loadMoreFooterContainer.setOnClickListener(this);
	}

	@Override
	public View getView()
	{
		return viewAllFooterView;
	}

	@Override
	public RecyclerView.ViewHolder getViewHolder()
	{
		return new ViewAllFooterItemViewHolder(getView());
	}

	private class ViewAllFooterItemViewHolder extends RecyclerView.ViewHolder
	{
		public ViewAllFooterItemViewHolder(View row)
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
		if (viewAllClickListener != null)
		{
			viewAllClickListener.onViewAllClicked();
		}
	}

	public void setOnClickListener(ViewAllClickedListener loadMoreClickListener)
	{
		this.viewAllClickListener = loadMoreClickListener;
	}
}
