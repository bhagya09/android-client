package com.bsb.hike.ui;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.photos.HikeEffectsFactory;
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
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Utils;
import com.viewpagerindicator.IconPagerAdapter;
import com.viewpagerindicator.TabPageIndicator;

public class PictureEditer extends HikeAppStateBaseFragmentActivity
{

	PhotosEditerFrameLayoutView editView;

	private int menuIcons[] = { R.drawable.filters, R.drawable.doodle_icon };

	private EditorClickListener clickHandler;

	private ImageView undoButton;

	private PhotoActionsFragment mPhotosActionsFragment;

	private String filename;

	private View mActionBarDoneContainer;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		overridePendingTransition(R.anim.fade_in_animation, R.anim.fade_out_animation);

		super.onCreate(savedInstanceState);

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
		FragmentPagerAdapter adapter = new PhotoEditViewPagerAdapter(getSupportFragmentManager(), clickHandler);

		ViewPager pager = (ViewPager) findViewById(R.id.pager);
		pager.setAdapter(adapter);

		TabPageIndicator indicator = (TabPageIndicator) findViewById(R.id.indicator);
		indicator.setViewPager(pager);

		undoButton = (ImageView) findViewById(R.id.undo);
		undoButton.setOnClickListener(clickHandler);

		TabPageIndicator tabs = (TabPageIndicator) findViewById(R.id.indicator);
		tabs.setOnPageChangeListener(clickHandler);

		setupActionBar();

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
		actionBarView.findViewById(R.id.back).setOnClickListener(new OnClickListener()
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

		private EditorClickListener mItemClickListener;

		public PhotoEditViewPagerAdapter(FragmentManager fm, EditorClickListener argItemClickListener)
		{
			super(fm);
			mItemClickListener = argItemClickListener;
		}

		@Override
		public Fragment getItem(int position)
		{

			switch (position)
			{
			case HikeConstants.HikePhotos.FILTER_FRAGMENT_ID:
				return new PreviewFragment(MenuType.Effects, mItemClickListener, editView.getScaledImageOriginal());
			case HikeConstants.HikePhotos.DOODLE_FRAGMENT_ID:
				return new PreviewFragment(MenuType.Doodle, mItemClickListener, editView.getScaledImageOriginal());
			}
			return null;
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
				me.select();
				if (prev != null && prev.getFilter() != me.getFilter())
				{
					prev.unSelect();
				}

			}

			else if (v.getClass() == DoodleEffectItemLinearLayout.class)
			{
				DoodleEffectItemLinearLayout me = (DoodleEffectItemLinearLayout) v;
				editView.setBrushColor(me.getBrushColor());
				doodlePreview.setBrushColor(me.getBrushColor());
				doodlePreview.refresh();

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
								if (actionCode == PhotoActionsFragment.ACTION_SEND)
								{
									editView.saveImage(HikeFileType.IMAGE, null,new HikePhotosListener() {
										
										@Override
										public void onFailure() {
											// TODO Auto-generated method stub
											
										}
										
										@Override
										public void onComplete(Bitmap bmp) {
											// TODO Auto-generated method stub
											
										}
										
										@Override
										public void onComplete(File f) {
											Intent forwardIntent = IntentManager.getForwardImageIntent(mContext, f);
											forwardIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
											startActivity(forwardIntent);											
										}
									});
								}
								else if (actionCode == PhotoActionsFragment.ACTION_SET_DP)
								{
									// User info is saved in shared preferences
									SharedPreferences preferences = HikeMessengerApp.getInstance().getApplicationContext()
											.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE);
									ContactInfo userInfo = Utils.getUserContactInfo(preferences);
									String mLocalMSISDN = userInfo.getMsisdn();
									editView.saveImage(HikeFileType.PROFILE, mLocalMSISDN,new HikePhotosListener() {
										
										@Override
										public void onFailure() {
											// TODO Auto-generated method stub
											
										}
										
										@Override
										public void onComplete(Bitmap bmp) {
											// TODO Auto-generated method stub
											
										}
										
										@Override
										public void onComplete(File f) {
											// TODO Auto-generated method stub
											getSupportFragmentManager().popBackStackImmediate();
											ProfilePicFragment profilePicFragment = new ProfilePicFragment();
											Bundle b = new Bundle();
											b.putString(HikeConstants.HikePhotos.FILENAME, f.getAbsolutePath());
											profilePicFragment.setArguments(b);
											getSupportFragmentManager().beginTransaction()
													.setCustomAnimations(R.anim.fade_in_animation, R.anim.photo_option_out, R.anim.fade_in_animation, R.anim.photo_option_out)
													.replace(R.id.overlayFrame, profilePicFragment).addToBackStack(null).commit();
										}
									});
									
								}
							}
						});
					}
					// Change fragment
					getSupportFragmentManager().beginTransaction()
							.setCustomAnimations(R.anim.fade_in_animation, R.anim.fade_out_animation, R.anim.fade_in_animation, R.anim.fade_out_animation)
							.replace(R.id.overlayFrame, mPhotosActionsFragment).addToBackStack(null).commit();

					mActionBarDoneContainer.setVisibility(View.INVISIBLE);

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
		if (getSupportFragmentManager().popBackStackImmediate())
		{
			mActionBarDoneContainer.setVisibility(View.VISIBLE);
		}
		else
		{
			// Tear down
			super.onBackPressed();
			if (filename != null)
			{
				File editFile = new File(filename);
				editFile.delete();
			}
		}
	}
}
