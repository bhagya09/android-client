package com.bsb.hike.ExplandingCells;

/**
 * Referenced from : https://www.youtube.com/watch?v=mwE61B56pVQ
 * This custom object is used to populate the list adapter.It contains a reference
 * to an image,title,and the extra text to be displayed.Furthermore,it keeps track
 * of the current state(collapsed/expanded)of the corresponding item in the list,
 * as well as store the height of the cell in its collapsed state.
 */

public class ExpandableListItem implements OnSizeChangedListener {

    private boolean mIsExpanded;
    private int mCollapsedHeight;
    private int mExpandedHeight;

    public ExpandableListItem(int collapsedHeight) {
        mCollapsedHeight = collapsedHeight;
        mIsExpanded = false;
        mExpandedHeight = -1;
    }

    public boolean isExpanded() {
        return mIsExpanded;
    }

    public void setExpanded(boolean isExpanded) {
        mIsExpanded = isExpanded;
    }

    public int getCollapsedHeight() {
        return mCollapsedHeight;
    }

    public void setCollapsedHeight(int mCollapsedHeight) {
        this.mCollapsedHeight = mCollapsedHeight;
    }

    public void setExpandedHeight(int expandedHeight) {
        mExpandedHeight = expandedHeight;
    }

    @Override
    public void onSizeChanged(int newHeight) {
        setExpandedHeight(newHeight);
    }
}