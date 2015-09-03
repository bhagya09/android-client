package com.bsb.hike.voip.view;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.voip.VoIPConstants;

public class CallIssuesDialogFragment extends DialogFragment
{
	public CallIssuesDialogFragment(){
	}

	private final String TAG = "CallIssuesPopup";

	private String selectedIssues = "";

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_CustomDialog);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) 
	{
		View view = inflater.inflate(R.layout.call_issues_popup, container, false);
		setCancelable(true);

		OnClickListener issueListener = new OnClickListener() {
			
			@Override
			public void onClick(View view) {
				view.setSelected(!view.isSelected());
			}
		};

		TableLayout issuesContainer = (TableLayout) view.findViewById(R.id.issues_container);
		int i, rowCount = issuesContainer.getChildCount();
		for(i=0; i<rowCount; i++)
		{
			TableRow row = (TableRow) issuesContainer.getChildAt(i);
			int j, colCount = row.getChildCount();
			for(j=0; j<colCount; j++)
			{
				row.getChildAt(j).setOnClickListener(issueListener);
			}
		}

		view.findViewById(R.id.call_issues_submit).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(getSelectedIssues())
				{
					dismiss();
					Toast.makeText(getActivity(), R.string.voip_call_issues_submit_toast, Toast.LENGTH_SHORT).show();
					getActivity().finish();
				}
			}
		});

		return view;
	}

	@Override
	public void onCancel(DialogInterface dialog)
	{
		getActivity().finish();
		super.onCancel(dialog);
	}

	private boolean getSelectedIssues()
	{
		TableLayout issuesContainer = (TableLayout) getView().findViewById(R.id.issues_container);
		int i, rowCount = issuesContainer.getChildCount();

		StringBuilder sb = new StringBuilder();
		boolean atLeastOneSelected = false;

		for(i=0; i<rowCount; i++)
		{
			TableRow row = (TableRow) issuesContainer.getChildAt(i);
			int j, colCount = row.getChildCount();
			for(j=0; j<colCount; j++)
			{
				View v = row.getChildAt(j);
				if(v.isSelected())
				{
					sb.append("1");
					atLeastOneSelected = true;
				}
				else
				{
					sb.append("0");
				}
			}
		}
		selectedIssues = sb.toString();
		return atLeastOneSelected;
	}

	@Override
	public void onDismiss(DialogInterface dialog)
	{
		sendAnalytics();
		super.onDismiss(dialog);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) 
	{
		super.onSaveInstanceState(outState);
	}

	private void sendAnalytics()
	{
		Bundle bundle = getArguments();
		int isCallInitiator = -1, callId = -1, rating = -1, network = -1;
		String toMsisdn = "";
		if(bundle!=null)
		{
			isCallInitiator = bundle.getInt(VoIPConstants.IS_CALL_INITIATOR);
			callId = bundle.getInt(VoIPConstants.CALL_ID);
			rating = bundle.getInt(VoIPConstants.CALL_RATING);
			network = bundle.getInt(VoIPConstants.CALL_NETWORK_TYPE);
			toMsisdn = bundle.getString(VoIPConstants.PARTNER_MSISDN);
		}

		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_TYPE, HikeConstants.LogEvent.VOIP);
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.VOIP_CALL_RATE_POPUP_SUBMIT);
			metadata.put(VoIPConstants.Analytics.CALL_RATING, rating);
			metadata.put(VoIPConstants.Analytics.CALL_ID, callId);
			metadata.put(VoIPConstants.Analytics.IS_CALLER, isCallInitiator);
			metadata.put(VoIPConstants.Analytics.NETWORK_TYPE, network);
			metadata.put(AnalyticsConstants.TO, toMsisdn);
			metadata.put(VoIPConstants.Analytics.CALL_ISSUES, selectedIssues);
			metadata.put(VoIPConstants.Analytics.NEW_LOG, -1);

			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
		}
		catch (JSONException e)
		{
			Logger.w(TAG, "Invalid json");
		}
	}
}
