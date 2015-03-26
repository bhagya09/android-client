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

	private ResponseBody(String mimeType, int contentLength, T content)
	{
		this.mimeType = mimeType;
		this.contentLength = contentLength;
		this.content = content;
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
}
