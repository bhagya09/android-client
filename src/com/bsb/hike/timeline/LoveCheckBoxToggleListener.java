package com.bsb.hike.timeline;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.timeline.model.ActionsDataModel;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.ActionsDataModel.ActionTypes;
import com.bsb.hike.timeline.model.ActionsDataModel.ActivityObjectTypes;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

import android.animation.ObjectAnimator;
import android.support.annotation.Nullable;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public abstract class LoveCheckBoxToggleListener implements OnCheckedChangeListener
{

	@Override
	public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked)
	{
		buttonView.setEnabled(false);
		buttonView.setClickable(false);

		final StatusMessage statusMessage = (StatusMessage) buttonView.getTag();

		JSONObject json = new JSONObject();

		try
		{
			json.put(HikeConstants.SU_ID, statusMessage.getMappedId());
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		if (!Utils.isUserOnline(HikeMessengerApp.getInstance()))
		{
			Toast.makeText(HikeMessengerApp.getInstance(), R.string.action_no_network, Toast.LENGTH_SHORT).show();
			toggleCompButtonState(buttonView, LoveCheckBoxToggleListener.this);
			buttonView.setEnabled(true);
			buttonView.setClickable(true);
			return;
		}

		if (isChecked)
		{
			RequestToken token = HttpRequests.createLoveLink(json, new IRequestListener()
			{
				@Override
				public void onRequestSuccess(Response result)
				{
					try
					{
						JSONObject response = (JSONObject) result.getBody().getContent();
						if (response.optString("stat").equals("ok"))
						{
							// Increment like count in actions table
							String selfMsisdn = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, null);

							ArrayList<String> actorList = new ArrayList<String>();
							actorList.add(selfMsisdn);

							HikeConversationsDatabase.getInstance().changeActionCountForObjID(statusMessage.getMappedId(),
									ActionsDataModel.ActivityObjectTypes.STATUS_UPDATE.getTypeString(), ActionsDataModel.ActionTypes.LIKE.getKey(), actorList, true);
						}
					}
					finally
					{
						buttonView.setEnabled(true);
						buttonView.setClickable(true);
					}
				}

				@Override
				public void onRequestProgressUpdate(float progress)
				{
					// Do nothing
				}

				@Override
				public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
				{
					Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), R.string.love_failed, Toast.LENGTH_SHORT).show();
					// Reverse action in actions heap
					TimelineActionsManager.getInstance().addMyAction(statusMessage.getMappedId(), ActionTypes.UNLIKE, ActivityObjectTypes.STATUS_UPDATE);

					// UI actions
					onActionsDataChanged();
					buttonView.setEnabled(true);
					buttonView.setClickable(true);
					toggleCompButtonState(buttonView, LoveCheckBoxToggleListener.this);
				}
			}, statusMessage.getMappedId());
			token.execute();

			// Add action to actions heap, even before making server call. This will ensure a more responsive UX.
			TimelineActionsManager.getInstance().addMyAction(statusMessage.getMappedId(), ActionTypes.LIKE, ActivityObjectTypes.STATUS_UPDATE);

			ObjectAnimator loveScaleX = ObjectAnimator.ofFloat(buttonView, "scaleX", 0.7f, 1.2f, 1f);
			ObjectAnimator loveScaleY = ObjectAnimator.ofFloat(buttonView, "scaleY", 0.7f, 1.2f, 1f);
			loveScaleX.start();
			loveScaleY.start();

			onActionsDataChanged();
		}
		else
		{
			RequestToken token = HttpRequests.removeLoveLink(json, new IRequestListener()
			{
				@Override
				public void onRequestSuccess(Response result)
				{
					try
					{
						JSONObject response = (JSONObject) result.getBody().getContent();
						if (response.optString("stat").equals("ok"))
						{
							// Decrement like count in actions table
							String selfMsisdn = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, null);

							ArrayList<String> actorList = new ArrayList<String>();
							actorList.add(selfMsisdn);

							HikeConversationsDatabase.getInstance().changeActionCountForObjID(statusMessage.getMappedId(),
									ActionsDataModel.ActivityObjectTypes.STATUS_UPDATE.getTypeString(), ActionsDataModel.ActionTypes.LIKE.getKey(), actorList, false);
						}
					}
					finally
					{
						buttonView.setEnabled(true);
						buttonView.setClickable(true);
					}
				}

				@Override
				public void onRequestProgressUpdate(float progress)
				{
					// Do nothing
				}

				@Override
				public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
				{
					Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), R.string.love_failed, Toast.LENGTH_SHORT).show();
					// Reverse action in actions heap
					TimelineActionsManager.getInstance().addMyAction(statusMessage.getMappedId(), ActionTypes.LIKE, ActivityObjectTypes.STATUS_UPDATE);

					onActionsDataChanged();
					buttonView.setEnabled(true);
					buttonView.setClickable(true);
					toggleCompButtonState(buttonView, LoveCheckBoxToggleListener.this);
				}
			}, statusMessage.getMappedId());
			token.execute();
			// Add action to actions heap, even before making server call. This will ensure a more responsive UX.
			TimelineActionsManager.getInstance().addMyAction(statusMessage.getMappedId(), ActionTypes.UNLIKE, ActivityObjectTypes.STATUS_UPDATE);
			// UI actions
			onActionsDataChanged();
		}

	}

	private void toggleCompButtonState(CompoundButton argButton, OnCheckedChangeListener argListener)
	{
		// unlink-relink onchange listener
		argButton.setOnCheckedChangeListener(null);
		argButton.toggle();
		argButton.setOnCheckedChangeListener(argListener);
	}

	//Override if required
	private void onActionsDataChanged()
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.ACTIVITY_UPDATE, null);
	};

}
