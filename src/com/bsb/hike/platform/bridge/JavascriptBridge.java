package com.bsb.hike.platform.bridge;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
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
import android.webkit.MimeTypeMap;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.cropimage.HikeCropActivity;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.FileRequestPersistent;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.platform.CocosGamingActivity;
import com.bsb.hike.platform.CustomTabFallBackImpl;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.IFileUploadListener;
import com.bsb.hike.platform.NativeBridge;
import com.bsb.hike.platform.PlatformHelper;
import com.bsb.hike.platform.PlatformUIDFetch;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.auth.AuthListener;
import com.bsb.hike.platform.auth.PlatformAuthenticationManager;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.ui.ComposeChatActivity;
import com.bsb.hike.ui.fragments.ShareLinkFragment;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.CustomAnnotation.DoNotObfuscate;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.ShareUtils;
import com.bsb.hike.utils.Sim;
import com.bsb.hike.utils.Telephony;
import com.bsb.hike.utils.Utils;
import com.google.gson.JsonArray;
import com.squareup.okhttp.Headers;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.HashMap;

/**
 * API bridge that connects the javascript to the Native environment. Make the instance of this class and add it as the JavaScript interface of the Card WebView.
 * This class caters Platform Bridge versions from:
 *
 * Platform Bridge Version Start = 0
 * Platform Bridge Version End = ~
 */


@DoNotObfuscate
public abstract class JavascriptBridge
{
	protected CustomWebView mWebView;

	protected WeakReference<Activity> weakActivity;

	public static final String tag = "JavascriptBridge";
	
	protected Handler mHandler;
	
	protected static final String REQUEST_CODE = "request_code";
	
	private static final int PICK_CONTACT_REQUEST = 1;

	public static final int PICK_CONTACT_AND_SEND_REQUEST = 2;
	
	protected static final int CLOSE_WEB_VIEW = 3;

	protected static final int SHARE_EXTERNAL = 5;

	boolean sendIntentData = false;

	protected static final int MAX_COUNT =2;

	//Hashmap of URL vs Count
	HashMap<String,Integer> platformRequest=new HashMap<String,Integer>();
	
	public JavascriptBridge(Activity activity, CustomWebView mWebView)
	{
		this.mWebView = mWebView;
		weakActivity = new WeakReference<Activity>(activity);
		this.mHandler = new Handler(HikeMessengerApp.getInstance().getMainLooper())
		{
			public void handleMessage(Message msg) {
				handleUiMessage(msg);
			};
		};
	}

	/**
	 * Platform Bridge Version 0
	 * Call this function to log analytics events.
	 *
	 * @param isUI    : whether the event is a UI event or not. This is a string. Send "true" or "false".
	 * @param subType : the subtype of the event to be logged, eg. send "click", to determine whether it is a click event.
	 * @param json    : any extra info for logging events, including the event key that is pretty crucial for analytics.
	 */
	@JavascriptInterface
	protected abstract void logAnalytics(String isUI, String subType, String json);

	/**
	 * Platform Bridge Version 0
	 * This function is called whenever the onLoadFinished of the html is called. This function calling is MUST.
	 * This function is also used for analytics purpose.
	 *
	 * @param height : The height of the loaded content
	 */
	@JavascriptInterface
	public void onLoadFinished(String height)
	{
	}
	
	protected void handleUiMessage(Message msg)
	{
		switch (msg.what)
		{
		case CLOSE_WEB_VIEW :
			
			Activity currActivity = weakActivity.get();
			if (currActivity != null)
			{
				currActivity.finish();
			}
			
			break;
			case SHARE_EXTERNAL :
				JSONObject json = (JSONObject)msg.obj;
				String title = json.optString("title");
				String caption = json.optString("caption");
				PlatformUtils.share(title,caption,weakActivity.get(),mWebView);

		default:
			break;
		}
	}
	
	protected void sendMessageToUiThread(int what,Object data)
	{
		sendMessageToUiThread(what, 0,0, data);
	}

	protected void sendMessageToUiThread(int what,int arg1, Object data)
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
	 * Platform Bridge Version 0
	 * call this function to Show toast for the string that is sent by the javascript.
	 *
	 * @param toast : the string to show in toast.
	 */
	@JavascriptInterface
	public void showToast(String toast)
	{
		if(weakActivity.get()!=null)
		{
			Toast.makeText(weakActivity.get(), toast, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Platform Bridge Version 0
	 * Call this function to vibrate the device.
	 *
	 * @param msecs : the number of milliseconds the device will vibrate.
	 */
	@JavascriptInterface
	public void vibrate(String msecs)
	{
		Utils.vibrate(Integer.parseInt(msecs));
	}

	/**
	 * Platform Bridge Version 0
	 * call this function with parameter as true to enable the debugging for javascript.
	 * The debuggable for javascript will get enabled only after KITKAT version.
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
	 * Platform Bridge Version 0
	 * calling this function will generate logs for testing at the android IDE. The first param will be tag used for logging and the second param
	 * is data that is used for logging. this will create verbose logs for testing purposes.
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
	 * Platform bridge Version 0
	 * Call this function to open a full page webView within hike.
	 *
	 * @param title : the title on the action bar.
	 * @param url   : the url that will be loaded.
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
					if(weakActivity.get()!=null)
					{
						PlatformUtils.openCustomTab(url,title,weakActivity.get(),new CustomTabFallBackImpl(weakActivity.get()));
					}
				}
			});
	}

	/**
	 * Platform Bridge Version 1
	 * call this function to open a web page in the default browser.
	 * @param url: : the url that will be loaded.
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
	 * Platform Bridge Version 0
	 * calling this function will share the screenshot of the webView along with the text at the top and a caption text
	 * to all social network platforms by calling the system's intent.
	 *
	 * @param text    : heading of the image with the webView's screenshot.
	 * @param caption : intent caption
	 */
	@JavascriptInterface
	public void share(String text, String caption)
	{
		JSONObject json = new JSONObject();
		try
		{
			json.put("text", text);
			json.put("caption", caption);
			sendMessageToUiThread(SHARE_EXTERNAL, json);
		}
		catch (JSONException e)
		{
			Logger.e(tag, "Error in share");
		}

	}

	/**
	 * Platform Bridge Version 0
	 * Whenever the content's height is changed, the html will call this function to resize the height of the Android Webview.
	 * Calling this function is MUST, whenever the height of the content changes.
	 *
	 * @param height : the new height when the content is reloaded.
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
				height = (int) (Utils.densityMultiplier * height); // javascript returns us in dp
				WebView webView = mWebView.get();
				if (webView != null)
				{
					Logger.i(tag, "HeightRunnable called with height=" + height
							+ " and current height is " + webView.getHeight());

					int initHeight = webView.getMeasuredHeight();

					Logger.i("HeightAnim", "InitHeight = " + initHeight
							+ " TargetHeight = " + height);

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
		if (mHandler == null)
		{
			return;
		}

		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mWebView.onActivityDestroyed();
			}
		});

	}
	
	@JavascriptInterface
	public void openActivity(final String data)
	{
		
		if (mHandler == null || weakActivity == null || weakActivity.get() == null)
		{
			return;
		}

		mHandler.post(new Runnable() {
			@Override
			public void run() {
				PlatformUtils.openActivity(weakActivity.get(), data);
			}
		});
	}
	
	/**
	 * Platform Bridge Version 0
	 * This function can be used to start a hike native contact chooser/picker which will show all hike contacts to user and user can select few contacts (minimum 1). It will call
	 * JavaScript function "onContactChooserResult(int resultCode,JsonArray array)" This JSOnArray contains list of JSONObject where each JSONObject reflects one user. As of now
	 * each JSON will have name and platform_id, e.g : [{'name':'Paul','platform_id':'dvgd78as'}] resultCode will be 0 for fail and 1 for success NOTE : JSONArray could be null as
	 * well, a micro app has to take care of this instance and startContactChooser not present
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
			intent.putExtra(HikeConstants.Extras.THUMBNAILS_REQUIRED, true);
			activity.startActivityForResult(intent, HikeConstants.PLATFORM_REQUEST);
		}
	}

    /**
     * Platform Bridge Version 8
     * This function can be used to start a hike native contact chooser/picker which will show hike contacts to user based on the given msisdn in requestJson
     * It will call JavaScript function callbackFromNative "(String functionCallbackId , JSON : [{(int resultCode,JsonArray array)})]". As of now
     * JSON will have name,platform_id,thumbnail,msisdn e.g : [{'name':'Paul','platform_id':'dvgd78as','msisdn':'9988776554','thumbnail':''}].
     * ResultCode will be 0 for fail and 1 for success NOTE : JSONArray could be null as well
     */
    @JavascriptInterface
    public void startContactChooserForMsisdnFilter(String id,String requestJson) {
        Activity activity = weakActivity.get();
        String title = "";

        if(!TextUtils.isEmpty(requestJson)) {
            try {
                JSONObject json = new JSONObject(requestJson);
                title = json.getString(HikeConstants.Extras.TITLE);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (activity != null) {

        	Intent intent = new Intent(activity, ComposeChatActivity.class);
            intent.putExtra(HikeConstants.Extras.EDIT, true);
            intent.putExtra(HikeConstants.Extras.COMPOSE_MODE, ComposeChatActivity.PAYMENT_MODE);
            intent.putExtra(HikeConstants.Extras.IS_CONTACT_CHOOSER_FILTER_INTENT,true);
            intent.putExtra(HikeConstants.Extras.FUNCTION_ID,id);
            intent.putExtra(HikeConstants.Extras.TITLE, title);
            activity.startActivityForResult(intent, HikeConstants.PLATFORM_MSISDN_FILTER_DISPLAY_REQUEST);
        }
    }
   


	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Logger.d(tag, "onactivity result of javascript");

		if (requestCode != -1)
		{
            if(requestCode == HikeConstants.PLATFORM_MSISDN_FILTER_DISPLAY_REQUEST)
            {
                JSONObject responseJsonObj = new JSONObject();
                if (!TextUtils.isEmpty(data.getStringExtra(HikeConstants.HIKE_CONTACT_PICKER_RESULT)))
                {
                    try {
                        responseJsonObj.put(HikeConstants.Extras.RESULT_CODE, "1");
                        responseJsonObj.put(HikeConstants.Extras.CONTACT_INFO, data.getStringExtra(HikeConstants.HIKE_CONTACT_PICKER_RESULT));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else
                {
                    try {
                        responseJsonObj.put(HikeConstants.Extras.RESULT_CODE, "0");
                        responseJsonObj.put(HikeConstants.Extras.CONTACT_INFO, "'[]'");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                callbackToJS(data.getStringExtra(HikeConstants.Extras.FUNCTION_ID),responseJsonObj.toString());
                return;
            }
            else if (requestCode == HikeConstants.PLATFORM_FILE_CHOOSE_REQUEST)

				handlePickFileResult(resultCode, data);
			else
			{

				requestCode = data.getIntExtra(REQUEST_CODE, -1);

				switch (requestCode)
				{
				case PICK_CONTACT_REQUEST:
				case PICK_CONTACT_AND_SEND_REQUEST:
					handlePickContactResult(resultCode, data);
					break;
				}
			}
		}
	}
	
	private void handlePickFileResult(int resultCode, Intent data)
	{
		if (resultCode == Activity.RESULT_OK)
		{
			String id = data.getStringExtra(HikeConstants.CALLBACK_ID);
			callbackToJS(id,PlatformUtils.getFileUploadJson(data));
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

	/**
	 *
	 * this function will call the js back when the javascript demands some value back from the native.
	 * @param id : the id of the function that native will call to call the js .
	 * @param value: value that will be given back. it is encoded with URL Encoded Scheme. Decode it before using.
	 */
	public void callbackToJS(final String id, final String value)
	{
		if (TextUtils.isEmpty(id))
		{
			Logger.e(tag, "Empty ID name when calling the JS back");
			return;
		}
		if (mHandler == null)
		{
			Logger.e(tag,"callbacck to JS is empty nHandler");
			return;
		}
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (!mWebView.isWebViewDestroyed())
				{
					if (Utils.isKitkatOrHigher())
					{
						Logger.d(tag, "Inside call back to js with id " + id);
						try
						{
							mWebView.evaluateJavascript("javascript:callbackFromNative" + "('" + id + "','" + getEncodedDataForJS(value) + "')",null);
						}
						catch (IllegalStateException e)
						{
							mWebView.loadUrl("javascript:callbackFromNative" + "('" + id + "','" + getEncodedDataForJS(value) + "')"); // On some Custom ROMs and Nokia phones depite being on API greater than 19 this function is not available.This API not supported on Android 4.3 and earlier\n\tat android.webkit.WebViewClassic.evaluateJavaScript(WebViewClassic.java:2674)\n\tat
						}
					}
					else
					{
						mWebView.loadUrl("javascript:callbackFromNative" + "('" + id + "','" + getEncodedDataForJS(value) + "')");
					}
				} else
				{
					Logger.e(tag, "CallBackToJs>>WebView not showing");
				}
			}
		});
	}
	
	/**
	 * Platform Bridge Version 1
	 * This method will be called when you need to get the Connection type on the device. The result returned will be one of the following ordinal values :
	 *
	 * <li>-1 in case of no network</li>
	 * <li>0 in case of unknown network</li>
	 * <li>1 in case of wifi</li>
	 * <li>2 in case of 2g</li>
	 * <li>3 in case of 3g</li>
	 * <li>4 in case of 4g</li>
	 *
	 * @param id
	 */
	@JavascriptInterface
	public void checkConnection(String id)
	{
		callbackToJS(id, Integer.toString(Utils.getNetworkType(HikeMessengerApp.getInstance().getApplicationContext())));
	}

	/**
	 * Platform Bridge Version 3
	 * call this function to call the non-messaging bot`
	 * @param id : : the id of the function that native will call to call the js .
	 * @param msisdn: the msisdn of the non-messaging bot to be opened.
	 * returns Success if success and failure if failure.
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
					return;
				}
			}
		}
		callbackToJS(id, "Failure");
	}

	/**
	 * Platform Bridge Version 3
	 * call this function to upload multiple files to the server
	 * @param id			:	the function id
	 * @param data          :   stringified json. Consists of filePath, uploadUrl, doCompress fields.
	 * returns the response on each file upload success
	 */
	@JavascriptInterface
	public void uploadFile(final String id,String data)
	{	Logger.d("FileUpload","input , uplaodFile with ID "+id);
		if(data == null)
		{
			callbackToJS(id, "Data field Null");
			return;
		}
		JSONObject json;
		String filePath = null;
		String url = null;
		boolean doCompress = false;
		try
		{
			json = new JSONObject(data);
			filePath = json.getString("filePath");
			url = json.getString("uploadUrl");
			doCompress = json.getBoolean("doCompress");
		}
		catch (JSONException e)
		{	
			Logger.e("fileUpload","Malformed Json object"+"filePath = "+filePath+" "+"url = " + url+" "+" docompress = "+ doCompress);
			return;
		}
		
		if(TextUtils.isEmpty(filePath)  || TextUtils.isEmpty(url))
		{
			callbackToJS(id, "JSON content Null or Length = 0");
			return;
		}
		
		String fileExtension = MimeTypeMap.getFileExtensionFromUrl(filePath);
		File temp_file =new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + "_temp");
		if(!temp_file.exists())
		{
			temp_file.mkdirs();
		}
		final String tempFilePath = PlatformContentConstants.PLATFORM_CONTENT_DIR + "_temp" + File.separator + (new File(filePath).getName());
		
		IFileUploadListener fileListener = new IFileUploadListener()
		{
			/*
			 * (non-Javadoc)
			 * params:id,null is sent in case of invalid response
			 */
			@Override
			public void onRequestFailure(String response)
			{
				Logger.d("FileUpload", "Failure Response from the server is ----->" + response + ", id is" + id);
				callbackToJS(id, "");
				File tempFile = new File(tempFilePath);
				if(tempFile.exists())
				{
					PlatformUtils.deleteDirectory(PlatformContentConstants.PLATFORM_CONTENT_DIR + "_temp");
				}
			}
			
			@Override
			public void onRequestSuccess(String response)
			{
				Logger.d("FileUpload", "Success Response from the server is ----->" + response+ ", id is" + id);
				callbackToJS(id, response);
				File tempFile = new File(tempFilePath);
				if(tempFile.exists())
				{
					PlatformUtils.deleteDirectory(PlatformContentConstants.PLATFORM_CONTENT_DIR + "_temp");
				}
			}
		};
		
		if(fileExtension != null && MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension).contains("image") && doCompress)
		{
			Utils.compressAndCopyImage(filePath, tempFilePath, weakActivity.get());
			Logger.d("FileUpload", "original size =" + (new File(filePath)).length());
			Logger.d("FileUpload", "compressed size =" + (new File(tempFilePath)).length());
			PlatformUtils.uploadFile(tempFilePath, url, fileListener);
		}
		else
		{
			PlatformUtils.uploadFile(filePath, url, fileListener);
		}
	}

	public void getInitJson(JSONObject jsonObj, String msisdn)
	{
		try
		{
			jsonObj.put(HikeConstants.MSISDN, msisdn);
			jsonObj.put(HikePlatformConstants.PLATFORM_USER_ID, HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_UID_SETTING, null));
			if(!HikeSharedPreferenceUtil.getInstance().getData(HikePlatformConstants.NEW_AUTH_ENABLE, false)){
				jsonObj.put(HikePlatformConstants.PLATFORM_TOKEN, HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_TOKEN_SETTING, null));
			}
			jsonObj.put(HikePlatformConstants.APP_VERSION, AccountUtils.getAppVersion());
			if(msisdn.equalsIgnoreCase(HikeConstants.MicroApp_Msisdn.HIKE_RECHARGE)){
				Telephony telephonyInfo = Telephony.getInstance(HikeMessengerApp.getInstance().getApplicationContext());
				   Sim[] sims= telephonyInfo.getSims();
					JSONArray array = new JSONArray();
					for(int i =0; i<sims.length; i++){
						if(sims[i]!=null){
						  array.put(sims[i].toJSONString());
						}else{
							 array.put(null);
						}
					}
				
			    jsonObj.put(HikePlatformConstants.SIM_OPERATORS,array);
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	protected String getEncodedDataForJS(String data) {
		try
		{
			return URLEncoder.encode(data,"utf-8").replaceAll("\\+", "%20");
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		return "";
		
	}

	/**
	 * Platform Bridge Version 1
	 * call this function for any post call.
	 * @param functionId : function id to call back to the js.
	 * @param data : the stringified data that contains:
	 *     "url": the url that will be called.
	 *     "params": the push params to be included in the body.
	 * Response to the js will be sent as follows:
	 * callbackFromNative(functionId, responseJson)
	 *    responseJson will be like this:
	 *          Success: "{ "status": "success", "status_code" : status_code , "response": response}"
	 *          Failure: "{ "status": "failure", "error_message" : error message, "status_code": status code}"
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
			JSONObject json;
			if(TextUtils.isEmpty(params))
			{
				json=null;
			}
			else
			{
				json=new JSONObject(params);
			}
			RequestToken token = HttpRequests.microAppPostRequest(url, json, new PlatformMicroAppRequestListener(functionId));
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
	 * Platform Bridge Version 3
	 * call this function for any get call.
	 * @param functionId : function id to call back to the js.
	 * @param url : "url": the url that will be called.
	 * Response to the js will be sent as follows:
	 * callbackFromNative(functionId, responseJson)
	 *    responseJson will be like this:
	 *          Success: "{ "status": "success", "status_code" : status_code , "response": response}"
	 *          Failure: "{ "status": "failure", "error_message" : error message, "status_code": status code}"
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


	/**
	 * Platform Bridge Version 3
	 * Call this function to enable zooming in webViews.
	 * @param enabled
	 */
	@JavascriptInterface
	public void setZoomEnabled(final String enabled)
	{
		Logger.d(tag, "set zoom enabled called with " + enabled);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
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

					mWebView.getSettings().setBuiltInZoomControls(Boolean.valueOf(enabled));
					mWebView.getSettings().setDisplayZoomControls(false);
				}
			});

		}
	}

	
	/**
	 * Platform Version 3
	 * Call this function to send email. This function opens Android email intent to send email. Hike Version, Device name, Android version and Phone number are added by default in the email body.
	 *
	 * @param subject: the Subject of the email. If subject is empty, the fallback subject is "Feedback on hike for Android" in different languages.
	 * @param body:    the body of the email. User can change the body on his own as well.
	 * @param sendTo:  the sender email id. if email id is empty, the fallback email is sent to "support@hike.in"
	 */
	@JavascriptInterface
	public void sendEmail(String subject, String body, String sendTo)
	{
		Context context = weakActivity.get();
		Intent intent = IntentFactory.getEmailOpenIntent(context, subject, body, sendTo);
		context.startActivity(intent);
	}

	/**
	 * Platform Bridge Version 6
	 * call this function to call the non-messaging bot`
	 * @param id : : the id of the function that native will call to call the js .
	 * @param msisdn: the msisdn of the non-messaging bot to be opened.
	 * @param data : the data to be sent to the bot.
	 * returns Success if success and failure if failure.
	 */
	@JavascriptInterface
	public void openNonMessagingBot(String id, String msisdn, String data)
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
					intent.putExtra(HikePlatformConstants.MICROAPP_DATA, data);
					weakActivity.get().startActivity(intent);
					callbackToJS(id, "Success");
					return;
				}
			}
		}
		callbackToJS(id, "Failure");
	}

	public void sendMicroappIntentData(String data)
	{
		if (sendIntentData)
		{
			return;
		}

		sendIntentData = true;
		mWebView.loadUrl("javascript:intentData(" + "'" + getEncodedDataForJS(data) + "')");
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
				failure.put(HikePlatformConstants.STATUS_CODE, httpException.getErrorCode());
				if(httpException.getErrorCode()== HttpURLConnection.HTTP_UNAUTHORIZED);
				{
					PlatformUIDFetch.fetchPlatformUid(HikePlatformConstants.PlatformFetchType.SELF);
				}
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

	/**
	 * Added in Platform Version:5
	 * @param stickerData
	 * 
	 * This function is used to share multifwd sticker.
	 * Sample JSON:{'catId':'expressions','stkId':'002_lol.png','selectAll':false}
	 */
	@JavascriptInterface
	public void sendMultiFwdSticker(String stickerData)
	{
		Logger.d(tag,"sendmultiFwdSticker");
		if (mHandler == null || weakActivity == null)
		{
			return;
		}
		
		try
		{
			JSONObject mmObject = new JSONObject(stickerData);
			final String stickerId = mmObject.optString(ProductPopupsConstants.STKID);
			final String categoryId = mmObject.optString(ProductPopupsConstants.CATID);
			final boolean selectAll = mmObject.optBoolean(ProductPopupsConstants.SELECTALL, false);
			final boolean sendAll=mmObject.optBoolean(ProductPopupsConstants.SENDALL,false);
			if (!TextUtils.isEmpty(stickerId) && !TextUtils.isEmpty(categoryId))
			{

				if(sendAll)
				{
					PlatformUtils.sendStickertoAllHikeContacts(stickerId,categoryId);
					return;
				}

				mHandler.post(new Runnable()
				{
					
					@Override
					public void run()
					{
						PlatformUtils.multiFwdStickers(weakActivity.get(), stickerId, categoryId, selectAll);
					}
				});
				}

		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Added in Platform Version:5
	 * This function is used to activate Stickey on the client
	 */
	@JavascriptInterface
	public void activiteStickey()
	{
		Logger.d(tag,"onChatHeadPopupActivateClick");
		PlatformUtils.OnChatHeadPopupActivateClick();
	}

	/**
	 * Added in Platform Version:5
	 * @param stickerData
	 * This function is used to download a sticker pack on the client.
	 * Sample JSON:{'catId':'doggy','categoryName':'Adorable Snuggles','totalStickers':30,'categorySize':100}
	 */
	@JavascriptInterface
	public void downloadStkPack(String stickerData)
	{
		Logger.d(tag,"downaloadStkPack");
		PlatformUtils.downloadStkPk(stickerData);
	}
	
	
	/**
	 * Platform Version 6 Call this function to close the current activity. This function closes the current activity and takes the user back to the previous activity.
	 */
	@JavascriptInterface
	public void closeWebView()
	{
		if (mHandler != null)
		{
			mHandler.sendEmptyMessage(CLOSE_WEB_VIEW);
		}
	}
	
	public void pickContactAndSend(ConvMessage message)
	{
		PlatformHelper.pickContactAndSend(message, weakActivity.get(),JavascriptBridge.this.hashCode());
	}
	
	public void sendSharedMessage(String cardObject, String hikeMessage, String sharedData, BotInfo mBotInfo)
	{
		PlatformHelper.sendSharedMessage(cardObject, hikeMessage, sharedData, mBotInfo, weakActivity.get(), JavascriptBridge.this.hashCode());
	}

	/**
	 * Platform Version 8
	 * This function is made for the special Group bot that has the information about inviting members to join group from WA/Others App.
	 * 
	 */
	@JavascriptInterface
	public void sendGCInvitationViaLinkSharing()
	{
		Context mContext = HikeMessengerApp.getInstance().getApplicationContext();
		String groupId = OneToNConversationUtils.createNewGroupId(mContext);
		int number = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.WA_GROUP_NUMBER, 1);
		String groupName = mContext.getString(R.string.wa_group) + " " + number;
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.WA_GROUP_NUMBER, number + 1);
		
		ShareLinkFragment fragment = ShareLinkFragment.newInstance(groupId, groupName, 1, true, true);
		fragment.initViaArguments();
		fragment.setButtonClickedType(ShareLinkFragment.WA);
		fragment.makeHttpCallForURL();
	}
	
	/**
	 * Platform Version 8
	 * This function is made for the special Group bot that has the information about inviting members to join group from WA/Others App.
	 * 
	 */
	@JavascriptInterface
	public void remindGCInvitationViaLinkSharing()
	{
		Context mContext = HikeMessengerApp.getInstance().getApplicationContext();
		String baseText = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.TEXT_FOR_GC_VIA_WA, mContext.getString(R.string.link_share_wa_msg)); 
		String url = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHARE_LINK_URL_FOR_GC, null);
		
		if(!TextUtils.isEmpty(url))
		{
			String finalText = baseText + "\n" + url;
			ShareUtils.shareContent(HikeConstants.Extras.ShareTypes.TEXT_SHARE, finalText, HikeConstants.Extras.WHATSAPP_PACKAGE);
		}
		else
		{
			//NO GROUP WAS CREATED
		}
	}

	/**
	 * Platform Version 6 Call this function to open a given Intent.
	 * 
	 * @param IntentName
	 *            JS has to ensure the intent is a valid name, and has to provide the intent URI(e.g. android.settings.LOCATION_SOURCE_SETTINGS)
	 */
	@JavascriptInterface
	public void openIntent(String intentURI)
	{

		Activity currActivity = weakActivity.get();
		if (currActivity != null)
		{
			Intent intent;
			try
			{
				intent = new Intent(intentURI);
			currActivity.startActivity(intent);
			}
			catch (ActivityNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	@JavascriptInterface
	/**
	 * Added in Platform Version:7
	 * 
	 * @param id
	 *            : : the id of the function that native will call to call the js . Show a native dialog and know the button clicked by the user
	 *            returns positive/negative. Title,Message and positiveBtn are compulsory.
	 */
	public void showDialog(final String id, String title, String message, String positiveBtn, String negativeBtn)
	{
		if(TextUtils.isEmpty(title)||TextUtils.isEmpty(message)||TextUtils.isEmpty(positiveBtn))
			return;
		Activity mContext = weakActivity.get();
		if (mContext != null)
		{
			HikeDialogListener nativeDialogListener = new HikeDialogListener()
			{

				@Override
				public void negativeClicked(HikeDialog hikeDialog)
				{
					callbackToJS(id, "negative");
					hikeDialog.dismiss();

				}

				@Override
				public void positiveClicked(HikeDialog hikeDialog)
				{
					callbackToJS(id, "positive");
					hikeDialog.dismiss();

				}

				@Override
				public void neutralClicked(HikeDialog hikeDialog)
				{
					// TODO Auto-generated method stub

				}

			};

			HikeDialogFactory.showDialog(mContext, HikeDialogFactory.MICROAPP_DIALOG, nativeDialogListener, title, message, positiveBtn, negativeBtn);
		}
	}
	/**
	 * Added in Platform Version 9. Call this function to set anon name
	 * @param name
	 */
	@JavascriptInterface
	public void setAnonName(String name)
	{
		if(!TextUtils.isEmpty(name))
		{
			PlatformUIDFetch.fetchPlatformUid(HikePlatformConstants.PlatformFetchType.SELF_ANONYMOUS_NAME, name);
		}
	}
	/**
	 * Added in Platform Version 9. Call this function to get anon name if exists,else will return null string.
	 * @param id
	 */
	@JavascriptInterface
	public void getAnonName(String id)
	{
		String anonName=HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.ANONYMOUS_NAME_SETTING,"");
		callbackToJS(id, anonName);
	}
	

	/**
	 * Platform Version 9
	 * This method is used to show a native popup with a WebView rendered within it.
	 * contentData must have cardObj. Inside cardObj, ld must be present and should be a JSONObject.
	 * @param contentData
	 */
	@JavascriptInterface
	public void showPopup(final String contentData)
	{
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				PlatformHelper.showPopup(contentData, weakActivity.get());
			}
		});
	}
	/**
	 * Platform Version 9
	 * Call this method to get the status of app download
	 * @param id
	 * @param app The app name
	 * returns progress
	 * returns empty string if download not yet started
	 */
	@JavascriptInterface
	public void readPartialDownloadState(String id,String app)
	{
		String filePath=PlatformContentConstants.PLATFORM_CONTENT_DIR+app+FileRequestPersistent.STATE_FILE_EXT;
		String data[]=PlatformUtils.readPartialDownloadState(filePath);
		if(data==null||data.length<2||TextUtils.isEmpty(data[1]))
		{
			callbackToJS(id,"");
			return;
		}
		callbackToJS(id, data[1]);
	}
	/**
	 * Platform Version 9
	 * Call this method to check if a bot exists
	 * @param id
	 * @param msisdn
	 * Returns true/false string
	 */
	@JavascriptInterface
	public void isBotExist(String id,String msisdn)
	{
		BotInfo botinfo=BotUtils.getBotInfoForBotMsisdn(msisdn);
		if(botinfo!=null)
			callbackToJS(id,"true");
		else
			callbackToJS(id,"false");
	}
	
	/**
	 * Call this method to provide a callback to the game with the appropriate response.
	 *
	 * @param callId
	 * @param response
	 */
	@JavascriptInterface
	public void nativePlatformCallback(String callId, String response)
	{
		NativeBridge nativeBridge = (NativeBridge) CocosGamingActivity.getNativeBridge();
		if (nativeBridge != null)
		{
			nativeBridge.platformCallback(callId, response);
		}

	}
	/**
	 * Platform Version 9
	 * Call this method to decrease the unread counter.
	 * @param msisdn whose unread count has to be modified.
	 */
	@JavascriptInterface
	public void resetUnreadCounter(String msisdn)
	{
		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
		if (botInfo==null)
			return;
		Utils.resetUnreadCounterForConversation(botInfo);
		botInfo.setUnreadCount(0);

	}

    /**
	 * Platform Version 9
	 * This function is made  to know whether a microapp exists.
	 * @param id: the id of the function that native will call to call the js .
	 * @param mapp: the name of the mapp.
	 */
	@JavascriptInterface
	public void isMicroappExist(String id, String mapp)
	{
        // Check for is Micro App exists in all of the directories path that are being used after the versioning release
		String microAppUnzipDirectoryPath = PlatformUtils.getMicroAppContentRootFolder();
        File fileInMappsDirectory = new File(microAppUnzipDirectoryPath + PlatformContentConstants.HIKE_MAPPS + mapp);
        File fileInGamesDirectory = new File(microAppUnzipDirectoryPath + PlatformContentConstants.HIKE_GAMES + mapp);
        File fileNameInGamesAfterMigrationCheck = new File(microAppUnzipDirectoryPath + PlatformContentConstants.HIKE_GAMES + PlatformContentConstants.HIKE_DIR_NAME.toLowerCase() + mapp);
        File fileInHikeWebMicroAppsDirectory = new File(microAppUnzipDirectoryPath + PlatformContentConstants.HIKE_WEB_MICRO_APPS + mapp);
        File fileInHikePopupsDirectory = new File(microAppUnzipDirectoryPath + PlatformContentConstants.HIKE_ONE_TIME_POPUPS + mapp);
        File fileInOldContentDirectory = new File(PlatformContentConstants.PLATFORM_CONTENT_OLD_DIR + mapp);

        if (fileInMappsDirectory.exists())
			callbackToJS(id, "true");
        else if(fileInGamesDirectory.exists())
            callbackToJS(id, "true");
        else if(fileNameInGamesAfterMigrationCheck.exists())
            callbackToJS(id, "true");
        else if(fileInHikeWebMicroAppsDirectory.exists())
            callbackToJS(id, "true");
        else if(fileInHikePopupsDirectory.exists())
            callbackToJS(id, "true");
        else if(fileInOldContentDirectory.exists())
            callbackToJS(id, "true");
		else
			callbackToJS(id, "false");
	}

	/**
	 * Platform Version 12
	 * Method to get server time offset. It will return the difference in the client and server
	 * @param id: the id of the function that native will call to call the js .
	 */
	@JavascriptInterface
	public void getServerTimeOffset(String id)
	{
		long offset = Utils.getServerTimeOffsetInMsec(HikeMessengerApp.getInstance().getApplicationContext());
		callbackToJS(id, String.valueOf(offset));
	}

	/**
	 * Platform Version 12
	 * Method to get group details. It will return the participants and the group information
	 * @param id: the id of the function that native will call to call the js .
	 */
	@JavascriptInterface
	public void getGroupDetails(String id, String groupId)
	{
		String groupDetails = PlatformHelper.getGroupDetails(groupId);
		callbackToJS(id, String.valueOf(groupDetails));
	}

	/**
	 * Platform Version 9
	 *
	 * This function is called to request Init in case of productpopup apps
	 */
	@JavascriptInterface
	public void requestInit()
	{

	}
	/**
	 * Platform Bridge Version 12
	 * call this function to call the non-messaging bot`
	 * @param id : : the id of the function that native will call to call the js .
	 * @param msisdn: the msisdn of the non-messaging bot to be opened.
	 * @param data : the data to be sent to the bot.
	 * @param source : The source of the bot open .
	 * returns Success if success and failure if failure.
	 */
	@JavascriptInterface
	public void openNonMessagingBot(String id, String msisdn, String data,String source)
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
					intent.putExtra(HikePlatformConstants.MICROAPP_DATA, data);
					intent.putExtra(AnalyticsConstants.BOT_NOTIF_TRACKER, source);
					weakActivity.get().startActivity(intent);
					callbackToJS(id, "Success");
					return;
				}
			}
		}
		callbackToJS(id, "Failure");
	}
}
