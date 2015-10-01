package com.bsb.hike.timeline.view;

import java.io.IOException;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.adapters.MoodAdapter;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.media.EmoticonPicker;
import com.bsb.hike.media.PopupListener;
import com.bsb.hike.modules.kpt.KptUtils;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.tasks.StatusUpdateTask;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.EmoticonTextWatcher;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontEditText;
import com.bsb.hike.view.CustomLinearLayout;
import com.bsb.hike.view.CustomLinearLayout.OnSoftKeyboardListener;
import com.bsb.hike.view.RoundedImageView;
import com.kpt.adaptxt.beta.CustomKeyboard;
import com.kpt.adaptxt.beta.RemoveDialogData;
import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.beta.view.AdaptxtEditText.AdaptxtEditTextEventListner;
import com.kpt.adaptxt.beta.view.AdaptxtEditText.AdaptxtKeyboordVisibilityStatusListner;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class StatusUpdate extends HikeAppStateBaseFragmentActivity implements Listener, OnSoftKeyboardListener, PopupListener, View.OnClickListener,
		AdaptxtKeyboordVisibilityStatusListner, AdaptxtEditTextEventListner
{

	private class ActivityTask
	{
		int moodId = -1;

		int moodIndex = -1;

		StatusUpdateTask task;

		boolean emojiShowing = false;

		boolean moodShowing = false;
		
		boolean keyboardShowing = false;

		boolean imageDeleted = false;
	}
	
	private CustomKeyboard mCustomKeyboard;
	
	private boolean systemKeyboard;

	private ActivityTask mActivityTask;

	private static final String TAG = "statusupdate";
	
	private ProgressDialog progressDialog;

	private String[] pubsubListeners = { HikePubSub.SOCIAL_AUTH_COMPLETED, HikePubSub.SOCIAL_AUTH_FAILED, HikePubSub.STATUS_POST_REQUEST_DONE };

	private ViewGroup moodParent;

	private RoundedImageView avatar;

	private CustomFontEditText statusTxt;

	private CustomLinearLayout parentLayout;

	private View doneBtn;

	private TextView postText;

	private ImageView arrow;
	
	private TextView title;
	
	private EmoticonPicker mEmoticonPicker;
	
	private static final int SHOW_EMOJI_PALETTE = 1;
	
	private boolean wasEmojiPreviouslyVisible;
	
	private String IS_IMAGE_DELETED = "is_img_d";

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

	private String mImagePath;

	private ImageView statusImage;
	
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
	

	private IconLoader mIconImageLoader;

	private ImageButton btnRemovePhoto;

	private View addMoodLayout;

	public static final String STATUS_UPDATE_IMAGE_PATH = "SUIMGPTH";

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

		if(savedInstanceState != null)
		{
			mActivityTask.imageDeleted = savedInstanceState.getBoolean(IS_IMAGE_DELETED);
		}
		
		initVarRef();
		
		initEmoticonPicker();
		
		systemKeyboard = HikeMessengerApp.isSystemKeyboard(StatusUpdate.this);
		mEmoticonPicker.setCustomKeyBoard(!systemKeyboard);
		if (!systemKeyboard)
		{
			initCustomKeyboard();			
		}
		
		addOnClickListeners();

		RoundedImageView roundAvatar = (RoundedImageView) avatar;
		roundAvatar.setOval(true);

		setupActionBar();

		readArguments(getIntent());

		mIconImageLoader = new IconLoader(getApplicationContext(), getApplicationContext().getResources().getDimensionPixelSize(R.dimen.icon_picture_size));

		mIconImageLoader.setDefaultAvatarIfNoCustomIcon(false);

		mIconImageLoader.setDefaultDrawableNull(false);

		String selfMsisdn = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, null);

		avatar.setImageDrawable(HikeMessengerApp.getLruCache().getDefaultAvatar(selfMsisdn, false));

		mIconImageLoader.loadImage(selfMsisdn, avatar, false, true, false);

		parentLayout.setOnSoftKeyboardListener(this);

		if (!TextUtils.isEmpty(mImagePath) && !mActivityTask.imageDeleted)
		{
			Bitmap bmp = HikeBitmapFactory.decodeFile(mImagePath);
			if(bmp == null)
			{
				removePhoto(null);
				Toast.makeText(getApplicationContext(), R.string.photos_oom_load, Toast.LENGTH_SHORT).show();
			}
			else
			{
				statusImage.setImageBitmap(bmp);
				statusTxt.setHint(R.string.status_hint_image);
			}
			
		}
		else
		{
			removePhoto(null);
		}

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
			}
		});

		statusTxt.addTextChangedListener(new EmoticonTextWatcher());
		
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
		else if (TextUtils.isEmpty(mImagePath) || mActivityTask.keyboardShowing)
		{
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}

		toggleEnablePostButton();

		HikeMessengerApp.getPubSub().addListeners(this, pubsubListeners);

		showProductPopup(ProductPopupsConstants.PopupTriggerPoints.STATUS.ordinal());

		if (!shouldShowMoodsButton())
		{
			addMoodLayout.setVisibility(View.GONE);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(IS_IMAGE_DELETED, mActivityTask.imageDeleted);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		if (statusImage != null && statusImage.getDrawable() != null)
		{
			ChatThreadUtils.applyMatrixTransformationToImageView(statusImage.getDrawable(), statusImage);
		}
	}

	private void readArguments(Intent intent)
	{
		mImagePath = intent.getStringExtra(STATUS_UPDATE_IMAGE_PATH);
	}

	/**
	 * Initialize variables and references
	 */
	public void initVarRef()
	{
		moodParent = (ViewGroup) findViewById(R.id.mood_parent_status);
		avatar = (RoundedImageView) findViewById(R.id.avatar);
		statusTxt = (CustomFontEditText) findViewById(R.id.status_txt);
		parentLayout = (CustomLinearLayout) findViewById(R.id.parent_layout);
		statusImage = (ImageView) findViewById(R.id.status_image);
		btnRemovePhoto = (ImageButton) findViewById(R.id.btn_remove_photo);
		addMoodLayout = findViewById(R.id.addMoodLayout);
	}
	
	private void initCustomKeyboard()
	{
		View keyboardHolder = (LinearLayout) findViewById(R.id.keyboardView_holder);
		mCustomKeyboard = new CustomKeyboard(StatusUpdate.this, keyboardHolder);
		mCustomKeyboard.registerEditText(R.id.status_txt, KPTConstants.MULTILINE_LINE_EDITOR, this, this);
		mCustomKeyboard.init(statusTxt);
		findViewById(R.id.status_txt).setOnClickListener(this);
		mEmoticonPicker.setCustomKeyBoardHeight(mCustomKeyboard.getKeyBoardAndCVHeight());
		mCustomKeyboard.showCustomKeyboard(statusTxt, true);
		KptUtils.updatePadding(StatusUpdate.this, R.id.parent_layout, mCustomKeyboard.getKeyBoardAndCVHeight());
	}

	@Override
	protected void onStop()
	{
		releaseEmoticon();

		super.onStop();

		if (mActivityTask.moodShowing || mActivityTask.emojiShowing)
		{
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}

		else
		{
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}
		
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		title = (TextView) actionBarView.findViewById(R.id.title);
		doneBtn = actionBarView.findViewById(R.id.done_container);
		arrow = (ImageView) actionBarView.findViewById(R.id.arrow);
		postText = (TextView) actionBarView.findViewById(R.id.post_btn);

		postText.setText(R.string.post);

		doneBtn.setVisibility(View.VISIBLE);

		Utils.toggleActionBarElementsEnable(doneBtn, arrow, postText, false);

		setTitle();

		doneBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				postStatus();
			}
		});

		actionBar.setCustomView(actionBarView);
		Toolbar parent = (Toolbar) actionBarView.getParent();
		parent.setContentInsetsAbsolute(0, 0);
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

	public void onEmojiClick()
	{
		if (mActivityTask.emojiShowing)
		{
			mActivityTask.emojiShowing = false;
			hideEmojiOrMoodLayout();
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
		hideEmojiOrMoodLayout();
		addMoodLayout.setVisibility(View.GONE);
		if (mEmoticonPicker.showEmoticonPicker(getResources().getConfiguration().orientation))
		{
			mActivityTask.emojiShowing = true;
			hideKeyboard();
			showCancelButton(false);
			setEmoticonButtonSelected(true);
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
		if (mEmoticonPicker!= null && mEmoticonPicker.isShowing())
		{
			mActivityTask.emojiShowing = false;
			hideEmojiOrMoodLayout();
		}
		showMoodSelector();
		setTitle();
	}

	public void removePhoto(View dontUseThis)
	{
		btnRemovePhoto.setVisibility(View.GONE);
		statusImage.setImageResource(0);
		statusImage.setVisibility(View.GONE);
		statusTxt.setHint(R.string.status_hint);
		mActivityTask.imageDeleted = true;
		mImagePath = null;
		toggleEnablePostButton();
	}

	@Override
	public void onBackPressed()
	{
		if (isEmojiOrMoodLayoutVisible())
		{
			hideEmojiOrMoodLayout();
			setTitle();
		}else if(mCustomKeyboard.isCustomKeyboardVisible()){
			hideKeyboard();
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
			setEmoticonButtonSelected(false);
		}
		toggleEnablePostButton();

		if (shouldShowMoodsButton())
		{
			addMoodLayout.setVisibility(View.VISIBLE);
		}
	}

	private void postStatus()
	{
		String status = null;
		/*
		 * If the text box is empty, the we take the hint text which is a prefill for moods.
		 */
		if (TextUtils.isEmpty(statusTxt.getText()) && mActivityTask.moodId != -1)
		{
			status = statusTxt.getHint().toString();
		}
		else
		{
			status = statusTxt.getText().toString();
		}

		try
		{
			mActivityTask.task = new StatusUpdateTask(status, mActivityTask.moodId, mImagePath);
		}
		catch (IOException e)
		{
			Toast.makeText(getApplicationContext(), R.string.could_not_post_pic, Toast.LENGTH_SHORT).show();
			e.printStackTrace();
			return;
		}
		
		if(mActivityTask.task != null)
		{
			mActivityTask.task.execute();

			progressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_status));
		}
		
	}

	public void hideEmoticonSelector()
	{
		onBackPressed();
	}

	private void showMoodSelector()
	{
		hideKeyboard();

		addMoodLayout.setVisibility(View.GONE);

		mActivityTask.moodShowing = true;

		showCancelButton(true);

		moodParent.setClickable(true);
		GridView moodPager = (GridView) findViewById(R.id.mood_pager);

		parentLayout.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				if (moodParent != null)
					moodParent.setVisibility(View.VISIBLE);
			}
		}, mActivityTask.keyboardShowing ? 300 : 0); // TODO Remove hack. Use Shareable popup layout

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
		avatar.setOval(false);

		String[] moodsArray = getResources().getStringArray(R.array.mood_headings);
		statusTxt.setHint(moodsArray[moodIndex]);

		toggleEnablePostButton();
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
	
	public void toggleEnablePostButton()
	{
		/*
		 * Enabling if the text length is > 0 or if the user has selected a mood with some prefilled text.
		 */
		boolean enable = mActivityTask.moodId >= 0 || statusTxt.getText().toString().trim().length() > 0 || !TextUtils.isEmpty(mImagePath);
		Utils.toggleActionBarElementsEnable(doneBtn, arrow, postText, enable);
	}

	@Override
	public void onEventReceived(final String type, Object object)
	{
		if (HikePubSub.STATUS_POST_REQUEST_DONE.equals(type))
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
						hideKeyboard();
						Intent in = new Intent(StatusUpdate.this, TimelineActivity.class);
						in.putExtra(HikeConstants.HikePhotos.HOME_ON_BACK_PRESS, true);
						StatusUpdate.this.startActivity(in);
						StatusUpdate.this.finish();
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
		KptUtils.destroyKeyboardResources(mCustomKeyboard, R.id.status_txt);
		
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
	protected void onPause()
	{
		KptUtils.pauseKeyboardResources(mCustomKeyboard);
		super.onPause();
	}
	
	@Override
	public void onShown()
	{
		mActivityTask.keyboardShowing = true;
		hideEmojiOrMoodLayout();
		Logger.d(StatusUpdate.class.getSimpleName(), "shown keyboard");
	}

	@Override
	public void onHidden()
	{
		mActivityTask.keyboardShowing = false;
		Logger.d(StatusUpdate.class.getSimpleName(), "hidden keyboard");
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.emoji_btn:
			setEmoticonButtonSelected(true);
			if (mCustomKeyboard != null)
			{
				mEmoticonPicker.setCustomKeyBoardHeight(mCustomKeyboard.getKeyBoardAndCVHeight());
			}
			onEmojiClick();
			break;
		case R.id.status_txt:
			setEmoticonButtonSelected(false);
			showKeyboard();
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
		int[] dontEatThisTouch = {R.id.emoji_btn, R.id.status_txt};
		mEmoticonPicker = new EmoticonPicker(this, statusTxt, findViewById(R.id.parent_layout), (int)getResources().getDimension(R.dimen.emoticon_pallete), dontEatThisTouch);
		mEmoticonPicker.setOnDismissListener(this);
		mEmoticonPicker.setDisableExtraPadding(true);
		mEmoticonPicker.useStatusUpdateEmojisList(true);
	}
	
	private void setEmoticonButtonSelected(boolean selected)
	{
		findViewById(R.id.emoji_btn).setSelected(selected);
	}
	
	private void releaseEmoticon()
	{
		if (mEmoticonPicker.isShowing())
		{
			hideEmojiOrMoodLayout();
		}
		/**
		 * It is important that along with releasing resources for Emoticons, we also close its window to prevent any BadWindow Exceptions later on.
		 */
		if (mEmoticonPicker != null)
		{
			mEmoticonPicker.releaseReources();
		}
	}

	private boolean shouldShowMoodsButton()
	{
		if (TextUtils.isEmpty(mImagePath))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	@Override
	protected void onStart()
	{
		initEmoticonPicker();
		super.onStart();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) 
	{
		super.onConfigurationChanged(newConfig);
		if (mEmoticonPicker!=null)
		{
			
			if (mEmoticonPicker.isShowing())
			{
				int currentEmoticonPage = mEmoticonPicker.getCurrentItem();
				hideEmojiOrMoodLayout();
				initEmoticonPicker();
				mEmoticonPicker.setCurrentItem(currentEmoticonPage);
				mEmoticonPicker.onOrientationChange(newConfig.orientation);
				mActivityTask.emojiShowing = true;
			}
			else if (mActivityTask.moodShowing)
			{
				moodParent.setVisibility(View.GONE);
				showMoodSelector();
			}
			else
			{
				hideEmojiOrMoodLayout();
				initEmoticonPicker();
			}
		}
	}

	@Override
	public String toString()
	{
		return "StatusUpdate [statusTxt=" + statusTxt + ", title=" + title + ", mImagePath=" + mImagePath + ", statusImage=" + statusImage + "]";
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(item.getItemId()==android.R.id.home)
		{
			hideKeyboard();
			actionBarBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onAdaptxtFocusChange(View arg0, boolean arg1)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAdaptxtTouch(View arg0, MotionEvent arg1)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAdaptxtclick(View arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReturnAction(int arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void analyticalData(String currentLanguage)
	{
		KptUtils.generateKeyboardAnalytics(currentLanguage);
	}

	@Override
	public void onInputViewCreated()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInputviewVisbility(boolean visible, int height)
	{
		if (visible)
		{
			mActivityTask.keyboardShowing = true;
			hideEmojiOrMoodLayout();
			Logger.d(StatusUpdate.class.getSimpleName(), "shown keyboard");
		}
		else
		{
			mActivityTask.keyboardShowing = false;
			Logger.d(StatusUpdate.class.getSimpleName(), "hidden keyboard");
		}
	}

	@Override
	public void showGlobeKeyView()
	{
		KptUtils.onGlobeKeyPressed(StatusUpdate.this, mCustomKeyboard);
	}

	@Override
	public void showQuickSettingView()
	{
		KptUtils.onGlobeKeyPressed(StatusUpdate.this, mCustomKeyboard);
	}
	
	private void hideKeyboard()
	{

		if (systemKeyboard)
		{
			Utils.hideSoftKeyboard(StatusUpdate.this, statusTxt);
		}
		else
		{
			mCustomKeyboard.showCustomKeyboard(statusTxt, false);
			KptUtils.updatePadding(StatusUpdate.this, R.id.parent_layout, 0);
		}
	}

	@Override
	public void dismissRemoveDialog() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showRemoveDialog(RemoveDialogData arg0) {
		// TODO Auto-generated method stub
		
	}
	
	private void showKeyboard()
	{
		if (systemKeyboard)
		{
			statusTxt.setFocusable(true);
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
			Utils.showSoftKeyboard(statusTxt, InputMethodManager.SHOW_FORCED);
		}
		else
		{
			mCustomKeyboard.showCustomKeyboard(statusTxt, true);
			KptUtils.updatePadding(StatusUpdate.this, R.id.parent_layout, mCustomKeyboard.getKeyBoardAndCVHeight());
		}
	}
	
}