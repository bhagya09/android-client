package com.bsb.hike.ui;

import java.io.File;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Window;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.GalleryItem;
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
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeUiHandler;
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

	PhotosEditerFrameLayoutView editView;

	private int menuIcons[] = { R.drawable.photos_tabs_filter_selector, R.drawable.photos_tabs_doodle_selector };

	private EditorClickListener clickHandler;
	
	private String[] notSupportedImageFormat= new String[]{"gif"};

	private ImageView undoButton;

	private PhotoActionsFragment mPhotosActionsFragment;

	private String filename;

	private View mActionBarDoneContainer;

	private PhotosTabPageIndicator indicator;

	private ViewPager pager;

	private View mActionBarBackButton;

	private View overlayFrame;
	
	private View progressLayout;

	private boolean startedForResult;
	private boolean startedForProfileUpdate;
	private boolean isScreenActive;
	

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		overridePendingTransition(R.anim.fade_in_animation, R.anim.fade_out_animation);

		super.onCreate(savedInstanceState);

		getWindow().requestFeature((int) Window.FEATURE_ACTION_BAR_OVERLAY);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.fragment_picture_editer);
		
		startedForResult = (getCallingActivity() != null);

		editView = (PhotosEditerFrameLayoutView) findViewById(R.id.editer);

		clickHandler = new EditorClickListener(this);
		
		progressLayout = findViewById(R.id.progressLayout);

		enableScreen(false);
		
		// Get filename from intent data
		Intent intent = getIntent();
		filename = intent.getStringExtra(HikeMessengerApp.FILE_PATH);

		if (filename == null)
		{
			// Check if intent is from GalleryActivity
			ArrayList<GalleryItem> galleryList = intent.getParcelableArrayListExtra(HikeConstants.Extras.GALLERY_SELECTIONS);
			if (galleryList != null && !galleryList.isEmpty())
			{
				filename = galleryList.get(0).getFilePath();
				sendAnalyticsGalleryPic();
			}
			if (filename == null && intent.getData()!=null)
			{
				filename = intent.getData().toString();
				sendAnalyticsGalleryPic();
			}
		}

		if (filename == null)
		{
			PictureEditer.this.finish();
			return;
		}
		
		String fileExtension = Utils.getFileExtension(filename);
		
		//Check file type
		String fileType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
		HikeFileType hikeFileType = HikeFileType.fromString(fileType, false);

		if (hikeFileType.compareTo(HikeFileType.IMAGE) != 0)
		{
			onError();
			return;
		}
		else if(isNotSupportedImageFormat(fileExtension))
		{
			if(startedForResult)
			{
				Intent retIntent = new Intent();
				retIntent.putExtra(HikeConstants.Extras.IMAGE_PATH, filename);
				retIntent.setAction(HikeConstants.HikePhotos.PHOTOS_ACTION_CODE);
				setResult(RESULT_OK, retIntent);
				finish();
			}
			else
			{
				onError();
				return;
			}
		}

		HikeBitmapFactory.correctBitmapRotation(filename, new HikePhotosListener()
		{
			@Override
			public void onFailure()
			{
				PictureEditer.this.runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						Toast.makeText(PictureEditer.this, getResources().getString(R.string.file_expire), Toast.LENGTH_SHORT).show();
						PictureEditer.this.finish();
					}
				});
			}

			@Override
			public void onComplete(final Bitmap bmp)
			{
				PictureEditer.this.runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
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

		if(!startedForResult)
		{
			startedForProfileUpdate = intent.getBooleanExtra(HikeConstants.HikePhotos.ONLY_PROFILE_UPDATE, false);
		}

		setupActionBar();

		// We need to create a single destination copy
		String destinationFileHandle = intent.getStringExtra(HikeConstants.HikePhotos.DESTINATION_FILENAME);

		if (TextUtils.isEmpty(destinationFileHandle))
		{
			destinationFileHandle = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.IMAGE_ROOT + File.separator + Utils.getOriginalFile(HikeFileType.IMAGE, null);
		}

		editView.setDestinationPath(destinationFileHandle);

		pager = (ViewPager) findViewById(R.id.pager);

		indicator = (PhotosTabPageIndicator) findViewById(R.id.indicator);

		undoButton = (ImageView) findViewById(R.id.undo);

		overlayFrame = findViewById(R.id.overlayFrame);

		editView.setCompressionEnabled(intent.getBooleanExtra(HikeConstants.HikePhotos.EDITOR_ALLOW_COMPRESSION_KEY, true));

	}
	
	private boolean isNotSupportedImageFormat(String formatExtension)
	{
		for(int i = 0;i<notSupportedImageFormat.length;i++)
		{
			if(notSupportedImageFormat[i].equalsIgnoreCase(formatExtension))
			{
				return true;
			}
		}
		return false;
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
	
	private void enableScreen(boolean state)
	{
		isScreenActive = state;
		if(!isScreenActive)
		{
			progressLayout.setVisibility(View.VISIBLE);
		}
		else
		{
			progressLayout.setVisibility(View.GONE);
		}
	}
	
	private void init(Bitmap srcBitmap)
	{
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
		
		enableScreen(true);
		
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
		HikeEffectsFactory.finish();
		super.finish();
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

		if (startedForResult)
		{
			((TextView) actionBarView.findViewById(R.id.done_text)).setText(R.string.image_quality_send);
		}

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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK)
		{
			switch (requestCode)
			{
			case HikeConstants.CROP_RESULT:
				enableScreen(true);
				uploadProfilePic(data.getStringExtra(MediaStore.EXTRA_OUTPUT), data.getStringExtra(HikeConstants.HikePhotos.ORIG_FILE));
				break;
			}
		}
	}

	private void uploadProfilePic(final String croppedImageFile, final String originalImageFile)
	{

		HikeUiHandler.getHandler().post(new Runnable()
		{
			@Override
			public void run()
			{
				editView.setVisibility(View.VISIBLE);
				mActionBarBackButton.setVisibility(View.GONE);
				ProfilePicFragment profilePicFragment = new ProfilePicFragment();
				Bundle b = new Bundle();
				b.putString(HikeConstants.HikePhotos.FILENAME, croppedImageFile);
				b.putString(HikeConstants.HikePhotos.ORIG_FILE, originalImageFile);
				profilePicFragment.setArguments(b);
				getSupportFragmentManager().beginTransaction()
						.setCustomAnimations(R.anim.fade_in_animation, R.anim.fade_out_animation, R.anim.fade_in_animation, R.anim.fade_out_animation)
						.replace(R.id.overlayFrame, profilePicFragment).addToBackStack(null).commit();
			}
		});
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
			
			if(!isScreenActive)
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
				if (!startedForResult && !startedForProfileUpdate)
				{
					loadPreviewFragment();
				}
				else if(!startedForResult && startedForProfileUpdate)
				{
					setupProfilePicUpload();
				}
				else
				{
					enableScreen(false);
					editView.saveImage(HikeFileType.IMAGE, null, new HikePhotosListener()
					{

						@Override
						public void onFailure()
						{
							progressLayout.setVisibility(View.GONE);
							Intent intent = new Intent();
							setResult(RESULT_CANCELED, intent);
							finish();

						}

						@Override
						public void onComplete(Bitmap bmp)
						{
							// TODO Auto-generated method stub

						}

						@Override
						public void onComplete(File f)
						{
							progressLayout.setVisibility(View.GONE);
							Intent intent = new Intent();
							intent.putExtra(HikeConstants.Extras.IMAGE_PATH, f.getAbsolutePath());
							intent.setAction(HikeConstants.HikePhotos.PHOTOS_ACTION_CODE);
							setResult(RESULT_OK, intent);
							finish();
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
							enableScreen(false);
							sendAnalyticsSendTo();
							editView.saveImage(HikeFileType.IMAGE, null, new HikePhotosListener()
							{
								@Override
								public void onFailure()
								{
									sendAnalyticsSendTo();
									editView.saveImage(HikeFileType.IMAGE, null, new HikePhotosListener()
									{
										@Override
										public void onFailure()
										{
											enableScreen(true);
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
							setupProfilePicUpload();
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
			enableScreen(false);
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
				public void onComplete(File f)
				{
					startActivityForResult(IntentFactory.getCropActivityIntent(PictureEditer.this, f.getAbsolutePath(), f.getAbsolutePath(), true, 100, true), HikeConstants.CROP_RESULT);
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
			if(!isScreenActive)
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

			Logger.e("akhil", "Scrolling to position : "+positionFinal + "/" + parent.getLastVisiblePosition());
			
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
		if(!isScreenActive)
		{
			return;
		}
		
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

	public EditorClickListener getClickHandler()
	{
		return clickHandler;
	}

}
