package com.hike.transporter.interfaces;

import com.hike.transporter.TException;

/**
 * 
 * @author himanshu
 *	This interface is exposed to the user and called when the connection is succussful or any error occur
 */
public interface IConnectionListener
{
	public void onConnect();

	public void onDisconnect(TException e);
}
