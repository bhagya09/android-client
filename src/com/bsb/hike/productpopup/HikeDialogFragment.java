package com.bsb.hike.productpopup;

import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebView;
import android.widget.LinearLayout;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.content.HikeWebClient;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import java.lang.ref.WeakReference;

public class HikeDialogFragment extends DialogFragment implements HikePubSub.Listener
{
	DialogPojo mmModel;

	CustomWebView mmWebView;

	ProductJavaScriptBridge mmBridge;

	View loadingCard;
	
	private String[] pubsub = new String[]{HikePubSub.ANONYMOUS_NAME_SET};

	public static HikeDialogFragment getInstance(DialogPojo productContentModel)
	{
		HikeDialogFragment mmDiallog = new HikeDialogFragment();
		 Bundle args=new Bundle();
		args.putParcelable(ProductPopupsConstants.BUNDLE_DATA, productContentModel);
		mmDiallog.setArguments(args);
		return mmDiallog;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		Logger.d("ProductPopup", "Dialog Orientation changed");

		if (loadingCard != null)
		{
			loadingCard.setVisibility(View.VISIBLE);
		}
		mmWebView.post((new Runnable()
		{

			@Override
			public void run()
			{
				mmWebView.loadDataWithBaseURL("", mmModel.getFormedData(), "text/html", "UTF-8", "");

			}
		}));
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mmModel = getArguments().getParcelable(ProductPopupsConstants.BUNDLE_DATA);
		if (mmModel.isFullScreen())
		{
		setStyle(STYLE_NO_TITLE, android.R.style.Theme_Holo_Light);
		}
		setRetainInstance(true);
		HikeMessengerApp.getPubSub().addListeners(this, pubsub);
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
		loadingCard = (View) view.findViewById(R.id.loading_data);
		mmWebView = (CustomWebView) view.findViewById(R.id.webView);
		if (!mmModel.isFullScreen())
		{
			int minHeight = (int) (mmModel.getHeight() * Utils.densityMultiplier);
			LayoutParams lp = mmWebView.getLayoutParams();
			lp.height = minHeight;
			 lp.width=LinearLayout.LayoutParams.MATCH_PARENT;
			Logger.i("HeightAnim", "set height given in card is =" + minHeight);
			mmWebView.setLayoutParams(lp);
		}
		if (mmModel.getData() instanceof ProductContentModel)
		{
			ProductContentModel model = (ProductContentModel) mmModel.getData();

			if (model.getConfig().showInPortraitOnly())
			{
				getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
		}
		loadingCard.setVisibility(View.VISIBLE);
		return view;
	}
	
	@Override
	public void onDetach()
	{
		// TODO Auto-generated method stub
		super.onDetach();
		HikeMessengerApp.getPubSub().removeListeners(this, pubsub);
	}
	/**
	 * 
	 * @param supportFragmentManager
	 * 
	 * 
	 * 
	 *            This method is responsible for attaching the fragment with the activity This is done as the fragment can perform commit after the onSaveInstance of the activity
	 *            is being called.
	 *            
	 * There is a developer option Dont keep Activites due which our onCreate and OnNewIntent was been fired(Ideally it shouldn't happen) So that is causing to add multiple
	 * popups on screen and it was not reflecting as the below command happens async. So calling it explicitly to make it sync.
	 * 
	 * TODO:Need to figure out why HomeActivity onNewIntent and OnCreate is been called
	 */
	public void showDialog(FragmentManager supportFragmentManager)
	{

		FragmentTransaction transaction = supportFragmentManager.beginTransaction();
		if (supportFragmentManager.findFragmentByTag(ProductPopupsConstants.DIALOG_TAG) != null)
		{
			 transaction.remove(supportFragmentManager.findFragmentByTag(ProductPopupsConstants.DIALOG_TAG)).commitAllowingStateLoss();
			 supportFragmentManager.executePendingTransactions();
			 transaction=supportFragmentManager.beginTransaction();
		}
			transaction.add(this, ProductPopupsConstants.DIALOG_TAG);
			transaction.commitAllowingStateLoss();
			
			supportFragmentManager.executePendingTransactions();
	}

	@Override
	public void onActivityCreated(Bundle arg0)
	{
		super.onActivityCreated(arg0);
		Logger.d("ProductPopup", "onActivityCreated");
		getDialog().setCanceledOnTouchOutside(false);
		if (mmModel.getData() instanceof ProductContentModel)
		{
			ProductContentModel productContentModel = (ProductContentModel) mmModel.getData();
			ProductInfoManager.recordPopupEvent(productContentModel.getAppName(), productContentModel.getPid(), productContentModel.isFullScreen(), ProductPopupsConstants.SEEN);
		}
		mmBridge = new ProductJavaScriptBridge(mmWebView, new WeakReference<HikeDialogFragment>(this), mmModel.getData());
		mmWebView.addJavascriptInterface(mmBridge, ProductPopupsConstants.POPUP_BRIDGE_NAME);
		mmWebView.setWebViewClient(new CustomWebClient());
		mmWebView.post(new Runnable()
		{

			@Override
			public void run()
			{
				Logger.d("ProductPopup", "in post runnable+ width is " + mmWebView.getWidth());
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
			Logger.d("ProductPopup", "Web View HEight and Width  on Page Started>>>>" + mmWebView.getHeight() + ">>>>>" + mmWebView.getWidth());
		}

		@Override
		public void onPageFinished(WebView view, String url)
		{
			super.onPageFinished(view, url);
			Logger.d("ProductPopup", "Widht after  onPageFinished " + mmWebView.getWidth());
			loadingCard.setVisibility(View.GONE);

		}
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (type.equals(HikePubSub.ANONYMOUS_NAME_SET))
		{
			mmBridge.anonNameSetStatus(object.toString());
		}
		
	}

}
