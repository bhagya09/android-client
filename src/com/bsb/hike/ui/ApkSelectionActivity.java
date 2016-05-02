package com.bsb.hike.ui;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.adapters.ApkExplorerListAdapter;
import com.bsb.hike.offline.OfflineConstants;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;

import java.util.*;

public class ApkSelectionActivity extends HikeAppStateBaseFragmentActivity implements OnItemClickListener  {
	ArrayList<ApplicationSelectionStatus>  apkInfo ;
	Map<ApplicationInfo,Boolean> selectedApplications;
    ListView list;
    CheckBox  checkBox;
    ApkExplorerListAdapter apkAdapter ;
	View apkActionBar;
	TextView title;
	boolean showingMultiSelectActionBar;
	int selectedItems = 0;
	View apkSelectedActionBar;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.apk_explorer);
		init();
	}
	
	private void init() {
		
		apkInfo =  new ArrayList<ApplicationSelectionStatus>();
		selectedApplications = new HashMap<ApplicationInfo,Boolean>();
		List<PackageInfo> packageInfos =  getPackageManager().getInstalledPackages(0);
  		
		if (packageInfos != null && !packageInfos.isEmpty())
		{
			for (int i = 0; i < packageInfos.size(); i++)
			{
				PackageInfo packageInfo = packageInfos.get(i);
				if (isUserApp(packageInfo.applicationInfo))
				{
					apkInfo.add(new ApplicationSelectionStatus(packageInfo.applicationInfo, false));
				}
			}
		}

		if (apkInfo.size() > 0)
		{
			Collections.sort(apkInfo, new Comparator<ApplicationSelectionStatus>()
			{
				@Override
				public int compare(ApplicationSelectionStatus applicationSelectionStatus1, ApplicationSelectionStatus applicationSelectionStatus2)
				{
					String applicationLabel1 = (String) getPackageManager().getApplicationLabel(applicationSelectionStatus1.getApplicationInfo());
					String applicationLabel2 = (String) getPackageManager().getApplicationLabel(applicationSelectionStatus2.getApplicationInfo());
					return applicationLabel1.compareToIgnoreCase(applicationLabel2);
				}
			});
		}

  		list = (ListView)findViewById(R.id.apk_list);	
  		apkAdapter = new ApkExplorerListAdapter(this,apkInfo);
  		list.setAdapter(apkAdapter);
  		list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
  		list.setOnItemClickListener(this);
  		setActionBar();

	}
	
	private void setActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		if (apkActionBar == null)
		{
			apkActionBar = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);
		}

		if (actionBar.getCustomView() == apkActionBar)
		{
			return;
		}

		View backContainer = apkActionBar.findViewById(R.id.back);

		title = (TextView) apkActionBar.findViewById(R.id.title);
		apkActionBar.findViewById(R.id.seprator).setVisibility(View.GONE);
		
		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{

				onBackPressed();
			}
		});
		
		title.setText(R.string.share_application);
		actionBar.setCustomView(apkActionBar);

		showingMultiSelectActionBar = false;
		removeSelection();
	}

	private void removeSelection()
	{
		for (int i = 0; i < apkInfo.size(); i++)
		{
			apkInfo.get(i).status = false;
		}
		apkAdapter.notifyDataSetChanged();
	}

	private void setupMultiSelectActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		if (apkSelectedActionBar == null)
		{
			apkSelectedActionBar = LayoutInflater.from(this).inflate(R.layout.chat_theme_action_bar, null);
		}
		View sendBtn = apkSelectedActionBar.findViewById(R.id.done_container);
		TextView save = (TextView) apkSelectedActionBar.findViewById(R.id.save);
		View closeBtn = apkSelectedActionBar.findViewById(R.id.close_action_mode);
		ViewGroup closeContainer = (ViewGroup) apkSelectedActionBar.findViewById(R.id.close_container);

		title = (TextView) apkSelectedActionBar.findViewById(R.id.title);
		
		title.setText(getString(R.string.gallery_num_selected, selectedItems));
		
		TextView send = (TextView) apkSelectedActionBar.findViewById(R.id.save);
		send.setText(R.string.send);
		

		sendBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				ArrayList<ApplicationInfo> apkList = new ArrayList<ApplicationInfo>();
				for (Map.Entry<ApplicationInfo, Boolean> map : selectedApplications.entrySet())
				{
					apkList.add(map.getKey());
				}
				Intent intent = getIntent();
				intent.putParcelableArrayListExtra(OfflineConstants.APK_SELECTION_RESULTS, apkList);
				setResult(RESULT_OK, intent);
				finish();
			}
		});

		closeContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				setActionBar();
				
				invalidateOptionsMenu();
			}
		});

		actionBar.setCustomView(apkSelectedActionBar);
		showingMultiSelectActionBar = true;
	}
	
	
    public void incrementSelectedItems()
    {
    	selectedItems++;
    	setupMultiSelectActionBar();
    }
    
    public void decrementSelectedItems()
    {
    	selectedItems--;
    	if(selectedItems==0)
    		setActionBar();
    	else
    		setupMultiSelectActionBar();
    }

	public void toggleSelection(int position)
	{

		ApplicationSelectionStatus applicationStatus = apkInfo.get(position);
		if (applicationStatus.getApplicationSelectionStatus())
		{
			applicationStatus.setApplicationSelectionStatus(false);
			selectedApplications.remove(applicationStatus.appInfo);
			decrementSelectedItems();
		}
		else
		{
			applicationStatus.setApplicationSelectionStatus(true);
			selectedApplications.put(applicationStatus.appInfo, true);
			incrementSelectedItems();
		}
		apkAdapter.notifyDataSetChanged();
	}
	
	private boolean isUserApp(ApplicationInfo applicationInfo) 
	{
		   int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
		   return (applicationInfo.flags & mask) == 0;
    }
	
	public class ApplicationSelectionStatus
	{
		ApplicationInfo appInfo;

		Boolean status; /* false - checkbox disable, true - checkbox enable */

		ApplicationSelectionStatus(ApplicationInfo appInfo, Boolean status)
		{
			this.appInfo = appInfo;
			this.status = status;
		}

		public ApplicationInfo getApplicationInfo()
		{
			return this.appInfo;
		}

		public Boolean getApplicationSelectionStatus()
		{
			return this.status;
		}

		public void setApplicationSelectionStatus(Boolean status)
		{
			this.status = status;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		toggleSelection(position);

	}
	
}




