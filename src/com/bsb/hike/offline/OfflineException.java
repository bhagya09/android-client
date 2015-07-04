package com.bsb.hike.offline;

import com.hike.transporter.TException;

/**
 * 
 * @author himanshu This class deals with all types of exception that are related with Offline Messaging
 */
public class OfflineException extends TException
{
	
	public static byte WIFI_CLOSED = 20;
	
	public static byte HOTSPOT_CLOSED = 21;
	
	public static byte CONNECTION_TIME_OUT = 22;
	
	public static byte GHOST_PACKET_NOT_RECEIVED = 23;

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
