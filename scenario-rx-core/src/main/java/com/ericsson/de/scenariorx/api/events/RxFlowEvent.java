package com.ericsson.de.scenariorx.api.events;

import com.ericsson.de.scenariorx.api.RxFlow;
import com.ericsson.de.scenariorx.impl.Flow;

public class RxFlowEvent extends RxEvent {

    private final RxFlow flow;

    private RxFlowEvent(Flow flow) {
        this.flow = (RxFlow) flow;
    }

    public RxFlow getFlow() {
        return flow;
    }

    public static class RxFlowStartedEvent extends RxFlowEvent {

        public RxFlowStartedEvent(Flow flow) {
            super(flow);
        }
    }

    public static class RxFlowFinishedEvent extends RxFlowEvent {

        public RxFlowFinishedEvent(Flow flow) {
            super(flow);
        }
    }
}
