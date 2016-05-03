package com.bsb.hike.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

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

    private String currName;

    private String currEmail;

    private int currGenderType;

    private boolean isBackPressed;

    private SharedPreferences prefs;

    private RequestToken editNameRequestToken;

    private RequestToken editEmailGenderRequestToken;
    
    public EditProfileTask(String msisdn, ProfileActivity.ProfileType profileType, String newName, String newEmail, int newGenderType, boolean isBackPressed)
    {
        this.msisdn = msisdn;
        this.profileType = profileType;
        this.newName = newName;
        this.newEmail = newEmail;
        this.newGenderType = newGenderType;
        this.isBackPressed = isBackPressed;
        this.prefs = HikeMessengerApp.getInstance().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE);
        this.currName = prefs.getString(HikeMessengerApp.NAME_SETTING, "");
        this.currEmail = prefs.getString(HikeConstants.Extras.EMAIL, "");
        this.currGenderType = prefs.getInt(HikeConstants.Extras.GENDER, 0);
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
        if (!TextUtils.isEmpty(newName) && !currName.equals(newName))
        {
            editProfileName();
        }

        if (!TextUtils.isEmpty(newEmail) && (!newEmail.equals(currEmail) || newGenderType != currGenderType))
        {
            editProfileEmailGender();
        }
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
				if (profileType != ProfileActivity.ProfileType.GROUP_INFO && profileType != ProfileActivity.ProfileType.BROADCAST_INFO)
				{
					/*
					 * if the request was successful, update the shared preferences and the UI
					 */
					String name = newName;
					SharedPreferences.Editor editor = prefs.edit();
					editor.putString(HikeMessengerApp.NAME_SETTING, name);
					editor.commit();
					HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_NAME_CHANGED, null);
				}
				if (isBackPressed)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_UPDATE_FINISH, null);
				}
			}

            @Override
            public void onRequestProgressUpdate(float progress)
            {
            }

            @Override
            public void onRequestFailure(HttpException httpException)
			{
				if (isBackPressed)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_UPDATE_FINISH, null);
				}
			}
        };
    }

    private void editProfileEmailGender()
	{
		JSONObject obj = new JSONObject();
		try
		{
			Logger.d(getClass().getSimpleName(), "Profile details Email: " + newEmail + " Gender: " + newGenderType);
			if (!TextUtils.isEmpty(newEmail) && newEmail.equals(currEmail))
			{
				obj.put(HikeConstants.EMAIL, newEmail);
			}
			if (newGenderType != currGenderType)
			{
				obj.put(HikeConstants.GENDER, newGenderType == 1 ? "m" : newGenderType == 2 ? "f" : "");
			}
			Logger.d(getClass().getSimpleName(), "JSON to be sent is: " + obj.toString());

			editEmailGenderRequestToken = HttpRequests.editProfileEmailGenderRequest(obj, getEditEmailGenderRequestListener());
			editEmailGenderRequestToken.execute();
		}
		catch (JSONException e)
		{
			Logger.e("ProfileActivity", "Could not set email or gender", e);
		}
	}

    private IRequestListener getEditEmailGenderRequestListener()
    {
        return new IRequestListener()
        {
            @Override
            public void onRequestFailure(HttpException httpException)
            {
                if (isBackPressed)
                {
                    HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_UPDATE_FINISH, null);
                }
            }

            @Override
            public void onRequestSuccess(Response result)
            {
                SharedPreferences.Editor editor = prefs.edit();
                if (Utils.isValidEmail(newEmail))
                {
                    editor.putString(HikeConstants.Extras.EMAIL, newEmail);
                }
                editor.putInt(HikeConstants.Extras.GENDER, newGenderType);
                editor.commit();
                if (isBackPressed)
                {
                    HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_UPDATE_FINISH, null);
                }
            }

            @Override
            public void onRequestProgressUpdate(float progress)
            {

            }
        };
    }

    @Override
    public void cancel()
    {
        if (editNameRequestToken != null)
        {
            editNameRequestToken.cancel();
        }
        if (editEmailGenderRequestToken != null)
        {
            editEmailGenderRequestToken.cancel();
        }
    }
}
