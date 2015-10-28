package com.bsb.hike.platform;

import java.io.File;

import org.cocos2dx.lib.Cocos2dxActivity;
import org.cocos2dx.lib.Cocos2dxHandler;
import org.cocos2dx.lib.Cocos2dxHelper;
import org.cocos2dx.lib.Cocos2dxVideoHelper;
import org.cocos2dx.lib.Cocos2dxWebViewHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.bots.NonMessagingBotConfiguration;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.chukong.cocosplay.client.CocosPlayClient;

/**
 * This is an Activity class which renders native games
 * 
 * @author sk
 * 
 */
public class CocosGamingActivity extends Cocos2dxActivity
{
	private static Context context;

	private String TAG = getClass().getCanonicalName();

	private boolean isPortrait;

	private static NativeBridge nativeBridge;

	private String msisdn;

	private static BotInfo botInfo;

	private static NonMessagingBotMetadata nonMessagingBotMetadata;

	private NonMessagingBotConfiguration botConfig;

	private static String platform_content_dir;

	@Override
	public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState)
	{
		super.onPostCreate(savedInstanceState, persistentState);
		Logger.d(TAG, "onPostCreate()");
	}

	public void onCreate(Bundle savedInstanceState)
	{
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		super.onCreateDuplicate(savedInstanceState);
		getSupportActionBar().hide();
		context = CocosGamingActivity.this;
		msisdn = getIntent().getStringExtra(HikeConstants.MSISDN);
		platform_content_dir = PlatformContentConstants.PLATFORM_CONTENT_DIR;
		botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);

		if (botInfo == null || botInfo.getMetadata() == null)
		{
			Toast.makeText(getApplicationContext(), R.string.some_error, Toast.LENGTH_SHORT).show();
			finish();
			Cocos2dxHelper.terminateProcess();
			Logger.e(TAG, "metadata is null");
			return;
		}
		HikeConversationsDatabase.getInstance().updateLastMessageStateAndCount(msisdn, ConvMessage.State.RECEIVED_READ.ordinal());
		botInfo.setUnreadCount(0);
		nonMessagingBotMetadata = new NonMessagingBotMetadata(botInfo.getMetadata());

		botConfig = null == botInfo.getConfigData() ? new NonMessagingBotConfiguration(botInfo.getConfiguration()) : new NonMessagingBotConfiguration(botInfo.getConfiguration(),
				botInfo.getConfigData());

		isPortrait = botConfig.isPortraitEnabled();

		if (isPortrait)
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		else
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}

		if (!TextUtils.isEmpty(getIntent().getStringExtra(HikeConstants.DATA)))
		{
			nativeBridge = new NativeBridge(msisdn, CocosGamingActivity.this, getIntent().getStringExtra(HikeConstants.DATA));
		}
		else
		{
			nativeBridge = new NativeBridge(msisdn, CocosGamingActivity.this);
		}

		loadGame();

	}

	public void loadGame()
	{
		CocosPlayClient.init(CocosGamingActivity.this, false);
		String cocosEnginePath = null;
		String cocosGamePath = null;

		if (nonMessagingBotMetadata != null)
		{
			JSONArray mapps = nonMessagingBotMetadata.getAsocmapp();
			if (mapps != null)
			{
				for (int i = 0; i < mapps.length(); i++)
				{
					JSONObject json = new JSONObject();
					try
					{
						json = mapps.getJSONObject(i);
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
					String appName = json.optString(HikeConstants.NAME);
					cocosEnginePath = platform_content_dir + appName + "/libcocos2d.so";

				}
			}
			cocosGamePath = getAppBasePath() + "libcocos2dcpp.so";
		}

		loadSoFile(cocosEnginePath, true);
		loadSoFile(cocosGamePath, false);

		CocosGamingActivity.sContext = this;
		CocosGamingActivity.this.mHandler = new Cocos2dxHandler(CocosGamingActivity.this);
		Cocos2dxHelper.initDuplicate(CocosGamingActivity.this, msisdn, getAppBasePath());
		appInit(getExternalPath());
		CocosGamingActivity.this.mGLContextAttrs = getGLContextAttrs();
		CocosGamingActivity.this.init();
		if (mVideoHelper == null)
		{
			mVideoHelper = new Cocos2dxVideoHelper(CocosGamingActivity.this, mFrameLayout);
		}

		if (mWebViewHelper == null)
		{
			mWebViewHelper = new Cocos2dxWebViewHelper(mFrameLayout);
		}

	}

	private void loadSoFile(String path, boolean isEngine)
	{
		try
		{
			System.load(path);
		}
		catch (UnsatisfiedLinkError e)
		{
			e.printStackTrace();

			if (isEngine)
			{
				Logger.e(TAG, "Game Engine load failed");
			}
			else
			{
				Logger.e(TAG, "Game load failed");
			}
			nativeBridge.logAnalytics("true", "gaming", getErrorJson(isEngine).toString());
			Toast.makeText(getApplicationContext(), R.string.some_error, Toast.LENGTH_SHORT).show();
			String parentMsisdn = nonMessagingBotMetadata.getParentMsisdn();
			if (parentMsisdn != null && parentMsisdn.length() > 0)
			{
				Intent intent = IntentFactory.getNonMessagingBotIntent(parentMsisdn, this);
				startActivity(intent);
			}
			finish();
			Cocos2dxHelper.terminateProcess();
			return;
		}
	}

	private JSONObject getErrorJson(boolean isEngine)
	{
		try
		{
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("ek", "act_game");
			jsonObject.put("ep", nonMessagingBotMetadata.getAppName());
			jsonObject.put("ec", botInfo.getMsisdn());
			if (isEngine)
			{
				jsonObject.put("eo", "engine_load_failed");
			}
			else
			{
				jsonObject.put("eo", "game_load_failed");
			}
			jsonObject.put("ef", "");
			jsonObject.put("eg", "");
			jsonObject.put("es", "");
			jsonObject.put("et", "");
			jsonObject.put("eu", "");
			jsonObject.put("ev", "");
			return jsonObject;
		}
		catch (JSONException e1)
		{
			e1.printStackTrace();
		}
		return null;
	}

	public static Object getNativeBridge()
	{
		return nativeBridge;
	}

	public static Object getBotInfo()
	{
		return botInfo;
	}

	public static native void platformCallback(String callID, String response);

	@Override
	protected void onActivityResult(int requestCode, int resultCode, final Intent data)
	{
		if (resultCode == RESULT_OK)
		{
			super.onActivityResult(requestCode, resultCode, data);
			Logger.d(TAG, "-onActivityResult");
			switch (requestCode)
			{
			case HikeConstants.PLATFORM_REQUEST:
				JSONObject response = new JSONObject();
				for (String key : data.getExtras().keySet())
				{
					Object value = data.getExtras().get(key);
					try
					{
						response.put(key, value);
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
				}

				final String res = response.toString();
				this.runOnGLThread(new Runnable()
				{
					@Override
					public void run()
					{
						platformCallback(NativeBridge.SEND_SHARED_MESSAGE, res);
					}
				});

				Logger.d(TAG, "+onActivityResult");
				break;
			}
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		HAManager.getInstance().startChatSession(msisdn);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		HAManager.getInstance().endChatSession(msisdn);
	}

	/**
	 * This method returns the basePath for the assets folder to be used by native games
	 * 
	 * @return basePath for assets
	 */
	public static String getExternalPath()
	{
		String path = platform_content_dir + nonMessagingBotMetadata.getAppName();
		return path + File.separator + "assets/";
	}

	public String getAppBasePath()
	{
		String path = platform_content_dir + nonMessagingBotMetadata.getAppName();

		return path + File.separator;
	}

}
