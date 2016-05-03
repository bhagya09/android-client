package com.bsb.hike.platform.ContentModules;

import android.text.TextUtils;

import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.utils.CustomAnnotation.DoNotObfuscate;
import com.bsb.hike.utils.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.annotations.Expose;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Content model
 */
@DoNotObfuscate
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

	private byte botType = HikePlatformConstants.PlatformBotType.WEB_MICRO_APPS;

	private String msisdn = "";

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
			mHash = new String(cardObj.appName + cardObj.layoutId + cardObj.mAppVersionCode + cardObj.ld).hashCode();
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
			mTemplateHash = new String(cardObj.layoutId + cardObj.mAppVersionCode + cardObj.appName).hashCode();
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
			mAppHash = new String(cardObj.appName + cardObj.mAppVersionCode).hashCode();
		}
		return mAppHash;
	}

	public static PlatformContentModel make(String contentData)
	{
		return make(0, contentData,HikePlatformConstants.PlatformBotType.WEB_MICRO_APPS);
	}

	public static PlatformContentModel make(String contentData, byte botType)
	{
		return make(0, contentData, botType);
	}

    /**
	 * Make.
	 *
	 * @param contentData
	 *            the content data
	 * @return the platform content model
	 */
	public static PlatformContentModel make(int unique, String contentData, byte botType)
	{
		Logger.d(TAG, "making PlatformContentModel");
		JsonParser parser = new JsonParser();
		JsonObject gsonObj = (JsonObject) parser.parse(contentData);

		try
		{
			object = new Gson().fromJson(gsonObj, PlatformContentModel.class);
			if (object.cardObj.getLd() != null)
			{
				String microApp = object.cardObj.getAppName();
                // Precautionary check for is Micro App Name check
                if(TextUtils.isEmpty(microApp))
                    return null;

				String unzipPath = PlatformContentConstants.HIKE_MICRO_APPS;
                // Keeping default code path as newer hierarichal versioning path
				String basePath = PlatformUtils.generateMappUnZipPathForBotType(botType, unzipPath, microApp);
                String platformSDKPath = PlatformUtils.generateMappUnZipPathForBotType(HikePlatformConstants.PlatformBotType.HIKE_MAPPS, unzipPath, HikePlatformConstants.PLATFORM_WEB_SDK);

                // Check if micro app is present in newer versioning path, else check for micro app in old content directory
                if(new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformUtils.generateMappUnZipPathForBotType(botType, unzipPath, microApp)).exists())
                    basePath = PlatformUtils.generateMappUnZipPathForBotType(botType, unzipPath, microApp);
                else if(new File(PlatformContentConstants.PLATFORM_CONTENT_OLD_DIR + microApp).exists())
                    basePath = microApp + File.separator;

                object.cardObj.ld.addProperty(PlatformContentConstants.KEY_TEMPLATE_PATH, PlatformContentConstants.CONTENT_AUTHORITY_BASE + basePath);
				object.cardObj.ld.addProperty(PlatformContentConstants.MESSAGE_ID, Integer.toString(unique));
				object.cardObj.ld.addProperty(HikePlatformConstants.PLATFORM_VERSION, HikePlatformConstants.CURRENT_VERSION);
                object.cardObj.ld.addProperty(HikePlatformConstants.PLATFORM_SDK_PATH,PlatformContentConstants.CONTENT_AUTHORITY_BASE + platformSDKPath);
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
	 * @param id`
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

	public JsonArray getLanguageData()
	{
		return cardObj.lan_array;
	}

	@Override
	public String toString()
	{
		return "" + cardObj.ld + formedData;
	}

	public String getLayout_url()
	{
		if(!TextUtils.isEmpty(cardObj.appPackageV2))
            return cardObj.appPackageV2;
        else
            return cardObj.appPackage;
	}

	public byte getBotType()
	{
		return botType;
	}

	public void setBotType(byte botType)
	{
		this.botType = botType;
	}

	public String getMsisdn()
	{
		return msisdn;
	}

	public void setMsisdn(String msisdn)
	{
		this.msisdn = msisdn;
	}

	@DoNotObfuscate
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

		public int getmAppVersionCode()
		{
			return mAppVersionCode;
		}

		public void setmAppVersionCode(int mAppVersionCode)
		{
			this.mAppVersionCode = mAppVersionCode;
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

        public String getAppPackageV2()
        {
            return appPackage;
        }

        public void setAppPackageV2(String appPackageV2)
        {
            this.appPackageV2 = appPackageV2;
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

		@Expose
		public String appName;

		@Expose
		public int mAppVersionCode;

		@Expose
		public String layoutId;

		@Expose
		public String appPackage;

        @Expose
        public String appPackageV2;

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
		public Boolean replace;

		@Expose
		public Boolean lpd;

		@Expose
		public JsonArray lan_array;

	}

}