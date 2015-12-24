package com.bsb.hike.tasks;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.platform.HikePlatformConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by konarkarora on 16/11/15.
 */
public class ConvertToJsonArrayTask extends AsyncTask<Void, Void, JSONArray>
{
    ArrayList<ContactInfo> list;
    ProgressDialog dialog;
    boolean thumbnailsRequired;
    private ConvertToJsonArrayCallback convertToJsonArrayCallback;
    
    public interface ConvertToJsonArrayCallback
    {
        public void onCallBack(JSONArray array);
    }

    public ConvertToJsonArrayTask(ConvertToJsonArrayCallback convertToJsonArrayCallback,ArrayList<ContactInfo> list, ProgressDialog dialog, boolean thumbnailsRequired)
    {
        this.list = list;
        this.dialog = dialog;
        this.thumbnailsRequired = thumbnailsRequired;
        this.convertToJsonArrayCallback = convertToJsonArrayCallback;
    }

    public ConvertToJsonArrayTask(ConvertToJsonArrayCallback convertToJsonArrayCallback,ArrayList<ContactInfo> list, boolean thumbnailsRequired)
    {
        this.list = list;
        this.convertToJsonArrayCallback = convertToJsonArrayCallback;
        this.thumbnailsRequired = thumbnailsRequired;
    }

    @Override
    protected void onPreExecute()
    {
        if(dialog != null)
            dialog.show();
        super.onPreExecute();
    }

    @Override
    protected JSONArray doInBackground(Void... params)
    {
        return convertToJSONArray(this.list, thumbnailsRequired);
    }

    @Override
    protected void onPostExecute(JSONArray array)
    {
        super.onPostExecute(array);
        if(dialog != null)
            dialog.dismiss();
        convertToJsonArrayCallback.onCallBack(array);
    }

    private JSONArray convertToJSONArray(List<ContactInfo> list, boolean thumbnailsRequired)
    {
        JSONArray array = new JSONArray();
        JSONObject platformInfo;
        for(ContactInfo contactInfo : list)
        {
            try
            {
                platformInfo = contactInfo.getPlatformInfo();
                if (thumbnailsRequired)
                {
                    platformInfo.put(HikePlatformConstants.THUMBNAIL, ContactManager.getInstance().getImagePathForThumbnail(contactInfo.getMsisdn()));
                }
                array.put(platformInfo);
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
        return array;
    }

}
