package com.hike.abtest;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.hike.abtest.dataPersist.DataPersist;
import com.hike.abtest.dataparser.DataParser;
import com.hike.abtest.dataparser.ExperimentAbort;
import com.hike.abtest.dataparser.ExperimentInit;
import com.hike.abtest.dataparser.ParserException;

import java.util.Collection;

public class UpdateExperimentService extends IntentService {
    private static final String ACTION_NOTIFICATION_RCVD = "com.hike.abtest.action.NOTIFICATION_RCVD";
    private static final String EXTRA_PAYLOAD = "com.hike.abtest.extra.PAYLOAD";
    private static final String EXTRA_REQUEST_TYPE = "com.hike.abtest.extra.requesttype";
    private static final String TAG = UpdateExperimentService.class.getSimpleName();
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILURE = "failure";
    private DataParser mDataParser = null;
    private static DataPersist mDataPersist = null;

    public UpdateExperimentService() {
        super(TAG);
    }

    public static void onRequestReceived(Context context, String requestType, String payLoad, DataPersist dataPersist) {
        if (dataPersist == null) {
            return;
        }
        mDataPersist = dataPersist;

        Intent intent = new Intent(context, UpdateExperimentService.class);
        intent.setAction(ACTION_NOTIFICATION_RCVD);
        intent.putExtra(EXTRA_REQUEST_TYPE, requestType);
        intent.putExtra(EXTRA_PAYLOAD, payLoad);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            final String payload = intent.getStringExtra(EXTRA_PAYLOAD);
            final String requestType = intent.getStringExtra(EXTRA_REQUEST_TYPE);
            try {
                mDataParser = new DataParser();
                handleRequest(requestType, payload);
            } catch (ParserException | IllegalArgumentException e) {
                Logger.e(TAG, "Error processing request:");
                e.printStackTrace();
            } finally {
                mDataParser = null;
            }
        } else {
            Logger.e(TAG, "Invalid request, Ignoring...");
        }
    }

    private void handleRequest(String requestType, String payload) throws ParserException {
        switch (requestType) {
            case DataParser.REQUEST_TYPE_EXPERIMENT_INIT:
                handleExperimentInit(payload);
                break;
            case DataParser.REQUEST_TYPE_EXPERIMENT_ABORT:
                handleExperimentAbort(payload);
                break;
            case DataParser.REQUEST_TYPE_EXPERIMENT_ROLL_OUT:
                handleRollOut(payload);
                break;
            default:
                throw new IllegalArgumentException("Invalid Request...");
        }
    }

    private void handleExperimentInit(String payload) throws ParserException {
        //Persist the experiments received. It will be applied in the next run
        Logger.d(TAG, "Processing experiment init request");
        ExperimentInit experimentInitReq = mDataParser.getExperimentInit(payload);
        experimentInitReq.parse();
        mDataPersist.persistNewExperiment(experimentInitReq.getExperimentsMap());
        respondStatus(DataParser.REQUEST_TYPE_EXPERIMENT_INIT,
                STATUS_SUCCESS, experimentInitReq.getExperimentsMap().keySet());
    }

    private void handleExperimentAbort(String payload) throws ParserException {
        //Remove Aborted experiment, is any issues with parsing
        Logger.d(TAG, "Processing experiment abort request");
        ExperimentAbort experimentAbortReq = mDataParser.getExperimentAbort(payload);
        experimentAbortReq.parse();
        mDataPersist.abortExperiment(experimentAbortReq.getExperimentIds());
        respondStatus(DataParser.REQUEST_TYPE_EXPERIMENT_ABORT,
                STATUS_SUCCESS, experimentAbortReq.getExperimentIds());
    }

    private void handleRollOut(String payload) throws ParserException {
        //Persist the Rollout. It will be applied in the next run
        Logger.d(TAG, "Processing experiment rollout request");
        ExperimentInit rollOutReq = mDataParser.getRollout(payload);
        rollOutReq.parse();
        mDataPersist.persistRollOuts(rollOutReq.getExperimentsMap());
        respondStatus(DataParser.REQUEST_TYPE_EXPERIMENT_ROLL_OUT,
                STATUS_SUCCESS, rollOutReq.getExperimentsMap().keySet());
    }

    private void respondStatus(String requestType, String status, Collection<String> experiments) {
        Logger.d(TAG, "Experiment Request Status: " + status);

        for (String expId : experiments) {
            AnalyticsUtil.sendRequestStatusAnalyticsJson(requestType, status, expId);
        }
    }
}
