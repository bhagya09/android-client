package com.bsb.hike.dialog;

import java.util.ArrayList;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.ImageQuality;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.adapters.AccountAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.AccountData;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfoData;
import com.bsb.hike.models.PhonebookContact;
import com.bsb.hike.tasks.SyncOldSMSTask;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontTextView;

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
		case DELETE_BLOCK:
		case DELETE_NON_MESSAGING_BOT:
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

	private static HikeDialog showImageQualityDialog(int dialogId, final Context context, final HikeDialogListener listener, Object... data)
	{
		final HikeDialog hikeDialog = new HikeDialog(context, dialogId);
		hikeDialog.setContentView(R.layout.image_quality_popup);
		hikeDialog.setCancelable(true);
		hikeDialog.setCanceledOnTouchOutside(true);
		SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		final Editor editor = appPrefs.edit();
		int quality = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.DEFAULT_IMG_QUALITY_FOR_SMO, ImageQuality.QUALITY_DEFAULT);
		final LinearLayout small_ll = (LinearLayout) hikeDialog.findViewById(R.id.hike_small_container);
		final LinearLayout medium_ll = (LinearLayout) hikeDialog.findViewById(R.id.hike_medium_container);
		final LinearLayout original_ll = (LinearLayout) hikeDialog.findViewById(R.id.hike_original_container);
		final CheckBox small = (CheckBox) hikeDialog.findViewById(R.id.hike_small_checkbox);
		final CheckBox medium = (CheckBox) hikeDialog.findViewById(R.id.hike_medium_checkbox);
		final CheckBox original = (CheckBox) hikeDialog.findViewById(R.id.hike_original_checkbox);
		CustomFontTextView smallSize = (CustomFontTextView) hikeDialog.findViewById(R.id.image_quality_small_cftv);
		CustomFontTextView mediumSize = (CustomFontTextView) hikeDialog.findViewById(R.id.image_quality_medium_cftv);
		CustomFontTextView originalSize = (CustomFontTextView) hikeDialog.findViewById(R.id.image_quality_original_cftv);
		Button once = (Button) hikeDialog.findViewById(R.id.btn_just_once);

		long image_small_size = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SERVER_CONFIG_IMAGE_SIZE_SMALL, HikeConstants.IMAGE_SIZE_SMALL);
		long image_medium_size = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SERVER_CONFIG_IMAGE_SIZE_MEDIUM, HikeConstants.IMAGE_SIZE_MEDIUM);
		
		if (data != null)
		{
			Long[] dataBundle = (Long[]) data;
			int smallsz, mediumsz, originalsz;
			if (dataBundle.length > 0)
			{

				originalsz = dataBundle[1].intValue();
				smallsz = (int) (dataBundle[0] * image_small_size);
				mediumsz = (int) (dataBundle[0] * image_medium_size);
				if (smallsz >= originalsz)
				{
					smallsz = originalsz;
					smallSize.setVisibility(View.GONE);
				}
				if (mediumsz >= originalsz)
				{
					mediumsz = originalsz;
					mediumSize.setVisibility(View.GONE);
					// if medium option text size is gone, so is small's
					smallSize.setVisibility(View.GONE);
				}
				smallSize.setText(" (" + Utils.getSizeForDisplay(smallsz) + ")");
				mediumSize.setText(" (" + Utils.getSizeForDisplay(mediumsz) + ")");
				originalSize.setText(" (" + Utils.getSizeForDisplay(originalsz) + ")");
			}
		}

		showImageQualityOption(quality, small, medium, original);

		OnClickListener imageQualityDialogOnClickListener = new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// TODO Auto-generated method stub
				switch (v.getId())
				{
				case R.id.hike_small_container:
					showImageQualityOption(ImageQuality.QUALITY_SMALL, small, medium, original);
					break;
				case R.id.hike_medium_container:
					showImageQualityOption(ImageQuality.QUALITY_MEDIUM, small, medium, original);
					break;
				case R.id.hike_original_container:
					showImageQualityOption(ImageQuality.QUALITY_ORIGINAL, small, medium, original);
					break;
				case R.id.btn_just_once:
					saveImageQualitySettings(editor, small, medium, original);
					HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.REMEMBER_IMAGE_CHOICE, false);
					callOnSucess(listener, hikeDialog);
					break;
				}
			}
		};

		small_ll.setOnClickListener(imageQualityDialogOnClickListener);
		medium_ll.setOnClickListener(imageQualityDialogOnClickListener);
		original_ll.setOnClickListener(imageQualityDialogOnClickListener);
		once.setOnClickListener(imageQualityDialogOnClickListener);

		hikeDialog.show();
		return hikeDialog;
	}

	private static void showImageQualityOption(int quality, CheckBox small, CheckBox medium, CheckBox original)
	{
		switch (quality)
		{
		case ImageQuality.QUALITY_ORIGINAL:
			small.setChecked(false);
			medium.setChecked(false);
			original.setChecked(true);
			break;
		case ImageQuality.QUALITY_MEDIUM:
			small.setChecked(false);
			medium.setChecked(true);
			original.setChecked(false);
			break;
		case ImageQuality.QUALITY_SMALL:
			small.setChecked(true);
			medium.setChecked(false);
			original.setChecked(false);
			break;
		}
	}

	private static void saveImageQualitySettings(Editor editor, CheckBox small, CheckBox medium, CheckBox original)
	{
		// TODO Auto-generated method stub
		if (medium.isChecked())
		{
			editor.putInt(HikeConstants.IMAGE_QUALITY, ImageQuality.QUALITY_MEDIUM);
		}
		else if (original.isChecked())
		{
			editor.putInt(HikeConstants.IMAGE_QUALITY, ImageQuality.QUALITY_ORIGINAL);
		}
		else
		{
			editor.putInt(HikeConstants.IMAGE_QUALITY, ImageQuality.QUALITY_SMALL);
		}
		editor.commit();
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
			hikeDialog.setPositiveButton(R.string.continue_txt, listener);
		}
		else
		{
			hikeDialog.setNegativeButton(R.string.cancel, listener);
			hikeDialog.setPositiveButton(R.string.allow, listener);
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
		dialog.setPositiveButton(R.string.ok, listener);
		dialog.setNegativeButton(R.string.cancel, listener);

		dialog.show();
		return dialog;
	}
	
	private static HikeDialog showDeleteAccountDialog(int dialogId, Context context, final HikeDialogListener listener)
	{
		final CustomAlertDialog correctMSISDNConfirmDialog = new CustomAlertDialog(context, dialogId);

		correctMSISDNConfirmDialog.setTitle(R.string.incorrect_msisdn_warning);
		correctMSISDNConfirmDialog.setMessage(R.string.incorrect_msisdn_msg);
		correctMSISDNConfirmDialog.setPositiveButton(R.string.ok, listener);
		correctMSISDNConfirmDialog.show();

		return correctMSISDNConfirmDialog;
	}
	
	private static HikeDialog showDeleteAccountConfirmDialog(int dialogId, Context context, final HikeDialogListener listener)
	{
		final CustomAlertDialog firstConfirmDialog = new CustomAlertDialog(context, dialogId);
		firstConfirmDialog.setTitle(R.string.are_you_sure);
		firstConfirmDialog.setMessage(R.string.delete_confirm_msg_1);
		firstConfirmDialog.setPositiveButton(R.string.confirm, listener);
		firstConfirmDialog.setNegativeButton(R.string.cancel, listener);
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
		forwardConfirmDialog.setPositiveButton(R.string.ok, listener);
		forwardConfirmDialog.setNegativeButton(R.string.cancel, listener);
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
		confirmDialog.setPositiveButton(R.string.yes, listener);
		confirmDialog.setNegativeButton(R.string.no, listener);
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
			confirmDialog.setPositiveButton(R.string.unlink_account, listener);
			confirmDialog.setNegativeButton(R.string.cancel,listener);
			break;
			
		case UNLINK_FB_DIALOG:
			confirmDialog.setTitle(R.string.unlink_facebook);
			confirmDialog.setMessage(R.string.unlink_facebook_confirmation);
			confirmDialog.setPositiveButton(R.string.unlink, listener);
			confirmDialog.setNegativeButton(R.string.cancel, listener);
			break;
			
		case UNLINK_TWITTER_DIALOG:
			confirmDialog.setTitle(R.string.unlink_twitter);
			confirmDialog.setMessage(R.string.unlink_twitter_confirmation);
			confirmDialog.setPositiveButton(R.string.unlink, listener);
			confirmDialog.setNegativeButton(R.string.cancel, listener);
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
		case DELETE_FILES_DIALOG:
			deleteConfirmDialog.setMessage(((int) data[0] == 1) ? context.getString(R.string.confirm_delete_msg) : context.getString(R.string.confirm_delete_msgs, (int) data[0]));
			deleteConfirmDialog.setTitle(((int) data[0] == 1) ? context.getString(R.string.confirm_delete_msg_header) : context.getString(R.string.confirm_delete_msgs_header, (int) data[0]));
			deleteConfirmDialog.setCheckBox(R.string.delete_media_from_sdcard, null, true);
			deleteConfirmDialog.setPositiveButton(R.string.delete, listener);
			deleteConfirmDialog.setNegativeButton(R.string.cancel, listener);
			break;
			
		case DELETE_PINS_DIALOG:
			deleteConfirmDialog.setMessage(((int) data[0] == 1) ? context.getString(R.string.confirm_delete_pin) : context.getString(R.string.confirm_delete_pins, (int) data[0]));
			deleteConfirmDialog.setTitle(((int) data[0] == 1) ? context.getString(R.string.confirm_delete_pin_header) : context.getString(R.string.confirm_delete_pins_header, (int) data[0]));
			deleteConfirmDialog.setPositiveButton(R.string.delete, listener);
			deleteConfirmDialog.setNegativeButton(R.string.cancel, listener);
			break;
			
		case DELETE_STATUS_DIALOG:
			deleteConfirmDialog.setTitle(R.string.delete_status);
			deleteConfirmDialog.setMessage(R.string.delete_status_confirmation);
			deleteConfirmDialog.setPositiveButton(R.string.ok, listener);
			deleteConfirmDialog.setNegativeButton(R.string.no, listener);
			break;
			
		case DELETE_STATUS_TIMELINE_DIALOG:
			deleteConfirmDialog.setTitle(R.string.delete_status);
			deleteConfirmDialog.setMessage(R.string.delete_status_timeline_confirmation);
			deleteConfirmDialog.setPositiveButton(R.string.yes, listener);
			deleteConfirmDialog.setNegativeButton(R.string.no, listener);
			break;
			
		case WIPE_TIMELINE_DIALOG:
			deleteConfirmDialog.setTitle(R.string.clear_timeline);
			deleteConfirmDialog.setTitle(R.string.clear_timeline_dialog);
			deleteConfirmDialog.setPositiveButton(R.string.yes, listener);
			deleteConfirmDialog.setNegativeButton(R.string.no, listener);
			break;
			
		case DELETE_FROM_GROUP:
			deleteConfirmDialog.setTitle(R.string.remove_from_group);
			deleteConfirmDialog.setMessage(context.getString(R.string.remove_confirm, (String) data[0]));
			deleteConfirmDialog.setPositiveButton(R.string.yes, listener);
			deleteConfirmDialog.setNegativeButton(R.string.no, listener);
			break;
		
		case DELETE_FROM_BROADCAST:
			deleteConfirmDialog.setTitle(R.string.remove_from_broadcast);
			deleteConfirmDialog.setMessage(context.getString(R.string.remove_confirm_broadcast, (String) data[0]));
			deleteConfirmDialog.setPositiveButton(R.string.yes, listener);
			deleteConfirmDialog.setNegativeButton(R.string.no, listener);
			break;
			
		case DELETE_CHAT_DIALOG:
		case DELETE_NON_MESSAGING_BOT:
			deleteConfirmDialog.setTitle(R.string.delete);
			deleteConfirmDialog.setMessage(context.getString(dialogId == DELETE_CHAT_DIALOG ? R.string.confirm_delete_chat_msg : R.string.confirm_delete_non_messaging,
					(String) data[0]));
			deleteConfirmDialog.setPositiveButton(R.string.yes, listener);
			deleteConfirmDialog.setNegativeButton(R.string.no, listener);
			break;
			
		case DELETE_GROUP_DIALOG:
			deleteConfirmDialog.setTitle(R.string.delete);
			deleteConfirmDialog.setMessage(context.getString(R.string.confirm_delete_group_msg, (String) data[0]));
			deleteConfirmDialog.setPositiveButton(android.R.string.ok, listener);
			deleteConfirmDialog.setNegativeButton(R.string.cancel, listener);
			break;
			
		case DELETE_BROADCAST_DIALOG:
			deleteConfirmDialog.setTitle(R.string.delete);
			deleteConfirmDialog.setMessage(context.getString(R.string.delete_broadcast_confirm));
			deleteConfirmDialog.setPositiveButton(android.R.string.ok, listener);
			deleteConfirmDialog.setNegativeButton(R.string.cancel, listener);
			break;
			
		case DELETE_ALL_CONVERSATIONS:
			deleteConfirmDialog.setTitle(R.string.deleteconversations);
			deleteConfirmDialog.setMessage(R.string.delete_all_question);
			deleteConfirmDialog.setPositiveButton(R.string.delete, listener);
			deleteConfirmDialog.setNegativeButton(R.string.cancel, listener);
			break;
			
		case DELETE_MESSAGES_DIALOG:
			deleteConfirmDialog.setTitle((int) data[0] == 1 ? R.string.confirm_delete_msg_header : R.string.confirm_delete_msgs_header);
			deleteConfirmDialog.setMessage((int) data[0] == 1 ? context.getString(R.string.confirm_delete_msg) : context.getString(R.string.confirm_delete_msgs, (int) data[0]));
			if ((boolean) data[1] == true)
			{
				deleteConfirmDialog.setCheckBox(R.string.delete_media_from_sdcard, null, true);
			}
			deleteConfirmDialog.setPositiveButton(R.string.delete, listener);
			deleteConfirmDialog.setNegativeButton(R.string.cancel, listener);
			break;
			
		case DELETE_BLOCK:
			deleteConfirmDialog.setTitle(R.string.delete_block);
			deleteConfirmDialog.setMessage(context.getString(R.string.confirm_delete_block_msg,(String) data[0]));
			deleteConfirmDialog.setPositiveButton(R.string.yes, listener);
			deleteConfirmDialog.setNegativeButton(R.string.no, listener);
		}
		deleteConfirmDialog.show();
		
		return deleteConfirmDialog;
	}
	
	private static HikeDialog showGPSDialog(int dialogId, Context context, final HikeDialogListener listener, Object... data)
	{
		final CustomAlertDialog alert = new CustomAlertDialog(context, dialogId);
		alert.setTitle(R.string.location);
		alert.setMessage((int) data[0]);
		alert.setPositiveButton(android.R.string.ok, listener);
		alert.setNegativeButton(R.string.cancel, listener);
		alert.show();
		
		return alert;
	}
	
	private static HikeDialog showH20Dialog(int dialogId, final Context context, final HikeDialogListener listener, Object... data)
	{
		final H20Dialog dialog = new H20Dialog(context, dialogId);
		boolean nativeOnly = (boolean) data[0];
		int selectedSMSCount = (int) data[1];
		int mCredits = (int) data[2];
		
		dialog.setCancelable(true);
		
		TextView popupHeader = (TextView) dialog.findViewById(R.id.popup_header);
		View hikeSMS = dialog.findViewById(R.id.hike_sms_container);
		View nativeSMS = dialog.findViewById(R.id.native_sms_container);
		TextView nativeHeader = (TextView) dialog.findViewById(R.id.native_sms_header);
		TextView hikeSmsHeader = (TextView) dialog.findViewById(R.id.hike_sms_header);
		TextView hikeSmsSubtext = (TextView) dialog.findViewById(R.id.hike_sms_subtext);

		popupHeader.setText(context.getString(R.string.send_sms_as, selectedSMSCount));
		hikeSmsSubtext.setText(context.getString(R.string.free_hike_sms_subtext, mCredits));

		hikeSMS.setVisibility(nativeOnly ? View.GONE : View.VISIBLE);
		nativeSMS.setVisibility(Utils.isKitkatOrHigher() ? View.GONE : View.VISIBLE);

		final CheckBox sendHike = (CheckBox) dialog.findViewById(R.id.hike_sms_checkbox);

		final CheckBox sendNative = (CheckBox) dialog.findViewById(R.id.native_sms_checkbox);

		final Button alwaysBtn = (Button) dialog.findViewById(R.id.btn_always);
		final Button justOnceBtn = (Button) dialog.findViewById(R.id.btn_just_once);

		sendHike.setChecked(true);
		
		if (!nativeOnly && mCredits < selectedSMSCount)
		{
			// Disable Free Hike SMS field and enable the native SMS one
			hikeSmsSubtext.setText(context.getString(R.string.free_hike_sms_subtext_diabled, mCredits));
			hikeSmsSubtext.setEnabled(false);
			hikeSmsHeader.setEnabled(false);
			hikeSMS.setEnabled(false);
			sendHike.setEnabled(false);
			sendHike.setChecked(false);
			sendNative.setChecked(true);
		}

		nativeHeader.setText(context.getString(R.string.regular_sms));
		
		OnClickListener onClickListener = new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				switch (v.getId())
				{
				case R.id.hike_sms_container:
				case R.id.hike_sms_checkbox:
					sendHike.setChecked(true);
					sendNative.setChecked(false);
					break;
					
				case R.id.native_sms_container:
				case R.id.native_sms_checkbox:
					sendHike.setChecked(false);
					sendNative.setChecked(true);
					break;
					
				case R.id.btn_always:
					HAManager.getInstance().record(HikeConstants.LogEvent.SMS_POPUP_ALWAYS_CLICKED, AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT);
					
					Utils.setSendUndeliveredAlwaysAsSmsSetting(context, true, !sendHike.isChecked());
					listener.positiveClicked(dialog);
					break;
					
				case R.id.btn_just_once:
					HAManager.getInstance().record(HikeConstants.LogEvent.SMS_POPUP_JUST_ONCE_CLICKED, AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT);
					
					listener.positiveClicked(dialog);
					break;
				}
			}
		};
		
		hikeSMS.setOnClickListener(onClickListener);
		sendHike.setOnClickListener(onClickListener);
		nativeSMS.setOnClickListener(onClickListener);
		sendNative.setOnClickListener(onClickListener);
		alwaysBtn.setOnClickListener(onClickListener);
		justOnceBtn.setOnClickListener(onClickListener);


		dialog.show();
		return dialog;
	}
	
	
	private static HikeDialog showSMSSyncDialog(int dialogId, final Context context, final HikeDialogListener listener, Object... data)
	{
		final CustomAlertDialog dialog = new CustomAlertDialog(context, dialogId);
		
		boolean syncConfirmation = (boolean) data[0]; 

		dialog.setTitle(R.string.import_sms);
		dialog.setMessage(R.string.import_sms_info);
		dialog.setPositiveButton(R.string.yes, listener);
		dialog.setNegativeButton(R.string.no, listener);

		DialogUtils.setupSyncDialogLayout(syncConfirmation, dialog);

		dialog.buttonPositive.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
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

		dialog.setOnDismissListener(new OnDismissListener()
		{

			@Override
			public void onDismiss(DialogInterface dialog)
			{
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
}
