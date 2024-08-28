package com.ericsson.de.scenariorx.impl;

import static java.lang.Thread.currentThread;

import java.util.List;

import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;
import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.api.events.RxScenarioEvent;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the debug logging listeners for Scenario, Flow & Test Step
 */
@SuppressWarnings("unused")
public class DebugLogScenarioListener implements ScenarioListener {

    public static final ScenarioListener INSTANCE = new DebugLogScenarioListener();

    private static final Logger logger = LoggerFactory.getLogger(DebugLogScenarioListener.class);

    private static final int DATA_RECORDS_LIMIT = 10;

    private DebugLogScenarioListener() {
    }

    @Subscribe
    public void debugScenario(RxScenarioEvent.RxScenarioStartedEvent event) {
        RxScenario scenario = event.getScenario();
        logger.info("Running Scenario '{}'", scenario.getName());
    }

    @Subscribe
    public void debugFlow(ScenarioEventBus.InternalFlowStartedEvent event) {
        Flow flow = event.getFlow();
        DataSourceStrategy dataSource = flow.dataSource;
        if (DataSourceStrategy.Empty.class.isAssignableFrom(dataSource.getClass())) {
            logger.info("Running Flow '{}' without Data Source", flow.getName());
        } else {
            List<RxDataRecordWrapper> dataRecords = event.getDataSource().toList().toBlocking().single();
            logger.info("Running Flow '{}' with {} consisting of {} Data Records: {}",
                    flow.getName(), dataSource.definition(), dataRecords.size(), samples(dataRecords));
        }
    }

    private String samples(List<RxDataRecordWrapper> dataRecords) {
        if (dataRecords.size() > DATA_RECORDS_LIMIT) {
            String temp = dataRecords.subList(0, DATA_RECORDS_LIMIT).toString();
            return temp.substring(0, temp.length() - 1) + "...]";
        } else {
            return dataRecords.toString();
        }
    }

    @Subscribe
    public void debugTestStep(ScenarioEventBus.InternalTestStepStartedEvent event) {
        Internals.Exec execution = event.getExecution();
        String oldThreadName = currentThread().getName();
        currentThread().setName(execution.flowPath + ".vUser-" + execution.vUser);
        logger.info("Running Test Step '{}' with Data Record {} and context {}",
                event.getName(), execution.dataRecord, execution.context.values);
        currentThread().setName(oldThreadName);
    }
}
