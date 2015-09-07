package com.bsb.hike.productpopup;

import com.bsb.hike.bots.BotConfiguration;

/**
 * 
 * @author himanshu
 *
 *         This class defines the configuration for Popups
 */
public class PopupConfiguration extends BotConfiguration
{

	public PopupConfiguration(int config)
	{
		super(config);
	}

	private static final byte PORTRAIT_ONLY = 0;

	public boolean showInPortraitOnly()
	{
		return isBitSet(PORTRAIT_ONLY);
	}
}
