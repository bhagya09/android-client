package com.bsb.hike.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MoodAdapter;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.media.EmoticonPicker;
import com.bsb.hike.media.PopupListener;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.tasks.StatusUpdateTask;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.EmoticonTextWatcher;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeTip;
import com.bsb.hike.utils.HikeTip.TipType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontEditText;
import com.bsb.hike.view.CustomLinearLayout;
import com.bsb.hike.view.CustomLinearLayout.OnSoftKeyboardListener;

public class StatusUpdate extends HikeAppStateBaseFragmentActivity implements Listener, OnSoftKeyboardListener, PopupListener, View.OnClickListener
{

	private class ActivityTask
	{
		int moodId = -1;

		int moodIndex = -1;
		
		StatusUpdateTask task;

		/*boolean fbSelected = false;

		boolean twitterSelected = false;*/

		boolean emojiShowing = false;

		boolean moodShowing = false;
	}

	private ActivityTask mActivityTask;

	private static final String TAG = "statusupdate";
	
	private SharedPreferences preferences;

	private ProgressDialog progressDialog;

	private String[] pubsubListeners = { HikePubSub.SOCIAL_AUTH_COMPLETED, HikePubSub.SOCIAL_AUTH_FAILED, HikePubSub.STATUS_POST_REQUEST_DONE };

	private ViewGroup moodParent;

	private ImageView avatar;

	private CustomFontEditText statusTxt;

	private CustomLinearLayout parentLayout;

	private TextView charCounter;

	private View tipView;

	private View doneBtn;

	private TextView postText;

	private ImageView arrow;
	
	private TextView title;
	
	private EmoticonPicker mEmoticonPicker;
	
	private static final int SHOW_EMOJI_PALETTE = 1;
	
	private boolean wasEmojiPreviouslyVisible;

	protected Handler uiHandler = new Handler()
	{
		public void handleMessage(android.os.Message msg)
		{
			/**
			 * Defensive check
			 */
			if (msg == null)
			{
				Logger.e(TAG, "Getting a null message in chat thread");
				return;
			}
			handlingMessage(msg);
		}

	};
	
	protected void handlingMessage(android.os.Message msg)
	{
		switch (msg.what)
		{
		case SHOW_EMOJI_PALETTE:
			showEmoticonPicker();
			break;
		default:
			Logger.d(TAG, "Did not find any matching event for msg.what : " + msg.what);
			break;
		}
	}
	
	//private View fb;

	//private View twitter;

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		if (wasEmojiPreviouslyVisible)
		{
			mActivityTask.emojiShowing = true;
		}
		return mActivityTask;
	}

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.status_dialog);

		addOnClickListeners();
		Object o = getLastCustomNonConfigurationInstance();

		if (o instanceof ActivityTask)
		{
			mActivityTask = (ActivityTask) o;
			if (mActivityTask.task != null)
			{
				progressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_status));
			}
		}
		else
		{
			mActivityTask = new ActivityTask();
		}

		preferences = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);

		moodParent = (ViewGroup) findViewById(R.id.mood_parent);
		
		setupActionBar();

		parentLayout = (CustomLinearLayout) findViewById(R.id.parent_layout);
		parentLayout.setOnSoftKeyboardListener(this);

		avatar = (ImageView) findViewById(R.id.avatar);

		charCounter = (TextView) findViewById(R.id.char_counter);

		statusTxt = (CustomFontEditText) findViewById(R.id.status_txt);

		String statusHint = getStatusDefaultHint();

		statusTxt.setHint(statusHint);
		
		charCounter.setText(Integer.toString(statusTxt.length()));

		setMood(mActivityTask.moodId, mActivityTask.moodIndex);

		statusTxt.addTextChangedListener(new TextWatcher()
		{

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
			}

			@Override
			public void afterTextChanged(Editable s)
			{
				toggleEnablePostButton();
				charCounter.setText(Integer.toString(s.length()));
			}
		});
		statusTxt.addTextChangedListener(new EmoticonTextWatcher());

		/*fb = findViewById(R.id.post_fb_btn);
		twitter = findViewById(R.id.post_twitter_btn);

		fb.setSelected(mActivityTask.fbSelected);
		twitter.setSelected(mActivityTask.twitterSelected);*/
		
		initEmoticonPicker();
		
		if (mActivityTask.emojiShowing)
		{
			sendUIMessage(SHOW_EMOJI_PALETTE, null);
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}
		else if (mActivityTask.moodShowing)
		{
			showMoodSelector();
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}
		else
		{
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
			toggleEnablePostButton();
		}

		HikeMessengerApp.getPubSub().addListeners(this, pubsubListeners);

		if (!preferences.getBoolean(HikeMessengerApp.SHOWN_MOODS_TIP, false))
		{
			tipView = findViewById(R.id.mood_tip);

			/*
			 * Center aligning with the button.
			 */
			RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) tipView.getLayoutParams();
			int screenWidth = getResources().getDisplayMetrics().widthPixels;
			int buttonWidth = screenWidth / 4;
			int marginRight = (int) ((buttonWidth / 2) - ((int) 22 * Utils.scaledDensityMultiplier));
			layoutParams.rightMargin = marginRight;

			tipView.setLayoutParams(layoutParams);
			HikeTip.showTip(this, TipType.MOOD, tipView);
		}
		
		
		showProductPopup(ProductPopupsConstants.PopupTriggerPoints.STATUS.ordinal());
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		title = (TextView) actionBarView.findViewById(R.id.title);
		doneBtn = actionBarView.findViewById(R.id.done_container);
		arrow = (ImageView) actionBarView.findViewById(R.id.arrow);
		postText = (TextView) actionBarView.findViewById(R.id.post_btn);

		postText.setText(R.string.post);

		doneBtn.setVisibility(View.VISIBLE);

		Utils.toggleActionBarElementsEnable(doneBtn, arrow, postText, false);

		setTitle();

		backContainer.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Utils.hideSoftKeyboard(StatusUpdate.this, statusTxt);
				actionBarBackPressed();
			}
		});

		doneBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				releaseEmoticon();
				postStatus();
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	private void setTitle()
	{
		title.setText(moodParent.getVisibility() == View.VISIBLE ? R.string.moods : R.string.status);
	}

	public void onTitleIconClick(View v)
	{
		if (isEmojiOrMoodLayoutVisible())
		{
			hideEmojiOrMoodLayout();
		}
		else
		{
			postStatus();
		}
	}

	/*public void onTwitterClick(View v)
	{
		setSelectionSocialButton(false, !v.isSelected());
		if (!v.isSelected() || preferences.getBoolean(HikeMessengerApp.TWITTER_AUTH_COMPLETE, false))
		{
			return;
		}
		startActivity(new Intent(StatusUpdate.this, TwitterAuthActivity.class));
	}

	public void onFacebookClick(View v)
	{
		setSelectionSocialButton(true, !v.isSelected());
		if (!v.isSelected() || preferences.getBoolean(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE, false))
		{
			return;
		}

		startFbSession();
	}

	private void startFbSession()
	{
		if (ensureOpenSession())
		{
			ensurePublishPermissions();
		}
	}

	private boolean ensureOpenSession()
	{
		Logger.d("StatusUpdate", "entered in ensureOpenSession");
		if (Session.getActiveSession() == null || !Session.getActiveSession().isOpened())
		{

			Logger.d("StatusUpdate", "active session is either null or closed");
			Session.openActiveSession(this, true, new Session.StatusCallback()
			{
				@Override
				public void call(Session session, SessionState state, Exception exception)
				{
					onSessionStateChanged(session, state, exception);
				}
			});
			return false;
		}

		return true;
	}

	private void onSessionStateChanged(Session session, SessionState state, Exception exception)
	{
		Logger.d("StatusUpdate", "inside onSessionStateChanged");
		Logger.d("StatusUpdate", "state = " + state.toString());
		if (state.isOpened())
		{
			startFbSession();
		}
		if (exception != null)
		{
			Logger.e("StatusUpdate", "error trying to open the session", exception);
		}
	}

	private boolean hasPublishPermission()
	{
		Session session = Session.getActiveSession();
		return session != null && session.getPermissions().contains("publish_stream");
	}

	private void ensurePublishPermissions()
	{
		Session session = Session.getActiveSession();
		Logger.d("StatusUpdate", "ensurePublishPermissions");
		if (!hasPublishPermission())
		{
			Logger.d("StatusUpdate", "not hasPublishPermission");
			session.requestNewPublishPermissions(new Session.NewPermissionsRequest(this, Arrays.asList("basic_info", "publish_stream")).setCallback(new StatusCallback()
			{

				@Override
				public void call(Session session, SessionState state, Exception exception)
				{
					if (exception != null)
					{
						Logger.e("StatusUpdate ", "Error Requesting NewPublishPermissions = " + exception.toString());
						return;
					}
					if (hasPublishPermission())
					{
						Logger.d("StatusUpdate", session.getExpirationDate().toString());
						makeMeRequest(session, session.getAccessToken(), session.getExpirationDate().getTime());
					}
					else
					{

					}

				}

			}));

		}
		else
		{
			Logger.d("StatusUpdate", "time = " + Long.valueOf(session.getExpirationDate().getTime()).toString());
			makeMeRequest(session, session.getAccessToken(), session.getExpirationDate().getTime());
		}
	}
	*/

	private void onEmojiClick()
	{
		if (mActivityTask.emojiShowing)
		{
			mActivityTask.emojiShowing = false;
			mEmoticonPicker.dismiss();
			setEmoticonButtonSelected(false);
		}
		else
		{
			showEmoticonPicker();
		}
	}
	
	public void showEmoticonPicker()
	{
		wasEmojiPreviouslyVisible = false;
		if (mEmoticonPicker.showEmoticonPicker(getResources().getConfiguration().orientation))
		{
			Utils.hideSoftKeyboard(this, statusTxt);
			mActivityTask.emojiShowing = true;
			showCancelButton(false);
			setEmoticonButtonSelected(false);
		}
		else
		{
			if (!retryToInflateEmoticons())
			{
				setEmoticonButtonSelected(false);
				mActivityTask.emojiShowing = false;
				Toast.makeText(getApplicationContext(), R.string.some_error, Toast.LENGTH_SHORT).show();
			}
		}
	}

	/**
	 * Got a failure while opening emoticon pallete possibly due to null context, mainView is null or mainView.getWindowToken() is null (this happens during
	 * device orientation change)
	 * @return
	 */
	private boolean retryToInflateEmoticons()
	{
		String errorMsg = "Inside method : retry to inflate emoticons. Houston!, something's not right here";
		HAManager.sendStickerEmoticonStrangeBehaviourReport(errorMsg);
		mEmoticonPicker = null;
		initEmoticonPicker();
		mActivityTask.emojiShowing = true;
		return	mEmoticonPicker.showEmoticonPicker(getResources().getConfiguration().orientation);
	}
	
	protected void sendUIMessage(int what, Object data)
	{
		Message message = Message.obtain();
		message.what = what;
		message.obj = data;
		uiHandler.sendMessage(message);
	}
	
	public void onMoodClick(View v)
	{
		/*if (findViewById(R.id.post_twitter_btn).isSelected() && statusTxt.length() > HikeConstants.MAX_MOOD_TWITTER_POST_LENGTH)
		{
			Toast.makeText(getApplicationContext(), R.string.mood_tweet_error, Toast.LENGTH_LONG).show();
			return;
		}*/
		if (tipView != null)
		{
			HikeTip.closeTip(TipType.MOOD, tipView, preferences);
		}
		if (mEmoticonPicker!= null && mEmoticonPicker.isShowing())
		{
			mActivityTask.emojiShowing = false;
			mEmoticonPicker.dismiss();
		}
		showMoodSelector();
		setTitle();
	}

	@Override
	public void onBackPressed()
	{
		if (isEmojiOrMoodLayoutVisible())
		{
			hideEmojiOrMoodLayout();
			setTitle();
		}
		else
		{
			super.onBackPressed();
		}
	}
		
	public void actionBarBackPressed()
	{
		if (isEmojiOrMoodLayoutVisible())
		{
			if (moodParent.getVisibility() == View.VISIBLE)
			{
				hideEmojiOrMoodLayout();
				setTitle();
			}
			else
			{
				releaseEmoticon();
				super.onBackPressed();
			}
		}
		else
		{
			super.onBackPressed();
		}
	}

	private boolean isEmojiOrMoodLayoutVisible()
	{
		return ((moodParent.getVisibility() == View.VISIBLE) || mActivityTask.emojiShowing);
	}

	private void hideEmojiOrMoodLayout()
	{
		if (moodParent.getVisibility() == View.VISIBLE)
		{
			mActivityTask.moodShowing = false;
			moodParent.setVisibility(View.GONE);
		}
		else if (mEmoticonPicker != null && mEmoticonPicker.isShowing())
		{
			mActivityTask.emojiShowing = false;
			mEmoticonPicker.dismiss();
		}
		toggleEnablePostButton();
		/*
		 * Show soft keyboard.
		 */
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.showSoftInput(statusTxt, InputMethodManager.SHOW_IMPLICIT);
	}

	private String getStatusDefaultHint()
	{
		return getString(R.string.whats_up_user, Utils.getFirstName(preferences.getString(HikeMessengerApp.NAME_SETTING, "")));
	}

	private void postStatus()
	{
		String status = null;
		/*
		 * If the text box is empty, the we take the hint text which is a prefill for moods.
		 */
		if (TextUtils.isEmpty(statusTxt.getText()))
		{
			status = statusTxt.getHint().toString();
		}
		else
		{
			status = statusTxt.getText().toString();
		}
		
		mActivityTask.task = new StatusUpdateTask(status, mActivityTask.moodId);
		mActivityTask.task.execute();
		
		progressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_status));
	}
	
	/*private void setSelectionSocialButton(boolean facebook, boolean selection)
	{
		View v = findViewById(facebook ? R.id.post_fb_btn : R.id.post_twitter_btn);
		v.setSelected(selection);
		if (!facebook)
		{
			if (mActivityTask.moodId != -1 && statusTxt.length() > HikeConstants.MAX_MOOD_TWITTER_POST_LENGTH)
			{
				Toast.makeText(getApplicationContext(), R.string.mood_tweet_error, Toast.LENGTH_LONG).show();
				v.setSelected(false);
				return;
			}
			if (statusTxt.length() > HikeConstants.MAX_TWITTER_POST_LENGTH)
			{
				Toast.makeText(getApplicationContext(), R.string.twitter_length_exceed, Toast.LENGTH_SHORT).show();
				v.setSelected(false);
				return;
			}
			setCharCountForStatus(v.isSelected());
			mActivityTask.twitterSelected = v.isSelected();
		}
		else
		{
			mActivityTask.fbSelected = v.isSelected();
		}
	}

	private void setCharCountForStatus(boolean isSelected)
	{
		charCounter.setVisibility(View.VISIBLE);

		if (isSelected)
		{
			statusTxt.setFilters(new InputFilter[] { new InputFilter.LengthFilter(mActivityTask.moodId != -1 ? HikeConstants.MAX_MOOD_TWITTER_POST_LENGTH
					: HikeConstants.MAX_TWITTER_POST_LENGTH) });
		}
		else
		{
			statusTxt.setFilters(new InputFilter[] {});
		}
	}*/

	private void showMoodSelector()
	{
		Utils.hideSoftKeyboard(this, statusTxt);

		mActivityTask.moodShowing = true;

		showCancelButton(true);

		moodParent.setClickable(true);
		GridView moodPager = (GridView) findViewById(R.id.mood_pager);

		moodParent.setVisibility(View.VISIBLE);

		boolean portrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
		int columns = portrait ? 4 : 6;

		moodPager.setNumColumns(columns);
		MoodAdapter moodAdapter = new MoodAdapter(this, columns);
		moodPager.setAdapter(moodAdapter);
		moodPager.setOnItemClickListener(moodAdapter);
	}

	public void setMood(int moodId, int moodIndex)
	{
		if (moodId == -1)
		{
			return;
		}
		mActivityTask.moodId = moodId;
		mActivityTask.moodIndex = moodIndex;

		avatar.setImageResource(EmoticonConstants.moodMapping.get(moodId));

		String[] moodsArray = getResources().getStringArray(R.array.mood_headings);
		statusTxt.setHint(moodsArray[moodIndex]);

		toggleEnablePostButton();
		if (isEmojiOrMoodLayoutVisible())
		{
			onBackPressed();
		}
		//setCharCountForStatus(findViewById(R.id.post_twitter_btn).isSelected());
	}

	private void showCancelButton(boolean moodLayout)
	{
		if (moodLayout)
		{
		}
	}

	@Override
	public void onPopupDismiss()
	{
		if(mActivityTask.emojiShowing)
		{
			wasEmojiPreviouslyVisible = true;
			mActivityTask.emojiShowing = false;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		/*if (requestCode == HikeConstants.FACEBOOK_REQUEST_CODE)
		{
			Session session = Session.getActiveSession();
			if (session != null && resultCode == RESULT_OK)
			{
				mActivityTask.fbSelected = true;
				session.onActivityResult(this, requestCode, resultCode, data);
			}
			else if (session != null && resultCode == RESULT_CANCELED)
			{
				Logger.d("StatusUpdate", "Facebook Permission Cancelled");
				// if we do not close the session here then requesting publish
				// permission just after canceling the permission will
				// throw an exception telling can not request publish
				// permission, there
				// is already a publish request pending.
				mActivityTask.fbSelected = false;
				session.closeAndClearTokenInformation();
				Session.setActiveSession(null);
			}
			fb.setSelected(mActivityTask.fbSelected);
		}*/
	}

	public void toggleEnablePostButton()
	{
		/*
		 * Enabling if the text length is > 0 or if the user has selected a mood with some prefilled text.
		 */
		boolean enable = mActivityTask.moodId >= 0 || statusTxt.getText().toString().trim().length() > 0;
		Utils.toggleActionBarElementsEnable(doneBtn, arrow, postText, enable);
	}

	@Override
	public void onEventReceived(final String type, Object object)
	{
		/*if (HikePubSub.SOCIAL_AUTH_COMPLETED.equals(type) || HikePubSub.SOCIAL_AUTH_FAILED.equals(type))
		{
			final boolean facebook = (Boolean) object;
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					setSelectionSocialButton(facebook, HikePubSub.SOCIAL_AUTH_COMPLETED.equals(type));
				}
			});
		}
		else*/ if (HikePubSub.STATUS_POST_REQUEST_DONE.equals(type))
		{
			final boolean statusPosted = (Boolean) object;
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					mActivityTask.task = null;
					if (progressDialog != null)
					{
						progressDialog.dismiss();
						progressDialog = null;
					}
					if (statusPosted)
					{
						Utils.hideSoftKeyboard(StatusUpdate.this, statusTxt);
						finish();
					}
					else
					{
						Toast.makeText(getApplicationContext(), R.string.update_status_fail, Toast.LENGTH_SHORT).show();
					}
				}
			});
		}
	}

	@Override
	protected void onDestroy()
	{
		/*
		 * We need to unregister all pubsublisteners whenever activity gets destroyed. Otherwise reference to this activity gets attached with our HikeMessengerApp which doesn't
		 * let GC pick up any instance of this activity. So whenever this activity gets destroyed its instance doesn't get cleared from heap.
		 */
		HikeMessengerApp.getPubSub().removeListeners(this, pubsubListeners);
		super.onDestroy();
		if (progressDialog != null)
		{
			progressDialog.dismiss();
			progressDialog = null;
		}
	}

	@Override
	public void onShown()
	{
	}

	@Override
	public void onHidden()
	{
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.emoji_btn:
			setEmoticonButtonSelected(true);
			onEmojiClick();
			break;
		default:
			Logger.e(TAG, "onClick Registered but not added in onClick : " + v.toString());
			break;
		}
	}
	
	private void addOnClickListeners()
	{
		findViewById(R.id.emoji_btn).setOnClickListener(this);
	}
	
	private void initEmoticonPicker()
	{
		int[] dontEatThisTouch = {R.id.emoji_btn};
		mEmoticonPicker = new EmoticonPicker(this, statusTxt, findViewById(R.id.parent_layout), (int)getResources().getDimension(R.dimen.emoticon_pallete), dontEatThisTouch);
		mEmoticonPicker.setOnDismissListener(this);
	}
	
	private void setEmoticonButtonSelected(boolean selected)
	{
		findViewById(R.id.emoji_btn).setSelected(selected);
	}
	
	private void releaseEmoticon()
	{
		/**
		 * It is important that along with releasing resources for Emoticons, we also close its window to prevent any BadWindow Exceptions later on.
		 */
		if (mEmoticonPicker != null)
		{	
			mEmoticonPicker.releaseReources();
			mEmoticonPicker.dismiss();
		}
	}
	
	@Override
	protected void onStop()
	{
		releaseEmoticon();
		super.onStop();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) 
	{
		if (mEmoticonPicker!=null)
		{
			mEmoticonPicker.dismiss();
			initEmoticonPicker();
			mEmoticonPicker.onOrientationChange(newConfig.orientation);
			mActivityTask.emojiShowing = true;
		}
		super.onConfigurationChanged(newConfig);
	}
}
