/**
 * 
 */
package com.bsb.hike.ces.disk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * @author suyash
 *
 */
public class CesFileOperation {

	private static final String NEW_LINE = "\n";
	private static final String TAG = "CesFileOperation";
	
	public static boolean storeCesData(JSONObject logLine, String filePath)
	{
		FileWriter writer = null;
		boolean success = false;
		StringBuilder dataBuilder = new StringBuilder();
		try{
			dataBuilder.append(logLine);
			dataBuilder.append(NEW_LINE);
		    writer = new FileWriter(new File(filePath), true);
		    writer.write(dataBuilder.toString());
		    writer.flush();
		    success = true;
		} catch (IOException e) {
			Logger.e(TAG, "IOException : ", e);
		}
		finally {
			Utils.closeStreams(writer);
		}
		return success;
	}

	public static boolean storeCesDataList(List<JSONObject> logLines, String filePath)
	{
		FileWriter writer = null;
		boolean success = false;
		StringBuilder dataBuilder = new StringBuilder();
		try{
			writer = new FileWriter(new File(filePath), true);
			for (Iterator<JSONObject> iterator = logLines.iterator(); iterator.hasNext();) {
				JSONObject logLine = (JSONObject) iterator.next();
				dataBuilder.append(logLine);
				dataBuilder.append(NEW_LINE);
				writer.write(dataBuilder.toString());
			}
		    writer.flush();
		    success = true;
		} catch (IOException e) {
			Logger.e(TAG, "IOException : ", e);
		}
		finally {
			Utils.closeStreams(writer);
		}
		return success;
	}

	public static List<JSONObject> retrieveCesData(String filePath)
	{
		InputStreamReader isr = null;
		BufferedReader bufferedReader = null;
		List<JSONObject> result = new LinkedList<JSONObject>();
		try
		{
			isr = new InputStreamReader(new FileInputStream(filePath));
			bufferedReader = new BufferedReader(isr);
			String receiveString = "";

            while ( (receiveString = bufferedReader.readLine()) != null )
            {
            	JSONObject logLine = new JSONObject(receiveString);
            	result.add(logLine);
            }

		} catch (FileNotFoundException e)
		{
			Logger.e(TAG, "FileNotFoundException : ", e);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "JSONException : ", e);
		}
		catch (IOException e)
		{
			Logger.e(TAG, "IOException : ", e);
		}
		finally
		{
			Utils.closeStreams(bufferedReader, isr);
		}
		return result;
	}

	public static List<JSONObject> retrieveCesData(List<JSONObject> data, String filePath)
	{
		InputStreamReader isr = null;
		BufferedReader bufferedReader = null;
		try
		{
			isr = new InputStreamReader(new FileInputStream(filePath));
			bufferedReader = new BufferedReader(isr);
			String receiveString = "";

            while ( (receiveString = bufferedReader.readLine()) != null )
            {
            	JSONObject logLine = new JSONObject(receiveString);
            	data.add(logLine);
            }

		} catch (FileNotFoundException e)
		{
			Logger.e(TAG, "FileNotFoundException : ", e);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "JSONException : ", e);
		}
		catch (IOException e)
		{
			Logger.e(TAG, "IOException : ", e);
		}
		finally
		{
			Utils.closeStreams(bufferedReader, isr);
		}
		return data;
	}
}
