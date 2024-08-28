package com.ericsson.de.scenariorx.api;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.HashMap;
import java.util.Map;

import com.ericsson.de.scenariorx.impl.Bridge;
import com.ericsson.de.scenariorx.impl.FlowBuilder;
import com.ericsson.de.scenariorx.impl.Invocation;
import com.google.common.base.Optional;

/**
 * Definition of the Test Step with base functionality
 */
public abstract class RxTestStep extends Invocation {

    public enum Status {
        SUCCESS, FAILED, SKIPPED
    }

    public static final String ERROR_PARAMETER_NULL = "Parameter cannot be null";
    public static final String ERROR_PARAMETER_ALREADY_SET = "Parameter '%s' already set";

    protected final String name;

    private final Map<String, Object> parameters = new HashMap<>();
    private final Map<String, String> bindings = new HashMap<>();
    private boolean alwaysRun;
    private RxContextDataSource resultingDataSource;

    public RxTestStep(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isAlwaysRun() {
        return alwaysRun;
    }

    /**
     * Overrides input field named <code>key</code> of Test Step
     *
     * @param key name of input
     * @return parameter
     */
    public RxTestStep.ParameterBuilder withParameter(String key) {
        validateParameter(key);
        return new RxTestStep.ParameterBuilder(key);
    }

    protected void validateParameter(String key) {
        checkArgument(key != null, ERROR_PARAMETER_NULL);
        checkState(!parameters.containsKey(key) && !bindings.containsKey(key),
                ERROR_PARAMETER_ALREADY_SET, key);
    }

    /**
     * @deprecated use {@link #withParameter(String).value(Object)} instead.
     */
    @Deprecated
    public RxTestStep withParameter(String key, Object value) {
        return withParameter(key).value(value);
    }

    /**
     * @see FlowBuilder#alwaysRun()
     */
    public RxTestStep alwaysRun() {
        RxTestStep copy = copy();
        copy.alwaysRun = true;
        return copy;
    }

    /**
     * If Test Step returns value, it's possible to collect resulting values from all executed Test Steps to Data Source
     * for further reuse of this Data Source in following flows.
     *
     * @param dataSource target Data Source
     * @return builder
     */
    public RxTestStep collectResultsToDataSource(RxContextDataSource dataSource) {
        RxTestStep copy = copy();
        copy.resultingDataSource = dataSource;
        return copy;
    }

    public Optional<Object> run(RxDataRecordWrapper dataRecord) throws Exception {
        RxDataRecordWrapper wrappedDataRecord = Bridge.wrapWithParameters(parameters, bindings, dataRecord);
        Optional<Object> result = doRun(wrappedDataRecord);
        parseResult(result);

        return result;
    }

    private void parseResult(Optional<Object> result) {
        if (resultingDataSource != null && result.isPresent()) {
            resultingDataSource.collectFromResult(getName(), result.get());
        }
    }

    protected abstract Optional<Object> doRun(RxDataRecordWrapper dataRecord) throws Exception;

    /**
     * Method for overrides
     *
     * @return copy of implementation with all implementation specific params
     */
    protected abstract RxTestStep copySelf();

    private RxTestStep copy() {
        RxTestStep copy = copySelf();
        copy.parameters.putAll(parameters);
        copy.bindings.putAll(bindings);
        copy.alwaysRun = alwaysRun;
        copy.resultingDataSource = resultingDataSource;

        return copy;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", getId())
                .add("name", getName())
                .toString();
    }

    public class ParameterBuilder {
        private final String key;

        ParameterBuilder(String key) {
            this.key = key;
        }

        /**
         * Override parameter with constant <code>value</code>
         *
         * @return builder
         */
        public RxTestStep value(Object value) {
            RxTestStep copy = copy();
            copy.parameters.put(key, value);
            return copy;
        }

        /**
         * Override parameter with field value of previously executed <code>testStep</code> of same Flow
         * If Test Step returns {@link RxDataRecord}, this will allow to get its fields.
         *
         * @return builder
         */
        public RxTestStep fromTestStepResult(RxTestStep testStep, String field) {
            RxTestStep copy = copy();
            copy.bindings.put(key, testStep.getName() + "." + field);
            return copy;
        }

        /**
         * Override parameter with result of previously executed <code>testStep</code> of same Flow
         * If Test Step returns {@link RxDataRecord} parameter should extend {@link RxDataRecord}
         * If Test Step returns object parameter should be type of return object.
         *
         * @return builder
         */
        public RxTestStep fromTestStepResult(RxTestStep testStep) {
            RxTestStep copy = copy();
            copy.bindings.put(key, testStep.getName());
            return copy;
        }

        /**
         * Override parameter with custom value
         *
         * @return builder
         */
        public RxTestStep bindTo(String field) {
            RxTestStep copy = copy();
            copy.bindings.put(key, field);
            return copy;
        }

        /**
         * Override parameter with field value from Data Source
         *
         * @return builder
         */
        public RxTestStep fromDataSource(RxDataSource dataSource, String field) {
            RxTestStep copy = copy();
            copy.bindings.put(key, dataSource.getName() + "." + field);
            return copy;
        }

        /**
         * Override parameter with {@link RxDataRecord} from Data Source
         *
         * @return builder
         */
        public RxTestStep fromDataSource(RxDataSource dataSource) {
            RxTestStep copy = copy();
            copy.bindings.put(key, dataSource.getName());
            return copy;
        }

        /**
         * Override parameter with field value from Scenario Context
         *
         * @return builder
         */
        public RxTestStep fromContext(String field) {
            RxTestStep copy = copy();
            copy.bindings.put(key, ScenarioContext.CONTEXT_RECORD_NAME + "." + field);
            return copy;
        }
    }
}
