package com.bsb.hike.modules.httpmgr.request;

import static com.bsb.hike.modules.httpmgr.request.PriorityConstants.*;
import static com.bsb.hike.modules.httpmgr.request.RequestConstants.GET;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import org.json.JSONObject;

import com.bsb.hike.filetransfer.FileSavedState;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.gcmnetworkmanager.Config;
import com.bsb.hike.modules.gcmnetworkmanager.HikeGcmNetworkMgr;
import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.HttpUtils;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.client.IClient;
import com.bsb.hike.modules.httpmgr.engine.ProgressByteProcessor;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.interceptor.IRequestInterceptor;
import com.bsb.hike.modules.httpmgr.interceptor.IResponseInterceptor;
import com.bsb.hike.modules.httpmgr.interceptor.Pipeline;
import com.bsb.hike.modules.httpmgr.log.LogFull;
import com.bsb.hike.modules.httpmgr.request.facade.IRequestFacade;
import com.bsb.hike.modules.httpmgr.request.listener.IProgressListener;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestCancellationListener;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.StringBody;
import com.bsb.hike.modules.httpmgr.requeststate.HttpRequestState;
import com.bsb.hike.modules.httpmgr.requeststate.HttpRequestStateDB;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.httpmgr.retry.BasicRetryPolicy;

import android.text.TextUtils;

/**
 * Encapsulates all of the information necessary to make an HTTP request.
 */
public abstract class Request<T> implements IRequestFacade
{
	public static final short REQUEST_TYPE_LONG = 0x0;

	public static final short REQUEST_TYPE_SHORT = 0x1;

	public static final int BUFFER_SIZE = 4 * 1024; // 4Kb

	private String defaultId = "";

	private String md5Id;

	private String analyticsParam;

	private String method;

	private URL url;

	private List<Header> headers;

	private IRequestBody body;

	private int priority;

	private short requestType;

	private BasicRetryPolicy retryPolicy;

	private volatile boolean isCancelled;

	private volatile boolean isFinished;

	private volatile boolean wrongRequest;

	private short wrongRequestErrorCode;

	private Pipeline<IRequestInterceptor> requestInteceptors;

	private Pipeline<IResponseInterceptor> responseInteceptors;

	private CopyOnWriteArrayList<IRequestListener> requestListeners;

	private IRequestCancellationListener requestCancellationListener;

	private IProgressListener progressListener;

	private boolean responseOnUIThread;

	private boolean asynchronous;

	private Future<?> future;

	private volatile FileSavedState state = null;

	protected int chunkSize;

	private Config gcmTaskConfig;

	protected Request(Init<?> builder)
	{
		this.defaultId = builder.id;
		this.analyticsParam = builder.analyticsParam;
		this.method = builder.method;
		this.url = builder.url;
		this.headers = builder.headers;
		this.body = builder.body;
		this.priority = builder.priority;
		this.requestType = builder.requestType;
		this.retryPolicy = builder.retryPolicy;
		addRequestListeners(builder.requestListeners);
		this.responseOnUIThread = builder.responseOnUIThread;
		this.asynchronous = builder.asynchronous;
		this.gcmTaskConfig = builder.gcmTaskConfig;
		ensureSaneDefaults();
		setHostUris();
	}

	private void ensureSaneDefaults()
	{
		if (TextUtils.isEmpty(method))
		{
			method = GET;
		}

		if (null == headers)
		{
			headers = new ArrayList<Header>();
		}

		if (priority > PRIORITY_LOW || priority < PRIORITY_HIGH)
		{
			priority = PRIORITY_NORMAL;
		}

		md5Id = generateId();

		if (requestInteceptors == null)
		{
			requestInteceptors = new Pipeline<IRequestInterceptor>();
		}

		if (responseInteceptors == null)
		{
			responseInteceptors = new Pipeline<IResponseInterceptor>();
		}

		if (url == null)
		{
			setWrongRequest(true);
			setWrongRequestErrorCode(HttpException.REASON_CODE_WRONG_URL);
			return;
		}

		if (gcmTaskConfig != null && !asynchronous)
		{
			setWrongRequest(true);
			setWrongRequestErrorCode(HttpException.REASON_CODE_CAN_NOT_USE_GCM_TASK_FOR_SYNC_CALLS);
			return;
		}
	}

	private void setHostUris()
	{
		if(retryPolicy != null && url != null)
		{
			this.retryPolicy.setHostUris(url);
		}
	}

	public void finish()
	{
		this.isFinished = true;
		this.method = null;
		this.defaultId = null;
		this.md5Id = null;
		this.url = null;
		this.headers = null;
		this.body = null;
		this.retryPolicy = null;
		this.requestListeners = null;
		this.requestInteceptors = null;
		this.responseInteceptors = null;
		this.requestCancellationListener = null;
		this.progressListener = null;
		this.future = null;
	}

	public Response executeRequest(IClient client) throws Throwable {
		return client.execute(this);
	}

	public abstract T parseResponse(InputStream in, int contentLength) throws IOException;

	protected void readBytes(InputStream is, ProgressByteProcessor progressByteProcessor) throws IOException
	{
		readBytes(is, progressByteProcessor, 0);
	}

	protected void readBytes(InputStream is, ProgressByteProcessor progressByteProcessor, int offset) throws IOException
	{
		final byte[] buffer = new byte[BUFFER_SIZE];
		int len = 0;
		FTState st = state == null ? FTState.NOT_STARTED : state.getFTState();
		while ((len = is.read(buffer)) != -1 && st != FTState.PAUSED)
		{
			progressByteProcessor.processBytes(buffer, offset, len);
		}
	}

	public FileSavedState getState()
	{
		if (state == null)
		{
			state = getStateFromDB();
		}
		return state;
	}

	protected FileSavedState getStateFromDB()
	{
		FileSavedState fss;
		HttpRequestState st = HttpRequestStateDB.getInstance().getRequestState(this.getId());
		if (st == null)
		{
			fss = new FileSavedState(FTState.INITIALIZED, 0, 0, 0);
		}
		else
		{
			LogFull.d("getting state from db");
			JSONObject md = st.getMetadata();
			fss = FileSavedState.getFileSavedStateFromJSON(md);
			LogFull.d("getting state from db file upload request ft state : "+fss.getFTState().name());
		}
		return fss;
	}

	public int getChunkSize()
	{
		return chunkSize;
	}

	public void deleteState()
	{
		HttpRequestStateDB.getInstance().deleteState(this.getId());
	}

	protected void saveStateInDB(FileSavedState fss)
	{
		HttpRequestState state = new HttpRequestState(this.getId());
		state.setMetadata(fss.toJSON());
		HttpRequestStateDB.getInstance().insertOrReplaceRequestState(state);
	}

	public void setState(FileSavedState state) {
		this.state = state;
	}

	/**
	 * Returns the unique id of the request
	 *
	 * @return
	 */
	public String getId()
	{
		return md5Id;
	}

	public String getCustomId()
	{
		return defaultId;
	}

	/**
	 * Returns the analytics key for this request
	 * 
	 * @return
	 */
	public String getAnalyticsParam()
	{
		return analyticsParam;
	}
	
	/**
	 * Returns the method (GET / POST etc) of the request
	 * 
	 * @return
	 */
	public String getMethod()
	{
		return method;
	}

	/**
	 * Returns the target url of the request
	 * 
	 * @return
	 */
	public URL getUrl()
	{
		return url;
	}

	/**
	 * Returns a list of headers of the request
	 * 
	 * @return
	 */
	@Override
	public List<Header> getHeaders()
	{
		return headers;
	}

	/**
	 * Returns the request body
	 * 
	 * @return
	 */
	@Override
	public IRequestBody getBody()
	{
		return body;
	}

	/**
	 * Returns the priority of the request
	 * 
	 * @return
	 */
	public int getPriority()
	{
		return priority;
	}

	/**
	 * Returns the request type of the request
	 * 
	 * @return
	 */
	public short getRequestType()
	{
		return requestType;
	}

	public boolean isCancelled()
	{
		return isCancelled;
	}

	/**
	 * Returns the object of class implementing {@link IRetryPolicy} which is used to schedule retries of the request in case of failure
	 * 
	 * @return
	 */
	public BasicRetryPolicy getRetryPolicy()
	{
		return retryPolicy;
	}

	/**
	 * Returns the {@link IRequestListener} object , request listener
	 * 
	 * @return
	 */
	public CopyOnWriteArrayList<IRequestListener> getRequestListeners()
	{
		return requestListeners;
	}

	public Pipeline<IRequestInterceptor> getRequestInterceptors()
	{
		return requestInteceptors;
	}

	public Pipeline<IResponseInterceptor> getResponseInterceptors()
	{
		return responseInteceptors;
	}
	
	/**
	 * Returns the {@link IRequestCancellationListener} object used when request is cancelled
	 * 
	 * @return
	 */
	public IRequestCancellationListener getRequestCancellationListener()
	{
		return requestCancellationListener;
	}

	/**
	 * Returns the {@link IProgressListener} object , used to update the request progress
	 * 
	 * @return
	 */
	public IProgressListener getProgressListener()
	{
		return progressListener;
	}

	/**
	 * Returns a boolean representing whether this request should run on ui thread or not
	 * 
	 * @return
	 */
	public boolean isResponseOnUIThread()
	{
		return responseOnUIThread;
	}

	/**
	 * Returns true if the request to be executed asynchronously (Non blocking call) otherwise false
	 * 
	 * @return
	 */
	public boolean isAsynchronous()
	{
		return asynchronous;
	}

	public Config getGcmTaskConfig()
	{
		return gcmTaskConfig;
	}

	public void setGcmTaskConfig(Config gcmTaskConfig)
	{
		this.gcmTaskConfig = gcmTaskConfig;
	}

	/**
	 * Returns the future of the request that is submitted to the executor
	 * 
	 * @return
	 */
	public Future<?> getFuture()
	{
		return future;
	}

	public void setId(String id)
	{
		this.md5Id = id;
	}

	public void setUrl(URL url)
	{
		this.url = url;
	}
	
	/**
	 * Sets the headers of the request
	 * 
	 * @param headers
	 */
	public void setHeaders(List<Header> headers)
	{
		if (null == headers)
		{
			headers = new ArrayList<Header>();
		}
		this.headers = headers;
	}

	public void addHeader(String name,String value)
	{
		if (TextUtils.isEmpty(name) || TextUtils.isEmpty(value))
		{
			return;
		}
		Header header = new Header(name, value);
		this.headers.add(header);
	}

	public void replaceOrAddHeader(String name, String value)
	{
		if (TextUtils.isEmpty(name) || TextUtils.isEmpty(value))
		{
			return;
		}

		boolean exists = false;
		for (Header header : headers)
		{
			if (header.getName().equals(name))
			{
				header.setValue(value);
				exists = true;
				break;
			}
		}

		if (!exists)
		{
			Header header = new Header(name, value);
			this.headers.add(header);
		}
	}

	@Override
	/**
	 * Adds more headers to the list of headers of the request
	 * 
	 * @param headers
	 */
	public void addHeaders(List<Header> headers)
	{
		if (null == headers)
		{
			return;
		}

		if (null == this.headers)
		{
			this.headers = headers;
		}
		else
		{
			this.headers.addAll(headers);
		}
	}

	/**
	 * Sets the body of the request
	 * 
	 * @param body
	 */
	public void setBody(IRequestBody body)
	{
		this.body = body;
	}

	/**
	 * Sets the priority of the request. Use priority constants or a positive integer. Will have no effect on a request after it starts being executed.
	 * 
	 * @param priority
	 *            the priority of request. Defaults to {@link #PRIORITY_NORMAL}.
	 * @throws IllegalArgumentException
	 *             if priority is not between 1 to 100 inclusive
	 * @see PriorityConstants#PRIORITY_LOW
	 * @see PriorityConstants#PRIORITY_NORMAL
	 * @see PriorityConstants#PRIORITY_HIGH
	 * 
	 */
	public void setPriority(int priority)
	{
		if (priority > PRIORITY_LOW || priority < PRIORITY_HIGH)
		{
			throw new IllegalArgumentException("Priority can be between " + PRIORITY_LOW + " to " + PRIORITY_HIGH);
		}
		this.priority = priority;
	}

	/**
	 * Sets the request type. Use request types constants
	 * 
	 * @param requestType
	 *            the request type of the request. Defaults to {@link #REQUEST_TYPE_LONG}
	 * @see #REQUEST_TYPE_LONG
	 * @see #REQUEST_TYPE_SHORT
	 */
	public void setRequestType(short requestType)
	{
		this.requestType = requestType;
	}

	/**
	 * Sets the cancelled boolean to true when request is cancelled
	 * 
	 * @param isCancelled
	 */
	public void setCancelled(boolean isCancelled)
	{
		this.isCancelled = isCancelled;
	}

	public void addRequestListeners(IRequestListener requestListener)
	{
		if (this.requestListeners == null)
		{
			this.requestListeners = new CopyOnWriteArrayList<IRequestListener>();
		}
		this.requestListeners.add(requestListener);
	}
	
	/**
	 * add list of listeners to existing list of request listeners
	 * @param requestListeners
	 */
	public void addRequestListeners(List<IRequestListener> requestListeners)
	{
		if (this.requestListeners == null)
		{
			this.requestListeners = new CopyOnWriteArrayList<IRequestListener>();
		}
		this.requestListeners.addAll(requestListeners);
	}
	
	/**
	 * remove list of listeners from existing list of request listeners
	 * @param requestListeners
	 */
	public void removeRequestListeners(List<IRequestListener> requestListeners)
	{
		if (this.requestListeners == null)
		{
			return;
		}
		this.requestListeners.removeAll(requestListeners);
	}

	/**
	 * Sets the request cancellation listener {@link IRequestCancellationListener}
	 * 
	 * @param requestCancellationListener
	 */
	public void setRequestCancellationListener(IRequestCancellationListener requestCancellationListener)
	{
		this.requestCancellationListener = requestCancellationListener;
	}

	/**
	 * Sets the progress listener of the request {@link IProgressListener}
	 * 
	 * @param progressListener
	 */
	public void setProgressListener(IProgressListener progressListener)
	{
		this.progressListener = progressListener;
	}

	/**
	 * Sets the future of the runnable submitted to the executor
	 * 
	 * @param future
	 */
	public void setFuture(Future<?> future)
	{
		this.future = future;
	}

	/**
	 * Sets the wrongRequest boolean to true when request is wrong
	 *
	 * @param wrongRequest
	 */
	public void setWrongRequest(boolean wrongRequest) {
		this.wrongRequest = wrongRequest;
	}

	/**
	 * return wrongRequest boolean which denotes whether request is wrong or not
	 * @return wrongRequest
	 */
	public boolean isWrongRequest() {
		return wrongRequest;
	}

	/**
	 * Sets the wrongRequestErrorCode when request is wrong
	 * @param wrongRequestErrorCode
	 */
	public void setWrongRequestErrorCode(short wrongRequestErrorCode) {
		this.wrongRequestErrorCode = wrongRequestErrorCode;
	}

	/**
	 * Gets the wrongRequestErrorCode when request is wrong
	 * @return
	 */
	public short getWrongRequestErrorCode() {
		return wrongRequestErrorCode;
	}

	/**
	 * Sets isCancelled field to true and cancels the future of this request that has been submitted to the executor
	 */
	public void cancel()
	{
		if (this.isFinished || this.isCancelled)
		{
			return;
		}
		this.isCancelled = true;

		HikeGcmNetworkMgr.getInstance().cancelTask(gcmTaskConfig);

		if (future != null)
		{
			future.cancel(true);
		}

		if (this.requestCancellationListener != null)
		{
			this.requestCancellationListener.onCancel();
		}
	}

	/**
	 * Used to update the progress of the request
	 * 
	 * @param f
	 */
	public void publishProgress(float f)
	{
		if (this.progressListener != null)
		{
			this.progressListener.onProgressUpdate(f);
		}
	}
	
	@Override
	public String toString()
	{
		String s = "\nRequest{\n" + "\t\turl : " + getUrl() + "\n" + "\t\tid : " + getId() + "\n" + "}";
		return s;
	}

	protected static abstract class Init<S extends Init<S>>
	{
		private String id;

		private String analyticsParam;
		
		private String method;

		private URL url;

		private List<Header> headers;

		private IRequestBody body;

		private int priority = PRIORITY_NORMAL;

		private short requestType = REQUEST_TYPE_LONG;

		private BasicRetryPolicy retryPolicy = new BasicRetryPolicy();

		private IRequestListener requestListeners;

		private boolean responseOnUIThread;

		private boolean asynchronous = true;

		private Config gcmTaskConfig;

		protected abstract S self();

		/**
		 * Sets the unique id of the request
		 * 
		 * @param id
		 */
		public S setId(String id)
		{
			this.id = id;
			return self();
		}

		/**
		 * Sets the analytics key for this request
		 * 
		 * @param key
		 * @return
		 */
		public S setAnalyticsParam(String analyticsParam)
		{
			this.analyticsParam = analyticsParam;
			return self();
		}
		
		/**
		 * Sets the method type to {@see RequestConstants#GET} and body null
		 * 
		 * @return
		 */
		public S get()
		{
			this.method = RequestConstants.GET;
			this.body = null;
			return self();
		}

		/**
		 * Sets the method type to {@see RequestConstants#HEAD} and body null
		 * 
		 * @return
		 */
		public S head()
		{
			this.method = RequestConstants.HEAD;
			this.body = null;
			return self();
		}

		/**
		 * Sets the method type to {@see RequestConstants#POST} and body passed as a parameter
		 * 
		 * @return
		 */
		public S post(IRequestBody body)
		{
			this.method = RequestConstants.POST;
			this.body = body != null ? body : new StringBody("");
			return self();
		}

		public S put(IRequestBody body)
		{
			this.method = RequestConstants.PUT;
			this.body = body;
			return self();
		}

		public S delete()
		{
			return delete(null);
		}
		
		public S delete(IRequestBody body)
		{
			this.method = RequestConstants.DELETE;
			this.body = body;
			return self();
		}

		public S patch(IRequestBody body)
		{
			this.method = RequestConstants.PATCH;
			this.body = body;
			return self();
		}

		/**
		 * Sets the url of the request
		 * 
		 * @param url
		 * @return
		 */
		public S setUrl(String url)
		{
			try
			{
				this.url = new URL(url);
			}
			catch (MalformedURLException ex)
			{
				LogFull.e("exception while setting url ", ex);
			}
			return self();
		}

		/**
		 * Sets the url of the request
		 * 
		 * @param url
		 * @return
		 */
		public S setUrl(URL url)
		{
			this.url = url;
			return self();
		}

		/**
		 * Sets the headers of the request
		 * 
		 * @param headers
		 */
		public S setHeaders(List<Header> headers)
		{
			this.headers = headers;
			return self();
		}

		/**
		 * Adds a header to the list of request headers
		 * 
		 * @param header
		 * @return
		 */
		public S addHeader(Header header)
		{
			if (null == this.headers)
			{
				this.headers = new ArrayList<Header>();
			}
			this.headers.add(header);
			return self();
		}

		/**
		 * Adds a list of headers to request headers
		 * 
		 * @param headers
		 * @return
		 */
		public S addHeader(List<Header> headers)
		{
			if (null == this.headers)
			{
				this.headers = new ArrayList<Header>();
			}
			this.headers.addAll(headers);
			return self();
		}
		
		/**
		 * Sets the priority of the request. Use priority constants or a positive integer. Will have no effect on a request after it starts being executed.
		 * 
		 * @param priority
		 *            the priority of request. Defaults to {@link #PRIORITY_NORMAL}.
		 * @see #PRIORITY_LOW
		 * @see #PRIORITY_NORMAL
		 * @see #PRIORITY_HIGH
		 */
		public S setPriority(int priority)
		{
			this.priority = priority;
			return self();
		}

		/**
		 * Sets the request type. Use request types constants
		 * 
		 * @param requestType
		 *            the request type of the request. Defaults to {@link #REQUEST_TYPE_LONG}
		 * @see #REQUEST_TYPE_LONG
		 * @see #REQUEST_TYPE_SHORT
		 */
		public S setRequestType(short requestType)
		{
			this.requestType = requestType;
			return self();
		}

		/**
		 * Set a {@link IRetryPolicy} that will be responsible to coordinate retry attempts by the RequestRunner. Can be null (no retry).
		 * 
		 * @param retryPolicy
		 *            the new retry policy
		 * @see
		 */
		public S setRetryPolicy(BasicRetryPolicy retryPolicy)
		{
			this.retryPolicy = retryPolicy;
			return self();
		}

		/**
		 * Sets the request listener {@link IRequestListener}
		 * 
		 * @param requestListener
		 */
		public S setRequestListener(IRequestListener requestListener)
		{
			this.requestListeners = requestListener;
			return self();
		}

		/**
		 * Sets the boolean whether request should be eun on ui thread or not
		 * 
		 * @param runOnUIThread
		 */
		public S setResponseOnUIThread(boolean responseOnUIThread)
		{
			this.responseOnUIThread = responseOnUIThread;
			return self();
		}

		/**
		 * Sets the asynchronous field , pass true if this request should be executed asynchronously (non blocking)
		 * 
		 * @param async
		 * @return
		 */
		public S setAsynchronous(boolean async)
		{
			this.asynchronous = async;
			return self();
		}

		/**
		 * Sets the properties of request which will be used for scheduling the task {@link com.google.android.gms.gcm.OneoffTask} in Gcm network manager
		 *
		 * @param config
		 * @return
		 */
		public S setGcmTaskConfig(Config config)
		{
			this.gcmTaskConfig = config;
			return self();
		}

		/**
		 * Returns an object of {@link RequestToken} which allows outside world to only have limited access to request class so that users can not update request after being
		 * submitted to the executor
		 * 
		 * @return
		 */
		public abstract RequestToken build();
	}

	public String generateId()
	{
		String input = url + defaultId;
		return HttpUtils.calculateMD5hash(input);
	}
	
	@Override
	public int hashCode()
	{
		int urlHashCode = url.hashCode();
		int headersHashCode = 0;
		for (Header header : headers)
		{
			headersHashCode += header.hashCode();
		}
		return (31 * urlHashCode) + headersHashCode;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}

		if (!(other instanceof Request))
		{
			return false;
		}

		Request<T> req = (Request<T>) other;
		if (this.getId() != req.getId())
		{
			return false;
		}
		return true;
	}
}
