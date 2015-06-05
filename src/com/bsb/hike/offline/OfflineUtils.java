package com.bsb.hike.offline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

/**
 * 
 * @author himanshu
 *
 *	Contains Utility functions for Offline related messaging.
 */
public class OfflineUtils
{
	// change this to wlan0 for hotspot mode
	private final static String p2pInt = "wlan0";

	// Fix for Spice , Micromax , Xiomi
	private final static String p2pIntOther = "ap0";

	private final static String ALPHABET = "+a0bc9de1fg2h_ij8kl7mn6op3qr4st5uv6wxyz";

	private final static int shiftKey = 10;

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

	public static int byteArrayToInt(byte[] bytes)
	{
		return (bytes[0] & 0xFF) << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
	}

	public static  String getStickerPath(Sticker sticker)
	{

		String rootPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(sticker.getCategoryId());
		if (rootPath == null)
		{
			return null;
		}
		return rootPath + HikeConstants.LARGE_STICKER_ROOT + "/" + sticker.getStickerId();
	}

	public static boolean isChatThemeMessage(JSONObject message) throws JSONException
	{
		return (message.has(HikeConstants.TYPE) && message.getString(HikeConstants.TYPE).equals(HikeConstants.CHAT_BACKGROUND));
	}

	public static  String getFileBasedOnType(int type,String fileName)
	{
		StringBuilder storagePath = new StringBuilder(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT);
		if(type==HikeFileType.OTHER.ordinal())
		{
			storagePath.append(HikeConstants.OTHER_ROOT);
		}
		else if(type==HikeFileType.IMAGE.ordinal())
		{
			storagePath.append(HikeConstants.IMAGE_ROOT);
		}
		else if(type==HikeFileType.VIDEO.ordinal())
		{
			storagePath.append(HikeConstants.VIDEO_ROOT);
		}
		else if(type== HikeFileType.AUDIO.ordinal())
		{
			storagePath.append(HikeConstants.AUDIO_ROOT);
		}
		else if(type==HikeFileType.AUDIO_RECORDING.ordinal())
		{
			storagePath.append(HikeConstants.AUDIO_RECORDING_ROOT);
		}
		else if(type==HikeFileType.APK.ordinal())
		{
			storagePath.append(HikeConstants.OTHER_ROOT);
		}
		storagePath.append(File.separator+fileName);
		return storagePath.toString();
	}

	public static boolean isPingPacketValid(JSONObject object)
	{
			if (object.optString(HikeConstants.TYPE).equals(OfflineConstants.PING))
			{
				return true;
			}

		return false;
	}

	public static JSONObject createPingPacket()
	{
		JSONObject object=new JSONObject();
		try
		{
			object.put(HikeConstants.TYPE, OfflineConstants.PING);
			object.put(HikeConstants.FROM, getMyMsisdn());
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return  object;
	}
	
	public static String createOfflineMsisdn(String msisdn)
	{
		return new StringBuilder("o:").append(msisdn).toString();
	}

	public static ConvMessage createOfflineInlineConvMessage(String msisdn,String message,String type)
	{
		ConvMessage convMessage = Utils.makeConvMessage(msisdn, message, true, State.RECEIVED_READ);
		convMessage.setIsOfflineMessage(true);
		try
		{
			JSONObject metaData = new JSONObject();
			metaData.put(HikeConstants.TYPE, type);
			convMessage.setMetadata(new MessageMetadata(metaData.toString(), false));
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return convMessage;

	}

	public static boolean isOfflineSsid(String ssid) {
		String decodedSSID = decodeSsid(ssid.substring(1, ssid.length()-2));
		if(decodedSSID.startsWith("h_"))
			return true;
		return false;
	}
	// caeser cipher
	public static String encodeSsid(String ssid)
	{
		String cipherText="";
		for(int i=0;i<ssid.length();i++)
		{
			int charPosition = ALPHABET.indexOf(ssid.charAt(i));
			int keyVal = (shiftKey+charPosition)%(ALPHABET.length());
			char replaceVal = ALPHABET.charAt(keyVal);
			cipherText += replaceVal;
		}
		return cipherText;
	}

	// decrypt caeser cipher
	public static String decodeSsid(String cipherText)
	{
		String plainText="";
		for(int i=0;i<cipherText.length();i++)
		{
			int charPosition = ALPHABET.indexOf(cipherText.charAt(i));
			int keyVal = (charPosition-shiftKey)%(ALPHABET.length());
			if(keyVal<0)
			{
				keyVal = ALPHABET.length() + keyVal;
			}
			char replaceVal = ALPHABET.charAt(keyVal);
			plainText += replaceVal;
		}
		return plainText;
	}

	public static  String generatePassword(String ssid)
	{
		String passkey  =  new StringBuffer(ssid).reverse().toString();
		MessageDigest md = null;
		String pass = "";
		try {
			md = MessageDigest.getInstance("SHA-1");
		}
		catch(NoSuchAlgorithmException e) {
			e.printStackTrace();
		} 
		try {
			pass = byteArrayToHexString(md.digest(passkey.getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return pass;
	}

	private static String byteArrayToHexString(byte[] b) 
	{
		String result = "";
		for (int i=0; i < b.length; i++) {
			result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring(1);
		}
		return result;
	}

	public static String getconnectedDevice(String ssid) {
		if(ssid==null)
			return null;
		String arr[] = ssid.split("_");
		return arr[2];
	}

	public static String  getMyMsisdn()
	{
		String msisdn = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, null);
		return msisdn;
	}

	public static String getSsidForMsisdn(String toMsisdn,String fromMsisdn) {
		return "h_" +  toMsisdn + "_" + fromMsisdn;
	}

	public static String getMsisdnFromPingPacket(JSONObject messageJSON)
	{
		return messageJSON.optString(HikeConstants.FROM);
	}
	

}
