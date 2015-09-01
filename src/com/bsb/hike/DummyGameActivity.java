package com.bsb.hike;

import org.cocos2dx.lib.Cocos2dxActivity;

import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.platform.GameUtils;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class DummyGameActivity extends Cocos2dxActivity
{
	public static Cocos2dxActivity gameActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dummy_game);
		gameActivity = this;
		BotInfo bot=BotUtils.getBotInfoForBotMsisdn("+hikenews+");
		GameUtils utilities=new GameUtils(bot,this);
		String data="data";
		String key="key1";
		utilities.putInCache(key, data);
		utilities.getFromCache(key);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.dummy_game, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings)
		{
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
