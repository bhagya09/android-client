package com.bsb.hike.bots;

import java.util.ArrayList;

/**
 * Created by konarkarora on 01/05/16.
 */
public class CustomKeyboard
{

	private boolean keep;

	private String type;

	private boolean remove;

	private String stSize;

	private ArrayList<ArrayList<TextKey>> textKeys = new ArrayList<ArrayList<TextKey>>();

	private ArrayList<StkrKey> stkrKeys = new ArrayList<StkrKey>();

	/**
	 *
	 * @return The keep
	 */
	public boolean getKeep()
	{
		return keep;
	}

	/**
	 *
	 * @param keep
	 *            The keep
	 */
	public void setKeep(boolean keep)
	{
		this.keep = keep;
	}

	/**
	 *
	 * @return The type
	 */
	public String getType()
	{
		return type;
	}

	/**
	 *
	 * @param type
	 *            The type
	 */
	public void setType(String type)
	{
		this.type = type;
	}

	/**
	 *
	 * @return The remove
	 */
	public boolean getRemove()
	{
		return remove;
	}

	/**
	 *
	 * @param remove
	 *            The remove
	 */
	public void setRemove(boolean remove)
	{
		this.remove = remove;
	}

	/**
	 *
	 * @return The textKeys
	 */
	public ArrayList<ArrayList<TextKey>> getTextKeys()
	{
		return textKeys;
	}

	/**
	 *
	 * @param textKeys
	 *            The textKeys
	 */
	public void setTextKeys(ArrayList<ArrayList<TextKey>> textKeys)
	{
		this.textKeys = textKeys;
	}

	/**
	 *
	 * @return The stkrKeys
	 */
	public ArrayList<StkrKey> getStkrKeys()
	{
		return stkrKeys;
	}

	/**
	 *
	 * @param stkrKeys
	 *            The stkrKeys
	 */
	public void setStkrKeys(ArrayList<StkrKey> stkrKeys)
	{
		this.stkrKeys = stkrKeys;
	}

	/**
	 * Gets st size.
	 *
	 * @return the st size
	 */
	public String getStSize()
	{
		return stSize;
	}

	/**
	 * Sets st size.
	 *
	 * @param stSize
	 *            the st size
	 */
	public void setStSize(String stSize)
	{
		this.stSize = stSize;
	}
}