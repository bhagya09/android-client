package com.bsb.hike.modules.packPreview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.utils.Utils;

/**
 * Created by anubhavgupta on 15/02/16.
 */
public class PackAuthorFooterItem extends BasePackPreviewAdapterItem
{
    private String author;

    private String copyrightString;

    private Context mContext;

    private View authorView;

    private TextView tvAuthor;

    private TextView tvCopyRight;

	public PackAuthorFooterItem(Context context, String author, String copyrightString)
	{
        this.mContext = context;
        this.author = author;
        this.copyrightString = copyrightString;
        init();
	}

    private void init()
    {
        authorView = LayoutInflater.from(mContext).inflate(R.layout.pack_author_footer, null);
        tvAuthor = (TextView) authorView.findViewById(R.id.tvAuthor);
        tvCopyRight = (TextView) authorView.findViewById(R.id.tvCopyRight);

        setTexts(author, copyrightString);
    }

    public void setTexts(String author, String copyrightString)
    {
        if(!TextUtils.isEmpty(author))
        {
            this.tvAuthor.setVisibility(View.VISIBLE);
            this.tvAuthor.setText(Html.fromHtml(mContext.getString(R.string.author_text, author)));
        }
        else
        {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tvCopyRight.getLayoutParams();
            params.topMargin = Utils.dpToPx(24);
        }

        if(!TextUtils.isEmpty(copyrightString))
        {
            this.tvCopyRight.setVisibility(View.VISIBLE);
            this.tvCopyRight.setText(copyrightString);
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

    @Override
    public void releaseResources()
    {

    }
}
