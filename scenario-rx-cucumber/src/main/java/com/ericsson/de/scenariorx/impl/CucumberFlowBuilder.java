package com.ericsson.de.scenariorx.impl;

import static com.ericsson.de.scenariorx.impl.Api.fromDataRecords;
import static com.ericsson.de.scenariorx.impl.CucumberFlowBuilder.Type.BACKGROUND;
import static com.ericsson.de.scenariorx.impl.CucumberFlowBuilder.Type.DEFAULT;
import static com.ericsson.de.scenariorx.impl.CucumberFlowBuilder.Type.OUTLINE;
import static com.ericsson.de.scenariorx.impl.CucumberFlowBuilder.Type.PERFORMANCE;
import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import com.ericsson.de.scenariorx.api.RxDataRecord;
import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;
import com.ericsson.de.scenariorx.api.RxFlow;
import com.ericsson.de.scenariorx.api.RxFlowBuilderInterfaces;
import com.ericsson.de.scenariorx.cucumber.extra.ExtraStepDefinitions;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

public class CucumberFlowBuilder extends PerformanceFlowBuilder {
    public enum Type {
        BACKGROUND, PERFORMANCE, OUTLINE, DEFAULT
    }

    private Type type = DEFAULT;
    private RxDataRecord outlineSample;

    protected CucumberFlowBuilder(String name) {
        super(name);
    }

    public void withOutline(List<RxDataRecord> dataRecords) {
        withType(OUTLINE);
        withDataSources(fromDataRecords("outline", dataRecords.toArray(new RxDataRecord[dataRecords.size()])));
        outlineSample = dataRecords.get(0);
    }


    public Type getType() {
        return type;
    }

    public String getName() {
        if (isBackground()) {
            return "Background";
        } else {
            return name;
        }
    }

    public RxDataRecord getOutlineSample() {
        return outlineSample;
    }

    public CucumberFlowBuilder withType(Type type) {
        checkArgument(this.type.equals(DEFAULT), "Can not run Scenario with two types: %s, %s", this.type, type);
        this.type = type;
        return this;
    }

    @Override
    public RxFlowBuilderInterfaces.Options<RxFlow> runWhile(Predicate<RxDataRecordWrapper> predicate) {
        Preconditions.checkArgument(isPerformance(), "Need to add to Scenario " + ExtraStepDefinitions.PERFORMANCE_TEST);
        return super.runWhile(predicate);
    }

    @Override
    public void withRampUp(RxRampUp.StrategyProvider rampUp) {
        Preconditions.checkArgument(isPerformance(), "Need to add to Scenario " + ExtraStepDefinitions.PERFORMANCE_TEST);
        super.withRampUp(rampUp);
    }

    public boolean isOutline() {
        return OUTLINE.equals(type);
    }

    public boolean isPerformance() {
        return PERFORMANCE.equals(type);
    }

    public boolean isBackground() {
        return BACKGROUND.equals(type);
    }
}
