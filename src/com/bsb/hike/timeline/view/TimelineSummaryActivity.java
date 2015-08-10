package com.bsb.hike.timeline.view;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
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
import com.bsb.hike.imageHttp.HikeImageDownloader;
import com.bsb.hike.imageHttp.HikeImageWorker;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.timeline.adapter.DisplayContactsAdapter;
import com.bsb.hike.timeline.model.ActionsDataModel;
import com.bsb.hike.timeline.model.ActionsDataModel.ActionTypes;
import com.bsb.hike.timeline.model.ActionsDataModel.ActivityObjectTypes;
import com.bsb.hike.timeline.model.FeedDataModel;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.HikeUiHandler;
import com.bsb.hike.utils.HikeUiHandler.IHandlerCallback;
import com.bsb.hike.utils.IntentFactory;
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
public class TimelineSummaryActivity extends AppCompatActivity implements OnClickListener, Listener, IHandlerCallback, HikeImageWorker.TaskCallbacks
{
	ImageView imageView;

	private String mappedId;

	private int imageSize;

	private String[] profilePicPubSubListeners = { HikePubSub.ICON_CHANGED };

	private View fadeScreen;

	private final String TAG = TimelineSummaryActivity.class.getSimpleName();

	private boolean hasCustomImage;

	private ProfileImageLoader profileImageLoader;

	private Runnable failedRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			dismissProgressDialog();
		}
	};

	private Runnable cancelledRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			dismissProgressDialog();
		}
	};

	private Runnable successRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			dismissProgressDialog();
			profileImageLoader.loadFromFile();
		}
	};

	private HikeUiHandler hikeUiHandler;

	private ArrayList<String> msisdns;

	private View infoContainer;

	private TextView textViewCaption;

	private TextView textViewCounts;

	private View foregroundScreen;

	private StatusMessage mStatusMessage;

	private TextView fullTextView;

	private boolean isTextStatusMessage;

	private View contentContainer;

	private View imageInfoDivider;

	private View actionBarView;

	private ActionBar actionBar;

	private CheckBox checkBoxLove;

	private boolean isLikedByMe;

	private boolean isShowCountEnabled;

	private boolean isShowLikesEnabled;

	private ActivityState mActivityState;

	private HikeImageDownloader mImageDownloader;

	private ProgressDialog mDialog;

	public class ActivityState
	{
		public String mappedId;

		public StatusMessage statusMessage;

		public ArrayList<String> msisdnsList;

		public boolean isLikedByMe;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		overridePendingTransition(0, 0);

		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		super.onCreate(savedInstanceState);

		setContentView(R.layout.image_viewer_activity);

		initReferences();

		isShowCountEnabled = Utils.isTimelineShowCountEnabled();

		isShowLikesEnabled = Utils.isTimelineShowLikesEnabled();

		HikeMessengerApp.getPubSub().addListeners(this, profilePicPubSubListeners);

		Object obj = getLastCustomNonConfigurationInstance();

		if (obj instanceof ActivityState)
		{
			mActivityState = (ActivityState) obj;
			mappedId = mActivityState.mappedId;
			mStatusMessage = mActivityState.statusMessage;
			msisdns = mActivityState.msisdnsList;
			isLikedByMe = mActivityState.isLikedByMe;
		}
		else
		{
			mActivityState = new ActivityState();

			Bundle extras = getIntent().getExtras();

			mappedId = extras.getString(HikeConstants.Extras.MAPPED_ID);

			// TODO think of a better place to do this without breaking animation
			mStatusMessage = HikeConversationsDatabase.getInstance().getStatusMessageFromMappedId(mappedId);

			msisdns = extras.getStringArrayList(HikeConstants.MSISDNS);

			isLikedByMe = extras.getBoolean(HikeConstants.Extras.LOVED_BY_SELF, false);
			
			mActivityState.mappedId = mappedId;
			mActivityState.statusMessage = mStatusMessage;
			mActivityState.msisdnsList = msisdns;
			mActivityState.isLikedByMe = isLikedByMe;
		}

		if (msisdns == null)
		{
			// Empty list
			msisdns = new ArrayList<String>();
		}

		checkBoxLove.setTag(mStatusMessage);

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

		if (mStatusMessage.getStatusMessageType() == StatusMessageType.TEXT)
		{
			isTextStatusMessage = true;
			fullTextView.setText(mStatusMessage.getText());
			imageView.setVisibility(View.GONE);
			fadeScreen.setBackgroundColor(Color.WHITE);
			textViewCounts.setTextColor(Color.BLACK);
			fullTextView.setTextColor(Color.BLACK);
			checkBoxLove.setButtonDrawable(R.drawable.btn_love_selector);
		}
		else
		{
			showImage();
			fullTextView.setVisibility(View.GONE);
		}

		setupActionBar();
	}

	private void notifyUI()
	{
		checkBoxLove.setOnCheckedChangeListener(null);

		if (isLikedByMe)
		{
			checkBoxLove.setChecked(true);
		}
		else
		{
			checkBoxLove.setChecked(false);
		}

		if (!msisdns.isEmpty())
		{

			if (isShowCountEnabled)
			{
				// Set count
				if (isTextStatusMessage)
				{
					textViewCounts.setText(String.format(getString(R.string.post_likes), msisdns.size()));
				}
				else
				{
					textViewCounts.setText(String.format(getString(R.string.photo_likes), msisdns.size()));
				}
			}
			else
			{
				if (isLikedByMe)
				{
					textViewCounts.setText(R.string.liked_it);
				}
				else
				{
					textViewCounts.setText(R.string.like_this);
				}
			}
		}
		else
		{
			textViewCounts.setText(R.string.like_this);
		}

		checkBoxLove.setOnCheckedChangeListener(onLoveToggleListener);
	}

	private void initReferences()
	{
		imageView = (ImageView) findViewById(R.id.image);
		fadeScreen = findViewById(R.id.bg_screen);
		foregroundScreen = findViewById(R.id.fg_screen);
		infoContainer = findViewById(R.id.image_info_container);
		textViewCaption = (TextView) findViewById(R.id.text_view_caption);
		textViewCaption.setMovementMethod(new ScrollingMovementMethod());
		textViewCounts = (TextView) findViewById(R.id.text_view_count);
		checkBoxLove = (CheckBox) findViewById(R.id.btn_love);
		fullTextView = (TextView) findViewById(R.id.text_view_full);
		fullTextView.setMovementMethod(new ScrollingMovementMethod());
		contentContainer = findViewById(R.id.content_container);
		imageInfoDivider = findViewById(R.id.imageInfoDivider);
		hikeUiHandler = new HikeUiHandler(this);
	}

	private int ANIM_DURATION = 280;

	public void runEnterAnimation()
	{
		if (mStatusMessage.getStatusMessageType() == StatusMessageType.TEXT)
		{
			ANIM_DURATION = 0;
		}
		contentContainer.setScaleX(0.8f);
		contentContainer.setScaleY(0.8f);
		contentContainer.setAlpha(0f);

		contentContainer.animate().setDuration(ANIM_DURATION).scaleX(1).scaleY(1).alpha(1f);

		float alphaFinal = isTextStatusMessage ? 1f : 0.95f;

		ObjectAnimator bgAnim = ObjectAnimator.ofFloat(fadeScreen, "alpha", 0f, alphaFinal);
		bgAnim.setDuration(ANIM_DURATION);
		bgAnim.start();

		infoContainer.setVisibility(View.VISIBLE);

		notifyUI();

		if (isTextStatusMessage)
		{
			imageInfoDivider.setVisibility(View.GONE);
			textViewCaption.setVisibility(View.GONE);
		}
		else
		{
			if (mStatusMessage.getStatusMessageType() == StatusMessageType.IMAGE)
			{
				foregroundScreen.setVisibility(View.VISIBLE);
				textViewCaption.setText(R.string.posted_photo);
			}
			else
			{
				textViewCaption.setText(mStatusMessage.getText());
			}
		}

		if (msisdns != null && !msisdns.isEmpty() && isShowLikesEnabled)
		{
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
		int duration = ANIM_DURATION * 1;
		infoContainer.setVisibility(View.GONE);
		contentContainer.animate().setDuration(duration).scaleX(0.9f).scaleY(0.9f).alpha(1f);
		ObjectAnimator bgAnim = ObjectAnimator.ofFloat(fadeScreen, "alpha", 0);
		bgAnim.setDuration(duration);

		ObjectAnimator fgAnim = ObjectAnimator.ofFloat(foregroundScreen, "alpha", 0);
		fgAnim.setDuration(duration);

		ObjectAnimator actionBarAnim = ObjectAnimator.ofFloat(actionBarView, "alpha", 0);
		actionBarAnim.setDuration(duration);

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
		finish();
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
				beginImageDownload();
			}
		});
		profileImageLoader.loadProfileImage(getSupportLoaderManager());
	}

	private void beginImageDownload()
	{
		String fileName = Utils.getProfileImageFileName(mappedId);
		mImageDownloader = HikeImageDownloader.newInstance(mappedId, fileName, hasCustomImage, true, null, null, null, true, false);
		mImageDownloader.setTaskCallbacks(this);
		mImageDownloader.startLoadingTask();
	}

	private void dismissProgressDialog()
	{
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}
	}

	private void showProgressDialog()
	{
		mDialog = ProgressDialog.show(TimelineSummaryActivity.this, null, getResources().getString(R.string.downloading_image));
		mDialog.setCancelable(true);
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		dismissProgressDialog();
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
			ContactInfo contactInfo = Utils.getUserContactInfo(TimelineSummaryActivity.this.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0));

			if (contactInfo.getMsisdn().equals((String) object))
			{
				TimelineSummaryActivity.this.runOnUiThread(new Runnable()
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
		if (!TextUtils.isEmpty(mStatusMessage.getFileKey()))
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
		final HikeDialog dialog = new HikeDialog(TimelineSummaryActivity.this, R.style.Theme_CustomDialog, 11);
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
				Intent intent = IntentFactory.createChatThreadIntentFromContactInfo(TimelineSummaryActivity.this,
						ContactManager.getInstance().getContactInfoFromPhoneNoOrMsisdn(msisdns.get(position)), false, false);
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
		actionBar.setDisplayHomeAsUpEnabled(true);

		if (!isTextStatusMessage)
		{
			actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.black)));
		}

		actionBarView = LayoutInflater.from(this).inflate(R.layout.chat_thread_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		View contactInfoContainer = actionBarView.findViewById(R.id.contact_info);

		TextView contactName = (TextView) contactInfoContainer.findViewById(R.id.contact_name);

		TextView contactStatus = (TextView) contactInfoContainer.findViewById(R.id.contact_status);

		String name = ContactManager.getInstance().getName(mStatusMessage.getMsisdn(), true);
		
		if(name == null)
		{
			ContactInfo userConInfo = Utils.getUserContactInfo(true);
			if (userConInfo.getMsisdn().equals(mStatusMessage.getMsisdn()))
			{
				name = getString(R.string.me);
			}
			else
			{
				name = mStatusMessage.getMsisdn();
			}
		}
		
		contactName.setText(name);

		contactStatus.setText(mStatusMessage.getTimestampFormatted(true, HikeMessengerApp.getInstance().getApplicationContext()));

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

		Drawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(mStatusMessage.getMsisdn());
		if (drawable == null)
		{
			drawable = HikeMessengerApp.getLruCache().getDefaultAvatar(mStatusMessage.getMsisdn(), false);
		}

		avatar.setScaleType(ScaleType.FIT_CENTER);
		avatar.setImageDrawable(drawable);
	}

	private OnCheckedChangeListener onLoveToggleListener = new OnCheckedChangeListener()
	{
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			final StatusMessage statusMessage = (StatusMessage) buttonView.getTag();

			JSONObject json = new JSONObject();

			try
			{
				json.put(HikeConstants.SU_ID, statusMessage.getMappedId());
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}

			if (isChecked)
			{
				RequestToken token = HttpRequests.createLoveLink(json, new IRequestListener()
				{
					@Override
					public void onRequestSuccess(Response result)
					{
						JSONObject response = (JSONObject) result.getBody().getContent();
						if (response.optString("stat").equals("ok"))
						{
							// Increment like count in actions table
							String selfMsisdn = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, null);

							ArrayList<String> actorList = new ArrayList<String>();
							actorList.add(selfMsisdn);

							HikeConversationsDatabase.getInstance().changeActionCountForObjID(statusMessage.getMappedId(),
									ActionsDataModel.ActivityObjectTypes.STATUS_UPDATE.getTypeString(), ActionsDataModel.ActionTypes.LIKE.getKey(), actorList, true);

							isLikedByMe = true;

							msisdns.add(selfMsisdn);

							notifyUI();

							FeedDataModel newFeed = new FeedDataModel(System.currentTimeMillis(), ActionTypes.LIKE, selfMsisdn, ActivityObjectTypes.STATUS_UPDATE, statusMessage
									.getMappedId());

							HikeMessengerApp.getPubSub().publish(HikePubSub.ACTIVITY_UPDATE, newFeed);
						}
					}

					@Override
					public void onRequestProgressUpdate(float progress)
					{
						// Do nothing
					}

					@Override
					public void onRequestFailure(HttpException httpException)
					{
						Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), R.string.love_failed, Toast.LENGTH_SHORT).show();
					}
				});
				token.execute();
			}
			else
			{
				RequestToken token = HttpRequests.removeLoveLink(json, new IRequestListener()
				{
					@Override
					public void onRequestSuccess(Response result)
					{
						JSONObject response = (JSONObject) result.getBody().getContent();
						if (response.optString("stat").equals("ok"))
						{
							// Decrement like count in actions table
							String selfMsisdn = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, null);

							ArrayList<String> actorList = new ArrayList<String>();
							actorList.add(selfMsisdn);

							HikeConversationsDatabase.getInstance().changeActionCountForObjID(statusMessage.getMappedId(),
									ActionsDataModel.ActivityObjectTypes.STATUS_UPDATE.getTypeString(), ActionsDataModel.ActionTypes.LIKE.getKey(), actorList, false);

							isLikedByMe = false;

							msisdns.remove(selfMsisdn);

							notifyUI();

							FeedDataModel newFeed = new FeedDataModel(System.currentTimeMillis(), ActionTypes.UNLIKE, selfMsisdn, ActivityObjectTypes.STATUS_UPDATE, statusMessage
									.getMappedId());

							HikeMessengerApp.getPubSub().publish(HikePubSub.ACTIVITY_UPDATE, newFeed);
						}
					}

					@Override
					public void onRequestProgressUpdate(float progress)
					{
						// Do nothing
					}

					@Override
					public void onRequestFailure(HttpException httpException)
					{
						Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), R.string.love_failed, Toast.LENGTH_SHORT).show();
					}
				});
				token.execute();
			}
		}
	};

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		return mActivityState;
	}

	@Override
	public void onProgressUpdate(float percent)
	{
		// DO NOTHING

	}

	@Override
	public void onTaskAlreadyRunning()
	{
		// TODO Auto-generated method stub

	}
}
