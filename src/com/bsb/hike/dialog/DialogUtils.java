package com.bsb.hike.dialog;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.ImageQuality;
import com.bsb.hike.HikeConstants.SMSSyncState;
import com.bsb.hike.R;
import com.bsb.hike.dialog.CustomAlertRadioButtonDialog.RadioButtonPojo;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class DialogUtils
{

	public static String getForwardConfirmationText(Context context, ArrayList<ContactInfo> arrayList, boolean forwarding)
	{
		// multi forward case
		if (forwarding)
		{
			return arrayList.size() == 1 ? context.getResources().getString(R.string.forward_to_singular) : context.getResources().getString(R.string.forward_to_plural,
					arrayList.size());
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
		if (Utils.isHoneycombOrHigher())
		{
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		else
		{
			asyncTask.execute();
		}
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
		medium = new RadioButtonPojo(R.string.image_quality_medium, false, mediumSize, ctx.getString(R.string.image_quality_medium), ctx.getString(R.string.small_fastest));
		original = new RadioButtonPojo(R.string.image_quality_original, false, originalSize, ctx.getString(R.string.image_quality_original), ctx.getString(R.string.small_fastest));

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

}
