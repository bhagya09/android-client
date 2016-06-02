package com.bsb.hike.bots;

import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.view.View;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.adapters.CustomKeyboardInputBoxAdapter;
import com.bsb.hike.media.ShareablePopup;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * The type Custom keyboard manager.
 */
public class CustomKeyboardManager implements ShareablePopup, CustomKeyboardTextPickerListener, CustomKeyboardStickerPickerListener
{

	private CustomKeyboardTextPickerListener customKeyboardTextPickerListener;

	private CustomKeyboardStickerPickerListener customKeyboardStickerPickerListener;

	/**
	 * The constant CUSTOM_INPUT_BOX_KEY.
	 */
	public static final String CUSTOM_INPUT_BOX_KEY = "customInputBox";

	private static final CustomKeyboardManager customKeyboardManager = new CustomKeyboardManager();

	private CustomKeyboardInputBoxAdapter customKeyboardInputBoxAdapter;

	private View viewToDisplay;

    private ConcurrentHashMap<String,Boolean> botsKeyboardsDisplayMap = new ConcurrentHashMap<>();

    private String msisdn;

    private Context context;

    private int currentConfig = Configuration.ORIENTATION_PORTRAIT;

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
	 * @param customKeyboardTextPickerListener
	 *            the text picker listener
	 * @param customKeyboardStickerPickerListener
	 *            the sticker picker listener
	 * @param msisdn
	 *            the bot msisdn
	 */
	public void initInputBox(Context context, CustomKeyboardTextPickerListener customKeyboardTextPickerListener, CustomKeyboardStickerPickerListener customKeyboardStickerPickerListener, String msisdn)
	{
        // Removing listening pubsubs on previous adapter so that gc can remove previous instance
        if(customKeyboardInputBoxAdapter != null)
            customKeyboardInputBoxAdapter.releaseResources();

        this.context = context;
        this.customKeyboardTextPickerListener = customKeyboardTextPickerListener;
		this.customKeyboardStickerPickerListener = customKeyboardStickerPickerListener;
        this.msisdn = msisdn;
		customKeyboardInputBoxAdapter = new CustomKeyboardInputBoxAdapter(context, customKeyboardTextPickerListener, customKeyboardStickerPickerListener);

        CustomKeyboard customKeyboard = getCustomKeyboardObject(msisdn);
		if (customKeyboard != null && customKeyboard.getT() != null && customKeyboardInputBoxAdapter != null && customKeyboard.getT().equals(HikePlatformConstants.BOT_CUSTOM_KEYBOARD_TYPE_TEXT))
		{
			ArrayList<ArrayList<Tk>> customKeyboardTextKeys = customKeyboard.getTk();

			viewToDisplay = customKeyboardInputBoxAdapter.initTextKeyboardView(customKeyboardTextKeys);
		}
		else if (customKeyboard != null && customKeyboard.getT() != null && customKeyboardInputBoxAdapter != null && customKeyboard.getT().equals(HikePlatformConstants.BOT_CUSTOM_KEYBOARD_TYPE_STICKER))
		{
			ArrayList<Sk> customKeyboardSks = customKeyboard.getSk();

			viewToDisplay = customKeyboardInputBoxAdapter.initStickerKeyboardView(customKeyboardSks);
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
        if (orientationChanged(screenOrientation))
        {
            Logger.i(getClass().getSimpleName(), "Orientation Changed");
            initInputBox(this.context,this.customKeyboardTextPickerListener,this.customKeyboardStickerPickerListener,this.msisdn);
            currentConfig = screenOrientation;
        }

        return viewToDisplay;
	}

    private boolean orientationChanged(int deviceOrientation)
    {
        return currentConfig != deviceOrientation;
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
	public boolean isInputBoxButtonShowing(String msisdn)
	{
		if(TextUtils.isEmpty(msisdn))
            return false;

        Boolean isInputBoxShowing = botsKeyboardsDisplayMap.get(msisdn);

        if(isInputBoxShowing == null)
            return false;
        else
            return isInputBoxShowing;
	}

	/**
	 * Sets input box button showing.
	 *
	 * @param isInputBoxButtonShowing
	 *            the is input box button showing
	 */
	public void setInputBoxButtonShowing(String msisdn,boolean isInputBoxButtonShowing)
	{
        if(!TextUtils.isEmpty(msisdn))
           botsKeyboardsDisplayMap.put(msisdn,isInputBoxButtonShowing);
	}
    
	/**
	 * Gets custom keyboard object.
	 *
	 * @return the custom keyboard object
	 */
	public CustomKeyboard getCustomKeyboardObject(String msisdn)
	{
		String keyboardDataJson = HikeSharedPreferenceUtil.getInstance(CustomKeyboardManager.CUSTOM_INPUT_BOX_KEY).getData(getKeyboardKey(msisdn),
				HikePlatformConstants.KEYBOARD_DEFAULT_DATA);

		if (TextUtils.isEmpty(keyboardDataJson) || keyboardDataJson.equals(HikePlatformConstants.KEYBOARD_DEFAULT_DATA))
			return null;

		JsonParser jsonParser = new JsonParser();
		JsonObject keyboardJsonObj = null;
		try
		{
			keyboardJsonObj = (jsonParser.parse(keyboardDataJson)).getAsJsonObject();
		}
		catch (JsonSyntaxException e)
		{
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
	public void onTextClicked(String string)
	{
		customKeyboardTextPickerListener.onTextClicked(string);
	}

	/**
	 * Release resources.
	 */
	public void releaseResources()
    {
        if(customKeyboardInputBoxAdapter != null)
            customKeyboardInputBoxAdapter.releaseResources();
    }

    @Override
	public void onCustomKeyboardStickerClicked(Sticker sticker)
	{
		customKeyboardStickerPickerListener.onCustomKeyboardStickerClicked(sticker);
	}
}