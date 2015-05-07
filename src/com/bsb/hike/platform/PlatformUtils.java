package com.bsb.hike.platform;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.productpopup.ProductPopupsConstants.HIKESCREEN;
import com.bsb.hike.ui.CreateNewGroupOrBroadcastActivity;
import com.bsb.hike.ui.HikeListActivity;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.StatusUpdate;
import com.bsb.hike.ui.TellAFriend;
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
				if (oldHelper == null)
				{
					oldHelper = new JSONObject();
				}
				Iterator<String> i = helperData.keys();
				while (i.hasNext())
				{
					String key = i.next();
					oldHelper.put(key, helperData.get(key));
				}
				cardObj.put(HikePlatformConstants.HELPER_DATA, oldHelper);
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
}
