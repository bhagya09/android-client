package com.bsb.hike.ui;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
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

import com.bsb.hike.AppConfig;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.chatHead.StickyCaller;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ImageViewerInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.ui.fragments.ImageViewerFragment;
import com.bsb.hike.utils.ChangeProfileImageBaseActivity;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

public class SettingsActivity extends ChangeProfileImageBaseActivity implements OnItemClickListener, OnClickListener, android.content.DialogInterface.OnClickListener
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
	
	private static class ViewHolder
	{
		TextView header;

		TextView summary;
		
		TextView descText;

		ImageView imageView;

		int id = -1;
	}
	
	private static class SettingsDisplayPojo
	{
		String text;
		int drawableResId;
		int id;
		String descText = "";
		
		public SettingsDisplayPojo(String text, int id, int drawableResId)
		{
			this.text = text;
			this.id = id;
			this.drawableResId = drawableResId;
		}
	}

	private static final float SCALE_FACTOR = 0.6f;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		ArrayList<SettingsDisplayPojo> items = new ArrayList<SettingsDisplayPojo>();

		items.add(new SettingsDisplayPojo(getString(R.string.notifications), R.string.notifications, R.drawable.ic_notifications_settings));
		items.add(new SettingsDisplayPojo(getString(R.string.settings_media), R.string.settings_media, R.drawable.ic_auto_download_media_settings));
		
		items.add(new SettingsDisplayPojo(getString(R.string.settings_chat), R.string.settings_chat, R.drawable.ic_settings_chat));
		items.add(new SettingsDisplayPojo(getString(R.string.settings_sticker), R.string.settings_sticker, R.drawable.ic_settings_sticker));

		if (HikeMessengerApp.isLocalisationEnabled())
		{
			items.add(new SettingsDisplayPojo(getString(R.string.language), R.string.language, R.drawable.ic_settings_languages));
		}
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(HikeConstants.FREE_SMS_PREF, true))
		{
			int credits = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getInt(HikeMessengerApp.SMS_SETTING, 0);
			SettingsDisplayPojo settingsPojo = new SettingsDisplayPojo(getString(R.string.sms_with_settings), R.string.sms_with_settings, R.drawable.ic_sms_settings);
			settingsPojo.descText = getString(R.string.sms_credits_with_settings, credits);
			items.add(settingsPojo);
		}
		else
		{
			items.add(new SettingsDisplayPojo(getString(R.string.sms), R.string.sms, R.drawable.ic_sms_settings));
		}

		// Check for connect apps in shared pref
		isConnectedAppsPresent = (!(TextUtils.isEmpty(HikeSharedPreferenceUtil.getInstance(HikeAuthActivity.AUTH_SHARED_PREF_NAME).getData(
				HikeAuthActivity.AUTH_SHARED_PREF_PKG_KEY, ""))));

		if (isConnectedAppsPresent)
		{
			items.add(new SettingsDisplayPojo(getString(R.string.connected_apps), R.string.connected_apps, R.drawable.ic_conn_apps));
		}
		items.add(new SettingsDisplayPojo(getString(R.string.manage_account), R.string.manage_account, R.drawable.ic_account_settings));
		items.add(new SettingsDisplayPojo(getString(R.string.privacy), R.string.privacy, R.drawable.ic_privacy_settings));
    	if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.ENABLE, false) && ChatHeadUtils.areWhitelistedPackagesSharable(this))
		{
			items.add(new SettingsDisplayPojo(getString(R.string.settings_share_stickers), R.string.settings_share_stickers, R.drawable.settings_icon_sticker_widget));
		}
		if (HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_STICKY_CALLER, false))
		{
			items.add(new SettingsDisplayPojo(getString(R.string.sticky_caller_settings), R.string.sticky_caller_settings, R.drawable.sticky_caller_settings));
		}
    	items.add(new SettingsDisplayPojo(getString(R.string.help), R.string.help, R.drawable.ic_help_settings));
		
		//Last item is being added as null for the app version TextView
		items.add(null);
		
		ArrayAdapter<SettingsDisplayPojo> listAdapter = new ArrayAdapter<SettingsDisplayPojo>(this, R.layout.setting_item, R.id.item, items)
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
				ViewHolder viewHolder = null;
				
				if (convertView == null)
				{
					switch (viewType)
					{
					case SETTINGS:
						convertView = getLayoutInflater().inflate(R.layout.setting_item, null);
						viewHolder = new ViewHolder();
						viewHolder.header = (TextView) convertView.findViewById(R.id.item);
						viewHolder.summary = (TextView) convertView.findViewById(R.id.summary);
						viewHolder.imageView = (ImageView) convertView.findViewById(R.id.icon);
						viewHolder.descText = (TextView) convertView.findViewById(R.id.item_desc);
						convertView.setTag(viewHolder);
						break;

					case VERSION:
						convertView = getLayoutInflater().inflate(R.layout.app_version_item, parent, false);
						convertView.setTag(null);
						break;
					}
				}
				
				else
				{
					viewHolder = (ViewHolder) convertView.getTag();
				}

				switch (viewType)
				{
				case SETTINGS:
					viewHolder.summary.setVisibility(View.GONE);
					SettingsDisplayPojo settingsObj = getItem(position);
					viewHolder.header.setText(settingsObj.text);
					viewHolder.imageView.setImageResource(settingsObj.drawableResId);
					viewHolder.id = (settingsObj.id);
					
					if (TextUtils.isEmpty(settingsObj.descText))
					{
						viewHolder.descText.setVisibility(View.GONE);
					}
					else
					{
						viewHolder.descText.setVisibility(View.VISIBLE);
						viewHolder.descText.setText(settingsObj.descText);
					}
					break;

				case VERSION:
					TextView appVersion = (TextView) convertView.findViewById(R.id.app_version);
					TextView withLove = (TextView) convertView.findViewById(R.id.with_love);

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

					//Made with Love In India
					String madeWithLove = getString(R.string.made_with_love_in_india);
					int heartStartIndex = madeWithLove.indexOf(':');
					int heartLastIndex = madeWithLove.lastIndexOf(':') + 1;
					Editable editable = new SpannableStringBuilder(madeWithLove);
					Drawable heart = getResources().getDrawable(R.drawable.ic_settings_loved);
					heart.setBounds(0, 0, heart.getIntrinsicWidth(), heart.getIntrinsicHeight());
					editable.setSpan(new ImageSpan(heart), heartStartIndex, heartLastIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					withLove.setText(editable);

					break;

				}
				return convertView;
			}
			
			@Override
			public boolean isEnabled(int position)
			{
				if (getItemViewType(position) == ViewType.VERSION.ordinal())
				{
					return false;
				}
				
				return super.isEnabled(position);
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
		setLocalMsisdn(contactInfo.getMsisdn());

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
		arguments.putBoolean(HikeConstants.CAN_EDIT_DP, true);

		HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_IMAGE, arguments);		
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		
		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.settings);
		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		ViewHolder holder = (ViewHolder) view.getTag();

		if (holder != null)
		{
			switch (holder.id)
			{
			case R.string.notifications:
				recordNotificationClick();
				IntentFactory.openSettingNotification(this);
				break;

			case R.string.settings_media:
				recordMediaClick();
				IntentFactory.openSettingMedia(this);
				break;

			case R.string.settings_chat:
				recordChatclick();
				IntentFactory.openSettingChat(this);
				break;

			case R.string.settings_sticker:
				recordStickerSettingsClick();
				IntentFactory.openStickerSettingsActivity(this);
				break;

            case R.string.language:
				recordLanguageClick();
				IntentFactory.openSettingLocalization(this);
				break;
				
			case R.string.sms_with_settings:
			case R.string.sms:
				recordSMSClick();
				IntentFactory.openSettingSMS(this);
				break;

			case R.string.connected_apps:
				IntentFactory.openConnectedApps(this);
				break;

			case R.string.manage_account:
				recordAccountClick();
				IntentFactory.openSettingAccount(this);
				break;

			case R.string.privacy:
				HAManager.logClickEvent(HikeConstants.LogEvent.PRIVACY_SETTING_CLICKED);
				IntentFactory.openSettingPrivacy(this);
				break;

			case R.string.settings_share_stickers:
				IntentFactory.openStickerSettings(this);
				break;

			case R.string.sticky_caller_settings:
				HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.CALLER_SETTINGS_BUTTON, null, AnalyticsConstants.StickyCallerEvents.HIKE, null);
				IntentFactory.openStickyCallerSettings(this, false);
				break;
				
			case R.string.help:
				IntentFactory.openSettingHelp(this);
				break;
			default:
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
		Drawable drawable = null;
		// workaround for bug AND-461 , if msisdn is null we will show default avatar
		if (contactInfo.getMsisdn() != null)
		{
			drawable = HikeMessengerApp.getLruCache().getIconFromCache(contactInfo.getMsisdn());
		}

		if (drawable == null)
		{
			drawable = HikeBitmapFactory.getDefaultTextAvatar(contactInfo.getMsisdn());
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
					addProfileImgInHeader();
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
	public String profileImageCropped()
	{
		String path = super.profileImageCropped();
		uploadProfilePicture(msisdn);
		return path;
	}

	@Override
	public void profilePictureUploaded()
	{
		super.profilePictureUploaded();
	}	
	
	@Override
	protected void openImageViewer(Object object)
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

	/**
	 * Sets the local msisdn of the profile
	 */
	protected void setLocalMsisdn(String msisdn)
	{
		this.msisdn = msisdn;
		super.setLocalMsisdn(this.msisdn);			
	}

	private void recordNotificationClick()
	{
		recordPreferencesPageOpen("notif");
	}

	private void recordMediaClick()
	{
		recordPreferencesPageOpen("media");
	}

	private void recordChatclick()
	{
		recordPreferencesPageOpen("chat_stng");
	}

	private void recordStickerSettingsClick()
	{
		recordPreferencesPageOpen("stk_stng");
	}

	private void recordLanguageClick()
	{
		recordPreferencesPageOpen("lng_stng");
	}

	private void recordAccountClick()
	{
		recordPreferencesPageOpen("account");
	}

	private void recordSMSClick()
	{
		recordPreferencesPageOpen("sms");
	}


	private void recordPreferencesPageOpen(String whichPage)
	{
		try
		{
			JSONObject json = HikeAnalyticsEvent.getSettingsAnalyticsJSON();

			if (json != null)
			{
				json.put(AnalyticsConstants.V2.FAMILY, whichPage);
				HAManager.getInstance().recordV2(json);
			}
		}

		catch (JSONException e)
		{
			e.toString();
		}
	}

}
