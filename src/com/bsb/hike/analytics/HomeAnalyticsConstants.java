package com.bsb.hike.analytics;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.support.annotation.StringDef;

/**
 * Created by piyush on 04/05/16.
 */
public class HomeAnalyticsConstants
{
	public static final String HOME_OVERFLOW_MENU = "hs_ovfl";

	public static final String HOMESCREEN_KINGDOM = "act_hs";

	public static final String HOME_OVERFLOW_MENU_ITEM = "hs_ovfl_item";

	public static final String SETTINGS_UK = "settings";

	public static final String SETTINGS_ORDER = "sttng";

	public static final String TIMELINE_UK = "timeline";

	public static final String BACKUP_UK = "backup";

	public static final String HIDDEN_UK = "hs_hi";

	public static final String INVITE_FRIENDS = "invt_frnds";

	/*****************************************************************
	 * Status update analytics constants
	 ******************************************************************/
	public static final String ORDER_STATUS_UPDATE = "su";

	public static final String SU_TYPE_TEXT = "text";

	public static final String SU_TYPE_IMAGE = "image";

	public static final String SU_TYPE_TEXT_IMAGE = "text_image";

	public static final String SU_TYPE_OTHER = "other";

	@StringDef({ SU_GENUS_CAMERA, SU_GENUS_GALLERY, SU_GENUS_OTHER })
	@Retention(RetentionPolicy.SOURCE)
	public @interface StatusUpdateGenus{}

	public static final String SU_GENUS_CAMERA = "camera";

	public static final String SU_GENUS_GALLERY = "gallery";

	public static final String SU_GENUS_OTHER = "other";

	@StringDef({ SU_SPECIES_OVERFLOW, SU_SPECIES_TIMELINE_PHOTO_BUTTON, SU_SPECIES_TIMELINE_TEXT_BUTTON, SU_SPECIES_OTHER})
	@Retention(RetentionPolicy.SOURCE)
	public @interface StatusUpdateSpecies{}

	public static final String SU_SPECIES_OVERFLOW = "suSpoverflow";

	public static final String SU_SPECIES_TIMELINE_PHOTO_BUTTON = "suSpPhBtn";

	public static final String SU_SPECIES_TIMELINE_TEXT_BUTTON = "suSpTxtBtn";

	public static final String SU_SPECIES_OTHER = "suSpOther";
}
