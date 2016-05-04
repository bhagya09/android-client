package com.bsb.hike.modules.httpmgr.request.requestbody;

import android.content.Context;

import com.bsb.hike.filetransfer.FTUtils;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.modules.httpmgr.request.IGetChunkSize;
import com.bsb.hike.utils.Utils;

/**
 * Created by sidharth on 11/02/16.
 */
public class FileTransferChunkSizePolicy implements IGetChunkSize
{
	private Context context;

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
}
