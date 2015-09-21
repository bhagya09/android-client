package com.kpt.adaptxt.beta;


public interface AdaptxtSettings {
	//equivalent to boolean true
	public static int KPT_TRUE = 0;
	// equivalent to boolean false
	public static int KPT_FALSE = 1;
	
	public static final int ATR_ERROR_NULL = -1;
	public static final int ATR_ERROR_SHORTCUT_SHORT = 1;
	public static final int ATR_ERROR_SHORTCUT_LONG = 2;
	public static final int ATR_ERROR_EXPANSION_SHORT = 3;
	public static final int ATR_INTERNAL_ERROR = 4;
	
	
	public static int KPT_SUCCESS = 0;
	// check if the context is invalid
	public static int KPT_ERROR_NOT_INITIALIZED = -1;
	// just the string value of the above constant
	public static String KPT_ERROR_NOT_INITIALIZED_STRING = "KPT_ERROR_NOT_INITIALIZED_STRING";
	// could not commit the value
	public static int KPT_ERROR_CAN_NOT_COMMIT = -2;
	
// Getters
	public int getPrivateModeState();
	public int getGlideState();
	public int getAutoCapitalizationState();
	public int getAutoSpacingState();
	public int getDisplaySuggestionsState();
	public int getDisplayAccentsState();
	public int getPopupOnKeyPressState();
	public int getSoundOnKeyPressState();
	public int getVibrateOnKeyPressState();
	
	public int getLongPressDuration();
	public float getKeyPressSoundVolume();
	public int getKeyPressVibrationDuration();
//	public int getCurrentThemeId();
	
	public String getPortraitKeyboardType();
	public int getAutoCorrectionState();
//	public String getLandscapeKeyboardType();
	
	
// Setters : Write the ranges in descriptions, validate the input in each method based on the ranges
	// Throw the exceptions or return error values accordingly
	
	public int setAutoCorrectionState(int isAutoCorrectitonOn);
	
	public int setPrivateModeState(int isPrivateModeOn);
	public int setGlideState(int isGlideOn);
	public int setAutoCapitalizationState(int isAutoCapitalizationOn);
	public int setAutoSpacingState(int isAutoSpacingOn);
	public int setDisplaySuggestionsState(int isDisplaySuggestionsOn);
	public int setDisplayAccentsState(int isDisplayAccentsOn);
	public int setPopupOnKeyPressState(int isPopupOnKeyPressOn);
	public int setSoundOnKeyPressState(int isSoundOnKeyPressOn);
	public int setVibrateOnKeyPressState(int isVibrateOnKeyPressOn);
	
	public int setLongPressDuration(int duration);
	public int setKeyPressSoundVolume(float volume);
	public int setKeyPressVibrationDuration(int duration);
	
	public int setPortraitKeyboardType(String keyboardType);
//	public int setLandscapeKeyboardType(String keyboardType);
	
	public int removeAccents();
	
	public int setHideSuggestionBarState(int isHideSuggBarOn);
	public int getHideSuggestionBarState();
	
}
