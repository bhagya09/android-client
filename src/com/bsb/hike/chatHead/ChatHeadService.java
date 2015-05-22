package com.bsb.hike.chatHead;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.animation.Animator.AnimatorListener;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.ShareUtils;
import com.bsb.hike.chatHead.ChatHeadActivity;

public class ChatHeadService extends Service
{

	private class CheckForegroundActivity extends TimerTask
	{
		String getActivePackage()
		{
			ActivityManager mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
			List<ActivityManager.RunningAppProcessInfo> processInfos = mActivityManager.getRunningAppProcesses();
			for (ActivityManager.RunningAppProcessInfo processInfo : processInfos)
			{
				if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && processInfo.importanceReasonCode == 0)
				{
					return processInfos.get(0).processName;
				}
			}
			return null;
		}

		public void appendLog(String text)
		{
			File logFile = new File("sdcard/log.file");
			if (!logFile.exists())
			{
				try
				{
					logFile.createNewFile();
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try
			{
				// BufferedWriter for performance, true to set append to file flag
				BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
				buf.append(text);
				buf.newLine();
				buf.close();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void run()
		{
			String foregroundPackage = getActivePackage();
			Log.d("ashish", foregroundPackage);
			appendLog(Calendar.getInstance().getTime().toString());
			Logger.d("ashish", Calendar.getInstance().getTime().toString());

			if (!chatHead.isShown())
			{
				switch (foregroundPackage)
				{
				case HikeConstants.Extras.WHATSAPP_PACKAGE:
				case HikeConstants.Extras.LINE_PACKAGE:
				case HikeConstants.Extras.VIBER_PACKAGE:
				case HikeConstants.Extras.WECHAT_PACKAGE:
				case HikeConstants.Extras.TELEGRAM_PACKAGE:
				case HikeConstants.Extras.FACEBOOK_PACKAGE:
					if (toShow)
					{
						foregroundApp = foregroundPackage;
						chatHead.post(new Runnable()
						{
							@Override
							public void run()
							{
								chatHead.setVisibility(View.VISIBLE);
							}
						});
					}
					break;
				default:
					toShow = true;
				}
			}
			else
			{
				if (!foregroundPackage.equals(HikeConstants.Extras.FACEBOOK_PACKAGE) && !foregroundPackage.equals(HikeConstants.Extras.VIBER_PACKAGE)
						&& !foregroundPackage.equals(HikeConstants.Extras.WECHAT_PACKAGE) && !foregroundPackage.equals(HikeConstants.Extras.TELEGRAM_PACKAGE)
						&& !foregroundPackage.equals(HikeConstants.Extras.WHATSAPP_PACKAGE) && !foregroundPackage.equals(HikeConstants.Extras.LINE_PACKAGE) && !flagActivityRunning)
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

		}

	}

	String foregroundApp = HikeConstants.Extras.HIKE_PACKAGE;

	WindowManager windowManager;

	static ChatHeadService instance;

	ImageView chatHead, closeHead;

	int savedPosX = HikeConstants.ChatHeadService.INITIAL_POS_X;

	int savedPosY = HikeConstants.ChatHeadService.INITIAL_POS_Y;

	static int dismissed = 0;

	static boolean toShow = true;

	public static boolean flagActivityRunning = false;

	WindowManager.LayoutParams chatHeadParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

	WindowManager.LayoutParams closeHeadParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

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

		translateX.setDuration(HikeConstants.ChatHeadService.ANIMATION_TIME);
		translateY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
		{
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator)
			{
				chatHeadParams.y = (Integer) valueAnimator.getAnimatedValue();
				windowManager.updateViewLayout(view2animate, chatHeadParams);

			}
		});

		translateY.setDuration(HikeConstants.ChatHeadService.ANIMATION_TIME);
		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.playTogether(translateX, translateY);
		animatorSet.start();
		animatorSet.addListener(new AnimatorListener()
		{

			@Override
			public void onAnimationStart(Animator animation)
			{
			}

			@Override
			public void onAnimationRepeat(Animator animation)
			{
			}

			@Override
			public void onAnimationEnd(Animator animation)
			{
				if (flag == HikeConstants.ChatHeadService.CREATING_CHAT_HEAD_ACTIVITY_ANIMATION)
				{
					flagActivityRunning = true;
					Intent intent = new Intent(getApplicationContext(), ChatHeadActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					startActivity(intent);
					ChatHeadActivity.flagActivity = true;
				}
				else if (flag == HikeConstants.ChatHeadService.FINISHING_CHAT_HEAD_ACTIVITY_ANIMATION)
				{
					ChatHeadActivity.getInstance().finish();
					ChatHeadActivity.getInstance().overridePendingTransition(0, 0);
				}
				else if (flag == HikeConstants.ChatHeadService.SHARING_BEFORE_FINISHING_ANIMATION)
				{
					Intent intent = ShareUtils.shareContent(HikeConstants.Extras.ShareTypes.STICKER_SHARE, path[0], foregroundApp);
					if (intent != null)
					{
						startActivity(intent);
					}
					ChatHeadActivity.getInstance().finish();
					ChatHeadActivity.getInstance().overridePendingTransition(0, 0);
				}
			}

			@Override
			public void onAnimationCancel(Animator animation)
			{
			}
		});

	}

	void setChatHeadParams()
	{
		chatHeadParams.x = HikeConstants.ChatHeadService.INITIAL_POS_X;
		chatHeadParams.y = HikeConstants.ChatHeadService.INITIAL_POS_Y;
		chatHeadParams.gravity = Gravity.TOP | Gravity.LEFT;
	}

	void setCloseHeadParams()
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
		rectChatHead = new Rect(chatHeadLocations[0] - HikeConstants.ChatHeadService.RECT_CONST, chatHeadLocations[1] - HikeConstants.ChatHeadService.RECT_CONST,
				chatHeadLocations[0] + chatHead.getWidth() + HikeConstants.ChatHeadService.RECT_CONST, chatHeadLocations[1] + chatHead.getHeight()
						+ HikeConstants.ChatHeadService.RECT_CONST);
		rectCloseHead = new Rect(closeHeadLocations[0] - HikeConstants.ChatHeadService.RECT_CONST, closeHeadLocations[1] - HikeConstants.ChatHeadService.RECT_CONST,
				closeHeadLocations[0] + closeHead.getWidth() + HikeConstants.ChatHeadService.RECT_CONST, closeHeadLocations[1] + closeHead.getHeight()
						+ HikeConstants.ChatHeadService.RECT_CONST);
		if (Rect.intersects(rectChatHead, rectCloseHead))
		{
			dismissed++;
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
			if (drag < HikeConstants.ChatHeadService.DRAG_CONST)
			{
				if (!flagActivityRunning)
				{
					final float scale = getApplicationContext().getResources().getDisplayMetrics().density;

					Resources resources = getApplicationContext().getResources();
					int resourceId = getResources().getIdentifier(HikeConstants.ChatHeadService.STATUS_BAR_HEIGHT, HikeConstants.ChatHeadService.STATUS_BAR_TYPE,
							HikeConstants.ChatHeadService.STATUS_BAR_PACKAGE);
					int status_bar_height = (int) resources.getDimension(resourceId);
					int pixelsX;
					if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
					{
						// 12 dp margin + 12 dp left part of image
						pixelsX = (int) (24 * scale);
					}
					else
					{
						pixelsX = (int) (24 * scale) + status_bar_height;

					}

					// 240 dp image height + 8dp image margin bottom + 42 dp size of icon
					int pixelsY = (int) (displaymetrics.heightPixels - (scale * 248) - chatHead.getHeight() - status_bar_height);

					savedPosX = chatHeadParams.x;
					savedPosY = chatHeadParams.y;
					overlayAnimation(chatHead, chatHeadParams.x, pixelsX, chatHeadParams.y, pixelsY, HikeConstants.ChatHeadService.CREATING_CHAT_HEAD_ACTIVITY_ANIMATION);
				}
				else
				{
					resetPosition(HikeConstants.ChatHeadService.FINISHING_CHAT_HEAD_ACTIVITY_ANIMATION);
				}
			}
			else
			{
				if (chatHeadLocations[0] <= (int) ((displaymetrics.widthPixels - chatHead.getWidth()) / 2))
				{
					overlayAnimation(chatHead, chatHeadParams.x, 0, chatHeadParams.y, chatHeadParams.y, HikeConstants.ChatHeadService.REMAINING_ANIMATION);
				}
				else
				{
					overlayAnimation(chatHead, chatHeadParams.x, displaymetrics.widthPixels - chatHead.getWidth(), chatHeadParams.y, chatHeadParams.y,
							HikeConstants.ChatHeadService.REMAINING_ANIMATION);
				}
			}
		}
		if (closeHead.isShown())
		{
			windowManager.removeView(closeHead);
		}
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

			rectChatHead = new Rect(chatHeadLocations[0] - HikeConstants.ChatHeadService.RECT_CONST, chatHeadLocations[1] - HikeConstants.ChatHeadService.RECT_CONST,
					chatHeadLocations[0] + chatHead.getWidth() + HikeConstants.ChatHeadService.RECT_CONST, chatHeadLocations[1] + chatHead.getHeight()
							+ HikeConstants.ChatHeadService.RECT_CONST);
			rectCloseHead = new Rect(closeHeadLocations[0] - HikeConstants.ChatHeadService.RECT_CONST, closeHeadLocations[1] - HikeConstants.ChatHeadService.RECT_CONST,
					closeHeadLocations[0] + closeHead.getWidth() + HikeConstants.ChatHeadService.RECT_CONST, closeHeadLocations[1] + closeHead.getHeight()
							+ HikeConstants.ChatHeadService.RECT_CONST);

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
		Logger.d("ashish", "on start command");
		return Service.START_STICKY;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		Logger.d("ashish", "create");

		instance = this;

		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		chatHead = new ImageView(this);

		chatHead.setImageResource(R.drawable.sticker_chat_head);

		setChatHeadParams();

		setCloseHeadParams();

		Timer processCheckTimer = new Timer();

		processCheckTimer.schedule(new CheckForegroundActivity(), 0L, 1000L);

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
		Logger.d("ashish", "destroy");
		super.onDestroy();

		if (chatHead.isShown())
			windowManager.removeView(chatHead);
		if (closeHead.isShown())
			windowManager.removeView(closeHead);
		if (flagActivityRunning)
		{
			ChatHeadActivity.getInstance().finish();
		}

	}

}