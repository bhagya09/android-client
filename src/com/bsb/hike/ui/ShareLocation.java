package com.bsb.hike.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

public class ShareLocation extends HikeAppStateBaseFragmentActivity implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener, GoogleMap.OnCameraChangeListener {

	private GoogleMap map;

	private SupportMapFragment MapFragment;

	private boolean fullScreenFlag = true;

	private LocationManager locManager;

	private Location myLocation = null;

	private boolean gpsDialogShown = false;

	private Marker userMarker;

	private Marker lastMarker;

	// places of interest
	private Marker[] placeMarkers;

	private String searchStr;
	

	// max
	private final int MAX_PLACES = 20;// most returned from google

	// marker options
	private MarkerOptions[] places;

	private ArrayList<ItemDetails> list;

	private ListView listview;

	private ItemListBaseAdapter adapter;

	private Dialog alert;

	private int currentLocationDevice;

	private boolean isTextSearch = false;

	private final int GPS_ENABLED = 1;

	private final int GPS_DISABLED = 2;

	private final int NO_LOCATION_DEVICE_ENABLED = 0;

	private Dialog playServiceErrordialog;

	private int selectedPosition = 0;

	private LocationClient mLocationClient;

	private int SEARCH_RADIUS = 2000; // 2KM

	private boolean selectingLocationManually;

	// These settings are the same as the settings for the map. They will in
	// fact give you updates at
	// the maximal rates currently possible.
	private static final LocationRequest REQUEST = LocationRequest.create().setInterval(1000)
	// 1 seconds
			.setFastestInterval(16)
			// 16ms = 60fps
			.setSmallestDisplacement(4).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

	private View doneBtn;

	private TextView title;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		/*
		 * if isGooglePlayServicesAvailable method returns 2=ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED this implies we need to update our playservice library if it returns
		 * 0=ConnectionResult.SUCCESS this implies we have correct version and working playservice api
		 */
		Logger.d(getClass().getSimpleName(), "is play service available = " + Integer.valueOf(GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)).toString());

		// Getting Google Play availability status
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

		// Showing status
		if (status != ConnectionResult.SUCCESS)
		{ // Google Play Services// are
			// not available
			int requestCode = 10;
			playServiceErrordialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
			playServiceErrordialog.show();
			playServiceErrordialog.setOnDismissListener(new Dialog.OnDismissListener()
			{

				@Override
				public void onDismiss(DialogInterface arg0)
				{
					finish();
				}

			});
			return;

		}
		else
		{ // Google Play Services are available

			setContentView(R.layout.share_location);
			gpsDialogShown = savedInstanceState != null && savedInstanceState.getBoolean(HikeConstants.Extras.GPS_DIALOG_SHOWN);
			listview = (ListView) findViewById(R.id.itemListView);
			list = new ArrayList<ItemDetails>();

			adapter = new ItemListBaseAdapter(this, list);
			listview.setAdapter(adapter);
			listview.setOnItemClickListener(new AdapterView.OnItemClickListener()
			{
				@Override
				public void onItemClick(AdapterView<?> parent, final View view, int position, long id)
				{
//					if (lastMarker != null)
//					{
//						lastMarker.setVisible(false);
//					}
					selectedPosition = position;
					Marker currentMarker = adapter.getMarker(position);
					currentMarker.setVisible(false);
					currentMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_share_location_item));
					lastMarker = currentMarker;
					map.animateCamera(CameraUpdateFactory.newLatLng(currentMarker.getPosition()));
					adapter.notifyDataSetChanged();
					if (selectedPosition != 0)
					{
						mLocationClient.disconnect();
					}
					else
					{
						mLocationClient.connect();
					}
				}
			});

			MapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
			map = MapFragment.getMap();
			map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			map.getUiSettings().setZoomControlsEnabled(false);
			map.getUiSettings().setCompassEnabled(false);
			map.getUiSettings().setMyLocationButtonEnabled(true);

			map.setTrafficEnabled(false);

			// Enable user to manually select location my moving the map around
			map.setOnCameraChangeListener(this);

			// map.setMyLocationEnabled(true);
			setUpLocationClientIfNeeded();
			mLocationClient.connect(); // onConnected is called when connection
										// is made.

			places = new MarkerOptions[MAX_PLACES];
			placeMarkers = new Marker[MAX_PLACES + 1];

			if (savedInstanceState != null)
			{
				isTextSearch = savedInstanceState.getBoolean(HikeConstants.Extras.IS_TEXT_SEARCH);
				searchStr = savedInstanceState.getString(HikeConstants.Extras.HTTP_SEARCH_STR);
				new GetPlaces().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, searchStr);
			}
		}

		Button fullScreenButton = (Button) findViewById(R.id.full_screen_button);
		fullScreenButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				// View mapView = (View) findViewById(R.id.map);
				if (fullScreenFlag)
				{
					fullScreenFlag = false;
					((View) findViewById(R.id.frame)).setLayoutParams(new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 0f));
				}
				else
				{
					fullScreenFlag = true;
					((View) findViewById(R.id.frame)).setLayoutParams(new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 5.68f));
				}
			}
		});

		Button myLocationButton = (Button) findViewById(R.id.my_location_button);
		myLocationButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				selectingLocationManually = false;
				updateMyLocation();
			}
		});

		Button searchButton = (Button) findViewById(R.id.search_button);
		searchButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				try
				{
					String searchString = ((EditText) findViewById(R.id.search)).getText().toString().trim();
					if (!searchString.equals(""))
					{
						searchString = URLEncoder.encode(searchString, "UTF-8");
						double lat = 0;
						double lng = 0;
						if (myLocation != null)
						{
							lat = myLocation.getLatitude();
							lng = myLocation.getLongitude();

//							lastMarker.setVisible(false);
							lastMarker = userMarker;
//							lastMarker.setVisible(true);
							selectedPosition = 0;
							map.animateCamera(CameraUpdateFactory.newLatLng(lastMarker.getPosition()));
							adapter.notifyDataSetChanged();

						}
						searchStr = "https://maps.googleapis.com/maps/api/place/textsearch/" + "json?query=" + searchString + "&location=" + lat + "," + lng + "&radius="
								+ SEARCH_RADIUS + "&sensor=true" + "&key=" + getResources().getString(R.string.places_api_key);// ADD
																																// KEY
						isTextSearch = true;
						new GetPlaces().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, searchStr);

						if (!mLocationClient.isConnected())
						{
							mLocationClient.connect();
						}
					}
				}
				catch (UnsupportedEncodingException e)
				{
					Logger.w(getClass().getSimpleName(), "in nearby search url encoding", e);
				}
			}
		});

		setupActionBar();
	}

	private void init()
	{
		getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.color.blue_hike));
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.share_location);

		doneBtn = actionBarView.findViewById(R.id.done_container);
		doneBtn.setVisibility(View.VISIBLE);

		TextView postText = (TextView) actionBarView.findViewById(R.id.post_btn);
		postText.setText(R.string.send);


		doneBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				sendSelectedLocation();
			}
		});

		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0, 0);

		init();
	}

	private void setUpLocationClientIfNeeded()
	{
		if (mLocationClient == null)
		{
			mLocationClient = new LocationClient(getApplicationContext(), this, // ConnectionCallbacks
					this); // OnConnectionFailedListener
		}
	}

	@Override
	protected void onDestroy()
	{
		if (mLocationClient != null)
		{
			mLocationClient.disconnect();
		}
		super.onDestroy();
	}

	@Override
	public void onLocationChanged(Location newLocation)
	{
		if (selectingLocationManually)
			return;

		if (myLocation != null)
		{
			userMarker.setPosition(new LatLng(newLocation.getLatitude(), newLocation.getLongitude()));
			map.animateCamera(CameraUpdateFactory.newLatLng(userMarker.getPosition()));
			Logger.d("ShareLocation", "is Location changed = " + Double.valueOf(myLocation.distanceTo(newLocation)).toString());
			if ((currentLocationDevice == GPS_ENABLED && myLocation.distanceTo(newLocation) > 100)
					|| (currentLocationDevice == GPS_DISABLED && myLocation.distanceTo(newLocation) > 800))
			{

				myLocation = newLocation;
				userMarker.setPosition(new LatLng(newLocation.getLatitude(), newLocation.getLongitude()));
				updateLocationAddress(myLocation.getLatitude(), myLocation.getLongitude(), userMarker);
				// do something on location change
				Logger.d("ShareLocation", "my longi in loc listener = " + Double.valueOf(newLocation.getLongitude()).toString());
				Logger.d("ShareLocation", "my lati in loc listener = " + Double.valueOf(newLocation.getLatitude()).toString());
				if (!isTextSearch)
				{
					lastMarker = userMarker;
					selectedPosition = 0;
					lastMarker.setVisible(false);
					adapter.notifyDataSetChanged();
					updateNearbyPlaces();
				}
			}
		}
		else
		{
			myLocation = newLocation;
			setMyLocation(newLocation);
			updateNearbyPlaces();
		}

	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0)
	{

	}

	@Override
	public void onConnected(Bundle arg0)
	{
		Logger.d("ShareLocation", "LocationClient Connected");
		if (myLocation == null)
		{
			Logger.d("ShareLocation", "LocationClient Connected inside if");
			updateMyLocation();
		}
		try {
			mLocationClient.requestLocationUpdates(REQUEST, this); // LocationListener
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDisconnected()
	{

	}

	@Override
	protected void onResume()
	{
		super.onResume();
		setUpLocationClientIfNeeded();
		if (selectedPosition == 0)
			mLocationClient.connect();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		Logger.d("ShareLocation", "onPause");
		if (mLocationClient != null)
		{
			Logger.d("ShareLocation", "Disconnecting LocationClient");
			mLocationClient.disconnect();
		}
	}

	public void sendSelectedLocation()
	{
		if (lastMarker == null)
		{
			Logger.d("ShareLocation", "sendSelectedLocation");
			Toast.makeText(getApplicationContext(), R.string.select_location, Toast.LENGTH_SHORT).show();
			return;
		}
		Intent result = new Intent();

		result.putExtra(HikeConstants.Extras.ZOOM_LEVEL, (int) map.getCameraPosition().zoom);
		result.putExtra(HikeConstants.Extras.LATITUDE, lastMarker.getPosition().latitude);
		result.putExtra(HikeConstants.Extras.LONGITUDE, lastMarker.getPosition().longitude);
		setResult(RESULT_OK, result);

		finish();
	}

	protected void onSaveInstanceState(Bundle outState)
	{

		outState.putBoolean(HikeConstants.Extras.IS_TEXT_SEARCH, isTextSearch);
		outState.putString(HikeConstants.Extras.HTTP_SEARCH_STR, searchStr);
		outState.putBoolean(HikeConstants.Extras.GPS_DIALOG_SHOWN, gpsDialogShown);
		super.onSaveInstanceState(outState);
	}

	private void updateMyLocation()
	{
		// get location manager
		showLocationDialog();
		try {
			myLocation = mLocationClient.getLastLocation();
			if (myLocation == null)
				myLocation = locManager.getLastKnownLocation(currentLocationDevice == GPS_ENABLED ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER);
			// myLocation = map.getMyLocation();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Logger.d(getClass().getSimpleName(), "inside updateMyLocation");

		if (myLocation != null)
		{
			setMyLocation(myLocation);
			updateNearbyPlaces();
		}
	}

	private void setMyLocation(Location loc)
	{
		double lat = loc.getLatitude();
		double lng = loc.getLongitude();
		// create LatLng
		LatLng myLatLng = new LatLng(lat, lng);

		// remove any existing marker
		if (userMarker != null)
			userMarker.remove();
		// create and set marker propertie//s
		userMarker = map.addMarker(new MarkerOptions().position(myLatLng).title("My Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_share_location_item)));
		lastMarker = userMarker;
		selectedPosition = 0;
		updateLocationAddress(lat, lng, userMarker);

		CameraPosition cameraPosition = new CameraPosition.Builder().target(myLatLng) // Sets the center of the map to Mountain View
				.zoom(HikeConstants.DEFAULT_ZOOM_LEVEL) // Sets the zoom
				.build(); // Creates a CameraPosition from the builder
		Logger.d(getClass().getSimpleName(), "stting up camera in set my location");
		map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

	}

	private void updateNearbyPlaces()
	{
		// build places query string
		if (searchStr == null)
		{
			searchStr = "https://maps.googleapis.com/maps/api/place/nearbysearch/" + "json?location=" + myLocation.getLatitude() + "," + myLocation.getLongitude() + "&radius="
					+ SEARCH_RADIUS + "&sensor=true" + "&key=" + getResources().getString(R.string.places_api_key);
			;
			isTextSearch = false;
		}
		new GetPlaces().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, searchStr);
	}

	@Override
	public void onCameraChange(CameraPosition cameraPosition) {

		Logger.w(getClass().getSimpleName(), "Location: " + cameraPosition.target.latitude
				+ ", " + cameraPosition.target.longitude);

		selectingLocationManually = true;
		double lat = cameraPosition.target.latitude;
		double lng = cameraPosition.target.longitude;
		// create LatLng
		LatLng myLatLng = new LatLng(lat, lng);

		// remove any existing marker
		if (userMarker != null)
			userMarker.remove();
		// create and set marker propertie//s
		userMarker = map.addMarker(new MarkerOptions().position(myLatLng).title("Custom Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_share_location_item)));
		lastMarker = userMarker;
		lastMarker.setVisible(false);
		selectedPosition = 0;
		updateLocationAddress(lat, lng, userMarker);

	}

	private class GetPlaces extends AsyncTask<String, Void, Integer>
	{

		@Override
		protected Integer doInBackground(String... placesURL)
		{
			// fetch places
			Logger.d(getClass().getSimpleName(), "GetPlaces Async Task do in background");
			JSONObject resultObject = null;
			for (String placeSearchURL : placesURL)
			{
				resultObject = Utils.getJSONfromURL(placeSearchURL);
			}

			JSONArray placesArray = null;
			try
			{
				placesArray = resultObject.getJSONArray("results");
				// loop through places
				for (int p = 0; p < placesArray.length(); p++)
				{
					// parse each place
					// if any values are missing we won't show the marker
					boolean missingValue = false;
					LatLng placeLL = null;
					String placeName = "";
					String address = "";
					try
					{
						// attempt to retrieve place data values
						missingValue = false;
						// get place at this index
						JSONObject placeObject = placesArray.getJSONObject(p);
						// get location section
						JSONObject loc = placeObject.getJSONObject("geometry").getJSONObject("location");
						// read lat lng
						placeLL = new LatLng(Double.valueOf(loc.getString("lat")), Double.valueOf(loc.getString("lng")));

						Logger.d(getClass().getSimpleName(), Integer.valueOf(p).toString() + " = " + (String) placeObject.get("name"));

						// vicinity
						if (!isTextSearch)
							address = placeObject.getString("vicinity");
						else
							address = placeObject.getString("formatted_address");

						// name
						placeName = placeObject.getString("name");
					}
					catch (JSONException jse)
					{
						Log.v(getClass().getSimpleName(), "Places missing value");
						missingValue = true;
						jse.printStackTrace();
					}
					// if values missing we don't display
					if (missingValue)
						places[p] = null;
					else
					{
						places[p] = new MarkerOptions().position(placeLL).title(placeName).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_share_location_item))
								.snippet(address);
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			return placesArray == null ? 0 : placesArray.length();
		}

		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();

			for (int pm = 1; pm < placeMarkers.length; pm++)
			{
				if (placeMarkers[pm] != null)
				{
					placeMarkers[pm].remove();
				}
			}
			Logger.d(getClass().getSimpleName(), "list length before = " + Integer.valueOf(list.size()).toString());
			int listSize = list.size();
			for (int i = listSize - 1; i > 0; i--)
			{
				Logger.d(getClass().getSimpleName(), Integer.valueOf(i).toString() + " = " + list.get(i).getName());
				list.remove(i);
			}
			adapter.notifyDataSetChanged();
			Logger.d(getClass().getSimpleName(), "list length after = " + Integer.valueOf(list.size()).toString());

			Logger.d(getClass().getSimpleName(), "GetPlaces Async Task do in background");
			findViewById(R.id.progress_dialog).setVisibility(View.VISIBLE);
		}

		// process data retrieved from doInBackground
		protected void onPostExecute(Integer totalPlaces)
		{
			for (int p = 0; p < totalPlaces; p++)
			{
				if (places[p] != null)
				{
					placeMarkers[p] = map.addMarker(places[p]);
					addItemToAdapter(places[p].getTitle(), places[p].getSnippet(), placeMarkers[p], false);
					placeMarkers[p].setVisible(false);
					adapter.notifyDataSetChanged();
				}
			}
			if(totalPlaces == 0)
				Toast.makeText(ShareLocation.this, getString(R.string.no_places_found), Toast.LENGTH_SHORT).show();
			findViewById(R.id.progress_dialog).setVisibility(View.GONE);
		}
	}

	private void addItemToAdapter(String str1, String str2, Marker mark, boolean isMyLocation)
	{
		ItemDetails item = new ItemDetails();
		item.setName(str1);
		item.setItemDescription(str2);
		if (isMyLocation)
		{
			adapter.setMarker(0, mark);
			list.add(0, item);
		}
		else
		{
			adapter.setMarker(adapter.getCount(), mark);
			list.add(item);
		}
	}

	private void showLocationDialog()
	{
		if (alert != null && alert.isShowing())
		{
			return;
		}
		locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		boolean hasGps = getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);

		if (locManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
		{
			currentLocationDevice = GPS_ENABLED;
		}
		else if (locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
		{
			currentLocationDevice = GPS_DISABLED;
		}
		else
		{
			currentLocationDevice = NO_LOCATION_DEVICE_ENABLED;
		}

		/*
		 * Don't show anything if the GPS is already enabled or the device does not have gps and the network is enabled or the the GPS dialog was shown once.
		 */
		if (currentLocationDevice == GPS_ENABLED)
		{
			return;
		}
		else if (currentLocationDevice == GPS_DISABLED && (!hasGps || gpsDialogShown))
		{
			return;
		}

		int messageId = currentLocationDevice == GPS_DISABLED ? R.string.gps_disabled : R.string.location_disabled;
		
		alert = HikeDialogFactory.showDialog(this, HikeDialogFactory.GPS_DIALOG, new HikeDialogListener()
		{

			@Override
			public void positiveClicked(HikeDialog hikeDialog)
			{
				Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				startActivity(callGPSSettingIntent);
				hikeDialog.dismiss();
			}

			@Override
			public void neutralClicked(HikeDialog hikeDialog)
			{
			}

			@Override
			public void negativeClicked(HikeDialog hikeDialog)
			{
				gpsDialogShown = currentLocationDevice == GPS_DISABLED;
				hikeDialog.dismiss();
			}
		}, messageId,R.string.OK);

		if (!ShareLocation.this.isFinishing())
			alert.show();

	}

	public class ItemDetails
	{

		private String name;

		private String itemDescription;

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public String getItemDescription()
		{
			return itemDescription;
		}

		public void setItemDescription(String itemDescription)
		{
			this.itemDescription = itemDescription;
		}
	}

	@SuppressLint("UseSparseArrays")
	public class ItemListBaseAdapter extends BaseAdapter
	{
		private ArrayList<ItemDetails> itemDetailsrrayList;

		HashMap<Integer, Marker> positionToLocationMap = new HashMap<Integer, Marker>();

		private LayoutInflater l_Inflater;

		public ItemListBaseAdapter(Context context, ArrayList<ItemDetails> results)
		{
			itemDetailsrrayList = results;
			l_Inflater = LayoutInflater.from(context);

		}

		public int getCount()
		{
			return itemDetailsrrayList.size();
		}

		public Object getItem(int position)
		{
			return itemDetailsrrayList.get(position);
		}

		public long getItemId(int position)
		{
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent)
		{
			ViewHolder holder;
			if (convertView == null)
			{
				convertView = l_Inflater.inflate(R.layout.item_details_view, null);
				holder = new ViewHolder();
				holder.txt_itemName = (TextView) convertView.findViewById(R.id.name);

				holder.txt_itemDescription = (TextView) convertView.findViewById(R.id.itemDescription);

				convertView.setTag(holder);
			}
			else
			{
				holder = (ViewHolder) convertView.getTag();
			}

			holder.txt_itemName.setText(itemDetailsrrayList.get(position).getName());
			// if it is My Location than set my location image to the left
			if (position == 0)
			{
				Drawable dr = (Drawable) getResources().getDrawable(R.drawable.my_location);
				Bitmap bitmap = ((BitmapDrawable) dr).getBitmap();
				// Scale it to required size
				int width = (int) getResources().getDimension(R.dimen.share_my_location_drawable_width);

				Bitmap b = HikeBitmapFactory.createScaledBitmap(bitmap, width, width, Bitmap.Config.RGB_565, true, true, false);
				Drawable scaled_dr = HikeBitmapFactory.getBitmapDrawable(getResources(), b);

				Logger.d("BitmapLocation","size : "+BitmapUtils.getBitmapSize(b));
				holder.txt_itemName.setCompoundDrawablesWithIntrinsicBounds(scaled_dr, null, null, null);
				holder.txt_itemName.setCompoundDrawablePadding((int) getResources().getDimension(R.dimen.share_my_location_drawable_padding));
			}
			else
			{
				holder.txt_itemName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}

			if (position == selectedPosition)
			{
				convertView.findViewById(R.id.isChecked).setVisibility(View.VISIBLE);
			}
			else
			{
				convertView.findViewById(R.id.isChecked).setVisibility(View.GONE);
			}

			holder.txt_itemDescription.setText(itemDetailsrrayList.get(position).getItemDescription());
			// holder.itemImage.setImageResource(itemDetailsrrayList.get(position).getImageNumber());

			return convertView;
		}

		public void setMarker(int position, Marker mark)
		{
			positionToLocationMap.put(position, mark);
			return;
		}

		public Marker getMarker(int position)
		{
			return positionToLocationMap.get(position);
		}

		class ViewHolder
		{
			TextView txt_itemName;

			TextView txt_itemDescription;
		}

	}

	private void updateLocationAddress(final double lat, final double lng, final Marker userMarker)
	{
		/*
		 * Getting the address blocks the UI so we run this code in a background thread.
		 */
		AsyncTask<Void, Void, String> asyncTask = new AsyncTask<Void, Void, String>()
		{

			@Override
			protected String doInBackground(Void... params)
			{
				return getAddressFromPosition(lat, lng, ShareLocation.this);
			}

			@Override
			protected void onPostExecute(String address)
			{
				userMarker.setSnippet(address);
				if (list.size() > 0)
					list.remove(0);
				addItemToAdapter(userMarker.getTitle(), address, userMarker, true);
				adapter.notifyDataSetChanged();
			}
		};
		asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static String getAddressFromPosition(double lat, double lng, Context context)
	{
		String address = "";
		try
		{
			JSONObject resultObj = Utils.getJSONfromURL("http://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + "," + lng + "&sensor=true");
			String Status = resultObj.getString("status");
			Logger.d("ShareLocation", "url = " + "http://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + "," + lng + "&sensor=true");
			if (Status.equalsIgnoreCase("OK"))
			{
				JSONArray Results = resultObj.getJSONArray("results");
				JSONObject zero = Results.getJSONObject(0);
				address = zero.getString("formatted_address");
				Logger.d("ShareLocation", "my address = " + address);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return address;
	}
	
	@Override
	public void onBackPressed()
	{
		finish();
	}
	
}
