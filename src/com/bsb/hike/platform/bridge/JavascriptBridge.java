package com.bsb.hike.platform.bridge;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.content.PlatformContent;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
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
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.productpopup.ProductPopupsConstants.HIKESCREEN;
import com.bsb.hike.ui.ComposeChatActivity;
import com.bsb.hike.ui.CreateNewGroupOrBroadcastActivity;
import com.bsb.hike.ui.HikeListActivity;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.StatusUpdate;
import com.bsb.hike.ui.TellAFriend;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * Created by shobhit on 13/02/15.
 */
public abstract class JavascriptBridge
{
	protected CustomWebView mWebView;

	protected WeakReference<Activity> weakActivity;;

	static final String tag = "JavascriptBridge";

	protected Handler mHandler;
	
	private static final String REQUEST_CODE = "request_code";
	
	private static final int PICK_CONTACT_REQUEST = 1;


	public JavascriptBridge(Activity activity, CustomWebView mWebView)
	{
		this.mWebView = mWebView;
		weakActivity = new WeakReference<Activity>(activity);
		this.mHandler = new Handler(HikeMessengerApp.getInstance().getMainLooper());
	}

	/**
	 * Call this function to log analytics events.
	 *
	 * @param isUI    : whether the event is a UI event or not. This is a string. Send "true" or "false".
	 * @param subType : the subtype of the event to be logged, eg. send "click", to determine whether it is a click event.
	 * @param json    : any extra info for logging events, including the event key that is pretty crucial for analytics.
	 */
	@JavascriptInterface
	protected abstract void logAnalytics(String isUI, String subType, String json);

	/**
	 * This function is called whenever the onLoadFinished of the html is called. This function calling is MUST.
	 * This function is also used for analytics purpose.
	 *
	 * @param height : The height of the loaded content
	 */
	@JavascriptInterface
	public abstract void onLoadFinished(String height);

	/**
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
						Intent intent = IntentFactory.getWebViewActivityIntent(weakActivity.get(), url, title);
						weakActivity.get().startActivity(intent);
					}
				}
			});
	}

	/**
	 * calling this function will share the screenshot of the webView along with the text at the top and a caption text
	 * to all social network platforms by calling the system's intent.
	 *
	 * @param text    : heading of the image with the webView's screenshot.
	 * @param caption : intent caption
	 */
	@JavascriptInterface
	public void share(String text, String caption)
	{
		FileOutputStream fos = null;
		File cardShareImageFile = null;
		Activity mContext = weakActivity.get();
		if(mContext!=null)
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
	 * this function will call the js back when the javascript demands some value back from the native.
	 * @param functionName : the function of javascript that native will call.
	 * @param value: value that will be given back.
	 */
	public void callbackToJS(final String functionName, final String value)
	{
		if (TextUtils.isEmpty(functionName))
		{
			Logger.e(tag, "Empty function name when calling the JS back");
			return;
		}
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mWebView.loadUrl("javascript:" + functionName + "(" + value + ")");
			}
		});
	}


	
}
