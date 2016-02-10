package com.bsb.hike.modules.packPreview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

/**
 * Created by anubhavgupta on 09/02/16.
 */
public class PackPreviewRelativeLayout extends RelativeLayout
{

	public interface TouchListener
	{
		void onTouch(MotionEvent event);
	}

	private TouchListener listener;

	public PackPreviewRelativeLayout(Context context)
	{
		this(context, null);
	}

	public PackPreviewRelativeLayout(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	public PackPreviewRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr)
	{
		this(context, attrs, defStyleAttr, 0);
	}

	public PackPreviewRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
	{
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public void setTouchListener(TouchListener listener)
	{
		this.listener = listener;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev)
	{
		if (listener != null)
		{
			listener.onTouch(ev);
		}
		return super.onInterceptTouchEvent(ev);
	}
}
