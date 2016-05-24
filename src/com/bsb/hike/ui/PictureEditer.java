package com.bsb.hike.ui;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.cropimage.CropCompression;
import com.bsb.hike.cropimage.HikeCropActivity;
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
import com.bsb.hike.ui.utils.StatusBarColorChanger;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.jess.ui.TwoWayAdapterView;
import com.jess.ui.TwoWayAdapterView.OnItemClickListener;
import com.jess.ui.TwoWayGridView;
import com.viewpagerindicator.IconPagerAdapter;
import com.viewpagerindicator.PhotosTabPageIndicator;

public class PictureEditer extends HikeAppStateBaseFragmentActivity
{

	private String TEMP_PROFILE_IMAGE = "temp_ppi";
	
	PhotosEditerFrameLayoutView editView;

	private int menuIcons[] = { R.drawable.photos_tabs_filter_selector, R.drawable.photos_tabs_doodle_selector };
	
	private int menuIconsDesc[] = { R.string.cont_desc_photos_tab_filter, R.string.cont_desc_photos_tab_doodle };

	private EditorClickListener clickHandler;
	
	private ImageView undoButton;

	private PhotoActionsFragment mPhotosActionsFragment;

	private String filename, mLocalMSISDN;

	private View mActionBarDoneContainer;

	private PhotosTabPageIndicator indicator;

	private ViewPager pager;

	private View mActionBarBackButton;

	private View overlayFrame;

	private View progressLayout;

	private boolean startedForProfileUpdate;

	private boolean isWorking;
	
	private String tempProfileImageName;

	private final String TAG = PictureEditer.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Logger.d(TAG, "Picture Editer onCreate");
		getWindow().requestFeature((int) Window.FEATURE_ACTION_BAR_OVERLAY);
		overridePendingTransition(R.anim.fade_in_animation, R.anim.fade_out_animation);

		super.onCreate(savedInstanceState);

//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.fragment_picture_editer);
		
		editView = (PhotosEditerFrameLayoutView) findViewById(R.id.editer);

		clickHandler = new EditorClickListener(this);
		
		progressLayout = findViewById(R.id.progressBar);

		// Get filename from intent data
		Intent intent = getIntent();
		filename = intent.getStringExtra(HikeMessengerApp.FILE_PATH);

		if (filename == null)
		{
			sendAnalyticsGalleryPic();
			filename = intent.getStringExtra(HikeConstants.Extras.GALLERY_SELECTION_SINGLE);
		}

		if (filename == null)
		{
			PictureEditer.this.finish();
			return;
		}
		
		Logger.d(TAG, "Checking file type");
		
		//special handling for webp images since some devices cant extract their mime type
		if (!Utils.getFileExtension(filename).equalsIgnoreCase("webp") && HikeFileType.fromFilePath(filename, false).compareTo(HikeFileType.IMAGE) != 0)
		{
			onError();
			return;
		}
		
		SharedPreferences preferences = HikeMessengerApp.getInstance().getApplicationContext()
				.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE);
		ContactInfo userInfo = Utils.getUserContactInfo(preferences);
		mLocalMSISDN = userInfo.getMsisdn();
		
		HikePhotosUtils.FilterTools.setCurrentDoodleItem(null);
		HikePhotosUtils.FilterTools.setCurrentFilterItem(null);
		
		beginProgress();

		Logger.d(TAG, "Image rotation correction");
		
		HikeBitmapFactory.correctBitmapRotation(filename, new HikePhotosListener()
		{
			@Override
			public void onFailure()
			{
				Logger.d(TAG, "Image rotation correction failed");
				sendAnalyticsFileCouldNotLoad();
				PictureEditer.this.runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						if(isStartedForResult())
						{
							Bundle bundle = new Bundle();
							bundle.putString(HikeConstants.Extras.IMAGE_PATH, filename);
							if(hasDelegateActivities())
							{
								launchNextDelegateActivity(bundle);
							}
							else
							{
								Intent intent = new Intent();
								intent.putExtras(bundle);
								intent.setAction(HikeConstants.HikePhotos.PHOTOS_ACTION_CODE);
								setResult(RESULT_OK, intent);
							}
						}
						PictureEditer.this.finish();
					}
				});
			}

			@Override
			public void onComplete(final Bitmap bmp)
			{
				Logger.d(TAG, "Image rotation correction success");
				PictureEditer.this.runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						Logger.d(TAG, "Picture Editer Initialized");
						// Init
						init(bmp);
					}
				});
			}

			@Override
			public void onComplete(File f)
			{
				// Not used
			}
		});

		if(!isStartedForResult())
		{
			startedForProfileUpdate = intent.getBooleanExtra(HikeConstants.HikePhotos.ONLY_PROFILE_UPDATE, false);
		}

		StatusBarColorChanger.setStatusBarColor(getWindow(), Color.BLACK);
		setupActionBar();

		// We need to create a single destination copy
		String destinationFileHandle = intent.getStringExtra(HikeConstants.HikePhotos.DESTINATION_FILENAME);

		if (TextUtils.isEmpty(destinationFileHandle))
		{
			String directory = getEditImageSaveDirectory(true);
			File dir = new File(directory);
			if (!dir.exists())
			{
				dir.mkdirs();
			}
			destinationFileHandle = getEditImageSaveDirectory(true)+ File.separator + Utils.getUniqueFilename(HikeFileType.IMAGE);
		}

		editView.setDestinationPath(destinationFileHandle);

		pager = (ViewPager) findViewById(R.id.pager);

		indicator = (PhotosTabPageIndicator) findViewById(R.id.indicator);

		undoButton = (ImageView) findViewById(R.id.undo);

		overlayFrame = findViewById(R.id.overlayFrame);

		editView.setCompressionEnabled(intent.getBooleanExtra(HikeConstants.HikePhotos.EDITOR_ALLOW_COMPRESSION_KEY, true));
		
		if(savedInstanceState !=null && savedInstanceState.containsKey(TEMP_PROFILE_IMAGE))
		{
			tempProfileImageName = savedInstanceState.getString(TEMP_PROFILE_IMAGE);
		}

	}
	
	private void onError()
	{
		Toast.makeText(getApplicationContext(), getString(R.string.only_photos_edit), Toast.LENGTH_SHORT).show();
		PictureEditer.this.finish();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		overridePendingTransition(R.anim.fade_in_animation, R.anim.fade_out_animation);
		editView.enable();
	}
	
	private void finishProgress()
	{
		Logger.d(TAG, "finishProgress");
		isWorking = false;
		progressLayout.setVisibility(View.GONE);
	}
	
	private void beginProgress()
	{
		isWorking = true;
		progressLayout.setVisibility(View.VISIBLE);
	}
	
	private void init(Bitmap srcBitmap)
	{
		Logger.d(TAG, "Picture Editer Init");
		
		
		FragmentPagerAdapter adapter = new PhotoEditViewPagerAdapter(getSupportFragmentManager());
		pager.setAdapter(adapter);
		pager.setVisibility(View.VISIBLE);

		indicator.setViewPager(pager);
		indicator.setVisibility(View.VISIBLE);

		editView.loadImageFromBitmap(srcBitmap);
		editView.setOnDoodlingStartListener(clickHandler);
		editView.enableFilters();
		undoButton.setOnClickListener(clickHandler);

		indicator.setOnPageChangeListener(clickHandler);
		
		finishProgress();
		
		Logger.d(TAG, "Picture Editer Initialized");
		
	}

	@Override
	protected void onPause()
	{
		overridePendingTransition(R.anim.fade_in_animation, R.anim.fade_out_animation);
		super.onPause();
	}

	@Override
	public void finish()
	{
		HikePhotosUtils.FilterTools.setCurrentDoodleItem(null);
		HikePhotosUtils.FilterTools.setCurrentFilterItem(null);
		
		HikeEffectsFactory.finish();
		super.finish();
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.photos_action_bar, null);

		mActionBarDoneContainer = actionBarView.findViewById(R.id.done_container);
		mActionBarDoneContainer.setVisibility(View.VISIBLE);
		mActionBarDoneContainer.setOnClickListener(clickHandler);

		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);
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

		@Override
		public String getIconContentDescription(int index)
		{
			return PictureEditer.this.getString(menuIconsDesc[index]);
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == HikeConstants.CROP_RESULT)
		{
			switch (resultCode)
			{
			case RESULT_OK:
				try
				{
					isWorking = true;
					uploadProfilePic(data.getStringExtra(HikeCropActivity.CROPPED_IMAGE_PATH), data.getStringExtra(HikeCropActivity.SOURCE_IMAGE_PATH));
				}
				finally
				{
					isWorking = false;
				}
				break;
			case RESULT_CANCELED:
				//The user returned from crop...deleting temporary profile image if created
				deleteTempProfilePicIfExists();
				break;
			}
		}
	}
	
	private void deleteTempProfilePicIfExists()
	{
		//The user returned from crop...deleting temporary profile image if created
		String fileName = getTempProfileImageName();
		
		if(fileName == null)
		{
			return;
		}
		
		File temp = new File(fileName);
		if(temp.exists())
		{
			temp.delete();
		}
	}

	private void uploadProfilePic(final String croppedImageFile, final String originalImageFile)
	{
		editView.setVisibility(View.VISIBLE);
		ProfilePicFragment profilePicFragment = new ProfilePicFragment();
		Bundle b = new Bundle();
		b.putString(HikeConstants.HikePhotos.FILENAME, croppedImageFile);
		b.putString(HikeConstants.HikePhotos.ORIG_FILE, croppedImageFile);
		profilePicFragment.setArguments(b);
		getSupportFragmentManager().beginTransaction().replace(R.id.overlayFrame, profilePicFragment).addToBackStack(null).commit();
	}

	public class EditorClickListener implements OnClickListener, OnPageChangeListener, OnDoodleStateChangeListener, OnItemClickListener
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
			
			if(isWorking)
			{
				return;
			}
			
			switch (v.getId())
			{
			case R.id.plusWidth:
				if (doodleWidth + HikeConstants.HikePhotos.DELTA_BRUSH_WIDTH <= HikeConstants.HikePhotos.MAX_BRUSH_WIDTH)
				{
					doodleWidth += HikeConstants.HikePhotos.DELTA_BRUSH_WIDTH;
				}
				doodlePreview.setBrushWidth(HikePhotosUtils.dpToPx(doodleWidth));
				doodlePreview.refresh();
				editView.setBrushWidth(HikePhotosUtils.dpToPx(doodleWidth));
				break;
			case R.id.minusWidth:
				if (doodleWidth - HikeConstants.HikePhotos.DELTA_BRUSH_WIDTH >= HikeConstants.HikePhotos.Min_BRUSH_WIDTH)
				{
					doodleWidth -= HikeConstants.HikePhotos.DELTA_BRUSH_WIDTH;
				}
				doodlePreview.setBrushWidth(HikePhotosUtils.dpToPx(doodleWidth));
				doodlePreview.refresh();
				editView.setBrushWidth(HikePhotosUtils.dpToPx(doodleWidth));
				break;
			case R.id.undo:
				editView.undoLastDoodleDraw();
				break;
			case R.id.done_container:
				if (!isStartedForResult() && !startedForProfileUpdate)
				{
					loadPreviewFragment();
				}
				else if(!isStartedForResult() && startedForProfileUpdate)
				{
					setupProfilePicUpload();
				}
				else
				{
					beginProgress();
					editView.saveImage(HikeFileType.IMAGE, filename, new HikePhotosListener()
					{

						@Override
						public void onFailure()
						{
							finishProgress();
							Intent intent = new Intent();
							setResult(RESULT_CANCELED, intent);
							onError();
						}

						@Override
						public void onComplete(Bitmap bmp)
						{
							// Do nothing
						}

						@Override
						public void onComplete(File f)
						{
							finishProgress();
							Bundle bundle = new Bundle();
							bundle.putString(HikeCropActivity.SOURCE_IMAGE_PATH, f.getAbsolutePath());
							if(hasDelegateActivities())
							{
								launchNextDelegateActivity(bundle);
							}
							else
							{
								Intent intent = new Intent();
								intent.putExtras(bundle);
								intent.setAction(HikeConstants.HikePhotos.PHOTOS_ACTION_CODE);
								setResult(RESULT_OK, intent);
								finish();

							}
						}
					});
				}
				break;
			}

		}

		private void loadPreviewFragment()
		{
			if (mPhotosActionsFragment == null)
			{
				mPhotosActionsFragment = new PhotoActionsFragment();
				mPhotosActionsFragment.setActionListener(new ActionListener()
				{
					@Override
					public void onAction(int actionCode)
					{
						getSupportFragmentManager().popBackStackImmediate();
						mActionBarDoneContainer.setVisibility(View.VISIBLE);
						if (actionCode == PhotoActionsFragment.ACTION_SEND)
						{
							beginProgress();
							sendAnalyticsSendTo();
							editView.saveImage(HikeFileType.IMAGE, filename, new HikePhotosListener()
							{
								@Override
								public void onFailure()
								{
									sendAnalyticsSendTo();
									editView.saveImage(HikeFileType.IMAGE, filename, new HikePhotosListener()
									{
										@Override
										public void onFailure()
										{
											finishProgress();
										}

										@Override
										public void onComplete(Bitmap bmp)
										{
											// Do nothing
										}

										@Override
										public void onComplete(File f)
										{
											finishProgress();
											Intent forwardIntent = IntentFactory.getForwardImageIntent(mContext, f);
											forwardIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
											startActivity(forwardIntent);
										}
									});
								}

								@Override
								public void onComplete(Bitmap bmp)
								{
									// Do nothing
								}

								@Override
								public void onComplete(File f)
								{
									finishProgress();
									Intent forwardIntent = IntentFactory.getForwardImageIntent(mContext, f);
									forwardIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
									startActivity(forwardIntent);
								}
							});
						}
						else if (actionCode == PhotoActionsFragment.ACTION_SET_DP)
						{
							setupProfilePicUpload();
						}
						else if (actionCode == PhotoActionsFragment.ACTION_POST)
						{
							beginProgress();

							editView.saveImage(HikeFileType.IMAGE, filename, new HikePhotosListener()
							{

								@Override
								public void onFailure()
								{
									finishProgress();
								}

								@Override
								public void onComplete(Bitmap bmp)
								{
									// Do nothing
								}

								@Override
								public void onComplete(File f)
								{
									finishProgress();

									if (f.exists())
									{
										startActivity(IntentFactory.getPostStatusUpdateIntent(PictureEditer.this,null, f.getAbsolutePath(),false));
									}
									else
									{
										Toast.makeText(getApplicationContext(), R.string.photos_oom_save, Toast.LENGTH_SHORT).show();
									}
								}
							});

						}
					}
				});
			}

			editView.disable();

			// Change fragment
			getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.photo_option_in, R.anim.photo_option_out, R.anim.photo_option_in, R.anim.photo_option_out)
					.replace(R.id.overlayFrame, mPhotosActionsFragment).addToBackStack(null).commit();

			mActionBarDoneContainer.setVisibility(View.INVISIBLE);

			overlayFrame.setVisibility(View.VISIBLE);

		}
		
		private void setupProfilePicUpload()
		{
			beginProgress();
			sendAnalyticsSetAsDp();
			// User info is saved in shared preferences

			editView.saveImage(HikeFileType.PROFILE, mLocalMSISDN, new HikePhotosListener()
			{

				@Override
				public void onFailure()
				{
					finishProgress();
				}

				@Override
				public void onComplete(Bitmap bmp)
				{
					// Do nothing
				}

				@Override
				public void onComplete(File f)
				{
					finishProgress();
					setTempProfileImageName(f.getAbsolutePath());

					CropCompression compression = new CropCompression().maxWidth(640).maxHeight(640).quality(80);
					Intent cropIntent = IntentFactory.getCropActivityIntent(PictureEditer.this, f.getAbsolutePath(), f.getAbsolutePath(), compression,false,true);
					startActivityForResult(cropIntent, HikeConstants.CROP_RESULT);
				}
			});
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
				editView.enableFilters();
				undoButton.setVisibility(View.GONE);
				break;
			case HikeConstants.HikePhotos.DOODLE_FRAGMENT_ID:
				editView.enableDoodling();
				editView.disableFilters();
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

		@Override
		public void onItemClick(TwoWayAdapterView<?> parent, View view, int position, long id)
		{
			if(isWorking)
			{
				return;
			}
			
			if (view.getClass() == FilterEffectItemLinearLayout.class)
			{
				FilterEffectItemLinearLayout prev = HikePhotosUtils.FilterTools.getCurrentFilterItem();
				FilterEffectItemLinearLayout me = (FilterEffectItemLinearLayout) view;
				editView.applyFilter(me.getFilter());
				if (prev != null && prev.getFilter() != me.getFilter())
				{
					prev.unSelect();
				}
				me.select();
			}
			else if (view.getClass() == DoodleEffectItemLinearLayout.class)
			{
				DoodleEffectItemLinearLayout prev = HikePhotosUtils.FilterTools.getCurrentDoodleItem();
				DoodleEffectItemLinearLayout me = (DoodleEffectItemLinearLayout) view;
				editView.setBrushColor(me.getBrushColor());
				doodlePreview.setBrushColor(me.getBrushColor());
				doodlePreview.refresh();
				if (prev != null && prev.getBrushColor() != me.getBrushColor())
				{
					prev.unSelect();
				}
				me.select();
			}

			autoScrollToNext((TwoWayGridView) parent, position);
		}

		/**
		 * {@link} : http://stackoverflow.com/questions/11431832/android-smoothscrolltoposition-not-working-correctly
		 * 
		 * @param TwoWayGridView
		 *            parent whose children are to be scrolled
		 * @param currentPosition
		 *            of the element selected.(Scrolling occurs relative to this position)
		 */
		private void autoScrollToNext(final TwoWayGridView parent, int currentPosition)
		{
			final int positionFinal;
			if (currentPosition - 1 == parent.getFirstVisiblePosition() || currentPosition == parent.getFirstVisiblePosition())
			{
				positionFinal = currentPosition - 1;
			}
			else if(currentPosition < parent.getLastVisiblePosition()-1)
			{
				positionFinal = currentPosition + 1;
			}
			else
			{
				positionFinal = parent.getLastVisiblePosition();
			}

			// SmoothScroll needs to do a lot of work
			parent.post(new Runnable()
			{
				@Override
				public void run()
				{
					parent.smoothScrollToPosition(positionFinal);
				}
			});
		}

	}

	@Override
	public void onBackPressed()
	{
		if (getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE))
		{
			getSupportActionBar().show();
			mActionBarDoneContainer.setVisibility(View.VISIBLE);
			editView.enable();
		}
		else
		{
			// Tear down
			super.onBackPressed();
		}
	}

	
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {

		if(tempProfileImageName != null)
		{
			outState.putString(TEMP_PROFILE_IMAGE, tempProfileImageName);
		}
		super.onSaveInstanceState(outState);
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

	private void sendAnalyticsGalleryPic()
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.EVENT_KEY, HikeConstants.LogEvent.PHOTOS_GALLERY_PICK);
			HikeAnalyticsEvent.analyticsForPhotos(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
	
	private void sendAnalyticsFileCouldNotLoad()
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.EVENT_KEY, HikeConstants.LogEvent.PHOTOS_UNABLE_TO_LOAD);
			HikeAnalyticsEvent.analyticsForPhotos(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.ERROR_EVENT, json);
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
	
	public static String getEditImageSaveDirectory(boolean includeRoot)
	{
		if(includeRoot)
		{
			return HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.IMAGE_ROOT;
		}
		return HikeConstants.IMAGE_ROOT;
	}

	public String getTempProfileImageName()
	{
		return tempProfileImageName;
	}
	
	public void setTempProfileImageName(String name)
	{
		
		tempProfileImageName = name;
	}
	
}
