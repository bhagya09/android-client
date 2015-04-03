package com.bsb.hike.dialog;

import android.content.Context;
import android.widget.CheckBox;

import com.bsb.hike.R;

public class H20Dialog extends HikeDialog
{
	private CheckBox hikeSMSCheckBox;

	public H20Dialog(Context context, int theme, int id)
	{
		super(context, theme, id);
		initViews();
	}

	private void initViews()
	{
		this.setContentView(R.layout.sms_undelivered_popup);
		hikeSMSCheckBox = (CheckBox) this.findViewById(R.id.hike_sms_checkbox);
	}

	public boolean isHikeSMSChecked()
	{
		return hikeSMSCheckBox.isChecked();
	}
}
