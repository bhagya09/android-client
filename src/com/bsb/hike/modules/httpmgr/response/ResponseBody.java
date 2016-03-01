package com.bsb.hike.modules.httpmgr.response;

/**
 * Contains the mime type , content and content length of the response body
 * 
 */
public class ResponseBody<T>
{
	private String mimeType;

	private int contentLength;

	private T content;

	private String errorString;

	private ResponseBody(String mimeType, int contentLength, T content)
	{
		this.mimeType = mimeType;
		this.contentLength = contentLength;
		this.content = content;
	}

	private ResponseBody(String mimeType, int contentLength, String errorString)
	{
		this.mimeType = mimeType;
		this.contentLength = contentLength;
		this.errorString = errorString;
	}

	public String getMimeType()
	{
		return mimeType;
	}

	public void setMimeType(String mimeType)
	{
		this.mimeType = mimeType;
	}

	public int getContentLength()
	{
		return contentLength;
	}

	public void setContentLength(int contentLength)
	{
		this.contentLength = contentLength;
	}

	public T getContent()
	{
		return content;
	}

	public void setContent(T content)
	{
		this.content = content;
	}

	public String getErrorString()
	{
		return errorString;
	}

	/**
	 * Returns the {@link ResponseBody} object using mime type and content byte array
	 * 
	 * @param mimeType
	 * @param content
	 * @return
	 */
	public static <T> ResponseBody<T> create(String mimeType, int contentLength, T content)
	{
		return new ResponseBody<T>(mimeType, contentLength, content);
	}

	public static <T> ResponseBody<T> createErrorResponse(String mimeType, int contentLength, String errorString)
	{
		return new ResponseBody<T>(mimeType, contentLength, errorString);
	}
}
