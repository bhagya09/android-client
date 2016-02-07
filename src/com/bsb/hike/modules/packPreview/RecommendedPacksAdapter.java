package com.bsb.hike.modules.packPreview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.smartImageLoader.MiniStickerLoader;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import java.util.List;

/**
 * Created by anubhavgupta on 25/01/16.
 */
public class RecommendedPacksAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private MiniStickerLoader miniStickerLoader;

    private Context mContext;

    private List<Sticker> stickerList;

    private int sizeEachImage;

    public RecommendedPacksAdapter(Context context)
    {
        this.mContext = context;
        this.miniStickerLoader = new MiniStickerLoader(true);
        this.sizeEachImage = StickerSearchUtils.getStickerSize();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        ImageView stickerIv = new ImageView(mContext);
        RecyclerView.LayoutParams ll = new RecyclerView.LayoutParams(sizeEachImage, sizeEachImage);
        int padding = (int) (5 * Utils.scaledDensityMultiplier);
        stickerIv.setLayoutParams(ll);
        stickerIv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        stickerIv.setPadding(padding, padding, padding, padding);
        return new StickerViewHolder(stickerIv);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        Sticker sticker = stickerList.get(i);
        StickerViewHolder stickerViewHolder = (StickerViewHolder) viewHolder;
        ImageView stickerIv = stickerViewHolder.stickerIv;
        miniStickerLoader.loadImage(StickerManager.getInstance().getStickerSetString(sticker.getStickerId(), sticker.getCategoryId()), stickerIv);
    }

    @Override
    public int getItemCount() {
        if(Utils.isEmpty(stickerList)) return 0;
        return stickerList.size();
    }

    public void setStickerList(List<Sticker> stickerList) {
        this.stickerList = stickerList;
    }

    private class StickerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        private ImageView stickerIv;

        public StickerViewHolder(View row)
        {
            super(row);
            stickerIv = (ImageView) row;
            row.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

        }
    }
}
