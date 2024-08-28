package com.ericsson.de.scenariorx.impl;

import static java.lang.Math.min;
import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ericsson.de.scenariorx.api.Builder;
import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;
import com.ericsson.de.scenariorx.api.RxDataSource;
import com.ericsson.de.scenariorx.api.RxExceptionHandler;
import com.ericsson.de.scenariorx.api.RxFlowBuilderInterfaces;
import com.ericsson.de.scenariorx.api.RxScenarioBuilder;
import com.ericsson.de.scenariorx.api.RxScenarioRunnerBuilder;
import com.ericsson.de.scenariorx.api.RxTestStep;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

/**
 * Builder for {@link Flow}
 */
@SuppressWarnings("WeakerAccess")
public abstract class FlowBuilder<T extends Flow> implements RxFlowBuilderInterfaces.FlowBuilderStates<T> {

    protected static final String DEFAULT_NAME = "fork";

    private static final int V_USERS_PER_PROCESSOR = 8;

    static final String ERROR_V_USERS_NOT_ONCE = "Methods withVUsers()/withVUsersAuto() can be called just once per flow";
    static final String ERROR_V_USERS_NEGATIVE = "vUser value should be greater than zero";
    static final String ERROR_V_USERS_AUTO_NO_DATA_SOURCES = "Method withVUsersAuto() can't be used without any data sources";
    static final String ERROR_V_USERS_AUTO_NON_SHARED = "Method withVUsersAuto() must be used with shared Data Sources only. ";
    static final String HINT_NON_SHARED = "The following Data Sources are not shared: ";

    static final String ERROR_DATA_SOURCES_NOT_ONCE = "Method withDataSources() can be called just once per flow";
    static final String ERROR_DATA_SOURCES_NULL = "Data Sources can't be null";
    static final String ERROR_DATA_SOURCES_EMPTY = "Data Sources can't be empty";
    static final String ERROR_DATA_SOURCE_NULL = "Data Source can't be null";
    static final String ERROR_DATA_SOURCE_TOO_CYCLIC = "At least one of data sources defined on one flow should be not cyclic to avoid forever loop";

    static final String ERROR_TEST_STEP_NULL = "Test step can't be null";
    static final String ERROR_WITH_BEFORE_NOT_ONCE = "Method withBefore() can be called just once per flow. ";
    static final String ERROR_WITH_AFTER_NOT_ONCE = "Method withAfter() can be called just once per flow. ";
    static final String HINT_SINGLE_CALL = "Please pass (several test steps if required) into single method call.";

    static final String ERROR_SUBFLOWS_NULL = "Sub Flows can't be null";
    static final String ERROR_SUBFLOW_NULL = "Sub Flow can't be null";

    public static final String ERROR_EXCEPTION_HANDLER_NULL = "RxExceptionHandler can't be null";
    public static final String ERROR_EXCEPTION_HANDLER_NOT_ONCE = "RxExceptionHandler can't be set twice. ";
    public static final String HINT_EXCEPTION_HANDLER = "In case you need multiple exception handlers use RxApi.compositeExceptionHandler()";
    public static final String ERROR_PREDICATE_NULL = "Predicate defined can not be null";
    protected final String name;

    private Integer vUsers = null;
    private boolean vUsersAuto = false;
    private RxDataSource[] dataSources = null;
    private final List<Invocation> testSteps = newArrayList();
    private Predicate<RxDataRecordWrapper> predicate = null;

    private List<RxTestStep> beforeInvocation = new ArrayList<>();
    private List<RxTestStep> afterInvocation = new ArrayList<>();

    protected ExceptionHandler exceptionHandler;

    protected FlowBuilder(String name) {
        this.name = name;
    }

    /**
     * Continuously executes a flow. Terminates once predicate is no longer satisfied.<br/>
     * <b>Note:</b> If Predicate returns false on first run of flow, flow execution will be skipped.
     *
     * @param predicate User defined predicate
     * @see Api#during(Integer, TimeUnit)
     * @return builder
     */
    @Override
    public RxFlowBuilderInterfaces.Options<T> runWhile(Predicate<RxDataRecordWrapper> predicate) {
        checkNotNull(predicate, ERROR_PREDICATE_NULL);
        this.predicate = predicate;
        return this;
    }

    /**
     * Sets concurrency level for current Flow. Flow will be executed in parallel for each vUser
     *
     * @param vUsers number of vUsers
     * @return builder
     */
    @Override
    public RxFlowBuilderInterfaces.Options<T> withVUsers(int vUsers) {
        checkVUsersNotSet();
        checkArgument(vUsers > 0, ERROR_V_USERS_NEGATIVE);
        this.vUsers = vUsers;
        return this;
    }

    @Override
    public RxFlowBuilderInterfaces.Options<T> withVUsersAuto() {
        checkVUsersNotSet();
        vUsersAuto = true;
        return this;
    }

    private void checkVUsersNotSet() {
        checkState(this.vUsers == null, ERROR_V_USERS_NOT_ONCE);
        checkState(!vUsersAuto, ERROR_V_USERS_NOT_ONCE);
    }

    /**
     * Add Data Sources to Flow. Flow will repeated with each Data Record
     *
     * @param dataSources Data Sources
     * @return builder
     */
    @Override
    public RxFlowBuilderInterfaces.Options<T> withDataSources(RxDataSource... dataSources) {
        checkDataSources(dataSources);
        this.dataSources = dataSources;
        return this;
    }

    private void checkDataSources(RxDataSource[] dataSources) {
        checkState(this.dataSources == null, ERROR_DATA_SOURCES_NOT_ONCE);
        checkNotNull(dataSources, ERROR_DATA_SOURCES_NULL);
        checkArgument(dataSources.length > 0, ERROR_DATA_SOURCES_EMPTY);
        boolean atLeastOneNotCyclic = false;
        for (RxDataSource dataSource : dataSources) {
            checkNotNull(dataSource, ERROR_DATA_SOURCE_NULL);
            if (!dataSource.isCyclic()) {
                atLeastOneNotCyclic = true;
            }
        }
        checkArgument(atLeastOneNotCyclic, ERROR_DATA_SOURCE_TOO_CYCLIC);
    }

    /**
     * Adds a test step as the last one in this flow.
     *
     * @param testStep test step to add
     * @return builder
     */
    @Override
    public RxFlowBuilderInterfaces.Steps<T> addTestStep(RxTestStep testStep) {
        checkNotNull(testStep, ERROR_TEST_STEP_NULL);
        return addInvocation(testStep);
    }

    /**
     * @deprecated For migration simplification only. Please use {@link #withBefore(RxTestStep...)}
     */
    @Override
    @Deprecated
    public RxFlowBuilderInterfaces.Before<T> beforeFlow(Runnable... runnables) {
        return withBefore(toTestSteps(runnables));
    }

    /**
     * Run given steps before flow. Will be run once not depending on vUser count or Data Sources
     *
     * @param testStep to run
     * @return builder
     */
    @Override
    public RxFlowBuilderInterfaces.Before<T> withBefore(RxTestStep... testStep) {
        checkState(beforeInvocation.isEmpty(), ERROR_WITH_BEFORE_NOT_ONCE + HINT_SINGLE_CALL);
        beforeInvocation = asList(testStep);
        return this;
    }

    /**
     * @deprecated For migration simplification only. Please use {@link #withAfter(RxTestStep...)}
     */
    @Override
    @Deprecated
    public RxFlowBuilderInterfaces.After<T> afterFlow(Runnable... runnables) {
        return withAfter(toTestSteps(runnables));
    }

    /**
     * Run given steps after flow. Will be run once not depending on vUser count or Data Sources
     *
     * @param testStep to run
     * @return builder
     */
    @Override
    public RxFlowBuilderInterfaces.After<T> withAfter(RxTestStep... testStep) {
        checkState(afterInvocation.isEmpty(), ERROR_WITH_AFTER_NOT_ONCE + HINT_SINGLE_CALL);
        afterInvocation = asList(testStep);
        return this;
    }

    private RxTestStep[] toTestSteps(Runnable[] runnables) {
        RxTestStep[] rxTestSteps = new RxTestStep[runnables.length];
        int i = 0;
        for (Runnable runnable : runnables) {
            rxTestSteps[i++] = Api.runnable(runnable);
        }
        return rxTestSteps;
    }

    /**
     * @see #addSubFlow(Flow subFlow)
     */
    @Override
    public RxFlowBuilderInterfaces.Steps<T> addSubFlow(Builder<T> subFlow) {
        checkNotNull(subFlow, ERROR_SUBFLOW_NULL);
        return addSubFlow(subFlow.build());
    }

    /**
     * Adds a subFlow as a subflow to this Flow.
     *
     * @param subFlow to add
     * @return builder
     */
    @Override
    public RxFlowBuilderInterfaces.Steps<T> addSubFlow(T subFlow) {
        checkNotNull(subFlow, ERROR_SUBFLOW_NULL);
        return split(subFlow);
    }

    /**
     * @return builder
     * @see #split(Flow...)
     */
    @Override
    public RxFlowBuilderInterfaces.Steps<T> split(Builder<T>... builders) {
        checkNotNull(builders, ERROR_SUBFLOWS_NULL);
        T[] flows = createFlowArray(builders.length);
        for (int i = 0; i < builders.length; i++) {
            checkNotNull(builders[i], ERROR_SUBFLOW_NULL);
            flows[i] = builders[i].build();
        }
        return split(flows);
    }

    protected abstract T[] createFlowArray(int length);

    /**
     * Run given flows in parallel
     *
     * @param parallelFlows flow to run in parallel
     * @return builder
     */
    @Override
    public RxFlowBuilderInterfaces.Steps<T> split(Flow... parallelFlows) {
        checkNotNull(parallelFlows, ERROR_SUBFLOWS_NULL);
        for (Flow flow : parallelFlows) {
            checkNotNull(flow, ERROR_SUBFLOW_NULL);
        }
        Internals.Fork fork = new Internals.Fork(newArrayList(parallelFlows));
        return addInvocation(fork);
    }

    private FlowBuilder<T> addInvocation(Invocation invocation) {
        testSteps.add(invocation);
        return this;
    }

    /**
     * Test Step added flow will be run even if previous Test Step threw an un-handled exception.
     *
     * @return builder
     */
    @Override
    public RxFlowBuilderInterfaces.AlwaysRun<T> alwaysRun() {
        Invocation lastInvocation = testSteps.remove(testSteps.size() - 1);
        testSteps.add(lastInvocation.alwaysRun());
        return this;
    }

    /**
     * Set an exception handler for the Flow, which will be called on exceptions in Test Steps.
     * If the exception handler does not propagate Exception, scenario flow will continue.
     *
     * @see RxScenarioRunnerBuilder#withDefaultExceptionHandler(RxExceptionHandler)
     * @see RxScenarioBuilder#withExceptionHandler(RxExceptionHandler)
     */
    public RxFlowBuilderInterfaces.Options<T> withExceptionHandler(RxExceptionHandler exceptionHandler) {
        checkNotNull(exceptionHandler, ERROR_EXCEPTION_HANDLER_NULL);
        checkState(this.exceptionHandler == null,
                ERROR_EXCEPTION_HANDLER_NOT_ONCE + HINT_EXCEPTION_HANDLER);
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    /**
     * @return Flow
     */
    @Override
    public T build() {
        vUsers = calculateVUsers();
        DataSourceStrategy dataSource = (dataSources == null)
                ? DataSourceStrategy.empty(name, vUsers)
                : DataSourceStrategy.fromDefinitions(dataSources, vUsers);

        return createFlow(name, dataSource, testSteps, beforeInvocation, afterInvocation, exceptionHandler, predicate);
    }

    protected abstract T createFlow(String name, DataSourceStrategy dataSource, List<Invocation> testSteps,
                                    List<RxTestStep> beforeInvocations, List<RxTestStep> afterInvocations,
                                    ExceptionHandler exceptionHandler, Predicate<RxDataRecordWrapper> predicate);

    private int calculateVUsers() {
        if (vUsersAuto) {
            int minDataRecords = minDataRecords();
            int vUserThreshold = vUserThreshold();
            return min(minDataRecords, vUserThreshold);
        } else if (vUsers == null) {
            return 1;
        } else {
            return vUsers;
        }
    }

    private int minDataRecords() {
        checkState(dataSources != null, ERROR_V_USERS_AUTO_NO_DATA_SOURCES);

        int minDataRecords = Integer.MAX_VALUE;
        List<String> nonSharedDSNames = newArrayList();
        for (RxDataSource dataSource : dataSources) {
            if (!dataSource.isShared()) {
                nonSharedDSNames.add(dataSource.getName());
            } else if (!dataSource.isCyclic()) {
                Preconditions.checkArgument(dataSource.getSize() > 0, format(DataSourceStrategy.ERROR_DATA_SOURCE_EMPTY, dataSource.getName()));
                minDataRecords = min(minDataRecords, dataSource.getSize());
            }
        }

        checkState(nonSharedDSNames.isEmpty(),
                ERROR_V_USERS_AUTO_NON_SHARED + HINT_NON_SHARED +
                        Joiner.on(',').join(nonSharedDSNames));

        return minDataRecords;
    }

    @VisibleForTesting
    int vUserThreshold() {
        int numProcessors = getRuntime().availableProcessors();
        return V_USERS_PER_PROCESSOR * numProcessors;
    }
}
