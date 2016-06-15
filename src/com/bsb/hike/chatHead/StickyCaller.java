package com.bsb.hike.chatHead;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.CallerQuickReplyListAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.ui.BlockCallerActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PhoneUtils;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class StickyCaller {
	private static final String TAG = "StickyCaller";

	public static final String BLOCKED_LIST = "user_block";

	public static final String UNBLOCKED_LIST = "user_unblock";

	public static final String BLOCK_MSISDNS = "block_msisdns";

	private static LinearLayout stickyCallerView;

	private static WindowManager windowManager;

	static FrameLayout stickyCallerFrameHolder;

	public static final short NONE = 0;

	public static final short FAILURE = 0;

	public static final short SUCCESS = 2;

	public static final short ALREADY_SAVED = 1;

	public static short CALL_TYPE = NONE;

	public static final short INCOMING = 1;

	public static final short OUTGOING = 2;

	public static final short MISSED = 3;

	public static final short CLIPBOARD = 4;

	public static final short SMS = 5;

	public static final short AFTER_INCOMING_UNKNOWN = 6;

	public static final short AFTER_OUTGOING_UNKNOWN = 7;

	public static final short QUICK_REPLY = 8;

	public static String callCurrentNumber = null;

	public static final String SHOW_STICKY_CALLER = "showStickyCaller";

	private static final String CALLER_Y_PARAMS = "callerYParamsNew";

	public static final String CALLER_Y_PARAMS_OLD = "callerYParams";

	private static final long CALLER_DELAY = 2000;

	public static final int OUTGOING_DELAY = 12000;

	public static final int INCOMING_DELAY = 2000;

	public static final String SMS_BODY = "sms_body";

	public static final String SHOW_KNOWN_NUMBER_CARD = "showKnownCardPref";

	public static final String SHOW_FREEMESSAGE = "showFreeMsg";

	public static final String SHOW_FREECALL = "showFreeCall";

	public static final String ENABLE_CLIPBOARD_CARD = "enblClipCard";

	public static final String SHOW_SMS_CARD_PREF = "showSmsCardPref";

	private static final String INDIA = "India";

	public static final String NAME = "name";

	public static final int ONE_RETRY = 1;

	public static final int THREE_RETRIES = 3;

	public static String MISSED_CALL_TIMINGS;

	public static boolean toCall = false;

	private static boolean horizontalMovementDetected = false;

	private static boolean verticalMovementDetected = false;

	private static int statusBarHeight;

	public static String sms;

	public static boolean contactsSynched;

	public static boolean showFailCard = false;

	private static CallerContentModel quickReplyContentModel;

	private static boolean isItemClicked = false;

	private static String quickCallerCardOpenSource;

	public static Runnable removeViewRunnable = new Runnable() {

		@Override
		public void run()
		{
			//this will ensure that the remove caller view is not called for the following cards on callback
			if (CALL_TYPE != MISSED && CALL_TYPE != SMS && CALL_TYPE != AFTER_INCOMING_UNKNOWN && CALL_TYPE != AFTER_OUTGOING_UNKNOWN && CALL_TYPE != QUICK_REPLY)
			{
				removeCallerView();
			}
		}

	};

	static LayoutParams callerParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, LayoutParams.TYPE_SYSTEM_ERROR, LayoutParams.FLAG_NOT_FOCUSABLE
			, PixelFormat.TRANSLUCENT);

	private static void actionMove(Context context, int initialX, int initialY, float initialTouchX, float initialTouchY, MotionEvent event) {
		if ((ChatHeadLayout.getOverlayView() == null || !ChatHeadLayout.getOverlayView().isShown()) && stickyCallerFrameHolder != null && stickyCallerView != null) {
			float XaxisMovement = event.getRawX() - initialTouchX;
			float YaxisMovement = event.getRawY() - initialTouchY;
			float wanderableTouchDistance = (float) ViewConfiguration.get(context).getScaledTouchSlop();
			Logger.d(TAG, "check : " + XaxisMovement + " > " + wanderableTouchDistance);
			if (!(horizontalMovementDetected || verticalMovementDetected)) {
				horizontalMovementDetected = Math.abs(XaxisMovement) > wanderableTouchDistance;
				verticalMovementDetected = Math.abs(YaxisMovement) > wanderableTouchDistance;
			}

			if (verticalMovementDetected) {
				int actualYmovement = (int) (YaxisMovement + ((float) initialY));
				if (actualYmovement < 0) {
					callerParams.y = 0;
				} else if (actualYmovement > statusBarHeight - stickyCallerView.getHeight()) {
					callerParams.y = statusBarHeight - stickyCallerView.getHeight();
				} else {
					callerParams.y = actualYmovement;
				}
				try {
					windowManager.updateViewLayout(stickyCallerFrameHolder, callerParams);
				} catch (Exception e) {
					Logger.d(TAG, "Exception on updating view ");
				}
			}

			if (horizontalMovementDetected) {
				float linearHorizontalAlpha = Math.max(0.0f, Math.min(1.0f, 1.0f - (Math.abs(XaxisMovement) / ((float) context.getResources().getDisplayMetrics().widthPixels))));
				Logger.d(TAG, "setting alpha as : " + linearHorizontalAlpha);
				stickyCallerView.setAlpha(linearHorizontalAlpha);
				stickyCallerView.setTranslationX(XaxisMovement);
			}

		}
		return;
	}

	public static void removeCallerView()
	{
		Handler uiHandler = new Handler(HikeMessengerApp.getInstance().getApplicationContext().getMainLooper());
		if (uiHandler != null)
		{
			uiHandler.removeCallbacks(removeViewRunnable);
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					try {
						windowManager.removeView(stickyCallerFrameHolder);
						HikeSharedPreferenceUtil.getInstance().saveData(CALLER_Y_PARAMS, callerParams.y);
						stickyCallerView = null;
					} catch (Exception e) {
						Logger.d("Sticky Caller", "Removing Caller View");
					}

				}
			});
		}
	}

	public static void removeCallerViewWithDelay(int delay) {

		Handler uiHandler = new Handler(HikeMessengerApp.getInstance().getApplicationContext().getMainLooper());
		if (uiHandler != null) {
			uiHandler.postDelayed(removeViewRunnable, delay);
		}

	}

	static View.OnTouchListener onTouchListener = new View.OnTouchListener() {
		int initialX, initialY;

		float initialTouchX, initialTouchY;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			VelocityTracker exitSpeedTracker = VelocityTracker.obtain();
			Context ctx = HikeMessengerApp.getInstance().getApplicationContext();
			statusBarHeight = Utils.getDeviceHeight() - ChatThreadUtils.getStatusBarHeight(ctx);
			switch (event.getAction()) {
				case MotionEvent.ACTION_OUTSIDE:
					removeCallerView();
					isItemClicked = false;
					break;
				case MotionEvent.ACTION_DOWN:
					isItemClicked = true;
					initialX = callerParams.x;
					initialY = callerParams.y;

					if (stickyCallerView != null && initialY > statusBarHeight - stickyCallerView.getHeight()) {
						initialY = statusBarHeight - stickyCallerView.getHeight();
					}
					initialTouchX = event.getRawX();
					initialTouchY = event.getRawY();
					break;
				case MotionEvent.ACTION_UP:
					if (horizontalMovementDetected && stickyCallerView != null) {
						exitSpeedTracker.computeCurrentVelocity(1000);
						float exitSpeed = exitSpeedTracker.getXVelocity();
						if ((Math.abs(exitSpeed) <= Utils.densityMultiplier * 400.0f || Math.abs(initialTouchX - event.getRawX()) <= Utils.densityMultiplier * 25.0f)
								&& Math.abs(stickyCallerView.getTranslationX()) < ((float) (Utils.getDeviceWidth() / 2))) {
							Logger.d("UmangK", "dismissing" + "0");
							actionOnMotionUpEvent(0);
						} else {
							float Xmove = 0.0f;
							if (Math.abs(stickyCallerView.getTranslationX()) >= ((float) (Utils.getDeviceWidth() / 2))) {
								Xmove = stickyCallerView.getTranslationX();
							}
							Logger.d("UmangK", "" + ((int) Math.copySign((float) Utils.getDeviceWidth(), Xmove)));
							actionOnMotionUpEvent((int) Math.copySign((float) Utils.getDeviceWidth(), Xmove));
						}


						horizontalMovementDetected = false;
					}
					verticalMovementDetected = false;

					if(isItemClicked)
					{
						isItemClicked = false;
						if(v instanceof ListView)
						{
							ListView listView = ((ListView)v);
							int position = listView.pointToPosition((int) event.getX(), (int) event.getY());
							if(position!=ListView.INVALID_POSITION)
							{
								listView.performItemClick(listView.getChildAt(position - listView.getFirstVisiblePosition()), position, listView.getItemIdAtPosition(position));
							}
						}
					}
					break;
				case MotionEvent.ACTION_MOVE:
					actionMove(HikeMessengerApp.getInstance(), initialX, initialY, initialTouchX, initialTouchY, event);
					if(horizontalMovementDetected || verticalMovementDetected)
					{
						isItemClicked = false;
					}
					break;
			}
			return true;
		}
	};

	private static String getNumberFromCard()
	{
		if (stickyCallerView != null)
		{
			CharSequence text = ((TextView) stickyCallerView.findViewById(R.id.caller_number)).getText();
			if (!TextUtils.isEmpty(text))
				return text.toString();
		}
		return null;
	}

	private static void actionOnMotionUpEvent(final int movedOnXaxis) {
		TimeInterpolator accelerateDecelerateInterpolator;
		float alpha = 0.0f;
		if (movedOnXaxis == 0) {
			alpha = 1.0f;
			accelerateDecelerateInterpolator = new AccelerateDecelerateInterpolator();
		} else {
			accelerateDecelerateInterpolator = new AccelerateInterpolator();
			if (movedOnXaxis == (-1 * Utils.getDeviceWidth()) || movedOnXaxis == Utils.getDeviceWidth()) {
				Logger.d("UmangK", "may not dismiss");
			}
		}
		stickyCallerView.animate().translationX((float) movedOnXaxis).alpha(alpha).setDuration(500L).setInterpolator(accelerateDecelerateInterpolator).setListener(new AnimatorListener() {

			@Override
			public void onAnimationStart(Animator animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationRepeat(Animator animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animator animation) {
				if (movedOnXaxis != 0) {
					Logger.d(TAG, "making caller gone");
					stickyCallerFrameHolder.setVisibility(View.GONE);
					String msisdn = CALL_TYPE == QUICK_REPLY ? quickReplyContentModel.getMsisdn() : getNumberFromCard();
					HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.CLOSE_SWIPE, msisdn,
							AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				}
			}

			@Override
			public void onAnimationCancel(Animator animation) {
				// TODO Auto-generated method stub

			}
		});
	}


	public static void showCallerViewWithDelay(final String number, final CallerContentModel callerContentModel, final short type, final String source)
	{
		Handler uiHandler = new Handler(HikeMessengerApp.getInstance().getApplicationContext().getMainLooper());
		if (uiHandler != null)
		{
			uiHandler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					showCallerView(number, callerContentModel, type, source);
				}

			}, CALLER_DELAY);
		}

	}

	public static void showCallerView(String number, CallerContentModel callerContentModel, short type, String source)
	{
		final Context context = HikeMessengerApp.getInstance().getApplicationContext();
		windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		callerParams.flags = LayoutParams.FLAG_LAYOUT_NO_LIMITS | LayoutParams.FLAG_NOT_FOCUSABLE;
		callerParams.gravity = Gravity.CENTER;
		try
		{
			windowManager.removeView(stickyCallerFrameHolder);
		}
		catch (Exception e)
		{
			Logger.d(TAG, "error in adding caller view");
		}
		switch (type)
		{
		case ALREADY_SAVED:
			HAManager.getInstance().stickyCallerAnalyticsNonUIEvent(getCallEventFromCallType(CALL_TYPE), AnalyticsConstants.StickyCallerEvents.KNOWN, number,
					AnalyticsConstants.StickyCallerEvents.SUCCESS, source);
			settingLayoutDataSuccess(context, number, callerContentModel, true);
			break;

		case SUCCESS:
			HAManager.getInstance().stickyCallerAnalyticsNonUIEvent(getCallEventFromCallType(CALL_TYPE), AnalyticsConstants.StickyCallerEvents.UNKNOWN, number,
					AnalyticsConstants.StickyCallerEvents.SUCCESS, source);
			settingLayoutDataSuccess(context, number, callerContentModel, false);
			break;

		case FAILURE:
			if (CALL_TYPE == INCOMING || CALL_TYPE == OUTGOING)
			{
				settingLayoutDataFailure(context, number);
			}
			else
			{
				return;
			}
			break;

		case QUICK_REPLY:
			HAManager.getInstance().stickyCallerAnalyticsNonUIEvent(getCallEventFromCallType(CALL_TYPE), AnalyticsConstants.StickyCallerEvents.KNOWN, number,
					AnalyticsConstants.StickyCallerEvents.SUCCESS, source);
			quickReplyContentModel = callerContentModel;
			settingQuickReplyCardLayout(context, number, callerContentModel);
			break;
		}

		setCallerParams();
		try
		{
			stickyCallerFrameHolder = new FrameLayout(context);
			stickyCallerFrameHolder.setOnKeyListener(
					new View.OnKeyListener() {
						@Override
						public boolean onKey(View v, int keyCode, KeyEvent event) {
							removeCallerView();
							return false;
						}
					}
			);
			stickyCallerFrameHolder.setFocusableInTouchMode(true);
			stickyCallerFrameHolder.addView(stickyCallerView);
			windowManager.addView(stickyCallerFrameHolder, callerParams);
			stickyCallerView.setOnTouchListener(onTouchListener);
			if (CALL_TYPE == CLIPBOARD)
			{
				removeCallerViewWithDelay(OUTGOING_DELAY);
			}
		}
		catch (Exception e)
		{
			Logger.d(TAG, "error in adding caller view");
		}
	}

	private static void setCallerParamType() {

		switch (CALL_TYPE) {
			case INCOMING:
			case OUTGOING:
			case CLIPBOARD:
				callerParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
				break;
			case AFTER_INCOMING_UNKNOWN:
			case AFTER_OUTGOING_UNKNOWN:
			case MISSED:
			case SMS:
			case QUICK_REPLY:
				callerParams.type = LayoutParams.TYPE_PHONE;
				break;
		}
		if (showFailCard)
		{
			callerParams.type = LayoutParams.TYPE_PHONE;
			showFailCard = false;
		}
	}

	private static void showSmsView() {
		if (StickyCaller.sms != null) {
			stickyCallerView.findViewById(R.id.sms_divider).setVisibility(View.VISIBLE);
			stickyCallerView.findViewById(R.id.caller_sms_head).setVisibility(View.VISIBLE);
			TextView smsText = (TextView) stickyCallerView.findViewById(R.id.caller_sms_text);
			smsText.setVisibility(View.VISIBLE);
			smsText.setText(StickyCaller.sms);
		}

	}

	public static String getCallEventFromCallType(short callType) {

		switch (callType)
		{
			case INCOMING:
				return AnalyticsConstants.StickyCallerEvents.RECEIVED;

			case MISSED:
				return AnalyticsConstants.StickyCallerEvents.MISSED;

			case OUTGOING:
				return AnalyticsConstants.StickyCallerEvents.DIALED;

			case CLIPBOARD:
				return AnalyticsConstants.StickyCallerEvents.CLIPBOARD;

			case SMS:
				return AnalyticsConstants.StickyCallerEvents.SMS;

			case AFTER_INCOMING_UNKNOWN:
				return AnalyticsConstants.StickyCallerEvents.AFTER_INCOMING_UNKNOWN;

			case AFTER_OUTGOING_UNKNOWN:
				return AnalyticsConstants.StickyCallerEvents.AFTER_OUTGOING_UNKNOWN;

			case QUICK_REPLY:
				return AnalyticsConstants.StickyCallerEvents.QUICK_REPLY;

		}
		return null;
	}

	private static void setValueOnID(int id, String value) {
		TextView callerNumber = (TextView) (stickyCallerView.findViewById(id));
		callerNumber.setVisibility(View.VISIBLE);
		callerNumber.setText(value);
	}

	private static void setBasicClickListener(String number) {
		View callerSettingsButton = stickyCallerView.findViewById(R.id.caller_settings_button);
		callerSettingsButton.setTag(number);
		callerSettingsButton.setOnClickListener(callerClickListener);
		View callerCloseButton = stickyCallerView.findViewById(R.id.caller_close_button);
		callerCloseButton.setTag(number);
		callerCloseButton.setOnClickListener(callerClickListener);
	}

	private static void setCallerParams() {
		callerParams.gravity = Gravity.TOP | Gravity.LEFT;
		callerParams.y = HikeSharedPreferenceUtil.getInstance().getData(CALLER_Y_PARAMS, (int) (130f * Utils.densityMultiplier));
		callerParams.x = 0;
		callerParams.alpha = 1.0f;
		setCallerParamType();
	}

	private static void setShowResponse(String text) {
		TextView fetchingData = (TextView) stickyCallerView.findViewById(R.id.show_response);
		fetchingData.setVisibility(View.VISIBLE);
		fetchingData.setText(text);
	}

	private static void settingLayoutDataSuccess(Context context, String number, CallerContentModel callerContentModel, boolean isSaved)
	{
		if (CALL_TYPE == INCOMING || CALL_TYPE == OUTGOING)
		{
			settingOtherCallLayoutDataSuccess(context, number, callerContentModel);
		}
		else
		{
			settingMissedCallLayoutDataSuccess(context, number, callerContentModel, isSaved);
		}
	}

	public static void updateLayoutData(final CallerContentModel callerContentModel)
	{
		Handler uiHandler = new Handler(HikeMessengerApp.getInstance().getApplicationContext().getMainLooper());
		if (uiHandler != null)
		{
			uiHandler.post(new Runnable() {

				@Override
				public void run() {
					if (stickyCallerView != null && callerContentModel != null && stickyCallerView.isShown()
							&& CALL_TYPE != QUICK_REPLY && ((TextView) stickyCallerView.findViewById(R.id.caller_number)).getText().equals(callerContentModel.getMsisdn())) {
						if (callerContentModel.getFullName() != null
								&& ((TextView) stickyCallerView.findViewById(R.id.caller_name)).getText().equals(callerContentModel.getFullName())) {
							setValueOnID(R.id.caller_name, callerContentModel.getFullName());
						}

						if (callerContentModel.getLocation() != null
								&& ((TextView) stickyCallerView.findViewById(R.id.caller_location)).getText().equals(callerContentModel.getFullName())) {
							setValueOnID(R.id.caller_location, callerContentModel.getLocation());
						}
						setSpam(callerContentModel);
					}
				}
			});
		}

	}

	private static void settingMissedCallLayoutDataSuccess(Context context, String number, CallerContentModel callerContentModel, boolean isSaved)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		stickyCallerView = (LinearLayout) inflater.inflate(R.layout.missed_call_layout, null);
		callerParams.flags = LayoutParams.FLAG_LAYOUT_NO_LIMITS | LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | LayoutParams.FLAG_NOT_TOUCH_MODAL;
		callerParams.gravity = Gravity.BOTTOM;
		setBasicClickListener(number);
		boolean showSaveContactDivider = false;
		boolean toShowBlockDivider = false;
			if (callerContentModel != null && !TextUtils.isEmpty(callerContentModel.getMsisdn()))
		{
			setValueOnID(R.id.caller_number, callerContentModel.getMsisdn());
		}
		else if (!TextUtils.isEmpty(number))
		{
			setValueOnID(R.id.caller_number, number);
		}
		if (callerContentModel != null && !TextUtils.isEmpty(callerContentModel.getFullName()))
		{
			setValueOnID(R.id.caller_name, callerContentModel.getFullName());
			setSaveContactClickListener(number, isSaved);
			showSaveContactDivider = true;
		}
		if (callerContentModel != null && !TextUtils.isEmpty(callerContentModel.getLocation()))
		{
			setValueOnID(R.id.caller_location, callerContentModel.getLocation());
		}
		else if (PhoneUtils.isIndianNumber(number))
		{
			setValueOnID(R.id.caller_location, INDIA);
		}
		if (((callerContentModel != null && callerContentModel.getIsOnHike()) || (PhoneUtils.isIndianMobileNumber(number)))
				&& (HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_FREECALL, true) || HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_FREEMESSAGE, true)))
		{
			setFreeCallButton(number);

			setFreeMsgDivider();

			setFreeSmsButton(number, callerContentModel);

			if (showSaveContactDivider && !isSaved)
			{
				stickyCallerView.findViewById(R.id.missed_call_save_contact_divider).setVisibility(View.VISIBLE);
			}

			toShowBlockDivider = true;
		}

		if (!isSaved)
		{
			toShowBlockDivider = true;
		}

		if ( CALL_TYPE == MISSED && MISSED_CALL_TIMINGS != null)
		{
			TextView missedCallText = ((TextView) stickyCallerView.findViewById(R.id.missed_call_time));
			missedCallText.setText(context.getString(R.string.voip_missed_call_notif) + MISSED_CALL_TIMINGS);
			missedCallText.setVisibility(View.VISIBLE);
		}

		if (CALL_TYPE == SMS)
		{
			showSmsView();
		}

		setBlockContactButton(number, toShowBlockDivider, callerContentModel, true);

		setSpam(callerContentModel);
	}

	private static void setFreeMsgDivider()
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_FREEMESSAGE, true) && HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_FREECALL, true))
		{
			View callerDivider = stickyCallerView.findViewById(R.id.missed_call_free_divider);
			callerDivider.setVisibility(View.VISIBLE);
		}
	}

	private static void setSpam(CallerContentModel callerContentModel)
	{
		if (callerContentModel.isSpam())
		{
			TextView spam = (TextView) stickyCallerView.findViewById(R.id.spam);
			spam.setVisibility(View.VISIBLE);
		}
	}

	private static void setBlockContactButton(String msisdn, boolean isIndianOrhikeNo, CallerContentModel callerContentModel, boolean isAfterCallCard)
	{
		if (CALL_TYPE == INCOMING || CALL_TYPE == MISSED || CALL_TYPE == AFTER_INCOMING_UNKNOWN || CALL_TYPE == AFTER_OUTGOING_UNKNOWN)
		{
			if (msisdn != null && callerContentModel != null && !callerContentModel.isBlock())
			{
				if (HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_FREEMESSAGE, true) || (isAfterCallCard && HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_FREECALL, true)) || isIndianOrhikeNo)
				{
					View callerBlockButtonDivider = stickyCallerView.findViewById(R.id.block_contact_divider);
					callerBlockButtonDivider.setVisibility(View.VISIBLE);
				}
				View callBlockButton = stickyCallerView.findViewById(R.id.block_contact);
				callBlockButton.setVisibility(View.VISIBLE);
				callBlockButton.setTag(msisdn);
				callBlockButton.setOnClickListener(callerClickListener);
			}
		}
	}

	private static void settingQuickReplyCardLayout(final Context context, final String number, final CallerContentModel callerContentModel)
	{
		final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		stickyCallerView = (LinearLayout) inflater.inflate(R.layout.caller_quick_reply_layout, null);
		callerParams.flags = LayoutParams.FLAG_LAYOUT_NO_LIMITS | LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | LayoutParams.FLAG_NOT_TOUCH_MODAL;
		final ListView defaultQuickReplyListView = (ListView)stickyCallerView.findViewById(R.id.caller_quick_reply_list);
		HashSet<String> set = (HashSet<String>)HikeSharedPreferenceUtil.getInstance().getStringSet(HikeConstants.CALLER_QUICK_REPLY_SET, new HashSet<String>(Arrays.asList(context.getResources().getStringArray(R.array.caller_quick_reply_items))));
		final BaseAdapter adapter = new CallerQuickReplyListAdapter(context, new ArrayList<String>(set));
		defaultQuickReplyListView.setAdapter(adapter);
		defaultQuickReplyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
				String itemValue = (String) defaultQuickReplyListView.getItemAtPosition(position);

				removeCallerView();

				JSONObject textJSON = new JSONObject();
				try
				{
					textJSON.put(AnalyticsConstants.StickyCallerEvents.TEXT, itemValue);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				Utils.openChatThreadViaFreeSmsButton(callerContentModel, itemValue);
				recordQuickReplyCardItemSelected(position, adapter.getCount(), itemValue);
			}
		});

		//Adding card movement from list view
		defaultQuickReplyListView.setOnTouchListener(onTouchListener);

		//Handling Close cross button
		View callerCloseButton = stickyCallerView.findViewById(R.id.qr_caller_close_button);
		callerCloseButton.setTag(number);
		callerCloseButton.setOnClickListener(callerClickListener);

		//Handling Custom Quick reply Button
		View customQuickReplyButton = stickyCallerView.findViewById(R.id.caller_free_layout);
		customQuickReplyButton.setTag(number);
		customQuickReplyButton.setOnClickListener(callerClickListener);

	}

	private static void settingOtherCallLayoutDataSuccess(Context context, String number, CallerContentModel callerContentModel)
	{
		boolean isIndianOrhikeNo = false ;
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (CALL_TYPE == INCOMING || CALL_TYPE == OUTGOING)
		{
			stickyCallerView = (LinearLayout) inflater.inflate(R.layout.caller_layout, null);
		}
		setBasicClickListener(number);
		if (callerContentModel != null && !TextUtils.isEmpty(callerContentModel.getMsisdn()))
		{
			setValueOnID(R.id.caller_number, callerContentModel.getMsisdn());
		}
		else if (!TextUtils.isEmpty(number))
		{ 
			setValueOnID(R.id.caller_number, number);
		}
		if (callerContentModel != null && !TextUtils.isEmpty(callerContentModel.getFullName()))
		{
			setValueOnID(R.id.caller_name, callerContentModel.getFullName());
		}
		if (callerContentModel != null && !TextUtils.isEmpty(callerContentModel.getLocation()))
		{
			setValueOnID(R.id.caller_location, callerContentModel.getLocation());
		}
		else if (PhoneUtils.isIndianNumber(number))
		{
			setValueOnID(R.id.caller_location, INDIA);
		}
		if (callerContentModel != null && (callerContentModel.getIsOnHike() || PhoneUtils.isIndianMobileNumber(number)))
		{
			isIndianOrhikeNo = true;

			setFreeSmsButton(number, callerContentModel);
		}
		setDismissWithVisible(isIndianOrhikeNo);

		setBlockContactButton(number, isIndianOrhikeNo, callerContentModel, false);

		setSpam(callerContentModel);
	}
	
	

	private static void setSaveContactClickListener(String number, boolean isSaved)
	{
		if (!isSaved)
		{
			View saveContact = stickyCallerView.findViewById(R.id.missed_call_save_contact);
			saveContact.setVisibility(View.VISIBLE);
			saveContact.setTag(number);
			saveContact.setOnClickListener(callerClickListener);
		}
	}

	private static void setFreeSmsButton(String number, CallerContentModel callerContentModel)
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_FREEMESSAGE, true))
		{
			View freeSmsButton = stickyCallerView.findViewById(R.id.caller_free_message);
			freeSmsButton.setVisibility(View.VISIBLE);
			freeSmsButton.setTag(number);
			freeSmsButton.setOnClickListener(callerClickListener);
			if(callerContentModel != null)
			{
				quickReplyContentModel = callerContentModel;
			}
		}
	}

	private static void setFreeCallButton(String number)
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_FREECALL, true))
		{
			View freeCallButton = stickyCallerView.findViewById(R.id.caller_free_call);
			freeCallButton.setVisibility(View.VISIBLE);
			freeCallButton.setTag(number);
			freeCallButton.setOnClickListener(callerClickListener);
		}
	}
	
	private static void setDismissWithVisible(boolean isIndianOrHikeNo)
	{
		if (CALL_TYPE != OUTGOING || (HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_FREEMESSAGE, true) && isIndianOrHikeNo))
		{
			View freeCallButton = stickyCallerView.findViewById(R.id.caller_dismiss_with);
			freeCallButton.setVisibility(View.VISIBLE);
			stickyCallerView.findViewById(R.id.caller_free_layout).setVisibility(View.VISIBLE);
		}
	}

	private static String getPhoneNumberFromTag(View v)
	{
		return v.getTag() != null ? v.getTag().toString() : "";
	}

	private static void settingLayoutDataFailure(Context context, String number)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		stickyCallerView = (LinearLayout) inflater.inflate(R.layout.caller_layout, null);
		setBasicClickListener(number);
		setShowResponse(context.getString(R.string.net_not_connected));
		showFailCard = true;
	}

	private static OnClickListener callerClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			removeCallerView();
			switch (v.getId())
			{
			case R.id.caller_free_call:
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.FREE_CALL_BUTTON, getPhoneNumberFromTag(v),
						AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				if (v.getTag() != null)
				{
					if (CALL_TYPE == INCOMING || CALL_TYPE == OUTGOING)
					{
						callCurrentNumber = getPhoneNumberFromTag(v);
						IncomingCallReceiver.callReceived = true;
						toCall = true;
						Utils.killCall();
					}
					ChatHeadUtils.onCallClickedFromCallerCard(HikeMessengerApp.getInstance().getApplicationContext(), getPhoneNumberFromTag(v),
							VoIPUtils.CallSource.HIKE_STICKY_CALLER);
				}
				break;
			/*  When "Free SMS" button is clicked on caller card */
			case R.id.caller_free_message:
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.FREE_SMS_BUTTON, getPhoneNumberFromTag(v), AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				if (v.getTag() != null)
				{
					IncomingCallReceiver.callReceived = true;
					quickCallerCardOpenSource = getCallEventFromCallType(CALL_TYPE);
					CALL_TYPE = QUICK_REPLY;
					Utils.killCall();
					StickyCaller.showCallerViewWithDelay(getPhoneNumberFromTag(v), quickReplyContentModel, StickyCaller.QUICK_REPLY, AnalyticsConstants.StickyCallerEvents.STATIC_QUICK_REPLY_BUTTON);
				}
				break;
			case R.id.missed_call_save_contact:
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.SAVE_CONTACT, getPhoneNumberFromTag(v), AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				CallerContentModel callerContentModel =  ContactManager.getInstance().getCallerContentModelFromMsisdn(v.getTag().toString());
				if (v.getTag() != null && callerContentModel != null && callerContentModel.getFullName() != null)
				{
					Utils.killCall();
					Utils.addToContacts(HikeMessengerApp.getInstance().getApplicationContext(), getPhoneNumberFromTag(v), callerContentModel.getFullName(), callerContentModel.getLocation());
				}
				break;
			case R.id.caller_settings_button:
				IncomingCallReceiver.callReceived = true;
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.CALLER_SETTINGS_BUTTON, getPhoneNumberFromTag(v), AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				IntentFactory.openStickyCallerSettings(HikeMessengerApp.getInstance().getApplicationContext(), true);
				break;
			case R.id.caller_close_button:
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.CLOSE_BUTTON, getPhoneNumberFromTag(v),
						AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				break;
			/*  When cross button is clicked on Quick reply card */
			case R.id.qr_caller_close_button:
				recordQuickReplyCrossButtonClicked();
				break;
			case R.id.block_contact:
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.BLOCK, getPhoneNumberFromTag(v),
						AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				Intent intent = new Intent(HikeMessengerApp.getInstance().getApplicationContext(), BlockCallerActivity.class);
				CallerContentModel contentModel = ContactManager.getInstance().getCallerContentModelFromMsisdn(v.getTag().toString());
				if (v.getTag() != null && contentModel != null && contentModel.getFullName() != null)
				{
					intent.putExtra(HikeConstants.MSISDN, v.getTag().toString());
					intent.putExtra(HikeConstants.NAME, contentModel.getFullName());
					intent.putExtra(HikeConstants.CALL_TYPE, getCallEventFromCallType(CALL_TYPE));
				}
				ChatHeadUtils.insertHomeActivitBeforeStarting(intent);
				IncomingCallReceiver.callReceived = true;
				CALL_TYPE = NONE;
				Utils.killCall();
				break;
			/*  When "Write your own" button is clicked on Quick reply card */
			case R.id.caller_free_layout:
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.CUSTOM_QUICK_REPLY_BUTTON, getPhoneNumberFromTag(v),
						AnalyticsConstants.StickyCallerEvents.CARD, getCallEventFromCallType(CALL_TYPE));
				if (v.getTag() != null)
				{
					IncomingCallReceiver.callReceived = true;
					CALL_TYPE = NONE;
					Utils.killCall();
					removeCallerView();
					Utils.openChatThreadViaFreeSmsButton(quickReplyContentModel, null);
					recordQuickReplyCardItemSelected(-1, -1, null);
				}
				break;
			}
		}
	};

	private static void recordQuickReplyCrossButtonClicked()
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.V2.UNIQUE_KEY, AnalyticsConstants.CALLER_FREE_SMS_CROSS);
			json.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.ACT_LOG);
			json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.STICKY_CALLER);
			json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CALLER_CARD);
			json.put(AnalyticsConstants.V2.ORDER, AnalyticsConstants.CALLER_FREE_SMS);
			json.put(AnalyticsConstants.V2.FAMILY, System.currentTimeMillis());
			json.put(AnalyticsConstants.V2.SPECIES, AnalyticsConstants.CROSS);
			json.put(AnalyticsConstants.V2.GENUS, quickCallerCardOpenSource);
			Logger.d("c_spam_logs", " QuickReplyCrossButtonClicked logs are \n " + json);
			HAManager.getInstance().recordV2(json);

		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private static void recordQuickReplyCardItemSelected(int position, int totalItem, String text)
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.V2.UNIQUE_KEY, AnalyticsConstants.CALLER_FREE_SMS_QUICK_REPLY_MSG);
			json.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.ACT_LOG);
			json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.STICKY_CALLER);
			json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CALLER_CARD);
			json.put(AnalyticsConstants.V2.ORDER, AnalyticsConstants.CALLER_FREE_SMS);
			json.put(AnalyticsConstants.V2.FAMILY, System.currentTimeMillis());
			json.put(AnalyticsConstants.V2.SPECIES, position + 1); //as list starts with 0
			json.put(AnalyticsConstants.V2.RACE, text);
			json.put(AnalyticsConstants.V2.BREED, totalItem);
			json.put(AnalyticsConstants.V2.GENUS, quickCallerCardOpenSource);
			Logger.d("c_spam_logs", " QuickReplyCrossButtonClicked logs are \n " + json);
			HAManager.getInstance().recordV2(json);

		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

}
