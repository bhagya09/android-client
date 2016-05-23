package com.bsb.hike.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeFile;
//import com.bsb.hike.platform.CardComponent.ImageComponent;
import com.bsb.hike.platform.CardComponent.MediaComponent;
import com.bsb.hike.platform.CardComponent.TextComponent;
//import com.bsb.hike.platform.CardComponent.VideoComponent;
import com.bsb.hike.utils.Utils;

public class PlatformMessageMetadata implements HikePlatformConstants {

    private final String CONTENT_UID = "contentUid";
    public int layoutId;
    public String notifText = "";
    public String channelSource = "";
    public int version;
    public boolean isInstalled;
    public HashMap<String, byte[]> thumbnailMap = new HashMap<String, byte[]>();
    Context mContext;
    private JSONObject json;
    public String contentId;
    private boolean isWide;
    private List<HikeFile> hikeFileList;
    public List<CardPojo> cards = new ArrayList<>();
    public PlatformMessageMetadata(String jsonString, Context context) throws JSONException {
        this(new JSONObject(jsonString), context, false);
    }
    public PlatformMessageMetadata(String jsonString, Context context, boolean isSent) throws JSONException {
        this(new JSONObject(jsonString), context, isSent);
    }
    public PlatformMessageMetadata(JSONObject jsonObject, Context context) throws JSONException {
        this(jsonObject, context, false);
    }
    public PlatformMessageMetadata(JSONObject metadata, Context context, boolean isSent) {
        this.json = metadata;
        this.mContext = context;
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
                CardPojo cardPojo = new CardPojo(cardJSON, mContext, hikeFileList);
                cards.add(cardPojo);
            }catch (JSONException ex){

            }
        }
    }

//    private void parseVideoComponents(JSONArray json) {
//        int total = json.length();
//        for (int i = 0; i < total; i++) {
//            JSONObject obj;
//            try {
//                obj = json.getJSONObject(i);
//                String key = obj.optString(KEY);
//                String thumbnail = obj.optString(THUMBNAIL);
//                if (!TextUtils.isEmpty(thumbnail) && TextUtils.isEmpty(key))
//                    key = String.valueOf(thumbnail.hashCode());
//                VideoComponent videoCom = new VideoComponent(
//                        obj.optString(TAG), key,
//                        obj.optString(URL), obj.optString(CONTENT_TYPE),
//                        obj.optString(MEDIA_SIZE), obj.optString(DURATION));
//                if (!TextUtils.isEmpty(thumbnail)) {
//                    // HikeConversationsDatabase.getInstance().addFileThumbnail(key, thumbnail.getBytes());
//                    thumbnailMap.put(key, Base64.decode(thumbnail, Base64.DEFAULT));
//                    obj.remove(THUMBNAIL);
//                    obj.put(KEY, key);
//                }
//
//                mediaComponents.add(videoCom);
//            } catch (JSONException e) {
//// TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//
//        }
//    }
//
//    private void parseAudioComponents(JSONArray json) {
//        int total = json.length();
//        for (int i = 0; i < total; i++) {
//            JSONObject obj;
//            try {
//                obj = json.getJSONObject(i);
//                CardComponent.AudioComponent audioCom = new CardComponent.AudioComponent(
//                        obj.optString(TAG), obj.optString(KEY),
//                        obj.optString(URL), obj.optString(CONTENT_TYPE),
//                        obj.optString(MEDIA_SIZE), obj.optString(DURATION));
//                mediaComponents.add(audioCom);
//            } catch (JSONException e) {
//// TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//
//        }
//    }



    public void addToThumbnailTable() {

        Set<String> thumbnailKeys = thumbnailMap.keySet();
        for (String key : thumbnailKeys)
            HikeConversationsDatabase.getInstance().addFileThumbnail(key, thumbnailMap.get(key));

    }

//    public void addThumbnailsToMetadata() {
//
//        if (json.has(ASSETS)) {
//            try {
//                JSONObject assets = json.getJSONObject(ASSETS);
//                if (assets.has(IMAGES)) {
//                    addThumbnailToImages(assets, IMAGES);
//                }
//                if (assets.has(VIDEOS)) {
//                    addThumbnailToImages(assets, VIDEOS);
//                }
//                for (MediaComponent mediaComponent : mediaComponents) {
//                    String base64 = getBase64FromDb(mediaComponent.getKey());
//                    if (!TextUtils.isEmpty(base64))
//                        mediaComponent.setThumbnail(base64);
//                }
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//
//        }
//    }

    private void addThumbnailToImages(JSONObject assets, String addTo) {
        JSONArray imagesItems = null;
        try {
            imagesItems = assets.getJSONArray(addTo);

            int length = imagesItems.length();
            for (int i = 0; i < length; i++) {
                JSONObject obj = imagesItems.getJSONObject(i);
                String key = obj.optString(KEY);
                String base64 = getBase64FromDb(key);
                if (!TextUtils.isEmpty(base64))
                    obj.put(THUMBNAIL, base64);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private String getBase64FromDb(String key) {
        if (!TextUtils.isEmpty(key)) {
            byte[] thumbnail = HikeConversationsDatabase.getInstance().getThumbnail(key);

            if (null != thumbnail) {
                String base64 = Base64.encodeToString(thumbnail, Base64.NO_WRAP);
                return base64;
            }
        }
        return "";
    }

    public JSONObject getHelperData(){

        if (json.has(HELPER_DATA)){
            JSONObject jsonObj = json.optJSONObject(HELPER_DATA);
            if (null != jsonObj)
                return jsonObj;
        }
        return new JSONObject();
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
        List<HikeFile> hikeFileList = new ArrayList<HikeFile>();
        for (int i = 0; i < fileList.length(); i++)
        {
            HikeFile newHikeFile = new HikeFile(fileList.optJSONObject(i), isSent);
            if(!TextUtils.isEmpty(caption))
            {
                newHikeFile.setCaption(caption);
            }
            hikeFileList.add(newHikeFile);
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