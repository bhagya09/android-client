package com.bsb.hike.platform;

/**
 * Interface used to deal with HttpClient response result in case of a file upload.
 * 
 * @author samarth
 */
public interface IFileUploadListener
{
	/**
	 * In case of successful request caller will receive the s3 path of the file that is uploaded on the server
	 * @param response
	 */
	void onRequestSuccess(String response);
	
	/**
	 * In case of unsuccessful request caller will receive the appropriate exception error as a response
	 * @param response
	 */
	void onRequestFailure(String response);
}