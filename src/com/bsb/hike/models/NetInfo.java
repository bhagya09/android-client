package com.bsb.hike.models;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Utils;

import android.net.NetworkInfo;
import android.text.TextUtils;

public class NetInfo
{
	
	private NetworkInfo info;
	
	private boolean isAvailable;
	
	private boolean isConnected;
	
	private boolean isConnectedOrConnecting;
	
	private boolean isRoaming;

	private int networkType;
	
	private boolean isWifi;
	
	private boolean isData;
	
	private String ssid;
	
	private NetInfo()
	{
		// TODO Auto-generated constructor stub
	}
	
	public static NetInfo getNetInfo(NetworkInfo info)
	{
		if(null == info)
		{
			return null;
		}
		
		NetInfo netInfo = new NetInfo();
		netInfo.setInfo(info);
		netInfo.setAvailable(info.isAvailable());
		netInfo.setConnectedOrConnecting(info.isConnectedOrConnecting());
		netInfo.setConnected(info.isConnected());
		netInfo.setRoaming(info.isRoaming());
		
		netInfo.setNetworkType(Utils.getNetworkType(HikeMessengerApp.getInstance(), info));
		
		if(netInfo.getNetworkType() == 1)
		{
			netInfo.setWifi(true);
		}
		if(netInfo.getNetworkType() > 1)
		{
			netInfo.setData(true);
		}
		
		netInfo.setSsid(info.getExtraInfo());
		
		
		return netInfo;
	}
	
	public NetworkInfo getInfo()
	{
		return info;
	}

	public void setInfo(NetworkInfo info)
	{
		this.info = info;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}

		if (!(other instanceof NetInfo))
		{
			return false;
		}

		NetInfo info = (NetInfo) other;
		
		// if network type is different returns false
		if (this.getNetworkType() != info.getNetworkType())
		{
			return false;
		}
		
		if((this.isWifi() && info.isWifi())) // both are on wifi
		{
			if(TextUtils.isEmpty(this.getSsid()) && TextUtils.isEmpty(info.getSsid())) // both the ssids values are empty
			{
				return true;
			}
			else if(TextUtils.isEmpty(this.getSsid()) || TextUtils.isEmpty(info.getSsid())) // one of ssids is null
			{
				return false;
			}
			
			return this.getSsid().equals(info.getSsid()); // return true if ssids are equal false otherwise
		}
		
		return true;
	}
	
	@Override
    public String toString() {
        synchronized (this) {
        	
        	if(getInfo() == null)
        	{
        		return null;
        	}
            StringBuilder builder = new StringBuilder("[");
            builder.append("type: ").append(getInfo().getTypeName()).append("[").append(getInfo().getSubtypeName()).
            append("], state: ").append(getInfo().getState()).append("/").append(getInfo().getDetailedState()).
            append(", reason: ").append(getInfo().getReason() == null ? "(unspecified)" : getInfo().getReason()).
            append(", extra: ").append(getSsid() == null ? "(none)" : getSsid()).
            append(", roaming: ").append(isRoaming()).
            append(", failover: ").append(getInfo().isFailover()).
            append(", isAvailable: ").append(isAvailable()).
            append("]");
            return builder.toString();
        }
    }

	public boolean isAvailable()
	{
		return isAvailable;
	}

	public void setAvailable(boolean isAvailable)
	{
		this.isAvailable = isAvailable;
	}

	public boolean isConnected()
	{
		return isConnected;
	}

	public void setConnected(boolean isConnected)
	{
		this.isConnected = isConnected;
	}

	public int getNetworkType()
	{
		return networkType;
	}

	public void setNetworkType(int networkType)
	{
		this.networkType = networkType;
	}

	public boolean isWifi()
	{
		return isWifi;
	}

	public void setWifi(boolean isWifi)
	{
		this.isWifi = isWifi;
	}

	public boolean isData()
	{
		return isData;
	}

	public void setData(boolean isData)
	{
		this.isData = isData;
	}

	public String getSsid()
	{
		return ssid;
	}

	public void setSsid(String ssid)
	{
		this.ssid = ssid;
	}
	
	public boolean isConnectedOrConnecting()
	{
		return isConnectedOrConnecting;
	}

	public void setConnectedOrConnecting(boolean isConnectedOrConnecting)
	{
		this.isConnectedOrConnecting = isConnectedOrConnecting;
	}

	public boolean isRoaming()
	{
		return isRoaming;
	}

	public void setRoaming(boolean isRoaming)
	{
		this.isRoaming = isRoaming;
	}
}
