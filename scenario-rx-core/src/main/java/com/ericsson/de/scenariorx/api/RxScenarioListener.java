package com.ericsson.de.scenariorx.api;

import com.ericsson.de.scenariorx.api.events.RxFlowEvent;
import com.ericsson.de.scenariorx.api.events.RxScenarioEvent;
import com.ericsson.de.scenariorx.api.events.RxTestStepEvent.RxTestStepFinishedEvent;
import com.ericsson.de.scenariorx.api.events.RxTestStepEvent.RxTestStepStartedEvent;
import com.ericsson.de.scenariorx.impl.ScenarioListener;
import com.google.common.eventbus.Subscribe;

/**
 * Base class for extending custom Scenario listeners from
 *
 * @see RxScenarioRunnerBuilder#addListener(RxScenarioListener)
 */
@SuppressWarnings("unused")
public abstract class RxScenarioListener implements ScenarioListener {

    @Subscribe
    public void onScenarioStarted(RxScenarioEvent.RxScenarioStartedEvent event) {
        // intentionally do nothing
    }

    @Subscribe
    public void onScenarioFinished(RxScenarioEvent.RxScenarioFinishedEvent event) {
        // intentionally do nothing
    }

    @Subscribe
    public void onFlowStarted(RxFlowEvent.RxFlowStartedEvent event) {
        // intentionally do nothing
    }

    @Subscribe
    public void onFlowFinished(RxFlowEvent.RxFlowFinishedEvent event) {
        // intentionally do nothing
    }

    @Subscribe
    public void onTestStepStarted(RxTestStepStartedEvent event) {
        // intentionally do nothing
    }

    @Subscribe
    public void onTestStepFinished(RxTestStepFinishedEvent event) {
        // intentionally do nothing
    }
}
