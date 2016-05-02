package com.bsb.hike.modules.gcmnetworkmanager;

import android.os.Bundle;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.google.android.gms.gcm.GcmTaskService;

/**
 * Created by anubhavgupta on 28/04/16.
 */
public class Config
{
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
