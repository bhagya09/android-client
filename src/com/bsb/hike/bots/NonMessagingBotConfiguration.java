package com.bsb.hike.bots;

import org.json.JSONObject;

/**
 * Created by shobhit on 22/04/15.
 */
public class NonMessagingBotConfiguration extends BotConfiguration
{
	private JSONObject configData;

	public JSONObject getConfigData()
	{
		return configData;
	}

	public NonMessagingBotConfiguration(int config)
	{
		super(config);
	}

	public NonMessagingBotConfiguration(int config, JSONObject configData)
	{
		super(config);
		this.configData = configData;
	}
}