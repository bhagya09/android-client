package com.bsb.hike.offline;

import com.bsb.hike.utils.Utils;
import com.google.gson.annotations.Expose;

/**
 * 
 * @author sahil
 *	Client info for offline connection
 */
public class OfflineClientInfoPOJO
{
	@Expose
	String v = "4.0.6";
	
	@Expose
	int ov = 1;
	
	@Expose
	int resId = Utils.getResolutionId() ;
	
	@Expose
	long conn_id = System.currentTimeMillis();
	
	public String getHikeVersion()
	{
		return v;
	}

	public int getOfflineVersionNumber()
	{
		return ov;
	}

	public int getResolutionId()
	{
		return resId;
	}

	public long getConnectionId()
	{
		return conn_id;
	}

}
