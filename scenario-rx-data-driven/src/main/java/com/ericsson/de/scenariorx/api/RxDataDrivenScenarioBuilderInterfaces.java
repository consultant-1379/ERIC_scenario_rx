package com.ericsson.de.scenariorx.api;

import com.ericsson.de.scenariorx.impl.Flow;
import com.ericsson.de.scenariorx.impl.Scenario;

@SuppressWarnings("WeakerAccess")
public final class RxDataDrivenScenarioBuilderInterfaces {

    private RxDataDrivenScenarioBuilderInterfaces() {
    }

    public interface ScenarioBuilderStates<S extends Scenario, F extends Flow>
            extends RxScenarioBuilderInterfaces.ScenarioStart<S, F>, Flows<S, F>, AlwaysRun<S, F>, ExceptionHandler<S>,
            ScenarioStart<S, F>,
            ToFlows<S, F> {

    }

    /*---------------- STATE INTERFACES ----------------*/

    public interface ScenarioStart<S extends Scenario, F extends Flow> extends RxScenarioBuilderInterfaces.ScenarioStart<S, F>, ToFlows<S, F> {

    }


    public interface Flows<S extends Scenario, F extends Flow> extends RxScenarioBuilderInterfaces.Flows<S, F>, ToExceptionHandler<S>, ToScenarioDataSources<S>, ToFlows<S, F> {

    }

    public interface AlwaysRun<S extends Scenario, F extends Flow> extends RxScenarioBuilderInterfaces.AlwaysRun<S, F>, ToExceptionHandler<S>, ToScenarioDataSources<S>, ToFlows<S, F> {

    }

    public interface ExceptionHandler<S extends Scenario> extends RxScenarioBuilderInterfaces.ExceptionHandler<S>, ToScenarioDataSources<S>, ToExceptionHandler<S> {

    }

    /*---------------- DESTINATION INTERFACES ----------------*/

    private interface ToFlows<S extends Scenario, F extends Flow> {

        Flows<S, F> addFlow(Builder<F> flowBuilder);

        Flows<S, F> addFlow(F flow);

        Flows<S, F> split(Builder<F>... subFlows);

        Flows<S, F> split(F... subFlows);

    }

    private interface ToScenarioDataSources<S extends Scenario> extends ToExceptionHandler<S> {

        ExceptionHandler<S> withScenarioDataSources(RxDataSource... dataSource);

        ExceptionHandler<S> runParallel(int vUsers);

        ExceptionHandler<S> runParallelAuto();

    }

    private interface ToExceptionHandler<S extends Scenario> {

        ExceptionHandler<S> withExceptionHandler(RxExceptionHandler exceptionHandler);

    }
}
