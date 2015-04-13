package com.bsb.hike.ui;

import java.util.ArrayList;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.AppConfig;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ImageViewerInfo;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.productpopup.DialogPojo;
import com.bsb.hike.productpopup.HikeDialogFragment;
import com.bsb.hike.productpopup.IActivityPopup;
import com.bsb.hike.productpopup.ProductContentModel;
import com.bsb.hike.productpopup.ProductInfoManager;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.ui.fragments.ImageViewerFragment;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

public class SettingsActivity extends HikeAppStateBaseFragmentActivity implements OnItemClickListener, OnClickListener
{
	private ContactInfo contactInfo;

	private String msisdn;

	private ImageView profileImgView;

	private ImageView statusMood;

	private TextView nameView;

	private TextView statusView;

	private String[] profilePubSubListeners = { HikePubSub.STATUS_MESSAGE_RECEIVED, HikePubSub.ICON_CHANGED, HikePubSub.PROFILE_UPDATE_FINISH };

	private boolean isConnectedAppsPresent;

	private enum ViewType
	{
		SETTINGS, VERSION
	};

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		ArrayList<String> items = new ArrayList<String>();

		items.add(getString(R.string.notifications));
		items.add(getString(R.string.settings_media));
		items.add(getString(R.string.settings_chat));
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(HikeConstants.FREE_SMS_PREF, true))
		{
			int credits = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getInt(HikeMessengerApp.SMS_SETTING, 0);
			items.add(getString(R.string.sms_with_credits, credits));
		}
		else
		{
			items.add(getString(R.string.sms));
		}

		// Check for connect apps in shared pref
		isConnectedAppsPresent = (!(TextUtils.isEmpty(HikeSharedPreferenceUtil.getInstance(HikeAuthActivity.AUTH_SHARED_PREF_NAME).getData(
				HikeAuthActivity.AUTH_SHARED_PREF_PKG_KEY, ""))));

		if (isConnectedAppsPresent)
		{
			items.add(getString(R.string.connected_apps));
		}
		items.add(getString(R.string.manage_account));
		items.add(getString(R.string.privacy));
		items.add(getString(R.string.help));
		items.add(null);

		final ArrayList<String> itemsSummary = new ArrayList<String>();

		itemsSummary.add(getString(R.string.notifications_hintext));
		itemsSummary.add(getString(R.string.media_settings_hinttext));
		itemsSummary.add(getString(R.string.sms_setting_hinttext));
		if (isConnectedAppsPresent)
		{
			itemsSummary.add(getString(R.string.connected_apps_hinttext));
		}
		itemsSummary.add(getString(R.string.account_hintttext));
		itemsSummary.add(getString(R.string.privacy_setting_hinttext));
		itemsSummary.add(getString(R.string.help_hinttext));

		final ArrayList<Integer> itemIcons = new ArrayList<Integer>();

		itemIcons.add(R.drawable.ic_notifications_settings);
		itemIcons.add(R.drawable.ic_auto_download_media_settings);
		itemIcons.add(R.drawable.ic_settings_chat);
		itemIcons.add(R.drawable.ic_sms_settings);
		if (isConnectedAppsPresent)
		{
			itemIcons.add(R.drawable.ic_conn_apps);
		}
		itemIcons.add(R.drawable.ic_account_settings);
		itemIcons.add(R.drawable.ic_privacy_settings);
		itemIcons.add(R.drawable.ic_help_settings);

		ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this, R.layout.setting_item, R.id.item, items)
		{

			@Override
			public int getItemViewType(int position)
			{
				if (getItem(position) == null)
				{
					return ViewType.VERSION.ordinal();
				}
				else
				{
					return ViewType.SETTINGS.ordinal();
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
					case SETTINGS:
						convertView = getLayoutInflater().inflate(R.layout.setting_item, null);
						break;

					case VERSION:
						convertView = getLayoutInflater().inflate(R.layout.app_version_item, parent, false);
						break;
					}
				}

				switch (viewType)
				{
				case SETTINGS:
					TextView header = (TextView) convertView.findViewById(R.id.item);
					TextView summary = (TextView) convertView.findViewById(R.id.summary);
					summary.setVisibility(View.GONE);
					ImageView iconImage = (ImageView) convertView.findViewById(R.id.icon);

					header.setText(getItem(position));
					iconImage.setImageResource(itemIcons.get(position));
					break;

				case VERSION:
					TextView appVersion = (TextView) convertView.findViewById(R.id.app_version);

					if (AppConfig.ALLOW_STAGING_TOGGLE)
					{
						LinearLayout ll_build_branch_version = (LinearLayout) convertView.findViewById(R.id.ll_commitId_branch_version);
						ll_build_branch_version.setVisibility(View.VISIBLE);
						TextView tv_build_number = (TextView) convertView.findViewById(R.id.tv_last_commit_hash);
						tv_build_number.setText(AppConfig.COMMIT_ID);

						TextView tv_branch_name = (TextView) convertView.findViewById(R.id.tv_branch_name);
						tv_branch_name.setText(AppConfig.BRANCH_NAME);
					}
					try
					{
						appVersion.setText(getString(R.string.app_version, getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
					}
					catch (NameNotFoundException e)
					{
						appVersion.setText("");
					}
					break;

				}
				return convertView;
			}

		};

		ListView settingsList = (ListView) findViewById(R.id.settings_content);
		addProfileHeaderView(settingsList);
		settingsList.setAdapter(listAdapter);
		settingsList.setOnItemClickListener(this);
		setupActionBar();

		HikeMessengerApp.getPubSub().addListeners(this, profilePubSubListeners);
		
		showProductPopup(ProductPopupsConstants.PopupTriggerPoints.SETTINGS_SCR.ordinal());
		
	}

	private void addProfileHeaderView(ListView settingsList)
	{
		View header = getLayoutInflater().inflate(R.layout.profile_header_other, null);
		header.findViewById(R.id.remove_fav).setVisibility(View.GONE);
		profileImgView = (ImageView) header.findViewById(R.id.profile_image);
		statusMood = (ImageView) header.findViewById(R.id.status_mood);
		nameView = (TextView) header.findViewById(R.id.name);
		statusView = (TextView) header.findViewById(R.id.subtext);
		contactInfo = Utils.getUserContactInfo(getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE));
		msisdn = contactInfo.getMsisdn();

		String infoSubText = getString(Utils.isLastSeenSetToFavorite() ? R.string.both_ls_status_update : R.string.status_updates_proper_casing);
		((TextView) header.findViewById(R.id.update_text)).setText(getString(R.string.add_fav_msg, infoSubText));
		// set name and status
		setNameInHeader(nameView);

		addProfileImgInHeader();

		addStatusInHeader();

		settingsList.addHeaderView(header, null, false);
	}

	private void setNameInHeader(TextView nameTextView)
	{
		// TODO Auto-generated method stub
		nameTextView.setText(contactInfo.getNameOrMsisdn());
	}

	public void onViewImageClicked(View v)
	{
		ImageViewerInfo imageViewerInfo = (ImageViewerInfo) v.getTag();

		String mappedId = imageViewerInfo.mappedId;
		String url = imageViewerInfo.url;

		Bundle arguments = new Bundle();
		arguments.putString(HikeConstants.Extras.MAPPED_ID, mappedId);
		arguments.putString(HikeConstants.Extras.URL, url);
		arguments.putBoolean(HikeConstants.Extras.IS_STATUS_IMAGE, imageViewerInfo.isStatusMessage);

		HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_IMAGE, arguments);
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.settings);
		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		if (isConnectedAppsPresent)
		{
			switch (position)
			{
			case 1:
				IntentFactory.openSettingNotification(this);
				break;
			case 2:
				IntentFactory.openSettingMedia(this);
				break;
			case 3:
				IntentFactory.openSettingChat(this);
				break;
			case 4:
				IntentFactory.openSettingSMS(this);
				break;
			case 5:
				IntentFactory.openConnectedApps(this);
				break;
			case 6:
				IntentFactory.openSettingAccount(this);
				break;
			case 7:
				HAManager.logClickEvent(HikeConstants.LogEvent.PRIVACY_SETTING_CLICKED);
				IntentFactory.openSettingPrivacy(this);
				break;
			case 8:
				IntentFactory.openSettingHelp(this);
				break;
			}
		}
		else
		{
			switch (position)
			{
			case 1:
				IntentFactory.openSettingNotification(this);
				break;
			case 2:
				IntentFactory.openSettingMedia(this);
				break;
			case 3:
				IntentFactory.openSettingChat(this);
				break;
			case 4:
				IntentFactory.openSettingSMS(this);
				break;
			case 5:
				IntentFactory.openSettingAccount(this);
				break;
			case 6:
				HAManager.logClickEvent(HikeConstants.LogEvent.PRIVACY_SETTING_CLICKED);
				IntentFactory.openSettingPrivacy(this);
				break;
			case 7:
				IntentFactory.openSettingHelp(this);
				break;
			}
		}
	}

	public void onBackPressed()
	{
		if (removeFragment(HikeConstants.IMAGE_FRAGMENT_TAG))
		{
			return;
		}
		super.onBackPressed();
	}

	@Override
	public boolean removeFragment(String tag)
	{
		boolean isRemoved = super.removeFragment(tag);

		if (isRemoved)
		{
			getSupportActionBar().show();
			setupActionBar();
		}
		return isRemoved;
	}

	private void addProfileImgInHeader()
	{
		// set profile picture
		Drawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(contactInfo.getMsisdn());
		if (drawable == null)
		{
			drawable = HikeMessengerApp.getLruCache().getDefaultAvatar(contactInfo.getMsisdn(), false);
		}
		profileImgView.setImageDrawable(drawable);
		
		ImageViewerInfo imageViewerInfo = new ImageViewerInfo(contactInfo.getMsisdn() + ProfileActivity.PROFILE_PIC_SUFFIX, null, false, !ContactManager.getInstance().hasIcon(
				contactInfo.getMsisdn()));
		profileImgView.setTag(imageViewerInfo);
	}

	private void addStatusInHeader()
	{
		// get hike status
		StatusMessageType[] statusMessagesTypesToFetch = { StatusMessageType.TEXT };
		StatusMessage status = HikeConversationsDatabase.getInstance().getLastStatusMessage(statusMessagesTypesToFetch, contactInfo);

		if (status != null)
		{
			if (status.hasMood())
			{
				statusMood.setVisibility(View.VISIBLE);
				statusMood.setImageResource(EmoticonConstants.moodMapping.get(status.getMoodId()));
			}
			else
			{
				statusMood.setVisibility(View.GONE);
			}
			statusView.setText(SmileyParser.getInstance().addSmileySpans(status.getText(), true));
		}
		else
		{
			status = new StatusMessage(HikeConstants.JOINED_HIKE_STATUS_ID, null, contactInfo.getMsisdn(), contactInfo.getName(), getString(R.string.joined_hike_update),
					StatusMessageType.JOINED_HIKE, contactInfo.getHikeJoinTime());

			if (status.getTimeStamp() == 0)
			{
				statusView.setText(status.getText());
			}
			else
			{
				statusView.setText(status.getText() + " " + status.getTimestampFormatted(true, SettingsActivity.this));
			}
		}
	}

	@Override
	public void onEventReceived(final String type, Object object)
	{
		super.onEventReceived(type, object);

		if (contactInfo.getMsisdn() == null)
		{
			return;
		}
		else if (HikePubSub.ICON_CHANGED.equals(type))
		{
			if (msisdn.equals((String) object))
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						addProfileImgInHeader();
					}
				});
			}
		}
		else if (HikePubSub.STATUS_MESSAGE_RECEIVED.equals(type))
		{
			StatusMessage status = (StatusMessage) object;

			if (status.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
			{
				return;
			}

			if (status.getMsisdn().equals(msisdn))
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						addStatusInHeader();
					}
				});
			}
		}
		else if (HikePubSub.PROFILE_UPDATE_FINISH.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					String name = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(HikeMessengerApp.NAME_SETTING, contactInfo.getNameOrMsisdn());
					contactInfo.setName(name);
					setNameInHeader(nameView);
				}
			});
		}
	}

	@Override
	protected void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, profilePubSubListeners);

		super.onDestroy();
	}

	@Override
	public void onClick(View v)
	{
		Intent intent = new Intent(SettingsActivity.this, ProfileActivity.class);
		startActivity(intent);
	}

	public void openTimeline(View v)
	{
		Intent intent = new Intent();
		intent.setClass(SettingsActivity.this, ProfileActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}
	
	@Override
	protected void openImageViewerFragment(Object object)
	{
		/*
		 * Making sure we don't add the fragment if the activity is finishing.
		 */
		if (isFinishing())
		{
			return;
		}

		Bundle arguments = (Bundle) object;

		ImageViewerFragment imageViewerFragment = new ImageViewerFragment();
		imageViewerFragment.setArguments(arguments);

		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		fragmentTransaction.add(R.id.parent_layout, imageViewerFragment, HikeConstants.IMAGE_FRAGMENT_TAG);
		fragmentTransaction.commitAllowingStateLoss();
	}
	
}
