package com.bsb.hike.tasks;

import android.os.AsyncTask;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.utils.StickerManager;

/**
 * Created by nehadua on 27/01/16.
 */
public class DeleteStickerPackAsyncTask extends AsyncTask<Void, Void, Void>
{
    private StickerCategory deleteCategory;

    public DeleteStickerPackAsyncTask(StickerCategory category) {
        this.deleteCategory = category;
    }

    protected void onPreExecute ()
    {
    }

    @Override
    protected Void doInBackground(Void... params)
    {
        StickerManager.getInstance().removeCategory(deleteCategory.getCategoryId(), false);     //false to not remove from shop table
        return null;
    }

    protected void onPostExecute(Void result)
    {
        HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_PACK_DELETED, deleteCategory);
    }
}