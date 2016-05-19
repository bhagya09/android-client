package com.bsb.hike.dialog;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.View;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.ImageQuality;
import com.bsb.hike.HikeConstants.SMSSyncState;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ComposeChatAdapter;
import com.bsb.hike.dialog.CustomAlertRadioButtonDialog.RadioButtonPojo;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class DialogUtils
{

	public static String getForwardConfirmationText(Context context, ArrayList<ContactInfo> arrayList, boolean forwarding)
	{

		boolean hasTimeline = false;
		String otherContactName = "";

		for(ContactInfo contactInfo:arrayList)
		{
			if(contactInfo!=null && contactInfo.getPhoneNum()!=null && contactInfo.getPhoneNum().equals(ComposeChatAdapter.HIKE_FEATURES_TIMELINE_ID))
			{
				hasTimeline = true;
			}
			else
			{
				otherContactName = contactInfo.getFirstName();
			}
		}

		// multi forward case
		if (forwarding)
		{
			if(hasTimeline)
			{
				if(arrayList.size() == 1)
				{
					return context.getString(R.string.share_on_timeline);
				}
				
				return arrayList.size() == 2 ? context.getString(R.string.forward_to_single_timeline, otherContactName) : context.getString(R.string.forward_to_plural_timeline,
						arrayList.size() - 1);
			}
			return arrayList.size() == 1 ? context.getResources().getString(R.string.forward_to_singular, otherContactName) : context.getResources().getString(R.string.forward_to_plural, arrayList.size());
		}

		if (hasTimeline)
		{
			if(arrayList.size() == 1)
			{
				return context.getString(R.string.share_on_timeline);
			}

			return arrayList.size() == 2 ? context.getString(R.string.share_with_contact_timeline, otherContactName) : context.getString(R.string.share_with_timeline,
					arrayList.size() - 1);
		}

		StringBuilder sb = new StringBuilder();

		int lastIndex = arrayList.size() - 1;

		boolean moreNamesThanMaxCount = false;
		if (lastIndex < 0)
		{
			lastIndex = 0;
		}
		else if (lastIndex == 1)
		{
			/*
			 * We increment the last index if its one since we can accommodate another name in this case.
			 */
			// lastIndex++;
			moreNamesThanMaxCount = true;
		}
		else if (lastIndex > 0)
		{
			moreNamesThanMaxCount = true;
		}

		for (int i = arrayList.size() - 1; i >= lastIndex; i--)
		{
			sb.append(arrayList.get(i).getFirstName());
			if (i > lastIndex + 1)
			{
				sb.append(", ");
			}
			else if (i == lastIndex + 1)
			{
				if (moreNamesThanMaxCount)
				{
					sb.append(", ");
				}
				else
				{
					sb.append(" and ");
				}
			}
		}
		String readByString = sb.toString();
		if (moreNamesThanMaxCount)
		{
			return context.getResources().getString(R.string.share_with_names_numbers, readByString, lastIndex);
		}
		else
		{
			return context.getResources().getString(R.string.share_with, readByString);
		}
	}
	
	public static void executeSMSSyncStateResultTask(AsyncTask<Void, Void, SMSSyncState> asyncTask)
	{
		asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void sendSMSSyncLogEvent(boolean syncing)
	{
		JSONObject data = new JSONObject();
		JSONObject metadata = new JSONObject();

		try
		{
			metadata.put(HikeConstants.PULL_OLD_SMS, syncing);

			data.put(HikeConstants.METADATA, metadata);
			data.put(HikeConstants.SUB_TYPE, HikeConstants.SMS);

			Utils.sendLogEvent(data);
		}
		catch (JSONException e)
		{
			Logger.w("LogEvent", e);
		}

	}

	public static void setupSyncDialogLayout(boolean syncConfirmation, CustomAlertDialog dialog)
	{
		dialog.buttonPanel.setVisibility(syncConfirmation ? View.VISIBLE : View.GONE);
		dialog.mProgressIndeterminate.setVisibility(syncConfirmation ? View.GONE : View.VISIBLE);
		dialog.setMessage(syncConfirmation ? R.string.import_sms_info : R.string.importing_sms_info);
	}
	
	protected static List<RadioButtonPojo> getImageQualityOptions(Context ctx, Object... data)
	{
		int quality = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.DEFAULT_IMG_QUALITY_FOR_SMO, ImageQuality.QUALITY_DEFAULT);
		long image_small_size = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SERVER_CONFIG_IMAGE_SIZE_SMALL, HikeConstants.IMAGE_SIZE_SMALL);
		long image_medium_size = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SERVER_CONFIG_IMAGE_SIZE_MEDIUM, HikeConstants.IMAGE_SIZE_MEDIUM);
		int smallsz, mediumsz, originalsz;
		String smallSize = "";
		String mediumSize = "";
		String originalSize = "";

		RadioButtonPojo small, medium, original;

		if (data != null)
		{
			Long[] dataBundle = (Long[]) data;

			if (dataBundle.length > 0)
			{

				originalsz = dataBundle[1].intValue();
				smallsz = (int) (dataBundle[0] * image_small_size);
				mediumsz = (int) (dataBundle[0] * image_medium_size);
				smallSize = " (" + Utils.getSizeForDisplay(smallsz) + ")";
				mediumSize = " (" + Utils.getSizeForDisplay(mediumsz) + ")";
				originalSize = " (" + Utils.getSizeForDisplay(originalsz) + ")";
				if (smallsz >= originalsz)
				{
					smallSize = "";
				}

				if (mediumsz >= originalsz)
				{
					mediumSize = "";
					smallSize = "";
				}
			}
		}

		small = new RadioButtonPojo(R.string.image_quality_small, false, smallSize, ctx.getString(R.string.image_quality_small), ctx.getString(R.string.small_fastest));
		medium = new RadioButtonPojo(R.string.image_quality_medium, false, mediumSize, ctx.getString(R.string.image_quality_medium), ctx.getString(R.string.medium_faster));
		original = new RadioButtonPojo(R.string.image_quality_original, false, originalSize, ctx.getString(R.string.image_quality_original), ctx.getString(R.string.original_slow));

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
		
		List<RadioButtonPojo> list = new ArrayList<RadioButtonPojo>(3);
		list.add(small);
		list.add(medium);
		list.add(original);
		
		return list;

	}
	
	public static List<RadioButtonPojo> getSMSOptions(Context context)
	{
		RadioButtonPojo nativeSMSRb, hikeSMSRb;
		
		String hikeSms = context.getString(R.string.free_hike_sms_subtext, context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getInt(HikeMessengerApp.SMS_SETTING, 0));
		
		nativeSMSRb = new RadioButtonPojo(R.string.regular_sms, false, "", context.getString(R.string.regular_sms), context.getString(R.string.carrier_charges_apply));
		
		hikeSMSRb = new RadioButtonPojo(R.string.free_hike_sms, false, "", context.getString(R.string.free_hike_sms), hikeSms);
		
		
		boolean sendNativeAlwaysPref = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.SEND_UNDELIVERED_AS_NATIVE_PREF, false);
		
		hikeSMSRb.setChecked(!sendNativeAlwaysPref);
		nativeSMSRb.setChecked(sendNativeAlwaysPref);
		
		List<RadioButtonPojo> list = new ArrayList<RadioButtonPojo>(2);
		list.add(hikeSMSRb);
		list.add(nativeSMSRb);
		
		return list;
	}
	
	
	public static List<RadioButtonPojo> getH20SMSOptions(Context context, boolean nativeOnly)
	{
		List<RadioButtonPojo> list = getSMSOptions(context);
		
		// Check Free Hike sms
		list.get(0).isChecked = true;
		// Uncheck native sms
		list.get(1).isChecked = false;
		
		if (nativeOnly)
		{
			list.remove(0);
		}
		
		if (Utils.isKitkatOrHigher())
		{
			list.remove(1);
		}
		
		return list;
	}

}
