package com.bsb.hike.ui;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.internal.nineoldandroids.animation.Animator;
import com.actionbarsherlock.internal.nineoldandroids.animation.Animator.AnimatorListener;
import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.timeline.adapter.DisplayContactsAdapter;
import com.bsb.hike.ui.fragments.HeadlessImageDownloaderFragment;
import com.bsb.hike.ui.fragments.HeadlessImageWorkerFragment;
import com.bsb.hike.utils.HikeUiHandler;
import com.bsb.hike.utils.HikeUiHandler.IHandlerCallback;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.ProfileImageLoader;
import com.bsb.hike.utils.Utils;

/**
 * 
 * TODO Need to make single base class for ImageViewerActivity and ImageViewerFragment. Currently we are copy-pasting code.
 * 
 * @author Atul M
 * 
 */
public class ImageViewerActivity extends SherlockFragmentActivity implements OnClickListener, Listener, IHandlerCallback, HeadlessImageWorkerFragment.TaskCallbacks
{
	ImageView imageView;

	private String mappedId;

	private String key;

	private boolean isStatusImage;

	private int imageSize;

	private String[] profilePicPubSubListeners = { HikePubSub.ICON_CHANGED };

	private View fadeScreen;

	private final String TAG = ImageViewerActivity.class.getSimpleName();

	private String fileKey;

	private HeadlessImageDownloaderFragment mImageWorkerFragment;

	private boolean hasCustomImage;

	private ProfileImageLoader profileImageLoader;

	private Runnable failedRunnable = new Runnable()
	{
		@Override
		public void run()
		{
		}
	};

	private Runnable cancelledRunnable = new Runnable()
	{
		@Override
		public void run()
		{
		}
	};

	private Runnable successRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			profileImageLoader.loadFromFile();
		}
	};

	private HikeUiHandler hikeUiHandler;

	private ArrayList<String> msisdns;

	private String imageCaption;

	private View infoContainer;

	private TextView textViewCaption;

	private TextView textViewCounts;

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

		msisdns = extras.getStringArrayList(HikeConstants.MSISDNS);

		imageCaption = extras.getString(HikeConstants.Extras.IMAGE_CAPTION);

		imageSize = getApplicationContext().getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size);

		ViewTreeObserver observer = imageView.getViewTreeObserver();

		observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener()
		{
			@Override
			public boolean onPreDraw()
			{
				imageView.getViewTreeObserver().removeOnPreDrawListener(this);

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
		infoContainer = findViewById(R.id.image_info_container);
		textViewCaption = (TextView) findViewById(R.id.text_view_caption);
		textViewCounts = (TextView) findViewById(R.id.text_view_count);
		imageView.setOnClickListener(this);
		hikeUiHandler = new HikeUiHandler(this);
	}

	private static final int ANIM_DURATION = 300;

	public void runEnterAnimation()
	{
		final long duration = (long) (ANIM_DURATION * 1);
		imageView.setTranslationY(20);
		imageView.setAlpha(0f);

		imageView.animate().setDuration(duration).translationY(0).alpha(1f).withEndAction(new Runnable()
		{
			public void run()
			{
				infoContainer.setVisibility(View.VISIBLE);
			}
		});

		ObjectAnimator bgAnim = ObjectAnimator.ofFloat(fadeScreen, "alpha", 0f, 0.95f);
		bgAnim.setDuration(600);
		bgAnim.start();

		textViewCaption.setText(imageCaption);

		// TODO Make this generic for all action types
		textViewCounts.setText(String.format(getString(R.string.post_likes), msisdns.size()));

		textViewCounts.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				showLikesContactsDialog();
			}
		});
	}

	/**
	 * The exit animation is basically a reverse of the enter animation, except that if the orientation has changed we simply scale the picture back into the center of the screen.
	 * 
	 * @param endAction
	 *            This action gets run after the animation completes (this is when we actually switch activities)
	 */
	public void runExitAnimation(final Runnable endAction)
	{
		imageView.animate().setDuration(300).translationY(20).alpha(0f);
		ObjectAnimator bgAnim = ObjectAnimator.ofFloat(fadeScreen, "alpha", 0);
		bgAnim.setDuration(600);
		bgAnim.addListener(new AnimatorListener()
		{
			@Override
			public void onAnimationStart(Animator animation)
			{
				// Do nothing
			}

			@Override
			public void onAnimationRepeat(Animator animation)
			{
				// Do nothing
			}

			@Override
			public void onAnimationEnd(Animator animation)
			{
				endAction.run();
			}

			@Override
			public void onAnimationCancel(Animator animation)
			{
				// Do nothing
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
			public Loader<Boolean> onCreateLoader(int arg0, Bundle arg1) {
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

	public void onCancelled()
	{
		hikeUiHandler.post(cancelledRunnable);
	}

	public void onSuccess(Response result)
	{
		hikeUiHandler.post(successRunnable);
	}

	public void onFailed()
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_IMAGE_NOT_DOWNLOADED, key);
		hikeUiHandler.post(failedRunnable);
	}

	@Override
	public void handleUIMessage(Message msg)
	{
		// TODO Auto-generated method stub
	}

	public boolean hasFileKey()
	{
		if (!TextUtils.isEmpty(fileKey))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	// TODO Make this generic for all action types
	// TODO Move to HikeDialogFactory
	public void showLikesContactsDialog()
	{
		final HikeDialog dialog = new HikeDialog(ImageViewerActivity.this, R.style.Theme_CustomDialog, 11);
		dialog.setContentView(R.layout.display_contacts_dialog);
		dialog.setCancelable(true);

		ListView listContacts = (ListView) dialog.findViewById(R.id.listContacts);
		DisplayContactsAdapter contactsAdapter = new DisplayContactsAdapter(msisdns);
		listContacts.setAdapter(contactsAdapter);
		listContacts.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
			{
				Intent intent = IntentFactory.createChatThreadIntentFromContactInfo(ImageViewerActivity.this,
						ContactManager.getInstance().getContactInfoFromPhoneNoOrMsisdn(msisdns.get(position)), true);
				// Add anything else to the intent
				intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});

		ImageButton cancelButton = (ImageButton) dialog.findViewById(R.id.btn_cancel);
		cancelButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				dialog.dismiss();
			}
		});

		dialog.show();
	}
}
