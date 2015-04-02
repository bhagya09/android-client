package com.bsb.hike.modules.httpmgr.request.requestbody;

import java.io.IOException;
import java.io.OutputStream;

import com.bsb.hike.modules.httpmgr.request.Request;
import com.squareup.okhttp.RequestBody;

/**
 * Binary data with an associated mime type.
 * 
 * @author anubhav & sidharth
 */
public interface IRequestBody
{
	/**
	 * Original filename.
	 * 
	 * Used only for multipart requests, may be null.
	 */
	String fileName();

	/**
	 * Returns the mime type.
	 */
	String mimeType();

	/**
	 * Length in bytes or -1 if unknown.
	 * 
	 * @return
	 */
	long length();

	/**
	 * Writes these bytes to the given output stream.
	 * 
	 * @param out
	 * @throws IOException
	 */
	void writeTo(Request<?> request, OutputStream out) throws IOException;

	/**
	 * Returns the request body of okhttp that are processed in internal classes
	 * 
	 * @return
	 */
	RequestBody getRequestBody();
}
