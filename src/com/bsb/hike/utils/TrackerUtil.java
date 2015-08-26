package com.bsb.hike.utils;

import android.content.Context;

import com.bsb.hike.HikeConstants;
import com.mobileapptracker.MobileAppTracker;

public class TrackerUtil
{

	private MobileAppTracker _mobileAppTracker = null;

	private MatResponse _matResponse = null;

	private Context _ctx;

	private boolean DEBUG = false; // toggle this field to switch between test

	// mode and live mode
	private static TrackerUtil _trackerUtil = null;

	public TrackerUtil(Context context)
	{
		this._ctx = context;
		init(context);
	}

	public static TrackerUtil getInstance(Context context)
	{
		if (_trackerUtil == null)
		{

			_trackerUtil = new TrackerUtil(context);
		}

		return _trackerUtil;
	}

	private void init(Context context)
	{

		// initialize the MobileAppTracker framework
		_mobileAppTracker = new MobileAppTracker(this._ctx, HikeConstants.MA_TRACKER_AD_ID, HikeConstants.MA_TRACKER_KEY, true, false);

		_matResponse = new MatResponse();

		_mobileAppTracker.setMATResponse(_matResponse);
		_mobileAppTracker.setRefId(HikeConstants.MA_TRACKER_REF_ID_PREFIX + System.currentTimeMillis());
		_mobileAppTracker.setSiteId(HikeConstants.MA_SITE_ID);

	}

	public void setTrackOptions(boolean isNewInstall)
	{

		// Enable these options for debugging only
		if (DEBUG)
		{
			_mobileAppTracker.setAllowDuplicates(true);
			_mobileAppTracker.setDebugMode(true);
		}

		// check if the user is a new user or a returning user.
		// please note that even if this method is called multiple times, the
		// mobile app tracker SDK will pass a no-op internally.
		// if there is no actual version update or new install. This is as per
		// the email communication from mobileAppTracker team
		if (!isNewInstall)
		{
			// the SDK will record an update for all versions including the
			// current version
			_mobileAppTracker.trackUpdate();

		}
		else
		{
			// the SDK will record an install for this version and an update for
			// all subsequent versions.
			_mobileAppTracker.trackInstall();
		}
	}
}
