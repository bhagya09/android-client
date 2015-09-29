package com.bsb.hike.ui.fragments;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.ShareUtils;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontTextView;

public class ShareLinkFragment extends DialogFragment implements OnClickListener
{
	private String grpId;
	
	private String grpName;
	
	private int grpSettings;
	
	public static final int OTHERS = -1;

	public static final int WA = -2;

	public static final int DELAY_MULTIPLIER = 1;

	public static final int NO_OF_RETRIES = 1;

	private int buttonClickedType;

	private boolean isNewGroup;

	private ProgressDialog mDialog;
	
	public final static String SHARE_LINK_FRAGMENT_TAG = "shareLinkFragmentTag";

	//private final String GROUP_ID = "groupId";

	//private final String TASK_STATUS = "taskStatus";

	private boolean isTaskRunning = false;
	
	private ShareLinkFragmentListener shareLinkFragmentListener;

	public static ShareLinkFragment newInstance(String groupId, String groupName, int groupSettings, boolean existingGroupChat)
	{
		ShareLinkFragment shareLinkFragment = new ShareLinkFragment();
		Bundle arguments = new Bundle();
		arguments.putBoolean(HikeConstants.Extras.EXISTING_GROUP_CHAT, existingGroupChat);
		arguments.putString(HikeConstants.Extras.CONVERSATION_ID, groupId);
		arguments.putString(HikeConstants.Extras.CONVERSATION_NAME, groupName);
		arguments.putInt(HikeConstants.Extras.CREATE_GROUP_SETTINGS, groupSettings);
		shareLinkFragment.setArguments(arguments);
		return shareLinkFragment;
	}

	public ShareLinkFragment()
	{
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.link_share_view, null);
		
		CustomFontTextView waText = (CustomFontTextView)parent.findViewById(R.id.share_via_WA);
		waText.setText(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.MENU_OPTION_FOR_GC_VIA_WA, R.string.watsapp));
		waText.setOnClickListener(this);
		
		CustomFontTextView otherSharableAppText = (CustomFontTextView)parent.findViewById(R.id.share_via_Others);
		otherSharableAppText.setText(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.MENU_OPTION_FOR_GC_VIA_WA, R.string.others));
		otherSharableAppText.setOnClickListener(this);

		parent.findViewById(R.id.add_via_Hike).setOnClickListener(this);
		
		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		
		return parent;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		if (getArguments() != null)
		{
			grpId = getArguments().getString(HikeConstants.Extras.CONVERSATION_ID, null);
			
			grpName = getArguments().getString(HikeConstants.Extras.CONVERSATION_NAME, null);

			grpSettings = getArguments().getInt(HikeConstants.Extras.CREATE_GROUP_SETTINGS, 0);
			
			isNewGroup = getArguments().getBoolean(HikeConstants.Extras.EXISTING_GROUP_CHAT, false);
		}

		/*if (savedInstanceState != null)
		{
			isTaskRunning = savedInstanceState.getBoolean(TASK_STATUS);
			if (isTaskRunning)
			{
				grpId = savedInstanceState.getString(GROUP_ID, grpId);
			}
		}*/

		boolean isWAInstalled = Utils.isPackageInstalled(getActivity(), HikeConstants.Extras.WHATSAPP_PACKAGE);
		boolean isAnyOtherShareableApp = Utils.getPackagesMatchingIntent(Intent.ACTION_SEND, null, "text/plain").size() > 0 ? true : false;

		if (isWAInstalled)
		{
			view.findViewById(R.id.share_via_WA).setVisibility(View.VISIBLE);
		}
		if (isAnyOtherShareableApp)
		{
			view.findViewById(R.id.share_via_Others).setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		try
		{
			this.shareLinkFragmentListener = (ShareLinkFragmentListener)activity;
		}
		catch (ClassCastException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.share_via_WA:
			buttonClickedType = WA;
			break;

		case R.id.share_via_Others:
			buttonClickedType = OTHERS;
			break;

		case R.id.add_via_Hike:
			shareLinkFragmentListener.addMembersViaHike();
			
			//dismiss the dialog
			dismiss();
			
			return;
			
		default:
			break;
		}

		// Start Loader here
		showProgressDialog();

		makeHttpCallForURL();
	}

	public void makeHttpCallForURL()
	{
		JSONObject json = new JSONObject();

		try
		{
			json.put(HikeConstants.Extras.CONVERSATION_ID, grpId);
			json.put(HikeConstants.Extras.CONVERSATION_NAME, grpName);
			json.put(HikeConstants.Extras.CREATE_GROUP_SETTINGS, grpSettings);
			json.put(HikeConstants.Extras.GROUP_CHAT_DP, OneToNConversationUtils.isGroupDPSet(grpId));
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		
		RequestToken token = HttpRequests.getShareLinkURLRequest(json, shareLinkURLReqListener, NO_OF_RETRIES, DELAY_MULTIPLIER);
		if (token != null && !token.isRequestRunning() && !isTaskRunning)
		{
			token.execute();
			isTaskRunning = true;
		}
		else
		{
			Logger.d(ShareLinkFragment.class.getSimpleName(), "As URL fetching task already running, so not starting new one");
		}
	}

	private IRequestListener shareLinkURLReqListener = new IRequestListener()
	{
		@Override
		public void onRequestSuccess(Response result)
		{
			if(!isAdded())
			{
				return;
			}
			
			final JSONObject response = (JSONObject) result.getBody().getContent();

			Logger.d(ShareLinkFragment.class.getSimpleName(), "responce from http call " + response);

			isTaskRunning = false;

			if (Utils.isResponseValid(response))
			{
				if (isNewGroup)
				{
					OneToNConversationUtils.createGroupOrBroadcast(getActivity(), new ArrayList<ContactInfo>(), grpName, grpId, grpSettings, true);
				}

				final String url = getLinkFromResponse(response);
				
				switch (buttonClickedType)
				{
				case WA:
					String str = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.TEXT_FOR_GC_VIA_WA,
							HikeMessengerApp.getInstance().getApplicationContext().getString(R.string.link_share_wa_msg))
							+ "\n " + url;
					openWA(str);
					break;

				case OTHERS:
					str = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.TEXT_FOR_GC_VIA_OTHERS, getString(R.string.link_share_others_msg))
					+ "\n " + url;
					ShareUtils.shareContent(HikeConstants.Extras.ShareTypes.TEXT_SHARE, str, null);
					break;

				default:
					break;
				}

				// Stop Loader here
				dismissProgressDialog();
				
				//dismiss Dialog
				dismiss();

			}
			
		}

		@Override
		public void onRequestProgressUpdate(float progress)
		{
			// Do nothing
		}

		@Override
		public void onRequestFailure(HttpException httpException)
		{
			Logger.d(ShareLinkFragment.class.getSimpleName(), "responce from http call failed " + httpException.toString());

			if(!isAdded())
			{
				return;
			}

			isTaskRunning = false;
			
			// Stop Loader here
			dismissProgressDialog();

			// Show Toast
			Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), getString(R.string.link_share_network_error), Toast.LENGTH_SHORT).show();

			//dismiss Dialog
			dismiss();
		}
	};

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		//outState.putString(GROUP_ID, grpId);
		//outState.putBoolean(TASK_STATUS, isTaskRunning);
	}
	
	/*@Override
	public void onStart()
	{
		super.onStart();
		//getDialog().getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
	}
	*/
	
	private void dismissProgressDialog()
	{
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}
	}

	private void showProgressDialog()
	{
		mDialog = ProgressDialog.show(getActivity(), null, null);
	}

	public interface ShareLinkFragmentListener
	{
		public void addMembersViaHike();
		
	}
	
	@Override
	public void onDestroyView()
	{
		Dialog dialog = getDialog();

	    if ((dialog != null) && getRetainInstance())
	    {
	        dialog.setDismissMessage(null);
	    }
		super.onDestroy();
	}

	/**
	 * Generates URL to share from response received from server
	 * 
	 * Example:- http://hike.in/<referral_id>:gc:<gc_id>
	 * @param response
	 * @return
	 */
	private String getLinkFromResponse(JSONObject response)
	{
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(HttpRequestConstants.BASE_LINK_SHARING_URL);
		Context mContext = HikeMessengerApp.getInstance().getApplicationContext();
		String utmSource = mContext.getSharedPreferences(HikeMessengerApp.REFERRAL, Context.MODE_PRIVATE).getString("utm_source", "");
		urlBuilder.append("/");
		urlBuilder.append(utmSource);
		urlBuilder.append(":gc:");
		try
		{
			String id = response.getString(HikeConstants.Extras.CONVERSATION_ID);
			urlBuilder.append(id);
		}
		catch(JSONException e)
		{
			
		}
		return urlBuilder.toString();
	}

	public void setButtonClickedType(int buttonClickedType)
	{
		this.buttonClickedType = buttonClickedType;
	}
	
	private void openWA(String str)
	{
		if (isStartedViaBot)
		{
			//Opens WA via adding Flag NEW_TASK as context is non Activity Context
			//Used in case when opened via clicking button in bot
			ShareUtils.shareContent(HikeConstants.Extras.ShareTypes.TEXT_SHARE, str, HikeConstants.Extras.WHATSAPP_PACKAGE);
		}
		else
		{
			IntentFactory.openInviteWatsApp(getActivity(), str);
		}
	}
}
