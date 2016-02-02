package com.bsb.hike.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import com.bsb.hike.adapters.StickerSettingsAdapter;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.utils.StickerManager;

/**
 * Created by nehadua on 27/01/16.
 */
public class DeleteStickerPackAsyncTask extends AsyncTask<Void, Integer, Boolean>
{
    private StickerCategory category;
    private Context context;
    private StickerSettingsAdapter adapter;

    protected void onPreExecute ()
    {
        category.setState(StickerCategory.DELETING);
        adapter.notifyDataSetChanged();
    }

    public DeleteStickerPackAsyncTask(Context context, StickerCategory category, StickerSettingsAdapter adapter)
    {
        this.category = category;
        this.context = context;
        this.adapter = adapter;
    }

    @Override
    protected Boolean doInBackground(Void... params)
    {
        StickerManager.getInstance().removeCategory(category.getCategoryId(), false);     //false to not remove from shop table
        return true;
    }

    protected void onPostExecute(Boolean result)
    {
            Toast.makeText(context, "Deleted " + category.getCategoryName() + " stickers pack.", Toast.LENGTH_SHORT).show();
            adapter.updateMappingOnPackDelete(category);
    }
}