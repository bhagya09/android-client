package com.bsb.hike.ui;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Window;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.photos.HikePhotosListener;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.photos.HikePhotosUtils.MenuType;
import com.bsb.hike.photos.views.CanvasImageView.OnDoodleStateChangeListener;
import com.bsb.hike.photos.views.DoodleEffectItemLinearLayout;
import com.bsb.hike.photos.views.FilterEffectItemLinearLayout;
import com.bsb.hike.photos.views.PhotosEditerFrameLayoutView;
import com.bsb.hike.ui.fragments.PhotoActionsFragment;
import com.bsb.hike.ui.fragments.PhotoActionsFragment.ActionListener;
import com.bsb.hike.ui.fragments.PreviewFragment;
import com.bsb.hike.ui.fragments.ProfilePicFragment;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Utils;
import com.viewpagerindicator.IconPagerAdapter;
import com.viewpagerindicator.PhotosTabPageIndicator;

public class PictureEditer extends HikeAppStateBaseFragmentActivity
{

	PhotosEditerFrameLayoutView editView;

	private int menuIcons[] = { R.drawable.photos_tabs_filter_selector, R.drawable.photos_tabs_doodle_selector };

	private EditorClickListener clickHandler;

	private ImageView undoButton;

	private PhotoActionsFragment mPhotosActionsFragment;

	private String filename;

	private View mActionBarDoneContainer;

	private PhotosTabPageIndicator indicator;

	private ViewPager pager;

	private View mActionBarBackButton;

	private View overlayFrame;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		overridePendingTransition(R.anim.fade_in_animation, R.anim.fade_out_animation);

		super.onCreate(savedInstanceState);

		getWindow().requestFeature((int) Window.FEATURE_ACTION_BAR_OVERLAY);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.fragment_picture_editer);

		editView = (PhotosEditerFrameLayoutView) findViewById(R.id.editer);

		clickHandler = new EditorClickListener(this);

		Intent intent = getIntent();
		filename = intent.getStringExtra(HikeConstants.HikePhotos.FILENAME);
		if (filename == null)
		{
			PictureEditer.this.finish();
			return;
		}

		editView = (PhotosEditerFrameLayoutView) findViewById(R.id.editer);
		editView.loadImageFromFile(filename);
		editView.setOnDoodlingStartListener(clickHandler);

		FragmentPagerAdapter adapter = new PhotoEditViewPagerAdapter(getSupportFragmentManager());

		pager = (ViewPager) findViewById(R.id.pager);
		pager.setAdapter(adapter);

		indicator = (PhotosTabPageIndicator) findViewById(R.id.indicator);
		indicator.setViewPager(pager);

		int density = getResources().getDisplayMetrics().densityDpi;

		switch (density)
		{
		case DisplayMetrics.DENSITY_LOW:
		case DisplayMetrics.DENSITY_MEDIUM:
			findViewById(R.id.indicatorView).setVisibility(View.GONE);
			break;

		}

		undoButton = (ImageView) findViewById(R.id.undo);
		undoButton.setOnClickListener(clickHandler);

		indicator.setOnPageChangeListener(clickHandler);

		setupActionBar();

		overlayFrame = findViewById(R.id.overlayFrame);

		try
		{
			new File(filename).delete();
		}
		catch (NullPointerException npe)
		{
			npe.printStackTrace();
		}
	}

	@Override
	protected void onResume()
	{
		overridePendingTransition(R.anim.fade_in_animation, R.anim.fade_out_animation);
		super.onResume();
		getSupportActionBar().getCustomView().findViewById(R.id.done_container).setVisibility(View.VISIBLE);
		editView.enable();
	}

	@Override
	protected void onPause()
	{
		overridePendingTransition(R.anim.fade_in_animation, R.anim.fade_out_animation);
		super.onPause();
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.photos_action_bar, null);
		mActionBarBackButton = actionBarView.findViewById(R.id.back);
		mActionBarBackButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});

		mActionBarDoneContainer = actionBarView.findViewById(R.id.done_container);

		mActionBarDoneContainer.setOnClickListener(clickHandler);

		actionBar.setCustomView(actionBarView);
	}

	public class PhotoEditViewPagerAdapter extends FragmentPagerAdapter implements IconPagerAdapter
	{

		public PhotoEditViewPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public Fragment getItem(int position)
		{
			PreviewFragment prevFragment = null;
			switch (position)
			{
			case HikeConstants.HikePhotos.FILTER_FRAGMENT_ID:
				prevFragment = PreviewFragment.newInstance(MenuType.EFFECTS_TYPE, editView.getScaledImageOriginal());
				break;
			case HikeConstants.HikePhotos.DOODLE_FRAGMENT_ID:
				prevFragment = PreviewFragment.newInstance(MenuType.DOODLE_TYPE, editView.getScaledImageOriginal());
				break;
			}
			return prevFragment;
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			return HikeConstants.HikePhotos.EMPTY_TAB_TITLE;
		}

		@Override
		public int getIconResId(int index)
		{
			return menuIcons[index];
		}

		@Override
		public int getCount()
		{
			return menuIcons.length;
		}
	}

	public class EditorClickListener implements OnClickListener, OnPageChangeListener, OnDoodleStateChangeListener
	{
		private DoodleEffectItemLinearLayout doodlePreview;

		private int doodleWidth;

		private Context mContext;

		private boolean doodleState;

		public EditorClickListener(Context context)
		{
			mContext = context;
			doodleWidth = HikeConstants.HikePhotos.DEFAULT_BRUSH_WIDTH;
			doodleState = false;
		}

		public void setDoodlePreview(DoodleEffectItemLinearLayout view)
		{
			doodlePreview = view;
		}

		@Override
		public void onClick(View v)
		{
			if (v.getClass() == FilterEffectItemLinearLayout.class)
			{
				FilterEffectItemLinearLayout prev = HikePhotosUtils.FilterTools.getCurrentFilterItem();
				FilterEffectItemLinearLayout me = (FilterEffectItemLinearLayout) v;
				editView.applyFilter(me.getFilter());
				if (prev != null && prev.getFilter() != me.getFilter())
				{
					prev.unSelect();
				}
				me.select();
			}
			else if (v.getClass() == DoodleEffectItemLinearLayout.class)
			{
				DoodleEffectItemLinearLayout prev = HikePhotosUtils.FilterTools.getCurrentDoodleItem();
				DoodleEffectItemLinearLayout me = (DoodleEffectItemLinearLayout) v;
				editView.setBrushColor(me.getBrushColor());
				doodlePreview.setBrushColor(me.getBrushColor());
				doodlePreview.refresh();
				if (prev != null && prev.getBrushColor() != me.getBrushColor())
				{
					prev.unSelect();
				}
				me.select();
			}
			else
			{
				switch (v.getId())
				{
				case R.id.plusWidth:
					if (doodleWidth + HikeConstants.HikePhotos.DELTA_BRUSH_WIDTH <= HikeConstants.HikePhotos.MAX_BRUSH_WIDTH)
					{
						doodleWidth += HikeConstants.HikePhotos.DELTA_BRUSH_WIDTH;
					}
					doodlePreview.setBrushWidth(HikePhotosUtils.dpToPx(mContext, doodleWidth));
					doodlePreview.refresh();
					editView.setBrushWidth(HikePhotosUtils.dpToPx(mContext, doodleWidth));
					break;
				case R.id.minusWidth:
					if (doodleWidth - HikeConstants.HikePhotos.DELTA_BRUSH_WIDTH >= HikeConstants.HikePhotos.Min_BRUSH_WIDTH)
					{
						doodleWidth -= HikeConstants.HikePhotos.DELTA_BRUSH_WIDTH;
					}
					doodlePreview.setBrushWidth(HikePhotosUtils.dpToPx(mContext, doodleWidth));
					doodlePreview.refresh();
					editView.setBrushWidth(HikePhotosUtils.dpToPx(mContext, doodleWidth));
					break;
				case R.id.undo:
					editView.undoLastDoodleDraw();
					break;
				case R.id.done_container:
					if (mPhotosActionsFragment == null)
					{
						mPhotosActionsFragment = new PhotoActionsFragment(new ActionListener()
						{
							@Override
							public void onAction(int actionCode)
							{
								getSupportFragmentManager().popBackStackImmediate();
								if (actionCode == PhotoActionsFragment.ACTION_SEND)
								{
									sendAnalyticsSendTo();
									editView.saveImage(HikeFileType.IMAGE, null, new HikePhotosListener()
									{
										@Override
										public void onFailure()
										{
											// Do nothing
										}

										@Override
										public void onComplete(Bitmap bmp)
										{
											// Do nothing
										}

										@Override
										public void onComplete(File f)
										{
											Intent forwardIntent = IntentFactory.getForwardImageIntent(mContext, f);
											forwardIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
											startActivity(forwardIntent);
										}
									});
								}
								else if (actionCode == PhotoActionsFragment.ACTION_SET_DP)
								{

									sendAnalyticsSetAsDp();
									// User info is saved in shared preferences
									SharedPreferences preferences = HikeMessengerApp.getInstance().getApplicationContext()
											.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE);
									ContactInfo userInfo = Utils.getUserContactInfo(preferences);
									String mLocalMSISDN = userInfo.getMsisdn();
									editView.saveImage(HikeFileType.PROFILE, mLocalMSISDN, new HikePhotosListener()
									{
										@Override
										public void onFailure()
										{
											// Do nothing
										}

										@Override
										public void onComplete(Bitmap bmp)
										{
											// Do nothing
										}

										@Override
										public void onComplete(final File f)
										{
											new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
											{
												@Override
												public void run()
												{
													editView.setVisibility(View.VISIBLE);
													mActionBarBackButton.setVisibility(View.GONE);
													ProfilePicFragment profilePicFragment = new ProfilePicFragment();
													Bundle b = new Bundle();
													b.putString(HikeConstants.HikePhotos.FILENAME, f.getAbsolutePath());
													profilePicFragment.setArguments(b);
													getSupportFragmentManager()
															.beginTransaction()
															.setCustomAnimations(R.anim.fade_in_animation, R.anim.fade_out_animation, R.anim.fade_in_animation,
																	R.anim.fade_out_animation).replace(R.id.overlayFrame, profilePicFragment).addToBackStack(null).commit();
												}
											}, 600);
										}
									});

								}
							}
						});
					}

					editView.disable();

					// Change fragment
					getSupportFragmentManager().beginTransaction()
							.setCustomAnimations(R.anim.photo_option_in, R.anim.photo_option_out, R.anim.photo_option_in, R.anim.photo_option_out)
							.replace(R.id.overlayFrame, mPhotosActionsFragment).addToBackStack(null).commit();

					mActionBarDoneContainer.setVisibility(View.INVISIBLE);

					overlayFrame.setVisibility(View.VISIBLE);

					break;
				}
			}
		}

		@Override
		public void onPageScrollStateChanged(int arg0)
		{
			// Do nothing

		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2)
		{
			// Do nothing
		}

		@Override
		public void onPageSelected(int arg0)
		{

			switch (arg0)
			{
			case HikeConstants.HikePhotos.FILTER_FRAGMENT_ID:
				editView.disableDoodling();
				undoButton.setVisibility(View.GONE);
				break;
			case HikeConstants.HikePhotos.DOODLE_FRAGMENT_ID:
				editView.enableDoodling();
				if (doodleState)
				{
					undoButton.setVisibility(View.VISIBLE);
				}
				break;
			}

		}

		@Override
		public void onDoodleStateChanged(boolean isCanvasEmpty)
		{
			doodleState = !isCanvasEmpty;
			if (isCanvasEmpty)
			{
				undoButton.setVisibility(View.GONE);
			}
			else
			{
				undoButton.setVisibility(View.VISIBLE);
			}
		}

	}

	@Override
	public void onBackPressed()
	{
		if (getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE))
		{
			getSupportActionBar().show();
			mActionBarDoneContainer.setVisibility(View.VISIBLE);
			mActionBarBackButton.setVisibility(View.VISIBLE);
			editView.enable();
		}
		else
		{
			// Tear down
			super.onBackPressed();
			HikePhotosUtils.FilterTools.setCurrentDoodleItem(null);
			HikePhotosUtils.FilterTools.setCurrentFilterItem(null);
			if (filename != null)
			{
				File editFile = new File(filename);
				editFile.delete();
			}
		}
	}

	private void sendAnalyticsSetAsDp()
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.EVENT_KEY, HikeConstants.LogEvent.PHOTOS_SET_AS_DP);
			HikeAnalyticsEvent.analyticsForPhotos(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private void sendAnalyticsSendTo()
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.EVENT_KEY, HikeConstants.LogEvent.PHOTOS_SEND_TO);
			HikeAnalyticsEvent.analyticsForPhotos(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public EditorClickListener getClickHandler()
	{
		return clickHandler;
	}

}
