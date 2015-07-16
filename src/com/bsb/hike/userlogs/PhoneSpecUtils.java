package com.bsb.hike.userlogs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;

/**
 * @author ashishagarwal
 * 
 *         Will provide the specs like ram space, disk space etc.
 * 
 */
public class PhoneSpecUtils
{

	public static final String DATE = "date";

	public static final String ELAPSED_TIME = "elapsedTime";

	public static final String AUTO_TIME_SET = "isAutoTimeSet";

	public static final String INTERNAL_MEMORY = "intMem";

	public static final String DATA_MEMORY = "dataMem";

	public static final String SD_MEMORY = "SDMem";

	public static final String CACHE_MEMORY = "cacheMem";

	public static final String RAM = "ram";

	public static final String PHONE_SPEC = "Phone Spec";

	public static final String TOTAL = "total";

	public static final String FREE = "free";

	public static final String BUSY = "busy";

	public static final String THRESHOLD = "threshold";

	public static final String LOW = "low";

	/**
	 * retuns the phonespec at the current instance of time
	 */

	public static JSONArray getPhoneSpec()
	{
		Context context = HikeMessengerApp.getInstance();
		JSONArray phoneSpecArray = new JSONArray();
		JSONObject phoneSpec = new JSONObject();
		try
		{
			phoneSpec.put(DATE, (System.currentTimeMillis()/1000));
			phoneSpec.put(ELAPSED_TIME, (SystemClock.elapsedRealtime()/1000));
			phoneSpec.put(AUTO_TIME_SET, isAutomaticDateAndTimeSet(context));
			phoneSpec.put(INTERNAL_MEMORY, new JSONObject(getInternalMem()));
			phoneSpec.put(DATA_MEMORY, new JSONObject(getDataMem()));
			phoneSpec.put(SD_MEMORY, new JSONObject(getSDCardMem()));
			phoneSpec.put(CACHE_MEMORY, new JSONObject(getCacheMem()));
			phoneSpec.put(RAM, new JSONObject(getRamSize(context)));
			Logger.d(PHONE_SPEC, phoneSpec.toString());
			return phoneSpecArray.put(phoneSpec);
		}
		catch (JSONException e)
		{
			return null;
		}
	}

	/**
	 * returns the busy, free and total Cache memory in bytes
	 */
	private static Map<String,Long> getCacheMem()
	{
		StatFs statFs = new StatFs(Environment.getDownloadCacheDirectory().getAbsolutePath());
		return getMemory(statFs);
	}

	/**
	 * returns the Ram details of the Device
	 */
	private static Map<String, Object> getRamSize(Context context)
	{
		long totalRamBytes;
		Map<String, Object> ram = new HashMap<String,Object>();
		ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		android.app.ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
		actManager.getMemoryInfo(memInfo);
		if (Utils.isJellybeanOrHigher())
		{
			totalRamBytes = memInfo.totalMem;
		}
		else
		{
			try
			{
				totalRamBytes = getTotalMemory();
			}
			catch (IOException e)
			{
				totalRamBytes = 0;
			}
		}
			ram.put(TOTAL, totalRamBytes);
			ram.put(THRESHOLD, memInfo.threshold);
			ram.put(LOW, memInfo.lowMemory);
			ram.put(FREE, memInfo.availMem);
			ram.put(BUSY, (totalRamBytes - memInfo.availMem));
			return ram;
	}

	/**
	 * returns the total Ram Size in bytes
	 */
	private static long getTotalMemory() throws IOException
	{
		String memInfoLine;
		long initial_memory = 0;
        //as buffered reader is auto closeable we don't need to close it
		try (FileReader localFileReader = new FileReader("/proc/meminfo"); BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);)
		{
			// read the file meminfo file and give the total memory
			memInfoLine = localBufferedReader.readLine();// meminfo
		}
		if (memInfoLine != null)
		{
			String[] memInfoStrings = memInfoLine.split("\\s+");
			initial_memory = Integer.valueOf(memInfoStrings[1]).intValue() * 1024;
		}
		return initial_memory;
	}

	/**
	 * returns the busy, free and total SDcard memory
	 */
	private static Map<String,Long> getSDCardMem()
	{
		StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
		return getMemory(statFs);
	}

	/**
	 * returns the busy, free and total external memory
	 */
	private static Map<String, Long> getDataMem()
	{
		StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
		return getMemory(statFs);
	}

	/**
	 * returns the busy, free and total internal memory
	 */
	private static Map<String,Long> getInternalMem()
	{
		StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
		return getMemory(statFs);
	}

	/**
	 * 
	 * returns the jsonObj of the freeMem, freeMem, busyMem of the statFs provided
	 */
	private static Map<String, Long> getMemory(StatFs statFs)
	{
		HashMap<String, Long> memory = new HashMap<String, Long>();
		memory.put(TOTAL, getTotalMem(statFs));
		memory.put(FREE, getFreeMem(statFs));
		memory.put(BUSY, getBusyMem(statFs));
		return memory;
	}

	/**
	 * 
	 * returns the busy memory in bytes of the statfs provided
	 */
	private static long getBusyMem(StatFs statFs)
	{
		return getTotalMem(statFs) - getFreeMem(statFs);
	}

	/**
	 * 
	 * returns the total memory in bytes of the statfs provided
	 */
	private static long getTotalMem(StatFs statFs)
	{
		long total;
		if (Utils.isJELLY_BEAN_MR2OrHigher())
		{
			total = (long) statFs.getBlockCountLong() * (long) statFs.getBlockSizeLong();
		}
		else
		{
			total = (long) statFs.getBlockCount() * (long) statFs.getBlockSize();
		}
		return total;
	}

	/**
	 * 
	 * returns the free memory in bytes of the statfs provided
	 */
	private static long getFreeMem(StatFs statFs)
	{
		long free;
		if (Utils.isJELLY_BEAN_MR2OrHigher())
		{
			free = (long) statFs.getAvailableBlocksLong() * (long) statFs.getBlockSizeLong();
		}
		else
		{
			free = (long) statFs.getAvailableBlocks() * (long) statFs.getBlockSize();
		}
		return free;
	}

	/**
	 * 
	 * returns whether the automatic date and time is set or not
	 */
	private static boolean isAutomaticDateAndTimeSet(Context context)
	{
		int isSet;
		if (Utils.isJellybeanMR1OrHigher())
		{
			isSet = android.provider.Settings.Global.getInt(context.getContentResolver(), android.provider.Settings.Global.AUTO_TIME, 0);
		}
		else
		{
			isSet = android.provider.Settings.System.getInt(context.getContentResolver(), android.provider.Settings.System.AUTO_TIME, 0);
		}
		return (isSet == 1) ? true : false;
	}
	
	
	/** 
	 * getting the int after setting the particular bit according to arguments provided
	 */
	public static int getNumberAfterSettingBit(int oldNumber, byte bit,boolean toSet)
	{
		if (toSet)
		{
			return (oldNumber | (1 << bit));
		}
		else
		{
			return (oldNumber & ~(1 << bit));
		}
	}
	
	/** 
	 * getting the package name from process by removing after : part
	 */
    public static String getPackageFromProcess(String process)
	{
		if (process!=null)
		{
			return process.split(":")[0];
		}
    	return null;
	} 
}