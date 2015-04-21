package com.bsb.hike.productpopup;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.platform.bridge.JavascriptBridge;
import com.bsb.hike.productpopup.ProductPopupsConstants.HIKESCREEN;
import com.bsb.hike.productpopup.ProductPopupsConstants.PopUpAction;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class ProductJavaScriptBridge extends JavascriptBridge
{
	WebView mmWebView;

	WeakReference<HikeDialogFragment> mHikeDialogFragment;
	
	Object productContentModel;

	public ProductJavaScriptBridge(WebView mWebView, WeakReference<HikeDialogFragment> activity, Object productContentModel)
	{
		super(activity.get().getActivity(), mWebView);
		this.mmWebView = mWebView;
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
		Logger.d("ProductPopup","Widht after  onLoadFinished " +mmWebView.getWidth());
		onResize(height);
	}

	@JavascriptInterface
	public void onSubmit(final String action, final String metaData)
	{
		if (mHandler != null)
		{

			mHandler.post(new Runnable()
			{

				@Override
				public void run()
				{
					if (getActivity() != null)
					{
						takeAction(action, metaData, getActivity());
					}
					deletePopupAndDismissDialog();

					onDestroy();
				}
			});
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
			 mHikeDialogFragment.get().dismiss();
			 HikeAlarmManager.cancelAlarm(mHikeDialogFragment.get().getActivity(), HikeAlarmManager.REQUESTCODE_PRODUCT_POPUP);
		}

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
					String stickerId = mmObject.optString(ProductPopupsConstants.STKID);
					String categoryId = mmObject.optString(ProductPopupsConstants.CATID);
					boolean selectAll = mmObject.optBoolean(ProductPopupsConstants.SELECTALL, false);
					if (!TextUtils.isEmpty(stickerId) && !TextUtils.isEmpty(categoryId))
					{
						multiFwdStickers(activity, stickerId, categoryId, selectAll);
					}
				}
				else if (activityName.equals(HIKESCREEN.OPEN_WEB_VIEW.toString()))
				{
					String url = ProductInfoManager.getInstance().getFormedUrl(metaData);

					if (!TextUtils.isEmpty(url))
						Utils.startWebViewActivity(activity, url, "hike");
				}
				else
				{
					openActivity(metaData,activity);
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

	}

	private void multiFwdStickers(Context context, String stickerId, String categoryId, boolean selectAll)
	{
		Intent intent = IntentFactory.getForwardStickerIntent(context, stickerId, categoryId);
		intent.putExtra(HikeConstants.Extras.SELECT_ALL_INITIALLY, selectAll);
		context.startActivity(intent);
	}

}
