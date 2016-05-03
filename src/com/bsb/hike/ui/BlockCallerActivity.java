package com.bsb.hike.ui;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.adapters.BlockCallerListAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ashishagarwal on 07/12/15.
 */
public class BlockCallerActivity extends HikeAppStateBaseFragmentActivity implements View.OnClickListener, HikeDialogListener
{

	private RecyclerView mCallerBlockListRecyclerView;

	private RecyclerView.LayoutManager mLayoutManager;

	private BlockCallerListAdapter blockCallerAdapter;

	private String msisdn;

	private View emptyView;

	private String callType;

	private Map<String, String> nameNumberMap = new HashMap<>();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.block_contact_settings);

		setupActionBar();

		//Msisdn is sent on the block contact from card so this will make sure to show the dialog and once dialog is shown we will remove the extra so that
		// again the dialog is not shown
		if (getIntent().hasExtra(HikeConstants.MSISDN) && getIntent().getStringExtra(HikeConstants.MSISDN) != null)
		{
			if (savedInstanceState == null || !savedInstanceState.getBoolean(HikeConstants.Extras.CLEARED_OUT, false))
			{
				msisdn = getIntent().getStringExtra(HikeConstants.MSISDN);
				callType = getIntent().getStringExtra(HikeConstants.CALL_TYPE);
				HikeDialogFactory.showDialog(this, HikeDialogFactory.CALLER_BLOCK_CONTACT_DIALOG, this, getIntent().getStringExtra(HikeConstants.NAME));
				getIntent().removeExtra(HikeConstants.MSISDN);
				getIntent().removeExtra(HikeConstants.NAME);
				getIntent().removeExtra(HikeConstants.CALL_TYPE);
			}
		}

		mCallerBlockListRecyclerView = (RecyclerView) findViewById(R.id.blockContactRecycleView);
		mCallerBlockListRecyclerView.setVisibility(View.GONE);
		mLayoutManager = new LinearLayoutManager(this);
		mCallerBlockListRecyclerView.setLayoutManager(mLayoutManager);
		FetchBlockCallerListTask fetchBlockCallerListTask = new FetchBlockCallerListTask();
		fetchBlockCallerListTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		emptyView = findViewById(R.id.block_activity_empty_view);

	}

	@Override
	public void negativeClicked(HikeDialog hikeDialog) {

		hikeDialog.dismiss();
		HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.NEGATIVE_CLICKED, msisdn,
				hikeDialog.getId() == HikeDialogFactory.CALLER_BLOCK_CONTACT_DIALOG ? AnalyticsConstants.StickyCallerEvents.BLOCK_DIALOG
						: AnalyticsConstants.StickyCallerEvents.UNBLOCK_DIALOG,
				callType);
	}

	@Override
	public void positiveClicked(HikeDialog hikeDialog) {

		ContentValues contentValues = new ContentValues();
		contentValues.put(DBConstants.HIKE_USER.IS_SYNCED, ChatHeadUtils.VALUE_FALSE);
		if (hikeDialog.getId() == HikeDialogFactory.CALLER_BLOCK_CONTACT_DIALOG)
		{
			contentValues.put(DBConstants.HIKE_USER.IS_BLOCK, ChatHeadUtils.VALUE_TRUE);
			ContactManager.getInstance().updateBlockStatusIntoCallerTable(msisdn, contentValues);
		}
		else
		{
			contentValues.put(DBConstants.HIKE_USER.IS_BLOCK, ChatHeadUtils.VALUE_FALSE);
			ContactManager.getInstance().updateBlockStatusIntoCallerTable(msisdn, contentValues);
		}
		FetchBlockCallerListTask fetchBlockCallerListTask = new FetchBlockCallerListTask();
		fetchBlockCallerListTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		hikeDialog.dismiss();
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.CALLER_BLOKED_LIST_SYNCHED, false);
		ChatHeadUtils.syncFromClientToServer();
		HAManager.getInstance().stickyCallerAnalyticsUIEvent(AnalyticsConstants.StickyCallerEvents.POSITIVE_CLICKED, msisdn,
				hikeDialog.getId() == HikeDialogFactory.CALLER_BLOCK_CONTACT_DIALOG ? AnalyticsConstants.StickyCallerEvents.BLOCK_DIALOG
						: AnalyticsConstants.StickyCallerEvents.UNBLOCK_DIALOG,
				callType);
	}

	@Override
	public void neutralClicked(HikeDialog hikeDialog) {

	}



	private void isEmptyViewVisible(boolean isEmptyView)
	{
		emptyView.setVisibility(isEmptyView? View.VISIBLE: View.GONE);
		mCallerBlockListRecyclerView.setVisibility(isEmptyView? View.GONE: View.VISIBLE);
	}

	private class FetchBlockCallerListTask extends AsyncTask<Void, Void, Cursor>
	{

		@Override
		protected void onPreExecute()
		{
		}

		@Override
		protected Cursor doInBackground(Void... params)
		{
			Cursor cursor = ContactManager.getInstance().getCallerBlockContactCursor();
			if (cursor != null && cursor.getCount() >= 1)
			{
				if (nameNumberMap != null)
				{
					for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
					{
						String msisdn = cursor.getString(cursor.getColumnIndex(DBConstants.MSISDN));
						if (!nameNumberMap.containsKey(msisdn))
						{
							nameNumberMap.put(msisdn, ChatHeadUtils.getNameFromNumber(BlockCallerActivity.this, msisdn));
						}
					}
				}
				cursor.moveToFirst();
			}
			return cursor;
		}

		@Override
		protected void onPostExecute(final Cursor result)
		{
			if (result != null && result.getCount() >= 1)
			{
				isEmptyViewVisible(false);
				if (blockCallerAdapter == null)
				{
					blockCallerAdapter = new BlockCallerListAdapter(BlockCallerActivity.this ,result, nameNumberMap, BlockCallerActivity.this, 0);
					mCallerBlockListRecyclerView.setAdapter(blockCallerAdapter);
				}
				else
				{
					blockCallerAdapter.swapCursor(result, nameNumberMap);
				}
			}
			else
			{
				isEmptyViewVisible(true);
			}
		}
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();

		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.blocked_list);
		actionBar.setCustomView(actionBarView);
	}

	protected void onSaveInstanceState(Bundle outState)
	{
		// first saving my state, so the bundle wont be empty.
		// http://code.google.com/p/android/issues/detail?id=19917
		outState.putBoolean(HikeConstants.Extras.CLEARED_OUT, true);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onClick(View v)
	{
		msisdn = ((TextView) v.findViewById(R.id.number)).getText().toString();

		HikeDialogFactory.showDialog(this, HikeDialogFactory.CALLER_UNBLOCK_CONTACT_DIALOG, this, ((TextView) v.findViewById(R.id.name)).getText().toString());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
