/**
 * 
 */
package com.bsb.hike.ces;

import android.support.annotation.Nullable;

import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.httpmgr.retry.BasicRetryPolicy;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONObject;

/**
 * @author suyash
 *
 */
public class CesTransport {

	private JSONObject scoreResponse;
	private boolean isLevel1InfoUploaded;
	private final byte SCORE = 0;
	private final byte LEVEL_ONE_INFO = 1;
	private byte mTaskType;
	private final String TAG = "CesTransport";

	public CesTransport() {
		// TODO Auto-generated constructor stub
	}

	public JSONObject sendCesScore(JSONObject score)
	{
		Logger.d(TAG, "Sending Json for score = " + score.toString());
		mTaskType = SCORE;
		BasicRetryPolicy retryPolicy = new BasicRetryPolicy(3, 30 * 1000, 2);
		RequestToken token = HttpRequests.uploadCesScore(score, getRequestListener(), retryPolicy);
		token.execute();
		return scoreResponse;
	}

	public boolean sendCesLevelOneInfo(JSONObject l1Data)
	{
		Logger.d(TAG, "Sending Json for level1 info = " + l1Data.toString());
		mTaskType = LEVEL_ONE_INFO;
		BasicRetryPolicy retryPolicy = new BasicRetryPolicy(3, 30 * 1000, 2);
		RequestToken token = HttpRequests.uploadCesLevelOneInfo(l1Data, getRequestListener(), retryPolicy);
		token.execute();
		return isLevel1InfoUploaded;
	}

	private IRequestListener getRequestListener()
	{
		return new IRequestListener() {
			
			@Override
			public void onRequestSuccess(Response result)
			{
				JSONObject response = (JSONObject) result.getBody().getContent();

				if (Utils.isResponseValid(response))
				{
					Logger.d(TAG, "Task = " + mTaskType + " , Success response from server = " + response.toString());
					if(mTaskType == SCORE)
					{
						CesTransport.this.scoreResponse = response;
					}
					else if(mTaskType == LEVEL_ONE_INFO)
					{
						CesTransport.this.isLevel1InfoUploaded = true;
					}
				}
			}
			
			@Override
			public void onRequestProgressUpdate(float progress) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
			{
				Logger.e(TAG, "Task = " + mTaskType + " , Failed !!!", httpException);
			}
		};
		
	}
}
