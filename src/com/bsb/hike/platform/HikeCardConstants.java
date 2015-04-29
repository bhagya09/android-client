package com.bsb.hike.platform;

/**
 * Contains constants used for making card messages.
 * 
 * @author Atul M
 */
public class HikeCardConstants
{
	/**
	 * NOTE : DO NOT ALTER VARIABLE NAMES. USED FOR SERIALIZATION BY GSON.
	 */

	/**
	 * Message type to be used in case of card layouts.
	 */
	public static final String CARD_MSG_TYPE = "c";

	/**
	 * Message "from" to be used while sending it to HikeService. This shall be replaced at messenger with hike-id maps.
	 */
	public static final String CARD_MSG_FROM = "hike-sdk";

	/** Default love id. */
	public static final String CARD_MSG_LOVE_ID = "-1";

	/** Title content type. */
	public static final String CARD_TITLE_CONTENT_TYPE = "text/plain";

	/** Title layout tag. */
	public static final String CARD_TITLE_TAG = "T1";

	/** Description content type. */
	public static final String CARD_DESC_CONTENT_TYPE = "text/plain";

	/** Description layout tag. */
	public static final String CARD_DESC_TAG = "T2";

	/** Price text content type. */
	public static final String CARD_PRICE_CONTENT_TYPE = "text/plain";

	/** Price text layout tag. */
	public static final String CARD_PRICE_TAG = "T4";

	/** Thumbnail layout tag. */
	public static final String CARD_THUMBNAIL_TAG = "I1";

	/** Action layout tag. */
	public static final String CARD_PRIMARY_ACTION_TAG = "CARD";
	
	public static final String CARD_CTA_TEXT_TAG = "T3";
	
	public static final String CARD_CTA_CONTENT_TYPE = "text/plain";
}
