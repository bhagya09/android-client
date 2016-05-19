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
    public int loveId;
    public String notifText = "";
    public String channelSource = "";
    public int version;
    public boolean isInstalled;
    public HashMap<String, byte[]> thumbnailMap = new HashMap<String, byte[]>();
    public List<TextComponent> textComponents = new ArrayList<CardComponent.TextComponent>();
    public List<MediaComponent> mediaComponents = new ArrayList<CardComponent.MediaComponent>();
    public ArrayList<CardComponent.ActionComponent> actionComponents = new ArrayList<CardComponent.ActionComponent>();
    public List<CardComponent.ImageComponent> imageComponents = new ArrayList<CardComponent.ImageComponent>();
    public String clickTrackUrl = "";
    Context mContext;
    private JSONObject json;
    public boolean showShare;
    public String backgroundColor;
    public String background;
    public String contentId;
    public CardComponent.ActionComponent cardAction;
    private boolean isSent;
    private boolean isWide;
    private List<HikeFile> hikeFileList;
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
        this.isSent = isSent;
        try {
			backgroundColor = getString(metadata, BACKGROUND_COLOR);
            background = getString(metadata,BACKGROUND);
            version = getInt(metadata, VERSION);
            layoutId = getInt(metadata, LAYOUT_ID);
            loveId = getInt(metadata, LOVE_ID);
            showShare = getBoolean(metadata, SHOW_SHARE);
            notifText = getString(metadata, NOTIF_TEXT);
            clickTrackUrl = getString(metadata, CLICK_TRACK_URL);
            contentId = getString(metadata,CONTENT_UID);
            isWide = getBoolean(metadata, WIDE);
            this.hikeFileList = getHikeFileListFromJSONArray(metadata.optJSONArray(HikeConstants.FILES), isSent, metadata.optString(HikeConstants.CAPTION));
            if(metadata.has(CARD_ACTION)){
                JSONObject cardActionObj = metadata.optJSONObject(CARD_ACTION);
                cardAction = new CardComponent.ActionComponent(cardActionObj.getString(ACTION), cardActionObj.getString(ACTION_TEXT), cardActionObj.optJSONObject(ACTION_EXTRA));
            }
            if (metadata.has(CHANNEL_SOURCE)) {
                channelSource = metadata.optString(CHANNEL_SOURCE);
                isInstalled = Utils.isPackageInstalled(mContext, channelSource);
            }

            if (metadata.has(ASSETS)) {
                JSONObject assets = metadata.optJSONObject(ASSETS);
                if (assets.has(TEXTS)) {
                    parseTextComponents(assets.getJSONArray(TEXTS));
                }
                if (assets.has(IMAGES)) {
                    parseImageComponents(assets.getJSONArray(IMAGES));
                }
//                if (assets.has(VIDEOS)) {
//                    parseVideoComponents(assets.getJSONArray(VIDEOS));
//                }
//                if (assets.has(AUDIO)) {
//                    parseAudioComponents(assets.getJSONArray(AUDIO));
//                }
                if (assets.has(ACTIONS)) {
                    parseActionComponents(assets.getJSONArray(ACTIONS));
                }

                if(assets.has(MEDIA)){
                    parseMediaComponents(assets.getJSONArray(MEDIA));
                }

            }


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    private void parseMediaComponents(JSONArray jsonArray){
        int total = jsonArray.length();
        for (int i = 0; i < total; i++) {
            try {
                JSONObject actionObj = jsonArray.getJSONObject(i);
                CardComponent.MediaComponent mediaComponent;
                if(actionObj.has(FILE_MAPPING)){
                    mediaComponent = new CardComponent.MediaComponent(actionObj.getString(TAG),hikeFileList.get(actionObj.getInt(FILE_MAPPING)));
                }else{
                    mediaComponent = new CardComponent.MediaComponent(actionObj.getString(TAG),actionObj.getString(URL));
                }
                mediaComponents.add(mediaComponent);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    private void parseActionComponents(JSONArray jsonArray) {

        int total = jsonArray.length();

        for (int i = 0; i < total; i++) {
            try {
                JSONObject actionObj = jsonArray.getJSONObject(i);
                CardComponent.ActionComponent actionComponent = new CardComponent.ActionComponent(actionObj.getString(ACTION), actionObj.optString(ACTION_TEXT), actionObj.optJSONObject(ACTION_EXTRA));
                actionComponents.add(actionComponent);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    private void parseTextComponents(JSONArray json) {
        int total = json.length();

        for (int i = 0; i < total; i++) {
            try {
				JSONObject obj = json.getJSONObject(i);
				TextComponent textCom = new TextComponent.Builder(obj.optString(TAG)).setText(obj.optString(TEXT)).setTextColor(obj.optString(TEXT_COLOR))
						.setTextSize(obj.optInt(TEXT_SIZE)).build();
				textComponents.add(textCom);
            } catch (JSONException e) {
// TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void parseImageComponents(JSONArray json) {
        int total = json.length();
        for (int i = 0; i < total; i++) {
            JSONObject obj;
            try {
                obj = json.getJSONObject(i);
                String key = obj.optString(KEY);
                String thumbnail = obj.optString(THUMBNAIL);
                if (!TextUtils.isEmpty(thumbnail) && TextUtils.isEmpty(key))
                    key = String.valueOf(thumbnail.hashCode());
                CardComponent.ImageComponent imageCom = new CardComponent.ImageComponent(
                        obj.optString(TAG), key,
                        obj.optString(URL), obj.optString(CONTENT_TYPE),
                        obj.optString(MEDIA_SIZE), obj.optString(DURATION));

                if (!TextUtils.isEmpty(thumbnail)) {
                    thumbnailMap.put(key, Base64.decode(thumbnail, Base64.DEFAULT));
                    obj.remove(THUMBNAIL);
                    obj.put(KEY, key);
                }
                imageComponents.add(imageCom);
            } catch (JSONException e) {
// TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }
//
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

	private boolean getBoolean(JSONObject json, String key)
	{
		if (json.has(key))
		{
			try
			{
				return json.getBoolean(key);
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}
    private int getInt(JSONObject json, String key) {
        if (json.has(key)) {
            try {
                return json.getInt(key);
            } catch (JSONException e) {
// TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return 0;
    }

    private String getString(JSONObject json, String key) {
        if (json.has(key)) {
            try {
                return json.getString(key);
            } catch (JSONException e) {
// TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }

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