package com.bsb.hike.ui.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
import com.bsb.hike.voip.view.CallFailedFragment.CallFailedFragListener;

public class OfflineDisconnectFragment extends SherlockFragment
{
	
	private static final String CONNECTINGMSISDN = "connecting_msisdn";

	private static final String CONNECTEDMSISDN = "connected_msisdn";

	private static final String  TYPE ="type" ;
	
	String connectingMsisdn;
	
	String connectedMsisdn;
	
	//type 1 - Connecting
	//type 0 - Connected
	int type ;
	
	ImageView avatar;
	
	Drawable avatarDrawable;
	
	View fragmentView;
	
	OfflineConnectionRequestListener listener;
	
	String firstMessage = "";
	
	String secondMessage = "";
	
	
	public static OfflineDisconnectFragment newInstance(String connectingMsisdn,String connectedMsisdn,int type)
	{
		OfflineDisconnectFragment offlineDisconnectFragment  = new OfflineDisconnectFragment();
		Bundle data = new Bundle(2);
		data.putString(CONNECTINGMSISDN,connectingMsisdn);
		data.putString(CONNECTEDMSISDN, connectedMsisdn);
		data.putInt(TYPE, type);
		offlineDisconnectFragment.setArguments(data);
		return offlineDisconnectFragment;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if(type==0)
		{
			fragmentView = inflater.inflate(R.layout.offline_disconnect_screen, null);
		}
		else
		{
			fragmentView = inflater.inflate(R.layout.offline_disconnecting_screen, null);
		}
		setupView();
		slideInContainer(fragmentView);
	    return fragmentView;
	}
	
	private void slideInContainer(View view)
	{
		TranslateAnimation anim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_SELF, 0);
		anim.setDuration(400);
		anim.setInterpolator(new OvershootInterpolator(0.9f));
		view.findViewById(R.id.offline_disconnect_view).startAnimation(anim);
	}
	
	private void setupView()
	{
		avatar = (ImageView)fragmentView.findViewById(R.id.connected_avatar);
		avatar.setScaleType(ScaleType.FIT_CENTER);
		
		if(type==0)
		{
			TextView connectionRequest = (TextView)fragmentView.findViewById(R.id.connect_request);
			TextView connectionWarning = (TextView)fragmentView.findViewById(R.id.disconnect_warning); 
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
			connectionRequest.setText(Html.fromHtml(firstMessage));
			connectionWarning.setText(Html.fromHtml(secondMessage));
		}
		else
		{
			TextView connectionRequest = (TextView)fragmentView.findViewById(R.id.connecting_request);
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
			connectionRequest.setText(Html.fromHtml(firstMessage));
			
		}
		
		
		
		
		fragmentView.findViewById(R.id.reject_disconnect).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				
				Animation anim = slideOutContainer(fragmentView);
				anim.setAnimationListener(new AnimationListener()
				{
					
					@Override
					public void onAnimationStart(Animation animation)
					{
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void onAnimationRepeat(Animation animation)
					{
						
					}
					
					@Override
					public void onAnimationEnd(Animation animation)
					{
						listener.removeDisconnectFragment(false);			
					}
				});
				fragmentView.findViewById(R.id.offline_disconnect_view).startAnimation(anim);
			}
		});
		
		
		fragmentView.findViewById(R.id.accept_disconnect).setOnClickListener(new OnClickListener()
		{	
			@Override
			public void onClick(View v)
			{
				
				
				Animation anim = slideOutContainer(fragmentView);
				anim.setAnimationListener(new AnimationListener()
				{
					
					@Override
					public void onAnimationStart(Animation animation)
					{
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void onAnimationRepeat(Animation animation)
					{
						
					}
					
					@Override
					public void onAnimationEnd(Animation animation)
					{
						listener.onDisconnectionRequest();
						
						//If connected user wants to disconnect and start another connection
						if(type==0)
						{
							listener.onConnectionRequest(true);
						}
						
						
						listener.removeDisconnectFragment(true);			
					}
				});
				fragmentView.findViewById(R.id.offline_disconnect_view).startAnimation(anim);
				
			}
		});

	}
	
	private Animation slideOutContainer(View view)
	{
		Animation anim = AnimationUtils.loadAnimation(getSherlockActivity(), R.anim.call_failed_frag_slide_out);
		anim.setDuration(300);
		return anim;
	}
	
	
	public interface OfflineConnectionRequestListener
	{ 
		public void onConnectionRequest(Boolean startAnimation);	
		
		public void onDisconnectionRequest();
		
		public void removeDisconnectFragment(boolean removeParent);
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
		type  = arguments.getInt(TYPE);
	} 
	
	public void setConnectionListner(OfflineConnectionRequestListener  listener)
	{
		this.listener =listener;
	}
}
