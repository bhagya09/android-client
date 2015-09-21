/* 
 * Copyright (C) 2008 The Android Open Source Project 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */ 

package com.kpt.adaptxt.beta;




/**
 * This class provides controlled logging functionality.
 * This a wrapper on top of android's system log class.
 * In the release build all the following logs must be disabled.
 * Should be enabled only for development purposes.
 * @author smadabusi
 *
 */
public class KPTLog {
	/**
	 * Sends a debug log message to log output.
	 * @param tag Used to identify the source
	 * @param msg The message to be logged.
	 */
	public static void d(String tag, String msg) {
		//Log.d(tag, msg);
	}
	
	/**
	 * Sends a error log message to log output.
	 * @param tag Used to identify the source
	 * @param msg The message to be logged.
	 */
	public static void e(String tag, String msg) {
		//Log.e(tag, msg);
	}
	
	/**
	 * Sends a information log message to log output.
	 * @param tag Used to identify the source
	 * @param msg The message to be logged.
	 */
	public static void i(String tag, String msg) {
		//Log.i(tag, msg);
	}
	
	/**
	 * Sends a warning log message to log output.
	 * @param tag Used to identify the source
	 * @param msg The message to be logged.
	 */
	public static void w(String tag, String msg) {
		//Log.w(tag, msg);
	}
	
	/**
	 * Sends a verbose log message to log output.
	 * @param tag Used to identify the source
	 * @param msg The message to be logged.
	 */
	public static void v(String tag, String msg) {
		//Log.v(tag, msg);
	}
	
	/**
	 * Sends a "What a Terrible Failure" log message to log output.
	 * @param tag Used to identify the source
	 * @param msg The message to be logged.
	 */
	public static void wtf(String tag, String msg) {
		//Log.wtf(tag, msg);
	}
}
