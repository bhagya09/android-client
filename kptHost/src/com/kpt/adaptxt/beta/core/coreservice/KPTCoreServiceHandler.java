/******************************************************************************
 * COPYRIGHT (c) 2010 KeyPoint Technologies (UK) Ltd. All rights reserved.
 *
 * The copyright to the computer program(s) herein is the property of KeyPoint
 * Technologies (UK) Ltd. The program(s) may be used and/or copied only with
 * the written permission from KeyPoint Technologies (UK) Ltd or in accordance
 * with the terms and conditions stipulated in the agreement/contract under
 * which the program(s) have been supplied.
 */
/**
 * @file    KPTCoreServiceHandler.java
 *
 * @brief   Provides the functionality to create and handle core engine service
 *
 * @details
 *
 *****************************************************************************/

package com.kpt.adaptxt.beta.core.coreservice;

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * KPTCoreServiceHandler provides the functionality to create and handle core engine service 
 * 
 * @author 
 *
 */

public class KPTCoreServiceHandler {
	
	/**
	 * Service Connection Object
	 */
	private KPTServiceConnection mServiceConnection;
	
	/**
	 * Handle to core engine interface
	 */
	private KPTCoreEngine mCoreEngine = null;
	
	/**
	 * Context for which service connection is made
	 */
	private Context mComponentContext;
	
	/**
	 * Singleton core service instance
	 */
	private static KPTCoreServiceHandler mCoreServiceHandler;
	
	/**
	 * To maintain list of listeners.
	 */
	private List<KPTCoreServiceListener> mCoreServiceListeners;
	
	/**
	 * Maintain reference count for singleton instance to release the core service properly
	 */
	private int mReferenceCount;

	
	/**
	 * Default Constructor
	 */
	private KPTCoreServiceHandler(Context componentContext) {
		mServiceConnection = new KPTServiceConnection();
		mComponentContext = componentContext;
		mCoreServiceListeners = new ArrayList<KPTCoreServiceListener>();
	}
	
	/**
	 * Gets the singleton instance of core service handler.
	 * @param componentContext Context of the calling component
	 * @return Singleton instance of this class.
	 */
	public static KPTCoreServiceHandler getCoreServiceInstance(Context componentContext) {
		if(mCoreServiceHandler == null) {
			mCoreServiceHandler = new KPTCoreServiceHandler(componentContext);
			mCoreServiceHandler.initializeCoreService();
		}
		mCoreServiceHandler.mReferenceCount++;
		//Log.e("KPT Debug", "KPTCoreServiceHandler.getCoreServiceInstance() mReferenceCount=" + mCoreServiceHandler.mReferenceCount);
		return mCoreServiceHandler;
	}
	
	/**
	 * Connects to the local core service
	 * @return If core service is successfully bound
	 */
	private boolean initializeCoreService() {
		//KPTLog.e("KPT Debug", "KPTCoreServiceHandler.initializeCoreService() mReferenceCount=" + mReferenceCount);
		boolean result = mComponentContext.bindService(new Intent(mComponentContext, 
	            KPTCoreService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
		//Log.e("VMC", "KPTCoreServiceHandler.initializeCoreService() result :  "+result);
		return result;
	}
	
	/**
	 * Registers an observer for receiving notification once
	 * connection with core service is established.
	 */
	public void registerCallback(KPTCoreServiceListener coreServiceListener) {
		if(!mCoreServiceListeners.isEmpty()) {
			
			// Don't add duplicate listeners if it already exists
			
			boolean listenerExists = false;
			for(KPTCoreServiceListener coreServiceObserver : mCoreServiceListeners) {
				if(coreServiceObserver == coreServiceListener) {
					listenerExists = true;
					break;
				}
			}
			if(!listenerExists) {
				mCoreServiceListeners.add(coreServiceListener);
			}
		}
		else {
			mCoreServiceListeners.add(coreServiceListener);
		}
	}
	
	/**
	 * Removes the registered observer from the observer's list. 
	 */
	public boolean unregisterCallback(KPTCoreServiceListener coreServiceListener) {
		if(!mCoreServiceListeners.isEmpty()) {
			boolean result = mCoreServiceListeners.remove(coreServiceListener);
			return result;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Connects to the local core service
	 * 
	 */
	public KPTCoreEngine getCoreInterface() {
		//Assert.assertNotNull(mCoreEngine);
		return mCoreEngine;
	}
	/**
	 * Force destroy's core service abruptly and should be called by the IME service alone
	 * 
	 */
	public void forceDestroyCoreService() {
		//KPTLog.e(KPTDebugString, "KPTCoreEngineImpl::forceDestroyCore() mReferenceCount " + mReferenceCount);
		mReferenceCount = 1;
		destroyCoreService();
	}
	
	/**
	 * Connects to the local core service
	 * 
	 */
	public void destroyCoreService() {
		//KPTLog.e("KPT Debug", "KPTCoreServiceHandler.destroyCoreService() mReferenceCount=" + mReferenceCount);
		mReferenceCount--;
		if(mComponentContext != null && mReferenceCount == 0) {
			mComponentContext.unbindService(mServiceConnection);
			mCoreServiceHandler = null;
		}
	}
	
	/**
	 * 	
	 * @author smadabusi
	 *
	 */
	private class KPTServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName serviceClass, IBinder binder) {
			//Log.e("VMC", "KPTServiceConnection : onServiceConnected _-_-_-_-_-_-_-_-_- localBinder.getCoreEngineInterface()");
			KPTLocalBinder localBinder = (KPTLocalBinder)binder;
			mCoreEngine = localBinder.getCoreEngineInterface();
			
			// Inform the observers if any observer is registered
			if(!mCoreServiceListeners.isEmpty()) {
				for(KPTCoreServiceListener coreServiceListener : mCoreServiceListeners) {
					coreServiceListener.serviceConnected(mCoreEngine);
				}
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName serviceClass) {
			//Log.e("KPT Debug", "KPTCoreServiceHandler.onServiceDisconnected() mReferenceCount=" + mReferenceCount);
			mCoreEngine = null;
		}
		
	}
	
}
