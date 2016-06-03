package com.bsb.hike.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.ImageQuality;
import com.bsb.hike.HikeConstants.MuteDuration;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.AccountAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.dialog.CustomAlertRadioButtonCheckboxDialog.CheckBoxPojo;
import com.bsb.hike.dialog.CustomAlertRadioButtonDialog.RadioButtonItemCheckedListener;
import com.bsb.hike.dialog.CustomAlertRadioButtonDialog.RadioButtonPojo;
import com.bsb.hike.models.AccountData;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfoData;
import com.bsb.hike.models.Mute;
import com.bsb.hike.models.PhonebookContact;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.tasks.SyncOldSMSTask;
import com.bsb.hike.timeline.adapter.DisplayContactsAdapter;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class HikeDialogFactory
{
	public static final int FAVORITE_ADDED_DIALOG = 2;

	public static final int RESET_STEALTH_DIALOG = 4;

	public static final int SHARE_IMAGE_QUALITY_DIALOG = 6;

	public static final int SMS_CLIENT_DIALOG = 7;

	public static final int CONTACT_SEND_DIALOG = 8;

	public static final int CONTACT_SAVE_DIALOG = 9;
	
	public static final int CLEAR_CONVERSATION_DIALOG = 10;
	
	public static final int DELETE_ACCOUNT_DIALOG = 11;
	
	public static final int DELETE_ACCOUNT_CONFIRM_DIALOG = 12;
	
	public static final int FORWARD_CONFIRMATION_DIALOG = 14;
	
	public static final int SHOW_INVITE_CONFIRMATION_DIALOG = 15;
	
	public static final int UNLINK_ACCOUNT_CONFIRMATION_DIALOG = 16;
	
	public static final int UNLINK_FB_DIALOG = 17;
	
	public static final int UNLINK_TWITTER_DIALOG = 18;
	
	public static final int DELETE_FILES_DIALOG = 19;
	
	public static final int DELETE_PINS_DIALOG = 20;
	
	public static final int DELETE_STATUS_DIALOG = 21;
	
	public static final int DELETE_FROM_GROUP = 22;
	
	public static final int GPS_DIALOG = 23;
	
	public static final int DELETE_CHAT_DIALOG = 24;
	
	public static final int DELETE_GROUP_DIALOG = 25;
	
	public static final int DELETE_ALL_CONVERSATIONS = 26;
	
	public static final int DELETE_MESSAGES_DIALOG = 27;
	
	public static final int SHOW_H20_SMS_DIALOG = 28;
	
	public static final int SMS_SYNC_DIALOG = 29; 
	
	public static final int HIKE_UPGRADE_DIALOG = 30;
	
	public static final int VOIP_INTRO_DIALOG = 31;

	public static final int DELETE_BROADCAST_DIALOG = 32;
	
	public static final int DELETE_FROM_BROADCAST = 33;
	
	public static final int REMOVE_DP_CONFIRM_DIALOG = 34;
	
	public static final int DELETE_BLOCK = 35;
	
	public static final int DELETE_NON_MESSAGING_BOT = 36;
	
	public static final int DELETE_STATUS_TIMELINE_DIALOG = 37;
	
	public static final int WIPE_TIMELINE_DIALOG = 38;
	
	public static final int SMS_PREF_DIALOG = 39;
	
	public static final int GROUP_ADD_MEMBER_SETTINGS = 40;
	
	public static final int UNDO_MULTI_EDIT_CHANGES_DIALOG = 42;
	
	public static final int ADD_TO_FAV_DIALOG = 43;
	
	public static final int ACCESSIBILITY_DIALOG = 44;
	
	public static final int LIKE_CONTACTS_DIALOG = 45;

	public static final int MICROAPP_DIALOG = 46;
	
	public static final int MAPP_DOWNLOAD_DIALOG = 47;

	public static final int CALLER_BLOCK_CONTACT_DIALOG = 48;

	public static final int CALLER_UNBLOCK_CONTACT_DIALOG = 49;

	public static final int DELETE_STICKER_PACK_DIALOG = 50;

	public static final int DELETE_GROUP_CONVERSATION_DIALOG= 51;

	public static final int DB_CORRUPT_RESTORE_DIALOG = 52;

	public static final int MUTE_CHAT_DIALOG = 53;

	public static final int STICKER_RESTORE_DIFF_DPI_DIALOG = 54;

	public static final int BLOCK_CHAT_CONFIRMATION_DIALOG = 55;

	public static final int CT_CONFIRMATION_DIALOG = 56;

	public static HikeDialog showDialog(Context context, int whichDialog, Object... data)
	{
		return showDialog(context, whichDialog, null, data);
	}

	public static HikeDialog showDialog(Context context, int dialogId, HikeDialogListener listener, Object... data)
	{

		switch (dialogId)
		{
		case FAVORITE_ADDED_DIALOG:
			return showAddedAsFavoriteDialog(dialogId, context, listener, data);
			
		case ADD_TO_FAV_DIALOG:
			return showAddToFavoriteDialog(dialogId, context, listener, data);
			
		case RESET_STEALTH_DIALOG:
			return showStealthResetDialog(dialogId, context, listener, data);
			
		case SHARE_IMAGE_QUALITY_DIALOG:
			return showImageQualityDialog(dialogId, context, listener, data);
			
		case SMS_CLIENT_DIALOG:
			return showSMSClientDialog(dialogId, context, listener, data);
			
		case CONTACT_SEND_DIALOG:
		case CONTACT_SAVE_DIALOG:
			return showPhonebookContactDialog(dialogId, context, listener, data);
			
		case CLEAR_CONVERSATION_DIALOG:
			return showClearConversationDialog(dialogId, context, listener);
			
		case DELETE_ACCOUNT_DIALOG:
			return showDeleteAccountDialog(dialogId, context, listener);
			
		case DELETE_ACCOUNT_CONFIRM_DIALOG :
			return showDeleteAccountConfirmDialog(dialogId, context, listener);
			
		case FORWARD_CONFIRMATION_DIALOG:
			return showForwardConfirmationDialog(dialogId, context, listener, data);
			
		case SHOW_INVITE_CONFIRMATION_DIALOG:
			return showInviteConfirmationDialog(dialogId, context, listener, data);
			
		case UNLINK_ACCOUNT_CONFIRMATION_DIALOG:
		case UNLINK_FB_DIALOG:
		case UNLINK_TWITTER_DIALOG:
			return showUnlinkAccountDialog(dialogId, context, listener);
			
		case DELETE_FILES_DIALOG:
		case DELETE_PINS_DIALOG:
		case DELETE_STATUS_DIALOG:
		case DELETE_STATUS_TIMELINE_DIALOG:
		case WIPE_TIMELINE_DIALOG:
		case DELETE_FROM_GROUP:
		case DELETE_FROM_BROADCAST:
		case DELETE_CHAT_DIALOG:
		case DELETE_GROUP_DIALOG:
		case DELETE_ALL_CONVERSATIONS:
		case DELETE_MESSAGES_DIALOG:
		case DELETE_BROADCAST_DIALOG:
		case DELETE_GROUP_CONVERSATION_DIALOG:
		case DELETE_BLOCK:
		case DELETE_NON_MESSAGING_BOT:
		case UNDO_MULTI_EDIT_CHANGES_DIALOG:
		case ACCESSIBILITY_DIALOG:
		case DELETE_STICKER_PACK_DIALOG:
			return showDeleteMessagesDialog(dialogId, context, listener, data);
			
		case GPS_DIALOG:
			return showGPSDialog(dialogId, context, listener, data);
			
		case SHOW_H20_SMS_DIALOG:
			return showH20Dialog(dialogId, context, listener, data);
			
		case SMS_SYNC_DIALOG:
			return showSMSSyncDialog(dialogId, context, listener, data);
			
		case HIKE_UPGRADE_DIALOG:
			return showHikeUpgradeDialog(dialogId, context, data);
			
		case VOIP_INTRO_DIALOG:
			return showVoipFtuePopUp(dialogId, context, listener, data);
		case GROUP_ADD_MEMBER_SETTINGS:
			return showGroupSettingsDialog(dialogId, context, listener, data);
		case MICROAPP_DIALOG:
			return showMicroAppDialog(dialogId,context,listener,data);
		case MAPP_DOWNLOAD_DIALOG:
			return showMicroappDownloadDialog(dialogId, context, listener, data);
		case CALLER_BLOCK_CONTACT_DIALOG:
		case CALLER_UNBLOCK_CONTACT_DIALOG:
			return showBlockContactDialog(context, dialogId, listener, data);
		case DB_CORRUPT_RESTORE_DIALOG:
			return showDBCorruptDialog(context, dialogId, listener, data);
		case BLOCK_CHAT_CONFIRMATION_DIALOG:
			return showBlockChatConfirmationDialog(context, dialogId, listener, data);
		case MUTE_CHAT_DIALOG:
			return showChatMuteDialog(context, dialogId, listener, data);
		case STICKER_RESTORE_DIFF_DPI_DIALOG:
			return showStickerRestoreDiffDpiDialog(context, dialogId, listener, data);

		case CT_CONFIRMATION_DIALOG:
			return showCTConfirmationDialog(context, dialogId, listener, data);
		}
		return null;
	}

	private static HikeDialog showBlockContactDialog(Context context, int dialogId, HikeDialogListener listener, Object... data)
	{
		final CustomAlertDialog blockConfirmDialog = new CustomAlertDialog(context, dialogId);
		switch (dialogId)
		{
		case CALLER_BLOCK_CONTACT_DIALOG:
			blockConfirmDialog.setMessage(String.format(context.getString(R.string.block_contact_sure), (String) data[0]));
			blockConfirmDialog.setTitle(context.getString(R.string.block_contact));
			break;
		case CALLER_UNBLOCK_CONTACT_DIALOG:
			blockConfirmDialog.setMessage(String.format(context.getString(R.string.unblock_contact_sure), (String) data[0]));
			blockConfirmDialog.setTitle(context.getString(R.string.unblock_contact));
			break;
		}
		blockConfirmDialog.setPositiveButton(R.string.dialog_btn_yes, listener);
		blockConfirmDialog.setNegativeButton(R.string.dialog_btn_no, listener);
		blockConfirmDialog.setCancelable(true);
		blockConfirmDialog.show();
		return blockConfirmDialog;
	}

	public static <T> HikeDialog showDialog(Context context, int dialogId, T data1, HikeDialogListener listener, Object... data2)
	{
		switch (dialogId)
		{
		case LIKE_CONTACTS_DIALOG:
			return showLikesContactListDialog(dialogId, data1, context, data2);
		}

		return null;
	}
	
	private static HikeDialog showAddedAsFavoriteDialog(int dialogId, Context context, final HikeDialogListener listener, Object... data)
	{
		String name = "";
		try
		{
			name = (String) data[0];
		}
		catch (ClassCastException ex)
		{
			throw new IllegalArgumentException("Make sure You are sending one string , that is name to fill with in dialog");
		}
		final HikeDialog hikeDialog = new HikeDialog(context, dialogId);
		hikeDialog.setContentView(R.layout.added_as_favorite_pop_up);
		hikeDialog.setCancelable(true);
		TextView heading = (TextView) hikeDialog.findViewById(R.id.addedYouAsFavHeading);
		heading.setText(context.getString(R.string.addedYouAsFavorite, name));
		TextView des = (TextView) hikeDialog.findViewById(R.id.addedYouAsFavDescription);
		des.setText(Html.fromHtml(context.getString(R.string.addedYouFrindDescription, name, name)));
		View no = hikeDialog.findViewById(R.id.noButton);
		View yes = hikeDialog.findViewById(R.id.yesButton);
		OnClickListener clickListener = new OnClickListener()
		{

			@Override
			public void onClick(View arg0)
			{
				switch (arg0.getId())
				{
				case R.id.noButton:
					if (listener != null)
					{
						listener.negativeClicked(hikeDialog);
					}
					else
					{
						hikeDialog.dismiss();
					}
					break;
				case R.id.yesButton:
					if (listener != null)
					{
						listener.positiveClicked(hikeDialog);
					}
					else
					{
						hikeDialog.dismiss();
					}
					break;
				}

			}
		};
		no.setOnClickListener(clickListener);
		yes.setOnClickListener(clickListener);
		hikeDialog.show();
		return hikeDialog;
	}
	
	private static HikeDialog showAddToFavoriteDialog(int dialogId, Context context, final HikeDialogListener listener, Object... data)
	{
		String name = "";
		try
		{
			name = (String) data[0];
		}
		catch (ClassCastException ex)
		{
			throw new IllegalArgumentException("Make sure You are sending one string , that is name to fill with in dialog");
		}
		final HikeDialog hikeDialog = new HikeDialog(context, dialogId);
		hikeDialog.setContentView(R.layout.added_as_favorite_pop_up);
		hikeDialog.setCancelable(true);
		hikeDialog.findViewById(R.id.addedYouAsFavHeading).setVisibility(View.GONE);
		TextView des = (TextView) hikeDialog.findViewById(R.id.addedYouAsFavDescription);
		des.setText(context.getString(R.string.add_to_fav_confirmation, name));
		View no = hikeDialog.findViewById(R.id.noButton);
		View yes = hikeDialog.findViewById(R.id.yesButton);
		OnClickListener clickListener = new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				switch (arg0.getId())
				{
				case R.id.noButton:
					if (listener != null)
					{
						listener.negativeClicked(hikeDialog);
					}
					else
					{
						hikeDialog.dismiss();
					}
					break;
				case R.id.yesButton:
					if (listener != null)
					{
						listener.positiveClicked(hikeDialog);
					}
					else
					{
						hikeDialog.dismiss();
					}
					break;
				}

			}
		};
		no.setOnClickListener(clickListener);
		yes.setOnClickListener(clickListener);
		hikeDialog.show();
		return hikeDialog;
	}

	private static HikeDialog showStealthResetDialog(int dialogId, Context context, final HikeDialogListener listener, Object... data)
	{
		final HikeDialog hikeDialog = new HikeDialog(context, dialogId);
		hikeDialog.setContentView(R.layout.stealth_ftue_popup);
		hikeDialog.setCancelable(true);

		String header = (String) data[0];
		String body = (String) data[1];
		String okBtnString = (String) data[2];
		String cancelBtnString = (String) data[3];

		TextView headerText = (TextView) hikeDialog.findViewById(R.id.header);
		TextView bodyText = (TextView) hikeDialog.findViewById(R.id.body);
		TextView cancelBtn = (TextView) hikeDialog.findViewById(R.id.noButton);
		TextView okBtn = (TextView) hikeDialog.findViewById(R.id.awesomeButton);

		hikeDialog.findViewById(R.id.btn_separator).setVisibility(View.VISIBLE);

		cancelBtn.setVisibility(View.VISIBLE);

		headerText.setText(header);
		bodyText.setText(body);
		cancelBtn.setText(cancelBtnString);
		okBtn.setText(okBtnString);

		okBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				listener.positiveClicked(hikeDialog);
			}
		});

		cancelBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				listener.negativeClicked(hikeDialog);
			}
		});

		hikeDialog.show();
		return hikeDialog;
	}

	private static HikeDialog showChatMuteDialog(final Context context, int dialogId, final HikeDialogListener listener, Object... data)
	{
		List<RadioButtonPojo> radioButtons = DialogUtils.getMuteDurationOptions(context);

		CheckBoxPojo checkBox = DialogUtils.showNotificationCheckBox(context);

		final Mute mute = (Mute) data[0];
		mute.setShowNotifInMute(checkBox.isChecked);
		mute.setMuteDuration(MuteDuration.DURATION_DEFAULT);

		final CustomAlertRadioButtonCheckboxDialog hikeDialog = new CustomAlertRadioButtonCheckboxDialog(context, dialogId, radioButtons, new RadioButtonItemCheckedListener() {

			@Override
			public void onRadioButtonItemClicked(RadioButtonPojo whichItem, CustomAlertRadioButtonDialog dialog) {
				dialog.selectedRadioGroup = whichItem;
				saveMuteDuration(mute, whichItem);
			}

		}, checkBox, new CustomAlertRadioButtonCheckboxDialog.CheckBoxListener() {

			@Override
			public void onCheckboxClicked(CheckBoxPojo whichItem, CustomAlertRadioButtonDialog dialog) {
				mute.setShowNotifInMute(whichItem.isChecked);
			}
		});

		hikeDialog.setCancelable(true);
		hikeDialog.setCanceledOnTouchOutside(true);
		hikeDialog.setTitle(OneToNConversationUtils.isOneToNConversation(mute.getMsisdn()) ? R.string.group_mute_dialog_title : R.string.chat_mute_dialog_title);
		hikeDialog.setPositiveButton(R.string.OK, listener);
		hikeDialog.setNegativeButton(R.string.CANCEL, listener);

		hikeDialog.show();
		return hikeDialog;

	}

	private static HikeDialog showImageQualityDialog(int dialogId, final Context context, final HikeDialogListener listener, Object... data)
	{
		
		SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		final Editor editor = appPrefs.edit();
		List<RadioButtonPojo> radioButtons = DialogUtils.getImageQualityOptions(context, data);
		
		final CustomAlertRadioButtonDialog hikeDialog = new CustomAlertRadioButtonDialog(context, dialogId,  radioButtons, new RadioButtonItemCheckedListener()
		{

			@Override
			public void onRadioButtonItemClicked(RadioButtonPojo whichItem, CustomAlertRadioButtonDialog dialog)
			{
				dialog.selectedRadioGroup = whichItem;
			}
			
		});

		hikeDialog.setCancelable(true);
		hikeDialog.setCanceledOnTouchOutside(true);
		hikeDialog.setTitle(R.string.image_quality_prefs);
		hikeDialog.setPositiveButton(R.string.send_uppercase, null);

		OnClickListener imageQualityDialogOnClickListener = new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
					saveImageQualitySettings(editor, hikeDialog.selectedRadioGroup);
					HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.REMEMBER_IMAGE_CHOICE, false);
					callOnSucess(listener, hikeDialog);
			}
		};

		hikeDialog.buttonPositive.setOnClickListener(imageQualityDialogOnClickListener);

		hikeDialog.show();
		return hikeDialog;
	}

	private static HikeDialog showGroupSettingsDialog(int dialogId, final Context context, final HikeDialogListener listener, Object... data)
	{

		String text = (String) data[0];

		final CustomAlertDialog confirmDialog = new CustomAlertDialog(context, dialogId);
		confirmDialog.setMessage(text);
		confirmDialog.setPositiveButton(R.string.YES, listener);
		confirmDialog.setNegativeButton(R.string.NO, listener);
		confirmDialog.show();
		
		return confirmDialog;
	}

	private static void saveImageQualitySettings(Editor editor, RadioButtonPojo pojo)
	{
		if (pojo != null)
		{
			switch (pojo.id)
			{
			case R.string.image_quality_small:
				editor.putInt(HikeConstants.IMAGE_QUALITY, ImageQuality.QUALITY_SMALL);
				editor.commit();				
				break;
				
			case R.string.image_quality_medium:
				editor.putInt(HikeConstants.IMAGE_QUALITY, ImageQuality.QUALITY_MEDIUM);
				editor.commit();				
				break;
				
			case R.string.image_quality_original:
				editor.putInt(HikeConstants.IMAGE_QUALITY, ImageQuality.QUALITY_ORIGINAL);
				editor.commit();				
				break;
			}
		}
	}

	private static void saveMuteDuration(Mute mute, RadioButtonPojo pojo)
	{
		if (pojo != null)
		{
			switch (pojo.id)
			{
				case R.string.mute_chat_eight_hrs:
					mute.setMuteDuration(MuteDuration.DURATION_EIGHT_HOURS);
					break;

				case R.string.mute_chat_one_week:
					mute.setMuteDuration(MuteDuration.DURATION_ONE_WEEK);
					break;

				case R.string.mute_chat_one_yr:
					mute.setMuteDuration(MuteDuration.DURATION_ONE_YEAR);
					break;
			}
		}
	}

	private static void callOnSucess(HikeDialogListener listener, HikeDialog hikeDialog)
	{
		// TODO Auto-generated method stub
		if (listener != null)
		{
			listener.positiveClicked(hikeDialog);
		}
		else
		{
			hikeDialog.dismiss();
		}
	}

	private static HikeDialog showSMSClientDialog(int dialogId, Context context, final HikeDialogListener listener, Object... data)
	{
		return showSMSClientDialog(dialogId, context, listener, (Boolean) data[0], (CompoundButton) data[1], (Boolean) data[2]);
	}

	private static HikeDialog showSMSClientDialog(int dialogId, final Context context, final HikeDialogListener listener, final boolean triggeredFromToggle,
			final CompoundButton checkBox, final boolean showingNativeInfoDialog)
	{
		final CustomAlertDialog hikeDialog = new CustomAlertDialog(context, dialogId);
		hikeDialog.setCancelable(showingNativeInfoDialog);
		hikeDialog.setTitle(showingNativeInfoDialog ? R.string.native_header : R.string.use_hike_for_sms);
		
		hikeDialog.setMessage(showingNativeInfoDialog ? R.string.native_info : R.string.use_hike_for_sms_info);
		
		if (showingNativeInfoDialog)
		{
			hikeDialog.setPositiveButton(R.string.CONTINUE, listener);
		}
		else
		{
			hikeDialog.setNegativeButton(R.string.CANCEL, listener);
			hikeDialog.setPositiveButton(R.string.ALLOW, listener);
		}

		hikeDialog.setOnCancelListener(new OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				if (showingNativeInfoDialog && checkBox != null)
				{
					checkBox.setChecked(false);
				}
			}
		});

		hikeDialog.show();
		return hikeDialog;
	}

	private static HikeDialog showPhonebookContactDialog(int dialogId, Context context, final HikeDialogListener listener, Object... data)
	{
		try
		{
			PhonebookContact contact = (PhonebookContact) data[0];
			String okText = (String) data[1];
			Boolean showAccountInfo = (Boolean) data[2];
			final ContactDialog contactDialog = new ContactDialog(context, dialogId);
			contactDialog.setContentView(R.layout.contact_share_info);
			contactDialog.data = contact;
			ViewGroup parent = (ViewGroup) contactDialog.findViewById(R.id.parent);
			TextView contactName = (TextView) contactDialog.findViewById(R.id.contact_name);
			ListView contactDetails = (ListView) contactDialog.findViewById(R.id.contact_details);
			Button yesBtn = (Button) contactDialog.findViewById(R.id.btn_ok);
			Button noBtn = (Button) contactDialog.findViewById(R.id.btn_cancel);
			View accountContainer = contactDialog.findViewById(R.id.account_container);
			final Spinner accounts = (Spinner) contactDialog.findViewById(R.id.account_spinner);
			final TextView accountInfo = (TextView) contactDialog.findViewById(R.id.account_info);

			

			contactDialog.setViewReferences(parent, accounts);

			yesBtn.setText(okText);

			if (showAccountInfo)
			{
				accountContainer.setVisibility(View.VISIBLE);
				accounts.setAdapter(new AccountAdapter(context, Utils.getAccountList(context)));
				if (accounts.getSelectedItem() != null)
				{
					accountInfo.setText(((AccountData) accounts.getSelectedItem()).getName());
				}
				else
				{
					accountInfo.setText(R.string.device);
				}
			}
			else
			{
				accountContainer.setVisibility(View.GONE);
			}

			accountContainer.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					accounts.performClick();
				}
			});

			accounts.setOnItemSelectedListener(new OnItemSelectedListener()
			{

				@Override
				public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id)
				{
					accountInfo.setText(((AccountData) accounts.getSelectedItem()).getName());
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0)
				{
				}

			});

			contactName.setText(contact.name);
			
			//If the account info is not shown, then remove the padding of 12dp used by it on top.
			if(!showAccountInfo)
				contactDetails.setPadding(contactDetails.getPaddingLeft(), 0, contactDetails.getPaddingRight(), contactDetails.getPaddingBottom());
			
			contactDetails.setAdapter(new ArrayAdapter<ContactInfoData>(context, R.layout.contact_share_item, R.id.info_value, contact.items)
			{

				@Override
				public View getView(int position, View convertView, ViewGroup parent)
				{
					View v = super.getView(position, convertView, parent);
					ContactInfoData contactInfoData = getItem(position);

					TextView header = (TextView) v.findViewById(R.id.info_head);
					header.setText(contactInfoData.getDataSubType());

					TextView details = (TextView) v.findViewById(R.id.info_value);
					details.setText(contactInfoData.getData());
					return v;
				}

			});
			yesBtn.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (listener != null)
					{
						listener.positiveClicked(contactDialog);
					}
					else
					{
						contactDialog.dismiss();
					}
				}
			});
			noBtn.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (listener != null)
					{
						listener.negativeClicked(contactDialog);
					}
					else
					{
						contactDialog.dismiss();
					}
				}
			});
			contactDialog.show();
			return contactDialog;
		}
		catch (ClassCastException c)
		{
			throw new IllegalArgumentException(
					"Make sure you are sending PhonebookContact object in data[0] and String for okText in data[1] and boolean to show account info in data[2] and dialog id in data[3]");
		}
	}
	
	private static HikeDialog showClearConversationDialog(int dialogId, Context context, final HikeDialogListener listener)
	{
		final CustomAlertDialog dialog = new CustomAlertDialog(context, dialogId);
		
		dialog.setTitle(R.string.clear_conversation);
		dialog.setMessage(R.string.confirm_clear_conversation);
		dialog.setPositiveButton(R.string.OK, listener);
		dialog.setNegativeButton(R.string.CANCEL, listener);

		dialog.show();
		return dialog;
	}
	
	private static HikeDialog showDeleteAccountDialog(int dialogId, Context context, final HikeDialogListener listener)
	{
		final CustomAlertDialog correctMSISDNConfirmDialog = new CustomAlertDialog(context, dialogId);

		correctMSISDNConfirmDialog.setTitle(R.string.incorrect_msisdn_warning);
		correctMSISDNConfirmDialog.setMessage(R.string.incorrect_msisdn_msg);
		correctMSISDNConfirmDialog.setPositiveButton(R.string.OK, listener);
		correctMSISDNConfirmDialog.show();

		return correctMSISDNConfirmDialog;
	}
	
	private static HikeDialog showDeleteAccountConfirmDialog(int dialogId, Context context, final HikeDialogListener listener)
	{
		final CustomAlertDialog firstConfirmDialog = new CustomAlertDialog(context, dialogId);
		firstConfirmDialog.setTitle(R.string.are_you_sure);
		firstConfirmDialog.setMessage(R.string.delete_account_description);
		firstConfirmDialog.setPositiveButton(R.string.CONFIRM, listener);
		firstConfirmDialog.setNegativeButton(R.string.CANCEL, listener);
		firstConfirmDialog.show();
		return firstConfirmDialog;
	}
	
	private static HikeDialog showForwardConfirmationDialog(int dialogId, Context context, final HikeDialogListener listener, Object... data)
	{
		boolean isSharing = (boolean) data[0];
		
		ArrayList<ContactInfo> arrayList = (ArrayList<ContactInfo>) data[1];
		final CustomAlertDialog forwardConfirmDialog = new CustomAlertDialog(context, dialogId);
		if (isSharing)
		{
			forwardConfirmDialog.setTitle(R.string.share);
			forwardConfirmDialog.setMessage(DialogUtils.getForwardConfirmationText(context, arrayList, false));
		}
		else
		{
			forwardConfirmDialog.setTitle(R.string.forward);
			forwardConfirmDialog.setMessage(DialogUtils.getForwardConfirmationText(context, arrayList, true));
		}
		forwardConfirmDialog.setPositiveButton(R.string.OK, listener);
		forwardConfirmDialog.setNegativeButton(R.string.CANCEL, listener);
		forwardConfirmDialog.show();
		
		return forwardConfirmDialog;
	}
	
	private static HikeDialog showInviteConfirmationDialog(int dialogId, Context context, final HikeDialogListener listener, Object... data)
	{
		final CustomAlertDialog confirmDialog = new CustomAlertDialog(context, dialogId);
		boolean selectAllChecked = (boolean) data[0];
		int selectedContactsSize = (int) data[1];
		if(!selectAllChecked)
		{
			confirmDialog.setTitle(R.string.invite_friends);
			confirmDialog.setMessage(context.getResources().getString(R.string.invite_friends_confirmation_msg, selectedContactsSize));
		}
		else
		{
			confirmDialog.setTitle(R.string.select_all_confirmation_header);
			confirmDialog.setMessage(context.getResources().getString(R.string.select_all_confirmation_msg, selectedContactsSize));
		}
		confirmDialog.setPositiveButton(R.string.YES, listener);
		confirmDialog.setNegativeButton(R.string.NO, listener);
		confirmDialog.show();
		
		return confirmDialog;
	}
	
	private static HikeDialog showUnlinkAccountDialog(int dialogId, Context context, final HikeDialogListener listener)
	{
		final CustomAlertDialog confirmDialog = new CustomAlertDialog(context, dialogId);
		
		switch (dialogId)
		{
		case UNLINK_ACCOUNT_CONFIRMATION_DIALOG:
			confirmDialog.setTitle(R.string.unlink_account);
			confirmDialog.setMessage(R.string.unlink_confirmation);
			confirmDialog.setPositiveButton(R.string.UNLINK_ACCOUNT, listener);
			confirmDialog.setNegativeButton(R.string.CANCEL,listener);
			break;
			
		case UNLINK_FB_DIALOG:
			confirmDialog.setTitle(R.string.unlink_facebook);
			confirmDialog.setMessage(R.string.unlink_facebook_confirmation);
			confirmDialog.setPositiveButton(R.string.UNLINK, listener);
			confirmDialog.setNegativeButton(R.string.CANCEL, listener);
			break;
			
		case UNLINK_TWITTER_DIALOG:
			confirmDialog.setTitle(R.string.unlink_twitter);
			confirmDialog.setMessage(R.string.unlink_twitter_confirmation);
			confirmDialog.setPositiveButton(R.string.UNLINK, listener);
			confirmDialog.setNegativeButton(R.string.CANCEL, listener);
			break;
		}
		
		confirmDialog.show();
		
		return confirmDialog;
	}
	
	private static HikeDialog showDeleteMessagesDialog(int dialogId, Context context, final HikeDialogListener listener, Object... data)
	{
		final CustomAlertDialog deleteConfirmDialog = new CustomAlertDialog(context, dialogId);
		
		switch (dialogId)
		{
		case ACCESSIBILITY_DIALOG:
			deleteConfirmDialog.setMessage(context.getString(R.string.accessibility_dialog_text));
			deleteConfirmDialog.setTitle(context.getString(R.string.accessbility));
			deleteConfirmDialog.setCancelable(false);
			deleteConfirmDialog.setPositiveButton(R.string.ENABLE, listener);
			deleteConfirmDialog.setNegativeButton(R.string.CANCEL, listener);
			break;
		case DELETE_FILES_DIALOG:
			deleteConfirmDialog.setMessage(((int) data[0] == 1) ? context.getString(R.string.confirm_delete_msg) : context.getString(R.string.confirm_delete_msgs, (int) data[0]));
			deleteConfirmDialog.setTitle(((int) data[0] == 1) ? context.getString(R.string.confirm_delete_msg_header) : context.getString(R.string.confirm_delete_msgs_header, (int) data[0]));
			deleteConfirmDialog.setCheckBox(R.string.delete_media_from_sdcard, null, true);
			deleteConfirmDialog.setPositiveButton(R.string.DELETE, listener);
			deleteConfirmDialog.setNegativeButton(R.string.CANCEL, listener);
			break;
			
		case DELETE_PINS_DIALOG:
			deleteConfirmDialog.setMessage(((int) data[0] == 1) ? context.getString(R.string.confirm_delete_pin) : context.getString(R.string.confirm_delete_pins, (int) data[0]));
			deleteConfirmDialog.setTitle(((int) data[0] == 1) ? context.getString(R.string.confirm_delete_pin_header) : context.getString(R.string.confirm_delete_pins_header, (int) data[0]));
			deleteConfirmDialog.setPositiveButton(R.string.DELETE, listener);
			deleteConfirmDialog.setNegativeButton(R.string.CANCEL, listener);
			break;
			
		case DELETE_STATUS_DIALOG:
			deleteConfirmDialog.setTitle(R.string.delete_status);
			deleteConfirmDialog.setMessage(R.string.delete_status_confirmation);
			deleteConfirmDialog.setPositiveButton(R.string.OK, listener);
			deleteConfirmDialog.setNegativeButton(R.string.NO, listener);
			break;
			
		case DELETE_STATUS_TIMELINE_DIALOG:
			deleteConfirmDialog.setTitle(R.string.delete_status);
			deleteConfirmDialog.setMessage(R.string.delete_status_timeline_confirmation);
			deleteConfirmDialog.setPositiveButton(R.string.DELETE, listener);
			deleteConfirmDialog.setNegativeButton(R.string.CANCEL, listener);
			break;
			
		case WIPE_TIMELINE_DIALOG:
			deleteConfirmDialog.setTitle(R.string.clear_timeline);
			deleteConfirmDialog.setMessage(R.string.clear_timeline_dialog);
			deleteConfirmDialog.setPositiveButton(R.string.CLEAR, listener);
			deleteConfirmDialog.setNegativeButton(R.string.CANCEL, listener);
			break;
			
		case DELETE_FROM_GROUP:
			deleteConfirmDialog.setTitle(R.string.remove_from_group);
			deleteConfirmDialog.setMessage(context.getString(R.string.remove_confirm, (String) data[0]));
			deleteConfirmDialog.setPositiveButton(R.string.YES, listener);
			deleteConfirmDialog.setNegativeButton(R.string.NO, listener);
			break;
		
		case DELETE_FROM_BROADCAST:
			deleteConfirmDialog.setTitle(R.string.remove_from_broadcast);
			deleteConfirmDialog.setMessage(context.getString(R.string.remove_confirm_broadcast, (String) data[0]));
			deleteConfirmDialog.setPositiveButton(R.string.YES, listener);
			deleteConfirmDialog.setNegativeButton(R.string.NO, listener);
			break;
			
		case DELETE_CHAT_DIALOG:
		case DELETE_NON_MESSAGING_BOT:
			deleteConfirmDialog.setTitle(R.string.delete);
			deleteConfirmDialog.setMessage(context.getString(dialogId == DELETE_CHAT_DIALOG ? R.string.confirm_delete_chat_msg : R.string.confirm_delete_non_messaging,
					(String) data[0]));
			deleteConfirmDialog.setPositiveButton(R.string.YES, listener);
			deleteConfirmDialog.setNegativeButton(R.string.NO, listener);
			break;
			
		case DELETE_GROUP_DIALOG:
			deleteConfirmDialog.setMessage(context.getString(R.string.confirm_delete_group_msg, (String) data[0]));
			deleteConfirmDialog.setCheckBox(R.string.delete_conversation,null, false);
			deleteConfirmDialog.setPositiveButton(R.string.YES, listener);
			deleteConfirmDialog.setNegativeButton(R.string.CANCEL, listener);
			deleteConfirmDialog.setTitle(R.string.leave_group);
			break;
			
		case DELETE_BROADCAST_DIALOG:
			deleteConfirmDialog.setTitle(R.string.delete);
			deleteConfirmDialog.setMessage(context.getString(R.string.delete_broadcast_confirm));
			deleteConfirmDialog.setPositiveButton(R.string.OK, listener);
			deleteConfirmDialog.setNegativeButton(R.string.CANCEL, listener);
			break;
		case DELETE_GROUP_CONVERSATION_DIALOG:
			deleteConfirmDialog.setTitle(R.string.delete);
			deleteConfirmDialog.setMessage(context.getString(R.string.delete_group_confirm, (String) data[0]));
			deleteConfirmDialog.setPositiveButton(context.getString(R.string.DELETE), listener);
			deleteConfirmDialog.setNegativeButton(R.string.CANCEL, listener);
			break;
			
		case DELETE_ALL_CONVERSATIONS:
			deleteConfirmDialog.setTitle(R.string.deleteconversations);
			deleteConfirmDialog.setMessage(R.string.delete_all_question);
			deleteConfirmDialog.setPositiveButton(R.string.DELETE, listener);
			deleteConfirmDialog.setNegativeButton(R.string.CANCEL, listener);
			break;
			
		case DELETE_MESSAGES_DIALOG:
			deleteConfirmDialog.setTitle((int) data[0] == 1 ? R.string.confirm_delete_msg_header : R.string.confirm_delete_msgs_header);
			deleteConfirmDialog.setMessage((int) data[0] == 1 ? context.getString(R.string.confirm_delete_msg) : context.getString(R.string.confirm_delete_msgs, (int) data[0]));
			if ((boolean) data[1] == true)
			{
				deleteConfirmDialog.setCheckBox(R.string.delete_media_from_sdcard, null, true);
			}
			deleteConfirmDialog.setPositiveButton(R.string.DELETE, listener);
			deleteConfirmDialog.setNegativeButton(R.string.CANCEL, listener);
			break;
			
		case DELETE_BLOCK:
			deleteConfirmDialog.setTitle(R.string.delete_block);
			deleteConfirmDialog.setMessage(context.getString(R.string.confirm_delete_block_msg,(String) data[0]));
			deleteConfirmDialog.setPositiveButton(R.string.YES, listener);
			deleteConfirmDialog.setNegativeButton(R.string.NO, listener);
			break;
			
		case UNDO_MULTI_EDIT_CHANGES_DIALOG:
			deleteConfirmDialog.setTitle(R.string.multi_edit_undo_warning_header);
			deleteConfirmDialog.setMessage(context.getString(R.string.multi_edit_undo_warning));
			deleteConfirmDialog.setPositiveButton(R.string.OK, listener);
			deleteConfirmDialog.setNegativeButton(R.string.CANCEL, listener);
			break;

		case DELETE_STICKER_PACK_DIALOG:
			deleteConfirmDialog.setTitle(context.getString(R.string.delete) + " " + data[0]);
			deleteConfirmDialog.setMessage(R.string.delete_pack_question);
			deleteConfirmDialog.setPositiveButton(R.string.DELETE, listener);
			deleteConfirmDialog.setNegativeButton(R.string.CANCEL, listener);
			break;
		}
		deleteConfirmDialog.show();
		
		return deleteConfirmDialog;
	}
	
	private static HikeDialog showGPSDialog(int dialogId, Context context, final HikeDialogListener listener, Object... data)
	{
		final CustomAlertDialog alert = new CustomAlertDialog(context, dialogId);
		alert.setTitle(R.string.location);
		alert.setMessage((int) data[0]);
		alert.setPositiveButton((int)data[1], listener);
		alert.setNegativeButton(R.string.CANCEL, listener);
		alert.show();
		
		return alert;
	}
	
	private static HikeDialog showH20Dialog(int dialogId, final Context context, final HikeDialogListener listener, Object... data)
	{
		boolean nativeOnly = (boolean) data[0];
		int selectedSMSCount = (int) data[1];
		int mCredits = (int) data[2];

		final H20Dialog dialog = new H20Dialog(context, HikeDialogFactory.SHOW_H20_SMS_DIALOG, DialogUtils.getH20SMSOptions(context, nativeOnly),
				new RadioButtonItemCheckedListener()
				{
					@Override
					public void onRadioButtonItemClicked(RadioButtonPojo whichItem, CustomAlertRadioButtonDialog dialog)
					{
						dialog.selectedRadioGroup = whichItem;
					}

				});

		dialog.setCancelable(true);
		dialog.setTitle(context.getString(R.string.send_sms_as, selectedSMSCount));
		dialog.setPositiveButton(R.string.ALWAYS, null);
		dialog.setNegativeButton(R.string.JUST_ONCE, null);

		if (!nativeOnly && mCredits < selectedSMSCount)
		{
			dialog.editH20Groups(R.string.free_hike_sms, R.string.regular_sms, (context.getString(R.string.free_hike_sms_subtext_diabled, mCredits)));
			// Disable Free Hike SMS field and enable the native SMS one
		}

		dialog.buttonPositive.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				HAManager.getInstance().record(HikeConstants.LogEvent.SMS_POPUP_ALWAYS_CLICKED, AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT);
				Utils.setSendUndeliveredAlwaysAsSmsSetting(context, true, !dialog.isHikeSMSChecked());
				listener.positiveClicked(dialog);
			}
		});

		dialog.buttonNegative.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				HAManager.getInstance().record(HikeConstants.LogEvent.SMS_POPUP_JUST_ONCE_CLICKED, AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT);
				listener.positiveClicked(dialog);
			}
		});

		dialog.show();
		return dialog;
	}
	
	
	private static HikeDialog showSMSSyncDialog(int dialogId, final Context context, final HikeDialogListener listener, Object... data)
	{
		final CustomAlertDialog dialog = new CustomAlertDialog(context, dialogId);
		
		boolean syncConfirmation = (boolean) data[0]; 

		dialog.setTitle(R.string.import_sms);
		dialog.setMessage(R.string.import_sms_info);
		dialog.setPositiveButton(R.string.YES, listener);
		dialog.setNegativeButton(R.string.NO, listener);

		DialogUtils.setupSyncDialogLayout(syncConfirmation, dialog);

		dialog.buttonPositive.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				HikeMessengerApp.getPubSub().publish(HikePubSub.SMS_SYNC_START, null);

				DialogUtils.executeSMSSyncStateResultTask(new SyncOldSMSTask(context));

				DialogUtils.setupSyncDialogLayout(false, dialog);

				DialogUtils.sendSMSSyncLogEvent(true);
			}
		});

		dialog.buttonNegative.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				dialog.dismiss();

				DialogUtils.sendSMSSyncLogEvent(false);
			}
		});

		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				Editor editor = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
				editor.putBoolean(HikeMessengerApp.SHOWN_SMS_SYNC_POPUP, true);
				editor.commit();
			}
		});

		dialog.show();
		return dialog;
	}
	
	/**
	 * This dialog can be used whenever we show an upgrading hike dialog from HomeActivity.
	 * 
	 * @param context
	 * @param data
	 * @return
	 */
	private static HikeDialog showHikeUpgradeDialog(int dialogId, Context context, Object[] data)
	{
		final HikeDialog dialog = new HikeDialog(context, dialogId);
		dialog.setContentView(R.layout.app_update_popup);
		dialog.setCancelable(false);

		ImageView icon = (ImageView) dialog.findViewById(R.id.dialog_icon);
		TextView titleTextView = (TextView) dialog.findViewById(R.id.dialog_header_tv);
		TextView messageTextView = (TextView) dialog.findViewById(R.id.dialog_message_tv);

		icon.setImageBitmap(HikeBitmapFactory.decodeResource(context.getResources(), R.drawable.art_sticker_mac));
		titleTextView.setText(context.getResources().getString(R.string.sticker_shop));
		messageTextView.setText(context.getResources().getString(R.string.hike_upgrade_string));

		dialog.show();
		return dialog;
	}
	
	private static HikeDialog showVoipFtuePopUp(int dialogId, final Context context, final HikeDialogListener listener, Object... data)
	{
		final HikeDialog dialog = new HikeDialog(context, dialogId);
		dialog.setContentView(R.layout.voip_ftue_popup);
		dialog.setCancelable(true);
		TextView okBtn = (TextView) dialog.findViewById(R.id.awesomeButton);
		View betaTag = (View) dialog.findViewById(R.id.beta_tag);
		
		okBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (listener != null)
				{
					listener.neutralClicked(dialog);
				}
				dialog.dismiss();
			}
		});

		RotateAnimation animation = new RotateAnimation(0.0f, 45.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		animation.setDuration(1);
		animation.setFillAfter(true);
		betaTag.startAnimation(animation);
		dialog.show();
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOW_VOIP_FTUE_POPUP, true);
		return dialog;
	}	

	private static <T> HikeDialog showLikesContactListDialog(int dialogId, T data1, final Context context, Object... data)
	{
		final HikeDialog dialog = new HikeDialog(context, R.style.Theme_CustomDialog, LIKE_CONTACTS_DIALOG);
		dialog.setContentView(R.layout.display_contacts_dialog);
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);

		if (data == null || data.length == 0 || !(data[0] instanceof ArrayList<?>))
		{
			return null;
		}

		ArrayList<String> msisdns = (ArrayList<String>) data[0];
		String statusMsisdn = (String) data1;

		ListView listContacts = (ListView) dialog.findViewById(R.id.listContacts);
		final DisplayContactsAdapter contactsAdapter = new DisplayContactsAdapter(msisdns, statusMsisdn);
		listContacts.setAdapter(contactsAdapter);
		listContacts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				// We are changing DataSet(msisdns) sent to Adapter inside DisplayContactsAdapter,
				// So we are fetching msisdn for item clicked from Adapter only
				String currentMsisdn = contactsAdapter.getMsisdnAsPerPostion(position);
				if (Utils.isSelfMsisdn(currentMsisdn)) {
					Intent intent2 = new Intent(context, ProfileActivity.class);
					intent2.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
					context.startActivity(intent2);
				} else {

					Intent intent = IntentFactory.createChatThreadIntentFromContactInfo(context, ContactManager.getInstance().getContact(currentMsisdn, true, true), false, false, ChatThreadActivity.ChatThreadOpenSources.LIKES_DIALOG);
					// Add anything else to the intent
					intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					context.startActivity(intent);
				}
				dialog.dismiss();
			}
		});
		return dialog;
	}

	private static HikeDialog showMicroAppDialog(int dialogId, final Context context, final HikeDialogListener listener, Object... data)
	{
		String title = (String) data[0];
		String text = (String) data[1];
		String positive = (String) data[2];
		String negative = (String) data[3];
		final CustomAlertDialog nativeDialog = new CustomAlertDialog(context, dialogId);
		nativeDialog.setTitle(title);
		nativeDialog.setMessage(text);
		nativeDialog.setPositiveButton(positive, listener);
		if (!TextUtils.isEmpty(negative))
			nativeDialog.setNegativeButton(negative, listener);
		nativeDialog.show();
		return nativeDialog;
	}

	private static HikeDialog showMicroappDownloadDialog(int dialogId, final Context context, final HikeDialogListener listener, Object... data)
	{
		final CustomAlertDialog dialog = new CustomAlertDialog(context, HikeDialogFactory.MAPP_DOWNLOAD_DIALOG, R.layout.mapp_download_dialog);
		BotInfo botInfo;
		if (data != null && data[0] != null && data[0] instanceof BotInfo)
		{
			botInfo = (BotInfo) data[0];
		}
		else
		{
			Logger.e("BotDiscovery", "BotInfo to showMicroappDownloadDialog is null or not instanceof BotInfo");
			return null;
		}

		dialog.setPositiveButton(context.getResources().getString(R.string.take_me_there), listener);
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);

		TextView bot_name = (TextView) dialog.findViewById(R.id.bot_name);
		bot_name.setText(botInfo.getConversationName());

		TextView description = (TextView) dialog.findViewById(R.id.bot_description);
		description.setText(botInfo.getBotDescription());

		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				if (context instanceof Activity) {
					Utils.unblockOrientationChange((Activity) context);
				}
			}
		});

		dialog.show();

		return dialog;
	}

	private static HikeDialog showDBCorruptDialog(Context context, int dialogId, HikeDialogListener listener, Object... data)
	{
		final CustomAlertDialog dialog = new CustomAlertDialog(context, dialogId, R.layout.db_corrupt_dialog);

		dialog.setTitle(context.getString(R.string.restore_chat_title));
		dialog.setMessage(context.getString(R.string.restore_chat_body));
		dialog.setCancelable(false);
		dialog.setPositiveButton(R.string.RESTORE_CAP, listener);
		dialog.setNegativeButton(R.string.SKIP_RESTORE, listener);

		dialog.show();
		return dialog;
	}

	private static HikeDialog showBlockChatConfirmationDialog(Context context, int dialogId, HikeDialogListener listener, Object... data)
	{
		final CustomAlertDialog dialog = new CustomAlertDialog(context, dialogId);

		dialog.setTitle(context.getString(R.string.block_dialog_title));
		dialog.setMessage(context.getString(R.string.block_dialog_body));
		dialog.setCancelable(true);

		boolean toShowSpamCheckBox = true;
		if(data != null)
		{
			toShowSpamCheckBox = (Boolean)data[0];
		}

		if(toShowSpamCheckBox)
		{
			dialog.setCheckBox(context.getString(R.string.spam_info_in_dialog), null, false);
		}

		dialog.setPositiveButton(R.string.YES, listener);
		dialog.setNegativeButton(R.string.CANCEL, listener);
		dialog.show();
		return dialog;
	}

	private static HikeDialog showStickerRestoreDiffDpiDialog(Context context, int dialogId, HikeDialogListener listener, Object[] data)
	{
		CustomAlertDialog dialog = new CustomAlertDialog(context, dialogId);
		dialog.setMessage(context.getString(R.string.sticker_restore_diffdpi_message));
		dialog.setTitle(context.getString(R.string.sticker_restore_diffdpi_title));
		dialog.setCancelable(false);
		dialog.setPositiveButton(R.string.OK, listener);
		dialog.show();
		return dialog;
	}

	private static HikeDialog showCTConfirmationDialog(Context context, int dialogId, HikeDialogListener listener, Object... data)
	{
		final CustomAlertDialog dialog = new CustomAlertDialog(context, dialogId, R.layout.db_corrupt_dialog);

		dialog.setTitle(context.getString(R.string.chat_theme));
		dialog.setMessage(context.getString(R.string.ct_confirmation_dialog));
		dialog.setCancelable(false);
		dialog.setPositiveButton(R.string.OK, listener);
		dialog.setNegativeButton(R.string.CANCEL, listener);

		dialog.show();
		return dialog;
	}

}
