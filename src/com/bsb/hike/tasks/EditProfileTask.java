package com.bsb.hike.tasks;

import android.content.Context;
import android.content.SharedPreferences;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sidharth on 03/05/16.
 */
public class EditProfileTask implements IHikeHTTPTask
{
    private String msisdn;

    private ProfileActivity.ProfileType profileType;

    private String newName;

    private String newEmail;

    private int newGenderType;

    private boolean isBackPressed;

    private SharedPreferences prefs;

    private RequestToken editNameRequestToken;

    public EditProfileTask(String msisdn, ProfileActivity.ProfileType profileType, String newName, String newEmail, int newGenderType, boolean isBackPressed)
    {
        this.msisdn = msisdn;
        this.profileType = profileType;
        this.newName = newName;
        this.newEmail = newEmail;
        this.newGenderType = newGenderType;
        this.isBackPressed = isBackPressed;
        this.prefs = HikeMessengerApp.getInstance().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE);
    }

    public void setNewName(String name)
    {
        this.newName = name;
    }

    public void setNewEmail(String email)
    {
        this.newEmail = email;
    }

    public void setNewGenderType(int genderType)
    {
        this.newGenderType = genderType;
    }

    @Override
    public void execute()
    {

    }

    private void editProfileName()
	{
		JSONObject json = new JSONObject();
		try
		{
			json.put("name", newName);

			if (this.profileType == ProfileActivity.ProfileType.GROUP_INFO)
			{
				editNameRequestToken = HttpRequests.editGroupProfileNameRequest(json, getEditNameRequestListener(), msisdn);
			}
			else
			{
				editNameRequestToken = HttpRequests.editProfileNameRequest(json, getEditNameRequestListener());
			}
			editNameRequestToken.execute();
		}
		catch (JSONException e)
		{
			Logger.e("ProfileActivity", "Could not set name", e);
		}
	}

    private IRequestListener getEditNameRequestListener()
    {
        return new IRequestListener()
        {
            @Override
            public void onRequestSuccess(Response result)
            {

            }

            @Override
            public void onRequestProgressUpdate(float progress)
            {
            }

            @Override
            public void onRequestFailure(HttpException httpException)
            {

            }
        };
    }

    @Override
    public void cancel()
    {

    }
}
