package com.bsb.hike.ui;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
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
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.productpopup.DialogPojo;
import com.bsb.hike.productpopup.HikeDialogFragment;
import com.bsb.hike.productpopup.IActivityPopup;
import com.bsb.hike.productpopup.ProductContentModel;
import com.bsb.hike.productpopup.ProductInfoManager;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.ui.fragments.UpdatesFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class TimelineActivity extends HikeAppStateBaseFragmentActivity implements Listener
{
	UpdatesFragment mainFragment;

	private Handler mHandler = new Handler();

	private TextView friendsTopBarIndicator;

	private SharedPreferences accountPrefs;

	private String[] homePubSubListeners = { HikePubSub.FAVORITE_COUNT_CHANGED };

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
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		initialiseTimelineScreen(savedInstanceState);
		accountPrefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		showProductPopup(ProductPopupsConstants.PopupTriggerPoints.TIMELINE.ordinal());
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

		View show_people_view = menu.findItem(R.id.show_people).getActionView();
		show_people_view.findViewById(R.id.overflow_icon_image).setContentDescription("Favorites in timeline");
		;
		friendsTopBarIndicator = (TextView) show_people_view.findViewById(R.id.top_bar_indicator);
		((ImageView) show_people_view.findViewById(R.id.overflow_icon_image)).setImageResource(R.drawable.ic_show_people);
		updateFriendsNotification(accountPrefs.getInt(HikeMessengerApp.FRIEND_REQ_COUNT, 0), 0);

		show_people_view.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(TimelineActivity.this, PeopleActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Intent intent = null;

		if (item.getItemId() == R.id.new_update)
		{
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
		}

		if (intent != null)
		{
			startActivity(intent);
			return true;
		}
		else
		{
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onBackPressed()
	{
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(HikeConstants.IMAGE_FRAGMENT_TAG);
		if (!(fragment != null && fragment.isVisible()) && getIntent().getBooleanExtra(HikeConstants.Extras.FROM_NOTIFICATION, false))
		{
			Intent intent = new Intent(TimelineActivity.this, HomeActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		}

		super.onBackPressed();
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
			friendsTopBarIndicator.setVisibility(View.GONE);
		}
		else
		{
			mHandler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					if (friendsTopBarIndicator != null)
					{
						int count = accountPrefs.getInt(HikeMessengerApp.FRIEND_REQ_COUNT, 0);
						if (count > 9)
						{
							friendsTopBarIndicator.setVisibility(View.VISIBLE);
							friendsTopBarIndicator.setText("9+");
							friendsTopBarIndicator.startAnimation(Utils.getNotificationIndicatorAnim());
						}
						else if (count > 0)
						{
							friendsTopBarIndicator.setVisibility(View.VISIBLE);
							friendsTopBarIndicator.setText(String.valueOf(count));
							friendsTopBarIndicator.startAnimation(Utils.getNotificationIndicatorAnim());
						}
					}
				}
			}, delayTime);
		}
	}

}
