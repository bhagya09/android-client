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

	public static final short REASON_CODE_SOCKET_EXCEPTION = 0x12;

	public static final short REASON_CODE_IO_EXCEPTION = 13;

	public static final short REASON_CODE_WRONG_URL = 14;

	public static final short REASON_CODE_REQUEST_PAUSED = 15;

    public static final short REASON_CODE_INCOMPLETE_REQUEST = 19;

    public static final short REASON_CODE_ZERO_BYTE_ZIP_DOWNLOAD = 20;

	public static final short REASON_CODE_SERVER_STATUS_FAILED = 21;

	public static final short REASON_CODE_CAN_NOT_USE_GCM_TASK_FOR_SYNC_CALLS = 22;

	/** Http custom status codes returned by server */

	/**
	 * This is returned when unzip fails at server end
	 */
	public static final int HTTP_UNZIP_FAILED = 420;

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
