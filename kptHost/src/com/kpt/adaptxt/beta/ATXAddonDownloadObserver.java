package com.kpt.adaptxt.beta;

/**
 *  Observer interface for communicating the download status information to the registered parties.
 */
public interface ATXAddonDownloadObserver {
    /**
     * API to report that the download for a language is started.
     */
    public void downloadStared(KPTAddonItem lang);

    /**
     * API to report the download progress update information(% downloaded information) 
     * for a language.
     */
    public void downloadProgressing(KPTAddonItem lang, Integer percentage);

    /**
     * API to report that the download of a language is completed
     */
    public void downloadCompleted(KPTAddonItem lang);

    /**
     * API to report if there is any error in the download process.
     */
    public void downloadError(KPTAddonItem lang, String errorMsg, Exception e);
}
