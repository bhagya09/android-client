package com.bsb.hike.dialog;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.HikeConstants.SMSSyncState;
import com.bsb.hike.models.ContactInfo;
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
	
	public static void setupSyncDialogLayout(boolean syncConfirmation, View btnContainer, ProgressBar syncProgress, TextView info, View btnDivider)
	{
		btnContainer.setVisibility(syncConfirmation ? View.VISIBLE : View.GONE);
		syncProgress.setVisibility(syncConfirmation ? View.GONE : View.VISIBLE);
		btnDivider.setVisibility(syncConfirmation ? View.VISIBLE : View.GONE);
		info.setText(syncConfirmation ? R.string.import_sms_info : R.string.importing_sms_info);
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
}
