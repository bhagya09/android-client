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
 * @file    KPTShutdownReceiver.java
 *
 * @brief   Handles closing of adaptxt core service during device shutdown.
 *
 * @details
 *
 *****************************************************************************/

package com.kpt.adaptxt.beta.packageinstaller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngine;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceHandler;

/**
 * Handles closing of adaptxt core service during device shutdown.
 * 
 * @author smadabusi
 *
 */
public class KPTShutdownReceiver extends BroadcastReceiver {

	/**
	 * Called when device is shutdown.
	 */
	@Override
	public void onReceive(Context appContext, Intent broadcastIntent) {
		//KPTLog.e("****Adaptxt shutting***", "####Adaptxt shutting down####");
		KPTCoreServiceHandler coreServiceHandler = KPTCoreServiceHandler.getCoreServiceInstance(appContext);
		KPTCoreEngine coreEngine = coreServiceHandler.getCoreInterface();
		if(coreEngine != null) {
			coreEngine.forceDestroyCore();
		}
		coreServiceHandler.destroyCoreService();
	}

}
