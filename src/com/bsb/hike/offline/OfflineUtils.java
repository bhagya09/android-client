package com.bsb.hike.offline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.PowerManager;
import android.text.TextUtils;
import android.view.Display;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

/**
 * 
 * @author himanshu, deepak malik, sahil
 * 
 *         Contains Utility functions for Offline related messaging.
 */
public class OfflineUtils
{
	private final static String TAG = OfflineUtils.class.getSimpleName();

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

	public static int updateDB(long msgId, ConvMessage.State status, String msisdn)
	{
		return HikeConversationsDatabase.getInstance().updateMsgStatus(msgId, status.ordinal(), msisdn);
	}

	public static int getTotalChunks(int fileSize)
	{
		return fileSize / OfflineConstants.CHUNK_SIZE + ((fileSize % OfflineConstants.CHUNK_SIZE != 0) ? 1 : 0);
	}

	public static boolean isChatThemeMessage(JSONObject message) throws JSONException
	{
		return (message.has(HikeConstants.TYPE) && message.getString(HikeConstants.TYPE).equals(HikeConstants.CHAT_BACKGROUND));
	}

	public static String getFileBasedOnType(int type, String fileName)
	{
		StringBuilder storagePath = new StringBuilder(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT);
		if (type == HikeFileType.OTHER.ordinal())
		{
			storagePath.append(HikeConstants.OTHER_ROOT);
		}
		else if (type == HikeFileType.IMAGE.ordinal())
		{
			storagePath.append(HikeConstants.IMAGE_ROOT);
		}
		else if (type == HikeFileType.VIDEO.ordinal())
		{
			storagePath.append(HikeConstants.VIDEO_ROOT);
		}
		else if (type == HikeFileType.AUDIO.ordinal())
		{
			storagePath.append(HikeConstants.AUDIO_ROOT);
		}
		else if (type == HikeFileType.AUDIO_RECORDING.ordinal())
		{
			storagePath.append(HikeConstants.AUDIO_RECORDING_ROOT);
		}
		else if (type == HikeFileType.APK.ordinal())
		{
			storagePath.append(HikeConstants.OTHER_ROOT);
		}
		storagePath.append(File.separator+fileName);
		return storagePath.toString();
	}

	public static String createOfflineMsisdn(String msisdn)
	{
		return new StringBuilder("o:").append(msisdn).toString();
	}

	public static ConvMessage createOfflineInlineConvMessage(String msisdn, String message, String type)
	{
		ConvMessage convMessage = Utils.makeConvMessage(msisdn, message, true, State.RECEIVED_UNREAD);
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

	public static boolean isOfflineSsid(String ssid)
	{
		String decodedSSID = decodeSsid(ssid);
		if (decodedSSID.startsWith("h_"))
			return true;
		return false;
	}

	// caeser cipher
	public static String encodeSsid(String ssid)
	{
		String cipherText = "";
		for (int i = 0; i < ssid.length(); i++)
		{
			int charPosition = ALPHABET.indexOf(ssid.charAt(i));
			int keyVal = (shiftKey + charPosition) % (ALPHABET.length());
			char replaceVal = ALPHABET.charAt(keyVal);
			cipherText += replaceVal;
		}
		return cipherText;
	}

	// decrypt caeser cipher
	public static String decodeSsid(String cipherText)
	{
		String plainText = "";
		for (int i = 0; i < cipherText.length(); i++)
		{
			int charPosition = ALPHABET.indexOf(cipherText.charAt(i));
			int keyVal = (charPosition - shiftKey) % (ALPHABET.length());
			if (keyVal < 0)
			{
				keyVal = ALPHABET.length() + keyVal;
			}
			char replaceVal = ALPHABET.charAt(keyVal);
			plainText += replaceVal;
		}
		return plainText;
	}

	public static String generatePassword(String ssid)
	{
		String passkey = new StringBuffer(ssid).reverse().toString();
		MessageDigest md = null;
		String pass = "";
		try
		{
			md = MessageDigest.getInstance("SHA-1");
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		try
		{
			pass = byteArrayToHexString(md.digest(passkey.getBytes("UTF-8")));
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		return pass;
	}

	private static String byteArrayToHexString(byte[] b)
	{
		String result = "";
		for (int i = 0; i < b.length; i++)
		{
			result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}

	public static String getconnectedDevice(String ssid)
	{
		if (ssid == null)
			return null;
		String arr[] = ssid.split("_");
		if (arr.length > 2)
			return arr[2];
		else
			return null;
	}

	public static String getMyMsisdn()
	{
		String msisdn = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, null);
		return msisdn;
	}

	public static String getSsidForMsisdn(String toMsisdn, String fromMsisdn)
	{
		return "h_" + toMsisdn + "_" + fromMsisdn;
	}

	public static String getMsisdnFromPingPacket(JSONObject messageJSON)
	{
		return messageJSON.optString(HikeConstants.FROM);
	}

	public static String getStickerPath(JSONObject sticker)
	{
		String path = "";
		try
		{
			String ctgId = sticker.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getString(StickerManager.CATEGORY_ID);
			String stkId = sticker.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getString(StickerManager.STICKER_ID);

			Sticker tempStk = new Sticker(ctgId, stkId);
			path = tempStk.getStickerPath(HikeMessengerApp.getInstance().getApplicationContext());
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "JSONException in getStickerPath. Check whether JSONObject is a sticker.");
			e.printStackTrace();
		}
		return path;
	}

	public static boolean isConnectedToSameMsisdn(JSONObject message, String connectedMsisdn)
	{

		if (TextUtils.isEmpty(connectedMsisdn))
		{
			return false;
		}
		String sendingMsisdn = message.optString(HikeConstants.TO).replace("o:", "");
		return sendingMsisdn.equals(connectedMsisdn);

	}

	public static String getFilePathFromJSON(JSONObject packet)
	{
		try
		{
			JSONArray jsonFiles = packet.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES);
			JSONObject jsonFile = jsonFiles.getJSONObject(0);
			return jsonFile.optString(HikeConstants.FILE_PATH);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static int getFileSizeFromJSON(JSONObject packet)
	{
		try
		{
			JSONArray jsonFiles = packet.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES);
			JSONObject jsonFile = jsonFiles.getJSONObject(0);
			return jsonFile.optInt(HikeConstants.FILE_SIZE);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return -1;
		}
	}

	public static long getMsgId(JSONObject packetData)
	{
		if (packetData.optJSONObject(HikeConstants.DATA) != null)
		{
			try
			{
				return packetData.getJSONObject(HikeConstants.DATA).optLong(HikeConstants.MESSAGE_ID, -1);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		return -1;
	}

	public static boolean isFileTransferMessage(JSONObject packet)
	{
		boolean isFileTransferMessage = false;
		JSONObject metadata;
		try
		{
			metadata = packet.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);
			if(metadata!=null)
			{
				MessageMetadata md = new MessageMetadata(metadata, true);
				isFileTransferMessage = md.getHikeFiles() != null && md.getHikeFiles().size() > 0;
			}
			
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return isFileTransferMessage;
	}

	public static void createStkDirectory(JSONObject messageJSON) throws JSONException, IOException
	{
		String ctgId = messageJSON.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getString(StickerManager.CATEGORY_ID);
		String stkId = messageJSON.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getString(StickerManager.STICKER_ID);
		Sticker sticker = new Sticker(ctgId, stkId);

		File stickerImage;
		String stickerPath = sticker.getStickerPath(HikeMessengerApp.getInstance().getApplicationContext());
		stickerImage = new File(stickerPath);

		// sticker is not present
		if (stickerImage == null || (stickerImage.exists() == false))
		{
			File parent = new File(stickerImage.getParent());
			if (!parent.exists())
				parent.mkdirs();
			stickerImage.createNewFile();
		}
	}

	public static void putStkLenInPkt(JSONObject packet, long length)
	{

		if (packet.optJSONObject(HikeConstants.DATA) != null)
		{
			try
			{
				JSONObject metaData = packet.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);
				metaData.put(HikeConstants.FILE_SIZE, length);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static  JSONObject getFileTransferMetadataForContact(JSONObject contactJson) throws JSONException
	{
		contactJson.put(HikeConstants.FILE_NAME, contactJson.optString(HikeConstants.NAME, HikeConstants.CONTACT_FILE_NAME));
		contactJson.put(HikeConstants.CONTENT_TYPE, HikeConstants.CONTACT_CONTENT_TYPE);
		contactJson.put(HikeConstants.FILE_KEY,"OfflineMessageFileKey"+System.currentTimeMillis());
		JSONArray files = new JSONArray();
		files.put(contactJson);
		JSONObject metadata = new JSONObject();
		metadata.put(HikeConstants.FILES, files);

		return metadata;
	}
	
	private static ConvMessage createConvMessage(String msisdn, JSONObject metadata,boolean isRecipientOnhike)
	{
		long time = System.currentTimeMillis() / 1000;
		ConvMessage convMessage = new ConvMessage(HikeConstants.CONTACT_FILE_NAME, msisdn, time, ConvMessage.State.SENT_UNCONFIRMED);
		try {
			convMessage.setMetadata(metadata);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return convMessage;
	}

	public static ConvMessage createOfflineContactConvMessage(String msisdn,
			JSONObject jsonData, boolean onHike){
		ConvMessage convMessage = null;
		try {
			convMessage = createConvMessage(msisdn, getFileTransferMetadataForContact(jsonData),onHike);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return convMessage;
	}

	public static boolean isContactTransferMessage(JSONObject packet) {
		
		boolean isContactTransferMessage = false;
		JSONObject metadata;
		JSONArray files;
		JSONObject fileJSON;
		try
		{
			metadata = packet.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);
			if(metadata!=null)
			{
				files = metadata.getJSONArray(HikeConstants.FILES);
				if(files!=null)
				{
					fileJSON = files.getJSONObject(0);
					isContactTransferMessage = fileJSON.getString(HikeConstants.CONTENT_TYPE).equals(HikeConstants.CONTACT_CONTENT_TYPE);
				}
			}
				
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return isContactTransferMessage;
	}
	
	public static void showSpinnerProgress(FileTransferModel fileTransferModel)
	{
		if (fileTransferModel == null)
			return;

		fileTransferModel.getTransferProgress().setCurrentChunks(fileTransferModel.getTransferProgress().getCurrentChunks() + 1);

		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
	}

	public static JSONObject createDisconnectPkt(String msisdn)
	{
		JSONObject disconnect = new JSONObject();
		try
		{
			disconnect.put(HikeConstants.TO, msisdn);
			disconnect.put(HikeConstants.TYPE, "d");
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return disconnect;
	}
	
	public static boolean isDisconnectPkt(JSONObject disconnect)
	{
		return disconnect.optString(HikeConstants.TYPE, "").equals("d");
	}

	public static MessageMetadata getUpdatedMessageMetaData(ConvMessage msg) {
        
        JSONObject metaData = msg.getMetadata().getJSON();
        JSONArray filesArray = new JSONArray();
        JSONObject fileJSON = null;
        HikeFile hikeFile = msg.getMetadata().getHikeFiles().get(0);
        try
        {
            fileJSON = metaData.getJSONArray(HikeConstants.FILES).getJSONObject(0);
            String fileName = fileJSON.getString(HikeConstants.FILE_NAME);
            File sourceFile  =  new File(fileJSON.getString(HikeConstants.FILE_PATH));
            hikeFile.setFileKey("OfflineKey"+System.currentTimeMillis()/1000);
            hikeFile.setFile(sourceFile);
            hikeFile.setFileSize((int)sourceFile.length());
            hikeFile.setFileName(fileName);
            hikeFile.setSent(true);    
            fileJSON =  hikeFile.serialize();
            filesArray.put(fileJSON);
            metaData.put(HikeConstants.FILES, filesArray);
            MessageMetadata messageMetadata = new MessageMetadata(metaData, true);
            return messageMetadata;
        }catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
	
}
