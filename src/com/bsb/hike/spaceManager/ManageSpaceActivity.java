package com.bsb.hike.spaceManager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.spaceManager.adapter.ManageSpaceAdapter;
import com.bsb.hike.spaceManager.models.CategoryItem;
import com.bsb.hike.spaceManager.items.HeaderItem;
import com.bsb.hike.spaceManager.models.SpaceManagerItem;
import com.bsb.hike.spaceManager.models.SubCategoryItem;
import com.bsb.hike.tasks.DeleteAccountTask;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.List;


public class ManageSpaceActivity extends HikeAppStateBaseFragmentActivity implements HikePubSub.UiListener,
        View.OnClickListener, ManageSpaceAdapter.IDeleteButtonToggleListener
{
    private static final String TAG = "ManageSpaceActivity";

    private List<SpaceManagerItem> spaceManagerItems;

    private List<CategoryItem> categoryList;

    private RecyclerView manageSpaceListView;

    private ManageSpaceAdapter manageSpaceAdapter;

    private LinearLayoutManager mLayoutManager;

    private View emptyLayout;

    protected HikeDialog dialog;

    private ProgressDialog deleteProgressDialog;

    private View doneBtn;

    private String[] uiPubSubTypes = {HikePubSub.SPACE_MANAGER_ITEMS_FETCH_SUCCESS, HikePubSub.SPACE_MANAGER_ITEMS_FETCH_FAIL, HikePubSub.SPACE_MANAGER_ITEMS_DELETE_SUCCESS, HikePubSub.SPACE_MANAGER_ITEMS_DELETE_FAIL};

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        long start = System.currentTimeMillis();

        if(SpaceManagerUtils.isSpaceManagerEnabled())
        {
            setContentView(R.layout.space_manager_main);
            manageSpaceListView = (RecyclerView) findViewById(R.id.smRecycleView);
            mLayoutManager = new LinearLayoutManager(ManageSpaceActivity.this);
            manageSpaceListView.setLayoutManager(mLayoutManager);
            emptyLayout = findViewById(R.id.sm_no_item);
            init();
        }
        else
        {
            loadFallbackView();
        }

        setupActionBar();

        recordScreenopenSource(getIntent());

        Logger.d(TAG, "time taken: " + (System.currentTimeMillis() - start));
    }

    private void loadFallbackView()
    {
    }

    private void init()
    {
        findViewById(R.id.progress_container).setVisibility(View.VISIBLE);
        HikeMessengerApp.getPubSub().addUiListener(this, uiPubSubTypes);
        fetchItems();
    }

    private void fetchItems()
    {
        Intent fetchItemsIntent = new Intent(this, SpaceManagerIntentService.class);
        fetchItemsIntent.setAction(SpaceManagerIntentService.ACTION_FETCH_SPACE_MANAGER_ITEMS);
        this.startService(fetchItemsIntent);
    }

    private void onItemFetchFailure()
    {
        Logger.d(TAG, "received invalid list or items");
        findViewById(R.id.progress_container).setVisibility(View.GONE);
        loadFallbackView();
    }

    private void setupActionBar()
    {
        ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

        View actionBarView = LayoutInflater.from(this).inflate(R.layout.space_manager_action_bar, null);
        actionBar.setCustomView(actionBarView);

        doneBtn = actionBarView.findViewById(R.id.done_container);
        doneBtn.setOnClickListener(this);
        toggleDeleteButton(false);
    }

    @Override
    public void onUiEventReceived(String type, Object object)
    {
        Logger.d(TAG, "received pubsub - " + type);
        if(type.equals(HikePubSub.SPACE_MANAGER_ITEMS_FETCH_SUCCESS))
        {
            if(object != null && object instanceof List)
            {
                Logger.d(TAG, "successfully fetched space manager items");
                categoryList = (ArrayList<CategoryItem>) object;
                spaceManagerItems = SpaceManagerUtils.getValidItemsList(categoryList);
                Logger.d(TAG, "items list: " + spaceManagerItems.toString());
                findViewById(R.id.progress_container).setVisibility(View.GONE);
                updateUI();
            }
            else
            {
                onItemFetchFailure();
            }
        }
        else if(type.equals(HikePubSub.SPACE_MANAGER_ITEMS_FETCH_FAIL))
        {
            onItemFetchFailure();
        }
        else if(type.equals(HikePubSub.SPACE_MANAGER_ITEMS_DELETE_SUCCESS))
        {
            Logger.d(TAG, "successfully deleted space manager items");
            spaceManagerItems = SpaceManagerUtils.getValidItemsList(categoryList);
            updateUI();
            dismissProgressDialog();
        }
        else if(type.equals(HikePubSub.SPACE_MANAGER_ITEMS_DELETE_FAIL))
        {
            Logger.d(TAG, "failure in deleting space manager items");
            dismissProgressDialog();
        }
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.done_container:
                handleDeletButtonClicked();
                break;

        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    private void handleDeletButtonClicked()
    {
        //TODO get total size to be deleted
        long size = SpaceManagerUtils.getTotalSizeToDelete(spaceManagerItems);

        this.dialog = HikeDialogFactory.showDialog(ManageSpaceActivity.this, HikeDialogFactory.SPACE_MANAGER_DELETE_CONFIRMATION_DIALOG, new HikeDialogListener()
        {
            @Override
            public void negativeClicked(HikeDialog hikeDialog)
            {
                //TODO Analytics logs
                hikeDialog.dismiss();
            }

            @Override
            public void positiveClicked(HikeDialog hikeDialog)
            {
                //TODO Analytics logs
                hikeDialog.dismiss();
                deleteProgressDialog = ProgressDialog.show(ManageSpaceActivity.this, null, getString(R.string.delete_space_loader_text));
                HikeHandlerUtil.getInstance().postRunnable(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        for (int i = 0; i < spaceManagerItems.size(); i++)
                        {
                            SpaceManagerItem item = spaceManagerItems.get(i);
                            if (item instanceof SubCategoryItem)
                            {
                                ((SubCategoryItem) item).onDelete();
                            }
                        }

                        HikeMessengerApp.getPubSub().publishOnUI(HikePubSub.SPACE_MANAGER_ITEMS_DELETE_SUCCESS, null);
                    }
                });
            }

            @Override
            public void neutralClicked(HikeDialog hikeDialog)
            {

            }
        }, Utils.getSizeForDisplay(size));
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        HikeMessengerApp.getPubSub().removeUiListener(this, uiPubSubTypes);
    }

    private void dismissProgressDialog()
    {
        if(deleteProgressDialog != null && deleteProgressDialog.isShowing())
        {
            deleteProgressDialog.dismiss();
        }
    }

    private void recordScreenopenSource(Intent intent)
    {
        if(intent.hasExtra(HikeConstants.SCREEN))
        {
            //TODO Analytics logs
            intent.removeExtra(HikeConstants.SCREEN);
        }
        else
        {
            //TODO Analytics logs
        }
    }

    public void toggleDeleteButton(boolean enable)
    {
        doneBtn.findViewById(R.id.arrow).setEnabled(enable);
        doneBtn.findViewById(R.id.delete_btn).setEnabled(enable);
        doneBtn.setEnabled(enable);
    }

    public HeaderItem getHeaderItem()
    {
        return new HeaderItem(getResources().getString(R.string.sm_clear_all_header) ,getResources().getString(R.string.sm_clear_all_subheader));
    }

    private void updateUI()
    {
        if(spaceManagerItems.isEmpty())
        {
            emptyLayout.setVisibility(View.VISIBLE);
            manageSpaceListView.setVisibility(View.GONE);
            doneBtn.setVisibility(View.GONE);
        }
        else if(manageSpaceAdapter == null)
        {
            spaceManagerItems.add(0, getHeaderItem());
            manageSpaceAdapter = new ManageSpaceAdapter(ManageSpaceActivity.this, spaceManagerItems, this);
            manageSpaceListView.setAdapter(manageSpaceAdapter);
            doneBtn.setVisibility(View.VISIBLE);
        }
        else
        {
            manageSpaceAdapter.notifyDataSetChanged();
        }
    }
}

