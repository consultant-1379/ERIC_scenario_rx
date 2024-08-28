package com.ericsson.de.scenariorx.impl;

import static com.ericsson.de.scenariorx.api.RxDataDrivenApi.TEST_SUITE_ID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.cifwk.taf.AllureFacade;
import com.ericsson.de.scenariorx.api.RxScenarioListener;
import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;

public class RxDataDrivenListener extends RxScenarioListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RxDataDrivenListener.class);

    @Subscribe
    public void onScenarioFinished(ScenarioEventBus.InternalScenarioFinishedEvent event) {
        if (getCurrentSuiteName().isPresent()) {
            AllureBuilder.build(event.results, event.getScenario());
        } else if (event.getScenario().getParameters().containsKey(TEST_SUITE_ID)) {
            String suiteName = event.getScenario().getParameters().get(TEST_SUITE_ID).toString();
            AllureFacade.startSuite(suiteName);
            AllureBuilder.build(event.results, event.getScenario());
            AllureFacade.finishSuite(suiteName);
        } else {
            LOGGER.error("Skipping Allure report generation:\n" +
                    "Scenario should have parameter `{}` or `com.ericsson.cifwk.taf.annotations.TestSuite` annotation", TEST_SUITE_ID);
        }
    }

    private Optional<String> getCurrentSuiteName() {
        try {
            return Optional.of(AllureFacade.getCurrentSuiteName());
        } catch (Exception e) {
            return Optional.absent();
        }
    }
}
