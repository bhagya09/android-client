package com.kpt.adaptxt.beta.glide;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import com.kpt.adaptxt.beta.AdaptxtIME;
import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.keyboard.Key;
import com.kpt.adaptxt.beta.util.KPTConstants;


public class KPTGlideKBShortcuts {

	private static KPTGlideKBShortcuts mKptGlideKBShortcuts;
	private Context mContext;

	public static KPTGlideKBShortcuts getInstance() {
		if(mKptGlideKBShortcuts == null) {
			mKptGlideKBShortcuts = new KPTGlideKBShortcuts();
		}
		return mKptGlideKBShortcuts;
	}

	public void setService(AdaptxtIME service,Context con) {
		this.mContext = con;
	}

	/**
	 * detect the shortcut based on the glide start key ie.. down key
	 * @param downKey key on which the glide is started
	 * @param upKey key on which the glide has ended
	 * @return whether a shortcut has been detected or not
	 */
	public boolean detectShortcut(Key downKey, Key upKey) {
		boolean result = false;
		if(downKey != null) {
			int downKeyCode = downKey.codes[0];
			switch (downKeyCode) {
			case KPTConstants.KEYCODE_MODE_CHANGE:
			case KPTConstants.KEYCODE_JUMP_TO_TERTIARY:
				/*if(upKey != null) {
					result = adaptxtKeyShortcuts(upKey);
				}
				break;
				case Keyboard.KEYCODE_MIC:
				if(upKey != null) {
					result = commaKeyShortcuts(upKey);
				}
				break;
			case Keyboard.KEYCODE_PERIOD:
				if(upKey != null) {
					result = dotKeyShortcuts(upKey);
				}
				break;*/
			default:
				result = false;
			}
		}
		return result;
	}

	/**
	 * key short cuts for Adaptxt key
	 * @param upKey
	 */
	private boolean adaptxtKeyShortcuts(Key upKey) {
		String keycode = String.valueOf(upKey.codes[0]);
		
		//the key should be checked for both caps and small char
		String strCaps = Character.toString((char) Integer.parseInt(keycode)).toUpperCase();
		String strSmall = Character.toString((char) Integer.parseInt(keycode)).toLowerCase();
		int unicodeCaps = (int)strCaps.charAt(0);
		int unicodeSmall = (int)strSmall.charAt(0);
		
		//Log.e("kpt","str caps-->"+strCaps+"<--star small-->"+strSmall+"<--");
		//Log.e("kpt","unicode small-->"+unicodeSmall+"<--unicode caps-->"+unicodeCaps);
		
		KPTCustomGesturePreferences customPrefs = KPTCustomGesturePreferences.getCustomGesturePreferences();
		if(customPrefs.chechIfInApplicationPreference(keycode)) {
			String value = (String) customPrefs.getOnlyApplicationsPreferences().get(String.valueOf(unicodeSmall));
			if(value == null) {
				value = (String) customPrefs.getOnlyApplicationsPreferences().get(String.valueOf(unicodeCaps));
			}
			
			String packageName = value.split(KPTCustomGesturePreferences.DELIMITER)[1];
			try {
				Intent i = new Intent();
				PackageManager manager = mContext.getPackageManager();
				i = manager.getLaunchIntentForPackage(packageName);
				if(i != null) {
					i.addCategory(Intent.CATEGORY_LAUNCHER);
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					mContext.startActivity(i);
				} else {
					Toast.makeText(mContext, mContext.getResources().getString(R.string.kpt_UI_STRING_TOAST_2_220), Toast.LENGTH_LONG).show();
				}
			} catch(ActivityNotFoundException e) {
				Toast.makeText(mContext, mContext.getResources().getString(R.string.kpt_UI_STRING_TOAST_2_220), Toast.LENGTH_LONG).show();
			}

		} else if(customPrefs.chechIfInWebsitePreference(keycode)) {
			String url = (String) customPrefs.getOnlyWebsitesPreferences().get(String.valueOf(unicodeSmall));
			if(url == null) {
				url = (String) customPrefs.getOnlyWebsitesPreferences().get(String.valueOf(unicodeCaps));
			}
			
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"+url));
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(intent);
			
		} /*else if(customPrefs.chechIfInClipboardPreference(keycode)) {
			String clipShortcut = (String)customPrefs.getOnlyClipboardPreferences().get(String.valueOf(unicodeSmall));
			if(clipShortcut == null) {
				clipShortcut = (String)customPrefs.getOnlyClipboardPreferences().get(String.valueOf(unicodeCaps));
			}
			
			int clipboardAction = Integer.parseInt(clipShortcut);
			switch (clipboardAction) {
			case KPTGlideCustomGesturesActivity.CLIPBOARD_ACTION_CUT:
				mImeService.clipboardCut();
				break;
			case KPTGlideCustomGesturesActivity.CLIPBOARD_ACTION_COPY:
				mImeService.clipboardCopy();
				break;
			case KPTGlideCustomGesturesActivity.CLIPBOARD_ACTION_PASTE:
				mImeService.clipboardPaste();
				break;
			case KPTGlideCustomGesturesActivity.CLIPBOARD_ACTION_SELECTALL:
				mImeService.clipboardSelectAll();
				break;
			case KPTGlideCustomGesturesActivity.CLIPBOARD_ACTION_SELECT:
				mImeService.clipboardSelectWord();
				break;
			case KPTGlideCustomGesturesActivity.CLIPBOARD_ACTION_HIDE_KEYBOARD:
				mImeService.hideKeyboard();
				break;
			case KPTGlideCustomGesturesActivity.CLIPBOARD_ACTION_BEGI_MSG:
				mImeService.clipboardBeginningOfMessage();
				break;
			case KPTGlideCustomGesturesActivity.CLIPBOARD_ACTION_END_MSG:
				mImeService.clipboardEndOfMessage();
				break;
			}
		} */else {
			//this short cut is not mapped
			return false;
		}
		return true;
	}
}
