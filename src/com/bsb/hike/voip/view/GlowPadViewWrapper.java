package com.bsb.hike.voip.view;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import com.bsb.hike.R;
import com.fima.glowpadview.GlowPadView;

public class GlowPadViewWrapper extends GlowPadView implements GlowPadView.OnTriggerListener
{
	private CallActions mCallActions;

//	private String TAG = "VoIPGlowPad";

	private final long PING_REPEAT_DELAY = 1500;

	private boolean pingAutoRepeat = true;

	private boolean targetTriggered;

	private Handler pingHandler = new Handler();

	public GlowPadViewWrapper(Context context) 
	{
		super(context);
	}

	public GlowPadViewWrapper(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() 
	{
		super.onFinishInflate();
		setOnTriggerListener(this);
	}

	public void setCallActionsListener(CallActions mCallActions)
	{
		this.mCallActions = mCallActions;
	}

	public void setAutoRepeat(boolean val)
	{
		pingAutoRepeat = val;
	}

	@Override
	public void onGrabbed(View v, int handle) {
//		Logger.d(TAG,"Call glow pad view - Grabbed");
		stopPing();
	}

	@Override
	public void onReleased(View v, int handle) 
	{
//		Logger.d(TAG,"Call glow pad view - onRelease");
		if(!targetTriggered)
		{
			startPing();
		}
	}

	@Override
	public void onTrigger(View v, int target) 
	{
		int resId = getResourceIdForTarget(target);
		if(resId == R.drawable.ic_item_call_hang)
		{
			mCallActions.onDeclineCall();
			targetTriggered = true;
		}
		else if(resId == R.drawable.ic_item_call_pick)
		{
			mCallActions.onAcceptCall();
			targetTriggered = true;
		}
		else if(resId == R.drawable.ic_item_decline_message)
		{
			mCallActions.onMessage();
			targetTriggered = true;
		}
	}

	@Override
	public void onGrabbedStateChange(View v, int handle) {
//		Logger.d(TAG,"Call glow pad view - Grabbed state changed");
	}

	@Override
	public void onFinishFinalAnimation() {
//		Logger.d(TAG,"Call glow pad view - Finish final anim");
	}

	public void stopPing()
	{
//		Logger.d(TAG, "stopping Ping");
		pingAutoRepeat = false;
		pingHandler.removeCallbacks(pingRunnable);
	}

	public void startPing()
	{
//		Logger.d(TAG, "starting ping, auto repeat:" + pingAutoRepeat);
		ping();
		if (pingAutoRepeat)
		{
			pingHandler.postDelayed(pingRunnable, PING_REPEAT_DELAY);
		}
	}

	Runnable pingRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			startPing();
		}
	};

}
