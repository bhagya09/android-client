package com.bsb.hike.platform.nativecards;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.CardComponent;
import com.bsb.hike.platform.CustomTabFallBackImpl;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * Created by pushkargupta on 05/05/16. Class to have all utility methods related to cards , cards to be moved
 */
public class NativeCardUtils
{
	public enum ActionType
	{
		FORWARD("forward", R.drawable.ic_forward), POST_TIMELINE("timeline", R.drawable.ic_post_timeline), OPEN_URL("open_url", R.drawable.ic_forward), OPEN_CAMERA("open_camera",
				R.drawable.ic_forward), OPEN_VIDEO("open_video", R.drawable.ic_forward), OPEN_IMAGE("open_image", R.drawable.ic_forward), OPEN_MAPS("maps", R.drawable.ic_forward), SHARE(
				"share", R.drawable.ic_share);
		private String action;

		private int drawableId;

		ActionType(String action, int drawableId)
		{
			this.action = action;
			this.drawableId = drawableId;
		}

		public String getAction()
		{
			return action;
		}

		public int getDrawableId()
		{
			return drawableId;
		}

	}

	public static File getFileForView(View view, Context mContext)
	{
		if (view == null)
		{
			return null;
		}
		FileOutputStream fos = null;
		File cardShareImageFile = null;
		cardShareImageFile = new File(mContext.getExternalCacheDir(), System.currentTimeMillis() + ".jpg");
		try
		{
			fos = new FileOutputStream(cardShareImageFile);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		Bitmap b = Utils.viewToBitmap(view);
		b.compress(Bitmap.CompressFormat.JPEG, 100, fos);
		try
		{
			fos.flush();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (fos != null)
			{
				try
				{
					fos.close();
				}
				catch (IOException e)
				{
					// Do nothing
					e.printStackTrace();
				}
			}

		}

		return cardShareImageFile;

	}

	public static void performAction(Context context, View shareView, CardComponent.ActionComponent actionComponent, ConvMessage convMessage) throws JSONException
	{
		if (actionComponent.getAction().equals(ActionType.FORWARD.getAction()))
		{
			forwardCard(context, shareView, convMessage);
		}
		else if (actionComponent.getAction().equals(ActionType.POST_TIMELINE.getAction()))
		{
			postToTimeLine(context, shareView, convMessage);
		}else if(actionComponent.getAction().equals(ActionType.SHARE.getAction())){
			shareCard(context,shareView, convMessage);
		}else if(actionComponent.getAction().equals(ActionType.OPEN_URL.getAction())){
			CustomTabFallBackImpl fallBack = new CustomTabFallBackImpl(context);
			PlatformUtils.openCustomTab(actionComponent.getActionUrl().getString(HikeConstants.URL), actionComponent.getActionUrl().optString(HikeConstants.TITLE),context, fallBack);
		}
		else if (actionComponent.getAction().equals(ActionType.OPEN_CAMERA.getAction()))
		{

		}
		else if (actionComponent.getAction().equals(ActionType.OPEN_IMAGE))
		{

		}
		else if (actionComponent.getAction().equals(ActionType.OPEN_VIDEO))
		{

		}
		else if (actionComponent.getAction().equals(ActionType.OPEN_MAPS))
		{

		}

	}

	public static int getDrawable(CardComponent.ActionComponent actionComponent)
	{

		if (actionComponent.getAction().equals(ActionType.FORWARD.getAction()))
		{
			return ActionType.FORWARD.getDrawableId();
		}
		else if (actionComponent.getAction().equals(ActionType.POST_TIMELINE.getAction()))
		{
			return ActionType.POST_TIMELINE.getDrawableId();
		}
		else if (actionComponent.getAction().equals(ActionType.SHARE.getAction()))
		{
			return ActionType.SHARE.getDrawableId();
		}
		else if (actionComponent.getAction().equals(ActionType.OPEN_URL.getAction()))
		{
			return ActionType.OPEN_URL.getDrawableId();
		}
		else if (actionComponent.getAction().equals(ActionType.OPEN_CAMERA.getAction()))
		{
			return ActionType.OPEN_CAMERA.getDrawableId();
		}
		else if (actionComponent.getAction().equals(ActionType.OPEN_IMAGE))
		{
			return ActionType.OPEN_IMAGE.getDrawableId();
		}
		else if (actionComponent.getAction().equals(ActionType.OPEN_VIDEO))
		{
			return ActionType.OPEN_VIDEO.getDrawableId();
		}
		else if (actionComponent.getAction().equals(ActionType.OPEN_MAPS))
		{
			return ActionType.OPEN_MAPS.getDrawableId();
		}
		else
		{
			return -1;
		}
	}

	public static void forwardCard(Context context, View view, ConvMessage convMessage)
	{

		File fileUri = null;
		if (convMessage.platformMessageMetadata.getHikeFiles() != null && convMessage.platformMessageMetadata.getHikeFiles().size() > 0) {
			fileUri = convMessage.platformMessageMetadata.getHikeFiles().get(0).getFile();
			if (fileUri == null || !fileUri.exists()) {
				Toast.makeText(context, R.string.download_image_before_sharing, Toast.LENGTH_SHORT).show();
				return;
			}
		} else {
			fileUri = NativeCardUtils.getFileForView(view, HikeMessengerApp.getInstance());
		}
		boolean showTimeLine = fileUri!=null && fileUri.exists();
		JSONArray multipleMsgArray = new JSONArray();
		JSONObject multipleMsgObject = getNativeCardForwardJSON(context, convMessage, fileUri);
		multipleMsgArray.put(multipleMsgObject);
		Intent intent = IntentFactory.getForwardIntentForCards(context, convMessage);
		intent.putExtra(AnalyticsConstants.NATIVE_CARD_FORWARD, convMessage.platformMessageMetadata.contentId);
		if(showTimeLine){
			intent.putExtra(HikeConstants.Extras.SHOW_TIMELINE, true);
		}
		intent.putExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT, multipleMsgArray.toString());
		context.startActivity(intent);
	}
    public static JSONObject getNativeCardForwardJSON(Context context, ConvMessage convMessage, File file){
		JSONObject multiMsgFwdObject = new JSONObject();
		JSONObject metadata = convMessage.platformMessageMetadata.getJsonFromObj();
		try
		{
			multiMsgFwdObject.put(HikeConstants.MESSAGE_TYPE.MESSAGE_TYPE, convMessage.getMessageType());
			if (metadata != null)
			{
				multiMsgFwdObject.put(HikeConstants.METADATA, metadata);
			}

			multiMsgFwdObject.put(HikeConstants.HIKE_MESSAGE, convMessage.getMessage());
			if (file != null)
			{
				// intent.putExtra((Intent.EXTRA_STREAM),fileUri);
				multiMsgFwdObject.put(HikeConstants.Extras.FILE_PATH, file.getPath());
				multiMsgFwdObject.put(HikeConstants.Extras.FILE_TYPE, "image/jpeg");

			}

		}
		catch (JSONException e)
		{
			Logger.e(context.getClass().getSimpleName(), "Invalid JSON", e);
		}
		return multiMsgFwdObject;
	}
	public static void postToTimeLine(Context context, View view, ConvMessage convMessage)
	{
		File file;
		if (convMessage.platformMessageMetadata.getHikeFiles() != null && convMessage.platformMessageMetadata.getHikeFiles().size() > 0) {
			file = convMessage.platformMessageMetadata.getHikeFiles().get(0).getFile();
			if (file == null || !file.exists()) {
				Toast.makeText(context, R.string.download_image_before_sharing, Toast.LENGTH_SHORT).show();
				return;
			}
		} else {
			file = NativeCardUtils.getFileForView(view, HikeMessengerApp.getInstance());
		}
		Intent intent = IntentFactory.getPostStatusUpdateIntent(context, null, file.getPath(), true);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(intent);
	}

	public static void shareCard(Context context, View view, ConvMessage convMessage) {
		File file;
		if (convMessage.platformMessageMetadata.getHikeFiles() != null && convMessage.platformMessageMetadata.getHikeFiles().size() > 0) {
			file = convMessage.platformMessageMetadata.getHikeFiles().get(0).getFile();
			if (file == null || !file.exists()) {
				Toast.makeText(context, R.string.download_image_before_sharing, Toast.LENGTH_SHORT).show();
				return;
			}
			Intent intent = IntentFactory.shareIntent("image/jpeg", file.getAbsolutePath(), null, HikeConstants.Extras.ShareTypes.IMAGE_SHARE, null,
					true);
			context.startActivity(intent);
		} else {
			file = NativeCardUtils.getFileForView(view, HikeMessengerApp.getInstance());
			Intent intent = IntentFactory.shareIntentWithFileProviderPath(context, "image/jpeg", file);
			context.startActivity(intent);
		}

	}

	public static boolean isNativeCardFTMessage(ConvMessage convMessage){
		if(convMessage != null && convMessage.platformMessageMetadata != null && convMessage.platformMessageMetadata.getHikeFiles() != null && convMessage.platformMessageMetadata.getHikeFiles().size()>0){
			return true;
		}
		return false;
	}
}
