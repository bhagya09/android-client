package com.bsb.hike.platform.ContentModules;

import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;

import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.utils.Logger;

import java.io.*;

public class PlatformContentUtils
{
	public static void copyFile(InputStream in, OutputStream out) throws IOException
	{
		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer)) != -1)
		{
			out.write(buffer, 0, read);
		}
	}

	public static File streamToTempFile(InputStream in, String prefix, String suffix)
	{
		File tempFile = null;
		try
		{
			// Hack to increase size of file name. Min 3 is required.
			if (prefix.length() <= 2)
			{
				prefix = prefix.concat("00");
			}
			tempFile = File.createTempFile(prefix, suffix);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		tempFile.deleteOnExit();

		FileOutputStream out = null;

		try
		{
			out = new FileOutputStream(tempFile);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}

		try
		{
			copyFile(in, out);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{

			try
			{
				if (in != null)
				{
					in.close();
				}

				if (out != null)
				{
					out.close();
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		return tempFile;
	}

	public static ParcelFileDescriptor openFileParcel(Uri uri, String mode)
	{
		Logger.d("FileContentProvider", "fetching: " + uri);

		ParcelFileDescriptor parcel = null;

		String filePath = uri.toString().replace(PlatformContentConstants.CONTENT_AUTHORITY_BASE, PlatformContentConstants.PLATFORM_CONTENT_DIR);

		try
		{
			parcel = ParcelFileDescriptor.open(new File(filePath), ParcelFileDescriptor.MODE_READ_ONLY);
		}
		catch (FileNotFoundException e)
		{
			Logger.e("FileContentProvider", "uri " + uri.toString(), e);
		}
		
		return parcel;
	}

	public static boolean copyAsset(AssetManager assetManager, String fromAssetPath, String toPath)
	{
		InputStream in = null;
		OutputStream out = null;
		try
		{
			in = assetManager.open(fromAssetPath);
			new File(toPath).createNewFile();
			out = new FileOutputStream(toPath);
			copyFile(in, out);
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	protected static String readDataFromFile(File file, PlatformContentCache.IExceptionHandler listener)
	{

		Logger.d("READING DATA FROM FILE: ", file.getAbsolutePath());
		// Read text from file
		StringBuilder text = new StringBuilder();

		BufferedReader br = null;
		try
		{
			br = new BufferedReader(new FileReader(file));

			String line;

			while ((line = br.readLine()) != null)
			{
				text.append(line);
			}
		}
		catch (FileNotFoundException fnfe)
		{
			// Template not found
			fnfe.printStackTrace();
			if (listener != null)
			{
				listener.onExceptionOcurred(fnfe);
			}
		}
		catch (IOException e)
		{
			// Template not found
			e.printStackTrace();
			if (listener != null)
			{
				listener.onExceptionOcurred(e);
			}
		}
		finally
		{
			try
			{
				if (br != null)
				{
					br.close();
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
				if (listener != null)
				{
					listener.onExceptionOcurred(e);
				}
			}
		}

		return text.toString();
	}

	public static boolean deleteDirectory(File path)
	{
		if (path.exists())
		{
			File[] files = path.listFiles();
			if (files == null)
			{
				return true;
			}
			for (int i = 0; i < files.length; i++)
			{
				if (files[i].isDirectory())
				{
					deleteDirectory(files[i]);
				}
				else
				{
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

	static public boolean hasStorage(boolean requireWriteAccess)
	{
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state))
		{
			if (requireWriteAccess)
			{
				boolean writable = checkFsWritable();
				return writable;
			}
			else
			{
				return true;
			}
		}
		else if (!requireWriteAccess && Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
		{
			return true;
		}
		return false;
	}

	private static boolean checkFsWritable()
	{
		// Create a temporary file to see whether a volume is really writeable.
		// It's important not to put it in the root directory which may have a
		// limit on the number of files.
		String directoryName = Environment.getExternalStorageDirectory().toString() + "/DCIM";
		File directory = new File(directoryName);
		if (!directory.isDirectory())
		{
			if (!directory.mkdirs())
			{
				return false;
			}
		}
		return directory.canWrite();
	}
}
