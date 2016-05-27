package com.bsb.hike.analytics;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import com.bsb.hike.utils.AccountUtils;

import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants.AppOpenSource;
import com.bsb.hike.media.ShareablePopupLayout;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.hike.abtest.ABTest;

/**
 * @author rajesh
 *         This is the class exposed for analytics api instrumentation in clients
 */
public class HAManager {
    /**
     * enum specifying the priority type of the analytics event
     */
    public enum EventPriority {
        NORMAL,
        HIGH
    }

    private static HAManager instance;

    private Context context;

    public static final String ANALYTICS_SETTINGS = "analyticssettings";

    private boolean isAnalyticsEnabled = true;

    private int analyticsSendFreq = AnalyticsConstants.DEFAULT_SEND_FREQUENCY;

    private int maxInMemorySize = AnalyticsConstants.MAX_EVENTS_IN_MEMORY;

    private long hourToSend;

    private boolean shouldSendLogs = false;

    private int analyticsUploadFrequency = 0;

    private String analyticsDirectory;

    private NetworkListener listner;

    private Session fgSessionInstance;

    private ArrayList<JSONObject> imageConfigEventsList;

    private File imageLogsEventFile;

    /**
     * Constructor
     */
    private HAManager() {
        this.context = HikeMessengerApp.getInstance().getApplicationContext();

        analyticsDirectory = context.getFilesDir().toString() + AnalyticsConstants.EVENT_FILE_DIR;
        Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Storage dir :" + analyticsDirectory);

        isAnalyticsEnabled = getPrefs().getBoolean(AnalyticsConstants.ANALYTICS, AnalyticsConstants.IS_ANALYTICS_ENABLED);

        Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Analytics service status :" + isAnalyticsEnabled);

        long fileMaxSize = getPrefs().getLong(AnalyticsConstants.ANALYTICS_FILESIZE, AnalyticsConstants.MAX_FILE_SIZE);

        Logger.d(AnalyticsConstants.ANALYTICS_TAG, "File max size :" + fileMaxSize + " KBs");

        long analyticsMaxSize = getPrefs().getLong(AnalyticsConstants.ANALYTICS_TOTAL_SIZE, AnalyticsConstants.MAX_ANALYTICS_SIZE);

        Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Total analytics size :" + analyticsMaxSize + " KBs");

        maxInMemorySize = getPrefs().getInt(AnalyticsConstants.ANALYTICS_IN_MEMORY_SIZE, AnalyticsConstants.MAX_EVENTS_IN_MEMORY);

        Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Max events in memory before they get dumped to file :" + maxInMemorySize);

        shouldSendLogs = getPrefs().getBoolean(AnalyticsConstants.SEND_WHEN_CONNECTED, false);

        hourToSend = getPrefs().getLong(AnalyticsConstants.ANALYTICS_ALARM_TIME, -1);

        Calendar cal = Calendar.getInstance();

        if (hourToSend == -1) {
            int rndHour = getRandomTime();
            hourToSend = Utils.getTimeInMillis(cal, 0, 0, rndHour, 0);

            Editor editor = getPrefs().edit();
            editor.putLong(AnalyticsConstants.ANALYTICS_ALARM_TIME, hourToSend);
            editor.commit();
        }

        cal.setTimeInMillis(hourToSend);
        Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Next alarm date(Constructor) :" + cal.get(Calendar.DAY_OF_MONTH));
        Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Next alarm time(Constructor) :" + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE));

        analyticsSendFreq = getPrefs().getInt(AnalyticsConstants.ANALYTICS_SEND_FREQUENCY, AnalyticsConstants.DEFAULT_SEND_FREQUENCY);

        Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Send frequency :" + analyticsSendFreq + " mins");

        fgSessionInstance = new Session();

        // set network listener
        listner = new NetworkListener(this.context);

        imageConfigEventsList = new ArrayList<JSONObject>();
    }

    /**
     * Singleton instance of HAManager
     *
     * @return HAManager instance
     */
    public static HAManager getInstance() {
        if (instance == null) {
            synchronized (HAManager.class) {
                if (instance == null) {
                    instance = new HAManager();
                }
            }
        }
        return instance;
    }

    /**
     * records the analytics event to the file
     *
     * @param type         event type
     * @param eventContext context of the event
     */
    public void record(String type, String eventContext) {
        if (!isAnalyticsEnabled)
            return;
        recordEvent(type, eventContext, EventPriority.NORMAL, null, AnalyticsConstants.EVENT_TAG_MOB);
    }

    /**
     * records the analytics event to the file
     *
     * @param type         event type
     * @param eventContext context of the event
     * @param priority     priority of the event
     * @param metadata     metadata of the event
     */
    public void record(String type, String eventContext, JSONObject metadata) {
        if (!isAnalyticsEnabled)
            return;
        recordEvent(type, eventContext, EventPriority.NORMAL, metadata, AnalyticsConstants.EVENT_TAG_MOB);
    }

    /**
     * records the analytics event to the file
     *
     * @param type         type of the event
     * @param eventContext context of the event
     * @param priority     event priority
     * @param tag          tag for the event
     */
    public void record(String type, String eventContext, EventPriority priority, String tag) {
        if (!isAnalyticsEnabled)
            return;
        recordEvent(type, eventContext, priority, null, tag);
    }

    /**
     * records the analytics event to the file
     *
     * @param type         type of the event
     * @param eventContext context of the event
     * @param priority     priority of the event
     * @param metadata     metadata of the event
     * @param tag          tag of the event
     */
    public void record(String type, String eventContext, EventPriority priority, JSONObject metadata, String tag) {
        if (!isAnalyticsEnabled)
            return;
        recordEvent(type, eventContext, priority, metadata, tag);
    }

    /**
     * Used to write analytics event to the file
     *
     * @param type         type of the event
     * @param eventContext context of the event
     * @param metadata     metadata of the event as JSONObject
     * @param tag          tag value for the event
     */
    public void record(String type, String eventContext, JSONObject metadata, String tag) {
        if (!isAnalyticsEnabled)
            return;
        recordEvent(type, eventContext, EventPriority.NORMAL, metadata, tag);
    }

    /**
     * Used to write analytics event to the file
     *
     * @param type         type type of the event
     * @param eventContext eventContext context of the event
     * @param priority     normal or high
     * @param metadata     metadata of the event as JSONObject
     */
    public void record(String type, String eventContext, EventPriority priority, JSONObject metadata) {
        if (!isAnalyticsEnabled)
            return;
        recordEvent(type, eventContext, priority, metadata, AnalyticsConstants.EVENT_TAG_MOB);
    }

    /**
     * Used to write analytics event to the file
     *
     * @param eventJson event as JSONObject
     * @Sample: {"uk":"XXXXXX","k":"micro_app","c":"db_corrupt","fa":"db_error","f":"\"\\\/data\\\/data\\\/com.bsb.hike\\\/databases\\\/chats\"","ver":"v2"}
     * @NOTE: Below fields are mandatory
     * 1. AnalyticsConstants.V2.UNIQUE_KEY
     * 2. AnalyticsConstants.V2.KINGDOM
     * 3. AnalyticsConstants.V2.VERSION(This is added by the API itself)
     */
    //TODO: choose better name
    public void recordV2(JSONObject eventJson) {
        if (!isAnalyticsEnabled)
            return;
        recordEventV2(eventJson);
    }

    /**
     * Used to write the event onto the text file
     *
     * @param type         type of the event
     * @param eventContext context for the event
     * @param priority     priority of the event
     * @param metadata     event metadata
     * @param tag          tag for the event
     * @throws NullPointerException
     */
    // TODO need to look for a better way to do this operation and avoid synchronization
    private synchronized void recordEvent(String type, String eventContext, EventPriority priority, JSONObject metadata, String tag) throws NullPointerException {
        if (type == null || eventContext == null) {
            throw new NullPointerException("Type and Context of event cannot be null.");
        }
        Logger.d(AnalyticsConstants.ANALYTICS_TAG, metadata.toString());

        AnalyticsStore.getInstance(this.context).storeEvent(generateAnalticsJson(type, eventContext,
                priority, metadata, tag));
        ABTestSample(metadata);//Will be removed on 15/May/2016
    }

    //Will be removed on 15/May/2016
    //Variable as defined by PM for the experiment.
    String SAMPLE_EXPERIMENT_VARIABLE = "ABTEST-SAMPLE-01";

    /**
     * Sample method for ABTest sdk usage reference.
     *
     * @param metadata
     */
    private void ABTestSample(JSONObject metadata) {
        //For sample purpose, this will be run as and when user enters TimeLine...
        if (metadata.has(HikeConstants.EVENT_KEY)) {
            String eventKey = null;
            try {
                eventKey = metadata.getString(HikeConstants.EVENT_KEY);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (!eventKey.equals(HikeConstants.LogEvent.TIMELINE_OPEN)) {
                return;
            }
        } else {
            return;
        }
        //Call ABTest API based on variable type and default value
        int abtestIntSample = ABTest.getInt(SAMPLE_EXPERIMENT_VARIABLE, -1);

        //Implement behaviors based on the variable value
        switch (abtestIntSample) {
            case 1:
                //Implement for Behavior 1
                break;
            case 2:
                //Implement for Behavior 2
                break;
            case 3:
                //Implement for Behavior 3
                break;
        }

        //After user goes through the experiment, log the experiment values.
        //(Check with PM/Analyst on this, for your respective case)
        logABtestSample(abtestIntSample);

    }

    //Will be removed on 15/May/2016
    private void logABtestSample(int abtestIntSample) {
        //Log only if we have received experiment value (non-default)
        if (abtestIntSample > 0) {
            //Get Experiment details from ABTest SDK
            JSONObject analyticsLog = ABTest.getLogDetails(SAMPLE_EXPERIMENT_VARIABLE);
            if (analyticsLog != null) {
                try {
                    //Add experiment values, which you receive from your respective PM/Analyst
                    analyticsLog.put(AnalyticsConstants.V2.UNIQUE_KEY, "AB-TEST-SAMPLE");
                    analyticsLog.put(AnalyticsConstants.V2.GENUS, "sampleExperiment");
                    analyticsLog.put(AnalyticsConstants.V2.FAMILY, "Var Value: " +
                            SAMPLE_EXPERIMENT_VARIABLE + ": " + abtestIntSample);
                    recordEventV2(analyticsLog);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    /**
     * Used to write the event onto the text file
     *
     * @param eventJson event data
     * @throws NullPointerException, IllegalArgumentException
     */
    private synchronized void recordEventV2(JSONObject eventJson) throws NullPointerException, IllegalArgumentException {
        if (eventJson == null) {
            throw new NullPointerException("Event cannot be null.");
        }
        if (!eventJson.has(AnalyticsConstants.V2.UNIQUE_KEY) ||
                !eventJson.has(AnalyticsConstants.V2.KINGDOM)) {
            throw new IllegalArgumentException("AnalyticsConstants.V2.UNIQUE_KEY and AnalyticsConstants.V2.KINGDOM are Mandatory");
        }
        try {
            if (!eventJson.has(AnalyticsConstants.V2.VERSION) ||
                    !eventJson.getString(AnalyticsConstants.V2.VERSION).equals(AnalyticsConstants.V2.VERSION_VALUE)) {
                eventJson.put(AnalyticsConstants.V2.VERSION, AnalyticsConstants.V2.VERSION_VALUE);
            }
            eventJson.put(AnalyticsConstants.V2.CTS, Utils.applyOffsetToMakeTimeServerSync(context, System.currentTimeMillis()));
            eventJson.put(AnalyticsConstants.V2.RECORD_ID, fgSessionInstance.getSessionId());
        } catch (JSONException e) {
            Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Error in Event Json, ignoring event...");
            return;
        }

        eventJson = Utils.cloneJsonObject(eventJson);
        Logger.d(AnalyticsConstants.ANALYTICS_TAG, eventJson.toString());

        AnalyticsStore.getInstance(this.context).storeEvent(eventJson);
    }

    private synchronized void dumpInMemoryEventsAndTryToUpload(boolean sendNow, boolean isOnDemandFromServer) {
        AnalyticsStore.getInstance(this.context).sendEvents();
    }

    /**
     * Returns current max log file size
     *
     * @return log file size in bytes
     */
    public long getMaxFileSize() {
        long maxFileSize = getPrefs().getLong(AnalyticsConstants.ANALYTICS_FILESIZE, AnalyticsConstants.MAX_FILE_SIZE);
        maxFileSize *= 1024;
        return maxFileSize;
    }

    /**
     * Returns the hour of the day when log file should be sent to the server
     *
     * @return hour of the day(0-23)
     */
    public long getWhenToSend() {
        return hourToSend;
    }

    /**
     * Returns whether analytics logging service is currently enabled or disabled
     *
     * @return true if logging service is enabled, false otherwise
     */
    public boolean isAnalyticsEnabled() {
        return isAnalyticsEnabled;
    }

    /**
     * Used to check if analytics data can be sent based on network availability
     *
     * @return true if data can be sent, false otherwise
     */
    public boolean isSendAnalyticsDataWhenConnected() {
        return shouldSendLogs;
    }

    /**
     * Used to set whether logs should be sent next time alarm is triggered and user is connected
     *
     * @param value to set
     */
    public void setIsSendAnalyticsDataWhenConnected(boolean value) {
        Editor edit = getPrefs().edit();
        edit.putBoolean(AnalyticsConstants.SEND_WHEN_CONNECTED, value);
        edit.commit();
        this.shouldSendLogs = value;
    }

    /**
     * Used to enable/disable the analytics service
     *
     * @param isAnalyticsEnabled true if analytics service is enabled, false otherwise
     */
    public void setAnalyticsEnabled(boolean isAnalyticsEnabled) {
        Editor edit = getPrefs().edit();
        edit.putBoolean(AnalyticsConstants.ANALYTICS, isAnalyticsEnabled);
        edit.commit();
        this.isAnalyticsEnabled = isAnalyticsEnabled;
    }

    /**
     * Used to set the next alarm time for sending analytics data
     *
     * @param alarmTime in milliseconds
     */
    public void setNextSendTimeToPrefs(long alarmTime) {
        Editor editor = getPrefs().edit();
        editor.putLong(AnalyticsConstants.ANALYTICS_ALARM_TIME, alarmTime);
        editor.commit();
        hourToSend = alarmTime;
    }

    /**
     * Used to set the maximum event file size
     *
     * @param size size in Kb
     */
    public void setFileMaxSize(long size) {
        Editor edit = getPrefs().edit();
        edit.putLong(AnalyticsConstants.ANALYTICS_FILESIZE, size);
        edit.commit();
    }

    /**
     * Used to set the maximum analytics size on the client
     *
     * @param size in kilobytes
     */
    public void setAnalyticsMaxSizeOnClient(long size) {
        Editor edit = getPrefs().edit();
        edit.putLong(AnalyticsConstants.ANALYTICS_TOTAL_SIZE, size);
        edit.commit();
    }

    /**
     * Used to set the analytics send frequency
     *
     * @param frequency on which upload should be retried(0-23)
     */
    public void setAnalyticsSendFrequency(int freq) {
        Editor edit = getPrefs().edit();
        edit.putInt(AnalyticsConstants.ANALYTICS_SEND_FREQUENCY, freq);
        edit.commit();
        analyticsSendFreq = freq;
    }

    /**
     * Used to get the current frequency to send analytics data
     */
    public int getAnalyticsSendFrequency() {
        return analyticsSendFreq;
    }

    /**
     * Used to get the maximum analytics size on the client
     *
     * @return size of analytics in bytes
     */
    public long getMaxAnalyticsSizeOnClient() {
        long maxSize = getPrefs().getLong(AnalyticsConstants.ANALYTICS_TOTAL_SIZE, AnalyticsConstants.MAX_ANALYTICS_SIZE);
        maxSize *= 1024;
        return maxSize;
    }

    /**
     * Used to get the maximum value of in memory events before they are written to file
     *
     * @return count of in memory events
     */
    public int getMaxInMemoryEventsSize() {
        return maxInMemorySize;
    }

    /**
     * Used to set the maximum number of in memory events before we write them to file
     *
     * @param size number of in memory events
     */
    public void setMaxInMemoryEventsSize(int size) {
        Editor edit = getPrefs().edit();
        edit.putInt(AnalyticsConstants.ANALYTICS_IN_MEMORY_SIZE, size);
        edit.commit();
        maxInMemorySize = size;
    }

    /**
     * Used to get the application's SharedPreferences
     *
     * @return SharedPreference of the application
     */
    private SharedPreferences getPrefs() {
        return context.getSharedPreferences(HAManager.ANALYTICS_SETTINGS, Context.MODE_PRIVATE);
    }

    /**
     * Returns how many times in the day analytics data has been tried to upload
     *
     * @return frequency in int
     */
    protected int getAnalyticsUploadRetryCount() {
        return analyticsUploadFrequency;
    }

    /**
     * Resets the upload frequency to 0
     */
    protected void resetAnalyticsUploadRetryCount() {
        analyticsUploadFrequency = 0;
    }

    /**
     * Increments the analytics upload frequency
     */
    protected void incrementAnalyticsUploadRetryCount() {
        analyticsUploadFrequency++;
    }

    /**
     * generates the analytics json object to be written to the file
     *
     * @param type         type of the event
     * @param eventContext context of the event
     * @param priority     priority of the event
     * @param metadata     metadata of the event
     * @param tag          tag for the event
     * @return
     */
    private JSONObject generateAnalticsJson(String type, String eventContext, EventPriority priority, JSONObject metadata, String tagValue) {
        JSONObject json = new JSONObject();
        JSONObject data = new JSONObject();
        try {
            data.put(AnalyticsConstants.EVENT_TYPE, type);
            data.put(AnalyticsConstants.EVENT_SUB_TYPE, eventContext);
            data.put(AnalyticsConstants.EVENT_PRIORITY, priority);
            long ts = Utils.applyOffsetToMakeTimeServerSync(context, System.currentTimeMillis());
            data.put(AnalyticsConstants.CURRENT_TIME_STAMP, ts);
            data.put(AnalyticsConstants.EVENT_TAG, tagValue);

            if (metadata == null) {
                metadata = new JSONObject();
            } else {
                //Some metadata creators, modify metadata after calling the recordEvent()
                //Due to this, there was a ConcurrentModificationException while persisting the JSON
                //Cloning metadata will help us in this.
                metadata = Utils.cloneJsonObject(metadata);
            }

            metadata.put(AnalyticsConstants.SESSION_ID, fgSessionInstance.getSessionId());

            data.put(AnalyticsConstants.METADATA, metadata);

            json.put(AnalyticsConstants.TYPE, AnalyticsConstants.ANALYTICS_EVENT);
            json.put(AnalyticsConstants.DATA, data);

            Logger.d(AnalyticsConstants.ANALYTICS_TAG, "analytics json : " + json.toString());
        } catch (JSONException e) {
            Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
        }
        return json;
    }

    /**
     * Used to send the analytics data to the server
     */
    public void sendAnalyticsData(boolean sendNow, boolean isOnDemandFromServer) {
        dumpInMemoryEventsAndTryToUpload(sendNow, isOnDemandFromServer);
    }

    /**
     * Used to generate a random time in the range 0-23 at which analytics data will be sent to the server
     *
     * @return random time in range 0-23
     */
    private int getRandomTime() {
        Random rand = new Random();
        int time = rand.nextInt(AnalyticsConstants.DAY_IN_SECONDS);
        return time;
    }

    /**
     * Used to return the directory in which analytics data is saved
     *
     * @return analytics directory
     */
    public String getAnalyticsDirectory() {
        return analyticsDirectory;
    }

    /**
     * Used to get an array of file names present in the Analytics directory of the application package
     *
     * @return array of strings with file names
     */
    protected static String[] getFileNames(Context context) {
        File dir = new File(HAManager.getInstance().getAnalyticsDirectory() + File.separator);

        String[] fileNames = dir.list();

        return fileNames;
    }

    public JSONObject recordAndReturnSessionStart() {
        fgSessionInstance.startSession();

        JSONObject metadata = getMetaDataForSession(fgSessionInstance, true);

		/*
			We are not recording SessionEvent in Analytics File, so commenting it
			If In future if We uncomment it
			Check that While Recording Event we add SeesionID, so now it will be added twice
		*/
        //HAManager.getInstance().record(AnalyticsConstants.SESSION_EVENT, AnalyticsConstants.FOREGROUND, EventPriority.HIGH, metadata, AnalyticsConstants.EVENT_TAG_SESSION);

        return metadata;
    }

    public JSONObject recordAndReturnSessionEnd() {
        JSONObject metadata = getMetaDataForSession(fgSessionInstance, false);
		
		/*
			We are not recording SessionEvent in Analytics File, so commenting it
			If In future if We want to log that as well just uncomment it but
			Check that While Recording Event we also add SeesionID, so now it will be added twice
			so remove it before recording
		 */
        //HAManager.getInstance().record(AnalyticsConstants.SESSION_EVENT, AnalyticsConstants.BACKGROUND, EventPriority.HIGH, metadata, AnalyticsConstants.EVENT_TAG_SESSION);

        fgSessionInstance.reset();

        uploadAnalyticsIfReqd();
        return metadata;
    }

    private void uploadAnalyticsIfReqd() {
        if (getPrefs().getInt(AnalyticsConstants.EVENTS_TO_UPLOAD_COUNT, 0) >
                AnalyticsConstants.DEFAULT_THRESHOLD_EVENTS_TO_UPLOAD) {
            Logger.d(AnalyticsConstants.ANALYTICS_TAG, "----Uploading events on Session end----");
            sendAnalyticsData(true, false);
        }
    }

    private JSONObject getMetaDataForSession(Session session, boolean sessionStart) {
        JSONObject metadata = null;
        try {
            metadata = new JSONObject();

            //1)Adding Session Id
            metadata.put(AnalyticsConstants.SESSION_ID, fgSessionInstance.getSessionId());

            //2)con:- 2g/3g/4g/wifi/off
            metadata.put(AnalyticsConstants.CONNECTION_TYPE, Utils.getNetworkType(context));

            if (sessionStart) {
                if (fgSessionInstance.getAppOpenSource() == AppOpenSource.FROM_NOTIFICATION) {
                    // 5)con-type :- normal/stleath 0/1
                    //metadata.put(AnalyticsConstants.CONVERSATION_TYPE, session.getConvType());

                    // 6)msg_type :- MessageType (Text/Audio/Vedio/Sticker/Image/Contact/Location)
                    //metadata.put(AnalyticsConstants.MESSAGE_TYPE, session.getMsgType());
                }

                // Not sending it for now. We will fix this code in later release when required
                //metadata.put(AnalyticsConstants.SOURCE_APP_OPEN, session.getAppOpenSource());

                // 3)srcctx :- uid/gid/null(in case of appOpen via Launcher)
                metadata.put(AnalyticsConstants.SOURCE_CONTEXT, session.getSrcContext());

                Logger.d(AnalyticsConstants.ANALYTICS_TAG, "--session-id :" + session.getSessionId() + "--network-type :" + Utils.getNetworkTypeAsString(context) + "--source-context :" + session.getSrcContext() + "--conv-type :" + session.getConvType() + "--msg-type :" + session.getMsgType());
            } else {
                metadata.put(AnalyticsConstants.SESSION_TIME, fgSessionInstance.getSessionTime());

                metadata.put(AnalyticsConstants.DATA_CONSUMED, fgSessionInstance.getDataConsumedInSession());

                Logger.d(AnalyticsConstants.ANALYTICS_TAG, "--session-id :" + session.getSessionId() + "--session-time :" + session.getSessionTime() + "--network-type :" + Utils.getNetworkTypeAsString(context) + "--data-consumed :" + session.getDataConsumedInSession() + "bytes");
            }
        } catch (JSONException e) {
            Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
        }
        return metadata;
    }

    public void setMetadatFieldsForSessionEvent(String appOpenSource, String srcContext, ConvMessage convMessage, int convType) {
        fgSessionInstance.setAppOpenSource(appOpenSource);
        fgSessionInstance.setMsgType(getMsgType(convMessage));
        fgSessionInstance.setSrcContext(srcContext);
        fgSessionInstance.setConvType(convType);
    }


    public void chatHeadshareAnalytics(String eventKey, String... strings) {
        JSONObject metadata = new JSONObject();
        try {
            metadata.put(HikeConstants.EVENT_KEY, eventKey);
            metadata.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.ChatHeadEvents.STICKER_WDGT);
            if (strings.length >= 1) {
                metadata.put(HikeConstants.ChatHead.APP_NAME, strings[0]);
            }
            if (strings.length >= 2) {
                metadata.put(AnalyticsConstants.ChatHeadEvents.CAT_ID, strings[1]);
            }
            if (strings.length >= 3) {
                metadata.put(AnalyticsConstants.ChatHeadEvents.STICKER_ID, strings[2]);
            }
            if (strings.length >= 4) {
                metadata.put(AnalyticsConstants.ChatHeadEvents.SOURCE, strings[3]);
            }
            record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void stickyCallerAnalyticsNonUIEvent(String eventType, String numberType, String msisdn, String status, String source) {
        JSONObject metadata = new JSONObject();
        try {
            metadata.put(HikeConstants.EVENT_TYPE, eventType);
            metadata.put(HikeConstants.EVENT_KEY, AnalyticsConstants.StickyCallerEvents.STICKY_CALLER);
            metadata.put(AnalyticsConstants.StickyCallerEvents.NUMBER_TYPE, numberType);
            metadata.put(AnalyticsConstants.StickyCallerEvents.MSISDN, msisdn);
            metadata.put(AnalyticsConstants.StickyCallerEvents.STATUS, status);
            metadata.put(AnalyticsConstants.StickyCallerEvents.SOURCE, source);
            record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.StickyCallerEvents.CALL_EVENT, EventPriority.HIGH, metadata);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void stickyCallerAnalyticsUIEvent(String eventType, String msisdn, String source, String callType) {
        JSONObject metadata = new JSONObject();
        try {
            metadata.put(HikeConstants.EVENT_KEY, AnalyticsConstants.StickyCallerEvents.STICKY_CALLER);
            metadata.put(HikeConstants.EVENT_TYPE, eventType);
            metadata.put(AnalyticsConstants.StickyCallerEvents.MSISDN, msisdn);
            metadata.put(AnalyticsConstants.StickyCallerEvents.SOURCE, source);
            metadata.put(AnalyticsConstants.StickyCallerEvents.CALL_TYPE, callType);
            record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateTipAndNotifAnalyticEvent(String eventType, String eventKey, String eventContext) {
        JSONObject metadata = new JSONObject();
        try {
            metadata.put(HikeConstants.EVENT_TYPE, eventType);
            metadata.put(HikeConstants.EVENT_KEY, eventKey);
            HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, eventContext, EventPriority.HIGH, metadata);
        } catch (JSONException e) {
            Logger.d(HikeConstants.UPDATE_TIP_AND_PERS_NOTIF_LOG, "update tip/notif analytics json exception");
        }
    }

    public void interceptAnalyticsEvent(String eventKey, String action, boolean isUIEvent) {
        JSONObject metadata = new JSONObject();
        try {
            metadata.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.InterceptEvents.INTERCEPTS);
            metadata.put(HikeConstants.EVENT_KEY, eventKey);
            metadata.put(AnalyticsConstants.InterceptEvents.INTERCEPT_ACTION, action);
            if (isUIEvent) {
                record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
            } else {
                record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.InterceptEvents.INTERCPET_NOTIF_EVENT, EventPriority.HIGH, metadata);
            }
        } catch (JSONException e) {
            Logger.d(HikeConstants.INTERCEPTS.INTERCEPT_LOG, "intercept analytics event exception:" + e.toString());
        }
    }

    public void serviceEventAnalytics(String eventType, String serviceName) {
        JSONObject metadata = new JSONObject();
        try {
            metadata.put(HikeConstants.EVENT_KEY, HikeConstants.SERVICE);
            metadata.put(HikeConstants.EVENT_TYPE, eventType);
            metadata.put(HikeConstants.SERVICE, serviceName);
            metadata.put(HikeConstants.TIMESTAMP, System.currentTimeMillis());
            record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.SERVICE_STATS, EventPriority.HIGH, metadata);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    public void shareWhatsappAnalytics(String shrType, String catId, String stkrId, String path) {
        JSONObject metadata = new JSONObject();
        try {
            metadata.put(HikeConstants.Extras.SHARE_TYPE, shrType);
            metadata.put(HikeConstants.Extras.CATEGORYID, catId);
            metadata.put(HikeConstants.Extras.STICKERID, stkrId);
            metadata.put(HikeConstants.Extras.PATH, path);
            metadata.put(HikeConstants.EVENT_KEY, HikeConstants.Extras.WHATSAPP_SHARE);
            record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void shareWhatsappAnalytics(String shr) {
        JSONObject metadata = new JSONObject();

        try {
            metadata.put(HikeConstants.Extras.SHARE_TYPE, shr);
            metadata.put(HikeConstants.EVENT_KEY, HikeConstants.Extras.WHATSAPP_SHARE);
            record(AnalyticsConstants.UI_EVENT, HikeConstants.LogEvent.CLICK, EventPriority.HIGH, metadata);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setAppOpenSource(String appOpenSource) {
        fgSessionInstance.setAppOpenSource(appOpenSource);
    }

    /**
     * @param convMessage
     */
    private String getMsgType(ConvMessage convMessage) {
        if (convMessage == null) {
            return "";
        }

        if (convMessage.isStickerMessage()) {
            return AnalyticsConstants.MessageType.STICKER;
        }
        /**
         * If NO Metadata ===> It was a "Text" Msg in 1-1 Conv
         */
        else if (convMessage.getMetadata() != null) {
            if (convMessage.getMetadata().isPokeMessage()) {
                return AnalyticsConstants.MessageType.NUDGE;
            }

            List<HikeFile> list = convMessage.getMetadata().getHikeFiles();
            /**
             * If No HikeFile List ====> It was a "Text" Msg in gc
             */
            if (list != null) {
                HikeFileType fileType = convMessage.getMetadata().getHikeFiles().get(0).getHikeFileType();
                switch (fileType) {
                    case CONTACT:
                        return AnalyticsConstants.MessageType.CONTACT;

                    case LOCATION:
                        return AnalyticsConstants.MessageType.LOCATION;

                    case AUDIO:
                        return AnalyticsConstants.MessageType.AUDIO;

                    case VIDEO:
                        return AnalyticsConstants.MessageType.VEDIO;

                    case IMAGE:
                        return AnalyticsConstants.MessageType.IMAGE;

                }
            } else {
                return AnalyticsConstants.MessageType.TEXT;
            }
        }

        return AnalyticsConstants.MessageType.TEXT;

    }

    /**
     * It records Events For Bot for this individual session
     */
    public void recordIndividualChatSession(String msisdn ,String source)
    {
        JSONObject metadata;
        ChatSession chatSession = fgSessionInstance.getIndividualChatSesions(msisdn);
        fgSessionInstance.removeChatSessionFromMap(msisdn);
        try
        {
            if (chatSession != null)
            {
                metadata = new JSONObject();
                // 1)to_user:- "+hikecricket+" for cricket bot
                metadata.put(AnalyticsConstants.TO_USER, chatSession.getMsisdn());

                // 2)duration:-Total time of Chat Session in this particular session got this msisdn
                metadata.put(AnalyticsConstants.SESSION_TIME, chatSession.getChatSessionTime());

                // 3)putting event key (ek) as bot_open
                metadata.put(AnalyticsConstants.EVENT_KEY, HikePlatformConstants.BOT_OPEN);

                metadata.put(AnalyticsConstants.NETWORK_TYPE, Integer.toString(Utils.getNetworkType(HikeMessengerApp.getInstance().getApplicationContext())));
                metadata.put(AnalyticsConstants.APP_VERSION, AccountUtils.getAppVersion());
                metadata.put(AnalyticsConstants.SOURCE_APP_OPEN,source);

                record(AnalyticsConstants.CHAT_ANALYTICS, AnalyticsConstants.NON_UI_EVENT, EventPriority.HIGH, metadata, AnalyticsConstants.EVENT_TAG_BOTS);
                botOpenMqttAnalytics(metadata);

                Logger.d(AnalyticsConstants.ANALYTICS_TAG, "--session-id :" + fgSessionInstance.getSessionId() + "--to_user :" + chatSession.getMsisdn() + "--session-time :"
                        + chatSession.getChatSessionTime());
            }
        }
        catch (JSONException e)
        {
            Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
        }

    }

    private void botOpenMqttAnalytics(JSONObject metadata) {

        try {
            JSONObject mqttMetadata = new JSONObject(metadata.toString());
            mqttMetadata.put(AnalyticsConstants.EVENT_KEY, HikePlatformConstants.BOT_OPEN_MQTT);
            JSONObject data = new JSONObject();
            data.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.CHAT_ANALYTICS);
            data.put(HikeConstants.METADATA, mqttMetadata);

            Utils.sendLogEvent(data, AnalyticsConstants.NON_UI_EVENT, null);
        } catch (JSONException e) {
            Logger.w("LE", "Invalid json");
        }

    }

    /**
     * Sets StartingTime for Bot Chat Session to CurrentTime
     */
    public void startChatSession(String msisdn) {
        fgSessionInstance.startChatSession(msisdn);
    }

    /**
     * Sets StartingTime for Bot Chat Session to CurrentTime
     */
    public void endChatSession(String msisdn) {
        fgSessionInstance.endChatSesion(msisdn);
    }

    public void recordLastSeenEvent(String screen, String api, String msg, String toUser) {
        if (!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.DETAILED_HTTP_LOGGING_ENABLED, false)) {
            return;
        }
        JSONObject metadata = null;

        try {
            metadata = new JSONObject();

            metadata.put("scr", screen);

            metadata.put("api", api);

            if (!TextUtils.isEmpty(msg)) {
                metadata.put("m", msg);
            }

            if (!TextUtils.isEmpty(toUser)) {
                metadata.put("to", toUser);
            }

            HAManager.getInstance().record(AnalyticsConstants.LAST_SEEN_ANALYTICS, AnalyticsConstants.NON_UI_EVENT, EventPriority.HIGH, metadata, AnalyticsConstants.LAST_SEEN_ANALYTICS);

            Logger.d(AnalyticsConstants.LAST_SEEN_ANALYTICS_TAG, " --screen :" + screen + " --api :" + api + " -- msg :" + msg + " --to_user " + toUser);
        } catch (JSONException e) {
            Logger.d(AnalyticsConstants.LAST_SEEN_ANALYTICS_TAG, "invalid json");
        }
    }

    /**
     * records the analytics event to the file
     *
     * @param whichEvent   Event String which is to be recorded
     * @param type         event type
     * @param eventContext context of the event
     */
    public void record(String whichEvent, String eventType, String eventContext) {
        try {

            JSONObject metadata = new JSONObject();
            metadata.put(HikeConstants.EVENT_KEY, whichEvent);
            record(eventType, eventContext, metadata);
        } catch (JSONException e) {
            Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
        }
    }

    /**
     * records the analytics event to the file
     *
     * @param whichEvent
     * @param eventType
     * @param eventContext
     * @param eventPriority
     */
    public void record(String whichEvent, String eventType, String eventContext, EventPriority eventPriority) {
        try {
            JSONObject metadata = new JSONObject();
            metadata.put(HikeConstants.EVENT_KEY, whichEvent);
            record(eventType, eventContext, eventPriority, metadata);
        } catch (JSONException e) {
            Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
        }
    }

    /**
     * Used for logging sticker pallate crash/undesired behaviours
     *
     * @param errorMsg
     */
    public static void sendStickerCrashDevEvent(String errorMsg) {
        JSONObject error = new JSONObject();
        try {
            error.put(StickerManager.STICKER_ERROR_LOG, errorMsg);
            HAManager.getInstance().record(AnalyticsConstants.DEV_EVENT, AnalyticsConstants.STICKER_PALLETE, EventPriority.HIGH, error);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Used for logging sticker/emoticon weird behaviours
     *
     * @param errorMsg
     */
    public static void sendStickerEmoticonStrangeBehaviourReport(String errorMsg) {
        JSONObject error = new JSONObject();
        try {
            error.put(ShareablePopupLayout.TAG, errorMsg);
            HAManager.getInstance().record(AnalyticsConstants.DEV_EVENT, AnalyticsConstants.STICKER_PALLETE, EventPriority.HIGH, error);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * Used for logging DevEvent related to error/invalid state of the app.
     */
    public void logDevEvent(String productArea, String devArea, JSONObject info) {
        JSONObject metadata = new JSONObject();
        try {
            metadata.put(AnalyticsConstants.DEV_AREA, devArea);

            if (info != null) {
                metadata.put(AnalyticsConstants.DEV_INFO, info);
            }

            HAManager.getInstance().record(AnalyticsConstants.DEV_EVENT, productArea, EventPriority.HIGH, metadata);
        } catch (JSONException e) {
            Logger.e(AnalyticsConstants.ANALYTICS_TAG, "Invalid json:", e);
        }

    }

    /**
     * Used for logging UI click event
     *
     * @param eventKey
     */
    public static void logClickEvent(String eventKey) {
        try {
            JSONObject md = new JSONObject();
            md.put(HikeConstants.EVENT_KEY, eventKey);
            HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, md);
        } catch (JSONException e) {
            Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
        }
    }

    /**
     * Used for logging user's google accounts at the time of signup OR upgrade.
     */
    public void logUserGoogleAccounts() {
        Logger.d(AnalyticsConstants.ANALYTICS_TAG, "logUserGoogleAccounts");

        if (getPrefs().getBoolean(AnalyticsConstants.USER_GOOGLE_ACCOUNTS_SENT, false)) return;

        final AccountManager am = AccountManager.get(context);
        Account[] accounts = am.getAccountsByType(AnalyticsConstants.ACCOUNT_TYPE_GOOGLE);

        if (accounts == null || accounts.length == 0) {
            Logger.d(AnalyticsConstants.ANALYTICS_TAG, "No google accounts!!");
            return;
        }

        StringBuilder userAccounts = new StringBuilder();
        for (Account account : accounts) {
            userAccounts.append(account.name);
            userAccounts.append(",");
        }
        userAccounts.deleteCharAt(userAccounts.length() - 1);

        Logger.d(AnalyticsConstants.ANALYTICS_TAG, "User google accounts: " + userAccounts);
        try {
            JSONObject metadata = new JSONObject();
            metadata.put(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.EVENT_USER_GOOGLE_ACCOUNTS);
            metadata.put(AnalyticsConstants.USER_GOOGLE_ACCOUNTS, userAccounts.toString());
            recordEvent(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.EVENT_USER_GOOGLE_ACCOUNTS,
                    EventPriority.HIGH, metadata, AnalyticsConstants.EVENT_TAG_MOB);
        } catch (JSONException e) {
            Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
            return;
        }

        Editor editor = getPrefs().edit();
        editor.putBoolean(AnalyticsConstants.USER_GOOGLE_ACCOUNTS_SENT, true);
        editor.apply();
    }

    public void resetAnalyticsEventsUploadCount() {
        SharedPreferences.Editor sharedPrefEditor = getPrefs().edit();
        sharedPrefEditor.putInt(AnalyticsConstants.EVENTS_TO_UPLOAD_COUNT, 0);
        sharedPrefEditor.commit();
    }

    public void incrementAnalyticsEventsUploadCount() {
        SharedPreferences.Editor sharedPrefEditor = getPrefs().edit();
        sharedPrefEditor.putInt(AnalyticsConstants.EVENTS_TO_UPLOAD_COUNT,
                getPrefs().getInt(AnalyticsConstants.EVENTS_TO_UPLOAD_COUNT, 0) + 1);
        sharedPrefEditor.commit();
    }
}
