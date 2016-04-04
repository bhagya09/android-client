package com.bsb.hike.productpopup;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.bridge.JavascriptBridge;
import com.bsb.hike.productpopup.ProductPopupsConstants.HIKESCREEN;
import com.bsb.hike.productpopup.ProductPopupsConstants.PopUpAction;
import com.bsb.hike.utils.CustomAnnotation.DoNotObfuscate;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

@DoNotObfuscate
public class ProductJavaScriptBridge extends JavascriptBridge
{
	WeakReference<HikeDialogFragment> mHikeDialogFragment;
	
	Object productContentModel;

	public ProductJavaScriptBridge(CustomWebView mWebView, WeakReference<HikeDialogFragment> activity, Object productContentModel)
	{
		super(activity.get().getActivity(), mWebView);
		this.mHikeDialogFragment = activity;
		this.productContentModel=productContentModel;

	}

	@Override
	@JavascriptInterface
	public void logAnalytics(String isUI, String subType, String json)
	{
		Logger.d("ProductPopup","Analytics are "+isUI+"...."+subType+"..."+json.toString()+"");
	
		try
		{
			JSONObject mmObject = new JSONObject(json);
			if (Boolean.valueOf(isUI))
			{
				HAManager.getInstance().record(HikeConstants.UI_EVENT, subType, EventPriority.HIGH, mmObject);
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}

	@Override
	@JavascriptInterface
	public void onLoadFinished(final String height)
	{
		Logger.d("ProductPopup","Widht after  onLoadFinished " +mWebView.getWidth());
		onResize(height);
	}

	@JavascriptInterface
	public void onSubmit(final String action, final String metaData)
	{
		if (getActivity() != null)
		{
			takeAction(action, metaData, getActivity());
		}
		deletePopupAndDismissDialog();

		onDestroy();
	}

	protected void takeAction(String action, String metaData,Activity activity)
	{
		if (action.equals(PopUpAction.OPENAPPSCREEN.toString()))
		{
			String activityName = null;
			JSONObject mmObject = null;
			try
			{
				mmObject = new JSONObject(metaData);
				activityName = mmObject.optString(HikeConstants.SCREEN);

				if (activityName.equals(HIKESCREEN.MULTI_FWD_STICKERS.toString()))
				{
					sendMultiFwdSticker(metaData);
				}
				else if (activityName.equals(HIKESCREEN.OPEN_WEB_VIEW.toString()))
				{
					String url = ProductInfoManager.getInstance().getFormedUrl(metaData);

					if (!TextUtils.isEmpty(url))
						Utils.startWebViewActivity(activity, url, "hike");
				}
				else
				{
					openActivity(metaData);
				}

			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		if (action.equals(PopUpAction.CALLTOSERVER.toString()))
		{
			ProductInfoManager.getInstance().callToServer(metaData);
		}
		if (action.equals(PopUpAction.DOWNLOAD_STKPK.toString()))
		{
			downloadStkPack(metaData);
		}
		if (action.equals(PopUpAction.ACTIVATE_CHAT_HEAD_APPS.toString()))
		{
			activiteStickey();
		}
	}
	
	protected Activity getActivity()
	{
		if(mHikeDialogFragment.get()!=null)
		{
			if(mHikeDialogFragment.get() instanceof HikeDialogFragment)
			{
				return mHikeDialogFragment.get().getActivity();
			}
		}
		
		return null;
	}

	protected void deletePopupAndDismissDialog()
	{
		
		// deleting the popup from the data and memory
		
		if (productContentModel instanceof ProductContentModel)
		{
			ArrayList<ProductContentModel> mmArrayList = new ArrayList<ProductContentModel>();
			mmArrayList.add((ProductContentModel) productContentModel);
			ProductInfoManager.getInstance().deletePopups(mmArrayList);
		}

		if (mHikeDialogFragment != null && mHikeDialogFragment.get() != null)
		{
			if (productContentModel != null && productContentModel instanceof ProductContentModel && ((ProductContentModel) productContentModel).getConfig().showInPortraitOnly())
			{
				//ACRA crash
				if (mHikeDialogFragment.get().getActivity() != null)
				{
					mHikeDialogFragment.get().getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
				}
			}
			if (mHikeDialogFragment.get().isAdded())
			{
				mHikeDialogFragment.get().dismiss();
			}
			HikeAlarmManager.cancelAlarm(HikeMessengerApp.getInstance().getApplicationContext(), HikeAlarmManager.REQUESTCODE_PRODUCT_POPUP);
			
			//clearing the notification once the popup is been addressed
			NotificationManager notificationManager = (NotificationManager) HikeMessengerApp.getInstance().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
			if (notificationManager != null)
				notificationManager.cancel(HikeNotification.NOTIFICATION_PRODUCT_POPUP);

		}

	}
	
	public void anonNameSetStatus(final String string)
	{
		if (mHandler == null)
		{
			return;
		}
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mWebView.loadUrl("javascript:anonymousName" + "('" + string + "')");
			}
		});
		
	}

	/**
	 * Platform Version 9
	 *
	 * This function is called to request Init in case of productpopup apps
	 */
	@Override
	@JavascriptInterface
	public void requestInit() {
		super.requestInit();
		Logger.d("RequestInit","Request Init called");
		JSONObject object = new JSONObject();
		try {
			PlatformUtils.addLocaleToInitJSON(object);
			getInitJson(object, null);
			mWebView.loadUrl("javascript:init('" + getEncodedDataForJS(object.toString()) + "')");
			Logger.d("RequestInit",(object.toString()));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
