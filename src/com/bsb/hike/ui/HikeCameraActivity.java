package com.bsb.hike.ui;

import java.io.File;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.actionbarsherlock.internal.nineoldandroids.animation.Animator;
import com.actionbarsherlock.internal.nineoldandroids.animation.AnimatorListenerAdapter;
import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.models.GalleryItem;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.ui.fragments.CameraFragment;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class HikeCameraActivity extends HikeAppStateBaseFragmentActivity implements OnClickListener
{
	private static final String FLASH_AUTO = "fauto";

	private static final String FLASH_ON = "fon";

	private static final String FLASH_OFF = "foff";

	private static final String TAG = "HikeCameraActivity";

	private static final int GALLERY_PICKER_REQUEST = 2;

	private CameraFragment cameraFragment;

	private boolean isUsingFFC = true;

	private View containerView;

	private Interpolator deceleratorInterp = new DecelerateInterpolator();

	private Interpolator overshootInterp = new OvershootInterpolator();

	private OrientationEventListener orientationListener;

	private Bitmap stillPreviewBitmap;

	private View flashButton;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		overridePendingTransition(R.anim.fade_in_animation, R.anim.fade_out_animation);

		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.hike_camera_activity);

		cameraFragment = CameraFragment.newInstance(false);

		new Handler().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				flipCamera(containerView);
			}
		}, 200);

		findViewById(R.id.btntakepic).setOnClickListener(HikeCameraActivity.this);

		ImageButton galleryBtn = (ImageButton) findViewById(R.id.btngallery);
		galleryBtn.setOnClickListener(HikeCameraActivity.this);

		ImageButton flipBtn = (ImageButton) findViewById(R.id.btnflip);
		flipBtn.setOnClickListener(HikeCameraActivity.this);

		flashButton = findViewById(R.id.btntoggleflash);
		flashButton.setOnClickListener(HikeCameraActivity.this);

		int density = getResources().getDisplayMetrics().densityDpi;

		switch (density)
		{
		case DisplayMetrics.DENSITY_LOW:
		case DisplayMetrics.DENSITY_MEDIUM:
			findViewById(R.id.flashContainer).setVisibility(View.GONE);
			break;

		}

		containerView = findViewById(R.id.container);

		orientationListener = new OrientationEventListener(HikeCameraActivity.this, SensorManager.SENSOR_DELAY_UI)
		{
			float state = 0;

			float stateLandscape1 = -90;

			float stateLandscape2 = 90;

			float statePortrait = 0;

			public void onOrientationChanged(int orientation)
			{
				if (orientation > 315 || orientation < 45)
				{
					if (state != statePortrait)
					{
						Logger.d("Animating", "from" + state + "to" + 0);
						ObjectAnimator anim = ObjectAnimator.ofFloat(findViewById(R.id.btntakepic), "rotation", state, 0f);
						anim.setDuration(500); // Duration in milliseconds
						anim.setInterpolator(overshootInterp);
						anim.start();
						state = statePortrait;
					}
				}
				else if (orientation > 45 && orientation < 180)
				{
					if (state != stateLandscape1)
					{
						Logger.d("Animating", "from" + state + "to" + stateLandscape1);
						ObjectAnimator anim = ObjectAnimator.ofFloat(findViewById(R.id.btntakepic), "rotation", state, stateLandscape1);
						anim.setInterpolator(overshootInterp);
						anim.setDuration(500); // Duration in milliseconds
						anim.start();
						state = stateLandscape1;
					}
				}
				else if (orientation > 180 && orientation < 315)
				{
					if (state != stateLandscape2)
					{
						Logger.d("Animating", "from" + state + "to" + stateLandscape2);
						ObjectAnimator anim = ObjectAnimator.ofFloat(findViewById(R.id.btntakepic), "rotation", state, stateLandscape2);
						anim.setDuration(500); // Duration in milliseconds
						anim.setInterpolator(overshootInterp);
						anim.start();
						state = stateLandscape2;
					}
				}
			}
		};

		containerView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (cameraFragment != null)
				{
					if (cameraFragment.isAutoFocusAvailable())
					{
						cameraFragment.autoFocus();
					}
				}
			}
		});
		setupActionBar();

		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT))
		{
			View ffcBtn = findViewById(R.id.btnflip);
			ffcBtn.setClickable(false);
			ffcBtn.setEnabled(false);
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1)
			{
				ffcBtn.setAlpha(0.3f);
			}
		}

		showProductPopup(ProductPopupsConstants.PopupTriggerPoints.PHOTOS.ordinal());
	}

	private void setupActionBar()
	{
		findViewById(R.id.back).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});
		findViewById(R.id.done_container).setVisibility(View.GONE);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		orientationListener.enable();
	}

	@Override
	protected void onPause()
	{
		overridePendingTransition(R.anim.fade_in_animation, R.anim.fade_out_animation);
		super.onPause();
		orientationListener.disable();
		ImageView iv = (ImageView) HikeCameraActivity.this.findViewById(R.id.tempiv);

		iv.setVisibility(View.GONE);

		if (stillPreviewBitmap != null)
		{
			stillPreviewBitmap.recycle();
			stillPreviewBitmap = null;
		}

		HikeCameraActivity.this.findViewById(R.id.btntakepic).setClickable(true);
		HikeCameraActivity.this.findViewById(R.id.btngallery).setClickable(true);
		HikeCameraActivity.this.findViewById(R.id.btnflip).setClickable(true);
		HikeCameraActivity.this.findViewById(R.id.btntoggleflash).setClickable(true);
	}

	@Override
	public void onClick(View v)
	{
		int viewId = v.getId();

		switch (viewId)
		{
		case R.id.btntakepic:
			HikeCameraActivity.this.findViewById(R.id.btntakepic).setClickable(false);
			HikeCameraActivity.this.findViewById(R.id.btngallery).setClickable(false);
			HikeCameraActivity.this.findViewById(R.id.btnflip).setClickable(false);
			HikeCameraActivity.this.findViewById(R.id.btntoggleflash).setClickable(false);
			cameraFragment.cancelAutoFocus();
			cameraFragment.takePicture();

			View previewStratBmp = cameraFragment.getCameraView().previewStrategy.getWidget();

			if (previewStratBmp instanceof TextureView)
			{
				stillPreviewBitmap = ((TextureView) previewStratBmp).getBitmap();
			}
			else if (previewStratBmp instanceof SurfaceView)
			{
				// TODO. For now we do not show still preview after camera snap in case if preview is on SurfaceView. This is causing brief black screen on Cyanogenmod phones.
			}

			final View snapOverlay = findViewById(R.id.snapOverlay);
			ObjectAnimator invisToVis = ObjectAnimator.ofFloat(snapOverlay, "alpha", 0f, 0.8f);
			invisToVis.setDuration(200);
			invisToVis.setInterpolator(deceleratorInterp);
			invisToVis.addListener(new AnimatorListenerAdapter()
			{
				@Override
				public void onAnimationEnd(Animator animation)
				{
					if (stillPreviewBitmap != null)
					{
						ImageView iv = (ImageView) HikeCameraActivity.this.findViewById(R.id.tempiv);
						iv.setImageBitmap(stillPreviewBitmap);
						iv.setVisibility(View.VISIBLE);
					}
					ObjectAnimator visToInvis = ObjectAnimator.ofFloat(snapOverlay, "alpha", 0.8f, 0f);
					visToInvis.setDuration(150);
					visToInvis.setInterpolator(deceleratorInterp);
					visToInvis.start();
				}
			});

			invisToVis.start();

			snapOverlay.setVisibility(View.VISIBLE);

			sendAnalyticsCameraClicked(isUsingFFC);

			break;
		case R.id.btngallery:
			// Open gallery
			Intent galleryPickerIntent = IntentManager.getHikeGalleryPickerIntentForResult(HikeCameraActivity.this, false, false, GalleryActivity.PHOTOS_EDITOR_ACTION_BAR_TYPE,
					null);
			startActivityForResult(galleryPickerIntent, GALLERY_PICKER_REQUEST);
			break;

		case R.id.btnflip:
			flipCamera(v);
			break;
		case R.id.btntoggleflash:

			ImageButton btnFlash = (ImageButton) v;

			String tag = (String) btnFlash.getTag();

			if (tag.equals(FLASH_AUTO))
			{
				btnFlash.setTag(FLASH_OFF);
				btnFlash.setImageDrawable(getResources().getDrawable(R.drawable.flashoff));
				cameraFragment.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			}
			else if (tag.equals(FLASH_OFF))
			{
				btnFlash.setTag(FLASH_ON);
				btnFlash.setImageDrawable(getResources().getDrawable(R.drawable.flashon));
				cameraFragment.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
			}
			else if (tag.equals(FLASH_ON))
			{
				btnFlash.setTag(FLASH_AUTO);
				btnFlash.setImageDrawable(getResources().getDrawable(R.drawable.flashauto));
				cameraFragment.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
			}
			break;
		}
	}

	private void flipCamera(final View v)
	{
		v.setEnabled(false);

		ObjectAnimator visToInvis = ObjectAnimator.ofFloat(containerView, "alpha", 1f, 0f);
		visToInvis.setDuration(200);
		visToInvis.setInterpolator(deceleratorInterp);

		final ObjectAnimator invisToVis = ObjectAnimator.ofFloat(containerView, "alpha", 0f, 1f);
		invisToVis.setDuration(1000);
		invisToVis.setInterpolator(deceleratorInterp);

		visToInvis.addListener(new AnimatorListenerAdapter()
		{
			@Override
			public void onAnimationEnd(Animator anim)
			{
				isUsingFFC = isUsingFFC ? false : true;
				if (isUsingFFC)
				{
					flashButton.setClickable(false);
					if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1)
					{
						flashButton.setAlpha(0.3f);
					}
				}
				else
				{
					flashButton.setClickable(true);
					if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1)
					{
						flashButton.setAlpha(1f);
					}
				}
				cameraFragment = CameraFragment.newInstance(isUsingFFC);
				replaceFragment(cameraFragment);
				new Handler().postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						invisToVis.start();
					}
				}, 500);
			}
		});

		invisToVis.addListener(new AnimatorListenerAdapter()
		{
			@Override
			public void onAnimationEnd(Animator animation)
			{
				v.setEnabled(true);
			}
		});

		visToInvis.start();
	}

	private void replaceFragment(Fragment argFragment)
	{
		getSupportFragmentManager().beginTransaction().replace(R.id.container, argFragment).commit();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK)
		{
			switch (requestCode)
			{
			case GALLERY_PICKER_REQUEST:
				File myDir = new File(Utils.getFileParent(HikeFileType.IMAGE, false));
				myDir.mkdir();
				String fname = Utils.getOriginalFile(HikeFileType.IMAGE, null);
				File file = new File(myDir, fname);
				if (file.exists())
				{
					file.delete();
				}
				ArrayList<GalleryItem> itemList = data.getExtras().getParcelableArrayList(HikeConstants.Extras.GALLERY_SELECTIONS);
				String src = null;
				if (itemList != null && !itemList.isEmpty())
				{
					src = itemList.get(0).getFilePath();
				}
				Utils.startCropActivityForResult(this, src, file.getAbsolutePath(), true);
				break;
			case HikeConstants.CROP_RESULT:
				Intent intent = new Intent(HikeCameraActivity.this, PictureEditer.class);
				intent.putExtra(HikeConstants.HikePhotos.FILENAME, data.getStringExtra(MediaStore.EXTRA_OUTPUT));
				startActivity(intent);
				sendAnalyticsGalleryPic();
				break;
			}
		}
		else if (resultCode == RESULT_CANCELED)
		{
			switch (requestCode)
			{
			case HikeConstants.CROP_RESULT:
				// Open gallery
				Intent galleryPickerIntent = IntentManager.getHikeGalleryPickerIntentForResult(HikeCameraActivity.this, false, false,
						GalleryActivity.PHOTOS_EDITOR_ACTION_BAR_TYPE, null);
				startActivityForResult(galleryPickerIntent, GALLERY_PICKER_REQUEST);
				break;

			default:
				break;
			}
		}
	}

	public Bitmap processSquareBitmap(Bitmap srcBmp)
	{
		if (containerView != null)
		{

			View preview = HikeCameraActivity.this.findViewById(R.id.previewWindow);

			Rect r = new Rect();

			preview.getGlobalVisibleRect(r);

			int bmpY = (int) (srcBmp.getHeight() * r.top / containerView.getHeight());

			int widthBmp = (bmpY * preview.getWidth()) / r.top;

			widthBmp = widthBmp > srcBmp.getWidth() ? srcBmp.getWidth() : widthBmp;

			Bitmap dstBmp = HikePhotosUtils.createBitmap(srcBmp, srcBmp.getWidth() / 2 - (widthBmp / 2), bmpY, widthBmp, widthBmp, false, false, true, true);

			// Bitmap dstBmp = Bitmap.createBitmap(srcBmp, srcBmp.getWidth() / 2 - (widthBmp / 2), bmpY, widthBmp, widthBmp);

			// ImageView iv = (ImageView) HikeCameraActivity.this.findViewById(R.id.containerImageView);
			//
			// iv.setImageBitmap(dstBmp);
			//
			// iv.setVisibility(View.VISIBLE);

			return dstBmp;
		}
		return null;
	}

	private void sendAnalyticsCameraClicked(boolean ffc)
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(HikeConstants.HikePhotos.PHOTOS_IS_FFC_MODE, ffc);
			json.put(AnalyticsConstants.EVENT_KEY, HikeConstants.LogEvent.PHOTOS_CAMERA_CLICK);
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
}
