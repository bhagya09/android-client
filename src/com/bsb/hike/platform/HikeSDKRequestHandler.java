package com.bsb.hike.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.ui.HikeAuthActivity;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * Handles requests made by HikeSDK.
 * 
 * @author AtulM
 * 
 */
public class HikeSDKRequestHandler extends Handler implements Listener
{
	
	private Context mContext;

	/** The cached message. */
	private static Message cachedMessage;

	private String cachedToken;

	private Handler authUIHandler;

	/**
	 * Instantiates a new hike sdk request handler.
	 *
	 * @param argContext the arg context
	 * @param looper the looper
	 */
	public HikeSDKRequestHandler(final Context argContext, Looper looper)
	{
		super(looper);
		authUIHandler = new Handler()
		{
			public void handleMessage(android.os.Message msg)
			{
				if (!Utils.requireAuth(mContext, true))
				{
					return;
				}
				IntentManager.openHikeSDKAuth(mContext, Message.obtain(msg));
			};
		};

		mContext = argContext;
		HikeMessengerApp.getPubSub().addListener(HikePubSub.AUTH_TOKEN_RECEIVED, HikeSDKRequestHandler.this);
	}

	/**
	 * Return error/failed operation message back to caller messenger.
	 *
	 * @param argMessage the arg message
	 */
	private void returnExceptionMessageToCaller(Message argMessage)
	{
		argMessage.arg2 = HikeSDKResponseCode.STATUS_EXCEPTION;
		try
		{
			Logger.i("hikesdk", argMessage.replyTo.hashCode() + "");
			argMessage.replyTo.send(argMessage);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
		return;
	}

	/* (non-Javadoc)
	 * @see android.os.Handler#handleMessage(android.os.Message)
	 */
	@Override
	public void handleMessage(Message msg)
	{
		super.handleMessage(msg);

		try
		{
			Logger.d(HikeSDKRequestHandler.class.getCanonicalName(), "Handle message: Verifying!");

			if (msg.arg2 == HikeSDKResponseCode.STATUS_FAILED)
			{
				returnExceptionMessageToCaller(msg);
				return;
			}

			if (!HikeAuthActivity.verifyRequest(mContext, Message.obtain(msg)))
			{
				cachedMessage = Message.obtain(msg);
				authUIHandler.sendMessage(Message.obtain(msg));
				return;
			}

			Logger.d(HikeSDKRequestHandler.class.getCanonicalName(), "Handle message: Verified!");

			Logger.d(HikeSDKRequestHandler.class.getCanonicalName(), "message.what== " + msg.what);

			if (msg.what == HikeService.SDK_REQ_GET_LOGGED_USER_INFO)
			{

				if (!isMessageValid(msg))
				{
					return;
				}
				Bundle reqUserInfoBundle = msg.getData();

				// User info is saved in shared preferences
				SharedPreferences preferences = mContext.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE);

				ContactInfo userInfo = Utils.getUserContactInfo(preferences);

				JSONObject reqUserInfoResponseJSON = new JSONObject();

				JSONArray reqUserInfoJSONArray = new JSONArray();

				if (userInfo != null)
				{
					JSONObject friendJSON = new JSONObject();

					String contactId = userInfo.getId();

					String contactName = userInfo.getNameOrMsisdn();

					if (!TextUtils.isEmpty(contactId) && !TextUtils.isEmpty(contactName))
					{
						try
						{
							friendJSON.put(HikeUser.HIKE_USER_ID_KEY, "-1");

							friendJSON.put(HikeUser.HIKE_USER_NAME_KEY, contactName);
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}

						reqUserInfoJSONArray.put(friendJSON);
					}
				}
				reqUserInfoResponseJSON.put(HikeUser.HIKE_USERS_LIST_ID, reqUserInfoJSONArray);

				if (cachedToken != null)
				{
					reqUserInfoResponseJSON.put(HikeSDKConstants.HIKE_REQ_SDK_CLIENT_ACC_TOKEN, cachedToken);
					cachedToken = null;
				}

				reqUserInfoBundle.putString(HikeSDKConstants.HIKE_REQ_DATA_ID, reqUserInfoResponseJSON.toString());

				msg.setData(reqUserInfoBundle);

				// Set STATUS_OK
				msg.arg2 = HikeSDKResponseCode.STATUS_OK;

				msg.replyTo.send(msg);

			}
			else if (msg.what == HikeService.SDK_REQ_GET_USERS)
			{

				Bundle reqGetFriendsBundle = msg.getData();

				if (!isMessageValid(msg))
				{
					return;
				}

				JSONObject reqGetFriendsData = null;

				try
				{
					reqGetFriendsData = new JSONObject(reqGetFriendsBundle.getString(HikeSDKConstants.HIKE_REQ_DATA_ID));
				}
				catch (JSONException e)
				{
					e.printStackTrace();
					returnExceptionMessageToCaller(msg);
					return;
				}
				String requestFilter = "-1";
				try
				{
					requestFilter = reqGetFriendsData.getString(HikeSDKConstants.HIKE_REQ_FILTER_ID);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				List<ContactInfo> contacts = null;

				int reqFilterKey = 0;
				try
				{
					reqFilterKey = Integer.parseInt(requestFilter);
				}
				catch (NumberFormatException nfe)
				{
					nfe.printStackTrace();
					returnExceptionMessageToCaller(msg);
					return;
				}

				switch (reqFilterKey)
				{
				case -1:
					// Favourites
					contacts = ContactManager.getInstance().getContactsOfFavoriteType(FavoriteType.FRIEND, 1, "");
					break;
				case -2:
					// On hike
					contacts = new ArrayList<ContactInfo>();
					List<ContactInfo> allContacts = ContactManager.getInstance().getAllContacts();
					for (ContactInfo contact : allContacts)
					{
						if (contact.isOnhike())
						{
							contacts.add(contact);
						}
					}
					break;
				case -3:
					// Not on hike
					contacts = new ArrayList<ContactInfo>();
					List<Pair<AtomicBoolean, ContactInfo>> nonHikePair = ContactManager.getInstance().getNonHikeContacts();
					for (Pair<AtomicBoolean, ContactInfo> pair : nonHikePair)
					{
						contacts.add(pair.second);
					}
					break;
				default:
					// All contacts
					contacts = ContactManager.getInstance().getAllContacts();
				}

				JSONObject responseJSON = new JSONObject();

				JSONArray friendsListJSON = new JSONArray();

				for (ContactInfo contact : contacts)
				{
					JSONObject friendJSON = new JSONObject();

					String contactId = contact.getId();

					String contactName = contact.getNameOrMsisdn();

					if (!TextUtils.isEmpty(contactId) && !TextUtils.isEmpty(contactName))
					{
						try
						{
							friendJSON.put(HikeUser.HIKE_USER_ID_KEY, contactId);
							friendJSON.put(HikeUser.HIKE_USER_NAME_KEY, contactName);
						}
						catch (JSONException jsonException)
						{
							jsonException.printStackTrace();
						}
						friendsListJSON.put(friendJSON);
					}
				}

				responseJSON.put(HikeUser.HIKE_USERS_LIST_ID, friendsListJSON);

				if (cachedToken != null)
				{
					responseJSON.put(HikeSDKConstants.HIKE_REQ_SDK_CLIENT_ACC_TOKEN, cachedToken);
					cachedToken = null;
				}

				reqGetFriendsBundle.putString(HikeSDKConstants.HIKE_REQ_DATA_ID, responseJSON.toString());

				msg.setData(reqGetFriendsBundle);

				// Set STATUS_OK
				msg.arg2 = HikeSDKResponseCode.STATUS_OK;

				msg.replyTo.send(msg);

			}
			else if (msg.what == HikeService.SDK_REQ_SEND_MESSAGE)
			{

				Bundle reqSendMessageBundle = msg.getData();

				if (!isMessageValid(msg))
				{
					return;
				}

				HikeMessengerApp.getPubSub().publish(HikePubSub.HIKE_SDK_MESSAGE, reqSendMessageBundle.get(HikeSDKConstants.HIKE_REQ_DATA_ID));

				if (cachedToken != null)
				{
					JSONObject sendMessageResponseJSON = new JSONObject();
					sendMessageResponseJSON.put(HikeSDKConstants.HIKE_REQ_SDK_CLIENT_ACC_TOKEN, cachedToken);
					Bundle sendMessageBundle = new Bundle();
					sendMessageBundle.putString(HikeSDKConstants.HIKE_REQ_DATA_ID, sendMessageResponseJSON.toString());
					msg.setData(sendMessageBundle);
				}

				// Set STATUS_OK
				msg.arg2 = HikeSDKResponseCode.STATUS_OK;

				msg.replyTo.send(msg);

			}
			else if (msg.what == HikeService.SDK_REQ_AUTH_CLIENT)
			{
				if (!isMessageValid(msg))
				{
					return;
				}

				// Set STATUS_OK
				msg.arg2 = HikeSDKResponseCode.STATUS_OK;

				msg.replyTo.send(msg);
			}
			else
			{
				returnExceptionMessageToCaller(msg);
				return;
			}
		}
		catch (JSONException e)
		{
			handleException(msg, e);
		}
		catch (RemoteException e)
		{
			handleException(msg, e);
		}
		catch (NullPointerException e)
		{
			handleException(msg, e);
		}
	}

	/**
	 * Checks if is message valid.
	 *
	 * @param msg the msg
	 * @return true, if is message valid
	 */
	private boolean isMessageValid(Message msg)
	{
		Bundle messageBundle = msg.getData();

		if (messageBundle == null || messageBundle.isEmpty())
		{
			returnExceptionMessageToCaller(msg);
			return false;
		}

		postAnalyticsEvents(messageBundle);
		return true;
	}

	/**
	 * Handle exception.
	 *
	 * @param msg the msg
	 * @param e the e
	 */
	private void handleException(Message msg, Exception e)
	{
		returnExceptionMessageToCaller(msg);
		e.printStackTrace();
		return;
	}

	/**
	 * Post analytics events.
	 *
	 * @param argBundle the arg bundle
	 */
	private void postAnalyticsEvents(Bundle argBundle)
	{
		String requestData = argBundle.getString(HikeSDKConstants.HIKE_REQ_DATA_ID);
		if (requestData == null)
		{
			return;
		}
		else
		{
			try
			{
				JSONObject requestJSON = new JSONObject(requestData);
				if (!TextUtils.isEmpty(requestJSON.optString(HikeSDKConstants.PREF_HIKE_SDK_INSTALL_CLICKED_KEY)))
				{
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SDK_INSTALL_HIKE_ACCEPT);
					metadata.put(HikeConstants.Extras.SDK_THIRD_PARTY_PKG, requestJSON.getString(HikeSDKConstants.HIKE_REQ_SDK_CLIENT_PKG_NAME));
					metadata.put(HikeConstants.LogEvent.SOURCE_APP, HikePlatformConstants.GAME_SDK_ID);
					HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.HIKE_SDK_INSTALL_ACCEPT, metadata);
				}
				if (!TextUtils.isEmpty(requestJSON.optString(HikeSDKConstants.PREF_HIKE_SDK_INSTALL_DENIED_KEY)))
				{
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SDK_INSTALL_HIKE_DECLINE);
					metadata.put(HikeConstants.Extras.SDK_THIRD_PARTY_PKG, requestJSON.getString(HikeSDKConstants.HIKE_REQ_SDK_CLIENT_PKG_NAME));
					metadata.put(HikeConstants.LogEvent.SOURCE_APP, HikePlatformConstants.GAME_SDK_ID);
					HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.HIKE_SDK_INSTALL_DECLINE, metadata);
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
				return;
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.bsb.hike.HikePubSub.Listener#onEventReceived(java.lang.String, java.lang.Object)
	 */
	@Override
	public void onEventReceived(String type, Object object)
	{
		if (type.equals(HikePubSub.AUTH_TOKEN_RECEIVED))
		{
			try
			{
				cachedToken = (String) object;
				if (cachedMessage != null)
				{
					Bundle cachedMessageBundle = cachedMessage.getData();

					String cachedMessageData = cachedMessageBundle.getString(HikeSDKConstants.HIKE_REQ_DATA_ID);

					try
					{
						JSONObject cachedMessageDataJSON = new JSONObject(cachedMessageData);
						cachedMessageDataJSON.put(HikeSDKConstants.HIKE_REQ_SDK_CLIENT_ACC_TOKEN, cachedToken);
						cachedMessageBundle.putString(HikeSDKConstants.HIKE_REQ_DATA_ID, cachedMessageDataJSON.toString());
						cachedMessage.setData(cachedMessageBundle);
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
					catch (NullPointerException e)
					{
						e.printStackTrace();
					}

					// TODO move this to util thread
					handleMessage(cachedMessage);

					cachedMessage = null;
				}
			}
			catch (ClassCastException cce)
			{
				cce.printStackTrace();
				if (cachedMessage != null)
				{
					returnExceptionMessageToCaller(cachedMessage);
					cachedMessage = null;
					return;
				}
			}
		}

	}
}