package com.bsb.hike.modules.packPreview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.bsb.hike.R;

/**
 * Created by anubhavgupta on 15/02/16.
 */
public class PackAuthorFooterItem extends BasePackPreviewAdapterItem
{
    private String author;

    private Context mContext;

    private View authorView;

    private TextView tvAuthor;

	public PackAuthorFooterItem(Context context, String author)
	{
        this.mContext = context;
        this.author = author;
        init();
	}

    private void init()
    {
        authorView = LayoutInflater.from(mContext).inflate(R.layout.pack_author_footer, null);
        tvAuthor = (TextView) authorView.findViewById(R.id.author);

        setAuthor(author);
    }

    public void setAuthor(String author)
    {
        if(!TextUtils.isEmpty(author))
        {
            this.tvAuthor.setText(author);
        }
    }

	@Override
	public View getView()
	{
		return authorView;
	}

	@Override
	public RecyclerView.ViewHolder getViewHolder()
	{
		return new PackAuthorFooterItemViewHolder(getView());
	}

    private class PackAuthorFooterItemViewHolder extends RecyclerView.ViewHolder
    {
        public PackAuthorFooterItemViewHolder(View row)
        {
            super(row);
        }
    }
}
