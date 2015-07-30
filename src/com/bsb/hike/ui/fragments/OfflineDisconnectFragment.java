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
import com.bsb.hike.offline.OfflineConstants.OFFLINE_STATE;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.offline.OfflineUtils;

public class OfflineDisconnectFragment extends Fragment
{
	String firstMessage;
	
	String secondMessage;
	
	ImageView avatar;
	
	Drawable avatarDrawable;
	
	View fragmentView;
	
	OfflineConnectionRequestListener listener;
	
	public OfflineDisconnectFragment(String firstMessage,String secondMessage,Drawable avatarDrawable ,OfflineConnectionRequestListener listener)
	{
		this.firstMessage = firstMessage;
		this.secondMessage = secondMessage;
		this.avatarDrawable = avatarDrawable;
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
		avatar = (ImageView)fragmentView.findViewById(R.id.connected_avatar);
		avatar.setScaleType(ScaleType.FIT_CENTER);
		avatar.setImageDrawable(avatarDrawable);
		TextView connectionRequest = (TextView)fragmentView.findViewById(R.id.connect_request);
		TextView connectionWarning = (TextView)fragmentView.findViewById(R.id.disconnect_warning);
		
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
	
}
