package com.hike.cognito.datapoints;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by abhijithkrishnappa on 23/05/16.
 */
public class DataPointTaskCallLogs extends DataPointTask {
    private static final String TAG = DataPointTaskCallLogs.class.getSimpleName();

    private static final String MISSED_CALL_COUNT = "m";
    private static final String RECEIVED_CALL_COUNT = "r";
    private static final String SENT_CALL_COUNT = "s";
    private static final String RECEIVED_CALL_DURATION = "rd";
    private static final String SENT_CALL_DURATION = "sd";
    private static final String PHONE_NUMBER = "ph";

    private static final String SENT_SMS = "ss";
    private static final String RECEIVED_SMS = "rs";

    private static final long milliSecInDay = 1000 * 60 * 60 * 24;
    private static final int DAYS_TO_LOG = 30;
    private static final int MAX_CURSOR_LIMIT = 500;

    private static final int SMS_TYPE_INBOX = 1;
    private static final int SMS_TYPE_SENT = 2;

    public DataPointTaskCallLogs(String url, boolean isPii, int transportType) {
        super(url, isPii, transportType);
    }

    public static class CallLogPojo {
        final String phoneNumber;
        int missedCallCount;
        int receivedCallCount;
        int sentCallCount;
        int sentCallDuration;
        int receivedCallDuration;
        int sentSmsCount;
        int receivedSmsCount;

        public CallLogPojo(String phoneNumber, int missedCallCount, int receivedCallCount, int sentCallCount,
                           int sentCallDuration, int receivedCallDuration, int sentSmsCount, int receivedSmsCount) {
            this.phoneNumber = phoneNumber;
            this.missedCallCount = missedCallCount;
            this.receivedCallCount = receivedCallCount;
            this.sentCallCount = sentCallCount;
            this.sentCallDuration = sentCallDuration;
            this.receivedCallDuration = receivedCallDuration;
            this.sentSmsCount = sentSmsCount;
            this.receivedSmsCount = receivedSmsCount;
        }
    }

    @Override
    JSONArray recordData() {
        //Map is being used to store and retrieve values multiple times
        Map<String, CallLogPojo> callLogMap = new HashMap<String, CallLogPojo>();

        getSmsDetails(callLogMap);

        getCallLogsDetails(callLogMap);


        JSONArray callJsonArray = toJsonArray(callLogMap);

        Logger.d(TAG, callJsonArray.toString());
        return callJsonArray;
    }

    private void getSmsDetails(Map<String, CallLogPojo> callLogMap) {
        Context ctx = HikeMessengerApp.getInstance().getApplicationContext();

        Uri smsUri = Uri.parse("content://sms");
        Cursor smsCur = ctx.getContentResolver().query(smsUri, null, null, null, null);

        if (smsCur != null && smsCur.moveToFirst()) {
            try {
                do {
                    String smsNumber = smsCur.getString(smsCur.getColumnIndexOrThrow("address"));
                    String smsDate = smsCur.getString(smsCur.getColumnIndexOrThrow("date"));
                    int smsType = smsCur.getInt(smsCur.getColumnIndexOrThrow("type"));

                    if (Long.parseLong(smsDate) > (System.currentTimeMillis() - (milliSecInDay * DAYS_TO_LOG))
                            && smsNumber != null && (smsType == 1 || smsType == 2)) {

                        CallLogPojo callLog = null;

                        if (callLogMap.containsKey(smsNumber)) {
                            callLog = callLogMap.get(smsNumber);
                        } else {
                            callLog = new CallLogPojo(smsNumber, 0, 0, 0, 0, 0, 0, 0);
                        }

                        switch (smsType) {
                            case SMS_TYPE_INBOX:
                                callLog.receivedSmsCount++;
                                break;
                            case SMS_TYPE_SENT:
                                callLog.sentSmsCount++;
                                break;
                        }
                        callLogMap.put(smsNumber, callLog);
                    }
                } while (smsCur.moveToNext());
            } catch (Exception e) {
                Logger.d(TAG, e.toString());
            } finally {
                smsCur.close();
            }
        }
    }

    private void getCallLogsDetails(Map<String, CallLogPojo> callLogMap) {
        Context ctx = HikeMessengerApp.getInstance().getApplicationContext();
        String strOrder = android.provider.CallLog.Calls.DATE + " DESC";
        String[] projection = new String[]{CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE, CallLog.Calls.DURATION};
        String selection = CallLog.Calls.DATE + " > ?";
        String[] selectors = new String[]{String.valueOf(System.currentTimeMillis() - (milliSecInDay * DAYS_TO_LOG))};
        Uri callUri = CallLog.Calls.CONTENT_URI;
        Uri callUriLimited = callUri.buildUpon()
                .appendQueryParameter(CallLog.Calls.LIMIT_PARAM_KEY, String.valueOf(MAX_CURSOR_LIMIT))
                .build();

        ContentResolver cr = ctx.getContentResolver();
        Cursor cur = cr.query(callUriLimited, projection, null, null, strOrder);

        if (cur != null) {
            try {
                while (cur.moveToNext()) {

                    String callNumber = cur.getString(cur.getColumnIndex(android.provider.CallLog.Calls.NUMBER));
                    String callDate = cur.getString(cur.getColumnIndex(android.provider.CallLog.Calls.DATE));
                    int duration = cur.getInt(cur.getColumnIndex(android.provider.CallLog.Calls.DURATION));

                    if (Long.parseLong(callDate) > (System.currentTimeMillis() - (milliSecInDay * DAYS_TO_LOG))) {

                        CallLogPojo callLog = null;

                        if (callLogMap.containsKey(callNumber)) {
                            callLog = callLogMap.get(callNumber);
                        } else {
                            callLog = new CallLogPojo(callNumber, 0, 0, 0, 0, 0, 0, 0);
                        }

                        switch (cur.getInt(cur.getColumnIndex(android.provider.CallLog.Calls.TYPE))) {
                            case CallLog.Calls.MISSED_TYPE:
                                callLog.missedCallCount++;
                                break;
                            case CallLog.Calls.OUTGOING_TYPE:
                                callLog.sentCallCount++;
                                callLog.sentCallDuration += duration;
                                break;
                            case CallLog.Calls.INCOMING_TYPE:
                                callLog.receivedCallCount++;
                                callLog.receivedCallDuration += duration;
                                break;

                        }
                        callLogMap.put(callNumber, callLog);
                    }

                }
            } catch (Exception e) {
                Logger.d(TAG, e.toString());
            } finally {
                cur.close();
            }
        }
    }

    private JSONArray toJsonArray(Map<String, CallLogPojo> callLogMap) {
        List<CallLogPojo> callLogList = new ArrayList<CallLogPojo>(callLogMap.size());
        for (Map.Entry<String, CallLogPojo> entry : callLogMap.entrySet()) {
            callLogList.add(entry.getValue());
        }
        JSONArray callJsonArray = new JSONArray();
        try {
            for (CallLogPojo callLog : callLogList) {
                JSONObject jsonObj = new JSONObject();
                if(TextUtils.isEmpty(callLog.phoneNumber))
                {
                    continue;
                }
                jsonObj.putOpt(PHONE_NUMBER, callLog.phoneNumber);
                jsonObj.putOpt(MISSED_CALL_COUNT, callLog.missedCallCount);
                jsonObj.putOpt(SENT_CALL_COUNT, callLog.sentCallCount);
                jsonObj.putOpt(RECEIVED_CALL_COUNT, callLog.receivedCallCount);
                jsonObj.putOpt(SENT_CALL_DURATION, callLog.sentCallDuration);
                jsonObj.putOpt(RECEIVED_CALL_DURATION, callLog.receivedCallDuration);
                jsonObj.putOpt(SENT_SMS, callLog.sentSmsCount);
                jsonObj.putOpt(RECEIVED_SMS, callLog.receivedSmsCount);
                callJsonArray.put(jsonObj);
            }
        } catch (JSONException jse) {
            jse.printStackTrace();
        }

        return callJsonArray;
    }
}
