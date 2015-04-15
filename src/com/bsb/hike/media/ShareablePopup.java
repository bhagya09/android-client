package com.bsb.hike.media;

import android.view.View;

public interface ShareablePopup
{
	/**
	 * Get the view based on the current device orientation.
	 * 
	 * @param screenOrientation
	 * @return
	 */
	public View getView(int screenOrientation);

	public int getViewId();

}