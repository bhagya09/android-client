package com.bsb.hike.offline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.State;
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
 * @author himanshu, deepak malik
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

	public static byte[] intToByteArray(int i)
	{
		byte[] result = new byte[4];

		result[0] = (byte) (i >> 24);
		result[1] = (byte) (i >> 16);
		result[2] = (byte) (i >> 8);
		result[3] = (byte) (i /* >> 0 */);
		return result;
	}

	public static boolean isGhostPacket(JSONObject packet)
	{
		if (packet.optString(HikeConstants.SUB_TYPE).equals(OfflineConstants.GHOST))
		{
			return true;
		}
		return false;
	}
	
	public static JSONObject createGhostPacket(String msisdn)
	{
		return createGhostPacket(msisdn,false);
	}

	public static JSONObject createGhostPacket(String msisdn,boolean screen)
	{
		JSONObject ghostJSON = new JSONObject();
		try
		{
			ghostJSON.putOpt(HikeConstants.TO, msisdn);
			ghostJSON.putOpt(HikeConstants.SUB_TYPE, OfflineConstants.GHOST);
			ghostJSON.put("screen", screen);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return ghostJSON;
	}

	public static int updateDB(long msgId, ConvMessage.State status, String msisdn)
	{
		return HikeConversationsDatabase.getInstance().updateMsgStatus(msgId, status.ordinal(), msisdn);
	}

	public static int getTotalChunks(int fileSize)
	{
		return fileSize / OfflineConstants.CHUNK_SIZE + ((fileSize % OfflineConstants.CHUNK_SIZE != 0) ? 1 : 0);
	}

	public static int byteArrayToInt(byte[] bytes)
	{
		return (bytes[0] & 0xFF) << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
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
		storagePath.append(File.separator + fileName);
		return storagePath.toString();
	}

	public static boolean isPingPacket(JSONObject object)
	{
		if (object.optString(HikeConstants.TYPE).equals(OfflineConstants.PING))
		{
			return true;
		}
		return false;
	}

	public static JSONObject createPingPacket()
	{
		JSONObject object = new JSONObject();
		try
		{
			object.put(HikeConstants.TYPE, OfflineConstants.PING);
			object.put(HikeConstants.FROM, getMyMsisdn());
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return object;
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

	public static void closeOutputStream(FileOutputStream outputStream) throws IOException
	{
		if (outputStream == null)
			return;

		outputStream.flush();
		outputStream.getFD().sync();
		outputStream.close();

	}

	public static boolean isConnectedToSameMsisdn(JSONObject message, String connectedMsisdn)
	{

		if (TextUtils.isEmpty(connectedMsisdn))
		{
			return false;
		}
		if (isPingPacket(message))
		{
			return true;
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
			return jsonFile.getString(HikeConstants.FILE_PATH);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return null;
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
			MessageMetadata md = new MessageMetadata(metadata, true);
			isFileTransferMessage = md.getHikeFiles() != null && md.getHikeFiles().size() > 0;
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

	public static void closeSocket(final Socket socket) throws IOException
	{
		if (socket == null)
		{
			return;
		}
		final InputStream is = socket.getInputStream();
		try
		{
			if(socket.isOutputShutdown())
			{
				Logger.d(TAG,"Output is already  shutdown");
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
			if(socket.isInputShutdown())
			{
				Logger.d(TAG,"Input is already  shutdown");
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

		HikeHandlerUtil.getInstance().postRunnableWithDelay(new Runnable()
		{
			
			@Override
			public void run()
			{
				try
				{
					while (is.read() >= 0)
					{

						Logger.d(TAG, "in While Loop");
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				try
				{
					socket.close();
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}, 0);
		
		
		// "read()" returns '-1' when the 'FIN' is reached

	}

	public static void closeSocket(ServerSocket serverSocket) throws IOException
	{
		if(serverSocket==null)
		{
			return;
		}
			serverSocket.close();
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
	
	public static long getStkLenFrmPkt(JSONObject packet)
	{
		if (packet.optJSONObject(HikeConstants.DATA) != null)
		{
			try
			{
				JSONObject metaData = packet.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);
				return metaData.optLong(HikeConstants.FILE_SIZE, -1);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		return -1;
	}
	
    
    public static  boolean isAvailable(Socket _socket_){
        if (isConnected(_socket_)) {
            try
            {
                if (_socket_.getInetAddress() == null) {
                    return false;
                }
                if (_socket_.getPort() == 0) {
                    return false;
                }
                if (_socket_.getRemoteSocketAddress() == null) {
                    return false;
                }
                if (_socket_.isClosed()) {
                    return false;
                }
                /* these aren't exact checks (a Socket can be half-open),
                   but since we usually require two-way data transfer,
                   we check these here too: */
                if (_socket_.isInputShutdown()) {
                    return false;
                }
                if (_socket_.isOutputShutdown()) {
                    return false;
                }
                /* ignore the result, catch exceptions: */
                _socket_.getInputStream();
                _socket_.getOutputStream();
            }
            catch (IOException ioex)
            {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }
    
    public static boolean isConnected(Socket _socket_)
	{
		if (_socket_ == null)
		{
			return false;
		}

		return _socket_.isConnected();
	}
}
