package com.bsb.hike.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.snowfall.SnowFallView;
import com.bsb.hike.ui.HomeActivity;

public class FestivePopup
{	
	private static final String TAG = "FestivePopup";

	public static SnowFallView snowFallView; 
	
	public static final int HOLI_POPUP = 4;

	public static SnowFallView startAndSetSnowFallView(final HomeActivity activity, final int popupType, 
			final boolean toShowAnimation)
	{
		if (activity == null)
		{
			return null;
		}

		if(popupType !=  HOLI_POPUP)
		{
			return null;
		}
		activity.findViewById(R.id.chat_bg_ftue_fade).setVisibility(View.VISIBLE);
		Handler animHandler = new Handler();
		AlphaAnimation alphaAnim = new AlphaAnimation(0.2f, 1f);
		alphaAnim.setFillAfter(true);

		if (toShowAnimation && ((int) Utils.densityMultiplier * 100) >= 100)
		{
			alphaAnim.setDuration(1400);
			activity.findViewById(R.id.chat_bg_ftue_fade).startAnimation(alphaAnim); // dim
			RelativeLayout layout = (RelativeLayout) activity.findViewById(R.id.parent_layout);
			
			snowFallView = new SnowFallView(activity, true);
			snowFallView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			snowFallView.setVisibility(View.GONE);
			layout.addView(snowFallView, 4);

			animHandler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					setupFestivePopup(activity, popupType);
				}
			}, 2800);

			animHandler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					snowFallView.setVisibility(View.VISIBLE);
					AlphaAnimation alphaAnim = new AlphaAnimation(0.1f, 1f);
					AccelerateInterpolator accInterpolator = new AccelerateInterpolator(1f);
					alphaAnim.setInterpolator(accInterpolator);
					alphaAnim.setDuration(600);
					alphaAnim.setFillAfter(true);
					snowFallView.startAnimation(alphaAnim);
				}
			}, 1200);
			return snowFallView;
		}
		else
		{
			alphaAnim.setDuration(1800);
			activity.findViewById(R.id.chat_bg_ftue_fade).startAnimation(alphaAnim); // dim
			animHandler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					setupFestivePopup(activity, popupType);
				}
			}, 2000);
			return null;
		}

	}

	public static void setupFestivePopup(final HomeActivity activity, final int popupType)
	{
		activity.findViewById(R.id.festive_popup_parent).setVisibility(View.VISIBLE);

		addFallAnimation(activity, activity.findViewById(R.id.festive_popup_parent), activity.findViewById(R.id.sticker_popup_image));

		activity.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				stopFestiveAnimAndPopup(activity);
				
				try
				{
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.FESTIVE_POPUP_WISH);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				}
				catch(JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}

				new Handler().postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						stopFestiveAnimAndPopup(activity);
					}
				}, 500);
				try
				{
					JSONObject metadata = new JSONObject();
					metadata.put(AnalyticsConstants.EVENT_KEY, HikeConstants.LogEvent.FESTIVE_POPUP_WISH);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);					
				}
				catch(JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}
				Intent intent = IntentManager.getForwardStickerIntent(activity, getStickerId(popupType), getCatId(popupType));
				intent.putExtra(HikeConstants.Extras.SELECT_ALL_INITIALLY, true);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				activity.startActivity(intent);
			}
		});
		activity.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try
				{
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.FESTIVE_POPUP_DISMISS);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				}
				catch(JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}
				stopFestiveAnimAndPopup(activity);
			}
		});
	}

	private static String getStickerId(int popupType)
	{
		return "027_holi.png";
	}

	private static String getCatId(int popupType)
	{
		return "festive";
	}

	private static void addMoveUpAnimation(View view)
	{
		Animation anim = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 0,
		                                        Animation.RELATIVE_TO_PARENT, 1, Animation.RELATIVE_TO_PARENT, 0);
		anim.setDuration(1600);
		view.startAnimation(anim);
	}

	private static void addFallAnimation(final Activity activity, View view, final View stickerView)
	{
		Handler animHandler = new Handler();

		AnimationSet boxFallAnimSet = new AnimationSet(true);
		boxFallAnimSet.setInterpolator(new DecelerateInterpolator(1.5f));

		AlphaAnimation aa1 = new AlphaAnimation(0f, 1f);
		aa1.setDuration(350);
		boxFallAnimSet.addAnimation(aa1);

		Animation boxFallAnim = new TranslateAnimation(0, 0, -400 * Utils.scaledDensityMultiplier, 0);
		boxFallAnim.setDuration(900);
		boxFallAnimSet.addAnimation(boxFallAnim);

		view.startAnimation(boxFallAnimSet);
		animHandler.postDelayed(new Runnable()
		{

			@Override
			public void run()
			{
				stickerView.setVisibility(View.VISIBLE);

				AnimationSet animSet = (AnimationSet) AnimationUtils.loadAnimation(activity, R.anim.scale_out_rotate);
				stickerView.startAnimation(animSet);
			}
		}, 700);
	}
	
	public static void stopFestiveAnimAndPopup(final HomeActivity activity)
	{
		if(activity != null)
		{
			// Removing overlay
			activity.findViewById(R.id.chat_bg_ftue_fade).clearAnimation();
			activity.findViewById(R.id.chat_bg_ftue_fade).setVisibility(View.GONE);

			// Removing popup
			activity.findViewById(R.id.festive_view).setVisibility(View.GONE);

			// Removing snowfall anim
			if(snowFallView!=null)
			{
				snowFallView.clearAnimation();
				snowFallView.setVisibility(View.GONE);
			}

			activity.showActionBarAfterFestivePopup();
		}

		HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.SHOW_FESTIVE_POPUP);
	}

	public static boolean isPastFestiveDate(int type)
	{
		String holiDate = "2015-03-06";

		Date currentDate, festiveDate;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String current = sdf.format(Calendar.getInstance().getTime());

		try
		{
			festiveDate = sdf.parse(holiDate);
			currentDate = sdf.parse(current);

			if(currentDate.after(festiveDate))
	        {
				return true;
	        }
		}
		catch (ParseException e)
		{
			Logger.d(TAG, "Error parsing date : " + e);
		}
        return false;
	}
}
