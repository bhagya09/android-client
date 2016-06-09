package com.bsb.hike.spaceManager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.widget.LinearLayout;
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
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.List;


public class ManageSpaceActivity extends HikeAppStateBaseFragmentActivity implements HikePubSub.UiListener,
        View.OnClickListener, ManageSpaceAdapter.IDeleteButtonToggleListener, DeleteAccountTask.DeleteAccountListener
{
    private static final String TAG = "ManageSpaceActivity";

    private List<SpaceManagerItem> spaceManagerItems;

    private List<CategoryItem> categoryList;

    private RecyclerView manageSpaceListView;

    private ManageSpaceAdapter manageSpaceAdapter;

    private LinearLayoutManager mLayoutManager;

    private ViewStub emptyLayoutViewStub;

    private ProgressDialog deleteProgressDialog;

    private LinearLayout progressBarLayout;

    private View doneBtn;

    private String[] uiPubSubTypes = {HikePubSub.SPACE_MANAGER_ITEMS_FETCH_SUCCESS, HikePubSub.SPACE_MANAGER_ITEMS_FETCH_FAIL, HikePubSub.SPACE_MANAGER_ITEMS_DELETE_SUCCESS, HikePubSub.SPACE_MANAGER_ITEMS_DELETE_FAIL};

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        long start = System.currentTimeMillis();

        recordScreenopenSource(getIntent());

        if(!Utils.isUserAuthenticated(ManageSpaceActivity.this))
        {
            accountDeleted();
        }
        if(SpaceManagerUtils.isSpaceManagerEnabled())
        {
            setContentView(R.layout.space_manager_main);
            manageSpaceListView = (RecyclerView) findViewById(R.id.smRecycleView);
            mLayoutManager = new LinearLayoutManager(ManageSpaceActivity.this);
            manageSpaceListView.setLayoutManager(mLayoutManager);
            emptyLayoutViewStub = (ViewStub)findViewById(R.id.stub_sm_emptyView);
            progressBarLayout = (LinearLayout)findViewById(R.id.progress_container);
            init();
        }
        else
        {
            loadFallbackView();
        }

        setupActionBar();

        Logger.d(TAG, "time taken: " + (System.currentTimeMillis() - start));
    }

    private void loadFallbackView()
    {
        setContentView(R.layout.space_manager_fallback_layout);
        findViewById(R.id.delete_button).setOnClickListener(this);
    }

    private void init()
    {
        progressBarLayout.setVisibility(View.VISIBLE);
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
        Logger.d(TAG, "received invalid list or other failure");
        progressBarLayout.setVisibility(View.GONE);

        // Default json string is corrupted, Log here or close Activity
        this.finish();
    }

    private void setupActionBar()
    {
        ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

        View actionBarView = LayoutInflater.from(this).inflate(R.layout.space_manager_action_bar, null);
        actionBar.setCustomView(actionBarView);

        doneBtn = actionBarView.findViewById(R.id.done_container);
        doneBtn.setOnClickListener(this);

        Toolbar parent=(Toolbar)actionBarView.getParent();
        parent.setContentInsetsAbsolute(0,0);
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
                progressBarLayout.setVisibility(View.GONE);
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
            Toast.makeText(ManageSpaceActivity.this, getString(R.string.space_successfully_deleted), Toast.LENGTH_SHORT).show();
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

            case R.id.delete_button:
                handleFallbackButtonClicked();
                break;
        }
    }

    private void handleFallbackButtonClicked()
    {
        HikeDialogFactory.showDialog(ManageSpaceActivity.this, HikeDialogFactory.SPACE_MANAGER_FALLBACK_DELETE_CONFIRMATION_DIALOG, new HikeDialogListener()
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
                
                //TODO start here DeleteAccount TASK
                DeleteAccountTask task = new DeleteAccountTask(ManageSpaceActivity.this, false, getApplicationContext());
                deleteProgressDialog = ProgressDialog.show(ManageSpaceActivity.this, getResources().getString(R.string.sm_fallback_header), getResources().getString(R.string.delete_space_fallback_loader_text));
                task.execute();
            }

            @Override
            public void neutralClicked(HikeDialog hikeDialog)
            {

            }
        });
    }

    private void handleDeletButtonClicked()
    {
        long size = SpaceManagerUtils.getTotalSizeToDelete(spaceManagerItems);

        HikeDialogFactory.showDialog(ManageSpaceActivity.this, HikeDialogFactory.SPACE_MANAGER_DELETE_CONFIRMATION_DIALOG, new HikeDialogListener()
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
        doneBtn.setVisibility(enable ? View.VISIBLE : View.GONE);
    }

    public HeaderItem getHeaderItem()
    {
        return new HeaderItem(getResources().getString(R.string.sm_clear_all_header) ,getResources().getString(R.string.sm_clear_all_subheader));
    }

    private void updateUI()
    {
        if(spaceManagerItems.isEmpty())
        {
            emptyLayoutViewStub.inflate();
            emptyLayoutViewStub.setVisibility(View.VISIBLE);
            manageSpaceListView.setVisibility(View.GONE);
            doneBtn.setVisibility(View.GONE);
        }
        else if(manageSpaceAdapter == null)
        {
            spaceManagerItems.add(0, getHeaderItem());
            manageSpaceAdapter = new ManageSpaceAdapter(ManageSpaceActivity.this, spaceManagerItems, this);
            manageSpaceListView.setAdapter(manageSpaceAdapter);
        }
        else
        {
            manageSpaceAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void accountDeleted(final boolean isSuccess)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                if (isSuccess)
                {
                    accountDeleted();
                }
                else
                {
                    dismissProgressDialog();
                    int duration = Toast.LENGTH_LONG;
                    Toast toast = Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.unlink_account_failed), duration);
                    toast.show();
                }
            }
        });
    }

    public void accountDeleted()
    {
        dismissProgressDialog();
		/*
		 * First we send the user to the Main Activity(MessagesList) from there we redirect him to the welcome screen.
		 */
        Intent dltIntent = new Intent(this, HomeActivity.class);
        dltIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(dltIntent);
        this.finish();
    }
}

