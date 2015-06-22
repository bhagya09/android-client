package com.bsb.hike.offline.runnables;

import static com.bsb.hike.offline.OfflineConstants.PORT_FILE_TRANSFER;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Environment;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.offline.FileTransferModel;
import com.bsb.hike.offline.IConnectCallback;
import com.bsb.hike.offline.OfflineException;
import com.bsb.hike.offline.OfflineManager;
import com.bsb.hike.offline.OfflineThreadManager;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.offline.TransferProgress;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * 
 * @author himanshu, deepak malik ,sahil 
 *	Runnable responsible for receving file from client
 */
public class FileConnectRunnable implements Runnable
{

	private static final String TAG = "OfflineThreadManager";

	private ServerSocket fileServerSocket = null;

	private Socket fileReceiveSocket = null;

	OfflineManager offlineManager = null;

	IConnectCallback connectCallback = null;

	File tempFile = null;

	FileTransferModel fileTransferModel = null;

	private JSONObject fileJSON = null;

	private JSONObject message = null;

	private String filePath = "";

	private long mappedMsgId = -1;

	private String fileName = "";

	private int fileSize = 0;

	private int totalChunks = 0;

	private ConvMessage convMessage = null;

	public FileConnectRunnable(IConnectCallback connectCallback)
	{
		this.connectCallback = connectCallback;
	}

	@Override
	public void run()
	{
		try
		{
			offlineManager = OfflineManager.getInstance();

			InputStream inputstream = connectAndGetInputStream();
			connectCallback.onConnect();
			OfflineThreadManager.getInstance().startFileTransferThread(new FileSendRunnable(connectCallback, fileReceiveSocket.getOutputStream()));
			FileReceiveRunnable runnable = new FileReceiveRunnable(inputstream, connectCallback);
			runnable.run();
		}
		catch(IOException e)
		{
			connectCallback.onDisconnect(new OfflineException(e));
		}
		
	}

	private InputStream connectAndGetInputStream() throws IOException
	{
		Logger.d(TAG, "Going to wait for fileReceive socket");
		fileServerSocket = new ServerSocket();
		fileServerSocket.setReuseAddress(true);
		SocketAddress addr = new InetSocketAddress(PORT_FILE_TRANSFER);
		fileServerSocket.bind(addr);
		Logger.d(TAG, "Going to wait for fileReceive socket");
		fileReceiveSocket = fileServerSocket.accept();
		Logger.d(TAG, "fileReceive socket connection success");
		return fileReceiveSocket.getInputStream();
	}

	public void shutDown()
	{
		try
		{
			OfflineUtils.closeSocket(fileReceiveSocket);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		try
		{
			OfflineUtils.closeSocket(fileServerSocket);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}
