package com.bsb.hike.modules.packPreview;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by anubhavgupta on 09/02/16.
 */
public class PackPreviewRecyclerView extends RecyclerView
{

	public interface TouchListener
	{
		void onTouch(MotionEvent event);
	}

	private TouchListener listener;

	public PackPreviewRecyclerView(Context context)
	{
		this(context, null);
	}

	public PackPreviewRecyclerView(Context context, @Nullable AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	public PackPreviewRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	@Override
	protected boolean onSetAlpha(int alpha)
	{
		for (int i = 0; i < getChildCount(); i++)
		{
			View view = getChildAt(i);
			if (view instanceof ImageView)
			{
				view.setAlpha(getAlpha());
			}
		}
		return true;
	}

	public void setTouchListener(TouchListener listener)
	{
		this.listener = listener;
	}


	@Override
	public boolean onTouchEvent(MotionEvent e)
	{
		if(listener != null)
		{
			listener.onTouch(e);
		}
		return super.onTouchEvent(e);
	}
}
