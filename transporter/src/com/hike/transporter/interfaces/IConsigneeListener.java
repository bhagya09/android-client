package com.hike.transporter.interfaces;

import java.net.Socket;

import org.json.JSONObject;

import com.hike.transporter.TException;
import com.hike.transporter.TransporterRunnable;
import com.hike.transporter.models.ReceiverConsignment;

/**
 * 
 * @author himanshu
 *	This interfaces forms a bridge b/w consigness and the callbackHandler and manages all types of packets
 */
public interface IConsigneeListener
{
	public void onHandShake(TransporterRunnable runnable, JSONObject json, Socket socket);

	public void onAckReceived(long awb);

	public void onHeartBeat(boolean screenOn);

	public void onApplicationData(ReceiverConsignment receiverConsignment);

	public void onChunkRead(ReceiverConsignment receiverConsignment);

	public void onFileCompleted(ReceiverConsignment receiverConsignment);

	public void onErrorOccuredConsignee(TException ex);
	
	public void onFileRequest(long awb, int fileSizeBytes);
	
	public void onFileRequestReply(long awb, int code);
	
	public void onHandShakeFromServer(boolean status);
	
}
