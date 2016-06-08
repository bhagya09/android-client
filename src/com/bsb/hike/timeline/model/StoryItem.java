package com.bsb.hike.timeline.model;

import android.content.Intent;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Building blocks of "stories" listview
 * <p/>
 * Created by AtulM on 24/05/16.
 */
public class StoryItem<T, K> {
    private int mCategory = CATEGORY_NONE;

    private int mType;

    private String mName;

    private String mSubText;

    private Intent mIntent;

    @IntDef({TYPE_INTENT, TYPE_FRIEND, TYPE_BRAND, TYPE_HEADER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface StoryItemType {
    }

    // Fire associated intent
    public static final int TYPE_INTENT = 0;

    // Open friends story
    public static final int TYPE_FRIEND = 1;

    // Open brand story? TODO
    public static final int TYPE_BRAND = 2;

    // Serves as header in StoryItemList
    public static final int TYPE_HEADER = 3;

    // Generalization of TYPE_INTENT, TYPE_FRIEND and TYPE_BRAND. Used to distinguish between header/non-header in adapters.
    public static final int TYPE_DEFAULT = 4;

    //Type Info (ContactInfo in case mType == TYPE_FRIEND)
    private K mTypeInfo;

    // Data objects list (StatusMessage in case of mType == TYPE_FRIEND)
    private List<T> mObjectList;

    @IntDef({CATEGORY_NONE, CATEGORY_RECENT, CATEGORY_ALL, CATEGORY_DEFAULT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface StoryCategory {
    }

    public static final int CATEGORY_NONE = 11;

    public static final int CATEGORY_RECENT = 12;

    public static final int CATEGORY_ALL = 13;

    public static final int CATEGORY_DEFAULT = 14;

    public StoryItem(@StoryItemType int argType, @NonNull String argName) {
        mType = argType;
        mName = argName;
    }

    public void setCategory(@StoryCategory int argCategory) {
        mCategory = argCategory;
    }

    public void setSubText(String argSubText) {
        mSubText = argSubText;
    }

    public void setIntent(@NonNull Intent argIntent) {
        mIntent = argIntent;
    }

    public int getType() {
        return mType;
    }

    public String getTitle() {
        return mName;
    }

    public String getSubText() {
        return mSubText;
    }

    public Intent getIntent() {
        return mIntent;
    }

    public int getCategory() {
        return mCategory;
    }

    public boolean isRead() {
        if (mCategory == CATEGORY_ALL || mCategory == CATEGORY_DEFAULT) {
            return true;
        } else {
            return false;
        }
    }

    public K getTypeInfo() {
        return mTypeInfo;
    }

    public void setTypeInfo(K mTypeInfo) {
        this.mTypeInfo = mTypeInfo;
    }

    public void setDataObjectList(List<T> dataList) {
        if (dataList != null) {
            mObjectList = dataList;
        }
    }

    public List<T> getDataObjects() {
        return mObjectList;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof StoryItem) {
            StoryItem item = (StoryItem) o;
            if (item.getTitle().equals(getTitle()) && item.getType() == getType()) {
                return true;
            }
        }
        return super.equals(o);
    }
}