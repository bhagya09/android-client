package com.bsb.hike.chatHead;

import java.net.HttpURLConnection;

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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
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

	public static final short NONE = 0;

	private static final short HORIZONTAL = 1;

	private static final short VERTICAL = 2;

	private static final int ANIMATION_TIME = 400;

	public static final short FAILURE = 0;

	public static final short LOADING = 1;

	public static final short SUCCESS = 2;

	public static final short ALREADY_SAVED = 3;
	
	public static short CALL_TYPE = NONE;
	
	public static short INCOMING = 1;
	
	public static short OUTGOING = 2;
	
	public static short MISSED = 3;
	
	public static String callCurrentNumber = null;

	public static String callCurrentName = null;

	public static final String ACTIVATE_STICKY_CALLER = "activateStickyCaller";

	public static final String SHOW_STICKY_CALLER = "showStickyCaller";

	private static final String CALLER_Y_PARAMS = "callerYParams";

	private static final long CALLER_DELAY = 2000;
	
	public static final int OUTGOING_DELAY = 12000;
	
	public static final int INCOMING_DELAY = 2000;

	public static final String SMS_BODY = "sms_body";
	
	public static String MISSED_CALL_TIMINGS;

	public static boolean toCall = false;

	static LayoutParams callerParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, LayoutParams.TYPE_SYSTEM_ERROR, LayoutParams.FLAG_NOT_FOCUSABLE
			| LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT);

	private static void actionMove(Context context, int initialX, int initialY, float initialTouchX, float initialTouchY, MotionEvent event)
	{
		if (ChatHeadLayout.getOverlayView() == null || !ChatHeadLayout.getOverlayView().isShown())
		{

			if ((moveType == NONE || moveType == HORIZONTAL) && (Math.abs(event.getRawX() - initialTouchX) > Math.abs(event.getRawY() - initialTouchY)))
			{
				callerParams.x = initialX + (int) (event.getRawX() - initialTouchX);
				callerParams.alpha = 1.0f - ((float) Math.abs(callerParams.x) / Utils.getDeviceWidth());
				moveType = HORIZONTAL;
			}
			else if ((moveType == NONE || moveType == VERTICAL) && Math.abs(event.getRawX() - initialTouchX) < Math.abs(event.getRawY() - initialTouchY))
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
		ValueAnimator translate = ValueAnimator.ofInt(from, to);
		translate.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
		{
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator)
			{
				callerParams.x = (int) valueAnimator.getAnimatedValue();
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
		translate.setDuration(ANIMATION_TIME);
		translate.start();
	}

	public static void removeCallerView()
	{
		try
		{
			HikeSharedPreferenceUtil.getInstance().saveData(CALLER_Y_PARAMS, callerParams.y);
			windowManager.removeView(stickyCallerView);
			stickyCallerView = null;
		}
		catch (Exception e)
		{
			Logger.d("Sticky Caller", "Removing Caller View");
		}
	}
	
	public static void removeCallerViewWithDelay(int delay)
	{
		HikeHandlerUtil mThread = HikeHandlerUtil.getInstance();
		mThread.startHandlerThread();
		if (mThread != null)
		{
			mThread.postRunnableWithDelay(new Runnable()
			{

				@Override
				public void run()
				{
					if (CALL_TYPE != MISSED)
					{
						removeCallerView();
					}
				}

			}, delay);
		}

	}

	static OnSwipeTouchListener onSwipeTouchListener = new OnSwipeTouchListener(HikeMessengerApp.getInstance().getApplicationContext())
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
			case MotionEvent.ACTION_OUTSIDE:
				removeCallerView();
				break;
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
	
	
	public static void showCallerViewWithDelay(final String number, final String result, final short type, final String source)
	{
		HikeHandlerUtil mThread = HikeHandlerUtil.getInstance();
		mThread.startHandlerThread();
		if (mThread != null)
		{
			mThread.postRunnableWithDelay(new Runnable()
			{

				@Override
				public void run()
				{
					showCallerView(number, result, type, source);
				}

			}, CALLER_DELAY);
		}
	}
	
	
	
	
	public static void showCallerView(String number, String result, short type, String source)
	{
		final Context context = HikeMessengerApp.getInstance().getApplicationContext();
		windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		callerParams.flags = LayoutParams.FLAG_LAYOUT_NO_LIMITS | LayoutParams.FLAG_NOT_FOCUSABLE ;
		try
		{
			windowManager.removeView(stickyCallerView);
		}
		catch (Exception e)
		{
			Logger.d("StickyCaller", "error in adding caller view");
		}
		if (CALL_TYPE == INCOMING || CALL_TYPE == OUTGOING)
		{
			switch (type)
			{
			case ALREADY_SAVED:
				HAManager.getInstance().stickyCallerAnalyticsNonUIEvent(getCallEventFromCallType(CALL_TYPE), AnalyticsConstants.StickyCallerEvents.KNOWN, number, AnalyticsConstants.StickyCallerEvents.SUCCESS, source);
				settingLayoutAlreadySavedContact(context, number, result);
				break;

			case LOADING:
				settingLayoutDataLoading(context, number, result);
				break;
			case SUCCESS:
				HAManager.getInstance().stickyCallerAnalyticsNonUIEvent(getCallEventFromCallType(CALL_TYPE), AnalyticsConstants.StickyCallerEvents.UNKNOWN, number, AnalyticsConstants.StickyCallerEvents.SUCCESS, source);
				settingLayoutDataSuccess(context, number, result);
				break;

			case FAILURE:
				HAManager.getInstance().stickyCallerAnalyticsNonUIEvent(getCallEventFromCallType(CALL_TYPE), AnalyticsConstants.StickyCallerEvents.UNKNOWN, number, AnalyticsConstants.StickyCallerEvents.FAIL, source);
				settingLayoutDataFailure(context, number, result);
				break;
			}
		}
		else if (CALL_TYPE == MISSED)
		{
			switch (type)
			{
			case ALREADY_SAVED:
				HAManager.getInstance().stickyCallerAnalyticsNonUIEvent(AnalyticsConstants.StickyCallerEvents.MISSED, AnalyticsConstants.StickyCallerEvents.KNOWN, number, AnalyticsConstants.StickyCallerEvents.SUCCESS, source);
				settingLayoutAlreadySavedContact(context, number, result);
				break;
				
			case SUCCESS:
				HAManager.getInstance().stickyCallerAnalyticsNonUIEvent(AnalyticsConstants.StickyCallerEvents.MISSED, AnalyticsConstants.StickyCallerEvents.UNKNOWN, number, AnalyticsConstants.StickyCallerEvents.SUCCESS, source);
				settingLayoutDataSuccess(context, number, result);
				break;
			
			case FAILURE:
				HAManager.getInstance().stickyCallerAnalyticsNonUIEvent(AnalyticsConstants.StickyCallerEvents.MISSED, AnalyticsConstants.StickyCallerEvents.UNKNOWN, number, AnalyticsConstants.StickyCallerEvents.FAIL, source);
				break;
				
			}
			CALL_TYPE = NONE;
		}
		setCallerParams();
		try
		{
			windowManager.addView(stickyCallerView, callerParams);
			stickyCallerView.setOnTouchListener(onSwipeTouchListener);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.d("StickyCaller", "error in adding caller view");
		}
	}

	private static String getCallEventFromCallType(short callType)
	{
		if (callType == INCOMING)
		{
			return AnalyticsConstants.StickyCallerEvents.RECEIVED;
		}
		if (callType == MISSED)
		{
			return AnalyticsConstants.StickyCallerEvents.MISSED;
		}
		if (callType == OUTGOING)
		{
			return AnalyticsConstants.StickyCallerEvents.DIALED;
		}
		return null;
	}

	private static void settingLayoutAlreadySavedContact(Context context, String number, String result)
	{
		if (CALL_TYPE == MISSED)
		{
			settingMissedCallLayoutAlreadySavedContact(context, number, result);
		}
		else
		{
			settingOtherCallLayoutAlreadySavedContact(context, number, result);
		}
	}
	
	private static void settingMissedCallLayoutAlreadySavedContact(Context context, String number, String result)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		stickyCallerView = (LinearLayout) inflater.inflate(R.layout.missed_call_layout, null);
		callerParams.flags = LayoutParams.FLAG_LAYOUT_NO_LIMITS | LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | LayoutParams.FLAG_NOT_TOUCH_MODAL  ;
		setBasicClickListener();
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
	
		if (Utils.isIndianNumber(number) || Utils.isOnHike(number))
		{
			stickyCallerView.findViewById(R.id.missed_call_free_divider).setVisibility(View.VISIBLE);

			setFreeCallButton();

			setFreeSmsButton();
		}
		if (MISSED_CALL_TIMINGS != null)
		{
			((TextView) stickyCallerView.findViewById(R.id.missed_call_time)).setText(context.getString(R.string.voip_missed_call_notif) + MISSED_CALL_TIMINGS);
		}
	}

	private static void settingOtherCallLayoutAlreadySavedContact(Context context, String number, String result)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		stickyCallerView = (LinearLayout) inflater.inflate(R.layout.caller_layout, null);
		setBasicClickListener();
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
	
		if (Utils.isIndianNumber(number) || Utils.isOnHike(number))
		{
			setDismissWithVisible();
			
			setFreeCallButton();

			setFreeSmsButton();
		}
	}
	
	private static void setCallButton()
	{
/*		View callButton = stickyCallerView.findViewById(R.id.caller_call_button); 
		callButton.setVisibility(View.VISIBLE);
		callButton.setOnClickListener(callerClickListener);
*/	}
	
	private static void setSMSButton()
	{
/*		View smsButton = stickyCallerView.findViewById(R.id.caller_sms_button); 
		smsButton.setVisibility(View.VISIBLE);
		smsButton.setOnClickListener(callerClickListener);
*/	}

	private static void setBasicClickListener()
	{
		stickyCallerView.findViewById(R.id.caller_settings_button).setOnClickListener(callerClickListener);
		stickyCallerView.findViewById(R.id.caller_close_button).setOnClickListener(callerClickListener);
	}

	private static void setCallerParams()
	{
		callerParams.gravity = Gravity.TOP | Gravity.LEFT;
		callerParams.y = HikeSharedPreferenceUtil.getInstance().getData(CALLER_Y_PARAMS, 0);
		callerParams.x = 0;
		callerParams.alpha = 1.0f;
	}

	private static void setShowResponse(String text)
	{
		TextView fetchingData = (TextView)stickyCallerView.findViewById(R.id.show_response);
		fetchingData.setVisibility(View.VISIBLE);
		fetchingData.setText(text);
	}
	
	private static void settingLayoutDataLoading(Context context, String number, String result)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		stickyCallerView = (LinearLayout) inflater.inflate(R.layout.caller_layout, null);
		setBasicClickListener();
		setShowResponse(context.getString(R.string.fetching_info));
		View hikeCallerlogo = stickyCallerView.findViewById(R.id.hike_caller_logo);
		hikeCallerlogo.setVisibility(View.VISIBLE);
		Animation myFadeInAnimation = AnimationUtils.loadAnimation(context, R.drawable.blink_animation);
		hikeCallerlogo.startAnimation(myFadeInAnimation);
	}

	private static void settingLayoutDataSuccess(Context context, String number, String result)
	{
		try
		{
			JsonParser parser = new JsonParser();
			JsonObject callerDetails = (JsonObject) parser.parse(result);
			CallerContentModel callerContentModel = new Gson().fromJson(callerDetails, CallerContentModel.class);
			if (CALL_TYPE == MISSED)
			{
				settingMissedCallLayoutDataSuccess(context, number, result, callerContentModel);
			}
			else
			{
				settingOtherCallLayoutDataSuccess(context, number, result, callerContentModel);
			}
		}
		catch (Exception e)
		{
			Logger.d("CallerLayoutSuccessError", "JsonException");
		}
	}

	private static void settingMissedCallLayoutDataSuccess(Context context, String number, String result, CallerContentModel callerContentModel)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		stickyCallerView = (LinearLayout) inflater.inflate(R.layout.missed_call_layout, null);
		callerParams.flags = LayoutParams.FLAG_LAYOUT_NO_LIMITS | LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | LayoutParams.FLAG_NOT_TOUCH_MODAL  ;
		setBasicClickListener();
		boolean showSaveContactDivider = false;
		if (number != null)
		{ 
			TextView callerNumber = (TextView) (stickyCallerView.findViewById(R.id.caller_number));
			callerNumber.setVisibility(View.VISIBLE);
			callerNumber.setText(number);
		}
		if (callerContentModel != null && callerContentModel.getFirstName() != null)
		{
			TextView callerName = (TextView) stickyCallerView.findViewById(R.id.caller_name);
			callerName.setVisibility(View.VISIBLE);
			String name = getFullName(callerContentModel); 
			callerName.setText(name);
			callCurrentName = name;
			setSaveContactClickListener();
			showSaveContactDivider =true;
		}
		else if (callerContentModel != null &&callerContentModel.getLastName() != null)
		{
			TextView callerName = (TextView) stickyCallerView.findViewById(R.id.caller_name);
			callerName.setVisibility(View.VISIBLE);
			callCurrentName = callerContentModel.getLastName();
			callerName.setText(callerContentModel.getLastName());
			setSaveContactClickListener();
			showSaveContactDivider = true;
		}
		if (callerContentModel != null && callerContentModel.getLocation() != null)
		{
			TextView callerLocation = (TextView) (stickyCallerView.findViewById(R.id.caller_location));
			callerLocation.setVisibility(View.VISIBLE);
			callerLocation.setText(callerContentModel.getLocation());
		}
		if ((callerContentModel != null && callerContentModel.getIsOnHike()) || Utils.isIndianNumber(number))
		{
			setFreeCallButton();

			setFreeSmsButton();
			
			stickyCallerView.findViewById(R.id.missed_call_free_divider).setVisibility(View.VISIBLE);
		
			if (showSaveContactDivider)
			{
				stickyCallerView.findViewById(R.id.missed_call_save_contact_divider).setVisibility(View.VISIBLE);
			}
		}
		
		if (MISSED_CALL_TIMINGS != null)
		{
			((TextView) stickyCallerView.findViewById(R.id.missed_call_time)).setText(context.getString(R.string.voip_missed_call_notif) + MISSED_CALL_TIMINGS);
		}
	}
	
	private static String getFullName(CallerContentModel callerContentModel)
	{
		String name = callerContentModel.getFirstName();
		if (callerContentModel.getLastName() != null)
		{
			name = name + " " + callerContentModel.getLastName();
		}
		return name;
	}
	
	private static void settingOtherCallLayoutDataSuccess(Context context, String number, String result, CallerContentModel callerContentModel)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		stickyCallerView = (LinearLayout) inflater.inflate(R.layout.caller_layout, null);
		setBasicClickListener();
		if (number != null)
		{ 
			TextView callerNumber = (TextView) (stickyCallerView.findViewById(R.id.caller_number));
			callerNumber.setVisibility(View.VISIBLE);
			callerNumber.setText(number);
		}
		if (callerContentModel != null && callerContentModel.getFirstName() != null)
		{
			TextView callerName = (TextView) stickyCallerView.findViewById(R.id.caller_name);
			callerName.setVisibility(View.VISIBLE);
			String name = getFullName(callerContentModel); 
			callerName.setText(name);
			callCurrentName = name;
		}
		else if (callerContentModel != null &&callerContentModel.getLastName() != null)
		{
			TextView callerName = (TextView) stickyCallerView.findViewById(R.id.caller_name);
			callerName.setVisibility(View.VISIBLE);
			callCurrentName = callerContentModel.getLastName();
			callerName.setText(callerContentModel.getLastName());
		}
		if (callerContentModel != null && callerContentModel.getLocation() != null)
		{
			TextView callerLocation = (TextView) (stickyCallerView.findViewById(R.id.caller_location));
			callerLocation.setVisibility(View.VISIBLE);
			callerLocation.setText(callerContentModel.getLocation());
		}
		if ((callerContentModel != null && callerContentModel.getIsOnHike()) || Utils.isIndianNumber(number))
		{
			setDismissWithVisible();
			
			setFreeCallButton();

			setFreeSmsButton();
		}
	}
	
	

	private static void setSaveContactClickListener()
	{
		View saveContact = stickyCallerView.findViewById(R.id.missed_call_save_contact);
		saveContact.setVisibility(View.VISIBLE);
		saveContact.setOnClickListener(callerClickListener);
	}

	private static void setFreeSmsButton()
	{
		
		View freeSmsButton = stickyCallerView.findViewById(R.id.caller_free_message); 
		freeSmsButton.setVisibility(View.VISIBLE);
		freeSmsButton.setOnClickListener(callerClickListener);
	}

	private static void setFreeCallButton()
	{
		View freeCallButton = stickyCallerView.findViewById(R.id.caller_free_call); 
		freeCallButton.setVisibility(View.VISIBLE);
		freeCallButton.setOnClickListener(callerClickListener);
	}
	
	private static void setDismissWithVisible()
	{
		View freeCallButton = stickyCallerView.findViewById(R.id.caller_dismiss_with); 
		freeCallButton.setVisibility(View.VISIBLE);
		stickyCallerView.findViewById(R.id.caller_free_layout).setVisibility(View.VISIBLE);
	}

	private static void settingLayoutDataFailure(Context context, String number, String result)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		stickyCallerView = (LinearLayout) inflater.inflate(R.layout.caller_layout, null);
		setBasicClickListener();
		if (result.equals(Integer.toString(HttpURLConnection.HTTP_FORBIDDEN)))
		{
			setShowResponse(context.getString(R.string.caller_error_403));
		}
		else if (result.equals(Integer.toString(HttpURLConnection.HTTP_NOT_ACCEPTABLE)) || result.equals(Integer.toString(HttpURLConnection.HTTP_CLIENT_TIMEOUT)))
		{
			setShowResponse(context.getString(R.string.caller_error_406));
		}
		else if (result.equals(Integer.toString(HttpURLConnection.HTTP_INTERNAL_ERROR)))
		{
			setShowResponse(context.getString(R.string.caller_error_500));
		}
		else
		{
			setShowResponse(context.getString(R.string.net_not_connected));
		}
	}

	private static OnClickListener callerClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			removeCallerView();
			switch (v.getId())
			{
/*			case R.id.caller_call_button:
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.CALL_BUTTON, StickyCaller.callCurrentNumber, AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				if (callCurrentNumber != null)
				{
					Utils.killCall();
					Utils.makeCall(callCurrentNumber);
				}
				break;
*/			case R.id.caller_free_call:
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.FREE_CALL_BUTTON, StickyCaller.callCurrentNumber, AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				if (callCurrentNumber != null)
				{
					if (CALL_TYPE == INCOMING || CALL_TYPE == OUTGOING)
					{
						IncomingCallReceiver.callReceived = true;
						toCall = true;
						Utils.killCall();
					}
					else if (StickyCaller.callCurrentNumber !=  null)
					{
						ChatHeadUtils.onCallClickedFromCallerCard(HikeMessengerApp.getInstance().getApplicationContext(), StickyCaller.callCurrentNumber, VoIPUtils.CallSource.HIKE_STICKY_CALLER);
					}
					
				}
				break;
			case R.id.caller_free_message:
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.FREE_SMS_BUTTON, StickyCaller.callCurrentNumber, AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				if (callCurrentNumber != null)
				{
					IncomingCallReceiver.callReceived = true;
					Utils.killCall();
					Intent intent = IntentFactory.createChatThreadIntentFromMsisdn(HikeMessengerApp.getInstance(), callCurrentNumber, true, false);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				    HikeMessengerApp.getInstance().startActivity(intent);
				}
				break;
			case R.id.missed_call_save_contact:
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.SAVE_CONTACT, StickyCaller.callCurrentNumber, AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				if (callCurrentNumber != null && callCurrentName != null)
				{
					Utils.killCall();
					Utils.addToContacts(HikeMessengerApp.getInstance().getApplicationContext(), callCurrentNumber, callCurrentName);
				}
				break;
/*			case R.id.caller_sms_button:
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.SMS_BUTTON, StickyCaller.callCurrentNumber, AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				if (callCurrentNumber != null)
				{
					Utils.sendSMS(callCurrentNumber, HikeMessengerApp.getInstance().getApplicationContext().getString(R.string.hi));
				}
				break;
*/			case R.id.caller_settings_button:
				IncomingCallReceiver.callReceived = true;
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.CALLER_SETTINGS_BUTTON, StickyCaller.callCurrentNumber, AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				Intent intent = IntentFactory.getStickyCallerSettingsIntent(HikeMessengerApp.getInstance());
				ChatHeadUtils.insertHomeActivitBeforeStarting(intent);
				break;
			case R.id.caller_close_button:
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.CLOSE_BUTTON, StickyCaller.callCurrentNumber, AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				break;
			}
		}
	};
	
}
