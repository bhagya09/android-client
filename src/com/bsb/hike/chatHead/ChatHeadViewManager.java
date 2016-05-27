package com.bsb.hike.chatHead;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.Animator.AnimatorListener;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.ui.utils.RecyclingImageView;
import com.hike.cognito.UserLogInfo;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.ShareUtils;
import com.bsb.hike.utils.Utils;

public class ChatHeadViewManager
{

	private static Context context;

	private static final int ANIMATION_TIME = 300;

	private static final int RECT_CONST = 4;

	private static int RECT_CONST_DP;

	private static final int INITIAL_POS_X = 0;

	private static final int INITIAL_POS_Y = 200;

	private static List<String> sharablePackageList = new ArrayList<String>();

	private static Map<String, String> sharablePackageMap = new HashMap<String, String>();

	public static String sharableActivePackage = HikeConstants.Extras.WHATSAPP_PACKAGE;

	private WindowManager windowManager;

	private ImageView chatHead, closeHead;

	private int savedPosX = INITIAL_POS_X;

	private int savedPosY = INITIAL_POS_Y;

	private static ChatHeadViewManager _instance;

	final GestureDetector gestureDetector = new GestureDetector(new GestureListener());

	private String TAG = "ChatHeadService";

	// boolean to show whether the chat head must be shown or not for a particular session
	private static boolean showStickeyOnActiveApps = true;

	private LayoutParams chatHeadParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, LayoutParams.TYPE_PHONE, LayoutParams.FLAG_NOT_FOCUSABLE,
			PixelFormat.TRANSLUCENT);

	private LayoutParams closeHeadParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, LayoutParams.TYPE_PHONE, LayoutParams.FLAG_NOT_FOCUSABLE,
			PixelFormat.TRANSLUCENT);

	private LayoutParams stickerPickerParams;

	public static String foregroundAppName = HikeConstants.Extras.WHATSAPP_PACKAGE;

	public static int dismissed = 0;

	private ChatHeadViewManager(Context ctx)
	{
		context = ctx;
	}

	public static ChatHeadViewManager getInstance(Context ctx)
	{
		if (_instance == null)
		{
			synchronized (ChatHeadViewManager.class)
			{
				if (_instance == null)
				{

					_instance = new ChatHeadViewManager(ctx);
				}
			}
		}
		return _instance;
	}

	public void onCreate()
	{
		HAManager.getInstance().serviceEventAnalytics(HikeConstants.CREATE, HikeConstants.STICKEY_SERVICE);

		windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

		chatHead = new RecyclingImageView(context);

		chatHead.setImageResource(R.drawable.sticker_chat_head);

		setChatHeadParams();

		setCloseHeadParams();

		setStickerPickerParams();

		createListfromJson();

		try
		{
			windowManager.addView(chatHead, chatHeadParams);
		}
		catch (Exception e)
		{
			Logger.d(TAG, "adding chat head");
		}

		chatHead.setVisibility(View.INVISIBLE);

		chatHead.setOnTouchListener(chatHeadOnTouchListener);

		UserLogInfo.recordSessionInfo(ChatHeadUtils.getRunningAppPackage(ChatHeadUtils.GET_TOP_MOST_SINGLE_PROCESS), UserLogInfo.START);

		RECT_CONST_DP = (int) (RECT_CONST * Utils.densityMultiplier);
	}

	public void onDestroy()
	{

		UserLogInfo.recordSessionInfo(ChatHeadUtils.getRunningAppPackage(ChatHeadUtils.GET_TOP_MOST_SINGLE_PROCESS), UserLogInfo.STOP);
		try
		{
			windowManager.removeView(chatHead);
		}
		catch (Exception e)
		{
			Logger.d(TAG, "chatHeadRemove");
		}
		try
		{
			windowManager.removeView(closeHead);
		}
		catch (Exception e)
		{
			Logger.d(TAG, "closeHeadRemove");
		}
		try
		{
			if (ChatHeadLayout.getOverlayView() != null)
			{
				windowManager.removeView(ChatHeadLayout.getOverlayView());
			}
		}
		catch (Exception e)
		{
			Logger.d(TAG, "ChatHeadLayoutRemove");
		}
	}

	private static void createListfromJson()
	{
		try
		{
			sharablePackageList.clear();
			JSONArray packagesJSONArray = new JSONArray(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.PACKAGE_LIST, ""));
			List<String> sharablePackages = Utils.getPackagesMatchingIntent(Intent.ACTION_SEND, null, "image/jpeg");
			for (int i = 0; i < packagesJSONArray.length(); i++)
			{
				JSONObject obj = packagesJSONArray.getJSONObject(i);
				{
					String packageName = obj.optString(HikeConstants.ChatHead.PACKAGE_NAME, null);
					if (obj.optBoolean(HikeConstants.ChatHead.APP_ENABLE, false) && packageName != null && sharablePackages.contains(packageName))
					{
						sharablePackageList.add(obj.getString(HikeConstants.ChatHead.PACKAGE_NAME));
						sharablePackageMap.put(obj.getString(HikeConstants.ChatHead.PACKAGE_NAME), obj.optString(HikeConstants.ChatHead.APP_NAME, ""));
					}
				}
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private void setStickerPickerParams()
	{
		int height = (int) (Utils.densityMultiplier * 248) + ChatThreadUtils.getStatusBarHeight(context);
		stickerPickerParams = new LayoutParams(LayoutParams.MATCH_PARENT, height, LayoutParams.TYPE_PHONE, LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.TRANSLUCENT);
		stickerPickerParams.gravity = Gravity.BOTTOM;
	}

	private class GestureListener extends SimpleOnGestureListener
	{

		@Override
		public void onLongPress(MotionEvent e)
		{
			Logger.d("MotionEvent", "long press evnet");
			try
			{
				windowManager.addView(closeHead, closeHeadParams);
			}
			catch (Exception e1)
			{
				Logger.d("Exception", "unable to add view on WindowManager");
			}
			super.onLongPress(e);
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
		{
			Logger.d("MotionEvent", "scrolled");
			try
			{
				if (closeHead.isShown())
				{
					windowManager.updateViewLayout(closeHead, closeHeadParams);
				}
				else
				{
					windowManager.addView(closeHead, closeHeadParams);
				}
			}
			catch (Exception e)
			{
				Logger.d("Exception", "unable to add view on WindowManager");
			}
			return super.onScroll(e1, e2, distanceX, distanceY);
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e)
		{
			Logger.d("MotionEvent", "tapsingle");
			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e)
		{
			Logger.d("MotionEvent", "tapDouble");
			return true;
		}

	}

	public void overlayAnimation(final View view2animate, Integer viewX, Integer endX, Integer viewY, Integer endY, final int flag, final String path)
	{
		ValueAnimator translateX = ValueAnimator.ofInt(viewX, endX);
		ValueAnimator translateY = ValueAnimator.ofInt(viewY, endY);
		translateX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
		{
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator)
			{
				chatHeadParams.x = (Integer) valueAnimator.getAnimatedValue();
			}
		});

		translateX.setDuration(ANIMATION_TIME);
		translateY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
		{
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator)
			{
				chatHeadParams.y = (Integer) valueAnimator.getAnimatedValue();
				// safe check
				try
				{
					windowManager.updateViewLayout(view2animate, chatHeadParams);
				}
				catch (Exception e)
				{
					Logger.d(TAG, "animation update chatheadparams");
				}
			}
		});

		translateY.setDuration(ANIMATION_TIME);
		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.playTogether(translateX, translateY);
		animatorSet.addListener(new AnimatorListener()
		{
			@Override
			public void onAnimationStart(Animator animation)
			{
				chatHead.setOnTouchListener(null);
				if (ChatHeadLayout.getOverlayView() != null)
				{
					try
					{
						windowManager.removeView(ChatHeadLayout.detachPicker(context.getApplicationContext()));
					}
					catch (Exception e)
					{
						Logger.d("UmangX", "busted");
					}
				}
			}

			@Override
			public void onAnimationRepeat(Animator animation)
			{
			}

			@Override
			public void onAnimationEnd(Animator animation)
			{
				chatHead.setOnTouchListener(chatHeadOnTouchListener);
				if(closeHead.isShown())
				{
					try
					{
						windowManager.removeView(closeHead);
					}
					catch (Exception e) {
						Logger.d(TAG, "close head remove exception");
					}
				}
				
				switch (flag)
				{
				case ChatHeadConstants.REMAINING_ANIMATION:
					savedPosX = chatHeadParams.x;
					savedPosY = chatHeadParams.y;
					break;
				case ChatHeadConstants.CREATING_CHAT_HEAD_ACTIVITY_ANIMATION:
//					if (!ChatHeadUtils.getRunningAppPackage(ChatHeadUtils.GET_TOP_MOST_SINGLE_PROCESS).contains(sharableActivePackage))
//					{
//						break;
//					}
					createAndOpenChatHeadPickerLayout(context.getApplicationContext());
					break;
				case ChatHeadConstants.SHARING_BEFORE_FINISHING_ANIMATION:
					ShareUtils.shareContent(HikeConstants.Extras.ShareTypes.STICKER_SHARE, path, sharableActivePackage, true);
					break;
				case ChatHeadConstants.STOPPING_SERVICE_ANIMATION:
					ChatHeadUtils.stopService();
					break;
				case ChatHeadConstants.GET_MORE_STICKERS_ANIMATION:
					Intent stickerShareWebViewIntent = IntentFactory.getStickerShareWebViewActivityIntent(context.getApplicationContext());
					ChatHeadUtils.insertHomeActivitBeforeStarting(stickerShareWebViewIntent);
					setChatHeadInvisible();
					break;
				case ChatHeadConstants.OPEN_HIKE_ANIMATION:
					context.startActivity(IntentFactory.getHomeActivityIntentAsLauncher(context.getApplicationContext()).addFlags(
							Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME));
					setChatHeadInvisible();
					break;
				case ChatHeadConstants.STICKER_SHOP_ANIMATION:
					HAManager.getInstance().record(HikeConstants.LogEvent.STKR_SHOP_BTN_CLICKED, AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH);
					Intent stickerShopIntent = IntentFactory.getStickerShopIntent(context.getApplicationContext());
					ChatHeadUtils.insertHomeActivitBeforeStarting(stickerShopIntent);
					break;
				case ChatHeadConstants.OPEN_SETTINGS_ANIMATION:
					Intent stickerShareIntent = IntentFactory.getStickerShareSettingsIntent(context.getApplicationContext());
					ChatHeadUtils.insertHomeActivitBeforeStarting(stickerShareIntent);
					setChatHeadInvisible();
					break;
				}
			}

			@Override
			public void onAnimationCancel(Animator animation)
			{
			}
		});
		animatorSet.start();

	}

	private OnTouchListener chatHeadOnTouchListener = new OnTouchListener()
	{
		int initialX;

		int initialY;

		float initialTouchX;

		float initialTouchY;

		@Override
		public boolean onTouch(View v, MotionEvent event)
		{
			boolean isSingleTap = gestureDetector.onTouchEvent(event);
			switch (event.getAction())
			{
			case MotionEvent.ACTION_DOWN:
				initialX = chatHeadParams.x;
				initialY = chatHeadParams.y;
				initialTouchX = event.getRawX();
				initialTouchY = event.getRawY();
				return true;

			case MotionEvent.ACTION_UP:
				actionUp(isSingleTap);
				return true;

			case MotionEvent.ACTION_MOVE:
				actionMove(initialX, initialY, initialTouchX, initialTouchY, event);
				return true;
			}

			return false;

		}
	};

	private void actionUp(boolean isSingleTap)
	{

		int[] chatHeadLocations = new int[2];

		int[] closeHeadLocations = new int[2];

		Rect rectChatHead, rectCloseHead;

		chatHead.getLocationOnScreen(chatHeadLocations);
		closeHead.getLocationOnScreen(closeHeadLocations);
		rectChatHead = new Rect(chatHeadLocations[0] - RECT_CONST_DP, chatHeadLocations[1] - RECT_CONST_DP, chatHeadLocations[0] + chatHead.getWidth() + RECT_CONST_DP,
				chatHeadLocations[1] + chatHead.getHeight() + RECT_CONST_DP);
		rectCloseHead = new Rect(closeHeadLocations[0] - RECT_CONST_DP, closeHeadLocations[1] - RECT_CONST_DP, closeHeadLocations[0] + closeHead.getWidth() + RECT_CONST_DP,
				closeHeadLocations[1] + closeHead.getHeight() + RECT_CONST_DP);
		if (closeHeadLocations[0] != 0 && closeHeadLocations[1] != 0 && Rect.intersects(rectChatHead, rectCloseHead))
		{
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.STICKER_HEAD_DISMISS, foregroundAppName);
			dismissed++;
			if (dismissed <= HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.DISMISS_COUNT, ChatHeadConstants.DISMISS_CONST))
			{
				if (chatHead.isShown())
				{
					showStickeyOnActiveApps = false;
					setChatHeadParams();
					try
					{
						windowManager.updateViewLayout(chatHead, chatHeadParams);
					}
					catch (Exception e)
					{
						Logger.d(TAG, "updating chat head params");
					}

					chatHead.setVisibility(View.INVISIBLE);
				}
				if (closeHead.isShown())
				{
					try
					{
						windowManager.removeView(closeHead);
					}
					catch (Exception e)
					{
						Logger.d(TAG, "removing close head");
					}
				}
			}
			else
			{
				openingChatHeadWindow();
			}
		}
		else
		{
			if (isSingleTap)
			{
				HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.STICKER_HEAD, foregroundAppName);
				if (ChatHeadLayout.getOverlayView() == null || !ChatHeadLayout.getOverlayView().isShown())
				{
					openingChatHeadWindow();
				}
				else
				{
					resetPosition(ChatHeadConstants.FINISHING_CHAT_HEAD_ACTIVITY_ANIMATION, null);
				}
			}
			else
			{

				if (chatHeadLocations[0] <= (int) ((context.getResources().getDisplayMetrics().widthPixels - chatHead.getWidth()) / 2))
				{
					overlayAnimation(chatHead, chatHeadParams.x, 0, chatHeadParams.y, chatHeadParams.y, ChatHeadConstants.REMAINING_ANIMATION, null);
				}
				else
				{
					overlayAnimation(chatHead, chatHeadParams.x, context.getResources().getDisplayMetrics().widthPixels - chatHead.getWidth(), chatHeadParams.y, chatHeadParams.y,
							ChatHeadConstants.REMAINING_ANIMATION, null);
				}
			}
		}
		if (closeHead.isShown())
		{
			try
			{
				windowManager.removeView(closeHead);
			}
			catch (Exception e)
			{
				Logger.d(TAG, "removing close head");
			}
		}
	}

	private void setChatHeadParams()
	{
		chatHeadParams.x = savedPosX;
		chatHeadParams.y = savedPosY;
		chatHeadParams.gravity = Gravity.TOP | Gravity.LEFT;
	}

	private void setCloseHeadParams()
	{
		closeHead = new RecyclingImageView(context);
		closeHead.setImageResource(R.drawable.close_chat_head);
		closeHeadParams.gravity = Gravity.BOTTOM | Gravity.CENTER;
	}

	public void setChatHeadInvisible()
	{
		chatHead.setVisibility(View.INVISIBLE);
	}

	private void openingChatHeadWindow()
	{
		final float scale = Utils.densityMultiplier;
		int status_bar_height = ChatThreadUtils.getStatusBarHeight(context);
		int pixelsX;
		// 8 dp margin + 38 dp left part of image to point - width of icon/2
		pixelsX = (int) ((46 * scale) - (chatHead.getWidth() / 2));
		// 240 dp image height + 8dp image margin bottom + size of icon
		int pixelsY = (int) (context.getResources().getDisplayMetrics().heightPixels - (scale * 248) - chatHead.getHeight() - status_bar_height);
		savedPosX = chatHeadParams.x;
		savedPosY = chatHeadParams.y;
		overlayAnimation(chatHead, chatHeadParams.x, pixelsX, chatHeadParams.y, pixelsY, ChatHeadConstants.CREATING_CHAT_HEAD_ACTIVITY_ANIMATION, null);
	}

	private void actionMove(int initialX, int initialY, Float initialTouchX, Float initialTouchY, MotionEvent event)
	{
		int[] chatHeadLocations = new int[2];

		int[] closeHeadLocations = new int[2];

		Rect rectChatHead, rectCloseHead;

		if (ChatHeadLayout.getOverlayView() == null || !ChatHeadLayout.getOverlayView().isShown())
		{
			chatHeadParams.x = initialX + (int) (event.getRawX() - initialTouchX);
			chatHeadParams.y = initialY + (int) (event.getRawY() - initialTouchY);
			if (chatHeadParams.x < 0)
				chatHeadParams.x = 0;
			if (chatHeadParams.x > context.getResources().getDisplayMetrics().widthPixels - chatHead.getWidth())
				chatHeadParams.x = context.getResources().getDisplayMetrics().widthPixels - chatHead.getWidth();
			if (chatHeadParams.y < 0)
				chatHeadParams.y = 0;
			if (chatHeadParams.y > context.getResources().getDisplayMetrics().heightPixels - chatHead.getHeight())
				chatHeadParams.y = context.getResources().getDisplayMetrics().heightPixels - chatHead.getHeight();
			// safe check
			try
			{
				windowManager.updateViewLayout(chatHead, chatHeadParams);
			}
			catch (Exception e)
			{
				Logger.d(TAG, "action move chat head");
			}
			chatHead.getLocationOnScreen(chatHeadLocations);
			closeHead.getLocationOnScreen(closeHeadLocations);

			rectChatHead = new Rect(chatHeadLocations[0] - RECT_CONST_DP, chatHeadLocations[1] - RECT_CONST_DP, chatHeadLocations[0] + chatHead.getWidth() + RECT_CONST_DP,
					chatHeadLocations[1] + chatHead.getHeight() + RECT_CONST_DP);
			rectCloseHead = new Rect(closeHeadLocations[0] - RECT_CONST_DP, closeHeadLocations[1] - RECT_CONST_DP, closeHeadLocations[0] + closeHead.getWidth() + RECT_CONST_DP,
					closeHeadLocations[1] + closeHead.getHeight() + RECT_CONST_DP);

			if (Rect.intersects(rectChatHead, rectCloseHead))
			{
				closeHead.setImageResource(R.drawable.close_chat_head_big);
			}
			else
			{
				closeHead.setImageResource(R.drawable.close_chat_head);
			}
		}
		return;
	}

	private void createAndOpenChatHeadPickerLayout(Context context)
	{
		// adding try catch as a safe check
		try
		{
			if (ChatHeadLayout.getOverlayView() == null || !ChatHeadLayout.getOverlayView().isShown())
			{
				try
				{
					windowManager.addView(ChatHeadLayout.attachPicker(context), stickerPickerParams);
				}
				catch (Exception e)
				{
					Logger.d(TAG, "cretae and open sticker picker layout");
				}
				ChatHeadLayout.getOverlayView().setOnTouchListener(new View.OnTouchListener()
				{

					@Override
					public boolean onTouch(View v, MotionEvent event)
					{
						resetPosition(ChatHeadConstants.FINISHING_CHAT_HEAD_ACTIVITY_ANIMATION, null);
						return false;
					}
				});
				ChatHeadLayout.getOverlayView().setOnKeyListener(new View.OnKeyListener()
				{

					@Override
					public boolean onKey(View v, int keyCode, KeyEvent event)
					{
						resetPosition(ChatHeadConstants.FINISHING_CHAT_HEAD_ACTIVITY_ANIMATION, null);
						return false;
					}
				});
			}
		}
		catch (Exception e)
		{
			Logger.d(TAG, "create and open chat head picker layout");
		}
	}

	public void resetPosition(int flag, String path)
	{
		int halfWidthDiff = (context.getResources().getDisplayMetrics().widthPixels - chatHead.getWidth()) / 2;
		overlayAnimation(chatHead, chatHeadParams.x, savedPosX <= halfWidthDiff ? 0 : halfWidthDiff * 2, chatHeadParams.y, savedPosY, flag, path);
	}

	public void onConfigChanged()
	{
		Log.d(TAG, "onConfigurationChanged");

		int[] chatHeadLocations = new int[2];

		try
		{
			windowManager.removeView(ChatHeadLayout.detachPicker(context.getApplicationContext()));
		}
		catch (Exception e)
		{
			Logger.d(TAG, "removing chathead windowmanager view");
		}
		chatHead.getLocationOnScreen(chatHeadLocations);
		if (chatHeadLocations[0] <= (int) ((context.getResources().getDisplayMetrics().widthPixels - chatHead.getWidth()) / 2))
		{
			chatHeadParams.x = 0;
			chatHeadParams.y = chatHeadLocations[1];
		}
		else
		{
			chatHeadParams.x = context.getResources().getDisplayMetrics().widthPixels - chatHead.getWidth();
			chatHeadParams.y = chatHeadLocations[1];
		}
		try
		{
			windowManager.updateViewLayout(chatHead, chatHeadParams);
		}
		catch (Exception e)
		{
			Logger.d(TAG, "configuration changed uodate view");
		}
		if (closeHead.isShown())
		{
			try
			{
				windowManager.removeView(closeHead);
			}
			catch (Exception e)
			{
				Logger.d(TAG, "removing close head");
			}
		}
	}

	public void actionWindowChange(	Set<String> foregroundPackages, boolean isAccessibilityBeingUsed)
	{
		UserLogInfo.recordSessionInfo(foregroundPackages, UserLogInfo.OPERATE);
		if (ChatHeadUtils.shouldRunChatHeadServiceForStickey() && !ChatHeadUtils.accessibilityMustBeActivated(isAccessibilityBeingUsed))
		{
			boolean isSharableAppActive = false;
			for (String sharablePackageName : sharablePackageList)
			{
				isSharableAppActive = foregroundPackages.contains(sharablePackageName);
				/*
				 * whitelisted app is in foreground and the recent package is not equals to the last package, when the whitelisted app is not in foreground and the recent package
				 * is equal to foreground package means while checking the other whitelisted apps it should not go true
				 */
				if (!showStickeyOnActiveApps && (isSharableAppActive ^ sharablePackageName.equals(sharableActivePackage)))
				{
					showStickeyOnActiveApps = true;
				}

				if (isSharableAppActive && showStickeyOnActiveApps)
				{
					foregroundAppName = sharablePackageMap.get(sharablePackageName);
					sharableActivePackage = sharablePackageName;
					break;
				}

			}

			if (!chatHead.isShown())
			{
				if (isSharableAppActive && showStickeyOnActiveApps)
				{
					chatHead.post(new Runnable()
					{
						@Override
						public void run()
						{
							chatHead.setVisibility(View.VISIBLE);
						}
					});
				}
			}
			else
			{
				if (!isSharableAppActive)
				{
					chatHead.post(new Runnable()
					{
						@Override
						public void run()
						{
							chatHead.setVisibility(View.INVISIBLE);
							resetPosition(ChatHeadConstants.FINISHING_CHAT_HEAD_ACTIVITY_ANIMATION, null);
						}
					});
				}
			}

		}

	}

}
