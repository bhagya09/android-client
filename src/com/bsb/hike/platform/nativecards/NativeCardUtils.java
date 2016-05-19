package com.bsb.hike.platform.nativecards;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.CardComponent;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by pushkargupta on 05/05/16.
 * Class to have all utility methods related to cards , cards to be moved
 */
public class NativeCardUtils
{
	public enum ActionType{
		FORWARD("forward", R.drawable.ic_share), POST_TIMELINE("timeline", R.drawable.ic_share), OPEN_URL("open_url", R.drawable.ic_share),OPEN_CAMERA("open_camera", R.drawable.ic_share),OPEN_VIDEO("open_video",R.drawable.ic_share),
		OPEN_IMAGE("open_image", R.drawable.ic_share), OPEN_MAPS("maps", R.drawable.ic_share), SHARE("share", R.drawable.ic_share);
		private String action;
		private int drawableId;
		ActionType(String action, int drawableId){
			this.action = action;
			this.drawableId = drawableId;
		}
		public String getAction(){
			return action;
		}
		public int getDrawableId(){
			return drawableId;
		}

	}
	public static File getFileForView(View view, Context mContext)
	{
		if(view == null){
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


	public static void performAction(Context context, View shareView, CardComponent.ActionComponent actionComponent, ConvMessage convMessage) throws JSONException {
		if(actionComponent.getAction().equals(ActionType.FORWARD.getAction())){
			shareCard(context,shareView,convMessage);
		}else if(actionComponent.getAction().equals(ActionType.POST_TIMELINE.getAction())){
			postToTimeLine(context, shareView);
		}else if(actionComponent.getAction().equals(ActionType.SHARE.getAction())){
			shareCard(context,shareView);
		}else if(actionComponent.getAction().equals(ActionType.OPEN_URL.getAction())){
            Intent intent = IntentFactory.getWebViewActivityIntent(context, actionComponent.getActionUrl().getString(HikeConstants.URL), null);
			context.startActivity(intent);
		}else if(actionComponent.getAction().equals(ActionType.OPEN_CAMERA.getAction())){

		}else if(actionComponent.getAction().equals(ActionType.OPEN_IMAGE)){

		}else if(actionComponent.getAction().equals(ActionType.OPEN_VIDEO)){

		}else if(actionComponent.getAction().equals(ActionType.OPEN_MAPS)){

		}

	}
	public static int getDrawable(CardComponent.ActionComponent actionComponent){

		if(actionComponent.getAction().equals(ActionType.FORWARD.getAction())){
			return ActionType.FORWARD.getDrawableId();
		}else if(actionComponent.getAction().equals(ActionType.POST_TIMELINE.getAction())){
			return ActionType.POST_TIMELINE.getDrawableId();
		}else if(actionComponent.getAction().equals(ActionType.SHARE.getAction())){
			return ActionType.SHARE.getDrawableId();
		}else if(actionComponent.getAction().equals(ActionType.OPEN_URL.getAction())){
			return ActionType.OPEN_URL.getDrawableId();
		}else if(actionComponent.getAction().equals(ActionType.OPEN_CAMERA.getAction())){
			return ActionType.OPEN_CAMERA.getDrawableId();
		}else if(actionComponent.getAction().equals(ActionType.OPEN_IMAGE)){
			return ActionType.OPEN_IMAGE.getDrawableId();
		}else if(actionComponent.getAction().equals(ActionType.OPEN_VIDEO)){
			return ActionType.OPEN_VIDEO.getDrawableId();
		}else if(actionComponent.getAction().equals(ActionType.OPEN_MAPS)){
			return ActionType.OPEN_MAPS.getDrawableId();
		}else{
			return -1;
		}
	}

	public static void shareCard(Context context, View view, ConvMessage convMessage){
		File fileUri = NativeCardUtils.getFileForView(view, HikeMessengerApp.getInstance());
		Intent intent = IntentFactory.getForwardIntentForCards(context, convMessage, fileUri);
		context.startActivity(intent);
	}

	public static void postToTimeLine(Context context, View view){
		File fileUri = NativeCardUtils.getFileForView(view, HikeMessengerApp.getInstance());
		Intent intent = IntentFactory.getPostStatusUpdateIntent(context, null, fileUri.getPath(),true);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(intent);
	}

	public static void shareCard(Context context, View view){
		File fileUri = NativeCardUtils.getFileForView(view, HikeMessengerApp.getInstance());
		Intent intent = IntentFactory.shareIntent("image/jpeg", fileUri.getPath(), null, HikeConstants.Extras.ShareTypes.IMAGE_SHARE,null, true);
		context.startActivity(intent);
	}


}
