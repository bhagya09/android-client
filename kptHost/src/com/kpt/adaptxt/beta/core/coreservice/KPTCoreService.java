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
 * @file    KPTCoreService.java
 *
 * @brief   The android service class through which adaptxt core engine
 * 			functionalities re provided.
 *
 * @details
 *
 *****************************************************************************/

package com.kpt.adaptxt.beta.core.coreservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * KPTCoreService The android service class through which adaptxt
 * core engine functionalities re provided. 
 * 
 * @author 
 *
 */

public class KPTCoreService extends Service {
	
	/**
	 * Local binder used by IMS and Home Screen
	 */
	private KPTLocalBinder mLocalBinder;
	
	/**
	 * Core Engine handle
	 */
	private KPTCoreEngineImpl mCoreEngineImpl;
	
	/**
	 * Creates and initializes binders
	 * 
	 */
	@Override
    public void onCreate() {
		mLocalBinder = new KPTLocalBinder();
		mCoreEngineImpl = KPTCoreEngineImpl.getCoreEngineImpl();
		mCoreEngineImpl.setApplicationContext(this);
    }

	/**
	 * returns binder objects based on the intents received
	 */
	@Override
	public IBinder onBind(Intent clientIntent) {
		mCoreEngineImpl.prepareCoreFiles(getFilesDir().getAbsolutePath(), getAssets());
		return mLocalBinder;
	}
	
	/**
	 * Destroys the binder resources
	 */
	@Override
    public void onDestroy() {
		//KPTLog.e("KPT Debug", "KPTCoreService onDestroy() calling forceDestroyCore");
		mCoreEngineImpl.forceDestroyCore();
    }

}
