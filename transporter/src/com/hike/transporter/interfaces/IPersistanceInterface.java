package com.hike.transporter.interfaces;

import java.util.List;

import com.hike.transporter.models.SenderConsignment;

/**
 * 
 * @author himanshu
 * 
 * This interface is implemented by our DB
 *	
 */
public interface IPersistanceInterface
{
	public void addToPersistance(String nameSpace, String message, long awb);
	
	public void deleteFromPersistance(long awb);
	
	public void deleteFromPersistance(String nameSpace);
	
	public void deleteAll();
	
	public List<SenderConsignment> getAllPendingMsgs(String nameSpace);
	
	public void deleteFromPersistance(List<Long> listAwbNumber);
	
	
}
