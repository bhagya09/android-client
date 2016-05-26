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

	/**
	 * Sets the specified bit position to 1
	 *
	 * @param bitPosition
	 */
	private void setBit(int bitPosition)
	{
		config = config | (1 << bitPosition);
	}

	/**
	 * Sets the specified bit position to 1
	 *
	 * @param bitPosition
	 */
	private void resetBit(int bitPosition)
	{
		config = config & (~(1 << bitPosition));
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

	public void toggleLastSeen()
	{
		if (shouldShowLastSeen())
		{
			hideLasSeen();
		}

		else
		{
			showLastSeen();
		}
	}

	public void toggleStatusUpdate()
	{
		if (shouldShowStatusUpdate())
		{
			hideStatusUpdate();
		}

		else
		{
			showStatusUpdate();
		}
	}

	public void showStatusUpdate()
	{
		setBit(STATUS_UPDATE);
	}

	public void hideStatusUpdate()
	{
		resetBit(STATUS_UPDATE);
	}

	public void showLastSeen()
	{
		setBit(LAST_SEEN);
	}

	public void hideLasSeen()
	{
		resetBit(LAST_SEEN);
	}

	public void setLastSeen(boolean showLastSeen)
	{
		if (showLastSeen)
			showLastSeen();
		else
			hideLasSeen();
	}

	public void setStatusUpdate(boolean showStatusUpdate)
	{
		if (showStatusUpdate)
			showStatusUpdate();
		else
			hideStatusUpdate();
	}



}
