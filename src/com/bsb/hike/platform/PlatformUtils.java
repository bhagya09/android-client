package com.bsb.hike.platform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpHeaderConstants;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.platform.content.PlatformContentListener;
import com.bsb.hike.platform.content.PlatformContentModel;
import com.bsb.hike.platform.content.PlatformContentRequest;
import com.bsb.hike.platform.content.PlatformZipDownloader;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.productpopup.ProductPopupsConstants.HIKESCREEN;
import com.bsb.hike.ui.CreateNewGroupOrBroadcastActivity;
import com.bsb.hike.ui.HikeListActivity;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.StatusUpdate;
import com.bsb.hike.ui.TellAFriend;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * @author piyush
 * 
 *         Class for all Utility methods related to Platform code
 */
public class PlatformUtils
{
	private static final String TAG = "PlatformUtils";
	
	private static final String BOUNDARY = "----------V2ymHFg03ehbqgZCaKO6jy";
	
	/**
	 * 
	 * metadata:{'layout_id':'','file_id':'','card_data':{},'helper_data':{}}
	 * 
	 * This function reads helper json given in parameter and update it in metadata of message , it inserts new keys in metadata present in helper and updates old
	 */
	public static String updateHelperData(String helper, String originalMetadata)
	{

		if (originalMetadata != null)
		{
			try
			{
				JSONObject metadataJSON = new JSONObject(originalMetadata);
				JSONObject helperData = new JSONObject(helper);
				JSONObject cardObj = metadataJSON.optJSONObject(HikePlatformConstants.CARD_OBJECT);
				JSONObject oldHelper = cardObj.optJSONObject(HikePlatformConstants.HELPER_DATA);
				JSONObject newHelperData = mergeJSONObjects(oldHelper, helperData);
				cardObj.put(HikePlatformConstants.HELPER_DATA, newHelperData);
				metadataJSON.put(HikePlatformConstants.CARD_OBJECT, cardObj);
				originalMetadata = metadataJSON.toString();
				return originalMetadata;
			}
			catch (JSONException e)
			{
				Logger.e(TAG, "Caught a JSON Exception in UpdateHelperMetadata" + e.toString());
				e.printStackTrace();
			}
		}
		else
		{
			Logger.e(TAG, "Meta data is null in UpdateHelperData");
		}
		return null;
	}

	/**
	 * Call this function to merge two JSONObjects. Will iterate for the keys present in the dataDiff. Will add the key in the oldData if not already
	 * present or will update the value in oldData if the key is present.
	 * @param oldData : the data that wants to be merged.
	 * @param dataDiff : the diff that will be merged with the old data.
	 * @return : the merged data.
	 */
	public static JSONObject mergeJSONObjects(JSONObject oldData, JSONObject dataDiff)
	{
		if (oldData == null)
		{
			oldData = new JSONObject();
		}
		Iterator<String> i = dataDiff.keys();
		while (i.hasNext())
		{
			String key = i.next();
			try
			{
				oldData.put(key, dataDiff.get(key));
			}
			catch (JSONException e)
			{
				Logger.e(TAG, "Caught a JSON Exception while merging helper data" + e.toString());
			}
		}
		return oldData;
	}
	
	public static void openActivity(Activity context, String data)
	{
		String activityName = null;
		JSONObject mmObject = null;

		if (context == null || TextUtils.isEmpty(data))
		{
			Logger.e(TAG, "Either activity is null or data is empty/null in openActivity");
			return;
		}

		try
		{
			mmObject = new JSONObject(data);
			activityName = mmObject.optString(HikeConstants.SCREEN);

			if (activityName.equals(HIKESCREEN.SETTING.toString()))
			{
				IntentFactory.openSetting(context);
			}

			if (activityName.equals(HIKESCREEN.ACCOUNT.toString()))
			{
				IntentFactory.openSettingAccount(context);
			}
			if (activityName.equals(HIKESCREEN.FREE_SMS.toString()))
			{
				IntentFactory.openSettingSMS(context);
			}
			if (activityName.equals(HIKESCREEN.MEDIA.toString()))
			{
				IntentFactory.openSettingMedia(context);
			}
			if (activityName.equals(HIKESCREEN.NOTIFICATION.toString()))
			{
				IntentFactory.openSettingNotification(context);
			}
			if (activityName.equals(HIKESCREEN.PRIVACY.toString()))
			{
				IntentFactory.openSettingPrivacy(context);
			}
			if (activityName.equals(HIKESCREEN.TIMELINE.toString()))
			{
				IntentFactory.openTimeLine(context);
			}
			if (activityName.equals(HIKESCREEN.NEWGRP.toString()))
			{
				context.startActivity(new Intent(context, CreateNewGroupOrBroadcastActivity.class));
			}
			if (activityName.equals(HIKESCREEN.INVITEFRNDS.toString()))
			{
				context.startActivity(new Intent(context, TellAFriend.class));
			}
			if (activityName.equals(HIKESCREEN.REWARDS_EXTRAS.toString()))
			{
				context.startActivity(IntentFactory.getRewardsIntent(context));
			}
			if (activityName.equals(HIKESCREEN.STICKER_SHOP.toString()))
			{
				context.startActivity(IntentFactory.getStickerShopIntent(context));
			}
			if (activityName.equals(HIKESCREEN.STICKER_SHOP_SETTINGS.toString()))
			{
				context.startActivity(IntentFactory.getStickerSettingIntent(context));
			}
			if (activityName.equals(HIKESCREEN.STATUS.toString()))
			{
				context.startActivity(new Intent(context, StatusUpdate.class));
			}
			if (activityName.equals(HIKESCREEN.HIDDEN_MODE.toString()))
			{
				if (context instanceof HomeActivity)
				{
					((HomeActivity) context).hikeLogoClicked();
				}
			}
			if (activityName.equals(HIKESCREEN.COMPOSE_CHAT.toString()))
			{
				context.startActivity(IntentFactory.getComposeChatIntent(context));
			}
			if (activityName.equals(HIKESCREEN.INVITE_SMS.toString()))
			{
				boolean selectAll = mmObject.optBoolean(ProductPopupsConstants.SELECTALL, false);
				Intent intent = new Intent(context, HikeListActivity.class);
				intent.putExtra(ProductPopupsConstants.SELECTALL, selectAll);
				context.startActivity(intent);
			}
			if (activityName.equals(HIKESCREEN.FAVOURITES.toString()))
			{
				context.startActivity(IntentFactory.getFavouritesIntent(context));
			}
			if (activityName.equals(HIKESCREEN.HOME_SCREEN.toString()))
			{
				context.startActivity(Utils.getHomeActivityIntent(context));
			}
			if (activityName.equals(HIKESCREEN.PROFILE_PHOTO.toString()))
			{

				Intent intent = IntentFactory.getProfileIntent(context);
				if (mmObject.optBoolean(ProductPopupsConstants.SHOW_CAMERA, false))
				{
					intent.putExtra(ProductPopupsConstants.SHOW_CAMERA, true);
				}
				context.startActivity(intent);

			}
			if (activityName.equals(HIKESCREEN.EDIT_PROFILE.toString()))
			{
				Intent intent = IntentFactory.getProfileIntent(context);
				intent.putExtra(HikeConstants.Extras.EDIT_PROFILE, true);
				context.startActivity(intent);

			}
			if (activityName.equals(HIKESCREEN.INVITE_WHATSAPP.toString()))
			{
				IntentFactory.openInviteWatsApp(context);
			}
			if (activityName.equals(HIKESCREEN.OPENINBROWSER.toString()))
			{
				String url = mmObject.optString(HikeConstants.URL);

				if (!TextUtils.isEmpty(url))
				{
					Intent in = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					context.startActivity(in);
				}
			}
			if (activityName.equals(HIKESCREEN.OPENAPPSTORE.toString()))
			{
				String url = mmObject.optString(HikeConstants.URL);

				if (!TextUtils.isEmpty(url))
				{
					Utils.launchPlayStore(url, context);
				}
			}
			if (activityName.equals(HIKESCREEN.HELP.toString()))
			{
				IntentFactory.openSettingHelp(context);
			}
			if (activityName.equals(HIKESCREEN.NUXINVITE.toString()))
			{
				context.startActivity(IntentFactory.openNuxFriendSelector(context));
			}
			if (activityName.equals(HIKESCREEN.NUXREMIND.toString()))
			{
				context.startActivity(IntentFactory.openNuxCustomMessage(context));
			}
			if (activityName.equals(HIKESCREEN.BROADCAST.toString()))
			{
				IntentFactory.createBroadcastDefault(context);
			}
			if (activityName.equals(HIKESCREEN.CHAT_HEAD.toString()))
			{
				IntentFactory.openSettingStickerOnOtherApp(context);
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		catch (ActivityNotFoundException e)
		{
			Toast.makeText(context, "No activity found", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}

	}

	/**
	 * download the microapp and then set the state to whatever that has been passed by the server.
	 * @param botInfo
	 * @param enableBot
	 */
	public static void downloadZipForNonMessagingBot(final BotInfo botInfo, final boolean enableBot, final String botChatTheme, final String notifType)
	{
		PlatformContentRequest rqst = PlatformContentRequest.make(
				PlatformContentModel.make(botInfo.getMetadata()), new PlatformContentListener<PlatformContentModel>()
				{

					@Override
					public void onComplete(PlatformContentModel content)
					{
						Logger.d(TAG, "microapp download packet success.");
						botCreationSuccessHandling(botInfo, enableBot, botChatTheme, notifType);
					}

					@Override
					public void onEventOccured(int uniqueCode,PlatformContent.EventCode event)
					{
						if (event == PlatformContent.EventCode.DOWNLOADING || event == PlatformContent.EventCode.LOADED)
						{
							//do nothing
							return;
						}
						else if (event == PlatformContent.EventCode.ALREADY_DOWNLOADED)
						{
							Logger.d(TAG, "microapp already exists");
							botCreationSuccessHandling(botInfo, enableBot, botChatTheme, notifType);
						}
						else
						{
							Logger.wtf(TAG, "microapp download packet failed." + event.toString());
							JSONObject json = new JSONObject();
							try
							{
								json.put(HikePlatformConstants.ERROR_CODE, event.toString());
								createBotAnalytics(HikePlatformConstants.BOT_CREATION_FAILED, botInfo);
							}
							catch (JSONException e)
							{
								e.printStackTrace();
							}

						}
					}
				});

		downloadAndUnzip(rqst, false);

	}

	public static void botCreationSuccessHandling(BotInfo botInfo, boolean enableBot, String botChatTheme, String notifType)
	{
		enableBot(botInfo, enableBot);
		BotUtils.updateBotParamsInDb(botChatTheme, botInfo, enableBot, notifType);
		createBotAnalytics(HikePlatformConstants.BOT_CREATED, botInfo);
	}

	private static void createBotAnalytics(String key, BotInfo botInfo)
	{
		createBotAnalytics(key, botInfo, null);
	}

	private static void createBotAnalytics(String key, BotInfo botInfo, JSONObject json)
	{
		if (json == null)
		{
			json = new JSONObject();
		}
		try
		{
			json.put(AnalyticsConstants.EVENT_KEY, key);
			json.put(AnalyticsConstants.BOT_NAME, botInfo.getConversationName());
			json.put(AnalyticsConstants.BOT_MSISDN, botInfo.getMsisdn());
			json.put(HikePlatformConstants.PLATFORM_USER_ID, HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_UID_SETTING, null));
			HikeAnalyticsEvent.analyticsForNonMessagingBots(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.DOWNLOAD_EVENT, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private static void enableBot(BotInfo botInfo, boolean enableBot)
	{
		if (enableBot && botInfo.isNonMessagingBot())
		{
			HikeConversationsDatabase.getInstance().addNonMessagingBotconversation(botInfo);
		}
	}

	/**
	 * download the microapp, can be used by nonmessaging as well as messaging only to download and unzip the app.
	 * @param downloadData: the data used to download microapp from ac packet to download the app.
	 */
	public static void downloadZipFromPacket(final JSONObject downloadData)
	{
		if (downloadData == null)
		{
			return;
		}

		final PlatformContentModel platformContentModel = PlatformContentModel.make(downloadData.toString());
		PlatformContentRequest rqst = PlatformContentRequest.make(
				platformContentModel, new PlatformContentListener<PlatformContentModel>()
				{

					@Override
					public void onComplete(PlatformContentModel content)
					{
						microappDownloadAnalytics(HikePlatformConstants.MICROAPP_DOWNLOADED, content);
						Logger.d(TAG, "microapp download packet success.");
					}

					@Override
					public void onEventOccured(int uniqueId,PlatformContent.EventCode event)
					{

						if (event == PlatformContent.EventCode.DOWNLOADING || event == PlatformContent.EventCode.LOADED)
						{
							//do nothing
							return;
						}

						JSONObject jsonObject = new JSONObject();
						try
						{
							jsonObject.put(HikePlatformConstants.ERROR_CODE, event.toString());
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}

						if (event == PlatformContent.EventCode.ALREADY_DOWNLOADED)
						{
							microappDownloadAnalytics(HikePlatformConstants.MICROAPP_DOWNLOADED, platformContentModel, jsonObject);
							Logger.d(TAG, "microapp already exists.");
						}
						else
						{
							microappDownloadAnalytics(HikePlatformConstants.MICROAPP_DOWNLOAD_FAILED, platformContentModel, jsonObject);
							Logger.wtf(TAG, "microapp download packet failed.Because it is" + event.toString());
						}
					}
				});
				boolean doReplace = downloadData.optBoolean(PlatformContentConstants.REPLACE_MICROAPP_VERSION);
				downloadAndUnzip(rqst, false,doReplace);

	}

	private static void microappDownloadAnalytics(String key, PlatformContentModel content)
	{
		microappDownloadAnalytics(key, content, null);
	}

	private static void microappDownloadAnalytics(String key, PlatformContentModel content, JSONObject json)
	{
		if (json == null)
		{
			json = new JSONObject();
		}

		try
		{
			json.put(AnalyticsConstants.EVENT_KEY, key);
			json.put(AnalyticsConstants.APP_NAME, content.getId());
			json.put(HikePlatformConstants.PLATFORM_USER_ID, HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_UID_SETTING, null));
			HikeAnalyticsEvent.analyticsForNonMessagingBots(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.DOWNLOAD_EVENT, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void downloadAndUnzip(PlatformContentRequest request, boolean isTemplatingEnabled , boolean doReplace)
	{

		PlatformZipDownloader downloader =  new PlatformZipDownloader(request, isTemplatingEnabled, doReplace);
		if (!downloader.isMicroAppExist() || doReplace)
		{
			downloader.downloadAndUnzip();
		}
		else
		{
			request.getListener().onEventOccured(request.getContentData()!=null ? request.getContentData().getUniqueId() : 0,PlatformContent.EventCode.ALREADY_DOWNLOADED);
		}
		
	}
	public static void downloadAndUnzip(PlatformContentRequest request, boolean isTemplatingEnabled)
	{
		downloadAndUnzip(request, isTemplatingEnabled, false);
	}

	/**
	 * Creating a forwarding message for Non-messaging microApp
	 * @param metadata: the metadata made after merging the json given by the microApp
	 * @param text:     hm text
	 * @return
	 */
	public static ConvMessage getConvMessageFromJSON(JSONObject metadata, String text, String msisdn)
	{


		ConvMessage convMessage = Utils.makeConvMessage(msisdn, true);
		convMessage.setMessage(text);
		convMessage.setMessageType(HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT);
		convMessage.webMetadata = new WebMetadata(metadata);
		convMessage.setMsisdn(msisdn);
		return convMessage;

	}
	
	public static byte[] prepareFileBody(String filePath)
	{
		String boundary = "\r\n--" + BOUNDARY + "--\r\n";
		File file = new File(filePath);
		int chunkSize = (int) file.length();
		String boundaryMessage = getBoundaryMessage(filePath);
		byte[] fileContent = new byte[(int) file.length()];
		FileInputStream fileInputStream = null;
	    try
		{
	    	fileInputStream = new FileInputStream(file);
			fileInputStream.read(fileContent);
		}
		catch (IOException | NullPointerException e)
		{
			e.printStackTrace();
		}
	    finally
	    {
		    try
			{
	    		if(fileInputStream != null)
	    		{
					fileInputStream.close();
	    		}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
	    }
	    return setupFileBytes(boundaryMessage, boundary, chunkSize,fileContent);
	}
	
	public static void uploadFile(final String filePath,final String url,final IFileUploadListener fileListener)
	{
		if(filePath == null)
		{
			Logger.d("FileUpload", "File Path specified as null");
			fileListener.onRequestFailure("File Path null");
		}
		HikeHandlerUtil mThread = HikeHandlerUtil.getInstance();
		mThread.startHandlerThread();
		mThread.postRunnable(new Runnable()
		{
			
			@Override
			public void run()
			{
			    byte[] fileBytes = prepareFileBody(filePath);
				String response = send(fileBytes,filePath,url,fileListener);
				Logger.d("FileUpload", response);
			}
		});

	}
	
	private static String send(byte[] fileBytes,final String filePath,final String url,IFileUploadListener filelistener)
	{
		HttpClient client = new DefaultHttpClient();
		client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, HikeConstants.CONNECT_TIMEOUT);
		long so_timeout = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.FT_UPLOAD_SO_TIMEOUT, 180 * 1000l);
		Logger.d("UploadFileTask", "Socket timeout = " + so_timeout);
		client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, (int) so_timeout);
		client.getParams().setParameter(CoreConnectionPNames.TCP_NODELAY, true);
		client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "android-" + AccountUtils.getAppVersion());
		
		HttpPost post = new HttpPost(url);
		String res = null;
		int resCode = 0;
		try
		{
			post.setHeader("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
			post.setEntity(new ByteArrayEntity(fileBytes));
			HttpResponse response = client.execute(post);
			Logger.d("FileUpload", response.toString());
			resCode = response.getStatusLine().getStatusCode();
			
			res = EntityUtils.toString(response.getEntity());
		}
		catch (IOException | NullPointerException ex)
		{
			Logger.d("FileUpload", ex.toString());
			filelistener.onRequestFailure(ex.toString());
			return ex.toString();
		}
		Logger.d("FileUpload", res);
		if(resCode == 200)
		{
			filelistener.onRequestSuccess(res);
		}
		else
		{
			filelistener.onRequestFailure(res);
		}
		return res;
	}
	/*
	 * gets the boundary message for the file path
	 */
	private static String getBoundaryMessage(String filePath)
	{
		String sendingFileType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(filePath));
		File selectedFile = new File(filePath);
		StringBuffer res = new StringBuffer("--").append(BOUNDARY).append("\r\n");
		String name = selectedFile.getName();
		res.append("Content-Disposition: form-data; name=\"").append("file").append("\"; filename=\"").append(name).append("\"\r\n").append("Content-Type: ")
				.append(sendingFileType).append("\r\n\r\n");
		return res.toString();
	}
	
	/*
	 * Sets up the file byte array with boundary message File Content and boundary
	 * returns the completed setup file byte array
	 */
	private static byte[] setupFileBytes(String boundaryMesssage, String boundary, int chunkSize,byte[] fileContent)
	{
		byte[] fileBytes = new byte[boundaryMesssage.length() + fileContent.length + boundary.length()];
		try
		{
			System.arraycopy(boundaryMesssage.getBytes(), 0, fileBytes, 0, boundaryMesssage.length());
			System.arraycopy(fileContent, 0, fileBytes, boundaryMesssage.length(), fileContent.length);
			System.arraycopy(boundary.getBytes(), 0, fileBytes, boundaryMesssage.length() + fileContent.length, boundary.length());
		}
		catch(NullPointerException | ArrayStoreException | IndexOutOfBoundsException e)
		{
			e.printStackTrace();
			Logger.d("FileUpload", e.toString());
		}
		return fileBytes;
	}

	public static List<Header> getHeaders()
	{

		HikeSharedPreferenceUtil mpref = HikeSharedPreferenceUtil.getInstance();
		String platformUID = mpref.getData(HikeMessengerApp.PLATFORM_UID_SETTING, null);
		String platformToken = mpref.getData(HikeMessengerApp.PLATFORM_TOKEN_SETTING, null);
		if (!TextUtils.isEmpty(platformToken) && !TextUtils.isEmpty(platformUID))
		{
			List<Header> headers = new ArrayList<Header>(1);
			headers.add(new Header(HttpHeaderConstants.COOKIE_HEADER_NAME,
					HikePlatformConstants.PLATFORM_TOKEN + "=" + platformToken + "; " + HikePlatformConstants.PLATFORM_USER_ID + "=" + platformUID));

			return headers;
		}
		return new ArrayList<Header>();
	}
	
	/*
	 * This function is called to read the list of files from the System from a folder
	 * 
	 * @param filePath : The complete file path that is about to be read returns the JSON Array of the file paths of the all the files in a folder
	 * @param doDeepLevelAccess : To specify if we want to read all the internal files and folders recursively
	 */
	public static JSONArray readFileList(String filePath,boolean doDeepLevelAccess)
	{	
		File directory = new File(filePath);
		if (directory.exists() && !directory.isDirectory())
		{
			Logger.d("FileSystemAccess", "Cannot read a single file");
			return null;
		}
		else if(!directory.exists())
		{
			Logger.d("FileSystemAccess", "Invalid file path!");
			return null;
		}
		ArrayList<File> list = filesReader(directory,doDeepLevelAccess);
		JSONArray mArray = new JSONArray();
		for (int i = 0; i < list.size(); i++)
		{
			String path = HikePlatformConstants.FILE_DESCRIPTOR + list.get(i).getAbsolutePath();// adding the file descriptor
			mArray.put(path);
		}
		return mArray;
	}
	
	public static JSONArray trimFilePath(JSONArray mArray)
	{
		JSONArray trimmedArray = new JSONArray();
		for (int i = 0; i < mArray.length(); i++)
		{
			String path;
			try
			{
				path = mArray.get(i).toString();
				path = path.replaceAll(PlatformContentConstants.PLATFORM_CONTENT_DIR, "");
				path = path.replaceAll(HikePlatformConstants.FILE_DESCRIPTOR, "");
				trimmedArray.put(path);
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return trimmedArray;
	}

	// Method that returns the reads the list of files
	public static ArrayList<File> filesReader(File root,boolean doDeepLevelAccess)
	{
		ArrayList<File> a = new ArrayList<>();

		File[] files = root.listFiles();
		for (int i = 0; i < files.length; i++)
		{
			if (doDeepLevelAccess)
			{
				if(files[i].isDirectory())
				{
					a.addAll(filesReader(files[i],doDeepLevelAccess));	
				}
				else
				{
					a.add(files[i]);
				}

			}
			else
			{
				a.add(files[i]);
			}
		}
		return a;
	}

	/*
	 * This function is called to copy a directory from one location to another location 
	 * @param sourceLocation : The folder which is about to be copied
	 * @param targetLocation : The folder where the directory is about to be copied
	 */
	public static boolean copyDirectoryTo(File sourceLocation, File targetLocation) throws IOException
	{
		if (sourceLocation.isDirectory())
		{
			if (!targetLocation.exists())
			{
				targetLocation.mkdir();
			}
			String[] children = sourceLocation.list();
			for (int i = 0; i < sourceLocation.listFiles().length; i++)
			{
				copyDirectoryTo(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
			}
		}
		else
		{
			
			  InputStream in = new FileInputStream(sourceLocation);
			  OutputStream out = new FileOutputStream(targetLocation);
			  byte[] buf = new byte[1024]; int len;
			  while ((len = in.read(buf)) > 0) 
			  { 
				  out.write(buf, 0, len); 
			  }
			  in.close();
			  out.close();
		}
		return true;
	}

	/*
	 * This function is called to delete a particular file from the System
	 * 
	 * @param filePath : The complete file path of the file that is about to be deleted returns whether the file is deleted or not
	 * Does not return a guaranteed call for a full delete
	 */
	public static boolean deleteDirectory(String filePath)
	{
		File deletedDir = new File(filePath);
		if (deletedDir.exists())
		{
			boolean isDeleted = deleteOp(deletedDir);
			Logger.d("FileSystemAccess", "Directory exists!");
			Logger.d("FileSystemAccess", (isDeleted) ? "File is deleted" : " File not deleted");
			return isDeleted;
		}
		else
		{
			Logger.d("FileSystemAccess", "Invalid file path!");
			return false;
		}
	}

	// This method performs the actual deletion of the file
	public static boolean deleteOp(File dir)
	{
		Logger.d("FileSystemAccess", "In delete");
		if (dir.exists())
		{// This checks if the file/folder exits or not
			if (dir.isDirectory())// This checks if the call is made to delete a particular file (eg. "index.html") or an entire sub-folder
			{
				String[] children = dir.list();
				for (int i = 0; i < children.length; i++)
				{
					File temp = new File(dir, children[i]);
					if (temp.isDirectory())
					{
						Logger.d("DeleteRecursive", "Recursive Call" + temp.getPath());
						deleteOp(temp);
					}
					else
					{
						Logger.d("DeleteRecursive", "Delete File" + temp.getPath());
						boolean b = temp.delete();
						if (!b)
						{
							Logger.d("DeleteRecursive", "DELETE FAIL");
							return false;
						}
					}
				}
				dir.delete();
			}
			else
			{
				dir.delete();
			}
			Logger.d("FileSystemAccess", "Delete done!");
			return true;
		}
		return false;
	}

}
