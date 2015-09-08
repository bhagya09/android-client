package com.bsb.hike.platform;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.cocos2dx.lib.Cocos2dxActivity;
import org.cocos2dx.lib.Cocos2dxHandler;
import org.cocos2dx.lib.Cocos2dxHelper;
import org.cocos2dx.lib.Cocos2dxVideoHelper;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.util.Base64;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.bsb.hike.utils.Logger;
import com.chukong.cocosplay.client.CocosPlayClient;
import com.google.gson.Gson;

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

	private boolean isInit = false;

	public static Cocos2dxActivity cocos2dActivity;

	private String downloadPathUrl;

	private boolean isPortrait;

	private String version;

	private static String appId;

	private static String appName;

	private String cocosEngineVersion;

	private Handler mHandler = new Handler();

	public static final String SHARED_PREF = "native_games_sharedpref";

	public static final String LIST_OF_APPS = "list_of_games_map";

	public static final String COCOS_ENGINE_VERSION = "cocos_engine_version";

	private SharedPreferences sharedPreferences;

	private SharedPreferences.Editor sharedPrefEditor;

	private Map<String, String> listOfAppsMap;

	private Gson gson = new Gson();

	private JSONObject gameDataJsonObject;

	private String requestId;

	private static NativeBridge nativeBridge;

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

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreateDuplicate(savedInstanceState);
		context = CocosGamingActivity.this;
		sContext = CocosGamingActivity.this;

		downloadPathUrl = getIntent().getStringExtra("downloadPathUrl");
		isPortrait = getIntent().getBooleanExtra("isPortrait", true);
		version = getIntent().getStringExtra("version");
		cocosEngineVersion = getIntent().getStringExtra("cocosEngineVersion");
		appId = getIntent().getStringExtra("appId");
		requestId = getIntent().getStringExtra("requestId");

		Logger.d(TAG, "isPortrait : " + isPortrait);
		String[] appTokens = downloadPathUrl.split("/");
		appName = appTokens[appTokens.length - 1];
		appName = appName.replace(".zip", "");

		Logger.d(TAG, "Integer size : " + Integer.SIZE);
		Logger.d(TAG, "System architecture : " + System.getProperty("os.arch"));

		sharedPreferences = getSharedPreferences(SHARED_PREF, Context.MODE_MULTI_PROCESS);
		sharedPrefEditor = getSharedPreferences(SHARED_PREF, Context.MODE_MULTI_PROCESS).edit();
		String listOfAppsString = sharedPreferences.getString(LIST_OF_APPS, null);
		if (listOfAppsString != null)
		{
			Logger.d(TAG, "listOfApps : " + listOfAppsString);
			listOfAppsMap = new HashMap<String, String>();
			listOfAppsMap = (Map<String, String>) gson.fromJson(listOfAppsString, listOfAppsMap.getClass());
			try
			{
				String jsonObjString = listOfAppsMap.get(appId);
				if (jsonObjString != null)
				{
					gameDataJsonObject = new JSONObject(jsonObjString);
					Logger.d(TAG, "Version of launch param : " + version + " :: savedVersion : " + Integer.parseInt(gameDataJsonObject.getString("version")));
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		if (isPortrait && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			Logger.d(TAG, "Downloading in portrait mode");
			new DownloadGameTask().execute("");
		}
		else if (!isPortrait && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
		{
			Logger.d(TAG, "Downloading in landscape mode");
			new DownloadGameTask().execute("");
		}

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		mHandler.postDelayed(new Runnable()
		{

			@Override
			public void run()
			{
				if (isPortrait)
				{
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				}
				else
				{
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				}
			}
		}, 250);

		cocos2dActivity = this;
	}

	public static Object getNativeBridge()
	{
		return nativeBridge;
		// return cocos2dActivity;
	}

	public static native void PlatformCallback(String callID, String response);

	@Override
	protected void onActivityResult(int requestCode, int resultCode, final Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		Logger.d(TAG, "-onActivityResult");
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		final String res = response.toString();
		this.runOnGLThread(new Runnable()
		{
			@Override
			public void run()
			{
				PlatformCallback("SEND_SHARED_MESSAGE", res);
			}
		});

		Logger.d(TAG, "+onActivityResult");
	}

	@Override
	protected void onResume()
	{
		Logger.d(TAG, "onResume()");
		if (isInit)
		{
			super.onResume();
		}
		else
		{
			super.onResumeDuplicate();
		}
	}

	@Override
	protected void onPause()
	{
		Logger.d(TAG, "onPause()");
		if (isInit)
		{
			super.onPause();
		}
		else
		{
			super.onPauseDuplicate();
		}
	}

	/**
	 * This method returns the basePath for the assets folder to be used by native games
	 * 
	 * @return basePath for assets
	 */
	public static String getExternalPath()
	{
		Logger.d("CocosGamingActivity", "getExternalPath() : " + getFileBasePath(context) + appName + "/assets/");
		return getFileBasePath(context) + appName + "/assets/";
	}

	public static String getFileBasePath(Context context)
	{
		File folder = context.getFilesDir();
		// File folder = context.getObbDir();
		if (folder == null)
			return null;
		Logger.d("CocosGamingActivity", "getFileBasePath() : " + folder.getAbsolutePath());
		return folder.getAbsolutePath() + "/";
	}

	/**
	 * AsyncTask to download the .so file and load on runtime
	 * 
	 * @author sk
	 * 
	 */
	class DownloadGameTask extends AsyncTask<String, Integer, Boolean>
	{

		private ProgressDialog pdia;

		private boolean isDownload = false;

		private boolean isEngineDownload = false;

		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();
			pdia = new ProgressDialog(CocosGamingActivity.this);
			if (!new File(getFileBasePath(context) + "cocosEngine-" + cocosEngineVersion + "/libcocos2d.so").exists())
			{
				isEngineDownload = true;
				pdia.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				pdia.setMessage("Downloading");
				pdia.setIndeterminate(false);
				pdia.setProgress(0);
				pdia.setCancelable(false);
				pdia.show();
			}
			if (Integer.parseInt(cocosEngineVersion) > sharedPreferences.getInt(COCOS_ENGINE_VERSION, 0))
			{
				isEngineDownload = true;
				pdia.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				pdia.setMessage("Downloading");
				pdia.setIndeterminate(false);
				pdia.setProgress(0);
				pdia.setCancelable(false);
				pdia.show();
			}
			if (!new File(getFileBasePath(context) + appName + "/libcocos2dcpp.so").exists())
			{
				isDownload = true;
				pdia.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				pdia.setMessage("Downloading");
				pdia.setIndeterminate(false);
				pdia.setProgress(0);
				pdia.setCancelable(false);
				pdia.show();
			}
			else
			{
				try
				{
					if (gameDataJsonObject != null && Integer.parseInt(version) > Integer.parseInt(gameDataJsonObject.getString("version")))
					{
						isDownload = true;
						pdia.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
						pdia.setMessage("Loading");
						pdia.setIndeterminate(false);
						pdia.setProgress(0);
						pdia.setCancelable(false);
						pdia.show();
					}
				}
				catch (NumberFormatException e)
				{
					e.printStackTrace();
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}
		}

		@Override
		protected void onProgressUpdate(Integer... values)
		{
			super.onProgressUpdate(values);
			pdia.setProgress(values[0]);
		}

		@Override
		protected Boolean doInBackground(String... params)
		{
			try
			{
				Logger.d(TAG, "DownloadGameTask::doInBackground()");
				// Check if app is stored locally or not
				if (isEngineDownload)
				{
					if (!downloadFromUrl("https://s3-ap-southeast-1.amazonaws.com/games-assets/MicroApps/cocosHike/cocosEngine-" + cocosEngineVersion + ".zip",
							getFileBasePath(CocosGamingActivity.this) + "libcocosengine" + ".zip"))
					{
						Logger.d(TAG, "Download failed");
						return false;
					}
					if (!unpackZip(getFileBasePath(CocosGamingActivity.this), "libcocosengine" + ".zip"))
					{
						Logger.d(TAG, "Unpacking Zip file failed");
						return false;
					}
					File zipFile = new File(getFileBasePath(context) + "libcocosengine" + ".zip");
					zipFile.delete();
					sharedPrefEditor.putInt(COCOS_ENGINE_VERSION, Integer.valueOf(cocosEngineVersion)).commit();
				}
				if (isDownload)
				{
					if (!downloadFromUrl(downloadPathUrl, getFileBasePath(CocosGamingActivity.this) + appName + ".zip"))
					{
						Logger.d(TAG, "Download failed");
						return false;
					}
					if (!unpackZip(getFileBasePath(CocosGamingActivity.this), appName + ".zip"))
					{
						Logger.d(TAG, "Unpacking Zip file failed");
						return false;
					}
					// if (!decryptSoFile(getFileBasePath(CocosGamingActivity.this) + appName + "/libcocos2dcpp.so.aes")) {
					// Logger.d(TAG, "Decrypting file failed");
					// return false;
					// }
					File zipFile = new File(getFileBasePath(context) + appName + ".zip");
					zipFile.delete();

					// File encryptedFile = new File(getFileBasePath(context) + appName + "/libcocos2dcpp.so.aes");
					// encryptedFile.delete();
				}
				return true;
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			super.onPostExecute(result);
			if (!result)
			{
				Toast.makeText(context, "Can't load game. This could be cause of bad connectivity or low storage.", Toast.LENGTH_SHORT).show();
				finish();
				return;
			}
			if (pdia != null && pdia.isShowing())
			{
				pdia.dismiss();
			}

			try
			{
				CocosPlayClient.init(CocosGamingActivity.this, false);
				Logger.d(TAG, "onPostExecute() 1");
				// loading cocos engine
				System.load(getFileBasePath(context) + "cocosEngine-" + cocosEngineVersion + "/libcocos2d.so");
				// loading game
				System.load(getFileBasePath(context) + appName + "/libcocos2dcpp.so");

				CocosGamingActivity.this.mHandler = new Cocos2dxHandler(CocosGamingActivity.this);
				Logger.d(TAG, "onPostExecute() 2");
				Cocos2dxHelper.initDuplicate(CocosGamingActivity.this, appId);
				// Cocos2dxHelper.init(CocosGamingActivity.this);
				Logger.d(TAG, "onPostExecute() 3");
				appInit(getExternalPath());
				Logger.d(TAG, "onPostExecute() 4");
				CocosGamingActivity.this.mGLContextAttrs = getGLContextAttrs();
				CocosGamingActivity.this.init();
				Logger.d(TAG, "onPostExecute() 5");
				if (mVideoHelper == null)
				{
					mVideoHelper = new Cocos2dxVideoHelper(CocosGamingActivity.this, mFrameLayout);
				}
				Logger.d(TAG, "onPostExecute() 6");
				if (listOfAppsMap == null)
				{
					listOfAppsMap = new HashMap<String, String>();
				}
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("version", version);
				jsonObject.put("isPortrait", isPortrait);
				jsonObject.put("downloadPathUrl", downloadPathUrl);
				jsonObject.put("appName", appName);
				listOfAppsMap.put(appId, jsonObject.toString());
				sharedPrefEditor.putString(LIST_OF_APPS, gson.toJson(listOfAppsMap)).commit();

				// nativeBridge = new NativeGameBridge(CocosGamingActivity.this, new BotInfo.HikeBotBuilder("+"+appName+"+").build());
				nativeBridge = new NativeBridge("+hikenews+", CocosGamingActivity.this);
				// if (requestId != null && !requestId.equals(""))
				// {
				// nativeBridge.initParams(nativeBridge.getRequestData(requestId));
				// }
			}
			catch (Exception e)
			{
				e.printStackTrace();
				Toast.makeText(context, "Can't load game", Toast.LENGTH_SHORT).show();
				finish();
				return;
			}

			isInit = true;
			onResume(); // to ensure that we call Cocos2dxActivity::onResume()
		}

		public boolean downloadFromUrl(String downloadUrl, String fileName)
		{

			try
			{
				int count;
				URL url = new URL(downloadUrl); // you can write here any link
				URLConnection conection = url.openConnection();
				File file = new File(fileName);
				long startTime = System.currentTimeMillis();
				Logger.d(TAG, "download begining");
				Logger.d(TAG, "download url:" + url);
				Logger.d(TAG, "downloaded file name:" + fileName);
				/* Open a connection to that URL. */
				conection.connect();

				// this will be useful so that you can show a tipical 0-100%
				// progress bar
				int lengthOfFile = conection.getContentLength();
				Logger.d(TAG, "downloaded file size:" + lengthOfFile + " bytes");
				Logger.d(TAG, "Available internal storage:" + getAvailableInternalStorage() + " bytes");
				// download the file
				if (lengthOfFile > getAvailableInternalStorage() && lengthOfFile != -1)
				{
					Logger.d(TAG, "No free space to download game");
					return false;
				}
				InputStream input = new BufferedInputStream(url.openStream(), 8192);

				// Output stream
				OutputStream output = new FileOutputStream(file);

				byte data[] = new byte[1024];

				long total = 0;

				while ((count = input.read(data)) != -1)
				{
					total += count;
					// publishing the progress....
					// After this onProgressUpdate will be called
					publishProgress((int) ((total * 100) / lengthOfFile));

					// writing data to file
					output.write(data, 0, count);
				}

				// flushing output
				output.flush();

				// closing streams
				output.close();
				input.close();
				Logger.d(TAG, "Time to decrypt file : " + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
				return true;
			}
			catch (IOException e)
			{
				Logger.d(TAG, "Error: " + e);
			}
			return false;
		}

		public long getAvailableInternalStorage()
		{
			return new File(context.getFilesDir().getAbsolutePath()).getFreeSpace();
		}

		public byte[] readFile(File file)
		{
			byte[] contents = null;
			int size = (int) file.length();
			contents = new byte[size];
			try
			{
				BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
				try
				{
					buf.read(contents);
					buf.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			return contents;
		}

		private boolean decryptSoFile(String path)
		{
			try
			{
				long startTime = System.currentTimeMillis();
				Cipher cipher = Cipher.getInstance("AES");
				Logger.d(TAG, "appSignature : " + getAppSignature().substring(0, 16));
				SecretKeySpec sks = new SecretKeySpec(getAppSignature().substring(0, 16).getBytes(), "AES");
				cipher.init(Cipher.DECRYPT_MODE, sks);
				byte[] encrypted = readFile(new File(path));
				Logger.d(TAG, "encrypted byte size : " + encrypted.length);
				byte[] decrypted = cipher.doFinal(encrypted);
				Logger.d(TAG, "decrypted byte size : " + decrypted.length);
				FileOutputStream fos = new FileOutputStream(getFileBasePath(CocosGamingActivity.this) + appName + "/libcocos2dcpp.so");
				fos.write(decrypted);
				fos.close();
				Logger.d(TAG, "Time to decrypt file : " + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
				return true;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			return false;
		}

		private boolean unpackZip(String path, String zipname)
		{
			InputStream is;
			ZipInputStream zis;
			long startTime = System.currentTimeMillis();
			try
			{
				String filename;
				is = new FileInputStream(path + zipname);
				zis = new ZipInputStream(new BufferedInputStream(is));
				ZipEntry ze;
				byte[] buffer = new byte[1024];
				int count;
				while ((ze = zis.getNextEntry()) != null)
				{
					filename = ze.getName();
					// Need to create directories if not exists, or it will
					// generate an Exception...
					if (ze.isDirectory())
					{
						File fmd = new File(path + "/" + filename);
						fmd.mkdirs();
						continue;
					}
					FileOutputStream fout = new FileOutputStream(path + "/" + filename);
					while ((count = zis.read(buffer)) != -1)
					{
						fout.write(buffer, 0, count);
					}
					fout.close();
					zis.closeEntry();
				}
				zis.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return false;
			}
			Logger.d(TAG, "Time to unpack zip file : " + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
			return true;
		}

	}
}
