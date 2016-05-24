package com.bsb.hike.models;

/**
 * Created by piyush on 24/05/16.
 */
public class PrivacyPreferences
{
	/**
	 * Bit positions for configData. These positions start from the least significant bit on the right hand side
	 */
	public static final byte LAST_SEEN = 0;

	public static final byte STATUS_UPDATE = 1;

	/**
	 * Bit positions end here.
	 */

	public static final int DEFAULT_VALUE = 0;

	int config = DEFAULT_VALUE;

	public PrivacyPreferences(int config)
	{
		this.config = config;
	}

	private boolean isBitSet(int bitPosition)
	{
		return ((config << bitPosition) & 1) == 1;
	}

	public int getConfig()
	{
		return config;
	}

	public void setConfig(int config)
	{
		this.config = config;
	}

	public boolean shouldShowLastSeen()
	{
		return isBitSet(LAST_SEEN);
	}

	public boolean shouldShowStatusUpdate()
	{
		return isBitSet(STATUS_UPDATE);
	}

}
