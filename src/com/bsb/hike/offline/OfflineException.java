package com.bsb.hike.offline;

import com.hike.transporter.TException;

/**
 * 
 * @author himanshu This class deals with all types of exception that are related with Offline Messaging
 */
public class OfflineException extends TException
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4437517770214959888L;

	public static byte WIFI_CLOSED = 20;
	
	public static byte HOTSPOT_CLOSED = 21;
	
	public static byte CONNECTION_TIME_OUT = 22;
	
	public static byte GHOST_PACKET_NOT_RECEIVED = 23;
	
	public static byte AP_IP_NOT_AVAILABLE = 24;

	public static byte CONNECTION_CANCELLED = 25;
	
	public static byte CANCEL_NOTIFICATION_REQUEST = 26;
	
	public static byte WIFI_COULD_NOT_START = 27;
	
	public static byte APP_SWIPE = 28;
	
	public static byte PEER_DISCONNECTED = 29;
	
	public OfflineException(byte reasonCode) {
		super(reasonCode);
	}

	public OfflineException(Exception e, byte reasonCode) {
		super(e, reasonCode);
	}

	public OfflineException(Exception e) {
		super(e);
	}
}
