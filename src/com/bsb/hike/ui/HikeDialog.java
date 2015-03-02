package com.bsb.hike.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.ImageQuality;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontTextView;

public class HikeDialog
{
	public static final int FAVORITE_ADDED_DIALOG = 2;

	public static final int STEALTH_FTUE_DIALOG = 3;

	public static final int RESET_STEALTH_DIALOG = 4;

	public static final int STEALTH_FTUE_EMPTY_STATE_DIALOG = 5;

	public static final int SHARE_IMAGE_QUALITY_DIALOG = 6;

	public static final int SMS_CLIENT_DIALOG = 7;

	public static final int HIKE_UPGRADE_DIALOG = 8;

	public static final int VOIP_INTRO_DIALOG = 9;

	public static Dialog showDialog(Context context, int whichDialog, Object... data)
	{
		return showDialog(context, whichDialog, null, data);
	}

	public static Dialog showDialog(Context context, int whichDialog, HikeDialogListener listener, Object... data)
	{

		switch (whichDialog)
		{
		case FAVORITE_ADDED_DIALOG:
			return showAddedAsFavoriteDialog(context, listener, data);
		case STEALTH_FTUE_DIALOG:
			return showStealthFtuePopUp(context, listener, true);
		case RESET_STEALTH_DIALOG:
			return showStealthResetDialog(context, listener, data);
		case STEALTH_FTUE_EMPTY_STATE_DIALOG:
			return showStealthFtuePopUp(context, listener, false);
		case SHARE_IMAGE_QUALITY_DIALOG:
			return showImageQualityDialog(context, listener, data);
		case SMS_CLIENT_DIALOG:
			return showSMSClientDialog(context, listener, data);
		case HIKE_UPGRADE_DIALOG:
			return showHikeUpgradeDialog(context, data);
		case VOIP_INTRO_DIALOG:
			return showVoipFtuePopUp(context, listener, data);
		}

		return null;

	}

	private static Dialog showAddedAsFavoriteDialog(Context context, final HikeDialogListener listener, Object... data)
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
		final Dialog dialog = new Dialog(context, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.added_as_favorite_pop_up);
		dialog.setCancelable(true);
		TextView heading = (TextView) dialog.findViewById(R.id.addedYouAsFavHeading);
		heading.setText(context.getString(R.string.addedYouAsFavorite, name));
		TextView des = (TextView) dialog.findViewById(R.id.addedYouAsFavDescription);
		des.setText(Html.fromHtml(context.getString(R.string.addedYouFrindDescription, name, name)));
		View no = dialog.findViewById(R.id.noButton);
		View yes = dialog.findViewById(R.id.yesButton);
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
						listener.negativeClicked(dialog);
					}
					else
					{
						dialog.dismiss();
					}
					break;
				case R.id.yesButton:
					if (listener != null)
					{
						listener.positiveClicked(dialog);
					}
					else
					{
						dialog.dismiss();
					}
					break;
				}

			}
		};
		no.setOnClickListener(clickListener);
		yes.setOnClickListener(clickListener);
		dialog.show();
		return dialog;
	}

	private static Dialog showStealthFtuePopUp(final Context context, final HikeDialogListener listener, boolean isStealthFtueDialog)
	{
		final Dialog dialog = new Dialog(context, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.stealth_ftue_popup);
		dialog.setCancelable(true);
		TextView okBtn = (TextView) dialog.findViewById(R.id.awesomeButton);
		TextView body = (TextView) dialog.findViewById(R.id.body);
		
		if(isStealthFtueDialog)
		{
			body.setText(R.string.stealth_mode_popup_msg);
			okBtn.setText(R.string.quick_setup);
		}
		else
		{
			body.setText(R.string.stealth_mode_empty_conv_popup_msg);
			okBtn.setText(android.R.string.ok);
		}
		
		okBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				if (listener != null)
				{
					listener.neutralClicked(dialog);
				}
				else
				{
					dialog.dismiss();
				}
				
				dialog.dismiss();
			}
		});

		dialog.show();
		return dialog;
	}
	
	private static Dialog showVoipFtuePopUp(final Context context, final HikeDialogListener listener, Object... data)
	{
		final Dialog dialog = new Dialog(context, R.style.Theme_CustomDialog);
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
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOWN_VOIP_INTRO_TIP, true);
		return dialog;
	}

	private static Dialog showStealthResetDialog(Context context, final HikeDialogListener listener, Object... data)
	{
		final Dialog dialog = new Dialog(context, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.stealth_ftue_popup);
		dialog.setCancelable(true);

		String header = (String) data[0];
		String body = (String) data[1];
		String okBtnString = (String) data[2];
		String cancelBtnString = (String) data[3];

		TextView headerText = (TextView) dialog.findViewById(R.id.header);
		TextView bodyText = (TextView) dialog.findViewById(R.id.body);
		TextView cancelBtn = (TextView) dialog.findViewById(R.id.noButton);
		TextView okBtn = (TextView) dialog.findViewById(R.id.awesomeButton);

		dialog.findViewById(R.id.btn_separator).setVisibility(View.VISIBLE);

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
				listener.positiveClicked(dialog);
			}
		});

		cancelBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				listener.negativeClicked(dialog);
			}
		});

		dialog.show();
		return dialog;
	}
	
	private static Dialog showImageQualityDialog(final Context context, final HikeDialogListener listener, Object... data)
	{
		final Dialog dialog = new Dialog(context, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.image_quality_popup);
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);
		SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		final Editor editor = appPrefs.edit();
		int quality = ImageQuality.QUALITY_DEFAULT;
		final LinearLayout small_ll = (LinearLayout) dialog.findViewById(R.id.hike_small_container);
		final LinearLayout medium_ll = (LinearLayout) dialog.findViewById(R.id.hike_medium_container);
		final LinearLayout original_ll = (LinearLayout) dialog.findViewById(R.id.hike_original_container);
		final CheckBox small = (CheckBox) dialog.findViewById(R.id.hike_small_checkbox);
		final CheckBox medium = (CheckBox) dialog.findViewById(R.id.hike_medium_checkbox);
		final CheckBox original = (CheckBox) dialog.findViewById(R.id.hike_original_checkbox);
		CustomFontTextView header = (CustomFontTextView) dialog.findViewById(R.id.image_quality_popup_header);
		CustomFontTextView smallSize = (CustomFontTextView) dialog.findViewById(R.id.image_quality_small_cftv);
		CustomFontTextView mediumSize = (CustomFontTextView) dialog.findViewById(R.id.image_quality_medium_cftv);
		CustomFontTextView originalSize = (CustomFontTextView) dialog.findViewById(R.id.image_quality_original_cftv);
		Button once = (Button) dialog.findViewById(R.id.btn_just_once);
		
		if(data!=null)
			{
			Long[] dataBundle = (Long[])data;
			int smallsz,mediumsz,originalsz;
			if(dataBundle.length>0)
				{
				
				originalsz = dataBundle[1].intValue();
				smallsz = (int) (dataBundle[0] * HikeConstants.IMAGE_SIZE_SMALL);
				mediumsz = (int) (dataBundle[0] * HikeConstants.IMAGE_SIZE_MEDIUM);
				if (smallsz >= originalsz)
				{
					smallsz = originalsz;
					smallSize.setVisibility(View.GONE);
				}
				if(mediumsz >= originalsz)
				{
					mediumsz = originalsz;
					mediumSize.setVisibility(View.GONE);
					// if medium option text size is gone, so is small's
					smallSize.setVisibility(View.GONE);
				}
				smallSize.setText(" (" + Utils.getSizeForDisplay(smallsz)+ ")");
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
					callOnSucess(listener, dialog);
					break;
				}
			}
		};

		small_ll.setOnClickListener(imageQualityDialogOnClickListener);
		medium_ll.setOnClickListener(imageQualityDialogOnClickListener);
		original_ll.setOnClickListener(imageQualityDialogOnClickListener);
		once.setOnClickListener(imageQualityDialogOnClickListener);

		dialog.show();
		return dialog;
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

	private static void callOnSucess(HikeDialogListener listener, Dialog dialog)
	{
		// TODO Auto-generated method stub
		if (listener != null)
		{
			listener.onSucess(dialog);
		}
		else
		{
			dialog.dismiss();
		}
	}

	private static Dialog showSMSClientDialog(Context context, final HikeDialogListener listener, Object... data)
	{
		return showSMSClientDialog(context, listener, (Boolean) data[0], (CompoundButton) data[1], (Boolean) data[2]);
	}
	
	private static Dialog showSMSClientDialog(final Context context, final HikeDialogListener listener, final boolean triggeredFromToggle, 
			final CompoundButton checkBox, final boolean showingNativeInfoDialog )
	{
		final Dialog dialog = new Dialog(context, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.enable_sms_client_popup);
		dialog.setCancelable(showingNativeInfoDialog);

		TextView header = (TextView) dialog.findViewById(R.id.header);
		TextView body = (TextView) dialog.findViewById(R.id.body);
		Button btnOk = (Button) dialog.findViewById(R.id.btn_ok);
		Button btnCancel = (Button) dialog.findViewById(R.id.btn_cancel);

		header.setText(showingNativeInfoDialog ? R.string.native_header : R.string.use_hike_for_sms);
		body.setText(showingNativeInfoDialog ? R.string.native_info : R.string.use_hike_for_sms_info);

		if (showingNativeInfoDialog)
		{
			btnCancel.setVisibility(View.GONE);
			btnOk.setText(R.string.continue_txt);
		}
		else
		{
			btnCancel.setText(R.string.cancel);
			btnOk.setText(R.string.allow);
		}

		btnOk.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				listener.positiveClicked(dialog);
			}
		});

		dialog.setOnCancelListener(new OnCancelListener()
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

		btnCancel.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				listener.negativeClicked(dialog);
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
	private static Dialog showHikeUpgradeDialog(Context context, Object[] data)
	{
		final Dialog dialog = new Dialog(context, R.style.Theme_CustomDialog);
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


	public static interface HikeDialogListener
	{
		public void negativeClicked(Dialog dialog);

		public void positiveClicked(Dialog dialog);

		public void neutralClicked(Dialog dialog);
		
		public void onSucess(Dialog dialog);
	}
}
