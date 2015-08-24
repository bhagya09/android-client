package com.bsb.hike.timeline.view;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.text.util.Linkify;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
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
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.imageHttp.HikeImageDownloader;
import com.bsb.hike.imageHttp.HikeImageWorker;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.timeline.TimelineActionsManager;
import com.bsb.hike.timeline.adapter.ActivityFeedCursorAdapter;
import com.bsb.hike.timeline.adapter.DisplayContactsAdapter;
import com.bsb.hike.timeline.model.ActionsDataModel;
import com.bsb.hike.timeline.model.ActionsDataModel.ActionTypes;
import com.bsb.hike.timeline.model.ActionsDataModel.ActivityObjectTypes;
import com.bsb.hike.timeline.model.FeedDataModel;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.utils.StatusBarColorChanger;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.HikeUiHandler;
import com.bsb.hike.utils.HikeUiHandler.IHandlerCallback;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.ProfileImageLoader;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

/**
 * 
 * TODO Need to make single base class for this and ImageViewerFragment. Currently we are copy-pasting code.
 * 
 * @author Atul M
 * 
 */
public class TimelineSummaryActivity extends HikeAppStateBaseFragmentActivity implements OnClickListener, Listener, IHandlerCallback, HikeImageWorker.TaskCallbacks
{
	private static final int LIKE_CONTACTS_DIALOG = 0;
	
	ImageView imageView;

	private String mappedId;

	private int imageSize;

	private String[] timelineSummaryPubSubListeners = { HikePubSub.ICON_CHANGED };

	private View fadeScreen;

	private boolean hasCustomImage;

	private ProfileImageLoader profileImageLoader;

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

	private ContactInfo profileContactInfo;
	
	private boolean isStopped = false;

	public class ActivityState
	{
		public String mappedId;

		public StatusMessage statusMessage;

		public ArrayList<String> msisdnsList;

		public boolean isLikedByMe;
		
		public boolean dialogShown;
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

		HikeMessengerApp.getPubSub().addListeners(this, timelineSummaryPubSubListeners);

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

			//Try to get actions data from cache
			mStatusMessage = HikeConversationsDatabase.getInstance().getStatusMessageFromMappedId(mappedId);
			ActionsDataModel actionsData = TimelineActionsManager.getInstance().getActionsData()
					.getActions(mStatusMessage.getMappedId(), ActionTypes.LIKE, ActivityObjectTypes.STATUS_UPDATE);

			if(actionsData == null)
			{
				// Try to get actions data from database
				ArrayList<String> suIDs = new ArrayList<String>();
				suIDs.add(mappedId);
				
				HikeConversationsDatabase.getInstance().getActionsData(ActionsDataModel.ActivityObjectTypes.STATUS_UPDATE.getTypeString(), suIDs,
						TimelineActionsManager.getInstance().getActionsData());
				actionsData = TimelineActionsManager.getInstance().getActionsData().getActions(mStatusMessage.getMappedId(), ActionTypes.LIKE, ActivityObjectTypes.STATUS_UPDATE);
			}
			
			if (actionsData != null)
			{
				msisdns = actionsData.getAllMsisdn();

				isLikedByMe = actionsData.isLikedBySelf();
			}

			// No likes
			if (msisdns == null)
			{
				// Empty list
				msisdns = new ArrayList<String>();
			}

			mActivityState.mappedId = mappedId;
			mActivityState.statusMessage = mStatusMessage;
			mActivityState.msisdnsList = msisdns;
			mActivityState.isLikedByMe = isLikedByMe;
		}
		
		JSONObject metadataSU = new JSONObject();
		try
		{
			metadataSU.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.TIMELINE_SUMMARY_OPEN);
			metadataSU.put(AnalyticsConstants.UPDATE_TYPE, "" + ActivityFeedCursorAdapter.getPostType(mStatusMessage));
			metadataSU.put(AnalyticsConstants.TIMELINE_U_ID, mStatusMessage.getMsisdn());
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, HAManager.EventPriority.HIGH, metadataSU);
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
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
			SmileyParser smileyParser = SmileyParser.getInstance();
			fullTextView.setText(smileyParser.addSmileySpans(mStatusMessage.getText(), true));
			Linkify.addLinks(fullTextView, Linkify.ALL);
			imageView.setVisibility(View.GONE);
			fadeScreen.setBackgroundColor(Color.WHITE);
			textViewCounts.setTextColor(0x3D000000);
			fullTextView.setTextColor(0x99000000);
			checkBoxLove.setButtonDrawable(R.drawable.btn_love_selector);
		}
		else
		{
			StatusBarColorChanger.setStatusBarColor(getWindow(), Color.BLACK);
			showImage();
			fullTextView.setVisibility(View.GONE);
		}

		setupActionBar();
		
		if(mActivityState.dialogShown)
		{
			mActivityState.dialogShown = false;
			showLikesContactsDialog();
		}
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

			if (isShowCountEnabled || mStatusMessage.isMyStatusUpdate())
			{
				// Set count
				if (msisdns.size() == 1)
				{
					textViewCounts.setText(String.format(getString(R.string.num_like), msisdns.size()));
				}
				else
				{
					textViewCounts.setText(String.format(getString(R.string.num_likes), msisdns.size()));
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

		float alphaFinal = isTextStatusMessage ? 1f : 1f;

		ObjectAnimator bgAnim = ObjectAnimator.ofFloat(fadeScreen, "alpha", 0f, alphaFinal);
		bgAnim.setDuration(ANIM_DURATION);
		bgAnim.start();

		infoContainer.setVisibility(View.VISIBLE);

		notifyUI();

		if (isTextStatusMessage)
		{
			imageInfoDivider.setBackgroundColor(0x14000000);
			textViewCaption.setVisibility(View.GONE);
		}
		else
		{
			foregroundScreen.setVisibility(View.VISIBLE);
			if (mStatusMessage.getStatusMessageType() == StatusMessageType.IMAGE)
			{
//				textViewCaption.setText(R.string.posted_photo);
				textViewCaption.setVisibility(View.GONE);
			}
			else
			{
				SmileyParser smileyParser = SmileyParser.getInstance();
				textViewCaption.setText(smileyParser.addSmileySpans(mStatusMessage.getText(), true));
				Linkify.addLinks(textViewCaption, Linkify.ALL);
			}
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
			}

			@Override
			public void onLoadFinished(Loader<Boolean> arg0, Boolean arg1)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED, null);
			}

			@Override
			public Loader<Boolean> onCreateLoader(int arg0, Bundle arg1)
			{
				return null;
			}

			@Override
			public void startDownloading()
			{
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

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListeners(this, timelineSummaryPubSubListeners);
	}

	@Override
	public void onClick(View v)
	{
		if (Utils.isSelfMsisdn(mStatusMessage.getMsisdn()))
		{
			Intent intent2 = new Intent(TimelineSummaryActivity.this, ProfileActivity.class);
			intent2.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
			startActivity(intent2);
		}
		else
		{

			Intent intent = IntentFactory.createChatThreadIntentFromContactInfo(TimelineSummaryActivity.this, ContactManager.getInstance()
					.getContact(mStatusMessage.getMsisdn(),true,true), false, false);
			// Add anything else to the intent
			intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		}
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
		else if ((HikePubSub.FAVORITE_TOGGLED.equals(type) || HikePubSub.FRIEND_REQUEST_ACCEPTED.equals(type) || HikePubSub.REJECT_FRIEND_REQUEST.equals(type)))
		{
			final Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;

			ContactInfo contactInfo = favoriteToggle.first;
			FavoriteType favoriteType = favoriteToggle.second;

			if (profileContactInfo != null)
			{
				if (!profileContactInfo.getMsisdn().equals(contactInfo.getMsisdn()))
				{
					return;
				}
				else
				{
					this.profileContactInfo.setFavoriteType(favoriteType);
				}
			}
		}
		
	}

	public void onCancelled()
	{
	}

	public void onSuccess(Response result)
	{
		hikeUiHandler.post(successRunnable);
	}

	public void onFailed()
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_IMAGE_NOT_DOWNLOADED, mappedId);
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
		if (msisdns != null && !msisdns.isEmpty() && (isShowLikesEnabled || mStatusMessage.isMyStatusUpdate()))
		{
			final HikeDialog dialog = new HikeDialog(TimelineSummaryActivity.this, R.style.Theme_CustomDialog, LIKE_CONTACTS_DIALOG);
			dialog.setContentView(R.layout.display_contacts_dialog);
			dialog.setCancelable(true);
			dialog.setCanceledOnTouchOutside(true);

			ListView listContacts = (ListView) dialog.findViewById(R.id.listContacts);
			DisplayContactsAdapter contactsAdapter = new DisplayContactsAdapter(msisdns);
			listContacts.setAdapter(contactsAdapter);
			listContacts.setOnItemClickListener(new AdapterView.OnItemClickListener()
			{
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
				{
					if (Utils.isSelfMsisdn(msisdns.get(position)))
					{
						Intent intent2 = new Intent(TimelineSummaryActivity.this, ProfileActivity.class);
						intent2.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
						startActivity(intent2);
					}
					else
					{

						Intent intent = IntentFactory.createChatThreadIntentFromContactInfo(TimelineSummaryActivity.this, ContactManager.getInstance()
								.getContact(msisdns.get(position),true,true), false, false);
						// Add anything else to the intent
						intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent);
					}
					mActivityState.dialogShown = false;
				}
			});
			dialog.show();
			mActivityState.dialogShown = true;
			JSONObject metadataSU = new JSONObject();
			try
			{
				metadataSU.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.TIMELINE_SUMMARY_LIKES_DIALOG_OPEN);
				metadataSU.put(AnalyticsConstants.UPDATE_TYPE, "" + ActivityFeedCursorAdapter.getPostType(mStatusMessage));
				metadataSU.put(AnalyticsConstants.TIMELINE_U_ID, mStatusMessage.getMsisdn());
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, HAManager.EventPriority.HIGH, metadataSU);
			}
			catch (JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}
		}
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

		View contactInfoContainer = actionBarView.findViewById(R.id.contact_info);

		TextView contactName = (TextView) contactInfoContainer.findViewById(R.id.contact_name);

		TextView contactStatus = (TextView) contactInfoContainer.findViewById(R.id.contact_status);

		//Get contact info
		ContactInfo contactInfo = ContactManager.getInstance().getContact(mStatusMessage.getMsisdn(), true,  false);

		// Check if this guy has a saved name
		String name = contactInfo.getName();

		try
		{
			if (TextUtils.isEmpty(name))
			{
				// Was this our own contact info?
				ContactInfo myContactInfo = Utils.getUserContactInfo(false);
				if (myContactInfo.getMsisdn().equals(mStatusMessage.getMsisdn()))
				{
					// Get name from account shared pref
					name = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.NAME_SETTING, getApplicationContext().getString(R.string.me));
				}
				else
				{
					// Neither my contact info nor has a name, show msisdn
					name = contactInfo.getNameOrMsisdn();
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		if(TextUtils.isEmpty(name))
		{
			name = contactInfo.getNameOrMsisdn();
		}

		contactName.setText(name);

		contactStatus.setText(mStatusMessage.getTimestampFormatted(true, HikeMessengerApp.getInstance().getApplicationContext()));

		/**
		 * Adding click listeners
		 */
		contactInfoContainer.setOnClickListener(this);

		actionBar.setCustomView(actionBarView);

		setAvatar();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			onBackPressed();
			return true;
		}

		return super.onOptionsItemSelected(item);
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
		public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked)
		{
			buttonView.setEnabled(false);
			buttonView.setClickable(false);

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

			profileContactInfo = ContactManager.getInstance().getContact(statusMessage.getMsisdn(), true, true);

			// First check if user is friends with msisdn
			if (profileContactInfo.getFavoriteType() != FavoriteType.FRIEND && !Utils.isSelfMsisdn(profileContactInfo.getMsisdn()))
			{
				toggleCompButtonState(buttonView, onLoveToggleListener);
				HikeDialogFactory.showDialog(TimelineSummaryActivity.this, HikeDialogFactory.ADD_TO_FAV_DIALOG, new HikeDialogListener()
				{
					@Override
					public void positiveClicked(HikeDialog hikeDialog)
					{
						Utils.toggleFavorite(getApplicationContext(), profileContactInfo, false);
						if (hikeDialog != null && hikeDialog.isShowing())
						{
							hikeDialog.dismiss();
						}
					}

					@Override
					public void neutralClicked(HikeDialog hikeDialog)
					{
						// Do nothing
					}

					@Override
					public void negativeClicked(HikeDialog hikeDialog)
					{
						if (hikeDialog != null && hikeDialog.isShowing())
						{
							hikeDialog.dismiss();
						}
					}
				}, profileContactInfo.getNameOrMsisdn());
				return;
			}

			if (isChecked)
			{
				RequestToken token = HttpRequests.createLoveLink(json, new IRequestListener()
				{
					@Override
					public void onRequestSuccess(Response result)
					{
						try
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
								// UI work
								if (!isStopped)
								{
									notifyUI();
								}

								FeedDataModel newFeed = new FeedDataModel(System.currentTimeMillis(), ActionTypes.LIKE, selfMsisdn, ActivityObjectTypes.STATUS_UPDATE,
										statusMessage.getMappedId());

								HikeMessengerApp.getPubSub().publish(HikePubSub.ACTIVITY_UPDATE, newFeed);
							}
						}
						finally
						{
							// UI work
							if (!isStopped)
							{
								buttonView.setEnabled(true);
								buttonView.setClickable(true);
							}
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

						// UI work
						if (!isStopped)
						{
							buttonView.setEnabled(true);
							buttonView.setClickable(true);
							toggleCompButtonState(buttonView, onLoveToggleListener);
						}
					}
				}, statusMessage.getMappedId());
				token.execute();

			}
			else
			{
				RequestToken token = HttpRequests.removeLoveLink(json, new IRequestListener()
				{
					@Override
					public void onRequestSuccess(Response result)
					{
						try
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

								FeedDataModel newFeed = new FeedDataModel(System.currentTimeMillis(), ActionTypes.UNLIKE, selfMsisdn, ActivityObjectTypes.STATUS_UPDATE,
										statusMessage.getMappedId());

								HikeMessengerApp.getPubSub().publish(HikePubSub.ACTIVITY_UPDATE, newFeed);

								// UI work
								if (!isStopped)
								{
									notifyUI();
								}
							}
						}
						finally
						{
							// UI work
							if (!isStopped)
							{
								buttonView.setEnabled(true);
								buttonView.setClickable(true);
							}
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

						// UI work
						if (!isStopped)
						{
							buttonView.setEnabled(true);
							buttonView.setClickable(true);
							toggleCompButtonState(buttonView, onLoveToggleListener);
						}
					}
				}, statusMessage.getMappedId());
				token.execute();

			}
		}
	};

	private void toggleCompButtonState(CompoundButton argButton,OnCheckedChangeListener argListener)
	{
		//unlink-relink onchange listener
		argButton.setOnCheckedChangeListener(null);
		argButton.toggle();
		argButton.setOnCheckedChangeListener(argListener);
	}

	@Override
	protected void onStop()
	{
		isStopped  = true;
		super.onStop();
	}
	
	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		return mActivityState;
	}
	
	@Override
	protected void onPause()
	{
		mActivityState.isLikedByMe = this.isLikedByMe;
		mActivityState.msisdnsList = msisdns;
		super.onPause();
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
