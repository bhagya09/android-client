/* 
 * Copyright (c) 2009, 2012 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.internal;

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttDisconnect;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttOutputStream;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

import com.bsb.hike.utils.Logger;


public class CommsSender implements Runnable {
	/**
	 * Sends MQTT packets to the server on its own thread
	 */
	private boolean running 		= false;
	private Object lifecycle 		= new Object();
	private ClientState clientState = null;
	private MqttOutputStream out;
	private ClientComms clientComms = null;
	private CommsTokenStore tokenStore = null;
	private Thread 	sendThread		= null;
	
	private final static String className = CommsSender.class.getName();
	private final String TAG = "CommsSender";
	
	public CommsSender(ClientComms clientComms, ClientState clientState, CommsTokenStore tokenStore, OutputStream out) {
		this.out = new MqttOutputStream(out);
		this.clientComms = clientComms;
		this.clientState = clientState;
		this.tokenStore = tokenStore;
	}
	
	/**
	 * Starts up the Sender thread.
	 */
	public void start(String threadName) {
		synchronized (lifecycle) {
			if (running == false) {
				running = true;
				sendThread = new Thread(this, threadName);
				sendThread.start();
			}
		}
	}

	/**
	 * Stops the Sender's thread.  This call will block.
	 */
	public void stop() {
		final String methodName = "stop";
		
		synchronized (lifecycle) {
			//@TRACE 800=stopping sender
			Logger.d(TAG, "stopping sender thread started");
			if (running) {
				running = false;
				if (!Thread.currentThread().equals(sendThread)) {
					try {
						// first notify get routine to finish
						clientState.notifyQueueLock();
						// Wait for the thread to finish.
						sendThread.join();
					}
					catch (InterruptedException ex) {
					}
				}
			}
			sendThread=null;
			//@TRACE 801=stopped
			Logger.d(TAG, "stopping sender completed");
		}
	}
	
	public void run() {
		final String methodName = "run";
		MqttWireMessage message = null;
		while (running && (out != null)) {
			try {
				message = clientState.get();
				if (message != null) {
					//@TRACE 802=network send key={0} msg={1}

					if (message instanceof MqttAck) {
						out.write(message);
						out.flush();
					} else {
						MqttToken token = tokenStore.getToken(message);
						// While quiescing the tokenstore can be cleared so need 
						// to check for null for the case where clear occurs
						// while trying to send a message.
						if (token != null) {
							synchronized (token) {
								out.write(message);
								try {
									out.flush();
								} catch (IOException ex) {
									// The flush has been seen to fail on disconnect of a SSL socket
									// as disconnect is in progress this should not be treated as an error
									if (!(message instanceof MqttDisconnect))
										throw ex;
								}
								clientState.notifySent(message);
							}
						}
					}
				} else { // null message
					//@TRACE 803=get message returned null, stopping}
					Logger.d(TAG, "get message returned null, stopping");
					running = false;
				}
			} catch (MqttException me) {
				handleRunException(message, me);
			} catch (Exception ex) {		
				handleRunException(message, ex);	
			}
		} // end while
		
		//@TRACE 805=<

	}

	private void handleRunException(MqttWireMessage message, Exception ex) {
		final String methodName = "handleRunException";
		//@TRACE 804=exception
		Logger.d(TAG, "exception in run , cause : " + ex.getCause());
		MqttException mex;
		if ( !(ex instanceof MqttException)) {
			mex = new MqttException(MqttException.REASON_CODE_CONNECTION_LOST, ex);
		} else {
			mex = (MqttException)ex;
		}

		running = false;
		clientComms.shutdownConnection(null, mex);
	}
}
