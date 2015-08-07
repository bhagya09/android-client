package com.bsb.hike.timeline.view;

import java.io.IOException;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
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
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.adapters.EmoticonAdapter;
import com.bsb.hike.adapters.MoodAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.media.EmoticonPickerListener;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.tasks.StatusUpdateTask;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.EmoticonTextWatcher;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomLinearLayout;
import com.bsb.hike.view.CustomLinearLayout.OnSoftKeyboardListener;
import com.bsb.hike.view.RoundedImageView;
import com.bsb.hike.view.StickerEmoticonIconPageIndicator;

public class StatusUpdate extends HikeAppStateBaseFragmentActivity implements Listener, OnSoftKeyboardListener, EmoticonPickerListener
{

	private class ActivityTask
	{
		int moodId = -1;

		int moodIndex = -1;

		StatusUpdateTask task;

		boolean emojiShowing = false;

		boolean moodShowing = false;
	}

	private ActivityTask mActivityTask;

	private ProgressDialog progressDialog;

	private String[] pubsubListeners = { HikePubSub.SOCIAL_AUTH_COMPLETED, HikePubSub.SOCIAL_AUTH_FAILED, HikePubSub.STATUS_POST_REQUEST_DONE };

	private ViewGroup moodParent;

	private ImageView avatar;

	private ViewGroup emojiParent;

	private EditText statusTxt;

	private CustomLinearLayout parentLayout;

	private View doneBtn;

	private TextView postText;

	private ImageView arrow;

	private TextView title;

	private String mImagePath;

	private ImageView statusImage;

	private IconLoader mIconImageLoader;

	private ImageButton btnRemovePhoto;

	private View addMoodLayout;

	public static final String STATUS_UPDATE_IMAGE_PATH = "SUIMGPTH";

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
			if (mActivityTask.task != null)
			{
				progressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_status));
			}
		}
		else
		{
			mActivityTask = new ActivityTask();
		}

		initVarRef();

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

		if (!TextUtils.isEmpty(mImagePath))
		{
			Bitmap bmp = HikeBitmapFactory.decodeFile(mImagePath);
			statusImage.setImageBitmap(bmp);
			statusTxt.setHint(R.string.status_hint_image);
		}else{
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
		
		statusTxt.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				hideEmojiOrMoodLayout();
			}
		});

		if (mActivityTask.emojiShowing)
		{
			showEmojiSelector();
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		}
		else if (mActivityTask.moodShowing)
		{
			showMoodSelector();
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		}
		else if(TextUtils.isEmpty(mImagePath))
		{
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		}
		
		toggleEnablePostButton();

		HikeMessengerApp.getPubSub().addListeners(this, pubsubListeners);

		showProductPopup(ProductPopupsConstants.PopupTriggerPoints.STATUS.ordinal());

		if (!shouldShowMoodsButton())
		{
			addMoodLayout.setVisibility(View.GONE);
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
		emojiParent = (ViewGroup) findViewById(R.id.emoji_container);
		moodParent = (ViewGroup) findViewById(R.id.mood_parent_status);
		avatar = (ImageView) findViewById(R.id.avatar);
		statusTxt = (EditText) findViewById(R.id.status_txt);
		parentLayout = (CustomLinearLayout) findViewById(R.id.parent_layout);
		statusImage = (ImageView) findViewById(R.id.status_image);
		btnRemovePhoto = (ImageButton) findViewById(R.id.btn_remove_photo);
		addMoodLayout = findViewById(R.id.addMoodLayout);
	}

	@Override
	protected void onStop()
	{
		super.onStop();

		if (mActivityTask.moodShowing || mActivityTask.emojiShowing)
		{
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		}

		else
		{
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
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
				actionBarBackPressed();
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
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);
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

	public void onEmojiClick(View v)
	{
		if (emojiParent.getVisibility() == View.VISIBLE)
		{
			mActivityTask.emojiShowing = false;
			hideEmojiOrMoodLayout();
		}
		else
		{
			showEmojiSelector();
		}
	}

	public void onMoodClick(View v)
	{
		if (emojiParent.getVisibility() == View.VISIBLE)
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
		mImagePath = null;
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
			if (emojiParent.getVisibility() == View.VISIBLE)
			{
				mActivityTask.emojiShowing = false;
				emojiParent.setVisibility(View.GONE);
				super.onBackPressed();
			}
			else
			{
				hideEmojiOrMoodLayout();
				setTitle();
			}
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
		mActivityTask.task.execute();

		progressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_status));
	}

	private void showEmojiSelector()
	{
		Utils.hideSoftKeyboard(this, statusTxt);
		
		hideEmojiOrMoodLayout();
		
		addMoodLayout.setVisibility(View.GONE);

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

		View eraseKey = (View) findViewById(R.id.erase_key_image);
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
		EmoticonAdapter statusEmojiAdapter = new EmoticonAdapter(this.getApplicationContext(), this,
				getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT, tabDrawable, true);

		ViewPager emoticonViewPager = (ViewPager) findViewById(R.id.emoticon_pager);
		emoticonViewPager.setAdapter(statusEmojiAdapter);
		emoticonViewPager.invalidate();

		StickerEmoticonIconPageIndicator pageIndicator = (StickerEmoticonIconPageIndicator) findViewById(R.id.emoticon_icon_indicator);
		pageIndicator.setViewPager(emoticonViewPager);
		pageIndicator.setCurrentItem(whichSubcategory);
	}

	private void showMoodSelector()
	{
		Utils.hideSoftKeyboard(this, statusTxt);

		addMoodLayout.setVisibility(View.GONE);
		
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
	}

	private void showCancelButton(boolean moodLayout)
	{
		if (moodLayout)
		{
		}
	}

	public void toggleEnablePostButton()
	{
		/*
		 * Enabling if the text length is > 0 or if the user has selected a mood with some prefilled text.
		 */
		boolean enable = mActivityTask.moodId >= 0 || statusTxt.getText().toString().trim().length() > 0 || isEmojiOrMoodLayoutVisible() || !TextUtils.isEmpty(mImagePath);
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
						Utils.hideSoftKeyboard(StatusUpdate.this, statusTxt);
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
		hideEmojiOrMoodLayout();
		Logger.d(StatusUpdate.class.getSimpleName(), "shown keyboard");
	}

	@Override
	public void onHidden()
	{
		Logger.d(StatusUpdate.class.getSimpleName(), "hidden keyboard");
	}

	@Override
	public void emoticonSelected(int emoticonIndex)
	{
		Utils.emoticonClicked(getApplicationContext(), emoticonIndex, statusTxt);
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
}
