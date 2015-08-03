package com.bsb.hike.modules.stickersearch.ui;

import android.content.Context;
import android.graphics.PointF;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.State;
import android.util.DisplayMetrics;

public class CustomLinearLayoutManager extends LinearLayoutManager
{

	private Context context;
	private int scrollSpeed;
	
	public CustomLinearLayoutManager(Context context, int scrollSpeed)
	{
		super(context);
		this.context = context;
		this.scrollSpeed = scrollSpeed;
	}
	
	public CustomLinearLayoutManager(Context context, int orientation, boolean reverseLayout, int scrollSpeed)
	{
		super(context, orientation, reverseLayout);
		this.context = context;
		this.scrollSpeed = scrollSpeed;
	}
	
	@Override
	public void smoothScrollToPosition(RecyclerView recyclerView, State state, int targetPosition)
	{
		// TODO Auto-generated method stub
		super.smoothScrollToPosition(recyclerView, state, targetPosition);
		
		LinearSmoothScroller smoothScroller = new LinearSmoothScroller(context)
		{
			
			@Override
			public PointF computeScrollVectorForPosition(int arg0)
			{
				return new PointF(1, 0);
			}
			
			@Override
			protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics)
			{
				return scrollSpeed / displayMetrics.densityDpi;
			}
		};
		
		smoothScroller.setTargetPosition(targetPosition);
		startSmoothScroll(smoothScroller);
	}

}
