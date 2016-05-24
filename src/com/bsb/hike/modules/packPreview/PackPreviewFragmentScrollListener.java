package com.bsb.hike.modules.packPreview;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.bsb.hike.utils.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by anubhavgupta on 29/01/16.
 */
public class PackPreviewFragmentScrollListener extends RecyclerView.OnScrollListener {

    private static final String TAG = PackPreviewFragmentScrollListener.class.getSimpleName();

    private int mNumCols;

    private OnVerticalScrollListener onScrollListener;

    private Map<Integer, Integer> sRecyclerViewItemHeights = new HashMap<>();

    private int mPrevScrollY = 0;

    public static int SCROLL_UP = 1;

    public static int SCROLL_DOWN = -1;

    public PackPreviewFragmentScrollListener(int numCols, OnVerticalScrollListener onScrollListener)
    {
        mNumCols = numCols;
        this.onScrollListener = onScrollListener;
    }

    public interface OnVerticalScrollListener
    {
        void onVerticalScrolled(int dy, int scrollDirection);
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        int scrollY = getScrollY(recyclerView, mNumCols);
        Logger.d(TAG, "scrollY -  : " + scrollY);
        int diff = scrollY - mPrevScrollY;
        int scrollDirection = diff > 0 ? SCROLL_UP : SCROLL_DOWN;
        onScrollListener.onVerticalScrolled(scrollY, scrollDirection);
    }

    public int getScrollY(RecyclerView rv, int columnCount) {
        View c = rv.getChildAt(0);
        if (c == null) {
            return 0;
        }

        LinearLayoutManager layoutManager = (LinearLayoutManager)rv.getLayoutManager();
        int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();

        int scrollY = -(c.getTop());
        Logger.d(TAG, "view top - : " + c.getTop());


        if(columnCount > 1){
            sRecyclerViewItemHeights.put(firstVisiblePosition, c.getHeight());
        } else {
            sRecyclerViewItemHeights.put(firstVisiblePosition, c.getHeight());
        }

        if(scrollY<0)
            scrollY = 0;

        for (int i = 0; i < firstVisiblePosition; ++i) {
            if (sRecyclerViewItemHeights.get(i) != null) { // (this is a sanity check)
                Logger.d(TAG, "intermediate scroll at i - : " + i + " add scroll y -  : " + scrollY);
                scrollY += sRecyclerViewItemHeights.get(i); //add all heights of the views that are gone
            }
        }
        return scrollY;
    }
}
