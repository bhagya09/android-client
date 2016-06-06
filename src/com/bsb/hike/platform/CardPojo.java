package com.bsb.hike.platform;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import com.bsb.hike.models.HikeFile;
import com.bsb.hike.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by varunarora on 20/05/16.
 */
public class CardPojo implements HikePlatformConstants{
    public HashMap<String, byte[]> thumbnailMap = new HashMap<String, byte[]>();
    public List<CardComponent.TextComponent> textComponents = new ArrayList<CardComponent.TextComponent>();
    public List<CardComponent.MediaComponent> mediaComponents = new ArrayList<CardComponent.MediaComponent>();
    public ArrayList<CardComponent.ActionComponent> actionComponents = new ArrayList<CardComponent.ActionComponent>();
    public List<CardComponent.ImageComponent> imageComponents = new ArrayList<CardComponent.ImageComponent>();
    public String backgroundColor;
    public String background;
    public CardComponent.ActionComponent cardAction;
    private List<HikeFile> hikeFileList;
    private JSONObject cardJSON;
    public CardPojo(JSONObject metadata, List<HikeFile> hikeFileList) {
        cardJSON = metadata;
        this.hikeFileList = hikeFileList;
        backgroundColor = PlatformUtils.getString(metadata, BACKGROUND_COLOR);
        background = PlatformUtils.getString(metadata, BACKGROUND);
        try {
            if (metadata.has(CARD_ACTION)) {
                JSONObject cardActionObj = metadata.optJSONObject(CARD_ACTION);
                cardAction = new CardComponent.ActionComponent(cardActionObj.getString(ACTION), cardActionObj.optString(ACTION_TEXT), cardActionObj.optJSONObject(ACTION_EXTRA));
            }
            if (metadata.has(ASSETS)) {
                JSONObject assets = metadata.optJSONObject(ASSETS);
                if (assets.has(TEXTS)) {
                    parseTextComponents(assets.getJSONArray(TEXTS));
                }
                if (assets.has(IMAGES)) {
                    parseImageComponents(assets.getJSONArray(IMAGES));
                }
                if (assets.has(ACTIONS)) {
                    parseActionComponents(assets.getJSONArray(ACTIONS));
                }

                if (assets.has(MEDIA)) {
                    parseMediaComponents(assets.getJSONArray(MEDIA));
                }

            }
        }catch (JSONException e) {
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
                CardComponent.TextComponent textCom = new CardComponent.TextComponent.Builder(obj.optString(TAG)).setText(obj.optString(TEXT)).setTextColor(obj.optString(TEXT_COLOR))
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

    public JSONObject getCardJSON(){
        return cardJSON;
    }
}
