package com.bsb.hike.voip.view;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPConstants;
import com.bsb.hike.voip.VoIPUtils;

public class CallRatePopup extends SherlockDialogFragment
{
	private int rating = -1;
	
	private final String TAG = "CallRatePopup";

	private int isCallInitiator, callId;

	public CallRatePopup(){
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_CustomDialog);
		Bundle bundle = getArguments();
		if(bundle!=null)
		{
			isCallInitiator = bundle.getInt(VoIPConstants.IS_CALL_INITIATOR);
			callId = bundle.getInt(VoIPConstants.CALL_ID);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) 
	{
		View view = inflater.inflate(R.layout.voip_call_rate_popup, container, false);
		setCancelable(true);

		view.findViewById(R.id.call_rate_dismiss).setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					dismiss();
				}
			});

		view.findViewById(R.id.call_rate_submit).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(rating >= 0)
				{
					submitRating();
					dismiss();
				}
			}
		});

		final LinearLayout starsContainer = (LinearLayout)view.findViewById(R.id.star_rate_container);
		int childCount = starsContainer.getChildCount();
		
		if(bundle!=null)
		{
			rating = bundle.getInt("rating");
			for(int i=0; i<=rating; i++)
			{
				starsContainer.getChildAt(i).setSelected(true);
			}
		}

		OnClickListener rateListener = new OnClickListener() {
			
			@Override
			public void onClick(View view) {
				int index = starsContainer.indexOfChild(view);
				if(index < rating)
				{
					for(int i=index+1; i<=rating; i++)
					{
						starsContainer.getChildAt(i).setSelected(false);
					}
				}
				else
				{
					for(int i=0; i<=index; i++)
					{
						starsContainer.getChildAt(i).setSelected(true);
					}
				}
				rating = index;
			}
		};

		for(int i=0;i<childCount;i++)
		{
			starsContainer.getChildAt(i).setOnClickListener(rateListener);
		}
		return view;
		 
	}

	@Override
	public void onStart() 
	{
		super.onStart();
		if(getDialog() == null)
		{
			return;
		}
		getDialog().getWindow().setLayout((int)(280*Utils.densityMultiplier), LinearLayout.LayoutParams.WRAP_CONTENT);
	}

	private void submitRating()
	{
		try
		{
			JSONObject data = new JSONObject();
			data.put(HikeConstants.SUB_TYPE, HikeConstants.UI_EVENT);

			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_TYPE, HikeConstants.LogEvent.VOIP);
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.VOIP_CALL_RATE_POPUP_SUBMIT);
			metadata.put(VoIPConstants.Analytics.CALL_RATING, rating+1);
			metadata.put(VoIPConstants.Analytics.CALL_ID, callId);
			metadata.put(VoIPConstants.Analytics.IS_CALLER, isCallInitiator);

			data.put(HikeConstants.METADATA, metadata);

			Utils.sendLogEvent(data);
		}
		catch (JSONException e)
		{
			Logger.w(TAG, "Invalid json");
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) 
	{
		super.onSaveInstanceState(outState);
		outState.putInt("rating", rating);
	}
	
}
