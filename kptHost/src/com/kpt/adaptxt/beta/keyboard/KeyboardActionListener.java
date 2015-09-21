package com.kpt.adaptxt.beta.keyboard;


public interface KeyboardActionListener {

	/**
	 * Called when the user presses a key. This is sent before the {@link #onKey} is called.
	 * For keys that repeat, this is only called once.
	 * @param primaryCode the unicode of the key being pressed. If the touch is not on a valid
	 * key, the value will be zero.
	 */
	void onPress(int primaryCode);

	/**
	 * Called when the user releases a key. This is sent after the {@link #onKey} is called.
	 * For keys that repeat, this is only called once.
	 * @param primaryCode the code of the key that was released
	 */
	void onRelease(int primaryCode);

	/**
	 * Send a key press to the listener.
	 * @param primaryCode this is the key that was pressed
	 * @param keyCodes the codes for all the possible alternative keys
	 * with the primary code being the first. If the primary key code is
	 * a single character such as an alphabet or number or symbol, the alternatives
	 * will include other characters that may be on the same key or adjacent keys.
	 * These codes are useful to correct for accidental presses of a key adjacent to
	 * the intended key.
	 */
	void onKey(int primaryCode, int[] keyCodes, boolean isPopupChar);

	/**
	 * Sends a sequence of characters to the listener.
	 * @param text the sequence of characters to be displayed.
	 */
	void onText(CharSequence text);

	/**
	 * Called when the user quickly moves the finger from right to left.
	 */
	void swipeLeft();

	/**
	 * Called when the user quickly moves the finger from left to right.
	 */
	void swipeRight();

	/**
	 * Called when the user quickly moves the finger from up to down.
	 */
	void swipeDown();

	/**
	 * Called when the user quickly moves the finger from down to up.
	 */
	void swipeUp();

	/**
	 * Called when keyboard settings need to be launched.
	 * Requirement added for adaptxt.
	 * @param class name of activity to be launched
	 */
	void launchSettings(Class settingsClass);

	/**
	 * Called when a key is pressed on Accented bubble launched from HKB
	 * @param primaryCode 
	 * @param keyCodes
	 */
	void onHardKey(int primaryCode, int[] keyCodes);
	
	/**
	 * call this when the glided coordinates has to be send to core to get the glide suggestions
	 * 
	 * @param xCoordinates
	 * @param yCoordinates
	 */
	void onGlideCompleted(float[] xCoordinates, float[] yCoordinates);  

	public static class Adapter implements KeyboardActionListener{

		@Override
		public void onPress(final int primaryCode) {}
		@Override
		public void onRelease(final int primaryCode) {}
		@Override
		public void onKey(final int primaryCode,final  int[] keyCodes,final boolean isPopupChar) {}
		@Override
		public void onText(final CharSequence text) {}
		@Override
		public void swipeLeft() {}
		@Override
		public void swipeRight() {}
		@Override
		public void swipeDown() {}
		@Override
		public void swipeUp() {}
		@Override
		public void launchSettings(final Class settingsClass) {}
		@Override
		public void onHardKey(final int primaryCode, final int[] keyCodes) {}
		@Override
		public void onGlideCompleted(float[] xCoordinates, float[] yCoordinates) {}
	}
}
