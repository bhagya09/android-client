package com.bsb.hike.models;


/**
 * 
 * @author himanshu
 * 
 * This class provides all the details to the custom message activity.
 *
 */

public class NuxCustomMessage
{
	private String customMessage;

	private String indicatorText;

	private String buttext;

	private boolean togglescreen;

	public NuxCustomMessage(String screentitle, String smsmessage, String buttext, boolean togglecustommsg)
	{
		super();
		this.customMessage = screentitle;
		this.indicatorText = smsmessage;
		this.buttext = buttext;
		this.togglescreen = togglecustommsg;
	}

	/**
	 * @return the screentitle
	 */
	public String getCustomMessage()
	{
		return customMessage;
	}

	/**
	 * @return the smsmessage
	 */
	public String getIndicatorText()
	{
		return indicatorText;
	}

	/**
	 * @return the buttext
	 */
	public String getButText()
	{
		return buttext;
	}

	/**
	 * @return the togglecustommsg
	 */
	public boolean isToggleCustomMsg()
	{
		return togglescreen;
	}


}
