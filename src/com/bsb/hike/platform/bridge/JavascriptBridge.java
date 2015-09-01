package com.bsb.hike.platform.bridge;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.platform.CocosGamingActivity;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.ui.ComposeChatActivity;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.google.gson.Gson;

/**
 * API bridge that connects the javascript to the Native environment. Make the instance of this class and add it as the JavaScript interface of the Card WebView. This class caters
 * Platform Bridge versions from:
 * 
 * Platform Bridge Version Start = 0 Platform Bridge Version End = ~
 */
public abstract class JavascriptBridge
{
	protected CustomWebView mWebView;

	protected WeakReference<Activity> weakActivity;;

	public static final String tag = "JavascriptBridge";

	protected Handler mHandler;

	private static final String REQUEST_CODE = "request_code";

	private static final int PICK_CONTACT_REQUEST = 1;

	public JavascriptBridge(Activity activity, CustomWebView mWebView)
	{
		this.mWebView = mWebView;
		weakActivity = new WeakReference<Activity>(activity);
		this.mHandler = new Handler(HikeMessengerApp.getInstance().getMainLooper())
		{
			public void handleMessage(Message msg)
			{
				handleUiMessage(msg);
			};
		};
	}

	/**
	 * Platform Bridge Version 0 Call this function to log analytics events.
	 * 
	 * @param isUI
	 *            : whether the event is a UI event or not. This is a string. Send "true" or "false".
	 * @param subType
	 *            : the subtype of the event to be logged, eg. send "click", to determine whether it is a click event.
	 * @param json
	 *            : any extra info for logging events, including the event key that is pretty crucial for analytics.
	 */
	@JavascriptInterface
	protected abstract void logAnalytics(String isUI, String subType, String json);

	/**
	 * Platform Bridge Version 0 This function is called whenever the onLoadFinished of the html is called. This function calling is MUST. This function is also used for analytics
	 * purpose.
	 * 
	 * @param height
	 *            : The height of the loaded content
	 */
	@JavascriptInterface
	public void onLoadFinished(String height)
	{
	}

	protected void handleUiMessage(Message msg)
	{

	}

	protected void sendMessageToUiThread(int what, Object data)
	{
		sendMessageToUiThread(what, 0, 0, data);
	}

	protected void sendMessageToUiThread(int what, int arg1, Object data)
	{
		sendMessageToUiThread(what, arg1, 0, data);
	}

	protected void sendMessageToUiThread(int what, int arg1, int arg2, Object data)
	{
		Message msg = Message.obtain();
		msg.what = what;
		msg.arg1 = arg1;
		msg.arg2 = arg2;
		msg.obj = data;
		mHandler.sendMessage(msg);
	}

	/**
	 * Platform Bridge Version 0 call this function to Show toast for the string that is sent by the javascript.
	 * 
	 * @param toast
	 *            : the string to show in toast.
	 */
	@JavascriptInterface
	public void showToast(String toast)
	{
		if (weakActivity.get() != null)
		{
			Toast.makeText(weakActivity.get(), toast, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Platform Bridge Version 0 Call this function to vibrate the device.
	 * 
	 * @param msecs
	 *            : the number of milliseconds the device will vibrate.
	 */
	@JavascriptInterface
	public void vibrate(String msecs)
	{
		Utils.vibrate(Integer.parseInt(msecs));
	}

	/**
	 * Platform Bridge Version 0 call this function with parameter as true to enable the debugging for javascript. The debuggable for javascript will get enabled only after KITKAT
	 * version.
	 * 
	 * @param setEnabled
	 */
	@JavascriptInterface
	public void setDebuggableEnabled(final String setEnabled)
	{
		Logger.d(tag, "set debuggable enabled called with " + setEnabled);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
		{
			if (null == mHandler)
			{
				return;
			}
			mHandler.post(new Runnable()
			{
				@SuppressLint("NewApi")
				@Override
				public void run()
				{
					if (Boolean.valueOf(setEnabled))
					{

						WebView.setWebContentsDebuggingEnabled(true);
					}
					else
					{
						WebView.setWebContentsDebuggingEnabled(false);
					}
				}
			});

		}
	}

	/**
	 * Platform Bridge Version 0 calling this function will generate logs for testing at the android IDE. The first param will be tag used for logging and the second param is data
	 * that is used for logging. this will create verbose logs for testing purposes.
	 * 
	 * @param tag
	 * @param data
	 */
	@JavascriptInterface
	public void logFromJS(String tag, String data)
	{
		Logger.v(tag, data);
	}

	/**
	 * Platform bridge Version 0 Call this function to open a full page webView within hike.
	 * 
	 * @param title
	 *            : the title on the action bar.
	 * @param url
	 *            : the url that will be loaded.
	 */
	@JavascriptInterface
	public void openFullPage(final String title, final String url)
	{
		Logger.i(tag, "open full page called with title " + title + " , and url = " + url);

		if (null == mHandler)
		{
			return;
		}
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (weakActivity.get() != null)
				{
					Intent intent = IntentFactory.getWebViewActivityIntent(weakActivity.get(), url, title);
					weakActivity.get().startActivity(intent);
				}
			}
		});
	}

	/**
	 * Platform Bridge Version 1 call this function to open a web page in the default browser.
	 * 
	 * @param url
	 *            : : the url that will be loaded.
	 */
	@JavascriptInterface
	public void openPageInBrowser(final String url)
	{
		Logger.i(tag, "openPageInBrowser called with url = " + url);

		if (null == mHandler)
		{
			return;
		}
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (weakActivity.get() != null)
				{
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					weakActivity.get().startActivity(browserIntent);
				}
			}
		});

	}

	/**
	 * Platform Bridge Version 0 calling this function will share the screenshot of the webView along with the text at the top and a caption text to all social network platforms by
	 * calling the system's intent.
	 * 
	 * @param text
	 *            : heading of the image with the webView's screenshot.
	 * @param caption
	 *            : intent caption
	 */
	@JavascriptInterface
	public void share(String text, String caption)
	{
		FileOutputStream fos = null;
		File cardShareImageFile = null;
		Activity mContext = weakActivity.get();
		if (mContext != null)
		{
			try
			{
				if (TextUtils.isEmpty(text))
				{
					text = mContext.getString(R.string.cardShareHeading); // fallback
				}

				cardShareImageFile = new File(mContext.getExternalCacheDir(), System.currentTimeMillis() + ".jpg");
				fos = new FileOutputStream(cardShareImageFile);
				View share = LayoutInflater.from(mContext).inflate(com.bsb.hike.R.layout.web_card_share, null);
				// set card image
				ImageView image = (ImageView) share.findViewById(com.bsb.hike.R.id.image);
				Bitmap b = Utils.viewToBitmap(mWebView);
				image.setImageBitmap(b);

				// set heading here
				TextView heading = (TextView) share.findViewById(R.id.heading);
				heading.setText(text);

				// set description text
				TextView tv = (TextView) share.findViewById(com.bsb.hike.R.id.description);
				tv.setText(Html.fromHtml(mContext.getString(com.bsb.hike.R.string.cardShareDescription)));

				Bitmap shB = Utils.undrawnViewToBitmap(share);
				Logger.i(tag, " width height of layout to share " + share.getWidth() + " , " + share.getHeight());
				shB.compress(Bitmap.CompressFormat.JPEG, 100, fos);
				fos.flush();
				Logger.i(tag, "share webview card " + cardShareImageFile.getAbsolutePath());
				IntentFactory.startShareImageIntent("image/jpeg", "file://" + cardShareImageFile.getAbsolutePath(),
						TextUtils.isEmpty(caption) ? mContext.getString(com.bsb.hike.R.string.cardShareCaption) : caption);
			}

			catch (Exception e)
			{
				e.printStackTrace();
				showToast(mContext.getString(com.bsb.hike.R.string.error_card_sharing));
			}
			finally
			{
				if (fos != null)
				{
					try
					{
						fos.close();
					}
					catch (IOException e)
					{
						// Do nothing
						e.printStackTrace();
					}
				}
			}
			if (cardShareImageFile != null && cardShareImageFile.exists())
			{
				cardShareImageFile.deleteOnExit();
			}
		}
	}

	/**
	 * Platform Bridge Version 0 Whenever the content's height is changed, the html will call this function to resize the height of the Android Webview. Calling this function is
	 * MUST, whenever the height of the content changes.
	 * 
	 * @param height
	 *            : the new height when the content is reloaded.
	 */
	@JavascriptInterface
	public void onResize(String height)
	{
		Logger.i(tag, "onresize called with height=" + (Integer.parseInt(height) * Utils.densityMultiplier));
		resizeWebview(height);
	}

	protected void resizeWebview(String heightS)
	{
		if (!TextUtils.isEmpty(heightS))
		{
			heightRunnable.mWebView = new WeakReference<WebView>(mWebView);
			heightRunnable.height = Integer.parseInt(heightS);
			if (null != mHandler)
			{
				mHandler.removeCallbacks(heightRunnable);
				mHandler.post(heightRunnable);
			}
		}
	}

	HeightRunnable heightRunnable = new HeightRunnable();

	static class HeightRunnable implements Runnable
	{
		WeakReference<WebView> mWebView;

		int height;

		@Override
		public void run()
		{
			if (height != 0)
			{
				height = (int) (Utils.densityMultiplier * height); // javascript
																	// returns
																	// us in dp
				WebView webView = mWebView.get();
				if (webView != null)
				{
					Logger.i(tag, "HeightRunnable called with height=" + height + " and current height is " + webView.getHeight());

					int initHeight = webView.getMeasuredHeight();

					Logger.i("HeightAnim", "InitHeight = " + initHeight + " TargetHeight = " + height);

					if (initHeight == height)
					{
						return;
					}
					else if (initHeight > height)
					{
						collapse(webView, height);
					}
					else if (initHeight < height)
					{
						expand(webView, height);
					}
				}

			}
		}
	}

	;

	public static void expand(final View v, final int targetHeight)
	{
		final int initHeight = v.getMeasuredHeight();

		final int animationHeight = targetHeight - initHeight;

		Animation a = new Animation()
		{
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t)
			{
				v.getLayoutParams().height = initHeight + (int) (animationHeight * interpolatedTime);
				v.requestLayout();
			}

			@Override
			public boolean willChangeBounds()
			{
				return true;
			}
		};

		a.setDuration(300);
		v.startAnimation(a);
	}

	public static void collapse(final View v, final int targetHeight)
	{
		final int initialHeight = v.getMeasuredHeight();

		final int animationHeight = initialHeight - targetHeight;

		Animation a = new Animation()
		{
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t)
			{
				v.getLayoutParams().height = initialHeight - (int) (animationHeight * interpolatedTime);
				v.requestLayout();
			}

			@Override
			public boolean willChangeBounds()
			{
				return true;
			}
		};

		a.setDuration(300);
		v.startAnimation(a);
	}

	public void onDestroy()
	{
		mWebView.removeCallbacks(heightRunnable);
		mWebView.onActivityDestroyed();
	}

	@JavascriptInterface
	public void openActivity(final String data)
	{

		if (mHandler == null || weakActivity == null || weakActivity.get() == null)
		{
			return;
		}

		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				PlatformUtils.openActivity(weakActivity.get(), data);
			}
		});
	}

	/**
	 * Platform Bridge Version 0 This function can be used to start a hike native contact chooser/picker which will show all hike contacts to user and user can select few contacts
	 * (minimum 1). It will call JavaScript function "onContactChooserResult(int resultCode,JsonArray array)" This JSOnArray contains list of JSONObject where each JSONObject
	 * reflects one user. As of now each JSON will have name and platform_id, e.g : [{'name':'Paul','platform_id':'dvgd78as'}] resultCode will be 0 for fail and 1 for success NOTE
	 * : JSONArray could be null as well, a micro app has to take care of this instance and startContactChooser not present
	 */
	@JavascriptInterface
	public void startContactChooser()
	{
		Activity activity = weakActivity.get();
		if (activity != null)
		{
			Intent intent = IntentFactory.getComposeChatIntent(activity);
			intent.putExtra(HikeConstants.Extras.COMPOSE_MODE, ComposeChatActivity.PICK_CONTACT_MODE);
			intent.putExtra(tag, JavascriptBridge.this.hashCode());
			intent.putExtra(REQUEST_CODE, PICK_CONTACT_REQUEST);
			activity.startActivityForResult(intent, HikeConstants.PLATFORM_REQUEST);
		}
	}

	public void onActivityResult(int resultCode, Intent data)
	{
		int requestCode = data.getIntExtra(REQUEST_CODE, -1);
		if (requestCode != -1)
		{
			switch (requestCode)
			{
			case PICK_CONTACT_REQUEST:
				handlePickContactResult(resultCode, data);
				break;
			}
		}
	}

	private void handlePickContactResult(int resultCode, Intent data)
	{
		Logger.i(tag, "pick contact result " + data.getExtras().toString());
		if (resultCode == Activity.RESULT_OK)
		{
			mWebView.loadUrl("javascript:onContactChooserResult('1','" + data.getStringExtra(HikeConstants.HIKE_CONTACT_PICKER_RESULT) + "')");
		}
		else
		{
			mWebView.loadUrl("javascript:onContactChooserResult('0','[]')");
		}
	}

	protected void startComPoseChatActivity(final ConvMessage message)
	{
		if (null == mHandler)
		{
			return;
		}

		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				Activity mContext = weakActivity.get();
				if (mContext != null)
				{
					final Intent intent = IntentFactory.getForwardIntentForConvMessage(mContext, message, PlatformContent.getForwardCardData(message.webMetadata.JSONtoString()));
					mContext.startActivity(intent);
				}
			}
		});
	}

	/**
	 * 
	 * this function will call the js back when the javascript demands some value back from the native.
	 * 
	 * @param id
	 *            : the id of the function that native will call to call the js .
	 * @param value
	 *            : value that will be given back. it is encoded with URL Encoded Scheme. Decode it before using.
	 */
	public void callbackToJS(final String id, final String value)
	{
		if (TextUtils.isEmpty(id))
		{
			Logger.e(tag, "Empty function name when calling the JS back");
			return;
		}
		if (mHandler == null || !mWebView.isWebViewShowing())
		{
			return;
		}
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mWebView.loadUrl("javascript:callbackFromNative" + "('" + id + "','" + getEncodedDataForJS(value) + "')");
			}
		});
	}

	/**
	 * Platform Bridge Version 1 This method will be called when you need to get the Connection type on the device. The result returned will be one of the following ordinal values
	 * :
	 * 
	 * <li>-1 in case of no network</li> <li>0 in case of unknown network</li> <li>1 in case of wifi</li> <li>2 in case of 2g</li> <li>3 in case of 3g</li> <li>4 in case of 4g</li>
	 * 
	 * @param id
	 */
	@JavascriptInterface
	public void checkConnection(String id)
	{
		callbackToJS(id, Integer.toString(Utils.getNetworkType(HikeMessengerApp.getInstance().getApplicationContext())));
	}

	/**
	 * Platform Bridge Version 3 call this function to call the non-messaging bot
	 * 
	 * @param id
	 *            : : the id of the function that native will call to call the js .
	 * @param msisdn
	 *            : the msisdn of the non-messaging bot to be opened. returns Success if success and failure if failure.
	 */
	@JavascriptInterface
	public void openNonMessagingBot(String id, String msisdn)
	{

		if (BotUtils.isBot(msisdn))
		{
			BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
			if (botInfo.isNonMessagingBot())
			{
				Intent intent = null;
				if (weakActivity.get() != null)
				{
					intent = IntentFactory.getNonMessagingBotIntent(msisdn, weakActivity.get());
				}
				if (null != intent)
				{
					weakActivity.get().startActivity(intent);
					callbackToJS(id, "Success");
				}
				else
				{
					callbackToJS(id, "Failure");
				}
			}
			else
			{
				callbackToJS(id, "Failure");
			}
		}
		else
		{
			callbackToJS(id, "Failure");
		}

	}

	public void getInitJson(JSONObject jsonObj, String msisdn)
	{
		try
		{
			jsonObj.put(HikeConstants.MSISDN, msisdn);
			jsonObj.put(HikePlatformConstants.PLATFORM_USER_ID, HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_UID_SETTING, null));
			jsonObj.put(HikePlatformConstants.PLATFORM_TOKEN, HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_TOKEN_SETTING, null));
			jsonObj.put(HikePlatformConstants.APP_VERSION, AccountUtils.getAppVersion());
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected String getEncodedDataForJS(String data)
	{
		try
		{
			return URLEncoder.encode(data, "utf-8").replaceAll("\\+", "%20");
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		return "";

	}

	/**
	 * Platform Bridge Version 1 call this function for any post call.
	 * 
	 * @param functionId
	 *            : function id to call back to the js.
	 * @param data
	 *            : the stringified data that contains: "url": the url that will be called. "params": the push params to be included in the body. Response to the js will be sent as
	 *            follows: callbackFromNative(functionId, responseJson) responseJson will be like this: Success: "{ "status": "success", "status_code" : status_code , "
	 *            response": response}" Failure: "{ "status": "failure", "error_message" : error message}"
	 * 
	 */
	@JavascriptInterface
	public void doPostRequest(final String functionId, String data)
	{
		try
		{
			JSONObject jsonObject = new JSONObject(data);
			String url = jsonObject.optString(HikePlatformConstants.URL);
			String params = jsonObject.optString(HikePlatformConstants.PARAMS);
			RequestToken token = HttpRequests.microAppPostRequest(url, new JSONObject(params), new PlatformMicroAppRequestListener(functionId));
			if (!token.isRequestRunning())
			{
				token.execute();
			}
		}
		catch (JSONException e)
		{
			Logger.e(tag, "error in JSON");
			e.printStackTrace();
		}
	}

	/**
	 * Platform Bridge Version 3 call this function for any post call.
	 * 
	 * @param functionId
	 *            : function id to call back to the js.
	 * @param url
	 *            : "url": the url that will be called. Response to the js will be sent as follows: callbackFromNative(functionId, responseJson) responseJson will be like this:
	 *            Success: "{ "status ": "success", "status_code" : status_code , "response ": response}" Failure: "{ "status": "failure", "error_message" : error message}"
	 * 
	 */
	@JavascriptInterface
	public void doGetRequest(final String functionId, String url)
	{
		RequestToken token = HttpRequests.microAppGetRequest(url, new PlatformMicroAppRequestListener(functionId));
		if (!token.isRequestRunning())
		{
			token.execute();
		}

	}

	private class PlatformMicroAppRequestListener implements IRequestListener
	{
		String functionId;

		PlatformMicroAppRequestListener(String functionId)
		{
			this.functionId = functionId;
		}

		@Override
		public int hashCode()
		{
			return super.hashCode();
		}

		@Override
		public void onRequestFailure(HttpException httpException)
		{
			Logger.e("JavascriptBridge", "microApp request failed with exception " + httpException.getMessage());
			JSONObject failure = new JSONObject();
			try
			{
				failure.put(HikePlatformConstants.STATUS, HikePlatformConstants.FAILURE);
				failure.put(HikePlatformConstants.ERROR_MESSAGE, httpException.getMessage());
			}
			catch (JSONException e)
			{
				Logger.e("JavascriptBridge", "Error while parsing failure request");
				e.printStackTrace();
			}
			callbackToJS(functionId, String.valueOf(failure));
		}

		@Override
		public void onRequestSuccess(Response result)
		{
			Logger.d("JavascriptBridge", "microapp request success with code " + result.getStatusCode());
			JSONObject success = new JSONObject();
			try
			{
				success.put(HikePlatformConstants.STATUS, HikePlatformConstants.SUCCESS);
				success.put(HikePlatformConstants.STATUS_CODE, result.getStatusCode());
				success.put(HikePlatformConstants.RESPONSE, result.getBody().getContent());
			}
			catch (JSONException e)
			{
				Logger.e("JavascriptBridge", "Error while parsing success request");
				e.printStackTrace();
			}
			callbackToJS(functionId, String.valueOf(success));
		}

		@Override
		public void onRequestProgressUpdate(float progress)
		{

		}
	}

	@JavascriptInterface
	public void launchGameRequest(String requestId, String gameurl, boolean isPortrait, String version, String appId, String cocosEngineVersion)
	{
		Intent gameIntent = new Intent(weakActivity.get(), CocosGamingActivity.class);
		gameIntent.putExtra("downloadPathUrl", gameurl);
		gameIntent.putExtra("isPortrait", isPortrait);
		gameIntent.putExtra("version", version);
		gameIntent.putExtra("appId", appId);
		gameIntent.putExtra("cocosEngineVersion", cocosEngineVersion);
		gameIntent.putExtra("requestId", requestId);
		weakActivity.get().startActivity(gameIntent);
	}

	@JavascriptInterface
	public void launchGameActivity(String gameurl, boolean isPortrait, String version, String appId, String cocosEngineVersion)
	{
		Intent gameIntent = new Intent(weakActivity.get(), CocosGamingActivity.class);
		gameIntent.putExtra("downloadPathUrl", gameurl);
		gameIntent.putExtra("isPortrait", isPortrait);
		gameIntent.putExtra("version", version);
		gameIntent.putExtra("appId", appId);
		gameIntent.putExtra("cocosEngineVersion", cocosEngineVersion);
		weakActivity.get().startActivity(gameIntent);
	}

	@JavascriptInterface
	public String getSystemArch()
	{
		return System.getProperty("os.arch");
	}

	@JavascriptInterface
	public String getListOfDownloadedNativeGames()
	{
		Gson gson = new Gson();
		SharedPreferences sharedPref = weakActivity.get().getSharedPreferences(CocosGamingActivity.SHARED_PREF, Context.MODE_PRIVATE);
		String listOfAppsString = sharedPref.getString(CocosGamingActivity.LIST_OF_APPS, null);
		if (listOfAppsString != null)
		{
			Map<String, String> listOfAppsMap = new HashMap<String, String>();
			listOfAppsMap = (Map<String, String>) gson.fromJson(listOfAppsString, listOfAppsMap.getClass());
			Set<String> appIds = listOfAppsMap.keySet();
			return gson.toJson(appIds);
		}
		return "[]";
	}

	@JavascriptInterface
	public boolean deleteNativeGame(String appId)
	{
		try
		{
			String gameName = null;
			Gson gson = new Gson();
			SharedPreferences sharedPref = weakActivity.get().getSharedPreferences(CocosGamingActivity.SHARED_PREF, Context.MODE_PRIVATE);
			SharedPreferences.Editor sharedPrefEditor = weakActivity.get().getSharedPreferences(CocosGamingActivity.SHARED_PREF, Context.MODE_PRIVATE).edit();
			String listOfAppsString = sharedPref.getString(CocosGamingActivity.LIST_OF_APPS, null);
			if (listOfAppsString != null)
			{
				Map<String, String> listOfAppsMap = new HashMap<String, String>();
				listOfAppsMap = (Map<String, String>) gson.fromJson(listOfAppsString, listOfAppsMap.getClass());
				JSONObject gameObject = new JSONObject(listOfAppsMap.get(appId));
				gameName = gameObject.getString("appName");
			}
			if (gameName == null)
			{
				return false;
			}
			File file = new File(CocosGamingActivity.getFileBasePath(weakActivity.get()) + gameName);
			if (file.exists())
			{
				Logger.d("CocosGamingActivity", "Deleting file : " + file.getAbsolutePath());
				deleteRecursive(file);
			}
			else
			{
				return false;
			}

			Map<String, String> listOfAppsMap = new HashMap<String, String>();
			listOfAppsMap = (Map<String, String>) new Gson().fromJson(listOfAppsString, listOfAppsMap.getClass());
			if (listOfAppsMap != null)
			{
				try
				{
					listOfAppsMap.remove(appId);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			sharedPrefEditor.putString(CocosGamingActivity.LIST_OF_APPS, gson.toJson(listOfAppsMap)).commit();
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}

	private void deleteRecursive(File fileOrDirectory)
	{
		if (fileOrDirectory.isDirectory())
			for (File child : fileOrDirectory.listFiles())
				deleteRecursive(child);

		fileOrDirectory.delete();
	}
}