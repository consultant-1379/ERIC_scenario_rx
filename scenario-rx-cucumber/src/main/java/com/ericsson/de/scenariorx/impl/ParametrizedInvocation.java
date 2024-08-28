package com.ericsson.de.scenariorx.impl;

import static com.ericsson.de.scenariorx.impl.Parameter.parametersFor;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayListWithCapacity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;
import com.ericsson.de.scenariorx.api.RxTestStep;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

@SuppressWarnings("WeakerAccess")
public class ParametrizedInvocation extends RxTestStep {

    private final Object instance;
    private final Method method;
    private final List<String> parameterNames;

    ParametrizedInvocation(Object instance, Method method, String name, List<String> parameterNames) {
        super(name);
        this.parameterNames = parameterNames;
        Preconditions.checkArgument(parameterNames.size() == method.getParameterTypes().length || parameterNames.isEmpty());
        this.instance = instance;
        this.method = method;
    }

    @Override
    protected Optional<Object> doRun(RxDataRecordWrapper dataRecord) throws Exception {
        List<Object> args = parseArguments(dataRecord);
        Object result = invokeMethodWith(args);

        return Optional.fromNullable(result);
    }

    @Override
    protected RxTestStep copySelf() {
        return new ParametrizedInvocation(instance, method, name, parameterNames);
    }

    private Object invokeMethodWith(List<Object> args) throws Exception {
        try {
            return method.invoke(instance, args.toArray());
        } catch (InvocationTargetException e) {
            throw propagate(e.getTargetException());
        }
    }

    private List<Object> parseArguments(RxDataRecordWrapper dataRecord) {
        List<Parameter> parameters = parametersFor(method);

        List<Object> arguments = newArrayListWithCapacity(parameters.size());

        for (int i = 0; i < parameters.size(); i++) {
            Parameter parameter = parameters.get(i);
            if (parameter.isAnnotated()) {
                Optional value = dataRecord.getFieldValue(parameter.getName(), parameter.getType());
                arguments.add(value.orNull());
            } else if (!parameterNames.isEmpty()) {
                Optional value = dataRecord.getFieldValue(parameterNames.get(i), parameter.getType());
                arguments.add(value.orNull());
            } else {
                throw new IllegalArgumentException("Unable to get parameter ");
            }
        }

        return arguments;
    }
}

