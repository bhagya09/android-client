package com.bsb.hike.chatHead;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class StickyCaller
{
	
private static LinearLayout stickyCallerView;
	
	static WindowManager windowManager;
	
	private static short moveType;
	
	private static final short NONE = 0;
	
	private static final short HORIZONTAL = 1;
	
	private static final short VERTICAL = 2;
	
	
	
	static LayoutParams callerParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, LayoutParams.TYPE_PHONE, LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_LAYOUT_NO_LIMITS ,
			PixelFormat.TRANSLUCENT);
	private static void actionMove(Context context, int initialX, int initialY, Float initialTouchX, Float initialTouchY, MotionEvent event)
	{
		if (ChatHeadLayout.getOverlayView() == null || !ChatHeadLayout.getOverlayView().isShown())
		{
			if ((moveType == NONE || moveType == HORIZONTAL) && Math.abs(event.getRawX() - initialTouchX) > Math.abs(event.getRawY() - initialTouchY))
			{
				callerParams.x = initialX + (int) (event.getRawX() - initialTouchX);
				callerParams.alpha = (float) (1.0-(Math.abs(callerParams.x)/Utils.getDeviceWidth()));
				moveType = HORIZONTAL;
			}
			else
			{
				callerParams.y = initialY + (int) (event.getRawY() - initialTouchY);
				if (callerParams.y < 0)
				{
					callerParams.y = 0;
				}
				if (callerParams.y > Utils.getDeviceHeight()- stickyCallerView.getHeight())
				{
					callerParams.y = Utils.getDeviceHeight() - stickyCallerView.getHeight();
				}
				moveType = VERTICAL;
			}
			try
			{
				windowManager.updateViewLayout(stickyCallerView, callerParams);
			}
			catch (Exception e)
			{
				Logger.d("Sticky Caller", "action move chat head");
			}
		}
		return;
	}

	
	
	public static void removeCallerView()
	{
		Context context = HikeMessengerApp.getInstance();
		WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		try
		{
			windowManager.removeView(stickyCallerView);
			stickyCallerView = null;
		}
		catch(Exception e)
		{
			Logger.d("Sticky Caller","Removing Caller View");
		}
	}


	
	
	public static void showCallerView(String number, String result)
	{
		final Context context = HikeMessengerApp.getInstance();
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		JsonParser parser = new JsonParser();
		JsonObject callerDetails = (JsonObject) parser.parse(result);
		CallerContentModel callerContentModel = new Gson().fromJson(callerDetails, CallerContentModel.class);
		stickyCallerView = (LinearLayout) inflater.inflate(R.layout.caller_layout, null);
		stickyCallerView.findViewById(R.id.caller_detail_view).setVisibility(View.VISIBLE);
		((TextView) (stickyCallerView.findViewById(R.id.caller_number))).setText(number);
        ((TextView) (stickyCallerView.findViewById(R.id.caller_name))).setText(callerContentModel.getFirsttName()+" "+callerContentModel.getLastName());
		((TextView) (stickyCallerView.findViewById(R.id.caller_location))).setText(callerContentModel.getLocation());
		windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		callerParams.gravity = Gravity.TOP | Gravity.LEFT;
		callerParams.x =0;
		callerParams.y=0;
		callerParams.alpha=(float) 1.0;
		windowManager.addView(stickyCallerView, callerParams);
		stickyCallerView.setOnTouchListener(new OnSwipeTouchListener(context)
		{	
			public void onSwipeRight()
			{
				stickyCallerView.clearAnimation();
				slideAnimation(callerParams.x, (Utils.getDeviceWidth()));

			}

			public void slideAnimation(int from, int to)
			{
				ValueAnimator translateX = ValueAnimator.ofInt(from, to);
				translateX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
				{
					@Override
					public void onAnimationUpdate(ValueAnimator valueAnimator)
					{
						callerParams.x = (Integer) valueAnimator.getAnimatedValue();
						callerParams.alpha = (float) (1.0-(Math.abs(callerParams.x)/Utils.getDeviceWidth()));
						windowManager.updateViewLayout(stickyCallerView, callerParams);
					}
				});
				translateX.setDuration(500);
				translateX.start();
			}

			public void onSwipeLeft()
			{
				stickyCallerView.clearAnimation();
				slideAnimation(callerParams.x, -(Utils.getDeviceWidth()));

			}

			int initialX, initialY;

			float initialTouchX, initialTouchY;

			public boolean onTouch(View v, MotionEvent event)
			{
				switch (event.getAction())
				{
				case MotionEvent.ACTION_DOWN:
					initialX = callerParams.x;
					initialY = callerParams.y;
					initialTouchX = event.getRawX();
					initialTouchY = event.getRawY();
					moveType = NONE;
				     break;
				case MotionEvent.ACTION_UP:
					if (callerParams.x < -(Utils.getDeviceWidth() / 2))
					{
						slideAnimation(callerParams.x,-(Utils.getDeviceWidth()));
					}
					else if (callerParams.x > (Utils.getDeviceWidth() / 2))
					{
						slideAnimation(callerParams.x,(Utils.getDeviceWidth()));
					}
					else
					{
						slideAnimation(callerParams.x, 0);
					}
					 break;
				case MotionEvent.ACTION_MOVE:
					actionMove(context, initialX, initialY, initialTouchX, initialTouchY, event);
					break;
				}
				return gestureDetector.onTouchEvent(event);
			}
		});

	}
}
