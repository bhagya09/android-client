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
 * @file    KPTCoreServiceListener.java
 *
 * @brief   Provides callback for core service connection
 *
 * @details
 *
 *****************************************************************************/

package com.kpt.adaptxt.beta.core.coreservice;

/**
 * Provides callback for core service connection
 * @author 
 *
 */
public interface KPTCoreServiceListener {
	/**
	 * Callback to inform observers that connection with service is established.
	 * 
	 * @param coreEngine The fully constructed core engine instance.
	 */
	public void serviceConnected(KPTCoreEngine coreEngine);
}
