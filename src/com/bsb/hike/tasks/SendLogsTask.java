package com.bsb.hike.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

public class SendLogsTask extends AsyncTask<Void, Void, Void>
{

	Context context;

	public SendLogsTask(Context context)
	{
		this.context = context;
	}

	@Override
	protected Void doInBackground(Void... params)
	{
		getLogCatDetails();
		return null;
	}

	@Override
	protected void onPostExecute(Void result)
	{
		try
		{

			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_EMAIL, "");
			intent.putExtra(Intent.EXTRA_SUBJECT, "hike logs");
			intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(getLogFile()));

			context.startActivity(intent);
		}
		catch (Exception e)
		{

		}

	}

	private void getLogCatDetails()
	{
		File file = getLogFile();
		FileOutputStream fos = null;

		try
		{
			fos = new FileOutputStream(file);

			int pid = android.os.Process.myPid();

			Process process = Runtime.getRuntime().exec("logcat -d -v time");

			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line = "";

			while ((line = bufferedReader.readLine()) != null)
			{
				if (line.contains(Integer.toString(pid)))
				{
					line += "\n";
					fos.write(line.getBytes());
				}
			}

			fos.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (fos != null)
			{
				try
				{
					fos.flush();
					fos.getFD().sync();
					fos.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	private File getLogFile()
	{
		File root = android.os.Environment.getExternalStorageDirectory();

		return new File(root, "myLogs.txt");
	}
}
