package com.bsb.hike.platform;

import java.io.File;

import org.cocos2dx.lib.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.bots.NonMessagingBotConfiguration;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.utils.CustomAnnotation.DoNotObfuscate;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.chukong.cocosplay.client.CocosPlayClient;

/**
 * This is an Activity class which renders native games
 * 
 * @author sk
 * 
 */
@DoNotObfuscate
public class CocosGamingActivity extends Cocos2dxActivity
{
	private static Context context;

	protected String TAG = getClass().getCanonicalName();

	private boolean isPortrait;

	private static NativeBridge nativeBridge;

	private String msisdn;

	private static BotInfo botInfo;

	private static NonMessagingBotMetadata nonMessagingBotMetadata;

	private NonMessagingBotConfiguration botConfig;

	private static String platform_content_dir;

	private long openTimestamp = 0;

	private long activeDuration = 0;
	
	private final String GAME_ANALYTICS_KINGDOM = "ek";
	private final String GAME_ANALYTICS_PHYLUM = "ep";
	private final String GAME_ANALYTICS_CLASS = "ec";
	private final String GAME_ANALYTICS_ORDER = "eo";
	private final String GAME_ANALYTICS_FAMILY = "ef";
	private final String GAME_ANALYTICS_GENUS = "eg";
	private final String GAME_ANALYTICS_SPECIES = "es";
	private final String GAME_ANALYTICS_T = "et";
	private final String GAME_ANALYTICS_U = "eu";
	private final String GAME_ANALYTICS_V = "ev";
	
	private final String GAME_ANALYTICS_KINGDOM_VALUE = "act_game";
	private final String GAME_ANALYTICS_ENGINE_FAILED = "engine_load_failed";
	private final String GAME_ANALYTICS_GAME_FAILED = "game_load_failed";
	private final String GAME_ANALYTICS_GAME_OPEN = "game_open";
	SharedPreferences settings;

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
		settings = getSharedPreferences(HikePlatformConstants.GAME_PROCESS, context.MODE_MULTI_PROCESS);
		setIsGameRunning(true);

		msisdn = getIntent().getStringExtra(HikeConstants.MSISDN);
		platform_content_dir = PlatformContentConstants.PLATFORM_CONTENT_DIR;
		if(TextUtils.isEmpty(msisdn))
		{
			finish();
			Cocos2dxHelper.terminateProcess();
			return;
		}
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

		checkAndRecordBotOpen();

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
                    cocosEnginePath = platform_content_dir + PlatformContentConstants.HIKE_MICRO_APPS + PlatformContentConstants.HIKE_MAPPS + appName + PlatformContentConstants.GAME_ENGINE_SO_FILE;
				    File cocosEngineFile = new File(cocosEnginePath);
                    if(!(cocosEngineFile.exists()))
                        cocosEnginePath = PlatformContentConstants.PLATFORM_CONTENT_OLD_DIR + appName + PlatformContentConstants.GAME_ENGINE_SO_FILE;

                }
			}
			cocosGamePath = getAppBasePath() + PlatformContentConstants.GAME_SO_FILE;
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
			jsonObject.put(GAME_ANALYTICS_KINGDOM, GAME_ANALYTICS_KINGDOM_VALUE);
			jsonObject.put(GAME_ANALYTICS_PHYLUM, nonMessagingBotMetadata.getAppName());
			jsonObject.put(GAME_ANALYTICS_CLASS, msisdn);
			if (isEngine)
			{
				jsonObject.put(GAME_ANALYTICS_ORDER, GAME_ANALYTICS_ENGINE_FAILED);
			}
			else
			{
				jsonObject.put(GAME_ANALYTICS_ORDER, GAME_ANALYTICS_GAME_FAILED);
			}
			jsonObject.put(GAME_ANALYTICS_FAMILY, "");
			jsonObject.put(GAME_ANALYTICS_GENUS, "");
			jsonObject.put(GAME_ANALYTICS_SPECIES, "");
			jsonObject.put(GAME_ANALYTICS_T, "");
			jsonObject.put(GAME_ANALYTICS_U, "");
			jsonObject.put(GAME_ANALYTICS_V, "");
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
				//nativeBridge.sendAppState(true); // AND-4907
				Logger.d(TAG, "+onActivityResult");
				break;
				case HikeConstants.PLATFORM_FILE_CHOOSE_REQUEST:
					final String id =data.getStringExtra(HikeConstants.CALLBACK_ID);
					this.runOnGLThread(new Runnable()
					{
						@Override
						public void run()
						{
							platformCallback(id,PlatformUtils.getFileUploadJson(data));
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
		Logger.d(TAG, "onResume()");
		super.onResume();
		HAManager.getInstance().startChatSession(msisdn);
		openTimestamp = System.currentTimeMillis();
		//nativeBridge.sendAppState(true);
		settings.edit().putBoolean(HikePlatformConstants.GAME_ACTIVE, true).commit();
	}

	@Override
	protected void onPause()
	{
		Logger.d(TAG, "onPause()");
		super.onPause();
		HAManager.getInstance().endChatSession(msisdn);
		activeDuration = activeDuration + (System.currentTimeMillis() - openTimestamp);
		//nativeBridge.sendAppState(false);
		setIsGameRunning(false);
	}

	@Override
	protected void onDestroy()
	{
		//nativeBridge.sendAppState(false);
		setIsGameRunning(false);
		sendGameOpenAnalytics();
		onHandlerDestroy();
		super.onDestroy();
	}

	public void sendGameOpenAnalytics()
	{
		activeDuration = activeDuration + (System.currentTimeMillis() - openTimestamp);
		Logger.d(TAG, "Active duration : " + activeDuration);
		nativeBridge.logAnalytics("true", "gaming", getGameOpenAnalyticsJson().toString());
	}

	private JSONObject getGameOpenAnalyticsJson()
	{
		try
		{
			JSONObject jsonObject = new JSONObject();
			jsonObject.put(GAME_ANALYTICS_KINGDOM, GAME_ANALYTICS_KINGDOM_VALUE);
			jsonObject.put(GAME_ANALYTICS_PHYLUM, nonMessagingBotMetadata.getAppName());
			jsonObject.put(GAME_ANALYTICS_CLASS, msisdn);
			jsonObject.put(GAME_ANALYTICS_ORDER, GAME_ANALYTICS_GAME_OPEN);
			jsonObject.put(GAME_ANALYTICS_FAMILY, String.valueOf(activeDuration));
			jsonObject.put(GAME_ANALYTICS_GENUS, "");
			jsonObject.put(GAME_ANALYTICS_SPECIES, "");
			jsonObject.put(GAME_ANALYTICS_T, "");
			jsonObject.put(GAME_ANALYTICS_U, "");
			jsonObject.put(GAME_ANALYTICS_V, "");
			return jsonObject;
		}
		catch (JSONException e1)
		{
			e1.printStackTrace();
		}
		return null;
	}

	/**
	 * This method returns the basePath for the assets folder to be used by native games
	 * 
	 * @return basePath for assets
	 */
	public static String getExternalPath()
	{
        String path = nonMessagingBotMetadata.getBotFilePath();
		return path + File.separator + "assets/";
	}

	public String getAppBasePath()
	{
        String path = nonMessagingBotMetadata.getBotFilePath();
        return path + File.separator;
	}

	/**
	 * Used to record analytics for bot opens via push notifications
	 * Sample JSON : {"ek":"bno","bot_msisdn":"+hikesnake+", "bot_source" : "bot_notif" }
	 */
	private void checkAndRecordBotOpen()
	{
		String source = (getIntent() != null && getIntent().hasExtra(AnalyticsConstants.BOT_NOTIF_TRACKER)) ? getIntent().getStringExtra(AnalyticsConstants.BOT_NOTIF_TRACKER) : "default";
		JSONObject json = new JSONObject();
		try
		{
			json.put(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.BOT_NOTIF_TRACKER);
			json.put(AnalyticsConstants.BOT_MSISDN, msisdn);
			json.put(AnalyticsConstants.BOT_OPEN_SOURCE, source);
			if(source.equals(AnalyticsConstants.BOT_OPEN_SOURCE_NOTIF))
			{
				nativeBridge.openViaNotif =true;
			}
			nativeBridge.logAnalytics("true", AnalyticsConstants.CLICK_EVENT, json.toString());
		}

		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public void setIsGameRunning(Boolean isGameRunning)
	{
		settings.edit().putBoolean(HikePlatformConstants.GAME_ACTIVE,isGameRunning).commit();
	}

}
