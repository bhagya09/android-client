package com.bsb.hike.notifications;

import com.google.gson.annotations.Expose;

public class NotificationRetryCountModel
{

	@Expose
	int h2h=3;
	
	@Expose
	int gc=1;
	
	@Expose
	int hidden=2;
	
	@Expose
	int hikebot=-1;
	
	@Expose
	int chtThm;
	
	@Expose
	int sttsUpt;
	
	@Expose
	int dpUpt;
	
	@Expose
	int nuj;
	
	@Expose
	int fav;
	
	@Expose
	int h2o;
	
	@Expose
	int ac_up = 3;
	
	@Expose
	int other;

	/**
	 * @return the h2h
	 */
	public int getH2h()
	{
		return h2h;
	}

	/**
	 * @return the gc
	 */
	public int getGc()
	{
		return gc;
	}

	/**
	 * @return the hidden
	 */
	public int getHidden()
	{
		return hidden;
	}

	/**
	 * @return the hikeBot
	 */
	public int getHikeBot()
	{
		return hikebot;
	}

	/**
	 * @return the chatThemeChange
	 */
	public int getChatThemeChange()
	{
		return chtThm;
	}

	/**
	 * @return the statusUpdate
	 */
	public int getStatusUpdate()
	{
		return sttsUpt;
	}

	/**
	 * @return the dpUpdate
	 */
	public int getDpUpdate()
	{
		return dpUpt;
	}

	/**
	 * @return the nuj
	 */
	public int getNuj()
	{
		return nuj;
	}

	/**
	 * @return the fav
	 */
	public int getFav()
	{
		return fav;
	}

	/**
	 * @return the h2o
	 */
	public int getH2o()
	{
		return h2o;
	}

	/**
	 * @return the other
	 */
	public int getOther()
	{
		return other;
	}
	
	/**
	 * Activity updates
	 * @return
	 */
	public int getAcUp()
	{
		return ac_up;
	}
	
	
	
}
