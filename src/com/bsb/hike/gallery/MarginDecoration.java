package com.bsb.hike.gallery;

import com.bsb.hike.R;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class MarginDecoration extends RecyclerView.ItemDecoration {
	private int margin;

	public MarginDecoration(Context context) {
		margin = context.getResources().getDimensionPixelSize(
				R.dimen.gallery_grid_spacing);
	}

	@Override
	public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
			RecyclerView.State state) {
		outRect.set(margin, margin, margin, margin);
	}
}
