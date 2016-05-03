package com.bsb.hike.productpopup;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.platform.ContentModules.PlatformContentModel;
import com.bsb.hike.utils.CustomAnnotation.DoNotObfuscate;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Comparator;

import static com.bsb.hike.productpopup.ProductPopupsConstants.END_TIME;
import static com.bsb.hike.productpopup.ProductPopupsConstants.IS_CANCELLABLE;
import static com.bsb.hike.productpopup.ProductPopupsConstants.IS_FULL_SCREEN;
import static com.bsb.hike.productpopup.ProductPopupsConstants.PUSH_TIME;
import static com.bsb.hike.productpopup.ProductPopupsConstants.PopupTriggerPoints;
import static com.bsb.hike.productpopup.ProductPopupsConstants.PushTypeEnum;
import static com.bsb.hike.productpopup.ProductPopupsConstants.START_TIME;
import static com.bsb.hike.productpopup.ProductPopupsConstants.TRIGGER_POINT;

@DoNotObfuscate
public class ProductContentModel implements Parcelable
{
	public PlatformContentModel mmContentModel;

	private int triggerpoint;

	private long starttime;

	private long endtime;

	private boolean isFullScreen;

	private int hashCode = -1;

	private String formedData;

	private String notifTitle;

	private long pushTime;
	
	private boolean isCancellable;
	
	private String pid;
	
	private PopupConfiguration popupConfiguration;

	JSONObject notificationData=null;
	
	private int config;

	private ProductContentModel(JSONObject contentData) {
		this.mmContentModel = PlatformContentModel.make(contentData.toString());

		starttime = contentData.optLong(START_TIME, 0l);
		endtime = contentData.optLong(END_TIME, new Long(Integer.MAX_VALUE));
		triggerpoint = contentData.optInt(TRIGGER_POINT, PopupTriggerPoints.HOME_SCREEN.ordinal());
		isFullScreen = contentData.optBoolean(IS_FULL_SCREEN, false);
		pushTime = contentData.optLong(PUSH_TIME, 0l);
		isCancellable = contentData.optBoolean(IS_CANCELLABLE, false);
		pid = mmContentModel.getPid();
		config = contentData.optInt(HikeConstants.CONFIGURATION, 0);
		popupConfiguration = new PopupConfiguration(config);


		Logger.d("productpopup", "Notification language data is " + mmContentModel.getLanguageData());

		if (mmContentModel.getLanguageData() != null) {
			notificationData = Utils.getDataBasedOnAppLanguage(new Gson().toJson(mmContentModel.getLanguageData()));

			Logger.d("productpopup", notificationData == null ? "data is null" : notificationData.toString());
		}

	}

	public PopupConfiguration getConfig()
	{
		return popupConfiguration;
	}
	
	public static ProductContentModel makeProductContentModel(JSONObject contentData)
	{
		return new ProductContentModel(contentData);

	}

	/**
	 * @return the triggerpoint
	 */
	public int getTriggerpoint()
	{
		return triggerpoint;
	}

	/**
	 * @return the starttime
	 */
	public long getStarttime()
	{
		return starttime;
	}

	/**
	 * @return the endtime
	 */
	public long getEndtime()
	{
		return endtime;
	}

	public String getAppName()
	{
		return mmContentModel.cardObj.getAppName();
	}
	
	public String getPid()
	{
		return pid;
	}

	public String getLayoutId()
	{
		return mmContentModel.cardObj.getLayoutId();
	}

	public String getAppPackage()
	{
		return mmContentModel.cardObj.getAppPackage();
	}

	public boolean shouldPlaySound()
	{
		String text = mmContentModel.cardObj.getPush();

		if (PushTypeEnum.getEnum(text)== PushTypeEnum.LOUD)
		{
			return true;
		}

		return false;
	}

	public String getUser() {
		String title = null;
		if (notificationData != null) {
			title = notificationData.optString(HikeConstants.NOTIFICATION_TITLE);
			if (TextUtils.isEmpty(title)) {
				title = mmContentModel.cardObj.getUser();
			}
		} else {
			title = mmContentModel.cardObj.getUser();
		}
		return title;
	}

	public JsonObject getLd()
	{
		return mmContentModel.cardObj.getLd();
	}

	public int getHeight()
	{
		return Integer.parseInt(mmContentModel.cardObj.getH());
	}

	public boolean isFullScreen()
	{
		return isFullScreen;
	}

	public void setFormedData(String compiledData)
	{
		this.formedData = compiledData;
	}

	public String getNotifTitle() {
		String notifText = null;
		if (notificationData != null) {
			notifText = notificationData.optString(HikeConstants.NOTIFICATION_TEXT);
			if (TextUtils.isEmpty(notifText)) {
				notifText = mmContentModel.cardObj.getnotifText();
			}
		} else {
			notifText = mmContentModel.cardObj.getnotifText();
		}
		return notifText;
	}

	/**
	 * Gets the hot data.
	 * 
	 * @return the hot data
	 */
	public String getFormedData()
	{
		return formedData;
	}

	public long getPushTime()
	{
		return pushTime;
	}
	
	public boolean isCancellable()
	{
		return isCancellable;
	}

	@Override
	public int hashCode()
	{
		if (hashCode == -1)
		{
			hashCode = new String(getStarttime() + getTriggerpoint() + "").hashCode();
		}
		return hashCode;
	}

	public String toJSONString()
	{
		Gson gson = new Gson();
		String str = gson.toJson(mmContentModel);
		JsonParser parser = new JsonParser();
		JsonObject jsonObj = (JsonObject) parser.parse(str);

		jsonObj.addProperty(START_TIME, starttime);
		jsonObj.addProperty(END_TIME, endtime);
		jsonObj.addProperty(TRIGGER_POINT, triggerpoint);
		jsonObj.addProperty(IS_FULL_SCREEN, isFullScreen);
		jsonObj.addProperty(PUSH_TIME,pushTime);
		jsonObj.addProperty(IS_CANCELLABLE, isCancellable);
		jsonObj.addProperty(HikeConstants.CONFIGURATION, config);
		return jsonObj.toString();
	}

	
	/**
	 * Comparater to sort the popups based on the start time ...
	 */
	public static Comparator<ProductContentModel> ProductContentStartTimeComp = new Comparator<ProductContentModel>()
	{
		public int compare(ProductContentModel lhs, ProductContentModel rhs)
		{

			if (rhs.getStarttime() - lhs.getStarttime() > 0)
			{
				return 1;
			}
			else
				return -1;

		}
	};

	/**
	 * Validating the push
	 * @return
	 */
	
	public boolean isPushReceived()
	{	
		if (!TextUtils.isEmpty(getUser()) && !TextUtils.isEmpty(getNotifTitle()) && endtime>System.currentTimeMillis())
		{
			return true;
		}
		return false;
	}

	public boolean isPushFuture()
	{
		if (pushTime != 0l)
		{
			return true;
		}
		else
			return false;
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(toJSONString());
	}
	
	public static final Parcelable.Creator<ProductContentModel> CREATOR = new Parcelable.Creator<ProductContentModel>()
	{
		public ProductContentModel createFromParcel(Parcel in)
		{
			String data = in.readString();

			JSONObject mmObject = null;
			try
			{
				mmObject = new JSONObject(data);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}

			return ProductContentModel.makeProductContentModel(mmObject);
		}

		public ProductContentModel[] newArray(int size)
		{
			return new ProductContentModel[size];
		}
	};

}
