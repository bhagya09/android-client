package com.bsb.hike.tasks;

import android.content.Context;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.models.Birthday;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.BirthdayUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

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

    private String newDob;

    private String currName;

    private String currEmail;

    private int currGenderType;

    private String currDob;

    private boolean isBackPressed;

    private RequestToken editNameRequestToken;

    private RequestToken editEmailGenderRequestToken;

    private RequestToken editDOBRequestToken;

    private AtomicInteger editProfileRequestsCount;

    private Context applicationCtx;

    public EditProfileTask(String msisdn, ProfileActivity.ProfileType profileType, String newName, String newEmail, int newGenderType, String newDob, boolean isBackPressed)
    {
        this.msisdn = msisdn;
        this.profileType = profileType;
        this.newName = newName;
        this.newEmail = newEmail;
        this.newGenderType = newGenderType;
        this.newDob = newDob;
        this.isBackPressed = isBackPressed;
        this.applicationCtx = HikeMessengerApp.getInstance().getApplicationContext();
        this.currName = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.NAME_SETTING, "");
        this.currEmail = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.EMAIL, "");
        this.currGenderType = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.GENDER, 0);
        this.currDob = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.DOB, "");
        this.editProfileRequestsCount = new AtomicInteger(0);
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

    public void setNewDOB(String dob)
    {
        this.newDob = dob;
    }

    @Override
    public void execute()
    {
        if (!TextUtils.isEmpty(newName) && !currName.equals(newName))
        {
            editProfileRequestsCount.incrementAndGet();
            editProfileName();
        }

        if ((!TextUtils.isEmpty(newEmail) && !newEmail.equals(currEmail)) || newGenderType != currGenderType)
        {
            editProfileRequestsCount.incrementAndGet();
            editProfileEmailGender();
        }

        if (!TextUtils.isEmpty(newDob) && !newDob.equals(currDob))
        {
            editProfileRequestsCount.incrementAndGet();
            editProfileDOB();
        }
    }

    private void editProfileName()
	{
        JSONObject json = getEditProfileRequestBody();
        if (json != null && json.length() > 0)
        {
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
	}

    private JSONObject getEditProfileRequestBody()
    {
        JSONObject json = null;
        try
        {
            json = new JSONObject();
            json.put(HikeConstants.NAME, newName);
        }
        catch (JSONException e)
        {
            Logger.e("ProfileActivity", "Could not set name", e);
        }
        return json;
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
                    HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.NAME_SETTING, newName);
					HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_NAME_CHANGED, null);
				}
                if (editProfileRequestsCount.decrementAndGet() == 0)
                {
                    HikeMessengerApp.getPubSub().publish(HikePubSub.DISMISS_EDIT_PROFILE_DIALOG, null);
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
            public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
			{
                if (editProfileRequestsCount.decrementAndGet() == 0)
                {
                    HikeMessengerApp.getPubSub().publish(HikePubSub.DISMISS_EDIT_PROFILE_DIALOG, null);
                    showErrorToast(R.string.update_profile_failed, Toast.LENGTH_LONG);
                }

				if (isBackPressed)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_UPDATE_FINISH, null);
				}
			}
        };
    }

    private void editProfileEmailGender()
	{
		JSONObject obj = getEditProfileEmailGenderRequestBody();
        if (obj != null && obj.length() > 0)
        {
            Logger.d(getClass().getSimpleName(), "JSON to be sent is: " + obj.toString());
            editEmailGenderRequestToken = HttpRequests.editProfileEmailGenderRequest(obj, getEditEmailGenderRequestListener());
            editEmailGenderRequestToken.execute();
        }

	}

    private JSONObject getEditProfileEmailGenderRequestBody()
    {
        JSONObject obj = null;
        try
        {
            obj = new JSONObject();
            Logger.d(getClass().getSimpleName(), "Profile details Email: " + newEmail + " Gender: " + newGenderType);
            if (!TextUtils.isEmpty(newEmail) && !newEmail.equals(currEmail) && Utils.isValidEmail(newEmail))
            {
                obj.put(HikeConstants.EMAIL, newEmail);
            }
            if (newGenderType != currGenderType)
            {
                obj.put(HikeConstants.GENDER, newGenderType == 1 ? "m" : newGenderType == 2 ? "f" : "");
            }
        }
        catch (JSONException e)
        {
            Logger.e("ProfileActivity", "Could not set email or gender", e);
        }
        return obj;
    }

    private IRequestListener getEditEmailGenderRequestListener()
    {
        return new IRequestListener()
        {
            @Override
            public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
            {
                if (editProfileRequestsCount.decrementAndGet() == 0)
                {
                    HikeMessengerApp.getPubSub().publish(HikePubSub.DISMISS_EDIT_PROFILE_DIALOG, null);
                    showErrorToast(R.string.update_profile_failed, Toast.LENGTH_LONG);
                }
                if (isBackPressed)
                {
                    HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_UPDATE_FINISH, null);
                }
            }

            @Override
            public void onRequestSuccess(Response result)
            {
                HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.Extras.EMAIL, newEmail);
                HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.Extras.GENDER, newGenderType);
                if (editProfileRequestsCount.decrementAndGet() == 0)
                {
                    HikeMessengerApp.getPubSub().publish(HikePubSub.DISMISS_EDIT_PROFILE_DIALOG, null);
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
        };
    }

    private void editProfileDOB()
    {
        JSONObject obj = getEditProfileDOBRequestBody();
        if (obj != null && obj.length() > 0)
        {
            Logger.d(getClass().getSimpleName(), "JSON to be sent is: " + obj.toString());
            editDOBRequestToken = HttpRequests.editDOBRequest(obj, getEditDOBRequestListener());
            editDOBRequestToken.execute();
        }
    }

    private JSONObject getEditProfileDOBRequestBody()
    {
        JSONObject payload = null;
        try
        {
            payload = new JSONObject();
            Birthday updatedDOB = new Birthday(newDob);
            JSONObject dobJSON = new JSONObject();
            dobJSON.put(HikeConstants.DAY, updatedDOB.day);
            dobJSON.put(HikeConstants.MONTH, (updatedDOB.month));
            dobJSON.put(HikeConstants.YEAR, updatedDOB.year);
            payload.put(HikeConstants.DOB, dobJSON);
            Logger.d(getClass().getSimpleName(), "JSON to be sent is: " + payload.toString());
        }
        catch (JSONException ex) {
            Logger.e(getClass().getSimpleName(), "Could not update DoB");
        }
        return payload;
    }

    private IRequestListener getEditDOBRequestListener()
    {
        return new IRequestListener()
        {
            @Override
            public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
            {
                Logger.d(getClass().getSimpleName(), "DoB update request failed");
                if (editProfileRequestsCount.decrementAndGet() == 0)
                {
                    HikeMessengerApp.getPubSub().publish(HikePubSub.DISMISS_EDIT_PROFILE_DIALOG, null);
                    showErrorToast(R.string.update_profile_failed, Toast.LENGTH_LONG);
                }
                if (isBackPressed)
                {
                    HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_UPDATE_FINISH, null);
                }
            }

            @Override
            public void onRequestSuccess(Response result)
            {
                Logger.d(getClass().getSimpleName(), "DoB updated request successful");
                HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.DOB, newDob);

                BirthdayUtils.recordBirthdayAnalytics(
                        AnalyticsConstants.BirthdayEvents.BIRTHDAY_SAVED,
                        AnalyticsConstants.BirthdayEvents.BIRTHDAY_STORE,
                        AnalyticsConstants.BirthdayEvents.BIRTHDAY_SAVED,
                        null, null, null, null, null, null, null, null);

                if (editProfileRequestsCount.decrementAndGet() == 0)
                {
                    HikeMessengerApp.getPubSub().publish(HikePubSub.DISMISS_EDIT_PROFILE_DIALOG, null);
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

	@Override
	public Bundle getRequestBundle()
	{
		return null;
	}

	@Override
	public String getRequestId()
	{
		return null;
	}

    private void showErrorToast(int stringResId, int duration)
    {
        Toast toast = Toast.makeText(applicationCtx, stringResId, duration);
        toast.show();
    }
}
