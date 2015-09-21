package com.kpt.adaptxt.beta.settings;

import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngine;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceHandler;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceListener;

/**
 * @author nikhil joshi
 * 
 */
public class KPTATRListview extends Activity implements KPTCoreServiceListener,
		Runnable {

	/**
	 * Core interface handle
	 */
	private KPTCoreEngine mCoreEngine = null;

	/**
	 * Service handler to initiate and obtain core interface handle
	 */
	private KPTCoreServiceHandler mCoreServiceHandler = null;

	/**
	 * ListView instance to display ATR shortcuts and expansions
	 */
	private ListView atrListView;

	/**
	 * Adapter to display the view in ListView
	 */
	private ATRListAdapter mAtrListAdapter;

	/**
	 * ArrayList for shortcut and expansion
	 */
	private ArrayList<String> mShortcutList = null;
	private ArrayList<String> mExpansionList = null;

	/**
	 * Async task to get the list items from Core and preparing UI
	 */
	Task mBackgroundTask;

	/**
	 * ProgressDialogs while running Async task and DeleteAll actions
	 */
	private ProgressDialog loading;
	private ProgressDialog mPdDeleteAll;

	private TextView mAddATRShortcut;
	private TextView mDelATRShortcut;

	private ContextThemeWrapper mContextWrapper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.kpt_atr_wordslist);
		mContextWrapper = new ContextThemeWrapper(this, R.style.AdaptxtTheme); 

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (mCoreEngine != null) {
			mCoreEngine.saveUserContext();
			mCoreEngine.destroyCore();
		}

		if (mCoreServiceHandler != null) {
			mCoreServiceHandler.unregisterCallback(this);
			mCoreServiceHandler.destroyCoreService();
		}
		mCoreEngine = null;
		mCoreServiceHandler = null;
	}

	public class Task extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub

			atrListView = (ListView) findViewById(R.id.listView);
			atrListView.setAdapter(mAtrListAdapter);

			atrListView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> adapterView, View view,
						final int position, long arg3) {

					new AlertDialog.Builder(KPTATRListview.this)
							.setIcon(
									getResources().getDrawable(
											R.drawable.kpt_adaptxt_launcher_icon))
							.setTitle(
									getResources()
											.getText(
													R.string.kpt_UI_STRING_POPUP_ITEMS_6_7003))
							.setPositiveButton(R.string.kpt_alert_dialog_ok,
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int whichButton) {

											if (position >= 0) {
												try {
													boolean delCompl = mCoreEngine
															.removeATRShortcutAndExpansion(mShortcutList
																	.get(position));

													if (delCompl) {
														mShortcutList
																.remove(position);
														mExpansionList
																.remove(position);
														mAtrListAdapter
																.notifyDataSetChanged();
													}
												} catch (Exception e) {
												}
											}

											dialog.dismiss();

										}
									}).create().show();

				}
			});

			if (loading != null || loading.isShowing())
				loading.dismiss();
			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub

			loading = ProgressDialog.show(KPTATRListview.this, getResources()
					.getString(R.string.kpt_title_wait),
					getResources().getString(R.string.kpt_loading_data));
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub

			if (mCoreServiceHandler == null) {
				mCoreServiceHandler = KPTCoreServiceHandler
						.getCoreServiceInstance(getApplicationContext());
				mCoreEngine = mCoreServiceHandler.getCoreInterface();

				if (mCoreEngine != null) {
					mCoreEngine.initializeCore(KPTATRListview.this);
				}
			}

			if (mCoreEngine == null) {
				mCoreServiceHandler.registerCallback(KPTATRListview.this);
			} else {
				populateUI();
			}
			return null;
		}

	}

	private void populateUI() {

		mShortcutList.clear();
		mExpansionList.clear();

		mShortcutList = mCoreEngine.getATRShortcuts(); // Retrieving shortcuts
														// from Core
		Collections.sort(mShortcutList);
		mExpansionList = mCoreEngine.getATRExpansions(); // Retrieving
															// expansions from
															// Core

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		// return super.onCreateOptionsMenu(menu);
		// MenuInflater inflater = getMenuInflater();
		// inflater.inflate(R.menu.atr_menu, menu);
		// return true;
		menu.add(0, 0, 0, R.string.kpt_UI_STRING_MENUKEY_ITEMS_1_7003);
		menu.add(0, 1, 0, R.string.kpt_UI_STRING_MENUKEY_ITEMS_2_7003);

		menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.kpt_ic_add));
		menu.getItem(1).setIcon(
				getResources().getDrawable(android.R.drawable.ic_menu_delete));

		// MenuItem addItem = menu.add("Add Button");
		// addItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		// addItem.setIcon(android.R.drawable.ic_menu_add);
		//
		// MenuItem deleteItem = menu.add("Delete All Button");
		// deleteItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		// deleteItem.setIcon(android.R.drawable.ic_menu_delete);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == 0) {

			Intent intent = new Intent(KPTATRListview.this,
					KPTAddATRShortcut.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_CLEAR_TOP);

			startActivity(intent);
		} else if (item.getItemId() == 1) {
			createDialog();
		}
		return true;
	}

	private void createDialog() {

		// ContextThemeWrapper ctw = new ContextThemeWrapper(this,
		// R.style.MyTheme);

		AlertDialog.Builder alertDialog = new AlertDialog.Builder(
				mContextWrapper);
		alertDialog.setTitle(getResources().getText(
				R.string.kpt_mydict_dialog_del_title));
		alertDialog.setMessage(getResources().getText(
				R.string.kpt_UI_STRING_DIALOG_MSG_8_7003));
		alertDialog.setPositiveButton(R.string.kpt_alert_dialog_ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

						if (mShortcutList.size() > 0) {

							dialog.dismiss();
							String str = String.format(getResources()
									.getString(R.string.kpt_dictionary_msg),
									(mShortcutList.size()));
							mPdDeleteAll = ProgressDialog.show(
									KPTATRListview.this, getResources()
											.getText(R.string.kpt_title_wait), str);

							Thread thread = new Thread(KPTATRListview.this);
							thread.start();
						}
					}
				});
		alertDialog.setNegativeButton(R.string.kpt_alert_dialog_cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.dismiss();
					}
				});
		alertDialog.create().show();

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);

		menu.add(R.string.kpt_UI_STRING_POPUP_ITEMS_6_7003);

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// TODO Auto-generated method stub

		if (item.getItemId() == 0) {

		}

		return super.onContextItemSelected(item);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();


		mAddATRShortcut = (TextView) findViewById(R.id.addshortcut);
		mDelATRShortcut = (TextView) findViewById(R.id.delshortcut);

		mAddATRShortcut.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				Intent intent = new Intent(KPTATRListview.this,
						KPTAddATRShortcut.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
						| Intent.FLAG_ACTIVITY_CLEAR_TOP);

				startActivity(intent);

			}
		});

		mDelATRShortcut.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				createDialog();
			}
		});

		mAtrListAdapter = new ATRListAdapter(this);
		mShortcutList = new ArrayList<String>();
		mExpansionList = new ArrayList<String>();

		if (mBackgroundTask == null
				|| mBackgroundTask.getStatus() == AsyncTask.Status.FINISHED) {
			Log.i("", "Calling Async Task");
			mBackgroundTask = new Task();
			mBackgroundTask.execute();
		} else {
			populateUI();
		}

	}

	private class ATRListAdapter extends BaseAdapter {

		private LayoutInflater mInflater;
		private Bitmap mDelIcon;

		public ATRListAdapter(Context context) {
			mInflater = LayoutInflater.from(context);

			mDelIcon = BitmapFactory.decodeResource(context.getResources(),
					android.R.drawable.ic_menu_delete);
		}

		public int getCount() {
			return mShortcutList.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		@Override
		public boolean isEnabled(int position) {
			// TODO Auto-generated method stub
			return super.isEnabled(position);
		}

		/**
		 * Make a view to hold each row.
		 * 
		 * @see android.widget.ListAdapter#getView(int, android.view.View,
		 *      android.view.ViewGroup)
		 */
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			final ViewHolder holder;

			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.kpt_atr_words_list, null);

				// Creates a ViewHolder and store references to the two children
				// views
				// we want to bind data to.
				holder = new ViewHolder();

				holder.mText = (TextView) convertView.findViewById(R.id.text);
				holder.mText1 = (TextView) convertView.findViewById(R.id.text0);
				holder.mIcon = (ImageView) convertView.findViewById(R.id.icon);
				convertView.setTag(holder);
			} else {
				// Get the ViewHolder back to get fast access to the TextView
				// and the ImageView.
				holder = (ViewHolder) convertView.getTag();
			}

			// Bind the data efficiently with the holder.
			convertView.setPadding(0, 5, 0, 5);

			switch (position) {
			default:
				if (mShortcutList.size() > 0 && mExpansionList.size() > 0) {
					holder.mText.setTextColor(Color.WHITE);
					holder.mText1.setTextColor(Color.WHITE);

					holder.mText.setText(mShortcutList.get(position));
					holder.mText1.setText(mExpansionList.get(position));
					holder.mIcon.setImageBitmap(mDelIcon);
				}
				break;
			}
			holder.mIcon.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (position >= 0) {

						boolean delCompl = mCoreEngine
								.removeATRShortcutAndExpansion(mShortcutList
										.get(position));

						if (delCompl) {
							mShortcutList.remove(position);
							mExpansionList.remove(position);
							notifyDataSetChanged();
						}
					}
				}
			});

			return convertView;
		}

		class ViewHolder {
			TextView mText;
			TextView mText1;
			ImageView mIcon;
		}

	}

	@Override
	public void serviceConnected(KPTCoreEngine coreEngine) {
		// TODO Auto-generated method stub

		mCoreEngine = coreEngine;
		mAtrListAdapter = new ATRListAdapter(this);

		if (mCoreEngine != null) {
			mCoreEngine.initializeCore(this);

			if (mBackgroundTask == null
					|| mBackgroundTask.getStatus() == AsyncTask.Status.FINISHED) {
				mBackgroundTask = new Task();
				mBackgroundTask.execute();
			} else {
				populateUI();
			}
		}
	}

	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (mPdDeleteAll != null) {
				if (mAtrListAdapter != null) {
					mAtrListAdapter.notifyDataSetChanged();
				}
				mPdDeleteAll.dismiss();
			}
		}
	};

	@Override
	public void run() {
		// TODO Auto-generated method stub

		if (mShortcutList.size() > 0) {
			if (mCoreEngine.removeAllATRShortcutAndExpansion(mShortcutList)) {
				mShortcutList.clear();
			}
		}
		mHandler.sendEmptyMessage(0);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
			/*
			 * Intent intent = new Intent(KPTATRListview.this,
			 * KPTAdaptxtIMESettings.class);
			 * intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			 * startActivity(intent);
			 */

		}
		return super.onKeyDown(keyCode, event);
	}

}
