package com.bsb.hike.tasks;

import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.ui.ProfileActivity;

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

    public EditProfileTask(String msisdn, ProfileActivity.ProfileType profileType, String newName, String newEmail, int newGenderType, boolean isBackPressed)
    {
        this.msisdn = msisdn;
        this.profileType = profileType;
        this.newName = newName;
        this.newEmail = newEmail;
        this.newGenderType = newGenderType;
        this.isBackPressed = isBackPressed;
    }

    @Override
    public void execute()
    {

    }

    @Override
    public void cancel()
    {

    }
}
