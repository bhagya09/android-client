package com.bsb.hike.bots;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.adapters.CustomKeyboardInputBoxAdapter;
import com.bsb.hike.media.ShareablePopup;
import com.bsb.hike.media.StickerPickerListener;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * The type Custom keyboard manager.
 */
public class CustomKeyboardManager implements ShareablePopup, TextPickerListener, StickerPickerListener
{

	private TextPickerListener textPickerListener;

	private StickerPickerListener stickerPickerListener;

	private boolean isInputBoxButtonShowing = false;

	/**
	 * The constant CUSTOM_INPUT_BOX_KEY.
	 */
	public static final String CUSTOM_INPUT_BOX_KEY = "customInputBox";

	private static final CustomKeyboardManager customKeyboardManager = new CustomKeyboardManager();

	private CustomKeyboardInputBoxAdapter customKeyboardInputBoxAdapter;

	private View viewToDisplay;

	private CustomKeyboardManager()
	{

	}

	/**
	 * Gets instance.
	 *
	 * @return the instance
	 */
	public static CustomKeyboardManager getInstance()
	{
		return customKeyboardManager;
	}

	/**
	 * Init input box.
	 *
	 * @param context
	 *            the context
	 * @param textPickerListener
	 *            the text picker listener
	 * @param stickerPickerListener
	 *            the sticker picker listener
	 * @param msisdn
	 *            the bot msisdn
	 */
	public void initInputBox(Context context, TextPickerListener textPickerListener, StickerPickerListener stickerPickerListener, String msisdn)
	{
		this.textPickerListener = textPickerListener;
		this.stickerPickerListener = stickerPickerListener;
		customKeyboardInputBoxAdapter = new CustomKeyboardInputBoxAdapter(context, textPickerListener, stickerPickerListener);

        CustomKeyboard customKeyboard = getCustomKeyboardObject(msisdn);
		if (customKeyboard != null && customKeyboard.getType() != null && customKeyboardInputBoxAdapter != null && customKeyboard.getType().equals(HikePlatformConstants.BOT_CUSTOM_KEYBOARD_TYPE_TEXT))
		{
			ArrayList<ArrayList<TextKey>> customKeyboardTextKeys = customKeyboard.getTextKeys();

			viewToDisplay = customKeyboardInputBoxAdapter.initTextKeyboardView(customKeyboardTextKeys);
		}
		else if (customKeyboard != null && customKeyboard.getType() != null && customKeyboardInputBoxAdapter != null && customKeyboard.getType().equals(HikePlatformConstants.BOT_CUSTOM_KEYBOARD_TYPE_STICKER))
		{
			ArrayList<StkrKey> customKeyboardStkrKeys = customKeyboard.getStkrKeys();

			viewToDisplay = customKeyboardInputBoxAdapter.initStickerKeyboardView(customKeyboardStkrKeys,customKeyboard.getStSize());
		}

	}

	/**
	 * Should show input box boolean.
	 *
	 * @param msisdn
	 *            the msisdn
	 * @return the boolean
	 */
	public boolean shouldShowInputBox(String msisdn)
	{
		return !(HikeSharedPreferenceUtil.getInstance(CUSTOM_INPUT_BOX_KEY).getData(getKeyboardKey(msisdn), HikePlatformConstants.KEYBOARD_DEFAULT_DATA)
				.equals(HikePlatformConstants.KEYBOARD_DEFAULT_DATA));
	}

	/**
	 * Gets keyboard key.
	 *
	 * @param msisdn
	 *            the msisdn
	 * @return the keyboard key
	 */
	public static String getKeyboardKey(String msisdn)
	{
		return msisdn + CUSTOM_INPUT_BOX_KEY;
	}

	@Override
	public View getView(int screenOrientation)
	{
		return viewToDisplay;
	}

	@Override
	public int getViewId()
	{
		return viewToDisplay.getId();
	}

	/**
	 * Is input box button showing boolean.
	 *
	 * @return the boolean
	 */
	public boolean isInputBoxButtonShowing()
	{
		return isInputBoxButtonShowing;
	}

	/**
	 * Sets input box button showing.
	 *
	 * @param isInputBoxButtonShowing
	 *            the is input box button showing
	 */
	public void setInputBoxButtonShowing(boolean isInputBoxButtonShowing)
	{
		this.isInputBoxButtonShowing = isInputBoxButtonShowing;
	}
    
	/**
	 * Gets custom keyboard object.
	 *
	 * @return the custom keyboard object
	 */
	public CustomKeyboard getCustomKeyboardObject(String msisdn)
    {
        String keyboardDataJson = HikeSharedPreferenceUtil.getInstance(CustomKeyboardManager.CUSTOM_INPUT_BOX_KEY).getData(getKeyboardKey(msisdn), HikePlatformConstants.KEYBOARD_DEFAULT_DATA);

        if(TextUtils.isEmpty(keyboardDataJson) || keyboardDataJson.equals(HikePlatformConstants.KEYBOARD_DEFAULT_DATA))
            return null;

        JsonParser jsonParser = new JsonParser();
        JsonObject keyboardJsonObj = null;
        try {
            keyboardJsonObj = (jsonParser.parse(keyboardDataJson)).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        CustomKeyboard customKeyboard = new Gson().fromJson(keyboardJsonObj, CustomKeyboard.class);
        return customKeyboard;
    }
    
	/**
	 * Save to shared preferences.
	 *
	 * @param msisdn
	 *            the msisdn
	 * @param data
	 *            the data
	 */
	public void saveToSharedPreferences(String msisdn, JSONObject data)
	{
		try
		{
			HikeSharedPreferenceUtil.getInstance(CUSTOM_INPUT_BOX_KEY).saveData(getKeyboardKey(msisdn), data.getJSONObject(HikeConstants.KEYBOARD_DATA).toString());
		}
		catch (JSONException e)
		{
			Logger.i(getClass().getSimpleName(), "JSONException while saving data in shared preferences");
			e.printStackTrace();
		}
	}

	/**
	 * Remove from shared preferences.
	 *
	 * @param msisdn
	 *            the msisdn
	 */
	public void removeFromSharedPreferences(String msisdn)
	{
		HikeSharedPreferenceUtil.getInstance(CUSTOM_INPUT_BOX_KEY).removeData(getKeyboardKey(msisdn));
	}

	@Override
	public void stickerSelected(Sticker sticker, String source)
	{
		stickerPickerListener.stickerSelected(sticker, source);
	}

	@Override
	public void onTextClicked(String string)
	{
		textPickerListener.onTextClicked(string);
	}

	/**
	 * Release resources.
	 */
	public void releaseResources()
    {
        if(customKeyboardInputBoxAdapter != null)
            customKeyboardInputBoxAdapter.releaseResources();
    }

}