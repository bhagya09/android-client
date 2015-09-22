package com.bsb.hike.chatHead;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.sax.StartElementListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewDebug.FlagToString;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class StickyCaller
{

	private static LinearLayout stickyCallerView;

	private static WindowManager windowManager;

	private static short moveType;

	private static final short NONE = 0;

	private static final short HORIZONTAL = 1;

	private static final short VERTICAL = 2;

	private static final int ANIMATION_TIME = 400;

	public static final short FAILURE = 0;

	public static final short LOADING = 1;

	public static final short SUCCESS = 2;

	public static final short ALREADY_SAVED = 3;
	
	public static boolean isOnCall = false;
	
	public static String callCurrentNumber = null;

	public static String callCurrentName = null;

	public static final String ACTIVATE_STICKY_CALLER = "activateStickyCaller";

	public static final String SHOW_STICKY_CALLER = "showStickyCaller";

	public static boolean toCall = false;

	static LayoutParams callerParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, LayoutParams.TYPE_PHONE, LayoutParams.FLAG_NOT_FOCUSABLE
			| LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT);

	private static void actionMove(Context context, int initialX, int initialY, Float initialTouchX, Float initialTouchY, MotionEvent event)
	{
		if (ChatHeadLayout.getOverlayView() == null || !ChatHeadLayout.getOverlayView().isShown())
		{

			if ((moveType == NONE || moveType == HORIZONTAL) && (Math.abs(event.getRawX() - initialTouchX) > Math.abs(event.getRawY() - initialTouchY)))
			{
				callerParams.x = initialX + (int) (event.getRawX() - initialTouchX);
				callerParams.alpha = 1.0f - ((float) Math.abs(callerParams.x) / Utils.getDeviceWidth());
				moveType = HORIZONTAL;
			}
			else if (Math.abs(event.getRawX() - initialTouchX) < Math.abs(event.getRawY() - initialTouchY))
			{
				try
				{
					callerParams.y = initialY + (int) (event.getRawY() - initialTouchY);
					if (callerParams.y < 0)
					{
						callerParams.y = 0;
					}
					if (callerParams.y > Utils.getDeviceHeight() - stickyCallerView.getHeight())
					{
						callerParams.y = Utils.getDeviceHeight() - stickyCallerView.getHeight();
					}
					moveType = VERTICAL;
				}
				catch (Exception e)
				{
					Logger.d("Sticky Caller", "stickyCallerView not exists");
				}
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

	public static void slideAnimation(int from, int to)
	{
		ValueAnimator translateX = ValueAnimator.ofInt(from, to);
		translateX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
		{
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator)
			{
				callerParams.x = (Integer) valueAnimator.getAnimatedValue();
				callerParams.alpha = 1.0f - ((float) Math.abs(callerParams.x) / Utils.getDeviceWidth());
				try
				{
					windowManager.updateViewLayout(stickyCallerView, callerParams);
				}
				catch (Exception e)
				{
					Logger.d("StickyCaller", "view not found");
				}
			}
		});
		translateX.setDuration(ANIMATION_TIME);
		translateX.start();
	}

	public static void removeCallerView()
	{
		try
		{
			windowManager.removeView(stickyCallerView);
			stickyCallerView = null;
		}
		catch (Exception e)
		{
			Logger.d("Sticky Caller", "Removing Caller View");
		}
	}

	static OnSwipeTouchListener onSwipeTouchListener = new OnSwipeTouchListener(HikeMessengerApp.getInstance())
	{
		public void onSwipeRight()
		{

			if (moveType != VERTICAL)
			{
				try
				{
					stickyCallerView.clearAnimation();
					slideAnimation(callerParams.x, Utils.getDeviceWidth());
				}
				catch (Exception e)
				{
					Logger.d("Sticky Caller", "stickyCallerView not exists");

				}

			}
		}

		public void onSwipeLeft()
		{
			if (moveType != VERTICAL)
			{
				try
				{
					stickyCallerView.clearAnimation();
					slideAnimation(callerParams.x, -(Utils.getDeviceWidth()));
				}
				catch (Exception e)
				{
					Logger.d("Sticky Caller", "stickyCallerView not exists");
				}

			}
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
				if (callerParams.x < -(3 * Utils.getDeviceWidth() / 4))
				{
					slideAnimation(callerParams.x, -(Utils.getDeviceWidth()));
				}
				else if (callerParams.x > (3 * Utils.getDeviceWidth() / 4))
				{
					slideAnimation(callerParams.x, Utils.getDeviceWidth());
				}
				else
				{
					slideAnimation(callerParams.x, 0);
				}
				break;
			case MotionEvent.ACTION_MOVE:
				actionMove(HikeMessengerApp.getInstance(), initialX, initialY, initialTouchX, initialTouchY, event);
				break;
			}
			return gestureDetector.onTouchEvent(event);
		}
	};

	public static void showCallerView(String number, String result, short type)
	{
		final Context context = HikeMessengerApp.getInstance();
		windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		try
		{
			windowManager.removeView(stickyCallerView);
		}
		catch (Exception e)
		{
			Logger.d("StickyCaller", "error in adding caller view");
		}
		switch (type)
		{
		case ALREADY_SAVED:
			settingLayoutAlreadySavedContact(context, number, result);
			break;

		case LOADING:
			settingLayoutDataLoading(context, number, result);
			break;
		case SUCCESS:
			settingLayoutDataSuccess(context, number, result);
			break;

		case FAILURE:
			settingLayoutDataFailure(context, number, result);
			break;
		}

		setCallerParams();
		try
		{
			windowManager.addView(stickyCallerView, callerParams);
			slideAnimation(callerParams.x, 0);
			stickyCallerView.setOnTouchListener(onSwipeTouchListener);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.d("StickyCaller", "error in adding caller view");
		}
	}

	private static void settingLayoutAlreadySavedContact(Context context, String number, String result)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		stickyCallerView = (LinearLayout) inflater.inflate(R.layout.caller_layout, null);
		setBasicClickListener();
		stickyCallerView.findViewById(R.id.caller_detail_view).setVisibility(View.VISIBLE);
		if (number != null)
		{
			TextView callerNumber = (TextView) (stickyCallerView.findViewById(R.id.caller_number));
			callerNumber.setVisibility(View.VISIBLE);
			callerNumber.setText(number);
		}
		if (result != null)
		{
			TextView callerName = (TextView) stickyCallerView.findViewById(R.id.caller_name);
			callerName.setVisibility(View.VISIBLE);
			callerName.setText(result);
		}
	
		setFreeCallButton();
		
		setFreeSmsButton();
	}

	private static void setBasicClickListener()
	{
		stickyCallerView.findViewById(R.id.caller_settings_button).setOnClickListener(callerClickListener);
		stickyCallerView.findViewById(R.id.caller_close_button).setOnClickListener(callerClickListener);
	}

	private static void setCallerParams()
	{
		callerParams.gravity = Gravity.TOP | Gravity.LEFT;
		callerParams.x = Utils.getDeviceWidth();
		callerParams.y = 0;
		callerParams.alpha = 1.0f;
	}

	private static void settingLayoutDataLoading(Context context, String number, String result)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		stickyCallerView = (LinearLayout) inflater.inflate(R.layout.caller_layout, null);
		setBasicClickListener();
		stickyCallerView.findViewById(R.id.hike_caller_logo).setVisibility(View.VISIBLE);
	}

	private static void settingLayoutDataSuccess(Context context, String number, String result)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		JsonParser parser = new JsonParser();
		JsonObject callerDetails = (JsonObject) parser.parse(result);
		CallerContentModel callerContentModel = new Gson().fromJson(callerDetails, CallerContentModel.class);
		stickyCallerView = (LinearLayout) inflater.inflate(R.layout.caller_layout, null);
		setBasicClickListener();
		stickyCallerView.findViewById(R.id.caller_detail_view).setVisibility(View.VISIBLE);
		if (number != null)
		{
			TextView callerNumber = (TextView) (stickyCallerView.findViewById(R.id.caller_number));
			callerNumber.setVisibility(View.VISIBLE);
			callerNumber.setText(number);
		}
		if (callerContentModel.getFirstName() != null)
		{
			TextView callerName = (TextView) stickyCallerView.findViewById(R.id.caller_name);
			callerName.setVisibility(View.VISIBLE);
			String name = callerContentModel.getFirstName();
			if (callerContentModel.getLastName() != null)
			{
				name = name + " " + callerContentModel.getLastName();
			}
			callerName.setText(name);
			callCurrentName = name;
			setSaveContactClickListener();
		}
		else if (callerContentModel.getLastName() != null)
		{
			TextView callerName = (TextView) stickyCallerView.findViewById(R.id.caller_name);
			callerName.setVisibility(View.VISIBLE);
			callerName.setText(callerContentModel.getLastName());
		}
		if (callerContentModel.getLocation() != null)
		{
			TextView callerLocation = (TextView) (stickyCallerView.findViewById(R.id.caller_location));
			callerLocation.setVisibility(View.VISIBLE);
			callerLocation.setText(callerContentModel.getLocation());
		}

		setFreeCallButton();
		
		setFreeSmsButton();
	}
	

	private static void setSaveContactClickListener()
	{
		View saveContact = stickyCallerView.findViewById(R.id.caller_save_contact);
		saveContact.setVisibility(View.VISIBLE);
		saveContact.setOnClickListener(callerClickListener);
	}

	private static void setFreeSmsButton()
	{
		
		View freeSmsButton = stickyCallerView.findViewById(R.id.caller_free_sms_button); 
		freeSmsButton.setVisibility(View.VISIBLE);
		freeSmsButton.setOnClickListener(callerClickListener);
	}

	private static void setFreeCallButton()
	{
		View freeCallButton = stickyCallerView.findViewById(R.id.caller_free_call_button); 
		freeCallButton.setVisibility(View.VISIBLE);
		freeCallButton.setOnClickListener(callerClickListener);
	}

	private static void settingLayoutDataFailure(Context context, String number, String result)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		stickyCallerView = (LinearLayout) inflater.inflate(R.layout.caller_layout, null);
		setBasicClickListener();
		stickyCallerView.findViewById(R.id.net_not_connected).setVisibility(View.VISIBLE);
		stickyCallerView.findViewById(R.id.hike_caller_logo).setVisibility(View.VISIBLE);
	}

	private static OnClickListener callerClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			switch (v.getId())
			{
			case R.id.caller_call_button:
				if (callCurrentNumber != null)
				{
					Utils.killCall();
					Utils.makeCall(callCurrentNumber);
				}
				break;
			case R.id.caller_free_call_button:
				if (callCurrentNumber != null)
				{
					Utils.killCall();
					toCall = true;
				}
				break;
			case R.id.caller_free_sms_button:
				if (callCurrentNumber != null)
				{
					Utils.killCall();
					Intent intent = IntentFactory.createChatThreadIntentFromMsisdn(HikeMessengerApp.getInstance(), callCurrentNumber, true, false);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				    HikeMessengerApp.getInstance().startActivity(intent);
				}
				break;
			case R.id.caller_save_contact:
				if (callCurrentNumber != null && callCurrentName != null)
				{
					Utils.killCall();
					Utils.addToContacts(HikeMessengerApp.getInstance(), callCurrentNumber, callCurrentName);
				}
				break;
			case R.id.caller_sms_button:
				if (callCurrentNumber != null)
				{
					Utils.killCall();
					Utils.sendSMS(callCurrentNumber);
				}
				break;
			case R.id.caller_settings_button:
				Intent intent = IntentFactory.getStickyCallerSettingsIntent(HikeMessengerApp.getInstance());
				ChatHeadService.insertHomeActivitBeforeStarting(intent);
				break;
			case R.id.caller_close_button:
				removeCallerView();
				break;
			}
		}
	};
	
}
