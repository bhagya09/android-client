package com.bsb.hike.ui.fragments;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
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

	private final int OTHERS = -1;

	public final static int WA = -2;

	private final int DELAY_MULTIPLIER = 1;

	private final int NO_OF_RETRIES = 1;

	private final String GROUP_ID = "gid";

	private final String GROUP_NAME = "name";

	private final String GROUP_SETTINGS = "gs";

	private final String GROUP_ID_BY_SERVER = "gc_id";

	private int buttonClickedType;

	private boolean isNewGroup;

	private ProgressBar mDialog;
	
	private LinearLayout menuContainer;

	public final static String SHARE_LINK_FRAGMENT_TAG = "shareLinkFragmentTag";

	private ShareLinkFragmentListener shareLinkFragmentListener;

	private boolean isStartedViaBot = false;
	
	private final byte TASK_DEFAULT = -1;
	
	private final byte TASK_COMPLETE = 1;

	private final byte TASK_FAILED = 2;

	private final byte TASK_INPROGRESS = 3;
	
	private byte mTaskStatus = TASK_DEFAULT;
	
	private static Context appContext = HikeMessengerApp.getInstance().getApplicationContext();
	
	private static final String TASK_STATUS_KEY = "tsk";
	
	public static ShareLinkFragment newInstance(String groupId, String groupName, int groupSettings, boolean existingGroupChat, boolean isStartedViaBot)
	{
		ShareLinkFragment shareLinkFragment = new ShareLinkFragment();
		Bundle arguments = new Bundle();
		arguments.putBoolean(HikeConstants.Extras.EXISTING_GROUP_CHAT, existingGroupChat);
		arguments.putString(HikeConstants.Extras.CONVERSATION_ID, groupId);
		arguments.putString(HikeConstants.Extras.CONVERSATION_NAME, groupName);
		arguments.putInt(HikeConstants.Extras.CREATE_GROUP_SETTINGS, groupSettings);
		arguments.putBoolean(HikeConstants.Extras.CREATE_GROUP_SRC, isStartedViaBot);
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
		
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.link_share_view, null);
		parent.setPadding(0, appContext.getResources().getDimensionPixelSize(R.dimen.menu_list_padding_top), 0, appContext.getResources().getDimensionPixelSize(R.dimen.menu_list_padding_bottom));
		
		CustomFontTextView waText = (CustomFontTextView) parent.findViewById(R.id.share_via_WA);
		waText.setText(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.MENU_OPTION_FOR_GC_VIA_WA, appContext.getString(R.string.watsapp)));
		waText.setOnClickListener(this);

		CustomFontTextView otherSharableAppText = (CustomFontTextView) parent.findViewById(R.id.share_via_Others);
		otherSharableAppText.setText(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.MENU_OPTIONS_FOR_GC_VIA_OTHERS, appContext.getString(R.string.others)));
		otherSharableAppText.setOnClickListener(this);

		parent.findViewById(R.id.add_via_Hike).setOnClickListener(this);

		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		
		mDialog = (ProgressBar)parent.findViewById(R.id.app_open_loader);

		menuContainer = (LinearLayout)parent.findViewById(R.id.menu_container);
		
		return parent;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		initViaArguments();

		boolean isWAInstalled = Utils.isPackageInstalled(getActivity(), HikeConstants.Extras.WHATSAPP_PACKAGE);
		boolean isAnyOtherShareableApp = Utils.getPackagesMatchingIntent(Intent.ACTION_SEND, null, "text/plain").size() > 0 ? true : false;

		boolean toShowWAMenuItem = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ENABLE_MENU_OPTION_FOR_GC_VIA_WA, false);
		boolean toShowOtherShareableAppMenuItem = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ENABLE_MENU_OPTIONS_FOR_GC_VIA_OTHERS, false);
		
		if (isWAInstalled && toShowWAMenuItem)
		{
			view.findViewById(R.id.share_via_WA).setVisibility(View.VISIBLE);
		}
		if (isAnyOtherShareableApp && toShowOtherShareableAppMenuItem)
		{
			view.findViewById(R.id.share_via_Others).setVisibility(View.VISIBLE);
		}
		
		if(savedInstanceState != null)
		{
			mTaskStatus = savedInstanceState.getByte(TASK_STATUS_KEY) ;
			
			if(mTaskStatus == TASK_INPROGRESS)
			{
				showProgressDialog();
			}
		}
	}

	public void initViaArguments()
	{
		if (getArguments() != null)
		{
			grpId = getArguments().getString(HikeConstants.Extras.CONVERSATION_ID, null);

			grpName = getArguments().getString(HikeConstants.Extras.CONVERSATION_NAME, null);

			grpSettings = getArguments().getInt(HikeConstants.Extras.CREATE_GROUP_SETTINGS, 0);

			isNewGroup = getArguments().getBoolean(HikeConstants.Extras.EXISTING_GROUP_CHAT, false);
			
			isStartedViaBot = getArguments().getBoolean(HikeConstants.Extras.CREATE_GROUP_SRC, false);
		}
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		try
		{
			this.shareLinkFragmentListener = (ShareLinkFragmentListener) activity;
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
			recordShareLinkDialogOptionClicked(AnalyticsConstants.JoinGroupViaLinkSharingAnalyticsConstants.CLICKED_WA);
			break;

		case R.id.share_via_Others:
			buttonClickedType = OTHERS;
			recordShareLinkDialogOptionClicked(AnalyticsConstants.JoinGroupViaLinkSharingAnalyticsConstants.CLICKED_OTHER);
			break;

		case R.id.add_via_Hike:
			shareLinkFragmentListener.addMembersViaHike();
			recordShareLinkDialogOptionClicked(AnalyticsConstants.JoinGroupViaLinkSharingAnalyticsConstants.CLICKED_HIKE);
			// dismiss the dialog
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
			json.put(GROUP_ID, grpId);
			json.put(GROUP_NAME, grpName);
			json.put(GROUP_SETTINGS, grpSettings);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		RequestToken token = HttpRequests.getShareLinkURLRequest(json, shareLinkURLReqListener, NO_OF_RETRIES, DELAY_MULTIPLIER);
		if (token != null && !token.isRequestRunning() && mTaskStatus != TASK_INPROGRESS)
		{
			token.execute();
			mTaskStatus = TASK_INPROGRESS;
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
			final JSONObject response = (JSONObject) result.getBody().getContent();

			Logger.d(ShareLinkFragment.class.getSimpleName(), "responce from http call " + response);

			if (Utils.isResponseValid(response))
			{
				if (isNewGroup)
				{
					if (isStartedViaBot)
					{
						OneToNConversationUtils.createNewShareGroupViaServerSentCard(grpName, grpId, grpSettings, true);
					}
					else
					{
						if(isAdded())
						{
							OneToNConversationUtils.createGroupOrBroadcast(getActivity(), new ArrayList<ContactInfo>(), grpName, grpId, grpSettings, true);
							if(OneToNConversationUtils.isGroupDPSetWhileCreatingGroup(grpId))
							{
								OneToNConversationUtils.uploadGroupProfileImage(grpId);
							}

							recordGroupCreation();
						}
						else
						{
							Logger.d(ShareLinkFragment.class.getSimpleName(), "New group call and fragment not added so no group created, so returning from here");
							return;
						}
					}
				}

				openThirdPartyApp(response);
				
				mTaskStatus = TASK_COMPLETE;

				if (isAdded() && isVisible())
				{
					closeDialog();
				}
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

			// Show Toast
			Toast.makeText(appContext,appContext.getString(R.string.link_share_network_error), Toast.LENGTH_SHORT).show();
			
			mTaskStatus = TASK_FAILED;
					
			if (isAdded() && isVisible())
			{
				closeDialog();
			}
			
		}
	};

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putByte(TASK_STATUS_KEY, mTaskStatus);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		
		if (isAdded() && isVisible() && 
				(mTaskStatus == TASK_COMPLETE || mTaskStatus == TASK_FAILED)) 
		{
			closeDialog();
		}
	}

	private void closeDialog()
	{
		// Stop Loader here
		dismissProgressDialog();

		// dismiss Dialog
		android.support.v4.app.FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.remove(this);

		ft.commitAllowingStateLoss();

		fm.executePendingTransactions();

	}
	
	private void dismissProgressDialog()
	{
		mDialog.setVisibility(View.GONE);
	}

	private void showProgressDialog()
	{
		mDialog.setVisibility(View.VISIBLE);
		menuContainer.setVisibility(View.INVISIBLE);
		
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
	        dialog.setDismissMessage(null);
	    
		super.onDestroyView();
		
	}
	
	/**
	 * Generates URL to share from response received from server
	 * 
	 * Example:- http://hike.in/<referral_id>:gc:<gc_id>
	 * 
	 * @param response
	 * @return
	 */
	private String getLinkFromResponse(JSONObject response)
	{
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(HttpRequestConstants.BASE_LINK_SHARING_URL);
		Context mContext = appContext;
		String inviteToken = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeConstants.INVITE_TOKEN, "");
		urlBuilder.append("/");
		urlBuilder.append(inviteToken);
		urlBuilder.append(":gc:");
		try
		{
			String id = response.getString(GROUP_ID_BY_SERVER);
			urlBuilder.append(id);
		}
		catch (JSONException e)
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
			if(isAdded())
			{
				IntentFactory.openInviteWatsApp(getActivity(), str);
			}
		}
	}
	
	private void openThirdPartyApp(final JSONObject response)
	{
		final String url = getLinkFromResponse(response);

		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHARE_LINK_URL_FOR_GC, url);
		
		switch (buttonClickedType)
		{
		case WA:
			if(Utils.isPackageInstalled(appContext, HikeConstants.PACKAGE_WATSAPP))
			{
				String str = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.TEXT_FOR_GC_VIA_WA,
						appContext.getString(R.string.link_share_wa_msg))
						+ "\n " + url;
				str = str.replace("$groupname", grpName);
				openWA(str);
			}
			break;

		case OTHERS:
			String str = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.TEXT_FOR_GC_VIA_OTHERS, 
					appContext.getString(R.string.link_share_others_msg))
			+ "\n " + url;
			str = str.replace("$groupname", grpName);
			ShareUtils.shareContent(HikeConstants.Extras.ShareTypes.TEXT_SHARE, str, null);
			break;

		default:
			break;
		}
	}

	private void recordShareLinkDialogOptionClicked(String uk)
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.V2.UNIQUE_KEY, uk);
			json.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.ACT_EXPERIMENT);
			json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.JoinGroupViaLinkSharingAnalyticsConstants.WA);
			json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.JoinGroupViaLinkSharingAnalyticsConstants.FUNNEL);
			json.put(AnalyticsConstants.V2.ORDER, AnalyticsConstants.JoinGroupViaLinkSharingAnalyticsConstants.CLICKED_ADD);

			HAManager.getInstance().recordV2(json);
		}

		catch (JSONException e)
		{
			e.toString();
		}
	}

	private void recordGroupCreation()
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.V2.UNIQUE_KEY, AnalyticsConstants.JoinGroupViaLinkSharingAnalyticsConstants.GROUP_CREATE);
			json.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.JoinGroupViaLinkSharingAnalyticsConstants.ACT_GROUP);
			json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.JoinGroupViaLinkSharingAnalyticsConstants.WA);
			json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.JoinGroupViaLinkSharingAnalyticsConstants.FUNNEL);
			json.put(AnalyticsConstants.V2.ORDER, AnalyticsConstants.JoinGroupViaLinkSharingAnalyticsConstants.GROUP_CREATE);

			HAManager.getInstance().recordV2(json);
		}

		catch (JSONException e)
		{
			e.toString();
		}
	}

}
