package com.hike.transporter.interfaces;

import com.hike.transporter.TException;

/**
 * 
 * @author himanshu
 *	This interface is exposed to the user and receives callback on various senarious
 */
public interface IConsignerListener
{
	public void onTransitBegin(long awb);

	public void onTransitEnd(long awb);

	public void onChunkSend(long awb, int FileSize);

	public void onErrorOccuredConsigner(TException e,long awb);
}
