package com.bsb.hike.offline;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;

public class OfflineUtils
{
	// change this to wlan0 for hotspot mode
	private final static String p2pInt = "wlan0";

	// Fix for Spice , Micromax , Xiomi
	private final static String p2pIntOther = "ap0";
	
	

	public static String getIPFromMac(String MAC)
	{
		BufferedReader br = null;
		try
		{
			br = new BufferedReader(new FileReader("/proc/net/arp"));
			String line;
			while ((line = br.readLine()) != null)
			{

				String[] splitted = line.split(" +");
				if (splitted != null && splitted.length >= 4)
				{
					// Basic sanity check
					String device = splitted[5];
					if (device.matches(".*" + p2pInt + ".*") || device.matches(".*" + p2pIntOther + ".*"))
					{
						return splitted[0];
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				br.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return null;
	}

	public static boolean isStickerMessage(JSONObject messageJSON)
	{
		try
		{
			if (messageJSON.has(HikeConstants.SUB_TYPE) && (messageJSON.getString(HikeConstants.SUB_TYPE).equals(HikeConstants.STICKER)))
			{
				return true;
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return false;
	}
	
	public static byte[] intToByteArray(int i)
	{
		byte[] result = new byte[4];
		
		result[0] = (byte) (i >> 24);
		result[1] = (byte) (i >> 16);
		result[2] = (byte) (i >> 8);
		result[3] = (byte) (i /*>> 0*/);
		return result;
	}
	
	public static boolean isGhostPacket(JSONObject packet)
	{

		try
		{
			if (packet.has(HikeConstants.SUB_TYPE) && packet.getString(HikeConstants.SUB_TYPE).equals(OfflineConstants.GHOST))
			{
				return true;
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return false;
	}

	public static  int updateDB(long msgId, ConvMessage.State status, String msisdn)
	{
		return HikeConversationsDatabase.getInstance().updateMsgStatus(msgId, status.ordinal(), msisdn);
	}

	public static int getTotalChunks(int fileSize)
	{
		 return fileSize/1024 + ((fileSize%1024!=0)?1:0);
	}
}
