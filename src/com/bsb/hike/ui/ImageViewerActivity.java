package com.bsb.hike.ui;

import android.animation.TimeInterpolator;
import android.app.ProgressDialog;
import android.graphics.ColorMatrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.ProfileImageLoader;
import com.bsb.hike.utils.Utils;

public class ImageViewerActivity extends FragmentActivity implements OnClickListener, Listener
{
	ImageView imageView;

	private ProgressDialog mDialog;

	private String mappedId;

	private String key;

	private boolean isStatusImage;

	private int imageSize;

	public static final int FROM_PROFILE_ACTIVITY = 1;

	public static final int FROM_SETTINGS_ACTIVITY = 2;

	private int whichActivity;

	private DisplayPictureEditListener mProfilePhotoEditListener;

	private String[] profilePicPubSubListeners = { HikePubSub.ICON_CHANGED };

	private boolean isViewEditable = false;

	private View background;

	int mLeftDelta;

	int mTopDelta;

	float mWidthScale;

	float mHeightScale;

	private final String TAG = ImageViewerActivity.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		overridePendingTransition(0, 0);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_viewer_activity);
		HikeMessengerApp.getPubSub().addListeners(this, profilePicPubSubListeners);
		imageView = (ImageView) findViewById(R.id.image);
		imageView.setOnClickListener(this);
		background = findViewById(R.id.bg_screen);

		Bundle extras = getIntent().getExtras();

		mappedId = extras.getString(HikeConstants.Extras.MAPPED_ID);

		isStatusImage = extras.getBoolean(HikeConstants.Extras.IS_STATUS_IMAGE);

		imageSize = getApplicationContext().getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size);

		String PACKAGE_NAME = "com.bsb.hike";
		final int thumbnailTop = extras.getInt(PACKAGE_NAME + ".top");
		final int thumbnailLeft = extras.getInt(PACKAGE_NAME + ".left");
		final int thumbnailWidth = extras.getInt(PACKAGE_NAME + ".width");
		final int thumbnailHeight = extras.getInt(PACKAGE_NAME + ".height");
		final int mOriginalOrientation = extras.getInt(PACKAGE_NAME + ".orientation");

		Logger.d(TAG, "thumbnailTop " + thumbnailTop);

		Logger.d(TAG, "thumbnailLeft " + thumbnailLeft);

		Logger.d(TAG, "thumbnailWidth " + thumbnailWidth);

		Logger.d(TAG, "thumbnailHeight " + thumbnailHeight);

		ViewTreeObserver observer = imageView.getViewTreeObserver();
		observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener()
		{

			@Override
			public boolean onPreDraw()
			{
				imageView.getViewTreeObserver().removeOnPreDrawListener(this);

				// Figure out where the thumbnail and full size versions are, relative
				// to the screen and each other
				int[] screenLocation = new int[2];
				imageView.getLocationOnScreen(screenLocation);
				mLeftDelta = thumbnailLeft - screenLocation[0];
				mTopDelta = thumbnailTop - screenLocation[1];

				// Scale factors to make the large version the same size as the thumbnail
				mWidthScale = (float) thumbnailWidth / imageView.getWidth();
				mHeightScale = (float) thumbnailHeight / imageView.getHeight();

				Logger.d(TAG, "imageViewPos x,y - " + screenLocation[0] + " , " + screenLocation[1]);

				Logger.d(TAG, "mLeftDelta " + mLeftDelta);

				Logger.d(TAG, "mTopDelta " + mTopDelta);

				Logger.d(TAG, "mWidthScale " + mWidthScale);

				Logger.d(TAG, "mHeightScale " + mHeightScale);

				runEnterAnimation();

				return true;
			}
		});

		showImage();
	}

	private static final TimeInterpolator sDecelerator = new DecelerateInterpolator();

	private static final TimeInterpolator sAccelerator = new AccelerateInterpolator();

	private static final TimeInterpolator sOverShootInterpolator = new OvershootInterpolator();

	private static final int ANIM_DURATION = 500;

	private BitmapDrawable mBitmapDrawable;

	private ColorMatrix colorizerMatrix = new ColorMatrix();

	public void runEnterAnimation()
	{
		final long duration = (long) (ANIM_DURATION * 1);

		// Set starting values for properties we're going to animate. These
		// values scale and position the full size version down to the thumbnail
		// size/location, from which we'll animate it back up
		imageView.setPivotX(0);
		imageView.setPivotY(0);
		imageView.setScaleX(mWidthScale);
		imageView.setScaleY(mHeightScale < 0.6f ? 0.6f : mHeightScale);
		imageView.setTranslationX(mLeftDelta);
		imageView.setAlpha(0f);
		imageView.setTranslationY(mTopDelta);

		// Animate scale and translation to go from thumbnail to full size
		imageView.animate().setDuration(duration).scaleX(1).scaleY(1).translationX(0).translationY(0).alpha(1).withEndAction(new Runnable()
		{
			public void run()
			{
				// Animate the description in after the image animation
				// is done. Slide and fade the text in from underneath
				// the picture.
				// // Fade in the black background
				
			}
		});


		ObjectAnimator bgAnim = ObjectAnimator.ofFloat(background, "alpha", 0f, 1f);
		bgAnim.setDuration(700);
		bgAnim.start();
		
		//
		// // Animate a color filter to take the image from grayscale to full color.
		// // This happens in parallel with the image scaling and moving into place.
		// ObjectAnimator colorizer = ObjectAnimator.ofFloat(PictureDetailsActivity.this,
		// "saturation", 0, 1);
		// colorizer.setDuration(duration);
		// colorizer.start();

		// // Animate a drop-shadow of the image
		// ObjectAnimator shadowAnim = ObjectAnimator.ofFloat(mShadowLayout, "shadowDepth", 0, 1);
		// shadowAnim.setDuration(duration);
		// shadowAnim.start();
	}
	
	 /**
     * The exit animation is basically a reverse of the enter animation, except that if
     * the orientation has changed we simply scale the picture back into the center of
     * the screen.
     * 
     * @param endAction This action gets run after the animation completes (this is
     * when we actually switch activities)
     */
    public void runExitAnimation(final Runnable endAction) {
        final long duration = (long) (ANIM_DURATION);

        // No need to set initial values for the reverse animation; the image is at the
        // starting size/location that we want to start from. Just animate to the
        // thumbnail size/location that we retrieved earlier 
        

        // First, slide/fade text out of the way
//        mTextView.animate().translationY(-mTextView.getHeight()).alpha(0).
//                setDuration(duration/2).setInterpolator(sAccelerator).
//                withEndAction(new Runnable() {
//                    public void run() {
//                      
//                    }
//                });
        
        // Animate image back to thumbnail size/location
        imageView.animate().setDuration(300).
                scaleX(mWidthScale).scaleY(mHeightScale).
                translationX(mLeftDelta).translationY(mTopDelta).alpha(0f).
                withEndAction(endAction);
//        	imageView.animate().alpha(0);
        // Fade out background
        ObjectAnimator bgAnim = ObjectAnimator.ofFloat(background, "alpha", 0);
        bgAnim.setDuration(300);
        bgAnim.start();

//        // Animate the shadow of the image
//        ObjectAnimator shadowAnim = ObjectAnimator.ofFloat(mShadowLayout,
//                "shadowDepth", 1, 0);
//        shadowAnim.setDuration(duration);
//        shadowAnim.start();

        // Animate a color filter to take the image back to grayscale,
        // in parallel with the image scaling and moving into place.
//        ObjectAnimator colorizer =
//                ObjectAnimator.ofFloat(PictureDetailsActivity.this,
//                "saturation", 1, 0);
//        colorizer.setDuration(duration);
//        colorizer.start();

        
    }
    
    @Override
    public void onBackPressed() {
        runExitAnimation(new Runnable() {
            public void run() {
                finish();
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        
        // override transitions to skip the standard window animations
        overridePendingTransition(0, 0);
    }
	private void showImage()
	{
		key = mappedId;

		if (!isStatusImage)
		{
			int idx = key.lastIndexOf(ProfileActivity.PROFILE_PIC_SUFFIX);

			if (idx > 0)
			{
				key = new String(key.substring(0, idx));
			}
		}
		ProfileImageLoader profileImageLoader = new ProfileImageLoader(ImageViewerActivity.this, key, imageView, imageSize, isStatusImage);
		profileImageLoader.setLoaderListener(new ProfileImageLoader.LoaderListener()
		{

			@Override
			public void onLoaderReset(Loader<Boolean> arg0)
			{
				// dismissProgressDialog();
			}

			@Override
			public void onLoadFinished(Loader<Boolean> arg0, Boolean arg1)
			{
				// dismissProgressDialog();
				if (isStatusImage)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED, null);
				}
			}

			@Override
			public Loader<Boolean> onCreateLoader(int arg0, Bundle arg1)
			{
				mDialog = ProgressDialog.show(ImageViewerActivity.this, null, getResources().getString(R.string.downloading_image));
				mDialog.setCancelable(true);
				return null;
			}
		});
		profileImageLoader.loadProfileImage(getSupportLoaderManager());
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		// dismissProgressDialog();
		HikeMessengerApp.getPubSub().removeListeners(this, profilePicPubSubListeners);
	}

	// @Override
	// public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	// {
	// menu.clear();
	//
	// if (isViewEditable)
	// {
	// inflater.inflate(R.menu.edit_dp, menu);
	// }
	// }
	//
	// @Override
	// public boolean onOptionsItemSelected(MenuItem item)
	// {
	// switch (item.getItemId())
	// {
	// case R.id.edit_dp:
	// if (mProfilePhotoEditListener != null)
	// {
	// mProfilePhotoEditListener.onDisplayPictureEditClicked(whichActivity);
	// }
	// break;
	// }
	// return true;
	// }

	// @Override
	// public void onAttach(Activity activity)
	// {
	// super.onAttach(activity);
	//
	// if (activity instanceof SettingsActivity)
	// {
	// whichActivity = FROM_SETTINGS_ACTIVITY;
	// }
	// else if (activity instanceof ProfileActivity)
	// {
	// whichActivity = FROM_PROFILE_ACTIVITY;
	// }
	//
	// isViewEditable = extras.getBoolean(HikeConstants.CAN_EDIT_DP);
	//
	// if (isViewEditable)
	// {
	// // activity should implement DisplayPictureEditListener interface
	// try
	// {
	// mProfilePhotoEditListener = (DisplayPictureEditListener) activity;
	// }
	// catch (ClassCastException e)
	// {
	// throw new ClassCastException(activity.toString() + " must implement DisplayPictureEditListener");
	// }
	// }
	// }
	//
	// private void dismissProgressDialog()
	// {
	// if (mDialog != null)
	// {
	// mDialog.dismiss();
	// mDialog = null;
	// }
	// }

	@Override
	public void onClick(View v)
	{
		// /*
		// * This object can become null, if the method is called when the fragment is not attached with the activity. In that case we do nothing and return.
		// */
		// if (getActivity() == null)
		// {
		// return;
		// }
		// getActivity().onBackPressed();
	}

	public interface DisplayPictureEditListener
	{
		public void onDisplayPictureEditClicked(int fromWhichActivity);
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.ICON_CHANGED.equals(type))
		{
			ContactInfo contactInfo = Utils.getUserContactInfo(ImageViewerActivity.this.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0));

			if (contactInfo.getMsisdn().equals((String) object))
			{
				ImageViewerActivity.this.runOnUiThread(new Runnable()
				{

					@Override
					public void run()
					{
						showImage();
					}
				});
			}
		}
	}
}
