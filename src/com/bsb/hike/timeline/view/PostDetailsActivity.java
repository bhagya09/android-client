package com.bsb.hike.timeline.view;

import java.util.ArrayList;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.timeline.adapter.DisplayContactsAdapter;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.ui.fragments.HeadlessImageDownloaderFragment;
import com.bsb.hike.ui.fragments.HeadlessImageWorkerFragment;
import com.bsb.hike.utils.HikeUiHandler;
import com.bsb.hike.utils.HikeUiHandler.IHandlerCallback;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.ProfileImageLoader;
import com.bsb.hike.utils.Utils;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.ObjectAnimator;

/**
 * 
 * TODO Need to make single base class for this and ImageViewerFragment. Currently we are copy-pasting code.
 * 
 * @author Atul M
 * 
 */
public class PostDetailsActivity extends AppCompatActivity implements OnClickListener, Listener, IHandlerCallback, HeadlessImageWorkerFragment.TaskCallbacks
{
	ImageView imageView;

	private String mappedId;

	private int imageSize;

	private String[] profilePicPubSubListeners = { HikePubSub.ICON_CHANGED };

	private View fadeScreen;

	private final String TAG = PostDetailsActivity.class.getSimpleName();

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

	private View infoContainer;

	private TextView textViewCaption;

	private TextView textViewCounts;

	private View foregroundScreen;

	private StatusMessage statusMessage;

	private TextView fullTextView;

	private boolean isTextStatusMessage;

	private View contentContainer;

	private View imageInfoDivider;

	private View actionBarView;

	private ActionBar actionBar;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		overridePendingTransition(0, 0);
		
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		super.onCreate(savedInstanceState);

		setContentView(R.layout.image_viewer_activity);

		initReferences();

		HikeMessengerApp.getPubSub().addListeners(this, profilePicPubSubListeners);

		Bundle extras = getIntent().getExtras();

		mappedId = extras.getString(HikeConstants.Extras.MAPPED_ID);

		// TODO think of a better place to do this without breaking animation
		statusMessage = HikeConversationsDatabase.getInstance().getStatusMessageFromMappedId(mappedId);

		msisdns = extras.getStringArrayList(HikeConstants.MSISDNS);

		imageSize = getApplicationContext().getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size);

		ViewTreeObserver observer = contentContainer.getViewTreeObserver();

		observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener()
		{
			@Override
			public boolean onPreDraw()
			{
				contentContainer.getViewTreeObserver().removeOnPreDrawListener(this);

				runEnterAnimation();

				return true;
			}
		});

		if (statusMessage.getStatusMessageType() == StatusMessageType.TEXT)
		{
			isTextStatusMessage = true;
			fullTextView.setText(statusMessage.getText());
			imageView.setVisibility(View.GONE);
			fadeScreen.setBackgroundColor(Color.WHITE);
			textViewCounts.setTextColor(Color.BLACK);
			fullTextView.setTextColor(Color.BLACK);
		}
		else
		{
			showImage();
			fullTextView.setVisibility(View.GONE);
		}

		setupActionBar();
	}

	private void initReferences()
	{
		imageView = (ImageView) findViewById(R.id.image);
		fadeScreen = findViewById(R.id.bg_screen);
		foregroundScreen = findViewById(R.id.fg_screen);
		infoContainer = findViewById(R.id.image_info_container);
		textViewCaption = (TextView) findViewById(R.id.text_view_caption);
		textViewCounts = (TextView) findViewById(R.id.text_view_count);
		fullTextView = (TextView) findViewById(R.id.text_view_full);
		contentContainer = findViewById(R.id.content_container);
		imageInfoDivider = findViewById(R.id.imageInfoDivider);
		imageView.setOnClickListener(this);
		hikeUiHandler = new HikeUiHandler(this);
	}

	private static final int ANIM_DURATION = 300;

	public void runEnterAnimation()
	{
		final long duration = (long) (ANIM_DURATION * 1);
		contentContainer.setTranslationY(20);
		contentContainer.setAlpha(0f);

		contentContainer.animate().setDuration(duration).translationY(0).alpha(1f);

		float alphaFinal = isTextStatusMessage ? 1f : 0.95f;

		ObjectAnimator bgAnim = ObjectAnimator.ofFloat(fadeScreen, "alpha", 0f, alphaFinal);
		bgAnim.setDuration(400);
		bgAnim.start();

		infoContainer.setVisibility(View.VISIBLE);

		if (isTextStatusMessage)
		{
			imageInfoDivider.setVisibility(View.GONE);
			textViewCaption.setVisibility(View.GONE);
		}
		else
		{
			foregroundScreen.setVisibility(View.VISIBLE);
			textViewCaption.setText(statusMessage.getText());
		}

		if (msisdns != null && !msisdns.isEmpty())
		{
			if (isTextStatusMessage)
			{
				textViewCounts.setText(String.format(getString(R.string.post_likes), msisdns.size()));
			}
			else
			{
				textViewCounts.setText(String.format(getString(R.string.photo_likes), msisdns.size()));
			}

			textViewCounts.setOnClickListener(new View.OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					showLikesContactsDialog();
				}
			});
		}
	}

	/**
	 * The exit animation is basically a reverse of the enter animation, except that if the orientation has changed we simply scale the picture back into the center of the screen.
	 * 
	 * @param endAction
	 *            This action gets run after the animation completes (this is when we actually switch activities)
	 */
	public void runExitAnimation(final Runnable endAction)
	{
		infoContainer.setVisibility(View.GONE);
		contentContainer.animate().setDuration(100).translationY(20).alpha(0f);
		ObjectAnimator bgAnim = ObjectAnimator.ofFloat(fadeScreen, "alpha", 0);
		bgAnim.setDuration(200);

		ObjectAnimator fgAnim = ObjectAnimator.ofFloat(foregroundScreen, "alpha", 0);
		fgAnim.setDuration(200);

		ObjectAnimator actionBarAnim = ObjectAnimator.ofFloat(actionBarView, "alpha", 0);
		actionBarAnim.setDuration(200);

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
		fgAnim.start();
		actionBarAnim.start();
	}

	@Override
	public void onBackPressed()
	{
//		runExitAnimation(new Runnable()
//		{
//			public void run()
//			{
				finish();
//			}
//		});
//
//		actionBar.hide();
	}

	@Override
	public void finish()
	{
		super.finish();

		// override transitions to skip the standard window animations
//		overridePendingTransition(0, 0);
	}

	private void showImage()
	{
		profileImageLoader = new ProfileImageLoader(this, mappedId, imageView, imageSize, true, true);
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
				HikeMessengerApp.getPubSub().publish(HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED, null);
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
			ContactInfo contactInfo = Utils.getUserContactInfo(PostDetailsActivity.this.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0));

			if (contactInfo.getMsisdn().equals((String) object))
			{
				PostDetailsActivity.this.runOnUiThread(new Runnable()
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
			String fileName = Utils.getProfileImageFileName(mappedId);
			mImageWorkerFragment = HeadlessImageDownloaderFragment.newInstance(mappedId, fileName, hasCustomImage, true, null, null, null, true);
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
		HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_IMAGE_NOT_DOWNLOADED, mappedId);
		hikeUiHandler.post(failedRunnable);
	}

	@Override
	public void handleUIMessage(Message msg)
	{
		// TODO Auto-generated method stub
	}

	public boolean hasFileKey()
	{
		if (!TextUtils.isEmpty(statusMessage.getFileKey()))
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
		final HikeDialog dialog = new HikeDialog(PostDetailsActivity.this, R.style.Theme_CustomDialog, 11);
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
				Intent intent = IntentFactory.createChatThreadIntentFromContactInfo(PostDetailsActivity.this,
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

	private void setupActionBar()
	{

		actionBar = getSupportActionBar();
		actionBar.setIcon(R.drawable.hike_logo_top_bar);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		if (!isTextStatusMessage)
		{
			actionBar.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
		}

		actionBarView = LayoutInflater.from(this).inflate(R.layout.chat_thread_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		View contactInfoContainer = actionBarView.findViewById(R.id.contact_info);

		TextView contactName = (TextView) contactInfoContainer.findViewById(R.id.contact_name);

		TextView contactStatus = (TextView) contactInfoContainer.findViewById(R.id.contact_status);

		contactName.setText(statusMessage.getNotNullName());

		contactStatus.setText(statusMessage.getTimestampFormatted(true, HikeMessengerApp.getInstance().getApplicationContext()));

		/**
		 * Adding click listeners
		 */
		contactInfoContainer.setOnClickListener(this);

		backContainer.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});

		actionBar.setCustomView(actionBarView);

		setAvatar();
	}

	/**
	 * This method is used to setAvatar for a contact.
	 */
	protected void setAvatar()
	{
		ImageView avatar = (ImageView) actionBarView.findViewById(R.id.avatar);

		if (avatar == null)
		{
			return;
		}

		Drawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(statusMessage.getMsisdn());
		if (drawable == null)
		{
			drawable = HikeMessengerApp.getLruCache().getDefaultAvatar(statusMessage.getMsisdn(), false);
		}

		avatar.setScaleType(ScaleType.FIT_CENTER);
		avatar.setImageDrawable(drawable);
	}
}
