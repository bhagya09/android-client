package com.bsb.hike.chatHead;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.animation.Animator.AnimatorListener;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.ShareUtils;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.chatHead.ChatHeadActivity;
import com.bsb.hike.chatthread.ChatThreadUtils;

public class ChatHeadService extends Service
{
	private static final int ANIMATION_TIME = 300;

	private static final int RECT_CONST = 10;

	private static final int DRAG_CONST = 3;

	private static final int INITIAL_POS_X = 0;

	private static final int INITIAL_POS_Y = 0;

	private static List<String> list = new ArrayList<String>();

	private static HashMap<String, String> hashMap = new HashMap<>();

	public static String foregroundApp = HikeConstants.Extras.WHATSAPP_PACKAGE;

	private WindowManager windowManager;

	private static ChatHeadService instance;

	private ImageView chatHead, closeHead;

	private int savedPosX = INITIAL_POS_X;

	private int savedPosY = INITIAL_POS_Y;

	// boolean to show whether the chat head must be shown or not for a particular session
	private static boolean toShow = true;

	private WindowManager.LayoutParams chatHeadParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

	private WindowManager.LayoutParams closeHeadParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

	public static boolean flagActivityRunning = false;

	public static String foregroundAppName = HikeConstants.Extras.WHATSAPP_PACKAGE;

	public static int dismissed = 0;

	public static FinishActivityListener mFinishActivityListener;

	private final Handler chatHeadHandler = new Handler();

	Runnable chatHeadRunnable = new Runnable()
	{

		@Override
		public void run()
		{
			boolean whiteListAppForegrounded = false;
			Set<String> foregroundPackages = ChatHeadUtils.getForegroundedPackages();

			for (String packName : list)
			{
				whiteListAppForegrounded = foregroundPackages.contains(packName);
				/* whitelisted app is in foreground and the recent package is not equals to the last package, when the whitelisted app is not in foreground */
				if (!toShow && (whiteListAppForegrounded && !packName.equals(foregroundApp)) || (!whiteListAppForegrounded))
				{
					toShow = true;
				}

				if (whiteListAppForegrounded && toShow)
				{
					foregroundAppName = hashMap.get(packName);
					break;
				}

			}

			if (!chatHead.isShown())
			{
				if ((whiteListAppForegrounded && toShow) || flagActivityRunning)
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
				if (!whiteListAppForegrounded && !flagActivityRunning)
				{
					chatHead.post(new Runnable()
					{
						@Override
						public void run()
						{
							chatHead.setVisibility(View.INVISIBLE);
						}
					});
				}
			}
			chatHeadHandler.postDelayed(this, 1000L);
		}
	};

	private static void createListfromJson()
	{
		try
		{
			list.clear();
			JSONArray jsonObj = new JSONArray(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.PACKAGE_LIST, ""));
			for (int i = 0; i < jsonObj.length(); i++)
			{
				JSONObject obj = jsonObj.getJSONObject(i);
				{
					if (obj.optBoolean(HikeConstants.ChatHead.APP_ENABLE, false))
					{
						list.add(obj.optString(HikeConstants.ChatHead.PACKAGE_NAME, ""));
						hashMap.put(obj.optString(HikeConstants.ChatHead.PACKAGE_NAME, ""), obj.optString(HikeConstants.ChatHead.APP_NAME, ""));
					}
				}

			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public static void registerReceiver(FinishActivityListener finishingActivityListener)
	{
		mFinishActivityListener = finishingActivityListener;
	}

	public static void unregisterReceiver(FinishActivityListener finishingActivityListener)
	{
		mFinishActivityListener = null;
	}

	public void overlayAnimation(final View view2animate, Integer viewX, Integer endX, Integer viewY, Integer endY, final int flag, final String... path)
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
				windowManager.updateViewLayout(view2animate, chatHeadParams);

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
				if (flag != ChatHeadUtils.REMAINING_ANIMATION && flagActivityRunning && (mFinishActivityListener != null))
				{
					flagActivityRunning = false;
					mFinishActivityListener.finishActivity();
				}
			}

			@Override
			public void onAnimationRepeat(Animator animation)
			{
			}

			@Override
			public void onAnimationEnd(Animator animation)
			{
				Intent intent;
				switch (flag)
				{
				case ChatHeadUtils.CREATING_CHAT_HEAD_ACTIVITY_ANIMATION:
					intent = new Intent(getApplicationContext(), ChatHeadActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
					break;
				case ChatHeadUtils.SHARING_BEFORE_FINISHING_ANIMATION:
					Intent i = ShareUtils.shareContent(HikeConstants.Extras.ShareTypes.STICKER_SHARE, path[0], foregroundApp);
					if (i != null)
					{
						startActivity(i);
					}
					break;
				case ChatHeadUtils.STOPPING_SERVICE_ANIMATION:
					ChatHeadUtils.stopService(getApplicationContext());
					break;
				case ChatHeadUtils.GET_MORE_STICKERS_ANIMATION:
					intent = IntentFactory.getStickerShareIntent(getApplicationContext());
					startActivity(intent);
					ChatHeadService.getInstance().setChatHeadInvisible();
					break;
				case ChatHeadUtils.OPEN_HIKE_ANIMATION:
					IntentFactory.openHomeActivityInOtherTask(getApplicationContext(), true);
					ChatHeadService.getInstance().setChatHeadInvisible();
					break;
				case ChatHeadUtils.STICKER_SHOP_ANIMATION:
					intent = IntentFactory.getStickerShopIntent(getApplicationContext(), true);
					startActivity(intent);
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

	private void setChatHeadParams()
	{
		chatHeadParams.x = INITIAL_POS_X;
		chatHeadParams.y = INITIAL_POS_Y;
		chatHeadParams.gravity = Gravity.TOP | Gravity.LEFT;
	}

	private void setCloseHeadParams()
	{
		closeHead = new ImageView(this);
		closeHead.setImageResource(R.drawable.close_chat_head);
		closeHeadParams.gravity = Gravity.BOTTOM | Gravity.CENTER;
	}

	public void setChatHeadInvisible()
	{
		chatHead.setVisibility(View.INVISIBLE);
	}

	private void actionUp(int drag)
	{

		int[] chatHeadLocations = new int[2];

		int[] closeHeadLocations = new int[2];

		Rect rectChatHead, rectCloseHead;

		DisplayMetrics displaymetrics = new DisplayMetrics();

		windowManager.getDefaultDisplay().getMetrics(displaymetrics);

		chatHead.getLocationOnScreen(chatHeadLocations);
		closeHead.getLocationOnScreen(closeHeadLocations);
		rectChatHead = new Rect(chatHeadLocations[0] - RECT_CONST, chatHeadLocations[1] - RECT_CONST, chatHeadLocations[0] + chatHead.getWidth() + RECT_CONST, chatHeadLocations[1]
				+ chatHead.getHeight() + RECT_CONST);
		rectCloseHead = new Rect(closeHeadLocations[0] - RECT_CONST, closeHeadLocations[1] - RECT_CONST, closeHeadLocations[0] + closeHead.getWidth() + RECT_CONST,
				closeHeadLocations[1] + closeHead.getHeight() + RECT_CONST);
		if (Rect.intersects(rectChatHead, rectCloseHead))
		{
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.STICKER_HEAD_DISMISS, ChatHeadService.foregroundAppName);
			dismissed++;
			if (dismissed <= HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.DISMISS_COUNT, ChatHeadActivity.DISMISS_CONST))
			{
				if (chatHead.isShown())
				{
					ChatHeadService.toShow = false;
					setChatHeadParams();
					windowManager.updateViewLayout(chatHead, chatHeadParams);
					chatHead.setVisibility(View.INVISIBLE);
				}
				if (closeHead.isShown())
				{
					windowManager.removeView(closeHead);
				}
			}
			else
			{
				openingChatHeadActivity();
			}
		}
		else
		{
			if (drag < DRAG_CONST)
			{
				HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.STICKER_HEAD, ChatHeadService.foregroundAppName);
				if (!flagActivityRunning)
				{
					openingChatHeadActivity();
				}
				else
				{
					resetPosition(ChatHeadUtils.FINISHING_CHAT_HEAD_ACTIVITY_ANIMATION);
				}
			}
			else
			{
				if (chatHeadLocations[0] <= (int) ((displaymetrics.widthPixels - chatHead.getWidth()) / 2))
				{
					overlayAnimation(chatHead, chatHeadParams.x, 0, chatHeadParams.y, chatHeadParams.y, ChatHeadUtils.REMAINING_ANIMATION);
				}
				else
				{
					overlayAnimation(chatHead, chatHeadParams.x, displaymetrics.widthPixels - chatHead.getWidth(), chatHeadParams.y, chatHeadParams.y,
							ChatHeadUtils.REMAINING_ANIMATION);
				}
			}
		}
		if (closeHead.isShown())
		{
			windowManager.removeView(closeHead);
		}
	}

	private void openingChatHeadActivity()
	{
		DisplayMetrics displaymetrics = new DisplayMetrics();

		windowManager.getDefaultDisplay().getMetrics(displaymetrics);

		final float scale = Utils.densityMultiplier;
		int status_bar_height = ChatThreadUtils.getStatusBarHeight(this);
		int pixelsX;
		// 8 dp margin + 38 dp left part of image to point - width of icon/2
		pixelsX = (int) ((46 * scale) - (chatHead.getWidth() / 2));
		// 240 dp image height + 8dp image margin bottom + size of icon
		int pixelsY = (int) (displaymetrics.heightPixels - (scale * 248) - chatHead.getHeight() - status_bar_height);
		savedPosX = chatHeadParams.x;
		savedPosY = chatHeadParams.y;
		overlayAnimation(chatHead, chatHeadParams.x, pixelsX, chatHeadParams.y, pixelsY, ChatHeadUtils.CREATING_CHAT_HEAD_ACTIVITY_ANIMATION);

	}

	private int actionMove(int drag, int initialX, int initialY, Float initialTouchX, Float initialTouchY, MotionEvent event)
	{
		int[] chatHeadLocations = new int[2];

		int[] closeHeadLocations = new int[2];

		Rect rectChatHead, rectCloseHead;

		DisplayMetrics displaymetrics = new DisplayMetrics();

		windowManager.getDefaultDisplay().getMetrics(displaymetrics);

		if (!flagActivityRunning)
		{
			drag++;
			chatHeadParams.x = initialX + (int) (event.getRawX() - initialTouchX);
			chatHeadParams.y = initialY + (int) (event.getRawY() - initialTouchY);
			if (chatHeadParams.x < 0)
				chatHeadParams.x = 0;
			if (chatHeadParams.x > displaymetrics.widthPixels - chatHead.getWidth())
				chatHeadParams.x = displaymetrics.widthPixels - chatHead.getWidth();
			if (chatHeadParams.y < 0)
				chatHeadParams.y = 0;
			if (chatHeadParams.y > displaymetrics.heightPixels - chatHead.getHeight())
				chatHeadParams.y = displaymetrics.heightPixels - chatHead.getHeight();
			windowManager.updateViewLayout(chatHead, chatHeadParams);
			chatHead.getLocationOnScreen(chatHeadLocations);
			closeHead.getLocationOnScreen(closeHeadLocations);

			rectChatHead = new Rect(chatHeadLocations[0] - RECT_CONST, chatHeadLocations[1] - RECT_CONST, chatHeadLocations[0] + chatHead.getWidth() + RECT_CONST,
					chatHeadLocations[1] + chatHead.getHeight() + RECT_CONST);
			rectCloseHead = new Rect(closeHeadLocations[0] - RECT_CONST, closeHeadLocations[1] - RECT_CONST, closeHeadLocations[0] + closeHead.getWidth() + RECT_CONST,
					closeHeadLocations[1] + closeHead.getHeight() + RECT_CONST);

			if (Rect.intersects(rectChatHead, rectCloseHead))
			{
				closeHead.setImageResource(R.drawable.close_chat_head_big);
			}
			else
			{
				closeHead.setImageResource(R.drawable.close_chat_head);
			}
		}
		return drag;
	}

	public static ChatHeadService getInstance()
	{
		return instance;
	}

	public void resetPosition(int flag, String... path)
	{
		DisplayMetrics displaymetrics = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(displaymetrics);
		if (savedPosX <= (int) ((displaymetrics.widthPixels - chatHead.getWidth()) / 2))
		{
			if (path.length > 0)
			{
				overlayAnimation(chatHead, chatHeadParams.x, 0, chatHeadParams.y, savedPosY, flag, path[0]);
			}
			else
			{
				overlayAnimation(chatHead, chatHeadParams.x, 0, chatHeadParams.y, savedPosY, flag);
			}

		}
		else
		{
			if (path.length > 0)
			{
				overlayAnimation(chatHead, chatHeadParams.x, displaymetrics.widthPixels - chatHead.getWidth(), chatHeadParams.y, savedPosY, flag, path);
			}
			else
			{
				overlayAnimation(chatHead, chatHeadParams.x, displaymetrics.widthPixels - chatHead.getWidth(), chatHeadParams.y, savedPosY, flag);
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		chatHeadHandler.removeCallbacks(chatHeadRunnable);
		chatHeadHandler.postDelayed(chatHeadRunnable, 1000L);
		return Service.START_STICKY;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		instance = this;

		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		chatHead = new ImageView(this);

		chatHead.setImageResource(R.drawable.sticker_chat_head);

		setChatHeadParams();

		setCloseHeadParams();

		createListfromJson();

		windowManager.addView(chatHead, chatHeadParams);

		chatHead.setVisibility(View.INVISIBLE);

		chatHead.setOnTouchListener(new OnTouchListener()
		{
			Integer drag;

			Integer initialX;

			Integer initialY;

			Float initialTouchX;

			Float initialTouchY;

			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				switch (event.getAction())
				{
				case MotionEvent.ACTION_DOWN:
					drag = 0;
					initialX = chatHeadParams.x;
					initialY = chatHeadParams.y;
					initialTouchX = event.getRawX();
					initialTouchY = event.getRawY();
					windowManager.addView(closeHead, closeHeadParams);
					return true;

				case MotionEvent.ACTION_UP:
					actionUp(drag);
					return true;

				case MotionEvent.ACTION_MOVE:
					drag = actionMove(drag, initialX, initialY, initialTouchX, initialTouchY, event);
					return true;
				}

				return false;
			}
		});

	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		int[] chatHeadLocations = new int[2];
		DisplayMetrics displaymetrics = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(displaymetrics);
		chatHead.getLocationOnScreen(chatHeadLocations);
		if (chatHeadLocations[0] <= (int) ((displaymetrics.widthPixels - chatHead.getWidth()) / 2))
		{
			chatHeadParams.x = 0;
			chatHeadParams.y = chatHeadLocations[1];
		}
		else
		{
			chatHeadParams.x = displaymetrics.widthPixels - chatHead.getWidth();
			chatHeadParams.y = chatHeadLocations[1];
		}
		windowManager.removeView(chatHead);
		windowManager.addView(chatHead, chatHeadParams);
		if (closeHead.isShown())
		{
			windowManager.removeView(closeHead);
		}
	}

	@Override
	public void onDestroy()
	{
		chatHeadHandler.removeCallbacks(chatHeadRunnable);

		if (chatHead.isShown())
			windowManager.removeView(chatHead);
		if (closeHead.isShown())
			windowManager.removeView(closeHead);
		if (flagActivityRunning && (mFinishActivityListener != null))
		{
			flagActivityRunning = false;
			mFinishActivityListener.finishActivity();
		}
		super.onDestroy();
	}

}
