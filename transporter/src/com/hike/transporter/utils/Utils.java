package com.hike.transporter.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Environment;
import android.os.Message;
import android.os.PowerManager;
import android.os.StatFs;
import android.view.Display;

import com.hike.transporter.HandlerUtil;
import com.hike.transporter.models.Config;
import com.hike.transporter.models.SenderConsignment;
import com.hike.transporter.models.Topic;
import com.hike.transporter.utils.TConstants.ERRORCODES;
import com.hike.transporter.utils.TConstants.THandlerConstants;

public class Utils
{

	public static enum ExternalStorageState
	{
		WRITEABLE, READ_ONLY, NONE
	}

	private static final String TAG = "Transporter";

	private static boolean screenOn = false;

	public static void setScreenStatus(boolean screenOn)
	{
		Utils.screenOn = screenOn;
	}

	public static boolean getScreenStatus()
	{
		return Utils.screenOn;
	}

	public static SenderConsignment createAckPacket(long awb, String topic)
	{
		SenderConsignment senderConsignment = null;
		JSONObject ackJSON = new JSONObject();
		try
		{

			ackJSON.put(TConstants.AWB, awb);
			senderConsignment = new SenderConsignment.Builder(ackJSON.toString(), topic).type(TConstants.ACK).ackRequired(false).persistance(false).build();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return senderConsignment;
	}

	public static boolean isAckPacket(JSONObject packet)
	{
		return (packet.optString(TConstants.TYPE).equals(TConstants.ACK));
	}

	public static boolean isHandShake(JSONObject packet)
	{
		return (packet.optString(TConstants.TYPE).equals(TConstants.HANDSHAKE));
	}

	public static boolean isFileRequest(JSONObject packet)
	{
		return packet.optString(TConstants.TYPE).equals(TConstants.FILE_REQUEST);
	}

	public static long getAwbNumberFromAckPkt(JSONObject json)
	{
		String data;
		JSONObject d = null;
		try
		{
			data = json.getString(TConstants.DATA);

			d = new JSONObject(data);
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return d.optLong(TConstants.AWB);
	}

	public static long getAwbNumberFromNormalPkt(JSONObject json)
	{
		return json.optLong(TConstants.AWB, -1);
	}

	@SuppressLint("NewApi")
	public static void initScreenStatus(Context context)
	{
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
		{
			DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
			for (Display display : dm.getDisplays())
			{
				if (display.getState() != Display.STATE_OFF)
				{
					screenOn = true;
				}
			}
		}
		else
		{
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			// noinspection deprecation
			screenOn = pm.isScreenOn();
		}
	}

	public static boolean isHeartBeatPkt(JSONObject packet)
	{
		if (packet.optString(TConstants.TYPE).equals(TConstants.HEARTBEAT))
		{
			return true;
		}
		return false;
	}

	public static boolean getScreenStatusFromHeartBeatPkt(JSONObject messageJSON)
	{
		String d;
		JSONObject data = null;
		try
		{
			d = messageJSON.getString(TConstants.DATA);

			data = new JSONObject(d);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return data.optBoolean(TConstants.SCREEN, false);
	}

	public static JSONObject createTextPkt(String data) throws JSONException
	{
		JSONObject textpkt = new JSONObject();
		textpkt.put(TConstants.TYPE, TConstants.TEXT);
		textpkt.put(TConstants.DATA, data);
		return textpkt;
	}

	public static JSONObject createFilePkt(String data, int fileSize) throws JSONException
	{
		JSONObject filepkt = createTextPkt(data);
		filepkt.put(TConstants.FILESIZE, fileSize);
		return filepkt;
	}

	public static boolean isFilePkt(JSONObject obj)
	{
		return (obj.optString(TConstants.TYPE).equals(TConstants.FILE));
	}

	public static int getFileSizeFromPacket(JSONObject obj)
	{
		return obj.optInt(TConstants.FILESIZE, -1);
	}
	
	public static int getFileSizeFromFileRequestPkt(JSONObject obj)
	{
		JSONObject data = null;
		String d = null;
		try
		{
			d = obj.getString(TConstants.DATA);
			data = new JSONObject(d);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return data.optInt(TConstants.FILESIZE, -1);
	}

	public static boolean isTextPkt(JSONObject obj)
	{
		if (obj.optString(TConstants.TYPE).equals(TConstants.TEXT) && obj.optInt(TConstants.FILESIZE, -1) == -1)
		{
			return true;
		}
		return false;
	}

	public static File createTempFile(Context context) throws IOException
	{
		File tempFile = new File(context.getExternalFilesDir("Transporter"), "transporter_" + System.currentTimeMillis()+".bin");
		File dirs = new File(tempFile.getParent());
		if (!dirs.exists())
			dirs.mkdirs();
		// created a temporary file which on successful download will be renamed.
		tempFile.createNewFile();
		return tempFile;
	}

	public static SenderConsignment createHandShakePkt(Topic topic, Config config)
	{
		JSONObject object = new JSONObject();
		try
		{
			object.put(TConstants.TOPIC, topic.getName());
			if (config != null)
			{
				object.put(TConstants.CONFIG, config.serialize());
				object.put(TConstants.TTLCNN, config.getTopics().size());
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		SenderConsignment senderConsignment = new SenderConsignment.Builder(object.toString(), topic.getName()).ackRequired(false).persistance(false).type(TConstants.HANDSHAKE)
				.build();
		return senderConsignment;
	}

	public static String getTopicFromHandShake(JSONObject json)
	{
		String topic = null;
		try
		{
			String data = json.getString(TConstants.DATA);
			JSONObject d = new JSONObject(data);
			topic = d.getString(TConstants.TOPIC);
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return topic;
	}

	public static String getConfig(JSONObject json)
	{
		try
		{
			String data = json.getString(TConstants.DATA);
			JSONObject d = new JSONObject(data);
			return d.optString(TConstants.CONFIG);
		}
		catch (JSONException e)
		{
			// TODO
			e.printStackTrace();
		}
		return null;
	}

	public static void closeSocket(final Socket socket) throws IOException
	{
		if (socket == null)
		{
			return;
		}
		try
		{
			if (socket.isOutputShutdown())
			{
				Logger.d(TAG, "Output is already  shutdown");
			}
			else
			{
				socket.shutdownOutput();
			}
		}
		catch (IOException e)
		{
			Logger.d(TAG, "exception in shutDownOutput");
		}

		try
		{
			if (socket.isInputShutdown())
			{
				Logger.d(TAG, "Input is already  shutdown");
			}
			else
			{
				socket.shutdownInput();
			}
		}
		catch (IOException e)
		{
			Logger.d(TAG, "exception in shutDownOutput");
		}

	}

	public static void closeSocket(ServerSocket serverSocket) throws IOException
	{
		if (serverSocket == null)
		{
			return;
		}
		serverSocket.close();
	}

	public static SenderConsignment createHeartBeatPacket(String topic)
	{
		JSONObject message = new JSONObject();
		try
		{
			message.put(TConstants.SCREEN, screenOn);
			SenderConsignment senderConsignment = new SenderConsignment.Builder(message.toString(), topic).type(TConstants.HEARTBEAT).ackRequired(false).persistance(false).build();
			return senderConsignment;
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * This function checks whether we have enough space on external memory or not
	 * 
	 * @param requiredBytes
	 * @return 0 if success OR error code check {@link TConstants.ERRORCODES}
	 */
	public static int isFreeSpaceAvailable(int requiredBytes)
	{
		ExternalStorageState state = getExternalStorageState();
		if (state == ExternalStorageState.WRITEABLE)
		{
			if (requiredBytes < getFreeSpace())
			{
				return TConstants.SUCCESS;
			}
			else
			{
				return ERRORCODES.NOT_ENOUGH_MEMORY.ordinal();
			}
		}
		if (state == ExternalStorageState.READ_ONLY)
		{
			return ERRORCODES.SD_CARD_NOT_WRITABLE.ordinal();
		}
		else
		{
			return ERRORCODES.SD_CARD_NOT_PRESENT.ordinal();
		}
	}

	private static ExternalStorageState getExternalStorageState()
	{
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state))
		{
			// We can read and write the media
			return ExternalStorageState.WRITEABLE;
		}
		else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
		{
			// We can only read the media
			return ExternalStorageState.READ_ONLY;
		}
		else
		{
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			return ExternalStorageState.NONE;
		}
	}

	@SuppressLint("NewApi")
	public static double getFreeSpace()
	{
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		double sdAvailSize = 0.0;
		if (isJELLY_BEAN_MR2OrHigher())
		{
			sdAvailSize = (double) stat.getAvailableBlocksLong() * (double) stat.getBlockSizeLong();
		}
		else
		{
			sdAvailSize = (double) stat.getAvailableBlocks() * (double) stat.getBlockSize();
		}

		return sdAvailSize;
	}

	public static boolean isJELLY_BEAN_MR2OrHigher()
	{
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
	}

	public static SenderConsignment createFileRequestPacket(long awb, int requiredBytes, String topic)
	{
		SenderConsignment senderConsignment = null;
		JSONObject json = new JSONObject();
		try
		{

			json.put(TConstants.FILESIZE, requiredBytes);
			json.put(TConstants.AWB, awb);
			senderConsignment = new SenderConsignment.Builder(json.toString(), topic).type(TConstants.FILE_REQUEST).ackRequired(false).persistance(false).build();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return senderConsignment;
	}

	public static SenderConsignment createFileRequestReplyPacket(long awb, int responseCode, String topic)
	{
		SenderConsignment senderConsignment = null;
		JSONObject json = new JSONObject();
		try
		{

			json.put(TConstants.CODE, responseCode);
			json.put(TConstants.AWB, awb);
			senderConsignment = new SenderConsignment.Builder(json.toString(), topic).type(TConstants.FILE_REQUEST_REPLY).ackRequired(false).persistance(false).build();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return senderConsignment;
	}

	public static boolean isFileRequestReply(JSONObject packet)
	{
		return packet.optString(TConstants.TYPE).equals(TConstants.FILE_REQUEST_REPLY);
	}

	/**
	 * 
	 * @param packet
	 * @return -1 if not present
	 */
	public static int getCode(JSONObject packet)
	{
		JSONObject data;
		String d = null;
		try
		{
			d = packet.getString(TConstants.DATA);
			data = new JSONObject(d);
			return data.optInt(TConstants.CODE, -1);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return -1;
	}

	public static SenderConsignment createHandShakePktFromServer(String topic)
	{
		JSONObject object = new JSONObject();
		SenderConsignment senderConsignment = null;
		try
		{
			object.put(TConstants.CONNECTEDSTATUS, true);
			senderConsignment = new SenderConsignment.Builder(object.toString(), topic).type(TConstants.HANDSHAKE_SERVER).ackRequired(false).persistance(false).build();
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return senderConsignment;
	}

	public static boolean isHandShakePktFromServer(JSONObject json)
	{
		return json.optString(TConstants.TYPE).equals(TConstants.HANDSHAKE_SERVER);
	}

	public static void closeStream(FileOutputStream fileOutputStream)
	{
		{
			if (fileOutputStream == null)
				return;
		}
		try
		{
			fileOutputStream.flush();
			fileOutputStream.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}
	

	public static int getTotalConnections(JSONObject config)
	{
		try
		{
			String data = config.getString(TConstants.DATA);
			JSONObject d = new JSONObject(data);
			return d.optInt(TConstants.TTLCNN);
		}
		catch (JSONException e)
		{
			// TODO
			e.printStackTrace();
		}
		return 0;
	}

	public static void closeStream(FileInputStream inputStream)
	{
		if (inputStream == null)
		{
			return;
		}
		try
		{
			inputStream.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void deleteTempFiles(Context context)
	{
		File dir = context.getExternalFilesDir("Transporter");
		if (dir!=null && dir.isDirectory())
		{
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++)
			{
				new File(dir, children[i]).delete();
			}
		}
	}
	
	public static void postDisconnectForGhostPackets(boolean screenOn,Config config)
	{
		HandlerUtil.getInstance().removeMessages(THandlerConstants.DISCONNECT_AFTER_TIMEOUT);
		Message msg = Message.obtain();
		msg.what = THandlerConstants.DISCONNECT_AFTER_TIMEOUT;
		Logger.d(TAG, "Screen Disconnection Time out is " + (screenOn ? config.getKeepAlive() : config.getKeepAliveScreenOff()) + "");
		HandlerUtil.getInstance().sendMessageDelayed(msg, screenOn ? config.getKeepAlive() : config.getKeepAliveScreenOff());
	}

}
