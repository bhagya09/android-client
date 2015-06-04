package com.bsb.hike.bots;

import org.json.JSONObject;

/**
 * Created by shobhit on 22/04/15.
 */
public class BotConfiguration
{
	private int config = Integer.MAX_VALUE;

	public BotConfiguration(int config)
	{
		this.config = config;
	}

	protected boolean isBitSet(int bitPosition)
	{
		return ((config>>bitPosition) & 1) ==1;
	}

	public int getConfig()
	{
		return config;
	}

	public void setConfig(int config)
	{
		this.config = config;
	}
}
