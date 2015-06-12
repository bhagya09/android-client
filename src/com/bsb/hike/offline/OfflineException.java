package com.bsb.hike.offline;

/**
 * 
 * @author himanshu This class deals with all types of exception that are related with Offline Messaging
 */
public class OfflineException extends Exception
{

	private static final long serialVersionUID = 100L;

	public static final short GHOST_PACKET_NOT_RECEIVED = 0x01;

	public static final short CONNECTION_TIME_OUT = 0x02;

	public static final short CLIENT_DISCONNETED = 0x03;

	public static final short SERVER_DISCONNED = 0x04;

	public static final short REASON_UNKNOW = 0x05;
	
	public static final short SOCKET_TIMEOUT_EXCEPTION = 0x06;

	public static final short DISCONNECT = 0x07;

	private short reasonCode;

	Exception exception;

	public OfflineException(Exception e, short reasonCode)
	{
		super();
		this.exception = e;
		this.setReasonCode(reasonCode);
	}

	public OfflineException(Exception e)
	{
		super();
		this.exception = e;
		setReasonCode(REASON_UNKNOW);
	}

	public OfflineException(short reasonCode)
	{
		super();
		setReasonCode(reasonCode);
	}
	/**
	 * @return the reasonCode
	 */
	public short getReasonCode()
	{
		return reasonCode;
	}

	/**
	 * @param reasonCode
	 *            the reasonCode to set
	 */
	public void setReasonCode(short reasonCode)
	{
		this.reasonCode = reasonCode;
	}

}
