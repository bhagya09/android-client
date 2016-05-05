package com.bsb.hike.modules.gcmnetworkmanager;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Logger;
import com.google.android.gms.gcm.GcmTaskService;

import java.util.List;

/**
 * Created by anubhavgupta on 28/04/16.
 */
public class Config
{
	public static final String LOG_TAG = "Config";

	public static final String TAG = "tag";

	public static final String NUM_OF_RETRIES = "num_of_retries";

	public static final String UPDATE_CURRENT = "update_current";

	public static final String PERSISTED = "persisted";

	public static final String SERVICE = "service";

	public static final String REQUIRED_NETWORK = "requiredNetwork";

	public static final String REQUIRES_CHARGING = "requiresCharging";

	public static final String EXTRAS = "extras";

	public static final String WINDOW_START = "window_start";

	public static final String WINDOW_END = "window_end";

	private int numRetries = HikeConstants.DEFAULT_RETRIES_GCM_NW_MANAGER;

	private long windowStartDelaySeconds;

	private long windowEndDelaySeconds;

	private Class<? extends GcmTaskService> gcmTaskService;

	private int requiredNetworkType;

	private boolean requiresCharging;

	private String mTag;

	private boolean persisted;

	private boolean updateCurrent;

	private Bundle mExtras;

	private Config(Builder builder)
	{
		this.numRetries = builder.numRetries;
		this.windowStartDelaySeconds = builder.windowStartDelaySeconds;
		this.windowEndDelaySeconds = builder.windowEndDelaySeconds;
		this.gcmTaskService = builder.gcmTaskService;
		this.requiredNetworkType = builder.requiredNetworkType;
		this.requiresCharging = builder.requiresCharging;
		this.mTag = builder.tag;
		this.persisted = builder.persisted;
		this.updateCurrent = builder.updateCurrent;
		this.mExtras = builder.extras;
		ensureSaneDefaults();
	}

	private void ensureSaneDefaults()
	{
        if(gcmTaskService == null)
        {
            gcmTaskService = GcmNwMgrService.class;
        }

        if(TextUtils.isEmpty(mTag))
        {
            throw new IllegalArgumentException("must specify tag");
        }
	}

	public void decrementRetries()
	{
		this.numRetries--;
	}

	public int getNumRetries()
	{
		return numRetries;
	}

	public long getWindowStart()
	{
		return this.windowStartDelaySeconds;
	}

	public long getWindowEnd()
	{
		return this.windowEndDelaySeconds;
	}

	public Class<? extends GcmTaskService> getService()
	{
		return this.gcmTaskService;
	}

	public String getTag()
	{
		return this.mTag;
	}

	public boolean isUpdateCurrent()
	{
		return this.updateCurrent;
	}

	public boolean isPersisted()
	{
		return this.persisted;
	}

	public int getRequiredNetwork()
	{
		return this.requiredNetworkType;
	}

	public boolean getRequiresCharging()
	{
		return this.requiresCharging;
	}

	public Bundle getExtras()
	{
		return this.mExtras;
	}

	public Bundle toBundle() 
	{
		Bundle bundle = new Bundle();
		bundle.putString(TAG, this.mTag);
		bundle.putInt(NUM_OF_RETRIES, this.numRetries);
		bundle.putBoolean(UPDATE_CURRENT, this.updateCurrent);
		bundle.putBoolean(PERSISTED, this.persisted);
		bundle.putString(SERVICE, this.gcmTaskService.getName());
		bundle.putInt(REQUIRED_NETWORK, this.requiredNetworkType);
		bundle.putBoolean(REQUIRES_CHARGING, this.requiresCharging);
		bundle.putBundle(EXTRAS, this.mExtras);
		bundle.putLong(WINDOW_START, this.windowStartDelaySeconds);
		bundle.putLong(WINDOW_END, this.windowEndDelaySeconds);
		return bundle;
	}

	public static Config fromBundle(Bundle bundle)
	{
		Class<? extends GcmTaskService> gcmTaskService = getClassFromString(bundle.getString(SERVICE));
		Config config = new Config.Builder()
				.setTag(bundle.getString(TAG))
				.setNumRetries(bundle.getInt(NUM_OF_RETRIES))
				.setService(gcmTaskService)
				.setUpdateCurrent(bundle.getBoolean(UPDATE_CURRENT))
				.setPersisted(bundle.getBoolean(PERSISTED))
				.setRequiredNetwork(bundle.getInt(REQUIRED_NETWORK))
				.setRequiresCharging(bundle.getBoolean(REQUIRES_CHARGING))
				.setExtras(bundle.getBundle(EXTRAS))
				.setExecutionWindow(bundle.getLong(WINDOW_START), bundle.getLong(WINDOW_END))
				.build();
		return config;
	}

	private static Class<? extends GcmTaskService> getClassFromString(String name)
	{
		Intent intent = new Intent("com.google.android.gms.gcm.ACTION_TASK_READY");
		intent.setPackage(HikeMessengerApp.getInstance().getPackageName());
		PackageManager packageManager = HikeMessengerApp.getInstance().getPackageManager();
		List<ResolveInfo> resolveInfos = packageManager.queryIntentServices(intent, 0);
		Class<? extends GcmTaskService> gcmTaskService = null;
		for (ResolveInfo info : resolveInfos)
		{
			if (info.serviceInfo.name.equals(name))
			{
				try
				{
					gcmTaskService = (Class<? extends GcmTaskService>) Class.forName(name);
				}
				catch (ClassNotFoundException e)
				{
					Logger.e(LOG_TAG, "Class not found exception occurred while getting class from string ", e);
				}
				catch (ClassCastException e)
				{
					Logger.e(LOG_TAG, "Class cast execption occurred while getting class from string ", e);
				}
			}
		}
		return gcmTaskService;
	}

	public static class Builder
	{
		private int numRetries;

		private long windowStartDelaySeconds;

		private long windowEndDelaySeconds;

		private Class<? extends GcmTaskService> gcmTaskService;

		private int requiredNetworkType;

		private boolean requiresCharging;

		private String tag;

		private boolean persisted;

		private boolean updateCurrent;

		private Bundle extras;

		public Builder()
		{

		}

		public Builder setNumRetries(int numRetries)
		{
			this.numRetries = numRetries;
			return this;
		}

		public Builder setExecutionWindow(long windowStartDelaySeconds, long windowEndDelaySeconds)
		{
			this.windowStartDelaySeconds = windowStartDelaySeconds;
			this.windowEndDelaySeconds = windowEndDelaySeconds;
			return this;
		}

		public Builder setService(Class<? extends GcmTaskService> gcmTaskService)
		{
			this.gcmTaskService = gcmTaskService;
			return this;
		}

		public Builder setRequiredNetwork(int requiredNetworkType)
		{
			this.requiredNetworkType = requiredNetworkType;
			return this;
		}

		public Builder setRequiresCharging(boolean requiresCharging)
		{
			this.requiresCharging = requiresCharging;
			return this;
		}

		public Builder setTag(String tag)
		{
			this.tag = tag;
			return this;
		}

		public Builder setPersisted(boolean persisted)
		{
			this.persisted = persisted;
			return this;
		}

		public Builder setUpdateCurrent(boolean updateCurrent)
		{
			this.updateCurrent = updateCurrent;
			return this;
		}

		public Builder setExtras(Bundle extras)
		{
			this.extras = extras;
			return this;
		}

		public Config build()
		{
			return new Config(this);
		}
	}
}
