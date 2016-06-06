package com.bsb.hike.models;

import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.bsb.hike.adapters.StickerPageAdapter;

/**
 * Created by anubhavgupta on 25/05/16.
 */
public class StickerPageObject {

    private View parentView;

    private GridView stickerGridView;

    private ViewGroup containerView;

    private StickerPageAdapter stickerPageAdapter;

    private StickerCategory stickerCategory;

    private StickerPageObject(Builder builder) {
        this.parentView = builder.parentView;
        this.stickerGridView = builder.stickerGridView;
        this.containerView = builder.containerView;
        this.stickerPageAdapter = builder.stickerPageAdapter;
        this.stickerCategory = builder.stickerCategory;
    }

    public View getParentView() {
        return parentView;
    }

    public void setParentView(View parentView) {
        this.parentView = parentView;
    }

    public GridView getStickerGridView() {
        return stickerGridView;
    }

    public void setStickerGridView(GridView stickerGridView) {
        this.stickerGridView = stickerGridView;
    }

    public ViewGroup getContainerView() {
        return containerView;
    }

    public void setContainerView(ViewGroup containerView) {
        this.containerView = containerView;
    }

    public StickerPageAdapter getStickerPageAdapter() {
        return stickerPageAdapter;
    }

    public void setStickerPageAdapter(StickerPageAdapter stickerPageAdapter) {
        this.stickerPageAdapter = stickerPageAdapter;
    }

    public StickerCategory getStickerCategory() {
        return stickerCategory;
    }

    public void setStickerCategory(StickerCategory stickerCategory) {
        this.stickerCategory = stickerCategory;
    }

    public static class Builder {

        private View parentView;

        private GridView stickerGridView;

        private ViewGroup containerView;

        private StickerPageAdapter stickerPageAdapter;

        private StickerCategory stickerCategory;

        public Builder setParentView(View parentView) {
            this.parentView = parentView;
            return this;
        }

        public Builder setStickerGridView(GridView stickerGridView) {
            this.stickerGridView = stickerGridView;
            return this;
        }

        public Builder setContainerView(ViewGroup containerView) {
            this.containerView = containerView;
            return this;
        }

        public Builder setStickerPageAdapter(StickerPageAdapter stickerPageAdapter) {
            this.stickerPageAdapter = stickerPageAdapter;
            return this;
        }

        public Builder setStickerCategory(StickerCategory stickerCategory) {
            this.stickerCategory = stickerCategory;
            return this;
        }

        public StickerPageObject build() {
            return new StickerPageObject(this);
        }
    }
}
