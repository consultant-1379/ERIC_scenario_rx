package cucumber.runtime.java;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import cucumber.api.java.ObjectFactory;
import cucumber.runtime.RuntimeGlue;
import cucumber.runtime.UndefinedStepsTracker;
import cucumber.runtime.Utils;
import cucumber.runtime.xstream.LocalizedXStreams;

public class RxScenarioBackend extends JavaBackend {
    Map<String, Method> methodByPattern = Maps.newHashMap();
    RuntimeGlue glue;
    private ObjectFactory objectFactory;

    public RxScenarioBackend(ObjectFactory objectFactory) {
        super(objectFactory);
        this.objectFactory = objectFactory;
    }

    public RxScenarioBackend init(ClassLoader classLoader, List<String> gluePaths) {
        glue = new RuntimeGlue(new UndefinedStepsTracker(), new LocalizedXStreams(classLoader));
        loadGlue(glue, gluePaths);
        return this;
    }

    @Override
    void addStepDefinition(Annotation annotation, Method method) {
        methodByPattern.put(pattern(annotation), method);
        super.addStepDefinition(annotation, method);
    }

    public RuntimeGlue getGlue() {
        return glue;
    }

    public Method getMethod(String pattern) {
        return methodByPattern.get(pattern);
    }

    private String pattern(Annotation annotation) {
        try {
            Method regexpMethod = annotation.getClass().getMethod("value");
            return (String) Utils.invoke(annotation, regexpMethod, 0);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static ObjectFactory buildObjectFactory() {
        return new DefaultJavaObjectFactory();
    }

    public ObjectFactory getObjectFactory() {
        return objectFactory;
    }

    public void clear() {
        objectFactory.stop();
    }
}
