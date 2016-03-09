package com.bsb.hike.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSSLUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.customClasses.AsyncTask.MyAsyncTask;

public abstract class FetchLastSeenBase extends MyAsyncTask<Void, Void, Boolean>
{
	public abstract boolean saveResult(JSONObject response) throws JSONException;

	public JSONObject sendRequest(String urlString) throws IOException
	{
		URL url = new URL(urlString);

		Logger.d(getClass().getSimpleName(), "URL:  " + url);

		URLConnection connection = url.openConnection();
		connection.setConnectTimeout(HikeConstants.CONNECT_TIMEOUT);
		connection.setReadTimeout(HikeConstants.SOCKET_TIMEOUT);

		Logger.d(getClass().getSimpleName(), "opened connection " + url);
		AccountUtils.addUserAgent(connection);
		AccountUtils.setNoTransform(connection);
		connection.addRequestProperty("Cookie", "user=" + AccountUtils.mToken + "; UID=" + AccountUtils.mUid);

		if (AccountUtils.ssl)
		{
			((HttpsURLConnection) connection).setSSLSocketFactory(HikeSSLUtil.getSSLSocketFactory());
		}

		Logger.d(getClass().getSimpleName(), "gettting is");
		InputStream is = connection.getInputStream();
		Logger.d(getClass().getSimpleName(), "got is");
		JSONObject response = AccountUtils.getResponse(is);
		Logger.d(getClass().getSimpleName(), "Response: " + response);
		return response;
	}
}
