package com.hike.transporter.interfaces;

import java.net.Socket;

import com.hike.transporter.TException;

/**
 * 
 * @author himanshu
 *	This Interface deals with the connection state of the client
 */
public interface IConnectionConnect
{
	public void onConnectionMade(Socket soc);

	public void onConnectionFailure(TException e);
}
