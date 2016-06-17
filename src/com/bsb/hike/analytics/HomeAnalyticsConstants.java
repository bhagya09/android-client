package com.bsb.hike.analytics;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.support.annotation.StringDef;

/**
 * Created by piyush on 04/05/16.
 */
public class HomeAnalyticsConstants {
    public static final String HOME_OVERFLOW_MENU = "hs_ovfl";

    public static final String HOMESCREEN_KINGDOM = "act_hs";

    public static final String HOME_OVERFLOW_MENU_ITEM = "hs_ovfl_item";

    public static final String SETTINGS_UK = "settings";

    public static final String SETTINGS_ORDER = "sttng";

    public static final String UK_TIMELINE = "timeline";

    public static final String UK_TL_OPEN = "TL_open";

    public static final String UK_HS_FRIENDS = "hs_friends";

    public static final String KINGDOM_ACT_LOG2 = "act_log2";

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

    public static final String SU_TYPE_DP = "dp";

    public static final String SU_TYPE_OTHER = "other";

    @StringDef({SU_GENUS_CAMERA, SU_GENUS_GALLERY, SU_GENUS_OTHER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface StatusUpdateGenus {
    }

    public static final String SU_GENUS_CAMERA = "camera";

    public static final String SU_GENUS_GALLERY = "gallery";

    public static final String SU_GENUS_OTHER = "other";

    @StringDef({SU_SPECIES_OVERFLOW, SU_SPECIES_TIMELINE_PHOTO_BUTTON, SU_SPECIES_TIMELINE_TEXT_BUTTON, SU_SPECIES_OTHER, SU_SPECIES_FRIENDS_TAB})
    @Retention(RetentionPolicy.SOURCE)
    public @interface StatusUpdateSpecies {
    }

    public static final String SU_SPECIES_OVERFLOW = "overflow";

    public static final String SU_SPECIES_TIMELINE_PHOTO_BUTTON = "tl_photo";

    public static final String SU_SPECIES_TIMELINE_TEXT_BUTTON = "tl_text";

    public static final String SU_SPECIES_FRIENDS_TAB = "friends_tab_cam";

    public static final String SU_SPECIES_OTHER = "other";

    @StringDef({DP_SPECIES_MY_PROFILE, DP_SPECIES_EDIT_PROFILE, DP_SPECIES_FULL_VIEW, DP_SPECIES_EXTERNAL_APP, DP_SPECIES_SIGN_UP, DP_SPECIES_OTHER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProfilePicUpdateSpecies {
    }

    public static final String DP_SPECIES_MY_PROFILE = "my_profile";

    public static final String DP_SPECIES_EDIT_PROFILE = "edit_profile";

    public static final String DP_SPECIES_FULL_VIEW = "dp_full_view";

    public static final String DP_SPECIES_EXTERNAL_APP = "ext_set_dp";

    public static final String DP_SPECIES_SIGN_UP = "sign_up";

	public static final String DP_SPECIES_EDIT_DP = "edit_dp";

	public static final String DP_SPECIES_OTHER = "other";

    public static final String HS_MOVE = "hs_move";

    public static final String HS_ME = "hs_me";

    public static final String NEW_COMP = "new_comp";

}
