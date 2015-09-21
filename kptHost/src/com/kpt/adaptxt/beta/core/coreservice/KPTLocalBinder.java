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
 * @file    KPTLocalBinder.java
 *
 * @brief   Core service's local binder object shared with the clients.
 *
 * @details
 *
 *****************************************************************************/

package com.kpt.adaptxt.beta.core.coreservice;

import android.os.Binder;

/**
 * KPTLocalBinder Core service's local binder object shared with the clients. 
 * 
 * @author 
 *
 */

public class KPTLocalBinder extends Binder {
	
	/**
	 * Core Engine reference
	 */
	private KPTCoreEngineImpl coreEngine = null;
	
	/**
	 * Initializes and gets core engine object.
	 * 
	 */
	public KPTCoreEngine getCoreEngineInterface() {
		coreEngine = KPTCoreEngineImpl.getCoreEngineImpl();
		return coreEngine;
	}
	
}
