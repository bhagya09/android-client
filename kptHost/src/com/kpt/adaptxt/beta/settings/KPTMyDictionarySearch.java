package com.kpt.adaptxt.beta.settings;

import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreEngine;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceHandler;
import com.kpt.adaptxt.beta.core.coreservice.KPTCoreServiceListener;

public class KPTMyDictionarySearch extends Activity implements
		KPTCoreServiceListener {

	public ProgressDialog loading;
	public MyDictionaryAdapter mDictAdapter;
	public FilterAdapter filterAdapter;
	private ListView lv;
	private EditText et;

	private ArrayList<String> array_sort = new ArrayList<String>();
	int textlength = 0;

	public ArrayList<String> mWordList = null;

	/**
	 * Core interface handle
	 */
	private KPTCoreEngine mCoreEngine = null;

	/**
	 * Service handler to initiate and obtain core interface handle
	 */
	private KPTCoreServiceHandler mCoreServiceHandler = null;
	private Bundle bundle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.kpt_search);
		lv = (ListView) findViewById(R.id.listView);

		// TODO Auto-generated method stub
		mWordList = new ArrayList<String>();
		mDictAdapter = new MyDictionaryAdapter(this);
		bundle = getIntent().getExtras();
		Log.v("Bundle is =", "" + bundle);

		/*
		 * if (mCoreServiceHandler == null) { mCoreServiceHandler =
		 * KPTCoreServiceHandler
		 * .getCoreServiceInstance(getApplicationContext()); mCoreEngine =
		 * mCoreServiceHandler.getCoreInterface();
		 * 
		 * if (mCoreEngine != null) {
		 * //KPTLog.e("Core Engine NOT NULL ON RESUME",
		 * "Core Engine NOT NULL ON RESUME"); mCoreEngine.initializeCore(this);
		 * } }
		 * 
		 * if (mCoreEngine == null) {
		 * mCoreServiceHandler.registerCallback(this); } else { populateUI(); }
		 */

		filterAdapter = new FilterAdapter(this);

		et = (EditText) findViewById(R.id.editText);
		lv = (ListView) findViewById(R.id.listView);
		// lv.setAdapter(mDictAdapter);

		et.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_FILTER);
		et.setImeOptions(EditorInfo.IME_ACTION_DONE);

		et.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				// Abstract Method of TextWatcher Interface.
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// Abstract Method of TextWatcher Interface.
			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				updateWordsList();
			}
		});
	}

	@Override
	protected void onDestroy() {
		// KPTLog.e("MYDICT", "OnDestroy");
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

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mWordList.clear();		
		String[] temp = bundle.getStringArray("words");
		Collections.addAll(mWordList, temp);
		mWordList.remove(0);
		mWordList.remove(0);
		mWordList.remove(0);
		Task task = new Task();
		task.execute();
	}

	public class Task extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub

			
			lv.setAdapter(mDictAdapter);
			loading.dismiss();
			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub

			loading = ProgressDialog.show(KPTMyDictionarySearch.this,
					getResources().getString(R.string.kpt_title_wait),
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
					// KPTLog.e("Core Engine NOT NULL ON RESUME","Core Engine NOT NULL ON RESUME");
					mCoreEngine.initializeCore(KPTMyDictionarySearch.this);
				}
			}

			if (mCoreEngine == null) {
				mCoreServiceHandler
						.registerCallback(KPTMyDictionarySearch.this);
			} else {
				// populateUI();
			}
			return null;
		}

	}

	/*
	 * @Override protected void onPause() { // TODO Auto-generated method stub
	 * super.onPause();
	 * 
	 * mWordList.clear(); mWordList = null; }
	 */

	protected void updateWordsList() {

		textlength = et.getText().length();
		array_sort.clear();
		for (String text : mWordList) {

			if (textlength <= text.length()) {
				if (et.getText()
						.toString()
						.equalsIgnoreCase(
								(String) text.subSequence(0, textlength))) {
					array_sort.add(text);
				}
			}
		}

		lv.setAdapter(filterAdapter);
	}

	private class FilterAdapter extends BaseAdapter {

		private LayoutInflater mInflater;
		private Bitmap mDelIcon;

		public FilterAdapter(Context context) {
			// Cache the LayoutInflate to avoid asking for a new one each time.
			mInflater = LayoutInflater.from(context);

			// Icons bound to the rows.
			mDelIcon = BitmapFactory.decodeResource(context.getResources(),
					android.R.drawable.ic_menu_delete);

		}

		public int getCount() {
			return array_sort.size();
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
			/*
			 * if(!mFocusable) { return false; } else {
			 */
			return super.isEnabled(position);
			// }

		}

		/**
		 * Make a view to hold each row.
		 * 
		 * @see android.widget.ListAdapter#getView(int, android.view.View,
		 *      android.view.ViewGroup)
		 */
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			// A ViewHolder keeps references to children views to avoid
			// unneccessary calls
			// to findViewById() on each row.
			final ViewHolder holder;

			// When convertView is not null, we can reuse it directly, there is
			// no need
			// to reinflate it. We only inflate a new View when the convertView
			// supplied
			// by ListView is null.
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.kpt_my_dictionary_list,
						null);

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
			Log.i("POSITION", "Position is==" + position);
			switch (position) {
			default:
				holder.mText.setTextColor(Color.WHITE);
				holder.mText.setText(array_sort.get(position));
				holder.mText1.setVisibility(View.GONE);
				holder.mIcon.setImageBitmap(mDelIcon);
				break;
			}
			holder.mIcon.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (position >= 0) {
						boolean delCompl = mCoreEngine
								.removeUserDictionaryWords(new String[] { holder.mText
										.getText().toString() });
						if (delCompl) {
							mWordList.remove(holder.mText.getText().toString());
							updateWordsList();
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

	private class MyDictionaryAdapter extends BaseAdapter {

		private LayoutInflater mInflater;
		private Bitmap mDelIcon;

		public MyDictionaryAdapter(Context context) {
			// Cache the LayoutInflate to avoid asking for a new one each time.
			mInflater = LayoutInflater.from(context);

			// Icons bound to the rows.
			mDelIcon = BitmapFactory.decodeResource(context.getResources(),
					android.R.drawable.ic_menu_delete);

		}

		public int getCount() {
			return mWordList.size();
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
			/*
			 * if(!mFocusable) { return false; } else {
			 */
			return super.isEnabled(position);
			// }

		}

		/**
		 * Make a view to hold each row.
		 * 
		 * @see android.widget.ListAdapter#getView(int, android.view.View,
		 *      android.view.ViewGroup)
		 */
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			// A ViewHolder keeps references to children views to avoid
			// unneccessary calls
			// to findViewById() on each row.
			final ViewHolder holder;

			// When convertView is not null, we can reuse it directly, there is
			// no need
			// to reinflate it. We only inflate a new View when the convertView
			// supplied
			// by ListView is null.
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.kpt_my_dictionary_list,
						null);

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
			Log.i("MY POSITION IS", "" + position);
			switch (position) {

			default:
				holder.mText.setTextColor(Color.WHITE);
				holder.mText.setText(mWordList.get(position));
				holder.mText1.setVisibility(View.GONE);
				holder.mIcon.setImageBitmap(mDelIcon);
				break;
			}
			holder.mIcon.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (position >= 0) {
						boolean delCompl = mCoreEngine
								.removeUserDictionaryWords(new String[] { holder.mText
										.getText().toString() });
						if (delCompl) {
							mWordList.remove(position);
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
		mDictAdapter = new MyDictionaryAdapter(this);
		if (mCoreEngine != null) {
			// KPTLog.e("MYDICT", "ServiceConnected");
			mCoreEngine.initializeCore(this);

			Task task = new Task();
			task.execute();
			// populateUI();
		}
	}

	/*
	 * private void populateUI() { mWordList.clear(); String wordDataArray[] =
	 * mCoreEngine.getUserDictionary();
	 * 
	 * if (wordDataArray != null) { Collections.addAll(mWordList,
	 * wordDataArray); wordDataArray = null; } Log.i("WORDS_LIST", "Size is --"
	 * + mWordList.size()); // lv.setAdapter(mDictAdapter); }
	 */
}
