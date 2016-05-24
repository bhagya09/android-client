/**
 * 
 */
package com.bsb.hike.ces.disk;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;

import android.support.annotation.IntDef;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.ces.CesConstants;
import com.bsb.hike.ces.CesUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * @author suyash
 *
 */
public class CesDiskManager
{
	private static final String CES_ROOT_DIR = "/ces";
	private final String L1_FILENAME = "l1ces";
	private final String L2_FILENAME = "l2ces";
	private final String FILE_EXTENSION = ".txt";
	private final int PERCENT_OF_FREE_DISK = 1;
	private File level1File;
	private File level2File;
	private List<JSONObject> l1_dataList;
	private List<JSONObject> l2_dataList;

	@IntDef({ DataFlushMode.FLUSH, DataFlushMode.DO_NOT_FLUSH })
	public @interface DataFlushMode
	{
		int FLUSH = 0;

		int DO_NOT_FLUSH = 1;
	}
	private int dataFlushMode;
	private final String TAG = "CesDiskManager";
	/**
	 * 
	 */
	public CesDiskManager(int module, String date, int mode)
	{
		String moduleName = null;
		switch (module) {
		case CesConstants.CESModule.FT:
			moduleName = CesConstants.FT_MODULE;
			break;
		case CesConstants.CESModule.MQTT:
			moduleName = CesConstants.MQTT_MODULE;
			break;
		case CesConstants.CESModule.STICKER:
			moduleName = CesConstants.STICKER_MODULE;
			break;
		default:
			break;
		}
		makeDir(moduleName, date);
		dataFlushMode = mode;
		l1_dataList = new LinkedList<JSONObject>();
		l2_dataList = new LinkedList<JSONObject>();
	}

	private void makeDir(String moduleName, String date)
	{
		File dir = new File(HikeConstants.HIKE_DIRECTORY_ROOT + CES_ROOT_DIR + "/" + date + "/" + moduleName);
		if(!dir.exists())
		{
			if(!dir.mkdirs())
			{
				Logger.d(TAG, "Unable to create directory !");
				return;
			}
		}
		level1File = new File(dir, L1_FILENAME + FILE_EXTENSION);
		level2File = new File(dir, L2_FILENAME + FILE_EXTENSION);
	}

	public void add(int infoType, JSONObject data)
	{
		if(data == null || !isDiskFreeSpaceAvailable())
		{
			return;
		}
		switch (infoType) {
		case CesConstants.CESInfoType.L1:
			if(dataFlushMode == DataFlushMode.FLUSH)
			{
				CesFileOperation.storeCesData(data, level1File.getPath());
			}
			else
			{
				l1_dataList.add(data);
			}
			break;
		case CesConstants.CESInfoType.L2:
			if(dataFlushMode == DataFlushMode.FLUSH)
			{
				CesFileOperation.storeCesData(data, level2File.getPath());
			}
			else
			{
				l2_dataList.add(data);
			}
			break;
		}
	}

	public void add(int infoType, List<JSONObject> dataList)
	{
		if(dataList == null || dataList.isEmpty()  || !isDiskFreeSpaceAvailable())
		{
			return;
		}
		switch (infoType) {
		case CesConstants.CESInfoType.L1:
			if(dataFlushMode == DataFlushMode.FLUSH)
			{
				CesFileOperation.storeCesDataList(dataList, level1File.getPath());
			}
			else
			{
				for (Iterator<JSONObject> iterator = dataList.iterator(); iterator.hasNext();)
				{
					JSONObject data = (JSONObject) iterator.next();
					l1_dataList.add(data);
				}
			}
			break;
		case CesConstants.CESInfoType.L2:
			if(dataFlushMode == DataFlushMode.FLUSH)
			{
				CesFileOperation.storeCesDataList(dataList, level2File.getPath());
			}
			else
			{
				for (Iterator<JSONObject> iterator = dataList.iterator(); iterator.hasNext();)
				{
					JSONObject data = (JSONObject) iterator.next();
					l2_dataList.add(data);
				}
			}
			break;
		}
	}

	public void flushData(int infoType)
	{
		switch (infoType) {
		case CesConstants.CESInfoType.L1:
			CesFileOperation.storeCesDataList(l1_dataList, level1File.getPath());
			l1_dataList.clear();
			break;
		case CesConstants.CESInfoType.L2:
			CesFileOperation.storeCesDataList(l2_dataList, level2File.getPath());
			l2_dataList.clear();
			break;
		}
	}

	/**
	 * return filepath
	 */
	public String getFilePath(int infoType)
	{
		String filePath = null;
		switch (infoType) {
		case CesConstants.CESInfoType.L1:
			filePath =  level1File.getPath();
			break;
		case CesConstants.CESInfoType.L2:
			filePath = level2File.getPath();
			break;
		}
		return filePath;
	}

	/**
	 * return list of Json data
	 */
	public List<JSONObject> get(int infoType)
	{
		switch (infoType) {
		case CesConstants.CESInfoType.L1:
			return CesFileOperation.retrieveCesData(level1File.getPath());
		case CesConstants.CESInfoType.L2:
			return CesFileOperation.retrieveCesData(level2File.getPath());
		}
		return null;
	}

	public void update(int infoType, JSONObject data)
	{
		delete(infoType);
		switch (infoType) {
		case CesConstants.CESInfoType.L1:
			CesFileOperation.storeCesData(data, level1File.getPath());
			break;
		case CesConstants.CESInfoType.L2:
			CesFileOperation.storeCesData(data, level2File.getPath());
			break;
		}
	}

	public void update(int infoType, List<JSONObject> dataList)
	{
		delete(infoType);
		switch (infoType) {
		case CesConstants.CESInfoType.L1:
			CesFileOperation.storeCesDataList(dataList, level1File.getPath());
			break;
		case CesConstants.CESInfoType.L2:
			CesFileOperation.storeCesDataList(dataList, level2File.getPath());
			break;
		}
	}

	public void delete(int infoType)
	{
		switch (infoType) {
		case CesConstants.CESInfoType.L1:
			if(level1File.exists())
			{
				level1File.delete();
			}
			break;
		case CesConstants.CESInfoType.L2:
			if(level2File.exists())
			{
				level2File.delete();
			}
			break;
		}
	}

	private boolean isDiskFreeSpaceAvailable()
	{
		boolean result = false;
		double freeSpace = (Utils.getFreeSpace() / 100) * HikeSharedPreferenceUtil.getInstance().getData(CesConstants.ConfigureKey.UTILIZE_DISK_PERCENT, PERCENT_OF_FREE_DISK);
		if(freeSpace > getCesFileSize())
		{
			result = true;
		}
		return result;
	}

	private long getCesFileSize() {
		File cesDir = new File(HikeConstants.HIKE_DIRECTORY_ROOT + CES_ROOT_DIR);
		if (cesDir == null || !cesDir.exists())
		{
			return 0;
		}
		if (!cesDir.isDirectory())
		{
			return cesDir.length();
		}
		final List<File> dirs = new LinkedList<File>();
		dirs.add(cesDir);
		long result = 0;
		while (!dirs.isEmpty())
		{
			final File dir = dirs.remove(0);
			if (!dir.exists())
			{
				continue;
			}
			final File[] listFiles = dir.listFiles();
			if (listFiles == null || listFiles.length == 0)
			{
				continue;
			}
			for (final File child : listFiles)
			{
				result += child.length();
				if (child.isDirectory())
				{
					dirs.add(child);
				}
			}
		}
		Logger.d(TAG, "Space used by CES = " + result);
		return result;
	}

	public void dumpCesL2Data()
	{
		CesFileOperation.dumpCesL2DataToAnalytics(getFilePath(CesConstants.CESInfoType.L2));
	}

	public static void deleteCesDataOnAndBefore(String date)
	{
		File dir = new File(HikeConstants.HIKE_DIRECTORY_ROOT + CES_ROOT_DIR + "/" + date);
		if(dir != null && dir.exists())
		{
			Utils.deleteFile(dir);
		}
		else
		{
			return;
		}
		deleteCesDataOnAndBefore(CesUtils.getDayBeforeUTCDate(date));
	}

	public static void deleteAllCesData()
	{
		File dir = new File(HikeConstants.HIKE_DIRECTORY_ROOT + CES_ROOT_DIR);
		if(dir.exists())
		{
			Utils.deleteFile(dir);
		}
	}
}
