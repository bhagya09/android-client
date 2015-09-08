package com.bsb.hike.ui;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.dialog.CustomAlertDialog;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.models.ConnectedApp;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

/**
 * This class is responsible for displaying "Connected apps" screen in Settings. Also takes care of underlying functionality.
 * 
 * @author AtulM
 * 
 */
public class ConnectedAppsActivity extends HikeAppStateBaseFragmentActivity implements OnClickListener
{

	/** The auth prefs. */
	private HikeSharedPreferenceUtil authPrefs;

	/** The connected app list. */
	private ArrayList<ConnectedApp> connectedAppList;

	private ListView listView;

	private BaseAdapter connectedAppsAdapter;

	private TextView text_view_connected_apps_numbers;

	private boolean isDataChanged;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.bsb.hike.utils.HikeAppStateBaseFragmentActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		authPrefs = HikeSharedPreferenceUtil.getInstance(HikeAuthActivity.AUTH_SHARED_PREF_NAME);

		setContentView(R.layout.connected_apps_main);

		setupActionBar();

		initData();

		bindContentAndActions();
	}

	/**
	 * Initializes the data.
	 */
	private void initData()
	{
		connectedAppList = new ArrayList<ConnectedApp>();

		String connectedPkgCSV = authPrefs.getData(HikeAuthActivity.AUTH_SHARED_PREF_PKG_KEY, "");

		String[] connectedPkgs = connectedPkgCSV.split(",");

		// We have host application package name. Get its name and image from package info
		PackageManager pm = getApplicationContext().getPackageManager();

		for (String connPkg : connectedPkgs)
		{
			ConnectedApp connApp = new ConnectedApp();

			String[] pkgInfo = connPkg.split(":");

			try
			{
				PackageInfo packageInfo = pm.getPackageInfo(pkgInfo[0], PackageManager.GET_ACTIVITIES);
				connApp.setTitle(packageInfo.applicationInfo.loadLabel(getApplicationContext().getPackageManager()).toString());
				connApp.setAppIcon(packageInfo.applicationInfo.loadIcon(pm));
				connApp.setPackageName(connPkg);
				connApp.setVersion(packageInfo.versionName);
			}
			catch (PackageManager.NameNotFoundException e)
			{
				e.printStackTrace();
				isDataChanged = true;
				continue;
			}

			connectedAppList.add(connApp);
		}

	}

	/**
	 * Setup action bar.
	 */
	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(getApplicationContext().getString(R.string.connected_apps));
		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);
	}

	/**
	 * Bind content and actions.
	 */
	private void bindContentAndActions()
	{
		final LayoutInflater inflater = LayoutInflater.from(getApplicationContext());

		connectedAppsAdapter = new BaseAdapter()
		{
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				if (convertView == null)
				{
					convertView = inflater.inflate(R.layout.connected_apps_list_item, null);
				}

				// We have host application package name. Get its name and image from package info
				try
				{

					((TextView) convertView.findViewById(R.id.text_view_conn_app_title)).setText(connectedAppList.get(position).getTitle());

					((TextView) convertView.findViewById(R.id.text_view_conn_app_since)).setText("ver " + connectedAppList.get(position).getVersion());

					((ImageView) convertView.findViewById(R.id.image_view_conn_app_pkg)).setImageDrawable(connectedAppList.get(position).getAppIcon());

					ImageView image_view_disconn_app = ((ImageView) convertView.findViewById(R.id.image_view_disconn_app));

					image_view_disconn_app.setTag(position);

					image_view_disconn_app.setOnClickListener(ConnectedAppsActivity.this);

				}
				catch (ArrayIndexOutOfBoundsException e)
				{
					e.printStackTrace();
				}

				return convertView;
			}

			@Override
			public long getItemId(int position)
			{
				return 0;
			}

			@Override
			public Object getItem(int position)
			{
				return null;
			}

			@Override
			public int getCount()
			{
				return connectedAppList.size();
			}
		};

		text_view_connected_apps_numbers = ((TextView) findViewById(R.id.text_view_connected_apps_numbers));

		if (connectedAppList.isEmpty())
		{
			text_view_connected_apps_numbers.setText(String.format(getString(R.string.connected_apps_to_hike), getString(R.string.no)));
		}
		else
		{
			text_view_connected_apps_numbers.setText(connectedAppList.size() == 1 ? getString(R.string.connected_app_to_hike) : String.format(
					getString(R.string.connected_apps_to_hike), connectedAppList.size()));
		}

		listView = ((ListView) findViewById(R.id.list_view_connected_apps));
		listView.setAdapter(connectedAppsAdapter);
	}

	/**
	 * Disconnect application
	 * 
	 * @param index
	 */
	private void disconnectApp(int index)
	{
		ConnectedApp disconnAppObj = connectedAppList.remove(index);

		isDataChanged = true;

		sendDisconnectedAppAnalytics(disconnAppObj.getTitle());

		invalidateUI();

		authPrefs.removeData(disconnAppObj.getPackageName().split(":")[0]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v)
	{
		try
		{
			final int position = (Integer) v.getTag();
			CustomAlertDialog alertDialog = new CustomAlertDialog(ConnectedAppsActivity.this, 0);  //A dialogId is supplied as well to the constructor. Choosing 0 randomly here
			HikeDialogListener dialogListener = new HikeDialogListener()
			{
				@Override
				public void positiveClicked(HikeDialog hikeDialog)
				{
					disconnectApp(position);
					hikeDialog.dismiss();
				}
				
				@Override
				public void neutralClicked(HikeDialog hikeDialog)
				{
					
				}
				
				@Override
				public void negativeClicked(HikeDialog hikeDialog)
				{
					hikeDialog.dismiss();
				}
			};
			alertDialog.setTitle(getString(R.string.are_you_sure));
			alertDialog.setMessage(getString(R.string.confirm_disconnect_app));
			alertDialog.setPositiveButton(getString(R.string.YES), dialogListener);
			alertDialog.setNegativeButton(getString(R.string.CANCEL), dialogListener);
			alertDialog.show();
		}
		catch (ClassCastException cce)
		{
			cce.printStackTrace();
		}
	}

	/**
	 * Refresh UI
	 */
	private void invalidateUI()
	{
		connectedAppsAdapter.notifyDataSetChanged();

		if (connectedAppList.isEmpty())
		{
			text_view_connected_apps_numbers.setText(String.format(getString(R.string.connected_apps_to_hike), getString(R.string.no)));
		}
		else
		{
			text_view_connected_apps_numbers.setText(connectedAppList.size() == 1 ? getString(R.string.connected_app_to_hike) : String.format(
					getString(R.string.connected_apps_to_hike), connectedAppList.size()));
		}
	}

	/**
	 * Disconnects all connected apps. The apps are stored as comma separated values in shared preferences each having expiry time (:) separated
	 * 
	 * @param argContext
	 */
	public static void disconnectAllApps(Context argContext)
	{
		HikeSharedPreferenceUtil prefs = HikeSharedPreferenceUtil.getInstance(HikeAuthActivity.AUTH_SHARED_PREF_NAME);

		prefs.deleteAllData();
	}

	/**
	 * Disconnects given application package name. The apps are stored as comma separated values in shared preferences, each having expiry time (:) separated
	 * 
	 * @param argContext
	 */
	public static void disconnectApp(Context argContext, String appPkg)
	{
		HikeSharedPreferenceUtil prefs = HikeSharedPreferenceUtil.getInstance(HikeAuthActivity.AUTH_SHARED_PREF_NAME);

		prefs.removeData(appPkg);

		String connectedPkgCSV = prefs.getData(HikeAuthActivity.AUTH_SHARED_PREF_PKG_KEY, "");

		if (TextUtils.isEmpty(connectedPkgCSV))
		{
			return;
		}

		String[] connectedPkgs = connectedPkgCSV.split(",");

		if (connectedPkgs.length > 0)
		{
			StringBuilder sb = null;

			for (String app : connectedPkgs)
			{
				if (app.split(":")[0].equals(appPkg))
				{
					// Do nothing
				}
				else
				{
					// Add to new CSV
					if (sb == null)
					{
						sb = new StringBuilder(app);
					}
					else
					{
						sb.append("," + app);
					}
				}
			}

			if (sb != null && sb.length() > 0)
			{
				prefs.saveData(HikeAuthActivity.AUTH_SHARED_PREF_PKG_KEY, String.valueOf(sb));
			}
			else
			{
				prefs.removeData(HikeAuthActivity.AUTH_SHARED_PREF_PKG_KEY);
			}
		}

	}

	@Override
	protected void onPause()
	{
		super.onPause();
		if (isDataChanged)
		{
			isDataChanged = false;

			saveCurrentData();
		}
	}

	private void saveCurrentData()
	{
		if (connectedAppList.isEmpty())
		{
			disconnectAllApps(getApplicationContext());
		}
		else
		{
			StringBuilder sb = null;

			for (ConnectedApp app : connectedAppList)
			{
				if (sb == null)
				{
					sb = new StringBuilder(app.getPackageName());
				}
				else
				{
					sb.append("," + app.getPackageName());
				}
			}

			authPrefs.saveData(HikeAuthActivity.AUTH_SHARED_PREF_PKG_KEY, String.valueOf(sb));
		}
	}

	private void sendDisconnectedAppAnalytics(String disconnectedAppTitle)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SDK_DISCONNECT_APP);
			metadata.put(HikeConstants.Extras.SDK_THIRD_PARTY_PKG, disconnectedAppTitle);
			metadata.put(HikeConstants.LogEvent.SOURCE_APP, HikePlatformConstants.GAME_SDK_ID);
			HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, HikeConstants.LogEvent.SDK_DISCONNECT_APP, metadata);			
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
}
