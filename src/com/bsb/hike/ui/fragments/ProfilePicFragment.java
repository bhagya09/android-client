package com.bsb.hike.ui.fragments;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.HoloCircularProgress;
import com.bsb.hike.view.RoundedImageView;

public class ProfilePicFragment extends SherlockFragment implements FinishableEvent
{
	private View mFragmentView;

	private HoloCircularProgress mCircularProgress;

	float mCurrentProgress = 0;

	private TextView text1;

	private TextView text2;

	private Interpolator animInterpolator = new LinearInterpolator();

	private String imagePath;

	private boolean failed;

	private RoundedImageView mCircularImageView;

	private boolean finished;

	private ImageView mProfilePicBg;

	private Bitmap smallerBitmap;

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

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.RGB_565;
		Bitmap bmp = BitmapFactory.decodeFile(imagePath, options);
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

		changeTextWithAnimation(text1, getString(R.string.photo_dp_saving));

		changeTextWithAnimation(text2, "");

		mCircularProgress.setProgressColor(getResources().getColor(R.color.photos_circular_progress_blue));

		mCircularProgress.resetProgress();

		mFragmentView.findViewById(R.id.retryButton).setVisibility(View.GONE);

		mFragmentView.findViewById(R.id.rounded_mask).setVisibility(View.GONE);

		if (imagePath != null)
		{

			// TODO move this code to network manager refactoring module
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

			String httpRequestURL = "/account";

			HikeHttpRequest request = new HikeHttpRequest(httpRequestURL + "/avatar", RequestType.PROFILE_PIC, new HikeHttpRequest.HikeHttpCallback()
			{
				public void onFailure()
				{
					showErrorState();
				}

				public void onSuccess(JSONObject response)
				{

					// User info is saved in shared preferences
					SharedPreferences preferences = HikeMessengerApp.getInstance().getApplicationContext()
							.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE);

					ContactInfo userInfo = Utils.getUserContactInfo(preferences);

					String mLocalMSISDN = userInfo.getMsisdn();

					ContactManager.getInstance().setIcon(mLocalMSISDN, bytes, false);

					Utils.renameTempProfileImage(mLocalMSISDN);

					/*
					 * Making the profile pic change a status message.
					 */
					JSONObject data = response.optJSONObject("status");

					if (data == null)
					{
						return;
					}

					String mappedId = data.optString(HikeConstants.STATUS_ID);
					String msisdn = preferences.getString(HikeMessengerApp.MSISDN_SETTING, "");
					String name = preferences.getString(HikeMessengerApp.NAME_SETTING, "");
					long time = (long) System.currentTimeMillis() / 1000;

					StatusMessage statusMessage = new StatusMessage(0, mappedId, msisdn, name, "", StatusMessageType.PROFILE_PIC, time, -1, 0);

					HikeConversationsDatabase.getInstance().addStatusMessage(statusMessage, true);

					ContactManager.getInstance().setIcon(statusMessage.getMappedId(), bytes, false);

					String srcFilePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT + "/" + msisdn + ".jpg";

					String destFilePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT + "/" + mappedId + ".jpg";

					/*
					 * Making a status update file so we don't need to download this file again.
					 */
					Utils.copyFile(srcFilePath, destFilePath, null);

					int unseenUserStatusCount = preferences.getInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0);
					Editor editor = preferences.edit();
					editor.putInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, ++unseenUserStatusCount);
					editor.putBoolean(HikeConstants.IS_HOME_OVERFLOW_CLICKED, false);
					editor.commit();

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

					updateProgress(90f - mCurrentProgress);
				}
			});

			request.setFilePath(imagePath);

			Utils.executeHttpTask(new HikeHTTPTask(ProfilePicFragment.this, R.string.delete_status_error), request);

			updateProgressUniformly(80f, 10f);
		}
	}

	private void updateProgressUniformly(final float total, final float interval)
	{
		if (total <= 0.0f || failed || mCurrentProgress >= 100)
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

				if (mCircularProgress.getProgress() >= 1f || failed)
				{
					animation.cancel();
				}
			}
		});
		mAnim.start();

		mCurrentProgress += i;

		if (mCurrentProgress >= 90f && !failed && !finished)
		{
			finished = true;

			changeTextWithAnimation(text1, getString(R.string.photo_dp_finishing));

			new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					if (!isAdded())
					{
						return;
					}
					updateProgress(10f);
					changeTextWithAnimation(text1, getString(R.string.photo_dp_saved));
				}
			}, 1000);

			changeTextWithAnimation(text2, getString(R.string.photo_dp_saved_sub));

			HikeHandlerUtil.getInstance().postRunnableWithDelay(new Runnable()
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
							if (isAdded() && !failed)
							{
								Intent in = new Intent(getActivity(), HomeActivity.class);
								in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								getActivity().startActivity(in);
							}
						}
					});
				}
			}, 4000);
		}

	}

	private void showErrorState()
	{

		failed = true;

		if (!isAdded())
		{
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
				failed = false;
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
		try
		{
			getActivity().getSupportFragmentManager().popBackStack();
			getActivity().getActionBar().show();
		}
		catch (NullPointerException npe)
		{
			// Do nothing
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
	}

	@Override
	public void onFinish(boolean success)
	{
		// TODO Auto-generated method stub

	}
}
