package com.bsb.hike.modules.httpmgr.request.requestbody;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.filetransfer.FTUtils;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.modules.httpmgr.log.LogFull;
import com.bsb.hike.modules.httpmgr.request.IGetChunkSize;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * Created by sidharth on 11/02/16.
 */
public class FileTransferChunkSizePolicy implements IGetChunkSize
{
	private Context context;
	public static final int DEFAULT_CHUNK_POLICY = 0;
	public static final int NET_SPEED_BASED_CHUNK_POLICY = 1;
	private final String NET_SPEED_AND_TYPE = "nwSpeed_nwType";
	private final String WIFI_BSSID = "wifi_bssid";
	private final String SEPARATOR = "_";
	private final String DEFAULT_VALUE = "NA";
	private int mNetSpeed;
	private final int DEFAULT_CHUNK_SIZE = 50 * 1024; //kb
	private final int THRESHOLD_2G_SPEED = 10; //kbps
	private final int THRESHOLD_3G_SPEED = 32; //kbps
	private final int THRESHOLD_4G_SPEED = 64; //kbps
	private final int THRESHOLD_WIFI_SPEED = 128; //kbps

	public FileTransferChunkSizePolicy(Context ctx)
	{
		this.context = ctx;
	}

	@Override
	public int getChunkSize()
	{
		int chunkSize;
		FileTransferManager.NetworkType networkType = FTUtils.getNetworkType(context);
		if (Utils.scaledDensityMultiplier > 1)
			chunkSize = networkType.getMaxChunkSize();
		else if (Utils.scaledDensityMultiplier == 1)
			chunkSize = networkType.getMinChunkSize() * 2;
		else
			chunkSize = networkType.getMinChunkSize();
		// chunkSize = NetworkType.WIFI.getMaxChunkSize();
		try
		{
			long mem = Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory();
			if (chunkSize > (int) (mem / 8))
				chunkSize = (int) (mem / 8);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return chunkSize;
	}

	@Override
	public int getChunkSize(int policy)
	{
		Logger.d("FileTransferChunkSizePolicy","Chunk size policy : " + policy);
		int chunkSize = DEFAULT_CHUNK_SIZE;
		FileTransferManager.NetworkType networkType = FTUtils.getNetworkType(context);
		Logger.d("FileTransferChunkSizePolicy","Network type : " + networkType);
		switch (policy)
		{
		case DEFAULT_CHUNK_POLICY:
			chunkSize = calculateSizeBasedOnDisplayMetrics(networkType);
			break;
		case NET_SPEED_BASED_CHUNK_POLICY:
			String netSpeedNType = HikeSharedPreferenceUtil.getInstance().getData(NET_SPEED_AND_TYPE, DEFAULT_VALUE);
			if(!netSpeedNType.equals(DEFAULT_VALUE))
			{
				FileTransferManager.NetworkType netType = parseNetType(netSpeedNType);
				int nSpeed = parseNetSpeed(netSpeedNType);
				if(netType == null || nSpeed <= 0)
				{
					chunkSize = DEFAULT_CHUNK_SIZE;
				}
				else if(netType == networkType)
				{
					boolean proceed = true;
					if(netType == FileTransferManager.NetworkType.WIFI)
					{
						WifiManager wifiManager = (WifiManager) HikeMessengerApp.getInstance().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
						WifiInfo wifiInfo = wifiManager.getConnectionInfo();
						if (wifiInfo != null) {
						    String savedBssid = HikeSharedPreferenceUtil.getInstance().getData(WIFI_BSSID, DEFAULT_VALUE);
						    String currentBssid = wifiInfo.getBSSID();
						    Logger.d("FileTransferChunkSizePolicy","Current Wifi BSSID : " + currentBssid);
						    Logger.d("FileTransferChunkSizePolicy","Saved Wifi BSSID : " + savedBssid);
						    if(!savedBssid.equals(currentBssid))
						    {
						    	proceed = false;
						    }
						}
					}
					if(proceed)
					{
						this.mNetSpeed = nSpeed;
						if(isAboveThresholdSpeed(nSpeed, networkType))
						{
							chunkSize = evaluateChunkSizeBasedOnNetSpeed();
						}
						else
						{
							chunkSize = calculateSizeBasedOnDisplayMetrics(networkType);
						}
					}
					else
					{
						chunkSize = DEFAULT_CHUNK_SIZE;
					}
				}
				else
				{
					chunkSize = calculateSizeBasedOnDisplayMetrics(networkType);
				}
			}
			else
			{
				chunkSize = DEFAULT_CHUNK_SIZE;
			}
			break;
		default:
			break;
		}
		chunkSize = reEvaluateBasedOnFreeExpandableHeap(chunkSize);
		Logger.d("FileTransferChunkSizePolicy","Final chunk size  : " + chunkSize);
		return chunkSize;
	}

	/**
	 * Considering the scaled density to estimate optimum chunk size.
	 * @param networkType
	 */
	private int calculateSizeBasedOnDisplayMetrics(FileTransferManager.NetworkType networkType)
	{
		int result;
		if (Utils.scaledDensityMultiplier > 1)
		{
			result = networkType.getMaxChunkSize();
		}
		else if (Utils.scaledDensityMultiplier == 1)
		{
			result = networkType.getMinChunkSize() * 2;
		}
		else
		{
			result = networkType.getMinChunkSize();
		}
		Logger.d("FileTransferChunkSizePolicy","Chunk size based on display metric  : " + result);
		return result;
	}

	/**
	 * Re evaluating the chunk size based on available expandable heap.
	 * @param chunkSize 
	 */
	private int reEvaluateBasedOnFreeExpandableHeap(int chunkSize)
	{
		int result = chunkSize;
		try
		{
			long mem = Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory();
			Logger.d("FileTransferChunkSizePolicy","Available expandable heap  : " + mem);
			if (result > (int) (mem / 8))
			{
				result = (int) (mem / 8);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Calculating chunk size based on network speed.
	 */
	private int evaluateChunkSizeBasedOnNetSpeed()
	{
		int result = (int) (this.mNetSpeed * FileTransferManager.FAKE_PROGRESS_DURATION);
		Logger.d("FileTransferChunkSizePolicy","Chunk size based on NetSpeed  : " + result);
		return result;
	}

	/**
	 * Convert kbps into bpms
	 * @param input - Net speed in kbps
	 * @return speed in bytes per mili second
	 */
	private long convertInBytesPerMiliSecond(int input)
	{
		return ((input * 1024) / 1000);
	}

	/**
	 * Set network speed in unit kbps
	 * @param time in mSec
	 * @param fileSize in bytes
	 */
	public void setNetworkSpeed(long time, long fileSize)
	{
		Logger.d("FileTransferChunkSizePolicy","Time taken  : " + time + " : File Size = " + fileSize);
		this.mNetSpeed = (int)(fileSize/time);
		int nType = Utils.getNetworkType(context);
		String value = this.mNetSpeed + SEPARATOR + nType;
		Logger.d("FileTransferChunkSizePolicy","NetSpeed  : " + this.mNetSpeed + "  : value = " + value);
		HikeSharedPreferenceUtil.getInstance().saveData(NET_SPEED_AND_TYPE, value);
		if(getNetworkType(nType) == FileTransferManager.NetworkType.WIFI)
		{
			WifiManager wifiManager = (WifiManager) HikeMessengerApp.getInstance().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			if (wifiInfo != null) {
			    Logger.d("FileTransferChunkSizePolicy","Wifi BSSID : " + wifiInfo.getBSSID());
			    HikeSharedPreferenceUtil.getInstance().saveData(WIFI_BSSID, wifiInfo.getBSSID());
			}
		}
	}

	/**
	 * Parsing network speed from persisted data
	 */
	private int parseNetSpeed(String input)
	{
		String nSpeed = input.substring(0, input.indexOf(SEPARATOR));
		Logger.d("FileTransferChunkSizePolicy","parse nSpeed  : " + nSpeed);
		int netSpeed = 0;
		try
		{
			netSpeed = Integer.parseInt(nSpeed);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return netSpeed;
	}

	/**
	 * Parsing network type from persisted data
	 */
	private FileTransferManager.NetworkType parseNetType(String input)
	{
		String nType = input.substring(input.indexOf(SEPARATOR) + 1, input.length());
		Logger.d("FileTransferChunkSizePolicy","parse nType  : " + nType);
		int networkType = 0;
		try
		{
			networkType = Integer.parseInt(nType);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
		return getNetworkType(networkType);
	}

	/**
	 * 
	 */
	private FileTransferManager.NetworkType getNetworkType(int networkType)
	{
		switch (networkType)
		{
		case -1:
			return FileTransferManager.NetworkType.NO_NETWORK;
		case 0:
			return FileTransferManager.NetworkType.TWO_G;
		case 1:
			return FileTransferManager.NetworkType.WIFI;
		case 2:
			return FileTransferManager.NetworkType.TWO_G;
		case 3:
			return FileTransferManager.NetworkType.THREE_G;
		case 4:
			return FileTransferManager.NetworkType.FOUR_G;
		default:
			return FileTransferManager.NetworkType.TWO_G;
		}
	}

	private boolean isAboveThresholdSpeed(int speed, FileTransferManager.NetworkType networkType)
	{
		boolean result = false;
		switch (networkType) {
			case WIFI:
				if(speed > THRESHOLD_WIFI_SPEED)
				{
					result = true;
				}
				break;
			case FOUR_G:
				if(speed > THRESHOLD_4G_SPEED)
				{
					result = true;
				}
				break;
			case THREE_G:
				if(speed > THRESHOLD_3G_SPEED)
				{
					result = true;
				}
				break;
			case TWO_G:
				if(speed > THRESHOLD_2G_SPEED)
				{
					result = true;
				}
				break;
			default:
				result = false;
				break;
		}
		return result;
	}
}
