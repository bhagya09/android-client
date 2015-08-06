package com.bsb.hike.modules.httpmgr.exception;

/**
 * Super class of all exceptions in HttpManager
 * 
 * @author sni
 */
public class HttpException extends Exception
{
	private static final long serialVersionUID = 647946546917050370L;
	
	public static final short REASON_CODE_UNEXPECTED_ERROR = 0x0;
	
	public static final short REASON_CODE_NO_NETWORK = 0x1;
	
	public static final short REASON_CODE_SOCKET_TIMEOUT = 0x2;
	
	public static final short REASON_CODE_CONNECTION_TIMEOUT = 0x3;
	
	public static final short REASON_CODE_MALFORMED_URL = 0x4;
	
	public static final short REASON_CODE_CANCELLATION = 0x7;
	
	public static final short REASON_CODE_OUT_OF_SPACE = 0x08;
	
	public static final short REASON_CODE_RESPONSE_PARSING_ERROR = 0x09;

	public static final short REASON_CODE_UNKNOWN_HOST_EXCEPTION = 0x10;

	public static final short REASON_CODE_INTERRUPTED_EXCEPTION = 0x11;

	private int errorCode;

	public HttpException(short errorCode)
	{
		super();
		this.errorCode = errorCode;
	}

	public HttpException(String message)
	{
		this(0, message);
	}

	public HttpException(Throwable thr)
	{
		this(0, thr);
	}

	public HttpException(String errorMsg, Throwable thr)
	{
		this(0, errorMsg, thr);
	}

	public HttpException(int errorCode, String message)
	{
		super(message);
		this.errorCode = errorCode;
	}

	public HttpException(int errorCode, Throwable thr)
	{
		super(thr);
		this.errorCode = errorCode;
	}

	public HttpException(int errorCode, String errorMsg, Throwable thr)
	{
		super(errorMsg, thr);
		this.errorCode = errorCode;
	}

	public int getErrorCode()
	{
		return errorCode;
	}

}
