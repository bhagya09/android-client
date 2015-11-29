package com.bsb.hike.offline;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.google.gson.annotations.Expose;

/**
 * 
 * @author himanshu
 *	Server side configurable paramters in case of Offline Messaging
 */
public class OfflineParameters
{
	@Expose
	boolean feature = true;

	@Expose
	int gtto = 20000;  // 20 sec

	@Expose
	int scrto = 60000*2; //2 min
	
	@Expose
	int connto = 90000; //1.5 min 
	
	@Expose
	int portno = OfflineConstants.PORT_PING;
	
	@Expose
	int ipreadtimes = 150; //sleep=100-->totoal time =15 sec
	
	@Expose
	int wifionwaittimes = 40; //sleep=500-->totoal time =20sec

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
		return HikeMessengerApp.getInstance().getString((R.string.connecting_to));
	}
	
	public String getStringOnTime8Sec()
	{
		return HikeMessengerApp.getInstance().getString(R.string.offline_animation_second_message);
	}
	
	public String getStringonTime18Sec()
	{
		return HikeMessengerApp.getInstance().getString(R.string.offline_animation_third_message);
	}
	
	public boolean shouldShowHikeDirectOption()
	{
		String options = HikeSharedPreferenceUtil.getInstance().getData(OfflineConstants.HIKE_DIRECT_MENU_OPTIONS, null);
		if (TextUtils.isEmpty(options))
		{
			return false;
		}
		JSONObject ob = null;
		try
		{
			ob = new JSONObject(options);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return false;
		}

		return ob.optBoolean(OfflineConstants.HIKE_DIRECT_SHOW_MENU) && isOfflineEnabled();
	}
	
	public boolean shouldShowConnectingScreen()
	{
		String options = HikeSharedPreferenceUtil.getInstance().getData(OfflineConstants.HIKE_DIRECT_MENU_OPTIONS, null);
		if (TextUtils.isEmpty(options))
		{
			return false;
		}
		JSONObject ob = null;
		try
		{
			ob = new JSONObject(options);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return false;
		}
		return ob.optBoolean(OfflineConstants.HIKE_DIRECT_START_CONN);
	}

}
