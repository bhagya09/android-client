package com.bsb.hike.dialog;

import java.util.List;

import com.bsb.hike.R;

import android.content.Context;

public class H20Dialog extends CustomAlertRadioButtonDialog
{

	public H20Dialog(Context context, int dialogId, List<RadioButtonPojo> radioButtonPojo, RadioButtonItemCheckedListener listener)
	{
		super(context, dialogId, radioButtonPojo, listener);
	}

	public void editH20Groups(int freeHikeSms, int regularSms, String freeHikeSmsText)
	{
		for (RadioButtonPojo pojo : radioButtonPojoList)
		{
			if (pojo.id == freeHikeSms)
			{
				pojo.enabled = false;
				pojo.isChecked = false;
				pojo.subText = freeHikeSmsText;
			}

			if (pojo.id == regularSms)
			{
				pojo.isChecked = true;
			}
		}

		mAdapter.notifyDataSetChanged();
	}

	public boolean isHikeSMSChecked()
	{

		for (RadioButtonPojo pojo : radioButtonPojoList)
		{
			if (pojo.id == R.string.free_hike_sms)
			{
				return pojo.isChecked;
			}
		}
		return false;
	}
}
