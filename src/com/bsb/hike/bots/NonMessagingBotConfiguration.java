package com.bsb.hike.bots;

import org.json.JSONObject;

/**
 * Created by shobhit on 22/04/15.
 */
public class NonMessagingBotConfiguration extends BotConfiguration
{
	private String metadata;

	public String getMetadata()
	{
		return metadata;
	}

	public NonMessagingBotConfiguration(int config)
	{
		super(config);
	}

	public NonMessagingBotConfiguration(int config, String metadata)
	{
		super(config);
		this.metadata = metadata;
	}
}