package com.ericsson.de.scenariorx.impl;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;

import static com.ericsson.de.scenariorx.api.RxApi.scenario;
import static com.ericsson.de.scenariorx.impl.Api.during;
import static com.ericsson.de.scenariorx.impl.CucumberFlowBuilder.Type.BACKGROUND;
import static com.ericsson.de.scenariorx.impl.CucumberFlowBuilder.Type.PERFORMANCE;
import static com.ericsson.de.scenariorx.impl.CucumberParameterTransformer.getArgs;
import static com.ericsson.de.scenariorx.impl.GherkinAdapter.adapt;
import static com.ericsson.de.scenariorx.impl.GherkinAdapter.getTags;
import static com.ericsson.de.scenariorx.impl.GherkinAdapter.outlineParams;
import static com.ericsson.de.scenariorx.impl.GherkinAdapter.tagsAndInheritedTags;
import static com.ericsson.de.scenariorx.impl.GherkinAdapter.toDataSource;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.de.scenariorx.api.DebugGraphMode;
import com.ericsson.de.scenariorx.api.RxApi;
import com.ericsson.de.scenariorx.api.RxDataRecord;
import com.ericsson.de.scenariorx.api.RxDataSource;
import com.ericsson.de.scenariorx.api.RxExceptionHandler;
import com.ericsson.de.scenariorx.api.RxFlow;
import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.api.RxScenarioListener;
import com.ericsson.de.scenariorx.api.RxScenarioRunnerBuilder;
import com.ericsson.de.scenariorx.api.RxTestStep;
import com.ericsson.de.scenariorx.cucumber.extra.ExtraStepDefinitions;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import cucumber.api.java.ObjectFactory;
import cucumber.runtime.HookDefinition;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.ScenarioImpl;
import cucumber.runtime.StepDefinitionMatch;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.Resource;
import cucumber.runtime.java.RxScenarioBackend;
import gherkin.AstBuilder;
import gherkin.I18n;
import gherkin.Parser;
import gherkin.TokenMatcher;
import gherkin.ast.Background;
import gherkin.ast.Feature;
import gherkin.ast.GherkinDocument;
import gherkin.ast.Scenario;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.ScenarioOutline;
import gherkin.ast.Step;
import gherkin.ast.Tag;

public class ScenarioRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScenarioRuntime.class);

    public static final I18n LOCALE = new I18n("en");

    public void run(RuntimeOptions runtimeOptions) throws IOException {
        run(runtimeOptions, Lists.<RxScenarioListener>newArrayList());
    }

    public void run(RuntimeOptions runtimeOptions, List<RxScenarioListener> listeners) throws IOException {
        ClassLoader classLoader = runtimeOptions.getClass().getClassLoader();
        List<String> gluePaths = addExtraGluePaths(runtimeOptions);

        CucumberListener cucumberListener = new CucumberListener(
                runtimeOptions.reporter(classLoader),
                runtimeOptions.formatter(classLoader)
        );

        CucumberFilter filter = new CucumberFilter().init(runtimeOptions.getFilters());

        RxScenarioRunnerBuilder runnerBuilder = RxApi.runner()
                .withGraphExportMode(DebugGraphMode.SVG)
                .addListener(cucumberListener);

        addListeners(listeners, runnerBuilder);

        ScenarioRunner runner = runnerBuilder.build();

        MultiLoader resourceLoader = new MultiLoader(classLoader);
        RxScenarioBackend scenarioBackend = new RxScenarioBackend(RxScenarioBackend.buildObjectFactory()).init(classLoader, gluePaths);

        for (Resource featurePath : getFeatures(resourceLoader, runtimeOptions.getFeaturePaths())) {
            try {
                Optional<Background> background = Optional.absent();

                Feature feature = loadFeature(featurePath.getAbsolutePath());
                cucumberListener.onFeatureStarted(feature);

                for (ScenarioDefinition scenarioDefinition : feature.getChildren()) {
                    if (filter.shouldSkip(feature, scenarioDefinition)) {
                        continue;
                    }

                    cucumber.api.Scenario cucumberScenario = new ScenarioImpl(cucumberListener.getReporter(),
                            newHashSet(getTags(scenarioDefinition)),
                            adapt(scenarioDefinition));

                    runHooks(cucumberScenario, tagsAndInheritedTags(feature, scenarioDefinition), scenarioBackend.getGlue().getBeforeHooks());

                    if (background.isPresent()) {
                        runScenario(runner,
                                scenarioBackend,
                                feature,
                                featurePath,
                                background.get(),
                                cucumberListener);
                    }

                    if (scenarioDefinition instanceof Background) {
                        checkArgument(!background.isPresent(), "Only one Background allowed");
                        background = Optional.of((Background) scenarioDefinition);
                    } else {
                        runScenario(runner,
                                scenarioBackend,
                                feature,
                                featurePath,
                                scenarioDefinition,
                                cucumberListener);
                    }

                    runHooks(cucumberScenario, tagsAndInheritedTags(feature, scenarioDefinition), scenarioBackend.getGlue().getAfterHooks());

                    scenarioBackend.clear();
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

            cucumberListener.done();
        }
    }

    private void addListeners(List<RxScenarioListener> listeners, RxScenarioRunnerBuilder runnerBuilder) {
        runnerBuilder.addListener(RxPerformanceListener.get("localhost", 2003));

        for (RxScenarioListener listener : listeners) {
            runnerBuilder.addListener(listener);
        }

        for (RxScenarioListener listener : ServiceLoader.load(RxScenarioListener.class)) {
            LOGGER.info("Loading additional listener " + listener.getClass());
            runnerBuilder.addListener(listener);
        }
    }

    private void runHooks(cucumber.api.Scenario cucumberScenario, Set<gherkin.formatter.model.Tag> tags, List<HookDefinition> hooks) throws Throwable {
        for (HookDefinition hook : hooks) {
            if (hook.matches(tags)) {
                hook.execute(cucumberScenario);
            }
        }
    }

    private void runScenario(ScenarioRunner runner, RxScenarioBackend scenarioBackend, Feature feature, Resource featurePath, ScenarioDefinition scenarioDefinition, CucumberListener cucumberListener) throws InstantiationException, IllegalAccessException {
        CucumberFlowBuilder flow = new CucumberFlowBuilder(scenarioDefinition.getName());

        if (scenarioDefinition instanceof ScenarioOutline) {
            ArrayList<RxDataRecord> dataRecords = toDataSource(ScenarioOutline.class.cast(scenarioDefinition).getExamples());
            flow.withOutline(dataRecords);
        } else if (scenarioDefinition instanceof Background) {
            flow.withType(BACKGROUND);
        }

        processTags(scenarioDefinition, flow);

        for (final Step gherkinStep : scenarioDefinition.getSteps()) {
            String stepName = gherkinStep.getText();

            gherkin.formatter.model.Step cucumberStep = flow.isOutline() ?
                    adapt(gherkinStep, flow.getOutlineSample()) :
                    adapt(gherkinStep);

            final StepDefinitionMatch match = scenarioBackend.getGlue().stepDefinitionMatch(featurePath.getPath(), cucumberStep, LOCALE);

            checkNotNull(match, "Step `" + gherkinStep.getText() + "` not found!");

            if (match.getLocation().startsWith(ExtraStepDefinitions.class.getSimpleName())) {
                extraMethod(match, flow, cucumberListener);
            } else {
                Method method = scenarioBackend.getMethod(match.getPattern());
                flow.addTestStep(fromCucumber(stepName, cucumberStep, match, method, flow.isOutline(), scenarioBackend.getObjectFactory()));
            }
        }

        if (flow.isPerformance()) {
            List<Internals.TestStepResult> testStepResults = runner.runPerformance(flow);
            new PerformanceReporter().summary(flow.getName(), testStepResults);
        } else {
            RxScenario rxScenario = scenario(flow.getName())
                    .withParameter("testCaseId", flow.getName())
                    .withParameter("testSuiteId", feature.getName())
                    .addFlow(flow.withExceptionHandler(RxExceptionHandler.PROPAGATE))
                    .withExceptionHandler(RxExceptionHandler.IGNORE)
                    .build();

            runner.run(rxScenario);
        }
    }

    void extraMethod(StepDefinitionMatch match, CucumberFlowBuilder flow, CucumberListener cucumberListener) {
        String stepName = match.getStepName();
        if (stepName.matches(ExtraStepDefinitions.PERFORMANCE_TEST)) {
            flow.withType(PERFORMANCE);
        } else if (stepName.matches(ExtraStepDefinitions.REPEAT_FOR)) {
            flow.runWhile(
                    during(
                            Integer.valueOf(getArgument(match, 0)),
                            TimeUnit.valueOf(getArgument(match, 1).toUpperCase())
                    )
            );
        } else if (stepName.matches(ExtraStepDefinitions.RAMPUP_DURING)) {
            flow.withRampUp(
                    RxRampUp.during(
                            Integer.valueOf(getArgument(match, 0)),
                            TimeUnit.valueOf(getArgument(match, 1).toUpperCase())
                    )
            );
        } else if (stepName.matches(ExtraStepDefinitions.REPEATING_FROM_FILE)) {
            String fileName = getArgument(match, 0);
            RxDataSource<RxDataRecord> dataSource = RxApi.fromCsv("ds", fileName, RxDataRecord.class);

            flow.withDataSources(dataSource.shared());
        } else if (stepName.matches(ExtraStepDefinitions.REPEATING_FROM)) {
            String dataSourceName = getArgument(match, 0);

            RxDataSource<RxDataRecord> dataSource = SimpleDataSourceProvider.provide(dataSourceName, cucumberListener);

            flow.withDataSources(dataSource.shared());
        } else if (stepName.matches(ExtraStepDefinitions.REPEATING_FROM_DEFAULT)) {
            String dataSourceName = getArgument(match, 0);
            String defaultLocation = getArgument(match, 1);

            RxDataSource<RxDataRecord> dataSource = SimpleDataSourceProvider.provide(dataSourceName, cucumberListener, defaultLocation);

            flow.withDataSources(dataSource.shared());
        } else if (stepName.matches(ExtraStepDefinitions.NUMBER_OF_USERS_EQUALS)) {
            int vUsers = Integer.parseInt(getArgument(match, 0));

            flow.withVUsers(vUsers);
        }
    }

    private String getArgument(StepDefinitionMatch match, int i) {
        checkArgument(match.getArguments().size() > i, "Unable to find arguments for step " + match.getStepName());
        return match.getArguments().get(i).getVal();
    }

    private void processTags(ScenarioDefinition scenarioDefinition, FlowBuilder<RxFlow> flow) {
        if (scenarioDefinition instanceof Scenario) {
            List<Tag> tags = ((Scenario) scenarioDefinition).getTags();
            for (Tag tag : tags) {
                String name = tag.getName();
                Pattern dataSourcePattern = Pattern.compile("@DataSource\\(\"(.*)\"\\)");
                Matcher matcher = dataSourcePattern.matcher(name);
                if (matcher.matches()) {
                    RxDataSource<RxDataRecord> dataSource = RxApi.fromCsv("ds", matcher.group(1), RxDataRecord.class);
                    flow.withDataSources(dataSource.shared());
                }
                Pattern vUsersPattern = Pattern.compile("@VUsers\\((.*)\\)");
                Matcher vUserMatcher = vUsersPattern.matcher(name);
                if (vUserMatcher.matches()) {
                    flow.withVUsers(Integer.parseInt(vUserMatcher.group(1)));
                }
            }
        }
    }

    private Iterable<Resource> getFeatures(MultiLoader resourceLoader, List<String> paths) {
        Iterable<Resource>[] features = new Iterable[paths.size()];

        Iterator<String> iterator = paths.iterator();
        for (int i = 0; i < features.length; i++) {
            features[i] = resourceLoader.resources(iterator.next(), ".feature");
        }

        return Iterables.concat(features);
    }

    private Feature loadFeature(String featurePath) throws Exception {
        String content = new String(readAllBytes(get(featurePath)));
        TokenMatcher matcher = new TokenMatcher();
        Parser<GherkinDocument> parser = new Parser<>(new AstBuilder());
        GherkinDocument gherkin = parser.parse(content, matcher);

        return gherkin.getFeature();
    }

    private RxTestStep fromCucumber(String stepName, gherkin.formatter.model.Step step, StepDefinitionMatch match, Method method, boolean outline, ObjectFactory objectFactory) {
        try {
            Object instance = objectFactory.getInstance(method.getDeclaringClass());

            List<String> parameterNames = outline ?
                    outlineParams(stepName) :
                    getDefaultParameterNames(method.getParameterTypes().length);

            RxTestStep invocation = new ParametrizedInvocation(instance, method, stepName, parameterNames);

            Object[] fromCucumber = getArgs(method, step, match.getArguments());
            checkArgument(fromCucumber.length == 0 || fromCucumber.length == parameterNames.size());
            for (int i = 0; i < fromCucumber.length; i++) {
                invocation = invocation
                        .withParameter(parameterNames.get(i))
                        .value(fromCucumber[i]);
            }

            return invocation;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Cucumber uses order in parameters. Name parameters to make it resolvable in scenarios
     * Could be overriden by @Named or @Input annotation of method arguments
     */
    private List<String> getDefaultParameterNames(int count) {
        ArrayList<String> names = newArrayList();
        for (int i = 0; i < count; i++) {
            names.add("var" + i);
        }
        return names;
    }

    private List<String> addExtraGluePaths(RuntimeOptions runtimeOptions) {
        String scenarioStepDefinitions = ExtraStepDefinitions.class.getPackage().getName();
        List<String> gluePaths = Lists.newArrayList(runtimeOptions.getGlue());
        for (String gluePath : gluePaths) {
            if (gluePath.contains(scenarioStepDefinitions)
                    || scenarioStepDefinitions.contains(gluePath)) {
                return gluePaths;
            }
        }

        gluePaths.add(scenarioStepDefinitions);
        return gluePaths;
    }
}
