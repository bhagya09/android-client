package com.bsb.hike.bots;

import java.util.ArrayList;

/**
 * Created by konarkarora on 01/05/16.
 */
public class CustomKeyboard
{

	private boolean keep;

	private String t;

	private boolean remove;

    private boolean hidden;

	private ArrayList<ArrayList<Tk>> tk = new ArrayList<ArrayList<Tk>>();

	private ArrayList<Sk> sk = new ArrayList<Sk>();

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
	public String getT()
	{
		return t;
	}

	/**
	 *
	 * @param t
	 *            The type
	 */
	public void setT(String t)
	{
		this.t = t;
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
	 * @return The tk
	 */
	public ArrayList<ArrayList<Tk>> getTk()
	{
		return tk;
	}

	/**
	 *
	 * @param tk
	 *            The tk
	 */
	public void setTk(ArrayList<ArrayList<Tk>> tk)
	{
		this.tk = tk;
	}

	/**
	 *
	 * @return The sk
	 */
	public ArrayList<Sk> getSk()
	{
		return sk;
	}

	/**
	 *
	 * @param sk
	 *            The sk
	 */
	public void setSk(ArrayList<Sk> sk)
	{
		this.sk = sk;
	}

	/**
	 * Is hidden boolean.
	 *
	 * @return the boolean
	 */
	public boolean isHidden() {
        return hidden;
    }

	/**
	 * Sets hidden.
	 *
	 * @param hidden
	 *            the hidden
	 */
	public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}