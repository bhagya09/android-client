package com.bsb.hike.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.utils.HikeSSLUtil;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.StickerTaskBase;
import com.bsb.hike.utils.Utils;

public class DownloadSingleStickerTask extends StickerTaskBase {

	private String urlString;
	private String dirPath;
	private String largeStickerPath;
	private String smallStickerPath;
	private String key;
	private String stId;

	public DownloadSingleStickerTask(Context context, String catId, String stId) {
		this.key = catId + stId;
		this.stId = stId;
		this.dirPath = Utils.getStickerDirectoryForCategoryId(context, catId);

		this.largeStickerPath = this.dirPath + HikeConstants.LARGE_STICKER_ROOT
				+ "/" + stId;
		this.smallStickerPath = this.dirPath + HikeConstants.SMALL_STICKER_ROOT
				+ "/" + stId;

		this.urlString = AccountUtils.base + "/stickers?catId=" + catId
				+ "&stId=" + stId + "&resId=" + Utils.getResolutionId();
	}

	@Override
	protected FTResult doInBackground(Void... arg0) {
		if (dirPath == null) {
			return FTResult.DOWNLOAD_FAILED;
		}

		FileOutputStream fos = null;
		try {
			File largeDir = new File(dirPath + HikeConstants.LARGE_STICKER_ROOT);
			if (!largeDir.exists()) {
				if (!largeDir.mkdirs()) {
					return FTResult.DOWNLOAD_FAILED;
				}
			}
			File smallDir = new File(dirPath + HikeConstants.SMALL_STICKER_ROOT);
			if (!smallDir.exists()) {
				if (!smallDir.mkdirs()) {
					return FTResult.DOWNLOAD_FAILED;
				}
			}

			Log.d(getClass().getSimpleName(), "Downloading sticker: "
					+ urlString);
			URL url = new URL(urlString);

			URLConnection connection = url.openConnection();
			AccountUtils.addUserAgent(connection);
			connection.addRequestProperty("Cookie", "user="
					+ AccountUtils.mToken + "; UID=" + AccountUtils.mUid);

			Log.d(getClass().getSimpleName(),
					"File size: " + connection.getContentLength());
			if (AccountUtils.ssl) {
				((HttpsURLConnection) connection)
						.setSSLSocketFactory(HikeSSLUtil.getSSLSocketFactory());
			}

			JSONObject response = AccountUtils.getResponse(connection
					.getInputStream());

			if (response == null
					|| !HikeConstants.OK.equals(response
							.getString(HikeConstants.STATUS))) {
				return FTResult.DOWNLOAD_FAILED;
			}

			JSONObject data = response.getJSONObject(HikeConstants.DATA_2);

			String stickerData = data.getString(stId);

			Utils.saveBase64StringToFile(new File(largeStickerPath),
					stickerData);

			boolean isDisabled = data.optBoolean(HikeConstants.DISABLED_ST);
			if (!isDisabled) {
				Bitmap thumbnail = Utils.scaleDownImage(largeStickerPath, -1,
						false);

				File smallImage = new File(smallStickerPath);
				Utils.saveBitmapToFile(smallImage, thumbnail);
			}
		} catch (JSONException e) {
			Log.e(getClass().getSimpleName(), "Invalid JSON", e);
			return FTResult.DOWNLOAD_FAILED;
		} catch (MalformedURLException e) {
			Log.e(getClass().getSimpleName(), "Invalid URL", e);
			return FTResult.DOWNLOAD_FAILED;
		} catch (IOException e) {
			Log.e(getClass().getSimpleName(), "Error while downloding file", e);
			return FTResult.DOWNLOAD_FAILED;
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {
				Log.e(getClass().getSimpleName(), "Error while closing file", e);
				return FTResult.DOWNLOAD_FAILED;
			}
		}
		return FTResult.SUCCESS;
	}

	@Override
	protected void onPostExecute(FTResult result) {
		HikeMessengerApp.stickerTaskMap.remove(key);
		if (result != FTResult.SUCCESS) {
			(new File(largeStickerPath)).delete();
			return;
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_DOWNLOADED,
				null);
	}
}
