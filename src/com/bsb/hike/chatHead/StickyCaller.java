package com.bsb.hike.chatHead;

import java.net.HttpURLConnection;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.sax.StartElementListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewDebug.FlagToString;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.R.string;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
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
	private static final String TAG = "StickyCaller";
	
	private static LinearLayout stickyCallerView;

	private static WindowManager windowManager;

	static FrameLayout stickyCallerFrameHolder;
	
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

	public static final String SHOW_STICKY_CALLER = "showStickyCaller";

	private static final String CALLER_Y_PARAMS = "callerYParams";

	private static final long CALLER_DELAY = 2000;
	
	public static final int OUTGOING_DELAY = 12000;
	
	public static final int INCOMING_DELAY = 2000;

	public static final String SMS_BODY = "sms_body";

	public static final String SHOW_KNOWN_NUMBER_CARD = "showKnownCardPref";
	
	public static final String SHOW_FREECALL_VIEW = "showFreeView";

	private static final String INDIA = "India";

	public static final String NAME = "name";
	
	public static final String ADDRESS = "address";
	
	public static String MISSED_CALL_TIMINGS;

	public static boolean toCall = false;

    private static boolean horizontalMovementDetected = false;
    
    private static boolean verticalMovementDetected = false;
    
    private static int statusBarHeight;
    
	public static Runnable removeViewRunnable = new Runnable()
	{

		@Override
		public void run()
		{
			if (CALL_TYPE != MISSED)
			{
				removeCallerView();
			}
		}

	};
	
	private static void removeViewCallBacks()
	{
		HikeHandlerUtil mThread = HikeHandlerUtil.getInstance();
		try
		{
			mThread.removeRunnable(removeViewRunnable);
		}
		catch (Exception e)
		{
			Logger.d(TAG,"exceptionRemoveViewCallbacks");
		}
	}

	static LayoutParams callerParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, LayoutParams.TYPE_SYSTEM_ERROR, LayoutParams.FLAG_NOT_FOCUSABLE
			, PixelFormat.TRANSLUCENT);

	private static void actionMove(Context context, int initialX, int initialY, float initialTouchX, float initialTouchY, MotionEvent event)
	{
		if ((ChatHeadLayout.getOverlayView() == null || !ChatHeadLayout.getOverlayView().isShown()) && stickyCallerFrameHolder != null && stickyCallerView != null) 
		{
			float XaxisMovement = event.getRawX() - initialTouchX;
			float YaxisMovement = event.getRawY() - initialTouchY;
			float wanderableTouchDistance =  (float)ViewConfiguration.get(context).getScaledTouchSlop();
			Logger.d(TAG,"check : " + XaxisMovement + " > " + wanderableTouchDistance);
			if(!(horizontalMovementDetected || verticalMovementDetected))
			{
				horizontalMovementDetected = Math.abs(XaxisMovement) >  wanderableTouchDistance;
				verticalMovementDetected = Math.abs(YaxisMovement) > wanderableTouchDistance;
			}

			if (verticalMovementDetected)
			{
				int actualYmovement = (int) (YaxisMovement + ((float) initialY));
				if (actualYmovement < 0)
				{
					callerParams.y = 0;
				}
				else if (actualYmovement > statusBarHeight - stickyCallerView.getHeight())
				{
					callerParams.y = statusBarHeight - stickyCallerView.getHeight();
				}
				else
				{
					callerParams.y = actualYmovement;
				}
				try
				{
					windowManager.updateViewLayout(stickyCallerFrameHolder, callerParams);
				}
				catch(Exception e)
				{
					Logger.d(TAG, "Exception on updating view ");
				}
			}

			
			if (horizontalMovementDetected)
			{
				float linearHorizontalAlpha = Math.max(0.0f, Math.min(1.0f, 1.0f - (Math.abs(XaxisMovement) / ((float) context.getResources().getDisplayMetrics().widthPixels))));
				Logger.d(TAG, "setting alpha as : " + linearHorizontalAlpha);
				stickyCallerView.setAlpha(linearHorizontalAlpha);
				stickyCallerView.setTranslationX(XaxisMovement);
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
					windowManager.updateViewLayout(stickyCallerFrameHolder, callerParams);
				}
				catch (Exception e)
				{
					Logger.d(TAG, "view not found");
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
			removeViewCallBacks();
			HikeSharedPreferenceUtil.getInstance().saveData(CALLER_Y_PARAMS, callerParams.y);
			windowManager.removeView(stickyCallerFrameHolder);
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
			mThread.postRunnableWithDelay(removeViewRunnable, delay);
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
//					stickyCallerView.clearAnimation();
//					slideAnimation(callerParams.x, Utils.getDeviceWidth());
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
//					stickyCallerView.clearAnimation();
//					slideAnimation(callerParams.x, -(Utils.getDeviceWidth()));
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
			VelocityTracker exitSpeedTracker =  VelocityTracker.obtain();
			Context ctx = HikeMessengerApp.getInstance().getApplicationContext();
			statusBarHeight = Utils.getDeviceHeight() - ChatThreadUtils.getStatusBarHeight(ctx);
			
			switch (event.getAction())
			{   
			case MotionEvent.ACTION_OUTSIDE:
				removeCallerView();
				break;
			case MotionEvent.ACTION_DOWN:
				initialX = callerParams.x;
				initialY = callerParams.y;
				
				  if (stickyCallerView != null && initialY > statusBarHeight - stickyCallerView.getHeight()) {
                      initialY = statusBarHeight - stickyCallerView.getHeight();
                  } 
				initialTouchX = event.getRawX();
				initialTouchY = event.getRawY();
				moveType = NONE;
				break;
			case MotionEvent.ACTION_UP:
				if(horizontalMovementDetected && stickyCallerView!=null)
				{
					exitSpeedTracker.computeCurrentVelocity(1000);
					float exitSpeed = exitSpeedTracker.getXVelocity();
					if ((Math.abs(exitSpeed) <= Utils.densityMultiplier * 400.0f || Math.abs(initialTouchX - event.getRawX()) <= Utils.densityMultiplier * 25.0f)
							&& Math.abs(stickyCallerView.getTranslationX()) < ((float) (Utils.getDeviceWidth() / 2)))
					{
						Logger.d("UmangK", "dismissing" + "0");
						actionOnMotionUpEvent(0);
					}
					else
					{
						float Xmove = 0.0f;
						if (Math.abs(stickyCallerView.getTranslationX()) >= ((float) (Utils.getDeviceWidth() / 2)))
						{
							Xmove = stickyCallerView.getTranslationX();
						}
						Logger.d("UmangK", "" + ((int) Math.copySign((float) Utils.getDeviceWidth(), Xmove)));
						actionOnMotionUpEvent((int) Math.copySign((float) Utils.getDeviceWidth(), Xmove));
					}
					  
					
					horizontalMovementDetected = false;
				}
				verticalMovementDetected = false;

				break;
			case MotionEvent.ACTION_MOVE:
				actionMove(HikeMessengerApp.getInstance(), initialX, initialY, initialTouchX, initialTouchY, event);
				break;
			}
			return gestureDetector.onTouchEvent(event);
		}
	};
	
	private static void actionOnMotionUpEvent(final int movedOnXaxis)
	{
		TimeInterpolator accelerateDecelerateInterpolator;
		float alpha = 0.0f;
		if (movedOnXaxis == 0)
		{
			alpha = 1.0f;
			accelerateDecelerateInterpolator = new AccelerateDecelerateInterpolator();
		}
		else
		{
			accelerateDecelerateInterpolator = new AccelerateInterpolator();
			if (movedOnXaxis == (-1 * Utils.getDeviceWidth()) || movedOnXaxis == Utils.getDeviceWidth())
			{
				Logger.d("UmangK", "may not dismiss");
			}
		}
		stickyCallerView.animate().translationX((float) movedOnXaxis).alpha(alpha).setDuration(500L).setInterpolator(accelerateDecelerateInterpolator).setListener(new AnimatorListener()
		{
			
			@Override
			public void onAnimationStart(Animator animation)
			{
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationRepeat(Animator animation)
			{
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationEnd(Animator animation)
			{
				if(movedOnXaxis != 0)
				{
					Logger.d(TAG,"making caller gone");
					stickyCallerFrameHolder.setVisibility(View.GONE);
				}
			}
			
			@Override
			public void onAnimationCancel(Animator animation)
			{
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	
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
		callerParams.gravity =Gravity.CENTER;
		try
		{
			windowManager.removeView(stickyCallerFrameHolder);
		}
		catch (Exception e)
		{
			Logger.d(TAG, "error in adding caller view");
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
			stickyCallerFrameHolder = new FrameLayout(context);
			stickyCallerFrameHolder.addView(stickyCallerView);
			windowManager.addView(stickyCallerFrameHolder, callerParams);
			stickyCallerView.setOnTouchListener(onSwipeTouchListener);
		}
		catch (Exception e)
		{
			Logger.d(TAG, "error in adding caller view");
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
		String name = null, address = null;
		try
		{
			JSONObject obj = new JSONObject(result);
			name = (String) obj.get(NAME);
			address = (String) obj.get(ADDRESS);
		}
		catch (JSONException e)
		{
		Logger.d("JSON Exception", "can't fetch data from saved contact");
		}
		if (CALL_TYPE == MISSED)
		{
			settingMissedCallLayoutAlreadySavedContact(context, number, name, address);
		}
		else
		{
			settingOtherCallLayoutAlreadySavedContact(context, number, name, address);
		}
	}
	
	private static void settingMissedCallLayoutAlreadySavedContact(Context context, String number, String name, String address)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		stickyCallerView = (LinearLayout) inflater.inflate(R.layout.missed_call_layout, null);
		callerParams.flags = LayoutParams.FLAG_LAYOUT_NO_LIMITS | LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | LayoutParams.FLAG_NOT_TOUCH_MODAL  ;
		callerParams.gravity = Gravity.BOTTOM;
		setBasicClickListener(number);
		if (number != null)
		{
			setValueOnID(R.id.caller_number, number);
		}
		if (name != null)
		{
			setValueOnID(R.id.caller_name, name);
		}
		else if (number != null)
		{
			setValueOnID(R.id.caller_name, number);
		}
		if (address != null)
		{
			setValueOnID(R.id.caller_location, address);
		}
		else if (Utils.isIndianNumber(number))
		{
			setValueOnID(R.id.caller_location, INDIA);
		}
		if ((Utils.isIndianMobileNumber(number) && HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_FREECALL_VIEW, true)) || Utils.isOnHike(number))
		{
			stickyCallerView.findViewById(R.id.missed_call_free_divider).setVisibility(View.VISIBLE);

			setFreeCallButton(number);

			setFreeSmsButton(number);
		}
		if (MISSED_CALL_TIMINGS != null)
		{
			((TextView) stickyCallerView.findViewById(R.id.missed_call_time)).setText(context.getString(R.string.voip_missed_call_notif) + MISSED_CALL_TIMINGS);
		}
	}

	private static void setValueOnID(int id, String value)
	{
		TextView callerNumber = (TextView) (stickyCallerView.findViewById(id));
		callerNumber.setVisibility(View.VISIBLE);
		callerNumber.setText(value);
	}
	private static void settingOtherCallLayoutAlreadySavedContact(Context context, String number, String name, String address)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		stickyCallerView = (LinearLayout) inflater.inflate(R.layout.caller_layout, null);
		setBasicClickListener(number);
		if (number != null)
		{
			setValueOnID(R.id.caller_number, number);
		}
		if (name != null)
		{
			setValueOnID(R.id.caller_name, name);
		}
		else if (number != null)
		{
			setValueOnID(R.id.caller_name, number);
		}
		if (address != null)
		{
			setValueOnID(R.id.caller_location, address);
		}
		else if (Utils.isIndianNumber(number))
		{
			setValueOnID(R.id.caller_location, INDIA);
		}
		if ((Utils.isIndianMobileNumber(number) && HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_FREECALL_VIEW, true)) || Utils.isOnHike(number))
		{
			setDismissWithVisible();
			
			setFreeCallButton(number);

			setFreeSmsButton(number);
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

	private static void setBasicClickListener(String number)
	{
		View callerSettingsButton = stickyCallerView.findViewById(R.id.caller_settings_button);
		callerSettingsButton.setTag(number);
		callerSettingsButton.setOnClickListener(callerClickListener);
		View callerCloseButton = stickyCallerView.findViewById(R.id.caller_close_button);
		callerCloseButton.setTag(number);
		callerCloseButton.setOnClickListener(callerClickListener);
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
		setBasicClickListener(number);
		setShowResponse(context.getString(R.string.getting_details));
		View hikeCallerlogo = stickyCallerView.findViewById(R.id.hike_caller_logo);
		hikeCallerlogo.setVisibility(View.VISIBLE);
		Animation myFadeInAnimation = AnimationUtils.loadAnimation(context, R.drawable.blink_animation);
		hikeCallerlogo.startAnimation(myFadeInAnimation);
	}

	private static void settingLayoutDataSuccess(Context context, String number, String result)
	{
		try
		{
			CallerContentModel callerContentModel = ChatHeadUtils.getCallerContentModelObject(result);
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
		callerParams.gravity = Gravity.BOTTOM;
		setBasicClickListener(number);
		boolean showSaveContactDivider = false;
		if (callerContentModel != null && callerContentModel.getMsisdn()!= null)
		{
			setValueOnID(R.id.caller_number, callerContentModel.getMsisdn());
		}
		else if (number != null)
		{ 
			setValueOnID(R.id.caller_number, number);
		}
		if (callerContentModel != null && callerContentModel.getFirstName() != null)
		{
			setValueOnID(R.id.caller_name, getFullName(callerContentModel));
			setSaveContactClickListener(number);
			showSaveContactDivider =true;
		}
		else if (callerContentModel != null &&callerContentModel.getLastName() != null)
		{
			setValueOnID(R.id.caller_name, callerContentModel.getLastName());
			setSaveContactClickListener(number);
			showSaveContactDivider = true;
		}
		if (callerContentModel != null && callerContentModel.getLocation() != null)
		{
			setValueOnID(R.id.caller_location, callerContentModel.getLocation());
		}
		else if (Utils.isIndianNumber(number))
		{
			setValueOnID(R.id.caller_location, INDIA);
		}
		if ((callerContentModel != null && callerContentModel.getIsOnHike()) || (Utils.isIndianMobileNumber(number) && HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_FREECALL_VIEW, true)))
		{
			setFreeCallButton(number);

			setFreeSmsButton(number);
			
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
		if (callerContentModel != null)
		{
			String name = "";
			if (callerContentModel.getFirstName() != null)
			{
				name = callerContentModel.getFirstName() + " ";
			}
			if (callerContentModel.getLastName() != null)
			{
				name = name + callerContentModel.getLastName();
			}
			return name;
		}
		return null;
	}
	
	private static void settingOtherCallLayoutDataSuccess(Context context, String number, String result, CallerContentModel callerContentModel)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		stickyCallerView = (LinearLayout) inflater.inflate(R.layout.caller_layout, null);
		setBasicClickListener(number);
		if (callerContentModel != null && callerContentModel.getMsisdn()!= null)
		{
			setValueOnID(R.id.caller_number, callerContentModel.getMsisdn());
		}
		else if (number != null)
		{ 
			setValueOnID(R.id.caller_number, number);
		}
		if (callerContentModel != null && callerContentModel.getFirstName() != null)
		{
			setValueOnID(R.id.caller_name, getFullName(callerContentModel));
		}
		else if (callerContentModel != null &&callerContentModel.getLastName() != null)
		{
			setValueOnID(R.id.caller_name, callerContentModel.getLastName());
		}
		if (callerContentModel != null && callerContentModel.getLocation() != null)
		{
			setValueOnID(R.id.caller_location, callerContentModel.getLocation());
		}
		else if (Utils.isIndianNumber(number))
		{
			setValueOnID(R.id.caller_location, INDIA);
		}
		if ((callerContentModel != null && callerContentModel.getIsOnHike()) || (Utils.isIndianMobileNumber(number) && HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_FREECALL_VIEW, true)))
		{
			setDismissWithVisible();
			
			setFreeCallButton(number);

			setFreeSmsButton(number);
		}
	}
	
	

	private static void setSaveContactClickListener(String number)
	{
		View saveContact = stickyCallerView.findViewById(R.id.missed_call_save_contact);
		saveContact.setVisibility(View.VISIBLE);
		saveContact.setTag(number);
		saveContact.setOnClickListener(callerClickListener);
	}

	private static void setFreeSmsButton(String number)
	{
		
		View freeSmsButton = stickyCallerView.findViewById(R.id.caller_free_message); 
		freeSmsButton.setVisibility(View.VISIBLE);
		freeSmsButton.setTag(number);
		freeSmsButton.setOnClickListener(callerClickListener);
	}

	private static void setFreeCallButton(String number)
	{
		View freeCallButton = stickyCallerView.findViewById(R.id.caller_free_call); 
		freeCallButton.setVisibility(View.VISIBLE);
		freeCallButton.setTag(number);
		freeCallButton.setOnClickListener(callerClickListener);
	}
	
	private static void setDismissWithVisible()
	{
		View freeCallButton = stickyCallerView.findViewById(R.id.caller_dismiss_with); 
		freeCallButton.setVisibility(View.VISIBLE);
		stickyCallerView.findViewById(R.id.caller_free_layout).setVisibility(View.VISIBLE);
	}

	private static String getPhoneNumberFromTag(View v)
	{
		return v.getTag() != null ? v.getTag().toString() : "";
	}
	
	private static void settingLayoutDataFailure(Context context, String number, String result)
	{
		if (result.equals(Integer.toString(HttpException.REASON_CODE_NO_NETWORK)))
		{
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			stickyCallerView = (LinearLayout) inflater.inflate(R.layout.caller_layout, null);
			setBasicClickListener(number);
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
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.FREE_CALL_BUTTON, getPhoneNumberFromTag(v), AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				if (v.getTag() != null)
				{
					if (CALL_TYPE == INCOMING || CALL_TYPE == OUTGOING)
					{
						callCurrentNumber = getPhoneNumberFromTag(v);
						IncomingCallReceiver.callReceived = true;
						toCall = true;
						Utils.killCall();
					}
						ChatHeadUtils.onCallClickedFromCallerCard(HikeMessengerApp.getInstance().getApplicationContext(), getPhoneNumberFromTag(v), VoIPUtils.CallSource.HIKE_STICKY_CALLER);
				}
				break;
			case R.id.caller_free_message:
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.FREE_SMS_BUTTON, getPhoneNumberFromTag(v), AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				if (v.getTag() != null)
				{
					IncomingCallReceiver.callReceived = true;
					Utils.killCall();
					Utils.sendFreeSms(getPhoneNumberFromTag(v));
				}
				break;
			case R.id.missed_call_save_contact:
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.SAVE_CONTACT, getPhoneNumberFromTag(v), AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				CallerContentModel callerContentModel = ChatHeadUtils.getCallerContentModelObject(HikeSharedPreferenceUtil.getInstance(HikeConstants.CALLER_SHARED_PREF).getData(getPhoneNumberFromTag(v), null));
				if (v.getTag() != null && getFullName(callerContentModel) != null)
				{
					Utils.killCall();
					Utils.addToContacts(HikeMessengerApp.getInstance().getApplicationContext(), getPhoneNumberFromTag(v), getFullName(callerContentModel), callerContentModel.getLocation());
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
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.CALLER_SETTINGS_BUTTON, getPhoneNumberFromTag(v), AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				IntentFactory.openStickyCallerSettings(HikeMessengerApp.getInstance().getApplicationContext(), true);
				break;
			case R.id.caller_close_button:
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.CLOSE_BUTTON, getPhoneNumberFromTag(v), AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				break;
			}
		}
	};
	
}
