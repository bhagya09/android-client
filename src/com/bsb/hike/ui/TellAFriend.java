package com.bsb.hike.ui;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.productpopup.DialogPojo;
import com.bsb.hike.productpopup.HikeDialogFragment;
import com.bsb.hike.productpopup.IActivityPopup;
import com.bsb.hike.productpopup.ProductContentModel;
import com.bsb.hike.productpopup.ProductInfoManager;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class TellAFriend extends HikeAppStateBaseFragmentActivity implements Listener, OnItemClickListener
{

	private static final int SMS = 1;

	//private static final int FACEBOOK = 2;

	//private static final int TWITTER = 3;

	private static final int EMAIL = 4;

	private static final int OTHER = 5;

	private static final int WATSAPP = 6;

	private static final int INVITE_EXTRA = 7;

	private SharedPreferences settings;

	private String[] pubSubListeners = { HikePubSub.SOCIAL_AUTH_COMPLETED, HikePubSub.DISMISS_POSTING_DIALOG };

	private ProgressDialog progressDialog;

	boolean pickFriendsWhenSessionOpened;

	private enum ViewType
	{
		ITEM, EXTRA
	};

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings);

		settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
		boolean watsAppPresent = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.WATSAPP_INVITE_ENABLED, true)
				&& Utils.isPackageInstalled(getApplicationContext(), HikeConstants.PACKAGE_WATSAPP);
		ArrayList<String> items = new ArrayList<String>();
		items.add(getString(!HikeMessengerApp.isIndianUser() || settings.getBoolean(HikeMessengerApp.SEND_NATIVE_INVITE, false) ? R.string.sms : R.string.free_sms_txt));
		if (watsAppPresent)
		{
			items.add(getString(R.string.invite_with_watsapp));
		}
		//items.add(getString(R.string.facebook));
		//items.add(getString(R.string.twitter));
		items.add(getString(R.string.email));
		items.add(getString(R.string.share_via_other));
		items.add(getString(R.string.invite_friends));
		final ArrayList<Integer> itemIcons = new ArrayList<Integer>();
		itemIcons.add(R.drawable.ic_invite_sms);
		if (watsAppPresent)
		{
			itemIcons.add(R.drawable.ic_whatsapp);
		}
		//itemIcons.add(R.drawable.ic_invite_fb);
		//itemIcons.add(R.drawable.ic_invite_twitter);
		itemIcons.add(R.drawable.ic_invite_email);
		itemIcons.add(R.drawable.ic_invite_other);

		// we could do with objects as well , that would be best but big change
		final ArrayList<Integer> itemTags = new ArrayList<Integer>();
		itemTags.add(SMS);
		if (watsAppPresent)
		{
			itemTags.add(WATSAPP);
		}
		//itemTags.add(FACEBOOK);
		//itemTags.add(TWITTER);
		itemTags.add(EMAIL);
		itemTags.add(OTHER);
		itemTags.add(INVITE_EXTRA);
		ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this, R.layout.setting_item, R.id.item, items)
		{

			public int getItemViewType(int position)
			{
				if (getString(R.string.invite_friends).equals(getItem(position)))
				{
					return ViewType.EXTRA.ordinal();
				}
				else
				{
					return ViewType.ITEM.ordinal();
				}
			}

			@Override
			public int getViewTypeCount()
			{
				return ViewType.values().length;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				ViewType viewType = ViewType.values()[getItemViewType(position)];
				if (convertView == null)
				{
					switch (viewType)
					{
					case ITEM:
						convertView = getLayoutInflater().inflate(R.layout.setting_item, null);
						break;

					case EXTRA:
						convertView = getLayoutInflater().inflate(R.layout.free_sms_item, null);
						break;
					}
				}
				switch (viewType)
				{
				case ITEM:
					TextView itemText = (TextView) convertView.findViewById(R.id.item);
					TextView tv = (TextView) convertView.findViewById(R.id.summary);

					itemText.setText(getItem(position));
					tv.setVisibility(View.GONE);
					ImageView iconImage = (ImageView) convertView.findViewById(R.id.icon);
					iconImage.setImageResource(itemIcons.get(position));
					break;

				case EXTRA:
					TextView text = (TextView) convertView.findViewById(R.id.item);
					TextView subText = (TextView) convertView.findViewById(R.id.summary);

					text.setText(R.string.invite_friends);
					subText.setText(R.string.invite_subtext);
					break;
				}
				convertView.setTag(itemTags.get(position));
				return convertView;
			}

		};

		ListView settingsList = (ListView) findViewById(R.id.settings_content);
		settingsList.setAdapter(listAdapter);
		settingsList.setOnItemClickListener(this);

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);

		if (savedInstanceState != null)
		{
			if (savedInstanceState.getBoolean(HikeConstants.Extras.DIALOG_SHOWING))
			{
				progressDialog = ProgressDialog.show(this, null, getString(R.string.posting_update));
			}
		}
		setupActionBar();
		showProductPopup(ProductPopupsConstants.PopupTriggerPoints.INVITEFRNDS.ordinal());
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putBoolean(HikeConstants.Extras.DIALOG_SHOWING, progressDialog != null && progressDialog.isShowing());
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onDestroy()
	{
		if (progressDialog != null)
		{
			progressDialog.dismiss();
			progressDialog = null;
		}
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		/*if (requestCode == HikeConstants.FACEBOOK_REQUEST_CODE)
		{
			Session session = Session.getActiveSession();
			if (session != null && resultCode == RESULT_OK)
			{
				session.onActivityResult(this, requestCode, resultCode, data);
			}
			else if (session != null && resultCode == RESULT_CANCELED)
			{
				Logger.d("TellAFriend", "Facebook Permission Cancelled");
				session.closeAndClearTokenInformation();
				Session.setActiveSession(null);
			}
		}*/
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		/*if (HikePubSub.SOCIAL_AUTH_COMPLETED.equals(type))
		{
			final boolean facebook = (Boolean) object;
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if (facebook)
					{
						onClickPickFriends();
					}
					else
					{
						// twitter
						postToSocialNetwork(false);
					}
				}
			});
		}
		else*/ if (HikePubSub.DISMISS_POSTING_DIALOG.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if (progressDialog != null)
					{
						progressDialog.dismiss();
						progressDialog = null;
					}
				}
			});
		}
	}

	/*private void onClickPickFriends()
	{
		startPickFriendsActivity();
	}

	private void startPickFriendsActivity()
	{
		if (ensureOpenSession())
		{
			Intent intent = new Intent(this, SocialNetInviteActivity.class);
			intent.putExtra(HikeConstants.Extras.IS_FACEBOOK, true);
			Logger.d("tell a friend", "calling socialNetInviteActivity");
			startActivity(intent);
		}
		else
		{
			pickFriendsWhenSessionOpened = true;
		}
	}

	private boolean ensureOpenSession()
	{
		Logger.d("ensure Open Session", "entered in ensureOpenSession");

		if (Session.getActiveSession() == null || !Session.getActiveSession().isOpened())
		{

			Logger.d("ensure Open Session", "active session is either null or closed");
			Session.openActiveSession(this, true, new Session.StatusCallback()
			{
				@Override
				public void call(Session session, SessionState state, Exception exception)
				{
					onSessionStateChanged(session, state, exception);
				}
			});
			return false;
		}

		return true;
	}

	private void onSessionStateChanged(Session session, SessionState state, Exception exception)
	{
		Logger.d("calling session change ", "inside onSessionStateChanged");
		if (pickFriendsWhenSessionOpened && state.isOpened())
		{
			pickFriendsWhenSessionOpened = false;
			startPickFriendsActivity();
		}
	}

	private void postToSocialNetwork(final boolean facebook)
	{
		HikeHttpRequest hikeHttpRequest = new HikeHttpRequest("/account/spread", RequestType.SOCIAL_POST, new HikeHttpCallback()
		{

			@Override
			public void onSuccess(JSONObject response)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.DISMISS_POSTING_DIALOG, null);
				parseResponse(response, facebook);
			}

			@Override
			public void onFailure()
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.DISMISS_POSTING_DIALOG, null);
				Toast.makeText(getApplicationContext(), R.string.posting_update_fail, Toast.LENGTH_SHORT).show();
			}

		});
		JSONObject data = new JSONObject();
		try
		{
			data.put(facebook ? HikeConstants.FACEBOOK_STATUS : HikeConstants.TWITTER_STATUS, true);
			hikeHttpRequest.setJSONData(data);
			Logger.d(getClass().getSimpleName(), "JSON: " + data);

			progressDialog = ProgressDialog.show(this, null, getString(facebook ? R.string.posting_update_facebook : R.string.posting_update_twitter));

			HikeHTTPTask hikeHTTPTask = new HikeHTTPTask(null, 0);
			Utils.executeHttpTask(hikeHTTPTask, hikeHttpRequest);
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), "Invalid JSON", e);
		}
	}

	private void parseResponse(JSONObject response, boolean facebook)
	{
		String responseString = response.optString(facebook ? HikeConstants.FACEBOOK_STATUS : HikeConstants.TWITTER_STATUS);

		if (TextUtils.isEmpty(responseString))
		{
			return;
		}

		if (HikeConstants.SocialPostResponse.SUCCESS.equals(responseString))
		{
			Toast.makeText(getApplicationContext(), R.string.posted_update, Toast.LENGTH_SHORT).show();
		}
		else if (HikeConstants.SocialPostResponse.FAILURE.equals(responseString))
		{
			Toast.makeText(getApplicationContext(), R.string.posting_update_fail, Toast.LENGTH_SHORT).show();
		}
		else
		{
			Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
			if (facebook)
			{
				editor.remove(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE);
				editor.remove(HikeMessengerApp.FACEBOOK_TOKEN);
				editor.remove(HikeMessengerApp.FACEBOOK_TOKEN_EXPIRES);
				editor.remove(HikeMessengerApp.FACEBOOK_USER_ID);
			}
			else
			{
				editor.remove(HikeMessengerApp.TWITTER_AUTH_COMPLETE);
				editor.remove(HikeMessengerApp.TWITTER_TOKEN);
				editor.remove(HikeMessengerApp.TWITTER_TOKEN_SECRET);
			}
			editor.commit();
			onItemClick(null, null, facebook ? 1 : 2, 0);
		}
	}*/

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.invite_friends);
		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(TellAFriend.this, HomeActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		int tag = (Integer) view.getTag();
		
		try
		{
			JSONObject metadata = new JSONObject();
			
			switch (tag)
			{
			case SMS:
				Utils.logEvent(this, HikeConstants.LogEvent.INVITE_BUTTON_CLICKED);
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.INVITE_SMS_SCREEN_FROM_INVITE);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				IntentManager.openInviteSMS(this);
				break;
			case WATSAPP:
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.WATS_APP_INVITE);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);	
				sendInviteViaWatsApp();
				break;
			/*case FACEBOOK:
				onClickPickFriends();
				break;
	
			case TWITTER:
				if (!settings.getBoolean(HikeMessengerApp.TWITTER_AUTH_COMPLETE, false))
				{
					startActivity(new Intent(this, TwitterAuthActivity.class));
				}
				else
				{
					postToSocialNetwork(false);
				}
				break;*/
	
			case EMAIL:
				Intent mailIntent = new Intent(Intent.ACTION_SENDTO);
	
				mailIntent.setData(Uri.parse("mailto:"));
				mailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));
				mailIntent.putExtra(Intent.EXTRA_TEXT, Utils.getInviteMessage(this, R.string.email_body));
	
				startActivity(mailIntent);
				break;
	
			case OTHER:
				Utils.logEvent(this, HikeConstants.LogEvent.DRAWER_INVITE);
				Utils.startShareIntent(this, Utils.getInviteMessage(this, R.string.invite_share_message));
				break;
			}
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}

	private void sendInviteViaWatsApp()
	{
		IntentManager.openInviteWatsApp(this);
	}
}
