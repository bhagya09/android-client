package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.provider.UserDictionary.Words;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.GridView;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ApkExplorerListAdapter;
import com.bsb.hike.offline.OfflineConstants;
import com.bsb.hike.utils.OneToNConversationUtils;
public class ApkSelectionActivity extends SherlockActivity implements OnItemClickListener  {
	ArrayList<ApplicationSelectionStatus>  apkInfo ;
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
		List<PackageInfo> packageInfos =  getPackageManager().getInstalledPackages(0);
  		
		for(int i=0;i<packageInfos.size();i++)
		{
			   PackageInfo packageInfo =  packageInfos.get(i);
			   if(isUserApp(packageInfo.applicationInfo))
			   {
			   		apkInfo.add(new ApplicationSelectionStatus(packageInfo.applicationInfo,false));      
			   }
		}
		Collections.sort(apkInfo, new Comparator<ApplicationSelectionStatus>() { 
	  	       @Override 
	  	       public int compare(ApplicationSelectionStatus applicationSelectionStatus1, ApplicationSelectionStatus applicationSelectionStatus2) {
	  	           String applicationLabel1  = (String)getPackageManager().getApplicationLabel(applicationSelectionStatus1.getApplicationInfo());
	  	           String applicationLabel2  = (String)getPackageManager().getApplicationLabel(applicationSelectionStatus2.getApplicationInfo());
	  	           return applicationLabel1.compareToIgnoreCase(applicationLabel2);
	  	       }
		});
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
	private void removeSelection() {
		for(int i=0;i<apkInfo.size();i++)
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
				SparseBooleanArray checked = list.getCheckedItemPositions(); 
				int numChecked = checked.size();
				ArrayList<ApplicationInfo> results  =  new ArrayList<ApplicationInfo>();
				for (int i = 0; numChecked > 0; i++){
				    if (checked.get(i)){  
				    	results.add(apkInfo.get(i).appInfo);
				        numChecked--; 
				    }      
				} 
				Intent intent =  getIntent();
				intent.putParcelableArrayListExtra(OfflineConstants.APK_SELECTION_RESULTS, results);
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
    public void toggleSelection(int position) {
		if(apkInfo.get(position).getApplicationSelectionStatus())
		{
			apkInfo.get(position).setApplicationSelectionStatus(false);
     		decrementSelectedItems();
		}
		else
		{
			apkInfo.get(position).setApplicationSelectionStatus(true);
    		incrementSelectedItems();
		}
		apkAdapter.notifyDataSetChanged();
	}
	
	private boolean isUserApp(ApplicationInfo applicationInfo) 
	{
		   int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
		   return (applicationInfo.flags & mask) == 0;
    }
	
	public  class ApplicationSelectionStatus{
		ApplicationInfo appInfo;
		Boolean status; /* false - checkbox disable, true - checkbox enable */

		ApplicationSelectionStatus(ApplicationInfo appInfo,Boolean status )
		{
			this.appInfo =  appInfo;
			this.status =  status;
		}
		public ApplicationInfo getApplicationInfo(){
			return this.appInfo;
		}
		public Boolean getApplicationSelectionStatus(){
			return this.status;
		}
		
		public void setApplicationSelectionStatus(Boolean status){
			this.status =  status;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		toggleSelection(position);
		
	}
//	private void setupMultiSelectActionBar()
//	{
//		ActionBar actionBar = getSupportActionBar();
//		actionBar.setDisplayOptions(ActionBar.);
//		if (multiSelectActionBar == null)
//		{
//			multiSelectActionBar = LayoutInflater.from(this).inflate(R.layout., null);
//		}
//		View sendBtn = multiSelectActionBar.findViewById(R.id.done_container);
//		TextView save = (TextView) multiSelectActionBar.findViewById(R.id.save);
//
//	}
}




