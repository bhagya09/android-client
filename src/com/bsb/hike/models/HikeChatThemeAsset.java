package com.bsb.hike.models;

/**
 * Created by sriram on 22/02/16.
 */
public class HikeChatThemeAsset {
    private String assetId;
    private int type;
    private String value;
    private boolean isDownloaded = false;

    public HikeChatThemeAsset(String assetId, int type, String value) {
        this.assetId = assetId;
        this.type = type;
        this.value = value;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setIsDownloaded(boolean isDownloaded) {
        this.isDownloaded = isDownloaded;
    }
}
