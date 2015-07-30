package com.bsb.hike.ui.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

import com.actionbarsherlock.app.SherlockFragment;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.OfflineConstants;
import com.bsb.hike.offline.OfflineConstants.OFFLINE_STATE;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.offline.OfflineUtils;

public class OfflineDisconnectFragment extends SherlockFragment
{
	
	private static final String CONNECTINGMSISDN = "connecting_msisdn";

	private static final String CONNECTEDMSISDN = "connected_msisdn";

	String connectingMsisdn;
	
	String connectedMsisdn;
	
	ImageView avatar;
	
	Drawable avatarDrawable;
	
	View fragmentView;
	
	OfflineConnectionRequestListener listener;
	
	String firstMessage = "";
	
	String secondMessage = "";
	
	public static OfflineDisconnectFragment newInstance(String connectingMsisdn,String connectedMsisdn)
	{
		OfflineDisconnectFragment offlineDisconnectFragment  = new OfflineDisconnectFragment();
		Bundle data = new Bundle(2);
		data.putString(CONNECTINGMSISDN,connectingMsisdn);
		data.putString(CONNECTEDMSISDN, connectedMsisdn);
		offlineDisconnectFragment.setArguments(data);
		return offlineDisconnectFragment;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		fragmentView = inflater.inflate(R.layout.offline_disconnect_screen, null);
		setupView();
	    return fragmentView;
	}
	
	private void setupView()
	{
		avatar = (ImageView)fragmentView.findViewById(R.id.connected_avatar);
		avatar.setScaleType(ScaleType.FIT_CENTER);
		TextView connectionRequest = (TextView)fragmentView.findViewById(R.id.connect_request);
		TextView connectionWarning = (TextView)fragmentView.findViewById(R.id.disconnect_warning);
		if(!TextUtils.isEmpty(connectedMsisdn))
		{
			 
		    ContactInfo connectingContactInfo  = ContactManager.getInstance().getContact(connectingMsisdn);
			String connectingContactFirstName = connectingMsisdn;
			if(connectingContactInfo!=null && !TextUtils.isEmpty(connectingContactInfo.getFirstName()))
			{
				connectingContactFirstName = connectingContactInfo.getFirstName();
			}
			firstMessage = getResources().getString(R.string.disconnect_warning,connectingContactFirstName);
			ContactInfo connectedContactInfo  = ContactManager.getInstance().getContact(connectedMsisdn);
			String connectedContactFirstName = connectedMsisdn;
			if(connectedContactInfo!=null && !TextUtils.isEmpty(connectedContactInfo.getFirstName()))
			{
				connectedContactFirstName = connectedContactInfo.getFirstName();
			}
			secondMessage = getResources().getString(R.string.connected_warning,connectedContactFirstName);
			Drawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(connectedMsisdn);
			if (drawable == null)
			{
				drawable = HikeMessengerApp.getLruCache().getDefaultAvatar(connectedMsisdn, false);
			}
			avatar.setImageDrawable(drawable);
		}
		else
		{
			ContactInfo connectingContactInfo  = ContactManager.getInstance().getContact(connectingMsisdn);
			String connectingContactFirstName = connectingMsisdn;
			if(connectingContactInfo!=null && !TextUtils.isEmpty(connectingContactInfo.getFirstName()))
			{
				connectingContactFirstName = connectingContactInfo.getFirstName();
			}
			firstMessage = getResources().getString(R.string.cancel_connection,connectingContactFirstName);
			Drawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(connectingMsisdn);
			if (drawable == null)
			{
				drawable = HikeMessengerApp.getLruCache().getDefaultAvatar(connectingMsisdn, false);
			}
			avatar.setImageDrawable(drawable);
		}
		
		connectionRequest.setText(Html.fromHtml(firstMessage));
		connectionWarning.setText(Html.fromHtml(secondMessage));
		
		fragmentView.findViewById(R.id.reject_disconnect).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Fragment fragment = getActivity().getSupportFragmentManager().findFragmentByTag(OfflineConstants.OFFLINE_DISCONNECT_FRAGMENT);
				if(fragment != null)
				    getActivity().getSupportFragmentManager().beginTransaction().remove(fragment).commit();
			}
		});
		
		
		fragmentView.findViewById(R.id.accept_disconnect).setOnClickListener(new OnClickListener()
		{	
			@Override
			public void onClick(View v)
			{
				listener.onDisconnectionRequest();
				//If connected user wants to disconnect and start another connection
				if(OfflineController.getInstance().getOfflineState() == OFFLINE_STATE.CONNECTED)
				{
					listener.onConnectionRequest(true);
				}
				Fragment fragment = getActivity().getSupportFragmentManager().findFragmentByTag(OfflineConstants.OFFLINE_DISCONNECT_FRAGMENT);
				if(fragment != null)
				    getActivity().getSupportFragmentManager().beginTransaction().remove(fragment).commit();
			}
		});

	}
	
	public interface OfflineConnectionRequestListener
	{ 
		public void onConnectionRequest(Boolean startAnimation);	
		
		public void onDisconnectionRequest();
    }
	
	@Override
	public void onActivityCreated(Bundle arg0)
	{
		super.onActivityCreated(arg0);
		
	}
	
	@Override 
	public void onCreate(Bundle savedInstanceState)
	{ 
	   super.onCreate(savedInstanceState);
	   Bundle arguments = getArguments();
	    if(arguments != null)
	    {
	       handleArguments(arguments);
	    }
	}

	private void handleArguments(Bundle arguments)
	{
		connectingMsisdn = arguments.getString(CONNECTINGMSISDN);
		connectedMsisdn = arguments.getString(CONNECTEDMSISDN);
	} 
	
	public void setConnectionListner(OfflineConnectionRequestListener  listener)
	{
		this.listener =listener;
	}
}
