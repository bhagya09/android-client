package com.bsb.hike.modules.kpt;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.os.Environment;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.platform.content.HikeUnzipTask;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.kpt.adaptxt.beta.AdaptxtSettingsRegisterListener;
import com.kpt.adaptxt.beta.KPTAdaptxtAddonSettings;
import com.kpt.adaptxt.beta.KPTAdaptxtAddonSettings.AdaptxtAddonInstallationListner;
import com.kpt.adaptxt.beta.KPTAddonItem;

public class DictionaryManager implements AdaptxtSettingsRegisterListener
{

	private static final String HIKE_LANGUAGE_DIR_NAME = "lang-dict";

	private final Context context;

	private static volatile DictionaryManager _instance = null;

	KPTAdaptxtAddonSettings kptSettings;

	private boolean kptCoreEngineStatus;

	ArrayList<KPTAddonItem> mLanguagesList;

	ArrayList<KPTAddonItem> mLanguagesWaitingQueue;

	private Byte WAITING = 0;

	private Byte DOWNLOADING = 1;

	private Byte INSTALLING = 2;

	private Byte mState = WAITING;

	public static enum LanguageDictionarySatus
	{
		INSTALLED, UNINSTALLED, UNSUPPORTED, PROCESSING, IN_QUEUE
	}

	private final ConcurrentHashMap<KPTAddonItem, LanguageDictionarySatus> languageStatusMap;

	private DictionaryManager(Context ctx)
	{
		languageStatusMap = new ConcurrentHashMap<KPTAddonItem, LanguageDictionarySatus>();
		mLanguagesList = new ArrayList<KPTAddonItem>();
		kptSettings = new KPTAdaptxtAddonSettings(ctx, this);
		context = ctx;
	}

	public static DictionaryManager getInstance(Context context)
	{
		if (_instance == null)
		{
			synchronized (DictionaryManager.class)
			{
				if (_instance == null)
					_instance = new DictionaryManager(context.getApplicationContext());
			}
		}
		return _instance;
	}

	public ArrayList<KPTAddonItem> getLanguagesList()
	{
		return mLanguagesList;
	}

	public LanguageDictionarySatus getDictionaryLanguageStatus(KPTAddonItem addOnItem)
	{
		return languageStatusMap.get(addOnItem);
	}

	private void fetchAndUpdateLanguages()
	{
		fetchKptLanguagesAndUpdate();
		notifyAllOfLanguageUpdate();
	}

	private void fetchKptLanguagesAndUpdate()
	{
		mLanguagesList.clear();

		List<KPTAddonItem> installedList = kptSettings.getInstalledLanguages();
		for (KPTAddonItem language : installedList)
		{
			languageStatusMap.put(language, LanguageDictionarySatus.INSTALLED);
		}
		mLanguagesList.addAll(installedList);

		List<KPTAddonItem> unInstalledList = kptSettings.getNotInstalledLanguageList();
		for (KPTAddonItem language : unInstalledList)
		{
			if (languageStatusMap.get(language) != LanguageDictionarySatus.IN_QUEUE)
				languageStatusMap.put(language, LanguageDictionarySatus.UNINSTALLED);
		}
		mLanguagesList.addAll(unInstalledList);

		List<KPTAddonItem> UnsupportedList = kptSettings.getUnsupportedLanguagesList();
		for (KPTAddonItem language : UnsupportedList)
		{
			languageStatusMap.put(language, LanguageDictionarySatus.UNSUPPORTED);
		}
		mLanguagesList.addAll(UnsupportedList);
	}

	private void notifyAllOfLanguageUpdate()
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.KPT_LANGUAGES_UPDATED, getLanguagesList());
	}

	public void downloadAndInstallLanguage(KPTAddonItem addOnItem)
	{
		if (languageStatusMap.get(addOnItem) == LanguageDictionarySatus.UNINSTALLED)
		{
			if (mLanguagesWaitingQueue == null)
				mLanguagesWaitingQueue = new ArrayList<KPTAddonItem>();

			mLanguagesWaitingQueue.add(0, addOnItem);
			languageStatusMap.put(addOnItem, LanguageDictionarySatus.IN_QUEUE);
			if (mState == WAITING)
				startProcessing();
		}
	}

	private void startProcessing()
	{
		if (!mLanguagesWaitingQueue.isEmpty())
		{
			KPTAddonItem addOnItem = mLanguagesWaitingQueue.remove(mLanguagesWaitingQueue.size() - 1);
			languageStatusMap.put(addOnItem, LanguageDictionarySatus.PROCESSING);
			downlaodAndUnzip(addOnItem);
		}
		else
			finishProcessing();
	}

	private void finishProcessing()
	{
		mState = WAITING;
	}

	private void processComplete()
	{
		fetchAndUpdateLanguages();
		startProcessing();
	}

	private void downlaodAndUnzip(final KPTAddonItem addOnItem)
	{
		String zipFileName = addOnItem.getZipFileName();
		String fileNameForURL = zipFileName.substring(0, zipFileName.indexOf("_"));
		File dictonaryDirectory = getDictionaryDownloadDirectory();
		if (dictonaryDirectory == null)
		{
			processComplete();
			// use error to show message;
			return;
		}
		final File dictionaryZip = new File(dictonaryDirectory, zipFileName);
		final File dictionaryFile = new File(dictonaryDirectory, addOnItem.getFileName());
		RequestToken token = HttpRequests.kptLanguageDictionaryZipDownloadRequest(dictionaryZip.getAbsolutePath(), AccountUtils.LANGUAGE_DICTIONARY_DOWNLOAD_BASE + fileNameForURL,
				new IRequestListener()
				{
					@Override
					public void onRequestFailure(HttpException httpException)
					{
						processComplete();
						// use error to show message;
					}

					@Override
					public void onRequestSuccess(Response result)
					{
						HikeUnzipTask dictionaryUnzipTask = new HikeUnzipTask(dictionaryZip.getAbsolutePath(), dictionaryFile.getAbsolutePath());
						dictionaryUnzipTask.unzip();
						kptSettings.installAdaptxtAddon(addOnItem, dictionaryFile.getAbsolutePath(), installationListener);
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
			mState = INSTALLING;
		}

		@Override
		public void onInstallationError(String arg0)
		{
			processComplete();
			// show error
		}

		@Override
		public void onInstallationEnded(String arg0)
		{
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
				Logger.d("DictionaryManager", "failed to create directory");
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
		kptCoreEngineStatus = status;
		if (kptCoreEngineStatus)
			fetchAndUpdateLanguages();
	}

	@Override
	public void coreEngineService()
	{
		if (kptCoreEngineStatus)
			fetchAndUpdateLanguages();
	}

}
