package com.bsb.hike.platform;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

public class GpsLocation
{

	boolean isGPSEnabled;

	boolean canGetLocation;

	protected LocationManager locationManager;

	private volatile static GpsLocation instance;

	public static String TAG = "GetGpsLocation";

	private HikePubSub pubSub;

	Context mContext;

	private GpsLocation()
	{
		isGPSEnabled = false;
		canGetLocation = false;
		this.pubSub = HikeMessengerApp.getPubSub();
		mContext = HikeMessengerApp.getInstance().getApplicationContext();
		locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

	}

	public static GpsLocation getInstance()
	{
		if (instance == null)
		{
			synchronized (GpsLocation.class)
			{
				if (instance == null)
				{
					instance = new GpsLocation();
				}
			}
		}
		return instance;
	}

	public void getLocation(LocationListener locationListener)
	{
		if (isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
		{
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		}
		else
		{
			pubSub.publish(HikePubSub.LOCATION_AVAILABLE, locationManager);
		}
	}

	public void requestRecurringLocation(LocationListener locationListener, long timeInterval, long duration)
	{
		if (timeInterval < 0 || duration < 0)
		{
			Logger.e(TAG, "Time interval or duration for recurring location updates are incorrect. Returning.");
			return;
		}

		HikeSharedPreferenceUtil.getInstance().saveData(HikePlatformConstants.RECURRING_LOCATION_END_TIME, System.currentTimeMillis() + duration);
		HikeSharedPreferenceUtil.getInstance().saveData(HikePlatformConstants.TIME_INTERVAL, timeInterval);

		// Running on HikeHandler because "The calling thread must be a Looper thread such as the main thread of the calling Activity."
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
		{
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, timeInterval, 0, locationListener, HikeHandlerUtil.getInstance().getLooper());
		}
		// Fallback to NETWORK_PROVIDER in case the GPS_PROVIDER isn't available.
		else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
		{
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, timeInterval, 0, locationListener, HikeHandlerUtil.getInstance().getLooper());
		}
		else
		{
			locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, timeInterval, 0, locationListener, HikeHandlerUtil.getInstance().getLooper());
		}
	}

	public LocationManager getLocationManager()
	{
		return this.locationManager;
	}

	public void removeUpdates(LocationListener locationListener)
	{
		if (locationManager != null)
		{
			locationManager.removeUpdates(locationListener);
		}
	}
}