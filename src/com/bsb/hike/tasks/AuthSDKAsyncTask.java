package com.bsb.hike.tasks;

import org.apache.http.client.methods.HttpRequestBase;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.view.View;

import com.bsb.hike.utils.AccountUtils;

public class AuthSDKAsyncTask extends AsyncTask<HttpRequestBase, Void, String>
{

	private View mProgressBarIndicator;

	private AuthAsyncTaskListener mListener;

	private Activity mActivity;

	private ProgressDialog progressDialog;

	private boolean mShowProgressDialog;

	public AuthSDKAsyncTask(Activity argActivity, View argProgressBar, boolean showProgressDialog, AuthAsyncTaskListener listener)
	{
		mProgressBarIndicator = argProgressBar;
		mListener = listener;
		mActivity = argActivity;
		mShowProgressDialog = showProgressDialog;
	}

	@Override
	protected void onPreExecute()
	{
		super.onPreExecute();
		if (mProgressBarIndicator != null)
		{
			mProgressBarIndicator.setVisibility(View.VISIBLE);
		}

		if (mShowProgressDialog)
		{
			progressDialog = new ProgressDialog(mActivity);
			progressDialog.setCancelable(true);
			progressDialog.setCanceledOnTouchOutside(false);
			progressDialog.setMessage("Please wait while we get you connected");
			progressDialog.setOnCancelListener(new OnCancelListener()
			{
				@Override
				public void onCancel(DialogInterface dialog)
				{
					AuthSDKAsyncTask.this.onPostExecute(null);
					AuthSDKAsyncTask.this.cancel(true);
				}
			});
			progressDialog.show();
		}
	}

	@Override
	protected String doInBackground(HttpRequestBase... params)
	{
		// for now, assuming only one request is present in the request array
		HttpRequestBase httpRequest;

		if (params != null)
		{
			httpRequest = params[0];
		}
		else
		{
			return null;
		}

		JSONObject jsonResponse = null;
		try
		{
			jsonResponse = AccountUtils.executeRequest(httpRequest);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			// We need to be absolutely sure that we are dismissing progress bar at some point
		}

		return jsonResponse == null ? null : jsonResponse.toString();
	}

	@Override
	protected void onPostExecute(String result)
	{
		super.onPostExecute(result);

		if (AuthSDKAsyncTask.this.isCancelled())
		{
			return;
		}

		if (progressDialog != null)
		{
			progressDialog.dismiss();
		}

		if (mProgressBarIndicator != null)
		{
			mProgressBarIndicator.setVisibility(View.GONE);
		}

		if (mListener != null)
		{
			if (result == null)
			{
				mListener.onFailed();
			}
			else
			{
				mListener.onComplete(result);
			}
		}

		mActivity = null;
	}

	public interface AuthAsyncTaskListener
	{
		void onComplete(String argResponse);

		void onFailed();
	}
}
