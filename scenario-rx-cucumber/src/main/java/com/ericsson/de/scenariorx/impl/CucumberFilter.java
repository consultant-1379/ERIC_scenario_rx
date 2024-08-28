package com.ericsson.de.scenariorx.impl;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;

import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;

public class CucumberFilter {
    List<String> tagFilters = Lists.newArrayList();
    List<Pattern> patternFilters = Lists.newArrayList();

    public CucumberFilter() {
    }

    public CucumberFilter init(List<Object> filters) {
        for (Object filter : filters) {
            Class<?> typeOfFilter = filter.getClass();

            if (String.class.isAssignableFrom(typeOfFilter)) {
                tagFilters.add(String.class.cast(filter));
            } else if (Number.class.isAssignableFrom(typeOfFilter)) {
                throw new UnsupportedOperationException();
            } else if (Pattern.class.isAssignableFrom(typeOfFilter)) {
                patternFilters.add(Pattern.class.cast(filter));
            } else {
                throw new RuntimeException("Could not create filter method for unknown filter of type: " + typeOfFilter);
            }
        }
        return this;
    }

    public boolean shouldSkip(Feature feature, ScenarioDefinition scenarioDefinition) {
        Set<gherkin.formatter.model.Tag> tags = GherkinAdapter.tagsAndInheritedTags(feature, scenarioDefinition);

        return shouldSkip(tags) || shouldSkip(scenarioDefinition.getName());
    }

    private boolean shouldSkip(Set<gherkin.formatter.model.Tag> tags) {
        if (tagFilters.isEmpty()) {
            return false;
        } else {
            for (gherkin.formatter.model.Tag tag : tags) {
                if (tagFilters.contains(tag.getName())) {
                    return false;
                }
            }
            return true;
        }
    }

    public boolean shouldSkip(String name) {
        if (patternFilters.isEmpty()) {
            return false;
        }

        for (Pattern patternFilter : patternFilters) {
            if (patternFilter.matcher(name).matches()) {
                return false;
            }
        }
        return true;
    }
}
