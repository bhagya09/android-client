package com.bsb.hike.chatHead;

import com.bsb.hike.HikeMessengerApp;
import android.content.ClipboardManager;
import android.content.ClipboardManager.OnPrimaryClipChangedListener;
import android.content.Context;

public class ClipboardListener implements OnPrimaryClipChangedListener
{
	private int MIN_DIGIT_CONST = 6;

	private int MAX_DIGIT_CONST = 13;

	private  String oldNumber = "";

	public void onPrimaryClipChanged()
	{
		String clipboardText;

		try
		{
			ClipboardManager mClipboard = (ClipboardManager) HikeMessengerApp.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
			clipboardText = mClipboard.getPrimaryClip().getItemAt(0).getText().toString();
		}
		catch (Exception e)
		{
			clipboardText = null;
		}

		String number = ChatHeadUtils.getValidNumber(clipboardText);

		if (number != null && !number.equals(oldNumber))
		{
			if (number.length() >= MIN_DIGIT_CONST && number.length() <= MAX_DIGIT_CONST)
			{
				oldNumber= number;
				StickyCaller.CALL_TYPE = StickyCaller.CLIPBOARD;
				ChatHeadUtils.postNumberRequest(HikeMessengerApp.getInstance().getApplicationContext(), number);
			}
		}

	}
}