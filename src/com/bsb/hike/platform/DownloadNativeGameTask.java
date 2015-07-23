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

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Base64;

import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.utils.Logger;
import com.google.gson.Gson;

public class DownloadNativeGameTask implements IHikeHTTPTask {
	
	private Context context;
	private String downloadPathUrl;
	private String version;
	private String appId;
	private String cocosEngineVersion;
	
	private boolean isEngineDownload;
	private boolean isGameDownload;
	private SharedPreferences sharedPreferences;
	private Editor sharedPrefEditor;
	private String appName;
	private String TAG = getClass().getCanonicalName();
	private Map<String, String> listOfAppsMap;
	private Gson gson;
	private JSONObject gameDataJsonObject;

	public DownloadNativeGameTask(Context context, String appId, String downloadPathUrl, boolean isPortrait, String version, String cocosEngineVersion) {
		super();
		this.context = context;
		this.appId = appId;
		this.downloadPathUrl = downloadPathUrl;
		this.version = version;
		this.cocosEngineVersion = cocosEngineVersion;
		gson = new Gson();
		init();
	}
	
	private void init() {
		String[] appTokens = downloadPathUrl.split("/");
		appName = appTokens[appTokens.length - 1];
		appName = appName.replace(".zip", "");

		Logger.d(TAG , "Integer size : " + Integer.SIZE);
		Logger.d(TAG, "System architecture : " + System.getProperty("os.arch"));

		sharedPreferences = context.getSharedPreferences(CocosGamingActivity.SHARED_PREF, Context.MODE_PRIVATE);
		sharedPrefEditor = context.getSharedPreferences(CocosGamingActivity.SHARED_PREF, Context.MODE_PRIVATE).edit();
		String listOfAppsString = sharedPreferences.getString(CocosGamingActivity.LIST_OF_APPS, null);
		if (listOfAppsString != null) {
			Logger.d(TAG, "listOfApps : " + listOfAppsString);
			listOfAppsMap = new HashMap<String, String>();
			listOfAppsMap = (Map<String, String>) gson.fromJson(listOfAppsString, listOfAppsMap.getClass());
			try {
				String jsonObjString = listOfAppsMap.get(appId);
				if (jsonObjString != null) {
					gameDataJsonObject = new JSONObject(jsonObjString);
					Logger.d(
							TAG,
							"Version of launch param : " + version + " :: savedVersion : "
									+ Integer.parseInt(gameDataJsonObject.getString("version")));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void execute() {
		if (!new File(CocosGamingActivity.getFileBasePath(context) + "cocosEngine-" + cocosEngineVersion + "/libcocos2d.so").exists()) {
			isEngineDownload = true;
		}
		if (Integer.parseInt(cocosEngineVersion) > sharedPreferences.getInt(CocosGamingActivity.COCOS_ENGINE_VERSION, 0)) {
			isEngineDownload = true;
		}
		if (!new File(CocosGamingActivity.getFileBasePath(context) + appName + "/libcocos2dcpp.so").exists()) {
			isGameDownload = true;
		} else {
			try {
				if (gameDataJsonObject != null && Integer.parseInt(version) > Integer.parseInt(gameDataJsonObject.getString("version"))) {
					isGameDownload = true;
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		if (isEngineDownload) {
			if (!downloadFromUrl("https://s3-ap-southeast-1.amazonaws.com/games-assets/MicroApps/cocosHike/cocosEngine-" + cocosEngineVersion
					+ ".zip", CocosGamingActivity.getFileBasePath(context) + "libcocosengine" + ".zip")) {
				Logger.d(TAG, "Download failed");
			}
			if (!unpackZip(CocosGamingActivity.getFileBasePath(context), "libcocosengine" + ".zip")) {
				Logger.d(TAG, "Unpacking Zip file failed");
			}
			File zipFile = new File(CocosGamingActivity.getFileBasePath(context) + "libcocosengine" + ".zip");
			zipFile.delete();
			sharedPrefEditor.putInt(CocosGamingActivity.COCOS_ENGINE_VERSION, Integer.valueOf(cocosEngineVersion)).commit();
		}
		if (isGameDownload) {
			if (!downloadFromUrl(downloadPathUrl, CocosGamingActivity.getFileBasePath(context) + appName + ".zip")) {
				Logger.d(TAG, "Download failed");
			}
			if (!unpackZip(CocosGamingActivity.getFileBasePath(context), appName + ".zip")) {
				Logger.d(TAG, "Unpacking Zip file failed");
			}
			if (!decryptSoFile(CocosGamingActivity.getFileBasePath(context) + appName + "/libcocos2dcpp.so.aes")) {
				Logger.d(TAG, "Decrypting file failed");
			}
			File zipFile = new File(CocosGamingActivity.getFileBasePath(context) + appName + ".zip");
			zipFile.delete();
			File encryptedFile = new File(CocosGamingActivity.getFileBasePath(context) + appName + "/libcocos2dcpp.so.aes");
			encryptedFile.delete();
		}
	}

	@Override
	public void cancel() {

	}
	
	private boolean downloadFromUrl(String downloadUrl, String fileName) {

		try {
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
			if (lengthOfFile > getAvailableInternalStorage() && lengthOfFile != -1) {
				Logger.d(TAG, "No free space to download game");
				return false;
			}
			InputStream input = new BufferedInputStream(url.openStream(), 8192);

			// Output stream
			OutputStream output = new FileOutputStream(file);

			byte data[] = new byte[1024];

			long total = 0;

			while ((count = input.read(data)) != -1) {
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
		} catch (IOException e) {
			Logger.d(TAG, "Error: " + e);
		}
		return false;
	}

	private void publishProgress(int i) {
		// write code to publish progress
	}

	private long getAvailableInternalStorage() {
		return new File(context.getFilesDir().getAbsolutePath()).getFreeSpace();
	}

	private byte[] readFile(File file) {
		byte[] contents = null;
		int size = (int) file.length();
		contents = new byte[size];
		try {
			BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
			try {
				buf.read(contents);
				buf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return contents;
	}

	private boolean decryptSoFile(String path) {
		try {
			long startTime = System.currentTimeMillis();
			Cipher cipher = Cipher.getInstance("AES");
			Logger.d(TAG, "appSignature : " + getAppSignature().substring(0, 16));
			SecretKeySpec sks = new SecretKeySpec(getAppSignature().substring(0, 16).getBytes(), "AES");
			cipher.init(Cipher.DECRYPT_MODE, sks);
			byte[] encrypted = readFile(new File(path));
			Logger.d(TAG, "encrypted byte size : " + encrypted.length);
			byte[] decrypted = cipher.doFinal(encrypted);
			Logger.d(TAG, "decrypted byte size : " + decrypted.length);
			FileOutputStream fos = new FileOutputStream(CocosGamingActivity.getFileBasePath(context) + appName + "/libcocos2dcpp.so");
			fos.write(decrypted);
			fos.close();
			Logger.d(TAG, "Time to decrypt file : " + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean unpackZip(String path, String zipname) {
		InputStream is;
		ZipInputStream zis;
		long startTime = System.currentTimeMillis();
		try {
			String filename;
			is = new FileInputStream(path + zipname);
			zis = new ZipInputStream(new BufferedInputStream(is));
			ZipEntry ze;
			byte[] buffer = new byte[1024];
			int count;
			while ((ze = zis.getNextEntry()) != null) {
				filename = ze.getName();
				// Need to create directories if not exists, or it will
				// generate an Exception...
				if (ze.isDirectory()) {
					File fmd = new File(path + "/" + filename);
					fmd.mkdirs();
					continue;
				}
				FileOutputStream fout = new FileOutputStream(path + "/" + filename);
				while ((count = zis.read(buffer)) != -1) {
					fout.write(buffer, 0, count);
				}
				fout.close();
				zis.closeEntry();
			}
			zis.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		Logger.d(TAG, "Time to unpack zip file : " + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
		return true;
	}
	
	private String getAppSignature() {
		try {
			Logger.d(TAG, "Getting keyHash");
			PackageInfo info = context.getPackageManager().getPackageInfo("com.bsb.hike", PackageManager.GET_SIGNATURES);
			for (Signature signature : info.signatures) {
				MessageDigest md = MessageDigest.getInstance("SHA");
				md.update(signature.toByteArray());
				// Logger.d(TAG, "KeyHash : " +
				// Base64.encodeToString(md.digest(), Base64.DEFAULT));
				return Base64.encodeToString(md.digest(), Base64.DEFAULT);
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

}
