package com.bsb.hike.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.FileListAdapter;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.FileListItem;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.tasks.InitiateMultiFileTransferTask;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class FileSelectActivity extends HikeAppStateBaseFragmentActivity implements OnScrollListener, HikePubSub.Listener
{

	public static abstract interface DocumentSelectActivityDelegate
	{
		public void didSelectFile(FileSelectActivity activity, String path, String name, String ext, long size);
	}

	private ListView listView;

	private FileListAdapter listAdapter;

	private File currentDir;

	private TextView emptyView;

	private ArrayList<FileListItem> items = new ArrayList<FileListItem>();

	private boolean receiverRegistered = false;

	private ArrayList<HistoryEntry> history = new ArrayList<HistoryEntry>();

	private long sizeLimit = HikeConstants.MAX_FILE_SIZE;

	public DocumentSelectActivityDelegate delegate;

	private boolean multiSelectMode;

	private String currentTitle;

	private volatile InitiateMultiFileTransferTask fileTransferTask;

	private ProgressDialog progressDialog;

	private class HistoryEntry
	{
		int scrollItem, scrollOffset;

		File dir;

		String title;
	}

	private BroadcastReceiver receiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			Runnable r = new Runnable()
			{
				public void run()
				{
					if (currentDir == null)
					{
						listRoots();
					}
					else
					{
						listFiles(currentDir);
					}
				}
			};
			if (Intent.ACTION_MEDIA_UNMOUNTED.equals(intent.getAction()))
			{
				listView.postDelayed(r, 1000);
			}
			else
			{
				r.run();
			}
		}
	};

	private int previousFirstVisibleItem;

	private int velocity;

	private long previousEventTime;

	private TextView title;

	private TextView multiSelectTitle;

	private TextView subText;
	
	@Override
	protected void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
		if(listAdapter != null)
		{
			listAdapter.getFileImageLoader().setExitTasksEarly(true);
		}
	}
	
	@Override
	public void onDestroy()
	{
		try
		{
			if (receiverRegistered)
			{
				unregisterReceiver(receiver);
			}
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Exception while unregistering receiver", e);
		}
		if (progressDialog != null)
		{
			progressDialog.dismiss();
			progressDialog = null;
		}
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.MULTI_FILE_TASK_FINISHED, this);
		super.onDestroy();
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (!receiverRegistered)
		{
			receiverRegistered = true;
			IntentFilter filter = new IntentFilter();
			filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
			filter.addAction(Intent.ACTION_MEDIA_CHECKING);
			filter.addAction(Intent.ACTION_MEDIA_EJECT);
			filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
			filter.addAction(Intent.ACTION_MEDIA_NOFS);
			filter.addAction(Intent.ACTION_MEDIA_REMOVED);
			filter.addAction(Intent.ACTION_MEDIA_SHARED);
			filter.addAction(Intent.ACTION_MEDIA_UNMOUNTABLE);
			filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
			filter.addDataScheme("file");
			registerReceiver(receiver, filter);
		}

		setContentView(R.layout.file_select_layout);

		Object object = getLastCustomNonConfigurationInstance();

		if (object instanceof InitiateMultiFileTransferTask)
		{
			fileTransferTask = (InitiateMultiFileTransferTask) object;
			progressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.multi_file_creation));
		}

		listAdapter = new FileListAdapter(this, items);
		emptyView = (TextView) findViewById(R.id.search_empty_view);
		listView = (ListView) findViewById(R.id.file_list);
		listView.setEmptyView(emptyView);
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
			{
				Logger.d("FileSelect", "Selected item.");
				FileListItem item = items.get(i);
				File file = item.getFile();
				if (multiSelectMode)
				{
					if (!file.isDirectory())
					{
						if(file.length() == 0)
						{
							Toast.makeText(FileSelectActivity.this, R.string.cannot_select_zero_byte_file, Toast.LENGTH_SHORT).show();
							return;
						}
						if (listAdapter.isSelected(item))
						{
							listAdapter.setSelected(item, false);
							if (listAdapter.getSeletctedFileItems().isEmpty())
							{
								setupActionBar(currentTitle);
								multiSelectMode = false;
							}
							else
							{
								setMultiSelectTitle();
							}
						}
						else
						{
							if (listAdapter.getSeletctedFileItems().size() >= FileTransferManager.getInstance(FileSelectActivity.this).remainingTransfers())
							{
								Toast.makeText(FileSelectActivity.this,
										getString(R.string.max_num_files_reached, FileTransferManager.getInstance(FileSelectActivity.this).getTaskLimit()), Toast.LENGTH_SHORT)
										.show();
								return;
							}
							listAdapter.setSelected(item, true);
							setMultiSelectTitle();
						}
						listAdapter.notifyDataSetChanged();
					}
				}
				else
				{
					if (file.isDirectory())
					{
						HistoryEntry he = new HistoryEntry();
						he.scrollItem = listView.getFirstVisiblePosition();
						he.scrollOffset = listView.getChildAt(0).getTop();
						he.dir = currentDir;
						he.title = title.getText().toString();
						if (!listFiles(file))
						{
							return;
						}
						history.add(he);
						setTitle(item.getTitle());
						listView.setSelection(0);
					}
					else
					{
						Logger.d("FileSelect", "Process the slected file.");
						if (!file.canRead())
						{
							showErrorBox(getString(R.string.access_error));
							return;
						}
						if (sizeLimit != 0)
						{
							/*if (file.length() > sizeLimit)
							{
								Toast.makeText(FileSelectActivity.this, getString(R.string.max_file_size, Utils.formatFileSize(sizeLimit)), Toast.LENGTH_SHORT).show();
								return;
							}*/
						}
						if (file.length() == 0)
						{
							Logger.d("FileSelect", "File size is 0.");
							return;
						}
						Intent intent = getIntent();
						intent.putExtra(HikeConstants.Extras.FILE_PATH, file.getAbsolutePath());
						intent.putExtra(HikeConstants.Extras.FILE_TYPE, item.getMimeType());
						intent.putExtra(FTAnalyticEvents.FT_ATTACHEMENT_TYPE, FTAnalyticEvents.FILE_ATTACHEMENT);
						setResult(Activity.RESULT_OK, intent);
						finish();
					}
				}
			}
		});

		if (!getIntent().hasExtra("allowLongPress"))
		{
			listView.setOnItemLongClickListener(new OnItemLongClickListener()
		{

			@Override
			public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id)
			{
				FileListItem listItem = items.get(position);

				File file = listItem.getFile();

				if (file.isDirectory())
				{
					return false;
				}
				if (file.length() == 0)
				{
					Toast.makeText(FileSelectActivity.this, R.string.cannot_select_zero_byte_file, Toast.LENGTH_SHORT).show();
					return false;
				}
				else if (file.length() > sizeLimit)
				{
					Toast.makeText(FileSelectActivity.this, getString(R.string.max_file_size, Utils.formatFileSize(sizeLimit)), Toast.LENGTH_SHORT).show();
					return false;
				}

				if (!multiSelectMode)
				{
					multiSelectMode = true;
					setupMultiSelectActionBar();
				}

				if (listAdapter.getSeletctedFileItems().size() >= FileTransferManager.getInstance(FileSelectActivity.this).remainingTransfers())
				{
					Toast.makeText(FileSelectActivity.this,
							getString(R.string.max_num_files_reached, FileTransferManager.getInstance(FileSelectActivity.this).getTaskLimit()), Toast.LENGTH_SHORT)
							.show();
					return false;
				}

				listAdapter.setSelected(listItem, true);

				listAdapter.notifyDataSetChanged();

				setMultiSelectTitle();

				return true;
			}

		});
		}

		listRoots();
		setupActionBar(getString(R.string.select_file));

		HikeMessengerApp.getPubSub().addListener(HikePubSub.MULTI_FILE_TASK_FINISHED, this);
	}
		

	private void setMultiSelectTitle()
	{
		if (multiSelectTitle == null)
		{
			return;
		}
		multiSelectTitle.setText(getString(R.string.gallery_num_selected, listAdapter.getSeletctedFileItems().size()));
	}

	private void setupActionBar(String titleString)
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		title = (TextView) actionBarView.findViewById(R.id.title);
		subText = (TextView) actionBarView.findViewById(R.id.subtext);

		setTitle(titleString);
		currentTitle = titleString;

		actionBar.setCustomView(actionBarView);
		Toolbar parent = (Toolbar) actionBarView.getParent();
		parent.setContentInsetsAbsolute(0, 0);
	}

	private void setupMultiSelectActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.chat_theme_action_bar, null);

		View sendBtn = actionBarView.findViewById(R.id.done_container);
		View closeBtn = actionBarView.findViewById(R.id.close_action_mode);
		ViewGroup closeContainer = (ViewGroup) actionBarView.findViewById(R.id.close_container);

		multiSelectTitle = (TextView) actionBarView.findViewById(R.id.title);
		multiSelectTitle.setText(getString(R.string.gallery_num_selected, 1));

		sendBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
                final ArrayList<ComposeChatActivity.FileTransferData> ftDataList = new ArrayList<ComposeChatActivity.FileTransferData>(listAdapter.getSeletctedFileItems().size());

                final String msisdn = getIntent().getStringExtra(HikeConstants.Extras.MSISDN);
                final boolean onHike = getIntent().getBooleanExtra(HikeConstants.Extras.ON_HIKE, true);

                ArrayList<ContactInfo> list = new ArrayList<ContactInfo>();
                list.add(ContactManager.getInstance().getContact(msisdn));
				for (Entry<String, FileListItem> fileDetailEntry : listAdapter.getSeletctedFileItems().entrySet())
				{
					FileListItem listItem = fileDetailEntry.getValue();

					String filePath = listItem.getFile().getAbsolutePath();
					String fileType = listItem.getMimeType();

                    File file = new File(filePath);

                    //TODO remove duplicate fileType
                    ComposeChatActivity.FileTransferData fileTransferData = new ComposeChatActivity.FileTransferData(filePath, null, HikeFile.HikeFileType.fromString(fileType), fileType, false, -1, false, list, file);
                    ftDataList.add(fileTransferData);
				}
				if (msisdn == null)
				{
					throw new IllegalArgumentException("You are not sending msisdn, and yet you expect to send files ?");
				}

				Intent intent = IntentFactory.createChatThreadIntentFromMsisdn(FileSelectActivity.this, msisdn, false,false, ChatThreadActivity.ChatThreadOpenSources.FILE_SHARING);
				fileTransferTask = new InitiateMultiFileTransferTask(getApplicationContext(), ftDataList, msisdn, onHike, FTAnalyticEvents.FILE_ATTACHEMENT, intent);
				Utils.executeAsyncTask(fileTransferTask);

				progressDialog = ProgressDialog.show(FileSelectActivity.this, null, getResources().getString(R.string.multi_file_creation));
			}
		});

		closeContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});

		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);

		Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left_noalpha);
		slideIn.setInterpolator(new AccelerateDecelerateInterpolator());
		slideIn.setDuration(200);
		closeBtn.startAnimation(slideIn);
		sendBtn.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_in));
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		if (fileTransferTask != null)
		{
			return fileTransferTask;
		}
		else
		{
			return null;
		}
	}

	private void setTitle(String titleString)
	{
		title.setText(titleString);

		if (!history.isEmpty())
		{
			subText.setVisibility(View.VISIBLE);
			subText.setText(R.string.tap_hold_multi_select);
		}
		else
		{
			subText.setVisibility(View.GONE);
		}
	}

	@Override
	protected void onResume()
	{
		// TODO Auto-generated method stub
		super.onResume();
		if(listAdapter != null)
		{
			listAdapter.getFileImageLoader().setExitTasksEarly(false);
			listAdapter.notifyDataSetChanged();
		}
	}
	
	@Override
	public void onBackPressed()
	{
		if (multiSelectMode)
		{
			listAdapter.clearSelection();
			listAdapter.notifyDataSetChanged();

			setupActionBar(currentTitle);
			multiSelectMode = false;
		}
		else if (history.size() > 0)
		{
			HistoryEntry he = history.remove(history.size() - 1);
			setTitle(he.title);
			if (he.dir != null)
			{
				listFiles(he.dir);
			}
			else
			{
				listRoots();
			}
			listView.setSelectionFromTop(he.scrollItem, he.scrollOffset);
		}
		else
		{
			super.onBackPressed();
		}
	}

	private boolean listFiles(File dir)
	{
		if (!dir.canRead())
		{
			if (dir.getAbsolutePath().startsWith(Environment.getExternalStorageDirectory().toString()) || dir.getAbsolutePath().startsWith("/sdcard")
					|| dir.getAbsolutePath().startsWith("/mnt/sdcard"))
			{
				if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY))
				{
					currentDir = dir;
					items.clear();
					String state = Environment.getExternalStorageState();
					if (Environment.MEDIA_SHARED.equals(state))
					{
						emptyView.setText(R.string.usb_active);
					}
					else
					{
						emptyView.setText(R.string.not_mounted);
					}
					listAdapter.notifyDataSetChanged();
					return true;
				}
			}
			showErrorBox(getString(R.string.access_error));
			return false;
		}
		emptyView.setText(R.string.no_files);
		File[] files = null;
		try
		{
			files = dir.listFiles();
		}
		catch (Exception e)
		{
			showErrorBox(e.getLocalizedMessage());
			return false;
		}
		if (files == null)
		{
			showErrorBox(getString(R.string.unknown_error));
			return false;
		}
		currentDir = dir;
		items.clear();
		Arrays.sort(files, new Comparator<File>()
		{
			@Override
			public int compare(File lhs, File rhs)
			{
				if (lhs.isDirectory() != rhs.isDirectory())
				{
					return lhs.isDirectory() ? -1 : 1;
				}
				return lhs.getName().compareToIgnoreCase(rhs.getName());
			}
		});
		for (File file : files)
		{
			if (file.getName().startsWith("."))
			{
				continue;
			}
			FileListItem item = new FileListItem();
			item.setListItemAttributesFromFile(item, file);
			items.add(item);
		}
		listAdapter.notifyDataSetChanged();
		return true;
	}

	private void showErrorBox(String error)
	{
		new AlertDialog.Builder(this).setTitle(R.string.app_name).setMessage(error).setPositiveButton(R.string.OK, null).show();
	}

	private void listRoots()
	{
		currentDir = null;
		items.clear();
		String extStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
		try {
			FileListItem ext = new FileListItem();
			ext.setTitle(getString(!Utils.hasGingerbread() || Environment.isExternalStorageRemovable() ? R.string.sd_card : R.string.internal_storage));
			ext.setIcon(R.drawable.ic_folder);
			ext.setSubtitle(getRootSubtitle(extStorage));
			ext.setFile(Environment.getExternalStorageDirectory());
			items.add(ext);
		}
		catch(Exception e) {
			Logger.e(getClass().getSimpleName(), "Exception while showing root", e);
		}
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader("/proc/mounts"));
			String line;
			HashMap<String, ArrayList<String>> aliases = new HashMap<String, ArrayList<String>>();
			ArrayList<String> result = new ArrayList<String>();
			String extDevice = null;
			while ((line = reader.readLine()) != null)
			{
				if ((!line.contains("/mnt") && !line.contains("/storage") && !line.contains("/sdcard")) || line.contains("asec") || line.contains("tmpfs") || line.contains("none"))
				{
					continue;
				}
				String[] info = line.split(" ");
				if (!aliases.containsKey(info[0]))
				{
					aliases.put(info[0], new ArrayList<String>());
				}
				aliases.get(info[0]).add(info[1]);
				if (info[1].equals(extStorage))
				{
					extDevice = info[0];
				}
				result.add(info[1]);
			}
			reader.close();
			if (extDevice != null)
			{
				result.removeAll(aliases.get(extDevice));
				for (String path : result)
				{
					try
					{
						boolean isSd = path.toLowerCase().contains("sd");
						FileListItem item = new FileListItem();
						item.setTitle(getString(isSd ? R.string.sd_card : R.string.external_storage));
						item.setIcon(R.drawable.ic_folder);
						item.setSubtitle(getRootSubtitle(path));
						item.setFile(new File(path));
						items.add(item);
					}
					catch (Exception e)
					{
						Logger.e(getClass().getSimpleName(), "Exception while showing root", e);
					}
				}
			}
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Exception while showing root", e);
		}
		FileListItem fs = new FileListItem();
		fs.setTitle("/");
		fs.setSubtitle(getString(R.string.system_root));
		fs.setIcon( R.drawable.ic_folder);
		fs.setFile(new File("/"));
		items.add(fs);
		listAdapter.notifyDataSetChanged();
	}

	private String getRootSubtitle(String path)
	{
		StatFs stat = new StatFs(path);
		long total = (long) stat.getBlockCount() * (long) stat.getBlockSize();
		long free = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
		if (total == 0)
		{
			return "";
		}
		return getString(R.string.free_of_total, Utils.formatFileSize(free), Utils.formatFileSize(total));
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		if (previousFirstVisibleItem != firstVisibleItem)
		{
			long currTime = System.currentTimeMillis();
			long timeToScrollOneElement = currTime - previousEventTime;
			velocity = (int) (((double) 1 / timeToScrollOneElement) * 1000);

			previousFirstVisibleItem = firstVisibleItem;
			previousEventTime = currTime;
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
		listAdapter.setIsListFlinging(velocity > HikeConstants.MAX_VELOCITY_FOR_LOADING_IMAGES && scrollState == OnScrollListener.SCROLL_STATE_FLING);
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		super.onEventReceived(type, object);

		if (HikePubSub.MULTI_FILE_TASK_FINISHED.equals(type))
		{
			fileTransferTask = null;
			
			final Intent intent = (Intent) object;

			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					startActivity(intent);

					if (progressDialog != null)
					{
						progressDialog.dismiss();
						progressDialog = null;
					}
				}
			});
		}
	}
}
