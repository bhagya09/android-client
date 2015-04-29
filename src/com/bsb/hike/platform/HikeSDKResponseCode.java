package com.bsb.hike.platform;

/**
 * Contains code used to determine status of various operations.
 * 
 * @author Atul M
 */
public class HikeSDKResponseCode
{

	/** The status code. */
	private byte statusCode;

	/** Status code indicating success state. */
	public static final byte STATUS_OK = -20;

	/**
	 * Status code indicating SDK is not connected with HikeService. Call initialize when this is obtained.
	 */
	public static final byte STATUS_UNBOUND = -21;

	/** Status code representing event of unknown exception being thrown. */
	public static final byte STATUS_EXCEPTION = -22;

	/** Status code indicating failure state. */
	public static final byte STATUS_FAILED = -23;

	/**
	 * Status code indicating that hike messenger client need to be updated. <br>
	 * <br>
	 * Use {@link com.bsb.hike.sdk.HikeSDK#openHikeAppPlayStore() openHikeAppPlayStore()} for updating.
	 */
	public static final byte MESSENGER_UPDATE_REQUIRED = -24;

	/**
	 * Status code indicating that SDK needs to be updated. <br>
	 * For latest version please visit <br>
	 * TODO www.hike.in
	 */
	public static final byte SDK_UPDATE_REQUIRED = -25;

	/** The Constant UNSUPPORTED_REQUEST. */
	public static final byte UNSUPPORTED_REQUEST = -26;

	/** The Constant STATUS_AUTH_REQUIRED. */
	public static final byte STATUS_AUTH_REQUIRED = -27;

	/**
	 * Gets the code.
	 * 
	 * @return the code
	 */
	public byte getCode()
	{
		return statusCode;
	}

	/**
	 * Sets the code.
	 * 
	 * @param argStatusCode
	 *            the new code
	 */
	public void setCode(byte argStatusCode)
	{
		statusCode = argStatusCode;
	}
}
