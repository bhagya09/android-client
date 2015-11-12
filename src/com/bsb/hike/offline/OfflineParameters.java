package com.bsb.hike.offline;

import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.google.gson.annotations.Expose;

/**
 * 
 * @author himanshu
 *	Server side configurable paramters in case of Offline Messaging
 */
public class OfflineParameters
{
	@Expose
	boolean feature = false;

	@Expose
	int gtto = 20000;  // 20 sec

	@Expose
	int scrto = 60000*2; //2 min
	
	@Expose
	int connto = 60000; //1 min 
	
	@Expose
	int portno = OfflineConstants.PORT_PING;
	
	@Expose
	int ipreadtimes = 150; //sleep=100-->totoal time =15 sec
	
	@Expose
	int wifionwaittimes = 40; //sleep=500-->totoal time =20sec
	
	@Expose
	String strOnTime0=null;
	
	@Expose
	String strOnTime8=null;
	
	@Expose
	String strOnTime18=null;

	public boolean isOfflineEnabled()
	{
		return feature;
	}

	public int getHeartBeatTimeout()
	{
		return gtto;
	}

	public int getKeepAliveScreenTimeout()
	{
		return scrto;
	}
	
	public int getConnectionTimeout()
	{
		return connto;
	}
	
	public int getPortNo()
	{
		return portno;
	}
	
	public int getMaxTryForIpExtraction()
	{
		return ipreadtimes;
	}
	
	public int getMaxWifiwaitTime()
	{
		return wifionwaittimes;
	}
	
	public String getInitialString()
	{
		if (TextUtils.isEmpty(strOnTime0))
		{
			strOnTime0 = HikeMessengerApp.getInstance().getString((R.string.connecting_to));
		}
		return strOnTime0;
	}
	
	public String getStringOnTime8Sec()
	{
		if (TextUtils.isEmpty(strOnTime8))
		{
			strOnTime8 = HikeMessengerApp.getInstance().getString(R.string.offline_animation_second_message);
		}
		return strOnTime8;
	}
	
	public String getStringonTime18Sec()
	{
		if (TextUtils.isEmpty(strOnTime18))
		{
			strOnTime18 = HikeMessengerApp.getInstance().getString(R.string.offline_animation_third_message);
		}
		return strOnTime18;
	}

}
