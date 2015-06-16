package com.bsb.hike.platform;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.http.util.ByteArrayBuffer;
import org.cocos2dx.lib.Cocos2dxActivity;
import org.cocos2dx.lib.Cocos2dxHandler;
import org.cocos2dx.lib.Cocos2dxHelper;
import org.cocos2dx.lib.Cocos2dxVideoHelper;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Toast;

public class CocosGamingActivity extends Cocos2dxActivity {
	private static Context context;
	private String TAG = getClass().getCanonicalName();
	private boolean isInit = false;
	Cocos2dxActivity cocos2dActivity;
	private String downloadPathUrl;
	private boolean isPortrait;
	private static String appName;
	private Handler mHandler = new Handler(); 

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		callOnCreate(savedInstanceState);
		context = CocosGamingActivity.this;
		sContext = CocosGamingActivity.this;
		downloadPathUrl = getIntent().getStringExtra("downloadPathUrl");
		isPortrait = getIntent().getBooleanExtra("isPortrait", true);
		String[] appTokens = downloadPathUrl.split("/");
		appName = appTokens[appTokens.length-1];
		appName = appName.replace(".zip", "");
		if (isPortrait) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				new AsyncTask<String, Void, Boolean>() {

					private ProgressDialog pdia;

					@Override
					protected void onPreExecute() {
						super.onPreExecute();
						pdia = new ProgressDialog(CocosGamingActivity.this);
						pdia.setMessage("Loading");
						pdia.setCancelable(false);
						pdia.show();
					}

					@Override
					protected Boolean doInBackground(String... params) {
						try {
							downloadFromUrl(downloadPathUrl, getFileBasePath(CocosGamingActivity.this) +appName +".zip");
							unpackZip(getFileBasePath(CocosGamingActivity.this),appName, appName +".zip");
							return true;
						} catch (Exception e) {
							e.printStackTrace();
							return false;
						}
					}

					@Override
					protected void onPostExecute(Boolean result) {
						super.onPostExecute(result);
						if (!result) {
							Toast.makeText(context, "Can't find game", Toast.LENGTH_SHORT).show();
							return;
						}
						if (pdia.isShowing()) {
							pdia.dismiss();
						}
//						CocosPlayClient.init(CocosGamingActivity.this, false);
						Log.d(TAG, "onPostExecute() 2");
						getAllFiles(new File(getFileBasePath(context)));

						Log.d(TAG, "onPostExecute() 1");
						System.load(getFileBasePath(context)+appName + "/libcocos2dcpp.so");
						CocosGamingActivity.this.mHandler = new Cocos2dxHandler(CocosGamingActivity.this);
						Log.d(TAG, "onPostExecute() 4");
						Cocos2dxHelper.init(CocosGamingActivity.this);
						Log.d(TAG, "onPostExecute() 3");
						Log.d(TAG, "onPostExecute() 5");
						CocosGamingActivity.this.mGLContextAttrs = getGLContextAttrs();
						CocosGamingActivity.this.init();
						Log.d(TAG, "onPostExecute() 6");
						if (mVideoHelper == null) {
							mVideoHelper = new Cocos2dxVideoHelper(CocosGamingActivity.this, mFrameLayout);
						}
						Log.d(TAG, "onPostExecute() 7");

						isInit = true;
					}

				}.execute("");
			}
		}, 500);
		
		
	}

	@Override
	protected void onResume() {
		if (isInit) {
			super.onResume();
		} else {
			super.onResumeDuplicate();
		}
	}

//	@Override
//	public void onBackPressed() {
//		super.onBackPressed();
//		Log.d(TAG, "onBackPressed()");
//		Cocos2dxHelper.terminateProcess();
//	}
//
//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		if (keyCode == KeyEvent.KEYCODE_BACK) {
//			super.onBackPressed();
//			Log.d(TAG, "onBackPressed()");
//			Cocos2dxHelper.terminateProcess();
//	        return true;
//	    }
//	    return super.onKeyDown(keyCode, event);
//	}

	@Override
	protected void onPause() {
		if (isInit) {
			super.onPause();
		} else {
			super.onPauseDuplicate();
		}
	}

	private void callOnCreate(Bundle savedInstanceState) {
		super.onCreateDuplicate(savedInstanceState);
	}

	public static String getExternalPath() {
		Log.d("CocosGamingActivity","getExternalPath() : " +getFileBasePath(context) + appName+"/assets/");
		return getFileBasePath(context) + appName+"/assets/";
	}

	public static String getFileBasePath(Context context) {
		File folder = context.getFilesDir();
		if (folder == null)
			return null;
		Log.d("CocosGamingActivity", "getFileBasePath() : " + folder.getAbsolutePath());
		return folder.getAbsolutePath() + "/";
	}

	private void getAllFiles(File curDir) {
		File[] filesList = curDir.listFiles();
		for (File f : filesList) {
			if (f.isDirectory())
				Log.d(TAG, "Folder : " + (f.getName()));
			if (f.isFile()) {
				Log.d(TAG, "File : " + (f.getName()));
			}
		}
	}

	public static void downloadFromUrl(String downloadUrl, String fileName) {

		try {
			URL url = new URL(downloadUrl); // you can write here any link
			File file = new File(fileName);
			long startTime = System.currentTimeMillis();
			Log.d("DownloadManager", "download begining");
			Log.d("DownloadManager", "download url:" + url);
			Log.d("DownloadManager", "downloaded file name:" + fileName);
			/* Open a connection to that URL. */
			URLConnection ucon = url.openConnection();
			/*
			 * Define InputStreams to read from the URLConnection.
			 */
			InputStream is = ucon.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);
			/*
			 * Read bytes to the Buffer until there is nothing more to read(-1).
			 */
			ByteArrayBuffer baf = new ByteArrayBuffer(5000);
			int current = 0;
			while ((current = bis.read()) != -1) {
				baf.append((byte) current);
			}
			/* Convert the Bytes read to a String. */
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(baf.toByteArray());
			fos.flush();
			fos.close();
			Log.d("DownloadManager", "download ready in" + ((System.currentTimeMillis() - startTime) / 1000) + " sec");

		} catch (IOException e) {
			Log.d("DownloadManager", "Error: " + e);
		}

	}

	private boolean unpackZip(String path,String directory, String zipname) {
		InputStream is;
		ZipInputStream zis;
		File theDir = new File(path+directory);

		// if the directory does not exist, create it
		if (!theDir.exists()) {
		    System.out.println("creating directory: " + directory);
		    boolean result = false;

		    try{
		        theDir.mkdir();
		        result = true;
		    } 
		    catch(SecurityException se){
		        //handle it
		    }        
		    if(result) {    
		        System.out.println("DIR created");  
		    }
		}
		try {
			String filename;
			is = new FileInputStream(path + zipname);
			zis = new ZipInputStream(new BufferedInputStream(is));
			ZipEntry ze;
			byte[] buffer = new byte[1024];
			int count;
			while ((ze = zis.getNextEntry()) != null) {
				filename = ze.getName();
				// Need to create directories if not exists, or
				// it will generate an Exception...
				if (ze.isDirectory()) {
					File fmd = new File(path + "/" + directory+"/" +filename);
					fmd.mkdirs();
					continue;
				}
				FileOutputStream fout = new FileOutputStream(path + "/" + directory+"/" +filename);
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
		return true;
	}
}
