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
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.OfflineAnalytics;
import com.bsb.hike.offline.OfflineConstants.DisconnectFragmentType;
import com.bsb.hike.utils.StealthModeManager;

public class OfflineDisconnectFragment extends Fragment
{

	private static final String CONNECTINGMSISDN = "connecting_msisdn";

	private static final String CONNECTEDMSISDN = "connected_msisdn";

	private static final String TYPE = "type";

	String connectingMsisdn;

	String connectedMsisdn;

	// type 1 - Connecting
	// type 0 - Connected
	DisconnectFragmentType type;

	ImageView avatar;

	Drawable avatarDrawable;

	View fragmentView;

	OfflineConnectionRequestListener listener;

	String firstMessage = "";

	String secondMessage = "";

	public static OfflineDisconnectFragment newInstance(String connectingMsisdn, String connectedMsisdn, DisconnectFragmentType type)
	{
		OfflineDisconnectFragment offlineDisconnectFragment = new OfflineDisconnectFragment();
		Bundle data = new Bundle(2);
		data.putString(CONNECTINGMSISDN, connectingMsisdn);
		data.putString(CONNECTEDMSISDN, connectedMsisdn);
		data.putInt(TYPE, type.ordinal());
		offlineDisconnectFragment.setArguments(data);
		return offlineDisconnectFragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		switch (type)
		{
		case CONNECTED:
			fragmentView = inflater.inflate(R.layout.offline_disconnect_screen, null);
			break;
		case CONNECTING:
		case REQUESTING:
			fragmentView = inflater.inflate(R.layout.offline_disconnecting_screen, null);
			break;
		default:
			break;
		}
		setupView();
		slideInContainer(fragmentView);
		return fragmentView;
	}

	private void slideInContainer(View view)
	{
		TranslateAnimation anim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_SELF,
				0);
		anim.setDuration(400);
		anim.setInterpolator(new OvershootInterpolator(0.9f));
		view.findViewById(R.id.offline_disconnect_view).startAnimation(anim);
	}

	private void setupView()
	{
		avatar = (ImageView) fragmentView.findViewById(R.id.connected_avatar);
		avatar.setScaleType(ScaleType.FIT_CENTER);
		TextView connectionRequest,connectionWarning;
		ContactInfo connectingContactInfo;
		Drawable drawable=null;
		Button positiveBtn = (Button)fragmentView.findViewById(R.id.accept_disconnect);
		Button negativeBtn =  (Button)fragmentView.findViewById(R.id.reject_disconnect);
		switch (type)
		{
		case CONNECTED:
			connectionRequest = (TextView) fragmentView.findViewById(R.id.connect_request);
			connectionWarning = (TextView) fragmentView.findViewById(R.id.disconnect_warning);
			connectingContactInfo = ContactManager.getInstance().getContact(connectingMsisdn);
			String connectingContactFirstName = connectingMsisdn;
			if (connectingContactInfo != null && !TextUtils.isEmpty(connectingContactInfo.getFirstName()))
			{
				connectingContactFirstName = connectingContactInfo.getFirstName();
			}
			firstMessage = getResources().getString(R.string.disconnect_warning, connectingContactFirstName);
			ContactInfo connectedContactInfo = ContactManager.getInstance().getContact(connectedMsisdn);
			String connectedContactFirstName = connectedMsisdn;
			if (connectedContactInfo != null && !TextUtils.isEmpty(connectedContactInfo.getFirstName()))
			{
				connectedContactFirstName = connectedContactInfo.getFirstName();
			}
			secondMessage = getResources().getString(R.string.connected_warning, connectedContactFirstName);
			 drawable = HikeMessengerApp.getLruCache().getIconFromCache(connectedMsisdn);
			if (drawable == null)
			{
				drawable = HikeBitmapFactory.getDefaultTextAvatar(connectedMsisdn);
			}
			avatar.setImageDrawable(drawable);
			connectionRequest.setText(Html.fromHtml(firstMessage));
			connectionWarning.setText(Html.fromHtml(secondMessage));
			break;
		case CONNECTING:
			connectionRequest = (TextView) fragmentView.findViewById(R.id.connecting_request);
			connectingContactInfo = ContactManager.getInstance().getContact(connectingMsisdn);
			connectingContactFirstName = connectingMsisdn;
			if (connectingContactInfo != null && !TextUtils.isEmpty(connectingContactInfo.getFirstName()))
			{
				connectingContactFirstName = connectingContactInfo.getFirstName();
			}
			firstMessage = getResources().getString(R.string.cancel_connection, connectingContactFirstName);
			drawable = HikeMessengerApp.getLruCache().getIconFromCache(connectingMsisdn);
			if (drawable == null)
			{
				drawable = HikeBitmapFactory.getDefaultTextAvatar(connectingMsisdn);
			}
			avatar.setImageDrawable(drawable);
			connectionRequest.setText(Html.fromHtml(firstMessage));
			break;
		case REQUESTING:
			connectionRequest = (TextView) fragmentView.findViewById(R.id.connecting_request);
			connectingContactInfo = ContactManager.getInstance().getContact(connectingMsisdn);
			connectingContactFirstName = connectingMsisdn;
			
			if (StealthModeManager.getInstance().isStealthMsisdn(connectingMsisdn) && !StealthModeManager.getInstance().isActive())
			{
				firstMessage = getResources().getString(R.string.hike_direct_request);
				drawable  = HikeBitmapFactory.getDefaultTextAvatar(connectingMsisdn);
			}
			else
			{
				if (connectingContactInfo != null && !TextUtils.isEmpty(connectingContactInfo.getFirstName()))
				{
					connectingContactFirstName = connectingContactInfo.getFirstNameAndSurname();
				}
				firstMessage = getResources().getString(R.string.hike_direct_request, connectingContactFirstName);
				firstMessage = connectingContactFirstName + " "+ firstMessage;
				
				drawable = HikeMessengerApp.getLruCache().getIconFromCache(connectingMsisdn);
				if (drawable == null)
				{
					drawable = HikeBitmapFactory.getDefaultTextAvatar(connectingMsisdn);
				}
			}
			avatar.setImageDrawable(drawable);
			connectionRequest.setText(Html.fromHtml(firstMessage));
			positiveBtn.setTextColor(getResources().getColor(R.color.black_60));
			negativeBtn.setTextColor(getResources().getColor(R.color.black_60));
			negativeBtn.setText(getResources().getString(R.string.cancel));
			positiveBtn.setText(getResources().getString(R.string.connect));
			break;
		default:
			break;

		}

		negativeBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{

				OfflineAnalytics.disconnectPopupClicked(type, 0);
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

		positiveBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{

				OfflineAnalytics.disconnectPopupClicked(type, 1);
				Animation anim = slideOutContainer(fragmentView);
				anim.setAnimationListener(new AnimationListener()
				{

					@Override
					public void onAnimationStart(Animation animation)
					{
						listener.onDisconnectionRequest();

						// If connected user wants to disconnect and start another connection
						if (type == DisconnectFragmentType.CONNECTED)
						{
							listener.onConnectionRequest(true);
						}
						

					}

					@Override
					public void onAnimationRepeat(Animation animation)
					{

					}

					@Override
					public void onAnimationEnd(Animation animation)
					{
						if(type == DisconnectFragmentType.REQUESTING)
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
		Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.call_failed_frag_slide_out);
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
		if (arguments != null)
		{
			handleArguments(arguments);
		}
	}

	private void handleArguments(Bundle arguments)
	{
		connectingMsisdn = arguments.getString(CONNECTINGMSISDN);
		connectedMsisdn = arguments.getString(CONNECTEDMSISDN);
		type = DisconnectFragmentType.values()[arguments.getInt(TYPE)];
	}

	public void setConnectionListner(OfflineConnectionRequestListener listener)
	{
		this.listener = listener;
	}
}
