package com.bsb.hike.ui;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import com.actionbarsherlock.internal.nineoldandroids.animation.Animator;
import com.actionbarsherlock.internal.nineoldandroids.animation.Animator.AnimatorListener;
import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.fragments.HeadlessImageDownloaderFragment;
import com.bsb.hike.ui.fragments.HeadlessImageWorkerFragment;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.ProfileImageLoader;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.HikeUiHandler.IHandlerCallback;

public class ImageViewerActivity extends FragmentActivity implements OnClickListener, Listener, IHandlerCallback, HeadlessImageWorkerFragment.TaskCallbacks
{
	ImageView imageView;

	private ProgressDialog mDialog;

	private String mappedId;

	private String key;

	private boolean isStatusImage;

	private int imageSize;

	private String[] profilePicPubSubListeners = { HikePubSub.ICON_CHANGED };

	private View fadeScreen;

	int mLeftDelta;

	int mTopDelta;

	float mWidthScale;

	float mHeightScale;

	private final String TAG = ImageViewerActivity.class.getSimpleName();

	public static final String animFromLeft = "animFromLeft";

	public static final String animFromTop = "animFromTop";

	public static final String animFromWidth = "animFromWidth";

	public static final String animFromHeight = "animFromHeight";

	public static final String FILE_TYPE_KEY = "ftk";

	public static final int SHOW_PROFILE_PIC = -11;

	private int fileType;

	private String fileKey;

	private HeadlessImageDownloaderFragment mImageWorkerFragment;

	private boolean hasCustomImage;

	private ProfileImageLoader profileImageLoader;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		overridePendingTransition(0, 0);

		super.onCreate(savedInstanceState);

		setContentView(R.layout.image_viewer_activity);

		initReferences();

		HikeMessengerApp.getPubSub().addListeners(this, profilePicPubSubListeners);

		Bundle extras = getIntent().getExtras();

		mappedId = extras.getString(HikeConstants.Extras.MAPPED_ID);

		fileKey = extras.getString(HikeConstants.Extras.FILE_KEY);

		isStatusImage = extras.getBoolean(HikeConstants.Extras.IS_STATUS_IMAGE);

		imageSize = getApplicationContext().getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size);

		fileType = extras.getInt(FILE_TYPE_KEY, SHOW_PROFILE_PIC);

		final int thumbnailTop = extras.getInt(animFromTop);

		final int thumbnailLeft = extras.getInt(animFromLeft);

		final int thumbnailWidth = extras.getInt(animFromWidth);

		final int thumbnailHeight = extras.getInt(animFromHeight);

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

	private void initReferences()
	{
		imageView = (ImageView) findViewById(R.id.image);
		fadeScreen = findViewById(R.id.bg_screen);

		imageView.setOnClickListener(this);
	}

	private static final int ANIM_DURATION = 300;

	public void runEnterAnimation()
	{
		final long duration = (long) (ANIM_DURATION * 1);

		// Set starting values for properties we're going to animate. These
		// values scale and position the full size version down to the thumbnail
		// size/location, from which we'll animate it back up
		// imageView.setPivotX(0);
		// imageView.setPivotY(0);
		// imageView.setScaleX(mWidthScale);
		// imageView.setScaleY(mHeightScale < 0.6f ? 0.6f : mHeightScale);
		// imageView.setTranslationX(mLeftDelta);
		// imageView.setTranslationY(mTopDelta);
		imageView.setTranslationY(20);
		imageView.setAlpha(0f);

		// Animate scale and translation to go from thumbnail to full size
		// imageView.animate().setDuration(duration).scaleX(1).scaleY(1).translationX(0).translationY(0).withEndAction(new Runnable()
		// {
		// public void run()
		// {
		// // Animate the description in after the image animation
		// // is done. Slide and fade the text in from underneath
		// // the picture.
		//
		// }
		// });

		imageView.animate().setDuration(duration).translationY(0).alpha(1f).withEndAction(new Runnable()
		{
			public void run()
			{
				// Animate the description in after the image animation
				// is done. Slide and fade the text in from underneath
				// the picture.

			}
		});

		ObjectAnimator bgAnim = ObjectAnimator.ofFloat(fadeScreen, "alpha", 0f, 0.95f);
		bgAnim.setDuration(600);
		bgAnim.start();
	}

	/**
	 * The exit animation is basically a reverse of the enter animation, except that if the orientation has changed we simply scale the picture back into the center of the screen.
	 * 
	 * @param endAction
	 *            This action gets run after the animation completes (this is when we actually switch activities)
	 */
	public void runExitAnimation(final Runnable endAction)
	{
		// Animate image back to thumbnail size/location
		// imageView.animate().setDuration(300).scaleX(mWidthScale).scaleY(mHeightScale).translationX(mLeftDelta).translationY(mTopDelta).withEndAction(endAction);
		imageView.animate().setDuration(300).translationY(20).alpha(0f);
		ObjectAnimator bgAnim = ObjectAnimator.ofFloat(fadeScreen, "alpha", 0);
		bgAnim.setDuration(600);
		bgAnim.addListener(new AnimatorListener()
		{

			@Override
			public void onAnimationStart(Animator animation)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationRepeat(Animator animation)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animator animation)
			{
				endAction.run();

			}

			@Override
			public void onAnimationCancel(Animator animation)
			{
				// TODO Auto-generated method stub

			}
		});
		bgAnim.start();
	}

	@Override
	public void onBackPressed()
	{
		runExitAnimation(new Runnable()
		{
			public void run()
			{
				finish();
			}
		});
	}

	@Override
	public void finish()
	{
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

		hasCustomImage = isStatusImage || ContactManager.getInstance().hasIcon(key);

		profileImageLoader = new ProfileImageLoader(this, key, imageView, imageSize, isStatusImage, true);
		profileImageLoader.setLoaderListener(new ProfileImageLoader.LoaderListener()
		{

			@Override
			public void onLoaderReset(Loader<Boolean> arg0)
			{
				dismissProgressDialog();
			}

			@Override
			public void onLoadFinished(Loader<Boolean> arg0, Boolean arg1)
			{
				dismissProgressDialog();
				if (isStatusImage)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED, null);
				}
			}

			@Override
			public Loader<Boolean> onCreateLoader(int arg0, Bundle arg1)
			{
				showProgressDialog();
				return null;
			}

			@Override
			public void startDownloading()
			{
				showProgressDialog();
				loadHeadLessImageDownloadingFragment();
			}
		});
		profileImageLoader.loadProfileImage(getSupportLoaderManager());
	}

	protected void showProgressDialog()
	{
		// TODO Auto-generated method stub

	}

	protected void dismissProgressDialog()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		// dismissProgressDialog();
		HikeMessengerApp.getPubSub().removeListeners(this, profilePicPubSubListeners);
	}

	@Override
	public void onClick(View v)
	{
		onBackPressed();
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

	private void loadHeadLessImageDownloadingFragment()
	{
		Logger.d(TAG, "isnide API loadHeadLessImageDownloadingFragment");
		FragmentManager fm = getSupportFragmentManager();
		mImageWorkerFragment = (HeadlessImageDownloaderFragment) fm.findFragmentByTag(HikeConstants.TAG_HEADLESS_IMAGE_DOWNLOAD_FRAGMENT);

		// If the Fragment is non-null, then it is currently being
		// retained across a configuration change.
		if (mImageWorkerFragment == null)
		{
			Logger.d(TAG, "starting new mImageLoaderFragment");
			String fileName = Utils.getProfileImageFileName(key);
			mImageWorkerFragment = HeadlessImageDownloaderFragment.newInstance(key, fileName, hasCustomImage, isStatusImage, null, null, null, true);
			mImageWorkerFragment.setTaskCallbacks(this);
			fm.beginTransaction().add(mImageWorkerFragment, HikeConstants.TAG_HEADLESS_IMAGE_DOWNLOAD_FRAGMENT).commit();
		}
		else
		{
			Toast.makeText(this, getString(R.string.task_already_running), Toast.LENGTH_SHORT).show();
			Logger.d(TAG, "As mImageLoaderFragment already there, so not starting new one");
		}

	}

	@Override
	public void onProgressUpdate(float percent)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onCancelled()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onFailed()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onSuccess(Response result)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void handleUIMessage(Message msg)
	{
		// TODO Auto-generated method stub

	}
}
