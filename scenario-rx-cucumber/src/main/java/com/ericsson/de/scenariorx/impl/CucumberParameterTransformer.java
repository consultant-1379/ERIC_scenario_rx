package com.ericsson.de.scenariorx.impl;

import static com.ericsson.de.scenariorx.impl.ScenarioRuntime.LOCALE;
import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cucumber.api.DataTable;
import cucumber.runtime.ParameterInfo;
import cucumber.runtime.table.TableConverter;
import cucumber.runtime.xstream.LocalizedXStreams;
import gherkin.formatter.Argument;
import gherkin.formatter.model.Step;

class CucumberParameterTransformer {
    static Object[] getArgs(Method stepDefinition, Step step, List<Argument> arguments) {
        ClassLoader classLoader = step.getClass().getClassLoader();
        LocalizedXStreams.LocalizedXStream xStream = new LocalizedXStreams(classLoader).get(LOCALE.getLocale());

        List<Object> result = new ArrayList<>();

        List<ParameterInfo> parameterInfos = ParameterInfo.fromMethod(stepDefinition);
        Annotation[][] annotations = stepDefinition.getParameterAnnotations();

        Iterator<Argument> argumentIterator = arguments.iterator();

        int n = 0;
        for (ParameterInfo parameterInfo : parameterInfos) {
            Object arg;
            if (Parameter.getScenarioParameterName(annotations[n]) != null) {
                arg = null; // skip Scenario Parameters as it will be resolved in Runtime
            } else if (parameterInfo.getType().equals(DataTable.class)) {
                checkNotNull(step.getRows(), "Expected Data Table, but no Data Table provided");
                arg = tableArgument(parameterInfos, step, n, xStream);
            } else if (isDocString(parameterInfos, argumentIterator, n, parameterInfo)) {
                checkNotNull(step.getDocString(), "Expected Doc String, but no Doc String provided");
                arg = parameterInfo.convert(step.getDocString().getValue(), xStream);
            } else {
                arg = parameterInfo.convert(argumentIterator.next().getVal(), xStream);
            }

            result.add(arg);
            n++;
        }

        return result.toArray(new Object[result.size()]);
    }

    private static boolean isDocString(List<ParameterInfo> parameterInfos, Iterator<Argument> argumentIterator, int n, ParameterInfo parameterInfo) {
        return parameterInfo.getType().equals(String.class)
                && parameterInfos.size() == (n + 1)
                && !argumentIterator.hasNext();
    }

    private static Object tableArgument(List<ParameterInfo> parameterInfos, Step step, int argIndex, LocalizedXStreams.LocalizedXStream xStream) {
        ParameterInfo parameterInfo = parameterInfos.get(argIndex);
        TableConverter tableConverter = new TableConverter(xStream, parameterInfo);
        DataTable table = new DataTable(step.getRows(), tableConverter);
        Type type = parameterInfo.getType();
        return tableConverter.convert(table, type, parameterInfo.isTransposed());
    }
}
