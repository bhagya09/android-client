package com.bsb.hike.timeline.view;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
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
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.ui.ImageViewerActivity;
import com.bsb.hike.ui.PeopleActivity;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.StatusUpdate;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class TimelineActivity extends HikeAppStateBaseFragmentActivity implements Listener
{
	UpdatesFragment mainFragment;

	private Handler mHandler = new Handler();

	private TextView overflowIndicator;
	
	//private TextView activityFeedTopBarIndicator;
	private MenuItem activityFeedMenuItem;
	
	private SharedPreferences accountPrefs;
	
	private int unreadCounter = -1;

	private String[] homePubSubListeners = { HikePubSub.FAVORITE_COUNT_CHANGED, HikePubSub.ACTIVITY_FEED_COUNT_CHANGED };

	private final String FRAGMENT_ACTIVITY_FEED_TAG = "fragmentActivityFeedTag";
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
		if (savedInstanceState != null)
		{
			return;
		}

		mainFragment = new UpdatesFragment();

		getSupportFragmentManager().beginTransaction().add(R.id.parent_layout, mainFragment).commit();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getSupportMenuInflater().inflate(R.menu.updates_menu, menu);

		final Menu m = menu;
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
				showTimelineMenuPopup(overflowMenuItem);
			}
		});
		
		activityFeedMenuItem.getActionView().setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				loadActivityFeedFragment();
			}
		});
		return super.onCreateOptionsMenu(menu);
	}
	
	public void showTimelineMenuPopup(View v)
	{
		PopupMenu popup = new PopupMenu(TimelineActivity.this, v);
		android.view.MenuInflater inflater = popup.getMenuInflater();
		inflater.inflate(R.menu.timeline_overflow_menu, popup.getMenu());
		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
		{
			@Override
			public boolean onMenuItemClick(android.view.MenuItem arg0)
			{
				switch (arg0.getItemId())
				{
				case R.id.clear_timeline:

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
				case R.id.favourites:
					Intent intent = new Intent(TimelineActivity.this, PeopleActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					break;
				case R.id.my_profile:
					Intent intent2 = new Intent(TimelineActivity.this, ProfileActivity.class);
					intent2.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
					startActivity(intent2);
					break;
				default:
					break;
				}
				return false;
			}
		});
		popup.show();
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
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(HikeConstants.IMAGE_FRAGMENT_TAG);
		if (!(fragment != null && fragment.isVisible()) && (getIntent().getBooleanExtra(HikeConstants.Extras.FROM_NOTIFICATION, false) || getIntent().getBooleanExtra(HikeConstants.HikePhotos.HOME_ON_BACK_PRESS, false)))
		{
			IntentFactory.openHomeActivity(TimelineActivity.this, true);
		}
		else
		{
			int count = getSupportFragmentManager().getBackStackEntryCount();
			if (count == 0)
			{
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
				final TextView activityFeedTopBarIndicator = (TextView)activityFeedMenuItem.getActionView().findViewById(R.id.top_bar_indicator_text);
				if (activityFeedTopBarIndicator != null)
				{
					if(count < 1)
					{
						activityFeedMenuItem.setVisible(false);
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
						activityFeedMenuItem.setVisible(true);
						activityFeedTopBarIndicator.setVisibility(View.VISIBLE);
						activityFeedTopBarIndicator.startAnimation(Utils.getNotificationIndicatorAnim());
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

		Bundle arguments = (Bundle) object;
		
		Intent intent = new Intent(this, ImageViewerActivity.class);
		intent.putExtras(arguments);
		startActivity(intent);
	}
	
	private void loadActivityFeedFragment()
	{
		ActivityFeedFragment activityFeedFragment = new ActivityFeedFragment();
		getSupportFragmentManager().
		beginTransaction().
		add(R.id.parent_layout, activityFeedFragment, FRAGMENT_ACTIVITY_FEED_TAG).
		addToBackStack(null).
		commit();

	}
	
	class FetchUnreadFeedsTask extends AsyncTask<Void, Void, Integer>
	{

		@Override
		protected Integer doInBackground(Void... params)
		{
			return HikeConversationsDatabase.getInstance().getUnreadActivityFeedCount();
		}
		
		@Override
		protected void onPostExecute(Integer result)
		{
			//updateFeedsNotification(result);
			unreadCounter = result;
		}
		
	}
}
