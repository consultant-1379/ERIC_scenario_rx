package com.ericsson.de.scenariorx.impl;

import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Parameter<T> {

    private final Class<T> type;
    private final Annotation[] annotations;

    public static List<Parameter> parametersFor(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        List<Parameter> parameters = new ArrayList<>();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            Annotation[] annotations = parameterAnnotations[i];
            Parameter parameter = new Parameter(parameterType, annotations);
            parameters.add(parameter);
        }
        return parameters;
    }

    public Parameter(Class<T> parameterType, Annotation... annotations) {
        this.type = parameterType;
        this.annotations = annotations;
    }

    public Class<T> getType() {
        return type;
    }

    public String getName() {
        return getScenarioParameterName(annotations);
    }

    public static String getScenarioParameterName(Annotation[] annotations) {
        if (isAnnotationPresent(Named.class, annotations)) {
            return getAnnotation(Named.class, annotations).value();
        }
        return null;
    }

    public static boolean isAnnotationPresent(Class<? extends Annotation> annotationClass, Annotation[] annotations) {
        return getAnnotation(annotationClass, annotations) != null;
    }

    public static <T extends Annotation> T getAnnotation(Class<T> annotationClass, Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotationClass.isInstance(annotation)) {
                return annotationClass.cast(annotation);
            }
        }
        return null;
    }

    public boolean isAnnotated() {
        return getName() != null;
    }
}
