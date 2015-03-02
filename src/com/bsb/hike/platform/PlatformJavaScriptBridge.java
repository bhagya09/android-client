package com.bsb.hike.platform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.WebViewCardRenderer.WebViewHolder;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * API bridge that connects the javascript to the Native environment. Make the instance of this class and add it as the JavaScript interface of the Card WebView.
 */

public class PlatformJavaScriptBridge
{

	private static final String tag = "platformbridge";

	Context mContext;

	WebView mWebView;

	ConvMessage message;

	BaseAdapter adapter;

	public PlatformJavaScriptBridge(Context c, WebView webView, ConvMessage convMessage, BaseAdapter adapter)
	{
		this.mContext = c;
		this.mWebView = webView;
		this.message = convMessage;
		this.adapter = adapter;
	}

	/**
	 * call this function to Show toast for the string that is sent by the javascript.
	 * @param toast :
	 */
	@JavascriptInterface
	public void showToast(String toast)
	{
		Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Call this function to vibrate the device.
	 * @param msecs : the number of milliseconds the device will vibrate.
	 */
	@JavascriptInterface
	public void vibrate(String msecs)
	{
		Utils.vibrate(Integer.parseInt(msecs));
	}

	/**
	 * call this function to delete the message. The message will get deleted instantaneously
	 */
	@JavascriptInterface
	public void deleteMessage()
	{
		ArrayList<Long> msgIds = new ArrayList<Long>(1);
		msgIds.add(message.getMsgID());
		Bundle bundle = new Bundle();
		if (adapter.getCount() <= 1)
		{
			bundle.putBoolean(HikeConstants.Extras.IS_LAST_MESSAGE, true);
		}
		else
		{
			bundle.putBoolean(HikeConstants.Extras.IS_LAST_MESSAGE, false);
		}

		bundle.putString(HikeConstants.Extras.MSISDN, message.getMsisdn());
		bundle.putBoolean(HikeConstants.Extras.DELETE_MEDIA_FROM_PHONE, false);
		HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_MESSAGE, new Pair<ArrayList<Long>, Bundle>(msgIds, bundle));
	}

	/**
	 * call this function with parameter as true to enable the debugging for javascript.
	 * The debuggable for javascript will get enabled only after KITKAT version.
	 * @param setEnabled
	 */
	@JavascriptInterface
	public void setDebuggableEnabled(final String setEnabled)
	{
		Logger.d(tag, "set debuggable enabled called with " + setEnabled);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
		{
			mWebView.post(new Runnable()
			{
				@Override
				public void run()
				{
					if (Boolean.valueOf(setEnabled))
					{

						mWebView.setWebContentsDebuggingEnabled(true);
					}
					else
					{
						mWebView.setWebContentsDebuggingEnabled(false);
					}
				}
			});


		}
	}

	@JavascriptInterface
	public void allowUniversalAccess()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
		{
			mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
			mWebView.getSettings().setAllowFileAccessFromFileURLs(true);
		}
	}

    /**
     * Call this function to log analytics events.
     * @param isUI : whether the event is a UI event or not. This is a string. Send "true" or "false".
     * @param subType : the subtype of the event to be logged, eg. send "click", to determine whether it is a click event.
     * @param json : any extra info for logging events, including the event key that is pretty crucial for analytics.
     */
    @JavascriptInterface
    public void logAnalytics(String isUI, String subType, String json)
    {

		try
		{
			String msisdn = message.getMsisdn();
			JSONObject jsonObject = new JSONObject(json);
			jsonObject.put(AnalyticsConstants.CHAT_MSISDN, msisdn);
			jsonObject.put(AnalyticsConstants.ORIGIN, Utils.conversationType(msisdn));
			jsonObject.put(HikePlatformConstants.CARD_TYPE, message.platformWebMessageMetadata.getAppName());
			if (Boolean.valueOf(isUI))
			{
				HikeAnalyticsEvent.analyticsForCards(AnalyticsConstants.MICROAPP_UI_EVENT, subType, jsonObject);
			}
			else
			{
				HikeAnalyticsEvent.analyticsForCards(AnalyticsConstants.MICROAPP_NON_UI_EVENT, subType, jsonObject);
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
		}
	}
	/**
	 * Call this function to set the alarm at certain time that is defined by the second parameter.
	 * The first param is a json that contains
	 * 1.alarm_data: the data that the javascript receives when the alarm is played.
	 * 2.delete_card: if present and true, used to delete the message on alarm getting played
	 * 3.conv_msisdn: this field is must Send the msisdn.
	 * 4.inc_unread: if inc_unread is present and true, we will increase red unread counter in Conversation screen.
	 * 5.notification: contains message  if you want to show notification at some particular time
	 * 6.notification_sound: true if we you want to play sound
	 * sample json  :  {alarm_data:{}, conv_msisdn:'', ;delete_card' : 'true' , 'inc_unread' :'true ' , 'notification': 'message', 'notification_sound':'true'}
	 * @param json
	 * @param timeInMills
	 */
	@JavascriptInterface
	public void setAlarm(String json, String timeInMills)
	{
		try
		{
			Logger.i(tag, "set alarm called " + json + " , mId " + message.getMsgID() + " , time " + timeInMills);
			PlatformAlarmManager.setAlarm(mContext, new JSONObject(json), (int)message.getMsgID(), Long.valueOf(timeInMills));
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * this function will update the helper data. It will replace the key if it is present in the helper data and will add it if it is
	 * not present in the helper data.
	 * @param json
	 */
	@JavascriptInterface
	public void updateHelperData(String json)
	{
		try
		{
			Logger.i(tag, "update metadata called " + json + " , message id=" + message.getMsgID());
			String updatedJSON = HikeConversationsDatabase.getInstance().updateHelperData((message.getMsgID()), json);
			if (updatedJSON != null)
			{
				message.platformWebMessageMetadata = new PlatformWebMessageMetadata(updatedJSON);
			}

		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * This function will replace the entire metadata of the message.
	 * @param metadata
	 */
	@JavascriptInterface
	public void replaceMetadata(String metadata)
	{
		try
		{
			message.platformWebMessageMetadata = new PlatformWebMessageMetadata(new JSONObject(metadata));
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * calling this function will delete the alarm associated with this javascript.
	 */
	@JavascriptInterface
	public void deleteAlarm()
	{
		HikeConversationsDatabase.getInstance().deleteAppAlarm((int)(message.getMsgID()));
	}

	/**
	 * calling this function will generate logs for testing at the android IDE. The first param will be tag used for logging and the second param
	 * is data that is used for logging. this will create verbose logs for testing purposes.
	 * @param tag
	 * @param data
	 */
	@JavascriptInterface
	public void logFromJS(String tag, String data)
	{
		Logger.v(tag, data);
	}

	/**
	 * Calling this function will update the metadata. If the key is already present, it will be replaced else it will be added to the existent metadata.
	 * If the json has JSONObject as key, there would be another round of iteration, and will replace the key-value pair if the key is already present
	 * and will add the key-value pair if the key is not present in the existent metadata.
	 * @param json
	 * @param notifyScreen : if true, the adapter will be notified of the change, else there will be only db update.
	 */
	@JavascriptInterface
	public void updateMetadata( String json, String notifyScreen)
	{

		try
		{
			Logger.i(tag, "update metadata called " + json + " , message id=" + message.getMsgID() +" notifyScren is "+notifyScreen);
			String updatedJSON = HikeConversationsDatabase.getInstance().updateJSONMetadata((int)(message.getMsgID()), json);

			if (updatedJSON != null)
			{
				message.platformWebMessageMetadata = new PlatformWebMessageMetadata(updatedJSON); // the new metadata to inflate in webview
				if (notifyScreen != null && Boolean.valueOf(notifyScreen))
				{
					mWebView.post(new Runnable()
					{

						@Override
						public void run()
						{
							Object obj = mWebView.getTag();
							if (obj instanceof WebViewHolder)
							{
								Logger.i(tag, "updated metadata and calling notifydataset of " + adapter.getClass().getName() + " and thread= " + Thread.currentThread().getName());
								WebViewHolder holder = (WebViewHolder) obj;
								holder.id = -1; // will make sure new metadata is inflated in webview
								adapter.notifyDataSetChanged();
							}
							else
							{
								Logger.e(tag, "Expected Tag of Webview was WebViewHolder and received " + obj.getClass().getCanonicalName());
							}

						}
					});
				}
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Calling this function will initiate forward of the message to a friend or group.
	 * @param json : if the data has changed , then send the updated fields and it will update the metadata.
	 *             If the key is already present, it will be replaced else it will be added to the existent metadata.
	 * 			If the json has JSONObject as key, there would be another round of iteration, and will replace the key-value pair if the key is already present
	 * 		and will add the key-value pair if the key is not present in the existent metadata.
	 */
	@JavascriptInterface
	public void forwardToChat( String json)
	{
		try
		{
			Logger.i(tag, "forward to chat called " + json + " , message id=" + message.getMsgID());

			if (!TextUtils.isEmpty(json))
			{
				String updatedJSON = HikeConversationsDatabase.getInstance().updateJSONMetadata((int)(message.getMsgID()), json);
				if (!TextUtils.isEmpty(updatedJSON))
				{
					message.platformWebMessageMetadata = new PlatformWebMessageMetadata(updatedJSON);
				}
			}

			final Intent intent = IntentManager.getForwardIntentForConvMessage(mContext, message,
					PlatformContent.getForwardCardData(message.platformWebMessageMetadata.JSONtoString()));
			mWebView.post(new Runnable()
			{
				@Override
				public void run()
				{
					mContext.startActivity(intent);
				}
			});

		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Call this function to open a full page webView within hike.
	 * @param title : the title on the action bar.
	 * @param url : the url that will be loaded.
	 */
	@JavascriptInterface
	public void openFullPage(String title, String url)
	{
		Logger.i(tag, "open full page called with title " + title + " , and url = " + url);
		final Intent intent = IntentManager.getWebViewActivityIntent(mContext, url, title);
		mWebView.post(new Runnable()
		{
			@Override
			public void run()
			{
				mContext.startActivity(intent);
			}
		});

	}
	
	@JavascriptInterface
	public void share(){
		share(null,null);
	}
	
	@JavascriptInterface
	public void share(String text,String caption)
	{
		FileOutputStream fos = null;
		File cardShareImageFile = null;
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
			shB.compress(CompressFormat.JPEG, 100, fos);
			fos.flush();
			Logger.i(tag, "share webview card " + cardShareImageFile.getAbsolutePath());
			Utils.startShareImageIntent("image/jpeg", "file://" + cardShareImageFile.getAbsolutePath(),
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
			
			if (cardShareImageFile != null && cardShareImageFile.exists())
			{
				cardShareImageFile.deleteOnExit();
			}
		}
	}

	/**
	 * This function is called whenever the onLoadFinished of the html is called. This function calling is MUST.
	 * This function is also used for analytics purpose.
	 * @param height : The height of the loaded content
	 */
	@JavascriptInterface
	public void onLoadFinished(String height)
	{
		try
		{
			int requiredHeightinDP = Integer.parseInt(height);
			int requiredHeightInPX = (int) (requiredHeightinDP * Utils.densityMultiplier);
			if(requiredHeightInPX != mWebView.getHeight())
			{
				Logger.i(tag, "onloadfinished called with height=" + requiredHeightInPX + " current height is " + mWebView.getHeight() +" : updated in DB as well");
				// lets save in DB, so that from next time onwards we will have less flickering
				message.platformWebMessageMetadata.setCardHeight(requiredHeightinDP);
				HikeConversationsDatabase.getInstance().updateMetadataOfMessage(message.getMsgID(), message.platformWebMessageMetadata.JSONtoString());
				resizeWebview(height);
			}else
			{
				Logger.i(tag, "onloadfinished called with height=" + requiredHeightInPX + " current height is " + mWebView.getHeight());
			}
				
		}catch(NumberFormatException ne)
		{
			ne.printStackTrace();
		}
		
	}

	/**
	 * Whenever the content's height is changed, the html will call this function to resize the height of the Android Webview.
	 * Calling this function is MUST, whenever the height of the content changes.
	 * @param height : the new height when the content is reloaded.
	 */
	@JavascriptInterface
	public void onResize(String height)
	{
		Logger.i(tag, "onresize called with height=" + (Integer.parseInt(height) * Utils.densityMultiplier));
		resizeWebview(height);
	}

	private void resizeWebview(String heightS)
	{
		if (!TextUtils.isEmpty(heightS))
		{
			heightRunnable.mWebView = new WeakReference<WebView>(mWebView);
			heightRunnable.height = Integer.parseInt(heightS);
			mWebView.post(heightRunnable);
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
					} else if (initHeight > height) 
					{
						collapse(webView, height);
					} else if (initHeight < height) 
					{
						expand(webView, height);
					}
				}

			}
		}
	};

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

	public void updateConvMessage(ConvMessage message)
	{
		this.message = message;
	}
	
	public void onDestroy()
	{
		mWebView.removeCallbacks(heightRunnable);
	}
}
