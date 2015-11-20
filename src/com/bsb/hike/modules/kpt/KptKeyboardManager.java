package com.bsb.hike.modules.kpt;

import android.content.Context;
import android.os.Environment;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickersearch.StickerLanguagesManager;
import com.bsb.hike.platform.content.HikeUnzipFile;
import com.bsb.hike.utils.Logger;
import com.kpt.adaptxt.beta.AdaptxtSettingsRegisterListener;
import com.kpt.adaptxt.beta.KPTAdaptxtAddonSettings;
import com.kpt.adaptxt.beta.KPTAdaptxtAddonSettings.AdaptxtAddonInstallationListner;
import com.kpt.adaptxt.beta.KPTAdaptxtAddonSettings.AdaptxtAddonUnInstallationListner;
import com.kpt.adaptxt.beta.KPTAddonItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class KptKeyboardManager implements AdaptxtSettingsRegisterListener
{
	public interface KptLanguageInstallErrorHandler
	{
		void onError(String message);
	}

	public static final int PREINSTALLED_LANGUAGE_COUNT = 1;

	public static final Byte WAITING = 0;

	public static final Byte DOWNLOADING = 1;

	public static final Byte INSTALLING = 2;

	private static final String TAG = "KptKeyboardManager";

	private static final String HIKE_LANGUAGE_DIR_NAME = "lang-dict";

	private final Context context;

	private static volatile KptKeyboardManager _instance = null;

	KPTAdaptxtAddonSettings kptSettings;

	private boolean kptCoreEngineStatus;

	ArrayList<KPTAddonItem> mInstalledLanguagesList;

	ArrayList<KPTAddonItem> mUnistalledLanguagesList;

	ArrayList<KPTAddonItem> mUnsupportedLanguagesList;

	ArrayList<KPTAddonItem> mLanguagesWaitingQueue;

	KptLanguageInstallErrorHandler mErrorHandler;

	private volatile Byte mState = WAITING;

	public enum LanguageDictionarySatus
	{
		INSTALLED_LOADED, INSTALLED_UNLOADED, UNINSTALLED, UNSUPPORTED, PROCESSING, IN_QUEUE
	}

	private final ConcurrentHashMap<String, LanguageDictionarySatus> languageStatusMap;

	private KptKeyboardManager(Context ctx)
	{
		Logger.d(TAG,"Initializing...");
		context = ctx;
		languageStatusMap = new ConcurrentHashMap<String, LanguageDictionarySatus>();
		mInstalledLanguagesList = new ArrayList<KPTAddonItem>();
		mUnistalledLanguagesList = new ArrayList<KPTAddonItem>();
		mUnsupportedLanguagesList = new ArrayList<KPTAddonItem>();
		kptSettings = new KPTAdaptxtAddonSettings(ctx, this);
		Logger.d(TAG,"Initialization complete.");
	}

	public static KptKeyboardManager getInstance(Context context)
	{
		if (_instance == null)
		{
			synchronized (KptKeyboardManager.class)
			{
				if (_instance == null)
					_instance = new KptKeyboardManager(context.getApplicationContext());
			}
		}
		return _instance;
	}

	public void setErrorHandler(KptLanguageInstallErrorHandler errorHandler)
	{
		mErrorHandler = errorHandler;
	}

	private void showError(String errorMessage)
	{
		if (mErrorHandler != null)
		{
			mErrorHandler.onError(errorMessage);
		}
	}

	public KPTAddonItem getCurrentLanguageAddonItem()
	{
		if(kptSettings != null)
			return kptSettings.getCurrentAddonItem();

		return null;
	}

	public Byte getCurrentState()
	{
		return mState;
	}

	public ArrayList<KPTAddonItem> getInstalledLanguagesList()
	{
		return mInstalledLanguagesList;
	}

	public ArrayList<KPTAddonItem> getUninstalledLanguagesList()
	{
		return mUnistalledLanguagesList;
	}

	public ArrayList<KPTAddonItem> getUnsupportedLanguagesList()
	{
		return mUnsupportedLanguagesList;
	}
	public LanguageDictionarySatus getDictionaryLanguageStatus(KPTAddonItem addOnItem)
	{
		return languageStatusMap.get(addOnItem.getDisplayName());
	}

	private void fetchAndUpdateLanguages()
	{
		Logger.d(TAG,"fetchAndUpdateLanguages");
		fetchKptLanguagesAndUpdate();
		notifyAllOfLanguageUpdate();
	}

	private void fetchKptLanguagesAndUpdate()
	{
		Logger.d(TAG, "fetchKptLanguagesAndUpdate()");
		if (!kptCoreEngineStatus)
			return;

		mInstalledLanguagesList.clear();

		KPTAdaptxtAddonSettings.KPTLanguageData data = kptSettings.getAllLanguageData();
		List<KPTAddonItem> installedList = data.getInstalledLanguage();
		for (KPTAddonItem language : installedList)
		{
			if (language.getIsLoaded())
			{
				languageStatusMap.put(language.getDisplayName(), LanguageDictionarySatus.INSTALLED_LOADED);
			}
			else
			{
				languageStatusMap.put(language.getDisplayName(), LanguageDictionarySatus.INSTALLED_UNLOADED);
			}
		}
		Logger.d(TAG, "adding installed languages: " + installedList.size());
		mInstalledLanguagesList.addAll(installedList);

		mUnistalledLanguagesList.clear();
		List<KPTAddonItem> unInstalledList = data.getUnInstalledLanguage();
		for (KPTAddonItem language : unInstalledList)
		{
			if (languageStatusMap.get(language.getDisplayName()) != LanguageDictionarySatus.IN_QUEUE)
				languageStatusMap.put(language.getDisplayName(), LanguageDictionarySatus.UNINSTALLED);
		}
		Logger.d(TAG, "adding uninstalled languages: " + unInstalledList.size());
		mUnistalledLanguagesList.addAll(unInstalledList);

		getUnsupportedLanguagesList().clear();
		List<KPTAddonItem> UnsupportedList = data.getUnSupportedLanguage();
		for (KPTAddonItem language : UnsupportedList)
		{
			languageStatusMap.put(language.getDisplayName(), LanguageDictionarySatus.UNSUPPORTED);
		}
		Logger.d(TAG, "adding unsupported languages: " + UnsupportedList.size());
		mUnsupportedLanguagesList.addAll(UnsupportedList);
	}

	private void notifyAllOfLanguageUpdate()
	{
		Logger.d(TAG,"notifyAllOfLanguageUpdate");
		HikeMessengerApp.getPubSub().publish(HikePubSub.KPT_LANGUAGES_UPDATED, null);
	}

	public void downloadAndInstallLanguage(KPTAddonItem addOnItem)
	{
		StickerLanguagesManager.getInstance().downloadTagsForLanguage(new Locale(addOnItem.getlocaleName()).getISO3Language());
		StickerLanguagesManager.getInstance().downloadDefaultTagsForLanguage(new Locale(addOnItem.getlocaleName()).getISO3Language());

		if (languageStatusMap.get(addOnItem.getDisplayName()) == LanguageDictionarySatus.UNINSTALLED)
		{
			if (mLanguagesWaitingQueue == null)
				mLanguagesWaitingQueue = new ArrayList<KPTAddonItem>();

			mLanguagesWaitingQueue.add(0, addOnItem);
			languageStatusMap.put(addOnItem.getDisplayName(), LanguageDictionarySatus.IN_QUEUE);
			if (mState == WAITING)
				startProcessing();
		}
		// this is just for kesting
		else
		{
			if (mLanguagesWaitingQueue == null)
				mLanguagesWaitingQueue = new ArrayList<KPTAddonItem>();
			kptSettings.unInstallAdaptxtAddon(addOnItem, new AdaptxtAddonUnInstallationListner()
			{
				@Override
				public void onUnInstallationStarted(String arg0)
				{
					Logger.d(TAG, "onUnInstallationStarted: " + arg0);
					// TODO Auto-generated method stub
				}

				@Override
				public void onUnInstallationError(String arg0)
				{
					Logger.d(TAG, "onUnInstallationError: " + arg0);
					processComplete();
				}

				@Override
				public void onUnInstallationEnded(String arg0)
				{
					Logger.d(TAG, "onUnInstallationEnded: " + arg0);
					processComplete();
				}
			});
		}

		notifyAllOfLanguageUpdate();
	}

	public void loadInstalledLanguage(KPTAddonItem addOnItem)
	{
		kptSettings.loadDictionary(addOnItem);
		fetchAndUpdateLanguages();
	}

	public void unloadInstalledLanguage(KPTAddonItem addOnItem)
	{
		kptSettings.unLoadDictionary(addOnItem);
		fetchAndUpdateLanguages();
	}

	private void startProcessing()
	{
		if (!mLanguagesWaitingQueue.isEmpty())
		{
			KPTAddonItem addOnItem = mLanguagesWaitingQueue.remove(mLanguagesWaitingQueue.size() - 1);
			languageStatusMap.put(addOnItem.getDisplayName(), LanguageDictionarySatus.PROCESSING);
			downlaodAndUnzip(addOnItem);
		}
		else
			finishProcessing();
	}

	private void finishProcessing()
	{
		mState = WAITING;
		HikeMessengerApp.getPubSub().publish(HikePubSub.KPT_LANGUAGES_INSTALLATION_FINISHED, null);
	}

	private void processComplete()
	{
		fetchAndUpdateLanguages();
		startProcessing();
	}

	private void downlaodAndUnzip(final KPTAddonItem addOnItem)
	{
		final String zipFileName = addOnItem.getZipFileName();
		String fileNameForURL = zipFileName.substring(0, zipFileName.indexOf("_")).toLowerCase();
		final File dictonaryDirectory = getDictionaryDownloadDirectory();
		if (dictonaryDirectory == null)
		{
			processComplete();
			showError(context.getString(R.string.out_of_space));
			return;
		}
		final File dictionaryZip = new File(dictonaryDirectory, zipFileName);
		RequestToken token = HttpRequests.kptLanguageDictionaryZipDownloadRequest(dictionaryZip.getAbsolutePath(), HttpRequestConstants.getLanguageDictionaryBaseUrl() + fileNameForURL,
				new IRequestListener()
				{
					@Override
					public void onRequestFailure(HttpException httpException)
					{
						httpException.printStackTrace();
						processComplete();
						showError(context.getString(R.string.download_failed));
					}

					@Override
					public void onRequestSuccess(Response result)
					{
						HikeUnzipFile dictionaryUnzipTask = new HikeUnzipFile(dictionaryZip.getAbsolutePath(), dictonaryDirectory.getAbsolutePath());
						dictionaryUnzipTask.unzip();
						dictionaryZip.delete();
						File atpfile = new File(dictonaryDirectory,zipFileName.replace(".zip", ".atp"));
						kptSettings.installAdaptxtAddon(addOnItem, atpfile.getAbsolutePath(), installationListener);
					}

					@Override
					public void onRequestProgressUpdate(float progress)
					{
						// do nothing
					}
				});

		if (!token.isRequestRunning())
		{
			mState = DOWNLOADING;
			token.execute();
		}
	}

	AdaptxtAddonInstallationListner installationListener = new AdaptxtAddonInstallationListner()
	{
		@Override
		public void onInstallationStarted(String arg0)
		{
			Logger.d(TAG,"onInstallationStarted: " + arg0);
			mState = INSTALLING;
		}

		@Override
		public void onInstallationError(String arg0)
		{
			Logger.d(TAG,"onInstallationError: " + arg0);
			processComplete();
			showError(context.getString(R.string.some_error));
		}

		@Override
		public void onInstallationEnded(String arg0)
		{
			Logger.d(TAG,"onInstallationEnded: " + arg0);
			processComplete();
			// show success message
		}
	};

	private File getDictionaryDownloadDirectory()
	{
		File hikeDir = context.getExternalFilesDir(null);
		if (hikeDir == null)
		{
			FTAnalyticEvents.logDevError(FTAnalyticEvents.UNABLE_TO_CREATE_LANG_DIC_DIR, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "File",
					"Hike dir is null when external storage state is - " + Environment.getExternalStorageState());
			return null;
		}
		File hikeLangDir = new File(hikeDir, HIKE_LANGUAGE_DIR_NAME);
		if (hikeLangDir != null && !hikeLangDir.exists())
		{
			if (!hikeLangDir.mkdirs())
			{
				Logger.d("KptKeyboardManager", "failed to create directory");
				FTAnalyticEvents.logDevError(FTAnalyticEvents.UNABLE_TO_CREATE_LANG_DIC_DIR, 0, FTAnalyticEvents.DOWNLOAD_FILE_TASK, "File",
						"Unable to create language dictionary dir when external storage state is - " + Environment.getExternalStorageState());
				return null;
			}
		}
		return hikeLangDir;
	}

	@Override
	public void coreEngineStatus(boolean status)
	{
		Logger.d(TAG,"coreEngineStatus callback: " + status);
		kptCoreEngineStatus = status;
		fetchKptLanguagesAndUpdate();
	}

	@Override
	public void coreEngineService()
	{
		Logger.d(TAG,"coreEngineService callback");
	}

	public KPTAdaptxtAddonSettings getKptSettings()
	{
		if (kptSettings != null)
		{
			return kptSettings;
		}
		return null;
	}
}
