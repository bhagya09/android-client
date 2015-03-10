package com.bsb.hike.ui;

import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.EmoticonAdapter;
import com.bsb.hike.adapters.EmoticonPageAdapter.EmoticonClickListener;
import com.bsb.hike.adapters.MoodAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.EmoticonTextWatcher;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeTip;
import com.bsb.hike.utils.HikeTip.TipType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomLinearLayout;
import com.bsb.hike.view.CustomLinearLayout.OnSoftKeyboardListener;
import com.bsb.hike.view.StickerEmoticonIconPageIndicator;

public class StatusUpdate extends HikeAppStateBaseFragmentActivity implements Listener, OnSoftKeyboardListener, EmoticonClickListener
{

	private class ActivityTask
	{
		int moodId = -1;

		int moodIndex = -1;

		HikeHTTPTask hikeHTTPTask = null;

		/*boolean fbSelected = false;

		boolean twitterSelected = false;*/

		boolean emojiShowing = false;

		boolean moodShowing = false;
	}

	private ActivityTask mActivityTask;

	private SharedPreferences preferences;

	private ProgressDialog progressDialog;

	private String[] pubsubListeners = { HikePubSub.SOCIAL_AUTH_COMPLETED, HikePubSub.SOCIAL_AUTH_FAILED, HikePubSub.STATUS_POST_REQUEST_DONE };

	private ViewGroup moodParent;

	private ImageView avatar;

	private ViewGroup emojiParent;

	private EditText statusTxt;

	private CustomLinearLayout parentLayout;

	private Handler handler;

	private TextView charCounter;

	private View tipView;

	private View doneBtn;

	private TextView postText;

	private ImageView arrow;

	private TextView title;

	//private View fb;

	//private View twitter;

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		return mActivityTask;
	}

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.status_dialog);

		Object o = getLastCustomNonConfigurationInstance();

		if (o instanceof ActivityTask)
		{
			mActivityTask = (ActivityTask) o;
			if (mActivityTask.hikeHTTPTask != null)
			{
				progressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_status));
			}
		}
		else
		{
			mActivityTask = new ActivityTask();
		}

		handler = new Handler();

		preferences = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);

		emojiParent = (ViewGroup) findViewById(R.id.emoji_container);
		moodParent = (ViewGroup) findViewById(R.id.mood_parent);

		setupActionBar();

		parentLayout = (CustomLinearLayout) findViewById(R.id.parent_layout);
		parentLayout.setOnSoftKeyboardListener(this);

		avatar = (ImageView) findViewById(R.id.avatar);

		charCounter = (TextView) findViewById(R.id.char_counter);

		statusTxt = (EditText) findViewById(R.id.status_txt);

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

		if (mActivityTask.emojiShowing)
		{
			showEmojiSelector();
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
				onBackPressed();
			}
		});

		doneBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				postStatus();
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	private void setTitle()
	{
		title.setText(moodParent.getVisibility() == View.VISIBLE ? R.string.moods : R.string.status);
	}

	private Runnable cancelStatusPost = new Runnable()
	{

		@Override
		public void run()
		{
			Toast.makeText(getApplicationContext(), R.string.update_status_fail, Toast.LENGTH_SHORT).show();
			mActivityTask.hikeHTTPTask = null;
			HikeMessengerApp.getPubSub().publish(HikePubSub.STATUS_POST_REQUEST_DONE, false);
		}
	};

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

	public void onEmojiClick(View v)
	{
		if (emojiParent.getVisibility() == View.VISIBLE)
		{
			mActivityTask.emojiShowing = false;
			emojiParent.setVisibility(View.GONE);
		}
		else
		{
			showEmojiSelector();
		}
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
		if (emojiParent.getVisibility() == View.VISIBLE)
		{
			mActivityTask.emojiShowing = false;
			emojiParent.setVisibility(View.GONE);
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

	private boolean isEmojiOrMoodLayoutVisible()
	{
		return ((moodParent.getVisibility() == View.VISIBLE) || (findViewById(R.id.emoji_container).getVisibility() == View.VISIBLE));
	}

	private void hideEmojiOrMoodLayout()
	{
		if (moodParent.getVisibility() == View.VISIBLE)
		{
			mActivityTask.moodShowing = false;
			moodParent.setVisibility(View.GONE);
		}
		else if (emojiParent.getVisibility() == View.VISIBLE)
		{
			mActivityTask.emojiShowing = false;
			emojiParent.setVisibility(View.GONE);
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
		HikeHttpRequest hikeHttpRequest = new HikeHttpRequest("/user/status", RequestType.STATUS_UPDATE, new HikeHttpCallback()
		{

			@Override
			public void onSuccess(JSONObject response)
			{
				mActivityTask = new ActivityTask();

				JSONObject data = response.optJSONObject("data");

				String mappedId = data.optString(HikeConstants.STATUS_ID);
				String text = data.optString(HikeConstants.STATUS_MESSAGE);
				int moodId = data.optInt(HikeConstants.MOOD) - 1;
				int timeOfDay = data.optInt(HikeConstants.TIME_OF_DAY);
				String msisdn = preferences.getString(HikeMessengerApp.MSISDN_SETTING, "");
				String name = preferences.getString(HikeMessengerApp.NAME_SETTING, "");
				long time = (long) System.currentTimeMillis() / 1000;

				StatusMessage statusMessage = new StatusMessage(0, mappedId, msisdn, name, text, StatusMessageType.TEXT, time, moodId, timeOfDay);
				HikeConversationsDatabase.getInstance().addStatusMessage(statusMessage, true);

				int unseenUserStatusCount = preferences.getInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0);
				Editor editor = preferences.edit();
				editor.putString(HikeMessengerApp.LAST_STATUS, text);
				editor.putInt(HikeMessengerApp.LAST_MOOD, moodId);
				editor.putInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, ++unseenUserStatusCount);
				editor.putBoolean(HikeConstants.IS_HOME_OVERFLOW_CLICKED, false);
				editor.commit();

				HikeMessengerApp.getPubSub().publish(HikePubSub.MY_STATUS_CHANGED, text);

				/*
				 * This would happen in the case where the user has added a self contact and received an mqtt message before saving this to the db.
				 */
				if (statusMessage.getId() != -1)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.STATUS_MESSAGE_RECEIVED, statusMessage);
					HikeMessengerApp.getPubSub().publish(HikePubSub.TIMELINE_UPDATE_RECIEVED, statusMessage);
				}
				HikeMessengerApp.getPubSub().publish(HikePubSub.STATUS_POST_REQUEST_DONE, true);
			}

			@Override
			public void onFailure()
			{
				Toast.makeText(getApplicationContext(), R.string.update_status_fail, Toast.LENGTH_SHORT).show();
				mActivityTask.hikeHTTPTask = null;
				HikeMessengerApp.getPubSub().publish(HikePubSub.STATUS_POST_REQUEST_DONE, false);
			}

		});
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

		//boolean facebook = findViewById(R.id.post_fb_btn).isSelected();
		//boolean twitter = findViewById(R.id.post_twitter_btn).isSelected();

		Logger.d(getClass().getSimpleName(), "Status: " + status);
		JSONObject data = new JSONObject();
		try
		{
			data.put(HikeConstants.STATUS_MESSAGE_2, status);
			//data.put(HikeConstants.FACEBOOK_STATUS, facebook);
			//data.put(HikeConstants.TWITTER_STATUS, twitter);
			if (mActivityTask.moodId != -1)
			{
				data.put(HikeConstants.MOOD, mActivityTask.moodId + 1);
				data.put(HikeConstants.TIME_OF_DAY, getTimeOfDay());
			}
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), "Invalid JSON", e);
		}
		Logger.d(getClass().getSimpleName(), "JSON: " + data);

		hikeHttpRequest.setJSONData(data);
		mActivityTask.hikeHTTPTask = new HikeHTTPTask(null, 0);
		Utils.executeHttpTask(mActivityTask.hikeHTTPTask, hikeHttpRequest);

		/*
		 * Starting the manual cancel as well.
		 */
		handler.removeCallbacks(cancelStatusPost);
		handler.postDelayed(cancelStatusPost, 60 * 1000);

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

	private void showEmojiSelector()
	{
		Utils.hideSoftKeyboard(this, statusTxt);

		mActivityTask.emojiShowing = true;

		showCancelButton(false);

		emojiParent.setClickable(true);

		emojiParent.setVisibility(View.VISIBLE);

		ViewGroup emoticonLayout = (ViewGroup) findViewById(R.id.emoji_container);

		int whichSubcategory = 0;

		int[] tabDrawables = null;

		int offset = 0;
		int emoticonsListSize = 0;
		tabDrawables = new int[] { R.drawable.emo_recent, R.drawable.emo_tab_5_selector, R.drawable.emo_tab_6_selector, R.drawable.emo_tab_7_selector,
				R.drawable.emo_tab_8_selector, R.drawable.emo_tab_9_selector };
		offset = EmoticonConstants.DEFAULT_SMILEY_RES_IDS.length;
		emoticonsListSize = EmoticonConstants.EMOJI_RES_IDS.length;

		/*
		 * Checking whether we have a few emoticons in the recents category. If not we show the next tab emoticons.
		 */
		if (whichSubcategory == 0)
		{
			int startOffset = offset;
			int endOffset = startOffset + emoticonsListSize;
			int recentEmoticonsSizeReq = EmoticonAdapter.MAX_EMOTICONS_PER_ROW_PORTRAIT;
			int[] recentEmoticons = HikeConversationsDatabase.getInstance().fetchEmoticonsOfType(startOffset, endOffset, recentEmoticonsSizeReq);
			if (recentEmoticons.length < recentEmoticonsSizeReq)
			{
				whichSubcategory++;
			}
		}
		setupEmoticonLayout(whichSubcategory, tabDrawables);
		emoticonLayout.setVisibility(View.VISIBLE);
		
		View eraseKey = (View) findViewById(R.id.erase_key);
		eraseKey.setVisibility(View.VISIBLE);
		eraseKey.setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				statusTxt.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
			}
		});
		
	}

	public void hideEmoticonSelector()
	{
		onBackPressed();
	}

	private void setupEmoticonLayout(int whichSubcategory, int[] tabDrawable)
	{

		EmoticonAdapter statusEmojiAdapter = new EmoticonAdapter(this, this, getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT, tabDrawable,
				true);

		ViewPager emoticonViewPager = (ViewPager) findViewById(R.id.emoticon_pager);
		emoticonViewPager.setAdapter(statusEmojiAdapter);
		emoticonViewPager.invalidate();

		StickerEmoticonIconPageIndicator pageIndicator = (StickerEmoticonIconPageIndicator) findViewById(R.id.icon_indicator);
		pageIndicator.setViewPager(emoticonViewPager);
		pageIndicator.setCurrentItem(whichSubcategory);
	}

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

	private int getTimeOfDay()
	{
		int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		if (hour >= 4 && hour < 12)
		{
			return 1;
		}
		else if (hour >= 12 && hour < 20)
		{
			return 2;
		}
		return 3;
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
		boolean enable = mActivityTask.moodId >= 0 || statusTxt.getText().toString().trim().length() > 0 || isEmojiOrMoodLayoutVisible();
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
					handler.removeCallbacks(cancelStatusPost);
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
		if (emojiParent.getVisibility() == View.VISIBLE)
		{
			mActivityTask.emojiShowing = false;
			emojiParent.setVisibility(View.GONE);
		}
	}

	@Override
	public void onHidden()
	{
	}

	@Override
	public void onEmoticonClicked(int emoticonIndex)
	{
		Utils.emoticonClicked(getApplicationContext(), emoticonIndex, statusTxt);
		
	}
}
