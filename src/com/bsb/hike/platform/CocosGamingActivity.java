package com.bsb.hike.platform;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.cocos2dx.lib.Cocos2dxActivity;
import org.cocos2dx.lib.Cocos2dxHandler;
import org.cocos2dx.lib.Cocos2dxHelper;
import org.cocos2dx.lib.Cocos2dxVideoHelper;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.bots.NonMessagingBotConfiguration;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.models.MessageEvent;
import com.bsb.hike.platform.bridge.JavascriptBridge;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.utils.Logger;
import com.chukong.cocosplay.client.CocosPlayClient;

/**
 * This is an Activity class which renders native games
 * 
 * @author sk
 * 
 */
public class CocosGamingActivity extends Cocos2dxActivity implements HikePubSub.Listener
{
	private static Context context;

	private String TAG = getClass().getCanonicalName();

	private boolean isPortrait;

	private Handler mHandler = new Handler();

	private static NativeBridge nativeBridge;

	private String[] pubsub = new String[] { HikePubSub.MESSAGE_EVENT_RECEIVED };

	private String msisdn;

	private static BotInfo botInfo;

	private static NonMessagingBotMetadata nonMessagingBotMetadata;

	private NonMessagingBotConfiguration botConfig;

	private static String PLATFORM_CONTENT_DIR;

	@Override
	public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState)
	{
		super.onPostCreate(savedInstanceState, persistentState);
		Logger.d(TAG, "onPostCreate()");
	}

	private String getAppSignature()
	{
		try
		{
			Logger.d(TAG, "Getting keyHash");
			PackageInfo info = getPackageManager().getPackageInfo("com.bsb.hike", PackageManager.GET_SIGNATURES);
			for (Signature signature : info.signatures)
			{
				MessageDigest md = MessageDigest.getInstance("SHA");
				md.update(signature.toByteArray());
				// Logger.d(TAG, "KeyHash : " +
				// Base64.encodeToString(md.digest(), Base64.DEFAULT));
				return Base64.encodeToString(md.digest(), Base64.DEFAULT);
			}
		}
		catch (NameNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreateDuplicate(savedInstanceState);
		context = CocosGamingActivity.this;
		msisdn = getIntent().getStringExtra(HikeConstants.MSISDN);
		PLATFORM_CONTENT_DIR=getIntent().getStringExtra(PlatformContentConstants.PLATFORM_CONTENT_DIR);
		botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);

		if (botInfo == null || botInfo.getMetadata() == null)
		{
			Toast.makeText(getApplicationContext(), R.string.some_error, Toast.LENGTH_SHORT).show();
			finish();
			Cocos2dxHelper.terminateProcess();
			Logger.e(TAG, "metadata is null");
			return;
		}
		nonMessagingBotMetadata = new NonMessagingBotMetadata(botInfo.getMetadata());

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

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

		if (getIntent().getStringExtra(HikeConstants.DATA) != null && getIntent().getStringExtra(HikeConstants.DATA).length() > 0)
		{
			nativeBridge = new NativeBridge(msisdn, CocosGamingActivity.this, getIntent().getStringExtra(HikeConstants.DATA));
		}
		else
		{
			nativeBridge = new NativeBridge(msisdn, CocosGamingActivity.this);
		}

		loadGame();

		HikeMessengerApp.getPubSub().addListeners(this, pubsub);
	}

	public void loadGame()
	{
		CocosPlayClient.init(CocosGamingActivity.this, false);
		// TODO do not hard code the path of the game engine. Please change this
		try
		{
			System.load(PLATFORM_CONTENT_DIR + "cocosEngine-7/libcocos2d.so");
			System.load(getAppBasePath() + "libcocos2dcpp.so"); // loading the game
		}catch(UnsatisfiedLinkError e)
		{
			Logger.e(TAG, "Game Engine not Found");
			Toast.makeText(getApplicationContext(), R.string.some_error, Toast.LENGTH_SHORT).show();
			finish();
			Cocos2dxHelper.terminateProcess();
		}

		CocosGamingActivity.this.mHandler = new Cocos2dxHandler(CocosGamingActivity.this);
		Cocos2dxHelper.initDuplicate(CocosGamingActivity.this, msisdn, getAppBasePath());
		appInit(getExternalPath());
		CocosGamingActivity.this.mGLContextAttrs = getGLContextAttrs();
		CocosGamingActivity.this.init();
		if (mVideoHelper == null)
		{
			mVideoHelper = new Cocos2dxVideoHelper(CocosGamingActivity.this, mFrameLayout);
		}

	}

	public static Object getNativeBridge()
	{
		return nativeBridge;
	}

	public static Object getBotInfo()
	{
		return botInfo;
	}

	public static native void PlatformCallback(String callID, String response);

	@Override
	protected void onActivityResult(int requestCode, int resultCode, final Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		Logger.d(TAG, "-onActivityResult");
		if (requestCode != JavascriptBridge.PICK_CONTACT_AND_SEND_REQUEST)
		{
			return;
		}
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
				PlatformCallback(NativeBridge.SEND_SHARED_MESSAGE, res);
			}
		});

		Logger.d(TAG, "+onActivityResult");
	}

	@Override
	protected void onResume()
	{
		super.onResume();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
	}

	/**
	 * This method returns the basePath for the assets folder to be used by native games
	 * 
	 * @return basePath for assets
	 */
	public static String getExternalPath()
	{
		String path = PLATFORM_CONTENT_DIR + nonMessagingBotMetadata.getAppName();
		return path + File.separator + "assets/";
	}

	public String getAppBasePath()
	{
		String path = PLATFORM_CONTENT_DIR + nonMessagingBotMetadata.getAppName();
		
		return path + File.separator;
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (type.equals(HikePubSub.MESSAGE_EVENT_RECEIVED))
		{

			if (object instanceof MessageEvent)
			{
				MessageEvent messageEvent = (MessageEvent) object;
				String parent_msisdn = messageEvent.getParent_msisdn();
				if (!TextUtils.isEmpty(parent_msisdn) && messageEvent.getParent_msisdn().equals(msisdn))
				{
					try
					{
						JSONObject jsonObject = PlatformUtils.getPlatformContactInfo(msisdn);
						jsonObject.put(HikePlatformConstants.EVENT_DATA, messageEvent.getEventMetadata());
						jsonObject.put(HikePlatformConstants.EVENT_ID, messageEvent.getEventId());
						jsonObject.put(HikePlatformConstants.EVENT_STATUS, messageEvent.getEventStatus());
						jsonObject.put(HikePlatformConstants.EVENT_TYPE, messageEvent.getEventType());

						PlatformCallback(NativeBridge.ON_EVENT_RECEIVE, jsonObject.toString());
					}
					catch (JSONException e)
					{
						Logger.e(TAG, "JSON Exception in message event received");
					}
				}
			}
		}

	}
}
