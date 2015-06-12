package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.GridView;

import com.bsb.hike.R;
import com.bsb.hike.adapters.ApkExplorerGridAdapter;
import com.bsb.hike.offline.OfflineConstants;
public class ApkSelectionActivity extends Activity {
	ArrayList<ApplicationInfo>  apkInfo;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.apk_explorer);
		
		List<PackageInfo> pi =  getPackageManager().getInstalledPackages(0);
		apkInfo = new ArrayList<ApplicationInfo>();
		//ArrayList<String> pnames =  new ArrayList<String>();
		//ArrayList<Drawable> drawables = new ArrayList<Drawable>();
  		for(int i=0;i<pi.size();i++)
		{
			   PackageInfo p1 =  pi.get(i);
			   if(isUserApp(p1.applicationInfo))
			   {
			   		apkInfo.add(p1.applicationInfo);      
			   }
		}
  		ApkExplorerGridAdapter apkAdapter = new ApkExplorerGridAdapter(this, apkInfo);
  		GridView grid=(GridView)findViewById(R.id.grid);
  		grid.setAdapter(apkAdapter);
  		grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				String uri = null;
				ApplicationInfo appDetails = apkInfo.get(position);
				uri = apkInfo.get(position).sourceDir;
				Intent intent = getIntent();
				intent.putExtra(OfflineConstants.EXTRAS_APK_PATH, uri);
				intent.putExtra(OfflineConstants.FILE_TYPE,MimeTypeMap.getSingleton().
						getMimeTypeFromExtension(com.bsb.hike.utils.Utils.getFileExtension(uri)));
				intent.putExtra(OfflineConstants.EXTRAS_APK_NAME,getPackageManager().getApplicationLabel(apkInfo.get(position)));
				setResult(RESULT_OK, intent);
			    finish();
			}
  		});
	}
	
	private boolean isUserApp(ApplicationInfo ai) {
		   int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
		   return (ai.flags & mask) == 0;
		}
}
