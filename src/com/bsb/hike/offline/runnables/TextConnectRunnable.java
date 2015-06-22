package com.bsb.hike.offline.runnables;

import static com.bsb.hike.offline.OfflineConstants.PORT_TEXT_MESSAGE;

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

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.offline.IConnectCallback;
import com.bsb.hike.offline.IMessageSentOffline;
import com.bsb.hike.offline.OfflineException;
import com.bsb.hike.offline.OfflineManager;
import com.bsb.hike.offline.OfflineMessagesManager;
import com.bsb.hike.offline.OfflineThreadManager;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.service.MqttMessagesManager;
import com.bsb.hike.utils.Logger;

/**
 * 
 * @author himanshu, deepak malik Runnable responsible for receving text from client
 */
public class TextConnectRunnable implements Runnable
{

	private static final String TAG = "OfflineThreadManager";

	int type;

	int msgSize;

	InputStream inputStream = null;

	private ServerSocket textServerSocket = null;

	private Socket textReceiverSocket = null;

	private OfflineManager offlineManager;

	IConnectCallback connectCallback;

	private IMessageSentOffline textCallback;

	private IMessageSentOffline fileCallback;

	File stickerImage = null;

	private OfflineMessagesManager messagesManager = null;

	public TextConnectRunnable(IMessageSentOffline textCallback, IMessageSentOffline fileCallback, IConnectCallback connectCallback)
	{
		this.textCallback = textCallback;
		this.fileCallback = fileCallback;
		this.connectCallback = connectCallback;
	}

	@Override
	public void run()
	{
		offlineManager = OfflineManager.getInstance();
		messagesManager = new OfflineMessagesManager();
		try
		{
			inputStream = connectAndGetInputStream();
			TextReceiveRunnable runnable;
			OfflineThreadManager.getInstance().startTextTransferThread(new TextSendRunnable(textReceiverSocket.getOutputStream(), connectCallback));
			runnable = new TextReceiveRunnable(inputStream, textCallback, fileCallback, connectCallback);
			runnable.run();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			connectCallback.onDisconnect(new OfflineException(e));
		}
	}

	private InputStream connectAndGetInputStream() throws IOException
	{
		textServerSocket = new ServerSocket();
		textServerSocket.setReuseAddress(true);
		SocketAddress addr = new InetSocketAddress(PORT_TEXT_MESSAGE);
		textServerSocket.bind(addr);

		Logger.d(TAG, "TextReceiveThread" + "Will be waiting on accept");
		textReceiverSocket = textServerSocket.accept();
		Logger.d(TAG, "TextReceiveThread" + "Connection successfull and the receiver buffer is " + textReceiverSocket.getReceiveBufferSize() + "and the send buffer is "
				+ textReceiverSocket.getSendBufferSize());
		connectCallback.onConnect();
		return textReceiverSocket.getInputStream();
	}

	public void shutDown()
	{
		try
		{
			OfflineUtils.closeSocket(textReceiverSocket);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		try
		{
			OfflineUtils.closeSocket(textServerSocket);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}
