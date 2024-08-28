package com.ericsson.de.scenariorx.impl;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * Representation of individual Test Scenario.
 */
public abstract class Scenario {

    final String name;
    final Map<String, Object> parameters;
    final Flow flow;
    final List<ScenarioListener> listeners;

    protected Scenario(String name, Map<String, Object> parameters, Flow flow, List<ScenarioListener> listeners) {
        this.name = name;
        this.parameters = parameters;
        this.flow = flow;
        this.listeners = listeners;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getParameters() {
        return ImmutableMap.copyOf(parameters);
    }
}
