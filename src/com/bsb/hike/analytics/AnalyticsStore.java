package com.bsb.hike.analytics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * @author rajesh
 *
 */
public class AnalyticsStore
{
	private Context context;
	
	private static AnalyticsStore _instance; 
		
	private File normalPriorityEventFile;
	
	private File highPriorityEventFile;
	
	private AtomicBoolean uploadUnderProgress = new AtomicBoolean(false);

	private ThreadPoolExecutor mSingleThreadExecutor;

	/**
	 * Constructor
	 * @param context application context
	 */
	private AnalyticsStore(Context context)
	{
		this.context = context.getApplicationContext();

		mSingleThreadExecutor = new ThreadPoolExecutor(1, 1, AnalyticsConstants.ONE_MINUTE,
				TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		mSingleThreadExecutor.allowCoreThreadTimeOut(true);
		try 
		{
			normalPriorityEventFile = createNewEventFile(EventPriority.NORMAL);
			
			highPriorityEventFile = createNewEventFile(EventPriority.HIGH);
		}
		catch (IOException e) 
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "IO exception while creating new event file");
		}		
	}
	
	/**
	 * static constructor of AnalyticsStore
	 * @param context application context
	 * @return singleton instance of AnalyticsStore 
	 */
	public static AnalyticsStore getInstance(Context context)
	{
		if(_instance == null)
		{
			synchronized (AnalyticsStore.class) 
			{
				if(_instance == null)
				{
					_instance = new AnalyticsStore(context.getApplicationContext());
				}
			}
		}
		return _instance;
	}
	
	/**
	 * Returns the file name which is a concatenation of filename and current system time
	 * @return name of the file
	 */
	private String generateNewEventFileName(EventPriority priority)	
	{
		String fileName = null;
		
		if(priority == EventPriority.NORMAL)
		{
			fileName = AnalyticsConstants.NORMAL_EVENT_FILE_NAME + Long.toString(System.currentTimeMillis()) + 
				 AnalyticsConstants.SRC_FILE_EXTENSION;
		}
		else if(priority == EventPriority.HIGH)
		{
			fileName = AnalyticsConstants.IMP_EVENT_FILE_NAME + Long.toString(System.currentTimeMillis()) + 
					 AnalyticsConstants.SRC_FILE_EXTENSION;			
		}
		return fileName;
	}
	
	/**
	 * gets the size of the event file whose priority is given
	 * @return the size of file in bytes
	 */
	protected long getFileSize(EventPriority priority)
	{
		long fileLength = 0;
		
		if(priority == EventPriority.NORMAL)
		{
			if(normalPriorityEventFile != null)
			{
				fileLength = normalPriorityEventFile.length();
			}
		}
		else if(priority == EventPriority.HIGH)
		{
			if(highPriorityEventFile != null)
			{
				fileLength = highPriorityEventFile.length();
			}
		}		
		return fileLength;
	}
	
	/**
	 * creates a new plain events(text) file 
	 */
	private File createNewEventFile(EventPriority priority) throws IOException
	{
		File eventFile = null;
		
		String fileName = generateNewEventFileName(priority);
		
		File dir = new File(HAManager.getInstance().getAnalyticsDirectory());

		if(!dir.exists())
		{
			boolean ret = dir.mkdirs();
			
			if(!ret)
			{				
				throw new IOException("Failed to create Analytics directory");
			}
		}
		eventFile = new File(dir, fileName);

		boolean val = eventFile.createNewFile();
		
		if(!val)
		{
			throw new IOException("Failed to create event file");
		}

		return eventFile;
	}

	/**
	 * Used to write event json to the file
	 * @param eventJson json event to be written to the file
	 */
	protected void storeEvent(final JSONObject eventJson) {
        mSingleThreadExecutor.execute(new Runnable() {
			@Override
			public void run() {
				long fileMaxSize = HAManager.getInstance().getMaxFileSize();
				FileWriter highFileWriter = null;
				StringBuilder high = new StringBuilder();
				try{
				    high.append(eventJson);
				    high.append(AnalyticsConstants.NEW_LINE);

					if (high.length() > 0) {
						if (!eventFileExists(EventPriority.HIGH)) {
							highPriorityEventFile = createNewEventFile(EventPriority.HIGH);
						}

						if (getFileSize(EventPriority.HIGH) >= fileMaxSize) {
							Logger.d(AnalyticsConstants.ANALYTICS_TAG, "high priority file size reached " +
									"its limit! " + highPriorityEventFile.getName());
							compressAndDeleteOriginalFile(highPriorityEventFile.getAbsolutePath());
							highPriorityEventFile = createNewEventFile(EventPriority.HIGH);
						}
						highFileWriter = new FileWriter(highPriorityEventFile, true);
						highFileWriter.write(high.toString());
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "events written to imp file! Size " +
								"now :" + highPriorityEventFile.length() + "bytes");
					}
				    HAManager.getInstance().incrementAnalyticsEventsUploadCount();
				} catch (IOException e) {
					Logger.e(AnalyticsConstants.ANALYTICS_TAG, "io exception while writing events to" +
							" file");
				}
				finally {
					if (highFileWriter != null) {
						closeCurrentFile(highFileWriter);
					}
				}
			}
		});
	}

	/**
	 * Used to send events to Server
	 */
	protected void sendEvents() {
		if(!Utils.isUserOnline(context)) {
			HAManager.getInstance().setIsSendAnalyticsDataWhenConnected(true);
			return;
		}

        mSingleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                ArrayList<String> filesToUpload = getFilesToUpload();

                if (filesToUpload == null || filesToUpload.size() == 0) {
                    Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Nothing to Upload!!!");
                } else {
                    int fileCount = filesToUpload.size();
                    Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Total files to upload:" + fileCount);
                    for (int i = 0; i < fileCount; i++) {
                        //Upload files in parallel
                        new AnalyticsUploadTask(filesToUpload.get(i), (i + 1) == fileCount).execute();
                    }
                }
            }
        });
	}

    /**
     * Used to get all the files required to upload. Gzip the files which are not gzipped.
     */
	public ArrayList<String> getFilesToUpload() {
		// we should find all residual txt files compress them all
		String[] fileNames = HAManager.getFileNames(context);

		if (fileNames != null) {
			int fileCount = fileNames.length;

			for (int i = 0; i < fileCount; i++) {
				if (fileNames[i].endsWith(AnalyticsConstants.SRC_FILE_EXTENSION)) {
					String absolutePath = HAManager.getInstance().getAnalyticsDirectory()
							+ File.separator + fileNames[i];

					try {
						compressAndDeleteOriginalFile(absolutePath);
					} catch (IOException ex) {
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "IOException while " +
								"compressing files on the fly!");
					}
				}
			}
		}

        fileNames = HAManager.getFileNames(context);
        if(fileNames == null || fileNames.length == 0) return null;

        ArrayList<String> filesWithPath = new ArrayList<String>();
        for(String fileName : fileNames) {
            filesWithPath.add(HAManager.getInstance().getAnalyticsDirectory()
                    + File.separator + fileName);
        }
		return filesWithPath;
	}

	/**
	 * Checks if the event file exists or not
	 * @return true if the file exists, false otherwise
	 */
	private boolean eventFileExists(EventPriority priority)
	{	
		boolean isExist = false;
		
		if(priority == EventPriority.NORMAL)
		{
			isExist = normalPriorityEventFile != null && normalPriorityEventFile.exists();
		}
		else if(priority == EventPriority.HIGH)
		{
			isExist = highPriorityEventFile != null && highPriorityEventFile.exists();			
		}
		return isExist;
	}
	
	/**
	 * closes the currently opened event file
	 */
	private void closeCurrentFile(FileWriter fileWriter)
	{		
		try 
		{
			fileWriter.flush();
			fileWriter.close();
		}
		catch (IOException e) 
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "io exception while file connection closing!");
		}
	}	
	
	/**
	 * Used to get the total size of the logged analytics data
	 * @return size of the logged data in bytes
	 */
	public long getTotalAnalyticsSize()
	{
		long dirSize = 0;
		
		File dir = new File(HAManager.getInstance().getAnalyticsDirectory() + File.separator);

		File[] file = dir.listFiles();

		if(file == null)
			return 0;
		
		int size = file.length;
		
		for(int i=0; i<size; i++)
		{
			dirSize += file[i].length();
		}
		return dirSize;
	}
	
	/**
	 * Used to delete the analytics log files having NORMAL priority
	 */
	public void deleteNormalPriorityData()
	{
		File dir = new File(HAManager.getInstance().getAnalyticsDirectory() + File.separator);

		File[] file = dir.listFiles();

		if(file == null)
			return;
		
		int size = file.length;
		
		for(int i=0; i<size; i++)
		{
			if(file[i].exists() && file[i].getName().startsWith(AnalyticsConstants.NORMAL_EVENT_FILE_NAME, 0))
			{
				file[i].delete();
			}
		}
	}
	
	/**
	 * Used to compress the text file to gzip file
	 * @param fileUrl is the file path to be compressed
	 * @throws IOException 
	 */
	private void gzipFile(String srcFileUrl) throws IOException
	{	
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "FILE COMPRESSION IN PROCESS");

		String destFileUrl = srcFileUrl.replace(AnalyticsConstants.SRC_FILE_EXTENSION, AnalyticsConstants.DEST_FILE_EXTENSION);
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Source file url: "+srcFileUrl);
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Destination file url: "+destFileUrl);

		byte[] buffer = new byte[4096];
		
		GZIPOutputStream gzos = null;
		FileInputStream fis = null;
		try
		{
			gzos = new GZIPOutputStream(new FileOutputStream(destFileUrl));

			fis = new FileInputStream(srcFileUrl);

			int len;

			while ((len = fis.read(buffer)) > 0) 
			{
				gzos.write(buffer, 0, len);
			}	 
		}
		catch(FileNotFoundException ex)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "ioExcepion while compressing file");
		}
		finally
		{
			if(fis != null)
			{
				fis.close();
			}
			if(gzos != null)
			{
				gzos.finish();
				gzos.close();
				new File(srcFileUrl).delete();
			}
		}
	}
	
	/**
	 * Used to compress file with a given path in gzip format and then deletes it
	 * @param filePath of the file to be compressed to gzip
	 * @throws IOException thrown by gzipFile() method
	 */
	private void compressAndDeleteOriginalFile(String filePath) throws IOException
	{
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "CHECKING FILE VALIDITY FOR COMPRESSION");

		File tempFile = new File(filePath);

		if(tempFile.length() > 0)
		{
			gzipFile(filePath);
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "File compressed :" + filePath);
		}
		else if(tempFile.length() == 0)
		{
			tempFile.delete();
		}
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "File was deleted :" + filePath);
	}
}
