package com.bsb.hike.voip.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPConstants;
import com.bsb.hike.voip.VoIPService;
import com.bsb.hike.voip.VoIPUtils;

public class CallFailedFragment extends SherlockFragment
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.call_failed_fragment, null);

		initViews(view);
		slideInContainer(view);

		return view;
	}

	private void initViews(final View view)
	{
		final String msisdn = getArguments().getString(VoIPConstants.PARTNER_MSISDN);
		int callFailedCode = getArguments().getInt(VoIPConstants.CALL_FAILED_REASON);
		String partnerName = getArguments().getString(VoIPConstants.PARTNER_NAME);

		String firstNameOrMsisdn = partnerName == null ? msisdn : getFirstName(partnerName);

		TextView headerView = (TextView) view.findViewById(R.id.heading);
		TextView subHeaderView = (TextView) view.findViewById(R.id.subheading);
		Button callAgainButton = (Button) view.findViewById(R.id.call_again);

		final boolean enableRedial = setHeaderText(callFailedCode, headerView, firstNameOrMsisdn);
		subHeaderView.setText(getString(R.string.voip_call_failed_frag_subheader, firstNameOrMsisdn));

		if(!enableRedial)
		{
			callAgainButton.setText(R.string.voip_call_failed_native_call);
		}

		callAgainButton.setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v) {
				if(enableRedial)
				{
					Intent intent = IntentManager.getVoipCallIntent(getSherlockActivity(), msisdn, VoIPUtils.CallSource.CALL_FAILED_FRAG);
					getSherlockActivity().startService(intent);
				}
				else
				{
					getSherlockActivity().finish();
					Utils.startNativeCall(getSherlockActivity(), msisdn);
				}
				slideOutContainer(view);
			}
		});

		view.findViewById(R.id.hike_message).setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v) {
				Intent intent = IntentManager.getChatThreadIntent(getSherlockActivity(), msisdn);
				intent.putExtra(HikeConstants.Extras.SHOW_KEYBOARD, true);
				startActivity(intent);
				getSherlockActivity().finish();
			}
		});

		view.findViewById(R.id.voice_clip).setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v) {
				Intent intent = IntentManager.getChatThreadIntent(getSherlockActivity(), msisdn);
				intent.putExtra(HikeConstants.Extras.SHOW_RECORDING_DIALOG, true);
				startActivity(intent);
				getSherlockActivity().finish();
			}
		});

		view.findViewById(R.id.dismiss_button).setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v) {
				getSherlockActivity().finish();
			}
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{	
		super.onSaveInstanceState(outState);
	}

	public interface CallFailedFragListener
	{
		void removeCallFailedFragment();
	}

	private String getFirstName(String name)
	{
		int index = name.indexOf(" ");
		return index == -1 ? name : name.substring(0, index); 
	}

	private boolean setHeaderText(int callFailedCode, TextView view, String partnerName)
	{
		boolean enableRedial = true;

		switch(callFailedCode)
		{
			case VoIPConstants.ConnectionFailCodes.PARTNER_SOCKET_INFO_TIMEOUT:
			case VoIPConstants.ConnectionFailCodes.PARTNER_BUSY:
			case VoIPConstants.ConnectionFailCodes.PARTNER_ANSWER_TIMEOUT:
			case VoIPConstants.ConnectionFailCodes.CALLER_IN_NATIVE_CALL:
				view.setText(getString(R.string.voip_not_reachable, partnerName));
				break;

			case VoIPConstants.ConnectionFailCodes.PARTNER_INCOMPAT:
				view.setText(getString(R.string.voip_incompat_platform));
				enableRedial = false;
				break;

			case VoIPConstants.ConnectionFailCodes.PARTNER_UPGRADE:
				view.setText(getString(R.string.voip_older_app, partnerName));
				enableRedial = false;
				break;

			case VoIPConstants.ConnectionFailCodes.EXTERNAL_SOCKET_RETRIEVAL_FAILURE:
			case VoIPConstants.ConnectionFailCodes.UDP_CONNECTION_FAIL:
			case VoIPConstants.ConnectionFailCodes.CALLER_BAD_NETWORK:
				view.setText(getString(R.string.voip_caller_poor_network, partnerName));
				break;

			default:
				((CallFailedFragListener)getSherlockActivity()).removeCallFailedFragment();
		}

		return enableRedial;
	}

	private void slideInContainer(View view)
	{
		Animation anim = AnimationUtils.loadAnimation(getSherlockActivity(), R.anim.call_failed_frag_slide_in);
		view.findViewById(R.id.container).startAnimation(anim);
	}

	private void slideOutContainer(View view)
	{
		Animation anim = AnimationUtils.loadAnimation(getSherlockActivity(), R.anim.call_failed_frag_slide_out);
		anim.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				((CallFailedFragListener)getSherlockActivity()).removeCallFailedFragment();
			}
		});
		view.findViewById(R.id.container).startAnimation(anim);
	}
}
