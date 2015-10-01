package com.bsb.hike.platform;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class GpsLocation implements LocationListener
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

	public void getLocation()
	{
		if (isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
		{
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		}
		else
		{
			pubSub.publish(HikePubSub.LOCATION_AVAILABLE, locationManager);
		}

	}

	@Override
	public void onLocationChanged(Location location)
	{

		pubSub.publish(HikePubSub.LOCATION_AVAILABLE, locationManager);
		locationManager.removeUpdates(this);

	}

	@Override
	public void onProviderDisabled(String provider)
	{
	}

	@Override
	public void onProviderEnabled(String provider)
	{
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{
	}

}