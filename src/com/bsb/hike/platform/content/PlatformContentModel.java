package com.bsb.hike.platform.content;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.utils.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

/**
 * Content model
 */
public class PlatformContentModel
{
	private static String TAG = "PlatformContentModel";

	boolean isForwardCard;

	/**
	 * The hot data.
	 */
	private String formedData;

	private int mHash = -1;
	private int mTemplateHash = -1;

	@Expose
	public PlatformCardObjectModel cardObj;

	@Expose
	public PlatformCardObjectModel fwdCardObj;

	private int mAppHash = -1;

	private int uniqueId;

	public String target_platform;

	private static PlatformContentModel object = null;

    private byte requestType = HikePlatformConstants.PlatformMappRequestType.HIKE_MICRO_APPS;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		if (mHash == -1)
		{
			mHash = new String(cardObj.appName + cardObj.layoutId + cardObj.appVersion + cardObj.ld).hashCode();
		}
		return mHash;
	}

	/**
	 * Template hash code. Replace with template UID
	 * 
	 * @return the int
	 */
	public int templateHashCode()
	{
		if (mTemplateHash == -1)
		{
			mTemplateHash = new String(cardObj.layoutId + cardObj.appVersion + cardObj.appName).hashCode();
		}
		return mTemplateHash;
	}

	/**
	 * Template hash code. Replace with template UID
	 * 
	 * @return the int
	 */
	public int appHashCode()
	{
		if (mAppHash == -1)
		{
			mAppHash = new String(cardObj.appName + cardObj.appVersion).hashCode();
		}
		return mAppHash;
	}

	public static PlatformContentModel make(String contentData){
		return make(0, contentData);
	}

    public static PlatformContentModel make(String contentData,byte requestType){
        return make(0, contentData,requestType);
    }

	/**
	 * Make.
	 * 
	 * @param card_data
	 *            the content data
	 * @return the platform content model
	 */
	public static PlatformContentModel make(int unique,String contentData)
	{
		Logger.d(TAG, "making PlatformContentModel");
		JsonParser parser = new JsonParser();
		JSONObject json = null;

        try {
			json = new JSONObject(contentData);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		JsonObject gsonObj = (JsonObject) parser.parse(contentData);

		try
		{
            JSONObject cardObj = json.optJSONObject(HikePlatformConstants.CARD_OBJECT);
            String compatibilityMapStr = cardObj.optString(HikePlatformConstants.COMPATIBILITY_MAP);
            object = new Gson().fromJson(gsonObj, PlatformContentModel.class);
            object.cardObj.setCompatibilityMap(compatibilityMapStr);
			if (object.cardObj.getLd() != null)
			{
                int microAppVersionCode = object.cardObj.getMappVersionCode();
                String microApp = object.cardObj.getMicroApp();
                String unzipPath = PlatformContentConstants.HIKE_MICRO_APPS;
                String basePath = PlatformUtils.generateMappUnZipPathForBotRequestType(HikePlatformConstants.PlatformMappRequestType.HIKE_MICRO_APPS,unzipPath,microApp,microAppVersionCode);

				object.cardObj.ld
						.addProperty(PlatformContentConstants.KEY_TEMPLATE_PATH, PlatformContentConstants.CONTENT_AUTHORITY_BASE + basePath);
				object.cardObj.ld.addProperty(PlatformContentConstants.MESSAGE_ID, Integer.toString(unique));
				object.cardObj.ld.addProperty(HikePlatformConstants.PLATFORM_VERSION, HikePlatformConstants.CURRENT_VERSION);

			}
		}
		catch (JsonParseException e)
		{
			e.printStackTrace();
			return null;
		}
		catch (IllegalArgumentException iae)
		{
			iae.printStackTrace();
			return null;
		}
		catch (Exception e)
		{
			// We dont want app to crash, instead safely provide control in onFailure
			e.printStackTrace();
			return null;
		}

		return object;
	}


	/**
	 * Make.
	 *
	 * @param card_data
	 *            the content data
	 * @return the platform content model
	 */
	public static PlatformContentModel make(int unique, String contentData, byte requestType)
	{
		Logger.d(TAG, "making PlatformContentModel");
		JsonParser parser = new JsonParser();
		JSONObject json = null;

		try
		{
			json = new JSONObject(contentData);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		JsonObject gsonObj = (JsonObject) parser.parse(contentData);

		try
		{
			JSONObject cardObj = json.optJSONObject(HikePlatformConstants.CARD_OBJECT);
			String compatibilityMapStr = cardObj.optString(HikePlatformConstants.COMPATIBILITY_MAP);
			object = new Gson().fromJson(gsonObj, PlatformContentModel.class);
			object.cardObj.setCompatibilityMap(compatibilityMapStr);
			if (object.cardObj.getLd() != null)
			{
                int microAppVersionCode = object.cardObj.getMappVersionCode();
                String microApp = object.cardObj.getMicroApp();
                String unzipPath = PlatformContentConstants.HIKE_MICRO_APPS;
                String basePath = PlatformUtils.generateMappUnZipPathForBotRequestType(requestType,unzipPath,microApp,microAppVersionCode);

                object.cardObj.ld.addProperty(PlatformContentConstants.KEY_TEMPLATE_PATH, PlatformContentConstants.CONTENT_AUTHORITY_BASE + basePath);
				object.cardObj.ld.addProperty(PlatformContentConstants.MESSAGE_ID, Integer.toString(unique));
				object.cardObj.ld.addProperty(HikePlatformConstants.PLATFORM_VERSION, HikePlatformConstants.CURRENT_VERSION);
			}
		}
		catch (JsonParseException e)
		{
			e.printStackTrace();
			return null;
		}
		catch (IllegalArgumentException iae)
		{
			iae.printStackTrace();
			return null;
		}
		catch (Exception e)
		{
			// We dont want app to crash, instead safely provide control in onFailure
			e.printStackTrace();
			return null;
		}

		return object;
	}

	public static String getForwardData(String originalData)
	{
		String forwardData = null;

		PlatformContentModel originalModel = make(originalData);

		mergeObjects(originalModel.cardObj, originalModel.fwdCardObj);

		originalModel.fwdCardObj = null;

		forwardData = new Gson().toJson(originalModel);

		return forwardData;
	}

	private static boolean mergeObjects(Object toObj, Object fromObj)
	{
		if (toObj != null && fromObj != null)
		{
			Field[] fields = PlatformCardObjectModel.class.getDeclaredFields();

			int fieldLength = fields.length;

			for (int i = 0; i < fieldLength; i++)
			{
				try
				{
					Field field = fields[i];

					if (field.getType().equals(JsonObject.class))
					{
						JsonObject fromJsonObject = (JsonObject) field.get(fromObj);

						JsonObject toJsonObject = (JsonObject) field.get(toObj);

						if (fromJsonObject == null || toJsonObject == null)
						{
							continue;
						}

						Set<Entry<String, JsonElement>> set = fromJsonObject.entrySet();

						Iterator<Entry<String, JsonElement>> setIterator = set.iterator();

						while (setIterator.hasNext())
						{
							Entry<String, JsonElement> entry = setIterator.next();
							toJsonObject.add(entry.getKey(), entry.getValue());
						}
					}
					else
					{
						Object fwdCardFieldValue = field.get(fromObj);

						if (fwdCardFieldValue != null)
						{
							field.set(toObj, fwdCardFieldValue);
						}
					}
				}
				catch (IllegalAccessException iae)
				{
					iae.printStackTrace();
				}
			}
		}
		return true;
	}

	/**
	 * Gets the layout_id.
	 * 
	 * @return the layout_id
	 */
	public String getTag()
	{
		return cardObj.layoutId;
	}

	/**
	 * Gets the version.
	 * 
	 * @return the version
	 */
	public String getVersion()
	{
		return cardObj.appVersion;
	}

    /**
     * Gets the version.
     *
     * @return the version
     */
    public int getMappVersionCode()
    {
        return cardObj.mappVersionCode;
    }

	/**
	 * Gets the content data.
	 * 
	 * @return the content data
	 */
	public String getContentJSON()
	{
		return cardObj.ld.toString();
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

	/**
	 * Sets the hot data.
	 * 
	 * @param compiledData
	 *            the new hot data
	 */
	public void setFormedData(String compiledData)
	{
		this.formedData = compiledData;
	}

	/**
	 * Gets the appID. This is same as the name of folder in which HTML templates are saved.
	 * 
	 * @return the appID
	 */
	public String getId()
	{
		return cardObj.appName;
	}

	/**
	 * Sets the appID.
	 * 
	 * @param appID
	 *            the new appID
	 */
	public void setId(String id)
	{
		this.cardObj.appName = id;
	}

	private PlatformContentModel()
	{
		// Cannot make objects directly
	}
	
	public void setUniqueId(int uniqueId)
	{
		this.uniqueId = uniqueId;
	}
	
	public int getUniqueId()
	{
		return uniqueId;
	}

	public String getPid()
	{
		if (cardObj.ld != null && cardObj.ld.has(ProductPopupsConstants.PID))
		{
			return cardObj.ld.get(ProductPopupsConstants.PID).toString();
		}
		else
		{
			return "";
		}
	}

	@Override
	public String toString()
	{
		return "" + cardObj.ld + formedData;
	}

	public String getLayout_url()
	{
		return cardObj.appPackage;
	}

    public byte getRequestType() {
        return requestType;
    }

    public void setRequestType(byte requestType) {
        this.requestType = requestType;
    }

    public class PlatformCardObjectModel
	{

		public String getAppName()
		{
			return appName;
		}

		public void setAppName(String appName)
		{
			this.appName = appName;
		}

		public String getAppVersion()
		{
			return appVersion;
		}

		public void setAppVersion(String appVersion)
		{
			this.appVersion = appVersion;
		}

        public int getMappVersionCode()
        {
            return mappVersionCode;
        }

        public void setMappVersionCode(int mappVersionCode)
        {
            this.mappVersionCode = mappVersionCode;
        }

		public String getLayoutId()
		{
			return layoutId;
		}

		public void setLayoutId(String layoutId)
		{
			this.layoutId = layoutId;
		}

		public String getAppPackage()
		{
			return appPackage;
		}

		public void setAppPackage(String appPackage)
		{
			this.appPackage = appPackage;
		}

		public String getPush()
		{
			return push;
		}

		public void setPush(String push)
		{
			this.push = push;
		}

		public String getUser()
		{
			return user;
		}

		public void setUser(String user)
		{
			this.user = user;
		}

		public String getHm()
		{
			return hm;
		}

		public void setHm(String hm)
		{
			this.hm = hm;
		}

		public JsonObject getLd()
		{
			return ld;
		}

		public void setLd(JsonObject ld)
		{
			this.ld = ld;
		}

		public JsonObject getHd()
		{
			return hd;
		}

		public void setHd(JsonObject hd)
		{
			this.hd = hd;
		}

		public void setCompatibilityMap(String compatibilityMapStr)
		{
			this.compatibilityMap = getMapFromString(compatibilityMapStr);
		}

		public TreeMap<Integer,Integer> getCompatibilityMap()
		{
			return compatibilityMap;
		}

		/*
		 * Code to generate Compatibility matrix TreeMap from json
		 */
		private TreeMap<Integer,Integer> getMapFromString(String json){
			Gson gson = new Gson();
			Type treeMap = new TypeToken<TreeMap<Integer, Integer>>(){}.getType();
			TreeMap<Integer,Integer> map = gson.fromJson(json, treeMap);
			return map;
		}

		public String getH()
		{
			return h;
		}

		public void setH(String h)
		{
			this.h = h;
		}

		public String getnotifText()
		{
			return notifText;
		}

		public void setnotifText(String notifText)
		{
			this.notifText = notifText;
		}

		public String getMicroApp()
		{
			return microApp;
		}

		public void setMicroApp(String microApp)
		{
			this.microApp = microApp;
		}
		
		@Expose
		public String appName;

		@Expose
		public String appVersion;

        @Expose
        public int mappVersionCode;

		@Expose
		public String layoutId;

		@Expose
		public String appPackage;

		@Expose
		public String push;

		@Expose
		public String user;

		@Expose
		public String hm;

		@Expose
		public JsonObject ld;

		@Expose
		public JsonObject hd;

		@Expose
		public String h;
		
		@Expose
		public String notifText;

		@Expose
		public String parent_msisdn;

		@Expose
		public boolean replace;

		@Expose
		public boolean lpd;

		@Expose
		public TreeMap<Integer,Integer> compatibilityMap;

        @Expose
        public String appMarketVersion;

        @Expose
        public String microApp;

	}

}