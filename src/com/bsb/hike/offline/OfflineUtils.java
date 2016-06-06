package com.bsb.hike.offline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Action;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.notifications.HikeNotificationMsgStack;
import com.bsb.hike.offline.OfflineConstants.OFFLINE_STATE;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.hike.transporter.utils.TConstants.ERRORCODES;
import com.bsb.hike.chatthread.ChatThreadActivity;
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

	public static void saveDeliveryReceipt(long msgId,ConvMessage message){

		HikeConversationsDatabase.getInstance().saveDeliveryReceipt(msgId,message.getMsisdn(),message.getTimestamp(),message.getMsisdn());
	}

	public static long getTotalChunks(long fileSize)
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
		storagePath.append(File.separator + fileName);
		return storagePath.toString();
	}

	public static String createOfflineMsisdn(String msisdn)
	{
		return new StringBuilder("o:").append(msisdn).toString();
	}

	public static ConvMessage createOfflineInlineConvMessage(String msisdn, String message, String type)
	{
		ConvMessage convMessage = Utils.makeConvMessage(msisdn, message, true, State.RECEIVED_UNREAD);
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
		Sticker stickerObject = getSticker(sticker);

		return (stickerObject == null) ? "" : stickerObject.getLargeStickerPath();
	}

    public static Sticker getSticker(JSONObject sticker)
    {
        Sticker tempStk = null;
        try
        {
            String ctgId = sticker.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getString(StickerManager.CATEGORY_ID);
            String stkId = sticker.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getString(StickerManager.STICKER_ID);

            tempStk = new Sticker(ctgId, stkId);

        }
        catch (JSONException e)
        {
            Logger.e(TAG, "JSONException in getLargeStickerPath. Check whether JSONObject is a sticker.");
            e.printStackTrace();
        }
        return tempStk;
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

	public static long getFileSizeFromJSON(JSONObject packet)
	{
		try
		{
			JSONArray jsonFiles = packet.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES);
			JSONObject jsonFile = jsonFiles.getJSONObject(0);
			return jsonFile.optLong(HikeConstants.FILE_SIZE);
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
			metadata = getMetadata(packet);
			if (metadata != null)
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

	public static String createStkDirectory(JSONObject messageJSON) throws JSONException, IOException
    {
        String ctgId = messageJSON.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getString(StickerManager.CATEGORY_ID);
        String stkId = messageJSON.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getString(StickerManager.STICKER_ID);
        Sticker sticker = new Sticker(ctgId, stkId);

        File stickerImage;
        String tempPath = getOfflineStkPath(ctgId, stkId);
        
        if(TextUtils.isEmpty(tempPath))
        {
			Logger.e(TAG, "No Sticker direct found");
        	return null;
        }
        
        //String stickerPath = sticker.getLargeStickerPath(HikeMessengerApp.getInstance().getApplicationContext());
        stickerImage = new File(tempPath);

        // sticker is not present
        if (stickerImage == null || (stickerImage.exists() == false))
        {
            File parent = new File(stickerImage.getParent());
            if (!parent.exists())
                parent.mkdirs();
            // stickerImage.createNewFile();
        }
        return tempPath;
    }

	public static String getOfflineStkPath(String ctgId, String stkId)
	{
		String rootPath = StickerManager.getInstance().getStickerCategoryDirPath(ctgId);
		if (TextUtils.isEmpty(rootPath))
		{
			return null;
		}
		String[] pathTokens = rootPath.split("/");
		String tempPath = "";
		for (int i = 0; i < (pathTokens.length - 1); i++)
		{
			tempPath += pathTokens[i] + "/";
		}
		tempPath += "SO/" + ctgId + "/" + stkId;
		Logger.d(TAG, tempPath);
		return tempPath;
	}
	
	public static boolean isSSIDWithQuotes(String ssid)
	{
		if(TextUtils.isEmpty(ssid))
		{
			return false;
		}
		if (ssid.length() > 2 && ssid.startsWith("\"") &&ssid.endsWith("\""))
		{
			return true;
		}
		return false;
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

	public static JSONObject getFileTransferMetadataForContact(JSONObject contactJson) throws JSONException
	{
		contactJson.put(HikeConstants.FILE_NAME, contactJson.optString(HikeConstants.NAME, HikeConstants.CONTACT_FILE_NAME));
		contactJson.put(HikeConstants.CONTENT_TYPE, HikeConstants.CONTACT_CONTENT_TYPE);
		
		JSONArray files = new JSONArray();
		files.put(contactJson);
		JSONObject metadata = new JSONObject();
		metadata.put(HikeConstants.FILES, files);

		return metadata;
	}

	private static ConvMessage createConvMessage(String msisdn, JSONObject metadata, boolean isRecipientOnhike)
	{
		long time = System.currentTimeMillis() / 1000;
		ConvMessage convMessage = new ConvMessage(HikeConstants.CONTACT_FILE_NAME, msisdn, time, ConvMessage.State.SENT_UNCONFIRMED);
		try
		{
			convMessage.setMetadata(metadata);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return convMessage;
	}

	public static ConvMessage createOfflineContactConvMessage(String msisdn, JSONObject jsonData, boolean onHike)
	{
		ConvMessage convMessage = null;
		try
		{
			convMessage = createConvMessage(msisdn, getFileTransferMetadataForContact(jsonData), onHike);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return convMessage;
	}

	public static boolean isContactTransferMessage(JSONObject packet)
	{

		boolean isContactTransferMessage = false;
		JSONObject metadata;
		JSONArray files;
		JSONObject fileJSON;
		try
		{
			metadata = getMetadata(packet);
			if (metadata != null)
			{
				files = metadata.optJSONArray(HikeConstants.FILES);
				if (files != null)
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

	public static JSONObject getMetadata(JSONObject packet)
	{
		try
		{
			if (packet.optJSONObject(HikeConstants.DATA)!=null)
			{
				if (packet.optJSONObject(HikeConstants.DATA).has(HikeConstants.METADATA))
				{
					return packet.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);
				}
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return null;
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

	public static MessageMetadata getUpdatedMessageMetaData(ConvMessage msg)
	{

		JSONObject metaData = msg.getMetadata().getJSON();
		JSONArray filesArray = new JSONArray();
		JSONObject fileJSON = null;
		HikeFile hikeFile = msg.getMetadata().getHikeFiles().get(0);
		try
		{
			fileJSON = metaData.getJSONArray(HikeConstants.FILES).getJSONObject(0);
			String fileName = fileJSON.getString(HikeConstants.FILE_NAME);
			File sourceFile = new File(fileJSON.optString(HikeConstants.FILE_PATH));
			hikeFile.setFileKey("OfflineKey" + System.currentTimeMillis() / 1000);
			hikeFile.setFile(sourceFile);
			hikeFile.setFileSize(sourceFile.length());
			hikeFile.setFileName(fileName);
			hikeFile.setSent(true);
			fileJSON = hikeFile.serialize();
			filesArray.put(fileJSON);
			metaData.put(HikeConstants.FILES, filesArray);
			MessageMetadata messageMetadata = new MessageMetadata(metaData, true);
			return messageMetadata;
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public static int getErrorStringId(ERRORCODES e)
	{
		if (e == ERRORCODES.FILE_NOT_EXISTS)
			return R.string.file_expire;
		else if (e == ERRORCODES.NOT_CONNECTED)
			return R.string.already_connected_offline;
		else if (e == ERRORCODES.NOT_ENOUGH_MEMORY)
			return R.string.not_enough_space_receiver;
		else if (e == ERRORCODES.SD_CARD_NOT_PRESENT)
			return R.string.no_sdcard_receiver;
		else if (e == ERRORCODES.SD_CARD_NOT_WRITABLE)
			return R.string.receiver_cannot_receive;
		else
			return R.string.unknown_error;
	}

	public static JSONObject createInfoPkt(long connectID)
	{
		JSONObject object = new JSONObject();
		try
		{
			object.put(HikeConstants.TYPE, OfflineConstants.INFO_PKT);
			object.put(HikeConstants.VERSION,Utils.getAppVersionName());
			object.put(OfflineConstants.OFFLINE_VERSION,OfflineConstants.OFFLINE_VERSION_NUMER);
			object.put(HikeConstants.RESOLUTION_ID, Utils.getResolutionId());
			object.put(OfflineConstants.CONNECTION_ID, connectID);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return object;
	}

	public static boolean isInfoPkt(JSONObject packet)
	{
		return packet.optString(HikeConstants.TYPE).equals(OfflineConstants.INFO_PKT);
	}
	
	public static void toggleToAndFromField(JSONObject message, String connectedDevice) throws JSONException 
	{
		message.put(HikeConstants.FROM,connectedDevice);
		message.remove(HikeConstants.TO);
	}

	public static boolean isMessageReadType(JSONObject messageJSON) {
		
		return HikeConstants.MqttMessageTypes.MESSAGE_READ.equals(messageJSON.optString(HikeConstants.TYPE));
	}

	public static String getConnectedMsisdn() {
		return OfflineController.getInstance().getConnectedDevice();
	}

	public static boolean isConnectedToSameMsisdn(String msisdn)
	{
		if(TextUtils.isEmpty(msisdn))
		{
			return false;
		}
		String connectedMsisdn = OfflineController.getInstance().getConnectedDevice();
		return !TextUtils.isEmpty(connectedMsisdn) && connectedMsisdn.equals(msisdn) && OfflineController.getInstance().getOfflineState() == OFFLINE_STATE.CONNECTED;
	}

	public static boolean isConnectingToSameMsisdn(String msisdn) 
	{	
		if(TextUtils.isEmpty(msisdn))
		{
			return false;
		}
		String connectingMsisdn = OfflineController.getInstance().getConnectingDevice(); 
		return !TextUtils.isEmpty(connectingMsisdn) && connectingMsisdn.equals(msisdn) && OfflineController.getInstance().getOfflineState() ==OFFLINE_STATE.CONNECTING;
	}

	public static String getConnectingMsisdn()
	{
		return OfflineController.getInstance().getConnectingDevice();
	}
	
	public static void stopFreeHikeConnection(Context context, String msisdn) 
	{
		if(OfflineController.getInstance().getOfflineState().equals(OFFLINE_STATE.CONNECTED))	
		{
			//HikeMessengerApp.getInstance().showToast(context.getString(R.string.disconnect_offline));
			OfflineController.getInstance().shutDown();
		}
		else if(OfflineController.getInstance().getOfflineState().equals(OFFLINE_STATE.CONNECTING))
		{
			//HikeMessengerApp.getInstance().showToast(context.getString(R.string.connection_cancelled));	
			OfflineController.getInstance().shutdown(new OfflineException(OfflineException.CONNECTION_CANCELLED));
		}
		
	}

	public static void sendOfflineRequestPacket(String targetMsisdn)
	{
		JSONObject message = new JSONObject();
		JSONObject data = new JSONObject();
		try
		{
			data.put(HikeConstants.TYPE,HikeConstants.GeneralEventMessagesTypes.OFFLINE);
		    data.put(HikeConstants.SUB_TYPE, HikeConstants.OFFLINE_MESSAGE_REQUEST);
			data.put(HikeConstants.TIMESTAMP,System.currentTimeMillis() / 1000);
			OfflineParameters offlineParameters = OfflineController.getInstance().getConfigurationParamerters();
			data.put(OfflineConstants.TIMEOUT,offlineParameters.getConnectionTimeout());
			message.put(HikeConstants.TO, targetMsisdn);
			message.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.GENERAL_EVENT_PACKET_ZERO);
			message.put(HikeConstants.DATA, data);
			HikeMqttManagerNew.getInstance().sendMessage(message, MqttConstants.MQTT_QOS_ZERO);
		}
		catch (JSONException e)
		{
			Logger.d(TAG, "Error in Json");
		}
	}

	public static boolean isHotSpotCreated(String connectedMsisdn)
	{
		if(TextUtils.isEmpty(connectedMsisdn))
		{
			return false;
		}
		
		String myMsisdn=getMyMsisdn();
		
		if (TextUtils.isEmpty(myMsisdn))
		{
			return false;
		}
		return (myMsisdn.compareTo(connectedMsisdn) > 0);
		
	}
	
	
	public static void handleOfflineRequestPacket(Context context,JSONObject packet)
	{
		String msisdn;
		try
		{
			msisdn = packet.getString(HikeConstants.FROM);
			OfflineParameters offlineParameters = OfflineController.getInstance().getConfigurationParamerters();
			if(TextUtils.isEmpty(msisdn)||isConnectedToSameMsisdn(msisdn)|| isConnectingToSameMsisdn(msisdn) || !offlineParameters.isOfflineEnabled())
			{
				return;
			}
			
			NotificationCompat.Action[] actions = getNotificationActions(context,msisdn);
			Intent intent = IntentFactory.createChatThreadIntentFromMsisdn(context, msisdn, false,false, ChatThreadActivity.ChatThreadOpenSources.NOTIF);
			intent.putExtra(OfflineConstants.START_CONNECT_FUNCTION, true);
			intent.putExtra(HikeConstants.C_TIME_STAMP, System.currentTimeMillis());
			HikeNotificationMsgStack hikeNotifMsgStack =  HikeNotificationMsgStack.getInstance();
			Drawable avatarDrawable = Utils.getAvatarDrawableForNotification(context,msisdn, false);
			ContactInfo contactInfo  = ContactManager.getInstance().getContact(msisdn);
			String contactFirstName = msisdn;
			if (StealthModeManager.getInstance().isStealthMsisdn(msisdn) && !StealthModeManager.getInstance().isActive())
			{
				intent = Utils.getHomeActivityIntent(context);
				HikeNotification.getInstance().showOfflineRequestStealthNotification(intent, context.getString(R.string.app_name),
						context.getString(R.string.incoming_hike_direct_request), context.getString(R.string.incoming_hike_direct_request), R.drawable.ic_stat_notify);
			}
			else
			{
				if (contactInfo != null && !TextUtils.isEmpty(contactInfo.getFirstName()))
				{
					contactFirstName = contactInfo.getFirstNameAndSurname();
				}

				HikeNotification.getInstance().showBigTextStyleNotification(intent, hikeNotifMsgStack.getNotificationIcon(), System.currentTimeMillis() / 1000,
						HikeNotification.OFFLINE_REQUEST_ID, context.getString(R.string.incoming_hike_direct_request), contactFirstName,
						context.getString(R.string.hike_direct_request), msisdn, null, avatarDrawable, false, 0, actions);
			}
			OfflineController.getInstance().handleOfflineRequest(packet);

		}
		catch (JSONException e)
		{
			Logger.d(TAG, "Error in JSon");
		}

	}

	private static Action[] getNotificationActions(Context context, String msisdn)
	{
		Intent chatThreadIntent = IntentFactory.createChatThreadIntentFromMsisdn(context, msisdn, false,false,
				ChatThreadActivity.ChatThreadOpenSources.NOTIF);
		chatThreadIntent.putExtra(OfflineConstants.START_CONNECT_FUNCTION, true);
		PendingIntent chatThreadPendingIntent = PendingIntent.getActivity(context, 0, chatThreadIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Intent cancel = new Intent("com.bsb.cancel");
		cancel.putExtra(HikeConstants.MSISDN, msisdn);
		PendingIntent cancelP = PendingIntent.getBroadcast(context, 0, cancel, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Action actions[] = new NotificationCompat.Action[2];

		
		actions[0] = new NotificationCompat.Action(R.drawable.ic_notifcrossicon, context.getString(R.string.cancel), cancelP);
		actions[1] = new NotificationCompat.Action(R.drawable.offline_inline_logo_white, context.getString(R.string.connect), chatThreadPendingIntent);

		return actions;
	}
	
	public static void sendOfflineRequestCancelPacket(String targetMsisdn)
	{
		JSONObject message = new JSONObject();
		JSONObject data = new JSONObject();
		try
		{
			data.put(HikeConstants.TYPE, HikeConstants.OFFLINE);
			data.put(HikeConstants.SUB_TYPE, HikeConstants.OFFLINE_MESSAGE_REQUEST_CANCEL);
			data.put(HikeConstants.TIMESTAMP, System.currentTimeMillis() / 1000);
			message.put(HikeConstants.TO, targetMsisdn);
			message.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.GENERAL_EVENT_PACKET_ZERO);
			message.put(HikeConstants.DATA, data);
			HikeMqttManagerNew.getInstance().sendMessage(message, MqttConstants.MQTT_QOS_ZERO);
		}
		catch (JSONException e)
		{
			Logger.d(TAG, "Error in Json");
		}
	}

	public static void handleOfflineCancelRequestPacket(Context context, JSONObject packet)
	{
		try
		{
			String msisdn = packet.getString(HikeConstants.FROM);
			if(OfflineUtils.isConnectingToSameMsisdn(msisdn))
			{
				OfflineController.getInstance().shutdown(new OfflineException(OfflineException.CANCEL_NOTIFICATION_REQUEST));
			}
			
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		
	}

	public static boolean shouldShowDisconnectFragment(String data)
	{
		if (TextUtils.isEmpty(data))
		{
			return false;
		}

		JSONObject obj;
		try
		{
			obj = new JSONObject(data);
			long timeout = obj.optLong(OfflineConstants.TIMEOUT);
			if (timeout > System.currentTimeMillis())
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
	
	public static String fetchMsisdnFromRequestPkt(String data)
	{
		JSONObject obj;
		try
		{
			obj = new JSONObject(data);
			return obj.optString(HikeConstants.MSISDN);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public static void sendInlineConnectRequest(String msisdn)
	{
		if (TextUtils.isEmpty(msisdn))
		{
			return;
		}
	/*	ContactInfo contactInfo = ContactManager.getInstance().getContact(msisdn);
		String contactName = msisdn;
		if (contactInfo != null && !TextUtils.isEmpty(contactInfo.getName()))
		{
			contactName = contactInfo.getName();
		}*/
		
		ConvMessage  convMessage =  createOfflineInlineConvMessage(msisdn, HikeMessengerApp.getInstance().getApplicationContext()
				.getString(R.string.connection_request_inline_msg),OfflineConstants.OFFLINE_INLINE_MESSAGE );
		HikeConversationsDatabase.getInstance().addConversationMessages(convMessage, true);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
	}
	
	public static void showToastForBatteryLevel()
	{
		float batteryLevel = Utils.currentBatteryLevel();
		if (batteryLevel <= 0 || batteryLevel > OfflineConstants.MIN_BATTERY_LEVEL)
		{
			return;
		}

		HikeMessengerApp.getInstance().showToast(R.string.low_battery_msg,Toast.LENGTH_LONG);
	}
	
	public static boolean isFeautureAvailable(int myVersion,int clientTwoVersion,int minClientVersion)
	{
		if(myVersion>= minClientVersion &&  clientTwoVersion >=minClientVersion)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public static int getConnectedDeviceVersion()
	{
		OfflineClientInfoPOJO connectedClientInfo  = OfflineController.getInstance().getConnectedClientInfo();
		if(connectedClientInfo!=null)
		{
			return connectedClientInfo.getOfflineVersionNumber();
		}
		return 1;
	}
	
	public static boolean willConnnectToHotspot(String connectingMisdn)
	{
		String myMsisdn = getMyMsisdn();
		return (myMsisdn.compareTo(connectingMisdn) < 0);
	}
	
	public static boolean isVoipPacket(JSONObject messageJSON)
	{
		String type = messageJSON.optString(HikeConstants.TYPE);
		if (TextUtils.isEmpty(type))
		{
			return false;
		}
		if (HikeConstants.MqttMessageTypes.MESSAGE_VOIP_0.equals(type) || HikeConstants.MqttMessageTypes.MESSAGE_VOIP_1.equals(type))
		{
			return true;
		}
		return false;

	}
	
	public static void handleUnsupportedPeer(Context context, JSONObject packet)
	{
	
		try
		{
			String msisdn = packet.getString(HikeConstants.FROM);
			
			if(OfflineUtils.isConnectingToSameMsisdn(msisdn))
			{
				JSONObject data  =  packet.optJSONObject(HikeConstants.DATA);
				if(data!=null)
				{
					//String errorMessage = data.getString(HikeConstants.HIKE_MESSAGE);
					//AND-4022. Making error message local, since server
					String errorMessage = context.getString(R.string.error_message_unsupported_peer);
					OfflineController.getInstance().shutdown(new OfflineException(OfflineException.UNSUPPORTED_PEER,errorMessage));
				}
				
			}
			
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "JsonException while handling hike direct peer unsupported packet");
		}
		
	}

	public static void handleUpgradablePeer(Context context, JSONObject packet)
	{
		try
		{
			String msisdn = packet.getString(HikeConstants.FROM);
			
			if(OfflineUtils.isConnectingToSameMsisdn(msisdn))
			{
				JSONObject data  =  packet.optJSONObject(HikeConstants.DATA);
				if(data!=null)
				{
					//String errorMessage = data.getString(HikeConstants.HIKE_MESSAGE);
					//AND-4022. Making error message local, since server
					String errorMessage = context.getString(R.string.error_message_upgrade_peer);
					OfflineController.getInstance().shutdown(new OfflineException(OfflineException.UPGRADABLE_UNSUPPORTED_PEER,errorMessage));
				}
				
			}
			
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "JsonException while handling hike direct peer upgrade packet");
		}
	}

	public static void recordHikeDirectOverFlowClicked()
	{
		try
		{
			JSONObject metaData = new JSONObject();
			metaData.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.HIKE_DIRECT_OVRFL_CLK);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, HAManager.EventPriority.HIGH, metaData);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}
}
	