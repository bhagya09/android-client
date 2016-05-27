package com.hike.abtest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.hike.abtest.dataPersist.DataPersist;
import com.hike.abtest.dataparser.DataParser;
import com.hike.abtest.dataparser.ExperimentsLoader;
import com.hike.abtest.dataparser.ParserException;
import com.hike.abtest.model.Experiment;
import com.hike.abtest.model.Variable;

/**
 * Created by abhijithkrishnappa on 06/04/16.
 */
public class DataManager {
    private static final String TAG = DataManager.class.getSimpleName();
    Map<String, Experiment> mExperimentMap = null;
    Map<String, Variable> mVariableMap = null;
    DataPersist mDataPersist = null;

    DataManager(DataPersist dataPersist) {
        mDataPersist = dataPersist;
        mExperimentMap = new HashMap<String, Experiment>();
        mVariableMap = new HashMap<String, Variable>();
    }

    public void loadExperiments() {
        try {
            Map<String, ?> experiments = mDataPersist.getAllExperiments();

            if (experiments == null || experiments.size() == 0) {
                Logger.d(TAG, "No experiments to load...");
                return;
            }

            long parseStartTime = System.nanoTime();
            ExperimentsLoader experimentsLoader = new DataParser().getExperimentLoader(experiments);
            experimentsLoader.parse();
            mExperimentMap = experimentsLoader.getExperiments();
            mVariableMap = experimentsLoader.getVariables();
            long parseEndTime = System.nanoTime();
            Logger.d(TAG, "Parsing time: " + (parseEndTime - parseStartTime) + "ns");
        } catch (ParserException e) {
            Logger.e(TAG, "Cant apply AB experiments...");
            e.printStackTrace();
        }

        Logger.d(TAG, "---Loaded Experiment Values---");
        dumpExperimentMap(mExperimentMap);
        Logger.d(TAG, "---Loaded Variables Values---");
        dumpVariableMap(mVariableMap);
    }

    public <T> T getVariable(String varName, Class<T> type) {
        T result = null;

        if (mVariableMap.isEmpty() || mExperimentMap.isEmpty()) {
            return result;
        }

        Variable variable = mVariableMap.get(varName);
        if (variable != null) {
            Experiment experiment = mExperimentMap.get(variable.getExperimentId());
            if (experiment != null) {
                int experimentState = experiment.getExperimentState();
                Logger.d(TAG, "experimentState: " + experimentState);
                if (experimentState == Experiment.EXPERIMENT_STATE_ROLLED_OUT ||
                        experimentState == Experiment.EXPERIMENT_STATE_RUNNING) {
                    result = type.cast(variable.getExperimentValue());
                } else if (experimentState == Experiment.EXPERIMENT_STATE_STOPPED) {
                    result = type.cast(variable.getDefaultValue());
                }
            }
        }
        return result;
    }

    public Experiment getExperiment(String varKey) {
        Experiment result = null;
        if (mExperimentMap == null || mVariableMap == null) {
            Logger.d(TAG, "No experiments..");
            return result;
        }
        Variable variable = mVariableMap.get(varKey);

        if (variable != null) {
            result = mExperimentMap.get(variable.getExperimentId());
        }
        return result;
    }

    private void dumpExperimentMap(Map<String, Experiment> experimentMap) {
        Collection<Experiment> experimentList = experimentMap.values();
        for (Experiment experiment : experimentList) {
            Logger.d(TAG, experiment.toString());
        }
    }

    private void dumpVariableMap(Map<String, Variable> variableMap) {
        Collection<Variable> variableList = variableMap.values();
        for (Variable variable : variableList) {
            Logger.d(TAG, variable.toString());
        }
    }
}
