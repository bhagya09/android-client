package com.hike.transporter;

public class TException extends Exception
{

	private static final long serialVersionUID = 100L;

	public static byte EXCEPTION_READ_FILE = 1;

	public static byte REASON_UNKNOWN = 2;

	public static byte IO_EXCEPTION = 3;

	public static byte INTERRUPTED_EXCEPTION = 4;

	public static byte JSON_EXCEPTION = 5;

	public static byte SOCKET_TIMEOUT = 6;

	public static byte IOEXCEPTION = 7;

	public static byte USER_DISCONNECTED = 8;

	public static byte HEARTBEAT_TIMEOUT = 9;

	public static byte CLIENT_TIMEOUT = 10;

	public static byte FILE_NOT_FOUND_EXCEPTION = 11;

	public static byte SERVER_EXCEPTION = 12;

	byte reasonCode;

	Exception exception;

	public TException(Exception e, byte reasonCode)
	{
		super();
		this.exception = e;
		this.setReasonCode(reasonCode);
	}

	public TException(Exception e)
	{
		super();
		this.exception = e;
		setReasonCode(REASON_UNKNOWN);
	}

	public TException(byte reasonCode)
	{
		super();
		setReasonCode(reasonCode);
	}

	public void setReasonCode(byte reasonCode)
	{
		this.reasonCode = reasonCode;
	}

	public int getReasonCode()
	{
		return reasonCode;
	}

}
