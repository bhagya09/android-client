package com.bsb.hike.chatthemes.model;

/**
 * Created by sriram on 13/06/16.
 */
public class ChatThemeToken {

    private String mThemeId = null;
    private String msisdn = null;
    private boolean isCustom = false;
    private String mImgPath;
    private String[] mAssets;

    public ChatThemeToken(String themeId, String msisdn, boolean isCustom) {
        this.mThemeId = themeId;
        this.msisdn = msisdn;
        this.isCustom = isCustom;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public String getThemeId() {
        return mThemeId;
    }

    public boolean isCustom() {
        return isCustom;
    }

    public String getImagePath() {
        return mImgPath;
    }

    public void setImagePath(String imgPath) {
        this.mImgPath = imgPath;
    }

    public void setAssets(String[] assets) {
        this.mAssets = assets;
    }

    public void setThemeId(String themeId) {
        this.mThemeId = themeId;
    }

    public String[] getAssets() {
        return mAssets;
    }
}
