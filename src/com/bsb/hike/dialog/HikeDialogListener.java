package com.bsb.hike.dialog;

import android.content.DialogInterface;

public interface HikeDialogListener
{
	public static final int BUTTON_POSITIVE  = DialogInterface.BUTTON_POSITIVE;

	public static final int BUTTON_NEGATIVE  = DialogInterface.BUTTON_NEGATIVE;

	public static final int BUTTON_NEUTRAL  = DialogInterface.BUTTON_NEUTRAL;

	public void negativeClicked(HikeDialog hikeDialog);

	public void positiveClicked(HikeDialog hikeDialog);

	public void neutralClicked(HikeDialog hikeDialog);

}