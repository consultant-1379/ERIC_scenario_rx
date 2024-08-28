package com.ericsson.de.scenariorx.api;

import com.ericsson.de.scenariorx.impl.Flow;
import com.ericsson.de.scenariorx.impl.Scenario;

/**
 * Determines the order in which Scenario builder methods can be called.
 */
@SuppressWarnings("WeakerAccess")
public final class RxScenarioBuilderInterfaces {

    private RxScenarioBuilderInterfaces() {
    }

    public interface ScenarioBuilderStates<S extends Scenario, F extends Flow>
            extends ScenarioStart<S, F>, Flows<S, F>, AlwaysRun<S, F>, ExceptionHandler<S> {

    }

    /*---------------- STATE INTERFACES ----------------*/

    public interface ScenarioStart<S extends Scenario, F extends Flow> extends ToFlows<S, F> {

        ScenarioStart<S, F> withParameter(String key, Object value);

    }

    public interface Flows<S extends Scenario, F extends Flow> extends ToFlows<S, F>, ToExceptionHandler<S>, Builder<S> {

        AlwaysRun<S, F> alwaysRun();

    }

    public interface AlwaysRun<S extends Scenario, F extends Flow> extends ToFlows<S, F>, ToExceptionHandler<S>, Builder<S> {

    }

    public interface ExceptionHandler<S extends Scenario> extends Builder<S> {

    }

    /*---------------- DESTINATION INTERFACES ----------------*/

    private interface ToFlows<S extends Scenario, F extends Flow> {

        Flows<S, F> addFlow(Builder<F> flowBuilder);

        Flows<S, F> addFlow(F flow);

        Flows<S, F> split(Builder<F>... subFlows);

        Flows<S, F> split(F... subFlows);

    }

    private interface ToExceptionHandler<S extends Scenario> {

        ExceptionHandler<S> withExceptionHandler(RxExceptionHandler exceptionHandler);

    }
}
