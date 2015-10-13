package com.bsb.hike.chatHead;

import com.bsb.hike.HikeMessengerApp;
import android.content.ClipboardManager;
import android.content.ClipboardManager.OnPrimaryClipChangedListener;
import android.content.Context;

public class ClipboardListener implements OnPrimaryClipChangedListener
{
	public void onPrimaryClipChanged()
	{
		ClipboardManager mClipboard = (ClipboardManager) HikeMessengerApp.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
		String clipboardText = mClipboard.getPrimaryClip().getItemAt(0).getText().toString();
		if (clipboardText != null)
		{
			String regex = "^\\s*\\+?(\\d{1,5}\\s?\\-?){1,6}\\s*$";
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
				StickyCaller.CALL_TYPE = StickyCaller.CLIPBOARD;
				ChatHeadUtils.postNumberRequest(HikeMessengerApp.getInstance().getApplicationContext(), number);
			}
		}
	}
}