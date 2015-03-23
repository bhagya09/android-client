package com.bsb.hike.productpopup;

import java.lang.ref.WeakReference;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnWindowAttachListener;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.bsb.hike.R;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.content.HikeWebClient;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class HikeDialogFragment extends DialogFragment
{
	DialogPojo mmModel;

	WebView mmWebView;

	ProductJavaScriptBridge mmBridge;
	
	View loadingCard;

	public static HikeDialogFragment getInstance(DialogPojo productContentModel)
	{
		HikeDialogFragment mmDiallog = new HikeDialogFragment(productContentModel);
		return mmDiallog;
	}

	private HikeDialogFragment(DialogPojo productContentModel)
	{
		mmModel = productContentModel;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		Logger.d("ProductPopup", "Dialog Orientation changed");
		
		mmWebView.loadDataWithBaseURL("", mmModel.getFormedData(), "text/html", "UTF-8", "");
		super.onConfigurationChanged(newConfig);
	}
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (mmModel.isFullScreen())
		{
		setStyle(STYLE_NO_TITLE, android.R.style.Theme_Holo_Light);
		}
		setRetainInstance(true);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		Dialog dialog = super.onCreateDialog(savedInstanceState);

		if (!mmModel.isFullScreen())
		{
			dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		}
		return dialog;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.product_popup, container, false);
		loadingCard=(View)view.findViewById(R.id.loading_data);
		mmWebView =  (WebView) view.findViewById(R.id.webView);
		if (!mmModel.isFullScreen())
		{
			int minHeight = (int) (mmModel.getHeight() * Utils.densityMultiplier);
			LayoutParams lp = mmWebView.getLayoutParams();
			lp.height = minHeight;
			lp.width=LinearLayout.LayoutParams.MATCH_PARENT;
			Logger.i("HeightAnim", "set height given in card is =" + minHeight);
			mmWebView.setLayoutParams(lp);
		}
		loadingCard.setVisibility(View.VISIBLE);
		return view;
	}
	
	/**
	 * 
	 * @param supportFragmentManager
	 * 
	 *           
	 * 
	 * This method is responsible for attaching the fragment with the activity 
	 *	This is done as the fragment can perform commit after the onSaveInstance of the activity is being called. 
	 */
	public void showDialog(FragmentManager supportFragmentManager)
	{
			FragmentTransaction transaction = supportFragmentManager.beginTransaction();
			if(supportFragmentManager.findFragmentByTag(ProductPopupsConstants.DIALOG_TAG)!=null)
			{
				transaction.remove(supportFragmentManager.findFragmentByTag(ProductPopupsConstants.DIALOG_TAG)).commitAllowingStateLoss();
				transaction=supportFragmentManager.beginTransaction();
			}
			transaction.add(this, ProductPopupsConstants.DIALOG_TAG);
			transaction.commitAllowingStateLoss();
	}

	@Override
	public void onActivityCreated(Bundle arg0)
	{
		super.onActivityCreated(arg0);

		mmBridge = new ProductJavaScriptBridge(mmWebView, new WeakReference<HikeDialogFragment>(this), mmModel.getData());

		mmWebView.addJavascriptInterface(mmBridge, ProductPopupsConstants.POPUP_BRIDGE_NAME);
		mmWebView.setWebViewClient(new CustomWebClient());
		mmWebView.post(new Runnable()
		{
			
			@Override
			public void run()
			{
				Logger.d("ProductPopup","in post runnable+ width is "+mmWebView.getWidth());
				mmWebView.loadDataWithBaseURL("", mmModel.getFormedData(), "text/html", "UTF-8", "");
			}
		});
		
	}

	
	class CustomWebClient extends HikeWebClient
	{
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon)
		{
			super.onPageStarted(view, url, favicon);
			Logger.d("ProductPopup", "Web View HEight and Width  on Page Started>>>>" + mmWebView.getHeight()+">>>>>"+mmWebView.getWidth());
		}

		@Override
		public void onPageFinished(WebView view, String url)
		{
			super.onPageFinished(view, url);
			Logger.d("ProductPopup","Widht after  onPageFinished " +mmWebView.getWidth());
			loadingCard.setVisibility(View.GONE);
			
		}
	}
	

}
