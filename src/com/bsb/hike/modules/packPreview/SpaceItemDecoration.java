package com.bsb.hike.modules.packPreview;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.bsb.hike.utils.Utils;

/**
 * Created by anubhavgupta on 08/02/16.
 */
public class SpaceItemDecoration extends RecyclerView.ItemDecoration
{
    private int horizontalSpacing;

    private int verticalSpacing;

	private int rowSize, totalSize;

	public SpaceItemDecoration(int rowSize, int totalSize)
	{
		this.rowSize = rowSize;
		this.totalSize = totalSize;
        this.horizontalSpacing = Utils.dpToPx(8);
        this.verticalSpacing = Utils.dpToPx(16);
    }

	@Override
	public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state)
	{
		int position = parent.getChildLayoutPosition(view) - 1;
        int rows = (int) Math.ceil(totalSize/(double) rowSize);
        int bottomRowStart = (rows - 1) * rowSize;
        int bottomRowEnd = totalSize - 1;

        outRect.left = horizontalSpacing/2;
        outRect.top = verticalSpacing/2;
        outRect.right = horizontalSpacing/2;
        outRect.bottom = verticalSpacing/2;

        if (position < 0 || position >= totalSize)
        {
            outRect.left = 0;
            outRect.top = 0;
            outRect.right = 0;
            outRect.bottom = 0;
        }
        if(position == 0)
        {
            outRect.left = 0;
            outRect.top = 0;
        }
        if(position > 0 && position < rowSize - 1)
        {
            outRect.top = 0;
        }
        if(position == rowSize -1)
        {
            outRect.top = 0;
            outRect.right = 0;
        }
        if(position == bottomRowStart)
        {
            outRect.left = 0;
            outRect.bottom = 0;
        }
        if(position == bottomRowEnd)
        {
            outRect.right = 0;
            outRect.bottom = 0;
        }
        if(position > bottomRowStart && position < bottomRowEnd)
        {
            outRect.bottom = 0;
        }
        if(position % rowSize == 0)
        {
            outRect.left = 0;
        }
        if(position % rowSize == (rowSize - 1))
        {
            outRect.right = 0;
        }
	}
}
