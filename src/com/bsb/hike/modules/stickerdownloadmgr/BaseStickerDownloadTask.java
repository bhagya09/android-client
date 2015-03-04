package com.bsb.hike.modules.stickerdownloadmgr;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.STResult;
import com.bsb.hike.http.GzipByteArrayEntity;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.HttpRequestType;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.STState;
import com.bsb.hike.modules.stickerdownloadmgr.retry.DefaultRetryPolicy;
import com.bsb.hike.modules.stickerdownloadmgr.retry.IRetryPolicy;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSSLUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

abstract class BaseStickerDownloadTask implements Callable<STResult>
{

	private IRetryPolicy retryPolicy;

	private String downloadUrl;

	private Handler handler;

	private Context context;

	private String taskId;

	private STState _state;

	private URL mUrl;

	private StickerException exception;

	private Object resultObj;

	private IStickerResultListener callback;

	protected BaseStickerDownloadTask(Handler handler, Context ctx, String taskId, IStickerResultListener callback)
	{
		this.handler = handler;
		context = ctx;
		this.taskId = taskId;
		this.callback = callback;
		this.retryPolicy = new DefaultRetryPolicy(taskId);
	}

	public String getTaskId()
	{
		return taskId;
	}

	public void setTaskId(String taskId)
	{
		this.taskId = taskId;
	}

	public String getDownloadUrl()
	{
		return downloadUrl;
	}

	public void setDownloadUrl(String downloadUrl)
	{
		this.downloadUrl = downloadUrl;
	}

	protected URLConnection initConn() throws IOException
	{
		mUrl = new URL(downloadUrl);
		URLConnection conn = (HttpURLConnection) mUrl.openConnection();
		conn.addRequestProperty("Cookie", "user=" + AccountUtils.mToken + "; UID=" + AccountUtils.mUid);
		if (AccountUtils.ssl)
		{
			((HttpsURLConnection) conn).setSSLSocketFactory(HikeSSLUtil.getSSLSocketFactory());
		}
		conn.setConnectTimeout(HikeConstants.CONNECT_TIMEOUT);
		conn.setReadTimeout(HikeConstants.SOCKET_TIMEOUT);
		AccountUtils.addUserAgent(conn);
		AccountUtils.setNoTransform(conn);
		return conn;
	}

	protected HttpClient initConnHead() throws IOException
	{
		mUrl = new URL(downloadUrl);
		HttpClient client = new DefaultHttpClient();
		client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, HikeConstants.CONNECT_TIMEOUT);
		client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, HikeConstants.SOCKET_TIMEOUT);
		return client;
	}

	protected Object download(JSONObject requestEntity, HttpRequestType requestType) throws Exception
	{
		while(true)
		{
			try
			{
				/**
				 * Return in case of no connectivity
				 */
				if(!StickerManager.getInstance().isMinimumMemoryAvailable())
				{
					throw new StickerException(StickerException.OUT_OF_SPACE);
				}
				if(!Utils.isUserOnline(context))
				{
					throw new StickerException(StickerException.NO_NETWORK);
				}
				
				if (HttpRequestType.POST == requestType)
				{
					GzipByteArrayEntity entity;
					entity = new GzipByteArrayEntity(requestEntity.toString().getBytes(), HTTP.DEFAULT_CONTENT_CHARSET);
					HttpPost httpPost = new HttpPost(downloadUrl);
					AccountUtils.addToken(httpPost);
					httpPost.setEntity(entity);
					HttpClient client = AccountUtils.getClient(httpPost);

					HttpResponse response;
					response = client.execute(httpPost);
					Logger.d("HTTP", "finished request");
					if (response.getStatusLine().getStatusCode() != 200)
					{
						Logger.w("HTTP", "Request Failed: " + response.getStatusLine());
						return null;
					}

					HttpEntity responseEntity = response.getEntity();
					return AccountUtils.getResponse(responseEntity.getContent());
				}
				else if (HttpRequestType.HEAD == requestType)
				{
					DefaultHttpClient client = (DefaultHttpClient) initConnHead();
					HttpHead head = new HttpHead(mUrl.toString());
					head.addHeader("Cookie", "user=" + AccountUtils.mToken + "; UID=" + AccountUtils.mUid);
					HttpResponse resp = client.execute(head);
					return resp;
				}
				else if(HttpRequestType.GET == requestType)
				{

					URLConnection connection = initConn();
					JSONObject response = AccountUtils.getResponse(connection.getInputStream());
					return response;
				}
			}
			catch (SocketTimeoutException e)
			{
				attemptRetryOnException(e);
			}
			catch (ConnectTimeoutException e)
			{
				attemptRetryOnException(e);
			}
			catch (MalformedURLException e)
			{
				throw new RuntimeException("Bad URL " + downloadUrl, e);
			}
			catch (IOException e)
			{
				attemptRetryOnException(e);
			}
		}

	}
	
	void attemptRetryOnException(Exception e) throws Exception
	{
		try
		{
			Logger.d(StickerDownloadManager.TAG, "retrying task : " + taskId);
			retryPolicy.retry(e);
		}
		catch(Exception ex)
		{
			Logger.d(StickerDownloadManager.TAG, "downlad failed for task : " + taskId);
			throw new StickerException(ex);
		}
	}

	protected void postExecute(STResult result)
	{
		StickerDownloadManager.getInstance().removeTask(taskId);
		if (result != STResult.SUCCESS)
		{
			if (callback != null)
			{
				callback.onFailure(resultObj, getException());
			}

		}
		else
		{
			if (callback != null)
			{
				callback.onSuccess(resultObj);
			}
		}
	}

	public IRetryPolicy getRetryPolicy()
	{
		return retryPolicy;
	}

	public void setRetryPolicy(IRetryPolicy retryPolicy)
	{
		this.retryPolicy = retryPolicy;
	}

	public StickerException getException()
	{
		return exception;
	}

	public void setException(StickerException exception)
	{
		this.exception = exception;
	}

	public Object getResult()
	{
		return resultObj;
	}

	public void setResult(Object resultObject)
	{
		this.resultObj = resultObject;
	}

	public IStickerResultListener getCallback()
	{
		return callback;
	}

	public void setCallback(IStickerResultListener callback)
	{
		this.callback = callback;
	}
}
