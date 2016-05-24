package com.bsb.hike.platform;

import java.io.File;

import android.support.v4.util.LruCache;
import android.text.TextUtils;

import com.bsb.hike.platform.ContentModules.PlatformContentModel;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.platform.content.PlatformRequestManager;
import com.bsb.hike.utils.Logger;
import com.samskivert.mustache.Template;

/**
 * Responsible for maintaining cache for formed data and pre-compiled templates.
 */
public class PlatformContentCache
{

	private static String TAG = "PlatformContentCache";

	private static int formedContentCacheSize = 4 * 1024 * 1024; // 4MB

	private static int templateCacheSize = 6; // Number of templates

	private static LruCache<Integer, PlatformContentModel> formedContentCache = new LruCache<Integer, PlatformContentModel>(formedContentCacheSize)
	{
		protected int sizeOf(Integer key, PlatformContentModel value)
		{
			return value.toString().getBytes().length;
		};
	};

	private static LruCache<Integer, Template> templateCache = new LruCache<Integer, Template>(templateCacheSize)
	{
		protected int sizeOf(Integer key, Template value)
		{
			return 1;
		};
	};

	/**
	 * Instantiates a new platform content cache.
	 */
	private PlatformContentCache()
	{

	}

	/**
	 * Gets the template.
	 * 
	 * @param content
	 *            the content
	 * @return the template
	 */
	public static Template getTemplate(PlatformContentRequest content)
	{
		Template template = templateCache.get(content.getContentData().templateHashCode());

		Logger.d(TAG, "getting template - " + content.getContentData().getContentJSON());

		if (template == null)
		{
			template = loadTemplateFromDisk(content);
		}

		return template;
	}

	/**
	 * Put template.
	 * 
	 * @param hashCode
	 *            the hash code
	 * @param template
	 *            the template
	 */
	public static void putTemplate(int hashCode, Template template)
	{
		Logger.d(TAG, "putting template in cache");
		if (template != null)
		{
			templateCache.put(hashCode, template);
		}
	}

	/**
	 * Load template from disk.
	 * 
	 * @param content
	 *            the content
	 * @return the template or null if the template is not found on disk
	 */
	private static Template loadTemplateFromDisk(final PlatformContentRequest content)
	{
		Logger.d(TAG, "loading template from disk");

        IExceptionHandler exceptionHandler = new IExceptionHandler()
        {
            @Override
            public void onExceptionOcurred(Exception ex)
            {
                Logger.wtf(TAG, "Got an  exception while reading from disk." + ex.toString());
                PlatformUtils.microappIOFailedAnalytics(content.getContentData().getId(), ex.toString(), true);
            }
        };

		String microAppPath = PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.HIKE_MICRO_APPS;
		String microAppName = content.getContentData().cardObj.getAppName();

        microAppPath = PlatformUtils.generateMappUnZipPathForBotType(content.getBotType(),microAppPath,microAppName);

		File file = new File(microAppPath, content.getContentData().getTag());

		// If file is not found in the newer structured hierarchy directory path, then look for file in the older content directory path used before versioning
		if (!file.exists())
		{
			file = new File(PlatformContentConstants.PLATFORM_CONTENT_OLD_DIR + content.getContentData().getId(), content.getContentData().getTag());
		}

		String templateString;
		templateString = PlatformContentUtils.readDataFromFile(file, exceptionHandler);

		if (TextUtils.isEmpty(templateString))
		{
			return null;
		}

		Logger.d(TAG, "loading template from disk - complete");

		Template downloadedTemplate = PlatformTemplateEngine.compileTemplate(templateString, exceptionHandler);

		if (downloadedTemplate == null)
		{
			PlatformRequestManager.reportFailure(content, PlatformContent.EventCode.INVALID_DATA);
			PlatformRequestManager.remove(content);
		}
		else
		{
			templateCache.put(content.getContentData().templateHashCode(), downloadedTemplate);
		}

		return downloadedTemplate;
	}

	/**
	 * Put hot content.
	 * 
	 * @param content
	 *            the content
	 */
	public static void putFormedContent(PlatformContentModel content)
	{
		Logger.d(TAG, "put formed content in cache");

		formedContentCache.put(content.hashCode(), content);
	}

	/**
	 * Gets the formed content.
	 * 
	 * @param content
	 *            the content
	 * @return the hot content
	 */
	public static PlatformContentModel getFormedContent(PlatformContentRequest request)
	{
		Logger.d(TAG, "get formed content from cache");

		return formedContentCache.get(request.getContentData().hashCode());
	}
    
	/**
	 * Clear formed content cache. Code only to be executed when we are clearing client data
	 */
	public static void clearFormedContentCache()
    {
        formedContentCache.evictAll();
    }

	public interface IExceptionHandler
	{
		public void onExceptionOcurred(Exception ex);
	}
}
