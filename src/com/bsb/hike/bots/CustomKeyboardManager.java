package com.bsb.hike.bots;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
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
		String keyboardDataJson = HikeSharedPreferenceUtil.getInstance(CUSTOM_INPUT_BOX_KEY).getData(getKeyboardKey(msisdn), HikePlatformConstants.KEYBOARD_DEFAULT_DATA);

		// Create custom keyboard object from keyboard json received in message
		JsonParser parser = new JsonParser();
		JsonObject keyboardJsonObj = (JsonObject) parser.parse(keyboardDataJson);
		CustomKeyboard customKeyboard = new Gson().fromJson(keyboardJsonObj, CustomKeyboard.class);

		if (customKeyboard != null && customKeyboard.getType() != null && customKeyboard.getType().equals("text"))
		{
			ArrayList<ArrayList<TextKey>> customKeyboardTextKeys = customKeyboard.getTextKeys();

			viewToDisplay = customKeyboardInputBoxAdapter.initTextKeyboardView(customKeyboardTextKeys);
		}
		else if (customKeyboard != null && customKeyboard.getType() != null && customKeyboard.getType().equals("stickers"))
		{
			ArrayList<StkrKey> customKeyboardStkrKeys = customKeyboard.getStkrKeys();

			viewToDisplay = customKeyboardInputBoxAdapter.initStickerKeyboardView(customKeyboardStkrKeys,customKeyboard.getStSize());
		}

		if (viewToDisplay == null)
			viewToDisplay = LayoutInflater.from(context).inflate(R.layout.custom_keyboard_layout, null);

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
			Logger.i("tagcontroller", "JSONException while saving data in shared preferences");
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
        customKeyboardInputBoxAdapter.releaseResources();
    }

}