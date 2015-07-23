package com.bsb.hike.ui.fragments;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.internal.nineoldandroids.animation.Animator;
import com.actionbarsherlock.internal.nineoldandroids.animation.AnimatorListenerAdapter;
import com.actionbarsherlock.internal.nineoldandroids.animation.FloatEvaluator;
import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import com.actionbarsherlock.internal.nineoldandroids.animation.ValueAnimator;
import com.actionbarsherlock.internal.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.ui.TimelineActivity;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeUiHandler;
import com.bsb.hike.utils.HikeUiHandler.IHandlerCallback;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.HoloCircularProgress;
import com.bsb.hike.view.RoundedImageView;

public class ProfilePicFragment extends SherlockFragment implements FinishableEvent, IHandlerCallback, HeadlessImageWorkerFragment.TaskCallbacks
{
	private View mFragmentView;

	private HoloCircularProgress mCircularProgress;

	float mCurrentProgress = 0;

	private TextView text1;

	private TextView text2;

	private Interpolator animInterpolator = new LinearInterpolator();

	private String imagePath;
	
	private byte mUploadStatus = -1;
	
	private final byte UPLOAD_COMPLETE = 1;
	
	private final byte UPLOAD_FAILED = 2;
	
	private final byte UPLOAD_INPROGRESS = 3;
	
	private RoundedImageView mCircularImageView;

	private ImageView mProfilePicBg;

	private Bitmap smallerBitmap;

	private String origImagePath;
	
	private HikeUiHandler hikeUiHandler;
	
	private HeadlessImageUploaderFragment mImageWorkerFragment;
	
	private static final String TAG = "dp_download";

	private Runnable failedRunnable = new Runnable()
	{
		
		@Override
		public void run()
		{
			if(isAdded() && isVisible())
			{
				Logger.d(TAG, "inside ImageViewerFragment, onFailed Recv");
				showErrorState();
				removeHeadLessFragment();
			}
		}
	};
	
	private Runnable successRunnable = new Runnable()
	{
		
		@Override
		public void run()
		{
			if(isAdded() && isVisible())
			{
				Logger.d(TAG, "inside ImageViewerFragment, onSucecess Recv");
				updateProgress(90f - mCurrentProgress);
				removeHeadLessFragment();
			}
		}
	};
	
	public void onActivityCreated(Bundle savedInstanceState) 
	{
		super.onActivityCreated(savedInstanceState);
		hikeUiHandler = new HikeUiHandler(this);
	};
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		mFragmentView = inflater.inflate(R.layout.profile_pic_fragment, null);

		mCircularProgress = (HoloCircularProgress) mFragmentView.findViewById(R.id.circular_progress);

		mCircularProgress.setProgress(0);

		mCircularImageView = ((RoundedImageView) mFragmentView.findViewById(R.id.circular_image_view));

		mProfilePicBg = ((ImageView) mFragmentView.findViewById(R.id.profile_pic_bg));

		Bundle bundle = getArguments();

		imagePath = bundle.getString(HikeConstants.HikePhotos.FILENAME);
		
		origImagePath = bundle.getString(HikeConstants.HikePhotos.ORIG_FILE);

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.RGB_565;
		Bitmap bmp = HikeBitmapFactory.decodeFile(imagePath, options);
		if (bmp != null)
		{
			mCircularImageView.setImageBitmap(bmp);
		}

		mProfilePicBg.setImageBitmap(bmp);

		text1 = (TextView) mFragmentView.findViewById(R.id.text1);

		text2 = (TextView) mFragmentView.findViewById(R.id.text2);

		new Handler().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				if (!isAdded())
				{
					return;
				}

				ObjectAnimator objectAnimatorButton = ObjectAnimator.ofFloat(mCircularImageView, "translationY", 100f, 0f);
				objectAnimatorButton.setDuration(500);
				objectAnimatorButton.start();
				ObjectAnimator objectAnimatorButton2 = ObjectAnimator.ofFloat(mCircularProgress, "translationY", 100f, 0f);
				objectAnimatorButton2.setDuration(500);
				objectAnimatorButton2.start();
				mCircularImageView.setVisibility(View.VISIBLE);
				mCircularProgress.setVisibility(View.VISIBLE);
				mProfilePicBg.setVisibility(View.VISIBLE);

				((HikeAppStateBaseFragmentActivity) getActivity()).getSupportActionBar().hide();
				startUpload();
			}
		}, 300);

		return mFragmentView;
	}

	private void startUpload()
	{

		mUploadStatus = UPLOAD_INPROGRESS;
		
		changeTextWithAnimation(text1, getString(R.string.photo_dp_saving));

		changeTextWithAnimation(text2, "");

		mCircularProgress.setProgressColor(getResources().getColor(R.color.photos_circular_progress_blue));

		mCircularProgress.resetProgress();

		mFragmentView.findViewById(R.id.retryButton).setVisibility(View.GONE);

		mFragmentView.findViewById(R.id.rounded_mask).setVisibility(View.GONE);

		if (imagePath != null)
		{
			if (smallerBitmap == null)
			{
				/* the server only needs a smaller version */
				smallerBitmap = HikeBitmapFactory.scaleDownBitmap(imagePath, HikeConstants.PROFILE_IMAGE_DIMENSIONS, HikeConstants.PROFILE_IMAGE_DIMENSIONS, Bitmap.Config.RGB_565,
						true, false);
			}

			if (smallerBitmap == null)
			{
				showErrorState();
			}

			final byte[] bytes = BitmapUtils.bitmapToBytes(smallerBitmap, Bitmap.CompressFormat.JPEG, 100);

			// User info is saved in shared preferences
			SharedPreferences preferences = HikeMessengerApp.getInstance().getApplicationContext()
					.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE);

			ContactInfo userInfo = Utils.getUserContactInfo(preferences);

			String mLocalMSISDN = userInfo.getMsisdn();
			
			loadHeadLessImageUploadingFragment(bytes, origImagePath, mLocalMSISDN);
			
			updateProgressUniformly(80f, 10f);
		}
	}

	public void loadHeadLessImageUploadingFragment(byte[] bytes, String origImagePath, String mLocalMSISDN)
	{
		Logger.d(TAG, "inside API loadHeadLessImageUploadingFragment");
		FragmentManager fm = getFragmentManager();
		mImageWorkerFragment = (HeadlessImageUploaderFragment) fm.findFragmentByTag(HikeConstants.TAG_HEADLESS_IMAGE_UPLOAD_FRAGMENT);

	    // If the Fragment is non-null, then it is currently being
	    // retained across a configuration change.
	    if (mImageWorkerFragment == null) 
	    {
	    	Logger.d("dp_upload", "starting new mImageLoaderFragment");
	    	mImageWorkerFragment = HeadlessImageUploaderFragment.newInstance(bytes, origImagePath, mLocalMSISDN, false, true);
	    	mImageWorkerFragment.setTaskCallbacks(this);
	        fm.beginTransaction().add(mImageWorkerFragment, HikeConstants.TAG_HEADLESS_IMAGE_UPLOAD_FRAGMENT).commit();
	    }
	    else
	    {
	    	Toast.makeText(getActivity(), getString(R.string.task_already_running), Toast.LENGTH_SHORT).show();
	    	Logger.d(TAG, "As mImageLoaderFragment already there, so not starting new one");
	    }

	}
	
	private void updateProgressUniformly(final float total, final float interval)
	{
		if (total <= 0.0f || mUploadStatus == UPLOAD_FAILED || mCurrentProgress >= 100)
		{
			return;
		}

		new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				updateProgress(interval);
				updateProgressUniformly(total - interval, interval);
			}
		}, 1300);
	}

	private void updateProgress(float i)
	{

		if (!isAdded())
		{
			return;
		}

		ValueAnimator mAnim = ObjectAnimator.ofFloat(mCurrentProgress, mCurrentProgress + i);
		mAnim.setInterpolator(animInterpolator);
		mAnim.setEvaluator(new FloatEvaluator());
		mAnim.setDuration(1300);
		mAnim.addUpdateListener(new AnimatorUpdateListener()
		{
			@Override
			public void onAnimationUpdate(final ValueAnimator animation)
			{
				Float value = (Float) animation.getAnimatedValue();

				mCircularProgress.setProgress(value / 100f);

				if (mCircularProgress.getProgress() >= 1f || mUploadStatus == UPLOAD_FAILED)
				{
					animation.cancel();
				}
			}
		});
		mAnim.start();

		mCurrentProgress += i;

		if (mCurrentProgress >= 90f && mUploadStatus == UPLOAD_INPROGRESS)
		{
			changeTextWithAnimation(text1, getString(R.string.photo_dp_finishing));

			new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					mUploadStatus = UPLOAD_COMPLETE;
					if (!isAdded())
					{
						return;
					}
					updateProgress(10f);
					changeTextWithAnimation(text1, getString(R.string.photo_dp_saved));
				}
			}, 1000);

			changeTextWithAnimation(text2, getString(R.string.photo_dp_saved_sub));

			HikeHandlerUtil.getInstance().postRunnableWithDelay(timelineLauncherRunnable, 3000);
		}

	}
	
	private Runnable timelineLauncherRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			if (!isAdded())
			{
				return;
			}
			ProfilePicFragment.this.getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if (isAdded() && mUploadStatus == UPLOAD_COMPLETE && isResumed())
					{
						Intent in = new Intent(getActivity(), TimelineActivity.class);
						in.putExtra(HikeConstants.HikePhotos.FROM_DP_UPLOAD, true);
						getActivity().startActivity(in);
						getActivity().finish();
					}
				}
			});
		}
	};

	private void showErrorState()
	{
		mUploadStatus = UPLOAD_FAILED;

		if (!isAdded())
		{
			Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), R.string.profile_pic_failed, Toast.LENGTH_SHORT).show();
			return;
		}

		mCircularProgress.setProgress(1f);

		changeTextWithAnimation(text1, getString(R.string.photo_dp_save_error));

		changeTextWithAnimation(text2, getString(R.string.photo_dp_save_error_sub));

		mCircularProgress.setProgressColor(getResources().getColor(R.color.photos_circular_progress_red));

		mFragmentView.findViewById(R.id.retryButton).setVisibility(View.VISIBLE);

		mFragmentView.findViewById(R.id.rounded_mask).setVisibility(View.VISIBLE);

		mFragmentView.findViewById(R.id.retryButton).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mCurrentProgress = 0.0f;
				mUploadStatus = UPLOAD_INPROGRESS;
				startUpload();
			}
		});
	}

	private void changeTextWithAnimation(final TextView tv, final String newText)
	{
		ObjectAnimator visToInvis = ObjectAnimator.ofFloat(tv, "alpha", 1f, 0.2f);
		visToInvis.setDuration(250);
		visToInvis.setInterpolator(animInterpolator);
		visToInvis.addListener(new AnimatorListenerAdapter()
		{
			@Override
			public void onAnimationEnd(Animator animation)
			{
				super.onAnimationEnd(animation);
				tv.setText(newText);
				ObjectAnimator invisToVis = ObjectAnimator.ofFloat(tv, "alpha", 0.2f, 1f);
				invisToVis.setDuration(250);
				invisToVis.setInterpolator(animInterpolator);
				invisToVis.start();
			}
		});
		visToInvis.start();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		if (mUploadStatus == UPLOAD_INPROGRESS)
		{
			HikeHandlerUtil.getInstance().removeRunnable(null);
			Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), R.string.profile_pic_upload_in_background, Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		if(mUploadStatus == UPLOAD_COMPLETE || mUploadStatus == UPLOAD_FAILED)
		{
			removeHeadLessFragment();
		}
		
		if (mUploadStatus == UPLOAD_COMPLETE)
		{
			timelineLauncherRunnable.run();
		}
	}

	@Override
	public void onFinish(boolean success)
	{
		// Do nothing
	}

	@Override
	public void onProgressUpdate(float percent)
	{
		
	}

	@Override
	public void onCancelled()
	{
		
	}

	@Override
	public void onFailed()
	{
		hikeUiHandler.post(failedRunnable);
	}

	@Override
	public void onSuccess(Response result)
	{
		// User info is saved in shared preferences
		SharedPreferences preferences = HikeMessengerApp.getInstance().getApplicationContext()
				.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE);

		ContactInfo userInfo = Utils.getUserContactInfo(preferences);

		String mLocalMSISDN = userInfo.getMsisdn();

		JSONObject response = (JSONObject) result.getBody().getContent();
		
		StatusMessage statusMessage = Utils.createTimelinePostForDPChange(response, true);

		Utils.incrementUnseenStatusCount();

		/*
		 * This would happen in the case where the user has added a self contact and received an mqtt message before saving this to the db.
		 */

		if (statusMessage.getId() != -1)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.STATUS_MESSAGE_RECEIVED, statusMessage);
			HikeMessengerApp.getPubSub().publish(HikePubSub.TIMELINE_UPDATE_RECIEVED, statusMessage);
		}

		HikeMessengerApp.getLruCache().clearIconForMSISDN(mLocalMSISDN);
		
		HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, mLocalMSISDN);

		HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_UPDATE_FINISH, null);
		
		hikeUiHandler.post(successRunnable);
	}

	@Override
	public void handleUIMessage(Message msg)
	{
		// TODO Auto-generated method stub
		
	}
	
	private void removeHeadLessFragment()
	{
		Logger.d(TAG, "inside ImageViewerFragment, removing UILessFragment");
		
		mImageWorkerFragment = (HeadlessImageUploaderFragment)getFragmentManager().findFragmentByTag(HikeConstants.TAG_HEADLESS_IMAGE_UPLOAD_FRAGMENT);
		if(mImageWorkerFragment !=null && isResumed())
		{
			getFragmentManager().beginTransaction().remove(mImageWorkerFragment).commit();
		}
	}
}
