package com.bsb.hike.tasks;

import android.os.AsyncTask;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.adapters.StickerSettingsAdapter.DeletePackListener;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.utils.StickerManager;

/**
 * Created by nehadua on 27/01/16.
 */
public class DeleteStickerPackAsyncTask extends AsyncTask<HikeDialog, Void, Void>
{
    private StickerCategory category;
    private Context context;
    private StickerSettingsAdapter adapter;
    private HikeDialog deleteDialog;

    protected void onPreExecute ()
    {
    }

    public DeleteStickerPackAsyncTask(Context context, StickerCategory category, StickerSettingsAdapter adapter)
    {
        this.category = category;
        this.context = context;
        this.adapter = adapter;
    }

    @Override
    protected Void doInBackground(HikeDialog... params)
    {
        this.deleteDialog = params[0];
        StickerManager.getInstance().removeCategory(category.getCategoryId(), false);     //false to not remove from shop table
        return null;
    }

    protected void onPostExecute(Void result)
    {
        if (deleteDialog.isShowing()) { deleteDialog.dismiss(); }
        Toast.makeText(context, "Deleted " + category.getCategoryName() + " stickers pack.", Toast.LENGTH_SHORT).show();
        adapter.updateMappingOnPackDelete(category);
    }
}