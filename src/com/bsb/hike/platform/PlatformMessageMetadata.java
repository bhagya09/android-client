package com.bsb.hike.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Base64;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeFile;


public class PlatformMessageMetadata implements HikePlatformConstants {

    private final String CONTENT_UID = "contentUid";
    public int layoutId;
    public String notifText = "";
    public String channelSource = "";
    public int version;
    public boolean isInstalled;
    public HashMap<String, byte[]> thumbnailMap = new HashMap<String, byte[]>();
    private JSONObject json;
    public String contentId;
    private boolean isWide;
    private List<HikeFile> hikeFileList;
    public List<CardPojo> cards = new ArrayList<>();
    public PlatformMessageMetadata(String jsonString, boolean isSent) throws JSONException {
        this(new JSONObject(jsonString), isSent);
    }
    public PlatformMessageMetadata(JSONObject metadata) {
        this(metadata, false);
    }
    public PlatformMessageMetadata(JSONObject metadata, boolean isSent) {
        this.json = metadata;
        try {
            version = PlatformUtils.getInt(metadata, VERSION);
            layoutId = PlatformUtils.getInt(metadata, LAYOUT_ID);
            notifText = PlatformUtils.getString(metadata, NOTIF_TEXT);
            contentId = PlatformUtils.getString(metadata, CONTENT_UID);
            isWide = PlatformUtils.getBoolean(metadata, WIDE);
            this.hikeFileList = getHikeFileListFromJSONArray(metadata.optJSONArray(HikeConstants.FILES), isSent, metadata.optString(HikeConstants.CAPTION));
            if(metadata.has(CARDS)){
                parseCards(metadata.getJSONArray(CARDS));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void parseCards(JSONArray jsonArray){
        int total = jsonArray.length();
        for(int i=0; i< total; i++){
            try{
                JSONObject cardJSON = jsonArray.getJSONObject(i);
                CardPojo cardPojo = new CardPojo(cardJSON, hikeFileList);
                cards.add(cardPojo);
            }catch (JSONException ex){

            }
        }
    }

    private void addThumbnailToDb(JSONObject fileJSONObj) {
        String thumbnail = fileJSONObj.optString(THUMBNAIL);
        String key = fileJSONObj.optString(HikeConstants.FILE_KEY);
        if (!TextUtils.isEmpty(thumbnail) && TextUtils.isEmpty(key))
            key = String.valueOf(thumbnail.hashCode());
        if (!TextUtils.isEmpty(thumbnail)) {
            HikeConversationsDatabase.getInstance().addFileThumbnail(key, Base64.decode(thumbnail, Base64.DEFAULT));
            fileJSONObj.remove(THUMBNAIL);
        }
    }

    public void addToThumbnailTable() {

        Set<String> thumbnailKeys = thumbnailMap.keySet();
        for (String key : thumbnailKeys)
            HikeConversationsDatabase.getInstance().addFileThumbnail(key, thumbnailMap.get(key));

    }



    public String JSONtoString() {
        return json.toString();
    }

    public JSONObject getJSON() {
        return json;
    }
    private List<HikeFile> getHikeFileListFromJSONArray(JSONArray fileList, boolean isSent, String caption)
    {
        if (fileList == null)
        {
            return null;
        }
        List<HikeFile> hikeFileList = new ArrayList<>();
        for (int i = 0; i < fileList.length(); i++)
        {
            JSONObject fileObj = fileList.optJSONObject(i);
            HikeFile newHikeFile = new HikeFile(fileObj, isSent);
            if(!TextUtils.isEmpty(caption))
            {
                newHikeFile.setCaption(caption);
            }

            hikeFileList.add(newHikeFile);
            addThumbnailToDb(fileObj);
        }
        return hikeFileList;
    }
    public List<HikeFile> getHikeFiles() {
        return hikeFileList;
    }
    public boolean isWideCard(){
        return isWide;
    }
}