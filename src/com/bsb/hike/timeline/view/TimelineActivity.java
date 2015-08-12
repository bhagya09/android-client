package com.bsb.hike.timeline.view;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager.BadTokenException;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.ui.PeopleActivity;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class TimelineActivity extends HikeAppStateBaseFragmentActivity implements Listener
{
	UpdatesFragment mainFragment;

	private Handler mHandler = new Handler();

	private TextView overflowIndicator;

	private MenuItem activityFeedMenuItem;

	private SharedPreferences accountPrefs;

	private int unreadCounter = -1;

	public static final int NO_FEED_PRESENT = -1;

	public static final int FETCH_FEED_FROM_DB = -2;

	private String[] homePubSubListeners = { HikePubSub.FAVORITE_COUNT_CHANGED, HikePubSub.ACTIVITY_FEED_COUNT_CHANGED , HikePubSub.TIMELINE_WIPE};

	private final String FRAGMENT_ACTIVITY_FEED_TAG = "fragmentActivityFeedTag";
	
	private final String MAIN_ACTIVITY_FEED_TAG = "mainActivityFeedTag";

	private PopupWindow overFlowWindow;

	@Override
	public void onEventReceived(String type, Object object)
	{
		super.onEventReceived(type, object);
		if (HikePubSub.FAVORITE_COUNT_CHANGED.equals(type))
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					updateFriendsNotification(accountPrefs.getInt(HikeMessengerApp.FRIEND_REQ_COUNT, 0), 0);
				}
			});
		}
		if (HikePubSub.ACTIVITY_FEED_COUNT_CHANGED.equals(type))
		{
			final int count = ((Integer) object).intValue();
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					unreadCounter = count;
					invalidateOptionsMenu();
				}
			});
		}
		if (HikePubSub.TIMELINE_WIPE.equals(type))
		{
			HikeConversationsDatabase.getInstance().clearTable(DBConstants.STATUS_TABLE);
			HikeConversationsDatabase.getInstance().clearTable(DBConstants.FEED_TABLE);
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					unreadCounter = NO_FEED_PRESENT;
					invalidateOptionsMenu();
				}
			});
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		initialiseTimelineScreen(savedInstanceState);
		accountPrefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		showProductPopup(ProductPopupsConstants.PopupTriggerPoints.TIMELINE.ordinal());

		if (getIntent() != null && getIntent().getBooleanExtra(HikeConstants.Extras.OPEN_ACTIVITY_FEED, false))
		{
			loadActivityFeedFragment();
		}

		FetchUnreadFeedsTask fetchUnreadFeedsTask = new FetchUnreadFeedsTask();

		if (Utils.isHoneycombOrHigher())
		{
			fetchUnreadFeedsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		else
		{
			fetchUnreadFeedsTask.execute();
		}
	}

	private void initialiseTimelineScreen(Bundle savedInstanceState)
	{
		setContentView(R.layout.timeline);
		setupMainFragment(savedInstanceState);
		setupActionBar();
		HikeMessengerApp.getPubSub().addListeners(this, homePubSubListeners);
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setIcon(R.drawable.hike_logo_top_bar);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);
		actionBarView.findViewById(R.id.seprator).setVisibility(View.GONE);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.timeline);

		backContainer.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	private void setupMainFragment(Bundle savedInstanceState)
	{

		if(getSupportFragmentManager().findFragmentByTag(MAIN_ACTIVITY_FEED_TAG) == null)
		{
			if(mainFragment == null)
			{
				mainFragment = new UpdatesFragment();
			}
			getSupportFragmentManager().beginTransaction().replace(R.id.parent_layout, mainFragment,MAIN_ACTIVITY_FEED_TAG).addToBackStack(MAIN_ACTIVITY_FEED_TAG).commit();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.updates_menu, menu);

		final MenuItem menuItem = menu.findItem(R.id.overflow_menu);
		final View overflowMenuItem = menuItem.getActionView();
		overflowMenuItem.setContentDescription("Timeline Overflow");
		overflowIndicator = (TextView) overflowMenuItem.findViewById(R.id.top_bar_indicator_text);
		activityFeedMenuItem = menu.findItem(R.id.activity_feed);
		activityFeedMenuItem.setVisible(false);
		updateFriendsNotification(accountPrefs.getInt(HikeMessengerApp.FRIEND_REQ_COUNT, 0), 0);
		overflowMenuItem.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showTimelineOverFlow();
			}
		});

		activityFeedMenuItem.getActionView().setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				loadActivityFeedFragment();
				logAnalyticLogs();
			}
		});
		return super.onCreateOptionsMenu(menu);
	}

	private void showTimelineOverFlow()
	{
		final ArrayList<OverFlowMenuItem> optionsList = new ArrayList<OverFlowMenuItem>();

		if(!mainFragment.isEmpty())
		{
			optionsList.add(new OverFlowMenuItem(getString(R.string.clear_timeline), 0, 0, R.string.clear_timeline));
		}
		
		optionsList.add(new OverFlowMenuItem(getString(R.string.favourites), 0, 0, R.string.favourites));

		optionsList.add(new OverFlowMenuItem(getString(R.string.my_profile), 0, 0, R.string.my_profile));

		overFlowWindow = new PopupWindow(this);

		FrameLayout homeScreen = (FrameLayout) findViewById(R.id.home_screen);

		View parentView = getLayoutInflater().inflate(R.layout.overflow_menu, homeScreen, false);

		overFlowWindow.setContentView(parentView);

		ListView overFlowListView = (ListView) parentView.findViewById(R.id.overflow_menu_list);

		TimelineOverflowAdapter overflowAdapter = new TimelineOverflowAdapter(this, R.layout.over_flow_menu_item, R.id.item_title, optionsList);

		overFlowListView.setAdapter(overflowAdapter);

		overFlowListView.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
			{
				overFlowWindow.dismiss();
				OverFlowMenuItem item = (OverFlowMenuItem) adapterView.getItemAtPosition(position);

				switch (item.id)
				{
				case R.string.clear_timeline:

					HikeDialogFactory.showDialog(TimelineActivity.this, HikeDialogFactory.WIPE_TIMELINE_DIALOG, new HikeDialogListener()
					{
						@Override
						public void positiveClicked(final HikeDialog hikeDialog)
						{
							HikeHandlerUtil.getInstance().postRunnableWithDelay(new Runnable()
							{
								@Override
								public void run()
								{
									HikeConversationsDatabase.getInstance().clearTable(DBConstants.STATUS_TABLE);
									HikeMessengerApp.getPubSub().publish(HikePubSub.TIMELINE_WIPE, null);
								}
							}, 0);
							if (hikeDialog != null && hikeDialog.isShowing())
							{
								hikeDialog.dismiss();
							}
						}

						@Override
						public void negativeClicked(HikeDialog hikeDialog)
						{
							if (hikeDialog != null && hikeDialog.isShowing())
							{
								hikeDialog.dismiss();
							}
						}

						@Override
						public void neutralClicked(HikeDialog hikeDialog)
						{

						}
					});

					break;
				case R.string.favourites:
					Intent intent = new Intent(TimelineActivity.this, PeopleActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					break;
				case R.string.my_profile:
					Intent intent2 = new Intent(TimelineActivity.this, ProfileActivity.class);
					intent2.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
					startActivity(intent2);
					break;
				}
			}
		});

		overFlowWindow.setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
		overFlowWindow.setOutsideTouchable(true);
		overFlowWindow.setFocusable(true);
		overFlowWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.overflow_menu_width));
		overFlowWindow.setHeight(LayoutParams.WRAP_CONTENT);
		/*
		 * In some devices Activity crashes and a BadTokenException is thrown by showAsDropDown method. Still need to find out exact repro of the bug.
		 */
		try
		{
			int rightMargin = getResources().getDimensionPixelSize(R.dimen.overflow_menu_right_margin);
			overFlowWindow.showAsDropDown(findViewById(R.id.overflow_anchor), -rightMargin, 0);
		}
		catch (BadTokenException e)
		{
			Logger.e(getClass().getSimpleName(), "Excepetion in HomeActivity Overflow popup", e);
		}
		overFlowWindow.getContentView().setFocusableInTouchMode(true);
		overFlowWindow.getContentView().setOnKeyListener(new View.OnKeyListener()
		{
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				return onKeyUp(keyCode, event);
			}
		});

	}

	public class TimelineOverflowAdapter extends ArrayAdapter<OverFlowMenuItem>
	{

		public TimelineOverflowAdapter(Context context, int resource, int textViewResourceId, List<OverFlowMenuItem> objects)
		{
			super(context, resource, textViewResourceId, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			if (convertView == null)
			{
				convertView = getLayoutInflater().inflate(R.layout.over_flow_menu_item, parent, false);
			}

			OverFlowMenuItem item = getItem(position);

			TextView itemTextView = (TextView) convertView.findViewById(R.id.item_title);
			itemTextView.setText(item.text);

			return convertView;
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		updateFeedsNotification(unreadCounter);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Intent intent = null;

		switch (item.getItemId())
		{
		case R.id.new_update:
			intent = new Intent(this, StatusUpdate.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.POST_UPDATE_FROM_TOP_BAR);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch (JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}
			if (intent != null)
			{
				startActivity(intent);
				return true;
			}

		case R.id.activity_feed:
			loadActivityFeedFragment();
			return true;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed()
	{
		int count = getSupportFragmentManager().getBackStackEntryCount();
		if (count == 1)
		{
			IntentFactory.openHomeActivity(TimelineActivity.this, true);
			super.onBackPressed();
		}
		else 
		{
			getSupportFragmentManager().popBackStack();
			ActionBar actionBar = getSupportActionBar();
			View actionBarView = actionBar.getCustomView();
			View backContainer = actionBarView.findViewById(R.id.back);

			TextView title = (TextView) actionBarView.findViewById(R.id.title);
			title.setText(R.string.timeline);

			backContainer.setOnClickListener(new View.OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					onBackPressed();
				}
			});
		}

	}

	@Override
	protected void onResume()
	{
		Utils.resetUnseenStatusCount(this);
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, this);
		super.onResume();
	}

	@Override
	protected void onPause()
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, null);
		super.onPause();
	}

	@Override
	protected void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, homePubSubListeners);
		super.onDestroy();
	}

	public void updateFriendsNotification(int count, int delayTime)
	{
		if (count < 1)
		{
			overflowIndicator.setVisibility(View.GONE);
		}
		else
		{
			mHandler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					if (overflowIndicator != null)
					{
						int count = accountPrefs.getInt(HikeMessengerApp.FRIEND_REQ_COUNT, 0);
						if (count > 9)
						{
							overflowIndicator.setVisibility(View.VISIBLE);
							overflowIndicator.setText("9+");
							overflowIndicator.startAnimation(Utils.getNotificationIndicatorAnim());
						}
						else if (count > 0)
						{
							overflowIndicator.setVisibility(View.VISIBLE);
							overflowIndicator.setText(String.valueOf(count));
							overflowIndicator.startAnimation(Utils.getNotificationIndicatorAnim());
						}
					}
				}
			}, delayTime);
		}
	}

	public void updateFeedsNotification(final int count)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				final TextView activityFeedTopBarIndicator = (TextView) activityFeedMenuItem.getActionView().findViewById(R.id.top_bar_indicator_text);
				if (activityFeedTopBarIndicator != null)
				{
					if (count == NO_FEED_PRESENT)
					{
						activityFeedMenuItem.setVisible(false);
					}
					else
					{
						activityFeedMenuItem.setVisible(true);
						if (count == 0)
						{
							activityFeedTopBarIndicator.setVisibility(View.GONE);
						}
						else
						{
							if (count > 9)
							{
								activityFeedTopBarIndicator.setText("9+");
							}
							else if (count > 0)
							{
								activityFeedTopBarIndicator.setText(String.valueOf(count));
							}
							activityFeedTopBarIndicator.setVisibility(View.VISIBLE);
							activityFeedTopBarIndicator.startAnimation(Utils.getNotificationIndicatorAnim());
						}
					}
				}
			}
		});
	}

	@Override
	protected void openImageViewer(Object object)
	{
		/*
		 * Making sure we don't add the fragment if the activity is finishing.
		 */
		if (isFinishing())
		{
			return;
		}
	}

	private void loadActivityFeedFragment()
	{
		ActivityFeedFragment activityFeedFragment = new ActivityFeedFragment();
		getSupportFragmentManager().beginTransaction().replace(R.id.parent_layout, activityFeedFragment, FRAGMENT_ACTIVITY_FEED_TAG).addToBackStack(FRAGMENT_ACTIVITY_FEED_TAG).commit();
	}

	class FetchUnreadFeedsTask extends AsyncTask<Void, Void, Integer>
	{

		@Override
		protected Integer doInBackground(Void... params)
		{
			if (HikeConversationsDatabase.getInstance().isAnyFeedEntryPresent())
			{
				return HikeConversationsDatabase.getInstance().getUnreadActivityFeedCount();
			}
			else
			{
				return NO_FEED_PRESENT;
			}
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			unreadCounter = result;
		}

	}

	@SuppressLint("NewApi")
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if (Build.VERSION.SDK_INT <= 10 || (Build.VERSION.SDK_INT >= 14 && ViewConfiguration.get(this).hasPermanentMenuKey()))
		{
			if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_MENU && (getSupportFragmentManager().getBackStackEntryCount() == 0))
			{
				if (overFlowWindow == null || !overFlowWindow.isShowing())
				{
					showTimelineOverFlow();
				}
				else
				{
					overFlowWindow.dismiss();
				}
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);

	}

	private void logAnalyticLogs()
	{
		TextView activityFeedTopBarIndicator = (TextView) activityFeedMenuItem.getActionView().findViewById(R.id.top_bar_indicator_text);
		JSONObject metadata = new JSONObject();
		try
		{
			if (activityFeedTopBarIndicator.isShown())
			{
				metadata.put(AnalyticsConstants.EVENT_SOURCE, AnalyticsConstants.WITH_RED_DOT);
			}
			else
			{
				metadata.put(AnalyticsConstants.EVENT_SOURCE, "null");
			}
			metadata.put(AnalyticsConstants.APP_VERSION_NAME, AccountUtils.getAppVersion());
			metadata.put(HikeConstants.LogEvent.OS_VERSION, Build.VERSION.RELEASE);

			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ACTIVITY_FEED_ACTIONBAR_CLICK);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, HAManager.EventPriority.HIGH, metadata);
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}
}
