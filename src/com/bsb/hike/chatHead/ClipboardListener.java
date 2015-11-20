package com.bsb.hike.chatHead;

import com.bsb.hike.HikeMessengerApp;
import android.content.ClipboardManager;
import android.content.ClipboardManager.OnPrimaryClipChangedListener;
import android.content.Context;

public class ClipboardListener implements OnPrimaryClipChangedListener
{
	private int MIN_DIGIT_CONST = 6;

	private int MAX_DIGIT_CONST = 13;

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
		if (clipboardText != null)
		{
			String regex = "^(\\s*\\+?(\\d{1,3}\\s?\\-?){3,6}\\s*)$";
			if (clipboardText.matches(regex))
			{
				String number = "";
				for (int var = 0; var < clipboardText.length(); var++)
				{
					if (Character.isDigit(clipboardText.charAt(var)) || (clipboardText.charAt(var) == '+'))
					{
						number = number + clipboardText.charAt(var);
					}
				}
				if (number.length() >= MIN_DIGIT_CONST && number.length() <= MAX_DIGIT_CONST)
				{
					StickyCaller.CALL_TYPE = StickyCaller.CLIPBOARD;
					ChatHeadUtils.postNumberRequest(HikeMessengerApp.getInstance().getApplicationContext(), number);
				}
			}
		}
	}
}