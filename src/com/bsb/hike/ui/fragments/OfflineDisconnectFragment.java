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

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.OfflineConstants;

public class OfflineDisconnectFragment extends Fragment
{
	String connectedMsisdn;
	
	String presentMsisdn;
	
	View fragmentView;
	
	OfflineConnectionRequestListener listener;
	
	public OfflineDisconnectFragment(String presentMsisdn,String connectedMsisdn,OfflineConnectionRequestListener listener)
	{
		this.presentMsisdn = presentMsisdn;
		this.connectedMsisdn = connectedMsisdn;
		this.listener = listener;
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
		ImageView avatar = (ImageView)fragmentView.findViewById(R.id.connected_avatar);
		Drawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(connectedMsisdn);
		if (drawable == null)
		{
			drawable = HikeMessengerApp.getLruCache().getDefaultAvatar(connectedMsisdn, false);
		}
		avatar.setScaleType(ScaleType.FIT_CENTER);
		avatar.setImageDrawable(drawable);
		TextView connectionRequest = (TextView)fragmentView.findViewById(R.id.connect_request);
		TextView connectionWarning = (TextView)fragmentView.findViewById(R.id.disconnect_warning);
		
		ContactInfo contactInfo  = ContactManager.getInstance().getContact(presentMsisdn);
		String contactFirstName = presentMsisdn;
		if(contactInfo!=null && !TextUtils.isEmpty(contactInfo.getFirstName()))
		{
			contactFirstName = contactInfo.getFirstName();
		}
		
		connectionRequest.setText(getResources().getString(R.string.disconnect_warning,contactFirstName));
		
		ContactInfo connectedContactInfo  = ContactManager.getInstance().getContact(connectedMsisdn);
		String connectedContactFirstName = connectedMsisdn;
		if(connectedContactInfo!=null && !TextUtils.isEmpty(connectedContactInfo.getFirstName()))
		{
			connectedContactFirstName = connectedContactInfo.getFirstName();
		}
		
		connectionWarning.setText(Html.fromHtml(getResources().getString(R.string.connected_warning,connectedContactFirstName)));
		
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
				listener.onConnectionRequest(true);
				Fragment fragment = getActivity().getSupportFragmentManager().findFragmentByTag(OfflineConstants.OFFLINE_DISCONNECT_FRAGMENT);
				if(fragment != null)
				    getActivity().getSupportFragmentManager().beginTransaction().remove(fragment).commit();
			}
		});

	}
	
	public interface OfflineConnectionRequestListener
	{ 
		public void onConnectionRequest(Boolean startAnimation);	
    }
	
}
