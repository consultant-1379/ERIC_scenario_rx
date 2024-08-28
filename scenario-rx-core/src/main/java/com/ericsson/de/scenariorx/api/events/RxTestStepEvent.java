package com.ericsson.de.scenariorx.api.events;

import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;
import com.ericsson.de.scenariorx.api.RxTestStep;

public class RxTestStepEvent extends RxEvent {

    private final String name;

    private RxTestStepEvent(RxTestStep testStep) {
        this.name = testStep.getName();
    }

    public String getName() {
        return name;
    }

    public static class RxTestStepStartedEvent extends RxTestStepEvent {

        private RxDataRecordWrapper dataRecord;

        public RxTestStepStartedEvent(RxTestStep testStep, RxDataRecordWrapper dataRecord) {
            super(testStep);
            this.dataRecord = dataRecord;
        }

        public RxDataRecordWrapper getDataRecord() {
            return dataRecord;
        }
    }

    public static class RxTestStepFinishedEvent extends RxTestStepEvent {

        private RxTestStep.Status status;
        private Throwable error;

        public RxTestStepFinishedEvent(RxTestStep testStep,
                                       RxTestStep.Status status,
                                       Throwable error) {
            super(testStep);
            this.status = status;
            this.error = error;
        }

        public RxTestStep.Status getStatus() {
            return status;
        }

        public Throwable getError() {
            return error;
        }
    }
}
