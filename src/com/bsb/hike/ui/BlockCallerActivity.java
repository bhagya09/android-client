package com.bsb.hike.ui;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.BlockCallerListAdapter;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.chatHead.StickyCaller;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.timeline.view.DividerItemDecoration;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by ashishagarwal on 07/12/15.
 */
public class BlockCallerActivity extends HikeAppStateBaseFragmentActivity implements View.OnClickListener, HikeDialogListener
{

	private RecyclerView mCallerBlockListRecyclerView;

	private RecyclerView.LayoutManager mLayoutManager;

	private BlockCallerListAdapter blockCallerAdapter;

	private String msisdn;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.block_contact_settings);

		setupActionBar();
		if (getIntent().hasExtra(HikeConstants.MSISDN) && getIntent().getStringExtra(HikeConstants.MSISDN) != null)
		{
			msisdn = getIntent().getStringExtra(HikeConstants.MSISDN);
			HikeDialogFactory.showDialog(this, HikeDialogFactory.CALLER_BLOCK_CONTACT_DIALOG, this, getIntent().getStringExtra(HikeConstants.NAME));
			getIntent().removeExtra(HikeConstants.MSISDN);
			getIntent().removeExtra(HikeConstants.NAME);
		}
		mCallerBlockListRecyclerView = (RecyclerView) findViewById(R.id.blockContactRecycleView);
		mCallerBlockListRecyclerView.setVisibility(View.GONE);
		mLayoutManager = new LinearLayoutManager(this);
		mCallerBlockListRecyclerView.setLayoutManager(mLayoutManager);
		FetchBlockCallerListTask fetchBlockCallerListTask = new FetchBlockCallerListTask();
		fetchBlockCallerListTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void negativeClicked(HikeDialog hikeDialog) {

		hikeDialog.dismiss();
	}

	@Override
	public void positiveClicked(HikeDialog hikeDialog) {

		if (hikeDialog.getId() == HikeDialogFactory.CALLER_BLOCK_CONTACT_DIALOG)
		{
			ContactManager.getInstance().updateBlockStatusIntoCallerTable(msisdn, 1);
		}
		else
		{
			ContactManager.getInstance().updateBlockStatusIntoCallerTable(msisdn, 0);
		}
		FetchBlockCallerListTask fetchBlockCallerListTask = new FetchBlockCallerListTask();
		fetchBlockCallerListTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		hikeDialog.dismiss();
		try
		{
			JSONObject json = new JSONObject();
			json.put(HikeConstants.MSISDN, msisdn);
			json.put(HikeConstants.IS_BLOCK, (hikeDialog.getId() == HikeDialogFactory.CALLER_BLOCK_CONTACT_DIALOG) ? 1 : 0);
			RequestToken requestToken = HttpRequests.postCallerMsisdn(HttpRequestConstants.getHikeCallerUrl(), json, null, ChatHeadUtils.HTTP_CALL_RETRY_DELAY,
					ChatHeadUtils.HTTP_CALL_RETRY_MULTIPLIER, false);
			requestToken.execute();
		}
		catch (JSONException e)
		{
			Logger.d("JSON Exception" , "Caller Block Server Call");
		}
	}

	@Override
	public void neutralClicked(HikeDialog hikeDialog) {

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
			return ContactManager.getInstance().getCallerBlockContactCursor();
		}

		@Override
		protected void onPostExecute(final Cursor result)
		{
			if (result != null && result.getCount() >=1)
			{
				findViewById(R.id.block_activity_empty_view).setVisibility(View.GONE);
				mCallerBlockListRecyclerView.setVisibility(View.VISIBLE);
				if (blockCallerAdapter == null)
				{
					blockCallerAdapter = new BlockCallerListAdapter(result, BlockCallerActivity.this, 0);
					mCallerBlockListRecyclerView.setAdapter(blockCallerAdapter);
				}
				else
				{
					blockCallerAdapter.swapCursor(result);
				}
			}
			else
			{
					findViewById(R.id.block_activity_empty_view).setVisibility(View.VISIBLE);
					mCallerBlockListRecyclerView.setVisibility(View.GONE);
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
