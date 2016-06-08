package com.bsb.hike.timeline.tasks;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.os.Looper;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class TimelineResourceCleaner implements Runnable
{
	private volatile static TimelineResourceCleaner instance;

	private AtomicBoolean isRunning = new AtomicBoolean(false);

	//TODO Make this dependent on total SD card space
	private final int DEFAULT_MAX_SIZE = 30;

	private final int[] DEFAULT_CANDIDATES = new int[] { StatusMessageType.IMAGE.getKey(), StatusMessageType.TEXT_IMAGE.getKey(), StatusMessageType.PROFILE_PIC.getKey() };

	private TimelineResourceCleaner()
	{
		// Avoid instantiation
	}

	public static TimelineResourceCleaner getInstance()
	{
		if (instance == null)
		{
			synchronized (TimelineResourceCleaner.class)
			{
				if (instance == null)
				{
					instance = new TimelineResourceCleaner();
				}
			}
		}
		return instance;
	}

	@Override
	public void run()
	{
		if (Looper.myLooper() == Looper.getMainLooper())
		{
			throw new RuntimeException("This could not run on main thread.");
		}

		synchronized (instance)
		{
			if (isRunning.get())
			{
				Logger.e(TimelineResourceCleaner.class.getSimpleName(), "Already running.");
				return;
			}
		}

		try
		{
			isRunning.set(true);

			List<StatusMessage> statusMessagesList = HikeConversationsDatabase.getInstance().getStatusMessages(false, -1, DEFAULT_CANDIDATES);

			if (statusMessagesList == null || statusMessagesList.size() <= DEFAULT_MAX_SIZE)
			{
				Logger.i(TimelineResourceCleaner.class.getSimpleName(), "Nothing to do. Present size < Max size");
				return;
			}
			else
			{
				Logger.i(TimelineResourceCleaner.class.getSimpleName(), "Begin clean");
				int totalSize = statusMessagesList.size() - 1;
				for (int i = totalSize; i > DEFAULT_MAX_SIZE; i--)
				{
					StatusMessage statusMsg = statusMessagesList.get(i);

					if (statusMsg == null)
					{
						// Not required. Defensive.
						continue;
					}

					String fileName = Utils.getProfileImageFileName(statusMsg.getMappedId());

					if (TextUtils.isEmpty(fileName))
					{
						continue;
					}

					File orgFile = new File(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT, fileName);
					Utils.deleteFile(orgFile);
				}
			}
		}
		finally
		{
			isRunning.set(false);
		}
	}
}
